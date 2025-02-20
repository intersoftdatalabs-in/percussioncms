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

package com.percussion.delivery.metadata.rdbms.impl;

import com.percussion.delivery.metadata.IPSCookieConsent;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.io.Serializable;
import java.util.Date;
import java.util.Optional;

/**
 * 
 * @author chriswright
 *
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "PSCookieConsent")
@Table(name = "PERC_COOKIE_CONSENT")
public class PSDbCookieConsent implements IPSCookieConsent, Serializable {

    @Id
    @GeneratedValue
    @Column(name = "CONSENT_ID")
    private long consentId;

    @Basic
    @Column(length = 100,
            name = "IP_ADDRESS")
    private String ip;

    @Basic
    @Column(length = 2000,
            name = "SERVICE_NAME")
    private String serviceName;
    
    @Basic
    @Column(length = 255,
            name = "SITE_NAME")
    private String siteName;
    
    @Basic
    @Column(name = "OPT_IN")
    private boolean optIn;

    @Basic
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "CONSENT_DATE")
    private Date consentDate;
    
    public PSDbCookieConsent() {}
    
    public PSDbCookieConsent(String siteName, String serviceName,
            Date consentDate, String ip, boolean optIn) {
        
        if (siteName == null)
            throw new IllegalArgumentException("siteName may not be null");
        if (serviceName == null)
            throw new IllegalArgumentException("serviceName may not be null");
        if (consentDate == null)
            throw new IllegalArgumentException("consentDate may not be null");
        if (ip == null)
            throw new IllegalArgumentException("ip may not be null");
        
        setSiteName(siteName);
        setService(serviceName);
        setConsentDate(consentDate);
        setIP(ip);
        setOptIn(optIn);
    }

    @Override
    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }

    @Override
    public String getSiteName() {
        return siteName;
    }

    @Override
    public void setIP(String ip) {
        this.ip = ip;
    }

    @Override
    public String getIP() {
        return ip;
    }

    @Override
    public void setConsentDate(Date consentDate) {
        this.consentDate = Optional
                .ofNullable(consentDate)
                .map(Date::getTime)
                .map(Date::new)
                .orElse(null);
    }

    @Override
    public Date getConsentDate() {
        return Optional
                .ofNullable(consentDate)
                .map(Date::getTime)
                .map(Date::new)
                .orElse(null);
    }

    @Override
    public void setService(String serviceName) {
        this.serviceName = serviceName;
    }

    @Override
    public String getService() {
        return serviceName;
    }

    @Override
    public void setOptIn(boolean optIn) {
        this.optIn = optIn;
    }

    @Override
    public boolean getOptIn() {
        return optIn;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (consentId ^ (consentId >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        PSDbCookieConsent other = (PSDbCookieConsent) obj;
        if (consentId != other.consentId)
            return false;
        return true;
    }
    
}
