/**
 * PM2: uvicorn on 127.0.0.1 (TLS at nginx). Port must match `config.HABITTRACKER_PORT` (default 9210).
 * Typically started by `deploy.sh` in this directory.
 */
const path = require('path');

const appRoot = __dirname;
const port = process.env.HABITTRACKER_PORT || '9210';

module.exports = {
  apps: [
    {
      name: 'habittracker',
      cwd: appRoot,
      script: path.join(appRoot, 'venv/bin/python'),
      args: ['-m', 'uvicorn', 'main:app', '--host', '127.0.0.1', '--port', String(port)],
      instances: 1,
      autorestart: true,
      max_restarts: 20,
      min_uptime: '5s',
      env: {
        NODE_ENV: 'production',
        DEBUG: 'false',
      },
    },
  ],
};
