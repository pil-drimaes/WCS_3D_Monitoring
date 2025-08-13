package com.example.cdcqueue.etl.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 하이브리드 풀링 엔진 (제네릭)
 * 
 * 조건부 쿼리와 전체 데이터 비교를 조합한 효율적인 데이터 변화 감지 엔진
 * 다양한 데이터 타입을 지원합니다.
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
public abstract class HybridPullingEngine<T> implements DataPullingEngine<T> {
    
    private static final Logger log = LoggerFactory.getLogger(HybridPullingEngine.class);
    
    /**
     * 마지막 풀링 시간
     */
    private final AtomicLong lastPullTime = new AtomicLong(0);
    
    /**
     * 마지막 전체 동기화 시간
     */
    private final AtomicLong lastFullSyncTime = new AtomicLong(0);
    
    /**
     * 데이터 캐시 (key -> T)
     */
    private final Map<String, T> dataCache = new ConcurrentHashMap<>();
    
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
     */
    public HybridPullingEngine() {
    }
    
    @Override
    public void initialize(PullingEngineConfig config) {
        // 연결 상태 확인
        if (!isConnected()) {
            log.error("Database is not connected. HybridPullingEngine initialization failed.");
            status.set(EngineStatus.ERROR);
            return;
        }
        
        // 캐시 초기화
        resetCache();
        
        // 초기 전체 데이터 로드
        loadFullData();
        
        status.set(EngineStatus.RUNNING);
        log.info("HybridPullingEngine initialized successfully");
    }
    
    @Override
    public List<T> pullNewData() {
        try {
            status.set(EngineStatus.RUNNING);
            long currentTime = System.currentTimeMillis();
            
            List<T> newData = new ArrayList<>();
            
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
            List<T> changedData = getChangedData();
            log.debug("Retrieved {} changed data records from database", changedData.size());
            
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
                log.info("Detected {} new/changed data records", newData.size());
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
     * 변경된 데이터를 조건부 쿼리로 가져옴 (하위 클래스에서 구현)
     */
    protected abstract List<T> getChangedData();
    
    /**
     * 전체 데이터를 가져옴 (하위 클래스에서 구현)
     */
    protected abstract List<T> getAllData();
    
    /**
     * 데이터의 키를 반환 (하위 클래스에서 구현)
     */
    protected abstract String getDataKey(T data);
    
    /**
     * 두 데이터가 동일한지 비교 (하위 클래스에서 구현)
     */
    protected abstract boolean isSameData(T data1, T data2);
    
    /**
     * 데이터베이스 연결 상태 확인 (하위 클래스에서 구현)
     */
    protected abstract boolean isConnected();
    
    /**
     * 변경된 데이터를 처리하여 새로운 데이터만 반환
     */
    private List<T> processChangedData(List<T> changedData) {
        List<T> newData = new ArrayList<>();
        
        log.debug("Processing {} changed data records", changedData.size());
        
        for (T data : changedData) {
            String key = getDataKey(data);
            T cachedData = dataCache.get(key);
            
            // 캐시에 없거나 데이터가 변경된 경우
            if (cachedData == null || !isSameData(cachedData, data)) {
                newData.add(data);
                dataCache.put(key, data);
                log.debug("Added new/changed data for key: {}", key);
            } else {
                log.debug("Skipped unchanged data for key: {}", key);
            }
        }
        
        log.debug("Found {} new/changed records out of {} total", newData.size(), changedData.size());
        return newData;
    }
    
    /**
     * 전체 데이터 동기화 수행
     */
    private List<T> performFullSync() {
        List<T> allData = getAllData();
        
        // 캐시 업데이트 및 새로운 데이터 식별
        List<T> newData = new ArrayList<>();
        Map<String, T> newCache = new HashMap<>();
        
        for (T data : allData) {
            String key = getDataKey(data);
            newCache.put(key, data);
            
            T cachedData = dataCache.get(key);
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
        
        // 10분(600000ms)마다 전체 동기화 (더 빠른 데이터 감지를 위해)
        return (currentTime - lastSync) > 600000;
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
        List<T> allData = getAllData();
        
        // 초기 데이터를 캐시에 저장
        for (T data : allData) {
            String key = getDataKey(data);
            dataCache.put(key, data);
        }
        
        log.info("Loaded {} initial data records into cache", allData.size());
    }
    
    @Override
    public boolean isHealthy() {
        return status.get() == EngineStatus.RUNNING && isConnected();
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