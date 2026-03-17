package com.percussion.ai.signing;

import dev.sigstore.oidc.client.OidcClients;

/** Utility to fetch a Sigstore OIDC ID token. */
public class OidcAuthenticator {
  public OidcAuthenticator() {
    super();
  }

  public static void main(String[] args) {
    try {
      System.err.println("Sigstore OIDC: Requesting identity token...");
      var token = OidcClients.PUBLIC_GOOD.getIDToken();
      System.err.println("Sigstore OIDC: Successfully authenticated.");
      // Print only the raw token to stdout so the shell script can capture it
      System.out.println(token.getIdToken());
    } catch (Exception e) {
      System.err.println("Error: Failed to fetch OIDC token: " + e.getMessage());
      if (e.getMessage() == null) {
        e.printStackTrace();
      }
      System.exit(1);
    }
  }
}
