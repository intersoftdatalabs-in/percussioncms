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
package com.percussion.sitemanage.task.impl;


import com.percussion.cms.IPSConstants;
import com.percussion.sitemanage.task.IPSAntService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.listener.CommonsLoggingListener;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import static org.apache.commons.lang.Validate.noNullElements;
import static org.apache.commons.lang.Validate.notEmpty;
import static org.apache.commons.lang.Validate.notNull;
import static org.springframework.util.CollectionUtils.isEmpty;

/**
 * Executes ant scripts in the servers running JVM.
 * @author adamgent
 */
@Component("antService")
public class PSAntService implements IPSAntService {

    private  Vector<String> runningFiles = new Vector<>();
    
    /**
     * The log instance to use for this class, never <code>null</code>.
     */
    public static final Logger log = LogManager.getLogger(IPSConstants.PUBLISHING_LOG);

    
    
    /**
     * {@inheritDoc}
     */
    public boolean isRunning(String file) {
        log.debug("RunningFiles: {}", runningFiles);
        return runningFiles.contains(new File(file).getAbsolutePath());
    }
    

    /**
     * {@inheritDoc}
     */
    public String runAnt(AntScript script) {
        notNull(script, "script");
        String file = script.getFile();
        List<String> targets = script.getTargets();
        List<? extends BuildListener> listeners = script.getListeners();
        Map<String, String> properties = script.getProperties();
        boolean blocking = script.isBlocking();
        
        notEmpty(file);
        if (targets == null) {
            targets = new ArrayList<>();
        }
        else {
            noNullElements(targets);
        }
        
        if (listeners == null) {
            listeners = new ArrayList<>();
        }
        else {
            noNullElements(listeners);
        }
        
        
        final File buildFile = new File(file); 
        if (isRunning(file)) return buildFile.getAbsolutePath();
        final Project project = new Project();
        ProjectHelper helper = ProjectHelper.getProjectHelper();
        project.init();
        //project.setUserProperty("ant.version", getAntVersion());
        project.setUserProperty("ant.file", buildFile.getAbsolutePath());
        helper.parse(project, buildFile);
        String defaultTarget = project.getDefaultTarget();
        if (targets.isEmpty())
            targets.add(defaultTarget);
        final Vector<String> targetList = new Vector<>(targets);
        
        for (BuildListener bl : listeners) {
            project.addBuildListener(bl);
        }
        
        project.addBuildListener(getLogListener(script));
         
        for(Map.Entry<String, String> e : properties.entrySet()) {
            project.setNewProperty(e.getKey(), e.getValue());    
        }

        Runnable r = new Runnable() {
            public void run() {
                project.fireBuildStarted();
                Exception error = null;
                try {
                    project.executeTargets(targetList);
                } catch (Exception e) {
                    error = e;
                } finally {
                    project.fireBuildFinished(error);
                    runningFiles.remove(buildFile.getAbsolutePath());
                }
            }

        };
        
        runningFiles.add(buildFile.getAbsolutePath());
        if (blocking) {
            r.run();
        } else {
            Thread t = new Thread(r);
            t.start();
        }
        return buildFile.getAbsolutePath();
        
    }

    /**
     * If the script has a listener, use that one. If not, create an instance of
     * {@link CommonsLoggingListener} and use that one.
     * 
     * @param script the {@link AntScript} to get the listener from.
     * @return the listener that the script has, or a new instance of
     *         {@link CommonsLoggingListener} if the script does not have
     *         listeners.
     */
    private BuildListener getLogListener(AntScript script)
    {
        if(!isEmpty(script.getListeners()))
        {
            return script.getListeners().get(0);
        }
        else
        {
            CommonsLoggingListener logListener = new CommonsLoggingListener();
            logListener.setMessageOutputLevel(getLogLevel());
            return logListener;
        }
    }
    
    private int getLogLevel() {
        /*
         * Add a logging listener to log the build to the server console.
         */
        int logLevel = Project.MSG_INFO;
        logLevel = log.isFatalEnabled() ? Project.MSG_ERR : logLevel;
        logLevel = log.isErrorEnabled() ? Project.MSG_ERR : logLevel;
        logLevel = log.isWarnEnabled() ? Project.MSG_WARN : logLevel;
        logLevel = log.isInfoEnabled() ? Project.MSG_INFO : logLevel;
        logLevel = log.isDebugEnabled() ? Project.MSG_DEBUG : logLevel;
        logLevel = log.isTraceEnabled() ? Project.MSG_DEBUG : logLevel;
        return logLevel;
        
    }
}

