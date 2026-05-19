package com.decoraciones.services.thumbnail;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("thumbnails")
public class ThumbnailController {

    private final ThumbnailService thumbnailService;

    public ThumbnailController(ThumbnailService thumbnailService) {
        this.thumbnailService = thumbnailService;
    }

    /**
     * GET /api/thumbnails?url=uploads/inventario/silla_oak/silla_oak-123.png
     *
     * Angular llama este endpoint para cada tarjeta del sidebar.
     * La primera vez genera y cachea; las siguientes son instantáneas.
     */
    @GetMapping
    public ResponseEntity<byte[]> getThumbnail(
            @RequestParam String url) {

        try {
            byte[] bytes = thumbnailService.getThumbnail(url);
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS))
                    .body(bytes);

        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }
}