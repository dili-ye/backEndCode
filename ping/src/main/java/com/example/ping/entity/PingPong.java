package com.example.ping.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Table("ping_pong")
public class PingPong {

    @Id
    private Long id;

    private String serviceCode;

    private String uuid;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss.SSS")
    private LocalDateTime requestTime;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss.SSS")
    private LocalDateTime responseTime;

    private Integer requestType;

    private String description;

    public PingPong(String serviceCode){
        PingPong pingPong = new PingPong();
        pingPong.setServiceCode(serviceCode);
        pingPong.setUuid(UUID.randomUUID().toString());
        pingPong.setRequestTime(LocalDateTime.now());
    }
}
