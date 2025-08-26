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
        if (initialDataProcessed) return;
        try {
            List<PodInfo> initialData = etlEngine.executeFullLoad(); // 변경
            for (PodInfo d : initialData) {
                processedIds.add(d.getUuidNo() + "_" + d.getReportTime());
            }
            initialDataProcessed = true;
        } catch (Exception e) {
            log.error("POD 초기 데이터 처리 오류: {}", e.getMessage(), e);
        }
    }

    @Override
    protected void processIncrementalData() {
        try {
            List<PodInfo> processedData = etlEngine.executeETL(); // 변경: 증분
            int newDataCount = 0;
            for (PodInfo d : processedData) {
                String id = d.getUuidNo() + "_" + d.getReportTime();
                if (!processedIds.contains(id)) {
                    processedIds.add(id);
                    newDataCount++;
                }
            }
            if (processedIds.size() > 10000) processedIds.clear();
            if (newDataCount > 0) {
                log.info("Processed {} new POD data records", newDataCount);
            }
        } catch (Exception e) {
            log.error("POD 증분 데이터 처리 오류: {}", e.getMessage(), e);
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