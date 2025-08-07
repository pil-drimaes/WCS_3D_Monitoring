# AGV ETL 모니터링 시스템

CDC 기능 없이 WCS DB에서 AGV 데이터를 실시간으로 모니터링하는 시스템입니다.

## 🏗️ 아키텍처

### 새로운 모듈식 구조

```
src/main/java/com/example/cdcqueue/
├── engine/           # 데이터 풀링 엔진
│   ├── DataPullingEngine.java      # 풀링 엔진 인터페이스
│   ├── HybridPullingEngine.java    # 하이브리드 풀링 구현
│   ├── PullingEngineConfig.java    # 풀링 엔진 설정
│   └── DatabaseConfig.java         # 데이터베이스 설정
├── parser/           # 데이터 파서
│   ├── DataParser.java             # 파서 인터페이스
│   ├── SqlDataParser.java          # SQL 데이터 파서
│   ├── ParserConfig.java           # 파서 설정
│   └── DataParseException.java     # 파서 예외
├── etl/              # ETL 엔진
│   ├── DataETLEngine.java          # ETL 엔진 인터페이스
│   ├── AgvDataETLEngine.java       # AGV 데이터 ETL 구현
│   ├── ETLConfig.java              # ETL 설정
│   ├── ETLStatistics.java          # ETL 통계
│   └── ETLEngineException.java     # ETL 예외
├── config/           # 설정
│   ├── ETLProperties.java          # ETL Properties
│   ├── DatabaseConfig.java         # 데이터베이스 설정
│   └── WebSocketConfig.java        # WebSocket 설정
├── service/          # 서비스
│   ├── NewAgvDataTask.java         # 새로운 AGV 데이터 태스크
│   └── WcsDatabaseService.java     # WCS DB 서비스
└── controller/       # 컨트롤러
    └── ETLController.java          # ETL API 컨트롤러
```

## 🚀 주요 기능

### 1. 하이브리드 풀링 방식
- **조건부 쿼리**: 마지막 체크 시간 이후의 변경된 데이터만 조회
- **전체 동기화**: 주기적으로 전체 데이터와 비교하여 정합성 보장
- **캐시 관리**: 중복 처리 방지를 위한 메모리 캐시

### 2. ETL 프로세스
- **Extract**: WCS DB에서 AGV 데이터 추출
- **Transform**: 데이터 검증 및 변환
- **Load**: 이벤트 큐에 적재하여 WebSocket으로 전송

### 3. 실시간 모니터링
- **WebSocket**: 실시간 이벤트 전송
- **대시보드**: ETL 상태 및 통계 모니터링
- **API**: RESTful API를 통한 상태 조회

## 📋 설정

### 1. 데이터베이스 설정

`application.properties`에서 WCS DB와 로컬 DB 설정:

```properties
# WCS DB 연결 설정
spring.datasource.wcs.url=jdbc:sqlserver://localhost:1433;databaseName=WCS_DB
spring.datasource.wcs.username=sa
spring.datasource.wcs.password=nice2025!

# 로컬 DB 연결 설정
spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=agv_etl_db
spring.datasource.username=sa
spring.datasource.password=nice2025!
```

### 2. ETL 설정

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

## 🖥️ 사용 방법

### 1. 애플리케이션 실행

```bash
# Gradle로 실행
./gradlew bootRun

# 또는 JAR 파일로 실행
java -jar build/libs/cdcQueue-0.0.1-SNAPSHOT.jar
```

### 2. 대시보드 접속

- **기존 CDC 대시보드**: http://localhost:8080/
- **새로운 ETL 대시보드**: http://localhost:8080/etl-dashboard.html

### 3. API 엔드포인트

#### ETL 상태 확인
```bash
GET /api/etl/status
```

#### ETL 통계 조회
```bash
GET /api/etl/statistics
```

#### WCS DB 데이터 조회
```bash
GET /api/etl/wcs/latest
GET /api/etl/wcs/all
```

#### 수동 ETL 실행
```bash
POST /api/etl/execute
```

## 📊 모니터링

### 1. ETL 엔진 상태
- 실시간 상태 모니터링
- 성공률, 처리 시간, 오류 횟수 통계
- WCS DB 연결 상태 확인

### 2. 데이터 처리 통계
- 총 처리 레코드 수
- 성공/실패/건너뛴 레코드 수
- 평균 처리 시간
- 재시도 횟수

### 3. 실시간 이벤트
- WebSocket을 통한 실시간 이벤트 수신
- AGV 위치 업데이트 실시간 표시
- 이벤트 히스토리 관리

## 🔧 개발 가이드

### 1. 새로운 풀링 전략 추가

```java
@Component
public class CustomPullingEngine implements DataPullingEngine {
    @Override
    public List<AgvData> pullNewData() {
        // 커스텀 풀링 로직 구현
    }
}
```

### 2. 새로운 파서 추가

```java
@Component
public class CustomDataParser implements DataParser<CustomData> {
    @Override
    public List<AgvData> parse(CustomData rawData) {
        // 커스텀 파싱 로직 구현
    }
}
```

### 3. 새로운 ETL 엔진 추가

```java
@Component
public class CustomETLEngine implements DataETLEngine {
    @Override
    public List<AgvData> executeETL() {
        // 커스텀 ETL 로직 구현
    }
}
```

## 🐛 문제 해결

### 1. WCS DB 연결 실패
- 데이터베이스 서버 상태 확인
- 연결 문자열 및 인증 정보 확인
- 방화벽 설정 확인

### 2. ETL 엔진 오류
- 로그 확인: `logging.level.com.example.cdcqueue.etl=DEBUG`
- 설정 파일 검증
- 데이터베이스 테이블 구조 확인

### 3. WebSocket 연결 실패
- 브라우저 콘솔 확인
- 서버 로그 확인
- 네트워크 연결 상태 확인

## 📝 변경 이력

### v2.0 (현재)
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