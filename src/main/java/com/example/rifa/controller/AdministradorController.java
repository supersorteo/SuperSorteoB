package com.example.rifa.controller;

import com.example.rifa.entity.Administrador;
import com.example.rifa.services.AdministradorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin")
public class AdministradorController {

    @Autowired
    private AdministradorService adminService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> payload) {
        String username = payload.get("username");
        String password = payload.get("password");

        if (adminService.validarCredenciales(username, password)) {
            return ResponseEntity.ok().body(Map.of("message", "Autenticado correctamente."));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario o contraseña incorrectos.");
        }
    }
/*
    @PutMapping("/password")
    public ResponseEntity<?> cambiarPassword(@RequestBody Map<String, String> payload) {
        String username = payload.get("username");
        String newPassword = payload.get("newPassword");

        if (!adminService.existeAdministrador(username)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Administrador no encontrado.");
        }

        boolean actualizado = adminService.cambiarPassword(username, newPassword);
        if (actualizado) {
            //return ResponseEntity.ok().body("Contraseña actualizada.");
            return ResponseEntity.ok().body(Map.of("message", "Contraseña actualizada."));

        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("No se pudo actualizar.");
        }
    }*/

    @PutMapping("/password")
    public ResponseEntity<?> cambiarPassword(@RequestBody Map<String, String> payload) {
        String username = payload.get("username");
        String newPassword = payload.get("newPassword");

        if (!adminService.existeAdministrador(username)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "Administrador no encontrado."));
        }

        boolean actualizado = adminService.cambiarPassword(username, newPassword);
        if (actualizado) {
            return ResponseEntity.ok(Map.of("success", true, "message", "Contraseña actualizada."));
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "No se pudo actualizar."));
        }
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Administrador admin) {
        if (adminService.existeAdministrador(admin.getUsername())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("El usuario ya existe.");
        }
        return ResponseEntity.ok(adminService.crear(admin));
    }

    // ✅ Obtener todos los administradores
    @GetMapping
    public List<Administrador> obtenerTodos() {
        return adminService.obtenerTodos();
    }



    @GetMapping("/buscar/{username}")
    public ResponseEntity<Object> obtenerPorUsername(@PathVariable String username) {
        Optional<Administrador> admin = adminService.obtenerPorUsername(username);

        if (admin.isPresent()) {
            return ResponseEntity.ok(admin.get());
        } else {
            Map<String, Object> error = new HashMap<>();
            error.put("status", 404);
            error.put("message", "Administrador no encontrado");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }




    // ✅ Eliminar administrador
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        if (adminService.eliminar(id)) {
            return ResponseEntity.ok("Administrador eliminado.");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No encontrado.");
        }
    }

}
