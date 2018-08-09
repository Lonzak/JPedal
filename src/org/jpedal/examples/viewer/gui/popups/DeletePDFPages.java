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
 * DeletePDFPages.java
 * ---------------
 */
package org.jpedal.examples.viewer.gui.popups;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.print.attribute.standard.PageRanges;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;

import org.jpedal.examples.viewer.Viewer;
import org.jpedal.utils.LogWriter;
import org.jpedal.utils.Messages;

public class DeletePDFPages extends Save {

	private static final long serialVersionUID = 7720446319401470639L;

	JLabel OutputLabel = new JLabel();
	ButtonGroup buttonGroup1 = new ButtonGroup();
	ButtonGroup buttonGroup2 = new ButtonGroup();

	JToggleButton jToggleButton3 = new JToggleButton();

	JToggleButton jToggleButton2 = new JToggleButton();

	JRadioButton printAll = new JRadioButton();
	JRadioButton printCurrent = new JRadioButton();
	JRadioButton printPages = new JRadioButton();

	JTextField pagesBox = new JTextField();

	public DeletePDFPages(String root_dir, int end_page, int currentPage) {
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
	final public int[] getDeletedPages() {

		int[] pagesToExport = null;

		if (this.printAll.isSelected()) {
			pagesToExport = new int[this.end_page];
			for (int i = 0; i < this.end_page; i++)
				pagesToExport[i] = i + 1;

		}
		else
			if (this.printCurrent.isSelected()) {
				pagesToExport = new int[1];
				pagesToExport[0] = this.currentPage;

			}
			else
				if (this.printPages.isSelected()) {

					try {
						PageRanges pages = new PageRanges(this.pagesBox.getText());

						int count = 0;
						int i = -1;
						while ((i = pages.next(i)) != -1)
							count++;

						pagesToExport = new int[count];
						count = 0;
						i = -1;
						while ((i = pages.next(i)) != -1) {
							if (i > this.end_page) {
								if (Viewer.showMessages) JOptionPane.showMessageDialog(this,
										Messages.getMessage("PdfViewerText.Page") + ' ' + i + ' ' + Messages.getMessage("PdfViewerError.OutOfBounds")
												+ ' ' + Messages.getMessage("PdfViewerText.PageCount") + ' ' + this.end_page);
								return null;
							}
							pagesToExport[count] = i;
							count++;
						}
					}
					catch (IllegalArgumentException e) {
						LogWriter.writeLog("Exception " + e + " in exporting pdfs");
						if (Viewer.showMessages) JOptionPane.showMessageDialog(this, Messages.getMessage("PdfViewerError.InvalidSyntax"));
					}
				}

		return pagesToExport;
	}

	private void jbInit() throws Exception {// 58

		this.pageRangeLabel.setText(Messages.getMessage("PdfViewerPageRange.text"));
		this.pageRangeLabel.setBounds(new Rectangle(13, 13, 199, 26));

		// printAll.setText(Messages.getMessage("PdfViewerRadioButton.All"));
		// printAll.setBounds( new Rectangle( 23, 42, 75, 22 ) );

		this.printCurrent.setText(Messages.getMessage("PdfViewerRadioButton.CurrentPage"));
		this.printCurrent.setBounds(new Rectangle(23, 62, 100, 22));
		this.printCurrent.setSelected(true);

		this.printPages.setText(Messages.getMessage("PdfViewerRadioButton.Pages"));
		this.printPages.setBounds(new Rectangle(23, 84, 70, 22));

		this.pagesBox.setBounds(new Rectangle(95, 84, 200, 22));
		this.pagesBox.addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(KeyEvent arg0) {
				DeletePDFPages.this.jToggleButton2.setSelected(true);

			}

			@Override
			public void keyReleased(KeyEvent arg0) {
				if (DeletePDFPages.this.pagesBox.getText().length() == 0) DeletePDFPages.this.printCurrent.setSelected(true);
				else DeletePDFPages.this.printPages.setSelected(true);

			}

			@Override
			public void keyTyped(KeyEvent arg0) {}
		});

		JTextArea pagesInfo = new JTextArea(Messages.getMessage("PdfViewerMessage.PageNumberOrRange") + '\n'
				+ Messages.getMessage("PdfViewerMessage.PageRangeExample"));
		pagesInfo.setBounds(new Rectangle(15, 115, 400, 40));
		pagesInfo.setOpaque(false);

		this.optionsForFilesLabel.setBounds(new Rectangle(13, 168, 199, 26));

		this.add(this.printAll, null);
		this.add(this.printCurrent, null);

		this.add(this.printPages, null);
		this.add(this.pagesBox, null);
		this.add(pagesInfo, null);

		// this.add( directionBox, null );
		// this.add( direction, null );
		this.add(this.changeButton, null);
		this.add(this.pageRangeLabel, null);

		this.add(this.jToggleButton2, null);
		this.add(this.jToggleButton3, null);

		this.buttonGroup1.add(this.printAll);
		this.buttonGroup1.add(this.printCurrent);
		this.buttonGroup1.add(this.printPages);
	}

	@Override
	final public Dimension getPreferredSize() {
		return new Dimension(370, 180);
	}
}
