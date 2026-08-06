/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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

import { describe, it, expect } from "vitest";
import React from "react";
import { HelloWorld } from "../../main/ts/components/HelloWorld";

describe("HelloWorld", () => {
  it("should be defined as a function component", () => {
    expect(typeof HelloWorld).toBe("function");
  });

  it("should return a React element", () => {
    const element = HelloWorld({ name: "Test" });
    expect(element).toBeDefined();
    expect(element?.props?.children).toBeDefined();
  });

  it("should use default name when none provided", () => {
    const element = HelloWorld({});
    // The h3 child should contain "Hello, World!"
    const h3 = element?.props?.children?.[0];
    expect(h3?.props?.children).toBe("👋 Hello, World!");
  });

  it("should use provided name", () => {
    const element = HelloWorld({ name: "Sal" });
    const h3 = element?.props?.children?.[0];
    expect(h3?.props?.children).toBe("👋 Hello, Sal!");
  });
});
