package com.decoraciones.features.ia;

import com.decoraciones.common.response.ApiResponse;
import com.decoraciones.domain.dtos.proyectodiseno.ArticuloInventarioDto;
import com.decoraciones.features.ia.IaRecommendationService.IaReprocessResponse;
import com.decoraciones.features.ia.IaRecommendationService.ReprocesarEstadoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/recomendaciones")
@RequiredArgsConstructor
public class IaRecommendationController {

    private final IaRecommendationService iaService;

    public record TextoRecomendacionRequest(String prompt, Integer limit, Long categoriaId) {}

    @PostMapping("/texto")
    public ResponseEntity<ApiResponse<List<ArticuloInventarioDto>>> recomendarPorTexto(
            @RequestBody TextoRecomendacionRequest request) {
        log.info("REST request for text recommendations: prompt='{}', limit={}, categoriaId={}",
                request.prompt(), request.limit(), request.categoriaId());
        List<ArticuloInventarioDto> response = iaService.recomendarPorTexto(
                request.prompt(), request.limit(), request.categoriaId());
        return ResponseEntity.ok(ApiResponse.success(response, "Recomendaciones por texto obtenidas correctamente"));
    }

    @PostMapping(value = "/imagen", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<List<ArticuloInventarioDto>>> recomendarPorImagen(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "categoriaId", required = false) Long categoriaId) {
        log.info("REST request for image recommendations: file={}, limit={}, categoriaId={}",
                file.getOriginalFilename(), limit, categoriaId);
        List<ArticuloInventarioDto> response = iaService.recomendarPorImagen(file, limit, categoriaId);
        return ResponseEntity.ok(ApiResponse.success(response, "Recomendaciones por imagen obtenidas correctamente"));
    }

    @PostMapping("/articulos/{id}/reprocesar")
    public ResponseEntity<ApiResponse<IaReprocessResponse>> iniciarReprocesamiento(
            @PathVariable Long id) {
        log.info("REST request to start reprocessing for article ID {}", id);
        IaReprocessResponse response = iaService.iniciarReprocesamiento(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Reprocesamiento de embeddings iniciado correctamente"));
    }

    @GetMapping("/articulos/{id}/reprocesar/estado")
    public ResponseEntity<ApiResponse<ReprocesarEstadoResponse>> obtenerEstadoReprocesamiento(
            @PathVariable Long id) {
        log.info("REST request to get reprocessing status for article ID {}", id);
        ReprocesarEstadoResponse response = iaService.obtenerEstadoReprocesamiento(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Estado de reprocesamiento obtenido correctamente"));
    }
}
