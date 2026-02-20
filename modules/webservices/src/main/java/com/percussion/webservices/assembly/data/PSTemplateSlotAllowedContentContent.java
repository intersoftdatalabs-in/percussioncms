package com.percussion.webservices.assembly.data;

public class PSTemplateSlotAllowedContentContent {
  private long contentTypeId;
  private long templateId;

  public PSTemplateSlotAllowedContentContent() {}

  public PSTemplateSlotAllowedContentContent(long contentTypeId, long templateId) {
    this.contentTypeId = contentTypeId;
    this.templateId = templateId;
  }

  public long getContentTypeId() {
    return contentTypeId;
  }

  public void setContentTypeId(long contentTypeId) {
    this.contentTypeId = contentTypeId;
  }

  public long getTemplateId() {
    return templateId;
  }

  public void setTemplateId(long templateId) {
    this.templateId = templateId;
  }
}
