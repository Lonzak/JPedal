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
 * ExtractPDFPagesNup.java
 * ---------------
 */
package org.jpedal.examples.viewer.gui.popups;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;

import javax.print.attribute.standard.PageRanges;
import javax.swing.ButtonGroup;
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
import org.jpedal.examples.viewer.utils.ItextFunctions;
import org.jpedal.utils.LogWriter;
import org.jpedal.utils.Messages;

public class ExtractPDFPagesNup extends Save {

	private static final long serialVersionUID = -2276797947489369291L;
	JLabel OutputLabel = new JLabel();
	ButtonGroup buttonGroup1 = new ButtonGroup();
	ButtonGroup buttonGroup2 = new ButtonGroup();

	JToggleButton jToggleButton3 = new JToggleButton();

	JToggleButton jToggleButton2 = new JToggleButton();

	JRadioButton printAll = new JRadioButton();
	JRadioButton printCurrent = new JRadioButton();
	JRadioButton printPages = new JRadioButton();

	JTextField pagesBox = new JTextField();

	ArrayList<String> papers;
	ArrayList<Dimension> paperDimensions;

	private javax.swing.JSpinner horizontalSpacing;
	private javax.swing.JLabel jLabel1;
	private javax.swing.JLabel jLabel11;
	private javax.swing.JLabel jLabel12;
	private javax.swing.JLabel jLabel13;
	private javax.swing.JLabel jLabel14;
	private javax.swing.JLabel jLabel15;
	private javax.swing.JLabel jLabel16;
	private javax.swing.JLabel jLabel17;
	private javax.swing.JLabel jLabel2;
	private javax.swing.JLabel jLabel3;
	private javax.swing.JLabel jLabel4;
	private javax.swing.JSpinner layoutColumns;
	private javax.swing.JSpinner layoutRows;
	private javax.swing.JComboBox layouts;
	private javax.swing.JSpinner leftRightMargins;
	private javax.swing.JSpinner scaleHeight;
	private javax.swing.JCheckBox pageProportionally;
	private javax.swing.JComboBox pageScalings;
	private javax.swing.JSpinner scaleWidth;
	private javax.swing.JSpinner paperHeight;
	private javax.swing.JComboBox paperOrientation;
	private javax.swing.JComboBox paperSizes;
	private javax.swing.JSpinner paperWidth;
	private javax.swing.JSpinner topBottomMargins;
	private javax.swing.JSpinner verticalSpacing;

	private JComboBox repeat = new JComboBox();
	private JSpinner copies = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
	private JComboBox ordering = new JComboBox();
	private JComboBox doubleSided = new JComboBox();

	public ExtractPDFPagesNup(String root_dir, int end_page, int currentPage) {
		super(root_dir, end_page, currentPage);

		genertatePaperSizes();

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

	public float getHorizontalSpacing() {
		return Float.parseFloat(this.horizontalSpacing.getValue().toString());
	}

	public float getVerticalSpacing() {
		return Float.parseFloat(this.verticalSpacing.getValue().toString());
	}

	public float getLeftRightMargin() {
		return Float.parseFloat(this.leftRightMargins.getValue().toString());
	}

	public float getTopBottomMargin() {
		return Float.parseFloat(this.topBottomMargins.getValue().toString());
	}

	public int getPaperWidth() {
		return Integer.parseInt(this.paperWidth.getValue().toString());
	}

	public int getPaperHeight() {
		return Integer.parseInt(this.paperHeight.getValue().toString());
	}

	public String getPaperOrientation() {
		return (String) this.paperOrientation.getSelectedItem();
	}

	public String getScale() {
		return (String) this.pageScalings.getSelectedItem();
	}

	public boolean isScaleProportional() {
		return this.pageProportionally.isSelected();
	}

	public float getScaleWidth() {
		return Float.parseFloat(this.scaleWidth.getValue().toString());
	}

	public float getScaleHeight() {
		return Float.parseFloat(this.scaleHeight.getValue().toString());
	}

	public String getSelectedLayout() {
		return (String) this.layouts.getSelectedItem();
	}

	public int getLayoutRows() {
		return Integer.parseInt(this.layoutRows.getValue().toString());
	}

	public int getLayoutColumns() {
		return Integer.parseInt(this.layoutColumns.getValue().toString());
	}

	public int getRepeat() {
		if (this.repeat.getSelectedIndex() == 0) return ItextFunctions.REPEAT_NONE;

		if (this.repeat.getSelectedIndex() == 1) return ItextFunctions.REPEAT_AUTO;

		return ItextFunctions.REPEAT_SPECIFIED;
	}

	public int getCopies() {
		return Integer.parseInt(this.copies.getValue().toString());
	}

	public int getPageOrdering() {
		if (this.ordering.getSelectedIndex() == 0) return ItextFunctions.ORDER_ACROSS;

		if (this.ordering.getSelectedIndex() == 1) return ItextFunctions.ORDER_DOWN;

		return ItextFunctions.ORDER_STACK;
	}

	public String getDoubleSided() {
		return (String) this.doubleSided.getSelectedItem();
	}

	private void jbInit() throws Exception {
		this.rootFilesLabel.setBounds(new Rectangle(13, 13, 400, 26));
		this.rootDir.setBounds(new Rectangle(20, 40, 232, 23));
		this.changeButton.setBounds(new Rectangle(272, 40, 101, 23));

		JLabel textAndFont = new JLabel(Messages.getMessage("PdfViewerNUPLabel.PaperSize"));
		textAndFont.setFont(new java.awt.Font("Dialog", Font.BOLD, 14));
		textAndFont.setDisplayedMnemonic('0');
		textAndFont.setBounds(new Rectangle(13, 70, 220, 26));

		JLabel scale = new JLabel(Messages.getMessage("PdfViewerNUPLabel.Scale"));
		scale.setFont(new java.awt.Font("Dialog", Font.BOLD, 14));
		scale.setDisplayedMnemonic('0');
		scale.setBounds(new Rectangle(13, 140, 220, 26));

		JLabel layout = new JLabel(Messages.getMessage("PdfViewerNUPLabel.Layout"));
		layout.setFont(new java.awt.Font("Dialog", Font.BOLD, 14));
		layout.setDisplayedMnemonic('0');
		layout.setBounds(new Rectangle(13, 210, 220, 26));

		JLabel margins = new JLabel(Messages.getMessage("PdfViewerNUPLabel.Margins"));
		margins.setFont(new java.awt.Font("Dialog", Font.BOLD, 14));
		margins.setDisplayedMnemonic('0');
		margins.setBounds(new Rectangle(13, 280, 220, 26));

		JLabel pageSettings = new JLabel(Messages.getMessage("PdfViewerNUPLabel.PageSettings"));
		pageSettings.setFont(new java.awt.Font("Dialog", Font.BOLD, 14));
		pageSettings.setDisplayedMnemonic('0');
		pageSettings.setBounds(new Rectangle(13, 400, 220, 26));

		this.layouts = new javax.swing.JComboBox();
		this.paperOrientation = new javax.swing.JComboBox();
		this.pageScalings = new javax.swing.JComboBox();
		this.jLabel1 = new javax.swing.JLabel();
		this.jLabel2 = new javax.swing.JLabel();
		this.topBottomMargins = new javax.swing.JSpinner(new SpinnerNumberModel(18.00, -720.00, 720.00, 1.00));
		this.leftRightMargins = new javax.swing.JSpinner(new SpinnerNumberModel(18.00, -720.00, 720.00, 1.00));
		this.pageProportionally = new javax.swing.JCheckBox();
		this.paperSizes = new javax.swing.JComboBox();
		this.jLabel11 = new javax.swing.JLabel();
		this.jLabel3 = new javax.swing.JLabel();
		this.jLabel4 = new javax.swing.JLabel();
		this.paperWidth = new javax.swing.JSpinner();
		this.paperHeight = new javax.swing.JSpinner();
		this.scaleWidth = new javax.swing.JSpinner(new SpinnerNumberModel(396.00, 72.00, 5184.00, 1.00));
		this.scaleHeight = new javax.swing.JSpinner(new SpinnerNumberModel(612.00, 72.00, 5184.00, 1.00));
		this.jLabel12 = new javax.swing.JLabel();
		this.jLabel13 = new javax.swing.JLabel();
		this.layoutRows = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
		this.layoutColumns = new JSpinner(new SpinnerNumberModel(2, 1, 100, 1));
		this.jLabel14 = new javax.swing.JLabel();
		this.verticalSpacing = new javax.swing.JSpinner(new SpinnerNumberModel(7.20, 0.00, 720.00, 1.00));
		this.horizontalSpacing = new javax.swing.JSpinner(new SpinnerNumberModel(7.20, 0.00, 720.00, 1.00));
		this.jLabel16 = new javax.swing.JLabel();
		this.jLabel15 = new javax.swing.JLabel();
		this.jLabel17 = new javax.swing.JLabel();

		this.layouts.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "2 Up", "4 Up", "8 Up",
				Messages.getMessage("PdfViewerNUPOption.Custom") }));
		this.layouts.setSelectedIndex(0);
		this.layouts.addItemListener(new java.awt.event.ItemListener() {
			@Override
			public void itemStateChanged(java.awt.event.ItemEvent evt) {
				layoutsSelectionChanged(evt);
			}
		});

		this.copies.setEnabled(false);

		this.repeat.setModel(new javax.swing.DefaultComboBoxModel(new String[] { Messages.getMessage("PdfViewerNUPOption.None"),
				Messages.getMessage("PdfViewerNUPOption.Auto"), Messages.getMessage("PdfViewerNUPOption.Specified") }));
		this.repeat.addItemListener(new java.awt.event.ItemListener() {
			@Override
			public void itemStateChanged(java.awt.event.ItemEvent evt) {
				if (ExtractPDFPagesNup.this.repeat.getSelectedItem().equals("None")) {
					ExtractPDFPagesNup.this.copies.getModel().setValue(1);
					ExtractPDFPagesNup.this.copies.setEnabled(false);
				}
				else
					if (ExtractPDFPagesNup.this.repeat.getSelectedItem().equals("Auto")) {
						int rows = Integer.parseInt(ExtractPDFPagesNup.this.layoutRows.getValue().toString());
						int coloumns = Integer.parseInt(ExtractPDFPagesNup.this.layoutColumns.getValue().toString());

						ExtractPDFPagesNup.this.copies.getModel().setValue(rows * coloumns);
						ExtractPDFPagesNup.this.copies.setEnabled(false);
					}
					else
						if (ExtractPDFPagesNup.this.repeat.getSelectedItem().equals("Specified")) {
							ExtractPDFPagesNup.this.copies.setEnabled(true);
						}
			}
		});

		this.ordering.setModel(new javax.swing.DefaultComboBoxModel(new String[] { Messages.getMessage("PdfViewerNUPOption.Across"),
				Messages.getMessage("PdfViewerNUPOption.Down") }));

		this.doubleSided.setModel(new javax.swing.DefaultComboBoxModel(new String[] { Messages.getMessage("PdfViewerNUPOption.None"),
				Messages.getMessage("PdfViewerNUPOption.Front&Back"), Messages.getMessage("PdfViewerNUPOption.Gutter") }));

		this.layouts.setBounds(20, 240, 110, 23);

		this.paperOrientation.setModel(new javax.swing.DefaultComboBoxModel(new String[] { Messages.getMessage("PdfViewerNUPOption.Auto"),
				Messages.getMessage("PdfViewerNUPOption.Portrait"), Messages.getMessage("PdfViewerNUPOption.Landscape") }));
		this.paperOrientation.setBounds(510, 100, 90, 23);

		this.pageScalings.setModel(new javax.swing.DefaultComboBoxModel(new String[] { Messages.getMessage("PdfViewerNUPOption.OriginalSize"),
				Messages.getMessage("PdfViewerNUPOption.Auto"), Messages.getMessage("PdfViewerNUPOption.Specified") }));
		this.pageScalings.setSelectedIndex(1);
		this.pageScalings.addItemListener(new java.awt.event.ItemListener() {
			@Override
			public void itemStateChanged(java.awt.event.ItemEvent evt) {
				scalingSelectionChanged(evt);
			}
		});

		this.pageScalings.setBounds(20, 170, 200, 23);

		this.jLabel1.setText(Messages.getMessage("PdfViewerNUPLabel.Width"));
		this.jLabel1.setBounds(148, 100, 50, 15);

		this.jLabel2.setText(Messages.getMessage("PdfViewerNUPLabel.Height"));
		this.jLabel2.setBounds(278, 100, 50, 15);

		this.pageProportionally.setSelected(true);
		this.pageProportionally.setText(Messages.getMessage("PdfViewerNUPText.Proportionally"));
		this.pageProportionally.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
		this.pageProportionally.setMargin(new java.awt.Insets(0, 0, 0, 0));
		this.pageProportionally.setBounds(240, 170, 120, 15);

		this.paperSizes.setModel(new javax.swing.DefaultComboBoxModel(getPaperSizes()));
		this.paperSizes.addItemListener(new java.awt.event.ItemListener() {
			@Override
			public void itemStateChanged(java.awt.event.ItemEvent evt) {
				pageSelectionChanged(evt);
			}
		});

		this.paperSizes.setBounds(20, 100, 110, 23);

		this.jLabel11.setText(Messages.getMessage("PdfViewerNUPLabel.Orientation"));
		this.jLabel11.setBounds(408, 100, 130, 15);

		this.jLabel3.setText(Messages.getMessage("PdfViewerNUPLabel.Width"));
		this.jLabel3.setBounds(370, 170, 50, 15);

		this.jLabel4.setText(Messages.getMessage("PdfViewerNUPLabel.Height"));
		this.jLabel4.setBounds(500, 170, 50, 15);

		this.paperWidth.setEnabled(false);
		this.paperWidth.setBounds(195, 100, 70, 23);

		this.paperHeight.setEnabled(false);
		this.paperHeight.setBounds(318, 100, 70, 23);

		this.scaleWidth.setEnabled(false);
		this.scaleWidth.setBounds(420, 170, 70, 23);

		this.scaleHeight.setEnabled(false);
		this.scaleHeight.setBounds(540, 170, 70, 23);

		this.jLabel12.setText(Messages.getMessage("PdfViewerNUPLabel.Rows"));
		this.jLabel12.setBounds(148, 240, 50, 15);

		this.jLabel13.setText(Messages.getMessage("PdfViewerNUPLabel.Columns"));
		this.jLabel13.setBounds(278, 240, 50, 15);

		this.layoutRows.setEnabled(false);
		this.layoutRows.setBounds(195, 240, 70, 23);

		this.layoutColumns.setEnabled(false);
		this.layoutColumns.setBounds(328, 240, 70, 23);

		this.jLabel14.setText(Messages.getMessage("PdfViewerNUPLabel.Left&RightMargins"));
		this.jLabel14.setBounds(22, 326, 200, 15);
		this.leftRightMargins.setBounds(210, 322, 70, 23);

		this.jLabel16.setText(Messages.getMessage("PdfViewerNUPLabel.HorizontalSpacing"));
		this.jLabel16.setBounds(22, 356, 180, 15);
		this.horizontalSpacing.setBounds(210, 354, 70, 23);

		this.jLabel15.setText(Messages.getMessage("PdfViewerNUPLabel.Top&BottomMargins"));
		this.jLabel15.setBounds(300, 326, 180, 15);
		this.topBottomMargins.setBounds(480, 320, 70, 23);

		this.jLabel17.setText(Messages.getMessage("PdfViewerNUPLabel.VerticalSpacing"));
		this.jLabel17.setBounds(300, 356, 180, 15);
		this.verticalSpacing.setBounds(480, 354, 70, 23);

		JLabel jLabel18 = new JLabel(Messages.getMessage("PdfViewerNUPLabel.Repeat"));
		jLabel18.setBounds(22, 446, 130, 15);
		this.repeat.setBounds(140, 442, 100, 23);

		JLabel jLabel20 = new JLabel(Messages.getMessage("PdfViewerNUPLabel.Copies"));
		jLabel20.setBounds(300, 446, 130, 15);
		this.ordering.setBounds(140, 474, 130, 23);

		JLabel jLabel19 = new JLabel(Messages.getMessage("PdfViewerNUPLabel.PageOrdering"));
		jLabel19.setBounds(22, 474, 130, 15);
		this.copies.setBounds(420, 440, 70, 23);

		JLabel jLabel21 = new JLabel(Messages.getMessage("PdfViewerNUPLabel.DoubleSided"));
		jLabel21.setBounds(300, 476, 130, 15);
		this.doubleSided.setBounds(420, 474, 100, 23);

		this.pageRangeLabel.setText(Messages.getMessage("PdfViewerNUPLabel.PageRange"));
		this.pageRangeLabel.setBounds(new Rectangle(13, 530, 199, 26));

		this.printAll.setText(Messages.getMessage("PdfViewerNUPOption.All"));
		this.printAll.setBounds(new Rectangle(23, 560, 75, 22));
		this.printAll.setSelected(true);

		this.printCurrent.setText(Messages.getMessage("PdfViewerNUPOption.CurrentPage"));
		this.printCurrent.setBounds(new Rectangle(23, 580, 100, 22));
		// printCurrent.setSelected(true);

		this.printPages.setText(Messages.getMessage("PdfViewerNUPOption.Pages"));
		this.printPages.setBounds(new Rectangle(23, 600, 70, 22));

		this.pagesBox.setBounds(new Rectangle(95, 602, 230, 22));
		this.pagesBox.addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(KeyEvent arg0) {}

			@Override
			public void keyReleased(KeyEvent arg0) {
				if (ExtractPDFPagesNup.this.pagesBox.getText().length() == 0) ExtractPDFPagesNup.this.printCurrent.setSelected(true);
				else ExtractPDFPagesNup.this.printPages.setSelected(true);

			}

			@Override
			public void keyTyped(KeyEvent arg0) {}
		});

		JTextArea pagesInfo = new JTextArea(Messages.getMessage("PdfViewerMessage.PageNumberOrRangeLong"));
		pagesInfo.setBounds(new Rectangle(23, 640, 600, 40));
		pagesInfo.setOpaque(false);

		pageSelectionChanged(null);

		this.add(this.rootDir, null);
		this.add(this.rootFilesLabel, null);
		this.add(this.changeButton, null);

		this.add(this.printAll, null);
		this.add(this.printCurrent, null);

		this.add(scale);
		this.add(layout);
		this.add(margins);

		this.add(this.layoutColumns);
		this.add(this.layoutRows);
		this.add(this.layouts);
		this.add(this.leftRightMargins);
		this.add(this.scaleHeight);
		this.add(this.pageProportionally);
		this.add(this.pageScalings);
		this.add(this.scaleWidth);
		this.add(this.paperHeight);
		this.add(this.paperOrientation);
		this.add(this.paperSizes);
		this.add(this.paperWidth);
		this.add(this.topBottomMargins);
		this.add(this.verticalSpacing);

		this.add(this.horizontalSpacing);
		this.add(this.jLabel1);
		this.add(this.jLabel2);
		this.add(this.jLabel3);
		this.add(this.jLabel4);
		this.add(this.jLabel11);
		this.add(this.jLabel12);
		this.add(this.jLabel13);
		this.add(this.jLabel14);
		this.add(this.jLabel15);
		this.add(this.jLabel16);
		this.add(this.jLabel17);

		this.add(pageSettings);
		this.add(jLabel18);
		this.add(this.repeat);
		this.add(jLabel19);
		this.add(this.copies);
		this.add(jLabel20);
		this.add(this.ordering);
		// this.add(jLabel21);
		// this.add(doubleSided);

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

	private void layoutsSelectionChanged(java.awt.event.ItemEvent evt) {
		String layout = (String) this.layouts.getSelectedItem();

		if (layout.equals("2 Up")) {
			this.layoutRows.getModel().setValue(1);
			this.layoutColumns.getModel().setValue(2);

			this.layoutRows.setEnabled(false);
			this.layoutColumns.setEnabled(false);
		}
		else
			if (layout.equals("4 Up")) {
				this.layoutRows.getModel().setValue(2);
				this.layoutColumns.getModel().setValue(2);

				this.layoutRows.setEnabled(false);
				this.layoutColumns.setEnabled(false);

			}
			else
				if (layout.equals("8 Up")) {
					this.layoutRows.getModel().setValue(2);
					this.layoutColumns.getModel().setValue(4);

					this.layoutRows.setEnabled(false);
					this.layoutColumns.setEnabled(false);

				}
				else
					if (layout.equals("Custom")) {
						this.layoutRows.setEnabled(true);
						this.layoutColumns.setEnabled(true);
					}
	}

	private void scalingSelectionChanged(java.awt.event.ItemEvent evt) {
		String scaling = (String) this.pageScalings.getSelectedItem();

		if (scaling.equals("Use Original Size")) {
			this.pageProportionally.setEnabled(false);
			this.scaleWidth.setEnabled(false);
			this.scaleHeight.setEnabled(false);
		}
		else
			if (scaling.equals("Auto")) {
				this.pageProportionally.setEnabled(true);
				this.scaleWidth.setEnabled(false);
				this.scaleHeight.setEnabled(false);
			}
			else
				if (scaling.equals("Specified")) {
					this.pageProportionally.setEnabled(true);
					this.scaleWidth.setEnabled(true);
					this.scaleHeight.setEnabled(true);
				}
	}

	private void pageSelectionChanged(java.awt.event.ItemEvent evt) {

		Dimension d = getPaperDimension((String) this.paperSizes.getSelectedItem());

		if (d == null) {
			this.paperWidth.setEnabled(true);
			this.paperHeight.setEnabled(true);
		}
		else {
			this.paperWidth.setEnabled(false);
			this.paperHeight.setEnabled(false);

			this.paperWidth.setValue(d.width);
			this.paperHeight.setValue(d.height);
		}
	}

	private void genertatePaperSizes() {
		this.papers = new ArrayList<String>();
		this.paperDimensions = new ArrayList<Dimension>();

		this.papers.add(Messages.getMessage("PdfViewerNUPComboBoxOption.Letter"));
		this.papers.add(Messages.getMessage("PdfViewerNUPComboBoxOption.Legal"));
		this.papers.add("11x17");
		this.papers.add(Messages.getMessage("PdfViewerNUPComboBoxOption.Ledger"));
		this.papers.add("A2");
		this.papers.add("A3");
		this.papers.add("A4");
		this.papers.add("A5");
		this.papers.add("B3");
		this.papers.add("B4");
		this.papers.add("B5");
		this.papers.add(Messages.getMessage("PdfViewerNUPComboBoxOption.Folio"));
		this.papers.add(Messages.getMessage("PdfViewerNUPComboBoxOption.Status"));
		this.papers.add(Messages.getMessage("PdfViewerNUPComboBoxOption.Note"));
		this.papers.add(Messages.getMessage("PdfViewerNUPComboBoxOption.Custom"));

		this.paperDimensions.add(new Dimension(612, 792));
		this.paperDimensions.add(new Dimension(612, 1008));
		this.paperDimensions.add(new Dimension(792, 1224));
		this.paperDimensions.add(new Dimension(1224, 792));
		this.paperDimensions.add(new Dimension(1190, 1684));
		this.paperDimensions.add(new Dimension(842, 1190));
		this.paperDimensions.add(new Dimension(595, 842));
		this.paperDimensions.add(new Dimension(421, 595));
		this.paperDimensions.add(new Dimension(1002, 1418));
		this.paperDimensions.add(new Dimension(709, 1002));
		this.paperDimensions.add(new Dimension(501, 709));
		this.paperDimensions.add(new Dimension(612, 936));
		this.paperDimensions.add(new Dimension(396, 612));
		this.paperDimensions.add(new Dimension(540, 720));

		// paperSizesMap.put("Custom",null);
	}

	private String[] getPaperSizes() {
		return this.papers.toArray(new String[this.papers.size()]);
	}

	private Dimension getPaperDimension(String paper) {
		if (paper.equals("Custom")) return null;

		return this.paperDimensions.get(this.papers.indexOf(paper));
	}

	@Override
	final public Dimension getPreferredSize() {
		return new Dimension(620, 680);
	}
}
