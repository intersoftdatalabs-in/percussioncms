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
package com.percussion.cms.objectstore.server;

import com.percussion.cms.PSDisplayChoices;
import com.percussion.cms.objectstore.IPSFieldCataloger;
import com.percussion.cms.objectstore.client.PSContentEditorFieldCataloger;
import com.percussion.cms.objectstore.client.PSLightWeightField;
import com.percussion.utils.request.PSRequestInfo;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FAILED IN JAVA1.8 - temporary Ignored
 * Tests {@link IPSFieldCataloger} methods in the {@link PSLocalCataloger}
 */
@Tag("IntegrationTest")
public class PSLocalCatalogerTest {

    /**
     * FAILED IN JAVA1.8 - temporary Ignored
     */
    @Test
    public void testNOOP() {
        assertTrue(true);
    }

    /**
     * FAILED IN JAVA1.8 - temporary Ignored
     * Test the field cataloger methods. Note that this does not currently test
     * all flag usage, but only the functionality modified for the Marlin
     * release.
     *
     * @throws Exception if the test fails.
     */
    @Disabled
    @Test
    public void ignored_testFieldCatalog() throws Exception {
        var cat = new PSLocalCataloger(PSRequestInfo.getRequestInfo(
                PSRequestInfo.KEY_PSREQUEST));

        PSContentEditorFieldCataloger ceCat;
        ceCat = new PSContentEditorFieldCataloger(cat, null,
                IPSFieldCataloger.FLAG_INCLUDE_ALL);
        var allMap = ceCat.getAll();

        var fields = new HashSet<String>();
        ceCat = new PSContentEditorFieldCataloger(cat, fields,
                IPSFieldCataloger.FLAG_INCLUDE_ALL);
        var allFields = getAllFieldNames(ceCat);

        var testMap = ceCat.getAll();

        var choiceFieldMap = new HashMap<String, PSDisplayChoices>();
        for (var key : allMap.keySet()) {
            var map1 = allMap.get(key);
            var map2 = testMap.get(key);
            assertNotNull(map2);
            assertEquals(map1.keySet(), map2.keySet());

            // add one field to set, check for choices
            var added = false;
            for (var obj : map1.keySet()) {
                var name = (String) obj;

                // skip workflow, community id system fields as they are special
                // cases referenced as part of choice filters for other fields
                if (name.equals("sys_workflowid") || name.equals("sys_communityid"))
                    continue;

                if (!added) {
                    fields.add(name);
                    added = true;
                }

                var field = (PSLightWeightField) map1.get(name);
                if (field.getDisplayChoices() != null &&
                        field.getDisplayChoices().areChoicesLoaded()) {
                    choiceFieldMap.put(name, field.getDisplayChoices());
                }
            }
        }

        // we need to have some choices
        assertFalse(choiceFieldMap.isEmpty());

        // test list of fields
        ceCat = new PSContentEditorFieldCataloger(cat, fields,
                IPSFieldCataloger.FLAG_INCLUDE_ALL);
        assertEquals(fields, getAllFieldNames(ceCat));

        // test choices
        var choiceFields = new HashSet<>(choiceFieldMap.keySet());
        ceCat = new PSContentEditorFieldCataloger(cat, choiceFields,
                IPSFieldCataloger.FLAG_EXCLUDE_CHOICES);
        var testSet = getAllFieldNames(ceCat);
        assertEquals(choiceFieldMap.keySet(), testSet);
        var choiceFieldSet = getAllFields(ceCat);
        for (var field : choiceFieldSet) {
            var choices = field.getDisplayChoices();
            assertNotNull(choices);
            assertFalse(choices.areChoicesLoaded());
            assertFalse(choices.getChoices().hasNext());

            // should still have a filter if one was defined
            var srcChoices = choiceFieldMap.get(field.getInternalName());
            assertEquals(choices.getChoiceFilter(), srcChoices.getChoiceFilter());
        }

        // test adding fields
        var noChoiceFields = new HashSet<>(allFields);
        noChoiceFields.removeAll(choiceFields);
        ceCat.loadFields(noChoiceFields, IPSFieldCataloger.FLAG_EXCLUDE_CHOICES);
        testSet = getAllFieldNames(ceCat);
        assertTrue(testSet.containsAll(noChoiceFields));
        assertTrue(testSet.containsAll(choiceFields));

        // choices should still have none loaded
        var testFields = getAllFields(ceCat);
        checkChoices(choiceFields, testFields, false);

        // check ctype map
        var typeMap = ceCat.getLocalContentTypeMap();
        assertEquals(ceCat.getLocalMap().keySet(), getAllFieldNames(typeMap));
        for (var typeFields : typeMap.values()) {
            checkChoices(choiceFields, typeFields, false);
        }

        // test adding fields w/ choices
        ceCat.loadFields(null, IPSFieldCataloger.FLAG_INCLUDE_ALL);
        testFields = getAllFields(ceCat);
        checkChoices(choiceFields, testFields, true);
        typeMap = ceCat.getLocalContentTypeMap();
        assertEquals(ceCat.getLocalMap().keySet(), getAllFieldNames(typeMap));
        for (var typeFields : typeMap.values()) {
            checkChoices(choiceFields, typeFields, true);
        }

        // ensure recataloging doesn't override
        ceCat.loadFields(choiceFields, IPSFieldCataloger.FLAG_EXCLUDE_CHOICES);
        testFields = getAllFields(ceCat);
        checkChoices(choiceFields, testFields, true);
        typeMap = ceCat.getLocalContentTypeMap();

        assertTrue(getAllFieldNames(ceCat).containsAll(getAllFieldNames(typeMap)));
        for (var typeFields : typeMap.values()) {
            checkChoices(choiceFields, typeFields, true);
        }

        // test no fields in ctor
        ceCat = new PSContentEditorFieldCataloger(cat,
                IPSFieldCataloger.FLAG_EXCLUDE_CHOICES);
        assertEquals(IPSFieldCataloger.FLAG_EXCLUDE_CHOICES,
                ceCat.getControlFlags());
        assertEquals(0, getAllFields(ceCat).size());

        // catalog w/no choices
        ceCat.loadFields(fields, IPSFieldCataloger.FLAG_EXCLUDE_CHOICES, false);
        checkChoices(choiceFields, getAllFields(ceCat), false);

        // catalog w/choices, no refresh, should still get them
        ceCat.loadFields(fields, IPSFieldCataloger.FLAG_INCLUDE_ALL, false);
        checkChoices(choiceFields, getAllFields(ceCat), true);

        // recatalog no choices, no refresh, should still have them
        ceCat.loadFields(fields, IPSFieldCataloger.FLAG_EXCLUDE_CHOICES, false);
        checkChoices(choiceFields, getAllFields(ceCat), true);

        // recatalog no choices, with refresh, should still have choices.
        ceCat.loadFields(fields, IPSFieldCataloger.FLAG_EXCLUDE_CHOICES, true);
        checkChoices(choiceFields, getAllFields(ceCat), true);

    }

    /**
     * Check if the supplied fields have choices loaded appropriately.
     *
     * @param choiceFields The set of field names that contain choices, assumed
     *                     not <code>null</code>.
     * @param testFields   A collection of fields to test, assumed not
     *                     <code>null</code>.
     * @param choicesLoaded <code>true</code> if the fields in the collection
     *                      that contain choices should have them loaded, <code>false</code> if not.
     * @throws Exception if the test fails or there are any errors.
     */
    private void checkChoices(Set<String> choiceFields,
                             Collection<PSLightWeightField> testFields, boolean choicesLoaded)
            throws Exception {
        for (var field : testFields) {
            if (choiceFields.contains(field.getInternalName())) {
                assertNotNull(field.getDisplayChoices());
                assertEquals(field.getDisplayChoices().areChoicesLoaded(),
                        choicesLoaded);
            } else if (field.getDisplayChoices() != null) {
                assertEquals(field.getDisplayChoices().areChoicesLoaded(),
                        choicesLoaded);
            }
        }
    }

    /**
     * Get all field names from the catalog.
     *
     * @param ceCat The catalog, assumed not <code>null</code>.
     * @return the set, never <code>null</code>, may be empty.
     */
    private Set<String> getAllFieldNames(PSContentEditorFieldCataloger ceCat) {
        var testMap = ceCat.getAll();
        var testSet = new HashSet<String>();
        for (var key : testMap.keySet()) {
            var map = (Map) testMap.get(key);
            for (var obj : map.keySet()) {
                var name = (String) obj;
                testSet.add(name);
            }
        }
        return testSet;
    }

    /**
     * Get all fields from the catalog.
     *
     * @param ceCat The catalog, assumed not <code>null</code>.
     * @return the set, never <code>null</code>, may be empty.
     */
    private Set<PSLightWeightField> getAllFields(
            PSContentEditorFieldCataloger ceCat) {
        var testMap = ceCat.getAll();
        var fieldSet = new HashSet<PSLightWeightField>();
        for (var key : testMap.keySet()) {
            var map = (Map) testMap.get(key);
            for (var obj : map.values()) {
                fieldSet.add((PSLightWeightField) obj);
            }
        }
        return fieldSet;
    }

    /**
     * Get all field names based on the supplied type map.
     *
     * @param typeMap Map of content type name to collection of fields, assumed
     *                not <code>null</code>.
     * @return The set of fieldnames, never <code>null</code>.
     */
    private Set<String> getAllFieldNames(Map<String,
            Collection<PSLightWeightField>> typeMap) {
        var results = new HashSet<String>();
        for (var coll : typeMap.values()) {
            for (var field : coll) {
                results.add(field.getInternalName());
            }
        }
        return results;
    }
}
