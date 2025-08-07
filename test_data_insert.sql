-- ETL 엔진 테스트를 위한 데이터 삽입 스크립트
-- 실제 WCS DB 스키마에 맞춰 업데이트됨
-- AGV가 실제로 움직이는 것처럼 데이터를 점진적으로 업데이트

USE cdc_test;
GO

-- agv_data 테이블 생성 (없는 경우)
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='agv_data' AND xtype='U')
BEGIN
    CREATE TABLE agv_data (
        uuid_no VARCHAR(50) PRIMARY KEY,
        robot_no VARCHAR(20) NOT NULL,
        map_code VARCHAR(20),
        zone_code VARCHAR(20),
        status INT,
        manual BIT DEFAULT 0,
        loaders VARCHAR(50),
        report_time BIGINT,
        battery DECIMAL(13,4),
        node_id VARCHAR(50),
        pos_x DECIMAL(13,4),
        pos_y DECIMAL(13,4),
        speed DECIMAL(13,4),
        task_id VARCHAR(50),
        next_target VARCHAR(50),
        pod_id VARCHAR(50)
    );
    PRINT 'agv_data 테이블이 생성되었습니다.';
END

-- 기존 데이터 삭제 (테스트용)
DELETE FROM agv_data;
PRINT '기존 테스트 데이터를 삭제했습니다.';

-- 1. 초기 AGV 데이터 삽입 (20개 로봇)
PRINT '=== 1. 초기 AGV 데이터 삽입 (20개 로봇) ===';
DECLARE @currentTime BIGINT = CAST(DATEDIFF(SECOND, '1970-01-01', GETDATE()) AS BIGINT) * 1000;

-- 1-1. 작업 중인 로봇들 (상태: 1 - Working)
INSERT INTO agv_data (uuid_no, robot_no, map_code, zone_code, status, manual, loaders, report_time, battery, node_id, pos_x, pos_y, speed, task_id, next_target, pod_id)
VALUES 
    (NEWID(), 'ROBOT_001', 'MAP_WAREHOUSE_A', 'ZONE_PICKING', 1, 0, 'Loader1,Loader2', @currentTime, 95.5000, 'Node_A_001', 100.5000, 200.3000, 1.2345, 'TASK_PICK_001', 'Target_Node_A_002', 'POD_A001'),
    (NEWID(), 'ROBOT_002', 'MAP_WAREHOUSE_A', 'ZONE_PICKING', 1, 0, 'Loader2,Loader3', @currentTime, 87.2000, 'Node_A_003', 150.2000, 250.7000, 0.9876, 'TASK_PICK_002', 'Target_Node_A_004', 'POD_A002'),
    (NEWID(), 'ROBOT_003', 'MAP_WAREHOUSE_A', 'ZONE_PICKING', 1, 0, 'Loader1,Loader3', @currentTime, 92.1000, 'Node_A_005', 200.8000, 300.1000, 1.5000, 'TASK_PICK_003', 'Target_Node_A_006', 'POD_A003'),
    (NEWID(), 'ROBOT_004', 'MAP_WAREHOUSE_A', 'ZONE_PICKING', 1, 0, 'Loader2', @currentTime, 94.2000, 'Node_A_007', 300.0000, 400.0000, 1.1000, 'TASK_PICK_004', 'Target_Node_A_008', 'POD_A004'),
    (NEWID(), 'ROBOT_005', 'MAP_WAREHOUSE_A', 'ZONE_PICKING', 1, 0, 'Loader1,Loader2,Loader3', @currentTime, 76.8000, 'Node_A_009', 350.5000, 450.2000, 0.8000, 'TASK_PICK_005', 'Target_Node_A_010', 'POD_A005');

-- 1-2. 대기 중인 로봇들 (상태: 2 - Idle)
INSERT INTO agv_data (uuid_no, robot_no, map_code, zone_code, status, manual, loaders, report_time, battery, node_id, pos_x, pos_y, speed, task_id, next_target, pod_id)
VALUES 
    (NEWID(), 'ROBOT_006', 'MAP_WAREHOUSE_A', 'ZONE_CHARGING', 2, 0, 'Loader1', @currentTime, 45.3000, 'Node_A_011', 500.0000, 600.0000, 0.0000, NULL, 'Target_Node_A_012', NULL),
    (NEWID(), 'ROBOT_007', 'MAP_WAREHOUSE_A', 'ZONE_CHARGING', 2, 0, 'Loader2', @currentTime, 52.7000, 'Node_A_013', 550.0000, 650.0000, 0.0000, NULL, 'Target_Node_A_014', NULL),
    (NEWID(), 'ROBOT_008', 'MAP_WAREHOUSE_A', 'ZONE_CHARGING', 2, 0, 'Loader3', @currentTime, 38.9000, 'Node_A_015', 600.0000, 700.0000, 0.0000, NULL, 'Target_Node_A_016', NULL),
    (NEWID(), 'ROBOT_009', 'MAP_WAREHOUSE_A', 'ZONE_CHARGING', 2, 0, 'Loader1,Loader2', @currentTime, 41.2000, 'Node_A_017', 650.0000, 750.0000, 0.0000, NULL, 'Target_Node_A_018', NULL),
    (NEWID(), 'ROBOT_010', 'MAP_WAREHOUSE_A', 'ZONE_CHARGING', 2, 0, 'Loader2,Loader3', @currentTime, 48.6000, 'Node_A_019', 700.0000, 800.0000, 0.0000, NULL, 'Target_Node_A_020', NULL);

-- 1-3. 오류 상태 로봇들 (상태: 3 - Error)
INSERT INTO agv_data (uuid_no, robot_no, map_code, zone_code, status, manual, loaders, report_time, battery, node_id, pos_x, pos_y, speed, task_id, next_target, pod_id)
VALUES 
    (NEWID(), 'ROBOT_011', 'MAP_WAREHOUSE_A', 'ZONE_MAINTENANCE', 3, 1, 'Loader1', @currentTime, 15.8000, 'Node_A_021', 750.0000, 850.0000, 0.0000, 'TASK_ERROR_001', 'Target_Node_A_022', 'POD_A006'),
    (NEWID(), 'ROBOT_012', 'MAP_WAREHOUSE_A', 'ZONE_MAINTENANCE', 3, 1, 'Loader2', @currentTime, 22.4000, 'Node_A_023', 800.0000, 900.0000, 0.0000, 'TASK_ERROR_002', 'Target_Node_A_024', 'POD_A007');

-- 1-4. 수동 모드 로봇들 (manual: 1)
INSERT INTO agv_data (uuid_no, robot_no, map_code, zone_code, status, manual, loaders, report_time, battery, node_id, pos_x, pos_y, speed, task_id, next_target, pod_id)
VALUES 
    (NEWID(), 'ROBOT_013', 'MAP_WAREHOUSE_A', 'ZONE_MANUAL', 1, 1, 'Loader1,Loader2,Loader3', @currentTime, 88.9000, 'Node_A_025', 850.0000, 950.0000, 0.5000, 'TASK_MANUAL_001', 'Target_Node_A_026', 'POD_A008'),
    (NEWID(), 'ROBOT_014', 'MAP_WAREHOUSE_A', 'ZONE_MANUAL', 1, 1, 'Loader1', @currentTime, 91.3000, 'Node_A_027', 900.0000, 1000.0000, 0.3000, 'TASK_MANUAL_002', 'Target_Node_A_028', 'POD_A009');

-- 1-5. 다른 맵에서 작업 중인 로봇들
INSERT INTO agv_data (uuid_no, robot_no, map_code, zone_code, status, manual, loaders, report_time, battery, node_id, pos_x, pos_y, speed, task_id, next_target, pod_id)
VALUES 
    (NEWID(), 'ROBOT_015', 'MAP_WAREHOUSE_B', 'ZONE_SORTING', 1, 0, 'Loader1,Loader2', @currentTime, 82.7000, 'Node_B_001', 1200.0000, 1300.0000, 1.8000, 'TASK_SORT_001', 'Target_Node_B_002', 'POD_B001'),
    (NEWID(), 'ROBOT_016', 'MAP_WAREHOUSE_B', 'ZONE_SORTING', 1, 0, 'Loader2,Loader3', @currentTime, 79.4000, 'Node_B_003', 1250.0000, 1350.0000, 1.6000, 'TASK_SORT_002', 'Target_Node_B_004', 'POD_B002'),
    (NEWID(), 'ROBOT_017', 'MAP_WAREHOUSE_B', 'ZONE_SORTING', 1, 0, 'Loader1,Loader3', @currentTime, 85.1000, 'Node_B_005', 1300.0000, 1400.0000, 1.9000, 'TASK_SORT_003', 'Target_Node_B_006', 'POD_B003');

-- 1-6. 극한 상황 테스트용 로봇들
INSERT INTO agv_data (uuid_no, robot_no, map_code, zone_code, status, manual, loaders, report_time, battery, node_id, pos_x, pos_y, speed, task_id, next_target, pod_id)
VALUES 
    (NEWID(), 'ROBOT_018', 'MAP_WAREHOUSE_C', 'ZONE_EXTREME', 1, 0, 'Loader1', @currentTime, 99.9999, 'Node_C_001', 999999.9999, -999999.9999, 5.0000, 'TASK_EXTREME_001', 'Target_Node_C_002', 'POD_C001'),
    (NEWID(), 'ROBOT_019', 'MAP_WAREHOUSE_C', 'ZONE_EXTREME', 1, 0, 'Loader2', @currentTime, 1.0000, 'Node_C_003', 0.0001, 0.0001, 0.1000, 'TASK_EXTREME_002', 'Target_Node_C_004', 'POD_C002'),
    (NEWID(), 'ROBOT_020', 'MAP_WAREHOUSE_C', 'ZONE_EXTREME', 2, 0, 'Loader3', @currentTime, 0.0001, 'Node_C_005', 500000.0000, 500000.0000, 0.0000, NULL, 'Target_Node_C_006', NULL);

PRINT '초기 20개 로봇 데이터 삽입 완료';

-- 2. AGV 움직임 시뮬레이션 (시간 간격을 두고 데이터 업데이트)
PRINT '=== 2. AGV 움직임 시뮬레이션 시작 ===';
DECLARE @currentTime BIGINT = CAST(DATEDIFF(SECOND, '1970-01-01', GETDATE()) AS BIGINT) * 1000;

-- 2-1. 5초 후: ROBOT_001, ROBOT_002 위치 이동 및 배터리 감소
WAITFOR DELAY '00:00:05';
PRINT '5초 후: ROBOT_001, ROBOT_002 이동 시작';
UPDATE agv_data 
SET 
    pos_x = pos_x + 15.5000, 
    pos_y = pos_y + 20.3000, 
    battery = battery - 2.1000, 
    report_time = @currentTime + 5000,
    speed = 1.5000,
    node_id = 'Node_A_002'
WHERE robot_no = 'ROBOT_001';

UPDATE agv_data 
SET 
    pos_x = pos_x + 12.2000, 
    pos_y = pos_y + 18.7000, 
    battery = battery - 1.8000, 
    report_time = @currentTime + 5000,
    speed = 1.2000,
    node_id = 'Node_A_004'
WHERE robot_no = 'ROBOT_002';

-- 2-2. 10초 후: ROBOT_006, ROBOT_007 충전 완료, 작업 시작
WAITFOR DELAY '00:00:05';
PRINT '10초 후: ROBOT_006, ROBOT_007 충전 완료, 작업 시작';
UPDATE agv_data 
SET 
    status = 1, 
    battery = 85.3000, 
    pos_x = pos_x + 25.0000, 
    pos_y = pos_y + 30.0000, 
    report_time = @currentTime + 10000,
    speed = 1.1000,
    task_id = 'TASK_PICK_006', 
    next_target = 'Target_Node_A_030', 
    pod_id = 'POD_A010',
    node_id = 'Node_A_030',
    zone_code = 'ZONE_PICKING'
WHERE robot_no = 'ROBOT_006';

UPDATE agv_data 
SET 
    status = 1, 
    battery = 92.7000, 
    pos_x = pos_x + 30.0000, 
    pos_y = pos_y + 35.0000, 
    report_time = @currentTime + 10000,
    speed = 1.3000,
    task_id = 'TASK_PICK_007', 
    next_target = 'Target_Node_A_031', 
    pod_id = 'POD_A011',
    node_id = 'Node_A_031',
    zone_code = 'ZONE_PICKING'
WHERE robot_no = 'ROBOT_007';

-- 2-3. 15초 후: ROBOT_003, ROBOT_004 작업 완료, 다음 위치로 이동
WAITFOR DELAY '00:00:05';
PRINT '15초 후: ROBOT_003, ROBOT_004 작업 완료, 다음 위치로 이동';
UPDATE agv_data 
SET 
    pos_x = pos_x + 22.8000, 
    pos_y = pos_y + 25.1000, 
    battery = battery - 3.2000, 
    report_time = @currentTime + 15000,
    speed = 1.8000,
    task_id = 'TASK_PICK_008', 
    next_target = 'Target_Node_A_032', 
    pod_id = 'POD_A012',
    node_id = 'Node_A_032'
WHERE robot_no = 'ROBOT_003';

UPDATE agv_data 
SET 
    pos_x = pos_x + 18.0000, 
    pos_y = pos_y + 22.0000, 
    battery = battery - 2.8000, 
    report_time = @currentTime + 15000,
    speed = 1.4000,
    task_id = 'TASK_PICK_009', 
    next_target = 'Target_Node_A_033', 
    pod_id = 'POD_A013',
    node_id = 'Node_A_033'
WHERE robot_no = 'ROBOT_004';

-- 2-4. 20초 후: ROBOT_011 오류 복구, 작업 재개
WAITFOR DELAY '00:00:05';
PRINT '20초 후: ROBOT_011 오류 복구, 작업 재개';
UPDATE agv_data 
SET 
    status = 1, 
    manual = 0, 
    battery = 65.8000, 
    pos_x = pos_x + 40.0000, 
    pos_y = pos_y + 45.0000, 
    report_time = @currentTime + 20000,
    speed = 0.8000,
    task_id = 'TASK_PICK_010', 
    next_target = 'Target_Node_A_034', 
    pod_id = 'POD_A014',
    node_id = 'Node_A_034',
    zone_code = 'ZONE_PICKING'
WHERE robot_no = 'ROBOT_011';

-- 2-5. 25초 후: ROBOT_008, ROBOT_009 충전 시작
WAITFOR DELAY '00:00:05';
PRINT '25초 후: ROBOT_008, ROBOT_009 충전 시작';
UPDATE agv_data 
SET 
    status = 2, 
    pos_x = 800.0000, 
    pos_y = 900.0000, 
    report_time = @currentTime + 25000,
    speed = 0.0000,
    task_id = NULL, 
    next_target = 'Target_Node_A_035', 
    pod_id = NULL,
    node_id = 'Node_A_035',
    zone_code = 'ZONE_CHARGING'
WHERE robot_no = 'ROBOT_008';

UPDATE agv_data 
SET 
    status = 2, 
    pos_x = 850.0000, 
    pos_y = 950.0000, 
    report_time = @currentTime + 25000,
    speed = 0.0000,
    task_id = NULL, 
    next_target = 'Target_Node_A_036', 
    pod_id = NULL,
    node_id = 'Node_A_036',
    zone_code = 'ZONE_CHARGING'
WHERE robot_no = 'ROBOT_009';

-- 2-6. 30초 후: ROBOT_015, ROBOT_016 맵 간 이동
WAITFOR DELAY '00:00:05';
PRINT '30초 후: ROBOT_015, ROBOT_016 맵 간 이동';
UPDATE agv_data 
SET 
    map_code = 'MAP_WAREHOUSE_A',
    zone_code = 'ZONE_PICKING',
    pos_x = 400.0000, 
    pos_y = 500.0000, 
    battery = battery - 5.2000, 
    report_time = @currentTime + 30000,
    speed = 2.1000,
    task_id = 'TASK_PICK_011', 
    next_target = 'Target_Node_A_037', 
    pod_id = 'POD_A015',
    node_id = 'Node_A_037'
WHERE robot_no = 'ROBOT_015';

UPDATE agv_data 
SET 
    map_code = 'MAP_WAREHOUSE_A',
    zone_code = 'ZONE_PICKING',
    pos_x = 450.0000, 
    pos_y = 550.0000, 
    battery = battery - 4.8000, 
    report_time = @currentTime + 30000,
    speed = 1.9000,
    task_id = 'TASK_PICK_012', 
    next_target = 'Target_Node_A_038', 
    pod_id = 'POD_A016',
    node_id = 'Node_A_038'
WHERE robot_no = 'ROBOT_016';

-- 2-7. 35초 후: ROBOT_013, ROBOT_014 수동 모드 해제, 자동 모드로 전환
WAITFOR DELAY '00:00:05';
PRINT '35초 후: ROBOT_013, ROBOT_014 수동 모드 해제, 자동 모드로 전환';
UPDATE agv_data 
SET 
    manual = 0, 
    pos_x = pos_x + 35.0000, 
    pos_y = pos_y + 40.0000, 
    battery = battery - 2.5000, 
    report_time = @currentTime + 35000,
    speed = 1.6000,
    task_id = 'TASK_PICK_013', 
    next_target = 'Target_Node_A_039', 
    pod_id = 'POD_A017',
    node_id = 'Node_A_039',
    zone_code = 'ZONE_PICKING'
WHERE robot_no = 'ROBOT_013';

UPDATE agv_data 
SET 
    manual = 0, 
    pos_x = pos_x + 30.0000, 
    pos_y = pos_y + 35.0000, 
    battery = battery - 2.2000, 
    report_time = @currentTime + 35000,
    speed = 1.4000,
    task_id = 'TASK_PICK_014', 
    next_target = 'Target_Node_A_040', 
    pod_id = 'POD_A018',
    node_id = 'Node_A_040',
    zone_code = 'ZONE_PICKING'
WHERE robot_no = 'ROBOT_014';

-- 2-8. 40초 후: ROBOT_005 배터리 부족, 충전소로 이동
WAITFOR DELAY '00:00:05';
PRINT '40초 후: ROBOT_005 배터리 부족, 충전소로 이동';
UPDATE agv_data 
SET 
    status = 2, 
    pos_x = 900.0000, 
    pos_y = 1000.0000, 
    battery = 15.8000, 
    report_time = @currentTime + 40000,
    speed = 0.5000,
    task_id = NULL, 
    next_target = 'Target_Node_A_041', 
    pod_id = NULL,
    node_id = 'Node_A_041',
    zone_code = 'ZONE_CHARGING'
WHERE robot_no = 'ROBOT_005';

-- 2-9. 45초 후: ROBOT_018, ROBOT_019 극한 상황에서 정상 범위로 복귀
WAITFOR DELAY '00:00:05';
PRINT '45초 후: ROBOT_018, ROBOT_019 극한 상황에서 정상 범위로 복귀';
UPDATE agv_data 
SET 
    pos_x = 100.0000, 
    pos_y = 200.0000, 
    battery = 85.5000, 
    report_time = @currentTime + 45000,
    speed = 1.2000,
    task_id = 'TASK_PICK_015', 
    next_target = 'Target_Node_A_042', 
    pod_id = 'POD_A019',
    node_id = 'Node_A_042',
    zone_code = 'ZONE_PICKING'
WHERE robot_no = 'ROBOT_018';

UPDATE agv_data 
SET 
    pos_x = 150.0000, 
    pos_y = 250.0000, 
    battery = 75.3000, 
    report_time = @currentTime + 45000,
    speed = 1.0000,
    task_id = 'TASK_PICK_016', 
    next_target = 'Target_Node_A_043', 
    pod_id = 'POD_A020',
    node_id = 'Node_A_043',
    zone_code = 'ZONE_PICKING'
WHERE robot_no = 'ROBOT_019';

-- 2-10. 50초 후: ROBOT_020 극한 배터리에서 충전 완료
WAITFOR DELAY '00:00:05';
PRINT '50초 후: ROBOT_020 극한 배터리에서 충전 완료';
UPDATE agv_data 
SET 
    status = 1, 
    pos_x = 200.0000, 
    pos_y = 300.0000, 
    battery = 95.0000, 
    report_time = @currentTime + 50000,
    speed = 1.5000,
    task_id = 'TASK_PICK_017', 
    next_target = 'Target_Node_A_044', 
    pod_id = 'POD_A021',
    node_id = 'Node_A_044',
    zone_code = 'ZONE_PICKING'
WHERE robot_no = 'ROBOT_020';

PRINT '=== AGV 움직임 시뮬레이션 완료 ===';

-- 3. 최종 데이터 확인
PRINT '=== 3. 최종 데이터 확인 ===';
SELECT 
    COUNT(*) as '총 레코드 수',
    COUNT(DISTINCT robot_no) as '고유 로봇 수',
    COUNT(CASE WHEN status = 1 THEN 1 END) as '작업 중',
    COUNT(CASE WHEN status = 2 THEN 1 END) as '대기 중',
    COUNT(CASE WHEN status = 3 THEN 1 END) as '오류 상태',
    COUNT(CASE WHEN manual = 1 THEN 1 END) as '수동 모드',
    MIN(report_time) as '최초 리포트',
    MAX(report_time) as '최신 리포트'
FROM agv_data;

-- 4. 맵별 로봇 분포 확인
PRINT '=== 4. 맵별 로봇 분포 ===';
SELECT 
    map_code,
    COUNT(*) as '로봇 수',
    COUNT(CASE WHEN status = 1 THEN 1 END) as '작업 중',
    COUNT(CASE WHEN status = 2 THEN 1 END) as '대기 중',
    COUNT(CASE WHEN status = 3 THEN 1 END) as '오류 상태'
FROM agv_data 
GROUP BY map_code
ORDER BY map_code;

-- 5. 최신 데이터 10개 확인 (시간순 정렬)
PRINT '=== 5. 최신 데이터 10개 (시간순 정렬) ===';
SELECT TOP 10 
    robot_no, 
    map_code, 
    zone_code, 
    status, 
    manual, 
    battery, 
    pos_x, 
    pos_y, 
    speed, 
    task_id, 
    pod_id,
    report_time
FROM agv_data 
ORDER BY report_time DESC;

-- 6. 변화 감지 테스트용 쿼리
PRINT '=== 6. 변화 감지 테스트용 쿼리 ===';
PRINT '다음 쿼리로 ETL Pulling이 제대로 작동하는지 확인하세요:';
PRINT 'SELECT * FROM agv_data WHERE report_time > [마지막 체크 시간] ORDER BY report_time DESC;';

PRINT '=== AGV 움직임 시뮬레이션 완료 ===';
PRINT 'ETL 엔진이 50초 동안의 AGV 움직임을 감지했는지 확인하세요!';
PRINT '실시간 모니터링: http://localhost:8080/realtime-monitor.html';
PRINT 'ETL 대시보드: http://localhost:8080/independent-etl-dashboard.html';

-- ===================================================================
-- 2번 부분만 따로 실행할 때 사용하는 스크립트 (변수 재선언 포함)
-- ===================================================================
/*
-- 2번 부분만 실행할 때는 이 부분을 주석 해제하고 사용하세요
DECLARE @currentTime BIGINT = CAST(DATEDIFF(SECOND, '1970-01-01', GETDATE()) AS BIGINT) * 1000;

-- 2. AGV 움직임 시뮬레이션 (시간 간격을 두고 데이터 업데이트)
PRINT '=== 2. AGV 움직임 시뮬레이션 시작 ===';

-- 2-1. 5초 후: ROBOT_001, ROBOT_002 위치 이동 및 배터리 감소
WAITFOR DELAY '00:00:05';
PRINT '5초 후: ROBOT_001, ROBOT_002 이동 시작';
UPDATE agv_data 
SET 
    pos_x = pos_x + 15.5000, 
    pos_y = pos_y + 20.3000, 
    battery = battery - 2.1000, 
    report_time = @currentTime + 5000,
    speed = 1.5000,
    node_id = 'Node_A_002'
WHERE robot_no = 'ROBOT_001';

-- ... 나머지 업데이트 쿼리들 ...
*/ 