// REFACTORED: CP-JAVA11
/*
 * Copyright 1999-2023 Percussion Software, Inc.
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package com.percussion.delivery.feeds.services.rdbms;

import com.percussion.delivery.feeds.services.IPSConnectionInfo;
import java.util.Objects;
import java.util.Optional;
import javax.persistence.*;

/**
 * JPA entity for storing secure connection information.
 * Contains sensitive data that must be handled securely.
 * Sunny Sal: "Passwords are like secrets, keep them safe and never in logs!"
 */
@Entity
@Table(name = "PERC_CONNECTION_INFO")
public class PSConnectionInfo implements IPSConnectionInfo {

    @Id
    private final long id = 1L; // Single instance per table design

    @Column(name = "url")
    private String url;

    @Column(name = "username")
    private String username;

    @Column(name = "password")
    private String password;

    @Column(name = "encrypted", nullable = false)
    private boolean encrypted;

    protected PSConnectionInfo() {
        // Required by JPA, do not use directly
    }

    /**
     * Creates connection info with the specified credentials.
     *
     * @param url Service URL (not null)
     * @param username Username for authentication
     * @param password Encrypted password
     * @param encrypted Whether the password is encrypted
     */
    public PSConnectionInfo(String url, String username, String password, boolean encrypted) {
        this.url = Objects.requireNonNull(url, "URL must not be null");
        this.username = username;
        this.password = password;
        this.encrypted = encrypted;
    }

    @Override
    public Optional<String> getUrl() {
        return Optional.ofNullable(url);
    }

    @Override
    public Optional<String> getUsername() {
        return Optional.ofNullable(username);
    }

    @Override
    public Optional<String> getPassword() {
        return Optional.ofNullable(password);
    }

    @Override
    public boolean isEncrypted() {
        return encrypted;
    }

    @Override
    public long getId() {
        return id;
    }

    // These setters are required by JPA but should be used with caution
    protected void setUrl(String url) {
        this.url = url;
    }

    protected void setUsername(String username) {
        this.username = username;
    }

    protected void setPassword(String password) {
        this.password = password;
    }

    protected void setEncrypted(boolean encrypted) {
        this.encrypted = encrypted;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PSConnectionInfo)) return false;
        PSConnectionInfo that = (PSConnectionInfo) o;
        return id == that.id &&
               encrypted == that.encrypted &&
               Objects.equals(url, that.url) &&
               Objects.equals(username, that.username);
        // Intentionally exclude password from equals
    }

    @Override
    public int hashCode() {
        // Exclude password from hash calculation for security
        return Objects.hash(id, url, username, encrypted);
    }

    @Override
    public String toString() {
        return toSafeString(); // Use the safe version from interface
    }
}
