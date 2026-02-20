package com.percussion.webservices.ui.data;

public enum SearchViewParentCategory {
  content,
  system,
  ui;

  public static SearchViewParentCategory fromString(String s) {
    if (s == null) return null;
    return SearchViewParentCategory.valueOf(s);
  }
}
