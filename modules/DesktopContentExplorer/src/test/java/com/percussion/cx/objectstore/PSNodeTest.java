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
package com.percussion.cx.objectstore;

import com.percussion.util.PSEntrySet;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test case for the {@link PSNode} class.
 */
public class PSNodeTest
{
   /**
    * Test the clone method to ensure child collections are properly supported.
    * 
    * @throws Exception if there are any errors 
    */
   @Test
   public void testClone() throws Exception
   {
      PSNode node1 = new PSNode("test1", "test 1", PSNode.TYPE_FOLDER, "url", 
         "iconKey", true, 1);
      
      PSNode node2 = new PSNode("test1", "test 1", PSNode.TYPE_FOLDER, "url", 
         "iconKey", true, 1);
