package com.example.rifa.services;

import com.example.rifa.entity.Usuario;
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

    public List<Usuario> obtenerTodosLosUsuarios() {
        return usuarioRepository.findAll();
    }

    public Usuario obtenerUsuarioPorId(int id) {
        return usuarioRepository.findById(id).orElse(null);
    }

   /* public Usuario registrarUsuario(Usuario usuario) {
        // Verificar si el correo electrónico ya está registrado
        Usuario usuarioExistente = usuarioRepository.findByEmail(usuario.getEmail());
        if (usuarioExistente != null) {
            throw new IllegalArgumentException("Este usuario ya está registrado");
        }

        // Validar que las contraseñas coincidan
        if (!usuario.getPassword().equals(usuario.getConfirmarPassword())) {
            throw new IllegalArgumentException("Las contraseñas no coinciden");
        }

        // Registrar el nuevo usuario
        usuario.setFechaRegistro(ZonedDateTime.now(ZoneId.of("UTC")));
        usuario.setEsVip(false); // Por defecto, no es VIP
        return usuarioRepository.save(usuario);
    }*/

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

   /* public Usuario actualizarUsuario(int id, Usuario usuarioActualizado) {
        return usuarioRepository.findById(id).map(usuario -> {
            usuario.setName(usuarioActualizado.getName());
            usuario.setEmail(usuarioActualizado.getEmail());
            usuario.setPassword(usuarioActualizado.getPassword());
            usuario.setConfirmarPassword(usuarioActualizado.getConfirmarPassword());
            usuario.setTelefono(usuarioActualizado.getTelefono());
            usuario.setEsVip(usuarioActualizado.isEsVip());
            return usuarioRepository.save(usuario);
        }).orElse(null);
    }*/

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


}
