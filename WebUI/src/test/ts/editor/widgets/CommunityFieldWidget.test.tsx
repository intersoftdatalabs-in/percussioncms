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

import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { CommunityFieldWidget } from "../../../../main/ts/editor/widgets/CommunityFieldWidget";

describe("CommunityFieldWidget", () => {
  afterEach(() => {
    cleanup();
  });

  it("lists communities and reports the selected id", async () => {
    const onChange = vi.fn();
    render(
      <CommunityFieldWidget
        name="sys_communityid"
        value=""
        readOnly={false}
        onChange={onChange}
        loadCommunities={async () => [
          { id: 10, name: "Default", label: "Default" },
          { id: 20, name: "Enterprise", label: "Enterprise" },
        ]}
      />,
    );
    await waitFor(() => {
      expect(screen.getByText("Enterprise")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("editor-field-sys_communityid"), {
      target: { value: "20" },
    });
    expect(onChange).toHaveBeenCalledWith("20");
  });
});
