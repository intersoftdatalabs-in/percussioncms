package com.percussion.webservices.assemblydesign;

import com.percussion.webservices.assembly.data.PSAssemblyTemplate;

public class LoadAssemblyTemplatesResponse {
    private PSAssemblyTemplate[] loadAssemblyTemplatesResponse;

    public PSAssemblyTemplate[] getLoadAssemblyTemplatesResponse() {
        return loadAssemblyTemplatesResponse;
    }

    public void setLoadAssemblyTemplatesResponse(PSAssemblyTemplate[] loadAssemblyTemplatesResponse) {
        this.loadAssemblyTemplatesResponse = loadAssemblyTemplatesResponse;
    }
}
