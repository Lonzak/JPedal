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
 * SwingCheckBoxMenuItem.java
 * ---------------
 */
package org.jpedal.examples.viewer.gui.swing;

import javax.swing.JCheckBoxMenuItem;

/** extended Menu with id stored internally so listener can identify commands */
public class SwingCheckBoxMenuItem extends JCheckBoxMenuItem implements SwingID {

	private static final long serialVersionUID = -8469478352245268523L;
	private int ID;

	public SwingCheckBoxMenuItem(String text) {
		super(text);
	}

	/**
	 * @return the iD
	 */
	@Override
	public int getID() {
		return this.ID;
	}

	/**
	 * @param id
	 *            the iD to set
	 */
	@Override
	public void setID(int id) {
		this.ID = id;
	}
}
