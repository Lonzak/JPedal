
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
 * SwingGUI.java
 * ---------------
 */
package org.jpedal.examples.viewer.gui;

import java.awt.Adjustable;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TimerTask;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import javax.swing.border.AbstractBorder;
import javax.swing.border.BevelBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.event.DocumentListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreeSelectionModel;

import org.jpedal.Display;
import org.jpedal.PageOffsets;
import org.jpedal.PdfDecoder;
import org.jpedal.SingleDisplay;
import org.jpedal.examples.viewer.Commands;
import org.jpedal.examples.viewer.Values;
import org.jpedal.examples.viewer.Viewer;
import org.jpedal.examples.viewer.gui.generic.GUIButton;
import org.jpedal.examples.viewer.gui.generic.GUICombo;
import org.jpedal.examples.viewer.gui.generic.GUISearchWindow;
import org.jpedal.examples.viewer.gui.generic.GUIThumbnailPanel;
import org.jpedal.examples.viewer.gui.popups.SwingProperties;
import org.jpedal.examples.viewer.gui.swing.CommandListener;
import org.jpedal.examples.viewer.gui.swing.FrameCloser;
import org.jpedal.examples.viewer.gui.swing.SearchList;
import org.jpedal.examples.viewer.gui.swing.SwingButton;
import org.jpedal.examples.viewer.gui.swing.SwingCheckBoxMenuItem;
import org.jpedal.examples.viewer.gui.swing.SwingCombo;
import org.jpedal.examples.viewer.gui.swing.SwingID;
import org.jpedal.examples.viewer.gui.swing.SwingMenuItem;
import org.jpedal.examples.viewer.gui.swing.SwingOutline;
import org.jpedal.examples.viewer.gui.swing.SwingSearchWindow;
import org.jpedal.examples.viewer.paper.PaperSizes;
import org.jpedal.examples.viewer.utils.Printer;
import org.jpedal.examples.viewer.utils.PropertiesFile;
import org.jpedal.exception.PdfException;
import org.jpedal.external.CustomMessageHandler;
import org.jpedal.external.Options;
import org.jpedal.fonts.FontMappings;
import org.jpedal.fonts.StandardFonts;
import org.jpedal.fonts.tt.TTGlyph;
import org.jpedal.gui.GUIFactory;
import org.jpedal.gui.ShowGUIMessage;
import org.jpedal.io.StatusBar;
import org.jpedal.linear.LinearThread;
import org.jpedal.objects.PdfFileInformation;
import org.jpedal.objects.PdfPageData;
import org.jpedal.objects.acroforms.actions.ActionHandler;
import org.jpedal.objects.acroforms.creation.FormFactory;
import org.jpedal.objects.acroforms.formData.GUIData;
import org.jpedal.objects.acroforms.rendering.AcroRenderer;
import org.jpedal.objects.layers.PdfLayerList;
import org.jpedal.objects.raw.FormObject;
import org.jpedal.objects.raw.PdfArrayIterator;
import org.jpedal.objects.raw.PdfDictionary;
import org.jpedal.objects.raw.PdfObject;
import org.jpedal.parser.DecodeStatus;
import org.jpedal.parser.DecoderOptions;
import org.jpedal.render.BaseDisplay;
import org.jpedal.utils.BrowserLauncher;
import org.jpedal.utils.LogWriter;
import org.jpedal.utils.Messages;
import org.jpedal.utils.SwingWorker;
import org.jpedal.utils.repositories.Vector_Int;
import org.w3c.dom.Node;
//<start-pdfhelp>
//<end-pdfhelp>

/**
 * <br>
 * Description: Swing GUI functions in Viewer
 * 
 * 
 */
public class SwingGUI extends GUI implements GUIFactory {

	private boolean previewOnSingleScroll = true;

	private Timer memoryMonitor = null;

	// flag for marks new thumbnail preview
	private boolean debugThumbnail = false;

	ScrollListener scrollListener;

	ScrollMouseListener scrollMouseListener;

	JScrollBar thumbscroll = null;

	public JScrollBar getThumbnailScrollBar() {
		return this.thumbscroll;
	}

	private boolean isMultiPageTiff = false;

	static final private boolean enableShowSign = true;

	boolean finishedDecoding = false;
	static int startSize = 30, expandedSize = 190;

	public static void setStartSize(int startSize) {
		SwingGUI.startSize = startSize;
	}

	// use new GUI layout
	public boolean useNewLayout = true;

	public static String windowTitle;

	String pageTitle, bookmarksTitle, signaturesTitle, layersTitle;
	boolean hasListener = false;
	private boolean isSetup = false;
	int lastTabSelected = -1;
	public boolean messageShown = false;

	/** grabbing cursor */
	private Cursor grabCursor, grabbingCursor, panCursor, panCursorL, panCursorTL, panCursorT, panCursorTR, panCursorR, panCursorBR, panCursorB,
			panCursorBL;
	public final static int GRAB_CURSOR = 1;
	public final static int GRABBING_CURSOR = 2;
	public final static int DEFAULT_CURSOR = 3;
	public final static int PAN_CURSOR = 4;
	public final static int PAN_CURSORL = 5;
	public final static int PAN_CURSORTL = 6;
	public final static int PAN_CURSORT = 7;
	public final static int PAN_CURSORTR = 8;
	public final static int PAN_CURSORR = 9;
	public final static int PAN_CURSORBR = 10;
	public final static int PAN_CURSORB = 11;
	public final static int PAN_CURSORBL = 12;

	private PaperSizes paperSizes;

	/** Multibox for new GUI Layout */
	// Component to contain memory, cursor and loading bars
	private JPanel multibox = new JPanel();
	public final static int CURSOR = 1;

	ButtonGroup layoutGroup = new ButtonGroup();

	ButtonGroup searchLayoutGroup = new ButtonGroup();

	ButtonGroup borderGroup = new ButtonGroup();

	/** Constants for glowing border */
	public int glowThickness = 11;
	public Color glowOuterColor = new Color(0.0f, 0.0f, 0.0f, 0.0f);
	public Color glowInnerColor = new Color(0.8f, 0.75f, 0.45f, 0.8f);

	/** Track whether both pages are properly displayed */
	private boolean pageTurnScalingAppropriate = true;

	/** Whether the left page drag or right page drag is drawing */
	private boolean dragLeft = false;
	private boolean dragTop = false;

	/** listener on buttons, menus, combboxes to execute options (one instance on all objects) */
	private CommandListener currentCommandListener;

	/** holds OPEN, INFO,etc */
	private JToolBar topButtons = new JToolBar();

	/** holds back/forward buttons at bottom of page */
	private JToolBar navButtons = new JToolBar();

	/** holds rotation, quality, scaling and status */
	private JToolBar comboBoxBar = new JToolBar();

	/** holds all menu entries (File, View, Help) */
	private JMenuBar currentMenu = new JMenuBar();

	/** tell user on first form change it can be saved */
	private boolean firstTimeFormMessage = true;

	/** visual display of current cursor co-ords on page */
	private JLabel coords = new JLabel();

	/** root element to hold display */
	private Container frame = new JFrame();

	/** alternative internal JFrame */
	private JDesktopPane desktopPane = new JDesktopPane();

	/** flag to disable functions */
	boolean isSingle = true;

	/** displayed on left to hold thumbnails, bookmarks */
	private JTabbedPane navOptionsPanel = new JTabbedPane();

	/** split display between PDF and thumbnails/bookmarks */
	private JSplitPane displayPane;

	/** Scrollpane for pdf panel */
	private JScrollPane scrollPane = new JScrollPane();

	private Font headFont = new Font("SansSerif", Font.BOLD, 14);

	private Font textFont1 = new Font("SansSerif", Font.PLAIN, 12);

	private Font textFont = new Font("Serif", Font.PLAIN, 12);

	/** Interactive display object - needs to be added to PdfDecoder */
	private StatusBar statusBar = new StatusBar(new Color(235, 154, 0));
	private StatusBar downloadBar = new StatusBar(new Color(185, 209, 0));

	private JLabel pageCounter1;

	// allow user to control messages in Viewer
	CustomMessageHandler customMessageHandler = null;

	public JTextField pageCounter2 = new JTextField(4);

	private JLabel pageCounter3;

	private JLabel optimizationLabel;

	private JTree signaturesTree;

	private JPanel layersPanel = new JPanel();

	/** user dir in which program can write */
	private String user_dir = System.getProperty("user.dir");

	/** stop user forcing open tab before any pages loaded */
	private boolean tabsNotInitialised = true;
	private JToolBar navToolBar = new JToolBar();
	private JToolBar pagesToolBar = new JToolBar();

	// Optional Buttons for menu Search
	public GUIButton nextSearch, previousSearch;

	// layers tab
	PdfLayerList layersObject;

	// Progress bar on nav bar
	private final JProgressBar memoryBar = new JProgressBar();

	// Component to display cursor position on page
	JToolBar cursor = new JToolBar();

	// Buttons on the function bar
	private GUIButton openButton;
	private GUIButton printButton;
	private GUIButton searchButton;
	private GUIButton docPropButton;
	private GUIButton infoButton;
	public GUIButton mouseMode;

	// Menu items for gui
	private JMenu fileMenu;
	private JMenu openMenu;
	private JMenuItem open;
	private JMenuItem openUrl;
	private JMenuItem save;
	private JMenuItem reSaveAsForms;
	private JMenuItem find;
	private JMenuItem documentProperties;
	private JMenuItem signPDF;
	private JMenuItem print;
	// private JMenuItem recentDocuments;
	private JMenuItem exit;
	private JMenu editMenu;
	private JMenuItem copy;
	private JMenuItem selectAll;
	private JMenuItem deselectAll;
	private JMenuItem preferences;
	private JMenu viewMenu;
	private JMenu goToMenu;
	private JMenuItem firstPage;
	private JMenuItem backPage;
	private JMenuItem forwardPage;
	private JMenuItem lastPage;
	private JMenuItem goTo;
	private JMenuItem previousDocument;
	private JMenuItem nextDocument;
	private JMenu pageLayoutMenu;
	private JMenuItem single;
	private JMenuItem continuous;
	private JMenuItem facing;
	private JMenuItem continuousFacing;
	private JMenuItem pageFlow;
	private JMenuItem textSelect;
	private JCheckBoxMenuItem separateCover;
	private JMenuItem panMode;
	private JMenuItem fullscreen;
	private JMenu windowMenu;
	private JMenuItem cascade;
	private JMenuItem tile;
	private JMenu exportMenu;
	private JMenu pdfMenu;
	private JMenuItem onePerPage;
	private JMenuItem nup;
	private JMenuItem handouts;
	private JMenu contentMenu;
	private JMenuItem images;
	private JMenuItem text;
	private JMenuItem bitmap;
	private JMenu pageToolsMenu;
	private JMenuItem rotatePages;
	private JMenuItem deletePages;
	private JMenuItem addPage;
	private JMenuItem addHeaderFooter;
	private JMenuItem stampText;
	private JMenuItem stampImage;
	private JMenuItem crop;
	private JMenu helpMenu;
	private JMenuItem visitWebsite;
	private JMenuItem tipOfTheDay;
	private JMenuItem checkUpdates;
	private JMenuItem about;

	// private Timer t;

	private DefaultMutableTreeNode topLayer = new DefaultMutableTreeNode("Layers");

	public SwingGUI(PdfDecoder decode_pdf, Values commonValues, GUIThumbnailPanel thumbnails, PropertiesFile properties) {

		this.decode_pdf = decode_pdf;
		this.commonValues = commonValues;
		this.thumbnails = thumbnails;
		this.properties = properties;

		// pass in SwingGUI so we can call via callback
		decode_pdf.addExternalHandler(this, Options.SwingContainer);

		/**
		 * setup display multiview display
		 */
		if (this.isSingle) {
			this.desktopPane.setBackground(this.frame.getBackground());
			this.desktopPane.setVisible(true);
			if (this.frame instanceof JFrame) ((JFrame) this.frame).getContentPane().add(this.desktopPane, BorderLayout.CENTER);
			else this.frame.add(this.desktopPane, BorderLayout.CENTER);

		}

		// <start-demo>
		/**
		 * //<end-demo>
		 * 
		 * /
		 **/
	}

	public JComponent getDisplayPane() {
		return this.displayPane;
	}

	public JDesktopPane getMultiViewerFrames() {
		return this.desktopPane;
	}

	public void setPdfDecoder(PdfDecoder decode_pdf) {
		this.decode_pdf = decode_pdf;
	}

	public void closeMultiViewerWindow(String selectedFile) {
		JInternalFrame[] allFrames = this.desktopPane.getAllFrames();
		for (JInternalFrame internalFrame : allFrames) {
			if (internalFrame.getTitle().equals(selectedFile)) {
				try {
					internalFrame.setClosed(true);
				}
				catch (PropertyVetoException e) {}
				break;
			}
		}
	}

	/**
	 * adjusty x co-ordinate shown in display for user to include any page centering
	 */
	public int AdjustForAlignment(int cx) {

		if (this.decode_pdf.getPageAlignment() == Display.DISPLAY_CENTERED) {
			int width = this.decode_pdf.getBounds().width;
			int pdfWidth = this.decode_pdf.getPDFWidth();

			if (this.decode_pdf.getDisplayView() != Display.SINGLE_PAGE) pdfWidth = (int) this.decode_pdf.getMaximumSize().getWidth();

			if (width > pdfWidth) cx = cx - ((width - pdfWidth) / (2));
		}

		return cx;
	}

	public String getBookmark(String bookmark) {
		return this.tree.getPage(bookmark);
	}

	public void reinitialiseTabs(boolean showVisible) {

		// not needed
		if (this.commonValues.getModeOfOperation() == Values.RUNNING_PLUGIN) return;

		if (this.properties.getValue("ShowSidetabbar").toLowerCase().equals("true")) {

			if (!this.isSingle) return;

			if (!showVisible) this.displayPane.setDividerLocation(startSize);

			this.lastTabSelected = -1;

			if (!this.commonValues.isPDF()) {
				this.navOptionsPanel.setVisible(false);
			}
			else {
				this.navOptionsPanel.setVisible(true);

				/**
				 * add/remove optional tabs
				 */
				if (!this.decode_pdf.hasOutline()) {

					int outlineTab = -1;

					if (DecoderOptions.isRunningOnMac) {
						// String tabName="";
						// see if there is an outlines tab
						for (int jj = 0; jj < this.navOptionsPanel.getTabCount(); jj++) {
							if (this.navOptionsPanel.getTitleAt(jj).equals(this.bookmarksTitle)) outlineTab = jj;
						}
					}
					else {
						// String tabName="";
						// see if there is an outlines tab
						for (int jj = 0; jj < this.navOptionsPanel.getTabCount(); jj++) {
							if (this.navOptionsPanel.getIconAt(jj).toString().equals(this.bookmarksTitle)) outlineTab = jj;
						}
					}

					if (outlineTab != -1) this.navOptionsPanel.remove(outlineTab);

				}
				else
					if (this.properties.getValue("Bookmarkstab").toLowerCase().equals("true")) {
						int outlineTab = -1;
						if (DecoderOptions.isRunningOnMac) {
							// String tabName="";
							// see if there is an outlines tab
							for (int jj = 0; jj < this.navOptionsPanel.getTabCount(); jj++) {
								if (this.navOptionsPanel.getTitleAt(jj).equals(this.bookmarksTitle)) outlineTab = jj;
							}

							if (outlineTab == -1) this.navOptionsPanel.addTab(this.bookmarksTitle, (SwingOutline) this.tree);
						}
						else {
							// String tabName="";
							// see if there is an outlines tab
							for (int jj = 0; jj < this.navOptionsPanel.getTabCount(); jj++) {
								if (this.navOptionsPanel.getIconAt(jj).toString().equals(this.bookmarksTitle)) outlineTab = jj;
							}

							if (outlineTab == -1) {
								VTextIcon textIcon2 = new VTextIcon(this.navOptionsPanel, this.bookmarksTitle, VTextIcon.ROTATE_LEFT);
								this.navOptionsPanel.addTab(null, textIcon2, (SwingOutline) this.tree);
							}
						}
					}

				/** handle signatures pane */
				AcroRenderer currentFormRenderer = this.decode_pdf.getFormRenderer();

				Iterator signatureObjects = currentFormRenderer.getSignatureObjects();

				if (signatureObjects != null) {

					DefaultMutableTreeNode root = new DefaultMutableTreeNode("Signatures");

					DefaultMutableTreeNode signed = new DefaultMutableTreeNode("The following have digitally counter-signed this document");

					DefaultMutableTreeNode blank = new DefaultMutableTreeNode("The following signature fields are not signed");

					while (signatureObjects.hasNext()) {

						FormObject formObj = (FormObject) signatureObjects.next();

						PdfObject sigObject = formObj.getDictionary(PdfDictionary.V);

						this.decode_pdf.getIO().checkResolved(formObj);

						if (sigObject == null) {

							if (!blank.isNodeChild(root)) root.add(blank);

							DefaultMutableTreeNode blankNode = new DefaultMutableTreeNode(formObj.getTextStreamValue(PdfDictionary.T) + " on page "
									+ formObj.getPageNumber());
							blank.add(blankNode);

						}
						else {

							if (!signed.isNodeChild(root)) root.add(signed);

							// String name = (String) OLDsigObject.get("Name");

							String name = sigObject.getTextStreamValue(PdfDictionary.Name);

							DefaultMutableTreeNode owner = new DefaultMutableTreeNode("Signed by " + name);
							signed.add(owner);

							DefaultMutableTreeNode type = new DefaultMutableTreeNode("Type");
							owner.add(type);

							String filter = null;// sigObject.getName(PdfDictionary.Filter);

							// @simon -new version to test
							PdfArrayIterator filters = sigObject.getMixedArray(PdfDictionary.Filter);
							if (filters != null && filters.hasMoreTokens()) filter = filters.getNextValueAsString(true);

							DefaultMutableTreeNode filterNode = new DefaultMutableTreeNode("Filter: " + filter);
							type.add(filterNode);

							String subFilter = sigObject.getName(PdfDictionary.SubFilter);

							DefaultMutableTreeNode subFilterNode = new DefaultMutableTreeNode("Sub Filter: " + subFilter);
							type.add(subFilterNode);

							DefaultMutableTreeNode details = new DefaultMutableTreeNode("Details");
							owner.add(details);

							// @simon - guess on my part....
							String rawDate = sigObject.getTextStreamValue(PdfDictionary.M);
							// if(rawDate!=null){

							StringBuilder date = new StringBuilder(rawDate);

							date.delete(0, 2);
							date.insert(4, '/');
							date.insert(7, '/');
							date.insert(10, ' ');
							date.insert(13, ':');
							date.insert(16, ':');
							date.insert(19, ' ');

							DefaultMutableTreeNode time = new DefaultMutableTreeNode("Time: " + date);
							details.add(time);
							// }

							String reason = sigObject.getTextStreamValue(PdfDictionary.Reason);

							DefaultMutableTreeNode reasonNode = new DefaultMutableTreeNode("Reason: " + reason);
							details.add(reasonNode);

							String location = sigObject.getTextStreamValue(PdfDictionary.Location);

							DefaultMutableTreeNode locationNode = new DefaultMutableTreeNode("Location: " + location);
							details.add(locationNode);

							DefaultMutableTreeNode field = new DefaultMutableTreeNode("Field: " + formObj.getTextStreamValue(PdfDictionary.T)
									+ " on page " + formObj.getPageNumber());
							details.add(field);
						}
					}
					if (this.signaturesTree == null) {
						this.signaturesTree = new JTree();

						SignaturesTreeCellRenderer treeCellRenderer = new SignaturesTreeCellRenderer();
						this.signaturesTree.setCellRenderer(treeCellRenderer);

					}
					((DefaultTreeModel) this.signaturesTree.getModel()).setRoot(root);

					checkTabShown(this.signaturesTitle);
				}
				else removeTab(this.signaturesTitle);

				// <link><a name="layers" />
				/**
				 * add a control Panel to enable/disable layers
				 */
				// layers object
				this.layersObject = (PdfLayerList) this.decode_pdf.getJPedalObject(PdfDictionary.Layer);

				if (this.layersObject != null && this.layersObject.getLayersCount() > 0) { // some files have empty Layers objects

					this.layersPanel.removeAll(); // flush any previous items

					this.layersPanel.setLayout(new BorderLayout());

					checkTabShown(this.layersTitle);

					/**
					 * add metadata to tab (Map of key values) as a Tree
					 */
					DefaultMutableTreeNode top = new DefaultMutableTreeNode("Info");

					Map metaData = this.layersObject.getMetaData();

					Iterator metaDataKeys = metaData.keySet().iterator();
					Object nextKey, value;
					while (metaDataKeys.hasNext()) {

						nextKey = metaDataKeys.next();
						value = metaData.get(nextKey);

						top.add(new DefaultMutableTreeNode(nextKey + "=" + value));

					}

					// add collapsed Tree at Top
					final JTree infoTree = new JTree(top);
					infoTree.setToolTipText("Double click to see any metadata");
					infoTree.setRootVisible(true);
					infoTree.collapseRow(0);
					this.layersPanel.add(infoTree, BorderLayout.NORTH);

					/**
					 * Display list of layers which can be recursive layerNames can contain comments or sub-trees as Object[] or String name of Layer
					 */
					final Object[] layerNames = this.layersObject.getDisplayTree();
					if (layerNames != null) {

						this.topLayer.removeAllChildren();

						final JTree layersTree = new JTree(this.topLayer);
						layersTree.setName("LayersTree");

						// Listener to redraw with altered layer
						layersTree.addTreeSelectionListener(new TreeSelectionListener() {

							@Override
							public void valueChanged(TreeSelectionEvent e) {

								final DefaultMutableTreeNode node = (DefaultMutableTreeNode) layersTree.getLastSelectedPathComponent();

								/* exit if nothing is selected */
								if (node == null) return;

								/* retrieve the full name of Layer that was selected */
								String rawName = (String) node.getUserObject();

								// and add path
								Object[] patentNames = ((DefaultMutableTreeNode) node.getParent()).getUserObjectPath();
								int size = patentNames.length;
								for (int jj = size - 1; jj > 0; jj--) { // note I ingore 0 which is root and work backwards
									rawName = rawName + PdfLayerList.deliminator + patentNames[jj].toString();
								}

								final String name = rawName;

								// if allowed toggle and update display
								if (SwingGUI.this.layersObject.isLayerName(name) && !SwingGUI.this.layersObject.isLocked(name)) {

									// toggle layer status when clicked
									Runnable updateAComponent = new Runnable() {

										@Override
										public void run() {
											SwingGUI.this.decode_pdf.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
											// force refresh
											SwingGUI.this.decode_pdf.invalidate();
											SwingGUI.this.decode_pdf.updateUI();
											SwingGUI.this.decode_pdf.validate();

											SwingGUI.this.scrollPane.invalidate();
											SwingGUI.this.scrollPane.updateUI();
											SwingGUI.this.scrollPane.validate();

											// update settings on display and in PdfDecoder
											CheckNode checkNode = (CheckNode) node;

											if (!checkNode.isEnabled()) { // selection not allowed so display info message

												checkNode.setSelected(checkNode.isSelected());
												ShowGUIMessage
														.showstaticGUIMessage(new StringBuffer(
																"This layer has been disabled because its parent layer is disabled"),
																"Parent Layer disabled");
											}
											else {
												boolean reversedStatus = !checkNode.isSelected();
												checkNode.setSelected(reversedStatus);
												SwingGUI.this.layersObject.setVisiblity(name, reversedStatus);

												// may be radio buttons which disable others so sync values
												// before repaint
												syncTreeDisplay(SwingGUI.this.topLayer, true);

												// decode again with new settings
												try {
													SwingGUI.this.decode_pdf.decodePage(SwingGUI.this.commonValues.getCurrentPage());
												}
												catch (Exception e) {
													e.printStackTrace(); // To change body of catch statement use File | Settings | File Templates.
												}

											}
											// deselect so works if user clicks on same again to deselect
											layersTree.invalidate();
											layersTree.clearSelection();
											layersTree.repaint();
											SwingGUI.this.decode_pdf.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
										}
									};

									SwingUtilities.invokeLater(updateAComponent);

								}
							}
						});

						// build tree from values
						this.topLayer.removeAllChildren();
						addLayersToTree(layerNames, this.topLayer, true);

						layersTree.setRootVisible(true);
						layersTree.expandRow(0);
						layersTree.setCellRenderer(new CheckRenderer());
						layersTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

						this.layersPanel.add(layersTree, BorderLayout.CENTER);

					}

				}
				else removeTab(this.layersTitle);

				setBookmarks(false);
			}

			// @kieran this is temporary but works for now.
			// DisplayPane adjustment
			int orig = this.displayPane.getDividerLocation();
			this.displayPane.setDividerLocation(0);
			this.displayPane.validate();
			this.displayPane.setDividerLocation(orig);

		}
	}

	private void syncTreeDisplay(DefaultMutableTreeNode topLayer, boolean isEnabled) {

		int count = topLayer.getChildCount();

		boolean parentIsEnabled = isEnabled, isSelected;
		String childName = "";
		TreeNode childNode;
		int ii = 0;
		while (true) {

			isEnabled = parentIsEnabled;
			isSelected = true;

			if (count == 0) childNode = topLayer;
			else childNode = topLayer.getChildAt(ii);

			if (childNode instanceof CheckNode) {

				CheckNode cc = (CheckNode) childNode;
				childName = (String) cc.getText();

				if (this.layersObject.isLayerName(childName)) {

					if (isEnabled) isEnabled = !this.layersObject.isLocked(childName);

					isSelected = (this.layersObject.isVisible(childName));
					cc.setSelected(isSelected);
					cc.setEnabled(isEnabled);
				}
			}

			if (childNode.getChildCount() > 0) {

				Enumeration children = childNode.children();
				while (children.hasMoreElements())
					syncTreeDisplay((DefaultMutableTreeNode) children.nextElement(), (isEnabled && isSelected));
			}

			ii++;
			if (ii >= count) break;
		}
	}

	private void addLayersToTree(Object[] layerNames, DefaultMutableTreeNode topLayer, boolean isEnabled) {

		String name;

		DefaultMutableTreeNode currentNode = topLayer;
		boolean parentEnabled = isEnabled, parentIsSelected = true;

		for (Object layerName : layerNames) {

			// work out type of node and handle
			if (layerName instanceof Object[]) { // its a subtree

				DefaultMutableTreeNode oldNode = currentNode;

				addLayersToTree((Object[]) layerName, currentNode, isEnabled && parentIsSelected);

				currentNode = oldNode;
				// if(currentNode!=null)
				// currentNode= (DefaultMutableTreeNode) currentNode.getParent();

				isEnabled = parentEnabled;
			}
			else {

				// store possible recursive settings
				parentEnabled = isEnabled;

				if (layerName == null) continue;

				if (layerName instanceof String) name = (String) layerName;
				else // its a byte[]
				name = new String((byte[]) layerName);

				/**
				 * remove full path in name
				 */
				String title = name;
				int ptr = name.indexOf(PdfLayerList.deliminator);
				if (ptr != -1) {
					title = title.substring(0, ptr);

				}

				if (name.endsWith(" R")) { // ignore
				}
				else
					if (!this.layersObject.isLayerName(name)) { // just text

						currentNode = new DefaultMutableTreeNode(title);
						topLayer.add(currentNode);
						topLayer = currentNode;

						parentIsSelected = true;

						// add a node
					}
					else
						if (topLayer != null) {

							currentNode = new CheckNode(title);
							topLayer.add(currentNode);

							// see if showing and set box to match
							if (this.layersObject.isVisible(name)) {
								((CheckNode) currentNode).setSelected(true);
								parentIsSelected = true;
							}
							else parentIsSelected = false;

							// check locks and allow Parents to disable children
							if (isEnabled) isEnabled = !this.layersObject.isLocked(name);

							((CheckNode) currentNode).setEnabled(isEnabled);
						}
			}
		}
	}

	private void checkTabShown(String title) {
		int outlineTab = -1;
		if (DecoderOptions.isRunningOnMac) {

			// see if there is an outlines tab
			for (int jj = 0; jj < this.navOptionsPanel.getTabCount(); jj++) {
				if (this.navOptionsPanel.getTitleAt(jj).equals(title)) outlineTab = jj;
			}

			if (outlineTab == -1) {
				if (title.equals(this.signaturesTitle) && this.properties.getValue("Signaturestab").toLowerCase().equals("true")) {
					if (this.signaturesTree == null) {
						this.signaturesTree = new JTree();

						SignaturesTreeCellRenderer treeCellRenderer = new SignaturesTreeCellRenderer();
						this.signaturesTree.setCellRenderer(treeCellRenderer);

					}
					this.navOptionsPanel.addTab(this.signaturesTitle, this.signaturesTree);
					this.navOptionsPanel.setTitleAt(this.navOptionsPanel.getTabCount() - 1, this.signaturesTitle);

				}
				else
					if (title.equals(this.layersTitle) && this.properties.getValue("Layerstab").toLowerCase().equals("true")) {

						JScrollPane scrollPane = new JScrollPane();
						scrollPane.getViewport().add(this.layersPanel);
						scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
						scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

						this.navOptionsPanel.addTab(this.layersTitle, scrollPane);
						this.navOptionsPanel.setTitleAt(this.navOptionsPanel.getTabCount() - 1, this.layersTitle);

					}
			}

		}
		else {
			// see if there is an outlines tab
			for (int jj = 0; jj < this.navOptionsPanel.getTabCount(); jj++) {
				if (this.navOptionsPanel.getIconAt(jj).toString().equals(title)) outlineTab = jj;
			}

			if (outlineTab == -1) {

				if (title.equals(this.signaturesTitle) && this.properties.getValue("Signaturestab").toLowerCase().equals("true")) { // stop spurious
																																	// display of Sig
																																	// tab
					VTextIcon textIcon2 = new VTextIcon(this.navOptionsPanel, this.signaturesTitle, VTextIcon.ROTATE_LEFT);
					this.navOptionsPanel.addTab(null, textIcon2, this.signaturesTree);
					// navOptionsPanel.setTitleAt(navOptionsPanel.getTabCount()-1, signaturesTitle);
				}
				else
					if (title.equals(this.layersTitle) && this.properties.getValue("Layerstab").toLowerCase().equals("true")) {
						VTextIcon textIcon = new VTextIcon(this.navOptionsPanel, this.layersTitle, VTextIcon.ROTATE_LEFT);

						JScrollPane scrollPane = new JScrollPane();
						scrollPane.getViewport().add(this.layersPanel);
						scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
						scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

						this.navOptionsPanel.addTab(null, textIcon, scrollPane);
					}
				// navOptionsPanel.setTitleAt(navOptionsPanel.getTabCount()-1, layersTitle);
			}
		}
	}

	private void removeTab(String title) {

		int outlineTab = -1;

		if (DecoderOptions.isRunningOnMac) {
			// String tabName="";
			// see if there is an outlines tab
			for (int jj = 0; jj < this.navOptionsPanel.getTabCount(); jj++) {
				if (this.navOptionsPanel.getTitleAt(jj).equals(title)) outlineTab = jj;
			}
		}
		else {
			// String tabName="";
			// see if there is an outlines tab
			for (int jj = 0; jj < this.navOptionsPanel.getTabCount(); jj++) {
				if (this.navOptionsPanel.getIconAt(jj).toString().equals(title)) outlineTab = jj;
			}
		}

		if (outlineTab != -1) this.navOptionsPanel.remove(outlineTab);
	}

	public void stopThumbnails() {

		if (!this.isSingle) return;

		if (this.thumbnails.isShownOnscreen()) {
			/** if running terminate first */
			this.thumbnails.terminateDrawing();

			this.thumbnails.removeAllListeners();

		}
	}

	public void reinitThumbnails() {

		this.isSetup = false;
	}

	/** reset so appears closed */
	@Override
	public void resetNavBar() {

		if (!this.properties.getValue("consistentTabBar").toLowerCase().equals("true")) {

			if (!this.isSingle) return;

			this.displayPane.setDividerLocation(startSize);
			this.tabsNotInitialised = true;

			// also reset layers
			this.topLayer.removeAllChildren();

			// disable page view buttons until we know we have multiple pages
			setPageLayoutButtonsEnabled(false);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.jpedal.examples.viewer.gui.swing.GUIFactory#setNoPagesDecoded() Called when new file opened so we set flags here
	 */
	@Override
	public void setNoPagesDecoded() {

		this.bookmarksGenerated = false;

		resetNavBar();

		// Ensure preview from last file doesn't appear
		if (this.scrollListener != null) this.scrollListener.lastImage = null;

		this.pagesDecoded.clear();
	}

	public void setBackNavigationButtonsEnabled(boolean flag) {

		// if(!isSingle)
		// return;

		this.back.setEnabled(flag);
		this.first.setEnabled(flag);
		this.fback.setEnabled(flag);
	}

	public void setForwardNavigationButtonsEnabled(boolean flag) {

		// if(!isSingle)
		// return;

		this.forward.setEnabled(flag);
		this.end.setEnabled(flag);
		this.fforward.setEnabled(flag);
	}

	public void setPageLayoutButtonsEnabled(boolean flag) {

		if (!this.isSingle) return;

		this.continuousButton.setEnabled(flag);
		this.continuousFacingButton.setEnabled(flag);
		this.facingButton.setEnabled(flag);

		this.pageFlowButton.setEnabled(flag);

		Enumeration menuOptions = this.layoutGroup.getElements();

		// @kieran - added fix below. Can you recode without Enumeration
		// object please (several refs) so we can keep 1.4 compatability.

		// export menu is broken in standalone (works in IDE). Is this related?

		// we cannot assume there are values so trap to avoid exception
		if (menuOptions.hasMoreElements()) {

			// first one is always ON
			((JMenuItem) menuOptions.nextElement()).setEnabled(true);

			// set other menu items
			while (menuOptions.hasMoreElements())
				((JMenuItem) menuOptions.nextElement()).setEnabled(flag);
		}
	}

	public void setSearchLayoutButtonsEnabled() {

		Enumeration menuOptions = this.searchLayoutGroup.getElements();

		// first one is always ON
		((JMenuItem) menuOptions.nextElement()).setEnabled(true);

		// set other menu items
		while (menuOptions.hasMoreElements()) {
			((JMenuItem) menuOptions.nextElement()).setEnabled(true);
		}
	}

	public void alignLayoutMenuOption(int mode) {

		// reset rotation
		// rotation=0;
		// setSelectedComboIndex(Commands.ROTATION,0);

		int i = 1;

		Enumeration menuOptions = this.layoutGroup.getElements();

		// cycle to correct value
		while (menuOptions.hasMoreElements() && i != mode) {
			menuOptions.nextElement();
			i++;
		}

		// choose item
		((JMenuItem) menuOptions.nextElement()).setSelected(true);
	}

	public void setDisplayMode(Integer mode) {

		if (mode.equals(GUIFactory.MULTIPAGE)) this.isSingle = false;
	}

	public boolean isSingle() {
		return this.isSingle;
	}

	public Object getThumbnailPanel() {
		return this.thumbnails;
	}

	public Object getOutlinePanel() {
		return this.tree;
	}

	public JScrollBar getVerticalScrollBar() {
		if (this.scrollPane.getVerticalScrollBar().isVisible()) {
			return this.scrollPane.getVerticalScrollBar();
		}
		else return this.thumbscroll;
	}

	/** used when clicking on thumbnails to move onto new page */
	private class PageChanger implements ActionListener {

		int page;

		public PageChanger(int i) {
			i++;
			this.page = i;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if ((!Values.isProcessing()) && (SwingGUI.this.commonValues.getCurrentPage() != this.page)) {

				// if loading on linearized thread, see if we can actually display
				if (!SwingGUI.this.decode_pdf.isPageAvailable(this.page)) {
					showMessageDialog("Page " + this.page + " is not yet loaded");
					return;
				}
				// commonValues.setCurrentPage(page);
				//
				SwingGUI.this.statusBar.resetStatus("");
				//
				// //setScalingToDefault();
				//
				// //decode_pdf.setPageParameters(getScaling(), commonValues.getCurrentPage());
				//
				// decodePage(false);
				SwingGUI.this.currentCommands.gotoPage(Integer.toString(this.page));
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.jpedal.examples.viewer.gui.swing.GUIFactory#initLayoutMenus(javax.swing.JMenu, java.lang.String[], int[])
	 */
	@Override
	public void initLayoutMenus(JMenu pageLayout, String[] descriptions, int[] value) {

		int count = value.length;
		for (int i = 0; i < count; i++) {
			JCheckBoxMenuItem pageView = new JCheckBoxMenuItem(descriptions[i]);
			pageView.setBorder(BorderFactory.createEmptyBorder());
			this.layoutGroup.add(pageView);
			if (i == 0) pageView.setSelected(true);

			if (pageLayout != null) {

				switch (value[i]) {
					case Display.SINGLE_PAGE:
						this.single = pageView;
						this.single.addActionListener(new ActionListener() {
							@Override
							public void actionPerformed(ActionEvent e) {
								SwingGUI.this.currentCommands.executeCommand(Commands.SINGLE, null);
							}
						});
						pageLayout.add(this.single);
						break;
					case Display.CONTINUOUS:
						this.continuous = pageView;
						this.continuous.addActionListener(new ActionListener() {
							@Override
							public void actionPerformed(ActionEvent e) {
								SwingGUI.this.currentCommands.executeCommand(Commands.CONTINUOUS, null);
							}
						});
						pageLayout.add(this.continuous);
						break;
					case Display.FACING:
						this.facing = pageView;
						this.facing.addActionListener(new ActionListener() {
							@Override
							public void actionPerformed(ActionEvent e) {
								SwingGUI.this.currentCommands.executeCommand(Commands.FACING, null);
							}
						});
						pageLayout.add(this.facing);
						break;
					case Display.CONTINUOUS_FACING:
						this.continuousFacing = pageView;
						this.continuousFacing.addActionListener(new ActionListener() {
							@Override
							public void actionPerformed(ActionEvent e) {
								SwingGUI.this.currentCommands.executeCommand(Commands.CONTINUOUS_FACING, null);
							}
						});
						pageLayout.add(this.continuousFacing);
						break;
				}
			}
		}

		if (!this.isSingle) return;

		// default is off
		setPageLayoutButtonsEnabled(false);
	}

	/**
	 * show fonts on system displayed
	 */
	private static JScrollPane getFontsAliasesInfoBox() {

		JPanel details = new JPanel();

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setPreferredSize(new Dimension(400, 300));
		scrollPane.getViewport().add(details);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		details.setOpaque(true);
		details.setBackground(Color.white);
		details.setEnabled(false);
		details.setLayout(new BoxLayout(details, BoxLayout.PAGE_AXIS));

		/**
		 * list of all fonts fonts
		 */
		StringBuilder fullList = new StringBuilder();

		for (Object nextFont : FontMappings.fontSubstitutionAliasTable.keySet()) {
			fullList.append(nextFont);
			fullList.append(" ==> ");
			fullList.append(FontMappings.fontSubstitutionAliasTable.get(nextFont));
			fullList.append('\n');
		}

		String xmlText = fullList.toString();
		if (xmlText.length() > 0) {

			JTextArea xml = new JTextArea();
			xml.setLineWrap(false);
			xml.setText(xmlText);
			details.add(xml);
			xml.setCaretPosition(0);
			xml.setOpaque(false);

			details.add(Box.createRigidArea(new Dimension(0, 5)));
		}

		return scrollPane;
	}

	// Font tree Display pane
	JScrollPane fontScrollPane = new JScrollPane();

	boolean sortFontsByDir = true;

	// <link><a name="fontdetails" />
	/**
	 * show fonts on system displayed
	 */
	private JPanel getFontsFoundInfoBox() {

		// Create font list display area
		JPanel fontDetails = new JPanel(new BorderLayout());
		fontDetails.setBackground(Color.WHITE);
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBackground(Color.WHITE);

		this.fontScrollPane.setBackground(Color.WHITE);
		this.fontScrollPane.getViewport().setBackground(Color.WHITE);
		this.fontScrollPane.setPreferredSize(new Dimension(400, 300));
		this.fontScrollPane.getViewport().add(fontDetails);
		this.fontScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		this.fontScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		// This allows the title to be centered above the filter box
		JPanel filterTitlePane = new JPanel();
		filterTitlePane.setBackground(Color.WHITE);
		JLabel filterTitle = new JLabel("Filter Font List");
		filterTitlePane.add(filterTitle);

		// Create buttons
		ButtonGroup bg = new ButtonGroup();
		JRadioButton folder = new JRadioButton("Sort By Folder");
		folder.setBackground(Color.WHITE);
		JRadioButton name = new JRadioButton("Sort By Name");
		name.setBackground(Color.WHITE);
		final JTextField filter = new JTextField();

		// Ensure correct display mode selected
		if (this.sortFontsByDir == true) folder.setSelected(true);
		else name.setSelected(true);

		bg.add(folder);
		bg.add(name);
		JPanel buttons = new JPanel(new BorderLayout());
		buttons.setBackground(Color.WHITE);
		buttons.add(filterTitlePane, BorderLayout.NORTH);
		buttons.add(folder, BorderLayout.WEST);
		buttons.add(filter, BorderLayout.CENTER);
		buttons.add(name, BorderLayout.EAST);

		folder.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (!SwingGUI.this.sortFontsByDir) {
					DefaultMutableTreeNode fontlist = new DefaultMutableTreeNode("Fonts");
					SwingGUI.this.sortFontsByDir = true;
					fontlist = populateAvailableFonts(fontlist, filter.getText());
					displayAvailableFonts(fontlist);
				}
			}
		});

		name.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (SwingGUI.this.sortFontsByDir) {
					DefaultMutableTreeNode fontlist = new DefaultMutableTreeNode("Fonts");
					SwingGUI.this.sortFontsByDir = false;
					fontlist = populateAvailableFonts(fontlist, filter.getText());
					displayAvailableFonts(fontlist);
				}
			}
		});

		filter.addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(KeyEvent e) {}

			@Override
			public void keyReleased(KeyEvent e) {
				DefaultMutableTreeNode fontlist = new DefaultMutableTreeNode("Fonts");
				populateAvailableFonts(fontlist, ((JTextField) e.getSource()).getText());
				displayAvailableFonts(fontlist);
			}

			@Override
			public void keyTyped(KeyEvent e) {}
		});

		// Start tree here
		DefaultMutableTreeNode top = new DefaultMutableTreeNode("Fonts");

		// Populate font list and build tree
		top = populateAvailableFonts(top, null);
		JTree fontTree = new JTree(top);
		// Added to keep the tree left aligned when top parent is closed
		fontDetails.add(fontTree, BorderLayout.WEST);

		// Peice it all together
		panel.add(buttons, BorderLayout.NORTH);
		panel.add(this.fontScrollPane, BorderLayout.CENTER);
		panel.setPreferredSize(new Dimension(400, 300));

		return panel;
	}

	private void displayAvailableFonts(DefaultMutableTreeNode fontlist) {

		// Remove old font tree display panel
		this.fontScrollPane.getViewport().removeAll();

		// Create new font list display
		JPanel jp = new JPanel(new BorderLayout());
		jp.setBackground(Color.WHITE);
		jp.add(new JTree(fontlist), BorderLayout.WEST);

		// Show font tree
		this.fontScrollPane.getViewport().add(jp);
	}

	/**
	 * list of all fonts properties in sorted order
	 */
	private DefaultMutableTreeNode populateAvailableFonts(DefaultMutableTreeNode top, String filter) {

		// get list
		if (FontMappings.fontSubstitutionTable != null) {
			Set fonts = FontMappings.fontSubstitutionTable.keySet();
			Iterator fontList = FontMappings.fontSubstitutionTable.keySet().iterator();

			int fontCount = fonts.size();
			ArrayList<String> fontNames = new ArrayList<String>(fontCount);

			while (fontList.hasNext())
				fontNames.add(fontList.next().toString());

			// sort
			Collections.sort(fontNames);

			// Sort and Display Fonts by Directory
			if (this.sortFontsByDir) {

				List<String> Location = new ArrayList<String>();
				List<DefaultMutableTreeNode> LocationNode = new ArrayList<DefaultMutableTreeNode>();

				// build display
				for (int ii = 0; ii < fontCount; ii++) {
					Object nextFont = fontNames.get(ii);

					String current = (FontMappings.fontSubstitutionLocation.get(nextFont));

					int ptr = current.lastIndexOf(System.getProperty("file.separator"));
					if (ptr == -1 && current.indexOf('/') != -1) ptr = current.lastIndexOf('/');

					if (ptr != -1) current = current.substring(0, ptr);

					if (filter == null || ((String) nextFont).toLowerCase().contains(filter.toLowerCase())) {
						if (!Location.contains(current)) {
							Location.add(current);
							DefaultMutableTreeNode loc = new DefaultMutableTreeNode(new DefaultMutableTreeNode(current));
							top.add(loc);
							LocationNode.add(loc);
						}

						DefaultMutableTreeNode FontTop = new DefaultMutableTreeNode(nextFont + " = "
								+ FontMappings.fontSubstitutionLocation.get(nextFont));
						int pos = Location.indexOf(current);
						LocationNode.get(pos).add(FontTop);

						// add details
						String loc = (String) FontMappings.fontPropertiesTable.get(nextFont + "_path");
						Integer type = (Integer) FontMappings.fontPropertiesTable.get(nextFont + "_type");

						Map properties = StandardFonts.getFontDetails(type, loc);
						if (properties != null) {

							for (Object key : properties.keySet()) {
								Object value = properties.get(key);

								// JLabel fontString=new JLabel(key+" = "+value);
								// fontString.setFont(new Font("Lucida",Font.PLAIN,10));
								// details.add(fontString);
								DefaultMutableTreeNode FontDetails = new DefaultMutableTreeNode(key + " = " + value);
								FontTop.add(FontDetails);

							}
						}
					}
				}
			}
			else {// Show all fonts in one list

				// build display
				for (int ii = 0; ii < fontCount; ii++) {
					Object nextFont = fontNames.get(ii);

					if (filter == null || ((String) nextFont).toLowerCase().contains(filter.toLowerCase())) {
						DefaultMutableTreeNode FontTop = new DefaultMutableTreeNode(nextFont + " = "
								+ FontMappings.fontSubstitutionLocation.get(nextFont));
						top.add(FontTop);

						// add details
						Map properties = (Map) FontMappings.fontPropertiesTable.get(nextFont);
						if (properties != null) {

							for (Object key : properties.keySet()) {
								Object value = properties.get(key);

								// JLabel fontString=new JLabel(key+" = "+value);
								// fontString.setFont(new Font("Lucida",Font.PLAIN,10));
								// details.add(fontString);
								DefaultMutableTreeNode FontDetails = new DefaultMutableTreeNode(key + " = " + value);
								FontTop.add(FontDetails);

							}
						}
					}
				}
			}
		}
		return top;
	}

	/**
	 * show fonts displayed
	 */
	private JScrollPane getFontInfoBox() {

		JPanel details = new JPanel();

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setPreferredSize(new Dimension(400, 300));
		scrollPane.getViewport().add(details);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		details.setOpaque(true);
		details.setBackground(Color.white);
		details.setEnabled(false);
		details.setLayout(new BoxLayout(details, BoxLayout.PAGE_AXIS));

		/**
		 * list of fonts
		 */
		String xmlTxt = this.decode_pdf.getInfo(PdfDictionary.Font);
		String xmlText = "Font Substitution mode: ";

		switch (FontMappings.getFontSubstitutionMode()) {
			case (1):
				xmlText = xmlText + "using file name";
				break;
			case (2):
				xmlText = xmlText + "using PostScript name";
				break;
			case (3):
				xmlText = xmlText + "using family name";
				break;
			case (4):
				xmlText = xmlText + "using the full font name";
				break;
			default:
				xmlText = xmlText + "Unknown FontSubstitutionMode";
				break;
		}

		xmlText = xmlText + '\n';

		if (xmlTxt.length() > 0) {

			JTextArea xml = new JTextArea();
			JLabel mode = new JLabel();

			mode.setAlignmentX(Component.CENTER_ALIGNMENT);
			mode.setText(xmlText);
			mode.setForeground(Color.BLUE);

			xml.setLineWrap(false);
			xml.setForeground(Color.BLACK);
			xml.setText('\n' + xmlTxt);

			details.add(mode);
			details.add(xml);

			xml.setCaretPosition(0);
			xml.setOpaque(false);

			// details.add(Box.createRigidArea(new Dimension(0,5)));
		}

		return scrollPane;
	}

	/**
	 * show fonts displayed
	 */
	private JScrollPane getImageInfoBox() {

		/**
		 * the generic panel details
		 */
		JPanel details = new JPanel();

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setPreferredSize(new Dimension(400, 300));
		scrollPane.getViewport().add(details);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		details.setOpaque(true);
		details.setBackground(Color.white);
		details.setEnabled(false);
		details.setLayout(new BoxLayout(details, BoxLayout.PAGE_AXIS));

		/**
		 * list of Images (not forms)
		 */
		String xmlTxt = this.decode_pdf.getInfo(PdfDictionary.Image);

		// and display in container
		if (xmlTxt.length() > 0) {

			JTextArea xml = new JTextArea();

			xml.setLineWrap(false);
			xml.setForeground(Color.BLACK);
			xml.setText('\n' + xmlTxt);

			details.add(xml);

			xml.setCaretPosition(0);
			xml.setOpaque(false);

			// details.add(Box.createRigidArea(new Dimension(0,5)));
		}

		return scrollPane;
	}

	/*
	 * (non-Javadoc)
	 * @see org.jpedal.examples.viewer.gui.swing.GUIFactory#getInfoBox()
	 */
	@Override
	public void getInfoBox() {

		// <start-wrap>
		final JPanel details = new JPanel();
		details.setPreferredSize(new Dimension(400, 260));
		details.setOpaque(false);
		details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));

		// general details
		JLabel header1 = new JLabel(Messages.getMessage("PdfViewerInfo.title"));
		header1.setOpaque(false);
		header1.setFont(this.headFont);
		header1.setAlignmentX(Component.CENTER_ALIGNMENT);
		details.add(header1);

		details.add(Box.createRigidArea(new Dimension(0, 15)));

		String xmlText = Messages.getMessage("PdfViewerInfo1");
		if (xmlText.length() > 0) {

			JTextArea xml = new JTextArea();
			xml.setFont(this.textFont1);
			xml.setOpaque(false);
			xml.setText(xmlText + "\n\nVersions\n JPedal: " + PdfDecoder.version + "          " + "Java: " + System.getProperty("java.version"));
			xml.setLineWrap(true);
			xml.setWrapStyleWord(true);
			xml.setEditable(false);
			details.add(xml);
			xml.setAlignmentX(Component.CENTER_ALIGNMENT);

		}

		ImageIcon logo = new ImageIcon(getClass().getResource("/org/jpedal/examples/viewer/res/logo.gif"));
		details.add(Box.createRigidArea(new Dimension(0, 25)));
		JLabel idr = new JLabel(logo);
		idr.setAlignmentX(Component.CENTER_ALIGNMENT);
		details.add(idr);

		final JLabel url = new JLabel("<html><center>" + Messages.getMessage("PdfViewerJpedalLibrary.Text")
				+ Messages.getMessage("PdfViewer.WebAddress"));
		url.setForeground(Color.blue);
		url.setHorizontalAlignment(SwingConstants.CENTER);
		url.setAlignmentX(Component.CENTER_ALIGNMENT);

		// @kieran - cursor
		url.addMouseListener(new MouseListener() {

			@Override
			public void mouseEntered(MouseEvent e) {
				if (SingleDisplay.allowChangeCursor) details.setCursor(new Cursor(Cursor.HAND_CURSOR));
				url.setText("<html><center>" + Messages.getMessage("PdfViewerJpedalLibrary.Link")
						+ Messages.getMessage("PdfViewerJpedalLibrary.Text") + Messages.getMessage("PdfViewer.WebAddress") + "</a></center>");
			}

			@Override
			public void mouseExited(MouseEvent e) {
				if (SingleDisplay.allowChangeCursor) details.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
				url.setText("<html><center>" + Messages.getMessage("PdfViewerJpedalLibrary.Text") + Messages.getMessage("PdfViewer.WebAddress"));
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				try {
					BrowserLauncher.openURL(Messages.getMessage("PdfViewer.VisitWebsite"));
				}
				catch (IOException e1) {
					showMessageDialog(Messages.getMessage("PdfViewer.ErrorWebsite"));
				}
			}

			@Override
			public void mousePressed(MouseEvent e) {}

			@Override
			public void mouseReleased(MouseEvent e) {}
		});

		details.add(url);
		details.add(Box.createRigidArea(new Dimension(0, 5)));

		details.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		showMessageDialog(details, Messages.getMessage("PdfViewerInfo3"), JOptionPane.PLAIN_MESSAGE);

		/**
		 * //<end-wrap>
		 * 
		 * final JPanel details=new JPanel(); details.setPreferredSize(new Dimension(400,260)); details.setOpaque(false); details.setLayout(new
		 * BoxLayout(details, BoxLayout.Y_AXIS));
		 * 
		 * //general details JLabel header1=new JLabel("PDF Java Ebook Solution"); header1.setOpaque(false); header1.setFont(headFont);
		 * header1.setAlignmentX(Component.CENTER_ALIGNMENT); details.add(header1);
		 * 
		 * details.add(Box.createRigidArea(new Dimension(0,15)));
		 * 
		 * String xmlText="This ebook was generated using the free PJES service offered by IDRSolutions. Go to our website to try it out yourself!";
		 * if(xmlText.length()>0){
		 * 
		 * JTextArea xml=new JTextArea(); xml.setFont(textFont1); xml.setOpaque(false); xml.setText(xmlText +
		 * "\n\nVersions\n PJES: "+PdfDecoder.version + "          " + "Java: " + System.getProperty("java.version")); xml.setLineWrap(true);
		 * xml.setWrapStyleWord(true); xml.setEditable(false); details.add(xml); xml.setAlignmentX(Component.CENTER_ALIGNMENT);
		 * 
		 * }
		 * 
		 * ImageIcon logo=new ImageIcon(getClass().getResource("/org/jpedal/examples/viewer/res/logo.gif")); details.add(Box.createRigidArea(new
		 * Dimension(0,25))); JLabel idr=new JLabel(logo); idr.setAlignmentX(Component.CENTER_ALIGNMENT); details.add(idr);
		 * 
		 * final JLabel url=new JLabel("<html><center>PJES from www.idrsolutions.com"); url.setForeground(Color.blue);
		 * url.setHorizontalAlignment(JLabel.CENTER); url.setAlignmentX(Component.CENTER_ALIGNMENT);
		 * 
		 * //@kieran - cursor url.addMouseListener(new MouseListener() { public void mouseEntered(MouseEvent e) { details.setCursor(new
		 * Cursor(Cursor.HAND_CURSOR));
		 * url.setText("<html><center><a href=\"http://www.idrsolutions.com\">PJES from www.idrsolutions.com</a></center>"); }
		 * 
		 * public void mouseExited(MouseEvent e) { details.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		 * url.setText("<html><center>PJES from www.idrsolutions.com"); }
		 * 
		 * public void mouseClicked(MouseEvent e) { try { BrowserLauncher.openURL("http://www.idrsolutions.com"); } catch (IOException e1) {
		 * showMessageDialog(Messages.getMessage("PdfViewer.ErrorWebsite")); } }
		 * 
		 * public void mousePressed(MouseEvent e) {} public void mouseReleased(MouseEvent e) {} });
		 * 
		 * details.add(url); details.add(Box.createRigidArea(new Dimension(0,5)));
		 * 
		 * details.setBorder(BorderFactory.createEmptyBorder(10,10,10,10)); showMessageDialog(details,"About",JOptionPane.PLAIN_MESSAGE); /
		 **/
	}

	/*
	 * (non-Javadoc)
	 * @see org.jpedal.examples.viewer.gui.swing.GUIFactory#resetRotationBox()
	 */
	@Override
	public void resetRotationBox() {

		PdfPageData currentPageData = this.decode_pdf.getPdfPageData();

		// >>> DON'T UNCOMMENT THIS LINE, causes major rotation issues, only useful for debuging <<<
		if (this.decode_pdf.getDisplayView() == Display.SINGLE_PAGE) {
			this.rotation = currentPageData.getRotation(this.commonValues.getCurrentPage());
		}
		// else
		// rotation=0;

		if (getSelectedComboIndex(Commands.ROTATION) != (this.rotation / 90)) {
			setSelectedComboIndex(Commands.ROTATION, (this.rotation / 90));
		}
		else
			if (!Values.isProcessing()) {
				this.decode_pdf.repaint();
			}
	}

	/**
	 * show document properties
	 */
	private JScrollPane getPropertiesBox(String file, String path, String user_dir, long size, int pageCount, int currentPage) {

		PdfFileInformation currentFileInformation = this.decode_pdf.getFileInformationData();

		/** get the Pdf file information object to extract info from */
		if (currentFileInformation != null) {

			JPanel details = new JPanel();
			details.setOpaque(true);
			details.setBackground(Color.white);
			details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));

			JScrollPane scrollPane = new JScrollPane();
			scrollPane.setPreferredSize(new Dimension(400, 300));
			scrollPane.getViewport().add(details);
			scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
			scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

			// general details
			JLabel header1 = new JLabel(Messages.getMessage("PdfViewerGeneral"));
			header1.setFont(this.headFont);
			header1.setOpaque(false);
			details.add(header1);

			JLabel g1 = new JLabel(Messages.getMessage("PdfViewerFileName") + file);
			g1.setFont(this.textFont);
			g1.setOpaque(false);
			details.add(g1);

			JLabel g2 = new JLabel(Messages.getMessage("PdfViewerFilePath") + path);
			g2.setFont(this.textFont);
			g2.setOpaque(false);
			details.add(g2);

			JLabel g3 = new JLabel(Messages.getMessage("PdfViewerCurrentWorkingDir") + ' ' + user_dir);
			g3.setFont(this.textFont);
			g3.setOpaque(false);
			details.add(g3);

			JLabel g4 = new JLabel(Messages.getMessage("PdfViewerFileSize") + size + " K");
			g4.setFont(this.textFont);
			g4.setOpaque(false);
			details.add(g4);

			JLabel g5 = new JLabel(Messages.getMessage("PdfViewerPageCount") + pageCount);
			g5.setOpaque(false);
			g5.setFont(this.textFont);
			details.add(g5);

			String g6Text = "PDF " + this.decode_pdf.getPDFVersion();

			// add in if Linearized
			if (this.decode_pdf.getJPedalObject(PdfDictionary.Linearized) != null) g6Text = g6Text + " ("
					+ Messages.getMessage("PdfViewerLinearized.text") + ") ";

			JLabel g6 = new JLabel(g6Text);
			g6.setOpaque(false);
			g6.setFont(this.textFont);
			details.add(g6);

			details.add(Box.createVerticalStrut(10));

			// general details
			JLabel header2 = new JLabel(Messages.getMessage("PdfViewerProperties"));
			header2.setFont(this.headFont);
			header2.setOpaque(false);
			details.add(header2);

			// get the document properties
			String[] values = currentFileInformation.getFieldValues();
			String[] fields = PdfFileInformation.getFieldNames();

			// add to list and display
			int count = fields.length;

			JLabel[] displayValues = new JLabel[count];

			for (int i = 0; i < count; i++) {
				if (values[i].length() > 0) {

					displayValues[i] = new JLabel(fields[i] + " = " + values[i]);
					displayValues[i].setFont(this.textFont);
					displayValues[i].setOpaque(false);
					details.add(displayValues[i]);
				}
			}

			details.add(Box.createVerticalStrut(10));

			/**
			 * get the Pdf file information object to extract info from
			 */
			PdfPageData currentPageSize = this.decode_pdf.getPdfPageData();

			if (currentPageSize != null) {

				// general details
				JLabel header3 = new JLabel(Messages.getMessage("PdfViewerCoords.text"));
				header3.setFont(this.headFont);
				details.add(header3);

				JLabel g7 = new JLabel(Messages.getMessage("PdfViewermediaBox.text") + currentPageSize.getMediaValue(currentPage));
				g7.setFont(this.textFont);
				details.add(g7);

				JLabel g8 = new JLabel(Messages.getMessage("PdfViewercropBox.text") + currentPageSize.getCropValue(currentPage));
				g8.setFont(this.textFont);
				details.add(g8);

				JLabel g9 = new JLabel(Messages.getMessage("PdfViewerLabel.Rotation") + currentPageSize.getRotation(currentPage));
				g9.setFont(this.textFont);
				details.add(g9);

			}

			details.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

			return scrollPane;

		}
		else {
			return new JScrollPane();
		}
	}

	/**
	 * page info option
	 */
	private static JScrollPane getXMLInfoBox(String xmlText) {

		JPanel details = new JPanel();
		details.setLayout(new BoxLayout(details, BoxLayout.PAGE_AXIS));

		details.setOpaque(true);
		details.setBackground(Color.white);

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setPreferredSize(new Dimension(400, 300));
		scrollPane.getViewport().add(details);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		JTextArea xml = new JTextArea();

		xml.setRows(5);
		xml.setColumns(15);
		xml.setLineWrap(true);
		xml.setText(xmlText);
		details.add(new JScrollPane(xml));
		xml.setCaretPosition(0);
		xml.setOpaque(true);
		xml.setBackground(Color.white);

		return scrollPane;
	}

	/*
	 * (non-Javadoc)
	 * @see org.jpedal.examples.viewer.gui.swing.GUIFactory#showDocumentProperties(java.lang.String, java.lang.String, long, int, int)
	 */
	@Override
	public void showDocumentProperties(String selectedFile, String inputDir, long size, int pageCount, int currentPage) {
		JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.setBackground(Color.WHITE);

		if (selectedFile == null) {

			showMessageDialog(Messages.getMessage("PdfVieweremptyFile.message"), Messages.getMessage("PdfViewerTooltip.pageSize"),
					JOptionPane.PLAIN_MESSAGE);
		}
		else {

			String filename = selectedFile;

			int ptr = filename.lastIndexOf('\\');
			if (ptr == -1) ptr = filename.lastIndexOf('/');

			String file = filename.substring(ptr + 1, filename.length());

			String path = filename.substring(0, ptr + 1);

			int ii = 0;
			tabbedPane.add(getPropertiesBox(file, path, this.user_dir, size, pageCount, currentPage));
			tabbedPane.setTitleAt(ii++, Messages.getMessage("PdfViewerTab.Properties"));

			tabbedPane.add(getFontInfoBox());
			tabbedPane.setTitleAt(ii++, Messages.getMessage("PdfViewerTab.Fonts"));

			if (org.jpedal.parser.ImageCommands.trackImages) {
				tabbedPane.add(getImageInfoBox());
				tabbedPane.setTitleAt(ii++, Messages.getMessage("PdfViewerTab.Images"));
			}

			tabbedPane.add(getFontsFoundInfoBox());
			tabbedPane.setTitleAt(ii++, "Available");

			tabbedPane.add(getFontsAliasesInfoBox());
			tabbedPane.setTitleAt(ii++, "Aliases");

			int nextTab = ii;

			/**
			 * add form details if applicable
			 */
			JScrollPane scroll = getFormList();

			if (scroll != null) {
				tabbedPane.add(scroll);
				tabbedPane.setTitleAt(nextTab, "Forms");
				nextTab++;
			}

			/**
			 * optional tab for new XML style info
			 */
			PdfFileInformation currentFileInformation = this.decode_pdf.getFileInformationData();
			String xmlText = currentFileInformation.getFileXMLMetaData();
			if (xmlText.length() > 0) {
				tabbedPane.add(getXMLInfoBox(xmlText));
				tabbedPane.setTitleAt(nextTab, "XML");
			}

			showMessageDialog(tabbedPane, Messages.getMessage("PdfViewerTab.DocumentProperties"), JOptionPane.PLAIN_MESSAGE);
		}
	}

	/**
	 * provide list of forms
	 */
	private JScrollPane getFormList() {

		JScrollPane scroll = null;

		// get the form renderer
		org.jpedal.objects.acroforms.rendering.AcroRenderer formRenderer = this.decode_pdf.getFormRenderer();

		if (formRenderer != null) {

			// get list of forms on page
			java.util.List formsOnPage = null;

			try {
				formsOnPage = formRenderer.getComponentNameList(this.commonValues.getCurrentPage());
			}
			catch (PdfException e) {

				LogWriter.writeLog("Exception " + e + " reading component list");
			}

			// allow for no forms
			if (formsOnPage != null) {

				int formCount = formsOnPage.size();

				JPanel formPanel = new JPanel();

				scroll = new JScrollPane();
				scroll.setPreferredSize(new Dimension(400, 300));
				scroll.getViewport().add(formPanel);
				scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
				scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

				/**
				 * create a JPanel to list forms and popup details
				 */

				formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
				JLabel formHeader = new JLabel("This page contains " + formCount + " form objects");
				formHeader.setFont(this.headFont);
				formPanel.add(formHeader);

				formPanel.add(Box.createRigidArea(new Dimension(10, 10)));

				/** sort form names in alphabetical order */
				Collections.sort(formsOnPage);

				// get FormRenderer and Data objects
				AcroRenderer renderer = this.decode_pdf.getFormRenderer();
				if (renderer == null) return scroll;
				GUIData formData = renderer.getCompData();

				/**
				 * populate our list with details
				 */
				for (Object aFormsOnPage : formsOnPage) {

					// get name of form
					String formName = (String) aFormsOnPage;

					// swing component we map data into
					Component[] comp = (Component[]) formRenderer.getComponentsByName(formName);

					if (comp != null) {

						// number of components - may be several child items
						// int count = comp.length;

						// take value or first if array to check for types (will be same if children)
						FormObject formObj = null;

						// extract list of actual PDF references to display and get FormObject
						String PDFrefs = "PDF ref=";

						JLabel ref = new JLabel();

						// actual data read from PDF
						Object[] rawFormData = formData.getRawForm(formName);
						for (Object aRawFormData : rawFormData) {
							formObj = (FormObject) aRawFormData;
							PDFrefs = PDFrefs + ' ' + formObj.getObjectRefAsString();
						}

						ref.setText(PDFrefs);

						/** display the form component description */
						// int formComponentType = ((Integer) formData.getTypeValueByName(formName)).intValue();

						String formDescription = formName;
						JLabel header = new JLabel(formDescription);

						JLabel type = new JLabel();
						type.setText("Type=" + PdfDictionary.showAsConstant(formObj.getParameterConstant(PdfDictionary.Type)) + " Subtype="
								+ PdfDictionary.showAsConstant(formObj.getParameterConstant(PdfDictionary.Subtype)));

						/** get the current Swing component type */
						String standardDetails = "java class=" + comp[0].getClass();

						JLabel details = new JLabel(standardDetails);

						header.setFont(this.headFont);
						header.setForeground(Color.blue);

						type.setFont(this.textFont);
						type.setForeground(Color.blue);

						details.setFont(this.textFont);
						details.setForeground(Color.blue);

						ref.setFont(this.textFont);
						ref.setForeground(Color.blue);

						formPanel.add(header);
						formPanel.add(type);
						formPanel.add(details);
						formPanel.add(ref);

						/**
						 * not currently used or setup JButton more = new JButton("View Form Data"); more.setFont(textFont);
						 * more.setForeground(Color.blue);
						 * 
						 * more.addActionListener(new ShowFormDataListener(formName)); formPanel.add(more);
						 * 
						 * formPanel.add(new JLabel(" "));
						 * 
						 * /
						 **/
					}
				}
			}
		}

		return scroll;
	}

	/**
	 * display form data in popup
	 * 
	 * private class ShowFormDataListener implements ActionListener{
	 * 
	 * private String formName;
	 * 
	 * public ShowFormDataListener(String formName){ this.formName=formName; }
	 * 
	 * public void actionPerformed(ActionEvent e) {
	 * 
	 * 
	 * //will return Object or Object[] if multiple items of same name Object[]
	 * values=decode_pdf.getFormRenderer().getCompData().getRawForm(formName);
	 * 
	 * int count=values.length;
	 * 
	 * JTabbedPane valueDisplay=new JTabbedPane(); for(int jj=0;jj<count;jj++){
	 * 
	 * FormObject form=(FormObject)values[jj];
	 * 
	 * if(values[jj]!=null){ String data=form.toString(); JTextArea text=new JTextArea(); text.setText(data); text.setWrapStyleWord(true);
	 * 
	 * JScrollPane scroll=new JScrollPane(); scroll.setPreferredSize(new Dimension(400,300)); scroll.getViewport().add(text);
	 * scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
	 * scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
	 * 
	 * valueDisplay.add(form.getObjectRefAsString(),scroll); } }
	 * 
	 * JOptionPane.showMessageDialog(getFrame(), valueDisplay,"Raw Form Data",JOptionPane.OK_OPTION); }
	 * 
	 * }/
	 **/

	GUISearchWindow searchFrame = null;
	boolean addSearchTab = false;
	boolean searchInMenu = false;

	/*
	 * Set Search Bar to be in the Left hand Tabbed pane
	 */
	public void searchInTab(GUISearchWindow searchFrame) {
		this.searchFrame = searchFrame;

		this.searchFrame.init(this.decode_pdf, this.commonValues);

		if (DecoderOptions.isRunningOnMac) {
			if (this.thumbnails.isShownOnscreen()) this.navOptionsPanel.addTab("Search", searchFrame.getContentPanel());
		}
		else {
			VTextIcon textIcon2 = new VTextIcon(this.navOptionsPanel, "Search", VTextIcon.ROTATE_LEFT);
			this.navOptionsPanel.addTab(null, textIcon2, searchFrame.getContentPanel());
		}
		this.addSearchTab = true;
	}

	JTextField searchText = null;
	SearchList results = null;
	public Commands currentCommands;

	JToggleButton options;
	JPopupMenu menu;

	private JToggleButton createMenuBarSearchOptions() {
		if (this.options == null) {
			this.options = new JToggleButton(new ImageIcon(getURLForImage(this.iconLocation + "menuSearchOptions.png")));
			this.menu = new JPopupMenu();

			this.options.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent e) {
					if (e.getStateChange() == ItemEvent.SELECTED) {
						SwingGUI.this.menu.show(((JComponent) e.getSource()), 0, ((JComponent) e.getSource()).getHeight());
					}
				}
			});
			this.options.setFocusable(false);
			this.options.setToolTipText(Messages.getMessage("PdfViewerSearch.Options"));
			// wholeWordsOnlyBox, caseSensitiveBox, multiLineBox, highlightAll

			// JMenuItem openFull = new JMenuItem("Open Full Search Window");
			// openFull.addActionListener(new ActionListener(){
			// public void actionPerformed(ActionEvent e) {
			//
			// }
			// });

			JCheckBoxMenuItem wholeWords = new JCheckBoxMenuItem(Messages.getMessage("PdfViewerSearch.WholeWords"));
			wholeWords.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					SwingGUI.this.searchFrame.setWholeWords(((JCheckBoxMenuItem) e.getSource()).isSelected());
				}
			});

			JCheckBoxMenuItem caseSense = new JCheckBoxMenuItem(Messages.getMessage("PdfViewerSearch.CaseSense"));
			caseSense.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					SwingGUI.this.searchFrame.setCaseSensitive(((JCheckBoxMenuItem) e.getSource()).isSelected());
				}
			});

			JCheckBoxMenuItem multiLine = new JCheckBoxMenuItem(Messages.getMessage("PdfViewerSearch.MultiLine"));
			multiLine.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					SwingGUI.this.searchFrame.setMultiLine(((JCheckBoxMenuItem) e.getSource()).isSelected());
				}
			});

			// menu.add(openFull);
			// menu.addSeparator();
			this.menu.add(wholeWords);
			this.menu.add(caseSense);
			this.menu.add(multiLine);

			this.menu.addPopupMenuListener(new PopupMenuListener() {
				@Override
				public void popupMenuWillBecomeVisible(PopupMenuEvent e) {}

				@Override
				public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
					SwingGUI.this.options.setSelected(false);
				}

				@Override
				public void popupMenuCanceled(PopupMenuEvent e) {
					SwingGUI.this.options.setSelected(false);
				}
			});
		}

		return this.options;
	}

	/*
	 * Set Search Bar to be in the Top Button Bar
	 */
	public void searchInMenu(GUISearchWindow searchFrame) {
		this.searchFrame = searchFrame;
		this.searchInMenu = true;
		searchFrame.find(this.decode_pdf, this.commonValues);
		// searchText.setPreferredSize(new Dimension(150,20));
		this.topButtons.add(this.searchText);
		this.topButtons.add(createMenuBarSearchOptions());
		addButton(GUIFactory.BUTTONBAR, Messages.getMessage("PdfViewerSearch.Previous"), this.iconLocation + "search_previous.gif",
				Commands.PREVIOUSRESULT);
		addButton(GUIFactory.BUTTONBAR, Messages.getMessage("PdfViewerSearch.Next"), this.iconLocation + "search_next.gif", Commands.NEXTRESULT);

		this.nextSearch.setVisible(false);
		this.previousSearch.setVisible(false);
	}

	public Commands getCommand() {
		return this.currentCommands;
	}

	public void clearRecentDocuments() {
		this.currentCommands.clearRecentDocuments();
	}

	private String iconLocation = "/org/jpedal/examples/viewer/res/";

	/**
	 * Pass a document listener to the page counter to watch for changes to the page number. This value is updated when the page is altered.
	 * 
	 * @param docListener
	 *            :: A document listener to listen for changes to page number. New page number can be found in the insertUpdate method using
	 *            DocumentEvent.getDocument().getText(int offset, int length)
	 */
	public void addPageChangeListener(DocumentListener docListener) {
		if (this.pageCounter2 != null) this.pageCounter2.getDocument().addDocumentListener(docListener);
	}

	/*
	 * (non-Javadoc)
	 * @see org.jpedal.examples.viewer.gui.swing.GUIFactory#init(java.lang.String[], org.jpedal.examples.viewer.Commands,
	 * org.jpedal.examples.viewer.utils.Printer)
	 */
	@Override
	public void init(String[] scalingValues, final Object currentCommands, Object currentPrinter) {

		// setup custom message and switch off error messages if used
		this.customMessageHandler = (CustomMessageHandler) (this.decode_pdf.getExternalHandler(Options.CustomMessageOutput));
		if (this.customMessageHandler != null) {
			PdfDecoder.showErrorMessages = false;
			Viewer.showMessages = false;
		}

		/**
		 * Set up from properties
		 */
		try {
			// Set viewer page inset
			String propValue = this.properties.getValue("pageInsets");
			if (propValue.length() > 0) inset = Integer.parseInt(propValue);

			// Set whether to use hinting
			propValue = this.properties.getValue("useHinting");
			String propValue2 = System.getProperty("org.jpedal.useTTFontHinting");

			// check JVM flag first
			if (propValue2 != null) {
				// check if properties file conflicts
				if (propValue.length() > 0 && !propValue2.toLowerCase().equals(propValue.toLowerCase())) JOptionPane.showMessageDialog(null,
						Messages.getMessage("PdfCustomGui.hintingFlagFileConflict"));

				if (propValue2.toLowerCase().equals("true")) TTGlyph.useHinting = true;
				else TTGlyph.useHinting = false;

				// check properties file
			}
			else
				if (propValue.length() > 0 && propValue.toLowerCase().equals("true")) TTGlyph.useHinting = true;
				else TTGlyph.useHinting = false;

			propValue = this.properties.getValue("changeTextAndLineart");
			if (propValue.length() > 0 && propValue.toLowerCase().equals("true")) ((Commands) currentCommands).executeCommand(Commands.CHANGELINEART,
					new Object[] { Boolean.parseBoolean(propValue) });

			propValue = this.properties.getValue("vbgColor");
			if (propValue.length() > 0) ((Commands) currentCommands).executeCommand(Commands.SETPAGECOLOR,
					new Object[] { Integer.parseInt(propValue) });

			propValue = this.properties.getValue("sbbgColor");
			if (propValue.length() > 0) {
				// @KIERAN - This is a place holder for when this becomes active
			}// replaceDocCol

			propValue = this.properties.getValue("replaceDocumentTextColors");
			if (propValue.length() > 0 && propValue.toLowerCase().equals("true")) {

				propValue = this.properties.getValue("vfgColor");
				if (propValue.length() > 0) ((Commands) currentCommands).executeCommand(Commands.SETTEXTCOLOR,
						new Object[] { Integer.parseInt(propValue) });

			}

			propValue = this.properties.getValue("TextColorThreshold");
			if (propValue.length() > 0) ((Commands) currentCommands).executeCommand(Commands.SETREPLACEMENTCOLORTHRESHOLD,
					new Object[] { Integer.parseInt(propValue) });

			// Set icon location
			propValue = this.properties.getValue("iconLocation");
			if (propValue.length() > 0) {
				this.iconLocation = propValue;
			}

			// Set border config value and repaint
			propValue = this.properties.getValue("borderType");
			if (propValue.length() > 0) SingleDisplay.CURRENT_BORDER_STYLE = Integer.parseInt(propValue);

			// Set autoScroll default and add to properties file
			propValue = this.properties.getValue("autoScroll");
			if (propValue.length() > 0) this.allowScrolling = Boolean.getBoolean(propValue);

			// set confirmClose
			propValue = this.properties.getValue("confirmClose");
			if (propValue.length() > 0) this.confirmClose = propValue.equals("true");

			// Dpi is taken into effect when zoom is called
			propValue = this.properties.getValue("resolution");
			if (propValue.length() > 0) this.decode_pdf.getDPIFactory().setDpi(Integer.parseInt(propValue));

			// Allow cursor to change
			propValue = this.properties.getValue("allowCursorToChange");
			if (propValue.length() > 0) if (propValue.toLowerCase().equals("true")) SingleDisplay.allowChangeCursor = true;
			else SingleDisplay.allowChangeCursor = false;

			// @kieran Ensure valid value if not recognised
			propValue = this.properties.getValue("pageMode");
			if (propValue.length() > 0) {
				int pageMode = Integer.parseInt(propValue);
				if (pageMode < Display.SINGLE_PAGE || pageMode > Display.PAGEFLOW) pageMode = Display.SINGLE_PAGE;
				// Default Page Layout
				this.decode_pdf.setPageMode(pageMode);
			}

			propValue = this.properties.getValue("maxmuliviewers");
			if (propValue.length() > 0) this.commonValues.setMaxMiltiViewers(Integer.parseInt(propValue));

			propValue = this.properties.getValue("showDownloadWindow");
			if (propValue.length() > 0) this.useDownloadWindow = Boolean.valueOf(propValue);

			propValue = this.properties.getValue("useHiResPrinting");
			if (propValue.length() > 0) this.hiResPrinting = Boolean.valueOf(propValue);

			// @kieran - in this code, it will break if we add new value for all users.
			// could we recode these all defensively so change one below to
			String val = this.properties.getValue("highlightBoxColor"); // empty string to old users
			if (val.length() > 0) DecoderOptions.highlightColor = new Color(Integer.parseInt(val));

			// how it is at moment
			// PdfDecoder.highlightColor = new Color(Integer.parseInt(properties.getValue("highlightBoxColor")));

			// //////////////////////////
			propValue = this.properties.getValue("highlightTextColor");
			if (propValue.length() > 0) DecoderOptions.backgroundColor = new Color(Integer.parseInt(propValue));

			propValue = this.properties.getValue("invertHighlights");
			if (propValue.length() > 0) BaseDisplay.invertHighlight = Boolean.valueOf(propValue);

			propValue = this.properties.getValue("showMouseSelectionBox");
			if (propValue.length() > 0) PdfDecoder.showMouseBox = Boolean.valueOf(propValue);

			propValue = this.properties.getValue("enhancedViewerMode");
			if (propValue.length() > 0) this.decode_pdf.useNewGraphicsMode = Boolean.valueOf(propValue);

			propValue = this.properties.getValue("enhancedFacingMode");
			if (propValue.length() > 0) SingleDisplay.default_turnoverOn = Boolean.valueOf(propValue);

			propValue = this.properties.getValue("enhancedGUI");
			if (propValue.length() > 0) this.useNewLayout = Boolean.valueOf(propValue);

			propValue = this.properties.getValue("highlightComposite");
			if (propValue.length() > 0) {
				float value = Float.parseFloat(propValue);
				if (value > 1) value = 1;
				if (value < 0) value = 0;
				PdfDecoder.highlightComposite = value;
			}

			propValue = this.properties.getValue("windowTitle");
			if (propValue.length() > 0) {
				windowTitle = propValue;
			}
			else {
				windowTitle = Messages.getMessage("PdfViewer.titlebar") + ' ' + PdfDecoder.version;
			}

		}
		catch (Exception e) {
			e.printStackTrace();
		}
		this.currentCommands = (Commands) currentCommands;

		/**
		 * single listener to execute all commands
		 */
		this.currentCommandListener = new CommandListener((Commands) currentCommands);

		/**
		 * set a title
		 */
		setViewerTitle(windowTitle);
		if (this.frame instanceof JFrame) {

			// Check if file location provided
			URL path = getURLForImage(this.iconLocation + "icon.png");
			if (path != null) {
				try {
					BufferedImage fontIcon = ImageIO.read(path);
					((JFrame) this.frame).setIconImage(fontIcon);
				}
				catch (Exception e) {}
			}
		}

		/** arrange insets */
		this.decode_pdf.setInset(inset, inset);

		// Add Background color to the panel to help break up view
		String propValue = this.properties.getValue("replacePdfDisplayBackground");
		if (propValue.length() > 0 && propValue.toLowerCase().equals("true")) {
			// decode_pdf.useNewGraphicsMode = false;
			propValue = this.properties.getValue("pdfDisplayBackground");
			if (propValue.length() > 0) ((Commands) currentCommands).executeCommand(Commands.SETDISPLAYBACKGROUND,
					new Object[] { Integer.parseInt(propValue) });

			this.decode_pdf.setBackground(new Color(Integer.parseInt(propValue)));

		}
		else {
			if (this.decode_pdf.getDecoderOptions().getDisplayBackgroundColor() != null) this.decode_pdf.setBackground(this.decode_pdf
					.getDecoderOptions().getDisplayBackgroundColor());
			else
				if (this.decode_pdf.useNewGraphicsMode) this.decode_pdf.setBackground(new Color(55, 55, 65));
				else this.decode_pdf.setBackground(new Color(190, 190, 190));
		}
		/**
		 * setup combo boxes
		 */

		// set new default if appropriate
		String choosenScaling = System.getProperty("org.jpedal.defaultViewerScaling");

		// <start-wrap>
		/**
		 * //<end-wrap> choosenScaling="${pjesScaling}"; /
		 **/

		if (choosenScaling != null) {
			int total = scalingValues.length;
			for (int aa = 0; aa < total; aa++) {
				if (scalingValues[aa].equals(choosenScaling)) {
					defaultSelection = aa;
					aa = total;
				}
			}
		}

		this.scalingBox = new SwingCombo(scalingValues);
		this.scalingBox.setBackground(Color.white);
		this.scalingBox.setEditable(true);
		this.scalingBox.setSelectedIndex(defaultSelection); // set default before we add a listener

		// if you enable, remember to change rotation and quality Comboboxes
		// scalingBox.setPreferredSize(new Dimension(85,25));

		this.rotationBox = new SwingCombo(this.rotationValues);
		this.rotationBox.setBackground(Color.white);
		this.rotationBox.setSelectedIndex(0); // set default before we add a listener

		/**
		 * add the pdf display to show page
		 **/
		JPanel containerForThumbnails = new JPanel();
		if (this.isSingle) {
			this.previewOnSingleScroll = this.properties.getValue("previewOnSingleScroll").toLowerCase().equals("true");
			if (this.previewOnSingleScroll) {
				this.thumbscroll = new JScrollBar(Adjustable.VERTICAL, 0, 1, 0, 1);

				if (this.scrollListener == null) {
					this.scrollListener = new ScrollListener();
					this.scrollMouseListener = new ScrollMouseListener();
				}
				this.thumbscroll.addAdjustmentListener(this.scrollListener);
				this.thumbscroll.addMouseListener(this.scrollMouseListener);
				// thumbscroll.addMouseMotionListener(scrollMouseListener);

				containerForThumbnails.setLayout(new BorderLayout());
				containerForThumbnails.add(this.thumbscroll, BorderLayout.EAST);
				this.scrollPane.getViewport().add(this.decode_pdf);
				containerForThumbnails.add(this.scrollPane, BorderLayout.CENTER);

				this.scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
				this.scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
			}
			else {
				this.scrollPane.getViewport().add(this.decode_pdf);

				this.scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
				this.scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

			}

			this.scrollPane.getVerticalScrollBar().setUnitIncrement(80);
			this.scrollPane.getHorizontalScrollBar().setUnitIncrement(80);

			// Keyboard control of next/previous page
			this.decode_pdf.addKeyListener(new KeyAdapter() {
				int count = 0;

				@Override
				public void keyPressed(KeyEvent e) {
					final JScrollBar scroll = SwingGUI.this.scrollPane.getVerticalScrollBar();

					if (this.count == 0) {
						if (e.getKeyCode() == KeyEvent.VK_UP && scroll.getValue() == scroll.getMinimum() && getCurrentPage() > 1) {

							// change page
							((Commands) currentCommands).executeCommand(Commands.BACKPAGE, null);

							// update scrollbar so at bottom of page
							SwingUtilities.invokeLater(new Runnable() {
								@Override
								public void run() {
									scroll.setValue(scroll.getMaximum());
								}
							});

						}
						else
							if (e.getKeyCode() == KeyEvent.VK_DOWN
									&& (scroll.getValue() == scroll.getMaximum() - scroll.getHeight() || scroll.getHeight() == 0)
									&& getCurrentPage() < SwingGUI.this.decode_pdf.getPageCount()) {

								// change page
								((Commands) currentCommands).executeCommand(Commands.FORWARDPAGE, null);

								// update scrollbar so at top of page
								SwingUtilities.invokeLater(new Runnable() {
									@Override
									public void run() {
										scroll.setValue(scroll.getMinimum());
									}
								});
							}
					}

					this.count++;
				}

				@Override
				public void keyReleased(KeyEvent e) {
					this.count = 0;
				}
			});

		}

		this.comboBoxBar.setBorder(BorderFactory.createEmptyBorder());
		this.comboBoxBar.setLayout(new FlowLayout(FlowLayout.LEADING));
		this.comboBoxBar.setFloatable(false);
		this.comboBoxBar.setFont(new Font("SansSerif", Font.PLAIN, 8));

		if (this.isSingle) {
			/**
			 * Create a left-right split pane with tabs and add to main display
			 */
			this.navOptionsPanel.setTabPlacement(SwingConstants.LEFT);
			this.navOptionsPanel.setOpaque(true);
			this.navOptionsPanel.setMinimumSize(new Dimension(0, 100));
			this.navOptionsPanel.setName("NavPanel");

			this.pageTitle = Messages.getMessage("PdfViewerJPanel.thumbnails");
			this.bookmarksTitle = Messages.getMessage("PdfViewerJPanel.bookmarks");
			this.layersTitle = Messages.getMessage("PdfViewerJPanel.layers");
			this.signaturesTitle = "Signatures";

			if (this.previewOnSingleScroll) this.displayPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, this.navOptionsPanel,
					containerForThumbnails);
			else this.displayPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, this.navOptionsPanel, this.scrollPane);

			this.displayPane.setOneTouchExpandable(false);

			// update scaling when divider moved
			this.displayPane.addPropertyChangeListener("dividerLocation", new PropertyChangeListener() {
				@Override
				public void propertyChange(PropertyChangeEvent e) {

					// hack to get it to use current values instead of old values
					SwingGUI.this.scrollPane.getViewport().setSize(
							(SwingGUI.this.scrollPane.getViewport().getWidth() + (Integer) e.getOldValue() - (Integer) e.getNewValue()),
							SwingGUI.this.scrollPane.getViewport().getHeight());

					SwingGUI.this.desktopPane.setSize((SwingGUI.this.desktopPane.getWidth() + (Integer) e.getOldValue() - (Integer) e.getNewValue()),
							SwingGUI.this.desktopPane.getHeight());

					zoom(false);
				}
			});

			if (DecoderOptions.isRunningOnMac) {
				this.navOptionsPanel.addTab(this.pageTitle, (Component) this.thumbnails);
				this.navOptionsPanel.setTitleAt(this.navOptionsPanel.getTabCount() - 1, this.pageTitle);

				if (this.thumbnails.isShownOnscreen()) {
					this.navOptionsPanel.addTab(this.bookmarksTitle, (SwingOutline) this.tree);
					this.navOptionsPanel.setTitleAt(this.navOptionsPanel.getTabCount() - 1, this.bookmarksTitle);
				}

			}
			else {

				if (this.thumbnails.isShownOnscreen()) {
					VTextIcon textIcon1 = new VTextIcon(this.navOptionsPanel, this.pageTitle, VTextIcon.ROTATE_LEFT);
					this.navOptionsPanel.addTab(null, textIcon1, (Component) this.thumbnails);

					// navOptionsPanel.setTitleAt(navOptionsPanel.getTabCount()-1, pageTitle);
				}

				VTextIcon textIcon2 = new VTextIcon(this.navOptionsPanel, this.bookmarksTitle, VTextIcon.ROTATE_LEFT);
				this.navOptionsPanel.addTab(null, textIcon2, (SwingOutline) this.tree);
				// navOptionsPanel.setTitleAt(navOptionsPanel.getTabCount()-1, bookmarksTitle);

			}

			// p.setTabDefaults(defaultValues);

			this.displayPane.setDividerLocation(startSize);

			if (!this.hasListener) {
				this.hasListener = true;
				this.navOptionsPanel.addMouseListener(new MouseListener() {

					@Override
					public void mouseClicked(MouseEvent mouseEvent) {
						handleTabbedPanes();
					}

					@Override
					public void mousePressed(MouseEvent mouseEvent) {
						// To change body of implemented methods use File | Settings | File Templates.
					}

					@Override
					public void mouseReleased(MouseEvent mouseEvent) {
						// To change body of implemented methods use File | Settings | File Templates.
					}

					@Override
					public void mouseEntered(MouseEvent mouseEvent) {
						// To change body of implemented methods use File | Settings | File Templates.
					}

					@Override
					public void mouseExited(MouseEvent mouseEvent) {
						// To change body of implemented methods use File | Settings | File Templates.
					}
				});

			}
		}

		/**
		 * setup global buttons
		 */
		// if(!commonValues.isContentExtractor()){
		this.first = new SwingButton();
		this.fback = new SwingButton();
		this.back = new SwingButton();
		this.forward = new SwingButton();
		this.fforward = new SwingButton();
		this.end = new SwingButton();

		// }

		this.snapshotButton = new SwingButton();

		this.singleButton = new SwingButton();
		this.continuousButton = new SwingButton();
		this.continuousFacingButton = new SwingButton();
		this.facingButton = new SwingButton();

		this.pageFlowButton = new SwingButton();

		this.openButton = new SwingButton();
		this.printButton = new SwingButton();
		this.searchButton = new SwingButton();
		this.docPropButton = new SwingButton();
		this.infoButton = new SwingButton();
		this.mouseMode = new SwingButton();

		this.previousSearch = new SwingButton();
		this.nextSearch = new SwingButton();

		/**
		 * set colours on display boxes and add listener to page number
		 */
		this.pageCounter2.setEditable(true);
		this.pageCounter2.setToolTipText(Messages.getMessage("PdfViewerTooltip.goto"));
		this.pageCounter2.setBorder(BorderFactory.createLineBorder(Color.black));
		this.pageCounter2.setColumns(4);
		this.pageCounter2.setMaximumSize(this.pageCounter2.getPreferredSize());

		this.pageCounter2.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {

				String value = SwingGUI.this.pageCounter2.getText().trim();

				((Commands) currentCommands).gotoPage(value);
			}

		});
		this.pageCounter2.setHorizontalAlignment(SwingConstants.CENTER);
		this.pageCounter2.setForeground(Color.black);
		setPageNumber();

		this.pageCounter3 = new JLabel(Messages.getMessage("PdfViewerOfLabel.text") + ' ');
		this.pageCounter3.setOpaque(false);

		/**
		 * create a menu bar and add to display
		 */
		JPanel top = new JPanel();
		top.setLayout(new BorderLayout());
		if (this.frame instanceof JFrame) ((JFrame) this.frame).getContentPane().add(top, BorderLayout.NORTH);
		else this.frame.add(top, BorderLayout.NORTH);

		/** nav bar at bottom to select pages and setup Toolbar on it */

		// navToolBar.setLayout(new FlowLayout());
		this.navToolBar.setLayout(new BoxLayout(this.navToolBar, BoxLayout.LINE_AXIS));
		this.navToolBar.setFloatable(false);

		// pagesToolBar.setLayout(new FlowLayout());
		this.pagesToolBar.setFloatable(false);

		this.navButtons.setBorder(BorderFactory.createEmptyBorder());
		this.navButtons.setLayout(new BorderLayout());
		this.navButtons.setFloatable(false);
		// comboBar.setFont(new Font("SansSerif", Font.PLAIN, 8));
		this.navButtons.setPreferredSize(new Dimension(5, 24));

		/**
		 * setup menu and create options
		 */
		top.add(this.currentMenu, BorderLayout.NORTH);

		/**
		 * create other tool bars and add to display
		 */
		this.topButtons.setBorder(BorderFactory.createEmptyBorder());
		this.topButtons.setLayout(new FlowLayout(FlowLayout.LEADING));
		this.topButtons.setFloatable(false);
		this.topButtons.setFont(new Font("SansSerif", Font.PLAIN, 8));

		top.add(this.topButtons, BorderLayout.CENTER);

		if (!this.useNewLayout) {
			/**
			 * zoom,scale,rotation, status,cursor
			 */
			top.add(this.comboBoxBar, BorderLayout.SOUTH);
		}

		if (this.frame instanceof JFrame) ((JFrame) this.frame).getContentPane().add(this.navButtons, BorderLayout.SOUTH);
		else this.frame.add(this.navButtons, BorderLayout.SOUTH);

		if (this.displayPane != null) { // null in MultiViewer
			if (this.frame instanceof JFrame) ((JFrame) this.frame).getContentPane().add(this.displayPane, BorderLayout.CENTER);
			else this.frame.add(this.displayPane, BorderLayout.CENTER);

		}

		/**
		 * Menu bar for using the majority of functions
		 */
		createMainMenu(true);

		// createSwingMenu(true);

		/**
		 * sets up all the toolbar items
		 */
		// <start-wrap>
		addButton(GUIFactory.BUTTONBAR, Messages.getMessage("PdfViewerToolbarTooltip.openFile"), this.iconLocation + "open.gif", Commands.OPENFILE);
		// <end-wrap>

		if (this.searchFrame != null && this.searchFrame.getStyle() == SwingSearchWindow.SEARCH_EXTERNAL_WINDOW) addButton(GUIFactory.BUTTONBAR,
				Messages.getMessage("PdfViewerToolbarTooltip.search"), this.iconLocation + "find.gif", Commands.FIND);

		// <start-wrap>
		addButton(GUIFactory.BUTTONBAR, Messages.getMessage("PdfViewerToolbarTooltip.properties"), this.iconLocation + "properties.gif",
				Commands.DOCINFO);
		// <end-wrap>

		if (!this.useNewLayout || this.commonValues.getModeOfOperation() == Values.RUNNING_PLUGIN) addButton(GUIFactory.BUTTONBAR,
				Messages.getMessage("PdfViewerToolbarTooltip.about"), this.iconLocation + "about.gif", Commands.INFO);

		// <start-wrap>
		/** snapshot screen function */
		addButton(GUIFactory.BUTTONBAR, Messages.getMessage("PdfViewerToolbarTooltip.snapshot"), this.iconLocation + "snapshot.gif",
				Commands.SNAPSHOT);
		// <end-wrap>

		if (this.useNewLayout) {
			JSeparator sep = new JSeparator(SwingConstants.VERTICAL);
			sep.setPreferredSize(new Dimension(5, 32));
			this.topButtons.add(sep);
		}

		/**
		 * combo boxes on toolbar
		 * */
		addCombo(Messages.getMessage("PdfViewerToolbarScaling.text"), Messages.getMessage("PdfViewerToolbarTooltip.zoomin"), Commands.SCALING);

		addCombo(Messages.getMessage("PdfViewerToolbarRotation.text"), Messages.getMessage("PdfViewerToolbarTooltip.rotation"), Commands.ROTATION);

		// <start-wrap>
		// <end-wrap>

		addButton(GUIFactory.BUTTONBAR, Messages.getMessage("PdfViewerToolbarTooltip.mouseMode"), this.iconLocation + "mouse_select.png",
				Commands.MOUSEMODE);

		if (this.useNewLayout) {

			JSeparator sep = new JSeparator(SwingConstants.VERTICAL);
			sep.setPreferredSize(new Dimension(5, 32));
			this.topButtons.add(sep);
			// <start-wrap>
			/**
			 * //<end-wrap>
			 * addButton(GUIFactory.BUTTONBAR,Messages.getMessage("PdfViewerToolbarTooltip.about"),iconLocation+"about.gif",Commands.INFO); /
			 **/
		}

		/**
		 * navigation toolbar for moving between pages
		 */
		createNavbar();

		// <start-wrap>
		addCursor();
		// <end-wrap>

		// p.setButtonDefaults(defaultValues);

		// <link><a name="newbutton" />
		/**
		 * external/itext button option example adding new option to Export menu an icon is set wtih location on classpath
		 * "/org/jpedal/examples/viewer/res/newfunction.gif" Make sure it exists at location and is copied into jar if recompiled
		 */
		// currentGUI.addButton(currentGUI.BUTTONBAR,tooltip,"/org/jpedal/examples/viewer/res/newfunction.gif",Commands.NEWFUNCTION);

		/**
		 * external/itext menu option example adding new option to Export menu Tooltip text can be externalised in
		 * Messages.getMessage("PdfViewerTooltip.NEWFUNCTION") and text added into files in res package
		 */

		if (this.searchFrame != null && this.searchFrame.getStyle() == SwingSearchWindow.SEARCH_MENU_BAR) searchInMenu(this.searchFrame);

		/** status object on toolbar showing 0 -100 % completion */
		initStatus();

		// p.setDisplayDefaults(defaultValues);

		// Ensure all gui sections are displayed correctly
		// Issues found when removing some sections
		this.frame.invalidate();
		this.frame.validate();
		this.frame.repaint();

		/**
		 * Load properties
		 */
		try {
			loadProperties();
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		/**
		 * set display to occupy half screen size and display, add listener and make sure appears in centre
		 */
		if (this.commonValues.getModeOfOperation() != Values.RUNNING_APPLET) {

			Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
			int width = d.width / 2, height = d.height / 2;
			if (width < minimumScreenWidth) width = minimumScreenWidth;

			// allow user to alter size
			String customWindowSize = System.getProperty("org.jpedal.startWindowSize");
			if (customWindowSize != null) {

				StringTokenizer values = new StringTokenizer(customWindowSize, "x");

				System.out.println(values.countTokens());
				if (values.countTokens() != 2) throw new RuntimeException("Unable to use value for org.jpedal.startWindowSize=" + customWindowSize
						+ "\nValue should be in format org.jpedal.startWindowSize=200x300");

				try {
					width = Integer.parseInt(values.nextToken().trim());
					height = Integer.parseInt(values.nextToken().trim());

				}
				catch (Exception ee) {
					throw new RuntimeException("Unable to use value for org.jpedal.startWindowSize=" + customWindowSize
							+ "\nValue should be in format org.jpedal.startWindowSize=200x300");
				}
			}

			if (this.frame instanceof JFrame) {
				this.frame.setSize(width, height);
				((JFrame) this.frame).setLocationRelativeTo(null); // centre on screen
				((JFrame) this.frame).setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
				((JFrame) this.frame).addWindowListener(new FrameCloser((Commands) currentCommands, this, this.decode_pdf, (Printer) currentPrinter,
						this.thumbnails, this.commonValues, this.properties));
				this.frame.setVisible(true);
			}
		}

		/** Ensure Document is redrawn when frame is resized and scaling set to width, height or window */
		this.frame.addComponentListener(new ComponentListener() {
			@Override
			public void componentHidden(ComponentEvent e) {}

			@Override
			public void componentMoved(ComponentEvent e) {}

			@Override
			public void componentResized(ComponentEvent e) {
				if (SwingGUI.this.decode_pdf.getParent() != null
						&& (getSelectedComboIndex(Commands.SCALING) < 3 || SwingGUI.this.decode_pdf.getDisplayView() == Display.FACING)) // always
																																			// rezoom
																																			// in
																																			// facing
																																			// mode
																																			// for
																																			// turnover
				zoom(false);
			}

			@Override
			public void componentShown(ComponentEvent e) {}
		});

		// add a border
		if (this.decode_pdf.useNewGraphicsMode) {
			this.decode_pdf.setPDFBorder(new AbstractBorder() {

				private static final long serialVersionUID = -5113144261774718512L;

				@Override
				public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
					Graphics2D g2 = (Graphics2D) g;

					int cornerDepth = (SwingGUI.this.glowThickness / 2) + 1;

					// left
					g2.setPaint(new GradientPaint(x - SwingGUI.this.glowThickness, 0, SwingGUI.this.glowOuterColor, x, 0,
							SwingGUI.this.glowInnerColor));
					g2.fillRect(x - SwingGUI.this.glowThickness, y, SwingGUI.this.glowThickness, height);

					// bottom left corner
					g2.setPaint(new GradientPaint(x - cornerDepth, y + height + cornerDepth, SwingGUI.this.glowOuterColor, x, y + height,
							SwingGUI.this.glowInnerColor));
					g2.fillRect(x - SwingGUI.this.glowThickness, y + height, SwingGUI.this.glowThickness, SwingGUI.this.glowThickness);

					// below
					g2.setPaint(new GradientPaint(0, y + height + SwingGUI.this.glowThickness, SwingGUI.this.glowOuterColor, 0, y + height,
							SwingGUI.this.glowInnerColor));
					g2.fillRect(x, y + height, width, SwingGUI.this.glowThickness);

					// bottom right corner
					g2.setPaint(new GradientPaint(x + width + cornerDepth, y + height + cornerDepth, SwingGUI.this.glowOuterColor, x + width, y
							+ height, SwingGUI.this.glowInnerColor));
					g2.fillRect(x + width, y + height, SwingGUI.this.glowThickness, SwingGUI.this.glowThickness);

					// right
					g2.setPaint(new GradientPaint(x + width + SwingGUI.this.glowThickness, 0, SwingGUI.this.glowOuterColor, x + width, 0,
							SwingGUI.this.glowInnerColor));
					g2.fillRect(x + width, y, SwingGUI.this.glowThickness, height);

					// top right corner
					g2.setPaint(new GradientPaint(x + width + cornerDepth, y - cornerDepth, SwingGUI.this.glowOuterColor, x + width, y,
							SwingGUI.this.glowInnerColor));
					g2.fillRect(x + width, y - SwingGUI.this.glowThickness, SwingGUI.this.glowThickness, SwingGUI.this.glowThickness);

					// above
					g2.setPaint(new GradientPaint(0, y - SwingGUI.this.glowThickness, SwingGUI.this.glowOuterColor, 0, y,
							SwingGUI.this.glowInnerColor));
					g2.fillRect(x, y - SwingGUI.this.glowThickness, width, SwingGUI.this.glowThickness);

					// top left corner
					g2.setPaint(new GradientPaint(x - cornerDepth, y - cornerDepth, SwingGUI.this.glowOuterColor, x, y, SwingGUI.this.glowInnerColor));
					g2.fillRect(x - SwingGUI.this.glowThickness, y - SwingGUI.this.glowThickness, SwingGUI.this.glowThickness,
							SwingGUI.this.glowThickness);

					// draw black over top
					g2.setPaint(Color.black);
					g2.drawRect(x, y, width, height);

				}

				@Override
				public Insets getBorderInsets(Component c, Insets insets) {
					insets.set(SwingGUI.this.glowThickness, SwingGUI.this.glowThickness, SwingGUI.this.glowThickness, SwingGUI.this.glowThickness);
					return insets;
				}
			});

		}
		else {
			this.decode_pdf.setPDFBorder(BorderFactory.createLineBorder(Color.black, 1));
		}
	}

	public PdfDecoder getPdfDecoder() {
		return this.decode_pdf;
	}

	private void handleTabbedPanes() {

		if (this.tabsNotInitialised) return;

		/**
		 * expand size if not already at size
		 */
		int currentSize = this.displayPane.getDividerLocation();
		int tabSelected = this.navOptionsPanel.getSelectedIndex();

		if (tabSelected == -1) return;

		if (currentSize == startSize) {
			/**
			 * workout selected tab
			 */
			// String tabName="";
			// if(DecoderOptions.isRunningOnMac){
			// tabName=navOptionsPanel.getTitleAt(tabSelected);
			// }else
			// tabName=navOptionsPanel.getIconAt(tabSelected).toString();

			// if(tabName.equals(pageTitle)){
			// add if statement or comment out this section to remove thumbnails
			setupThumbnailPanel();

			// }else if(tabName.equals(bookmarksTitle)){
			setBookmarks(true);
			// }

			// if(searchFrame!=null)
			// searchFrame.find();

			this.displayPane.setDividerLocation(expandedSize);
		}
		else
			if (tabSelected == this.lastTabSelected) this.displayPane.setDividerLocation(startSize);

		this.lastTabSelected = tabSelected;
	}

	/*
	 * (non-Javadoc)
	 * @see org.jpedal.examples.viewer.gui.swing.GUIFactory#addCursor()
	 */
	@Override
	public void addCursor() {

		/** add cursor location */
		if (!this.useNewLayout) {
			this.cursor.setBorder(BorderFactory.createEmptyBorder());
			this.cursor.setLayout(new FlowLayout(FlowLayout.LEADING));
			this.cursor.setFloatable(false);
			this.cursor.setFont(new Font("SansSerif", Font.ITALIC, 10));
			this.cursor.add(new JLabel(Messages.getMessage("PdfViewerToolbarCursorLoc.text")));

			this.cursor.add(initCoordBox());

			this.cursor.setPreferredSize(new Dimension(200, 32));

			/** setup cursor */
			this.topButtons.add(this.cursor);
		}
		else {
			initCoordBox();
		}
	}

	private boolean cursorOverPage = false;

	public void setMultibox(int[] flags) {

		// deal with flags
		if (flags.length > 1 && flags[0] == CURSOR) {
			// if no change, return
			if (this.cursorOverPage != (flags[1] == 1)) this.cursorOverPage = flags[1] == 1;
			else return;
		}

		// LOAD_PROGRESS:
		if (this.statusBar.isEnabled() && this.statusBar.isVisible() && !this.statusBar.isDone()) {
			this.multibox.removeAll();
			this.statusBar.getStatusObject().setSize(this.multibox.getSize());
			this.multibox.add(this.statusBar.getStatusObject(), BorderLayout.CENTER);

			this.multibox.repaint();
			return;
		}

		// CURSOR:
		if (this.cursor.isEnabled() && this.cursor.isVisible() && this.cursorOverPage && this.decode_pdf.isOpen()) {
			this.multibox.removeAll();
			this.multibox.add(this.coords, BorderLayout.CENTER);

			this.multibox.repaint();
			return;
		}

		// DOWNLOAD_PROGRESS:
		if (this.downloadBar.isEnabled() && this.downloadBar.isVisible() && !this.downloadBar.isDone()
				&& (this.decode_pdf.isLoadingLinearizedPDF() || !this.decode_pdf.isOpen())) {
			this.multibox.removeAll();
			this.downloadBar.getStatusObject().setSize(this.multibox.getSize());
			this.multibox.add(this.downloadBar.getStatusObject(), BorderLayout.CENTER);

			this.multibox.repaint();
			return;
		}

		// MEMORY:
		if (this.memoryBar.isEnabled() && this.memoryBar.isVisible()) {
			this.multibox.removeAll();
			this.memoryBar.setSize(this.multibox.getSize());
			this.memoryBar.setForeground(new Color(125, 145, 255));
			this.multibox.add(this.memoryBar, BorderLayout.CENTER);

			this.multibox.repaint();
			return;
		}
	}

	/** setup keyboard shortcuts */
	private void setKeyAccelerators(int ID, JMenuItem menuItem) {

		int systemMask = java.awt.Event.CTRL_MASK;
		if (DecoderOptions.isRunningOnMac) {
			systemMask = java.awt.Event.META_MASK;
		}

		switch (ID) {

			case Commands.FIND:
				menuItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F, systemMask));
				break;

			case Commands.SAVE:
				menuItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, systemMask));
				break;
			case Commands.PRINT:
				menuItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_P, systemMask));
				break;
			case Commands.EXIT:
				menuItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Q, systemMask));
				break;
			case Commands.DOCINFO:
				menuItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_D, systemMask));
				break;
			case Commands.OPENFILE:
				menuItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, systemMask));
				break;
			case Commands.OPENURL:
				menuItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_U, systemMask));
				break;
			case Commands.PREVIOUSDOCUMENT:
				menuItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_LEFT, InputEvent.ALT_MASK | InputEvent.SHIFT_MASK));
				break;
			case Commands.NEXTDOCUMENT:
				menuItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_RIGHT, InputEvent.ALT_MASK | InputEvent.SHIFT_MASK));
				break;
			case Commands.FIRSTPAGE:
				menuItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_HOME, systemMask));
				break;
			case Commands.BACKPAGE:
				menuItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_UP, InputEvent.CTRL_MASK));
				break;
			case Commands.FORWARDPAGE:
				menuItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DOWN, InputEvent.CTRL_MASK));
				break;
			case Commands.LASTPAGE:
				menuItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_END, systemMask));
				break;
			case Commands.GOTO:
				menuItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_N, systemMask | InputEvent.SHIFT_MASK));
				break;
			case Commands.BITMAP:
				menuItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_B, InputEvent.ALT_MASK));
				break;
			case Commands.COPY:
				menuItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, systemMask));
				break;
			case Commands.SELECTALL:
				menuItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_A, systemMask));
				break;
			case Commands.DESELECTALL:
				menuItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_A, systemMask + InputEvent.SHIFT_DOWN_MASK));
				break;
			case Commands.PREFERENCES:
				menuItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_K, systemMask));
				break;

		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.jpedal.examples.viewer.gui.swing.GUIFactory#addButton(int, java.lang.String, java.lang.String, int)
	 */
	@Override
	public void addButton(int line, String toolTip, String path, final int ID) {

		GUIButton newButton = new SwingButton();

		/** specific buttons */
		switch (ID) {

			case Commands.FIRSTPAGE:
				newButton = this.first;
				break;
			case Commands.FBACKPAGE:
				newButton = this.fback;
				break;
			case Commands.BACKPAGE:
				newButton = this.back;
				break;
			case Commands.FORWARDPAGE:
				newButton = this.forward;
				break;
			case Commands.FFORWARDPAGE:
				newButton = this.fforward;
				break;
			case Commands.LASTPAGE:
				newButton = this.end;
				break;
			case Commands.SNAPSHOT:
				newButton = this.snapshotButton;
				break;
			case Commands.SINGLE:
				newButton = this.singleButton;
				newButton.setName("SINGLE");
				break;
			case Commands.CONTINUOUS:
				newButton = this.continuousButton;
				newButton.setName("CONTINUOUS");
				break;
			case Commands.CONTINUOUS_FACING:
				newButton = this.continuousFacingButton;
				newButton.setName("CONTINUOUS_FACING");
				break;
			case Commands.FACING:
				newButton = this.facingButton;
				newButton.setName("FACING");
				break;
			case Commands.PAGEFLOW:
				newButton = this.pageFlowButton;
				newButton.setName("PAGEFLOW");
				break;
			case Commands.PREVIOUSRESULT:
				newButton = this.previousSearch;
				newButton.setName("PREVIOUSRESULT");
				break;
			case Commands.NEXTRESULT:
				newButton = this.nextSearch;
				newButton.setName("NEXTRESULT");
				break;
			case Commands.OPENFILE:
				newButton = this.openButton;
				newButton.setName("open");
				break;
			case Commands.PRINT:
				newButton = this.printButton;
				newButton.setName("print");
				break;
			case Commands.FIND:
				newButton = this.searchButton;
				newButton.setName("search");
				break;
			case Commands.DOCINFO:
				newButton = this.docPropButton;
				break;
			case Commands.INFO:
				newButton = this.infoButton;
				break;
			case Commands.MOUSEMODE:
				newButton = this.mouseMode;
				newButton.setName("mousemode");
				break;
		}

		// @kieran : This may be a good idea. See how you feel when time to commit.
		((SwingButton) newButton).addMouseListener(new MouseListener() {
			@Override
			public void mouseClicked(MouseEvent e) {}

			@Override
			public void mousePressed(MouseEvent e) {}

			@Override
			public void mouseReleased(MouseEvent e) {}

			@Override
			public void mouseEntered(MouseEvent e) {
				if (SingleDisplay.allowChangeCursor) ((SwingButton) e.getSource()).setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			}

			@Override
			public void mouseExited(MouseEvent e) {
				if (SingleDisplay.allowChangeCursor) ((SwingButton) e.getSource()).setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			}
		});

		newButton.init(getURLForImage(path), ID, toolTip);

		// add listener
		((AbstractButton) newButton).addActionListener(this.currentCommandListener);

		int mode = this.commonValues.getModeOfOperation();

		// remove background for the applet as default L&F has a shaded toolbar
		if (mode == Values.RUNNING_APPLET) ((AbstractButton) newButton).setContentAreaFilled(false);

		// add to toolbar
		if (line == BUTTONBAR || mode == Values.RUNNING_PLUGIN) {
			this.topButtons.add((AbstractButton) newButton);

			// add spaces for plugin
			if (mode == Values.RUNNING_PLUGIN && (mode == Commands.LASTPAGE || mode == Commands.PAGEFLOW)) this.topButtons.add(Box
					.createHorizontalGlue());

		}
		else
			if (line == NAVBAR) {
				this.navToolBar.add((AbstractButton) newButton);
			}
			else
				if (line == PAGES) {
					this.pagesToolBar.add((AbstractButton) newButton, BorderLayout.CENTER);
				}
	}

	/*
	 * (non-Javadoc)
	 * @see org.jpedal.examples.viewer.gui.swing.GUIFactory#addMenuItem(javax.swing.JMenu, java.lang.String, java.lang.String, int)
	 */
	@Override
	public void addMenuItem(JMenu parentMenu, String text, String toolTip, final int ID) {
		addMenuItem(parentMenu, text, toolTip, ID, false);
	}

	public void addMenuItem(JMenu parentMenu, String text, String toolTip, final int ID, boolean isCheckBox) {

		SwingID menuItem;
		if (isCheckBox) menuItem = new SwingCheckBoxMenuItem(text);
		else menuItem = new SwingMenuItem(text);

		if (toolTip.length() > 0) menuItem.setToolTipText(toolTip);
		menuItem.setID(ID);
		setKeyAccelerators(ID, (JMenuItem) menuItem);

		// add listener
		menuItem.addActionListener(this.currentCommandListener);

		switch (ID) {
			case Commands.OPENFILE:
				this.open = (JMenuItem) menuItem;
				parentMenu.add(this.open);
				break;
			case Commands.OPENURL:
				this.openUrl = (JMenuItem) menuItem;
				parentMenu.add(this.openUrl);
				break;
			case Commands.SAVE:
				this.save = (JMenuItem) menuItem;
				parentMenu.add(this.save);
				break;
			case Commands.SAVEFORM:
				this.reSaveAsForms = (JMenuItem) menuItem;
				// add name to resave option so fest can get to it.
				this.reSaveAsForms.setName("resaveForms");
				parentMenu.add(this.reSaveAsForms);
				break;
			case Commands.FIND:
				this.find = (JMenuItem) menuItem;
				parentMenu.add(this.find);
				break;
			case Commands.DOCINFO:
				this.documentProperties = (JMenuItem) menuItem;
				parentMenu.add(this.documentProperties);
				break;
			case Commands.SIGN:
				this.signPDF = (JMenuItem) menuItem;
				parentMenu.add(this.signPDF);
				break;
			case Commands.PRINT:
				this.print = (JMenuItem) menuItem;
				parentMenu.add(this.print);
				break;
			case Commands.EXIT:
				this.exit = (JMenuItem) menuItem;
				// set name to exit so fest can find it
				this.exit.setName("exit");
				parentMenu.add(this.exit);
				break;
			case Commands.COPY:
				this.copy = (JMenuItem) menuItem;
				parentMenu.add(this.copy);
				break;
			case Commands.SELECTALL:
				this.selectAll = (JMenuItem) menuItem;
				parentMenu.add(this.selectAll);
				break;
			case Commands.DESELECTALL:
				this.deselectAll = (JMenuItem) menuItem;
				parentMenu.add(this.deselectAll);
				break;
			case Commands.PREFERENCES:
				this.preferences = (JMenuItem) menuItem;
				parentMenu.add(this.preferences);
				break;
			case Commands.FIRSTPAGE:
				this.firstPage = (JMenuItem) menuItem;
				parentMenu.add(this.firstPage);
				break;
			case Commands.BACKPAGE:
				this.backPage = (JMenuItem) menuItem;
				parentMenu.add(this.backPage);
				break;
			case Commands.FORWARDPAGE:
				this.forwardPage = (JMenuItem) menuItem;
				parentMenu.add(this.forwardPage);
				break;
			case Commands.LASTPAGE:
				this.lastPage = (JMenuItem) menuItem;
				parentMenu.add(this.lastPage);
				break;
			case Commands.GOTO:
				this.goTo = (JMenuItem) menuItem;
				parentMenu.add(this.goTo);
				break;
			case Commands.PREVIOUSDOCUMENT:
				this.previousDocument = (JMenuItem) menuItem;
				parentMenu.add(this.previousDocument);
				break;
			case Commands.NEXTDOCUMENT:
				this.nextDocument = (JMenuItem) menuItem;
				parentMenu.add(this.nextDocument);
				break;
			case Commands.FULLSCREEN:
				this.fullscreen = (JMenuItem) menuItem;
				parentMenu.add(this.fullscreen);
				break;
			case Commands.MOUSEMODE:
				this.fullscreen = (JMenuItem) menuItem;
				parentMenu.add(this.fullscreen);
				break;
			case Commands.PANMODE:
				this.panMode = (JMenuItem) menuItem;
				parentMenu.add(this.panMode);
				break;
			case Commands.TEXTSELECT:
				this.textSelect = (JMenuItem) menuItem;
				parentMenu.add(this.textSelect);
				break;
			case Commands.SEPARATECOVER:
				this.separateCover = (JCheckBoxMenuItem) menuItem;
				boolean separateCoverOn = this.properties.getValue("separateCoverOn").toLowerCase().equals("true");
				this.separateCover.setState(separateCoverOn);
				SingleDisplay.default_separateCover = separateCoverOn;
				parentMenu.add(this.separateCover);
				break;
			case Commands.CASCADE:
				this.cascade = (JMenuItem) menuItem;
				parentMenu.add(this.cascade);
				break;
			case Commands.TILE:
				this.tile = (JMenuItem) menuItem;
				parentMenu.add(this.tile);
				break;
			case Commands.PDF:
				this.onePerPage = (JMenuItem) menuItem;
				parentMenu.add(this.onePerPage);
				break;
			case Commands.NUP:
				this.nup = (JMenuItem) menuItem;
				parentMenu.add(this.nup);
				break;
			case Commands.HANDOUTS:
				this.handouts = (JMenuItem) menuItem;
				parentMenu.add(this.handouts);
				break;
			case Commands.IMAGES:
				this.images = (JMenuItem) menuItem;
				parentMenu.add(this.images);
				break;
			case Commands.TEXT:
				this.text = (JMenuItem) menuItem;
				parentMenu.add(this.text);
				break;
			case Commands.BITMAP:
				this.bitmap = (JMenuItem) menuItem;
				parentMenu.add(this.bitmap);
				break;
			case Commands.ROTATE:
				this.rotatePages = (JMenuItem) menuItem;
				parentMenu.add(this.rotatePages);
				break;
			case Commands.DELETE:
				this.deletePages = (JMenuItem) menuItem;
				parentMenu.add(this.deletePages);
				break;
			case Commands.ADD:
				this.addPage = (JMenuItem) menuItem;
				parentMenu.add(this.addPage);
				break;
			case Commands.ADDHEADERFOOTER:
				this.addHeaderFooter = (JMenuItem) menuItem;
				parentMenu.add(this.addHeaderFooter);
				break;
			case Commands.STAMPTEXT:
				this.stampText = (JMenuItem) menuItem;
				parentMenu.add(this.stampText);
				break;
			case Commands.STAMPIMAGE:
				this.stampImage = (JMenuItem) menuItem;
				parentMenu.add(this.stampImage);
				break;
			case Commands.SETCROP:
				this.crop = (JMenuItem) menuItem;
				parentMenu.add(this.crop);
				break;
			case Commands.VISITWEBSITE:
				this.visitWebsite = (JMenuItem) menuItem;
				parentMenu.add(this.visitWebsite);
				break;
			case Commands.TIP:
				this.tipOfTheDay = (JMenuItem) menuItem;
				parentMenu.add(this.tipOfTheDay);
				break;
			case Commands.UPDATE:
				this.checkUpdates = (JMenuItem) menuItem;
				parentMenu.add(this.checkUpdates);
				break;
			case Commands.INFO:
				this.about = (JMenuItem) menuItem;
				parentMenu.add(this.about);
				break;

			default:
				if (menuItem instanceof JMenuItem) parentMenu.add((JMenuItem) menuItem);
				else
					if (menuItem instanceof JCheckBoxMenuItem) parentMenu.add((JCheckBoxMenuItem) menuItem);
		}
	}

	/**
	 * @return the path of the directory containing overriding icons
	 */
	public String getIconLocation() {
		return this.iconLocation;
	}

	/**
	 * Retrieve the URL of the actual image to use f
	 * 
	 * @param path
	 *            Preferred name and location
	 * @return URL of file to use
	 */
	public URL getURLForImage(String path) {
		String file = path.substring(path.lastIndexOf('/') + 1);
		URL url;

		// <start-wrap>

		// Check if file location provided
		path = path.substring(0, path.indexOf('.')) + ".gif";
		File p = new File(path);
		url = getClass().getResource(path);

		// It's a file location check for gif
		if (p.exists()) {
			try {
				url = p.toURI().toURL();
			}
			catch (MalformedURLException e) {

			}

		}

		if (url == null) {
			path = path.substring(0, path.indexOf('.')) + ".png";
			p = new File(path);
			url = getClass().getResource(path);

			// It's a file location check for png
			if (p.exists()) {
				try {
					url = p.toURI().toURL();
				}
				catch (MalformedURLException e) {}
			}
		}

		if (url != null) {
			return url;
		}
		else { // use default graphic
				// <end-wrap>
			path = "/org/jpedal/examples/viewer/res/" + file;
			url = getClass().getResource(path);
			return url;
			// <start-wrap>
		}
		// <end-wrap>
	}

	/*
	 * (non-Javadoc)
	 * @see org.jpedal.examples.viewer.gui.swing.GUIFactory#addCombo(java.lang.String, java.lang.String, int)
	 */
	@Override
	public void addCombo(String title, String tooltip, int ID) {

		GUICombo combo = null;
		switch (ID) {
			case Commands.SCALING:
				combo = this.scalingBox;
				break;
			case Commands.ROTATION:
				combo = this.rotationBox;
				break;

		}

		combo.setID(ID);

		this.optimizationLabel = new JLabel(title);
		if (tooltip.length() > 0) combo.setToolTipText(tooltip);

		// <start-wrap>
		/**
		 * //<end-wrap> topButtons.add((SwingCombo) combo); /
		 **/

		// <start-wrap>
		if (this.useNewLayout) {
			this.topButtons.add((SwingCombo) combo);
		}
		else {
			this.comboBoxBar.add(this.optimizationLabel);
			this.comboBoxBar.add((SwingCombo) combo);
		}
		// <end-wrap>

		// add listener
		((SwingCombo) combo).addActionListener(this.currentCommandListener);
	}

	/*
	 * (non-Javadoc)
	 * @see org.jpedal.examples.viewer.gui.swing.GUIFactory#setViewerTitle(java.lang.String)
	 */
	@Override
	public void setViewerTitle(String title) {

		// <start-wrap>
		/**
		 * //<end-wrap> // hard-coded for file if(org.jpedal.examples.viewer.Viewer.message==null) title=org.jpedal.examples.viewer.Viewer.file; else
		 * title=org.jpedal.examples.viewer.Viewer.message; /
		 **/

		if (title != null) {

			// <start-demo>
			/**
			 * //<end-demo> title="("+dx+" days left) "+title; /
			 **/

			// <end-full>
			title = "LGPL " + title;
			/**/
			if (this.frame instanceof JFrame) ((JFrame) this.frame).setTitle(title);
		}
		else {

			String finalMessage = "";

			if (this.titleMessage == null) finalMessage = (windowTitle + ' ' + this.commonValues.getSelectedFile());
			else finalMessage = this.titleMessage + this.commonValues.getSelectedFile();

			PdfObject linearObj = (PdfObject) this.decode_pdf.getJPedalObject(PdfDictionary.Linearized);
			if (linearObj != null) {
				LinearThread linearizedBackgroundReaderer = (LinearThread) this.decode_pdf.getJPedalObject(PdfDictionary.LinearizedReader);

				if (linearizedBackgroundReaderer != null && linearizedBackgroundReaderer.isAlive()) finalMessage = finalMessage + " (still loading)";
				else finalMessage = finalMessage + " (Linearized)";
			}

			// <start-demo>
			/**
			 * //<end-demo> finalMessage="("+dx+" days left) "+finalMessage; /
			 **/

			finalMessage = "LGPL " + finalMessage;
			/**/
			if (this.commonValues.isFormsChanged()) finalMessage = "* " + finalMessage;

			if (this.frame instanceof JFrame) ((JFrame) this.frame).setTitle(finalMessage);

		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.jpedal.examples.viewer.gui.swing.GUIFactory#resetComboBoxes(boolean)
	 */
	@Override
	public void resetComboBoxes(boolean value) {
		this.scalingBox.setEnabled(value);
		this.rotationBox.setEnabled(value);
	}

	/*
	 * (non-Javadoc)
	 * @see org.jpedal.examples.viewer.gui.swing.GUIFactory#createPane(javax.swing.JTextPane, java.lang.String, boolean)
	 */
	@Override
	public final JScrollPane createPane(JTextPane text_pane, String content, boolean useXML) throws BadLocationException {

		text_pane.setEditable(true);
		text_pane.setFont(new Font("Lucida", Font.PLAIN, 14));

		text_pane.setToolTipText(Messages.getMessage("PdfViewerTooltip.text"));
		Document doc = text_pane.getDocument();
		text_pane.setBorder(BorderFactory.createTitledBorder(new EtchedBorder(), Messages.getMessage("PdfViewerTitle.text")));
		text_pane.setForeground(Color.black);

		SimpleAttributeSet token_attribute = new SimpleAttributeSet();
		SimpleAttributeSet text_attribute = new SimpleAttributeSet();
		SimpleAttributeSet plain_attribute = new SimpleAttributeSet();
		StyleConstants.setForeground(token_attribute, Color.blue);
		StyleConstants.setForeground(text_attribute, Color.black);
		StyleConstants.setForeground(plain_attribute, Color.black);
		int pointer = 0;

		/** put content in and color XML */
		if ((useXML) && (content != null)) {
			// tokenise and write out data
			StringTokenizer data_As_tokens = new StringTokenizer(content, "<>", true);

			while (data_As_tokens.hasMoreTokens()) {
				String next_item = data_As_tokens.nextToken();

				if ((next_item.equals("<")) && ((data_As_tokens.hasMoreTokens()))) {

					String current_token = next_item + data_As_tokens.nextToken() + data_As_tokens.nextToken();

					doc.insertString(pointer, current_token, token_attribute);
					pointer = pointer + current_token.length();

				}
				else {
					doc.insertString(pointer, next_item, text_attribute);
					pointer = pointer + next_item.length();
				}
			}
		}
		else doc.insertString(pointer, content, plain_attribute);

		// wrap in scrollpane
		JScrollPane text_scroll = new JScrollPane();
		text_scroll.getViewport().add(text_pane);
		text_scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		text_scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		return text_scroll;
	}

	/*
	 * (non-Javadoc)
	 * @see org.jpedal.examples.viewer.gui.swing.GUIFactory#getSelectedComboIndex(int)
	 */
	@Override
	public int getSelectedComboIndex(int ID) {

		switch (ID) {
			case Commands.SCALING:
				return this.scalingBox.getSelectedIndex();
			case Commands.ROTATION:
				return this.rotationBox.getSelectedIndex();
			default:
				return -1;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.jpedal.examples.viewer.gui.swing.GUIFactory#setSelectedComboIndex(int, int)
	 */
	@Override
	public void setSelectedComboIndex(int ID, int index) {
		switch (ID) {
			case Commands.SCALING:
				this.scalingBox.setSelectedIndex(index);
				break;
			case Commands.ROTATION:
				this.rotationBox.setSelectedIndex(index);
				break;

		}
	}

	/**
	 * @param ID
	 * @return comboBox or null if not (QUALITY, SCALING or ROTATION)
	 */ 
	public GUICombo getCombo(int ID) {

		switch (ID) {
			case Commands.SCALING:
				return this.scalingBox;
			case Commands.ROTATION:
				return this.rotationBox;

		}

		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.jpedal.examples.viewer.gui.swing.GUIFactory#setSelectedComboItem(int, java.lang.String)
	 */
	@Override
	public void setSelectedComboItem(int ID, String index) {
		switch (ID) {
			case Commands.SCALING:
				this.scalingBox.setSelectedItem(index + '%');
				break;
			case Commands.ROTATION:
				this.rotationBox.setSelectedItem(index);
				break;

		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.jpedal.examples.viewer.gui.swing.GUIFactory#getSelectedComboItem(int)
	 */
	@Override
	public Object getSelectedComboItem(int ID) {

		switch (ID) {
			case Commands.SCALING:
				return this.scalingBox.getSelectedItem();
			case Commands.ROTATION:
				return this.rotationBox.getSelectedItem();
			default:
				return null;

		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.jpedal.examples.viewer.gui.swing.GUIFactory#zoom()
	 */
	@Override
	public void zoom(boolean Rotated) {
		scaleAndRotate(this.scaling, this.rotation);
	}

	/** all scaling and rotation should go through this. */
	private void scaleAndRotate(float scalingValue, int rotationValue) {

		if (this.decode_pdf.getDisplayView() == Display.PAGEFLOW) {
			this.decode_pdf.setPageParameters(this.scaling, this.commonValues.getCurrentPage(), this.rotation);
			return;
		}

		// ignore if called too early
		if (!this.decode_pdf.isOpen() && this.currentCommands.isPDF()) return;

		float width, height;

		if (this.isSingle) {

			width = this.scrollPane.getViewport().getWidth() - inset - inset;
			height = this.scrollPane.getViewport().getHeight() - inset - inset;

		}
		else {
			width = this.desktopPane.getWidth();
			height = this.desktopPane.getHeight();
		}

		if (this.decode_pdf != null) {

			// get current location and factor out scaling so we can put back at same page
			// final float x= (decode_pdf.getVisibleRect().x/scaling);
			// final float y= (decode_pdf.getVisibleRect().y/scaling);

			// System.out.println(x+" "+y+" "+scaling+" "+decode_pdf.getVisibleRect());
			/** update value and GUI */
			int index = getSelectedComboIndex(Commands.SCALING);

			if (this.decode_pdf.getDisplayView() == Display.PAGEFLOW) {

				// Ensure we only display in window mode
				setSelectedComboIndex(Commands.SCALING, 0);
				index = 0;

				// Disable scaling option
				this.scalingBox.setEnabled(false);
			}
			else
				if (this.decode_pdf.getDisplayView() != Display.PAGEFLOW) {

					// No long pageFlow. enable scaling option
					this.scalingBox.setEnabled(true);
				}

			if (index == -1) {
				String numberValue = (String) getSelectedComboItem(Commands.SCALING);
				float zoom = -1;
				if ((numberValue != null) && (numberValue.length() > 0)) {
					try {
						zoom = Float.parseFloat(numberValue);
					}
					catch (Exception e) {
						zoom = -1;
						// its got characters in it so get first valid number string
						int length = numberValue.length();
						int ii = 0;
						while (ii < length) {
							char c = numberValue.charAt(ii);
							if (((c >= '0') && (c <= '9')) | (c == '.')) ii++;
							else break;
						}

						if (ii > 0) numberValue = numberValue.substring(0, ii);

						// try again if we reset above
						if (zoom == -1) {
							try {
								zoom = Float.parseFloat(numberValue);
							}
							catch (Exception e1) {
								zoom = -1;
							}
						}
					}
					if (zoom > 1000) {
						zoom = 1000;
					}
				}

				// if nothing off either attempt, use window value
				if (zoom == -1) {
					// its not set so use To window value
					index = defaultSelection;
					setSelectedComboIndex(Commands.SCALING, index);
				}
				else {
					this.scaling = this.decode_pdf.getDPIFactory().adjustScaling(zoom / 100);

					setSelectedComboItem(Commands.SCALING, String.valueOf(zoom));
				}
			}

			int page = this.commonValues.getCurrentPage();

			// Multipage tiff should be treated as a single page
			if (this.isMultiPageTiff) page = 1;

			// always check in facing mode with turnover on
			if (index != -1
					|| this.decode_pdf.getDisplayView() == Display.SINGLE_PAGE
					|| (this.decode_pdf.getDisplayView() == Display.FACING && this.currentCommands.getPages().getBoolean(
							Display.BoolValue.TURNOVER_ON))) {
				PdfPageData pageData = this.decode_pdf.getPdfPageData();
				int cw, ch, raw_rotation = 0;

				if (this.decode_pdf.getDisplayView() == Display.FACING) raw_rotation = pageData.getRotation(page);

				boolean isRotated = (this.rotation + raw_rotation) % 180 == 90;

				PageOffsets offsets = (PageOffsets) this.decode_pdf.getExternalHandler(Options.CurrentOffset);
				switch (this.decode_pdf.getDisplayView()) {
					case Display.CONTINUOUS_FACING:
						if (isRotated) {
							cw = offsets.getMaxH() * 2;
							ch = offsets.getMaxW();
						}
						else {
							cw = offsets.getMaxW() * 2;
							ch = offsets.getMaxH();
						}
						break;
					case Display.CONTINUOUS:
						if (isRotated) {
							cw = offsets.getMaxH();
							ch = offsets.getMaxW();
						}
						else {
							cw = offsets.getMaxW();
							ch = offsets.getMaxH();
						}
						break;
					case Display.FACING:
						int leftPage;

						if (this.currentCommands.getPages().getBoolean(Display.BoolValue.SEPARATE_COVER)) {
							leftPage = (page / 2) * 2;
							if (this.commonValues.getPageCount() == 2) leftPage = 1;
						}
						else {
							leftPage = (page);
							if ((leftPage & 1) == 0) leftPage--;
						}

						if (isRotated) {
							cw = pageData.getCropBoxHeight(leftPage);

							// if first or last page double the width, otherwise add other page width
							if (leftPage + 1 > this.commonValues.getPageCount() || leftPage == 1) cw = cw * 2;
							else cw += pageData.getCropBoxHeight(leftPage + 1);

							ch = pageData.getCropBoxWidth(leftPage);
							if (leftPage + 1 <= this.commonValues.getPageCount() && ch < pageData.getCropBoxWidth(leftPage + 1)) ch = pageData
									.getCropBoxWidth(leftPage + 1);
						}
						else {
							cw = pageData.getCropBoxWidth(leftPage);

							// if first or last page double the width, otherwise add other page width
							if (leftPage + 1 > this.commonValues.getPageCount()) cw = cw * 2;
							else cw += pageData.getCropBoxWidth(leftPage + 1);

							ch = pageData.getCropBoxHeight(leftPage);
							if (leftPage + 1 <= this.commonValues.getPageCount() && ch < pageData.getCropBoxHeight(leftPage + 1)) ch = pageData
									.getCropBoxHeight(leftPage + 1);
						}
						break;
					default:
						if (isRotated) {
							cw = pageData.getCropBoxHeight(page);
							ch = pageData.getCropBoxWidth(page);
						}
						else {
							cw = pageData.getCropBoxWidth(page);
							ch = pageData.getCropBoxHeight(page);
						}
				}

				if (this.isSingle) {

					if (this.displayPane != null) width = width - this.displayPane.getDividerSize();

				}

				float x_factor = 0, y_factor = 0, window_factor = 0;
				x_factor = width / cw;
				y_factor = height / ch;

				if (x_factor < y_factor) window_factor = x_factor;
				else window_factor = y_factor;

				if (index != -1) {
					if (index < 3) { // handle scroll to width/height/window
						if (index == 0) {// window
							this.scaling = window_factor;
						}
						else
							if (index == 1) // height
							this.scaling = y_factor;
							else
								if (index == 2) // width
								this.scaling = x_factor;
					}
					else {
						this.scaling = this.decode_pdf.getDPIFactory().adjustScaling(this.scalingFloatValues[index]);
					}
				}
				if (this.decode_pdf.getDisplayView() == Display.FACING) { // Enable turnover if both pages properly displayed
					if (this.scaling <= window_factor) {
						this.pageTurnScalingAppropriate = true;
					}
					else {
						this.pageTurnScalingAppropriate = false;
					}
				}

				if (this.thumbscroll != null) {
					if (this.decode_pdf.getDisplayView() == Display.SINGLE_PAGE && this.scaling <= window_factor) {

						this.scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
						this.scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

						this.thumbscroll.setVisible(true);
					}
					else {

						this.scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
						this.scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

						this.thumbscroll.setVisible(false);
					}
				}
			}

			// this check for 0 to avoid error and replace with 1
			// PdfPageData pagedata = decode_pdf.getPdfPageData();
			// if((pagedata.getCropBoxHeight(commonValues.getCurrentPage())*scaling<100) &&//keep the page bigger than 100 pixels high
			// (pagedata.getCropBoxWidth(commonValues.getCurrentPage())*scaling<100) && commonValues.isPDF()){//keep the page bigger than 100 pixels
			// wide
			// scaling=1;
			// setSelectedComboItem(Commands.SCALING,"100");
			// }

			// THIS section commented out so altering scalingbox does NOT reset rotation
			// if(!scalingBox.getSelectedIndex()<3){
			/** update our components */
			// resetRotationBox();
			// }

			// Ensure page rotation is taken into account
			// int pageRot = decode_pdf.getPdfPageData().getRotation(commonValues.getCurrentPage());
			// allow for clicking on it before page opened
			this.decode_pdf.setPageParameters(this.scaling, page, this.rotation);

			// Ensure the page is displayed in the correct rotation
			setRotation();

			// move to correct page
			// setPageNumber();
			// decode_pdf.setDisplayView(decode_pdf.getDisplayView(),Display.DISPLAY_CENTERED);

			// open new page
			// if((!commonValues.isProcessing())&&(commonValues.getCurrentPage()!=newPage)){

			// commonValues.setCurrentPage(newPage);
			// decodePage(false);
			// currentGUI.zoom();
			// }

			// ensure at same page location

			Runnable updateAComponent = new Runnable() {
				@Override
				public void run() {
					//
					SwingGUI.this.decode_pdf.invalidate();
					SwingGUI.this.decode_pdf.updateUI();
					SwingGUI.this.decode_pdf.validate();

					SwingGUI.this.scrollPane.invalidate();
					SwingGUI.this.scrollPane.updateUI();
					SwingGUI.this.scrollPane.validate();

					// move to correct page
					// scrollToPage is handled via the page change code so no need to do it here
					// if(commonValues.isPDF())
					// scrollToPage(commonValues.getCurrentPage());
					// scrollPane.getViewport().scrollRectToVisible(new Rectangle((int)(x*scaling)-1,(int)(y*scaling),1,1));
					// System.out.println("Scroll to page="+y+" "+(y*scaling)+" "+scaling);

				}
			};
			// boolean callAsThread=SwingUtilities.isEventDispatchThread();
			// if (callAsThread)
			// scroll
			SwingUtilities.invokeLater(updateAComponent);
			// else{

			// //move to correct page
			// if(commonValues.isPDF())
			// scrollToPage(commonValues.getCurrentPage());

			// scrollPane.updateUI();

			// }
			// decode_pdf.invalidate();
			// scrollPane.updateUI();
			// decode_pdf.repaint();
			// scrollPane.repaint();
			// frame.validate();

		}
	}

	public void snapScalingToDefaults(float newScaling) {
		newScaling = this.decode_pdf.getDPIFactory().adjustScaling(newScaling / 100);

		float width, height;

		if (this.isSingle) {
			width = this.scrollPane.getViewport().getWidth() - inset - inset;
			height = this.scrollPane.getViewport().getHeight() - inset - inset;
		}
		else {
			width = this.desktopPane.getWidth();
			height = this.desktopPane.getHeight();
		}

		PdfPageData pageData = this.decode_pdf.getPdfPageData();
		int cw, ch, raw_rotation = 0;

		if (this.decode_pdf.getDisplayView() == Display.FACING) raw_rotation = pageData.getRotation(this.commonValues.getCurrentPage());

		boolean isRotated = (this.rotation + raw_rotation) % 180 == 90;

		PageOffsets offsets = (PageOffsets) this.decode_pdf.getExternalHandler(Options.CurrentOffset);
		switch (this.decode_pdf.getDisplayView()) {
			case Display.CONTINUOUS_FACING:
				if (isRotated) {
					cw = offsets.getMaxH() * 2;
					ch = offsets.getMaxW();
				}
				else {
					cw = offsets.getMaxW() * 2;
					ch = offsets.getMaxH();
				}
				break;
			case Display.CONTINUOUS:
				if (isRotated) {
					cw = offsets.getMaxH();
					ch = offsets.getMaxW();
				}
				else {
					cw = offsets.getMaxW();
					ch = offsets.getMaxH();
				}
				break;
			case Display.FACING:
				int leftPage;
				if (this.currentCommands.getPages().getBoolean(Display.BoolValue.SEPARATE_COVER)) {
					leftPage = (this.commonValues.getCurrentPage() / 2) * 2;
					if (this.commonValues.getPageCount() == 2) leftPage = 1;
				}
				else {
					leftPage = this.commonValues.getCurrentPage();
					if ((leftPage & 1) == 0) leftPage--;
				}

				if (isRotated) {
					cw = pageData.getCropBoxHeight(leftPage);

					// if first or last page double the width, otherwise add other page width
					if (leftPage + 1 > this.commonValues.getPageCount() || leftPage == 1) cw = cw * 2;
					else cw += pageData.getCropBoxHeight(leftPage + 1);

					ch = pageData.getCropBoxWidth(leftPage);
					if (leftPage + 1 <= this.commonValues.getPageCount() && ch < pageData.getCropBoxWidth(leftPage + 1)) ch = pageData
							.getCropBoxWidth(leftPage + 1);
				}
				else {
					cw = pageData.getCropBoxWidth(leftPage);

					// if first or last page double the width, otherwise add other page width
					if (leftPage + 1 > this.commonValues.getPageCount()) cw = cw * 2;
					else cw += pageData.getCropBoxWidth(leftPage + 1);

					ch = pageData.getCropBoxHeight(leftPage);
					if (leftPage + 1 <= this.commonValues.getPageCount() && ch < pageData.getCropBoxHeight(leftPage + 1)) ch = pageData
							.getCropBoxHeight(leftPage + 1);
				}
				break;
			default:
				if (isRotated) {
					cw = pageData.getCropBoxHeight(this.commonValues.getCurrentPage());
					ch = pageData.getCropBoxWidth(this.commonValues.getCurrentPage());
				}
				else {
					cw = pageData.getCropBoxWidth(this.commonValues.getCurrentPage());
					ch = pageData.getCropBoxHeight(this.commonValues.getCurrentPage());
				}
		}

		if (this.isSingle) {
			if (this.displayPane != null) width = width - this.displayPane.getDividerSize();
		}

		float x_factor, y_factor, window_factor;
		x_factor = width / cw;
		y_factor = height / ch;

		if (x_factor < y_factor) {
			window_factor = x_factor;
			x_factor = -1;
		}
		else {
			window_factor = y_factor;
			y_factor = -1;
		}

		if (getSelectedComboIndex(Commands.SCALING) != 0
				&& ((newScaling < window_factor * 1.1 && newScaling > window_factor * 0.91) || ((window_factor > this.scaling && window_factor < newScaling) || (window_factor < this.scaling && window_factor > newScaling)))) {
			setSelectedComboIndex(Commands.SCALING, 0);
			this.scaling = window_factor;
		}

		else
			if (y_factor != -1
					&& getSelectedComboIndex(Commands.SCALING) != 1
					&& ((newScaling < y_factor * 1.1 && newScaling > y_factor * 0.91) || ((y_factor > this.scaling && y_factor < newScaling) || (y_factor < this.scaling && y_factor > newScaling)))) {
				setSelectedComboIndex(Commands.SCALING, 1);
				this.scaling = y_factor;
			}

			else
				if (x_factor != -1
						&& getSelectedComboIndex(Commands.SCALING) != 2
						&& ((newScaling < x_factor * 1.1 && newScaling > x_factor * 0.91) || ((x_factor > this.scaling && x_factor < newScaling) || (x_factor < this.scaling && x_factor > newScaling)))) {
					setSelectedComboIndex(Commands.SCALING, 2);
					this.scaling = x_factor;
				}

				else {
					setSelectedComboItem(Commands.SCALING, String.valueOf((int) this.decode_pdf.getDPIFactory().removeScaling(newScaling * 100)));
					this.scaling = newScaling;
				}
	}

	/*
	 * (non-Javadoc)
	 * @see org.jpedal.examples.viewer.gui.swing.GUIFactory#rotate()
	 */
	@Override
	public void rotate() {
		this.rotation = Integer.parseInt((String) getSelectedComboItem(Commands.ROTATION));
		scaleAndRotate(this.scaling, this.rotation);
		this.decode_pdf.updateUI();
	}

	public void scrollToPage(int page) {

		this.commonValues.setCurrentPage(page);

		if (this.commonValues.getCurrentPage() > 0) {

			int yCord = 0;
			int xCord = 0;

			if (this.decode_pdf.getDisplayView() != Display.SINGLE_PAGE) {
				yCord = this.currentCommands.pages.getYCordForPage(this.commonValues.getCurrentPage(), this.scaling);
				xCord = 0;
			}
			// System.out.println("Before="+decode_pdf.getVisibleRect()+" "+decode_pdf.getPreferredSize());

			PdfPageData pageData = this.decode_pdf.getPdfPageData();

			int ch = (int) (pageData.getCropBoxHeight(this.commonValues.getCurrentPage()) * this.scaling);
			int cw = (int) (pageData.getCropBoxWidth(this.commonValues.getCurrentPage()) * this.scaling);

			int centerH = xCord + ((cw - this.scrollPane.getHorizontalScrollBar().getVisibleAmount()) / 2);
			int centerV = yCord + (ch - this.scrollPane.getVerticalScrollBar().getVisibleAmount()) / 2;

			this.scrollPane.getHorizontalScrollBar().setValue(centerH);
			this.scrollPane.getVerticalScrollBar().setValue(centerV);

			// decode_pdf.scrollRectToVisible(new Rectangle(0,(int) (yCord),(int)r.width-1,(int)r.height-1));
			// decode_pdf.scrollRectToVisible(new Rectangle(0,(int) (yCord),(int)r.width-1,(int)r.height-1));

			// System.out.println("After="+decode_pdf.getVisibleRect()+" "+decode_pdf.getPreferredSize());

			// System.out.println("Scroll to page="+commonValues.getCurrentPage()+" "+yCord+" "+(yCord*scaling)+" "+scaling);
		}

		if (this.decode_pdf.getPageCount() > 1) setPageLayoutButtonsEnabled(true);
	}

	/*
	 * (non-Javadoc)
	 * @see org.jpedal.examples.viewer.gui.swing.GUIFactory#decodePage(boolean)
	 */
	@Override
	public void decodePage(final boolean resizePanel) {

		// Remove Image extraction outlines when page is changed
		this.currentCommands.getPages().setHighlightedImage(null);

		resetRotationBox();

		/** if running terminate first */
		if (this.thumbnails.isShownOnscreen()) this.thumbnails.terminateDrawing();

		if (this.thumbnails.isShownOnscreen()) {

			LinearThread linearizedBackgroundRenderer = (LinearThread) this.decode_pdf.getJPedalObject(PdfDictionary.LinearizedReader);

			if (linearizedBackgroundRenderer == null || (linearizedBackgroundRenderer != null && !linearizedBackgroundRenderer.isAlive())) setupThumbnailPanel();
		}

		if (this.decode_pdf.getDisplayView() == Display.SINGLE_PAGE) {
			this.pageCounter2.setForeground(Color.black);
			this.pageCounter2.setText(String.valueOf(this.commonValues.getCurrentPage()));
			this.pageCounter3.setText(Messages.getMessage("PdfViewerOfLabel.text") + ' ' + this.commonValues.getPageCount());
		}

		// Set textbox size
		int col = ("/" + this.commonValues.getPageCount()).length();
		if (this.decode_pdf.getDisplayView() == Display.FACING || this.decode_pdf.getDisplayView() == Display.CONTINUOUS_FACING) col = col * 2;
		if (col < 4) col = 4;
		if (col > 10) col = 10;
		this.pageCounter2.setColumns(col);
		this.pageCounter2.setMaximumSize(this.pageCounter2.getPreferredSize());
		this.navToolBar.invalidate();
		this.navToolBar.doLayout();

		// allow user to now open tabs
		this.tabsNotInitialised = false;

		this.decode_pdf.unsetScaling();

		/**
		 * ensure text and color extracted. If you do not need color, take out line for faster decode
		 */
		// decode_pdf.setExtractionMode(PdfDecoder.TEXT);
		this.decode_pdf.setExtractionMode(PdfDecoder.TEXT + PdfDecoder.TEXTCOLOR);

		// remove any search highlight
		this.decode_pdf.getTextLines().clearHighlights();

		// stop user changing scaling while decode in progress
		resetComboBoxes(false);
		setPageLayoutButtonsEnabled(false);

		Values.setProcessing(true);

		// SwingWorker worker = new SwingWorker() {

		// public Object construct() {
		this.decode_pdf.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

		try {

			this.statusBar.updateStatus("Decoding Page", 0);

			/**
			 * make sure screen fits display nicely
			 */
			// if ((resizePanel) && (thumbnails.isShownOnscreen()))
			// zoom();

			// if (Thread.interrupted())
			// throw new InterruptedException();

			/**
			 * decode the page
			 */
			try {
				this.decode_pdf.decodePage(this.commonValues.getCurrentPage());

				// wait to ensure decoded
				this.decode_pdf.waitForDecodingToFinish();

				if (!this.decode_pdf.getPageDecodeStatus(DecodeStatus.ImagesProcessed)) {

					String status = (Messages.getMessage("PdfViewer.ImageDisplayError") + Messages.getMessage("PdfViewer.ImageDisplayError1")
							+ Messages.getMessage("PdfViewer.ImageDisplayError2") + Messages.getMessage("PdfViewer.ImageDisplayError3")
							+ Messages.getMessage("PdfViewer.ImageDisplayError4") + Messages.getMessage("PdfViewer.ImageDisplayError5")
							+ Messages.getMessage("PdfViewer.ImageDisplayError6") + Messages.getMessage("PdfViewer.ImageDisplayError7"));

					showMessageDialog(status);
				}

				/**
				 * see if lowres poor quality image and flag up if so
				 * 
				 * String imageDetailStr=decode_pdf.getInfo(PdfDictionary.Image);
				 * 
				 * //get iterator (each image is a single line) StringTokenizer allImageDetails=new StringTokenizer(imageDetailStr,"\n");
				 * 
				 * 
				 * while(allImageDetails.hasMoreTokens()){
				 * 
				 * String str=allImageDetails.nextToken(); StringTokenizer imageDetails=new StringTokenizer(str," ()");
				 * 
				 * //System.out.println(imageDetails.countTokens()+" ==>"+str); //if single image check further if(imageDetails.countTokens()>2){
				 * //ignore forms String imageName=imageDetails.nextToken(); String imageType=imageDetails.nextToken(); String
				 * imageW=imageDetails.nextToken().substring(2); String imageH=imageDetails.nextToken().substring(2); String
				 * bitsPerPixel=imageDetails.nextToken(); String dpi=imageDetails.nextToken().substring(4);
				 * 
				 * //we can also look at PDF creation tool String[] metaData=decode_pdf.getFileInformationData().getFieldValues();
				 * 
				 * //test here and take action or set flag if(Integer.parseInt(dpi)<144 && metaData[5].equals("iText 2.1.7 by 1T3XT")){
				 * System.out.println("Low resolution image will not print well in Java"); } } }
				 * 
				 * /
				 **/

				/**
				 * Tell user if hinting is probably required
				 */
				if (this.decode_pdf.getPageDecodeStatus(DecodeStatus.TTHintingRequired)) {

					String status = Messages.getMessage("PdfCustomGui.ttHintingRequired");

					showMessageDialog(status);
				}

				if (this.decode_pdf.getPageDecodeStatus(DecodeStatus.NonEmbeddedCIDFonts)) {

					String status = ("This page contains non-embedded CID fonts \n"
							+ this.decode_pdf.getPageDecodeStatusReport(DecodeStatus.NonEmbeddedCIDFonts)
							+ "\nwhich may need mapping to display correctly.\n" + "See http://www.idrsolutions.com/how-do-fonts-work");

					showMessageDialog(status);
				}
				// read values for page display
				PdfPageData page_data = this.decode_pdf.getPdfPageData();

				this.mediaW = page_data.getMediaBoxWidth(this.commonValues.getCurrentPage());
				this.mediaH = page_data.getMediaBoxHeight(this.commonValues.getCurrentPage());
				this.mediaX = page_data.getMediaBoxX(this.commonValues.getCurrentPage());
				this.mediaY = page_data.getMediaBoxY(this.commonValues.getCurrentPage());

				this.cropX = page_data.getCropBoxX(this.commonValues.getCurrentPage());
				this.cropY = page_data.getCropBoxY(this.commonValues.getCurrentPage());
				this.cropW = page_data.getCropBoxWidth(this.commonValues.getCurrentPage());
				this.cropH = page_data.getCropBoxHeight(this.commonValues.getCurrentPage());

				// resetRotationBox();

				// create custom annot icons
				if (this.decode_pdf.getExternalHandler(Options.UniqueAnnotationHandler) != null) {
					/**
					 * ANNOTATIONS code to create unique icons
					 * 
					 * this code allows you to create a unique set on icons for any type of annotations, with an icons for every annotation, not just
					 * types.
					 */
					FormFactory formfactory = this.decode_pdf.getFormRenderer().getFormFactory();

					// swing needs it to be done with invokeLater
					if (formfactory.getType() == FormFactory.SWING) {
						final Runnable doPaintComponent2 = new Runnable() {
							@Override
							public void run() {

								createUniqueAnnotationIcons();

								// validate();
							}
						};
						SwingUtilities.invokeLater(doPaintComponent2);

					}
					else {
						createUniqueAnnotationIcons();
					}

				}

				this.statusBar.updateStatus("Displaying Page", 0);

			}
			catch (Exception e) {
				System.err
						.println(Messages.getMessage("PdfViewerError.Exception") + ' ' + e + ' ' + Messages.getMessage("PdfViewerError.DecodePage"));
				e.printStackTrace();
				Values.setProcessing(false);
			}

			// tell user if we had a memory error on decodePage
			if (PdfDecoder.showErrorMessages) {
				String status = this.decode_pdf.getPageDecodeReport();
				if (status.contains("java.lang.OutOfMemoryError")) {
					status = (Messages.getMessage("PdfViewer.OutOfMemoryDisplayError") + Messages.getMessage("PdfViewer.OutOfMemoryDisplayError1")
							+ Messages.getMessage("PdfViewer.OutOfMemoryDisplayError2") + Messages.getMessage("PdfViewer.OutOfMemoryDisplayError3")
							+ Messages.getMessage("PdfViewer.OutOfMemoryDisplayError4") + Messages.getMessage("PdfViewer.OutOfMemoryDisplayError5"));

					showMessageDialog(status);

				}
				else
					if (status.contains("JPeg 2000")) { // tell user if not set up for jpeg2000
						status = Messages.getMessage("PdfViewer.jpeg2000") + Messages.getMessage("PdfViewer.jpeg2000_1")
								+ Messages.getMessage("PdfViewer.jpeg2000_2") + Messages.getMessage("PdfViewer.jpeg2000_3")
								+ Messages.getMessage("PdfViewer.jpeg2000_4");

						Object[] options = { "Ok", "Open website help page" };

						int n = showMessageDialog(status, options, 0);
						if (n == 1) {
							try {
								BrowserLauncher.openURL("http://www.idrsolutions.com/additional-jars/");
							}
							catch (Exception e1) {}
							catch (Error err) {}
						}
					}
			}

			Values.setProcessing(false);

			// make sure fully drawn
			// decode_pdf.repaint();

			setViewerTitle(null); // restore title

			this.currentCommands.setPageProperties(getSelectedComboItem(Commands.ROTATION), getSelectedComboItem(Commands.SCALING));

			if (this.decode_pdf.getPageCount() > 0 && this.thumbnails.isShownOnscreen() && this.decode_pdf.getDisplayView() == Display.SINGLE_PAGE) this.thumbnails
					.generateOtherVisibleThumbnails(this.commonValues.getCurrentPage());

		}
		catch (Exception e) {
			e.printStackTrace();
			Values.setProcessing(false);// remove processing flag so that the viewer can be exited.
			setViewerTitle(null); // restore title
		}

		selectBookmark();

		// Update multibox
		this.statusBar.setProgress(100);
		if (this.useNewLayout) {
			// ActionListener listener = new ActionListener(){
			// public void actionPerformed(ActionEvent e) {
			// setMultibox(new int[]{});
			// }
			// };
			// t = new Timer(800, listener);
			// t.setRepeats(false);
			// t.start();

			try {
				Thread.sleep(800);
			}
			catch (Exception e) {}
			setMultibox(new int[] {});
		}

		// reanable user changing scaling
		resetComboBoxes(true);

		if (this.decode_pdf.getPageCount() > 1) setPageLayoutButtonsEnabled(true);

		addFormsListeners();

		// resize (ensure at least certain size)
		// zoom(flase) is called twice so remove this call
		// zoom(false);

		// <link><a name="draw" />

		// sample code to add shapes and text on current page - should be called AFTER page decoded for display
		// (can appear on multiple pages for printing)
		//

		// in this example, we create a rectangle, a filled rectangle and draw some text.

		// initialise objects arrays - we will put 4 shapes on the page
		// (using PDF co-ordinates with origin bottom left corner)
		/*
		 * int count=4; //adding shapes to page // Due to the way some pdf's are created it is necessery to take the offset of a page // into account
		 * when addding custom objects to the page. Variables mX and mY represent // that offset and need to be taken in to account when placing any
		 * additional object // on a page. int mX = decode_pdf.getPdfPageData().getMediaBoxX(1); int mY = decode_pdf.getPdfPageData().getMediaBoxY(1);
		 * int[] type=new int[count]; Color[] colors=new Color[count]; Object[] obj=new Object[count]; //example stroked shape type[0]=
		 * org.jpedal.render.DynamicVectorRenderer.STROKEDSHAPE; colors[0]=Color.RED; obj[0]=new Rectangle(35+mX,35+mY,510,50); //ALSO sets location.
		 * Any shape can be used //example filled shape type[1]= org.jpedal.render.DynamicVectorRenderer.FILLEDSHAPE; colors[1]=Color.GREEN;
		 * obj[1]=new Rectangle(40+mX,40+mY,500,40); //ALSO sets location. Any shape can be used //example text object type[2]=
		 * org.jpedal.render.DynamicVectorRenderer.STRING; org.jpedal.render.TextObject textObject=new org.jpedal.render.TextObject(); //composite
		 * object so we can pass in parameters textObject.x=40+mX; textObject.y=40+mY;
		 * textObject.text="Example text on page "+commonValues.getCurrentPage(); textObject.font=new Font("Serif",Font.PLAIN,48);
		 * colors[2]=Color.BLUE; obj[2]=textObject; //ALSO sets location //example custom (from version 3.40)
		 * type[3]=org.jpedal.render.DynamicVectorRenderer.CUSTOM; JPedalCustomDrawObject exampleObj=new ExampleCustomDrawObject();
		 * exampleObj.setMedX(mX); exampleObj.setMedY(mY); obj[3]=exampleObj; //pass into JPEDAL after page decoded - will be removed automatically on
		 * new page/open file //BUT PRINTING retains values until manually removed try{
		 * decode_pdf.drawAdditionalObjectsOverPage(commonValues.getCurrentPage(),type,colors,obj); }catch(PdfException e){ e.printStackTrace(); } /*
		 */

		// <link><a name="remove_additional_obj" />
		// this code will remove ALL items already drawn on page
		// try{
		// decode_pdf.flushAdditionalObjectsOnPage(commonValues.getCurrentPage());
		// }catch(PdfException e){
		// e.printStackTrace();
		// //ShowGUIMessage.showGUIMessage( "", new JLabel(e.getMessage()),"Exception adding object to display");
		// }

		// <link><a name="print" />

		// Example to PRINT (needs to be create beforehand)
		// objects can be the same as from draw

		/*
		 * for(int pages=1;pages<decode_pdf.getPageCount()+1;pages++){ //note +1 for last page!!! int count = 4; // Due to the way some pdf's are
		 * created it is necessery to take the offset of a page // into account when addding custom objects to the page. Variables mX and mY represent
		 * // that offset and need to be taken in to account when placing any additional object // on a page. int mX =
		 * decode_pdf.getPdfPageData().getMediaBoxX(1); int mY = decode_pdf.getPdfPageData().getMediaBoxY(1); int[] typePrint=new int[count]; Color[]
		 * colorsPrint=new Color[count]; Object[] objPrint=new Object[count]; //example custom (from version 3.40)
		 * typePrint[0]=org.jpedal.render.DynamicVectorRenderer.CUSTOM; JPedalCustomDrawObject examplePrintObj=new ExampleCustomDrawObject();
		 * examplePrintObj.setMedX(mX); examplePrintObj.setMedY(mY); objPrint[0]=examplePrintObj; //example stroked shape typePrint[1]=
		 * org.jpedal.render.DynamicVectorRenderer.STROKEDSHAPE; colorsPrint[1]=Color.RED; objPrint[1]=new Rectangle(35+mX,35+mY,510,50); //ALSO sets
		 * location. Any shape can be used //example filled shape typePrint[2]= org.jpedal.render.DynamicVectorRenderer.FILLEDSHAPE;
		 * colorsPrint[2]=Color.GREEN; objPrint[2]=new Rectangle(40+mX,40+mY,500,40); //ALSO sets location. Any shape can be used //example text
		 * object typePrint[3]= org.jpedal.render.DynamicVectorRenderer.STRING; org.jpedal.render.TextObject textPrintObject=new
		 * org.jpedal.render.TextObject(); //composite object so we can pass in parameters textPrintObject.x=40+mX; textPrintObject.y=40+mY;
		 * textPrintObject.text="Print Ex text on page "+pages; textPrintObject.font=new Font("Serif",Font.PLAIN,48); colorsPrint[3]=Color.BLUE;
		 * objPrint[3]=textPrintObject; //ALSO sets location //pass into JPEDAL after page decoded - will be removed automatically on new page/open
		 * file //BUT PRINTING retains values until manually removed try{ decode_pdf.printAdditionalObjectsOverPage(pages,typePrint ,colorsPrint,
		 * objPrint); }catch(PdfException e){ e.printStackTrace(); } } /*
		 */

		// <link><a name="global_print" />
		// global printout
		/*
		 * int count = 1; // Due to the way some pdf's are created it is necessery to take the offset of a page // into account when addding custom
		 * objects to the page. Variables medX and medY represent // that offset and need to be taken in to account when placing any additional object
		 * // on a page. int medX = decode_pdf.getPdfPageData().getMediaBoxX(1); int medY = decode_pdf.getPdfPageData().getMediaBoxY(1); int[]
		 * typePrint=new int[count]; Color[] colorsPrint=new Color[count]; Object[] objPrint=new Object[count]; //example custom (from version 3.40)
		 * typePrint[0]=org.jpedal.render.DynamicVectorRenderer.CUSTOM; JPedalCustomDrawObject exampleGlobalPrintObj=new
		 * ExampleCustomDrawObject(JPedalCustomDrawObject.ALLPAGES); exampleGlobalPrintObj.setMedX(medX); exampleGlobalPrintObj.setMedY(medY);
		 * //JPedalCustomDrawObject examplePrintObj=new ExampleCustomDrawObject(); objPrint[0]=exampleGlobalPrintObj; //pass into JPEDAL after page
		 * decoded - will be removed automatically on new page/open file //BUT PRINTING retains values until manually removed try{
		 * decode_pdf.printAdditionalObjectsOverAllPages(typePrint ,colorsPrint, objPrint); }catch(PdfException e){ e.printStackTrace(); }/*
		 */

		if (this.displayPane != null) reinitialiseTabs(this.displayPane.getDividerLocation() > startSize);

		this.finishedDecoding = true;

		// scrollPane.updateUI();

		zoom(false);

		this.decode_pdf.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

		// return null;
		// }
		// };

		// worker.start();

		// zoom(false);
		/**/
	}

	// <link><a name="listen" />

	/** this method adds listeners to GUI widgets to track changes */
	public void addFormsListeners() {

		// rest forms changed flag to show no changes
		this.commonValues.setFormsChanged(false);

		/** see if flag set - not default behaviour */
		boolean showMessage = false;
		String formsFlag = System.getProperty("org.jpedal.listenforms");
		if (formsFlag != null) showMessage = true;

		// get the form renderer which also contains the processed form data.
		// if you want simple form data, also look at the ExtractFormDataAsObject.java example
		org.jpedal.objects.acroforms.rendering.AcroRenderer formRenderer = this.decode_pdf.getFormRenderer();

		if (formRenderer == null) return;

		// get list of forms on page
		java.util.List formsOnPage = null;

		/**
		 * once you have the name you can also use formRenderer.getComponentsByName(String objectName) to get all componentonce you know the name -
		 * return Object[] as can have multiple values (ie radio/check boxes)
		 * 
		 */
		try {
			formsOnPage = formRenderer.getComponentNameList(this.commonValues.getCurrentPage());
		}
		catch (PdfException e) {

			LogWriter.writeLog("Exception " + e + " reading component list");
		}

		// allow for no forms
		if (formsOnPage == null) {

			if (showMessage) showMessageDialog(Messages.getMessage("PdfViewer.NoFields"));

			return;
		}

		int formCount = formsOnPage.size();

		JPanel formPanel = new JPanel();
		/**
		 * create a JPanel to list forms and tell user a box example
		 **/
		if (showMessage) {
			formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
			JLabel formHeader = new JLabel("This page contains " + formCount + " form objects");
			formHeader.setFont(this.headFont);
			formPanel.add(formHeader);

			formPanel.add(Box.createRigidArea(new Dimension(10, 10)));
			JTextPane instructions = new JTextPane();
			instructions.setPreferredSize(new Dimension(450, 180));
			instructions.setEditable(false);
			instructions.setText("This provides a simple example of Forms handling. We have"
					+ " added a listener to each form so clicking on it shows the form name.\n\n"
					+ "Code is in addExampleListeners() in org.examples.viewer.Viewer\n\n"
					+ "This could be easily be extended to interface with a database directly "
					+ "or collect results on an action and write back using itext.\n\n"
					+ "Forms have been converted into Swing components and are directly accessible" + " (as is the original data).\n\n"
					+ "If you don't like the standard SwingSet you can replace with your own set.");
			instructions.setFont(this.textFont);
			formPanel.add(instructions);
			formPanel.add(Box.createRigidArea(new Dimension(10, 10)));
		}

		/**
		 * pop-up to show forms on page
		 **/
		if (showMessage) {
			final JDialog displayFrame = new JDialog((JFrame) null, true);
			if (this.commonValues.getModeOfOperation() != Values.RUNNING_APPLET) {
				displayFrame.setLocationRelativeTo(null);
				displayFrame.setLocation(this.frame.getLocationOnScreen().x + 10, this.frame.getLocationOnScreen().y + 10);
			}

			JScrollPane scroll = new JScrollPane();
			scroll.getViewport().add(formPanel);
			scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

			displayFrame.setSize(500, 500);
			displayFrame.setTitle("List of forms on this page");
			displayFrame.getContentPane().setLayout(new BorderLayout());
			displayFrame.getContentPane().add(scroll, BorderLayout.CENTER);

			JPanel buttonBar = new JPanel();
			buttonBar.setLayout(new BorderLayout());
			displayFrame.getContentPane().add(buttonBar, BorderLayout.SOUTH);

			// close option just removes display
			JButton no = new JButton(Messages.getMessage("PdfViewerButton.Close"));
			no.setFont(new Font("SansSerif", Font.PLAIN, 12));
			buttonBar.add(no, BorderLayout.EAST);
			no.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					displayFrame.dispose();
				}
			});

			/** show the popup */
			displayFrame.setVisible(true);
		}
	}

	/**
	 * put the outline data into a display panel which we can pop up for the user - outlines, thumbnails
	 * 
	 * private void createOutlinePanels() {
	 * 
	 * //boolean hasNavBars=false;
	 * 
	 * // set up first 10 thumbnails by default. Rest created as needed.
	 * 
	 * //add if statement or comment out this section to remove thumbnails setupThumbnailPanel();
	 * 
	 * // add any outline
	 * 
	 * setBookmarks(false);
	 * 
	 * /** resize to show if there are nav bars
	 * 
	 * if(hasNavBars){ if(!thumbnails.isShownOnscreen()){ if( !commonValues.isContentExtractor()) navOptionsPanel.setVisible(true);
	 * displayPane.setDividerLocation(divLocation); //displayPane.invalidate(); //displayPane.repaint();
	 * 
	 * } } }/
	 **/

	// <start-thin>
	public void setupThumbnailPanel() {

		this.decode_pdf.addExternalHandler(this.thumbnails, Options.ThumbnailHandler);

		if (this.isSetup) return;

		this.isSetup = true;

		if (this.thumbnails.isShownOnscreen()) {

			int pages = this.decode_pdf.getPageCount();

			// setup and add to display

			this.thumbnails.setupThumbnails(pages, this.textFont, Messages.getMessage("PdfViewerPageLabel.text"), this.decode_pdf.getPdfPageData());

			// add listener so clicking on button changes to page - has to be in Viewer so it can update it
			Object[] buttons = this.thumbnails.getButtons();
			for (int i = 0; i < pages; i++)
				((JButton) buttons[i]).addActionListener(new PageChanger(i));

			// add global listener
			this.thumbnails.addComponentListener();

		}
	}

	// <end-thin>

	public void setBookmarks(boolean alwaysGenerate) {

		// ignore if not opened
		int currentSize = this.displayPane.getDividerLocation();

		if ((currentSize == startSize) && !alwaysGenerate) return;

		// ignore if already done and flag
		if (this.bookmarksGenerated) {
			return;
		}
		this.bookmarksGenerated = true;

		org.w3c.dom.Document doc = this.decode_pdf.getOutlineAsXML();

		Node rootNode = null;
		if (doc != null) rootNode = doc.getFirstChild();

		if (rootNode != null) {

			this.tree.reset(rootNode);

			// Listen for when the selection changes - looks up dests at present
			((JTree) this.tree.getTree()).addTreeSelectionListener(new TreeSelectionListener() {

				/** Required by TreeSelectionListener interface. */
				@Override
				public void valueChanged(TreeSelectionEvent e) {

					DefaultMutableTreeNode node = SwingGUI.this.tree.getLastSelectedPathComponent();

					if (node == null) return;

					/** get title and open page if valid */
					// String title=(String)node.getUserObject();

					JTree jtree = ((JTree) SwingGUI.this.tree.getTree());

					DefaultTreeModel treeModel = (DefaultTreeModel) jtree.getModel();

					List<TreeNode> flattenedTree = new ArrayList<TreeNode>();

					/** flatten out the tree so we can find the index of the selected node */
					getFlattenedTreeNodes((TreeNode) treeModel.getRoot(), flattenedTree);
					flattenedTree.remove(0); // remove the root node as we don't account for this

					int index = flattenedTree.indexOf(node);

					// String page = tree.getPageViaNodeNumber(index);
					String ref = SwingGUI.this.tree.convertNodeIDToRef(index);

					PdfObject Aobj = SwingGUI.this.decode_pdf.getOutlineData().getAobj(ref);

					// handle in our handler code
					if (Aobj != null) {
						/* int pageToDisplay= */SwingGUI.this.decode_pdf.getFormRenderer().getActionHandler()
								.gotoDest(Aobj, ActionHandler.MOUSECLICKED, PdfDictionary.Dest);

						// align to viewer knows if page changed and set rotation to default for page
						// if(pageToDisplay!=-1){
						// currentCommands.gotoPage(Integer.toString(pageToDisplay));
						// commonValues.setCurrentPage(pageToDisplay);
						// setSelectedComboIndex(Commands.ROTATION, decode_pdf.getPdfPageData().getRotation(pageToDisplay)/90);
						// }
					}

					// if((page==null)||(page.length()==0))
					// page=tree.getPage(title);
					//
					// if(page!=null && page.length()>0){
					// int pageToDisplay=Integer.parseInt(page);
					//
					// if((!commonValues.isProcessing())&&(commonValues.getCurrentPage()!=pageToDisplay)){
					// commonValues.setCurrentPage(pageToDisplay);
					// /**reset as rotation may change!*/
					//
					// decode_pdf.setPageParameters(getScaling(), commonValues.getCurrentPage());
					// decodePage(false);
					// }
					//
					// //Point p= tree.getPoint(title);
					// //if(p!=null)
					// // decode_pdf.ensurePointIsVisible(p);
					//
					// }else{
					// showMessageDialog(Messages.getMessage("PdfViewerError.NoBookmarkLink")+title);
					// System.out.println("No dest page set for "+title);
					// }
				}
			});

		}
		else {
			this.tree.reset(null);
		}
	}

	private static void getFlattenedTreeNodes(TreeNode theNode, List<TreeNode> items) {
		// add the item
		items.add(theNode);

		// recursion
		for (Enumeration theChildren = theNode.children(); theChildren.hasMoreElements();) {
			getFlattenedTreeNodes((TreeNode) theChildren.nextElement(), items);
		}
	}

	private void selectBookmark() {
		if (this.decode_pdf.hasOutline() && (this.tree != null)) this.tree.selectBookmark();
	}

	/*
	 * (non-Javadoc)
	 * @see org.jpedal.examples.viewer.gui.swing.GUIFactory#initStatus()
	 */
	@Override
	public void initStatus() {
		this.decode_pdf.setStatusBarObject(this.statusBar);

		// <start-wrap>
		// and initialise the display
		if (!this.useNewLayout) this.comboBoxBar.add(this.statusBar.getStatusObject());
		else setMultibox(new int[] {});
		// <end-wrap>
	}

	/*
	 * (non-Javadoc)
	 * @see org.jpedal.examples.viewer.gui.swing.GUIFactory#initThumbnails(int, org.jpedal.utils.repositories.Vector_Int)
	 */
	@Override
	public void initThumbnails(int itemSelectedCount, Vector_Int pageUsed) {

		this.navOptionsPanel.removeAll();
		if (this.thumbnails.isShownOnscreen()) this.thumbnails.setupThumbnails(itemSelectedCount - 1, pageUsed.get(),
				this.commonValues.getPageCount());

		if (DecoderOptions.isRunningOnMac) {
			this.navOptionsPanel.add((Component) this.thumbnails, "Extracted items");
		}
		else {
			VTextIcon textIcon2 = new VTextIcon(this.navOptionsPanel, "Extracted items", VTextIcon.ROTATE_LEFT);
			this.navOptionsPanel.addTab(null, textIcon2, (Component) this.thumbnails);
		}

		this.displayPane.setDividerLocation(150);
	}

	// moved to method so we can call it from the already added actions listeners on the forms
	public void checkformSavedMessage() {
		String propValue = this.properties.getValue("showsaveformsmessage");
		boolean showSaveFormsMessage = false;

		if (propValue.length() > 0) showSaveFormsMessage = propValue.equals("true");

		if (showSaveFormsMessage && this.firstTimeFormMessage && this.commonValues.isFormsChanged() == false) {
			this.firstTimeFormMessage = false;

			JPanel panel = new JPanel();
			panel.setLayout(new GridBagLayout());
			final GridBagConstraints p = new GridBagConstraints();

			p.anchor = GridBagConstraints.WEST;
			p.gridx = 0;
			p.gridy = 0;
			String str = (Messages.getMessage("PdfViewerFormsWarning.ChangedFormsValue"));
			if (!this.commonValues.isItextOnClasspath()) str = (Messages.getMessage("PdfViewerFormsWarning.ChangedFormsValueNoItext"));

			JCheckBox cb = new JCheckBox();
			cb.setText(Messages.getMessage("PdfViewerFormsWarning.CheckBox"));
			Font font = cb.getFont();

			JTextArea ta = new JTextArea(str);
			ta.setOpaque(false);
			ta.setFont(font);

			p.ipady = 20;
			panel.add(ta, p);

			p.ipady = 0;
			p.gridy = 1;
			panel.add(cb, p);

			JOptionPane.showMessageDialog(this.frame, panel);

			if (cb.isSelected()) this.properties.setValue("showsaveformsmessage", "false");

		}
		this.commonValues.setFormsChanged(true);
		setViewerTitle(null);
	}

	/*
	 * (non-Javadoc)
	 * @see org.jpedal.examples.viewer.gui.swing.GUIFactory#setCoordText(java.lang.String)
	 */
	@Override
	public void setCoordText(String string) {
		this.coords.setText(string);
	}

	private JLabel initCoordBox() {

		this.coords.setBackground(Color.white);
		this.coords.setOpaque(true);

		if (this.useNewLayout) {
			this.coords.setBorder(BorderFactory.createEtchedBorder());
			this.coords.setPreferredSize(this.memoryBar.getPreferredSize());
		}
		else {
			this.coords.setPreferredSize(new Dimension(120, 20));
			this.coords.setBorder(BorderFactory.createLineBorder(Color.black, 1));
		}

		this.coords.setText("  X: " + " Y: " + ' ' + ' ');

		return this.coords;
	}

	// When page changes make sure only relevant navigation buttons are displayed
	public void hideRedundentNavButtons() {

		int maxPages = this.decode_pdf.getPageCount();
		if (this.commonValues.isMultiTiff()) {
			maxPages = this.commonValues.getPageCount();
		}

		if (((this.decode_pdf.getDisplayView() == Display.FACING && this.currentCommands.getPages().getBoolean(Display.BoolValue.SEPARATE_COVER)) || this.decode_pdf
				.getDisplayView() == Display.CONTINUOUS_FACING) && (maxPages & 1) == 1) maxPages--;

		if (this.commonValues.getCurrentPage() == 1) setBackNavigationButtonsEnabled(false);
		else setBackNavigationButtonsEnabled(true);

		if (this.commonValues.getCurrentPage() == maxPages) setForwardNavigationButtonsEnabled(false);
		else setForwardNavigationButtonsEnabled(true);

		// update single mode toolbar to be visible in only SINGLE if set
		if (this.thumbscroll != null) {
			if (this.decode_pdf.getDisplayView() == Display.SINGLE_PAGE) {

				this.scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
				this.scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

				this.thumbscroll.setVisible(true);
			}
			else
				if (this.decode_pdf.getDisplayView() == Display.PAGEFLOW) {

					this.scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
					this.scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

					this.thumbscroll.setVisible(false);
				}
				else {

					this.scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
					this.scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

					this.thumbscroll.setVisible(false);
				}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.jpedal.examples.viewer.gui.swing.GUIFactory#setPageNumber()
	 */
	@Override
	public void setPageNumber() {

		if (SwingUtilities.isEventDispatchThread()) setPageNumberWorker();
		else {
			Runnable r = new Runnable() {
				@Override
				public void run() {
					setPageNumberWorker();
				}
			};
			SwingUtilities.invokeLater(r);
		}
	}

	public boolean isMultiPageTiff() {
		return this.isMultiPageTiff;
	}

	public void setMultiPageTiff(boolean isMultiPageTiff) {
		this.isMultiPageTiff = isMultiPageTiff;
	}

	/*
	 * (non-Javadoc)
	 * @see org.jpedal.examples.viewer.gui.swing.GUIFactory#setPageNumber()
	 */
	private void setPageNumberWorker() {

		if (!this.decode_pdf.isOpen() && !this.isMultiPageTiff) {
			this.pageCounter2.setText(" ");
		}
		else {

			if (this.previewOnSingleScroll && this.thumbscroll != null) {

				// hack because Listern does not f******g work on windows
				this.scrollListener.ignoreChange = true;
				this.thumbscroll.setMaximum(this.decode_pdf.getPageCount());
				this.scrollListener.ignoreChange = true;
				this.thumbscroll.setValue(this.commonValues.getCurrentPage() - 1);
				this.scrollListener.ignoreChange = false;
				if (this.debugThumbnail) System.out.println("setpage=" + this.commonValues.getCurrentPage());
			}

			int currentPage = this.commonValues.getCurrentPage();
			if (this.decode_pdf.getDisplayView() == Display.FACING || this.decode_pdf.getDisplayView() == Display.CONTINUOUS_FACING) {
				if (this.decode_pdf.getPageCount() == 2) this.pageCounter2.setText("1/2");
				else
					if (this.currentCommands.getPages().getBoolean(Display.BoolValue.SEPARATE_COVER)
							|| this.decode_pdf.getDisplayView() == Display.CONTINUOUS_FACING) {
						int base = currentPage & -2;
						if (base != this.decode_pdf.getPageCount() && base != 0) this.pageCounter2.setText(base + "/" + (base + 1));
						else this.pageCounter2.setText(String.valueOf(currentPage));
					}
					else {
						int base = currentPage - (1 - (currentPage & 1));
						if (base != this.decode_pdf.getPageCount()) this.pageCounter2.setText(base + "/" + (base + 1));
						else this.pageCounter2.setText(String.valueOf(currentPage));
					}

			}
			else {
				this.pageCounter2.setText(String.valueOf(currentPage));
			}
			this.pageCounter3.setText(Messages.getMessage("PdfViewerOfLabel.text") + ' ' + this.commonValues.getPageCount()); //$NON-NLS-1$
			hideRedundentNavButtons();
		}
	}

	public int getPageNumber() {
		return this.commonValues.getCurrentPage();
	}

	/**
	 * note - to plugin put all on single line so addButton values over-riddern
	 */
	private void createNavbar() {

		List v = new ArrayList();

		// <start-pdfhelp>
		if (this.memoryMonitor == null) { // ensure only 1 instance
			this.memoryMonitor = new Timer(500, new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent event) {
					int free = (int) (Runtime.getRuntime().freeMemory() / (1024 * 1024));
					int total = (int) (Runtime.getRuntime().totalMemory() / (1024 * 1024));

					// this broke the image saving when it was run every time
					if (SwingGUI.this.finishedDecoding) {
						SwingGUI.this.finishedDecoding = false;
					}

					// System.out.println((Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/1000);
					SwingGUI.this.memoryBar.setMaximum(total);
					SwingGUI.this.memoryBar.setValue(total - free);
					SwingGUI.this.memoryBar.setStringPainted(true);
					SwingGUI.this.memoryBar.setString((total - free) + "M of " + total + 'M');
				}
			});
			this.memoryMonitor.start();
		}

		if (!this.useNewLayout) {
			this.navButtons.add(this.memoryBar, BorderLayout.WEST);
		}
		else {
			this.multibox.setLayout(new BorderLayout());

			if (this.commonValues.getModeOfOperation() != Values.RUNNING_PLUGIN) this.navButtons.add(this.multibox, BorderLayout.WEST);
		}
		// <end-pdfhelp>
		this.navButtons.add(Box.createHorizontalGlue());

		/**
		 * navigation toolbar for moving between pages
		 */
		this.navToolBar.add(Box.createHorizontalGlue());

		addButton(NAVBAR, Messages.getMessage("PdfViewerNavBar.RewindToStart"), this.iconLocation + "start.gif", Commands.FIRSTPAGE);

		addButton(NAVBAR, Messages.getMessage("PdfViewerNavBar.Rewind10"), this.iconLocation + "fback.gif", Commands.FBACKPAGE);

		addButton(NAVBAR, Messages.getMessage("PdfViewerNavBar.Rewind1"), this.iconLocation + "back.gif", Commands.BACKPAGE);

		/** put page count in middle of forward and back */
		this.pageCounter1 = new JLabel(Messages.getMessage("PdfViewerPageLabel.text"));
		this.pageCounter1.setOpaque(false);
		this.navToolBar.add(this.pageCounter1);
		// pageCounter2.setMaximumSize(new Dimension(5,50));
		this.navToolBar.add(this.pageCounter2);
		this.navToolBar.add(this.pageCounter3);

		addButton(NAVBAR, Messages.getMessage("PdfViewerNavBar.Forward1"), this.iconLocation + "forward.gif", Commands.FORWARDPAGE);

		addButton(NAVBAR, Messages.getMessage("PdfViewerNavBar.Forward10"), this.iconLocation + "fforward.gif", Commands.FFORWARDPAGE);

		addButton(NAVBAR, Messages.getMessage("PdfViewerNavBar.ForwardLast"), this.iconLocation + "end.gif", Commands.LASTPAGE);

		this.navToolBar.add(Box.createHorizontalGlue());

		Dimension size;

		// <start-pdfhelp>
		size = new Dimension(110, 0);
		javax.swing.Box.Filler filler = new Box.Filler(size, size, size);
		this.navButtons.add(filler, BorderLayout.EAST);
		/**/

		if (this.useNewLayout) this.multibox.setPreferredSize(size);
		else this.memoryBar.setPreferredSize(size);
		// <end-pdfhelp>

		boolean[] defaultValues = new boolean[v.size()];
		for (int i = 0; i != v.size(); i++) {
			if (v.get(i).equals(Boolean.TRUE)) {
				defaultValues[i] = true;
			}
			else {
				defaultValues[i] = false;
			}
		}

		// p.setNavDefaults(defaultValues);

		// on top in plugin
		if (this.commonValues.getModeOfOperation() == Values.RUNNING_PLUGIN) this.topButtons.add(this.navToolBar, BorderLayout.CENTER);
		else this.navButtons.add(this.navToolBar, BorderLayout.CENTER);
	}

	@Override
	public void setPage(int page) {

		if (((this.decode_pdf.getDisplayView() == Display.FACING && this.currentCommands.getPages().getBoolean(Display.BoolValue.SEPARATE_COVER)) || this.decode_pdf
				.getDisplayView() == Display.CONTINUOUS_FACING) && (page & 1) == 1 && page != 1) {
			page--;
		}
		else
			if (this.decode_pdf.getDisplayView() == Display.FACING && !this.currentCommands.getPages().getBoolean(Display.BoolValue.SEPARATE_COVER)
					&& (page & 1) == 0) {
				page--;
			}

		this.commonValues.setCurrentPage(page);
		setPageNumber();
		// Page changed so save this page as last viewed
		setThumbnails();
	}

	public void resetPageNav() {
		this.pageCounter2.setText("");
		this.pageCounter3.setText("");
	}

	public void setRotation() {
		// PdfPageData currentPageData=decode_pdf.getPdfPageData();
		// rotation=currentPageData.getRotation(commonValues.getCurrentPage());

		// Broke files with when moving from rotated page to non rotated.
		// The pages help previous rotation
		// rotation = (rotation + (getSelectedComboIndex(Commands.ROTATION)*90));

		if (this.rotation > 360) this.rotation = this.rotation - 360;

		if (getSelectedComboIndex(Commands.ROTATION) != (this.rotation / 90)) {
			setSelectedComboIndex(Commands.ROTATION, (this.rotation / 90));
		}
		else
			if (!Values.isProcessing()) {
				this.decode_pdf.repaint();
			}
	}

	public void setRotationFromExternal(int rot) {
		this.rotation = rot;
		this.rotationBox.setSelectedIndex(this.rotation / 90);
		if (!Values.isProcessing()) {
			this.decode_pdf.repaint();
		}
	}

	public void setScalingFromExternal(String scale) {

		if (scale.startsWith("Fit ")) { // allow for Fit Page, Fit Width, Fit Height
			this.scalingBox.setSelectedItem(scale);
		}
		else {
			this.scaling = Float.parseFloat(scale);
			this.scalingBox.setSelectedItem(scale + '%');
		}

		if (!Values.isProcessing()) {
			this.decode_pdf.repaint();
		}
	}

	public void createMainMenu(boolean includeAll) {

		String addSeparator = "";
		// <start-wrap>

		this.fileMenu = new JMenu(Messages.getMessage("PdfViewerFileMenu.text"));

		addToMainMenu(this.fileMenu);

		/**
		 * add open options
		 **/

		this.openMenu = new JMenu(Messages.getMessage("PdfViewerFileMenuOpen.text"));
		this.fileMenu.add(this.openMenu);

		addMenuItem(this.openMenu, Messages.getMessage("PdfViewerFileMenuOpen.text"), Messages.getMessage("PdfViewerFileMenuTooltip.open"),
				Commands.OPENFILE);

		addMenuItem(this.openMenu, Messages.getMessage("PdfViewerFileMenuOpenurl.text"), Messages.getMessage("PdfViewerFileMenuTooltip.openurl"),
				Commands.OPENURL);

		addSeparator = this.properties.getValue("Save") + this.properties.getValue("Resaveasforms") + this.properties.getValue("Find");
		if (addSeparator.length() > 0 && addSeparator.toLowerCase().contains("true")) {
			this.fileMenu.addSeparator();
		}

		addMenuItem(this.fileMenu, Messages.getMessage("PdfViewerFileMenuSave.text"), Messages.getMessage("PdfViewerFileMenuTooltip.save"),
				Commands.SAVE);

		// not set if I just run from jar as no IText....
		if (includeAll && this.commonValues.isItextOnClasspath()) addMenuItem(this.fileMenu,
				Messages.getMessage("PdfViewerFileMenuResaveForms.text"), Messages.getMessage("PdfViewerFileMenuTooltip.saveForms"),
				Commands.SAVEFORM);

		// Remember to finish this off
		addMenuItem(this.fileMenu, Messages.getMessage("PdfViewerFileMenuFind.text"), Messages.getMessage("PdfViewerFileMenuTooltip.find"),
				Commands.FIND);

		// =====================

		addSeparator = this.properties.getValue("Documentproperties");
		if (addSeparator.length() > 0 && addSeparator.toLowerCase().equals("true")) {
			this.fileMenu.addSeparator();
		}
		addMenuItem(this.fileMenu, Messages.getMessage("PdfViewerFileMenuDocProperties.text"), Messages.getMessage("PdfViewerFileMenuTooltip.props"),
				Commands.DOCINFO);

		// @SIGNING

		addSeparator = this.properties.getValue("Print");
		if (addSeparator.length() > 0 && addSeparator.toLowerCase().equals("true")) {
			this.fileMenu.addSeparator();
		}
		addMenuItem(this.fileMenu, Messages.getMessage("PdfViewerFileMenuPrint.text"), Messages.getMessage("PdfViewerFileMenuTooltip.print"),
				Commands.PRINT);

		addSeparator = this.properties.getValue("Recentdocuments");
		if (addSeparator.length() > 0 && addSeparator.toLowerCase().equals("true")) {
			this.fileMenu.addSeparator();
			this.currentCommands.recentDocumentsOption(this.fileMenu);
		}

		addSeparator = this.properties.getValue("Exit");
		if (addSeparator.length() > 0 && addSeparator.toLowerCase().equals("true")) {
			this.fileMenu.addSeparator();
		}
		addMenuItem(this.fileMenu, Messages.getMessage("PdfViewerFileMenuExit.text"), Messages.getMessage("PdfViewerFileMenuTooltip.exit"),
				Commands.EXIT);

		// EDIT MENU
		this.editMenu = new JMenu(Messages.getMessage("PdfViewerEditMenu.text"));
		addToMainMenu(this.editMenu);

		addMenuItem(this.editMenu, Messages.getMessage("PdfViewerEditMenuCopy.text"), Messages.getMessage("PdfViewerEditMenuTooltip.Copy"),
				Commands.COPY);

		addMenuItem(this.editMenu, Messages.getMessage("PdfViewerEditMenuSelectall.text"), Messages.getMessage("PdfViewerEditMenuTooltip.Selectall"),
				Commands.SELECTALL);

		addMenuItem(this.editMenu, Messages.getMessage("PdfViewerEditMenuDeselectall.text"),
				Messages.getMessage("PdfViewerEditMenuTooltip.Deselectall"), Commands.DESELECTALL);

		addSeparator = this.properties.getValue("Preferences");
		if (addSeparator.length() > 0 && addSeparator.toLowerCase().equals("true")) {
			this.editMenu.addSeparator();
		}
		addMenuItem(this.editMenu, Messages.getMessage("PdfViewerEditMenuPreferences.text"),
				Messages.getMessage("PdfViewerEditMenuTooltip.Preferences"), Commands.PREFERENCES);

		this.viewMenu = new JMenu(Messages.getMessage("PdfViewerViewMenu.text"));
		addToMainMenu(this.viewMenu);

		this.goToMenu = new JMenu(Messages.getMessage("GoToViewMenuGoto.text"));
		this.viewMenu.add(this.goToMenu);

		addMenuItem(this.goToMenu, Messages.getMessage("GoToViewMenuGoto.FirstPage"), "", Commands.FIRSTPAGE);

		addMenuItem(this.goToMenu, Messages.getMessage("GoToViewMenuGoto.BackPage"), "", Commands.BACKPAGE);

		addMenuItem(this.goToMenu, Messages.getMessage("GoToViewMenuGoto.ForwardPage"), "", Commands.FORWARDPAGE);

		addMenuItem(this.goToMenu, Messages.getMessage("GoToViewMenuGoto.LastPage"), "", Commands.LASTPAGE);

		addMenuItem(this.goToMenu, Messages.getMessage("GoToViewMenuGoto.GoTo"), "", Commands.GOTO);

		addSeparator = this.properties.getValue("Previousdocument") + this.properties.getValue("Nextdocument");
		if (addSeparator.length() > 0 && addSeparator.toLowerCase().contains("true")) {
			this.goToMenu.addSeparator();
		}

		addMenuItem(this.goToMenu, Messages.getMessage("GoToViewMenuGoto.PreviousDoucment"), "", Commands.PREVIOUSDOCUMENT);

		addMenuItem(this.goToMenu, Messages.getMessage("GoToViewMenuGoto.NextDoucment"), "", Commands.NEXTDOCUMENT);

		/**
		 * add page layout
		 **/
		// if(properties.getValue("PageLayoutMenu").toLowerCase().equals("true")){

		// <end-wrap>

		String[] descriptions = { Messages.getMessage("PageLayoutViewMenu.SinglePage"), Messages.getMessage("PageLayoutViewMenu.Continuous"),
				Messages.getMessage("PageLayoutViewMenu.Facing"), Messages.getMessage("PageLayoutViewMenu.ContinousFacing"),
				Messages.getMessage("PageLayoutViewMenu.PageFlow") };
		int[] value = { Display.SINGLE_PAGE, Display.CONTINUOUS, Display.FACING, Display.CONTINUOUS_FACING, Display.PAGEFLOW };

		if (this.isSingle) initLayoutMenus(this.pageLayoutMenu, descriptions, value);

		// <start-wrap>
		if (this.properties.getValue("separateCover").equals("true")) addMenuItem(this.viewMenu,
				Messages.getMessage("PdfViewerViewMenuSeparateCover.text"), Messages.getMessage("PdfViewerViewMenuTooltip.separateCover"),
				Commands.SEPARATECOVER, true);

		// addMenuItem(view,Messages.getMessage("PdfViewerViewMenuAutoscroll.text"),Messages.getMessage("PdfViewerViewMenuTooltip.autoscroll"),Commands.AUTOSCROLL);
		if (this.properties.getValue("panMode").equals("true")) addMenuItem(this.viewMenu, Messages.getMessage("PdfViewerViewMenuPanMode.text"),
				Messages.getMessage("PdfViewerViewMenuTooltip.panMode"), Commands.PANMODE);

		if (this.properties.getValue("textSelect").equals("true")) addMenuItem(this.viewMenu,
				Messages.getMessage("PdfViewerViewMenuTextSelectMode.text"), Messages.getMessage("PdfViewerViewMenuTooltip.textSelect"),
				Commands.TEXTSELECT);

		addSeparator = this.properties.getValue("Fullscreen");
		if (addSeparator.length() > 0 && addSeparator.toLowerCase().contains("true")) {
			this.goToMenu.addSeparator();
		}

		// full page mode
		addMenuItem(this.viewMenu, Messages.getMessage("PdfViewerViewMenuFullScreenMode.text"),
				Messages.getMessage("PdfViewerViewMenuTooltip.fullScreenMode"), Commands.FULLSCREEN);

		if (!this.isSingle) {
			this.windowMenu = new JMenu(Messages.getMessage("PdfViewerWindowMenu.text"));
			addToMainMenu(this.windowMenu);

			addMenuItem(this.windowMenu, Messages.getMessage("PdfViewerWindowMenuCascade.text"), "", Commands.CASCADE);

			addMenuItem(this.windowMenu, Messages.getMessage("PdfViewerWindowMenuTile.text"), "", Commands.TILE);

		}

		/**
		 * items options if IText available
		 */
		if (this.commonValues.isItextOnClasspath()) {
			this.pageToolsMenu = new JMenu(Messages.getMessage("PdfViewerPageToolsMenu.text"));
			addToMainMenu(this.pageToolsMenu);

			addMenuItem(this.pageToolsMenu, Messages.getMessage("PdfViewerPageToolsMenuRotate.text"), "", Commands.ROTATE);
			addMenuItem(this.pageToolsMenu, Messages.getMessage("PdfViewerPageToolsMenuDelete.text"), "", Commands.DELETE);
			addMenuItem(this.pageToolsMenu, Messages.getMessage("PdfViewerPageToolsMenuAddPage.text"), "", Commands.ADD);
			addMenuItem(this.pageToolsMenu, Messages.getMessage("PdfViewerPageToolsMenuAddHeaderFooter.text"), "", Commands.ADDHEADERFOOTER);
			addMenuItem(this.pageToolsMenu, Messages.getMessage("PdfViewerPageToolsMenuStampText.text"), "", Commands.STAMPTEXT);
			addMenuItem(this.pageToolsMenu, Messages.getMessage("PdfViewerPageToolsMenuStampImage.text"), "", Commands.STAMPIMAGE);
			addMenuItem(this.pageToolsMenu, Messages.getMessage("PdfViewerPageToolsMenuSetCrop.text"), "", Commands.SETCROP);

		}

		this.helpMenu = new JMenu(Messages.getMessage("PdfViewerHelpMenu.text"));
		addToMainMenu(this.helpMenu);

		addMenuItem(this.helpMenu, Messages.getMessage("PdfViewerHelpMenu.VisitWebsite"), "", Commands.VISITWEBSITE);
		addMenuItem(this.helpMenu, Messages.getMessage("PdfViewerHelpMenuTip.text"), "", Commands.TIP);
		addMenuItem(this.helpMenu, Messages.getMessage("PdfViewerHelpMenuUpdates.text"), "", Commands.UPDATE);
		addMenuItem(this.helpMenu, Messages.getMessage("PdfViewerHelpMenuabout.text"), Messages.getMessage("PdfViewerHelpMenuTooltip.about"),
				Commands.INFO);

		// <end-wrap>
	}

	/*
	 * (non-Javadoc)
	 * @see org.jpedal.examples.viewer.gui.swing.GUIFactory#addToMainMenu(javax.swing.JMenu)
	 */
	@Override
	public void addToMainMenu(JMenu fileMenuList) {
		this.currentMenu.add(fileMenuList);
	}

	/*
	 * (non-Javadoc)
	 * @see org.jpedal.examples.viewer.gui.swing.GUIFactory#getFrame()
	 */
	@Override
	public Container getFrame() {
		return this.frame;
	}

	/*
	 * (non-Javadoc)
	 * @see org.jpedal.examples.viewer.gui.swing.GUIFactory#getTopButtonBar()
	 */
	@Override
	public JToolBar getTopButtonBar() {
		return this.topButtons;
	}

	public JToolBar getDisplaySettingsBar() {
		return this.comboBoxBar;
	}

	public JMenuBar getMenuBar() {
		return this.currentMenu;
	}

	/*
	 * (non-Javadoc)
	 * @see org.jpedal.examples.viewer.gui.swing.GUIFactory#showMessageDialog(java.lang.Object)
	 */
	@Override
	public void showMessageDialog(Object message1) {

		/**
		 * allow user to replace messages with our action
		 */
		boolean showMessage = true;

		// check user has not setup message and if we still show message
		if (this.customMessageHandler != null) showMessage = this.customMessageHandler.showMessage(message1);

		if (showMessage) JOptionPane.showMessageDialog(this.frame, message1);
	}

	/*
	 * (non-Javadoc)
	 * @see org.jpedal.examples.viewer.gui.swing.GUIFactory#showMessageDialog(java.lang.Object)
	 */
	public int showMessageDialog(Object message1, Object[] options, int selectedChoice) {

		int n = 0;
		/**
		 * allow user to replace messages with our action
		 */
		boolean showMessage = true;

		// check user has not setup message and if we still show message
		if (this.customMessageHandler != null) showMessage = this.customMessageHandler.showMessage(message1);

		if (showMessage) {
			n = JOptionPane.showOptionDialog(this.frame, message1, "Message", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null,
					options, options[selectedChoice]);
		}

		return n;
	}

	/*
	 * (non-Javadoc)
	 * @see org.jpedal.examples.viewer.gui.swing.GUIFactory#showMessageDialog(java.lang.Object, java.lang.String, int)
	 */
	@Override
	public void showMessageDialog(Object message, String title, int type) {

		/**
		 * allow user to replace messages with our action
		 */
		boolean showMessage = true;

		// check user has not setup message and if we still show message
		if (this.customMessageHandler != null) showMessage = this.customMessageHandler.showMessage(message);

		if (showMessage) JOptionPane.showMessageDialog(this.frame, message, title, type);
	}

	/*
	 * (non-Javadoc)
	 * @see org.jpedal.examples.viewer.gui.swing.GUIFactory#showInputDialog(java.lang.Object, java.lang.String, int)
	 */
	@Override
	public String showInputDialog(Object message, String title, int type) {

		/**
		 * allow user to replace messages with our action
		 */
		String returnMessage = null;

		// check user has not setup message and if we still show message
		if (this.customMessageHandler != null) returnMessage = this.customMessageHandler.requestInput(new Object[] { message, title, title });

		if (returnMessage == null) return JOptionPane.showInputDialog(this.frame, message, title, type);
		else return returnMessage;
	}

	/*
	 * (non-Javadoc)
	 * @see org.jpedal.examples.viewer.gui.swing.GUIFactory#showInputDialog(java.lang.String)
	 */
	@Override
	public String showInputDialog(String message) {

		/**
		 * allow user to replace messages with our action
		 */
		String returnMessage = null;

		// check user has not setup message and if we still show message
		if (this.customMessageHandler != null) returnMessage = this.customMessageHandler.requestInput(new String[] { message });

		if (returnMessage == null) return JOptionPane.showInputDialog(this.frame, message);
		else return returnMessage;
	}

	/*
	 * (non-Javadoc)
	 * @see org.jpedal.examples.viewer.gui.swing.GUIFactory#showOptionDialog(java.lang.Object, java.lang.String, int, int, java.lang.Object,
	 * java.lang.Object[], java.lang.Object)
	 */
	@Override
	public int showOptionDialog(Object displayValue, String message, int option, int type, Object icon, Object[] options, Object initial) {

		/**
		 * allow user to replace messages with our action
		 */
		int returnMessage = -1;

		// check user has not setup message and if we still show message
		if (this.customMessageHandler != null) returnMessage = this.customMessageHandler.requestConfirm(new Object[] { displayValue, message,
				String.valueOf(option), String.valueOf(type), icon, options, initial });

		if (returnMessage == -1) return JOptionPane.showOptionDialog(this.frame, displayValue, message, option, type, (Icon) icon, options, initial);
		else return returnMessage;
	}

	/*
	 * (non-Javadoc)
	 * @see org.jpedal.examples.viewer.gui.swing.GUIFactory#showConfirmDialog(java.lang.String, java.lang.String, int)
	 */
	@Override
	public int showConfirmDialog(String message, String message2, int option) {

		/**
		 * allow user to replace messages with our action
		 */
		int returnMessage = -1;

		// check user has not setup message and if we still show message
		if (this.customMessageHandler != null) returnMessage = this.customMessageHandler.requestConfirm(new Object[] { message, message2,
				String.valueOf(option) });

		if (returnMessage == -1) return JOptionPane.showConfirmDialog(this.frame, message, message2, option);
		else return returnMessage;
	}

	/*
	 * (non-Javadoc)
	 * @see org.jpedal.examples.viewer.gui.swing.GUIFactory#showOverwriteDialog(String file,boolean yesToAllPresent)
	 */
	@Override
	public int showOverwriteDialog(String file, boolean yesToAllPresent) {

		int n = -1;

		/**
		 * allow user to replace messages with our action and remove popup
		 */
		int returnMessage = -1;

		// check user has not setup message and if we still show message
		if (this.customMessageHandler != null) returnMessage = this.customMessageHandler.requestConfirm(new Object[] { file,
				String.valueOf(yesToAllPresent) });

		if (returnMessage != -1) return returnMessage;

		if (yesToAllPresent) {

			final Object[] buttonRowObjects = new Object[] { Messages.getMessage("PdfViewerConfirmButton.Yes"),
					Messages.getMessage("PdfViewerConfirmButton.YesToAll"), Messages.getMessage("PdfViewerConfirmButton.No"),
					Messages.getMessage("PdfViewerConfirmButton.Cancel") };

			n = JOptionPane.showOptionDialog(
					this.frame,
					file + '\n' + Messages.getMessage("PdfViewerMessage.FileAlreadyExists") + '\n'
							+ Messages.getMessage("PdfViewerMessage.ConfirmResave"), Messages.getMessage("PdfViewerMessage.Overwrite"),
					JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, buttonRowObjects, buttonRowObjects[0]);

		}
		else {
			n = JOptionPane.showOptionDialog(
					this.frame,
					file + '\n' + Messages.getMessage("PdfViewerMessage.FileAlreadyExists") + '\n'
							+ Messages.getMessage("PdfViewerMessage.ConfirmResave"), Messages.getMessage("PdfViewerMessage.Overwrite"),
					JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);
		}

		return n;
	}

	/*
	 * (non-Javadoc)
	 * @see org.jpedal.examples.viewer.gui.swing.GUIFactory#showMessageDialog(javax.swing.JTextArea)
	 */
	@Override
	public void showMessageDialog(JTextArea info) {

		/**
		 * allow user to replace messages with our action
		 */
		boolean showMessage = true;

		// check user has not setup message and if we still show message
		if (this.customMessageHandler != null) showMessage = this.customMessageHandler.showMessage(info);

		if (showMessage) JOptionPane.showMessageDialog(this.frame, info);
	}

	@Override
	public void showItextPopup() {

		JEditorPane p = new JEditorPane("text/html", "Itext is not on the classpath.<BR>" + "JPedal includes code to take advantage of itext and<BR>"
				+ "provide additional functionality with options<BR>" + "to spilt pdf files, and resave forms data<BR>"
				+ "\nItext website - <a href=http://itextpdf.com/>http://itextpdf.com</a>");
		p.setEditable(false);
		p.setOpaque(false);
		p.addHyperlinkListener(new HyperlinkListener() {
			@Override
			public void hyperlinkUpdate(HyperlinkEvent e) {
				if (e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED)) {
					try {
						BrowserLauncher.openURL("http://itextpdf.com/");
					}
					catch (IOException e1) {
						showMessageDialog(Messages.getMessage("PdfViewer.ErrorWebsite"));
					}
				}
			}
		});

		/**
		 * allow user to replace messages with our action
		 */
		boolean showMessage = true;

		// check user has not setup message and if we still show message
		if (this.customMessageHandler != null) showMessage = this.customMessageHandler.showMessage(p);

		if (showMessage) showMessageDialog(p);

		// Hack for 13 to make sure the message box is large enough to hold the message
		/**
		 * JOptionPane optionPane = new JOptionPane(); optionPane.setMessage(p); optionPane.setMessageType(JOptionPane.INFORMATION_MESSAGE);
		 * optionPane.setOptionType(JOptionPane.DEFAULT_OPTION);
		 * 
		 * JDialog dialog = optionPane.createDialog(frame, "iText"); dialog.pack(); dialog.setSize(400,200); dialog.setVisible(true); /
		 **/
	}

	@Override
	public void showFirstTimePopup() {

		// allow user to disable
		boolean showMessage = (this.customMessageHandler != null && this.customMessageHandler.showMessage("first time popup"))
				|| this.customMessageHandler == null;

		if (!showMessage || this.commonValues.getModeOfOperation() == Values.RUNNING_APPLET) return;

		try {
			final JPanel a = new JPanel();
			a.setLayout(new BoxLayout(a, BoxLayout.Y_AXIS));

			JLabel m1 = new JLabel(Messages.getMessage("PdfViewerGPL.message1"));
			JLabel m2 = new JLabel(Messages.getMessage("PdfViewerGPL.message2"));
			JLabel m3 = new JLabel(Messages.getMessage("PdfViewerGPL.message3"));

			m1.setFont(m1.getFont().deriveFont(Font.BOLD));

			m1.setAlignmentX(0.5f);
			m2.setAlignmentX(0.5f);
			m3.setAlignmentX(0.5f);

			a.add(m1);
			a.add(Box.createRigidArea(new Dimension(14, 14)));
			a.add(m2);
			a.add(m3);
			a.add(Box.createRigidArea(new Dimension(10, 10)));
			/**/// to help comment out code not needed in gpl demos.

			MouseAdapter supportListener = new MouseAdapter() {
				@Override
				public void mouseEntered(MouseEvent e) {
					if (SingleDisplay.allowChangeCursor) a.setCursor(new Cursor(Cursor.HAND_CURSOR));
				}

				@Override
				public void mouseExited(MouseEvent e) {
					if (SingleDisplay.allowChangeCursor) a.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
				}

				@Override
				public void mouseClicked(MouseEvent e) {
					try {
						BrowserLauncher.openURL(Messages.getMessage("PdfViewer.SupportLink.Link"));
					}
					catch (IOException e1) {
						showMessageDialog(Messages.getMessage("PdfViewer.ErrorWebsite"));
					}
				}
			};

			JLabel img = new JLabel(new ImageIcon(getClass().getResource("/org/jpedal/examples/viewer/res/supportScreenshot.png")));
			img.setBorder(BorderFactory.createRaisedBevelBorder());
			img.setAlignmentX(Component.CENTER_ALIGNMENT);
			img.addMouseListener(supportListener);
			a.add(img);

			JLabel supportLink = new JLabel("<html><center><u>" + Messages.getMessage("PdfViewer.SupportLink.Text1") + ' '
					+ Messages.getMessage("PdfViewer.SupportLink.Text2") + "</u></html>");
			supportLink.setMaximumSize(new Dimension(245, 60));
			supportLink.setForeground(Color.BLUE);
			supportLink.addMouseListener(supportListener);
			supportLink.setAlignmentX(Component.CENTER_ALIGNMENT);
			a.add(supportLink);
			a.add(Box.createRigidArea(new Dimension(10, 10)));

			JOptionPane.showMessageDialog(this.frame, a, Messages.getMessage("PdfViewerTitle.RunningFirstTime"), JOptionPane.PLAIN_MESSAGE);
		}
		catch (Exception e) {
			// JOptionPane.showMessageDialog(null, "caught an exception "+e);
			System.err.println(Messages.getMessage("PdfViewerFirstRunDialog.Error"));
		}
		catch (Error e) {
			// JOptionPane.showMessageDialog(null, "caught an error "+e);
			System.err.println(Messages.getMessage("PdfViewerFirstRunDialog.Error"));
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.jpedal.examples.viewer.gui.swing.GUIFactory#showConfirmDialog(java.lang.Object, java.lang.String, int, int)
	 */
	@Override
	public int showConfirmDialog(Object message, String title, int optionType, int messageType) {

		/**
		 * allow user to replace messages with our action
		 */
		int returnMessage = -1;

		// check user has not setup message and if we still show message
		if (this.customMessageHandler != null) returnMessage = this.customMessageHandler.requestConfirm(new Object[] { message, title,
				String.valueOf(optionType), String.valueOf(messageType) });

		if (returnMessage == -1) return JOptionPane.showConfirmDialog(this.frame, message, title, optionType, messageType);
		else return returnMessage;
	}

	/*
	 * (non-Javadoc)
	 * @see org.jpedal.examples.viewer.gui.swing.GUIFactory#updateStatusMessage(java.lang.String)
	 */
	public void setDownloadProgress(String message, int percentage) {
		this.downloadBar.setProgress(message, percentage);

		if (this.useNewLayout) setMultibox(new int[] {});
	}

	/*
	 * (non-Javadoc)
	 * @see org.jpedal.examples.viewer.gui.swing.GUIFactory#updateStatusMessage(java.lang.String)
	 */
	@Override
	public void updateStatusMessage(String message) {
		this.statusBar.updateStatus(message, 0);
	}

	/*
	 * (non-Javadoc)
	 * @see org.jpedal.examples.viewer.gui.swing.GUIFactory#resetStatusMessage(java.lang.String)
	 */
	@Override
	public void resetStatusMessage(String message) {
		this.statusBar.resetStatus(message);
	}

	/*
	 * (non-Javadoc)
	 * @see org.jpedal.examples.viewer.gui.swing.GUIFactory#setStatusProgress(int)
	 */
	@Override
	public void setStatusProgress(int size) {
		this.statusBar.setProgress(size);

		if (this.useNewLayout) setMultibox(new int[] {});
	}

	/*
	 * (non-Javadoc)
	 * @see org.jpedal.examples.viewer.gui.swing.GUIFactory#isPDFOutlineVisible()
	 */
	@Override
	public boolean isPDFOutlineVisible() {
		return this.navOptionsPanel.isVisible();
	}

	/*
	 * (non-Javadoc)
	 * @see org.jpedal.examples.viewer.gui.swing.GUIFactory#setPDFOutlineVisible(boolean)
	 */
	@Override
	public void setPDFOutlineVisible(boolean visible) {
		this.navOptionsPanel.setVisible(visible);
	}

	@Override
	public void setSplitDividerLocation(int size) {
		this.displayPane.setDividerLocation(size);
	}

	@Override
	public void setQualityBoxVisible(boolean visible) {}

	private void setThumbnails() {
		SwingWorker worker = new SwingWorker() {
			@Override
			public Object construct() {

				if (SwingGUI.this.thumbnails.isShownOnscreen()) {
					setupThumbnailPanel();

					if (SwingGUI.this.decode_pdf.getDisplayView() == Display.SINGLE_PAGE) SwingGUI.this.thumbnails
							.generateOtherVisibleThumbnails(SwingGUI.this.commonValues.getCurrentPage());
				}

				return null;
			}
		};
		worker.start();
	}

	public void setSearchText(JTextField searchText) {
		this.searchText = searchText;
	}

	public void setResults(SearchList results) {
		this.results = results;
	}

	public SearchList getResults() {
		return this.results;
	}

	/**
	 * Method incorrectly named. New named method is getNavigationBar()
	 * 
	 * @return JToolBar containing all navigation buttons
	 * 
	 *         public JToolBar getComboBar() { return navButtons; }/
	 **/

	public JToolBar getNavigationBar() {
		return this.navButtons;
	}

	public JTabbedPane getSideTabBar() {
		return this.navOptionsPanel;
	}

	public ButtonGroup getSearchLayoutGroup() {
		return this.searchLayoutGroup;
	}

	public void setSearchFrame(GUISearchWindow searchFrame) {
		this.searchFrame = searchFrame;
	}

	// <link><a name="exampledraw" />
	/**
	 * example of a custom draw object
	 * 
	 * private static class ExampleCustomDrawObject implements JPedalCustomDrawObject {
	 * 
	 * private boolean isVisible=true;
	 * 
	 * private int page = 0;
	 * 
	 * public int medX = 0; public int medY = 0;
	 * 
	 * 
	 * public ExampleCustomDrawObject(){
	 * 
	 * }
	 * 
	 * public ExampleCustomDrawObject(Integer option){
	 * 
	 * if(option.equals(JPedalCustomDrawObject.ALLPAGES)) page=-1; else throw new
	 * RuntimeException("Only valid setting is JPedalCustomDrawObject.ALLPAGES"); }
	 * 
	 * public int getPage(){ return page; }
	 * 
	 * 
	 * public void print(Graphics2D g2, int x) {
	 * 
	 * //custom code or just pass through if(page==x || page ==-1 || page==0) paint(g2); }
	 * 
	 * public void paint(Graphics2D g2) { if(isVisible){
	 * 
	 * //your code here
	 * 
	 * //if you alter something, put it back Paint paint=g2.getPaint();
	 * 
	 * //loud shape we can see g2.setPaint(Color.orange); g2.fillRect(100+medX,100+medY,100,100); // PDF co-ordinates due to transform
	 * 
	 * g2.setPaint(Color.RED); g2.drawRect(100+medX,100+medY,100,100); // PDF co-ordinates due to transform
	 * 
	 * //put back values g2.setPaint(paint); } }
	 * 
	 * /**example onto rotated page public void paint(Graphics2D g2) { if(isVisible){
	 * 
	 * //your code here
	 * 
	 * AffineTransform aff=g2.getTransform();
	 * 
	 * 
	 * //allow for 90 degrees - detect of G2 double[] matrix=new double[6]; aff.getMatrix(matrix);
	 * 
	 * //System.out.println("0="+matrix[0]+" 1="+matrix[1]+" 2="+matrix[2]+" 3="+matrix[3]+" 4="+matrix[4]+" 5="+matrix[5]); if(matrix[1]>0 &&
	 * matrix[2]>0){ //90
	 * 
	 * g2.transform(AffineTransform.getScaleInstance(-1, 1)); g2.transform(AffineTransform.getRotateInstance(90 *Math.PI/180));
	 * 
	 * //BOTH X and Y POSITIVE!!!! g2.drawString("hello world", 60,60); }else if(matrix[0]<0 && matrix[3]>0){ //180 degrees (origin now top right)
	 * g2.transform(AffineTransform.getScaleInstance(-1, 1));
	 * 
	 * g2.drawString("hello world", -560,60);//subtract cropW from first number to use standard values
	 * 
	 * }else if(matrix[1]<0 && matrix[2]<0){ //270
	 * 
	 * g2.transform(AffineTransform.getScaleInstance(-1, 1)); g2.transform(AffineTransform.getRotateInstance(-90 *Math.PI/180));
	 * 
	 * //BOTH X and Y NEGATIVE!!!! g2.drawString("hello world", -560,-60); //subtract CropW and CropH if you want standard values }else{ //0 degress
	 * g2.transform(AffineTransform.getScaleInstance(1, -1)); // X ONLY POSITIVE!!!! g2.drawString("hello world", 60,-60); }
	 * 
	 * //restore!!! g2.setTransform(aff); } }
	 * 
	 * 
	 * public void setVisible(boolean isVisible) { this.isVisible=isVisible; }
	 * 
	 * public void setMedX(int medX) { this.medX = medX; }
	 * 
	 * public void setMedY(int medY) { this.medY = medY; } }/
	 **/

	public void removeSearchWindow(boolean justHide) {
		this.searchFrame.removeSearchWindow(justHide);
	}

	public void showPreferencesDialog() {
		// JFrame frame = new JFrame("Preferences");
		// frame.getContentPane().setLayout(new BorderLayout());
		// frame.getContentPane().add("Center", p);
		// frame.pack();
		// frame.setLocation(100, 100);
		// frame.setVisible(true);

		// <start-pdfhelp>
		SwingProperties p = new SwingProperties();
		p.setParent(this.frame);

		// p.setHideGuiPartsDefaults(defaultValues);

		p.showPreferenceWindow(this);

		// <end-pdfhelp>
	}

	public void setFrame(Container frame) {
		this.frame = frame;
	}

	public void getRSSBox() {
		final JPanel panel = new JPanel();

		JPanel top = new JPanel();
		top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

		JPanel labelPanel = new JPanel();
		labelPanel.setLayout(new BoxLayout(labelPanel, BoxLayout.X_AXIS));
		labelPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		JLabel label = new JLabel("Click on the link below to load a web browser and sign up to our RSS feed.");
		label.setAlignmentX(Component.LEFT_ALIGNMENT);

		labelPanel.add(label);
		labelPanel.add(Box.createHorizontalGlue());

		top.add(labelPanel);

		JPanel linkPanel = new JPanel();
		linkPanel.setLayout(new BoxLayout(linkPanel, BoxLayout.X_AXIS));
		linkPanel.add(Box.createHorizontalGlue());
		linkPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		final JLabel url = new JLabel("<html><center>" + "http://www.jpedal.org/jpedal.rss");
		url.setAlignmentX(Component.LEFT_ALIGNMENT);

		url.setForeground(Color.blue);
		url.setHorizontalAlignment(SwingConstants.CENTER);

		// @kieran - cursor
		url.addMouseListener(new MouseListener() {
			@Override
			public void mouseEntered(MouseEvent e) {
				if (SingleDisplay.allowChangeCursor) panel.getTopLevelAncestor().setCursor(new Cursor(Cursor.HAND_CURSOR));
				url.setText("<html><center><a>http://www.jpedal.org/jpedal.rss</a></center>");
			}

			@Override
			public void mouseExited(MouseEvent e) {
				if (SingleDisplay.allowChangeCursor) panel.getTopLevelAncestor().setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
				url.setText("<html><center>http://www.jpedal.org/jpedal.rss");
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				try {
					BrowserLauncher.openURL("http://www.jpedal.org/jpedal.rss");
				}
				catch (IOException e1) {

					JPanel errorPanel = new JPanel();
					errorPanel.setLayout(new BoxLayout(errorPanel, BoxLayout.Y_AXIS));

					JLabel errorMessage = new JLabel("Your web browser could not be successfully loaded.  "
							+ "Please copy and paste the URL below, manually into your web browser.");
					errorMessage.setAlignmentX(Component.LEFT_ALIGNMENT);
					errorMessage.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

					JTextArea textArea = new JTextArea("http://www.jpedal.org/jpedal.rss");
					textArea.setEditable(false);
					textArea.setRows(5);
					textArea.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
					textArea.setAlignmentX(Component.LEFT_ALIGNMENT);

					errorPanel.add(errorMessage);
					errorPanel.add(textArea);

					showMessageDialog(errorPanel, "Error loading web browser", JOptionPane.PLAIN_MESSAGE);

				}
			}

			@Override
			public void mousePressed(MouseEvent e) {}

			@Override
			public void mouseReleased(MouseEvent e) {}
		});

		linkPanel.add(url);
		linkPanel.add(Box.createHorizontalGlue());
		top.add(linkPanel);

		JLabel image = new JLabel(new ImageIcon(getClass().getResource("/org/jpedal/examples/viewer/res/rss.png")));
		image.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));

		JPanel imagePanel = new JPanel();
		imagePanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		imagePanel.setLayout(new BoxLayout(imagePanel, BoxLayout.X_AXIS));
		imagePanel.add(Box.createHorizontalGlue());
		imagePanel.add(image);
		imagePanel.add(Box.createHorizontalGlue());

		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.add(top);
		panel.add(imagePanel);

		showMessageDialog(panel, "Subscribe to JPedal RSS Feed", JOptionPane.PLAIN_MESSAGE);
	}

	private void loadProperties() {

		try {
			Component[] c = this.comboBoxBar.getComponents();

			// default value used to load props
			boolean set = false;
			String propValue = "";

			// Disable entire section
			propValue = this.properties.getValue("sideTabBarCollapseLength");
			if (propValue.length() > 0) {
				int value = Integer.parseInt(propValue);
				startSize = value;
				reinitialiseTabs(false);
				// properties.setValue("sideTabBarCollapseLength", String.valueOf(value));
			}

			propValue = this.properties.getValue("ShowMenubar");
			set = propValue.length() > 0 && propValue.toLowerCase().equals("true");
			this.currentMenu.setEnabled(set);
			this.currentMenu.setVisible(set);
			// properties.setValue("ShowMenubar", String.valueOf(set));

			propValue = this.properties.getValue("ShowButtons");
			set = propValue.length() > 0 && propValue.toLowerCase().equals("true");
			this.topButtons.setEnabled(set);
			this.topButtons.setVisible(set);
			// properties.setValue("ShowButtons", String.valueOf(set));

			propValue = this.properties.getValue("ShowDisplayoptions");
			set = propValue.length() > 0 && propValue.toLowerCase().equals("true");
			this.comboBoxBar.setEnabled(set);
			this.comboBoxBar.setVisible(set);
			// properties.setValue("ShowDisplayoptions", String.valueOf(set));

			propValue = this.properties.getValue("ShowNavigationbar");
			set = propValue.length() > 0 && propValue.toLowerCase().equals("true");
			this.navButtons.setEnabled(set);
			this.navButtons.setVisible(set);
			// properties.setValue("ShowNavigationbar", String.valueOf(set));

			if (this.displayPane != null) {
				propValue = this.properties.getValue("ShowSidetabbar");
				set = propValue.length() > 0 && propValue.toLowerCase().equals("true");
				if (!set) this.displayPane.setDividerSize(0);
				else this.displayPane.setDividerSize(5);
				this.displayPane.getLeftComponent().setEnabled(set);
				this.displayPane.getLeftComponent().setVisible(set);
				// properties.setValue("ShowSidetabbar", String.valueOf(set));
			}

			/**
			 * Items on nav pane
			 */
			propValue = this.properties.getValue("Firstbottom");
			set = propValue.length() > 0 && propValue.toLowerCase().equals("true");
			this.first.setEnabled(set);
			this.first.setVisible(set);

			propValue = this.properties.getValue("Back10bottom");
			set = propValue.length() > 0 && propValue.toLowerCase().equals("true");
			this.fback.setEnabled(set);
			this.fback.setVisible(set);

			propValue = this.properties.getValue("Backbottom");
			set = propValue.length() > 0 && propValue.toLowerCase().equals("true");
			this.back.setEnabled(set);
			this.back.setVisible(set);

			propValue = this.properties.getValue("Gotobottom");
			set = propValue.length() > 0 && propValue.toLowerCase().equals("true");
			this.pageCounter1.setEnabled(set);
			this.pageCounter1.setVisible(set);

			this.pageCounter2.setEnabled(set);
			this.pageCounter2.setVisible(set);

			this.pageCounter3.setEnabled(set);
			this.pageCounter3.setVisible(set);

			propValue = this.properties.getValue("Forwardbottom");
			set = propValue.length() > 0 && propValue.toLowerCase().equals("true");
			this.forward.setEnabled(set);
			this.forward.setVisible(set);

			propValue = this.properties.getValue("Forward10bottom");
			set = propValue.length() > 0 && propValue.toLowerCase().equals("true");
			this.fforward.setEnabled(set);
			this.fforward.setVisible(set);

			propValue = this.properties.getValue("Lastbottom");
			set = propValue.length() > 0 && propValue.toLowerCase().equals("true");
			this.end.setEnabled(set);
			this.end.setVisible(set);

			propValue = this.properties.getValue("Singlebottom");
			set = propValue.length() > 0 && propValue.toLowerCase().equals("true");
			// singleButton.setEnabled(set);
			this.singleButton.setVisible(set);

			propValue = this.properties.getValue("Continuousbottom");
			set = propValue.length() > 0 && propValue.toLowerCase().equals("true");
			// continuousButton.setEnabled(set);
			this.continuousButton.setVisible(set);

			propValue = this.properties.getValue("Continuousfacingbottom");
			set = propValue.length() > 0 && propValue.toLowerCase().equals("true");
			// continuousFacingButton.setEnabled(set);
			this.continuousFacingButton.setVisible(set);

			propValue = this.properties.getValue("Facingbottom");
			set = propValue.length() > 0 && propValue.toLowerCase().equals("true");
			// facingButton.setEnabled(set);
			this.facingButton.setVisible(set);

			propValue = this.properties.getValue("PageFlowbottom");
			set = propValue.length() > 0 && propValue.toLowerCase().equals("true");
			// pageFlowButton.setEnabled(set);
			this.pageFlowButton.setVisible(set);

			propValue = this.properties.getValue("Memorybottom");
			set = propValue.length() > 0 && propValue.toLowerCase().equals("true");
			this.memoryBar.setEnabled(set);
			this.memoryBar.setVisible(set);

			/**
			 * Items on option pane
			 */
			propValue = this.properties.getValue("Scalingdisplay");
			set = propValue.length() > 0 && propValue.toLowerCase().equals("true");
			this.scalingBox.setEnabled(set);
			this.scalingBox.setVisible(set);
			for (int i = 0; i != c.length; i++) {
				if (c[i] instanceof JLabel) {
					if (((JLabel) c[i]).getText().equals(Messages.getMessage("PdfViewerToolbarScaling.text"))) {
						c[i].setEnabled(set);
						c[i].setVisible(set);
						// properties.setValue("Scalingdisplay", String.valueOf(set));
					}
				}
			}

			propValue = this.properties.getValue("Rotationdisplay");
			set = propValue.length() > 0 && propValue.toLowerCase().equals("true");
			this.rotationBox.setEnabled(set);
			this.rotationBox.setVisible(set);
			for (int i = 0; i != c.length; i++) {
				if (c[i] instanceof JLabel) {
					if (((JLabel) c[i]).getText().equals(Messages.getMessage("PdfViewerToolbarRotation.text"))) {
						c[i].setEnabled(set);
						c[i].setVisible(set);
						// properties.setValue("Rotationdisplay", String.valueOf(set));
					}
				}
			}

			propValue = this.properties.getValue("Imageopdisplay");
			set = propValue.length() > 0 && propValue.toLowerCase().equals("true");

			if (this.qualityBox != null) {
				this.qualityBox.setVisible(set);
				this.qualityBox.setEnabled(set);
			}

			for (int i = 0; i != c.length; i++) {
				if (c[i] instanceof JLabel) {
					if (((JLabel) c[i]).getText().equals(Messages.getMessage("PdfViewerToolbarImageOp.text"))) {
						c[i].setVisible(set);
						c[i].setEnabled(set);
						// properties.setValue("Imageopdisplay", String.valueOf(set));
					}
				}
			}

			propValue = this.properties.getValue("Progressdisplay");
			set = propValue.length() > 0 && propValue.toLowerCase().equals("true");
			this.statusBar.setEnabled(set);
			// <start-wrap>
			this.statusBar.setVisible(set);
			// <end-wrap>

			propValue = this.properties.getValue("Downloadprogressdisplay");
			set = propValue.length() > 0 && propValue.toLowerCase().equals("true");
			this.downloadBar.setEnabled(set);
			// <start-wrap>
			this.downloadBar.setVisible(set);
			// <end-wrap>

			/**
			 * Items on button bar
			 */
			propValue = this.properties.getValue("Openfilebutton");
			set = propValue.length() > 0 && propValue.toLowerCase().equals("true");
			this.openButton.setEnabled(set);
			this.openButton.setVisible(set);

			propValue = this.properties.getValue("Printbutton");
			set = propValue.length() > 0 && propValue.toLowerCase().equals("true");
			this.printButton.setEnabled(set);
			this.printButton.setVisible(set);

			propValue = this.properties.getValue("Searchbutton");
			set = propValue.length() > 0 && propValue.toLowerCase().equals("true");
			this.searchButton.setEnabled(set);
			this.searchButton.setVisible(set);

			propValue = this.properties.getValue("Propertiesbutton");
			set = propValue.length() > 0 && propValue.toLowerCase().equals("true");
			this.docPropButton.setEnabled(set);
			this.docPropButton.setVisible(set);

			propValue = this.properties.getValue("Aboutbutton");
			set = propValue.length() > 0 && propValue.toLowerCase().equals("true");
			this.infoButton.setEnabled(set);
			this.infoButton.setVisible(set);

			propValue = this.properties.getValue("Snapshotbutton");
			set = propValue.length() > 0 && propValue.toLowerCase().equals("true");
			this.snapshotButton.setEnabled(set);
			this.snapshotButton.setVisible(set);

			propValue = this.properties.getValue("CursorButton");
			set = propValue.length() > 0 && propValue.toLowerCase().equals("true");
			this.cursor.setEnabled(set);
			this.cursor.setVisible(set);

			propValue = this.properties.getValue("MouseModeButton");
			set = propValue.length() > 0 && propValue.toLowerCase().equals("true");
			this.mouseMode.setEnabled(set);
			this.mouseMode.setVisible(set);

			/**
			 * Items on signature tab
			 */
			if (DecoderOptions.isRunningOnMac) {
				propValue = this.properties.getValue("Pagetab");
				set = (this.properties.getValue("Pagetab").toLowerCase().equals("true") && this.navOptionsPanel.getTabCount() != 0);
				for (int i = 0; i < this.navOptionsPanel.getTabCount(); i++) {

					if (this.navOptionsPanel.getTitleAt(i).equals(this.pageTitle) && !set) {
						this.navOptionsPanel.remove(i);
					}
				}

				propValue = this.properties.getValue("Bookmarkstab");
				set = (this.properties.getValue("Bookmarkstab").toLowerCase().equals("true") && this.navOptionsPanel.getTabCount() != 0);
				for (int i = 0; i < this.navOptionsPanel.getTabCount(); i++) {

					if (this.navOptionsPanel.getTitleAt(i).equals(this.bookmarksTitle) && !set) {
						this.navOptionsPanel.remove(i);
					}
				}

				propValue = this.properties.getValue("Layerstab");
				set = (this.properties.getValue("Layerstab").toLowerCase().equals("true") && this.navOptionsPanel.getTabCount() != 0);
				for (int i = 0; i < this.navOptionsPanel.getTabCount(); i++) {

					if (this.navOptionsPanel.getTitleAt(i).equals(this.layersTitle) && !set) {
						this.navOptionsPanel.remove(i);
					}
				}

				propValue = this.properties.getValue("Signaturestab");
				set = (this.properties.getValue("Signaturestab").toLowerCase().equals("true") && this.navOptionsPanel.getTabCount() != 0);
				for (int i = 0; i < this.navOptionsPanel.getTabCount(); i++) {

					if (this.navOptionsPanel.getTitleAt(i).equals(this.signaturesTitle) && !set) {
						this.navOptionsPanel.remove(i);
					}
				}
			}
			else {
				propValue = this.properties.getValue("Pagetab");
				set = (this.properties.getValue("Pagetab").toLowerCase().equals("true") && this.navOptionsPanel.getTabCount() != 0);
				for (int i = 0; i < this.navOptionsPanel.getTabCount(); i++) {

					if (this.navOptionsPanel.getIconAt(i).toString().equals(this.pageTitle) && !set) {
						this.navOptionsPanel.remove(i);
					}
				}

				propValue = this.properties.getValue("Bookmarkstab");
				set = (this.properties.getValue("Bookmarkstab").toLowerCase().equals("true") && this.navOptionsPanel.getTabCount() != 0);
				for (int i = 0; i < this.navOptionsPanel.getTabCount(); i++) {

					if (this.navOptionsPanel.getIconAt(i).toString().equals(this.bookmarksTitle) && !set) {
						this.navOptionsPanel.remove(i);
					}
				}

				propValue = this.properties.getValue("Layerstab");
				set = (this.properties.getValue("Layerstab").toLowerCase().equals("true") && this.navOptionsPanel.getTabCount() != 0);
				for (int i = 0; i < this.navOptionsPanel.getTabCount(); i++) {

					if (this.navOptionsPanel.getIconAt(i).toString().equals(this.layersTitle) && !set) {
						this.navOptionsPanel.remove(i);
					}
				}

				propValue = this.properties.getValue("Signaturestab");
				set = (this.properties.getValue("Signaturestab").toLowerCase().equals("true") && this.navOptionsPanel.getTabCount() != 0);
				for (int i = 0; i < this.navOptionsPanel.getTabCount(); i++) {

					if (this.navOptionsPanel.getIconAt(i).toString().equals(this.signaturesTitle) && !set) {
						this.navOptionsPanel.remove(i);
					}
				}
			}
			/**
			 * Items from the menu item
			 */
			if (this.fileMenu != null) { // all of these will be null in 'Wrapper' mode so ignore

				propValue = this.properties.getValue("FileMenu");
				set = propValue.length() > 0 && propValue.toLowerCase().equals("true");
				this.fileMenu.setEnabled(set);
				this.fileMenu.setVisible(set);

				propValue = this.properties.getValue("OpenMenu");
				set = propValue.length() > 0 && propValue.toLowerCase().equals("true");
				this.openMenu.setEnabled(set);
				this.openMenu.setVisible(set);

				propValue = this.properties.getValue("Open");
				set = propValue.length() > 0 && propValue.toLowerCase().equals("true");
				this.open.setEnabled(set);
				this.open.setVisible(set);

				propValue = this.properties.getValue("Openurl");
				set = propValue.length() > 0 && propValue.toLowerCase().equals("true");
				this.openUrl.setEnabled(set);
				this.openUrl.setVisible(set);

				propValue = this.properties.getValue("Save");
				set = propValue.length() > 0 && propValue.toLowerCase().equals("true");
				this.save.setEnabled(set);
				this.save.setVisible(set);

				propValue = this.properties.getValue("Find");
				set = propValue.length() > 0 && propValue.toLowerCase().equals("true");
				this.find.setEnabled(set);
				this.find.setVisible(set);

				propValue = this.properties.getValue("Documentproperties");
				set = propValue.length() > 0 && propValue.toLowerCase().equals("true");
				this.documentProperties.setEnabled(set);
				this.documentProperties.setVisible(set);

				// @SIGNING
				if (enableShowSign && this.signPDF != null) {
					propValue = this.properties.getValue("Signpdf");
					set = propValue.length() > 0 && propValue.toLowerCase().equals("true");
					this.signPDF.setEnabled(this.commonValues.isItextOnClasspath() && this.commonValues.isEncrypOnClasspath());
					this.signPDF.setVisible(set);
				}

				propValue = this.properties.getValue("Print");
				set = propValue.length() > 0 && propValue.toLowerCase().equals("true");
				this.print.setEnabled(set);
				this.print.setVisible(set);

				propValue = this.properties.getValue("Recentdocuments");
				set = propValue.length() > 0 && propValue.toLowerCase().equals("true");
				this.currentCommands.enableRecentDocuments(set);

				propValue = this.properties.getValue("Exit");
				set = propValue.length() > 0 && propValue.toLowerCase().equals("true");
				this.exit.setEnabled(set);
				this.exit.setVisible(set);

				propValue = this.properties.getValue("EditMenu");
				set = propValue.length() > 0 && propValue.toLowerCase().equals("true");
				this.editMenu.setEnabled(set);
				this.editMenu.setVisible(set);

				propValue = this.properties.getValue("Copy");
				set = propValue.length() > 0 && propValue.toLowerCase().equals("true");
				this.copy.setEnabled(set);
				this.copy.setVisible(set);

				propValue = this.properties.getValue("Selectall");
				set = propValue.length() > 0 && propValue.toLowerCase().equals("true");
				this.selectAll.setEnabled(set);
				this.selectAll.setVisible(set);

				propValue = this.properties.getValue("Deselectall");
				set = propValue.length() > 0 && propValue.toLowerCase().equals("true");
				this.deselectAll.setEnabled(set);
				this.deselectAll.setVisible(set);

				propValue = this.properties.getValue("Preferences");
				set = (propValue.length() > 0 && propValue.toLowerCase().equals("true"))
						&& (!this.properties.getValue("readOnly").toLowerCase().equals("true"));
				this.preferences.setEnabled(set);
				this.preferences.setVisible(set);

				propValue = this.properties.getValue("ViewMenu");
				set = propValue.length() > 0 && propValue.toLowerCase().equals("true");
				this.viewMenu.setEnabled(set);
				this.viewMenu.setVisible(set);

				propValue = this.properties.getValue("GotoMenu");
				set = propValue.length() > 0 && propValue.toLowerCase().equals("true");
				this.goToMenu.setEnabled(set);
				this.goToMenu.setVisible(set);

				propValue = this.properties.getValue("Firstpage");
				set = propValue.length() > 0 && propValue.toLowerCase().equals("true");
				this.firstPage.setEnabled(set);
				this.firstPage.setVisible(set);

				propValue = this.properties.getValue("Backpage");
				set = propValue.length() > 0 && propValue.toLowerCase().equals("true");
				this.backPage.setEnabled(set);
				this.backPage.setVisible(set);

				propValue = this.properties.getValue("Forwardpage");
				set = propValue.length() > 0 && propValue.toLowerCase().equals("true");
				this.forwardPage.setEnabled(set);
				this.forwardPage.setVisible(set);

				propValue = this.properties.getValue("Lastpage");
				set = propValue.length() > 0 && propValue.toLowerCase().equals("true");
				this.lastPage.setEnabled(set);
				this.lastPage.setVisible(set);

				propValue = this.properties.getValue("Goto");
				set = propValue.length() > 0 && propValue.toLowerCase().equals("true");
				this.goTo.setEnabled(set);
				this.goTo.setVisible(set);

				propValue = this.properties.getValue("Previousdocument");
				set = propValue.length() > 0 && propValue.toLowerCase().equals("true");
				this.previousDocument.setEnabled(set);
				this.previousDocument.setVisible(set);

				propValue = this.properties.getValue("Nextdocument");
				set = propValue.length() > 0 && propValue.toLowerCase().equals("true");
				this.nextDocument.setEnabled(set);
				this.nextDocument.setVisible(set);

				if (this.pageLayoutMenu != null) {
					propValue = this.properties.getValue("PagelayoutMenu");
					set = propValue.length() > 0 && propValue.toLowerCase().equals("true");
					this.pageLayoutMenu.setEnabled(set);
					this.pageLayoutMenu.setVisible(set);
				}

				if (this.single != null) {
					propValue = this.properties.getValue("Single");
					set = propValue.length() > 0 && propValue.toLowerCase().equals("true");
					this.single.setEnabled(set);
					this.single.setVisible(set);
				}

				if (this.continuous != null) {
					propValue = this.properties.getValue("Continuous");
					set = propValue.length() > 0 && propValue.toLowerCase().equals("true");
					this.continuous.setEnabled(set);
					this.continuous.setVisible(set);
				}

				if (this.facing != null) {
					propValue = this.properties.getValue("Facing");
					set = propValue.length() > 0 && propValue.toLowerCase().equals("true");
					this.facing.setEnabled(set);
					this.facing.setVisible(set);
				}

				if (this.continuousFacing != null) {
					propValue = this.properties.getValue("Continuousfacing");
					set = propValue.length() > 0 && propValue.toLowerCase().equals("true");
					this.continuousFacing.setEnabled(set);
					this.continuousFacing.setVisible(set);
				}

				if (this.pageFlow != null) {

					propValue = this.properties.getValue("PageFlow");
					set = propValue.length() > 0 && propValue.toLowerCase().equals("true");
					this.pageFlow.setEnabled(set);
					this.pageFlow.setVisible(set);
				}

				if (this.textSelect != null) {

					propValue = this.properties.getValue("textSelect");
					set = propValue.length() > 0 && propValue.toLowerCase().equals("true");
					this.textSelect.setEnabled(set);
					this.textSelect.setVisible(set);
				}

				if (this.separateCover != null) {
					propValue = this.properties.getValue("separateCover");
					set = propValue.length() > 0 && propValue.toLowerCase().equals("true");
					this.separateCover.setEnabled(set);
					this.separateCover.setVisible(set);
				}

				if (this.panMode != null) {

					propValue = this.properties.getValue("panMode");
					set = propValue.length() > 0 && propValue.toLowerCase().equals("true");
					this.panMode.setEnabled(set);
					this.panMode.setVisible(set);
				}

				if (this.fullscreen != null) {

					propValue = this.properties.getValue("Fullscreen");
					set = propValue.length() > 0 && propValue.toLowerCase().equals("true");
					this.fullscreen.setEnabled(set);
					this.fullscreen.setVisible(set);
				}

				if (this.windowMenu != null) {

					propValue = this.properties.getValue("WindowMenu");
					set = propValue.length() > 0 && propValue.toLowerCase().equals("true");
					this.windowMenu.setEnabled(set);
					this.windowMenu.setVisible(set);

					propValue = this.properties.getValue("Cascade");
					set = propValue.length() > 0 && propValue.toLowerCase().equals("true");
					this.cascade.setEnabled(set);
					this.cascade.setVisible(set);

					propValue = this.properties.getValue("Tile");
					set = propValue.length() > 0 && propValue.toLowerCase().equals("true");
					this.tile.setEnabled(set);
					this.tile.setVisible(set);
				}

				if (this.commonValues.isItextOnClasspath()) {}

				propValue = this.properties.getValue("HelpMenu");
				set = propValue.length() > 0 && propValue.toLowerCase().equals("true");
				this.helpMenu.setEnabled(set);
				this.helpMenu.setVisible(set);

				propValue = this.properties.getValue("Visitwebsite");
				set = propValue.length() > 0 && propValue.toLowerCase().equals("true");
				this.visitWebsite.setEnabled(set);
				this.visitWebsite.setVisible(set);

				propValue = this.properties.getValue("Tipoftheday");
				set = propValue.length() > 0 && propValue.toLowerCase().equals("true");
				this.tipOfTheDay.setEnabled(set);
				this.tipOfTheDay.setVisible(set);

				propValue = this.properties.getValue("Checkupdates");
				set = propValue.length() > 0 && propValue.toLowerCase().equals("true");
				this.checkUpdates.setEnabled(set);
				this.checkUpdates.setVisible(set);

				propValue = this.properties.getValue("About");
				set = propValue.length() > 0 && propValue.toLowerCase().equals("true");
				this.about.setEnabled(set);
				this.about.setVisible(set);

				/*
				 * Ensure none of the menus start with a separator
				 */
				for (int k = 0; k != this.fileMenu.getMenuComponentCount(); k++) {
					if (this.fileMenu.getMenuComponent(k).isVisible()) {
						if (this.fileMenu.getMenuComponent(k) instanceof JSeparator) {
							this.fileMenu.remove(this.fileMenu.getMenuComponent(k));
						}
						break;
					}
				}

				for (int k = 0; k != this.editMenu.getMenuComponentCount(); k++) {
					if (this.editMenu.getMenuComponent(k).isVisible()) {
						if (this.editMenu.getMenuComponent(k) instanceof JSeparator) {
							this.editMenu.remove(this.editMenu.getMenuComponent(k));
						}
						break;
					}
				}

				for (int k = 0; k != this.viewMenu.getMenuComponentCount(); k++) {
					if (this.viewMenu.getMenuComponent(k).isVisible()) {
						if (this.viewMenu.getMenuComponent(k) instanceof JSeparator) {
							this.viewMenu.remove(this.viewMenu.getMenuComponent(k));
						}
						break;
					}
				}

				for (int k = 0; k != this.goToMenu.getMenuComponentCount(); k++) {
					if (this.goToMenu.getMenuComponent(k).isVisible()) {
						if (this.goToMenu.getMenuComponent(k) instanceof JSeparator) {
							this.goToMenu.remove(this.goToMenu.getMenuComponent(k));
						}
						break;
					}
				}

			}

			checkButtonSeparators();

		}
		catch (Exception ee) {
			ee.printStackTrace();
		}

		// <start-demo>
		/**
		 * //<end-demo>
		 * 
		 * /
		 **/
	}

	public void alterProperty(String value, boolean set) {
		Component[] c = this.comboBoxBar.getComponents();

		// Disable entire section
		if (value.equals("ShowMenubar")) {
			this.currentMenu.setEnabled(set);
			this.currentMenu.setVisible(set);
			this.properties.setValue("ShowMenubar", String.valueOf(set));
		}
		if (value.equals("ShowButtons")) {
			this.topButtons.setEnabled(set);
			this.topButtons.setVisible(set);
			this.properties.setValue("ShowButtons", String.valueOf(set));
		}
		if (value.equals("ShowDisplayoptions")) {
			this.comboBoxBar.setEnabled(set);
			this.comboBoxBar.setVisible(set);
			this.properties.setValue("ShowDisplayoptions", String.valueOf(set));
		}
		if (value.equals("ShowNavigationbar")) {
			this.navButtons.setEnabled(set);
			this.navButtons.setVisible(set);
			this.properties.setValue("ShowNavigationbar", String.valueOf(set));
		}
		if (value.equals("ShowSidetabbar")) {
			if (!set) this.displayPane.setDividerSize(0);
			else this.displayPane.setDividerSize(5);
			this.displayPane.getLeftComponent().setEnabled(set);
			this.displayPane.getLeftComponent().setVisible(set);
			this.properties.setValue("ShowSidetabbar", String.valueOf(set));
		}

		/**
		 * Items on nav pane
		 */
		if (value.equals("Firstbottom")) {
			this.first.setEnabled(set);
			this.first.setVisible(set);
		}
		if (value.equals("Back10bottom")) {
			this.fback.setEnabled(set);
			this.fback.setVisible(set);
		}
		if (value.equals("Backbottom")) {
			this.back.setEnabled(set);
			this.back.setVisible(set);
		}
		if (value.equals("Gotobottom")) {
			this.pageCounter1.setEnabled(set);
			this.pageCounter1.setVisible(set);

			this.pageCounter2.setEnabled(set);
			this.pageCounter2.setVisible(set);

			this.pageCounter3.setEnabled(set);
			this.pageCounter3.setVisible(set);
		}
		if (value.equals("Forwardbottom")) {
			this.forward.setEnabled(set);
			this.forward.setVisible(set);
		}
		if (value.equals("Forward10bottom")) {
			this.fforward.setEnabled(set);
			this.fforward.setVisible(set);
		}
		if (value.equals("Lastbottom")) {
			this.end.setEnabled(set);
			this.end.setVisible(set);
		}
		if (value.equals("Singlebottom")) {
			// singleButton.setEnabled(set);
			this.singleButton.setVisible(set);
		}
		if (value.equals("Continuousbottom")) {
			// continuousButton.setEnabled(set);
			this.continuousButton.setVisible(set);
		}
		if (value.equals("Continuousfacingbottom")) {
			// continuousFacingButton.setEnabled(set);
			this.continuousFacingButton.setVisible(set);
		}
		if (value.equals("Facingbottom")) {
			// facingButton.setEnabled(set);
			this.facingButton.setVisible(set);
		}
		if (value.equals("PageFlowbottom")) {
			// pageFlowButton.setEnabled(set);
			this.pageFlowButton.setVisible(set);
		}
		if (value.equals("Memorybottom")) {
			this.memoryBar.setEnabled(set);
			this.memoryBar.setVisible(set);
		}

		/**
		 * Items on option pane
		 */
		if (value.equals("Scalingdisplay")) {
			this.scalingBox.setEnabled(set);
			this.scalingBox.setVisible(set);
			for (int i = 0; i != c.length; i++) {
				if (c[i] instanceof JLabel) {
					if (((JLabel) c[i]).getText().equals(Messages.getMessage("PdfViewerToolbarScaling.text"))) {
						c[i].setEnabled(set);
						c[i].setVisible(set);
						this.properties.setValue("Scalingdisplay", String.valueOf(set));
					}
				}
			}
		}
		if (value.equals("Rotationdisplay")) {
			this.rotationBox.setEnabled(set);
			this.rotationBox.setVisible(set);
			for (int i = 0; i != c.length; i++) {
				if (c[i] instanceof JLabel) {
					if (((JLabel) c[i]).getText().equals(Messages.getMessage("PdfViewerToolbarRotation.text"))) {
						c[i].setEnabled(set);
						c[i].setVisible(set);
						this.properties.setValue("Rotationdisplay", String.valueOf(set));
					}
				}
			}
		}
		if (value.equals("Imageopdisplay")) {
			this.qualityBox.setVisible(set);
			this.qualityBox.setEnabled(set);
			for (int i = 0; i != c.length; i++) {
				if (c[i] instanceof JLabel) {
					if (((JLabel) c[i]).getText().equals(Messages.getMessage("PdfViewerToolbarImageOp.text"))) {
						c[i].setVisible(set);
						c[i].setEnabled(set);
						this.properties.setValue("Imageopdisplay", String.valueOf(set));
					}
				}
			}
		}
		if (value.equals("Progressdisplay")) {
			this.statusBar.setEnabled(set);
			// <start-wrap>
			this.statusBar.setVisible(set);
			// <end-wrap>
			this.properties.setValue("Progressdisplay", String.valueOf(set));
		}
		if (value.equals("Downloadprogressdisplay")) {
			this.downloadBar.setEnabled(set);
			// <start-wrap>
			this.downloadBar.setVisible(set);
			// <end-wrap>
			this.properties.setValue("Downloadprogressdisplay", String.valueOf(set));
		}

		/**
		 * Items on button bar
		 */
		if (value.equals("Openfilebutton")) {
			this.openButton.setEnabled(set);
			this.openButton.setVisible(set);
		}
		if (value.equals("Printbutton")) {
			this.printButton.setEnabled(set);
			this.printButton.setVisible(set);
		}
		if (value.equals("Searchbutton")) {
			this.searchButton.setEnabled(set);
			this.searchButton.setVisible(set);
		}
		if (value.equals("Propertiesbutton")) {
			this.docPropButton.setEnabled(set);
			this.docPropButton.setVisible(set);
		}
		if (value.equals("Aboutbutton")) {
			this.infoButton.setEnabled(set);
			this.infoButton.setVisible(set);
		}
		if (value.equals("Snapshotbutton")) {
			this.snapshotButton.setEnabled(set);
			this.snapshotButton.setVisible(set);
		}

		if (value.equals("CursorButton")) {
			this.cursor.setEnabled(set);
			this.cursor.setVisible(set);
		}

		if (value.equals("MouseModeButton")) {
			this.mouseMode.setEnabled(set);
			this.mouseMode.setVisible(set);
		}

		/**
		 * Items on signature tab
		 */
		if (DecoderOptions.isRunningOnMac) {
			if (value.equals("Pagetab") && this.navOptionsPanel.getTabCount() != 0) {
				for (int i = 0; i < this.navOptionsPanel.getTabCount(); i++) {

					if (this.navOptionsPanel.getTitleAt(i).equals(this.pageTitle) && !set) {
						this.navOptionsPanel.remove(i);
					}
				}
			}
			if (value.equals("Bookmarkstab") && this.navOptionsPanel.getTabCount() != 0) {
				for (int i = 0; i < this.navOptionsPanel.getTabCount(); i++) {

					if (this.navOptionsPanel.getTitleAt(i).equals(this.bookmarksTitle) && !set) {
						this.navOptionsPanel.remove(i);
					}
				}
			}
			if (value.equals("Layerstab") && this.navOptionsPanel.getTabCount() != 0) {
				for (int i = 0; i < this.navOptionsPanel.getTabCount(); i++) {

					if (this.navOptionsPanel.getTitleAt(i).equals(this.layersTitle) && !set) {
						this.navOptionsPanel.remove(i);
					}
				}
			}
			if (value.equals("Signaturestab") && this.navOptionsPanel.getTabCount() != 0) {
				for (int i = 0; i < this.navOptionsPanel.getTabCount(); i++) {

					if (this.navOptionsPanel.getTitleAt(i).equals(this.signaturesTitle) && !set) {
						this.navOptionsPanel.remove(i);
					}
				}
			}
		}
		else {
			if (value.equals("Pagetab") && this.navOptionsPanel.getTabCount() != 0) {
				for (int i = 0; i < this.navOptionsPanel.getTabCount(); i++) {

					if (this.navOptionsPanel.getIconAt(i).toString().equals(this.pageTitle) && !set) {
						this.navOptionsPanel.remove(i);
					}
				}
			}
			if (value.equals("Bookmarkstab") && this.navOptionsPanel.getTabCount() != 0) {
				for (int i = 0; i < this.navOptionsPanel.getTabCount(); i++) {

					if (this.navOptionsPanel.getIconAt(i).toString().equals(this.bookmarksTitle) && !set) {
						this.navOptionsPanel.remove(i);
					}
				}
			}
			if (value.equals("Layerstab") && this.navOptionsPanel.getTabCount() != 0) {
				for (int i = 0; i < this.navOptionsPanel.getTabCount(); i++) {

					if (this.navOptionsPanel.getIconAt(i).toString().equals(this.layersTitle) && !set) {
						this.navOptionsPanel.remove(i);
					}
				}
			}
			if (value.equals("Signaturestab") && this.navOptionsPanel.getTabCount() != 0) {
				for (int i = 0; i < this.navOptionsPanel.getTabCount(); i++) {

					if (this.navOptionsPanel.getIconAt(i).toString().equals(this.signaturesTitle) && !set) {
						this.navOptionsPanel.remove(i);
					}
				}
			}
		}
		/**
		 * Items from the menu item
		 */
		if (value.equals("FileMenu")) {
			this.fileMenu.setEnabled(set);
			this.fileMenu.setVisible(set);
		}
		if (value.equals("OpenMenu")) {
			this.openMenu.setEnabled(set);
			this.openMenu.setVisible(set);
		}
		if (value.equals("Open")) {
			this.open.setEnabled(set);
			this.open.setVisible(set);
		}
		if (value.equals("Openurl")) {
			this.openUrl.setEnabled(set);
			this.openUrl.setVisible(set);
		}

		if (value.equals("Save")) {
			this.save.setEnabled(set);
			this.save.setVisible(set);
		}

		// added check to code (as it may not have been initialised)
		if (value.equals("Resaveasforms") && this.reSaveAsForms != null) { // will not be initialised if Itext not on path
			this.reSaveAsForms.setEnabled(set);
			this.reSaveAsForms.setVisible(set);
		}

		if (value.equals("Find")) {
			this.find.setEnabled(set);
			this.find.setVisible(set);
		}
		if (value.equals("Documentproperties")) {
			this.documentProperties.setEnabled(set);
			this.documentProperties.setVisible(set);
		}
		// @SIGNING
		if (enableShowSign && value.equals("Signpdf")) {
			this.signPDF.setEnabled(set);
			this.signPDF.setVisible(set);
		}

		if (value.equals("Print")) {
			this.print.setEnabled(set);
			this.print.setVisible(set);
		}
		if (value.equals("Recentdocuments")) {
			this.currentCommands.enableRecentDocuments(set);
		}
		if (value.equals("Exit")) {
			this.exit.setEnabled(set);
			this.exit.setVisible(set);
		}

		if (value.equals("EditMenu")) {
			this.editMenu.setEnabled(set);
			this.editMenu.setVisible(set);
		}
		if (value.equals("Copy")) {
			this.copy.setEnabled(set);
			this.copy.setVisible(set);
		}
		if (value.equals("Selectall")) {
			this.selectAll.setEnabled(set);
			this.selectAll.setVisible(set);
		}
		if (value.equals("Deselectall")) {
			this.deselectAll.setEnabled(set);
			this.deselectAll.setVisible(set);
		}
		if (value.equals("Preferences")) {
			this.preferences.setEnabled(set);
			this.preferences.setVisible(set);
		}

		if (value.equals("ViewMenu")) {
			this.viewMenu.setEnabled(set);
			this.viewMenu.setVisible(set);
		}
		if (value.equals("GotoMenu")) {
			this.goToMenu.setEnabled(set);
			this.goToMenu.setVisible(set);
		}
		if (value.equals("Firstpage")) {
			this.firstPage.setEnabled(set);
			this.firstPage.setVisible(set);
		}
		if (value.equals("Backpage")) {
			this.backPage.setEnabled(set);
			this.backPage.setVisible(set);
		}
		if (value.equals("Forwardpage")) {
			this.forwardPage.setEnabled(set);
			this.forwardPage.setVisible(set);
		}
		if (value.equals("Lastpage")) {
			this.lastPage.setEnabled(set);
			this.lastPage.setVisible(set);
		}
		if (value.equals("Goto")) {
			this.goTo.setEnabled(set);
			this.goTo.setVisible(set);
		}
		if (value.equals("Previousdocument")) {
			this.previousDocument.setEnabled(set);
			this.previousDocument.setVisible(set);
		}
		if (value.equals("Nextdocument")) {
			this.nextDocument.setEnabled(set);
			this.nextDocument.setVisible(set);
		}

		if (value.equals("PagelayoutMenu")) {
			this.pageLayoutMenu.setEnabled(set);
			this.pageLayoutMenu.setVisible(set);
		}
		if (value.equals("Single")) {
			this.single.setEnabled(set);
			this.single.setVisible(set);
		}
		if (value.equals("Continuous")) {
			this.continuous.setEnabled(set);
			this.continuous.setVisible(set);
		}
		if (value.equals("Facing")) {
			this.facing.setEnabled(set);
			this.facing.setVisible(set);
		}
		if (value.equals("Continuousfacing")) {
			this.continuousFacing.setEnabled(set);
			this.continuousFacing.setVisible(set);
		}

		// @kieran Remove when ready
		if (this.pageFlow != null) if (value.equals("PageFlow")) {
			this.pageFlow.setEnabled(set);
			this.pageFlow.setVisible(set);
		}
		if (this.panMode != null) if (value.equals("panMode")) {
			this.panMode.setEnabled(set);
			this.panMode.setVisible(set);
		}
		if (this.textSelect != null) if (value.equals("textSelect")) {
			this.textSelect.setEnabled(set);
			this.textSelect.setVisible(set);
		}
		if (value.equals("Fullscreen")) {
			this.fullscreen.setEnabled(set);
			this.fullscreen.setVisible(set);
		}
		if (this.separateCover != null) if (value.equals("separateCover")) {
			this.separateCover.setEnabled(set);
			this.separateCover.setVisible(set);
		}

		if (this.windowMenu != null) {
			if (value.equals("WindowMenu")) {
				this.windowMenu.setEnabled(set);
				this.windowMenu.setVisible(set);
			}
			if (value.equals("Cascade")) {
				this.cascade.setEnabled(set);
				this.cascade.setVisible(set);
			}
			if (value.equals("Tile")) {
				this.tile.setEnabled(set);
				this.tile.setVisible(set);
			}
		}
		if (this.commonValues.isItextOnClasspath()) {
			if (value.equals("ExportMenu")) {
				this.exportMenu.setEnabled(set);
				this.exportMenu.setVisible(set);
			}
			if (value.equals("PdfMenu")) {
				this.pdfMenu.setEnabled(set);
				this.pdfMenu.setVisible(set);
			}
			if (value.equals("Oneperpage")) {
				this.onePerPage.setEnabled(set);
				this.onePerPage.setVisible(set);
			}
			if (value.equals("Nup")) {
				this.nup.setEnabled(set);
				this.nup.setVisible(set);
			}
			if (value.equals("Handouts")) {
				this.handouts.setEnabled(set);
				this.handouts.setVisible(set);
			}

			if (value.equals("ContentMenu")) {
				this.contentMenu.setEnabled(set);
				this.contentMenu.setVisible(set);
			}
			if (value.equals("Images")) {
				this.images.setEnabled(set);
				this.images.setVisible(set);
			}
			if (value.equals("Text")) {
				this.text.setEnabled(set);
				this.text.setVisible(set);
			}

			if (value.equals("Bitmap")) {
				this.bitmap.setEnabled(set);
				this.bitmap.setVisible(set);
			}

			if (value.equals("PagetoolsMenu")) {
				this.pageToolsMenu.setEnabled(set);
				this.pageToolsMenu.setVisible(set);
			}
			if (value.equals("Rotatepages")) {
				this.rotatePages.setEnabled(set);
				this.rotatePages.setVisible(set);
			}
			if (value.equals("Deletepages")) {
				this.deletePages.setEnabled(set);
				this.deletePages.setVisible(set);
			}
			if (value.equals("Addpage")) {
				this.addPage.setEnabled(set);
				this.addPage.setVisible(set);
			}
			if (value.equals("Addheaderfooter")) {
				this.addHeaderFooter.setEnabled(set);
				this.addHeaderFooter.setVisible(set);
			}
			if (value.equals("Stamptext")) {
				this.stampText.setEnabled(set);
				this.stampText.setVisible(set);
			}
			if (value.equals("Stampimage")) {
				this.stampImage.setEnabled(set);
				this.stampImage.setVisible(set);
			}
			if (value.equals("Crop")) {
				this.crop.setEnabled(set);
				this.crop.setVisible(set);
			}
		}
		if (value.equals("HelpMenu")) {
			this.helpMenu.setEnabled(set);
			this.helpMenu.setVisible(set);
		}
		if (value.equals("Visitwebsite")) {
			this.visitWebsite.setEnabled(set);
			this.visitWebsite.setVisible(set);
		}
		if (value.equals("Tipoftheday")) {
			this.tipOfTheDay.setEnabled(set);
			this.tipOfTheDay.setVisible(set);
		}
		if (value.equals("Checkupdates")) {
			this.checkUpdates.setEnabled(set);
			this.checkUpdates.setVisible(set);
		}
		if (value.equals("About")) {
			this.about.setEnabled(set);
			this.about.setVisible(set);
		}

		checkButtonSeparators();
	}

	private void checkButtonSeparators() {
		/**
		 * Ensure the buttonBar doesn't start or end with a separator
		 */
		boolean before = false, after = false;
		JSeparator currentSep = null;
		for (int k = 0; k != this.topButtons.getComponentCount(); k++) {
			if (this.topButtons.getComponent(k) instanceof JSeparator) {
				if (currentSep == null) currentSep = (JSeparator) this.topButtons.getComponent(k);
				else {
					if (!before || !after) currentSep.setVisible(false);
					else currentSep.setVisible(true);
					before = before || after;
					after = false;
					currentSep = (JSeparator) this.topButtons.getComponent(k);
				}
			}
			else {
				if (this.topButtons.getComponent(k).isVisible()) {
					if (currentSep == null) before = true;
					else after = true;
				}
			}
		}
		if (currentSep != null) {
			if (!before || !after) currentSep.setVisible(false);
			else currentSep.setVisible(true);
		}
	}

	public void getHelpBox() {
		final JPanel panel = new JPanel();

		JPanel top = new JPanel();
		top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

		JPanel labelPanel = new JPanel();
		labelPanel.setLayout(new BoxLayout(labelPanel, BoxLayout.X_AXIS));
		labelPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		JLabel label = new JLabel("<html><p>Please click on this link for lots of tutorials and documentation</p>");
		label.setAlignmentX(Component.LEFT_ALIGNMENT);

		labelPanel.add(label);
		labelPanel.add(Box.createHorizontalGlue());

		top.add(labelPanel);

		JPanel linkPanel = new JPanel();
		linkPanel.setLayout(new BoxLayout(linkPanel, BoxLayout.X_AXIS));
		linkPanel.add(Box.createHorizontalGlue());
		linkPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		final JLabel url = new JLabel("<html><center>http://www.idrsolutions.com/java-pdf-library-support/");
		url.setAlignmentX(Component.LEFT_ALIGNMENT);

		url.setForeground(Color.blue);
		url.setHorizontalAlignment(SwingConstants.CENTER);

		// @kieran - cursor
		url.addMouseListener(new MouseListener() {
			@Override
			public void mouseEntered(MouseEvent e) {
				if (SingleDisplay.allowChangeCursor) panel.getTopLevelAncestor().setCursor(new Cursor(Cursor.HAND_CURSOR));
				url.setText("<html><center><a>http://www.jpedal.org/support.php</a></center>");
			}

			@Override
			public void mouseExited(MouseEvent e) {
				if (SingleDisplay.allowChangeCursor) panel.getTopLevelAncestor().setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
				url.setText("<html><center>http://www.jpedal.org/support.php");
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				try {
					BrowserLauncher.openURL("http://www.idrsolutions.com/java-pdf-library-support/");
				}
				catch (IOException e1) {

					JPanel errorPanel = new JPanel();
					errorPanel.setLayout(new BoxLayout(errorPanel, BoxLayout.Y_AXIS));

					JLabel errorMessage = new JLabel("Your web browser could not be successfully loaded.  "
							+ "Please copy and paste the URL below, manually into your web browser.");
					errorMessage.setAlignmentX(Component.LEFT_ALIGNMENT);
					errorMessage.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

					JTextArea textArea = new JTextArea("http://www.idrsolutions.com/java-pdf-library-support/");
					textArea.setEditable(false);
					textArea.setRows(5);
					textArea.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
					textArea.setAlignmentX(Component.LEFT_ALIGNMENT);

					errorPanel.add(errorMessage);
					errorPanel.add(textArea);

					showMessageDialog(errorPanel, "Error loading web browser", JOptionPane.PLAIN_MESSAGE);

				}
			}

			@Override
			public void mousePressed(MouseEvent e) {}

			@Override
			public void mouseReleased(MouseEvent e) {}
		});

		linkPanel.add(url);
		linkPanel.add(Box.createHorizontalGlue());
		top.add(linkPanel);

		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.add(top);

		showMessageDialog(panel, "JPedal Tutorials and documentation", JOptionPane.PLAIN_MESSAGE);
	}

	@Override
	public void dispose() {

		super.dispose();

		this.pageTitle = null;
		this.bookmarksTitle = null;

		this.signaturesTitle = null;
		this.layersTitle = null;

		this.layoutGroup = null;

		this.searchLayoutGroup = null;

		this.borderGroup = null;

		this.currentCommandListener = null;

		if (this.topButtons != null) this.topButtons.removeAll();
		this.topButtons = null;

		if (this.navButtons != null) this.navButtons.removeAll();
		this.navButtons = null;

		if (this.comboBoxBar != null) this.comboBoxBar.removeAll();
		this.comboBoxBar = null;

		if (this.currentMenu != null) this.currentMenu.removeAll();
		this.currentMenu = null;

		if (this.coords != null) this.coords.removeAll();
		this.coords = null;

		if (this.frame != null) this.frame.removeAll();
		this.frame = null;

		if (this.desktopPane != null) this.desktopPane.removeAll();
		this.desktopPane = null;

		if (this.navOptionsPanel != null) this.navOptionsPanel.removeAll();
		this.navOptionsPanel = null;

		if (this.scrollPane != null) this.scrollPane.removeAll();
		this.scrollPane = null;

		this.headFont = null;

		this.textFont = null;

		this.statusBar = null;

		this.downloadBar = null;

		this.pageCounter2 = null;

		this.pageCounter3 = null;

		this.optimizationLabel = null;

		if (this.signaturesTree != null) {
			this.signaturesTree.setCellRenderer(null);
			this.signaturesTree.removeAll();
		}
		this.signaturesTree = null;

		if (this.layersPanel != null) this.layersPanel.removeAll();
		this.layersPanel = null;

		this.user_dir = null;

		if (this.navToolBar != null) this.navToolBar.removeAll();
		this.navToolBar = null;

		if (this.pagesToolBar != null) this.pagesToolBar.removeAll();
		this.pagesToolBar = null;

		this.nextSearch = null;

		this.previousSearch = null;

		this.layersObject = null;

		// release memory at end
		if (this.memoryMonitor != null) {
			this.memoryMonitor.stop();
		}
	}

	/**
	 * get Map containing Form Objects setup for Unique Annotations
	 * 
	 * @return Map
	 */
	public Map getHotspots() {
		return this.objs;
	}

	public Point convertPDFto2D(int cx, int cy) {

		float scaling = getScaling();
		int inset = getPDFDisplayInset();
		int rotation = getRotation();

		if (this.decode_pdf.getDisplayView() != Display.SINGLE_PAGE) {
			// cx=0;
			// cy=0;
		}
		else
			if (rotation == 90) {
				int tmp = (cx - this.cropY);
				cx = (cy - this.cropX);
				cy = tmp;
			}
			else
				if ((rotation == 180)) {
					cx = -cx - (this.cropW + this.cropX);
					cy = (cy - this.cropY);
				}
				else
					if ((rotation == 270)) {
						int tmp = -(this.cropH + this.cropY) - cx;
						cx = (this.cropW + this.cropX) + cy;
						cy = tmp;
					}
					else {
						cx = (cx - this.cropX);
						cy = (this.cropH + this.cropY) - cy;
					}

		cx = (int) ((cx) * scaling);
		cy = (int) ((cy) * scaling);

		if (this.decode_pdf.getPageAlignment() == Display.DISPLAY_CENTERED) {
			int width = this.decode_pdf.getBounds().width;
			int pdfWidth = this.decode_pdf.getPDFWidth();

			if (this.decode_pdf.getDisplayView() != Display.SINGLE_PAGE) pdfWidth = (int) this.decode_pdf.getMaximumSize().getWidth();

			if (width > pdfWidth) cx = cx + ((width - pdfWidth) / (2));
		}

		cx = cx + inset;
		cy = cy + inset;

		return new Point(cx, cy);
	}

	/* used by JS to set values that dont need to save the new forms values */
	public boolean getFormsDirtyFlag() {
		return this.commonValues.isFormsChanged();
	}

	/* used by JS to set values that dont need to save the new forms values */
	public void setFormsDirtyFlag(boolean dirty) {
		this.commonValues.setFormsChanged(dirty);
	}

	public int getCurrentPage() {
		return this.commonValues.getCurrentPage();
	}

	public boolean getPageTurnScalingAppropriate() {
		return this.pageTurnScalingAppropriate;
	}

	public boolean getDragLeft() {
		return this.dragLeft;
	}

	public boolean getDragTop() {
		return this.dragTop;
	}

	public void setDragCorner(int a) {
		if (a == org.jpedal.external.OffsetOptions.INTERNAL_DRAG_CURSOR_BOTTOM_LEFT
				|| a == org.jpedal.external.OffsetOptions.INTERNAL_DRAG_CURSOR_TOP_LEFT || a == org.jpedal.external.OffsetOptions.INTERNAL_DRAG_BLANK) this.dragLeft = true;
		else this.dragLeft = false;

		if (a == org.jpedal.external.OffsetOptions.INTERNAL_DRAG_CURSOR_TOP_LEFT
				|| a == org.jpedal.external.OffsetOptions.INTERNAL_DRAG_CURSOR_TOP_RIGHT) this.dragTop = true;
		else this.dragTop = false;
	}

	public void setCursor(int type) {
		this.decode_pdf.setCursor(getCursor(type));
	}

	public BufferedImage getCursorImageForFX(int type) {
		switch (type) {
			case GRAB_CURSOR:
				try {
					return ImageIO.read(getURLForImage(this.iconLocation + "grab32.png"));
				}
				catch (Exception e) {
					return null;
				}
			case GRABBING_CURSOR:
				try {
					return ImageIO.read(getURLForImage(this.iconLocation + "grabbing32.png"));
				}
				catch (Exception e) {
					return null;
				}
			default:
				return null;
		}
	}

	public Cursor getCursor(int type) {
		switch (type) {
			case GRAB_CURSOR:
				if (this.grabCursor == null) {
					Toolkit kit = Toolkit.getDefaultToolkit();
					Image img = kit.getImage(getURLForImage(this.iconLocation + "grab32.png"));
					this.grabCursor = kit.createCustomCursor(img, new Point(8, 8), "grab");
				}
				return this.grabCursor;

			case GRABBING_CURSOR:
				if (this.grabbingCursor == null) {
					Toolkit kit = Toolkit.getDefaultToolkit();
					Image img = kit.getImage(getURLForImage(this.iconLocation + "grabbing32.png"));
					this.grabbingCursor = kit.createCustomCursor(img, new Point(8, 8), "grabbing");
				}
				return this.grabbingCursor;

			case PAN_CURSOR:
				if (this.panCursor == null) {
					Toolkit kit = Toolkit.getDefaultToolkit();
					Image img = kit.getImage(getURLForImage(this.iconLocation + "pan32.png"));
					this.panCursor = kit.createCustomCursor(img, new Point(10, 10), "pan");
				}
				return this.panCursor;

			case PAN_CURSORL:
				if (this.panCursorL == null) {
					Toolkit kit = Toolkit.getDefaultToolkit();
					Image img = kit.getImage(getURLForImage(this.iconLocation + "panl32.png"));
					this.panCursorL = kit.createCustomCursor(img, new Point(11, 10), "panl");
				}
				return this.panCursorL;

			case PAN_CURSORTL:
				if (this.panCursorTL == null) {
					Toolkit kit = Toolkit.getDefaultToolkit();
					Image img = kit.getImage(getURLForImage(this.iconLocation + "pantl32.png"));
					this.panCursorTL = kit.createCustomCursor(img, new Point(10, 10), "pantl");
				}
				return this.panCursorTL;

			case PAN_CURSORT:
				if (this.panCursorT == null) {
					Toolkit kit = Toolkit.getDefaultToolkit();
					Image img = kit.getImage(getURLForImage(this.iconLocation + "pant32.png"));
					this.panCursorT = kit.createCustomCursor(img, new Point(10, 11), "pant");
				}
				return this.panCursorT;

			case PAN_CURSORTR:
				if (this.panCursorTR == null) {
					Toolkit kit = Toolkit.getDefaultToolkit();
					Image img = kit.getImage(getURLForImage(this.iconLocation + "pantr32.png"));
					this.panCursorTR = kit.createCustomCursor(img, new Point(10, 10), "pantr");
				}
				return this.panCursorTR;

			case PAN_CURSORR:
				if (this.panCursorR == null) {
					Toolkit kit = Toolkit.getDefaultToolkit();
					Image img = kit.getImage(getURLForImage(this.iconLocation + "panr32.png"));
					this.panCursorR = kit.createCustomCursor(img, new Point(10, 10), "panr");
				}
				return this.panCursorR;

			case PAN_CURSORBR:
				if (this.panCursorBR == null) {
					Toolkit kit = Toolkit.getDefaultToolkit();
					Image img = kit.getImage(getURLForImage(this.iconLocation + "panbr32.png"));
					this.panCursorBR = kit.createCustomCursor(img, new Point(10, 10), "panbr");
				}
				return this.panCursorBR;

			case PAN_CURSORB:
				if (this.panCursorB == null) {
					Toolkit kit = Toolkit.getDefaultToolkit();
					Image img = kit.getImage(getURLForImage(this.iconLocation + "panb32.png"));
					this.panCursorB = kit.createCustomCursor(img, new Point(10, 10), "panb");
				}
				return this.panCursorB;

			case PAN_CURSORBL:
				if (this.panCursorBL == null) {
					Toolkit kit = Toolkit.getDefaultToolkit();
					Image img = kit.getImage(getURLForImage(this.iconLocation + "panbl32.png"));
					this.panCursorBL = kit.createCustomCursor(img, new Point(10, 10), "panbl");
				}
				return this.panCursorBL;

			default:
				return Cursor.getDefaultCursor();

		}
	}

	public void rescanPdfLayers() {
		try {
			if (SwingUtilities.isEventDispatchThread()) {
				// refresh the layers tab so JS updates are carried across.
				syncTreeDisplay(this.topLayer, true);
				this.layersPanel.invalidate();
				this.layersPanel.repaint();
			}
			else {
				final Runnable doPaintComponent = new Runnable() {
					@Override
					public void run() {
						// refresh the layers tab so JS updates are carried across.
						syncTreeDisplay(SwingGUI.this.topLayer, true);
						SwingGUI.this.layersPanel.invalidate();
						SwingGUI.this.layersPanel.repaint();
					}
				};
				SwingUtilities.invokeAndWait(doPaintComponent);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * generate thumbnails for display
	 */
	private class ScrollMouseListener extends MouseAdapter {

		// public void mouseClicked(MouseEvent e) {
		//
		// //this.
		// //if(debugThumbnail)
		// System.out.println("clicked");
		//
		// scrollListener.mousePressed=true;
		// }

		// public void mouseExited(MouseEvent e) {
		//
		// //if(debugThumbnail)
		// System.out.println("Exit");
		//
		// // scrollListener.mousePressed=true;
		// }
		//
		// public void mouseEntered(MouseEvent e) {
		//
		// //if(debugThumbnail)
		// System.out.println("mouseEntered");
		//
		// //scrollListener.mousePressed=true;
		// }
		//
		// public void mouseDragged(MouseEvent e) {
		//
		// //if(debugThumbnail)
		// System.out.println("mouseDragged");
		//
		// scrollListener.mousePressed=true;
		// }

		@Override
		public void mousePressed(MouseEvent e) {

			if (SwingGUI.this.debugThumbnail) System.out.println("pressed");

			SwingGUI.this.scrollListener.mousePressed = true;

			SwingGUI.this.scrollListener.usingMouseClick = true;
			// if(mousePressed)
			SwingGUI.this.scrollListener.startTimer();

		}

		@Override
		public void mouseReleased(MouseEvent e) {

			if (SwingGUI.this.debugThumbnail) System.out.println("release");

			if (SwingGUI.this.scrollListener.mousePressed) SwingGUI.this.scrollListener.releaseAndUpdate();

			SwingGUI.this.scrollListener.mousePressed = false;

		}
	}

	/**
	 * Class to handle thumbnail preview
	 */
	private class ScrollListener implements AdjustmentListener {

		java.util.Timer t = null;
		int pNum = -1, lastPageSent = -1;
		boolean usingMouseClick = false;
		boolean mousePressed = false;
		boolean showLast = false;
		public BufferedImage lastImage;
		public boolean ignoreChange = false;

		private void startTimer() {
			// turn if off if running
			if (this.t != null) this.t.cancel();

			// Removed this code as changes the functionality beyond the original.
			// Kept here as a comment incase wanted at some point in the future
			/**
			 * //If first thumnail to display, set no delay, this allows thumnails to appear from the very begining //Only used when mouse pressed and
			 * no thumbnail so far, //otherwise scroll wheel will decode each page from start page and scroll destination if(mousePressed && !showLast
			 * && lastImage==null){ TimerTask listener = new PageListener(); t = new java.util.Timer(); t.schedule(listener, 0); }else //This will
			 * delay thumbnail update so we don't spam viewer with update requests /
			 **/

			if (SwingGUI.this.thumbnails.isShownOnscreen()) {
				long delay = 175;

				// Increase delay when using mouse wheel, otherwise scroll is impractical
				if (!this.usingMouseClick) {
					delay = 500;
				}
				// restart - if its not stopped it will trigger page update
				TimerTask listener = new PageListener();
				this.t = new java.util.Timer();
				this.t.schedule(listener, delay);
			}
		}

		/**
		 * get page and start timer to creatye thumbnail If not moved, will draw this page
		 */
		@Override
		public void adjustmentValueChanged(AdjustmentEvent e) {

			this.pNum = e.getAdjustable().getValue() + 1;

			// If mouse has not been pressed, do not create preview thumbnail
			// This prevents thumbnails on scroll which causes some issue.
			if (!this.mousePressed) {
				this.showLast = false;
				this.lastImage = null;
			}

			// Show loading image
			if (this.showLast) {
				// Use stored image
				((SingleDisplay) SwingGUI.this.decode_pdf.getExternalHandler(Options.Display)).setPreviewThumbnail(this.lastImage, "Page "
						+ this.pNum + " of " + SwingGUI.this.decode_pdf.getPageCount());
				SwingGUI.this.decode_pdf.repaint();
			}
			else
				if (this.lastImage != null) {
					// Create loading image
					BufferedImage img = new BufferedImage(this.lastImage.getWidth(), this.lastImage.getHeight(), this.lastImage.getType());
					Graphics2D g2 = (Graphics2D) img.getGraphics();

					// Draw last image
					g2.drawImage(this.lastImage, 0, 0, null);

					// Gray out
					g2.setPaint(new Color(0, 0, 0, 130));
					g2.fillRect(0, 0, this.lastImage.getWidth(), this.lastImage.getHeight());

					// Draw loading string
					String l = "Loading...";
					int textW = g2.getFontMetrics().stringWidth(l);
					int textH = g2.getFontMetrics().getHeight();
					g2.setPaint(Color.WHITE);
					g2.drawString(l, (this.lastImage.getWidth() / 2) - (textW / 2), (this.lastImage.getHeight() / 2) + (textH / 2));

					// Store and set to reuse
					this.lastImage = img;
					this.showLast = true;

					// Update
					((SingleDisplay) SwingGUI.this.decode_pdf.getExternalHandler(Options.Display)).setPreviewThumbnail(img, "Page " + this.pNum
							+ " of " + SwingGUI.this.decode_pdf.getPageCount());
					SwingGUI.this.decode_pdf.repaint();
				}

			// System.out.println(pNum);
			// if(mousePressed)
			startTimer();
			// else if(!ignoreChange)
			// releaseAndUpdate();

			// reset
			this.ignoreChange = false;

		}

		public synchronized void setThumbnail() {

			// if(mousePressed){

			if (this.lastPageSent != this.pNum) {

				this.lastPageSent = this.pNum;

				try {

					// decode_pdf.waitForDecodingToFinish();

					BufferedImage image = SwingGUI.this.thumbnails.getImage(this.pNum);

					if (SwingGUI.this.debugThumbnail) System.out.println(this.pNum + " " + image);

					// Store and turn off using stored image
					this.lastImage = image;
					this.showLast = false;
					((SingleDisplay) SwingGUI.this.decode_pdf.getExternalHandler(Options.Display)).setPreviewThumbnail(image, "Page " + this.pNum
							+ " of " + SwingGUI.this.decode_pdf.getPageCount());

					SwingGUI.this.decode_pdf.repaint();

				}
				catch (Exception ee) {}
				// }
			}
		}

		public void releaseAndUpdate() {

			if (this.usingMouseClick) this.usingMouseClick = false;

			// turn if off if running
			if (this.t != null) this.t.cancel();

			// System.out.println("releaseAndUpdate");

			if (SwingGUI.this.decode_pdf.getDisplayView() != Display.PAGEFLOW) ((SingleDisplay) SwingGUI.this.decode_pdf
					.getExternalHandler(Options.Display)).setPreviewThumbnail(null,
					"Page " + this.pNum + " of " + SwingGUI.this.decode_pdf.getPageCount());

			// if(pNum>0 && lastPage!=pNum){

			SwingGUI.this.currentCommands.gotoPage(Integer.toString(this.pNum));
			// }else
			// decode_pdf.clearScreen();

			SwingGUI.this.decode_pdf.repaint();
		}

		/**
		 * used to update preview thumbnail to next page
		 */
		class PageListener extends TimerTask {
			@Override
			public void run() {

				if (ScrollListener.this.mousePressed) setThumbnail();
				else {
					ScrollListener.this.usingMouseClick = false;
					releaseAndUpdate();
				}
			}
		}
	}
}