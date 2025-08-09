public interface SecurityProvider {
  /**
   * Authenticates a user based on provided credentials.
   * @param username The username.
   * @param password The password or token.
   * @return true if authentication is successful, false otherwise.
   */
  boolean authenticate(String username, String password);

  /**
   * Returns the name of the provider (e.g., "Database", "SSO").
   */
  String getProviderName();
}
