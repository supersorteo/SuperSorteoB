package com.example.rifa.controller;

import com.example.rifa.entity.CodigoVip;
import com.example.rifa.services.CodigoVipService;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.resources.preference.Preference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
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
    public ResponseEntity<CodigoVip> generarCodigoVip(@RequestParam int cantidadRifas) {
        try {
            CodigoVip codigoVip = codigoVipService.generarCodigoVip(cantidadRifas);
            return ResponseEntity.ok(codigoVip); // Retornamos el objeto con ID, código, cantidadRifas, utilizado
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }


    @PostMapping("/pago")
    public ResponseEntity<Map<String, String>> generarPreferenciaPago(@RequestBody Map<String, Object> payload) {
        try {
            int cantidadRifas = Integer.parseInt(payload.get("cantidadRifas").toString());
            int usuarioId = Integer.parseInt(payload.get("usuarioId").toString());

            // Generar código VIP
            CodigoVip codigoVip = codigoVipService.generarCodigoVip(cantidadRifas);

            // Crear preferencia Mercado Pago
            PreferenceItemRequest item =
                    PreferenceItemRequest.builder()
                            .title("Código VIP - " + cantidadRifas + " rifas")
                            .quantity(1)
                            .unitPrice(BigDecimal.valueOf((float) codigoVip.getPrecio()))
                            .currencyId("ARS")
                            .build();

            PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                    .success("https://supersorteo-5f1f3.web.app/success")
                    .failure("https://supersorteo-5f1f3.web.app/failure")
                    .pending("https://supersorteo-5f1f3.web.app/pending")
                    .build();

            PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                    .items(List.of(item))
                    .backUrls(backUrls)
                    .autoReturn("approved")
                    .build();

            Preference preference = new PreferenceClient().create(preferenceRequest);

            Map<String, String> response = new HashMap<>();
            response.put("initPoint", preference.getInitPoint());
            response.put("id", preference.getId());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al generar preferencia: " + e.getMessage()));
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
