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
package com.percussion.ant;

import java.io.File;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * Task to create the workbench's help hints xml file and moves the required resources to a
 * specified folder. This task requires that the Jericho Html parser jar be on the classpath. *
 */
public class PSMakeHelpHints extends Task {

  /**
   * Creates a new task instance with default settings.
   */
  public PSMakeHelpHints() {}

  /**
   * The location of the help mappings file (Required)
   *
   * @param helpMappings the file containing help mappings, must not be <code>null</code>
   */
  public void setHelpmappings(File helpMappings) {
    m_helpMappings = helpMappings;
  }

  /**
   * The help plugin directory where all the help files can be found (Required)
   *
   * @param helpPath the directory containing help plugin files, must not be <code>null</code>
   */
  public void setHelppath(File helpPath) {
    m_helpPath = helpPath;
  }

  /**
   * The target path for the file to be created (Required)
   *
   * @param target the target file path where the help hints XML will be written, must not be <code>null</code>
   */
  public void setTarget(File target) {
    m_target = target;
  }

  /*
   * @see org.apache.tools.ant.Task#execute()
   */
  @Override
  public void execute() throws BuildException {
    if (m_helpMappings == null) throw new BuildException("helpmappings is required.");
    if (m_helpPath == null) throw new BuildException("helppath is required.");
    if (m_target == null) throw new BuildException("target is required.");

    try {
      PSHelpHintFileCreator creator =
          new PSHelpHintFileCreator(m_helpMappings, m_helpPath, m_target);
      creator.createFile();

    } catch (Exception e) {
      throw new BuildException(e);
    }
  }

  private File m_helpMappings;
  private File m_helpPath;
  private File m_target;
}
