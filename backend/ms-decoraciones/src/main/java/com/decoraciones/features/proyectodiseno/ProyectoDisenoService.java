package com.decoraciones.features.proyectodiseno;

import com.decoraciones.common.errors.ProyectoDisenoNoEncontradoException;
import com.decoraciones.domain.dtos.proyectodiseno.*;
import com.decoraciones.domain.models.*;
import com.decoraciones.features.elementolienzo.ElementoLienzoRepository;
import com.decoraciones.features.escenariobase.EscenarioBaseRepository;
import com.decoraciones.features.imagenarticulo.ImagenArticuloRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ProyectoDisenoService {

    private final ProyectoDisenoRepository proyectoRepository;
    private final EscenarioBaseRepository escenarioRepository;
    private final ElementoLienzoRepository elementoRepository;
    private final ImagenArticuloRepository imagenArticuloRepository;
    private final com.decoraciones.features.reserva.InventarioLockService lockService;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.upload.dir}")
    private String uploadDir;

    public ProyectoDisenoService(ProyectoDisenoRepository proyectoRepository,
                                 EscenarioBaseRepository escenarioRepository,
                                 ElementoLienzoRepository elementoRepository,
                                 ImagenArticuloRepository imagenArticuloRepository,
                                 com.decoraciones.features.reserva.InventarioLockService lockService) {
        this.proyectoRepository    = proyectoRepository;
        this.escenarioRepository   = escenarioRepository;
        this.elementoRepository    = elementoRepository;
        this.imagenArticuloRepository = imagenArticuloRepository;
        this.lockService = lockService;
    }

    // ── Proyectos ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ProyectoDisenoResponse> findAllByUsuario(Long usuarioId) {
        return proyectoRepository
                .findAllByUsuarioIdOrderByFechaUltimaModificacionDesc(usuarioId)
                .stream()
                .map(this::toProyectoResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProyectoDisenoResponse findById(Long id, Long usuarioId) {
        ProyectoDiseno proyecto = proyectoRepository
                .findByIdWithEscenarios(id, usuarioId)
                .orElseThrow(ProyectoDisenoNoEncontradoException::new);
        return toProyectoResponseConEscenarios(proyecto);
    }

    public ProyectoDisenoResponse create(ProyectoDisenoRequest request, Usuario usuario) {
        ProyectoDiseno proyecto = new ProyectoDiseno();
        proyecto.setNombre(request.nombre());
        proyecto.setDescripcion(request.descripcion());
        proyecto.setEstado(request.estado() != null ? request.estado() : "borrador");
        proyecto.setFechaEvento(request.fechaEvento());
        proyecto.setLugarEvento(request.lugarEvento());
        proyecto.setNumeroMetadato(request.numeroMetadato());
        proyecto.setDistanciaKm(request.distanciaKm());
        proyecto.setLatitud(request.latitud());
        proyecto.setLongitud(request.longitud());
        proyecto.setUsuario(usuario);
        return toProyectoResponse(proyectoRepository.save(proyecto));
    }

    public ProyectoDisenoResponse update(Long id, Long usuarioId, ProyectoDisenoRequest request) {
        ProyectoDiseno proyecto = proyectoRepository
                .findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(ProyectoDisenoNoEncontradoException::new);

        proyecto.setNombre(request.nombre());
        proyecto.setDescripcion(request.descripcion());
        proyecto.setEstado(request.estado());
        proyecto.setFechaEvento(request.fechaEvento());
        proyecto.setLugarEvento(request.lugarEvento());
        proyecto.setNumeroMetadato(request.numeroMetadato());
        proyecto.setDistanciaKm(request.distanciaKm());
        proyecto.setLatitud(request.latitud());
        proyecto.setLongitud(request.longitud());

        return toProyectoResponse(proyectoRepository.save(proyecto));
    }

    public void delete(Long id, Long usuarioId) {
        ProyectoDiseno proyecto = proyectoRepository
                .findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(ProyectoDisenoNoEncontradoException::new);
        proyecto.setIsDeleted(true);
        proyectoRepository.save(proyecto);
    }

    // ── Escenarios ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public EscenarioBaseResponse findEscenarioById(Long escenarioId, Long proyectoId) {
        EscenarioBase escenario = escenarioRepository
                .findByIdWithElementos(escenarioId)
                .orElseThrow(ProyectoDisenoNoEncontradoException::new);
        return toEscenarioResponse(escenario);
    }

    public EscenarioBaseResponse createEscenario(Long proyectoId, Long usuarioId,
                                                 EscenarioBaseRequest request) {
        ProyectoDiseno proyecto = proyectoRepository
                .findByIdAndUsuarioId(proyectoId, usuarioId)
                .orElseThrow(ProyectoDisenoNoEncontradoException::new);

        EscenarioBase escenario = new EscenarioBase();
        escenario.setNombre(request.nombre());
        escenario.setDescripcion(request.descripcion());
        escenario.setDimensionesAltoPx(request.dimensionesAltoPx());
        escenario.setDimensionesAnchoPx(request.dimensionesAnchoPx());
        escenario.setActivo(true);
        escenario.setProyectoDiseno(proyecto);

        EscenarioBase saved = escenarioRepository.save(escenario);

        // Si es el primer escenario, establecerlo como activo por defecto
        if (proyecto.getEscenarioBaseId() == null) {
            proyecto.setEscenarioBaseId(saved.getId());
            proyectoRepository.save(proyecto);
        }

        return toEscenarioResponse(saved);
    }

    public EscenarioBaseResponse updateEscenario(Long escenarioId, Long proyectoId,
                                                 EscenarioBaseRequest request) {
        EscenarioBase escenario = escenarioRepository
                .findByIdAndProyectoDisenoId(escenarioId, proyectoId)
                .orElseThrow(ProyectoDisenoNoEncontradoException::new);

        escenario.setNombre(request.nombre());
        escenario.setDescripcion(request.descripcion());
        escenario.setDimensionesAltoPx(request.dimensionesAltoPx());
        escenario.setDimensionesAnchoPx(request.dimensionesAnchoPx());

        return toEscenarioResponse(escenarioRepository.save(escenario));
    }

    public EscenarioBaseResponse uploadImagenEscenario(Long escenarioId, Long proyectoId,
                                                       MultipartFile file) throws IOException {
        EscenarioBase escenario = escenarioRepository
                .findByIdAndProyectoDisenoId(escenarioId, proyectoId)
                .orElseThrow(ProyectoDisenoNoEncontradoException::new);

        String extension  = getExtension(file.getOriginalFilename());
        String nombreArchivo = "escenario-" + escenarioId + "-" + UUID.randomUUID() + "." + extension;

        UUID proyectoUuid = escenario.getProyectoDiseno().getUuid();
        UUID escenarioUuid = escenario.getUuid();

        Path destino = Paths.get(uploadDir, proyectoUuid.toString(), escenarioUuid.toString(), nombreArchivo);
        Files.createDirectories(destino.getParent());
        Files.write(destino, file.getBytes());

        String relativePath = proyectoUuid.toString() + "/" + escenarioUuid.toString() + "/" + nombreArchivo;
        escenario.setImagenUrl(relativePath);
        return toEscenarioResponse(escenarioRepository.save(escenario));
    }

    public void deleteEscenario(Long escenarioId, Long proyectoId) {
        EscenarioBase escenario = escenarioRepository
                .findByIdAndProyectoDisenoId(escenarioId, proyectoId)
                .orElseThrow(ProyectoDisenoNoEncontradoException::new);
        escenario.setIsDeleted(true);
        escenarioRepository.save(escenario);
    }

    // ── Elementos del lienzo ──────────────────────────────────────────────────

    /**
     * Reemplaza TODOS los elementos del escenario en una sola transacción.
     * Valida que la cantidad total por artículo no supere el stockTotal.
     */
    public List<ElementoLienzoResponse> guardarElementos(Long escenarioId,
                                                         Long proyectoId,
                                                         List<ElementoLienzoRequest> requests) {
        EscenarioBase escenario = escenarioRepository
                .findByIdAndProyectoDisenoId(escenarioId, proyectoId)
                .orElseThrow(ProyectoDisenoNoEncontradoException::new);

        // Validar stock antes de operar
        validarStock(requests, proyectoId, escenarioId);

        // Soft delete de todos los elementos actuales del escenario
        elementoRepository.softDeleteAllByEscenarioId(escenarioId);

        // Insertar los nuevos elementos
        List<ElementoLienzo> nuevos = requests.stream().map(req -> {
            ElementoLienzo el = new ElementoLienzo();
            el.setProyectoId(proyectoId);
            el.setEscenarioBase(escenario);
            el.setArticuloInventario(new ArticuloInventario() {{ setId(req.articuloId()); }});
            el.setCantidad(req.cantidad());
            el.setPosX(req.posX());
            el.setPosY(req.posY());
            el.setWidth(req.width());
            el.setHeight(req.height());
            el.setScaleX(req.scaleX() != null ? req.scaleX() : 1.0);
            el.setScaleY(req.scaleY() != null ? req.scaleY() : 1.0);
            el.setRotacionDeg(req.rotacionDeg() != null ? req.rotacionDeg() : 0.0);
            el.setOpacity(req.opacity() != null ? req.opacity() : 1.0);
            el.setZIndex(req.zIndex() != null ? req.zIndex() : 0);
            el.setLayer(req.layer() != null ? req.layer() : "main");
            el.setVistaActual(req.vistaActual() != null ? req.vistaActual() : "FRONTAL");
            return el;
        }).toList();

        return elementoRepository.saveAll(nuevos)
                .stream()
                .map(this::toElementoResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ElementoLienzoResponse> findElementosByEscenario(Long escenarioId) {
        return elementoRepository
                .findAllByEscenarioBaseIdOrderByZIndexAsc(escenarioId)
                .stream()
                .map(this::toElementoResponse)
                .toList();
    }

    // ── Validación de stock ───────────────────────────────────────────────────

    private void validarStock(List<ElementoLienzoRequest> requests,
                              Long proyectoId, Long escenarioIdActual) {
        ProyectoDiseno proyecto = proyectoRepository.findById(proyectoId).orElseThrow();
        java.time.LocalDate fechaEvento = proyecto.getFechaEvento() != null ? proyecto.getFechaEvento() : java.time.LocalDate.now().plusDays(30);

        // Agrupar por artículo para validar en lote
        requests.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        ElementoLienzoRequest::articuloId,
                        java.util.stream.Collectors.summingInt(ElementoLienzoRequest::cantidad)
                ))
                .forEach((articuloId, cantidadEnEsteEscenario) -> {
                    // Cantidad ya usada en otros escenarios del mismo proyecto (excluyendo el actual)
                    Integer cantidadOtrosEscenarios = elementoRepository
                            .sumCantidadByArticuloAndProyectoExcludingEscenario(articuloId, proyectoId, escenarioIdActual);

                    int totalRequerido = cantidadOtrosEscenarios + cantidadEnEsteEscenario;
                    int disponible = lockService.getStockDisponible(articuloId, fechaEvento, fechaEvento.plusDays(1), proyectoId);

                    if (disponible < totalRequerido) {
                        throw new IllegalArgumentException("Stock insuficiente para el artículo ID: " + articuloId + ". Requerido: " + totalRequerido + ", Disponible: " + disponible);
                    }
                });
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private ProyectoDisenoResponse toProyectoResponse(ProyectoDiseno p) {
        return new ProyectoDisenoResponse(
                p.getId(), p.getNombre(), p.getDescripcion(), p.getEstado(),
                p.getFechaEvento(), p.getLugarEvento(), p.getNumeroMetadato(),
                p.getDistanciaKm(), p.getLatitud(), p.getLongitud(), p.getCostoRealTotal(), p.getEscenarioBaseId(),
                p.getFechaCreacion(), p.getFechaUltimaModificacion(),
                List.of()   // sin escenarios en listado general — evita N+1
        );
    }

    private ProyectoDisenoResponse toProyectoResponseConEscenarios(ProyectoDiseno p) {
        List<EscenarioBaseResponse> escenarios = p.getEscenarios()
                .stream()
                .map(this::toEscenarioResponse)
                .toList();
        return new ProyectoDisenoResponse(
                p.getId(), p.getNombre(), p.getDescripcion(), p.getEstado(),
                p.getFechaEvento(), p.getLugarEvento(), p.getNumeroMetadato(),
                p.getDistanciaKm(), p.getLatitud(), p.getLongitud(), p.getCostoRealTotal(), p.getEscenarioBaseId(),
                p.getFechaCreacion(), p.getFechaUltimaModificacion(),
                escenarios
        );
    }

    private EscenarioBaseResponse toEscenarioResponse(EscenarioBase e) {
        String imagenUrl = e.getImagenUrl() != null
                ? baseUrl + "/uploads/" + e.getImagenUrl()
                : null;
        List<ElementoLienzoResponse> elementos = e.getElementos() == null
                ? List.of()
                : e.getElementos().stream().map(this::toElementoResponse).toList();
        return new EscenarioBaseResponse(
                e.getId(), e.getNombre(), e.getDescripcion(),
                imagenUrl, e.getDimensionesAltoPx(), e.getDimensionesAnchoPx(),
                e.getActivo(), elementos
        );
    }

    private ElementoLienzoResponse toElementoResponse(ElementoLienzo el) {
        ArticuloInventario art = el.getArticuloInventario();

        // Buscar imagen principal del artículo
        String imagenUrl = imagenArticuloRepository
                .findByArticuloInventarioIdAndEsPrincipalTrue(art.getId())
                .map(img -> baseUrl + "/" + img.getUrl())
                .orElse(null);

        List<com.decoraciones.domain.dtos.articuloinventario.ImagenArticuloResponse> imagenes = imagenArticuloRepository
                .findByArticuloInventarioIdOrderByOrdenAsc(art.getId())
                .stream()
                .map(img -> new com.decoraciones.domain.dtos.articuloinventario.ImagenArticuloResponse(
                        img.getId(),
                        baseUrl + "/" + img.getUrl(),
                        img.getEsPrincipal(),
                        img.getOrden(),
                        img.getProcesadoIa(),
                        img.getFechaSubida(),
                        img.getTipoVista() != null ? img.getTipoVista().name() : null
                ))
                .toList();

        return new ElementoLienzoResponse(
                el.getId(),
                art.getId(),
                art.getNombre(),
                imagenUrl,
                art.getCostoAdquisicion(),
                art.getPorcentajeGanancia(),
                el.getCantidad(),
                el.getPosX(), el.getPosY(),
                el.getWidth(), el.getHeight(),
                el.getScaleX(), el.getScaleY(),
                el.getRotacionDeg(), el.getOpacity(),
                el.getZIndex(), el.getLayer(),
                el.getVistaActual(),
                imagenes
        );
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "jpg";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
