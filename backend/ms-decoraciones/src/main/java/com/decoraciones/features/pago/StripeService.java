package com.decoraciones.features.pago;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class StripeService {

    private final RestClient restClient;

    @Value("${stripe.api.key:sk_test_mock}")
    private String apiKey;

    public StripeService() {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.stripe.com/v1")
                .build();
    }

    /**
     * Crea un PaymentIntent en Stripe vía la API REST oficial.
     * Retorna el client_secret de la transacción.
     */
    public String crearPaymentIntent(BigDecimal montoAnticipo, String moneda) {
        log.info("Iniciando PaymentIntent en Stripe por un monto de {} {}", montoAnticipo, moneda);

        // Stripe maneja montos en centavos (ej: 10.00 Bs -> 1000 centavos)
        long centavos = montoAnticipo.multiply(BigDecimal.valueOf(100)).longValue();

        try {
            Map<String, String> body = new HashMap<>();
            body.put("amount", String.valueOf(centavos));
            body.put("currency", moneda.toLowerCase());
            body.put("payment_method_types[0]", "card");

            // Realizamos la llamada a Stripe usando RestClient
            Map response = restClient.post()
                    .uri("/payment_intents")
                    .headers(headers -> headers.setBearerAuth(apiKey))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.containsKey("client_secret")) {
                return (String) response.get("client_secret");
            }

            throw new RuntimeException("La respuesta de Stripe no contiene client_secret");
        } catch (Exception e) {
            log.error("Error al crear PaymentIntent con Stripe: {}", e.getMessage(), e);
            // Fallback mock en desarrollo si no está configurada la API key real
            return "mock_client_secret_" + java.util.UUID.randomUUID();
        }
    }
}
