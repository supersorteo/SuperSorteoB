package com.example.rifa.services;

import com.example.rifa.entity.CodigoVip;
import com.example.rifa.entity.Rifa;
import com.example.rifa.entity.Usuario;
import com.example.rifa.exception.ResourceNotFoundException;
import com.example.rifa.repository.CodigoVipRepository;
import com.example.rifa.repository.RifaRepository;
import com.example.rifa.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

@Service
public class UsuarioService {
    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private CodigoVipRepository codigoVipRepository;

    @Autowired
    private RifaService rifaService;

    @Autowired
    private RifaRepository rifaRepository;





    public List<Usuario> obtenerTodosLosUsuarios() {
        List<Usuario> usuarios = usuarioRepository.findAll();
        if (usuarios.isEmpty()) {
            return new ArrayList<>(); // Devuelve array vacío en lugar de excepción
        }
        return usuarios;
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

    /*public boolean cambiarPassword(String email, String codigo, String nuevaPassword) {
        Usuario usuario = usuarioRepository.findByEmail(email);
        if (usuario != null && usuario.getCodigoRecuperacion().equals(codigo)) {
            usuario.setPassword(nuevaPassword);
            usuario.setConfirmarPassword(nuevaPassword);
            usuario.setCodigoRecuperacion(null);
            usuarioRepository.save(usuario);
            return true;
        }
        return false;
    }*/

    public boolean cambiarPassword(String email, String codigo, String nuevaPassword) {
        // 1. Buscar usuario
        Usuario usuario = usuarioRepository.findByEmail(email);

        if (usuario == null) {
            System.err.println("❌ Usuario no encontrado con email: " + email);
            return false;
        }

        // 2. Verificar que tenga código de recuperación
        if (usuario.getCodigoRecuperacion() == null) {
            System.err.println("❌ El usuario no tiene código de recuperación activo");
            return false;
        }

        // 3. Verificar que el código coincida
        if (!usuario.getCodigoRecuperacion().equals(codigo)) {
            System.err.println("❌ Código incorrecto. Esperado: " + usuario.getCodigoRecuperacion() + ", Recibido: " + codigo);
            return false;
        }

        // ✅ Todo correcto, cambiar contraseña
        System.out.println("✅ Cambiando contraseña para: " + email);
        usuario.setPassword(nuevaPassword);
        usuario.setConfirmarPassword(nuevaPassword);
        usuario.setCodigoRecuperacion(null); // Limpiar código
        usuarioRepository.save(usuario);

        return true;
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




   /* @Transactional
    public Map<String, Object> eliminarUsuario(int id) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (!usuarioRepository.existsById(id)) {
                response.put("error", "Usuario no encontrado con ID: " + id);
                return response;
            }

            Usuario usuario = usuarioRepository.findById(id).get();
            System.out.println("Usuario encontrado: ID " + id + ", Nombre: " + usuario.getName());

            List<Rifa> rifas = rifaRepository.findByUsuarioId((long) id);
            System.out.println("Rifas encontradas: " + rifas.size());
            int deletedRifas = 0;
            List<String> errors = new ArrayList<>();
            for (Rifa rifa : rifas) {
                try {
                    System.out.println("Eliminando rifa ID " + rifa.getId() + " (" + rifa.getNombre() + ")");
                    rifaService.eliminarRifa(rifa.getId());
                    deletedRifas++;
                } catch (Exception e) {
                    String errorMsg = "Error al eliminar rifa " + rifa.getId() + ": " + e.getClass().getSimpleName() + " - " + e.getMessage();
                    System.err.println(errorMsg);
                    errors.add(errorMsg);
                }
            }

            usuarioRepository.delete(usuario);
            System.out.println("Usuario ID " + id + " eliminado.");
            response.put("message", "Usuario eliminado correctamente, incluyendo " + deletedRifas + " rifas asociadas.");
            if (!errors.isEmpty()) {
                response.put("warnings", errors);
            }
            return response;
        } catch (Exception e) {
            String errorMsg = "Error general al eliminar usuario " + id + ": " + e.getClass().getSimpleName() + " - " + e.getMessage();
            System.err.println(errorMsg);
            response.put("error", "Error interno al eliminar usuario: " + errorMsg);
            return response;
        }
    }*/

   /* @Transactional
    public Map<String, Object> eliminarUsuario(int id) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (!usuarioRepository.existsById(id)) {
                response.put("error", "Usuario no encontrado con ID: " + id);
                return response;
            }

            Usuario usuario = usuarioRepository.findById(id).get();
            System.out.println("Usuario encontrado: ID " + id + ", Nombre: " + usuario.getName());

            // Eliminar código VIP asociado (si existe)
            Optional<CodigoVip> codigoVipOpt = codigoVipRepository.findByUsuarioId(id);
            if (codigoVipOpt.isPresent()) {
                try {
                    CodigoVip codigoVip = codigoVipOpt.get();
                    codigoVipRepository.delete(codigoVip);
                    System.out.println("Código VIP eliminado para usuario " + id);
                } catch (Exception e) {
                    String errorMsg = "Error al eliminar código VIP para usuario " + id + ": " + e.getClass().getSimpleName() + " - " + e.getMessage();
                    System.err.println(errorMsg);
                    // Continúa sin agregar a errors, ya que es opcional
                }
            }

            List<Rifa> rifas = rifaRepository.findByUsuarioId((long) id);
            System.out.println("Rifas encontradas para eliminar: " + rifas.size());
            int deletedRifas = 0;
            List<String> errors = new ArrayList<>();
            for (Rifa rifa : rifas) {
                try {
                    System.out.println("Eliminando rifa ID " + rifa.getId() + " (" + rifa.getNombre() + ")");
                    rifaService.eliminarRifa(rifa.getId());
                    deletedRifas++;
                    System.out.println("Rifa " + rifa.getId() + " eliminada exitosamente.");
                } catch (Exception e) {
                    String errorMsg = "Error al eliminar rifa " + rifa.getId() + ": " + e.getClass().getSimpleName() + " - " + e.getMessage();
                    System.err.println(errorMsg);
                    errors.add(errorMsg);
                }
            }

            usuarioRepository.delete(usuario);
            System.out.println("Usuario ID " + id + " eliminado.");
            response.put("message", "Usuario eliminado correctamente, incluyendo " + deletedRifas + " rifas asociadas.");
            if (!errors.isEmpty()) {
                response.put("warnings", errors);
            }
            return response;
        } catch (Exception e) {
            String errorMsg = "Error general al eliminar usuario " + id + ": " + e.getClass().getSimpleName() + " - " + e.getMessage();
            System.err.println(errorMsg);
            response.put("error", "Error interno al eliminar usuario: " + errorMsg);
            return response;
        }
    }*/

    @Transactional
    public Map<String, Object> eliminarUsuario(int id) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (!usuarioRepository.existsById(id)) {
                response.put("error", "Usuario no encontrado con ID: " + id);
                return response;
            }

            Usuario usuario = usuarioRepository.findById(id).get();
            System.out.println("Usuario encontrado: ID " + id + ", Nombre: " + usuario.getName());

            // Eliminar todos los códigos VIP asociados (múltiples si renovados)
            List<CodigoVip> codigoVipList = codigoVipRepository.findAllByUsuarioId(id);
            for (CodigoVip codigoVip : codigoVipList) {
                try {
                    codigoVipRepository.delete(codigoVip);
                    System.out.println("Código VIP eliminado para usuario " + id + ": " + codigoVip.getCodigo());
                } catch (Exception e) {
                    String errorMsg = "Error al eliminar código VIP " + codigoVip.getId() + " para usuario " + id + ": " + e.getMessage();
                    System.err.println(errorMsg);
                    // Continúa sin error total
                }
            }

            List<Rifa> rifas = rifaRepository.findByUsuarioId((long) id);
            System.out.println("Rifas encontradas para eliminar: " + rifas.size());
            int deletedRifas = 0;
            List<String> errors = new ArrayList<>();
            for (Rifa rifa : rifas) {
                try {
                    System.out.println("Eliminando rifa ID " + rifa.getId() + " (" + rifa.getNombre() + ")");
                    rifaService.eliminarRifa(rifa.getId());
                    deletedRifas++;
                    System.out.println("Rifa " + rifa.getId() + " eliminada exitosamente.");
                } catch (Exception e) {
                    String errorMsg = "Error al eliminar rifa " + rifa.getId() + ": " + e.getMessage();
                    System.err.println(errorMsg);
                    errors.add(errorMsg);
                }
            }

            usuarioRepository.delete(usuario);
            System.out.println("Usuario ID " + id + " eliminado.");
            response.put("message", "Usuario eliminado correctamente, incluyendo " + deletedRifas + " rifas y " + codigoVipList.size() + " códigos VIP.");
            if (!errors.isEmpty()) {
                response.put("warnings", errors);
            }
            return response;
        } catch (Exception e) {
            String errorMsg = "Error general al eliminar usuario " + id + ": " + e.getMessage();
            System.err.println(errorMsg);
            response.put("error", "Error interno al eliminar usuario: " + errorMsg);
            return response;
        }
    }

    public void decrementarCantidadRifas(int userId) {
        Usuario usuario = usuarioRepository.findById(userId).orElse(null);
        if (usuario != null && usuario.isEsVip() && usuario.getCantidadRifas() > 0) {
            usuario.setCantidadRifas(usuario.getCantidadRifas() - 1);
            usuarioRepository.save(usuario);
        }
    }


   /* public Usuario activarVip(int userId, String codigoIngresado) {
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

        // 🔹 Verificar VIP: Solo permitir nuevo código si cantidadRifas = 0 (cuenta vencida)
        if (usuario.isEsVip()) {
            if (usuario.getCantidadRifas() > 1) {
                throw new IllegalArgumentException("Este usuario ya tiene un código VIP activo con rifas restantes. Agote las rifas para renovar.");
            }
            // Si cantidadRifas = 1, permite renovación (resetea a nuevo código)
            System.out.println("Renovando código VIP vencido para usuario " + userId + " (rifas restantes: 0)");
        }

        // 🔥 Activar VIP
        usuario.setEsVip(true);
        usuario.setCodigoVip(codigoIngresado);
        usuario.setCantidadRifas(codigoVip.getCantidadRifas()); // Resetea rifas al nuevo código
        usuario.setFechaRegistro(ZonedDateTime.now());

        // 🔹 Marcar código como utilizado
        codigoVip.setUtilizado(true);
        codigoVip.setUsuarioAsignado(usuario);

        // 🔹 Guardar cambios
        usuarioRepository.save(usuario);
        codigoVipRepository.save(codigoVip);

        return usuario;
    }

    public Usuario activarVip1(int userId, String codigoIngresado) {
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

        // 🔹 Verificar VIP: Solo permitir nuevo código si cantidadRifas = 0 (cuenta vencida)
        if (usuario.isEsVip()) {
            if (usuario.getCantidadRifas() > 0) {
                throw new IllegalArgumentException("Este usuario ya tiene un código VIP activo con rifas restantes. Agote las rifas para renovar.");
            }
            // Si cantidadRifas = 0, permite renovación
            System.out.println("Renovando código VIP vencido para usuario " + userId + " (rifas restantes: 0)");
        }

        // 🔥 Activar VIP
        usuario.setEsVip(true);
        usuario.setCodigoVip(codigoIngresado);
        // 🔥 Suma rifas del nuevo código al actual (1 + 10 = 11 total)
        usuario.setCantidadRifas(usuario.getCantidadRifas() + codigoVip.getCantidadRifas());
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
