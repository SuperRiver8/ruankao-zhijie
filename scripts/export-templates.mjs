// 导出供 AI 使用的 Schema、分类型示例和 ZIP 分文件内容。
import fs from 'node:fs';
const root = 'backend/src/main/resources/import-schema.json';
const schema = JSON.parse(fs.readFileSync(root, 'utf8'));
const text = { type: 'string', minLength: 1 };
const texts = { type: 'array', items: text };
const base = {
  type: 'object',
  required: ['title'],
  properties: {
    title: { ...text, maxLength: 300 },
    body: { type: 'string' },
    knowledgeCodes: texts,
    attachmentIds: { type: 'array', items: { type: 'string', format: 'uuid' } },
    attachmentPaths: texts,
    certificateCode: text,
    syllabusCode: text,
    subject: text,
    chapter: text,
  },
};
schema.$defs = {
  KNOWLEDGE: base,
  SYLLABUS: {
    allOf: [
      base,
      {
        required: ['version', 'subjects'],
        properties: {
          version: text,
          subjects: {
            type: 'array',
            minItems: 1,
            items: {
              type: 'object',
              required: ['code', 'name', 'chapters'],
              properties: {
                code: text,
                name: text,
                chapters: {
                  type: 'array',
                  items: {
                    type: 'object',
                    required: ['code', 'name'],
                    properties: { code: text, name: text },
                  },
                },
              },
            },
          },
        },
      },
    ],
  },
  MAPPING: {
    allOf: [
      base,
      {
        required: ['syllabusCode', 'subject', 'chapter', 'knowledgeCodes'],
        properties: { knowledgeCodes: { ...texts, minItems: 1 } },
      },
    ],
  },
  QUESTION: {
    allOf: [
      base,
      {
        required: ['type', 'stem', 'difficulty'],
        properties: {
          type: { enum: ['SINGLE', 'MULTIPLE', 'BOOLEAN', 'FILL', 'SHORT', 'CASE', 'ESSAY'] },
          stem: text,
          difficulty: { type: 'integer', minimum: 1, maximum: 5 },
          answer: { ...texts, minItems: 1 },
          options: {
            type: 'array',
            minItems: 2,
            items: { type: 'object', required: ['id', 'text'], properties: { id: text, text } },
          },
          children: { type: 'array', minItems: 1, items: { $ref: '#/$defs/QUESTION' } },
          rubric: {
            anyOf: [text, { type: 'array', minItems: 1 }, { type: 'object', minProperties: 1 }],
          },
        },
        allOf: [
          {
            if: { properties: { type: { enum: ['SINGLE', 'MULTIPLE'] } } },
            then: { required: ['answer', 'options'] },
          },
          {
            if: { properties: { type: { enum: ['BOOLEAN', 'FILL'] } } },
            then: { required: ['answer'] },
          },
          {
            if: { properties: { type: { enum: ['SHORT', 'ESSAY'] } } },
            then: { required: ['rubric'] },
          },
          { if: { properties: { type: { const: 'CASE' } } }, then: { required: ['children'] } },
        ],
      },
    ],
  },
  MATERIAL: {
    allOf: [
      base,
      {
        anyOf: [
          { required: ['body'], properties: { body: text } },
          { required: ['attachmentIds'], properties: { attachmentIds: { minItems: 1 } } },
          { required: ['attachmentPaths'], properties: { attachmentPaths: { minItems: 1 } } },
        ],
      },
    ],
  },
  PAPER: {
    allOf: [
      base,
      {
        required: ['durationMinutes'],
        properties: {
          durationMinutes: { type: 'integer', minimum: 1, maximum: 600 },
          points: { type: 'number', exclusiveMinimum: 0 },
          count: { type: 'integer', minimum: 1, maximum: 200 },
          questionIds: {
            type: 'array',
            minItems: 1,
            maxItems: 200,
            uniqueItems: true,
            items: { type: 'string', format: 'uuid' },
          },
        },
        oneOf: [
          { required: ['count'], not: { required: ['questionIds'] } },
          { required: ['questionIds'], not: { required: ['count'] } },
        ],
      },
    ],
  },
};
schema.properties.entries.items.allOf = Object.keys(schema.$defs).map((kind) => ({
  if: { properties: { kind: { const: kind } } },
  then: { properties: { payload: { $ref: `#/$defs/${kind}` } } },
}));
fs.writeFileSync(root, JSON.stringify(schema, null, 2) + '\n');
fs.mkdirSync('templates/schemas', { recursive: true });
fs.writeFileSync('templates/import-schema.json', JSON.stringify(schema, null, 2) + '\n');
const example = JSON.parse(fs.readFileSync('templates/example.json', 'utf8'));
for (const kind of Object.keys(schema.$defs)) {
  const copy = structuredClone(schema);
  copy.properties.entries.items.properties.kind = { const: kind };
  fs.writeFileSync(
    `templates/schemas/${kind.toLowerCase()}.schema.json`,
    JSON.stringify(copy, null, 2) + '\n',
  );
  fs.writeFileSync(
    `templates/${kind.toLowerCase()}.example.json`,
    JSON.stringify(
      { ...example, entries: example.entries.filter((e) => e.kind === kind) },
      null,
      2,
    ) + '\n',
  );
}
fs.mkdirSync('templates/zip-example', { recursive: true });
const files = [];
for (const [kind, name] of Object.entries({
  KNOWLEDGE: 'knowledge-points',
  SYLLABUS: 'syllabuses',
  MAPPING: 'syllabus-mappings',
  QUESTION: 'questions',
  MATERIAL: 'materials',
  PAPER: 'papers',
})) {
  files.push(`${name}.json`);
  fs.writeFileSync(
    `templates/zip-example/${name}.json`,
    JSON.stringify(
      example.entries.filter((e) => e.kind === kind),
      null,
      2,
    ) + '\n',
  );
}
fs.writeFileSync(
  'templates/zip-example/manifest.json',
  JSON.stringify({ templateVersion: '1.0', namespace: example.namespace, files }, null, 2) + '\n',
);
