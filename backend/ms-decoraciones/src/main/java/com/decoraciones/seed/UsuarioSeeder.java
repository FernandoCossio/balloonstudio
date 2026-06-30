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
        if (usuarioRepository.count() > 0) {
            System.out.println("UsuarioSeeder: Ya existen usuarios en la base de datos. Omitiendo seed.");
            return;
        }
        seedUsuarios();
    }

    private void seedUsuarios() {
        Rol adminRol = rolRepository.findByNombre("ADMIN")
                .orElseThrow(() -> new RuntimeException("Error: Rol ADMIN no encontrado."));
        Rol empleadoRol = rolRepository.findByNombre("EMPLEADO")
                .orElseThrow(() -> new RuntimeException("Error: Rol EMPLEADO no encontrado."));
        Rol clienteRol = rolRepository.findByNombre("CLIENTE")
                .orElseThrow(() -> new RuntimeException("Error: Rol CLIENTE no encontrado."));

        // 1. Administradores
        seedUsuario("fcossio100", "fcossio100@gmail.com", "Fernando Cossio", "70000001", true, Set.of(adminRol));
        seedUsuario("axelmquispia", "axelmquispia@gmail.com", "Axel Quispe", "70000002", true, Set.of(adminRol));

        // 2. Empleados Específicos
        seedUsuario("fcossio333", "fcossio333@gmail.com", "Fernando Cossio Empleado", "70000003", true, Set.of(empleadoRol));
        seedUsuario("alexandernetx", "alexander.netx@gmail.com", "Alexander Netx", "70000004", true, Set.of(empleadoRol));

        // 3. Empleados Genéricos
        for (int i = 1; i <= 5; i++) {
            seedUsuario("empleado" + i, "empleado" + i + "@gmail.com", "Empleado Genérico " + i, "7100000" + i, true, Set.of(empleadoRol));
        }

        // 4. Clientes Específicos
        seedUsuario("fcossio0x41", "fcossio0x41@gmail.com", "Fernando Cossio Cliente", "72000001", true, Set.of(clienteRol));
        seedUsuario("axelmqdocs", "axelmq.docs@gmail.com", "Axel Quispe Cliente", "72000002", true, Set.of(clienteRol));

        // 5. Clientes Genéricos
        // 10 activos (cliente1 a cliente10)
        for (int i = 1; i <= 10; i++) {
            seedUsuario("cliente" + i, "cliente" + i + "@gmail.com", "Cliente Genérico Activo " + i, "730000" + (i < 10 ? "0" + i : i), true, Set.of(clienteRol));
        }
        // 5 inactivos (cliente11 a cliente15)
        for (int i = 11; i <= 15; i++) {
            seedUsuario("cliente" + i, "cliente" + i + "@gmail.com", "Cliente Genérico Inactivo " + i, "730000" + i, false, Set.of(clienteRol));
        }
    }

    private void seedUsuario(String username, String email, String nombreCompleto, String telefono, boolean activo, Set<Rol> roles) {
        if (usuarioRepository.existsByUsernameIgnoreCase(username) || usuarioRepository.existsByEmailIgnoreCase(email)) {
            System.out.println("UsuarioSeeder: El usuario o email ya existe. Omitiendo: " + username + " / " + email);
            return;
        }

        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setEmail(email);
        usuario.setNombreCompleto(nombreCompleto);
        usuario.setPassword(passwordEncoder.encode("123123")); // Contraseña requerida: 123123
        usuario.setTelefono(telefono);
        usuario.setActivo(activo);
        usuario.setRoles(roles);

        usuarioRepository.save(usuario);
        System.out.println("Usuario creado: " + username + " (" + email + ") - Activo: " + activo);
    }
}
