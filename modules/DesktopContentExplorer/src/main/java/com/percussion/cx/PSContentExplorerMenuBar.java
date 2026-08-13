/*
 * Copyright (c) 2023 Intersoft Data Labs, Inc.
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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.percussion.cx;

import com.percussion.cx.objectstore.PSMenuAction;
import com.percussion.cx.objectstore.PSMenuBar;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPopupMenu;

/**
 * The class that represents top-level menu bar in the applet.
 *
 * <p>Declared {@code final} so Swing initialization from the constructor cannot observe a
 * partially constructed subclass (javac {@code this-escape}). Session-only collaborators are
 * {@code transient} (the menu bar is never serialized).
 */
public final class PSContentExplorerMenuBar extends JMenuBar implements IPSSelectionListener {
  private static final long serialVersionUID = 1L;
  /**
   * Constructs the global menu bar with supplied parameters.
   *
   * @param menuBar the menu bar that holds the top-level menu actions, may not be <code>null</code>
   * @param menuSource the menu source on which the menu actions should act on, may not be <code>
   *     null</code>
   * @param actManager the action manager, may not be <code>null</code>
   */
  public PSContentExplorerMenuBar(
      PSMenuBar menuBar, PSMenuSource menuSource, PSActionManager actManager) {

    if (menuBar == null) throw new IllegalArgumentException("menuBar may not be null.");

    if (menuSource == null) throw new IllegalArgumentException("menuSource may not be null.");

    if (actManager == null) throw new IllegalArgumentException("actManager may not be null.");
    this.setFocusTraversalKeysEnabled(true);
    m_menuBar = menuBar;
    m_menuSource = menuSource;
    m_actManager = actManager;
    m_menus = new ArrayList<PSContentExplorerMenu>();

    createMenus();
  }

  /** Refresh the menu with the current menu source selected. */
  public void refreshMenus() {
    for (PSContentExplorerMenu cxMenu : m_menus) {
      cxMenu.refreshChildMenus();
    }
  }

  /** Create all menus for this menu bar */
  private void createMenus() {
    Iterator<?> actions = m_menuBar.getActions();

    while (actions.hasNext()) {
      Object next = actions.next();
      if (!(next instanceof PSMenuAction action)) {
        continue;
      }
      PSContentExplorerMenu menu = new PSContentExplorerMenu(action, m_menuSource, m_actManager);
      m_menus.add(menu);
      m_menuActions.add(action);

      add(menu.getMenu());
    }
  }

  /**
   * Gets the menu represented by this menu bar as pop-up menu.
   *
   * @return the pop-up menu, never <code>null</code> and will have menu elements.
   */
  public JPopupMenu getPopupMenu() {
    JPopupMenu popup = new JPopupMenu();

    for (PSMenuAction action : m_menuActions) {
      PSContentExplorerMenu cxMenu = new PSContentExplorerMenu(action, m_menuSource, m_actManager);

      //  JMenu menu = new JMenu(cxMenu.getMenu().getAction());
      JMenu menu = cxMenu.getMenu();
      menu.setFont(PSContentExplorerMenu.POPUP_MENU_FONT);
      menu.getPopupMenu().addPopupMenuListener(cxMenu);
      popup.add(menu);
    }

    return popup;
  }

  /** The underlying PS menu bar, may be <code>null</code>. */
  private transient PSMenuBar m_menuBar;

  /** The source used to populate the menu bar dynamically. */
  private transient PSMenuSource m_menuSource;

  /** The action manager used to look up menu actions. */
  private transient PSActionManager m_actManager;

  /** The list of menus in the menu bar, may be <code>null</code>. */
  private transient List<PSContentExplorerMenu> m_menus;

  /**
   * The list of <code>PSContentExplorerMenu</code>s that represent that exists in this menu bar,
   * initialized and filled in the constructor and never <code>null</code> or modified after that.
   */
  private transient List<PSMenuAction> m_menuActions = new ArrayList<PSMenuAction>();

  /**
   * Notification event that the selection has changed in main view of applet. Updates the default
   * selection and context selection of the menu source if the supplied selection is an instance of
   * {@link PSNavigationalSelection}, otherwise only context selection.
   *
   * @param selection the selection object that represents the current selection in the main view.
   */
  public void selectionChanged(PSSelection selection) {
    if (selection == null) throw new IllegalArgumentException("selection may not be null.");

    if (selection instanceof PSNavigationalSelection) m_menuSource.setSource(selection);

    m_menuSource.setContextSource(selection);

    refreshMenus();
    m_actManager.informListeners(PSActionEvent.REFRESH_OPTIONS);
  }
}
