const fs = require('fs');
const path = require('path');

const envPath = path.resolve(__dirname, '../../../.env');
require('dotenv').config({ path: envPath });

const INSTALL_PATH = process.env.DEV_PERCUSSION_INSTALL;
const DTS_INSTALL_PATH = process.env.DEV_PERCUSSION_DTS_INSTALL;
const ADMIN_USERNAME = process.env.ADMIN_USERNAME || 'Admin';
const USERNAME_EDITOR = process.env.EDITOR_USERNAME || 'Editor';
const USERNAME_CONTRIBUTOR = process.env.CONTRIBUTOR_USERNAME || 'Contributor';

const users = ['Admin', 'Editor', 'Contributor'];

function getUrlFromInstall(installPath, propsFile, portKey) {
  const filePath = path.join(installPath, propsFile);
  
  if (!fs.existsSync(filePath)) {
    throw new Error(`${propsFile} not found at: ${filePath}`);
  }
  
  const content = fs.readFileSync(filePath, 'utf-8');
  const portMatch = content.match(new RegExp(`${portKey}=(\\d+)`));
  
  if (!portMatch) {
    throw new Error(`${portKey} not found in ${propsFile}`);
  }
  
  const port = portMatch[1];
  return `http://localhost:${port}`;
}

function getPasswordsFromInstall(installPath) {
  const passwordFile = path.join(installPath, 'var/config/generated/passwords');
  
  if (!fs.existsSync(passwordFile)) {
    throw new Error(`Password file not found at: ${passwordFile}`);
  }
  
  const content = fs.readFileSync(passwordFile, 'utf-8');
  const passwords = {};
  
  users.forEach(user => {
    const match = content.match(new RegExp(`${user}=([^\\n]+)`));
    if (match) {
      passwords[user] = match[1].trim();
    }
  });
  
  if (Object.keys(passwords).length === 0) {
    throw new Error('No passwords found in passwords file');
  }
  
  return passwords;
}

function updateEnvFile(key, value) {
  let envContent = fs.readFileSync(envPath, 'utf-8');
  
  const regex = new RegExp(`^${key}=.*$`, 'm');
  if (regex.test(envContent)) {
    envContent = envContent.replace(regex, `${key}=${value}`);
  } else {
    envContent += `\n${key}=${value}`;
  }
  
  fs.writeFileSync(envPath, envContent);
  console.log(`Updated .env file with ${key}`);
}

function updateEnvFileWithPasswords(passwords) {
  users.forEach(user => {
    const key = `${user.toUpperCase()}_PASSWORD`;
    if (!process.env[key] && passwords[user]) {
      updateEnvFile(key, passwords[user]);
    }
  });
}

let PERCUSSION_URL = process.env.DEV_PERCUSSION_URL;

if (!PERCUSSION_URL && INSTALL_PATH) {
  console.log('DEV_PERCUSSION_URL not found in .env, calculating from installation...');
  PERCUSSION_URL = getUrlFromInstall(INSTALL_PATH, 'jetty/base/etc/installation.properties', 'jetty.http.port');
  updateEnvFile('DEV_PERCUSSION_URL', PERCUSSION_URL);
} else if (!PERCUSSION_URL) {
  PERCUSSION_URL = 'http://localhost:9992';
}

let DTS_URL = process.env.DEV_PERCUSSION_DTS_URL;

if (!DTS_URL && DTS_INSTALL_PATH) {
  console.log('DEV_PERCUSSION_DTS_URL not found in .env, calculating from DTS installation...');
  try {
    DTS_URL = getUrlFromInstall(DTS_INSTALL_PATH, 'Deployment/Server/conf/perc/perc-catalina.properties', 'http.port');
    updateEnvFile('DEV_PERCUSSION_DTS_URL', DTS_URL);
  } catch (e) {
    console.log('DTS not configured, skipping...');
    DTS_URL = null;
  }
}

let passwords = {};

if (INSTALL_PATH) {
  const missingPasswords = users.some(user => !process.env[`${user.toUpperCase()}_PASSWORD`]);
  if (missingPasswords) {
    console.log('Some passwords missing, reading from installation...');
    passwords = getPasswordsFromInstall(INSTALL_PATH);
    updateEnvFileWithPasswords(passwords);
  }
}

const ADMIN_PASSWORD = process.env.ADMIN_PASSWORD || passwords['Admin'];
const EDITOR_PASSWORD = process.env.EDITOR_PASSWORD || passwords['Editor'];
const CONTRIBUTOR_PASSWORD = process.env.CONTRIBUTOR_PASSWORD || passwords['Contributor'];

async function login(page, username, password) {
  if (!password) {
    throw new Error(`Password for ${username} not set and could not be read from installation`);
  }

  await page.goto(`${PERCUSSION_URL}/Rhythmyx/login`);
  await page.fill('input[name="j_username"]', username);
  await page.fill('input[name="j_password"]', password);
  await page.selectOption('select[name="j_locale"]', 'en-us');

  // The CMS uses a multipart/form-data POST with OWASP-CSRFTOKEN. The
  // server returns 302 → /Rhythmyx/index.jsp (JSP welcome). We submit
  // and poll page.url() until we leave the login path, with a hard
  // timeout. This avoids any race with Playwright's navigation events
  // on multipart responses.
  await Promise.all([
    page
      .waitForFunction(
        () => !window.location.pathname.endsWith('/Rhythmyx/login'),
        null,
        { timeout: 15_000 },
      )
      .catch(async () => {
        // networkidle sometimes fires before the JS function evaluates;
        // fall back to a short poll.
        await page.waitForTimeout(1500);
      }),
    page.click('button[type="submit"]'),
  ]);

  const url = page.url();
  if (url.includes('/Rhythmyx/login')) {
    throw new Error(`Login did not navigate away from /Rhythmyx/login (still at ${url})`);
  }
}

async function loginAsAdmin(page) {
  await login(page, ADMIN_USERNAME, ADMIN_PASSWORD);
}

async function loginAsEditor(page) {
  await login(page, USERNAME_EDITOR, EDITOR_PASSWORD);
}

async function loginAsContributor(page) {
  await login(page, USERNAME_CONTRIBUTOR, CONTRIBUTOR_PASSWORD);
}

module.exports = {
  BASE_URL: PERCUSSION_URL,
  DTS_URL,
  INSTALL_PATH,
  DTS_INSTALL_PATH,
  ADMIN_USERNAME,
  ADMIN_PASSWORD,
  EDITOR_PASSWORD,
  CONTRIBUTOR_PASSWORD,
  loginAsAdmin,
  loginAsEditor,
  loginAsContributor
};
