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

package com.percussion.install;

// REFACTORED: CP-JAVA11
public class Cm1InstallUtilHarness
{

   /**
    * @param args
    */
   public static void main(String[] args)
   {
      String cm1Dir = args[0];
      boolean isDerbyUp = InstallUtil.isDerbyRunning(cm1Dir);
      System.out.print("Derby Running: " + isDerbyUp + "\n");
      boolean isCM1Up = InstallUtil.isServerRunning(cm1Dir);
      System.out.print("CM1 Running: " + isCM1Up + "\n");
      if (args.length > 1 && args[1] != null) {
         String dtsDir = args[1];
         boolean isDtsUp = InstallUtil.checkTomcatServerRunning(dtsDir);
         System.out.print("DTS Running: " + isDtsUp);
      }
   }
}
