package com.example.WCS_DataStream.etl.scheduler;

import com.example.WCS_DataStream.etl.engine.AgvDataETLEngine;
import com.example.WCS_DataStream.etl.engine.ETLEngine;
import com.example.WCS_DataStream.etl.model.AgvData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import com.example.WCS_DataStream.etl.service.PostgreSQLDataService;

/**
 * AGV 데이터 ETL 스케줄러
 * 
 * 
 * 주요 기능:
 * - ETL 엔진을 통한 데이터 처리
 * - 하이브리드 풀링 방식으로 데이터 변화 감지
 * - 데이터 검증 및 변환
 * - 데이터 로드 시 Kafka 전송
 * 
 * @author AGV Monitoring System
 * @version 2.0
 */
@Component
@ConditionalOnProperty(prefix = "etl.agv", name = "enabled", havingValue = "true")
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
    public AgvDataScheduler(AgvDataETLEngine etlEngine, PostgreSQLDataService postgreSQLDataService) {
        super(postgreSQLDataService);  // 부모 생성자 호출
        this.etlEngine = etlEngine;
    }

    @Override
    @org.springframework.scheduling.annotation.Scheduled(
        fixedRateString = "${etl.agv.interval}",
        initialDelayString = "${etl.agv.initialDelay}"
    )
    public void executeETLProcess() {
        super.executeETLProcess();
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
        if (initialDataProcessed) return;
        try {
            etlEngine.resetCache();
            List<AgvData> initialData = etlEngine.executeFullLoad(); // 변경
            for (AgvData d : initialData) {
                processedIds.add(d.getUuidNo() + "_" + d.getReportTime());
            }
            initialDataProcessed = true;
        } catch (Exception e) {
            log.error("AGV 초기 데이터 처리 실패: {}", e.getMessage(), e);
        }
    }

    @Override
    protected void processIncrementalData() {
        try {
            List<AgvData> processedData = etlEngine.executeETL(); 
            int newDataCount = 0;
            for (AgvData d : processedData) {
                String id = d.getUuidNo() + "_" + d.getReportTime();
                if (!processedIds.contains(id)) {
                    processedIds.add(id);
                    newDataCount++;
                }
            }
            if (processedIds.size() > 10000) processedIds.clear();
            if (newDataCount > 0) {
                log.info("Processed {} new AGV data records", newDataCount);
            }
        } catch (Exception e) {
            log.error("AGV 증분 데이터 처리 오류: {}", e.getMessage(), e);
        }
    }

    
    /**
     * 스케줄러 캐시 초기화
     */
    @Override
    public void clearSchedulerCache() {
        processedIds.clear();
        lastProcessedTime.set(null);
        initialDataProcessed = false;
        etlEngine.resetCache();
        log.info("Cleared AGV data scheduler cache and engine cache (REDIS)");
    }
    
    /**
     * 처리된 데이터 ID 수 반환
     */
    public int getProcessedIdsCount() {
        return processedIds.size();
    }
} 