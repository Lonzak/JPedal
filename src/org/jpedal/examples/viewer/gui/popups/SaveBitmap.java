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
 * SaveBitmap.java
 * ---------------
 */
package org.jpedal.examples.viewer.gui.popups;

import java.awt.Dimension;
import java.awt.Rectangle;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JToggleButton;

import org.jpedal.utils.Messages;

/** specific code for Bitmap save function */
public class SaveBitmap extends Save {

	private static final long serialVersionUID = 736637161324692392L;
	JLabel OutputLabel = new JLabel();
	ButtonGroup buttonGroup1 = new ButtonGroup();
	JToggleButton jToggleButton3 = new JToggleButton();

	JToggleButton jToggleButton2 = new JToggleButton();

	JRadioButton isPNG = new JRadioButton();

	JRadioButton isTiff = new JRadioButton();

	JRadioButton isJPEG = new JRadioButton();

	public SaveBitmap(String root_dir, int end_page, int currentPage) {
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
	final public String getPrefix() {
		String prefix = "png";
		if (this.isTiff.isSelected()) prefix = "tif";
		if (this.isJPEG.isSelected()) prefix = "jpg";
		return prefix;
	}

	@Override
	final public Dimension getPreferredSize() {
		return new Dimension(490, 280);
	}

	private void jbInit() throws Exception {

		this.scalingLabel.setBounds(new Rectangle(13, 12, 400, 19));

		this.scaling.setBounds(new Rectangle(400, 12, 69, 23));

		this.rootFilesLabel.setBounds(new Rectangle(13, 55, 400, 26));

		this.rootDir.setBounds(new Rectangle(23, 82, 232, 23));

		this.changeButton.setBounds(new Rectangle(272, 82, 101, 23));

		this.OutputLabel.setText(Messages.getMessage("PdfViewerMessage.OutputType"));
		this.OutputLabel.setBounds(new Rectangle(23, 216, 300, 24));
		this.isTiff.setText("Tiff");
		this.isTiff.setBounds(new Rectangle(180, 218, 50, 19));
		this.isJPEG.setBounds(new Rectangle(240, 217, 67, 19));
		this.isJPEG.setSelected(true);
		this.isJPEG.setText("JPEG");
		this.isPNG.setBounds(new Rectangle(305, 217, 62, 19));
		this.isPNG.setText("PNG");
		this.isPNG.setName("radioPNG");

		this.optionsForFilesLabel.setBounds(new Rectangle(13, 176, 600, 26));

		this.startPage.setBounds(new Rectangle(125, 142, 75, 22));

		this.pageRangeLabel.setBounds(new Rectangle(13, 113, 400, 26));

		this.startLabel.setBounds(new Rectangle(23, 142, 100, 22));

		this.endLabel.setBounds(new Rectangle(220, 142, 75, 22));

		this.endPage.setBounds(new Rectangle(285, 142, 75, 22));

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
		this.add(this.OutputLabel, null);
		this.add(this.jToggleButton2, null);
		this.add(this.jToggleButton3, null);
		this.add(this.isTiff, null);
		this.add(this.isJPEG, null);
		this.add(this.isPNG, null);

		this.buttonGroup1.add(this.isTiff);
		this.buttonGroup1.add(this.isJPEG);
		this.buttonGroup1.add(this.isPNG);
	}

}
