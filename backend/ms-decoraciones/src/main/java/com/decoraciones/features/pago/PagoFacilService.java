package com.decoraciones.features.pago;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class PagoFacilService {

    private final RestClient restClient;

    @Value("${pagofacil.base-url}")
    private String baseUrl;

    @Value("${pagofacil.token-service}")
    private String tokenService;

    @Value("${pagofacil.token-secret}")
    private String tokenSecret;

    @Value("${pagofacil.callback-url}")
    private String callbackUrl;

    public PagoFacilService() {
       this.restClient = RestClient.builder().build();
    }

    public record PagoFacilQrResponse(
            String transactionId,
            String qrUrl,
            String qrBase64
    ) {}

    /**
     * Inicia sesión en la API de PagoFácil para obtener un token JWT.
     */
    public String login() {
        log.info("Iniciando sesión en PagoFácil en url: {}", baseUrl);
        try {
            Map response = restClient.post()
                    .uri(baseUrl + "/login")
                    .header("Content-Type", "application/json")
                    .header("tcTokenService", tokenService)
                    .header("tcTokenSecret", tokenSecret)
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.containsKey("values")) {
                Map values = (Map) response.get("values");
                if (values != null && values.containsKey("accessToken")) {
                    return (String) values.get("accessToken");
                }
            }
            throw new RuntimeException("La respuesta de login de PagoFacil no contiene accessToken en values. Body: " + response);
        } catch (Exception e) {
            log.error("Error al iniciar sesión en PagoFácil: {}", e.getMessage(), e);
            throw new RuntimeException("Error en autenticación PagoFácil: " + e.getMessage());
        }
    }

    public PagoFacilQrResponse generarQR(
            String token,
            String orderId,
            double amount,
            String clientName,
            String clientCi,
            String email,
            String phone
    ) {
        log.info("Generando QR en PagoFácil para pedido ID: {}, monto: {}", orderId, amount);

        String safePhone = (phone != null && !phone.trim().isEmpty()) ? phone.trim() : "75540850";
        String safeEmail = (email != null && !email.trim().isEmpty()) ? email.trim() : "cliente@correo.com";

        Map<String, Object> body = new HashMap<>();
        body.put("paymentMethod", 34); // Método 34: QR Master
        body.put("clientName", clientName);
        body.put("documentType", 1); // 1: CI
        body.put("documentId", clientCi);
        body.put("phoneNumber", safePhone);
        body.put("email", safeEmail);
        body.put("paymentNumber", orderId);
        body.put("amount", amount);
        body.put("currency", 2); // 2: Bolivianos (BOB)
        body.put("clientCode", clientCi);
        body.put("callbackUrl", callbackUrl);

        Map<String, Object> detail = new HashMap<>();
        detail.put("serial", 1);
        detail.put("product", "Reserva Decoracion Venta " + orderId);
        detail.put("quantity", 1);
        detail.put("price", amount);
        detail.put("discount", 0);
        detail.put("total", amount);
        body.put("orderDetail", List.of(detail));

        try {
            String jsonBody = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(body);
            log.info("Cuerpo enviado a PagoFácil: {}", jsonBody);

            Map response = restClient.post()
                    .uri(baseUrl + "/generate-qr")
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .body(jsonBody)
                    .retrieve()
                    .body(Map.class);

            if (response == null) {
                throw new RuntimeException("Respuesta vacía de PagoFácil al generar QR");
            }

            log.debug("Respuesta de PagoFacil: {}", response);

            if (!response.containsKey("values")) {
                throw new RuntimeException("La respuesta de PagoFácil no contiene el bloque 'values'. Body: " + response);
            }

            Map values = (Map) response.get("values");
            if (values == null) {
                throw new RuntimeException("El bloque 'values' en la respuesta de PagoFácil está vacío.");
            }

            String transactionId = null;
            if (values.get("transactionId") != null) {
                transactionId = String.valueOf(values.get("transactionId"));
            }

            String checkoutUrl = (String) values.get("checkoutUrl");
            String qrContentUrl = (String) values.get("qrContentUrl");
            String qrBase64 = (String) values.get("qrBase64");

            if (checkoutUrl != null) checkoutUrl = checkoutUrl.replace("\\/", "/");
            if (qrContentUrl != null) qrContentUrl = qrContentUrl.replace("\\/", "/");
            if (qrBase64 != null) qrBase64 = qrBase64.replace("\\/", "/");

            String finalQrUrl = qrContentUrl != null ? qrContentUrl : (checkoutUrl != null ? checkoutUrl : "");

            return new PagoFacilQrResponse(transactionId, finalQrUrl, qrBase64);
        } catch (Exception e) {
            log.error("Error al generar QR en PagoFácil: {}", e.getMessage(), e);
            throw new RuntimeException("Error al generar QR en PagoFácil: " + e.getMessage());
        }
    }

    public boolean consultarTransaccion(String token, String transactionId) {
        log.info("Consultando transacción en PagoFácil con ID: {}", transactionId);
        
        Map<String, Object> body = new HashMap<>();
        try {
            body.put("pagofacilTransactionId", Long.parseLong(transactionId));
        } catch (NumberFormatException e) {
            body.put("pagofacilTransactionId", transactionId);
        }

        try {
            String jsonBody = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(body);
            Map response = restClient.post()
                    .uri(baseUrl + "/query-transaction")
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .body(jsonBody)
                    .retrieve()
                    .body(Map.class);

            if (response == null) {
                throw new RuntimeException("Respuesta vacía al consultar transacción");
            }

            log.info("Respuesta de consulta de transacción PagoFacil: {}", response);

            String paymentStatus = null;
            if (response.containsKey("values") && response.get("values") instanceof Map) {
                Map values = (Map) response.get("values");
                if (values != null && values.get("paymentStatus") != null) {
                    paymentStatus = String.valueOf(values.get("paymentStatus"));
                }
            }
            if (paymentStatus == null && response.get("paymentStatus") != null) {
                paymentStatus = String.valueOf(response.get("paymentStatus"));
            }

            return "2".equals(paymentStatus) || "Pagado".equalsIgnoreCase(paymentStatus)
                    || "5".equals(paymentStatus) || "Revisión".equalsIgnoreCase(paymentStatus);
        } catch (Exception e) {
            log.error("Error al consultar transacción en PagoFácil: {}", e.getMessage(), e);
            return false;
        }
    }
}
