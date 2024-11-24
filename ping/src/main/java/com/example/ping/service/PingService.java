package com.example.ping.service;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class PingService {

    private final WebClient webClient = WebClient.create("http://localhost:8081");

    public Mono<ResponseEntity<String>> pingPong() {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/pong").queryParam("hello", "Hello").build())
                .retrieve()
                .toEntity(String.class);
    }

}
