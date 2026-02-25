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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Dashboard module exports.
 */

export { Dashboard, type DashboardProps } from "./Dashboard";
export { DashboardLayout, type DashboardLayoutProps, type DashboardWidget } from "./DashboardLayout";
export { WelcomeWidget, type WelcomeWidgetProps } from "./WelcomeWidget";
export { WorkflowStatusWidget, type WorkflowStatusWidgetProps } from "./WorkflowStatusWidget";
export { ActivityWidget, type ActivityWidgetProps } from "./ActivityWidget";
export { ProcessMonitorWidget, type ProcessMonitorWidgetProps } from "./ProcessMonitorWidget";
export { EffectivenessWidget, type EffectivenessWidgetProps } from "./EffectivenessWidget";
export { AssetsStatusWidget, type AssetsStatusWidgetProps } from "./AssetsStatusWidget";
export { BulkUploadWidget, type BulkUploadWidgetProps } from "./BulkUploadWidget";
export { styles } from "./dashboard.styles";
