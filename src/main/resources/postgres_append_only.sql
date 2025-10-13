-- Append-only 전환 마이그레이션 (PostgreSQL)
-- 목적: uuid의 UNIQUE/PK 제약 제거, id BIGSERIAL PK 추가, uuid 인덱스 유지
-- 대상 테이블: public.ant_robot_info, public.ant_flypick_info, public.ant_pod_info,
--              public.mushiny_agv_info, public.mushiny_pod_info

-- 여러 네이밍 케이스 방어적 드롭
ALTER TABLE public.ant_robot_info DROP CONSTRAINT IF EXISTS ant_robot_info_pkey;
ALTER TABLE public.ant_robot_info DROP CONSTRAINT IF EXISTS pk_ant_robot_info;
ALTER TABLE public.ant_robot_info DROP CONSTRAINT IF EXISTS uq_ant_robot_info_robot_no;
ALTER TABLE public.ant_robot_info DROP CONSTRAINT IF EXISTS ant_robot_info_uuid_key;
DROP INDEX IF EXISTS ux_ant_robot_info_uuid;
DROP INDEX IF EXISTS ant_robot_info_uuid_idx;
ALTER TABLE public.ant_robot_info ADD COLUMN IF NOT EXISTS id BIGSERIAL;
ALTER TABLE public.ant_robot_info ADD PRIMARY KEY (id);
CREATE INDEX IF NOT EXISTS idx_ant_robot_info_uuid ON public.ant_robot_info (uuid);

-- ========== ant_flypick_info ==========
ALTER TABLE public.ant_flypick_info DROP CONSTRAINT IF EXISTS ant_flypick_info_pkey;
ALTER TABLE public.ant_flypick_info DROP CONSTRAINT IF EXISTS pk_ant_flypick_info;
ALTER TABLE public.ant_flypick_info DROP CONSTRAINT IF EXISTS ant_flypick_info_uuid_key;
DROP INDEX IF EXISTS ux_ant_flypick_info_uuid;
DROP INDEX IF EXISTS ant_flypick_info_uuid_idx;
ALTER TABLE public.ant_flypick_info ADD COLUMN IF NOT EXISTS id BIGSERIAL;
ALTER TABLE public.ant_flypick_info ADD PRIMARY KEY (id);
CREATE INDEX IF NOT EXISTS idx_ant_flypick_info_uuid ON public.ant_flypick_info (uuid);

-- ========== ant_pod_info ==========
ALTER TABLE public.ant_pod_info DROP CONSTRAINT IF EXISTS ant_pod_info_pkey;
ALTER TABLE public.ant_pod_info DROP CONSTRAINT IF EXISTS pk_ant_pod_info;
ALTER TABLE public.ant_pod_info DROP CONSTRAINT IF EXISTS ant_pod_info_uuid_key;
DROP INDEX IF EXISTS ux_ant_pod_info_uuid;
DROP INDEX IF EXISTS ant_pod_info_uuid_idx;
ALTER TABLE public.ant_pod_info ADD COLUMN IF NOT EXISTS id BIGSERIAL;
ALTER TABLE public.ant_pod_info ADD PRIMARY KEY (id);
CREATE INDEX IF NOT EXISTS idx_ant_pod_info_uuid ON public.ant_pod_info (uuid);

-- ========== ant_inv_info ==========
ALTER TABLE public.ant_inv_info DROP CONSTRAINT IF EXISTS ant_inv_info_pkey;
ALTER TABLE public.ant_inv_info DROP CONSTRAINT IF EXISTS pk_ant_inv_info;
ALTER TABLE public.ant_inv_info DROP CONSTRAINT IF EXISTS ant_inv_info_uuid_key;
DROP INDEX IF EXISTS ux_ant_inv_info_uuid;
DROP INDEX IF EXISTS ant_inv_info_uuid_idx;
ALTER TABLE public.ant_inv_info ADD COLUMN IF NOT EXISTS id BIGSERIAL;
ALTER TABLE public.ant_inv_info ADD PRIMARY KEY (id);
CREATE INDEX IF NOT EXISTS idx_ant_inv_info_uuid ON public.ant_inv_info (uuid);

-- ========== mushiny_agv_info ==========
ALTER TABLE public.mushiny_agv_info DROP CONSTRAINT IF EXISTS mushiny_agv_info_pkey;
ALTER TABLE public.mushiny_agv_info DROP CONSTRAINT IF EXISTS pk_mushiny_agv_info;
ALTER TABLE public.mushiny_agv_info DROP CONSTRAINT IF EXISTS mushiny_agv_info_uuid_key;
DROP INDEX IF EXISTS ux_mushiny_agv_info_uuid;
DROP INDEX IF EXISTS mushiny_agv_info_uuid_idx;
ALTER TABLE public.mushiny_agv_info ADD COLUMN IF NOT EXISTS id BIGSERIAL;
ALTER TABLE public.mushiny_agv_info ADD PRIMARY KEY (id);
CREATE INDEX IF NOT EXISTS idx_mushiny_agv_info_uuid ON public.mushiny_agv_info (uuid);

-- ========== mushiny_pod_info ==========
ALTER TABLE public.mushiny_pod_info DROP CONSTRAINT IF EXISTS mushiny_pod_info_pkey;
ALTER TABLE public.mushiny_pod_info DROP CONSTRAINT IF EXISTS pk_mushiny_pod_info;
ALTER TABLE public.mushiny_pod_info DROP CONSTRAINT IF EXISTS mushiny_pod_info_uuid_key;
DROP INDEX IF EXISTS ux_mushiny_pod_info_uuid;
DROP INDEX IF EXISTS mushiny_pod_info_uuid_idx;
ALTER TABLE public.mushiny_pod_info ADD COLUMN IF NOT EXISTS id BIGSERIAL;
ALTER TABLE public.mushiny_pod_info ADD PRIMARY KEY (id);
CREATE INDEX IF NOT EXISTS idx_mushiny_pod_info_uuid ON public.mushiny_pod_info (uuid);

-- 권장: ANALYZE로 통계 갱신
ANALYZE public.ant_robot_info;
ANALYZE public.ant_flypick_info;
ANALYZE public.ant_pod_info;
ANALYZE public.mushiny_agv_info;
ANALYZE public.mushiny_pod_info;


