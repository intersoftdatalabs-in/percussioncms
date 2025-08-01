        context.assertIsSatisfied();
/*

    public void testUpdateRoles() {
        // Placeholder for future role update tests
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
    public void testFindRoles() {
        var roles = new ArrayList<String>();
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
        roles.add("c");
        context.checking(new Expectations() {{
 * limitations under the License.
 */


        var result = cut.findRoles("fred");
import com.percussion.role.service.impl.PSRoleService;
        assertTrue(result.contains("c"));
        context.assertIsSatisfied();
import com.percussion.services.security.IPSBackEndRoleMgr;
import com.percussion.services.security.IPSRoleMgr;
    @Test
import com.percussion.services.workflow.IPSWorkflowService;
        context.checking(new Expectations() {{
import com.percussion.share.dao.IPSGenericDao;
            will(returnValue(null));
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.sitemanage.dao.IPSUserLoginDao;
import com.percussion.user.data.PSCurrentUser;
        assertThrows(PSValidationException.class, () -> cut.checkUser("fred"));
        context.assertIsSatisfied();
import com.percussion.user.data.PSUserLogin;
import com.percussion.user.data.PSUserProviderType;
    private static abstract class MockCurrentUserName {
        public abstract String mockName();
    }

    private class TestUserService extends PSUserService {
import org.hamcrest.core.CombinableMatcher;
                                IPSBackEndRoleMgr backendRoleMgr, IPSRoleMgr roleMgr, IPSNotificationService notificationService,
                                IPSWorkflowService workflowService, IPSSecurityWs securityWs, IPSContentWs contentWs, IPSIdMapper idMapper, IPSUtilityService utilityService) {
            super(userLoginDao, passwordFilter, backendRoleMgr, roleMgr, notificationService, workflowService, securityWs, contentWs, idMapper, utilityService);
import org.junit.jupiter.api.Test;

import javax.security.auth.Subject;
        protected String getCurrentUserName() {
import java.util.Collections;
import java.util.List;

        @Override
        public PSCurrentUser getCurrentUser() {
            var user = new PSCurrentUser();
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.jupiter.api.Assertions.*;

public class PSUserServiceMockTest {

    private static final Logger log = LogManager.getLogger(PSUserServiceMockTest.class);

    Mockery context;
    PSUserService cut;
    IPSBackEndRoleMgr backendRoleMgr;
    IPSRoleMgr roleMgr;
    IPSUserLoginDao dao;
    IPSPasswordFilter filter;
    IPSFolderHelper folderHelper;
    IPSWorkflowService workflowService;
    IPSSecurityWs securityWs;
    IPSContentWs contentWs;
    IPSIdMapper idMapper;
    List<String> validRoles;
    IPSNotificationService notificationService;
    IPSUtilityService utilityService;

    MockCurrentUserName mockUserName = new MockCurrentUserName() {
        @Override
        public String mockName() {
            return "dummy-admin";
        }
    };

    @BeforeEach
    public void setUp() throws Exception {
        context = new Mockery();
        backendRoleMgr = context.mock(IPSBackEndRoleMgr.class);
        roleMgr = context.mock(IPSRoleMgr.class);
        notificationService = context.mock(IPSNotificationService.class);
        dao = context.mock(IPSUserLoginDao.class);
        filter = context.mock(IPSPasswordFilter.class);
        folderHelper = context.mock(IPSFolderHelper.class);
        workflowService = context.mock(IPSWorkflowService.class);
        securityWs = context.mock(IPSSecurityWs.class);
        contentWs = context.mock(IPSContentWs.class);
        utilityService = context.mock(IPSUtilityService.class);

        cut = new TestUserService(dao, filter, backendRoleMgr, roleMgr, notificationService, workflowService, securityWs, contentWs, idMapper, utilityService);

        validRoles = new ArrayList<>();
        validRoles.add("a");
        validRoles.add("b");
        validRoles.add("c");
        validRoles.addAll(PSRoleService.DEFAULT_ROLES);

        context.checking(new Expectations() {{
            allowing(backendRoleMgr).getRhythmyxRoles();
            will(returnValue(validRoles));
        }});
    }

    private void expectFindByName(final String name) throws IPSGenericDao.LoadException {
        final var user = new PSUserLogin();
        user.setUserid(name);
        context.checking(new Expectations() {{
            one(dao).findByName(name);
            will(returnValue(asList(user)));
        }});
    }

    @Test
    public void testValid() throws IPSGenericDao.LoadException, PSValidationException {
        var user = new PSUser();
        user.setName("Fred9");
        user.getRoles().add("a");

        expectFindByName("Fred9");
        cut.doValidation(user, false);
        assertTrue(true);
    }

    @Test
    public void testValidBadUser() {
        var user = new PSUser();
        user.setName("Fred9!Z");
        user.getRoles().add("a");
        assertThrows(PSValidationException.class, () -> cut.doValidation(user, false));
    }

    @Test
    public void testValidDirectoryUserWithBadCharactersForInternalNames() throws PSValidationException {
        var user = new PSUser();
        user.setName("Fred9!Z");
        user.getRoles().add("a");
        user.setProviderType(PSUserProviderType.DIRECTORY);
        cut.doValidation(user, false);
    }

    @Test
    public void shouldFailCreatingUserWithNoRoles() {
        var user = new PSUser();
        user.setName("Fred9!Z");
        assertTrue(user.getRoles().isEmpty());
        assertThrows(PSValidationException.class, () -> cut.doValidation(user, false));
    }

    @Test
    public void testValidNoUser() {
        var user = new PSUser();
        user.setName(null);
        user.getRoles().add("a");
        assertThrows(PSValidationException.class, () -> cut.doValidation(user, false));
    }

    @Test
    public void testValidTooLongUser() {
        var user = new PSUser();
        user.setName("Fred123456789012345678901234567890123456789012345678901234567890");
        user.getRoles().add("a");
        assertThrows(PSValidationException.class, () -> cut.doValidation(user, false));
    }

    @Test
    public void testValidBadRole() throws IPSGenericDao.LoadException {
        var user = new PSUser();
        user.setName("fred");
        user.getRoles().add("q");
        expectFindByName("fred");
        assertThrows(PSValidationException.class, () -> cut.doValidation(user, false));
    }

    @Test
    public void testCreate() throws Exception {
        var user = new PSUser();
        user.setName("fred");
        user.setPassword("secret");
        user.setEmail("fred@yahoo.com");
        user.getRoles().add("a");
        var rl = user.getRoles();
        rl.addAll(PSRoleService.DEFAULT_ROLES);

        context.checking(new Expectations() {{
            one(filter).encrypt("secret");
            will(returnValue("super-secret"));
            var rvalue = new PSUserLogin();
            one(dao).create(with(any(PSUserLogin.class)));
            will(returnValue(rvalue));
            one(backendRoleMgr).setRhythmyxRoles(with("fred"), with(1), with(hasRoles(asList("a"))));
            one(dao).findByName("fred");
            will(returnValue(new ArrayList<PSUserLogin>()));
            one(backendRoleMgr).setSubjectEmail(with("fred"), with("fred@yahoo.com"));
        }});

        var actual = cut.create(user);

        assertThat(actual.getPassword(), is(nullValue()));
        assertEquals(actual.getEmail(), "fred@yahoo.com");

        context.assertIsSatisfied();
    }

    @SuppressWarnings("unchecked")
    private static Matcher<Collection<String>> hasRoles(List<String> roles) {
        return CombinableMatcher.<Collection<String>>both(hasItems(roles.toArray(new String[]{})))
                .and(hasItems(PSRoleService.DEFAULT_ROLES.toArray(new String[]{})));
    }

    @Test
    public void testDelete() throws Exception {
        var login = new PSUserLogin();
        login.setUserid("fred");
        context.checking(new Expectations() {{
            allowing(dao).find("fred");
            will(returnValue(login));
            one(dao).delete("fred");
            one(backendRoleMgr).setRhythmyxRoles("fred", 1, Collections.<String>emptyList());
            one(roleMgr).findUsers(asList("fred"), "Default", "backend");
            will(returnValue(Collections.<Subject>emptyList()));
        }});

        cut.delete("fred");
        context.assertIsSatisfied();
    }

    @Test
    public void testNoDeleteSelf() throws Exception {
        log.info("deleting self user");
        var login = new PSUserLogin();
        context.checking(new Expectations() {{
            allowing(dao).find("dummy-admin");
            will(returnValue(login));
            one(roleMgr).findUsers(asList("dummy-admin"), "Default", "backend");
            will(returnValue(Collections.<Subject>emptyList()));
        }});

        assertThrows(PSValidationException.class, () -> cut.delete("dummy-admin"));
    }

    @Test
    public void testFind() throws Exception {
        var login = new PSUserLogin();
        var roles = new ArrayList<String>();
        roles.add("a");
        login.setUserid("fred");
        context.checking(new Expectations() {{
            allowing(dao).find("fred");
            will(returnValue(login));
            one(backendRoleMgr).getRhythmyxRoles("fred", 1);
            will(returnValue(roles));
            atMost(2).of(roleMgr).findUsers(asList("fred"), "Default", "backend");
            will(returnValue(Collections.<Subject>emptyList()));
        }});

        var result = cut.find("fred");
        assertNotNull(result);
        assertEquals("fred", result.getName());
        assertNull(result.getPassword());
        assertTrue(result.getRoles().contains("a"));
        context.assertIsSatisfied();
    }

    @Test
    public void shouldGetCurrentUser() throws Exception {
        var actual = new PSUserLogin();
        var roles = new ArrayList<String>();
        roles.add("a");
        actual.setUserid("dummy-admin");

        context.checking(new Expectations() {{
            allowing(dao).find("dummy-admin");
            will(returnValue(actual));
            one(backendRoleMgr).getRhythmyxRoles("dummy-admin", 1);
            will(returnValue(roles));
            atMost(2).of(roleMgr).findUsers(asList("dummy-admin"), "Default", "backend");
            will(returnValue(Collections.<Subject>emptyList()));
        }});

        var result = cut.getCurrentUser();
        assertNotNull(result, "Result cannot be null");
        assertThat(result.getName(), is("dummy-admin"));
    }

    @Test
    public void testGetRoles() throws PSDataServiceException {
        var rl = cut.getRoles();
        var roles = rl.getRoles();
        assertNotNull(roles);
        assertEquals(3, roles.size());
    }

    @Test
    public void testGetUsers() throws Exception {
        var logins = new ArrayList<PSUserLogin>();
        var l1 = new PSUserLogin();
        l1.setUserid("fred");
        logins.add(l1);
        l1 = new PSUserLogin();
        l1.setUserid("Bob");
        logins.add(l1);
        l1 = new PSUserLogin();
        l1.setUserid("alice");
        logins.add(l1);

        context.checking(new Expectations() {{
            one(roleMgr).findUsers(null, "Default", "backend");
            will(returnValue(createMockSubject("fred", "Bob", "alice")));
        }});

        var result = cut.getUsers();
        assertNotNull(result);
        var u = result.getUsers();
        assertNotNull(u);
        assertEquals(3, u.size());
        assertEquals("alice", u.get(0));
        assertEquals("Bob", u.get(1));
        context.assertIsSatisfied();
    }

    private List<Subject> createMockSubject(String... users) {
        var subs = new ArrayList<Subject>();
        for (var u : users) {
            var s = new Subject();
            s.getPrincipals().add(new PSTypedPrincipal(u, PrincipalTypes.SUBJECT));
            subs.add(s);
            s.getPublicCredentials().add(u);
        }
        return subs;
    }

    @Test
    public void testUpdate() throws PSDataServiceException {
        var user = new PSUser();
        var login = new PSUserLogin();
        user.setName("fred");
        user.setPassword("secret");
        user.setEmail("fred@yahoo.com");
        var roles = Collections.<String>emptyList();
        context.checking(new Expectations() {{
            allowing(dao).find("fred");
            will(returnValue(login));
            one(filter).encrypt("secret");
            will(returnValue("super-secret"));
            one(dao).save(with(any(PSUserLogin.class)));
            one(backendRoleMgr).setRhythmyxRoles(with("fred"), with(1), with(hasRoles(roles)));
            one(backendRoleMgr).setSubjectEmail(with("fred"), with("fred@yahoo.com"));
        }});

        var actual = cut.update(user);
        assertThat(actual.getPassword(), is(nullValue()));
        context.assertIsSatisfied();
    }

    @Test
    public void testChangePassword() throws PSDataServiceException {
        var user = new PSUser();
        var login = new PSUserLogin();
        user.setName("fred");
        user.setPassword("secret");
        user.setEmail("fred@yahoo.com");
        context.checking(new Expectations() {{
            allowing(dao).find("fred");
            will(returnValue(login));
            one(filter).encrypt("secret");
            will(returnValue("super-secret"));
            one(dao).save(with(any(PSUserLogin.class)));
        }});

        var actual = cut.changePassword(user);
        assertThat(actual.getPassword(), is(nullValue()));
        context.assertIsSatisfied();
    }

    @Test
    public void testUpdateSelfNoAdmin() {
        var user = new PSUser();
        user.setName("dummy-admin");
        user.setPassword("secret");
        var roles = new ArrayList<String>();
        roles.add("a");
        roles.add("Admin");

        context.checking(new Expectations() {{
            one(backendRoleMgr).getRhythmyxRoles("dummy-admin", 1);
            will(returnValue(roles));
            one(dao).find("dummy-admin");
            will(returnValue(null));
        }});

        assertThrows(PSValidationException.class, () -> cut.update(user));
    }
    
    public void testUpdateRoles()
    {
        
    }

    @Test
    public void testFindRoles()
    {
        final List<String> roles = new ArrayList<String>();
        roles.add("a");
        roles.add("c"); 
        context.checking(new Expectations(){{
            one(backendRoleMgr).getRhythmyxRoles("fred", 1);
            will(returnValue(roles));
        }});
        
        List<String> result = cut.findRoles("fred");
        assertNotNull(result);
        assertTrue(result.contains("c")); 
        
        context.assertIsSatisfied(); 
        
    }

    @Test(expected=PSValidationException.class)
    public void testCheckUser() throws PSSecurityCatalogException, PSDataServiceException {
        context.checking(new Expectations(){{
            one(dao).find("fred");
            will(returnValue(null)); 
            one(roleMgr).findUsers(asList("fred"), "Default", "backend");
            will(returnValue(Collections.<Subject>emptyList()));
        }});

        cut.checkUser("fred");

        context.assertIsSatisfied(); 
    }

    
    private class TestUserService extends PSUserService
    {
 
        
        
        private TestUserService(IPSUserLoginDao userLoginDao, IPSPasswordFilter passwordFilter,
                IPSBackEndRoleMgr backendRoleMgr, IPSRoleMgr roleMgr, IPSNotificationService notificationService,
                IPSWorkflowService workflowService, IPSSecurityWs securityWs, IPSContentWs contentWs, IPSIdMapper idMapper, IPSUtilityService utilityService)
        {
            super(userLoginDao, passwordFilter, backendRoleMgr, roleMgr, notificationService, workflowService, securityWs, contentWs,idMapper,utilityService);
        }

        @Override
        protected String getCurrentUserName()
        {
            return mockUserName.mockName();
        }
        
        public PSCurrentUser getCurrentUser()
        {
            PSCurrentUser user = new PSCurrentUser();
            user.setName("dummy-admin");
            return user;
        }
        
    }
    
    protected static abstract class MockCurrentUserName {
        public abstract String mockName();
    }
}
