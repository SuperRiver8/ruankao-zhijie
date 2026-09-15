# 软考知阶

基于 Java 21 / Spring Boot / MyBatis XML / PostgreSQL / Redis 与 Vite / React / TypeScript 的可运行首期实现，管理后台使用 Ant Design。

## 部署与启动

部署配置、环境变量示例、Dockerfile、Nginx 配置及手动 SQL 已统一放在与 backend 同级的 [deploy](deploy/README.md) 目录。请按该目录的说明启动服务、手动建表和初始化管理员。

## 本地开发

后端按技术层组织，接口路径保持不变：

```text
backend/src/main/java/cn/zhijie/
├── controller/    # HTTP 接口、请求参数和响应
├── config/        # 安全与定时任务配置
├── dao/           # MyBatis 数据访问接口
├── pojo/          # 请求、响应及登录身份实体
│   ├── request/   # 接口请求实体
│   └── response/  # 接口响应实体
├── service/       # 业务校验、事务与数据访问编排
├── integration/   # PDF、二维码及授权核验扩展
├── task/          # 后台任务调度入口
├── exception/     # 全局异常处理
└── util/          # 加密、JSON 等通用工具
```

业务请求遵循 `Controller → Service → DAO`；Mapper XML 保存在 `backend/src/main/resources/mappers`，扫描包和 XML 命名空间与 `dao` 一致。

Mapper 接口与 XML 按业务一一对应，Service 仅注入所需的 Mapper：

| Mapper              | 数据范围                                 |
| ------------------- | ---------------------------------------- |
| UserMapper          | 用户账号、身份核实与权限                 |
| CertificateMapper   | 证书目录                                 |
| MembershipMapper    | 已认证证书、徽章、会员等级规则与变化历史 |
| CertificationMapper | 获证申请、申请版本、核验与审核记录       |
| AttachmentMapper    | 附件元数据及发布访问检查                 |
| ContentMapper       | 内容实体、内容版本及发布                 |
| ImportMapper        | 导入批次、队列与处理结果                 |
| ExamMapper          | 考试记录、答案及评分结果                 |
| LearningMapper      | 备考目标、学习进度、错题与收藏           |
| AuditMapper         | 操作审计日志                             |

跨表查询放在对应业务 Mapper 中；审核、认证和等级更新仍由 Service 的同一事务协调。

接口返回明确的实体或实体列表，不使用 `Object` 或未限定的 `ResponseEntity<?>`。实体采用 Java 21 record，字段通过 `@JsonProperty` 保持已有 JSON 命名，私密数据库字段不进入响应。动态内容、试卷快照和 JSON Schema 使用 `JsonNode`，附件下载使用 `ResponseEntity<FileSystemResource>`。DAO 内部 SQL 投影及参数仍使用 Map，由 Service 转换为响应实体；不直接作为接口返回。

环境需要 Java 21、Maven、Node.js 22、PostgreSQL、Redis。后端环境变量与 `deploy/.env.example` 一致；本地 Java 进程不会自动读取 `deploy/.env`，请在终端或 IDE 中注入。

```powershell
mvn -f backend/pom.xml spring-boot:run
cd frontend
npm install
npm run dev
# 另开终端，在 frontend 执行 npm run dev:admin
```

构建检查：`mvn -f backend/pom.xml test` 与 `npm --prefix frontend run build`。受限环境可为 Maven 添加 `-Dmaven.repo.local=.cache/m2`。

## 代码格式

项目使用 `.editorconfig` 和统一 Prettier 配置；Java 使用 4 空格缩进，前端及配置使用 2 空格缩进。安装前端开发依赖后，在项目根目录执行：

```powershell
npm --prefix frontend run format
npm --prefix frontend run format:check
```

格式化覆盖 Java、TypeScript / React、CSS、HTML、JSON、XML、YAML、SQL 和 Markdown，跳过依赖、构建产物、缓存及实际环境变量文件。Mapper XML 保留 SQL 文本空白，避免格式化拆开 PostgreSQL 运算符或 MyBatis 占位符。

## 已实现的主流程

- 注册、登录、短期 JWT、Redis 会话、原子轮换 Refresh Token、退出、封禁、权限版本校验、登录限流。后台分 ADMIN / EDITOR / REVIEWER；首期管理范围为平台全局。
- 证书目录、独立备考目标、最高有效资格等级、证书徽章、可配置等级名称 / 图标 / 排序 / 权益元数据。
- 证明上传、AES-GCM 加密姓名和编号、HMAC 编号查重、PDF 文本辅助检查、二维码官方域名提示、独立自动结果 / 官方结果 / 人工状态。
- 退回补充保留申请版本；官方查验与账号身份归属分别确认；自审禁止；通过、认证记录和等级更新在同一事务内；撤销后重新计算；数据库唯一索引防止同编号重复认领。
- 证书、身份辅助附件仅本人及审核角色访问，内容附件随发布状态访问；统一受控 API；查看原件和敏感详情记录审计日志。
- 大纲 / 知识点 / 映射 / 题目 / 资料 / 试卷使用稳定编码与内容版本。管理页提供结构化外层和 JSON 内容编辑。
- JSON / 带附件 ZIP 校验、Schema、编码字典、重复跳过、冲突预览、异步事务提交、失败重试，导入只创建待审核内容。
- 学习记录、收藏、专项练习、错题重练、固定卷 / 动态卷、服务端截止时间、答案持久化、交卷幂等、冻结题目与选项顺序 / 评分规则、客观题判分、主观题自评与报告。

## 认证审核操作

1. 用户提交已获证申请；预检查失败仍可人工审核，不会自动通过。
2. 审核员在会员身份页面记录可靠的账号身份核实依据。不得只凭同名；已核实身份不允许直接覆盖。
3. 在获证审核页对照原件、字段差异与查验入口，人工使用官方渠道查验。填写官方记录依据、账号归属依据并明确确认归属。
4. 只有官方匹配、身份已核实且姓名匹配、两类依据齐全才能通过。无法确认时退回补充，不能直接认定为假证。
5. 公共展示为“平台审核通过”，不代表官方背书。核验依据加密保存，审核理由对用户可见，请勿在理由中写证件号码。

## 实现边界与后续扩展

- 官方核验无自动 API 实现；`OfficialVerificationProvider` 保留授权接入边界。图片 OCR 通过 `CertificateExtractor` 扩展；当前仅提取 PDF 文本，图片识别失败不阻断审核。二维码只检查域名，不自动请求任何地址。
- 首期身份核实由有权限的人工记录依据，没有接入实名认证供应商；不采集官方查询网站密码。不同编号类型指向同一证书时仍需管理员交叉核对。
- 首期权益仅保存配置，不据此限制学习；不包含付费订阅。后台是全局角色范围，暂未按专业或证书划分管理员范围。
- 案例题整组自评，主观题结果明确标记用户自评。复杂选做规则、分科及格线、人工阅卷分配尚未实现。
- 大纲章节保存在版本 JSON 中，知识点引用已校验；富文本当前支持 Markdown / GFM，数学公式及受控定位扩展待增加。
- 资料上传支持 PDF、常见图片、MP3（ID3 标头）、WAV、OGG、MP4；音视频转码、断点续传和资料定位待扩展。列表首期有条数上限，尚未提供服务端分页。
