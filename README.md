# WCS DataStream - ETL 시스템

## 프로젝트 개요

WCS(Warehouse Control System) 데이터를 주기적으로 추출(Extract)·변환(Transform)·적재(Load)하는 독립형 ETL 시스템입니다. 도메인별(AGV/Inventory/POD) 엔진이 병렬로 동작하며, 캐시와 DB 중복 검사를 통해 변경 데이터만 안정적으로 적재합니다.

## 전체 동작 순서(7단계)

1. 애플리케이션을 실행한다.
   - Spring Boot 기동과 함께 스케줄링이 활성화됩니다.
2. WCS_DB의 각 테이블을 읽어온다.
   - 애플리케이션 시작 직후 1회 Full Load 수행(도메인별 전체 조회).
3. 캐시메모리에 저장해놓는다.
   - `uuid_no -> 최신 레코드` 형태로 in-memory 캐시에 보관됩니다.
4. PostGreDB에서 테이블이 비어있으면 insert하고 중복데이터면 Insert하지 않는다.
   - DB 레벨에서 `uuid_no + report_time` 조합으로 중복을 1차 차단합니다.
5. 스케줄링 설정을 통해 0.1초마다 각 테이블을 마지막 LatestTime 조건 이후로 데이터를 가져와서 캐시 데이터와 비교한다.
   - PostgreSQL에 저장된 `MAX(report_time)`를 기준으로 WCS에서 `>= latest`만 증분 조회합니다.
6. 데이터 비교를 감지하면 PostGre DB에 각 데이터를 삽입한다.
   - 현재는 모든 필드를 비교하여 다르면 변경으로 간주합니다(`isSameData`).
   - 향후 X/Y 좌표나 상태만 비교하도록 손쉽게 커스터마이징할 수 있습니다.
7. 최신데이터만 캐시에 업데이트한다.
   - 새 레코드의 `report_time`이 캐시보다 클 때만 캐시를 갱신합니다.

## 시스템 아키텍처
  
```
WCS_DB (Source) → ETL 엔진(Extract/Transform/Filter) → PostgreSQL (Target) + Kafka
      ↑                 ↓                                 ↓
   스케줄러           변경 감지(캐시 + DB)              중복 방지·저장
```

- 스케줄러: 0.1초(fixedRate) 간격으로 각 ETL 엔진을 트리거합니다.
- 변경 감지: 
  - DB 중복 체크: `uuid_no + report_time` 존재 시 스킵(1차 방어)
  - 캐시 비교: 최신 레코드와 `isSameData`로 내용 비교(2차 방어)
- 저장/발행: 변경된 데이터만 PostgreSQL에 Insert, 필요 시 Kafka로 이벤트 발행

## 구성 요소와 역할

- 엔트리 포인트
  - `DataStreamApplication`(Spring Boot): 애플리케이션 시작, 스케줄링 활성화
  - `SchedulerInitializer`: 기동 완료 시 각 스케줄러 초기화/캐시 리셋
- 스케줄러(`etl/scheduler`)
  - `AgvDataScheduler`, `InventoryDataScheduler`, `PodDataScheduler`
    - 시작 시 `executeFullLoad()` 1회 수행, 이후 0.1초 간격으로 증분 `executeETL()` 수행
- ETL 엔진(`etl/engine`)
  - `AgvDataETLEngine`, `InventoryDataETLEngine`, `PodDataETLEngine`
    - Extract: PostgreSQL의 `MAX(report_time)` 기준으로 WCS 증분 조회
    - Transform: UUID/누락 값 보정 등 최소 변환
    - Filter: 
      - DB 중복 검사(`is...DataExists(uuid_no, report_time)`) → 스킵
      - 캐시 최신 레코드와 `isSameData` 비교 → 변경 시만 처리
    - Load: PostgreSQL 배치 Insert, Kafka 메시지 발행(도메인별 토픽)
    - 캐시: `uuid_no -> 최신 레코드`로 갱신(최신 시각만 반영)
- 서비스(`etl/service`)
  - `AgvDataService`, `InventoryDataService`, `PodDataService`: WCS DB 조회(전체/증분)
  - `PostgreSQLDataService`: Insert/중복 체크/최신시각 조회
  - `KafkaProducerService`: 변경 사항 메시지 발행
- 모델(`etl/model`)
  - `AgvData`, `InventoryInfo`, `PodInfo`: Lombok `@Data` 기반(필드 동일성 판단에 활용)

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

- 스케줄링: 기본 0.1초(100ms) 주기 실행
- 배치 저장: 기본 100건 단위 배치 처리
- 중복 방지: 
  - DB 1차 방어(`uuid_no + report_time`)
  - 캐시 2차 방어(최신 레코드와 `isSameData` 비교)
- 캐시 관리: 스케줄러 측 `processedIds`는 필요 시 자동 정리(10,000건 초과 시 clear)

## 설정 예시(`application.properties`)

```properties
# PostgreSQL
spring.datasource.postgresql.url=jdbc:postgresql://localhost:5432/etl_db
spring.datasource.postgresql.username=postgres
spring.datasource.postgresql.password=password

# Kafka
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.consumer.group-id=agv-etl-group

# ETL 스케줄링 (도메인별 주기/초기 지연) - 예시
etl.agv.interval=100
etl.agv.initialDelay=1000
etl.inventory.interval=100
etl.inventory.initialDelay=1200
etl.pod.interval=100
etl.pod.initialDelay=1400
```

## 실행 방법

```bash
# 애플리케이션 실행
./gradlew bootRun

# 또는 빌드 후 실행
./gradlew build
java -jar build/libs/WCS_DataStream-*.jar
```

## API/모니터링(선택)

- 컨트롤러를 통해 ETL 상태/통계/수동 실행 등 API를 제공할 수 있습니다(구성에 따라 변동).
- 로그에서 추출/필터/저장 건수와 실행 시간을 확인할 수 있습니다.

## 주의사항

1. WCS DB와 PostgreSQL, (사용 시) Kafka 브로커가 실행 중이어야 합니다.
2. 스케줄링 주기가 매우 짧으므로 시스템 리소스를 고려해 조정하세요.
3. 비교 기준 변경 시 각 엔진의 `isSameData(...)`만 수정하면 됩니다. 

## Software 구성도

| 영역 | 구성요소 | 주요 역할 | 비고 |
|---|---|---|---|
| 데이터 소스 | WCS DB(SQL Server) | 원천 데이터(AGV/Inventory/POD) 제공 | 읽기 전용 |
| 애플리케이션 | 스케줄러(Base/도메인) | Full/Incremental 트리거 | 주기 설정 가능 |
| 애플리케이션 | ETL 엔진(AGV/Inventory/POD) | Extract/Transform/Filter/Load | 캐시·중복방지 포함 |
| 애플리케이션 | 서비스 계층 | WCS 조회, PostgreSQL 저장, Kafka 전송, Redis 캐시 | 트랜잭션/예외 처리 |
| 미들웨어 | Redis | 캐시 비교·중복 제거 | 선택 필수(성능) |
| 미들웨어 | Kafka | 변경 이벤트 발행 | 선택 기능 |
| 타깃 DB | PostgreSQL | 적재/이력 저장 | 운영 저장소 |
| 모니터링 | Prometheus | 메트릭 수집(/actuator/prometheus, exporters) | 스크랩 |
| 시각화 | Grafana | 메트릭 대시보드 | 알림 설정 가능 |

## Software Layer 구성도

| Layer | 서브 구성요소 | 책임 | 입출력 |
|---|---|---|---|
| Presentation | REST Controller(선택), Grafana 대시보드 | 상태/통계/시각화 | HTTP, 대시보드 |
| Scheduler | BaseETLScheduler + 도메인 스케줄러 | 주기 트리거, 초기화/강제재처리 | 메소드 호출 |
| Domain Service | ETL 엔진(AGV/Inventory/POD) | 추출/변환/필터/적재 | 도메인 모델 리스트 |
| Application Service | WCS/PG/Redis/Kafka 서비스 | DB I/O, 캐시, 메시지 | JDBC, Redis, Kafka |
| Infrastructure | JdbcTemplate/트랜잭션/설정 | 커넥션/트랜잭션/설정 바인딩 | DataSource, TM |
| Observability | Actuator, Micrometer, Exporters | 메트릭/헬스 | /actuator, /metrics |

### 서비스 실행 순서
1) Zookeeper → 2) Kafka → 3) Redis → 4) PostgreSQL → 5) 애플리케이션 → 6) Prometheus → 7) Grafana

### 서비스 수량(기본 도커 컴포즈)
- App 1, PostgreSQL 1, Redis 1, Zookeeper 1, Kafka 1, Kafka-UI 1, Prometheus 1, Grafana 1, (선택) Exporters 2 