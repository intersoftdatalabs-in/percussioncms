#!/usr/bin/env python3
# Copyright (c) 2026 Intersoft Data Labs, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#
# See the License for the specific language governing permissions and
# limitations under the License.
"""Pinned product-term glossary for the perc-i18n translation pipeline.

Machine translation renders some Percussion product terms literally
("write down", "knock down", "dismantle") instead of their product sense.
This module pins the correct per-locale rendering for those terms so that
``i18n_translate.py`` / ``i18n_translate_direct.py`` back-fills (and any
``--force`` re-translation) reuse the reviewed text instead of asking the
engine again.

Precedent: issue #7131 (``CmsUi.tmx`` "Take Down" shipped as de
``Notieren``, es ``Derribar``, fr ``Démonter``, zh ``记录下来`` — all
"write/demolish down" instead of "remove from the live site").

Contract:

* Keys are exact full-string ``en-us`` segments, matched after ``strip()``.
  Case variants of the same term are listed explicitly (``"Take Down"`` and
  ``"Take down"``) rather than case-folded, so an unexpected new casing
  still goes to the engine (and review) instead of silently matching.
* Targets are lowercase-hyphen BCP-47 (``zh-tw``, ``ja-jp``); :func:`lookup`
  normalizes ``ZH_TW`` / ``zh_TW`` to the same bucket.
* ``lou`` is intentionally absent: Louisiana Creole drafts are owned by
  ``i18n_lou_draft.py``.
* Compound / status strings (``"not taken down"``, ``"Take down failed"``,
  ``"Takedown failed"``, ``"Take down (unpublish)"``) are intentionally NOT
  pinned here — they need per-locale grammar a term table cannot supply.
  They are tracked as follow-up on #7131 / PR #7116 rebase review.

Cross-platform: pure data + string ops; no I/O on import.
"""
from __future__ import annotations

# Product sense: "remove from the live site" (antonym of Publish).
# Title-case action label (e.g. perc.ui.finder.view@Take Down).
_TAKE_DOWN: dict[str, str] = {
    'ar': 'إلغاء النشر',
    'bn': 'অপ্রকাশিত করুন',
    'de': 'Zurückziehen',
    'es': 'Retirar',
    'fr': 'Dépublier',
    'he': 'לבטל פרסום',
    'hi': 'अप्रकाशित करें',
    'it': 'Ritirare',
    'ja-jp': '公開停止',
    'nb': 'Avpubliser',
    'nl': 'Depubliceren',
    'pl': 'Wycofać publikację',
    'pt': 'Despublicar',
    'ru': 'Снять с публикации',
    'sv': 'Avpublicera',
    'te': 'ప్రచురణ ఉపసంహరించండి',
    'tr': 'Yayından kaldır',
    'uk': 'Зняти з публікації',
    'zh-cn': '下线',
    'zh-tw': '下線',
}

# Plural count-noun label (e.g. perc.ui.activity.gadget@Take Downs).
_TAKE_DOWNS: dict[str, str] = {
    'ar': 'إلغاءات النشر',
    'bn': 'অপ্রকাশিত',
    'de': 'Zurückziehungen',
    'es': 'Retiros',
    'fr': 'Retraits',
    'he': 'ביטולי פרסום',
    'hi': 'अप्रकाशित',
    'it': 'Ritiri',
    'ja-jp': '公開停止',
    'nb': 'Avpubliseringer',
    'nl': 'Depublicaties',
    'pl': 'Wycofania publikacji',
    'pt': 'Despublicações',
    'ru': 'Снятия с публикации',
    'sv': 'Avpubliceringar',
    'te': 'ప్రచురణ ఉపసంహరణలు',
    'tr': 'Yayından kaldırmalar',
    'uk': 'Зняття з публікації',
    'zh-cn': '下线',
    'zh-tw': '下線',
}

#: Exact en-us source string -> {target lang -> pinned translation}.
TERMS: dict[str, dict[str, str]] = {
    'Take Down': dict(_TAKE_DOWN),
    # Sentence-case button variant (e.g. perc.ui.explorer@Take down):
    # same product sense, same rendering.
    'Take down': dict(_TAKE_DOWN),
    'Take Downs': dict(_TAKE_DOWNS),
}


def normalize_target(target: str) -> str:
    """Normalize a locale tag to the glossary bucket (lowercase-hyphen)."""
    return target.strip().lower().replace('_', '-')


def lookup(text: str, target: str) -> str | None:
    """Return the pinned translation for an exact en-us source string.

    Returns ``None`` when the source string is not a pinned term or the
    target locale has no pinned rendering — callers then fall through to
    cache / machine translation as before.
    """
    entry = TERMS.get(text.strip())
    if not entry:
        return None
    return entry.get(normalize_target(target))
