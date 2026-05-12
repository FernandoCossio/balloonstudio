package com.decoraciones.features.empleado;

import com.decoraciones.domain.models.Empleado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class EmpleadoService {

    private final EmpleadoRepository empleadoRepository;

    public EmpleadoService(EmpleadoRepository empleadoRepository) {
        this.empleadoRepository = empleadoRepository;
    }

    @Transactional(readOnly = true)
    public List<Empleado> findAll() {
        return empleadoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Empleado findById(Long id) {
        return empleadoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Empleado no encontrado con id: " + id));
    }

    public Empleado create(Empleado empleado) {
        if (empleadoRepository.existsByCi(empleado.getCi())) {
            throw new RuntimeException("Ya existe un empleado con CI: " + empleado.getCi());
        }
        if (empleado.getEmail() != null && empleadoRepository.existsByEmail(empleado.getEmail())) {
            throw new RuntimeException("Ya existe un empleado con email: " + empleado.getEmail());
        }
        return empleadoRepository.save(empleado);
    }

    public Empleado update(Long id, Empleado datos) {
        Empleado existente = findById(id);

        if (!existente.getCi().equals(datos.getCi()) && empleadoRepository.existsByCi(datos.getCi())) {
            throw new RuntimeException("Ya existe un empleado con CI: " + datos.getCi());
        }
        if (datos.getEmail() != null
                && !datos.getEmail().equalsIgnoreCase(existente.getEmail())
                && empleadoRepository.existsByEmail(datos.getEmail())) {
            throw new RuntimeException("Ya existe un empleado con email: " + datos.getEmail());
        }

        existente.setNombre(datos.getNombre());
        existente.setApellido(datos.getApellido());
        existente.setCi(datos.getCi());
        existente.setCargo(datos.getCargo());
        existente.setTelefono(datos.getTelefono());
        existente.setEmail(datos.getEmail());
        existente.setFechaContratacion(datos.getFechaContratacion());
        existente.setActivo(datos.getActivo());
        return empleadoRepository.save(existente);
    }

    public void delete(Long id) {
        Empleado existente = findById(id);
        existente.setIsDeleted(true);
        empleadoRepository.save(existente);
    }
}
