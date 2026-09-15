# AI 内容导入规范 v1.0

先从后台下载实时字典和 JSON Schema。`schemas/` 提供六类独立 Schema，`*.example.json` 提供分类型示例（引用须已存在）；`example.json` 是包含全部依赖、可直接导入的演示包，不代表官方大纲或真题。上传仅进入预览，确认提交后后台任务创建待审核版本。将 `zip-example/` 的内容打包到 ZIP 根目录即可演示 ZIP 导入。模板更新后运行 `node scripts/export-templates.mjs` 重新导出。

## 包结构

单 JSON 使用 `templateVersion`、`namespace`、`entries`。每个条目包含 `kind`、`externalId`、`payload`。

ZIP 的 `manifest.json` 内容示例：

```json
{
  "templateVersion": "1.0",
  "namespace": "ruankao-demo",
  "files": ["knowledge-points.json", "syllabus-mappings.json", "questions.json", "materials.json"]
}
```

每个分文件是条目数组（不是完整包对象）。附件放在 `attachments/`，在 payload 的 `attachmentPaths` 中引用相对路径，例如 `["attachments/diagram.png"]`。服务端校验路径、大小及文件签名后转换为私有存储的内容附件 ID。也可通过后台上传后直接使用 `attachmentIds`。同一上传者的相同内容附件会复用 ID。

## 模板与字段

| kind      | 必填字段（payload）                                   | 其他常用字段                                                                                |
| --------- | ----------------------------------------------------- | ------------------------------------------------------------------------------------------- |
| KNOWLEDGE | title                                                 | body、tags                                                                                  |
| SYLLABUS  | title、version、subjects 数组                         | certificateCode；科目含 code/name/chapters，章节含 code/name                                |
| MAPPING   | title、syllabusCode、subject、chapter、knowledgeCodes | requirement                                                                                 |
| QUESTION  | title、type、stem、difficulty                         | certificateCode、syllabusCode、subject、chapter、knowledgeCodes、attachmentIds、explanation |
| MATERIAL  | title                                                 | body、knowledgeCodes、certificateCode、attachmentIds                                        |
| PAPER     | title、durationMinutes、questionIds 或 count          | points、certificateCode、type、difficulty、shuffleQuestions、shuffleOptions                 |

题目难度为 1–5，与证书初中高级无关。`knowledgeCodes`、`syllabusCode` 引用当前命名空间中的稳定编码，证书引用全局目录 code。

| type          | 作答 / 答案结构                                                                     |
| ------------- | ----------------------------------------------------------------------------------- |
| SINGLE        | options 为至少两个 `{id,text}`；answer 为单元素选项 ID 数组                         |
| MULTIPLE      | 同上；answer 至少两个不同 ID；全对才得分                                            |
| BOOLEAN       | answer 为 `["true"]` 或 `["false"]`                                                 |
| FILL          | answer 为按空位排列的字符串数组；忽略首尾空格、精确匹配                             |
| SHORT / ESSAY | rubric 为评分要点；作答为文字，交卷后依据评分要点自评                               |
| CASE          | stem 为公共材料，children 为子题数组；禁止嵌套 CASE；整组保存和版本化，首期整组自评 |

固定卷 `questionIds` 引用平台题目 UUID，动态卷使用 count 和可选的证书、题型、难度过滤。首次导入适合动态卷；题目发布后可配置固定卷。附件：证书材料支持 PDF/PNG/JPEG/WebP；正文 Markdown 不接受原始 HTML，图片通过受控附件组件查看。

## AI 提示词

> 请严格按随附 import-schema.json 和字典生成软考学习内容，输出纯 JSON。templateVersion 为 1.0，namespace 为我指定的稳定来源。不得虚构目录编码、官方大纲或真题来源。每条记录含 kind、externalId、payload。选择题答案必须引用存在的选项 ID；主观题提供明确评分点；案例公共材料和子题为一条完整记录。所有知识点和大纲引用必须来自字典或同批条目。只生成学习内容，不生成用户、获证认证、会员等级数据。题目解析请说明推理过程。校验不通过时修正，不通过改编码规避冲突。

## 限制与重复提交

- 单包最多 500 条，上传 20MB，ZIP 解压总计 30MB，最多 1000 个文件；更大数据请按稳定命名空间拆批。
- 内容相同跳过；同编码内容变化标记 CONFLICT，确认更新后生成新版本。JSON 对象字段顺序不影响内容判断，数组顺序保留。
- `Idempotency-Key` 对同一上传请求保持不变。提交按钮可重试；后台任务和每批提交结果持久化，失败整批回滚。
- 后续内容发布审核是独立操作；不得通过导入创建会员认证。
