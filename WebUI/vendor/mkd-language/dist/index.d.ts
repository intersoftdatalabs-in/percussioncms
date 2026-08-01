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
    /** User contact email (may be empty). */
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
     * Set to null to disable attribute-based lookup.
     */
    messageIdAttr?: string | null;
    /**
     * Optional resolver when attribute is missing or for computed keys.
     * Used only when the attribute is absent.
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
    /** Extra CSS selectors merged into the default set. */
    includeSelectors?: string[];
    /** Subtree root to scan. Default: document.documentElement */
    root?: ParentNode;
    /** Submission backend. Default: NoopSubmissionClient */
    client?: SubmissionClient;
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

type UiStringKey = 'trigger.ariaLabel' | 'dialog.title' | 'tab.text' | 'tab.aria' | 'field.currentText' | 'field.proposedText' | 'field.messageId' | 'field.messageIdMissing' | 'field.locale' | 'field.notes' | 'field.email' | 'field.ariaLabelCurrent' | 'field.ariaLabelProposed' | 'field.ariaLabelledby' | 'field.ariaLabelledbyHelp' | 'field.title' | 'field.ariaEmpty' | 'action.submit' | 'action.cancel' | 'action.submitting' | 'error.localeRequired' | 'error.noChange' | 'error.proposedRequired' | 'error.submitFailed' | 'status.success' | 'status.failure';
type UiCatalog = Record<UiStringKey, string>;

/** Register additional catalogs at runtime (tests / future locales). */
declare function registerCatalog(locale: string, catalog: UiCatalog): void;

export { type CorrectionSubmission, type InitOptions, type LocaleValue, type MkdLanguageHandle, NoopSubmissionClient, type SubmissionClient, type UiCatalog, type UiStringKey, init, registerCatalog };
