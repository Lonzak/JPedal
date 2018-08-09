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
 * CheckNode.java
 * ---------------
 */

package org.jpedal.examples.viewer.gui;

import javax.swing.tree.DefaultMutableTreeNode;

public class CheckNode extends DefaultMutableTreeNode {

	private static final long serialVersionUID = -21248842224901044L;
	protected int selectionMode;
	protected boolean isSelected, isEnabled;
	private Object text;

	public CheckNode() {
		this(null);
	}

	public CheckNode(Object userObject) {
		this(userObject, true, false);

		this.text = userObject;
	}

	public CheckNode(Object userObject, boolean allowsChildren, boolean isSelected) {
		super(userObject, allowsChildren);
		this.isSelected = isSelected;
		this.selectionMode = 0;

		this.text = userObject;
	}

	public Object getText() {
		return this.text;
	}

	public void setSelectionMode(int mode) {
		this.selectionMode = mode;
	}

	public int getSelectionMode() {
		return this.selectionMode;
	}

	public void setSelected(boolean isSelected) {
		this.isSelected = isSelected;
	}

	public boolean isSelected() {
		return this.isSelected;
	}

	public void setEnabled(boolean isEnabled) {
		this.isEnabled = isEnabled;
	}

	public boolean isEnabled() {
		return this.isEnabled;
	}
}
