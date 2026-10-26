package com.yassir.jobflow.service;

import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.stereotype.Service;
import com.yassir.jobflow.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;

    public JwtService(JwtEncoder jwtEncoder) {
        this.jwtEncoder = jwtEncoder;
    }
    @Value("${jwt.expiration}")
private long jwtExpiration;

public String generateToken(User user) {

    Instant now = Instant.now();

    JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer("jobflow")
            .issuedAt(now)
            .expiresAt(now.plus(jwtExpiration, ChronoUnit.SECONDS))
            .subject(user.getEmail())
            .claim("role", user.getRole().name())
            .claim("userId", user.getId())
            .build();

   JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

return jwtEncoder.encode(
        JwtEncoderParameters.from(header, claims)
).getTokenValue();
}
}