package com.percussion.webservices.content;

public enum PSFieldTransferEncoding {
  none,
  base64;

  public static PSFieldTransferEncoding fromString(String s) {
    if (s == null) return null;
    return PSFieldTransferEncoding.valueOf(s);
  }
}
