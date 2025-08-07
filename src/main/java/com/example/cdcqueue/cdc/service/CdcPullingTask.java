package com.example.cdcqueue.cdc.service;

import com.example.cdcqueue.common.model.AgvData;
import com.example.cdcqueue.common.model.CdcEvent;
import com.example.cdcqueue.common.queue.EventQueue;
import com.example.cdcqueue.etl.service.AgvDataService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * CDC(Change Data Capture) Pulling Task
 * 
 * 데이터베이스에서 AGV 데이터 변화를 감지하고 이벤트를 생성하는 스케줄링 태스크
 * 100ms마다 실행되어 새로운 AGV 데이터를 감지하고 이벤트 큐에 추가합니다.
 * 
 * 주요 기능:
 * - 100ms마다 데이터베이스 폴링
 * - 마지막 체크 이후의 새로운 데이터만 조회
 * - 새로운 데이터를 CDC 이벤트로 변환
 * - 이벤트 큐에 이벤트 추가
 * 
 * 동작 방식:
 * 1. 마지막 체크 시간 확인
 * 2. 해당 시간 이후의 새로운 데이터 조회
 * 3. 각 데이터를 CDC 이벤트로 변환
 * 4. 이벤트 큐에 추가
 * 5. 마지막 체크 시간 업데이트
 * 
 * 스레드 안전성:
 * - AtomicReference를 사용하여 마지막 체크 시간 관리
 * - ConcurrentLinkedQueue를 통한 이벤트 큐 동기화
 * 
 * @author AGV Monitoring System
 * @version 1.0
 */
@Component
public class CdcPullingTask {
    
    /**
     * 로거
     */
    private static final Logger log = LoggerFactory.getLogger(CdcPullingTask.class);
    
    /**
     * 이벤트 큐 (생성된 이벤트를 저장)
     */
    private final EventQueue queue;
    
    /**
     * AGV 데이터 서비스 (AGV 데이터 조회)
     */
    private final AgvDataService wcsService;
    
    /**
     * JSON 직렬화/역직렬화를 위한 ObjectMapper
     */
    private final ObjectMapper objectMapper;
    
    /**
     * 마지막 체크 시간 (스레드 안전하게 관리)
     * AtomicReference를 사용하여 다중 스레드 환경에서 안전하게 동작
     */
    private final AtomicReference<LocalDateTime> lastTimestamp = new AtomicReference<>(null);

    /**
     * 생성자
     * 
     * @param queue 이벤트 큐
     * @param wcsService AGV 데이터 서비스
     * @param objectMapper JSON 직렬화용 ObjectMapper
     */
    @Autowired
    public CdcPullingTask(EventQueue queue, AgvDataService wcsService, ObjectMapper objectMapper) {
        this.queue = queue;
        this.wcsService = wcsService;
        this.objectMapper = objectMapper;
    }

    /**
     * AGV 데이터 폴링 작업 (100ms마다 실행)
     * 
     * CDC 기능이 비활성화됨
     * ETL 모듈을 사용하세요.
     */
    // @Scheduled(fixedRate = 100) // 100ms마다 실행 - CDC 비활성화
    public void pollAgvData() {
        try {
            // 마지막 체크 시간 이후의 새로운 데이터 조회
            LocalDateTime lastCheck = lastTimestamp.get();
            List<AgvData> newData = wcsService.getAgvDataAfterTimestamp(lastCheck);
            
            // 새로운 데이터를 CDC 이벤트로 변환하여 큐에 추가
            for (AgvData agvData : newData) {
                String id = UUID.randomUUID().toString();
                String type = "AGV_UPDATE_CDC";
                String payload = objectMapper.writeValueAsString(agvData);
                
                CdcEvent event = new CdcEvent(id, type, payload);
                queue.add(event);
                
                log.debug("Added CDC AGV event to queue: Robot_No={}, Pos_X={}, Pos_Y={}", 
                         agvData.getRobotNo(), agvData.getPosX(), agvData.getPosY());
            }
            
            // 마지막 체크 시간 업데이트
            lastTimestamp.set(LocalDateTime.now());
            
            if (!newData.isEmpty()) {
                log.info("CDC: Processed {} new AGV data records", newData.size());
            }
            
        } catch (Exception e) {
            log.error("Error in CDC polling: {}", e.getMessage(), e);
        }
    }
}