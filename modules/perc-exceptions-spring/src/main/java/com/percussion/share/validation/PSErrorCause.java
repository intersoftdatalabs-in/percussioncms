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
package com.percussion.share.validation;

import com.percussion.security.SecureStringUtils;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.collections.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A JAXB serializable Exception wrapper.
 *
 * @author adamgent
 */
@XmlRootElement(name = "ErrorCause")
public class PSErrorCause {

  private static final Logger log = LogManager.getLogger("Server");
  private PSErrorCause errorCause;
  private List<PSErrorCauseElement> errorCauseStackTrace;
  private StackTraceElement[] stackTrace;

  private String message;
  private String localizedMessage;
  private Throwable cause;

  /** Default no-arg constructor required for JAXB serialization. */
  public PSErrorCause() {
    super();
  }

  /**
   * Constructs a wrapper for the given cause without sending the stack trace to the client.
   *
   * @param cause the cause to wrap, may be {@code null}.
   */
  public PSErrorCause(Throwable cause) {
    init(cause, false);
  }

  /**
   * Initializes the wrapper with the given throwable and stack-trace visibility flag.
   *
   * @param t the throwable to wrap, may be {@code null}.
   * @param sendErrorStackToClient when {@code true}, the stack trace is included for delivery to
   *     the client.
   */
  protected void init(Throwable t, boolean sendErrorStackToClient) {
    setLocalizedMessage(t.getLocalizedMessage());
    setMessage(t.getMessage());

    if (sendErrorStackToClient) {
      setStackTrace(t.getStackTrace());
    }

    setCause(t.getCause());
  }

  /**
   * Returns the nested cause wrapper, if any.
   *
   * @return the nested {@link PSErrorCause}, may be {@code null}.
   */
  public PSErrorCause getErrorCause() {
    return errorCause;
  }

  /**
   * Sets the nested cause wrapper.
   *
   * @param cause the nested cause wrapper, may be {@code null}.
   */
  public void setErrorCause(PSErrorCause cause) {
    this.errorCause = cause;
  }

  /**
   * Returns the underlying throwable that this wrapper was built from. Not serialized by JAXB.
   *
   * @return the underlying throwable, may be {@code null}.
   */
  @XmlTransient
  public Throwable getCause() {
    return cause;
  }

  /**
   * Sets the underlying throwable and updates the nested cause wrapper accordingly.
   *
   * @param cause the underlying throwable, may be {@code null}.
   */
  public void setCause(Throwable cause) {
    this.cause = cause;
    if (cause != null) {
      setErrorCause(new PSErrorCause(cause));
    }
  }

  /**
   * Returns the list of {@link PSErrorCauseElement} entries that make up the cause stack trace.
   *
   * @return the cause stack trace list, may be {@code null}.
   */
  public List<PSErrorCauseElement> getErrorCauseStackTrace() {
    return errorCauseStackTrace;
  }

  /**
   * Replaces the cause stack trace with the supplied list.
   *
   * @param errorCauseStackTrace the new cause stack trace list, may be {@code null}.
   */
  public void setErrorCauseStackTrace(List<PSErrorCauseElement> errorCauseStackTrace) {
    this.errorCauseStackTrace = errorCauseStackTrace;
  }

  /**
   * Returns the combined stack trace reconstructed from the cause stack and the explicit stack
   * trace. Not serialized by JAXB.
   *
   * @return the combined stack trace, never {@code null} but may be empty.
   */
  @XmlTransient
  public StackTraceElement[] getStackTrace() {
    List<StackTraceElement> stack = new ArrayList<>();
    if (getErrorCauseStackTrace() != null) {
      for (PSErrorCauseElement element : getErrorCauseStackTrace()) {
        stack.add(element.getStackTraceElement());
      }
    }
    if (stackTrace != null) {
      CollectionUtils.addAll(stack, stackTrace);
    }
    return stack.toArray(new StackTraceElement[] {});
  }

  /**
   * Sets the explicit stack trace and rebuilds the serializable cause stack trace list.
   *
   * @param stackTrace the new stack trace, may be {@code null}.
   */
  public void setStackTrace(StackTraceElement[] stackTrace) {
    this.stackTrace = stackTrace;
    if (stackTrace != null) {
      errorCauseStackTrace = new ArrayList<>();
      for (StackTraceElement st : stackTrace) {
        errorCauseStackTrace.add(new PSErrorCauseElement(st));
      }
    }
  }

  /**
   * Returns the message of the underlying throwable.
   *
   * @return the message, may be {@code null}.
   */
  public String getMessage() {
    return message;
  }

  /**
   * Sets the message after sanitizing it for safe HTML rendering.
   *
   * @param message the message to sanitize and store, may be {@code null}.
   */
  public void setMessage(String message) {
    this.message = SecureStringUtils.sanitizeStringForHTML(message);
  }

  /**
   * Returns the localized message of the underlying throwable.
   *
   * @return the localized message, may be {@code null}.
   */
  public String getLocalizedMessage() {
    return localizedMessage;
  }

  /**
   * Sets the localized message after sanitizing it for safe HTML rendering.
   *
   * @param localizedMessage the localized message to sanitize and store, may be {@code null}.
   */
  public void setLocalizedMessage(String localizedMessage) {
    this.localizedMessage = SecureStringUtils.sanitizeStringForHTML(localizedMessage);
  }

  /**
   * JAXB-serializable representation of a single {@link StackTraceElement}.
   *
   * @author adamgent
   */
  public static class PSErrorCauseElement {

    private String className;
    private String fileName;
    private int lineNumber;
    private String methodName;

    /** Default no-arg constructor required for JAXB serialization. */
    public PSErrorCauseElement() {
      super();
    }

    /**
     * Reconstructs the underlying {@link StackTraceElement} from the serialized fields.
     *
     * @return the reconstructed stack trace element, never {@code null}.
     */
    public StackTraceElement getStackTraceElement() {
      return new StackTraceElement(getClassName(), getMethodName(), getFileName(), getLineNumber());
    }

    /**
     * Constructs an element by copying the relevant fields from the supplied stack trace element.
     *
     * @param element the stack trace element to copy, never {@code null}.
     */
    public PSErrorCauseElement(StackTraceElement element) {
      setClassName(element.getClassName());
      setFileName(element.getFileName());
      setLineNumber(element.getLineNumber());
      setMethodName(element.getMethodName());
    }

    /**
     * Returns the fully qualified class name of the stack frame.
     *
     * @return the class name, may be {@code null}.
     */
    @XmlAttribute
    public String getClassName() {
      return className;
    }

    /**
     * Sets the fully qualified class name of the stack frame.
     *
     * @param className the class name, may be {@code null}.
     */
    public void setClassName(String className) {
      this.className = className;
    }

    /**
     * Returns the source file name of the stack frame.
     *
     * @return the file name, may be {@code null}.
     */
    @XmlAttribute
    public String getFileName() {
      return fileName;
    }

    /**
     * Sets the source file name of the stack frame.
     *
     * @param fileName the file name, may be {@code null}.
     */
    public void setFileName(String fileName) {
      this.fileName = fileName;
    }

    /**
     * Returns the line number of the stack frame.
     *
     * @return the line number; values &lt; 0 indicate an unknown line.
     */
    @XmlAttribute
    public int getLineNumber() {
      return lineNumber;
    }

    /**
     * Sets the line number of the stack frame.
     *
     * @param lineNumber the line number; pass a negative value when unknown.
     */
    public void setLineNumber(int lineNumber) {
      this.lineNumber = lineNumber;
    }

    /**
     * Returns the method name of the stack frame.
     *
     * @return the method name, may be {@code null}.
     */
    @XmlAttribute
    public String getMethodName() {
      return methodName;
    }

    /**
     * Sets the method name of the stack frame.
     *
     * @param methodName the method name, may be {@code null}.
     */
    public void setMethodName(String methodName) {
      this.methodName = methodName;
    }
  }

  /** */
  private static final long serialVersionUID = -3237445850903443415L;
}
