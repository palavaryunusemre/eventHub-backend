package com.eventhub.config;

import com.eventhub.services.JwtService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        final String jwt = resolveToken(request);
        if (jwt == null) {
            filterChain.doFilter(request,response);
            return;
        }

        final String email;
        try {
            email = jwtService.findEmail(jwt);
        } catch (JwtException | IllegalArgumentException e) {
            // Süresi dolmuş/bozuk token — istek anonim geçmesin, açıkça 401 dönelim ki
            // frontend'in axios interceptor'ı bunu görüp eventhub_refresh ile sessizce yenilesin.
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        if(email!=null && SecurityContextHolder.getContext().getAuthentication()==null){
            try {
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                if(jwtService.tokenControl(jwt,userDetails)){
                    UsernamePasswordAuthenticationToken authenticationToken=new UsernamePasswordAuthenticationToken(
                            userDetails,
                            jwt,
                            userDetails.getAuthorities());
                    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                }
            } catch (UsernameNotFoundException e) {
                // Token geçerli ama kullanıcı artık DB'de yok (silinmiş/eski cookie) —
                // isteği anonim bırak, endpoint kendi yetki kuralına göre karar versin.
            }
        }
        filterChain.doFilter(request,response);
    }

    /** Önce Authorization: Bearer header'ına bakar, yoksa eventhub_jwt cookie'sine düşer. */
    private String resolveToken(HttpServletRequest request) {
        final String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        if (request.getCookies() == null) {
            return null;
        }
        return Arrays.stream(request.getCookies())
                .filter(cookie -> JwtService.COOKIE_NAME.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}
