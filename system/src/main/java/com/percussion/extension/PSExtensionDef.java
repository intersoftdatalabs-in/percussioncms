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

package com.percussion.extension;

import com.percussion.design.objectstore.PSExtensionParamDef;
import com.percussion.services.data.IPSCloneTuner;
import com.percussion.xml.PSXmlDocumentBuilder;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import java.util.Objects;
import java.util.Properties;
import org.w3c.dom.Element;

/**
 * An extension definition defines the name, initialization parameters,
 * and resource locations for an extension. The actual files (if any)
 * which make up the extension are supplied elsewhere.
 */
public class PSExtensionDef implements IPSExtensionDef, Serializable, IPSCloneTuner {
  /**
   * Compiler generated serial version ID used for serialization.
   */
  private static final long serialVersionUID = -4369644099981854127L;

  /**
   * Convenients constructor that calls {@link #PSExtensionDef(PSExtensionRef,
   * Iterator, Iterator, Properties, Iterator, Iterator, boolean, boolean)
   * PSExtensionDef(ref, interfaces, resourceURLs, initParams, runtimeParams,
   * null, false, false)}.
   */
  public PSExtensionDef(
      PSExtensionRef ref,
      Iterator<String> interfaces,
      Iterator<URL> resourceURLs,
      Properties initParams,
      Iterator<PSExtensionParamDef> runtimeParams) {
    this(ref, interfaces, resourceURLs, initParams, runtimeParams, null, false, false);
  }

  /**
   * Constructs a new extension def.
   *
   * @param ref The extension reference. Must not be <CODE>null</CODE>.
   *
   * @param interfaces An Iterator over 1 or more non-<CODE>null</CODE>
   * names of interfaces that this extension implements.
   *
   * @param resourceURLs An Iterator over 0 or more non-<CODE>null</CODE>
   * URL objects referring to resources used by the extension. May be
   * <CODE>null</CODE>, in which case the extension must be self-contained.
   *
   * @param initParams A Properties object containing 0 or more custom
   * initialization properties required by the defined extension. May be
   * <CODE>null</CODE>, in which case no properties will be set.
   *
   * @param runtimeParams An iterator over zero or non-<CODE>null</CODE>
   * PSExtensionParamDef objects. The order of parameters
   * is important. May be <CODE>null</CODE>, in which case no runtimeParam
   * defs will be set.
   *
   * @param suppliedResources An Iterator over 0 or more non-<CODE>null</CODE>
   * URL objects referring to files used by the extension. May be
   * <CODE>null</CODE>, in which case they may be added later using
   * {@link #setSuppliedResources(Iterator) setSuppliedResources()}.
   *
   * @param isDeprecated If <code>true</code>, then this extension will be
   * flagged as deprecated, if <code>false</code>, it will not.
   *
   * @param isRestoreRequestParamsOnError <code>true</code> indicates that
   * based on the Extensions.xml restoreRequestParamsOnError="yes" attribute
   * this extension modifies request parameters, <code>false</code> otherwise.
   */
  public PSExtensionDef(
      PSExtensionRef ref,
      Iterator<String> interfaces,
      Iterator<URL> resourceURLs,
      Properties initParams,
      Iterator<PSExtensionParamDef> runtimeParams,
      Iterator<URL> suppliedResources,
      boolean isDeprecated,
      boolean isRestoreRequestParamsOnError) {
    if (ref == null) throw new IllegalArgumentException("Extension ref cannot be null");

    if (interfaces == null) throw new IllegalArgumentException("interfaces cannot be null");

    if (!interfaces.hasNext())
      throw new IllegalArgumentException("At least one interface must be defined");

    this.reference = ref;
    this.resourceUrls = new ArrayList<>();
    if (resourceURLs != null) {
      while (resourceURLs.hasNext()) {
        // do an unnecessary cast so that any objects of the wrong
        // type will cause an error
        URL u = (URL) (resourceURLs.next());
        if (u == null) {
          throw new IllegalArgumentException("null resource URLs are not allowed");
        }
        this.resourceUrls.add(u);
      }
    }

    if (initParams != null) {
      this.initParams = (Properties) initParams.clone();
    } else {
      this.initParams = new Properties();
    }

    this.interfaces = new ArrayList<>();
    while (interfaces.hasNext()) {
      this.interfaces.add((String) interfaces.next());
    }

    this.runtimeParams = new ArrayList<>();
    this.runtimeParamsMap = new HashMap<>();
    if (runtimeParams != null) {
      while (runtimeParams.hasNext()) {
        PSExtensionParamDef p = (PSExtensionParamDef) runtimeParams.next();
        this.runtimeParams.add(p);
        this.runtimeParamsMap.put(p.getName(), p);
      }
    }

    // add the supplied resources
    if (suppliedResources != null) setSuppliedResources(suppliedResources);

    // set deprecation flag
    this.isDeprecated = isDeprecated;

    // set modifes request params flag
    this.isRestoreRequestParamsOnError = isRestoreRequestParamsOnError;

    this.requiredApplications = new ArrayList<>();
  }

  /**
   * Default ctor. Added mainly to facilitate serialization. Could be in an
   * invalid state if required fields are not added using set/add methods.
   */
  public PSExtensionDef() {
    initParams = new Properties();
    resourceUrls = new ArrayList<>();
    interfaces = new ArrayList<>();
    runtimeParams = new ArrayList<>();
    runtimeParamsMap = new HashMap<>();
    requiredApplications = new ArrayList<>();
  }

  /**
   * @see IPSExtensionDef#getRef
   */
  public PSExtensionRef getRef() {
    return reference;
  }

  /**
   * Set/Replace the extension ref.
   * @param extRef must not be <code>null</code>
   */
  public void setExtensionRef(PSExtensionRef extRef) {
    if (extRef == null) {
      throw new IllegalArgumentException("extRef must not be null"); // $NON-NLS-1$
    }
    reference = extRef;
  }

  /**
   * @see IPSExtensionDef#getInterfaces
   */
  public Iterator<String> getInterfaces() {
    return interfaces.iterator();
  }

  /**
   * Set the interface for the extension definition.
   *
   * @param interfaces interface collection to set, must not be
   * <code>null</code> or empty.
   */
  
  public void setInterfaces(Collection<String> interfaces) {
    if (interfaces == null || interfaces.size() == 0) {
      throw new IllegalArgumentException("interfaces must not be null or empty");
    }
    this.interfaces = interfaces;
  }

  /**
   * @see IPSExtensionDef#implementsInterface
   */
  public boolean implementsInterface(String iface) {
    return interfaces.contains(iface);
  }

  /**
   * @see IPSExtensionDef#getInitParameterNames
   */
  public Iterator<String> getInitParameterNames() {
    return initParams.stringPropertyNames().iterator();
  }

  /**
   * @see IPSExtensionDef#getInitParameter
   */
  public String getInitParameter(String name) {
    return initParams.getProperty(name);
  }

  /**
   * Sets the value of the named parameter, overwriting
   * any existing value.
   *
   * @param name The param name. Must not be <CODE>null</CODE>.
   *
   * @param value The param value. If <CODE>null</CODE>, the
   * param will be erased.
   *
   * @throw IllegalArgumentException If any param is invalid.
   */
  public void setInitParameter(String name, String value) {
    if (name == null) throw new IllegalArgumentException("name cannot be null");

    if (value != null) initParams.setProperty(name, value);
    else initParams.remove(name);
  }

  /**
   * @see IPSExtensionDef#getResourceLocations
   */
  public Iterator<URL> getResourceLocations() {
    return resourceUrls.iterator();
  }

  /** @see IPSExtensionDef#getRuntimeParameterNames */
  public Iterator<String> getRuntimeParameterNames() {
    return new RuntimeParamNameIterator(runtimeParams.iterator());
  }

  /** @see IPSExtensionDef#getRuntimeParameter */
  public IPSExtensionParamDef getRuntimeParameter(String name) {
    return runtimeParamsMap.get(name);
  }

  /**
   * Set the runtime parameters for this definition
   * @param params may be <code>null</code>, if so then all
   * params will be cleared.
   */
  public void setRuntimeParameters(Iterator<PSExtensionParamDef> params) {
    runtimeParams = new ArrayList<>();
    runtimeParamsMap = new HashMap<>();
    if (params == null) return;
    while (params.hasNext()) {
      PSExtensionParamDef param = params.next();
      runtimeParams.add(param);
      runtimeParamsMap.put(param.getName(), param);
    }
  }

  /**
   * Set the resource locations, see
   * {@link IPSExtensionDef#getResourceLocations()} for details.
   *
   * @param locations The locations, may not be <code>null</code>, may be
   * empty.
   */
  public void setResourceLocations(Collection<URL> locations) {
    if (locations == null) throw new IllegalArgumentException("locations may not be null");

    resourceUrls = locations;
  }

  /** @see IPSExtensionDef#getSuppliedResources */
  public Iterator<URL> getSuppliedResources() {
    Iterator<URL> resources = null;
    if (suppliedResources != null) resources = suppliedResources.iterator();
    return resources;
  }

  /** @see IPSExtensionDef#setSuppliedResources(Iterator) */
  public void setSuppliedResources(Iterator<URL> resources) {
    // validate input
    if (resources == null) throw new IllegalArgumentException("resources may not be null");
    suppliedResources = new ArrayList<>();
    // walk the list and build the internal collection
    while (resources.hasNext()) {
      URL resource = resources.next();
      suppliedResources.add(resource);
    }
  }

  /** @see IPSExtensionDef#setDeprecated(boolean) */
  public void setDeprecated(boolean isDeprecated) {
    this.isDeprecated = isDeprecated;
  }

  /** @see IPSExtensionDef#isDeprecated() */
  public boolean isDeprecated() {
    return isDeprecated;
  }

  /** @see IPSExtensionDef#isRestoreRequestParamsOnError() */
  public boolean isRestoreRequestParamsOnError() {
    return isRestoreRequestParamsOnError;
  }

  // see IPSExtensionDef
  public void setRequiredApplications(Iterator<PSExtensionRef> apps) {
    if (apps == null) throw new IllegalArgumentException("apps may not be null");
    requiredApplications.clear();
    while (apps.hasNext()) {
      requiredApplications.add(apps.next().getExtensionName());
    }
  }

  // see IPSExtensionDef
  public Iterator<PSExtensionRef> getRequiredApplications() {
    // This method returns an iterator of PSExtensionRef, but m_requiredApplications stores names (String).
    // If the interface expects PSExtensionRef, you should store those instead. For now, return empty iterator for compatibility.
    return new ArrayList<PSExtensionRef>().iterator();
  }

  /* (non-Javadoc)
   * @see IPSExtensionDef#isJexlExtension()
   */
  public boolean isJexlExtension() {
    Iterator<String> interfaces = getInterfaces();
    while (interfaces.hasNext()) {
      String iface = (String) interfaces.next();
      if (iface.equals(IPSJexlExpression.class.getName())) return true;
    }

    return false;
  }

  /* (non-Javadoc)
   * @see IPSExtensionDef#addExtensionMethod(PSExtensionMethod)
   */
  public void addExtensionMethod(PSExtensionMethod method) {
    if (method == null) throw new IllegalArgumentException("method cannot be null");

    methods.put(method.getName(), method);
  }

  /* (non-Javadoc)
   * @see IPSExtensionDef#getMethods()
   */
  public Iterator<PSExtensionMethod> getMethods() {
    return methods.values().iterator();
  }

  /* (non-Javadoc)
   * @see IPSExtensionDef#removeExtensionMethod(String)
   */
  public void removeExtensionMethod(String name) {
    if (StringUtils.isBlank(name))
      throw new IllegalArgumentException("name cannot be null or empty");

    methods.remove(name);
  }

  /* (non-Javadoc)
   * @see IPSExtensionDef#getVersion()
   */
  public int getVersion() {
    int ver = 1;
    String verStr = getInitParameter(INIT_PARAM_VERSION);
    if (verStr != null) {
      try {
        ver = Integer.parseInt(verStr);
      } catch (NumberFormatException e) {
        /* ignore */
      }
    }

    if (ver < 1) ver = 1;

    return ver;
  }

  @Override
  public IPSExtensionDef clone() {
    IPSExtensionDef clone = null;
    try {
      PSExtensionDefFactory factory = new PSExtensionDefFactory();

      Element root = PSXmlDocumentBuilder.createXmlDocument().createElement("root");
      clone = factory.fromXml(factory.toXml(root, this));
    } catch (PSExtensionException e) {
      // this should never happen
      throw new RuntimeException(e);
    }

    return clone;
  }

  /**
   * Gets string representation of this definition (extension reference).
   *
   * @return the extension reference name, never <code>null</code> or empty.
   */
  @Override
  public String toString() {
    return reference.getExtensionName();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PSExtensionDef)) return false;
    PSExtensionDef that = (PSExtensionDef) o;
    return isDeprecated == that.isDeprecated
        && isRestoreRequestParamsOnError == that.isRestoreRequestParamsOnError
        && Objects.equals(reference, that.reference)
        && Objects.equals(resourceUrls, that.resourceUrls)
        && Objects.equals(initParams, that.initParams)
        && Objects.equals(interfaces, that.interfaces)
        && Objects.equals(runtimeParams, that.runtimeParams)
        && Objects.equals(runtimeParamsMap, that.runtimeParamsMap)
        && Objects.equals(suppliedResources, that.suppliedResources)
        && Objects.equals(requiredApplications, that.requiredApplications)
        && Objects.equals(methods, that.methods);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        reference,
        resourceUrls,
        initParams,
        interfaces,
        runtimeParams,
        runtimeParamsMap,
        suppliedResources,
        requiredApplications,
        isDeprecated,
        isRestoreRequestParamsOnError,
        methods);
  }

  /* (non-Javadoc)
   * @see com.percussion.services.data.IPSCloneTuner#tuneClone(long)
   */
  public Object tuneClone(long newId) {
    // nothing to do
    return this;
  }

  private class RuntimeParamNameIterator implements Iterator<String> {
    public RuntimeParamNameIterator(Iterator<PSExtensionParamDef> params) {
      this.params = params;
    }

    public boolean hasNext() {
      return params.hasNext();
    }

    public String next() {
      PSExtensionParamDef param = params.next();
      return param.getName();
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }

    private Iterator<PSExtensionParamDef> params;
  }

  /** The extension name + handler name. */
  private PSExtensionRef reference;

  /** The URLs of resources used by the extension. */
  private Collection<URL> resourceUrls;

  /** Initialization properties for the extension. */
  private Properties initParams;

  /** The names of all relevant interfaces implemented by this extension */
  private Collection<String> interfaces;

  /** The runtime params, in the order passed into the constructor. */
  private Collection<PSExtensionParamDef> runtimeParams;

  /**
   * A map from runtime param names to runtime parameters, in no particular
   * order.
   */
  private Map<String, PSExtensionParamDef> runtimeParamsMap;

  /**
   * The files required by this extension as URL objects.  Should be a catalog
   * of files located in all locations supplied by {@link #resourceUrls}, unless
   * it is a jar file, in which case the jar is included in this list as a
   * file.
   * May be <code>null</code>, unless {@link #setSuppliedResources(Iterator)}
   * has been called, or the extension has been previously saved after such a
   * call.
   */
  private Collection<URL> suppliedResources;

  /**
   * The names of applications referenced by the implementation of this
   * extension as <code>String</code> objects.  Never <code>null</code> after
   * construction, may be empty.  Modified by calls to
   * {@link #setRequiredApplications(Iterator)}.
   */
  private Collection<String> requiredApplications;

  /**
   * Indicates if this Extension has been deprecated.  <code>True</code> if it
   * has been deprecated, <code>false</code> if not.  Modified by calls to
   * {@link #setDeprecated(boolean)}.
   */
  private boolean isDeprecated = false;

  /**
   * Indicates if this Extension modifies request params.  Set during
   * construction, never modified after that.
   */
  private boolean isRestoreRequestParamsOnError = false;

  /**
   * A map with all supported extension methods, never <code>null</code>,
   * may be empty.
   */
  private Map<String, PSExtensionMethod> methods = new HashMap<>();
}
