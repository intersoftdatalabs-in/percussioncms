package com.percussion.webservices.assembly;

import com.percussion.services.assembly.IPSTemplateSlot;
import com.percussion.webservices.assembly.data.PSAssemblyTemplateWs;

import java.util.List;

/**
 * Adapter that implements the webservices IPSAssemblyWs and delegates
 * calls to the shim implementation.
 */
public class AssemblyWsWrapper implements IPSAssemblyWs
{
    private final com.percussion.services.shim.ws.assembly.IPSAssemblyWs delegate;

    public AssemblyWsWrapper(final com.percussion.services.shim.ws.assembly.IPSAssemblyWs delegate)
    {
        this.delegate = delegate;
    }

    @Override
    public List<IPSTemplateSlot> loadSlots(String name)
    {
        return delegate.loadSlots(name);
    }

    @Override
    public List<PSAssemblyTemplateWs> loadAssemblyTemplates(String name, String contentType)
    {
        return delegate.loadAssemblyTemplates(name, contentType);
    }
}
