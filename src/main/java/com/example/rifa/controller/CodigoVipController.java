package com.example.rifa.controller;

import com.example.rifa.entity.CodigoVip;
import com.example.rifa.services.CodigoVipService;
import com.example.rifa.services.UsuarioService;
import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.exceptions.MPApiException;
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

    @Autowired
    private UsuarioService usuarioService;





    @PostMapping
    public ResponseEntity<CodigoVip> generarCodigoVip(@RequestParam int cantidadRifas) {
        try {
            CodigoVip codigoVip = codigoVipService.generarCodigoVip(cantidadRifas);
            return ResponseEntity.ok(codigoVip); // Retornamos el objeto con ID, código, cantidadRifas, utilizado
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }



    /*@PostMapping("/pago")
    public ResponseEntity<Map<String, String>> generarPreferenciaPago0(@RequestBody Map<String, Object> payload) {
        try {
            // ✅ Configurar el token de acceso
            //prueba
           // MercadoPagoConfig.setAccessToken("APP_USR-2830553727018436-111212-36bcd222790027ee0e3220aa5a01701f-2392507839");

            //produccion
            MercadoPagoConfig.setAccessToken("APP_USR-5214207697296450-102008-8f7211acae008977d555578fd793ed5e-57269258");

            // ✅ Extraer datos del payload
            int cantidadRifas = Integer.parseInt(payload.get("cantidadRifas").toString());
            int usuarioId = Integer.parseInt(payload.get("usuarioId").toString());

            System.out.println("🔍 Datos recibidos:");
            System.out.println("Cantidad de rifas: " + cantidadRifas);
            System.out.println("ID del usuario: " + usuarioId);

            // ✅ Generar código VIP
            CodigoVip codigoVip = codigoVipService.generarCodigoVip(cantidadRifas);
            System.out.println("🎟️ Código generado: " + codigoVip.getCodigo());
            System.out.println("💰 Precio asignado: " + codigoVip.getPrecio());

            // ✅ Validar precio
            if (codigoVip.getPrecio() <= 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "Precio inválido para la cantidad de rifas."));
            }

            // ✅ Crear ítem de preferencia
            PreferenceItemRequest item = PreferenceItemRequest.builder()
                    .title("Código VIP - " + cantidadRifas + " rifas")
                    .quantity(1)
                    .unitPrice(BigDecimal.valueOf(codigoVip.getPrecio()))
                    .currencyId("ARS")
                    .build();

            // ✅ URLs de retorno
            PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                    .success("https://supersorteo-5f1f3.web.app/success")
                    .failure("https://supersorteo-5f1f3.web.app/failure")
                    .pending("https://supersorteo-5f1f3.web.app/pending")
                    .build();

            // ✅ Crear preferencia
            PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                    .items(List.of(item))
                    .backUrls(backUrls)
                    .autoReturn("approved")
                    .build();

            Preference preference = new PreferenceClient().create(preferenceRequest);

            System.out.println("✅ Preferencia creada - ID: " + preference.getId());
            System.out.println("🔗 Init Point: " + preference.getInitPoint());

            Map<String, String> response = new HashMap<>();
            response.put("initPoint", preference.getInitPoint());
            response.put("id", preference.getId());
            response.put("precio", String.valueOf(codigoVip.getPrecio()));

            return ResponseEntity.ok(response);

        } catch (MPApiException e) {
            System.out.println("❌ MPApiException:");
            System.out.println("Status Code: " + e.getStatusCode());
            System.out.println("Content: " + e.getApiResponse().getContent());
            System.out.println("Cause: " + e.getCause());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error Mercado Pago: " + e.getMessage()));
        } catch (Exception e) {
            System.out.println("❌ Error general: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error inesperado: " + e.getMessage()));
        }
    }*/

    @PostMapping("/pago")
    public ResponseEntity<Map<String, String>> generarPreferenciaPago(@RequestBody Map<String, Object> payload) {
        try {
            MercadoPagoConfig.setAccessToken("APP_USR-5214207697296450-102008-8f7211acae008977d555578fd793ed5e-57269258");
            int cantidadRifas = Integer.parseInt(payload.get("cantidadRifas").toString());
            int usuarioId = Integer.parseInt(payload.get("usuarioId").toString());

            System.out.println("🔍 Datos recibidos:");
            System.out.println("Cantidad de rifas: " + cantidadRifas);
            System.out.println("ID del usuario: " + usuarioId);


            // Genera código VIP
            CodigoVip codigoVip = codigoVipService.generarCodigoVip(cantidadRifas);
            System.out.println("🎟️ Código generado: " + codigoVip.getCodigo());
            System.out.println("💰 Precio asignado: " + codigoVip.getPrecio());
            System.out.println("💰 Cantidad de rifas: " + codigoVip.getCantidadRifas());


            // Asigna VIP al usuario automáticamente
            usuarioService.activarVip(usuarioId, codigoVip.getCodigo()); // Tu método existente

            // Crea preference MP
            PreferenceItemRequest item = PreferenceItemRequest.builder()
                    .title("Código VIP - " + cantidadRifas + " rifas")
                    .quantity(1)
                    .unitPrice(BigDecimal.valueOf(codigoVip.getPrecio()))
                    .currencyId("ARS")
                    .build();

            PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                    .success("https://supersorteo-5f1f3.web.app/success?usuarioId=" + usuarioId) // Pasa usuarioId
                    .failure("https://supersorteo-5f1f3.web.app/failure")
                    .pending("https://supersorteo-5f1f3.web.app/pending")
                    .build();

            PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                    .items(List.of(item))
                    .backUrls(backUrls)
                    .autoReturn("approved")
                    .metadata(Map.of("usuarioId", String.valueOf(usuarioId)))
                    .build();

            Preference preference = new PreferenceClient().create(preferenceRequest);

            Map<String, String> response = new HashMap<>();
            response.put("initPoint", preference.getInitPoint());
            response.put("id", preference.getId());
            response.put("precio", String.valueOf(codigoVip.getPrecio()));
            response.put("codigoVip", codigoVip.getCodigo()); // Opcional: Frontend muestra código
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error inesperado: " + e.getMessage()));
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

    @GetMapping("/vendidos")
    public ResponseEntity<List<CodigoVip>> getSoldCodesWithUserDetails() {
        List<CodigoVip> soldCodes = codigoVipService.getSoldCodesWithUserDetails();
        return ResponseEntity.ok(soldCodes);
    }


}
