package com.example.OrderService.client;

import com.example.OrderService.dto.user.UserResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

@Service
public class UserClient {

    private final WebClient webClient;

    public UserClient(WebClient userServiceClient) {
        this.webClient = userServiceClient;
    }

    @CircuitBreaker(name = "userService", fallbackMethod = "fallbackUser")
    public UserResponse getUserById(Long id) {

        String token = (String) SecurityContextHolder.getContext().getAuthentication().getCredentials();

        return webClient.get()
                .uri("/users/{id}", id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(UserResponse.class)
                .block();
    }

    public UserResponse fallbackUser(Long id, Throwable t) {

        UserResponse fallback = new UserResponse();
        fallback.setId(id);

        return fallback;
    }

}
