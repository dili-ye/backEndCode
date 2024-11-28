package com.example.ping.mq;

import com.example.ping.dao.PingPongDao;
import com.example.ping.entity.PingPong;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RocketMQMessageListener(topic = "ping_pong", consumerGroup = "${rocketmq.consumer.group}")
public class RocketMQConsumer implements RocketMQListener<PingPong> {

    @Autowired
    private PingPongDao pingPongDao;

//    private static Logger infoLogger = LogManager.getLogger("InfoLogger");
    @Override
    public void onMessage(PingPong pingPong) {
        if(StringUtils.isNotEmpty(pingPong.getUuid())){
            PingPong ex = new PingPong();
            ex.setUuid(pingPong.getUuid());
            pingPongDao.exists(Example.of(ex)).doOnNext(exist->{
                if(!exist){
//                    infoLogger.info("服务编号:{}, 唯一标识码:{},创建时间:{}, 详情:{}",pingPong.getServiceCode(),pingPong.getUuid(),pingPong.getRequestTime(),pingPong.getDescription());
                    log.info("服务编号:{}, 唯一标识码:{},创建时间:{}, 详情:{}",pingPong.getServiceCode(),pingPong.getUuid(),pingPong.getRequestTime(),pingPong.getDescription());
                    pingPongDao.save(pingPong).subscribe();
                }
            }).subscribe();
        }
    }
}
