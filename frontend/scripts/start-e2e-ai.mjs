import { spawn } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const child = spawn('python', ['-m', 'app.server', '--port', '8092'], {
  cwd: fileURLToPath(new URL('../../ai-service/', import.meta.url)),
  stdio: 'inherit',
  windowsHide: true,
});

for (const signal of ['SIGINT', 'SIGTERM'])
  process.on(signal, () => child.kill(signal));
child.on('error', (error) => {
  console.error(error.message);
  process.exit(1);
});
child.on('exit', (code) => process.exit(code ?? 1));
