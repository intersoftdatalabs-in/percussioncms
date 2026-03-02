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
package com.percussion.testing;

import java.io.File;

import com.percussion.utils.xml.PSEntityResolver;

/**
 * The utility class to provide the default connection properties for the
 * remote Rhythmyx Server. This should be used by all Junit tests that are
 * invoked as a remote client.
 */
public class PSClientTestCase extends PSConfigHelperTestCase implements
      IPSClientBasedJunitTest
{
   /**
    * Default constructor.
    */
   public PSClientTestCase()
   {
       // Guard against null system property / environment variable so tests
       // that run outside a full server environment don't NPE during
       // construction. Only set the resolution home when a valid path is
       // present and exists on disk.
       String rxDeploy = System.getProperty("rxdeploydir");
       String rxHome = System.getenv("RHYTHMYX_HOME");
       File homeDir = null;

       if (rxDeploy != null && !rxDeploy.isEmpty()) {
           homeDir = new File(rxDeploy);
       } else if (rxHome != null && !rxHome.isEmpty()) {
           homeDir = new File(rxHome);
       }

       if (homeDir != null && homeDir.exists()) {
           PSEntityResolver.setResolutionHome(homeDir);
       }
   }

   /**
    * Simply call super(String).
    *
    * @param arg0 the name of the TestCase.
    */
   public PSClientTestCase(String arg0)
   {
      super(arg0);
   }
}
