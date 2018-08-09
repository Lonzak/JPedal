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
 * LayoutEditor.java
 * ---------------
 */

package org.jpedal.examples.viewer.javabean;

import java.beans.PropertyEditorSupport;

public class LayoutEditor extends PropertyEditorSupport {

	// public LayoutEditor() {
	// setSource(this);
	// }
	//
	// public LayoutEditor(Object source) {
	// if (source == null) {
	// throw new NullPointerException();
	// }
	// setSource(source);
	// }

	@Override
	public String[] getTags() {
		return new String[] { "Single", "Continuous", "Continuous-Facing", "Facing" };
	}

	@Override
	public void setAsText(String s) {
		setValue(String.valueOf(s));
	}

	// public void setAsText(String s) {
	// if (s.equals("0"))
	// setValue(new Integer(0));
	// else if (s.equals("90"))
	// setValue(new Integer(90));
	// else if (s.equals("180"))
	// setValue(new Integer(180));
	// else if (s.equals("270"))
	// setValue(new Integer(270));
	// else
	// throw new IllegalArgumentException(s);
	// }

	// public String getJavaInitializationString() {
	// switch (((Number) getValue()).intValue()) {
	// default:
	// case 0:
	// return "0";
	// case 90:
	// return "90";
	// case 180:
	// return "180";
	// case 270:
	// return "270";
	// }
	// }
}
