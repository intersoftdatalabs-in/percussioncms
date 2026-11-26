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

/**
 * PercRegistrationView.js
 *
 * @author Luis Mendez
 * View for RegistrationWidget. Contains functionality to handle the registration and redirect behaviors
 * (*) Dependencies
 * /jslib/jquery.cookie.js
 * /jslib/profiles/3x/jquery/plugins/jquery-validation/jquery.validate.js
 */
(function ($) {
  $(document).ready(function () {
    $.PercRegistrationView.init();
  });
  var regMode = "";
  $.PercRegistrationView = {
    init: init,
    // Behavior seams extracted for issue #7276 (cognitive-complexity slice);
    // covered directly by PercRegistrationViewRefactorTest.
    readRegistrationWidgetOptions: readRegistrationWidgetOptions,
    readRegistrationFormOptions: readRegistrationFormOptions,
    buildRegisterObject: buildRegisterObject,
    resolveRegistrationRedirectUrl: resolveRegistrationRedirectUrl,
    redirectAfterRegistration: redirectAfterRegistration,
    redirectToConfirmationPage: redirectToConfirmationPage,
    handleRegistrationResponse: handleRegistrationResponse,
    handleRegistrationSuccess: handleRegistrationSuccess,
  };
  /**
   * Initialize and configure each instance of registration widget in the page.
   */
  function init() {
    var urlstring = $.deparam.querystring();

    // Verify that resetkey is valid when Registration form is rendered in Reset password mode
    if (
      "undefined" !== typeof urlstring.resetkey &&
      0 < $("#perc-password-reset").length
    ) {
      var resetKey = urlstring.resetkey;
      $.PercMembershipService.validateResetPwKey(
        resetKey,
        function (status, data) {
          data = $.PercServiceUtils.toJSON(data);
          if ("SUCCESS" === data.status) {
            var userEmail = data.userSummary.email;
            var resetForm = $("#perc-password-reset");
            resetForm.show();
            resetForm.find("#perc-registration-email-field").val(userEmail);
            resetForm.data("key", resetKey);
          } else {
            console.log(data.message);
          }
        },
      );
    }

    //Show the registration form if there is no rvkey param in the url if it exists then confirm the user and redirect them to reg success page.
    if (
      "undefined" !== typeof urlstring.rvkey &&
      0 < $(".perc-reg-confirmation-mode").length
    ) {
      $(".perc-reg-confirmation-mode").show();
      var rvKey = urlstring.rvkey;
      $.PercMembershipService.confirmRegistration(
        rvKey,
        function (status, data) {
          data = $.PercServiceUtils.toJSON(data);
          if ("SUCCESS" === data.status) {
            var redirectUrl = $("#perc-confirmation-page").val();
            if (!redirectUrl || "" === redirectUrl) {
              redirectUrl = "/";
            }
            // CodeQL js/xss-through-dom (alert #988): redirectUrl is a form
            // field value (attacker-controllable via DOM); sanitize before
            // navigation so javascript:/data: schemes cannot execute.
            window.location.href =
              $.PercServiceUtils.sanitizeUrlForHref(redirectUrl);
          } else {
            $(".perc-reg-confirmation-message").text(data.message);
          }
        },
      );
    } else if ($(".perc-registration-mode").length) {
      $(".perc-registration-mode").show();
    }

    // If user is logged-in remove the password reset form and password reset request form from DOM
    var sessionId = $.cookie("perc_membership_session_id");
    if (null !== sessionId && "undefined" !== typeof sessionId) {
      //If the user is logged in, show the welcome message
      $.PercMembershipService.getUser(sessionId, function (status, data) {
        if (status === $.PercServiceUtils.STATUS_SUCCESS && "" !== data.email) {
          $("#perc-password-request, #perc-password-reset").remove();
        }
      });
    }

    $("#perc-registration-form, #perc-registration-cform").each(function () {
      var self = $(this);
      var options;

      // If data property does not exists, create it and add the paramUrlRedirect element
      if (self.parent(".percRegistration").attr("data") !== undefined) {
        options = JSON.parse(self.parent(".percRegistration").attr("data"));
      } else {
        options = {};
      }

      if ($("#perc-registration-cform").length) {
        options.verReq = true;
      }
      self.data("options", options);
      self.on("submit", handleSubmitForm);
    });

    // Bind the submit button of password reset request form
    $("#perc-password-request").each(function () {
      var self = $(this);
      self.submit(handleResetPwdRequest);
    });

    // Bind the submit button of password reset form
    $("#perc-password-reset").each(function () {
      var self = $(this);
      self.submit(handleResetPassword);
    });
  }

  /**
   * Callback bound to submit event of reset password request form.
   * @param e
   * @return boolean false Because we are preventing a form submission
   */

  function handleResetPwdRequest(e) {
    e.preventDefault();
    var self = $(this);
    if (self.validate().form()) {
      var email = $("#perc-registration-email-field").val();
      var redirectUrl = $("input[name='perc_password_reset_page']").val(); // mkd-code-review[secrets.heuristic]: adjudicated FP (issue #7275, precedent #7257/#6777) — reset-page input name string, not credential material.
      $.PercMembershipService.resetPwRequest(
        email,
        redirectUrl,
        function (status, data) {
          if (status === $.PercServiceUtils.STATUS_SUCCESS) {
            if ("AUTH_FAILED" === data.status) {
              $(".perc-reg-error-message").show().html(data.message);
            } else {
              //On Reset Request Successful  show the confirmation message.
              $(".perc-confirmation-message").show();
              $("#perc-password-request").remove();
              $(".perc-reg-error-message").hide();
            }
          } else {
            alert(data.message);
          }
        },
      );
    }
    return false;
  }

  /**
   * Callback bound to submit event of update password form.
   * @param e
   * @return boolean false Because we are preventing a form submission
   */
  function handleResetPassword(e) {
    e.preventDefault();
    var self = $(this);
    if (self.validate().form()) {
      var email = $("#perc-registration-email-field").val();
      var password = $("#perc-registration-password-field").val(); // mkd-code-review[secrets.heuristic]: adjudicated FP (issue #7275, precedent #7257/#6777) — client-side registration form field read, no secret in source.
      var resetKey = self.data("key");
      $.PercMembershipService.resetPassword(
        email,
        password,
        resetKey,
        function (status, data) {
          if (status === $.PercServiceUtils.STATUS_SUCCESS) {
            // If successful login, store the ID session in a cookie and redirect to corresponding page
            // Session cookie (no expires): the old rewrite ignored the
            // expires:"null" marker, while the npm jquery.cookie 1.4.1
            // dist (issue #7177) would throw on its missing toUTCString.
            $.cookie("perc_membership_session_id", data.sessionId, {
              path: "/",
            });
            // After successfully resetting the password - take user to home page fo website.
            window.location.replace("/");
          } else {
            alert(data.message);
          }
        },
      );
    }
    return false;
  }

  /**
   * Read the registration widget options from the wrapping
   * {@code .percRegistration} element.
   * @param form the submitted registration form element
   * @return the parsed widget options object
   */
  function readRegistrationWidgetOptions(form) {
    return JSON.parse(
      form.parent().parent(".percRegistration").attr("data"),
    );
  }

  /**
   * Read the per-form options stored on the element during {@code init}.
   * @param form the submitted registration form element
   * @return the form options object
   */
  function readRegistrationFormOptions(form) {
    return form.data("options");
  }

  /**
   * Build the registration payload from the submitted form inputs.
   * @param form the submitted registration form element
   * @return the object passed to the registration service
   */
  function buildRegisterObject(form) {
    return {
      email: form.find("#perc-registration-email-field").val(),
      password: form.find("#perc-registration-password-field").val(), // mkd-code-review[secrets.heuristic]: adjudicated FP (issue #7276, precedent #7257/#7275) — client-side registration form field read, no secret in source.
      confirmationRequired: "perc-registration-cform" === form.attr("name"),
      confirmationPage: window.location.href,
    };
  }

  /**
   * Resolve the post-registration redirect target from the URL parameters.
   * @param urlstring the parsed query string object
   * @return the registrationRedirect value, or "" when absent
   */
  function resolveRegistrationRedirectUrl(urlstring) {
    if ("undefined" !== typeof urlstring.registrationRedirect) {
      return urlstring.registrationRedirect;
    }
    return "";
  }

  /**
   * Redirect after a direct (no verification) registration: fall back to the
   * widget home page when no registrationRedirect parameter is present,
   * otherwise strip that parameter before navigating.
   * @param redirectURL the registrationRedirect value ("" when absent)
   * @param widgetOptions the registration widget options object
   * @param urlstring the parsed query string object (mutated to drop the
   * consumed registrationRedirect parameter)
   * @return the URL assigned to {@code window.location}
   */
  function redirectAfterRegistration(redirectURL, widgetOptions, urlstring) {
    var target = redirectURL;
    // Confirmation page and registrationRedirect are not defined,
    // redirect to Home
    if ("" === target) {
      target = widgetOptions.homePageLink;
    } else {
      delete urlstring.registrationRedirect;
    }

    var params = $.param.querystring("", urlstring);
    var destination = target;
    if (1 < params.length) {
      destination = target + params;
    }
    window.location = destination;
    return destination;
  }

  /**
   * Redirect to the confirmation page, forwarding the current URL parameters.
   * @param confirmationPage the configured confirmation page URL
   * @return the sanitized URL assigned to {@code window.location}
   */
  function redirectToConfirmationPage(confirmationPage) {
    var params = "";
    if ($.param.querystring()) {
      params = "?" + $.param.querystring();
    }
    // CodeQL js/xss-through-dom (alert #989): confirmation_page is
    // user-supplied via form; sanitize before navigation.
    var destination = $.PercServiceUtils.sanitizeUrlForHref(
      confirmationPage + params,
    );
    window.location = destination;
    return destination;
  }

  /**
   * Show a registration error inline and remain on the same page.
   * @param message the message to display
   */
  function showRegistrationError(message) {
    $(".perc-reg-error-message").show().html(message);
  }

  /**
   * Show the email-verification confirmation in place of the form.
   */
  function showRegistrationVerificationSent() {
    $(".perc-registration-mode").hide();
    $(".perc-reg-confirmation-message").html(
      "<div>Thank you for registering with us.</div><div>Please check your email and confirm your registration to activate your account.</div>",
    );
    $(".perc-reg-confirmation-mode").show();
  }

  /**
   * Handle a successful registration: either show the verification notice
   * (no session is established until the email is confirmed) or persist the
   * session cookie and redirect for direct registrations.
   * @param form the submitted registration form element
   * @param options the per-form options object
   * @param widgetOptions the registration widget options object
   * @param data the parsed registration response payload
   * @param urlstring the parsed query string object
   */
  function handleRegistrationSuccess(
    form,
    options,
    widgetOptions,
    data,
    urlstring,
  ) {
    if (options.verReq) {
      showRegistrationVerificationSent();
      return;
    }

    // If successful registration, store the ID session in a cookie
    // Session cookie (no expires): see the expires:"null" note
    // above (issue #7177).
    $.cookie("perc_membership_session_id", data.sessionId, {
      path: "/",
    });

    // If there is no confirmation page redirect to registrationRedirect (after
    // removing the corresponding URL parameter)
    var confirmationPage = form.find("#perc-confirmation-page").val();
    if ("" === confirmationPage) {
      redirectAfterRegistration(
        resolveRegistrationRedirectUrl(urlstring),
        widgetOptions,
        urlstring,
      );
      return;
    }
    // Redirect to the confirmation page and pass all the current URL parameters
    redirectToConfirmationPage(confirmationPage);
  }

  /**
   * Handle the registration-service callback: transport failure,
   * service-level failure, or the success path.
   * @param form the submitted registration form element
   * @param options the per-form options object
   * @param widgetOptions the registration widget options object
   * @param status the transport status of the registration request
   * @param rawData the raw registration response payload
   */
  function handleRegistrationResponse(
    form,
    options,
    widgetOptions,
    status,
    rawData,
  ) {
    var data = $.PercServiceUtils.toJSON(rawData);
    if (status !== $.PercServiceUtils.STATUS_SUCCESS) {
      // If there was an error with the AJAX registration request
      showRegistrationError("There was an unexpected error.");
      return;
    }
    // Check for registration service REST service result and redirect
    if (
      data.status.toUpperCase() !==
      $.PercServiceUtils.STATUS_SUCCESS.toUpperCase()
    ) {
      // If the registration wasn't succesful show the error message inline
      // and remain in the same page.
      showRegistrationError(data.message);
      return;
    }
    // Retrieve where the user came from (URL param) and Confirmation page
    handleRegistrationSuccess(
      form,
      options,
      widgetOptions,
      data,
      $.deparam.querystring(),
    );
  }

  /**
   * Callback bound to submit event of registration widget.
   * @param e event
   * @return boolean false Because we are preventing a form submission
   */
  function handleSubmitForm(e) {
    e.preventDefault();
    var form = $(this);
    if (!form.validate().form()) {
      return false;
    }
    // Retrieve widget and form options
    var widgetOptions = readRegistrationWidgetOptions(form);
    var options = readRegistrationFormOptions(form);
    var registerObj = buildRegisterObject(form);

    //registraionPageUrl:options.verReq
    $.PercMembershipService.register(registerObj, function (status, data) {
      handleRegistrationResponse(form, options, widgetOptions, status, data);
    });
    return false;
  }
})(jQuery);
