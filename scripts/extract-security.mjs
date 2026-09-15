import fs from 'node:fs';
const path = 'backend/src/main/java/cn/zhijie/config/SecurityConfig.java';
let s = fs.readFileSync(path, 'utf8');
s = s
  .replace(
    'import cn.zhijie.service.AuthService;',
    'import cn.zhijie.service.AuthService;\nimport cn.zhijie.security.*;\nimport org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;',
  )
  .replace('@Configuration', '@Configuration\n@EnableMethodSecurity')
  .replace(
    '        AuthService auth,',
    '        AuthService auth,\n        SecurityErrorHandler errors,',
  );
s = s.replace(
  '.exceptionHandling(x -> x.authenticationEntryPoint((req, res, e) -> res.sendError(401)))',
  '.exceptionHandling(x -> x.authenticationEntryPoint(errors).accessDeniedHandler(errors))',
);
const start = s.indexOf('                new OncePerRequestFilter()');
const end = s.indexOf('                UsernamePasswordAuthenticationFilter.class', start);
s =
  s.slice(0, start) + '                new JwtAuthenticationFilter(auth,errors),\n' + s.slice(end);
fs.writeFileSync(path, s);
const base = 'backend/src/main/java/cn/zhijie/service/';
for (const [file, roles] of [
  ['UserAdministrationService', 'ADMIN,REVIEWER'],
  ['CatalogService', 'ADMIN,EDITOR'],
  ['MemberRuleService', 'ADMIN'],
  ['AuditQueryService', 'ADMIN'],
]) {
  const path = base + file + '.java';
  let s = fs.readFileSync(path, 'utf8').replace(
    '@Service',
    `@org.springframework.security.access.prepost.PreAuthorize("hasAnyRole(${roles
      .split(',')
      .map((r) => "'" + r + "'")
      .join(',')})")\n@Service`,
  );
  fs.writeFileSync(path, s);
}
