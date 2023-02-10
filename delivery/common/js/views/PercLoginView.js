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

(function($) {
    $(document).ready(function() {
        $.PercLoginView.init();
    });
    
    $.PercLoginView = {
        init : init
    };
    
    /**
     * Initialize and configure each instance of login widget in the page.
     */
    function init() {
        // Append the login links the corresponding loginRedirect parameter
        $('.percLogin').each(function() {
            var currentLoginWidget = $(this);
            // Retrieve sessionId's value. If it exists and is not undefined, check if the session
            // is still active by retrieving the user info using the service getUser
            var sessionId = $.cookie('perc_membership_session_id');
            if (null != sessionId && 'undefined' !== typeof (sessionId)) {
                //If the user is logged in, show the welcome message
                $.PercMembershipService.getUser(sessionId, function(status, data) {
                    if (status === $.PercServiceUtils.STATUS_SUCCESS && null != data.userSummary && '' !== data.userSummary.email) {
                        var loginMessage = (JSON.parse(currentLoginWidget.attr("data"))).loggedInMessage;
                        var showUsername = (JSON.parse(currentLoginWidget.attr("data"))).showUsername;
                        if(null !== showUsername && 'undefined' !== typeof (showUsername) && 'showUsername' === showUsername) {
                            loginMessage += data.userSummary.email;   
                        }
    
                        // Replace .login-message-box contents with logged in message
                        currentLoginWidget.find('.login-contents-box').html(loginMessage);
                        // Select the form and remove it from the DOM (only applied in login page)
                        $('.perc-login-form').remove();
                    }
                });         
            }
            
            // If not in login page and not logged in, append loginRedicrect URL param to links
            $('.perc-login-page-link').each(function() {
                var self = $(this);
                var url  = self.attr('href');
                self.attr('href', url + '?loginRedirect=' + encodeURIComponent(window.location));
            });
            
            // Configure the login form handler. The form is rendered only in render as form mode.
            $('.perc-login-form').each(function() {
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
        var self = $(this);
        var currentLoginWidget = $(self.parents(".percLogin"));
        if (self.validate().form()) {
            // Retrieve options object and create loginObj passed to login service
            var options = JSON.parse(currentLoginWidget.attr('data'));
            var loginObj = {
                email    : self.find("input[name=perc-login-email-field]").val(),
                password : self.find("input[name=perc-login-password-field]").val()
            };
            
            $.PercMembershipService.login(loginObj, function(status, data) {
                // Get the loginRedirect parameter from the URL parameter
                data = JSON.parse(data)
                var redirectPage = '';
                var urlstring = $.deparam.querystring();
                if ("undefined" !== typeof (urlstring.loginRedirect)) {
                    options.loginRedirect = urlstring.loginRedirect;
                }
                
                if (status === $.PercServiceUtils.STATUS_SUCCESS) {
                    if (data.status.toUpperCase() === $.PercServiceUtils.STATUS_SUCCESS.toUpperCase()) {
                        // If successful login, store the ID session in a cookie and redirect to corresponding page
                        $.cookie('perc_membership_session_id', data.sessionId, { "path": "/", "expires": "null" });
                        
                        self.parent().find(".perc-login-error-message").hide();
                        // If there's a redirect parameter, redirect to that page, else refresh current page.
                        redirectPage = (options.loginRedirect) ? options.loginRedirect : window.location.toString();
                        // Redirect to corresponding page
                        window.location = redirectPage;
                    }
                    else {
                    	// The AJAX request was successful but there was an error in the service (e.g. wrong password)
                    	// Show the error inline to the user.
                    	var errorMessage = data.message;
                    	if (null !== errorMessage && 'undefined' !== typeof (errorMessage)) {
                    		self.parent().find(".perc-login-error-message").show().html(errorMessage);
                        }
                    }
                }
                else {
                    // There was an error with the AJAX login request
                	self.parent().find(".perc-login-error-message").show().html("There was an unexpected error.");
                }
            });
        }

        return false;
    }    
})(jQuery);
