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
package com.percussion.deployer.server;

import com.percussion.deployer.objectstore.PSApplicationIDTypeMapping;
import com.percussion.deployer.objectstore.idtypes.PSBindingIdContext;
import com.percussion.deployer.objectstore.idtypes.PSBindingParamIdContext;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

/** A helper class for managing the IDs found in a JEXL binding. */
public class PSJexlExpressionHelper {
  private final Map<Integer, PSJexlBindingParamOccurence> m_paramMap = new HashMap<>();

  /**
   * Constructs a new helper from the supplied id type mapping.
   *
   * @param mapping the application id type mapping, may not be <code>null</code>.
   */
  public PSJexlExpressionHelper(PSApplicationIDTypeMapping mapping) {
    Validate.notNull(mapping, "Mapping may not be null");

    var paramCtx = (PSBindingParamIdContext) mapping.getContext();
    var bindingCtx = (PSBindingIdContext) paramCtx.getParentCtx();
    if (bindingCtx == null) {
      throw new IllegalStateException(
          "A param context exists without a parent binding information");
    }

    var pVal = paramCtx.getParam().getValueText();
    var pMap =
        m_paramMap.computeIfAbsent(bindingCtx.getIndex(), k -> new PSJexlBindingParamOccurence());
    var occurMap = pMap.getOccurence(pVal);

    if (occurMap == null) {
      occurMap = new PSJexlOccurence();
      occurMap.addValue(paramCtx.getOccurence(), "");
      pMap.addOccurence(pVal, occurMap);
    } else if (!occurMap.hasKey(paramCtx.getOccurence())) {
      occurMap.addValue(paramCtx.getOccurence(), "");
    }
  }

  /**
   * Returns the param occurrence from the supplied index.
   *
   * @param ix the index, may be any integer.
   * @return the param occurrence, may be <code>null</code>.
   */
  public PSJexlBindingParamOccurence getParamOccurenceFromIndex(int ix) {
    return m_paramMap.get(ix);
  }

  /**
   * Returns the JEXL binding param occurrence value for the supplied context.
   *
   * @param oldVal the old value, may not be <code>null</code> or empty.
   * @param pCtx the param context, may not be <code>null</code>.
   * @param occurNum the occurrence number.
   * @return the occurrence value, may be <code>null</code>.
   */
  public String getJexlBindingParamOccurenceValue(
      String oldVal, PSBindingParamIdContext pCtx, int occurNum) {
    Validate.notBlank(oldVal, "Old value may not be null or empty");
    Validate.notNull(pCtx, "Param context may not be null");

    var bindingCtx = (PSBindingIdContext) pCtx.getParentCtx();
    if (bindingCtx == null) {
      throw new IllegalStateException("JEXL param context must have a parent binding context");
    }

    var pMap = getParamOccurenceFromIndex(bindingCtx.getIndex());
    if (pMap == null) {
      return null;
    }

    var occurMap = pMap.getOccurence(oldVal);
    return (occurMap != null && occurMap.hasKey(pCtx.getOccurence()))
        ? occurMap.getValue(pCtx.getOccurence())
        : null;
  }

  /**
   * Updates the binding parameter with a new value for the supplied context.
   *
   * @param oldVal the old value, may not be <code>null</code> or empty.
   * @param newVal the new value, may not be <code>null</code>.
   * @param pCtx the param context, may not be <code>null</code>.
   */
  public void updateBindingParam(String oldVal, String newVal, PSBindingParamIdContext pCtx) {
    Validate.notBlank(oldVal, "Old value may not be null or empty");
    Validate.notNull(newVal, "New value may not be null");
    Validate.notNull(pCtx, "Param context may not be null");

    var bindingCtx = (PSBindingIdContext) pCtx.getParentCtx();
    if (bindingCtx == null) {
      throw new IllegalStateException("JEXL param context must have a parent binding context");
    }

    var pOccur = getParamOccurenceFromIndex(bindingCtx.getIndex());
    if (pOccur == null) {
      return;
    }

    var occurMap = pOccur.getOccurence(oldVal);
    if (occurMap != null && occurMap.hasKey(pCtx.getOccurence())) {
      occurMap.addValue(pCtx.getOccurence(), newVal);
    }
  }

  /**
   * An occurence map keeps track of the occurence of this id and its new value like this: It means
   * for an oldValue=301, there are 3 occurences in a JEXL expression, the first occurence has been
   * replaced with 14, the second occurence was replaced with 1001 and the third one isn't yet
   * replaced.<br>
   * "301" -->
   * <li>{0, "14" }
   * <li>{1, "1001"}
   * <li>{2, "" }
   */
  class PSJexlOccurence {
    /** map of occurence(Integer) with its value String */
    private Map<Integer, String> m_values = new HashMap<>();

    /**
     * Based on the occurence number, get its value
     *
     * @see PSJexlOccurence
     * @param ix
     * @return the new or old value
     */
    protected String getValue(int ix) {
      return m_values.get(ix);
    }

    /**
     * check to see if there is such an occurence
     *
     * @param occurence
     * @return true if the occurence exists
     */
    protected boolean hasKey(int occurence) {
      return m_values.get(occurence) == null ? false : true;
    }

    /**
     * Add the value with its occurence, if it exists remove and add it back
     *
     * @param occurence the occurence of this value in the jex expression
     * @param newValue may be empty or <code>null</code>
     */
    protected void addValue(int occurence, String newValue) {
      m_values.remove(occurence);
      m_values.put(occurence, newValue);
    }
  }

  /**
   * This class holds a map of all the ids with an occurence map. Ex: <"301", Map<(int)occurence,
   * String newVal>
   *
   * @see PSJexlOccurence
   */
  class PSJexlBindingParamOccurence {
    /**
     * a map to hold the old id value references in an expression
     *
     * @see PSJexlOccurence
     */
    private Map<String, PSJexlOccurence> m_occur = new HashMap<>();

    /**
     * with the given id, get all the occurences of that id in an jexl expression
     *
     * @param paramVal the old id value
     * @return the occurence map, may be <code>null</code>
     */
    protected PSJexlOccurence getOccurence(String paramVal) {
      return m_occur.get(paramVal);
    }

    /**
     * for a given id, add the occurence map
     *
     * @param pValue old id value never <code>null</code>
     * @param jom the occurence map @see PSJexlOccurence
     */
    protected void addOccurence(String pValue, PSJexlOccurence jom) {
      if (StringUtils.isBlank(pValue))
        throw new IllegalArgumentException("id value may not be null");

      PSJexlOccurence curMap = m_occur.get(pValue);
      if (curMap == null) m_occur.put(pValue, jom);
      else {
        m_occur.remove(pValue);
        m_occur.put(pValue, jom);
      }
    }
  }
}
