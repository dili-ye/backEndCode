package com.example.pong

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.test.web.reactive.server.WebTestClient
import spock.lang.Specification

import java.util.concurrent.atomic.AtomicInteger

@WebFluxTest
class PongSpec extends Specification{

    @Autowired
    WebTestClient webTestClient

    def "test normal"() {
        expect:
        sleep(1000)
        request().expectStatus().isOk()
                .expectBody(String.class).isEqualTo("World")
    }

    def "test 429"() {
        sleep(1000)
        def firstResponse = request()
        when:
            firstResponse.expectStatus().isOk()
        then:
            def secondResponse = request()
            when:
                secondResponse.expectStatus().isEqualTo(429)
            then:
                sleep(500)
                def thirdResponse = request()
                when:
                    thirdResponse.expectStatus().isEqualTo(429)
                then:
                    sleep(500)
                    request().expectStatus().isOk()


    }

    def "test thread cycle"() {

        sleep(1000)
        AtomicInteger okCount = new AtomicInteger(0)
        when:
        Thread.start {
            for (i in 1..11) {
                sleep(100)
                def responseStatus1 = request().returnResult(Void.class).status
                println("${Thread.currentThread().name} ,第 ${i} times request ,status ${responseStatus1} ,time: ${System.currentTimeMillis()}")
                if(responseStatus1.value()==200){
                    okCount.incrementAndGet()
                }
            }
        }
        Thread.start {
            for (i in 1..11) {
                sleep(100)
                def responseStatus2 = request().returnResult(Void.class).status
                println("${Thread.currentThread().name} ,第 ${i} times request ,status ${responseStatus2} ,time: ${System.currentTimeMillis()}")
                if( responseStatus2.value()==200){
                    okCount.incrementAndGet()
                }
            }
        }

        sleep(3000)

        then:
        okCount.get()==2

    }

    private WebTestClient.ResponseSpec request() {
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/pong")
                        .queryParam("hello", "Hello")
                        .build())
                .exchange()
    }

}
