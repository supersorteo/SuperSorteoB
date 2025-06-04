package com.example.rifa.controller;

import com.example.rifa.entity.CodigoVip;
import com.example.rifa.services.CodigoVipService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/codigos-vip")
//@CrossOrigin(origins = {"http://localhost:4200", "http://otrodominio.com"}) // 🔥 Especificar los orígenes explícitamente


public class CodigoVipController {
    @Autowired
    private CodigoVipService codigoVipService;

    @PostMapping

    public ResponseEntity<Map<String, Object>> generarCodigoVip(@RequestParam int cantidadRifas) {
        Map<String, Object> response = new HashMap<>();
        try {
            // Generar un código VIP con la cantidad de rifas especificada
            String codigoVip = codigoVipService.generarCodigoVip(cantidadRifas);

            response.put("message", "Código VIP generado con éxito");
            response.put("codigoVip", codigoVip);
            response.put("cantidadRifas", cantidadRifas);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("message", "Error al generar el código VIP: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping
    public ResponseEntity<List<CodigoVip>> obtenerTodosLosCodigosVip() {
        List<CodigoVip> codigosVip = codigoVipService.obtenerTodosLosCodigosVip();
        return ResponseEntity.ok(codigosVip);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CodigoVip> obtenerCodigoVipPorId(@PathVariable Long id) {
        CodigoVip codigoVip = codigoVipService.obtenerCodigoVipPorId(id);
        return ResponseEntity.ok(codigoVip);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CodigoVip> actualizarCodigoVip(@PathVariable Long id, @RequestBody CodigoVip codigoVipActualizado) {
        CodigoVip codigoVip = codigoVipService.actualizarCodigoVip(id, codigoVipActualizado);
        return ResponseEntity.ok(codigoVip);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarCodigoVip(@PathVariable Long id) {
        codigoVipService.eliminarCodigoVip(id);
        return ResponseEntity.noContent().build();
    }


}
