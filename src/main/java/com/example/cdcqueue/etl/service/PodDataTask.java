package com.example.cdcqueue.etl.service;

import com.example.cdcqueue.common.model.PodInfo;
import com.example.cdcqueue.etl.PodDataETLEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * POD 정보 ETL 스케줄링 태스크
 * 
 * POD 정보 데이터의 ETL 처리를 독립적으로 스케줄링하는 태스크
 * 
 * @author AGV Monitoring System
 * @version 2.0
 */
@Component
public class PodDataTask {
    
    private static final Logger log = LoggerFactory.getLogger(PodDataTask.class);
    
    private final PodDataETLEngine etlEngine;
    
    @Autowired
    public PodDataTask(PodDataETLEngine etlEngine) {
        this.etlEngine = etlEngine;
    }
    
    /**
     * POD 정보 ETL 처리 (3초마다 실행)
     */
    @Scheduled(fixedRate = 3000) // 3초마다 실행
    public void processPodData() {
        try {
            // 마지막 처리 시간 조회 (처음 실행시에는 1시간 전으로 설정)
            LocalDateTime lastProcessedTime = etlEngine.getLatestTimestamp();
            if (lastProcessedTime == null) {
                lastProcessedTime = LocalDateTime.now().minusHours(1);
            }
            
            // ETL 프로세스 실행
            int processedCount = etlEngine.processETL(lastProcessedTime);
            
            if (processedCount > 0) {
                log.info("POD 정보 ETL 처리 완료: {}개 레코드 처리", processedCount);
            }
            
        } catch (Exception e) {
            log.error("POD 정보 ETL 처리 중 오류 발생: {}", e.getMessage(), e);
        }
    }
} 