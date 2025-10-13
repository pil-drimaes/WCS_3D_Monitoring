package com.example.WCS_DataStream.etl.service;

import org.springframework.stereotype.Service;

import java.sql.Timestamp;

@Service
public class EtlOffsetStore {

    private final RedisCacheService redis;

    public EtlOffsetStore(RedisCacheService redis) {
        this.redis = redis;
    }

    public static final String NS = "etlOffset";

    public static class Offset {
        public Timestamp lastTs;
        public String lastUuid;
        public Offset() {}
        public Offset(Timestamp lastTs, String lastUuid) { this.lastTs = lastTs; this.lastUuid = lastUuid; }
    }

    public Offset get(String jobName) {
        return redis.get(NS, jobName, Offset.class);
    }

    public void set(String jobName, Offset offset) {
        redis.set(NS, jobName, offset);
    }
} 