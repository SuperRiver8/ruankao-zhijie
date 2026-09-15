# 软考知阶

软考学习与获证认证平台，提供内容管理、题库导入、学习考试和证书人工审核。

## 项目结构

```text
backend/             Spring Boot 后端
frontend/customer/   用户端（React + TypeScript）
frontend/admin/      管理后台（React + Ant Design）
deploy/              独立部署包、配置、打包脚本及初始化 SQL
templates/           JSON / ZIP 导入模板
```

## 环境准备

- Java 21、Maven 3.9、Node.js 22（含 npm）。
- PostgreSQL，数据库名为 `ruankao_zhijie`。
- 本地开发无需 Redis；生产环境使用 Redis。

**以下命令均从项目根目录执行，环境变量示例使用 PowerShell。**

### 初始化数据库

使用与后端一致的数据库账号执行。示例账号 `zhijie` 需事先创建，并具备建库权限：

```powershell
psql -h localhost -U zhijie -d postgres -v ON_ERROR_STOP=1 -f deploy/sql/000_create_database.sql
psql -h localhost -U zhijie -d ruankao_zhijie -v ON_ERROR_STOP=1 -f deploy/sql/001_init.sql
```

数据库已存在时跳过建库命令。**001 仅用于空数据库初始化，不要在已有业务数据库重复执行。** 本地开发不自动建表；Docker 全新部署自动初始化空数据库，不使用 Flyway，也不会自动迁移旧数据。

初始化 SQL 创建后台账号 **`admin` / `admin123`**，首次登录后在“账户设置”修改。用户端账号独立注册，不能用于后台登录。

### 配置后端环境变量

在启动后端的 PowerShell 终端设置：

```powershell
$env:API_DOCS_ENABLED = 'true'
$env:DATABASE_URL = 'jdbc:postgresql://localhost:5432/ruankao_zhijie'
$env:DATABASE_USER = 'zhijie'
$env:DATABASE_PASSWORD = '替换为数据库密码'
$env:SESSION_STORE = 'memory'
$env:JOBS_ENABLED = 'false'
$env:JWT_SECRET = '替换为至少32字节的随机密钥'
$env:ENCRYPTION_KEY = '替换为Base64编码的32字节密钥'
```

首次开发可用以下命令分别生成 JWT 密钥和加密密钥；每运行一次生成一个新值：

```powershell
$bytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
[Convert]::ToBase64String($bytes)
$rng.Dispose()
```

保存生成的配置供后续启动使用。已有加密数据时不能随意更换 `ENCRYPTION_KEY`，否则原证书材料无法解密。

使用 IDEA 时，将上述变量填写到运行配置的“环境变量”，启动 `cn.zhijie.Application`。IDEA 和直接启动的 Java 进程不会自动读取 `deploy/.env`。

### 安装前端依赖

```powershell
npm --prefix frontend/customer ci
npm --prefix frontend/admin ci
```

## 启动开发服务

在已配置环境变量的终端启动后端：

```powershell
mvn -f backend/pom.xml spring-boot:run
```

另开终端启动用户端：

```powershell
npm --prefix frontend/customer run dev
```

再开终端启动管理后台：

```powershell
npm --prefix frontend/admin run dev
```

| 服务                 | 默认地址                                    |
| -------------------- | ------------------------------------------- |
| 用户端               | http://localhost:5173                       |
| 管理后台             | http://localhost:5174                       |
| 后端 API             | http://localhost:8080/api                   |
| 接口文档（开发环境） | http://localhost:8080/swagger-ui/index.html |

两端开发服务器将 `/api` 请求代理到后端。停止服务按 `Ctrl+C`。

内存会话重启后失效，需重新登录。直接运行默认关闭导入任务；需要处理后台上传的 JSON／ZIP 内容时，设置 `JOBS_ENABLED=true` 并重启后端。仅使用 `application.yml` 和环境变量配置，无需设置 Spring profile；接口文档通过 `API_DOCS_ENABLED=true` 开启。

## 构建与运行

### 后端

```powershell
# 编译、测试并打包
mvn -f backend/pom.xml clean package

# 在已配置环境变量的终端运行构建产物
java -jar backend/target/zhijie-api-0.1.0.jar
```

### 前端

```powershell
# 分别执行类型检查与构建
npm --prefix frontend/customer run build
npm --prefix frontend/admin run build

# 或一次构建两端
npm --prefix frontend run build
```

产物分别位于 `frontend/customer/dist` 和 `frontend/admin/dist`。本地查看构建效果：

```powershell
# 分别在两个终端执行
npm --prefix frontend/customer run preview -- --port 4173
npm --prefix frontend/admin run preview -- --port 4174
```

Preview 仅用于本地检查。前后端联调优先使用开发启动命令；生产部署使用 [部署说明](deploy/README.md) 中的 Docker Compose 和 Nginx 配置。

## 常用维护命令

```powershell
# 后端测试
mvn -f backend/pom.xml test

# 全项目格式化 / 格式检查（需先安装 customer 的开发依赖）
node scripts/format.mjs
node scripts/format.mjs --check

# 从已启动的开发后端更新两端接口类型
npm --prefix frontend/customer run types:generate
npm --prefix frontend/admin run types:generate
```

遇到 `package.json` 不存在时，确认使用了对应项目的 `--prefix`。遇到数据库“权限不够”时，核对 `DATABASE_USER` 是否拥有业务表的访问权限；关闭导入任务不会修复数据库权限。

证书图片、PDF 文本提取和二维码仅辅助检查，会员认证等级需经管理员核对官方记录与账号归属后更新。导入规范见 [模板说明](templates/README.md)。
