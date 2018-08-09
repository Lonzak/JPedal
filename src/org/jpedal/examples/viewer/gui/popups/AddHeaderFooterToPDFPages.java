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
 * AddHeaderFooterToPDFPages.java
 * ---------------
 */
package org.jpedal.examples.viewer.gui.popups;

import java.awt.Color;
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
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
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

public class AddHeaderFooterToPDFPages extends Save {

	private static final long serialVersionUID = -8681143216306570454L;

	JLabel OutputLabel = new JLabel();
	ButtonGroup buttonGroup1 = new ButtonGroup();
	ButtonGroup buttonGroup2 = new ButtonGroup();

	JToggleButton jToggleButton3 = new JToggleButton();

	JToggleButton jToggleButton2 = new JToggleButton();

	JRadioButton printAll = new JRadioButton();
	JRadioButton printCurrent = new JRadioButton();
	JRadioButton printPages = new JRadioButton();

	JTextField pagesBox = new JTextField();

	JTextField leftHeaderBox = new JTextField();
	JTextField centerHeaderBox = new JTextField();
	JTextField rightHeaderBox = new JTextField();
	JTextField leftFooterBox = new JTextField();
	JTextField centerFooterBox = new JTextField();
	JTextField rightFooterBox = new JTextField();

	JComboBox fontsList = new JComboBox(new String[] { "Courier", "Courier-Bold", "Courier-Oblique", "Courier-BoldOblique", "Helvetica",
			"Helvetica-Bold", "Helvetica-BoldOblique", "Helvetica-Oblique", "Times-Roman", "Times-Bold", "Times-Italic", "Times-BoldItalic",
			"Symbol", "ZapfDingbats" });

	JSpinner fontSize = new JSpinner(new SpinnerNumberModel(10, 1, 100, 1));

	JLabel colorBox = new JLabel();

	JSpinner leftRightBox = new JSpinner(new SpinnerNumberModel(36.00, 1.00, 1000.00, 1));
	JSpinner topBottomBox = new JSpinner(new SpinnerNumberModel(36.00, 1.00, 1000.00, 1));

	JTextArea tagsList = new JTextArea();

	public AddHeaderFooterToPDFPages(String root_dir, int end_page, int currentPage) {
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
						LogWriter.writeLog(Messages.getMessage("PdfViewerError.Exception") + ' ' + e + ' '
								+ Messages.getMessage("PdfViewerError.ExportPdfError"));
						if (Viewer.showMessages) JOptionPane.showMessageDialog(this, Messages.getMessage("PdfViewerError.InvalidSyntax"));
					}
				}

		return pagesToExport;
	}

	public float getLeftRightMargin() {
		return Float.parseFloat(this.leftRightBox.getValue().toString());
	}

	public float getTopBottomMargin() {
		return Float.parseFloat(this.topBottomBox.getValue().toString());
	}

	public String getFontName() {
		return (String) this.fontsList.getSelectedItem();
	}

	public int getFontSize() {
		return Integer.parseInt(this.fontSize.getValue().toString());
	}

	public Color getFontColor() {
		return this.colorBox.getBackground();
	}

	public String getLeftHeader() {
		return this.leftHeaderBox.getText();
	}

	public String getCenterHeader() {
		return this.centerHeaderBox.getText();
	}

	public String getRightHeader() {
		return this.rightHeaderBox.getText();
	}

	public String getLeftFooter() {
		return this.leftFooterBox.getText();
	}

	public String getCenterFooter() {
		return this.centerFooterBox.getText();
	}

	public String getRightFooter() {
		return this.rightFooterBox.getText();
	}

	private void jbInit() throws Exception {

		JLabel textAndFont = new JLabel(Messages.getMessage("PdfViewerLabel.TextAndFont"));
		textAndFont.setFont(new java.awt.Font("Dialog", Font.BOLD, 14));
		textAndFont.setDisplayedMnemonic('0');
		textAndFont.setBounds(new Rectangle(13, 13, 220, 26));

		JLabel left = new JLabel(Messages.getMessage("PdfViewerLabel.Left"));
		left.setBounds(new Rectangle(130, 40, 50, 23));

		JLabel center = new JLabel(Messages.getMessage("PdfViewerLabel.Center"));
		center.setBounds(new Rectangle(300, 40, 50, 23));

		JLabel right = new JLabel(Messages.getMessage("PdfViewerLabel.Right"));
		right.setBounds(new Rectangle(475, 40, 50, 23));

		JLabel header = new JLabel(Messages.getMessage("PdfViewerLabel.Header"));
		header.setBounds(new Rectangle(20, 60, 90, 23));

		JLabel footer = new JLabel(Messages.getMessage("PdfViewerLabel.Footer"));
		footer.setBounds(new Rectangle(20, 90, 50, 23));

		this.leftHeaderBox.setBounds(new Rectangle(85, 60, 133, 23));
		this.centerHeaderBox.setBounds(new Rectangle(250, 60, 133, 23));
		this.rightHeaderBox.setBounds(new Rectangle(425, 60, 133, 23));

		this.leftFooterBox.setBounds(new Rectangle(85, 90, 133, 23));
		this.centerFooterBox.setBounds(new Rectangle(250, 90, 133, 23));
		this.rightFooterBox.setBounds(new Rectangle(425, 90, 133, 23));

		JLabel font = new JLabel(Messages.getMessage("PdfViewerLabel.Font"));
		font.setBounds(new Rectangle(20, 120, 75, 23));

		this.fontsList.setBounds(new Rectangle(85, 120, 150, 23));
		this.fontsList.setSelectedItem("Helvetica");

		JLabel size = new JLabel(Messages.getMessage("PdfViewerLabel.Size"));
		size.setBounds(new Rectangle(250, 120, 50, 23));

		this.fontSize.setBounds(new Rectangle(290, 120, 50, 23));

		JLabel color = new JLabel(Messages.getMessage("PdfViewerLabel.Color"));
		color.setBounds(new Rectangle(360, 120, 50, 23));

		this.colorBox.setBackground(Color.black);
		this.colorBox.setOpaque(true);
		this.colorBox.setBounds(new Rectangle(410, 120, 23, 23));

		JButton chooseColor = new JButton(Messages.getMessage("PdfViewerButton.ChooseColor"));
		chooseColor.setBounds(new Rectangle(450, 120, 160, 23));
		chooseColor.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				AddHeaderFooterToPDFPages.this.colorBox.setBackground(JColorChooser.showDialog(null, "Color",
						AddHeaderFooterToPDFPages.this.colorBox.getBackground()));
			}
		});

		this.tagsList.setText("You may use the following\n" + "tags as part of the text.\n\n" + "<d> - Date in short format\n"
				+ "<D> - Date in long format\n" + "<t> - Time in 12-hour format\n" + "<T> - Time in 24-hour format\n" + "<f> - Filename\n"
				+ "<F> - Full path filename\n" + "<p> - Current page number\n" + "<P> - Total number of pages");
		this.tagsList.setOpaque(false);
		this.tagsList.setBounds(350, 160, 200, 210);

		JLabel margins = new JLabel(Messages.getMessage("PdfViewerLabel.Margins"));

		margins.setFont(new java.awt.Font("Dialog", Font.BOLD, 14));
		margins.setDisplayedMnemonic('0');
		margins.setBounds(new Rectangle(13, 150, 220, 26));

		JLabel leftRight = new JLabel(Messages.getMessage("PdfViewerLabel.LeftAndRight"));
		leftRight.setBounds(new Rectangle(20, 185, 90, 23));

		this.leftRightBox.setBounds(new Rectangle(100, 185, 70, 23));

		JLabel topBottom = new JLabel(Messages.getMessage("PdfViewerLabel.TopAndBottom"));
		topBottom.setBounds(new Rectangle(180, 185, 120, 23));

		this.topBottomBox.setBounds(new Rectangle(300, 185, 70, 23));

		this.pageRangeLabel.setText(Messages.getMessage("PdfViewerPageRange.text"));
		this.pageRangeLabel.setBounds(new Rectangle(13, 220, 400, 26));

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
				if (AddHeaderFooterToPDFPages.this.pagesBox.getText().length() == 0) AddHeaderFooterToPDFPages.this.printCurrent.setSelected(true);
				else AddHeaderFooterToPDFPages.this.printPages.setSelected(true);

			}

			@Override
			public void keyTyped(KeyEvent arg0) {}
		});

		JTextArea pagesInfo = new JTextArea(Messages.getMessage("PdfViewerMessage.PageNumberOrRangeLong"));
		pagesInfo.setBounds(new Rectangle(23, 320, 620, 40));
		pagesInfo.setOpaque(false);

		this.add(this.printAll, null);
		this.add(this.printCurrent, null);

		this.add(this.printPages, null);
		this.add(this.pagesBox, null);
		this.add(pagesInfo, null);

		this.add(left, null);
		this.add(center, null);
		this.add(right, null);
		this.add(header, null);
		this.add(footer, null);
		this.add(this.leftHeaderBox, null);
		this.add(this.centerHeaderBox, null);
		this.add(this.rightHeaderBox, null);
		this.add(this.leftFooterBox, null);
		this.add(this.centerFooterBox, null);
		this.add(this.rightFooterBox, null);
		this.add(font, null);
		this.add(this.fontsList, null);
		this.add(size, null);
		this.add(this.fontSize, null);
		this.add(color, null);
		this.add(this.colorBox, null);
		this.add(chooseColor, null);
		this.add(margins, null);
		this.add(leftRight, null);
		this.add(this.leftRightBox, null);
		this.add(topBottom, null);
		this.add(this.topBottomBox, null);

		this.add(textAndFont, null);
		this.add(this.changeButton, null);
		this.add(this.pageRangeLabel, null);

		// this.add(tagsList, null);

		this.add(this.jToggleButton2, null);
		this.add(this.jToggleButton3, null);

		this.buttonGroup1.add(this.printAll);
		this.buttonGroup1.add(this.printCurrent);
		this.buttonGroup1.add(this.printPages);
	}

	@Override
	final public Dimension getPreferredSize() {
		return new Dimension(620, 350);
	}
}
