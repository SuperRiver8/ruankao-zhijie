# 部署说明

后台与用户账户已分离：`admin_user` 保存后台账号，`app_user` 保存用户端会员。默认管理员由 `sql/001_init.sql` 插入，账号 `admin`、密码 `admin123`，只保存 BCrypt 摘要。Java 启动不再创建账号。

**新版 001 仅适用于空数据库初始化，不能在已有数据库上直接重复执行。** 本次不提供旧数据迁移，不自动删除数据库或执行 SQL；已有数据请先备份并自行准备空数据库。

后台 ADMIN 可在“后台账号”中新建、编辑、禁用后台账号并分配 ADMIN、EDITOR、REVIEWER 角色。“会员身份”仅管理用户端会员，不能授予后台权限。用户端注册账号不能登录后台。

“账户设置”需验证当前密码，修改账号或密码后旧会话失效。首次登录后请修改默认密码。本次身份隔离升级使旧令牌失效，两个前端都需要重新登录。

前端已拆为 `frontend/customer` 和 `frontend/admin` 两个独立项目。各自执行 `npm ci`、`npm run dev`；端口仍为 5173、5174。Docker 分别安装依赖，构建 `customer/dist` 和 `admin/dist`，Nginx 的访问端口保持不变。

部署配置统一放在与 `backend` 同级的 `deploy` 目录：

```text
deploy/
├── .env.example                         # 环境变量示例
├── compose.yml                          # 服务编排
├── backend.Dockerfile                   # 后端镜像
├── backend.Dockerfile.dockerignore      # 后端构建上下文过滤
├── frontend.Dockerfile                  # 前端镜像
├── frontend.Dockerfile.dockerignore     # 前端构建上下文过滤
├── nginx.conf                           # 双前端与 API 代理
└── sql/                                 # 手动执行的 SQL
    ├── 000_create_database.sql          # 创建 ruankao_zhijie 数据库
    └── 001_init.sql
```

## 启动

以下命令使用 PowerShell，从项目根目录开始：

```powershell
cd deploy
Copy-Item .env.example .env
```

编辑 `deploy/.env`，设置数据库密码、Redis 密码、至少 32 字节的 JWT 密钥，以及 Base64 编码的 32 字节 AES 密钥。AES 密钥可用以下命令生成：

```powershell
[Convert]::ToBase64String([System.Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
```

实际 `.env` 已被 Git 忽略，不要提交密钥。以下命令均在 `deploy` 目录执行：

```powershell
docker compose up -d postgres redis
# 首次启动先等待数据库就绪。
docker compose ps
Get-Content -Raw -Encoding utf8 sql/000_create_database.sql | docker compose exec -T postgres psql -v ON_ERROR_STOP=1 -U zhijie -d postgres
Get-Content -Raw -Encoding utf8 sql/001_init.sql | docker compose exec -T postgres psql -v ON_ERROR_STOP=1 -U zhijie -d ruankao_zhijie
docker compose up -d --build api web
```

业务数据库名统一为 `ruankao_zhijie`，连接用户名仍为 `zhijie`。先连接 `postgres` 维护库执行 `000_create_database.sql`，再连接 `ruankao_zhijie` 执行 `001_init.sql`。创建数据库需要 CREATEDB 权限，且不能在事务中执行；数据库已存在时跳过创建脚本，建表脚本仅对空数据库执行一次。

Compose 不自动创建业务数据库，也不配置 Flyway、自动迁移或容器自动建表。以后结构变更的 SQL 也放在 `sql/`，人工评审后执行。若此前已经使用 `zhijie` 数据库存有业务数据，本次配置修改不会迁移或删除旧数据；应先另行完成数据迁移，再启动使用新库名的 API。

打开 [管理后台](http://localhost:5174)，使用 `admin / admin123` 登录并修改密码。用户端在 [customer](http://localhost:5173) 独立注册。后台导入 `templates/example.json` 后审核发布，再使用用户端学习及考试。

如需从项目根目录运行 Compose，请显式指定配置和环境文件：

```powershell
docker compose --env-file deploy/.env -f deploy/compose.yml up -d
```

## 不安装 Redis 的本地调试

开发环境默认关闭导入任务，不会每 3 秒查询 `import_batch`。需要处理后台上传的 JSON／ZIP 内容包时，在后端运行环境设置 `JOBS_ENABLED=true` 并重启。关闭时导入提交接口会明确提示未启用，不会创建一直排队的任务；生产环境默认启用。

若日志出现“对表 import_batch 权限不够”，说明数据库连接账号没有该表的访问权限。关闭任务只能停止轮询，不能修复数据库权限。请核对 `DATABASE_USER` 与建表账号；其他业务表也可能存在同样的问题。

后端默认使用 `dev` 配置。未配置非空 `REDIS_HOST` 或 `spring.data.redis.host` 时，会话和登录限流使用服务器内存，不创建 Redis 连接，也不检查 Redis 健康状态。前端继续使用 Bearer JWT，无需修改。重启后重新登录即可；内存模式仅适用于单实例开发调试。

在 IDEA 的运行配置中设置数据库连接和密钥，例如：

```text
SPRING_PROFILES_ACTIVE=dev
SESSION_STORE=auto
DATABASE_URL=jdbc:postgresql://localhost:5432/ruankao_zhijie
DATABASE_USER=zhijie
DATABASE_PASSWORD=你的数据库密码
JWT_SECRET=至少32字节的随机密钥
ENCRYPTION_KEY=Base64编码的32字节密钥
```

不要设置 `REDIS_HOST`；如果系统已有 Redis 地址环境变量，可显式设置 `SESSION_STORE=memory`。IDEA 不会自动加载 `deploy/.env`。PostgreSQL 和手动初始化的数据库仍然必需，JWT/AES 密钥也必须配置；该模式仅移除 Redis 运行依赖。

`SESSION_STORE` 可选 `auto`、`memory`、`redis`。`auto` 遇到显式 Redis 地址就使用 Redis；Redis 已配置但不可用时返回服务不可用，不降级、不绕过鉴权。Redis 模式需要配置主机、端口及对应密码，健康检查包含 Redis。

`prod` 默认 `redis`，缺少地址或选择内存模式都会启动失败。Compose 已固定 `SPRING_PROFILES_ACTIVE=prod` 和 `SESSION_STORE=redis`。保留原有 Redis 会话键格式，本次不需要修改数据库。

## 路径与备份

Docker 构建上下文为项目根目录，Dockerfile 的 COPY 路径已相应调整。Compose 项目名固定为 `ruankao-zhijie`，附件仍保存在项目根目录的 `data/attachments`，避免移动配置后切换到另一份数据目录。此前使用自定义 Compose 项目名的部署，继续用 `-p 原项目名` 指定。

默认仅监听本机端口。对外部署时配置 HTTPS 反向代理与实际 CORS 域名。不能将附件目录映射为静态目录；容器用户需有附件数据目录读写权限。

备份应在暂停写入时同时保存 PostgreSQL（`pg_dump`）、项目根目录 `data/attachments` 及妥善保管的 AES 密钥。密钥丢失无法解密认证材料，不能随意更换。Redis 会话可撤销重建。生产环境应另外配置备份保留策略、监控、附件扫描与依法确定的敏感材料保留期限。

本地 Java 进程不会自动读取 `deploy/.env`，请在终端或 IDE 中注入其中的环境变量。
