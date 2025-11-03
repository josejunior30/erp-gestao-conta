package com.juneba.erp.controller;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.juneba.erp.Exception.InvalidPasswordException;
import com.juneba.erp.entities.LoginRequest;
import com.juneba.erp.service.JwtService;
import com.juneba.erp.util.HttpRequestUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;


@RestController
@RequestMapping("/auth")
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<Object> login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        final String ip = HttpRequestUtils.clientIp(http);
        try {
            log.info("Login tentativa email={} ip={}", request.getEmail(), ip);
            Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
            String jwt = jwtService.generateToken(auth);
            log.info("Login OK email={} ip={}", request.getEmail(), ip);
            return ResponseEntity.ok(Map.of("token", jwt));
        } catch (BadCredentialsException e) {
            log.warn("Login falhou (senha incorreta) email={} ip={}", request.getEmail(), ip);
            throw new InvalidPasswordException();
        } catch (AuthenticationException e) {
            log.warn("Login falhou (auth) email={} ip={} tipo={}", request.getEmail(), ip, e.getClass().getSimpleName());
            throw e;
        } catch (Exception e) {
            log.error("Login erro 500 email={} ip={} erro={}", request.getEmail(), ip, e.toString());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "erro interno"));
        }
    }
}