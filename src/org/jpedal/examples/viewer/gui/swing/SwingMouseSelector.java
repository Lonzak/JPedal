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
 * SwingMouseSelector.java
 * ---------------
 */

package org.jpedal.examples.viewer.gui.swing;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Date;

import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.RepaintManager;
import javax.swing.filechooser.FileFilter;

import org.jpedal.Display;
import org.jpedal.PdfDecoder;
import org.jpedal.SingleDisplay;
import org.jpedal.examples.viewer.Commands;
import org.jpedal.examples.viewer.Values;
import org.jpedal.examples.viewer.Viewer;
import org.jpedal.examples.viewer.gui.GUI;
import org.jpedal.examples.viewer.gui.SwingGUI;
import org.jpedal.exception.PdfException;
import org.jpedal.external.Options;
import org.jpedal.grouping.SearchType;
import org.jpedal.parser.DecoderOptions;
import org.jpedal.render.DynamicVectorRenderer;
import org.jpedal.utils.Messages;

public class SwingMouseSelector implements SwingMouseFunctionality {

	private PdfDecoder decode_pdf;
	private SwingGUI currentGUI;
	private Values commonValues;
	private Commands currentCommands;

	// Experimental multi page highlight flag
	public static boolean activateMultipageHighlight = true;

	// Variables to keep track of multiple clicks
	private int clickCount = 0;
	private long lastTime = -1;

	// Page currently under the mouse
	private int pageMouseIsOver = -1;

	// Page currently being highlighted
	private int pageOfHighlight = -1;

	// Find current highlighted page
	private boolean startHighlighting = false;

	/*
	 * ID of objects found during selection
	 */
	public int id = -1;
	public int lastId = -1;

	// used to track changes when dragging rectangle around
	private int old_m_x2 = -1, old_m_y2 = -1;

	// Use alt to extract only within exact area
	boolean altIsDown = false;

	private JPopupMenu rightClick = new JPopupMenu();
	private boolean menuCreated = false;

	// Right click options
	JMenuItem copy;
	// ======================================
	JMenuItem selectAll, deselectall;
	// ======================================
	JMenu extract;
	JMenuItem extractText, extractImage;
	ImageIcon snapshotIcon;
	JMenuItem snapShot;
	// ======================================
	JMenuItem find;
	// ======================================
	JMenuItem speakHighlighted;

	public SwingMouseSelector(PdfDecoder decode_pdf, SwingGUI currentGUI, Values commonValues, Commands currentCommands) {

		this.decode_pdf = decode_pdf;
		this.currentGUI = currentGUI;
		this.commonValues = commonValues;
		this.currentCommands = currentCommands;

		// decode_pdf.addExternalHandler(this, Options.SwingMouseHandler);
	}

	public void updateRectangle() {
		// TODO Auto-generated method stub
	}

	/**
	 * Mouse Button Listener
	 */
	@Override
	public void mouseClicked(MouseEvent event) {

		if (this.decode_pdf.getDisplayView() == Display.SINGLE_PAGE || activateMultipageHighlight) {
			long currentTime = new Date().getTime();

			if (this.lastTime + 500 < currentTime) this.clickCount = 0;

			this.lastTime = currentTime;

			if (event.getButton() == MouseEvent.BUTTON1 || event.getButton() == MouseEvent.NOBUTTON) {
				// Single mode actions
				if (this.clickCount != 4) this.clickCount++;

				// highlight image on page if over
				// int[] c = smh.getCursorLocation();
				float scaling = this.currentGUI.getScaling();
				int inset = GUI.getPDFDisplayInset();
				int mouseX = (int) ((this.currentGUI.AdjustForAlignment(event.getX()) - inset) / scaling);
				int mouseY = (int) (this.decode_pdf.getPdfPageData().getCropBoxHeight(this.commonValues.getCurrentPage()) - ((event.getY() - inset) / scaling));

				Point mousePoint = getCoordsOnPage(event.getX(), event.getY(), this.commonValues.getCurrentPage());
				mouseX = (int) mousePoint.getX();
				mouseY = (int) mousePoint.getY();

				if (this.decode_pdf.getDisplayView() == Display.SINGLE_PAGE) this.id = this.decode_pdf.getDynamicRenderer().isInsideImage(mouseX,
						mouseY);
				else this.id = -1;

				if (this.lastId != this.id && this.id != -1) {
					Rectangle imageArea = this.decode_pdf.getDynamicRenderer().getArea(this.id);

					if (imageArea != null) {
						int h = imageArea.height;
						int w = imageArea.width;

						int x = imageArea.x;
						int y = imageArea.y;
						this.decode_pdf.getDynamicRenderer().setneedsHorizontalInvert(false);
						this.decode_pdf.getDynamicRenderer().setneedsVerticalInvert(false);
						// Check for negative values
						if (w < 0) {
							this.decode_pdf.getDynamicRenderer().setneedsHorizontalInvert(true);
							w = -w;
							x = x - w;
						}
						if (h < 0) {
							this.decode_pdf.getDynamicRenderer().setneedsVerticalInvert(true);
							h = -h;
							y = y - h;
						}

						if (this.currentGUI.currentCommands.isImageExtractionAllowed()) {
							this.currentCommands.pages.setHighlightedImage(new int[] { x, y, w, h });
						}

					}
					this.lastId = this.id;
				}
				else {
					if (this.currentGUI.currentCommands.isImageExtractionAllowed()) {
						this.currentCommands.pages.setHighlightedImage(null);
					}
					this.lastId = -1;
				}

				if (this.id == -1) {
					if (this.clickCount > 1) {
						switch (this.clickCount) {
							case 1: // single click adds caret to page
								/**
								 * Does nothing yet. IF above prevents this case from ever happening Add Caret code here and add shift click code for
								 * selection. Also remember to comment out "if(clickCount>1)" from around this switch to activate
								 */
								break;
							case 2: // double click selects line
								Rectangle[] lines = this.decode_pdf.getTextLines().getLineAreas(this.pageMouseIsOver);
								Rectangle point = new Rectangle(mouseX, mouseY, 1, 1);

								if (lines != null) { // Null is page has no lines
									for (int i = 0; i != lines.length; i++) {
										if (lines[i].intersects(point)) {
											this.decode_pdf.updateCursorBoxOnScreen(lines[i], DecoderOptions.highlightColor);
											this.decode_pdf.getTextLines().addHighlights(new Rectangle[] { lines[i] }, false, this.pageMouseIsOver);
											// decode_pdf.setMouseHighlightArea(lines[i]);
										}
									}
								}
								break;
							case 3: // triple click selects paragraph
								Rectangle para = this.decode_pdf.getTextLines().setFoundParagraph(mouseX, mouseY, this.pageMouseIsOver);
								if (para != null) {
									this.decode_pdf.updateCursorBoxOnScreen(para, DecoderOptions.highlightColor);
									// decode_pdf.repaint();
									// decode_pdf.setMouseHighlightArea(para);
								}
								break;
							case 4: // quad click selects page
								this.currentGUI.currentCommands.executeCommand(Commands.SELECTALL, null);
								break;
						}
					}
				}
			}
			else
				if (event.getButton() == MouseEvent.BUTTON2) {

				}
				else
					if (event.getButton() == MouseEvent.BUTTON3) {

					}
		}
	}

	@Override
	public void mousePressed(MouseEvent event) {

		if (this.decode_pdf.getDisplayView() == Display.SINGLE_PAGE || activateMultipageHighlight) {
			if (event.getButton() == MouseEvent.BUTTON1 || event.getButton() == MouseEvent.NOBUTTON) {
				/** remove any outline and reset variables used to track change */

				this.decode_pdf.updateCursorBoxOnScreen(null, null); // remove box
				this.currentCommands.pages.setHighlightedImage(null);// remove image highlight
				this.decode_pdf.getTextLines().clearHighlights();

				// Remove focus from form is if anywhere on pdf panel is clicked / mouse dragged
				this.decode_pdf.grabFocus();

				// int[] values = updateXY(event.getX(), event.getY());
				Point values = getCoordsOnPage(event.getX(), event.getY(), this.commonValues.getCurrentPage());
				this.commonValues.m_x1 = (int) values.getX();
				this.commonValues.m_y1 = (int) values.getY();

			}
		}
	}

	@Override
	public void mouseReleased(MouseEvent event) {
		if (this.decode_pdf.getDisplayView() == Display.SINGLE_PAGE || activateMultipageHighlight) {
			if (event.getButton() == MouseEvent.BUTTON1 || event.getButton() == MouseEvent.NOBUTTON) {

				// If we have been highlighting, stop now and reset all flags
				if (this.startHighlighting) {
					this.startHighlighting = false;
					// pageOfHighlight = -1;
				}

				repaintArea(new Rectangle(this.commonValues.m_x1 - this.currentGUI.cropX, this.commonValues.m_y2 + this.currentGUI.cropY,
						this.commonValues.m_x2 - this.commonValues.m_x1 + this.currentGUI.cropX, (this.commonValues.m_y1 - this.commonValues.m_y2)
								+ this.currentGUI.cropY), this.currentGUI.mediaH);// redraw
				this.decode_pdf.repaint();

				if (this.currentCommands.extractingAsImage) {

					/** remove any outline and reset variables used to track change */
					this.decode_pdf.updateCursorBoxOnScreen(null, null); // remove box
					this.decode_pdf.getTextLines().clearHighlights(); // remove highlighted text
					this.currentCommands.pages.setHighlightedImage(null);// remove image highlight

					this.decode_pdf.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

					this.currentGUI.currentCommands.extractSelectedScreenAsImage();
					this.currentCommands.extractingAsImage = false;
					PdfDecoder.showMouseBox = false;

				}

				// Ensure this is reset to -1 regardless
				this.pageOfHighlight = -1;

			}
			else
				if (event.getButton() == MouseEvent.BUTTON3) {
					if (this.currentGUI.getProperties().getValue("allowRightClick").toLowerCase().equals("true")) {
						if (!this.menuCreated) createRightClickMenu();

						if (this.currentCommands.pages.getHighlightedImage() == null) this.extractImage.setEnabled(false);
						else this.extractImage.setEnabled(true);

						if (this.decode_pdf.getTextLines().getHighlightedAreas(this.commonValues.getCurrentPage()) == null) {
							this.extractText.setEnabled(false);
							this.find.setEnabled(false);
							this.speakHighlighted.setEnabled(false);
							this.copy.setEnabled(false);
						}
						else {
							this.extractText.setEnabled(true);
							this.find.setEnabled(true);
							this.speakHighlighted.setEnabled(true);
							this.copy.setEnabled(true);
						}

						// <start-wrap>
						if (this.decode_pdf != null && this.decode_pdf.isOpen()) this.rightClick.show(this.decode_pdf, event.getX(), event.getY());
						// <end-wrap>
					}
				}
		}
	}

	/**
	 * Mouse Motion Listener
	 */
	@Override
	public void mouseEntered(MouseEvent arg0) {
	}

	@Override
	public void mouseExited(MouseEvent arg0) {
	}

	@Override
	public void mouseDragged(MouseEvent event) {

		if (event.getButton() == MouseEvent.BUTTON1 || event.getButton() == MouseEvent.NOBUTTON) {

			this.altIsDown = event.isAltDown();
			if (!this.startHighlighting) this.startHighlighting = true;

			Point values = getCoordsOnPage(event.getX(), event.getY(), this.commonValues.getCurrentPage());

			if (this.pageMouseIsOver == this.pageOfHighlight) {
				this.commonValues.m_x2 = (int) values.getX();
				this.commonValues.m_y2 = (int) values.getY();
			}

			if (this.commonValues.isPDF()) generateNewCursorBox();

		}
	}

	@Override
	public void mouseMoved(MouseEvent event) {

		// Update cursor for this position
		// int[] values = updateXY(event.getX(), event.getY());
		// int x =values[0];
		// int y =values[1];
		// decode_pdf.getObjectUnderneath(x, y);
	}

	/**
	 * get raw co-ords and convert to correct scaled units
	 * 
	 * @return int[] of size 2, [0]=new x value, [1] = new y value
	 */
	protected int[] updateXY(int originalX, int originalY) {

		float scaling = this.currentGUI.getScaling();
		int inset = GUI.getPDFDisplayInset();
		int rotation = this.currentGUI.getRotation();

		// get co-ordinates of top point of outine rectangle
		int x = (int) (((this.currentGUI.AdjustForAlignment(originalX)) - inset) / scaling);
		int y = (int) ((originalY - inset) / scaling);

		// undo any viewport scaling
		if (this.commonValues.maxViewY != 0) { // will not be zero if viewport in play
			x = (int) (((x - (this.commonValues.dx * scaling)) / this.commonValues.viewportScale));
			y = (int) ((this.currentGUI.mediaH - ((this.currentGUI.mediaH - (y / scaling) - this.commonValues.dy) / this.commonValues.viewportScale)) * scaling);
		}

		int[] ret = new int[2];
		if (rotation == 90) {
			ret[1] = x + this.currentGUI.cropY;
			ret[0] = y + this.currentGUI.cropX;
		}
		else
			if ((rotation == 180)) {
				ret[0] = this.currentGUI.mediaW - (x + this.currentGUI.mediaW - this.currentGUI.cropW - this.currentGUI.cropX);
				ret[1] = y + this.currentGUI.cropY;
			}
			else
				if ((rotation == 270)) {
					ret[1] = this.currentGUI.mediaH - (x + this.currentGUI.mediaH - this.currentGUI.cropH - this.currentGUI.cropY);
					ret[0] = this.currentGUI.mediaW - (y + this.currentGUI.mediaW - this.currentGUI.cropW - this.currentGUI.cropX);
				}
				else {
					ret[0] = x + this.currentGUI.cropX;
					ret[1] = this.currentGUI.mediaH - (y + this.currentGUI.mediaH - this.currentGUI.cropH - this.currentGUI.cropY);
				}
		return ret;
	}

	/**
	 * Create right click menu if does not exist
	 */
	private void createRightClickMenu() {

		this.copy = new JMenuItem(Messages.getMessage("PdfRightClick.copy"));
		this.selectAll = new JMenuItem(Messages.getMessage("PdfRightClick.selectAll"));
		this.deselectall = new JMenuItem(Messages.getMessage("PdfRightClick.deselectAll"));
		this.extract = new JMenu(Messages.getMessage("PdfRightClick.extract"));
		this.extractText = new JMenuItem(Messages.getMessage("PdfRightClick.extractText"));
		this.extractImage = new JMenuItem(Messages.getMessage("PdfRightClick.extractImage"));
		this.snapshotIcon = new ImageIcon(getClass().getResource("/org/jpedal/examples/viewer/res/snapshot_menu.gif"));
		this.snapShot = new JMenuItem(Messages.getMessage("PdfRightClick.snapshot"), this.snapshotIcon);
		this.find = new JMenuItem(Messages.getMessage("PdfRightClick.find"));
		this.speakHighlighted = new JMenuItem("Speak Highlighted text");

		this.rightClick.add(this.copy);
		this.copy.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (SwingMouseSelector.this.decode_pdf.getDisplayView() == Display.SINGLE_PAGE) SwingMouseSelector.this.currentGUI.currentCommands
						.executeCommand(Commands.COPY, null);
				else {
					if (Viewer.showMessages) JOptionPane.showMessageDialog(SwingMouseSelector.this.currentGUI.getFrame(),
							"Copy is only avalible in single page display mode");
				}
			}
		});

		this.rightClick.addSeparator();

		this.rightClick.add(this.selectAll);
		this.selectAll.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				SwingMouseSelector.this.currentGUI.currentCommands.executeCommand(Commands.SELECTALL, null);
			}
		});

		this.rightClick.add(this.deselectall);
		this.deselectall.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				SwingMouseSelector.this.currentGUI.currentCommands.executeCommand(Commands.DESELECTALL, null);
			}
		});

		this.rightClick.addSeparator();

		this.rightClick.add(this.extract);

		this.extract.add(this.extractText);
		this.extractText.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (SwingMouseSelector.this.decode_pdf.getDisplayView() == Display.SINGLE_PAGE) SwingMouseSelector.this.currentGUI.currentCommands
						.extractSelectedText();
				else {
					if (Viewer.showMessages) JOptionPane.showMessageDialog(SwingMouseSelector.this.currentGUI.getFrame(),
							"Text Extraction is only avalible in single page display mode");
				}
			}
		});

		this.extract.add(this.extractImage);
		this.extractImage.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (SwingMouseSelector.this.currentCommands.pages.getHighlightedImage() == null) {
					if (Viewer.showMessages) JOptionPane.showMessageDialog(SwingMouseSelector.this.decode_pdf,
							"No image has been selected for extraction.", "No image selected", JOptionPane.ERROR_MESSAGE);
				}
				else {
					if (SwingMouseSelector.this.decode_pdf.getDisplayView() == 1) {
						JFileChooser jf = new JFileChooser();
						FileFilter ff1 = new FileFilter() {
							@Override
							public boolean accept(File f) {
								return f.isDirectory() || f.getName().toLowerCase().endsWith(".jpg") || f.getName().toLowerCase().endsWith(".jpeg");
							}

							@Override
							public String getDescription() {
								return "JPG (*.jpg)";
							}
						};
						FileFilter ff2 = new FileFilter() {
							@Override
							public boolean accept(File f) {
								return f.isDirectory() || f.getName().toLowerCase().endsWith(".png");
							}

							@Override
							public String getDescription() {
								return "PNG (*.png)";
							}
						};
						FileFilter ff3 = new FileFilter() {
							@Override
							public boolean accept(File f) {
								return f.isDirectory() || f.getName().toLowerCase().endsWith(".tif") || f.getName().toLowerCase().endsWith(".tiff");
							}

							@Override
							public String getDescription() {
								return "TIF (*.tiff)";
							}
						};
						jf.addChoosableFileFilter(ff3);
						jf.addChoosableFileFilter(ff2);
						jf.addChoosableFileFilter(ff1);
						jf.showSaveDialog(null);

						File f = jf.getSelectedFile();
						boolean failed = false;
						if (f != null) {
							String filename = f.getAbsolutePath();
							String type = jf.getFileFilter().getDescription().substring(0, 3).toLowerCase();

							// Check to see if user has entered extension if so ignore filter
							if (filename.indexOf('.') != -1) {
								String testExt = filename.substring(filename.indexOf('.') + 1).toLowerCase();
								if (testExt.equals("jpg") || testExt.equals("jpeg")) type = "jpg";
								else
									if (testExt.equals("png")) type = "png";
									else
										// *.tiff files using JAI require *.TIFF
										if (testExt.equals("tif") || testExt.equals("tiff")) type = "tiff";
										else {
											// Unsupported file format
											if (Viewer.showMessages) JOptionPane.showMessageDialog(null,
													"Sorry, we can not currently save images to ." + testExt + " files.");
											failed = true;
										}
							}

							// JAI requires *.tiff instead of *.tif
							if (type.equals("tif")) type = "tiff";

							// Image saved in All files filter, default to .png
							if (type.equals("all")) type = "png";

							// If no extension at end of name, added one
							if (!filename.toLowerCase().endsWith('.' + type)) filename = filename + '.' + (type);

							// If valid extension was choosen
							if (!failed) SwingMouseSelector.this.decode_pdf.getDynamicRenderer()
									.saveImage(SwingMouseSelector.this.id, filename, type);
						}
					}
				}
			}
		});

		this.extract.add(this.snapShot);
		this.snapShot.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				SwingMouseSelector.this.currentGUI.currentCommands.executeCommand(Commands.SNAPSHOT, null);
			}
		});

		this.rightClick.addSeparator();

		this.rightClick.add(this.find);
		this.find.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {

				/** ensure co-ords in right order */
				Rectangle coords = SwingMouseSelector.this.decode_pdf.getCursorBoxOnScreen();
				if (coords == null) {
					if (Viewer.showMessages) JOptionPane.showMessageDialog(SwingMouseSelector.this.decode_pdf,
							"There is no text selected.\nPlease highlight the text you wish to search.", "No Text selected",
							JOptionPane.ERROR_MESSAGE);
					return;
				}

				String textToFind = SwingMouseSelector.this.currentGUI.showInputDialog(Messages.getMessage("PdfViewerMessage.GetUserInput"));

				// if cancel return to menu.
				if (textToFind == null || textToFind.length() < 1) {
					return;
				}

				int t_x1 = coords.x;
				int t_x2 = coords.x + coords.width;
				int t_y1 = coords.y;
				int t_y2 = coords.y + coords.height;

				if (t_y1 < t_y2) {
					int temp = t_y2;
					t_y2 = t_y1;
					t_y1 = temp;
				}

				if (t_x1 > t_x2) {
					int temp = t_x2;
					t_x2 = t_x1;
					t_x1 = temp;
				}

				if (t_x1 < SwingMouseSelector.this.currentGUI.cropX) t_x1 = SwingMouseSelector.this.currentGUI.cropX;
				if (t_x1 > SwingMouseSelector.this.currentGUI.mediaW - SwingMouseSelector.this.currentGUI.cropX) t_x1 = SwingMouseSelector.this.currentGUI.mediaW
						- SwingMouseSelector.this.currentGUI.cropX;

				if (t_x2 < SwingMouseSelector.this.currentGUI.cropX) t_x2 = SwingMouseSelector.this.currentGUI.cropX;
				if (t_x2 > SwingMouseSelector.this.currentGUI.mediaW - SwingMouseSelector.this.currentGUI.cropX) t_x2 = SwingMouseSelector.this.currentGUI.mediaW
						- SwingMouseSelector.this.currentGUI.cropX;

				if (t_y1 < SwingMouseSelector.this.currentGUI.cropY) t_y1 = SwingMouseSelector.this.currentGUI.cropY;
				if (t_y1 > SwingMouseSelector.this.currentGUI.mediaH - SwingMouseSelector.this.currentGUI.cropY) t_y1 = SwingMouseSelector.this.currentGUI.mediaH
						- SwingMouseSelector.this.currentGUI.cropY;

				if (t_y2 < SwingMouseSelector.this.currentGUI.cropY) t_y2 = SwingMouseSelector.this.currentGUI.cropY;
				if (t_y2 > SwingMouseSelector.this.currentGUI.mediaH - SwingMouseSelector.this.currentGUI.cropY) t_y2 = SwingMouseSelector.this.currentGUI.mediaH
						- SwingMouseSelector.this.currentGUI.cropY;

				// <start-demo>
				/**
				 * <end-demo> if(Viewer.showMessages)
				 * JOptionPane.showMessageDialog(currentGUI.getFrame(),Messages.getMessage("PdfViewerMessage.FindDemo")); textToFind=null; /
				 **/

				int searchType = SearchType.DEFAULT;

				int caseSensitiveOption = SwingMouseSelector.this.currentGUI.showConfirmDialog(Messages.getMessage("PdfViewercase.message"), null,
						JOptionPane.YES_NO_OPTION);

				if (caseSensitiveOption == JOptionPane.YES_OPTION) searchType |= SearchType.CASE_SENSITIVE;

				int findAllOption = SwingMouseSelector.this.currentGUI.showConfirmDialog(Messages.getMessage("PdfViewerfindAll.message"), null,
						JOptionPane.YES_NO_OPTION);

				if (findAllOption == JOptionPane.NO_OPTION) searchType |= SearchType.FIND_FIRST_OCCURANCE_ONLY;

				int hyphenOption = SwingMouseSelector.this.currentGUI.showConfirmDialog(Messages.getMessage("PdfViewerfindHyphen.message"), null,
						JOptionPane.YES_NO_OPTION);

				if (hyphenOption == JOptionPane.YES_OPTION) searchType |= SearchType.MUTLI_LINE_RESULTS;

				if (textToFind != null) {
					try {
						float[] co_ords;

						// if((searchType & SearchType.MUTLI_LINE_RESULTS)==SearchType.MUTLI_LINE_RESULTS)
						// co_ords =
						// decode_pdf.getGroupingObject().findTextInRectangleAcrossLines(t_x1,t_y1,t_x2,t_y2,commonValues.getCurrentPage(),textToFind,searchType);
						// else
						// co_ords =
						// decode_pdf.getGroupingObject().findTextInRectangle(t_x1,t_y1,t_x2,t_y2,commonValues.getCurrentPage(),textToFind,searchType);

						co_ords = SwingMouseSelector.this.decode_pdf.getGroupingObject().findText(
								new Rectangle(t_x1, t_y1, t_x2 - t_x1, t_y2 - t_y1), SwingMouseSelector.this.commonValues.getCurrentPage(),
								new String[] { textToFind }, searchType);

						if (co_ords != null) {
							if (co_ords.length < 3) SwingMouseSelector.this.currentGUI.showMessageDialog(Messages
									.getMessage("PdfViewerMessage.Found") + ' ' + co_ords[0] + ',' + co_ords[1]);
							else {
								StringBuilder displayCoords = new StringBuilder();
								String coordsMessage = Messages.getMessage("PdfViewerMessage.FoundAt");
								for (int i = 0; i < co_ords.length; i = i + 5) {
									displayCoords.append(coordsMessage).append(' ');
									displayCoords.append(co_ords[i]);
									displayCoords.append(',');
									displayCoords.append(co_ords[i + 1]);

									// //Other two coords of text
									// displayCoords.append(',');
									// displayCoords.append(co_ords[i+2]);
									// displayCoords.append(',');
									// displayCoords.append(co_ords[i+3]);

									displayCoords.append('\n');
									if (co_ords[i + 4] == -101) {
										coordsMessage = Messages.getMessage("PdfViewerMessage.FoundAtHyphen");
									}
									else {
										coordsMessage = Messages.getMessage("PdfViewerMessage.FoundAt");
									}

								}
								SwingMouseSelector.this.currentGUI.showMessageDialog(displayCoords.toString());
							}
						}
						else SwingMouseSelector.this.currentGUI.showMessageDialog(Messages.getMessage("PdfViewerMessage.NotFound"));

					}
					catch (PdfException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}

				}
			}

		});

		this.menuCreated = true;
		this.decode_pdf.add(this.rightClick);
	}

	/**
	 * generate new cursorBox and highlight extractable text, if hardware acceleration off and extraction on<br>
	 * and update current cursor box displayed on screen
	 */
	protected void generateNewCursorBox() {

		// redraw rectangle of dragged box onscreen if it has changed significantly
		if ((this.old_m_x2 != -1) | (this.old_m_y2 != -1) | (Math.abs(this.commonValues.m_x2 - this.old_m_x2) > 5)
				| (Math.abs(this.commonValues.m_y2 - this.old_m_y2) > 5)) {

			// allow for user to go up
			int top_x = this.commonValues.m_x1;
			if (this.commonValues.m_x1 > this.commonValues.m_x2) top_x = this.commonValues.m_x2;
			int top_y = this.commonValues.m_y1;
			if (this.commonValues.m_y1 > this.commonValues.m_y2) top_y = this.commonValues.m_y2;
			int w = Math.abs(this.commonValues.m_x2 - this.commonValues.m_x1);
			int h = Math.abs(this.commonValues.m_y2 - this.commonValues.m_y1);

			// add an outline rectangle to the display
			Rectangle currentRectangle = new Rectangle(top_x, top_y, w, h);

			// tell JPedal to highlight text in this area (you can add other areas to array)
			this.decode_pdf.updateCursorBoxOnScreen(currentRectangle, DecoderOptions.highlightColor);
			if (!this.currentCommands.extractingAsImage) {
				int type = this.decode_pdf.getDynamicRenderer().getObjectUnderneath(this.commonValues.m_x1, this.commonValues.m_y1);

				if ((this.altIsDown && (type != DynamicVectorRenderer.TEXT && type != DynamicVectorRenderer.TRUETYPE
						&& type != DynamicVectorRenderer.TYPE1C && type != DynamicVectorRenderer.TYPE3))) {

					// Highlight all within the rectangle
					this.decode_pdf.getTextLines().addHighlights(new Rectangle[] { currentRectangle }, true, this.pageOfHighlight);
				}
				else { // Find start and end locations and highlight all object in order in between
					Rectangle r = new Rectangle(this.commonValues.m_x1, this.commonValues.m_y1, this.commonValues.m_x2 - this.commonValues.m_x1,
							this.commonValues.m_y2 - this.commonValues.m_y1);

					this.decode_pdf.getTextLines().addHighlights(new Rectangle[] { r }, false, this.pageOfHighlight);

				}
			}
			// reset tracking
			this.old_m_x2 = this.commonValues.m_x2;
			this.old_m_y2 = this.commonValues.m_y2;

		}

		((SingleDisplay) this.decode_pdf.getExternalHandler(Options.Display)).refreshDisplay();
		this.decode_pdf.repaint();
	}

	private Point getPageCoordsInSingleDisplayMode(int x, int y, int page) {
		// <start-adobe>
		if (this.currentGUI.useNewLayout) {

			int[] flag = new int[2];

			flag[0] = SwingGUI.CURSOR;
			flag[1] = 0;

			int pageWidth, pageHeight;
			if (this.currentGUI.getRotation() % 180 == 90) {
				pageWidth = this.decode_pdf.getPdfPageData().getScaledCropBoxHeight(page);
				pageHeight = this.decode_pdf.getPdfPageData().getScaledCropBoxWidth(page);
			}
			else {
				pageWidth = this.decode_pdf.getPdfPageData().getScaledCropBoxWidth(page);
				pageHeight = this.decode_pdf.getPdfPageData().getScaledCropBoxHeight(page);
			}

			Rectangle pageArea = new Rectangle((this.decode_pdf.getWidth() / 2) - (pageWidth / 2), this.decode_pdf.getInsetH(), pageWidth, pageHeight);

			if (pageArea.contains(x, y))
			// set displayed
			flag[1] = 1;
			else
			// set not displayed
			flag[1] = 0;

			// Set highlighting page
			if (this.pageOfHighlight == -1 && this.startHighlighting) {
				this.pageOfHighlight = page;
			}

			// Keep track of page the mouse is over at all times
			this.pageMouseIsOver = page;

			this.currentGUI.setMultibox(flag);

		}

		// <end-adobe>

		float scaling = this.currentGUI.getScaling();
		int inset = GUI.getPDFDisplayInset();
		int rotation = this.currentGUI.getRotation();

		// Apply inset to values
		int ex = this.currentGUI.AdjustForAlignment(x) - inset;
		int ey = y - inset;

		// undo any viewport scaling
		if (this.commonValues.maxViewY != 0) { // will not be zero if viewport in play
			ex = (int) (((ex - (this.commonValues.dx * scaling)) / this.commonValues.viewportScale));
			ey = (int) ((this.currentGUI.mediaH - ((this.currentGUI.mediaH - (ey / scaling) - this.commonValues.dy) / this.commonValues.viewportScale)) * scaling);
		}

		// Apply page scale to value
		x = (int) ((ex) / scaling);
		y = (int) ((ey / scaling));

		// Apply rotation to values
		if (rotation == 90) {
			int tmp = (x + this.currentGUI.cropY);
			x = (y + this.currentGUI.cropX);
			y = tmp;
		}
		else
			if ((rotation == 180)) {
				x = (this.currentGUI.cropW + this.currentGUI.cropX) - x;
				y = (y + this.currentGUI.cropY);
			}
			else
				if ((rotation == 270)) {
					int tmp = (this.currentGUI.cropH + this.currentGUI.cropY) - x;
					x = (this.currentGUI.cropW + this.currentGUI.cropX) - y;
					y = tmp;
				}
				else {
					x = (x + this.currentGUI.cropX);
					if (this.decode_pdf.getDisplayView() == Display.SINGLE_PAGE) y = (this.currentGUI.cropH + this.currentGUI.cropY) - y;
					else y = (this.currentGUI.cropY) + y;
				}

		return new Point(x, y);
	}

	private Point getPageCoordsInContinuousDisplayMode(int x, int y, int page) {

		Display pages = (SingleDisplay) this.decode_pdf.getExternalHandler(Options.Display);

		// <start-adobe>
		if (this.currentGUI.useNewLayout) {
			int[] flag = new int[2];

			flag[0] = SwingGUI.CURSOR;
			flag[1] = 0;

			// In continuous pages are centred so we need make
			int xAdjustment = (this.decode_pdf.getWidth() / 2) - (this.decode_pdf.getPdfPageData().getScaledCropBoxWidth(page) / 2);
			if (xAdjustment < 0) xAdjustment = 0;
			else {
				// This adjustment is the correct position.
				// Offset removed to that when used later we get either offset unaltered or correct position
				xAdjustment = xAdjustment - pages.getXCordForPage(page);
			}
			Rectangle pageArea = new Rectangle(pages.getXCordForPage(page) + xAdjustment, pages.getYCordForPage(page), this.decode_pdf
					.getPdfPageData().getScaledCropBoxWidth(page), this.decode_pdf.getPdfPageData().getScaledCropBoxHeight(page));
			if (pageArea.contains(x, y)) {
				// set displayed
				flag[1] = 1;
			}

			if (flag[1] == 0) {
				if (y < pageArea.y && page > 1) {
					while (flag[1] == 0 && page > 1) {
						page--;
						pageArea = new Rectangle(pages.getXCordForPage(page) + xAdjustment, pages.getYCordForPage(page), this.decode_pdf
								.getPdfPageData().getScaledCropBoxWidth(page), this.decode_pdf.getPdfPageData().getScaledCropBoxHeight(page));
						if (pageArea.contains(x, y)) {
							// set displayed
							flag[1] = 1;
						}
					}
				}
				else {
					if (y > pageArea.getMaxY() && page < this.commonValues.getPageCount()) {
						while (flag[1] == 0 && page < this.commonValues.getPageCount()) {
							page++;
							pageArea = new Rectangle(pages.getXCordForPage(page) + xAdjustment, pages.getYCordForPage(page), this.decode_pdf
									.getPdfPageData().getScaledCropBoxWidth(page), this.decode_pdf.getPdfPageData().getScaledCropBoxHeight(page));
							if (pageArea.contains(x, y)) {
								// set displayed
								flag[1] = 1;
							}
						}
					}
				}
			}

			// Set highlighting page
			if (this.pageOfHighlight == -1 && this.startHighlighting) {
				this.pageOfHighlight = page;
			}

			// Keep track of page mouse is over at all times
			this.pageMouseIsOver = page;

			// Tidy coords for multipage views
			y = ((pages.getYCordForPage(page) + this.decode_pdf.getPdfPageData().getScaledCropBoxHeight(page)) + this.decode_pdf.getInsetH()) - y;

			this.currentGUI.setMultibox(flag);

			// if(flag[1]==1 && (findPageToHighlight && commonValues.getCurrentHighlightedPage()==-1)){
			// commonValues.setCurrentHighlightedPage(page);
			// }
			// else{
			// commonValues.setCurrentHighlightedPage(-1);
			// }
		}

		// <end-adobe>

		float scaling = this.currentGUI.getScaling();
		int inset = GUI.getPDFDisplayInset();
		int rotation = this.currentGUI.getRotation();

		// Apply inset to values
		int ex = this.currentGUI.AdjustForAlignment(x) - inset;
		int ey = y - inset;

		// undo any viewport scaling
		if (this.commonValues.maxViewY != 0) { // will not be zero if viewport in play
			ex = (int) (((ex - (this.commonValues.dx * scaling)) / this.commonValues.viewportScale));
			ey = (int) ((this.currentGUI.mediaH - ((this.currentGUI.mediaH - (ey / scaling) - this.commonValues.dy) / this.commonValues.viewportScale)) * scaling);
		}

		// Apply page scale to value
		x = (int) ((ex) / scaling);
		y = (int) ((ey / scaling));

		// Apply rotation to values
		if (rotation == 90) {
			int tmp = (x + this.currentGUI.cropY);
			x = (y + this.currentGUI.cropX);
			y = tmp;
		}
		else
			if ((rotation == 180)) {
				x = (this.currentGUI.cropW + this.currentGUI.cropX) - x;
				y = (y + this.currentGUI.cropY);
			}
			else
				if ((rotation == 270)) {
					int tmp = (this.currentGUI.cropH + this.currentGUI.cropY) - x;
					x = (this.currentGUI.cropW + this.currentGUI.cropX) - y;
					y = tmp;
				}
				else {
					x = (x + this.currentGUI.cropX);
					if (this.decode_pdf.getDisplayView() == Display.SINGLE_PAGE) y = (this.currentGUI.cropH + this.currentGUI.cropY) - y;
					else y = (this.currentGUI.cropY) + y;
				}

		return new Point(x, y);
	}

	private Point getPageCoordsInContinuousFacingDisplayMode(int x, int y, int page) {
		// <start-adobe>

		Display pages = (SingleDisplay) this.decode_pdf.getExternalHandler(Options.Display);

		if (this.currentGUI.useNewLayout) {
			int[] flag = new int[2];

			flag[0] = SwingGUI.CURSOR;
			flag[1] = 0;

			// Check if we are in the region of the left or right pages
			if (page != 1 && x > (this.decode_pdf.getWidth() / 2) && page < this.commonValues.getPageCount()) {// && x>pageArea.x){
				page++;
			}

			// Set the adjustment for page position
			int xAdjustment = (this.decode_pdf.getWidth() / 2) - (this.decode_pdf.getPdfPageData().getScaledCropBoxWidth(page))
					- (this.decode_pdf.getInsetW());

			// Unsure if this is needed. Still checking
			if (xAdjustment < 0) {
				System.err.println("x adjustment is less than 0");
				xAdjustment = 0;
			}

			// Check to see if pagearea contains the mouse
			Rectangle pageArea = new Rectangle(pages.getXCordForPage(page) + xAdjustment, pages.getYCordForPage(page), this.decode_pdf
					.getPdfPageData().getScaledCropBoxWidth(page), this.decode_pdf.getPdfPageData().getScaledCropBoxHeight(page));
			if (pageArea.contains(x, y)) {
				// set displayed
				flag[1] = 1;
			}

			// If neither of the two current pages contain the mouse start checking the other pages
			// Could be improved to minimise on the loops and calls to decode_pdf.getPageOffsets(page)
			if (flag[1] == 0) {
				if (y < pageArea.y && page > 1) {
					while (flag[1] == 0 && page > 1) {
						page--;
						xAdjustment = (this.decode_pdf.getWidth() / 2) - (this.decode_pdf.getPdfPageData().getScaledCropBoxWidth(page))
								- (this.decode_pdf.getInsetW());
						if (xAdjustment < 0) xAdjustment = 0;
						pageArea = new Rectangle(pages.getXCordForPage(page) + xAdjustment, pages.getYCordForPage(page), this.decode_pdf
								.getPdfPageData().getScaledCropBoxWidth(page), this.decode_pdf.getPdfPageData().getScaledCropBoxHeight(page));
						if (pageArea.contains(x, y)) {
							// set displayed
							flag[1] = 1;
						}

					}
				}
				else {
					if (y > pageArea.getMaxY() && page < this.commonValues.getPageCount()) {
						while (flag[1] == 0 && page < this.commonValues.getPageCount()) {
							page++;
							xAdjustment = (this.decode_pdf.getWidth() / 2) - (this.decode_pdf.getPdfPageData().getScaledCropBoxWidth(page))
									- (this.decode_pdf.getInsetW());
							if (xAdjustment < 0) xAdjustment = 0;
							pageArea = new Rectangle(pages.getXCordForPage(page) + xAdjustment, pages.getYCordForPage(page), this.decode_pdf
									.getPdfPageData().getScaledCropBoxWidth(page), this.decode_pdf.getPdfPageData().getScaledCropBoxHeight(page));
							if (pageArea.contains(x, y)) {
								// set displayed
								flag[1] = 1;
							}

						}
					}
				}
			}

			// Set highlighting page
			if (this.pageOfHighlight == -1 && this.startHighlighting) {
				this.pageOfHighlight = page;
			}

			// Keep track of page mouse is over at all times
			this.pageMouseIsOver = page;

			// Tidy coords for multipage views
			y = (((pages.getYCordForPage(page) + this.decode_pdf.getPdfPageData().getScaledCropBoxHeight(page)) + this.decode_pdf.getInsetH())) - y;

			x = x - ((pages.getXCordForPage(page)) - this.decode_pdf.getInsetW());

			this.currentGUI.setMultibox(flag);

		}
		// <end-adobe>

		float scaling = this.currentGUI.getScaling();
		int inset = GUI.getPDFDisplayInset();
		int rotation = this.currentGUI.getRotation();

		// Apply inset to values
		int ex = this.currentGUI.AdjustForAlignment(x) - inset;
		int ey = y - inset;

		// undo any viewport scaling
		if (this.commonValues.maxViewY != 0) { // will not be zero if viewport in play
			ex = (int) (((ex - (this.commonValues.dx * scaling)) / this.commonValues.viewportScale));
			ey = (int) ((this.currentGUI.mediaH - ((this.currentGUI.mediaH - (ey / scaling) - this.commonValues.dy) / this.commonValues.viewportScale)) * scaling);
		}

		// Apply page scale to value
		x = (int) ((ex) / scaling);
		y = (int) ((ey / scaling));

		// Apply rotation to values
		if (rotation == 90) {
			int tmp = (x + this.currentGUI.cropY);
			x = (y + this.currentGUI.cropX);
			y = tmp;
		}
		else
			if ((rotation == 180)) {
				x = (this.currentGUI.cropW + this.currentGUI.cropX) - x;
				y = (y + this.currentGUI.cropY);
			}
			else
				if ((rotation == 270)) {
					int tmp = (this.currentGUI.cropH + this.currentGUI.cropY) - x;
					x = (this.currentGUI.cropW + this.currentGUI.cropX) - y;
					y = tmp;
				}
				else {
					x = (x + this.currentGUI.cropX);
					if (this.decode_pdf.getDisplayView() == Display.SINGLE_PAGE) y = (this.currentGUI.cropH + this.currentGUI.cropY) - y;
					else y = (this.currentGUI.cropY) + y;
				}

		return new Point(x, y);
	}

	private Point getPageCoordsInFacingDisplayMode(int x, int y, int page) {
		// <start-adobe>
		if (this.currentGUI.useNewLayout) {

			/**
			 * TO BE IMPLEMENTED
			 */
			int[] flag = new int[2];

			flag[0] = SwingGUI.CURSOR;
			flag[1] = 0;

			flag[1] = 0;

			this.currentGUI.setMultibox(flag);

		}
		// <end-adobe>

		float scaling = this.currentGUI.getScaling();
		int inset = GUI.getPDFDisplayInset();
		int rotation = this.currentGUI.getRotation();

		// Apply inset to values
		int ex = this.currentGUI.AdjustForAlignment(x) - inset;
		int ey = y - inset;

		// undo any viewport scaling
		if (this.commonValues.maxViewY != 0) { // will not be zero if viewport in play
			ex = (int) (((ex - (this.commonValues.dx * scaling)) / this.commonValues.viewportScale));
			ey = (int) ((this.currentGUI.mediaH - ((this.currentGUI.mediaH - (ey / scaling) - this.commonValues.dy) / this.commonValues.viewportScale)) * scaling);
		}

		// Apply page scale to value
		x = (int) ((ex) / scaling);
		y = (int) ((ey / scaling));

		// Apply rotation to values
		if (rotation == 90) {
			int tmp = (x + this.currentGUI.cropY);
			x = (y + this.currentGUI.cropX);
			y = tmp;
		}
		else
			if ((rotation == 180)) {
				x = (this.currentGUI.cropW + this.currentGUI.cropX) - x;
				y = (y + this.currentGUI.cropY);
			}
			else
				if ((rotation == 270)) {
					int tmp = (this.currentGUI.cropH + this.currentGUI.cropY) - x;
					x = (this.currentGUI.cropW + this.currentGUI.cropX) - y;
					y = tmp;
				}
				else {
					x = (x + this.currentGUI.cropX);
					if (this.decode_pdf.getDisplayView() == Display.SINGLE_PAGE) y = (this.currentGUI.cropH + this.currentGUI.cropY) - y;
					else y = (this.currentGUI.cropY) + y;
				}

		return new Point(x, y);
	}

	/**
	 * Find and updates coords for the current page
	 * 
	 * @param x
	 *            :: The x coordinate of the cursors location in display area coordinates
	 * @param y
	 *            :: The y coordinate of the cursors location in display area coordinates
	 * @param page
	 *            :: The page we are currently on
	 * @return Point object of the cursor location in page coordinates
	 */
	public Point getCoordsOnPage(int x, int y, int page) {

		// Update cursor position if over page

		Point pagePosition = null;
		switch (this.decode_pdf.getDisplayView()) {
			case Display.SINGLE_PAGE:
				pagePosition = getPageCoordsInSingleDisplayMode(x, y, page);
				x = pagePosition.x;
				y = pagePosition.y;
				break;
			case Display.CONTINUOUS:
				pagePosition = getPageCoordsInContinuousDisplayMode(x, y, page);
				x = pagePosition.x;
				y = pagePosition.y;
				break;

			case Display.FACING:
				pagePosition = getPageCoordsInFacingDisplayMode(x, y, page);
				x = pagePosition.x;
				y = pagePosition.y;

				break;

			case Display.CONTINUOUS_FACING:
				pagePosition = getPageCoordsInContinuousFacingDisplayMode(x, y, page);
				x = pagePosition.x;
				y = pagePosition.y;

				break;
			default:
				break;
		}

		return new Point(x, y);
	}

	/** requests repaint of an area */
	public void repaintArea(Rectangle screenBox, int maxY) {

		int strip = 10;

		float scaling = this.decode_pdf.getScaling();

		int x = (int) (screenBox.x * scaling) - strip;
		int y = (int) ((maxY - screenBox.y - screenBox.height) * scaling) - strip;
		int width = (int) ((screenBox.x + screenBox.width) * scaling) + strip + strip;
		int height = (int) ((screenBox.y + screenBox.height) * scaling) + strip + strip;

		/** repaint manager */
		RepaintManager currentManager = RepaintManager.currentManager(this.decode_pdf);

		currentManager.addDirtyRegion(this.decode_pdf, x, y, width, height);
	}
}
