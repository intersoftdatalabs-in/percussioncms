/**
 * Bootstrap 5 Compatibility Layer
 *
 * Bridges jQuery plugin-style API calls (from Bootstrap 4) to Bootstrap 5's standalone JavaScript API.
 * This allows existing code using $.fn.modal('show'), $.fn.tooltip(), etc. to continue working
 * without major refactoring.
 *
 * Usage:
 *   jQuery code continues to work as-is:
 *   $('#myModal').modal('show')        // Still works!
 *   $('[data-bs-toggle="tooltip"]').tooltip()  // Still works!
 *
 * Bootstrap 5 is required to be loaded before this script.
 *
 * @requires bootstrap@5.3.0+
 * @requires jquery@3.6+
 */

(function () {
  "use strict";

  if (typeof window.bootstrap === "undefined") {
    console.error(
      "Bootstrap 5 is required. Please load Bootstrap before loading this compatibility layer.",
    );
    return;
  }

  if (typeof jQuery === "undefined") {
    console.warn("jQuery not found. Some compatibility features may not work.");
  }

  /**
   * Modal Compatibility
   * Maps jQuery API to Bootstrap 5 Modal
   */
  if (jQuery && jQuery.fn) {
    jQuery.fn.modal = function (action) {
      return this.each(function () {
        const el = this;
        let modalInstance = bootstrap.Modal.getInstance(el);

        switch (action) {
          case "show":
            if (!modalInstance) {
              modalInstance = new bootstrap.Modal(el);
            }
            modalInstance.show();
            break;
          case "hide":
            if (modalInstance) {
              modalInstance.hide();
            }
            break;
          case "toggle":
            if (!modalInstance) {
              modalInstance = new bootstrap.Modal(el);
            }
            modalInstance.toggle();
            break;
          case "dispose":
            if (modalInstance) {
              modalInstance.dispose();
            }
            break;
          default:
            // Return instance for chaining when no action specified
            if (!modalInstance) {
              modalInstance = new bootstrap.Modal(el);
            }
            return modalInstance;
        }
      });
    };

    /**
     * Tooltip Compatibility
     * Maps jQuery API to Bootstrap 5 Tooltip
     */
    jQuery.fn.tooltip = function (action) {
      return this.each(function () {
        const el = this;
        let tooltipInstance = bootstrap.Tooltip.getInstance(el);

        switch (action) {
          case "show":
            if (!tooltipInstance) {
              tooltipInstance = new bootstrap.Tooltip(el);
            }
            tooltipInstance.show();
            break;
          case "hide":
            if (tooltipInstance) {
              tooltipInstance.hide();
            }
            break;
          case "toggle":
            if (!tooltipInstance) {
              tooltipInstance = new bootstrap.Tooltip(el);
            }
            tooltipInstance.toggle();
            break;
          case "dispose":
            if (tooltipInstance) {
              tooltipInstance.dispose();
            }
            break;
          case "update":
            if (tooltipInstance) {
              tooltipInstance.update();
            }
            break;
          default:
            // Return instance for chaining when no action specified
            // or initialize if called with options object
            if (typeof action === "object") {
              // action is options
              tooltipInstance = new bootstrap.Tooltip(el, action);
            } else if (!tooltipInstance) {
              tooltipInstance = new bootstrap.Tooltip(el);
            }
            return tooltipInstance;
        }
      });
    };

    /**
     * Popover Compatibility
     * Maps jQuery API to Bootstrap 5 Popover
     */
    jQuery.fn.popover = function (action) {
      return this.each(function () {
        const el = this;
        let popoverInstance = bootstrap.Popover.getInstance(el);

        switch (action) {
          case "show":
            if (!popoverInstance) {
              popoverInstance = new bootstrap.Popover(el);
            }
            popoverInstance.show();
            break;
          case "hide":
            if (popoverInstance) {
              popoverInstance.hide();
            }
            break;
          case "toggle":
            if (!popoverInstance) {
              popoverInstance = new bootstrap.Popover(el);
            }
            popoverInstance.toggle();
            break;
          case "dispose":
            if (popoverInstance) {
              popoverInstance.dispose();
            }
            break;
          case "update":
            if (popoverInstance) {
              popoverInstance.update();
            }
            break;
          default:
            // Return instance for chaining or initialize
            if (typeof action === "object") {
              popoverInstance = new bootstrap.Popover(el, action);
            } else if (!popoverInstance) {
              popoverInstance = new bootstrap.Popover(el);
            }
            return popoverInstance;
        }
      });
    };

    /**
     * Dropdown Compatibility
     * Maps jQuery API to Bootstrap 5 Dropdown
     */
    jQuery.fn.dropdown = function (action) {
      return this.each(function () {
        const el = this;
        let dropdownInstance = bootstrap.Dropdown.getInstance(el);

        switch (action) {
          case "show":
            if (!dropdownInstance) {
              dropdownInstance = new bootstrap.Dropdown(el);
            }
            dropdownInstance.show();
            break;
          case "hide":
            if (dropdownInstance) {
              dropdownInstance.hide();
            }
            break;
          case "toggle":
            if (!dropdownInstance) {
              dropdownInstance = new bootstrap.Dropdown(el);
            }
            dropdownInstance.toggle();
            break;
          case "dispose":
            if (dropdownInstance) {
              dropdownInstance.dispose();
            }
            break;
          case "update":
            if (dropdownInstance) {
              dropdownInstance.update();
            }
            break;
          default:
            // Return instance for chaining or initialize
            if (typeof action === "object") {
              dropdownInstance = new bootstrap.Dropdown(el, action);
            } else if (!dropdownInstance) {
              dropdownInstance = new bootstrap.Dropdown(el);
            }
            return dropdownInstance;
        }
      });
    };

    /**
     * Carousel Compatibility
     * Maps jQuery API to Bootstrap 5 Carousel
     */
    jQuery.fn.carousel = function (action) {
      return this.each(function () {
        const el = this;
        let carouselInstance = bootstrap.Carousel.getInstance(el);

        switch (action) {
          case "cycle":
            if (!carouselInstance) {
              carouselInstance = new bootstrap.Carousel(el);
            }
            carouselInstance.cycle();
            break;
          case "pause":
            if (carouselInstance) {
              carouselInstance.pause();
            }
            break;
          case "prev":
            if (carouselInstance) {
              carouselInstance.prev();
            }
            break;
          case "next":
            if (carouselInstance) {
              carouselInstance.next();
            }
            break;
          case "dispose":
            if (carouselInstance) {
              carouselInstance.dispose();
            }
            break;
          default:
            // If action is a number, go to that slide
            if (typeof action === "number") {
              if (!carouselInstance) {
                carouselInstance = new bootstrap.Carousel(el);
              }
              carouselInstance.to(action);
            } else if (typeof action === "object") {
              // action is options
              carouselInstance = new bootstrap.Carousel(el, action);
            } else if (!carouselInstance) {
              carouselInstance = new bootstrap.Carousel(el);
            }
            return carouselInstance;
        }
      });
    };

    /**
     * Collapse Compatibility
     * Maps jQuery API to Bootstrap 5 Collapse
     */
    jQuery.fn.collapse = function (action) {
      return this.each(function () {
        const el = this;
        let collapseInstance = bootstrap.Collapse.getInstance(el);

        switch (action) {
          case "show":
            if (!collapseInstance) {
              collapseInstance = new bootstrap.Collapse(el, { toggle: false });
            }
            collapseInstance.show();
            break;
          case "hide":
            if (collapseInstance) {
              collapseInstance.hide();
            }
            break;
          case "toggle":
            if (!collapseInstance) {
              collapseInstance = new bootstrap.Collapse(el);
            }
            collapseInstance.toggle();
            break;
          case "dispose":
            if (collapseInstance) {
              collapseInstance.dispose();
            }
            break;
          default:
            // Return instance for chaining or initialize
            if (typeof action === "object") {
              collapseInstance = new bootstrap.Collapse(el, action);
            } else if (!collapseInstance) {
              collapseInstance = new bootstrap.Collapse(el);
            }
            return collapseInstance;
        }
      });
    };

    /**
     * Scrollspy Compatibility
     * Maps jQuery API to Bootstrap 5 ScrollSpy
     */
    jQuery.fn.scrollspy = function (action) {
      return this.each(function () {
        const el = this;
        let scrollspy = bootstrap.ScrollSpy.getInstance(el);

        switch (action) {
          case "refresh":
            if (scrollspy) {
              scrollspy.refresh();
            }
            break;
          case "dispose":
            if (scrollspy) {
              scrollspy.dispose();
            }
            break;
          default:
            // Return instance for chaining or initialize
            if (typeof action === "object") {
              scrollspy = new bootstrap.ScrollSpy(el, action);
            } else if (!scrollspy) {
              scrollspy = new bootstrap.ScrollSpy(el);
            }
            return scrollspy;
        }
      });
    };

    /**
     * Alert Compatibility
     * Maps jQuery API to Bootstrap 5 Alert
     */
    jQuery.fn.alert = function (action) {
      return this.each(function () {
        const el = this;
        let alertInstance = bootstrap.Alert.getInstance(el);

        switch (action) {
          case "close":
            if (!alertInstance) {
              alertInstance = new bootstrap.Alert(el);
            }
            alertInstance.close();
            break;
          case "dispose":
            if (alertInstance) {
              alertInstance.dispose();
            }
            break;
          default:
            // Return instance for chaining or initialize
            if (typeof action === "object") {
              alertInstance = new bootstrap.Alert(el, action);
            } else if (!alertInstance) {
              alertInstance = new bootstrap.Alert(el);
            }
            return alertInstance;
        }
      });
    };

    /**
     * Button Compatibility
     * Maps jQuery API to Bootstrap 5 Button
     */
    jQuery.fn.button = function (action) {
      return this.each(function () {
        const el = this;
        let buttonInstance = bootstrap.Button.getInstance(el);

        switch (action) {
          case "toggle":
            if (!buttonInstance) {
              buttonInstance = new bootstrap.Button(el);
            }
            buttonInstance.toggle();
            break;
          case "dispose":
            if (buttonInstance) {
              buttonInstance.dispose();
            }
            break;
          default:
            // Return instance for chaining or initialize
            if (typeof action === "object") {
              buttonInstance = new bootstrap.Button(el, action);
            } else if (!buttonInstance) {
              buttonInstance = new bootstrap.Button(el);
            }
            return buttonInstance;
        }
      });
    };
  }

  /**
   * Auto-initialization for data-bs-* attributes
   * This mimics Bootstrap 4's auto-initialization behavior
   */
  document.addEventListener("DOMContentLoaded", function () {
    // Initialize tooltips
    document
      .querySelectorAll('[data-bs-toggle="tooltip"]')
      .forEach(function (element) {
        new bootstrap.Tooltip(element);
      });

    // Initialize popovers
    document
      .querySelectorAll('[data-bs-toggle="popover"]')
      .forEach(function (element) {
        new bootstrap.Popover(element);
      });
  });

  console.log("Bootstrap 5 Compatibility Layer loaded successfully");
})();
