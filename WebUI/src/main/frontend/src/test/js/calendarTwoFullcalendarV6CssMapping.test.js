/**
 * CalendarTwo FullCalendar v6 CSS class-delta mapping (issue #6804, slice 2a;
 * parent plan #6687, audit #5627).
 *
 * <p>Slice 1 (#6792) locked the widget-consumed 2.9.1 surface that the
 * migration must preserve. This spike maps the <em>styling</em> half of that
 * surface against the recommended successor line — {@code fullcalendar@6.1.15}
 * Standard Bundle (MIT), installed under the {@code fullcalendar-v6} npm
 * alias so the EOL {@code fullcalendar@2.9.1} pin (payload + parity guard)
 * stays untouched — and records exactly which snippet CSS hooks die under v6
 * so the slice-3 migration knows what to retarget. No payload, snippet, or
 * manifest <em>behavior</em> changes here: the v6 bundle is test-fixture
 * only until the migration slice.
 *
 * <p>Mapping (v2 shipped → v6.1.15), verified by the assertions below against
 * the real v6 dist in jsdom:
 *
 * <table>
 * <caption>v6 fc-* CSS class deltas vs the snippet responsive overrides</caption>
 * <tr><th>Snippet / v2 hook</th><th>v2 mechanism</th><th>v6.1.15 reality</th>
 *     <th>Migration action (slice 3)</th></tr>
 * <tr><td>{@code .fc-left / .fc-center / .fc-right} responsive overrides
 *     ({@code max-width: 480px} stacking block)</td>
 *     <td>v2 toolbar sections</td><td>GONE — v6 toolbar is
 *     {@code .fc-header-toolbar.fc-toolbar} (flex,
 *     {@code justify-content: space-between}) with three <em>unstyled</em>
 *     {@code .fc-toolbar-chunk} children (the class name never appears in
 *     the injected CSS)</td>
 *     <td>Retarget the 480px override to {@code .fc-toolbar-chunk};
 *     v6 does not wrap the toolbar by itself</td></tr>
 * <tr><td>View-toggle buttons
 *     ({@code fc-button fc-state-default fc-corner-left/right})</td>
 *     <td>v2 theme CSS ({@code fullcalendar.css} defines
 *     {@code .fc-state-default} / {@code .fc-corner-*})</td>
 *     <td>v6 defines {@code .fc-button} / {@code .fc-button-primary} but
 *     NO {@code .fc-state-default} and NO {@code .fc-corner-*} (asserted
 *     against the injected CSSOM below)</td>
 *     <td>Carry a compat rule for the toggles or restyle them; they sit
 *     outside the calendar root so v6 never styles them</td></tr>
 * <tr><td>Month title ({@code h2})</td><td>v2 header {@code h2}</td>
 *     <td>{@code h2.fc-toolbar-title} — same element, tighter hook</td>
 *     <td>Carry over (class-qualified)</td></tr>
 * <tr><td>Event text ({@code .fc-event .fc-title})</td>
 *     <td>v2 event inner</td><td>v6 daygrid event text is
 *     {@code .fc-event-title}; {@code .fc-title} renders nowhere</td>
 *     <td>Update selectors / qTip2 call sites that target
 *     {@code .fc-title}</td></tr>
 * <tr><td>View wrapper ({@code .fc-view-container})</td><td>v2</td>
 *     <td>v6 uses {@code .fc-view-harness} &gt;
 *     {@code .fc-dayGridMonth-view.fc-view}</td>
 *     <td>Update any CSS/JS targeting {@code .fc-view-container}</td></tr>
 * <tr><td>{@code fullcalendar.print.css} ({@code media="print"} link)</td>
 *     <td>Separate file in the 2.9.1 dist</td><td>NOT SHIPPED — the v6
 *     bundle is JS-only (no {@code *.css} in the package) and its injected
 *     CSSOM carries zero {@code @media print} rules</td>
 *     <td>Slice 2b authors the print equivalent (recorded, not resolved
 *     here)</td></tr>
 * <tr><td>Style delivery ({@code <link>} CSS files)</td>
 *     <td>Static CSS links</td><td>JS-injected
 *     {@code <style data-fullcalendar>} via CSSOM {@code insertRule}
 *     (element text stays empty); honors a {@code meta[name=csp-nonce]}
 *     nonce</td>
 *     <td>CSP interplay (nonce-aware injection) feeds slice 3b; no static
 *     print/toolbar stylesheet to link</td></tr>
 * </table>
 */
import { describe, expect, it } from "vitest";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const FRONTEND_ROOT = path.dirname(path.dirname(path.dirname(__dirname)));
const NODE_MODULES = path.join(FRONTEND_ROOT, "node_modules");
const V6_ROOT = path.join(NODE_MODULES, "fullcalendar-v6");

function loadV6Global() {
  // Slice-2a fixture only (mirrors the widget script order): the v6
  // Standard Bundle UMD global, evaluated the same way published pages
  // would load it via a plain <script> tag (no bundler).
  window.eval(
    fs.readFileSync(path.join(V6_ROOT, "index.global.min.js"), "utf8"),
  );
  return window.FullCalendar;
}

function renderV6Month(id) {
  document.body.insertAdjacentHTML("beforeend", `<div id="${id}"></div>`);
  const calendar = new window.FullCalendar.Calendar(
    document.getElementById(id),
    {
      initialView: "dayGridMonth",
      initialDate: "2026-11-01",
      headerToolbar: { left: "title", center: "", right: "today prev,next" },
      events: [{ title: "Delta Event", start: "2026-11-10" }],
    },
  );
  calendar.render();
  return calendar;
}

function collectCssFiles(dir, out = []) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) collectCssFiles(full, out);
    else if (entry.name.endsWith(".css")) out.push(full);
  }
  return out;
}

function injectedV6CssText() {
  const styleEl = document.head.querySelector("style[data-fullcalendar]");
  expect(
    styleEl,
    "v6 must inject its <style data-fullcalendar> element on render",
  ).not.toBeNull();
  const rules = Array.from(styleEl.sheet?.cssRules ?? []);
  expect(rules.length).toBeGreaterThan(0);
  return rules.map((rule) => rule.cssText ?? "").join("\n");
}

describe("calendarTwo FullCalendar v6 CSS class deltas (issue #6804 slice 2a)", () => {
  it("pins fullcalendar-v6 to 6.1.15 MIT without moving the 2.9.1 pin", () => {
    const installed = JSON.parse(
      fs.readFileSync(path.join(V6_ROOT, "package.json"), "utf8"),
    );
    expect(installed.version).toBe("6.1.15");
    expect(installed.license).toBe("MIT");
    const manifest = JSON.parse(
      fs.readFileSync(path.join(FRONTEND_ROOT, "package.json"), "utf8"),
    );
    expect(manifest.dependencies["fullcalendar-v6"]).toBe(
      "npm:fullcalendar@6.1.15",
    );
    // The EOL line stays exactly where slice 5 put it (payload + parity
    // guard owner): this slice only *adds* the v6 spike pin alongside it.
    expect(manifest.dependencies["fullcalendar"]).toBe("2.9.1");
  });

  it("exposes the v6 global entry point at 6.1.15", () => {
    const FullCalendar = loadV6Global();
    expect(typeof FullCalendar.Calendar).toBe("function");
    expect(FullCalendar.version).toBe("6.1.15");
  });

  it("renders the month grid and the event (visual-parity baseline)", () => {
    loadV6Global();
    const calendar = renderV6Month("v6-css-delta-grid");
    const root = document.getElementById("v6-css-delta-grid");
    // Month grid: fixed 6-week dayGridMonth for November 2026.
    const dayCells = root.querySelectorAll(".fc-daygrid-day");
    expect(dayCells.length).toBe(42);
    // The static source renders through the v6 event pipeline.
    const rendered = root.querySelectorAll(".fc-event .fc-event-title");
    expect(Array.from(rendered).map((node) => node.textContent)).toContain(
      "Delta Event",
    );
    // Month title keeps the v2 element shape (h2), class-qualified in v6.
    const title = root.querySelector("h2.fc-toolbar-title");
    expect(title?.textContent).toContain("November 2026");
    calendar.destroy();
  });

  it("maps the toolbar: chunks replace fc-left/center/right", () => {
    loadV6Global();
    const calendar = renderV6Month("v6-css-delta-toolbar");
    const root = document.getElementById("v6-css-delta-toolbar");
    const toolbar = root.querySelector(".fc-header-toolbar");
    expect(toolbar).not.toBeNull();
    // v6 toolbar: exactly three unstyled chunk children (no per-section
    // order hooks — chunk order is DOM order).
    const chunks = toolbar.querySelectorAll(":scope > .fc-toolbar-chunk");
    expect(chunks.length).toBe(3);
    // The snippet's 480px responsive overrides target v2 section classes
    // that v6 never renders — the migration must retarget them.
    for (const legacy of [".fc-left", ".fc-center", ".fc-right"]) {
      expect(
        root.querySelectorAll(legacy).length,
        `${legacy} must not exist under v6 (snippet override is dead)`,
      ).toBe(0);
    }
    calendar.destroy();
  });

  it("maps the buttons: no fc-state-default / fc-corner-* under v6", () => {
    loadV6Global();
    const calendar = renderV6Month("v6-css-delta-buttons");
    const root = document.getElementById("v6-css-delta-buttons");
    const buttons = Array.from(
      root.querySelectorAll(".fc-header-toolbar button"),
    );
    expect(buttons.length).toBeGreaterThan(0);
    for (const button of buttons) {
      expect(button.className).toMatch(/fc-button/);
      expect(button.className).toMatch(/fc-button-primary/);
    }
    // The snippet's view-toggle buttons reuse these v2 theme classes
    // outside the calendar root — v6 never styles them.
    for (const legacy of [
      ".fc-state-default",
      ".fc-corner-left",
      ".fc-corner-right",
    ]) {
      expect(
        root.querySelectorAll(legacy).length,
        `${legacy} must not exist under v6 (toggle styling needs a compat rule)`,
      ).toBe(0);
    }
    const cssText = injectedV6CssText();
    expect(cssText).toMatch(/\.fc-button/);
    expect(cssText).not.toMatch(/\.fc-state-default/);
    expect(cssText).not.toMatch(/fc-corner/);
    calendar.destroy();
  });

  it("maps view/event hooks and records the print boundary for slice 2b", () => {
    loadV6Global();
    const calendar = renderV6Month("v6-css-delta-hooks");
    const root = document.getElementById("v6-css-delta-hooks");
    // v6 view wrapper replaces .fc-view-container; v6 event text hook
    // replaces .fc-title.
    expect(root.querySelector(".fc-view-harness")).not.toBeNull();
    expect(root.querySelector(".fc-dayGridMonth-view")).not.toBeNull();
    expect(root.querySelectorAll(".fc-view-container").length).toBe(0);
    expect(root.querySelectorAll(".fc-title").length).toBe(0);
    // Print: the v6 bundle ships no CSS at all (JS-injected styles only)
    // and the injected CSSOM carries no @media print rules, while the
    // snippet links fullcalendar.print.css — authoring that equivalent is
    // slice 2b scope, recorded here as the boundary.
    expect(collectCssFiles(V6_ROOT)).toEqual([]);
    const cssText = injectedV6CssText();
    expect(cssText).not.toMatch(/@media print/);
    calendar.destroy();
  });
});
