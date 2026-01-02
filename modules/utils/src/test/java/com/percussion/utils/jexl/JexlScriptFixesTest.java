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

package com.percussion.utils.jexl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class JexlScriptFixesTest {

  @Test
  public void fixScript() {

    String testScript = "sdfgsdfg foreach($item in list ) sdfgsdfg";
    String result = JexlScriptFixes.fixScript(testScript, "Unit Test", "fixScript");
<<<<<<< HEAD
    Assertions.assertEquals("sdfgsdfg for($item : list) sdfgsdfg", result);
=======
    Assert.assertEquals("sdfgsdfg for($item : list) sdfgsdfg", result);
>>>>>>> development-8.1.x
    System.out.println(testScript + " ----> " + result);

    testScript = "if ( !$test )";
    result = JexlScriptFixes.fixScript(testScript, "Unit Test", "fixScript");
    System.out.println(testScript + " ----> " + result);
<<<<<<< HEAD
    Assertions.assertEquals("if ( ! $test )", result);
=======
    Assert.assertEquals("if ( ! $test )", result);
>>>>>>> development-8.1.x

    testScript = "if ( $ref1=$ref2 )";
    result = JexlScriptFixes.fixScript(testScript, "Unit Test", "fixScript");
    System.out.println(testScript + " ----> " + result);
<<<<<<< HEAD
    Assertions.assertEquals("if ( $ref1 = $ref2 )", result);
=======
    Assert.assertEquals("if ( $ref1 = $ref2 )", result);
>>>>>>> development-8.1.x

    testScript = "$params=$rx.string.stringToMap(null);";
    result = JexlScriptFixes.fixScript(testScript, "Unit Test", "fixScript");
    System.out.println(testScript + " ----> " + result);
<<<<<<< HEAD
    Assertions.assertEquals("$params = $rx.string.stringToMap(null);", result);
=======
    Assert.assertEquals("$params = $rx.string.stringToMap(null);", result);
>>>>>>> development-8.1.x

    testScript =
        "sdfgsdfg foreach($item in list ) sdfgsdfg sdfgsdfg foreach($item in list ) sdfgsdfg";
    result = JexlScriptFixes.fixScript(testScript, "Unit Test", "fixScript");
    System.out.println(testScript + " ----> " + result);
<<<<<<< HEAD
    Assertions.assertEquals(
=======
    Assert.assertEquals(
>>>>>>> development-8.1.x
        "sdfgsdfg for($item : list) sdfgsdfg sdfgsdfg for($item : list) sdfgsdfg", result);
  }
}
