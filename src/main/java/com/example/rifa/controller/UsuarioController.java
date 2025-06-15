package com.example.rifa.controller;

import com.example.rifa.entity.CodigoVip;
import com.example.rifa.entity.Usuario;
import com.example.rifa.repository.UsuarioRepository;
import com.example.rifa.services.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/usuarios")
//@CrossOrigin(origins = {"http://localhost:4200", "http://otrodominio.com"}) // 🔥 Especificar los orígenes explícitamente



public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;



    @GetMapping
    public List<Usuario> obtenerTodosLosUsuarios() {
        return usuarioService.obtenerTodosLosUsuarios();
    }

    @GetMapping("/{id}")
    public Usuario obtenerUsuarioPorId(@PathVariable int id) {
        return usuarioService.obtenerUsuarioPorId(id);
    }

    @PostMapping
    public ResponseEntity<?> registrarUsuario(@RequestBody Usuario usuario) {
        try {
            Usuario nuevoUsuario = usuarioService.registrarUsuario(usuario);
            return ResponseEntity.ok(nuevoUsuario);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }




    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody Usuario usuario) {
        Optional<Usuario> foundUser = usuarioService.findUserByEmail(usuario.getEmail());

        if (foundUser.isPresent()) {
            Usuario existingUser = foundUser.get();

            if (existingUser.getPassword().equals(usuario.getPassword())) {
                // 🔥 Si es su primer login, actualizar `primeraVez = false`
                boolean primerLogin = existingUser.isPrimeraVez();
                if (primerLogin) {
                    existingUser.setPrimeraVez(false);
                    usuarioService.actualizarUsuario(existingUser.getId(), existingUser);
                }

                Map<String, Object> response = new HashMap<>();
                response.put("usuario", existingUser);
                response.put("primerInicioSesion", primerLogin);

                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Contraseña incorrecta");
            }
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Usuario no registrado");
        }
    }



    @PostMapping("/recuperar-password")
    public String recuperarPassword(@RequestParam String email) {
        return usuarioService.generarCodigoRecuperacion(email);
    }

    @PostMapping("/cambiar-password")
    public boolean cambiarPassword(@RequestParam String email, @RequestParam String codigo, @RequestParam String nuevaPassword) {
        return usuarioService.cambiarPassword(email, codigo, nuevaPassword);
    }

    @PutMapping("/{id}")
    public Usuario actualizarUsuario(@PathVariable int id, @RequestBody Usuario usuarioActualizado) {
        return usuarioService.actualizarUsuario(id, usuarioActualizado);
    }

    @DeleteMapping("/{id}")
    public String eliminarUsuario(@PathVariable int id) {
        return usuarioService.eliminarUsuario(id);
    }





    @PutMapping("/{id}/activar-vip")
    public ResponseEntity<?> activarVip(@PathVariable int id, @RequestBody Map<String, String> body) {
        String codigoVip = body.get("codigoVip");
        if (codigoVip == null || codigoVip.isEmpty()) {
            return ResponseEntity.badRequest().body("Código VIP es requerido");
        }

        Usuario usuarioActualizado = usuarioService.activarVip(id, codigoVip);
        if (usuarioActualizado == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Código VIP inválido o ya utilizado");
        }

        return ResponseEntity.ok(usuarioActualizado);
    }






}
