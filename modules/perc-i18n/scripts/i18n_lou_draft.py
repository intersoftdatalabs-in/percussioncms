#!/usr/bin/env python3
"""Draft Louisiana Creole (``lou`` / Kouri-Vini) translations for canonical TMX.

Google Translate does not support Louisiana Creole. This tool seeds a full
``xml:lang="lou"`` pass from ``en-us`` using:

1. A curated English → Kouri-Vini glossary for CMS UI labels
2. Multi-word phrase replacements
3. Word-level fallback for remaining tokens (preserves placeholders, brands)

Quality is **draft** — suitable for ship-locale matrix completeness and review
by a Kouri-Vini speaker; not a certified localization.

Usage (repo root)::

    python modules/perc-i18n/scripts/i18n_lou_draft.py
    python modules/perc-i18n/scripts/i18n_lou_draft.py --file DeveloperUi.tmx --dry-run
    python modules/perc-i18n/scripts/i18n_lou_draft.py --force   # rewrite existing lou

Does not call external APIs. Safe to run offline.
"""
from __future__ import annotations

import argparse
import html
import re
import sys
from pathlib import Path
from xml.sax.saxutils import escape as xml_escape

REPO_ROOT = Path(__file__).resolve().parent.parent.parent.parent
I18N_DIR = REPO_ROOT / "modules" / "perc-i18n" / "src" / "main" / "resources" / "i18n"
DEFAULT_FILES = ("CmsUi.tmx", "SystemResources.tmx", "DeveloperUi.tmx")
TARGET = "lou"
SOURCE = "en-us"
PLACEHOLDER_RE = re.compile(r"^\s*\{[0-9]+(,[0-9]+)*\}\s*$")

# Exact full-string glossary (case-sensitive match on en-us seg after strip).
# Kouri-Vini orthography is not fully standardized; forms lean practical / readable.
EXACT: dict[str, str] = {
    "Error": "Erè",
    "Cancel": "Anulé",
    "Status": "Status",
    "Delete": "Efase",
    "Site": "Sit",
    "Name": "Non",
    "Title": "Tit",
    "Close": "Fèmen",
    "Warning": "Avètisman",
    "Warning:": "Avètisman:",
    "Back": "An aryè",
    "Save": "Sové",
    "Search": "Chèche",
    "Edit": "Édité",
    "Actions": "Aksyon",
    "Type": "Tip",
    "Preview": "Prévi",
    "Content": "Kontni",
    "Page": "Paj",
    "Description": "Déskripsyon",
    "Description:": "Déskripsyon:",
    "Sites": "Sit-yé",
    "Design": "Désin",
    "Publish": "Pibliyé",
    "No": "Non",
    "Yes": "Wi",
    "Open": "Ouvri",
    "Roles": "Ròl-yé",
    "None": "Anyen",
    "Custom": "Pèsonalizé",
    "Select": "Chwazi",
    "Folder": "Dòsyé",
    "Add": "Ajouté",
    "Submit": "Soumèt",
    "Help": "Lèd",
    "Admin": "Admin",
    "Home": "Lakay",
    "Workflow": "Workflow",
    "All": "Tout",
    "Date": "Dat",
    "Published": "Pibliyé",
    "Revisions": "Révisyon-yé",
    "Asset": "Asset",
    "New Folder": "Nouvo dòsyé",
    "Name:": "Non:",
    "Security": "Sékirité",
    "Revision": "Révisyon",
    "Import": "Importé",
    "Password": "Mo d pas",
    "Login": "Koneksyon",
    "Apply": "Aplike",
    "Continue anyway": "Kontiné kanmèm",
    "Activate": "Aktivé",
    "User": "Itilizatè",
    "Refresh": "Rafraîchi",
    "Preview Page": "Prévi paj",
    "Permissions": "Pèrmisyon-yé",
    "Image": "Imaj",
    "Locale": "Lang",
    "Remove": "Wété",
    "Change Password": "Chanje mo d pas",
    "Finish": "Finí",
    "User Name": "Non itilizatè",
    "Dashboard": "Tablèt de bò",
    "Editor": "Édité",
    "Site Design": "Désin sit",
    "Show": "Montre",
    "Full": "Konplé",
    "Server": "Sèrvé",
    "Delete Server": "Efase sèrvé",
    "Operation": "Opérasyon",
    "Filename": "Non fiché",
    "Last Modified": "Dènyé chanjman",
    "OK": "OK",
    "Ok": "OK",
    "or": "oswa",
    "Or": "Oswa",
    "and": "épi",
    "And": "Épi",
    "Loading...": "Ka chajé...",
    "Loading": "Ka chajé",
    "Please wait...": "Tanpri tann...",
    "Success": "Siksé",
    "Failed": "Échouvé",
    "Failure": "Échèk",
    "Required": "Obligatwa",
    "Optional": "Opsyonèl",
    "Settings": "Réglaj",
    "Preferences": "Préférans",
    "Options": "Opsyon-yé",
    "Configuration": "Konfigirasyon",
    "Properties": "Propriété-yé",
    "Details": "Détay",
    "View": "Vwè",
    "Download": "Téléchajé",
    "Upload": "Téléversé",
    "Create": "Kréyé",
    "New": "Nouvo",
    "Copy": "Kopi",
    "Paste": "Kolé",
    "Cut": "Koupé",
    "Move": "Déplasé",
    "Rename": "Rénomé",
    "Update": "Mizajou",
    "Next": "Apré",
    "Previous": "Anvan",
    "First": "Prémyé",
    "Last": "Dènyé",
    "More": "Plis",
    "Less": "Mwens",
    "Clear": "Netwayé",
    "Reset": "Réinitialisé",
    "Filter": "Filtré",
    "Sort": "Triyé",
    "Find": "Jwenn",
    "Replace": "Ranplasé",
    "Print": "Enprimé",
    "Export": "Éksporté",
    "Enabled": "Aktivé",
    "Disabled": "Désaktivé",
    "Active": "Aktif",
    "Inactive": "Inaktif",
    "Online": "An liy",
    "Offline": "Hors liy",
    "True": "Vré",
    "False": "Fo",
    "On": "Asou",
    "Off": "Étenn",
    "Default": "Pa défò",
    "Advanced": "Avanse",
    "Basic": "Debaz",
    "General": "Jénéral",
    "Language": "Lang",
    "Timezone": "Fizé lè",
    "Time zone": "Fizé lè",
    "Email": "Imèl",
    "Address": "Adrès",
    "Phone": "Téléfon",
    "Comments": "Kòmantè-yé",
    "Comment": "Kòmantè",
    "Tags": "Tag-yé",
    "Tag": "Tag",
    "Category": "Katégori",
    "Categories": "Katégori-yé",
    "Template": "Modèl",
    "Templates": "Modèl-yé",
    "Widget": "Widget",
    "Widgets": "Widget-yé",
    "Theme": "Tèm",
    "Themes": "Tèm-yé",
    "Layout": "Mizajou",
    "Navigation": "Navigasyon",
    "Menu": "Menu",
    "Link": "Lyen",
    "Links": "Lyen-yé",
    "URL": "URL",
    "Path": "Chimen",
    "File": "Fiché",
    "Files": "Fiché-yé",
    "Folder": "Dòsyé",
    "Folders": "Dòsyé-yé",
    "Library": "Bibliyotèk",
    "Media": "Médya",
    "Calendar": "Kalandyé",
    "Schedule": "Orè",
    "Start": "Koumansé",
    "Stop": "Arrété",
    "Restart": "Rédémarré",
    "Test": "Tès",
    "Version": "Vèsyon",
    "History": "Listwa",
    "Log": "Jounal",
    "Logs": "Jounal-yé",
    "Message": "Mésaj",
    "Messages": "Mésaj-yé",
    "Notification": "Notifikasyon",
    "Notifications": "Notifikasyon-yé",
    "Profile": "Profil",
    "Account": "Kont",
    "Sign out": "Dékonèkté",
    "Sign in": "Konekté",
    "Log out": "Dékonèkté",
    "Log in": "Konekté",
    "Logout": "Dékonèksyon",
    "Username": "Non itilizatè",
    "Remember me": "Sonjé mwen",
    "Forgot password": "Bliyé mo d pas",
    "Confirm": "Konfirmé",
    "Confirm Password": "Konfirmé mo d pas",
    "New Password": "Nouvo mo d pas",
    "Old Password": "Vyé mo d pas",
    "Current Password": "Mo d pas aktwèl",
    "Are you sure?": "To sir?",
    "Are you sure": "To sir",
    "Please confirm": "Tanpri konfirmé",
    "Not found": "Pa jwenn",
    "Access denied": "Aksè réfuzé",
    "Unauthorized": "Pa otorizé",
    "Invalid": "Envalid",
    "Valid": "Valid",
    "Empty": "Vid",
    "Unknown": "Enkoni",
    "Other": "Lòt",
    "Owner": "Propriyétè",
    "Created": "Kréyé",
    "Modified": "Modifyé",
    "Updated": "Miz a jou",
    "Deleted": "Efasé",
    "Selected": "Chwazi",
    "Available": "Disponib",
    "Unavailable": "Pa disponib",
    "Public": "Piblik",
    "Private": "Privé",
    "Draft": "Brouillon",
    "Approved": "Aprouvé",
    "Pending": "An atant",
    "Rejected": "Réjeté",
    "Archived": "Archivé",
    "Check in": "Check-in",
    "Check out": "Check-out",
    "Check-in": "Check-in",
    "Check-out": "Check-out",
    "Workflow": "Workflow",
    "Transition": "Tranzisyon",
    "Community": "Kominité",
    "Communities": "Kominité-yé",
    "Percussion Community": "Kominité Percussion",
    "role:": "ròl:",
    "user:": "itilizatè:",
    "virtual:": "virtwèl:",
    "Error while loading roles.": "Erè pandan chajman ròl-yé.",
    "Redirect creation error": "Erè pandan kréyasyon redirect",
    "Widget Builder": "Konstriktè widget",
    "Site Design": "Désin sit",
}

# Longer phrase fragments (applied before word map; longest first).
PHRASES: list[tuple[str, str]] = [
    ("Change Password", "Chanje mo d pas"),
    ("User Name", "Non itilizatè"),
    ("Last Modified", "Dènyé chanjman"),
    ("New Folder", "Nouvo dòsyé"),
    ("Preview Page", "Prévi paj"),
    ("Continue anyway", "Kontiné kanmèm"),
    ("Delete Server", "Efase sèrvé"),
    ("Access denied", "Aksè réfuzé"),
    ("Please wait", "Tanpri tann"),
    ("Are you sure", "To sir"),
    ("Forgot password", "Bliyé mo d pas"),
    ("Confirm Password", "Konfirmé mo d pas"),
    ("New Password", "Nouvo mo d pas"),
    ("Old Password", "Vyé mo d pas"),
    ("Current Password", "Mo d pas aktwèl"),
    ("Sign out", "Dékonèkté"),
    ("Sign in", "Konekté"),
    ("Log out", "Dékonèkté"),
    ("Log in", "Konekté"),
    ("Check in", "Check-in"),
    ("Check out", "Check-out"),
    ("Time zone", "Fizé lè"),
    ("Error while loading roles", "Erè pandan chajman ròl-yé"),
    ("Redirect creation error", "Erè pandan kréyasyon redirect"),
    ("Percussion Community", "Kominité Percussion"),
    ("Site Design", "Désin sit"),
    ("Widget Builder", "Konstriktè widget"),
]

# Whole-word replacements (case-sensitive keys; we also try Title and lower variants).
WORDS: dict[str, str] = {
    "Error": "Erè",
    "error": "erè",
    "Errors": "Erè-yé",
    "errors": "erè-yé",
    "Cancel": "Anulé",
    "cancel": "anulé",
    "Delete": "Efase",
    "delete": "efase",
    "Deleted": "Efasé",
    "deleted": "efasé",
    "Save": "Sové",
    "save": "sové",
    "Saved": "Sové",
    "saved": "sové",
    "Search": "Chèche",
    "search": "chèche",
    "Edit": "Édité",
    "edit": "édité",
    "Close": "Fèmen",
    "close": "fèmen",
    "Open": "Ouvri",
    "open": "ouvri",
    "Add": "Ajouté",
    "add": "ajouté",
    "Remove": "Wété",
    "remove": "wété",
    "Create": "Kréyé",
    "create": "kréyé",
    "Update": "Mizajou",
    "update": "mizajou",
    "Updated": "Miz a jou",
    "updated": "miz a jou",
    "Name": "Non",
    "name": "non",
    "Title": "Tit",
    "title": "tit",
    "Page": "Paj",
    "page": "paj",
    "Pages": "Paj-yé",
    "pages": "paj-yé",
    "Site": "Sit",
    "site": "sit",
    "Sites": "Sit-yé",
    "sites": "sit-yé",
    "Folder": "Dòsyé",
    "folder": "dòsyé",
    "Folders": "Dòsyé-yé",
    "folders": "dòsyé-yé",
    "File": "Fiché",
    "file": "fiché",
    "Files": "Fiché-yé",
    "files": "fiché-yé",
    "User": "Itilizatè",
    "user": "itilizatè",
    "Users": "Itilizatè-yé",
    "users": "itilizatè-yé",
    "Password": "Mo d pas",
    "password": "mo d pas",
    "Login": "Koneksyon",
    "login": "koneksyon",
    "Logout": "Dékonèksyon",
    "logout": "dékonèksyon",
    "Help": "Lèd",
    "help": "lèd",
    "Home": "Lakay",
    "home": "lakay",
    "Content": "Kontni",
    "content": "kontni",
    "Publish": "Pibliyé",
    "publish": "pibliyé",
    "Published": "Pibliyé",
    "published": "pibliyé",
    "Preview": "Prévi",
    "preview": "prévi",
    "Select": "Chwazi",
    "select": "chwazi",
    "Selected": "Chwazi",
    "selected": "chwazi",
    "All": "Tout",
    "all": "tout",
    "None": "Anyen",
    "none": "anyen",
    "Yes": "Wi",
    "yes": "wi",
    "No": "Non",
    "Warning": "Avètisman",
    "warning": "avètisman",
    "Success": "Siksé",
    "success": "siksé",
    "Failed": "Échouvé",
    "failed": "échouvé",
    "Failure": "Échèk",
    "failure": "échèk",
    "Required": "Obligatwa",
    "required": "obligatwa",
    "Invalid": "Envalid",
    "invalid": "envalid",
    "Valid": "Valid",
    "valid": "valid",
    "Loading": "Ka chajé",
    "loading": "ka chajé",
    "Please": "Tanpri",
    "please": "tanpri",
    "and": "épi",
    "or": "oswa",
    "to": "a",
    "from": "dépi",
    "for": "pou",
    "with": "épi",
    "without": "san",
    "of": "a",
    "in": "nan",
    "on": "asou",
    "by": "pa",
    "is": "sé",
    "are": "sé",
    "was": "té",
    "were": "té",
    "be": "été",
    "been": "été",
    "not": "pa",
    "Not": "Pa",
    "this": "sa-a",
    "This": "Sa-a",
    "that": "sa",
    "That": "Sa",
    "the": "la",
    "The": "La",
    "a": "yon",
    "A": "Yon",
    "an": "yon",
    "An": "Yon",
    "your": "to",
    "Your": "To",
    "you": "to",
    "You": "To",
    "my": "mo",
    "My": "Mo",
    "I": "Mo",
    "we": "nou",
    "We": "Nou",
    "they": "yé",
    "They": "Yé",
    "he": "li",
    "He": "Li",
    "she": "li",
    "She": "Li",
    "it": "li",
    "It": "Li",
    "New": "Nouvo",
    "new": "nouvo",
    "Old": "Vyé",
    "old": "vyé",
    "Current": "Aktwèl",
    "current": "aktwèl",
    "Date": "Dat",
    "date": "dat",
    "Time": "Lè",
    "time": "lè",
    "Server": "Sèrvé",
    "server": "sèrvé",
    "Status": "Status",
    "status": "status",
    "Type": "Tip",
    "type": "tip",
    "Description": "Déskripsyon",
    "description": "déskripsyon",
    "Language": "Lang",
    "language": "lang",
    "Email": "Imèl",
    "email": "imèl",
    "Image": "Imaj",
    "image": "imaj",
    "Link": "Lyen",
    "link": "lyen",
    "Links": "Lyen-yé",
    "links": "lyen-yé",
    "Path": "Chimen",
    "path": "chimen",
    "Version": "Vèsyon",
    "version": "vèsyon",
    "Role": "Ròl",
    "role": "ròl",
    "Roles": "Ròl-yé",
    "roles": "ròl-yé",
    "Permission": "Pèrmisyon",
    "permission": "pèrmisyon",
    "Permissions": "Pèrmisyon-yé",
    "permissions": "pèrmisyon-yé",
    "Security": "Sékirité",
    "security": "sékirité",
    "Settings": "Réglaj",
    "settings": "réglaj",
    "Options": "Opsyon-yé",
    "options": "opsyon-yé",
    "Actions": "Aksyon",
    "actions": "aksyon",
    "View": "Vwè",
    "view": "vwè",
    "Show": "Montre",
    "show": "montre",
    "Hide": "Kaché",
    "hide": "kaché",
    "Refresh": "Rafraîchi",
    "refresh": "rafraîchi",
    "Reset": "Réinitialisé",
    "reset": "réinitialisé",
    "Clear": "Netwayé",
    "clear": "netwayé",
    "Apply": "Aplike",
    "apply": "aplike",
    "Submit": "Soumèt",
    "submit": "soumèt",
    "Confirm": "Konfirmé",
    "confirm": "konfirmé",
    "Continue": "Kontiné",
    "continue": "kontiné",
    "Finish": "Finí",
    "finish": "finí",
    "Next": "Apré",
    "next": "apré",
    "Previous": "Anvan",
    "previous": "anvan",
    "Back": "An aryè",
    "back": "an aryè",
    "Upload": "Téléversé",
    "upload": "téléversé",
    "Download": "Téléchajé",
    "download": "téléchajé",
    "Import": "Importé",
    "import": "importé",
    "Export": "Éksporté",
    "export": "éksporté",
    "Copy": "Kopi",
    "copy": "kopi",
    "Move": "Déplasé",
    "move": "déplasé",
    "Rename": "Rénomé",
    "rename": "rénomé",
    "Enabled": "Aktivé",
    "enabled": "aktivé",
    "Disabled": "Désaktivé",
    "disabled": "désaktivé",
    "Active": "Aktif",
    "active": "aktif",
    "Default": "Pa défò",
    "default": "pa défò",
    "Custom": "Pèsonalizé",
    "custom": "pèsonalizé",
    "Empty": "Vid",
    "empty": "vid",
    "Unknown": "Enkoni",
    "unknown": "enkoni",
    "Available": "Disponib",
    "available": "disponib",
    "Public": "Piblik",
    "public": "piblik",
    "Private": "Privé",
    "private": "privé",
    "Draft": "Brouillon",
    "draft": "brouillon",
    "Approved": "Aprouvé",
    "approved": "aprouvé",
    "Pending": "An atant",
    "pending": "an atant",
    "Template": "Modèl",
    "template": "modèl",
    "Templates": "Modèl-yé",
    "templates": "modèl-yé",
    "Widget": "Widget",
    "widget": "widget",
    "Theme": "Tèm",
    "theme": "tèm",
    "Community": "Kominité",
    "community": "kominité",
    "Message": "Mésaj",
    "message": "mésaj",
    "Messages": "Mésaj-yé",
    "messages": "mésaj-yé",
    "Comment": "Kòmantè",
    "comment": "kòmantè",
    "Comments": "Kòmantè-yé",
    "comments": "kòmantè-yé",
    "while": "pandan",
    "While": "Pandan",
    "loading": "chajman",
    "Loading": "Chajman",
}

# Tokens left as-is (product / technical).
KEEP = {
    "Percussion",
    "CMS",
    "DTS",
    "URL",
    "HTML",
    "CSS",
    "JSON",
    "XML",
    "API",
    "REST",
    "ID",
    "UUID",
    "OK",
    "HTTP",
    "HTTPS",
    "PDF",
    "SQL",
    "SSO",
    "LDAP",
    "SAML",
    "OAuth",
    "GA4",
    "WCAG",
    "JDK",
    "Java",
    "Jetty",
    "Tomcat",
    "MySQL",
    "Oracle",
    "H2",
    "Solr",
    "Workflow",  # often left as product term
}

TOKEN_RE = re.compile(
    r"(\{[0-9]+(?:,[0-9]+)*\}|"  # placeholders
    r"%[sd]|"  # printf
    r"https?://\S+|"
    r"[A-Za-z][A-Za-z0-9_.@/-]*|"
    r"[^A-Za-z]+)"
)


def translate_text(en: str) -> str:
    """Produce a draft Louisiana Creole string from English."""
    if PLACEHOLDER_RE.match(en):
        return en
    stripped = en.strip()
    if not stripped:
        return en
    if stripped in EXACT:
        return _preserve_outer_ws(en, EXACT[stripped])

    # Case-insensitive exact for short labels
    for k, v in EXACT.items():
        if stripped.lower() == k.lower() and len(stripped) < 48:
            # preserve capitalization style lightly
            if stripped.isupper():
                return _preserve_outer_ws(en, v.upper())
            if stripped[0].isupper():
                return _preserve_outer_ws(en, v[0].upper() + v[1:] if v else v)
            return _preserve_outer_ws(en, v)

    out = en
    # Phrase replace (case-sensitive first, then ignore case for short phrases)
    for src, dst in sorted(PHRASES, key=lambda x: -len(x[0])):
        if src in out:
            out = out.replace(src, dst)
        else:
            # case-insensitive for whole phrase
            pat = re.compile(re.escape(src), re.IGNORECASE)
            if pat.search(out):
                out = pat.sub(dst, out)

    def repl_token(m: re.Match[str]) -> str:
        tok = m.group(0)
        if not tok or not tok[0].isalpha():
            return tok
        if tok in KEEP or tok.upper() in KEEP:
            return tok
        if tok in WORDS:
            return WORDS[tok]
        # brand-like CamelCase / ALLCAPS leave
        if tok.isupper() and len(tok) > 1:
            return tok
        if any(c.isupper() for c in tok[1:]) and tok[0].isupper():
            return tok  # CamelCase
        # try lowercase map
        low = tok.lower()
        if low in WORDS:
            w = WORDS[low]
            if tok[0].isupper():
                return w[0].upper() + w[1:] if w else w
            return w
        return tok

    out = TOKEN_RE.sub(repl_token, out)
    return out


def _preserve_outer_ws(original: str, core: str) -> str:
    lead = original[: len(original) - len(original.lstrip())]
    trail = original[len(original.rstrip()) :]
    return f"{lead}{core}{trail}"


class TmxFile:
    def __init__(self, path: Path):
        self.path = path
        self.text = path.read_text(encoding="utf-8")

    def list_en(self) -> list[tuple[str, str]]:
        """Return [(tuid, en_seg)] for all TUs with en-us."""
        root_items: list[tuple[str, str]] = []
        for m in re.finditer(r'<tu\s+tuid="([^"]+)"[^>]*>(.*?)</tu>', self.text, re.DOTALL):
            tuid = html.unescape(m.group(1))
            body = m.group(2)
            en_m = re.search(
                rf'<tuv\s+xml:lang="{SOURCE}"><seg>(.*?)</seg></tuv>', body, re.DOTALL
            )
            if en_m:
                root_items.append((tuid, html.unescape(en_m.group(1))))
        return root_items

    def inject(self, translations: dict[str, str], force: bool) -> int:
        inserted = 0
        out: list[str] = []
        pos = 0
        tu_pattern = re.compile(r'<tu\s+tuid="([^"]+)"[^>]*>.*?</tu>', re.DOTALL)
        for m in tu_pattern.finditer(self.text):
            tuid = html.unescape(m.group(1))
            if tuid not in translations:
                continue
            tu_text = m.group(0)
            has_lou = re.search(rf'<tuv\s+xml:lang="{TARGET}"', tu_text)
            if has_lou and not force:
                continue
            seg = xml_escape(translations[tuid])
            new_tuv = f'<tuv xml:lang="{TARGET}"><seg>{seg}</seg></tuv>'
            if has_lou and force:
                # replace existing lou tuv
                tu_text = re.sub(
                    rf'<tuv\s+xml:lang="{TARGET}"><seg>.*?</seg></tuv>',
                    new_tuv,
                    tu_text,
                    count=1,
                    flags=re.DOTALL,
                )
                replacement = tu_text
            else:
                replacement = tu_text[: -len("</tu>")] + new_tuv + "</tu>"
            out.append(self.text[pos : m.start()])
            out.append(replacement)
            pos = m.end()
            inserted += 1
        out.append(self.text[pos:])
        if inserted:
            self.text = "".join(out)
        return inserted

    def commit(self) -> None:
        self.path.write_text(self.text, encoding="utf-8")


def main(argv: list[str] | None = None) -> int:
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--file", action="append", dest="files")
    p.add_argument("--dry-run", action="store_true")
    p.add_argument("--force", action="store_true", help="Replace existing lou TUVS")
    p.add_argument("--limit", type=int, default=0)
    args = p.parse_args(argv)

    files = args.files or list(DEFAULT_FILES)
    total = 0
    for name in files:
        path = I18N_DIR / name
        if not path.is_file():
            print(f"SKIP missing {path}", file=sys.stderr)
            continue
        tmx = TmxFile(path)
        items = tmx.list_en()
        translations: dict[str, str] = {}
        for tuid, en in items:
            if args.limit and len(translations) >= args.limit:
                break
            translations[tuid] = translate_text(en)
        if args.dry_run:
            print(f"{name}: would write {len(translations)} lou TUVS (sample):")
            for i, (tuid, en) in enumerate(items[:8]):
                print(f"  [{en!r}] -> [{translations.get(tuid)!r}]")
            continue
        n = tmx.inject(translations, force=args.force)
        tmx.commit()
        print(f"{name}: inserted/updated {n} lou TUVS")
        total += n
    print(f"Done. total={total}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
