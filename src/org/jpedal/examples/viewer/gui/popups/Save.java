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
 * Save.java
 * ---------------
 */
package org.jpedal.examples.viewer.gui.popups;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.jpedal.examples.viewer.Viewer;
import org.jpedal.examples.viewer.utils.FileFilterer;
import org.jpedal.utils.LogWriter;
import org.jpedal.utils.Messages;

/** allow user to select page range and values to save */
public class Save extends JComponent {

	private static final long serialVersionUID = -137153741770519366L;
	protected JTextField startPage = new JTextField();
	protected JTextField endPage = new JTextField();
	protected JTextField rootDir = new JTextField();

	protected Object[] scales = { "10", "25", "50", "75", "100" };
	protected JComboBox scaling = new JComboBox(this.scales);

	protected JLabel scalingLabel = new JLabel();
	protected JLabel rootFilesLabel = new JLabel();

	protected JButton changeButton = new JButton();

	protected JLabel endLabel = new JLabel();
	protected JLabel startLabel = new JLabel();
	protected JLabel pageRangeLabel = new JLabel();

	protected String root_dir;
	protected int end_page;
	protected int currentPage;

	protected JLabel optionsForFilesLabel = new JLabel();

	public Save(final String root_dir, int end_page, int currentPage) {

		this.currentPage = currentPage;
		this.root_dir = root_dir;
		this.end_page = end_page;

		this.scalingLabel.setFont(new java.awt.Font("Dialog", Font.BOLD, 14));
		this.scalingLabel.setText(Messages.getMessage("PdfViewerOption.Scaling") + '\n');
		this.scaling.setSelectedItem("100");
		this.scaling.setName("exportScaling");

		this.rootFilesLabel.setFont(new java.awt.Font("Dialog", Font.BOLD, 14));
		this.rootFilesLabel.setDisplayedMnemonic('0');
		this.rootFilesLabel.setText(Messages.getMessage("PdfViewerOption.RootDir"));
		this.rootDir.setText(root_dir);
		this.rootDir.setName("extRootDir");

		this.changeButton.setText(Messages.getMessage("PdfViewerOption.Browse"));
		this.changeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser = new JFileChooser(root_dir);
				chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
				String[] png = new String[] { "png", "tif", "tiff", "jpg", "jpeg" }; //$NON-NLS-1$
				chooser.addChoosableFileFilter(new FileFilterer(png, "Images (Tiff, Jpeg,Png)")); //$NON-NLS-1$
				chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				int state = chooser.showOpenDialog(null);

				File file = chooser.getSelectedFile();

				if (file != null && state == JFileChooser.APPROVE_OPTION) {
					Save.this.rootDir.setText(file.getAbsolutePath());
				}
			}
		});

		this.optionsForFilesLabel.setText(Messages.getMessage("PdfViewerOption.Output"));
		this.optionsForFilesLabel.setFont(new java.awt.Font("Dialog", Font.BOLD, 14));
		this.optionsForFilesLabel.setDisplayedMnemonic('0');

		this.pageRangeLabel.setText(Messages.getMessage("PdfViewerOption.PageRange"));
		this.pageRangeLabel.setFont(new java.awt.Font("Dialog", Font.BOLD, 14));
		this.pageRangeLabel.setDisplayedMnemonic('0');

		this.startLabel.setText(Messages.getMessage("PdfViewerOption.StartPage"));
		this.endLabel.setText(Messages.getMessage("PdfViewerOption.EndPage"));

		this.startPage.setText("1");
		this.endPage.setText(String.valueOf(end_page));
	}

	/**
	 * get scaling value
	 */
	final public int getScaling() {
		return Integer.parseInt((String) this.scaling.getSelectedItem());
	}

	/** popup display for user to make selection */
	public int display(Component c, String title) {

		setSize(400, 200);
		JPanel popupPanel = new JPanel();
		popupPanel.setLayout(new BorderLayout());
		popupPanel.add(this, BorderLayout.CENTER);
		popupPanel.setSize(400, 200);
		Object[] options = { Messages.getMessage("PdfMessage.Ok"), Messages.getMessage("PdfMessage.Cancel") };

		if (Viewer.showMessages) return JOptionPane.showOptionDialog(c, popupPanel, title,

		JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
		else return 0;
	}

	/**
	 * get start page
	 */
	final public int getStartPage() {

		int page = -1;

		try {
			page = Integer.parseInt(this.startPage.getText());
		}
		catch (Exception e) {
			LogWriter.writeLog("Exception " + e + " in exporting");
			if (Viewer.showMessages) JOptionPane.showMessageDialog(this, Messages.getMessage("PdfViewerError.InvalidSyntax"));
		}

		if (page < 1) {
			if (Viewer.showMessages) JOptionPane.showMessageDialog(this, Messages.getMessage("PdfViewerError.NegativePageValue"));
		}

		if (page > this.end_page) {
			if (Viewer.showMessages) JOptionPane.showMessageDialog(
					this,
					Messages.getMessage("PdfViewerText.Page") + ' ' + page + ' ' + Messages.getMessage("PdfViewerError.OutOfBounds") + ' '
							+ Messages.getMessage("PdfViewerText.PageCount") + ' ' + this.end_page);

			page = -1;
		}

		return page;
	}

	/**
	 * get root dir
	 */
	final public String getRootDir() {
		return this.rootDir.getText();
	}

	/**
	 * get end page
	 */
	final public int getEndPage() {

		int page = -1;
		try {
			page = Integer.parseInt(this.endPage.getText());
		}
		catch (Exception e) {
			LogWriter.writeLog("Exception " + e + " in exporting");

			if (Viewer.showMessages) JOptionPane.showMessageDialog(this, Messages.getMessage("PdfViewerError.InvalidSyntax"));
		}

		if (page < 1) {
			if (Viewer.showMessages) JOptionPane.showMessageDialog(this, Messages.getMessage("PdfViewerError.NegativePageValue"));
		}
		if (page > this.end_page) {
			if (Viewer.showMessages) JOptionPane.showMessageDialog(
					this,
					Messages.getMessage("PdfViewerText.Page") + ' ' + page + ' ' + Messages.getMessage("PdfViewerError.OutOfBounds") + ' '
							+ Messages.getMessage("PdfViewerText.PageCount") + ' ' + this.end_page);

			page = -1;
		}
		return page;
	}

	/**
	 * size
	 */
	@Override
	final public Dimension getSize() {
		return getPreferredSize();
	}

	/**
	 * size
	 */
	@Override
	public Dimension getPreferredSize() {
		return new Dimension(400, 330);
	}

	/**
	 * size
	 */
	@Override
	final public Dimension getMinimumSize() {
		return getPreferredSize();
	}

	/**
	 * size
	 */
	@Override
	final public Dimension getMaximumSize() {
		return getPreferredSize();
	}
}
