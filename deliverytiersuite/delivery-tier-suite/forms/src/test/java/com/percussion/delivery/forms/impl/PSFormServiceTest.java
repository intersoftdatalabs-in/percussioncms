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

package com.percussion.delivery.forms.impl;

import com.percussion.delivery.forms.data.IPSFormData;
import com.percussion.delivery.forms.data.PSFormData;
import com.percussion.delivery.utils.spring.PSNonValidatingGenericXMLContextLoader;
import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 *
 * @author miltonpividori
 *
 */
@ExtendWith(SpringExtension.class)
@WebAppConfiguration(value = "src/test/webapp" )
@ActiveProfiles({"dev", "integration"})
@ContextHierarchy({
        @ContextConfiguration(loader= PSNonValidatingGenericXMLContextLoader.class,locations={"file:src/test/webapp/WEB-INF/test-beans.xml"})
})
@TestPropertySource(inheritLocations = true, inheritProperties = true,
        locations = {"file:src/test/webapp/WEB-INF/perc-security.properties",
        "file:src/test/webapp/WEB-INF/perc-datasources.properties",
        "file:src/test/webapp/WEB-INF/perc-form-processor.properties"})
public class PSFormServiceTest extends PSBaseFormServiceTest
{

    @BeforeEach
    public void setup(){

    }

    @Test
    public void testSave_NullForm()
    {
        try
        {
            formService.save(null);
            fail("save didn't throw an exception with null form");
        }
        catch(IllegalArgumentException ex)
        {

        }
    }

    @Test
    public void testSave()
    {
        String[] fieldValue1 = new String[] { "field1", "value1" };
        String[] fieldValue2 = new String[] { "field2", "value2" };
        String[] multipleValuesField = new String[] {
                "multipleValuesField",
                StringUtils.join(new String[] { "first", "second", "third" },
                        FIELD_VALUES_SEPARATOR)
        };

        IPSFormData formData =
                generateFormData("testform1",
                        fieldValue1[0], fieldValue1[1],
                        fieldValue2[0], fieldValue2[1],
                        multipleValuesField[0], multipleValuesField[1]);

        formService.save(formData);

        List<IPSFormData> allForms = formService.findAllForms();

        assertEquals(1, allForms.size());
        assertNotNull(allForms.get(0).getName());


        assertEquals(3, allForms.get(0).getFields().size());
        for (String fieldName : allForms.get(0).getFields().keySet())
        {
            String value = allForms.get(0).getFields().get(fieldName);
            value = value.replace(FIELD_VALUES_SEPARATOR,"");
            if (fieldName.equals(fieldValue1[0]))
            {
                assertEquals( fieldValue1[1], value);
            }
            else if (fieldName.equals(fieldValue2[0]))
            {
                assertEquals(fieldValue2[1], value);
            }
            else if (fieldName.equals(multipleValuesField[0]))
            {
                String mValue = multipleValuesField[1];
                value = value.replace("\\",FIELD_VALUES_SEPARATOR);

                assertEquals(mValue, value);

            }
            else
                fail("invalid field name");
        }




    }

    @Test
    public void testSave_InvalidForm()
    {
        String[] fieldValue1 = new String[] { "field1", "value1" };
        String[] fieldValue2 = new String[] { "field2", "value2" };
        String[] multipleValuesField = new String[] {
                "multipleValuesField",
                StringUtils.join(new String[] { "first", "second", "third" },
                        FIELD_VALUES_SEPARATOR)
        };

        IPSFormData formData =
                generateFormData("testfor!m1",
                        fieldValue1[0], fieldValue1[1],
                        fieldValue2[0], fieldValue2[1],
                        multipleValuesField[0], multipleValuesField[1]);
        try {
            formService.save(formData);
            fail("Invalid form name should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {

        }

    }

    @Test
    public void testFilterInvalidForm()
    {
        addInvalidFormToDb();
        try
        {
            List<IPSFormData> allForms = formService.findAllForms();
            for (IPSFormData form : allForms)
            {
                if (form.getName().startsWith(INVALID_FORM_NAME_PREFIX))
                    fail("Found invalid form " + form.getName() + " that should have been filtered by service");
            }
            List<String> formNames = formService.findDistinctFormNames();
            for (String formName : formNames)
            {
                if (formName.startsWith(INVALID_FORM_NAME_PREFIX))
                    fail("Found invalid form name" + formName + " that should have been filtered by service");
            }
        }
        finally
        {
            removeInvalidFormsFromDb();
        }
    }

    @Test
    public void testDelete_NonExistingForm()
    {
        String[] fieldValue1 = new String[] { "field1", "value1" };
        String[] fieldValue2 = new String[] { "field2", "value2" };

        IPSFormData formData =
                generateFormData("testform1",
                        fieldValue1[0], fieldValue1[1],
                        fieldValue2[0], fieldValue2[1]);

        formService.delete(formData);
    }

    @Test
    public void testDelete()
    {
        String[] fieldValue1 = new String[] { "field1", "value1" };
        String[] fieldValue2 = new String[] { "field2", "value2" };

        IPSFormData formData =
                generateFormData("testform1",
                        fieldValue1[0], fieldValue1[1],
                        fieldValue2[0], fieldValue2[1]);

        formService.save(formData);
        List<IPSFormData> allForms = formService.findAllForms();
        assertEquals(1, allForms.size());

        formService.delete(allForms.get(0));

        assertEquals(0, getAllForms().size());
    }

    @Test
    public void testMarkAsExported()
    {
        String[] fieldValue1 = new String[] { "field1", "value1" };
        String[] fieldValue2 = new String[] { "field2", "value2" };

        for (int i=0; i<10; i++)
        {
            IPSFormData formData =
                    generateFormData("testform" + i,
                            fieldValue1[0], fieldValue1[1],
                            fieldValue2[0], fieldValue2[1]);

            formService.save(formData);
        }

        List<IPSFormData> allForms = formService.findAllForms();

        formService.markAsExported(allForms.subList(0, 5));

        // Assert
        allForms = formService.findAllForms();
        assertEquals(10,allForms.size());

        for (int i = 0; i < 5; i++)
        {
            IPSFormData form = allForms.get(i);

            assertTrue(form.isExported() == 'y');
        }

        for (int i = 5; i < 10; i++)
        {
            IPSFormData form = allForms.get(i);

            assertTrue(form.isExported() == 'n');
        }
    }

    @Test
    public void testMarkAsExported_NonExistingForms()
    {
        Collection<IPSFormData> forms = new ArrayList<IPSFormData>();
        String[] fieldValue1 = new String[] { "field1", "value1" };
        String[] fieldValue2 = new String[] { "field2", "value2" };

        for (int i=0; i<10; i++)
        {
            PSFormData formData =
                    generateFormData("testform1" + i,
                            fieldValue1[0], fieldValue1[1],
                            fieldValue2[0], fieldValue2[1]);

            forms.add(formData);
        }

        formService.markAsExported(forms);
    }

    @Test
    public void testGetExportedFormCount_WithFormNameArgument()
    {
        String[] fieldValue1 = new String[] { "field1", "value1" };
        String[] fieldValue2 = new String[] { "field2", "value2" };

        for (int i=0; i<10; i++)
        {
            IPSFormData formData =
                    generateFormData("testform" + (i == 3 ? 2 : i),
                            fieldValue1[0], fieldValue1[1],
                            fieldValue2[0], fieldValue2[1]);

            formService.save(formData);
        }

        formService.markAsExported(getAllForms());

        // Assert
        String testFormName = "testform2";

        long count = formService.getExportedFormCount(testFormName);
        assertEquals( 2, count);

        count = formService.getExportedFormCount(testFormName.toUpperCase());
        assertEquals( 2, count);
    }

    @Test
    public void testGetExportedFormCount_FormNameWithSpecialCharacters()
    {
        String[] fieldValue1 = new String[] { "field1", "value1" };
        String[] fieldValue2 = new String[] { "field2", "value2" };

        for (int i=0; i<10; i++)
        {
            IPSFormData formData =
                    generateFormData("testform" + (i == 3 ? 2 : i),
                            fieldValue1[0], fieldValue1[1],
                            fieldValue2[0], fieldValue2[1]);

            formService.save(formData);
        }

        formService.markAsExported(getAllForms());

        // Assert
        long count = formService.getExportedFormCount("testform_");
        assertEquals(0, count);
    }

    @Test
    public void testGetExportedFormCount_WithoutFormNameArgument()
    {
        String[] fieldValue1 = new String[] { "field1", "value1" };
        String[] fieldValue2 = new String[] { "field2", "value2" };

        for (int i=0; i<10; i++)
        {
            IPSFormData formData =
                    generateFormData("testform" + i,
                            fieldValue1[0], fieldValue1[1],
                            fieldValue2[0], fieldValue2[1]);

            formService.save(formData);
        }

        formService.markAsExported(getAllForms().subList(0, 4));

        // Assert
        long count = formService.getExportedFormCount(null);
        assertEquals( 4, count);

        count = formService.getExportedFormCount(StringUtils.EMPTY);
        assertEquals(4, count);
    }

    @Test
    public void testGetTotalFormCount_WithFormNameArgument()
    {
        String[] fieldValue1 = new String[] { "field1", "value1" };
        String[] fieldValue2 = new String[] { "field2", "value2" };

        for (int i=0; i<10; i++)
        {
            IPSFormData formData =
                    generateFormData("testform" + (i == 3 ? 2 : i),
                            fieldValue1[0], fieldValue1[1],
                            fieldValue2[0], fieldValue2[1]);

            formService.save(formData);
        }

        String testFormName = "testform2";

        // Assert
        long count = formService.getTotalFormCount(testFormName);
        assertEquals( 2, count);

        count = formService.getTotalFormCount(testFormName.toUpperCase());
        assertEquals(2, count);
    }

    @Test
    public void testGetTotalFormCount_FormNameWithSpecialCharacters()
    {
        String[] fieldValue1 = new String[] { "field1", "value1" };
        String[] fieldValue2 = new String[] { "field2", "value2" };

        for (int i=0; i<10; i++)
        {
            IPSFormData formData =
                    generateFormData("testform" + i,
                            fieldValue1[0], fieldValue1[1],
                            fieldValue2[0], fieldValue2[1]);

            formService.save(formData);
        }

        // Assert
        long count = formService.getTotalFormCount("testform_");
        assertEquals(0, count);
    }

    @AfterEach
    // @Transactional
    public void tearDown()
    {
        List<IPSFormData> forms = formService.findAllForms();
        for(IPSFormData formData:forms){
            formService.delete(formData);
        }


    }

    @Test
    public void testGetTotalFormCount_WithoutFormNameArgument()
    {
        String[] fieldValue1 = new String[] { "field1", "value1" };
        String[] fieldValue2 = new String[] { "field2", "value2" };

        for (int i=0; i<10; i++)
        {
            IPSFormData formData =
                    generateFormData("testform" + i,
                            fieldValue1[0], fieldValue1[1],
                            fieldValue2[0], fieldValue2[1]);

            formService.save(formData);
        }

        // Assert
        long count = formService.getTotalFormCount(null);
        assertEquals(10, count);

        count = formService.getTotalFormCount(StringUtils.EMPTY);
        assertEquals(10, count);
    }

    @Test
    public void testDeleteExportedForms_FormDoesNotExist()
    {
        formService.deleteExportedForms("testform1");
    }

    @Test
    public void testDeleteExportedForms_FormExists_ButIsNotExported()
    {
        String[] fieldValue1 = new String[] { "field1", "value1" };
        String[] fieldValue2 = new String[] { "field2", "value2" };

        for (int i=0; i<10; i++)
        {
            IPSFormData formData =
                    generateFormData("testform" + i,
                            fieldValue1[0], fieldValue1[1],
                            fieldValue2[0], fieldValue2[1]);

            formService.save(formData);
        }

        formService.deleteExportedForms("testform1");

        // Assert
        List<IPSFormData> forms = formService.findAllForms();
        assertEquals( 10, forms.size());
    }

    @Test
    public void testDeleteExportedForms_ValidFormName()
    {
        String[] fieldValue1 = new String[] { "field1", "value1" };
        String[] fieldValue2 = new String[] { "field2", "value2" };

        for (int i=0; i<10; i++)
        {
            IPSFormData formData =
                    generateFormData("testform" + i,
                            fieldValue1[0], fieldValue1[1],
                            fieldValue2[0], fieldValue2[1]);

            formService.save(formData);
        }

        formService.markAsExported(getAllForms());

        String deletedForm = "testform2";
        formService.deleteExportedForms(deletedForm);

        // Assert
        List<IPSFormData> forms = formService.findAllForms();
        assertEquals(9, forms.size());
        for (IPSFormData f : forms)
        {
            assertFalse( f.getName().equals(deletedForm));
        }
    }

    @Test
    public void testDeleteExportedForms_DiffersFormNameOnlyInCasing()
    {
        String[] fieldValue1 = new String[] { "field1", "value1" };
        String[] fieldValue2 = new String[] { "field2", "value2" };

        for (int i=0; i<10; i++)
        {
            IPSFormData formData =
                    generateFormData("testform" + i,
                            fieldValue1[0], fieldValue1[1],
                            fieldValue2[0], fieldValue2[1]);

            formService.save(formData);
        }

        formService.markAsExported(getAllForms());

        String deletedForm = "testform2";
        formService.deleteExportedForms(deletedForm.toUpperCase());

        // Assert
        List<IPSFormData> forms = formService.findAllForms();
        assertEquals( 9, forms.size());
        for (IPSFormData f : forms)
        {
            assertFalse(f.getName().equals(deletedForm));
        }
    }

    @Test
    public void testDeleteExportedForms_FormNameWithSpecialCharacters()
    {
        String[] fieldValue1 = new String[] { "field1", "value1" };
        String[] fieldValue2 = new String[] { "field2", "value2" };

        for (int i=0; i<10; i++)
        {
            IPSFormData formData =
                    generateFormData("testform" + i,
                            fieldValue1[0], fieldValue1[1],
                            fieldValue2[0], fieldValue2[1]);

            formService.save(formData);
        }

        formService.markAsExported(getAllForms());

        String deletedForm = "testform_";
        formService.deleteExportedForms(deletedForm);

        // Assert
        List<IPSFormData> forms = formService.findAllForms();
        assertEquals(10, forms.size());
    }

    @Test
    public void testFindFormsByName_WithFormNameArgument_CheckOrder() throws Exception
    {
        String[] fieldValue1 = new String[] { "field1", "value1" };
        String[] fieldValue2 = new String[] { "field2", "value2" };

        for (int i=0; i<3; i++)
        {
            IPSFormData formData =
                    generateFormData("testform",
                            fieldValue1[0], fieldValue1[1],
                            fieldValue2[0], fieldValue2[1]);

            formService.save(formData);

            Thread.sleep(2000);
        }

        String testFormName = "testform";

        // Assert
        List<IPSFormData> forms = formService.findFormsByName(testFormName);
        checkFindFormsByNameResult(fieldValue1, fieldValue2, testFormName, forms);

        // Different case
        forms = formService.findFormsByName(testFormName.toUpperCase());
        checkFindFormsByNameResult(fieldValue1, fieldValue2, testFormName, forms);
    }

    private void checkFindFormsByNameResult(String[] fieldValue1, String[] fieldValue2,
                                            String testFormName, List<IPSFormData> forms)
    {
        assertNotNull( forms);
        assertEquals(3, forms.size());

        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(Calendar.YEAR, cal.getActualMinimum(Calendar.YEAR));

        Calendar previousDate = cal;
        Calendar currentDate = Calendar.getInstance();

        for (IPSFormData aForm : forms)
        {
            assertEquals( testFormName, aForm.getName());
            assertEquals( 2, aForm.getFields().size());
            assertTrue( aForm.getFields().containsKey(fieldValue1[0]));
            String filedValue1 = aForm.getFields().get(fieldValue1[0]);
            assertEquals( fieldValue1[1], filedValue1.replace(FIELD_VALUES_SEPARATOR,"") );
            assertTrue(aForm.getFields().containsKey(fieldValue1[0]));
            String filedValue2 = aForm.getFields().get(fieldValue2[0]);
            assertEquals( fieldValue2[1], filedValue2.replace(FIELD_VALUES_SEPARATOR,""));

            // Make sure the comments are ascending sorted
            currentDate.setTime(aForm.getCreated());
            assertTrue( previousDate.compareTo(currentDate) < 0);
            previousDate.setTime(aForm.getCreated());
        }
    }

    @Test
    public void testFindFormsByName_FormNameWithSpecialCharacters()
    {
        String[] fieldValue1 = new String[] { "field1", "value1" };
        String[] fieldValue2 = new String[] { "field2", "value2" };

        for (int i=0; i<10; i++)
        {
            IPSFormData formData =
                    generateFormData("testform" + i,
                            fieldValue1[0], fieldValue1[1],
                            fieldValue2[0], fieldValue2[1]);

            formService.save(formData);
        }

        // Assert
        String testFormName = "testform_";

        List<IPSFormData> forms = formService.findFormsByName(testFormName);
        assertNotNull( forms);
        assertEquals(0, forms.size());
    }

    @Test
    public void testFindFormsByName_WithoutFormNameArgument()
    {
        String[] fieldValue1 = new String[] { "field1", "value1" };
        String[] fieldValue2 = new String[] { "field2", "value2" };

        for (int i=0; i<10; i++)
        {
            IPSFormData formData =
                    generateFormData("testform" + i,
                            fieldValue1[0], fieldValue1[1],
                            fieldValue2[0], fieldValue2[1]);

            formService.save(formData);
        }

        // Assert
        try
        {
            formService.findFormsByName(null);
            fail("exception not thrown");
        }
        catch (IllegalArgumentException e)
        {

        }
        List<IPSFormData> forms = formService.findFormsByName(StringUtils.EMPTY);
        assertNotNull(forms);
        assertEquals(0, forms.size());
    }

    @Test
    public void testFindAllForms()
    {
        String[] fieldValue1 = new String[] { "field1", "value1" };
        String[] fieldValue2 = new String[] { "field2", "value2" };

        for (int i=0; i<10; i++)
        {
            IPSFormData formData =
                    generateFormData("testform" + i,
                            fieldValue1[0], fieldValue1[1],
                            fieldValue2[0], fieldValue2[1]);

            formService.save(formData);
        }

        // Assert
        List<IPSFormData> forms = formService.findAllForms();
        assertNotNull(forms);
        assertEquals(10, forms.size());
    }

    @Test
    public void testFindDistinctFormNames()
    {
        String[] fieldValue1 = new String[] { "field1", "value1" };
        String[] fieldValue2 = new String[] { "field2", "value2" };
        String[] formNames = new String[] {
                "testform",
                "TESTFORM",
                "testForm",
                "anotherForm",
                "lastForm"
        };

        for (int i=0; i<10; i++)
        {

            IPSFormData formData =
                    generateFormData(formNames[i % 5],
                            fieldValue1[0], fieldValue1[1],
                            fieldValue2[0], fieldValue2[1]);

            formService.save(formData);
        }

        // Assert
        List<String> distinctFormNames = formService.findDistinctFormNames();
        assertNotNull( distinctFormNames);
        assertEquals(3, distinctFormNames.size());
    }

    @Test
    @Disabled("Test fails for no good reason - bad mock")
    public void testSendFormEmail() throws Exception
    {
        String[] fieldValue1 = new String[] { "field1", "value1" };
        String[] fieldValue2 = new String[] { "field2", "value2" };

        IPSFormData formData = generateFormData("testform", fieldValue2[0], fieldValue2[1], fieldValue1[0], fieldValue1[1]);

        String toList = "test1@percussion1.com,test2@percussion2.com, test3@percussion3.com";
        String subject = "testFormEmailData";

        formService.emailFormData(toList, subject, formData);
        validateEmailSent(fieldValue1, fieldValue2, toList, subject);
    }

    @Test
    public void testValidateFormName() throws Exception
    {

        String valid1="abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ-_";
        String invalid1="quote'";
        String invalid2="test<1";
        String invalid3="test>1";

        assertTrue(formService.isValidFormName(valid1));
        assertFalse(formService.isValidFormName(invalid1));
        assertFalse(formService.isValidFormName(invalid2));
        assertFalse(formService.isValidFormName(invalid3));
    }

}
