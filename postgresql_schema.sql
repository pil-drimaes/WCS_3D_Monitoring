-- PostgreSQL 로봇 정보 및 재고/POD 데이터 테이블 스키마
-- 실제 운영 DB용 테이블 구조

-- 데이터베이스 생성 (필요시)
-- CREATE DATABASE agv_operational_db;


CREATE TABLE IF NOT EXISTS robot_info (
    id BIGSERIAL PRIMARY KEY,
    uuid_no VARCHAR(50) NOT NULL,
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

-- 재고 정보 테이블
CREATE TABLE IF NOT EXISTS inventory_info (
    id BIGSERIAL PRIMARY KEY,
    uuid_no VARCHAR(50) NOT NULL,
    inventory VARCHAR(50) NOT NULL,
    batch_num VARCHAR(50) NOT NULL,
    unitload VARCHAR(50) NOT NULL,
    sku VARCHAR(50) NOT NULL,
    pre_qty INTEGER,
    new_qty INTEGER,
    origin_order VARCHAR(50),
    status INTEGER NOT NULL DEFAULT 0,
    report_time BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- POD 정보 테이블
CREATE TABLE IF NOT EXISTS pod_info (
    id BIGSERIAL PRIMARY KEY,
    uuid_no VARCHAR(50) NOT NULL,
    pod_id VARCHAR(50) NOT NULL,
    pod_face VARCHAR(50) NOT NULL,
    location VARCHAR(50) NOT NULL,
    report_time BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 인덱스 생성 (성능 최적화)
-- robot_info 인덱스
CREATE INDEX IF NOT EXISTS idx_robot_info_robot_no ON robot_info(robot_no);
CREATE INDEX IF NOT EXISTS idx_robot_info_report_time ON robot_info(report_time);
CREATE INDEX IF NOT EXISTS idx_robot_info_status ON robot_info(status);
CREATE INDEX IF NOT EXISTS idx_robot_info_map_code ON robot_info(map_code);

-- inventory_info 인덱스
CREATE INDEX IF NOT EXISTS idx_inventory_info_inventory ON inventory_info(inventory);
CREATE INDEX IF NOT EXISTS idx_inventory_info_batch_num ON inventory_info(batch_num);
CREATE INDEX IF NOT EXISTS idx_inventory_info_sku ON inventory_info(sku);
CREATE INDEX IF NOT EXISTS idx_inventory_info_report_time ON inventory_info(report_time);
CREATE INDEX IF NOT EXISTS idx_inventory_info_status ON inventory_info(status);

-- pod_info 인덱스
CREATE INDEX IF NOT EXISTS idx_pod_info_pod_id ON pod_info(pod_id);
CREATE INDEX IF NOT EXISTS idx_pod_info_location ON pod_info(location);
CREATE INDEX IF NOT EXISTS idx_pod_info_report_time ON pod_info(report_time);

-- 업데이트 시간 자동 갱신을 위한 함수
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- 트리거 생성
CREATE TRIGGER update_robot_info_updated_at 
    BEFORE UPDATE ON robot_info 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_inventory_info_updated_at 
    BEFORE UPDATE ON inventory_info 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_pod_info_updated_at 
    BEFORE UPDATE ON pod_info 
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
    message_offset BIGINT,
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
AND table_name IN ('robot_info', 'inventory_info', 'pod_info', 'etl_processing_history', 'kafka_message_history')
ORDER BY table_name, ordinal_position;

-- 샘플 데이터 삽입 (테스트용)
-- 로봇 정보 샘플 데이터
INSERT INTO robot_info (uuid_no, robot_no, map_code, zone_code, status, manual, loaders, report_time, battery, node_id, pos_x, pos_y, speed, task_id, next_target, pod_id)
VALUES 
    ('550e8400-e29b-41d4-a716-446655440000', 'ROBOT_001', 'MAP_001', 'ZONE_2024Q3', 1, false, 'Loader1,Loader2', 1754533100000, 95.5000, 'Node_123', 123.4567, 456.7890, 1.2345, 'TASK_001', 'Target_Node_A', 'POD001'),
    ('550e8400-e29b-41d4-a716-446655440001', 'ROBOT_002', 'MAP_001', 'ZONE_2024Q3', 2, false, 'Loader1', 1754533101000, 87.2000, 'Node_124', 124.5678, 457.8901, 0.9876, 'TASK_002', 'Target_Node_B', 'POD002'),
    ('550e8400-e29b-41d4-a716-446655440002', 'ROBOT_003', 'MAP_002', 'ZONE_2024Q3', 1, true, 'Loader3', 1754533102000, 92.1000, 'Node_125', 125.6789, 458.9012, 1.5432, 'TASK_003', 'Target_Node_C', 'POD003')
ON CONFLICT (uuid_no) DO NOTHING;

-- 재고 정보 샘플 데이터
INSERT INTO inventory_info (uuid_no, inventory, batch_num, unitload, sku, pre_qty, new_qty, origin_order, status, report_time)
VALUES 
    ('f48ac10b-58cc-4372-a567-0e02b2c3d479', 'WH-A01-12-3', '20250615-BATCH01', 'UL001', 'SKU-8859476325', 150, 135, 'SO-20250615-0012', 0, 1754565354000),
    ('f48ac10b-58cc-4372-a567-0e02b2c3d480', 'WH-A01-12-4', '20250615-BATCH02', 'UL002', 'SKU-8859476326', 200, 180, 'SO-20250615-0013', 0, 1754565355000),
    ('f48ac10b-58cc-4372-a567-0e02b2c3d481', 'WH-A01-12-5', '20250615-BATCH03', 'UL003', 'SKU-8859476327', 100, 95, 'SO-20250615-0014', 1, 1754565356000)
ON CONFLICT (uuid_no) DO NOTHING;

-- POD 정보 샘플 데이터
INSERT INTO pod_info (uuid_no, pod_id, pod_face, location, report_time)
VALUES 
    ('550e8400-e29b-41d4-a716-446655440003', 'POD001', 'A1', '123', 1754565354000),
    ('550e8400-e29b-41d4-a716-446655440004', 'POD002', 'B2', '124', 1754565355000),
    ('550e8400-e29b-41d4-a716-446655440005', 'POD003', 'C3', '125', 1754565356000)
ON CONFLICT (uuid_no) DO NOTHING;

PRINT '테이블 스키마와 샘플 데이터가 성공적으로 생성되었습니다.'; 