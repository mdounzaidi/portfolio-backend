package com.mdounzaidi.portfolio_backend.account.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.HashMap;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtAuthorityConverterTest {

    private final JwtAuthorityConverter converter = new JwtAuthorityConverter();

    @Test
    void convert_shouldCreateAuthoritiesFromRolesClaim() {
        Jwt jwt = jwtWithClaims(Map.of("roles", List.of("ROLE_USER", "ROLE_ADMIN")));

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertEquals(
                List.of("ROLE_USER", "ROLE_ADMIN"),
                authorities.stream().map(GrantedAuthority::getAuthority).toList()
        );
    }

    @Test
    void convert_shouldReturnEmptyAuthorities_whenRolesClaimIsMissing() {
        Jwt jwt = jwtWithClaims(Map.of());

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertTrue(authorities.isEmpty());
    }

    private Jwt jwtWithClaims(Map<String, Object> claims) {
        Instant now = Instant.now();
        Map<String, Object> jwtClaims = new HashMap<>();
        jwtClaims.put("sub", "testuser");
        jwtClaims.putAll(claims);

        return new Jwt(
                "token",
                now,
                now.plusSeconds(60),
                Map.of("alg", "HS256"),
                jwtClaims
        );
    }
}
