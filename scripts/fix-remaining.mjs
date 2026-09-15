import fs from 'node:fs';
const root = 'backend/src/main/java/cn/zhijie/service/';
function edit(n, fn) {
  const p = root + n + '.java';
  fs.writeFileSync(p, fn(fs.readFileSync(p, 'utf8')));
}
edit('UserService', (s) =>
  s.replace(
    /return response\([\s\S]*?ProfileResponse.class\s*\);/,
    `return new ProfileResponse(a.id(),u.getUsername(),a.role(),u.getIdentityVerified(),u.getMemberLevel(),membershipMapper.badges(a.id()).stream().map(ResponseMapper::badge).toList(),learningMapper.targets(a.id()).stream().map(ResponseMapper::certificate).toList(),membershipMapper.rules().stream().map(ResponseMapper::memberRule).toList(),membershipMapper.levelHistories(a.id()).stream().map(ResponseMapper::levelHistory).toList());`,
  ),
);
edit('ContentManagementService', (s) =>
  s
    .replace('p.containsKey("id") ? uuid(p.id())', 'p.id()!=null ? p.id()')
    .replace('((Number) p.level()).intValue()', 'p.level()')
    .replace('return response(map("id", id), IdResponse.class);', 'return new IdResponse(id);')
    .replace(
      'JSON.valueToTree(p.getOrDefault("entitlements", Map.of()))',
      'p.entitlements()==null?JSON.createObjectNode():p.entitlements()',
    ),
);
edit('LearningService', (s) =>
  s
    .replace(
      '((Number) p.getOrDefault("progress", 0)).intValue()',
      'p.progress()==null?0:p.progress()',
    )
    .replace('.orElse(Map.of())', '.orElseGet(LearningProjection::new)')
    .replace('prior.getOrDefault("wrong", false)', 'Boolean.TRUE.equals(prior.getWrong())'),
);
edit('ExamService', (s) => s.replaceAll('.document(', '.map('));
