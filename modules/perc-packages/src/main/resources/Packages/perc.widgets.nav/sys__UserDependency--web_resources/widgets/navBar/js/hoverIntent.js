/*
 * hoverIntent r7 // 2013.02.11 // jQuery 1.9.0+
 * http://cherne.net/brian/resources/jquery.hoverIntent.html
 *
 * You may use hoverIntent under the terms of the MIT license.
 * Copyright 2007, 2013 Brian Cherne
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 *
 * Percussion-maintained copy (issue #6689): jQuery 4-compatible cleanups
 * (`==` to `===`, `jQuery.extend` to `$.extend`); behavior contract unchanged
 * for superfish 1.7.10. Byte-identical compat aliases (`hoverIntent.js` /
 * `hoverIntent.min.js`) keep already-published pages and custom templates
 * resolving; see the upgrade note.
 */

/**
 * Percussion hover-intent shim ($.fn.hoverIntent, hoverIntent r7-derived).
 *
 * Percussion-maintained copy of the classic briancherne jQuery hoverIntent
 * plugin (unmaintained 1.x line, no npm equivalent — issue #6655 spike
 * verdict) carrying the `$.fn.hoverIntent` contract that superfish 1.7.10
 * consumes at runtime (`$.fn.hoverIntent && !o.disableHI` in
 * `applyHandlers`). superfish.js and navbar.js stay untouched; the payload
 * references move to this file (preview) and its minified publish twin
 * (perc-hover-intent.min.js). Byte-identical compat aliases
 * (`hoverIntent.js` / `hoverIntent.min.js`) keep already-published pages
 * and custom templates resolving; see the upgrade note.
 *
 * Contract (same as hoverIntent r7):
 *   - `$(...).hoverIntent(handlerIn, handlerOut, selector)` — the form
 *     superfish uses with its `li:has(...)` targets.
 *   - `$(...).hoverIntent(handlerIn, handlerOut)` — direct binding.
 *   - `$(...).hoverIntent({over, out, interval, sensitivity, timeout, ...})`.
 *   - Defaults: interval 100ms, sensitivity 7px, timeout 0ms.
 *   - `over` fires only once the pointer slows below the sensitivity
 *     threshold, so fast diagonal pass-through never opens a submenu;
 *     `out` fires `timeout` ms after mouseleave while hovering.
 *   - All listeners use the `.hoverIntent` namespace so superfish
 *     `destroy` (`off('.hoverIntent')`) keeps working unchanged.
 */
(function ($) {
  $.fn.hoverIntent = function (handlerIn, handlerOut, selector) {
    // default configuration values
    var cfg = {
      interval: 100,
      sensitivity: 7,
      timeout: 0,
    };

    if (typeof handlerIn === "object") {
      cfg = $.extend(cfg, handlerIn);
    } else if (typeof handlerOut === "function") {
      cfg = $.extend(cfg, {
        over: handlerIn,
        out: handlerOut,
        selector: selector,
      });
    } else {
      cfg = $.extend(cfg, {
        over: handlerIn,
        out: handlerIn,
        selector: handlerOut,
      });
    }

    // instantiate variables
    // cX, cY = current X and Y position of mouse, updated by mousemove event
    // pX, pY = previous X and Y position of mouse, set by mouseenter and polling interval
    var cX, cY, pX, pY;

    // A private function for getting mouse position
    var track = function (ev) {
      cX = ev.pageX;
      cY = ev.pageY;
    };

    // A private function for comparing current and previous mouse position
    var compare = function (ev, ob) {
      ob.hoverIntent_t = clearTimeout(ob.hoverIntent_t);
      // compare mouse positions to see if they've crossed the threshold
      if (Math.abs(pX - cX) + Math.abs(pY - cY) < cfg.sensitivity) {
        $(ob).off("mousemove.hoverIntent", track);
        // set hoverIntent state to true (so mouseOut can be called)
        ob.hoverIntent_s = 1;
        return cfg.over.apply(ob, [ev]);
      } else {
        // set previous coordinates for next time
        pX = cX;
        pY = cY;
        // use self-calling timeout, guarantees intervals are spaced out properly (avoids JavaScript timer bugs)
        ob.hoverIntent_t = setTimeout(function () {
          compare(ev, ob);
        }, cfg.interval);
      }
    };

    // A private function for delaying the mouseOut function
    var delay = function (ev, ob) {
      ob.hoverIntent_t = clearTimeout(ob.hoverIntent_t);
      ob.hoverIntent_s = 0;
      return cfg.out.apply(ob, [ev]);
    };

    // A private function for handling mouse 'hovering'
    var handleHover = function (e) {
      // copy objects to be passed into t (required for event object to be passed in IE)
      var ev = $.extend({}, e);
      var ob = this;

      // cancel hoverIntent timer if it exists
      if (ob.hoverIntent_t) {
        ob.hoverIntent_t = clearTimeout(ob.hoverIntent_t);
      }

      // if e.type == "mouseenter"
      if (e.type === "mouseenter") {
        // set "previous" X and Y position based on initial entry point
        pX = ev.pageX;
        pY = ev.pageY;
        // update "current" X and Y position based on mousemove
        $(ob).on("mousemove.hoverIntent", track);
        // start polling interval (self-calling timeout) to compare mouse coordinates over time
        if (ob.hoverIntent_s !== 1) {
          ob.hoverIntent_t = setTimeout(function () {
            compare(ev, ob);
          }, cfg.interval);
        }

        // else e.type == "mouseleave"
      } else {
        // unbind expensive mousemove event
        $(ob).off("mousemove.hoverIntent", track);
        // if hoverIntent state is true, then call the mouseOut function after the specified delay
        if (ob.hoverIntent_s === 1) {
          ob.hoverIntent_t = setTimeout(function () {
            delay(ev, ob);
          }, cfg.timeout);
        }
      }
    };

    // listen for mouseenter and mouseleave
    return this.on(
      {
        "mouseenter.hoverIntent": handleHover,
        "mouseleave.hoverIntent": handleHover,
      },
      cfg.selector,
    );
  };
})(jQuery);
