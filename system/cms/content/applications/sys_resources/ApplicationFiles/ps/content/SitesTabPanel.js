ps.declare(
  "ps.content.SitesTabPanel",
  ps.content.FolderSitesBaseTabPanel,
  function (_parent) {
    ps.assert(_parent, "Parent must be specified");

    this.prefix = ps.util.BROWSETAB_SITES_PANEL_PREF;
    this.parent = _parent;
  },
  {
    init: function () {
      this.tabId = this.prefix + ".tab";
      this.tab = ps.widget.byId(this.tabId);
      ps.assert(this.tab, "Tab for " + this.prefix + " should exist");
      this.url =
        this.parent.rxroot + "/ui/content/sitesfolderpanel.jsp?mode=sites";
      ps.content.SitesTabPanel.superclass.init.apply(this);
    },

    /**
     * Returns site name selected in UI, if available and requested by the user.
     * Otherwise returns <code>null</code>.
     */
    _getUISiteName: function () {
      var id = "ps.select.templates.includeSitesCheckbox";
      if (this._mustById(id).checked) {
        var parts = this.getFolder().split("/");
        parts = parts.filter(function (part) {
          return part.length > 0;
        });
        return parts[0];
      } else {
        return null;
      }
    },

    /**
     * Returns folder path selected in UI, if available and requested by the user.
     * Otherwise returns <code>null</code>.
     */
    _getUIFolderPath: function () {
      var id = "ps.select.templates.includeFoldersCheckbox";
      return this._mustById(id).checked ? this.getFolder() : null;
    },
  },
);
