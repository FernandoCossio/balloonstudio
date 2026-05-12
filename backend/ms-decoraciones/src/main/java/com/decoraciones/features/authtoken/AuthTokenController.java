package com.decoraciones.features.authtoken;

import com.decoraciones.domain.dtos.auth.ActivarCuentaDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth-token")
@RequiredArgsConstructor
public class AuthTokenController {

    private final AuthTokenService authTokenService;

    @GetMapping("/verify/{token}")
    public ResponseEntity<?> verifyToken(@PathVariable String token) {
        boolean isValid = authTokenService.verificarToken(token);
        if (isValid) {
            return ResponseEntity.ok(Map.of("message", "Token válido"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("message", "Token inválido o expirado"));
        }
    }

    @PostMapping("/activate")
    public ResponseEntity<?> activateAccount(@Valid @RequestBody ActivarCuentaDto dto) {
        try {
            authTokenService.activarCuenta(dto);
            return ResponseEntity.ok(Map.of("message", "Cuenta activada correctamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/resend")
    public ResponseEntity<?> resendEmail(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "El email es obligatorio"));
        }
        try {
            authTokenService.reenviarEmailActivacion(email);
            return ResponseEntity.ok(Map.of("message", "Email de activación reenviado"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
