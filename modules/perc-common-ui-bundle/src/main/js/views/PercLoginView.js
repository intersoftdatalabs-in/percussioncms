/*
 * Copyright 1999-2023 Percussion Software, Inc.
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

(function ($) {
  $(document).ready(function () {
    $.PercLoginView.init();
  });

  $.PercLoginView = {
    init: init,
    // Behavior seams extracted for issue #7257 (cognitive-complexity slice);
    // covered directly by PercLoginViewRefactorTest.
    readLoginOptions: readLoginOptions,
    buildLoginCredentials: buildLoginCredentials,
    resolveLoginRedirect: resolveLoginRedirect,
    resolveRedirectTarget: resolveRedirectTarget,
    handleLoginResponse: handleLoginResponse,
  };

  /**
   * Initialize and configure each instance of login widget in the page.
   */
  function init() {
    // Append the login links the corresponding loginRedirect parameter
    $(".percLogin").each(function () {
      var currentLoginWidget = $(this);
      // Retrieve sessionId's value. If it exists and is not undefined, check if the session
      // is still active by retrieving the user info using the service getUser
      var sessionId = $.cookie("perc_membership_session_id");
      if (null != sessionId && "undefined" !== typeof sessionId) {
        //If the user is logged in, show the welcome message
        $.PercMembershipService.getUser(sessionId, function (status, data) {
          if (
            status === $.PercServiceUtils.STATUS_SUCCESS &&
            null != data.userSummary &&
            "" !== data.userSummary.email
          ) {
            var loginMessage = JSON.parse(
              currentLoginWidget.attr("data"),
            ).loggedInMessage;
            var showUsername = JSON.parse(
              currentLoginWidget.attr("data"),
            ).showUsername;
            if (
              null !== showUsername &&
              "undefined" !== typeof showUsername &&
              "showUsername" === showUsername
            ) {
              loginMessage += data.userSummary.email;
            }

            // Replace .login-message-box contents with logged in message
            currentLoginWidget.find(".login-contents-box").html(loginMessage);
            // Select the form and remove it from the DOM (only applied in login page)
            $(".perc-login-form").remove();
          }
        });
      }

      // If not in login page and not logged in, append loginRedicrect URL param to links
      $(".perc-login-page-link").each(function () {
        var self = $(this);
        var url = self.attr("href");
        self.attr(
          "href",
          url + "?loginRedirect=" + encodeURIComponent(window.location),
        );
      });

      // Configure the login form handler. The form is rendered only in render as form mode.
      $(".perc-login-form").each(function () {
        var self = $(this);
        self.on("submit", handleSubmitForm);
      });
    });
  }

  /**
   * Callback bound to submit event of login widget.
   * @param e
   * @return boolean false Because we are preventing a form submission
   */
  function handleSubmitForm(e) {
    // Prevent event default behavior
    e.preventDefault();
    var form = $(this);
    if (!form.validate().form()) {
      return false;
    }
    // Retrieve options object and create loginObj passed to login service
    var currentLoginWidget = $(form.parents(".percLogin"));
    var options = readLoginOptions(currentLoginWidget);
    var loginObj = buildLoginCredentials(form);

    $.PercMembershipService.login(loginObj, function (status, data) {
      handleLoginResponse(form, options, status, data);
    });

    return false;
  }

  /**
   * Parse the widget options object from the login widget markup.
   * @param currentLoginWidget the `.percLogin` widget element
   * @return the parsed options object
   */
  function readLoginOptions(currentLoginWidget) {
    return JSON.parse(currentLoginWidget.attr("data"));
  }

  /**
   * Build the credentials object passed to the login service.
   * @param form the submitted login form element
   * @return the login credentials object
   */
  function buildLoginCredentials(form) {
    return {
      email: form.find("input[name=perc-login-email-field]").val(),
      password: form.find("input[name=perc-login-password-field]").val(),
    };
  }

  /**
   * Overlay the loginRedirect URL parameter onto the widget options.
   * @param options the widget options object (mutated in place)
   * @return the same options object
   */
  function resolveLoginRedirect(options) {
    // Get the loginRedirect parameter from the URL parameter
    var urlstring = $.deparam.querystring();
    if ("undefined" === typeof urlstring.loginRedirect) {
      return options;
    }
    options.loginRedirect = urlstring.loginRedirect;
    return options;
  }

  /**
   * Select the post-login redirect target.
   * @param options the widget options object
   * @return the redirect URL, or the current page when no redirect is set
   */
  function resolveRedirectTarget(options) {
    // If there's a redirect parameter, redirect to that page, else refresh current page.
    return options.loginRedirect
      ? options.loginRedirect
      : window.location.toString();
  }

  /**
   * Show a login error inline to the user.
   * @param form the submitted login form element
   * @param message the message to display
   */
  function showLoginError(form, message) {
    form.parent().find(".perc-login-error-message").show().html(message);
  }

  /**
   * Handle the login-service callback: transport failure, service-level
   * failure (e.g. wrong password), or the success path.
   * @param form the submitted login form element
   * @param options the widget options object
   * @param status the transport status of the login request
   * @param rawData the raw login response payload
   */
  function handleLoginResponse(form, options, status, rawData) {
    var data = JSON.parse(rawData);
    resolveLoginRedirect(options);
    if (status !== $.PercServiceUtils.STATUS_SUCCESS) {
      // There was an error with the AJAX login request
      showLoginError(form, "There was an unexpected error.");
      return;
    }
    if (
      data.status.toUpperCase() !==
      $.PercServiceUtils.STATUS_SUCCESS.toUpperCase()
    ) {
      // The AJAX request was successful but there was an error in the service
      showServiceError(form, data.message);
      return;
    }
    handleLoginSuccess(form, options, data);
  }

  /**
   * Handle a service-level login failure (e.g. wrong password).
   * @param form the submitted login form element
   * @param errorMessage the service error message, when present
   */
  function showServiceError(form, errorMessage) {
    // The AJAX request was successful but there was an error in the service
    // Show the error inline to the user.
    if (null === errorMessage || "undefined" === typeof errorMessage) {
      return;
    }
    showLoginError(form, errorMessage);
  }

  /**
   * Handle a successful login: persist the session and redirect.
   * @param form the submitted login form element
   * @param options the widget options object
   * @param data the parsed login response payload
   */
  function handleLoginSuccess(form, options, data) {
    // If successful login, store the ID session in a cookie and redirect to corresponding page
    // Session cookie (no expires): the old rewrite ignored the
    // expires:"null" marker, while the npm jquery.cookie 1.4.1
    // dist (issue #7177) would throw on its missing toUTCString.
    $.cookie("perc_membership_session_id", data.sessionId, {
      path: "/",
    });

    form.parent().find(".perc-login-error-message").hide();
    // Redirect to corresponding page
    window.location = resolveRedirectTarget(options);
  }
})(jQuery);
