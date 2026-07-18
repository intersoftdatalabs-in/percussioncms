document.addEventListener("DOMContentLoaded", function (event) {
  (function ($) {
    $(function () {
      /**
       * Escape text for insertion into an HTML context (attribute or text).
       * Closes CodeQL js/xss #945 on the mobile-preview document.write shell.
       */
      function escapeHtml(value) {
        return String(value == null ? "" : value)
          .replace(/&/g, "&amp;")
          .replace(/</g, "&lt;")
          .replace(/>/g, "&gt;")
          .replace(/"/g, "&quot;")
          .replace(/'/g, "&#39;");
      }

      /**
       * Restrict a URL to same-origin http(s) for use as an iframe src.
       * Rejects javascript:/data: and cross-origin values.
       */
      function safeSameOriginHttpUrl(rawUrl, origin) {
        try {
          var u = new URL(rawUrl, origin);
          if (u.origin !== origin) {
            return origin + "/";
          }
          if (u.protocol !== "http:" && u.protocol !== "https:") {
            return origin + "/";
          }
          return u.href;
        } catch (e) {
          return origin + "/";
        }
      }

      var prurl = window.location.href;
      var baseurl =
        window.location.protocol + "//" + window.location.host;
      if (prurl.indexOf("percmobilepreview=true") > 0) {
        prurl = prurl.replace("percmobilepreview=true", "");
        prurl = safeSameOriginHttpUrl(prurl, baseurl);
        javascript: void (function () {
          var d = document;
          var safeTitle = escapeHtml(d.title || "Mobile Preview");
          var safeBase = escapeHtml(baseurl);
          var safeFrameSrc = escapeHtml(prurl);
          d.write(
            '<!DOCTYPE html><html><head><meta charset="UTF-8"><title>' +
              safeTitle +
              '</title><link rel="stylesheet" href="' +
              safeBase +
              '/cm/jslib/profiles/3x/libraries/fontawesome/css/all.css" rel="stylesheet"><link rel="stylesheet" href="' +
              safeBase +
              '/Rhythmyx/sys_resources/mobilepreview/css/PercMobileApp.css"><script src="' +
              safeBase +
              '/Rhythmyx/sys_resources/js/href.js"></script><script src="' +
              safeBase +
              '/Rhythmyx/sys_resources/mobilepreview/js/PercViewport.js"></script></head><body onload=""><header><div class="percclose" title="Collapse Toolbar"><a href="#"><i id="perccollapse" class="icon-double-angle-down fas fa-angle-double-down fa-2x"></i></a></div><div id="percsize"></div><div id="percdevices"><a href="#" id="largetabletportrait" class="perc-large-tablet-portrait" title="Large Tablet(Portrait)"><span><i class="icon-tablet icon-large fas fa-tablet"></i></span></a><a href="#" id="largetabletlandscape" class="perc-large-tablet-landscape" title="Large Tablet(Landscape)"><span><i class="icon-tablet icon-large fas fa-tablet"></i></span></a><a href="#" id="smalltabletportrait" class="perc-small-tablet-portrait" title="Small Tablet(Portrait)"><span><i class="icon-tablet icon-2x fas fa-tablet"></i></span></a><a href="#" id="smalltabletlandscape" class="perc-small-tablet-landscape" title="Small Tablet(Landscape)"><span><i class="icon-tablet icon-2x fas fa-tablet"></i></span></a><a href="#" id="smartphoneportrait" class="perc-smartphone-portrait" title="Mobile Phone(Portrait)"><span><i class="icon-mobile-phone icon-2x fas fa-mobile"></i></span></a><a href="#" id="smartphonelandscape" class="perc-smartphone-landscape" title="Mobile Phone(Landscape)"><span><i class="icon-mobile-phone icon-2x fas fa-mobile"></i></span></a><a href="#" id="auto" class="perc-auto active"><span>Auto</span></a></div></header><section><div id="percwrapper"><iframe id="percmobilepreviewframe" name="percmobilepreviewframe" onload="javascript:window:mobilePreviewFrameOnload();" src="' +
              safeFrameSrc +
              '" title="Mobile Preview"></iframe></div></section></body></html>'
          );
          d.close();
        })();
      }
    });
  })(jQuery);
});
