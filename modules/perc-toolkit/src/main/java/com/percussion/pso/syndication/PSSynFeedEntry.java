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
package com.percussion.pso.syndication;

import com.rometools.modules.mediarss.MediaEntryModule;
import com.rometools.modules.mediarss.types.MediaContent;
import com.rometools.rome.feed.synd.SyndCategory;
import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndEnclosure;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndLink;
import com.rometools.rome.feed.synd.SyndPerson;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

<<<<<<< HEAD
/****
 * Provides Velocity friendly methods for a
 * Syndication Feed entry.
=======
/**
 * ** Provides Velocity friendly methods for a Syndication Feed entry.
>>>>>>> development-8.1.x
 *
 * @author natechadwick
 */
public class PSSynFeedEntry {

  private SyndEntry entry;

<<<<<<< HEAD
  /***
   * Returns the name of the first entry author in the collection of authors.
=======
  /**
   * * Returns the name of the first entry author in the collection of authors.
   *
>>>>>>> development-8.1.x
   * @return
   */
  public String getAuthor() {
    String ret = "";

    if (entry != null) if (entry.getAuthor() != null) ret = entry.getAuthor();
    return ret;
  }

<<<<<<< HEAD
  /***
   * Returns the entry authors.
=======
  /**
   * * Returns the entry authors.
   *
>>>>>>> development-8.1.x
   * @return
   */
  public List getAuthorsList() {
    return entry.getAuthors();
  }

<<<<<<< HEAD
  /***
   * Returns a comma seperated list of the entry authors.
=======
  /**
   * * Returns a comma seperated list of the entry authors.
   *
>>>>>>> development-8.1.x
   * @return
   */
  public String getAuthors() {
    String ret = "";
    Object a;

    // @TODO: Add Atom Support
    for (int i = 0; i < entry.getAuthors().size(); i++) {
      a = entry.getAuthors().get(i);
      if (!(a instanceof SyndPerson)) {
        if (ret == "") ret = a.toString();
        else ret.concat("," + a.toString());
      }
    }
    return ret;
  }
<<<<<<< HEAD

  /***
   * Returns the feed categories.
   * @return
   */
  public List getCategoriesList() {
    return entry.getCategories();
  }

  /***
   * Returns the feed categories as a comma separated string.
   * @return
   */
  public String getCategories() {
    String ret = "";
    SyndCategory a;

    for (int i = 0; i < entry.getCategories().size(); i++) {
      a = (SyndCategory) entry.getCategories().get(i);
      if (ret == "") ret = a.getName();
      else ret.concat("," + a.getName());
    }

    return ret;
  }

  /***
   * the feed author.
   * @return
   */
  public String getContributors() {
    String ret = "";
    Object a;

=======
  /**
   * * Returns the feed categories.
   *
   * @return
   */
  public List getCategoriesList() {
    return entry.getCategories();
  }

  /**
   * * Returns the feed categories as a comma separated string.
   *
   * @return
   */
  public String getCategories() {
    String ret = "";
    SyndCategory a;

    for (int i = 0; i < entry.getCategories().size(); i++) {
      a = (SyndCategory) entry.getCategories().get(i);
      if (ret == "") ret = a.getName();
      else ret.concat("," + a.getName());
    }

    return ret;
  }

  /**
   * * the feed author.
   *
   * @return
   */
  public String getContributors() {
    String ret = "";
    Object a;

>>>>>>> development-8.1.x
    // @TODO: Add Atom Support
    for (int i = 0; i < entry.getContributors().size(); i++) {
      a = entry.getContributors().get(i);
      if (!(a instanceof SyndPerson)) {
        if (ret == "") ret = a.toString();
        else ret.concat("," + a.toString());
      }
    }
    return ret;
  }

  public List getContributorsList() {
    return entry.getContributors();
  }

<<<<<<< HEAD
  /***
   * Returns the entry contents.
=======
  /**
   * * Returns the entry contents.
   *
>>>>>>> development-8.1.x
   * @return
   */
  public String getContents() {
    String ret = "";

    for (int i = 0; i < entry.getContents().size(); i++) {
      if (ret == "") ret = ((SyndContent) entry.getContents().get(i)).getValue();
      else ret = ret + "\r\n" + entry.getContents().get(i).getValue();
    }
    return ret;
  }

<<<<<<< HEAD
  /***
   * Returns the entry description.
=======
  /**
   * * Returns the entry description.
   *
>>>>>>> development-8.1.x
   * @return
   */
  public String getDescription() {
    String ret = "";

    if (entry != null)
      if (entry.getDescription() != null)
        if (entry.getDescription().getValue() != null) ret = entry.getDescription().getValue();
    return ret;
  }

  public List<PSSynFeedEnclosure> getEnclosures() {
    ArrayList<PSSynFeedEnclosure> ret = new ArrayList<PSSynFeedEnclosure>();

    for (int i = 0; i < entry.getEnclosures().size(); i++) {
      ret.add(new PSSynFeedEnclosure((SyndEnclosure) entry.getEnclosures().get(i)));
    }
    return ret;
  }

<<<<<<< HEAD
  /***
   *  Returns the entry link.
   */
=======
  /** * Returns the entry link. */
>>>>>>> development-8.1.x
  public String getLink() {
    String ret = "";

    if (entry != null) if (entry.getLink() != null) ret = entry.getLink();

    return ret;
  }

<<<<<<< HEAD
  /***
   * Returns the entry links
=======
  /**
   * * Returns the entry links
   *
>>>>>>> development-8.1.x
   * @return
   */
  public List<SyndLink> getLinks() {
    List<SyndLink> links = entry.getLinks();
    return links;
  }

  public List<PSSynFeedMediaContent> getMediaRSSContent() {
    MediaEntryModule mediaModule = null;

    List<PSSynFeedMediaContent> contents = new ArrayList<PSSynFeedMediaContent>();

    mediaModule = (MediaEntryModule) entry.getModule(MediaEntryModule.URI);
    if (mediaModule != null && mediaModule instanceof MediaEntryModule) {
      MediaEntryModule mentry = (MediaEntryModule) mediaModule;

      for (MediaContent mc : mentry.getMediaContents()) {
        contents.add(new PSSynFeedMediaContent(mc));
      }
    }
    return contents;
  }

<<<<<<< HEAD
  /***
   * Returns the entry published date.
=======
  /**
   * * Returns the entry published date.
   *
>>>>>>> development-8.1.x
   * @return
   */
  public Date getPublishedDate() {
    return entry.getPublishedDate();
  }

<<<<<<< HEAD
  /***
   *  Returns the entry title.
=======
  /**
   * * Returns the entry title.
   *
>>>>>>> development-8.1.x
   * @return
   */
  public String getTitle() {
    String ret = "";
    if (entry != null) if (entry.getTitle() != null) ret = entry.getTitle();
    return ret;
  }

<<<<<<< HEAD
  /***
   *  Returns the entry updated date.
=======
  /**
   * * Returns the entry updated date.
   *
>>>>>>> development-8.1.x
   * @return
   */
  public Date getUpdatedDate() {
    if (entry.getUpdatedDate() != null) {
      return entry.getUpdatedDate();
    } else {
      Calendar calendar = Calendar.getInstance();

      return calendar.getTime();
    }
  }

  public String getUri() {
    return entry.getUri();
  }

  public PSSynFeedEntry(SyndEntry arg) {
    entry = arg;
  }
}
