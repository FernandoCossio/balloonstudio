package com.decoraciones.features.authtoken;

import com.decoraciones.common.errors.AuthTokenInvalidoException;
import com.decoraciones.common.errors.AuthTokenNoEncontradoException;
import com.decoraciones.common.errors.CuentaYaActivaException;
import com.decoraciones.common.errors.EmailObligatorioException;
import com.decoraciones.common.errors.PasswordNoCoincideException;
import com.decoraciones.common.errors.UsuarioNoEncontradoException;
import com.decoraciones.domain.dtos.auth.ActivarCuentaDto;
import com.decoraciones.domain.enums.auth.TipoToken;
import com.decoraciones.domain.models.AuthToken;
import com.decoraciones.domain.models.Usuario;
import com.decoraciones.features.usuario.UsuarioRepository;
import com.decoraciones.services.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthTokenService {

    private final AuthTokenRepository authTokenRepository;
    private final UsuarioRepository usuarioRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    @Transactional
    public String generarYEnviarTokenActivacion(Usuario usuario) {
        // Revocar tokens anteriores de activacion
        authTokenRepository.revokeAllByUsuarioIdAndTipo(usuario.getId(), TipoToken.ACTIVACION_CUENTA);

        // Crear nuevo token
        String token = UUID.randomUUID().toString();
        AuthToken authToken = new AuthToken();
        authToken.setToken(token);
        authToken.setUsuario(usuario);
        authToken.setTipo(TipoToken.ACTIVACION_CUENTA);
        authToken.setExpiresAt(LocalDateTime.now().plusHours(24));
        authToken.setIsRevoked(false);

        authTokenRepository.save(authToken);

        // Enviar email
        String enlace = frontendUrl + "/auth/activar-cuenta?token=" + token;
        String mensaje = """
        <div style="background:#f5f5f5;padding:2rem;font-family:Arial,sans-serif;">
          <div style="max-width:580px;margin:0 auto;background:#fff;border-radius:12px;overflow:hidden;border:0.5px solid #ddd;">
    
            <div style="background:#1a1a1a;padding:2rem;text-align:center;">
              <div style="text-align:center;line-height:1.1;">
                <div style="font-size:22px;font-weight:900;color:#fff;font-style:italic;">Ball<span style="color:#E87DA8;">o</span><span style="color:#6EC6D4;">o</span>n</div>
                <div style="font-size:13px;color:#fff;font-style:italic;letter-spacing:1px;">studio</div>
              </div>
              <div style="height:3px;background:linear-gradient(90deg,#E87DA8 0%%,#6EC6D4 100%%);margin-top:1.5rem;"></div>
            </div>
    
            <div style="padding:2.5rem 2.5rem 1.5rem;">
              <p style="font-size:13px;color:#999;text-transform:uppercase;letter-spacing:2px;margin:0 0 0.5rem;">¡Bienvenido/a!</p>
              <h1 style="font-size:26px;font-weight:800;color:#1a1a1a;margin:0 0 1.5rem;">Hola, <span style="color:#E87DA8;">%s</span> 🎉</h1>
              <p style="font-size:15px;color:#444;line-height:1.7;margin:0 0 1rem;">
                Gracias por registrarte en <strong>Balloon Studio</strong>. Estamos emocionados de tenerte con nosotros para hacer tus eventos inolvidables.
              </p>
              <p style="font-size:15px;color:#444;line-height:1.7;margin:0 0 2rem;">
                Para comenzar, activá tu cuenta haciendo clic en el botón de abajo:
              </p>
              <div style="text-align:center;margin:0 0 2rem;">
                <a href="%s" style="display:inline-block;background:#E87DA8;color:#fff;font-size:15px;font-weight:700;padding:14px 40px;border-radius:50px;text-decoration:none;">
                  ✓ Activar mi cuenta
                </a>
              </div>
              <div style="background:#f9f9f9;border-radius:8px;padding:1rem 1.25rem;border-left:4px solid #6EC6D4;margin:0 0 2rem;">
                <p style="font-size:13px;color:#666;margin:0;line-height:1.6;">
                  <strong style="color:#444;">⏰ Este enlace expira en 24 horas.</strong><br>
                  Si no creaste esta cuenta, podés ignorar este correo.
                </p>
              </div>
              <p style="font-size:14px;color:#888;line-height:1.6;margin:0;">
                Si el botón no funciona, copiá este enlace en tu navegador:<br>
                <span style="color:#E87DA8;font-size:12px;word-break:break-all;">%s</span>
              </p>
            </div>
    
            <div style="border-top:0.5px solid #eee;background:#fafafa;padding:1.5rem 2.5rem;">
              <div style="display:flex;justify-content:space-between;flex-wrap:wrap;gap:12px;">
                <div>
                  <p style="font-size:13px;font-weight:700;color:#1a1a1a;margin:0 0 2px;">Balloon Studio</p>
                  <p style="font-size:12px;color:#999;margin:0;">Decoraciones & Eventos</p>
                </div>
                <div style="text-align:right;">
                  <p style="font-size:12px;color:#999;margin:0 0 2px;">📧 contacto@balloonstudio.bo</p>
                  <p style="font-size:12px;color:#999;margin:0 0 2px;">📱 +591 7X XXX XXX</p>
                  <p style="font-size:12px;color:#999;margin:0;">📍 Santa Cruz de la Sierra, Bolivia</p>
                </div>
              </div>
              <div style="border-top:0.5px solid #eee;margin-top:1rem;padding-top:0.75rem;text-align:center;">
                <p style="font-size:11px;color:#bbb;margin:0;">© 2025 Balloon Studio · Todos los derechos reservados</p>
              </div>
            </div>
    
          </div>
        </div>
        """.formatted(usuario.getNombreCompleto(), enlace, enlace);

        emailService.sendEmail(usuario.getEmail(), "Activación de cuenta - Decoraciones", mensaje);
        return token;
    }

    @Transactional(readOnly = true)
    public void verificarToken(String token) {
        boolean isValid = authTokenRepository.findByTokenAndTipo(token, TipoToken.ACTIVACION_CUENTA)
                .map(AuthToken::isValid)
                .orElse(false);

        if (!isValid) {
            throw new AuthTokenInvalidoException();
        }
    }

    @Transactional
    public void activarCuenta(ActivarCuentaDto dto) {
        AuthToken authToken = authTokenRepository.findByTokenAndTipo(dto.token(), TipoToken.ACTIVACION_CUENTA)
                .orElseThrow(AuthTokenNoEncontradoException::new);

        if (!authToken.isValid()) {
            throw new AuthTokenInvalidoException();
        }

        if (!dto.password().equals(dto.confirmPassword())) {
            throw new PasswordNoCoincideException();
        }

        Usuario usuario = authToken.getUsuario();
        usuario.setPassword(passwordEncoder.encode(dto.password()));
        usuario.setActivo(true);
        usuarioRepository.save(usuario);

        authToken.setUsedAt(LocalDateTime.now());
        authTokenRepository.save(authToken);
    }

    @Transactional
    public void reenviarEmailActivacion(String email) {
        if (email == null || email.isBlank()) {
            throw new EmailObligatorioException();
        }

        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(UsuarioNoEncontradoException::new);

        if (usuario.getActivo()) {
            throw new CuentaYaActivaException();
        }

        generarYEnviarTokenActivacion(usuario);
    }
}
