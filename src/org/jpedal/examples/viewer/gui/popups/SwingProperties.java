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
 * SwingProperties.java
 * ---------------
 */

package org.jpedal.examples.viewer.gui.popups;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JTree;
import javax.swing.JViewport;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;

import org.jpedal.Display;
import org.jpedal.PdfDecoder;
import org.jpedal.examples.viewer.Viewer;
import org.jpedal.examples.viewer.gui.CheckNode;
import org.jpedal.examples.viewer.gui.SwingGUI;
import org.jpedal.examples.viewer.utils.PropertiesFile;
import org.jpedal.parser.DecoderOptions;
import org.jpedal.utils.Messages;
import org.jpedal.utils.SwingWorker;
import org.w3c.dom.NodeList;

public class SwingProperties extends JPanel {

	private static final long serialVersionUID = 310781187885328136L;

	Map reverseMessage = new HashMap();

	// Array of menu tabs.
	String[] menuTabs = { "ShowMenubar", "ShowButtons", "ShowDisplayoptions", "ShowNavigationbar", "ShowSidetabbar" };

	String propertiesLocation = "";

	PropertiesFile properties = null;

	// Window Components
	JDialog propertiesDialog;

	JButton confirm = new JButton("OK");

	JButton cancel = new JButton("Cancel");

	JTabbedPane tabs = new JTabbedPane();

	// Settings Fields Components

	// DPI viewer value
	JTextField resolution;

	// Search window display style
	JComboBox searchStyle;

	// Show border around page
	JCheckBox border;

	// Show download window
	JCheckBox downloadWindow;

	// Use Hi Res Printing
	JCheckBox HiResPrint;

	// Use Hi Res Printing
	JCheckBox constantTabs;

	// Use enhanced viewer
	JCheckBox enhancedViewer;

	// Use enhanced viewer
	JCheckBox enhancedFacing;

	// Use enhanced viewer
	JCheckBox thumbnailScroll;

	// Use enhanced user interface
	JCheckBox enhancedGUI;

	// Use right click functionality
	JCheckBox rightClick;

	// Allow scrollwheel zooming
	JCheckBox scrollwheelZoom;

	// perform automatic update check
	JCheckBox update = new JCheckBox(Messages.getMessage("PdfPreferences.CheckForUpdate"));

	// max no of multiviewers
	JTextField maxMultiViewers;

	// inset value
	JTextField pageInsets;
	JLabel pageInsetsText;

	// window title
	JTextField windowTitle;
	JLabel windowTitleText;

	// icons Location
	JTextField iconLocation;
	JLabel iconLocationText;

	// Printer blacklist
	JTextField printerBlacklist;
	JLabel printerBlacklistText;

	// Default printer
	JComboBox defaultPrinter;
	JLabel defaultPrinterText;

	// Default pagesize
	JComboBox defaultPagesize;
	JLabel defaultPagesizeText;

	// Default resolution
	JTextField defaultDPI;
	JLabel defaultDPIText;

	JTextField sideTabLength;
	JLabel sideTabLengthText;

	// Use parented hinting functions
	JCheckBox useHinting;

	// Set autoScroll when mouse at the edge of page
	JCheckBox autoScroll;

	// Set whether to prompt user on close
	JCheckBox confirmClose;

	// Set if we should open the file at the last viewed page
	JCheckBox openLastDoc;

	// Set default page layout
	JComboBox pageLayout = new JComboBox(new String[] { "Single Page", "Continuous", "Continuous Facing", "Facing", "PageFlow" });

	// Speech Options
	JComboBox voiceSelect;

	JPanel highlightBoxColor = new JPanel();
	JPanel highlightTextColor = new JPanel();
	JPanel viewBGColor = new JPanel();
	JPanel pdfDecoderBackground = new JPanel();
	// JPanel sideBGColor = new JPanel();
	JPanel foreGroundColor = new JPanel();
	JCheckBox invertHighlight = new JCheckBox("Highlight Inverts Page");
	JCheckBox replaceDocTextCol = new JCheckBox("Replace Document Text Colors");
	JCheckBox replaceDisplayBGCol = new JCheckBox("Replace Display Background Color");

	JCheckBox changeTextAndLineArt = new JCheckBox("Change Color of Text and Line art");
	JCheckBox showMouseSelectionBox = new JCheckBox("Show Mouse Selection Box");
	JTextField highlightComposite = new JTextField(String.valueOf(PdfDecoder.highlightComposite));

	// private SwingGUI swingGUI;

	private Container parent;

	private boolean preferencesSetup = false;

	private JButton clearHistory;

	private JLabel historyClearedLabel;

	// Only allow numerical input to the field
	KeyListener numericalKeyListener = new KeyListener() {

		boolean consume = false;

		@Override
		public void keyPressed(KeyEvent e) {
			this.consume = false;
			if ((e.getKeyChar() < '0' || e.getKeyChar() > '9') && (e.getKeyCode() != 8 || e.getKeyCode() != 127)) this.consume = true;
		}

		@Override
		public void keyReleased(KeyEvent e) {}

		@Override
		public void keyTyped(KeyEvent e) {
			if (this.consume) e.consume();
		}
	};

	/**
	 * showPreferenceWindow()
	 * 
	 * Ensure current values are loaded then display window.
	 * 
	 * @param swingGUI
	 */
	public void showPreferenceWindow(SwingGUI swingGUI) {

		if (this.parent instanceof JFrame) this.propertiesDialog = new JDialog(((JFrame) this.parent));
		else this.propertiesDialog = new JDialog();

		this.propertiesDialog.setModal(true);

		if (!this.preferencesSetup) {
			this.preferencesSetup = true;

			createPreferenceWindow(swingGUI);
		}

		if (this.properties.getValue("readOnly").toLowerCase().equals("true")) {
			JOptionPane.showMessageDialog(this, "You do not have permission alter jPedal properties.\n"
					+ "Access to the properties window has therefore been disabled.", "Can not write to properties file",
					JOptionPane.INFORMATION_MESSAGE);
		}

		if (this.properties.isReadOnly()) {
			JOptionPane
					.showMessageDialog(this, "Current properties file is read only.\n"
							+ "Any alteration can only be saved as another properties file.", "Properties file is read only",
							JOptionPane.INFORMATION_MESSAGE);
			this.confirm.setEnabled(false);
		}
		else {
			this.confirm.setEnabled(true);
		}

		// this.swingGUI = swingGUI;
		this.propertiesDialog.setLocationRelativeTo(this.parent);
		this.propertiesDialog.setVisible(true);
	}

	private void saveGUIPreferences(SwingGUI gui) {
		Component[] components = this.tabs.getComponents();
		for (int i = 0; i != components.length; i++) {
			if (components[i] instanceof JPanel) {
				Component[] panelComponets = ((JPanel) components[i]).getComponents();
				for (int j = 0; j != panelComponets.length; j++) {
					if (panelComponets[j] instanceof JScrollPane) {
						Component[] scrollComponents = ((JScrollPane) panelComponets[j]).getComponents();
						for (int k = 0; k != scrollComponents.length; k++) {
							if (scrollComponents[k] instanceof JViewport) {
								Component[] viewportComponents = ((JViewport) scrollComponents[k]).getComponents();
								for (int l = 0; l != viewportComponents.length; l++) {
									if (viewportComponents[l] instanceof JTree) {
										JTree tree = ((JTree) viewportComponents[l]);
										CheckNode root = (CheckNode) tree.getModel().getRoot();
										if (root.getChildCount() > 0) {
											saveMenuPreferencesChildren(root, gui);
										}
									}
								}
							}

						}
					}
					if (panelComponets[j] instanceof JButton) {
						JButton tempButton = ((JButton) panelComponets[j]);
						String value = ((String) this.reverseMessage.get(tempButton.getText().substring(
								(Messages.getMessage("PdfCustomGui.HideGuiSection") + ' ').length())));
						if (tempButton.getText().startsWith(Messages.getMessage("PdfCustomGui.HideGuiSection") + ' ')) {
							this.properties.setValue(value, "true");
							gui.alterProperty(value, true);
						}
						else {
							this.properties.setValue(value, "false");
							gui.alterProperty(value, false);
						}
					}
				}
			}
		}
	}

	private void saveMenuPreferencesChildren(CheckNode root, SwingGUI gui) {
		for (int i = 0; i != root.getChildCount(); i++) {
			CheckNode node = (CheckNode) root.getChildAt(i);
			String value = ((String) this.reverseMessage.get(node.getText()));
			if (node.isSelected()) {
				this.properties.setValue(value, "true");
				gui.alterProperty(value, true);
			}
			else {
				this.properties.setValue(value, "false");
				gui.alterProperty(value, false);
			}

			if (node.getChildCount() > 0) {
				saveMenuPreferencesChildren(node, gui);
			}
		}
	}

	/**
	 * createPreferanceWindow(final GUI gui) Set up all settings fields then call the required methods to build the window
	 * 
	 * @param gui
	 *            - Used to allow any changed settings to be saved into an external properties file.
	 * 
	 */
	private void createPreferenceWindow(final SwingGUI gui) {

		// Get Properties file containing current preferences
		this.properties = gui.getProperties();
		// Get Properties file location
		this.propertiesLocation = gui.getPropertiesFileLocation();

		// Set window title
		this.propertiesDialog.setTitle(Messages.getMessage("PdfPreferences.windowTitle"));

		this.update.setToolTipText(Messages.getMessage("PdfPreferences.update.toolTip"));
		this.invertHighlight.setText(Messages.getMessage("PdfPreferences.InvertHighlight"));
		this.showMouseSelectionBox.setText(Messages.getMessage("PdfPreferences.ShowSelectionBow"));
		this.invertHighlight.setToolTipText(Messages.getMessage("PdfPreferences.invertHighlight.toolTip"));
		this.showMouseSelectionBox.setToolTipText(Messages.getMessage("PdfPreferences.showMouseSelection.toolTip"));
		this.highlightBoxColor.setToolTipText(Messages.getMessage("PdfPreferences.highlightBox.toolTip"));
		this.highlightTextColor.setToolTipText(Messages.getMessage("PdfPreferences.highlightText.toolTip"));

		// @kieran
		// @removed by Mark as always misused
		// Set up the properties window gui components
		String propValue = this.properties.getValue("resolution");
		if (propValue.length() > 0) this.resolution = new JTextField(propValue);
		else this.resolution = new JTextField(72);
		this.resolution.setToolTipText(Messages.getMessage("PdfPreferences.resolutionInput.toolTip"));

		propValue = this.properties.getValue("maxmultiviewers");
		if (propValue.length() > 0) this.maxMultiViewers = new JTextField(propValue);
		else this.maxMultiViewers = new JTextField(20);
		this.maxMultiViewers.setToolTipText(Messages.getMessage("PdfPreferences.maxMultiViewer.toolTip"));

		this.searchStyle = new JComboBox(new String[] { Messages.getMessage("PageLayoutViewMenu.WindowSearch"),
				Messages.getMessage("PageLayoutViewMenu.TabbedSearch"), Messages.getMessage("PageLayoutViewMenu.MenuSearch") });
		this.searchStyle.setToolTipText(Messages.getMessage("PdfPreferences.searchStyle.toolTip"));

		this.pageLayout = new JComboBox(new String[] { Messages.getMessage("PageLayoutViewMenu.SinglePage"),
				Messages.getMessage("PageLayoutViewMenu.Continuous"), Messages.getMessage("PageLayoutViewMenu.Facing"),
				Messages.getMessage("PageLayoutViewMenu.ContinousFacing"), Messages.getMessage("PageLayoutViewMenu.PageFlow") });
		this.pageLayout.setToolTipText(Messages.getMessage("PdfPreferences.pageLayout.toolTip"));

		this.pageInsetsText = new JLabel(Messages.getMessage("PdfViewerViewMenu.pageInsets"));
		this.pageInsets = new JTextField();
		this.pageInsets.setToolTipText(Messages.getMessage("PdfPreferences.pageInsets.toolTip"));

		this.windowTitleText = new JLabel(Messages.getMessage("PdfCustomGui.windowTitle"));
		this.windowTitle = new JTextField();
		this.windowTitle.setToolTipText(Messages.getMessage("PdfPreferences.windowTitle.toolTip"));

		this.iconLocationText = new JLabel(Messages.getMessage("PdfViewerViewMenu.iconLocation"));
		this.iconLocation = new JTextField();
		this.iconLocation.setToolTipText(Messages.getMessage("PdfPreferences.iconLocation.toolTip"));

		this.defaultDPIText = new JLabel(Messages.getMessage("PdfViewerPrint.defaultDPI"));
		this.defaultDPI = new JTextField();
		this.defaultDPI.setToolTipText(Messages.getMessage("PdfPreferences.defaultDPI.toolTip"));

		this.sideTabLengthText = new JLabel(Messages.getMessage("PdfCustomGui.SideTabLength"));
		this.sideTabLength = new JTextField();
		this.sideTabLength.setToolTipText(Messages.getMessage("PdfPreferences.sideTabLength.toolTip"));

		this.useHinting = new JCheckBox(Messages.getMessage("PdfCustomGui.useHinting"));
		// useHinting.addActionListener(new ActionListener() {
		// public void actionPerformed(ActionEvent e) {
		// if (useHinting.isSelected()) {
		// JOptionPane.showMessageDialog(null,Messages.getMessage("PdfCustomGui.patentedHintingMessage"));
		// }
		// }
		// });
		this.useHinting.setToolTipText(Messages.getMessage("PdfPreferences.useHinting.toolTip"));

		this.autoScroll = new JCheckBox(Messages.getMessage("PdfViewerViewMenuAutoscrollSet.text"));
		this.autoScroll.setToolTipText("Set if autoscroll should be enabled / disabled");

		this.confirmClose = new JCheckBox(Messages.getMessage("PfdViewerViewMenuConfirmClose.text"));
		this.confirmClose.setToolTipText("Set if we should confirm closing the viewer");

		this.openLastDoc = new JCheckBox(Messages.getMessage("PdfViewerViewMenuOpenLastDoc.text"));
		this.openLastDoc.setToolTipText("Set if last document should be opened upon start up");

		this.border = new JCheckBox(Messages.getMessage("PageLayoutViewMenu.Borders_Show"));
		this.border.setToolTipText("Set if we should display a border for the page");

		this.downloadWindow = new JCheckBox(Messages.getMessage("PageLayoutViewMenu.DownloadWindow_Show"));
		this.downloadWindow.setToolTipText("Set if the download window should be displayed");

		this.HiResPrint = new JCheckBox(Messages.getMessage("Printing.HiRes"));
		this.HiResPrint.setToolTipText("Set if hi res printing should be enabled / disabled");

		this.constantTabs = new JCheckBox(Messages.getMessage("PdfCustomGui.consistentTabs"));
		this.constantTabs.setToolTipText("Set to keep sidetabs consistant between files");

		this.enhancedViewer = new JCheckBox(Messages.getMessage("PdfCustomGui.enhancedViewer"));
		this.enhancedViewer.setToolTipText("Set to use enahnced viewer mode");

		this.enhancedFacing = new JCheckBox(Messages.getMessage("PdfCustomGui.enhancedFacing"));
		this.enhancedFacing.setToolTipText("Set to turn facing mode to page turn mode");

		this.thumbnailScroll = new JCheckBox(Messages.getMessage("PdfCustomGui.thumbnailScroll"));
		this.thumbnailScroll.setToolTipText("Set to show thumbnail whilst scrolling");

		this.enhancedGUI = new JCheckBox(Messages.getMessage("PdfCustomGui.enhancedGUI"));
		this.enhancedGUI.setToolTipText("Set to enabled the enhanced gui");

		this.rightClick = new JCheckBox(Messages.getMessage("PdfCustomGui.allowRightClick"));
		this.rightClick.setToolTipText("Set to enable / disable the right click functionality");

		this.scrollwheelZoom = new JCheckBox(Messages.getMessage("PdfCustomGui.allowScrollwheelZoom"));
		this.scrollwheelZoom.setToolTipText("Set to enable zooming when scrolling with ctrl pressed");

		this.historyClearedLabel = new JLabel(Messages.getMessage("PageLayoutViewMenu.HistoryCleared"));
		this.historyClearedLabel.setForeground(Color.red);
		this.historyClearedLabel.setVisible(false);
		this.clearHistory = new JButton(Messages.getMessage("PageLayoutViewMenu.ClearHistory"));
		this.clearHistory.setToolTipText("Clears the history of previous files");
		this.clearHistory.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				gui.clearRecentDocuments();

				SwingWorker searcher = new SwingWorker() {
					@Override
					public Object construct() {
						for (int i = 0; i < 6; i++) {
							SwingProperties.this.historyClearedLabel.setVisible(!SwingProperties.this.historyClearedLabel.isVisible());
							try {
								Thread.sleep(300);
							}
							catch (InterruptedException e) {}
						}
						return null;
					}
				};

				searcher.start();
			}
		});
		JButton save = new JButton(Messages.getMessage("PdfPreferences.SaveAs"));
		save.setToolTipText("Save preferences in a new file");
		JButton reset = new JButton(Messages.getMessage("PdfPreferences.ResetToDefault"));
		reset.setToolTipText("Reset  and save preferences to program defaults");

		// Create JFrame
		this.propertiesDialog.getContentPane().setLayout(new BorderLayout());
		this.propertiesDialog.getContentPane().add(this, BorderLayout.CENTER);
		this.propertiesDialog.pack();
		if (DecoderOptions.isRunningOnMac) this.propertiesDialog.setSize(600, 475);
		else this.propertiesDialog.setSize(550, 450);

		this.confirm.setText(Messages.getMessage("PdfPreferences.OK"));
		this.cancel.setText(Messages.getMessage("PdfPreferences.Cancel"));

		/*
		 * Listeners that are reqired for each setting field
		 */
		// Set properties and close the window
		this.confirm.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				setPreferences(gui);
				if (Viewer.showMessages) JOptionPane.showMessageDialog(null, Messages.getMessage("PdfPreferences.savedTo")
						+ SwingProperties.this.propertiesLocation + '\n' + Messages.getMessage("PdfPreferences.restart"), "Restart Jpedal",
						JOptionPane.INFORMATION_MESSAGE);
				SwingProperties.this.propertiesDialog.setVisible(false);
			}
		});
		this.confirm.setToolTipText("Save the preferences in the current loaded preferences file");
		// Close the window, don't save the properties
		this.cancel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				SwingProperties.this.propertiesDialog.setVisible(false);
			}
		});
		this.cancel.setToolTipText("Leave preferences window without saving changes");
		// Save the properties into a new file
		save.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// The properties file used when jpedal opened
				String lastProperties = gui.getPropertiesFileLocation();

				JFileChooser fileChooser = new JFileChooser();

				int i = fileChooser.showSaveDialog(SwingProperties.this.propertiesDialog);

				if (i == JFileChooser.CANCEL_OPTION) {
					// Do nothing
				}
				else
					if (i == JFileChooser.ERROR_OPTION) {
						// Do nothing
					}
					else
						if (i == JFileChooser.APPROVE_OPTION) {
							File f = fileChooser.getSelectedFile();

							if (f.exists()) f.delete();

							// Setup properties in the new location
							gui.setPropertiesFileLocation(f.getAbsolutePath());
							setPreferences(gui);
						}
				// Reset to the properties file used when jpedal opened
				gui.setPropertiesFileLocation(lastProperties);
			}
		});
		// Reset the properties to JPedal defaults
		reset.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int result = JOptionPane.showConfirmDialog(SwingProperties.this.propertiesDialog, Messages.getMessage("PdfPreferences.reset"),
						"Reset to Default", JOptionPane.YES_NO_OPTION);
				// The properties file used when jpedal opened
				if (result == JOptionPane.YES_OPTION) {
					String lastProperties = gui.getPropertiesFileLocation();

					File f = new File(lastProperties);
					if (f.exists()) {
						f.delete();
						// System.exit(1);
					}

					gui.getProperties().loadProperties(lastProperties);

					if (Viewer.showMessages) JOptionPane.showMessageDialog(SwingProperties.this.propertiesDialog,
							Messages.getMessage("PdfPreferences.restart"));
					SwingProperties.this.propertiesDialog.setVisible(false);
				}
			}
		});

		this.highlightComposite.addKeyListener(new KeyListener() {

			boolean consume = false;

			@Override
			public void keyPressed(KeyEvent e) {
				this.consume = false;
				if ((((JTextField) e.getSource()).getText().contains(".") && e.getKeyChar() == '.')
						&& ((e.getKeyChar() < '0' || e.getKeyChar() > '9') && (e.getKeyCode() != 8 || e.getKeyCode() != 127))) this.consume = true;
			}

			@Override
			public void keyReleased(KeyEvent e) {}

			@Override
			public void keyTyped(KeyEvent e) {
				if (this.consume) e.consume();
			}

		});
		this.highlightComposite.setToolTipText("Set the transparency of the highlight");

		this.resolution.addKeyListener(this.numericalKeyListener);
		this.maxMultiViewers.addKeyListener(this.numericalKeyListener);

		/**
		 * Set the current properties from the properties file
		 */
		setLayout(new BorderLayout());

		// JButtonBar toolbar = new JButtonBar(JButtonBar.VERTICAL);
		JPanel toolbar = new JPanel();

		BoxLayout layout = new BoxLayout(toolbar, BoxLayout.Y_AXIS);
		toolbar.setLayout(layout);

		// if(DecoderOptions.isRunningOnMac)
		// toolbar.setPreferredSize(new Dimension(120,0));

		add(new ButtonBarPanel(toolbar), BorderLayout.CENTER);

		toolbar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Color.gray));

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));

		Dimension dimension = new Dimension(5, 40);
		Box.Filler filler = new Box.Filler(dimension, dimension, dimension);

		this.confirm.setPreferredSize(this.cancel.getPreferredSize());

		if (this.properties.isReadOnly()) this.confirm.setEnabled(false);
		else {
			this.confirm.setEnabled(true);
		}

		buttonPanel.add(reset);
		buttonPanel.add(Box.createHorizontalGlue());
		buttonPanel.add(this.confirm);
		buttonPanel.add(save);
		getRootPane().setDefaultButton(this.confirm);

		buttonPanel.add(filler);
		buttonPanel.add(this.cancel);
		buttonPanel.add(filler);

		buttonPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.gray));

		add(buttonPanel, BorderLayout.SOUTH);
	}

	public void setPreferences(SwingGUI gui) {
		int borderStyle = 0;
		int pageMode = (this.pageLayout.getSelectedIndex() + 1);
		if (pageMode < Display.SINGLE_PAGE || pageMode > Display.PAGEFLOW) pageMode = Display.SINGLE_PAGE;
		if (this.border.isSelected()) {
			borderStyle = 1;
		}

		int hBox = this.highlightBoxColor.getBackground().getRGB();
		int hText = this.highlightTextColor.getBackground().getRGB();
		int vbg = this.viewBGColor.getBackground().getRGB();
		int pbg = this.pdfDecoderBackground.getBackground().getRGB();
		int vfg = this.foreGroundColor.getBackground().getRGB();
		// int sbbg = sideBGColor.getBackground().getRGB();
		boolean changeTL = this.changeTextAndLineArt.isSelected();
		boolean isInvert = this.invertHighlight.isSelected();
		boolean replaceTextColors = this.replaceDocTextCol.isSelected();
		boolean replacePdfDisplayBackground = this.replaceDisplayBGCol.isSelected();
		boolean isBoxShown = this.showMouseSelectionBox.isSelected();

		/**
		 * set preferences from all but menu options
		 */
		this.properties.setValue("borderType", String.valueOf(borderStyle));
		this.properties.setValue("useHinting", String.valueOf(this.useHinting.isSelected()));
		this.properties.setValue("pageMode", String.valueOf(pageMode));
		this.properties.setValue("pageInsets", String.valueOf(this.pageInsets.getText()));
		this.properties.setValue("windowTitle", String.valueOf(this.windowTitle.getText()));
		String loc = this.iconLocation.getText();
		if (!loc.endsWith("/") && !loc.endsWith("\\")) loc = loc + '/';
		this.properties.setValue("iconLocation", String.valueOf(loc));
		this.properties.setValue("sideTabBarCollapseLength", String.valueOf(this.sideTabLength.getText()));
		this.properties.setValue("autoScroll", String.valueOf(this.autoScroll.isSelected()));
		this.properties.setValue("confirmClose", String.valueOf(this.confirmClose.isSelected()));
		this.properties.setValue("openLastDocument", String.valueOf(this.openLastDoc.isSelected()));
		this.properties.setValue("resolution", String.valueOf(this.resolution.getText()));
		this.properties.setValue("searchWindowType", String.valueOf(this.searchStyle.getSelectedIndex()));
		this.properties.setValue("automaticupdate", String.valueOf(this.update.isSelected()));
		this.properties.setValue("maxmultiviewers", String.valueOf(this.maxMultiViewers.getText()));
		this.properties.setValue("showDownloadWindow", String.valueOf(this.downloadWindow.isSelected()));
		this.properties.setValue("useHiResPrinting", String.valueOf(this.HiResPrint.isSelected()));
		this.properties.setValue("consistentTabBar", String.valueOf(this.constantTabs.isSelected()));
		this.properties.setValue("highlightComposite", String.valueOf(this.highlightComposite.getText()));
		this.properties.setValue("highlightBoxColor", String.valueOf(hBox));
		this.properties.setValue("highlightTextColor", String.valueOf(hText));
		this.properties.setValue("vbgColor", String.valueOf(vbg));
		this.properties.setValue("pdfDisplayBackground", String.valueOf(pbg));
		this.properties.setValue("vfgColor", String.valueOf(vfg));
		this.properties.setValue("replaceDocumentTextColors", String.valueOf(replaceTextColors));
		this.properties.setValue("replacePdfDisplayBackground", String.valueOf(replacePdfDisplayBackground));
		// properties.setValue("sbbgColor", String.valueOf(sbbg));
		this.properties.setValue("changeTextAndLineart", String.valueOf(changeTL));
		this.properties.setValue("invertHighlights", String.valueOf(isInvert));
		this.properties.setValue("showMouseSelectionBox", String.valueOf(isBoxShown));
		this.properties.setValue("allowRightClick", String.valueOf(this.rightClick.isSelected()));
		this.properties.setValue("allowScrollwheelZoom", String.valueOf(this.scrollwheelZoom.isSelected()));
		this.properties.setValue("enhancedViewerMode", String.valueOf(this.enhancedViewer.isSelected()));
		this.properties.setValue("enhancedFacingMode", String.valueOf(this.enhancedFacing.isSelected()));
		this.properties.setValue("previewOnSingleScroll", String.valueOf(this.thumbnailScroll.isSelected()));
		this.properties.setValue("enhancedGUI", String.valueOf(this.enhancedGUI.isSelected()));
		this.properties.setValue("printerBlacklist", String.valueOf(this.printerBlacklist.getText()));
		if (((String) this.defaultPrinter.getSelectedItem()).startsWith("System Default")) this.properties.setValue("defaultPrinter", "");
		else this.properties.setValue("defaultPrinter", String.valueOf(this.defaultPrinter.getSelectedItem()));
		this.properties.setValue("defaultDPI", String.valueOf(this.defaultDPI.getText()));
		this.properties.setValue("defaultPagesize", String.valueOf(this.defaultPagesize.getSelectedItem()));
		// Save all options found in a tree
		saveGUIPreferences(gui);
	}

	class ButtonBarPanel extends JPanel {

		private static final long serialVersionUID = 4270546965081614451L;
		private Component currentComponent;

		// Switch between idependent and properties dependent
		// private boolean newPreferencesCode = true;

		public ButtonBarPanel(JPanel toolbar) {
			setLayout(new BorderLayout());

			// Add scroll pane as too many options
			JScrollPane jsp = new JScrollPane();
			jsp.getViewport().add(toolbar);
			jsp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
			jsp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

			add(jsp, BorderLayout.WEST);

			ButtonGroup group = new ButtonGroup();

			addButton(Messages.getMessage("PdfPreferences.GeneralTitle"), "/org/jpedal/examples/viewer/res/display.png", createGeneralSettings(),
					toolbar, group);

			addButton(Messages.getMessage("PdfPreferences.PageDisplayTitle"), "/org/jpedal/examples/viewer/res/pagedisplay.png",
					createPageDisplaySettings(), toolbar, group);

			addButton(Messages.getMessage("PdfPreferences.InterfaceTitle"), "/org/jpedal/examples/viewer/res/interface.png",
					createInterfaceSettings(), toolbar, group);

		}

		private JPanel makePanel(String title) {
			JPanel panel = new JPanel(new BorderLayout());
			JLabel topLeft = new JLabel(title);
			topLeft.setFont(topLeft.getFont().deriveFont(Font.BOLD));
			topLeft.setOpaque(true);
			topLeft.setBackground(panel.getBackground().brighter());

			// JLabel topRight = new JLabel("( "+propertiesLocation+" )");
			// topRight.setOpaque(true);
			// topRight.setBackground(panel.getBackground().brighter());

			JPanel topbar = new JPanel(new BorderLayout());
			topbar.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
			topbar.setFont(topbar.getFont().deriveFont(Font.BOLD));
			topbar.setOpaque(true);
			topbar.setBackground(panel.getBackground().brighter());

			topbar.add(topLeft, BorderLayout.WEST);
			// topbar.add(topRight, BorderLayout.EAST);

			panel.add(topbar, BorderLayout.NORTH);
			panel.setPreferredSize(new Dimension(400, 300));
			panel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
			return panel;
		}

		/*
		 * Creates a pane holding all General settings
		 */
		private JPanel createGeneralSettings() {

			/**
			 * Set values from Properties file
			 */
			String propValue = SwingProperties.this.properties.getValue("resolution");
			if (propValue.length() > 0) SwingProperties.this.resolution.setText(propValue);

			propValue = SwingProperties.this.properties.getValue("useHinting");
			if (propValue.length() > 0 && propValue.equals("true")) SwingProperties.this.useHinting.setSelected(true);
			else SwingProperties.this.useHinting.setSelected(false);

			propValue = SwingProperties.this.properties.getValue("autoScroll");
			if (propValue.equals("true")) SwingProperties.this.autoScroll.setSelected(true);
			else SwingProperties.this.autoScroll.setSelected(false);

			propValue = SwingProperties.this.properties.getValue("confirmClose");
			if (propValue.equals("true")) SwingProperties.this.confirmClose.setSelected(true);
			else SwingProperties.this.confirmClose.setSelected(false);

			propValue = SwingProperties.this.properties.getValue("automaticupdate");
			if (propValue.equals("true")) SwingProperties.this.update.setSelected(true);
			else SwingProperties.this.update.setSelected(false);

			propValue = SwingProperties.this.properties.getValue("openLastDocument");
			if (propValue.equals("true")) SwingProperties.this.openLastDoc.setSelected(true);
			else SwingProperties.this.openLastDoc.setSelected(false);

			JPanel panel = makePanel(Messages.getMessage("PdfPreferences.GeneralTitle"));

			JPanel pane = new JPanel();
			JScrollPane scroll = new JScrollPane(pane);
			scroll.setBorder(BorderFactory.createEmptyBorder());
			pane.setLayout(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.BOTH;

			c.insets = new Insets(5, 0, 0, 5);
			c.weighty = 0;
			c.weightx = 0;
			c.gridx = 0;
			c.gridy = 0;
			JLabel label = new JLabel(Messages.getMessage("PdfPreferences.GeneralSection"));
			label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
			label.setFont(label.getFont().deriveFont(Font.BOLD));
			pane.add(label, c);

			c.gridy++;

			c.insets = new Insets(10, 0, 0, 5);
			c.gridx = 0;
			JLabel label2 = new JLabel(Messages.getMessage("PdfViewerViewMenu.Resolution"));
			label2.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
			pane.add(label2, c);

			c.insets = new Insets(10, 0, 0, 0);
			c.weightx = 1;
			c.gridx = 1;
			pane.add(SwingProperties.this.resolution, c);

			c.gridy++;

			c.gridwidth = 2;
			c.gridx = 0;
			SwingProperties.this.useHinting.setMargin(new Insets(0, 0, 0, 0));
			SwingProperties.this.useHinting.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
			pane.add(SwingProperties.this.useHinting, c);

			c.gridy++;

			c.gridwidth = 2;
			c.gridx = 0;
			SwingProperties.this.autoScroll.setMargin(new Insets(0, 0, 0, 0));
			SwingProperties.this.autoScroll.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
			pane.add(SwingProperties.this.autoScroll, c);

			c.gridy++;

			c.gridwidth = 2;
			c.gridx = 0;
			SwingProperties.this.confirmClose.setMargin(new Insets(0, 0, 0, 0));
			SwingProperties.this.confirmClose.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
			pane.add(SwingProperties.this.confirmClose, c);

			c.gridy++;

			c.insets = new Insets(15, 0, 0, 5);
			c.weighty = 0;
			c.weightx = 0;
			c.gridx = 0;
			JLabel label3 = new JLabel(Messages.getMessage("PdfPreferences.StartUp"));
			label3.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
			label3.setFont(label3.getFont().deriveFont(Font.BOLD));
			pane.add(label3, c);

			c.gridy++;

			c.insets = new Insets(10, 0, 0, 0);
			c.weighty = 0;
			c.weightx = 1;
			c.gridwidth = 2;
			c.gridx = 0;
			SwingProperties.this.update.setMargin(new Insets(0, 0, 0, 0));
			SwingProperties.this.update.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
			pane.add(SwingProperties.this.update, c);

			c.gridy++;

			c.gridwidth = 2;
			c.gridx = 0;
			SwingProperties.this.openLastDoc.setMargin(new Insets(0, 0, 0, 0));
			SwingProperties.this.openLastDoc.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
			pane.add(SwingProperties.this.openLastDoc, c);

			c.gridy++;

			c.gridwidth = 2;
			c.gridx = 0;
			JPanel clearHistoryPanel = new JPanel();
			clearHistoryPanel.setLayout(new BoxLayout(clearHistoryPanel, BoxLayout.X_AXIS));
			clearHistoryPanel.add(SwingProperties.this.clearHistory);
			clearHistoryPanel.add(Box.createHorizontalGlue());

			clearHistoryPanel.add(SwingProperties.this.historyClearedLabel);
			clearHistoryPanel.add(Box.createHorizontalGlue());
			pane.add(clearHistoryPanel, c);

			c.gridy++;

			c.weighty = 1;
			c.gridx = 0;
			pane.add(Box.createVerticalGlue(), c);

			panel.add(scroll, BorderLayout.CENTER);

			return panel;
		}

		/*
		 * Creates a pane holding all Page Display settings (e.g Insets, borders, display modes, etc)
		 */
		private JPanel createPageDisplaySettings() {

			/**
			 * Set values from Properties file
			 */
			String propValue = SwingProperties.this.properties.getValue("enhancedViewerMode");
			if (propValue.length() > 0 && propValue.equals("true")) SwingProperties.this.enhancedViewer.setSelected(true);
			else SwingProperties.this.enhancedViewer.setSelected(false);

			propValue = SwingProperties.this.properties.getValue("borderType");
			if (propValue.length() > 0) if (Integer.parseInt(propValue) == 1) SwingProperties.this.border.setSelected(true);
			else SwingProperties.this.border.setSelected(false);

			propValue = SwingProperties.this.properties.getValue("pageInsets");
			if (propValue != null && propValue.length() != 0) SwingProperties.this.pageInsets.setText(propValue);
			else SwingProperties.this.pageInsets.setText("25");

			propValue = SwingProperties.this.properties.getValue("pageMode");
			if (propValue.length() > 0) {
				int mode = Integer.parseInt(propValue);
				if (mode < Display.SINGLE_PAGE || mode > Display.PAGEFLOW) mode = Display.SINGLE_PAGE;

				SwingProperties.this.pageLayout.setSelectedIndex(mode - 1);
			}

			propValue = SwingProperties.this.properties.getValue("enhancedFacingMode");
			if (propValue.length() > 0 && propValue.equals("true")) SwingProperties.this.enhancedFacing.setSelected(true);
			else SwingProperties.this.enhancedFacing.setSelected(false);

			propValue = SwingProperties.this.properties.getValue("previewOnSingleScroll");
			if (propValue.length() > 0 && propValue.equals("true")) SwingProperties.this.thumbnailScroll.setSelected(true);
			else SwingProperties.this.thumbnailScroll.setSelected(false);

			JPanel panel = makePanel(Messages.getMessage("PdfPreferences.PageDisplayTitle"));

			JPanel pane = new JPanel();
			JScrollPane scroll = new JScrollPane(pane);
			scroll.setBorder(BorderFactory.createEmptyBorder());
			pane.setLayout(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.BOTH;

			c.insets = new Insets(5, 0, 0, 5);
			c.weighty = 0;
			c.weightx = 0;
			c.gridx = 0;
			c.gridy = 0;
			JLabel label = new JLabel(Messages.getMessage("PdfPreferences.GeneralSection"));
			label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
			label.setFont(label.getFont().deriveFont(Font.BOLD));
			pane.add(label, c);

			c.gridy++;

			c.insets = new Insets(5, 0, 0, 0);
			c.gridwidth = 2;
			c.gridx = 0;
			SwingProperties.this.enhancedViewer.setMargin(new Insets(0, 0, 0, 0));
			SwingProperties.this.enhancedViewer.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
			pane.add(SwingProperties.this.enhancedViewer, c);

			c.gridy++;

			c.gridwidth = 2;
			c.gridx = 0;
			SwingProperties.this.border.setMargin(new Insets(0, 0, 0, 0));
			SwingProperties.this.border.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
			pane.add(SwingProperties.this.border, c);

			c.gridy++;

			c.insets = new Insets(5, 0, 0, 0);
			c.gridwidth = 2;
			c.gridx = 0;
			pane.add(SwingProperties.this.pageInsetsText, c);
			c.gridwidth = 2;
			c.gridx = 1;
			pane.add(SwingProperties.this.pageInsets, c);

			c.gridy++;

			c.insets = new Insets(15, 0, 0, 5);
			c.weighty = 0;
			c.weightx = 0;
			c.gridx = 0;
			JLabel label2 = new JLabel(Messages.getMessage("PdfPreferences.DisplayModes"));
			label2.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
			label2.setFont(label2.getFont().deriveFont(Font.BOLD));
			pane.add(label2, c);

			c.gridy++;

			c.insets = new Insets(5, 0, 0, 5);
			c.weighty = 0;
			c.weightx = 0;
			c.gridx = 0;
			JLabel label1 = new JLabel(Messages.getMessage("PageLayoutViewMenu.PageLayout"));
			label1.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
			pane.add(label1, c);

			c.insets = new Insets(5, 0, 0, 0);
			c.weightx = 1;
			c.gridx = 1;
			pane.add(SwingProperties.this.pageLayout, c);

			c.gridy++;

			c.gridwidth = 2;
			c.gridx = 0;
			SwingProperties.this.thumbnailScroll.setMargin(new Insets(0, 0, 0, 0));
			SwingProperties.this.thumbnailScroll.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
			pane.add(SwingProperties.this.thumbnailScroll, c);

			c.gridy++;

			c.weighty = 1;
			c.gridx = 0;
			pane.add(Box.createVerticalGlue(), c);
			panel.add(scroll, BorderLayout.CENTER);

			return panel;
		}

		/*
		 * Creates a pane holding all Interface settings (e.g Search Style, icons, etc)
		 */
		private JPanel createInterfaceSettings() {

			/**
			 * Set values from Properties file
			 */
			String propValue = SwingProperties.this.properties.getValue("enhancedGUI");
			if (propValue.length() > 0 && propValue.equals("true")) SwingProperties.this.enhancedGUI.setSelected(true);
			else SwingProperties.this.enhancedGUI.setSelected(false);

			propValue = SwingProperties.this.properties.getValue("allowRightClick");
			if (propValue.length() > 0 && propValue.equals("true")) SwingProperties.this.rightClick.setSelected(true);
			else SwingProperties.this.rightClick.setSelected(false);

			propValue = SwingProperties.this.properties.getValue("allowScrollwheelZoom");
			if (propValue.length() > 0 && propValue.equals("true")) SwingProperties.this.scrollwheelZoom.setSelected(true);
			else SwingProperties.this.scrollwheelZoom.setSelected(false);

			propValue = SwingProperties.this.properties.getValue("windowTitle");
			if (propValue != null && propValue.length() != 0) SwingProperties.this.windowTitle.setText(propValue);

			propValue = SwingProperties.this.properties.getValue("iconLocation");
			if (propValue != null && propValue.length() != 0) SwingProperties.this.iconLocation.setText(propValue);
			else SwingProperties.this.iconLocation.setText("/org/jpedal/examples/viewer/res/");

			propValue = SwingProperties.this.properties.getValue("searchWindowType");
			if (propValue.length() > 0) SwingProperties.this.searchStyle.setSelectedIndex(Integer.parseInt(propValue));
			else SwingProperties.this.searchStyle.setSelectedIndex(0);

			propValue = SwingProperties.this.properties.getValue("maxmultiviewers");
			if (propValue != null && propValue.length() > 0) SwingProperties.this.maxMultiViewers.setText(propValue);

			propValue = SwingProperties.this.properties.getValue("sideTabBarCollapseLength");
			if (propValue != null && propValue.length() != 0) SwingProperties.this.sideTabLength.setText(propValue);
			else SwingProperties.this.sideTabLength.setText("30");

			propValue = SwingProperties.this.properties.getValue("consistentTabBar");
			if (propValue.length() > 0 && propValue.equals("true")) SwingProperties.this.constantTabs.setSelected(true);
			else SwingProperties.this.constantTabs.setSelected(false);

			String showBox = SwingProperties.this.properties.getValue("showMouseSelectionBox");
			if (showBox.length() > 0 && showBox.toLowerCase().equals("true")) SwingProperties.this.showMouseSelectionBox.setSelected(true);
			else SwingProperties.this.showMouseSelectionBox.setSelected(false);

			JPanel panel = makePanel(Messages.getMessage("PdfPreferences.InterfaceTitle"));

			JTabbedPane tabs = new JTabbedPane();

			JPanel pane = new JPanel();
			JScrollPane scroll = new JScrollPane(pane);
			scroll.setBorder(BorderFactory.createEmptyBorder());
			pane.setLayout(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.BOTH;

			c.insets = new Insets(5, 0, 0, 5);
			c.gridwidth = 1;
			c.gridy = 0;
			c.weighty = 0;
			c.weightx = 0;
			c.gridx = 0;
			JLabel label = new JLabel(Messages.getMessage("PdfPreferences.GeneralTitle"));
			label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
			label.setFont(label.getFont().deriveFont(Font.BOLD));
			pane.add(label, c);

			c.gridy++;

			c.insets = new Insets(5, 0, 0, 5);
			c.gridx = 0;
			c.gridwidth = 2;
			SwingProperties.this.enhancedGUI.setMargin(new Insets(0, 0, 0, 0));
			SwingProperties.this.enhancedGUI.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
			pane.add(SwingProperties.this.enhancedGUI, c);

			c.gridy++;

			c.insets = new Insets(3, 0, 0, 0);
			c.gridwidth = 1;
			c.gridx = 0;
			pane.add(SwingProperties.this.windowTitleText, c);
			c.gridx = 1;
			pane.add(SwingProperties.this.windowTitle, c);

			c.gridy++;

			c.insets = new Insets(5, 0, 0, 0);
			c.gridwidth = 1;
			c.gridx = 0;
			pane.add(SwingProperties.this.iconLocationText, c);
			c.gridx = 1;
			pane.add(SwingProperties.this.iconLocation, c);

			c.gridy++;

			c.insets = new Insets(5, 0, 0, 5);
			c.gridx = 0;
			JLabel label5 = new JLabel(Messages.getMessage("PageLayoutViewMenu.SearchLayout"));
			label5.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
			pane.add(label5, c);

			c.insets = new Insets(5, 0, 0, 0);
			c.weightx = 1;
			c.gridx = 1;
			pane.add(SwingProperties.this.searchStyle, c);

			c.gridy++;

			c.insets = new Insets(15, 0, 0, 5);
			c.gridwidth = 1;
			c.weighty = 0;
			c.weightx = 0;
			c.gridx = 0;
			JLabel label1 = new JLabel(Messages.getMessage("PdfPreferences.SideTab"));
			label1.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
			label1.setFont(label1.getFont().deriveFont(Font.BOLD));
			pane.add(label1, c);

			c.gridy++;

			c.insets = new Insets(5, 0, 0, 0);
			c.gridwidth = 1;
			c.gridx = 0;
			SwingProperties.this.sideTabLengthText.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
			pane.add(SwingProperties.this.sideTabLengthText, c);

			c.insets = new Insets(5, 0, 0, 0);
			c.weightx = 1;
			c.gridx = 1;
			pane.add(SwingProperties.this.sideTabLength, c);

			c.gridy++;

			c.insets = new Insets(5, 0, 0, 0);
			c.gridwidth = 2;
			c.gridx = 0;
			SwingProperties.this.constantTabs.setMargin(new Insets(0, 0, 0, 0));
			SwingProperties.this.constantTabs.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
			pane.add(SwingProperties.this.constantTabs, c);

			c.gridy++;

			c.weighty = 1;
			c.gridx = 0;
			pane.add(Box.createVerticalGlue(), c);
			// pane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(0.3f,0.5f,1f), 1), "Display Settings"));

			tabs.add(Messages.getMessage("PdfPreferences.AppearanceTab"), scroll);

			JPanel pane2 = new JPanel();
			JScrollPane scroll2 = new JScrollPane(pane2);
			scroll2.setBorder(BorderFactory.createEmptyBorder());
			pane2.setLayout(new GridBagLayout());
			c = new GridBagConstraints();
			c.fill = GridBagConstraints.BOTH;

			c.insets = new Insets(5, 0, 0, 5);
			c.gridwidth = 1;
			c.gridy = 0;
			c.weighty = 0;
			c.weightx = 0;
			c.gridx = 0;
			JLabel label3 = new JLabel(Messages.getMessage("PdfPreferences.GeneralTitle"));
			label3.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
			label3.setFont(label3.getFont().deriveFont(Font.BOLD));
			pane2.add(label3, c);

			c.gridy++;

			c.gridwidth = 2;
			c.gridx = 0;
			SwingProperties.this.rightClick.setMargin(new Insets(0, 0, 0, 0));
			SwingProperties.this.rightClick.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
			pane2.add(SwingProperties.this.rightClick, c);

			c.gridy++;

			c.gridwidth = 2;
			c.gridx = 0;
			SwingProperties.this.scrollwheelZoom.setMargin(new Insets(0, 0, 0, 0));
			SwingProperties.this.scrollwheelZoom.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
			pane2.add(SwingProperties.this.scrollwheelZoom, c);

			c.gridy++;

			c.insets = new Insets(0, 0, 0, 5);
			c.gridwidth = 1;
			c.gridx = 0;
			pane2.add(SwingProperties.this.showMouseSelectionBox, c);

			c.gridy++;

			c.weighty = 1;
			c.gridx = 0;
			pane2.add(Box.createVerticalGlue(), c);
			// pane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(0.3f,0.5f,1f), 1), "Display Settings"));

			tabs.add(Messages.getMessage("PdfPreferences.Mouse"), scroll2);

			JPanel pane3 = new JPanel();
			JScrollPane scroll3 = new JScrollPane(pane3);
			scroll3.setBorder(BorderFactory.createEmptyBorder());
			pane3.setLayout(new GridBagLayout());
			c = new GridBagConstraints();
			c.fill = GridBagConstraints.BOTH;

			c.insets = new Insets(5, 0, 0, 5);
			c.gridwidth = 1;
			c.gridy = 0;
			c.weighty = 0;
			c.weightx = 0;
			c.gridx = 0;
			JLabel label6 = new JLabel(Messages.getMessage("PdfPreferences.GeneralTitle"));
			label6.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
			label6.setFont(label6.getFont().deriveFont(Font.BOLD));
			pane3.add(label6, c);

			panel.add(tabs, BorderLayout.CENTER);

			return panel;
		}

		/*
		 * Creates a pane holding all Printing settings
		 */

		private void addMenuToTree(int tab, NodeList nodes, CheckNode top, java.util.List previous) {

			for (int i = 0; i != nodes.getLength(); i++) {

				if (i < nodes.getLength()) {
					String name = nodes.item(i).getNodeName();
					if (!name.startsWith("#")) {
						// Node to add
						CheckNode newLeaf = new CheckNode(Messages.getMessage("PdfCustomGui." + name));
						newLeaf.setEnabled(true);
						// Set to reversedMessage for saving of preferences
						SwingProperties.this.reverseMessage.put(Messages.getMessage("PdfCustomGui." + name), name);
						String propValue = SwingProperties.this.properties.getValue(name);
						// Set if should be selected
						if (propValue.length() > 0 && propValue.equals("true")) {
							newLeaf.setSelected(true);
						}
						else {
							newLeaf.setSelected(false);
						}

						// If has child nodes
						if (nodes.item(i).hasChildNodes()) {
							// Store this top value
							previous.add(top);
							// Set this node to ned top
							top.add(newLeaf);
							// Add new menu to tree
							addMenuToTree(tab, nodes.item(i).getChildNodes(), newLeaf, previous);
						}
						else {
							// Add to current top
							top.add(newLeaf);
						}
					}
				}
			}
		}

		private void show(Component component) {
			if (this.currentComponent != null) {
				remove(this.currentComponent);
			}

			add("Center", this.currentComponent = component);
			revalidate();
			repaint();
		}

		private void addButton(String title, String iconUrl, final Component component, JPanel bar, ButtonGroup group) {
			Action action = new AbstractAction(title, new ImageIcon(getClass().getResource(iconUrl))) {

				private static final long serialVersionUID = -8952528314596587694L;

				@Override
				public void actionPerformed(ActionEvent e) {
					show(component);
				}
			};

			JToggleButton button = new JToggleButton(action);
			button.setVerticalTextPosition(SwingConstants.BOTTOM);
			button.setHorizontalTextPosition(SwingConstants.CENTER);

			button.setContentAreaFilled(false);
			if (DecoderOptions.isRunningOnMac) button.setHorizontalAlignment(SwingConstants.LEFT);

			// Center buttons
			button.setAlignmentX(Component.CENTER_ALIGNMENT);

			bar.add(button);

			group.add(button);

			if (group.getSelection() == null) {
				button.setSelected(true);
				show(component);
			}
		}
	}

	public void setParent(Container parent) {
		this.parent = parent;
	}

	public void dispose() {

		this.removeAll();

		this.reverseMessage = null;

		this.menuTabs = null;
		this.propertiesLocation = null;

		if (this.propertiesDialog != null) this.propertiesDialog.removeAll();
		this.propertiesDialog = null;

		this.confirm = null;

		this.cancel = null;

		if (this.tabs != null) this.tabs.removeAll();
		this.tabs = null;

		this.resolution = null;

		this.searchStyle = null;

		this.border = null;

		this.downloadWindow = null;

		this.HiResPrint = null;

		this.constantTabs = null;

		this.enhancedViewer = null;

		this.enhancedFacing = null;

		this.thumbnailScroll = null;

		this.enhancedGUI = null;

		this.rightClick = null;

		this.scrollwheelZoom = null;

		this.update = null;

		this.maxMultiViewers = null;

		this.pageInsets = null;
		this.pageInsetsText = null;

		this.windowTitle = null;
		this.windowTitleText = null;

		this.iconLocation = null;
		this.iconLocationText = null;

		this.printerBlacklist = null;
		this.printerBlacklistText = null;

		this.defaultPrinter = null;
		this.defaultPrinterText = null;

		this.defaultPagesize = null;
		this.defaultPagesizeText = null;

		this.defaultDPI = null;
		this.defaultDPIText = null;

		this.sideTabLength = null;
		this.sideTabLengthText = null;

		this.useHinting = null;

		this.autoScroll = null;

		this.confirmClose = null;

		this.openLastDoc = null;

		this.pageLayout = null;

		if (this.highlightBoxColor != null) this.highlightBoxColor.removeAll();
		this.highlightBoxColor = null;

		if (this.highlightTextColor != null) this.highlightTextColor.removeAll();
		this.highlightTextColor = null;

		if (this.viewBGColor != null) this.viewBGColor.removeAll();
		this.viewBGColor = null;

		if (this.pdfDecoderBackground != null) this.pdfDecoderBackground.removeAll();
		this.pdfDecoderBackground = null;

		if (this.foreGroundColor != null) this.foreGroundColor.removeAll();
		this.foreGroundColor = null;

		// if(sideBGColor!=null)
		// sideBGColor.removeAll();
		// sideBGColor =null;

		if (this.invertHighlight != null) this.invertHighlight.removeAll();
		this.invertHighlight = null;

		if (this.replaceDocTextCol != null) this.replaceDocTextCol.removeAll();
		this.replaceDocTextCol = null;

		if (this.replaceDisplayBGCol != null) this.replaceDisplayBGCol.removeAll();
		this.replaceDisplayBGCol = null;

		if (this.changeTextAndLineArt != null) this.changeTextAndLineArt.removeAll();
		this.changeTextAndLineArt = null;

		this.showMouseSelectionBox = null;

		if (this.highlightComposite != null) this.highlightComposite.removeAll();
		this.highlightComposite = null;

		if (this.propertiesDialog != null) this.propertiesDialog.removeAll();
		this.parent = null;

		this.clearHistory = null;

		this.historyClearedLabel = null;
	}

	private static boolean hasFreetts() {
		return false;
		/**/
	}
}