/**
 * CMS Install Test - Fresh install and start for each database backend
 *
 * Usage:
 *   TEST_DB_TYPE=h2 npm test -- tests/install.spec.js
 *   TEST_DB_TYPE=mysql npm test -- tests/install.spec.js
 *   TEST_DB_TYPE=postgresql npm test -- tests/install.spec.js
 *   TEST_DB_TYPE=sqlserver npm test -- tests/install.spec.js
 *
 * Environment Variables:
 *   TEST_DB_TYPE        - Database type: h2, mysql, postgresql, sqlserver (default: h2)
 *   TEST_INSTALL_DIR   - Installation directory (default: /tmp/perc-test-{db_type})
 *   TEST_CMS_PORT      - CMS port (default: 19992)
 *   TEST_SKIP_INSTALL  - Skip install/start, test running CMS only (default: false)
 *   PERC_DIST_PATH     - Path to perc-distribution-tree/target/*.zip (auto-detected)
 */

const { test, expect } = require('@playwright/test');
const { execSync } = require('child_process');
const path = require('path');
const fs = require('fs');

const DB_TYPE = process.env.TEST_DB_TYPE || 'h2';
const INSTALL_DIR = process.env.TEST_INSTALL_DIR || '/tmp/perc-test-' + DB_TYPE;
const CMS_PORT = process.env.TEST_CMS_PORT || '19992';
const SKIP_INSTALL = process.env.TEST_SKIP_INSTALL === 'true';

const DB_CONFIGS = {
  h2: {
    dbType: 'h2',
    dbName: '',
    dbSchema: '',
    dbUser: '',
    dbPassword: ''
  },
  mysql: {
    dbType: 'mysql',
    dbHost: 'localhost',
    dbPort: '3306',
    dbName: 'percdb',
    dbUser: 'percuser',
    dbPassword: 'PercUser123!'
  },
  postgresql: {
    dbType: 'postgresql',
    dbHost: 'localhost',
    dbPort: '5432',
    dbName: 'percdb',
    dbUser: 'percuser',
    dbPassword: 'PercUser123!'
  },
  sqlserver: {
    dbType: 'sqlserver',
    dbHost: 'localhost',
    dbPort: '1433',
    dbName: 'percdb',
    dbUser: 'percuser',
    dbPassword: 'PercUser123!'
  }
};

function getDbConfig(dbType) {
  const config = DB_CONFIGS[dbType];
  if (!config) {
    throw new Error(`Unknown database type: ${dbType}. Supported: ${Object.keys(DB_CONFIGS).join(', ')}`);
  }
  return config;
}

function startDatabaseContainer(dbType) {
  if (dbType === 'h2') {
    console.log('H2 is embedded, no container needed');
    return;
  }

  const containers = {
    mysql: { image: 'mysql:8.0', port: 3306, name: 'perc-test-mysql' },
    postgresql: { image: 'postgres:16', port: 5432, name: 'perc-test-postgres' },
    sqlserver: { image: 'mcr.microsoft.com/mssql/server:2022-latest', port: 1433, name: 'perc-test-sqlserver' }
  };

  const container = containers[dbType];
  if (!container) {
    throw new Error(`No container config for ${dbType}`);
  }

  console.log(`Starting ${container.image} container...`);

  try {
    execSync(`docker stop ${container.name} 2>/dev/null || true`, { stdio: 'ignore' });
    execSync(`docker rm ${container.name} 2>/dev/null || true`, { stdio: 'ignore' });
  } catch (e) {
    // Container might not exist
  }

  const env = {
    mysql: `MYSQL_ROOT_PASSWORD=RootUser123! MYSQL_USER=percuser MYSQL_PASSWORD=PercUser123! MYSQL_DATABASE=percdb`,
    postgresql: `POSTGRES_USER=percuser POSTGRES_PASSWORD=PercUser123! POSTGRES_DB=percdb`,
    sqlserver: `ACCEPT_EULA=Y MSSQL_SA_PASSWORD=PercUser123! MSSQL_PID=Developer`
  };

  const portMap = `${container.port}:${container.port}`;
  const cmd = dbType === 'sqlserver'
    ? `docker run -d --name ${container.name} -e "${env[dbType]}" -p ${portMap} ${container.image}`
    : `docker run -d --name ${container.name} -e "${env[dbType]}" -p ${portMap} ${container.image}`;

  execSync(cmd, { stdio: 'inherit' });

  console.log(`Waiting for ${dbType} to be ready...`);
  execSync(`sleep 10`, { stdio: 'ignore' });
}

function installCMS(dbType) {
  const config = getDbConfig(dbType);

  console.log(`Installing CMS to ${dbType} at ${INSTALL_DIR}...`);

  if (fs.existsSync(INSTALL_DIR)) {
    console.log(`Removing existing installation at ${INSTALL_DIR}...`);
    execSync(`rm -rf ${INSTALL_DIR}`, { stdio: 'inherit' });
  }

  execSync(`mkdir -p ${INSTALL_DIR}`, { stdio: 'inherit' });

  const dbArgs = [];
  if (dbType !== 'h2') {
    dbArgs.push(`--db.type=${config.dbType}`);
    dbArgs.push(`--db.host=${config.dbHost}`);
    dbArgs.push(`--db.port=${config.dbPort}`);
    dbArgs.push(`--db.name=${config.dbName}`);
    dbArgs.push(`--db.user=${config.dbUser}`);
    dbArgs.push(`--db.password=${config.dbPassword}`);
  } else {
    dbArgs.push('--db.type=h2');
  }

  const installCmd = [
    'java',
    '-jar', 'perc-preinstall.jar',
    INSTALL_DIR,
    '--silent',
    ...dbArgs,
    `-Dperc.java.home=${process.env.JAVA_HOME || '/usr/lib/jvm/java-21'}`
  ].join(' ');

  console.log('Running install:', installCmd);
  execSync(installCmd, { stdio: 'inherit', cwd: '/opt/Percussion' });

  console.log('Installation complete');
}

function startCMS() {
  console.log('Starting CMS...');
  const startScript = path.join(INSTALL_DIR, 'StartJetty.sh');
  if (!fs.existsSync(startScript)) {
    throw new Error(`Start script not found: ${startScript}`);
  }

  execSync(`nohup ${startScript} > ${INSTALL_DIR}/jetty.log 2>&1 &`, {
    stdio: 'ignore',
    shell: true
  });

  console.log('Waiting for CMS to start...');
  const maxWait = 300;
  const startTime = Date.now();

  while (Date.now() - startTime < maxWait * 1000) {
    try {
      const response = require('http').request({
        hostname: 'localhost',
        port: CMS_PORT,
        path: '/Rhythmyx/login',
        method: 'GET'
      }, (res) => {
        if (res.statusCode === 200 || res.statusCode === 302) {
          console.log('CMS is up!');
          return;
        }
      });
      response.on('error', () => {});
      response.end();
    } catch (e) {
      // Ignore
    }
    execSync('sleep 5', { stdio: 'ignore' });
  }

  throw new Error('CMS failed to start within timeout');
}

test.describe('CMS Install Tests', () => {
  test(`Install and start CMS with ${DB_TYPE} database`, async ({ page }) => {
    if (SKIP_INSTALL) {
      console.log('Skipping install, testing running CMS...');
    } else {
      startDatabaseContainer(DB_TYPE);
      installCMS(DB_TYPE);
      startCMS();
    }

    const cmsUrl = `http://localhost:${CMS_PORT}/Rhythmyx/login`;
    console.log(`Navigating to ${cmsUrl}...`);

    await page.goto(cmsUrl, { timeout: 60000 });

    const title = await page.title();
    console.log('Page title:', title);

    await expect(page).toHaveTitle(/Percussion|Login|Rhythmyx/i, { timeout: 30000 });

    console.log('Login page verified successfully!');
  });
});
