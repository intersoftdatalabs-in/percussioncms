package com.percussion.webservices.assembly;

import com.percussion.error.PSMissingBeanConfigurationException;

/**
 * Compatibility wrapper that delegates to the shim locator in the services package.
 */
public class PSAssemblyWsLocator
{
    public static IPSAssemblyWs getAssemblyWebservice() throws PSMissingBeanConfigurationException
    {
        return com.percussion.services.shim.ws.assembly.PSAssemblyWsLocator.getAssemblyWebservice();
    }

    public static IPSAssemblyDesignWs getAssemblyDesignWebservice() throws PSMissingBeanConfigurationException
    {
        return com.percussion.services.shim.ws.assembly.PSAssemblyWsLocator.getAssemblyDesignWebservice();
    }
}
