package com.juneba.erp.Exception;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleBodyValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, List<String>> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.groupingBy(
                        FieldError::getField,
                        LinkedHashMap::new,
                        Collectors.mapping(fe -> Optional.ofNullable(fe.getDefaultMessage()).orElse("inválido"),
                                Collectors.toList())
                ));

        log.warn("Validação 400 path={} fields={}", req.getRequestURI(), String.join(",", errors.keySet()));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "https://errors.juneba.com/validation-error");
        body.put("title", "Erro de validação");
        body.put("status", 400);
        body.put("errors", errors);

        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<Object> handleInvalidPassword(InvalidPasswordException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "credenciais inválidas"));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Object> handleAuth(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "credenciais inválidas"));
    }
    
    @ExceptionHandler(UpstreamException.class)
    public ResponseEntity<Object> handleUpstream(UpstreamException ex, HttpServletRequest req) {
        int us = ex.getHttpStatus();
        HttpStatus mapped;
        if (us == 400 || us == 404 || us == 409 || us == 422 || us == 429) {
            mapped = HttpStatus.valueOf(us);
        } else {
            mapped = HttpStatus.BAD_GATEWAY; 
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "https://errors.juneba.com/upstream-error");
        body.put("title", mapped.is4xxClientError() ? "Erro de solicitação" : "Falha no provedor externo");
        body.put("status", mapped.value());
        body.put("detail", mapped.is4xxClientError() ? "Requisição inválida ao provedor" : "Comunicação com provedor falhou");
        body.put("instance", req.getRequestURI());
        body.put("upstreamStatus", us);
        body.put("endpoint", ex.getEndpoint());
        if (!mapped.is4xxClientError()) body.put("snippet", ex.getResponseSnippet()); 

        return ResponseEntity.status(mapped).body(body);
    }

    @ExceptionHandler(UpstreamIoException.class)
    public ResponseEntity<Object> handleUpstreamIo(UpstreamIoException ex, HttpServletRequest req) {
        log.error("Upstream I/O path={} endpoint={} msg={}", req.getRequestURI(), ex.getEndpoint(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of(
                        "type", "https://errors.juneba.com/upstream-io",
                        "title", "Falha de comunicação com provedor externo",
                        "status", 502,
                        "endpoint", ex.getEndpoint()
                ));
    }
}