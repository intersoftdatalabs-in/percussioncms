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
package com.percussion.pso.imageedit.web.impl;

import com.percussion.pso.imageedit.data.MasterImageMetaData;
import com.percussion.pso.imageedit.data.OpenImageResult;
import com.percussion.pso.imageedit.web.ImagePersistenceManager;
import com.percussion.pso.imageedit.web.ImageUrlBuilder;
import com.percussion.pso.utils.RxRequestUtils;
import com.percussion.system.utils.IPSHtmlParameters;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.servlet.mvc.ParameterizableViewController;

/**
 * Spring MVC controller used by the image-editor test page to load and render sample {@link
 * MasterImageMetaData} for the image-editor UI.
 *
 * @author DavidBenua
 */
public class ImageEditorTestPageController extends ParameterizableViewController
    implements Controller {
  private static final Logger log = LogManager.getLogger(ImageEditorTestPageController.class);

  private ImagePersistenceManager imagePersistenceManager;

  private ImageUrlBuilder urlBuilder;

  /**
   * Default constructor
   * Creates a new ImageEditorTestPageController.
   *
   */
  public ImageEditorTestPageController() {}

  /**
   * handleRequestInternal operation.
   *
   * @see
   *     org.springframework.web.servlet.mvc.AbstractController#handleRequestInternal(HttpServletRequest,
   *     HttpServletResponse)
   * @param request the request
   * @param response the response
   * @return the result
   * @throws Exception if an error occurs
   */
  @Override
  protected ModelAndView handleRequestInternal(
      HttpServletRequest request, HttpServletResponse response) throws Exception {
    // get the view name
    ModelAndView mav = super.handleRequestInternal(request, response);
    String user = RxRequestUtils.getUserName(request);
    log.debug("user is {}", user);
    String session = RxRequestUtils.getSessionId(request);
    log.debug("session is {}", session);
    String contentid = request.getParameter(IPSHtmlParameters.SYS_CONTENTID);
    if (StringUtils.isNotBlank(contentid)) {
      OpenImageResult oir = imagePersistenceManager.OpenImage(contentid);
      log.debug("ItemStatus is  " + oir.getItemStatus());

      MasterImageMetaData masterImage = oir.getMasterImage();
      log.info("masterimage " + masterImage);
      mav.addObject("masterImage", masterImage);
      mav.addObject("urlBuilder", urlBuilder);
    }

    return mav;
  }

  /**
   * Returns the imagePersistenceManager.
   * @return the imagePersistenceManager
   */
  public ImagePersistenceManager getImagePersistenceManager() {
    return imagePersistenceManager;
  }

  /**
   * Sets the imagePersistenceManager.
   * @param imagePersistenceManager the imagePersistenceManager to set
   */
  public void setImagePersistenceManager(ImagePersistenceManager imagePersistenceManager) {
    this.imagePersistenceManager = imagePersistenceManager;
  }

  /**
   * Returns the urlBuilder.
   * @return the urlBuilder
   */
  public ImageUrlBuilder getUrlBuilder() {
    return urlBuilder;
  }

  /**
   * Sets the urlBuilder.
   * @param urlBuilder the urlBuilder to set
   */
  public void setUrlBuilder(ImageUrlBuilder urlBuilder) {
    this.urlBuilder = urlBuilder;
  }
}
