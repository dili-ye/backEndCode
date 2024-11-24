package com.example.ping

import com.example.ping.entity.PingPong
import com.example.ping.service.PingScheduler
import com.example.ping.service.PingService
import com.google.common.base.Verify
import javafx.beans.binding.When
import org.apache.rocketmq.spring.core.RocketMQTemplate
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.internal.verification.Times
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.support.AutowireCandidateResolver
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import spock.lang.Specification
import spock.lang.TempDir

@SpringBootTest
class PingSpec extends Specification{


    @TempDir
    File tempDir

    @Autowired
    PingService pingService

    @Autowired
    RocketMQTemplate rocketMQTemplate

    @Autowired
    PingScheduler pingScheduler

    def "测试正常响应逻辑"() {
        expect:
        pingScheduler.schedulePing()
        sleep(5000)
    }

    def "测试频率限制逻辑"() {
        given:
        File lockFile = new File(PingScheduler.LOCK_FILE_PATH)
        lockFile.write(System.currentTimeMillis().toString() + System.lineSeparator())
        lockFile.append((System.currentTimeMillis() - 500).toString() + System.lineSeparator())

        when:
        pingScheduler.schedulePing()

        sleep(5000)
        then:
        0 * pingService.pingPong()
        0 * _
    }

    def "测试429错误响应逻辑"() {
        expect:
        pingScheduler.schedulePing()
        pingScheduler.schedulePing()

        sleep(5000)

    }

}
