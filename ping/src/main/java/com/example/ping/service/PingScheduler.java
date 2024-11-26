package com.example.ping.service;

import com.example.ping.entity.PingPong;
import io.netty.util.internal.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.ArrayList;
import java.util.List;

import static com.example.ping.constant.RequestTypeEnum.*;


@Service
@Slf4j
public class PingScheduler {

    private final PingService pingService;

    private static final int RATE_LIMIT = 2;
    private static final long RATE_MS = 1000L;
    public static final String LOCK_FILE_PATH ="ping-service.lock";

    @Value("${server.port}")
    private Integer port;

    public PingScheduler(PingService pingService) {
        this.pingService = pingService;
    }

    @Scheduled(fixedRate = 300)
    public void schedulePing() throws IOException {
        File file = initFile();
        processFileLock(file);
    }

    private void processFileLock(File file) {
        PingPong pingPong = new PingPong("ping_"+port);
        RandomAccessFile raf = null;
        FileLock lock = null ;
        FileChannel channel = null;

        try {
            raf = new RandomAccessFile(file, "rw");
            channel = raf.getChannel();
            lock = channel.tryLock();
            if(lock == null){
                pingService.processPingPongAndMQSend(pingPong,FILE_LOCK_ERROR,null);
                return;
            }

            long currentTime = System.currentTimeMillis();
            FileLockResult result = getFileLockResult(raf, currentTime);

            if(result.sendFlag){
                pingService.requestProcess(pingPong);
                updateLockTime(result, currentTime, raf);
            }else {
                pingService.processPingPongAndMQSend(pingPong,REQUEST_LIMIT,null);
            }
        }catch (OverlappingFileLockException e) {
            //同JVM内不同线程获取会报错
            pingService.processPingPongAndMQSend(pingPong,FILE_LOCK_ERROR,null);
        }catch (Exception e){
            e.printStackTrace();
        } finally {
            try {
                if (lock != null && lock.isValid()) {
                    lock.release();
                }
                if (channel != null) {
                    channel.close();
                }
                if (raf != null) {
                    raf.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void updateLockTime(FileLockResult result, long currentTime, RandomAccessFile raf) throws IOException {
        result.timestamps.add(currentTime);
        raf.setLength(0);
        for (int i = 0; i< result.timestamps.size(); i++) {
            Long timestamp = result.timestamps.get(i);
            if (currentTime - timestamp < RATE_MS) {
                raf.writeBytes(timestamp + System.lineSeparator());
            }
        }
    }

    private FileLockResult getFileLockResult(RandomAccessFile raf, long currentTime) throws IOException {
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
        FileLockResult result = new FileLockResult(timestamps, sendFlag);
        return result;
    }

    static class FileLockResult {
        /**
         * 滑动时间窗口毫秒值
         */
        public List<Long> timestamps;
        /**
         * 是否发送
         */
        public boolean sendFlag;

        public FileLockResult(List<Long> timestamps, boolean sendFlag) {
            this.timestamps = timestamps;
            this.sendFlag = sendFlag;
        }
    }

    private File initFile() throws IOException {
        File file = new File(LOCK_FILE_PATH);
        if(!file.exists()){
            file.createNewFile();
        }
        return file;
    }

}
