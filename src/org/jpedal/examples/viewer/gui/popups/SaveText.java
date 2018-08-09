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
 * SaveText.java
 * ---------------
 */
package org.jpedal.examples.viewer.gui.popups;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JToggleButton;

import org.jpedal.examples.viewer.utils.Exporter;
import org.jpedal.utils.Messages;

public class SaveText extends Save {

	private static final long serialVersionUID = 2521947776981324925L;
	JLabel outputFileTypeLabel = new JLabel();
	JLabel outputFormat = new JLabel();

	ButtonGroup buttonGroup1 = new ButtonGroup();
	ButtonGroup buttonGroup2 = new ButtonGroup();

	JToggleButton jToggleButton3 = new JToggleButton();

	JToggleButton jToggleButton2 = new JToggleButton();

	JRadioButton isPlainText = new JRadioButton();
	JRadioButton isXML = new JRadioButton();

	JRadioButton isWordlist = new JRadioButton();
	JRadioButton isTable = new JRadioButton();
	JRadioButton isRectangle = new JRadioButton();

	public SaveText(String root_dir, int end_page, int currentPage) {

		super(root_dir, end_page, currentPage);

		try {
			jbInit();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public boolean isXMLExtaction() {
		return this.isXML.isSelected();
	}

	public int getTextType() {
		int prefix = Exporter.RECTANGLE;

		if (this.isWordlist.isSelected()) prefix = Exporter.WORDLIST;
		if (this.isTable.isSelected()) prefix = Exporter.TABLE;

		return prefix;
	}

	@Override
	final public Dimension getPreferredSize() {
		return new Dimension(490, 280);
	}

	private void jbInit() throws Exception {

		this.rootFilesLabel.setBounds(new Rectangle(13, 13, 400, 26));

		this.rootDir.setBounds(new Rectangle(23, 40, 232, 23));
		this.changeButton.setBounds(new Rectangle(272, 39, 101, 23));

		this.pageRangeLabel.setBounds(new Rectangle(13, 71, 400, 26));

		this.startLabel.setBounds(new Rectangle(23, 100, 150, 22));
		this.startPage.setBounds(new Rectangle(150, 100, 75, 22));

		this.endLabel.setBounds(new Rectangle(260, 100, 75, 22));
		this.endPage.setBounds(new Rectangle(320, 100, 75, 22));

		this.optionsForFilesLabel.setBounds(new Rectangle(13, 134, 400, 26));

		this.outputFileTypeLabel.setText(Messages.getMessage("PdfViewerMessage.OutputType"));
		this.outputFileTypeLabel.setBounds(new Rectangle(23, 174, 164, 19));

		this.isPlainText.setText(Messages.getMessage("PdfViewerOption.PlainText"));
		this.isPlainText.setBounds(new Rectangle(180, 174, 100, 19));

		this.isXML.setBounds(new Rectangle(280, 174, 95, 19));
		this.isXML.setSelected(true);
		this.isXML.setText(Messages.getMessage("PdfViewerOption.XML"));

		this.outputFormat.setText(Messages.getMessage("PdfViewerMessage.OutputFormat"));
		this.outputFormat.setBounds(new Rectangle(23, 214, 164, 19));

		this.isRectangle.setText(Messages.getMessage("PdfViewerOption.Rectangle"));
		this.isRectangle.setBounds(new Rectangle(180, 214, 75, 19));
		this.isRectangle.setSelected(true);
		this.isRectangle.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				SaveText.this.isPlainText.setText(Messages.getMessage("PdfViewerOption.PlainText"));
			}
		});

		this.isWordlist.setText(Messages.getMessage("PdfViewerOption.Wordlist"));
		this.isWordlist.setBounds(new Rectangle(280, 214, 100, 19));
		// isWordlist.setBounds( new Rectangle(300, 214, 95, 19 ) );
		this.isWordlist.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				SaveText.this.isPlainText.setText(Messages.getMessage("PdfViewerOption.PlainText"));
			}
		});

		this.isTable.setText(Messages.getMessage("PdfViewerOption.Table"));
		this.isTable.setBounds(new Rectangle(225, 214, 75, 19));
		this.isTable.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				SaveText.this.isPlainText.setText(Messages.getMessage("PdfViewerOption.CSV"));
			}
		});

		// common
		this.add(this.startPage, null);
		this.add(this.endPage, null);
		this.add(this.rootDir, null);
		this.add(this.rootFilesLabel, null);
		this.add(this.changeButton, null);
		this.add(this.endLabel, null);
		this.add(this.startLabel, null);
		this.add(this.pageRangeLabel, null);

		this.add(this.optionsForFilesLabel, null);
		this.add(this.outputFileTypeLabel, null);
		this.add(this.jToggleButton2, null);
		this.add(this.jToggleButton3, null);
		this.add(this.isPlainText, null);
		this.add(this.isXML, null);
		this.add(this.outputFormat, null);
		this.add(this.isRectangle, null);
		this.add(this.isWordlist, null);

		this.buttonGroup1.add(this.isXML);
		this.buttonGroup1.add(this.isPlainText);

		this.buttonGroup2.add(this.isRectangle);
		this.buttonGroup2.add(this.isTable);
		this.buttonGroup2.add(this.isWordlist);
	}
}
