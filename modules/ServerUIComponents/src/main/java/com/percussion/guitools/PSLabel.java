/*
 * Copyright 1999-2025 Percussion Software, Inc.
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

package com.percussion.guitools;

import java.awt.*;
import java.awt.font.TextAttribute;
import java.util.Map;
import javax.swing.*;

/**
 * The default font used by <code>JLabel</code> (family - Dialog, size - 12, weight - Regular) is
 * unable to display special characters such as TM symbol. This class sets the font family to
 * "Arial" which is able to display such characters. This class mimics all constructors of <code>
 * JLabel</code> for ease of use.
 */
public class PSLabel extends JLabel {
  /** Default constructor; see {@link JLabel#JLabel()}. */
  public PSLabel() {
    super();
    init();
  }

  /**
   * Constructs a label displaying the supplied icon; see {@link JLabel#JLabel(Icon)}.
   *
   * @param image the icon to display; may be {@code null}.
   */
  public PSLabel(Icon image) {
    super(image);
    init();
  }

  /**
   * Constructs a label displaying the supplied icon and alignment; see {@link JLabel#JLabel(Icon,
   * int)}.
   *
   * @param image the icon to display; may be {@code null}.
   * @param horizontalAlignment one of the {@code SwingConstants} horizontal alignment values.
   */
  public PSLabel(Icon image, int horizontalAlignment) {
    super(image, horizontalAlignment);
    init();
  }

  /**
   * Constructs a label displaying the supplied text; see {@link JLabel#JLabel(String)}.
   *
   * @param text the label text; may be {@code null}.
   */
  public PSLabel(String text) {
    super(text);
    init();
  }

  /**
   * Constructs a label displaying the supplied text, icon, and alignment; see {@link
   * JLabel#JLabel(String, Icon, int)}.
   *
   * @param text the label text; may be {@code null}.
   * @param icon the icon to display; may be {@code null}.
   * @param horizontalAlignment one of the {@code SwingConstants} horizontal alignment values.
   */
  public PSLabel(String text, Icon icon, int horizontalAlignment) {
    super(text, icon, horizontalAlignment);
    init();
  }

  /**
   * Constructs a label displaying the supplied text with the given alignment; see {@link
   * JLabel#JLabel(String, int)}.
   *
   * @param text the label text; may be {@code null}.
   * @param horizontalAlignment one of the {@code SwingConstants} horizontal alignment values.
   */
  public PSLabel(String text, int horizontalAlignment) {
    super(text, horizontalAlignment);
    init();
  }

  /** Sets the font family to "Arial". Does not modify any other font attribute. */
  private void init() {
    Map fontAttr = getFont().getAttributes();
    fontAttr.put(TextAttribute.FAMILY, "Arial");
    setFont(new Font(fontAttr));
  }
}
