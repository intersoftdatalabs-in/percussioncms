/*
 * Copyright 1999-2023 Percussion Software, Inc.
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

package com.percussion.ImageListControl;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Insets;
import java.io.Serializable;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.ListCellRenderer;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

/** The renderer that uses the data within the ImageListControlModel and
  * prepares the data to be displayed onscreen as a GUI component.
*/

class ImageListControlRenderer extends JPanel
                               implements ListCellRenderer, Serializable
{
  public ImageListControlRenderer()
  {
    super();
    noFocusBorder = new EmptyBorder(5, 5, 5, 5);
     setOpaque(true);
     setBorder(noFocusBorder);

    m_image = new JLabel();
    m_textLabel = new JTextArea();
    m_textLabel.setBorder(new EmptyBorder(3,3,3,3));

    setLayout(new FlowLayout());
    add(m_image);
    add(m_textLabel);
  }

/** Implementation of ListCellRenderer interface for the special layout of this
  * horizontal image list.
*/
  public Component getListCellRendererComponent(JList list,
                                                 Object value,
                                                int index,
                                                boolean isSelected,
                                                boolean cellHasFocus)
  {
    m_image.setIcon(((ImageListItem)value).getImage());
    m_image.setEnabled(list.isEnabled());

    m_textLabel.setRows(1);
    m_textLabel.setColumns(10);
    m_textLabel.setEditable(false);
    m_textLabel.setLineWrap(true);
    m_textLabel.setWrapStyleWord(true);

    if (isSelected)
    {
       setBackground(list.getSelectionBackground());
      m_image.setBackground(list.getSelectionBackground());
      m_textLabel.setBackground(list.getSelectionBackground());
       m_textLabel.setForeground(list.getSelectionForeground());
    }
     else
    {
       setBackground(list.getBackground());
      m_image.setBackground(list.getBackground());
      m_textLabel.setBackground(list.getBackground());
       m_textLabel.setForeground(list.getForeground());
    }

    String text = ((ImageListItem)value).getText();

    // setting basic font sizes, text string dimensions and the image dimensions
     m_textLabel.setFont(list.getFont());
    m_fontMetrics = m_textLabel.getFontMetrics(m_textLabel.getFont());
    
    int textHeight = m_fontMetrics.getHeight();
    int textWidth = m_fontMetrics.stringWidth(text);

    int iconHeight = ((ImageListItem)value).getImage().getIconHeight();
    int iconWidth = ((ImageListItem)value).getImage().getIconWidth();

    // save the number of characters where the text string is greater
    // than the (image.width * 2).
    int nChars;
    int totalWidth = 0;
    for(nChars = 0; nChars < text.length(); nChars++)
    {
      totalWidth += m_fontMetrics.charWidth(text.charAt(nChars));
      if (totalWidth > iconWidth * 2)
      {
        break;
      }
    }

    // if the text string is actually greater than (image.width * 4); meaning
    // the text is greater than 2 lines.
    if (textWidth > iconWidth * 4)
    {
      int totalChars;
      // *** not sure if I should do this... ***  (column.width != char.width)
      if (nChars >= 10)
        m_textLabel.setColumns(nChars - (nChars/2 - 1));
      else
        m_textLabel.setColumns(nChars);
      // ***
      String endString = new String("...");

      m_textLabel.setRows(3);
    }
    // if the width of the text string is greater than (image.width * 2).
    else if (textWidth > iconWidth * 2 && textWidth <= iconWidth * 4)
    {
      // *** not sure if I should do this... ***  (column.width != char.width)
      if (nChars >= 10)
        m_textLabel.setColumns(nChars - (nChars/2 - 1));
      else
        m_textLabel.setColumns(nChars);
      // ***

      m_textLabel.setRows(2);
    }
    else // if text width is < image * 2 (height would be 1 row)
    {
      m_textLabel.setRows(1);

      if (nChars >= 10)
        m_textLabel.setColumns(nChars - (nChars/2 - 1));
      else
        m_textLabel.setColumns(nChars);
    }

    textHeight = m_textLabel.getPreferredScrollableViewportSize().height;
    textWidth = m_textLabel.getPreferredScrollableViewportSize().width;

    Insets imageInsets = m_image.getInsets();
    Insets textInsets = m_textLabel.getInsets();

    // *** combining the sizes of the image and the textarea and set it as the
    // preferred size of the JPanel holding these 2 components ***

    // if the icon is wider than the text
    if (iconWidth > textWidth)
    {
      int height = textHeight + textInsets.top + textInsets.bottom;
      int width = iconWidth + imageInsets.left + imageInsets.right;

      m_textLabel.setPreferredSize(new Dimension(width, height));
      setPreferredSize(new Dimension(width, height + iconHeight + imageInsets.top + imageInsets.bottom));
    }
    // else, the text is wider than the icon
    else
    {
      int height = textHeight + textInsets.top + textInsets.bottom;
      int width = textWidth + textInsets.left + textInsets.right;

      m_textLabel.setPreferredSize(new Dimension(width, height));
      setPreferredSize(new Dimension(width, height + iconHeight + imageInsets.top + imageInsets.bottom));
    }


    m_textLabel.setText(text);


    return this;

  }

//
// MEMBER VARIABLES
//

  protected static Border noFocusBorder;

  protected JLabel m_image = null;

  protected JTextArea m_textLabel = null;

  protected FontMetrics m_fontMetrics = null;
}

