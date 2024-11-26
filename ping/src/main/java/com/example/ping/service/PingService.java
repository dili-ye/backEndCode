package com.example.ping.service;

import com.example.ping.constant.RequestTypeEnum;
import com.example.ping.entity.PingPong;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.LocalDateTime;

import static com.example.ping.constant.RequestTypeEnum.*;

@Service
public class PingService {

    private final WebClient webClient ;

    private final RocketMQTemplate rocketMQTemplate;

    public PingService(WebClient webClient,RocketMQTemplate rocketMQTemplate) {
        this.webClient = webClient ;
        this.rocketMQTemplate = rocketMQTemplate;
    }

    /**
     * request pong
     * @return
     */
    public Mono<ResponseEntity<String>> requestPong() {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/pong").queryParam("hello", "Hello").build())
                .retrieve()
                .toEntity(String.class);
    }


    public void requestProcess(PingPong pingPong) {
        requestPong()
                .subscribe(response -> {
                    if (response.getStatusCode().is2xxSuccessful()) {
                        processPingPongAndMQSend(pingPong,OK,response.getBody());
                    } else if (response.getStatusCode().value() == 429) {
                        processPingPongAndMQSend(pingPong,TOO_MANY_REQUEST,null);
                    } else {
                        processPingPongAndMQSend(pingPong,UNKNOWN_ERROR,response.getBody());
                    }
                }, error -> {
                    handleError(error, pingPong);
                });
    }

    public void processPingPongAndMQSend(PingPong pingPong, RequestTypeEnum typeEnum, String desc) {
        if(typeEnum!=null){
            pingPong.setRequestType(typeEnum.getCode());
            pingPong.setDescription(typeEnum.getDesc(desc));
            if(REQUEST_LIMIT!=typeEnum){
                pingPong.setResponseTime(LocalDateTime.now());
            }
        }
        rocketMQTemplate.convertAndSend("ping_pong", pingPong);
    }

    private void handleError(Throwable error,PingPong pingPong) {

        if(error instanceof ResponseStatusException){
            ResponseStatusException responseStatusException = (ResponseStatusException) error;
            if(responseStatusException.getStatus().value() == HttpStatus.TOO_MANY_REQUESTS.value()){
                processPingPongAndMQSend(pingPong,TOO_MANY_REQUEST,null);
                return ;
            }
        }
        if (error instanceof WebClientResponseException) {
            WebClientResponseException webClientResponseException = (WebClientResponseException) error;
            if (webClientResponseException.getStatusCode().value() == HttpStatus.TOO_MANY_REQUESTS.value()) {
                processPingPongAndMQSend(pingPong,TOO_MANY_REQUEST,null);
                return ;
            }
        }
        processPingPongAndMQSend(pingPong,UNKNOWN_ERROR,error.getMessage());
    }

}
