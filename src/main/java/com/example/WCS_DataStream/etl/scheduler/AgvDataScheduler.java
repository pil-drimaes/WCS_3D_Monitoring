package com.example.WCS_DataStream.etl.scheduler;

import com.example.WCS_DataStream.etl.config.ETLConfig;
import com.example.WCS_DataStream.etl.engine.AgvDataETLEngine;
import com.example.WCS_DataStream.etl.engine.ETLEngine;
import com.example.WCS_DataStream.etl.model.AgvData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
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
public class AgvDataScheduler extends BaseETLScheduler<AgvData> {
    
    private static final Logger log = LoggerFactory.getLogger(AgvDataScheduler.class);
    
    /**
     * ETL 엔진
     */
    private final AgvDataETLEngine etlEngine;
    
    /**
     * 처리된 데이터 ID 추적 (중복 방지)
     */
    private final Set<String> processedIds = Collections.synchronizedSet(new HashSet<>());
    
    /**
     * 초기 데이터 처리 완료 여부
     */
    private boolean initialDataProcessed = false;
    
    /**
     * 생성자
     * 
     * @param etlEngine ETL 엔진
     */
    @Autowired
    public AgvDataScheduler(AgvDataETLEngine etlEngine) {
        this.etlEngine = etlEngine;
    }
    
    @Override
    protected ETLEngine<AgvData> getETLEngine() {
        return etlEngine;
    }
    
    @Override
    protected String getSchedulerName() {
        return "AGV Data";
    }
    
    /**
     * 초기 데이터 처리 (애플리케이션 시작 시 한 번만)
     */
    @Override
    protected void processInitialData() {
        if (initialDataProcessed) {
            return;
        }
        
        try {
            log.info("AGV 초기 데이터 처리 시작");
            
            // ETL 엔진을 통해 초기 데이터 처리
            List<AgvData> initialData = etlEngine.executeETL();
            
            // 처리된 데이터 ID를 캐시에 저장
            for (AgvData agvData : initialData) {
                String dataId = agvData.getUuidNo() + "_" + agvData.getReportTime();
                processedIds.add(dataId);
            }
            
            initialDataProcessed = true;
            log.info("AGV 초기 데이터 처리 완료: {}개 레코드", initialData.size());
            
        } catch (Exception e) {
            log.error("AGV 초기 데이터 처리 실패: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 증분 데이터 처리 (변경된 데이터만)
     */
    @Override
    protected void processIncrementalData() {
        try {
            // ETL 프로세스 실행
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
            
        } catch (Exception e) {
            log.error("Error in AGV incremental data processing: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 캐시 초기화
     */
    @Override
    public void clearCache() {
        processedIds.clear();
        lastProcessedTime.set(null);
        initialDataProcessed = false;
        log.info("Cleared AGV data scheduler cache");
    }
    
    /**
     * 애플리케이션 시작 시 초기화
     */
    public void initializeOnStartup() {
        log.info("AGV 스케줄러 애플리케이션 시작 시 초기화");
        clearCache();
        // 다음 executeETLProcess() 호출 시 전체 데이터를 다시 처리
    }
    
    /**
     * 처리된 데이터 ID 수 반환
     */
    public int getProcessedIdsCount() {
        return processedIds.size();
    }
} 