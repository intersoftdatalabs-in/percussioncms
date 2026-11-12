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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * DataTables 3.x core swap (issue #6002).
 *
 * <p>Tags: {@code @classic-ui} {@code @ui}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/classic-datatables-core.spec.js}</p>
 *
 * <p>Covers the §1 core migration: the vendored DataTables 1.12.1 core was
 * deleted and the stable jslib URLs now serve the Dependabot-tracked npm
 * datatables.net 3.x dist (JS) + datatables.net-dt default styling (CSS).
 * The legacy fn* instance API is gone in DT3, so the spec proves the served
 * bytes are DT 3.x with no fn* surface and exercises the migrated 1.10+ call
 * patterns (DataTable() constructor, row()/cell()/draw()/destroy(),
 * footerCallback, render, custom order plug-in via the dataTableExt alias,
 * header-click sorting) against a live grid in the dashboard page.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

const CORE_JS =
  "/Rhythmyx/cm/jslib/profiles/3x/jquery/plugins/jquery-datatables/js/jquery.dataTables.js";
const CORE_CSS =
  "/Rhythmyx/cm/jslib/profiles/3x/jquery/plugins/jquery-datatables/css/jquery.dataTables.css";

test.describe("DataTables 3.x core swap (issue #6002)", () => {
  test(
    "dashboard serves DT 3.x with no legacy fn* surface @ui",
    { tag: ["@classic-ui", "@ui"] },
    async ({ page }) => {
      test.setTimeout(120_000);
      const pluginErrors = [];
      page.on("pageerror", (err) => {
        pluginErrors.push(String((err && err.stack) || err));
      });
      page.on("console", (msg) => {
        if (
          msg.type() === "error" &&
          msg.text().includes("jquery-datatables")
        ) {
          pluginErrors.push(msg.text());
        }
      });
      page.on("requestfailed", (req) => {
        if (req.url().includes("jquery-datatables")) {
          pluginErrors.push(
            `${req.url()} :: ${req.failure()?.errorText}`,
          );
        }
      });

      await loginAsAdmin(page);
      for (const asset of [CORE_JS, CORE_CSS]) {
        const resp = await page.request.get(`${BASE_URL}${asset}`);
        expect(resp.status(), `expected 200 for ${asset}`).toBe(200);
      }
      await page.goto(`${BASE_URL}/Rhythmyx/cm/app/dashboard.jsp`, {
        waitUntil: "networkidle",
      });

      const probe = await page.evaluate(() => ({
        hasJQuery:
          typeof window.jQuery === "function" &&
          typeof window.jQuery.fn.DataTable === "function",
        version:
          window.jQuery &&
          window.jQuery.fn &&
          window.jQuery.fn.DataTable &&
          window.jQuery.fn.DataTable.version,
        aliasIntact:
          !!(
            window.jQuery &&
            window.jQuery.fn &&
            window.jQuery.fn.dataTableExt &&
            window.jQuery.fn.dataTableExt.afnSortData
          ),
      }));
      expect(probe.hasJQuery, "DataTable plugin missing").toBe(true);
      expect(String(probe.version)).toMatch(/^3\./);
      expect(probe.aliasIntact, "dataTableExt alias regressed").toBe(true);
      expect(
        pluginErrors,
        `datatables plugin errors:\n${pluginErrors.join("\n")}`,
      ).toEqual([]);
    },
  );

  test(
    "migrated 1.10+ grid patterns sort and mutate without errors @ui",
    { tag: ["@classic-ui", "@ui"] },
    async ({ page }) => {
      test.setTimeout(120_000);
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));

      await loginAsAdmin(page);
      await page.goto(`${BASE_URL}/Rhythmyx/cm/app/dashboard.jsp`, {
        waitUntil: "networkidle",
      });

      const result = await page.evaluate(() => {
        const $ = window.jQuery;
        const out = { steps: [] };
        // Custom order plug-in via the dataTableExt alias, mirroring the
        // perc-type-string / dom-elapsed-time plug-ins in classic sources.
        $.fn.dataTableExt.afnSortData["e2e-seconds"] = function () {
          const values = [];
          this.api()
            .column(1, { order: "index" })
            .nodes()
            .map((td) => {
              values.push(Number($(td).text()));
            });
          return values;
        };
        document.body.insertAdjacentHTML(
          "beforeend",
          `<table id="e2e-dt"><thead><tr><th>Name</th><th>Secs</th></tr></thead><tbody>` +
            `<tr><td>beta</td><td>30</td></tr>` +
            `<tr><td>alpha</td><td>10</td></tr>` +
            `<tr><td>gamma</td><td>20</td></tr>` +
            `</tbody><tfoot><tr><th></th><th></th></tr></tfoot></table>`,
        );
        const api = $("#e2e-dt").DataTable({
          paging: false,
          searching: false,
          info: false,
          order: [[1, "asc"]],
          footerCallback(tfoot) {
            $(tfoot).find("th:first").text("3 rows");
          },
          columns: [
            { type: "string" },
            {
              type: "num",
              orderDataType: "e2e-seconds",
              render(data, type) {
                return type === "display" ? `${data}s` : data;
              },
            },
          ],
        });
        out.steps.push(`legacyGone:${typeof api.fnGetData === "undefined"}`);
        out.steps.push(`hasRowApi:${typeof api.row === "function"}`);
        out.firstAfterSort = $("#e2e-dt tbody tr:first td:first").text();
        out.footerText = $("#e2e-dt tfoot th:first").text();
        // Mutations through the migrated CRUD patterns.
        const added = api.row.add(["delta", 5]);
        api.draw(false);
        out.addedNode = added.node().tagName;
        api.cell($("#e2e-dt tbody tr")[0], 0).data("alpha2");
        api.draw(false);
        out.editedFirst = $("#e2e-dt tbody tr:first td:first").text();
        api
          .row($("#e2e-dt tbody tr")[0])
          .remove()
          .draw(false);
        out.remaining = api.rows().data().length;
        api.destroy();
        out.destroyed = !$.fn.DataTable.isDataTable("#e2e-dt");
        document.getElementById("e2e-dt").remove();
        return out;
      });

      expect(result.steps).toEqual([
        "legacyGone:true",
        "hasRowApi:true",
      ]);
      // Custom order plug-in sorted 10/20/30 ascending by the numeric column.
      expect(result.firstAfterSort).toBe("alpha");
      expect(result.footerText).toBe("3 rows");
      expect(result.addedNode).toBe("TR");
      expect(result.editedFirst).toBe("alpha2");
      expect(result.remaining).toBe(3);
      expect(result.destroyed).toBe(true);
      expect(pageErrors, `page errors:\n${pageErrors.join("\n")}`).toEqual(
        [],
      );
    },
  );
});
