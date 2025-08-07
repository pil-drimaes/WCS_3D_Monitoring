# 🚀 ETL 시스템 전체 구조

## 📁 ETL 폴더 구조
```
src/main/java/com/example/cdcqueue/etl/
├── AgvDataETLEngine.java          # 메인 ETL 엔진
├── DataETLEngine.java             # ETL 인터페이스
├── ETLConfig.java                 # ETL 설정
├── ETLEngineException.java        # 예외 처리
├── ETLStatistics.java             # 통계 관리
├── controller/
│   └── AgvDataController.java     # API 엔드포인트
├── engine/
│   ├── DataPullingEngine.java     # 데이터 풀링 인터페이스
│   ├── HybridPullingEngine.java   # 하이브리드 풀링 엔진
│   ├── PullingEngineConfig.java   # 풀링 설정
│   └── DatabaseConfig.java        # DB 설정
└── service/
    ├── AgvDataService.java        # AGV 데이터 서비스
    ├── AgvDataTask.java        # 스케줄링 태스크
    └── WebSocketPushTask.java     # WebSocket 푸시
```

## 🚀 ETL 실행 흐름 (시작점부터 끝까지)

### 1단계: 애플리케이션 시작

**애플리케이션 시작 시:**
- `@EnableScheduling`: 스케줄링 기능 활성화
- `@EnableConfigurationProperties`: 설정 파일 읽기 활성화
- Spring Boot가 모든 `@Component`를 스캔하여 빈으로 등록

### 2단계: 스케줄링 태스크 시작

**스케줄링 태스크 동작:**
- **1초마다** `processAgvData()` 실행
- 첫 실행 시 `initializeETL()` 호출하여 ETL 엔진 초기화
- 이후 `etlEngine.executeETL()` 호출

### 3단계: ETL 엔진 실행

**ETL 엔진의 3단계 프로세스:**

1. **Extract (추출)**: `extractData()` → `pullingEngine.pullNewData()`
2. **Transform (변환)**: `transformData()` → 데이터 검증 및 변환
3. **Load (적재)**: `loadData()` → 이벤트 큐에 추가

### 4단계: 데이터 풀링 엔진 (변화 감지 핵심)

**하이브리드 풀링 엔진 동작:**
- **증분 풀링**: 마지막 체크 시간 이후 데이터만 조회
- **전체 동기화**: 1시간마다 전체 데이터 동기화
- **캐시 비교**: 메모리 캐시와 비교하여 실제 변화만 감지

### 5단계: 데이터베이스 서비스 (실제 데이터 조회)

**데이터베이스 쿼리 실행:**
```sql
SELECT uuid_no, robot_no, map_code, zone_code, status, manual, 
       loaders, report_time, battery, node_id, pos_x, pos_y, 
       speed, task_id, next_target, pod_id
FROM agv_data 
WHERE report_time > ? 
ORDER BY report_time DESC
```

## 🔍 변화 감지 로직 상세 분석

### 핵심 변화 감지 메커니즘

**1. 시간 기반 변화 감지**
- `report_time > ?` 조건으로 마지막 체크 시간 이후 데이터만 조회
- Unix timestamp 기반으로 정확한 시간 비교

**2. 캐시 기반 변화 감지**
```java
private boolean isSameData(AgvData data1, AgvData data2) {
    return Objects.equals(data1.getAgvId(), data2.getAgvId()) &&
           Objects.equals(data1.getX(), data2.getX()) &&
           Objects.equals(data1.getY(), data2.getY()) &&
           Objects.equals(data1.getTimestamp(), data2.getTimestamp());
}
```

**3. 하이브리드 풀링 전략**
- **증분 풀링**: 마지막 체크 시간 이후 데이터만 조회
- **전체 동기화**: 1시간마다 전체 데이터 동기화
- **캐시 비교**: 메모리 캐시와 비교하여 실제 변화만 감지

## 📊 ETL 시스템 전체 흐름도

```
┌─────────────────┐
│   애플리케이션 시작  │
│ @EnableScheduling│
└─────────┬───────┘
          │
          ▼
┌─────────────────┐
│ AgvDataTask  │
│ @Scheduled(0.1초)  │
└─────────┬───────┘
          │
          ▼
┌─────────────────┐
│ AgvDataETLEngine│
│ executeETL()    │
└─────────┬───────┘
          │
          ▼
┌─────────────────┐
│ 1. Extract      │
│ pullNewData()   │
└─────────┬───────┘
          │
          ▼
┌─────────────────┐
│HybridPullingEngine│
│ 변화 감지 로직    │
└─────────┬───────┘
          │
          ▼
┌─────────────────┐
│ AgvDataService  │
│ SQL 쿼리 실행    │
└─────────┬───────┘
          │
          ▼
┌─────────────────┐
│ WCS Database    │
│ agv_data 테이블  │
└─────────────────┘
```

## 🔍 변화 감지 조건 및 로직

### 1. 시간 기반 변화 감지
```sql
-- 핵심 SQL 쿼리
SELECT * FROM agv_data 
WHERE report_time > ? 
ORDER BY report_time DESC
```

### 2. 캐시 기반 변화 감지
```java
// 캐시에 저장된 데이터와 비교
private boolean isSameData(AgvData data1, AgvData data2) {
    return Objects.equals(data1.getAgvId(), data2.getAgvId()) &&
           Objects.equals(data1.getX(), data2.getX()) &&
           Objects.equals(data1.getY(), data2.getY()) &&
           Objects.equals(data1.getTimestamp(), data2.getTimestamp());
}
```

### 3. 하이브리드 풀링 전략
- **증분 풀링**: 마지막 체크 시간 이후 데이터만 조회
- **전체 동기화**: 1시간마다 전체 데이터 동기화
- **캐시 비교**: 메모리 캐시와 비교하여 실제 변화만 감지

## ⚡ 성능 최적화 포인트

1. **시간 기반 필터링**: `report_time > ?` 조건으로 불필요한 데이터 조회 방지
2. **캐시 활용**: `ConcurrentHashMap`으로 메모리 캐시 관리
3. **배치 처리**: 여러 레코드를 한 번에 처리
4. **전체 동기화**: 1시간마다 캐시 리셋으로 메모리 누수 방지

---

이렇게 ETL 시스템은 **시간 기반 + 캐시 기반**의 하이브리드 방식으로 데이터 변화를 효율적으로 감지하고 처리합니다! 🎯 