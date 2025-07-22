/*
 * Copyright 1999-2023 Percussion Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package com.percussion.delivery.feeds;

import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.percussion.delivery.exceptions.PSJsonMappingErrorResponse;
import com.percussion.delivery.exceptions.PSUncaughtError;
import com.percussion.delivery.feeds.services.PSFeedService;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.glassfish.jersey.server.spring.AutowiredInjectResolver;
import org.glassfish.jersey.server.spring.SpringComponentProvider;
import org.glassfish.jersey.server.spring.SpringLifecycleListener;
import org.glassfish.jersey.server.spring.SpringWebApplicationInitializer;
import org.glassfish.jersey.server.spring.scope.RequestContextFilter;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Jersey application configuration for the Feeds service.
 */
public class PSFeedsApplication extends ResourceConfig {

    public PSFeedsApplication() {
        // Register Jersey components using method chaining
        registerJerseyComponents()
            .registerSpringComponents()
            .registerExceptionMappers()
            .registerFeatures();
    }

    private PSFeedsApplication registerJerseyComponents() {
        register(JacksonJaxbJsonProvider.class);
        register(PSFeedService.class);
        return this;
    }

    private PSFeedsApplication registerSpringComponents() {
        register(SpringLifecycleListener.class);
        register(SpringWebApplicationInitializer.class);
        register(SpringComponentProvider.class);
        register(AutowiredInjectResolver.class);
        register(RequestContextFilter.class);
        return this;
    }

    private PSFeedsApplication registerExceptionMappers() {
        register(PSJsonMappingErrorResponse.class);
        register(PSUncaughtError.class);
        return this;
    }

    private PSFeedsApplication registerFeatures() {
        register(RolesAllowedDynamicFeature.class);

        // Configure logging with modern fluent builder pattern
        var loggingFeature = new LoggingFeature(
            Logger.getLogger(LoggingFeature.DEFAULT_LOGGER_NAME),
            Level.INFO,
            LoggingFeature.Verbosity.PAYLOAD_ANY,
            LoggingFeature.DEFAULT_MAX_ENTITY_SIZE
        );
        register(loggingFeature);

        return this;
    }
}
