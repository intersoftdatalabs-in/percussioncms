/** Payload sent to the submission service. */
interface CorrectionSubmission {
    /** Visible text at the time the popover opened. */
    currentText: string;
    /** User-proposed replacement for visible text. */
    proposedText: string;
    /** Current aria-label when present; null if absent. */
    currentAriaLabel: string | null;
    /** User-proposed aria-label; null if not applicable. */
    proposedAriaLabel: string | null;
    /** Raw aria-labelledby attribute when present. */
    ariaLabelledby: string | null;
    /** Optional title attribute snapshot. */
    currentTitle: string | null;
    /**
     * Catalog key when resolved.
     * Convention (docs): app.component.screen.id
     */
    messageId: string | null;
    /** Optional rationale. */
    notes: string;
    /** User contact email (required for submit). Used for reply-to on approve/reject. */
    email: string;
    /** Content locale the correction applies to (BCP 47). */
    locale: string;
    source: {
        tagName: string;
        matchReason: string;
        elementId?: string;
        pageUrl: string;
    };
    /** Client timestamp (ISO-8601). */
    submittedAt: string;
}
interface SubmissionClient {
    submit(payload: CorrectionSubmission): Promise<void>;
}
type LocaleValue = string | (() => string | undefined);
interface InitOptions {
    /**
     * Content locale being corrected (BCP 47).
     * String or getter re-read when popover opens.
     * Required for a successful submit.
     */
    locale?: LocaleValue;
    /**
     * Locale for library chrome (popover labels, errors, trigger a11y name).
     * Default: content locale if set, else navigator.language, else 'en'.
     */
    uiLocale?: LocaleValue;
    /** Static default email. */
    userEmail?: string;
    /** Dynamic email; wins over userEmail when both set. */
    getUserEmail?: () => string | undefined;
    /**
     * Host attribute name that holds the message / i18n key.
     * Default: 'data-i18n-key'.
     * Set to null to disable attribute-based lookup and attr-based scanning.
     */
    messageIdAttr?: string | null;
    /**
     * When true (default) and {@link messageIdAttr} is set, include
     * `[messageIdAttr]` in the scan selector so keyed nodes (headings,
     * spans, etc.) get triggers without also adding {@link targetClass}.
     */
    scanMessageIdAttr?: boolean;
    /**
     * Optional resolver when attribute is missing or for computed keys.
     * Used only when the attribute is absent.
     * Prefer {@link createTrackedMessage} for host apps that already have
     * a `message(key) → string` helper and want zero template changes.
     */
    getMessageId?: (el: Element) => string | undefined;
    /**
     * Walk ancestors for messageIdAttr up to root. Default: true.
     */
    messageIdAncestorWalk?: boolean;
    /**
     * CSS class that marks additional translatable nodes.
     * Default: 'mkd-lang-target'
     */
    targetClass?: string;
    /**
     * When true (default), also scan common chrome hosts that are not
     * buttons/labels/links: `[role="tab"]`, `legend`, `th`, `h1`–`h6`.
     * Disable if those surfaces are too noisy for your app.
     */
    includeChromeSelectors?: boolean;
    /** Extra CSS selectors merged into the default set. */
    includeSelectors?: string[];
    /** Subtree root to scan. Default: document.documentElement */
    root?: ParentNode;
    /**
     * Submission backend. Default:
     * - {@link HttpSubmissionClient} when {@link postUrl} is set
     * - {@link NoopSubmissionClient} otherwise
     *
     * Explicit `client` always wins over `postUrl`.
     */
    client?: SubmissionClient;
    /**
     * Host REST endpoint that accepts {@link CorrectionSubmission} as JSON.
     *
     * **Integration link for plugins:** e.g. Percussion React sets
     * `postUrl` to a server route that holds the GCM token and calls the
     * Java thin SDK → Rust `mkd-gcm` → NNTP. The browser never sees the PAT.
     *
     * Ignored when `client` is provided.
     */
    postUrl?: string;
    /**
     * Extra headers for {@link postUrl} posts (CSRF, etc.).
     * String map or getter re-read on each submit.
     */
    postHeaders?: HeadersInit | (() => HeadersInit | undefined);
    /**
     * fetch credentials for {@link postUrl}. Default `same-origin`.
     */
    postCredentials?: RequestCredentials;
    /**
     * If true (default), skip elements matching
     * [data-mkd-lang-ignore] or .mkd-lang-ignore
     */
    respectIgnore?: boolean;
    /** z-index for popover portal. Default: 10000 */
    zIndex?: number;
    /** Disable MutationObserver (scan only once). Default: false */
    once?: boolean;
    /** Log noop submissions to console.debug. Default: false */
    debug?: boolean;
    /**
     * Optional overrides for library chrome strings.
     * Keys match UiStringKey from the i18n catalog.
     */
    messages?: Partial<Record<string, string>>;
}
interface MkdLanguageHandle {
    /** Re-run scan immediately. */
    rescan(): void;
    /** Tear down all UI and observers. */
    destroy(): void;
    /** Update options without full destroy. */
    configure(options: Partial<InitOptions>): void;
}

/**
 * Start the client. Safe to call once per page; subsequent calls
 * reconfigure and rescan rather than stacking instances.
 */
declare function init(options?: InitOptions): MkdLanguageHandle;

/**
 * Default submission client — resolves successfully without network I/O.
 */
declare class NoopSubmissionClient implements SubmissionClient {
    private readonly debug;
    constructor(debug?: boolean);
    submit(payload: CorrectionSubmission): Promise<void>;
}

type HttpSubmissionOptions = {
    /**
     * Absolute or same-origin URL of the host REST endpoint that accepts
     * {@link CorrectionSubmission} as JSON (e.g. Percussion BFF).
     */
    postUrl: string;
    /**
     * Extra headers (e.g. CSRF). `Content-Type: application/json` is always set.
     * String or getter re-read on each submit.
     */
    headers?: HeadersInit | (() => HeadersInit | undefined);
    /**
     * fetch credentials mode. Default `same-origin` so session cookies work
     * for same-site host endpoints without forcing cross-site cookies.
     */
    credentials?: RequestCredentials;
    /** Optional fetch implementation (tests). Default: global fetch. */
    fetchImpl?: typeof fetch;
};
/**
 * POSTs the correction JSON body to a host URL.
 *
 * Host (Percussion, etc.) validates the session, attaches the server-side
 * GCM token, and calls the Java (or other) thin SDK → Rust mkd-gcm → NNTP.
 * The browser never holds the GCM PAT.
 */
declare class HttpSubmissionClient implements SubmissionClient {
    private readonly postUrl;
    private readonly headers?;
    private readonly credentials;
    private readonly fetchImpl;
    constructor(options: HttpSubmissionOptions);
    submit(payload: CorrectionSubmission): Promise<void>;
}

type TrackedMessageResolve = (key: string, args?: unknown[]) => string;
type TrackedMessage = {
    /**
     * Drop-in wrapper: resolve display text and remember key ↔ text for
     * later {@link getMessageId} lookups during scan / popover open.
     */
    message: (key: string, args?: unknown[]) => string;
    /**
     * Pass to {@code init({ getMessageId })} so elements that already show
     * tracked strings get a catalog key without DOM attributes.
     */
    getMessageId: (el: Element) => string | undefined;
    /** Drop all remembered mappings (e.g. tests or locale hard-reset). */
    clear: () => void;
    /** Number of distinct normalized display strings currently tracked. */
    size: () => number;
};
/**
 * Normalize display text the same way the scanner extracts visible text.
 */
declare function normalizeTrackedText(text: string): string;
/**
 * Lowest-friction host integration for apps that already have
 * {@code message(key) → string} (or equivalent).
 *
 * Wrap once at the i18n boundary; keep existing call sites unchanged:
 *
 * @example
 * ```ts
 * const tracked = createTrackedMessage((key, args) => I18N.message(key, args));
 * export const message = tracked.message;
 *
 * init({
 *   locale: () => session.locale,
 *   getMessageId: tracked.getMessageId,
 *   // optional: still emit attrs where you want explicit keys
 * });
 * ```
 *
 * Collision policy: if two keys resolve to the same normalized display
 * string, the last registration wins. Prefer unique chrome copy, or set
 * {@code data-i18n-key} on the ambiguous host.
 */
declare function createTrackedMessage(resolve: TrackedMessageResolve): TrackedMessage;

/** Default host attribute for catalog keys (matches {@link InitOptions.messageIdAttr}). */
declare const MESSAGE_ID_ATTR: "data-i18n-key";
/** Default class for extra scan targets (matches {@link InitOptions.targetClass}). */
declare const TARGET_CLASS: "mkd-lang-target";
type MessageIdPropsOptions = {
    /**
     * Attribute name. Default {@link MESSAGE_ID_ATTR}.
     * Pass the same value you give {@code init({ messageIdAttr })}.
     */
    attr?: string;
    /**
     * When true, also add {@link TARGET_CLASS} (or {@link className} merge).
     * Usually unnecessary when {@code scanMessageIdAttr} is enabled (default).
     */
    markTarget?: boolean;
    /** Existing className to preserve / merge. */
    className?: string;
    /** Override target class when {@link markTarget} is true. */
    targetClass?: string;
};
type MessageIdProps = {
    [key: string]: string | undefined;
    className?: string;
};
/**
 * Framework-agnostic prop bag for hosts that already render a catalog key.
 *
 * Prefer {@link createTrackedMessage} when you can wrap the app's
 * {@code message(key)} helper once — no per-element props required.
 *
 * @example
 * ```tsx
 * <button {...messageIdProps(MSG.SAVE)}>{message(MSG.SAVE)}</button>
 * <h1 {...messageIdProps(MSG.TITLE)}>{message(MSG.TITLE)}</h1>
 * ```
 */
declare function messageIdProps(key: string, options?: MessageIdPropsOptions): MessageIdProps;

type UiStringKey = 'trigger.ariaLabel' | 'dialog.title' | 'tab.text' | 'tab.aria' | 'field.currentText' | 'field.proposedText' | 'field.messageId' | 'field.messageIdMissing' | 'field.locale' | 'field.notes' | 'field.email' | 'field.ariaLabelCurrent' | 'field.ariaLabelProposed' | 'field.ariaLabelledby' | 'field.ariaLabelledbyHelp' | 'field.title' | 'field.ariaEmpty' | 'action.submit' | 'action.cancel' | 'action.submitting' | 'error.localeRequired' | 'error.noChange' | 'error.proposedRequired' | 'error.emailRequired' | 'error.emailInvalid' | 'error.submitFailed' | 'status.success' | 'status.failure' | 'footer.privacy' | 'footer.accessibility' | 'footer.terms' | 'footer.copyright';
type UiCatalog = Record<UiStringKey, string>;
/**
 * Stable message ids for library chrome — hosts and crowdsource pipelines
 * can target these like any app key (`mkd.language.ui.*`).
 */
declare const UI_MESSAGE_IDS: Record<UiStringKey, string>;
/** Legal footer destinations (product-owned). */
declare const FOOTER_LINKS: {
    readonly privacy: "https://monkeyking.dev/privacy";
    readonly accessibility: "https://monkeyking.dev/accessibility";
    readonly terms: "https://monkeyking.dev/terms";
};

/** Register additional catalogs at runtime (tests / crowdsourced packs). */
declare function registerCatalog(locale: string, catalog: UiCatalog): void;
/** Locales with a built-in catalog (not including runtime registrations). */
declare function builtInLocales(): string[];

type ValidationResult = {
    ok: true;
} | {
    ok: false;
    errorKey: 'error.localeRequired' | 'error.noChange' | 'error.proposedRequired' | 'error.emailRequired' | 'error.emailInvalid';
};
/** Practical email check (not full RFC 5322). */
declare function isValidEmail(email: string): boolean;
declare function validateSubmission(payload: Pick<CorrectionSubmission, 'locale' | 'currentText' | 'proposedText' | 'currentAriaLabel' | 'proposedAriaLabel' | 'notes' | 'email'>): ValidationResult;

export { type CorrectionSubmission, FOOTER_LINKS, HttpSubmissionClient, type HttpSubmissionOptions, type InitOptions, type LocaleValue, MESSAGE_ID_ATTR, type MessageIdProps, type MessageIdPropsOptions, type MkdLanguageHandle, NoopSubmissionClient, type SubmissionClient, TARGET_CLASS, type TrackedMessage, type TrackedMessageResolve, UI_MESSAGE_IDS, type UiCatalog, type UiStringKey, builtInLocales, createTrackedMessage, init, isValidEmail, messageIdProps, normalizeTrackedText, registerCatalog, validateSubmission };
