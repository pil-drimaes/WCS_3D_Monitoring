package com.example.WCS_DataStream.etl.engine;

import com.example.WCS_DataStream.etl.ETLEngineException;
import com.example.WCS_DataStream.etl.ETLStatistics;
import com.example.WCS_DataStream.etl.model.InventoryInfo;
import com.example.WCS_DataStream.etl.service.InventoryDataService;

import com.example.WCS_DataStream.etl.service.PostgreSQLDataService;
import com.example.WCS_DataStream.etl.service.KafkaProducerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.example.WCS_DataStream.etl.service.RedisCacheService;

/**
 * 재고 정보 ETL 엔진
 * 
 * 재고 정보 데이터의 Extract, Transform, Load를 처리하는 엔진
 * 중복 데이터 필터링을 포함한 효율적인 ETL 처리
 * 
 * @author AGV Monitoring System
 * @version 1.0
 */
@Component
public class InventoryDataETLEngine extends ETLEngine<InventoryInfo> {
    
    private static final Logger log = LoggerFactory.getLogger(InventoryDataETLEngine.class);
    
    private final InventoryDataService inventoryDataService;
    private final KafkaProducerService kafkaProducerService;
    private final PostgreSQLDataService postgreSQLDataService;
    private final RedisCacheService redisCacheService;

    private static final String CACHE_NS = "inventory-cache";
    
    // 마지막 실행 시간 추적
    private final AtomicLong lastExecutionTime = new AtomicLong(0);
    
    // 중복 데이터 필터링을 위한 캐시 (uuid_no -> 최신 데이터)
    // private final ConcurrentHashMap<String, InventoryInfo> processedDataCache = new ConcurrentHashMap<>();
    
    public InventoryDataETLEngine(InventoryDataService inventoryDataService,
                                 KafkaProducerService kafkaProducerService,
                                 PostgreSQLDataService postgreSQLDataService,
                                 RedisCacheService redisCacheService) {
        this.inventoryDataService = inventoryDataService;
        this.kafkaProducerService = kafkaProducerService;
        this.postgreSQLDataService = postgreSQLDataService;
        this.redisCacheService = redisCacheService;
    }

    @Override
    protected boolean checkTableExists() {
        return postgreSQLDataService.isInventoryTableExists();
    }
    
    /**
     * ETL 프로세스 실행 (DataETLEngine 인터페이스 구현)
     * 
     * @return 처리된 재고 데이터 리스트
     * @throws ETLEngineException ETL 처리 중 오류 발생 시
     */
    @Override
    public List<InventoryInfo> executeETL() throws ETLEngineException {
        return super.executeETL();
    }

    public List<InventoryInfo> executeFullLoad() throws ETLEngineException {
        try {
            if (!checkTableExists()) {
                throw new ETLEngineException("PostgreSQL inventory_info 테이블이 존재하지 않음");
            }
            List<InventoryInfo> allData = inventoryDataService.getAllInventoryData();
            List<InventoryInfo> filtered = filterDuplicateData(allData);
            return processETLInternal(filtered);
        } catch (Exception e) {
            throw new ETLEngineException("재고 전체 로드 중 오류", e);
        }
    }
    
    private List<InventoryInfo> processETLInternal(LocalDateTime lastProcessedTime) {
        log.debug("재고 정보 ETL 프로세스 시작 - Extract 단계");
        
        try {
            List<InventoryInfo> allData = inventoryDataService.getInventoryDataAfterTimestamp(lastProcessedTime);
            log.debug("추출된 재고 데이터 {}개", allData.size());
            
            if (allData.isEmpty()) {
                log.debug("처리할 새로운 재고 데이터가 없습니다");
                return new ArrayList<>();
            }
            
            List<InventoryInfo> filteredData = filterDuplicateData(allData);
            log.debug("중복 필터링 후 {}개 데이터", filteredData.size());
            
            if (filteredData.isEmpty()) {
                log.debug("중복 필터링 후 처리할 데이터가 없습니다");
                return new ArrayList<>();
            }
            
            List<InventoryInfo> transformedData = transformData(filteredData);
            
            int savedCount = inventoryDataService.saveInventoryDataBatch(transformedData);
            log.info("재고 데이터 ETL 완료: {}개 중 {}개 저장", transformedData.size(), savedCount);
            
            publishToKafka(transformedData);
            
            lastExecutionTime.set(System.currentTimeMillis());
            
            return transformedData;
            
        } catch (Exception e) {
            log.error("재고 정보 ETL 프로세스 실패: {}", e.getMessage(), e);
            throw new RuntimeException("재고 정보 ETL 처리 중 오류 발생", e);
        }
    }
    
    private List<InventoryInfo> processETLInternal(List<InventoryInfo> filteredData) {
        log.debug("재고 정보 ETL 프로세스 시작 - Transform & Load 단계");
        
        try {
            if (filteredData.isEmpty()) {
                log.debug("처리할 데이터가 없습니다");
                return new ArrayList<>();
            }
            
            List<InventoryInfo> transformedData = transformData(filteredData);
            
            int savedCount = inventoryDataService.saveInventoryDataBatch(transformedData);
            log.info("재고 데이터 ETL 완료: {}개 중 {}개 저장", transformedData.size(), savedCount);
            
            publishToKafka(transformedData);
            
            lastExecutionTime.set(System.currentTimeMillis());
            
            return transformedData;
            
        } catch (Exception e) {
            log.error("재고 정보 ETL 프로세스 실패: {}", e.getMessage(), e);
            throw new RuntimeException("재고 정보 ETL 처리 중 오류 발생", e);
        }
    }
    
    /**
     * ETL 프로세스 실행 (기존 메서드 - 호환성 유지)
     * 
     * @param lastProcessedTime 마지막 처리 시간
     * @return 처리된 레코드 수
     */
    public int processETL(LocalDateTime lastProcessedTime) {
        List<InventoryInfo> result = processETLInternal(lastProcessedTime);
        return result.size();
    }
    
    /**
     * 중복 데이터 필터링 (PostgreSQL 기반 + 캐시 기반)
     * 
     * @param allData 모든 데이터
     * @return 중복이 제거된 데이터
     */
    private List<InventoryInfo> filterDuplicateData(List<InventoryInfo> allData) {
        return allData.stream()
            .filter(data -> {
                if (data.getUuidNo() == null || data.getReportTime() == null) {
                    return true; // UUID나 report_time이 없으면 처리
                }
                
                // 1) PostgreSQL 중복 체크 (uuid_no, report_time)
                try {
                    boolean existsInPostgres = postgreSQLDataService.isInventoryDataExists(data.getUuidNo(), data.getReportTime());
                    if (existsInPostgres) {
                        log.debug("PostgreSQL에 이미 존재하는 재고 데이터 제외: uuid={}, report_time={}", 
                                 data.getUuidNo(), data.getReportTime());
                        return false;
                    }
                } catch (Exception e) {
                    log.warn("PostgreSQL 중복 체크 실패, Redis 비교로 진행: uuid={}, error={}", 
                             data.getUuidNo(), e.getMessage());
                }
                
                // 2) Redis 캐시 비교
                String key = data.getUuidNo();
                InventoryInfo cached = redisCacheService.get(CACHE_NS, key, InventoryInfo.class);
                
                if (cached == null) {
                    // 첫 데이터: 캐시에 저장하고 포함
                    try {
                        InventoryInfo latest = postgreSQLDataService.getLatestInventoryByInventory(data.getInventory());
                        if (latest != null && isSameData(data, latest)) {
                            // 내용 동일: report_time만 캐시에 반영하고 제외
                            latest.setReportTime(data.getReportTime());
                            redisCacheService.set(CACHE_NS, key, latest);
                            log.debug("내용 동일로 처리 제외(DB fallback, report_time만 갱신): uuid={}, report_time={}", key, data.getReportTime());
                            return false;
                        }
                    } catch (Exception e) {
                        log.debug("DB fallback 비교 실패(inventory): uuid={}, error={}", key, e.getMessage());
                    }
                    redisCacheService.set(CACHE_NS, key, data);
                    log.debug("첫 재고 데이터 캐시 저장(REDIS): uuid={}, report_time={}", key, data.getReportTime());
                    return true;
                }
                
                if (data.getReportTime() > cached.getReportTime()) {
                    boolean same = isSameData(data, cached);
                    if (same) {
                        // 내용 동일: report_time만 갱신하고 제외
                        cached.setReportTime(data.getReportTime());
                        redisCacheService.set(CACHE_NS, key, cached);
                        log.debug("내용 동일로 처리 제외(REDIS, report_time만 갱신): uuid={}, report_time={}", key, data.getReportTime());
                        return false;
                    } else {
                        // 내용 변경: 전체 갱신 후 포함
                        redisCacheService.set(CACHE_NS, key, data);
                        log.debug("업데이트된 재고 데이터 감지(REDIS): uuid={}, old_time={}, new_time={}",
                                 key, cached.getReportTime(), data.getReportTime());
                        return true;
                    }
                }
                
                // 최신이 아니거나 같은 시간: 제외
                if (!isSameData(data, cached)) {
                    log.debug("변경 감지되었으나 최신 아님(REDIS): uuid={}, cached_time={}, data_time={}",
                              key, cached.getReportTime(), data.getReportTime());
                } else {
                    log.debug("중복 재고 데이터 제외(REDIS): uuid={}, cached_time={}, data_time={}",
                              key, cached.getReportTime(), data.getReportTime());
                }
                return false;
            })
            .toList();
    }
    
    private List<InventoryInfo> transformData(List<InventoryInfo> inventoryDataList) {
        inventoryDataList.forEach(data -> {
            if (data.getUuidNo() == null || data.getUuidNo().isEmpty()) {
                data.setUuidNo(UUID.randomUUID().toString());
            }
            
            if (data.getReportTime() == null) {
                data.setReportTime(System.currentTimeMillis());
            }
        });
        
        return inventoryDataList;
    }
    
    /**
     * Kafka로 메시지 발행
     * 
     * @param inventoryDataList 발행할 데이터 리스트
     */
    private void publishToKafka(List<InventoryInfo> inventoryDataList) {
        try {
            for (InventoryInfo inventoryInfo : inventoryDataList) {
                String message = String.format(
                    "재고 정보 업데이트: inventory=%s, batch_num=%s, pre_qty=%d, new_qty=%d, status=%d",
                    inventoryInfo.getInventory(),
                    inventoryInfo.getBatchNum(),
                    inventoryInfo.getPreQty(),
                    inventoryInfo.getNewQty(),
                    inventoryInfo.getStatus()
                );
                
                kafkaProducerService.sendMessageWithCallback("inventory-updates", inventoryInfo.getUuidNo(), message, true);
            }
            
            log.debug("재고 정보 Kafka 메시지 {}개 발행 완료", inventoryDataList.size());
            
        } catch (Exception e) {
            log.error("Kafka 메시지 발행 실패: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 최신 타임스탬프 조회
     * 
     * @return 최신 타임스탬프
     */
    public LocalDateTime getLatestTimestamp() {
        try {
            LocalDateTime latest = inventoryDataService.getLatestTimestamp();
            if (latest == null) {
                // 첫 실행 시에는 1년 전으로 설정하여 모든 데이터를 처리하도록 함
                log.info("재고 정보 ETL 엔진 첫 실행: 1년 전부터 모든 데이터 처리");
                return LocalDateTime.now().minusYears(1);
            }
            return latest;
        } catch (Exception e) {
            log.warn("최신 타임스탬프 조회 실패, 1년 전으로 설정: {}", e.getMessage());
            return LocalDateTime.now().minusYears(1);
        }
    }
    

    
    /**
     * 마지막 실행 시간 조회
     * 
     * @return 마지막 실행 시간
     */
    public long getLastExecutionTime() {
        return lastExecutionTime.get();
    }
    
    /**
     * 캐시 초기화 (메모리 관리용)
     */
    public void clearCache() {
        redisCacheService.clearNamespace(CACHE_NS);
        log.info("재고 정보 ETL 엔진 캐시 초기화 완료 (REDIS)");
    }
    
    /**
     * 강제 캐시 리셋 (테스트용)
     */
    public void resetCache() {
        redisCacheService.clearNamespace(CACHE_NS);
        lastExecutionTime.set(0);
        log.info("재고 정보 ETL 엔진 캐시 강제 리셋 완료 (REDIS)");
    }
    
    @Override
    protected List<InventoryInfo> extractData() throws ETLEngineException {
        try {
            long wm = postgreSQLDataService.getInventoryLastProcessedTime();
            LocalDateTime lastProcessedTime = java.time.Instant.ofEpochMilli(wm)
                .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
            return inventoryDataService.getInventoryDataAfterTimestamp(lastProcessedTime);
        } catch (Exception e) {
            throw new ETLEngineException("Error during data extraction", e);
        }
    }
    
    @Override
    protected List<InventoryInfo> transformAndLoad(List<InventoryInfo> data) throws ETLEngineException {
        try {
            List<InventoryInfo> transformed = transformData(data);
            List<InventoryInfo> filtered = filterDuplicateData(transformed); 
            inventoryDataService.saveInventoryDataBatch(filtered);
            publishToKafka(filtered);
            return filtered;
        } catch (Exception e) {
            throw new ETLEngineException("Error during transform and load", e);
        }
    }
    
    @Override
    public boolean isConnected() {
        return inventoryDataService.isConnected();
    }
    
    @Override
    protected String getDataKey(InventoryInfo data) {
        return data.getUuidNo();
    }
    
    @Override
    protected boolean isSameData(InventoryInfo data1, InventoryInfo data2) {
        InventoryInfo a = data1, b = data2;
        if (a == b) return true;
        if (a == null || b == null) return false;
        // 설비/아이템 식별(변하지 않는 키)
        boolean sameIdentity =
            java.util.Objects.equals(a.getUuidNo(), b.getUuidNo()) &&
            java.util.Objects.equals(a.getInventory(), b.getInventory());
        if (!sameIdentity) return false;

        // 변화 감지 대상 필드만 비교: newQty
        return java.util.Objects.equals(a.getBatchNum(), b.getBatchNum())
            && java.util.Objects.equals(a.getUnitload(), b.getUnitload())
            && java.util.Objects.equals(a.getSku(), b.getSku())
            && java.util.Objects.equals(a.getPreQty(), b.getPreQty())
            && java.util.Objects.equals(a.getNewQty(), b.getNewQty())
            && java.util.Objects.equals(a.getOriginOrder(), b.getOriginOrder())
            && java.util.Objects.equals(a.getStatus(), b.getStatus());
            // && java.util.Objects.equals(a.getReportTime(), b.getReportTime());
    }

    @Override
    public boolean isHealthy() {
        return super.isHealthy();
    }
    
    @Override
    public ETLStatistics getStatistics() {
        return super.getStatistics();
    }
} 