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

package com.percussion.fastforward.sfp;

/**
 * This interface defines all string constants representing the DTD for the remote publisher edition
 * XML document that is sent part of the SOAP request to the publisher client. The DTD and a typical
 * XML document shall be of the following syntax:
 *
 * <p>&lt;?xml version="1.0" encoding="UTF-8"?&gt;<br>
 * &lt;!DOCTYPE psxpub:pubdata[<br>
 * &lt;!ELEMENT psxpub:pubdata (destsite, publisherconfig, contentlist) &gt;<br>
 * &lt;!ATTLIST psxpub:pubdata xmlns:psxpub CDATA #FIXED "urn:www.percussion. com/publisher" &gt;
 * <br>
 * &lt;!ELEMENT destsite (#PCDATA) &gt;<br>
 * &lt;!ATTLIST destsite siteid #REQUIRED&gt;<br>
 * &lt;!ATTLIST destsite name #IMPLIED&gt;<br>
 * &lt;!ATTLIST destsite ipaddress #REQUIRED&gt;<br>
 * &lt;!ATTLIST destsite port #IMPLIED&gt;<br>
 * &lt;!ATTLIST destsite userid #IMPLIED&gt;<br>
 * &lt;!ATTLIST destsite password #IMPLIED&gt;<br>
 * &lt;!ATTLIST destsite root #IMPLIED&gt;<br>
 * &lt;!ELEMENT publisherconfig (param*) &gt;<br>
 * &lt;!ELEMENT param* (#PCDATA) &gt;<br>
 * &lt;!ATTLIST param name #REQUIRED&gt;<br>
 * &lt;!ELEMENT contentlist (contentitem *) &gt;<br>
 * &lt;!ATTLIST contentlist deliverytype #REQUIRED&gt;<br>
 * &lt;!ATTLIST contentlist context #REQUIRED&gt;<br>
 * &lt;!ATTLIST contentlist publicationid #REQUIRED&gt;<br>
 * &lt;!ATTLIST contentlist editionid #REQUIRED&gt;<br>
 * &lt;!ATTLIST contentlist publisherid #REQUIRED&gt;<br>
 * &lt;!ATTLIST contentlist clistid #REQUIRED&gt;<br>
 * &lt;!ATTLIST contentlist pubstatusid #REQUIRED&gt;<br>
 * &lt;!ATTLIST contentlist pageindex #REQUIRED&gt;<br>
 * &lt;!ATTLIST contentlist islastpage #REQUIRED&gt;<br>
 * &lt;!ELEMENT contentitem* (title, contenturl, delivery, customproperties?) &gt;<br>
 * &lt;!ATTLIST contentitem contentid #REQUIRED&gt;<br>
 * &lt;!ATTLIST contentitem unpublish #IMPLIED&gt;<br>
 * &lt;!ATTLIST contentitem revision #IMPLIED&gt;<br>
 * &lt;!ATTLIST contentitem variantid #REQUIRED&gt;<br>
 * &lt;!ELEMENT title (#PCDATA) &gt;<br>
 * &lt;!ELEMENT contenturl (#PCDATA) &gt;<br>
 * &lt;!ELEMENT delivery (location*) &gt;<br>
 * &lt;!ELEMENT customproperties (customproperty1, customproperty2) &gt;<br>
 * &lt;!ELEMENT customproperty1 (#PCDATA) &gt;<br>
 * &lt;!ELEMENT customproperty2 (#PCDATA) &gt;<br>
 * &lt;!ELEMENT location (#PCDATA) &gt;<br>
 * ]&gt;<br>
 * &lt;!-- sample document --&gt;<br>
 * &lt;psxpub:pubdata xmlns:psxpub="urn:www.percussion.com/publisher"&gt;<br>
 * &lt;destsite siteid="111" name="site1" ipaddress="yy.yyy.yy.yyy" port="27" userid="ftpuser"
 * password="23dfs54g8j" rootdir="wwwroot/testsite"&gt;site description &lt;/destsite&gt;<br>
 * &lt;publisherconfig&gt;<br>
 * &lt;param name="rxserver"&gt;12.345.567.32 &lt;/param&gt;<br>
 * &lt;param name="rxport"&gt;9992 &lt;/param&gt;<br>
 * &lt;param name="rxsslport"&gt;9443 &lt;/param&gt;<br>
 * &lt;param name="statusurl"&gt;/Rhythmyx/rx_pubMain/updatestatus.xml &lt;/param&gt;<br>
 * &lt;param name="filesystem"&gt;com.percussion.cml.publisher. PSFilePublisherHandler
 * &lt;/param&gt;<br>
 * &lt;param name="ftp"&gt;com.percussion.cml.publisher.PSFtpPublisherHandler &lt;/param&gt;<br>
 * &lt;param name="usserid"&gt;cmsuser &lt;/param&gt;<br>
 * &lt;param name="password"&gt;1sgw437yurg &lt;/param&gt;<br>
 * &lt;/publisherconfig&gt;<br>
 * &lt;contentlist clistid="11" context="1" deliverytype="filesystem" publicationid="222"
 * editionid="100" publisherid="333" pubstatusid="403" pageindex="3" islastpage="false" &gt;<br>
 * &lt;contentitem contentid="1" variantid="101"&gt;<br>
 * &lt;title&gt;testtitle &lt;/title&gt;<br>
 * &lt;contenturl&gt;https://www.percussion.com/rhythmyx/index.htm &lt;/contenturl&gt;<br>
 * &lt;delivery&gt;<br>
 * &lt;location&gt;test/test.htm &lt;/location&gt;<br>
 * &lt;/delivery&gt;<br>
 * &lt;/contentitem&gt;<br>
 * &lt;contentitem contentid="2"&gt;<br>
 * &lt;title&gt;testtitle &lt;/title&gt;<br>
 * &lt;contenturl&gt;http://www.microsoft.com/windows/default.asp &lt;/contenturl&gt;<br>
 * &lt;delivery&gt;<br>
 * &lt;location&gt;test/ms.htm &lt;/location&gt;<br>
 * &lt;/delivery&gt;<br>
 * &lt;/contentitem&gt;<br>
 * &lt;/contentlist&gt;<br>
 * &lt;/psxpub:pubdata&gt;<br>
 */
public interface IPSDTDPublisherEdition {
  /*
   * Element names
   */
  /** The name of the root element of the publisher edition XML document. */
  public static final String ELEM_ROOT = "psxpub:pubdata";

  /** The name of the destination site element. */
  public static final String ELEM_SITE = "destsite";

  /** The name of the content list element. */
  public static final String ELEM_CONTENTLIST = "contentlist";

  /** The name of the content item element. */
  public static final String ELEM_CONTENTITEM = "contentitem";

  /** The name of the content title element. */
  public static final String ELEM_CONTENTTITLE = "title";

  /** The name of the content URL element. */
  public static final String ELEM_CONTENTURL = "contenturl";

  /** The name of the delivery element. */
  public static final String ELEM_DELIVERY = "delivery";

  /** The name of the location element. */
  public static final String ELEM_LOCATION = "location";

  /** The name of the modify date element. */
  public static final String ELEM_MODIFYDATE = "modifydate";

  /** The name of the modify user element. */
  public static final String ELEM_MODIFYUSER = "modifyuser";

  /** The name of the expire date element. */
  public static final String ELEM_EXPIREDATE = "expiredate";

  /** The name of the content type element. */
  public static final String ELEM_CONTENTTYPE = "contenttype";

  /** The name of the publisher configuration element. */
  public static final String ELEM_CONFIG = "publisherconfig";

  /** The name of a publisher configuration parameter element. */
  public static final String ELEM_PARAM = "param";

  /** The name of the custom properties container element. */
  public static final String ELEM_CUSTOMPROPERTIES = "customproperties";

  /*
   * Attribute names
   */
  /** The XML namespace attribute name used by the publisher edition document. */
  public static final String ATTR_NS = "xmlns:psxpub";

  /** The user identifier attribute name on the destination site element. */
  public static final String ATTR_USERID = "userid";

  /** The password attribute name on the destination site element. */
  public static final String ATTR_PASSWORD = "password";

  /** The IP address attribute name on the destination site element. */
  public static final String ATTR_IPADDRESS = "ipaddress";

  /** The port attribute name on the destination site element. */
  public static final String ATTR_PORT = "port";

  /** The root directory attribute name on the destination site element. */
  public static final String ATTR_ROOTDIR = "rootdir";

  /** The name attribute name on the destination site element. */
  public static final String ATTR_NAME = "name";

  /** The edition identifier attribute name on the content list element. */
  public static final String ATTR_EDITIONID = "editionid";

  /** The source site identifier attribute name. */
  public static final String ATTR_SRCSITEID = "srcsiteid";

  /** The recovery publication status identifier attribute name. */
  public static final String ATTR_RECOVERYPUBSTATUSID = "recoverypubstatusid";

  /** The site identifier attribute name on the destination site element. */
  public static final String ATTR_SITEID = "siteid";

  /** The publisher identifier attribute name on the content list element. */
  public static final String ATTR_PUBLISHERID = "publisherid";

  /** The publication identifier attribute name on the content list element. */
  public static final String ATTR_PUBLICATIONID = "publicationid";

  /** The publication status identifier attribute name on the content list element. */
  public static final String ATTR_PUBSTATUSID = "pubstatusid";

  /** The delivery type attribute name on the content list element. */
  public static final String ATTR_DELIVERYTYPE = "deliverytype";

  /** The unpublish attribute name on the content item element. */
  public static final String ATTR_UNPUBLISH = "unpublish";

  /** The content identifier attribute name on the content item element. */
  public static final String ATTR_CONTENTID = "contentid";

  /** The revision attribute name on the content item element. */
  public static final String ATTR_REVISION = "revision";

  /** The context attribute name on the content list element. */
  public static final String ATTR_CONTEXT = "context";

  /** The variant identifier attribute name on the content item element. */
  public static final String ATTR_VARIANTID = "variantid";

  /** The content list identifier attribute name on the content list element. */
  public static final String ATTR_CLISTID = "clistid";

  /** The page index attribute name on the content list element. */
  public static final String ATTR_PAGEINDEX = "pageindex";

  /** The is-last-page attribute name on the content list element. */
  public static final String ATTR_ISLASTPAGE = "islastpage";

  /** The elapsed time attribute name on the content list element. */
  public static final String ATTR_ELAPSETIME = "elapsetime";

  /**
   * The name for the attribute holding the publisher user identification, never <code>null</code>.
   */
  public static final String ATTR_PUBUID = "pubuid";

  /** The name for the attribute holding the publisher password, never <code>null</code>. */
  public static final String ATTR_PUBPW = "pubpw";

  /** The parameter name for the rhythmyx server name. */
  public static final String PARAM_RXSERVER = "rxserver";

  /** The parameter name for the rhythmyx server port. */
  public static final String PARAM_RXPORT = "rxport";

  /** The parameter name for the rhythmyx server SSL port. */
  public static final String PARAM_RXSSLPORT = "rxsslport";

  /** The parameter name for the rhythmyx server user name. */
  public static final String PARAM_USERID = "userid";

  /** The parameter name for the rhythmyx server password. */
  public static final String PARAM_PASSWORD = "password";

  /** The parameter name for the publisher user name parameter. */
  public static final String PARAM_PUBUID = "pubuid";

  /** The parameter name for the publisher password. */
  public static final String PARAM_PUBPW = "pubpw";

  /**
   * The parameter name for the publisher SSL port. Use this name to specify the publisher SSL port
   * in the publisher setup form.
   */
  public static final String PARAM_SSLPORT = "sslport";

  /**
   * The parameter name for the publisher log location. Use this name to specify the log location in
   * the publisher setup form.
   */
  public static final String PARAM_LOG_LOCATION = "loglocation";

  /**
   * The parameter name for the publisher soap request file. Use this name to specify the soap
   * request file in the publisher setup form.
   */
  public static final String PARAM_SOAP_REQUEST = "soaprequest";

  /**
   * The parameter name for the database publisher jdbc context factory used for jndi lookups. Use
   * this name to specify the database publisher jdbc context factory in the publisher setup form.
   */
  public static final String PARAM_JDBC_CONTEXTFACTORY = "jdbccontextfactory";

  /**
   * The parameter name for the database publisher jndi provider url to use. Use this name to
   * specify the database publisher jndi provider url in the publisher setup form.
   */
  public static final String PARAM_JNDI_PROVIDERURL = "jndiproviderurl";

  /**
   * The parameter name for the number of items after which the FTP client will log out of and then
   * log back into a server session. Defaults to Integer.MAX_INT if not found in the database.
   */
  public static final String PARAM_FTP_RELOGIN_ITEMCOUNT = "ftpreloginitemcount";

  /**
   * The parameter name for the time (in seconds) that the FTP Publisher client will wait for the
   * complete content list to arrive. The client will time out if this request is not complete in
   * the specified number of seconds. Defaults to 0 if not found in the database.
   */
  public static final String PARAM_SERVER_REQUEST_TIMEOUT = "serverrequesttimeout";

  /**
   * The parameter name for the field that indicates whether to use Active mode (false) or Passive
   * mode (true) in FTP Publisher requests. Defaults to false (use Active mode) if not found in the
   * database.
   */
  public static final String PARAM_FTP_USE_PASSIVE_MODE = "enablepassivemode";

  /**
   * Parameter name for the time (in seconds) to wait for data to arrive on FTP sockets when
   * publishing. Note that timeouts should be rare, especially since most of the time we're
   * tranmitting data, not receiving it.
   *
   * <p>Defaults to 60 seconds. 0 means no timeout is set.
   */
  public static final String PARAM_FTP_RCV_TIMEOUT = "ftprcvtimeout";
}
