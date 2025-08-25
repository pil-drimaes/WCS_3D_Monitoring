package com.example.WCS_DataStream.etl.scheduler;

import com.example.WCS_DataStream.etl.engine.ETLEngine;
import com.example.WCS_DataStream.etl.engine.PodDataETLEngine;
import com.example.WCS_DataStream.etl.model.PodInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import com.example.WCS_DataStream.etl.service.PostgreSQLDataService;

/**
 * POD 정보 ETL 스케줄러
 * 
 * POD 정보 데이터의 ETL 처리를 독립적으로 스케줄링하는 스케줄러
 * 
 * @author AGV Monitoring System
 * @version 2.0
 */
@Component
public class PodDataScheduler extends BaseETLScheduler<PodInfo> {
    
    private static final Logger log = LoggerFactory.getLogger(PodDataScheduler.class);
    
    private final PodDataETLEngine etlEngine;
    
    /**
     * 처리된 데이터 ID 추적 (중복 방지)
     */
    private final Set<String> processedIds = Collections.synchronizedSet(new HashSet<>());
    
    /**
     * 초기 데이터 처리 완료 여부
     */
    private boolean initialDataProcessed = false;
    
    @Autowired
    public PodDataScheduler(PodDataETLEngine etlEngine, PostgreSQLDataService postgreSQLDataService) {
        super(postgreSQLDataService);  // 부모 생성자 호출
        this.etlEngine = etlEngine;
    }
    
    @Override
    protected ETLEngine<PodInfo> getETLEngine() {
        return etlEngine;
    }
    
    @Override
    protected String getSchedulerName() {
        return "POD Data";
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
            log.info("POD 정보 초기 데이터 처리 시작");
            
            // ETL 엔진을 통해 초기 데이터 처리 (AgvDataScheduler와 동일한 방식)
            List<PodInfo> initialData = etlEngine.executeETL();
            
            // 처리된 데이터 ID를 캐시에 저장
            for (PodInfo podInfo : initialData) {
                String dataId = podInfo.getUuidNo() + "_" + podInfo.getReportTime();
                processedIds.add(dataId);
            }
            
            initialDataProcessed = true;
            log.info("POD 정보 초기 데이터 처리 완료: {}개 레코드", initialData.size());
            
        } catch (Exception e) {
            log.error("POD 정보 초기 데이터 처리 중 오류 발생: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 증분 데이터 처리 (변경된 데이터만)
     */
    @Override
    protected void processIncrementalData() {
        try {
            // ETL 프로세스 실행
            List<PodInfo> processedData = etlEngine.executeETL();
            
            // 중복 처리 방지: 새로운 데이터만 처리
            int newDataCount = 0;
            for (PodInfo podInfo : processedData) {
                String dataId = podInfo.getUuidNo() + "_" + podInfo.getReportTime();
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
                log.info("Processed {} new POD data records through ETL engine (total: {})", newDataCount, processedData.size());
            }
            
        } catch (Exception e) {
            log.error("POD 정보 증분 데이터 처리 중 오류 발생: {}", e.getMessage(), e);
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
        log.info("POD 정보 스케줄러 캐시 초기화 완료");
    }
    
    /**
     * 애플리케이션 시작 시 초기화
     */
    public void initializeOnStartup() {
        log.info("POD 정보 스케줄러 애플리케이션 시작 시 초기화");
        clearCache();
        // 다음 executeETLProcess() 호출 시 전체 데이터를 다시 처리
    }
} 