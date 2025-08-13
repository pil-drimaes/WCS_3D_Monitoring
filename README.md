# AGV ETL 모니터링 시스템

CDC 기능 없이 WCS DB에서 AGV 데이터를 실시간으로 모니터링하는 시스템입니다. 하이브리드 풀링 방식과 Kafka 메시지 큐를 통한 확장 가능한 아키텍처를 제공합니다.

## 🏗️ 아키텍처

### 전체 시스템 구조

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   WCS Database  │    │   ETL Engine    │    │   Kafka Queue   │
│   (SQL Server)  │───▶│  (Spring Boot)  │───▶│   (Confluent)   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                │                        │
                                ▼                        ▼
                       ┌─────────────────┐    ┌─────────────────┐
                       │  WebSocket      │    │  PostgreSQL     │
                       │  (Real-time)    │    │  (Event Store)  │
                       └─────────────────┘    └─────────────────┘
```

### 모듈식 구조

```
src/main/java/com/example/cdcqueue/
├── cdc/                    # CDC 관련 (레거시)
│   ├── controller/
│   │   └── ETLController.java
│   └── service/
│       ├── CdcPullingTask.java
│       ├── DatabaseService.java
│       └── WcsDatabaseService.java
├── etl/                    # ETL 엔진 (새로운 아키텍처)
│   ├── engine/
│   │   ├── DataPullingEngine.java      # 풀링 엔진 인터페이스
│   │   ├── HybridPullingEngine.java    # 하이브리드 풀링 구현
│   │   ├── PullingEngineConfig.java    # 풀링 엔진 설정
│   │   └── DatabaseConfig.java         # 데이터베이스 설정 (Spring Bean)
│   ├── service/
│   │   ├── AgvDataService.java         # AGV 데이터 서비스
│   │   ├── AgvDataTask.java            # AGV 데이터 태스크
│   │   ├── InventoryDataService.java   # 재고 데이터 서비스
│   │   ├── InventoryDataTask.java      # 재고 데이터 태스크
│   │   ├── PodDataService.java         # Pod 데이터 서비스
│   │   ├── PodDataTask.java            # Pod 데이터 태스크
│   │   ├── KafkaProducerService.java   # Kafka 프로듀서 서비스
│   │   ├── PostgreSQLDataService.java  # PostgreSQL 데이터 서비스
│   │   └── WebSocketPushTask.java      # WebSocket 푸시 태스크
│   ├── controller/
│   │   ├── AgvDataController.java      # AGV 데이터 컨트롤러
│   │   ├── InventoryDataController.java # 재고 데이터 컨트롤러
│   │   ├── PodDataController.java      # Pod 데이터 컨트롤러
│   │   └── TestController.java         # 테스트 컨트롤러
│   ├── AgvDataETLEngine.java           # AGV 데이터 ETL 엔진
│   ├── InventoryDataETLEngine.java     # 재고 데이터 ETL 엔진
│   ├── PodDataETLEngine.java           # Pod 데이터 ETL 엔진
│   ├── DataETLEngine.java              # ETL 엔진 인터페이스
│   ├── ETLConfig.java                  # ETL 설정
│   ├── ETLStatistics.java              # ETL 통계
│   └── ETLEngineException.java         # ETL 예외
├── common/               # 공통 모듈
│   ├── config/
│   │   ├── DatabaseConfig.java         # 데이터베이스 설정 (Spring Bean)
│   │   ├── ETLProperties.java          # ETL Properties
│   │   ├── JacksonConfig.java          # Jackson 설정
│   │   ├── KafkaConfig.java            # Kafka 설정
│   │   ├── PostgreSQLConfig.java       # PostgreSQL 설정
│   │   └── WebSocketConfig.java        # WebSocket 설정
│   ├── model/
│   │   ├── AgvApiResponse.java         # AGV API 응답
│   │   ├── AgvData.java                # AGV 데이터 모델
│   │   ├── CdcEvent.java               # CDC 이벤트
│   │   ├── InventoryInfo.java          # 재고 정보
│   │   ├── PodApiResponse.java         # Pod API 응답
│   │   └── PodInfo.java                # Pod 정보
│   └── queue/
│       └── EventQueue.java             # 이벤트 큐
└── parser/               # 데이터 파서
    ├── DataParser.java                 # 파서 인터페이스
    ├── SqlDataParser.java              # SQL 데이터 파서
    ├── ParserConfig.java               # 파서 설정
    └── DataParseException.java         # 파서 예외
```

## 🚀 주요 기능

### 1. 하이브리드 풀링 방식
- **조건부 쿼리**: 마지막 체크 시간 이후의 변경된 데이터만 조회
- **전체 동기화**: 주기적으로(1시간마다) 전체 데이터와 비교하여 정합성 보장
- **캐시 관리**: 중복 처리 방지를 위한 메모리 캐시
- **시간 기반 변경 감지**: `reportTime` 필드를 기준으로 데이터 변경 감지

### 2. 다중 데이터 타입 지원
- **AGV 데이터**: 로봇 위치, 상태, 배터리, 속도, 작업 정보
- **재고 데이터**: 재고 위치, 수량, 상태 정보
- **Pod 데이터**: Pod 위치, 상태, 작업 정보

### 3. Kafka 메시지 큐 통합
- **토픽 기반 메시징**: AGV 데이터, ETL 상태, 에러 이벤트 분리
- **배치 처리**: 대량 데이터의 효율적인 처리
- **메시지 이력 관리**: PostgreSQL에 전송 이력 저장
- **에러 처리**: 실패한 메시지의 재시도 및 로깅

### 4. 실시간 모니터링
- **WebSocket**: 실시간 이벤트 전송
- **다중 대시보드**: AGV, 재고, Pod별 전용 대시보드
- **API**: RESTful API를 통한 상태 조회 및 제어

## 📋 설정

### 1. 데이터베이스 설정

`application.properties`에서 다중 데이터베이스 설정:

```properties
# 로컬 DB 연결 설정 (이벤트 저장용)
spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=cdc_test;encrypt=true;trustServerCertificate=true
spring.datasource.username=sa
spring.datasource.password=nice2025!
spring.datasource.driver-class-name=com.microsoft.sqlserver.jdbc.SQLServerDriver

# WCS DB 연결 설정 (ETL 풀링용)
spring.datasource.wcs.url=jdbc:sqlserver://localhost:1433;databaseName=cdc_test;encrypt=true;trustServerCertificate=true
spring.datasource.wcs.username=sa
spring.datasource.wcs.password=nice2025!
spring.datasource.wcs.driver-class-name=com.microsoft.sqlserver.jdbc.SQLServerDriver

# PostgreSQL DB 연결 설정 (실제 운영 DB 쓰기용)
spring.datasource.postgresql.url=jdbc:postgresql://localhost:5432/cdcqueue?sslmode=disable&connectTimeout=30&socketTimeout=30&reWriteBatchedInserts=true
spring.datasource.postgresql.username=cdcuser
spring.datasource.postgresql.password=cdcpassword
spring.datasource.postgresql.driver-class-name=org.postgresql.Driver
```

### 2. Kafka 설정

#### Kafka 서버 설정 (Docker Compose)
```yaml
# docker-compose.yml
services:
  zookeeper:
    image: confluentinc/cp-zookeeper:7.4.10
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    ports:
      - "2181:2181"

  kafka:
    image: confluentinc/cp-kafka:7.4.10
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
      - "9101:9101"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: 'zookeeper:2181'
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:29092,PLAINTEXT_HOST://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
      KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS: 0
      KAFKA_JMX_PORT: 9101
      KAFKA_JMX_HOSTNAME: localhost
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: 'true'
      KAFKA_DELETE_TOPIC_ENABLE: 'true'

  kafka-ui:
    image: provectuslabs/kafka-ui:latest
    ports:
      - "8080:8080"
    environment:
      KAFKA_CLUSTERS_0_NAME: local
      KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:29092
      KAFKA_CLUSTERS_0_ZOOKEEPER: zookeeper:2181
```

#### Kafka 클라이언트 설정
```properties
# application.properties
# Kafka 설정
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer
spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer
spring.kafka.consumer.properties.spring.json.trusted.packages=com.example.cdcqueue.*
spring.kafka.consumer.group-id=agv-etl-group

# Kafka 토픽 설정
kafka.topic.agv-data=agv-data-events
kafka.topic.etl-status=etl-status-events
kafka.topic.error-events=error-events
```

#### Kafka Producer 설정 (Java)
```java
// KafkaConfig.java
@Bean
public ProducerFactory<String, Object> producerFactory() {
    Map<String, Object> configProps = new HashMap<>();
    configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
    configProps.put(ProducerConfig.ACKS_CONFIG, "all");           // 모든 복제본 확인
    configProps.put(ProducerConfig.RETRIES_CONFIG, 3);           // 재시도 3회
    configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);    // 배치 크기 16KB
    configProps.put(ProducerConfig.LINGER_MS_CONFIG, 1);         // 배치 대기 시간 1ms
    configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432); // 버퍼 32MB
    
    return new DefaultKafkaProducerFactory<>(configProps);
}
```

### 3. ETL 설정

```properties
# ETL 설정
etl.pulling.interval=1000                    # 풀링 간격 (1초)
etl.pulling.batch-size=100                   # 배치 크기
etl.pulling.strategy=HYBRID                  # 하이브리드 전략
etl.validation.enabled=true                  # 데이터 검증 활성화
etl.transformation.enabled=true              # 데이터 변환 활성화
etl.error-handling.mode=CONTINUE             # 오류 시 계속 진행
etl.retry.count=3                           # 재시도 횟수
etl.retry.interval=5000                     # 재시도 간격 (5초)
```

## 🖥️ 사용 방법

### 1. 환경 준비

#### Docker Compose로 인프라 실행
```bash
# Kafka, PostgreSQL, Zookeeper 실행
docker-compose up -d

# 상태 확인
docker-compose ps
```

#### 데이터베이스 스키마 설정
```bash
# PostgreSQL 스키마 적용
docker exec -i postgres-cdcqueue psql -U cdcuser -d cdcqueue < postgresql_schema.sql

# SQL Server 테이블 생성 (필요시)
sqlcmd -S localhost -U sa -P nice2025! -i test_data_insert.sql
```

### 2. 애플리케이션 실행

```bash
# Gradle로 실행
./gradlew bootRun

# 또는 JAR 파일로 실행
java -jar build/libs/cdcQueue-0.0.1-SNAPSHOT.jar
```

### 3. 대시보드 접속

- **메인 대시보드**: http://localhost:8081/
- **AGV 대시보드**: http://localhost:8081/agv-dashboard.html
- **재고 대시보드**: http://localhost:8081/inventory-dashboard.html
- **Pod 대시보드**: http://localhost:8081/pod-dashboard.html
- **실시간 모니터**: http://localhost:8081/realtime-monitor.html
- **Kafka UI**: http://localhost:8080/ (Kafka 관리 도구)

### 4. API 엔드포인트

#### ETL 상태 확인
```bash
GET /api/etl/status
```

#### ETL 통계 조회
```bash
GET /api/etl/statistics
```

#### AGV 데이터 조회
```bash
GET /api/agv/latest          # 최신 AGV 데이터
GET /api/agv/all            # 전체 AGV 데이터
GET /api/agv/robot/{robotNo} # 특정 로봇 데이터
```

#### 재고 데이터 조회
```bash
GET /api/inventory/latest    # 최신 재고 데이터
GET /api/inventory/all      # 전체 재고 데이터
```

#### Pod 데이터 조회
```bash
GET /api/pod/latest         # 최신 Pod 데이터
GET /api/pod/all           # 전체 Pod 데이터
```

#### 수동 ETL 실행
```bash
POST /api/etl/execute       # 전체 ETL 실행
POST /api/etl/agv/execute   # AGV ETL만 실행
POST /api/etl/inventory/execute # 재고 ETL만 실행
POST /api/etl/pod/execute   # Pod ETL만 실행
```

## 📊 모니터링

### 1. Kafka 토픽 모니터링

#### 토픽 생성 확인
```bash
# Kafka 컨테이너에 접속
docker exec -it kafka bash

# 토픽 목록 확인
kafka-topics --list --bootstrap-server localhost:9092

# 토픽 상세 정보 확인
kafka-topics --describe --topic agv-data-events --bootstrap-server localhost:9092
```

#### 메시지 모니터링
```bash
# 실시간 메시지 확인
kafka-console-consumer --topic agv-data-events --from-beginning --bootstrap-server localhost:9092

# 특정 토픽의 메시지 수 확인
kafka-run-class kafka.tools.GetOffsetShell --broker-list localhost:9092 --topic agv-data-events
```

### 2. ETL 엔진 상태
- 실시간 상태 모니터링
- 성공률, 처리 시간, 오류 횟수 통계
- WCS DB 연결 상태 확인
- 캐시 상태 및 메모리 사용량

### 3. 데이터 처리 통계
- 총 처리 레코드 수
- 성공/실패/건너뛴 레코드 수
- 평균 처리 시간
- 재시도 횟수
- Kafka 전송 성공률

### 4. 실시간 이벤트
- WebSocket을 통한 실시간 이벤트 수신
- AGV 위치 업데이트 실시간 표시
- 이벤트 히스토리 관리
- 에러 이벤트 알림

## 🔧 개발 가이드

### 1. 새로운 풀링 전략 추가

```java
@Component
public class CustomPullingEngine implements DataPullingEngine {
    @Override
    public void initialize(PullingEngineConfig config) {
        // 초기화 로직
    }
    
    @Override
    public List<AgvData> pullNewData() {
        // 커스텀 풀링 로직 구현
        return new ArrayList<>();
    }
    
    @Override
    public boolean isHealthy() {
        return true;
    }
    
    @Override
    public long getLastPullTime() {
        return System.currentTimeMillis();
    }
}
```

### 2. 새로운 ETL 엔진 추가

```java
@Component
public class CustomETLEngine implements DataETLEngine {
    @Override
    public List<AgvData> executeETL() {
        // 커스텀 ETL 로직 구현
        return new ArrayList<>();
    }
    
    @Override
    public ETLStatistics getStatistics() {
        return new ETLStatistics();
    }
    
    @Override
    public boolean isHealthy() {
        return true;
    }
}
```

### 3. 새로운 Kafka 토픽 추가

```java
// KafkaConfig.java에 토픽 추가
@Value("${kafka.topic.custom-data:custom-data-events}")
private String customDataTopic;

@Bean(name = "customDataTopic")
public String customDataTopic() {
    return customDataTopic;
}

// KafkaProducerService.java에 메서드 추가
public boolean sendCustomData(CustomData customData) {
    return sendMessage(customDataTopic, serializeToJson(customData));
}
```

## 🐛 문제 해결

### 1. Kafka 연결 문제

#### 연결 확인
```bash
# Kafka 서버 상태 확인
docker exec -it kafka kafka-broker-api-versions --bootstrap-server localhost:9092

# 토픽 생성 테스트
docker exec -it kafka kafka-topics --create \
    --topic test-topic \
    --bootstrap-server localhost:9092 \
    --partitions 1 \
    --replication-factor 1
```

#### 버전 호환성 확인
- Spring Boot 3.5.3 + Spring Kafka 3.1.1 + Kafka 3.6.x 조합 권장
- `kafka-version-check.md` 파일 참조

### 2. WCS DB 연결 실패
- 데이터베이스 서버 상태 확인
- 연결 문자열 및 인증 정보 확인
- 방화벽 설정 확인
- SSL 인증서 설정 확인

### 3. ETL 엔진 오류
- 로그 확인: `logging.level.com.example.cdcqueue.etl=DEBUG`
- 설정 파일 검증
- 데이터베이스 테이블 구조 확인
- 캐시 리셋: `/api/etl/reset-cache`

### 4. WebSocket 연결 실패
- 브라우저 콘솔 확인
- 서버 로그 확인
- 네트워크 연결 상태 확인
- CORS 설정 확인

## 📝 변경 이력

### v2.1 (현재)
- Kafka 메시지 큐 통합
- 다중 데이터 타입 지원 (AGV, 재고, Pod)
- PostgreSQL 이벤트 스토어 추가
- Kafka UI 대시보드 추가
- 메시지 이력 관리 기능

### v2.0
- 새로운 모듈식 ETL 아키텍처 구현
- 하이브리드 풀링 방식 도입
- 실시간 모니터링 대시보드 추가
- 설정 기반 동작 제어

### v1.0 (이전)
- CDC 기반 데이터 감지
- 기본 WebSocket 실시간 전송
- 단순한 데이터 처리 구조

## 📄 라이선스

이 프로젝트는 MIT 라이선스 하에 배포됩니다. 