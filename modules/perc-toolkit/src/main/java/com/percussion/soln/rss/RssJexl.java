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

package com.percussion.soln.rss;

import static org.apache.commons.collections.CollectionUtils.filter;

import com.percussion.extension.IPSExtensionDef;
import com.percussion.extension.IPSJexlExpression;
import com.percussion.extension.PSExtensionException;
import com.percussion.services.assembly.IPSAssemblyService;
import com.percussion.services.assembly.IPSAssemblyTemplate;
import com.percussion.services.assembly.IPSAssemblyTemplate.OutputFormat;
import com.percussion.services.assembly.PSAssemblyException;
import com.percussion.services.assembly.PSAssemblyServiceLocator;
import com.percussion.services.assembly.jexl.PSLocationUtils;
import com.percussion.soln.jcr.NodeUtils;
import com.rometools.rome.feed.synd.*;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedOutput;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;
import javax.jcr.Node;
import org.apache.commons.collections.Predicate;

/**
 * RssJexl class.
 */
public class RssJexl implements IPSJexlExpression {
  /**
   * Creates a new RssJexl.
   */
  public RssJexl() {
    // default
  }


  private PSLocationUtils locationUtils;
  private IPSAssemblyService assemblyService;

  /**
   * createEntries operation.
   *
   * @return the result
   */
  public List<SyndEntry> createEntries() {
    return new ArrayList<>();
  }

  /**
   * createFeed operation.
   *
   * @return the result
   */
  public SyndFeed createFeed() {
    return new SyndFeedImpl();
  }

  /**
   * createFeed operation.
   *
   * @param node the node
   * @param titleFields the title fields
   * @param bodyFields the body fields
   * @return the result
   */
  public SyndFeed createFeed(Node node, String titleFields, String bodyFields) {
    SyndFeed feed = createFeed();
    String title = getValue(node, titleFields);
    feed.setTitle(title);
    String body = getValue(node, bodyFields);
    feed.setDescription(body);
    return feed;
  }

  /**
   * createEntry operation.
   *
   * @return the result
   */
  public SyndEntry createEntry() {
    return new SyndEntryImpl();
  }

  /**
   * createEntry operation.
   *
   * @param node the node
   * @param titleFields the title fields
   * @param bodyFields the body fields
   * @return the result
   */
  public SyndEntry createEntry(Node node, String titleFields, String bodyFields) {
    SyndEntry entry = createEntry();
    String title = getValue(node, titleFields);
    entry.setTitle(title);
    entry.setUpdatedDate(new Date());
    entry.setAuthor(getValue(node, "author"));
    String description = getValue(node, bodyFields);
    SyndContent content = createContent();
    content.setType("text/html");
    content.setValue(description);
    entry.setDescription(content);
    return entry;
  }

  /**
   * Returns the value.
   *
   * @param node the node
   * @param titleFields the title fields
   * @return the result
   */
  protected String getValue(Node node, String titleFields) {
    return locationUtils.getFirstDefined(node, titleFields, "");
  }

  /**
   * createContent operation.
   *
   * @return the result
   */
  public SyndContent createContent() {
    return new SyndContentImpl();
  }

  /**
   * Returns the rss.
   *
   * @param feed the feed
   * @return the result
   * @throws IOException if an error occurs
   * @throws FeedException if an error occurs
   */
  public String getRss(SyndFeed feed) throws IOException, FeedException {
    feed.setFeedType("rss_2.0");
    return feedToString(feed);
  }

  /**
   * Returns the atom.
   *
   * @param feed the feed
   * @return the result
   * @throws IOException if an error occurs
   * @throws FeedException if an error occurs
   */
  public String getAtom(SyndFeed feed) throws IOException, FeedException {
    feed.setFeedType("atom_1.0");
    return feedToString(feed);
  }

  /**
   * feedToString operation.
   *
   * @param feed the feed
   * @return the result
   * @throws IOException if an error occurs
   * @throws FeedException if an error occurs
   */
  public String feedToString(SyndFeed feed) throws IOException, FeedException {
    StringWriter writer = new StringWriter();
    SyndFeedOutput output = new SyndFeedOutput();
    output.output(feed, writer);
    writer.close();
    return writer.getBuffer().toString();
  }

  /**
   * init operation.
   *
   * @param arg0 the arg0
   * @param arg1 the arg1
   * @throws PSExtensionException if an error occurs
   */
  public void init(IPSExtensionDef arg0, File arg1) throws PSExtensionException {
    setLocationUtils(new PSLocationUtils());
    setAssemblyService(PSAssemblyServiceLocator.getAssemblyService());
  }

  /**
   * Sets the assembly service.
   *
   * @param assemblyService the assembly service
   */
  public void setAssemblyService(IPSAssemblyService assemblyService) {
    this.assemblyService = assemblyService;
  }

  /**
   * Sets the location utils.
   *
   * @param locationUtils the location utils
   */
  public void setLocationUtils(PSLocationUtils locationUtils) {
    this.locationUtils = locationUtils;
  }

  /**
   * findEntryTemplate operation.
   *
   * @param node the node
   * @return the result
   * @throws PSAssemblyException if an error occurs
   */
  public String findEntryTemplate(Node node) throws PSAssemblyException {
    String contentType = getContentType(node);
    Collection<IPSAssemblyTemplate> templates =
        findTemplates("rss.*entry", contentType, "text/xml", OutputFormat.Snippet);
    return pickTemplate(templates);
  }

  /**
   * findFeedTemplate operation.
   *
   * @param node the node
   * @return the result
   * @throws PSAssemblyException if an error occurs
   */
  public String findFeedTemplate(Node node) throws PSAssemblyException {
    String contentType = getContentType(node);
    Collection<IPSAssemblyTemplate> templates =
        findTemplates("rss.*feed", contentType, "text/xml", OutputFormat.Page);
    return pickTemplate(templates);
  }

  /**
   * Returns the content type.
   *
   * @param node the node
   * @return the result
   */
  protected String getContentType(Node node) {
    return NodeUtils.getContentType(node);
  }

  private String pickTemplate(Collection<IPSAssemblyTemplate> templates) {
    if (templates.isEmpty()) return null;
    return templates.iterator().next().getName();
  }

  /**
   * findTemplates operation.
   *
   * @param name the name
   * @param description the description
   * @param mimeType the mime type
   * @param format the format
   * @return the result
   * @throws PSAssemblyException if an error occurs
   */
  public Collection<IPSAssemblyTemplate> findTemplates(
      final String name, final String description, final String mimeType, final OutputFormat format)
      throws PSAssemblyException {

    Collection<IPSAssemblyTemplate> templates = findAllTemplates();
    templates = new ArrayList<>(templates);
    final Pattern np = name == null ? null : Pattern.compile(name, Pattern.CASE_INSENSITIVE);
    final Pattern dp =
        description == null ? null : Pattern.compile(description, Pattern.CASE_INSENSITIVE);
    final Pattern mt =
        mimeType == null ? null : Pattern.compile(mimeType, Pattern.CASE_INSENSITIVE);

    filter(
        templates,
        new TemplatePredicate() {
          /**
           * evalTemplate operation.
           *
           * @param t the t
           * @return the result
           */
          @Override
          public boolean evalTemplate(IPSAssemblyTemplate t) {
            String name = fix(t.getName());
            String mime = fix(t.getMimeType());
            String description = fix(t.getDescription());

            return (format == null || t.getOutputFormat() == format)
                && (np == null || np.matcher(name).find())
                && (mt == null || mt.matcher(mime).matches())
                && (dp == null || dp.matcher(description).find());
          }
        });
    return templates;
  }

  /**
   * findAllTemplates operation.
   *
   * @return the result
   * @throws PSAssemblyException if an error occurs
   */
  protected Collection<IPSAssemblyTemplate> findAllTemplates() throws PSAssemblyException {
    return assemblyService.findAllTemplates();
  }

  private String fix(String input) {
    if (input == null) return "";
    return input;
  }

  public abstract static class TemplatePredicate implements Predicate {

    /**
     * evaluate operation.
     *
     * @param t the t
     * @return the result
     */
    public boolean evaluate(Object t) {
      return evalTemplate((IPSAssemblyTemplate) t);
    }

    /**
     * evalTemplate operation.
     *
     * @param t the t
     * @return the result
     */
    public abstract boolean evalTemplate(IPSAssemblyTemplate t);
  }
}
