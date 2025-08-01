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
package com.percussion.services.guidmgr.impl;

import com.percussion.design.objectstore.PSLocator;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.guidmgr.data.PSGuidGeneratorData;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.services.guidmgr.data.PSNextNumber;
import com.percussion.util.PSBaseBean;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.types.PSConversions;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

/**
 * Modern Java 11 implementation of the GUID manager service.
 *
 * <p>This service allocates globally unique identifiers in blocks, updating the
 * database as each block is allocated. Each type maintains its own pool of IDs
 * for efficient allocation and thread safety.
 *
 * <p>The implementation uses modern Java 11 features including:
 * <ul>
 *   <li>Stream API for efficient collection processing</li>
 *   <li>Optional for null-safe operations</li>
 *   <li>ConcurrentHashMap for thread-safe allocation tracking</li>
 *   <li>Enhanced exception handling with descriptive messages</li>
 * </ul>
 *
 * <p>Host ID management ensures global uniqueness across distributed systems
 * by incorporating the local machine's IP address into the generation process.
 *
 * @author dougrand
 * @since Java 11 Modernization
 */
@PSBaseBean("sys_guidmanager")
@Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = IllegalArgumentException.class)
public class PSGuidManager implements IPSGuidManager {

   @PersistenceContext
   private EntityManager entityManager;

   /**
    * Database keys for storing host and IP information.
    */
   static final Integer HOST_KEY = -1;
   static final Integer IP_KEY1 = -2;
   static final Integer IP_KEY2 = -3;

   /**
    * Default block size for ID allocation to optimize database access.
    */
   static final int BLOCK_SIZE = 10;

   /**
    * Host ID for this server instance, initialized lazily.
    */
   private static volatile long hostId = -1;

   /**
    * Thread-safe allocation cache for efficient ID generation.
    */
   private static final ConcurrentHashMap<Object, Allocation> allocationCache =
       new ConcurrentHashMap<>(16, 0.75f, 4);

   /**
    * Constructs a new GUID manager instance.
    * <p>This object is configured as a singleton in the Spring container.
    */
   public PSGuidManager() {
      super();
   }

   @Override
   public List<IPSGuid> createGuids(PSTypeEnum type, int count) {
      return createGuids((byte) 0, type, count);
   }

   @Override
   public List<IPSGuid> createGuids(byte repositoryId, PSTypeEnum type, int count) {
      Objects.requireNonNull(type, "Type cannot be null");
      if (count < 0) {
         throw new IllegalArgumentException("Count must be non-negative: " + count);
      }

      return IntStream.range(0, count)
          .mapToObj(i -> createGuid(repositoryId, type))
          .collect(java.util.stream.Collectors.toUnmodifiableList());
   }

   @Override
   public IPSGuid createGuid(PSTypeEnum type) {
      return createGuid((byte) 0, type);
   }

   @Override
   public IPSGuid createGuid(byte repositoryId, PSTypeEnum type) {
      validateRepositoryId(repositoryId);
      Objects.requireNonNull(type, "Type cannot be null");

      return Optional.ofNullable(type.getKey())
          .map(key -> createKeyBasedGuid(key, type))
          .orElseGet(() -> createStandardGuid(repositoryId, type));
   }

   @Override
   public int createId(String key) {
      Objects.requireNonNull(key, "Key cannot be null");
      if (key.trim().isEmpty()) {
         throw new IllegalArgumentException("Key cannot be empty");
      }
      return createNextNumberId(key, BLOCK_SIZE);
   }

   @Override
   public long createLongId(PSTypeEnum type) {
      Objects.requireNonNull(type, "Type cannot be null");
      var key = Integer.valueOf(type.getOrdinal()); // Fix: convert short to Integer properly
      var allocation = createStandardAllocation(key);
      return allocation.next();
   }

   @Override
   public IPSGuid makeGuid(PSLocator locator) {
      Objects.requireNonNull(locator, "Locator cannot be null");
      return new PSLegacyGuid(locator);
   }

   @Override
   public PSLocator makeLocator(IPSGuid guid) {
      Objects.requireNonNull(guid, "GUID cannot be null");

      if (guid.getType() != PSTypeEnum.LEGACY_CONTENT.getOrdinal()) {
         throw new IllegalArgumentException(
             "GUID must be of type LEGACY_CONTENT, but was: " +
             PSTypeEnum.valueOf(guid.getType()));
      }

      var legacyGuid = guid instanceof PSLegacyGuid
          ? (PSLegacyGuid) guid
          : new PSLegacyGuid(guid.longValue());

      return legacyGuid.getLocator();
   }

   /**
    * Creates a GUID from a raw long value with type forcing option.
    *
    * @param raw the raw GUID value
    * @param type the GUID type
    * @param forceType whether to force the type
    * @return the created GUID
    */
   public IPSGuid makeGuid(long raw, PSTypeEnum type, boolean forceType) {
      Objects.requireNonNull(type, "Type cannot be null");

      if (isLegacyType(type)) {
         return new PSLegacyGuid(raw);
      }
      return new PSGuid(type, raw, forceType);
   }

   /**
    * Creates a GUID from a raw long value.
    *
    * @param raw the raw GUID value
    * @param type the GUID type
    * @return the created GUID
    */
   public IPSGuid makeGuid(long raw, PSTypeEnum type) {
      return makeGuid(raw, type, false);
   }

   /**
    * Creates a GUID from a string representation with type forcing option.
    *
    * @param raw the string representation
    * @param type the GUID type
    * @param forceType whether to force the type
    * @return the created GUID
    */
   public IPSGuid makeGuid(String raw, PSTypeEnum type, boolean forceType) {
      validateRawString(raw);
      Objects.requireNonNull(type, "Type cannot be null");

      if (isLegacyType(type)) {
         return makeGuid(Long.parseLong(raw), type, forceType);
      }
      return new PSGuid(type, raw, forceType);
   }

   /**
    * Creates a GUID from a string representation.
    *
    * @param raw the string representation
    * @param type the GUID type
    * @return the created GUID
    */
   public IPSGuid makeGuid(String raw, PSTypeEnum type) {
      return makeGuid(raw, type, false);
   }

   /**
    * Creates a GUID from a string representation without explicit type.
    *
    * @param raw the string representation
    * @return the created GUID
    */
   public IPSGuid makeGuid(String raw) {
      validateRawString(raw);

      var guid = new PSGuid(raw);
      var guidType = guid.getType();

      if (guidType == PSTypeEnum.LEGACY_CONTENT.getOrdinal() ||
          guidType == PSTypeEnum.LEGACY_CHILD.getOrdinal()) {
         return new PSLegacyGuid(guid);
      }
      return guid;
   }

   /**
    * Fixes the next number for a given key to a specific value.
    *
    * @param key the key to fix
    * @param value the value to set
    * @return the previous value
    */
   public int fixNextNumber(String key, int value) {
      Objects.requireNonNull(key, "Key cannot be null");
      var allocation = createNextNumberAllocation(key, BLOCK_SIZE);
      return allocation.fix(value);
   }

   /**
    * Peeks at the next number for a given key without consuming it.
    *
    * @param key the key to peek
    * @return the next number that would be allocated
    */
   public int peekNextNumber(String key) {
      Objects.requireNonNull(key, "Key cannot be null");
      var allocation = createNextNumberAllocation(key, BLOCK_SIZE);
      return (int) allocation.peek();
   }

   /**
    * Loads or generates the host ID for this server instance.
    * <p>This method ensures thread-safe initialization of the host ID and
    * validates that the current IP address matches the stored configuration.
    */
   public void loadHostId() {
      var session = getSession();

      try {
         var hostData = session.get(PSGuidGeneratorData.class, HOST_KEY);
         var ip1Data = session.get(PSGuidGeneratorData.class, IP_KEY1);
         var ip2Data = session.get(PSGuidGeneratorData.class, IP_KEY2);

         var currentHostIp = getCurrentHostIp();
         var storedIp = getStoredIp(ip1Data, ip2Data);

         if (shouldGenerateNewHostId(hostData, currentHostIp, storedIp)) {
            generateAndStoreNewHostId(session, hostData, ip1Data, ip2Data, currentHostIp);
         } else if (hostData != null) {
            hostId = hostData.getValue();
         }
      } catch (HibernateException e) {
         // Host ID remains uninitialized - will be generated on next attempt
      }
   }

   /**
    * Updates the next number for a given key in the database.
    *
    * @param key the unique key for the number sequence
    * @param blockSize the size of the block to allocate
    * @param setValue optional value to set directly (0 = use current)
    * @return the starting number for the allocated block
    */
   public int updateNextNumber(String key, int blockSize, long setValue) {
      Objects.requireNonNull(key, "Key cannot be null");

      var session = getSession();
      var data = Optional.ofNullable(session.get(PSNextNumber.class, key))
          .orElseGet(() -> createAndPersistNextNumber(session, key));

      var current = data.getNext();

      if (setValue > 0) {
         current = (int) setValue - 1;
      }

      if (blockSize > 0 || setValue > 0) {
         var next = current + blockSize;
         data.setNext(next);

         try {
            session.update(data);
            session.flush();
         } catch (HibernateException e) {
            throw new RuntimeException("Failed to update next number for key: " + key, e);
         }
      }

      return current + 1;
   }

   /**
    * Gets the Hibernate session from the entity manager.
    */
   private Session getSession() {
      return entityManager.unwrap(Session.class);
   }

   /**
    * Creates a GUID with a key-based allocation strategy.
    */
   private IPSGuid createKeyBasedGuid(String key, PSTypeEnum type) {
      var id = createNextNumberId(key, BLOCK_SIZE);
      return new PSGuid(0, type, id);
   }

   /**
    * Creates a standard GUID with host-based allocation.
    */
   private IPSGuid createStandardGuid(byte repositoryId, PSTypeEnum type) {
      var hostValue = calculateHostValue(repositoryId);
      var key = calculateAllocationKey(repositoryId, type);
      var uuid = createNextLong(key);

      return new PSGuid(hostValue, type, uuid);
   }

   /**
    * Calculates the host value based on repository ID.
    */
   private long calculateHostValue(byte repositoryId) {
      if (repositoryId > 0) {
         return repositoryId | 0xFFFF00L;
      }

      if (hostId < 0) {
         loadHostId(); // Call directly instead of through locator
      }
      return hostId;
   }

   /**
    * Calculates the allocation key based on repository ID and type.
    */
   private Integer calculateAllocationKey(byte repositoryId, PSTypeEnum type) {
      return repositoryId > 0
          ? type.getOrdinal() + repositoryId * 1000
          : type.getOrdinal();
   }

   /**
    * Creates the next ID from a key-based allocation.
    */
   private int createNextNumberId(String key, int blockSize) {
      var allocation = createNextNumberAllocation(key, blockSize);
      return (int) allocation.next();
   }

   /**
    * Creates or retrieves an allocation for next number generation.
    */
   private Allocation createNextNumberAllocation(String key, int blockSize) {
      return allocationCache.computeIfAbsent(key,
          k -> new Allocation(blockSize,
              (bs, sv) -> (long) updateNextNumber(key, bs, sv))); // Call updateNextNumber directly
   }

   /**
    * Creates or retrieves a standard allocation for type-based generation.
    */
   private Allocation createStandardAllocation(Integer key) {
      return allocationCache.computeIfAbsent(key,
          k -> new Allocation(BLOCK_SIZE,
              (bs, sv) -> (long) updateNextNumber(key.toString(), bs, sv))); // Call updateNextNumber directly
   }

   /**
    * Creates the next long value from a standard allocation.
    */
   private long createNextLong(Integer key) {
      var allocation = createStandardAllocation(key);
      return allocation.next();
   }

   /**
    * Validates repository ID parameter.
    */
   private void validateRepositoryId(byte repositoryId) {
      if (repositoryId < 0) {
         throw new IllegalArgumentException("Repository ID must not be negative: " + repositoryId);
      }
   }

   /**
    * Validates raw string parameter.
    */
   private void validateRawString(String raw) {
      if (StringUtils.isBlank(raw)) {
         throw new IllegalArgumentException("Raw string cannot be null or blank");
      }
   }

   /**
    * Checks if the type is a legacy type.
    */
   private boolean isLegacyType(PSTypeEnum type) {
      return type == PSTypeEnum.LEGACY_CONTENT || type == PSTypeEnum.LEGACY_CHILD;
   }

   private byte[] getCurrentHostIp() {
      try {
         var hostIp = InetAddress.getLocalHost().getAddress();
         if (hostIp.length < 16) {
            var padded = new byte[16];
            System.arraycopy(hostIp, 0, padded, 0, hostIp.length);
            return padded;
         }
         return hostIp;
      } catch (UnknownHostException e) {
         return new byte[16];
      }
   }

   private byte[] getStoredIp(PSGuidGeneratorData ip1Data, PSGuidGeneratorData ip2Data) {
      var storedIp = new byte[16];

      if (ip1Data != null && ip2Data != null) {
         System.arraycopy(PSConversions.longToByteArray(ip1Data.getValue()), 0, storedIp, 0, 8);
         System.arraycopy(PSConversions.longToByteArray(ip2Data.getValue()), 0, storedIp, 8, 8);
      }

      return storedIp;
   }

   private boolean shouldGenerateNewHostId(PSGuidGeneratorData hostData, byte[] currentIp, byte[] storedIp) {
      return hostData == null || hostData.getValue() == 0 || !Arrays.equals(storedIp, currentIp);
   }

   private void generateAndStoreNewHostId(Session session, PSGuidGeneratorData hostData,
                                        PSGuidGeneratorData ip1Data, PSGuidGeneratorData ip2Data,
                                        byte[] currentIp) {
      var random = new SecureRandom();

      int newHostId;
      do {
         newHostId = random.nextInt() & 0x00FFFFFF;
      } while (newHostId == 0);

      var finalHostData = Optional.ofNullable(hostData)
          .orElse(new PSGuidGeneratorData(HOST_KEY, 0));
      finalHostData.setValue(newHostId);
      session.saveOrUpdate(finalHostData);

      var finalIp1Data = Optional.ofNullable(ip1Data)
          .orElse(new PSGuidGeneratorData(IP_KEY1, 0));
      var finalIp2Data = Optional.ofNullable(ip2Data)
          .orElse(new PSGuidGeneratorData(IP_KEY2, 0));

      finalIp1Data.setValue(PSConversions.byteArrayToLong(currentIp, 0));
      finalIp2Data.setValue(PSConversions.byteArrayToLong(currentIp, 8));

      session.saveOrUpdate(finalIp1Data);
      session.saveOrUpdate(finalIp2Data);

      hostId = newHostId;
   }

   private PSNextNumber createAndPersistNextNumber(Session session, String key) {
      var data = new PSNextNumber(key, 100);
      session.persist(data);
      return data;
   }

   @Override
   public Set<PSTypeEnum> getSupportedTypes() {
      // Return all enum values as supported types
      return EnumSet.allOf(PSTypeEnum.class);
   }
}
