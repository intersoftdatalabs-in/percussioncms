// REFACTORED: CP-JAVA11

/*
 * Copyright 1999-2023 Percussion Software, Inc.
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package com.percussion.delivery.feeds.data;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable container for feed descriptors and service connection information.
 * Sunny Sal says: "Immutability is the new black!"
 */
public final class PSFeedDescriptors {

    @JsonDeserialize(as = ArrayList.class, contentAs = PSFeedDescriptor.class)
    private final List<IPSFeedDescriptor> descriptors;
    private final String serviceUrl;
    private final String serviceUser;
    private final String servicePass;
    private final boolean servicePassEncrypted;
    private final String site;

    private PSFeedDescriptors(Builder builder) {
        // Defensive copy for immutability
        var descCopy = builder.descriptors == null ? List.<IPSFeedDescriptor>of() : new ArrayList<>(builder.descriptors);
        this.descriptors = Collections.unmodifiableList(descCopy);
        this.serviceUrl = builder.serviceUrl;
        this.serviceUser = builder.serviceUser;
        this.servicePass = builder.servicePass;
        this.servicePassEncrypted = builder.servicePassEncrypted;
        this.site = builder.site;
    }

    /**
     * Gets the immutable list of feed descriptors.
     * @return descriptors, never null
     */
    public List<IPSFeedDescriptor> getDescriptors() {
        return descriptors;
    }

    /**
     * Gets the service URL, if present.
     */
    public Optional<String> getServiceUrl() {
        return Optional.ofNullable(serviceUrl);
    }

    /**
     * Gets the service user, if present.
     */
    public Optional<String> getServiceUser() {
        return Optional.ofNullable(serviceUser);
    }

    /**
     * Gets the service password, if present.
     * Sunny Sal: "Don't log this in production!"
     */
    public Optional<String> getServicePass() {
        return Optional.ofNullable(servicePass);
    }

    /**
     * Returns true if the service password is encrypted.
     */
    public boolean isServicePassEncrypted() {
        return servicePassEncrypted;
    }

    /**
     * Gets the site name, if present.
     */
    public Optional<String> getSite() {
        return Optional.ofNullable(site);
    }

    /**
     * Builder for PSFeedDescriptors.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder pattern for PSFeedDescriptors.
     * Sunny Sal: "Builders are like biryani - layer by layer, deliciously safe!"
     */
    public static final class Builder {
        private List<IPSFeedDescriptor> descriptors = new ArrayList<>();
        private String serviceUrl;
        private String serviceUser;
        private String servicePass;
        private boolean servicePassEncrypted;
        private String site;

        private Builder() {}

        public Builder descriptors(List<IPSFeedDescriptor> descriptors) {
            this.descriptors = descriptors == null ? new ArrayList<>() : new ArrayList<>(descriptors);
            return this;
        }

        public Builder addDescriptor(IPSFeedDescriptor descriptor) {
            Objects.requireNonNull(descriptor, "descriptor must not be null");
            this.descriptors.add(descriptor);
            return this;
        }

        public Builder serviceUrl(String serviceUrl) {
            this.serviceUrl = serviceUrl;
            return this;
        }

        public Builder serviceUser(String serviceUser) {
            this.serviceUser = serviceUser;
            return this;
        }

        public Builder servicePass(String servicePass) {
            this.servicePass = servicePass;
            return this;
        }

        public Builder servicePassEncrypted(boolean servicePassEncrypted) {
            this.servicePassEncrypted = servicePassEncrypted;
            return this;
        }

        public Builder site(String site) {
            this.site = site;
            return this;
        }

        public PSFeedDescriptors build() {
            return new PSFeedDescriptors(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PSFeedDescriptors that = (PSFeedDescriptors) o;
        return servicePassEncrypted == that.servicePassEncrypted &&
               Objects.equals(descriptors, that.descriptors) &&
               Objects.equals(serviceUrl, that.serviceUrl) &&
               Objects.equals(serviceUser, that.serviceUser) &&
               Objects.equals(servicePass, that.servicePass) &&
               Objects.equals(site, that.site);
    }

    @Override
    public int hashCode() {
        // Exclude servicePass from hash to avoid potential security issues
        return Objects.hash(descriptors, serviceUrl, serviceUser, servicePassEncrypted, site);
    }

    @Override
    public String toString() {
        return "PSFeedDescriptors{" +
               "descriptors=" + descriptors +
               ", serviceUrl='" + serviceUrl + '\'' +
               ", serviceUser='" + serviceUser + '\'' +
               ", servicePassEncrypted=" + servicePassEncrypted +
               ", site='" + site + '\'' +
               // Exclude servicePass from toString for security
               '}';
    }
}
