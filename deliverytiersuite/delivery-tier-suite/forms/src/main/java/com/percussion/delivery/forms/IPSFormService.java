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

package com.percussion.delivery.forms;

import com.percussion.delivery.exceptions.PSEmailException;
import com.percussion.delivery.forms.data.IPSFormData;
import com.percussion.delivery.forms.impl.PSRecaptchaService;
import com.percussion.delivery.utils.PSEmailServiceNotInitializedException;
import org.apache.commons.mail.EmailException;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface IPSFormService {
    void save(IPSFormData formdata);
    void delete(IPSFormData form);
    PSRecaptchaService getRecaptchaService();
    void setRecaptchaService(PSRecaptchaService recaptchaService);
    IPSFormData createFormData(String formname, Map<String, String[]> formdata);
    void deleteExported(String formName);
    List<IPSFormData> findAll(String formName);
    List<IPSFormData> findAll(String formName, int limit, int offset);
    IPSFormData findById(String id);
    void sendEmail(IPSFormData form, String to, String from, String subject, String body) throws EmailException, PSEmailServiceNotInitializedException, PSEmailException;
    void exportToCSV(String formName, String filePath);
}
