package com.example.gateway.Filter;

import com.example.gateway.Util.jwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    @Autowired
    private RouteValidator validator;
    @Autowired
    private jwtUtil jwtUtil;

    public AuthenticationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getURI().getPath();

            if (!validator.isSecured(path)) {
                return chain.filter(exchange);
            }

            if (!exchange.getRequest().getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                return Mono.error(new RuntimeException("missing authorization Header"));
            }

            String authHeader = exchange.getRequest().getHeaders().get(HttpHeaders.AUTHORIZATION).get(0);
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                authHeader = authHeader.substring(7);
            }

            try {
                Claims claims = jwtUtil.extractClaims(authHeader);
                String username = claims.getSubject();
                Integer userId = claims.get("userId", Integer.class);

                if (username == null || userId == null) {
                    return Mono.error(new RuntimeException("Missing claims in token"));
                }

                ServerHttpRequest request = exchange.getRequest().mutate()
                        .header("UserId", userId.toString())
                        .header("Username", username)
                        .build();

                return chain.filter(exchange.mutate().request(request).build());
            } catch (Exception e) {
                return Mono.error(new RuntimeException("Unauthorized access to application", e));
            }
        };
    }


    public static class Config {
    }
}
