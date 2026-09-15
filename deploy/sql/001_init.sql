-- 在 deploy 目录手动执行：psql -U zhijie -d ruankao_zhijie -f sql/001_init.sql
BEGIN;

CREATE TABLE app_user (
  id uuid PRIMARY KEY,
  username varchar(80) NOT NULL UNIQUE,
  password_hash text NOT NULL,
  role varchar(20) NOT NULL DEFAULT 'USER' CHECK (role = 'USER'),
  enabled boolean NOT NULL DEFAULT TRUE,
  permission_version integer NOT NULL DEFAULT 1,
  identity_verified boolean NOT NULL DEFAULT FALSE,
  identity_name_encrypted text,
  identity_evidence text,
  member_level integer NOT NULL DEFAULT 0,
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE admin_user (
  id uuid PRIMARY KEY,
  username varchar(80) NOT NULL UNIQUE,
  password_hash text NOT NULL,
  role varchar(20) NOT NULL CHECK (role IN ('ADMIN','EDITOR','REVIEWER')),
  enabled boolean NOT NULL DEFAULT TRUE,
  permission_version integer NOT NULL DEFAULT 1,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

-- 初始密码 admin123，登录后通过账户设置修改。
INSERT INTO admin_user(id,username,password_hash,role) VALUES ('00000000-0000-4000-8000-000000000001','admin','$2a$10$PxkNQXKyvYAYNAEg86.plOC1EBJhnnIrYPwytTCDR61OhiOustf02','ADMIN');

CREATE TABLE member_rule (
  level integer PRIMARY KEY,
  name text NOT NULL,
  icon text NOT NULL,
  sort_order integer NOT NULL,
  entitlements jsonb NOT NULL DEFAULT '{}'
);

INSERT INTO
  member_rule
VALUES
  (0, '普通会员', 'user', 0, '{}'),
  (1, '初级认证会员', 'bronze', 1, '{}'),
  (2, '中级认证会员', 'silver', 2, '{}'),
  (3, '高级认证会员', 'gold', 3, '{}');

CREATE TABLE certificate (
  id uuid PRIMARY KEY,
  code text UNIQUE NOT NULL,
  name text NOT NULL,
  exam_system text NOT NULL DEFAULT '计算机技术与软件专业技术资格（水平）考试',
  specialty text NOT NULL,
  level integer NOT NULL CHECK (level BETWEEN 1 AND 3),
  enabled boolean NOT NULL DEFAULT TRUE
);

INSERT INTO
  certificate
VALUES
  (
    '00000000-0000-0000-0000-000000000001',
    'PROGRAMMER',
    '程序员',
    '计算机技术与软件专业技术资格（水平）考试',
    '软件',
    1,
    TRUE
  ),
  (
    '00000000-0000-0000-0000-000000000002',
    'SOFTWARE_DESIGNER',
    '软件设计师',
    '计算机技术与软件专业技术资格（水平）考试',
    '软件',
    2,
    TRUE
  ),
  (
    '00000000-0000-0000-0000-000000000003',
    'SYSTEM_ARCHITECT',
    '系统架构设计师',
    '计算机技术与软件专业技术资格（水平）考试',
    '软件',
    3,
    TRUE
  ),
  (
    '00000000-0000-0000-0000-000000000004',
    'NETWORK_ENGINEER',
    '网络工程师',
    '计算机技术与软件专业技术资格（水平）考试',
    '网络',
    2,
    TRUE
  );

CREATE TABLE study_target (
  user_id uuid REFERENCES app_user,
  certificate_id uuid REFERENCES certificate,
  PRIMARY KEY (user_id, certificate_id)
);

CREATE TABLE attachment (
  id uuid PRIMARY KEY,
  owner_id uuid REFERENCES app_user,
  admin_owner_id uuid REFERENCES admin_user,
  CHECK (num_nonnulls(owner_id,admin_owner_id)=1),
  original_name text NOT NULL,
  storage_key text NOT NULL UNIQUE,
  media_type text NOT NULL,
  size bigint NOT NULL,
  checksum text NOT NULL,
  access_level text NOT NULL CHECK (access_level IN ('PRIVATE', 'CONTENT')),
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE certificate_application (
  id uuid PRIMARY KEY,
  user_id uuid NOT NULL REFERENCES app_user,
  certificate_id uuid NOT NULL REFERENCES certificate,
  holder_name_encrypted text NOT NULL,
  number_type text NOT NULL CHECK (
    number_type IN ('CERTIFICATE', 'MANAGEMENT', 'QUERY')
  ),
  number_encrypted text NOT NULL,
  number_hash text NOT NULL,
  obtained_on date NOT NULL,
  attachment_id uuid NOT NULL REFERENCES attachment,
  auxiliary_id uuid REFERENCES attachment,
  note text NOT NULL DEFAULT '',
  revision integer NOT NULL DEFAULT 1,
  auto_result text NOT NULL DEFAULT 'UNCHECKED',
  auto_details jsonb NOT NULL DEFAULT '{}',
  official_result text NOT NULL DEFAULT 'UNVERIFIED' CHECK (
    official_result IN ('UNVERIFIED', 'MATCH', 'MISMATCH', 'UNCERTAIN')
  ),
  status text NOT NULL DEFAULT 'PENDING' CHECK (
    status IN (
      'PENDING',
      'REVIEWING',
      'SUPPLEMENT',
      'APPROVED',
      'REJECTED',
      'REVOKED'
    )
  ),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX application_duplicate ON certificate_application (number_hash);

CREATE TABLE application_revision (
  application_id uuid REFERENCES certificate_application,
  revision integer,
  snapshot jsonb NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (application_id, revision)
);

CREATE TABLE verification_record (
  id uuid PRIMARY KEY,
  application_id uuid REFERENCES certificate_application,
  reviewer_id uuid REFERENCES admin_user,
  result text NOT NULL,
  official_source text NOT NULL,
  evidence text NOT NULL,
  ownership_evidence text NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE review_record (
  id uuid PRIMARY KEY,
  application_id uuid REFERENCES certificate_application,
  reviewer_id uuid REFERENCES admin_user,
  action text NOT NULL,
  reason text NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE user_certificate (
  id uuid PRIMARY KEY,
  user_id uuid NOT NULL REFERENCES app_user,
  certificate_id uuid NOT NULL REFERENCES certificate,
  application_id uuid UNIQUE NOT NULL REFERENCES certificate_application,
  number_hash text NOT NULL,
  active boolean NOT NULL DEFAULT TRUE,
  certified_at timestamptz NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX one_active_certificate_number ON user_certificate (number_hash)
WHERE
  active;

CREATE TABLE member_level_history (
  id uuid PRIMARY KEY,
  user_id uuid REFERENCES app_user,
  old_level integer NOT NULL,
  new_level integer NOT NULL,
  application_id uuid REFERENCES certificate_application,
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE audit_log (
  id uuid PRIMARY KEY,
  actor_id uuid REFERENCES app_user,
  admin_actor_id uuid REFERENCES admin_user,
  CHECK (num_nonnulls(actor_id,admin_actor_id)<=1),
  action text NOT NULL,
  target_id uuid,
  detail text NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now()
);

-- 内容实体编码稳定，版本不可覆盖已发布数据。
CREATE TABLE content_entity (
  id uuid PRIMARY KEY,
  kind text NOT NULL CHECK (
    kind IN (
      'KNOWLEDGE',
      'SYLLABUS',
      'MAPPING',
      'QUESTION',
      'MATERIAL',
      'PAPER'
    )
  ),
  namespace text NOT NULL,
  external_id text NOT NULL,
  current_version integer NOT NULL DEFAULT 1,
  UNIQUE (namespace, kind, external_id)
);

CREATE TABLE content_version (
  entity_id uuid REFERENCES content_entity,
  version integer,
  title text NOT NULL,
  payload jsonb NOT NULL,
  checksum text NOT NULL,
  status text NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PUBLISHED', 'REJECTED')),
  created_by uuid REFERENCES admin_user,
  created_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (entity_id, version)
);

CREATE TABLE import_batch (
  id uuid PRIMARY KEY,
  owner_id uuid REFERENCES admin_user,
  idempotency_key text NOT NULL,
  checksum text NOT NULL,
  status text NOT NULL,
  payload jsonb NOT NULL,
  report jsonb NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  allow_updates boolean NOT NULL DEFAULT FALSE,
  UNIQUE (owner_id, idempotency_key)
);

CREATE TABLE exam_attempt (
  id uuid PRIMARY KEY,
  user_id uuid REFERENCES app_user,
  paper_id uuid REFERENCES content_entity,
  snapshot jsonb NOT NULL,
  answers jsonb NOT NULL DEFAULT '{}',
  result jsonb,
  started_at timestamptz NOT NULL DEFAULT now(),
  deadline timestamptz NOT NULL,
  submitted_at timestamptz,
  status text NOT NULL DEFAULT 'IN_PROGRESS'
);

CREATE TABLE learning_record (
  user_id uuid REFERENCES app_user,
  entity_id uuid REFERENCES content_entity,
  version integer NOT NULL,
  favorite boolean NOT NULL DEFAULT FALSE,
  wrong boolean NOT NULL DEFAULT FALSE,
  progress integer NOT NULL DEFAULT 0 CHECK (progress BETWEEN 0 AND 100),
  updated_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (user_id, entity_id)
);

COMMIT;
