package com.example.cdcqueue.etl.engine;

import com.example.cdcqueue.common.model.PodInfo;
import com.example.cdcqueue.etl.service.PodDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Pod 데이터 전용 하이브리드 풀링 엔진
 * 
 * HybridPullingEngine의 구체적인 구현체로 Pod 데이터에 특화된 로직을 제공합니다.
 * 
 * @author AGV Monitoring System
 * @version 2.0
 */
@Component
public class PodHybridPullingEngine extends HybridPullingEngine<PodInfo> {
    
    private static final Logger log = LoggerFactory.getLogger(PodHybridPullingEngine.class);
    
    private final PodDataService podDataService;
    
    @Autowired
    public PodHybridPullingEngine(PodDataService podDataService) {
        this.podDataService = podDataService;
    }
    
    @Override
    protected List<PodInfo> getChangedData() {
        LocalDateTime lastCheck = getLastCheckTime();
        return podDataService.getPodDataAfterTimestamp(lastCheck);
    }
    
    @Override
    protected List<PodInfo> getAllData() {
        return podDataService.getAllPodData();
    }
    
    @Override
    protected String getDataKey(PodInfo data) {
        return data.getUuidNo();
    }
    
    @Override
    protected boolean isSameData(PodInfo data1, PodInfo data2) {
        // reportTime이 다르면 다른 데이터로 인식 (시간 기반 변경 감지)
        if (!Objects.equals(data1.getReportTime(), data2.getReportTime())) {
            return false;
        }
        
        // 다른 중요 필드들도 비교
        return Objects.equals(data1.getUuidNo(), data2.getUuidNo()) &&
               Objects.equals(data1.getPodId(), data2.getPodId()) &&
               Objects.equals(data1.getPodFace(), data2.getPodFace()) &&
               Objects.equals(data1.getLocation(), data2.getLocation());
    }
    
    @Override
    protected boolean isConnected() {
        return podDataService.isConnected();
    }
    
    /**
     * 마지막 체크 시간 반환 (private 메서드를 protected로 오버라이드)
     */
    protected LocalDateTime getLastCheckTime() {
        long lastPull = getLastPullTime();
        if (lastPull == 0) {
            return LocalDateTime.now().minusHours(1);
        }
        return LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(lastPull), 
            java.time.ZoneId.systemDefault()
        );
    }
} 