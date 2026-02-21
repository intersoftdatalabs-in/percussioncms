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
 * Main entry point for the modern UI bundle.
 *
 * <p>Importing this module initialises the bridge layer so that
 * {@code window.PercModernUI.mount()} is available for legacy pages.</p>
 */

// Initialise the bridge (registers on window.PercModernUI)
import "./bridge";

console.info("[PercModernUI] Modern UI bridge loaded.");
