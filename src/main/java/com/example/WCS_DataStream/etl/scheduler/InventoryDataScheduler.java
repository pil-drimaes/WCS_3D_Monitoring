package com.example.WCS_DataStream.etl.scheduler;

import com.example.WCS_DataStream.etl.engine.InventoryDataETLEngine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 재고 정보 ETL 스케줄러
 * 
 * 재고 정보 데이터의 ETL 처리를 독립적으로 스케줄링하는 스케줄러
 * 
 * @author AGV Monitoring System
 * @version 2.0
 */
@Component
public class InventoryDataScheduler {
    
    private static final Logger log = LoggerFactory.getLogger(InventoryDataScheduler.class);
    
    private final InventoryDataETLEngine etlEngine;
    
    @Autowired
    public InventoryDataScheduler(InventoryDataETLEngine etlEngine) {
        this.etlEngine = etlEngine;
    }
    
    /**
     * 재고 정보 ETL 처리 (0.1초마다 실행)
     */
    @Scheduled(fixedRate = 100) // 0.1초마다 실행
    public void scheduleInventoryDataProcessing() {
        try {
            // 마지막 처리 시간 조회 (처음 실행시에는 1시간 전으로 설정)
            LocalDateTime lastProcessedTime = etlEngine.getLatestTimestamp();
            if (lastProcessedTime == null) {
                lastProcessedTime = LocalDateTime.now().minusHours(1);
            }
            
            // ETL 프로세스 실행
            int processedCount = etlEngine.processETL(lastProcessedTime);
            
            if (processedCount > 0) {
                log.info("재고 정보 ETL 처리 완료: {}개 레코드 처리", processedCount);
            }
            
        } catch (Exception e) {
            log.error("재고 정보 ETL 처리 중 오류 발생: {}", e.getMessage(), e);
        }
    }
} 