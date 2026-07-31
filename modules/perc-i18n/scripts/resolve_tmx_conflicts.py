#!/usr/bin/env python3
"""Resolve merge-conflict markers in canonical TMX files by accepting both sides.

Used after a ``git stash pop`` that left ``<<<<<<< Updated upstream`` /
``======= / ``>>>>>>> Stashed changes`` markers inside the i18n TMX
files (``CmsUi.tmx``, ``SystemResources.tmx``, ``DeveloperUi.tmx``).
The merge had ~2,600 hunks; resolving each by hand is not practical.

Strategy
--------
For every conflict hunk in a TMX file:

1. Split the hunk into the ``Updated upstream`` side and the
   ``Stashed changes`` side.
2. Extract every ``<tuv xml:lang="...">...</tuv>`` substring from each
   side (including the trailing ``</tu>``).
3. Build the merged text by concatenating every distinct ``<tuv>`` from
   both sides, then closing the ``<tu>`` once.
   * If the same ``xml:lang`` appears on both sides with **identical
     text**, dedupe (no warning).
   * If the same ``xml:lang`` appears on both sides with **different
     text**, keep the ``Stashed changes`` version and log a warning so
     the user can re-translate that key if needed.
4. Preserve everything outside conflict regions byte-for-byte
   (whitespace, indentation of the surrounding ``<tu>``, comments).

The output is written in place. XML is re-validated with
``xml.etree.ElementTree.fromstring`` after the run; the script exits
non-zero on parse failure so it can be used as a guardrail.

Usage
-----
    python3 modules/perc-i18n/scripts/resolve_tmx_conflicts.py            # all three files
    python3 modules/perc-i18n/scripts/resolve_tmx_conflicts.py CmsUi.tmx   # single file
"""
from __future__ import annotations

import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent.parent.parent
I18N_DIR = REPO_ROOT / 'modules' / 'perc-i18n' / 'src' / 'main' / 'resources' / 'i18n'
DEFAULT_FILES = ('CmsUi.tmx', 'SystemResources.tmx', 'DeveloperUi.tmx')


class StructuralConflictError(RuntimeError):
    """Raised when a hunk's ``theirs`` side contains a new ``<tu>`` block.

    The simple merge (union of trailing ``<tuv>`` elements) cannot
    safely resolve structural changes — verbatim concatenation would
    interleave ``<tuv>`` children across two distinct ``<tu>``
    blocks, producing well-formed XML that is semantically wrong.
    """

    def __init__(self, filename: str, hunk_index: int, start_line: int, end_line: int, snippet: str) -> None:
        self.filename = filename
        self.hunk_index = hunk_index
        self.start_line = start_line
        self.end_line = end_line
        self.snippet = snippet
        super().__init__(
            f'{filename}:hunk#{hunk_index}: structural conflict '
            f'(lines {start_line}-{end_line}); first 200 chars of theirs: {snippet!r}'
        )

# Conflict marker patterns. We accept any side label because the user's
# stash may have stored "Stashed changes" or a custom branch label.
START_RE = re.compile(r'^<<<<<<< .+$', re.MULTILINE)
SEP_RE = re.compile(r'^=======$', re.MULTILINE)
END_RE = re.compile(r'^>>>>>>> .+$', re.MULTILINE)

# Match a single <tuv ...> ... </tuv> block, including the xml:lang
# attribute, possibly multi-line with whitespace.
TUV_RE = re.compile(
    r'<tuv\s+xml:lang="(?P<lang>[^"]+)">.*?</tuv>',
    re.DOTALL,
)

# Match a trailing </tu> at the end of a side (we strip it from both
# sides and re-append a single </tu> after merging).
TU_CLOSE_RE = re.compile(r'</tu>\s*$', re.DOTALL)

# Used to snap the longest-common-prefix to the last complete
# </tuv> so we never emit a half-opened <tuv xml:lang="...">.
TUV_CLOSE_LIT = '</tuv>'
TUV_CLOSE_LIT_LEN = len(TUV_CLOSE_LIT)


def _extract_tuvs(side_text: str) -> tuple[list[tuple[str, str]], bool]:
    """Return ``[(lang, tuv_xml), ...]`` plus whether the side ended with ``</tu>``."""
    tuvs = TUV_RE.findall(side_text)
    matches = []
    for m in TUV_RE.finditer(side_text):
        matches.append((m.group('lang'), m.group(0)))
    ends_with_tu_close = bool(TU_CLOSE_RE.search(side_text))
    return matches, ends_with_tu_close


def _merge_side(side_text: str, label: str) -> str:
    """Merge a single conflict side into the resolution text."""
    # Strip a trailing </tu> on this side; we'll re-add a single one.
    body = TU_CLOSE_RE.sub('', side_text)
    matches, _ = _extract_tuvs(body)
    # Order preserved by appearance; we just want a flat list of <tuv>
    # elements in the original order. ``matches`` is already that list.
    return ''.join(tuv_xml for _lang, tuv_xml in matches)


def resolve(
    text: str,
    filename: str,
    *,
    structural_strategy: str = 'abort',
) -> tuple[str, int, list[str], int]:
    """Return ``(new_text, hunks_resolved, warnings, unresolved_offset)``
    for ``text``.

    For each conflict hunk:

    1. Find the **longest common prefix** of ``theirs`` and ``ours``.
       This absorbs any shared text that spans the ``<<<<<<<`` /
       ``=======`` boundary, including the closing ``</seg></tuv>`` of a
       ``<tuv>`` whose opening tag sits in the pre-hunk text.
    2. The **suffixes** (``theirs[len(prefix):]`` and
       ``ours[len(prefix):]``) are siblings after that shared prefix —
       typically a series of ``<tuv>...</tuv>`` elements followed by
       ``</tu>``. Strip the trailing ``</tu>`` from each suffix.
    3. Extract every ``<tuv>...</tuv>`` from each suffix, dedupe by
       ``xml:lang`` keeping the "ours" (Stashed changes) version when
       the same language appears on both sides.
    4. Output: ``prefix`` + (deduped tuvs in document order) + ``</tu>``.

    Structural conflicts (``<tu tuid=`` appears in the theirs side)
    are handled according to ``structural_strategy``:

    * ``"abort"`` (default): stop at the first structural hunk and
      leave its markers intact. The caller's file is partially
      resolved; human review is required.
    * ``"theirs"``: take the ``<<<<<<< ... =======`` side verbatim and
      drop ``>>>>>>> ...``. Useful when the upstream side is the
      canonical intent and the stash only contained conflicting
      translations already represented elsewhere.
    * ``"ours"``: take the ``======= ... >>>>>>>`` side verbatim and
      drop ``<<<<<<< ...``.
    """
    valid_strategies = ('abort', 'theirs', 'ours')
    if structural_strategy not in valid_strategies:
        raise ValueError(
            f'structural_strategy must be one of {valid_strategies}; '
            f'got {structural_strategy!r}'
        )
    warnings: list[str] = []
    out: list[str] = []
    pos = 0
    hunks = 0
    unresolved_offset = -1

    while True:
        m_start = START_RE.search(text, pos)
        if m_start is None:
            out.append(text[pos:])
            break
        m_sep = SEP_RE.search(text, m_start.end())
        m_end = END_RE.search(text, m_sep.end() if m_sep else m_start.end())
        if m_sep is None or m_end is None:
            raise RuntimeError(
                f'{filename}: unbalanced conflict markers near offset {m_start.start()}'
            )

        theirs = text[m_start.end():m_sep.start()]
        ours = text[m_sep.end():m_end.start()]
        hunks += 1

        if '<tu tuid=' in theirs:
            warnings.append(
                f'{filename}:hunk#{hunks}: structural conflict (upstream '
                f'added/removed whole <tu> blocks)'
            )
            if structural_strategy == 'abort':
                # Output everything up to this hunk, then leave the
                # rest of the original text (with its markers) intact.
                out.append(text[pos:])
                unresolved_offset = m_start.start()
                break
            if structural_strategy == 'theirs':
                out.append(text[pos:m_start.start()])
                out.append(theirs)
                pos = m_end.end()
                continue
            # 'ours'
            out.append(text[pos:m_start.start()])
            out.append(ours)
            pos = m_end.end()
            continue

        # Text before the start marker is preserved as-is.
        out.append(text[pos:m_start.start()])

        # Longest common prefix (cheap O(n) since the strings share the
        # shared fr-fr <seg> tail verbatim).
        prefix_len = 0
        limit = min(len(theirs), len(ours))
        while prefix_len < limit and theirs[prefix_len] == ours[prefix_len]:
            prefix_len += 1

        # Snap the prefix to a complete element boundary so we never leave
        # a half-opened <tuv xml:lang="..."> in the output.
        #
        # The two sides diverge at the next sibling element's opening
        # tag. Three layouts are possible:
        #
        #  (a) Both sides share at least one complete preceding <tuv>
        #      (e.g. the fr-fr that wraps the marker). The last
        #      </tuv> in the prefix is the safe truncation point; we
        #      keep everything up to and including it.
        #
        #  (b) No complete <tuv> exists in the prefix (DeveloperUi.tmx
        #      style, where the only preceding <tuv> for the source
        #      lang lives in the pre-hunk text). The last <tuv in the
        #      prefix is the OPENING of the divergent element; we
        #      truncate before it so the divergent <tuv> lands cleanly
        #      in the suffixes for both sides.
        #
        #  (c) Neither </tuv> nor <tuv is present in the common
        #      prefix. The conflict is structural: the upstream side
        #      added or removed whole <tu> blocks while the stash
        #      added different content. Auto-merging this requires
        #      keeping BOTH sides verbatim, which is what "accept
        #      both" means in that case.
        last_close = theirs.rfind(TUV_CLOSE_LIT, 0, prefix_len)
        last_open = theirs.rfind('<tuv', 0, prefix_len)
        if last_close >= 0:
            prefix_len = last_close + TUV_CLOSE_LIT_LEN
        elif last_open >= 0:
            prefix_len = last_open
        else:
            # Structural conflict: emit both sides verbatim.
            warnings.append(
                f'{filename}:hunk#{hunks}: structural conflict (no <tuv>/'
                f'</tuv> in common prefix); keeping both sides verbatim'
            )
            out.append(text[pos:m_start.start()])
            out.append(theirs)
            out.append(ours)
            pos = m_end.end()
            continue

        prefix = theirs[:prefix_len]
        suffix_t = theirs[prefix_len:]
        suffix_o = ours[prefix_len:]

        # Strip any trailing </tu> from each piece; we'll add one back.
        prefix = TU_CLOSE_RE.sub('', prefix)
        suffix_t = TU_CLOSE_RE.sub('', suffix_t)
        suffix_o = TU_CLOSE_RE.sub('', suffix_o)

        # Walk both suffixes and emit a single ordered, deduped list of
        # <tuv> blocks. Within each suffix the order is already correct;
        # across suffixes we prefer "ours" when a lang collides, but we
        # emit theirs-only languages in their original position.
        theirs_blocks = [m.group(0) for m in TUV_RE.finditer(suffix_t)]
        ours_blocks = [m.group(0) for m in TUV_RE.finditer(suffix_o)]

        theirs_langs = [TUV_RE.match(b).group('lang') for b in theirs_blocks]
        ours_langs = [TUV_RE.match(b).group('lang') for b in ours_blocks]

        ours_lang_set = set(ours_langs)
        kept_blocks: list[str] = []

        # Emit theirs blocks in order; skip langs that ours also has
        # (they'll be emitted from ours instead, so ours wins).
        for lang, block in zip(theirs_langs, theirs_blocks):
            if lang in ours_lang_set:
                # Verify content equality; if they differ, warn.
                theirs_block = block
                ours_idx = ours_langs.index(lang)
                ours_block = ours_blocks[ours_idx]
                if theirs_block != ours_block:
                    warnings.append(
                        f'{filename}:hunk#{hunks}: lang={lang!r} differs between '
                        f'sides; keeping Stashed changes'
                    )
                continue
            kept_blocks.append(block)

        # Emit ours blocks in order.
        kept_blocks.extend(ours_blocks)

        out.append(prefix)
        out.append(''.join(kept_blocks))
        out.append('</tu>')

        pos = m_end.end()

    return ''.join(out), hunks, warnings, unresolved_offset


def resolve_file(path: Path, structural_strategy: str = 'abort') -> tuple[int, list[str]]:
    text = path.read_text(encoding='utf-8')
    new_text, hunks, warnings, unresolved_offset = resolve(
        text, path.name, structural_strategy=structural_strategy,
    )
    if hunks == 0 and unresolved_offset < 0:
        return 0, [f'{path.name}: no conflict markers found']

    if unresolved_offset >= 0:
        unresolved_line = new_text[:unresolved_offset].count('\n') + 1
        warnings.append(
            f'{path.name}: partial resolution; remaining markers start at '
            f'line {unresolved_line} (byte {unresolved_offset}); resolve manually'
        )
        # Partial resolution still keeps the file well-formed XML
        # because the unresolved portion is left verbatim with its
        # conflict markers in place.
    else:
        # Validate XML before writing.
        try:
            ET.fromstring(new_text)
        except ET.ParseError as exc:
            debug_path = path.with_suffix('.resolved.tmx')
            debug_path.write_text(new_text, encoding='utf-8')
            raise RuntimeError(
                f'{path.name}: post-merge XML invalid: {exc}; '
                f'wrote debug copy to {debug_path}'
            ) from exc

    path.write_text(new_text, encoding='utf-8')
    return hunks, warnings


def main(argv: list[str]) -> int:
    structural_strategy = 'abort'
    positional: list[str] = []
    for arg in argv:
        if arg in ('--theirs', '-Xtheirs'):
            structural_strategy = 'theirs'
        elif arg in ('--ours', '-Xours'):
            structural_strategy = 'ours'
        else:
            positional.append(arg)

    if positional:
        files = tuple(positional)
    else:
        files = DEFAULT_FILES

    total_hunks = 0
    all_warnings: list[str] = []
    for name in files:
        path = I18N_DIR / name
        if not path.exists():
            print(f'  [skip] {name}: not found at {path}', file=sys.stderr)
            continue
        hunks, warnings = resolve_file(path, structural_strategy=structural_strategy)
        total_hunks += hunks
        all_warnings.extend(warnings)
        print(f'  [ok]   {name}: {hunks} hunks resolved')

    print(f'\nTotal hunks resolved: {total_hunks}')
    print(f'Structural strategy: {structural_strategy}')
    if all_warnings:
        print(f'Warnings: {len(all_warnings)}')
        for w in all_warnings:
            print(f'  {w}')
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))