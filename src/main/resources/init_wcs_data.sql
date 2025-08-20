-- WCS_DB 초기 데이터 삽입 스크립트 (수정됨)
-- robot_info: 20개, inventory_info: 10개, pod_info: 10개

USE cdc_test;

-- 기존 데이터 삭제
DELETE FROM robot_info;
DELETE FROM inventory_info;
DELETE FROM pod_info;

-- 1. robot_info 테이블 초기 데이터 (20개)
INSERT INTO robot_info (uuid_no, robot_no, map_code, zone_code, status, manual, loaders, report_time, battery, node_id, pos_x, pos_y, speed, task_id, next_target, pod_id) VALUES
('robot-001', 'ROBOT_001', 'MAP_A', 'ZONE_1', 1, 0, 'LOADER_A', 1704067200000, 85.5, 'NODE_001', 100.0, 200.0, 2.5, 'TASK_001', 'TARGET_A', 'POD_001'),
('robot-002', 'ROBOT_002', 'MAP_A', 'ZONE_1', 2, 0, 'LOADER_B', 1704067201000, 92.3, 'NODE_002', 150.0, 250.0, 3.0, 'TASK_002', 'TARGET_B', 'POD_002'),
('robot-003', 'ROBOT_003', 'MAP_A', 'ZONE_2', 1, 0, 'LOADER_C', 1704067202000, 78.9, 'NODE_003', 200.0, 300.0, 2.8, 'TASK_003', 'TARGET_C', 'POD_003'),
('robot-004', 'ROBOT_004', 'MAP_B', 'ZONE_2', 3, 0, 'LOADER_D', 1704067203000, 95.1, 'NODE_004', 250.0, 350.0, 2.2, 'TASK_004', 'TARGET_D', 'POD_004'),
('robot-005', 'ROBOT_005', 'MAP_B', 'ZONE_3', 1, 0, 'LOADER_E', 1704067204000, 88.7, 'NODE_005', 300.0, 400.0, 3.2, 'TASK_005', 'TARGET_E', 'POD_005'),
('robot-006', 'ROBOT_006', 'MAP_B', 'ZONE_3', 2, 0, 'LOADER_F', 1704067205000, 91.4, 'NODE_006', 350.0, 450.0, 2.9, 'TASK_006', 'TARGET_F', 'POD_006'),
('robot-007', 'ROBOT_007', 'MAP_C', 'ZONE_4', 1, 0, 'LOADER_G', 1704067206000, 76.2, 'NODE_007', 400.0, 500.0, 2.7, 'TASK_007', 'TARGET_G', 'POD_007'),
('robot-008', 'ROBOT_008', 'MAP_C', 'ZONE_4', 4, 0, 'LOADER_H', 1704067207000, 89.6, 'NODE_008', 450.0, 550.0, 3.1, 'TASK_008', 'TARGET_H', 'POD_008'),
('robot-009', 'ROBOT_009', 'MAP_C', 'ZONE_5', 1, 0, 'LOADER_I', 1704067208000, 94.3, 'NODE_009', 500.0, 600.0, 2.6, 'TASK_009', 'TARGET_I', 'POD_009'),
('robot-010', 'ROBOT_010', 'MAP_D', 'ZONE_5', 2, 0, 'LOADER_J', 1704067209000, 87.8, 'NODE_010', 550.0, 650.0, 2.8, 'TASK_010', 'TARGET_J', 'POD_010'),
('robot-011', 'ROBOT_011', 'MAP_D', 'ZONE_6', 1, 0, 'LOADER_K', 1704067210000, 93.5, 'NODE_011', 600.0, 700.0, 3.0, 'TASK_011', 'TARGET_K', 'POD_011'),
('robot-012', 'ROBOT_012', 'MAP_D', 'ZONE_6', 3, 0, 'LOADER_L', 1704067211000, 81.9, 'NODE_012', 650.0, 750.0, 2.4, 'TASK_012', 'TARGET_L', 'POD_012'),
('robot-013', 'ROBOT_013', 'MAP_E', 'ZONE_7', 1, 0, 'LOADER_M', 1704067212000, 96.2, 'NODE_013', 700.0, 800.0, 2.9, 'TASK_013', 'TARGET_M', 'POD_013'),
('robot-014', 'ROBOT_014', 'MAP_E', 'ZONE_7', 2, 0, 'LOADER_N', 1704067213000, 90.7, 'NODE_014', 750.0, 850.0, 3.1, 'TASK_014', 'TARGET_N', 'POD_014'),
('robot-015', 'ROBOT_015', 'MAP_E', 'ZONE_8', 1, 0, 'LOADER_O', 1704067214000, 84.1, 'NODE_015', 800.0, 900.0, 2.7, 'TASK_015', 'TARGET_O', 'POD_015'),
('robot-016', 'ROBOT_016', 'MAP_F', 'ZONE_8', 4, 0, 'LOADER_P', 1704067215000, 88.9, 'NODE_016', 850.0, 950.0, 2.8, 'TASK_016', 'TARGET_P', 'POD_016'),
('robot-017', 'ROBOT_017', 'MAP_F', 'ZONE_9', 1, 0, 'LOADER_Q', 1704067216000, 92.6, 'NODE_017', 900.0, 1000.0, 3.2, 'TASK_017', 'TARGET_Q', 'POD_017'),
('robot-018', 'ROBOT_018', 'MAP_F', 'ZONE_9', 2, 0, 'LOADER_R', 1704067217000, 79.4, 'NODE_018', 950.0, 1050.0, 2.6, 'TASK_018', 'TARGET_R', 'POD_018'),
('robot-019', 'ROBOT_019', 'MAP_G', 'ZONE_10', 1, 0, 'LOADER_S', 1704067218000, 95.8, 'NODE_019', 1000.0, 1100.0, 2.9, 'TASK_019', 'TARGET_S', 'POD_019'),
('robot-020', 'ROBOT_020', 'MAP_G', 'ZONE_10', 3, 0, 'LOADER_T', 1704067219000, 86.3, 'NODE_020', 1050.0, 1150.0, 3.0, 'TASK_020', 'TARGET_T', 'POD_020');

-- 2. inventory_info 테이블 초기 데이터 (10개)
INSERT INTO inventory_info (uuid_no, inventory, batch_num, unitload, sku, pre_qty, new_qty, origin_order, status, report_time) VALUES
('inv-001', 'INV_001', 'BATCH_001', 'UNIT_001', 'SKU_001', 100, 95, 'ORDER_001', 1, 1704067200000),
('inv-002', 'INV_002', 'BATCH_002', 'UNIT_002', 'SKU_002', 150, 140, 'ORDER_002', 1, 1704067201000),
('inv-003', 'INV_003', 'BATCH_003', 'UNIT_003', 'SKU_003', 200, 190, 'ORDER_003', 2, 1704067202000),
('inv-004', 'INV_004', 'BATCH_004', 'UNIT_004', 'SKU_004', 75, 70, 'ORDER_004', 1, 1704067203000),
('inv-005', 'INV_005', 'BATCH_005', 'UNIT_005', 'SKU_005', 120, 115, 'ORDER_005', 1, 1704067204000),
('inv-006', 'INV_006', 'BATCH_006', 'UNIT_006', 'SKU_006', 180, 175, 'ORDER_006', 2, 1704067205000),
('inv-007', 'INV_007', 'BATCH_007', 'UNIT_007', 'SKU_007', 90, 85, 'ORDER_007', 1, 1704067206000),
('inv-008', 'INV_008', 'BATCH_008', 'UNIT_008', 'SKU_008', 250, 245, 'ORDER_008', 1, 1704067207000),
('inv-009', 'INV_009', 'BATCH_009', 'UNIT_009', 'SKU_009', 160, 155, 'ORDER_009', 2, 1704067208000),
('inv-010', 'INV_010', 'BATCH_010', 'UNIT_010', 'SKU_010', 110, 105, 'ORDER_010', 1, 1704067209000);

-- 3. pod_info 테이블 초기 데이터 (10개)
INSERT INTO pod_info (uuid_no, pod_id, pod_face, location, report_time) VALUES
('pod-001', 'POD_001', 'FACE_A', 'LOC_001', 1704067200000),
('pod-002', 'POD_002', 'FACE_B', 'LOC_002', 1704067201000),
('pod-003', 'POD_003', 'FACE_A', 'LOC_003', 1704067202000),
('pod-004', 'POD_004', 'FACE_C', 'LOC_004', 1704067203000),
('pod-005', 'POD_005', 'FACE_B', 'LOC_005', 1704067204000),
('pod-006', 'POD_006', 'FACE_A', 'LOC_006', 1704067205000),
('pod-007', 'POD_007', 'FACE_D', 'LOC_007', 1704067206000),
('pod-008', 'POD_008', 'FACE_C', 'LOC_008', 1704067207000),
('pod-009', 'POD_009', 'FACE_B', 'LOC_009', 1704067208000),
('pod-010', 'POD_010', 'FACE_A', 'LOC_010', 1704067209000);

-- 데이터 삽입 확인
SELECT 'robot_info' as table_name, COUNT(*) as record_count FROM robot_info
UNION ALL
SELECT 'inventory_info' as table_name, COUNT(*) as record_count FROM inventory_info
UNION ALL
SELECT 'pod_info' as table_name, COUNT(*) as record_count FROM pod_info; 