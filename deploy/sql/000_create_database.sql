-- 连接 PostgreSQL 的 postgres 维护库后手动执行，执行账号需要 CREATEDB 权限。
-- 在 deploy 目录执行：psql -v ON_ERROR_STOP=1 -U zhijie -d postgres -f sql/000_create_database.sql
-- 仅首次创建时执行，不要放在事务中；数据库所有者为当前执行账号。
CREATE DATABASE ruankao_zhijie
WITH
  ENCODING = 'UTF8' TEMPLATE = template0;
