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

/**
 * Keep Explorer row context menus inside the viewport (#3629).
 * A long nested catalog can otherwise open below/right of the window so
 * MENU parents are not clickable.
 */

export interface ViewportSize {
  width: number;
  height: number;
}

export interface MenuSize {
  width: number;
  height: number;
}

const PAD = 8;
const DEFAULT_MENU: MenuSize = { width: 240, height: 280 };

export function clampContextMenuPosition(
  x: number,
  y: number,
  viewport: ViewportSize,
  size: MenuSize = DEFAULT_MENU,
): { x: number; y: number } {
  const maxX = Math.max(PAD, viewport.width - size.width - PAD);
  const maxY = Math.max(PAD, viewport.height - size.height - PAD);
  return {
    x: Math.min(Math.max(PAD, x), maxX),
    y: Math.min(Math.max(PAD, y), maxY),
  };
}
