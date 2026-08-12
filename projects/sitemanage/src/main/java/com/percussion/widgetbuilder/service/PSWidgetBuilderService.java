// REFACTORED: CP-JAVA11
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

/**
 * Service implementation for Widget Builder operations.
 *
 * <p>Sunny Sal says: "Widget deployment is like a Bollywood dance number—lots of moving parts, but
 * the end result is spectacular!"
 */
package com.percussion.widgetbuilder.service;

import static com.percussion.cms.IPSConstants.SAAS_FLAG;

import com.percussion.cms.IPSConstants;
import com.percussion.deployer.server.PSLocalDeployerClient;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.server.PSServer;
import com.percussion.services.notification.IPSNotificationService;
import com.percussion.services.widgetbuilder.IPSWidgetBuilderDefinitionDao;
import com.percussion.share.service.IPSSystemProperties;
import com.percussion.widgetbuilder.data.PSWidgetBuilderDefinitionData;
import com.percussion.widgetbuilder.data.PSWidgetBuilderDefinitionDataList;
import com.percussion.widgetbuilder.data.PSWidgetBuilderFieldsListData;
import com.percussion.widgetbuilder.data.PSWidgetBuilderResourceListData;
import com.percussion.widgetbuilder.data.PSWidgetBuilderSummaryData;
import com.percussion.widgetbuilder.data.PSWidgetBuilderSummaryDataList;
import com.percussion.widgetbuilder.data.PSWidgetBuilderValidationResults;
import com.percussion.widgetbuilder.utils.PSWidgetPackageBuilder;
import com.percussion.widgetbuilder.utils.PSWidgetPackageSpec;
import com.percussion.widgetbuilder.utils.validate.PSWidgetBuilderDefinitionValidator;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.io.File;
import java.io.FileFilter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.helper.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Service implementation for Widget Builder operations.
 *
 * <p>Sunny Sal says: "Widget deployment is like a Bollywood dance number—lots of moving parts, but
 * the end result is spectacular!"
 */
@Path("/widgetbuilder")
@Component("widgetBuilderService")
@Lazy
public class PSWidgetBuilderService implements IPSWidgetBuilderService {

  private final IPSWidgetBuilderDefinitionDao dao;
  private IPSSystemProperties systemProps;

  private static final Logger log = LogManager.getLogger(IPSConstants.WIDGET_BUILDER_LOG);

  @Autowired
  public PSWidgetBuilderService(
      final IPSWidgetBuilderDefinitionDao dao, IPSNotificationService notificationService) {
    Validate.notNull(dao, "dao must not be null");
    this.dao = dao;
  }

  @Autowired
  public void setSystemProps(IPSSystemProperties systemProps) {
    this.systemProps = systemProps;
  }

  public IPSSystemProperties getSystemProps() {
    return systemProps;
  }

  @Override
  @GET
  @Path("/active")
  @Produces(MediaType.TEXT_PLAIN)
  public boolean isWidgetBuilderEnabled() {
    return Boolean.parseBoolean(getSystemProps().getProperty("isWidgetBuilderActive"));
  }

  @Override
  @GET
  @Path("/deployed/{definitionId}")
  @Produces(MediaType.TEXT_PLAIN)
  public boolean isWidgetDefinitionDeployed(@PathParam("definitionId") final long definitionId) {
    // TODO: Implement actual deployment check logic
    return true;
  }

  @Override
  @DELETE
  @Path("/definition/{definitionId}")
  public void deleteWidgetBuilderDefinition(@PathParam("definitionId") final long definitionId) {
    try {
      dao.delete(definitionId);
    } catch (Exception e) {
      log.error(
          "Failed to delete widget definition: {}. Error: {}",
          definitionId,
          PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new RuntimeException("Failed to delete widget definition: " + definitionId, e);
    }
  }

  @Override
  @GET
  @Path("/definitions")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public List<PSWidgetBuilderDefinitionData> loadAll() {
    try {
      var definitions = Optional.ofNullable(dao.getAll()).orElse(List.of());
      var returnResults =
          definitions.stream().map(PSWidgetBuilderDefinitionData::new).collect(Collectors.toList());
      return new PSWidgetBuilderDefinitionDataList(returnResults);
    } catch (Exception e) {
      log.error(
          "Failed to load widget definitions. Error: {}", PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new RuntimeException("Failed to load widget definitions", e);
    }
  }

  @Override
  @GET
  @Path("/summaries")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public List<PSWidgetBuilderSummaryData> loadAllSummaries() {
    try {
      var definitions = Optional.ofNullable(dao.getAll()).orElse(List.of());
      var returnResults =
          definitions.stream().map(PSWidgetBuilderSummaryData::new).collect(Collectors.toList());
      return new PSWidgetBuilderSummaryDataList(returnResults);
    } catch (Exception e) {
      log.error("Failed to load widget summaries. Error: {}", PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new RuntimeException("Failed to load widget summaries", e);
    }
  }

  @Override
  @GET
  @Path("/definition/{definitionId}")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSWidgetBuilderDefinitionData loadWidgetDefinition(
      @PathParam("definitionId") final long definitionId) {
    var daoObjectOpt = dao.find(definitionId);
    return daoObjectOpt.map(PSWidgetBuilderDefinitionData::new).orElse(null);
  }

  @Override
  @POST
  @Path("/definition/")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSWidgetBuilderValidationResults saveWidgetBuilderDefinition(
      PSWidgetBuilderDefinitionData definition) {
    try {
      var results = validate(definition);
      if (!results.getResults().isEmpty()) {
        return results;
      }
      if (StringUtils.isNotBlank(definition.getWidgetTrayCustomizedIconPath())) {
        definition.setWidgetTrayCustomizedIconPath(
            definition.getWidgetTrayCustomizedIconPath().replace("\\", "/"));
        var imagePath = new File(PSServer.getRxDir(), definition.getWidgetTrayCustomizedIconPath());
        if (!imagePath.exists()) {
          log.warn("No valid path found for widget tray icon: {}", imagePath);
          definition.setWidgetTrayCustomizedIconPath("");
        }
      }
      if (StringUtils.isNotBlank(definition.getDescription())) {
        definition.setDescription(definition.getDescription().replace("\"", "'"));
      }
      var daoObject = dao.save(PSWidgetBuilderDefinitionData.createDaoObject(definition));
      if (daoObject != null) {
        results.setDefinitionId(
            Long.parseLong(new PSWidgetBuilderDefinitionData(daoObject).getId()));
      }
      return results;
    } catch (Exception e) {
      log.error(
          "Error saving Widget Builder Widget Definition. Error: {}",
          PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new RuntimeException("Failed to save widget definition", e);
    }
  }

  @Override
  @POST
  @Path("/validate/")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  // TODO: Remove me @SuppressFBWarnings("DLS_DEAD_LOCAL_STORE")
  public PSWidgetBuilderValidationResults validate(PSWidgetBuilderDefinitionData definition) {
    Validate.notNull(definition, "definition must not be null");
    var results = new PSWidgetBuilderValidationResults();
    results.setResults(PSWidgetBuilderDefinitionValidator.validate(definition, loadAll()));
    return results;
  }

  @Override
  @POST
  @Path("/deploy/{definitionId}")
  public void deployWidget(@PathParam("definitionId") final long definitionId) {
    var srcFile =
        new File(PSServer.getRxDir(), "sys_resources/widgetbuilder/percWidgetTemplate.zip");
    var tmpDir = new File(PSServer.getRxDir(), "rx_resources/widgets_generated/temp");
    var tgtDir = new File(PSServer.getRxDir(), "rx_resources/widgets_generated");
    if (!tgtDir.exists() && !tgtDir.mkdirs()) {
      log.error("Unable to create target widget directory: {}", tgtDir.getAbsolutePath());
      throw new RuntimeException(
          "Unable to create target widget directory: " + tgtDir.getAbsoluteFile());
    }
    if (!tmpDir.exists() && !tmpDir.mkdirs()) {
      log.error("Unable to create temp widget directory: {}", tmpDir.getAbsolutePath());
      throw new RuntimeException(
          "Unable to create temp widget directory: " + tmpDir.getAbsoluteFile());
    }
    try {
      var definitionOpt = this.dao.find(definitionId);
      Validate.isTrue(definitionOpt.isPresent(), "Widget definition not found for deployment");
      var definition = definitionOpt.get();
      var builder = new PSWidgetPackageBuilder(srcFile, tmpDir);
      var spec =
          new PSWidgetPackageSpec(
              definition.getPrefix().orElse(""),
              definition.getPublisherUrl().orElse(""),
              definition.getLabel().orElse(""),
              definition.getDescription().orElse(""),
              definition.getVersion().orElse(""),
              PSServer.getVersion());
      spec.setResponsive(definition.isResponsive());
      if (StringUtils.isNotBlank(definition.getWidgetTrayCustomizedIconPath().orElse(""))) {
        spec.setWidgetTrayCustomizedIconPath(
            definition.getWidgetTrayCustomizedIconPath().orElse(""));
      }
      if (StringUtils.isNotBlank(definition.getToolTipMessage().orElse(""))) {
        spec.setTooTipMessage(definition.getToolTipMessage().orElse(""));
      }
      if (StringUtils.isNotBlank(definition.getFields())) {
        spec.setFields(PSWidgetBuilderFieldsListData.fromXml(definition.getFields()).getFields());
      }
      if (StringUtils.isNotBlank(definition.getCssFiles())) {
        spec.setCssFiles(
            PSWidgetBuilderResourceListData.fromXml(definition.getCssFiles()).getResourceList());
      }
      if (StringUtils.isNotBlank(definition.getJsFiles())) {
        spec.setJsFiles(
            PSWidgetBuilderResourceListData.fromXml(definition.getJsFiles()).getResourceList());
      }
      spec.setWidgetHtml(definition.getWidgetHtml());
      var result = builder.generatePackage(tgtDir, spec);
      if (result.exists()) {
        var client = new PSLocalDeployerClient();
        try {
          client.installPackage(result);
          copyWidgetMutables(result);
        } catch (Exception e) {
          log.error("Failed to install package. Error: {}", PSExceptionUtils.getMessageForLog(e));
          log.debug(PSExceptionUtils.getDebugMessageForLog(e));
          throw new RuntimeException("Failed to install package.", e);
        }
      } else {
        throw new RuntimeException(
            "Failed to generate package for widget definition: " + definitionId);
      }
    } catch (Exception e) {
      log.error("WidgetBuilder Error: {}", PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      if (e instanceof RuntimeException) {
        throw (RuntimeException) e;
      }
      throw new RuntimeException("Failed to build package", e);
    }
  }

  /**
   * Copy new ppkg file and all object store psx_ce files to mutable directory for persistence in
   * docker SAAS installation.
   *
   * @param ppkg Package file to be copied. Null value will result in a RuntimeException thrown
   */
  private void copyWidgetMutables(File ppkg) {
    if (PSServer.getServerProps() != null
        && StringUtils.equalsIgnoreCase(PSServer.getServerProps().getProperty(SAAS_FLAG), "true")) {
      var mutableDir = new File(PSServer.getRxDir(), "var");
      var mutableWidgetDir = new File(mutableDir, "widgets_generated");
      var objectStoreDir = new File(PSServer.getRxDir(), "ObjectStore");
      var mutableObjectStoreDir = new File(mutableDir, "ObjectStore");

      if (!mutableWidgetDir.exists() && !mutableWidgetDir.mkdirs()) {
        log.error(
            "Unable to create mutable widget directory: {}", mutableWidgetDir.getAbsolutePath());
        throw new RuntimeException(
            "Unable to create mutable widget directory: " + mutableWidgetDir.getAbsoluteFile());
      }
      if (!mutableObjectStoreDir.exists() && !mutableObjectStoreDir.mkdirs()) {
        log.error(
            "Unable to create mutable object store directory: {}",
            mutableObjectStoreDir.getAbsolutePath());
        throw new RuntimeException(
            "Unable to create mutable object store directory: "
                + mutableObjectStoreDir.getAbsoluteFile());
      }
      try {
        if (ppkg.exists()) {
          FileUtils.copyFileToDirectory(ppkg, mutableWidgetDir);
        } else {
          throw new RuntimeException("Widget ppkg file must exist for copy to mutable directory.");
        }
        FileFilter ceFilter = FileFilterUtils.prefixFileFilter("psx_ce");
        FileUtils.copyDirectory(objectStoreDir, mutableObjectStoreDir, ceFilter);
      } catch (Exception e) {
        log.error(
            "An unexpected Exception occurred while saving the widget definition. Error: {}",
            PSExceptionUtils.getMessageForLog(e));
        log.debug(PSExceptionUtils.getDebugMessageForLog(e));
        if (e instanceof RuntimeException) {
          throw (RuntimeException) e;
        }
        throw new RuntimeException("Failed to copy widget package mutables.", e);
      }
    }
  }
}
