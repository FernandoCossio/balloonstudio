package com.decoraciones.auth.userdetails;

import com.decoraciones.domain.models.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class UsuarioPrincipal implements UserDetails {

    private final Long id;
    private final String username;
    private final String password;
    private final boolean activo;
    private final Set<GrantedAuthority> authorities;

    private UsuarioPrincipal(
            Long id,
            String username,
            String password,
            boolean activo,
            Set<GrantedAuthority> authorities) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.activo = activo;
        this.authorities = authorities;
    }

    /**
     * Factory method: único punto de transformación Usuario → UserDetails.
     */
    public static UsuarioPrincipal from(Usuario usuario) {
        Set<GrantedAuthority> authorities = usuario.getRoles().stream()
                .map(rol -> rol.getNombre())
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(nombre -> !nombre.isEmpty())
                .map(nombre -> nombre.startsWith("ROLE_") ? nombre : "ROLE_" + nombre)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toUnmodifiableSet());

        return new UsuarioPrincipal(
                usuario.getId(),
                usuario.getUsername(),
                usuario.getPassword(),
                Boolean.TRUE.equals(usuario.getActivo()),
                authorities
        );
    }

    public Long getId() {
        return id;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public boolean isEnabled() {
        return activo;
    }

    @Override public boolean isAccountNonExpired()    { return true; }
    @Override public boolean isAccountNonLocked()     { return true; }
    @Override public boolean isCredentialsNonExpired(){ return true; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UsuarioPrincipal other)) return false;
        return Objects.equals(username, other.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username);
    }
}
