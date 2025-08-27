package com.example.WCS_DataStream.etl.engine;

import com.example.WCS_DataStream.etl.ETLEngineException;
import com.example.WCS_DataStream.etl.ETLStatistics;
import com.example.WCS_DataStream.etl.model.AgvData;
import com.example.WCS_DataStream.etl.service.AgvDataService;

import com.example.WCS_DataStream.etl.service.KafkaProducerService;
import com.example.WCS_DataStream.etl.service.PostgreSQLDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AGV 데이터 ETL 엔진
 * 
 * AGV 데이터의 Extract(추출), Transform(변환), Load(적재) 과정을 관리하는 엔진
 * 
 * 동작 방식:
 * 1. Extract: 풀링 엔진을 통해 새로운 데이터 추출
 * 2. Transform: 파서를 통해 데이터 변환 및 검증
 * 3. Load: 처리된 데이터를 Kafka와 PostgreSQL에 적재
 * 
 * @author AGV Monitoring System
 * @version 1.0
 */
@Component
public class AgvDataETLEngine extends ETLEngine<AgvData> {
    
    private static final Logger log = LoggerFactory.getLogger(AgvDataETLEngine.class);
    

    private final AgvDataService agvDataService;
    private final KafkaProducerService kafkaProducerService;
    private final PostgreSQLDataService postgreSQLDataService;
    
    // 마지막 실행 시간 추적
    private final AtomicLong lastExecutionTime = new AtomicLong(0);

    // 중복 데이터 필터링을 위한 캐시 (uuid_no -> report_time)
    private final ConcurrentHashMap<String, Long> processedDataCache = new ConcurrentHashMap<>();

    public AgvDataETLEngine(AgvDataService agvDataService,
                           KafkaProducerService kafkaProducerService,
                           PostgreSQLDataService postgreSQLDataService) {
        this.agvDataService = agvDataService;
        this.kafkaProducerService = kafkaProducerService;
        this.postgreSQLDataService = postgreSQLDataService;
    }

    @Override
    protected boolean checkTableExists() {
        return postgreSQLDataService.isAgvTableExists();
    }

    /**
     * ETL 프로세스 실행 (DataETLEngine 인터페이스 구현)
     * 
     * @return 처리된 AGV 데이터 리스트
     * @throws ETLEngineException ETL 처리 중 오류 발생 시
     */

     
     @Override
     public List<AgvData> executeETL() throws ETLEngineException {
         return super.executeETL(); // 
     }
     
     // 초기 1회 전체 처리
     public List<AgvData> executeFullLoad() throws ETLEngineException {
         try {
             if (!checkTableExists()) {
                 throw new ETLEngineException("PostgreSQL robot_info 테이블이 존재하지 않음");
             }
             List<AgvData> allData = agvDataService.getAllAgvData();
             List<AgvData> filtered = filterDuplicateData(allData);
             return processETLInternal(filtered);
         } catch (Exception e) {
             throw new ETLEngineException("AGV 전체 로드 중 오류", e);
         }
     }

    /**
     * ETL 프로세스 실행 (내부 구현 - 시간 기반)
     * 
     * @param lastProcessedTime 마지막 처리 시간
     * @return 처리된 AGV 데이터 리스트
     */
    private List<AgvData> processETLInternal(LocalDateTime lastProcessedTime) {
        log.debug("AGV 데이터 ETL 프로세스 시작 - Extract 단계");

        try {
            // Extract: WCS DB에서 데이터 추출
            List<AgvData> allData = agvDataService.getAgvDataAfterTimestamp(lastProcessedTime);
            log.debug("추출된 AGV 데이터 {}개", allData.size());
            
            if (allData.isEmpty()) {
                log.debug("처리할 새로운 AGV 데이터가 없습니다");
                return new ArrayList<>();
            }
            
            // 중복 데이터 필터링
            List<AgvData> filteredData = filterDuplicateData(allData);
            log.debug("중복 필터링 후 {}개 데이터", filteredData.size());
            
            if (filteredData.isEmpty()) {
                log.debug("중복 필터링 후 처리할 데이터가 없습니다");
                return new ArrayList<>();
            }
            
            // Transform: 데이터 변환 (필요시)
            List<AgvData> transformedData = transformData(filteredData);
            
            // Load: PostgreSQL에 데이터 저장
            int savedCount = agvDataService.saveAgvDataBatch(transformedData);
            log.info("AGV 데이터 ETL 완료: {}개 중 {}개 저장", transformedData.size(), savedCount);
            
            // Kafka 메시지 발행
            publishToKafka(transformedData);
            
            // 마지막 실행 시간 업데이트
            lastExecutionTime.set(System.currentTimeMillis());
            
            // 통계 업데이트
            updateStatistics(allData.size(), transformedData.size(), System.currentTimeMillis());
            
            return transformedData;
            
        } catch (Exception e) {
            log.error("AGV 데이터 ETL 처리 중 오류 발생", e);
            throw new RuntimeException("AGV 데이터 ETL 처리 중 오류 발생", e);
        }
    }
    
    /**
    * ETL 프로세스 실행 (내부 구현 - 데이터 기반)
    * 
    * @param filteredData 이미 필터링된 데이터
    * @return 처리된 AGV 데이터 리스트
    */
    private List<AgvData> processETLInternal(List<AgvData> filteredData) {
        log.debug("AGV 데이터 ETL 프로세스 시작 - Transform & Load 단계");
        
        try {
            if (filteredData.isEmpty()) {
                log.debug("처리할 데이터가 없습니다");
                return new ArrayList<>();
            }
            
            // Transform: 데이터 변환 (필요시)
            List<AgvData> transformedData = transformData(filteredData);
            
            // Load: PostgreSQL에 데이터 저장
            int savedCount = agvDataService.saveAgvDataBatch(transformedData);
            log.info("AGV 데이터 ETL 완료: {}개 중 {}개 저장", transformedData.size(), savedCount);
            
            // Kafka 메시지 발행
            publishToKafka(transformedData);
            
            // 마지막 실행 시간 업데이트
            lastExecutionTime.set(System.currentTimeMillis());
            
            // 통계 업데이트
            updateStatistics(filteredData.size(), transformedData.size(), System.currentTimeMillis());
            
            return transformedData;
            
        } catch (Exception e) {
            log.error("AGV 데이터 ETL 처리 중 오류 발생", e);
            throw new RuntimeException("AGV 데이터 ETL 처리 중 오류 발생", e);
        }
    }

    /**
     * ETL 프로세스 실행 (기존 메서드 - 호환성 유지)
     * 
     * @param lastProcessedTime 마지막 처리 시간
     * @return 처리된 AGV 데이터 리스트
     */
    public int processETL(LocalDateTime lastProcessedTime) {
        List<AgvData> result = processETLInternal(lastProcessedTime);
        return result.size();
    }

    /**
     * 중복 데이터 필터링 (PostgreSQL 기반 + 캐시 기반)
     * 
     * @param allData 모든 데이터
     * @return 중복이 제거된 데이터
     */
    private List<AgvData> filterDuplicateData(List<AgvData> allData) {
        return allData.stream()
            .filter(data -> {
                if (data.getUuidNo() == null || data.getReportTime() == null) {
                    return true; // UUID나 report_time이 없으면 처리
                }
                
                // 1. PostgreSQL에서 이미 존재하는지 확인
                try {
                    boolean existsInPostgres = postgreSQLDataService.isAgvDataExists(data.getUuidNo(), data.getReportTime());
                    if (existsInPostgres) {
                        log.debug("PostgreSQL에 이미 존재하는 AGV 데이터 제외: uuid={}, report_time={}", 
                                 data.getUuidNo(), data.getReportTime());
                    return false;
                    }
                } catch (Exception e) {
                    log.warn("PostgreSQL 중복 체크 실패, 캐시 기반으로 처리: uuid={}, error={}", 
                             data.getUuidNo(), e.getMessage());
                }

                // 2. 캐시 기반 중복 체크 (백업)
                String key = data.getUuidNo();
                Long cachedTime = processedDataCache.get(key);
                
                if (cachedTime == null) {
                    // 새로운 데이터인 경우
                    processedDataCache.put(key, data.getReportTime());
                    log.debug("새로운 AGV 데이터 감지: uuid={}, report_time={}", key, data.getReportTime());
                    return true;
                }
                
                if (data.getReportTime() > cachedTime) {
                    // report_time이 더 최신인 경우 (업데이트된 데이터)
                    processedDataCache.put(key, data.getReportTime());
                    log.debug("업데이트된 AGV 데이터 감지: uuid={}, old_time={}, new_time={}", 
                             key, cachedTime, data.getReportTime());
                    return true;
                }

            // report_time이 같은 경우 중복으로 처리
                log.debug("중복 AGV 데이터 제외: uuid={}, cached_time={}, data_time={}", 
                            key, cachedTime, data.getReportTime());
                return false;
            })
            .toList();
    }

    /**
     * 데이터 변환 (Transform)
     * 
     * @param agvDataList 원본 데이터 리스트
     * @return 변환된 데이터 리스트
     */
    private List<AgvData> transformData(List<AgvData> agvDataList) {
        // UUID가 없는 경우에만 생성 (원본 UUID 유지)
        agvDataList.forEach(data -> {
            if (data.getUuidNo() == null || data.getUuidNo().isEmpty()) {
                data.setUuidNo(UUID.randomUUID().toString());
            }
            
            // report_time이 없는 경우 현재 시간으로 설정
            if (data.getReportTime() == null) {
                data.setReportTime(System.currentTimeMillis());
            }
        });
        
        return agvDataList;
    }

    /**
     * Kafka로 메시지 발행
     * 
     * @param agvDataList 발행할 데이터 리스트
     */
    private void publishToKafka(List<AgvData> agvDataList) {
        try {
            for (AgvData agvData : agvDataList) {
                String message = String.format(
                    "AGV 데이터 업데이트: robot_no=%s, status=%d, battery=%s, speed=%s, pos_x=%s, pos_y=%s",
                    agvData.getRobotNo(), agvData.getStatus(), agvData.getBattery(), agvData.getSpeed(), 
                    agvData.getPosX(), agvData.getPosY()
                );
                
                kafkaProducerService.sendMessage("agv-updates", message);
            }
            
            log.debug("AGV 데이터 Kafka 메시지 {}개 발행 완료", agvDataList.size());
            
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
            LocalDateTime latest = agvDataService.getLatestTimestamp();
            if (latest == null) {
                log.info("AGV 데이터 ETL 엔진 첫 실행: 1년 전부터 모든 데이터 처리");
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
        log.info("AGV 데이터 ETL 엔진 캐시 초기화 완료");
    }
    
    /**
     * 강제 캐시 리셋 (테스트용)
     */
    public void resetCache() {
        processedDataCache.clear();
        lastExecutionTime.set(0);
        log.info("AGV 데이터 ETL 엔진 캐시 강제 리셋 완료");
    }
    


    // ETLEngine 추상 메서드 구현

    @Override
    protected List<AgvData> extractData() throws ETLEngineException {
        try {
            long wm = postgreSQLDataService.getRobotLastProcessedTime();
            LocalDateTime lastProcessedTime = java.time.Instant.ofEpochMilli(wm)
                .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
            return agvDataService.getAgvDataAfterTimestamp(lastProcessedTime);
        } catch (Exception e) {
            throw new ETLEngineException("Error during data extraction", e);
        }
    }

    @Override
    protected List<AgvData> transformAndLoad(List<AgvData> data) throws ETLEngineException {
        try {
            List<AgvData> transformed = transformData(data);
            List<AgvData> filtered = filterDuplicateData(transformed); 
            agvDataService.saveAgvDataBatch(filtered);
            publishToKafka(filtered);
            return filtered;
        } catch (Exception e) {
            throw new ETLEngineException("Error during transform and load", e);
        }
    }

    @Override
    public boolean isConnected() {
        return agvDataService.isConnected();
    }

    @Override
    protected String getDataKey(AgvData data) {
        return data.getUuidNo();
    }

    @Override
    protected boolean isSameData(AgvData data1, AgvData data2) {
        return data1.getUuidNo().equals(data2.getUuidNo()) &&
               data1.getReportTime().equals(data2.getReportTime());
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