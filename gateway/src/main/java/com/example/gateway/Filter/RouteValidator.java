package com.example.gateway.Filter;


import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;


import java.util.*;
import java.util.function.Predicate;


@Component
public class RouteValidator {


    public static  final List<String> openApiEndpoints = List.of(
            "/User/api/v1/auth/register",
            "/User/api/v1/auth/login",
            "/eureka"

    );
    public Predicate<ServerHttpRequest> isSecured =
            request -> openApiEndpoints
                    .stream()
                    .noneMatch(uri ->request.getURI().getPath().contains(uri)
                    );
    public boolean isSecured(String path) {
        return openApiEndpoints.stream().noneMatch(path::contains);
    }

//    public boolean isPublic(String path) {
//        return openApiEndpoints.stream().anyMatch(path::contains);
//    }
}
