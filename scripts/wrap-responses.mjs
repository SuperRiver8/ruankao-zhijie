import fs from 'node:fs';
const dir = 'backend/src/main/java/cn/zhijie/controller/';
for (const file of fs.readdirSync(dir)) {
  let s = fs.readFileSync(dir + file, 'utf8');
  const re = /    (List<\w+>|\w+Response|void) (\w+)\([^]*?\)\s*(?:throws Exception\s*)?\{/g;
  const changes = [];
  let m;
  while ((m = re.exec(s))) {
    if (m[2] === 'dictionary') continue;
    let end = re.lastIndex,
      d = 1;
    while (d) {
      if (s[end] === '{') d++;
      if (s[end] === '}') d--;
      end++;
    }
    let block = s.slice(m.index, end);
    const type = m[1];
    block = block.replace(
      '    ' + type + ' ' + m[2],
      '    ApiResponse<' + (type === 'void' ? 'Void' : type) + '> ' + m[2],
    );
    if (type === 'void') block = block.slice(0, -1) + '    return ApiResponse.ok(null);\n    }';
    else block = block.replace(/return ([^]*?);/, 'return ApiResponse.ok($1);');
    changes.push([m.index, end, block]);
    re.lastIndex = end;
  }
  for (const [start, end, block] of changes.reverse()) s = s.slice(0, start) + block + s.slice(end);
  fs.writeFileSync(dir + file, s);
}
