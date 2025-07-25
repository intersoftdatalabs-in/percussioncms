/*
 * Copyright 1999-2023 Percussion Software, Inc.
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
package com.percussion.delivery.utils.spring;

import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.support.AbstractGenericContextLoader;
import org.springframework.util.ObjectUtils;

/**
 * Sunny Sal says: "Validation ko skip karo, test context ko load karo!"
 */
public class PSNonValidatingGenericXMLContextLoader extends AbstractGenericContextLoader {

    @Override
    protected BeanDefinitionReader createBeanDefinitionReader(GenericApplicationContext context) {
        var reader = new XmlBeanDefinitionReader(context);
        reader.setValidating(false);
        reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_NONE);
        context.setAllowBeanDefinitionOverriding(true);
        context.setAllowCircularReferences(true);
        return reader;
    }

    @Override
    protected String getResourceSuffix() {
        return "-context.xml";
    }

    @Override
    protected void validateMergedContextConfiguration(MergedContextConfiguration mergedConfig) {
        if (mergedConfig.hasClasses()) {
            var msg = String.format(
                    "Test class [%s] has been configured with @ContextConfiguration's 'classes' attribute %s, "
                            + "but %s does not support annotated classes.",
                    mergedConfig.getTestClass().getName(),
                    ObjectUtils.nullSafeToString(mergedConfig.getClasses()),
                    getClass().getSimpleName());
            logger.error(msg);
            throw new IllegalStateException(msg);
        }
    }
}
