package com.orabank.backend.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authentifie la requete a partir de l'en-tete {@code Authorization: Bearer ...}.
 * L'employe est relu en base a chaque appel : une desactivation prend effet
 * immediatement, sans attendre l'expiration du jeton.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** Code d'erreur pose sur la requete, relu par SecurityConfig pour repondre 401. */
    public static final String ERROR_ATTRIBUTE = "authError";

    private final AppUserDetailsService userDetailsService;
    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        try {
            String matricule = jwtService.parseClaims(header.substring(7).trim()).getSubject();
            AppUserDetails user = userDetailsService.loadUserByUsername(matricule);
            if (user.isEnabled()) {
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
            } else {
                log.info("Jeton presente pour un compte desactive ({}).", matricule);
                request.setAttribute(ERROR_ATTRIBUTE, "TOKEN_INVALID");
            }
        } catch (ExpiredJwtException expired) {
            request.setAttribute(ERROR_ATTRIBUTE, "TOKEN_EXPIRED");
        } catch (JwtException | UsernameNotFoundException | IllegalArgumentException invalid) {
            request.setAttribute(ERROR_ATTRIBUTE, "TOKEN_INVALID");
        }

        chain.doFilter(request, response);
    }
}
