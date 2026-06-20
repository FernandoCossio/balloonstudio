package com.decoraciones.features.empleado;

import com.decoraciones.common.errors.AutoDesactivacionException;
import com.decoraciones.common.errors.EmpleadoEmailDuplicadoException;
import com.decoraciones.common.errors.EmpleadoNoEncontradoException;
import com.decoraciones.common.errors.RolNoEncontradoException;
import com.decoraciones.common.errors.UsuarioDuplicadoException;
import com.decoraciones.domain.models.Rol;
import com.decoraciones.domain.models.Usuario;
import com.decoraciones.features.authtoken.AuthTokenService;
import com.decoraciones.features.refreshtoken.RefreshTokenService;
import com.decoraciones.features.rol.RolRepository;
import com.decoraciones.features.usuario.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class EmpleadoService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthTokenService authTokenService;
    private final RefreshTokenService refreshTokenService;

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    @Transactional(readOnly = true)
    public Page<Usuario> findEmpleados(String nombre, String rol, Pageable pageable) {
        return usuarioRepository.findEmpleados(nombre, rol, pageable);
    }

    @Transactional(readOnly = true)
    public Usuario findById(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(EmpleadoNoEncontradoException::new);
        
        boolean esCliente = usuario.getRoles().stream()
                .anyMatch(rol -> "CLIENTE".equalsIgnoreCase(rol.getNombre()));
        if (esCliente) {
            throw new EmpleadoNoEncontradoException();
        }
        
        return usuario;
    }

    public Usuario create(String nombreCompleto, String email, String telefono, String usernameInput) {
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new EmpleadoEmailDuplicadoException();
        }
        String username = (usernameInput != null && !usernameInput.trim().isEmpty()) 
                ? usernameInput.trim() 
                : email;

        if (usuarioRepository.existsByUsernameIgnoreCase(username)) {
            throw new UsuarioDuplicadoException();
        }

        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setEmail(email);
        usuario.setNombreCompleto(nombreCompleto);
        usuario.setTelefono(telefono);
        usuario.setActivo(true); 
        usuario.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));

        Rol rolEmpleado = rolRepository.findByNombre("EMPLEADO")
                .orElseThrow(RolNoEncontradoException::new);
        usuario.setRoles(Set.of(rolEmpleado));

        Usuario guardado = usuarioRepository.save(usuario);

        String token = authTokenService.generarYEnviarTokenActivacion(guardado);

        String urlActivacion = frontendUrl + "/auth/activar-cuenta?token=" + token;
        System.out.println("\n=================================================================================");
        System.out.println("   NUEVO EMPLEADO REGISTRADO: " + guardado.getNombreCompleto());
        System.out.println("   CORREO: " + guardado.getEmail());
        System.out.println("   ENLACE DE ACTIVACIÓN (COPIAR PARA PRUEBAS LOCALES):");
        System.out.println("   " + urlActivacion);
        System.out.println("=================================================================================\n");

        return guardado;
    }

    public Usuario update(Long id, String nombreCompleto, String email, String telefono, String usernameInput) {
        Usuario existente = findById(id);

        if (!existente.getEmail().equalsIgnoreCase(email) && usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new EmpleadoEmailDuplicadoException();
        }
        if (usernameInput != null && !usernameInput.trim().isEmpty()) {
            String nuevoUsername = usernameInput.trim();
            if (!existente.getUsername().equalsIgnoreCase(nuevoUsername) && usuarioRepository.existsByUsernameIgnoreCase(nuevoUsername)) {
                throw new UsuarioDuplicadoException();
            }
            existente.setUsername(nuevoUsername);
        } else {
            if (existente.getUsername().equalsIgnoreCase(existente.getEmail())) {
                if (!existente.getUsername().equalsIgnoreCase(email)) {
                    if (usuarioRepository.existsByUsernameIgnoreCase(email)) {
                        throw new UsuarioDuplicadoException();
                    }
                    existente.setUsername(email);
                }
            }
        }

        existente.setNombreCompleto(nombreCompleto);
        existente.setEmail(email);
        existente.setTelefono(telefono);

        return usuarioRepository.save(existente);
    }

    public void deactivate(Long empleadoId, Long currentAdminId) {
        if (empleadoId.equals(currentAdminId)) {
            throw new AutoDesactivacionException();
        }

        Usuario empleado = findById(empleadoId);
        empleado.setActivo(false);
        usuarioRepository.save(empleado);

        refreshTokenService.revokeAllByUsuario(empleadoId);
        
        System.out.println(" Cuenta de empleado desactivada y sesiones invalidadas para el ID: " + empleadoId);
    }

    public void activate(Long empleadoId) {
        Usuario empleado = findById(empleadoId);
        empleado.setActivo(true);
        usuarioRepository.save(empleado);
        
        System.out.println(" Cuenta de empleado reactivada para el ID: " + empleadoId);
    }
}
