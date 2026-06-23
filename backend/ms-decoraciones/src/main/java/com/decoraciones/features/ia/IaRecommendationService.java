package com.decoraciones.features.ia;

import com.decoraciones.common.errors.ArticuloInventarioNoEncontradoException;
import com.decoraciones.domain.dtos.articuloinventario.ArticuloInventarioResponse;
import com.decoraciones.domain.models.ArticuloInventario;
import com.decoraciones.domain.models.ImagenArticulo;
import com.decoraciones.domain.enums.imagenarticulo.EstadoIa;
import com.decoraciones.features.inventario.ArticuloInventarioMapper;
import com.decoraciones.features.inventario.ArticuloInventarioRepository;
import com.decoraciones.features.imagenarticulo.ImagenArticuloRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class IaRecommendationService {

    private final RestClient restClient;
    private final ArticuloInventarioRepository articuloRepository;
    private final ImagenArticuloRepository imagenRepository;
    private final ArticuloInventarioMapper articuloMapper;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    public IaRecommendationService(
            @Value("${app.ia.url}") String iaUrl,
            ArticuloInventarioRepository articuloRepository,
            ImagenArticuloRepository imagenRepository,
            ArticuloInventarioMapper articuloMapper) {
        this.restClient = RestClient.builder()
                .baseUrl(iaUrl)
                .requestFactory(new SimpleClientHttpRequestFactory())
                .build();
        this.articuloRepository = articuloRepository;
        this.imagenRepository = imagenRepository;
        this.articuloMapper = articuloMapper;
    }

    // Records for mapping IA response
    public record IaRecommendItem(Long id, String nombre, String tipo_articulo, Integer stock_total, Double score) {}
    public record IaRecommendData(List<IaRecommendItem> items, String query_type, Integer total_resultados, String model_version, Double tiempo_ms) {}
    public record IaResponseWrapper(String status, IaRecommendData data, String message) {}

    public record IaReprocessResponse(Long articulo_id, String estado, String mensaje) {}
    public record IaReprocessWrapper(String status, IaReprocessResponse data, String message) {}

    public record ReprocesarEstadoResponse(String articuloEstado, String imagenEstadoIa) {}

    @Transactional(readOnly = true)
    public List<com.decoraciones.domain.dtos.proyectodiseno.ArticuloInventarioDto> recomendarPorTexto(String prompt, Integer limit, Long categoriaId) {
        log.info("Requesting text recommendations from IA for prompt: '{}', limit: {}, categoriaId: {}", prompt, limit, categoriaId);
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("prompt", prompt);
        requestBody.put("limit", limit != null ? limit : 5);
        if (categoriaId != null) {
            requestBody.put("categoria_id", categoriaId);
        }

        String jsonPayload;
        try {
            jsonPayload = objectMapper.writeValueAsString(requestBody);
        } catch (Exception e) {
            log.error("Failed to serialize request body to JSON string", e);
            throw new RuntimeException("Error al serializar cuerpo de petición: " + e.getMessage(), e);
        }

        try {
            IaResponseWrapper response = restClient.post()
                    .uri("/api/v1/recommend/text")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jsonPayload)
                    .retrieve()
                    .body(IaResponseWrapper.class);

            if (response == null || !"success".equalsIgnoreCase(response.status()) || response.data() == null) {
                log.warn("IA text recommendation failed or returned empty response: {}", response);
                return List.of();
            }

            return enrichAndSortArticles(response.data().items());
        } catch (Exception e) {
            log.error("Error communicating with IA for text recommendations", e);
            throw new RuntimeException("Error al obtener recomendaciones por texto de la IA: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public List<com.decoraciones.domain.dtos.proyectodiseno.ArticuloInventarioDto> recomendarPorImagen(MultipartFile file, Integer limit, Long categoriaId) {
        log.info("Requesting image recommendations from IA for file: {}, limit: {}, categoriaId: {}", file.getOriginalFilename(), limit, categoriaId);

        try {
            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("file", file.getResource(), MediaType.parseMediaType(Objects.requireNonNull(file.getContentType())));

            MultiValueMap<String, HttpEntity<?>> multipartBody = builder.build();

            IaResponseWrapper response = restClient.post()
                    .uri(uriBuilder -> {
                        var uBuilder = uriBuilder.path("/api/v1/recommend/image");
                        uBuilder.queryParam("limit", limit != null ? limit : 5);
                        if (categoriaId != null) {
                            uBuilder.queryParam("categoria_id", categoriaId);
                        }
                        return uBuilder.build();
                    })
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(multipartBody)
                    .retrieve()
                    .body(IaResponseWrapper.class);

            if (response == null || !"success".equalsIgnoreCase(response.status()) || response.data() == null) {
                log.warn("IA image recommendation failed or returned empty response: {}", response);
                return List.of();
            }

            return enrichAndSortArticles(response.data().items());
        } catch (Exception e) {
            log.error("Error communicating with IA for image recommendations", e);
            throw new RuntimeException("Error al obtener recomendaciones por imagen de la IA: " + e.getMessage(), e);
        }
    }

    public IaReprocessResponse iniciarReprocesamiento(Long articuloId) {
        log.info("Starting reprocessing for article ID {}", articuloId);

        // Fetch article
        ArticuloInventario articulo = articuloRepository.findById(articuloId)
                .orElseThrow(ArticuloInventarioNoEncontradoException::new);

        // Fetch principal image
        ImagenArticulo principal = imagenRepository.findByArticuloInventarioIdAndEsPrincipalTrue(articuloId)
                .orElseThrow(() -> new IllegalArgumentException("El artículo no tiene una imagen principal configurada."));

        // Set states to PENDIENTE
        articulo.setEstado("PENDIENTE");
        principal.setEstadoIa(EstadoIa.PENDIENTE);

        articuloRepository.save(articulo);
        imagenRepository.save(principal);

        try {
            IaReprocessWrapper response = restClient.post()
                    .uri("/internal/reprocess/{id}", articuloId)
                    .retrieve()
                    .body(IaReprocessWrapper.class);

            if (response == null || !"success".equalsIgnoreCase(response.status()) || response.data() == null) {
                throw new RuntimeException("El microservicio de IA falló al iniciar el procesamiento");
            }

            return response.data();
        } catch (Exception e) {
            log.error("Error starting reprocessing on IA microservice for article ID {}", articuloId, e);
            // Revert states
            String restoreState = (articulo.getStockTotal() != null && articulo.getStockTotal() <= 0) 
                    ? "STOCK_BAJO" : "DISPONIBLE";
            articulo.setEstado(restoreState);
            principal.setEstadoIa(EstadoIa.FALLIDO);
            articuloRepository.save(articulo);
            imagenRepository.save(principal);
            throw new RuntimeException("No se pudo iniciar el reprocesamiento en el microservicio de IA: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public ReprocesarEstadoResponse obtenerEstadoReprocesamiento(Long articuloId) {
        ArticuloInventario articulo = articuloRepository.findById(articuloId)
                .orElseThrow(ArticuloInventarioNoEncontradoException::new);

        String articuloEstado = articulo.getEstado();
        String imagenEstadoIa = imagenRepository.findByArticuloInventarioIdAndEsPrincipalTrue(articuloId)
                .map(img -> img.getEstadoIa() != null ? img.getEstadoIa().name() : null)
                .orElse(null);

        return new ReprocesarEstadoResponse(articuloEstado, imagenEstadoIa);
    }

    private List<com.decoraciones.domain.dtos.proyectodiseno.ArticuloInventarioDto> enrichAndSortArticles(List<IaRecommendItem> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }

        // Map to keep track of score and rank/index
        Map<Long, Double> scores = new HashMap<>();
        List<Long> idsInOrder = new ArrayList<>();
        for (IaRecommendItem item : items) {
            scores.put(item.id(), item.score());
            idsInOrder.add(item.id());
        }

        // Fetch from DB
        List<ArticuloInventario> databaseArticles = articuloRepository.findAllById(idsInOrder);
        
        // Map database articles to DTO
        Map<Long, com.decoraciones.domain.dtos.proyectodiseno.ArticuloInventarioDto> responseMap = databaseArticles.stream()
                .map(articuloMapper::toDto)
                .collect(Collectors.toMap(com.decoraciones.domain.dtos.proyectodiseno.ArticuloInventarioDto::id, r -> r));

        // Sort according to IA recommendations list
        List<com.decoraciones.domain.dtos.proyectodiseno.ArticuloInventarioDto> sortedResult = new ArrayList<>();
        for (Long id : idsInOrder) {
            com.decoraciones.domain.dtos.proyectodiseno.ArticuloInventarioDto response = responseMap.get(id);
            if (response != null) {
                // Return decorated response if necessary, or just the list in sorted order
                sortedResult.add(response);
            }
        }
        return sortedResult;
    }
}
