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
 * SwingMousePanMode.java
 * ---------------
 */

package org.jpedal.examples.viewer.gui.swing;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

import javax.swing.SwingUtilities;

import org.jpedal.PdfDecoder;
import org.jpedal.examples.viewer.gui.SwingGUI;
import org.jpedal.external.Options;

public class SwingMousePanMode implements SwingMouseFunctionality {

	private Point currentPoint;
	private PdfDecoder decode_pdf;
	private Rectangle currentView;

	public SwingMousePanMode(PdfDecoder decode_pdf) {
		this.decode_pdf = decode_pdf;
	}

	public void setupMouse() {
		/**
		 * track and display screen co-ordinates and support links
		 */

		// set cursor
		SwingGUI gui = ((SwingGUI) this.decode_pdf.getExternalHandler(Options.SwingContainer));
		this.decode_pdf.setCursor(gui.getCursor(SwingGUI.GRAB_CURSOR));
	}

	@Override
	public void mouseClicked(MouseEvent arg0) {
	}

	@Override
	public void mouseEntered(MouseEvent arg0) {
	}

	@Override
	public void mouseExited(MouseEvent arg0) {
	}

	@Override
	public void mousePressed(MouseEvent arg0) {
		if (arg0.getButton() == MouseEvent.BUTTON1 || arg0.getButton() == MouseEvent.NOBUTTON) {
			this.currentPoint = arg0.getPoint();
			this.currentView = this.decode_pdf.getVisibleRect();

			// set cursor
			SwingGUI gui = ((SwingGUI) this.decode_pdf.getExternalHandler(Options.SwingContainer));
			this.decode_pdf.setCursor(gui.getCursor(SwingGUI.GRABBING_CURSOR));
		}
	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
		// reset cursor
		SwingGUI gui = ((SwingGUI) this.decode_pdf.getExternalHandler(Options.SwingContainer));
		this.decode_pdf.setCursor(gui.getCursor(SwingGUI.GRAB_CURSOR));
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		if (SwingUtilities.isLeftMouseButton(e)) {
			final Point newPoint = e.getPoint();

			int diffX = this.currentPoint.x - newPoint.x;
			int diffY = this.currentPoint.y - newPoint.y;

			Rectangle view = this.currentView;

			view.x += diffX;

			view.y += diffY;

			if (!view.contains(this.decode_pdf.getVisibleRect())) this.decode_pdf.scrollRectToVisible(view);
		}
	}

	@Override
	public void mouseMoved(MouseEvent e) {
	}
}