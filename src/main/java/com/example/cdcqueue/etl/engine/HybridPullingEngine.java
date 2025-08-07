package com.example.cdcqueue.etl.engine;

import com.example.cdcqueue.common.model.AgvData;
import com.example.cdcqueue.etl.service.AgvDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 하이브리드 풀링 엔진
 * 
 * 조건부 쿼리와 전체 데이터 비교를 조합한 효율적인 데이터 변화 감지 엔진
 * WCS DB 서비스를 사용하여 데이터를 가져옵니다.
 * 
 * 동작 방식:
 * 1. 첫 실행 시: 전체 데이터를 가져와서 캐시에 저장
 * 2. 이후 실행: 조건부 쿼리로 변경된 데이터만 가져옴
 * 3. 주기적으로: 전체 데이터와 비교하여 동기화
 * 
 * 성능 최적화:
 * - 조건부 쿼리로 대부분의 경우 빠른 감지
 * - 주기적 전체 비교로 데이터 정합성 보장
 * - 캐시를 통한 중복 처리 방지
 * 
 * @author AGV Monitoring System
 * @version 2.0
 */
@Component
public class HybridPullingEngine implements DataPullingEngine {
    
    private static final Logger log = LoggerFactory.getLogger(HybridPullingEngine.class);
    
    /**
     * AGV 데이터 서비스
     */
    private final AgvDataService wcsDatabaseService;
    
    /**
     * 마지막 풀링 시간
     */
    private final AtomicLong lastPullTime = new AtomicLong(0);
    
    /**
     * 마지막 전체 동기화 시간
     */
    private final AtomicLong lastFullSyncTime = new AtomicLong(0);
    
    /**
     * AGV 데이터 캐시 (robot_no -> AgvData)
     */
    private final Map<String, AgvData> dataCache = new ConcurrentHashMap<>();
    
    /**
     * 엔진 상태
     */
    private final AtomicReference<EngineStatus> status = new AtomicReference<>(EngineStatus.STOPPED);
    
    /**
     * 엔진 상태 열거형
     */
    public enum EngineStatus {
        RUNNING, STOPPED, ERROR
    }
    
    /**
     * 생성자
     * 
     * @param wcsDatabaseService AGV 데이터 서비스
     */
    @Autowired
    public HybridPullingEngine(AgvDataService wcsDatabaseService) {
        this.wcsDatabaseService = wcsDatabaseService;
    }
    
    @Override
    public void initialize(PullingEngineConfig config) {
        // WCS DB 연결 상태 확인
        if (!wcsDatabaseService.isConnected()) {
            log.error("WCS Database is not connected. HybridPullingEngine initialization failed.");
            status.set(EngineStatus.ERROR);
            return;
        }
        
        // 캐시 초기화
        resetCache();
        
        // 초기 전체 데이터 로드
        loadFullData();
        
        status.set(EngineStatus.RUNNING);
        log.info("HybridPullingEngine initialized successfully with WCS Database Service");
    }
    
    @Override
    public List<AgvData> pullNewData() {
        try {
            status.set(EngineStatus.RUNNING);
            long currentTime = System.currentTimeMillis();
            
            List<AgvData> newData = new ArrayList<>();
            
            // 첫 실행인 경우 전체 데이터를 새로운 데이터로 반환
            if (lastPullTime.get() == 0) {
                log.info("First run detected, returning all cached data as new data");
                newData = new ArrayList<>(dataCache.values());
                lastPullTime.set(currentTime);
                lastFullSyncTime.set(currentTime);
                return newData;
            }
            
            // 캐시가 비어있는 경우 전체 데이터를 다시 로드
            if (dataCache.isEmpty()) {
                log.info("Cache is empty, reloading full data");
                loadFullData();
                newData = new ArrayList<>(dataCache.values());
                lastPullTime.set(currentTime);
                lastFullSyncTime.set(currentTime);
                return newData;
            }
            
            // 조건부 쿼리로 변경된 데이터 가져오기
            List<AgvData> changedData = getChangedData();
            log.debug("Retrieved {} changed data records from WCS DB", changedData.size());
            
            // 전체 동기화가 필요한지 확인 (1시간마다)
            if (shouldPerformFullSync()) {
                log.info("Performing full data synchronization");
                newData = performFullSync();
                lastFullSyncTime.set(currentTime);
            } else {
                // 변경된 데이터만 처리
                newData = processChangedData(changedData);
            }
            
            lastPullTime.set(currentTime);
            
            if (!newData.isEmpty()) {
                log.info("Detected {} new/changed AGV data records", newData.size());
            } else {
                log.debug("No new data detected in this pull cycle");
            }
            
            return newData;
            
        } catch (Exception e) {
            status.set(EngineStatus.ERROR);
            log.error("Error in pullNewData: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 변경된 데이터를 조건부 쿼리로 가져옴
     * 변경된 데이터는 첫번째, 이전 1시간 데이터 중 변경된 데이터만 가져옴
     * 첫번째 데이터는 캐시에 없는 데이터만 가져옴
     * 두번째 시간 
     * 14:00:00.000 - 첫 실행
        ├─ 기준 시간: 13:00:00.000 (1시간 전)
        ├─ 가져오는 데이터: 13:00:00 ~ 14:00:00 (1시간 분량)
        └─ lastPullTime = 14:00:00.000

        14:00:00.100 - 두 번째 실행 (0.1초 후)
        ├─ 기준 시간: 14:00:00.000 (이전 풀링 시간)
        ├─ 가져오는 데이터: 14:00:00 ~ 14:00:00.100 (0.1초 분량)
        └─ lastPullTime = 14:00:00.100

        14:00:00.200 - 세 번째 실행 (0.1초 후)
        ├─ 기준 시간: 14:00:00.100 (이전 풀링 시간)
        ├─ 가져오는 데이터: 14:00:00.100 ~ 14:00:00.200 (0.1초 분량)
        └─ lastPullTime = 14:00:00.200
     */
    private List<AgvData> getChangedData() {
        LocalDateTime lastCheck = getLastCheckTime();
        return wcsDatabaseService.getAgvDataAfterTimestamp(lastCheck);
    }
    
    /**
     * 변경된 데이터를 처리하여 새로운 데이터만 반환
     */
    private List<AgvData> processChangedData(List<AgvData> changedData) {
        List<AgvData> newData = new ArrayList<>();
        
        log.debug("Processing {} changed data records", changedData.size());
        
        for (AgvData data : changedData) {
            AgvData cachedData = dataCache.get(data.getRobotNo());
            
            // 캐시에 없거나 데이터가 변경된 경우
            if (cachedData == null || !isSameData(cachedData, data)) {
                newData.add(data);
                dataCache.put(data.getRobotNo(), data);
                log.debug("Added new/changed data for robot: {}", data.getRobotNo());
            } else {
                log.debug("Skipped unchanged data for robot: {}", data.getRobotNo());
            }
        }
        
        log.debug("Found {} new/changed records out of {} total", newData.size(), changedData.size());
        return newData;
    }
    
    /**
     * 전체 데이터 동기화 수행
     */
    private List<AgvData> performFullSync() {
        List<AgvData> allData = wcsDatabaseService.getAllAgvData();
        
        // 캐시 업데이트 및 새로운 데이터 식별
        List<AgvData> newData = new ArrayList<>();
        Map<String, AgvData> newCache = new HashMap<>();
        
        for (AgvData data : allData) {
            newCache.put(data.getRobotNo(), data);
            
            AgvData cachedData = dataCache.get(data.getRobotNo());
            if (cachedData == null || !isSameData(cachedData, data)) {
                newData.add(data);
            }
        }
        
        // 캐시 교체
        dataCache.clear();
        dataCache.putAll(newCache);
        
        return newData;
    }
    
    /**
     * 전체 동기화가 필요한지 확인
     */
    private boolean shouldPerformFullSync() {
        long currentTime = System.currentTimeMillis();
        long lastSync = lastFullSyncTime.get();
        
        // 1시간(3600000ms)마다 전체 동기화
        return (currentTime - lastSync) > 3600000;
    }
    
    /**
     * 마지막 체크 시간 반환
     */
    private LocalDateTime getLastCheckTime() {
        long lastPull = lastPullTime.get();
        if (lastPull == 0) {
            return LocalDateTime.now().minusHours(1);
        }
        return LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(lastPull), 
            java.time.ZoneId.systemDefault()
        );
    }
    
    /**
     * 초기 전체 데이터 로드
     */
    private void loadFullData() {
        log.info("Loading initial full data");
        List<AgvData> allData = wcsDatabaseService.getAllAgvData();
        
        // 초기 데이터를 캐시에 저장
        for (AgvData data : allData) {
            dataCache.put(data.getRobotNo(), data);
        }
        
        log.info("Loaded {} initial AGV data records into cache", allData.size());
    }
    
    /**
     * 두 AGV 데이터가 동일한지 비교
     * 시간 기반 변경 감지로 수정 (reportTime이 다르면 다른 데이터로 인식)
     */
    private boolean isSameData(AgvData data1, AgvData data2) {
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
    public boolean isHealthy() {
        return status.get() == EngineStatus.RUNNING && wcsDatabaseService.isConnected();
    }
    
    @Override
    public long getLastPullTime() {
        return lastPullTime.get();
    }
    
    /**
     * 캐시를 강제로 리셋하여 다음 실행 시 전체 데이터를 다시 처리하도록 함
     */
    public void resetCache() {
        log.info("Resetting HybridPullingEngine cache");
        dataCache.clear();
        lastPullTime.set(0);
        lastFullSyncTime.set(0);
    }
} 