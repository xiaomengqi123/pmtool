package com.pmtool;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/** Keeps one safe correlation id for the lifetime of an HTTP request. */
@Component
class TraceIdFilter extends OncePerRequestFilter {
    static final String HEADER = "X-Trace-Id";
    private static final String MDC_KEY = "traceId";

    static String current() {
        String traceId = MDC.get(MDC_KEY);
        return traceId == null ? "system" : traceId;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requested = request.getHeader(HEADER);
        String traceId = requested != null && requested.matches("[A-Za-z0-9][A-Za-z0-9_-]{0,63}")
                ? requested : UUID.randomUUID().toString();
        MDC.put(MDC_KEY, traceId);
        response.setHeader(HEADER, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
