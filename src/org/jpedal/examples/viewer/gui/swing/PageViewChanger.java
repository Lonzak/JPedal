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
 * PageViewChanger.java
 * ---------------
 */
package org.jpedal.examples.viewer.gui.swing;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.SwingUtilities;

import org.jpedal.PdfDecoder;

/** used from 2.8 onwards in views with multiple pages to setup new view settings from menu when option choosen */
public class PageViewChanger implements ActionListener {

	int id, alignment;
	private PdfDecoder decode_pdf;

	public PageViewChanger(int alignment, int i, PdfDecoder decode_pdf) {
		this.id = i;
		this.alignment = alignment;
		this.decode_pdf = decode_pdf;
	}

	@Override
	public void actionPerformed(ActionEvent e) {

		if (SwingUtilities.isEventDispatchThread()) {

			this.decode_pdf.setDisplayView(this.id, this.alignment);

		}
		else {
			final Runnable doPaintComponent = new Runnable() {

				@Override
				public void run() {
					PageViewChanger.this.decode_pdf.setDisplayView(PageViewChanger.this.id, PageViewChanger.this.alignment);
				}
			};
			SwingUtilities.invokeLater(doPaintComponent);
		}
		// decode_pdf.setDisplayView(id,alignment);
	}
}
