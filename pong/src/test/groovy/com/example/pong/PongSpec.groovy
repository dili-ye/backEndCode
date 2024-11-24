package com.example.pong

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.test.web.reactive.server.WebTestClient
import spock.lang.Specification

@WebFluxTest
class PongSpec extends Specification{

    @Autowired
    WebTestClient webTestClient

    def "test normal"() {
        expect:
        request().expectStatus().isOk()
                .expectBody(String.class).isEqualTo("World")
    }

    def "test 429"() {
        sleep(1000)
        println("sleep 1000ms ,time: ${System.currentTimeMillis()}")
        def firstResponse = request()
        when:
            firstResponse.expectStatus().isOk()
            println("first request is ok ,time: ${System.currentTimeMillis()}")
        then:
            def secondResponse = request()
            when:
                secondResponse.expectStatus().isEqualTo(429)
                println("second request is 429 ,time: ${System.currentTimeMillis()}")
            then:
                sleep(500)
                def thirdResponse = request()
                when:
                    thirdResponse.expectStatus().isEqualTo(429)
                    println("third request is 429 ,time: ${System.currentTimeMillis()}")
                then:
                    sleep(500)
                    request().expectStatus().isOk()
                    println("last request is ok ,time: ${System.currentTimeMillis()}")


    }

    def "test thread cycle"() {
        expect:
        Thread.start {
            for (i in 1..11) {
                sleep(100)
                def responseStatus1 = request().expectBody().returnResult().status
                println("${Thread.currentThread().name} ,第 ${i} times request status ${responseStatus1} ,time: ${System.currentTimeMillis()}")
            }
        }
        Thread.start {
            for (i in 1..11) {
                sleep(100)
                def responseStatus2 = request().expectBody().returnResult().getStatus()
                println("${Thread.currentThread().name} ,第 ${i} times request status ${responseStatus2} ,time: ${System.currentTimeMillis()}")
            }
        }

        sleep(3000)
    }

    private WebTestClient.ResponseSpec request() {
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/pong")
                        .queryParam("hello", "Hello")
                        .build())
                .exchange()
    }

}
