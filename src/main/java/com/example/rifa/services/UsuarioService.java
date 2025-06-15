package com.example.rifa.services;

import com.example.rifa.entity.CodigoVip;
import com.example.rifa.entity.Usuario;
import com.example.rifa.repository.CodigoVipRepository;
import com.example.rifa.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UsuarioService {
    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private CodigoVipRepository codigoVipRepository;

    public List<Usuario> obtenerTodosLosUsuarios() {
        return usuarioRepository.findAll();
    }

    public Usuario obtenerUsuarioPorId(int id) {
        return usuarioRepository.findById(id).orElse(null);
    }


    public Usuario registrarUsuario(Usuario usuario) {
        Usuario usuarioExistente = usuarioRepository.findByEmail(usuario.getEmail());
        if (usuarioExistente != null) {
            throw new IllegalArgumentException("Este usuario ya está registrado");
        }

        if (!usuario.getPassword().equals(usuario.getConfirmarPassword())) {
            throw new IllegalArgumentException("Las contraseñas no coinciden");
        }

        usuario.setFechaRegistro(ZonedDateTime.now(ZoneId.of("UTC")));
        usuario.setEsVip(false); // Por defecto, no es VIP
        usuario.setCantidadRifas(usuario.isEsVip() ? 10 : 1); // 🔥 Inicializamos según si es VIP
        usuario.setPrimeraVez(true);
        return usuarioRepository.save(usuario);
    }



    public Usuario login(String email, String password) {
        Usuario usuario = usuarioRepository.findByEmail(email);
        if (usuario != null && usuario.getPassword().equals(password)) {
            return usuario;
        }
        return null;
    }

    public Optional<Usuario> findUserByEmail(String email) {
        return Optional.ofNullable(usuarioRepository.findByEmail(email));
    }

    public String generarCodigoRecuperacion(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email);
        if (usuario != null) {
            String codigo = UUID.randomUUID().toString();
            usuario.setCodigoRecuperacion(codigo);
            usuarioRepository.save(usuario);

            // Enviar correo electrónico con el código de recuperación
            String subject = "Recuperación de Contraseña";
            String text = "Tu código de recuperación es: " + codigo;
            emailService.sendEmail(email, subject, text);

            return codigo;
        }
        return null;
    }

    public boolean cambiarPassword(String email, String codigo, String nuevaPassword) {
        Usuario usuario = usuarioRepository.findByEmail(email);
        if (usuario != null && usuario.getCodigoRecuperacion().equals(codigo)) {
            usuario.setPassword(nuevaPassword);
            usuario.setConfirmarPassword(nuevaPassword);
            usuario.setCodigoRecuperacion(null);
            usuarioRepository.save(usuario);
            return true;
        }
        return false;
    }


    public Usuario actualizarUsuario(int id, Usuario usuarioActualizado) {
        return usuarioRepository.findById(id).map(usuario -> {
            usuario.setName(usuarioActualizado.getName());
            usuario.setEmail(usuarioActualizado.getEmail());
            usuario.setPassword(usuarioActualizado.getPassword());
            usuario.setConfirmarPassword(usuarioActualizado.getConfirmarPassword());
            usuario.setTelefono(usuarioActualizado.getTelefono());
            usuario.setEsVip(usuarioActualizado.isEsVip());

            // 🔥 Mantener `cantidadRifas` existente si no se proporciona en la actualización
            usuario.setCantidadRifas(usuarioActualizado.getCantidadRifas() != null ? usuarioActualizado.getCantidadRifas() : usuario.getCantidadRifas());

            return usuarioRepository.save(usuario);
        }).orElse(null);
    }


    public String eliminarUsuario(int id) {
        if (usuarioRepository.existsById(id)) {
            usuarioRepository.deleteById(id);
            return "Usuario eliminado correctamente";
        } else {
            return "Usuario no encontrado";
        }
    }

    public void decrementarCantidadRifas(int userId) {
        Usuario usuario = usuarioRepository.findById(userId).orElse(null);
        if (usuario != null && usuario.isEsVip() && usuario.getCantidadRifas() > 0) {
            usuario.setCantidadRifas(usuario.getCantidadRifas() - 1);
            usuarioRepository.save(usuario);
        }
    }

    /*public Usuario activarVip(int userId, String codigoIngresado) {
        Usuario usuario = usuarioRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        CodigoVip codigoVip = codigoVipRepository.findByCodigo(codigoIngresado)
                .orElseThrow(() -> new IllegalArgumentException("Código VIP inválido o ya utilizado"));

        if (codigoVip.isUtilizado()) {
            throw new IllegalArgumentException("Este código ya fue utilizado.");
        }

        // Activar VIP
        usuario.setEsVip(true);
        usuario.setCodigoVip(codigoIngresado);
        usuario.setCantidadRifas(codigoVip.getCantidadRifas());
        usuario.setFechaRegistro(ZonedDateTime.now());

        // Marcar el código como utilizado
        codigoVip.setUtilizado(true);

        // Guardar cambios
        usuarioRepository.save(usuario);
        codigoVipRepository.save(codigoVip);

        return usuario;
    }*/

    public Usuario activarVip(int userId, String codigoIngresado) {
        // 🔹 Buscar el usuario
        Usuario usuario = usuarioRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con ID: " + userId));

        // 🔹 Buscar el código VIP
        Optional<CodigoVip> codigoVipOpt = codigoVipRepository.findByCodigo(codigoIngresado);

        if (codigoVipOpt.isEmpty()) {
            throw new IllegalArgumentException("El código VIP ingresado no existe en la base de datos.");
        }

        CodigoVip codigoVip = codigoVipOpt.get();

        // 🔹 Verificar si el código ya está utilizado
        if (codigoVip.isUtilizado()) {
            throw new IllegalArgumentException("Este código ya fue utilizado.");
        }

        // 🔹 Verificar si el código está reservado por otro usuario
        if (codigoVip.getUsuarioAsignado() != null && codigoVip.getUsuarioAsignado().getId() != userId) {
            throw new IllegalArgumentException("Este código VIP está reservado por otro usuario.");
        }

        // 🔹 Evitar que un usuario VIP cambie su código
        if (usuario.isEsVip() && !usuario.getCodigoVip().equals(codigoIngresado)) {
            throw new IllegalArgumentException("Este usuario ya tiene un código VIP asignado.");
        }

        // 🔥 Activar VIP
        usuario.setEsVip(true);
        usuario.setCodigoVip(codigoIngresado);
        usuario.setCantidadRifas(codigoVip.getCantidadRifas());
        usuario.setFechaRegistro(ZonedDateTime.now());

        // 🔹 Marcar código como utilizado
        codigoVip.setUtilizado(true);
        codigoVip.setUsuarioAsignado(usuario);

        // 🔹 Guardar cambios
        usuarioRepository.save(usuario);
        codigoVipRepository.save(codigoVip);

        return usuario;
    }



}
