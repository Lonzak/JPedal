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
 * FrameCloser.java
 * ---------------
 */
package org.jpedal.examples.viewer.gui.swing;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.jpedal.PdfDecoder;
import org.jpedal.examples.viewer.Commands;
import org.jpedal.examples.viewer.Values;
import org.jpedal.examples.viewer.Viewer;
import org.jpedal.examples.viewer.gui.generic.GUIThumbnailPanel;
import org.jpedal.examples.viewer.utils.Printer;
import org.jpedal.examples.viewer.utils.PropertiesFile;
import org.jpedal.gui.GUIFactory;
import org.jpedal.utils.Messages;

/** cleanly shutdown if user closes window */
public class FrameCloser extends WindowAdapter {

	private Commands currentCommands;
	GUIFactory currentGUI;
	PdfDecoder decode_pdf;
	private Printer currentPrinter;
	GUIThumbnailPanel thumbnails;
	Values commonValues;
	PropertiesFile properties;

	public FrameCloser(Commands currentCommands, GUIFactory currentGUI, PdfDecoder decode_pdf, Printer currentPrinter, GUIThumbnailPanel thumbnails,
			Values commonValues, PropertiesFile properties) {
		this.currentCommands = currentCommands;
		this.currentGUI = currentGUI;
		this.decode_pdf = decode_pdf;
		this.currentPrinter = currentPrinter;
		this.thumbnails = thumbnails;
		this.commonValues = commonValues;
		this.properties = properties;
	}

	@Override
	public void windowClosing(WindowEvent e) {

		try {
			this.properties.setValue("lastDocumentPage", String.valueOf(this.commonValues.getCurrentPage()));
			// properties.writeDoc();
		}
		catch (Exception e1) {
			// TODO Auto-generated catch block
		}

		if (Printer.isPrinting()) this.currentGUI.showMessageDialog(Messages.getMessage("PdfViewerBusyPrinting.message"));

		if (!Values.isProcessing()) {

			// tell our code to exit cleanly asap
			this.thumbnails.terminateDrawing();

			int confirm = 0;
			if (this.currentGUI.confirmClose()) confirm = this.currentGUI.showConfirmDialog(Messages.getMessage("PdfViewerCloseing.message"), null,
					JOptionPane.YES_NO_OPTION);

			if (confirm == 0) {

				// <start-wrap>
				/**
				 * warn user on forms
				 */
				this.currentCommands.handleUnsaveForms();
				// <end-wrap>

				this.decode_pdf.closePdfFile();

				if (Viewer.exitOnClose) System.exit(0);
				else {
					this.currentGUI.getFrame().setVisible(false);
					if (this.currentGUI.getFrame() instanceof JFrame) {
						((JFrame) this.currentGUI.getFrame()).dispose();
					}
				}
			}

		}
		else {

			this.currentGUI.showMessageDialog(Messages.getMessage("PdfViewerDecodeWait.message"));
		}
	}
}
