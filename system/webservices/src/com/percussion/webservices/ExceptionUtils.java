package com.percussion.webservices;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Local shim for webservices to provide the minimal ExceptionUtils API used
 * by existing webservice code. Keeps behavior simple and stable (stack trace
 * as a string).
 */
public final class ExceptionUtils {

    private ExceptionUtils() { /* utility */ }

    public static String getFullStackTrace(Throwable t) {
        if (t == null) return "";
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    public static String getFullStackTrace(Exception e) {
        return getFullStackTrace((Throwable) e);
    }

    public static String getFullStackTrace(RuntimeException e) {
        return getFullStackTrace((Throwable) e);
    }

    public static String getFullStackTrace(com.percussion.services.locking.PSLockException e) {
        return getFullStackTrace((Throwable) e);
    }

    public static String getFullStackTrace(com.percussion.services.assembly.PSAssemblyException e) {
        return getFullStackTrace((Throwable) e);
    }

    public static String getFullStackTrace(com.percussion.error.PSException e) {
        return getFullStackTrace((Throwable) e);
    }

    public static String getFullStackTrace(com.percussion.cms.PSCmsException e) {
        return getFullStackTrace((Throwable) e);
    }

    public static String getFullStackTrace(java.lang.Error e) {
        return getFullStackTrace((Throwable) e);
    }

    /*
     * Compatibility helpers: some modules import org.apache.commons.lang3.exception.ExceptionUtils
     * and call getStackTrace(...). Provide those method signatures here so those callers work
     * without changing all imports at once.
     */
    public static String getStackTrace(Throwable t) {
        return getFullStackTrace(t);
    }

    public static String getStackTrace(Exception e) {
        return getFullStackTrace((Throwable) e);
    }

    public static String getStackTrace(RuntimeException e) {
        return getFullStackTrace((Throwable) e);
    }

    public static String getStackTrace(com.percussion.services.assembly.PSAssemblyException e) {
        return getFullStackTrace((Throwable) e);
    }

    public static String getStackTrace(com.percussion.error.PSException e) {
        return getFullStackTrace((Throwable) e);
    }
}
