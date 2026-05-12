package com.decoraciones.features.usuario;

import com.decoraciones.common.errors.UsuarioDuplicadoException;
import com.decoraciones.domain.dtos.usuario.RegistrarClienteDto;
import com.decoraciones.domain.dtos.usuario.ResponseUsuarioDto;
import com.decoraciones.domain.models.Rol;
import com.decoraciones.domain.models.Usuario;
import com.decoraciones.features.authtoken.AuthTokenService;
import com.decoraciones.features.rol.RolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthTokenService authTokenService;

    @Transactional
    public ResponseUsuarioDto registrarCliente(RegistrarClienteDto dto) {
        if (usuarioRepository.findByUsernameIgnoreCase(dto.username()).isPresent()) {
            throw new UsuarioDuplicadoException();
        }
        if (usuarioRepository.findByEmailIgnoreCase(dto.email()).isPresent()) {
            throw new UsuarioDuplicadoException();
        }

        Usuario usuario = new Usuario();
        usuario.setUsername(dto.username());
        usuario.setEmail(dto.email());
        usuario.setNombreCompleto(dto.nombreCompleto());
        // Se pone una contraseña aleatoria temporal, ya que se definirá en la activación
        usuario.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        usuario.setTelefono(dto.telefono());
        usuario.setActivo(false);

        Rol rolCliente = rolRepository.findByNombre("CLIENTE")
                .orElseThrow(() -> new RuntimeException("Rol CLIENTE no encontrado"));
        
        usuario.setRoles(Set.of(rolCliente));

        Usuario guardado = usuarioRepository.save(usuario);

        // Generar y enviar token de activación
        authTokenService.generarYEnviarTokenActivacion(guardado);

        return mapToDto(guardado);
    }

    private ResponseUsuarioDto mapToDto(Usuario usuario) {
        return new ResponseUsuarioDto(
                usuario.getUuid(),
                usuario.getUsername(),
                usuario.getEmail(),
                usuario.getNombreCompleto(),
                usuario.getTelefono(),
                usuario.getRoles().stream().map(Rol::getNombre).collect(Collectors.toSet())
        );
    }
}
