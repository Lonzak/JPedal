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
 * DisplayOffsets.java
 * ---------------
 */
package org.jpedal;

// <start-adobe><start-thin><start-ulc>
import java.awt.Point;

import org.jpedal.examples.viewer.gui.SwingGUI;
import org.jpedal.external.ExternalHandlers;
import org.jpedal.external.Options;

//<end-ulc><end-thin><end-adobe>

public class DisplayOffsets {

	/** allow user to displace display */
	private int userOffsetX = 0, userOffsetY = 0, userPrintOffsetX = 0, userPrintOffsetY = 0;

	// store cursor position for facing drag
	private int facingCursorX = 10000, facingCursorY = 10000;

	public void setUserOffsets(int x, int y, int mode, ExternalHandlers externalHandlers, PdfDecoder pdfDecoder) {
		switch (mode) {

			case org.jpedal.external.OffsetOptions.DISPLAY:
				this.userOffsetX = x;
				this.userOffsetY = y;
				break;

			case org.jpedal.external.OffsetOptions.PRINTING:
				this.userPrintOffsetX = x;
				this.userPrintOffsetY = -y; // make it negative so both work in same direction
				break;

			// <start-adobe><start-thin><start-ulc>
			case org.jpedal.external.OffsetOptions.INTERNAL_DRAG_BLANK:
				this.facingCursorX = 0;
				this.facingCursorY = pdfDecoder.getHeight();
				SwingGUI gui1 = (SwingGUI) externalHandlers.getExternalHandler(Options.SwingContainer);
				if (gui1 != null) gui1.setDragCorner(mode);
				pdfDecoder.repaint();
				break;

			case org.jpedal.external.OffsetOptions.INTERNAL_DRAG_CURSOR_BOTTOM_LEFT:
				this.facingCursorX = x;
				this.facingCursorY = y;
				SwingGUI gui2 = (SwingGUI) externalHandlers.getExternalHandler(Options.SwingContainer);
				if (gui2 != null) gui2.setDragCorner(mode);
				pdfDecoder.repaint();
				break;

			case org.jpedal.external.OffsetOptions.INTERNAL_DRAG_CURSOR_BOTTOM_RIGHT:
				this.facingCursorX = x;
				this.facingCursorY = y;
				SwingGUI gui3 = (SwingGUI) externalHandlers.getExternalHandler(Options.SwingContainer);
				if (gui3 != null) gui3.setDragCorner(mode);
				pdfDecoder.repaint();
				break;

			case org.jpedal.external.OffsetOptions.INTERNAL_DRAG_CURSOR_TOP_LEFT:
				this.facingCursorX = x;
				this.facingCursorY = y;
				SwingGUI gui4 = (SwingGUI) externalHandlers.getExternalHandler(Options.SwingContainer);
				if (gui4 != null) gui4.setDragCorner(mode);
				pdfDecoder.repaint();
				break;

			case org.jpedal.external.OffsetOptions.INTERNAL_DRAG_CURSOR_TOP_RIGHT:
				this.facingCursorX = x;
				this.facingCursorY = y;
				SwingGUI gui5 = (SwingGUI) externalHandlers.getExternalHandler(Options.SwingContainer);
				if (gui5 != null) gui5.setDragCorner(mode);
				pdfDecoder.repaint();
				break;
			// <end-ulc><end-thin><end-adobe>

			default:
				throw new RuntimeException("No such mode - look in org.jpedal.external.OffsetOptions for valid values");
		}
	}

	public Point getUserOffsets(int mode) {

		switch (mode) {

			case org.jpedal.external.OffsetOptions.DISPLAY:
				return new Point(this.userOffsetX, this.userOffsetY);

			case org.jpedal.external.OffsetOptions.PRINTING:
				return new Point(this.userPrintOffsetX, this.userPrintOffsetY);

			case org.jpedal.external.OffsetOptions.INTERNAL_DRAG_CURSOR_BOTTOM_RIGHT:
				return new Point(this.facingCursorX, this.facingCursorY);

			default:
				throw new RuntimeException("No such mode - look in org.jpedal.external.OffsetOptions for valid values");
		}
	}

	public int getUserPrintOffsetX() {
		return this.userPrintOffsetX;
	}

	public int getUserPrintOffsetY() {
		return this.userPrintOffsetY;
	}

	public int getUserOffsetX() {
		return this.userOffsetX;
	}

	public int getUserOffsetY() {
		return this.userOffsetY;
	}
}
