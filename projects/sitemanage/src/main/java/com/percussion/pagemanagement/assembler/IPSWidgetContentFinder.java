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

package com.percussion.pagemanagement.assembler;

import com.percussion.services.assembly.IPSContentFinder;

/**
 * Widget finders calculate which assets are related to a particular page or template via a widget.
 * Each widget finder is responsible for filtering the returned list of assembly items from the
 * context passed into the find method.
 *
 * <p>Widget finders are reusable across pages or templates and are referenced by the widget
 * preferences.
 */
public interface IPSWidgetContentFinder extends IPSContentFinder<PSWidgetInstance> {
  // Marker interface for widget content finders.
}
