package com.example.ping.dao;

import com.example.ping.entity.PingPong;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PingPongDao extends R2dbcRepository<PingPong,Long> {
}
