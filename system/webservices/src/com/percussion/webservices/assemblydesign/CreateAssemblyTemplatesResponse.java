package com.percussion.webservices.assemblydesign;

import com.percussion.webservices.assembly.data.PSAssemblyTemplate;

public class CreateAssemblyTemplatesResponse {
    private PSAssemblyTemplate[] createAssemblyTemplatesResponse;

    public PSAssemblyTemplate[] getCreateAssemblyTemplatesResponse() {
        return createAssemblyTemplatesResponse;
    }

    public void setCreateAssemblyTemplatesResponse(PSAssemblyTemplate[] createAssemblyTemplatesResponse) {
        this.createAssemblyTemplatesResponse = createAssemblyTemplatesResponse;
    }
}
