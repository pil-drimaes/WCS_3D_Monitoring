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
    
    // 마지막 실행 시간 추적
    private final AtomicLong lastExecutionTime = new AtomicLong(0);
    
    // 중복 데이터 필터링을 위한 캐시 (uuid_no -> 최신 데이터)
    private final ConcurrentHashMap<String, InventoryInfo> processedDataCache = new ConcurrentHashMap<>();
    
    public InventoryDataETLEngine(InventoryDataService inventoryDataService,
                                 KafkaProducerService kafkaProducerService,
                                 PostgreSQLDataService postgreSQLDataService) {
        this.inventoryDataService = inventoryDataService;
        this.kafkaProducerService = kafkaProducerService;
        this.postgreSQLDataService = postgreSQLDataService;
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
    
    /**
     * ETL 프로세스 실행 (내부 구현 - 시간 기반)
     * 
     * @param lastProcessedTime 마지막 처리 시간
     * @return 처리된 재고 데이터 리스트
     */
    private List<InventoryInfo> processETLInternal(LocalDateTime lastProcessedTime) {
        log.debug("재고 정보 ETL 프로세스 시작 - Extract 단계");
        
        try {
            // Extract: WCS DB에서 데이터 추출
            List<InventoryInfo> allData = inventoryDataService.getInventoryDataAfterTimestamp(lastProcessedTime);
            log.debug("추출된 재고 데이터 {}개", allData.size());
            
            if (allData.isEmpty()) {
                log.debug("처리할 새로운 재고 데이터가 없습니다");
                return new ArrayList<>();
            }
            
            // 중복 데이터 필터링
            List<InventoryInfo> filteredData = filterDuplicateData(allData);
            log.debug("중복 필터링 후 {}개 데이터", filteredData.size());
            
            if (filteredData.isEmpty()) {
                log.debug("중복 필터링 후 처리할 데이터가 없습니다");
                return new ArrayList<>();
            }
            
            // Transform: 데이터 변환 (필요시)
            List<InventoryInfo> transformedData = transformData(filteredData);
            
            // Load: PostgreSQL에 데이터 저장
            int savedCount = inventoryDataService.saveInventoryDataBatch(transformedData);
            log.info("재고 데이터 ETL 완료: {}개 중 {}개 저장", transformedData.size(), savedCount);
            
            // Kafka 메시지 발행
            publishToKafka(transformedData);
            
            // 마지막 실행 시간 업데이트
            lastExecutionTime.set(System.currentTimeMillis());
            
            return transformedData;
            
        } catch (Exception e) {
            log.error("재고 정보 ETL 프로세스 실패: {}", e.getMessage(), e);
            throw new RuntimeException("재고 정보 ETL 처리 중 오류 발생", e);
        }
    }
    
    /**
     * ETL 프로세스 실행 (내부 구현 - 데이터 기반)
     * 
     * @param filteredData 이미 필터링된 데이터
     * @return 처리된 재고 데이터 리스트
     */
    private List<InventoryInfo> processETLInternal(List<InventoryInfo> filteredData) {
        log.debug("재고 정보 ETL 프로세스 시작 - Transform & Load 단계");
        
        try {
            if (filteredData.isEmpty()) {
                log.debug("처리할 데이터가 없습니다");
                return new ArrayList<>();
            }
            
            // Transform: 데이터 변환 (필요시)
            List<InventoryInfo> transformedData = transformData(filteredData);
            
            // Load: PostgreSQL에 데이터 저장
            int savedCount = inventoryDataService.saveInventoryDataBatch(transformedData);
            log.info("재고 데이터 ETL 완료: {}개 중 {}개 저장", transformedData.size(), savedCount);
            
            // Kafka 메시지 발행
            publishToKafka(transformedData);
            
            // 마지막 실행 시간 업데이트
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
                
                // 1. PostgreSQL에서 이미 존재하는지 확인 (AGV와 동일한 방식)
                try {
                    boolean existsInPostgres = postgreSQLDataService.isInventoryDataExists(data.getUuidNo(), data.getReportTime());
                    if (existsInPostgres) {
                        log.debug("PostgreSQL에 이미 존재하는 재고 데이터 제외: uuid={}, report_time={}", 
                                 data.getUuidNo(), data.getReportTime());
                        return false;
                    }
                } catch (Exception e) {
                    log.warn("PostgreSQL 중복 체크 실패, 캐시 기반으로 처리: uuid={}, error={}", 
                             data.getUuidNo(), e.getMessage());
                }
                
                // 2. 캐시 기반 비교 (전체 필드 비교)
                String key = data.getUuidNo();
                InventoryInfo cached = processedDataCache.get(key);

                if (cached == null) {
                    processedDataCache.put(key, data);
                    log.debug("새로운 재고 데이터 감지: uuid={}, report_time={}", key, data.getReportTime());
                    return true;
                }

                if (data.getReportTime() > cached.getReportTime()) {
                    boolean same = isSameData(data, cached);
                    processedDataCache.put(key, data);
                    if (!same) {
                        log.debug("업데이트된 재고 데이터 감지: uuid={}, old_time={}, new_time={}", 
                                 key, cached.getReportTime(), data.getReportTime());
                        return true;
                    } else {
                        log.debug("내용 동일로 처리 제외: uuid={}, report_time={}", key, data.getReportTime());
                        return false;
                    }
                }

                if (!isSameData(data, cached)) {
                    log.debug("변경 감지되었으나 최신 아님: uuid={}, cached_time={}, data_time={}",
                              key, cached.getReportTime(), data.getReportTime());
                } else {
                    log.debug("중복 재고 데이터 제외: uuid={}, cached_time={}, data_time={}",
                              key, cached.getReportTime(), data.getReportTime());
                }
                return false;
            })
            .toList();
    }
    
    /**
     * 데이터 변환 (Transform)
     * 
     * @param inventoryDataList 원본 데이터 리스트
     * @return 변환된 데이터 리스트
     */
    private List<InventoryInfo> transformData(List<InventoryInfo> inventoryDataList) {
        // UUID가 없는 경우에만 생성 (원본 UUID 유지)
        inventoryDataList.forEach(data -> {
            if (data.getUuidNo() == null || data.getUuidNo().isEmpty()) {
                data.setUuidNo(UUID.randomUUID().toString());
            }
            
            // report_time이 없는 경우 현재 시간으로 설정
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
                
                kafkaProducerService.sendMessage("inventory-updates", message);
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
        processedDataCache.clear();
        log.info("재고 정보 ETL 엔진 캐시 초기화 완료");
    }
    
    /**
     * 강제 캐시 리셋 (테스트용)
     */
    public void resetCache() {
        processedDataCache.clear();
        lastExecutionTime.set(0);
        log.info("재고 정보 ETL 엔진 캐시 강제 리셋 완료");
    }
    
    /**
     * 중복 데이터 확인
     */
    private boolean isDuplicateData(InventoryInfo data) {
        String key = getDataKey(data);
        InventoryInfo cached = processedDataCache.get(key);
        
        if (cached == null) {
            processedDataCache.put(key, data);
            return false;
        }
        
        if (data.getReportTime() > cached.getReportTime()) {
            boolean same = isSameData(data, cached);
            processedDataCache.put(key, data);
            return !same;
        }
        
        return false;
    }
    
    // ETLEngine 추상 메서드 구현
    
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
        return data1 != null && data1.equals(data2);
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