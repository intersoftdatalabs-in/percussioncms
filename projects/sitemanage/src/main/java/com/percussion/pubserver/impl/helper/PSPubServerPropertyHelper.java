package com.percussion.pubserver.impl.helper;

import com.percussion.pubserver.data.PSPublishServerProperty;
import com.percussion.services.pubserver.data.PSPubServerProperty;

import java.util.List;
import java.util.Set;

/**
 * Utility routines for working with publish-server properties.
 * <p>
 * These methods were originally private helpers in {@link
 * com.percussion.pubserver.impl.PSPubServerService} but have been moved out
 * to keep the service class focused and much smaller.
 */
public final class PSPubServerPropertyHelper {

    private PSPubServerPropertyHelper() {
        // static helpers only
    }

    /**
     * Finds the first value for the given key in a set of server properties.
     *
     * @param properties never {@code null}
     * @param key        never {@code null}
     * @return the value or {@code null} if not present
     */
    public static String findProperty(Set<PSPubServerProperty> properties, String key) {
        return properties.stream()
                .filter(property -> property.getName().equalsIgnoreCase(key))
                .map(PSPubServerProperty::getValue)
                .findFirst()
                .orElse(null);
    }

    /**
     * Updates all publish-server properties in the list that match the given key.
     *
     * @param properties never {@code null}
     * @param key        never {@code null}
     * @param value      may be {@code null}
     */
    public static void updateProperty(List<PSPublishServerProperty> properties,
                                      String key,
                                      String value) {
        properties.stream()
                .filter(property -> property.getKey().equalsIgnoreCase(key))
                .forEach(property -> property.setValue(value));
    }
}
