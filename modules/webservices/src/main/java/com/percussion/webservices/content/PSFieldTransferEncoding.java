package com.percussion.webservices.content;

public enum PSFieldTransferEncoding {
  none,
  base64;

  public static PSFieldTransferEncoding fromString(String s) {
    if (s == null) return null;
    return PSFieldTransferEncoding.valueOf(s);
  }

  /** Returns the enum value as a String. */
  public String getValue() {
    return name();
  }
}
