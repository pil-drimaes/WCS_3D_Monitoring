-- WCS_DB 데이터 업데이트 스크립트
-- robot_info, inventory_info, pod_info의 기존 데이터를 업데이트

USE cdc_test;

-- 현재 시간을 기준으로 report_time 설정
DECLARE @currentTime BIGINT = CAST(DATEDIFF_BIG(millisecond, '1970-01-01', GETDATE()) AS BIGINT);

-- 1. robot_info 테이블의 기존 데이터 5개 업데이트
UPDATE robot_info 
SET pos_x = 120.0, 
    pos_y = 220.0, 
    battery = 82.5, 
    status = 2, 
    report_time = @currentTime
WHERE robot_no = 'ROBOT_001';

UPDATE robot_info 
SET pos_x = 160.0, 
    pos_y = 260.0, 
    battery = 88.3, 
    status = 3, 
    report_time = @currentTime
WHERE robot_no = 'ROBOT_002';

UPDATE robot_info 
SET pos_x = 210.0, 
    pos_y = 310.0, 
    battery = 79.9, 
    status = 1, 
    report_time = @currentTime
WHERE robot_no = 'ROBOT_003';

UPDATE robot_info 
SET pos_x = 260.0, 
    pos_y = 360.0, 
    battery = 96.1, 
    status = 4, 
    report_time = @currentTime
WHERE robot_no = 'ROBOT_004';

UPDATE robot_info 
SET pos_x = 310.0, 
    pos_y = 410.0, 
    battery = 89.7, 
    status = 2, 
    report_time = @currentTime
WHERE robot_no = 'ROBOT_005';

-- 2. inventory_info 테이블의 기존 데이터 5개 업데이트
UPDATE inventory_info 
SET pre_qty = 95, 
    new_qty = 90, 
    status = 2, 
    report_time = @currentTime
WHERE inventory = 'INV_001';

UPDATE inventory_info 
SET pre_qty = 140, 
    new_qty = 135, 
    status = 2, 
    report_time = @currentTime
WHERE inventory = 'INV_002';

UPDATE inventory_info 
SET pre_qty = 190, 
    new_qty = 185, 
    status = 3, 
    report_time = @currentTime
WHERE inventory = 'INV_003';

UPDATE inventory_info 
SET pre_qty = 70, 
    new_qty = 65, 
    status = 2, 
    report_time = @currentTime
WHERE inventory = 'INV_004';

UPDATE inventory_info 
SET pre_qty = 115, 
    new_qty = 110, 
    status = 2, 
    report_time = @currentTime
WHERE inventory = 'INV_005';

-- 3. pod_info 테이블의 기존 데이터 5개 업데이트
UPDATE pod_info 
SET location = 'LOC_001_UPDATED', 
    report_time = @currentTime
WHERE pod_id = 'POD_001';

UPDATE pod_info 
SET location = 'LOC_002_UPDATED', 
    report_time = @currentTime
WHERE pod_id = 'POD_002';

UPDATE pod_info 
SET location = 'LOC_003_UPDATED', 
    report_time = @currentTime
WHERE pod_id = 'POD_003';

UPDATE pod_info 
SET location = 'LOC_004_UPDATED', 
    report_time = @currentTime
WHERE pod_id = 'POD_004';

UPDATE pod_info 
SET location = 'LOC_005_UPDATED', 
    report_time = @currentTime
WHERE pod_id = 'POD_005';

-- 업데이트된 데이터 확인 (컬럼 수 맞춤)
SELECT 'robot_info' as table_name, robot_no, pos_x, pos_y, battery, status, report_time FROM robot_info WHERE robot_no IN ('ROBOT_001', 'ROBOT_002', 'ROBOT_003', 'ROBOT_004', 'ROBOT_005')
UNION ALL
SELECT 'inventory_info' as table_name, inventory, CAST(pre_qty AS VARCHAR(10)), CAST(new_qty AS VARCHAR(10)), CAST(status AS VARCHAR(10)), CAST(status AS VARCHAR(10)), CAST(report_time AS VARCHAR(20)) FROM inventory_info WHERE inventory IN ('INV_001', 'INV_002', 'INV_003', 'INV_004', 'INV_005')
UNION ALL
SELECT 'pod_info' as table_name, pod_id, location, location, location, location, CAST(report_time AS VARCHAR(20)) FROM pod_info WHERE pod_id IN ('POD_001', 'POD_002', 'POD_003', 'POD_004', 'POD_005');

-- 전체 데이터 수 확인
SELECT 'robot_info' as table_name, COUNT(*) as record_count FROM robot_info
UNION ALL
SELECT 'inventory_info' as table_name, COUNT(*) as record_count FROM inventory_info
UNION ALL
SELECT 'pod_info' as table_name, COUNT(*) as record_count FROM pod_info; 