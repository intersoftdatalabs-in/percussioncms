# JavaScript CodeQL Vulnerabilities Index

## Summary

- **Total Files with Vulnerabilities**: 20
- **Total Findings**: 73
- **All Severity Level**: Warning
- **Primary Categories**: XSS (90%), Regex (10%)

---

## Vulnerability Index

|                                           File Path                                           |  Severity   | Issue Category | Finding Count |
|-----------------------------------------------------------------------------------------------|-------------|----------------|---------------|
| WebUI/war/jslibMin/perc_admin.packed.js                                                       | Warning     | XSS            | 13            |
| WebUI/war/jslibMin/perc_publish.packed.js                                                     | Warning     | XSS, Regex     | 13            |
| WebUI/war/jslibMin/perc_architecture.packed.js                                                | Warning     | XSS            | 10            |
| WebUI/war/jslibMin/perc_dashboard.packed.js                                                   | Warning     | XSS            | 9             |
| WebUI/war/jslibMin/perc_editTemplate.packed.js                                                | Warning     | XSS            | 7             |
| WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-perc-retiredjs/jquery.blockUI.js            | Warning     | XSS            | 5             |
| WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-form/jquery.form.js                         | Warning     | XSS            | 2             |
| WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-layout/jquery.layout_and_plugins.js         | Warning     | XSS            | 2             |
| WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-print-this/printThis.js                     | Warning     | XSS            | 2             |
| WebUI/war/jslibMin/perc_users.packed.js                                                       | Warning     | XSS            | 2             |
| WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-jeditable/jquery.jeditable.js               | Warning     | XSS            | 1             |
| WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-perc-retiredjs/jquery.print-this.js         | Warning     | XSS            | 1             |
| system/cms/content/applications/sys_resources/ApplicationFiles/dojo/dojo.js                   | Warning     | Regex          | 1             |
| system/cms/content/applications/sys_resources/ApplicationFiles/dojo/src/widget/ContentPane.js | Warning     | Regex          | 1             |
| system/cms/content/applications/sys_resources/ApplicationFiles/dojo/src/widget/html/loader.js | Warning     | Regex          | 1             |
| WebUI/war/api/lib/swagger.js                                                                  | Warning     | Regex          | 1             |
| WebUI/war/shared-common-minuet.js                                                             | Warning     | Regex          | 1             |
| WebUI/war/views/PercCommonMinuetView.js                                                       | Warning     | Regex          | 1             |
| **TOTAL**                                                                                     | **Warning** | **—**          | **73**        |

---

## Breakdown by Category

### XSS Vulnerabilities (66 findings)

|                                       File Path                                       | Count |              Rule              |
|---------------------------------------------------------------------------------------|-------|--------------------------------|
| WebUI/war/jslibMin/perc_admin.packed.js                                               | 13    | js/html-constructed-from-input |
| WebUI/war/jslibMin/perc_publish.packed.js                                             | 12    | js/html-constructed-from-input |
| WebUI/war/jslibMin/perc_architecture.packed.js                                        | 10    | js/html-constructed-from-input |
| WebUI/war/jslibMin/perc_dashboard.packed.js                                           | 9     | js/html-constructed-from-input |
| WebUI/war/jslibMin/perc_editTemplate.packed.js                                        | 7     | js/html-constructed-from-input |
| WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-perc-retiredjs/jquery.blockUI.js    | 5     | js/html-constructed-from-input |
| WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-form/jquery.form.js                 | 2     | js/html-constructed-from-input |
| WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-layout/jquery.layout_and_plugins.js | 2     | js/html-constructed-from-input |
| WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-print-this/printThis.js             | 2     | js/html-constructed-from-input |
| WebUI/war/jslibMin/perc_users.packed.js                                               | 2     | js/html-constructed-from-input |
| WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-jeditable/jquery.jeditable.js       | 1     | js/html-constructed-from-input |
| WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-perc-retiredjs/jquery.print-this.js | 1     | js/html-constructed-from-input |

### Regex Vulnerabilities (7 findings)

|                                           File Path                                           | Count |                Rule                |
|-----------------------------------------------------------------------------------------------|-------|------------------------------------|
| system/cms/content/applications/sys_resources/ApplicationFiles/dojo/dojo.js                   | 1     | js/overly-large-range              |
| system/cms/content/applications/sys_resources/ApplicationFiles/dojo/src/widget/ContentPane.js | 1     | js/overly-large-range              |
| system/cms/content/applications/sys_resources/ApplicationFiles/dojo/src/widget/html/loader.js | 1     | js/overly-large-range              |
| WebUI/war/api/lib/swagger.js                                                                  | 1     | js/useless-regexp-character-escape |
| WebUI/war/jslibMin/perc_publish.packed.js                                                     | 1     | js/useless-regexp-character-escape |
| WebUI/war/shared-common-minuet.js                                                             | 1     | js/useless-regexp-character-escape |
| WebUI/war/views/PercCommonMinuetView.js                                                       | 1     | js/useless-regexp-character-escape |

---

## Classification: Custom vs. Third-Party

### Custom Percussion Code (52 findings - Require Fixing)

**Packed Bundles** (51 findings):
- WebUI/war/jslibMin/perc_admin.packed.js (13)
- WebUI/war/jslibMin/perc_publish.packed.js (12)
- WebUI/war/jslibMin/perc_architecture.packed.js (10)
- WebUI/war/jslibMin/perc_dashboard.packed.js (9)
- WebUI/war/jslibMin/perc_editTemplate.packed.js (7)
- WebUI/war/jslibMin/perc_users.packed.js (2)

**Direct Source Files** (2 findings):
- WebUI/war/shared-common-minuet.js (1)
- WebUI/war/views/PercCommonMinuetView.js (1)

### Third-Party Code (21 findings - Can Be Suppressed)

**jQuery Plugins** (14 findings):
- WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-perc-retiredjs/jquery.blockUI.js (5)
- WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-form/jquery.form.js (2)
- WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-layout/jquery.layout_and_plugins.js (2)
- WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-print-this/printThis.js (2)
- WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-jeditable/jquery.jeditable.js (1)
- WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-perc-retiredjs/jquery.print-this.js (1)

**Dojo Framework** (3 findings):
- system/cms/content/applications/sys_resources/ApplicationFiles/dojo/dojo.js (1)
- system/cms/content/applications/sys_resources/ApplicationFiles/dojo/src/widget/ContentPane.js (1)
- system/cms/content/applications/sys_resources/ApplicationFiles/dojo/src/widget/html/loader.js (1)

**Swagger Library** (1 finding):
- WebUI/war/api/lib/swagger.js (1)

**Minified Custom Code** (3 findings):
- WebUI/war/jslibMin/perc_publish.packed.js (1) — includes Regex issue

---

## Next Steps

1. **Review this index** to understand scope and priority
2. **Create suppression rules** for third-party code (21 findings)
3. **Map packed files** to source code for 52 custom findings
4. **Plan remediation** for custom code vulnerabilities

