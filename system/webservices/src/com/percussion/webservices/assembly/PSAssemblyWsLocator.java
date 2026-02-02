package com.percussion.webservices.assembly;

import com.percussion.error.PSMissingBeanConfigurationException;

/**
 * Compatibility wrapper that delegates to the shim locator in the services package.
 */
public class PSAssemblyWsLocator
{
    public static IPSAssemblyWs getAssemblyWebservice() throws PSMissingBeanConfigurationException
    {
        var shim = com.percussion.services.shim.ws.assembly.PSAssemblyWsLocator.getAssemblyWebservice();
        return new AssemblyWsWrapper(shim);
    }

    public static IPSAssemblyDesignWs getAssemblyDesignWebservice() throws PSMissingBeanConfigurationException
    {
        // the design webservice is still returned by the shim locator and should be compatible
        // when possible adapt similarly, but for now return the shim and rely on casting at call sites
        return (IPSAssemblyDesignWs) com.percussion.services.shim.ws.assembly.PSAssemblyWsLocator.getAssemblyDesignWebservice();
    }
}
