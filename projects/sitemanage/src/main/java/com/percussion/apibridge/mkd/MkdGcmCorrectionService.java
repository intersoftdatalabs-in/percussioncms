/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.percussion.apibridge.mkd;

import com.percussion.rest.i18n.I18nCorrectionSubmission;
// import dev.monkeyking.gcm.CorrectionSubmission;
// import dev.monkeyking.gcm.GcmClient;
// import dev.monkeyking.gcm.GcmException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Posts language corrections via the thin mkd-gcm Java SDK. Isolates JNA/native load so unit tests
 * can mock this type.
 */
public class MkdGcmCorrectionService {

  private static final Logger log = LogManager.getLogger(MkdGcmCorrectionService.class);

  /**
   * @param submission validated REST body
   * @return GCM Message-ID when available
   */
  public String postCorrection(I18nCorrectionSubmission submission) {
    /*    String group = MkdLanguageConfig.gcmGroup();
    String token = MkdLanguageConfig.gcmTokenPlain();
    if (StringUtils.isBlank(group) || StringUtils.isBlank(token)) {
      throw new IllegalStateException(
          "GCM not configured (set perc.mkd.gcm.group and perc.mkd.gcm.token in"
              + " server.properties)");
    }

    CorrectionSubmission sdk = toSdk(submission);
    try (GcmClient client = openClient()) {
      client.setBearerToken(token);
      String from = MkdLanguageConfig.gcmFrom();
      if (StringUtils.isNotBlank(from)) {
        client.setDefaultFrom(from);
      }
      String mid = client.postCorrection(group, sdk);
      log.info(
          "Submitted i18n correction messageId={} locale={} gcmMid={}",
          submission.getMessageId(),
          submission.getLocale(),
          mid);
      return mid;
    } catch (UnsatisfiedLinkError e) {
      // Product ships Win64/Linux64 natives only (mkd-gcm-natives 0.2.0 has no Darwin
      // artifacts). Opt-in GCM corrections need a loadable mkd_gcm_ffi for the host OS.
      throw new IllegalStateException(
          "mkd_gcm_ffi native library not found for this host (product ships Windows"
              + " x86_64 / Linux x86_64 under <installdir>/bin; macOS natives are not"
              + " bundled until upstream mkd-gcm-natives publishes them). Place the"
              + " library under <installdir>/bin and set LD_LIBRARY_PATH / PATH /"
              + " -Djna.library.path if you have a platform build.",
          e);
    } catch (GcmException e) {
      // Do not concatenate SDK message text (may echo tokens/PII) into the wrapper message.
      throw new MkdGcmBackendException("GCM rejected correction", e);
    } */
    return null; // TODO: remove this when mkd-gcm is available
  }

  /* protected GcmClient openClient() {
    String url = MkdLanguageConfig.gcmUrl();
    if (StringUtils.isNotBlank(url)) {
      return GcmClient.connectUrl(url);
    }
    String host = MkdLanguageConfig.gcmHost();
    if (StringUtils.isBlank(host)) {
      throw new IllegalStateException(
          "GCM host not configured (perc.mkd.gcm.host or perc.mkd.gcm.url)");
    }
    return GcmClient.connect(host, MkdLanguageConfig.gcmPort());
    return null; // TODO: remove this when mkd-gcm is available
  }

  static CorrectionSubmission toSdk(I18nCorrectionSubmission in) {
     CorrectionSubmission out = new CorrectionSubmission();
    out.currentText = in.getCurrentText();
    out.proposedText = in.getProposedText();
    out.currentAriaLabel = in.getCurrentAriaLabel();
    out.proposedAriaLabel = in.getProposedAriaLabel();
    out.ariaLabelledby = in.getAriaLabelledby();
    out.currentTitle = in.getCurrentTitle();
    out.messageId = in.getMessageId();
    out.notes = in.getNotes() == null ? "" : in.getNotes();
    out.email = in.getEmail();
    out.locale = in.getLocale();
    out.submittedAt = in.getSubmittedAt();
    I18nCorrectionSource src = in.getSource();
    if (src != null) {
      CorrectionSubmission.Source s = new CorrectionSubmission.Source();
      s.tagName = src.getTagName();
      s.matchReason = src.getMatchReason();
      s.elementId = src.getElementId();
      s.pageUrl = src.getPageUrl();
      out.source = s;
    }
    return out;

  } */
}
