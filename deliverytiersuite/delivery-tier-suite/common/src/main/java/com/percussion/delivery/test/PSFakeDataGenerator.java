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
package com.percussion.delivery.test;

import com.percussion.error.PSExceptionUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Parses test data from data files or generates in-memory lists of fake data for unit tests.
 * Sunny Sal says: "Fake it till you make it (in tests)!"
 *
 * // REFACTORED: CP-JAVA11
 */
public class PSFakeDataGenerator {

    private static final Logger log = LogManager.getLogger(PSFakeDataGenerator.class);
    public static final int NUMBER = 0;
    public static final int GENDER = 1;
    public static final int GIVEN_NAME = 2;
    public static final int MIDDLE_INITIAL = 3;
    public static final int SURNAME = 4;
    public static final int STREET_ADDRESS = 5;
    public static final int CITY = 6;
    public static final int STATE = 7;
    public static final int ZIP_CODE = 8;
    public static final int COUNTRY = 9;
    public static final int EMAIL_ADDRESS = 10;
    public static final int USERNAME = 11;
    public static final int PASSWORD = 12;
    public static final int TELEPHONE_NUMBER = 13;
    public static final int MOTHERS_MAIDEN = 14;
    public static final int BIRTHDAY = 15;
    public static final int CC_TYPE = 16;
    public static final int CC_NUMBER = 17;
    public static final int CVV2 = 18;
    public static final int CC_EXPIRES = 19;
    public static final int NATIONAL_ID = 20;
    public static final int UPS = 21;
    public static final int OCCUPATION = 22;
    public static final int COMPANY = 23;
    public static final int VEHICLE = 24;
    public static final int DOMAIN = 25;
    public static final int BLOOD_TYPE = 26;
    public static final int POUNDS = 27;
    public static final int KILOGRAMS = 28;
    public static final int FEET_INCHES = 29;
    public static final int CENTIMETERS = 30;
    public static final int GUID = 31;
    public static final int LATITUDE = 32;
    public static final int LONGITUDE = 33;

    /**
     * Returns up to {@code count} number of FakeRegistrant objects.
     *
     * @param count The number of registrations to return, 0 for all available data. Be careful as test datasets can be large.
     * @return A list of FakeRegistrants
     */
    public static List<FakeRegistrant> getFakeRegistrations(int count) {
        var ret = new ArrayList<FakeRegistrant>();
        int lineNumber = 0;

        try (var br = new BufferedReader(
                new InputStreamReader(PSFakeDataGenerator.class.getResourceAsStream("/FakeData.csv")))) {
            String line;
            while ((line = br.readLine()) != null) {
                // Break out if we have enough lines; if count is 0, get all
                if (count > 0 && lineNumber >= count + 1) {
                    break;
                }
                lineNumber++;

                // Skip line 1 - it has field names.
                if (lineNumber > 1) {
                    var st = new StringTokenizer(line, ",");
                    var data = new FakeRegistrant();
                    int tokenNumber = 0;

                    while (st.hasMoreTokens()) {
                        var token = st.nextToken();
                        switch (tokenNumber) {
                            case NUMBER -> data.setNumber(Integer.parseInt(token));
                            case GENDER -> data.setGender(token);
                            case GIVEN_NAME -> data.setGivenName(token);
                            case MIDDLE_INITIAL -> data.setMiddleInitial(token);
                            case SURNAME -> data.setSurname(token);
                            case STREET_ADDRESS -> data.setStreetAddress(token);
                            case CITY -> data.setCity(token);
                            case STATE -> data.setState(token);
                            case ZIP_CODE -> data.setZipCode(token);
                            case COUNTRY -> data.setCountry(token);
                            case EMAIL_ADDRESS -> data.setEmailAddress(token);
                            case USERNAME -> data.setUsername(token);
                            case PASSWORD -> data.setPassword(token);
                            case TELEPHONE_NUMBER -> data.setTelephoneNumber(token);
                            case MOTHERS_MAIDEN -> data.setMothersMaiden(token);
                            case BIRTHDAY -> data.setBirthday(token);
                            case CC_TYPE -> data.setCcType(token);
                            case CC_NUMBER -> data.setCcNumber(token);
                            case CVV2 -> data.setCvv2(token);
                            case CC_EXPIRES -> data.setCcExpires(token);
                            case NATIONAL_ID -> data.setNationalId(token);
                            case UPS -> data.setUps(token);
                            case OCCUPATION -> data.setOccupation(token);
                            case COMPANY -> data.setCompany(token);
                            case VEHICLE -> data.setVehicle(token);
                            case DOMAIN -> data.setDomain(token);
                            case BLOOD_TYPE -> data.setBloodType(token);
                            case POUNDS -> data.setPounds(token);
                            case KILOGRAMS -> data.setKilograms(token);
                            case FEET_INCHES -> data.setFeetInches(token);
                            case CENTIMETERS -> data.setCentimeters(token);
                            case GUID -> data.setGuid(token);
                            case LATITUDE -> data.setLatitude(token);
                            case LONGITUDE -> data.setLongitude(token);
                            default -> { /* ignore unknown tokens */ }
                        }
                        tokenNumber++;
                    }
                    ret.add(data);
                }
            }
        } catch (IOException e) {
            log.error(PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
        }
        return ret;
    }
}
