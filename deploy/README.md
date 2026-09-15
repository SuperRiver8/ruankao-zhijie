# 独立部署说明

本地编译，服务器运行。上传完整 `deploy` 后无需上传源码，服务器只需 Docker Engine 和 Compose 插件，并能拉取 PostgreSQL、Redis、Java、Nginx 基础镜像。

## 1. 本地打包

本地安装 Java 21、Maven 3.9、Node.js 22 或更高版本。在项目根目录运行：

```powershell
powershell -ExecutionPolicy Bypass -File deploy/package.ps1
```

脚本安装前端依赖、检查类型、构建两端并打包后端，跳过后端测试。失败立即停止，所有编译成功后才更新 `frontend` 和 `app.jar`，不修改 `.env`。

```text
deploy/
├── docker compose.yml
├── Dockerfile
├── Dockerfile.dockerignore
├── nginx/
│   └── nginx.cnf             # 挂载到 Nginx 容器的站点配置
├── package.ps1
├── .env.example
├── .env                     # 自行配置，不提交 Git
├── app.jar                  # 后端可执行 JAR，不提交 Git
├── postgres-data/           # 服务器 PostgreSQL 数据，不提交 Git
├── attachments/             # 服务器附件数据，不提交 Git
├── frontend/                # 脚本生成，不提交 Git
│   ├── customer/
│   └── admin/
└── sql/
    ├── 000_create_database.sql
    └── 001_init.sql
```

## 2. 配置环境

后端仅保留 `application.yml`，不再使用 dev/prod profile，无需设置 `SPRING_PROFILES_ACTIVE`。Compose 通过环境变量选择 Redis 会话存储，`JOBS_ENABLED` 默认开启导入任务，`API_DOCS_ENABLED` 默认关闭接口文档，可在 `.env` 中修改。数据库、Redis、JWT、AES 和附件路径继续通过环境变量传入。

首次执行，已有 `.env` 时跳过复制：

```powershell
Copy-Item deploy/.env.example deploy/.env
```

编辑 `.env`：`SERVER_HOST` 填服务器 IP，不含协议和端口；默认用户端 `5173`、后台 `5174`。数据库密码、Redis 密码、JWT 密钥和 AES 密钥分别生成，填写到对应变量。以下命令每运行一次生成一个随机值，运行四次：

```powershell
$bytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
[Convert]::ToBase64String($bytes)
$rng.Dispose()
```

保留生成的密钥。AES 密钥丢失或更换会导致已有认证材料无法解密。`.env.dev` 是历史本地配置，不参与部署，不应作为服务器 `.env` 使用。

## 3. 上传并启动

上传整个 `deploy`，包含隐藏的 `.env` 和完整 `frontend` 和 `app.jar`。不要上传本地调试用 `.env.dev`。例如放到服务器 `/opt/ruankao/deploy`。

以下均为服务器 Linux Shell 命令：

```sh
cd /opt/ruankao/deploy
chmod 600 .env
docker compose config --quiet
docker compose up -d --build
docker compose ps
docker compose logs --tail=100 api
```

Compose 自动读取目录中的 `docker compose.yml` 和 `.env`。放行安全组和防火墙 TCP 5173、5174（修改端口后放行对应端口）。

- 用户端：`http://服务器IP:5173`，自行注册。
- 管理后台：`http://服务器IP:5174`，首次使用 `admin / admin123`，登录后进入“账户设置”修改密码。
- API 由两端 Nginx 的 `/api/` 同源代理，不开放宿主机 8080。
- PostgreSQL、Redis 仅容器网络可访问。

空 PostgreSQL 数据卷首次启动创建 `ruankao_zhijie`，自动执行 `001_init.sql`。不挂载 `000_create_database.sql`，它仅供本地手动建库。已有卷不会重新执行初始化，后续升级 SQL 需评审后手动执行。初始化失败时先查看 `docker compose logs postgres`，排查后处理空库，不要在已有业务库重复执行初始化脚本。

所有容器默认使用北京时间（`Asia/Shanghai`，UTC+8）；Java 默认时区、PostgreSQL 会话及日志时区也显式设为北京时间。

## 4. 更新与维护

前端直接使用 `nginx:alpine` 镜像，通过只读挂载加载 `frontend/customer`、`frontend/admin` 和 `nginx/nginx.cnf`，无需前端 Dockerfile；后端继续使用 `Dockerfile` 构建镜像。

本地重新运行打包脚本，将新的产物、后端 Dockerfile 和配置覆盖到服务器。保留服务器 `.env`，不要用示例或本地新密钥覆盖，然后执行：

```sh
docker compose up -d --build
docker compose ps
docker compose logs --tail=100 api web
```

仅修改 `nginx/nginx.cnf` 后运行 `docker compose exec web nginx -t`，通过后执行 `docker compose exec web nginx -s reload`。若上传工具替换了文件或整个产物目录，执行 `docker compose up -d --force-recreate web`，使容器重新挂载最新文件。

```sh
# 停止服务，保留数据
docker compose down
# 启动已有镜像
docker compose up -d
```

PostgreSQL 挂载当前部署目录的 `./postgres-data` 到 `/var/lib/postgresql/data`，附件挂载 `./attachments` 到 `/data/attachments`。上传更新时保留这两个服务器目录。路径相对于 Compose 文件所在目录。

首次部署时 Docker 自动创建挂载目录，Dockerfile 中的目录授权会保证附件目录可写。

不要在需要保留 Redis 数据时执行 `docker compose down -v`。项目名保持 `ruankao-zhijie`，Redis 继续使用原命名卷。历史使用自定义项目名的部署，所有命令继续加 `-p 原项目名`。原 PostgreSQL 和附件命名卷不会自动迁移到当前目录；已有部署应先停服备份，将原数据库恢复到新目录对应的数据库，并迁入附件后再恢复访问。

### 旧附件迁移

升级旧部署时先停止旧 API，备份数据库和原项目的 `data/attachments`，保留原 AES 密钥。新 API 首次启动前执行（替换实际绝对路径）：

```sh
docker compose build api
docker compose run --rm --no-deps --user root --entrypoint sh \
  -v /原项目绝对路径/data/attachments:/old-attachments:ro api \
  -c 'cp -a /old-attachments/. /data/attachments/ && chown -R zhijie:zhijie /data/attachments'
docker compose up -d --build
```

目标 `deploy/attachments` 目录应为空，核对迁移结果后再恢复访问。已有 PostgreSQL 数据目录缺少业务数据库或表时不会自动补建，应按现有数据库状态准备迁移 SQL。

### 备份

以下命令在暂停业务写入期间同时备份数据库和附件；另行妥善保存 `.env`：

```sh
mkdir -p backups
docker compose stop web api
docker compose exec -T postgres pg_dump -U zhijie -d ruankao_zhijie -Fc > backups/database.dump
docker compose run --rm --no-deps --user root --entrypoint tar api \
  -C /data/attachments -czf - . > backups/attachments.tar.gz
docker compose start api web
```

每次备份使用单独目录或转存文件，避免覆盖历史备份。恢复时先停止业务，在空目标库用 `pg_restore` 恢复数据库，将附件归档解压至 `deploy/attachments` 并恢复容器中 `zhijie` 用户对应的所有权，同时使用原 `.env` 密钥。
