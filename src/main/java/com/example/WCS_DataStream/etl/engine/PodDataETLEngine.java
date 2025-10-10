package com.example.WCS_DataStream.etl.engine;

import com.example.WCS_DataStream.etl.ETLEngineException;
import com.example.WCS_DataStream.etl.model.PodInfo;
import com.example.WCS_DataStream.etl.service.PodDataService;
import com.example.WCS_DataStream.etl.service.KafkaProducerService;
import com.example.WCS_DataStream.etl.service.PostgreSQLDataService;
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
 * POD 정보 ETL 엔진
 * 
 * POD 정보 데이터의 Extract, Transform, Load를 처리하는 엔진
 * 중복 데이터 필터링을 포함한 효율적인 ETL 처리
 * 
 * @author AGV Monitoring System
 * @version 2.0
 */

@Component
public class PodDataETLEngine extends ETLEngine<PodInfo> {
    
    private static final Logger log = LoggerFactory.getLogger(PodDataETLEngine.class);
    
    private final PodDataService podDataService;
    private final KafkaProducerService kafkaProducerService;
    private final PostgreSQLDataService postgreSQLDataService;
    private final RedisCacheService redisCacheService;

    private static final String CACHE_NS = "pod-cache";
    
    // 마지막 실행 시간 추적
    private final AtomicLong lastExecutionTime = new AtomicLong(0);
    
    // 중복 데이터 필터링을 위한 캐시 (uuid_no -> 최신 데이터)
    // private final ConcurrentHashMap<String, PodInfo> processedDataCache = new ConcurrentHashMap<>();
    
    public PodDataETLEngine(PodDataService podDataService,
                           KafkaProducerService kafkaProducerService,
                           PostgreSQLDataService postgreSQLDataService,
                           RedisCacheService redisCacheService) {
        this.podDataService = podDataService;
        this.kafkaProducerService = kafkaProducerService;
        this.postgreSQLDataService = postgreSQLDataService;
        this.redisCacheService = redisCacheService;
    }
    
    /**
     * ETL 프로세스 실행 (DataETLEngine 인터페이스 구현)
     * 
     * @return 처리된 POD 데이터 리스트
     * @throws ETLEngineException ETL 처리 중 오류 발생 시
     */
    @Override
    protected boolean checkTableExists() {
        return postgreSQLDataService.isPodTableExists();
    }
    @Override
    public List<PodInfo> executeETL() throws ETLEngineException {
        return super.executeETL();
    }

    public List<PodInfo> executeFullLoad() throws ETLEngineException {
        try {
            if (!checkTableExists()) {
                throw new ETLEngineException("PostgreSQL pod_info 테이블이 존재하지 않음");
            }
            List<PodInfo> allData = podDataService.getAllPodData();
            List<PodInfo> transformed = transformData(allData);
            List<PodInfo> filtered = filterDuplicateData(transformed);
            return processETLInternal(filtered);
        } catch (Exception e) {
            throw new ETLEngineException("POD 전체 로드 중 오류", e);
        }
    }
    
    /**
     * ETL 프로세스 실행 (기존 메서드 - 호환성 유지)
     * 
     * @param lastProcessedTime 마지막 처리 시간
     * @return 처리된 레코드 수
     */
    public int processETL(LocalDateTime lastProcessedTime) {
        log.debug("POD 정보 ETL 프로세스 시작 - Extract 단계");
        
        try {
            // Extract: WCS DB에서 데이터 추출
            List<PodInfo> allData = podDataService.getPodDataAfterTimestamp(lastProcessedTime);
            log.debug("추출된 POD 데이터 {}개", allData.size());
            
            if (allData.isEmpty()) {
                log.debug("처리할 새로운 POD 데이터가 없습니다");
                return 0;
            }
            
            // 중복 데이터 필터링
            List<PodInfo> filteredData = filterDuplicateData(allData);
            log.debug("중복 필터링 후 {}개 데이터", filteredData.size());
            
            if (filteredData.isEmpty()) {
                log.debug("중복 필터링 후 처리할 데이터가 없습니다");
                return 0;
            }
            
            // Transform: 데이터 변환 (필요시)
            List<PodInfo> transformedData = transformData(filteredData);
            
            // Load: PostgreSQL에 데이터 저장
            int savedCount = podDataService.savePodDataBatch(transformedData);
            log.info("POD 데이터 ETL 완료: {}개 중 {}개 저장", transformedData.size(), savedCount);
            
            // Kafka 메시지 발행
            publishToKafka(transformedData);
            
            // 마지막 실행 시간 업데이트
            lastExecutionTime.set(System.currentTimeMillis());
            
            return savedCount;
            
        } catch (Exception e) {
            log.error("POD 정보 ETL 프로세스 실패: {}", e.getMessage(), e);
            throw new RuntimeException("POD 정보 ETL 처리 중 오류 발생", e);
        }
    }
    
    /**
     * ETL 프로세스 실행 (내부 구현 - 데이터 기반)
     * 
     * @param filteredData 이미 필터링된 데이터
     * @return 처리된 POD 데이터 리스트
     */
    private List<PodInfo> processETLInternal(List<PodInfo> filteredData) {
        log.debug("POD 정보 ETL 프로세스 시작 - Transform & Load 단계");
        
        try {
            if (filteredData.isEmpty()) {
                log.debug("처리할 데이터가 없습니다");
                return new ArrayList<>();
            }
            
            // Transform: 데이터 변환 (필요시)
            List<PodInfo> transformedData = transformData(filteredData);
            
            // Load: PostgreSQL에 데이터 저장
            int savedCount = podDataService.savePodDataBatch(transformedData);
            log.info("POD 데이터 ETL 완료: {}개 중 {}개 저장", transformedData.size(), savedCount);
            
            // Kafka 메시지 발행
            publishToKafka(transformedData);
            
            // 마지막 실행 시간 업데이트
            lastExecutionTime.set(System.currentTimeMillis());
            
            return transformedData;
            
        } catch (Exception e) {
            log.error("POD 정보 ETL 프로세스 실패: {}", e.getMessage(), e);
            throw new RuntimeException("POD 정보 ETL 처리 중 오류 발생", e);
        }
    }
    
    /**
     * 중복 데이터 필터링 (PostgreSQL 기반 + 캐시 기반)
     * 
     * @param allData 모든 데이터
     * @return 중복이 제거된 데이터
     */
    private List<PodInfo> filterDuplicateData(List<PodInfo> allData) {
        return allData.stream()
            .filter(data -> {
                if (data.getUuidNo() == null || data.getReportTime() == null) {
                    return true; // UUID나 report_time이 없으면 처리
                }
                
                // 1) PostgreSQL 중복 체크 (uuid_no, report_time)
                try {
                    boolean existsInPostgres = postgreSQLDataService.isPodDataExists(data.getUuidNo(), data.getReportTime());
                    if (existsInPostgres) {
                        log.debug("PostgreSQL에 이미 존재하는 POD 데이터 제외: uuid={}, report_time={}", 
                                 data.getUuidNo(), data.getReportTime());
                        return false;
                    }
                } catch (Exception e) {
                    log.warn("PostgreSQL 중복 체크 실패, Redis 비교로 진행: uuid={}, error={}", 
                             data.getUuidNo(), e.getMessage());
                }
                
                // 2) Redis 캐시 비교
                String key = data.getUuidNo();
                PodInfo cached = redisCacheService.get(CACHE_NS, key, PodInfo.class);
                
                if (cached == null) {
                    // 첫 데이터: 캐시에 저장하고 포함
                    redisCacheService.set(CACHE_NS, key, data);
                    log.debug("첫 POD 데이터 캐시 저장(REDIS): uuid={}, report_time={}", key, data.getReportTime());
                    return true;
                }
                
                if (data.getReportTime() >= cached.getReportTime()) {
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
                        log.debug("업데이트된 POD 데이터 감지(REDIS): uuid={}, old_time={}, new_time={}",
                                 key, cached.getReportTime(), data.getReportTime());
                        return true;
                    }
                }
                
                // 최신이 아니거나 같은 시간: 제외
                if (!isSameData(data, cached)) {
                    log.debug("변경 감지되었으나 최신 아님(REDIS): uuid={}, cached_time={}, data_time={}",
                              key, cached.getReportTime(), data.getReportTime());
                } else {
                    log.debug("중복 POD 데이터 제외(REDIS): uuid={}, cached_time={}, data_time={}",
                              key, cached.getReportTime(), data.getReportTime());
                }
                return false;
            })
            .toList();
    }
    
    /**
     * 데이터 변환 (Transform)
     * 
     * @param podDataList 원본 데이터 리스트
     * @return 변환된 데이터 리스트
     */
    private List<PodInfo> transformData(List<PodInfo> podDataList) {
        // UUID가 없는 경우에만 생성 (원본 UUID 유지)
        podDataList.forEach(data -> {
            if (data.getUuidNo() == null || data.getUuidNo().isEmpty()) {
                data.setUuidNo(UUID.randomUUID().toString());
            }
            
            // report_time이 없는 경우 현재 시간으로 설정
            if (data.getReportTime() == null) {
                data.setReportTime(System.currentTimeMillis());
            }
        });
        
        return podDataList;
    }
    
    /**
     * Kafka로 메시지 발행
     * 
     * @param podDataList 발행할 데이터 리스트
     */
    private void publishToKafka(List<PodInfo> podDataList) {
        try {
            for (PodInfo podInfo : podDataList) {
                String message = String.format(
                    "POD 정보 업데이트: pod_id=%s, pod_face=%s, location=%s",
                    podInfo.getPodId(),
                    podInfo.getPodFace(),
                    podInfo.getLocation()
                );
                
                kafkaProducerService.sendMessageWithCallback("pod-updates", podInfo.getUuidNo(), message, true);
            }
            
            log.debug("POD 정보 Kafka 메시지 {}개 발행 완료", podDataList.size());
            
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
            LocalDateTime latest = podDataService.getLatestTimestamp();
            if (latest == null) {
                // 첫 실행 시에는 1년 전으로 설정하여 모든 데이터를 처리하도록 함
                log.info("POD 정보 ETL 엔진 첫 실행: 1년 전부터 모든 데이터 처리");
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
        log.info("POD 정보 ETL 엔진 캐시 초기화 완료 (REDIS)");
    }
    
    /**
     * 강제 캐시 리셋 (테스트용)
     */
    public void resetCache() {
        redisCacheService.clearNamespace(CACHE_NS);
        lastExecutionTime.set(0);
        log.info("POD 정보 ETL 엔진 캐시 강제 리셋 완료 (REDIS)");
    }
    
    // ETLEngine 추상 메서드 구현
    
    @Override
    protected List<PodInfo> extractData() throws ETLEngineException {
        try {
            long pgMs = postgreSQLDataService.getPodLastProcessedTime();
            java.time.LocalDateTime pgTime = java.time.Instant.ofEpochMilli(pgMs)
                .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();

            java.time.LocalDateTime wcsLatest;
            try {
                wcsLatest = podDataService.getLatestTimestamp();
            } catch (Exception ignore) {
                wcsLatest = pgTime;
            }
            java.time.LocalDateTime watermark = (wcsLatest != null && wcsLatest.isBefore(pgTime))
                ? wcsLatest.minusSeconds(1)
                : pgTime;

            return podDataService.getPodDataAfterTimestamp(watermark);
        } catch (Exception e) {
            throw new ETLEngineException("Error during data extraction", e);
        }
    }
    
    @Override
    protected List<PodInfo> transformAndLoad(List<PodInfo> data) throws ETLEngineException {
        try {
            // 데이터 변환
            List<PodInfo> transformedData = transformData(data);
            
            // 중복 데이터 필터링
            List<PodInfo> filteredData = filterDuplicateData(transformedData);
            
            // PostgreSQL에 저장
            podDataService.savePodDataBatch(filteredData);
            
            // Kafka로 전송
            publishToKafka(filteredData);
            
            return filteredData;
        } catch (Exception e) {
            log.error("Error during transform and load: {}", e.getMessage(), e);
            throw new ETLEngineException("Error during transform and load", e);
        }
    }

    
    
    @Override
    public boolean isConnected() {
        return podDataService.isConnected();
    }
    
    @Override
    protected String getDataKey(PodInfo data) {
        return data.getUuidNo();
    }
    
    @Override
    protected boolean isSameData(PodInfo data1, PodInfo data2) {
        PodInfo a = data1, b = data2;
        if (a == b) return true;
        if (a == null || b == null) return false;
        // 설비 식별(변하지 않는 키)
        boolean sameIdentity =
            java.util.Objects.equals(a.getUuidNo(), b.getUuidNo()) &&
            java.util.Objects.equals(a.getPodId(), b.getPodId());
        if (!sameIdentity) return false;

        // 변화 감지 대상 필드만 비교: location
        return java.util.Objects.equals(a.getPodFace(), b.getPodFace())
            && java.util.Objects.equals(a.getLocation(), b.getLocation());
            // && java.util.Objects.equals(a.getReportTime(), b.getReportTime());
        // report_time은 비교하지 않음
    }
} 