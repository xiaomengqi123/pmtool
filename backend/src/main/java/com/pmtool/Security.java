package com.pmtool;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

record CurrentUser(Long id, String username, String role) {}

@Component
class JwtService {
    private final byte[] key; private final long expiryMinutes;
    JwtService(@Value("${pmtool.jwt.secret}") String secret, @Value("${pmtool.jwt.expiry-minutes}") long expiryMinutes) { this.key=secret.getBytes(StandardCharsets.UTF_8); this.expiryMinutes=expiryMinutes; }
    String create(UserAccount user) { return Jwts.builder().subject(String.valueOf(user.id)).claim("username",user.username).claim("role",user.roleCode).issuedAt(java.util.Date.from(Instant.now())).expiration(java.util.Date.from(Instant.now().plus(expiryMinutes, ChronoUnit.MINUTES))).signWith(Keys.hmacShaKeyFor(key)).compact(); }
    CurrentUser parse(String token) { Claims c=Jwts.parser().verifyWith(Keys.hmacShaKeyFor(key)).build().parseSignedClaims(token).getPayload(); return new CurrentUser(Long.valueOf(c.getSubject()),c.get("username",String.class),c.get("role",String.class)); }
}

@Component
class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwt; private final UserRepository users;
    JwtAuthenticationFilter(JwtService jwt,UserRepository users) { this.jwt=jwt;this.users=users; }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String h=request.getHeader(HttpHeaders.AUTHORIZATION);
        if (h!=null && h.startsWith("Bearer ")) try {
            CurrentUser tokenUser=jwt.parse(h.substring(7));
            users.findById(tokenUser.id()).filter(user->!user.deleted&&user.enabled).ifPresent(user->{
                CurrentUser u=new CurrentUser(user.id,user.username,user.roleCode);
                var auth=new UsernamePasswordAuthenticationToken(u,null,List.of(new SimpleGrantedAuthority("ROLE_"+u.role())));
                org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
            });
        } catch (Exception ignored) { }
        chain.doFilter(request,response);
    }
}

@Configuration @EnableWebSecurity @EnableMethodSecurity
class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean CorsConfigurationSource corsConfigurationSource(@Value("${pmtool.cors.allowed-origins}") String origins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::trim).filter(value->!value.isEmpty()).toList());
        config.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization","Content-Type","X-Trace-Id"));
        config.setExposedHeaders(List.of("X-Trace-Id"));
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
    @Bean SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwt, CorsConfigurationSource cors) throws Exception {
        return http.csrf(c->c.disable()).cors(c->c.configurationSource(cors)).sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a->a.requestMatchers("/api/v1/auth/login","/actuator/health","/v3/api-docs/**","/swagger-ui/**","/swagger-ui.html").permitAll().anyRequest().authenticated())
            .exceptionHandling(e->e.authenticationEntryPoint((request,response,exception)->response.sendError(HttpServletResponse.SC_UNAUTHORIZED)))
            .addFilterBefore(jwt, UsernamePasswordAuthenticationFilter.class).build();
    }
}
