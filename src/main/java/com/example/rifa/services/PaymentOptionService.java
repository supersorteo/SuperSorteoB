package com.example.rifa.services;

import com.example.rifa.entity.PaymentOption;
import com.example.rifa.repository.PaymentOptionRepository;
import com.example.rifa.repository.UsuarioRepository;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.resources.payment.Payment;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PaymentOptionService {
    @Autowired
    private PaymentOptionRepository repository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RifaService rifaService;

    @Autowired
    private RestTemplate restTemplate;

    // Autowired ObjectMapper
    @Autowired
    private ObjectMapper objectMapper;

    @Value("${mp.access.token}")


    private String mpAccessToken;
    /*
    public PaymentOption save(@Valid PaymentOption paymentOption) {
        if (paymentOption.getUsuarioId() == null) {
            throw new IllegalArgumentException("El ID del usuario es obligatorio para crear un método de pago.");
        }
        return repository.save(paymentOption);
    }*/

    public PaymentOption save(@Valid PaymentOption paymentOption) {
        if (paymentOption.getUsuarioId() == null) {
            throw new IllegalArgumentException("El ID del usuario es obligatorio para crear un método de pago.");
        }
        // Si cbu es null o vacío, asigna un valor por defecto (puedes ajustarlo según tus necesidades)
        if (paymentOption.getCbu() == null || paymentOption.getCbu().trim().isEmpty()) {
            paymentOption.setCbu("0000000000000000000000"); // 22 ceros como valor por defecto
        }
        return repository.save(paymentOption);
    }



    public PaymentOption savePaymentOption(PaymentOption paymentOption) {
        if (paymentOption.getUsuarioId() == null) {
            throw new IllegalArgumentException("El ID del usuario es obligatorio para crear un método de pago.");
        }
        // Valida CBU si presente
        if (paymentOption.getCbu() != null && !paymentOption.getCbu().matches("\\d{22}")) {
            throw new IllegalArgumentException("CBU debe tener exactamente 22 dígitos.");
        }
        // Si MP, set mpPaymentId null inicial
        if ("MP".equals(paymentOption.getBankCode())) {
            paymentOption.setMpPaymentId(null); // Se setea post-pago
        }
        return repository.save(paymentOption);
    }


    public List<PaymentOption> findAll() {
        return repository.findAll();
    }

    public Optional<PaymentOption> findByBankCode(String bankCode) {
        return repository.findByBankCode(bankCode);
    }

    public Optional<PaymentOption> findById(Long id) {
        return repository.findById(id);
    }

    public PaymentOption update(Long id, @Valid PaymentOption updatedOption) {
        return repository.findById(id)
                .map(existingOption -> {
                    existingOption.setBankCode(updatedOption.getBankCode());
                    existingOption.setAlias(updatedOption.getAlias());
                    existingOption.setCbu(updatedOption.getCbu());
                    if (updatedOption.getUsuarioId() != null) {
                        existingOption.setUsuarioId(updatedOption.getUsuarioId());
                    }
                    return repository.save(existingOption);
                })
                .orElseThrow(() -> new RuntimeException("Opción de pago no encontrada con id " + id));
    }


    public void deleteById(Long id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("Opción de pago no encontrada con id " + id);
        }
        repository.deleteById(id);
    }

    public List<PaymentOption> findByUsuarioId(Long usuarioId) {
        return repository.findByUsuarioId(usuarioId);
    }

    /*
    public String mercado(){
        PreferenceItemRequest.builder()
                .id("1234")
                .title("Games")
                .description("PS5")
                .pictureUrl("http://picture.com/PS5")
                .categoryId("games")
                .quantity(2)
                .currencyId("BRL")
                .unitPrice(new BigDecimal( val: "4000"))
            .build();

        List<PreferenceItemRequest> items = new ArrayList<>();
        items.add(itemRequest);
        PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                .items(items).build();
        PreferenceClient client = new PreferenceClient();
        Preference preference = client.create(request);

        return null;
    }
*/


    public Map<String, Object> createPaymentPreference0(Long rifaId, BigDecimal amount, Integer usuarioId) {
        try {
            String url = "https://api.mercadopago.com/checkout/preferences"; // Sandbox/prod
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(mpAccessToken);

            // Body JSON (full, con HTTPS back_urls)
            Map<String, Object> body = new HashMap<>();
            body.put("items", List.of(Map.of(
                    "title", "Rifa ID " + rifaId,
                    "quantity", 1,
                    "unit_price", amount.doubleValue(),
                    "currency_id", "ARS"
            )));
            body.put("back_urls", Map.of(
                    "success", "https://tu-ngrok-url.ngrok.io/success", // Usa ngrok para HTTPS
                    "failure", "https://tu-ngrok-url.ngrok.io/failure",
                    "pending", "https://tu-ngrok-url.ngrok.io/pending"
            ));
            body.put("auto_return", "approved");
            body.put("metadata", Map.of(
                    "rifaId", rifaId.toString(),
                    "usuarioId", usuarioId.toString()
            ));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> result = new HashMap<>();
                result.put("id", response.getBody().get("id"));
                result.put("initPoint", response.getBody().get("init_point"));
                return result;
            } else {
                throw new RuntimeException("Error MP: " + response.getStatusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("Error al crear preferencia MP: " + e.getMessage());
        }
    }

    public Map<String, Object> createPaymentPreference(Long rifaId, BigDecimal amount, Integer usuarioId) {
        try {
            String url = "https://api.mercadopago.com/checkout/preferences";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(mpAccessToken);

            Map<String, Object> body = new HashMap<>();
            body.put("items", List.of(Map.of(
                    "title", "Rifa ID " + rifaId,
                    "quantity", 1,
                    "unit_price", amount.doubleValue(),
                    "currency_id", "ARS"
            )));
            body.put("back_urls", Map.of(
                    "success", "http://localhost:4200/success",
                    "failure", "http://localhost:4200/failure",
                    "pending", "http://localhost:4200/pending"
            ));
            body.put("auto_return", "approved");
            body.put("metadata", Map.of(
                    "rifaId", rifaId.toString(),
                    "usuarioId", usuarioId.toString()
            ));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            // 🔥 Fix: 201 CREATED es éxito (MP usa 201 para preferences)
            if (response.getStatusCode().value() == 201 || response.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> result = new HashMap<>();
                result.put("id", response.getBody().get("id"));
                result.put("initPoint", response.getBody().get("init_point"));
                return result;
            } else {
                throw new RuntimeException("Error MP: " + response.getStatusCode() + " - " + response.getBody());
            }
        } catch (Exception e) {
            throw new RuntimeException("Error al crear preferencia MP: " + e.getMessage());
        }
    }

    public void handlePaymentWebhook(Map<String, Object> notification) {
        if ("payment".equals(notification.get("type"))) {
            Map<String, String> data = (Map<String, String>) notification.get("data");
            String dataId = data.get("id");
            if (dataId == null) {
                System.err.println("Webhook MP: dataId null, ignorando");
                return;
            }
            try {
                MercadoPagoConfig config = new MercadoPagoConfig(); // Fix: args token, sandbox
                PaymentClient client = new PaymentClient();
                Payment payment = client.get(Long.valueOf(dataId)); // Casting String to Long
                if ("approved".equals(payment.getStatus())) {
                    Map<String, Object> metadata = payment.getMetadata(); // Map de metadata
                    String rifaId = (String) metadata.get("rifaId");
                    String usuarioIdStr = (String) metadata.get("usuarioId");
                    if (rifaId != null && usuarioIdStr != null) {
                        Long usuarioId = Long.valueOf(usuarioIdStr); // Casting correcto
                        // Actualiza rifa (asigna números comprados, notifica)
                        rifaService.actualizarPagoRifa(rifaId, dataId);
                        // Guarda en PaymentOption (set mpPaymentId)
                        List<PaymentOption> options = repository.findByUsuarioId(usuarioId); // Fix: List, no Optional
                        if (!options.isEmpty()) {
                            PaymentOption option = options.get(0); // Toma primera (o itera si múltiples)
                            option.setMpPaymentId(dataId);
                            repository.save(option);
                            System.out.println("Webhook MP: Pago confirmado y actualizado para rifa " + rifaId + ", usuario " + usuarioId);
                        } else {
                            System.err.println("Webhook MP: PaymentOption no encontrada para usuario " + usuarioId);
                        }
                    } else {
                        System.err.println("Webhook MP: Metadata rifaId/usuarioId no encontrada");
                    }
                } else {
                    System.out.println("Webhook MP: Pago no aprobado, status: " + payment.getStatus());
                }
            } catch (NumberFormatException e) {
                System.err.println("Webhook MP: dataId inválido para Long: " + dataId);
            } catch (Exception e) {
                System.err.println("Error en webhook MP: " + e.getMessage());
            }
        } else {
            System.out.println("Webhook MP: Tipo no payment, ignorando: " + notification.get("type"));
        }
    }

}
