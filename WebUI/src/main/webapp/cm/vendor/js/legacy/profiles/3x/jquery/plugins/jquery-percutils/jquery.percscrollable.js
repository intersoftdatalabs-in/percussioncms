/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * [jquery.percscrollable]
 *
 * First-party replacement for the retired jQuery Tools Scrollable 1.1.2
 * (2009, Tero Piirainen; removed per issue #7181, parent audit #5627).
 * Exposes the same `$.fn.scrollable` API subset consumed by the classic
 * template dialogs (`size`, `items`, `keyboard`, `globalNav`, `prev`,
 * `next`, `prevPage`, `nextPage`, `clickable`, plus the
 * `.data("scrollable")` handle with `begin` / `seekTo` / `updateItemWrap` /
 * `getConf`), so no call site changes when swapping the script include.
 * Positioning keeps the original contract: the item wrap is animated to
 * `-item.position()` with jQuery, so the existing
 * `div.perc-scrollable` / `div.perc-items` CSS is untouched.
 */
(function ($) {
  var defaults = {
    // basics
    size: 5,
    vertical: false,
    speed: 400,
    keyboard: true,

    // by default this is the same as size
    keyboardSteps: null,

    // other
    disabledClass: "disabled",
    hoverClass: null,
    clickable: true,
    activeClass: "active",
    easing: "swing",
    loop: false,

    items: ".items",
    item: null,

    // navigational elements
    prev: ".prev",
    next: ".next",
    prevPage: ".prevPage",
    nextPage: ".nextPage",
    api: false,

    // CALLBACKS: onBeforeSeek, onSeek, onReload (bound as events)
  };

  var current;

  // constructor
  function PercScrollable(root, conf) {
    // current instance
    var self = this,
      $self = $(this),
      horizontal = !conf.vertical,
      wrap = root.children(),
      index = 0,
      forward;

    if (!current) {
      current = self;
    }

    // bind all callbacks from configuration
    $.each(conf, function (name, fn) {
      if (typeof fn === "function") {
        $self.on(name, fn);
      }
    });

    if (wrap.length > 1) {
      wrap = $(conf.items, root);
    }

    // navigational items can be anywhere when globalNav = true
    function find(query) {
      var els = $(query);
      return conf.globalNav ? els : root.parent().find(query);
    }

    // get handle to navigational elements
    var prev = find(conf.prev),
      next = find(conf.next),
      prevPage = find(conf.prevPage),
      nextPage = find(conf.nextPage);

    // methods
    $.extend(self, {
      getIndex: function () {
        return index;
      },

      getClickIndex: function () {
        var items = self.getItems();
        return items.index(items.filter("." + conf.activeClass));
      },

      getConf: function () {
        return conf;
      },

      getSize: function () {
        return self.getItems().length;
      },

      getPageAmount: function () {
        return Math.ceil(this.getSize() / conf.size);
      },

      getPageIndex: function () {
        return Math.ceil(index / conf.size);
      },

      getNaviButtons: function () {
        return prev.add(next).add(prevPage).add(nextPage);
      },

      getRoot: function () {
        return root;
      },

      getItemWrap: function () {
        return wrap;
      },

      updateItemWrap: function () {
        wrap = self.getRoot().children();
      },

      getItems: function () {
        return wrap.children(conf.item);
      },

      getVisibleItems: function () {
        return self.getItems().slice(index, index + conf.size);
      },

      /* all seeking functions depend on this */
      seekTo: function (i, time, fn) {
        if (i < 0) {
          i = 0;
        }

        // nothing happens
        if (index === i) {
          return self;
        }

        // function given as second argument
        if (typeof time === "function") {
          fn = time;
        }

        // seeking exceeds the end
        if (i > self.getSize() - conf.size) {
          return conf.loop ? self.begin() : this.end();
        }

        var item = self.getItems().eq(i);
        if (!item.length) {
          return self;
        }

        // onBeforeSeek
        var e = $.Event("onBeforeSeek");

        $self.trigger(e, [i]);
        if (e.isDefaultPrevented()) {
          return self;
        }

        // get the (possibly altered) speed
        if (time === undefined || typeof time === "function") {
          time = conf.speed;
        }

        function callback() {
          if (fn) {
            fn.call(self, i);
          }
          $self.trigger("onSeek", [i]);
        }

        if (horizontal) {
          wrap.animate(
            { left: -item.position().left },
            time,
            conf.easing,
            callback,
          );
        } else {
          wrap.animate(
            { top: -item.position().top },
            time,
            conf.easing,
            callback,
          );
        }

        current = self;
        index = i;

        // onStart
        e = $.Event("onStart");
        $self.trigger(e, [i]);
        if (e.isDefaultPrevented()) {
          return self;
        }

        /* default behaviour */

        // prev/next buttons disabled flags
        prev.add(prevPage).toggleClass(conf.disabledClass, i === 0);
        next
          .add(nextPage)
          .toggleClass(conf.disabledClass, i >= self.getSize() - conf.size);

        return self;
      },

      move: function (offset, time, fn) {
        forward = offset > 0;
        return this.seekTo(index + offset, time, fn);
      },

      next: function (time, fn) {
        return this.move(1, time, fn);
      },

      prev: function (time, fn) {
        return this.move(-1, time, fn);
      },

      movePage: function (offset, time, fn) {
        forward = offset > 0;
        var steps = conf.size * offset;

        var i = index % conf.size;
        if (i > 0) {
          steps += offset > 0 ? -i : conf.size - i;
        }

        return this.move(steps, time, fn);
      },

      nextPage: function (time, fn) {
        return this.movePage(1, time, fn);
      },

      prevPage: function (time, fn) {
        return this.movePage(-1, time, fn);
      },

      setPage: function (page, time, fn) {
        return this.seekTo(page * conf.size, time, fn);
      },

      begin: function (time, fn) {
        forward = false;
        return this.seekTo(0, time, fn);
      },

      end: function (time, fn) {
        forward = true;
        var to = this.getSize() - conf.size;
        return to > 0 ? this.seekTo(to, time, fn) : self;
      },

      reload: function () {
        $self.trigger("onReload");
        return self;
      },

      focus: function () {
        current = self;
        return self;
      },

      click: function (i, time, fn) {
        var item = self.getItems().eq(i),
          klass = conf.activeClass,
          size = conf.size;

        // check that i is sane
        if (i < 0 || i >= self.getSize()) {
          return self;
        }

        // size == 1
        if (size == 1) {
          if (conf.loop) {
            return self.next();
          }

          if (i === 0 || i == self.getSize() - 1) {
            forward = forward === undefined ? true : !forward;
          }
          return forward === false ? self.prev() : self.next();
        }

        // size == 2
        if (size == 2) {
          if (i == index) {
            i--;
          }
          self.getItems().removeClass(klass);
          item.addClass(klass);
          return self.seekTo(i, time, fn);
        }

        if (!item.hasClass(klass)) {
          self.getItems().removeClass(klass);
          item.addClass(klass);
          var delta = Math.floor(size / 2);
          var to = i - delta;

          // next to last item must work
          if (to > self.getSize() - size) {
            to = self.getSize() - size;
          }

          if (to !== i) {
            return self.seekTo(to);
          }
        }

        return self;
      },

      // bind / unbind
      // NOTE: preserved verbatim from the retired jTools original, quirk
      // included: `on` unbinds and `off` binds nothing, so the hover /
      // clickable / keyboard block registered via `self.onReload(...)`
      // below never fires. That is the long-deployed behavior (every call
      // site binds its own item click and arrow-key handlers), and this
      // shim must not change it. Do not "fix" without a live pass over
      // the classic template dialogs.
      on: function (name, fn) {
        $self.off(name, fn);
        return self;
      },

      off: function (name) {
        $self.on(name);
        return self;
      },
    });

    // callbacks
    $.each("onBeforeSeek,onStart,onSeek,onReload".split(","), function (i, ev) {
      self[ev] = function (fn) {
        return self.on(ev, fn);
      };
    });

    // prev button
    prev.addClass(conf.disabledClass).on("click", function () {
      self.prev();
    });

    // next button
    next.on("click", function () {
      self.next();
    });

    // prev page button
    nextPage.on("click", function () {
      self.nextPage();
    });

    if (self.getSize() < conf.size) {
      next.add(nextPage).addClass(conf.disabledClass);
    }

    // next page button
    prevPage.addClass(conf.disabledClass).on("click", function () {
      self.prevPage();
    });

    // hover
    var hc = conf.hoverClass,
      keyId = "keydown." + Math.random().toString().substring(10);

    self.onReload(function () {
      // hovering
      if (hc) {
        self
          .getItems()
          .on("mouseenter", function () {
            $(this).addClass(hc);
          })
          .on("mouseleave", function () {
            $(this).removeClass(hc);
          });
      }

      // clickable
      if (conf.clickable) {
        self.getItems().each(function (i) {
          $(this)
            .off("click.scrollable")
            .on("click.scrollable", function (e) {
              if ($(e.target).is("a")) {
                return;
              }
              return self.click(i);
            });
        });
      }

      // keyboard
      if (conf.keyboard) {
        // keyboard works on one instance at the time. thus we need to unbind first
        $(document)
          .off(keyId)
          .on(keyId, function (evt) {
            // do nothing with CTRL / ALT buttons
            if (evt.altKey || evt.ctrlKey) {
              return;
            }

            // do nothing for unstatic and unfocused instances
            if (conf.keyboard !== "static" && current !== self) {
              return;
            }

            var s = conf.keyboardSteps;

            if (horizontal && (evt.keyCode === 37 || evt.keyCode === 39)) {
              self.move(evt.keyCode === 37 ? -s : s);
              return evt.preventDefault();
            }

            if (!horizontal && (evt.keyCode === 38 || evt.keyCode === 40)) {
              self.move(evt.keyCode === 38 ? -s : s);
              return evt.preventDefault();
            }

            return true;
          });
      } else {
        $(document).off(keyId);
      }
    });

    self.reload();
  }

  // jQuery plugin implementation
  $.fn.scrollable = function (conf) {
    // already constructed --> return API
    var el = this.eq(typeof conf == "number" ? conf : 0).data("scrollable");
    if (el) {
      return el;
    }

    var globals = $.extend({}, defaults);
    conf = $.extend(globals, conf);

    conf.keyboardSteps = conf.keyboardSteps || conf.size;

    this.each(function () {
      el = new PercScrollable($(this), conf);
      $(this).data("scrollable", el);
    });

    return conf.api ? el : this;
  };
})(jQuery);
