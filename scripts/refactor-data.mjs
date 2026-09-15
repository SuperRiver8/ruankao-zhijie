import fs from 'node:fs';
const base = 'backend/src/main';
const entities = {
  UserEntity:
    'UUID id,String username,String password_hash,String role,Boolean enabled,Integer permission_version,Boolean identity_verified,String identity_name_encrypted,String identity_evidence,Integer member_level,Instant created_at',
  CertificateEntity:
    'UUID id,String code,String name,String exam_system,String specialty,Integer level,Boolean enabled',
  MemberRuleEntity:
    'Integer level,String name,String icon,Integer sort_order,JsonNode entitlements',
  BadgeProjection: 'String name,Integer level,String specialty,Instant certified_at',
  AttachmentEntity:
    'UUID id,UUID owner_id,String original_name,String storage_key,String media_type,Long size,String checksum,String access_level,Instant created_at',
  ApplicationProjection:
    'UUID id,UUID user_id,UUID certificate_id,String holder_name_encrypted,String number_type,String number_encrypted,String number_hash,LocalDate obtained_on,UUID attachment_id,UUID auxiliary_id,String note,Integer revision,String auto_result,JsonNode auto_details,String official_result,String status,Instant created_at,Instant updated_at,String certificate_name,String username',
  VerificationEntity:
    'UUID id,UUID application_id,UUID reviewer_id,String result,String official_source,String evidence,String ownership_evidence,Instant created_at',
  ReviewEntity:
    'UUID id,UUID application_id,UUID reviewer_id,String action,String reason,Instant created_at',
  LevelHistoryEntity:
    'UUID id,UUID user_id,Integer old_level,Integer new_level,UUID application_id,Instant created_at',
  AuditEntity:
    'UUID id,UUID actor_id,String action,UUID target_id,String detail,Instant created_at',
  ContentProjection:
    'UUID id,String kind,String namespace,String external_id,Integer current_version,String title,JsonNode payload,String checksum,String status,Integer version',
  ImportBatchEntity:
    'UUID id,UUID owner_id,String idempotency_key,String checksum,String status,JsonNode payload,JsonNode report,Instant created_at,Boolean allow_updates',
  ExamEntity:
    'UUID id,UUID user_id,UUID paper_id,JsonNode snapshot,JsonNode answers,JsonNode result,Instant started_at,Instant deadline,Instant submitted_at,String status',
  LearningProjection:
    'UUID user_id,UUID entity_id,Integer version,Boolean favorite,Boolean wrong,Integer progress,Instant updated_at,String title',
};
const camel = (s) => s.replace(/_([a-z])/g, (_, c) => c.toUpperCase());
const cap = (s) => s[0].toUpperCase() + s.slice(1);
const fields = (s) => s.split(',').map((x) => x.split(' '));
fs.mkdirSync(`${base}/java/cn/zhijie/pojo/entity`, { recursive: true });
fs.mkdirSync(`${base}/java/cn/zhijie/pojo/query`, { recursive: true });
for (const [name, def] of Object.entries(entities)) {
  const f = fields(def);
  fs.writeFileSync(
    `${base}/java/cn/zhijie/pojo/entity/${name}.java`,
    `package cn.zhijie.pojo.entity;\nimport java.util.UUID;import java.time.*;import com.fasterxml.jackson.databind.JsonNode;\n// 数据库实体或关联查询投影，仅用于持久化与业务层。\npublic class ${name} {\n${f.map(([t, n]) => `private ${t} ${camel(n)};\npublic ${t} get${cap(camel(n))}(){return ${camel(n)};}\npublic void set${cap(camel(n))}(${t} value){this.${camel(n)}=value;}`).join('\n')}\n}\n`,
  );
}
const results = {
  userByName: 'UserEntity',
  user: 'UserEntity',
  lockUser: 'UserEntity',
  users: 'UserEntity',
  certificates: 'CertificateEntity',
  certificate: 'CertificateEntity',
  targets: 'CertificateEntity',
  rules: 'MemberRuleEntity',
  badges: 'BadgeProjection',
  attachment: 'AttachmentEntity',
  contentAttachment: 'AttachmentEntity',
  application: 'ApplicationProjection',
  lockApplication: 'ApplicationProjection',
  applications: 'ApplicationProjection',
  verifications: 'VerificationEntity',
  reviews: 'ReviewEntity',
  levelHistories: 'LevelHistoryEntity',
  audits: 'AuditEntity',
  contents: 'ContentProjection',
  content: 'ContentProjection',
  publishedContent: 'ContentProjection',
  findContent: 'ContentProjection',
  batchByKey: 'ImportBatchEntity',
  batch: 'ImportBatchEntity',
  lockBatch: 'ImportBatchEntity',
  batches: 'ImportBatchEntity',
  queuedBatches: 'ImportBatchEntity',
  attempt: 'ExamEntity',
  lockAttempt: 'ExamEntity',
  attempts: 'ExamEntity',
  learning: 'LearningProjection',
};
const commands = {
  insertUser: 'UUID id,String username,String password',
  updateUser: 'UUID id,String role,Boolean enabled',
  verifyIdentity: 'UUID id,String name,String evidence',
  saveCertificate: 'UUID id,String code,String name,String specialty,Integer level',
  saveRule: 'Integer level,String name,String icon,Integer sort,JsonNode entitlements',
  addTarget: 'UUID userId,UUID certificateId',
  removeTarget: 'UUID userId,UUID certificateId',
  insertAttachment:
    'UUID id,UUID ownerId,String name,String key,String type,Long size,String checksum,String access',
  contentAttachment: 'UUID ownerId,String checksum',
  audit: 'UUID id,UUID actor,String action,UUID target,String detail',
  insertApplication:
    'UUID id,UUID userId,UUID certificateId,String holder,String numberType,String number,String hash,LocalDate obtainedOn,UUID attachmentId,UUID auxiliaryId,String note,String autoResult,JsonNode autoDetails',
  resubmitApplication:
    'UUID id,UUID userId,UUID certificateId,String holder,String numberType,String number,String hash,LocalDate obtainedOn,UUID attachmentId,UUID auxiliaryId,String note,String autoResult,JsonNode autoDetails',
  duplicateCount: 'String hash,UUID id',
  setApplicationState: 'UUID id,String status,String official',
  insertVerification:
    'UUID id,UUID applicationId,UUID reviewer,String result,String source,String evidence,String ownership',
  insertReview: 'UUID id,UUID applicationId,UUID reviewer,String action,String reason',
  grantCertificate: 'UUID id,UUID userId,UUID certificateId,UUID applicationId,String hash',
  setLevel: 'UUID id,Integer level',
  levelHistory: 'UUID id,UUID userId,Integer old,Integer newLevel,UUID applicationId',
  findContent: 'String namespace,String kind,String externalId',
  insertEntity: 'UUID id,String kind,String namespace,String externalId',
  nextVersion: 'UUID id,Integer version',
  insertVersion: 'UUID id,Integer version,String title,JsonNode payload,String checksum,UUID actor',
  publish: 'UUID id,Integer version,String status',
  insertBatch: 'UUID id,UUID ownerId,String key,String checksum,JsonNode payload,JsonNode report',
  batchByKey: 'UUID ownerId,String key',
  finishBatch: 'UUID id,String status,JsonNode report',
  enqueueBatch: 'UUID id,Boolean allow',
  insertAttempt: 'UUID id,UUID userId,UUID paperId,JsonNode snapshot,Instant deadline',
  saveAnswers: 'UUID id,JsonNode answers',
  submitAttempt: 'UUID id,JsonNode result',
  saveLearning:
    'UUID userId,UUID entityId,Integer version,Boolean favorite,Boolean wrong,Integer progress',
};
for (const [name, def] of Object.entries(commands))
  fs.writeFileSync(
    `${base}/java/cn/zhijie/pojo/query/${cap(name)}Command.java`,
    `package cn.zhijie.pojo.query;import java.util.UUID;import java.time.*;import com.fasterxml.jackson.databind.JsonNode;\npublic record ${cap(name)}Command(${def}) {}\n`,
  );
const tableColumns = (name) => fields(entities[name]).map((x) => x[1]);
for (const file of fs.readdirSync(`${base}/java/cn/zhijie/dao`)) {
  const path = `${base}/java/cn/zhijie/dao/${file}`;
  let s = fs
    .readFileSync(path, 'utf8')
    .replace(
      'import java.util.*;',
      'import java.util.*;import cn.zhijie.pojo.entity.*;import cn.zhijie.pojo.query.*;',
    );
  for (const [method, type] of Object.entries(results))
    s = s
      .replace(new RegExp(`List<Map<String, Object>> ${method}\\(`), `List<${type}> ${method}(`)
      .replace(new RegExp(`Map<String, Object> ${method}\\(`), `${type} ${method}(`);
  for (const [method] of Object.entries(commands))
    s = s.replace(new RegExp(`(${method}\\()Map<String, Object> p`), `$1${cap(method)}Command p`);
  fs.writeFileSync(path, s);
  const xmlPath = `${base}/resources/mappers/${file.replace('.java', '.xml')}`;
  let xml = fs.readFileSync(xmlPath, 'utf8');
  const used = new Set();
  xml = xml.replace(
    /<(select|insert|update|delete)\b([^>]*\bid="([^"]+)"[^>]*)>([\s\S]*?)<\/\1>/g,
    (all, tag, attr, id, sql) => {
      const type = results[id];
      if (type) {
        used.add(type);
        attr = attr.replace('resultType="map"', `resultMap="${type}Map"`);
      }
      sql = sql.replaceAll('#{new}', '#{newLevel}');
      if (type) {
        const cols = tableColumns(type);
        if (type === 'ApplicationProjection')
          sql = sql.replace(
            /\ba\.\*/g,
            cols
              .filter((x) => !['certificate_name', 'username'].includes(x))
              .map((x) => 'a.' + x)
              .join(','),
          );
        else if (type === 'CertificateEntity')
          sql = sql.replace(/\bc\.\*/g, cols.map((x) => 'c.' + x).join(','));
        else if (type === 'ContentProjection')
          sql = sql.replace(
            /\be\.\*/g,
            ['id', 'kind', 'namespace', 'external_id', 'current_version']
              .map((x) => 'e.' + x)
              .join(','),
          );
        else if (type === 'LearningProjection')
          sql = sql.replace(
            /\bl\.\*/g,
            cols
              .filter((x) => x !== 'title')
              .map((x) => 'l.' + x)
              .join(','),
          );
        const tableCols =
          type === 'ApplicationProjection'
            ? cols.filter((x) => !['certificate_name', 'username'].includes(x))
            : cols;
        sql = sql
          .replace(
            /SELECT \*,(?:payload::text,report::text|snapshot::text,answers::text,result::text)/g,
            `SELECT ${tableCols.join(',')}`,
          )
          .replace(/SELECT \* FROM/g, `SELECT ${tableCols.join(',')} FROM`);
        sql = sql.replace(/(\b(?:v\.)?(?:payload|answers|result|report))::text/g, '$1');
      }
      for (const [t, n] of commands[id] ? fields(commands[id]) : [])
        if (t === 'JsonNode')
          sql = sql.replaceAll(
            `#{${n}}`,
            `#{${n},typeHandler=cn.zhijie.config.JsonNodeTypeHandler}`,
          );
      return `<${tag}${attr}>${sql}</${tag}>`;
    },
  );
  const maps = [...used]
    .map(
      (type) =>
        `<resultMap id="${type}Map" type="cn.zhijie.pojo.entity.${type}">\n${fields(entities[type])
          .map(
            ([t, n]) =>
              `<result column="${n}" property="${camel(n)}"${t === 'JsonNode' ? ' typeHandler="cn.zhijie.config.JsonNodeTypeHandler"' : ''}/>`,
          )
          .join('\n')}\n</resultMap>`,
    )
    .join('\n');
  xml = xml.replace(/(<mapper[^>]*>)/, `$1\n${maps}`);
  fs.writeFileSync(xmlPath, xml);
}
// 对现有 DAO 调用中的具名参数按定义转换，JSON 文档仍通过 JsonNode 表达。
function endParen(s, start) {
  let d = 1,
    quote = '',
    escape = false;
  for (let i = start; i < s.length; i++) {
    let c = s[i];
    if (quote) {
      if (escape) escape = false;
      else if (c === '\\') escape = true;
      else if (c === quote) quote = '';
      continue;
    }
    if (c === '"' || c === "'") {
      quote = c;
      continue;
    }
    if (c === '(') d++;
    if (c === ')' && !--d) return i;
  }
  throw Error('括号不匹配');
}
function split(s) {
  let d = 0,
    q = '',
    esc = false,
    start = 0,
    out = [];
  for (let i = 0; i < s.length; i++) {
    const c = s[i];
    if (q) {
      if (esc) esc = false;
      else if (c === '\\') esc = true;
      else if (c === q) q = '';
      continue;
    }
    if (c === '"' || c === "'") {
      q = c;
      continue;
    }
    if ('([{'.includes(c)) d++;
    if (')]}'.includes(c)) d--;
    if (c === ',' && d === 0) {
      out.push(s.slice(start, i).trim());
      start = i + 1;
    }
  }
  out.push(s.slice(start).trim());
  return out;
}
// 专用括号扫描，避免三元表达式和嵌套 JSON 影响参数顺序。
function matching(s, start) {
  let depth = 1,
    q = '',
    esc = false;
  for (let i = start; i < s.length; i++) {
    const c = s[i];
    if (q) {
      if (esc) esc = false;
      else if (c === '\\') esc = true;
      else if (c === q) q = '';
      continue;
    }
    if (c === '"' || c === "'") {
      q = c;
      continue;
    }
    if (c === '(') depth++;
    else if (c === ')' && --depth === 0) return i;
  }
  throw Error('括号不匹配');
}
for (const folder of ['service', 'task'])
  for (const file of fs.readdirSync(`${base}/java/cn/zhijie/${folder}`)) {
    const p = `${base}/java/cn/zhijie/${folder}/${file}`;
    let s = fs.readFileSync(p, 'utf8');
    for (const [method, def] of Object.entries(commands)) {
      const re = new RegExp(`\\b${method}\\(\\s*map\\(`, 'g');
      let match;
      while ((match = re.exec(s))) {
        const start = match.index + match[0].length,
          end = matching(s, start);
        const arr = split(s.slice(start, end));
        const pairs = new Map();
        for (let i = 0; i < arr.length; i += 2) pairs.set(arr[i].replaceAll('"', ''), arr[i + 1]);
        const args = fields(def).map(([t, n]) => {
          let v = pairs.get(n === 'newLevel' ? 'new' : n);
          if (v === undefined) throw Error(`${file} ${method} 缺少 ${n}`);
          if (t === 'JsonNode') {
            if (v.startsWith('json(')) v = v.slice(5, -1);
            v = `JSON.valueToTree(${v})`;
          }
          if (t === 'Long') v = `(long) (${v})`;
          return v;
        });
        const replacement = `${method}(new ${cap(method)}Command(${args.join(', ')})`;
        s = s.slice(0, match.index) + replacement + s.slice(end + 1);
        re.lastIndex = match.index + replacement.length;
      }
    }
    s = s.replace(
      /(package [\w.]+;)/,
      '$1\nimport cn.zhijie.pojo.entity.*;import cn.zhijie.pojo.query.*;import cn.zhijie.pojo.request.*;',
    );
    fs.writeFileSync(p, s);
  }
fs.writeFileSync('scripts/refactor-meta.json', JSON.stringify({ entities, commands, results }));
