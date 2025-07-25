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

// REFACTORED: CP-JAVA11
package com.percussion.delivery.integrations.ems.model;

import com.percussion.delivery.integrations.ems.IPSEMSEventService;
import com.percussion.error.PSExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

/**
 * Represents a category entry for EMS integration.
 * <p>Refactored to use Java 11 features and java.time API.</p>
 *
 * @author natechadwick, refactored by Sunny Sal
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Category {
    private static final Logger log = LogManager.getLogger(Category.class);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern(IPSEMSEventService.TIME_FORMAT_STRING.replace("HH:mm:ss", "HH:mm:ss"));

    private Integer categoryID;
    private String categoryDesc;
    private boolean useCutOff;
    private LocalDateTime cutOffTime;
    private Integer cutOffDays;
    private Integer cutOffHours;
    private boolean poNumberRequired;
    private boolean billingReferenceRequired;
    private String currencySymbol;
    private boolean useStates;
    private Integer defaultStateId;

    private static Optional<LocalDateTime> parseDateTime(String value, DateTimeFormatter formatter) {
        try {
            return Optional.of(LocalDateTime.parse(value.replace("T", " ").trim(), formatter));
        } catch (DateTimeParseException e) {
            log.error("Error parsing date/time: {} with format: {}, Error: {}", value, formatter, e.getMessage());
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            return Optional.empty();
        }
    }

    /**
     * Gets the category ID.
     *
     * @return the category ID
     */
    public Integer getCategoryID() {
        return categoryID;
    }

    /**
     * Sets the category ID.
     *
     * @param categoryID the category ID to set
     */
    public void setCategoryID(Integer categoryID) {
        this.categoryID = categoryID;
    }

    /**
     * Gets the category description.
     *
     * @return the category description
     */
    public String getCategoryDesc() {
        return categoryDesc;
    }

    /**
     * Sets the category description.
     *
     * @param categoryDesc the category description to set
     */
    public void setCategoryDesc(String categoryDesc) {
        this.categoryDesc = categoryDesc;
    }

    /**
     * Checks if cut-off is used.
     *
     * @return true if cut-off is used, false otherwise
     */
    public boolean isUseCutOff() {
        return useCutOff;
    }

    /**
     * Sets the cut-off usage.
     *
     * @param useCutOff true to use cut-off, false otherwise
     */
    public void setUseCutOff(boolean useCutOff) {
        this.useCutOff = useCutOff;
    }

    /**
     * Gets the cut-off time.
     *
     * @return an Optional containing the cut-off time, or empty if not set
     */
    public Optional<LocalDateTime> getCutOffTime() {
        return Optional.ofNullable(cutOffTime);
    }

    /**
     * Sets the cut-off time.
     *
     * @param cutOffTime the cut-off time to set
     */
    public void setCutOffTime(String cutOffTime) {
        this.cutOffTime = parseDateTime(cutOffTime, TIME_FORMAT).orElse(null);
    }

    /**
     * Gets the cut-off days.
     *
     * @return the cut-off days
     */
    public Integer getCutOffDays() {
        return cutOffDays;
    }

    /**
     * Sets the cut-off days.
     *
     * @param cutOffDays the cut-off days to set
     */
    public void setCutOffDays(Integer cutOffDays) {
        this.cutOffDays = cutOffDays;
    }

    /**
     * Gets the cut-off hours.
     *
     * @return the cut-off hours
     */
    public Integer getCutOffHours() {
        return cutOffHours;
    }

    /**
     * Sets the cut-off hours.
     *
     * @param cutOffHours the cut-off hours to set
     */
    public void setCutOffHours(Integer cutOffHours) {
        this.cutOffHours = cutOffHours;
    }

    /**
     * Checks if PO number is required.
     *
     * @return true if PO number is required, false otherwise
     */
    public boolean isPoNumberRequired() {
        return poNumberRequired;
    }

    /**
     * Sets the PO number requirement.
     *
     * @param poNumberRequired true if PO number is required, false otherwise
     */
    public void setPoNumberRequired(boolean poNumberRequired) {
        this.poNumberRequired = poNumberRequired;
    }

    /**
     * Checks if billing reference is required.
     *
     * @return true if billing reference is required, false otherwise
     */
    public boolean isBillingReferenceRequired() {
        return billingReferenceRequired;
    }

    /**
     * Sets the billing reference requirement.
     *
     * @param billingReferenceRequired true if billing reference is required, false otherwise
     */
    public void setBillingReferenceRequired(boolean billingReferenceRequired) {
        this.billingReferenceRequired = billingReferenceRequired;
    }

    /**
     * Gets the currency symbol.
     *
     * @return the currency symbol
     */
    public String getCurrencySymbol() {
        return currencySymbol;
    }

    /**
     * Sets the currency symbol.
     *
     * @param currencySymbol the currency symbol to set
     */
    public void setCurrencySymbol(String currencySymbol) {
        this.currencySymbol = currencySymbol;
    }

    /**
     * Checks if states are used.
     *
     * @return true if states are used, false otherwise
     */
    public boolean isUseStates() {
        return useStates;
    }

    /**
     * Sets the states usage.
     *
     * @param useStates true to use states, false otherwise
     */
    public void setUseStates(boolean useStates) {
        this.useStates = useStates;
    }

    /**
     * Gets the default state ID.
     *
     * @return the default state ID
     */
    public Integer getDefaultStateId() {
        return defaultStateId;
    }

    /**
     * Sets the default state ID.
     *
     * @param defaultStateId the default state ID to set
     */
    public void setDefaultStateId(Integer defaultStateId) {
        this.defaultStateId = defaultStateId;
    }
}
