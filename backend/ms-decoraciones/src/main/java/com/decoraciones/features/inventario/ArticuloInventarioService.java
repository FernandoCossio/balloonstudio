package com.decoraciones.features.inventario;

import com.decoraciones.common.errors.ArticuloInventarioNoEncontradoException;
import com.decoraciones.domain.dtos.articuloinventario.ArticuloInventarioRequest;
import com.decoraciones.domain.dtos.articuloinventario.ArticuloInventarioResponse;
import com.decoraciones.domain.dtos.proyectodiseno.ArticuloInventarioDto;
import com.decoraciones.domain.models.ArticuloInventario;
import com.decoraciones.domain.models.Categoria;
import com.decoraciones.features.categoria.CategoriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class ArticuloInventarioService {

    private final ArticuloInventarioMapper mapper;
    private final ArticuloInventarioRepository articuloRepository;
    private final CategoriaRepository categoriaRepository;

    public ArticuloInventarioService(ArticuloInventarioRepository articuloRepository,
                                     CategoriaRepository categoriaRepository,
                                     ArticuloInventarioMapper mapper) {
        this.articuloRepository = articuloRepository;
        this.categoriaRepository = categoriaRepository;
        this.mapper = mapper;
    }

    private ArticuloInventario findEntityById(Long id) {
        return articuloRepository.findById(id)
                .orElseThrow(ArticuloInventarioNoEncontradoException::new);
    }

    @Transactional(readOnly = true)
    public List<ArticuloInventarioResponse> findAll() {
        return articuloRepository.findAll().stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ArticuloInventarioResponse findById(Long id) {
        return mapper.toResponse(findEntityById(id));
    }

    public ArticuloInventarioResponse create(ArticuloInventarioRequest request) {
        ArticuloInventario articulo = mapper.toEntity(request);
        if (request.categoriaIds() != null && !request.categoriaIds().isEmpty()) {
            Set<Categoria> categorias = new HashSet<>(categoriaRepository.findAllById(request.categoriaIds()));
            articulo.setCategorias(categorias);
        }
        ArticuloInventario saved = articuloRepository.save(articulo);
        return mapper.toResponse(saved);
    }

    public ArticuloInventarioResponse update(Long id, ArticuloInventarioRequest request) {
        ArticuloInventario existente = findEntityById(id);

        existente.setNombre(request.nombre());
        existente.setDescripcion(request.descripcion());
        existente.setTipoArticulo(request.tipoArticulo());
        existente.setEstado(request.estado());
        existente.setCostoAdquisicion(request.costoAdquisicion());
        existente.setPorcentajeGanancia(request.porcentajeGanancia());
        existente.setValorResidual(request.valorResidual());
        existente.setVidaUtilAnos(request.vidaUtilAnos());
        existente.setVidaUtilUsos(request.vidaUtilUsos());
        existente.setStockTotal(request.stockTotal());
        existente.setPesoKg(request.pesoKg());
        existente.setVolumenM3(request.volumenM3());
        existente.setTiempoArmadoMin(request.tiempoArmadoMin());
        existente.setDiasPreparacionPrevios(request.diasPreparacionPrevios());
        existente.setDiasLimpiezaPosteriores(request.diasLimpiezaPosteriores());
        existente.setMantenimientoPromedioBs(request.mantenimientoPromedioBs());
        existente.setNivelComplejidad(request.nivelComplejidad());
        existente.setEmbeddingVisual(request.embeddingVisual());

        if (request.categoriaIds() != null) {
            Set<Categoria> categorias = new HashSet<>(categoriaRepository.findAllById(request.categoriaIds()));
            existente.setCategorias(categorias);
        }

        ArticuloInventario saved = articuloRepository.save(existente);
        return mapper.toResponse(saved);
    }

    public void delete(Long id) {
        ArticuloInventario existente = findEntityById(id);
        existente.setIsDeleted(true);
        articuloRepository.save(existente);
    }

    @Transactional(readOnly = true)
    public List<ArticuloInventarioDto> getCatalogo(String tipoArticulo, String estado) {
        return articuloRepository.findAll().stream()
                .filter(a -> tipoArticulo == null || tipoArticulo.isBlank()
                        || tipoArticulo.equalsIgnoreCase(a.getTipoArticulo()))
                .filter(a -> estado == null || estado.isBlank()
                        || estado.equalsIgnoreCase(a.getEstado()))
                .map(mapper::toDto)
                .toList();
    }
}
