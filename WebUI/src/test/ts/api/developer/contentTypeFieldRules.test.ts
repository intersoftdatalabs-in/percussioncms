/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { afterEach, describe, expect, it, vi } from "vitest";
import {
  CONTENT_TYPE_FIELD_RULE_EXPRESSIONS_ROOT,
  emptyFieldRuleExpressions,
  fieldRuleExpressionTextsEqual,
  fieldRuleExpressionsEqual,
  fieldRuleExpressionsToTexts,
  formatFieldRuleLine,
  getContentTypeFieldRuleExpressions,
  parseFieldRuleLine,
  parseFieldRuleLines,
  parseTranslationLine,
  parseTranslationLines,
  replaceContentTypeFieldRuleExpressions,
  textsToFieldRuleExpressions,
  toFieldRuleExpressionsPutBody,
  unwrapContentTypeFieldRuleExpressions,
  wrapContentTypeFieldRuleExpressionsForWire,
} from "../../../../main/ts/api/developer/contentTypeFieldRules";
import * as client from "../../../../main/ts/api/client";
import { PATHS } from "../../../../main/ts/api/paths";

describe("unwrapContentTypeFieldRuleExpressions", () => {
  it("unwraps Jackson ContentTypeFieldRuleExpressions root", () => {
    const unwrapped = unwrapContentTypeFieldRuleExpressions({
      ContentTypeFieldRuleExpressions: {
        fieldName: "sys_title",
        validation: [{ type: "conditional", conditionals: [{ variable: "sys_title", operator: "<>", value: "" }] }],
        visibility: [],
        inputTranslation: [{ extension: "Java/global/percussion/generic/sys_ToUpperCase" }],
        outputTranslation: { empty: true },
      },
    });
    expect(unwrapped.fieldName).toBe("sys_title");
    expect(unwrapped.validation).toHaveLength(1);
    expect(unwrapped.validation?.[0].conditionals?.[0].variable).toBe("sys_title");
    expect(unwrapped.visibility).toEqual([]);
    expect(unwrapped.inputTranslation?.[0].extension).toContain("sys_ToUpperCase");
    expect(unwrapped.outputTranslation).toEqual([]);
  });

  it("unwraps a lone validation rule object and empty-collection beans", () => {
    const unwrapped = unwrapContentTypeFieldRuleExpressions({
      fieldName: "page_title",
      validation: { type: "extension", extension: "Java/global/percussion/generic/sys_ToUpperCase" },
      visibility: { empty: false },
      inputTranslation: [],
      outputTranslation: [],
    });
    expect(unwrapped.validation).toHaveLength(1);
    expect(unwrapped.validation?.[0].type).toBe("extension");
    expect(unwrapped.visibility).toEqual([]);
  });

  it("returns empty lists for null payload", () => {
    expect(unwrapContentTypeFieldRuleExpressions(null)).toEqual(emptyFieldRuleExpressions());
  });
});

describe("field-rule expression text parse/format", () => {
  it("parses a conditional with != as <> and quoted empty value", () => {
    const rule = parseFieldRuleLine('sys_title != ""', "validation");
    expect(rule.type).toBe("conditional");
    expect(rule.conditionals?.[0]).toEqual({
      variable: "sys_title",
      operator: "<>",
      value: "",
    });
  });

  it("parses IS NOT NULL before IS NULL", () => {
    const rule = parseFieldRuleLine("sys_title IS NOT NULL", "validation");
    expect(rule.conditionals?.[0].operator).toBe("IS NOT NULL");
    expect(rule.conditionals?.[0].value).toBe("");
  });

  it("parses ext: FQN with optional param", () => {
    const rule = parseFieldRuleLine(
      "ext:Java/global/percussion/generic/sys_ToUpperCase | sys_title",
      "validation",
    );
    expect(rule.type).toBe("extension");
    expect(rule.extension).toBe("Java/global/percussion/generic/sys_ToUpperCase");
    expect(rule.parameters).toEqual([{ value: "sys_title" }]);
  });

  it("parses ref: on validation and rejects it on visibility", () => {
    expect(parseFieldRuleLine("ref:sharedRequired", "validation")).toEqual({
      type: "reference",
      reference: "sharedRequired",
    });
    expect(() => parseFieldRuleLine("ref:sharedRequired", "visibility")).toThrow(/reference/i);
  });

  it("skips blank lines when parsing lists", () => {
    const rules = parseFieldRuleLines("sys_title <> \"\"\n\npage_title = hello\n", "validation");
    expect(rules).toHaveLength(2);
    expect(rules[1].conditionals?.[0].value).toBe("hello");
  });

  it("parses translation FQN with optional param", () => {
    const t = parseTranslationLine("Java/global/percussion/generic/sys_ToUpperCase | sys_title");
    expect(t.extension).toBe("Java/global/percussion/generic/sys_ToUpperCase");
    expect(t.parameters).toEqual([{ value: "sys_title" }]);
    expect(parseTranslationLines("\n\n").length).toBe(0);
  });

  it("round-trips conditional and extension text", () => {
    const env = textsToFieldRuleExpressions("sys_title", {
      validation: 'sys_title <> ""',
      visibility: "ext:Java/global/percussion/generic/sys_ToUpperCase",
      inputTranslation: "Java/global/percussion/generic/sys_ToUpperCase | sys_title",
      outputTranslation: "",
    });
    const texts = fieldRuleExpressionsToTexts(env);
    expect(texts.validation).toBe('sys_title <> ""');
    expect(texts.visibility).toBe("ext:Java/global/percussion/generic/sys_ToUpperCase");
    expect(texts.inputTranslation).toBe(
      "Java/global/percussion/generic/sys_ToUpperCase | sys_title",
    );
    expect(texts.outputTranslation).toBe("");
    expect(formatFieldRuleLine(env.validation![0])).toContain("sys_title");
  });

  it("compares texts and envelopes", () => {
    const a = emptyFieldRuleExpressions("sys_title");
    const b = emptyFieldRuleExpressions("sys_title");
    expect(fieldRuleExpressionsEqual(a, b)).toBe(true);
    b.validation = [{ type: "conditional", conditionals: [{ variable: "x", operator: "=", value: "1" }] }];
    expect(fieldRuleExpressionsEqual(a, b)).toBe(false);
    expect(
      fieldRuleExpressionTextsEqual(
        { validation: "", visibility: "", inputTranslation: "", outputTranslation: "" },
        { validation: "", visibility: "", inputTranslation: "", outputTranslation: "" },
      ),
    ).toBe(true);
  });
});

describe("toFieldRuleExpressionsPutBody / wrap", () => {
  it("sends the four required lists and wraps Jackson root", () => {
    const body = toFieldRuleExpressionsPutBody({
      fieldName: "sys_title",
      validation: [
        { type: "conditional", conditionals: [{ variable: "sys_title", operator: "<>", value: "" }] },
      ],
      visibility: [],
      inputTranslation: [{ extension: "Java/global/percussion/generic/sys_ToUpperCase" }],
      outputTranslation: [],
      designGaps: [{ code: "CT_FIELD_RULE_APPLY_WHEN" }],
      validationExpression: "ignored",
    });
    expect(body.designGaps).toBeUndefined();
    expect(body.validationExpression).toBeUndefined();
    expect(body.validation?.[0].type).toBe("conditional");
    expect(wrapContentTypeFieldRuleExpressionsForWire(body)).toEqual({
      [CONTENT_TYPE_FIELD_RULE_EXPRESSIONS_ROOT]: body,
    });
  });
});

describe("get/replace field rule expressions", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("GETs the field path and unwraps the envelope", async () => {
    const getMock = vi.spyOn(client, "get").mockResolvedValue({
      ContentTypeFieldRuleExpressions: {
        fieldName: "sys_title",
        validation: [],
        visibility: [],
        inputTranslation: [],
        outputTranslation: [],
      },
    });
    const out = await getContentTypeFieldRuleExpressions("percPage", "sys_title");
    expect(getMock).toHaveBeenCalledWith(
      `${PATHS.CONTENT_TYPES}/${encodeURIComponent("percPage")}/fields/${encodeURIComponent("sys_title")}/ruleExpressions`,
    );
    expect(out.fieldName).toBe("sys_title");
    expect(out.validation).toEqual([]);
  });

  it("PUTs a wrapped full-replace body", async () => {
    const putMock = vi.spyOn(client, "put").mockResolvedValue({
      ContentTypeFieldRuleExpressions: {
        fieldName: "sys_title",
        validation: [
          { type: "conditional", conditionals: [{ variable: "sys_title", operator: "<>", value: "#3896" }] },
        ],
        visibility: [],
        inputTranslation: [],
        outputTranslation: [],
      },
    });
    const out = await replaceContentTypeFieldRuleExpressions("percPage", "sys_title", {
      fieldName: "sys_title",
      validation: [
        { type: "conditional", conditionals: [{ variable: "sys_title", operator: "<>", value: "#3896" }] },
      ],
      visibility: [],
      inputTranslation: [],
      outputTranslation: [],
    });
    expect(putMock).toHaveBeenCalledWith(
      `${PATHS.CONTENT_TYPES}/${encodeURIComponent("percPage")}/fields/${encodeURIComponent("sys_title")}/ruleExpressions`,
      {
        ContentTypeFieldRuleExpressions: {
          fieldName: "sys_title",
          validation: [
            {
              type: "conditional",
              conditionals: [{ variable: "sys_title", operator: "<>", value: "#3896" }],
            },
          ],
          visibility: [],
          inputTranslation: [],
          outputTranslation: [],
        },
      },
    );
    expect(out.validation?.[0].conditionals?.[0].value).toBe("#3896");
  });
});
