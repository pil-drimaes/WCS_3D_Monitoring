-- WCS 시스템의 agv_data 테이블에 테스트 데이터 생성 스크립트
-- MSSQL용

-- 기존 데이터 삭제 (선택사항)
-- DELETE FROM agv_data

-- 현재 시간을 밀리초 단위로 계산 (1970년 1월 1일부터)
DECLARE @currentTime BIGINT = CAST(DATEDIFF(SECOND, '1970-01-01', GETDATE()) AS BIGINT) * 1000;

-- 테스트 데이터 삽입
INSERT INTO agv_data (uuid_no, robot_no, map_code, zone_code, status, manual, loaders, report_time, battery, node_id, pos_x, pos_y, speed, task_id, next_target, pod_id) VALUES
('uuid-001', 'AGV01', 'MAP001', 'ZONE01', 1, 0, 'LOADER01', @currentTime - 60000, 85.5, 'NODE001', 78.4, 32.9, 2.5, 'TASK001', 'TARGET001', 'POD001'),
('uuid-002', 'AGV02', 'MAP001', 'ZONE02', 2, 0, 'LOADER02', @currentTime - 55000, 92.3, 'NODE002', 43.0, 83.4, 3.1, 'TASK002', 'TARGET002', 'POD002'),
('uuid-003', 'AGV03', 'MAP001', 'ZONE03', 1, 0, 'LOADER03', @currentTime - 50000, 78.9, 'NODE003', 92.1, 57.2, 2.8, 'TASK003', 'TARGET003', 'POD003'),
('uuid-004', 'AGV04', 'MAP001', 'ZONE04', 3, 0, 'LOADER04', @currentTime - 45000, 95.1, 'NODE004', 22.5, 10.2, 1.9, 'TASK004', 'TARGET004', 'POD004'),
('uuid-005', 'AGV05', 'MAP001', 'ZONE05', 1, 0, 'LOADER05', @currentTime - 40000, 88.7, 'NODE005', 23.2, 73.6, 2.2, 'TASK005', 'TARGET005', 'POD005'),
('uuid-006', 'AGV06', 'MAP001', 'ZONE06', 2, 0, 'LOADER06', @currentTime - 35000, 91.4, 'NODE006', 21.0, 73.9, 3.0, 'TASK006', 'TARGET006', 'POD006'),
('uuid-007', 'AGV07', 'MAP001', 'ZONE07', 1, 0, 'LOADER07', @currentTime - 30000, 87.2, 'NODE007', 73.4, 38.3, 2.7, 'TASK007', 'TARGET007', 'POD007'),
('uuid-008', 'AGV08', 'MAP001', 'ZONE08', 3, 0, 'LOADER08', @currentTime - 25000, 93.8, 'NODE008', 48.2, 59.0, 2.1, 'TASK008', 'TARGET008', 'POD008'),
('uuid-009', 'AGV09', 'MAP001', 'ZONE09', 1, 0, 'LOADER09', @currentTime - 20000, 89.6, 'NODE009', 6.5, 56.6, 2.9, 'TASK009', 'TARGET009', 'POD009'),
('uuid-010', 'AGV10', 'MAP001', 'ZONE10', 2, 0, 'LOADER10', @currentTime - 15000, 94.3, 'NODE010', 78.7, 24.6, 2.4, 'TASK010', 'TARGET010', 'POD010');

-- 추가 데이터 (더 최근 시간으로)
INSERT INTO agv_data (uuid_no, robot_no, map_code, zone_code, status, manual, loaders, report_time, battery, node_id, pos_x, pos_y, speed, task_id, next_target, pod_id) VALUES
('uuid-011', 'AGV01', 'MAP001', 'ZONE01', 1, 0, 'LOADER01', @currentTime - 10000, 84.2, 'NODE011', 31.4, 37.5, 2.6, 'TASK011', 'TARGET011', 'POD011'),
('uuid-012', 'AGV02', 'MAP001', 'ZONE02', 2, 0, 'LOADER02', @currentTime - 8000, 90.8, 'NODE012', 28.4, 31.0, 3.2, 'TASK012', 'TARGET012', 'POD012'),
('uuid-013', 'AGV03', 'MAP001', 'ZONE03', 1, 0, 'LOADER03', @currentTime - 6000, 86.5, 'NODE013', 36.9, 92.5, 2.3, 'TASK013', 'TARGET013', 'POD013'),
('uuid-014', 'AGV04', 'MAP001', 'ZONE04', 3, 0, 'LOADER04', @currentTime - 4000, 92.9, 'NODE014', 4.0, 93.6, 1.8, 'TASK014', 'TARGET014', 'POD014'),
('uuid-015', 'AGV05', 'MAP001', 'ZONE05', 1, 0, 'LOADER05', @currentTime - 2000, 88.1, 'NODE015', 9.8, 67.5, 2.5, 'TASK015', 'TARGET015', 'POD015');

-- 현재 데이터 확인
SELECT TOP 10 * FROM agv_data ORDER BY report_time DESC;

-- 최신 타임스탬프 확인
SELECT 
    'Latest timestamp' as info,
    MAX(report_time) as latest_report_time,
    DATEADD(MILLISECOND, MAX(report_time), '1970-01-01') as latest_date
FROM agv_data; 