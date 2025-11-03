package com.juneba.erp.service;

import java.time.Instant;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import com.juneba.erp.entities.SecurityUserDetails;

@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final long expirationSec;

    public JwtService(JwtEncoder encoder,
                      JwtDecoder decoder,
                      @Value("${app.jwt.expiration-seconds:3600}") long expirationSec) {
        this.jwtEncoder = encoder;
        this.jwtDecoder = decoder;
        this.expirationSec = expirationSec;
    }

    public String generateToken(Authentication authentication) {
        SecurityUserDetails principal = (SecurityUserDetails) authentication.getPrincipal();
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(expirationSec);

        String authorities = authentication.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .collect(Collectors.joining(","));

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("demo-app")
                .issuedAt(now)
                .expiresAt(exp)
                .subject(String.valueOf(principal.getId()))
                .claim("email", principal.getUsername())
                .claim("roles", authorities)
                .build();

        JwsHeader jwsHeader = JwsHeader.with(SignatureAlgorithm.RS256).build(); // RS256 assimétrico
        JwtEncoderParameters params = JwtEncoderParameters.from(jwsHeader, claims);
        return jwtEncoder.encode(params).getTokenValue();
    }

    public Jwt parseToken(String token) {
        return jwtDecoder.decode(token);
    }
}
