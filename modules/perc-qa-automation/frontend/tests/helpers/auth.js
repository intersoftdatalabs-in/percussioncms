const fs = require("fs");
const path = require("path");
const {
  resolveCmsBaseUrl,
  resolveRolePassword,
  hasQaModeUrlEnv,
  DEV_FALLBACK_URL,
} = require("./resolve-cms-env");

const envPath = path.resolve(__dirname, "../../../.env");
require("dotenv").config({ path: envPath });

const INSTALL_PATH = process.env.DEV_PERCUSSION_INSTALL;
const DTS_INSTALL_PATH = process.env.DEV_PERCUSSION_DTS_INSTALL;
const ADMIN_USERNAME = process.env.ADMIN_USERNAME || "Admin";
const USERNAME_EDITOR = process.env.EDITOR_USERNAME || "Editor";
const USERNAME_CONTRIBUTOR = process.env.CONTRIBUTOR_USERNAME || "Contributor";

const users = ["Admin", "Editor", "Contributor"];

/**
 * Portable join under an install root. Uses path.join so Windows/Unix separators
 * are correct; prop paths under the install use OS-native segments.
 *
 * @param {string} installPath
 * @param {...string} segments
 * @returns {string}
 */
function installFile(installPath, ...segments) {
  return path.join(installPath, ...segments);
}

function getUrlFromInstall(installPath, propsFile, portKey) {
  const filePath = installFile(installPath, ...propsFile.split("/"));

  if (!fs.existsSync(filePath)) {
    throw new Error(`${propsFile} not found at: ${filePath}`);
  }

  const content = fs.readFileSync(filePath, "utf-8");
  const portMatch = content.match(new RegExp(`${portKey}=(\\d+)`));

  if (!portMatch) {
    throw new Error(`${portKey} not found in ${propsFile}`);
  }

  const port = portMatch[1];
  return `http://localhost:${port}`;
}

function getPasswordsFromInstall(installPath) {
  const passwordFile = installFile(
    installPath,
    "var",
    "config",
    "generated",
    "passwords",
  );

  if (!fs.existsSync(passwordFile)) {
    throw new Error(`Password file not found at: ${passwordFile}`);
  }

  const content = fs.readFileSync(passwordFile, "utf-8");
  const passwords = {};

  users.forEach((user) => {
    const match = content.match(new RegExp(`${user}=([^\\n]+)`));
    if (match) {
      passwords[user] = match[1].trim();
    }
  });

  if (Object.keys(passwords).length === 0) {
    throw new Error("No passwords found in passwords file");
  }

  return passwords;
}

function updateEnvFile(key, value) {
  // First-run setup: copy .env.example -> .env so dotenv has a file to
  // load. Subsequent calls update the .env file in place. We do this
  // lazily so a first-time user (who set DEV_PERCUSSION_INSTALL but
  // forgot to copy .env.example) does not crash with ENOENT.
  // QA mode (TEST_CMS_URL only) never requires this file.
  if (!fs.existsSync(envPath)) {
    const examplePath = path.resolve(__dirname, "../../../.env.example");
    if (fs.existsSync(examplePath)) {
      fs.copyFileSync(examplePath, envPath);
      console.log(`Created ${envPath} from .env.example`);
    } else {
      // No template; create an empty .env with the key we're about to
      // write so subsequent reads can find it.
      fs.writeFileSync(envPath, "");
    }
  }

  let envContent = fs.readFileSync(envPath, "utf-8");

  const regex = new RegExp(`^${key}=.*$`, "m");
  if (regex.test(envContent)) {
    // Use a replacer function to avoid `$&`, `$'`, etc. in `value` being
    // interpreted as regex replacement special sequences (which would
    // corrupt the .env file for passwords containing those characters).
    envContent = envContent.replace(regex, () => `${key}=${value}`);
  } else {
    envContent += `\n${key}=${value}`;
  }

  fs.writeFileSync(envPath, envContent);
  console.log(`Updated .env file with ${key}`);
}

function updateEnvFileWithPasswords(passwords) {
  users.forEach((user) => {
    const key = `${user.toUpperCase()}_PASSWORD`;
    if (!process.env[key] && passwords[user]) {
      updateEnvFile(key, passwords[user]);
    }
  });
}

/**
 * Optional install-based URL discovery for human dev mode only.
 * Skipped when QA env already supplies a base URL (TEST_CMS_URL / host port).
 *
 * @returns {string | null}
 */
function discoverInstallUrl() {
  if (!INSTALL_PATH) {
    return null;
  }
  if (hasQaModeUrlEnv(process.env) || process.env.DEV_PERCUSSION_URL) {
    // Explicit URL already wins; avoid rewriting .env during QA runs.
    return null;
  }
  console.log(
    "DEV_PERCUSSION_URL not found in .env, calculating from installation...",
  );
  const discovered = getUrlFromInstall(
    INSTALL_PATH,
    "jetty/base/etc/installation.properties",
    "jetty.http.port",
  );
  // Explicit first-run side effect for human dev mode: persist discovered URL
  // so subsequent requires do not re-read install properties. Opt out with
  // PERC_QA_SKIP_ENV_WRITE=1 (unit/inspection loads). QA mode never reaches
  // here (hasQaModeUrlEnv / DEV_PERCUSSION_URL short-circuit above).
  if (process.env.PERC_QA_SKIP_ENV_WRITE === "1") {
    console.log(
      "Skipping .env write for discovered DEV_PERCUSSION_URL (PERC_QA_SKIP_ENV_WRITE=1)",
    );
  } else {
    console.log(
      `Persisting discovered DEV_PERCUSSION_URL to .env for next run: ${discovered}`,
    );
    updateEnvFile("DEV_PERCUSSION_URL", discovered);
  }
  return discovered;
}

const installUrl = discoverInstallUrl();
const resolved = resolveCmsBaseUrl(process.env, {
  installUrl,
  fallbackUrl: DEV_FALLBACK_URL,
});
const PERCUSSION_URL = resolved.url;

if (resolved.source === "TEST_CMS_URL" || resolved.source === "CMS_HOST_PORT") {
  console.log(`CMS base URL from ${resolved.source}: ${PERCUSSION_URL}`);
} else if (resolved.source === "DEV_PERCUSSION_URL") {
  console.log(`CMS base URL from DEV_PERCUSSION_URL: ${PERCUSSION_URL}`);
} else if (resolved.source === "install") {
  console.log(
    `CMS base URL from install discovery (DEV_PERCUSSION_INSTALL): ${PERCUSSION_URL}`,
  );
} else if (resolved.source === "fallback") {
  console.log(
    `CMS base URL fallback (set TEST_CMS_URL for QA mode or DEV_PERCUSSION_URL / DEV_PERCUSSION_INSTALL for dev): ${PERCUSSION_URL}`,
  );
}

let DTS_URL = process.env.DEV_PERCUSSION_DTS_URL;

if (!DTS_URL && DTS_INSTALL_PATH) {
  console.log(
    "DEV_PERCUSSION_DTS_URL not found in .env, calculating from DTS installation...",
  );
  try {
    DTS_URL = getUrlFromInstall(
      DTS_INSTALL_PATH,
      "Deployment/Server/conf/perc/perc-catalina.properties",
      "http.port",
    );
    updateEnvFile("DEV_PERCUSSION_DTS_URL", DTS_URL);
  } catch (e) {
    console.log("DTS not configured, skipping...");
    DTS_URL = null;
  }
}

let passwords = {};

// Host-install password discovery is opt-in for human dev mode only.
// QA mode supplies ADMIN_PASSWORD (etc.) via env from qa-up / docker exec.
if (INSTALL_PATH) {
  const missingPasswords = users.some(
    (user) => !process.env[`${user.toUpperCase()}_PASSWORD`],
  );
  if (missingPasswords) {
    try {
      console.log("Some passwords missing, reading from installation...");
      passwords = getPasswordsFromInstall(INSTALL_PATH);
      updateEnvFileWithPasswords(passwords);
      // Refresh process.env so resolveRolePassword sees newly written keys
      // when dotenv already ran; updateEnvFile writes disk only.
      users.forEach((user) => {
        const key = `${user.toUpperCase()}_PASSWORD`;
        if (!process.env[key] && passwords[user]) {
          process.env[key] = passwords[user];
        }
      });
    } catch (e) {
      // QA mode may point INSTALL_PATH at a non-local path by accident; do not
      // hard-fail if passwords are already in env for some roles.
      console.log(`Install password discovery skipped: ${e.message || e}`);
    }
  }
}

const adminResolved = resolveRolePassword("Admin", process.env, passwords);
const editorResolved = resolveRolePassword("Editor", process.env, passwords);
const contributorResolved = resolveRolePassword(
  "Contributor",
  process.env,
  passwords,
);

const ADMIN_PASSWORD = adminResolved.password;
const EDITOR_PASSWORD = editorResolved.password;
const CONTRIBUTOR_PASSWORD = contributorResolved.password;

function missingPasswordMessage(username) {
  return (
    `Password for ${username} not set. For QA mode set ${username.toUpperCase()}_PASSWORD ` +
    `(Admin default username ADMIN_USERNAME=Admin; password from perc-devctl qa-up output, ` +
    `env, or docker exec — never commit secrets). For dev mode set DEV_PERCUSSION_INSTALL ` +
    `so passwords can be read from var/config/generated/passwords, or set the env key directly.`
  );
}

/**
 * Options for login form fill / login helpers.
 * @typedef {object} LoginOptions
 * @property {string} [locale] login j_locale tag (e.g. {@code de-de}, {@code hi-in}).
 *   When omitted, existing tests keep English ({@code en-us}).
 */

/**
 * Select a locale on the modern LocaleSelect combobox (hidden j_locale).
 *
 * @param {import("@playwright/test").Page} page
 * @param {string} locale
 */
async function selectModernLoginLocale(page, locale) {
  const target = String(locale || "").trim();
  if (!target) {
    return;
  }
  const hiddenLocale = page.locator(
    'input[type="hidden"][name="j_locale"], input[name="j_locale"]',
  );
  // Avoid Playwright auto-wait on empty locator (would burn the default timeout).
  if ((await hiddenLocale.count()) > 0) {
    const current = await hiddenLocale.first().inputValue().catch(() => "");
    if (current === target) {
      return;
    }
  }

  await page.locator('[data-testid="perc-login-locale"]').click();
  const byTestId = page.locator(
    `[data-testid="perc-login-locale-option-${target}"]`,
  );
  if ((await byTestId.count()) > 0) {
    await byTestId.first().click();
  } else {
    // Fallback: option label often looks like "de-de - Deutsch"
    const escaped = target.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
    await page
      .locator('[role="option"]')
      .filter({ hasText: new RegExp(escaped, "i") })
      .first()
      .click();
  }

  if ((await hiddenLocale.count()) > 0) {
    await page
      .waitForFunction(
        (loc) => {
          const el = document.querySelector(
            'input[name="j_locale"], input[type="hidden"][name="j_locale"]',
          );
          return el && el.value === loc;
        },
        target,
        { timeout: 10_000 },
      )
      .catch(() => {
        /* best-effort; form still POSTs whatever is in the hidden field */
      });
  }
}

/**
 * Read available modern LocaleSelect option tags (opens the listbox briefly).
 *
 * @param {import("@playwright/test").Page} page
 * @returns {Promise<string[]>}
 */
async function listModernLoginLocales(page) {
  const trigger = page.locator('[data-testid="perc-login-locale"]');
  if ((await trigger.count()) === 0) {
    return [];
  }
  await trigger.click();
  const tags = await page
    .locator('[data-testid^="perc-login-locale-option-"]')
    .evaluateAll((els) =>
      els
        .map((el) => {
          const id = el.getAttribute("data-testid") || "";
          return id.replace(/^perc-login-locale-option-/, "");
        })
        .filter(Boolean),
    );
  // Close listbox (Escape or re-click) so it does not obscure submit.
  await page.keyboard.press("Escape").catch(() => {});
  return tags;
}

/**
 * Fill Admin/role credentials on either the modern React login (data-testid)
 * or the legacy JSP form (name= attributes). Locale defaults to en-us when
 * the modern LocaleSelect already posts a hidden j_locale.
 *
 * @param {import("@playwright/test").Page} page
 * @param {string} username
 * @param {string} password
 * @param {LoginOptions} [options]
 */
async function fillLoginForm(page, username, password, options = {}) {
  const desiredLocale =
    options && options.locale ? String(options.locale).trim() : "";
  const modernRoot = page.locator('[data-testid="perc-login-root"]');
  const modernForm = page.locator('[data-testid="perc-login-form"]');
  const legacyUser = page.locator('input[name="j_username"]');

  // Prefer modern React login (#2065 H2 qa-up ships rxlogin → perc-login-root).
  if ((await modernRoot.count()) > 0 || (await modernForm.count()) > 0) {
    await modernForm.or(modernRoot).first().waitFor({ state: "visible", timeout: 30_000 });
    await page.locator('[data-testid="perc-login-username"]').fill(username);
    await page.locator('[data-testid="perc-login-password"]').fill(password);
    // LocaleSelect posts hidden input[name=j_locale]; default bootstrap is en-us.
    if (desiredLocale) {
      await selectModernLoginLocale(page, desiredLocale);
    } else {
      // Only open the combobox when we must force English for existing specs.
      const hiddenLocale = page.locator(
        'input[type="hidden"][name="j_locale"], input[name="j_locale"]',
      );
      if ((await hiddenLocale.count()) > 0) {
        const current = await hiddenLocale.first().inputValue().catch(() => "");
        if (current && current !== "en-us") {
          await selectModernLoginLocale(page, "en-us");
        }
      }
    }
    return { submit: page.locator('[data-testid="perc-login-submit"]') };
  }

  // Legacy native form (select[name=j_locale]).
  await legacyUser.waitFor({ state: "visible", timeout: 30_000 });
  await page.fill('input[name="j_username"]', username);
  await page.fill('input[name="j_password"]', password);
  const nativeLocale = page.locator('select[name="j_locale"]');
  if ((await nativeLocale.count()) > 0) {
    await nativeLocale.selectOption(desiredLocale || "en-us");
  }
  return { submit: page.locator('button[type="submit"]') };
}

/**
 * @param {import("@playwright/test").Page} page
 * @param {string} username
 * @param {string} password
 * @param {LoginOptions} [options]
 */
async function login(page, username, password, options = {}) {
  if (!password) {
    throw new Error(missingPasswordMessage(username));
  }

  await page.goto(`${PERCUSSION_URL}/Rhythmyx/login`);
  // Wait for SPA mount or legacy form before filling.
  await page.waitForLoadState("domcontentloaded");
  const { submit } = await fillLoginForm(page, username, password, options);

  // The CMS uses a multipart/form-data POST with OWASP-CSRFTOKEN. Modern
  // login may land at /cm/app/spa.jsp?entry=home; legacy lands at index.jsp.
  // Poll until we leave the login path, with a hard timeout.
  await Promise.all([
    page
      .waitForFunction(
        () => {
          const p = window.location.pathname;
          return !p.endsWith("/Rhythmyx/login") && !p.endsWith("/login");
        },
        null,
        { timeout: 30_000 },
      )
      .catch(async () => {
        // networkidle sometimes fires before the JS function evaluates;
        // fall back to a short poll.
        await page.waitForTimeout(1500);
      }),
    submit.click(),
  ]);

  const url = page.url();
  if (url.includes("/Rhythmyx/login") || /\/login(\?|$)/.test(url)) {
    throw new Error(
      `Login did not navigate away from login page (still at ${url})`,
    );
  }
}

/**
 * @param {import("@playwright/test").Page} page
 * @param {LoginOptions} [options]
 */
async function loginAsAdmin(page, options = {}) {
  await login(page, ADMIN_USERNAME, ADMIN_PASSWORD, options);
}

/**
 * @param {import("@playwright/test").Page} page
 * @param {LoginOptions} [options]
 */
async function loginAsEditor(page, options = {}) {
  await login(page, USERNAME_EDITOR, EDITOR_PASSWORD, options);
}

/**
 * @param {import("@playwright/test").Page} page
 * @param {LoginOptions} [options]
 */
async function loginAsContributor(page, options = {}) {
  await login(page, USERNAME_CONTRIBUTOR, CONTRIBUTOR_PASSWORD, options);
}

/**
 * Headers for CMS REST calls that opt into Basic auth.
 *
 * <p>{@code RX_USEBASICAUTH: true} alone is <strong>not</strong> enough — the
 * server still expects an {@code Authorization: Basic …} header. Specs that
 * only set the RX flag get HTTP 401 against a stock 8.2 install.</p>
 *
 * @param {string} username
 * @param {string} password
 * @returns {Record<string, string>}
 */
function basicAuthHeaders(username, password) {
  if (!password) {
    throw new Error(`basicAuthHeaders: ${missingPasswordMessage(username)}`);
  }
  const token = Buffer.from(`${username}:${password}`, "utf8").toString(
    "base64",
  );
  return {
    RX_USEBASICAUTH: "true",
    Authorization: `Basic ${token}`,
  };
}

function adminBasicAuthHeaders() {
  return basicAuthHeaders(ADMIN_USERNAME, ADMIN_PASSWORD);
}

module.exports = {
  BASE_URL: PERCUSSION_URL,
  BASE_URL_SOURCE: resolved.source,
  DTS_URL,
  INSTALL_PATH,
  DTS_INSTALL_PATH,
  ADMIN_USERNAME,
  ADMIN_PASSWORD,
  EDITOR_PASSWORD,
  CONTRIBUTOR_PASSWORD,
  loginAsAdmin,
  loginAsEditor,
  loginAsContributor,
  fillLoginForm,
  selectModernLoginLocale,
  listModernLoginLocales,
  basicAuthHeaders,
  adminBasicAuthHeaders,
  // Re-export pure helpers for specs / tests that want the same precedence.
  resolveCmsBaseUrl,
  resolveRolePassword,
  hasQaModeUrlEnv,
};
