import fs from 'node:fs';
const fields = new Set(
  Object.values(JSON.parse(fs.readFileSync('scripts/response-meta.json', 'utf8'))).flat(),
);
for (const file of ['mobile.tsx', 'admin.tsx', 'practice.tsx', 'shared.tsx']) {
  const path = 'frontend/src/' + file;
  let source = fs.readFileSync(path, 'utf8');
  for (const field of fields) {
    const snake = field.replace(/[A-Z]/g, (c) => '_' + c.toLowerCase());
    if (snake !== field) source = source.replace(new RegExp('\\b' + snake + '\\b', 'g'), field);
  }
  fs.writeFileSync(path, source);
}
