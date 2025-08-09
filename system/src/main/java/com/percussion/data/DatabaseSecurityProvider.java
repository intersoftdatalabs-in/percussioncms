package com.percussion.data;

public class DatabaseSecurityProvider implements SecurityProvider {

  @Override
  public boolean authenticate(String username, String password) {
    // Implement database authentication logic here
    // For example, check against a user table in the database
    if (username == null || password == null) {
      return false;
    }
    // Simulate a successful authentication for demonstration purposes
    return "admin".equals(username) && "password".equals(password);
  }

  @Override
  public String getProviderName() {
    return "Database";
  }
}
