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
 * Wizard.java
 * ---------------
 */

package org.jpedal.examples.viewer.gui.popups;

/**
 * Creates a wizard dialog with next, back, finish and cancel buttons.
 * In order to use you must implement a WizardPanelModel which gives the 
 * Wizard the panels it must contain and controls the flow of the panels,
 * such as whether the advance button can be used and what the next panel
 * to be shown is.
 */
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Frame;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Map;
import java.util.Set;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/* Create a Wizard dialog box */
public class Wizard {

	private static final String BACK_TEXT = "< Back";
	private static final String NEXT_TEXT = "Next >";
	private static final String CANCEL_TEXT = "Cancel";
	private static final String FINISH_TEXT = "Finish";

	private JDialog wizardDialog;
	private WizardPanelModel panelManager;

	private JPanel cardPanel;
	private CardLayout cardLayout;

	private JButton backButton;
	private JButton advanceButton;
	private JButton cancelButton;

	private int returnCode = JOptionPane.CANCEL_OPTION;

	/**
	 * Create a Wizard dialog box using the panels given in the given WizardPanelModel.
	 * 
	 * @param owner
	 *            Parent frame
	 * @param panelManager
	 *            Implements the WizardPanelModel interface, therefore containing all the panels and logic.
	 */
	public Wizard(Frame owner, WizardPanelModel panelManager) {
		this.wizardDialog = new JDialog(owner);
		this.panelManager = panelManager;
		initComponents();
	}

	private void initComponents() {

		JPanel buttonPanel = new JPanel();
		Box buttonBox = new Box(BoxLayout.X_AXIS);
		this.wizardDialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		this.cardPanel = new JPanel();
		this.cardPanel.setBorder(new EmptyBorder(new Insets(5, 10, 5, 10)));
		this.cardLayout = new CardLayout();
		this.cardPanel.setLayout(this.cardLayout);

		Map panels = this.panelManager.getJPanels();
		Set keys = panels.keySet();

		for (Object key1 : keys) {
			String key = (String) key1;
			this.cardPanel.add(key, (JPanel) panels.get(key));
		}

		this.backButton = new JButton(BACK_TEXT);
		this.advanceButton = new JButton(NEXT_TEXT);
		this.cancelButton = new JButton(CANCEL_TEXT);

		this.backButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				previousPanel();
			}
		});

		this.advanceButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				nextPanel();
			}
		});

		this.cancelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Wizard.this.returnCode = JOptionPane.CANCEL_OPTION;
				Wizard.this.panelManager.close();
				Wizard.this.wizardDialog.dispose();
			}
		});

		buttonPanel.setLayout(new BorderLayout());
		buttonPanel.add(new JSeparator(), BorderLayout.NORTH);

		buttonBox.setBorder(new EmptyBorder(new Insets(5, 10, 5, 10)));
		buttonBox.add(this.backButton);
		buttonBox.add(Box.createHorizontalStrut(10));
		buttonBox.add(this.advanceButton);
		buttonBox.add(Box.createHorizontalStrut(30));
		buttonBox.add(this.cancelButton);
		buttonPanel.add(buttonBox, java.awt.BorderLayout.EAST);
		this.wizardDialog.getContentPane().add(buttonPanel, java.awt.BorderLayout.SOUTH);
		this.wizardDialog.getContentPane().add(this.cardPanel, java.awt.BorderLayout.CENTER);
		this.cardLayout.show(this.cardPanel, this.panelManager.getStartPanelID());
		setBackButtonEnabled(this.panelManager.hasPrevious());
		setNextButtonEnabled(this.panelManager.canAdvance());

		this.panelManager.registerNextChangeListeners(new buttonNextState());
		this.panelManager.registerNextKeyListeners(new textboxPressState());
	}

	private void setBackButtonEnabled(boolean b) {
		this.backButton.setEnabled(b);
	}

	private void setNextButtonEnabled(boolean b) {
		this.advanceButton.setEnabled(b);
	}

	private void nextPanel() {
		if (this.advanceButton.getText().equals(FINISH_TEXT)) {
			this.panelManager.close();
			this.returnCode = JOptionPane.OK_OPTION;
			this.wizardDialog.dispose();
		}
		else {
			this.cardLayout.show(this.cardPanel, this.panelManager.next());
			setBackButtonEnabled(this.panelManager.hasPrevious());
			setNextButtonEnabled(this.panelManager.canAdvance());
			if (this.panelManager.isFinishPanel()) {
				this.advanceButton.setText(FINISH_TEXT);
			}
		}
	}

	private void previousPanel() {
		if (this.panelManager.isFinishPanel()) {
			this.advanceButton.setText(NEXT_TEXT);
		}
		this.cardLayout.show(this.cardPanel, this.panelManager.previous());
		setBackButtonEnabled(this.panelManager.hasPrevious());
		setNextButtonEnabled(this.panelManager.canAdvance());
	}

	/**
	 * Display a modal wizard dialog in the middle of the screen.
	 * 
	 * @return The return code is either JOptionPane.CANCEL_OPTION or JOPtionPane.OK_OPTION
	 */
	public int showModalDialog() {
		this.wizardDialog.setModal(true);
		this.wizardDialog.pack();
		this.wizardDialog.setLocationRelativeTo(null);
		this.wizardDialog.setVisible(true);

		return this.returnCode;
	}

	private class buttonNextState implements ChangeListener {

		@Override
		public void stateChanged(ChangeEvent e) {
			setNextButtonEnabled(Wizard.this.panelManager.canAdvance());
		}
	}

	private class textboxPressState implements KeyListener {
		@Override
		public void keyReleased(KeyEvent e) {

		}

		@Override
		public void keyPressed(KeyEvent e) {

		}

		@Override
		public void keyTyped(KeyEvent e) {
			setNextButtonEnabled(Wizard.this.panelManager.canAdvance());
		}
	}
}
