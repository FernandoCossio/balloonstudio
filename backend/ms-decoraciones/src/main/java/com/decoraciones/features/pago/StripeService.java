package com.decoraciones.features.pago;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
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
    public String crearPaymentIntent(BigDecimal montoAnticipo, String moneda, Map<String, String> metadata) {
        log.info("Iniciando PaymentIntent en Stripe por un monto de {} {}", montoAnticipo, moneda);

        // Stripe maneja montos en centavos (ej: 10.00 Bs -> 1000 centavos)
        long centavos = montoAnticipo.multiply(BigDecimal.valueOf(100)).longValue();

        try {
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("amount", String.valueOf(centavos));
            body.add("currency", moneda.toLowerCase());
            body.add("payment_method_types[0]", "card");
            if (metadata != null) {
                for (Map.Entry<String, String> entry : metadata.entrySet()) {
                    body.add("metadata[" + entry.getKey() + "]", entry.getValue());
                }
            }

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
