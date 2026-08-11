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

import com.percussion.border.PSFocusBorder;
import com.percussion.cms.objectstore.PSSecurityProviderInstanceSummary;
import com.percussion.guitools.PSAccessibleActionListener;
import com.percussion.guitools.PSDialog;
import com.percussion.guitools.PSPropertyPanel;
import com.percussion.guitools.UTStandardCommandPanel;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

/** New user ACL dialog is used to enter ACL's and user name for creation of a new user. */
public class PSACLNewUserDialog extends PSDialog implements ItemListener {

  /**
   * Constructs a new <code>PSACLDialog</code> using the passed in list of providers
   *
   * @param dialog the dialog that is the owner of this dialog. May be <code>null</code>.
   * @param providers <code>Iterator</code> of security providers May not be <code>null</code>, but
   *     may be empty.
   * @param applet the content explorer applet that owns this dialog and supplies its resources. May
   *     not be <code>null</code>.
   */
  public PSACLNewUserDialog(
      Dialog dialog,
      Iterator<PSSecurityProviderInstanceSummary> providers,
      PSContentExplorerApplet applet) {
    super(dialog, applet.getResourceString(PSACLNewUserDialog.class, "New User ACL Entry"));

    if (applet == null) throw new IllegalArgumentException("applet may not be null");
    m_applet = applet;

    initDialog(providers);
  }

  /**
   * Constructs a new <code>PSACLDialog</code> using the passed in list of providers
   *
   * @param frame the frame that is the owner of this dialog. May be <code>null</code>.
   * @param providers <code>Iterator</code> of security providers May not be <code>null</code>, but
   *     may be empty.
   * @param applet the content explorer applet that owns this dialog and supplies its resources. May
   *     not be <code>null</code>.
   */
  public PSACLNewUserDialog(
      Frame frame,
      Iterator<PSSecurityProviderInstanceSummary> providers,
      PSContentExplorerApplet applet) {
    super(frame, applet.getResourceString(PSACLNewUserDialog.class, "New User ACL Entry"));

    if (applet == null) throw new IllegalArgumentException("applet may not be null");
    m_applet = applet;

    initDialog(providers);
  }

  /**
   * Returns the selected provider
   *
   * @return returns the selected provider. May be <code>null</code>.
   */
  public PSSecurityProviderInstanceSummary getSelectedProvider() {
    // JComboBox#getSelectedItem remains Object-typed even on JComboBox<E>
    return (PSSecurityProviderInstanceSummary) m_instanceComboBox.getSelectedItem();
  }

  /**
   * Returns the user name.
   *
   * @return the user name. May be <code>null</code>.
   */
  public String getUserName() {
    return m_nameTextField.getText();
  }

  /**
   * Initializes the dialog and loads data in combo boxes.
   *
   * @param providers the iterator of security provider instances.
   */
  private void initDialog(Iterator<PSSecurityProviderInstanceSummary> providers) {
    JPanel mainPanel = new JPanel(new BorderLayout());

    PSPropertyPanel propPanel = new PSPropertyPanel();
    propPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 10));

    m_typeComboBox = new JComboBox<>();
    m_typeComboBox.addActionListener(new PSAccessibleActionListener());
    m_typeComboBox.addItemListener(this);
    m_instanceComboBox = new JComboBox<>();
    m_instanceComboBox.addActionListener(new PSAccessibleActionListener());
    m_instanceComboBox.addItemListener(this);
    m_nameTextField = new JTextField();

    propPanel.addPropertyRow(
        m_applet.getResourceString(getClass(), "Security provider type:"),
        m_typeComboBox,
        PSContentExplorerApplet.getResourceMnemonic(getClass(), "Security provider type:", 'p'));

    propPanel.addPropertyRow(
        m_applet.getResourceString(getClass(), "Provider instance:"),
        m_instanceComboBox,
        PSContentExplorerApplet.getResourceMnemonic(getClass(), "Provider instance:", 'i'));

    propPanel.addPropertyRow(
        m_applet.getResourceString(getClass(), "Name:"),
        m_nameTextField,
        PSContentExplorerApplet.getResourceMnemonic(getClass(), "Name:", 'N'));

    UTStandardCommandPanel defCommandPanel =
        new UTStandardCommandPanel(this, SwingConstants.HORIZONTAL, true);

    defCommandPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

    mainPanel.add(propPanel, BorderLayout.CENTER);
    JPanel cmdPanel = new JPanel(new BorderLayout());
    cmdPanel.add(defCommandPanel, BorderLayout.EAST);
    mainPanel.add(cmdPanel, BorderLayout.SOUTH);

    setContentPane(mainPanel);

    loadComboBoxes(providers);

    pack();
    center();
    setResizable(true);

    // Add focus highlights
    PSDisplayOptions dispOptions =
        (PSDisplayOptions) UIManager.getDefaults().get(PSContentExplorerConstants.DISPLAY_OPTIONS);
    PSFocusBorder focusBorder = new PSFocusBorder(1, dispOptions);
    focusBorder.addToAllNavigable(mainPanel);
  }

  /**
   * Loads instances into instance comboBox
   *
   * @param provider the <code>Provider</code> that contains the instances. May be <code>null</code>
   *     .
   */
  private void loadInstances(ProviderType provider) {
    if (null == provider) return;
    m_instanceComboBox.removeAllItems();
    for (PSSecurityProviderInstanceSummary instance : provider.getInstances()) {
      m_instanceComboBox.addItem(instance);
    }
  }

  /**
   * Initializes loading of combo boxes
   *
   * @param providers. May be <code>null</code>.
   */
  private void loadComboBoxes(Iterator<PSSecurityProviderInstanceSummary> providers) {
    if (null == providers) return;

    List<ProviderType> temp = groupProvidersByType(providers);
    for (ProviderType type : temp) {
      m_typeComboBox.addItem(type);
    }

    loadInstances((ProviderType) m_typeComboBox.getSelectedItem());
  }

  /**
   * Groups security provider instances by provider type for the type combo box. Package-visible for
   * unit tests. Preserves historic {@link ProviderType#equals(Object)} / list membership behavior.
   *
   * @param providers iterator of provider summaries; may be <code>null</code>
   * @return ordered list of provider type groups; never <code>null</code>
   */
  static List<ProviderType> groupProvidersByType(
      Iterator<PSSecurityProviderInstanceSummary> providers) {
    List<ProviderType> temp = new ArrayList<>();
    if (providers == null) {
      return temp;
    }
    while (providers.hasNext()) {
      PSSecurityProviderInstanceSummary provider = providers.next();
      ProviderType type = new ProviderType(provider.getTypeId(), provider.getTypeName());
      int existingIndex = temp.indexOf(type);
      if (existingIndex >= 0) {
        type = temp.get(existingIndex);
        type.addInstance(provider);
      } else {
        type.addInstance(provider);
        temp.add(type);
      }
    }
    return temp;
  }

  // see ItemListener interface for details
  public void itemStateChanged(ItemEvent event) {
    Object source = event.getSource();

    if (m_typeComboBox == source) {
      loadInstances((ProviderType) m_typeComboBox.getSelectedItem());
    }
  }

  /**
   * Convenience nested class to represent a provider type group. Static so grouping helpers can
   * construct instances without a dialog.
   */
  static class ProviderType {

    /**
     * Construct new ProviderType object
     *
     * @param typeId the provider type id
     * @param typeName the provider type name
     */
    ProviderType(int typeId, String typeName) {
      m_typeId = typeId;
      m_typeName = typeName;
    }

    /**
     * Adds an instance to this provider type
     *
     * @param instance the instance to add. May be <code>null</code>.
     */
    void addInstance(PSSecurityProviderInstanceSummary instance) {
      if (null == instance) return;
      m_instances.add(instance);
    }

    /**
     * Returns instances of this provider type.
     *
     * @return list of instances. Never <code>null</code>.
     */
    public List<PSSecurityProviderInstanceSummary> getInstances() {
      return m_instances;
    }

    /**
     * Returns an instance by name
     *
     * @param name May not be <code>null</code>.
     * @return the instance if it exists, else <code>null</code>.
     */
    PSSecurityProviderInstanceSummary getInstance(String name) {
      if (null == name) throw new IllegalArgumentException("Instance name cannot be null.");
      for (PSSecurityProviderInstanceSummary inst : m_instances) {
        if (inst.getInstanceName().equals(name)) return inst;
      }
      return null;
    }

    public boolean equals(Object object) {
      if (this == object) return true;

      if (!(object instanceof ProviderType)) return false;

      ProviderType that = (ProviderType) object;

      return new org.apache.commons.lang3.builder.EqualsBuilder()
          .appendSuper(super.equals(object))
          .append(m_typeId, that.m_typeId)
          .append(m_typeName, that.m_typeName)
          .append(m_instances, that.m_instances)
          .isEquals();
    }

    public int hashCode() {
      return new org.apache.commons.lang3.builder.HashCodeBuilder(17, 37)
          .appendSuper(super.hashCode())
          .append(m_typeId)
          .append(m_typeName)
          .append(m_instances)
          .toHashCode();
    }

    /**
     * Returns the typeName as the string representation for this object.
     *
     * @return the typeName for this <code>ProviderType</code>.
     */
    public String toString() {
      return m_typeName;
    }

    /** The list of provider instances. Never <code>null</code>, may be empty. */
    protected List<PSSecurityProviderInstanceSummary> m_instances = new ArrayList<>();

    /** The provider type id */
    protected int m_typeId;

    /** The provider type name */
    protected String m_typeName;
  }

  /**
   * Security provider type combo box. Initialized in {@link #initDialog(Iterator)}, never <code>
   * null</code> after that.
   */
  private JComboBox<ProviderType> m_typeComboBox;

  /**
   * Provider instance combo box. Initialized in {@link #initDialog(Iterator)}, never <code>null
   * </code> after that.
   */
  private JComboBox<PSSecurityProviderInstanceSummary> m_instanceComboBox;

  /**
   * User name text field. Initialized in {@link #initDialog(Iterator)}, never <code>null</code>
   * after that.
   */
  private JTextField m_nameTextField;

  /** A reference back to the applet that initiated the action manager. */
  private PSContentExplorerApplet m_applet;
}
