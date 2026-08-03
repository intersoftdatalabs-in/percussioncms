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
// REFACTORED: CP-JAVA11

package com.percussion.rx.config.impl;

import com.percussion.rx.config.IPSConfigHandler.ObjectState;
import com.percussion.rx.config.PSConfigException;
import com.percussion.rx.design.IPSAssociationSet;
import com.percussion.services.content.data.PSKeyword;
import com.percussion.services.content.data.PSKeywordChoice;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.utils.types.PSPair;
import java.util.ArrayList;
import java.util.List;

/**
 * Sets the properties of keyword.
 *
 * @author bjoginipally
 */
public class PSKeywordSetter extends PSSimplePropertySetter {

  /** Default constructor for use by Spring. */
  public PSKeywordSetter() {}

  @Override
  protected boolean applyProperty(
      Object obj,
      ObjectState state,
      List<IPSAssociationSet> aSets,
      String propName,
      Object propValue)
      throws Exception {
    if (!(obj instanceof PSKeyword)) {
      throw new IllegalArgumentException("obj type must be PSKeyword.");
    }

    var kw = (PSKeyword) obj;
    if (CHOICES_PAIRS.equals(propName)) {
      setChoices(kw, propValue);
    } else {
      super.applyProperty(obj, state, aSets, propName, propValue);
    }

    return true;
  }

  @Override
  protected Object getPropertyValue(Object obj, String propName) throws PSNotFoundException {
    if (CHOICES_PAIRS.equals(propName)) {
      var kw = (PSKeyword) obj;
      var choices = new ArrayList<PSPair<String, String>>();
      for (var c : kw.getChoices()) {
        choices.add(new PSPair<>(c.getLabel(), c.getValue()));
      }
      return choices;
    }
    return super.getPropertyValue(obj, propName);
  }

  /**
   * Creates choice for each supplied pair and adds the choices to the keyword.
   *
   * @param kw The object of the keyword assumed not null.
   * @param propValue The choice values object.
   */
  private void setChoices(PSKeyword kw, Object propValue) {
    if (!(propValue instanceof List)) {
      throw new PSConfigException("The value type of the " + CHOICES_PAIRS + " must be a List");
    }

    var choices = (List<PSPair<String, String>>) propValue;
    var kwChoices = new ArrayList<PSKeywordChoice>();
    for (int i = 0; i < choices.size(); i++) {
      var pair = choices.get(i);
      var ch = new PSKeywordChoice();
      ch.setLabel(pair.getFirst());
      ch.setValue(pair.getSecond());
      ch.setSequence(i);
      kwChoices.add(ch);
    }
    kw.setChoices(kwChoices);
  }

  /** The property name for the keyword choices. */
  public static final String CHOICES_PAIRS = "choicePairs";
}
