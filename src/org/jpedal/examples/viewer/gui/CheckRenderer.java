/*
 * ===========================================
 * Java Pdf Extraction Decoding Access Library
 * ===========================================
 *
 * Project Info:  http://www.idrsolutions.com
 * Help section for developers at http://www.idrsolutions.com/java-pdf-library-support/
 *
 * (C) Copyright 1997-2013, IDRsolutions and Contributors.
 *
 * 	This file is part of JPedal
 *
     This library is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 2.1 of the License, or (at your option) any later version.

    This library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public
    License along with this library; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA


 *
 * ---------------
 * CheckRenderer.java
 * ---------------
 */

package org.jpedal.examples.viewer.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;
import javax.swing.tree.TreeCellRenderer;

public class CheckRenderer extends JPanel implements TreeCellRenderer {

	private static final long serialVersionUID = 6604312347309529878L;
	protected JCheckBox check;
	protected TreeLabel label;

	public CheckRenderer() {
		setLayout(null);
		add(this.check = new JCheckBox());
		add(this.label = new TreeLabel());
		this.check.setBackground(UIManager.getColor("Tree.textBackground"));
	}

	@Override
	public Component getTreeCellRendererComponent(JTree tree, Object value, boolean isSelected, boolean expanded, boolean leaf, int row,
			boolean hasFocus) {

		String stringValue = tree.convertValueToText(value, isSelected, expanded, leaf, row, hasFocus);

		setEnabled(tree.isEnabled());

		if (value instanceof CheckNode) {

			this.check.setSelected(((CheckNode) value).isSelected());
			setEnabled(((CheckNode) value).isEnabled());
			this.check.setEnabled(((CheckNode) value).isEnabled());

			this.label.setFont(tree.getFont());
			this.label.setText(stringValue);
			this.label.setSelected(isSelected);
			this.label.setFocus(hasFocus);

			return this;
		}
		else return new JLabel(stringValue);
	}

	@Override
	public Dimension getPreferredSize() {
		Dimension d_check = this.check.getPreferredSize();
		Dimension d_label = this.label.getPreferredSize();
		return new Dimension(d_check.width + d_label.width, (d_check.height < d_label.height ? d_label.height : d_check.height));
	}

	@Override
	public void doLayout() {
		Dimension d_check = this.check.getPreferredSize();
		Dimension d_label = this.label.getPreferredSize();
		int y_check = 0;
		int y_label = 0;
		if (d_check.height < d_label.height) {
			y_check = (d_label.height - d_check.height) / 2;
		}
		else {
			y_label = (d_check.height - d_label.height) / 2;
		}
		this.check.setLocation(0, y_check);
		this.check.setBounds(0, y_check, d_check.width, d_check.height);
		this.label.setLocation(d_check.width, y_label);
		this.label.setBounds(d_check.width, y_label, d_label.width, d_label.height);
	}

	@Override
	public void setBackground(Color color) {
		if (color instanceof ColorUIResource) color = null;
		super.setBackground(color);
	}

	static class TreeLabel extends JLabel {

		private static final long serialVersionUID = -7189638268587448744L;
		boolean isSelected;
		boolean hasFocus;

		TreeLabel() {}

		@Override
		public void setBackground(Color color) {
			if (color instanceof ColorUIResource) color = null;
			super.setBackground(color);
		}

		@Override
		public void paint(Graphics g) {
			String str;
			if ((str = getText()) != null) {
				if (0 < str.length()) {
					if (this.isSelected) {
						g.setColor(UIManager.getColor("Tree.selectionBackground"));
					}
					else {
						g.setColor(UIManager.getColor("Tree.textBackground"));
					}
					Dimension d = getPreferredSize();
					int imageOffset = 0;
					Icon currentI = getIcon();
					if (currentI != null) {
						imageOffset = currentI.getIconWidth() + Math.max(0, getIconTextGap() - 1);
					}
					g.fillRect(imageOffset, 0, d.width - 1 - imageOffset, d.height);
					if (this.hasFocus) {
						g.setColor(UIManager.getColor("Tree.selectionBorderColor"));
						g.drawRect(imageOffset, 0, d.width - 1 - imageOffset, d.height - 1);
					}
				}
			}
			super.paint(g);
		}

		@Override
		public Dimension getPreferredSize() {
			Dimension retDimension = super.getPreferredSize();
			if (retDimension != null) {
				retDimension = new Dimension(retDimension.width + 3, retDimension.height);
			}
			return retDimension;
		}

		void setSelected(boolean isSelected) {
			this.isSelected = isSelected;
		}

		void setFocus(boolean hasFocus) {
			this.hasFocus = hasFocus;
		}
	}
}
