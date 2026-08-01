var WIFXPath = "/Rhythmyx/rx_resources/webimagefx/";

function isVBScriptSupported() {
  var isWindows = window.navigator.platform.indexOf("Win") > -1;
  var isIE = false;
  var ua = window.navigator.userAgent;
  var pOpera = ua.indexOf("Opera");
  if (pOpera == -1) {
    var pIE = ua.indexOf("MSIE ");
    isIE = pIE > -1;
  }
  return isWindows && isIE;
}

/**
 * Build the WebImageFX license handler URL without embedding raw
 * window.location.href into a script src (CodeQL js/xss #946).
 *
 * Uses origin + a path-only CMS root prefix (characters restricted to a
 * safe identifier set). Query/hash and free-form href text never enter
 * the script src.
 */
function getWifxLicenseHandlerPath() {
  var path = window.location.pathname || "";
  var pos = path.indexOf("/Rhythmyx");
  var rxRoot = pos >= 0 ? path.substring(0, pos + 9) : "/Rhythmyx";
  // Fail closed if the derived prefix is not a safe path.
  if (!/^\/[A-Za-z0-9._\-\/]*$/.test(rxRoot)) {
    rxRoot = "/Rhythmyx";
  }
  var origin = window.location.protocol + "//" + window.location.host;
  return origin + rxRoot + "/rx_wep/ektron?licensekey=webimagefx1";
}

function defaultMsgsFilename() {
  var strLanguageCode = "";

  if (navigator.language) {
    // for Netscape
    strLanguageCode = navigator.language;
  }
  if (navigator.userLanguage) {
    // for IE
    strLanguageCode = navigator.userLanguage;
  }
  var strTranslatedLangCodes = "zh-tw";
  if (strTranslatedLangCodes.indexOf(strLanguageCode) == -1) {
    strLanguageCode = strLanguageCode.substring(0, 2);
    var strTranslatedLanguages = "ar,da,de,es,fr,he,it,ja,ko,nl,pt,ru,sv,zh";
    if (strTranslatedLanguages.indexOf(strLanguageCode) == -1) {
      // not a translated language
      strLanguageCode = ""; // use default (English)
    }
  }
  // Language code is allow-listed above; still strip anything outside [a-z-]
  // so the script filename cannot break out of the src attribute.
  strLanguageCode = String(strLanguageCode).replace(/[^a-z\-]/g, "");
  return "webimagefxmessages" + strLanguageCode + ".js";
}

if (typeof WebImageFXIncludes == "undefined") {
  // Include license key(s) that are in file webimagefxlicensekey.txt
  document.writeln(
    '<script language="JavaScript1.2" src="' +
      getWifxLicenseHandlerPath() +
      '"></script>',
  );
  // The above two license key values are concatinated in RegisterLicense().

  if (isVBScriptSupported()) {
    document.writeln(
      '<script type="text/vbscript" language="VBScript" src="' +
        WIFXPath +
        'wifx.vbs"></script>',
    );
  }

  // Assign default messages file if not already defined.
  if ("undefined" == typeof WebImageFXMsgsFilename || !WebImageFXMsgsFilename) {
    WebImageFXMsgsFilename = defaultMsgsFilename();
  }

  var WebImageFXIncludes = [
    "webimagefxevents.js",
    "webimagefxdefaults.js",
    WebImageFXMsgsFilename,
    "wifx.js",
  ];

  for (var i = 0; i < WebImageFXIncludes.length; i++) {
    document.writeln(
      '<script language="JavaScript1.2" src="' +
        WIFXPath +
        WebImageFXIncludes[i] +
        '"></script>',
    );
  }
}
