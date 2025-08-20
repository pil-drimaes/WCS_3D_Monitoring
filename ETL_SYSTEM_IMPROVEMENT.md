# ETL 시스템 개선 사항 및 테스트 가이드

## 개선된 구조

### 1. 공통 BaseETLScheduler 생성
- 모든 ETL 스케줄러가 상속받는 공통 베이스 클래스
- 초기화, 설정 관리, 공통 로직 제공
- 초기 데이터 처리와 증분 데이터 처리 분리

### 2. 스케줄러 구조 통일
- `AgvDataScheduler`: BaseETLScheduler 상속, 중복 방지 캐시 포함
- `InventoryDataScheduler`: BaseETLScheduler 상속, 초기/증분 데이터 처리 분리
- `PodDataScheduler`: BaseETLScheduler 상속, 초기/증분 데이터 처리 분리

### 3. 중복 데이터 처리 개선
- `AgvDataETLEngine`: PostgreSQL 중복 체크를 통한 중복 방지
- `InventoryDataETLEngine`: report_time 기반 중복 필터링
- `PodDataETLEngine`: report_time 기반 중복 필터링

## 테스트 시나리오

### 1단계: 초기 데이터 삽입
```sql
-- WCS_DB에 초기 데이터 삽입
-- robot_info: 20개, inventory_info: 10개, pod_info: 10개
-- src/main/resources/init_wcs_data.sql 실행
```

### 2단계: 애플리케이션 실행
```bash
# 애플리케이션 실행
./gradlew bootRun
```

**예상 동작:**
- BaseETLScheduler가 각 스케줄러 초기화
- 각 스케줄러가 초기 데이터 처리 (한 번만)
- PostgreSQL System_DB에 초기 데이터 삽입
- 캐시에 처리된 데이터 ID 저장

### 3단계: 증분 데이터 삽입
```sql
-- WCS_DB에 새로운 데이터 1개씩 삽입
-- src/main/resources/incremental_wcs_data.sql 실행
```

**예상 동작:**
- ETL 엔진이 새로운 데이터 감지
- 중복 체크 후 새로운 데이터만 PostgreSQL에 삽입
- Kafka로 메시지 전송

### 4단계: 기존 데이터 업데이트
```sql
-- WCS_DB의 기존 데이터 업데이트
-- src/main/resources/update_wcs_data.sql 실행
```

**예상 동작:**
- `isSameData()` 함수가 데이터 변화 감지
- 변경된 데이터를 PostgreSQL에 새 레코드로 삽입
- report_time이 다른 새로운 레코드 생성

## 주요 개선 사항

### 1. 중복 처리 방지
- **이전**: 모든 데이터를 계속 insert
- **개선**: PostgreSQL 중복 체크 + 캐시 기반 중복 방지

### 2. 초기/증분 데이터 분리
- **이전**: 모든 실행에서 동일한 로직
- **개선**: 초기 실행 시 전체 데이터, 이후 증분 데이터만 처리

### 3. 공통 관리
- **이전**: 각 스케줄러마다 다른 초기화 로직
- **개선**: BaseETLScheduler에서 공통 초기화 및 설정 관리

## 로그 확인 포인트

### 애플리케이션 시작 시
```
[INFO] AGV Data ETL 엔진 초기화 완료
[INFO] Inventory Data ETL 엔진 초기화 완료
[INFO] POD Data ETL 엔진 초기화 완료
[INFO] AGV 초기 데이터 처리 시작
[INFO] 재고 정보 초기 데이터 처리 시작
[INFO] POD 정보 초기 데이터 처리 시작
```

### 초기 데이터 처리 완료 시
```
[INFO] AGV 초기 데이터 처리 완료: 20개 레코드
[INFO] 재고 정보 초기 데이터 처리 완료: 10개 레코드 처리
[INFO] POD 정보 초기 데이터 처리 완료: 10개 레코드 처리
```

### 증분 데이터 처리 시
```
[INFO] AGV 증분 데이터 처리 완료: 1개 레코드 처리
[INFO] 재고 정보 증분 데이터 처리 완료: 1개 레코드 처리
[INFO] POD 정보 증분 데이터 처리 완료: 1개 레코드 처리
```

## 문제 해결

### 1. 초기 데이터가 System_DB에 삽입되지 않는 경우
- WCS_DB 연결 상태 확인
- PostgreSQL 연결 상태 확인
- 로그에서 오류 메시지 확인

### 2. 중복 데이터가 계속 삽입되는 경우
- PostgreSQL 중복 체크 메서드 확인
- 캐시 초기화 상태 확인
- `isSameData()` 함수 로직 확인

### 3. 특정 테이블만 데이터가 처리되지 않는 경우
- 해당 서비스의 데이터베이스 연결 확인
- 테이블 구조 및 컬럼명 확인
- SQL 쿼리 오류 로그 확인

## 성능 최적화

### 1. 배치 처리
- 기본 배치 크기: 100
- 설정 가능: `etl.pulling.batch-size`

### 2. 풀링 간격
- 기본 풀링 간격: 0.1초 (100ms)
- 설정 가능: `etl.pulling.interval`

### 3. 캐시 관리
- 처리된 데이터 ID 캐시 크기 제한: 10,000개
- 메모리 관리 자동 정리

## 설정 변경

### application.properties에서 ETL 설정 변경
```properties
# ETL 설정
etl.pulling.interval=1000
etl.pulling.batch-size=100
etl.pulling.strategy=HYBRID
etl.validation.enabled=true
etl.transformation.enabled=true
etl.error-handling.mode=CONTINUE
etl.retry.count=3
etl.retry.interval=5000
```

### 개별 스케줄러 설정 변경
각 스케줄러에서 `createDefaultConfig()` 메서드를 오버라이드하여 개별 설정 가능 