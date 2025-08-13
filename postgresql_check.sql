-- PostgreSQL 데이터 확인 스크립트
-- ETL 엔진이 MSSQL에서 데이터를 가져와서 PostgreSQL에 저장했는지 확인

-- 1. 테이블별 레코드 수 확인
SELECT 'robot_info' as table_name, COUNT(*) as record_count FROM robot_info
UNION ALL
SELECT 'inventory_info' as table_name, COUNT(*) as record_count FROM inventory_info
UNION ALL
SELECT 'pod_info' as table_name, COUNT(*) as record_count FROM pod_info;

-- 2. robot_info 테이블 데이터 확인 (최신순)
SELECT uuid_no, robot_no, map_code, zone_code, status, manual, 
       loaders, report_time, battery, node_id, pos_x, pos_y, 
       speed, task_id, next_target, pod_id
FROM robot_info 
ORDER BY report_time DESC 
LIMIT 10;

-- 3. inventory_info 테이블 데이터 확인 (최신순)
SELECT uuid_no, inventory, batch_num, unitload, sku, pre_qty, new_qty, 
       origin_order, status, report_time
FROM inventory_info 
ORDER BY report_time DESC 
LIMIT 10;

-- 4. pod_info 테이블 데이터 확인 (최신순)
SELECT uuid_no, pod_id, pod_face, location, report_time
FROM pod_info 
ORDER BY report_time DESC 
LIMIT 10;

-- 5. 최근 변경된 데이터 확인 (report_time 기준)
SELECT 'Recent Changes' as info, 'robot_info' as table_name, robot_no as id, 
       pos_x, pos_y, battery, status, report_time 
FROM robot_info 
WHERE report_time >= 1754584750000
ORDER BY report_time DESC;

-- 6. 특정 로봇의 위치 변화 확인
SELECT robot_no, pos_x, pos_y, battery, status, report_time
FROM robot_info 
WHERE robot_no IN ('ROBOT_001', 'ROBOT_005', 'ROBOT_010', 'ROBOT_015', 'ROBOT_020')
ORDER BY robot_no, report_time DESC;

-- 7. ETL 처리 이력 확인 (있는 경우)
SELECT * FROM etl_processing_history 
ORDER BY end_time DESC 
LIMIT 10;

-- 8. 데이터 품질 확인 (NULL 값 체크)
SELECT 'robot_info' as table_name, 
       COUNT(*) as total_records,
       COUNT(CASE WHEN robot_no IS NULL THEN 1 END) as null_robot_no,
       COUNT(CASE WHEN pos_x IS NULL THEN 1 END) as null_pos_x,
       COUNT(CASE WHEN pos_y IS NULL THEN 1 END) as null_pos_y,
       COUNT(CASE WHEN battery IS NULL THEN 1 END) as null_battery
FROM robot_info
UNION ALL
SELECT 'inventory_info' as table_name,
       COUNT(*) as total_records,
       COUNT(CASE WHEN inventory IS NULL THEN 1 END) as null_inventory,
       COUNT(CASE WHEN sku IS NULL THEN 1 END) as null_sku,
       COUNT(CASE WHEN new_qty IS NULL THEN 1 END) as null_new_qty,
       COUNT(CASE WHEN status IS NULL THEN 1 END) as null_status
FROM inventory_info
UNION ALL
SELECT 'pod_info' as table_name,
       COUNT(*) as total_records,
       COUNT(CASE WHEN pod_id IS NULL THEN 1 END) as null_pod_id,
       COUNT(CASE WHEN location IS NULL THEN 1 END) as null_location,
       COUNT(CASE WHEN report_time IS NULL THEN 1 END) as null_report_time,
       0 as dummy
FROM pod_info;

-- 9. 데이터 중복 확인
SELECT robot_no, COUNT(*) as duplicate_count
FROM robot_info 
GROUP BY robot_no 
HAVING COUNT(*) > 1
ORDER BY duplicate_count DESC;

-- 10. 최신 데이터와 이전 데이터 비교 (변경 감지 확인)
WITH latest_data AS (
    SELECT robot_no, pos_x, pos_y, battery, status, report_time,
           ROW_NUMBER() OVER (PARTITION BY robot_no ORDER BY report_time DESC) as rn
    FROM robot_info
)
SELECT l.robot_no, l.pos_x as latest_pos_x, l.pos_y as latest_pos_y, 
       l.battery as latest_battery, l.status as latest_status, l.report_time as latest_time
FROM latest_data l
WHERE l.rn = 1
ORDER BY l.report_time DESC
LIMIT 5; 