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
package com.percussion.utils.http;

/**
 * Simple test for the PSModernHttpClient to verify basic functionality
 */
public class PSModernHttpClientTest {
    
    public static void main(String[] args) {
        try {
            // Test basic instantiation
            PSModernHttpClient client = new PSModernHttpClient("https://httpbin.org");
            System.out.println("PSModernHttpClient created successfully");
            
            // Test GET request (commented out to avoid external dependency in build)
            // String response = client.get("/get");
            // System.out.println("GET response: " + response);
            
            System.out.println("PSModernHttpClient test passed!");
            
        } catch (Exception e) {
            System.err.println("PSModernHttpClient test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}