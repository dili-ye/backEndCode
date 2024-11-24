package com.example.ping.service;

import com.example.ping.entity.PingPong;
import io.netty.util.internal.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.example.ping.constant.RequestTypeEnum.*;


@Service
@Slf4j
public class PingScheduler {

    private final PingService pingService;

    private final RocketMQTemplate rocketMQTemplate;

    private static final int RATE_LIMIT = 2;
    private static final long RATE_MS = 1000L;
    public static final String LOCK_FILE_PATH ="ping-service.lock";

    @Value("${server.port}")
    private Integer port;

    public PingScheduler(PingService pingService, RocketMQTemplate rocketMQTemplate) {
        this.pingService = pingService;
        this.rocketMQTemplate = rocketMQTemplate;
    }

    @Scheduled(fixedRate = 300)
    public void schedulePing() throws IOException {
        long currentTime = System.currentTimeMillis();
        File file = new File(LOCK_FILE_PATH);
        if(!file.exists()){
            file.createNewFile();
        }
        PingPong pingPong = new PingPong();
        pingPong.setServiceCode("ping_"+port);
        pingPong.setUuid(UUID.randomUUID().toString());
        pingPong.setRequestTime(LocalDateTime.now());
        RandomAccessFile raf = null;
        FileLock lock = null ;
        FileChannel channel = null;
        try {
            raf = new RandomAccessFile(file, "rw");
            channel = raf.getChannel();
            lock = channel.tryLock();
            if(lock != null){
                List<Long> timestamps = new ArrayList<>();
                String line ;
                while (!StringUtil.isNullOrEmpty(line = raf.readLine())) {
                    long timestamp = Long.parseLong(line.trim());
                    timestamps.add(timestamp);
                }
                boolean sendFlag = true;
                if(timestamps.size()>=RATE_LIMIT){
                    Long rateTime = timestamps.get(timestamps.size() - RATE_LIMIT);
                    if(currentTime - rateTime < RATE_MS){
                        sendFlag = false;
                    }
                }
                if(sendFlag){
                    pingService.pingPong()
                            .subscribe(response -> {
                                pingPong.setResponseTime(LocalDateTime.now());
                                if (response.getStatusCode().is2xxSuccessful()) {
                                    pingPong.setRequestType(OK.getCode());
                                    pingPong.setDescription(OK.getDesc()+response.getBody());
                                } else if (response.getStatusCode().value() == 429) {
                                    pingPong.setRequestType(TOO_MANY_REQUEST.getCode());
                                    pingPong.setDescription(TOO_MANY_REQUEST.getDesc());
                                } else {
                                    pingPong.setRequestType(UNKNOWN_ERROR.getCode());
                                    pingPong.setDescription(UNKNOWN_ERROR.getDesc()+ response.getBody());
                                }
                                rocketMQTemplate.convertAndSend("ping_pong",pingPong);
                            }, error -> {
                                handleError(error,pingPong);
                                rocketMQTemplate.convertAndSend("ping_pong",pingPong);
                            });
                    timestamps.add(currentTime);
                    raf.setLength(0);
                    for (int i = 0;i< timestamps.size();i++) {
                        Long timestamp = timestamps.get(i);
                        if (currentTime - timestamp < RATE_MS) {
                            raf.writeBytes(timestamp + System.lineSeparator());
                        }
                    }
                }else {
                    pingPong.setRequestType(REQUEST_LIMIT.getCode());
                    pingPong.setDescription(REQUEST_LIMIT.getDesc());
                    rocketMQTemplate.convertAndSend("ping_pong",pingPong);
                }
            }else {
                pingPong.setRequestType(REQUEST_LIMIT.getCode());
                pingPong.setDescription(REQUEST_LIMIT.getDesc());
                rocketMQTemplate.convertAndSend("ping_pong",pingPong);
            }
        }catch (Exception e) {
            e.printStackTrace();
        }finally {
            try {
                if (lock != null && lock.isValid()) {
                    lock.release(); // 释放锁
                }
                if (channel != null) {
                    channel.close(); // 关闭通道
                }
                if (raf != null) {
                    raf.close(); // 关闭 RandomAccessFile
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void handleError(Throwable error,PingPong pingPong) {
        pingPong.setResponseTime(LocalDateTime.now());
        pingPong.setRequestType(UNKNOWN_ERROR.getCode());
        pingPong.setDescription(UNKNOWN_ERROR.getDesc()+ error.getMessage());
        if (error instanceof WebClientResponseException) {
            WebClientResponseException webClientResponseException = (WebClientResponseException) error;
            if (webClientResponseException.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                pingPong.setRequestType(TOO_MANY_REQUEST.getCode());
                pingPong.setDescription(TOO_MANY_REQUEST.getDesc());
            }
        }
    }
}
