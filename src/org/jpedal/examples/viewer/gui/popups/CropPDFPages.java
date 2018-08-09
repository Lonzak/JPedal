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
 * CropPDFPages.java
 * ---------------
 */
package org.jpedal.examples.viewer.gui.popups;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.print.attribute.standard.PageRanges;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;

import org.jpedal.examples.viewer.Viewer;
import org.jpedal.utils.LogWriter;
import org.jpedal.utils.Messages;

public class CropPDFPages extends Save {

	private static final long serialVersionUID = -2546648206602573759L;
	JLabel OutputLabel = new JLabel();
	ButtonGroup buttonGroup1 = new ButtonGroup();
	ButtonGroup buttonGroup2 = new ButtonGroup();

	JToggleButton jToggleButton3 = new JToggleButton();

	JToggleButton jToggleButton2 = new JToggleButton();

	JSpinner bottomMargin = new JSpinner(new SpinnerNumberModel(0.00, 0.00, 1000.00, 1.00));
	JSpinner topMargin = new JSpinner(new SpinnerNumberModel(0.00, 0.00, 1000.00, 1.00));
	JSpinner leftMargin = new JSpinner(new SpinnerNumberModel(0.00, 0.00, 1000.00, 1.00));
	JSpinner rightMargin = new JSpinner(new SpinnerNumberModel(0.00, 0.00, 1000.00, 1.00));

	JCheckBox applyToCurrent = new JCheckBox();

	JRadioButton printAll = new JRadioButton();
	JRadioButton printCurrent = new JRadioButton();
	JRadioButton printPages = new JRadioButton();

	JTextField pagesBox = new JTextField();

	public CropPDFPages(String root_dir, int end_page, int currentPage) {
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
	final public int[] getPages() {

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

	public boolean applyToCurrentCrop() {
		return this.applyToCurrent.isSelected();
	}

	public float[] getCrop() {
		float left = Float.parseFloat(this.leftMargin.getValue().toString());
		float bottom = Float.parseFloat(this.bottomMargin.getValue().toString());
		float right = Float.parseFloat(this.rightMargin.getValue().toString());
		float top = Float.parseFloat(this.topMargin.getValue().toString());

		return new float[] { left, bottom, right, top };
	}

	private void jbInit() throws Exception {

		JLabel textAndFont = new JLabel(Messages.getMessage("PdfViewerLabel.CropMargins"));
		textAndFont.setFont(new java.awt.Font("Dialog", Font.BOLD, 14));
		textAndFont.setDisplayedMnemonic('0');
		textAndFont.setBounds(new Rectangle(13, 13, 220, 26));

		JLabel jLabel1 = new JLabel(Messages.getMessage("PdfViewerLabel.Top"));
		jLabel1.setBounds(140, 50, 70, 15);

		this.topMargin.setBounds(200, 45, 60, 23);

		JLabel jLabel5 = new javax.swing.JLabel(Messages.getMessage("PdfViewerLabel.Left"));
		jLabel5.setBounds(25, 100, 50, 15);

		this.leftMargin.setBounds(70, 95, 60, 23);

		JLabel jLabel6 = new javax.swing.JLabel(Messages.getMessage("PdfViewerLabel.Right"));
		jLabel6.setBounds(295, 100, 70, 15);

		this.rightMargin.setBounds(340, 95, 60, 23);

		JLabel jLabel7 = new javax.swing.JLabel(Messages.getMessage("PdfViewerLabel.Bottom"));
		jLabel7.setBounds(140, 150, 110, 15);

		this.bottomMargin.setBounds(200, 145, 60, 23);

		this.applyToCurrent.setSelected(true);
		this.applyToCurrent.setText(Messages.getMessage("PdfViewerCheckBox.ApplyToPriorCroppingRectangle"));
		this.applyToCurrent.setBounds(5, 190, 305, 15);

		JButton jButton1 = new javax.swing.JButton(Messages.getMessage("PdfViewerButton.Set2Zero"));
		jButton1.setBounds(310, 185, 130, 23);
		jButton1.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				CropPDFPages.this.leftMargin.setValue(0);
				CropPDFPages.this.rightMargin.setValue(0);
				CropPDFPages.this.topMargin.setValue(0);
				CropPDFPages.this.bottomMargin.setValue(0);
			}
		});

		this.pageRangeLabel.setText(Messages.getMessage("PdfViewerPageRange.text"));
		this.pageRangeLabel.setBounds(new Rectangle(13, 220, 199, 26));

		this.printAll.setText(Messages.getMessage("PdfViewerRadioButton.All"));
		this.printAll.setBounds(new Rectangle(23, 250, 75, 22));

		this.printCurrent.setText(Messages.getMessage("PdfViewerRadioButton.CurrentPage"));
		this.printCurrent.setBounds(new Rectangle(23, 270, 100, 22));
		this.printCurrent.setSelected(true);

		this.printPages.setText(Messages.getMessage("PdfViewerRadioButton.Pages"));
		this.printPages.setBounds(new Rectangle(23, 292, 70, 22));

		this.pagesBox.setBounds(new Rectangle(95, 292, 230, 22));
		this.pagesBox.addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(KeyEvent arg0) {}

			@Override
			public void keyReleased(KeyEvent arg0) {
				if (CropPDFPages.this.pagesBox.getText().length() == 0) CropPDFPages.this.printCurrent.setSelected(true);
				else CropPDFPages.this.printPages.setSelected(true);

			}

			@Override
			public void keyTyped(KeyEvent arg0) {}
		});

		JTextArea pagesInfo = new JTextArea(Messages.getMessage("PdfViewerMessage.PageNumberOrRange") + '\n'
				+ Messages.getMessage("PdfViewerMessage.PageRangeExample"));
		pagesInfo.setBounds(new Rectangle(23, 325, 400, 40));
		pagesInfo.setOpaque(false);

		this.add(jLabel1);
		this.add(this.bottomMargin);
		this.add(jLabel5);
		this.add(this.topMargin);
		this.add(this.leftMargin);
		this.add(this.rightMargin);
		this.add(jLabel7);
		this.add(jLabel6);
		this.add(this.applyToCurrent);
		this.add(jButton1);

		this.add(this.printAll, null);
		this.add(this.printCurrent, null);

		this.add(this.printPages, null);
		this.add(this.pagesBox, null);
		this.add(pagesInfo, null);

		this.add(textAndFont, null);
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
		return new Dimension(440, 400);
	}
}
