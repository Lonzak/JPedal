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
 * PDFListener.java
 * ---------------
 */
package org.jpedal.objects.acroforms.actions;

import org.jpedal.objects.acroforms.rendering.AcroRenderer;
import org.jpedal.objects.raw.FormObject;

/**
 * shared non component-specific code
 */
public class PDFListener {

	public static final boolean debugMouseActions = false;

	public FormObject formObject;
	public AcroRenderer acrorend;
	public ActionHandler handler;

	protected PDFListener(FormObject form, AcroRenderer acroRend, ActionHandler formsHandler) {
		this.formObject = form;
		this.acrorend = acroRend;
		this.handler = formsHandler;
	}

	public void mouseReleased(Object e) {
		if (debugMouseActions) System.out.println("PDFListener.mouseReleased()");
		this.handler.A(e, this.formObject, ActionHandler.MOUSERELEASED);
		this.handler.U(e, this.formObject);
	}

	public void mouseClicked(Object e) {
		if (debugMouseActions) System.out.println("PDFListener.mouseClicked()");
		this.handler.A(e, this.formObject, ActionHandler.MOUSECLICKED);
	}

	public void mousePressed(Object e) {
		if (debugMouseActions) System.out.println("PDFListener.mousePressed()");
		this.handler.A(e, this.formObject, ActionHandler.MOUSEPRESSED);
		this.handler.D(e, this.formObject);
	}

	public void keyReleased(Object e) {
		if (debugMouseActions) System.out.println("PDFListener.keyReleased(" + e + ')');
		this.handler.K(e, this.formObject, ActionHandler.MOUSERELEASED);
		this.handler.V(e, this.formObject, ActionHandler.MOUSERELEASED);
	}

	public void focusLost(Object e) {
		if (debugMouseActions) System.out.println("PDFListener.focusLost()");

		this.handler.Bl(e, this.formObject);
		this.handler.K(e, this.formObject, ActionHandler.FOCUS_EVENT);
		this.handler.V(e, this.formObject, ActionHandler.FOCUS_EVENT);
		// format the field value after it has been altered
		this.handler.F(this.formObject);

		// this is added in so that the popup forms do not flash with focus
		// but is causes forms focus to be lost unexpectadly.
		// acrorend.getCompData().loseFocus();
	}

	public void focusGained(Object e) {
		if (debugMouseActions) System.out.println("PDFListener.focusGained()");

		this.handler.Fo(e, this.formObject);

		// this needs to only be done on certain files, that specify this, not all.
		// user can enter some values (ie 1.10.2007 as its still valid for a date which are then turned into
		// 01.10.2007 when user quits field. If user re-enters form, this sets it back to 1.10.2007
		// String fieldRef = formObject.getPDFRef();
		String fieldRef = this.formObject.getObjectRefAsString();

		Object lastUnformattedValue = this.acrorend.getCompData().getLastUnformattedValue(fieldRef);
		if (lastUnformattedValue != null && !lastUnformattedValue.equals(this.acrorend.getCompData().getValue(fieldRef))) {
			this.acrorend.getCompData().setValue(fieldRef, lastUnformattedValue, false, false);
		}
	}
}