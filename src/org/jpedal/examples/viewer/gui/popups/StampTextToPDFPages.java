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
 * StampTextToPDFPages.java
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

public class StampTextToPDFPages extends Save {

	private static final long serialVersionUID = -7862345051208881536L;
	JLabel OutputLabel = new JLabel();
	ButtonGroup buttonGroup1 = new ButtonGroup();
	ButtonGroup buttonGroup2 = new ButtonGroup();

	JToggleButton jToggleButton3 = new JToggleButton();

	JToggleButton jToggleButton2 = new JToggleButton();

	JRadioButton printAll = new JRadioButton();
	JRadioButton printCurrent = new JRadioButton();
	JRadioButton printPages = new JRadioButton();

	JTextField pagesBox = new JTextField();

	JTextField textBox = new JTextField();

	JSpinner rotationBox = new JSpinner(new SpinnerNumberModel(0, 0, 360, 1));

	JComboBox placementBox = new JComboBox(new String[] { Messages.getMessage("PdfViewerLabel.Overlay"),
			Messages.getMessage("PdfViewerLabel.Underlay") });

	JComboBox fontsList = new JComboBox(new String[] { "Courier", "Courier-Bold", "Courier-Oblique", "Courier-BoldOblique", "Helvetica",
			"Helvetica-Bold", "Helvetica-BoldOblique", "Helvetica-Oblique", "Times-Roman", "Times-Bold", "Times-Italic", "Times-BoldItalic",
			"Symbol", "ZapfDingbats" });

	JSpinner fontSize = new JSpinner(new SpinnerNumberModel(10, 1, 100, 1));

	JLabel colorBox = new JLabel();

	JComboBox horizontalBox = new JComboBox(new String[] { Messages.getMessage("PdfViewerLabel.FromLeft"),
			Messages.getMessage("PdfViewerLabel.Centered"), Messages.getMessage("PdfViewerLabel.FromRight") });

	JComboBox verticalBox = new JComboBox(new String[] { Messages.getMessage("PdfViewerLabel.FromTop"),
			Messages.getMessage("PdfViewerLabel.Centered"), Messages.getMessage("PdfViewerLabel.FromBottom") });

	JSpinner horizontalOffset = new JSpinner(new SpinnerNumberModel(0.00, -1000.00, 1000.00, 1));
	JSpinner verticalOffset = new JSpinner(new SpinnerNumberModel(0.00, -1000.00, 1000.00, 1));

	public StampTextToPDFPages(String root_dir, int end_page, int currentPage) {
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
								if (Viewer.showMessages) JOptionPane.showMessageDialog(
										this,
										Messages.getMessage("PdfViewerText.Page") + ' ' + pages + ' '
												+ Messages.getMessage("PdfViewerError.OutOfBounds") + ' '
												+ Messages.getMessage("PdfViewerText.PageCount") + ' ' + this.end_page);
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

	public float getHorizontalOffset() {
		return Float.parseFloat(this.horizontalOffset.getValue().toString());
	}

	public float getVerticalOffset() {
		return Float.parseFloat(this.verticalOffset.getValue().toString());
	}

	public String getHorizontalPosition() {
		return (String) this.horizontalBox.getSelectedItem();
	}

	public String getVerticalPosition() {
		return (String) this.verticalBox.getSelectedItem();
	}

	public int getRotation() {
		return Integer.parseInt(this.rotationBox.getValue().toString());
	}

	public String getPlacement() {
		return (String) this.placementBox.getSelectedItem();
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

	public String getText() {
		return this.textBox.getText();
	}

	private void jbInit() throws Exception {

		JLabel textAndFont = new JLabel(Messages.getMessage("PdfViewerLabel.TextAndFont"));
		textAndFont.setFont(new java.awt.Font("Dialog", Font.BOLD, 14));
		textAndFont.setDisplayedMnemonic('0');
		textAndFont.setBounds(new Rectangle(13, 13, 220, 26));

		JLabel text = new JLabel(Messages.getMessage("PdfViewerLabel.Text"));
		text.setBounds(new Rectangle(20, 40, 50, 23));

		this.textBox.setBounds(new Rectangle(50, 40, 430, 23));

		JLabel rotation = new JLabel(Messages.getMessage("PdfViewerLabel.Rotation"));
		rotation.setBounds(new Rectangle(20, 80, 80, 23));

		this.rotationBox.setBounds(new Rectangle(90, 80, 50, 23));

		JLabel degrees = new JLabel(Messages.getMessage("PdfViewerText.Degrees"));
		degrees.setBounds(new Rectangle(150, 80, 50, 23));

		JLabel placement = new JLabel(Messages.getMessage("PdfViewerLabel.Placement"));
		placement.setBounds(new Rectangle(210, 80, 70, 23));

		this.placementBox.setBounds(new Rectangle(270, 80, 90, 23));

		JLabel font = new JLabel(Messages.getMessage("PdfViewerLabel.Font"));
		font.setBounds(new Rectangle(20, 120, 90, 23));

		this.fontsList.setBounds(new Rectangle(90, 120, 150, 23));
		this.fontsList.setSelectedItem("Helvetica");

		JLabel size = new JLabel(Messages.getMessage("PdfViewerLabel.Size"));
		size.setBounds(new Rectangle(250, 120, 50, 23));

		this.fontSize.setBounds(new Rectangle(300, 120, 50, 23));

		JLabel color = new JLabel(Messages.getMessage("PdfViewerLabel.Color"));
		color.setBounds(new Rectangle(360, 120, 50, 23));

		this.colorBox.setBackground(Color.black);
		this.colorBox.setOpaque(true);
		this.colorBox.setBounds(new Rectangle(400, 120, 23, 23));

		JButton chooseColor = new JButton(Messages.getMessage("PdfViewerButton.ChooseColor"));
		chooseColor.setBounds(new Rectangle(440, 120, 150, 23));
		chooseColor.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				StampTextToPDFPages.this.colorBox.setBackground(JColorChooser.showDialog(null, "Color",
						StampTextToPDFPages.this.colorBox.getBackground()));
			}
		});

		JLabel positionAndOffset = new JLabel(Messages.getMessage("PdfViewerLabel.PositionAndOffset"));
		positionAndOffset.setFont(new java.awt.Font("Dialog", Font.BOLD, 14));
		positionAndOffset.setDisplayedMnemonic('0');
		positionAndOffset.setBounds(new Rectangle(13, 150, 220, 26));

		JLabel horizontal = new JLabel(Messages.getMessage("PdfViewerLabel.Horizontal"));
		horizontal.setBounds(new Rectangle(20, 185, 90, 23));

		this.horizontalBox.setBounds(new Rectangle(80, 185, 120, 23));
		this.horizontalBox.setSelectedItem(Messages.getMessage("PdfViewerLabel.Centered"));

		JLabel vertical = new JLabel(Messages.getMessage("PdfViewerLabel.Vertical"));
		vertical.setBounds(new Rectangle(20, 215, 90, 23));

		this.verticalBox.setBounds(new Rectangle(80, 215, 120, 23));
		this.verticalBox.setSelectedItem(Messages.getMessage("PdfViewerLabel.Centered"));

		JLabel hOffset = new JLabel(Messages.getMessage("PdfViewerLabel.Offset"));
		hOffset.setBounds(new Rectangle(250, 185, 90, 23));

		this.horizontalOffset.setBounds(new Rectangle(315, 185, 70, 23));

		JLabel vOffset = new JLabel(Messages.getMessage("PdfViewerLabel.Offset"));
		vOffset.setBounds(new Rectangle(250, 215, 90, 23));

		this.verticalOffset.setBounds(new Rectangle(315, 215, 70, 23));

		this.pageRangeLabel.setText(Messages.getMessage("PdfViewerPageRange.text"));
		this.pageRangeLabel.setBounds(new Rectangle(13, 250, 199, 26));

		this.printAll.setText(Messages.getMessage("PdfViewerRadioButton.All"));
		this.printAll.setBounds(new Rectangle(23, 280, 75, 22));

		this.printCurrent.setText(Messages.getMessage("PdfViewerRadioButton.CurrentPage"));
		this.printCurrent.setBounds(new Rectangle(23, 300, 100, 22));
		this.printCurrent.setSelected(true);

		this.printPages.setText(Messages.getMessage("PdfViewerRadioButton.Pages"));
		this.printPages.setBounds(new Rectangle(23, 322, 70, 22));

		this.pagesBox.setBounds(new Rectangle(95, 322, 230, 22));
		this.pagesBox.addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(KeyEvent arg0) {}

			@Override
			public void keyReleased(KeyEvent arg0) {
				if (StampTextToPDFPages.this.pagesBox.getText().length() == 0) StampTextToPDFPages.this.printCurrent.setSelected(true);
				else StampTextToPDFPages.this.printPages.setSelected(true);

			}

			@Override
			public void keyTyped(KeyEvent arg0) {}
		});

		JTextArea pagesInfo = new JTextArea(Messages.getMessage("PdfViewerMessage.PageNumberOrRangeLong"));
		pagesInfo.setBounds(new Rectangle(23, 355, 600, 40));
		pagesInfo.setOpaque(false);

		this.add(this.printAll, null);
		this.add(this.printCurrent, null);

		this.add(this.printPages, null);
		this.add(this.pagesBox, null);
		this.add(pagesInfo, null);

		this.add(text, null);
		this.add(this.textBox, null);
		this.add(rotation, null);
		this.add(this.rotationBox, null);
		this.add(degrees, null);
		this.add(placement, null);
		this.add(this.placementBox, null);

		this.add(positionAndOffset, null);
		this.add(horizontal, null);
		this.add(this.horizontalBox, null);
		this.add(vertical, null);
		this.add(this.verticalBox, null);
		this.add(hOffset, null);
		this.add(this.horizontalOffset, null);
		this.add(vOffset, null);
		this.add(this.verticalOffset, null);

		this.add(font, null);
		this.add(this.fontsList, null);
		this.add(size, null);
		this.add(this.fontSize, null);
		this.add(color, null);
		this.add(this.colorBox, null);
		this.add(chooseColor, null);

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
		return new Dimension(600, 400);
	}
}
