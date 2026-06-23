package com.decoraciones.config;

import com.decoraciones.domain.models.Rol;
import com.decoraciones.domain.models.Usuario;
import com.decoraciones.domain.models.FactorEstacional;
import com.decoraciones.features.rol.RolRepository;
import com.decoraciones.features.usuario.UsuarioRepository;
import com.decoraciones.features.reserva.FactorEstacionalRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Set;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final FactorEstacionalRepository factorEstacionalRepository;

    public DataSeeder(UsuarioRepository usuarioRepository,
                      RolRepository rolRepository,
                      PasswordEncoder passwordEncoder,
                      FactorEstacionalRepository factorEstacionalRepository) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.factorEstacionalRepository = factorEstacionalRepository;
    }

    @Override
    public void run(String... args) {
        seedRoles();
        seedAdminUser();
        seedFactoresEstacionales();
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

    private void seedFactoresEstacionales() {
        // Factores estacionales de ejemplo para los 12 meses
        double[] factores = {
            0.90, // Enero
            1.00, // Febrero
            1.00, // Marzo
            1.10, // Abril (Semana Santa / Eventos)
            1.20, // Mayo (Día de la madre / Bodas)
            1.00, // Junio
            1.00, // Julio
            0.90, // Agosto (Temporada baja)
            1.20, // Septiembre (Primavera)
            1.10, // Octubre
            1.15, // Noviembre
            1.30  // Diciembre (Navidad / Fin de año - Temporada alta)
        };
        String[] descripciones = {
            "Enero (Bajo)", "Febrero (Normal)", "Marzo (Normal)", "Abril (Medio-Alto)",
            "Mayo (Alto)", "Junio (Normal)", "Julio (Normal)", "Agosto (Bajo)",
            "Septiembre (Alto)", "Octubre (Medio)", "Noviembre (Medio-Alto)", "Diciembre (Muy Alto)"
        };

        for (int i = 0; i < 12; i++) {
            int mes = i + 1;
            if (factorEstacionalRepository.findByMes(mes).isEmpty()) {
                FactorEstacional fe = new FactorEstacional();
                fe.setMes(mes);
                fe.setDescripcion(descripciones[i]);
                fe.setFactorEstacional(BigDecimal.valueOf(factores[i]));
                factorEstacionalRepository.save(fe);
                log.info("[Seeder] Factor estacional para mes {} ({}) creado: {}.", mes, descripciones[i], factores[i]);
            }
        }
    }
}
