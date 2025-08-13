package com.example.cdcqueue.etl.engine;

import com.example.cdcqueue.common.model.AgvData;
import com.example.cdcqueue.etl.service.AgvDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * AGV 데이터 전용 하이브리드 풀링 엔진
 * 
 * HybridPullingEngine의 구체적인 구현체로 AGV 데이터에 특화된 로직을 제공합니다.
 * 
 * @author AGV Monitoring System
 * @version 2.0
 */
@Component
public class AgvHybridPullingEngine extends HybridPullingEngine<AgvData> {
    
    private static final Logger log = LoggerFactory.getLogger(AgvHybridPullingEngine.class);
    
    private final AgvDataService agvDataService;
    
    @Autowired
    public AgvHybridPullingEngine(AgvDataService agvDataService) {
        this.agvDataService = agvDataService;
    }
    
    @Override
    protected List<AgvData> getChangedData() {
        LocalDateTime lastCheck = getLastCheckTime();
        return agvDataService.getAgvDataAfterTimestamp(lastCheck);
    }
    
    @Override
    protected List<AgvData> getAllData() {
        return agvDataService.getAllAgvData();
    }
    
    @Override
    protected String getDataKey(AgvData data) {
        return data.getRobotNo();
    }
    
    @Override
    protected boolean isSameData(AgvData data1, AgvData data2) {
        // reportTime이 다르면 다른 데이터로 인식 (시간 기반 변경 감지)
        if (!Objects.equals(data1.getReportTime(), data2.getReportTime())) {
            return false;
        }
        
        // 다른 중요 필드들도 비교
        return Objects.equals(data1.getRobotNo(), data2.getRobotNo()) &&
               Objects.equals(data1.getPosX(), data2.getPosX()) &&
               Objects.equals(data1.getPosY(), data2.getPosY()) &&
               Objects.equals(data1.getStatus(), data2.getStatus()) &&
               Objects.equals(data1.getBattery(), data2.getBattery()) &&
               Objects.equals(data1.getSpeed(), data2.getSpeed()) &&
               Objects.equals(data1.getTaskId(), data2.getTaskId());
    }
    
    @Override
    protected boolean isConnected() {
        return agvDataService.isConnected();
    }
    
    /**
     * 마지막 체크 시간 반환 (private 메서드를 protected로 오버라이드)
     */
    protected LocalDateTime getLastCheckTime() {
        long lastPull = getLastPullTime();
        if (lastPull == 0) {
            // 첫 실행 시에는 1년 전으로 설정하여 모든 데이터를 가져오도록 함
            return LocalDateTime.now().minusYears(1);
        }
        return LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(lastPull), 
            java.time.ZoneId.systemDefault()
        );
    }
    
    /**
     * 캐시를 강제로 리셋하여 다음 실행 시 전체 데이터를 다시 처리하도록 함
     */
    public void resetCache() {
        log.info("Resetting AgvHybridPullingEngine cache");
        // 부모 클래스의 resetCache 메서드 호출
        super.resetCache();
    }
} 