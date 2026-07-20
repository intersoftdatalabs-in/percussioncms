/*
 * Copyright 1999-2026 Percussion Software, Inc.
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

import type { ComponentType } from "react";
import { HelloWorld } from "./components/HelloWorld";
import { Dashboard, WorkflowStatusWidget, ActivityWidget, ProcessMonitorWidget, EffectivenessWidget, AssetsStatusWidget, BulkUploadWidget, ReportsWidget, TrafficWidget } from "./dashboard";
import { HomeShell } from "./home";
import { UnavailableView } from "./home/UnavailableView";
import { PublishingShell } from "./publishing";
import { WidgetBuilderApp } from "./widgetbuilder/WidgetBuilderApp";
import { ContentExplorerShell } from "./contentExplorer/ContentExplorerShell";
import { ClipboardPanel } from "./contentExplorer/clipboard/ClipboardPanel";
import { SiteCopyWizard } from "./contentExplorer/wizards/SiteCopyWizard";
import { SubfolderCopyWizard } from "./contentExplorer/wizards/SubfolderCopyWizard";
import { DependencyViewer } from "./contentExplorer/views/DependencyViewer";
import { RelationshipsView } from "./contentExplorer/views/RelationshipsView";
import { ContextMenu } from "./contentExplorer/ContextMenu";
import { ActionToolbar } from "./contentExplorer/ActionToolbar";
import { ContentBrowser } from "./contentBrowser/ContentBrowser";
import { WorkflowAdminShell } from "./workflowAdmin/WorkflowAdminShell";

/** Map of component names to their React component types. */
export const componentRegistry = new Map<string, ComponentType<any>>();

// Register components available to the bridge
componentRegistry.set("HelloWorld", HelloWorld);
componentRegistry.set("Dashboard", Dashboard);
componentRegistry.set("WorkflowStatusWidget", WorkflowStatusWidget);
componentRegistry.set("ActivityWidget", ActivityWidget);
componentRegistry.set("ProcessMonitorWidget", ProcessMonitorWidget);
componentRegistry.set("EffectivenessWidget", EffectivenessWidget);
componentRegistry.set("AssetsStatusWidget", AssetsStatusWidget);
componentRegistry.set("BulkUploadWidget", BulkUploadWidget);
componentRegistry.set("ReportsWidget", ReportsWidget);
componentRegistry.set("TrafficWidget", TrafficWidget);
componentRegistry.set("HomeShell", HomeShell);
componentRegistry.set("PublishingShell", PublishingShell);
componentRegistry.set("WidgetBuilderApp", WidgetBuilderApp);
componentRegistry.set("UnavailableView", UnavailableView);
componentRegistry.set("ContentExplorerShell", ContentExplorerShell);
componentRegistry.set("ContentBrowser", ContentBrowser);
componentRegistry.set("ClipboardPanel", ClipboardPanel);
componentRegistry.set("SiteCopyWizard", SiteCopyWizard);
componentRegistry.set("SubfolderCopyWizard", SubfolderCopyWizard);
componentRegistry.set("DependencyViewer", DependencyViewer);
componentRegistry.set("RelationshipsView", RelationshipsView);
componentRegistry.set("ActionToolbar", ActionToolbar);
componentRegistry.set("ContextMenu", ContextMenu);
componentRegistry.set("WorkflowAdminShell", WorkflowAdminShell);
