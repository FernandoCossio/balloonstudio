package com.decoraciones.auth.userdetails;

import com.decoraciones.common.errors.UsuarioNoEncontradoException;
import com.decoraciones.domain.models.Usuario;
import com.decoraciones.features.usuario.UsuarioRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {

	private final UsuarioRepository usuarioRepository;

	public DatabaseUserDetailsService(UsuarioRepository usuarioRepository) {
		this.usuarioRepository = usuarioRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String usernameOrEmail) {
		Usuario usuario = usuarioRepository.findByUsernameIgnoreCase(usernameOrEmail)
				.or(() -> usuarioRepository.findByEmailIgnoreCase(usernameOrEmail))
				.orElseThrow(UsuarioNoEncontradoException::new);

		return UsuarioPrincipal.from(usuario);
	}
}
