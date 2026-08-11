// REFACTORED: CP-JAVA11
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
package com.percussion.share.data;

import com.percussion.share.service.exception.PSDataServiceException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.collections.Transformer;

/**
 * Abstract transformer for converting objects of type OLD to type NEW.
 *
 * @param <OLD> the source type
 * @param <NEW> the target type
 */
public abstract class PSAbstractTransformer<OLD, NEW> implements Transformer {

  /**
   * Collects and transforms a collection of OLD objects to a list of NEW objects.
   *
   * @param old the collection of OLD objects
   * @return a list of NEW objects
   */
  public List<NEW> collect(Collection<OLD> old) {
    // Prefer typed loop over CollectionUtils.collect (raw Transformer API).
    var newList = new ArrayList<NEW>(old.size());
    for (OLD item : old) {
      try {
        newList.add(doTransform(item));
      } catch (PSDataServiceException e) {
        throw new RuntimeException(e);
      }
    }
    return newList;
  }

  @Override
  @SuppressWarnings("unchecked") // commons-collections Transformer is raw; domain type is OLD
  public Object transform(Object old) {
    try {
      return doTransform((OLD) old);
    } catch (PSDataServiceException e) {
      // Not sure how to handle the error state here, so wrap in RuntimeException.
      throw new RuntimeException(e);
    }
  }

  /**
   * Performs the transformation from OLD to NEW.
   *
   * @param old the source object
   * @return the transformed object
   * @throws PSDataServiceException if transformation fails
   */
  protected abstract NEW doTransform(OLD old) throws PSDataServiceException;
}
