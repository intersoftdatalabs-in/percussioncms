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

import type { ComponentType } from "react";

type Loader = () => Promise<{ default: ComponentType<any> } | ComponentType<any>>;

/**
 * Dynamic import factories — main chunk must not static-import feature shells.
 * Shared by bridge mounts and future SPA lazy routes.
 */
const loaders: Record<string, Loader> = {
  HelloWorld: () =>
    import("./components/HelloWorld").then((m) => ({ default: m.HelloWorld })),
  Dashboard: () =>
    import("./dashboard").then((m) => ({ default: m.Dashboard })),
  WorkflowStatusWidget: () =>
    import("./dashboard").then((m) => ({ default: m.WorkflowStatusWidget })),
  ActivityWidget: () =>
    import("./dashboard").then((m) => ({ default: m.ActivityWidget })),
  ProcessMonitorWidget: () =>
    import("./dashboard").then((m) => ({ default: m.ProcessMonitorWidget })),
  EffectivenessWidget: () =>
    import("./dashboard").then((m) => ({ default: m.EffectivenessWidget })),
  AssetsStatusWidget: () =>
    import("./dashboard").then((m) => ({ default: m.AssetsStatusWidget })),
  BulkUploadWidget: () =>
    import("./dashboard").then((m) => ({ default: m.BulkUploadWidget })),
  ReportsWidget: () =>
    import("./dashboard").then((m) => ({ default: m.ReportsWidget })),
  TrafficWidget: () =>
    import("./dashboard").then((m) => ({ default: m.TrafficWidget })),
  HomeShell: () => import("./home").then((m) => ({ default: m.HomeShell })),
  PublishingShell: () =>
    import("./publishing").then((m) => ({ default: m.PublishingShell })),
  WidgetBuilderApp: () =>
    import("./widgetbuilder/WidgetBuilderApp").then((m) => ({
      default: m.WidgetBuilderApp,
    })),
  UnavailableView: () =>
    import("./home/UnavailableView").then((m) => ({ default: m.UnavailableView })),
  ContentExplorerShell: () =>
    import("./contentExplorer/ContentExplorerShell").then((m) => ({
      default: m.ContentExplorerShell,
    })),
  ContentBrowser: () =>
    import("./contentBrowser/ContentBrowser").then((m) => ({
      default: m.ContentBrowser,
    })),
  ClipboardPanel: () =>
    import("./contentExplorer/clipboard/ClipboardPanel").then((m) => ({
      default: m.ClipboardPanel,
    })),
  SiteCopyWizard: () =>
    import("./contentExplorer/wizards/SiteCopyWizard").then((m) => ({
      default: m.SiteCopyWizard,
    })),
  SubfolderCopyWizard: () =>
    import("./contentExplorer/wizards/SubfolderCopyWizard").then((m) => ({
      default: m.SubfolderCopyWizard,
    })),
  DependencyViewer: () =>
    import("./contentExplorer/views/DependencyViewer").then((m) => ({
      default: m.DependencyViewer,
    })),
  RelationshipsView: () =>
    import("./contentExplorer/views/RelationshipsView").then((m) => ({
      default: m.RelationshipsView,
    })),
  SearchPanel: () =>
    import("./contentExplorer/SearchPanel").then((m) => ({
      default: m.SearchPanel,
    })),
  FolderSecurityPanel: () =>
    import("./contentExplorer/FolderSecurityPanel").then((m) => ({
      default: m.FolderSecurityPanel,
    })),
  ActionToolbar: () =>
    import("./contentExplorer/ActionToolbar").then((m) => ({
      default: m.ActionToolbar,
    })),
  ContextMenu: () =>
    import("./contentExplorer/ContextMenu").then((m) => ({
      default: m.ContextMenu,
    })),
  WorkflowAdminShell: () =>
    import("./workflowAdmin/WorkflowAdminShell").then((m) => ({
      default: m.WorkflowAdminShell,
    })),
  AdminShell: () =>
    import("./admin/AdminShell").then((m) => ({ default: m.AdminShell })),
  DeveloperShell: () =>
    import("./developer").then((m) => ({ default: m.DeveloperShell })),
};

const cache = new Map<string, Promise<ComponentType<any>>>();

/**
 * Lazily load a registered component by name (shared by bridge + SPA).
 *
 * @param name - registry key (e.g. HomeShell)
 * @returns resolved component type
 */
export function loadComponent(name: string): Promise<ComponentType<any>> {
  const existing = cache.get(name);
  if (existing) {
    return existing;
  }
  const loader = loaders[name];
  if (!loader) {
    return Promise.reject(new Error(`Unknown component: ${name}`));
  }
  const promise = loader()
    .then((mod) => {
      if (typeof mod === "function") {
        return mod as ComponentType<any>;
      }
      const def = (mod as { default: ComponentType<any> }).default;
      if (!def) {
        throw new Error(`Component module for "${name}" has no default export`);
      }
      return def;
    })
    .catch((err) => {
      cache.delete(name);
      throw err;
    });
  cache.set(name, promise);
  return promise;
}

/** Whether a name has a loader (does not load the module). */
export function isRegisteredComponent(name: string): boolean {
  return Object.prototype.hasOwnProperty.call(loaders, name);
}

/** Registered component names (for tests / diagnostics). */
export function listRegisteredComponentNames(): string[] {
  return Object.keys(loaders).sort();
}
