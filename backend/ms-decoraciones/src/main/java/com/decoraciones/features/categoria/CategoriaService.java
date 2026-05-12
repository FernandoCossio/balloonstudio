package com.decoraciones.features.categoria;

import com.decoraciones.common.errors.CategoriaDuplicadaException;
import com.decoraciones.common.errors.CategoriaNoEncontradaException;
import com.decoraciones.domain.models.Categoria;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    public CategoriaService(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    @Transactional(readOnly = true)
    public List<Categoria> findAll() {
        return categoriaRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Categoria findById(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(CategoriaNoEncontradaException::new);
    }

    public Categoria create(Categoria categoria) {
        if (categoriaRepository.existsByNombreIgnoreCase(categoria.getNombre())) {
            throw new CategoriaDuplicadaException();
        }
        return categoriaRepository.save(categoria);
    }

    public Categoria update(Long id, Categoria datos) {
        Categoria existente = findById(id);
        if (!existente.getNombre().equalsIgnoreCase(datos.getNombre())
                && categoriaRepository.existsByNombreIgnoreCase(datos.getNombre())) {
            throw new CategoriaDuplicadaException();
        }
        existente.setNombre(datos.getNombre());
        existente.setDescripcion(datos.getDescripcion());
        return categoriaRepository.save(existente);
    }

    public void delete(Long id) {
        Categoria existente = findById(id);
        existente.setIsDeleted(true);
        categoriaRepository.save(existente);
    }
}
