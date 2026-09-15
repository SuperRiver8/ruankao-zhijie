import fs from 'node:fs';
const root = 'backend/src/main/java/cn/zhijie/';
const read = (p) => fs.readFileSync(root + p, 'utf8');
const write = (p, s) => fs.writeFileSync(root + p, s);
function method(s, name) {
  const start = s.search(new RegExp('    public [^\\n]+ ' + name + '\\('));
  if (start < 0) throw Error(name);
  const open = s.indexOf('{', start);
  let e = open + 1,
    d = 1;
  while (d) {
    if (s[e] === '{') d++;
    if (s[e] === '}') d--;
    e++;
  }
  return s.slice(start, e);
}
let learning = read('service/LearningService.java');
let exam = read('service/ExamService.java');
exam = exam.replace(
  '    private final ContentMapper',
  method(learning, 'list') + '\n    private final ContentMapper',
);
for (const n of ['start', 'practice', 'selfGrade', 'list', 'read', 'save', 'submit'])
  learning = learning.replace(method(learning, n), '');
for (const line of [
  '    private final ExamService service;',
  '    private final ExamMapper examMapper;',
  '        ExamService service,',
  '        ExamMapper examMapper,',
  '        this.service = service;',
  '        this.examMapper = examMapper;',
])
  learning = learning.replace(line, '');
write('service/LearningService.java', learning);
write('service/ExamService.java', exam);
let controller = read('controller/ExamController.java');
const pos = controller.indexOf('    @GetMapping("/learning")');
const endpoints = controller.slice(pos, controller.lastIndexOf('}'));
write(
  'controller/LearningController.java',
  controller.slice(0, controller.indexOf('@RestController')) +
    '@RestController\n@RequestMapping("/api")\npublic class LearningController {\nprivate final LearningService application;\npublic LearningController(LearningService application){this.application=application;}\n' +
    endpoints +
    '}\n',
);
controller = (controller.slice(0, pos) + '}\n').replaceAll('LearningService', 'ExamService');
write('controller/ExamController.java', controller);
let management = read('service/ImportManagementService.java');
let imports = read('service/ImportService.java');
imports = imports
  .replace(
    'import cn.zhijie.dao.ContentMapper;',
    'import cn.zhijie.dao.ContentMapper;\nimport cn.zhijie.dao.CertificateMapper;',
  )
  .replace(
    '    private final UserMapper',
    '    private final CertificateMapper certificateMapper;\n    private final UserMapper',
  )
  .replace(
    '        UserMapper userMapper,',
    '        CertificateMapper certificateMapper,\n        UserMapper userMapper,',
  )
  .replace(
    '        this.userMapper = userMapper;',
    '        this.certificateMapper = certificateMapper;\n        this.userMapper = userMapper;',
  );
imports = imports.replace(
  '    public JsonNode parse(',
  ['schema', 'dictionary', 'list'].map((n) => method(management, n)).join('\n') +
    '\n    public JsonNode parse(',
);
write('service/ImportService.java', imports);
write(
  'controller/ImportController.java',
  read('controller/ImportController.java')
    .replaceAll('ImportManagementService', 'ImportService')
    .replace(
      'application.commit(a, id, p)',
      'application.commit(a, id, Boolean.TRUE.equals(p.allowUpdates()))',
    ),
);
fs.unlinkSync(root + 'service/ImportManagementService.java');
