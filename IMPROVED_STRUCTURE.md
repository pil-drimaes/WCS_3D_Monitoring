# CDC Queue 애플리케이션 구조 개선 방안

## 🚨 **현재 문제점**

### 1. 파일명과 역할 불일치
- `AgvDataETLEngine` → AGV만 처리하는데 이름이 혼란스러움
- `AgvDataTask` → Task라는 이름이 적절하지 않음
- `service` 폴더에 스케줄러 클래스들이 섞여있음

### 2. 사용하지 않는 의존성
- `SimpMessagingTemplate` → WebSocket 관련이지만 실제 사용 안함
- WebSocket 관련 설정 파일들이 남아있음

### 3. 폴더 구조 문제
- 스케줄러, ETL 엔진, 서비스가 한 폴더에 섞여있음

---

## 🏗️ **개선된 폴더 구조**

```
src/main/java/com/example/WCS_DataStream/
├── DataStreamApplication.java
├── common/                          # 공통 컴포넌트
│   ├── config/                      # 설정 클래스들
│   │   ├── DatabaseConfig.java
│   │   ├── PostgreSQLConfig.java
│   │   ├── KafkaConfig.java
│   │   ├── JacksonConfig.java
│   │   
│  
│       
├── etl                            # ETL 관련
│   ├── engine/                      # ETL 엔진들
│   │   ├── DataETLEngine.java      # 인터페이스
│   │   ├── AgvDataETLEngine.java   # AGV 전용 ETL
│   │   ├── InventoryDataETLEngine.java
│   │   └── PodDataETLEngine.java
│   ├── model/                       # 데이터 모델
│   │   ├── AgvData.java
│   │   ├── InventoryInfo.java
│   │   ├── PodInfo.java
│   │   └── CdcEvent.java
│   ├── pulling/                     # 데이터 풀링 엔진들
│   │   ├── DataPullingEngine.java  # 인터페이스
│   │   ├── HybridPullingEngine.java # 추상 클래스
│   │   ├── AgvHybridPullingEngine.java
│   │   ├── InventoryHybridPullingEngine.java
│   │   └── PodHybridPullingEngine.java
│   ├── scheduler/                   # 스케줄러들 (새로 생성)
│   │   ├── ETLTaskScheduler.java   # 통합 스케줄러
│   │   ├── AgvDataScheduler.java   # AGV 전용 스케줄러
│   │   ├── InventoryDataScheduler.java
│   │   └── PodDataScheduler.java
│   ├── service/                     # 데이터 서비스들
│   │   ├── AgvDataService.java     # MSSQL 데이터 접근
│   │   ├── InventoryDataService.java
│   │   ├── PodDataService.java
│   │   ├── PostgreSQLDataService.java # PostgreSQL 데이터 접근
│   │   ├── KafkaProducerService.java  # Kafka 메시지 전송
│   │   └── KafkaConsumerService.java  # Kafka 메시지 수신
│   ├── config/                      # ETL 설정
│   │   ├── ETLConfig.java
│   │   ├── PullingEngineConfig.java
│   │   └── DatabaseConnectionConfig.java
│   ├── exception/                   # 예외 클래스들
│   │   └── ETLEngineException.java
│   └── statistics/                  # 통계 관련
│       └── ETLStatistics.java
└── controller/                      # API 컨트롤러들
    ├── AgvDataController.java
    ├── InventoryDataController.java
    └── PodDataController.java
```

---

## 🔄 **파일명 변경 방안**

### 1. 스케줄러 클래스명 변경
```
현재: AgvDataTask.java
변경: AgvDataScheduler.java

현재: InventoryDataTask.java  
변경: InventoryDataScheduler.java

현재: PodDataTask.java
변경: PodDataScheduler.java
```

### 2. ETL 엔진 클래스명 변경 (선택사항)
```
현재: AgvDataETLEngine.java
변경: AgvETLEngine.java (더 간결하게)

현재: InventoryDataETLEngine.java
변경: InventoryETLEngine.java

현재: PodDataETLEngine.java  
변경: PodETLEngine.java
```

---

## 🧹 **정리해야 할 의존성**

### 1. 제거할 Import
```java
// AgvDataETLEngine.java에서 제거
import org.springframework.messaging.simp.SimpMessagingTemplate;

// 사용하지 않는 필드 제거
private final SimpMessagingTemplate messagingTemplate;
```

### 2. WebSocket 관련 코드 정리
```java
// WebSocket 메시지 전송 코드 제거
// messagingTemplate.convertAndSend() 호출 부분들 제거
```

---

## 🚀 **구현 우선순위**

### **Phase 1: 즉시 수정 (필수)**
1. 사용하지 않는 `SimpMessagingTemplate` Import 제거
2. WebSocket 관련 코드 정리
3. 0.1초 스케줄링 설정 적용

### **Phase 2: 구조 개선 (권장)**
1. `scheduler` 폴더 생성
2. Task 클래스들을 Scheduler로 이동 및 이름 변경
3. 폴더별 책임 명확화

### **Phase 3: 이름 변경 (선택)**
1. ETL 엔진 클래스명 간소화
2. 일관된 네이밍 컨벤션 적용

---

## 📝 **수정 예시 코드**

### **AgvDataScheduler.java (기존 AgvDataTask.java)**
```java
@Component
public class AgvDataScheduler {  // Task → Scheduler로 변경
    
    @Scheduled(fixedRate = 100)  // 0.1초마다 실행
    public void scheduleAgvDataProcessing() {  // 메서드명도 명확하게
        agvDataETLEngine.processData();
    }
}
```

### **AgvETLEngine.java (기존 AgvDataETLEngine.java)**
```java
@Component
public class AgvETLEngine implements DataETLEngine<AgvData> {  // 이름 간소화
    
    // SimpMessagingTemplate 관련 코드 제거
    // WebSocket 메시지 전송 코드 제거
    
    public void processData() {
        // ETL 로직만 남기고 정리
    }
}
```

---

## ✅ **기대 효과**

1. **명확한 책임 분리**: 각 폴더와 클래스의 역할이 명확해짐
2. **의존성 정리**: 사용하지 않는 WebSocket 관련 코드 제거
3. **일관된 네이밍**: Task → Scheduler로 더 적절한 이름 사용
4. **유지보수성 향상**: 관련 기능별로 폴더 구조화
5. **확장성 개선**: 새로운 ETL 엔진 추가 시 구조가 명확함

---

*이 구조 개선을 통해 코드의 가독성과 유지보수성을 크게 향상시킬 수 있습니다.* 