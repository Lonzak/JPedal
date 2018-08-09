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
 * MultiViewListener.java
 * ---------------
 */

package org.jpedal.examples.viewer;

import java.io.File;

import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;

import org.jpedal.PdfDecoder;
import org.jpedal.examples.viewer.gui.SwingGUI;

public class MultiViewListener implements InternalFrameListener {

	Object pageScaling = null, pageRotation = null;
	private PdfDecoder decode_pdf;
	private SwingGUI currentGUI;
	private Values commonValues;
	private Commands currentCommands;

	public MultiViewListener(PdfDecoder decode_pdf, SwingGUI currentGUI, Values commonValues, Commands currentCommands) {
		this.decode_pdf = decode_pdf;
		this.currentGUI = currentGUI;
		this.commonValues = commonValues;
		this.currentCommands = currentCommands;

		// pageScaling = "Window";
		// pageRotation = currentGUI.getRotation();

		// System.out.println("constructor"+ " "+pageScaling+" " +pageRotation);
	}

	@Override
	public void internalFrameOpened(InternalFrameEvent e) {
		// To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void internalFrameClosing(InternalFrameEvent e) {
		this.currentGUI.setBackNavigationButtonsEnabled(false);
		this.currentGUI.setForwardNavigationButtonsEnabled(false);
		this.currentGUI.resetPageNav();
	}

	@Override
	public void internalFrameClosed(InternalFrameEvent e) {

		this.decode_pdf.flushObjectValues(true);

		this.decode_pdf.closePdfFile();
	}

	@Override
	public void internalFrameIconified(InternalFrameEvent e) {
		// To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void internalFrameDeiconified(InternalFrameEvent e) {
		// To change body of implemented methods use File | Settings | File
		// Templates.
	}

	/**
	 * switch to active PDF
	 * 
	 * @param e
	 */
	@Override
	public void internalFrameActivated(InternalFrameEvent e) {

		// System.out.println("activated pdf = "+decode_pdf.getClass().getName() + "@" + Integer.toHexString(decode_pdf.hashCode()));
		// choose selected PDF
		this.currentGUI.setPdfDecoder(this.decode_pdf);
		this.currentCommands.setPdfDecoder(this.decode_pdf);
		/**
		 * align details in Viewer and variables
		 */
		int page = this.decode_pdf.getlastPageDecoded();

		this.commonValues.setPageCount(this.decode_pdf.getPageCount());
		this.commonValues.setCurrentPage(page);

		String fileName = this.decode_pdf.getFileName();
		if (fileName != null) {
			this.commonValues.setSelectedFile(fileName);
			File file = new File(fileName);
			this.commonValues.setInputDir(file.getParent());
			this.commonValues.setFileSize(file.length() >> 10);
		}

		// System.err.println("ACTIVATED "+pageScaling+" "+pageRotation+"
		// count="+decode_pdf.getPageCount()/*+"
		// "+localPdf.getDisplayRotation()+" "+localPdf.getDisplayScaling()*/);

		this.commonValues.setPDF(this.currentCommands.isPDF());
		this.commonValues.setMultiTiff(this.currentGUI.isMultiPageTiff());

		// System.err.println("ACTIVATED "+pageScaling+" "+pageRotation+" count="+decode_pdf.getPageCount()/*+" "+localPdf.getDisplayRotation()+" "+localPdf.getDisplayScaling()*/);

		if (this.pageScaling != null) this.currentGUI.setSelectedComboItem(Commands.SCALING, this.pageScaling.toString());

		if (this.pageRotation != null) this.currentGUI.setSelectedComboItem(Commands.ROTATION, this.pageRotation.toString());

		// currentGUI.setPage(page);
		// //pageCounter2.setText(""+page);

		// pageCounter3.setText(""+decode_pdf.getPageCount());

		this.currentGUI.setPageNumber();

		this.decode_pdf.updateUI();

		this.currentGUI.removeSearchWindow(false);
		// searchFrame.removeSearchWindow(false);

		// Only show navigation buttons required for newly activated frame
		this.currentGUI.hideRedundentNavButtons();
	}

	@Override
	public void internalFrameDeactivated(InternalFrameEvent e) {

		// save current settings
		if (this.pageScaling != null) {
			this.pageScaling = this.currentGUI.getSelectedComboItem(Commands.SCALING);
		}
		if (this.pageRotation != null) {
			this.pageRotation = this.currentGUI.getSelectedComboItem(Commands.ROTATION);
		}
		// System.err.println("DEACTIVATED "+pageScaling+" "+pageRotation);
	}

	public void setPageProperties(Object rotation, Object scaling) {
		this.pageRotation = rotation;
		this.pageScaling = scaling;
	}
}
