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

package com.percussion.services.integrations.siteimprove;

import com.percussion.services.integrations.IPSIntegrationProviderService;

import junit.framework.TestCase;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

//Remove @Ignore in order to run tests, these tests manually test siteimprove endpoints.
@Ignore
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PSSiteImproveProviderServiceTest extends TestCase {

	// Testing Resources
	private static final String TESTING_USER = "percussionbot@gmail.com";
	private static final String TESTING_TOKEN = "388c3dc6a18b9582baada754b1408b7e";
	private static final String TESTING_SITE = "https://www.percussion.com/";
	private static final String TESTING_PAGE = TESTING_SITE + "products/";
	private static final String EMAIL = "email";
	private static final String APIKEY = "apikey";

	private static IPSIntegrationProviderService providerService = new PSSiteImproveProviderService();
	private static Map<String, String> testingCredentials = new HashMap<String, String>();
	private static String siteId;


	@Override
	public void setUp() {
		testingCredentials.put(EMAIL, TESTING_USER);
		testingCredentials.put(APIKEY, TESTING_TOKEN);
	}
	
	//To prevent the test suite from failing.
	public void testPlaceholderTest() throws Exception{
	   
	}

	@Test
	public void validateCredentialsTest() throws Exception {
		//Ensure a valid username and email validates properly.
		Assert.assertTrue(providerService.validateCredentials(testingCredentials));
	}

	@Test
	public void validateInvalidCredentialsTest() throws Exception {
		// Try invalid usernames and emails
		Map<String, String> badCredentials = new HashMap<String, String>();
		badCredentials.put(EMAIL, UUID.randomUUID().toString());
		badCredentials.put(APIKEY, UUID.randomUUID().toString());
		Map<String, String> otherBadCredentials = new HashMap<String, String>(badCredentials);
		Assert.assertFalse(providerService.validateCredentials(badCredentials));

		//Remove email
		try {
			badCredentials.remove(EMAIL);
			providerService.validateCredentials(badCredentials);
		} catch (Exception e) {
			assertTrue(e.getMessage().equals("Missing either email or apikey to validate"));
			assertTrue(e instanceof Exception);
		}

		//Remove apikey
		try {
			otherBadCredentials.remove(APIKEY);
			providerService.validateCredentials(otherBadCredentials);
		} catch (Exception e) {
			assertTrue(e.getMessage().equals("Missing either email or apikey to validate"));
			assertTrue(e instanceof Exception);
		}

		Map<String, String> emptyCredentials = new HashMap<String, String>();

		//Try empty credentials
		try {
			providerService.validateCredentials(emptyCredentials);
		} catch (Exception e) {
			assertTrue(e.getMessage().equals("Missing either email or apikey to validate"));
			assertTrue(e instanceof Exception);
		}

		//Try null credentials
		try {
			providerService.validateCredentials(null);
		} catch (Exception e) {
			assertTrue(e instanceof NullPointerException);
		}
	}


	@Test
	public void a_SiteImproveGetSiteTest() throws Exception {
		siteId = providerService.retrieveSiteInfo(TESTING_SITE, testingCredentials);
		Assert.assertTrue(!siteId.isEmpty());
	}

	@Test
	public void b_SiteImproveGetPageTest() throws Exception {

		String pageId = providerService.retrievePageInfo(siteId, TESTING_PAGE, testingCredentials);
		Assert.assertTrue(!pageId.isEmpty());
	}

	@Test
	public void c_SiteImproveUpdatePageTest() throws Exception {
		providerService.updatePageInfo(siteId, TESTING_PAGE, testingCredentials);
		Thread.sleep(2000);
	}

	@Test
	public void d_SiteImproveUpdateSiteTest() throws Exception {
		providerService.updateSiteInfo(siteId, testingCredentials);
		Thread.sleep(2000);
	}

	@Test
	public void siteImproveBadGetSiteTest() throws Exception {

		Map<String, String> badTestingCredentials = new HashMap<String, String>();
		badTestingCredentials.put(EMAIL, UUID.randomUUID().toString());
		badTestingCredentials.put(APIKEY, UUID.randomUUID().toString());

		try {
			providerService.retrieveSiteInfo(TESTING_SITE, badTestingCredentials);
		} catch (Exception e) {
			Assert.assertNotNull(e);
		}

		//try combination of nulls
		try {
			providerService.retrieveSiteInfo(TESTING_SITE, null);
		} catch (Exception e) {
			assertTrue(e instanceof NullPointerException);
			try {
				providerService.retrieveSiteInfo(null, Collections.<String, String>emptyMap());
			} catch (Exception ex) {
				assertTrue(ex instanceof NullPointerException);
				try {
					providerService.retrieveSiteInfo("", Collections.<String, String>emptyMap());
				} catch (Exception exe) {
					assertTrue(exe instanceof NullPointerException);
				}
			}
		}

		try {
			providerService.retrieveSiteInfo(UUID.randomUUID().toString(), testingCredentials);
		} catch (Exception e) {
			Assert.assertNotNull(e);
		}

	}

	@Test
	public void siteImproveBadGetPageTest() throws Exception {

		try {
			providerService.retrievePageInfo(UUID.randomUUID().toString(), TESTING_PAGE, testingCredentials);
		} catch (Exception e) {
			Assert.assertNotNull(e);
		}

		//try combination of empty string and nulls
		try {
			providerService.retrievePageInfo(null, null, testingCredentials);
		} catch (Exception e) {
			assertTrue(e instanceof NullPointerException);
			try {
				providerService.retrievePageInfo(null, "", testingCredentials);
			} catch (Exception ex) {
				assertTrue(ex instanceof NullPointerException);
			}
		}
	}

	@Test
	public void siteImproveBadUpdateSiteTest() throws Exception {

		try {
			providerService.updateSiteInfo(UUID.randomUUID().toString(), testingCredentials);
		} catch (Exception e) {
			Assert.assertNotNull(e);
		}

		try {
			providerService.updateSiteInfo(null, testingCredentials);
		} catch (Exception e) {
			assertTrue(e instanceof NullPointerException);
			try {
				providerService.updateSiteInfo("", testingCredentials);
			} catch (Exception ex) {
				assertTrue(ex instanceof NullPointerException);
			}
		}

	}

	@Test
	public void siteImproveBadUpdatePageTest() throws Exception {

		try {
			providerService.updatePageInfo(null, UUID.randomUUID().toString(), testingCredentials);
		} catch (Exception e) {
			Assert.assertNotNull(e);
		}

		try {
			providerService.updatePageInfo(null, null, testingCredentials);
		} catch (Exception e) {
			assertTrue(e instanceof NullPointerException);
			try {
				providerService.updatePageInfo(null, "", testingCredentials);
			} catch (Exception ex) {
				assertTrue(ex instanceof NullPointerException);
			}
		}
	}

}
