# WCS DataStream - ETL 시스템

## 요약

WCS(Warehouse Control System) 원천 DB에서 데이터를 추출·필터링·적재하고, 변경 이벤트를 Kafka로 발행하며, Prometheus/Grafana로 관측 가능한 독립형 ETL 시스템입니다. 벤더/도메인별 파이프라인(예: Ant Robot/Pod, Mushiny AGV/Pod, Ant FLYPICK 등)을 개별 토글로 켜고 끌 수 있으며, 캐시 비교와 DB 중복 방지로 "변경된 데이터만" 처리합니다.

## 구조 한눈에 보기

```
WCS DB(SQL Server) ──► 스케줄러(Quartz/Scheduling) ──► ETL 엔진(도메인별)
         │                         │
         │                         ├── 캐시 비교(최신 레코드) + DB 중복검사
         │                         └── Kafka 이벤트 발행(벤더별 토픽)
         │
         ▼
PostgreSQL(Target, Append-only/이력)  +  Prometheus(메트릭) → Grafana(대시보드)
```

---

## 주요 변경사항(신규/수정/삭제)

### 신규
- 벤더/도메인 파이프라인 분리 및 토글:
  - `etl.antRobot.enabled`, `etl.antFlypick.enabled`, `etl.antPod.enabled`, `etl.mushinyAgv.enabled`, `etl.mushinyPod.enabled`
  - 도메인별 간격/초기지연 설정(`etl.<domain>.interval`, `etl.<domain>.initialDelay`)
- Kafka 이벤트 발행 서비스 추가: `KafkaEventPublisher` (벤더별 토픽으로 레코드 발행)
- Micrometer 메트릭 바인더: `EtlMetricsBinder`
  - `etl_last_execution_epoch_ms{domain=...}`
  - `etl_processed_records_total{domain=...}`
- 모니터링 스택 연동 강화: Prometheus 스크랩 설정 + Grafana 대시보드(JSON 제공)
- Docker Compose 인프라 세트: PostgreSQL 16.9, Redis 7.2, Kafka/ZK 7.4.10, Prometheus, Grafana, Exporters

### 수정
- 스케줄링 구성 고도화: Quartz 설정 추가(메모리 JobStore, 스레드풀)와 Spring Scheduling 병행
- 데이터베이스 분리 설정:
  - 원천 WCS DB(`etl.database.*` 또는 `spring.datasource.wcs.*`)와 타깃 PostgreSQL(`spring.datasource.postgresql.*`) 분리
- Kafka 설정 및 토픽 명세 정리: 벤더/도메인별 토픽 키(`kafka.topic.*`) 통일
- 실행 포트 조정: `server.port=8081` (Kafka-UI 등과 충돌 방지)

### 삭제
- 문서 정리: `ETL_SYSTEM_IMPROVEMENT.md`, `IMPROVED_STRUCTURE.md` 제거(본 README 및 APPLICATION_GUIDE로 통합)

## 전체 동작 순서(7단계)

1. 애플리케이션을 실행한다.
   - Spring Boot 기동과 함께 스케줄링/Quartz 설정이 활성화됩니다.
2. WCS_DB의 각 테이블을 읽어온다.
   - 애플리케이션 시작 직후 1회 Full Load 수행(도메인별 전체 조회).
3. 캐시메모리에 저장해놓는다.
   - `uuid_no -> 최신 레코드` 형태로 in-memory 캐시에 보관됩니다.
4. PostGreDB에서 테이블이 비어있으면 insert하고 중복데이터면 Insert하지 않는다.
   - DB 레벨에서 `uuid_no + report_time` 조합으로 중복을 1차 차단합니다.
5. 스케줄링/Quartz 설정을 통해 도메인/벤더별 간격으로 마지막 LatestTime 이후 데이터를 가져와 캐시와 비교한다.
   - PostgreSQL에 저장된 `MAX(report_time)`를 기준으로 WCS에서 `>= latest`만 증분 조회합니다.
6. 데이터 비교를 감지하면 PostGre DB에 각 데이터를 삽입한다.
   - 현재는 모든 필드를 비교하여 다르면 변경으로 간주합니다(`isSameData`).
   - 향후 X/Y 좌표나 상태만 비교하도록 손쉽게 커스터마이징할 수 있습니다.
7. 최신데이터만 캐시에 업데이트한다.
   - 새 레코드의 `report_time`이 캐시보다 클 때만 캐시를 갱신합니다.

## 시스템 아키텍처
  
```
WCS_DB (Source) → 스케줄러(Quartz/Scheduling) → ETL 엔진(Extract/Transform/Filter)
      ↑                       │                        │
      │                       ├── 캐시 비교 + DB 중복   ├── Kafka 발행(옵션)
      │                       └── 배치 Insert           │
      └────────────── PostgreSQL(Target) ◄──────────────┘
```

- 스케줄러: 도메인/벤더별 간격으로 각 ETL 엔진을 트리거합니다(Quartz + Scheduling 병행).
- 변경 감지: 
  - DB 중복 체크: `uuid_no + report_time` 존재 시 스킵(1차 방어)
  - 캐시 비교: 최신 레코드와 `isSameData`로 내용 비교(2차 방어)
- 저장/발행: 변경된 데이터만 PostgreSQL에 Insert, 필요 시 Kafka로 이벤트 발행

## 구성 요소와 역할

- 엔트리 포인트
  - `DataStreamApplication`: 애플리케이션 시작, 스케줄링 활성화
  - `SchedulerInitializer`: 애플리케이션 기동 시 각 스케줄러 상태 초기화
- 스케줄러(`etl/scheduler`)
  - 도메인/벤더별 스케줄러: `AntRobotScheduler`, `AntPodScheduler`, `AntFlypickScheduler`, `MushinyAgvScheduler`, `MushinyPodScheduler`
    - 공통 베이스: `BaseETLScheduler`
    - 동작: 시작 후 최초 1회 초기 처리(`processInitialData()`), 이후 간격(`etl.<domain>.interval`)마다 증분 처리(`processIncrementalData()`)
    - 캐시: 스케줄러 별 `processed` 집합으로 최근 처리 키 관리(과도 시 자동 정리)
- ETL 엔진(`etl/engine`)
  - `AntRobotEtlEngine`, `AntPodEtlEngine`, `AntFlypickEtlEngine`, `MushinyAgvEtlEngine`, `MushinyPodEtlEngine`
    - Extract: Redis 기반 `EtlOffsetStore`의 `(lastTs,lastUuid)`를 기준으로 WCS 증분 조회(WCS 리포지토리 `fetchIncremental(...)` 사용)
    - Transform/Load: 대상(System) 리포지토리 `upsert(...)` 수행, 일부 엔진은 `KafkaEventPublisher`로 이벤트 발행
    - Offset 관리: 처리 후 최대 타임스탬프/UUID를 산출하여 `EtlOffsetStore.set(job, offset)` 저장
    - 비교 로직: 엔진별 `isSameData(...)` 구현(기본은 UUID 동일성)
- 서비스/리포지토리(`etl/service`)
  - 원천(WCS) 조회: `WcsAntRobotRepository`, `WcsAntPodRepository`, `WcsMushinyAgvRepository`, ...
  - 대상(System) 적재: `SystemAgvRepository`, `SystemAntPodRepository`, `SystemMushinyAgvRepository`, `SystemMushinyPodRepository`
  - 공통: `PostgreSQLDataService`(연결 확인, 배치/단건 저장, 통계/이력), `RedisCacheService`, `EtlOffsetStore`
  - 이벤트: `KafkaEventPublisher`(토픽 키는 `kafka.topic.*`)
- 모델(`etl/model`)
  - 도메인 모델: `AgvData`, `InventoryInfo`, `PodInfo`
  - 벤더별 DTO: `...vendor.ant.*`, `...vendor.mushiny.*`

### 관측성(Observability)
- `EtlMetricsBinder`가 Micrometer 지표를 등록합니다.
  - `etl_last_execution_epoch_ms{domain}`: 최근 ETL 실행 시각(epoch ms)
  - `etl_processed_records_total{domain}`: 누적 처리(성공) 건수
- Actuator `/actuator/prometheus` 노출 → Prometheus 스크랩 → Grafana 시각화

## 변경 감지(비교) 로직

- 현재 기본값: “모든 필드” 동일성으로 판단
  - 각 엔진의 `isSameData(...)`가 Lombok `equals`에 의존하여 전체 필드를 비교합니다.
- 커스터마이징 예시
  - AGV의 X/Y 좌표 변화만 감지: `AgvDataETLEngine.isSameData`에서 `posX`, `posY`만 비교하도록 수정
  - 상태 변화만 감지: 해당 상태 필드만 비교하도록 수정

## 증분 처리 기준(LatestTime)

- PostgreSQL 대상 테이블의 `MAX(report_time)`를 조회하여 WCS에서 `>= latest` 조건으로 가져옵니다.
- 경계값 중복은 DB 중복 체크와 캐시 비교에서 스킵됩니다.

## 성능/중복 방지 전략

- 스케줄링: 도메인/벤더별 간격(예: Ant Robot 500ms, Ant/Mushiny Pod 1000ms 등)
- 배치 저장: 기본 100건 단위 배치 처리(연결 문자열에서 `reWriteBatchedInserts=true` 권장)
- 중복 방지: 
  - DB 1차 방어(`uuid_no + report_time`)
  - 캐시 2차 방어(최신 레코드와 `isSameData` 비교)
- 캐시 관리: 스케줄러 측 `processedIds`는 필요 시 자동 정리(10,000건 초과 시 clear)

## 설정 예시(`application.properties`)

```properties
# 서버
server.port=8081

# WCS 원천 DB (SQL Server)
spring.datasource.wcs.url=jdbc:sqlserver://localhost:1433;databaseName=cdc_test;encrypt=true;trustServerCertificate=true
spring.datasource.wcs.username=sa
spring.datasource.wcs.password=******
spring.datasource.wcs.driver-class-name=com.microsoft.sqlserver.jdbc.SQLServerDriver

# ETL 추출용(대체 키)
etl.database.url=jdbc:sqlserver://localhost:1433;databaseName=cdc_test;encrypt=true;trustServerCertificate=true
etl.database.username=sa
etl.database.password=******
etl.database.driver=com.microsoft.sqlserver.jdbc.SQLServerDriver

# PostgreSQL 타깃 DB
spring.datasource.postgresql.url=jdbc:postgresql://localhost:5432/cdcqueue?sslmode=disable&reWriteBatchedInserts=true
spring.datasource.postgresql.username=cdcuser
spring.datasource.postgresql.password=cdcpassword
spring.datasource.postgresql.driver-class-name=org.postgresql.Driver

# Kafka
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.consumer.group-id=agv-etl-group

# Kafka 토픽(벤더/도메인)
kafka.topic.antRobotInfo=ant_robot_info_events
kafka.topic.antPodInfo=ant_pod_info_events
kafka.topic.mushinyAgv=mushiny_agv_events
kafka.topic.mushinyPod=mushiny_pod_events

# Quartz + Scheduling
spring.quartz.job-store-type=memory
spring.quartz.thread-pool.thread-count=10

# 스케줄 토글/간격(도메인별)
etl.antRobot.enabled=true
etl.antRobot.interval=500
etl.antRobot.initialDelay=0

etl.antFlypick.enabled=true
etl.antFlypick.interval=500
etl.antFlypick.initialDelay=0

etl.antPod.enabled=true
etl.antPod.interval=1000
etl.antPod.initialDelay=0

etl.mushinyAgv.enabled=true
etl.mushinyAgv.interval=500
etl.mushinyAgv.initialDelay=0

etl.mushinyPod.enabled=true
etl.mushinyPod.interval=1000
etl.mushinyPod.initialDelay=0

# Redis
spring.data.redis.host=localhost
spring.data.redis.port=6379

# Actuator/Prometheus
management.endpoints.web.exposure.include=health,info,prometheus
management.endpoint.prometheus.enabled=true
```

## 실행 방법

```bash
# 인프라 기동(PostgreSQL, Redis, Kafka, Prometheus, Grafana 등)
docker compose up -d postgres redis zookeeper kafka prometheus grafana kafka-ui pgadmin postgres-exporter redis-exporter

# 애플리케이션 실행(개발)
./gradlew bootRun

# 또는 빌드 후 실행
./gradlew build
java -jar build/libs/WCS_DataStream-*.jar
```

## API/모니터링(선택)

- 컨트롤러를 통해 ETL 상태/통계/수동 실행 등 API를 제공할 수 있습니다(구성에 따라 변동).
- 로그에서 추출/필터/저장 건수와 실행 시간을 확인할 수 있습니다.
- Prometheus 엔드포인트: `GET /actuator/prometheus`
- 주요 메트릭 예시:
  - `etl_last_execution_epoch_ms{domain="antRobot"}`
  - `etl_processed_records_total{domain="mushinyAgv"}`
  - `hikaricp_connections_active{pool="PostgreSQLHikariCP"}` (DB 풀 사용 현황)

## 주의사항

1. WCS DB와 PostgreSQL, (사용 시) Kafka 브로커가 실행 중이어야 합니다.
2. 스케줄링 주기가 매우 짧으므로 시스템 리소스를 고려해 조정하세요.
3. 비교 기준 변경 시 각 엔진의 `isSameData(...)`만 수정하면 됩니다.
4. PostgreSQL 커넥션풀(HikariCP)은 기본 최대 10, 최소 idle 2로 설정되어 있습니다.
   - 지표 확인: `/actuator/prometheus`의 `hikaricp_*` 메트릭으로 활성/대기/idle 상태를 모니터링
   - 필요 시 코드 또는 설정으로 풀 크기를 조정하세요.

## Software 구성도

| 영역 | 구성요소 | 주요 역할 | 비고 |
|---|---|---|---|
| 데이터 소스 | WCS DB(SQL Server) | 원천 데이터(AGV/Inventory/POD) 제공 | 읽기 전용 |
| 애플리케이션 | 스케줄러(Base/도메인) | Full/Incremental 트리거 | 주기 설정 가능 |
| 애플리케이션 | ETL 엔진(AGV/Inventory/POD) | Extract/Transform/Filter/Load | 캐시·중복방지 포함 |
| 애플리케이션 | 서비스 계층 | WCS 조회, PostgreSQL 저장, Kafka 전송, Redis 캐시 | 트랜잭션/예외 처리 |
| 미들웨어 | Redis | 캐시 비교·중복 제거 | 권장 |
| 미들웨어 | Kafka | 변경 이벤트 발행 | 선택 기능 |
| 타깃 DB | PostgreSQL | 적재/이력 저장 | 운영 저장소 |
| 모니터링 | Prometheus | 메트릭 수집(/actuator/prometheus, exporters) | 스크랩 |
| 시각화 | Grafana | 메트릭 대시보드 | 알림 설정 가능 |

## Software Layer 구성도

| Layer | 서브 구성요소 | 책임 | 입출력 |
|---|---|---|---|
| Presentation | REST Controller(선택), Grafana 대시보드 | 상태/통계/시각화 | HTTP, 대시보드 |
| Scheduler | Quartz + 도메인 스케줄러 | 주기 트리거, 초기화/강제재처리 | 메소드 호출 |
| Domain Service | ETL 엔진(AGV/Inventory/POD) | 추출/변환/필터/적재 | 도메인 모델 리스트 |
| Application Service | WCS/PG/Redis/Kafka 서비스 | DB I/O, 캐시, 메시지 | JDBC, Redis, Kafka |
| Infrastructure | JdbcTemplate/트랜잭션/설정 | 커넥션/트랜잭션/설정 바인딩 | DataSource, TM |
| Observability | Actuator, Micrometer, Exporters | 메트릭/헬스 | /actuator, /metrics |

### 서비스 실행 순서
1) Zookeeper → 2) Kafka → 3) Redis → 4) PostgreSQL → 5) 애플리케이션 → 6) Prometheus → 7) Grafana

### 서비스 수량(기본 도커 컴포즈)
- App 1, PostgreSQL 1, Redis 1, Zookeeper 1, Kafka 1, Kafka-UI 1, Prometheus 1, Grafana 1, (선택) Exporters 2 

## 환경 구성 표

| 구분 | 항목 | WCS_DataStream |
|---|---|---|
| 개발 Framework | Base | Spring Boot 3.5.3 |  | Front-End | 정적 HTML(대시보드) / Grafana |
|  | Back-End | Java 21 (JDK 21), Spring Scheduling/Quartz, WebSocket(선택) |
| 개발도구 | IDE/Build | IntelliJ/VS Code(선택), Gradle, Docker/Docker Compose |
|  | JDK | Temurin/OpenJDK 21 |
| 웹서버 | Web server | Spring Boot 내장 Tomcat 10.1.x |
|  | WAS | (내장) Tomcat 10.1.x |
| 소스관리 | 형상관리 | Git (GitLab) |
|  | 배포도구 | Docker Compose, 컨테이너 기반 배포 |
| Database | Source(DB) | SQL Server (WCS_DB, 읽기 전용) |
|  | Target(DB) | PostgreSQL 16.9 (적재/이력) |
| OS | Web/App Server | Linux (개발 호스트: 5.15 커널), 컨테이너 기반 운영 |
|  | DB Server | PostgreSQL 컨테이너(16.9) |
| 미들웨어 | Kafka | Confluent Platform cp-kafka:7.4.10 (Zookeeper 포함) |
|  | Redis | redis:7.2-alpine (캐시) |
| 모니터링 | Prometheus | prom/prometheus:latest (스크랩: 앱/Exporters) |
|  | Grafana | grafana/grafana:latest (대시보드) |
| 관리도구 | Kafka UI | provectuslabs/kafka-ui:latest |
|  | pgAdmin | dpage/pgadmin4:latest |

> 비고: 서비스 실행 순서는 README 상단의 "서비스 실행 순서" 참고. 환경은 `docker-compose.yml` 기준으로 재현됩니다.