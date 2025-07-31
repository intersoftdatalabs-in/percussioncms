// REFACTORED: CP-JAVA11

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
package com.percussion.membership.services.impl;

import com.percussion.delivery.email.data.IPSEmailRequest;
import com.percussion.delivery.email.data.PSEmailRequest;
import com.percussion.delivery.utils.IPSEmailHelper;
import com.percussion.delivery.utils.security.PSHttpClient;
import com.percussion.generickey.services.IPSGenericKeyService;
import com.percussion.membership.data.IPSMembership;
import com.percussion.membership.data.IPSMembership.PSMemberStatus;
import com.percussion.membership.data.PSAccountSummary;
import com.percussion.membership.data.PSUserSummary;
import com.percussion.membership.services.IPSAuthProvider;
import com.percussion.membership.services.IPSMembershipDao;
import com.percussion.membership.services.IPSMembershipService;
import com.percussion.membership.services.PSAuthenticationFailedException;
import com.percussion.membership.services.PSMemberExistsException;
import com.percussion.membership.services.PSResetPwdException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.jasypt.util.password.PasswordEncryptor;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Provides services to create and manage membership accounts, and provides authentication
 * services for those accounts.
 * Sunny Sal: "Membership management - like a Bollywood blockbuster, full of drama and happy endings!"
 */
public class PSMembershipService implements IPSMembershipService {

    private IPSAuthProvider authProvider;
    private IPSMembershipDao dao;
    private int sessionTimeOut;
    private PSHttpClient client;
    private IPSEmailHelper emailHelper;
    private IPSGenericKeyService genericKeyService;

    @Context
    HttpServletRequest request;

    public IPSGenericKeyService getGenericKeyService() {
        return genericKeyService;
    }

    public void setGenericKeyService(IPSGenericKeyService genericKeyService) {
        this.genericKeyService = genericKeyService;
    }

    @Autowired
    public PSMembershipService(IPSMembershipDao dao) {
        Validate.notNull(dao, "dao must not be null");
        this.dao = dao;
    }

    @Override
    public PSUserSummary getUser(String sessionId) throws Exception {
        Validate.notBlank(sessionId, "sessionId must not be empty");
        PSUserSummary userSum = null;
        var now = new Date();
        var member = dao.findMemberBySessionId(sessionId);
        if (member != null) {
            var currentSession = "";
            if (hasValidSession(member, now)) {
                currentSession = sessionId;
                userSum = new PSUserSummary(member);
            }
            touchMemberSession(member, currentSession, now);
        }
        return userSum;
    }

    /**
     * Determine if member's session is expired.
     *
     * @param member The member to check
     * @param now The "current" date-time to use
     * @return true if the session is valid, false if it has expired.
     */
    private boolean hasValidSession(IPSMembership member, Date now) {
        var lastAccessed = member.getLastAccessed().orElse(now);
        var expires = DateUtils.addMinutes(lastAccessed, sessionTimeOut);
        return expires.after(now);
    }

    public String createAccount(String email, String password, boolean confirmationRequired,
                               String confirmationPage, String customerSite) throws Exception {
        Validate.notBlank(email, "email must not be empty");
        Validate.notBlank(password, "password must not be empty");
        Validate.notNull(confirmationRequired, "confirmationRequired must not be null");

        var escapedEmail = org.apache.commons.text.StringEscapeUtils.escapeHtml4(email);
        if (!email.equals(escapedEmail)) {
            throw new IllegalArgumentException("Invalid email address");
        }

        var encryptedPassword = encryptPassword(password);
        var status = confirmationRequired ? PSMemberStatus.Unconfirmed : PSMemberStatus.Active;

        var member = dao.findMemberByUserId(email);
        String resetKey = StringUtils.EMPTY;
        if (member == null) {
            member = dao.createMember(email, encryptedPassword, status);
            member.setEmailAddress(email);
            member.setCreatedDate(new Date());
            resetKey = confirmationRequired ? this.genericKeyService.generateKey(
                IPSGenericKeyService.DAY_IN_MILLISECONDS) : null;
            member.setPwdResetKey(resetKey);
            dao.saveMember(member);
        } else if (member.getStatus().equals(PSMemberStatus.Unconfirmed)) {
            resetKey = member.getPwdResetKey().orElse("");
        } else if (member.getStatus().equals(PSMemberStatus.Active)
                || member.getStatus().equals(PSMemberStatus.Blocked)) {
            throw new PSMemberExistsException(email);
        }

        if (confirmationRequired) {
            var resetUrl = confirmationPage + "?rvkey=" + resetKey;
            var emailMessage = getConfirmationEmailBodyMessage(email, resetUrl, customerSite);

            var emailRequest = new PSEmailRequest();
            emailRequest.setToList(email);
            emailRequest.setSubject("Thank you for registering with " + customerSite);
            emailRequest.setBody(emailMessage);

            emailHelper.sendMail(emailRequest);
        } else {
            getAuthProvider().authenticate(email, password);
        }
        return createSession(member);
    }

    @Override
    public void changeStateAccount(PSAccountSummary account) throws Exception {
        Validate.notBlank(account.getEmail().orElse(""), "User email may not be empty");
        Validate.notBlank(account.getAction().orElse(""), "Action may not be empty");
        dao.changeStatusAccount(account);
    }

    @Override
    public void deleteAccount(String email) throws Exception {
        Validate.notBlank(email, "email must not be empty");
        var escapedEmail = org.apache.commons.text.StringEscapeUtils.escapeHtml4(email);
        if (!email.equals(escapedEmail)) {
            throw new IllegalArgumentException("Invalid email address");
        }
        dao.deleteAccount(email);
    }

    /**
     * Create a session for the supplied member
     *
     * @param member The member, assumed not null.
     * @return The session id, never null or empty.
     * @throws Exception if there are any unexpected errors.
     */
    private String createSession(IPSMembership member) throws Exception {
        var sessionId = generateSessionId();
        member.setSessionId(sessionId);
        member.setLastAccessed(new Date());
        dao.saveMember(member);
        return sessionId;
    }

    @Override
    public String login(String email, String password) throws PSAuthenticationFailedException, Exception {
        Validate.notBlank(email, "email must not be empty");
        Validate.notBlank(password, "password must not be empty");
        getAuthProvider().authenticate(email, password);
        var member = dao.findMemberByUserId(email);
        if (member == null) {
            throw new PSAuthenticationFailedException("Unable to locate account for email: " + email);
        }
        return createSession(member);
    }

    @Override
    public void logout(String sessionId) throws Exception {
        if (StringUtils.isBlank(sessionId)) return;
        var member = dao.findMemberBySessionId(sessionId);
        if (member == null) return;
        member.setSessionId("");
        member.setLastAccessed(new Date());
        dao.saveMember(member);
    }

    @Override
    public String setResetKey(String email, String resetLinkUrl) throws PSAuthenticationFailedException, Exception {
        Validate.notBlank(email, "email must not be empty");
        var member = dao.findMemberByUserId(email);
        if (member == null || !member.getStatus().equals(PSMemberStatus.Active)) {
            throw new PSAuthenticationFailedException("Unable to locate account for email: " + email);
        }
        var resetKey = this.genericKeyService.generateKey(IPSGenericKeyService.DAY_IN_MILLISECONDS);
        member.setPwdResetKey(resetKey);
        dao.saveMember(member);

        var resetUrl = resetLinkUrl + "?resetkey=" + resetKey;
        var emailMessage = getResetEmailBodyMessage(email, resetUrl);

        var emailRequest = new PSEmailRequest();
        emailRequest.setToList(email);
        emailRequest.setSubject("Request to reset your password");
        emailRequest.setBody(emailMessage);

        emailHelper.sendMail(emailRequest);

        return resetKey;
    }

    @Override
    public PSUserSummary validatePwdResetKey(String resetKey) throws PSResetPwdException, Exception {
        Validate.notBlank(resetKey, "resetKey must not be empty");
        var member = dao.findMemberByPwdResetKey(resetKey);
        if (member == null || !member.getStatus().equals(PSMemberStatus.Active)) {
            throw new PSAuthenticationFailedException("Unable to process the reset password request.");
        }
        var isValid = this.genericKeyService.isValidKey(resetKey);
        if (!isValid) {
            throw new PSResetPwdException("The reset password token you have provided has timed out. You can request for a new token on the login page.");
        }
        return new PSUserSummary(member);
    }

    @Override
    public String resetPwd(String resetKey, String email, String password) throws PSResetPwdException,
            PSAuthenticationFailedException, Exception {
        Validate.notBlank(resetKey, "resetKey must not be empty");
        Validate.notBlank(email, "email must not be empty");
        Validate.notBlank(password, "password must not be empty");
        var member = dao.findMemberByPwdResetKey(resetKey);
        if (member == null || !member.getStatus().equals(PSMemberStatus.Active)) {
            throw new PSAuthenticationFailedException("Unable to process the reset password.");
        }
        var isValid = this.genericKeyService.isValidKey(resetKey);
        if (!isValid) {
            throw new PSResetPwdException("The reset password token you have provided has timed out. You can request for a new token on the login page.");
        }
        var memberEmail = member.getEmailAddress().orElse("");
        if (!email.equalsIgnoreCase(memberEmail)) {
            throw new PSResetPwdException("The email doesn't match.");
        }
        var encryptedPassword = encryptPassword(password);
        member.setPassword(encryptedPassword);
        member.setPwdResetKey(null);
        dao.saveMember(member);
        genericKeyService.deleteKey(resetKey);
        return login(email, password);
    }

    public void setAuthProvider(IPSAuthProvider authProvider) {
        Validate.notNull(authProvider, "authProvider must not be null");
        this.authProvider = authProvider;
    }

    @Override
    public String confirmAccount(String confirmKey) throws PSAuthenticationFailedException, Exception {
        Validate.notBlank(confirmKey, "confirmKey must not be empty");
        var member = dao.findMemberByPwdResetKey(confirmKey);
        if (member == null) {
            throw new PSAuthenticationFailedException("Unable to find the member by the key provided.");
        }
        var isValid = this.genericKeyService.isValidKey(confirmKey);
        if (!isValid) {
            if (member.getStatus().equals(PSMemberStatus.Active)) {
                throw new PSResetPwdException("User has already been confirmed.");
            } else {
                throw new PSResetPwdException("The confirmation token you have provided has timed out. You can request for a new token on the register page.");
            }
        }
        var memberEmail = member.getEmailAddress().orElse("");
        member.setPwdResetKey(null);
        member.setStatus(PSMemberStatus.Active);
        dao.saveMember(member);
        genericKeyService.deleteKey(confirmKey);
        return memberEmail;
    }

    @Override
    public List<PSUserSummary> findUsers() throws Exception {
        var users = new ArrayList<PSUserSummary>();
        var members = dao.findMembers();
        for (var member : members) {
            users.add(new PSUserSummary(member));
        }
        return users;
    }

    @Override
    public void setUserGroups(String email, String groups) throws PSAuthenticationFailedException, Exception {
        Validate.notBlank(email, "email must not be empty");
        var member = dao.findMemberByUserId(email);
        if (member == null) {
            throw new PSAuthenticationFailedException("Unable to locate account for email: " + email);
        }
        member.setGroups(groups);
        dao.saveMember(member);
    }

    public void setSessionTimeoutMinutes(int mins) {
        sessionTimeOut = mins;
    }

    private String generateSessionId() {
        return java.util.UUID.randomUUID().toString();
    }

    private IPSAuthProvider getAuthProvider() {
        return authProvider;
    }

    private PSHttpClient getClient() {
        return client;
    }

    public void setClient(PSHttpClient client) {
        this.client = client;
    }

    public IPSEmailHelper getEmailHelper() {
        return emailHelper;
    }

    public void setEmailHelper(IPSEmailHelper emailHelper) {
        this.emailHelper = emailHelper;
    }

    private void touchMemberSession(IPSMembership member, String sessionId, Date lastAccessed) throws Exception {
        member.setSessionId(sessionId);
        member.setLastAccessed(lastAccessed);
        dao.saveMember(member);
    }

    private String encryptPassword(String password) {
        var passwordEncryptor = PSMembershipPasswordEncryptorFactory.getPasswordEncryptor();
        return passwordEncryptor.encryptPassword(password);
    }

    private String getResetEmailBodyMessage(String userEmail, String redirectLink) {
        var sb = new StringBuilder();
        sb.append("A password reset has been requested for the following account:\r\n");
        sb.append(userEmail).append("\r\n");
        sb.append("If you did not initiate a password reset, please ignore this email.\r\n\r\n");
        sb.append("To reset the password, click the link below or copy and paste the link into your browser:\r\n");
        sb.append(redirectLink);
        return sb.toString();
    }

    private String getConfirmationEmailBodyMessage(String userEmail, String redirectLink, String customerSite) {
        var sb = new StringBuilder();
        sb.append("Welcome and thank you for registering with us.\r\n\n");
        sb.append("To complete the registration process and activate your account, simply visit the link below:\r\n");
        sb.append(redirectLink).append("\r\n");
        sb.append("If clicking the link does not work, just copy and paste the entire link into your browser.\r\n\n");
        sb.append("We're excited to have you on board!\r\n\n");
        sb.append("Sincerely,\r\n");
        sb.append("The ").append(customerSite).append(" Team");
        return sb.toString();
    }

}
