package com.percussion.webservices.assemblydesign;

import com.percussion.webservices.assembly.data.PSTemplateSlot;

public class CreateSlotsResponse {
    private PSTemplateSlot[] createSlotsResponse;

    public PSTemplateSlot[] getCreateSlotsResponse() {
        return createSlotsResponse;
    }

    public void setCreateSlotsResponse(PSTemplateSlot[] createSlotsResponse) {
        this.createSlotsResponse = createSlotsResponse;
    }
}
