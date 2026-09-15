import fs from 'node:fs';
const base = 'backend/src/main/java/cn/zhijie';
const meta = JSON.parse(fs.readFileSync('scripts/refactor-meta.json', 'utf8'));
const cap = (s) => s[0].toUpperCase() + s.slice(1),
  camel = (s) => s.replace(/_([a-z])/g, (_, c) => c.toUpperCase());
const requestTypes = {
  AuthService: { register: 'LoginRequest', login: 'LoginRequest' },
  UserService: { register: 'LoginRequest', login: 'LoginRequest', refresh: 'RefreshRequest' },
  ContentService: { save: 'ContentRequest', publish: 'ContentReviewRequest' },
  ContentManagementService: {
    create: 'ContentRequest',
    edit: 'ContentRequest',
    review: 'ContentReviewRequest',
    user: 'UserUpdateRequest',
    certificate: 'CertificateRequest',
    rule: 'MemberRuleRequest',
  },
  MembershipService: {
    submit: 'ApplicationRequest',
    review: 'CertificateReviewRequest',
    identity: 'IdentityRequest',
  },
  ExamService: {
    practice: 'PracticeRequest',
    save: 'AnswersRequest',
    selfGrade: 'SelfGradeRequest',
  },
  LearningService: {
    practice: 'PracticeRequest',
    save: 'AnswersRequest',
    selfGrade: 'SelfGradeRequest',
    learning: 'LearningRequest',
  },
  ImportManagementService: { commit: 'ImportCommitRequest' },
};
const fields = new Set(
  Object.values(meta.entities).flatMap((x) => x.split(',').map((v) => v.split(' ')[1])),
);
for (const folder of ['service', 'task'])
  for (const file of fs.readdirSync(`${base}/${folder}`)) {
    let s = fs.readFileSync(`${base}/${folder}/${file}`, 'utf8');
    const types = requestTypes[file.replace('.java', '')] || {};
    // 接口请求直接传入 Service，不再先转换为 Map。
    for (const [method, type] of Object.entries(types))
      s = s.replace(
        new RegExp(
          `(\\b${method}\\([\\s\\S]*?)Map<String, Object> (p|filters|answers|scores)(?=[,)])`,
        ),
        `$1${type} $2`,
      );
    for (const [variable] of [['p']]) {
      s = s
        .replace(/required\(p, "(\w+)"\)/g, (_, f) => `required(p.${f}(), "${f}")`)
        .replace(/p\.get\("(\w+)"\)/g, (_, f) => `p.${f}()`);
    }
    // 只替换持久化实体变量；JsonNode 使用 path，动态索引继续保留。
    s = s.replace(
      /\b(row|u|user|old|existing|cert|file|f|c|q|l|r|prior|batch|job|v)\.get\("([\w_]+)"\)/g,
      (all, v, n) => (fields.has(n) ? `${v}.get${cap(camel(n))}()` : all),
    );
    s = s.replace(/\(\(Number\) ([\w]+\.get\w+\(\))\)\.intValue\(\)/g, '$1');
    s = s.replace(/\(\(java\.sql\.Timestamp\) ([\w]+\.get\w+\(\))\)\.toInstant\(\)/g, '$1');
    s = s.replace(
      /\b(u|row|user|old|existing|cert|file|f|c|q|l|r|batch)\.(get\w+\(\))\.toString\(\)/g,
      (all, v, g) => (['getId()', 'getEntityId()'].includes(g) ? all : `${v}.${g}`),
    );
    s = s.replaceAll(
      'import static cn.zhijie.util.PojoMapper.*;',
      'import cn.zhijie.util.ResponseMapper;',
    );
    fs.writeFileSync(`${base}/${folder}/${file}`, s);
  }
for (const file of fs.readdirSync(`${base}/controller`)) {
  const p = `${base}/controller/${file}`;
  let s = fs
    .readFileSync(p, 'utf8')
    .replaceAll('import static cn.zhijie.util.PojoMapper.*;', '')
    .replaceAll('parameters(p)', 'p')
    .replaceAll('f.get("media_type").toString()', 'f.getMediaType()')
    .replaceAll('f.get("original_name").toString()', 'f.getOriginalName()');
  fs.writeFileSync(p, s);
}
const pairs = {
  UserResponse: 'UserEntity',
  CertificateResponse: 'CertificateEntity',
  MemberRuleResponse: 'MemberRuleEntity',
  BadgeResponse: 'BadgeProjection',
  LevelHistoryResponse: 'LevelHistoryEntity',
  AuditResponse: 'AuditEntity',
  LearningResponse: 'LearningProjection',
  ContentResponse: 'ContentProjection',
  ImportBatchResponse: 'ImportBatchEntity',
  ExamResponse: 'ExamEntity',
  ExamSummaryResponse: 'ExamEntity',
  ReviewResponse: 'ReviewEntity',
  VerificationResponse: 'VerificationEntity',
};
const responseFields = {};
for (const f of fs.readdirSync(`${base}/pojo/response`)) {
  const p = `${base}/pojo/response/${f}`;
  let s = fs
    .readFileSync(p, 'utf8')
    .replace(/@JsonProperty\("[^"]+"\)\s*/g, '')
    .replace('import com.fasterxml.jackson.annotation.JsonProperty;', '');
  // JSON 文档使用结构化节点，不再将 JSON 二次编码成字符串。
  if (['ImportBatchResponse.java', 'ExamSummaryResponse.java'].includes(f)) {
    s = s.replace(/String (payload|report|result)\b/g, 'JsonNode $1');
    if (!s.includes('import com.fasterxml.jackson.databind.JsonNode;'))
      s = s.replace(/(package [\w.]+;)/, '$1\nimport com.fasterxml.jackson.databind.JsonNode;');
  }
  const m = s.match(/public record (\w+)\(([\s\S]*)\)\s*\{/);
  if (m) responseFields[m[1]] = m[2].split(',').map((x) => x.trim().split(/\s+/).at(-1));
  fs.writeFileSync(p, s);
}
let converters =
  'package cn.zhijie.util;\nimport cn.zhijie.pojo.entity.*;import cn.zhijie.pojo.response.*;import java.time.Instant;\n// 显式白名单映射，持久化敏感字段不会直接序列化给客户端。\npublic final class ResponseMapper {private ResponseMapper(){}\n';
for (const [resp, entity] of Object.entries(pairs)) {
  const name = resp[0].toLowerCase() + resp.slice(1).replace('Response', '');
  converters += `public static ${resp} ${name}(${entity} e){return new ${resp}(${responseFields[resp].map((f) => (f === 'serverTime' ? 'Instant.now()' : `e.get${cap(f)}()`)).join(',')});}\n`;
}
converters += '}';
fs.writeFileSync(`${base}/util/ResponseMapper.java`, converters);
fs.writeFileSync('scripts/response-meta.json', JSON.stringify(responseFields));
