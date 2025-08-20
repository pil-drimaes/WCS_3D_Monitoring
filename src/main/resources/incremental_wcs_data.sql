-- WCS_DB 증분 데이터 삽입 스크립트
-- robot_info, inventory_info, pod_info에 각각 1개씩 새로운 데이터 삽입

USE cdc_test;

-- 현재 시간을 기준으로 report_time 설정
DECLARE @currentTime BIGINT = CAST(DATEDIFF_BIG(millisecond, '1970-01-01', GETDATE()) AS BIGINT);

-- 1. robot_info 테이블에 새로운 데이터 1개 삽입
INSERT INTO robot_info (uuid_no, robot_no, map_code, zone_code, status, manual, loaders, report_time, battery, node_id, pos_x, pos_y, speed, task_id, next_target, pod_id) VALUES
('robot-021', 'ROBOT_021', 'MAP_H', 'ZONE_11', 1, 0, 'LOADER_U', @currentTime, 89.2, 'NODE_021', 1100.0, 1200.0, 2.8, 'TASK_021', 'TARGET_U', 'POD_021');

-- 2. inventory_info 테이블에 새로운 데이터 1개 삽입
INSERT INTO inventory_info (uuid_no, inventory, batch_num, unitload, sku, pre_qty, new_qty, origin_order, status, report_time) VALUES
('inv-011', 'INV_011', 'BATCH_011', 'UNIT_011', 'SKU_011', 130, 125, 'ORDER_011', 1, @currentTime);

-- 3. pod_info 테이블에 새로운 데이터 1개 삽입
INSERT INTO pod_info (uuid_no, pod_id, pod_face, location, report_time) VALUES
('pod-011', 'POD_011', 'FACE_E', 'LOC_011', @currentTime);

-- 데이터 삽입 확인
SELECT 'robot_info' as table_name, COUNT(*) as record_count FROM robot_info
UNION ALL
SELECT 'inventory_info' as table_name, COUNT(*) as record_count FROM inventory_info
UNION ALL
SELECT 'pod_info' as table_name, COUNT(*) as record_count FROM pod_info;

-- 새로 삽입된 데이터 확인
SELECT 'robot_info' as table_name, robot_no, report_time FROM robot_info WHERE robot_no = 'ROBOT_021'
UNION ALL
SELECT 'inventory_info' as table_name, inventory, report_time FROM inventory_info WHERE inventory = 'INV_011'
UNION ALL
SELECT 'pod_info' as table_name, pod_id, report_time FROM pod_info WHERE pod_id = 'POD_011'; 