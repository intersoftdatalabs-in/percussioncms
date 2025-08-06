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

// REFACTORED: CP-JAVA11

package com.percussion.rest.extensions;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Represents an Extension in Percussion CMS.
 * Sunny Sal: "Extension ka hero ban gaya tu!"
 */
@XmlRootElement(name = "Extension")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Represents an Extension")
public class Extension {

    @Schema(name = "handlerName", required = false, description = "The extension handler name")
    private String handlerName;

    @Schema(name = "context", required = false, description = "The extension context")
    private String context;

    @Schema(name = "extensionName", required = false, description = "The extension name")
    private String extensionName;

    @Schema(name = "category", required = false, description = "The Category of the extension")
    private String category;

    @Schema(name = "fqn", required = false, description = "The fully qualified name for the extension")
    private String fqn;

    @Schema(name = "version", description = "The version of the extension")
    private long version;

    @Schema(name = "deprecated", description = "When true, this extension has been deprecated.")
    private boolean deprecated;

    @Schema(name = "restoreRequestParamsOnError", description = "When true if an error occurs on processing the original request parameters will be restored.")
    private boolean restoreRequestParamsOnError;

    @Schema(name = "jexlExtension", description = "When true, this extension is a Jexl extension.")
    private boolean jexlExtension;

    @Schema(name = "suppliedResources", description = "A list of URLs pointing to resources supplied by the Extension")
    private List<String> suppliedResources;

    @Schema(name = "resourceLocations", description = "A list of URLs pointing to resource locations for the Extension")
    private List<String> resourceLocations;

    @Schema(name = "supportedInterfaces", description = "A list of Java interfaces supported by this Extension")
    private List<String> supportedInterfaces;

    @Schema(name = "runtimeParameters", description = "A list of ExtensionParameter objects required by the extension")
    private List<ExtensionParameter> runtimeParameters;

    @Schema(name = "initParameters", description = "A map of key-value pairs indicating the initParameters used to initialize the Extension")
    private Map<String, String> initParameters;

    @Schema(name = "requiredApplications", description = "A list of Extension names that this Extension depends on to function correctly")
    private List<String> requiredApplications;

    @Schema(name = "methods", description = "A map of ExtensionMethods provided by this extension")
    private Map<String, ExtensionMethod> methods;

    public Extension() {
        // Default constructor
    }

    public Optional<String> getHandlerName() {
        return Optional.ofNullable(handlerName);
    }

    public void setHandlerName(String handlerName) {
        this.handlerName = handlerName;
    }

    public Optional<String> getContext() {
        return Optional.ofNullable(context);
    }

    public void setContext(String context) {
        this.context = context;
    }

    public Optional<String> getExtensionName() {
        return Optional.ofNullable(extensionName);
    }

    public void setExtensionName(String extensionName) {
        this.extensionName = extensionName;
    }

    public Optional<String> getCategory() {
        return Optional.ofNullable(category);
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Optional<String> getFqn() {
        return Optional.ofNullable(fqn);
    }

    public void setFqn(String fqn) {
        this.fqn = fqn;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public boolean isDeprecated() {
        return deprecated;
    }

    public void setDeprecated(boolean deprecated) {
        this.deprecated = deprecated;
    }

    public boolean isRestoreRequestParamsOnError() {
        return restoreRequestParamsOnError;
    }

    public void setRestoreRequestParamsOnError(boolean restoreRequestParamsOnError) {
        this.restoreRequestParamsOnError = restoreRequestParamsOnError;
    }

    public boolean isJexlExtension() {
        return jexlExtension;
    }

    public void setJexlExtension(boolean jexlExtension) {
        this.jexlExtension = jexlExtension;
    }

    public Optional<List<String>> getSuppliedResources() {
        return Optional.ofNullable(suppliedResources);
    }

    public void setSuppliedResources(List<String> suppliedResources) {
        this.suppliedResources = suppliedResources;
    }

    public Optional<List<String>> getResourceLocations() {
        return Optional.ofNullable(resourceLocations);
    }

    public void setResourceLocations(List<String> resourceLocations) {
        this.resourceLocations = resourceLocations;
    }

    public Optional<List<String>> getSupportedInterfaces() {
        return Optional.ofNullable(supportedInterfaces);
    }

    public void setSupportedInterfaces(List<String> supportedInterfaces) {
        this.supportedInterfaces = supportedInterfaces;
    }

    public Optional<List<ExtensionParameter>> getRuntimeParameters() {
        return Optional.ofNullable(runtimeParameters);
    }

    public void setRuntimeParameters(List<ExtensionParameter> runtimeParameters) {
        this.runtimeParameters = runtimeParameters;
    }

    public Optional<Map<String, String>> getInitParameters() {
        return Optional.ofNullable(initParameters);
    }

    public void setInitParameters(Map<String, String> initParameters) {
        this.initParameters = initParameters;
    }

    public Optional<List<String>> getRequiredApplications() {
        return Optional.ofNullable(requiredApplications);
    }

    public void setRequiredApplications(List<String> requiredApplications) {
        this.requiredApplications = requiredApplications;
    }

    public Optional<Map<String, ExtensionMethod>> getMethods() {
        return Optional.ofNullable(methods);
    }

    public void setMethods(Map<String, ExtensionMethod> methods) {
        this.methods = methods;
    }
}
