import { spawnSync } from 'node:child_process';
const npm = process.env.npm_execpath;
if (!npm)
  throw new Error('Run through npm run verify or frontend/dev.ps1 verify.');
for (const task of ['format:check', 'lint', 'test', 'build']) {
  const result = spawnSync(process.execPath, [npm, 'run', task], {
    stdio: 'inherit',
  });
  if (result.status !== 0) process.exit(result.status ?? 1);
}
