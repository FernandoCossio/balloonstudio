package com.decoraciones.features.ia;

import com.decoraciones.domain.models.ArticuloInventario;
import com.decoraciones.domain.models.ImagenArticulo;
import com.decoraciones.domain.enums.imagenarticulo.EstadoIa;
import com.decoraciones.features.inventario.ArticuloInventarioRepository;
import com.decoraciones.features.imagenarticulo.ImagenArticuloRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@Slf4j
public class RedisIaResultListener implements MessageListener {

    private final ArticuloInventarioRepository articuloRepository;
    private final ImagenArticuloRepository imagenRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RedisIaResultListener(ArticuloInventarioRepository articuloRepository,
                                 ImagenArticuloRepository imagenRepository) {
        this.articuloRepository = articuloRepository;
        this.imagenRepository = imagenRepository;
    }

    @Override
    @Transactional
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            log.info("Received Redis message from channel {}: {}", new String(message.getChannel()), body);

            // Parse body
            Map<String, Object> payload = objectMapper.readValue(body, Map.class);
            
            Number articuloIdNum = (Number) payload.get("articulo_id");
            Number imagenIdNum = (Number) payload.get("imagen_id");
            String estado = (String) payload.get("estado");
            String error = (String) payload.get("error");

            if (articuloIdNum == null) {
                log.warn("Missing 'articulo_id' in payload");
                return;
            }

            Long articuloId = articuloIdNum.longValue();

            articuloRepository.findById(articuloId).ifPresentOrElse(articulo -> {
                // Restore state of the article
                if ("PENDIENTE".equalsIgnoreCase(articulo.getEstado())) {
                    String nuevoEstado = (articulo.getStockTotal() != null && articulo.getStockTotal() <= 0) 
                            ? "STOCK_BAJO" : "DISPONIBLE";
                    articulo.setEstado(nuevoEstado);
                    articuloRepository.save(articulo);
                    log.info("Restored article ID {} state to {}", articuloId, nuevoEstado);
                }

                // If imagenId was specified, we can make sure its state is updated
                if (imagenIdNum != null) {
                    Long imagenId = imagenIdNum.longValue();
                    imagenRepository.findById(imagenId).ifPresent(imagen -> {
                        if ("PROCESADO".equalsIgnoreCase(estado)) {
                            imagen.setEstadoIa(EstadoIa.PROCESADO);
                        } else {
                            imagen.setEstadoIa(EstadoIa.FALLIDO);
                        }
                        imagenRepository.save(imagen);
                        log.info("Updated image ID {} stateIa to {}", imagenId, imagen.getEstadoIa());
                    });
                }
            }, () -> log.warn("Article ID {} not found in database", articuloId));

        } catch (IOException e) {
            log.error("Failed to parse Redis message", e);
        } catch (Exception e) {
            log.error("Error processing Redis message in listener", e);
        }
    }
}
