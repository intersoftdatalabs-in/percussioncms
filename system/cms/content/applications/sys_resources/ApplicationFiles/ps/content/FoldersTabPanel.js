ps.declare(
  "ps.content.FoldersTabPanel",
  ps.content.FolderSitesBaseTabPanel,
  function (_parent) {
    ps.assert(_parent, "Parent must be specified");

    this.prefix = ps.util.BROWSETAB_FOLDERS_PANEL_PREF;
    this.parent = _parent;
  },
  {
    init: function () {
      this.tabId = this.prefix + ".tab";
      this.tab = ps.widget.byId(this.tabId);
      ps.assert(this.tab, "Tab for " + this.prefix + " should exist");
      this.url =
        this.parent.rxroot + "/ui/content/sitesfolderpanel.jsp?mode=folders";
      ps.content.FoldersTabPanel.superclass.init.apply(this);
    },

    /**
     * Is called when templates selection panel is loaded.
     */
    _onTemplatesSiteFolderParamLoaded: function () {
      ps.assert(
        false,
        "Templates site folder params pane should not be loaded " +
          "on the folders tab",
      );
    },
  },
);
