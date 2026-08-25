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
package com.percussion.services.guidmgr;

import com.percussion.services.catalog.IPSCatalogItem;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSDesignGuid;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.utils.guid.IPSGuid;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Utility class for various GUID operations. This class provides convenient
 * static methods for GUID creation, conversion, and manipulation using modern
 * Java 11 features for enhanced performance and type safety.
 * <p>
 * All methods in this class are thread-safe and use efficient stream processing
 * where applicable.
 */
public final class PSGuidUtils {

    // Private constructor to prevent instantiation
    private PSGuidUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Convenience method that loads the GUID manager and calls its method with
     * the same signature.
     *
     * @param id the numeric ID to convert to a GUID
     * @param type the GUID type, not {@code null}
     * @return a new GUID, never {@code null}
     * @throws IllegalArgumentException if type is null
     * @see IPSGuidManager#makeGuid(long, PSTypeEnum)
     */
    public static IPSGuid makeGuid(long id, PSTypeEnum type) {
        Objects.requireNonNull(type, "type cannot be null");
        var mgr = PSGuidManagerLocator.getGuidMgr();
        return mgr.makeGuid(id, type);
    }

    /**
     * Convenience method that loads the GUID manager and calls its method with
     * the same signature.
     *
     * @param id the string ID to convert to a GUID, not {@code null}
     * @param type the GUID type, not {@code null}
     * @return a new GUID, never {@code null}
     * @throws IllegalArgumentException if id or type is null
     * @see IPSGuidManager#makeGuid(String, PSTypeEnum)
     */
    public static IPSGuid makeGuid(String id, PSTypeEnum type) {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(type, "type cannot be null");
        var mgr = PSGuidManagerLocator.getGuidMgr();
        return mgr.makeGuid(id, type);
    }

    /**
     * Safely create a GUID, returning an Optional if creation fails.
     *
     * @param id the numeric ID to convert to a GUID
     * @param type the GUID type, may be {@code null}
     * @return an Optional containing the GUID if creation is successful, empty otherwise
     */
    public static Optional<IPSGuid> makeGuidSafe(long id, PSTypeEnum type) {
        try {
            return type != null ? Optional.of(makeGuid(id, type)) : Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Safely create a GUID, returning an Optional if creation fails.
     *
     * @param id the string ID to convert to a GUID, may be {@code null}
     * @param type the GUID type, may be {@code null}
     * @return an Optional containing the GUID if creation is successful, empty otherwise
     */
    public static Optional<IPSGuid> makeGuidSafe(String id, PSTypeEnum type) {
        try {
            return (id != null && type != null) ? Optional.of(makeGuid(id, type)) : Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Get a list of GUIDs from the supplied summaries in the same order using Stream API.
     *
     * @param summaries the summaries for which to create a list of GUIDs,
     *                  not {@code null}, may be empty
     * @return a list of GUIDs in the same order as the supplied summaries,
     *         never {@code null}, may be empty
     * @throws IllegalArgumentException if summaries is null or contains non-IPSCatalogSummary elements
     */

    public static List<IPSGuid> getIds(List<?> summaries) {
        Objects.requireNonNull(summaries, "summaries cannot be null");

        return summaries.stream()
            .peek(summary -> {
                if (!(summary instanceof IPSCatalogSummary)) {
                    throw new IllegalArgumentException(
                        "all summary elements must be of type IPSCatalogSummary");
                }
            })
            .map(summary -> ((IPSCatalogSummary) summary).getGUID())
            .toList();
    }

    /**
     * Get a list of GUIDs from the supplied summaries using a more type-safe approach.
     *
     * @param summaries the summaries for which to create a list of GUIDs,
     *                  not {@code null}, may be empty
     * @return a list of GUIDs in the same order as the supplied summaries,
     *         never {@code null}, may be empty
     * @throws IllegalArgumentException if summaries is null
     */
    public static List<IPSGuid> getGuidsFromSummaries(Collection<? extends IPSCatalogSummary> summaries) {
        Objects.requireNonNull(summaries, "summaries cannot be null");

        return summaries.stream()
            .map(IPSCatalogSummary::getGUID)
            .toList();
    }

    /**
     * Transform the supplied GUID array into a Long array using Stream API.
     *
     * @param ids the GUIDs to be transformed, not {@code null}, may be empty
     * @return an array of Long values for the supplied GUIDs in the same order,
     *         never {@code null}, may be empty
     * @throws IllegalArgumentException if ids is null
     */
    public static Long[] toLongArray(IPSGuid[] ids) {
        Objects.requireNonNull(ids, "ids cannot be null");

        return Arrays.stream(ids)
            .map(IPSGuid::longValue)
            .toArray(Long[]::new);
    }

    /**
     * Transform the supplied GUID collection into a Long array using Stream API.
     *
     * @param ids the GUIDs to be transformed, not {@code null}, may be empty
     * @return an array of Long values for the supplied GUIDs,
     *         never {@code null}, may be empty
     * @throws IllegalArgumentException if ids is null
     */
    public static Long[] toLongArray(Collection<IPSGuid> ids) {
        Objects.requireNonNull(ids, "ids cannot be null");

        return ids.stream()
            .map(IPSGuid::longValue)
            .toArray(Long[]::new);
    }

    /**
     * Transform the supplied GUID array into an int array for legacy compatibility.
     *
     * @param ids the GUIDs to be transformed, not {@code null}, may be empty
     * @return an array of int values for the supplied GUIDs in the same order,
     *         never {@code null}, may be empty
     * @throws IllegalArgumentException if ids is null
     */
    public static int[] toIntArray(IPSGuid[] ids) {
        Objects.requireNonNull(ids, "ids cannot be null");

        return Arrays.stream(ids)
            .mapToInt(guid -> (int) guid.longValue())
            .toArray();
    }

    /**
     * Create a stream of GUIDs from the supplied array for efficient processing.
     *
     * @param ids the GUIDs to stream, not {@code null}
     * @return a stream of GUIDs, never {@code null}
     * @throws IllegalArgumentException if ids is null
     */
    public static Stream<IPSGuid> streamGuids(IPSGuid[] ids) {
        Objects.requireNonNull(ids, "ids cannot be null");
        return Arrays.stream(ids);
    }

    /**
     * Filter GUIDs by type using Stream API.
     *
     * @param ids the GUIDs to filter, not {@code null}
     * @param type the type to filter by, not {@code null}
     * @return a list of GUIDs matching the specified type, never {@code null}
     * @throws IllegalArgumentException if ids or type is null
     */
    public static List<IPSGuid> filterByType(Collection<IPSGuid> ids, PSTypeEnum type) {
        Objects.requireNonNull(ids, "ids cannot be null");
        Objects.requireNonNull(type, "type cannot be null");

        return ids.stream()
            .filter(guid -> type.equals(guid.getType()))
            .toList();
    }

    /**
     * Check if all GUIDs in the collection are of the same type.
     *
     * @param ids the GUIDs to check, not {@code null}
     * @return {@code true} if all GUIDs are of the same type or collection is empty,
     *         {@code false} otherwise
     * @throws IllegalArgumentException if ids is null
     */
    public static boolean areAllSameType(Collection<IPSGuid> ids) {
        Objects.requireNonNull(ids, "ids cannot be null");

        return ids.stream()
            .map(IPSGuid::getType)
            .distinct()
            .count() <= 1;
    }

    /**
     * Check if a GUID is valid (non-null with valid UUID and type).
     *
     * @param guid the GUID to validate, may be {@code null}
     * @return {@code true} if the GUID is valid, {@code false} otherwise
     */
    public static boolean isValidGuid(IPSGuid guid) {
        return guid != null && guid.getUUID() > 0 && guid.getType() != 0;
    }

    /**
     * Convenience to check if a list of GUIDs is blank (null or empty).
     *
     * @param ids the ids to check, may be {@code null}
     * @return {@code true} if {@code null} or empty, {@code false} otherwise
     */
    public static boolean isBlank(java.util.List<IPSGuid> ids) {
        return ids == null || ids.isEmpty();
    }

    /**
     * Packed 64-bit GUID value including type (and host when present).
     *
     * <p>{@link IPSGuid#longValue()} returns only the UUID when host is 0. Design
     * object locks persist {@link PSDesignGuid#getValue()} (type bits included), so
     * lock queries must use this packed form or they miss the row. Example: NODEDEF
     * typeId 1001 packs to {@code 8589935593}, not {@code 1001}.
     *
     * @param id guid, not {@code null}
     * @return packed long matching {@code PSObjectLock} objectId / lock id columns
     */
    public static long toFullLong(IPSGuid id) {
        Objects.requireNonNull(id, "id cannot be null");
        try {
            if (id instanceof PSDesignGuid designGuid) {
                return designGuid.getValue();
            }
            if (id instanceof PSGuid) {
                return new PSDesignGuid(id).getValue();
            }
            var type = PSTypeEnum.valueOf(id.getType());
            if (type == null) {
                return id.longValue();
            }
            return new PSDesignGuid(new PSGuid(id.getHostId(), type, id.getUUID())).getValue();
        } catch (IllegalArgumentException e) {
            // Type missing (0) — packed conversion is not possible
            return id.longValue();
        }
    }

    /**
     * Convert a list of GUIDs to packed long values for use in queries.
     *
     * <p>Uses {@link #toFullLong(IPSGuid)}, not {@link IPSGuid#longValue()}, so
     * host-0 design GUIDs keep their type bits (see object lock {@code objectId}).
     *
     * @param ids the ids to convert, not {@code null}
     * @return a list of packed long values in the same order as the supplied GUIDs
     */
    public static java.util.List<Long> toFullLongList(java.util.List<IPSGuid> ids) {
        java.util.Objects.requireNonNull(ids, "ids cannot be null");
        return ids.stream().map(PSGuidUtils::toFullLong).toList();
    }

    /**
     * Filter out invalid GUIDs from a collection.
     *
     * @param ids the GUIDs to filter, not {@code null}
     * @return a list containing only valid GUIDs, never {@code null}
     * @throws IllegalArgumentException if ids is null
     */
    public static List<IPSGuid> filterValidGuids(Collection<IPSGuid> ids) {
        Objects.requireNonNull(ids, "ids cannot be null");

        return ids.stream()
            .filter(PSGuidUtils::isValidGuid)
            .toList();
    }

    /**
     * Convert a collection of GUIDs to their string representations.
     *
     * @param ids the GUIDs to convert, not {@code null}
     * @return a list of string representations, never {@code null}
     * @throws IllegalArgumentException if ids is null
     */
    public static List<String> toStringList(Collection<IPSGuid> ids) {
        Objects.requireNonNull(ids, "ids cannot be null");

        return ids.stream()
            .map(IPSGuid::toString)
            .toList();
    }

    /**
     * Get GUID statistics for a collection.
     *
     * @param ids the GUIDs to analyze, not {@code null}
     * @return a string containing statistics about the GUIDs
     * @throws IllegalArgumentException if ids is null
     */
    public static String getGuidStatistics(Collection<IPSGuid> ids) {
        Objects.requireNonNull(ids, "ids cannot be null");

        var totalCount = ids.size();
        var validCount = (int) ids.stream().filter(PSGuidUtils::isValidGuid).count();
        var typeCount = ids.stream().map(IPSGuid::getType).distinct().count();

        return String.format(
            "GUID Statistics: %d total, %d valid, %d distinct types",
            totalCount, validCount, typeCount
        );
    }

    /* -----------------------------------------------------------------
     * Backwards-compatible helper overloads (legacy API)
     * These methods provide commonly used conversions that older code
     * expects (for example, toGuidList overloads used throughout the
     * codebase). They are thin adapters to the newer type-safe APIs.
     * ------------------------------------------------------------------*/

    /**
     * Return a singleton list for a single GUID, or an empty list if null.
     */
    public static java.util.List<IPSGuid> toGuidList(IPSGuid guid)
    {
        return guid == null ? java.util.List.of() : java.util.List.of(guid);
    }


    /**
     * Convert a collection of catalog summary objects to their GUIDs.
     */
    public static java.util.List<IPSGuid> toGuidList(java.util.Collection<? extends IPSCatalogSummary> summaries)
    {
        return summaries == null ? java.util.List.of() : summaries.stream()
                .map(IPSCatalogSummary::getGUID)
                .toList();
    }

    /**
     * Convert a list of template slots to their GUIDs.
     */
    public static java.util.List<IPSGuid> toGuidList(java.util.List<? extends com.percussion.services.assembly.IPSTemplateSlot> slots)
    {
        return slots == null ? java.util.List.of() : slots.stream()
                .map(com.percussion.services.assembly.IPSTemplateSlot::getGUID)
                .toList();
    }

    /**
     * Convert a primitive long[] of legacy ids into a list of legacy guids.
     */
    public static java.util.List<IPSGuid> toGuidList(long[] ids)
    {
        if (ids == null || ids.length == 0)
            return java.util.List.of();
        java.util.List<IPSGuid> rval = new java.util.ArrayList<>(ids.length);
        for (long id : ids)
            rval.add(new PSLegacyGuid(id));
        return java.util.List.copyOf(rval);
    }
}


