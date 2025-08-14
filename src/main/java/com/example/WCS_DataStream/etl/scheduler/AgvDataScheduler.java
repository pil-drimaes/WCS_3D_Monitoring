package com.example.WCS_DataStream.etl.scheduler;

import com.example.WCS_DataStream.etl.config.ETLConfig;
import com.example.WCS_DataStream.etl.engine.AgvDataETLEngine;
import com.example.WCS_DataStream.etl.model.AgvData;
// 사용하지 않는 import 제거
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;

/**
 * AGV 데이터 ETL 스케줄러
 * 
 * ETL 엔진을 사용하여 WCS DB에서 AGV 데이터를 가져와서 이벤트 큐에 추가합니다.
 * 기존 CDC 방식과 별도로 동작하여 다양한 데이터 소스를 지원할 수 있습니다.
 * 
 * 주요 기능:
 * - ETL 엔진을 통한 데이터 처리
 * - 하이브리드 풀링 방식으로 데이터 변화 감지
 * - 데이터 검증 및 변환
 * - 이벤트 큐에 CDC 이벤트 추가
 * 
 * @author AGV Monitoring System
 * @version 2.0
 */
@Component
public class AgvDataScheduler {
    
    private static final Logger log = LoggerFactory.getLogger(AgvDataScheduler.class);
    
    /**
     * ETL 엔진
     */
    private final AgvDataETLEngine etlEngine;
    
    /**
     * ETL 설정
     */
    private ETLConfig etlConfig;
    
    /**
     * 초기화 완료 여부
     */
    private boolean initialized = false;
    
    /**
     * 마지막 처리 시간 (중복 처리 방지용)
     */
    private final AtomicReference<LocalDateTime> lastProcessedTime = new AtomicReference<>(null);
    
    /**
     * 처리된 데이터 ID 추적 (중복 방지)
     */
    private final Set<String> processedIds = Collections.synchronizedSet(new HashSet<>());
    
    /**
     * 생성자
     * 
     * @param etlEngine ETL 엔진
     * @param objectMapper JSON 직렬화용 ObjectMapper
     */
    @Autowired
    public AgvDataScheduler(AgvDataETLEngine etlEngine, ObjectMapper objectMapper) {
        this.etlEngine = etlEngine;
    }
    
    /**
     * AGV 데이터 처리 (0.1초마다 실행)
     * 
     * ETL 엔진을 사용하여 새로운 AGV 데이터를 처리하고 이벤트 큐에 추가합니다.
     */
    @Scheduled(fixedRate = 100) // 0.1초마다 실행
    public void scheduleAgvDataProcessing() {
        try {
            
            if (!initialized) {
                initializeETL();
            }
            
            // NOTE: ETL 프로세스 실행
            List<AgvData> processedData = etlEngine.executeETL();
            
            // 중복 처리 방지: 새로운 데이터만 처리
            int newDataCount = 0;
            for (AgvData agvData : processedData) {
                String dataId = agvData.getUuidNo() + "_" + agvData.getReportTime();
                if (!processedIds.contains(dataId)) {
                    processedIds.add(dataId);
                    newDataCount++;
                }
            }
            
            // 처리된 데이터 수 제한 (메모리 관리)
            if (processedIds.size() > 10000) {
                processedIds.clear();
                log.info("Cleared processed IDs cache for memory management");
            }
            
            if (newDataCount > 0) {
                log.info("Processed {} new AGV data records through ETL engine (total: {})", newDataCount, processedData.size());
            }
            
            // 마지막 처리 시간 업데이트
            lastProcessedTime.set(LocalDateTime.now());
            
        } catch (Exception e) {
            log.error("Error in AGV data processing: {}", e.getMessage(), e);
        }
    }
    
    /**
     * ETL 엔진 초기화
     */
    private void initializeETL() {
        try {
            // ETL 설정
            etlConfig = new ETLConfig();
            etlConfig.setPullInterval(Duration.ofMillis(100)); // 0.1초
            etlConfig.setBatchSize(100);
            etlConfig.setStrategy(ETLConfig.PullingStrategy.HYBRID);
            etlConfig.setExecutionInterval(Duration.ofMillis(100));
            etlConfig.setValidationEnabled(true);
            etlConfig.setTransformationEnabled(true);
            etlConfig.setErrorHandlingMode(ETLConfig.ErrorHandlingMode.CONTINUE);
            etlConfig.setRetryCount(3);
            etlConfig.setRetryInterval(Duration.ofSeconds(5));
            
            // ETL 엔진 초기화
            etlEngine.initialize(etlConfig);
            
            initialized = true;
            log.info("ETL engine initialized successfully with default configuration");
            
        } catch (Exception e) {
            log.error("Error initializing ETL engine: {}", e.getMessage(), e);
        }
    }
    
    // EventQueue 관련 메서드 제거
    
    /**
     * ETL 엔진 상태 확인
     */
    public boolean isETLInitialized() {
        return initialized;
    }
    
    /**
     * 마지막 처리 시간 반환
     */
    public LocalDateTime getLastProcessedTime() {
        return lastProcessedTime.get();
    }
    
    /**
     * 처리된 데이터 ID 수 반환
     */
    public int getProcessedIdsCount() {
        return processedIds.size();
    }
    
    /**
     * 캐시 초기화
     */
    public void clearCache() {
        processedIds.clear();
        lastProcessedTime.set(null);
        log.info("Cleared AGV data scheduler cache");
    }
} 