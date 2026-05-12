package com.decoraciones.seed;

import com.decoraciones.domain.models.Rol;
import com.decoraciones.domain.models.Usuario;
import com.decoraciones.features.rol.RolRepository;
import com.decoraciones.features.usuario.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@Order(2)
public class UsuarioSeeder implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioSeeder(UsuarioRepository usuarioRepository, RolRepository rolRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedAdminUser();
    }

    private void seedAdminUser() {
        String adminUsername = "admin";
        if (usuarioRepository.findByUsernameIgnoreCase(adminUsername).isEmpty()) {
            Rol adminRol = rolRepository.findByNombre("ADMIN")
                    .orElseThrow(() -> new RuntimeException("Error: Rol ADMIN no encontrado."));

            Usuario admin = new Usuario();
            admin.setUsername(adminUsername);
            admin.setEmail("admin@decoraciones.com");
            admin.setNombreCompleto("Administrador del Sistema");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setTelefono("00000000");
            admin.setActivo(true);
            admin.setRoles(Set.of(adminRol));

            usuarioRepository.save(admin);
            System.out.println("Usuario administrador creado por defecto (admin / admin123)");
        }
    }
}
