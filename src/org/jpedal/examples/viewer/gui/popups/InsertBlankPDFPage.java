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
 * InsertBlankPDFPage.java
 * ---------------
 */
package org.jpedal.examples.viewer.gui.popups;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JToggleButton;

import org.jpedal.examples.viewer.Viewer;
import org.jpedal.utils.LogWriter;
import org.jpedal.utils.Messages;

public class InsertBlankPDFPage extends Save {

	private static final long serialVersionUID = -3183431115979663148L;
	JLabel OutputLabel = new JLabel();
	ButtonGroup buttonGroup1 = new ButtonGroup();
	// ButtonGroup buttonGroup2 = new ButtonGroup();

	JToggleButton jToggleButton3 = new JToggleButton();

	JToggleButton jToggleButton2 = new JToggleButton();

	JRadioButton addToEnd = new JRadioButton();
	JRadioButton addBeforePage = new JRadioButton();

	public InsertBlankPDFPage(String root_dir, int end_page, int currentPage) {
		super(root_dir, end_page, currentPage);

		try {
			jbInit();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	// /////////////////////////////////////////////////////////////////////
	/**
	 * get root dir
	 */
	final public int getInsertBefore() {

		int page = -1;

		if (this.addBeforePage.isSelected()) {
			try {
				page = Integer.parseInt(this.startPage.getText());
			}
			catch (Exception e) {
				LogWriter.writeLog(Messages.getMessage("PdfViewerError.Exception") + ' ' + e + ' '
						+ Messages.getMessage("PdfViewerError.ExportError"));
				if (Viewer.showMessages) JOptionPane.showMessageDialog(this, Messages.getMessage("PdfViewerError.InvalidSyntax"));
			}

			if (page < 1) {
				if (Viewer.showMessages) JOptionPane.showMessageDialog(this, Messages.getMessage("PdfViewerError.NegativePageValue"));
			}
			if (page > this.end_page) {
				if (Viewer.showMessages) JOptionPane.showMessageDialog(this,
						Messages.getMessage("PdfViewerText.Page") + ' ' + page + ' ' + Messages.getMessage("PdfViewerError.OutOfBounds") + ' '
								+ Messages.getMessage("PdfViewerText.PageCount") + ' ' + this.end_page);

				page = -1;
			}
		}
		else page = -2;

		return page;
	}

	private void jbInit() throws Exception {

		this.pageRangeLabel.setText(Messages.getMessage("PdfViewerTitle.Location"));
		this.pageRangeLabel.setBounds(new Rectangle(13, 13, 199, 26));

		this.addToEnd.setText(Messages.getMessage("PdfViewerTitle.AddPageToEnd"));
		this.addToEnd.setBounds(new Rectangle(23, 42, 400, 22));
		this.addToEnd.setSelected(true);

		this.addBeforePage.setText(Messages.getMessage("PdfViewerTitle.InsertBeforePage"));
		this.addBeforePage.setBounds(new Rectangle(23, 70, 150, 22));

		this.startPage.setBounds(new Rectangle(175, 70, 75, 22));
		this.startPage.setText("");
		this.startPage.addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(KeyEvent arg0) {}

			@Override
			public void keyReleased(KeyEvent arg0) {
				if (InsertBlankPDFPage.this.startPage.getText().length() == 0) InsertBlankPDFPage.this.addToEnd.setSelected(true);
				else InsertBlankPDFPage.this.addBeforePage.setSelected(true);

			}

			@Override
			public void keyTyped(KeyEvent arg0) {}
		});

		this.add(this.changeButton, null);
		this.add(this.pageRangeLabel, null);

		this.add(this.addToEnd, null);
		this.add(this.addBeforePage, null);

		this.add(this.startPage, null);

		this.add(this.jToggleButton2, null);
		this.add(this.jToggleButton3, null);

		this.buttonGroup1.add(this.addToEnd);
		this.buttonGroup1.add(this.addBeforePage);
	}

	@Override
	final public Dimension getPreferredSize() {
		return new Dimension(350, 180);
	}
}
