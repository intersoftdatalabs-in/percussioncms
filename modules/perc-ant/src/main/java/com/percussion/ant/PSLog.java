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
import java.io.FileWriter;
import java.io.IOException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * This task writes the specified message to the given log file. The file will be created if it
 * doesn't exist. The message will be appended to an existing file depending on the value of the
 * append attibute. <br>
 * Example Usage: <br>
 *
 * <pre>
 *
 * First set the taskdef:
 *
 *  <code>
 *  &lt;taskdef name="PSLog"
 *             class="com.percussion.ant.PSLog"
 *             classpath="c:\lib"/&gt;
 *  </code>
 *
 *  <code>
 *  &lt;PSLog file="c:/install.log" message="Installing" append="true"/&gt;
 *  </code>
 * </pre>
 */
public class PSLog extends Task {
  /** Creates a new task for writing a message to a log file. */
  public PSLog() {
    super();
  }

  @Override
  public void execute() throws BuildException {
    File logFile = null;
    FileWriter fWriter = null;
    try {
      logFile = new File(m_file);
      if (!logFile.exists()) {
        File parentDir = logFile.getParentFile();
        if (!parentDir.exists()) parentDir.mkdirs();
      } else if (!m_bAppend) logFile.delete();

      fWriter = new FileWriter(logFile, m_bAppend);
      fWriter.write(m_message + "\n");
    } catch (IOException ioe) {
      throw new BuildException(ioe);
    } finally {
      if (fWriter != null) {
        try {
          fWriter.close();
        } catch (IOException e) {
          throw new BuildException(e);
        }
      }
    }
  }

  /**
   * Sets the log file path.
   *
   * @param file the log file path, never <code>null</code> or empty.
   * @throws IllegalArgumentException if file is <code>null</code> or empty.
   */
  public void setFile(String file) {
    if (file == null || file.trim().length() == 0) {
      throw new IllegalArgumentException("file may not be null or empty");
    }

    m_file = file;
  }

  /**
   * Sets the message to log.
   *
   * @param message the message to write to the log file.
   */
  public void setMessage(String message) {
    m_message = message;
  }

  /**
   * Sets whether to append to the log file or overwrite it.
   *
   * @param append <code>true</code> to append, <code>false</code> to overwrite.
   */
  public void setAppend(boolean append) {
    m_bAppend = append;
  }

  /** The location of the log file, default is default.log. */
  private String m_file = "default.log";

  /** The message to log, never <code>null</code> may be empty. */
  private String m_message = "";

  /**
   * If <code>true</code>, messages will be appended to the log file if it exists, otherwise the
   * file will be overwritten with the new message.
   */
  private boolean m_bAppend = false;
}
