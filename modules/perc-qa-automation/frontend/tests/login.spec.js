const { chromium } = require('playwright');
const { loginAsAdmin, BASE_URL, ADMIN_USERNAME } = require('./helpers/auth');

(async () => {
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext();
  const page = await context.newPage();

  try {
    console.log('Logging in as Admin...');
    console.log(`Using BASE_URL: ${BASE_URL}`);
    
    await loginAsAdmin(page);

    const url = page.url();
    const title = await page.title();

    console.log('Current URL after login:', url);
    console.log('Page title:', title);

    if (url.includes('error') || title.includes('Error')) {
      console.log('Login failed - redirected to error page');
      process.exit(1);
    } else if (url.includes('login') || url.includes('Login')) {
      console.log('Still on login page - authentication may have failed');
      process.exit(1);
    } else {
      console.log('Login successful!');
      process.exit(0);
    }

  } catch (error) {
    console.error('Error:', error.message);
    process.exit(1);
  } finally {
    await browser.close();
  }
})();
