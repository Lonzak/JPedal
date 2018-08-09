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
 * EncryptPDFDocument.java
 * ---------------
 */
package org.jpedal.examples.viewer.gui.popups;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;

public class EncryptPDFDocument extends Save {

	private static final long serialVersionUID = -2200153563871754545L;

	JLabel OutputLabel = new JLabel();

	JToggleButton jToggleButton3 = new JToggleButton();

	JToggleButton jToggleButton2 = new JToggleButton();

	JCheckBox userPasswordCheck = new JCheckBox("Password required to open document");
	JCheckBox masterPasswordCheck = new JCheckBox("Password required to chnge permissions and passwords");

	JCheckBox printing = new JCheckBox("No Printing");
	JCheckBox modifyDocument = new JCheckBox("No modifying the document");
	JCheckBox contentExtract = new JCheckBox("No content copying or extraction");
	JCheckBox modifyAnnotations = new JCheckBox("No modifying annotations");
	JCheckBox formFillIn = new JCheckBox("No form fields fill-in");

	JTextField userPasswordBox = new JTextField();
	JTextField masterPasswordBox = new JTextField();

	final String[] securityItems = { "128-bit RC4 (Acrobat 5.0 and up)", "40-bit RC4 (Acrobat 3.x, 4.x)" };

	JComboBox encryptionLevel = new JComboBox(this.securityItems);

	public EncryptPDFDocument(String root_dir, int end_page, int currentPage) {
		super(root_dir, end_page, currentPage);

		try {
			jbInit();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void jbInit() throws Exception {

		this.pageRangeLabel.setText("Password");
		this.pageRangeLabel.setBounds(new Rectangle(13, 13, 199, 26));

		this.userPasswordCheck.setBounds(new Rectangle(23, 40, 300, 22));
		this.userPasswordCheck.addMouseListener(new MouseListener() {
			@Override
			public void mouseClicked(MouseEvent e) {
				EncryptPDFDocument.this.userPasswordBox.setEditable(EncryptPDFDocument.this.userPasswordCheck.isSelected());
			}

			@Override
			public void mouseEntered(MouseEvent e) {}

			@Override
			public void mouseExited(MouseEvent e) {}

			@Override
			public void mousePressed(MouseEvent e) {}

			@Override
			public void mouseReleased(MouseEvent e) {}
		});

		JLabel userPasswordLabel = new JLabel("User password:");
		userPasswordLabel.setBounds(new Rectangle(50, 70, 100, 22));
		this.userPasswordBox.setBounds(new Rectangle(180, 70, 150, 22));
		this.userPasswordBox.setEditable(false);

		this.masterPasswordCheck.setBounds(new Rectangle(23, 100, 440, 22));
		this.masterPasswordCheck.addMouseListener(new MouseListener() {
			@Override
			public void mouseClicked(MouseEvent e) {
				EncryptPDFDocument.this.masterPasswordBox.setEditable(EncryptPDFDocument.this.masterPasswordCheck.isSelected());
			}

			@Override
			public void mouseEntered(MouseEvent e) {}

			@Override
			public void mouseExited(MouseEvent e) {}

			@Override
			public void mousePressed(MouseEvent e) {}

			@Override
			public void mouseReleased(MouseEvent e) {}
		});

		JLabel masterPasswordLabel = new JLabel("Master password:");
		masterPasswordLabel.setBounds(new Rectangle(50, 130, 120, 22));
		this.masterPasswordBox.setBounds(new Rectangle(180, 130, 150, 22));
		this.masterPasswordBox.setEditable(false);

		JLabel permissionsLabel = new JLabel("Permissions");
		permissionsLabel.setFont(new java.awt.Font("Dialog", Font.BOLD, 14));
		permissionsLabel.setDisplayedMnemonic('0');
		permissionsLabel.setBounds(new Rectangle(13, 180, 199, 26));

		JLabel encryptionLevelLabel = new JLabel("Encryption Level:");
		encryptionLevelLabel.setBounds(new Rectangle(23, 210, 125, 22));

		this.encryptionLevel.setBounds(new Rectangle(150, 210, 250, 22));

		this.printing.setBounds(new Rectangle(23, 250, 200, 22));
		this.modifyDocument.setBounds(new Rectangle(23, 280, 200, 22));
		this.contentExtract.setBounds(new Rectangle(23, 310, 220, 22));
		this.modifyAnnotations.setBounds(new Rectangle(23, 340, 200, 22));
		this.formFillIn.setBounds(new Rectangle(23, 370, 200, 22));

		this.add(this.changeButton, null);
		this.add(this.pageRangeLabel, null);

		this.add(this.userPasswordCheck, null);
		this.add(this.masterPasswordCheck, null);

		this.add(userPasswordLabel);
		this.add(this.userPasswordBox, null);

		this.add(masterPasswordLabel);
		this.add(this.masterPasswordBox, null);

		this.add(permissionsLabel, null);
		this.add(encryptionLevelLabel, null);
		this.add(this.encryptionLevel, null);

		this.add(this.printing);
		this.add(this.modifyDocument);
		this.add(this.contentExtract);
		this.add(this.modifyAnnotations);
		this.add(this.formFillIn);

		this.add(this.jToggleButton2, null);
		this.add(this.jToggleButton3, null);
	}

	@Override
	final public Dimension getPreferredSize() {
		return new Dimension(420, 400);
	}

	public String getPermissions() {
		String permissions = "";

		if (this.printing.isSelected()) permissions += "1";
		else permissions += "0";

		if (this.modifyDocument.isSelected()) permissions += "1";
		else permissions += "0";

		if (this.contentExtract.isSelected()) permissions += "1";
		else permissions += "0";

		if (this.modifyAnnotations.isSelected()) permissions += "1";
		else permissions += "0";

		if (this.formFillIn.isSelected()) permissions += "1";
		else permissions += "0";

		return permissions;
	}

	public int getEncryptionLevel() {
		return this.encryptionLevel.getSelectedIndex();
	}

	public String getMasterPassword() {
		return this.masterPasswordBox.getText();
	}

	public String getUserPassword() {
		return this.userPasswordBox.getText();
	}
}
