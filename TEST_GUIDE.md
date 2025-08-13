# ETL 엔진 테스트 가이드

## 📋 테스트 개요

이 문서는 AGV 데이터 ETL 엔진의 동작을 검증하기 위한 테스트 과정을 설명합니다.

## 🎯 테스트 목표

1. **초기 데이터 처리**: MSSQL의 샘플 데이터를 PostgreSQL로 정상 이관 확인
2. **실시간 데이터 감지**: 0.1초 주기로 데이터 변화 감지 확인
3. **데이터 업데이트 처리**: MSSQL 데이터 변경 시 PostgreSQL 동기화 확인
4. **ETL 엔진 안정성**: 지속적인 데이터 처리 및 오류 처리 확인

## 🚀 테스트 단계

### 1단계: MSSQL 샘플 데이터 삽입

**파일**: `test_data_insert.sql`

```bash
# MSSQL Management Studio 또는 sqlcmd에서 실행
sqlcmd -S localhost -U sa -P nice2025! -d cdc_test -i test_data_insert.sql
```

**예상 결과**:
- `robot_info`: 20개 레코드
- `inventory_info`: 10개 레코드  
- `pod_info`: 10개 레코드

### 2단계: 애플리케이션 실행

```bash
# 프로젝트 루트 디렉토리에서
./gradlew bootRun
```

**확인 사항**:
- 애플리케이션이 8081 포트에서 정상 실행
- ETL 엔진 초기화 완료
- PostgreSQL 연결 성공

### 3단계: ETL 상태 모니터링

**브라우저에서 접속**: `http://localhost:8081/etl-test.html`

**확인 항목**:
- ETL 엔진 상태: `RUNNING`
- MSSQL 연결: `true`
- PostgreSQL 연결: `true`
- PostgreSQL 테이블 존재: `true`

### 4단계: 초기 데이터 이관 확인

**PostgreSQL 확인**:
```sql
-- PostgreSQL에서 실행
SELECT COUNT(*) FROM robot_info;        -- 20개 예상
SELECT COUNT(*) FROM inventory_info;    -- 10개 예상
SELECT COUNT(*) FROM pod_info;          -- 10개 예상
```

### 5단계: 데이터 업데이트 테스트

**파일**: `test_data_update.sql`

```bash
# MSSQL에서 실행
sqlcmd -S localhost -U sa -P nice2025! -d cdc_test -i test_data_update.sql
```

**변경 내용**:
- 새로운 로봇 1개 추가 (ROBOT_021)
- 새로운 재고 1개 추가 (WH-F01-20-3)
- 새로운 POD 1개 추가 (POD011)
- 기존 로봇 5개 위치/배터리/상태 업데이트
- 기존 재고 5개 수량/상태 업데이트
- 기존 POD 3개 위치 업데이트

### 6단계: 실시간 데이터 감지 확인

**모니터링 방법**:
1. ETL 테스트 페이지에서 "ETL 상태 확인" 버튼 클릭
2. 5초마다 자동 새로고침으로 상태 변화 관찰
3. PostgreSQL 데이터 개수 변화 확인

**예상 결과**:
- 새로운 데이터 감지 및 PostgreSQL 저장
- 업데이트된 데이터 감지 및 PostgreSQL 동기화
- 총 레코드 수 증가 확인

## 🔍 상세 검증 항목

### 데이터 품질 검증

```sql
-- PostgreSQL에서 실행
-- 1. 데이터 중복 확인
SELECT robot_no, COUNT(*) FROM robot_info GROUP BY robot_no HAVING COUNT(*) > 1;

-- 2. NULL 값 확인
SELECT COUNT(*) FROM robot_info WHERE robot_no IS NULL OR pos_x IS NULL;

-- 3. 데이터 정합성 확인
SELECT robot_no, pos_x, pos_y, battery, status, report_time 
FROM robot_info 
WHERE robot_no IN ('ROBOT_001', 'ROBOT_005', 'ROBOT_010', 'ROBOT_015', 'ROBOT_020')
ORDER BY robot_no, report_time DESC;
```

### ETL 성능 검증

**로그 확인**:
```bash
# 애플리케이션 로그에서 확인
tail -f logs/application.log | grep "ETL"
```

**성능 지표**:
- 데이터 처리 속도: 0.1초 이내
- PostgreSQL 저장 성공률: 100%
- 오류 발생률: 0%

## 🚨 문제 해결

### 일반적인 문제

1. **PostgreSQL 연결 실패**
   - PostgreSQL 서비스 실행 상태 확인
   - 연결 정보 (host, port, username, password) 확인
   - 방화벽 설정 확인

2. **MSSQL 연결 실패**
   - SQL Server 서비스 실행 상태 확인
   - TCP/IP 프로토콜 활성화 확인
   - 포트 1433 접근 가능 여부 확인

3. **ETL 엔진 초기화 실패**
   - 데이터베이스 테이블 존재 여부 확인
   - 로그에서 구체적인 오류 메시지 확인
   - 의존성 서비스 상태 확인

### 디버깅 방법

1. **로그 레벨 조정**
   ```properties
   # application.properties
   logging.level.com.example.cdcqueue.etl=DEBUG
   logging.level.com.example.cdcqueue.engine=DEBUG
   ```

2. **수동 ETL 실행**
   - ETL 테스트 페이지에서 "ETL 수동 실행" 버튼 사용
   - 개별 단계별 실행 결과 확인

3. **캐시 리셋**
   - 데이터 변화 감지 문제 시 "캐시 리셋" 버튼 사용
   - 풀링 엔진 캐시 초기화

## 📊 성공 기준

### 기능적 요구사항
- [ ] 초기 데이터 40개 모두 PostgreSQL 이관 완료
- [ ] 0.1초 주기 데이터 변화 감지 정상 작동
- [ ] 새로운 데이터 추가 시 실시간 감지 및 저장
- [ ] 기존 데이터 업데이트 시 실시간 감지 및 동기화

### 성능 요구사항
- [ ] 데이터 처리 지연시간 < 1초
- [ ] PostgreSQL 저장 성공률 > 99%
- [ ] 메모리 사용량 안정적 유지
- [ ] CPU 사용률 < 50%

### 안정성 요구사항
- [ ] 24시간 연속 실행 시 오류 없음
- [ ] 네트워크 단절 시 자동 복구
- [ ] 데이터베이스 연결 실패 시 재시도 로직 정상 작동
- [ ] 로그 파일 크기 관리 정상

## 🔄 지속적 모니터링

### 자동화된 테스트

```bash
# 주기적 상태 확인 스크립트
while true; do
    curl -s http://localhost:8081/api/test/etl/status | jq '.etlEngineHealthy'
    sleep 30
done
```

### 알림 설정

- ETL 엔진 상태 변화 시 알림
- 데이터 처리 실패 시 알림
- 성능 지표 임계값 초과 시 알림

## 📝 테스트 결과 기록

**테스트 일시**: _______________
**테스트자**: _______________
**환경**: _______________

| 항목 | 예상 결과 | 실제 결과 | 상태 |
|------|-----------|-----------|------|
| 초기 데이터 이관 | 40개 | ___개 | ⭕/❌ |
| 실시간 감지 | 0.1초 | ___초 | ⭕/❌ |
| 데이터 동기화 | 100% | ___% | ⭕/❌ |
| 성능 | <1초 | ___초 | ⭕/❌ |

**특이사항**: _______________

**개선점**: _______________ 