CREATE TABLE ping_pong (
   "id" SERIAL PRIMARY KEY,
   "service_code" varchar(20),
   "uuid" varchar(50),
   "request_time" timestamp(6),
   "response_time" timestamp(6),
   "request_type" int2,
   "description" varchar(255)
);

COMMENT ON COLUMN ping_pong.id IS '唯一id';
COMMENT ON COLUMN ping_pong.service_code IS '服务唯一编码';
COMMENT ON COLUMN ping_pong.uuid IS '全局唯一编码';
COMMENT ON COLUMN ping_pong.request_time IS '发送时间';
COMMENT ON COLUMN ping_pong.response_time IS '响应时间';
COMMENT ON COLUMN ping_pong.request_type IS '类型';
COMMENT ON COLUMN ping_pong.description IS '描述';
