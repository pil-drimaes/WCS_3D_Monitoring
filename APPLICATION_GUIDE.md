# CDC Queue 애플리케이션 동작 가이드

## 📖 **목차**
1. [애플리케이션 시작 흐름](#1-애플리케이션-시작-흐름)
2. [ETL 엔진 구조](#2-etl-엔진-구조)
3. [데이터 처리 순서](#3-데이터-처리-순서)
4. [Pulling 엔진 동작 방식](#4-pulling-엔진-동작-방식)
5. [PostgreSQL Insert 과정](#5-postgresql-insert-과정)
6. [Kafka 메시지 전송](#6-kafka-메시지-전송)
7. [코드별 역할과 책임](#7-코드별-역할과-책임)
8. [실제 실행 로그 분석](#8-실제-실행-로그-분석)

---

## 1. 애플리케이션 시작 흐름

### 1.1 메인 클래스: `CdcQueueApplication.java`
```java
@SpringBootApplication
@EnableScheduling  // 스케줄링 활성화
public class CdcQueueApplication {
    public static void main(String[] args) {
        SpringApplication.run(CdcQueueApplication.class, args);
    }
}
```

**시작 순서:**
1. Spring Boot 애플리케이션 컨텍스트 초기화
2. `@EnableScheduling`으로 스케줄된 태스크 활성화
3. 모든 `@Component`, `@Service`, `@Controller` 빈 생성
4. `@PostConstruct` 메서드 실행
5. 스케줄된 태스크 시작

### 1.2 설정 파일 로드: `application.properties`
```properties
# 데이터베이스 연결 설정
spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=cdc_test
spring.datasource.postgresql.url=jdbc:postgresql://localhost:5432/cdcqueue
spring.datasource.postgresql.username=cdcuser
spring.datasource.postgresql.password=cdcpassword

# ETL 설정
etl.pulling.interval=1000        # 1초마다 풀링
etl.pulling.batch-size=100      # 배치 크기
etl.pulling.strategy=HYBRID     # 하이브리드 전략
```

---

## 2. ETL 엔진 구조

### 2.1 ETL 엔진 계층 구조
```
CdcQueueApplication
├── AgvDataETLEngine          # AGV 데이터 ETL
├── InventoryDataETLEngine    # 재고 데이터 ETL  
├── PodDataETLEngine          # POD 데이터 ETL
└── Scheduled Tasks           # 스케줄된 실행 태스크
```

### 2.2 각 엔진의 독립성
**중요**: 각 ETL 엔진은 **독립적으로** 실행됩니다!
- `AgvDataETLEngine`: robot_info 테이블만 처리
- `InventoryDataETLEngine`: inventory_info 테이블만 처리
- `PodDataETLEngine`: pod_info 테이블만 처리

**동시 실행**: 3개 엔진이 병렬로 실행되어 각각 다른 테이블을 처리

---

## 3. 데이터 처리 순서

### 3.1 전체 ETL 프로세스 흐름
```
MSSQL (Source) → Pulling Engine → ETL Engine → PostgreSQL (Target) → Kafka
     ↓              ↓              ↓              ↓              ↓
  robot_info   AgvHybrid      AgvDataETL    robot_info    ETL Status
inventory_info InventoryHybrid InventoryETL inventory_info   Message
   pod_info    PodHybrid      PodDataETL      pod_info
```

### 3.2 각 엔진별 실행 주기
```java
// AgvDataTask.java
@Scheduled(fixedRate = 1000)  // 1초마다 실행
public void processAgvData() {
    agvDataETLEngine.processData();
}

// InventoryDataTask.java  
@Scheduled(fixedRate = 1000)  // 1초마다 실행
public void processInventoryData() {
    inventoryDataETLEngine.processData();
}

// PodDataTask.java
@Scheduled(fixedRate = 1000)  // 1초마다 실행
public void processPodData() {
    podDataETLEngine.processData();
}
```

**실행 순서**: 
1. 애플리케이션 시작과 동시에 3개 태스크 모두 활성화
2. 각 태스크는 독립적인 스레드에서 1초마다 실행
3. **동시 실행**: 3개 엔진이 거의 동시에 실행됨

---

## 4. Pulling 엔진 동작 방식

### 4.1 Hybrid Pulling 전략
```java
// HybridPullingEngine.java
public abstract class HybridPullingEngine<T> {
    
    @Override
    public List<T> pullData() {
        if (shouldPerformFullSync()) {
            // 전체 동기화: 캐시와 비교하여 변경된 데이터만 반환
            return performFullSync();
        } else {
            // 증분 풀링: 마지막 체크 시간 이후 변경된 데이터만 반환
            return performIncrementalPull();
        }
    }
    
    private boolean shouldPerformFullSync() {
        // 10분마다 전체 동기화 수행
        return Duration.between(lastFullSync, LocalDateTime.now()).toMinutes() >= 10;
    }
}
```

### 4.2 풀링 주기 설정
```java
// AgvHybridPullingEngine.java
@Override
protected PullingEngineConfig getDefaultConfig() {
    PullingEngineConfig config = new PullingEngineConfig();
    config.setPullInterval(Duration.ofMillis(100));  // 0.1초마다 풀링
    config.setBatchSize(100);
    return config;
}
```

**실제 동작**:
- **Pulling Engine**: 0.1초마다 MSSQL에서 데이터 변경 감지
- **ETL Engine**: 1초마다 Pulling Engine의 결과를 처리
- **결과**: 데이터 변경은 0.1초 내에 감지되지만, 실제 처리는 1초마다

---

## 5. PostgreSQL Insert 과정

### 5.1 데이터 Insert 흐름
```java
// AgvDataETLEngine.java
public void processData() {
    try {
        // 1. MSSQL에서 데이터 추출
        List<AgvData> dataList = pullingEngine.pullData();
        
        if (dataList.isEmpty()) {
            return; // 처리할 데이터가 없음
        }
        
        // 2. 데이터 변환 및 검증
        List<AgvData> validData = validateAndTransformData(dataList);
        
        // 3. PostgreSQL에 Insert
        int insertedCount = postgreSQLDataService.insertAgvData(validData);
        
        // 4. Kafka 메시지 전송
        sendETLStatusMessage(dataList.size(), insertedCount);
        
        // 5. 캐시 업데이트
        updateProcessedDataCache(validData);
        
    } catch (Exception e) {
        handleError(e);
    }
}
```

### 5.2 Insert SQL 실행
```java
// PostgreSQLDataService.java
public int insertAgvData(List<AgvData> dataList) {
    String sql = """
        INSERT INTO robot_info (uuid_no, robot_id, pos_x, pos_y, battery, report_time, created_at)
        VALUES (?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT (uuid_no) 
        DO UPDATE SET 
            robot_id = EXCLUDED.robot_id,
            pos_x = EXCLUDED.pos_x,
            pos_y = EXCLUDED.pos_y,
            battery = EXCLUDED.battery,
            report_time = EXCLUDED.report_time,
            updated_at = CURRENT_TIMESTAMP
        """;
    
    return jdbcTemplate.batchUpdate(sql, dataList, batchSize, 
        (ps, data) -> {
            ps.setString(1, data.getUuidNo());
            ps.setString(2, data.getRobotId());
            ps.setDouble(3, data.getPosX());
            ps.setDouble(4, data.getPosY());
            ps.setInt(5, data.getBattery());
            ps.setLong(6, data.getReportTime());
            ps.setTimestamp(7, Timestamp.from(Instant.now()));
        });
}
```

**Insert 특징**:
- **UPSERT 방식**: `ON CONFLICT DO UPDATE`로 중복 데이터 처리
- **배치 처리**: 여러 데이터를 한 번에 Insert
- **트랜잭션**: 각 배치별로 트랜잭션 처리

---

## 6. Kafka 메시지 전송

### 6.1 ETL 상태 메시지 구조
```java
// KafkaProducerService.java
public static class ETLStatusMessage {
    private String batchId;           // 배치 ID
    private int totalCount;           // 총 데이터 수
    private int successCount;         // 성공한 Insert 수
    private int failureCount;         // 실패한 Insert 수
    private long processingTime;      // 처리 시간 (ms)
    private String status;            // 상태 (SUCCESS, FAILURE, PARTIAL)
    private LocalDateTime timestamp;  // 타임스탬프
}
```

### 6.2 메시지 전송 과정
```java
// AgvDataETLEngine.java
private void sendETLStatusMessage(int totalCount, int insertedCount) {
    ETLStatusMessage message = new ETLStatusMessage(
        UUID.randomUUID().toString(),
        totalCount,
        insertedCount,
        totalCount - insertedCount,
        System.currentTimeMillis() - startTime,
        insertedCount == totalCount ? "SUCCESS" : "PARTIAL"
    );
    
    kafkaProducerService.sendMessage("etl-status-events", message);
}
```

### 6.3 Kafka 토픽 구조
```properties
# application.properties
kafka.topic.etl-status=etl-status-events
kafka.topic.agv-events=agv-data-events
kafka.topic.inventory-events=inventory-data-events
kafka.topic.pod-events=pod-data-events
```

**메시지 전송 시점**:
- ETL 엔진이 데이터 처리 완료 후
- 각 엔진별로 독립적으로 전송
- 실패 시에도 상태 메시지 전송

---

## 7. 코드별 역할과 책임

### 7.1 핵심 클래스별 책임

#### **ETL 엔진 (Orchestrator)**
- `AgvDataETLEngine`: AGV 데이터 ETL 프로세스 조율
- `InventoryDataETLEngine`: 재고 데이터 ETL 프로세스 조율
- `PodDataETLEngine`: POD 데이터 ETL 프로세스 조율

#### **Pulling 엔진 (Data Extractor)**
- `AgvHybridPullingEngine`: MSSQL에서 AGV 데이터 추출
- `InventoryHybridPullingEngine`: MSSQL에서 재고 데이터 추출
- `PodHybridPullingEngine`: MSSQL에서 POD 데이터 추출

#### **데이터 서비스 (Data Handler)**
- `AgvDataService`: MSSQL 데이터 접근 및 쿼리
- `PostgreSQLDataService`: PostgreSQL 데이터 Insert/Update
- `KafkaProducerService`: Kafka 메시지 전송
- `KafkaConsumerService`: Kafka 메시지 수신

#### **스케줄 태스크 (Scheduler)**
- `AgvDataTask`: AGV ETL 주기적 실행
- `InventoryDataTask`: 재고 ETL 주기적 실행
- `PodDataTask`: POD ETL 주기적 실행

### 7.2 설정 클래스
- `DatabaseConfig`: MSSQL 데이터소스 설정
- `PostgreSQLConfig`: PostgreSQL 데이터소스 설정
- `KafkaConfig`: Kafka 프로듀서/컨슈머 설정

---

## 8. 실제 실행 로그 분석

### 8.1 애플리케이션 시작 로그
```
2025-01-13 15:49:00.000 INFO  --- [main] c.e.c.CdcQueueApplication : Starting CdcQueueApplication
2025-01-13 15:49:01.000 INFO  --- [main] c.e.c.CdcQueueApplication : Started CdcQueueApplication
2025-01-13 15:49:01.100 INFO  --- [task-1] c.e.c.e.s.AgvDataTask : AGV 데이터 처리 시작
2025-01-13 15:49:01.100 INFO  --- [task-2] c.e.c.e.s.InventoryDataTask : 재고 데이터 처리 시작
2025-01-13 15:49:01.100 INFO  --- [task-3] c.e.c.e.s.PodDataTask : POD 데이터 처리 시작
```

### 8.2 ETL 실행 로그
```
2025-01-13 15:49:01.200 INFO  --- [task-1] c.e.c.e.AgvDataETLEngine : AGV 데이터 풀링 시작
2025-01-13 15:49:01.250 INFO  --- [task-1] c.e.c.e.AgvDataETLEngine : MSSQL에서 20개 AGV 데이터 추출
2025-01-13 15:49:01.300 INFO  --- [task-1] c.e.c.e.AgvDataETLEngine : PostgreSQL에 20개 데이터 Insert 성공
2025-01-13 15:49:01.350 INFO  --- [task-1] c.e.c.e.AgvDataETLEngine : Kafka 메시지 전송 완료
```

### 8.3 Pulling 엔진 로그
```
2025-01-13 15:49:01.100 INFO  --- [pulling-1] c.e.c.e.e.AgvHybridPullingEngine : 증분 풀링 실행
2025-01-13 15:49:01.200 INFO  --- [pulling-1] c.e.c.e.e.AgvHybridPullingEngine : 증분 풀링 실행
2025-01-13 15:49:01.300 INFO  --- [pulling-1] c.e.c.e.e.AgvHybridPullingEngine : 증분 풀링 실행
2025-01-13 15:49:11.100 INFO  --- [pulling-1] c.e.c.e.e.AgvHybridPullingEngine : 전체 동기화 실행 (10분 주기)
```

---

## 🔍 **핵심 포인트 요약**

### **동시성 처리**
- 3개 ETL 엔진이 **독립적으로 병렬 실행**
- 각 엔진은 서로 다른 테이블 처리
- Pulling과 ETL이 **다른 주기**로 실행

### **데이터 흐름**
- **MSSQL → Pulling Engine → ETL Engine → PostgreSQL → Kafka**
- Pulling: 0.1초마다 변경 감지
- ETL: 1초마다 데이터 처리

### **성능 최적화**
- 하이브리드 풀링으로 효율적인 데이터 감지
- 배치 처리로 대량 데이터 Insert
- 캐시를 통한 중복 데이터 필터링

### **모니터링**
- Kafka를 통한 실시간 ETL 상태 모니터링
- 상세한 로그로 각 단계별 성능 추적
- 에러 발생 시 즉시 감지 및 처리

---

## 📝 **테스트 시나리오**

### **시나리오 1: 초기 데이터 로드**
1. MSSQL에 20개 robot_info, 10개 inventory_info, 10개 pod_info 삽입
2. 애플리케이션 시작
3. 각 ETL 엔진이 독립적으로 데이터 처리 확인
4. PostgreSQL에 모든 데이터 Insert 확인

### **시나리오 2: 실시간 데이터 변경**
1. MSSQL에서 기존 데이터 업데이트 (pos_x, pos_y, battery)
2. Pulling 엔진이 0.1초 내에 변경 감지 확인
3. ETL 엔진이 1초 내에 PostgreSQL 업데이트 확인
4. Kafka 메시지로 상태 전송 확인

### **시나리오 3: 동시 처리 성능**
1. 3개 테이블에 동시에 데이터 변경
2. 각 ETL 엔진이 병렬로 처리하는지 확인
3. 전체 처리 시간 측정
4. 메모리 사용량 모니터링

---

*이 가이드는 CDC Queue 애플리케이션의 전체 동작 방식을 이해하고 디버깅하는 데 도움이 됩니다.* 