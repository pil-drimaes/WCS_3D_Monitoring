# CDC Queue - Change Data Capture System

MSSQL에서 PostgreSQL로 실시간 데이터 동기화를 수행하는 Change Data Capture (CDC) 시스템입니다.

## 🚀 주요 기능

### **실시간 데이터 동기화**
- **Robot 데이터**: 0.1초마다 실시간 감지 및 동기화 ✅
- **Inventory 데이터**: 3초마다 재고 정보 동기화
- **Pod 데이터**: 3초마다 POD 정보 동기화

### **하이브리드 풀링 엔진**
- 조건부 쿼리와 주기적 전체 동기화를 결합한 효율적인 데이터 변경 감지
- 10분마다 전체 동기화 수행으로 데이터 무결성 보장

### **자동 중복 필터링**
- `report_time` 기반의 스마트한 중복 데이터 필터링
- 새로운 데이터와 업데이트된 데이터만 처리

## 🏗️ 아키텍처

```
MSSQL (Source) → ETL Engine → PostgreSQL (Target)
     ↓              ↓              ↓
  robot_info   AgvDataETLEngine  robot_info
inventory_info InventoryDataETLEngine inventory_info
   pod_info    PodDataETLEngine     pod_info
```

## 📁 프로젝트 구조

```
src/main/java/com/example/cdcqueue/
├── CdcQueueApplication.java          # 메인 애플리케이션
├── common/
│   ├── config/
│   │   ├── DatabaseConfig.java      # 데이터베이스 설정
│   │   ├── ETLProperties.java       # ETL 설정
│   │   ├── JacksonConfig.java       # JSON 설정
│   │   └── PostgreSQLConfig.java    # PostgreSQL 설정
│   ├── model/
│   │   ├── AgvData.java            # Robot 데이터 모델
│   │   ├── InventoryInfo.java      # 재고 데이터 모델
│   │   ├── PodInfo.java            # POD 데이터 모델
│   │   └── CdcEvent.java           # CDC 이벤트 모델
│   └── queue/
│       └── EventQueue.java         # 이벤트 큐
├── etl/
│   ├── AgvDataETLEngine.java       # Robot ETL 엔진 (핵심)
│   ├── InventoryDataETLEngine.java # 재고 ETL 엔진
│   ├── PodDataETLEngine.java       # POD ETL 엔진
│   ├── DataETLEngine.java          # ETL 인터페이스
│   ├── ETLConfig.java              # ETL 설정
│   ├── ETLStatistics.java          # ETL 통계
│   ├── ETLEngineException.java     # ETL 예외 처리
│   ├── engine/
│   │   ├── HybridPullingEngine.java    # 하이브리드 풀링 엔진
│   │   ├── AgvHybridPullingEngine.java # Robot 전용 풀링 엔진
│   │   ├── DataPullingEngine.java      # 풀링 엔진 인터페이스
│   │   └── PullingEngineConfig.java    # 풀링 엔진 설정
│   ├── service/
│   │   ├── AgvDataService.java         # Robot 데이터 서비스
│   │   ├── InventoryDataService.java   # 재고 데이터 서비스
│   │   ├── PodDataService.java         # POD 데이터 서비스
│   │   ├── PostgreSQLDataService.java  # PostgreSQL 데이터 서비스
│   │   ├── AgvDataTask.java            # Robot 스케줄링 태스크
│   │   ├── InventoryDataTask.java      # 재고 스케줄링 태스크
│   │   └── PodDataTask.java            # POD 스케줄링 태스크
│   └── controller/
│       ├── AgvDataController.java      # Robot API 컨트롤러
│       ├── InventoryDataController.java # 재고 API 컨트롤러
│       └── PodDataController.java      # POD API 컨트롤러
└── resources/
    ├── application.properties         # 애플리케이션 설정
    └── static/                       # 대시보드 UI
        ├── index.html                 # 메인 대시보드
        ├── realtime-monitor.html      # 실시간 모니터링
        ├── independent-etl-dashboard.html # 독립 ETL 대시보드
        ├── inventory-dashboard.html   # 재고 대시보드
        └── pod-dashboard.html         # POD 대시보드
```

## ⚙️ 설정

### **데이터베이스 연결**
```properties
# MSSQL (Source)
spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=cdc_test
spring.datasource.username=sa
spring.datasource.password=nice2025!

# PostgreSQL (Target)
spring.postgresql.url=jdbc:postgresql://localhost:5432/cdcqueue
spring.postgresql.username=postgres
spring.postgresql.password=postgres
```

### **ETL 설정**
```properties
# Robot ETL (0.1초마다 실행)
etl.agv.pull-interval=100ms
etl.agv.batch-size=100

# Inventory ETL (3초마다 실행)
etl.inventory.pull-interval=3000ms

# Pod ETL (3초마다 실행)
etl.pod.pull-interval=3000ms

# 전체 동기화 주기 (10분)
etl.full-sync-interval=600000ms
```

## 🚀 실행 방법

### **1. 데이터베이스 시작**
```bash
# PostgreSQL 시작
docker-compose up -d postgres

# MSSQL 시작 (별도 설정 필요)
# localhost:1433, database: cdc_test
```

### **2. 애플리케이션 실행**
```bash
./gradlew bootRun
```

### **3. 대시보드 접속**
- **메인 대시보드**: http://localhost:8081/
- **실시간 모니터링**: http://localhost:8081/realtime-monitor.html
- **재고 대시보드**: http://localhost:8081/inventory-dashboard.html
- **POD 대시보드**: http://localhost:8081/pod-dashboard.html

## 📊 API 엔드포인트

### **Robot 데이터**
- `GET /api/agv/data` - Robot 데이터 조회
- `GET /api/agv/status` - Robot 상태 조회

### **재고 데이터**
- `GET /api/inventory/data` - 재고 데이터 조회
- `GET /api/inventory/status` - 재고 상태 조회

### **POD 데이터**
- `GET /api/pod/data` - POD 데이터 조회
- `GET /api/pod/status` - POD 상태 조회

### **PostgreSQL 데이터**
- `GET /api/postgresql/data` - PostgreSQL 데이터 상태 조회

## 🔧 핵심 기술

### **ETL 엔진**
- **Extract**: MSSQL에서 변경된 데이터 추출
- **Transform**: 데이터 변환 및 검증
- **Load**: PostgreSQL에 데이터 저장

### **하이브리드 풀링**
- **조건부 쿼리**: `report_time` 기반 변경 데이터 감지
- **전체 동기화**: 주기적 전체 데이터 검증
- **중복 필터링**: 스마트한 데이터 중복 제거

### **실시간 처리**
- **0.1초 주기**: Robot 데이터 실시간 감지
- **3초 주기**: Inventory/POD 데이터 주기적 감지
- **10분 주기**: 전체 데이터 무결성 검증

## 📈 성능 특징

- **Robot ETL**: 0.1초마다 실시간 처리 (22개 → 45개 자동 증가 확인)
- **메모리 효율성**: `ConcurrentHashMap` 기반 캐시 관리
- **배치 처리**: 대용량 데이터 처리 지원
- **자동 복구**: 연결 실패 시 자동 재시도

## 🧪 테스트 결과

### **✅ 성공한 기능**
1. **초기 데이터 로드**: 모든 테이블 정상 삽입
2. **Robot 실시간 감지**: 데이터 변경 시 즉시 PostgreSQL 반영
3. **자동 중복 필터링**: report_time 기반 스마트 필터링

### **🔧 최적화 완료**
1. **불필요한 파일 제거**: 테스트용 파일들 정리
2. **사용되지 않는 기능 제거**: WebSocket, Kafka 관련 코드 정리
3. **코드 구조 정리**: 핵심 ETL 기능 중심으로 단순화

## 📝 라이선스

이 프로젝트는 MIT 라이선스 하에 배포됩니다. 

## 🤝 기여

버그 리포트나 기능 제안은 이슈로 등록해 주세요.

---

**CDC Queue** - 실시간 데이터 동기화의 새로운 표준 🚀 