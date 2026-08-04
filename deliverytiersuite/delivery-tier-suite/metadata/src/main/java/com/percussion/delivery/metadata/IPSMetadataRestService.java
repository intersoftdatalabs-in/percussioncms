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

package com.percussion.delivery.metadata;

import com.percussion.delivery.metadata.data.*;
import com.percussion.delivery.services.IPSRestService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * JAX-RS resource surface for the DTS metadata micro-service. Wraps the {@link
 * IPSMetadataIndexerService} and the cookie-consent services so they can be exercised over HTTP by
 * the published-site clients.
 *
 * @author natechadwick
 */
public interface IPSMetadataRestService extends IPSRestService {

  /**
   * Process a metadata query and returns a list of metadata entries.
   *
   * <p>The metadata query may include a list of criteria, such as "dcterms:title like '%page%'".
   * Also it can contains max results and start index values to paginate, and an ordering setting.
   *
   * @param metadataQuery A PSMetadataQuery containing the query. Never <code>null</code>.
   * @return PSSearchResults object according to criteria.Which will have total entry count and list
   *     of PSMetadataRestEntry objects according the criteria. Never <code>null</code>, may be
   *     empty.
   */
  @POST
  @Path("/get")
  @Produces(MediaType.APPLICATION_JSON)
  public abstract PSSearchResults get(PSMetadataQuery metadataQuery);

  /**
   * Given a metadata query ({@link PSMetadataQuery}), it gets all pages according to it, then it
   * makes a list of tags of those pages, and retuns a metadata tag list ({@link
   * PSMetadataRestTagList} with tags and pages occurrences of each one).
   *
   * @param metadataQuery A PSMetadataQuery containing the query. Never <code>null</code>. by count
   *     of occurrences, in ascendant order. If this parameter is not set or different from 'count',
   *     it's alphabetically sorted, in descendant order.
   * @return A metadata tag list containing a list of {@link PSMetadataRestTagList} with the tag
   *     name and tag count (number of pages containing that tag). Never <code>null</code>, may be
   *     empty.
   */
  @POST
  @Path("/tags/get")
  @Produces(MediaType.APPLICATION_JSON)
  public abstract PSMetadataRestTagList getTags(PSMetadataQuery metadataQuery);

  /**
   * Returns a paginated list of blog entries that match the supplied metadata query.
   *
   * @param metadataQuery A PSMetadataQuery containing the query. Never <code>null</code>.
   * @return the blog aggregation result, never <code>null</code>, may be empty.
   */
  @POST
  @Path("/blog/getCurrent")
  @Produces(MediaType.APPLICATION_JSON)
  public abstract PSMetadataBlogResult getBlog(PSMetadataQuery metadataQuery);

  /**
   * Given a metadata query ({@link PSMetadataQuery}), it gets all pages according to it, then it
   * makes a list of categories of those pages, and returns a metadata category list ({@link
   * PSMetadataRestCategory} with categories and pages occurrences of each one.
   *
   * @param metadataQuery A PSMetadataQuery containing the query. Never <code>null</code>.
   * @return A metadata tag list containing a list of {@link PSMetadataRestCategory} with the
   *     category name, category count (number of pages containing that category and his children).
   *     Never <code>null</code>, may be empty.
   */
  @POST
  @Path("/categories/get")
  @Produces(MediaType.APPLICATION_JSON)
  public abstract List<PSMetadataRestCategory> getCategories(PSMetadataQuery metadataQuery);

  /**
   * Returns the list of blog posts that match the supplied metadata query.
   *
   * @param metadataQuery A PSMetadataQuery containing the query. Never <code>null</code>.
   * @return the blog list result, never <code>null</code>, may be empty.
   */
  @POST
  @Path("/blogs/get")
  @Produces(MediaType.APPLICATION_JSON)
  public abstract PSMetadataRestBlogList getBlogs(PSMetadataQuery metadataQuery);

  /**
   * Given a metadata query ({@link PSMetadataQuery}), it gets all pages according to it, then it
   * makes a list of pages, and returns a metadata event list ({@link PSMetadataDatedEntries} with
   * the pages that match the criteria.
   *
   * @param metadataQuery A PSMetadataQuery containing the query. Never <code>null</code>.
   * @return A metadata entries containing a list of {@link PSMetadataDatedEvent} with the title,
   *     summary, start date and end date name. Never <code>null</code>, may be empty.
   */
  @POST
  @Path("/dated/get")
  @Produces(MediaType.APPLICATION_JSON)
  public abstract PSMetadataDatedEntries getDatedEntries(PSMetadataQuery metadataQuery);

  /**
   * Method to charge the call to the indexer to delete the metadatas entries.
   *
   * @param pagepaths A pagepaths containing the collection of metadatas entries. Never <code>null
   *     </code>.
   */
  @POST
  @Path("/delete")
  @RolesAllowed("deliverymanager")
  public abstract void delete(Collection<String> pagepaths);

  /**
   * Method to charge the call to the indexer to remove indexed directories that no longer exist.
   * The scanner gets a list of indexed directories, and if they now longer exist, they are removed
   * using cleanFolderIndexes(String) method.
   *
   * @return Set String. Never <code>null</code>.
   */
  @GET
  @Path("/indexedDirectories")
  @Produces(MediaType.APPLICATION_JSON)
  public abstract Set<String> getAllIndexedDirectories();

  /**
   * Returns the indexer service that backs this REST resource.
   *
   * @return the indexerService, never <code>null</code>.
   */
  public abstract IPSMetadataIndexerService getIndexerService();

  /**
   * Replaces the indexer service that backs this REST resource.
   *
   * @param indexerService the indexerService to set; may not be <code>null</code>.
   */
  public abstract void setIndexerService(IPSMetadataIndexerService indexerService);

  /**
   * Method to update a category in the DTS when it is modified for any of its property. The method
   * is responsible to update the relevant DTS based on the request that was made.
   *
   * @param category - The updated category json String.
   * @param sitename - Site in which the category is modified
   * @param deliveryserver - Staging or Production
   * @return the response payload describing the outcome of the category update; never <code>null
   *     </code>.
   */
  @POST
  @Path("/categories/update/{sitename}/{deliveryserver}")
  @Consumes(MediaType.APPLICATION_JSON)
  public abstract String updateCategoryInDTS(
      String category,
      @PathParam("sitename") String sitename,
      @PathParam("deliveryserver") String deliveryserver);

  /**
   * Reports the running status of the visit tracking scheduler.
   *
   * @return the status payload, never <code>null</code>.
   */
  @GET
  @Path("/visits/status")
  @Produces(MediaType.APPLICATION_JSON)
  public abstract String getVisitServiceStatus();

  /**
   * Returns the most-visited blog posts matching the supplied visit query.
   *
   * @param visitQuery the visit query; may be <code>null</code>.
   * @return the list of blog post entries, never <code>null</code>, may be empty.
   */
  @GET
  @Path("/topblogposts")
  @Produces(MediaType.APPLICATION_JSON)
  public abstract List<PSMetadataRestEntry> getTopVisitedBlogPosts(PSVisitQuery visitQuery);

  /**
   * Tracks a blog post visit captured from a client.
   *
   * @param visitEntry the visit entry to record; may be <code>null</code>.
   */
  @POST
  @Path("/trackblogpost")
  @Consumes(MediaType.APPLICATION_JSON)
  public abstract void trackBlogPost(PSVisitRestEntry visitEntry);

  /**
   * Saves a client cookie consent request.
   *
   * @param consentQuery - object with required information to save cookie consent:
   * @param req - HTTP request used to grab IP.
   */
  @POST
  @Path("/consent/log")
  @Consumes(MediaType.APPLICATION_JSON)
  public abstract void saveCookieConsent(
      PSCookieConsentQuery consentQuery, @Context HttpServletRequest req);

  /**
   * Gets all cookie consent entries in .CSV format.
   *
   * @param csvFileName - the name of the file.
   * @return A .CSV file never <code>null</code>. May be empty.
   */
  @GET
  @Path("/consent/log/{csvFileName}")
  @Produces({"text/csv"})
  @RolesAllowed("deliverymanager")
  public abstract Response exportAllSiteCookieConsentStats(
      @PathParam("csvFileName") String csvFileName);

  /**
   * Gets all cookie consent entries for the supplied site in .CSV format.
   *
   * @param siteName - the site the cookie consent entries are reported for; may not be {@code
   *     null}.
   * @param csvFileName - the name of the file to produce.
   * @return A .CSV file never <code>null</code>. May be empty.
   */
  @GET
  @Path("/consent/log/{siteName}/{csvFileName}")
  @Produces({"text/csv"})
  @RolesAllowed("deliverymanager")
  public abstract Response exportSiteCookieConsentStats(
      @PathParam("siteName") String siteName, @PathParam("csvFileName") String csvFileName);

  /**
   * Gets the total consent entries for all sites.
   *
   * @return A key/value pair with sitename/total as pair.
   */
  @GET
  @Path("/consent/log/totals")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("deliverymanager")
  public abstract Map<String, Integer> getAllCookieConsentTotals();

  /**
   * Returns cookie consent entries per site with totals for each service/cookie that was approved
   * by the client.
   *
   * @param siteName - the name of the site to find entries for.
   * @return A map representation of services/totals as key/value pair. May be empty, never <code>
   *     null</code>.
   */
  @GET
  @Path("/consent/log/totals/{siteName}")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("deliverymanager")
  public abstract Map<String, Integer> getCookieConsentEntriesPerSite(
      @PathParam("siteName") String siteName);

  /**
   * Deletes all cookie consent entries from the DB.
   *
   * @return HTTP response indicating success or failure
   */
  @DELETE
  @Path("/consent/log")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("deliverymanager")
  public abstract Response deleteAllCookieConsentEntries();

  /**
   * Deletes cookie consent entries for a site.
   *
   * @param siteName - the site in which to delete the cookie consent entries for.
   * @return HTTP response indicating success or failure.
   */
  @DELETE
  @Path("/consent/log/{siteName}")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("deliverymanager")
  public abstract Response deleteCookieConsentEntriesForSite(
      @PathParam("siteName") String siteName);
}
