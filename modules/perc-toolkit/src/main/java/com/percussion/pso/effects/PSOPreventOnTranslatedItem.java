/*
 * Copyright 1999-2025 Percussion Software, Inc.
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
package com.percussion.pso.effects;

// REFACTORED: CP-JAVA11
import com.percussion.cms.objectstore.PSRelationshipFilter;
import com.percussion.design.objectstore.PSLocator;
import com.percussion.extension.IPSExtensionDef;
import com.percussion.extension.PSExtensionException;
import com.percussion.extension.PSExtensionProcessingException;
import com.percussion.extension.PSParameterMismatchException;
import com.percussion.relationship.IPSEffect;
import com.percussion.relationship.IPSExecutionContext;
import com.percussion.relationship.PSEffectResult;
import com.percussion.relationship.annotation.PSEffectContext;
import com.percussion.relationship.annotation.PSHandlesEffectContext;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.server.IPSRequestContext;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.guidmgr.PSGuidManagerLocator;
// ...existing code...
import com.percussion.webservices.PSErrorException;
import com.percussion.webservices.content.IPSContentWs;
import com.percussion.webservices.content.PSContentWsLocator;
import com.percussion.webservices.system.IPSSystemWs;
import com.percussion.webservices.system.PSSystemWsLocator;
import java.io.File;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// ...existing code...

/**
 * A Percussion CMS relationship effects that prevents operations on content items which are
 * Translated.
 *
 * <p>It can be used with any relationship (you must add it to the relationship configuration), but
 * it is intended to prevent creating a Translation of a Translation or a Promotable Version of a
 * Translation.
 *
 * @author DavidBenua // REFACTORED: CP-JAVA11
 */
@PSHandlesEffectContext(
    optional = {
      PSEffectContext.PRE_CONSTRUCTION,
      PSEffectContext.PRE_DESTRUCTION,
      PSEffectContext.PRE_UPDATE,
      PSEffectContext.PRE_CHECKIN,
      PSEffectContext.PRE_CLONE,
      PSEffectContext.PRE_WORKFLOW
    })
/**
 * PSOPreventOnTranslatedItem class.
 */
public class PSOPreventOnTranslatedItem implements IPSEffect {
  /**
   * Creates a new PSOPreventOnTranslatedItem.
   */
  public PSOPreventOnTranslatedItem() {
    // default
  }


  /** Logger for this class */
  private static final Logger log = LogManager.getLogger(PSOPreventOnTranslatedItem.class);

  /** cws. */
  protected static IPSContentWs cws = null;
  /** sws. */
  protected static IPSSystemWs sws = null;
  /** gmgr. */
  protected static IPSGuidManager gmgr = null;

  /**
   * Initialize service pointers.
   * initServices operation.
   *
   */
  protected static void initServices() {
    if (cws == null) {
      cws = PSContentWsLocator.getContentWebservice();
      sws = PSSystemWsLocator.getSystemWebservice();
      gmgr = PSGuidManagerLocator.getGuidMgr();
    }
  }

  /**
   * Tests if this relationship owner is a translation of some other item.
   * test operation.
   * @param params the params
   * @param req the req
   * @param exCtx the ex ctx
   * @param result the result
   * @throws PSExtensionProcessingException if an error occurs
   * @throws PSParameterMismatchException if an error occurs
   *
   */
  public void test(
      Object[] params, IPSRequestContext req, IPSExecutionContext exCtx, PSEffectResult result)
      throws PSExtensionProcessingException, PSParameterMismatchException {
    initServices();

    // TODO: Replace deprecated isConstruction()/isDestruction() with context type check when
    // available
    // For now, always run effect for backward compatibility
    try {
      var owner = exCtx.getCurrentRelationship().getOwner().getId();
      var transownerId = findTranslationOwner(owner);
      if (transownerId > 1) {
        log.debug("Item is a translation of {} preventing relationship", transownerId);
        result.setError(MSG_TRANSLATED_ITEM);
        return;
      }
    } catch (Exception e) {
      log.error("unexpected exception, Error: {}", PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new PSExtensionProcessingException(this.getClass().getName(), e);
    }
    result.setSuccess();
  }

  private static final String MSG_TRANSLATED_ITEM =
      "This operation is not valid on translated items";

  /**
   * attempt operation.
   *
   * @param params the params
   * @param request the request
   * @param context the context
   * @param result the result
   * @throws PSExtensionProcessingException if an error occurs
   * @throws PSParameterMismatchException if an error occurs
   */
  public void attempt(
      Object[] params,
      IPSRequestContext request,
      IPSExecutionContext context,
      PSEffectResult result)
      throws PSExtensionProcessingException, PSParameterMismatchException {
    result.setSuccess();
  }

  /**
   * recover operation.
   *
   * @param params the params
   * @param request the request
   * @param exCtx the ex ctx
   * @param e the e
   * @param result the result
   * @throws PSExtensionProcessingException if an error occurs
   */
  @Override
  public void recover(
      Object[] params,
      IPSRequestContext request,
      IPSExecutionContext exCtx,
      PSExtensionProcessingException e,
      PSEffectResult result)
      throws PSExtensionProcessingException {
    result.setSuccess();
  }

  /**
   * init operation.
   *
   * @param def the def
   * @param codeRoot the code root
   * @throws PSExtensionException if an error occurs
   */
  public void init(IPSExtensionDef def, File codeRoot) throws PSExtensionException {}

  /**
   * findTranslationOwner operation.
   *
   * @param id the id
   * @return the result
   * @throws PSErrorException if an error occurs
   */
  public int findTranslationOwner(int id) throws PSErrorException {

    var filter = new PSRelationshipFilter();
    filter.setCategory(PSRelationshipFilter.FILTER_CATEGORY_TRANSLATION);
    var dependent = new PSLocator(id, -1);
    var guid = gmgr.makeGuid(dependent);
    filter.limitToEditOrCurrentOwnerRevision(true);
    var parents = sws.findOwners(guid, filter);

    if (parents.size() > 1) {
      log.error("Item {} has more than one translation parent", id);
      return -1;
    } else if (parents.size() == 1) {
      return parents.get(0).getUUID();
    } else {
      return -1;
    }
  }
}
