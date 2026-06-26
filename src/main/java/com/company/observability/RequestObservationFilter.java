package com.company.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RequestObservationFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(RequestObservationFilter.class);
    private final SqlStatementCounter sqlStatementCounter;

    public RequestObservationFilter(SqlStatementCounter sqlStatementCounter) {
        this.sqlStatementCounter = sqlStatementCounter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String correlationId = Optional.ofNullable(request.getHeader("X-Correlation-Id"))
                .filter(value -> !value.isBlank())
                .orElseGet(() -> UUID.randomUUID().toString());
        long start = System.nanoTime();
        sqlStatementCounter.reset();
        MDC.put("correlationId", correlationId);
        response.setHeader("X-Correlation-Id", correlationId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            int sqlStatements = sqlStatementCounter.current();
            if (!response.isCommitted()) {
                response.setHeader("X-SQL-Statement-Count", Integer.toString(sqlStatements));
            }
            log.info("request method={} path={} status={} elapsedMs={} sqlStatements={} correlationId={}", request.getMethod(), request.getRequestURI(), response.getStatus(), elapsedMs, sqlStatements, correlationId);
            sqlStatementCounter.clear();
            MDC.clear();
        }
    }
}
