package com.example.ping

import com.example.ping.constant.RequestTypeEnum
import com.example.ping.dao.PingPongDao
import com.example.ping.entity.PingPong
import com.example.ping.service.PingScheduler
import com.example.ping.service.PingService
import org.apache.rocketmq.spring.core.RocketMQTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono
import spock.lang.Specification

import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

@SpringBootTest
@ActiveProfiles("test")
class PingSpec extends Specification{

    def "test ping 200"() {
        given:
        sleep(1000)
        def mockWebClient = Mock(WebClient)
        def mockWebClientBuilder = Mock(WebClient.Builder)
        def mockWebClientRequest = Mock(WebClient.RequestBodyUriSpec)
        def mockWebClientResponse = Mock(WebClient.ResponseSpec)
        mockWebClient.get() >> mockWebClientRequest
        mockWebClientRequest.uri(_) >> mockWebClientRequest
        mockWebClientRequest.retrieve() >> mockWebClientResponse
        mockWebClientResponse.toEntity(String.class) >> Mono.just(ResponseEntity.ok("World"))
        mockWebClientBuilder.baseUrl("http://localhost:8081").build() >> mockWebClient

        def mockRocketMQTemplate = Mock(RocketMQTemplate)

        def pingService = Spy(new PingService(mockWebClient ,mockRocketMQTemplate))
        def pingScheduler = Spy(new PingScheduler(pingService))

        when:
        pingScheduler.schedulePing()
        sleep(1000)
        then:
        1 * pingService.processPingPongAndMQSend(_, RequestTypeEnum.OK,"World")
        1 * mockRocketMQTemplate.convertAndSend("ping_pong",_)
    }

    def "test ping limit"() {
        given:
        sleep(1000)
        //访问限制文件设置两个一秒内的数值, 造成限制
        File lockFile = new File(PingScheduler.LOCK_FILE_PATH)
        lockFile.write(System.currentTimeMillis().toString() + System.lineSeparator())
        lockFile.append((System.currentTimeMillis() - 500).toString() + System.lineSeparator())
        def mockWebClient = Mock(WebClient)
        def mockRocketMQTemplate = Mock(RocketMQTemplate)
        def pingService = Spy(new PingService(mockWebClient ,mockRocketMQTemplate))
        def pingScheduler = Spy(new PingScheduler(pingService))

        when:
        pingScheduler.schedulePing()
        sleep(1000)
        then:
        1 * pingService.processPingPongAndMQSend(_, RequestTypeEnum.REQUEST_LIMIT,null)
        1 * mockRocketMQTemplate.convertAndSend("ping_pong",_)
    }


    def "test ping 429"() {
        given:
        sleep(1000)
        def mockWebClient = Mock(WebClient)
        def mockWebClientBuilder = Mock(WebClient.Builder)
        def mockWebClientRequest = Mock(WebClient.RequestBodyUriSpec)
        def mockWebClientResponse = Mock(WebClient.ResponseSpec)
        mockWebClient.get() >> mockWebClientRequest
        mockWebClientRequest.uri(_) >> mockWebClientRequest
        mockWebClientRequest.retrieve() >> mockWebClientResponse
        mockWebClientResponse.toEntity(String.class) >> Mono.just(ResponseEntity.status(429).body("Too Many Requests"))
        mockWebClientBuilder.baseUrl("http://localhost:8081").build() >> mockWebClient

        def mockRocketMQTemplate = Mock(RocketMQTemplate)

        def pingService = Spy(new PingService(mockWebClient ,mockRocketMQTemplate))
        def pingScheduler = Spy(new PingScheduler(pingService))

        when:
        pingScheduler.schedulePing()
        sleep(1000)
        then:
        1 * pingService.processPingPongAndMQSend(_, RequestTypeEnum.TOO_MANY_REQUEST,null)
        1 * mockRocketMQTemplate.convertAndSend("ping_pong",_)
    }


    def "test ping unknown "() {
        given:
        sleep(1000)
        def mockWebClient = Mock(WebClient)
        def mockWebClientBuilder = Mock(WebClient.Builder)
        def mockWebClientRequest = Mock(WebClient.RequestBodyUriSpec)
        def mockWebClientResponse = Mock(WebClient.ResponseSpec)
        mockWebClient.get() >> mockWebClientRequest
        mockWebClientRequest.uri(_) >> mockWebClientRequest
        mockWebClientRequest.retrieve() >> mockWebClientResponse
        mockWebClientResponse.toEntity(String.class) >> Mono.just(ResponseEntity.status(500).body("test 500"))
        mockWebClientBuilder.baseUrl("http://localhost:8081").build() >> mockWebClient

        def mockRocketMQTemplate = Mock(RocketMQTemplate)

        def pingService = Spy(new PingService(mockWebClient ,mockRocketMQTemplate))
        def pingScheduler = Spy(new PingScheduler(pingService))

        when:
        pingScheduler.schedulePing()
        sleep(1000)
        then:
        1 * pingService.processPingPongAndMQSend(_, RequestTypeEnum.UNKNOWN_ERROR,_)
        1 * mockRocketMQTemplate.convertAndSend("ping_pong",_)
    }


    def "test ping error1"() {
        given:
        sleep(1000)
        def mockWebClient = Mock(WebClient)
        def mockWebClientBuilder = Mock(WebClient.Builder)
        def mockWebClientRequest = Mock(WebClient.RequestBodyUriSpec)
        def mockWebClientResponse = Mock(WebClient.ResponseSpec)
        mockWebClient.get() >> mockWebClientRequest
        mockWebClientRequest.uri(_) >> mockWebClientRequest
        mockWebClientRequest.retrieve() >> mockWebClientResponse
        mockWebClientResponse.toEntity(String.class) >> Mono.error(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS))
        mockWebClientBuilder.baseUrl("http://localhost:8081").build() >> mockWebClient

        def mockRocketMQTemplate = Mock(RocketMQTemplate)

        def pingService = Spy(new PingService(mockWebClient ,mockRocketMQTemplate))
        def pingScheduler = Spy(new PingScheduler(pingService))

        when:
        pingScheduler.schedulePing()
        sleep(1000)
        then:
        1 * pingService.processPingPongAndMQSend(_, RequestTypeEnum.TOO_MANY_REQUEST,null)
        1 * mockRocketMQTemplate.convertAndSend("ping_pong",_)
    }

    def "test ping error2"() {
        given:
        sleep(1000)
        def mockWebClient = Mock(WebClient)
        def mockWebClientBuilder = Mock(WebClient.Builder)
        def mockWebClientRequest = Mock(WebClient.RequestBodyUriSpec)
        def mockWebClientResponse = Mock(WebClient.ResponseSpec)
        mockWebClient.get() >> mockWebClientRequest
        mockWebClientRequest.uri(_) >> mockWebClientRequest
        mockWebClientRequest.retrieve() >> mockWebClientResponse
        mockWebClientResponse.toEntity(String.class) >> Mono.error(new WebClientResponseException("Too Many Requests",429,null,null,null,null))
        mockWebClientBuilder.baseUrl("http://localhost:8081").build() >> mockWebClient

        def mockRocketMQTemplate = Mock(RocketMQTemplate)

        def pingService = Spy(new PingService(mockWebClient ,mockRocketMQTemplate))
        def pingScheduler = Spy(new PingScheduler(pingService))

        when:
        pingScheduler.schedulePing()
        sleep(1000)
        then:
        1 * pingService.processPingPongAndMQSend(_, RequestTypeEnum.TOO_MANY_REQUEST,null)
        1 * mockRocketMQTemplate.convertAndSend("ping_pong",_)
    }

    def "test ping error3"() {
        given:
        sleep(1000)
        def mockWebClient = Mock(WebClient)
        def mockWebClientBuilder = Mock(WebClient.Builder)
        def mockWebClientRequest = Mock(WebClient.RequestBodyUriSpec)
        def mockWebClientResponse = Mock(WebClient.ResponseSpec)
        mockWebClient.get() >> mockWebClientRequest
        mockWebClientRequest.uri(_) >> mockWebClientRequest
        mockWebClientRequest.retrieve() >> mockWebClientResponse
        mockWebClientResponse.toEntity(String.class) >> Mono.error(new RuntimeException("unknown"))
        mockWebClientBuilder.baseUrl("http://localhost:8081").build() >> mockWebClient

        def mockRocketMQTemplate = Mock(RocketMQTemplate)

        def pingService = Spy(new PingService(mockWebClient ,mockRocketMQTemplate))
        def pingScheduler = Spy(new PingScheduler(pingService))

        when:
        pingScheduler.schedulePing()
        sleep(1000)
        then:
        1 * pingService.processPingPongAndMQSend(_, RequestTypeEnum.UNKNOWN_ERROR,_)
        1 * mockRocketMQTemplate.convertAndSend("ping_pong",_)
    }



    def "test file lock"() {
        given:
        sleep(1000)
        //访问限制文件设置两个一秒内的数值, 造成限制
        FileChannel channel = FileChannel.open(Paths.get(PingScheduler.LOCK_FILE_PATH), StandardOpenOption.WRITE)
        FileLock lock = channel.lock()

        def mockWebClient = Mock(WebClient)
        def mockRocketMQTemplate = Mock(RocketMQTemplate)
        def pingService = Spy(new PingService(mockWebClient ,mockRocketMQTemplate))
        def pingScheduler = Spy(new PingScheduler(pingService))

        when:
        try {
            pingScheduler.schedulePing()
            sleep(1000)
        }finally{
            if(lock!=null && lock.isValid()){
                lock.release()
            }
        }

        then:
        1 * pingService.processPingPongAndMQSend(_, RequestTypeEnum.FILE_LOCK_ERROR,null)
        1 * mockRocketMQTemplate.convertAndSend("ping_pong",_)
    }

    def "test file delete"() {
        given:
        sleep(1000)
        def mockWebClient = Mock(WebClient)
        def mockWebClientBuilder = Mock(WebClient.Builder)
        def mockWebClientRequest = Mock(WebClient.RequestBodyUriSpec)
        def mockWebClientResponse = Mock(WebClient.ResponseSpec)
        mockWebClient.get() >> mockWebClientRequest
        mockWebClientRequest.uri(_) >> mockWebClientRequest
        mockWebClientRequest.retrieve() >> mockWebClientResponse
        mockWebClientResponse.toEntity(String.class) >> Mono.just(ResponseEntity.ok("World"))
        mockWebClientBuilder.baseUrl("http://localhost:8081").build() >> mockWebClient

        def mockRocketMQTemplate = Mock(RocketMQTemplate)

        def pingService = Spy(new PingService(mockWebClient ,mockRocketMQTemplate))
        def pingScheduler = Spy(new PingScheduler(pingService))

        File lockFile = new File(PingScheduler.LOCK_FILE_PATH)
        lockFile.delete()
        when:
        pingScheduler.schedulePing()
        sleep(1000)
        then:
        1 * pingService.processPingPongAndMQSend(_, RequestTypeEnum.OK,"World")
        1 * mockRocketMQTemplate.convertAndSend("ping_pong",_)
    }

    @Autowired
    private PingPongDao pingPongDao

    def "test pgsql"(){
        given:
        sleep(1000)
        PingPong pingPong = new PingPong("test")
        PingPong uuid = pingPong.getUuid()

        when:
        def save = pingPongDao.save(pingPong).block()
        then:
        save == pingPong

        when:
        def find = pingPongDao.findById(pingPong.getId()).block()
        then:
        find == pingPong

        when:
        def delete = pingPongDao.deleteById(pingPong.getId()).block()
        then:
        delete == null

    }

}
