// REFACTORED: CP-JAVA11
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
package com.percussion.services;

import com.percussion.cms.IPSConstants;
import com.percussion.error.PSExceptionUtils;
import com.percussion.error.PSMissingBeanConfigurationException;
import com.percussion.util.PSOsTool;
import com.percussion.utils.container.PSContainerUtilsFactory;
import com.percussion.utils.servlet.PSServletUtils;
import com.percussion.utils.spring.PSFileSystemXmlApplicationContext;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.mock.jndi.SimpleNamingContextBuilder;
import org.springframework.web.context.support.XmlWebApplicationContext;

import javax.naming.NamingException;
import javax.naming.spi.NamingManager;
import javax.servlet.ServletContext;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Creates the appropriate Spring context for use in locators. By configuring
 * this class appropriately, either a local or server configuration is loaded.
 * In particular, the {@link #init(ServletContext)} method is called by the
 * initialization servlet and sets up so that the server configuration is
 * loaded. Otherwise, the spring configuration uses the local definition and
 * initializes on first use.
 * 
 * @author dougrand
 */
public class PSBaseServiceLocator {

    /**
     * This static holds a reference to the context used to initialize Spring.
     * This is initialized from the application servlet, the initial call to
     * getBean, or one of the other <code>init</code> calls.
     */
    private static volatile ConfigurableApplicationContext ms_context = null;

    /**
     * This is set to <code>true</code> once the JNDI naming information is setup.
     */
    private static volatile boolean ms_setNamingContextBuilder = false;

    /**
     * The logger to use in this class
     */
    private static final Logger ms_logger = LogManager.getLogger(IPSConstants.SERVER_LOG);

    /**
     * The location of the configuration directory in the source tree
     */
    private static final File ms_configdir = new File("ear/config");

    /**
     * The location of the spring directory under the configuration tree
     */
    private static final File ms_fileconfig = new File(ms_configdir, "spring");

    /**
     * For dynamically generated configuration files.
     */
    private static final String ms_generatedFileConfig = "/com/percussion/testing/local-beans.xml";

    private static volatile boolean isInitialized = false;
    private static volatile boolean initializing = false;

    /**
     * Hibernate path within the application
     */
    private static final String ms_hibernatepath =
        "jetty/base/webapps/Rhythmyx/WEB-INF/classes";

    /**
     * Track all the application contexts that are loaded. We need to tear
     * them down during shutdown.
     */
    private static final List<ConfigurableApplicationContext> ms_ctxList =
        new ArrayList<>();

    /**
     * Read-write lock for thread-safe context operations
     */
    private static final ReentrantReadWriteLock contextLock = new ReentrantReadWriteLock();

    // Private constructor to prevent instantiation
    private PSBaseServiceLocator() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Initialize the configuration for the server context. Only call this within
     * a J2EE container.
     *
     * @param servletCtx the servlet context, must never be <code>null</code>
     * @throws IllegalArgumentException if servletCtx is null
     */
    public static void init(ServletContext servletCtx) {
        Objects.requireNonNull(servletCtx, "servletCtx must never be null");

        if (isInitialized) {
            return;
        }

        contextLock.writeLock().lock();
        try {
            if (isInitialized) {
                return;
            }

            initializing = true;
            ms_logger.info("Loading Container configuration");
            PSContainerUtilsFactory.getConfigurationContextInstance().load();
            ms_logger.info("Initializing Base Service Locator");

            XmlWebApplicationContext ctx = new XmlWebApplicationContext();
            ctx.setServletContext(servletCtx);

            String sysConfigDir = PSServletUtils.getSpringConfigPath();
            List<String> configFiles = buildConfigFilesList(sysConfigDir);

            String[] files = configFiles.toArray(new String[0]);
            ctx.setConfigLocations(files);
            ms_context = ctx;

            ctx.refresh();
            ms_ctxList.add(ctx);

            // try loading cataloger configs as child context
            ms_logger.info("Loading cataloger bean configurations");
            configFiles.clear();
            configFiles.add(sysConfigDir + "/" + PSServletUtils.CATALOGER_BEANS_FILE_NAME);
            Optional<XmlWebApplicationContext> childCtx = initChildCtx(ctx, configFiles);
            if (childCtx.isPresent()) {
                ms_context = childCtx.get();
            }

            // try loading user configs as child context
            List<String> userConfigFiles = getUserConfigFiles();
            if (!userConfigFiles.isEmpty()) {
                ms_logger.info("Loading user defined bean configurations");
                childCtx = initChildCtx(ctx, userConfigFiles);
                if (childCtx.isPresent()) {
                    ms_context = childCtx.get();
                }
            }

            isInitialized = true;
            initializing = false;
            ms_logger.info("Finished Initializing Base Service Locator");
        } finally {
            contextLock.writeLock().unlock();
        }
    }

    /**
     * Build the list of configuration files for Spring context initialization.
     *
     * @param sysConfigDir the system configuration directory path
     * @return list of configuration file paths, never null
     */
    private static List<String> buildConfigFilesList(String sysConfigDir) {
        return Stream.of(
            PSServletUtils.SERVER_BEANS_FILE_NAME,
            PSServletUtils.DESIGN_BEANS_FILE_NAME,
            PSServletUtils.DEPLOYER_BEANS_FILE_NAME,
            PSServletUtils.BEANS_FILE_NAME,
            PSServletUtils.IMAGEWIDGET_BEANS_FILE_NAME,
            PSServletUtils.IMAGEWIDGET_SERVLET_FILE_NAME,
            "projects" + File.separator + PSServletUtils.SITEMANAGE_FILE_NAME,
            "projects" + File.separator + PSServletUtils.PACKAGE_BEANS_FILE_NAME
        )
        .map(fileName -> sysConfigDir + File.separator + fileName)
        .collect(Collectors.toList());
    }

    /**
     * Create a child context using the supplied parent and list of config files,
     * logs any errors.
     *
     * @param parentCtx The context to set as the parent, assumed not
     * <code>null</code>.
     * @param configFiles The list of configuration files to use to initialize
     * the context, assumed not <code>null</code> or empty.
     *
     * @return The child context with the supplied parent set as its parent
     * context, or empty Optional if there is an error initializing the ctx.
     */
    private static Optional<XmlWebApplicationContext> initChildCtx(
        XmlWebApplicationContext parentCtx, List<String> configFiles) {
        try {
            String[] files = configFiles.toArray(new String[0]);
            XmlWebApplicationContext ctx = new XmlWebApplicationContext();
            ctx.setParent(ms_context);
            ctx.setServletContext(parentCtx.getServletContext());
            ctx.setConfigLocations(files);
            ctx.refresh();
            ms_ctxList.add(ctx);
            return Optional.of(ctx);
        } catch (Exception e) {
            ms_logger.error("Error loading child bean configurations: {}",
                PSExceptionUtils.getMessageForLog(e));
            return Optional.empty();
        }
    }

    /**
     * Initializes the Spring context with the config files specified.
     *
     * @param files the spring context config files
     * @throws PSMissingBeanConfigurationException if configuration fails
     */
    public static synchronized void initCtx(String[] files)
            throws PSMissingBeanConfigurationException {
        if (ms_context == null) {
            String[] fixedFiles = PSOsTool.isUnixPlatform()
                ? Arrays.stream(files)
                    .map(file -> "/" + file)
                    .toArray(String[]::new)
                : files;

            ms_context = new PSFileSystemXmlApplicationContext(fixedFiles);
        }
    }

    /**
     * Initializes the Spring context with the config files specified. Also
     * sets the hibernate configuration directory and initializes the initial
     * context.
     *
     * @param files the spring context config files
     * @param rxRoot the Rhythmyx root installation directory
     * @throws PSMissingBeanConfigurationException if configuration fails
     */
    public static synchronized void initCtxHib(String[] files, String rxRoot)
            throws PSMissingBeanConfigurationException {
        try {
            if (ms_context == null) {
                if (!ms_setNamingContextBuilder) {
                    NamingManager.setInitialContextFactoryBuilder(
                        new SimpleNamingContextBuilder());
                    ms_setNamingContextBuilder = true;
                }

                PSFileSystemXmlApplicationContext.setConfigDir(
                    new File(rxRoot, ms_hibernatepath));
                initCtx(files);
            }
        } catch (NamingException e) {
            ms_logger.error("Naming exception: {}", PSExceptionUtils.getMessageForLog(e));
            throw new PSMissingBeanConfigurationException("Failed to initialize naming context", e);
        }
    }

    /**
     * Checks to see if the base locator has been initialized.
     *
     * @return <code>true</code> if initialized
     */
    public static synchronized boolean isInitialized() {
        return ms_context != null;
    }

    /**
     * Sets the main application managed by this locator as a parent to the
     * supplied context. This method should be called after this locator's
     * context has been initialized, after the child context is constructed, but
     * before the child context is refreshed. The caller is responsible for
     * closing the supplied child context as necessary.
     *
     * @param ctx The context to set the parent on, may not be <code>null</code>.
     *
     * @throws IllegalStateException if {@link #isInitialized()} returns
     * <code>false</code>.
     * @throws IllegalArgumentException if ctx is null
     */
    public static void addAsParentCtx(ConfigurableApplicationContext ctx) {
        Objects.requireNonNull(ctx, "ctx may not be null");

        if (!isInitialized()) {
            throw new IllegalStateException("Base context must be initialized");
        }

        ctx.setParent(getCtx());
    }

    /**
     * Dynamically locate the user spring config files. Ignores files ending in
     * "-servlet.xml" as these are loaded by the dispatch servlet.
     *
     * @return The list of paths rooted relative to the servlet base directory,
     *         never <code>null</code>, may be empty.
     */
    private static List<String> getUserConfigFiles() {
        String userConfigPath = PSServletUtils.getUserSpringConfigPath();
        File userConfigDir = PSServletUtils.getUserSpringConfigDir();

        File[] files = userConfigDir.listFiles();
        if (files == null) {
            return new ArrayList<>();
        }

        return Arrays.stream(files)
            .filter(File::isFile)
            .map(File::getName)
            .filter(name -> !name.endsWith("-servlet.xml"))
            .map(name -> userConfigPath + "/" + name)
            .collect(Collectors.toList());
    }

    /**
     * Destroy the configuration for the server context as well as any parent
     * context. This should be called as part of server shutdown.
     */
    public static synchronized void destroy() {
        ms_logger.info("Destroying Base Service Locator");

        contextLock.writeLock().lock();
        try {
            for (int i = ms_ctxList.size() - 1; i >= 0; i--) {
                ConfigurableApplicationContext ctx = ms_ctxList.get(i);
                if (ctx != null) {
                    ApplicationContext parent = ctx.getParent();
                    if (parent instanceof ConfigurableApplicationContext) {
                        ConfigurableApplicationContext parentCtx = (ConfigurableApplicationContext) parent;
                        if (parentCtx.isActive()) {
                            parentCtx.close();
                        }
                    }
                    if (ctx.isActive()) {
                        ctx.close();
                    }
                }
            }
            ms_ctxList.clear();
            ms_context = null;
            isInitialized = false;
        } finally {
            contextLock.writeLock().unlock();
        }
    }

    /**
     * Lookup the given bean and return the bean from the spring configuration.
     *
     * @param beanName the bean's name, must never be <code>null</code> or empty
     * @return the bean, never <code>null</code>
     * @throws PSMissingBeanConfigurationException if the bean is unknown to spring
     * @throws IllegalArgumentException if beanName is null or empty
     */
    public static Object getBean(String beanName)
            throws PSMissingBeanConfigurationException {
        if (StringUtils.isBlank(beanName)) {
            throw new IllegalArgumentException("beanName may not be null or empty");
        }

        ApplicationContext ctx = getCtx();
        Objects.requireNonNull(ctx, "Application Context cannot be null");

        try {
            return ctx.getBean(beanName);
        } catch (BeansException e) {
            throw new PSMissingBeanConfigurationException("Bean " + beanName + " is unknown", e);
        }
    }

    /**
     * Retrieve and initialize (if necessary) the Spring configuration object.
     *
     * @return the configuration object, never <code>null</code>
     * @throws PSMissingBeanConfigurationException if configuration fails
     */
    public static ApplicationContext getCtx() throws PSMissingBeanConfigurationException {
        if (isInitialized) {
            return ms_context;
        }

        contextLock.writeLock().lock();
        try {
            if (isInitialized || initializing) {
                return ms_context;
            }

            initializing = true;

            if (!ms_setNamingContextBuilder) {
                ms_logger.info("Setting initial test JNDI context factory builder.");
                NamingManager.setInitialContextFactoryBuilder(
                    new SimpleNamingContextBuilder());
                ms_setNamingContextBuilder = true;
            }

            String rxDeployDir = System.getProperty("rxdeploydir");
            if (rxDeployDir != null && !rxDeployDir.isEmpty()) {
                loadFileConfig();
            } else {
                loadGenerated();
            }

            isInitialized = true;
            return ms_context;
        } catch
