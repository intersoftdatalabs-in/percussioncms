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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import type { PSPathItem } from "../../../main/ts/api/contentExplorer/types";
import {
  defaultReducedActionHandlers,
  ReducedActions,
} from "../../../main/ts/contentExplorer/ReducedActions";
import type { ReducedActionHandlers } from "../../../main/ts/contentExplorer/ReducedActions";
import { mockFetch } from "./setup";
import { renderA11yGate } from "./a11y";

const FOLDER: PSPathItem = {
  id: "f-1",
  path: "/Sites/Foo",
  name: "Foo",
  type: "folder",
  accessLevel: "ADMIN",
};

const READ_ONLY: PSPathItem = {
  id: "r-1",
  path: "/Sites/Foo/RO",
  name: "RO",
  type: "page",
  accessLevel: "READ",
};

function makeHandlers(): {
  handlers: ReducedActionHandlers;
  calls: Record<keyof ReducedActionHandlers, unknown[]>;
} {
  const calls: Record<string, unknown[]> = {
    onOpen: [],
    onPreview: [],
    onCreateFolder: [],
    onRename: [],
    onMove: [],
    onCopy: [],
    onDelete: [],
  };
  const handlers: ReducedActionHandlers = {
    onOpen: (item) => calls.onOpen.push(item),
    onPreview: (item) => calls.onPreview.push(item),
    onCreateFolder: async (parent, name) =>
      calls.onCreateFolder.push({ parent, name }),
    onRename: async (item, newName) => calls.onRename.push({ item, newName }),
    onMove: async (item, targetPath) =>
      calls.onMove.push({ item, targetPath }),
    onCopy: async (item, targetPath) =>
      calls.onCopy.push({ item, targetPath }),
    onDelete: async (item) => calls.onDelete.push(item),
    prompt: () => null,
    confirm: () => false,
  };
  return { handlers, calls: calls as Record<keyof ReducedActionHandlers, unknown[]> };
}

describe("ReducedActions", () => {
  it("disables item-scoped actions when no item is selected", () => {
    const { handlers } = makeHandlers();
    render(
      <ReducedActions
        item={null}
        folder={FOLDER}
        handlers={handlers}
        onError={() => undefined}
      />,
    );
    expect(screen.getByTestId("action-open")).toBeDisabled();
    expect(screen.getByTestId("action-rename")).toBeDisabled();
    expect(screen.getByTestId("action-delete")).toBeDisabled();
    expect(screen.getByTestId("action-create-folder")).toBeEnabled();
  });

  it("disables the Preview button when no preview handler is supplied", () => {
    const { handlers } = makeHandlers();
    render(
      <ReducedActions
        item={FOLDER}
        folder={FOLDER}
        handlers={handlers}
        hasPreviewHandler={false}
        onError={() => undefined}
      />,
    );
    expect(screen.getByTestId("action-preview")).toBeDisabled();
  });

  it("keeps Open enabled for folders so browse does not use the editor (#3638)", () => {
    const { handlers, calls } = makeHandlers();
    render(
      <ReducedActions
        item={FOLDER}
        folder={FOLDER}
        handlers={handlers}
        hasPreviewHandler={true}
        onError={() => undefined}
      />,
    );
    const open = screen.getByTestId("action-open");
    expect(open).toBeEnabled();
    fireEvent.click(open);
    expect(calls.onOpen).toHaveLength(1);
    expect(calls.onOpen[0]).toMatchObject({ type: "folder" });
  });

  it("disables Preview for folders even when a handler is present (#2733)", () => {
    const { handlers } = makeHandlers();
    render(
      <ReducedActions
        item={FOLDER}
        folder={FOLDER}
        handlers={handlers}
        hasPreviewHandler={true}
        onError={() => undefined}
      />,
    );
    expect(screen.getByTestId("action-preview")).toBeDisabled();
  });

  it("enables Preview for listed percPage rows (#3456)", () => {
    const { handlers, calls } = makeHandlers();
    const listed: PSPathItem = {
      id: "16777215-101-9",
      name: "About",
      path: "/Sites/Corporate_Investments/Pages/About",
      type: "percPage",
      leaf: false,
      accessLevel: "READ",
    };
    render(
      <ReducedActions
        item={listed}
        folder={FOLDER}
        handlers={handlers}
        hasPreviewHandler={true}
        onError={() => undefined}
      />,
    );
    const btn = screen.getByTestId("action-preview");
    expect(btn).toBeEnabled();
    fireEvent.click(btn);
    expect(calls.onPreview).toHaveLength(1);
    expect(calls.onPreview[0]).toMatchObject({ type: "percPage" });
  });

  it("enables the Preview button for previewable pages when a handler is supplied (#2733)", () => {
    const { handlers, calls } = makeHandlers();
    render(
      <ReducedActions
        item={READ_ONLY}
        folder={FOLDER}
        handlers={handlers}
        hasPreviewHandler={true}
        onError={() => undefined}
      />,
    );
    const btn = screen.getByTestId("action-preview");
    expect(btn).toBeEnabled();
    fireEvent.click(btn);
    expect(calls.onPreview).toHaveLength(1);
  });

  it("enables delete only on writable folders", () => {
    const { handlers } = makeHandlers();
    const { rerender } = render(
      <ReducedActions
        item={READ_ONLY}
        folder={null}
        handlers={handlers}
        onError={() => undefined}
      />,
    );
    expect(screen.getByTestId("action-delete")).toBeDisabled();
    rerender(
      <ReducedActions
        item={FOLDER}
        folder={null}
        handlers={handlers}
        onError={() => undefined}
      />,
    );
    expect(screen.getByTestId("action-delete")).toBeEnabled();
  });

  it("fires onRename when the user enters a new name via the prompt helper (#3645)", async () => {
    const { handlers, calls } = makeHandlers();
    handlers.prompt = () => "Renamed";
    render(
      <ReducedActions
        item={FOLDER}
        folder={FOLDER}
        handlers={handlers}
        onError={() => undefined}
      />,
    );
    expect(screen.getByTestId("action-rename")).toBeEnabled();
    fireEvent.click(screen.getByTestId("action-rename"));
    await waitFor(() => expect(calls.onRename).toHaveLength(1));
    expect(calls.onRename[0]).toMatchObject({
      item: FOLDER,
      newName: "Renamed",
    });
  });

  it("fires onCreateFolder when the user enters a name via the prompt helper", async () => {
    const { handlers, calls } = makeHandlers();
    handlers.prompt = () => "My Folder";
    render(
      <ReducedActions
        item={null}
        folder={FOLDER}
        handlers={handlers}
        onError={() => undefined}
      />,
    );
    fireEvent.click(screen.getByTestId("action-create-folder"));
    await waitFor(() => expect(calls.onCreateFolder).toHaveLength(1));
    expect(calls.onCreateFolder[0]).toMatchObject({
      parent: FOLDER,
      name: "My Folder",
    });
  });

  it("does not invoke any server call when the user cancels the confirm dialog", async () => {
    const { handlers, calls } = makeHandlers();
    handlers.confirm = () => false;
    mockFetch(async () => new Response("", { status: 200 }));
    render(
      <ReducedActions
        item={FOLDER}
        folder={null}
        handlers={handlers}
        onError={() => undefined}
      />,
    );
    fireEvent.click(screen.getByTestId("action-delete"));
    await waitFor(() => expect(calls.onDelete).toHaveLength(0));
  });

  it("invokes onDelete when the user confirms", async () => {
    const { handlers, calls } = makeHandlers();
    handlers.confirm = () => true;
    mockFetch(async () => new Response("", { status: 200 }));
    render(
      <ReducedActions
        item={FOLDER}
        folder={null}
        handlers={handlers}
        onError={() => undefined}
      />,
    );
    fireEvent.click(screen.getByTestId("action-delete"));
    await waitFor(() => expect(calls.onDelete).toHaveLength(1));
    expect(calls.onDelete[0]).toBe(FOLDER);
  });

  it("surfaces server errors via onError", async () => {
    const { handlers } = makeHandlers();
    handlers.confirm = () => true;
    // Handler must reject for runItemAction to surface onError (mockFetch alone
    // is unused when custom handlers do not call pathApi).
    handlers.onDelete = async () => {
      throw new Error("denied");
    };
    const onError = vi.fn();
    render(
      <ReducedActions
        item={FOLDER}
        folder={null}
        handlers={handlers}
        onError={onError}
      />,
    );
    fireEvent.click(screen.getByTestId("action-delete"));
    await waitFor(() => expect(onError).toHaveBeenCalled());
    expect(String(onError.mock.calls[0]?.[0])).toMatch(/500|denied/);
  });

  it("default onMove POSTs MoveFolderItem wrap to pathmanagement moveItem (#3655)", async () => {
    const handlers = defaultReducedActionHandlers();
    let url = "";
    let posted: unknown;
    mockFetch(async (input, init) => {
      url = typeof input === "string" ? input : (input as Request).url;
      posted = JSON.parse(String((init as RequestInit)?.body ?? "{}"));
      return new Response(JSON.stringify({ NoContent: { operation: "moveItem" } }), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      });
    });
    await handlers.onMove(FOLDER, "/Sites/Dst");
    expect(url).toContain("/pathmanagement/path/moveItem");
    expect(url).not.toContain("/content-explorer/folders");
    expect(posted).toEqual({
      MoveFolderItem: {
        itemPath: "/Sites/Foo",
        targetFolderPath: "/Sites/Dst",
      },
    });
    expect(posted).not.toHaveProperty("sourcePath");
  });

  it("default onCopy POSTs copy/folder for folders and copy/item for assets (#3656)", async () => {
    const handlers = defaultReducedActionHandlers();
    const urls: string[] = [];
    mockFetch(async (input, init) => {
      urls.push(typeof input === "string" ? input : (input as Request).url);
      JSON.parse(String((init as RequestInit)?.body ?? "{}"));
      return new Response(JSON.stringify({ message: "Copied OK" }), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      });
    });
    await handlers.onCopy(FOLDER, "/Assets/Dst");
    expect(urls.at(-1)).toContain("/folders/copy/folder");
    expect(urls.at(-1)).not.toContain("/folders/copy/item");

    const asset: PSPathItem = {
      id: "a-3656",
      path: "/Assets/qa3656_src",
      name: "qa3656_src",
      type: "percSimpleTextAsset",
      category: "ASSET",
      accessLevel: "WRITE",
      leaf: true,
    };
    await handlers.onCopy(asset, "/Assets/Dst");
    expect(urls.at(-1)).toContain("/folders/copy/item");
    expect(urls.at(-1)).not.toContain("/folders/copy/folder");
  });

  it("passes the zero serious/critical axe-core gate (admin item)", () => {
    const handlers = makeHandlers();
    const { container } = render(
      <ReducedActions item={FOLDER} handlers={handlers} />,
    );
    return renderA11yGate(container);
  });

  it("passes the zero serious/critical axe-core gate (read-only item)", () => {
    const handlers = makeHandlers();
    const { container } = render(
      <ReducedActions item={READ_ONLY} handlers={handlers} />,
    );
    return renderA11yGate(container);
  });
});