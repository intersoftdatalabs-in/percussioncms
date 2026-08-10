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
package com.percussion.wizard;

import com.percussion.cx.PSContentExplorerApplet;
import com.percussion.guitools.ErrorDialogs;
import com.percussion.guitools.PSDialog;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.swing.JPanel;

/**
 * A standard wizard dialog using the card layout for easy 'next' and 'back' wizard functonality.
 */
public class PSWizardDialog extends PSDialog implements IPSWizardDialog {
  /**
   * Construct a new wizard dialog for the supplied pages.
   *
   * @param parent the parent for this dialog, may be <code>null</code>.
   * @param pages and array of page arrays. For each page the user must supply an array of page
   *     data, where the first element is the name of the page panel as <code>String</code>, the
   *     second element is the page panel input data as <code>Object</code> and the third element is
   *     the page instruction as <code>String</code>.
   * @param title the dialog title, may be <code>null</code> or empty.
   * @param applet the content explorer applet, may not be <code>null</code>.
   */
  public PSWizardDialog(
      Frame parent, Object[][] pages, String title, PSContentExplorerApplet applet) {
    super(parent, "");

    if (title == null) throw new IllegalArgumentException("title cannot be null");

    title = title.trim();
    if (title.length() == 0) throw new IllegalArgumentException("title cannot be empty");
    setTitle(title);

    if (pages == null || pages.length == 0)
      throw new IllegalArgumentException("pages cannot be null or empty");

    if (applet == null) throw new IllegalArgumentException("applet must not be null");

    m_applet = applet;

    initDialog(pages);
  }

  /**
   * Resolves the wizard page type for a zero-based page index. Pure helper for unit tests.
   *
   * @param pageIndex zero-based index of the active page
   * @param pageCount total number of pages, must be &gt; 0
   * @return one of {@link IPSWizardDialog#TYPE_FIRST}, {@link IPSWizardDialog#TYPE_MID}, {@link
   *     IPSWizardDialog#TYPE_LAST}
   * @throws IllegalArgumentException if pageCount or pageIndex is invalid
   */
  public static int resolvePageType(int pageIndex, int pageCount) {
    if (pageCount <= 0) {
      throw new IllegalArgumentException("pageCount must be > 0");
    }
    if (pageIndex < 0 || pageIndex >= pageCount) {
      throw new IllegalArgumentException("pageIndex out of range");
    }
    if (pageIndex == 0) {
      return TYPE_FIRST;
    }
    if (pageIndex >= pageCount - 1) {
      return TYPE_LAST;
    }
    return TYPE_MID;
  }

  /**
   * Returns whether {@code type} is one of {@link IPSWizardDialog#TYPES}. Pure helper for unit
   * tests.
   *
   * @param type candidate page type
   * @return <code>true</code> when type is valid
   */
  public static boolean isValidPageType(int type) {
    for (int t : TYPES) {
      if (t == type) {
        return true;
      }
    }
    return false;
  }

  /**
   * Builds the last-page summary body from ordered page summaries, preserving historical append
   * rules: a non-empty summary is appended with a trailing newline only when it is not the last
   * page index in the wizard (skipped pages are {@code null} entries; empty summaries are omitted).
   * Pure helper for unit tests.
   *
   * @param summaries ordered by page index; {@code null} entry means the page was skipped
   * @return summary body without the original instruction prefix, never <code>null</code>
   */
  public static String collectSummaryBody(List<String> summaries) {
    if (summaries == null || summaries.isEmpty()) {
      return "";
    }
    StringBuilder summary = new StringBuilder();
    int n = summaries.size();
    for (int i = 0; i < n; i++) {
      String sum = summaries.get(i);
      if (sum == null) {
        continue;
      }
      if (sum.trim().isEmpty()) {
        continue;
      }
      // Matches Iterator.hasNext() after next(): only non-final map keys append.
      if (i < n - 1) {
        summary.append(sum).append('\n');
      }
    }
    return summary.toString();
  }

  /**
   * Collects non-skipped page summaries in page-index order for {@link #collectSummaryBody(List)}.
   * Pure helper for unit tests.
   *
   * @param pages wizard pages keyed by page index, may not be <code>null</code>
   * @param skippedPages skipped pages keyed by page index, may be <code>null</code>
   * @return ordered list with {@code null} for skipped pages, never <code>null</code>
   */
  public static List<String> collectOrderedSummaries(
      Map<Integer, IPSWizardPanel> pages, Map<Integer, IPSWizardPanel> skippedPages) {
    if (pages == null) {
      throw new IllegalArgumentException("pages cannot be null");
    }
    List<String> summaries = new ArrayList<>(pages.size());
    for (Map.Entry<Integer, IPSWizardPanel> e : pages.entrySet()) {
      if (skippedPages != null && skippedPages.get(e.getKey()) != null) {
        summaries.add(null);
        continue;
      }
      IPSWizardPanel panel = e.getValue();
      summaries.add(panel == null ? "" : panel.getSummary());
    }
    return summaries;
  }

  /**
   * Prepends the original last-page instruction to a collected summary body. Pure helper for unit
   * tests.
   *
   * @param originalInstruction instruction saved from the last page, may be <code>null</code>
   * @param summaryBody body from {@link #collectSummaryBody(List)}, may be <code>null</code>
   * @return combined last-page instruction text, never <code>null</code>
   */
  public static String prependLastPageInstruction(String originalInstruction, String summaryBody) {
    String orig = originalInstruction == null ? "" : originalInstruction;
    String body = summaryBody == null ? "" : summaryBody;
    return orig + "\n\n" + body;
  }

  /**
   * Collects panel data in page-index order. Pure helper for unit tests.
   *
   * @param pages wizard pages keyed by page index, may not be <code>null</code>
   * @return array of page data (each element may be <code>null</code>), never <code>null</code>
   */
  public static Object[] collectPageData(Map<Integer, IPSWizardPanel> pages) {
    if (pages == null) {
      throw new IllegalArgumentException("pages cannot be null");
    }
    Object[] data = new Object[pages.size()];
    for (int i = 0; i < pages.size(); i++) {
      IPSWizardPanel panel = pages.get(Integer.valueOf(i));
      data[i] = panel == null ? null : panel.getData();
    }
    return data;
  }

  /* (non-Javadoc)
   * @see IPSWizardDialog#onNext()
   */
  public void onNext() {
    try {
      Integer key = Integer.valueOf(m_pageIndex);
      IPSWizardPanel panel = m_pages.get(key);
      panel.validatePanel();

      while (panel.skipNext()) {
        m_cards.next(m_mainPanel);
        ++m_pageIndex;

        key = Integer.valueOf(m_pageIndex);
        m_skippedPages.put(key, m_pages.get(key));
        panel = m_pages.get(key);
      }

      m_cards.next(m_mainPanel);
      ++m_pageIndex;

      updateControls();
    } catch (PSWizardValidationError e) {
      ErrorDialogs.showErrorMessage(
          this, e.getMessage(), m_applet.getResourceString(getClass(), "Error"));
    }
  }

  /* (non-Javadoc)
   * @see IPSWizardDialog#onBack()
   */
  public void onBack() {
    m_cards.previous(m_mainPanel);
    --m_pageIndex;

    Integer key = Integer.valueOf(m_pageIndex);
    IPSWizardPanel panel = m_skippedPages.get(key);
    while (panel != null) {
      m_skippedPages.remove(key);

      m_cards.previous(m_mainPanel);
      --m_pageIndex;

      key = Integer.valueOf(m_pageIndex);
      panel = m_skippedPages.get(key);
    }

    updateControls();
  }

  /* (non-Javadoc)
   * @see IPSWizardDialog#onCancel()
   */
  public void onCancel() {
    super.onCancel();
  }

  /* (non-Javadoc)
   * @see IPSWizardDialog#onFinish()
   */
  public void onFinish() {
    super.onOk();
  }

  /* (non-Javadoc)
   * @see Object[] IPSWizardDialog#getData()
   */
  public Object[] getData() {
    return collectPageData(m_pages);
  }

  /**
   * Initialize the dialog for the supplied pages and page instructions.
   *
   * @param pages and array of page arrays. For each page the user must supply an array of page
   *     data, where the first element is the name of the page panel as <code>String</code>, the
   *     second element is the page panel input data as <code>Object</code> and the third element is
   *     the page instruction as <code>String</code>.
   */
  private void initDialog(Object[][] pages) {
    Container cp = getContentPane();

    cp.setLayout(new BorderLayout());
    m_mainPanel = createMainPanel(pages);
    cp.add(m_mainPanel, BorderLayout.CENTER);
    m_wizardCommands = new PSWizardCommandPanel(this);
    cp.add(m_wizardCommands, BorderLayout.SOUTH);

    pack();
    center();
    setResizable(true);
    setVisible(true);
  }

  /**
   * Creates tha main dialog panel.
   *
   * @param pages and array of page arrays. For each page the user must supply an array of page
   *     data, where the first element is the name of the page panel as <code>String</code>, the
   *     second element is the page panel input data as <code>Object</code> and the third element is
   *     the page instruction as <code>String</code>.
   * @return the new panel, never <code>null</code>.
   */
  private JPanel createMainPanel(Object[][] pages) {
    JPanel panel = new JPanel();
    panel.setLayout(m_cards);
    panel.setPreferredSize(new Dimension(600, 300));
    System.out.println("Number of pages =" + pages.length);
    for (int i = 0; i < pages.length; i++) {
      Object[] page = pages[i];
      System.out.println("create page " + (String) page[PAGE_PANEL]);

      Integer key = Integer.valueOf(i);
      PSWizardPanel pagePanel = instantiate((String) page[PAGE_PANEL]);
      if (pagePanel == null)
        throw new IllegalArgumentException(
            "a supplied wizard page does not conform to the described " + "interface");

      pagePanel.setData(page[PAGE_DATA]);
      pagePanel.setInstruction((String) page[PAGE_INSTRUCTION]);

      m_pages.put(key, pagePanel);
      panel.add(key.toString(), pagePanel);
    }

    return panel;
  }

  /**
   * Are we on the last wizard page?
   *
   * @return <code>true</code> if we are, <code>false</code> otherwise.
   */
  private boolean isLast() {
    return m_pageIndex >= m_pages.size() - 1;
  }

  /**
   * Get the wizard page type.
   *
   * @return the type of the currently active page, one of the <code>TYPE_XXX</code> values.
   */
  private int getPageType() {
    return resolvePageType(m_pageIndex, m_pages.size());
  }

  /**
   * Updates the wizard controls based on the current page type. The command panel is always
   * updated. If we are on the last wizard page, all pages summary informations are collected and
   * set as the page instruction for the last page.
   */
  private void updateControls() {
    m_wizardCommands.updateControls(getPageType());

    if (isLast()) {
      /*
       * Collect the summaries from all pages to build the instruction
       * set on the last wizard page.
       */
      List<String> summaries = collectOrderedSummaries(m_pages, m_skippedPages);
      String body = collectSummaryBody(summaries);

      IPSWizardPanel page = m_pages.get(Integer.valueOf(m_pages.size() - 1));
      if (m_lastPageInstruction == null) {
        m_lastPageInstruction = page.getInstruction();
      }
      page.setInstruction(prependLastPageInstruction(m_lastPageInstruction, body));
    }
  }

  /**
   * Instantiate the wizard panel for the supplied class name.
   *
   * @param className the name of the class to be instantiated, it is assumed that the class exists
   *     and has a default constructor.
   * @return the new panel, may be <code>null</code> if the instantiation failed.
   */
  private PSWizardPanel instantiate(String className) {
    System.out.println("Instantiate PSWizardPanel");
    PSWizardPanel page = null;
    try {
      Class<?> c = Class.forName(className);
      Constructor<?> ctor = c.getConstructor(PSContentExplorerApplet.class);

      page = (PSWizardPanel) ctor.newInstance(m_applet);
    } catch (Exception e) {
      System.out.println("Error finding class " + className);
      e.printStackTrace();
    }

    return page;
  }

  /**
   * The original last page instruction, saved in the first call to {@link updateControls()} for the
   * last page, never <code>null</code> or changed after that.
   */
  private String m_lastPageInstruction = null;

  /** This keeps track of the ccurrent wizard page. */
  private int m_pageIndex = 0;

  /**
   * A map of wizard pages. The map key is an <code>Integer</code> with the page index (starting at
   * 0) while the map value is the page as <code>IPSWizardPanel</code>. Initialized in {@link
   * #createMainPanel(Object[][])}, never changed after that.
   */
  private final Map<Integer, IPSWizardPanel> m_pages = new TreeMap<>();

  /**
   * The map used to keep track of skipped wizard pages. The map key is an <code>Integer</code> with
   * the page index (starting at 0) while the map value is the page as <code>IPSWizardPanel</code>.
   * Initialized to an empty map, updated on calls to {@link #onNext()} or {@link #onBack()}.
   */
  private final Map<Integer, IPSWizardPanel> m_skippedPages = new TreeMap<>();

  /** The card layout used for the wizards main panel. */
  private final CardLayout m_cards = new CardLayout();

  /**
   * The main wizard panel, initialized during construction, never <code>null</code> or changed
   * after that.
   */
  private JPanel m_mainPanel = null;

  /**
   * The wizards command panel, initialized during construction, never <code>null</code> or changed
   * after that.
   */
  private PSWizardCommandPanel m_wizardCommands = null;

  /** The index of the page panel for the page array as supplied in constructor. */
  private static final int PAGE_PANEL = 0;

  /** The index of the page data for the page array as supplied in constructor. */
  private static final int PAGE_DATA = 1;

  /** The index of the page instruction for the page array as supplied in constructor. */
  private static final int PAGE_INSTRUCTION = 2;

  /** A reference back to the applet that initiated this action manager. */
  private PSContentExplorerApplet m_applet;
}
