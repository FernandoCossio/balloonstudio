package com.decoraciones.features.inventario;

import com.decoraciones.common.errors.ArticuloInventarioNoEncontradoException;
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

    @Transactional(readOnly = true)
    public List<ArticuloInventario> findAll() {
        return articuloRepository.findAll();
    }

    @Transactional(readOnly = true)
    public ArticuloInventario findById(Long id) {
        return articuloRepository.findById(id)
                .orElseThrow(ArticuloInventarioNoEncontradoException::new);
    }

    public ArticuloInventario create(ArticuloInventario articulo, Set<Long> categoriaIds) {
        if (categoriaIds != null && !categoriaIds.isEmpty()) {
            Set<Categoria> categorias = new HashSet<>(categoriaRepository.findAllById(categoriaIds));
            articulo.setCategorias(categorias);
        }
        return articuloRepository.save(articulo);
    }

    public ArticuloInventario update(Long id, ArticuloInventario datos, Set<Long> categoriaIds) {
        ArticuloInventario existente = findById(id);

        existente.setNombre(datos.getNombre());
        existente.setDescripcion(datos.getDescripcion());
        existente.setTipoArticulo(datos.getTipoArticulo());
        existente.setEstado(datos.getEstado());
        existente.setCostoAdquisicion(datos.getCostoAdquisicion());
        existente.setPorcentajeGanancia(datos.getPorcentajeGanancia());
        existente.setValorResidual(datos.getValorResidual());
        existente.setVidaUtilAnos(datos.getVidaUtilAnos());
        existente.setVidaUtilUsos(datos.getVidaUtilUsos());
        existente.setStockTotal(datos.getStockTotal());
        existente.setPesoKg(datos.getPesoKg());
        existente.setVolumenM3(datos.getVolumenM3());
        existente.setTiempoArmadoMin(datos.getTiempoArmadoMin());
        existente.setDiasPreparacionPrevios(datos.getDiasPreparacionPrevios());
        existente.setDiasLimpiezaPosteriores(datos.getDiasLimpiezaPosteriores());
        existente.setMantenimientoPromedioBs(datos.getMantenimientoPromedioBs());
        existente.setNivelComplejidad(datos.getNivelComplejidad());
        existente.setEmbeddingVisual(datos.getEmbeddingVisual());

        if (categoriaIds != null) {
            Set<Categoria> categorias = new HashSet<>(categoriaRepository.findAllById(categoriaIds));
            existente.setCategorias(categorias);
        }

        return articuloRepository.save(existente);
    }

    public void delete(Long id) {
        ArticuloInventario existente = findById(id);
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
