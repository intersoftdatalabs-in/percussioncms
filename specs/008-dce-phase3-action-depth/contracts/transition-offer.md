# Contract: Transition offer set

## Purpose

Define what action menus MUST show so US1 acceptance is verifiable without implementation knowledge.

## Rules

1. For any item, actor, and workflow state, the menu offer set equals exactly the permitted transitions — the same set DCE offers.
2. Zero permitted transitions is a valid offer set: menus show no transition option (never a dead option that fails on use).
3. After an executed transition changes item state, the offer set is recomputed before the next menu open.
4. Offer behavior is consistent across tree, list, and related host surfaces for the same item and actor (FR-010).

## Verification

SC-001 sample matrix: ≥20 items across ≥5 workflow states, 100% match with DCE behavior.
