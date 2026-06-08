# Bug Fix #809 – Social Buttons: Replace legacy Twitter icon and links with X branding

## Problem

The **Social Buttons** widget:
- Emitted the legacy Twitter bird icon using FontAwesome 5's `.fa-twitter` brand icon.
- Renders the legacy sky blue brand color (`#00a9f1`).
- Contains user-facing references to Twitter (e.g. placeholder URLs like `http://www.twitter.com/percussion`, `aria-label="Twitter"`).
- Still points the share dialog to `https://twitter.com/intent/tweet`.

Affected: 8.1.x (and prior versions)

## Solution / Fix

1. **Rebranded Icons to X**:
   Since the embedded FontAwesome (v5.6.1) does not support the modern `fa-x-twitter` brand icon, we implemented a custom CSS mask override using standard X SVG vectors for both editor preview and frontend output:

   ```css
   .perc-twitter-social-button i::before,
   .perc-social-button-ui .fa-twitter::before,
   .perc-social-button .fa-twitter::before {
     content: "" !important;
     display: inline-block;
     width: 1em;
     height: 1em;
     background-color: currentColor;
     mask: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 512 512'><path d='M389.2 48h70.6L305.6 224.2 487 464H345L233.7 318.6 106.5 464H35.8L200.7 275.5 26.8 48H172.4L272.9 180.9 389.2 48zM364.4 421.8h39.1L151.1 88h-42L364.4 421.8z'/></svg>") no-repeat center;
     -webkit-mask: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 512 512'><path d='M389.2 48h70.6L305.6 224.2 487 464H345L233.7 318.6 106.5 464H35.8L200.7 275.5 26.8 48H172.4L272.9 180.9 389.2 48zM364.4 421.8h39.1L151.1 88h-42L364.4 421.8z'/></svg>") no-repeat center;
   }
   ```
2. **Updated Brand Color**:
   Changed the standard color of the Twitter/X button to black (`#111111`) inside:
   - `percSocialButtons.css` (both control and widget files)
   - `percSocialButtons.xml` (the standard brand color mapping for JEXL/Velocity template rendering)
3. **Rebranded UI Controls and Shared Dialog Links**:
   - Updated placeholder to `https://x.com/percussion` and label to `X (Twitter)` in [percSocialButtonsControl.xsl](file:///home/nate/projects/java8/percussioncms/system/Packages/perc.widget.socialButtons/SupportFile-rx_resources/stylesheets/controls/percSocialButtonsControl.xsl).
   - Pointed the frontend share URL to `https://x.com/intent/tweet` in [percSocialButtons.xml](file:///home/nate/projects/java8/percussioncms/system/Packages/perc.widget.socialButtons/sys__UserDependency--rxconfig/Widgets/percSocialButtons.xml).
4. **Bumped Widget Package Version**:
   - Bumped the package version from `1.2.3` to `1.2.4` in [psx_archiveInfo.xml](file:///home/nate/projects/java8/percussioncms/system/Packages/perc.widget.socialButtons/psx_archiveInfo.xml).

---

## Files Updated

- [percSocialButtonsControl.xsl](file:///home/nate/projects/java8/percussioncms/system/Packages/perc.widget.socialButtons/SupportFile-rx_resources/stylesheets/controls/percSocialButtonsControl.xsl)
- [percSocialButtons.css](file:///home/nate/projects/java8/percussioncms/system/Packages/perc.widget.socialButtons/sys__UserDependency--rx_resources/widgets/percSocialButtons/css/percSocialButtons.css) (rx_resources)
- [percSocialButtons.css](file:///home/nate/projects/java8/percussioncms/system/Packages/perc.widget.socialButtons/SupportFile-rx_resources/widgets/percSocialButtons/css/percSocialButtons.css) (SupportFile)
- [percSocialButtons.xml](file:///home/nate/projects/java8/percussioncms/system/Packages/perc.widget.socialButtons/sys__UserDependency--rxconfig/Widgets/percSocialButtons.xml)
- [psx_archiveInfo.xml](file:///home/nate/projects/java8/percussioncms/system/Packages/perc.widget.socialButtons/psx_archiveInfo.xml)

