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
 * SignWizardModel.java
 * ---------------
 */

package org.jpedal.examples.viewer.gui.popups;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jpedal.PdfDecoder;
import org.jpedal.examples.viewer.objects.SignData;
import org.jpedal.examples.viewer.utils.FileFilterer;
import org.jpedal.examples.viewer.utils.ItextFunctions;
import org.jpedal.exception.PdfException;
import org.jpedal.objects.PdfPageData;
import org.jpedal.objects.acroforms.rendering.AcroRenderer;
import org.jpedal.utils.LogWriter;
import org.jpedal.utils.Messages;

/**
 * This class implements the WizardPanelModel and in this case contains the JPanels to be drawn as inner classes. The methods in SignWizardModel are
 * mainly concerned with controlling what panels are next and whether they can be currently reached.
 */
public class SignWizardModel implements WizardPanelModel {
	// Each panel must have a unique String identifier
	private static final String MODE_SELECT = "0";
	private static final String PFX_PANEL = "1";
	private static final String KEYSTORE_PANEL = "3";
	private static final String COMMON_PANEL = "4";
	private static final String ENCRYPTION_PANEL = "5";
	private static final String VISIBLE_SIGNATURE_PANEL = "6";

	public static final String NO_FILE_SELECTED = Messages.getMessage("PdfSigner.NoFileSelected");

	private static final int MAXIMUM_PANELS = 5;

	private SignData signData;
	private PdfDecoder pdfDecoder;
	private String rootDir;

	/* The JPanels in this wizard */
	private ModeSelect modeSelect;
	private PFXPanel pFXPanel;
	private KeystorePanel keystorePanel;
	private CommonPanel commonPanel;
	private EncryptionPanel encryptionPanel;
	private SignaturePanel signaturePanel;

	/* Maps the JPanels' ID to the panel */
	private Map panels;

	/* The ID of the currently displayed panel */
	private String currentPanel;

	/**
	 * @param signData
	 *            Will contain all the information acquired from the user for signing a Pdf
	 * @param pdfFile
	 *            The path to the Pdf document to be signed.
	 */
	public SignWizardModel(SignData signData, String pdfFile, String rootDir) {
		this.signData = signData;
		this.rootDir = rootDir;

		this.pdfDecoder = new PdfDecoder();
		try {
			this.pdfDecoder.openPdfFile(pdfFile);
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		if (this.pdfDecoder.isEncrypted()) {
			String password = System.getProperty("org.jpedal.password");
			if (password != null) {
				try {
					this.pdfDecoder.setEncryptionPassword(password);
				}
				catch (PdfException e) {
					e.printStackTrace();
				}
			}
		}
		/* JPanel contents vary depending on whether the Pdf has bee previously signed. */
		testForSignedPDF();

		this.panels = new HashMap();
		this.modeSelect = new ModeSelect();
		this.pFXPanel = new PFXPanel();
		this.keystorePanel = new KeystorePanel();
		this.commonPanel = new CommonPanel();
		this.encryptionPanel = new EncryptionPanel();
		this.signaturePanel = new SignaturePanel();

		this.panels.put(MODE_SELECT, this.modeSelect);
		this.panels.put(PFX_PANEL, this.pFXPanel);
		this.panels.put(KEYSTORE_PANEL, this.keystorePanel);
		this.panels.put(COMMON_PANEL, this.commonPanel);
		this.panels.put(ENCRYPTION_PANEL, this.encryptionPanel);
		this.panels.put(VISIBLE_SIGNATURE_PANEL, this.signaturePanel);

		this.currentPanel = MODE_SELECT;
	}

	/**
	 * A map of the JPanels the Wizard Dialog should contain.
	 * 
	 * @return The ID strings mapped to their corresponding JPanels
	 */
	@Override
	public Map getJPanels() {
		return this.panels;
	}

	/**
	 * Advance to the next JPanel.
	 * 
	 * @return Unique identifier for the now current JPanel
	 */
	@Override
	public String next() {
		updateSignData();

		if (this.currentPanel.equals(MODE_SELECT)) {
			if (!this.signData.isKeystoreSign()) {
				return this.currentPanel = PFX_PANEL;
			}
			else {
				return this.currentPanel = KEYSTORE_PANEL;
			}
		}
		else
			if (this.currentPanel.equals(PFX_PANEL)) {
				return this.currentPanel = VISIBLE_SIGNATURE_PANEL;
			}
			else
				if (this.currentPanel.equals(KEYSTORE_PANEL)) {
					return this.currentPanel = VISIBLE_SIGNATURE_PANEL;
				}
				else
					if (this.currentPanel.equals(VISIBLE_SIGNATURE_PANEL)) {
						return this.currentPanel = ENCRYPTION_PANEL;
					}
					else
						if (this.currentPanel.equals(ENCRYPTION_PANEL)) {
							return this.currentPanel = COMMON_PANEL;
						}
		/*
		 * The following exception should never be thrown and is here to alerted me should I create a trail of panels that is incorrect
		 */
		throw new NullPointerException("Whoops! Tried to move to a nextID where there is no nextID to be had");
	}

	/**
	 * Set the current JPanel to the previous JPanel.
	 * 
	 * @return Unique identifier for the now current JPanel
	 */
	@Override
	public String previous() {
		updateSignData();
		if (this.currentPanel.equals(PFX_PANEL) || this.currentPanel.equals(KEYSTORE_PANEL)) {
			return this.currentPanel = MODE_SELECT;
		}
		else
			if (this.currentPanel.equals(ENCRYPTION_PANEL)) {
				return this.currentPanel = VISIBLE_SIGNATURE_PANEL;
			}
			else
				if (this.currentPanel.equals(VISIBLE_SIGNATURE_PANEL)) {
					if (this.signData.isKeystoreSign()) {
						return this.currentPanel = KEYSTORE_PANEL;
					}
					else {
						return this.currentPanel = PFX_PANEL;
					}
				}
				else
					if (this.currentPanel.equals(COMMON_PANEL)) {
						return this.currentPanel = ENCRYPTION_PANEL;
					}

		throw new NullPointerException("Tried to move to get a previousID where there is no previous");
	}

	@Override
	public boolean hasPrevious() {
		return !this.currentPanel.equals(MODE_SELECT);
	}

	@Override
	public String getStartPanelID() {
		return MODE_SELECT;
	}

	@Override
	public boolean isFinishPanel() {
		return this.currentPanel == COMMON_PANEL;
	}

	/**
	 * Indicates whether the next or finish button can be enabled.
	 * 
	 * @return true if the current panel can be advanced in its current state
	 */
	@Override
	public boolean canAdvance() {
		if (this.currentPanel.equals(COMMON_PANEL)) {
			return this.commonPanel.canFinish();
		}
		else
			if (this.currentPanel.equals(PFX_PANEL)) {
				return this.pFXPanel.canAdvance();
			}
			else
				if (this.currentPanel.equals(KEYSTORE_PANEL)) {
					return this.keystorePanel.canAdvance();
				}
				else
					if (this.currentPanel.equals(ENCRYPTION_PANEL)) {
						return this.encryptionPanel.canAdvance();
					}
					else {
						return true;
					}
	}

	/**
	 * Harvest user data from the currently displayed panel
	 */
	public void updateSignData() {
		if (this.currentPanel.equals(PFX_PANEL)) {
			this.pFXPanel.collectData();
		}
		else
			if (this.currentPanel.equals(KEYSTORE_PANEL)) {
				this.keystorePanel.collectData();
			}
			else
				if (this.currentPanel.equals(COMMON_PANEL)) {
					this.commonPanel.collectData();
				}
				else
					if (this.currentPanel.equals(ENCRYPTION_PANEL)) {
						this.encryptionPanel.collectData();
					}
					else
						if (this.currentPanel.equals(MODE_SELECT)) {
							this.modeSelect.collectData();
						}
						else
							if (this.currentPanel.equals(VISIBLE_SIGNATURE_PANEL)) {
								this.signaturePanel.collectData();
							}
							else {
								/* Should never be throw, here to indicate if I've made a mistake in the flow of the JPanels */
								throw new NullPointerException("Tried to update a panel which doesnt exist");
							}
	}

	/**
	 * When an event is triggered with one of the registered panels the wizard will call back this class and check if the panel can be advanced.
	 * 
	 * @param wizard
	 *            Listeners to enable/disable advance button
	 */
	@Override
	public void registerNextChangeListeners(ChangeListener wizard) {
		this.commonPanel.registerChange(wizard);
		this.pFXPanel.registerChange(wizard);
		this.keystorePanel.registerChange(wizard);
		this.encryptionPanel.registerChange(wizard);
	}

	/**
	 * Same as the previous method but listens for key changes instead.
	 * 
	 * @param wizard
	 *            Listeners to enable/disable advance button
	 */
	@Override
	public void registerNextKeyListeners(KeyListener wizard) {
		this.pFXPanel.registerListener(wizard);
		this.keystorePanel.registerNextKeyListeners(wizard);
		this.encryptionPanel.registerNextKeyListeners(wizard);
	}

	/**
	 * To avoid memory leaks I want to close the decoder I opened in this class when ever the dialog is closed. Also collects any last data.
	 */
	@Override
	public void close() {
		updateSignData();
		this.pdfDecoder.closePdfFile();
	}

	/**
	 * Don't want to corrupt any Pdf files so a check is performed to find whether a signature should be appended to the document or created fresh.
	 */
	private void testForSignedPDF() {
		this.signData.setAppend(false);

		for (int page = 1; page <= this.pdfDecoder.getPageCount(); page++) {
			try {
				this.pdfDecoder.decodePage(page);
				this.pdfDecoder.waitForDecodingToFinish();
				AcroRenderer currentFormRenderer = this.pdfDecoder.getFormRenderer();
				Iterator signatureObjects = currentFormRenderer.getSignatureObjects();
				if (signatureObjects != null) {
					this.signData.setAppend(true);
					break;
				}
			}
			catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private boolean isPdfSigned() {
		return this.signData.isAppendMode();
	}

	/**
	 * The individual JPanels that I want to show in the Wizard
	 */
	private class PFXPanel extends JPanel {

		private static final long serialVersionUID = 5362079107382052293L;
		private JLabel keyFileLabel = new JLabel();
		private JButton browseKeyButton = new JButton();
		private JLabel currentKeyFilePath = new JLabel(NO_FILE_SELECTED);
		private JCheckBox visiblePassCheck = new JCheckBox();

		private JLabel passwordLabel = new JLabel();
		private JPasswordField passwordField = new JPasswordField();

		private volatile boolean keyNext = false;
		private volatile boolean passNext = false;

		private int y = 0;

		public PFXPanel() {
			try {
				init();
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}

		private void init() throws Exception {
			setLayout(new BorderLayout());
			add(new TitlePanel(Messages.getMessage("PdfSigner.PfxSignMode")), BorderLayout.NORTH);

			JPanel inputPanel = new JPanel(new GridBagLayout());
			inputPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
			GridBagConstraints c = new GridBagConstraints();

			// Key
			this.keyFileLabel.setText(Messages.getMessage("PdfSigner.KeyFile")); // @TODO Internalise signing messages Messages.getMessage()
			this.keyFileLabel.setFont(new java.awt.Font("Dialog", Font.BOLD, 14));
			c.anchor = GridBagConstraints.FIRST_LINE_START; // Has no effect
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = c.gridy = 0;
			c.insets = new Insets(0, 10, 10, 0);
			inputPanel.add(this.keyFileLabel, c);

			c = new GridBagConstraints();
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 0;
			c.gridy = ++this.y;
			c.gridwidth = 3;
			this.currentKeyFilePath.setPreferredSize(new Dimension(250, 20));
			c.insets = new Insets(10, 10, 10, 10);
			inputPanel.add(this.currentKeyFilePath, c);
			this.browseKeyButton.setText(Messages.getMessage("PdfViewerOption.Browse")); // Messages.getMessage("PdfViewerOption.Browse"));
			this.browseKeyButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					JFileChooser chooser = new JFileChooser(SignWizardModel.this.rootDir);
					String[] pfx = new String[] { "pfx" };
					chooser.addChoosableFileFilter(new FileFilterer(pfx, "Key (pfx)"));
					int state = chooser.showOpenDialog(null);

					File file = chooser.getSelectedFile();

					if (file != null && state == JFileChooser.APPROVE_OPTION) {
						PFXPanel.this.currentKeyFilePath.setText(file.getAbsolutePath());
						PFXPanel.this.keyNext = true;
					}
				}
			});
			c = new GridBagConstraints();
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 2;
			c.gridy = 0;
			c.insets = new Insets(0, 25, 0, 10);
			inputPanel.add(this.browseKeyButton, c);

			// c = new GridBagConstraints();
			// c.gridx = 0;
			// c.gridy = ++y;
			// c.gridwidth = 3;
			// c.fill = GridBagConstraints.HORIZONTAL;
			// inputPanel.add(new JSeparator(SwingConstants.HORIZONTAL), c);

			// Key password
			this.passwordLabel.setText(Messages.getMessage("PdfSigner.Password")); // Messages.getMessage("PdfViewerPassword.message"));
			this.passwordLabel.setFont(new java.awt.Font("Dialog", Font.BOLD, 14));
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 0;
			c.gridy = ++this.y;
			c.insets = new Insets(20, 10, 10, 10);
			inputPanel.add(this.passwordLabel, c);

			this.passwordField.addKeyListener(new KeyListener() {
				@Override
				public void keyReleased(KeyEvent e) {

				}

				@Override
				public void keyPressed(KeyEvent e) {
					PFXPanel.this.passNext = true;
				}

				@Override
				public void keyTyped(KeyEvent e) {

				}
			});

			c = new GridBagConstraints();
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 1;
			c.gridy = this.y;
			c.gridwidth = 1;
			c.insets = new Insets(20, 10, 0, 10);
			this.passwordField.setPreferredSize(new Dimension(100, 20));
			inputPanel.add(this.passwordField, c);

			c = new GridBagConstraints();
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 2;
			c.gridy = this.y;
			c.insets = new Insets(20, 0, 0, 0);
			this.visiblePassCheck.setToolTipText(Messages.getMessage("PdfSigner.ShowPassword"));
			this.visiblePassCheck.addActionListener(new ActionListener() {
				private char defaultChar;

				@Override
				public void actionPerformed(ActionEvent e) {
					if (PFXPanel.this.visiblePassCheck.isSelected()) {
						this.defaultChar = PFXPanel.this.passwordField.getEchoChar();
						PFXPanel.this.passwordField.setEchoChar((char) 0);
					}
					else {
						PFXPanel.this.passwordField.setEchoChar(this.defaultChar);
					}
				}
			});
			inputPanel.add(this.visiblePassCheck, c);

			add(inputPanel, BorderLayout.CENTER);

			add(new ProgressPanel(2), BorderLayout.SOUTH);
		}

		public void registerChange(ChangeListener e) {
			this.browseKeyButton.addChangeListener(e);
		}

		public void registerListener(KeyListener e) {
			this.passwordField.addKeyListener(e);
		}

		public boolean canAdvance() {
			return this.passNext && this.keyNext;
		}

		public void collectData() {
			SignWizardModel.this.signData.setKeyFilePassword(this.passwordField.getPassword());
			SignWizardModel.this.signData.setKeyFilePath(this.currentKeyFilePath.getText());
		}
	}

	private class ModeSelect extends JPanel {

		private static final long serialVersionUID = 6239826095260699312L;
		private String selfString = Messages.getMessage("PdfSigner.HaveKeystore");
		private String otherString = Messages.getMessage("PdfSigner.HavePfx");
		private int y = 0;

		private JRadioButton selfButton = new JRadioButton(this.selfString);
		private String[] certifyOptions = { Messages.getMessage("PdfSigner.NotCertified"), Messages.getMessage("PdfSigner.NoChangesAllowed"),
				Messages.getMessage("PdfSigner.FormFilling"), Messages.getMessage("PdfSigner.FormFillingAndAnnotations") };
		private JComboBox certifyCombo = new JComboBox(this.certifyOptions);
		private int certifyMode = ItextFunctions.NOT_CERTIFIED;

		public ModeSelect() {
			if (!SignWizardModel.this.signData.isAppendMode()) {
				this.certifyCombo = new JComboBox(this.certifyOptions);
			}
			else {
				String[] s = { "Not Allowed..." };
				this.certifyCombo = new JComboBox(s);
			}
			setLayout(new BorderLayout());
			add(new TitlePanel(Messages.getMessage("PdfSigner.SelectSigningMode")), BorderLayout.NORTH);

			JPanel optionPanel = new JPanel();
			optionPanel.setLayout(new GridBagLayout());
			// buttons.setAlignmentX(Component.CENTER_ALIGNMENT);
			GridBagConstraints c = new GridBagConstraints();

			this.selfButton.setActionCommand(this.selfString);
			// selfButton.setAlignmentX(Component.LEFT_ALIGNMENT);
			c.gridx = 0;
			c.gridy = this.y;
			c.anchor = GridBagConstraints.FIRST_LINE_START; // Has no effect
			c.fill = GridBagConstraints.HORIZONTAL;
			c.insets = new Insets(10, 0, 20, 0);
			this.selfButton.setFont(new Font("Dialog", Font.BOLD, 12));
			optionPanel.add(this.selfButton, c);

			JRadioButton otherButton = new JRadioButton(this.otherString);
			otherButton.setActionCommand(this.otherString);
			// otherButton.setAlignmentX(Component.LEFT_ALIGNMENT);
			otherButton.setSelected(true);
			SignWizardModel.this.signData.setSignMode(false);
			c = new GridBagConstraints();
			c.gridx = 0;
			c.gridy = ++this.y;
			c.fill = GridBagConstraints.HORIZONTAL;
			otherButton.setFont(new Font("Dialog", Font.BOLD, 12));
			optionPanel.add(otherButton, c);

			c = new GridBagConstraints();
			c.gridx = 0;
			c.gridy = ++this.y;
			c.fill = GridBagConstraints.HORIZONTAL;
			c.insets = new Insets(30, 0, 30, 0);
			optionPanel.add(new JSeparator(SwingConstants.HORIZONTAL), c);

			JLabel certifyLabel = new JLabel(Messages.getMessage("PdfSigner.CertificationAuthor"));
			certifyLabel.setFont(new Font("Dialog", Font.BOLD, 12));
			c = new GridBagConstraints();
			c.gridx = 0;
			c.gridy = ++this.y;
			c.fill = GridBagConstraints.CENTER;
			optionPanel.add(certifyLabel, c);

			c = new GridBagConstraints();
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 0;
			c.gridy = ++this.y;
			c.insets = new Insets(10, 0, 0, 0);
			c.anchor = GridBagConstraints.PAGE_END;
			this.certifyCombo.setEnabled(!isPdfSigned());
			this.certifyCombo.setSelectedIndex(0);
			this.certifyCombo.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					String mode = (String) ModeSelect.this.certifyCombo.getSelectedItem();
					if (mode.equals(Messages.getMessage("PdfSigner.NotCertified"))) {
						ModeSelect.this.certifyMode = ItextFunctions.NOT_CERTIFIED;
					}
					else
						if (mode.equals(Messages.getMessage("PdfSigner.NoChangesAllowed"))) {
							ModeSelect.this.certifyMode = ItextFunctions.CERTIFIED_NO_CHANGES_ALLOWED;
						}
						else
							if (mode.equals(Messages.getMessage("PdfSigner.FormFilling"))) {
								ModeSelect.this.certifyMode = ItextFunctions.CERTIFIED_FORM_FILLING;
							}
							else
								if (mode.equals(Messages.getMessage("PdfSigner.FormFillingAndAnnotations"))) {
									ModeSelect.this.certifyMode = ItextFunctions.CERTIFIED_FORM_FILLING_AND_ANNOTATIONS;
								}
								else {
									throw new NullPointerException("The certifyCombo box is sending a string that is not recognised.");
								}
				}
			});
			optionPanel.add(this.certifyCombo, c);

			if (isPdfSigned()) {
				this.certifyCombo.setToolTipText(Messages.getMessage("PdfSigner.NotPermittedOnSigned"));
			}

			add(optionPanel, BorderLayout.CENTER);

			ButtonGroup group = new ButtonGroup();
			group.add(this.selfButton);
			group.add(otherButton);

			add(new ProgressPanel(1), BorderLayout.SOUTH);
		}

		public void collectData() {
			SignWizardModel.this.signData.setSignMode(this.selfButton.isSelected());
			SignWizardModel.this.signData.setCertifyMode(this.certifyMode);
		}
	}

	private class KeystorePanel extends JPanel {

		private static final long serialVersionUID = -1708113426345065086L;
		private JLabel keyStoreLabel = new JLabel();
		private JLabel currentKeyStorePath = new JLabel(NO_FILE_SELECTED);
		private JButton browseKeyStoreButton = new JButton();

		private JLabel passwordKeyStoreLabel = new JLabel();
		private JPasswordField passwordKeyStoreField = new JPasswordField();
		private JCheckBox visiblePassKeyCheck = new JCheckBox();

		private JLabel aliasNameLabel = new JLabel();
		private JTextField aliasNameField = new JTextField();
		private JLabel aliasPasswordLabel = new JLabel();
		private JPasswordField aliasPasswordField = new JPasswordField();
		private JCheckBox visiblePassAliasCheck = new JCheckBox();

		private volatile boolean storeAdvance, storePassAdvance, aliasAdvance, aliasPassAdvance = false;

		public KeystorePanel() {
			try {
				init();
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}

		private void init() {
			setLayout(new BorderLayout());
			add(new TitlePanel(Messages.getMessage("PdfSigner.KeyStoreMode")), BorderLayout.NORTH);

			JPanel inputPanel = new JPanel(new GridBagLayout());
			inputPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
			GridBagConstraints c = new GridBagConstraints();

			// Keystore file
			this.keyStoreLabel.setText(Messages.getMessage("PdfSigner.SelectKeyStore"));
			this.keyStoreLabel.setFont(new java.awt.Font("Dialog", Font.BOLD, 14));
			c.anchor = GridBagConstraints.FIRST_LINE_START;
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = c.gridy = 0;
			c.insets = new Insets(0, 10, 10, 0);
			inputPanel.add(this.keyStoreLabel, c);

			c = new GridBagConstraints();
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 0;
			c.gridy = 1;
			c.gridwidth = 3;
			c.insets = new Insets(0, 20, 0, 10);
			this.currentKeyStorePath.setPreferredSize(new Dimension(250, 20));
			inputPanel.add(this.currentKeyStorePath, c);

			this.browseKeyStoreButton.setText(Messages.getMessage("PdfViewerOption.Browse"));
			this.browseKeyStoreButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					JFileChooser chooser = new JFileChooser(SignWizardModel.this.rootDir);
					chooser.setFileHidingEnabled(false);
					// String[] keystore = new String[] { "keystore" };
					// chooser.addChoosableFileFilter(new FileFilterer(keystore, "*.*"));
					int state = chooser.showOpenDialog(null);

					File file = chooser.getSelectedFile();

					if (file != null && state == JFileChooser.APPROVE_OPTION) {
						KeystorePanel.this.currentKeyStorePath.setText(file.getAbsolutePath());
						KeystorePanel.this.storeAdvance = true;
					}
				}
			});
			c = new GridBagConstraints();
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 1;
			c.gridy = 0;
			c.insets = new Insets(0, 30, 0, 0);
			inputPanel.add(this.browseKeyStoreButton, c);

			// KeyStore password
			this.passwordKeyStoreLabel.setText(Messages.getMessage("PdfSigner.Password")); // Messages.getMessage("PdfViewerPassword.message"));
			this.passwordKeyStoreLabel.setFont(new java.awt.Font("Dialog", Font.BOLD, 14));
			c = new GridBagConstraints();
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 0;
			c.gridy = 2;
			c.insets = new Insets(30, 10, 0, 10);
			inputPanel.add(this.passwordKeyStoreLabel, c);

			this.passwordKeyStoreField.addKeyListener(new KeyListener() {
				@Override
				public void keyReleased(KeyEvent e) {

				}

				@Override
				public void keyPressed(KeyEvent e) {

				}

				@Override
				public void keyTyped(KeyEvent e) {
					KeystorePanel.this.storePassAdvance = true;
				}
			});

			c = new GridBagConstraints();
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 1;
			c.gridy = 2;
			c.gridwidth = 1;
			c.insets = new Insets(30, 10, 0, 10);
			// passwordKeyStoreField.setPreferredSize(new Dimension(200,20));
			inputPanel.add(this.passwordKeyStoreField, c);

			c = new GridBagConstraints();
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 2;
			c.gridy = 2;
			c.insets = new Insets(30, 0, 0, 0);
			this.visiblePassKeyCheck.setToolTipText(Messages.getMessage("PdfSigner.ShowPassword"));
			this.visiblePassKeyCheck.addActionListener(new ActionListener() {
				private char defaultChar;

				@Override
				public void actionPerformed(ActionEvent e) {
					if (KeystorePanel.this.visiblePassKeyCheck.isSelected()) {
						this.defaultChar = KeystorePanel.this.passwordKeyStoreField.getEchoChar();
						KeystorePanel.this.passwordKeyStoreField.setEchoChar((char) 0);
					}
					else {
						KeystorePanel.this.passwordKeyStoreField.setEchoChar(this.defaultChar);
					}
				}
			});
			inputPanel.add(this.visiblePassKeyCheck, c);

			c = new GridBagConstraints();
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 0;
			c.gridy = 4;
			c.gridwidth = 4;
			c.insets = new Insets(10, 0, 10, 0);
			inputPanel.add(new JSeparator(SwingConstants.HORIZONTAL), c);

			// //Alias
			this.aliasNameLabel.setText(Messages.getMessage("PdfSigner.AliasName"));
			this.aliasNameLabel.setFont(new java.awt.Font("Dialog", Font.BOLD, 14));
			c = new GridBagConstraints();
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 0;
			c.gridy = 5;
			c.insets = new Insets(0, 10, 10, 0);
			inputPanel.add(this.aliasNameLabel, c);
			this.aliasNameField.addKeyListener(new KeyListener() {
				@Override
				public void keyReleased(KeyEvent e) {

				}

				@Override
				public void keyPressed(KeyEvent e) {

				}

				@Override
				public void keyTyped(KeyEvent e) {
					KeystorePanel.this.aliasAdvance = true;
				}
			});
			c = new GridBagConstraints();
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 1;
			c.gridy = 5;
			c.gridwidth = 2;
			c.insets = new Insets(0, 10, 0, 10);
			this.aliasNameField.setPreferredSize(new Dimension(150, 20));
			inputPanel.add(this.aliasNameField, c);

			this.aliasPasswordLabel.setText(Messages.getMessage("PdfSigner.AliasPassword"));
			this.aliasPasswordLabel.setFont(new java.awt.Font("Dialog", Font.BOLD, 14));
			c = new GridBagConstraints();
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 0;
			c.gridy = 7;
			c.insets = new Insets(10, 10, 0, 10);
			inputPanel.add(this.aliasPasswordLabel, c);
			this.aliasPasswordField.addKeyListener(new KeyListener() {
				@Override
				public void keyReleased(KeyEvent e) {

				}

				@Override
				public void keyPressed(KeyEvent e) {

				}

				@Override
				public void keyTyped(KeyEvent e) {
					KeystorePanel.this.aliasPassAdvance = true;
				}
			});
			c = new GridBagConstraints();
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 1;
			c.gridy = 7;
			// c.gridwidth = 2;
			c.insets = new Insets(0, 10, 0, 10);
			c.anchor = GridBagConstraints.PAGE_END;
			this.aliasPasswordField.setPreferredSize(new Dimension(100, 20));
			inputPanel.add(this.aliasPasswordField, c);

			c = new GridBagConstraints();
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 2;
			c.gridy = 7;
			c.insets = new Insets(10, 0, 0, 0);
			this.visiblePassAliasCheck.setToolTipText(Messages.getMessage("PdfSigner.ShowPassword"));
			this.visiblePassAliasCheck.addActionListener(new ActionListener() {
				private char defaultChar;

				@Override
				public void actionPerformed(ActionEvent e) {
					if (KeystorePanel.this.visiblePassAliasCheck.isSelected()) {
						this.defaultChar = KeystorePanel.this.aliasPasswordField.getEchoChar();
						KeystorePanel.this.aliasPasswordField.setEchoChar((char) 0);
					}
					else {
						KeystorePanel.this.aliasPasswordField.setEchoChar(this.defaultChar);
					}
				}
			});
			inputPanel.add(this.visiblePassAliasCheck, c);

			add(inputPanel, BorderLayout.CENTER);

			add(new ProgressPanel(2), BorderLayout.SOUTH);
		}

		public void registerChange(ChangeListener e) {
			this.browseKeyStoreButton.addChangeListener(e);
		}

		public void registerNextKeyListeners(KeyListener e) {
			this.passwordKeyStoreField.addKeyListener(e);
			this.aliasNameField.addKeyListener(e);
			this.aliasPasswordField.addKeyListener(e);
		}

		public boolean canAdvance() {
			return this.storeAdvance && this.storePassAdvance && this.aliasAdvance && this.aliasPassAdvance;
		}

		public void collectData() {
			SignWizardModel.this.signData.setKeyStorePath(this.currentKeyStorePath.getText());
			SignWizardModel.this.signData.setKeystorePassword(this.passwordKeyStoreField.getPassword());
			SignWizardModel.this.signData.setAlias(this.aliasNameField.getText());
			SignWizardModel.this.signData.setAliasPassword(this.aliasPasswordField.getPassword());
		}
	}

	private class CommonPanel extends JPanel {

		private static final long serialVersionUID = 3825885785941603037L;
		private JLabel reasonLabel = new JLabel();
		private JTextField signerReasonArea = new JTextField();

		private JLabel locationLabel = new JLabel();
		private JTextField signerLocationField = new JTextField();

		private JLabel outputFileLabel = new JLabel();
		private JLabel currentOutputFilePath = new JLabel();
		private JButton browseOutputButton = new JButton();

		private volatile boolean canAdvance = false;

		public CommonPanel() {
			try {
				init();
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}

		private void init() {
			setLayout(new BorderLayout());
			add(new TitlePanel(Messages.getMessage("PdfSigner.ReasonAndLocation")), BorderLayout.NORTH);

			JPanel inputPanel = new JPanel(new GridBagLayout());
			inputPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
			GridBagConstraints c = new GridBagConstraints();

			// Reason
			this.reasonLabel.setText(Messages.getMessage("PdfSigner.Reason") + ':');
			this.reasonLabel.setFont(new java.awt.Font("Dialog", Font.BOLD, 14));
			c.anchor = GridBagConstraints.FIRST_LINE_START; // Has no effect
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = c.gridy = 0;
			c.insets = new Insets(10, 0, 0, 0);
			inputPanel.add(this.reasonLabel, c);

			c = new GridBagConstraints();
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 0;
			c.gridy = 1;
			c.gridwidth = 3;
			c.insets = new Insets(10, 0, 10, 0);
			this.signerReasonArea.setPreferredSize(new Dimension(200, 20));
			inputPanel.add(this.signerReasonArea, c);

			// Location
			this.locationLabel.setText(Messages.getMessage("PdfSigner.Location") + ':');
			this.locationLabel.setFont(new java.awt.Font("Dialog", Font.BOLD, 14));
			c = new GridBagConstraints();
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 0;
			c.gridy = 2;
			inputPanel.add(this.locationLabel, c);

			c = new GridBagConstraints();
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 0;
			c.gridy = 3;
			c.insets = new Insets(10, 0, 0, 0);
			c.gridwidth = 3;
			this.signerLocationField.setPreferredSize(new Dimension(200, 20));
			inputPanel.add(this.signerLocationField, c);

			c = new GridBagConstraints();
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 0;
			c.gridy = 4;
			c.gridwidth = 3;
			c.insets = new Insets(10, 0, 0, 0);
			inputPanel.add(new JSeparator(SwingConstants.HORIZONTAL), c);

			// OutputFile
			this.outputFileLabel.setText(Messages.getMessage("PdfSigner.OutputFile"));
			this.outputFileLabel.setFont(new java.awt.Font("Dialog", Font.BOLD, 14));
			c = new GridBagConstraints();
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 0;
			c.gridy = 5;
			c.insets = new Insets(5, 10, 0, 0);
			inputPanel.add(this.outputFileLabel, c);

			this.currentOutputFilePath.setText(NO_FILE_SELECTED);
			c = new GridBagConstraints();
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 0;
			c.gridy = 6;
			c.insets = new Insets(10, 0, 0, 0);
			c.gridwidth = 3;
			this.currentOutputFilePath.setPreferredSize(new Dimension(100, 20));
			inputPanel.add(this.currentOutputFilePath, c);

			this.browseOutputButton.setText(Messages.getMessage("PdfViewerOption.Browse"));
			this.browseOutputButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					JFileChooser chooser = new JFileChooser(SignWizardModel.this.rootDir);
					int state = chooser.showSaveDialog(null);

					File file = chooser.getSelectedFile();

					if (file != null && state == JFileChooser.APPROVE_OPTION) {
						if (file.exists()) {
							JOptionPane.showMessageDialog(null, Messages.getMessage("PdfSigner.PleaseChooseAnotherFile"),
									Messages.getMessage("PdfViewerGeneralError.message"), JOptionPane.ERROR_MESSAGE);
							CommonPanel.this.canAdvance = false;
							CommonPanel.this.currentOutputFilePath.setText(NO_FILE_SELECTED);
							SignWizardModel.this.signData.setOutputFilePath(null);
						}
						else
							if (file.isDirectory()) {
								JOptionPane.showMessageDialog(null, Messages.getMessage("PdfSigner.NoFileSelected"),
										Messages.getMessage("PdfViewerGeneralError.message"), JOptionPane.ERROR_MESSAGE);
								CommonPanel.this.canAdvance = false;
								CommonPanel.this.currentOutputFilePath.setText(NO_FILE_SELECTED);
							}
							else {
								CommonPanel.this.currentOutputFilePath.setText(file.getAbsolutePath());
								CommonPanel.this.canAdvance = true;
							}
					}
				}
			});
			c = new GridBagConstraints();
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 2;
			c.gridy = 5;
			c.insets = new Insets(5, 25, 0, 25);
			c.anchor = GridBagConstraints.LAST_LINE_END;
			inputPanel.add(this.browseOutputButton, c);

			add(inputPanel, BorderLayout.CENTER);

			add(new ProgressPanel(5), BorderLayout.SOUTH);
		}

		public boolean canFinish() {
			return this.canAdvance;
		}

		public void registerChange(ChangeListener e) {
			this.browseOutputButton.addChangeListener(e);
		}

		public void collectData() {
			SignWizardModel.this.signData.setReason(this.signerReasonArea.getText());
			SignWizardModel.this.signData.setLocation(this.signerLocationField.getText());
			SignWizardModel.this.signData.setOutputFilePath(this.currentOutputFilePath.getText());
		}
	}

	private class EncryptionPanel extends JPanel {

		private static final long serialVersionUID = -6371040190955762586L;
		private JCheckBox encryptionCheck = new JCheckBox("Encrypt");
		private JCheckBox allowPrinting = new JCheckBox("Allow Printing");
		private JCheckBox allowModifyContent = new JCheckBox("Allow Content Modification");
		private JCheckBox allowCopy = new JCheckBox("Allow Copy");
		private JCheckBox allowModifyAnnotation = new JCheckBox("Allow Annotation Modification");
		private JCheckBox allowFillIn = new JCheckBox("Allow Fill In");
		private JCheckBox allowScreenReader = new JCheckBox("Allow Screen Reader");
		private JCheckBox allowAssembly = new JCheckBox("Allow Assembly");
		private JCheckBox allowDegradedPrinting = new JCheckBox("Allow Degraded Printing");
		private JPasswordField userPassword = new JPasswordField();
		private JPasswordField ownerPassword = new JPasswordField();
		private JCheckBox flatten = new JCheckBox("Flatten PDF");

		private JCheckBox visiblePassUserCheck = new JCheckBox();
		private JCheckBox visiblePassOwnerCheck = new JCheckBox();
		private boolean ownerAdvance = false;
		private volatile boolean canAdvance = true;

		public EncryptionPanel() {
			int y = 0;
			setLayout(new BorderLayout());
			add(new TitlePanel(Messages.getMessage("PdfSigner.EncryptionOptions")), BorderLayout.NORTH);

			JPanel optionPanel = new JPanel();
			optionPanel.setLayout(new GridBagLayout());

			GridBagConstraints c = new GridBagConstraints();
			c.gridx = 0;
			c.gridy = y;
			c.fill = GridBagConstraints.HORIZONTAL;
			c.anchor = GridBagConstraints.PAGE_START;
			// encryptionCheck.setFont(new Font("Dialog", Font.BOLD, 12));
			this.encryptionCheck.setEnabled(!isPdfSigned());
			this.encryptionCheck.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					EncryptionPanel.this.canAdvance = !EncryptionPanel.this.encryptionCheck.isSelected() || EncryptionPanel.this.ownerAdvance;
				}
			});

			optionPanel.add(this.encryptionCheck, c);
			this.encryptionCheck.setSelected(false);

			c = new GridBagConstraints();
			c.gridx = 2;
			c.gridy = y;
			c.fill = GridBagConstraints.HORIZONTAL;
			c.anchor = GridBagConstraints.FIRST_LINE_END;
			this.flatten.setEnabled(!isPdfSigned());
			optionPanel.add(this.flatten, c);

			c = new GridBagConstraints();
			c.gridx = 0;
			c.gridy = ++y;
			c.gridwidth = 3;
			c.fill = GridBagConstraints.HORIZONTAL;
			optionPanel.add(new JSeparator(SwingConstants.HORIZONTAL), c);

			// Encryption Options.
			this.allowPrinting.setEnabled(false);
			c = new GridBagConstraints();
			c.gridx = 0;
			c.gridy = ++y;
			c.fill = GridBagConstraints.HORIZONTAL;
			optionPanel.add(this.allowPrinting, c);
			this.allowModifyContent.setEnabled(false);
			c = new GridBagConstraints();
			c.gridx = 1;
			c.gridy = y;
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridwidth = 2;
			optionPanel.add(this.allowModifyContent, c);

			this.allowCopy.setEnabled(false);
			c = new GridBagConstraints();
			c.gridx = 0;
			c.gridy = ++y;
			c.fill = GridBagConstraints.HORIZONTAL;
			optionPanel.add(this.allowCopy, c);
			this.allowModifyAnnotation.setEnabled(false);
			c = new GridBagConstraints();
			c.gridx = 1;
			c.gridy = y;
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridwidth = 2;
			optionPanel.add(this.allowModifyAnnotation, c);

			this.allowFillIn.setEnabled(false);
			c = new GridBagConstraints();
			c.gridx = 0;
			c.gridy = ++y;
			c.fill = GridBagConstraints.HORIZONTAL;
			optionPanel.add(this.allowFillIn, c);
			this.allowScreenReader.setEnabled(false);
			c = new GridBagConstraints();
			c.gridx = 1;
			c.gridy = y;
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridwidth = 2;
			optionPanel.add(this.allowScreenReader, c);

			this.allowAssembly.setEnabled(false);
			c = new GridBagConstraints();
			c.gridx = 0;
			c.gridy = ++y;
			c.fill = GridBagConstraints.HORIZONTAL;
			optionPanel.add(this.allowAssembly, c);
			this.allowDegradedPrinting.setEnabled(false);
			c = new GridBagConstraints();
			c.gridx = 1;
			c.gridy = y;
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridwidth = 2;
			optionPanel.add(this.allowDegradedPrinting, c);

			c = new GridBagConstraints();
			c.gridx = 0;
			c.gridy = ++y;
			c.gridwidth = 3;
			c.fill = GridBagConstraints.HORIZONTAL;
			optionPanel.add(new JSeparator(SwingConstants.HORIZONTAL), c);

			c = new GridBagConstraints();
			c.gridx = 0;
			c.gridy = ++y;
			c.fill = GridBagConstraints.HORIZONTAL;
			c.insets = new Insets(5, 0, 0, 0);
			optionPanel.add(new JLabel(Messages.getMessage("PdfSigner.UserPassword")), c);
			c = new GridBagConstraints();
			c.gridx = 1;
			c.gridy = y;
			c.fill = GridBagConstraints.HORIZONTAL;
			c.insets = new Insets(5, 0, 0, 0);
			this.userPassword.setEnabled(false);
			this.userPassword.setPreferredSize(new Dimension(100, 20));
			this.userPassword.addKeyListener(new KeyListener() {
				@Override
				public void keyReleased(KeyEvent e) {

				}

				@Override
				public void keyPressed(KeyEvent e) {

				}

				@Override
				public void keyTyped(KeyEvent e) {
					EncryptionPanel.this.ownerAdvance = true;
					EncryptionPanel.this.canAdvance = true;
				}
			});
			optionPanel.add(this.userPassword, c);

			c = new GridBagConstraints();
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 2;
			c.gridy = y;
			c.insets = new Insets(0, 0, 0, 0);
			this.visiblePassUserCheck.setToolTipText(Messages.getMessage("PdfSigner.ShowPassword"));
			this.visiblePassUserCheck.addActionListener(new ActionListener() {
				private char defaultChar;

				@Override
				public void actionPerformed(ActionEvent e) {
					if (EncryptionPanel.this.visiblePassUserCheck.isSelected()) {
						this.defaultChar = EncryptionPanel.this.userPassword.getEchoChar();
						EncryptionPanel.this.userPassword.setEchoChar((char) 0);
					}
					else {
						EncryptionPanel.this.userPassword.setEchoChar(this.defaultChar);
					}
				}
			});
			this.visiblePassUserCheck.setEnabled(false);
			optionPanel.add(this.visiblePassUserCheck, c);

			c = new GridBagConstraints();
			c.gridx = 0;
			c.gridy = ++y;
			c.fill = GridBagConstraints.HORIZONTAL;
			c.insets = new Insets(5, 0, 0, 0);
			optionPanel.add(new JLabel(Messages.getMessage("PdfSigner.OwnerPassword")), c);
			c = new GridBagConstraints();
			c.gridx = 1;
			c.gridy = y;
			c.fill = GridBagConstraints.HORIZONTAL;
			c.insets = new Insets(5, 0, 0, 0);
			this.ownerPassword.setEnabled(false);
			this.ownerPassword.setPreferredSize(new Dimension(100, 20));
			optionPanel.add(this.ownerPassword, c);

			c = new GridBagConstraints();
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 2;
			c.gridy = y;
			c.insets = new Insets(0, 0, 0, 0);
			this.visiblePassOwnerCheck.setToolTipText(Messages.getMessage("PdfSigner.ShowPassword"));
			this.visiblePassOwnerCheck.addActionListener(new ActionListener() {
				private char defaultChar;

				@Override
				public void actionPerformed(ActionEvent e) {
					if (EncryptionPanel.this.visiblePassOwnerCheck.isSelected()) {
						this.defaultChar = EncryptionPanel.this.ownerPassword.getEchoChar();
						EncryptionPanel.this.ownerPassword.setEchoChar((char) 0);
					}
					else {
						EncryptionPanel.this.ownerPassword.setEchoChar(this.defaultChar);
					}
				}
			});
			this.visiblePassOwnerCheck.setEnabled(false);
			optionPanel.add(this.visiblePassOwnerCheck, c);

			if (isPdfSigned()) {
				c = new GridBagConstraints();
				c.fill = GridBagConstraints.HORIZONTAL;
				c.gridx = 0;
				c.gridy = ++y;
				c.gridwidth = 3;
				c.insets = new Insets(25, 0, 0, 0);
				JLabel notAvailable = new JLabel(Messages.getMessage("PdfSigner.DisabledSigned"), SwingConstants.CENTER);
				notAvailable.setForeground(Color.red);
				optionPanel.add(notAvailable, c);
			}

			this.encryptionCheck.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent e) {
					boolean enable = e.getStateChange() == ItemEvent.SELECTED;
					EncryptionPanel.this.allowPrinting.setEnabled(enable);
					EncryptionPanel.this.allowModifyContent.setEnabled(enable);
					EncryptionPanel.this.allowCopy.setEnabled(enable);
					EncryptionPanel.this.allowModifyAnnotation.setEnabled(enable);
					EncryptionPanel.this.allowFillIn.setEnabled(enable);
					EncryptionPanel.this.allowScreenReader.setEnabled(enable);
					EncryptionPanel.this.allowAssembly.setEnabled(enable);
					EncryptionPanel.this.allowDegradedPrinting.setEnabled(enable);
					EncryptionPanel.this.userPassword.setEnabled(enable);
					EncryptionPanel.this.ownerPassword.setEnabled(enable);
					EncryptionPanel.this.visiblePassUserCheck.setEnabled(enable);
					EncryptionPanel.this.visiblePassOwnerCheck.setEnabled(enable);
				}
			});

			add(optionPanel, BorderLayout.CENTER);
			add(new ProgressPanel(4), BorderLayout.SOUTH);
		}

		public void registerChange(ChangeListener wizard) {
			this.encryptionCheck.addChangeListener(wizard);
		}

		public void registerNextKeyListeners(KeyListener wizard) {
			this.userPassword.addKeyListener(wizard);
		}

		public boolean canAdvance() {
			return this.canAdvance;
		}

		public void collectData() {
			SignWizardModel.this.signData.setFlatten(this.flatten.isSelected());
			SignWizardModel.this.signData.setEncrypt(this.encryptionCheck.isSelected());
			if (this.encryptionCheck.isSelected()) {
				SignWizardModel.this.signData.setEncryptUserPass(this.userPassword.getPassword());
				SignWizardModel.this.signData.setEncryptOwnerPass(this.ownerPassword.getPassword());

				int result = 0;

				if (this.allowPrinting.isSelected()) result |= ItextFunctions.ALLOW_PRINTING;
				if (this.allowModifyContent.isSelected()) result |= ItextFunctions.ALLOW_MODIFY_CONTENTS;
				if (this.allowCopy.isSelected()) result |= ItextFunctions.ALLOW_COPY;
				if (this.allowModifyAnnotation.isSelected()) result |= ItextFunctions.ALLOW_MODIFY_ANNOTATIONS;
				if (this.allowFillIn.isSelected()) result |= ItextFunctions.ALLOW_FILL_IN;
				if (this.allowScreenReader.isSelected()) result |= ItextFunctions.ALLOW_SCREENREADERS;
				if (this.allowAssembly.isSelected()) result |= ItextFunctions.ALLOW_ASSEMBLY;
				if (this.allowDegradedPrinting.isSelected()) result |= ItextFunctions.ALLOW_DEGRADED_PRINTING;

				SignWizardModel.this.signData.setEncryptPermissions(result);
			}
		}
	}

	private class SignaturePanel extends JPanel {

		private static final long serialVersionUID = 6938983094179969472L;
		private JCheckBox visibleCheck = new JCheckBox(Messages.getMessage("PdfSigner.VisibleSignature"));
		private JComponent sigPreviewComp;
		private JSlider pageSlider;
		private JLabel pageNumberLabel;
		private int currentPage = 1;
		private Point signRectOrigin;
		private Point signRectEnd;
		private int offsetX, offsetY;

		private float scale;
		private int previewWidth, previewHeight;
		private volatile boolean drawRect = false;
		private boolean signAreaUndefined = true;

		private BufferedImage previewImage;

		public SignaturePanel() {

			try {
				this.previewImage = SignWizardModel.this.pdfDecoder.getPageAsImage(this.currentPage);
			}
			catch (Exception e) {
				// tell user and log
				if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
			}

			int y = 0;
			setLayout(new BorderLayout());
			add(new TitlePanel(Messages.getMessage("PdfSigner.VisibleSignature") + ' ' + Messages.getMessage("PdfViewerMenu.options")),
					BorderLayout.NORTH);

			JPanel optionPanel = new JPanel();
			optionPanel.setLayout(new GridBagLayout());

			GridBagConstraints c = new GridBagConstraints();
			c.gridx = 0;
			c.gridy = y;
			c.insets = new Insets(5, 0, 0, 0);
			c.fill = GridBagConstraints.HORIZONTAL;
			c.anchor = GridBagConstraints.PAGE_START;
			this.visibleCheck.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					SignaturePanel.this.sigPreviewComp.repaint();
					if (SignWizardModel.this.pdfDecoder.getPageCount() > 1) SignaturePanel.this.pageSlider
							.setEnabled(SignaturePanel.this.visibleCheck.isSelected());
				}

			});
			optionPanel.add(this.visibleCheck, c);

			c = new GridBagConstraints();
			c.gridx = 0;
			c.gridy = ++y;
			c.fill = GridBagConstraints.HORIZONTAL;
			c.insets = new Insets(10, 0, 10, 0);
			optionPanel.add(new JSeparator(SwingConstants.HORIZONTAL), c);

			c = new GridBagConstraints();
			c.gridx = 0;
			c.gridy = ++y;
			c.fill = GridBagConstraints.HORIZONTAL;
			optionPanel.add(previewPanel(), c);

			add(optionPanel, BorderLayout.CENTER);

			add(new ProgressPanel(3), BorderLayout.SOUTH);

		}

		public void collectData() {
			SignWizardModel.this.signData.setVisibleSignature(this.visibleCheck.isSelected());
			if (this.visibleCheck.isSelected()) {
				int height = this.previewImage.getHeight();
				int x1 = (int) ((this.signRectOrigin.getX() - this.offsetX) / this.scale);
				int y1 = (int) (height - ((this.signRectOrigin.getY() - this.offsetY) / this.scale));
				int x2 = (int) ((this.signRectEnd.getX() - this.offsetX) / this.scale);
				int y2 = (int) (height - ((this.signRectEnd.getY() - this.offsetY) / this.scale));

				PdfPageData pageData = SignWizardModel.this.pdfDecoder.getPdfPageData();
				int cropX = pageData.getCropBoxX(this.currentPage);
				int cropY = pageData.getCropBoxY(this.currentPage);
				x1 += cropX;
				y1 += cropY;
				x2 += cropX;
				y2 += cropY;

				SignWizardModel.this.signData.setRectangle(x1, y1, x2, y2);
				SignWizardModel.this.signData.setSignPage(this.currentPage);
			}
		}

		private JPanel previewPanel() {
			JPanel result = new JPanel(new BorderLayout());

			this.sigPreviewComp = new JComponent() {

				private static final long serialVersionUID = 3489687587790924068L;

				@Override
				public void paintComponent(Graphics g) {
					sigPreview(g);
				}
			};
			this.sigPreviewComp.setPreferredSize(new Dimension(200, 200));
			this.sigPreviewComp.setToolTipText(Messages.getMessage("PdfSigner.ClickAndDrag"));
			this.sigPreviewComp.addMouseListener(new MouseListener() {

				@Override
				public void mouseClicked(MouseEvent e) {}

				@Override
				public void mouseEntered(MouseEvent e) {}

				@Override
				public void mouseExited(MouseEvent e) {}

				@Override
				public void mousePressed(MouseEvent e) {
					if (SignaturePanel.this.visibleCheck.isSelected()) {
						SignaturePanel.this.signRectOrigin.setLocation(e.getX(), e.getY());
						SignaturePanel.this.drawRect = true;

						Thread rect = new Thread(signAreaThread());
						rect.start();
					}
				}

				@Override
				public void mouseReleased(MouseEvent e) {
					if (SignaturePanel.this.visibleCheck.isSelected()) {
						SignaturePanel.this.drawRect = false;
						SignaturePanel.this.sigPreviewComp.repaint();
					}
				}

			});

			result.add(this.sigPreviewComp, BorderLayout.CENTER);

			// Add a slider if there is more than one page
			if (SignWizardModel.this.pdfDecoder.getPageCount() > 1) {
				this.pageNumberLabel = new JLabel(Messages.getMessage("PdfSigner.PageNumber") + ' ' + this.currentPage);
				this.pageNumberLabel.setHorizontalAlignment(SwingConstants.CENTER);
				result.add(this.pageNumberLabel, BorderLayout.NORTH);

				this.pageSlider = new JSlider(SwingConstants.HORIZONTAL, 1, SignWizardModel.this.pdfDecoder.getPageCount(), this.currentPage);
				this.pageSlider.setMajorTickSpacing(SignWizardModel.this.pdfDecoder.getPageCount() - 1);
				this.pageSlider.setPaintLabels(true);

				this.pageSlider.addChangeListener(new ChangeListener() {
					@Override
					public void stateChanged(ChangeEvent e) {
						if (SignaturePanel.this.pageSlider.getValueIsAdjusting()) {
							SignaturePanel.this.currentPage = SignaturePanel.this.pageSlider.getValue();
							try {
								SignaturePanel.this.previewImage = SignWizardModel.this.pdfDecoder.getPageAsImage(SignaturePanel.this.currentPage);
								SignaturePanel.this.sigPreviewComp.repaint();
								SignaturePanel.this.pageNumberLabel.setText(Messages.getMessage("PdfSigner.PageNumber") + ' '
										+ SignaturePanel.this.currentPage);
							}
							catch (Exception ex) {
								// tell user and log
								if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + ex.getMessage());
							}
						}
					}
				});
				result.add(this.pageSlider, BorderLayout.SOUTH);
				this.pageSlider.setEnabled(false);
			}

			return result;
		}

		private void sigPreview(Graphics g) {
			int panelWidth = this.sigPreviewComp.getWidth();
			int panelHeight = this.sigPreviewComp.getHeight();
			this.previewWidth = this.previewImage.getWidth();
			this.previewHeight = this.previewImage.getHeight();

			this.scale = (this.previewWidth > this.previewHeight) ? (float) panelWidth / this.previewWidth : (float) panelHeight / this.previewHeight;

			this.previewWidth *= this.scale;
			this.previewHeight *= this.scale;
			this.offsetX = (panelWidth - this.previewWidth) / 2;
			this.offsetY = (panelHeight - this.previewHeight) / 2;

			g.drawImage(this.previewImage, this.offsetX, this.offsetY, this.previewWidth, this.previewHeight, null);

			if (this.visibleCheck.isSelected()) {
				g.clipRect(this.offsetX, this.offsetY, this.previewWidth, this.previewHeight);
				drawSignBox(g);
			}
		}

		private void drawSignBox(Graphics g) {
			if (this.signAreaUndefined) {
				PdfPageData pageData = SignWizardModel.this.pdfDecoder.getPdfPageData();
				this.signRectOrigin = new Point(this.offsetX, this.offsetY);
				this.signRectEnd = new Point((int) (pageData.getCropBoxWidth(this.currentPage) * this.scale) - 1 + this.offsetX,
						(int) (pageData.getCropBoxHeight(this.currentPage) * this.scale) - 1 + this.offsetY);
				this.signAreaUndefined = false;
			}
			int xO = (int) this.signRectOrigin.getX();
			int yO = (int) this.signRectOrigin.getY();
			int xE = (int) this.signRectEnd.getX();
			int yE = (int) this.signRectEnd.getY();
			if (xO > xE) {
				int temp = xE;
				xE = xO;
				xO = temp;
			}
			if (yO > yE) {
				int temp = yO;
				yO = yE;
				yE = temp;
			}

			g.drawRect(xO, yO, xE - xO, yE - yO);
			g.drawLine(xO, yO, xE, yE);
			g.drawLine(xO, yE, xE, yO);
		}

		private Runnable signAreaThread() {
			return new Runnable() {
				@Override
				public void run() {
					Point origin = SignaturePanel.this.sigPreviewComp.getLocationOnScreen();

					while (SignaturePanel.this.drawRect) {
						try {
							Thread.sleep(100);
						}
						catch (Exception e) {
							// tell user and log
							if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
						}
						double x = MouseInfo.getPointerInfo().getLocation().getX() - origin.getX();
						double y = MouseInfo.getPointerInfo().getLocation().getY() - origin.getY();

						SignaturePanel.this.signRectEnd.setLocation(x, y);
						SignaturePanel.this.sigPreviewComp.repaint();
					}
				}
			};
		}
	}

	private static class ProgressPanel extends JPanel {

		private static final long serialVersionUID = 8032594741795633401L;

		public ProgressPanel(int current) {
			setBorder(new EtchedBorder());
			JLabel progressLabel = new JLabel("Step " + current + " of " + MAXIMUM_PANELS);
			progressLabel.setAlignmentX(RIGHT_ALIGNMENT);
			add(progressLabel);
		}
	}

	private static class TitlePanel extends JPanel {

		private static final long serialVersionUID = 4046138883517734293L;

		public TitlePanel(String title) {
			setBackground(Color.gray);
			setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));

			JLabel textLabel = new JLabel();
			textLabel.setBackground(Color.gray);
			textLabel.setFont(new Font("Dialog", Font.BOLD, 14));
			textLabel.setText(title);
			textLabel.setBorder(new EmptyBorder(new Insets(10, 10, 10, 10)));
			textLabel.setOpaque(true);
			add(textLabel);
		}
	}
}
