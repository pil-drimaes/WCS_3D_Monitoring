-- PostgreSQL AGV 데이터 테이블 스키마
-- 실제 운영 DB용 테이블 구조

-- 데이터베이스 생성 (필요시)
-- CREATE DATABASE agv_operational_db;

-- AGV 데이터 테이블
CREATE TABLE IF NOT EXISTS agv_data (
    id BIGSERIAL PRIMARY KEY,
    uuid_no VARCHAR(50) UNIQUE NOT NULL,
    robot_no VARCHAR(20) NOT NULL,
    map_code VARCHAR(20),
    zone_code VARCHAR(20),
    status INTEGER,
    manual BOOLEAN DEFAULT FALSE,
    loaders VARCHAR(50),
    report_time BIGINT NOT NULL,
    battery DECIMAL(13,4),
    node_id VARCHAR(50),
    pos_x DECIMAL(13,4),
    pos_y DECIMAL(13,4),
    speed DECIMAL(13,4),
    task_id VARCHAR(50),
    next_target VARCHAR(50),
    pod_id VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 인덱스 생성 (성능 최적화)
CREATE INDEX IF NOT EXISTS idx_agv_data_robot_no ON agv_data(robot_no);
CREATE INDEX IF NOT EXISTS idx_agv_data_report_time ON agv_data(report_time);
CREATE INDEX IF NOT EXISTS idx_agv_data_status ON agv_data(status);
CREATE INDEX IF NOT EXISTS idx_agv_data_map_code ON agv_data(map_code);

-- 업데이트 시간 자동 갱신을 위한 함수
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- 트리거 생성
CREATE TRIGGER update_agv_data_updated_at 
    BEFORE UPDATE ON agv_data 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

-- ETL 처리 이력 테이블
CREATE TABLE IF NOT EXISTS etl_processing_history (
    id BIGSERIAL PRIMARY KEY,
    batch_id VARCHAR(50) NOT NULL,
    processed_count INTEGER DEFAULT 0,
    success_count INTEGER DEFAULT 0,
    failed_count INTEGER DEFAULT 0,
    skipped_count INTEGER DEFAULT 0,
    processing_time_ms BIGINT,
    start_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    end_time TIMESTAMP WITH TIME ZONE,
    status VARCHAR(20) DEFAULT 'RUNNING',
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 인덱스 생성
CREATE INDEX IF NOT EXISTS idx_etl_history_batch_id ON etl_processing_history(batch_id);
CREATE INDEX IF NOT EXISTS idx_etl_history_start_time ON etl_processing_history(start_time);
CREATE INDEX IF NOT EXISTS idx_etl_history_status ON etl_processing_history(status);

-- Kafka 메시지 이력 테이블 (디버깅용)
CREATE TABLE IF NOT EXISTS kafka_message_history (
    id BIGSERIAL PRIMARY KEY,
    topic VARCHAR(100) NOT NULL,
    partition INTEGER,
    offset BIGINT,
    key VARCHAR(100),
    message TEXT,
    status VARCHAR(20) DEFAULT 'SENT',
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 인덱스 생성
CREATE INDEX IF NOT EXISTS idx_kafka_history_topic ON kafka_message_history(topic);
CREATE INDEX IF NOT EXISTS idx_kafka_history_created_at ON kafka_message_history(created_at);

-- 테이블 생성 확인
SELECT table_name, column_name, data_type, is_nullable 
FROM information_schema.columns 
WHERE table_schema = 'public' 
AND table_name IN ('agv_data', 'etl_processing_history', 'kafka_message_history')
ORDER BY table_name, ordinal_position;

-- 샘플 데이터 삽입 (테스트용)
INSERT INTO agv_data (uuid_no, robot_no, map_code, zone_code, status, manual, loaders, report_time, battery, node_id, pos_x, pos_y, speed, task_id, next_target, pod_id)
VALUES 
    ('test-uuid-001', 'ROBOT_001', 'MAP_WAREHOUSE_A', 'ZONE_PICKING', 1, FALSE, 'Loader1,Loader2', 
     EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000, 95.5000, 'Node_A_001', 100.5000, 200.3000, 1.2345, 'TASK_PICK_001', 'Target_Node_A_002', 'POD_A001')
ON CONFLICT (uuid_no) DO NOTHING;

PRINT 'PostgreSQL 스키마 생성 완료!'; 