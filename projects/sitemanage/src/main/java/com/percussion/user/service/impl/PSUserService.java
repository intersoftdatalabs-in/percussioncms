/*
 * Copyright 1999-2025 Percussion Software, Inc.
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
package com.percussion.user.service.impl;

import static com.percussion.role.service.IPSRoleService.ADMINISTRATOR_ROLE;
import static com.percussion.role.service.IPSRoleService.DESIGNER_ROLE;
import static com.percussion.utils.request.PSRequestInfoBase.KEY_PSREQUEST;
import static com.percussion.utils.request.PSRequestInfoBase.initRequestInfo;
import static com.percussion.utils.request.PSRequestInfoBase.resetRequestInfo;
import static com.percussion.utils.request.PSRequestInfoBase.setRequestInfo;
import static com.percussion.webservices.PSWebserviceUtils.getItemSummary;
import static com.percussion.webservices.PSWebserviceUtils.setUserName;
import static java.util.Arrays.asList;
import static org.apache.commons.collections.CollectionUtils.containsAny;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.Validate.isTrue;
import static org.apache.commons.lang3.Validate.notNull;

import com.intsof.percussioncms.auditlog.AuditOutcome;
import com.percussion.cms.IPSConstants;
import com.percussion.cms.PSAuthenticateUserUtils;
import com.percussion.services.audit.PSSystemAuditLogger;
import com.percussion.cms.objectstore.PSComponentSummary;
import com.percussion.cms.objectstore.PSFolder;
import com.percussion.design.objectstore.PSAttribute;
import com.percussion.design.objectstore.PSAttributeList;
import com.percussion.design.objectstore.PSSubject;
import com.percussion.legacy.security.deprecated.PSLegacyEncrypter;
import com.percussion.metadata.data.PSMetadata;
import com.percussion.metadata.service.IPSMetadataService;
import com.percussion.pathmanagement.service.impl.PSAssetPathItemService;
import com.percussion.role.service.IPSRoleService;
import com.percussion.role.service.impl.PSRoleService;
import com.percussion.security.IPSPasswordFilter;
import com.percussion.security.IPSTypedPrincipal;
import com.percussion.security.IPSTypedPrincipal.PrincipalTypes;
import com.percussion.security.PSEncryptionException;
import com.percussion.security.PSPasswordHandler;
import com.percussion.security.PSSecurityCatalogException;
import com.percussion.security.PSSecurityException;
import com.percussion.security.PSSecurityProvider;
import com.percussion.security.PSThreadRequestUtils;
import com.percussion.security.SecureStringUtils;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.server.PSRequest;
import com.percussion.server.PSServer;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.PSGuidUtils;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.services.notification.IPSNotificationListener;
import com.percussion.services.notification.IPSNotificationService;
import com.percussion.services.notification.PSNotificationEvent;
import com.percussion.services.notification.PSNotificationEvent.EventType;
import com.percussion.services.notification.PSNotificationServiceLocator;
import com.percussion.services.security.IPSBackEndRoleMgr;
import com.percussion.services.security.IPSRoleMgr;
import com.percussion.services.security.PSJaasUtils;
import com.percussion.services.system.PSAssignmentTypeHelper;
import com.percussion.services.workflow.IPSWorkflowService;
import com.percussion.services.workflow.data.PSAssignmentTypeEnum;
import com.percussion.services.workflow.data.PSState;
import com.percussion.services.workflow.data.PSWorkflow;
import com.percussion.servlets.PSSecurityFilter;
import com.percussion.share.dao.IPSGenericDao;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.share.service.IPSSystemProperties;
import com.percussion.share.service.PSCollectionUtils;
import com.percussion.share.service.exception.PSBeanValidationException;
import com.percussion.share.service.exception.PSBeanValidationUtils;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.share.service.exception.PSParameterValidationUtils;
import com.percussion.share.service.exception.PSSpringValidationException;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.share.validation.PSAbstractBeanValidator;
import com.percussion.share.validation.PSValidationErrorsBuilder;
import com.percussion.sitemanage.dao.IPSUserLoginDao;
import com.percussion.system.utils.IPSHtmlParameters;
import com.percussion.user.data.PSAccessLevel;
import com.percussion.user.data.PSAccessLevelRequest;
import com.percussion.user.data.PSCurrentUser;
import com.percussion.user.data.PSExternalUser;
import com.percussion.user.data.PSImportedUser;
import com.percussion.user.data.PSImportedUser.ImportStatus;
import com.percussion.user.data.PSImportedUserList;
import com.percussion.user.data.PSRoleList;
import com.percussion.user.data.PSUser;
import com.percussion.user.data.PSUserAccountUpdate;
import com.percussion.user.data.PSUserList;
import com.percussion.user.data.PSUserLogin;
import com.percussion.user.data.PSUserProviderType;
import com.percussion.user.service.IPSUserService;
import com.percussion.user.service.IPSUserService.PSDirectoryServiceStatus.ServiceStatus;
import com.percussion.utils.PSSpringBeanProvider;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.service.IPSUtilityService;
import com.percussion.utils.service.impl.PSBackEndRoleManagerFacade;
import com.percussion.utils.service.impl.PSUtilityService;
import com.percussion.webservices.PSWebserviceUtils;
import com.percussion.webservices.content.IPSContentWs;
import com.percussion.webservices.security.IPSSecurityWs;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
// Java 11 Optional
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
// Java 11 Streams
import javax.security.auth.Subject;
import org.apache.commons.lang3.StringUtils; // Modernized: Use lang3
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * See the interface for documentation.
 *
 * @author DavidBenua
 * @author adamgent
 */
@Path("/user")
@Component("userService")
@Lazy
public class PSUserService implements IPSUserService {
  private static final Logger log = LogManager.getLogger(IPSConstants.SECURITY_LOG);

  public static final String VAR_CONFIG_PATH = "var" + File.separatorChar + "config";

  public static final String PWD_CONFIG_PATH = VAR_CONFIG_PATH + File.separatorChar + "generated";

  // Used to get the email on the user
  private static final String EMAIL_ATTRIBUTE_NAME = "sys_email";
  private static final String PWD_FILE = "passwords";

  private final IPSUserLoginDao userLoginDao;

  private final IPSPasswordFilter passwordFilter;

  private final IPSRoleMgr roleMgr;

  private final PSBackEndRoleManagerFacade backEndRoleMgr;

  private final IPSWorkflowService workflowService;

  private final IPSSecurityWs securityWs;

  private final IPSContentWs contentWs;

  private final IPSIdMapper idMapper;

  private List<String> accessibilityRoles = null;

  private IPSSystemProperties systemProps;

  private final ExecutorService executorService = Executors.newSingleThreadExecutor();

  private final IPSUtilityService utilityService;

  private final IPSMetadataService metadataService;

  /**
   * Canonical allowed user landing-page types (PascalCase product labels). Peer to role homepage
   * types Home/Dashboard/Editor; expanded for top-level CMS modules.
   */
  public static final Set<String> ALLOWED_HOMEPAGE_TYPES =
      Collections.unmodifiableSet(
          new LinkedHashSet<>(
              Arrays.asList(
                  HOMEPAGE_TYPE_HOME,
                  HOMEPAGE_TYPE_DASHBOARD,
                  HOMEPAGE_TYPE_EDITOR,
                  HOMEPAGE_TYPE_DESIGNER,
                  HOMEPAGE_TYPE_ARCHITECTURE,
                  HOMEPAGE_TYPE_PUBLISH,
                  HOMEPAGE_TYPE_WORKFLOW,
                  HOMEPAGE_TYPE_WIDGET_BUILDER,
                  HOMEPAGE_TYPE_EXPLORER,
                  HOMEPAGE_TYPE_DEVELOPER)));

  /** Maps canonical homepage type → {@code index.jsp} {@code view} key. */
  private static final Map<String, String> HOMEPAGE_TYPE_TO_VIEW_KEY =
      Map.ofEntries(
          Map.entry(HOMEPAGE_TYPE_HOME, "home"),
          Map.entry(HOMEPAGE_TYPE_DASHBOARD, "dash"),
          Map.entry(HOMEPAGE_TYPE_EDITOR, "editor"),
          Map.entry(HOMEPAGE_TYPE_DESIGNER, "design"),
          Map.entry(HOMEPAGE_TYPE_ARCHITECTURE, "arch"),
          Map.entry(HOMEPAGE_TYPE_PUBLISH, "publish"),
          Map.entry(HOMEPAGE_TYPE_WORKFLOW, "workflow"),
          Map.entry(HOMEPAGE_TYPE_WIDGET_BUILDER, "widgetbuilder"),
          Map.entry(HOMEPAGE_TYPE_EXPLORER, "explorer"),
          Map.entry(HOMEPAGE_TYPE_DEVELOPER, "developer"));

  public static final String PERCUSSION_ADMIN_NAME = "PercussionAdmin";
  public static final String ADMIN_NAME = "Admin";
  public static final String ADMIN1_NAME = "admin1";
  public static final String ADMIN2_NAME = "admin2";
  public static final String EDITOR_NAME = "Editor";
  public static final String CONTRIBUTOR_NAME = "Contributor";
  public static final String RXSERVER_NAME = "rxserver";
  public static final String RXPUBLISHER_NAME = "rxpublisher";
  public static final String ARTIST1_NAME = "artist1";
  public static final String ARTIST2_NAME = "artist2";
  public static final String AUTHOR1_NAME = "author1";
  public static final String AUTHOR2_NAME = "author2";
  public static final String DESIGNER1_NAME = "designer1";
  public static final String DESIGNER2_NAME = "designer2";
  public static final String EDITOR1_NAME = "editor1";
  public static final String EDITOR2_NAME = "editor2";
  public static final String QA1_NAME = "qa1";
  public static final String QA2_NAME = "qa2";

  /** Name of the auto-generated directory set. */
  public static final String DIRECTORY_SET_NAME = "DirectorySet";

  public static final List<String> SYSTEM_USERS = asList(RXSERVER_NAME, PERCUSSION_ADMIN_NAME);

  /**
   * Intentional publish-to-registry via inner server-startup listener. Justified {@code
   * this-escape} suppress (lifecycle registration at construction).
   */
  @SuppressWarnings("this-escape")
  @Autowired
  public PSUserService(
      IPSUserLoginDao userLoginDao,
      IPSPasswordFilter passwordFilter,
      IPSBackEndRoleMgr backEndRoleMgr,
      IPSRoleMgr roleMgr,
      IPSNotificationService notificationService,
      IPSWorkflowService workflowService,
      IPSSecurityWs securityWs,
      IPSContentWs contentWs,
      IPSIdMapper idMapper,
      IPSUtilityService utilityService,
      IPSMetadataService metadataService) {
    super();
    this.userLoginDao = userLoginDao;
    this.passwordFilter = passwordFilter;
    this.backEndRoleMgr = new PSBackEndRoleManagerFacade(backEndRoleMgr);
    this.roleMgr = roleMgr;
    this.workflowService = workflowService;
    this.securityWs = securityWs;
    this.contentWs = contentWs;
    this.idMapper = idMapper;
    this.utilityService = utilityService;
    this.metadataService = metadataService;
    setupServerStartupListener(notificationService);
  }

  /**
   * Registers {@link PSCreatePercussionUserNotificationListener} for server startup.
   *
   * <p>Final so subclass constructors cannot override registration order.
   *
   * @param notificationService never <code>null</code>.
   */
  protected final void setupServerStartupListener(IPSNotificationService notificationService) {
    if (notificationService != null) {
      PSCreatePercussionUserNotificationListener listener =
          new PSCreatePercussionUserNotificationListener();
      notificationService.addListener(EventType.CORE_SERVER_INITIALIZED, listener);
    }
  }

  /**
   * Create the PercussionUser for SaaS PIG test
   *
   * @author adamgent
   */
  protected class PSCreatePercussionUserNotificationListener
      implements IPSNotificationListener, Runnable {

    private String errorMessage = "The server could not create the percussion user. ";

    /** We run the user work in a separate thread to avoid dirtying the server start up thread. */
    @Override
    public void run() {
      try {
        PSThreadRequestUtils.initServerThreadRequest();
        if (utilityService.isSaaSEnvironment() && findUsername(PERCUSSION_ADMIN_NAME) == null) {
          log.info("Creating Percussion User");
          /*
           * Here we have to setup thread-local meta data used for web
           * services. On server start up this is not setup.
           */
          PSRequest req = PSRequest.getContextForRequest();
          resetRequestInfo();
          initRequestInfo(null);
          setRequestInfo(KEY_PSREQUEST, req);
          setUserName(PSSecurityProvider.INTERNAL_USER_NAME);

          TimeUnit.SECONDS.sleep(30);

          createPercussionUser();
          log.info("Finished creating Percussion User");
        }
        log.info("Replacing legacy 'demo' password for generated users...");
        log.info(
            "Generated passwords can be found in the {}{}var{}config{}generated{}passwords file.",
            PSServer.getRxDir().getAbsolutePath(),
            File.separatorChar,
            File.separatorChar,
            File.separatorChar,
            File.separatorChar);
        int count = 0;
        count += updateLegacyPasswordsForUser(ADMIN_NAME);
        count += updateLegacyPasswordsForUser(EDITOR_NAME);
        count += updateLegacyPasswordsForUser(CONTRIBUTOR_NAME);
        count += updateLegacyPasswordsForUser(RXSERVER_NAME);
        count += updateLegacyPasswordsForUser(ADMIN1_NAME);
        count += updateLegacyPasswordsForUser(ADMIN2_NAME);
        count += updateLegacyPasswordsForUser(ARTIST1_NAME);
        count += updateLegacyPasswordsForUser(ARTIST2_NAME);
        count += updateLegacyPasswordsForUser(AUTHOR1_NAME);
        count += updateLegacyPasswordsForUser(AUTHOR2_NAME);
        count += updateLegacyPasswordsForUser(DESIGNER1_NAME);
        count += updateLegacyPasswordsForUser(DESIGNER2_NAME);
        count += updateLegacyPasswordsForUser(EDITOR1_NAME);
        count += updateLegacyPasswordsForUser(EDITOR2_NAME);
        count += updateLegacyPasswordsForUser(QA1_NAME);
        count += updateLegacyPasswordsForUser(QA2_NAME);

        if (count > 0) {
          log.info("Done generating {} new password(s) for generated users.", count);
        }

      } catch (InterruptedException | PSDataServiceException e) {
        log.warn("Shutting down user update thread...");
        Thread.currentThread().interrupt();
      }
    }

    @Override
    public void notifyEvent(PSNotificationEvent event) {
      notNull(event, "event");
      isTrue(
          EventType.CORE_SERVER_INITIALIZED == event.getType(),
          "Should only be registered for server startup.");

      try {
        /*
         * This will execute our work concurrently.
         */
        executorService.execute(this);
      } catch (Exception e) {
        throw new RuntimeException(errorMessage, e);
      }
    }
  }

  private void writeTemporaryPassword(String uid, String pwd) {
    File pwdFile =
        new File(PSServer.getRxDir().getAbsolutePath() + File.separatorChar + PWD_CONFIG_PATH);
    try {
      if (!pwdFile.exists()) {
        pwdFile.mkdirs();
        Properties props = new Properties();
        props.put(uid, pwd);
        try (FileOutputStream outputStream =
            new FileOutputStream(
                PSServer.getRxDir().getAbsolutePath()
                    + File.separatorChar
                    + PWD_CONFIG_PATH
                    + File.separatorChar
                    + PWD_FILE)) {
          props.store(outputStream, "File for generated temporary passwords");
        }
      } else {
        Properties props = new Properties();
        try (FileInputStream fis =
            new FileInputStream(
                PSServer.getRxDir().getAbsolutePath()
                    + File.separatorChar
                    + PWD_CONFIG_PATH
                    + File.separatorChar
                    + PWD_FILE)) {
          props.load(fis);
        }

        props.put(uid, pwd);

        try (FileOutputStream outputStream =
            new FileOutputStream(
                PSServer.getRxDir().getAbsolutePath()
                    + File.separatorChar
                    + PWD_CONFIG_PATH
                    + File.separatorChar
                    + PWD_FILE)) {
          props.store(outputStream, "File for generated temporary passwords");
        }
      }
    } catch (IOException e) {
      log.error("{}", PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
    }
  }

  /**
   * generates a new password for any built-in / generated users that have "demo" as their password.
   *
   * @return a count of user passwords that have been changed - 0 or 1
   */
  private int updateLegacyPasswordsForUser(String userName) {

    boolean found = false;
    int ret = 0;
    PSUserLogin u = null;
    try {

      List<PSUserLogin> users = userLoginDao.findByName(userName);
      if (users != null && !users.isEmpty()) {
        u = users.get(0);
        log.debug("Found User: {}", u.getUserid());
      }
      found = true;
    } catch (PSDataServiceException e) {
      // ignore if not found
    }

    if (found && u != null) {
      try {
        if (PSLegacyEncrypter.LEGACY_USER_PWD.equalsIgnoreCase(u.getPassword())
            || PSLegacyEncrypter.LEGACY_USER_PWD_ENC.equalsIgnoreCase(u.getPassword())
            || PSPasswordHandler.getHashedPassword(PSLegacyEncrypter.LEGACY_USER_PWD)
                .equals(u.getPassword())) {
          String pw = SecureStringUtils.generateRandomPassword();
          String cryptPW = (passwordFilter == null) ? pw : passwordFilter.encrypt(pw);
          u.setPassword(cryptPW);
          try {

            userLoginDao.save(u);

            writeTemporaryPassword(userName, pw);

            log.info("Generating new temporary password: {} for {}", pw, userName);
            log.info(
                "This temporary password will be stored in: {}",
                PSServer.getRxDir().getAbsolutePath()
                    + File.separatorChar
                    + PWD_CONFIG_PATH
                    + File.separatorChar
                    + PWD_FILE);
            log.info(
                "Please change this temporary password using the Change Password feature after"
                    + " installation / upgrade.");
            ret = 1;
          } catch (PSDataServiceException e) {
            log.error(
                "An unexpected error resetting legacy passwords: {}",
                PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
          }
        }
      } catch (PSEncryptionException e) {
        log.error(PSExceptionUtils.getMessageForLog(e));
      }
    }
    return ret;
  }

  /**
   * Create the PercussionUser. Will generate a password and write the password to system log and to
   * the PWD_CONFIG_PATH + "password" file.
   */
  protected void createPercussionUser() throws PSDataServiceException {

    PSUser user = new PSUser();

    String password = SecureStringUtils.generateRandomPassword();

    user.setName(PERCUSSION_ADMIN_NAME);
    user.setPassword(password);

    user.setEmail("");
    List<String> roles = new ArrayList<>();
    roles.add(IPSRoleService.ADMINISTRATOR_ROLE);

    user.setRoles(roles);
    createUser(user);
    log.info("Generating temporary password: {} for {}", password, PERCUSSION_ADMIN_NAME);
    log.info(
        "This temporary password will be stored in: {}",
        PWD_CONFIG_PATH + File.separatorChar + PWD_FILE);
    log.info(
        "Please change this temporary password using the Change Password feature after installation"
            + " / upgrade.");
    writeTemporaryPassword(PERCUSSION_ADMIN_NAME, password);
  }

  @Override
  @POST
  @Path("/create")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSUser create(PSUser user) throws PSDataServiceException {
    log.debug("creating user {}", user);
    doValidation(user, true);

    // XSS residual (Jackson/JAXB/CXF or documented pass-through): JSON/XML DTO via Jackson/JAXB;
    // not HTML body (alert #753)
    return createUser(user); // codeql[java/xss]
  }

  private PSUser createUser(PSUser user) throws PSDataServiceException {
    PSUserLogin login = new PSUserLogin();
    login.setUserid(user.getName());
    String cryptPW =
        (passwordFilter == null) ? user.getPassword() : passwordFilter.encrypt(user.getPassword());
    login.setPassword(cryptPW);
    try {
      login = userLoginDao.create(login);

      updateRoles(user.getName(), user.getRoles());
      backEndRoleMgr.setSubjectEmail(user.getName(), user.getEmail());
    } catch (IPSGenericDao.SaveException e) {
      log.error(
          "Failed to create user {} because could not add roles to user: {}",
          user.getName(),
          PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
    }

    PSUser rvalue = null;
    try {
      rvalue = user.clone();
    } catch (CloneNotSupportedException e) {
      throw new PSDataServiceException(e);
    }
    rvalue.setProviderType(PSUserProviderType.INTERNAL);
    rvalue.setPassword(null);
    rvalue.setEmail(user.getEmail());
    return rvalue;
  }

  @Override
  @DELETE
  @Path("/delete/{name}")
  public void delete(@PathParam("name") String name) throws PSDataServiceException {
    log.debug("deleting user {}", name);
    checkUser(name);
    if (PSCollectionUtils.containsIgnoringCase(SYSTEM_USERS, name))
      PSParameterValidationUtils.validateParameters("delete")
          .rejectField("name", "Cannot delete system user", name)
          .throwIfInvalid();
    PSUserProviderType provider = fromProvider(name);
    String current = getCurrentUserName();
    if (name.equalsIgnoreCase(current)) {
      String emsg = "Cannot delete the current user";
      log.error(emsg);
      PSParameterValidationUtils.validateParameters("delete")
          .rejectField("name", emsg, name)
          .throwIfInvalid();
    }
    // remove from all roles
    backEndRoleMgr.setRoles(name, Collections.<String>emptyList());
    if (provider == PSUserProviderType.INTERNAL) {
      userLoginDao.remove(name);
    }
    try {
      PSSystemAuditLogger.userDelete(currentServletRequest(), AuditOutcome.SUCCESS, name);
    } catch (Exception e) {
      // Just handling exception
    }

    PSNotificationEvent notifyEvent = new PSNotificationEvent(EventType.USER_DELETE, name);
    IPSNotificationService srv = PSNotificationServiceLocator.getNotificationService();
    srv.notifyEvent(notifyEvent);
  }

  @Override
  @GET
  @Path("/find/{name}")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSUser find(@PathParam("name") String name) throws PSDataServiceException {
    PSUser user = new PSUser();
    user.setName(name);
    checkUser(name);
    PSUserProviderType provider = fromProvider(user.getName());
    user.setProviderType(provider);
    List<String> roles = findRoles(name);
    roles = filterOutSystemRoles(roles);
    user.setRoles(roles);
    // /find keeps historical INTERNAL-only email exposure for other users (privacy for API
    // consumers). Directory email is loaded on self-profile via getCurrentUser().
    if (provider.equals(PSUserProviderType.INTERNAL)) {
      try {
        user.setEmail(getSubjectEmail(name));
      } catch (PSSecurityCatalogException e) {
        log.error("Failed to get the email for the user: {}", name);
      }
    }
    return user;
  }

  /**
   * Filter out the pre-defined system roles for the specified roles.
   *
   * @param srcRoles the list of role names in question, assumed not <code>null</code>.
   * @return a list of role names that does not contain any of the pre-defined system roles. It may
   *     be empty, but never <code>null</code>.
   */
  private List<String> filterOutSystemRoles(Collection<String> srcRoles) {
    List<String> result = new ArrayList<>();
    for (String role : srcRoles) {
      if (!PSCollectionUtils.containsIgnoringCase(PSRoleService.SYSTEM_ROLES, role)) {
        result.add(role);
      }
    }

    return result;
  }

  /**
   * Gets the list of roles. The list of roles is returned in alphabetical order according to the
   * current default locale.
   *
   * @see com.percussion.user.service.IPSUserService#getRoles()
   */
  @Override
  @GET
  @Path("/roles")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSRoleList getRoles() throws PSDataServiceException {
    PSRoleList roles = new PSRoleList();

    List<String> rl = filterOutSystemRoles(backEndRoleMgr.getRoles());
    roles.setRoles(rl);
    return roles;
  }

  /**
   * Gets the list of users. The list of users is returned in alphabetical order according to the
   * current default locale.
   *
   * @see com.percussion.user.service.IPSUserService#getUsers()
   */
  @Override
  @GET
  @Path("/users")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSUserList getUsers() throws PSDataServiceException {
    List<String> names = findUserNames(null);
    names.removeAll(SYSTEM_USERS);
    PSUserList result = new PSUserList();
    sort(names);
    result.setUsers(names);
    return result;
  }

  @Override
  @GET
  @Path("/users/names/{nameFilter}")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSUserList getUserNames(@PathParam("nameFilter") String nameFilter)
      throws PSDataServiceException {
    if (nameFilter == null || StringUtils.isEmpty(nameFilter) || nameFilter.equalsIgnoreCase("*")) {
      nameFilter = "%";
    }

    List<String> names = findUserNames(nameFilter);
    names.removeAll(SYSTEM_USERS);
    PSUserList result = new PSUserList();
    sort(names);
    result.setUsers(names);
    return result;
  }

  /**
   * Gets the list of users which are members of the specified role. The list of users is returned
   * in alphabetical order according to the current default locale.
   *
   * @see com.percussion.user.service.IPSUserService#getUsers()
   */
  @Override
  @GET
  @Path("/usersByRole/{role}")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSUserList getUsersByRole(@PathParam("role") String roleName)
      throws PSDataServiceException {
    PSParameterValidationUtils.rejectIfBlank("getUsersByRole", "roleName", roleName);

    PSUserList userList = getUsers();
    Iterator<String> iter = userList.getUsers().iterator();
    while (iter.hasNext()) {
      PSUser user = find(iter.next());
      if (!user.getRoles().contains(roleName)) {
        iter.remove();
      }
    }

    return userList;
  }

  private PSUserProviderType fromProvider(String name) throws PSDataServiceException {
    boolean internal = userLoginDao.find(name) != null;
    return internal ? PSUserProviderType.INTERNAL : PSUserProviderType.DIRECTORY;
  }

  @Override
  @POST
  @Path("/update")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSUser update(PSUser user) throws PSDataServiceException {
    log.debug("updating user {}", user);
    PSUserProviderType provider = fromProvider(user.getName());
    user.setProviderType(provider);
    doValidation(user, false);

    if (provider == PSUserProviderType.INTERNAL
        && isNotBlank(user.getPassword())) { // only update if there is a password supplied
      // and its an internal user.
      PSUserLogin login = new PSUserLogin();
      login.setUserid(user.getName());
      String cryptPW =
          (passwordFilter == null)
              ? user.getPassword()
              : passwordFilter.encrypt(user.getPassword());
      login.setPassword(cryptPW);

      userLoginDao.save(login);
    }
    updateRoles(user.getName(), user.getRoles());

    PSUser rvalue = null;
    try {
      rvalue = user.clone();
    } catch (CloneNotSupportedException e) {
      throw new PSDataServiceException(e);
    }
    rvalue.setProviderType(provider);
    rvalue.setPassword(null);
    if (provider.equals(PSUserProviderType.INTERNAL)) {
      backEndRoleMgr.setSubjectEmail(user.getName(), user.getEmail());
      rvalue.setEmail(user.getEmail());
    }
    try {
      PSSystemAuditLogger.userUpdate(
          currentServletRequest(), AuditOutcome.SUCCESS, user.getName(), "update");
    } catch (Exception e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
    }
    // XSS residual (Jackson/JAXB/CXF or documented pass-through): JSON/XML DTO via Jackson/JAXB;
    // not HTML body (alert #754)
    return rvalue; // codeql[java/xss]
  }

  @Override
  @PUT
  @Path("/changepw")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSUser changePassword(PSUser user) throws PSDataServiceException {
    // Validate before any lookup so a null body is 400, not NPE 500.
    PSParameterValidationUtils.validateParameters("changePassword")
        .rejectIfNull("user", user)
        .throwIfInvalid();
    PSParameterValidationUtils.validateParameters("changePassword")
        .rejectIfBlank("password", user.getPassword())
        .throwIfInvalid();
    PSParameterValidationUtils.validateParameters("changePassword")
        .rejectIfBlank("name", user.getName())
        .throwIfInvalid();

    String userName = user.getName();
    log.debug("changing password for user {}", userName);

    PSUserProviderType provider = fromProvider(userName);
    user.setProviderType(provider);
    if (provider != PSUserProviderType.INTERNAL) {
      PSParameterValidationUtils.validateParameters("changePassword")
          .rejectField(
              "password",
              "Password can only be changed for internal users",
              userName)
          .throwIfInvalid();
    }

    // Load the session user once *before* the credential write. Reloading
    // getCurrentUser()/find() after USERLOGIN is updated can fail (session
    // subject refresh) and would map to HTTP 500 even though the password
    // already persisted — issue #3338.
    PSCurrentUser currentUser = getCurrentUser();
    if (currentUser == null || !userName.equalsIgnoreCase(currentUser.getName())) {
      String emsg = "Can only change the password of the current user";
      log.error(emsg);
      PSParameterValidationUtils.validateParameters("changePassword")
          .rejectField("name", emsg, userName)
          .throwIfInvalid();
    }

    PSUserLogin login = new PSUserLogin();
    login.setUserid(userName);
    String cryptPW =
        (passwordFilter == null)
            ? user.getPassword()
            : passwordFilter.encrypt(user.getPassword());
    login.setPassword(cryptPW);
    userLoginDao.save(login);

    try {
      PSSystemAuditLogger.userUpdate(
          currentServletRequest(), AuditOutcome.SUCCESS, userName, "changepw");
    } catch (Exception e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
    }

    // Never return null (CXF JSON/XML writers 500 on a null entity). Build a
    // fresh DTO from the pre-loaded session user — do not clone the request
    // body (BeanUtils.cloneBean) and do not re-fetch after persist.
    PSUser rvalue = passwordChangeResult(currentUser);
    // XSS residual (Jackson/JAXB/CXF or documented pass-through): JSON/XML DTO via Jackson/JAXB;
    // not HTML body (alert #755)
    return rvalue; // codeql[java/xss]
  }

  /**
   * Wire DTO for a successful self-service password change. Password is always
   * cleared. Roles/email come from the user loaded before persist.
   */
  private static PSUser passwordChangeResult(PSCurrentUser currentUser) {
    PSUser rvalue = new PSUser();
    rvalue.setName(currentUser.getName());
    rvalue.setEmail(currentUser.getEmail() == null ? "" : currentUser.getEmail());
    rvalue.setProviderType(PSUserProviderType.INTERNAL);
    rvalue.setPassword(null);
    List<String> roles = currentUser.getRoles();
    if (roles != null) {
      rvalue.setRoles(new ArrayList<>(roles));
    }
    return rvalue;
  }

  @Override
  @GET
  @Path("/current")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSCurrentUser getCurrentUser() throws PSDataServiceException {
    String userName;
    try {
      userName = getCurrentUserName();
      if (isBlank(userName))
        throw new PSNoCurrentUserException("No current user in current request");
    } catch (Exception e) {
      throw new PSNoCurrentUserException("Error getting current user.", e);
    }
    PSUser user = find(userName);
    PSCurrentUser currUser = new PSCurrentUser(user);

    // Self-profile needs directory email too; find() intentionally omits it for non-INTERNAL.
    // Catalog failure is best-effort: leave email empty and surface the failure in server logs
    // so operators can diagnose directory issues without failing the whole self-profile load.
    if (currUser.getProviderType() != PSUserProviderType.INTERNAL) {
      try {
        currUser.setEmail(getSubjectEmail(userName));
      } catch (PSSecurityCatalogException e) {
        currUser.setEmail("");
        log.warn(
            "Directory email catalog failed for user {} — returning empty email: {}",
            userName,
            PSExceptionUtils.getMessageForLog(e));
        log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      }
    }

    boolean isAdmin = currUser.getRoles().contains(ADMINISTRATOR_ROLE);
    currUser.setAdminUser(isAdmin);

    boolean isDesigner = currUser.getRoles().contains(DESIGNER_ROLE);
    currUser.setDesignerUser(isDesigner);

    boolean isAccessibility = containsAny(currUser.getRoles(), getAccessibilityRoles());
    currUser.setAccessibilityUser(isAccessibility);

    enrichCurrentUserCommunities(currUser);

    return currUser;
  }

  /**
   * Self-service account update for the signed-in user only (issue #2395 / parent #2374).
   *
   * <p>No user name on the path or body — always mutates the session user (no IDOR). Only email
   * for {@link PSUserProviderType#INTERNAL} accounts is persisted; directory-managed accounts
   * reject email changes. Roles, name, password, and provider type are never accepted here.
   */
  @Override
  @PUT
  @Path("/profile")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSCurrentUser updateMyAccount(PSUserAccountUpdate update) throws PSDataServiceException {
    PSParameterValidationUtils.validateParameters("updateMyAccount")
        .rejectIfNull("update", update)
        .throwIfInvalid();

    String email = StringUtils.trimToEmpty(update.getEmail());
    if (isNotBlank(email) && !isValidEmailAddress(email)) {
      PSParameterValidationUtils.validateParameters("updateMyAccount")
          .rejectField("email", "Enter a valid email address.", email)
          .throwIfInvalid();
    }

    PSCurrentUser current = getCurrentUser();
    if (current.getProviderType() != PSUserProviderType.INTERNAL) {
      PSParameterValidationUtils.validateParameters("updateMyAccount")
          .rejectField(
              "email",
              "Email is managed by the directory service and cannot be changed here.",
              email)
          .throwIfInvalid();
    }

    try {
      backEndRoleMgr.setSubjectEmail(current.getName(), email);
    } catch (RuntimeException e) {
      // Persist failed — audit FAILURE so the trail is not silent, then rethrow.
      logSelfServiceAccountUpdateAudit(AuditOutcome.FAILURE);
      log.error(
          "Self-service email update failed for user {}: {}",
          current.getName(),
          PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw e;
    }

    // Persist succeeded — only then record SUCCESS (not before end-to-end completion of the write).
    logSelfServiceAccountUpdateAudit(AuditOutcome.SUCCESS);

    // Return the already-loaded session user with the new email applied. Avoid a second
    // getCurrentUser() which can fail after the write (session/directory) with no rollback.
    current.setEmail(email);
    return current;
  }

  /**
   * Self-service default community for the signed-in user only (issue #3508). Persists {@link
   * PSAuthenticateUserUtils#SYS_DEFAULTCOMMUNITY} on the user subject. Blank body clears the
   * stored default so role-level defaults apply at the next login.
   */
  @Override
  @PUT
  @Path("/defaultCommunity")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Consumes({MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSCurrentUser updateMyDefaultCommunity(String communityName) throws PSDataServiceException {
    PSCurrentUser current = getCurrentUser();
    String canonical = canonicalizeAllowedCommunity(communityName, current.getCommunities());
    if (canonical == null) {
      PSParameterValidationUtils.validateParameters("updateMyDefaultCommunity")
          .rejectField(
              "defaultCommunity",
              "Community is not in your allowed communities.",
              communityName)
          .throwIfInvalid();
    }

    try {
      backEndRoleMgr.setSubjectAttribute(
          current.getName(), PSAuthenticateUserUtils.SYS_DEFAULTCOMMUNITY, canonical);
    } catch (RuntimeException e) {
      logSelfServiceAccountUpdateAudit(AuditOutcome.FAILURE);
      log.error(
          "Self-service default community update failed for user {}: {}",
          current.getName(),
          PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw e;
    }

    logSelfServiceAccountUpdateAudit(AuditOutcome.SUCCESS);
    current.setDefaultCommunity(canonical);
    return current;
  }

  /**
   * Maps a requested community name onto the user's membership list. Blank request clears
   * (returns empty string). Unknown / disallowed names return {@code null}.
   */
  static String canonicalizeAllowedCommunity(String requested, List<String> allowed) {
    if (requested == null || requested.isBlank()) {
      return "";
    }
    if (allowed == null) {
      return null;
    }
    String trimmed = requested.trim();
    for (String name : allowed) {
      if (name != null && trimmed.equalsIgnoreCase(name.trim())) {
        return name.trim();
      }
    }
    return null;
  }

  /**
   * Best-effort audit for self-service account updates. Never throws — audit infrastructure must
   * not mask the primary operation outcome.
   */
  private void logSelfServiceAccountUpdateAudit(AuditOutcome outcome) {
    try {
      PSRequest req = PSSecurityFilter.getCurrentRequest();
      if (req != null && req.getServletRequest() != null) {
        String actor = req.getServletRequest().getRemoteUser();
        PSSystemAuditLogger.userUpdate(
            req.getServletRequest(), outcome, actor, "self-service-account");
      }
    } catch (Exception e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
    }
  }

  private static jakarta.servlet.http.HttpServletRequest currentServletRequest() {
    try {
      PSRequest req = PSSecurityFilter.getCurrentRequest();
      return req != null ? req.getServletRequest() : null;
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Best-effort session community summary for the profile hub. Failures are logged and left empty
   * so account identity still returns.
   *
   * <p>Stored {@code sys_defaultCommunity} is applied only after membership names are loaded, and
   * only when it is still in that list — {@code GET /user/user/current} never returns a disallowed
   * default.
   */
  private void enrichCurrentUserCommunities(PSCurrentUser currUser) {
    String storedDefault = null;
    try {
      storedDefault =
          backEndRoleMgr.getSubjectAttribute(
              currUser.getName(), PSAuthenticateUserUtils.SYS_DEFAULTCOMMUNITY);
    } catch (Exception e) {
      log.debug(
          "Unable to load default community for current user: {}",
          PSExceptionUtils.getMessageForLog(e));
    }
    try {
      PSRequest req = PSSecurityFilter.getCurrentRequest();
      if (req == null || req.getUserSession() == null) {
        applyAllowedStoredDefault(currUser, storedDefault);
        return;
      }
      String currentCommunity = req.getUserSession().getUserCurrentCommunity();
      if (isNotBlank(currentCommunity)) {
        currUser.setCurrentCommunity(currentCommunity);
      }
      List<String> names = req.getUserSession().getUserCommunityNames(req);
      if (names != null && !names.isEmpty()) {
        List<String> sorted = new ArrayList<>(names);
        sort(sorted);
        currUser.setCommunities(sorted);
      }
    } catch (Exception e) {
      log.debug(
          "Unable to load community summary for current user: {}",
          PSExceptionUtils.getMessageForLog(e));
    }
    applyAllowedStoredDefault(currUser, storedDefault);
  }

  /**
   * Sets {@code defaultCommunity} only when {@code storedDefault} is still in the user's
   * membership list. Unknown or disallowed names become empty.
   */
  static void applyAllowedStoredDefault(PSCurrentUser currUser, String storedDefault) {
    String canonical = canonicalizeAllowedCommunity(storedDefault, currUser.getCommunities());
    currUser.setDefaultCommunity(canonical == null ? "" : canonical);
  }

  /**
   * Lightweight email shape check for self-service updates. Empty email is allowed at the call
   * site (clears stored value) and is rejected here so callers must special-case blank.
   *
   * <p>Domain labels may not start/end with hyphen or contain consecutive dots (rejects e.g.
   * {@code user@domain..com}, {@code user@-domain.com}, {@code user@domain-.com}).
   */
  static boolean isValidEmailAddress(String email) {
    if (email == null) {
      return false;
    }
    String value = email.trim();
    if (value.isEmpty() || value.length() > 254) {
      return false;
    }
    // local@label(.label)+ — each label starts/ends alnum; TLD at least 2 letters
    return value.matches(
        "^[A-Za-z0-9._%+-]+@(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\\.)+[A-Za-z]{2,}$");
  }

  /**
   * GET persisted default landing override for the current user. Empty string when unset (caller
   * should fall back to role resolve via {@code /role/userhomepage}).
   */
  @GET
  @Path("/homepage")
  @Produces(MediaType.TEXT_PLAIN)
  public String getCurrentUserHomepageOverride() throws PSDataServiceException {
    return getHomepageOverride(requireCurrentUserName());
  }

  /**
   * PUT default landing override for the current user. Body is plain-text product type or view-key
   * alias; blank body clears the override.
   */
  @PUT
  @Path("/homepage")
  @Consumes(MediaType.TEXT_PLAIN)
  @Produces(MediaType.TEXT_PLAIN)
  public String setCurrentUserHomepageOverride(String homepage) throws PSDataServiceException {
    return setHomepageOverride(requireCurrentUserName(), homepage);
  }

  /** DELETE default landing override for the current user. */
  @DELETE
  @Path("/homepage")
  public void clearCurrentUserHomepageOverride() throws PSDataServiceException {
    clearHomepageOverride(requireCurrentUserName());
  }

  /**
   * GET persisted default landing override for a named user (admin-managed). Empty string when
   * unset.
   */
  @GET
  @Path("/homepage/{userName}")
  @Produces(MediaType.TEXT_PLAIN)
  public String getHomepageOverride(@PathParam("userName") String userName)
      throws PSDataServiceException {
    PSParameterValidationUtils.rejectIfBlank("getHomepageOverride", "userName", userName);
    assertCanManageHomepage(userName);
    try {
      var md = metadataService.find(homepageMetadataKey(userName));
      if (md == null || isBlank(md.getData())) {
        return "";
      }
      // Stale/invalid stored value treated as unset for read
      String normalized = normalizeHomepageType(md.getData());
      return normalized == null ? "" : normalized;
    } catch (IPSGenericDao.LoadException e) {
      throw new PSDataServiceException("Failed to load homepage override for user " + userName, e);
    }
  }

  /**
   * PUT default landing override for a named user. Blank body clears. Invalid value → validation
   * error (400).
   */
  @PUT
  @Path("/homepage/{userName}")
  @Consumes(MediaType.TEXT_PLAIN)
  @Produces(MediaType.TEXT_PLAIN)
  public String setHomepageOverride(
      @PathParam("userName") String userName, String homepage) throws PSDataServiceException {
    PSParameterValidationUtils.rejectIfBlank("setHomepageOverride", "userName", userName);
    assertCanManageHomepage(userName);
    if (isBlank(homepage)) {
      clearHomepageOverride(userName);
      return "";
    }
    String normalized = normalizeHomepageType(homepage);
    if (normalized == null) {
      PSParameterValidationUtils.validateParameters("setHomepageOverride")
          .rejectField(
              "homepage",
              "Invalid homepage value '"
                  + homepage
                  + "'. Allowed: "
                  + ALLOWED_HOMEPAGE_TYPES
                  + " (or view keys home/dash/editor/design/arch/publish/workflow/widgetbuilder/explorer/developer).",
              homepage)
          .throwIfInvalid();
    }
    try {
      var key = homepageMetadataKey(userName);
      var md = metadataService.find(key);
      if (md == null) {
        md = new PSMetadata(key, normalized);
      } else {
        md.setData(normalized);
      }
      metadataService.save(md);
      return normalized;
    } catch (IPSGenericDao.LoadException | IPSGenericDao.SaveException e) {
      throw new PSDataServiceException("Failed to save homepage override for user " + userName, e);
    }
  }

  /** DELETE default landing override for a named user. */
  @DELETE
  @Path("/homepage/{userName}")
  public void clearHomepageOverride(@PathParam("userName") String userName)
      throws PSDataServiceException {
    PSParameterValidationUtils.rejectIfBlank("clearHomepageOverride", "userName", userName);
    assertCanManageHomepage(userName);
    try {
      var key = homepageMetadataKey(userName);
      var md = metadataService.find(key);
      if (md != null) {
        metadataService.delete(key);
      }
    } catch (IPSGenericDao.LoadException | IPSGenericDao.DeleteException e) {
      throw new PSDataServiceException(
          "Failed to clear homepage override for user " + userName, e);
    }
  }

  /**
   * Canonical metadata key for a user's homepage override.
   *
   * <p>Usernames are compared case-insensitively for authz ({@link #assertCanManageHomepage}); the
   * key must use the same case folding so {@code /homepage/alice} and {@code /homepage/Alice} share
   * one stored value (and {@link com.percussion.role.service.impl.PSRoleService#getUserHomepage}
   * finds the override regardless of {@code current.getName()} casing).
   */
  static String homepageMetadataKey(String userName) {
    return META_DATA_HOMEPAGE_PREFIX + userName.trim().toLowerCase(Locale.ROOT);
  }

  /**
   * Normalizes a raw homepage string to a canonical product type, or {@code null} if invalid.
   *
   * <p>Accepts canonical types (case-sensitive) and common view-key / label aliases
   * (case-insensitive).
   */
  public static String normalizeHomepageType(String raw) {
    if (isBlank(raw)) {
      return null;
    }
    String trimmed = raw.trim();
    if (ALLOWED_HOMEPAGE_TYPES.contains(trimmed)) {
      return trimmed;
    }
    String lower = trimmed.toLowerCase(Locale.ROOT);
    switch (lower) {
      case "home":
        return HOMEPAGE_TYPE_HOME;
      case "dash":
      case "dashboard":
        return HOMEPAGE_TYPE_DASHBOARD;
      case "editor":
      case "pageeditor":
      case "webmgt":
        return HOMEPAGE_TYPE_EDITOR;
      case "design":
      case "designer":
      case "siteadmin":
      case "admin":
        return HOMEPAGE_TYPE_DESIGNER;
      case "arch":
      case "architecture":
      case "navigation":
      case "site_arch":
      case "sitearch":
        return HOMEPAGE_TYPE_ARCHITECTURE;
      case "publish":
        return HOMEPAGE_TYPE_PUBLISH;
      case "workflow":
        return HOMEPAGE_TYPE_WORKFLOW;
      case "widgetbuilder":
      case "widget-builder":
      case "widget_builder":
        return HOMEPAGE_TYPE_WIDGET_BUILDER;
      case "explorer":
        return HOMEPAGE_TYPE_EXPLORER;
      case "developer":
        return HOMEPAGE_TYPE_DEVELOPER;
      default:
        return null;
    }
  }

  /**
   * Maps a canonical homepage type to the {@code index.jsp} {@code view} query key. Unknown/blank
   * → {@code home}.
   */
  public static String homepageTypeToViewKey(String homepageType) {
    if (isBlank(homepageType)) {
      return "home";
    }
    return HOMEPAGE_TYPE_TO_VIEW_KEY.getOrDefault(homepageType, "home");
  }

  private String requireCurrentUserName() throws PSNoCurrentUserException {
    try {
      String userName = getCurrentUserName();
      if (isBlank(userName)) {
        throw new PSNoCurrentUserException("No current user in current request");
      }
      return userName;
    } catch (PSNoCurrentUserException e) {
      throw e;
    } catch (Exception e) {
      throw new PSNoCurrentUserException("Error getting current user.", e);
    }
  }

  /**
   * Current user may manage their own homepage; only Admin may manage another user's.
   */
  private void assertCanManageHomepage(String targetUserName) throws PSDataServiceException {
    String current;
    try {
      current = getCurrentUserName();
    } catch (Exception e) {
      throw new PSNoCurrentUserException("Error getting current user.", e);
    }
    if (isBlank(current)) {
      throw new PSNoCurrentUserException("No current user in current request");
    }
    if (StringUtils.equalsIgnoreCase(current, targetUserName)) {
      return;
    }
    if (!isAdminUser(current)) {
      PSParameterValidationUtils.validateParameters("homepage")
          .rejectField(
              "userName",
              "Only an Admin user may get or set another user's default landing page.",
              targetUserName)
          .throwIfInvalid();
    }
  }

  @POST
  @Path("/accessLevel")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSAccessLevel getAccessLevel(PSAccessLevelRequest request) {
    try {
      PSParameterValidationUtils.rejectIfNull("getAccessLevel", "request", request);

      PSAssignmentTypeEnum assignmentType = PSAssignmentTypeEnum.READER;

      String type = request.getType();
      int workflowId =
          request.getWorkflowId() > 0 ? request.getWorkflowId() : getWorkflowId(request);

      try {
        PSWorkflow wf = null;
        if (workflowId > 0) {
          wf = workflowService.loadWorkflow(PSGuidUtils.makeGuid(workflowId, PSTypeEnum.WORKFLOW));
          if (wf == null) log.debug("Got invalid workflow id '{}", workflowId);
        }
        if (wf == null) {
          // load default workflow by id since interface doesn't declare getDefaultWorkflow
          wf = workflowService.loadWorkflow(workflowService.getDefaultWorkflowId());
        }

        PSState state = wf.getInitialState();
        int communityId = (int) securityWs.loadCommunities("Default").get(0).getId();

        PSUser user = getCurrentUser();
        PSAssignmentTypeHelper helper =
            new PSAssignmentTypeHelper(user.getName(), user.getRoles(), communityId);
        assignmentType = helper.getAssignmentType(wf, state, communityId, null);
      } catch (SQLException | PSDataServiceException throwables) {
        log.error(
            "Error occurred determining access level of current user for type '{}', workflow id"
                + " '{}'. {}",
            type,
            workflowId,
            throwables.getMessage());
        log.debug(throwables);
        throw new WebApplicationException(throwables.getMessage());
      }

      PSAccessLevel accessLevel = new PSAccessLevel();
      accessLevel.setAccessLevel(assignmentType.name());

      return accessLevel;
    } catch (PSValidationException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e.getMessage());
    }
  }

  /**
   * Get the workflow ID from the request, from the parent-folder or the (source) item (which may be
   * copied from).
   *
   * @param request the request, assumed not <code>null</code>.
   * @return the workflow ID. It may be <code>-1</code> if there is no (source) item or the
   *     parent-folder does not have the workflow ID property.
   */
  private int getWorkflowId(PSAccessLevelRequest request) {
    int workflowId = -1;
    IPSGuid guid = null;
    String itemId = "-1".equals(request.getItemId()) ? null : request.getItemId();

    if (isNotBlank(itemId)) {
      int contentId = ((PSLegacyGuid) idMapper.getGuid(itemId)).getContentId();
      PSComponentSummary compSum = getItemSummary(contentId);
      workflowId = compSum.getWorkflowAppId();
    } else {
      String uiPath = request.getParentFolderPath();
      if (isBlank(uiPath)) return -1;

      String parentFolderPath = "/" + uiPath;
      if (uiPath.startsWith("/Assets"))
        parentFolderPath = PSAssetPathItemService.ASSET_ROOT_SUB + uiPath;

      guid = contentWs.getIdByPath(parentFolderPath);

      PSFolder parentFolder = contentWs.loadFolder(guid, false);
      String parentWorkflowId = parentFolder.getPropertyValue(IPSHtmlParameters.SYS_WORKFLOWID);
      if (isNotBlank(parentWorkflowId)) {
        try {
          workflowId = Integer.parseInt(parentWorkflowId);
        } catch (NumberFormatException e) {
          return -1;
        }
      }
    }
    return workflowId;
  }

  /**
   * Validates the specified user. It validates the user object according to its annotation and
   * invokes {@link PSUserValidator#doValidation(PSUser, PSBeanValidationException)} for additional
   * validation.
   *
   * @param user the user in question, not <code>null</code>.
   * @param isCreateUser if <code>true</code>, validating creating the user.
   * @throws PSBeanValidationException if failed to validate the specified user.
   */
  protected void doValidation(PSUser user, boolean isCreateUser) throws PSValidationException {
    log.debug("validating user {}", user);
    user.setCreateUser(isCreateUser);
    PSUserValidator validator = new PSUserValidator(isCreateUser);

    validator.validate(user).throwIfInvalid();
  }

  /**
   * Sets to the given roles to the given user removing the old roles associated to the user.
   *
   * <p><em>This also makes sure that the default roles are always associated to the user.</em>
   *
   * @param userName never <code>null</code> or empty.
   * @param roles never <code>null</code>.
   */
  protected void updateRoles(String userName, List<String> roles) {
    /*
     * We use a set to remove duplicates and add the default roles.
     */
    Set<String> updateRoles = roles != null ? new HashSet<>(roles) : new HashSet<>();
    updateRoles.addAll(PSRoleService.DEFAULT_ROLES);
    backEndRoleMgr.setRoles(userName, updateRoles);
  }

  /**
   * Sets to the given roles to the given users removing the old roles associated to the users.
   *
   * <p><em>This also makes sure that the default roles are always associated to the users.</em>
   *
   * @param userNames list of users. Never <code>null</code> or empty.
   * @param roles never <code>null</code>.
   */
  protected void updateRoles(List<String> userNames, List<String> roles) {
    /*
     * We use a set to remove duplicates and add the default roles.
     */
    Set<String> updateRoles = roles != null ? new HashSet<>(roles) : new HashSet<>();
    updateRoles.addAll(PSRoleService.DEFAULT_ROLES);
    backEndRoleMgr.setRoles(userNames, updateRoles);
  }

  /**
   * Gets a list of roles that the specified user is a member of.
   *
   * @param userName the user name, assumed not <code>null</code> or empty.
   * @return a list of role names, never <code>null</code> but may be empty.
   */
  protected List<String> findRoles(String userName) {
    return backEndRoleMgr.getRoles(userName);
  }

  /**
   * Validates that the user is already in the system.
   *
   * @param name never <code>null</code> or empty.
   */
  protected void checkUser(String name) throws PSDataServiceException {
    PSParameterValidationUtils.rejectIfBlank("checkUser", "name", name);

    boolean found = findUsername(name) != null || userLoginDao.find(name) != null;

    if (!found) {
      log.error("User not found {}", name);
      PSValidationErrorsBuilder builder =
          new PSValidationErrorsBuilder(PSUser.class.getCanonicalName());
      builder.reject("no.such.user", "User not found").throwIfInvalid();
    }
  }

  /**
   * Get the names of users matching the supplied filter.
   *
   * @param nameFilter <code>null</code> to find all users, "%" and "_" sql wildcards are supported.
   * @return Returns a list of matching subjects, never null, may be empty
   */
  private List<String> findUserNames(String nameFilter) {

    try {
      List<Subject> subjects = findExistingUsers(nameFilter);
      int size = subjects == null ? 0 : subjects.size();
      List<String> userNames = new ArrayList<>(size);

      if (subjects != null) {
        for (Subject s : subjects) {
          userNames.add(getUsername(s));
        }
        sort(userNames);
      } else {
        if (nameFilter == null) {
          nameFilter = "null";
        }
        log.warn("No users found for filter: {}", nameFilter);
      }
      return userNames;
    } catch (PSSecurityCatalogException e) {
      throw new RuntimeException(e);
    }
  }

  private void sort(List<String> names) {
    Collator coll = Collator.getInstance();
    Collections.sort(names, coll);
  }

  /**
   * Finds existing registered users of the system. This includes external and internal.
   *
   * @param name if <code>null</code> will find all existing users, "%" and "_" sql wildcards are
   *     supported.
   * @return never <code>null</code>.
   * @throws PSSecurityCatalogException If there are any errors.
   */
  private List<Subject> findExistingUsers(String name) throws PSSecurityCatalogException {
    List<String> names = name == null ? null : asList(name);
    return roleMgr.findUsers(names, "Default", "backend");
  }

  /**
   * Gets the email of the provided subject
   *
   * @param subjectName assumed not <code>null</code> or empty.
   * @return not <code>null</code> may be empty.
   * @throws PSSecurityCatalogException
   */
  private String getSubjectEmail(String subjectName) throws PSSecurityCatalogException {
    String email = "";
    List<Subject> subjects = findExistingUsers(subjectName);
    if (!subjects.isEmpty()) {
      Subject subject = subjects.get(0);
      PSSubject sub = PSJaasUtils.convertSubject(subject);
      if (sub != null) {
        PSAttributeList attrs = sub.getAttributes();
        PSAttribute attribute = attrs.getAttribute(EMAIL_ATTRIBUTE_NAME);
        if (attribute != null) {
          List<String> attrList = attribute.getValues();
          if (!attrList.isEmpty()) {
            email = attrList.get(0);
          }
        }
      }
    }
    return email;
  }

  /**
   * Finds the user name.
   *
   * @param name never <code>null</code>.
   * @return <code>null</code> if no user is found otherwise the first user name found.
   */
  private String findUsername(String name) {
    notNull(name);
    try {
      List<Subject> subjects = findExistingUsers(name);
      if (subjects != null && !subjects.isEmpty()) return getUsername(subjects.get(0));
    } catch (Exception e) {
      throw new PSDirectoryServiceException("Error while checking for user: " + name, e);
    }
    return null;
  }

  /**
   * Gets a user name from a subject.
   *
   * @param subject never <code>null</code>.
   * @return never <code>null</code>.
   * @throws NullPointerException if the subject does not have a proper public credential.
   */
  private String getUsername(Subject subject) {
    return subject.getPublicCredentials().iterator().next().toString();
  }

  private boolean isUser(Subject subject) {
    Set<IPSTypedPrincipal> ps = subject.getPrincipals(IPSTypedPrincipal.class);
    if (ps == null || ps.isEmpty()) return false;
    PrincipalTypes t = ps.iterator().next().getPrincipalType();
    return t == PrincipalTypes.USER || t == PrincipalTypes.SUBJECT;
  }

  @GET
  @Path("/external/find")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public List<PSExternalUser> findUsersFromDirectoryService() {
    return findUsersFromDirectoryService("%");
  }

  @Override
  @GET
  @Path("/external/find/{query}")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public List<PSExternalUser> findUsersFromDirectoryService(@PathParam("query") String query)
      throws PSDirectoryServiceException,
          PSDirectoryServiceConnectionException,
          PSDirectoryServiceDisabledException {
    if (query == null || StringUtils.isEmpty(query)) {
      query = "%";
    }

    // Replace * wildcards with %
    query = query.replace("*", "%");

    query = SecureStringUtils.sanitizeStringForLDAP(query, false);

    List<Subject> subjects;
    try {
      subjects =
          roleMgr.findUsers(
              Collections.singletonList(query), DIRECTORY_SET_NAME, "directorySet", null, true);
    } catch (PSSecurityCatalogException | PSSecurityException e) {
      log.error("General directory service failure: {}", PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      if (e.getMessage().contains("LDAP: error code 4 - Sizelimit Exceeded")) {
        throw new PSDirectoryServiceException(
            "The returned results exceeded LDAP server limit, please refine your search to get the"
                + " results.");
      } else if (e.getMessage().contains("timed out")) {
        throw new PSDirectoryServiceException(
            "The network connection to the remote LDAP server has timed out.  Please check server"
                + " network connectivity and try again.");
      }
      throw new PSDirectoryServiceException(e);
    } catch (IllegalArgumentException ae) {
      throw new PSDirectoryServiceDisabledException("No directory service enabled:", ae);
    }
    int size = subjects == null ? 0 : subjects.size();
    List<PSExternalUser> users = new ArrayList<>(size);
    if (subjects != null) {
      for (Subject s : subjects) {
        if (isUser(s)) {
          String userName = getUsername(s);
          users.add(new PSExternalUser(userName));
        }
      }
      Collections.sort(users);
    } else {
      log.warn("No users found in Directory Service matching query [{}]", query);
    }
    return users;
  }

  @Override
  @POST
  @Path("/import")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public List<PSImportedUser> importDirectoryUsers(PSImportUsers importUsers)
      throws PSDirectoryServiceException, PSSpringValidationException {
    PSUtilityService utilityService =
        (PSUtilityService) PSSpringBeanProvider.getBean("utilityService");

    try {
      PSParameterValidationUtils.rejectIfNull("importDirectoryUsers", "importUsers", importUsers);
      PSBeanValidationUtils.validate(importUsers).throwIfInvalid();
      List<PSExternalUser> users = importUsers.getExternalUsers();
      List<String> userNames = new ArrayList<>();

      for (PSExternalUser e : users) {
        userNames.add(e.getName());
        try {
          PSSystemAuditLogger.userCreate(
              currentServletRequest(), AuditOutcome.SUCCESS, e.getName());
        } catch (Exception auditEx) {
          log.error(PSExceptionUtils.getMessageForLog(auditEx));
          log.debug(PSExceptionUtils.getDebugMessageForLog(auditEx));
        }
      }

      List<PSImportedUser> importedUsers = importUsers(userNames);

      return new PSImportedUserList(importedUsers);
    } catch (PSValidationException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e.getMessage());
    }
  }

  /**
   * Imports a single user by setting the roles to that user. An exception will generally not be
   * thrown. Instead the return object will contain the exception along with whether or not it
   * succeeded.
   *
   * @param name user name.
   * @return never <code>null</code>.
   * @see ImportStatus
   */
  private PSImportedUser importUser(String name) {

    String user = null;
    ImportStatus status = null;

    try {
      user = findUsername(name);
    } catch (Exception e) {
      log.error(
          "While importing invalid  user name: {}. Error: {}",
          name,
          PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      status = ImportStatus.INVALID;
    }

    if (status == null && user != null) {
      status = ImportStatus.DUPLICATE;
    } else if (status == null) {
      try {
        updateRoles(name, PSRoleService.DEFAULT_IMPORTED_USER_ROLES);
        status = ImportStatus.SUCCESS;
      } catch (Exception e) {
        log.error("Error importing user: {} {}", name, PSExceptionUtils.getMessageForLog(e));
        log.debug(PSExceptionUtils.getDebugMessageForLog(e));
        status = ImportStatus.ERROR;
      }
    }

    PSImportedUser u = new PSImportedUser();
    u.setName(name);
    u.setStatus(status);
    return u;
  }

  /**
   * Imports several users by setting the roles to them. An exception will generally not be thrown.
   * Instead the return object will contain the status whether or not it succeeded.
   *
   * @param names list of user names to import.
   * @return list of imported users. Never <code>null</code>.
   * @see ImportStatus
   */
  private List<PSImportedUser> importUsers(List<String> names) {

    String user = null;
    ImportStatus status = null;
    List<String> updateRolesUsers = new ArrayList<>();
    List<PSImportedUser> users = new ArrayList<>();

    for (String name : names) {
      try {
        user = findUsername(name);

        if (status == null && user != null) {
          status = ImportStatus.DUPLICATE;
        } else if (status == null) {
          updateRolesUsers.add(name);
        }
      } catch (Exception e) {
        log.error(
            "While importing invalid  user name: {} Error: {}",
            name,
            PSExceptionUtils.getMessageForLog(e));
        log.debug(PSExceptionUtils.getDebugMessageForLog(e));
        status = ImportStatus.INVALID;
      }

      PSImportedUser u = new PSImportedUser();
      u.setName(name);
      u.setStatus(status);
      users.add(u);
    }

    try {
      updateRoles(names, PSRoleService.DEFAULT_IMPORTED_USER_ROLES);
      for (PSImportedUser singleUser : users) {
        if (singleUser.getStatus() == null) {
          singleUser.setStatus(ImportStatus.SUCCESS);
        }
      }
    } catch (Exception e) {
      log.error("Error importing users", e);
      for (PSImportedUser singleUser : users) {
        if (singleUser.getStatus() == null) {
          singleUser.setStatus(ImportStatus.ERROR);
        }
      }
    }

    return users;
  }

  @GET
  @Path("/external/status")
  @Override
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSDirectoryServiceStatus checkDirectoryService() {
    try {
      // No-arg overload exists for this exact purpose: probe the directory
      // service with a wildcard query so any directory / connectivity /
      // configuration failure surfaces as a PSDirectoryServiceException
      // carrying the relevant status. Do not invent a query parameter here.
      findUsersFromDirectoryService();
    } catch (PSDirectoryServiceException e) {
      return e.getStatus();
    }
    PSDirectoryServiceStatus status = new PSDirectoryServiceStatus();
    status.setStatus(ServiceStatus.ENABLED);
    return status;
  }

  /**
   * Set the system properties on this service. This service will always use the the values provided
   * by the most recently set instance of the properties.
   *
   * @param props the system properties
   */
  @Autowired
  public void setSystemProps(IPSSystemProperties props) {
    systemProps = props;

    // force to reset the cached data for next calls to getAccessibilityRoles()
    accessibilityRoles = null;
  }

  public IPSSystemProperties getSystemProps() {
    return systemProps;
  }

  private List<String> getAccessibilityRoles() {
    if (accessibilityRoles != null) return accessibilityRoles;

    accessibilityRoles = new ArrayList<>();
    String roles = systemProps.getProperty("accessibilityRoles");
    if (StringUtils.isBlank(roles)) return accessibilityRoles;

    String[] array = roles.split(",");
    accessibilityRoles = new ArrayList<>(Arrays.asList(array));

    return accessibilityRoles;
  }

  protected String getCurrentUserName() {
    return PSWebserviceUtils.getUserName();
  }

  /**
   * Check if user has admin role.
   *
   * @param userName
   */
  public boolean isAdminUser(String userName) {
    if (userName == null) {
      return false;
    }

    return findRoles(userName).contains(IPSRoleService.ADMINISTRATOR_ROLE);
  }

  @Override
  public boolean isDesignUser(String userName) {
    if (StringUtils.isBlank(userName)) return false;

    return findRoles(userName).contains(IPSRoleService.DESIGNER_ROLE);
  }

  /**
   * This is used to validate a {@link PSUser} object before updating an existing user or create a
   * new one.
   *
   * <p>This invocation of {@link #doValidation(PSUser, PSBeanValidationException)} is indirectly
   * done by {@link PSUserService#doValidation(PSUser, boolean)}.
   */
  protected class PSUserValidator extends PSAbstractBeanValidator<PSUser> {
    /** It is <code>true</code> if validating {@link PSUser} object for creating a user. */
    boolean isCreateUser = false;

    PSUserValidator(boolean isCreate) {
      this.isCreateUser = isCreate;
    }

    @Override
    protected void doValidation(PSUser user, PSBeanValidationException e) {
      try {
        // make sure all roles exist in the system.
        List<String> allRoles = backEndRoleMgr.getRoles();

        if (PSCollectionUtils.containsIgnoringCase(SYSTEM_USERS, user.getName())) {
          e.rejectValue(
              "name",
              "user.nameRestricted",
              "That user name is restricted for system use. Please choose a different user name");
        }
        /*
         * Lets not continue validating if it's already invalid.
         */
        if (e.hasErrors()) {
          return;
        }

        for (String rl : user.getRoles()) {
          if (!PSCollectionUtils.containsIgnoringCase(allRoles, rl)) {
            String msg =
                "Cannot add role \"" + rl + "\" because role named \"" + rl + "\" does not exist.";
            e.rejectValue("roles", "no.such.role", msg);
          }
        }

        if (isCreateUser) {
          // make sure created user not in the system
          boolean differByCase = false;
          String existingName = null;
          String newName = user.getName();
          List<PSUserLogin> users = userLoginDao.findByName(newName);
          if (users.size() > 1) {
            log.warn("Multiple user login entries found for name : {}", newName);
          }

          for (PSUserLogin usr : users) {
            String userId = usr.getUserid();
            if (userId.equals(newName)) {
              existingName = userId;
              break;
            } else if (userId.equalsIgnoreCase(newName)) {
              existingName = userId;
              differByCase = true;
              break;
            }
          }
          if (existingName != null) existingName = findUsername(newName);

          if (existingName != null) {
            String errorMsg =
                "Cannot create user \""
                    + user.getName()
                    + "\" because a user named \""
                    + existingName
                    + "\" already exists.";
            if (differByCase) {
              errorMsg += "  User names must differ by more than just case.";
            }
            log.debug(errorMsg);
            e.rejectValue("name", "not.create.existing.user", errorMsg);
          }
        } else {
          cannotRemoveAdminRoleByYourself(user, e);
        }
      } catch (IPSGenericDao.LoadException loadException) {
        e.addSuppressed(loadException);
      }
    }

    /**
     * Make sure the user cannot remove "Admin" role from he/she own profile.
     *
     * @param user the modified user profile, assumed not <code>null</code>.
     * @param e used to collect validation errors.
     */
    private void cannotRemoveAdminRoleByYourself(PSUser user, PSBeanValidationException e) {
      String current = getCurrentUserName();
      if (StringUtils.isBlank(user.getName())
          || !StringUtils.equalsIgnoreCase(user.getName(), current)) {
        return;
      }

      List<String> origRoles = findRoles(user.getName());

      if (PSCollectionUtils.containsIgnoringCase(origRoles, "Admin")
          && (!PSCollectionUtils.containsIgnoringCase(user.getRoles(), "Admin"))) {
        String emsg = "Cannot remove \"Admin\" role from your own profile";
        log.debug(emsg);
        e.rejectValue("roles", "cannot.remove.own.admin.role", emsg);
      }
    }
  }
}
