// 从项目根目录执行统一格式化，避免调用者所在目录影响配置和忽略规则。
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const root = fileURLToPath(new URL('../', import.meta.url));
const result = spawnSync(
  process.execPath,
  [
    'frontend/customer/node_modules/prettier/bin/prettier.cjs',
    process.argv.includes('--check') ? '--check' : '--write',
    '**/*.{java,ts,tsx,js,mjs,json,xml,yml,yaml,css,html,md,sql}',
  ],
  { cwd: root, stdio: 'inherit' },
);
if (result.error) throw result.error;
process.exit(result.status ?? 1);

