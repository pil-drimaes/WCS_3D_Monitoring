-- MSSQL 데이터 업데이트 테스트 스크립트
-- 1. 새로운 데이터 1개씩 삽입
-- 2. 기존 데이터 3-5개 업데이트

USE cdc_test;
GO

-- 1. 새로운 데이터 삽입 (각 테이블에 1개씩)

-- robot_info에 새로운 로봇 추가
INSERT INTO robot_info (uuid_no, robot_no, map_code, zone_code, status, manual, loaders, report_time, battery, node_id, pos_x, pos_y, speed, task_id, next_target, pod_id) VALUES
('550e8400-e29b-41d4-a716-446655440020', 'ROBOT_021', 'MAP_008', 'ZONE_2024Q2', 1, 0, 'Loader41,Loader42', 1754584750000, 88.3000, 'Node_143', 163.0000, 464.0000, 1.2000, 'TASK_021', 'Target_Node_U', 'POD021');

-- inventory_info에 새로운 재고 추가
INSERT INTO inventory_info (uuid_no, inventory, batch_num, unitload, sku, pre_qty, new_qty, origin_order, status, report_time) VALUES
('f48ac10b-58cc-4372-a567-0e02b2c3d489', 'WH-F01-20-3', '20250615-BATCH11', 'UL011', 'SKU-8859476335', 170, 155, 'SO-20250615-0022', 0, 1754584751000);

-- pod_info에 새로운 POD 추가
INSERT INTO pod_info (uuid_no, pod_id, pod_face, location, report_time) VALUES
('550e8400-e29b-41d4-a716-446655440021', 'POD011', 'D2', 133, 1754584752000);

-- 2. 기존 데이터 업데이트 (3-5개씩)

-- robot_info 데이터 업데이트 (위치, 배터리, 상태 변경)
UPDATE robot_info 
SET pos_x = 200.0000, pos_y = 500.0000, battery = 75.5000, status = 2, report_time = 1754584753000
WHERE robot_no = 'ROBOT_001';

UPDATE robot_info 
SET pos_x = 210.0000, pos_y = 510.0000, battery = 82.3000, speed = 1.8000, report_time = 1754584754000
WHERE robot_no = 'ROBOT_005';

UPDATE robot_info 
SET pos_x = 220.0000, pos_y = 520.0000, battery = 91.7000, task_id = 'TASK_999', report_time = 1754584755000
WHERE robot_no = 'ROBOT_010';

UPDATE robot_info 
SET pos_x = 230.0000, pos_y = 530.0000, battery = 68.9000, status = 3, report_time = 1754584756000
WHERE robot_no = 'ROBOT_015';

UPDATE robot_info 
SET pos_x = 240.0000, pos_y = 540.0000, battery = 79.2000, next_target = 'Target_Node_ZZZ', report_time = 1754584757000
WHERE robot_no = 'ROBOT_020';

-- inventory_info 데이터 업데이트 (수량, 상태 변경)
UPDATE inventory_info 
SET new_qty = 120, status = 1, report_time = 1754584758000
WHERE inventory = 'WH-A01-12-3';

UPDATE inventory_info 
SET new_qty = 175, status = 0, report_time = 1754584759000
WHERE inventory = 'WH-B01-08-2';

UPDATE inventory_info 
SET new_qty = 70, status = 1, report_time = 1754584760000
WHERE inventory = 'WH-C01-14-9';

UPDATE inventory_info 
SET new_qty = 155, status = 0, report_time = 1754584761000
WHERE inventory = 'WH-D01-09-4';

UPDATE inventory_info 
SET new_qty = 165, status = 1, report_time = 1754584762000
WHERE inventory = 'WH-E01-16-6';

-- pod_info 데이터 업데이트 (위치, 상태 변경)
UPDATE pod_info 
SET location = 200, report_time = 1754584763000
WHERE pod_id = 'POD001';

UPDATE pod_info 
SET location = 210, report_time = 1754584764000
WHERE pod_id = 'POD005';

UPDATE pod_info 
SET location = 220, report_time = 1754584765000
WHERE pod_id = 'POD010';

-- 3. 업데이트된 데이터 확인

-- robot_info 업데이트 확인
SELECT 'Updated ROBOT_001' as info, robot_no, pos_x, pos_y, battery, status, report_time FROM robot_info WHERE robot_no = 'ROBOT_001'
UNION ALL
SELECT 'Updated ROBOT_005' as info, robot_no, pos_x, pos_y, battery, speed, report_time FROM robot_info WHERE robot_no = 'ROBOT_005'
UNION ALL
SELECT 'Updated ROBOT_010' as info, robot_no, pos_x, pos_y, battery, task_id, report_time FROM robot_info WHERE robot_no = 'ROBOT_010'
UNION ALL
SELECT 'Updated ROBOT_015' as info, robot_no, pos_x, pos_y, battery, status, report_time FROM robot_info WHERE robot_no = 'ROBOT_015'
UNION ALL
SELECT 'Updated ROBOT_020' as info, robot_no, pos_x, pos_y, battery, next_target, report_time FROM robot_info WHERE robot_no = 'ROBOT_020';

-- inventory_info 업데이트 확인
SELECT 'Updated WH-A01-12-3' as info, inventory, new_qty, status, report_time FROM inventory_info WHERE inventory = 'WH-A01-12-3'
UNION ALL
SELECT 'Updated WH-B01-08-2' as info, inventory, new_qty, status, report_time FROM inventory_info WHERE inventory = 'WH-B01-08-2'
UNION ALL
SELECT 'Updated WH-C01-14-9' as info, inventory, new_qty, status, report_time FROM inventory_info WHERE inventory = 'WH-C01-14-9'
UNION ALL
SELECT 'Updated WH-D01-09-4' as info, inventory, new_qty, status, report_time FROM inventory_info WHERE inventory = 'WH-D01-09-4'
UNION ALL
SELECT 'Updated WH-E01-16-6' as info, inventory, new_qty, status, report_time FROM inventory_info WHERE inventory = 'WH-E01-16-6';

-- pod_info 업데이트 확인
SELECT 'Updated POD001' as info, pod_id, location, report_time FROM pod_info WHERE pod_id = 'POD001'
UNION ALL
SELECT 'Updated POD005' as info, pod_id, location, report_time FROM pod_info WHERE pod_id = 'POD005'
UNION ALL
SELECT 'Updated POD010' as info, pod_id, location, report_time FROM pod_info WHERE pod_id = 'POD010';

-- 4. 전체 데이터 개수 확인
SELECT 'robot_info' as table_name, COUNT(*) as record_count FROM robot_info
UNION ALL
SELECT 'inventory_info' as table_name, COUNT(*) as record_count FROM inventory_info
UNION ALL
SELECT 'pod_info' as table_name, COUNT(*) as record_count FROM pod_info;

-- 5. 최근 변경된 데이터 확인 (report_time 기준)
SELECT 'Recent Changes' as info, 'robot_info' as table_name, robot_no as id, report_time FROM robot_info WHERE report_time >= 1754584750000
UNION ALL
SELECT 'Recent Changes' as info, 'inventory_info' as table_name, inventory as id, report_time FROM inventory_info WHERE report_time >= 1754584750000
UNION ALL
SELECT 'Recent Changes' as info, 'pod_info' as table_name, pod_id as id, report_time FROM pod_info WHERE report_time >= 1754584750000
ORDER BY report_time DESC; 