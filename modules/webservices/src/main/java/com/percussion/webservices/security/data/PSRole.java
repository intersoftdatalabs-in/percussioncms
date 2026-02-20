package com.percussion.webservices.security.data;

/**
 * Compatibility shim: expose PSRole in the historical package used by system code. Delegates to
 * generated PSRole in the securityservices package via inheritance.
 */
public class PSRole extends PSRoleGen {
  // No changes; exists for backward-compatible package placement
}
