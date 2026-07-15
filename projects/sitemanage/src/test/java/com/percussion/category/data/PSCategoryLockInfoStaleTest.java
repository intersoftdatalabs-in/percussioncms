package com.percussion.category.data;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.codehaus.jettison.json.JSONObject;
import org.junit.jupiter.api.Test;

/** Regression for GH-1182 / v8.1.7 PR #1173: stale category locks are detected. */
class PSCategoryLockInfoStaleTest {

  @Test
  void isLockStaleWhenSessionMissing() throws Exception {
    var json = new JSONObject();
    json.put("sessionId", "nonexistent-session-id-for-stale-lock-test");
    json.put("userName", "tester");
    // No live session for this id → stale
    assertTrue(PSCategoryLockInfo.isLockStale(json));
  }

  @Test
  void isLockStaleFalseForNullOrBlankSession() throws Exception {
    assertFalse(PSCategoryLockInfo.isLockStale(null));
    var json = new JSONObject();
    json.put("sessionId", "");
    assertFalse(PSCategoryLockInfo.isLockStale(json));
  }
}
