package com.decoraciones.seed;

import com.decoraciones.domain.models.Rol;
import com.decoraciones.features.rol.RolRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class RolSeeder implements CommandLineRunner {

    private final RolRepository rolRepository;

    public RolSeeder(RolRepository rolRepository) {
        this.rolRepository = rolRepository;
    }

    @Override
    public void run(String... args) {
        seedRoles();
    }

    private void seedRoles() {
        seedRol("ADMIN", "Administrador del sistema con acceso total");
        seedRol("CLIENTE", "Usuario cliente con acceso a funciones de compra");
        seedRol("EMPLEADO", "Empleado con acceso a funciones de sistema");
    }

    private void seedRol(String nombre, String descripcion) {
        if (rolRepository.findByNombre(nombre).isEmpty()) {
            Rol rol = new Rol();
            rol.setNombre(nombre);
            rol.setDescripcion(descripcion);
            rolRepository.save(rol);
            System.out.println("Rol creado: " + nombre);
        }
    }
}
