package com.juneba.erp.controller;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.juneba.erp.DTO.LoginResponse;
import com.juneba.erp.entities.LoginRequest;
import com.juneba.erp.service.JwtService;

@RestController
@RequestMapping(value="/auth")
public class AuthController {
	
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

	 private final AuthenticationManager authenticationManager;
	    private final JwtService jwtService;

	    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService) {
	        this.authenticationManager = authenticationManager;
	        this.jwtService = jwtService;
	    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
        	logger.info("Tentativa de login com o email: {}", request.getEmail());
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
            String jwt = jwtService.generateToken(auth);
            logger.info("Login bem-sucedido para o email: {}", request.getEmail());
            return ResponseEntity.ok(new LoginResponse(jwt));
        } catch (Exception e) {
        	 logger.error("Falha no login para o email: {}. Erro: {}", request.getEmail(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Credenciais inválidas: " + e.getMessage()));
        }
    }

}