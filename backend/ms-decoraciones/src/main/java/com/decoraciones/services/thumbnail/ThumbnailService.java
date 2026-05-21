package com.decoraciones.services.thumbnail;

import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ThumbnailService {

    @Value("${app.upload.dir}")
    private String uploadDir;

    private final Map<String, byte[]> cache = new ConcurrentHashMap<>();

    private static final int THUMB_WIDTH  = 120;
    private static final int THUMB_HEIGHT = 120;

    /**
     * Recibe la url tal como está en BD:
     * "uploads/inventario/silla_oak/silla_oak-1234567890.png"
     *
     * Retorna los bytes JPEG del thumbnail 120x120.
     * Lanza IOException si el archivo no existe en disco.
     */
    public byte[] getThumbnail(String relativeUrl) throws IOException {

        if (cache.containsKey(relativeUrl)) {
            return cache.get(relativeUrl);
        }

        // Construir ruta absoluta: uploadDir ya apunta a la raíz,
        // relativeUrl empieza con "uploads/" → quitamos ese prefijo
        String sinPrefijo = relativeUrl.replaceFirst("^uploads/", "");
        Path imagenPath = Paths.get(uploadDir).resolve(sinPrefijo);

        if (!Files.exists(imagenPath)) {
            throw new IOException("Imagen no encontrada: " + imagenPath);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Thumbnails.of(imagenPath.toFile())
                .size(THUMB_WIDTH, THUMB_HEIGHT)
                .keepAspectRatio(true)
                .outputFormat("JPEG")
                .outputQuality(0.85)
                .toOutputStream(out);

        byte[] bytes = out.toByteArray();
        cache.put(relativeUrl, bytes);
        return bytes;
    }

    public void evictCache(String relativeUrl) {
        cache.remove(relativeUrl);
    }
}