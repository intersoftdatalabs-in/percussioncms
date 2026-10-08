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
  keywordUpdateForClearedChoiceDescription,
  keywordUpdateForDescribedChoice,
  keywordUpdateForRelabeledChoice,
  keywordUpdateForRemovedChoice,
  keywordUpdateForResequencedChoice,
  keywordUpdateForRevaluedChoice,
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

describe("keywordUpdateForRevaluedChoice", () => {
  const two: KeywordChoiceSummary[] = [
    { label: "High", value: "high", description: "top", sequence: 1 },
    { label: "Low", value: "low", description: "bottom", sequence: 2 },
  ];

  it("does not build a write for a missing choice or the same value", () => {
    expect(keywordUpdateForRevaluedChoice(baseline, two, -1, "mid")).toBe("missing");
    expect(keywordUpdateForRevaluedChoice(baseline, two, 2, "mid")).toBe("missing");
    expect(keywordUpdateForRevaluedChoice(baseline, two, 1.5, "mid")).toBe("missing");
    expect(keywordUpdateForRevaluedChoice(baseline, two, 1, "low")).toBe("unchanged");
    expect(keywordUpdateForRevaluedChoice(baseline, two, 1, " low ")).toBe("unchanged");
  });

  it("does not build a write when a blank value already means the label", () => {
    expect(keywordUpdateForRevaluedChoice(baseline, two, 1, "   ")).toBe("unchanged");
    const storedAsLabel: KeywordChoiceSummary[] = [
      { label: "High", value: "high", sequence: 1 },
      { label: "Low", value: "Low", sequence: 2 },
    ];
    expect(keywordUpdateForRevaluedChoice(baseline, storedAsLabel, 1, "")).toBe("unchanged");
    const emptyValue: KeywordChoiceSummary[] = [
      { label: "High", value: "high", sequence: 1 },
      { label: "Low", value: "", sequence: 2 },
    ];
    expect(keywordUpdateForRevaluedChoice(baseline, emptyValue, 1, "  ")).toBe("unchanged");
  });

  it("does not build a write when the label and the value are both blank", () => {
    const blank: KeywordChoiceSummary[] = [{ label: "  ", value: "keep", sequence: 1 }];
    expect(keywordUpdateForRevaluedChoice(baseline, blank, 0, "   ")).toBe("blank");
  });

  it("does not build a write when the value matches another choice", () => {
    expect(keywordUpdateForRevaluedChoice(baseline, two, 1, "HIGH")).toBe("duplicate");
    expect(keywordUpdateForRevaluedChoice(baseline, two, 0, " Low ")).toBe("duplicate");
    const distinct: KeywordChoiceSummary[] = [
      { label: "High", value: "low", description: "top", sequence: 1 },
      { label: "Low", value: "mid", description: "bottom", sequence: 2 },
    ];
    expect(keywordUpdateForRevaluedChoice(baseline, distinct, 1, "   ")).toBe("duplicate");
  });

  it("changes one value and keeps label, description, sequence, and the other choice", () => {
    expect(keywordUpdateForRevaluedChoice(baseline, two, 1, " mid ")).toEqual({
      label: "Priority",
      description: "Item priority",
      sequence: 4,
      choices: [
        { label: "High", value: "high", description: "top", sequence: 1 },
        { label: "Low", value: "mid", description: "bottom", sequence: 2 },
      ],
    });
    expect(keywordUpdateForRevaluedChoice(baseline, two, 0, "HIGH")).toEqual({
      label: "Priority",
      description: "Item priority",
      sequence: 4,
      choices: [
        { label: "High", value: "HIGH", description: "top", sequence: 1 },
        { label: "Low", value: "low", description: "bottom", sequence: 2 },
      ],
    });
  });

  it("stores a blank value as the label when that value is not already in use", () => {
    const distinct: KeywordChoiceSummary[] = [
      { label: "High", value: "high", description: "top", sequence: 1 },
      { label: "Low", value: "mid", description: "bottom", sequence: 2 },
    ];
    expect(keywordUpdateForRevaluedChoice(baseline, distinct, 1, "  ")).toEqual({
      label: "Priority",
      description: "Item priority",
      sequence: 4,
      choices: [
        { label: "High", value: "high", description: "top", sequence: 1 },
        { label: "Low", value: "Low", description: "bottom", sequence: 2 },
      ],
    });
  });
});

describe("keywordUpdateForDescribedChoice", () => {
  const two: KeywordChoiceSummary[] = [
    { label: "High", value: "high", description: "top", sequence: 1 },
    { label: "Low", value: "low", description: "bottom", sequence: 2 },
  ];

  it("does not build a write for a missing choice or the same description", () => {
    expect(keywordUpdateForDescribedChoice(baseline, two, -1, "mid")).toBe("missing");
    expect(keywordUpdateForDescribedChoice(baseline, two, 2, "mid")).toBe("missing");
    expect(keywordUpdateForDescribedChoice(baseline, two, 1.5, "mid")).toBe("missing");
    expect(keywordUpdateForDescribedChoice(baseline, two, 1, "bottom")).toBe("unchanged");
    expect(keywordUpdateForDescribedChoice(baseline, two, 1, " bottom ")).toBe("unchanged");
  });

  it("does not clear a stored description when the draft is blank", () => {
    expect(keywordUpdateForDescribedChoice(baseline, two, 1, "")).toBe("blank");
    expect(keywordUpdateForDescribedChoice(baseline, two, 1, "   ")).toBe("blank");
    expect(keywordUpdateForDescribedChoice(baseline, two, 0, "\n")).toBe("blank");
  });

  it("does not write when there is no stored description and the draft is blank", () => {
    const empty: KeywordChoiceSummary[] = [
      { label: "High", value: "high", sequence: 1 },
      { label: "Low", value: "low", description: "  ", sequence: 2 },
    ];
    expect(keywordUpdateForDescribedChoice(baseline, empty, 1, "")).toBe("unchanged");
    expect(keywordUpdateForDescribedChoice(baseline, empty, 1, "   ")).toBe("unchanged");
  });

  it("sets one description and keeps label, value, sequence, and the other choice", () => {
    expect(keywordUpdateForDescribedChoice(baseline, two, 1, " middle ")).toEqual({
      label: "Priority",
      description: "Item priority",
      sequence: 4,
      choices: [
        { label: "High", value: "high", description: "top", sequence: 1 },
        { label: "Low", value: "low", description: "middle", sequence: 2 },
      ],
    });
  });

  it("allows the same description on another choice", () => {
    expect(keywordUpdateForDescribedChoice(baseline, two, 1, "top")).toEqual({
      label: "Priority",
      description: "Item priority",
      sequence: 4,
      choices: [
        { label: "High", value: "high", description: "top", sequence: 1 },
        { label: "Low", value: "low", description: "top", sequence: 2 },
      ],
    });
  });

  it("does not copy the keyword description onto the choice", () => {
    const sent = keywordUpdateForDescribedChoice(baseline, two, 1, "Item priority");
    expect(sent).not.toBe("unchanged");
    if (typeof sent === "string") {
      throw new Error(sent);
    }
    expect(sent.description).toBe("Item priority");
    expect(sent.choices?.[1]?.description).toBe("Item priority");
    expect(sent.choices?.[0]?.description).toBe("top");
    expect(sent.choices?.[1]?.label).toBe("Low");
    expect(sent.choices?.[1]?.value).toBe("low");
    expect(sent.choices?.[1]?.sequence).toBe(2);
  });
});

describe("keywordUpdateForClearedChoiceDescription", () => {
  const two: KeywordChoiceSummary[] = [
    { label: "High", value: "high", description: "top", sequence: 1 },
    { label: "Low", value: "low", description: "bottom", sequence: 2 },
  ];

  it("does not build a write for a missing choice or an empty description", () => {
    expect(keywordUpdateForClearedChoiceDescription(baseline, two, -1)).toBe("missing");
    expect(keywordUpdateForClearedChoiceDescription(baseline, two, 2)).toBe("missing");
    expect(keywordUpdateForClearedChoiceDescription(baseline, two, 1.5)).toBe("missing");
    const empty: KeywordChoiceSummary[] = [
      { label: "High", value: "high", sequence: 1 },
      { label: "Low", value: "low", description: "   ", sequence: 2 },
    ];
    expect(keywordUpdateForClearedChoiceDescription(baseline, empty, 1)).toBe("unchanged");
    expect(keywordUpdateForClearedChoiceDescription(baseline, empty, 0)).toBe("unchanged");
  });

  it("still refuses a blank description on the set action", () => {
    expect(keywordUpdateForDescribedChoice(baseline, two, 1, "")).toBe("blank");
    expect(keywordUpdateForDescribedChoice(baseline, two, 1, "   ")).toBe("blank");
  });

  it("sends an empty description for one choice and keeps the rest", () => {
    expect(keywordUpdateForClearedChoiceDescription(baseline, two, 1)).toEqual({
      label: "Priority",
      description: "Item priority",
      sequence: 4,
      choices: [
        { label: "High", value: "high", description: "top", sequence: 1 },
        { label: "Low", value: "low", description: "", sequence: 2 },
      ],
    });
  });

  it("accepts a response that omits the cleared description", () => {
    const sent = keywordUpdateForClearedChoiceDescription(baseline, two, 1);
    if (typeof sent === "string") {
      throw new Error(sent);
    }
    const accepted = savedChoicesAfterAdd(sent, {
      label: "Priority",
      description: "Item priority",
      sequence: 4,
      choices: [
        { label: "High", value: "high", description: "top", sequence: 1 },
        { label: "Low", value: "low", sequence: 2 },
      ],
    });
    expect(accepted).toEqual([
      { label: "High", value: "high", description: "top", sequence: 1 },
      { label: "Low", value: "low", sequence: 2 },
    ]);
    expect(sent.description).toBe("Item priority");
    expect(sent.choices?.[1]?.description).toBe("");
    expect(sent.choices?.[1]?.label).toBe("Low");
    expect(sent.choices?.[1]?.value).toBe("low");
    expect(sent.choices?.[1]?.sequence).toBe(2);
  });
});

describe("keywordUpdateForResequencedChoice", () => {
  const two: KeywordChoiceSummary[] = [
    { label: "High", value: "high", description: "top", sequence: 1 },
    { label: "Low", value: "low", description: "bottom", sequence: 2 },
  ];

  it("does not build a write for a missing choice or the same sequence", () => {
    expect(keywordUpdateForResequencedChoice(baseline, two, -1, "9")).toBe("missing");
    expect(keywordUpdateForResequencedChoice(baseline, two, 2, "9")).toBe("missing");
    expect(keywordUpdateForResequencedChoice(baseline, two, 1.5, "9")).toBe("missing");
    expect(keywordUpdateForResequencedChoice(baseline, two, 1, "2")).toBe("unchanged");
    expect(keywordUpdateForResequencedChoice(baseline, two, 1, " 2 ")).toBe("unchanged");
    expect(keywordUpdateForResequencedChoice(baseline, two, 1, "02")).toBe("unchanged");
  });

  it("does not write a blank sequence", () => {
    expect(keywordUpdateForResequencedChoice(baseline, two, 1, "")).toBe("blank");
    expect(keywordUpdateForResequencedChoice(baseline, two, 1, "   ")).toBe("blank");
    expect(keywordUpdateForResequencedChoice(baseline, two, 0, "\n")).toBe("blank");
  });

  it("does not write a non-integer or a negative sequence", () => {
    expect(keywordUpdateForResequencedChoice(baseline, two, 1, "1.5")).toBe("invalid");
    expect(keywordUpdateForResequencedChoice(baseline, two, 1, "9a")).toBe("invalid");
    expect(keywordUpdateForResequencedChoice(baseline, two, 1, "-1")).toBe("invalid");
    expect(keywordUpdateForResequencedChoice(baseline, two, 1, "+3")).toBe("invalid");
    expect(keywordUpdateForResequencedChoice(baseline, two, 1, "1e2")).toBe("invalid");
    expect(keywordUpdateForResequencedChoice(baseline, two, 1, "2147483648")).toBe("invalid");
  });

  it("changes one sequence and keeps label, value, description, and the other choice", () => {
    expect(keywordUpdateForResequencedChoice(baseline, two, 1, " 9 ")).toEqual({
      label: "Priority",
      description: "Item priority",
      sequence: 4,
      choices: [
        { label: "High", value: "high", description: "top", sequence: 1 },
        { label: "Low", value: "low", description: "bottom", sequence: 9 },
      ],
    });
    expect(keywordUpdateForResequencedChoice(baseline, two, 1, "2147483647")).toEqual({
      label: "Priority",
      description: "Item priority",
      sequence: 4,
      choices: [
        { label: "High", value: "high", description: "top", sequence: 1 },
        { label: "Low", value: "low", description: "bottom", sequence: 2147483647 },
      ],
    });
  });

  it("allows the same sequence on another choice", () => {
    expect(keywordUpdateForResequencedChoice(baseline, two, 1, "1")).toEqual({
      label: "Priority",
      description: "Item priority",
      sequence: 4,
      choices: [
        { label: "High", value: "high", description: "top", sequence: 1 },
        { label: "Low", value: "low", description: "bottom", sequence: 1 },
      ],
    });
  });

  it("does not copy the keyword sequence onto the choice", () => {
    const sent = keywordUpdateForResequencedChoice(baseline, two, 1, "4");
    expect(sent).not.toBe("unchanged");
    if (typeof sent === "string") {
      throw new Error(sent);
    }
    expect(sent.sequence).toBe(4);
    expect(sent.description).toBe("Item priority");
    expect(sent.choices?.[1]?.sequence).toBe(4);
    expect(sent.choices?.[0]?.sequence).toBe(1);
    expect(sent.choices?.[1]?.label).toBe("Low");
    expect(sent.choices?.[1]?.value).toBe("low");
    expect(sent.choices?.[1]?.description).toBe("bottom");
  });

  it("writes zero when the stored sequence is missing", () => {
    const missing: KeywordChoiceSummary[] = [
      { label: "High", value: "high", description: "top", sequence: 1 },
      { label: "Low", value: "low", description: "bottom" },
    ];
    expect(keywordUpdateForResequencedChoice(baseline, missing, 1, "0")).toEqual({
      label: "Priority",
      description: "Item priority",
      sequence: 4,
      choices: [
        { label: "High", value: "high", description: "top", sequence: 1 },
        { label: "Low", value: "low", description: "bottom", sequence: 0 },
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
