import fs from 'node:fs';
const base = 'backend/src/main/java/cn/zhijie/';
const fields = {
  LoginRequest: { username: '@NotBlank @Size(max=40)', password: '@NotBlank @Size(max=100)' },
  RefreshRequest: { refreshToken: '@NotBlank' },
  ApplicationRequest: {
    certificateId: '@NotNull',
    holderName: '@NotBlank @Size(max=100)',
    number: '@NotBlank @Size(max=100)',
    numberType: '@NotBlank',
    obtainedOn: '@NotNull @PastOrPresent',
    attachmentId: '@NotNull',
    note: '@Size(max=4000)',
  },
  CertificateRequest: {
    code: '@NotBlank',
    name: '@NotBlank',
    specialty: '@NotBlank',
    level: '@NotNull @Min(1) @Max(3)',
  },
  CertificateReviewRequest: { action: '@NotBlank', reason: '@NotBlank @Size(max=4000)' },
  ContentRequest: {
    kind: '@NotBlank',
    namespace: '@NotBlank',
    externalId: '@NotBlank',
    payload: '@NotNull',
  },
  ContentReviewRequest: { version: '@NotNull @Min(1)', status: '@NotBlank', reason: '@NotBlank' },
  IdentityRequest: { holderName: '@NotBlank', evidence: '@NotBlank' },
  LearningRequest: { favorite: '@NotNull', progress: '@Min(0) @Max(100)' },
  MemberRuleRequest: { name: '@NotBlank', icon: '@NotBlank', sort: '@NotNull' },
  PracticeRequest: { count: '@Min(1) @Max(100)' },
  UserUpdateRequest: { role: '@NotBlank', enabled: '@NotNull' },
};
for (const [name, props] of Object.entries(fields)) {
  const file = base + 'pojo/request/' + name + '.java';
  let s = fs
    .readFileSync(file, 'utf8')
    .replace(
      'package cn.zhijie.pojo.request;',
      'package cn.zhijie.pojo.request;\nimport jakarta.validation.constraints.*;',
    );
  for (const [name, annotations] of Object.entries(props))
    s = s.replace(new RegExp('(\\w+ ' + name + ')(?=[,)\\s])'), annotations + ' $1');
  fs.writeFileSync(file, s);
}
for (const name of fs.readdirSync(base + 'controller')) {
  const file = base + 'controller/' + name;
  let s = fs.readFileSync(file, 'utf8');
  if (s.includes('@RequestBody'))
    s = s
      .replace(
        'package cn.zhijie.controller;',
        'package cn.zhijie.controller;\nimport jakarta.validation.Valid;',
      )
      .replaceAll('@RequestBody', '@Valid @RequestBody');
  fs.writeFileSync(file, s);
}
const file = 'backend/src/test/java/cn/zhijie/CoreRulesTest.java';
let s = fs
  .readFileSync(file, 'utf8')
  .replace(
    'import cn.zhijie.pojo.Actor;',
    'import cn.zhijie.pojo.Actor;\nimport cn.zhijie.pojo.entity.*;\nimport cn.zhijie.pojo.request.CertificateReviewRequest;',
  );
s = s
  .replaceAll(
    'map("user_id", owner, "status", "PENDING", "official_result", "UNVERIFIED")',
    'application(owner)',
  )
  .replaceAll('map("user_id", owner, "status", "PENDING")', 'application(owner)')
  .replaceAll('map("identity_verified", true)', 'identity(true)')
  .replaceAll('map("identity_verified", false)', 'identity(false)')
  .replaceAll('anyMap()', 'any()');
for (const result of ['UNCERTAIN', 'MATCH'])
  s = s.replace(
    'map("action", "APPROVED", "reason", "测试", "officialResult", "' + result + '")',
    'new CertificateReviewRequest("APPROVED", "测试", "' + result + '", null, null, null, false)',
  );
s = s.replace(
  'map("action", "APPROVED", "reason", "测试")',
  'new CertificateReviewRequest("APPROVED", "测试", null, null, null, null, false)',
);
s = s.replace(
  '    private MembershipService membership(',
  `    private ApplicationProjection application(UUID owner) {
        var row=new ApplicationProjection();row.setUserId(owner);row.setStatus("PENDING");row.setOfficialResult("UNVERIFIED");return row;
    }
    private UserEntity identity(boolean verified) {var row=new UserEntity();row.setIdentityVerified(verified);return row;}
    private MembershipService membership(`,
);
fs.writeFileSync(file, s);
