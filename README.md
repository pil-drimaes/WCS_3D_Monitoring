# WCS DataStream - ETL 시스템

## 프로젝트 개요

WCS(Warehouse Control System) 데이터를 실시간으로 추출, 변환, 적재하는 ETL 시스템입니다. 기존 CDC 시스템과 완전히 분리된 독립적인 ETL 엔진으로 구성되어 있습니다.

## 주요 기능

### 📊 ETL 엔진
- **AGV 데이터 ETL**: 로봇 정보 실시간 처리
- **재고 데이터 ETL**: 재고 정보 실시간 처리  
- **POD 데이터 ETL**: POD 정보 실시간 처리
- **중복 데이터 필터링**: PostgreSQL + 캐시 기반 중복 방지
- **변화 감지**: `isSame` 함수를 통한 데이터 변화 감지

### 📊 데이터 처리
- **초기 데이터 로드**: 애플리케이션 시작 시 WCS_DB 전체 데이터 로드
- **증분 데이터 처리**: 마지막 업데이트 시간 기준 변경된 데이터만 처리
- **배치 처리**: 대용량 데이터 효율적 처리
- **실시간 스케줄링**: 5초 간격으로 데이터 변경사항 모니터링

### 📋️ 데이터베이스 연동
- **WCS DB**: SQL Server 기반 원본 데이터 추출
- **PostgreSQL**: 처리된 데이터 저장 및 중복 체크
- **연결 풀링**: Spring Boot 자동 연결 관리
- **테이블 존재 확인**: 초기화 시점에만 확인하여 성능 최적화

### 📡 메시징 시스템
- **Kafka 연동**: 실시간 데이터 변경사항 전파
- **토픽별 분리**: AGV, 재고, POD 데이터별 독립적 토픽
- **메시지 이력**: PostgreSQL에 Kafka 메시지 처리 이력 저장

## 시스템 아키텍처

```
WCS_DB (SQL Server) → ETL 엔진 → PostgreSQL + Kafka
     ↓                    ↓              ↓
  데이터 추출        중복 필터링      데이터 저장
  (Extract)        (Transform)     (Load)
```

이 README는 현재 구현된 코드의 주요 기능과 아키텍처를 반영하여 작성되었습니다. 필요에 따라 추가적인 섹션이나 상세 내용을 추가할 수 있습니다.

## API 엔드포인트

### ETL 상태 관리
- `GET /api/etl/status` - ETL 엔진 상태 조회
- `GET /api/etl/statistics` - ETL 통계 정보
- `POST /api/etl/execute` - 수동 ETL 실행
- `POST /api/etl/reinitialize` - ETL 엔진 재초기화
- `POST /api/etl/reset-cache` - 캐시 리셋

### 데이터 조회
- `GET /api/etl/wcs/data` - WCS DB 전체 AGV 데이터
- `GET /api/etl/wcs/connection` - WCS DB 연결 상태

### 캐시 관리
- `GET /api/etl/cache/data` - 캐시된 데이터 조회
- `GET /api/etl/cache/statistics` - 캐시 통계 정보

## 설정

### application.properties
```properties
# WCS DB 연결 설정
etl.database.driver=com.microsoft.sqlserver.jdbc.SQLServerDriver
etl.database.url=jdbc:sqlserver://localhost:1433;databaseName=cdc_test;encrypt=true;trustServerCertificate=true
etl.database.username=sa
etl.database.password=nice2025!

# PostgreSQL 설정
spring.datasource.postgresql.url=jdbc:postgresql://localhost:5432/etl_db
spring.datasource.postgresql.username=postgres
spring.datasource.postgresql.password=password

# Kafka 설정
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.consumer.group-id=agv-etl-group
```

## 데이터 모델

### AgvData
- `uuid_no`: 고유 식별자
- `robot_no`: 로봇 번호
- `status`: 로봇 상태
- `battery`: 배터리 잔량
- `pos_x`, `pos_y`: 위치 좌표
- `report_time`: 리포트 시간

### InventoryInfo
- `uuid_no`: 고유 식별자
- `inventory`: 재고 정보
- `sku`: 상품 코드
- `pre_qty`, `new_qty`: 수량 정보
- `report_time`: 리포트 시간

### PodInfo
- `uuid_no`: 고유 식별자
- `pod_id`: POD ID
- `pod_face`: POD 면
- `location`: 위치 정보
- `report_time`: 리포트 시간

## 성능 최적화

### 캐시 전략
- **메모리 캐시**: 처리된 데이터 ID 캐싱
- **LRU 정리**: 10,000개 이상 시 자동 정리
- **중복 방지**: UUID + report_time 기반 중복 체크

### 스케줄링 최적화
- **초기 로드**: 애플리케이션 시작 시 1회만
- **증분 처리**: 변경된 데이터만 처리
- **배치 크기**: 100개 단위로 처리

## 모니터링

### ETL 통계
- 총 처리 레코드 수
- 성공한 레코드 수
- 평균 처리 시간
- 마지막 실행 시간

### 시스템 상태
- 데이터베이스 연결 상태
- 테이블 존재 여부
- 캐시 사용량
- Kafka 메시지 처리 상태

## 개발 환경

- **Java**: 21
- **Spring Boot**: 3.5.3
- **데이터베이스**: SQL Server, PostgreSQL
- **메시징**: Apache Kafka
- **빌드 도구**: Gradle

## 실행 방법

```bash
# 프로젝트 클론
git clone <repository-url>
cd WCS_DataStream

# 애플리케이션 실행
./gradlew bootRun

# 또는 빌드 후 실행
./gradlew build
java -jar build/libs/WCS_DataStream-*.jar
```

## API 테스트

### ETL 상태 확인
```bash
curl http://localhost:8081/api/etl/status
```

### 수동 ETL 실행
```bash
curl -X POST http://localhost:8081/api/etl/execute
```

### WCS 데이터 조회
```bash
curl http://localhost:8081/api/etl/wcs/data
```

## 주의사항

1. **데이터베이스 연결**: WCS DB와 PostgreSQL이 모두 실행 중이어야 함
2. **Kafka 서버**: Kafka 브로커가 실행 중이어야 함
3. **메모리 관리**: 캐시 크기가 10,000개를 초과하면 자동 정리됨
4. **스케줄링**: 5초 간격으로 실행되므로 시스템 리소스 고려 필요

## 라이센스

AGV Monitoring System - Version 2.0 