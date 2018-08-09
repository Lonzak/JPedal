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
 * SaveImage.java
 * ---------------
 */
package org.jpedal.examples.viewer.gui.popups;

import java.awt.Dimension;
import java.awt.Rectangle;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JToggleButton;

import org.jpedal.PdfDecoder;
import org.jpedal.utils.Messages;

public class SaveImage extends Save {

	private static final long serialVersionUID = -8481998906468591630L;

	private ButtonGroup buttonGroup1 = new ButtonGroup();

	private JToggleButton jToggleButton2 = new JToggleButton();
	private JToggleButton jToggleButton3 = new JToggleButton();

	private JLabel OutputLabel = new JLabel();
	private JRadioButton isPNG = new JRadioButton();
	private JRadioButton isTiff = new JRadioButton();
	private JRadioButton isJPEG = new JRadioButton();

	private JRadioButton isHires = new JRadioButton();
	private JRadioButton isNormal = new JRadioButton();
	private JRadioButton isDownsampled = new JRadioButton();

	private ButtonGroup buttonGroup2 = new ButtonGroup();

	public SaveImage(String root_dir, int end_page, int currentPage) {

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
	final public String getPrefix() {
		String prefix = "png";
		if (this.isTiff.isSelected()) prefix = "tif";
		if (this.isJPEG.isSelected()) prefix = "jpg";
		return prefix;
	}

	/**
	 * get root dir
	 */
	final public int getImageType() {
		int prefix = PdfDecoder.CLIPPEDIMAGES;

		if (this.isNormal.isSelected()) prefix = PdfDecoder.RAWIMAGES;
		if (this.isDownsampled.isSelected()) prefix = PdfDecoder.FINALIMAGES;

		return prefix;
	}

	private void jbInit() throws Exception {

		this.rootFilesLabel.setBounds(new Rectangle(13, 12, 400, 26));

		this.rootDir.setBounds(new Rectangle(23, 39, 232, 23));

		this.changeButton.setBounds(new Rectangle(272, 39, 101, 23));

		this.startPage.setBounds(new Rectangle(125, 99, 75, 22));

		this.pageRangeLabel.setBounds(new Rectangle(13, 70, 400, 26));

		this.startLabel.setBounds(new Rectangle(23, 100, 100, 22));

		this.endLabel.setBounds(new Rectangle(220, 99, 75, 22));

		this.endPage.setBounds(new Rectangle(285, 99, 75, 22));

		this.optionsForFilesLabel.setBounds(new Rectangle(13, 133, 600, 26));

		this.OutputLabel.setText(Messages.getMessage("PdfViewerMessage.OutputType"));
		this.OutputLabel.setBounds(new Rectangle(23, 173, 900, 24));
		this.isTiff.setText("Tiff");
		this.isTiff.setBounds(new Rectangle(180, 175, 50, 19));
		this.isJPEG.setBounds(new Rectangle(290, 174, 67, 19));
		this.isJPEG.setSelected(true);
		this.isJPEG.setText("JPEG");
		this.isPNG.setBounds(new Rectangle(360, 174, 62, 19));
		this.isPNG.setText("PNG");

		this.isHires.setText(Messages.getMessage("PdfViewerOption.Hires"));
		this.isHires.setBounds(new Rectangle(180, 200, 112, 19));
		this.isHires.setSelected(true);
		this.isNormal.setBounds(new Rectangle(290, 200, 73, 19));
		this.isNormal.setText(Messages.getMessage("PdfViewerOption.Normal"));
		this.isDownsampled.setBounds(new Rectangle(360, 200, 200, 19));
		this.isDownsampled.setText(Messages.getMessage("PdfViewerOption.Downsampled"));

		// common
		this.add(this.startPage, null);
		this.add(this.endPage, null);
		this.add(this.rootDir, null);
		this.add(this.scaling, null);
		this.add(this.scalingLabel, null);
		this.add(this.rootFilesLabel, null);
		this.add(this.changeButton, null);
		this.add(this.endLabel, null);
		this.add(this.startLabel, null);
		this.add(this.pageRangeLabel, null);

		this.add(this.optionsForFilesLabel, null);
		this.add(this.jToggleButton2, null);
		this.add(this.jToggleButton3, null);
		this.add(this.OutputLabel, null);
		this.add(this.isTiff, null);
		this.add(this.isJPEG, null);
		this.add(this.isPNG, null);
		this.buttonGroup1.add(this.isTiff);
		this.buttonGroup1.add(this.isJPEG);
		this.buttonGroup1.add(this.isPNG);

		this.add(this.isHires, null);
		this.add(this.isNormal, null);
		this.add(this.isDownsampled, null);
		this.buttonGroup2.add(this.isHires);
		this.buttonGroup2.add(this.isNormal);
		this.buttonGroup2.add(this.isDownsampled);
	}

	@Override
	final public Dimension getPreferredSize() {
		return new Dimension(500, 250);
	}
}
