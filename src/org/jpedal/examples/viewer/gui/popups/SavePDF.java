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
 * SavePDF.java
 * ---------------
 */
package org.jpedal.examples.viewer.gui.popups;

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

public class SavePDF extends Save {

	private static final long serialVersionUID = 6358862376179850200L;
	JLabel OutputLabel = new JLabel();
	ButtonGroup buttonGroup1 = new ButtonGroup();
	ButtonGroup buttonGroup2 = new ButtonGroup();

	JToggleButton jToggleButton3 = new JToggleButton();

	JToggleButton jToggleButton2 = new JToggleButton();

	JRadioButton printAll = new JRadioButton();
	JRadioButton printCurrent = new JRadioButton();
	JRadioButton printPages = new JRadioButton();

	JTextField pagesBox = new JTextField();

	JRadioButton exportSingle = new JRadioButton();
	JRadioButton exportMultiple = new JRadioButton();

	public SavePDF(String root_dir, int end_page, int currentPage) {

		super(root_dir, end_page, currentPage);

		try {
			jbInit();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * get root dir
	 */
	final public int[] getExportPages() {

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

	final public boolean getExportType() {
		return this.exportMultiple.isSelected();
	}

	private void jbInit() throws Exception {

		this.rootFilesLabel.setBounds(new Rectangle(13, 13, 400, 26));

		this.rootDir.setBounds(new Rectangle(23, 40, 232, 23));

		this.changeButton.setBounds(new Rectangle(272, 40, 101, 23));

		// rootDir.setBounds( new Rectangle( 23, 39, 232, 23 ) );
		// changeButton.setBounds( new Rectangle( 272, 39, 101, 23 ) );

		this.pageRangeLabel.setBounds(new Rectangle(13, 71, 300, 26));

		this.printAll.setText(Messages.getMessage("PdfViewerRadioButton.All"));
		this.printAll.setBounds(new Rectangle(23, 100, 75, 22));

		this.printCurrent.setText(Messages.getMessage("PdfViewerRadioButton.CurrentPage"));
		this.printCurrent.setBounds(new Rectangle(23, 120, 120, 22));
		this.printCurrent.setSelected(true);

		this.printPages.setText(Messages.getMessage("PdfViewerRadioButton.Pages"));
		this.printPages.setBounds(new Rectangle(23, 142, 70, 22));

		this.pagesBox.setBounds(new Rectangle(95, 142, 200, 22));
		this.pagesBox.addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(KeyEvent arg0) {}

			@Override
			public void keyReleased(KeyEvent arg0) {
				if (SavePDF.this.pagesBox.getText().length() == 0) SavePDF.this.printCurrent.setSelected(true);
				else SavePDF.this.printPages.setSelected(true);

			}

			@Override
			public void keyTyped(KeyEvent arg0) {}
		});

		JTextArea pagesInfo = new JTextArea(Messages.getMessage("PdfViewerMessage.PageNumberOrRange") + '\n'
				+ Messages.getMessage("PdfViewerMessage.PageRangeExample"));
		pagesInfo.setBounds(new Rectangle(23, 165, 400, 40));
		pagesInfo.setOpaque(false);

		this.optionsForFilesLabel.setBounds(new Rectangle(13, 220, 400, 26));

		this.exportMultiple.setText(Messages.getMessage("PdfViewerTitle.ExportMultiplePDFPages"));
		this.exportMultiple.setBounds(new Rectangle(23, 250, 350, 22));
		this.exportMultiple.setSelected(true);

		this.exportSingle.setText(Messages.getMessage("PdfViewerTitle.ExportSinglePDFPages"));
		this.exportSingle.setBounds(new Rectangle(23, 270, 300, 22));

		this.add(this.printAll, null);
		this.add(this.printCurrent, null);

		this.add(this.printPages, null);
		this.add(this.pagesBox, null);
		this.add(pagesInfo, null);

		this.add(this.optionsForFilesLabel, null);
		this.add(this.exportSingle, null);
		this.add(this.exportMultiple, null);

		this.add(this.rootDir, null);
		this.add(this.rootFilesLabel, null);
		this.add(this.changeButton, null);
		this.add(this.pageRangeLabel, null);

		this.add(this.jToggleButton2, null);
		this.add(this.jToggleButton3, null);

		this.buttonGroup1.add(this.printAll);
		this.buttonGroup1.add(this.printCurrent);
		this.buttonGroup1.add(this.printPages);

		this.buttonGroup2.add(this.exportMultiple);
		this.buttonGroup2.add(this.exportSingle);
	}

}
