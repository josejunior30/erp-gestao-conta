package com.juneba.erp.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
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

	 private final AuthenticationManager authenticationManager;
	    private final JwtService jwtService;

	    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService) {
	        this.authenticationManager = authenticationManager;
	        this.jwtService = jwtService;
	    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
            String jwt = jwtService.generateToken(auth);
            return ResponseEntity.ok(new LoginResponse(jwt));
        } catch (Exception e) {
           
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Credenciais inválidas: " + e.getMessage()));
        }
    }

}