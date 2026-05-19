package com.decoraciones.features.authtoken;

import com.decoraciones.common.response.ApiResponse;
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
    public ResponseEntity<ApiResponse<Void>> verifyToken(@PathVariable String token) {
        authTokenService.verificarToken(token);
        return ResponseEntity.ok(ApiResponse.success(null, "Token válido"));
    }

    @PostMapping("/activate")
    public ResponseEntity<ApiResponse<Void>> activateAccount(@Valid @RequestBody ActivarCuentaDto dto) {
        authTokenService.activarCuenta(dto);
        return ResponseEntity.ok(ApiResponse.success(null, "Cuenta activada correctamente"));
    }

    @PostMapping("/resend")
    public ResponseEntity<ApiResponse<Void>> resendEmail(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        authTokenService.reenviarEmailActivacion(email);
        return ResponseEntity.ok(ApiResponse.success(null, "Email de activación reenviado"));
    }
}
