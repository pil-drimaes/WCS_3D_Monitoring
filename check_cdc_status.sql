-- CDC 기능 상태 확인
SELECT 
    name,
    is_cdc_enabled,
    CASE 
        WHEN is_cdc_enabled = 1 THEN '활성화됨'
        ELSE '비활성화됨'
    END as cdc_status
FROM sys.databases 
WHERE name = 'cdc_test';

-- CDC 테이블 확인
SELECT 
    t.name as table_name,
    t.is_tracked_by_cdc,
    CASE 
        WHEN t.is_tracked_by_cdc = 1 THEN 'CDC 추적 활성화'
        ELSE 'CDC 추적 비활성화'
    END as cdc_tracking_status
FROM sys.tables t
WHERE t.name = 'TestData';

-- CDC 작업 확인
SELECT 
    job_id,
    name as job_name,
    enabled,
    CASE 
        WHEN enabled = 1 THEN '활성화됨'
        ELSE '비활성화됨'
    END as job_status
FROM msdb.dbo.sysjobs 
WHERE name LIKE '%CDC%'; 