# PostgreSQL + Kafka 설정 가이드

## 🚀 **1. Docker Compose로 인프라 실행**

### **1-1. 모든 서비스 시작**
```bash
# 프로젝트 루트 디렉토리에서 실행
docker-compose up -d

# 로그 확인
docker-compose logs -f
```

### **1-2. 개별 서비스 확인**
```bash
# PostgreSQL 상태 확인
docker exec -it postgres-cdcqueue psql -U cdcuser -d cdcqueue -c "\l"

# Kafka 토픽 생성 (필요시)
docker exec -it kafka kafka-topics --create --topic agv-data-events --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
docker exec -it kafka kafka-topics --create --topic etl-status-events --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1
docker exec -it kafka kafka-topics --create --topic error-events --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1

# 토픽 목록 확인
docker exec -it kafka kafka-topics --list --bootstrap-server localhost:9092
```

---

## 🗄️ **2. PostgreSQL 스키마 설정**

### **2-1. 스키마 자동 생성**
Docker Compose 실행 시 `postgresql_schema.sql`이 자동으로 실행됩니다.

### **2-2. 수동으로 스키마 생성 (필요시)**
```bash
# PostgreSQL에 직접 연결
docker exec -it postgres-cdcqueue psql -U cdcuser -d cdcqueue

# 스키마 파일 실행
\i /docker-entrypoint-initdb.d/01-schema.sql

# 테이블 확인
\dt

# 샘플 데이터 확인
SELECT * FROM agv_data LIMIT 5;
```

---

## 📊 **3. 관리 도구 접속**

### **3-1. pgAdmin (PostgreSQL 관리)**
- URL: http://localhost:5050
- Email: admin@admin.com
- Password: admin

**서버 연결 설정:**
- Host: postgres-cdcqueue
- Port: 5432
- Database: cdcqueue
- Username: cdcuser
- Password: cdcpassword

### **3-2. Kafka UI (Kafka 관리)**
- URL: http://localhost:8080
- 클러스터: local
- Bootstrap Servers: kafka:29092

---

## ⚙️ **4. 애플리케이션 설정**

### **4-1. application.properties 확인**
```properties
# PostgreSQL 연결 설정
spring.datasource.postgresql.url=jdbc:postgresql://localhost:5432/cdcqueue
spring.datasource.postgresql.username=cdcuser
spring.datasource.postgresql.password=cdcpassword

# Kafka 설정
spring.kafka.bootstrap-servers=localhost:9092
kafka.topic.agv-data=agv-data-events
kafka.topic.etl-status=etl-status-events
kafka.topic.error-events=error-events
```

### **4-2. 애플리케이션 실행**
```bash
# Gradle 빌드
./gradlew build

# 애플리케이션 실행
./gradlew bootRun
```

---

## 🔍 **5. 테스트 및 모니터링**

### **5-1. PostgreSQL 데이터 확인**
```sql
-- AGV 데이터 확인
SELECT robot_no, status, battery, pos_x, pos_y, updated_at 
FROM agv_data 
ORDER BY updated_at DESC 
LIMIT 10;

-- ETL 처리 이력 확인
SELECT batch_id, processed_count, success_count, processing_time_ms, status 
FROM etl_processing_history 
ORDER BY start_time DESC 
LIMIT 10;

-- Kafka 메시지 이력 확인
SELECT topic, status, created_at 
FROM kafka_message_history 
ORDER BY created_at DESC 
LIMIT 10;
```

### **5-2. Kafka 메시지 확인**
```bash
# AGV 데이터 토픽 메시지 확인
docker exec -it kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic agv-data-events --from-beginning

# ETL 상태 토픽 메시지 확인
docker exec -it kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic etl-status-events --from-beginning
```

---

## 🛠️ **6. 문제 해결**

### **6-1. PostgreSQL 연결 문제**
```bash
# 컨테이너 상태 확인
docker ps | grep postgres

# 로그 확인
docker logs postgres-cdcqueue

# 네트워크 확인
docker network ls
docker network inspect cdcqueue_cdc-network
```

### **6-2. Kafka 연결 문제**
```bash
# Kafka 컨테이너 상태 확인
docker ps | grep kafka

# Zookeeper 연결 확인
docker exec -it kafka kafka-topics --list --bootstrap-server localhost:9092

# 로그 확인
docker logs kafka
```

### **6-3. 애플리케이션 로그 확인**
```bash
# 애플리케이션 실행 시 로그 확인
./gradlew bootRun --debug

# 또는 로그 파일 확인
tail -f logs/application.log
```

---

## 📈 **7. 성능 모니터링**

### **7-1. PostgreSQL 성능 확인**
```sql
-- 활성 연결 수 확인
SELECT count(*) FROM pg_stat_activity;

-- 테이블 크기 확인
SELECT 
    schemaname,
    tablename,
    attname,
    n_distinct,
    correlation
FROM pg_stats 
WHERE tablename = 'agv_data';

-- 인덱스 사용률 확인
SELECT 
    schemaname,
    tablename,
    indexname,
    idx_scan,
    idx_tup_read,
    idx_tup_fetch
FROM pg_stat_user_indexes 
WHERE tablename = 'agv_data';
```

### **7-2. Kafka 성능 확인**
```bash
# 토픽 상세 정보 확인
docker exec -it kafka kafka-topics --describe --topic agv-data-events --bootstrap-server localhost:9092

# 컨슈머 그룹 확인
docker exec -it kafka kafka-consumer-groups --bootstrap-server localhost:9092 --list
```

---

## 🔄 **8. 데이터 흐름**

### **8-1. 전체 데이터 흐름**
```
MSSQL (WCS DB) → ETL Engine → Kafka → PostgreSQL
     ↓              ↓           ↓         ↓
   읽기 전용    변화 감지    메시지 큐   실제 저장
```

### **8-2. 각 단계별 역할**
1. **MSSQL (WCS DB)**: 원본 AGV 데이터 (읽기 전용)
2. **ETL Engine**: 데이터 변화 감지 및 변환
3. **Kafka**: 메시지 큐 (비동기 처리)
4. **PostgreSQL**: 실제 운영 데이터 저장

---

## ✅ **9. 설정 완료 체크리스트**

- [ ] Docker Compose 실행 완료
- [ ] PostgreSQL 스키마 생성 완료
- [ ] Kafka 토픽 생성 완료
- [ ] 애플리케이션 설정 완료
- [ ] 연결 테스트 완료
- [ ] 데이터 흐름 테스트 완료

**모든 설정이 완료되면 ETL 시스템이 MSSQL에서 데이터를 읽어서 Kafka를 통해 PostgreSQL에 저장하는 완전한 데이터 파이프라인이 구축됩니다!** 