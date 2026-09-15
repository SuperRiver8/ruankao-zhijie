import fs from 'node:fs';
const base = 'backend/src/main/java/cn/zhijie';
function edit(file, fn) {
  const path = `${base}/${file}.java`;
  fs.writeFileSync(path, fn(fs.readFileSync(path, 'utf8')));
}
edit('service/AuthService', (s) =>
  s
    .replaceAll('Map<String, Object> u', 'UserEntity u')
    .replace(
      /return cn\.zhijie\.util\.PojoMapper\.response\([\s\S]*?TokenResponse\.class\s*\);/,
      'return new TokenResponse(jwt.serialize(), sid + "." + refresh, 900);',
    ),
);
edit('service/AttachmentService', (s) =>
  s
    .replace(/Map<String, Object> (ownedPrivate|authorize)\(/g, 'AttachmentEntity $1(')
    .replace('path(Map<String, Object> f)', 'path(AttachmentEntity f)')
    .replace(
      /return response\(\s*map\(\s*"id",\s*existing.getId\(\),[\s\S]*?AttachmentResponse.class\s*\);/,
      'return new AttachmentResponse(existing.getId(), name, type, b.length);',
    )
    .replace(
      /return response\(\s*map\("id", id,[\s\S]*?AttachmentResponse.class\s*\);/,
      'return new AttachmentResponse(id, name, type, b.length);',
    ),
);
edit('service/ContentService', (s) => {
  s = s
    .replace(
      /return response\(\s*map\("id", existing.getId\(\), "skipped", true\),\s*SaveContentResponse.class\s*\);/,
      'return new SaveContentResponse(existing.getId(), null, true);',
    )
    .replace(
      /return response\(\s*map\("id", id, "skipped", true\),\s*SaveContentResponse.class\s*\);/,
      'return new SaveContentResponse(id, null, true);',
    )
    .replace(
      /return response\(map\("id", id, "version", version\), SaveContentResponse.class\);/,
      'return new SaveContentResponse(id, version, false);',
    );
  const start = s.indexOf('                var r = new LinkedHashMap<>(row);'),
    end = s.indexOf('\n            })', start);
  s =
    s.slice(0, start) +
    `                var payload = row.getPayload().deepCopy();
                if (!admin && Set.of("QUESTION", "PAPER").contains(row.getKind())) stripAnswers(payload);
                row.setPayload(payload);
                return ResponseMapper.content(row);` +
    s.slice(end);
  return s.replace('((Number) p.version()).intValue()', 'p.version()');
});
edit('service/ImportService', (s) =>
  s
    .replaceAll('List<Map<String, Object>> report', 'List<ImportReportItem> report')
    .replace(
      /report.add\(\s*map\([\s\S]*?\)\s*\);/,
      'report.add(new ImportReportItem(index++, kind, external, status, message));',
    )
    .replaceAll('r.getStatus()', 'r.status()')
    .replace(
      /response\((old|batch), ImportBatchResponse.class\)/g,
      'ResponseMapper.importBatch($1)',
    )
    .replaceAll(
      'response(importMapper.batch(id), ImportBatchResponse.class)',
      'ResponseMapper.importBatch(importMapper.batch(id))',
    ),
);
// 将实体列表直接通过显式转换生成响应。
const names = {
  CertificateResponse: 'certificate',
  ContentResponse: 'content',
  ImportBatchResponse: 'importBatch',
  UserResponse: 'user',
  AuditResponse: 'audit',
  ExamSummaryResponse: 'examSummary',
  LearningResponse: 'learning',
};
for (const file of [
  'UserService',
  'ContentManagementService',
  'ImportManagementService',
  'LearningService',
])
  edit(`service/${file}`, (s) => {
    for (const [type, method] of Object.entries(names))
      s = s.replace(
        new RegExp(`responses\\(([\\w]+Mapper\\.[\\w]+\\([^;]*?\\)), ${type}.class\\)`, 'g'),
        `$1.stream().map(ResponseMapper::${method}).toList()`,
      );
    return s;
  });
