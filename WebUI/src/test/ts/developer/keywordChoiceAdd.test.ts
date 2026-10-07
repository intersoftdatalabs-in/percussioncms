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

import { describe, expect, it } from "vitest";
import type { KeywordChoiceSummary, KeywordSummary } from "../../../main/ts/api/developer/types";
import {
  isBlankChoiceDraft,
  isDuplicateChoice,
  keywordUpdateForAddedChoice,
  keywordUpdateForRelabeledChoice,
  keywordUpdateForRemovedChoice,
  savedChoicesAfterAdd,
  unwrapKeywordPayload,
} from "../../../main/ts/developer/keywordChoiceAdd";

const previous: KeywordChoiceSummary[] = [
  { label: "High", value: "high", description: "top", sequence: 1 },
];

const baseline: KeywordSummary = {
  label: "Priority",
  description: "Item priority",
  sequence: 4,
  choices: previous,
};

describe("keywordUpdateForAddedChoice", () => {
  it("does not build a write for a blank label", () => {
    expect(isBlankChoiceDraft({ label: "   ", value: "x" })).toBe(true);
    expect(
      keywordUpdateForAddedChoice(baseline, previous, { label: "  ", value: "low" }),
    ).toBe("blank");
  });

  it("does not build a write when the label or value already exists", () => {
    expect(isDuplicateChoice(previous, { label: "HIGH", value: "other" })).toBe(true);
    expect(isDuplicateChoice(previous, { label: "Other", value: "mid" })).toBe(false);
    expect(isDuplicateChoice(previous, { label: "Other", value: " HIGH " })).toBe(true);
    expect(
      keywordUpdateForAddedChoice(baseline, previous, { label: "high", value: "high" }),
    ).toBe("duplicate");
    expect(
      keywordUpdateForAddedChoice(baseline, previous, { label: "Other", value: "high" }),
    ).toBe("duplicate");
  });

  it("appends one choice and keeps keyword label, description, and sequence", () => {
    const sent = keywordUpdateForAddedChoice(baseline, previous, {
      label: " Low ",
      value: "",
    });
    expect(sent).toEqual({
      label: "Priority",
      description: "Item priority",
      sequence: 4,
      choices: [
        { label: "High", value: "high", description: "top", sequence: 1 },
        { label: "Low", value: "Low", sequence: 2 },
      ],
    });
  });
});

describe("keywordUpdateForRemovedChoice", () => {
  const two: KeywordChoiceSummary[] = [
    { label: "High", value: "high", description: "top", sequence: 1 },
    { label: "Low", value: "low", description: "bottom", sequence: 2 },
  ];

  it("does not build a write for an index that is not a choice", () => {
    expect(keywordUpdateForRemovedChoice(baseline, two, -1)).toBe("missing");
    expect(keywordUpdateForRemovedChoice(baseline, two, 2)).toBe("missing");
    expect(keywordUpdateForRemovedChoice(baseline, two, 1.5)).toBe("missing");
  });

  it("drops one choice and keeps keyword label, description, sequence, and the other choice", () => {
    expect(keywordUpdateForRemovedChoice(baseline, two, 0)).toEqual({
      label: "Priority",
      description: "Item priority",
      sequence: 4,
      choices: [{ label: "Low", value: "low", description: "bottom", sequence: 2 }],
    });
  });

  it("clears the list when the last choice is removed and does not omit choices", () => {
    expect(keywordUpdateForRemovedChoice(baseline, previous, 0)).toEqual({
      label: "Priority",
      description: "Item priority",
      sequence: 4,
      choices: [],
    });
  });
});

describe("keywordUpdateForRelabeledChoice", () => {
  const two: KeywordChoiceSummary[] = [
    { label: "High", value: "high", description: "top", sequence: 1 },
    { label: "Low", value: "low", description: "bottom", sequence: 2 },
  ];

  it("does not build a write for a blank label, the same label, or a missing choice", () => {
    expect(keywordUpdateForRelabeledChoice(baseline, two, 1, "   ")).toBe("blank");
    expect(keywordUpdateForRelabeledChoice(baseline, two, 1, "Low")).toBe("unchanged");
    expect(keywordUpdateForRelabeledChoice(baseline, two, 1, " Low ")).toBe("unchanged");
    expect(keywordUpdateForRelabeledChoice(baseline, two, -1, "Medium")).toBe("missing");
    expect(keywordUpdateForRelabeledChoice(baseline, two, 2, "Medium")).toBe("missing");
  });

  it("does not build a write when the label matches another choice", () => {
    expect(keywordUpdateForRelabeledChoice(baseline, two, 1, "HIGH")).toBe("duplicate");
    expect(keywordUpdateForRelabeledChoice(baseline, two, 0, " low ")).toBe("duplicate");
  });

  it("changes one label and keeps value, description, sequence, and the other choice", () => {
    expect(keywordUpdateForRelabeledChoice(baseline, two, 1, " Medium ")).toEqual({
      label: "Priority",
      description: "Item priority",
      sequence: 4,
      choices: [
        { label: "High", value: "high", description: "top", sequence: 1 },
        { label: "Medium", value: "low", description: "bottom", sequence: 2 },
      ],
    });
    expect(keywordUpdateForRelabeledChoice(baseline, two, 0, "HIGH")).toEqual({
      label: "Priority",
      description: "Item priority",
      sequence: 4,
      choices: [
        { label: "HIGH", value: "high", description: "top", sequence: 1 },
        { label: "Low", value: "low", description: "bottom", sequence: 2 },
      ],
    });
  });
});

describe("savedChoicesAfterAdd", () => {
  const sent = keywordUpdateForAddedChoice(baseline, previous, {
    label: "Low",
    value: "low",
  }) as KeywordSummary;

  it("accepts a response that keeps metadata and previous choices", () => {
    const listed = savedChoicesAfterAdd(sent, {
      ...sent,
      guid: { uuid: 9 },
    });
    expect(listed).toEqual(sent.choices);
  });

  it("accepts a Keyword envelope", () => {
    expect(unwrapKeywordPayload({ Keyword: sent })?.label).toBe("Priority");
    expect(savedChoicesAfterAdd(sent, { Keyword: sent })).toEqual(sent.choices);
  });

  it("accepts one JAXB choice object and a KeywordChoice list", () => {
    const one: KeywordSummary = {
      label: "Priority",
      description: "Item priority",
      sequence: 4,
      choices: [{ label: "High", value: "high", sequence: 1 }],
    };
    expect(
      savedChoicesAfterAdd(one, {
        ...one,
        choices: { label: "High", value: "high", sequence: 1 },
      } as unknown as KeywordSummary),
    ).toEqual(one.choices);
    expect(
      savedChoicesAfterAdd(sent, {
        ...sent,
        choices: { KeywordChoice: sent.choices },
      } as unknown as KeywordSummary),
    ).toEqual(sent.choices);
  });

  it("accepts an empty choice list only when the response is also empty", () => {
    const cleared: KeywordSummary = {
      label: "Priority",
      description: "Item priority",
      sequence: 4,
      choices: [],
    };
    expect(savedChoicesAfterAdd(cleared, cleared)).toEqual([]);
    expect(savedChoicesAfterAdd(cleared, { Keyword: { ...cleared, choices: null } })).toEqual(
      [],
    );
    expect(
      savedChoicesAfterAdd(cleared, {
        ...cleared,
        choices: [{ label: "High", value: "high", sequence: 1 }],
      }),
    ).toBeNull();
    expect(savedChoicesAfterAdd(cleared, { ...cleared, label: "Renamed" })).toBeNull();
  });

  it("rejects a response that changes the keyword or drops a previous choice", () => {
    expect(savedChoicesAfterAdd(sent, { ...sent, label: "Renamed" })).toBeNull();
    expect(savedChoicesAfterAdd(sent, { ...sent, description: "changed" })).toBeNull();
    expect(savedChoicesAfterAdd(sent, { ...sent, sequence: 9 })).toBeNull();
    expect(
      savedChoicesAfterAdd(sent, {
        ...sent,
        choices: [{ label: "Low", value: "low", sequence: 2 }],
      }),
    ).toBeNull();
    expect(
      savedChoicesAfterAdd(sent, {
        ...sent,
        choices: [
          ...(sent.choices ?? []),
          { label: "Invented", value: "invented", sequence: 3 },
        ],
      }),
    ).toBeNull();
  });
});
