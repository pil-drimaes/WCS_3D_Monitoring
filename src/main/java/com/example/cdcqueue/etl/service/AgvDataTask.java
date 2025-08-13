package com.example.cdcqueue.etl.service;

import com.example.cdcqueue.common.config.ETLProperties;
import com.example.cdcqueue.etl.AgvDataETLEngine;
import com.example.cdcqueue.etl.ETLConfig;
import com.example.cdcqueue.etl.engine.PullingEngineConfig;
import com.example.cdcqueue.common.model.AgvData;
import com.example.cdcqueue.common.model.CdcEvent;

import com.example.cdcqueue.common.queue.EventQueue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;

/**
 * 새로운 아키텍처를 사용하는 AGV 데이터 처리 태스크
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
public class AgvDataTask {
    
    private static final Logger log = LoggerFactory.getLogger(AgvDataTask.class);
    
    /**
     * ETL 엔진
     */
    private final AgvDataETLEngine etlEngine;
    
    /**
     * 이벤트 큐
     */
    private final EventQueue eventQueue;
    
    /**
     * JSON 직렬화용 ObjectMapper
     */
    private final ObjectMapper objectMapper;
    
    /**
     * ETL 설정 Properties
     */
    private final ETLProperties etlProperties;
    
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
     * @param eventQueue 이벤트 큐
     * @param objectMapper JSON 직렬화용 ObjectMapper
     * @param etlProperties ETL 설정 Properties
     */
    @Autowired
    public AgvDataTask(AgvDataETLEngine etlEngine, EventQueue eventQueue, ObjectMapper objectMapper, ETLProperties etlProperties) {
        this.etlEngine = etlEngine;
        this.eventQueue = eventQueue;
        this.objectMapper = objectMapper;
        this.etlProperties = etlProperties;
    }
    
    /**
     * AGV 데이터 처리 (설정된 간격마다 실행)
     * 
     * ETL 엔진을 사용하여 새로운 AGV 데이터를 처리하고 이벤트 큐에 추가합니다.
     */
    @Scheduled(fixedRate = 100) // 기본 0.1ms마다 실행 
    public void processAgvData() {
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
                    addToEventQueue(agvData);
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
            // 풀링 엔진 설정 (Properties에서 읽어옴)
            PullingEngineConfig pullingConfig = new PullingEngineConfig();
            pullingConfig.setPullInterval(etlProperties.getPulling().getInterval());
            pullingConfig.setBatchSize(etlProperties.getPulling().getBatchSize());
            pullingConfig.setStrategy(PullingEngineConfig.PullingStrategy.valueOf(etlProperties.getPulling().getStrategy()));
            // WCS DB 설정은 별도 데이터소스로 관리되므로 DatabaseConfig는 설정하지 않음
            

            
            // ETL 설정 (Properties에서 읽어옴)
            etlConfig = new ETLConfig();
            etlConfig.setPullingConfig(pullingConfig);

            etlConfig.setExecutionInterval(etlProperties.getPulling().getInterval());
            etlConfig.setValidationEnabled(etlProperties.getValidation().isEnabled());
            etlConfig.setTransformationEnabled(etlProperties.getTransformation().isEnabled());
            etlConfig.setErrorHandlingMode(ETLConfig.ErrorHandlingMode.valueOf(etlProperties.getErrorHandling().getMode()));
            etlConfig.setRetryCount(etlProperties.getErrorHandling().getRetryCount());
            etlConfig.setRetryInterval(etlProperties.getErrorHandling().getRetryInterval());
            
            // ETL 엔진 초기화
            etlEngine.initialize(etlConfig);
            
            initialized = true;
            log.info("ETL engine initialized successfully with properties: {}", etlProperties);
            
        } catch (Exception e) {
            log.error("Error initializing ETL engine: {}", e.getMessage(), e);
        }
    }
    
    /**
     * AGV 데이터를 이벤트 큐에 추가
     */
    private void addToEventQueue(AgvData agvData) {
        try {
            // 고유 ID 생성
            String id = UUID.randomUUID().toString();
            String type = "AGV_UPDATE_ETL";
            
            // AGV 데이터를 JSON으로 직렬화
            String payload = objectMapper.writeValueAsString(agvData);
            
            // CDC 이벤트 생성
            CdcEvent event = new CdcEvent(id, type, payload);
            
            // 이벤트 큐에 추가
            eventQueue.add(event);
            
            log.debug("Added ETL AGV event to queue: Robot_No={}, Pos_X={}, Pos_Y={}", 
                     agvData.getRobotNo(), agvData.getPosX(), agvData.getPosY());
            
        } catch (JsonProcessingException e) {
            log.error("Error serializing AGV data to JSON: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error adding AGV data to event queue: {}", e.getMessage(), e);
        }
    }
    
    /**
     * ETL 엔진 상태 확인
     */
    public boolean isETLHealthy() {
        return initialized && etlEngine.isHealthy();
    }
    
    /**
     * ETL 통계 조회
     */
    public String getETLStatistics() {
        if (!initialized) {
            return "ETL engine not initialized";
        }
        return etlEngine.getStatistics().toString();
    }
} 