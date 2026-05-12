package com.decoraciones.config;

import com.decoraciones.domain.models.Rol;
import com.decoraciones.domain.models.Usuario;
import com.decoraciones.features.rol.RolRepository;
import com.decoraciones.features.usuario.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UsuarioRepository usuarioRepository,
                      RolRepository rolRepository,
                      PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedRoles();
        seedAdminUser();
    }

    private void seedRoles() {
        crearRolSiNoExiste("ADMIN", "Administrador del sistema");
        crearRolSiNoExiste("EMPLEADO", "Empleado con acceso limitado");
    }

    private void crearRolSiNoExiste(String nombre, String descripcion) {
        if (rolRepository.findByNombreIgnoreCase(nombre).isEmpty()) {
            Rol rol = new Rol();
            rol.setNombre(nombre);
            rol.setDescripcion(descripcion);
            rolRepository.save(rol);
            log.info("[Seeder] Rol '{}' creado.", nombre);
        }
    }

    private void seedAdminUser() {
        if (usuarioRepository.findByUsernameIgnoreCase("admin").isPresent()) {
            log.info("[Seeder] Usuario 'admin' ya existe, omitiendo.");
            return;
        }

        Rol rolAdmin = rolRepository.findByNombreIgnoreCase("ADMIN")
                .orElseThrow(() -> new IllegalStateException("Rol ADMIN no encontrado"));

        Usuario admin = new Usuario();
        admin.setUsername("admin");
        admin.setEmail("admin@decoraciones.com");
        admin.setNombreCompleto("Administrador");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setActivo(true);
        admin.setRoles(Set.of(rolAdmin));

        usuarioRepository.save(admin);
        log.info("[Seeder] Usuario admin creado → username: admin | password: admin123");
    }
}
