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

import { describe, expect, it } from "vitest";
import {
  advance,
  back,
  createWizard,
  currentStepId,
  finishWizard,
  isFinalStep,
  isFinished,
  resetWizard,
} from "../../../main/ts/contentExplorer/wizards/state";

describe("createWizard", () => {
  it("throws when given an empty step list", () => {
    expect(() => createWizard([])).toThrow(/at least one step/);
  });

  it("starts at step 0 by default", () => {
    const w = createWizard(["source", "target", "confirm"]);
    expect(w.current).toBe(0);
    expect(w.submitting).toBe(false);
    expect(w.result).toBeUndefined();
  });

  it("accepts a named initial step", () => {
    const w = createWizard(["a", "b", "c"], "b");
    expect(w.current).toBe(1);
  });

  it("throws for an unknown initial step", () => {
    expect(() => createWizard(["a"], "z")).toThrow(/unknown initial step/);
  });
});

describe("advance / back / isFinalStep / currentStepId", () => {
  const steps = ["source", "target", "options", "confirm"];

  it("advance moves to the next step", () => {
    const w = createWizard(steps);
    expect(advance(w).current).toBe(1);
  });

  it("advance at the final step flips to submitting=true (host submits)", () => {
    const w = createWizard(steps);
    const a = advance(advance(advance(w)));
    expect(a.current).toBe(3);
    expect(isFinalStep(a)).toBe(true);
    expect(advance(a).submitting).toBe(true);
  });

  it("advance is a no-op when already submitting", () => {
    const w = createWizard(steps);
    const submitting = { ...w, current: 3, submitting: true };
    expect(advance(submitting).submitting).toBe(true);
    expect(advance(submitting).current).toBe(3);
  });

  it("back moves to the previous step (no-op at step 0)", () => {
    const w = createWizard(steps);
    expect(back(w).current).toBe(0);
    expect(back({ ...w, current: 2 }).current).toBe(1);
  });

  it("back is a no-op when submitting", () => {
    const w = { ...createWizard(steps), current: 3, submitting: true };
    expect(back(w).current).toBe(3);
  });

  it("currentStepId returns the steps array's entry", () => {
    const w = createWizard(steps);
    expect(currentStepId(w)).toBe("source");
    expect(currentStepId({ ...w, current: 2 })).toBe("options");
  });
});

describe("finishWizard / resetWizard / isFinished", () => {
  it("finishWizard marks the result and clears submitting", () => {
    const w = { ...createWizard(["a"]), submitting: true };
    const done = finishWizard(w, { kind: "ok" });
    expect(done.submitting).toBe(false);
    expect(done.result).toEqual({ kind: "ok" });
    expect(isFinished(done)).toBe(true);
  });

  it("finishWizard captures the error message verbatim", () => {
    const w = { ...createWizard(["a"]), submitting: true };
    const done = finishWizard(w, { kind: "error", message: "boom" });
    expect(done.result).toEqual({ kind: "error", message: "boom" });
  });

  it("resetWizard returns to step 0 with no result and not submitting", () => {
    const w = {
      ...createWizard(["a", "b"]),
      current: 1,
      submitting: true,
      result: { kind: "error" as const, message: "x" },
    };
    const r = resetWizard(w);
    expect(r.current).toBe(0);
    expect(r.submitting).toBe(false);
    expect(r.result).toBeUndefined();
    expect(isFinished(r)).toBe(false);
  });

  it("isFinished is false for a fresh wizard", () => {
    const w = createWizard(["a"]);
    expect(isFinished(w)).toBe(false);
  });
});
