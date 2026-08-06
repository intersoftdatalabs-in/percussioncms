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

import React, { createContext, useCallback, useContext, useState } from "react";
import { message, MSG } from "../i18n/message";

interface DirtyFormContextValue {
  dirty: boolean;
  setDirty: (dirty: boolean) => void;
  /** Returns false if navigation should be blocked (user cancelled). */
  confirmIfDirty: () => boolean;
}

const DirtyFormContext = createContext<DirtyFormContextValue>({
  dirty: false,
  setDirty: () => undefined,
  confirmIfDirty: () => true,
});

export function DirtyFormProvider({
  children,
}: {
  children: React.ReactNode;
}): React.ReactElement {
  const [dirty, setDirty] = useState(false);

  const confirmIfDirty = useCallback(() => {
    if (!dirty) {
      return true;
    }
    const ok = window.confirm(message(MSG.PUBLISH_DISCARD_CHANGES));
    if (ok) {
      setDirty(false);
    }
    return ok;
  }, [dirty]);

  return (
    <DirtyFormContext.Provider value={{ dirty, setDirty, confirmIfDirty }}>
      {children}
    </DirtyFormContext.Provider>
  );
}

export function useDirtyForm(): DirtyFormContextValue {
  return useContext(DirtyFormContext);
}
