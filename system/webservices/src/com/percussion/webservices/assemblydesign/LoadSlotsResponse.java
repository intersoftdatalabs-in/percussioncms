package com.percussion.webservices.assemblydesign;

import com.percussion.webservices.assembly.data.PSTemplateSlot;

public class LoadSlotsResponse {
    private PSTemplateSlot[] loadSlotsResponse;

    public PSTemplateSlot[] getLoadSlotsResponse() {
        return loadSlotsResponse;
    }

    public void setLoadSlotsResponse(PSTemplateSlot[] loadSlotsResponse) {
        this.loadSlotsResponse = loadSlotsResponse;
    }
}
