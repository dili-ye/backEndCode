package com.example.pong.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@RestController
public class PongController {

    private static final AtomicLong timestamp = new AtomicLong(0);

    @RequestMapping(value = "/pong", method = RequestMethod.GET)
    public Mono<String> pong(@RequestParam("hello") String hello, ServerWebExchange exchange) {
        return Mono.just(System.nanoTime())
                .handle((currentTime, sink) -> {
                    boolean flag = timestamp.updateAndGet(lastTime -> {
                        if ((currentTime - lastTime) >= TimeUnit.SECONDS.toNanos(1)) {
                            return currentTime;
                        }
                        return lastTime;
                    }) == currentTime;

                    if (flag) {
                        log.info("receive a message , address:{}, key :{} , value:World",exchange.getRequest().getURI().getHost()+":"+exchange.getRequest().getURI().getPort(),hello);
                        sink.next("World");
                    } else {
                        log.error("receive a message , but to many request,address:{} ,key :{}",exchange.getRequest().getURI().getHost()+":"+exchange.getRequest().getURI().getPort(),hello);
                        sink.error(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS));
                    }
                });
    }
}
