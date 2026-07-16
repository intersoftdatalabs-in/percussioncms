# Code Scanning Alerts for intersoftdatalabs-in/percussioncms

State filter: open
Generated: 2026-07-11T16:05:54Z (UTC)

- **Alert #1708** — `js/prototype-pollution-utility` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-07-10T23:05:34Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1708
  - **Location:** WebUI/src/main/webapp/cm/pages/cui/components/knockoutjs/dist/knockout.debug.js:42
  - **Message:** The property chain here is recursively assigned to target without guarding against prototype pollution.

- **Alert #1707** — `js/prototype-pollution-utility` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-07-10T23:05:34Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1707
  - **Location:** WebUI/src/main/webapp/cm/cui/components/knockoutjs/dist/knockout.debug.js:42
  - **Message:** The property chain here is recursively assigned to target without guarding against prototype pollution.

- **Alert #1706** — `js/useless-regexp-character-escape` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-07-10T23:05:34Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1706
  - **Location:** WebUI/src/main/webapp/cm/pages/cui/components/knockoutjs/dist/knockout.debug.js:2001
  - **Message:** The escape sequence '\\w' is equivalent to just 'w', so the sequence is not a character class when it is used in a regular expression.\nThe escape sequence '\\w' is equivalent to just 'w', so the sequence is not a character class when it is used in a regular expression.\nThe escape sequence '\\w' is equivalent to just 'w', so the sequence is not a character class when it is used in a regular expression.

- **Alert #1705** — `js/useless-regexp-character-escape` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-07-10T23:05:34Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1705
  - **Location:** WebUI/src/main/webapp/cm/cui/components/knockoutjs/dist/knockout.debug.js:2001
  - **Message:** The escape sequence '\\w' is equivalent to just 'w', so the sequence is not a character class when it is used in a regular expression.\nThe escape sequence '\\w' is equivalent to just 'w', so the sequence is not a character class when it is used in a regular expression.\nThe escape sequence '\\w' is equivalent to just 'w', so the sequence is not a character class when it is used in a regular expression.

- **Alert #1704** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-07-10T23:05:34Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1704
  - **Location:** WebUI/src/main/webapp/cm/pages/cui/components/twitter-bootstrap-3.0.0/dist/js/bootstrap.js:524
  - **Message:** Potential XSS vulnerability in the '$.fn.collapse' plugin.

- **Alert #1703** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-07-10T23:05:34Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1703
  - **Location:** WebUI/src/main/webapp/cm/cui/components/twitter-bootstrap-3.0.0/dist/js/bootstrap.js:524
  - **Message:** Potential XSS vulnerability in the '$.fn.collapse' plugin.

- **Alert #1702** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-07-10T23:05:34Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1702
  - **Location:** WebUI/src/main/webapp/cm/pages/cui/components/twitter-bootstrap-3.0.0/dist/js/bootstrap.js:1790
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1701** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-07-10T23:05:34Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1701
  - **Location:** WebUI/src/main/webapp/cm/pages/cui/components/twitter-bootstrap-3.0.0/dist/js/bootstrap.js:1329
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1700** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-07-10T23:05:34Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1700
  - **Location:** WebUI/src/main/webapp/cm/pages/cui/components/twitter-bootstrap-3.0.0/dist/js/bootstrap.js:1058
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1699** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-07-10T23:05:34Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1699
  - **Location:** WebUI/src/main/webapp/cm/pages/cui/components/twitter-bootstrap-3.0.0/dist/js/bootstrap.js:785
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1698** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-07-10T23:05:34Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1698
  - **Location:** WebUI/src/main/webapp/cm/pages/cui/components/twitter-bootstrap-3.0.0/dist/js/bootstrap.js:662
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1697** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-07-10T23:05:34Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1697
  - **Location:** WebUI/src/main/webapp/cm/pages/cui/components/twitter-bootstrap-3.0.0/dist/js/bootstrap.js:658
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1696** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-07-10T23:05:34Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1696
  - **Location:** WebUI/src/main/webapp/cm/pages/cui/components/twitter-bootstrap-3.0.0/dist/js/bootstrap.js:471
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1695** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-07-10T23:05:34Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1695
  - **Location:** WebUI/src/main/webapp/cm/pages/cui/components/twitter-bootstrap-3.0.0/dist/js/bootstrap.js:106
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1694** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-07-10T23:05:34Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1694
  - **Location:** WebUI/src/main/webapp/cm/pages/cui/components/knockoutjs/build/knockout-raw.js:23
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1693** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-07-10T23:05:34Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1693
  - **Location:** WebUI/src/main/webapp/cm/cui/components/twitter-bootstrap-3.0.0/dist/js/bootstrap.js:1790
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1692** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-07-10T23:05:34Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1692
  - **Location:** WebUI/src/main/webapp/cm/cui/components/twitter-bootstrap-3.0.0/dist/js/bootstrap.js:1329
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1691** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-07-10T23:05:34Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1691
  - **Location:** WebUI/src/main/webapp/cm/cui/components/twitter-bootstrap-3.0.0/dist/js/bootstrap.js:1058
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1690** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-07-10T23:05:34Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1690
  - **Location:** WebUI/src/main/webapp/cm/cui/components/twitter-bootstrap-3.0.0/dist/js/bootstrap.js:785
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1689** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-07-10T23:05:34Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1689
  - **Location:** WebUI/src/main/webapp/cm/cui/components/twitter-bootstrap-3.0.0/dist/js/bootstrap.js:662
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1688** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-07-10T23:05:34Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1688
  - **Location:** WebUI/src/main/webapp/cm/cui/components/twitter-bootstrap-3.0.0/dist/js/bootstrap.js:658
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1687** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-07-10T23:05:34Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1687
  - **Location:** WebUI/src/main/webapp/cm/cui/components/twitter-bootstrap-3.0.0/dist/js/bootstrap.js:471
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1686** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-07-10T23:05:34Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1686
  - **Location:** WebUI/src/main/webapp/cm/cui/components/twitter-bootstrap-3.0.0/dist/js/bootstrap.js:106
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1685** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-07-10T23:05:34Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1685
  - **Location:** WebUI/src/main/webapp/cm/cui/components/knockoutjs/build/knockout-raw.js:23
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1684** — `js/bad-tag-filter` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-07-10T23:05:34Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1684
  - **Location:** WebUI/src/main/webapp/cm/pages/cui/components/knockoutjs/dist/knockout.debug.js:771
  - **Message:** This regular expression does not match comments containing newlines.

- **Alert #1683** — `js/bad-tag-filter` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-07-10T23:05:34Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1683
  - **Location:** WebUI/src/main/webapp/cm/cui/components/knockoutjs/dist/knockout.debug.js:771
  - **Message:** This regular expression does not match comments containing newlines.

- **Alert #1682** — `java/ssrf` (critical, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T21:44:23Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1682
  - **Location:** modules/extensions-main/src/main/java/com/percussion/extensions/general/PSProxyQueryResource.java:185
  - **Message:** Potential server-side request forgery due to a user-provided value.\nPotential server-side request forgery due to a user-provided value.\nPotential server-side request forgery due to a user-provided value.\nPotential server-side request forgery due to a user-provided value.\nPotential server-side request forgery due to a user-provided value.\nPotential server-side request forgery due to a user-provided value.\nPotential server-side request forgery due to a user-provided value.\nPotential server-side request forgery due to a user-provided value.\nPotential server-side request forgery due to a user-provided value.\nPotential server-side request forgery due to a user-provided value.\nPotential server-side request forgery due to a user-provided value.\nPotential server-side request forgery due to a user-provided value.\nPotential server-side request forgery due to a user-provided value.\nPotential server-side request forgery due to a user-provided value.\nPotential server-side request forgery due to a user-provided value.\nPotential server-side request forgery due to a user-provided value.\nPotential server-side request forgery due to a user-provided value.

- **Alert #1681** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1681
  - **Location:** WebUI/src/main/webapp/cm/widgets/PercSimpleMenu.js:93
  - **Message:** Potential XSS vulnerability in the '$.fn.percSimpleMenu' plugin.

- **Alert #1680** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1680
  - **Location:** WebUI/src/main/webapp/cm/shared-common-minuet.js:93622
  - **Message:** Potential XSS vulnerability in the '$.fn.perc_toggle' plugin.

- **Alert #1679** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1679
  - **Location:** WebUI/src/main/webapp/cm/shared-common-minuet.js:93621
  - **Message:** Potential XSS vulnerability in the '$.fn.perc_toggle' plugin.

- **Alert #1678** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1678
  - **Location:** WebUI/src/main/webapp/cm/shared-common-minuet.js:93619
  - **Message:** Potential XSS vulnerability in the '$.fn.perc_toggle' plugin.

- **Alert #1677** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1677
  - **Location:** WebUI/src/main/webapp/cm/shared-common-minuet.js:93618
  - **Message:** Potential XSS vulnerability in the '$.fn.perc_toggle' plugin.

- **Alert #1676** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1676
  - **Location:** WebUI/src/main/webapp/cm/shared-common-minuet.js:93617
  - **Message:** Potential XSS vulnerability in the '$.fn.perc_toggle' plugin.

- **Alert #1675** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1675
  - **Location:** WebUI/src/main/webapp/cm/shared-common-minuet.js:93617
  - **Message:** Potential XSS vulnerability in the '$.fn.perc_toggle' plugin.

- **Alert #1674** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1674
  - **Location:** WebUI/src/main/webapp/cm/shared-common.js:90362
  - **Message:** Potential XSS vulnerability in the '$.fn.perc_toggle' plugin.

- **Alert #1673** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1673
  - **Location:** WebUI/src/main/webapp/cm/shared-common.js:90361
  - **Message:** Potential XSS vulnerability in the '$.fn.perc_toggle' plugin.

- **Alert #1672** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1672
  - **Location:** WebUI/src/main/webapp/cm/shared-common.js:90359
  - **Message:** Potential XSS vulnerability in the '$.fn.perc_toggle' plugin.

- **Alert #1671** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1671
  - **Location:** WebUI/src/main/webapp/cm/shared-common.js:90358
  - **Message:** Potential XSS vulnerability in the '$.fn.perc_toggle' plugin.

- **Alert #1670** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1670
  - **Location:** WebUI/src/main/webapp/cm/shared-common.js:90357
  - **Message:** Potential XSS vulnerability in the '$.fn.perc_toggle' plugin.

- **Alert #1669** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1669
  - **Location:** WebUI/src/main/webapp/cm/shared-common.js:90357
  - **Message:** Potential XSS vulnerability in the '$.fn.perc_toggle' plugin.

- **Alert #1668** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1668
  - **Location:** WebUI/src/main/webapp/cm/vendor/js/legacy/profiles/3x/jquery/plugins/jquery-perc-retiredjs/tools.scrollable-1.1.2.js:82
  - **Message:** Potential XSS vulnerability in the '$.fn.scrollable' plugin.

- **Alert #1667** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1667
  - **Location:** WebUI/src/main/webapp/cm/vendor/js/legacy/profiles/3x/jquery/plugins/jquery-perc-retiredjs/tools.scrollable-1.1.2.js:77
  - **Message:** Potential XSS vulnerability in the '$.fn.scrollable' plugin.

- **Alert #1666** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1666
  - **Location:** WebUI/src/main/webapp/cm/vendor/js/legacy/profiles/3x/jquery/plugins/jquery-perc-retiredjs/jquery.blockUI.js:488
  - **Message:** Potential XSS vulnerability in the '$.fn.block' plugin.

- **Alert #1665** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1665
  - **Location:** WebUI/src/main/webapp/cm/shared-common-minuet.js:70131
  - **Message:** Potential XSS vulnerability in the '$.fn.editable' plugin.

- **Alert #1664** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1664
  - **Location:** WebUI/src/main/webapp/cm/shared-common-minuet.js:70112
  - **Message:** Potential XSS vulnerability in the '$.fn.editable' plugin.

- **Alert #1663** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1663
  - **Location:** WebUI/src/main/webapp/cm/vendor/js/legacy/profiles/3x/jquery/plugins/jquery-jeditable/jquery.jeditable.js:627
  - **Message:** Potential XSS vulnerability in the '$.fn.editable' plugin.

- **Alert #1662** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1662
  - **Location:** WebUI/src/main/webapp/cm/vendor/js/legacy/profiles/3x/jquery/plugins/jquery-jeditable/jquery.jeditable.js:608
  - **Message:** Potential XSS vulnerability in the '$.fn.editable' plugin.

- **Alert #1661** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1661
  - **Location:** WebUI/src/main/webapp/cm/shared-common.js:70171
  - **Message:** Potential XSS vulnerability in the '$.fn.editable' plugin.

- **Alert #1660** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1660
  - **Location:** WebUI/src/main/webapp/cm/shared-common.js:70152
  - **Message:** Potential XSS vulnerability in the '$.fn.editable' plugin.

- **Alert #1659** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1659
  - **Location:** WebUI/src/main/webapp/cm/vendor/js/legacy/profiles/3x/jquery/plugins/jquery-form/jquery.form.js:510
  - **Message:** Potential XSS vulnerability in the '$.fn.ajaxSubmit' plugin.

- **Alert #1658** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1658
  - **Location:** WebUI/src/main/webapp/cm/shared-common-minuet.js:59113
  - **Message:** Potential XSS vulnerability in the '$.fn.scrollable' plugin.

- **Alert #1657** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1657
  - **Location:** WebUI/src/main/webapp/cm/shared-common-minuet.js:59108
  - **Message:** Potential XSS vulnerability in the '$.fn.scrollable' plugin.

- **Alert #1656** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1656
  - **Location:** WebUI/src/main/webapp/cm/vendor/js/legacy/profiles/3x/jquery/plugins/jquery-fancytree/modules/jquery.fancytree.ui-deps.js:962
  - **Message:** Potential XSS vulnerability in the '$.fn.position' plugin.

- **Alert #1655** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1655
  - **Location:** WebUI/src/main/webapp/cm/shared-common.js:59153
  - **Message:** Potential XSS vulnerability in the '$.fn.scrollable' plugin.

- **Alert #1654** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1654
  - **Location:** WebUI/src/main/webapp/cm/shared-common.js:59148
  - **Message:** Potential XSS vulnerability in the '$.fn.scrollable' plugin.

- **Alert #1653** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1653
  - **Location:** WebUI/src/main/webapp/cm/shared-common-minuet.js:43417
  - **Message:** Potential XSS vulnerability in the '$.fn.block' plugin.

- **Alert #1652** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1652
  - **Location:** WebUI/src/main/webapp/cm/shared-common-minuet.js:41786
  - **Message:** Potential XSS vulnerability in the '$.fn.ajaxSubmit' plugin.

- **Alert #1651** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1651
  - **Location:** WebUI/src/main/webapp/cm/shared-common.js:43457
  - **Message:** Potential XSS vulnerability in the '$.fn.block' plugin.

- **Alert #1650** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1650
  - **Location:** WebUI/src/main/webapp/cm/shared-common.js:41826
  - **Message:** Potential XSS vulnerability in the '$.fn.ajaxSubmit' plugin.

- **Alert #1649** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1649
  - **Location:** WebUI/src/main/webapp/cm/vendor/js/legacy/profiles/3x/jquery/plugins/jquery-fancytree/jquery.fancytree-all-deps.js:954
  - **Message:** Potential XSS vulnerability in the '$.fn.position' plugin.

- **Alert #1648** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1648
  - **Location:** WebUI/src/main/webapp/cm/shared-common.js:21364
  - **Message:** Potential XSS vulnerability in the '$.fn.position' plugin.

- **Alert #1647** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1647
  - **Location:** WebUI/src/main/webapp/cm/shared-common-minuet.js:14017
  - **Message:** Potential XSS vulnerability in the '$.fn.position' plugin.

- **Alert #1646** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1646
  - **Location:** WebUI/src/main/webapp/cm/vendor/js/legacy/profiles/3x/jquery/libraries/jquery-ui/jquery-ui.js:958
  - **Message:** Potential XSS vulnerability in the '$.fn.position' plugin.

- **Alert #1645** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1645
  - **Location:** WebUI/src/main/webapp/cm/plugins/perc_utils.js:1184
  - **Message:** Potential XSS vulnerability in the '$.fn.perc_toggle' plugin.

- **Alert #1644** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1644
  - **Location:** WebUI/src/main/webapp/cm/plugins/perc_utils.js:1183
  - **Message:** Potential XSS vulnerability in the '$.fn.perc_toggle' plugin.

- **Alert #1643** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1643
  - **Location:** WebUI/src/main/webapp/cm/plugins/perc_utils.js:1181
  - **Message:** Potential XSS vulnerability in the '$.fn.perc_toggle' plugin.

- **Alert #1642** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1642
  - **Location:** WebUI/src/main/webapp/cm/plugins/perc_utils.js:1180
  - **Message:** Potential XSS vulnerability in the '$.fn.perc_toggle' plugin.

- **Alert #1641** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1641
  - **Location:** WebUI/src/main/webapp/cm/plugins/perc_utils.js:1179
  - **Message:** Potential XSS vulnerability in the '$.fn.perc_toggle' plugin.

- **Alert #1640** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1640
  - **Location:** WebUI/src/main/webapp/cm/plugins/perc_utils.js:1179
  - **Message:** Potential XSS vulnerability in the '$.fn.perc_toggle' plugin.

- **Alert #1639** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1639
  - **Location:** WebUI/src/main/webapp/cm/jslib/profiles/3x/jquery/plugins/jquery-perc-retiredjs/tools.scrollable-1.1.2.js:82
  - **Message:** Potential XSS vulnerability in the '$.fn.scrollable' plugin.

- **Alert #1638** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1638
  - **Location:** WebUI/src/main/webapp/cm/jslib/profiles/3x/jquery/plugins/jquery-perc-retiredjs/tools.scrollable-1.1.2.js:77
  - **Message:** Potential XSS vulnerability in the '$.fn.scrollable' plugin.

- **Alert #1637** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1637
  - **Location:** WebUI/src/main/webapp/cm/jslib/profiles/3x/jquery/plugins/jquery-perc-retiredjs/jquery.blockUI.js:488
  - **Message:** Potential XSS vulnerability in the '$.fn.block' plugin.

- **Alert #1636** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1636
  - **Location:** WebUI/src/main/webapp/cm/jslib/profiles/3x/jquery/plugins/jquery-jeditable/jquery.jeditable.js:627
  - **Message:** Potential XSS vulnerability in the '$.fn.editable' plugin.

- **Alert #1635** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1635
  - **Location:** WebUI/src/main/webapp/cm/jslib/profiles/3x/jquery/plugins/jquery-jeditable/jquery.jeditable.js:608
  - **Message:** Potential XSS vulnerability in the '$.fn.editable' plugin.

- **Alert #1634** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1634
  - **Location:** WebUI/src/main/webapp/cm/jslib/profiles/3x/jquery/plugins/jquery-form/jquery.form.js:510
  - **Message:** Potential XSS vulnerability in the '$.fn.ajaxSubmit' plugin.

- **Alert #1633** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1633
  - **Location:** WebUI/src/main/webapp/cm/jslib/profiles/3x/jquery/plugins/jquery-fancytree/modules/jquery.fancytree.ui-deps.js:962
  - **Message:** Potential XSS vulnerability in the '$.fn.position' plugin.

- **Alert #1632** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1632
  - **Location:** WebUI/src/main/webapp/cm/jslib/profiles/3x/jquery/plugins/jquery-fancytree/jquery.fancytree-all-deps.js:954
  - **Message:** Potential XSS vulnerability in the '$.fn.position' plugin.

- **Alert #1631** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1631
  - **Location:** WebUI/src/main/webapp/cm/jslib/profiles/3x/jquery/libraries/jquery-ui/jquery-ui.js:958
  - **Message:** Potential XSS vulnerability in the '$.fn.position' plugin.

- **Alert #1630** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1630
  - **Location:** WebUI/src/main/webapp/cm/app/js/legacy/plugins/perc_utils.js:1184
  - **Message:** Potential XSS vulnerability in the '$.fn.perc_toggle' plugin.

- **Alert #1629** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1629
  - **Location:** WebUI/src/main/webapp/cm/app/js/legacy/plugins/perc_utils.js:1183
  - **Message:** Potential XSS vulnerability in the '$.fn.perc_toggle' plugin.

- **Alert #1628** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1628
  - **Location:** WebUI/src/main/webapp/cm/app/js/legacy/plugins/perc_utils.js:1181
  - **Message:** Potential XSS vulnerability in the '$.fn.perc_toggle' plugin.

- **Alert #1627** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1627
  - **Location:** WebUI/src/main/webapp/cm/app/js/legacy/plugins/perc_utils.js:1180
  - **Message:** Potential XSS vulnerability in the '$.fn.perc_toggle' plugin.

- **Alert #1626** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1626
  - **Location:** WebUI/src/main/webapp/cm/app/js/legacy/plugins/perc_utils.js:1179
  - **Message:** Potential XSS vulnerability in the '$.fn.perc_toggle' plugin.

- **Alert #1625** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1625
  - **Location:** WebUI/src/main/webapp/cm/app/js/legacy/plugins/perc_utils.js:1179
  - **Message:** Potential XSS vulnerability in the '$.fn.perc_toggle' plugin.

- **Alert #1624** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1624
  - **Location:** WebUI/src/main/webapp/cm/widgets/perc_page_edit_dialog.js:344
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1623** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1623
  - **Location:** WebUI/src/main/webapp/cm/widgets/PercInlineEditDataTable.js:290
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1622** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1622
  - **Location:** WebUI/src/main/webapp/cm/widgets/PercInlineEditDataTable.js:228
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1621** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1621
  - **Location:** WebUI/src/main/webapp/cm/widgetbuilder/js/views/PercWidgetFieldsViews.js:161
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1620** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1620
  - **Location:** WebUI/src/main/webapp/cm/widgetbuilder/js/views/PercWidgetBuilderDefinitionView.js:65
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1619** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1619
  - **Location:** WebUI/src/main/webapp/cm/views/PercUserView.js:765
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1618** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1618
  - **Location:** WebUI/src/main/webapp/cm/views/PercUserView.js:741
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1617** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1617
  - **Location:** WebUI/src/main/webapp/cm/views/PercChangeTemplateDialog.js:186
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1616** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1616
  - **Location:** WebUI/src/main/webapp/cm/views/PercCSSGalleryView.js:61
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1615** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1615
  - **Location:** WebUI/src/main/webapp/cm/shared-common-minuet.js:98399
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1614** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1614
  - **Location:** WebUI/src/main/webapp/cm/shared-common-minuet.js:97562
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.\nDOM text is reinterpreted as HTML without escaping meta-characters.\nDOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1613** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1613
  - **Location:** WebUI/src/main/webapp/cm/vendor/js/legacy/profiles/3x/libraries/bootstrap/js/bootstrap.js:1281
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1612** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1612
  - **Location:** WebUI/src/main/webapp/cm/vendor/js/legacy/profiles/3x/libraries/bootstrap/js/bootstrap.bundle.js:1280
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1611** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1611
  - **Location:** WebUI/src/main/webapp/cm/shared-common-minuet.js:93087
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.\nDOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1610** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1610
  - **Location:** WebUI/src/main/webapp/cm/shared-common.js:95139
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1609** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1609
  - **Location:** WebUI/src/main/webapp/cm/shared-common.js:94302
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.\nDOM text is reinterpreted as HTML without escaping meta-characters.\nDOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1608** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1608
  - **Location:** WebUI/src/main/webapp/cm/shared-common.js:89827
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.\nDOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1607** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1607
  - **Location:** WebUI/src/main/webapp/cm/vendor/js/legacy/profiles/3x/jquery/plugins/jquery-ui-multiselect-widget/jquery.multiselect.js:410
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1606** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1606
  - **Location:** WebUI/src/main/webapp/cm/shared-common-minuet.js:60682
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1605** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1605
  - **Location:** WebUI/src/main/webapp/cm/shared-common-minuet.js:60147
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1604** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1604
  - **Location:** WebUI/src/main/webapp/cm/shared-common.js:60722
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1603** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1603
  - **Location:** WebUI/src/main/webapp/cm/shared-common.js:60187
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1602** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1602
  - **Location:** WebUI/src/main/webapp/cm/shared-common-minuet.js:36969
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1601** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1601
  - **Location:** WebUI/src/main/webapp/cm/vendor/js/legacy/profiles/3x/jquery/plugins/jquery-dropdown/jquery.dropdown.js:36
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1600** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1600
  - **Location:** WebUI/src/main/webapp/cm/shared-finder.js:25929
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.\nDOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1599** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1599
  - **Location:** WebUI/src/main/webapp/cm/shared-finder.js:25736
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.\nDOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1598** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1598
  - **Location:** WebUI/src/main/webapp/cm/plugins/perc_utils.js:649
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1597** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1597
  - **Location:** WebUI/src/main/webapp/cm/plugins/PercRedirectHandler.js:295
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1596** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1596
  - **Location:** WebUI/src/main/webapp/cm/plugins/PercListEditorWidget.js:310
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.\nDOM text is reinterpreted as HTML without escaping meta-characters.\nDOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1595** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1595
  - **Location:** WebUI/src/main/webapp/cm/jslib/profiles/3x/libraries/bootstrap/js/bootstrap.bundle.js:1280
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1594** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1594
  - **Location:** WebUI/src/main/webapp/cm/jslib/profiles/3x/libraries/bootstrap/js/bootstrap.js:1281
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1593** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1593
  - **Location:** WebUI/src/main/webapp/cm/jslib/profiles/3x/jquery/plugins/jquery-ui-multiselect-widget/jquery.multiselect.js:410
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1592** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1592
  - **Location:** WebUI/src/main/webapp/cm/jslib/profiles/3x/jquery/plugins/jquery-dropdown/jquery.dropdown.js:36
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1591** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1591
  - **Location:** WebUI/src/main/webapp/cm/classes/perc_template_layout_class.js:86
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1590** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1590
  - **Location:** WebUI/src/main/webapp/cm/app/widgetbuilder/js/views/PercWidgetFieldsViews.js:161
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1589** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1589
  - **Location:** WebUI/src/main/webapp/cm/app/widgetbuilder/js/views/PercWidgetBuilderDefinitionView.js:65
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1588** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1588
  - **Location:** WebUI/src/main/webapp/cm/app/js/legacy/views/PercUserView.js:765
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1587** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1587
  - **Location:** WebUI/src/main/webapp/cm/app/js/legacy/views/PercUserView.js:741
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1586** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1586
  - **Location:** WebUI/src/main/webapp/cm/app/js/legacy/views/PercChangeTemplateDialog.js:186
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1585** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1585
  - **Location:** WebUI/src/main/webapp/cm/app/js/legacy/views/PercCSSGalleryView.js:61
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1584** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1584
  - **Location:** WebUI/src/main/webapp/cm/app/js/legacy/plugins/perc_utils.js:649
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1583** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1583
  - **Location:** WebUI/src/main/webapp/cm/app/js/legacy/plugins/PercRedirectHandler.js:295
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1582** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1582
  - **Location:** WebUI/src/main/webapp/cm/app/js/legacy/plugins/PercListEditorWidget.js:310
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.\nDOM text is reinterpreted as HTML without escaping meta-characters.\nDOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1581** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1581
  - **Location:** WebUI/src/main/webapp/cm/app/js/legacy/classes/perc_template_layout_class.js:86
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1580** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1580
  - **Location:** WebUI/src/main/webapp/cm/widgets/PercDataTable/PercDataTable.js:512
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #1579** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1579
  - **Location:** WebUI/src/main/webapp/cm/widgets/PercDataTable/PercDataTable.js:510
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #1578** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1578
  - **Location:** WebUI/src/main/webapp/cm/widgets/PercDataTable/PercDataTable.js:370
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #1577** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1577
  - **Location:** WebUI/src/main/webapp/cm/widgets/PercDataTable/PercDataTable.js:368
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #1576** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1576
  - **Location:** WebUI/src/main/webapp/cm/widgets/PercDataTable/PercDataTable.js:354
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #1575** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1575
  - **Location:** WebUI/src/main/webapp/cm/widgets/PercDataTable/PercDataTable.js:352
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #1574** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1574
  - **Location:** WebUI/src/main/webapp/cm/widgets/PercDataTableWrong/PercDataTable.js:149
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #1573** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1573
  - **Location:** WebUI/src/main/webapp/cm/widgets/PercDataTableWrong/PercDataTable.js:147
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #1572** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1572
  - **Location:** WebUI/src/main/webapp/cm/widgets/PercDataTableWrong/PercDataTable.js:145
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #1571** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1571
  - **Location:** WebUI/src/main/webapp/cm/widgets/PercDataTableWrong/PercDataTable.js:143
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #1570** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1570
  - **Location:** WebUI/src/main/webapp/cm/vendor/js/legacy/profiles/3x/jquery/plugins/jquery-print-this/printThis.js:194
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #1569** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1569
  - **Location:** WebUI/src/main/webapp/cm/vendor/js/legacy/profiles/3x/jquery/plugins/jquery-print-this/printThis.js:169
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #1568** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1568
  - **Location:** WebUI/src/main/webapp/cm/vendor/js/legacy/profiles/3x/jquery/plugins/jquery-perc-retiredjs/jquery.print-this.js:67
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #1567** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1567
  - **Location:** WebUI/src/main/webapp/cm/vendor/js/legacy/profiles/3x/jquery/plugins/jquery-perc-retiredjs/jquery.blockUI.js:369
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #1566** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1566
  - **Location:** WebUI/src/main/webapp/cm/vendor/js/legacy/profiles/3x/jquery/plugins/jquery-perc-retiredjs/jquery.blockUI.js:362
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #1565** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1565
  - **Location:** WebUI/src/main/webapp/cm/vendor/js/legacy/profiles/3x/jquery/plugins/jquery-perc-retiredjs/jquery.blockUI.js:358
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #1564** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1564
  - **Location:** WebUI/src/main/webapp/cm/vendor/js/legacy/profiles/3x/jquery/plugins/jquery-perc-retiredjs/jquery.blockUI.js:343
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #1563** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1563
  - **Location:** WebUI/src/main/webapp/cm/vendor/js/legacy/profiles/3x/jquery/plugins/jquery-perc-retiredjs/jquery.blockUI.js:311
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #1562** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1562
  - **Location:** WebUI/src/main/webapp/cm/vendor/js/legacy/profiles/3x/jquery/plugins/jquery-layout/jquery.layout_and_plugins.js:2683
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #1561** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1561
  - **Location:** WebUI/src/main/webapp/cm/vendor/js/legacy/profiles/3x/jquery/plugins/jquery-layout/jquery.layout_and_plugins.js:2670
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #1560** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1560
  - **Location:** WebUI/src/main/webapp/cm/shared-common-minuet.js:69740
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.\nThis HTML construction which depends on library input might later allow cross-site scripting.\nThis HTML construction which depends on library input might later allow cross-site scripting.\nThis HTML construction which depends on library input might later allow cross-site scripting.\nThis HTML construction which depends on library input might later allow cross-site scripting.\nThis HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #1559** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1559
  - **Location:** WebUI/src/main/webapp/cm/shared-common-minuet.js:65522
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #1558** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1558
  - **Location:** WebUI/src/main/webapp/cm/shared-common-minuet.js:65509
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #1557** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1557
  - **Location:** WebUI/src/main/webapp/cm/vendor/js/legacy/profiles/3x/jquery/plugins/jquery-jeditable/jquery.jeditable.js:236
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.\nThis HTML construction which depends on library input might later allow cross-site scripting.\nThis HTML construction which depends on library input might later allow cross-site scripting.\nThis HTML construction which depends on library input might later allow cross-site scripting.\nThis HTML construction which depends on library input might later allow cross-site scripting.\nThis HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #1556** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1556
  - **Location:** WebUI/src/main/webapp/cm/shared-common.js:69780
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.\nThis HTML construction which depends on library input might later allow cross-site scripting.\nThis HTML construction which depends on library input might later allow cross-site scripting.\nThis HTML construction which depends on library input might later allow cross-site scripting.\nThis HTML construction which depends on library input might later allow cross-site scripting.\nThis HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #1555** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1555
  - **Location:** WebUI/src/main/webapp/cm/vendor/js/legacy/profiles/3x/jquery/plugins/jquery-form/jquery.form.js:720
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.\nThis HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #1554** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1554
  - **Location:** WebUI/src/main/webapp/cm/vendor/js/legacy/profiles/3x/jquery/plugins/jquery-form/jquery.form.js:519
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #1553** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1553
  - **Location:** WebUI/src/main/webapp/cm/shared-common.js:65562
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #1552** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1552
  - **Location:** WebUI/src/main/webapp/cm/shared-common.js:65549
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #1551** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1551
  - **Location:** WebUI/src/main/webapp/cm/shared-common-minuet.js:43298
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #1550** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1550
  - **Location:** WebUI/src/main/webapp/cm/shared-common-minuet.js:43291
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #1549** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1549
  - **Location:** WebUI/src/main/webapp/cm/shared-common-minuet.js:43287
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #1548** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1548
  - **Location:** WebUI/src/main/webapp/cm/shared-common-minuet.js:43272
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #1547** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1547
  - **Location:** WebUI/src/main/webapp/cm/shared-common-minuet.js:43240
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #1546** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1546
  - **Location:** WebUI/src/main/webapp/cm/shared-common-minuet.js:41996
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.\nThis HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #1545** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1545
  - **Location:** WebUI/src/main/webapp/cm/shared-common-minuet.js:41795
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #1544** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1544
  - **Location:** WebUI/src/main/webapp/cm/shared-common.js:43338
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #1543** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1543
  - **Location:** WebUI/src/main/webapp/cm/shared-common.js:43331
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #1542** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1542
  - **Location:** WebUI/src/main/webapp/cm/shared-common.js:43327
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #1541** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1541
  - **Location:** WebUI/src/main/webapp/cm/shared-common.js:43312
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #1540** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1540
  - **Location:** WebUI/src/main/webapp/cm/shared-common.js:43280
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #1539** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1539
  - **Location:** WebUI/src/main/webapp/cm/shared-common.js:42036
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.\nThis HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #1538** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1538
  - **Location:** WebUI/src/main/webapp/cm/shared-common.js:41835
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #1537** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1537
  - **Location:** WebUI/src/main/webapp/cm/shared-finder.js:22969
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #1536** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1536
  - **Location:** WebUI/src/main/webapp/cm/shared-finder.js:22967
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #1535** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1535
  - **Location:** WebUI/src/main/webapp/cm/shared-finder.js:22827
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #1534** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1534
  - **Location:** WebUI/src/main/webapp/cm/shared-finder.js:22825
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #1533** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1533
  - **Location:** WebUI/src/main/webapp/cm/shared-finder.js:22811
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #1532** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1532
  - **Location:** WebUI/src/main/webapp/cm/shared-finder.js:22809
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #1531** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1531
  - **Location:** WebUI/src/main/webapp/cm/jslib/profiles/3x/jquery/plugins/jquery-layout/jquery.layout_and_plugins.js:2683
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #1530** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1530
  - **Location:** WebUI/src/main/webapp/cm/jslib/profiles/3x/jquery/plugins/jquery-layout/jquery.layout_and_plugins.js:2670
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #1529** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1529
  - **Location:** WebUI/src/main/webapp/cm/jslib/profiles/3x/jquery/plugins/jquery-print-this/printThis.js:194
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #1528** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1528
  - **Location:** WebUI/src/main/webapp/cm/jslib/profiles/3x/jquery/plugins/jquery-print-this/printThis.js:169
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #1527** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1527
  - **Location:** WebUI/src/main/webapp/cm/jslib/profiles/3x/jquery/plugins/jquery-perc-retiredjs/jquery.print-this.js:67
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #1526** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1526
  - **Location:** WebUI/src/main/webapp/cm/jslib/profiles/3x/jquery/plugins/jquery-perc-retiredjs/jquery.blockUI.js:369
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #1525** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1525
  - **Location:** WebUI/src/main/webapp/cm/jslib/profiles/3x/jquery/plugins/jquery-perc-retiredjs/jquery.blockUI.js:362
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #1524** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1524
  - **Location:** WebUI/src/main/webapp/cm/jslib/profiles/3x/jquery/plugins/jquery-perc-retiredjs/jquery.blockUI.js:358
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #1523** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1523
  - **Location:** WebUI/src/main/webapp/cm/jslib/profiles/3x/jquery/plugins/jquery-perc-retiredjs/jquery.blockUI.js:343
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #1522** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1522
  - **Location:** WebUI/src/main/webapp/cm/jslib/profiles/3x/jquery/plugins/jquery-perc-retiredjs/jquery.blockUI.js:311
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #1521** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1521
  - **Location:** WebUI/src/main/webapp/cm/jslib/profiles/3x/jquery/plugins/jquery-jeditable/jquery.jeditable.js:236
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.\nThis HTML construction which depends on library input might later allow cross-site scripting.\nThis HTML construction which depends on library input might later allow cross-site scripting.\nThis HTML construction which depends on library input might later allow cross-site scripting.\nThis HTML construction which depends on library input might later allow cross-site scripting.\nThis HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #1520** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1520
  - **Location:** WebUI/src/main/webapp/cm/jslib/profiles/3x/jquery/plugins/jquery-form/jquery.form.js:720
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.\nThis HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #1519** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1519
  - **Location:** WebUI/src/main/webapp/cm/jslib/profiles/3x/jquery/plugins/jquery-form/jquery.form.js:519
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #1518** — `js/prototype-pollution-utility` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1518
  - **Location:** WebUI/src/main/webapp/cm/widgets/repository/common/lib/jqplot/jquery.jqplot.js:9852
  - **Message:** Properties are copied from options to target without guarding against prototype pollution.

- **Alert #1517** — `js/prototype-pollution-utility` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1517
  - **Location:** WebUI/src/main/webapp/cm/widgets/repository/common/lib/jqplot/jquery.jqplot.js:9796
  - **Message:** Properties are copied from obj2 to obj1 without guarding against prototype pollution.

- **Alert #1516** — `js/prototype-pollution-utility` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1516
  - **Location:** WebUI/src/main/webapp/cm/vendor/js/legacy/profiles/3x/libraries/requirejs/require.js:80
  - **Message:** Properties are copied from e to i without guarding against prototype pollution.

- **Alert #1515** — `js/prototype-pollution-utility` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1515
  - **Location:** WebUI/src/main/webapp/cm/vendor/js/legacy/profiles/3x/jquery/plugins/jquery-fancytree/modules/jquery.fancytree.js:315
  - **Message:** Properties are copied from options to target without guarding against prototype pollution.

- **Alert #1514** — `js/prototype-pollution-utility` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1514
  - **Location:** WebUI/src/main/webapp/cm/vendor/js/legacy/profiles/3x/jquery/plugins/jquery-fancytree/jquery.fancytree-all.js:315
  - **Message:** Properties are copied from options to target without guarding against prototype pollution.

- **Alert #1513** — `js/prototype-pollution-utility` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1513
  - **Location:** WebUI/src/main/webapp/cm/vendor/js/legacy/profiles/3x/jquery/plugins/jquery-fancytree/jquery.fancytree-all-deps.js:1874
  - **Message:** Properties are copied from options to target without guarding against prototype pollution.

- **Alert #1512** — `js/prototype-pollution-utility` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1512
  - **Location:** WebUI/src/main/webapp/cm/shared-finder.js:2946
  - **Message:** Properties are copied from n to s without guarding against prototype pollution.

- **Alert #1511** — `js/prototype-pollution-utility` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1511
  - **Location:** WebUI/src/main/webapp/cm/jslib/profiles/3x/libraries/requirejs/require.js:80
  - **Message:** Properties are copied from e to i without guarding against prototype pollution.

- **Alert #1510** — `js/prototype-pollution-utility` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1510
  - **Location:** WebUI/src/main/webapp/cm/jslib/profiles/3x/jquery/plugins/jquery-fancytree/modules/jquery.fancytree.js:315
  - **Message:** Properties are copied from options to target without guarding against prototype pollution.

- **Alert #1509** — `js/prototype-pollution-utility` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1509
  - **Location:** WebUI/src/main/webapp/cm/jslib/profiles/3x/jquery/plugins/jquery-fancytree/jquery.fancytree-all.js:315
  - **Message:** Properties are copied from options to target without guarding against prototype pollution.

- **Alert #1508** — `js/prototype-pollution-utility` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1508
  - **Location:** WebUI/src/main/webapp/cm/jslib/profiles/3x/jquery/plugins/jquery-fancytree/jquery.fancytree-all-deps.js:1874
  - **Message:** Properties are copied from options to target without guarding against prototype pollution.

- **Alert #1507** — `js/prototype-pollution-utility` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1507
  - **Location:** WebUI/src/main/webapp/cm/gadgets/repository/common/lib/jqplot/jquery.jqplot.js:9852
  - **Message:** Properties are copied from options to target without guarding against prototype pollution.

- **Alert #1506** — `js/prototype-pollution-utility` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1506
  - **Location:** WebUI/src/main/webapp/cm/gadgets/repository/common/lib/jqplot/jquery.jqplot.js:9796
  - **Message:** Properties are copied from obj2 to obj1 without guarding against prototype pollution.

- **Alert #1505** — `js/incomplete-multi-character-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1505
  - **Location:** WebUI/src/main/webapp/cm/shared-common-minuet.js:58282
  - **Message:** This string may still contain <script, which may cause an HTML element injection vulnerability.

- **Alert #1504** — `js/incomplete-multi-character-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1504
  - **Location:** WebUI/src/main/webapp/cm/shared-common-minuet.js:58188
  - **Message:** This string may still contain <script, which may cause an HTML element injection vulnerability.

- **Alert #1503** — `js/incomplete-multi-character-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1503
  - **Location:** WebUI/src/main/webapp/cm/shared-common-minuet.js:49618
  - **Message:** This string may still contain <script, which may cause an HTML element injection vulnerability.

- **Alert #1502** — `js/incomplete-multi-character-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1502
  - **Location:** WebUI/src/main/webapp/cm/shared-common-minuet.js:49357
  - **Message:** This string may still contain <script, which may cause an HTML element injection vulnerability.

- **Alert #1501** — `js/incomplete-multi-character-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1501
  - **Location:** WebUI/src/main/webapp/cm/shared-common.js:58322
  - **Message:** This string may still contain <script, which may cause an HTML element injection vulnerability.

- **Alert #1500** — `js/incomplete-multi-character-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1500
  - **Location:** WebUI/src/main/webapp/cm/shared-common.js:58228
  - **Message:** This string may still contain <script, which may cause an HTML element injection vulnerability.

- **Alert #1499** — `js/incomplete-multi-character-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1499
  - **Location:** WebUI/src/main/webapp/cm/shared-common-minuet.js:45170
  - **Message:** This string may still contain <script, which may cause an HTML element injection vulnerability.

- **Alert #1498** — `js/incomplete-multi-character-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1498
  - **Location:** WebUI/src/main/webapp/cm/shared-common.js:49658
  - **Message:** This string may still contain <script, which may cause an HTML element injection vulnerability.

- **Alert #1497** — `js/incomplete-multi-character-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1497
  - **Location:** WebUI/src/main/webapp/cm/shared-common.js:49397
  - **Message:** This string may still contain <script, which may cause an HTML element injection vulnerability.

- **Alert #1496** — `js/incomplete-multi-character-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1496
  - **Location:** WebUI/src/main/webapp/cm/shared-common.js:45210
  - **Message:** This string may still contain <script, which may cause an HTML element injection vulnerability.

- **Alert #1495** — `js/incomplete-multi-character-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1495
  - **Location:** WebUI/src/main/webapp/cm/vendor/js/legacy/profiles/3x/jquery/plugins/jquery-datatables/js/jquery.dataTables.js:14611
  - **Message:** This string may still contain <script, which may cause an HTML element injection vulnerability.

- **Alert #1494** — `js/incomplete-multi-character-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1494
  - **Location:** WebUI/src/main/webapp/cm/vendor/js/legacy/profiles/3x/jquery/plugins/jquery-datatables/js/jquery.dataTables.js:14517
  - **Message:** This string may still contain <script, which may cause an HTML element injection vulnerability.

- **Alert #1493** — `js/incomplete-multi-character-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1493
  - **Location:** WebUI/src/main/webapp/cm/vendor/js/legacy/profiles/3x/jquery/plugins/jquery-datatables/js/jquery.dataTables.js:5947
  - **Message:** This string may still contain <script, which may cause an HTML element injection vulnerability.

- **Alert #1492** — `js/incomplete-multi-character-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1492
  - **Location:** WebUI/src/main/webapp/cm/vendor/js/legacy/profiles/3x/jquery/plugins/jquery-datatables/js/jquery.dataTables.js:5686
  - **Message:** This string may still contain <script, which may cause an HTML element injection vulnerability.

- **Alert #1491** — `js/incomplete-multi-character-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1491
  - **Location:** WebUI/src/main/webapp/cm/vendor/js/legacy/profiles/3x/jquery/plugins/jquery-datatables/js/jquery.dataTables.js:1499
  - **Message:** This string may still contain <script, which may cause an HTML element injection vulnerability.

- **Alert #1490** — `js/incomplete-multi-character-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1490
  - **Location:** WebUI/src/main/webapp/cm/jslib/profiles/3x/jquery/plugins/jquery-datatables/js/jquery.dataTables.js:14611
  - **Message:** This string may still contain <script, which may cause an HTML element injection vulnerability.

- **Alert #1489** — `js/incomplete-multi-character-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1489
  - **Location:** WebUI/src/main/webapp/cm/jslib/profiles/3x/jquery/plugins/jquery-datatables/js/jquery.dataTables.js:14517
  - **Message:** This string may still contain <script, which may cause an HTML element injection vulnerability.

- **Alert #1488** — `js/incomplete-multi-character-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1488
  - **Location:** WebUI/src/main/webapp/cm/jslib/profiles/3x/jquery/plugins/jquery-datatables/js/jquery.dataTables.js:5947
  - **Message:** This string may still contain <script, which may cause an HTML element injection vulnerability.

- **Alert #1487** — `js/incomplete-multi-character-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1487
  - **Location:** WebUI/src/main/webapp/cm/jslib/profiles/3x/jquery/plugins/jquery-datatables/js/jquery.dataTables.js:5686
  - **Message:** This string may still contain <script, which may cause an HTML element injection vulnerability.

- **Alert #1486** — `js/incomplete-multi-character-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1486
  - **Location:** WebUI/src/main/webapp/cm/jslib/profiles/3x/jquery/plugins/jquery-datatables/js/jquery.dataTables.js:1499
  - **Message:** This string may still contain <script, which may cause an HTML element injection vulnerability.

- **Alert #1485** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1485
  - **Location:** WebUI/src/main/webapp/cm/widgets/perc_site_map.js:2405
  - **Message:** This replaces only the first occurrence of "{{".

- **Alert #1484** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1484
  - **Location:** WebUI/src/main/webapp/cm/vendor/js/legacy/profiles/3x/libraries/momentjs/moment-with-locales.js:803
  - **Message:** This replaces only the first occurrence of "\\\\".

- **Alert #1483** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1483
  - **Location:** WebUI/src/main/webapp/cm/shared-common-minuet.js:94199
  - **Message:** This replaces only the first occurrence of /'/.

- **Alert #1482** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1482
  - **Location:** WebUI/src/main/webapp/cm/shared-common-minuet.js:86390
  - **Message:** This replaces only the first occurrence of "\\\\".

- **Alert #1481** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1481
  - **Location:** WebUI/src/main/webapp/cm/shared-common.js:90939
  - **Message:** This replaces only the first occurrence of /'/.

- **Alert #1480** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1480
  - **Location:** WebUI/src/main/webapp/cm/vendor/js/legacy/profiles/3x/jquery/plugins/jquery-testrunner/testrunner.js:857
  - **Message:** This does not escape backslash characters in the input.

- **Alert #1479** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1479
  - **Location:** WebUI/src/main/webapp/cm/shared-common-minuet.js:51742
  - **Message:** This does not escape backslash characters in the input.

- **Alert #1478** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1478
  - **Location:** WebUI/src/main/webapp/cm/shared-common-minuet.js:48052
  - **Message:** This replaces only the first occurrence of '"'.

- **Alert #1477** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1477
  - **Location:** WebUI/src/main/webapp/cm/shared-common.js:51782
  - **Message:** This does not escape backslash characters in the input.

- **Alert #1476** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1476
  - **Location:** WebUI/src/main/webapp/cm/shared-common.js:48092
  - **Message:** This replaces only the first occurrence of '"'.

- **Alert #1475** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1475
  - **Location:** WebUI/src/main/webapp/cm/vendor/js/legacy/profiles/3x/jquery/libraries/jquery-ui/jquery-ui.js:19507
  - **Message:** This does not escape backslash characters in the input.

- **Alert #1474** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1474
  - **Location:** WebUI/src/main/webapp/cm/vendor/js/legacy/profiles/3x/jquery/plugins/jquery-datatables/js/jquery.dataTables.js:8071
  - **Message:** This does not escape backslash characters in the input.

- **Alert #1473** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1473
  - **Location:** WebUI/src/main/webapp/cm/vendor/js/legacy/profiles/3x/jquery/plugins/jquery-datatables/js/jquery.dataTables.js:4381
  - **Message:** This replaces only the first occurrence of '"'.

- **Alert #1472** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1472
  - **Location:** WebUI/src/main/webapp/cm/vendor/js/legacy/jquery-ui.js:12703
  - **Message:** This does not escape backslash characters in the input.

- **Alert #1471** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1471
  - **Location:** WebUI/src/main/webapp/cm/shared-finder.js:13311
  - **Message:** This replaces only the first occurrence of "%".

- **Alert #1470** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1470
  - **Location:** WebUI/src/main/webapp/cm/services/PercUserService.js:176
  - **Message:** This replaces only the first occurrence of "%".

- **Alert #1469** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1469
  - **Location:** WebUI/src/main/webapp/cm/plugins/perc_utils.js:1761
  - **Message:** This replaces only the first occurrence of /'/.

- **Alert #1468** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1468
  - **Location:** WebUI/src/main/webapp/cm/plugins/perc_css_utils.js:107
  - **Message:** This replaces only the first occurrence of '"'.

- **Alert #1467** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1467
  - **Location:** WebUI/src/main/webapp/cm/perc_common_ui.js:1680
  - **Message:** This replaces only the first occurrence of "\\\\".

- **Alert #1466** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1466
  - **Location:** WebUI/src/main/webapp/cm/plugins/PercSectionTreeDialog.js:169
  - **Message:** This does not escape backslash characters in the input.

- **Alert #1465** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1465
  - **Location:** WebUI/src/main/webapp/cm/plugins/PercSectionTreeDialog.js:169
  - **Message:** This does not escape backslash characters in the input.

- **Alert #1464** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1464
  - **Location:** WebUI/src/main/webapp/cm/plugins/PercSectionTreeDialog.js:159
  - **Message:** This does not escape backslash characters in the input.

- **Alert #1463** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1463
  - **Location:** WebUI/src/main/webapp/cm/plugins/PercSectionTreeDialog.js:159
  - **Message:** This does not escape backslash characters in the input.

- **Alert #1462** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1462
  - **Location:** WebUI/src/main/webapp/cm/jslib/profiles/3x/libraries/momentjs/moment-with-locales.js:803
  - **Message:** This replaces only the first occurrence of "\\\\".

- **Alert #1461** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1461
  - **Location:** WebUI/src/main/webapp/cm/jslib/profiles/3x/jquery/plugins/jquery-testrunner/testrunner.js:857
  - **Message:** This does not escape backslash characters in the input.

- **Alert #1460** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1460
  - **Location:** WebUI/src/main/webapp/cm/jslib/profiles/3x/jquery/libraries/jquery-ui/jquery-ui.js:19507
  - **Message:** This does not escape backslash characters in the input.

- **Alert #1459** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1459
  - **Location:** WebUI/src/main/webapp/cm/jslib/profiles/3x/jquery/plugins/jquery-datatables/js/jquery.dataTables.js:8071
  - **Message:** This does not escape backslash characters in the input.

- **Alert #1458** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1458
  - **Location:** WebUI/src/main/webapp/cm/jslib/jquery-ui.js:12703
  - **Message:** This does not escape backslash characters in the input.

- **Alert #1457** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1457
  - **Location:** WebUI/src/main/webapp/cm/jslib/profiles/3x/jquery/plugins/jquery-datatables/js/jquery.dataTables.js:4381
  - **Message:** This replaces only the first occurrence of '"'.

- **Alert #1456** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1456
  - **Location:** WebUI/src/main/webapp/cm/app/js/legacy/services/PercUserService.js:176
  - **Message:** This replaces only the first occurrence of "%".

- **Alert #1455** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1455
  - **Location:** WebUI/src/main/webapp/cm/app/js/legacy/plugins/perc_utils.js:1761
  - **Message:** This replaces only the first occurrence of /'/.

- **Alert #1454** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1454
  - **Location:** WebUI/src/main/webapp/cm/app/js/legacy/plugins/perc_css_utils.js:107
  - **Message:** This replaces only the first occurrence of '"'.

- **Alert #1453** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1453
  - **Location:** WebUI/src/main/webapp/cm/app/js/legacy/plugins/PercSectionTreeDialog.js:169
  - **Message:** This does not escape backslash characters in the input.

- **Alert #1452** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1452
  - **Location:** WebUI/src/main/webapp/cm/app/js/legacy/plugins/PercSectionTreeDialog.js:169
  - **Message:** This does not escape backslash characters in the input.

- **Alert #1451** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1451
  - **Location:** WebUI/src/main/webapp/cm/app/js/legacy/plugins/PercSectionTreeDialog.js:159
  - **Message:** This does not escape backslash characters in the input.

- **Alert #1450** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1450
  - **Location:** WebUI/src/main/webapp/cm/app/js/legacy/plugins/PercSectionTreeDialog.js:159
  - **Message:** This does not escape backslash characters in the input.

- **Alert #1449** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1449
  - **Location:** WebUI/src/main/webapp/cm/api/lib/handlebars-1.0.0.js:1867
  - **Message:** This replaces only the first occurrence of "\\n".

- **Alert #1448** — `js/unsafe-html-expansion` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1448
  - **Location:** WebUI/src/main/webapp/cm/vendor/js/legacy/profiles/3x/jquery/jquery-migrate-3.3.2.js:823
  - **Message:** This self-closing HTML tag expansion invalidates prior sanitization as this regular expression may match part of an attribute value.

- **Alert #1447** — `js/unsafe-html-expansion` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1447
  - **Location:** WebUI/src/main/webapp/cm/vendor/js/legacy/profiles/3x/jquery/jquery-migrate-3.3.2.js:814
  - **Message:** This self-closing HTML tag expansion invalidates prior sanitization as this regular expression may match part of an attribute value.

- **Alert #1446** — `js/unsafe-html-expansion` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1446
  - **Location:** WebUI/src/main/webapp/cm/jslib/profiles/3x/jquery/jquery-migrate-3.3.2.js:823
  - **Message:** This self-closing HTML tag expansion invalidates prior sanitization as this regular expression may match part of an attribute value.

- **Alert #1445** — `js/unsafe-html-expansion` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1445
  - **Location:** WebUI/src/main/webapp/cm/jslib/profiles/3x/jquery/jquery-migrate-3.3.2.js:814
  - **Message:** This self-closing HTML tag expansion invalidates prior sanitization as this regular expression may match part of an attribute value.

- **Alert #1444** — `js/incomplete-html-attribute-sanitization` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-17T13:00:30Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1444
  - **Location:** WebUI/src/main/webapp/cm/api/lib/highlight.7.3.pack.js:81
  - **Message:** Cross-site scripting vulnerability as the output of this final HTML sanitizer step may contain double quotes when it reaches this attribute definition.

- **Alert #1443** — `java/regex-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-13T21:40:33Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1443
  - **Location:** deliverytiersuite/delivery-tier-suite/metadata/src/main/java/com/percussion/delivery/metadata/rdbms/impl/PSBlogPostVisitDao.java:243
  - **Message:** This regular expression is constructed from a user-provided value.

- **Alert #1442** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T18:18:36Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1442
  - **Location:** modules/perc-packages/src/main/resources/Packages/perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendar/js/jquery.qtip.js:289
  - **Message:** Potential XSS vulnerability in the '$.fn.qtip' plugin.

- **Alert #1441** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T18:18:36Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1441
  - **Location:** modules/perc-packages/src/main/resources/Packages/perc.widgets.image/sys__UserDependency--web_resources/widgets/image/lightbox/lightbox.js:241
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1440** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T18:18:36Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1440
  - **Location:** modules/perc-packages/src/main/resources/Packages/perc.widgets.image/sys__UserDependency--rx_resources/widgets/image/js/jquery.imageAssetControl.js:431
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1439** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T18:18:36Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1439
  - **Location:** modules/perc-packages/src/main/resources/Packages/perc.widgets.image/SupportFile-rx_resources/widgets/image/js/jquery.imageAssetControl.js:431
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1438** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T18:18:36Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1438
  - **Location:** modules/perc-packages/src/main/resources/Packages/perc.widget.form/SupportFile-rx_resources/widgets/form/js/PercFormController.js:1837
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1437** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T18:18:36Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1437
  - **Location:** modules/perc-packages/src/main/resources/Packages/perc.widget.form/SupportFile-rx_resources/widgets/form/js/PercFormController.js:1829
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1436** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T18:18:36Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1436
  - **Location:** modules/perc-packages/src/main/resources/Packages/perc.widget.socialButtons/sys__UserDependency--rx_resources/widgets/percSocialButtons/js/percSocialButtons.js:136
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1435** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T18:18:36Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1435
  - **Location:** modules/perc-packages/src/main/resources/Packages/perc.widget.socialButtons/SupportFile-rx_resources/widgets/percSocialButtons/js/percSocialButtons.js:138
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1434** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T18:18:36Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1434
  - **Location:** modules/perc-packages/src/main/resources/Packages/perc.widget.imageSlider/sys__UserDependency--rx_resources/widgets/percImageSlider/js/percImageSlider.js:322
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1433** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T18:18:36Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1433
  - **Location:** modules/perc-packages/src/main/resources/Packages/perc.widget.imageSlider/sys__UserDependency--rx_resources/widgets/percImageSlider/js/percImageSlider.js:120
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1432** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T18:18:36Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1432
  - **Location:** modules/perc-packages/src/main/resources/Packages/perc.widget.imageSlider/SupportFile-rx_resources/widgets/percImageSlider/js/percImageSlider.js:323
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1431** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T18:18:36Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1431
  - **Location:** modules/perc-packages/src/main/resources/Packages/perc.widget.imageSlider/SupportFile-rx_resources/widgets/percImageSlider/js/percImageSlider.js:120
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1430** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T18:18:36Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1430
  - **Location:** modules/perc-packages/src/main/resources/Packages/perc.widget.form/SupportFile-rx_resources/widgets/form/js/PercFormView.js:524
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.\nDOM text is reinterpreted as HTML without escaping meta-characters.\nDOM text is reinterpreted as HTML without escaping meta-characters.\nDOM text is reinterpreted as HTML without escaping meta-characters.\nDOM text is reinterpreted as HTML without escaping meta-characters.\nDOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1429** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T18:18:36Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1429
  - **Location:** modules/perc-packages/src/main/resources/Packages/perc.widget.directory/sys__UserDependency--web_resources/widgets/directory/js/perc-directory.js:141
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1428** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T18:18:36Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1428
  - **Location:** modules/perc-packages/src/main/resources/Packages/perc.widget.directory/sys__UserDependency--web_resources/widgets/directory/js/perc-directory.js:120
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1426** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T18:18:36Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1426
  - **Location:** modules/perc-packages/src/main/resources/Packages/perc.widget.blogIndexPage/SupportFile-rx_resources/widgets/blogIndexPage/js/jquery.blogIndexPage.js:69
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1425** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T18:18:36Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1425
  - **Location:** modules/perc-packages/src/main/resources/Packages/perc.widget.blogIndexPage/SupportFile-rx_resources/widgets/blogIndexPage/js/jquery.blogIndexPage.js:64
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1424** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T18:18:36Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1424
  - **Location:** modules/perc-packages/src/main/resources/Packages/perc.widget.blogIndexPage/SupportFile-rx_resources/widgets/blogIndexPage/js/jquery.blogIndexPage.js:59
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1423** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T18:18:36Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1423
  - **Location:** modules/perc-packages/src/main/resources/Packages/perc.PageAutoListWidget/sys__UserDependency--rx_resources/widgets/pageAutoList/js/jquery.pageAutoList.js:577
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1422** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T18:18:36Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1422
  - **Location:** modules/perc-packages/src/main/resources/Packages/perc.PageAutoListWidget/sys__UserDependency--rx_resources/widgets/pageAutoList/js/jquery.pageAutoList.js:572
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1421** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T18:18:36Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1421
  - **Location:** modules/perc-packages/src/main/resources/Packages/perc.PageAutoListWidget/sys__UserDependency--rx_resources/widgets/pageAutoList/js/jquery.pageAutoList.js:567
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1420** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T18:18:36Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1420
  - **Location:** modules/perc-packages/src/main/resources/Packages/perc.PageAutoListWidget/sys__UserDependency--rx_resources/widgets/pageAutoList/js/jquery.pageAutoList.js:549
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1419** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T18:18:36Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1419
  - **Location:** modules/perc-packages/src/main/resources/Packages/perc.PageAutoListWidget/sys__UserDependency--rx_resources/widgets/pageAutoList/js/jquery.pageAutoList.js:544
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1418** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T18:18:36Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1418
  - **Location:** modules/perc-packages/src/main/resources/Packages/perc.PageAutoListWidget/sys__UserDependency--rx_resources/widgets/pageAutoList/js/jquery.pageAutoList.js:539
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1417** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T18:18:36Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1417
  - **Location:** modules/perc-packages/src/main/resources/Packages/perc.PageAutoListWidget/SupportFile-rx_resources/widgets/pageAutoList/js/jquery.pageAutoList.js:577
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1416** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T18:18:36Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1416
  - **Location:** modules/perc-packages/src/main/resources/Packages/perc.PageAutoListWidget/SupportFile-rx_resources/widgets/pageAutoList/js/jquery.pageAutoList.js:572
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1415** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T18:18:36Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1415
  - **Location:** modules/perc-packages/src/main/resources/Packages/perc.PageAutoListWidget/SupportFile-rx_resources/widgets/pageAutoList/js/jquery.pageAutoList.js:567
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1414** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T18:18:36Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1414
  - **Location:** modules/perc-packages/src/main/resources/Packages/perc.PageAutoListWidget/SupportFile-rx_resources/widgets/pageAutoList/js/jquery.pageAutoList.js:549
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1413** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T18:18:36Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1413
  - **Location:** modules/perc-packages/src/main/resources/Packages/perc.PageAutoListWidget/SupportFile-rx_resources/widgets/pageAutoList/js/jquery.pageAutoList.js:544
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1412** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T18:18:36Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1412
  - **Location:** modules/perc-packages/src/main/resources/Packages/perc.PageAutoListWidget/SupportFile-rx_resources/widgets/pageAutoList/js/jquery.pageAutoList.js:539
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1411** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T18:18:36Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1411
  - **Location:** modules/perc-packages/src/main/resources/Packages/perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/jquery.qtip.js:1173
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #1410** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T18:18:36Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1410
  - **Location:** modules/perc-packages/src/main/resources/Packages/perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendar/js/jquery.qtip.js:1722
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #1409** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T18:18:36Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1409
  - **Location:** modules/perc-packages/src/main/resources/Packages/perc.CategoryDropDownControl/SupportFile-rx_resources/widgets/categoryDropDown/js/categoryDropdown.js:286
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #1408** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T18:18:36Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1408
  - **Location:** modules/perc-packages/src/main/resources/Packages/perc.CategoryDropDownControl/SupportFile-rx_resources/widgets/categoryDropDown/js/categoryDropdown.js:286
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #1407** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T18:18:36Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1407
  - **Location:** modules/perc-packages/src/main/resources/Packages/perc.baseWidgets/sys__UserDependency--web_resources/cm/jslib/jquery-ui.js:12703
  - **Message:** This does not escape backslash characters in the input.

- **Alert #1406** — `js/polynomial-redos` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1406
  - **Location:** WebUI/src/main/webapp/cm/pages/cui/components/requirejs-text/text.js:39
  - **Message:** This regular expression that depends on library input may run slow on strings starting with '<body' and with many repetitions of '<body'.\nThis regular expression that depends on library input may run slow on strings starting with '<body>' and with many repetitions of '\\t'.\nThis regular expression that depends on library input may run slow on strings starting with '<body>a' and with many repetitions of '\\t'.\nThis regular expression that depends on library input may run slow on strings starting with '<body' and with many repetitions of '<body'.\nThis regular expression that depends on library input may run slow on strings starting with '<body>' and with many repetitions of '\\t'.\nThis regular expression that depends on library input may run slow on strings starting with '<body>a' and with many repetitions of '\\t'.

- **Alert #1405** — `js/polynomial-redos` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1405
  - **Location:** WebUI/src/main/webapp/cm/pages/cui/components/requirejs-text/text.js:38
  - **Message:** This regular expression that depends on library input may run slow on strings starting with '<?xml\\tversion="a' and with many repetitions of '0'.\nThis regular expression that depends on library input may run slow on strings starting with '<?xml\\tversion="a' and with many repetitions of '0'.

- **Alert #1404** — `js/polynomial-redos` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1404
  - **Location:** WebUI/src/main/webapp/cm/cui/components/requirejs-text/text.js:39
  - **Message:** This regular expression that depends on library input may run slow on strings starting with '<body' and with many repetitions of '<body'.\nThis regular expression that depends on library input may run slow on strings starting with '<body>' and with many repetitions of '\\t'.\nThis regular expression that depends on library input may run slow on strings starting with '<body>a' and with many repetitions of '\\t'.\nThis regular expression that depends on library input may run slow on strings starting with '<body' and with many repetitions of '<body'.\nThis regular expression that depends on library input may run slow on strings starting with '<body>' and with many repetitions of '\\t'.\nThis regular expression that depends on library input may run slow on strings starting with '<body>a' and with many repetitions of '\\t'.

- **Alert #1403** — `js/polynomial-redos` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1403
  - **Location:** WebUI/src/main/webapp/cm/cui/components/requirejs-text/text.js:38
  - **Message:** This regular expression that depends on library input may run slow on strings starting with '<?xml\\tversion="a' and with many repetitions of '0'.\nThis regular expression that depends on library input may run slow on strings starting with '<?xml\\tversion="a' and with many repetitions of '0'.

- **Alert #1360** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1360
  - **Location:** WebUI/src/main/webapp/cm/pages/cui/components/twitter-bootstrap-3.0.0/js/collapse.js:31
  - **Message:** Potential XSS vulnerability in the '$.fn.collapse' plugin.

- **Alert #1359** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1359
  - **Location:** WebUI/src/main/webapp/cm/pages/cui/components/jquery-ui/jquery-ui.js:958
  - **Message:** Potential XSS vulnerability in the '$.fn.position' plugin.

- **Alert #1349** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1349
  - **Location:** WebUI/src/main/webapp/cm/cui/components/twitter-bootstrap-3.0.0/js/collapse.js:31
  - **Message:** Potential XSS vulnerability in the '$.fn.collapse' plugin.

- **Alert #1348** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1348
  - **Location:** WebUI/src/main/webapp/cm/cui/components/jquery-ui/jquery-ui.js:958
  - **Message:** Potential XSS vulnerability in the '$.fn.position' plugin.

- **Alert #1314** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1314
  - **Location:** WebUI/src/main/webapp/cm/plugins/perc_template_layout_helper.js:236
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1311** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1311
  - **Location:** WebUI/src/main/webapp/cm/pages/cui/components/twitter-bootstrap-3.0.0/js/tooltip.js:254
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1310** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1310
  - **Location:** WebUI/src/main/webapp/cm/pages/cui/components/twitter-bootstrap-3.0.0/js/tab.js:51
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1309** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1309
  - **Location:** WebUI/src/main/webapp/cm/pages/cui/components/twitter-bootstrap-3.0.0/js/modal.js:230
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1308** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1308
  - **Location:** WebUI/src/main/webapp/cm/pages/cui/components/twitter-bootstrap-3.0.0/js/dropdown.js:112
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1307** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1307
  - **Location:** WebUI/src/main/webapp/cm/pages/cui/components/twitter-bootstrap-3.0.0/js/collapse.js:169
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1306** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1306
  - **Location:** WebUI/src/main/webapp/cm/pages/cui/components/twitter-bootstrap-3.0.0/js/collapse.js:165
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1305** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1305
  - **Location:** WebUI/src/main/webapp/cm/pages/cui/components/twitter-bootstrap-3.0.0/js/carousel.js:196
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1304** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1304
  - **Location:** WebUI/src/main/webapp/cm/pages/cui/components/twitter-bootstrap-3.0.0/js/alert.js:40
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1299** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1299
  - **Location:** WebUI/src/main/webapp/cm/cui/components/twitter-bootstrap-3.0.0/js/tooltip.js:254
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1298** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1298
  - **Location:** WebUI/src/main/webapp/cm/cui/components/twitter-bootstrap-3.0.0/js/tab.js:51
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1297** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1297
  - **Location:** WebUI/src/main/webapp/cm/cui/components/twitter-bootstrap-3.0.0/js/modal.js:230
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1296** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1296
  - **Location:** WebUI/src/main/webapp/cm/cui/components/twitter-bootstrap-3.0.0/js/dropdown.js:112
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1295** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1295
  - **Location:** WebUI/src/main/webapp/cm/cui/components/twitter-bootstrap-3.0.0/js/collapse.js:169
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1294** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1294
  - **Location:** WebUI/src/main/webapp/cm/cui/components/twitter-bootstrap-3.0.0/js/collapse.js:165
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1293** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1293
  - **Location:** WebUI/src/main/webapp/cm/cui/components/twitter-bootstrap-3.0.0/js/carousel.js:196
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1292** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1292
  - **Location:** WebUI/src/main/webapp/cm/cui/components/twitter-bootstrap-3.0.0/js/alert.js:40
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1283** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1283
  - **Location:** WebUI/src/main/webapp/cm/app/js/legacy/plugins/perc_template_layout_helper.js:236
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1279** — `js/xss-through-exception` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1279
  - **Location:** WebUI/src/main/webapp/cm/pages/cui/components/twitter-bootstrap-3.0.0/assets/js/less.js:286
  - **Message:** Exception text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1278** — `js/xss-through-exception` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1278
  - **Location:** WebUI/src/main/webapp/cm/cui/components/twitter-bootstrap-3.0.0/assets/js/less.js:286
  - **Message:** Exception text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1215** — `js/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1215
  - **Location:** WebUI/src/main/webapp/cm/pages/cui/components/twitter-bootstrap-3.0.0/js/tests/vendor/qunit.js:794
  - **Message:** Cross-site scripting vulnerability due to user-provided value.

- **Alert #1214** — `js/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1214
  - **Location:** WebUI/src/main/webapp/cm/pages/cui/components/twitter-bootstrap-3.0.0/js/tests/vendor/qunit.js:705
  - **Message:** Cross-site scripting vulnerability due to user-provided value.\nCross-site scripting vulnerability due to user-provided value.

- **Alert #1213** — `js/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1213
  - **Location:** WebUI/src/main/webapp/cm/pages/cui/components/twitter-bootstrap-3.0.0/js/tests/vendor/qunit.js:549
  - **Message:** Cross-site scripting vulnerability due to user-provided value.

- **Alert #1212** — `js/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1212
  - **Location:** WebUI/src/main/webapp/cm/pages/cui/components/twitter-bootstrap-3.0.0/js/tests/vendor/qunit.js:545
  - **Message:** Cross-site scripting vulnerability due to user-provided value.

- **Alert #1211** — `js/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1211
  - **Location:** WebUI/src/main/webapp/cm/pages/cui/components/twitter-bootstrap-3.0.0/js/tests/vendor/qunit.js:168
  - **Message:** Cross-site scripting vulnerability due to user-provided value.

- **Alert #1210** — `js/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1210
  - **Location:** WebUI/src/main/webapp/cm/pages/cui/components/twitter-bootstrap-3.0.0/js/tests/vendor/qunit.js:40
  - **Message:** Cross-site scripting vulnerability due to user-provided value.

- **Alert #1209** — `js/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1209
  - **Location:** WebUI/src/main/webapp/cm/pages/cui/components/twitter-bootstrap-3.0.0/assets/js/less.js:286
  - **Message:** Cross-site scripting vulnerability due to user-provided value.

- **Alert #1208** — `js/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1208
  - **Location:** WebUI/src/main/webapp/cm/cui/components/twitter-bootstrap-3.0.0/js/tests/vendor/qunit.js:794
  - **Message:** Cross-site scripting vulnerability due to user-provided value.

- **Alert #1207** — `js/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1207
  - **Location:** WebUI/src/main/webapp/cm/cui/components/twitter-bootstrap-3.0.0/js/tests/vendor/qunit.js:705
  - **Message:** Cross-site scripting vulnerability due to user-provided value.\nCross-site scripting vulnerability due to user-provided value.

- **Alert #1206** — `js/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1206
  - **Location:** WebUI/src/main/webapp/cm/cui/components/twitter-bootstrap-3.0.0/js/tests/vendor/qunit.js:549
  - **Message:** Cross-site scripting vulnerability due to user-provided value.

- **Alert #1205** — `js/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1205
  - **Location:** WebUI/src/main/webapp/cm/cui/components/twitter-bootstrap-3.0.0/js/tests/vendor/qunit.js:545
  - **Message:** Cross-site scripting vulnerability due to user-provided value.

- **Alert #1204** — `js/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1204
  - **Location:** WebUI/src/main/webapp/cm/cui/components/twitter-bootstrap-3.0.0/js/tests/vendor/qunit.js:168
  - **Message:** Cross-site scripting vulnerability due to user-provided value.

- **Alert #1203** — `js/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1203
  - **Location:** WebUI/src/main/webapp/cm/cui/components/twitter-bootstrap-3.0.0/js/tests/vendor/qunit.js:40
  - **Message:** Cross-site scripting vulnerability due to user-provided value.

- **Alert #1202** — `js/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1202
  - **Location:** WebUI/src/main/webapp/cm/cui/components/twitter-bootstrap-3.0.0/assets/js/less.js:286
  - **Message:** Cross-site scripting vulnerability due to user-provided value.

- **Alert #1195** — `js/prototype-pollution-utility` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1195
  - **Location:** WebUI/src/main/webapp/cm/pages/cui/require.js:80
  - **Message:** Properties are copied from e to i without guarding against prototype pollution.

- **Alert #1194** — `js/prototype-pollution-utility` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1194
  - **Location:** WebUI/src/main/webapp/cm/pages/cui/components/requirejs/require.js:80
  - **Message:** Properties are copied from e to i without guarding against prototype pollution.

- **Alert #1193** — `js/prototype-pollution-utility` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1193
  - **Location:** WebUI/src/main/webapp/cm/pages/cui/components/knockoutjs/src/google-closure-compiler-utils.js:10
  - **Message:** The property chain here is recursively assigned to target without guarding against prototype pollution.

- **Alert #1187** — `js/prototype-pollution-utility` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1187
  - **Location:** WebUI/src/main/webapp/cm/cui/require.js:80
  - **Message:** Properties are copied from e to i without guarding against prototype pollution.

- **Alert #1186** — `js/prototype-pollution-utility` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1186
  - **Location:** WebUI/src/main/webapp/cm/cui/components/requirejs/require.js:80
  - **Message:** Properties are copied from e to i without guarding against prototype pollution.

- **Alert #1185** — `js/prototype-pollution-utility` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1185
  - **Location:** WebUI/src/main/webapp/cm/cui/components/knockoutjs/src/google-closure-compiler-utils.js:10
  - **Message:** The property chain here is recursively assigned to target without guarding against prototype pollution.

- **Alert #1142** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1142
  - **Location:** WebUI/src/main/webapp/cm/pages/cui/components/jquery-ui/jquery-ui.js:19507
  - **Message:** This does not escape backslash characters in the input.

- **Alert #1141** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1141
  - **Location:** WebUI/src/main/webapp/cm/pages/cui/widgets/app/app.viewmodel.js:86
  - **Message:** This replaces only the first occurrence of /\\[\\\\[\\]/.

- **Alert #1140** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1140
  - **Location:** WebUI/src/main/webapp/cm/pages/cui/widgets/app/app.viewmodel.js:86
  - **Message:** This replaces only the first occurrence of /\\[\\\\]\\]/.

- **Alert #1139** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1139
  - **Location:** WebUI/src/main/webapp/cm/pages/cui/components/twitter-bootstrap-3.0.0/assets/js/less.js:3347
  - **Message:** This does not escape backslash characters in the input.

- **Alert #1138** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1138
  - **Location:** WebUI/src/main/webapp/cm/pages/cui/components/twitter-bootstrap-3.0.0/js/tests/vendor/qunit.js:1166
  - **Message:** This does not escape backslash characters in the input.

- **Alert #1137** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1137
  - **Location:** WebUI/src/main/webapp/cm/pages/cui/components/twitter-bootstrap-3.0.0/assets/js/less.js:72
  - **Message:** This replaces only the first occurrence of "\\\\".

- **Alert #1135** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1135
  - **Location:** WebUI/src/main/webapp/cm/pages/app/includes/siteimprove_integration.html:189
  - **Message:** This does not escape backslash characters in the input.

- **Alert #1134** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1134
  - **Location:** WebUI/src/main/webapp/cm/pages/app/includes/siteimprove_integration.html:169
  - **Message:** This replaces only the first occurrence of "\\\\".

- **Alert #1129** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1129
  - **Location:** WebUI/src/main/webapp/cm/cui/components/jquery-ui/jquery-ui.js:19507
  - **Message:** This does not escape backslash characters in the input.

- **Alert #1128** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1128
  - **Location:** WebUI/src/main/webapp/cm/cui/widgets/app/app.viewmodel.js:86
  - **Message:** This replaces only the first occurrence of /\\[\\\\[\\]/.

- **Alert #1127** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1127
  - **Location:** WebUI/src/main/webapp/cm/cui/widgets/app/app.viewmodel.js:86
  - **Message:** This replaces only the first occurrence of /\\[\\\\]\\]/.

- **Alert #1126** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1126
  - **Location:** WebUI/src/main/webapp/cm/cui/components/twitter-bootstrap-3.0.0/assets/js/less.js:3347
  - **Message:** This does not escape backslash characters in the input.

- **Alert #1125** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1125
  - **Location:** WebUI/src/main/webapp/cm/cui/components/twitter-bootstrap-3.0.0/js/tests/vendor/qunit.js:1166
  - **Message:** This does not escape backslash characters in the input.

- **Alert #1124** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1124
  - **Location:** WebUI/src/main/webapp/cm/cui/components/twitter-bootstrap-3.0.0/assets/js/less.js:72
  - **Message:** This replaces only the first occurrence of "\\\\".

- **Alert #1116** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1116
  - **Location:** WebUI/src/main/webapp/cm/app/includes/siteimprove_integration.html:189
  - **Message:** This does not escape backslash characters in the input.

- **Alert #1115** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1115
  - **Location:** WebUI/src/main/webapp/cm/app/includes/siteimprove_integration.html:169
  - **Message:** This replaces only the first occurrence of "\\\\".

- **Alert #1113** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1113
  - **Location:** WebUI/src/main/webapp/cm/WEB-INF/classes/features/perc/getDashboardColumn/perc_getDashboardColumn.js:11
  - **Message:** This replaces only the first occurrence of /\\[\\\\[\\]/.

- **Alert #1112** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1112
  - **Location:** WebUI/src/main/webapp/cm/WEB-INF/classes/features/perc/getDashboardColumn/perc_getDashboardColumn.js:11
  - **Message:** This replaces only the first occurrence of /\\[\\\\]\\]/.

- **Alert #1111** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1111
  - **Location:** WebUI/src/main/webapp/WEB-INF/classes/features/perc/getDashboardColumn/perc_getDashboardColumn.js:11
  - **Message:** This replaces only the first occurrence of /\\[\\\\[\\]/.

- **Alert #1110** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1110
  - **Location:** WebUI/src/main/webapp/WEB-INF/classes/features/perc/getDashboardColumn/perc_getDashboardColumn.js:11
  - **Message:** This replaces only the first occurrence of /\\[\\\\]\\]/.

- **Alert #1109** — `js/bad-tag-filter` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1109
  - **Location:** WebUI/src/main/webapp/cm/pages/cui/components/knockoutjs/src/utils.domManipulation.js:2
  - **Message:** This regular expression does not match comments containing newlines.

- **Alert #1108** — `js/bad-tag-filter` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1108
  - **Location:** WebUI/src/main/webapp/cm/cui/components/knockoutjs/src/utils.domManipulation.js:2
  - **Message:** This regular expression does not match comments containing newlines.

- **Alert #1105** — `js/unsafe-html-expansion` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1105
  - **Location:** WebUI/src/main/webapp/cm/pages/cui/components/jquery-migrate/jquery-migrate.js:823
  - **Message:** This self-closing HTML tag expansion invalidates prior sanitization as this regular expression may match part of an attribute value.

- **Alert #1104** — `js/unsafe-html-expansion` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1104
  - **Location:** WebUI/src/main/webapp/cm/pages/cui/components/jquery-migrate/jquery-migrate.js:814
  - **Message:** This self-closing HTML tag expansion invalidates prior sanitization as this regular expression may match part of an attribute value.

- **Alert #1101** — `js/unsafe-html-expansion` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1101
  - **Location:** WebUI/src/main/webapp/cm/cui/components/jquery-migrate/jquery-migrate.js:823
  - **Message:** This self-closing HTML tag expansion invalidates prior sanitization as this regular expression may match part of an attribute value.

- **Alert #1100** — `js/unsafe-html-expansion` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1100
  - **Location:** WebUI/src/main/webapp/cm/cui/components/jquery-migrate/jquery-migrate.js:814
  - **Message:** This self-closing HTML tag expansion invalidates prior sanitization as this regular expression may match part of an attribute value.

- **Alert #1090** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1090
  - **Location:** WebUI/src/main/webapp/cm/pages/cui/components/twitter-bootstrap-3.0.0/getting-started.html:120
  - **Message:** Script loaded from content delivery network with no integrity check.

- **Alert #1089** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1089
  - **Location:** WebUI/src/main/webapp/cm/pages/cui/components/twitter-bootstrap-3.0.0/_includes/social-buttons.html:4
  - **Message:** Iframe loaded using unencrypted connection.

- **Alert #1088** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1088
  - **Location:** WebUI/src/main/webapp/cm/pages/cui/components/twitter-bootstrap-3.0.0/_includes/social-buttons.html:7
  - **Message:** Iframe loaded using unencrypted connection.

- **Alert #1087** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1087
  - **Location:** WebUI/src/main/webapp/cm/pages/cui/components/twitter-bootstrap-3.0.0/_includes/footer.html:7
  - **Message:** Script loaded using unencrypted connection.

- **Alert #1086** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1086
  - **Location:** WebUI/src/main/webapp/cm/cui/components/twitter-bootstrap-3.0.0/getting-started.html:120
  - **Message:** Script loaded from content delivery network with no integrity check.

- **Alert #1085** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1085
  - **Location:** WebUI/src/main/webapp/cm/cui/components/twitter-bootstrap-3.0.0/_includes/social-buttons.html:4
  - **Message:** Iframe loaded using unencrypted connection.

- **Alert #1084** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1084
  - **Location:** WebUI/src/main/webapp/cm/cui/components/twitter-bootstrap-3.0.0/_includes/social-buttons.html:7
  - **Message:** Iframe loaded using unencrypted connection.

- **Alert #1083** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:49:57Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1083
  - **Location:** WebUI/src/main/webapp/cm/cui/components/twitter-bootstrap-3.0.0/_includes/footer.html:7
  - **Message:** Script loaded using unencrypted connection.

- **Alert #1082** — `java/xxe` (critical, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:44:14Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1082
  - **Location:** system/business/src/com/percussion/share/dao/PSSerializerUtils.java:88
  - **Message:** XML parsing depends on a user-provided value without guarding against external entity expansion.

- **Alert #1081** — `java/unvalidated-url-redirection` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-07T14:44:14Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1081
  - **Location:** deliverytiersuite/delivery-tier-suite/common/src/main/java/com/percussion/delivery/exceptions/PSUncaughtError.java:60
  - **Message:** Untrusted URL redirection depends on a user-provided value.

- **Alert #1076** — `java/error-message-exposure` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-02T19:49:03Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1076
  - **Location:** projects/sitemanage/src/main/java/com/percussion/designmanagement/service/impl/PSWebResourcesRestService.java:184
  - **Message:** Error information can be exposed to an external user.

- **Alert #1075** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-02T19:45:44Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1075
  - **Location:** system/src/test/resources/com/percussion/delivery/bw-corona.html:6376
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1074** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-02T19:45:44Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1074
  - **Location:** system/src/test/resources/com/percussion/delivery/bw-corona.html:4332
  - **Message:** Script loaded from content delivery network with no integrity check.

- **Alert #1073** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-02T19:45:44Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1073
  - **Location:** system/src/test/resources/com/percussion/delivery/bw-corona.html:6385
  - **Message:** Script loaded from content delivery network with no integrity check.

- **Alert #1072** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-02T19:45:44Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1072
  - **Location:** system/src/test/resources/com/percussion/delivery/bw-corona.html:6386
  - **Message:** Script loaded from content delivery network with no integrity check.

- **Alert #1071** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-02T19:45:44Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1071
  - **Location:** system/src/test/resources/com/percussion/delivery/bw-corona.html:6391
  - **Message:** Script loaded from content delivery network with no integrity check.

- **Alert #1070** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-02T19:45:44Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1070
  - **Location:** system/src/test/resources/com/percussion/delivery/bw-corona.html:6392
  - **Message:** Script loaded from content delivery network with no integrity check.

- **Alert #1069** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-03-02T19:45:44Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1069
  - **Location:** system/src/test/resources/com/percussion/xsl/encoding/Yahoo-EUC-JP.xhtm:46
  - **Message:** Script loaded using unencrypted connection.

- **Alert #1068** — `java/insecure-trustmanager` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-28T05:14:07Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1068
  - **Location:** system/business/src/com/percussion/delivery/client/PSDeliveryClient.java:825
  - **Message:** This uses TrustManager, which is defined in PSDeliveryClient$ and trusts any certificate.

- **Alert #1067** — `java/ssrf` (critical, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-28T05:14:07Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1067
  - **Location:** system/services/src/com/percussion/services/assembly/jexl/PSDocumentUtils.java:236
  - **Message:** Potential server-side request forgery due to a user-provided value.\nPotential server-side request forgery due to a user-provided value.\nPotential server-side request forgery due to a user-provided value.\nPotential server-side request forgery due to a user-provided value.\nPotential server-side request forgery due to a user-provided value.\nPotential server-side request forgery due to a user-provided value.\nPotential server-side request forgery due to a user-provided value.\nPotential server-side request forgery due to a user-provided value.\nPotential server-side request forgery due to a user-provided value.\nPotential server-side request forgery due to a user-provided value.\nPotential server-side request forgery due to a user-provided value.\nPotential server-side request forgery due to a user-provided value.\nPotential server-side request forgery due to a user-provided value.\nPotential server-side request forgery due to a user-provided value.\nPotential server-side request forgery due to a user-provided value.\nPotential server-side request forgery due to a user-provided value.\nPotential server-side request forgery due to a user-provided value.

- **Alert #1066** — `java/ssrf` (critical, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-28T05:14:07Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1066
  - **Location:** system/services/src/com/percussion/services/assembly/jexl/PSDocumentUtils.java:222
  - **Message:** Potential server-side request forgery due to a user-provided value.\nPotential server-side request forgery due to a user-provided value.\nPotential server-side request forgery due to a user-provided value.\nPotential server-side request forgery due to a user-provided value.\nPotential server-side request forgery due to a user-provided value.\nPotential server-side request forgery due to a user-provided value.\nPotential server-side request forgery due to a user-provided value.\nPotential server-side request forgery due to a user-provided value.\nPotential server-side request forgery due to a user-provided value.\nPotential server-side request forgery due to a user-provided value.\nPotential server-side request forgery due to a user-provided value.\nPotential server-side request forgery due to a user-provided value.\nPotential server-side request forgery due to a user-provided value.\nPotential server-side request forgery due to a user-provided value.\nPotential server-side request forgery due to a user-provided value.\nPotential server-side request forgery due to a user-provided value.\nPotential server-side request forgery due to a user-provided value.

- **Alert #1064** — `java/ssrf` (critical, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-28T05:14:07Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1064
  - **Location:** modules/extensions-main/src/main/java/com/percussion/extensions/general/PSProxyQueryResource.java:173
  - **Message:** Potential server-side request forgery due to a user-provided value.\nPotential server-side request forgery due to a user-provided value.\nPotential server-side request forgery due to a user-provided value.\nPotential server-side request forgery due to a user-provided value.\nPotential server-side request forgery due to a user-provided value.\nPotential server-side request forgery due to a user-provided value.\nPotential server-side request forgery due to a user-provided value.\nPotential server-side request forgery due to a user-provided value.\nPotential server-side request forgery due to a user-provided value.\nPotential server-side request forgery due to a user-provided value.\nPotential server-side request forgery due to a user-provided value.\nPotential server-side request forgery due to a user-provided value.\nPotential server-side request forgery due to a user-provided value.\nPotential server-side request forgery due to a user-provided value.\nPotential server-side request forgery due to a user-provided value.\nPotential server-side request forgery due to a user-provided value.\nPotential server-side request forgery due to a user-provided value.

- **Alert #1063** — `java/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T01:02:25Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1063
  - **Location:** projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSiteDataRestService.java:127
  - **Message:** Cross-site scripting vulnerability due to a user-provided value.

- **Alert #1062** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T01:02:25Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1062
  - **Location:** projects/sitemanage/src/main/java/com/percussion/utils/service/impl/PSSiteConfigUtils.java:341
  - **Message:** This path depends on a user-provided value.

- **Alert #1061** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T01:02:25Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1061
  - **Location:** projects/sitemanage/src/main/java/com/percussion/utils/service/impl/PSSiteConfigUtils.java:340
  - **Message:** This path depends on a user-provided value.

- **Alert #1060** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T01:02:25Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1060
  - **Location:** projects/sitemanage/src/main/java/com/percussion/utils/service/impl/PSSiteConfigUtils.java:306
  - **Message:** This path depends on a user-provided value.

- **Alert #1059** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T01:02:25Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1059
  - **Location:** projects/sitemanage/src/main/java/com/percussion/utils/service/impl/PSSiteConfigUtils.java:260
  - **Message:** This path depends on a user-provided value.

- **Alert #1058** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T01:02:25Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1058
  - **Location:** projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSiteDataService.java:498
  - **Message:** This path depends on a user-provided value.

- **Alert #1057** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T01:02:25Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1057
  - **Location:** projects/sitemanage/src/main/java/com/percussion/sitemanage/importer/theme/PSCSSParser.java:275
  - **Message:** This path depends on a user-provided value.

- **Alert #1056** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T01:02:25Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1056
  - **Location:** projects/sitemanage/src/main/java/com/percussion/sitemanage/importer/theme/PSCSSParser.java:264
  - **Message:** This path depends on a user-provided value.

- **Alert #1055** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T01:02:25Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1055
  - **Location:** projects/sitemanage/src/main/java/com/percussion/sitemanage/importer/theme/PSCSSParser.java:236
  - **Message:** This path depends on a user-provided value.

- **Alert #1054** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T01:02:25Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1054
  - **Location:** projects/sitemanage/src/main/java/com/percussion/sitemanage/importer/helpers/impl/PSImportThemeHelper.java:216
  - **Message:** This path depends on a user-provided value.

- **Alert #1053** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T01:02:25Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1053
  - **Location:** projects/sitemanage/src/main/java/com/percussion/pathmanagement/service/impl/PSFileSystemPathItemService.java:207
  - **Message:** This path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.

- **Alert #1049** — `java/error-message-exposure` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T01:02:25Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1049
  - **Location:** projects/sitemanage/src/main/java/com/percussion/foldermanagement/service/impl/PSFolderRestService.java:156
  - **Message:** Error information can be exposed to an external user.\nError information can be exposed to an external user.

- **Alert #1044** — `java/sql-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T01:02:25Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1044
  - **Location:** projects/sitemanage/src/main/java/com/percussion/pagemanagement/dao/impl/PSPageDaoHelper.java:386
  - **Message:** This query depends on a user-provided value.

- **Alert #1043** — `java/regex-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T01:02:25Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1043
  - **Location:** projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSiteDataService.java:1797
  - **Message:** This regular expression is constructed from a user-provided value.

- **Alert #1042** — `java/regex-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T01:02:25Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1042
  - **Location:** projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSiteDataService.java:1795
  - **Message:** This regular expression is constructed from a user-provided value.

- **Alert #1041** — `java/regex-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T01:02:25Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1041
  - **Location:** projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSiteDataService.java:1793
  - **Message:** This regular expression is constructed from a user-provided value.

- **Alert #1040** — `js/redos` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1040
  - **Location:** deliverytiersuite/delivery-tier-suite/p13n-ds/src-js/p13n/perc_p13n_profile.js:325
  - **Message:** This part of the regular expression may cause exponential backtracking on strings starting with '<script' and containing many repetitions of '\\t'.

- **Alert #1039** — `js/redos` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1039
  - **Location:** deliverytiersuite/delivery-tier-suite/p13n-ds/lib-js/jquery-treeview/lib/jquery.js:2568
  - **Message:** This part of the regular expression may cause exponential backtracking on strings starting with '<script' and containing many repetitions of '\\t'.

- **Alert #1038** — `js/redos` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1038
  - **Location:** deliverytiersuite/delivery-tier-suite/p13n-ds/lib-js/jquery-treeview/lib/jquery.js:29
  - **Message:** This part of the regular expression may cause exponential backtracking on strings starting with '<' and containing many repetitions of '\\t'.

- **Alert #1037** — `js/polynomial-redos` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1037
  - **Location:** WebUI/war/cui/components/requirejs-text/text.js:39
  - **Message:** This regular expression that depends on library input may run slow on strings starting with '<body' and with many repetitions of '<body'.\nThis regular expression that depends on library input may run slow on strings starting with '<body>' and with many repetitions of '\\t'.\nThis regular expression that depends on library input may run slow on strings starting with '<body>a' and with many repetitions of '\\t'.\nThis regular expression that depends on library input may run slow on strings starting with '<body' and with many repetitions of '<body'.\nThis regular expression that depends on library input may run slow on strings starting with '<body>' and with many repetitions of '\\t'.\nThis regular expression that depends on library input may run slow on strings starting with '<body>a' and with many repetitions of '\\t'.

- **Alert #1036** — `js/polynomial-redos` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1036
  - **Location:** WebUI/war/cui/components/requirejs-text/text.js:38
  - **Message:** This regular expression that depends on library input may run slow on strings starting with '<?xml\\tversion="a' and with many repetitions of '0'.\nThis regular expression that depends on library input may run slow on strings starting with '<?xml\\tversion="a' and with many repetitions of '0'.

- **Alert #1034** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1034
  - **Location:** deliverytiersuite/delivery-tier-suite/p13n-ds/lib-js/jquery-treeview/jquery.treeview.js:250
  - **Message:** Potential XSS vulnerability in the '$.fn.treeview' plugin.

- **Alert #1033** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1033
  - **Location:** WebUI/war/cui/components/twitter-bootstrap-3.0.0/js/collapse.js:31
  - **Message:** Potential XSS vulnerability in the '$.fn.collapse' plugin.

- **Alert #1032** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1032
  - **Location:** WebUI/war/cui/components/twitter-bootstrap-3.0.0/dist/js/bootstrap.js:524
  - **Message:** Potential XSS vulnerability in the '$.fn.collapse' plugin.

- **Alert #1031** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1031
  - **Location:** WebUI/war/cui/components/jquery-ui/jquery-ui.js:958
  - **Message:** Potential XSS vulnerability in the '$.fn.position' plugin.

- **Alert #1030** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1030
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/trinidad/adf/jsLibsDebug/Core.js:1353
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.\nDOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #1029** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/1029
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/trinidad/adf/jsLibs/Core.js:696
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.\nDOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #997** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/997
  - **Location:** WebUI/war/shared-finder.js:17705
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.\nDOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #996** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/996
  - **Location:** WebUI/war/shared-finder.js:17521
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.\nDOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #995** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/995
  - **Location:** modules/perc-toolkit/src/main/resources/InstallDir/rx_resources/js/cropper_custom.js:94
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #994** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/994
  - **Location:** modules/perc-tinymce/src/main/resources/META-INF/resources/sys_resources/tinymce/plugins/percglobalvariables/plugin.js:180
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #993** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/993
  - **Location:** modules/perc-common-ui-bundle/src/main/js/views/PercRssView.js:180
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #992** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/992
  - **Location:** modules/perc-common-ui-bundle/src/main/js/views/PercRssView.js:175
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #991** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/991
  - **Location:** modules/perc-common-ui-bundle/src/main/js/views/PercTagListView.js:125
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #990** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/990
  - **Location:** modules/perc-common-ui-bundle/src/main/js/views/PercRssView.js:82
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #989** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/989
  - **Location:** modules/perc-common-ui-bundle/src/main/js/views/PercRegistrationView.js:270
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #988** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/988
  - **Location:** modules/perc-common-ui-bundle/src/main/js/views/PercRegistrationView.js:80
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #987** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/987
  - **Location:** modules/perc-common-ui-bundle/src/main/js/views/PercMostReadBlogPostsView.js:102
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #986** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/986
  - **Location:** modules/perc-common-ui-bundle/src/main/js/views/PercMostReadBlogPostsView.js:67
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #985** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/985
  - **Location:** modules/perc-common-ui-bundle/src/main/js/views/PercCategoryListView.js:220
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #984** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/984
  - **Location:** modules/perc-common-ui-bundle/src/main/js/views/PercBlogPostView.js:167
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #983** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/983
  - **Location:** modules/perc-common-ui-bundle/src/main/js/views/PercBlogPostView.js:151
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #982** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/982
  - **Location:** modules/perc-common-ui-bundle/src/main/js/views/PercArchiveListView.js:317
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #981** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/981
  - **Location:** modules/perc-common-ui-bundle/src/main/js/views/PercArchiveListView.js:217
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #980** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/980
  - **Location:** modules/perc-common-ui-bundle/src/main/js/views/PercArchiveListView.js:146
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #979** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/979
  - **Location:** deliverytiersuite/delivery-tier-suite/p13n-ds/lib-js/jquery-form/jquery.form.js:404
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #978** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/978
  - **Location:** WebUI/war/jslib/profiles/3x/libraries/bootstrap/js/bootstrap.bundle.js:1100
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #977** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/977
  - **Location:** WebUI/war/jslib/profiles/3x/libraries/bootstrap/js/bootstrap.js:1101
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #976** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/976
  - **Location:** WebUI/war/cui/components/twitter-bootstrap-3.0.0/js/tooltip.js:254
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #975** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/975
  - **Location:** WebUI/war/cui/components/twitter-bootstrap-3.0.0/js/tab.js:51
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #974** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/974
  - **Location:** WebUI/war/cui/components/twitter-bootstrap-3.0.0/js/modal.js:230
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #973** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/973
  - **Location:** WebUI/war/cui/components/twitter-bootstrap-3.0.0/js/dropdown.js:112
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #972** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/972
  - **Location:** WebUI/war/cui/components/twitter-bootstrap-3.0.0/dist/js/bootstrap.js:1790
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #971** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/971
  - **Location:** WebUI/war/cui/components/twitter-bootstrap-3.0.0/js/collapse.js:169
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #970** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/970
  - **Location:** WebUI/war/cui/components/twitter-bootstrap-3.0.0/js/collapse.js:165
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #969** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/969
  - **Location:** WebUI/war/cui/components/twitter-bootstrap-3.0.0/dist/js/bootstrap.js:1329
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #968** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/968
  - **Location:** WebUI/war/cui/components/twitter-bootstrap-3.0.0/js/carousel.js:196
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #967** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/967
  - **Location:** WebUI/war/cui/components/twitter-bootstrap-3.0.0/dist/js/bootstrap.js:1058
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #966** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/966
  - **Location:** WebUI/war/cui/components/twitter-bootstrap-3.0.0/dist/js/bootstrap.js:785
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #965** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/965
  - **Location:** WebUI/war/cui/components/twitter-bootstrap-3.0.0/dist/js/bootstrap.js:662
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #964** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/964
  - **Location:** WebUI/war/cui/components/twitter-bootstrap-3.0.0/dist/js/bootstrap.js:658
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #963** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/963
  - **Location:** WebUI/war/cui/components/twitter-bootstrap-3.0.0/js/alert.js:40
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #962** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/962
  - **Location:** WebUI/war/cui/components/twitter-bootstrap-3.0.0/dist/js/bootstrap.js:471
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #961** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/961
  - **Location:** WebUI/war/cui/components/twitter-bootstrap-3.0.0/dist/js/bootstrap.js:106
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #960** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/960
  - **Location:** WebUI/war/cui/components/knockoutjs/build/knockout-raw.js:23
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #959** — `js/xss-through-exception` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/959
  - **Location:** WebUI/war/cui/components/twitter-bootstrap-3.0.0/assets/js/less.js:286
  - **Message:** Exception text is reinterpreted as HTML without escaping meta-characters.

- **Alert #953** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/953
  - **Location:** WebUI/war/shared-finder.js:15045
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #952** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/952
  - **Location:** WebUI/war/shared-finder.js:15045
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #951** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/951
  - **Location:** WebUI/war/shared-finder.js:14913
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #950** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/950
  - **Location:** WebUI/war/shared-finder.js:14913
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #949** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/949
  - **Location:** WebUI/war/shared-finder.js:14911
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #948** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/948
  - **Location:** WebUI/war/shared-finder.js:14911
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #947** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/947
  - **Location:** deliverytiersuite/delivery-tier-suite/p13n-ds/lib-js/jquery-form/jquery.form.js:324
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #946** — `js/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/946
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/webimagefx/webimagefx.js:49
  - **Message:** Cross-site scripting vulnerability due to user-provided value.

- **Alert #945** — `js/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/945
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/mobilepreview/js/PercMobilePreview.js:11
  - **Message:** Cross-site scripting vulnerability due to user-provided value.

- **Alert #944** — `js/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/944
  - **Location:** WebUI/war/cui/components/twitter-bootstrap-3.0.0/js/tests/vendor/qunit.js:794
  - **Message:** Cross-site scripting vulnerability due to user-provided value.

- **Alert #943** — `js/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/943
  - **Location:** WebUI/war/cui/components/twitter-bootstrap-3.0.0/js/tests/vendor/qunit.js:705
  - **Message:** Cross-site scripting vulnerability due to user-provided value.\nCross-site scripting vulnerability due to user-provided value.

- **Alert #942** — `js/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/942
  - **Location:** WebUI/war/cui/components/twitter-bootstrap-3.0.0/js/tests/vendor/qunit.js:549
  - **Message:** Cross-site scripting vulnerability due to user-provided value.

- **Alert #941** — `js/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/941
  - **Location:** WebUI/war/cui/components/twitter-bootstrap-3.0.0/js/tests/vendor/qunit.js:545
  - **Message:** Cross-site scripting vulnerability due to user-provided value.

- **Alert #940** — `js/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/940
  - **Location:** WebUI/war/cui/components/twitter-bootstrap-3.0.0/js/tests/vendor/qunit.js:168
  - **Message:** Cross-site scripting vulnerability due to user-provided value.

- **Alert #939** — `js/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/939
  - **Location:** WebUI/war/cui/components/twitter-bootstrap-3.0.0/js/tests/vendor/qunit.js:40
  - **Message:** Cross-site scripting vulnerability due to user-provided value.

- **Alert #938** — `js/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/938
  - **Location:** WebUI/war/cui/components/twitter-bootstrap-3.0.0/assets/js/less.js:286
  - **Message:** Cross-site scripting vulnerability due to user-provided value.

- **Alert #935** — `js/prototype-pollution-utility` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/935
  - **Location:** deliverytiersuite/delivery-tier-suite/p13n-ds/lib-js/jquery-treeview/lib/jquery.js:592
  - **Message:** Properties are copied from options to target without guarding against prototype pollution.

- **Alert #934** — `js/prototype-pollution-utility` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/934
  - **Location:** WebUI/war/shared-finder.js:33
  - **Message:** Properties are copied from n to s without guarding against prototype pollution.

- **Alert #933** — `js/prototype-pollution-utility` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/933
  - **Location:** WebUI/war/cui/require.js:80
  - **Message:** Properties are copied from e to i without guarding against prototype pollution.

- **Alert #932** — `js/prototype-pollution-utility` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/932
  - **Location:** WebUI/war/cui/components/requirejs/require.js:80
  - **Message:** Properties are copied from e to i without guarding against prototype pollution.

- **Alert #931** — `js/prototype-pollution-utility` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/931
  - **Location:** WebUI/war/cui/components/knockoutjs/src/google-closure-compiler-utils.js:10
  - **Message:** The property chain here is recursively assigned to target without guarding against prototype pollution.

- **Alert #930** — `js/prototype-pollution-utility` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/930
  - **Location:** WebUI/war/cui/components/knockoutjs/dist/knockout.debug.js:42
  - **Message:** The property chain here is recursively assigned to target without guarding against prototype pollution.

- **Alert #929** — `js/unvalidated-dynamic-method-call` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/929
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/trinidad/adf/jsLibs/Core.js:384
  - **Message:** Invocation of method with user-controlled name may dispatch to unexpected target and cause an exception.

- **Alert #928** — `js/code-injection` (critical, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/928
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/dojo/src/io/RepubsubIO.js:115
  - **Message:** This code execution depends on a user-provided value.

- **Alert #926** — `js/code-injection` (critical, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/926
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/dojo/dojo.js:677
  - **Message:** This code execution depends on a user-provided value.

- **Alert #925** — `js/code-injection` (critical, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/925
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/dojo/src/hostenv_browser.js:28
  - **Message:** This code execution depends on a user-provided value.

- **Alert #923** — `js/incomplete-multi-character-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/923
  - **Location:** deliverytiersuite/delivery-tier-suite/p13n-ds/src-js/p13n/perc_p13n_profile.js:325
  - **Message:** This string may still contain <script, which may cause an HTML element injection vulnerability.

- **Alert #922** — `js/incomplete-multi-character-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/922
  - **Location:** deliverytiersuite/delivery-tier-suite/p13n-ds/lib-js/jquery-treeview/lib/jquery.js:2568
  - **Message:** This string may still contain <script, which may cause an HTML element injection vulnerability.

- **Alert #921** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/921
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy.js:4779
  - **Message:** This does not escape backslash characters in the input.

- **Alert #920** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/920
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy.js:4779
  - **Message:** This does not escape backslash characters in the input.

- **Alert #919** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/919
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy.js:4779
  - **Message:** This does not escape backslash characters in the input.

- **Alert #918** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/918
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy-2stage-to-3stage.js:4804
  - **Message:** This does not escape backslash characters in the input.

- **Alert #917** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/917
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy-2stage-to-3stage.js:4804
  - **Message:** This does not escape backslash characters in the input.

- **Alert #916** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/916
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy-2stage-to-3stage.js:4804
  - **Message:** This does not escape backslash characters in the input.

- **Alert #915** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/915
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy-3stage-to-2stage.js:4764
  - **Message:** This does not escape backslash characters in the input.

- **Alert #914** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/914
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy-3stage-to-2stage.js:4764
  - **Message:** This does not escape backslash characters in the input.

- **Alert #913** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/913
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy-3stage-to-2stage.js:4764
  - **Message:** This does not escape backslash characters in the input.

- **Alert #912** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/912
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy.js:1727
  - **Message:** This does not escape backslash characters in the input.

- **Alert #911** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/911
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy.js:1727
  - **Message:** This does not escape backslash characters in the input.

- **Alert #910** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/910
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy.js:1727
  - **Message:** This does not escape backslash characters in the input.

- **Alert #909** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/909
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy.js:1708
  - **Message:** This does not escape backslash characters in the input.

- **Alert #908** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/908
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy.js:1708
  - **Message:** This does not escape backslash characters in the input.

- **Alert #907** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/907
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy.js:1708
  - **Message:** This does not escape backslash characters in the input.

- **Alert #906** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/906
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy.js:708
  - **Message:** This does not escape backslash characters in the input.

- **Alert #905** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/905
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy.js:708
  - **Message:** This does not escape backslash characters in the input.

- **Alert #904** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/904
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy.js:708
  - **Message:** This does not escape backslash characters in the input.

- **Alert #903** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/903
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy.js:692
  - **Message:** This does not escape backslash characters in the input.

- **Alert #902** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/902
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy.js:692
  - **Message:** This does not escape backslash characters in the input.

- **Alert #901** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/901
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy.js:692
  - **Message:** This does not escape backslash characters in the input.

- **Alert #900** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/900
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy.js:665
  - **Message:** This does not escape backslash characters in the input.

- **Alert #899** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/899
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy.js:665
  - **Message:** This does not escape backslash characters in the input.

- **Alert #898** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/898
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy.js:665
  - **Message:** This does not escape backslash characters in the input.

- **Alert #897** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/897
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy-3stage-to-2stage.js:1727
  - **Message:** This does not escape backslash characters in the input.

- **Alert #896** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/896
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy-2stage-to-3stage.js:1727
  - **Message:** This does not escape backslash characters in the input.

- **Alert #895** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/895
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy-3stage-to-2stage.js:1727
  - **Message:** This does not escape backslash characters in the input.

- **Alert #894** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/894
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy-2stage-to-3stage.js:1727
  - **Message:** This does not escape backslash characters in the input.

- **Alert #893** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/893
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy-2stage-to-3stage.js:1727
  - **Message:** This does not escape backslash characters in the input.

- **Alert #892** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/892
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy-3stage-to-2stage.js:1727
  - **Message:** This does not escape backslash characters in the input.

- **Alert #891** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/891
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy-3stage-to-2stage.js:1708
  - **Message:** This does not escape backslash characters in the input.

- **Alert #890** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/890
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy-3stage-to-2stage.js:1708
  - **Message:** This does not escape backslash characters in the input.

- **Alert #889** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/889
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy-3stage-to-2stage.js:1708
  - **Message:** This does not escape backslash characters in the input.

- **Alert #888** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/888
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy-2stage-to-3stage.js:1708
  - **Message:** This does not escape backslash characters in the input.

- **Alert #887** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/887
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy-2stage-to-3stage.js:1708
  - **Message:** This does not escape backslash characters in the input.

- **Alert #886** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/886
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy-2stage-to-3stage.js:1708
  - **Message:** This does not escape backslash characters in the input.

- **Alert #885** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/885
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy-2stage-to-3stage.js:708
  - **Message:** This does not escape backslash characters in the input.

- **Alert #884** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/884
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy-2stage-to-3stage.js:708
  - **Message:** This does not escape backslash characters in the input.

- **Alert #883** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/883
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy-2stage-to-3stage.js:708
  - **Message:** This does not escape backslash characters in the input.

- **Alert #882** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/882
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy-2stage-to-3stage.js:692
  - **Message:** This does not escape backslash characters in the input.

- **Alert #881** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/881
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy-2stage-to-3stage.js:692
  - **Message:** This does not escape backslash characters in the input.

- **Alert #880** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/880
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy-2stage-to-3stage.js:692
  - **Message:** This does not escape backslash characters in the input.

- **Alert #879** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/879
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy-2stage-to-3stage.js:665
  - **Message:** This does not escape backslash characters in the input.

- **Alert #878** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/878
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy-2stage-to-3stage.js:665
  - **Message:** This does not escape backslash characters in the input.

- **Alert #877** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/877
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy-2stage-to-3stage.js:665
  - **Message:** This does not escape backslash characters in the input.

- **Alert #876** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/876
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy-3stage-to-2stage.js:708
  - **Message:** This does not escape backslash characters in the input.

- **Alert #875** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/875
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy-3stage-to-2stage.js:708
  - **Message:** This does not escape backslash characters in the input.

- **Alert #874** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/874
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy-3stage-to-2stage.js:708
  - **Message:** This does not escape backslash characters in the input.

- **Alert #873** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/873
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy-3stage-to-2stage.js:692
  - **Message:** This does not escape backslash characters in the input.

- **Alert #872** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/872
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy-3stage-to-2stage.js:692
  - **Message:** This does not escape backslash characters in the input.

- **Alert #871** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/871
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy-3stage-to-2stage.js:692
  - **Message:** This does not escape backslash characters in the input.

- **Alert #870** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/870
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy-3stage-to-2stage.js:665
  - **Message:** This does not escape backslash characters in the input.

- **Alert #869** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/869
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy-3stage-to-2stage.js:665
  - **Message:** This does not escape backslash characters in the input.

- **Alert #868** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/868
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/js/taxonomy/jquery.jstree.rev236-taxonomy-3stage-to-2stage.js:665
  - **Message:** This does not escape backslash characters in the input.

- **Alert #867** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/867
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/dojo/src/string/extras.js:160
  - **Message:** This does not escape backslash characters in the input.

- **Alert #865** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/865
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/dojo/dojo.js:3782
  - **Message:** This does not escape backslash characters in the input.

- **Alert #860** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/860
  - **Location:** system/Docs/Rhythmyx_Workflow_Tab_Help/dhtml_search.js:878
  - **Message:** This replaces only the first occurrence of "<".

- **Alert #859** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/859
  - **Location:** system/Docs/Rhythmyx_Workflow_Tab_Help/dhtml_search.js:878
  - **Message:** This replaces only the first occurrence of ">".

- **Alert #858** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/858
  - **Location:** system/Docs/Rhythmyx_Workflow_Tab_Help/dhtml_search.js:878
  - **Message:** This replaces only the first occurrence of '"'.

- **Alert #857** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/857
  - **Location:** system/Docs/Rhythmyx_Publishing_Design_Help/dhtml_search.js:1677
  - **Message:** This replaces only the first occurrence of "<".

- **Alert #856** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/856
  - **Location:** system/Docs/Rhythmyx_Publishing_Design_Help/dhtml_search.js:1677
  - **Message:** This replaces only the first occurrence of ">".

- **Alert #855** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/855
  - **Location:** system/Docs/Rhythmyx_Publishing_Design_Help/dhtml_search.js:1677
  - **Message:** This replaces only the first occurrence of '"'.

- **Alert #854** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/854
  - **Location:** system/Docs/Rhythmyx_Publishing_Runtime_Help/dhtml_search.js:200
  - **Message:** This replaces only the first occurrence of "<".

- **Alert #853** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/853
  - **Location:** system/Docs/Rhythmyx_Publishing_Runtime_Help/dhtml_search.js:200
  - **Message:** This replaces only the first occurrence of ">".

- **Alert #852** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/852
  - **Location:** system/Docs/Rhythmyx_Publishing_Runtime_Help/dhtml_search.js:200
  - **Message:** This replaces only the first occurrence of '"'.

- **Alert #851** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/851
  - **Location:** system/Docs/Rhythmyx_Administration_Tab_Help/dhtml_search.js:1008
  - **Message:** This replaces only the first occurrence of "<".

- **Alert #850** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/850
  - **Location:** system/Docs/Rhythmyx_Administration_Tab_Help/dhtml_search.js:1008
  - **Message:** This replaces only the first occurrence of ">".

- **Alert #849** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/849
  - **Location:** system/Docs/Rhythmyx_Administration_Tab_Help/dhtml_search.js:1008
  - **Message:** This replaces only the first occurrence of '"'.

- **Alert #848** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/848
  - **Location:** system/Docs/Percussion_Package_Manager_Help/dhtml_search.js:165
  - **Message:** This replaces only the first occurrence of "<".

- **Alert #847** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/847
  - **Location:** system/Docs/Percussion_Package_Manager_Help/dhtml_search.js:165
  - **Message:** This replaces only the first occurrence of ">".

- **Alert #846** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/846
  - **Location:** system/Docs/Percussion_Package_Manager_Help/dhtml_search.js:165
  - **Message:** This replaces only the first occurrence of '"'.

- **Alert #845** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/845
  - **Location:** system/Docs/Active_Assembly_Tutorial/dhtml_search.js:491
  - **Message:** This replaces only the first occurrence of "<".

- **Alert #844** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/844
  - **Location:** system/Docs/Active_Assembly_Tutorial/dhtml_search.js:491
  - **Message:** This replaces only the first occurrence of ">".

- **Alert #843** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/843
  - **Location:** system/Docs/Active_Assembly_Tutorial/dhtml_search.js:491
  - **Message:** This replaces only the first occurrence of '"'.

- **Alert #842** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/842
  - **Location:** system/Docs/Active_Assembly_Interface/dhtml_search.js:1090
  - **Message:** This replaces only the first occurrence of "<".

- **Alert #841** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/841
  - **Location:** system/Docs/Active_Assembly_Interface/dhtml_search.js:1090
  - **Message:** This replaces only the first occurrence of ">".

- **Alert #840** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/840
  - **Location:** system/Docs/Active_Assembly_Interface/dhtml_search.js:1090
  - **Message:** This replaces only the first occurrence of '"'.

- **Alert #839** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/839
  - **Location:** WebUI/war/shared-finder.js:6235
  - **Message:** This replaces only the first occurrence of "%".

- **Alert #838** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/838
  - **Location:** deliverytiersuite/delivery-tier-suite/p13n-ds/lib-js/log4javascript/log4javascript_uncompressed.js:1073
  - **Message:** This does not escape backslash characters in the input.

- **Alert #837** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/837
  - **Location:** deliverytiersuite/delivery-tier-suite/p13n-ds/lib-js/log4javascript/log4javascript.js:876
  - **Message:** This does not escape backslash characters in the input.

- **Alert #836** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/836
  - **Location:** WebUI/war/perc_common_ui.js:1616
  - **Message:** This replaces only the first occurrence of '\\\\'.

- **Alert #835** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/835
  - **Location:** WebUI/war/plugins/PercSectionTreeDialog.js:169
  - **Message:** This does not escape backslash characters in the input.

- **Alert #834** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/834
  - **Location:** WebUI/war/plugins/PercSectionTreeDialog.js:169
  - **Message:** This does not escape backslash characters in the input.

- **Alert #833** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/833
  - **Location:** WebUI/war/plugins/PercSectionTreeDialog.js:159
  - **Message:** This does not escape backslash characters in the input.

- **Alert #832** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/832
  - **Location:** WebUI/war/plugins/PercSectionTreeDialog.js:159
  - **Message:** This does not escape backslash characters in the input.

- **Alert #831** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/831
  - **Location:** WebUI/war/cui/components/jquery-ui/jquery-ui.js:19507
  - **Message:** This does not escape backslash characters in the input.

- **Alert #830** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/830
  - **Location:** WebUI/war/cui/widgets/app/app.viewmodel.js:86
  - **Message:** This replaces only the first occurrence of /\\[\\\\[\\]/.

- **Alert #829** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/829
  - **Location:** WebUI/war/cui/widgets/app/app.viewmodel.js:86
  - **Message:** This replaces only the first occurrence of /\\[\\\\]\\]/.

- **Alert #828** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/828
  - **Location:** WebUI/war/cui/components/twitter-bootstrap-3.0.0/assets/js/less.js:3347
  - **Message:** This does not escape backslash characters in the input.

- **Alert #827** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/827
  - **Location:** WebUI/war/cui/components/twitter-bootstrap-3.0.0/js/tests/vendor/qunit.js:1166
  - **Message:** This does not escape backslash characters in the input.

- **Alert #826** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/826
  - **Location:** WebUI/war/cui/components/twitter-bootstrap-3.0.0/assets/js/less.js:72
  - **Message:** This replaces only the first occurrence of "\\\\".

- **Alert #825** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/825
  - **Location:** WebUI/war/app/includes/siteimprove_integration.html:189
  - **Message:** This does not escape backslash characters in the input.

- **Alert #824** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/824
  - **Location:** WebUI/war/app/includes/siteimprove_integration.html:169
  - **Message:** This replaces only the first occurrence of "\\\\".

- **Alert #823** — `js/bad-tag-filter` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/823
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/dojo/src/widget/html/loader.js:308
  - **Message:** This regular expression does not match script end tags like </script >.

- **Alert #822** — `js/bad-tag-filter` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/822
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/dojo/src/widget/ContentPane.js:529
  - **Message:** This regular expression does not match script end tags like </script >.

- **Alert #820** — `js/bad-tag-filter` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/820
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/dojo/dojo.js:11245
  - **Message:** This regular expression does not match script end tags like </script >.

- **Alert #819** — `js/bad-tag-filter` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/819
  - **Location:** deliverytiersuite/delivery-tier-suite/p13n-ds/src-js/p13n/perc_p13n_profile.js:325
  - **Message:** This regular expression does not match upper case <SCRIPT> tags.

- **Alert #818** — `js/bad-tag-filter` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/818
  - **Location:** deliverytiersuite/delivery-tier-suite/p13n-ds/lib-js/jquery-treeview/lib/jquery.js:2568
  - **Message:** This regular expression does not match upper case <SCRIPT> tags.

- **Alert #817** — `js/bad-tag-filter` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/817
  - **Location:** WebUI/war/cui/components/knockoutjs/src/utils.domManipulation.js:2
  - **Message:** This regular expression does not match comments containing newlines.

- **Alert #816** — `js/bad-tag-filter` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/816
  - **Location:** WebUI/war/cui/components/knockoutjs/dist/knockout.debug.js:771
  - **Message:** This regular expression does not match comments containing newlines.

- **Alert #815** — `js/unsafe-html-expansion` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/815
  - **Location:** WebUI/war/cui/components/jquery-migrate/jquery-migrate.js:823
  - **Message:** This self-closing HTML tag expansion invalidates prior sanitization as this regular expression may match part of an attribute value.

- **Alert #814** — `js/unsafe-html-expansion` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/814
  - **Location:** WebUI/war/cui/components/jquery-migrate/jquery-migrate.js:814
  - **Message:** This self-closing HTML tag expansion invalidates prior sanitization as this regular expression may match part of an attribute value.

- **Alert #813** — `js/useless-regexp-character-escape` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/813
  - **Location:** WebUI/war/cui/components/knockoutjs/dist/knockout.debug.js:2001
  - **Message:** The escape sequence '\\w' is equivalent to just 'w', so the sequence is not a character class when it is used in a regular expression.\nThe escape sequence '\\w' is equivalent to just 'w', so the sequence is not a character class when it is used in a regular expression.\nThe escape sequence '\\w' is equivalent to just 'w', so the sequence is not a character class when it is used in a regular expression.

- **Alert #812** — `js/overly-large-range` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/812
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/dojo/src/widget/html/loader.js:248
  - **Message:** Suspicious character range that overlaps with \\w in the same character class, and is equivalent to \\["#$%&'()*+,\\-.\\/0-9:\\].

- **Alert #811** — `js/overly-large-range` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/811
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/dojo/src/widget/ContentPane.js:474
  - **Message:** Suspicious character range that overlaps with \\w in the same character class, and is equivalent to \\["#$%&'()*+,\\-.\\/0-9:\\].

- **Alert #809** — `js/overly-large-range` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/809
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/dojo/dojo.js:11198
  - **Message:** Suspicious character range that overlaps with \\w in the same character class, and is equivalent to \\["#$%&'()*+,\\-.\\/0-9:\\].

- **Alert #803** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/803
  - **Location:** WebUI/war/cui/components/twitter-bootstrap-3.0.0/getting-started.html:120
  - **Message:** Script loaded from content delivery network with no integrity check.

- **Alert #802** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/802
  - **Location:** WebUI/war/cui/components/twitter-bootstrap-3.0.0/_includes/social-buttons.html:4
  - **Message:** Iframe loaded using unencrypted connection.

- **Alert #801** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/801
  - **Location:** WebUI/war/cui/components/twitter-bootstrap-3.0.0/_includes/social-buttons.html:7
  - **Message:** Iframe loaded using unencrypted connection.

- **Alert #800** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-02-27T00:59:45Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/800
  - **Location:** WebUI/war/cui/components/twitter-bootstrap-3.0.0/_includes/footer.html:7
  - **Message:** Script loaded using unencrypted connection.

- **Alert #797** — `java/ssrf` (critical, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-19T00:28:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/797
  - **Location:** deliverytiersuite/delivery-tier-suite/feeds/src/main/java/com/percussion/delivery/feeds/services/PSFeedService.java:550
  - **Message:** Potential server-side request forgery due to a user-provided value.

- **Alert #796** — `java/implicit-cast-in-compound-assignment` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-18T21:09:28Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/796
  - **Location:** deliverytiersuite/delivery-tier-suite/feeds/src/test/java/com/percussion/delivery/feeds/PSFeedServicePerformanceTest.java:582
  - **Message:** Implicit cast of source type long to narrower destination type int.

- **Alert #792** — `java/unvalidated-url-forward` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/792
  - **Location:** modules/servletutils/src/main/java/com/percussion/servlet_utils/servlet/PSServletUtils.java:247
  - **Message:** Untrusted URL forward depends on a user-provided value.

- **Alert #791** — `java/insecure-trustmanager` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/791
  - **Location:** projects/sitemanage/src/main/java/com/percussion/sitemanage/importer/PSSiteImporter.java:327
  - **Message:** This uses TrustManager, which is defined in PSSiteImporter$ and trusts any certificate.

- **Alert #790** — `java/stack-trace-exposure` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/790
  - **Location:** modules/TableFactory/src/main/java/com/percussion/tablefactory/PSJdbcTableFactoryException.java:138
  - **Message:** Error information can be exposed to an external user.

- **Alert #789** — `java/stack-trace-exposure` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/789
  - **Location:** deployer/src/test/java/com/percussion/webdav/test/util/PSServletRequesterTest.java:279
  - **Message:** Error information can be exposed to an external user.

- **Alert #788** — `java/error-message-exposure` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/788
  - **Location:** system/src/main/java/com/percussion/servlets/taglib/PSPageSidenavTag.java:42
  - **Message:** Error information can be exposed to an external user.\nError information can be exposed to an external user.

- **Alert #787** — `java/error-message-exposure` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/787
  - **Location:** system/servlet/src/com/percussion/webdav/method/PSWebdavConfigValidator.java:618
  - **Message:** Error information can be exposed to an external user.\nError information can be exposed to an external user.

- **Alert #786** — `java/error-message-exposure` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/786
  - **Location:** system/services/src/com/percussion/services/aaclient/PSAaClientServlet.java:144
  - **Message:** Error information can be exposed to an external user.\nError information can be exposed to an external user.

- **Alert #784** — `java/error-message-exposure` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/784
  - **Location:** projects/sitemanage/src/main/java/com/percussion/integrations/siteimprove/rest/PSSiteimprove.java:200
  - **Message:** Error information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.

- **Alert #783** — `java/error-message-exposure` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/783
  - **Location:** projects/sitemanage/src/main/java/com/percussion/integrations/ems/rest/PSEmsRestService.java:150
  - **Message:** Error information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.

- **Alert #782** — `java/error-message-exposure` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/782
  - **Location:** projects/sitemanage/src/main/java/com/percussion/integrations/ems/rest/PSEmsRestService.java:143
  - **Message:** Error information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.

- **Alert #781** — `java/error-message-exposure` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/781
  - **Location:** projects/sitemanage/src/main/java/com/percussion/integrations/ems/rest/PSEmsRestService.java:136
  - **Message:** Error information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.

- **Alert #780** — `java/error-message-exposure` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/780
  - **Location:** projects/sitemanage/src/main/java/com/percussion/integrations/ems/rest/PSEmsRestService.java:129
  - **Message:** Error information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.

- **Alert #779** — `java/error-message-exposure` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/779
  - **Location:** projects/sitemanage/src/main/java/com/percussion/integrations/ems/rest/PSEmsRestService.java:114
  - **Message:** Error information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.

- **Alert #778** — `java/error-message-exposure` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/778
  - **Location:** projects/sitemanage/src/main/java/com/percussion/integrations/ems/rest/PSEmsRestService.java:107
  - **Message:** Error information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.

- **Alert #777** — `java/error-message-exposure` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/777
  - **Location:** projects/sitemanage/src/main/java/com/percussion/integrations/ems/rest/PSEmsRestService.java:100
  - **Message:** Error information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.

- **Alert #776** — `java/error-message-exposure` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/776
  - **Location:** projects/sitemanage/src/main/java/com/percussion/foldermanagement/service/impl/PSFolderRestService.java:199
  - **Message:** Error information can be exposed to an external user.\nError information can be exposed to an external user.

- **Alert #775** — `java/error-message-exposure` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/775
  - **Location:** projects/sitemanage/src/main/java/com/percussion/foldermanagement/service/impl/PSFolderRestService.java:191
  - **Message:** Error information can be exposed to an external user.\nError information can be exposed to an external user.

- **Alert #774** — `java/error-message-exposure` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/774
  - **Location:** projects/sitemanage/src/main/java/com/percussion/foldermanagement/service/impl/PSFolderRestService.java:187
  - **Message:** Error information can be exposed to an external user.\nError information can be exposed to an external user.

- **Alert #773** — `java/error-message-exposure` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/773
  - **Location:** projects/sitemanage/src/main/java/com/percussion/foldermanagement/service/impl/PSFolderRestService.java:160
  - **Message:** Error information can be exposed to an external user.\nError information can be exposed to an external user.

- **Alert #772** — `java/error-message-exposure` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/772
  - **Location:** projects/sitemanage/src/main/java/com/percussion/foldermanagement/service/impl/PSFolderRestService.java:148
  - **Message:** Error information can be exposed to an external user.\nError information can be exposed to an external user.

- **Alert #771** — `java/error-message-exposure` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/771
  - **Location:** projects/sitemanage/src/main/java/com/percussion/designmanagement/service/impl/PSWebResourcesRestService.java:189
  - **Message:** Error information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.

- **Alert #770** — `java/error-message-exposure` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/770
  - **Location:** modules/perc-toolkit/src/main/java/com/percussion/pso/preview/SimpleXmlView.java:59
  - **Message:** Error information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.

- **Alert #769** — `java/error-message-exposure` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/769
  - **Location:** modules/perc-toolkit/src/main/java/com/percussion/pso/imageedit/web/SimpleXmlView.java:58
  - **Message:** Error information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.\nError information can be exposed to an external user.

- **Alert #768** — `java/error-message-exposure` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/768
  - **Location:** modules/ContentUI/src/main/java/com/percussion/content/ui/aa/PSAAClientServlet.java:89
  - **Message:** Error information can be exposed to an external user.\nError information can be exposed to an external user.

- **Alert #766** — `java/regex-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/766
  - **Location:** projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSiteDataService.java:1871
  - **Message:** This regular expression is constructed from a user-provided value.

- **Alert #765** — `java/regex-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/765
  - **Location:** modules/utils/src/main/java/com/percussion/utils/string/PSFolderStringUtils.java:73
  - **Message:** This regular expression is constructed from a user-provided value.\nThis regular expression is constructed from a user-provided value.\nThis regular expression is constructed from a user-provided value.\nThis regular expression is constructed from a user-provided value.\nThis regular expression is constructed from a user-provided value.\nThis regular expression is constructed from a user-provided value.

- **Alert #763** — `java/redos` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/763
  - **Location:** modules/extensions-main/src/main/java/com/percussion/extensions/translations/PSFormEncodeDecodeHelper.java:171
  - **Message:** This part of the regular expression may cause exponential backtracking on strings containing many repetitions of '\\n'.

- **Alert #762** — `java/polynomial-redos` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/762
  - **Location:** deliverytiersuite/delivery-tier-suite/metadata/src/main/java/com/percussion/delivery/metadata/data/impl/PSCriteriaElement.java:118
  - **Message:** This regular expression that depends on a user-provided value may run slow on strings with many repetitions of '0'.\nThis regular expression that depends on a user-provided value may run slow on strings with many repetitions of '0'.\nThis regular expression that depends on a user-provided value may run slow on strings with many repetitions of '0'.\nThis regular expression that depends on a user-provided value may run slow on strings with many repetitions of '0'.\nThis regular expression that depends on a user-provided value may run slow on strings with many repetitions of '0'.\nThis regular expression that depends on a user-provided value may run slow on strings with many repetitions of '0'.

- **Alert #761** — `java/polynomial-redos` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/761
  - **Location:** deliverytiersuite/delivery-tier-suite/metadata/src/main/java/com/percussion/delivery/metadata/data/impl/PSCriteriaElement.java:111
  - **Message:** This regular expression that depends on a user-provided value may run slow on strings with many repetitions of '0'.\nThis regular expression that depends on a user-provided value may run slow on strings with many repetitions of '0'.\nThis regular expression that depends on a user-provided value may run slow on strings with many repetitions of '0'.\nThis regular expression that depends on a user-provided value may run slow on strings with many repetitions of '0'.\nThis regular expression that depends on a user-provided value may run slow on strings with many repetitions of '0'.\nThis regular expression that depends on a user-provided value may run slow on strings with many repetitions of '0'.

- **Alert #759** — `java/weak-cryptographic-algorithm` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/759
  - **Location:** modules/perc-legacy/src/main/java/com/percussion/legacy/security/deprecated/PSAesCBC.java:144
  - **Message:** Cryptographic algorithm AES/CBC/PKCS5Padding is insecure. CBC mode with PKCS#5 or PKCS#7 padding is vulnerable to padding oracle attacks. Consider using GCM instead.

- **Alert #758** — `java/weak-cryptographic-algorithm` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/758
  - **Location:** modules/perc-legacy/src/main/java/com/percussion/legacy/security/deprecated/PSAesCBC.java:111
  - **Message:** Cryptographic algorithm AES/CBC/PKCS5Padding is insecure. CBC mode with PKCS#5 or PKCS#7 padding is vulnerable to padding oracle attacks. Consider using GCM instead.

- **Alert #757** — `java/weak-cryptographic-algorithm` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/757
  - **Location:** modules/perc-legacy/src/main/java/com/percussion/legacy/security/deprecated/PSAesCBC.java:83
  - **Message:** Cryptographic algorithm AES/CBC/PKCS5Padding is insecure. CBC mode with PKCS#5 or PKCS#7 padding is vulnerable to padding oracle attacks. Consider using GCM instead.

- **Alert #756** — `java/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/756
  - **Location:** system/services/src/com/percussion/services/aaclient/PSAaClientServlet.java:144
  - **Message:** Cross-site scripting vulnerability due to a user-provided value.

- **Alert #755** — `java/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/755
  - **Location:** projects/sitemanage/src/main/java/com/percussion/user/service/impl/PSUserService.java:806
  - **Message:** Cross-site scripting vulnerability due to a user-provided value.

- **Alert #754** — `java/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/754
  - **Location:** projects/sitemanage/src/main/java/com/percussion/user/service/impl/PSUserService.java:743
  - **Message:** Cross-site scripting vulnerability due to a user-provided value.

- **Alert #753** — `java/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/753
  - **Location:** projects/sitemanage/src/main/java/com/percussion/user/service/impl/PSUserService.java:493
  - **Message:** Cross-site scripting vulnerability due to a user-provided value.

- **Alert #752** — `java/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/752
  - **Location:** projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSiteDataRestService.java:209
  - **Message:** Cross-site scripting vulnerability due to a user-provided value.

- **Alert #751** — `java/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/751
  - **Location:** projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSiteDataRestService.java:186
  - **Message:** Cross-site scripting vulnerability due to a user-provided value.

- **Alert #750** — `java/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/750
  - **Location:** projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSiteDataRestService.java:110
  - **Message:** Cross-site scripting vulnerability due to a user-provided value.

- **Alert #749** — `java/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/749
  - **Location:** projects/sitemanage/src/main/java/com/percussion/role/service/impl/PSRoleService.java:118
  - **Message:** Cross-site scripting vulnerability due to a user-provided value.\nCross-site scripting vulnerability due to a user-provided value.

- **Alert #748** — `java/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/748
  - **Location:** projects/sitemanage/src/main/java/com/percussion/pagemanagement/service/impl/PSPageRestService.java:344
  - **Message:** Cross-site scripting vulnerability due to a user-provided value.

- **Alert #747** — `java/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/747
  - **Location:** projects/sitemanage/src/main/java/com/percussion/integrations/siteimprove/rest/PSSiteimprove.java:200
  - **Message:** Cross-site scripting vulnerability due to a user-provided value.

- **Alert #746** — `java/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/746
  - **Location:** projects/sitemanage/src/main/java/com/percussion/foldermanagement/service/impl/PSFolderRestService.java:199
  - **Message:** Cross-site scripting vulnerability due to a user-provided value.

- **Alert #745** — `java/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/745
  - **Location:** projects/sitemanage/src/main/java/com/percussion/foldermanagement/service/impl/PSFolderRestService.java:195
  - **Message:** Cross-site scripting vulnerability due to a user-provided value.

- **Alert #744** — `java/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/744
  - **Location:** projects/sitemanage/src/main/java/com/percussion/foldermanagement/service/impl/PSFolderRestService.java:187
  - **Message:** Cross-site scripting vulnerability due to a user-provided value.

- **Alert #743** — `java/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/743
  - **Location:** projects/sitemanage/src/main/java/com/percussion/foldermanagement/service/impl/PSFolderRestService.java:160
  - **Message:** Cross-site scripting vulnerability due to a user-provided value.\nCross-site scripting vulnerability due to a user-provided value.

- **Alert #742** — `java/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/742
  - **Location:** projects/sitemanage/src/main/java/com/percussion/foldermanagement/service/impl/PSFolderRestService.java:156
  - **Message:** Cross-site scripting vulnerability due to a user-provided value.\nCross-site scripting vulnerability due to a user-provided value.

- **Alert #741** — `java/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/741
  - **Location:** projects/sitemanage/src/main/java/com/percussion/foldermanagement/service/impl/PSFolderRestService.java:152
  - **Message:** Cross-site scripting vulnerability due to a user-provided value.\nCross-site scripting vulnerability due to a user-provided value.

- **Alert #740** — `java/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/740
  - **Location:** projects/sitemanage/src/main/java/com/percussion/foldermanagement/service/impl/PSFolderRestService.java:148
  - **Message:** Cross-site scripting vulnerability due to a user-provided value.\nCross-site scripting vulnerability due to a user-provided value.

- **Alert #739** — `java/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/739
  - **Location:** projects/sitemanage/src/main/java/com/percussion/dashboardmanagement/service/impl/PSUserProfileRestService.java:49
  - **Message:** Cross-site scripting vulnerability due to a user-provided value.

- **Alert #738** — `java/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/738
  - **Location:** projects/sitemanage/src/main/java/com/percussion/dashboardmanagement/service/impl/PSDashboardService.java:80
  - **Message:** Cross-site scripting vulnerability due to a user-provided value.

- **Alert #737** — `java/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/737
  - **Location:** projects/sitemanage/src/main/java/com/percussion/assetmanagement/service/impl/PSAssetRestService.java:531
  - **Message:** Cross-site scripting vulnerability due to a user-provided value.

- **Alert #736** — `java/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/736
  - **Location:** projects/sitemanage/src/main/java/com/percussion/assetmanagement/service/impl/PSAssetRestService.java:485
  - **Message:** Cross-site scripting vulnerability due to a user-provided value.

- **Alert #735** — `java/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/735
  - **Location:** projects/sitemanage/src/main/java/com/percussion/assetmanagement/service/impl/PSAssetRestService.java:234
  - **Message:** Cross-site scripting vulnerability due to a user-provided value.

- **Alert #734** — `java/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/734
  - **Location:** modules/perc-toolkit/src/main/java/com/percussion/pso/restservice/impl/ItemRestServiceImpl.java:2041
  - **Message:** Cross-site scripting vulnerability due to a user-provided value.

- **Alert #732** — `java/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/732
  - **Location:** modules/perc-toolkit/src/main/java/com/percussion/pso/restservice/impl/ItemRestServiceImpl.java:1864
  - **Message:** Cross-site scripting vulnerability due to a user-provided value.\nCross-site scripting vulnerability due to a user-provided value.

- **Alert #731** — `java/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/731
  - **Location:** modules/perc-toolkit/src/main/java/com/percussion/pso/restservice/impl/ItemRestServiceImpl.java:1862
  - **Message:** Cross-site scripting vulnerability due to a user-provided value.

- **Alert #730** — `java/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/730
  - **Location:** modules/perc-toolkit/src/main/java/com/percussion/pso/restservice/impl/ItemRestServiceImpl.java:798
  - **Message:** Cross-site scripting vulnerability due to a user-provided value.

- **Alert #729** — `java/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/729
  - **Location:** modules/perc-toolkit/src/main/java/com/percussion/pso/restservice/impl/ItemRestServiceImpl.java:777
  - **Message:** Cross-site scripting vulnerability due to a user-provided value.\nCross-site scripting vulnerability due to a user-provided value.\nCross-site scripting vulnerability due to a user-provided value.

- **Alert #728** — `java/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/728
  - **Location:** deliverytiersuite/delivery-tier-suite/metadata/src/main/java/com/percussion/delivery/metadata/impl/PSMetadataRestService.java:479
  - **Message:** Cross-site scripting vulnerability due to a user-provided value.

- **Alert #727** — `java/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/727
  - **Location:** deliverytiersuite/delivery-tier-suite/feeds/src/main/java/com/percussion/delivery/feeds/services/PSFeedService.java:419
  - **Message:** Cross-site scripting vulnerability due to a user-provided value.

- **Alert #726** — `java/ssrf` (critical, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/726
  - **Location:** system/src/main/java/com/percussion/xml/PSDtdTree.java:218
  - **Message:** Potential server-side request forgery due to a user-provided value.

- **Alert #723** — `java/zipslip` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/723
  - **Location:** system/src/main/java/com/percussion/system/utils/PSArchiveFiles.java:343
  - **Message:** Unsanitized archive entry, which may contain '..', is used in a file system operation.

- **Alert #722** — `java/zipslip` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/722
  - **Location:** projects/sitemanage/src/main/java/com/percussion/widgetbuilder/utils/PSWidgetPackageBuilder.java:122
  - **Message:** Unsanitized archive entry, which may contain '..', is used in a file system operation.\nUnsanitized archive entry, which may contain '..', is used in a file system operation.

- **Alert #720** — `java/zipslip` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/720
  - **Location:** modules/perc-ant/src/main/java/com/percussion/ant/install/PSExtractJarFiles.java:73
  - **Message:** Unsanitized archive entry, which may contain '..', is used in a file system operation.\nUnsanitized archive entry, which may contain '..', is used in a file system operation.\nUnsanitized archive entry, which may contain '..', is used in a file system operation.\nUnsanitized archive entry, which may contain '..', is used in a file system operation.

- **Alert #718** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/718
  - **Location:** system/src/main/java/com/percussion/xml/PSDtdTree.java:207
  - **Message:** This path depends on a user-provided value.

- **Alert #717** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/717
  - **Location:** system/src/main/java/com/percussion/server/PSServer.java:351
  - **Message:** This path depends on a user-provided value.

- **Alert #716** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/716
  - **Location:** system/src/main/java/com/percussion/process/PSLocalCommandHandler.java:458
  - **Message:** This path depends on a user-provided value.

- **Alert #715** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/715
  - **Location:** system/src/main/java/com/percussion/process/PSLocalCommandHandler.java:427
  - **Message:** This path depends on a user-provided value.

- **Alert #714** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/714
  - **Location:** system/src/main/java/com/percussion/process/PSLocalCommandHandler.java:377
  - **Message:** This path depends on a user-provided value.

- **Alert #713** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/713
  - **Location:** system/src/main/java/com/percussion/process/PSLocalCommandHandler.java:377
  - **Message:** This path depends on a user-provided value.

- **Alert #712** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/712
  - **Location:** system/src/main/java/com/percussion/process/PSLocalCommandHandler.java:361
  - **Message:** This path depends on a user-provided value.

- **Alert #711** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/711
  - **Location:** system/src/main/java/com/percussion/process/PSLocalCommandHandler.java:355
  - **Message:** This path depends on a user-provided value.

- **Alert #710** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/710
  - **Location:** system/src/main/java/com/percussion/process/PSLocalCommandHandler.java:355
  - **Message:** This path depends on a user-provided value.

- **Alert #709** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/709
  - **Location:** system/src/main/java/com/percussion/process/PSLocalCommandHandler.java:353
  - **Message:** This path depends on a user-provided value.

- **Alert #708** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/708
  - **Location:** system/src/main/java/com/percussion/process/PSLocalCommandHandler.java:334
  - **Message:** This path depends on a user-provided value.

- **Alert #707** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/707
  - **Location:** system/src/main/java/com/percussion/process/PSProcessDaemon.java:803
  - **Message:** This path depends on a user-provided value.

- **Alert #706** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/706
  - **Location:** projects/sitemanage/src/main/java/com/percussion/theme/service/impl/PSThemeService.java:489
  - **Message:** This path depends on a user-provided value.

- **Alert #705** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/705
  - **Location:** projects/sitemanage/src/main/java/com/percussion/theme/service/impl/PSThemeService.java:488
  - **Message:** This path depends on a user-provided value.

- **Alert #704** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/704
  - **Location:** projects/sitemanage/src/main/java/com/percussion/theme/service/impl/PSRegionCSSFileService.java:350
  - **Message:** This path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.

- **Alert #703** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/703
  - **Location:** projects/sitemanage/src/main/java/com/percussion/theme/service/impl/PSRegionCSSFileService.java:348
  - **Message:** This path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.

- **Alert #702** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/702
  - **Location:** projects/sitemanage/src/main/java/com/percussion/theme/service/impl/PSRegionCSSFileService.java:335
  - **Message:** This path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.

- **Alert #701** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/701
  - **Location:** projects/sitemanage/src/main/java/com/percussion/theme/service/impl/PSRegionCSSFileService.java:325
  - **Message:** This path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.

- **Alert #700** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/700
  - **Location:** projects/sitemanage/src/main/java/com/percussion/theme/service/impl/PSRegionCSSFileService.java:315
  - **Message:** This path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.

- **Alert #699** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/699
  - **Location:** projects/sitemanage/src/main/java/com/percussion/theme/service/impl/PSRegionCSSFileService.java:314
  - **Message:** This path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.

- **Alert #698** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/698
  - **Location:** projects/sitemanage/src/main/java/com/percussion/theme/service/impl/PSThemeService.java:403
  - **Message:** This path depends on a user-provided value.

- **Alert #697** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/697
  - **Location:** projects/sitemanage/src/main/java/com/percussion/theme/service/impl/PSRegionCSSFileService.java:287
  - **Message:** This path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.

- **Alert #696** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/696
  - **Location:** projects/sitemanage/src/main/java/com/percussion/theme/service/impl/PSRegionCSSFileService.java:285
  - **Message:** This path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.

- **Alert #695** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/695
  - **Location:** projects/sitemanage/src/main/java/com/percussion/theme/service/impl/PSThemeService.java:376
  - **Message:** This path depends on a user-provided value.

- **Alert #694** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/694
  - **Location:** projects/sitemanage/src/main/java/com/percussion/theme/service/impl/PSThemeService.java:353
  - **Message:** This path depends on a user-provided value.

- **Alert #693** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/693
  - **Location:** projects/sitemanage/src/main/java/com/percussion/theme/service/impl/PSThemeService.java:204
  - **Message:** This path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.

- **Alert #692** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/692
  - **Location:** projects/sitemanage/src/main/java/com/percussion/theme/service/impl/PSThemeService.java:193
  - **Message:** This path depends on a user-provided value.

- **Alert #691** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/691
  - **Location:** projects/sitemanage/src/main/java/com/percussion/theme/service/impl/PSThemeService.java:160
  - **Message:** This path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.

- **Alert #690** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/690
  - **Location:** projects/sitemanage/src/main/java/com/percussion/theme/service/impl/PSThemeService.java:119
  - **Message:** This path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.

- **Alert #689** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/689
  - **Location:** projects/sitemanage/src/main/java/com/percussion/pathmanagement/service/impl/PSFileSystemPathItemService.java:219
  - **Message:** This path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.

- **Alert #688** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/688
  - **Location:** projects/sitemanage/src/main/java/com/percussion/pathmanagement/service/impl/PSFileSystemPathItemService.java:195
  - **Message:** This path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.

- **Alert #687** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/687
  - **Location:** projects/sitemanage/src/main/java/com/percussion/pathmanagement/service/impl/PSFileSystemPathItemService.java:191
  - **Message:** This path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.

- **Alert #686** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/686
  - **Location:** projects/sitemanage/src/main/java/com/percussion/pathmanagement/service/impl/PSFileSystemPathItemService.java:184
  - **Message:** This path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.

- **Alert #685** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/685
  - **Location:** projects/sitemanage/src/main/java/com/percussion/pathmanagement/service/impl/PSFileSystemPathItemService.java:129
  - **Message:** This path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.

- **Alert #684** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/684
  - **Location:** projects/sitemanage/src/main/java/com/percussion/pagemanagement/service/impl/PSRenderLinkService.java:686
  - **Message:** This path depends on a user-provided value.

- **Alert #683** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/683
  - **Location:** projects/sitemanage/src/main/java/com/percussion/designmanagement/service/impl/PSFileSystemService.java:300
  - **Message:** This path depends on a user-provided value.

- **Alert #682** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/682
  - **Location:** projects/sitemanage/src/main/java/com/percussion/designmanagement/service/impl/PSFileSystemService.java:298
  - **Message:** This path depends on a user-provided value.

- **Alert #681** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/681
  - **Location:** projects/sitemanage/src/main/java/com/percussion/designmanagement/service/impl/PSFileSystemService.java:284
  - **Message:** This path depends on a user-provided value.\nThis path depends on a user-provided value.

- **Alert #680** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/680
  - **Location:** projects/sitemanage/src/main/java/com/percussion/designmanagement/service/impl/PSFileSystemService.java:248
  - **Message:** This path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.

- **Alert #679** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/679
  - **Location:** projects/sitemanage/src/main/java/com/percussion/designmanagement/service/impl/PSFileSystemService.java:248
  - **Message:** This path depends on a user-provided value.\nThis path depends on a user-provided value.

- **Alert #678** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/678
  - **Location:** projects/sitemanage/src/main/java/com/percussion/designmanagement/service/impl/PSFileSystemService.java:143
  - **Message:** This path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.

- **Alert #677** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/677
  - **Location:** projects/sitemanage/src/main/java/com/percussion/designmanagement/service/impl/PSWebResourcesRestService.java:97
  - **Message:** This path depends on a user-provided value.

- **Alert #676** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/676
  - **Location:** projects/sitemanage/src/main/java/com/percussion/designmanagement/service/impl/PSWebResourcesRestService.java:97
  - **Message:** This path depends on a user-provided value.

- **Alert #675** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/675
  - **Location:** projects/sitemanage/src/main/java/com/percussion/cloudservice/impl/PSCloudService.java:237
  - **Message:** This path depends on a user-provided value.\nThis path depends on a user-provided value.\nThis path depends on a user-provided value.

- **Alert #674** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/674
  - **Location:** projects/sitemanage/src/main/java/com/percussion/assetmanagement/service/impl/PSAssetService.java:1890
  - **Message:** This path depends on a user-provided value.

- **Alert #673** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/673
  - **Location:** projects/sitemanage/src/main/java/com/percussion/assetmanagement/service/impl/PSAssetService.java:1850
  - **Message:** This path depends on a user-provided value.\nThis path depends on a user-provided value.

- **Alert #672** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/672
  - **Location:** projects/sitemanage/src/main/java/com/percussion/apibridge/AssetAdaptor.java:896
  - **Message:** This path depends on a user-provided value.

- **Alert #671** — `java/path-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/671
  - **Location:** projects/sitemanage/src/main/java/com/percussion/apibridge/AssetAdaptor.java:896
  - **Message:** This path depends on a user-provided value.

- **Alert #666** — `java/xxe` (critical, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/666
  - **Location:** modules/perc-toolkit/src/main/java/com/percussion/pso/restservice/jexl/PSOImportJexl.java:278
  - **Message:** XML parsing depends on a user-provided value without guarding against external entity expansion.

- **Alert #663** — `java/unsafe-hostname-verification` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/663
  - **Location:** projects/sitemanage/src/main/java/com/percussion/sitemanage/importer/PSSiteImporter.java:335
  - **Message:** The hostname verifier defined by this type always accepts any certificate, even if the hostname does not match.

- **Alert #661** — `java/sql-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/661
  - **Location:** modules/utils/src/main/java/com/percussion/util/PSSQLStatement.java:85
  - **Message:** This query depends on a user-provided value.

- **Alert #660** — `java/sql-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/660
  - **Location:** modules/TableFactory/src/main/java/com/percussion/tablefactory/PSJdbcTableMetaData.java:462
  - **Message:** This query depends on a user-provided value.

- **Alert #659** — `java/sql-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/659
  - **Location:** modules/TableFactory/src/main/java/com/percussion/tablefactory/PSJdbcTableMetaData.java:366
  - **Message:** This query depends on a user-provided value.

- **Alert #658** — `java/sql-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/658
  - **Location:** modules/TableFactory/src/main/java/com/percussion/tablefactory/PSJdbcTableFactory.java:1227
  - **Message:** This query depends on a user-provided value.

- **Alert #657** — `java/sql-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/657
  - **Location:** modules/TableFactory/src/main/java/com/percussion/tablefactory/PSJdbcResultSetIteratorStep.java:100
  - **Message:** This query depends on a user-provided value.

- **Alert #656** — `java/sql-injection` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/656
  - **Location:** deliverytiersuite/delivery-tier-suite/metadata/src/main/java/com/percussion/delivery/metadata/rdbms/impl/PSMetadataQueryService.java:563
  - **Message:** This query depends on a user-provided value.\nThis query depends on a user-provided value.\nThis query depends on a user-provided value.\nThis query depends on a user-provided value.\nThis query depends on a user-provided value.

- **Alert #650** — `java/static-initialization-vector` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/650
  - **Location:** modules/perc-legacy/src/main/java/com/percussion/legacy/security/deprecated/PSAesCBC.java:116
  - **Message:** A static initialization vector should not be used for encryption.

- **Alert #649** — `java/static-initialization-vector` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/649
  - **Location:** modules/perc-legacy/src/main/java/com/percussion/legacy/security/deprecated/PSAesCBC.java:88
  - **Message:** A static initialization vector should not be used for encryption.

- **Alert #648** — `java/ldap-injection` (critical, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/648
  - **Location:** system/src/main/java/com/percussion/security/PSJndiGroupProvider.java:200
  - **Message:** This LDAP query depends on a user-provided value.

- **Alert #647** — `java/unvalidated-url-redirection` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/647
  - **Location:** system/src/main/java/com/percussion/servlets/PSSecurityFilter.java:1194
  - **Message:** Untrusted URL redirection depends on a user-provided value.

- **Alert #646** — `java/unvalidated-url-redirection` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/646
  - **Location:** system/src/main/java/com/percussion/servlets/PSSecurityFilter.java:1192
  - **Message:** Untrusted URL redirection depends on a user-provided value.

- **Alert #645** — `java/unvalidated-url-redirection` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/645
  - **Location:** system/src/main/java/com/percussion/servlets/PSSecurityFilter.java:512
  - **Message:** Untrusted URL redirection depends on a user-provided value.

- **Alert #644** — `java/unvalidated-url-redirection` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/644
  - **Location:** deliverytiersuite/delivery-tier-suite/comments/src/main/java/com/percussion/delivery/comments/services/PSCommentsRestService.java:378
  - **Message:** Untrusted URL redirection depends on a user-provided value.

- **Alert #643** — `java/unvalidated-url-redirection` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/643
  - **Location:** deliverytiersuite/delivery-tier-suite/comments/src/main/java/com/percussion/delivery/comments/services/PSCommentsRestService.java:345
  - **Message:** Untrusted URL redirection depends on a user-provided value.

- **Alert #639** — `java/implicit-cast-in-compound-assignment` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/639
  - **Location:** system/src/main/java/com/percussion/HTTPClient/RespInputStream.java:140
  - **Message:** Implicit cast of source type long to narrower destination type int.

- **Alert #638** — `java/implicit-cast-in-compound-assignment` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-08-16T19:41:35Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/638
  - **Location:** system/src/main/java/com/percussion/HTTPClient/BufferedInputStream.java:115
  - **Message:** Implicit cast of source type long to narrower destination type int.

- **Alert #628** — `java/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:21:16Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/628
  - **Location:** system/servlet/src/com/percussion/hooks/servlet/RhythmyxServlet.java:535
  - **Message:** Cross-site scripting vulnerability due to a user-provided value.\nCross-site scripting vulnerability due to a user-provided value.

- **Alert #627** — `java/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:21:16Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/627
  - **Location:** system/servlet/src/com/percussion/hooks/servlet/RhythmyxServlet.java:513
  - **Message:** Cross-site scripting vulnerability due to a user-provided value.\nCross-site scripting vulnerability due to a user-provided value.

- **Alert #625** — `java/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:21:16Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/625
  - **Location:** system/release/tomcat/Tomcat/webapps/tomcat-docs/appdev/sample/src/mypackage/Hello.java:78
  - **Message:** Cross-site scripting vulnerability due to a user-provided value.

- **Alert #624** — `java/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:21:16Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/624
  - **Location:** system/release/tomcat/Tomcat/webapps/tomcat-docs/appdev/sample/src/mypackage/Hello.java:77
  - **Message:** Cross-site scripting vulnerability due to a user-provided value.

- **Alert #595** — `java/xss` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:21:16Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/595
  - **Location:** deliverytiersuite/delivery-tier-suite/p13n-ds/src/main/java/com/percussion/soln/p13n/delivery/ds/web/DeliveryController.java:157
  - **Message:** Cross-site scripting vulnerability due to a user-provided value.

- **Alert #457** — `java/insecure-cookie` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:21:16Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/457
  - **Location:** modules/p13n-api/src/main/java/com/percussion/soln/p13n/tracking/web/CookieGenerator.java:189
  - **Message:** Cookie is added to response without the 'secure' flag being set.

- **Alert #434** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/434
  - **Location:** WebUI/war/widgets/PercSimpleMenu.js:94
  - **Message:** Potential XSS vulnerability in the '$.fn.percSimpleMenu' plugin.

- **Alert #433** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/433
  - **Location:** WebUI/war/plugins/perc_utils.js:1080
  - **Message:** Potential XSS vulnerability in the '$.fn.perc_toggle' plugin.

- **Alert #432** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/432
  - **Location:** WebUI/war/plugins/perc_utils.js:1079
  - **Message:** Potential XSS vulnerability in the '$.fn.perc_toggle' plugin.

- **Alert #431** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/431
  - **Location:** WebUI/war/plugins/perc_utils.js:1076
  - **Message:** Potential XSS vulnerability in the '$.fn.perc_toggle' plugin.

- **Alert #430** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/430
  - **Location:** WebUI/war/plugins/perc_utils.js:1075
  - **Message:** Potential XSS vulnerability in the '$.fn.perc_toggle' plugin.

- **Alert #429** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/429
  - **Location:** WebUI/war/plugins/perc_utils.js:1074
  - **Message:** Potential XSS vulnerability in the '$.fn.perc_toggle' plugin.

- **Alert #428** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/428
  - **Location:** WebUI/war/plugins/perc_utils.js:1074
  - **Message:** Potential XSS vulnerability in the '$.fn.perc_toggle' plugin.

- **Alert #427** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/427
  - **Location:** WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-perc-retiredjs/tools.scrollable-1.1.2.js:79
  - **Message:** Potential XSS vulnerability in the '$.fn.scrollable' plugin.

- **Alert #426** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/426
  - **Location:** WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-perc-retiredjs/tools.scrollable-1.1.2.js:75
  - **Message:** Potential XSS vulnerability in the '$.fn.scrollable' plugin.

- **Alert #425** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/425
  - **Location:** WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-perc-retiredjs/jquery.blockUI.js:437
  - **Message:** Potential XSS vulnerability in the '$.fn.block' plugin.

- **Alert #424** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/424
  - **Location:** WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-jeditable/jquery.jeditable.js:585
  - **Message:** Potential XSS vulnerability in the '$.fn.editable' plugin.

- **Alert #423** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/423
  - **Location:** WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-jeditable/jquery.jeditable.js:566
  - **Message:** Potential XSS vulnerability in the '$.fn.editable' plugin.

- **Alert #422** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/422
  - **Location:** WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-form/jquery.form.js:473
  - **Message:** Potential XSS vulnerability in the '$.fn.ajaxSubmit' plugin.

- **Alert #421** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/421
  - **Location:** WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-fancytree/modules/jquery.fancytree.ui-deps.js:912
  - **Message:** Potential XSS vulnerability in the '$.fn.position' plugin.

- **Alert #420** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/420
  - **Location:** WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-fancytree/jquery.fancytree-all-deps.js:907
  - **Message:** Potential XSS vulnerability in the '$.fn.position' plugin.

- **Alert #419** — `js/unsafe-jquery-plugin` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/419
  - **Location:** WebUI/war/jslib/profiles/3x/jquery/libraries/jquery-ui/jquery-ui.js:912
  - **Message:** Potential XSS vulnerability in the '$.fn.position' plugin.

- **Alert #372** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/372
  - **Location:** projects/sitemanage/src/test/java/com/percussion/share/dao/home_w.html:85
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #371** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/371
  - **Location:** projects/sitemanage/src/test/java/com/percussion/share/dao/home_w.html:61
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.\nDOM text is reinterpreted as HTML without escaping meta-characters.\nDOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #334** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/334
  - **Location:** WebUI/war/widgets/perc_page_edit_dialog.js:221
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #333** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/333
  - **Location:** WebUI/war/widgets/PercInlineEditDataTable.js:268
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #332** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/332
  - **Location:** WebUI/war/widgets/PercInlineEditDataTable.js:220
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #331** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/331
  - **Location:** WebUI/war/widgetbuilder/js/views/PercWidgetFieldsViews.js:140
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #330** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/330
  - **Location:** WebUI/war/widgetbuilder/js/views/PercWidgetBuilderDefinitionView.js:55
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #329** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/329
  - **Location:** WebUI/war/views/PercUserView.js:681
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #328** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/328
  - **Location:** WebUI/war/views/PercUserView.js:658
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #325** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/325
  - **Location:** WebUI/war/views/PercChangeTemplateDialog.js:199
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #324** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/324
  - **Location:** WebUI/war/views/PercCSSGalleryView.js:63
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #322** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/322
  - **Location:** WebUI/war/plugins/perc_utils.js:621
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #320** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/320
  - **Location:** WebUI/war/plugins/perc_template_layout_helper.js:243
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #319** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/319
  - **Location:** WebUI/war/plugins/PercRedirectHandler.js:266
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #318** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/318
  - **Location:** WebUI/war/plugins/PercListEditorWidget.js:301
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.\nDOM text is reinterpreted as HTML without escaping meta-characters.\nDOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #315** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/315
  - **Location:** WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-ui-multiselect-widget/jquery.multiselect.js:363
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #313** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/313
  - **Location:** WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-dropdown/jquery.dropdown.js:39
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #312** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/312
  - **Location:** WebUI/war/classes/perc_template_layout_class.js:95
  - **Message:** DOM text is reinterpreted as HTML without escaping meta-characters.

- **Alert #306** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/306
  - **Location:** WebUI/war/widgets/PercDataTable/PercDataTable.js:473
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #305** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/305
  - **Location:** WebUI/war/widgets/PercDataTable/PercDataTable.js:473
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #304** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/304
  - **Location:** WebUI/war/widgets/PercDataTable/PercDataTable.js:341
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #303** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/303
  - **Location:** WebUI/war/widgets/PercDataTable/PercDataTable.js:341
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #302** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/302
  - **Location:** WebUI/war/widgets/PercDataTable/PercDataTable.js:339
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #301** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/301
  - **Location:** WebUI/war/widgets/PercDataTable/PercDataTable.js:339
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #300** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/300
  - **Location:** WebUI/war/widgets/PercDataTableWrong/PercDataTable.js:124
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #299** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/299
  - **Location:** WebUI/war/widgets/PercDataTableWrong/PercDataTable.js:124
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #298** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/298
  - **Location:** WebUI/war/widgets/PercDataTableWrong/PercDataTable.js:124
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #297** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/297
  - **Location:** WebUI/war/widgets/PercDataTableWrong/PercDataTable.js:124
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #296** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/296
  - **Location:** WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-layout/jquery.layout_and_plugins.js:2666
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #295** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/295
  - **Location:** WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-layout/jquery.layout_and_plugins.js:2653
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #294** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/294
  - **Location:** WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-print-this/printThis.js:182
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #293** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/293
  - **Location:** WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-print-this/printThis.js:165
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #292** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/292
  - **Location:** WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-perc-retiredjs/jquery.print-this.js:54
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #291** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/291
  - **Location:** WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-perc-retiredjs/jquery.blockUI.js:324
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #290** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/290
  - **Location:** WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-perc-retiredjs/jquery.blockUI.js:321
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #289** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/289
  - **Location:** WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-perc-retiredjs/jquery.blockUI.js:318
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #288** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/288
  - **Location:** WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-perc-retiredjs/jquery.blockUI.js:310
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #287** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/287
  - **Location:** WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-perc-retiredjs/jquery.blockUI.js:295
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #286** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/286
  - **Location:** WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-form/jquery.form.js:672
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.\nThis HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #285** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/285
  - **Location:** WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-form/jquery.form.js:482
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #284** — `js/html-constructed-from-input` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/284
  - **Location:** WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-jeditable/jquery.jeditable.js:212
  - **Message:** This HTML construction which depends on library input might later allow cross-site scripting.\nThis HTML construction which depends on library input might later allow cross-site scripting.\nThis HTML construction which depends on library input might later allow cross-site scripting.\nThis HTML construction which depends on library input might later allow cross-site scripting.\nThis HTML construction which depends on library input might later allow cross-site scripting.\nThis HTML construction which depends on library input might later allow cross-site scripting.

- **Alert #276** — `js/prototype-pollution-utility` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/276
  - **Location:** WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-fancytree/modules/jquery.fancytree.js:318
  - **Message:** Properties are copied from options to target without guarding against prototype pollution.

- **Alert #275** — `js/prototype-pollution-utility` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/275
  - **Location:** WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-fancytree/jquery.fancytree-all.js:318
  - **Message:** Properties are copied from options to target without guarding against prototype pollution.

- **Alert #274** — `js/prototype-pollution-utility` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/274
  - **Location:** WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-fancytree/jquery.fancytree-all-deps.js:1779
  - **Message:** Properties are copied from options to target without guarding against prototype pollution.

- **Alert #273** — `js/prototype-pollution-utility` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/273
  - **Location:** WebUI/war/gadgets/repository/common/lib/jqplot/jquery.jqplot.js:8793
  - **Message:** Properties are copied from options to target without guarding against prototype pollution.

- **Alert #272** — `js/prototype-pollution-utility` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/272
  - **Location:** WebUI/war/gadgets/repository/common/lib/jqplot/jquery.jqplot.js:8747
  - **Message:** Properties are copied from obj2 to obj1 without guarding against prototype pollution.

- **Alert #206** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/206
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/dojo/src/debug/deep.html:225
  - **Message:** This replaces only the first occurrence of "\\r".

- **Alert #170** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/170
  - **Location:** WebUI/war/widgets/perc_site_map.js:2193
  - **Message:** This replaces only the first occurrence of "{{".

- **Alert #169** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/169
  - **Location:** WebUI/war/services/PercUserService.js:169
  - **Message:** This replaces only the first occurrence of "%".

- **Alert #168** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/168
  - **Location:** WebUI/war/plugins/perc_utils.js:1669
  - **Message:** This replaces only the first occurrence of /'/.

- **Alert #167** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/167
  - **Location:** WebUI/war/plugins/perc_css_utils.js:118
  - **Message:** This replaces only the first occurrence of "\\"".

- **Alert #162** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/162
  - **Location:** WebUI/war/jslib/profiles/3x/libraries/momentjs/moment-with-locales.js:810
  - **Message:** This replaces only the first occurrence of '\\\\'.

- **Alert #161** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/161
  - **Location:** WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-testrunner/testrunner.js:778
  - **Message:** This does not escape backslash characters in the input.

- **Alert #160** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/160
  - **Location:** WebUI/war/jslib/profiles/3x/jquery/libraries/jquery-ui/jquery-ui.js:17945
  - **Message:** This does not escape backslash characters in the input.

- **Alert #158** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/158
  - **Location:** WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-datatables/js/jquery.dataTables.js:8347
  - **Message:** This does not escape backslash characters in the input.

- **Alert #157** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/157
  - **Location:** WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-datatables/js/jquery.dataTables.js:4541
  - **Message:** This replaces only the first occurrence of '"'.

- **Alert #154** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/154
  - **Location:** WebUI/war/api/lib/handlebars-1.0.0.js:912
  - **Message:** This replaces only the first occurrence of "\\n".

- **Alert #153** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/153
  - **Location:** WebUI/war/WEB-INF/classes/features/perc/getDashboardColumn/perc_getDashboardColumn.js:11
  - **Message:** This replaces only the first occurrence of /\\[\\\\[\\]/.

- **Alert #152** — `js/incomplete-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/152
  - **Location:** WebUI/war/WEB-INF/classes/features/perc/getDashboardColumn/perc_getDashboardColumn.js:11
  - **Message:** This replaces only the first occurrence of /\\[\\\\]\\]/.

- **Alert #148** — `js/unsafe-html-expansion` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/148
  - **Location:** WebUI/war/jslib/profiles/3x/jquery/jquery-migrate-3.3.2.js:745
  - **Message:** This self-closing HTML tag expansion invalidates prior sanitization as this regular expression may match part of an attribute value.

- **Alert #147** — `js/unsafe-html-expansion` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/147
  - **Location:** WebUI/war/jslib/profiles/3x/jquery/jquery-migrate-3.3.2.js:736
  - **Message:** This self-closing HTML tag expansion invalidates prior sanitization as this regular expression may match part of an attribute value.

- **Alert #143** — `js/incomplete-multi-character-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/143
  - **Location:** WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-datatables/js/jquery.dataTables.js:15005
  - **Message:** This string may still contain <script, which may cause an HTML element injection vulnerability.

- **Alert #142** — `js/incomplete-multi-character-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/142
  - **Location:** WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-datatables/js/jquery.dataTables.js:14910
  - **Message:** This string may still contain <script, which may cause an HTML element injection vulnerability.

- **Alert #141** — `js/incomplete-multi-character-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/141
  - **Location:** WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-datatables/js/jquery.dataTables.js:6134
  - **Message:** This string may still contain <script, which may cause an HTML element injection vulnerability.

- **Alert #140** — `js/incomplete-multi-character-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/140
  - **Location:** WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-datatables/js/jquery.dataTables.js:5883
  - **Message:** This string may still contain <script, which may cause an HTML element injection vulnerability.

- **Alert #139** — `js/incomplete-multi-character-sanitization` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/139
  - **Location:** WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-datatables/js/jquery.dataTables.js:1498
  - **Message:** This string may still contain <script, which may cause an HTML element injection vulnerability.

- **Alert #98** — `js/useless-regexp-character-escape` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/98
  - **Location:** WebUI/war/views/PercCommonMinuetView.js:182
  - **Message:** The escape sequence '\\?' is equivalent to just '?', so the sequence may still represent a meta-character when it is used in a regular expression.

- **Alert #97** — `js/useless-regexp-character-escape` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/97
  - **Location:** WebUI/war/views/PercCommonMinuetView.js:181
  - **Message:** The escape sequence '\\?' is equivalent to just '?', so the sequence may still represent a meta-character when it is used in a regular expression.\nThe escape sequence '\\?' is equivalent to just '?', so the sequence may still represent a meta-character when it is used in a regular expression.

- **Alert #96** — `js/useless-regexp-character-escape` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/96
  - **Location:** WebUI/war/api/lib/swagger.js:938
  - **Message:** The escape sequence '\\}}' is equivalent to just '}}', so the sequence may still represent a meta-character when it is used in a regular expression.

- **Alert #95** — `js/useless-regexp-character-escape` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/95
  - **Location:** WebUI/war/api/lib/swagger.js:938
  - **Message:** The escape sequence '\\}}' is equivalent to just '}}', so the sequence may still represent a meta-character when it is used in a regular expression.

- **Alert #94** — `js/useless-regexp-character-escape` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/94
  - **Location:** WebUI/war/api/lib/swagger.js:938
  - **Message:** The escape sequence '\\{{' is equivalent to just '{{', so the sequence may still represent a meta-character when it is used in a regular expression.

- **Alert #86** — `js/unvalidated-dynamic-method-call` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/86
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/trinidad/adf/jsLibsDebug/Core.js:697
  - **Message:** Invocation of method with user-controlled name may dispatch to unexpected target and cause an exception.

- **Alert #78** — `js/code-injection` (critical, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/78
  - **Location:** system/cms/content/applications/sys_resources/ApplicationFiles/dojo/iframe_history.html:21
  - **Message:** This code execution depends on a user-provided value.

- **Alert #57** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/57
  - **Location:** projects/sitemanage/src/test/resources/importer/CM1905-SamplePage.html:55
  - **Message:** Script loaded using unencrypted connection.

- **Alert #56** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/56
  - **Location:** projects/sitemanage/src/test/resources/importer/CM1905-SamplePage.html:56
  - **Message:** Script loaded using unencrypted connection.

- **Alert #55** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/55
  - **Location:** projects/sitemanage/src/test/resources/importer/CM1905-SamplePage.html:57
  - **Message:** Script loaded using unencrypted connection.

- **Alert #54** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/54
  - **Location:** projects/sitemanage/src/test/java/com/percussion/sitemanage/importer/helpers/CM1094-SamplePage.html:30
  - **Message:** Script loaded using unencrypted connection.

- **Alert #53** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/53
  - **Location:** projects/sitemanage/src/test/java/com/percussion/sitemanage/importer/helpers/CM1094-SamplePage.html:31
  - **Message:** Script loaded using unencrypted connection.

- **Alert #52** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/52
  - **Location:** projects/sitemanage/src/test/java/com/percussion/sitemanage/importer/helpers/CM1094-SamplePage.html:32
  - **Message:** Script loaded using unencrypted connection.

- **Alert #51** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/51
  - **Location:** projects/sitemanage/src/test/java/com/percussion/sitemanage/importer/helpers/CM1094-SamplePage.html:38
  - **Message:** Script loaded using unencrypted connection.

- **Alert #50** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/50
  - **Location:** projects/sitemanage/src/test/java/com/percussion/sitemanage/importer/helpers/CM1094-SamplePage.html:39
  - **Message:** Script loaded using unencrypted connection.

- **Alert #49** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/49
  - **Location:** projects/sitemanage/src/test/java/com/percussion/sitemanage/importer/helpers/CM1094-SamplePage.html:42
  - **Message:** Script loaded using unencrypted connection.

- **Alert #48** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/48
  - **Location:** projects/sitemanage/src/test/java/com/percussion/sitemanage/importer/helpers/CM1094-SamplePage.html:45
  - **Message:** Script loaded using unencrypted connection.

- **Alert #47** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/47
  - **Location:** projects/sitemanage/src/test/java/com/percussion/sitemanage/importer/helpers/CM1094-SamplePage.html:63
  - **Message:** Script loaded using unencrypted connection.

- **Alert #46** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/46
  - **Location:** projects/sitemanage/src/test/java/com/percussion/sitemanage/importer/helpers/CM1094-SamplePage.html:64
  - **Message:** Script loaded using unencrypted connection.

- **Alert #45** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/45
  - **Location:** projects/sitemanage/src/test/java/com/percussion/sitemanage/importer/helpers/CM1094-SamplePage.html:65
  - **Message:** Script loaded using unencrypted connection.

- **Alert #44** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/44
  - **Location:** projects/sitemanage/src/test/java/com/percussion/sitemanage/importer/helpers/CM1094-SamplePage.html:71
  - **Message:** Script loaded using unencrypted connection.

- **Alert #43** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/43
  - **Location:** projects/sitemanage/src/test/java/com/percussion/sitemanage/importer/helpers/CM1094-SamplePage.html:72
  - **Message:** Script loaded using unencrypted connection.

- **Alert #42** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/42
  - **Location:** projects/sitemanage/src/test/java/com/percussion/sitemanage/importer/helpers/CM1094-SamplePage.html:75
  - **Message:** Script loaded using unencrypted connection.

- **Alert #41** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/41
  - **Location:** projects/sitemanage/src/test/java/com/percussion/sitemanage/importer/helpers/CM1094-SamplePage.html:78
  - **Message:** Script loaded using unencrypted connection.

- **Alert #40** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/40
  - **Location:** projects/sitemanage/src/test/java/com/percussion/sitemanage/importer/helpers/CM1094-SamplePage.html:87
  - **Message:** Script loaded using unencrypted connection.

- **Alert #39** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/39
  - **Location:** projects/sitemanage/src/test/java/com/percussion/sitemanage/importer/helpers/CM1094-SamplePage.html:88
  - **Message:** Script loaded using unencrypted connection.

- **Alert #38** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/38
  - **Location:** projects/sitemanage/src/test/java/com/percussion/sitemanage/importer/helpers/CM1094-SamplePage.html:89
  - **Message:** Script loaded using unencrypted connection.

- **Alert #37** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/37
  - **Location:** projects/sitemanage/src/test/java/com/percussion/sitemanage/importer/helpers/CM1094-SamplePage.html:95
  - **Message:** Script loaded using unencrypted connection.

- **Alert #36** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/36
  - **Location:** projects/sitemanage/src/test/java/com/percussion/sitemanage/importer/helpers/CM1094-SamplePage.html:96
  - **Message:** Script loaded using unencrypted connection.

- **Alert #35** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/35
  - **Location:** projects/sitemanage/src/test/java/com/percussion/sitemanage/importer/helpers/CM1094-SamplePage.html:99
  - **Message:** Script loaded using unencrypted connection.

- **Alert #34** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/34
  - **Location:** projects/sitemanage/src/test/java/com/percussion/sitemanage/importer/helpers/CM1094-SamplePage.html:102
  - **Message:** Script loaded using unencrypted connection.

- **Alert #33** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/33
  - **Location:** projects/sitemanage/src/test/java/com/percussion/sitemanage/importer/helpers/CM1094-SamplePage.html:107
  - **Message:** Script loaded using unencrypted connection.

- **Alert #32** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/32
  - **Location:** projects/sitemanage/src/test/java/com/percussion/sitemanage/importer/helpers/CM1094-SamplePage.html:108
  - **Message:** Script loaded using unencrypted connection.

- **Alert #31** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/31
  - **Location:** projects/sitemanage/src/test/java/com/percussion/sitemanage/importer/helpers/CM1094-SamplePage.html:109
  - **Message:** Script loaded using unencrypted connection.

- **Alert #30** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/30
  - **Location:** projects/sitemanage/src/test/java/com/percussion/sitemanage/importer/helpers/CM1094-SamplePage.html:115
  - **Message:** Script loaded using unencrypted connection.

- **Alert #29** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/29
  - **Location:** projects/sitemanage/src/test/java/com/percussion/sitemanage/importer/helpers/CM1094-SamplePage.html:116
  - **Message:** Script loaded using unencrypted connection.

- **Alert #28** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/28
  - **Location:** projects/sitemanage/src/test/java/com/percussion/sitemanage/importer/helpers/CM1094-SamplePage.html:119
  - **Message:** Script loaded using unencrypted connection.

- **Alert #27** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/27
  - **Location:** projects/sitemanage/src/test/java/com/percussion/sitemanage/importer/helpers/CM1094-SamplePage.html:122
  - **Message:** Script loaded using unencrypted connection.

- **Alert #26** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/26
  - **Location:** projects/sitemanage/src/test/java/com/percussion/share/dao/home_ws.html:343
  - **Message:** Script loaded using unencrypted connection.

- **Alert #25** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/25
  - **Location:** projects/sitemanage/src/test/java/com/percussion/share/dao/home_sw.html:639
  - **Message:** Script loaded using unencrypted connection.

- **Alert #24** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/24
  - **Location:** projects/sitemanage/src/test/java/com/percussion/share/dao/home_cc.html:628
  - **Message:** Script loaded using unencrypted connection.

- **Alert #23** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/23
  - **Location:** modules/perc-toolkit/src/main/resources/InstallDir/webapp/WEB-INF/pages/importtest_post.jsp:52
  - **Message:** Script loaded using unencrypted connection.

- **Alert #22** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/22
  - **Location:** modules/perc-toolkit/src/main/resources/InstallDir/user/pages/DispatchTemplateGenerator.jsp:31
  - **Message:** Script loaded using unencrypted connection.\nScript loaded from content delivery network with no integrity check.

- **Alert #21** — `js/functionality-from-untrusted-source` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2025-07-15T16:17:58Z
  - **URL:** https://github.com/intersoftdatalabs-in/percussioncms/security/code-scanning/21
  - **Location:** modules/perc-toolkit/src/main/resources/InstallDir/user/pages/DispatchTemplateGenerator.jsp:32
  - **Message:** Script loaded using unencrypted connection.\nScript loaded from content delivery network with no integrity check.

