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
 * ActionHandler.java
 * ---------------
 */
package org.jpedal.objects.acroforms.actions;

import java.awt.event.MouseEvent;

import org.jpedal.PdfDecoder;
import org.jpedal.objects.Javascript;
import org.jpedal.objects.acroforms.rendering.AcroRenderer;
import org.jpedal.objects.layers.PdfLayerList;
import org.jpedal.objects.raw.FormObject;
import org.jpedal.objects.raw.PdfObject;

public interface ActionHandler {

	final public static boolean drawPopups = true;

	final public static int MOUSEPRESSED = 1;
	final public static int MOUSERELEASED = 2;
	final public static int MOUSECLICKED = 3;
	final public static int MOUSEENTERED = 4;
	final public static int MOUSEEXITED = 5;

	final public static int FOCUS_EVENT = 6;

	final public static int TODO = -1;

	final public static int NOMESSAGE = 0;
	final public static int REJECTKEY = 1;
	final public static int STOPPROCESSING = 2;
	final public static int VALUESCHANGED = 3;

	/**
	 * A action when pressed in active area ?some others should now be ignored?
	 */
	public void A(Object e, FormObject formObject, int eventType);

	/**
	 * E action when cursor enters active area
	 */
	public void E(Object e, FormObject formObject);

	/**
	 * X action when cursor exits active area
	 */
	public void X(Object e, FormObject formObject);

	/**
	 * D action when cursor button pressed inside active area
	 */
	public void D(Object e, FormObject formObj);

	/**
	 * U action when cursor button released inside active area
	 */
	public void U(Object e, FormObject formObj);

	/**
	 * Fo action on input focus
	 */
	public void Fo(Object e, FormObject formObj);

	/**
	 * Bl action when input focus lost
	 */
	public void Bl(Object e, FormObject formObj);

	/**
	 * PO action when page containing is opened, action O from page dictionary, and OpenAction in document catalog should be done first
	 */
	public void PO(PdfObject pdfObject, int type);

	/**
	 * O action when page containing is opened,
	 */
	public void O(PdfObject pdfObject, int type);

	/**
	 * PC action when page is closed
	 */
	public void PC(PdfObject pdfObject, int type);

	/**
	 * PV action on viewing containing page
	 */
	public void PV(PdfObject pdfObject, int type);

	/**
	 * PI action when no longer visible in viewer
	 */
	public void PI(PdfObject pdfObject, int type);

	/**
	 * K action on - [javascript] keystroke in textfield or combobox modifys the list box selection (can access the keystroke for validity and reject
	 * or modify)
	 */
	public int K(Object e, FormObject formObject, int actionID);

	/**
	 * F the display formatting of the field (e.g 2 decimal places) [javascript]
	 */
	public void F(FormObject formObject);

	/**
	 * V action when fields value is changed [javascript]
	 */
	public void V(Object e, FormObject formObject, int actionID);

	/**
	 * C action when another field changes (recalculate this field) [javascript] should not be called other than from internal methods to action
	 * changes on other fields.
	 */
	public void C(FormObject formObject);

	public PdfDecoder getPDFDecoder();

	/**
	 * creates a returns an action listener that will change the down icon for each click <br>
	 * 2 icons that need to be changed when the button is sellected and not selected, so that when the button is pressed the appropriate icon is shown
	 * correctly
	 * 
	 * (ms) 09-10-08 Added rotation parameter
	 */
	public Object setupChangingDownIcon(Object downOff, Object downOn, int rotation);

	/**
	 * setup mouse actions to allow the text of the button to change with the captions provided <br>
	 * should change the caption as the moouse actions occure on the field
	 */
	public Object setupChangingCaption(String normalCaption, String rolloverCaption, String downCaption);

	/**
	 * setup hand cursor when hovering and reset, on exiting
	 */
	public Object setHoverCursor();

	public void init(PdfDecoder panel, Javascript javascript, AcroRenderer defaultAcroRenderer);

	void setPageAccess(int pageHeight, int insetH);

	void setActionFactory(ActionFactory actionFactory);

	public ActionFactory getActionFactory();

	public PdfLayerList getLayerHandler();

	// <start-adobe><start-thin>
	public void setMouseHandler(org.jpedal.examples.viewer.gui.swing.SwingMouseListener swingMouseHandler);

	public void updateCordsFromFormComponent(MouseEvent e, boolean mousePressed);

	public void changeTo(String file, int page, Object location, Integer type, boolean storeView);

	// <end-thin><end-adobe>

	// allow Swing client to plug into Dest handling code returning page as may change
	public int gotoDest(PdfObject aobj, int mouseclicked, int dest);
}
