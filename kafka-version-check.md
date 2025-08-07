# Kafka 버전 호환성 확인 가이드

## 📋 **Spring Boot 3.5.3 Kafka 호환성**

### **1. 공식 호환성 매트릭스**

| Spring Boot 버전 | Spring Kafka 버전 | Kafka 클라이언트 버전 | 권장 Kafka 서버 버전 |
|-----------------|------------------|---------------------|-------------------|
| 3.5.x | 3.1.x | 3.6.x | 3.6.x |
| 3.4.x | 3.0.x | 3.5.x | 3.5.x |
| 3.3.x | 3.0.x | 3.4.x | 3.4.x |
| 3.2.x | 3.0.x | 3.4.x | 3.4.x |

### **2. 현재 프로젝트 설정**

```gradle
// build.gradle
plugins {
    id 'org.springframework.boot' version '3.5.3'
    id 'io.spring.dependency-management' version '1.1.7'
}

ext {
    set('springKafkaVersion', '3.1.1')  // Spring Boot 3.5.3과 호환
}

dependencies {
    implementation 'org.springframework.kafka:spring-kafka'
    // Kafka 클라이언트는 spring-kafka에 포함됨
}
```

### **3. Docker Compose Kafka 버전**

```yaml
# docker-compose.yml
services:
  kafka:
    image: confluentinc/cp-kafka:7.4.0  # Kafka 3.6.x
    # Spring Boot 3.5.3 + Spring Kafka 3.1.1과 호환
```

## 🔧 **호환성 확인 방법**

### **1. Spring Boot 의존성 확인**
```bash
# Spring Boot 버전 확인
./gradlew properties | grep springBootVersion

# Spring Kafka 버전 확인
./gradlew dependencies --configuration compileClasspath | grep spring-kafka
```

### **2. Kafka 클라이언트 버전 확인**
```bash
# Kafka 클라이언트 버전 확인
./gradlew dependencies --configuration compileClasspath | grep kafka-clients
```

### **3. Docker Kafka 버전 확인**
```bash
# Kafka 컨테이너 버전 확인
docker exec -it kafka kafka-topics --version
```

## ⚠️ **호환성 문제 해결**

### **1. 버전 불일치 시 해결 방법**

#### **Spring Kafka 버전 강제 지정**
```gradle
ext {
    set('springKafkaVersion', '3.1.1')
}

dependencies {
    implementation 'org.springframework.kafka:spring-kafka'
    implementation 'org.apache.kafka:kafka-clients:3.6.1'
}
```

#### **Docker Kafka 버전 변경**
```yaml
services:
  kafka:
    image: confluentinc/cp-kafka:7.4.0  # Kafka 3.6.x
    # 또는
    image: confluentinc/cp-kafka:7.3.0  # Kafka 3.5.x
```

### **2. 호환성 테스트**

#### **연결 테스트**
```java
@SpringBootTest
class KafkaConnectionTest {
    
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    
    @Test
    void testKafkaConnection() {
        // Kafka 연결 테스트
        kafkaTemplate.send("test-topic", "test-message");
    }
}
```

#### **토픽 생성 테스트**
```bash
# Kafka 토픽 생성 테스트
docker exec -it kafka kafka-topics --create \
    --topic test-topic \
    --bootstrap-server localhost:9092 \
    --partitions 1 \
    --replication-factor 1
```

## 📊 **권장 설정**

### **Spring Boot 3.5.3 + Kafka 3.6.x 조합**

```gradle
// build.gradle
plugins {
    id 'org.springframework.boot' version '3.5.3'
    id 'io.spring.dependency-management' version '1.1.7'
}

dependencies {
    implementation 'org.springframework.kafka:spring-kafka'
    // Spring Kafka 3.1.1이 자동으로 포함됨
}
```

```yaml
# docker-compose.yml
services:
  kafka:
    image: confluentinc/cp-kafka:7.4.0  # Kafka 3.6.x
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
```

## 🚀 **활성화 단계**

### **1단계: 의존성 활성화**
```gradle
// build.gradle에서 주석 해제
implementation 'org.springframework.kafka:spring-kafka'
```

### **2단계: 설정 활성화**
```java
// KafkaConfig.java에서 주석 해제
@Bean
public ProducerFactory<String, Object> producerFactory() {
    // 설정 코드
}
```

### **3단계: 서비스 활성화**
```java
// KafkaProducerService.java에서 Kafka 코드 활성화
```

### **4단계: Docker 실행**
```bash
docker-compose up -d kafka zookeeper
```

## ✅ **호환성 체크리스트**

- [ ] Spring Boot 3.5.3 확인
- [ ] Spring Kafka 3.1.1 확인
- [ ] Kafka 클라이언트 3.6.x 확인
- [ ] Docker Kafka 3.6.x 확인
- [ ] 연결 테스트 성공
- [ ] 토픽 생성 테스트 성공
- [ ] 메시지 전송 테스트 성공 