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

/**
 * Smoke-test component to validate the React/TypeScript/Vite pipeline.
 *
 * <p>Can be mounted from legacy pages via:</p>
 * <pre>
 *   window.PercModernUI.mount('some-div', 'HelloWorld', { name: 'Sal' });
 * </pre>
 */

import React from "react";

export interface HelloWorldProps {
  name?: string;
}

export const HelloWorld: React.FC<HelloWorldProps> = ({ name = "World" }) => {
  return React.createElement(
    "div",
    {
      style: {
        fontFamily: "'Open Sans', sans-serif",
        padding: "1rem",
        border: "1px solid #007ea8",
        borderRadius: "4px",
        backgroundColor: "#f0f8ff",
        color: "#133c55",
      },
    },
    React.createElement("h3", null, `👋 Hello, ${name}!`),
    React.createElement(
      "p",
      null,
      "This component is rendered by the modern React/TypeScript pipeline.",
    ),
  );
};
