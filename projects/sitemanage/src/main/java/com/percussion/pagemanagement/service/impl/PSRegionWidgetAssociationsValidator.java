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
package com.percussion.pagemanagement.service.impl;

import com.percussion.pagemanagement.data.PSRegionWidgetAssociations;
import com.percussion.pagemanagement.data.PSRegionWidgets;
import com.percussion.pagemanagement.data.PSWidgetItem;
import com.percussion.pagemanagement.service.IPSWidgetService;
import com.percussion.share.service.exception.PSBeanValidationException;
import com.percussion.share.service.exception.PSPropertiesValidationException;
import com.percussion.share.service.exception.PSSpringValidationException;
import com.percussion.share.validation.PSAbstractBeanValidator;
import org.springframework.validation.ObjectError;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Validates Region Widget Associations.
 * Ensures there are no duplicate regionIds and validates widget items.
 *
 * @param <BEAN> Page or Template
 * @author adamgent
 */
public abstract class PSRegionWidgetAssociationsValidator<BEAN> extends PSAbstractBeanValidator<BEAN> {

    private final IPSWidgetService widgetService;

    public PSRegionWidgetAssociationsValidator(IPSWidgetService widgetService) {
        super();
        this.widgetService = widgetService;
    }

    @Override
    protected void doValidation(BEAN bean, PSBeanValidationException e) {
        var wa = getWidgetAssociations(bean, e);
        if (wa != null) {
            doWidgetAssociations(wa, e);
        }
    }

    public abstract String getField();

    public abstract PSRegionWidgetAssociations getWidgetAssociations(BEAN wa, PSBeanValidationException e);

    protected void doWidgetAssociations(PSRegionWidgetAssociations associations, PSBeanValidationException e) {
        Set<String> ids = new HashSet<>();
        for (var ws : associations.getRegionWidgetAssociations()) {
            if (!ids.add(ws.getRegionId())) {
                e.reject("regionWidgetAssocations.dupIds", "Duplicate ids for region");
            }
            var items = ws.getWidgetItems();
            if (items != null) {
                for (var item : items) {
                    validateWidgetItem(item, e);
                }
            }
        }
    }

    protected void validateWidgetItem(PSWidgetItem widgetItem, PSBeanValidationException e) {
        try {
            var we = widgetService.validateWidgetItem(widgetItem);
            var errors = we.getAllErrors();
            if (errors != null && !errors.isEmpty()) {
                var messageBuilder = new StringBuilder();
                for (Iterator<ObjectError> iter = errors.iterator(); iter.hasNext(); ) {
                    var error = iter.next();
                    messageBuilder.append(error.getDefaultMessage());
                    if (iter.hasNext()) {
                        messageBuilder.append(",");
                    }
                }
                e.reject("regionWidgetAssocations.widgetItem", messageBuilder.toString());
            }
        } catch (PSPropertiesValidationException psPropertiesValidationException) {
            e.addSuppressed(psPropertiesValidationException);
        }
    }
}
