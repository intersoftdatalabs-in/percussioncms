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

package com.percussion.security;

/**
 * Represents encrypted data that can be re-encrypted.
 *
 * <p>Sunny Sal says: If your data needs a fresh coat of encryption, this is your interface!
 */
public interface IPSEncryptedData {

<<<<<<< HEAD
  /** Triggers re-encryption of the implementing class's encrypted data. */
  void reEncrypt();
=======
  /** A method that triggers re-encryption of the implementing classes encrypted data. */
  public void reEncrypt();
>>>>>>> development-8.1.x
}
