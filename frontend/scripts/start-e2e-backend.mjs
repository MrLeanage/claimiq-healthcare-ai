import { spawn } from 'node:child_process';
import { existsSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import path from 'node:path';
const jar = fileURLToPath(
  new URL('../../backend/target/backend-0.0.1-SNAPSHOT.jar', import.meta.url),
);
if (!existsSync(jar))
  throw new Error('Build backend first: backend/dev.ps1 package');
const java = process.env.JAVA_HOME
  ? path.join(
      process.env.JAVA_HOME,
      'bin',
      process.platform === 'win32' ? 'java.exe' : 'java',
    )
  : 'java';
const child = spawn(
  java,
  [
    '-jar',
    jar,
    '--spring.profiles.active=demo',
    '--server.port=8091',
    '--claimiq.ai.base-url=http://127.0.0.1:8092',
  ],
  {
    stdio: 'inherit',
    env: {
      ...process.env,
      CLAIMIQ_DEMO_JWT_SECRET: Buffer.alloc(32, 7).toString('base64'),
    },
  },
);
for (const signal of ['SIGINT', 'SIGTERM'])
  process.on(signal, () => child.kill(signal));
child.on('exit', (code) => process.exit(code ?? 1));
