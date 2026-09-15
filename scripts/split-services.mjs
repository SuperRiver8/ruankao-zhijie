import fs from 'node:fs';
const root = 'backend/src/main/java/cn/zhijie/';
const read = (p) => fs.readFileSync(root + p, 'utf8');
const write = (p, s) => fs.writeFileSync(root + p, s);
function method(source, name) {
  const re = new RegExp('    public [^\\n]+ ' + name + '\\(');
  const start = source.search(re);
  if (start < 0) throw new Error(name);
  const open = source.indexOf('{', start);
  let depth = 1,
    end = open + 1;
  while (depth) {
    if (source[end] === '{') depth++;
    if (source[end] === '}') depth--;
    end++;
  }
  return source.slice(start, end);
}
const source = read('service/ContentManagementService.java');
const imports = `package cn.zhijie.service;
import static cn.zhijie.util.Support.*;
import cn.zhijie.util.ResponseMapper;
import cn.zhijie.dao.*;
import cn.zhijie.pojo.Actor;
import cn.zhijie.pojo.query.*;
import cn.zhijie.pojo.request.*;
import cn.zhijie.pojo.response.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
`;
for (const [name, fields, methods] of [
  [
    'UserAdministrationService',
    [
      ['UserMapper', 'userMapper'],
      ['Audit', 'audit'],
    ],
    ['users', 'user'],
  ],
  [
    'CatalogService',
    [
      ['CertificateMapper', 'certificateMapper'],
      ['Audit', 'audit'],
    ],
    ['certificate'],
  ],
  [
    'MemberRuleService',
    [
      ['MembershipMapper', 'membershipMapper'],
      ['Audit', 'audit'],
    ],
    ['rule'],
  ],
  ['AuditQueryService', [['AuditMapper', 'auditMapper']], ['audits']],
]) {
  write(
    'service/' + name + '.java',
    imports +
      '@Service\npublic class ' +
      name +
      ' {\n' +
      fields.map(([t, n]) => 'private final ' + t + ' ' + n + ';').join('\n') +
      '\npublic ' +
      name +
      '(' +
      fields.map(([t, n]) => t + ' ' + n).join(',') +
      '){' +
      fields.map(([, n]) => 'this.' + n + '=' + n + ';').join('') +
      '}\n' +
      methods
        .map(
          (n) =>
            (['user', 'certificate', 'rule'].includes(n) ? '    @Transactional\n' : '') +
            method(source, n),
        )
        .join('\n') +
      '\n}\n',
  );
}
let controller = read('controller/ContentController.java');
const controllerHeader = controller
  .slice(0, controller.indexOf('@RestController'))
  .replace('import cn.zhijie.service.ContentManagementService;', 'import cn.zhijie.service.*;');
function endpoint(source, name) {
  const m = new RegExp('    (?:List<\\w+>|void|IdResponse) ' + name + '\\(').exec(source);
  if (!m) throw new Error(name);
  const start = source.lastIndexOf('    @', m.index);
  const open = source.indexOf('{', m.index);
  let d = 1,
    e = open + 1;
  while (d) {
    if (source[e] === '{') d++;
    if (source[e] === '}') d--;
    e++;
  }
  return source.slice(start, e);
}
for (const [name, service, names] of [
  ['UserAdministrationController', 'UserAdministrationService', ['users', 'user']],
  ['CatalogController', 'CatalogService', ['certificate']],
  ['MemberRuleController', 'MemberRuleService', ['rule']],
  ['AuditController', 'AuditQueryService', ['audits']],
]) {
  const endpoints = names.map((n) => endpoint(controller, n));
  write(
    'controller/' + name + '.java',
    controllerHeader +
      '@RestController\n@RequestMapping("/api")\npublic class ' +
      name +
      ' {\nprivate final ' +
      service +
      ' application;\npublic ' +
      name +
      '(' +
      service +
      ' application){this.application=application;}\n' +
      endpoints.join('\n') +
      '\n}\n',
  );
  for (const e of endpoints) controller = controller.replace(e, '');
}
controller = controller
  .replaceAll('ContentManagementService', 'ContentService')
  .replace('application.list(a, kind)', 'application.list(a, kind, false)')
  .replace('application.adminList(a, kind)', 'application.list(a, kind, true)')
  .replace('application.create(a, p)', 'application.save(a, null, p)')
  .replace('application.edit(a, id, p)', 'application.save(a, id, p)')
  .replace('application.review(a, id, p)', 'application.publish(a, id, p)');
write('controller/ContentController.java', controller);
fs.unlinkSync(root + 'service/ContentManagementService.java');
let user = read('service/UserService.java');
for (const n of ['register', 'login', 'refresh', 'logout'])
  user = user.replace(method(user, n), '');
user = user
  .replace('    private final AuthService auth;', '')
  .replace('        AuthService auth,', '')
  .replace('        this.auth = auth;', '');
write('service/UserService.java', user);
let auth = read('controller/AuthController.java').replace(
  'import cn.zhijie.service.UserService;',
  'import cn.zhijie.service.UserService;\nimport cn.zhijie.service.AuthService;',
);
auth = auth
  .replace(
    '    private final UserService application;',
    '    private final UserService application;\n    private final AuthService auth;',
  )
  .replace(
    'AuthController(UserService application)',
    'AuthController(UserService application, AuthService auth)',
  )
  .replace(
    'this.application = application;',
    'this.application = application;\n        this.auth = auth;',
  );
for (const n of ['register', 'login', 'logout'])
  auth = auth.replace('application.' + n + '(', 'auth.' + n + '(');
auth = auth.replace('application.refresh(p)', 'auth.refresh(p.refreshToken())');
write('controller/AuthController.java', auth);
