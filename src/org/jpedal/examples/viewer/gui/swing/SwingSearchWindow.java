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
 * SwingSearchWindow.java
 * ---------------
 */
package org.jpedal.examples.viewer.gui.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.jpedal.PdfDecoder;
import org.jpedal.SingleDisplay;
import org.jpedal.examples.viewer.Values;
import org.jpedal.examples.viewer.Viewer;
import org.jpedal.examples.viewer.gui.SwingGUI;
import org.jpedal.examples.viewer.gui.generic.GUISearchWindow;
import org.jpedal.exception.PdfException;
import org.jpedal.grouping.DefaultSearchListener;
import org.jpedal.grouping.PdfGroupingAlgorithms;
import org.jpedal.grouping.SearchListener;
import org.jpedal.grouping.SearchType;
import org.jpedal.objects.PdfPageData;
import org.jpedal.utils.Messages;
import org.jpedal.utils.SwingWorker;
import org.jpedal.utils.repositories.Vector_Rectangle;

/** provides interactive search Window and search capabilities */
public class SwingSearchWindow extends JFrame implements GUISearchWindow {

	private static final long serialVersionUID = 8701391157577423692L;
	public static int SEARCH_EXTERNAL_WINDOW = 0;
	public static int SEARCH_TABBED_PANE = 1;
	public static int SEARCH_MENU_BAR = 2;

	private boolean backGroundSearch = false;

	int style = 0;

	/** flag to stop multiple listeners */
	private boolean isSetup = false;

	boolean usingMenuBarSearch = false;

	int lastPage = -1;

	String defaultMessage = "Search PDF Here";

	JProgressBar progress = new JProgressBar(0, 100);
	int pageIncrement = 0;
	JTextField searchText = null;
	JTextField searchCount;
	DefaultListModel listModel;
	SearchList resultsList;
	JLabel label = null;

	private JPanel advancedPanel;
	private JComboBox searchType;
	private JCheckBox wholeWordsOnlyBox, caseSensitiveBox, multiLineBox, highlightAll, searchAll, useRegEx;

	@Override
	public void setWholeWords(boolean wholeWords) {
		this.wholeWordsOnlyBox.setSelected(wholeWords);
	}

	@Override
	public void setCaseSensitive(boolean caseSensitive) {
		this.caseSensitiveBox.setSelected(caseSensitive);
	}

	@Override
	public void setMultiLine(boolean multiLine) {
		this.multiLineBox.setSelected(multiLine);
	}

	public void setHighlightAll(boolean highlightAllOnPage) {
		this.highlightAll.setSelected(highlightAllOnPage);
	}

	public void setRegularExpressionUsage(boolean RegEx) {
		this.useRegEx.setSelected(RegEx);
	}

	ActionListener AL = null;
	ListSelectionListener LSL = null;
	WindowListener WL;
	KeyListener KL;

	/** swing thread to search in background */
	SwingWorker searcher = null;

	/** flag to show searching taking place */
	public boolean isSearch = false;

	/** Flag to show search has happened and needs reset */
	public boolean hasSearched = false;

	public boolean requestInterupt = false;

	JButton searchButton = null;

	/** number fo search items */
	private int itemFoundCount = 0;

	/** used when fiding text to highlight on page */
	Map textPages = new HashMap();
	Map textRectangles = new HashMap();

	/** Current Search value */
	String[] searchTerms = { "" };

	/** Search this page only */
	boolean singlePageSearch = false;

	final JPanel nav = new JPanel();

	Values commonValues;
	SwingGUI currentGUI;
	PdfDecoder decode_pdf;

	int searchTypeParameters = SearchType.DEFAULT;

	int firstPageWithResults = 0;

	/** deletes message when user starts typing */
	private boolean deleteOnClick;

	public SwingSearchWindow(SwingGUI currentGUI) {
		this.currentGUI = currentGUI;
		this.setName("searchFrame");
	}

	@Override
	public Component getContentPanel() {
		return getContentPane();
	}

	@Override
	public boolean isSearching() {
		return this.isSearch;
	}

	@Override
	public void init(final PdfDecoder dec, final Values values) {

		this.decode_pdf = dec;
		this.commonValues = values;

		if (this.isSetup) { // global variable so do NOT reinitialise
			this.searchCount.setText(Messages.getMessage("PdfViewerSearch.ItemsFound") + ' ' + this.itemFoundCount);
			this.searchText.selectAll();
			this.searchText.grabFocus();
		}
		else {
			this.isSetup = true;

			setTitle(Messages.getMessage("PdfViewerSearchGUITitle.DefaultMessage"));

			this.defaultMessage = Messages.getMessage("PdfViewerSearchGUI.DefaultMessage");

			this.searchText = new JTextField(10);
			this.searchText.setText(this.defaultMessage);
			this.searchText.setName("searchText");

			this.searchButton = new JButton(Messages.getMessage("PdfViewerSearch.Button"));

			this.advancedPanel = new JPanel(new GridBagLayout());

			this.searchType = new JComboBox(new String[] { Messages.getMessage("PdfViewerSearch.MatchWhole"),
					Messages.getMessage("PdfViewerSearch.MatchAny") });

			this.wholeWordsOnlyBox = new JCheckBox(Messages.getMessage("PdfViewerSearch.WholeWords"));
			this.wholeWordsOnlyBox.setName("wholeWords");

			this.caseSensitiveBox = new JCheckBox(Messages.getMessage("PdfViewerSearch.CaseSense"));
			this.caseSensitiveBox.setName("caseSensitive");

			this.multiLineBox = new JCheckBox(Messages.getMessage("PdfViewerSearch.MultiLine"));
			this.multiLineBox.setName("multiLine");

			this.highlightAll = new JCheckBox(Messages.getMessage("PdfViewerSearch.HighlightsCheckBox"));
			this.highlightAll.setName("highlightAll");

			this.useRegEx = new JCheckBox(Messages.getMessage("PdfViewerSearch.RegExCheckBox"));
			this.useRegEx.setName("useregex");

			this.searchType.setName("combo");

			GridBagConstraints c = new GridBagConstraints();

			this.advancedPanel.setPreferredSize(new Dimension(this.advancedPanel.getPreferredSize().width, 150));
			c.gridx = 0;
			c.gridy = 0;

			c.anchor = GridBagConstraints.PAGE_START;
			c.fill = GridBagConstraints.HORIZONTAL;

			c.weightx = 1;
			c.weighty = 0;
			this.advancedPanel.add(new JLabel(Messages.getMessage("PdfViewerSearch.ReturnResultsAs")), c);

			c.insets = new Insets(5, 0, 0, 0);
			c.gridy = 1;
			this.advancedPanel.add(this.searchType, c);

			c.gridy = 2;
			this.advancedPanel.add(new JLabel(Messages.getMessage("PdfViewerSearch.AdditionalOptions")), c);

			c.insets = new Insets(0, 0, 0, 0);
			c.weighty = 1;
			c.gridy = 3;
			this.advancedPanel.add(this.wholeWordsOnlyBox, c);
			c.weighty = 1;
			c.gridy = 4;
			this.advancedPanel.add(this.caseSensitiveBox, c);

			c.weighty = 1;
			c.gridy = 5;
			this.advancedPanel.add(this.multiLineBox, c);

			c.weighty = 1;
			c.gridy = 6;
			this.advancedPanel.add(this.highlightAll, c);

			c.weighty = 1;
			c.gridy = 7;
			this.advancedPanel.add(this.useRegEx, c);

			this.advancedPanel.setVisible(false);

			this.nav.setLayout(new BorderLayout());

			this.WL = new WindowListener() {
				@Override
				public void windowOpened(WindowEvent arg0) {}

				// flush objects on close
				@Override
				public void windowClosing(WindowEvent arg0) {

					removeSearchWindow(true);
				}

				@Override
				public void windowClosed(WindowEvent arg0) {}

				@Override
				public void windowIconified(WindowEvent arg0) {}

				@Override
				public void windowDeiconified(WindowEvent arg0) {}

				@Override
				public void windowActivated(WindowEvent arg0) {}

				@Override
				public void windowDeactivated(WindowEvent arg0) {}
			};

			this.addWindowListener(this.WL);

			this.nav.add(this.searchButton, BorderLayout.EAST);

			this.nav.add(this.searchText, BorderLayout.CENTER);

			this.searchAll = new JCheckBox();
			this.searchAll.setSelected(true);
			this.searchAll.setText(Messages.getMessage("PdfViewerSearch.CheckBox"));

			JPanel topPanel = new JPanel();
			topPanel.setLayout(new BorderLayout());
			topPanel.add(this.searchAll, BorderLayout.NORTH);

			this.label = new JLabel("<html><center> " + "Show Advanced");
			this.label.setForeground(Color.blue);
			this.label.setName("advSearch");

			this.label.addMouseListener(new MouseListener() {
				boolean isVisible = false;

				String text = "Show Advanced";

				@Override
				public void mouseEntered(MouseEvent e) {
					if (SingleDisplay.allowChangeCursor) SwingSearchWindow.this.nav.setCursor(new Cursor(Cursor.HAND_CURSOR));
					SwingSearchWindow.this.label.setText("<html><center><a href=" + this.text + '>' + this.text + "</a></center>");
				}

				@Override
				public void mouseExited(MouseEvent e) {
					if (SingleDisplay.allowChangeCursor) SwingSearchWindow.this.nav.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
					SwingSearchWindow.this.label.setText("<html><center>" + this.text);
				}

				@Override
				public void mouseClicked(MouseEvent e) {
					if (this.isVisible) {
						this.text = Messages.getMessage("PdfViewerSearch.ShowOptions");
						SwingSearchWindow.this.label.setText("<html><center><a href=" + this.text + '>' + this.text + "</a></center>");
						SwingSearchWindow.this.advancedPanel.setVisible(false);
					}
					else {
						this.text = Messages.getMessage("PdfViewerSearch.HideOptions");
						SwingSearchWindow.this.label.setText("<html><center><a href=" + this.text + '>' + this.text + "</a></center>");
						SwingSearchWindow.this.advancedPanel.setVisible(true);
					}

					this.isVisible = !this.isVisible;
				}

				@Override
				public void mousePressed(MouseEvent e) {}

				@Override
				public void mouseReleased(MouseEvent e) {}
			});

			this.label.setBorder(BorderFactory.createEmptyBorder(3, 4, 4, 4));
			topPanel.add(this.label, BorderLayout.SOUTH);
			// nav.

			this.nav.add(topPanel, BorderLayout.NORTH);
			this.itemFoundCount = 0;
			this.textPages.clear();
			this.textRectangles.clear();
			this.listModel = null;

			this.searchCount = new JTextField(Messages.getMessage("PdfViewerSearch.ItemsFound") + ' ' + this.itemFoundCount);
			this.searchCount.setEditable(false);
			this.nav.add(this.searchCount, BorderLayout.SOUTH);

			this.listModel = new DefaultListModel();
			this.resultsList = new SearchList(this.listModel, this.textPages, this.textRectangles);
			this.resultsList.setName("results");

			// <link><a name="search" />
			/**
			 * highlight text on item selected
			 */
			this.LSL = new ListSelectionListener() {
				@Override
				public void valueChanged(ListSelectionEvent e) {
					/**
					 * Only do something on mouse button up, prevents this code being called twice on mouse click
					 */
					if (!e.getValueIsAdjusting()) {

						if (!Values.isProcessing()) {// {if (!event.getValueIsAdjusting()) {

							float scaling = SwingSearchWindow.this.currentGUI.getScaling();
							// int inset=currentGUI.getPDFDisplayInset();

							int id = SwingSearchWindow.this.resultsList.getSelectedIndex();

							SwingSearchWindow.this.decode_pdf.getTextLines().clearHighlights();
							// System.out.println("clicked pdf = "+decode_pdf.getClass().getName() + "@" +
							// Integer.toHexString(decode_pdf.hashCode()));

							if (id != -1) {

								Integer key = id;
								Object newPage = SwingSearchWindow.this.textPages.get(key);

								if (newPage != null) {
									int nextPage = (Integer) newPage;

									// move to new page
									if (SwingSearchWindow.this.commonValues.getCurrentPage() != nextPage) {

										SwingSearchWindow.this.commonValues.setCurrentPage(nextPage);

										SwingSearchWindow.this.currentGUI.resetStatusMessage(Messages.getMessage("PdfViewer.LoadingPage") + ' '
												+ SwingSearchWindow.this.commonValues.getCurrentPage());

										/** reset as rotation may change! */
										SwingSearchWindow.this.decode_pdf.setPageParameters(scaling,
												SwingSearchWindow.this.commonValues.getCurrentPage());

										// decode the page
										SwingSearchWindow.this.currentGUI.decodePage(false);

										SwingSearchWindow.this.decode_pdf.invalidate();
									}

									while (Values.isProcessing()) {
										// Ensure page has been processed else highlight may be incorrect
										try {
											Thread.sleep(500);
										}
										catch (InterruptedException ee) {
											ee.printStackTrace(); // To change body of catch statement use File | Settings | File Templates.
										}
									}

									/**
									 * Highlight all search results on page.
									 */
									if ((SwingSearchWindow.this.searchTypeParameters & SearchType.HIGHLIGHT_ALL_RESULTS) == SearchType.HIGHLIGHT_ALL_RESULTS) {

										// PdfHighlights.clearAllHighlights(decode_pdf);
										Rectangle[] showAllOnPage;
										Vector_Rectangle storageVector = new Vector_Rectangle();
										int lastPage = -1;
										for (int k = 0; k != SwingSearchWindow.this.resultsList.getModel().getSize(); k++) {
											Object page = SwingSearchWindow.this.textPages.get(k);

											if (page != null) {

												int currentPage = (Integer) page;
												if (currentPage != lastPage) {
													storageVector.trim();
													showAllOnPage = storageVector.get();

													for (int p = 0; p != showAllOnPage.length; p++) {
														System.out.println(showAllOnPage[p]);
													}

													SwingSearchWindow.this.decode_pdf.getTextLines().addHighlights(showAllOnPage, true, lastPage);
													lastPage = currentPage;
													storageVector = new Vector_Rectangle();
												}

												Object highlight = SwingSearchWindow.this.textRectangles.get(k);

												if (highlight instanceof Rectangle) {
													storageVector.addElement((Rectangle) highlight);
												}
												if (highlight instanceof Rectangle[]) {
													Rectangle[] areas = (Rectangle[]) highlight;
													for (int i = 0; i != areas.length; i++) {
														storageVector.addElement(areas[i]);
													}
												}
												// decode_pdf.addToHighlightAreas(decode_pdf, storageVector, currentPage);
												// }
											}
										}
										storageVector.trim();
										showAllOnPage = storageVector.get();

										SwingSearchWindow.this.decode_pdf.getTextLines().addHighlights(showAllOnPage, true, lastPage);
									}
									else {
										// PdfHighlights.clearAllHighlights(decode_pdf);
										Object page = SwingSearchWindow.this.textPages.get(key);
										int currentPage = (Integer) page;

										Vector_Rectangle storageVector = new Vector_Rectangle();
										Rectangle scroll = null;
										Object highlight = SwingSearchWindow.this.textRectangles.get(key);
										if (highlight instanceof Rectangle) {
											storageVector.addElement((Rectangle) highlight);
											scroll = (Rectangle) highlight;
										}

										if (highlight instanceof Rectangle[]) {
											Rectangle[] areas = (Rectangle[]) highlight;
											scroll = areas[0];
											for (int i = 0; i != areas.length; i++) {
												storageVector.addElement(areas[i]);
											}
										}
										SwingSearchWindow.this.currentGUI.currentCommands.scrollRectToHighlight(scroll, currentPage);
										storageVector.trim();
										SwingSearchWindow.this.decode_pdf.getTextLines().addHighlights(storageVector.get(), true, currentPage);
										// PdfHighlights.addToHighlightAreas(decode_pdf, storageVector, currentPage);

									}

									SwingSearchWindow.this.decode_pdf.invalidate();
									SwingSearchWindow.this.decode_pdf.repaint();
									SwingSearchWindow.this.currentGUI.zoom(false);
								}
							}
						}

						// When page changes make sure only relevant navigation buttons are displayed
						if (SwingSearchWindow.this.commonValues.getCurrentPage() == 1) SwingSearchWindow.this.currentGUI
								.setBackNavigationButtonsEnabled(false);
						else SwingSearchWindow.this.currentGUI.setBackNavigationButtonsEnabled(true);

						if (SwingSearchWindow.this.commonValues.getCurrentPage() == SwingSearchWindow.this.decode_pdf.getPageCount()) SwingSearchWindow.this.currentGUI
								.setForwardNavigationButtonsEnabled(false);
						else SwingSearchWindow.this.currentGUI.setForwardNavigationButtonsEnabled(true);

					}
					else {
						SwingSearchWindow.this.resultsList.repaint();

					}
				}
			};

			this.resultsList.addListSelectionListener(this.LSL);
			this.resultsList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);

			// setup searching
			// if(AL==null){
			this.AL = new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {

					if (!SwingSearchWindow.this.isSearch) {

						try {
							SwingSearchWindow.this.searchTypeParameters = SearchType.DEFAULT;

							if (SwingSearchWindow.this.wholeWordsOnlyBox.isSelected()) SwingSearchWindow.this.searchTypeParameters |= SearchType.WHOLE_WORDS_ONLY;

							if (SwingSearchWindow.this.caseSensitiveBox.isSelected()) SwingSearchWindow.this.searchTypeParameters |= SearchType.CASE_SENSITIVE;

							if (SwingSearchWindow.this.multiLineBox.isSelected()) SwingSearchWindow.this.searchTypeParameters |= SearchType.MUTLI_LINE_RESULTS;

							if (SwingSearchWindow.this.highlightAll.isSelected()) SwingSearchWindow.this.searchTypeParameters |= SearchType.HIGHLIGHT_ALL_RESULTS;

							if (SwingSearchWindow.this.useRegEx.isSelected()) SwingSearchWindow.this.searchTypeParameters |= SearchType.USE_REGULAR_EXPRESSIONS;

							String textToFind = SwingSearchWindow.this.searchText.getText().trim();
							if (SwingSearchWindow.this.searchType.getSelectedIndex() == 0) { // find exact word or phrase
								SwingSearchWindow.this.searchTerms = new String[] { textToFind };
							}
							else { // match any of the words
								SwingSearchWindow.this.searchTerms = textToFind.split(" ");
								for (int i = 0; i < SwingSearchWindow.this.searchTerms.length; i++) {
									SwingSearchWindow.this.searchTerms[i] = SwingSearchWindow.this.searchTerms[i].trim();
								}
							}

							SwingSearchWindow.this.singlePageSearch = !SwingSearchWindow.this.searchAll.isSelected();

							searchText();
						}
						catch (Exception e1) {
							e1.printStackTrace();
						}
					}
					else {
						SwingSearchWindow.this.requestInterupt = true;
						// searcher.interrupt();
						SwingSearchWindow.this.isSearch = false;
						SwingSearchWindow.this.searchButton.setText(Messages.getMessage("PdfViewerSearch.Button"));
					}
					SwingSearchWindow.this.currentGUI.getPdfDecoder().requestFocusInWindow();
				}
			};

			this.searchButton.addActionListener(this.AL);
			// }

			this.searchText.selectAll();
			this.deleteOnClick = true;

			this.KL = new KeyListener() {
				@Override
				public void keyTyped(KeyEvent e) {
					if (SwingSearchWindow.this.searchText.getText().length() == 0) {
						SwingSearchWindow.this.currentGUI.nextSearch.setVisible(false);
						SwingSearchWindow.this.currentGUI.previousSearch.setVisible(false);
					}

					// clear when user types
					if (SwingSearchWindow.this.deleteOnClick) {
						SwingSearchWindow.this.deleteOnClick = false;
						SwingSearchWindow.this.searchText.setText("");
					}
					int id = e.getID();
					if (id == KeyEvent.KEY_TYPED) {
						char key = e.getKeyChar();

						if (key == '\n') {

							if (!SwingSearchWindow.this.decode_pdf.isOpen()) {
								SwingSearchWindow.this.currentGUI.showMessageDialog("File must be open before you can search.");
							}
							else {
								try {
									SwingSearchWindow.this.currentGUI.nextSearch.setVisible(true);
									SwingSearchWindow.this.currentGUI.previousSearch.setVisible(true);

									SwingSearchWindow.this.currentGUI.nextSearch.setEnabled(false);
									SwingSearchWindow.this.currentGUI.previousSearch.setEnabled(false);

									SwingSearchWindow.this.isSearch = false;
									SwingSearchWindow.this.searchTypeParameters = SearchType.DEFAULT;

									if (SwingSearchWindow.this.wholeWordsOnlyBox.isSelected()) SwingSearchWindow.this.searchTypeParameters |= SearchType.WHOLE_WORDS_ONLY;

									if (SwingSearchWindow.this.caseSensitiveBox.isSelected()) SwingSearchWindow.this.searchTypeParameters |= SearchType.CASE_SENSITIVE;

									if (SwingSearchWindow.this.multiLineBox.isSelected()) SwingSearchWindow.this.searchTypeParameters |= SearchType.MUTLI_LINE_RESULTS;

									if (SwingSearchWindow.this.highlightAll.isSelected()) SwingSearchWindow.this.searchTypeParameters |= SearchType.HIGHLIGHT_ALL_RESULTS;

									if (SwingSearchWindow.this.useRegEx.isSelected()) SwingSearchWindow.this.searchTypeParameters |= SearchType.USE_REGULAR_EXPRESSIONS;

									String textToFind = SwingSearchWindow.this.searchText.getText().trim();
									if (SwingSearchWindow.this.searchType.getSelectedIndex() == 0) { // find exact word or phrase
										SwingSearchWindow.this.searchTerms = new String[] { textToFind };
									}
									else { // match any of the words
										SwingSearchWindow.this.searchTerms = textToFind.split(" ");
										for (int i = 0; i < SwingSearchWindow.this.searchTerms.length; i++) {
											SwingSearchWindow.this.searchTerms[i] = SwingSearchWindow.this.searchTerms[i].trim();
										}
									}

									SwingSearchWindow.this.singlePageSearch = !SwingSearchWindow.this.searchAll.isSelected();

									searchText();

									SwingSearchWindow.this.currentGUI.getPdfDecoder().requestFocusInWindow();
								}
								catch (Exception e1) {
									e1.printStackTrace();
								}
							}
						}
					}
				}

				@Override
				public void keyPressed(KeyEvent arg0) {}

				@Override
				public void keyReleased(KeyEvent arg0) {}
			};

			this.searchText.addKeyListener(this.KL);
			if (this.style == SEARCH_EXTERNAL_WINDOW || this.style == SEARCH_TABBED_PANE) {
				// build frame
				JScrollPane scrollPane = new JScrollPane();
				scrollPane.getViewport().add(this.resultsList);
				scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
				scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
				scrollPane.getVerticalScrollBar().setUnitIncrement(80);
				scrollPane.getHorizontalScrollBar().setUnitIncrement(80);

				getContentPane().setLayout(new BorderLayout());
				getContentPane().add(scrollPane, BorderLayout.CENTER);
				getContentPane().add(this.nav, BorderLayout.NORTH);
				getContentPane().add(this.advancedPanel, BorderLayout.SOUTH);

				// position and size
				Container frame = this.currentGUI.getFrame();
				if (this.commonValues.getModeOfOperation() == Values.RUNNING_APPLET) {
					if (this.currentGUI.getFrame() instanceof JFrame) frame = ((JFrame) this.currentGUI.getFrame()).getContentPane();
				}

				if (this.style == SEARCH_EXTERNAL_WINDOW) {
					int w = 230;

					int h = frame.getHeight();
					int x1 = frame.getLocationOnScreen().x;
					int x = frame.getWidth() + x1;
					int y = frame.getLocationOnScreen().y;
					Dimension d = Toolkit.getDefaultToolkit().getScreenSize();

					int width = d.width;
					if (x + w > width && this.style == SEARCH_EXTERNAL_WINDOW) {
						x = width - w;
						frame.setSize(x - x1, frame.getHeight());
					}

					setSize(w, h);
					setLocation(x, y);
				}
				this.searchAll.setFocusable(false);

				this.searchText.grabFocus();

			}
			else {
				// Whole Panel not used, take what is needed
				this.currentGUI.setSearchText(this.searchText);
			}
		}
	}

	/**
	 * find text on page withSwingWindow
	 */
	@Override
	public void findWithoutWindow(final PdfDecoder dec, final Values values, int searchType, boolean listOfTerms, boolean singlePageOnly,
			String searchValue) {

		if (!this.isSearch) {
			this.backGroundSearch = true;
			this.isSearch = true;

			this.decode_pdf = dec;
			this.commonValues = values;

			this.decode_pdf.setLayout(new BorderLayout());
			this.decode_pdf.add(this.progress, BorderLayout.SOUTH);
			this.progress.setValue(0);
			this.progress.setMaximum(this.commonValues.getPageCount());
			this.progress.setVisible(true);
			this.decode_pdf.validate();

			String textToFind = searchValue;
			if (!listOfTerms) { // find exact word or phrase
				this.searchTerms = new String[] { textToFind };
			}
			else { // match any of the words
				this.searchTerms = textToFind.split(" ");
				for (int i = 0; i < this.searchTerms.length; i++) {
					this.searchTerms[i] = this.searchTerms[i].trim();
				}
			}

			this.searchTypeParameters = searchType;

			this.singlePageSearch = singlePageOnly;

			find(dec, values);

		}
		else {
			this.currentGUI.showMessageDialog("Please wait for search to finish before starting another.");
		}
	}

	/**
	 * find text on page
	 */
	@Override
	public void find(final PdfDecoder dec, final Values values) {

		// System.out.println("clicked pdf = "+decode_pdf.getClass().getName() + "@" + Integer.toHexString(decode_pdf.hashCode()));

		/**
		 * pop up new window to search text (initialise if required
		 */
		if (!this.backGroundSearch) {
			init(dec, values);
			if (this.style == SEARCH_EXTERNAL_WINDOW) setVisible(true);
		}
		else {
			try {
				searchText();
			}
			catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@Override
	public void removeSearchWindow(boolean justHide) {

		// System.out.println("remove search window");

		setVisible(false);

		setVisible(false);

		if (this.searcher != null) this.searcher.interrupt();

		if (this.isSetup && !justHide) {
			if (this.listModel != null) this.listModel.clear();// removeAllElements();

			// searchText.setText(defaultMessage);
			// searchAll=null;
			// if(nav!=null)
			// nav.removeAll();

			this.itemFoundCount = 0;
			this.isSearch = false;

		}

		// lose any highlights and force redraw with non-existent box
		if (this.decode_pdf != null) {
			this.decode_pdf.getTextLines().clearHighlights();
			this.decode_pdf.repaint();
		}
	}

	private void searchText() throws Exception {

		/** if running terminate first */
		if ((this.searcher != null)) this.searcher.interrupt();

		if (this.style == SEARCH_MENU_BAR) {
			this.usingMenuBarSearch = true;
		}
		else {
			this.usingMenuBarSearch = false;
		}

		// reset list of pages searched
		this.lastPage = -1;

		if (this.listModel == null) this.listModel = new DefaultListModel();

		if (this.resultsList == null) this.resultsList = new SearchList(this.listModel, this.textPages, this.textRectangles);

		this.resultsList.setStatus(SearchList.SEARCH_INCOMPLETE);

		if (!this.backGroundSearch) {
			this.searchButton.setText(Messages.getMessage("PdfViewerSearchButton.Stop"));
			this.searchButton.invalidate();
			this.searchButton.repaint();

			this.searchCount.setText(Messages.getMessage("PdfViewerSearch.Scanning1"));
			this.searchCount.repaint();
		}

		this.searcher = new SwingWorker() {
			@Override
			public Object construct() {

				SwingSearchWindow.this.isSearch = true;
				SwingSearchWindow.this.hasSearched = true;

				try {

					// System.out.println("seareching pdf = "+decode_pdf.getClass().getName() + "@" + Integer.toHexString(decode_pdf.hashCode()));

					SwingSearchWindow.this.listModel.removeAllElements();

					if (!SwingSearchWindow.this.backGroundSearch) SwingSearchWindow.this.resultsList.repaint();

					SwingSearchWindow.this.textPages.clear();

					SwingSearchWindow.this.textRectangles.clear();
					SwingSearchWindow.this.itemFoundCount = 0;
					SwingSearchWindow.this.decode_pdf.getTextLines().clearHighlights();

					// System.out.println("textToFind = "+textToFind);

					// get page sizes
					PdfPageData pageSize = SwingSearchWindow.this.decode_pdf.getPdfPageData();

					// int x1, y1, x2, y2;

					// page range
					// int startPage = 1;
					// int endPage = commonValues.getPageCount() + 1;

					if (SwingSearchWindow.this.singlePageSearch || SwingSearchWindow.this.usingMenuBarSearch) {
						// startPage = commonValues.getCurrentPage();
						// endPage = startPage + 1;
						if (SwingSearchWindow.this.singlePageSearch) {
							searchPageRange(pageSize, SwingSearchWindow.this.commonValues.getCurrentPage(),
									SwingSearchWindow.this.commonValues.getCurrentPage() + 1);
						}
						else {
							for (int p = 0; p != SwingSearchWindow.this.commonValues.getPageCount() + 1
									&& SwingSearchWindow.this.resultsList.getResultCount() < 1; p++) {
								int page = SwingSearchWindow.this.commonValues.getCurrentPage() + p;
								if (page > SwingSearchWindow.this.commonValues.getPageCount()) page -= SwingSearchWindow.this.commonValues
										.getPageCount();
								searchPageRange(pageSize, page, page + 1);
							}
						}

					}
					else
						if (!SwingSearchWindow.this.backGroundSearch || !SwingSearchWindow.this.usingMenuBarSearch) {
							// this page to end
							searchPageRange(pageSize, 1, SwingSearchWindow.this.commonValues.getPageCount() + 1);
						}

					if (!SwingSearchWindow.this.backGroundSearch) {
						SwingSearchWindow.this.searchCount.setText(Messages.getMessage("PdfViewerSearch.ItemsFound") + ' '
								+ SwingSearchWindow.this.itemFoundCount + "  " + Messages.getMessage("PdfViewerSearch.Done"));
						SwingSearchWindow.this.searchButton.setText(Messages.getMessage("PdfViewerSearch.Button"));
					}

					SwingSearchWindow.this.resultsList.invalidate();
					SwingSearchWindow.this.resultsList.repaint();
					SwingSearchWindow.this.resultsList.setSelectedIndex(0);
					SwingSearchWindow.this.resultsList.setLength(SwingSearchWindow.this.listModel.capacity());
					SwingSearchWindow.this.currentGUI.setResults(SwingSearchWindow.this.resultsList);

					SwingSearchWindow.this.currentGUI.nextSearch.setEnabled(true);
					SwingSearchWindow.this.currentGUI.previousSearch.setEnabled(true);

					// reset search button
					SwingSearchWindow.this.isSearch = false;
					SwingSearchWindow.this.requestInterupt = false;

				}
				catch (InterruptedException ee) {

					// Exception caused so use alert user and allow search
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							SwingSearchWindow.this.requestInterupt = false;
							SwingSearchWindow.this.backGroundSearch = false;
							SwingSearchWindow.this.currentGUI.showMessageDialog("Search stopped by user.");
							if (!SwingSearchWindow.this.backGroundSearch) {
								SwingSearchWindow.this.currentGUI.nextSearch.setEnabled(true);
								SwingSearchWindow.this.currentGUI.previousSearch.setEnabled(true);
							}
						}
					});
				}
				catch (Exception e) {
					// Exception caused so use alert user and allow search
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							SwingSearchWindow.this.requestInterupt = false;
							SwingSearchWindow.this.backGroundSearch = false;
							if (Viewer.showMessages) SwingSearchWindow.this.currentGUI
									.showMessageDialog("An error occured during search. Some results may be missing.\n\nPlease send the file to IDRSolutions for investigation.");
							if (!SwingSearchWindow.this.backGroundSearch) {
								SwingSearchWindow.this.currentGUI.nextSearch.setEnabled(true);
								SwingSearchWindow.this.currentGUI.previousSearch.setEnabled(true);
							}
						}
					});

				}

				if (!Values.isProcessing()) {// {if (!event.getValueIsAdjusting()) {

					float scaling = SwingSearchWindow.this.currentGUI.getScaling();
					// int inset=currentGUI.getPDFDisplayInset();

					SwingSearchWindow.this.resultsList.setSelectedIndex(0);
					int id = SwingSearchWindow.this.resultsList.getSelectedIndex();

					SwingSearchWindow.this.decode_pdf.getTextLines().clearHighlights();
					// System.out.println("clicked pdf = "+decode_pdf.getClass().getName() + "@" + Integer.toHexString(decode_pdf.hashCode()));

					/**
					 * Sometimes the selected index is not registered by this point Set manual if this is the case
					 */
					if (id == -1 && SwingSearchWindow.this.resultsList.getResultCount() > 0) {
						id = 0;
					}

					if (id != -1) {

						Integer key = id;
						Object newPage = SwingSearchWindow.this.textPages.get(key);

						if (newPage != null) {
							int nextPage = (Integer) newPage;

							// move to new page
							if (SwingSearchWindow.this.commonValues.getCurrentPage() != nextPage) {

								SwingSearchWindow.this.commonValues.setCurrentPage(nextPage);

								SwingSearchWindow.this.currentGUI.resetStatusMessage(Messages.getMessage("PdfViewer.LoadingPage") + ' '
										+ SwingSearchWindow.this.commonValues.getCurrentPage());

								/** reset as rotation may change! */
								SwingSearchWindow.this.decode_pdf.setPageParameters(scaling, SwingSearchWindow.this.commonValues.getCurrentPage());

								// decode the page
								SwingSearchWindow.this.currentGUI.decodePage(false);

								SwingSearchWindow.this.decode_pdf.invalidate();
							}

							while (Values.isProcessing()) {
								// Ensure page has been processed else highlight may be incorrect
								try {
									Thread.sleep(500);
								}
								catch (InterruptedException e) {
									e.printStackTrace(); // To change body of catch statement use File | Settings | File Templates.
								}
							}

							SwingSearchWindow.this.firstPageWithResults = SwingSearchWindow.this.commonValues.getCurrentPage();

							/**
							 * Highlight all search results on page.
							 */

							if ((SwingSearchWindow.this.searchTypeParameters & SearchType.HIGHLIGHT_ALL_RESULTS) == SearchType.HIGHLIGHT_ALL_RESULTS) {
								Rectangle[] showAllOnPage;
								Vector_Rectangle storageVector = new Vector_Rectangle();
								int lastPage = -1;
								int currentPage = 0;
								// System.out.println("size = "+getSize());
								for (int k = 0; k != SwingSearchWindow.this.textPages.size(); k++) {
									Object page = SwingSearchWindow.this.textPages.get(k);

									if (page != null) {
										currentPage = (Integer) page;
										if (currentPage != lastPage && lastPage != -1) {
											storageVector.trim();
											showAllOnPage = storageVector.get();
											SwingSearchWindow.this.decode_pdf.getTextLines().addHighlights(showAllOnPage, true, lastPage);
											lastPage = currentPage;
											storageVector = new Vector_Rectangle();
										}

										Object highlight = SwingSearchWindow.this.textRectangles.get(k);

										if (highlight instanceof Rectangle) {
											storageVector.addElement((Rectangle) highlight);
										}
										if (highlight instanceof Rectangle[]) {
											Rectangle[] areas = (Rectangle[]) highlight;
											for (int i = 0; i != areas.length; i++) {
												storageVector.addElement(areas[i]);
											}
										}
										// decode_pdf.addToHighlightAreas(decode_pdf, storageVector, currentPage);
										// }
									}
								}
								storageVector.trim();
								showAllOnPage = storageVector.get();
								SwingSearchWindow.this.decode_pdf.getTextLines().addHighlights(showAllOnPage, true, currentPage);

							}
							else {

								Object highlight = SwingSearchWindow.this.textRectangles.get(key);

								if (highlight instanceof Rectangle) {
									SwingSearchWindow.this.currentGUI.currentCommands.scrollRectToHighlight((Rectangle) highlight,
											SwingSearchWindow.this.commonValues.getCurrentPage());

									// add text highlight
									SwingSearchWindow.this.decode_pdf.getTextLines().addHighlights(new Rectangle[] { (Rectangle) highlight }, true,
											SwingSearchWindow.this.commonValues.getCurrentPage());
								}
								if (highlight instanceof Rectangle[]) {
									SwingSearchWindow.this.currentGUI.currentCommands.scrollRectToHighlight(((Rectangle[]) highlight)[0],
											SwingSearchWindow.this.commonValues.getCurrentPage());

									// add text highlight
									SwingSearchWindow.this.decode_pdf.getTextLines().addHighlights(((Rectangle[]) highlight), true,
											SwingSearchWindow.this.commonValues.getCurrentPage());
								}

							}

							SwingSearchWindow.this.decode_pdf.invalidate();
							SwingSearchWindow.this.decode_pdf.repaint();

						}
					}
				}

				// When page changes make sure only relevant navigation buttons are displayed
				if (SwingSearchWindow.this.commonValues.getCurrentPage() == 1) SwingSearchWindow.this.currentGUI
						.setBackNavigationButtonsEnabled(false);
				else SwingSearchWindow.this.currentGUI.setBackNavigationButtonsEnabled(true);

				if (SwingSearchWindow.this.commonValues.getCurrentPage() == SwingSearchWindow.this.decode_pdf.getPageCount()) SwingSearchWindow.this.currentGUI
						.setForwardNavigationButtonsEnabled(false);
				else SwingSearchWindow.this.currentGUI.setForwardNavigationButtonsEnabled(true);

				SwingSearchWindow.this.decode_pdf.remove(SwingSearchWindow.this.progress);
				SwingSearchWindow.this.decode_pdf.validate();
				SwingSearchWindow.this.backGroundSearch = false;
				SwingSearchWindow.this.resultsList.setStatus(SearchList.SEARCH_COMPLETE_SUCCESSFULLY);
				return null;
			}
		};

		this.searcher.start();
	}

	@Override
	public int getFirstPageWithResults() {
		return this.firstPageWithResults;
	}

	private void searchPageRange(PdfPageData pageSize, int startPage, int endPage) throws Exception {

		int x1;
		int x2;
		int y1;
		int y2;// search all pages

		int listCount = 0;

		// System.out.println("Search range "+startPage+" "+endPage);

		for (int page = startPage; page < endPage && !this.requestInterupt; page++) {

			// @kieran -changed by Mark to stop thread issue
			if (Thread.interrupted()) {
				continue;
				// throw new InterruptedException();
			}

			// System.out.println("page=="+page);

			this.progress.setValue(this.progress.getValue() + 1);
			this.decode_pdf.repaint();

			/** common extraction code */
			PdfGroupingAlgorithms currentGrouping;

			/** create a grouping object to apply grouping to data */
			try {
				if (page == this.commonValues.getCurrentPage()) currentGrouping = this.decode_pdf.getGroupingObject();
				else {
					this.decode_pdf.decodePageInBackground(page);
					currentGrouping = this.decode_pdf.getBackgroundGroupingObject();
				}

				// tell JPedal we want teasers
				currentGrouping.generateTeasers();

				// allow us to add options
				currentGrouping.setIncludeHTML(true);

				// set size
				x1 = pageSize.getCropBoxX(page);
				x2 = pageSize.getCropBoxWidth(page);
				y1 = pageSize.getCropBoxY(page);
				y2 = pageSize.getCropBoxHeight(page);

				final SearchListener listener = new DefaultSearchListener();

				SortedMap highlightsWithTeasers = currentGrouping.findMultipleTermsInRectangleWithMatchingTeasers(x1, y1, x2, y2,
						pageSize.getRotation(page), page, this.searchTerms, this.searchTypeParameters, listener);

				// changed by MArk
				if (Thread.interrupted()) {
					continue;
					// throw new InterruptedException();
				}

				/**
				 * update data structures with results from this page
				 */
				if (!highlightsWithTeasers.isEmpty()) {

					// @kieran
					// switch on buttons as soon as search produces valid results
					if (!this.backGroundSearch) {
						this.currentGUI.nextSearch.setEnabled(true);
						this.currentGUI.previousSearch.setEnabled(true);

					}
					// update count display
					this.itemFoundCount = this.itemFoundCount + highlightsWithTeasers.size();

					for (Object o : highlightsWithTeasers.entrySet()) {
						Map.Entry e = (Map.Entry) o;

						/* highlight is a rectangle or a rectangle[] */
						Object highlight = e.getKey();

						final String teaser = (String) e.getValue();

						if (!SwingUtilities.isEventDispatchThread()) {
							Runnable setTextRun = new Runnable() {
								@Override
								public void run() {

									// if highights ensure displayed by wrapping in tags
									if (!teaser.contains("<b>")) SwingSearchWindow.this.listModel.addElement(teaser);
									else SwingSearchWindow.this.listModel.addElement("<html>" + teaser + "</html>");
								}
							};
							SwingUtilities.invokeLater(setTextRun);
						}
						else {
							if (!teaser.contains("<b>")) this.listModel.addElement(teaser);
							else this.listModel.addElement("<html>" + teaser + "</html>");
						}

						Integer key = listCount;
						listCount++;
						this.textRectangles.put(key, highlight);
						this.textPages.put(key, page);
					}

				}

				// new value or 16 pages elapsed
				if (!this.backGroundSearch && (!highlightsWithTeasers.isEmpty()) | ((page % 16) == 0)) {
					this.searchCount.setText(Messages.getMessage("PdfViewerSearch.ItemsFound") + ' ' + this.itemFoundCount + ' '
							+ Messages.getMessage("PdfViewerSearch.Scanning") + page);
					this.searchCount.invalidate();
					this.searchCount.repaint();
				}
			}
			catch (PdfException e1) {
				this.backGroundSearch = false;
				this.requestInterupt = false;
			}
			if (this.requestInterupt) {
				this.currentGUI.showMessageDialog("Search stopped by user.");
			}
			this.lastPage = page;
		}
	}

	public int getListLength() {
		return this.listModel.capacity();
	}

	@Override
	public void grabFocusInInput() {
		this.searchText.grabFocus();
	}

	@Override
	public boolean isSearchVisible() {
		return this.isVisible();
	}

	@Override
	public void setStyle(int style) {
		this.style = style;
	}

	@Override
	public int getStyle() {
		return this.style;
	}

	public JTextField getSearchText() {
		return this.searchText;
	}

	public void setSearchText(String s) {
		this.deleteOnClick = false;
		this.searchText.setText(s);
	}

	@Override
	public Map getTextRectangles() {
		return this.textRectangles;
	}

	@Override
	public SearchList getResults() {
		return this.resultsList;
	}

	@Override
	public SearchList getResults(int page) {

		if (this.usingMenuBarSearch && page != this.lastPage && this.style == SEARCH_MENU_BAR) {

			// if(listModel == null)
			this.listModel = new DefaultListModel();

			this.textPages.clear();
			this.textRectangles.clear();
			// if(resultsList==null)
			this.resultsList = new SearchList(this.listModel, this.textPages, this.textRectangles);

			this.resultsList.setStatus(SearchList.SEARCH_INCOMPLETE);

			try {
				searchPageRange(this.decode_pdf.getPdfPageData(), page, page + 1);
			}
			catch (Exception e) {
				e.printStackTrace();
			}

			this.lastPage = page;
		}

		return this.resultsList;
	}

	/**
	 * Reset search text and menu bar buttons when opening new page
	 */
	@Override
	public void resetSearchWindow() {
		if (this.isSetup) {

			this.searchText.setText(this.defaultMessage);
			this.deleteOnClick = true;

			if (this.hasSearched) {
				// resultsList = null;
				this.currentGUI.nextSearch.setVisible(false);
				this.currentGUI.previousSearch.setVisible(false);
				this.hasSearched = false;
			}
			this.currentGUI.getPdfDecoder().requestFocusInWindow();
			// isSetup = false;
		}
	}
}
