package com.example.rifa.controller;

import com.example.rifa.entity.PaymentOption;
import com.example.rifa.services.PaymentOptionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payment-options")
@Validated
public class PaymentOptionController {

    @Autowired
    private PaymentOptionService service;

    @PostMapping
    public ResponseEntity<PaymentOption> createPaymentOption(@Valid @RequestBody PaymentOption paymentOption) {
        return ResponseEntity.ok(service.save(paymentOption));
    }

    @GetMapping
    public ResponseEntity<List<PaymentOption>> getAllPaymentOptions() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/bank/{bankCode}")
    public ResponseEntity<PaymentOption> getPaymentOptionByBankCode(@PathVariable String bankCode) {
        return service.findByBankCode(bankCode)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("{id}")
    public ResponseEntity<PaymentOption> getPaymentOptionById(@PathVariable Long id) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<PaymentOption> updatePaymentOption(@PathVariable Long id, @Valid @RequestBody PaymentOption updatedOption) {
        try {
            return ResponseEntity.ok(service.update(id, updatedOption));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePaymentOption(@PathVariable Long id) {
        try {
            service.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<PaymentOption>> getPaymentOptionsByUsuarioId(@PathVariable Long usuarioId) { // Cambiado de /creator
        return ResponseEntity.ok(service.findByUsuarioId(usuarioId));
    }

    @PostMapping("/create-preference")
    public ResponseEntity<Map<String, Object>> createPreference0(@RequestParam Long rifaId, @RequestParam BigDecimal amount) {
        String url = "https://api.mercadopago.com/checkout/preferences";
        RestTemplate restTemplate = new RestTemplate();

        Map<String, Object> item = Map.of(
                "title", "Rifa ID " + rifaId,
                "quantity", 1,
                "unit_price", amount,
                "currency_id", "ARS"
        );

        Map<String, Object> body = Map.of(
                "items", List.of(item),
                "back_urls", Map.of(
                        "success", "http://localhost:4200/success",
                        "failure", "http://localhost:4200/failure",
                        "pending", "http://localhost:4200/pending"
                ),
                "auto_return", "approved"
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("TU_ACCESS_TOKEN");
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);

        return ResponseEntity.ok(response);
    }


   /* @PostMapping("/preference")
    public ResponseEntity<Map<String, Object>> createPreference(@RequestBody Map<String, Object> request) {
        try {
            Long rifaId = Long.valueOf((String) request.get("rifaId"));
            BigDecimal amount = new BigDecimal((Double) request.get("amount"));
            Integer usuarioId = Integer.valueOf((String) request.get("usuarioId"));
            Map<String, Object> response = service.createPaymentPreference(rifaId, amount, usuarioId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Error al crear preferencia MP: " + e.getMessage()));
        }
    }*/

    @PostMapping("/preference")
    public ResponseEntity<Map<String, Object>> createPreference(@RequestBody Map<String, Object> request) {
        try {
            Object rifaIdObj = request.get("rifaId");
            Object amountObj = request.get("amount");
            Object usuarioIdObj = request.get("usuarioId");

            if (rifaIdObj == null || amountObj == null || usuarioIdObj == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "rifaId, amount y usuarioId son obligatorios"));
            }

            Long rifaId = Long.valueOf(rifaIdObj.toString());
            BigDecimal amount = new BigDecimal(amountObj.toString());
            Integer usuarioId = Integer.valueOf(usuarioIdObj.toString());

            Map<String, Object> response = service.createPaymentPreference(rifaId, amount, usuarioId);
            return ResponseEntity.ok(response);
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "rifaId, amount o usuarioId inválido: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Error al crear preferencia MP: " + e.getMessage()));
        }
    }
    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(@RequestBody String body) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> notification = mapper.readValue(body, Map.class);
            service.handlePaymentWebhook(notification);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            System.err.println("Error en webhook MP: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

   /* @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(@RequestBody String body) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> notification = mapper.readValue(body, Map.class);
            service.handlePaymentWebhook(notification);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            System.err.println("Error en webhook MP: " + e.getMessage());
            return ResponseEntity.ok().build(); // Siempre 200 para MP
        }
    }*/
}
