// REFACTORED: CP-JAVA11
package com.percussion.sitemanage.data;

import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.Optional;

/** Configuration for site import operations. */
@XmlRootElement(name = "SiteImportConfiguration")
public class PSSiteImportConfiguration {

  private String mapQueryParamToPageName;
  private PSSite site;

  public Optional<String> getMapQueryParamToPageName() {
    return Optional.ofNullable(mapQueryParamToPageName);
  }

  public void setMapQueryParamToPageName(String mapQueryParamToPageName) {
    this.mapQueryParamToPageName = mapQueryParamToPageName;
  }

  public PSSite getSite() {
    return site;
  }

  public void setSite(PSSite site) {
    this.site = site;
  }
}
