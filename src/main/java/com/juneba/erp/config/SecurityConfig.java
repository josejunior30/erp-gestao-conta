package com.juneba.erp.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.security.KeyFactory;
import org.springframework.core.io.Resource;
import com.nimbusds.jose.jwk.RSAKey;


@Configuration
public class SecurityConfig {
    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Value("${app.jwt.private-key}")
    private Resource privateKeyPem;

    @Value("${app.jwt.public-key}")
    private Resource publicKeyPem;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }

    @Bean
    public RSAPublicKey rsaPublicKey() {
        try {
            byte[] der = readPemBlock(publicKeyPem, "PUBLIC KEY");
            X509EncodedKeySpec spec = new X509EncodedKeySpec(der);
            return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Falha ao carregar public.pem (X.509): " + e.getMessage(), e);
        }
    }

    @Bean
    public RSAPrivateKey rsaPrivateKey() {
        try {
            byte[] der = readPemBlock(privateKeyPem, "PRIVATE KEY"); // requer PKCS#8
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(der);
            return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(spec);
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Falha ao carregar private.pem (PKCS#8): " + e.getMessage(), e);
        }
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource(RSAPublicKey pub, RSAPrivateKey priv) {
        RSAKey jwk = new RSAKey.Builder(pub).privateKey(priv).keyID("rsa-key-1").build();
        JWKSet set = new JWKSet(jwk);
        return (selector, ctx) -> selector.select(set);
    }

    @Bean
    public JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        log.debug("Inicializando JwtEncoder (RS256).");
        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    public JwtDecoder jwtDecoder(RSAPublicKey pub) {
        log.debug("Inicializando JwtDecoder (RS256).");
        return NimbusJwtDecoder.withPublicKey(pub).build();
    }

    // Apenas PKCS#8 (privada) e X.509 (pública). Se tiver PKCS#1, converta antes.
    private static byte[] readPemBlock(Resource pem, String type) throws IOException {
        String text = new String(pem.getInputStream().readAllBytes(), StandardCharsets.US_ASCII);
        String begin = "-----BEGIN " + type + "-----";
        String end = "-----END " + type + "-----";
        int i = text.indexOf(begin);
        int j = text.indexOf(end);
        if (i < 0 || j < 0) {
            throw new IOException("PEM inválido, esperado blocos " + begin + " ... " + end);
        }
        String base64 = text.substring(i + begin.length(), j).replaceAll("\\s", "");
        return Base64.getDecoder().decode(base64);
    }
}