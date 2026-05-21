package com.decoraciones.features.imagenarticulo;

import com.decoraciones.common.errors.ArticuloInventarioNoEncontradoException;
import com.decoraciones.common.errors.ImagenNoEncontradaException;
import com.decoraciones.common.errors.ImagenNoPerteneceAlArticuloException;
import com.decoraciones.domain.models.ArticuloInventario;
import com.decoraciones.domain.models.ImagenArticulo;
import com.decoraciones.features.inventario.ArticuloInventarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImagenArticuloService {

    @Value("${app.upload.dir}")
    private String uploadDir;

    private final ImagenArticuloRepository imagenRepository;
    private final ArticuloInventarioRepository articuloRepository;
    @Transactional
    public List<ImagenArticulo> uploadImagenes(Long articuloId, MultipartFile[] files) throws IOException {
        log.info("Iniciando la carga de {} archivos para el artículo ID: {}", files != null ? files.length : 0, articuloId);

        ArticuloInventario articulo = articuloRepository.findById(articuloId)
                .orElseThrow(ArticuloInventarioNoEncontradoException::new);

        if (files == null || files.length == 0) {
            log.info("No se proporcionaron archivos para cargar en artículo ID: {}", articuloId);
            return List.of();
        }

        List<ImagenArticulo> imagenesExistentes = imagenRepository.findByArticuloInventarioIdOrderByOrdenAsc(articuloId);
        int nextOrden = imagenesExistentes.size() + 1;
        boolean yaTienePrincipal = imagenesExistentes.stream().anyMatch(ImagenArticulo::getEsPrincipal);

        Path articulosDir = Paths.get(uploadDir, "articulos", String.valueOf(articuloId));
        if (!Files.exists(articulosDir)) {
            Files.createDirectories(articulosDir);
            log.debug("Directorio creado para las imágenes del artículo: {}", articulosDir);
        }

        List<ImagenArticulo> creadas = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;
            }

             String ext = getFileExtension(file.getOriginalFilename()).toLowerCase();
            String newFilename = UUID.randomUUID().toString() + (ext.isEmpty() ? "" : "." + ext);

            Path targetPath = articulosDir.resolve(newFilename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            String relativeUrl = "uploads/articulos/" + articuloId + "/" + newFilename;

            ImagenArticulo imagen = new ImagenArticulo();
            imagen.setArticuloInventario(articulo);
            imagen.setUrl(relativeUrl);
            imagen.setEsPrincipal(!yaTienePrincipal && creadas.isEmpty()); 
            imagen.setOrden(nextOrden++);
            imagen.setProcesadoIa(false);
            imagen.setFechaSubida(LocalDateTime.now());

            ImagenArticulo saved = imagenRepository.save(imagen);
            creadas.add(saved);
            log.info("Imagen guardada exitosamente. ID: {}, URL: {}", saved.getId(), relativeUrl);
        }

        log.info("Carga completada. Se crearon {} nuevas imágenes para el artículo ID: {}", creadas.size(), articuloId);
        return creadas;
    }

    @Transactional
    public void setPrincipal(Long articuloId, Long imagenId) {
        log.info("Estableciendo la imagen ID: {} como principal para el artículo ID: {}", imagenId, articuloId);

        ImagenArticulo targetImagen = imagenRepository.findById(imagenId)
                .orElseThrow(ImagenNoEncontradaException::new);

        if (!targetImagen.getArticuloInventario().getId().equals(articuloId)) {
            log.warn("Intento de asociar imagen ID: {} que no pertenece al artículo ID: {}", imagenId, articuloId);
            throw new ImagenNoPerteneceAlArticuloException();
        }

        List<ImagenArticulo> imagenes = imagenRepository.findByArticuloInventarioIdOrderByOrdenAsc(articuloId);
        for (ImagenArticulo img : imagenes) {
            img.setEsPrincipal(img.getId().equals(imagenId));
        }

        imagenRepository.saveAll(imagenes);
        log.info("Imagen ID: {} configurada como principal exitosamente", imagenId);
    }

    @Transactional
    public void deleteImagen(Long articuloId, Long imagenId) {
        log.info("Eliminando la imagen ID: {} asociada al artículo ID: {}", imagenId, articuloId);

        ImagenArticulo imagen = imagenRepository.findById(imagenId)
                .orElseThrow(ImagenNoEncontradaException::new);

        if (!imagen.getArticuloInventario().getId().equals(articuloId)) {
            log.warn("Conflicto al eliminar imagen ID: {}. No pertenece al artículo ID: {}", imagenId, articuloId);
            throw new ImagenNoPerteneceAlArticuloException();
        }

        String url = imagen.getUrl();
        Path physicalPath;
        if (url != null && url.startsWith("uploads/")) {
            physicalPath = Paths.get(uploadDir).resolve(url.substring(8));
        } else if (url != null) {
            physicalPath = Paths.get(uploadDir).getParent().resolve(url);
        } else {
            physicalPath = null;
        }

        if (physicalPath != null) {
            try {
                if (Files.deleteIfExists(physicalPath)) {
                    log.info("Archivo físico eliminado correctamente: {}", physicalPath);
                } else {
                    log.warn("El archivo físico no existía en la ruta: {}", physicalPath);
                }
            } catch (IOException e) {
                log.error("No se pudo borrar el archivo físico: {}. Error: {}", physicalPath, e.getMessage(), e);
            }
        }

        imagen.setIsDeleted(true);
        boolean eraPrincipal = imagen.getEsPrincipal();
        imagenRepository.save(imagen);
        log.info("Imagen ID: {} marcada como eliminada (Soft Delete)", imagenId);

        if (eraPrincipal) {
            List<ImagenArticulo> restantes = imagenRepository.findByArticuloInventarioIdOrderByOrdenAsc(articuloId);
            if (!restantes.isEmpty()) {
                ImagenArticulo nuevaPrincipal = restantes.get(0);
                nuevaPrincipal.setEsPrincipal(true);
                imagenRepository.save(nuevaPrincipal);
                log.info("La imagen ID: {} ha sido designada como nueva principal", nuevaPrincipal.getId());
            }
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int lastIndex = filename.lastIndexOf('.');
        return (lastIndex == -1) ? "" : filename.substring(lastIndex + 1);
    }
}
