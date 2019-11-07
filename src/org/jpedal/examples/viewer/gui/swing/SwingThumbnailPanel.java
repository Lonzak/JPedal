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
 * SwingThumbnailPanel.java
 * ---------------
 */
package org.jpedal.examples.viewer.gui.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.InvocationTargetException;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.jpedal.PdfDecoder;
import org.jpedal.ThumbnailDecoder;
import org.jpedal.examples.viewer.Values;
import org.jpedal.examples.viewer.gui.generic.GUIThumbnailPanel;
import org.jpedal.objects.PdfPageData;
import org.jpedal.utils.LogWriter;
import org.jpedal.utils.SwingWorker;
import org.jpedal.utils.repositories.Vector_Object;

/**
 * Used in GUI example code. <br>
 * adds thumbnail capabilities to viewer, <br>
 * shows pages as thumbnails within this panel, <br>
 * So this panel can be added to the viewer
 * 
 */
public class SwingThumbnailPanel extends JScrollPane implements GUIThumbnailPanel {

	private static final long serialVersionUID = 3160861494248549900L;

	static final boolean debugThumbnails = false;

	/** Swing thread to decode in background - we have one thread we use for various tasks */
	SwingWorker worker = null;

	JPanel panel = new JPanel();

	/** handles drawing of thumbnails if needed */
	private ThumbPainter painter = new ThumbPainter();

	/** can switch on or off thumbnails */
	private boolean showThumbnailsdefault = true;

	private boolean showThumbnails = this.showThumbnailsdefault;

	/** flag to allow interruption in orderly manner */
	public boolean interrupt = false;

	/** flag to show drawig taking place */
	public boolean drawing, generateOtherVisibleThumbnails = false;

	/** custom decoder to create Thumbnails */
	public ThumbnailDecoder thumbDecoder;

	/**
	 * thumbnails settings below
	 */
	/** buttons to display thumbnails */
	private JButton[] pageButton;

	private BufferedImage[] images;

	private boolean[] buttonDrawn;

	private boolean[] isLandscape;

	private int[] pageHeight;

	/** weight and height for thumbnails */
	static final private int thumbH = 100, thumbW = 70;

	Values commonValues;
	final PdfDecoder decode_pdf;

	boolean isExtractor = false;
	private int lastPage = -1; // flag to ensure only changes result in processing

	public SwingThumbnailPanel(Values commonValues, final PdfDecoder decode_pdf) {

		if (debugThumbnails) System.out.println("SwingThumbnailPanel");

		setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

		this.commonValues = commonValues;
		this.decode_pdf = decode_pdf;

		this.thumbDecoder = new ThumbnailDecoder(decode_pdf);

		this.addComponentListener(new ComponentListener() {
			@Override
			public void componentResized(ComponentEvent componentEvent) {

				if (!SwingThumbnailPanel.this.isExtractor) {
					/** draw thumbnails in background, having checked not already drawing */
					if (SwingThumbnailPanel.this.drawing) terminateDrawing();

					decode_pdf.waitForDecodingToFinish();

					if (decode_pdf.isOpen()) drawThumbnails();
				}
			}

			@Override
			public void componentMoved(ComponentEvent componentEvent) {
				// To change body of implemented methods use File | Settings | File Templates.
			}

			@Override
			public void componentShown(ComponentEvent componentEvent) {
				// To change body of implemented methods use File | Settings | File Templates.
			}

			@Override
			public void componentHidden(ComponentEvent componentEvent) {
				// To change body of implemented methods use File | Settings | File Templates.
			}
		});
	}

	//
	/** class to paint thumbnails */
	private class ThumbPainter extends ComponentAdapter {

		boolean requestMade = false;
		/** used to track user stopping movement */
		Timer trapMultipleMoves = new Timer(250, new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent event) {

				if (!ThumbPainter.this.requestMade) {

					ThumbPainter.this.requestMade = true;

					if (debugThumbnails) System.out.println("actionPerformed");

					if (Values.isProcessing()) {
						if (debugThumbnails) System.out.println("Still processing page");
					}
					else {

						if (debugThumbnails) System.out.println("actionPerformed2");

						/** create any new thumbnails revaled by scroll */
						/** draw thumbnails in background, having checked not already drawing */
						if (SwingThumbnailPanel.this.drawing) terminateDrawing();

						if (debugThumbnails) System.out.println("actionPerformed3");

						ThumbPainter.this.requestMade = false;

						drawThumbnails();
					}
				}
			}
		});

		/*
		 * (non-Javadoc)
		 * @see java.awt.event.ComponentListener#componentMoved(java.awt.event.ComponentEvent)
		 */
		@Override
		public void componentMoved(ComponentEvent e) {

			// allow us to disable on scroll
			if (this.trapMultipleMoves.isRunning()) this.trapMultipleMoves.stop();

			this.trapMultipleMoves.setRepeats(false);
			this.trapMultipleMoves.start();

		}
	}

	/**
	 * create thumbnails of general images
	 * 
	 * @param thumbnailsStored
	 */
	@Override
	public void generateOtherThumbnails(String[] imageFiles, Vector_Object thumbnailsStored) {

		if (debugThumbnails) System.out.println("generateOtherThumbnails>>>>>>>>>>>>");

		this.drawing = true;

		getViewport().removeAll();
		this.panel.removeAll();

		/** draw thumbnails in background */
		int pages = imageFiles.length;

		// create display for thumbnails
		getViewport().add(this.panel);
		this.panel.setLayout(new GridLayout(pages, 1, 0, 10));

		for (int i = 0; i < pages; i++) {

			// load the image to process
			BufferedImage page;
			try {
				// Load the source image from a file or cache
				if (imageFiles[i] != null) {

					Object cachedThumbnail = thumbnailsStored.elementAt(i);

					// wait if still drawing
					this.decode_pdf.waitForDecodingToFinish();

					if (cachedThumbnail == null) {
						// page = javax.media.JAI.create("fileload", imageFiles[i]).getAsBufferedImage();
						page = ImageIO.read(new File(imageFiles[i]));

						thumbnailsStored.addElement(page);
					}
					else {
						page = (BufferedImage) cachedThumbnail;
					}

					if (page != null) {

						int w = page.getWidth();
						int h = page.getHeight();

						/** add a border */
						Graphics2D g2 = (Graphics2D) page.getGraphics();
						g2.setColor(Color.black);
						g2.draw(new Rectangle(0, 0, w - 1, h - 1));

						/** scale and refresh button */
						ImageIcon pageIcon;
						if (h > w) pageIcon = new ImageIcon(page.getScaledInstance(-1, 100, Image.SCALE_FAST));
						else pageIcon = new ImageIcon(page.getScaledInstance(100, -1, Image.SCALE_FAST));

						this.pageButton[i].setIcon(pageIcon);
						this.pageButton[i].setVisible(true);
						this.buttonDrawn[i] = true;

						this.panel.add(this.pageButton[i]);

						if (debugThumbnails) System.out.println("Added button");

					}
				}
			}
			catch (Exception e) {
				LogWriter.writeLog("Exception " + e + " loading " + imageFiles[i]);
			}
		}

		this.drawing = false;
		if (debugThumbnails) System.out.println("generateOtherThumbnails<<<<<<<<<<<<");

		this.panel.setVisible(true);
	}

	/**
	 * setup thumbnails if needed
	 */
	@Override
	public synchronized void generateOtherVisibleThumbnails(final int currentPage) {

		try {

			// flag to show drawing which terminate can reset
			this.generateOtherVisibleThumbnails = true;

			// stop multiple calls
			if (currentPage == -1 || currentPage == this.lastPage || this.pageButton == null) return;

			this.lastPage = currentPage;

			if (debugThumbnails) System.out.println("generateOtherVisibleThumbnails------->" + currentPage);

			int count = this.decode_pdf.getPageCount();

			for (int i1 = 0; i1 < count; i1++) {

				if (!this.generateOtherVisibleThumbnails) return;

				if (i1 != currentPage - 1 && i1 < this.pageButton.length) this.pageButton[i1].setBorder(BorderFactory.createEmptyBorder(10, 10, 10,
						10));
			}

			// Ensure that the value being used is within array length
			if (currentPage - 1 < this.pageButton.length) {

				// set button and scroll to
				if ((count > 1) && (currentPage > 0)) this.pageButton[currentPage - 1].setBorder(BorderFactory.createLineBorder(Color.red));

				// update thumbnail pane if needed
				Rectangle rect = this.panel.getVisibleRect();

				if (!rect.contains(this.pageButton[currentPage - 1].getLocation())) {

					if (SwingUtilities.isEventDispatchThread()) {
						Rectangle vis = new Rectangle(this.pageButton[currentPage - 1].getLocation().x,
								this.pageButton[currentPage - 1].getLocation().y, this.pageButton[currentPage - 1].getBounds().width,
								this.pageButton[currentPage - 1].getBounds().height);
						this.panel.scrollRectToVisible(vis);
					}
					else {
						SwingUtilities.invokeAndWait(new Runnable() {

							@Override
							public void run() {

								if (!SwingThumbnailPanel.this.generateOtherVisibleThumbnails) return;

								Rectangle vis = new Rectangle(SwingThumbnailPanel.this.pageButton[currentPage - 1].getLocation().x,
										SwingThumbnailPanel.this.pageButton[currentPage - 1].getLocation().y,
										SwingThumbnailPanel.this.pageButton[currentPage - 1].getBounds().width,
										SwingThumbnailPanel.this.pageButton[currentPage - 1].getBounds().height);

								if (!SwingThumbnailPanel.this.generateOtherVisibleThumbnails) return;

								SwingThumbnailPanel.this.panel.scrollRectToVisible(vis);
							}
						});
					}

				}
			}
			if (!this.generateOtherVisibleThumbnails) return;

			// commonValues.setProcessing(false);

			/** draw thumbnails in background, having checked not already drawing */
			if (this.drawing) terminateDrawing();

			if (!this.generateOtherVisibleThumbnails) return;

			/** draw thumbnails in background */
			drawThumbnails();

		}
		catch (InterruptedException e) {}
		catch (InvocationTargetException e) {}
	}

	/**
	 * redraw thumbnails if scrolled
	 */
	public void drawThumbnails() {

		if (!isEnabled()) return;

		// do not generate if still loading Linearized
		if (this.decode_pdf.isLoadingLinearizedPDF()) return;

		if (debugThumbnails) System.out.println("start drawThumbnails------->");

		// allow for re-entry
		if (this.drawing) this.terminateDrawing();

		// create the thread to just do the thumbnails
		this.worker = new SwingWorker() {

			@Override
			public Object construct() {

				SwingThumbnailPanel.this.drawing = true;

				try {
					Rectangle rect = SwingThumbnailPanel.this.panel.getVisibleRect();
					int pages = SwingThumbnailPanel.this.decode_pdf.getPageCount();

					for (int i = 0; i < pages; i++) {

						// wait if still drawing
						SwingThumbnailPanel.this.decode_pdf.waitForDecodingToFinish();

						if (SwingThumbnailPanel.this.interrupt) i = pages;
						else
							if ((SwingThumbnailPanel.this.buttonDrawn != null) && (SwingThumbnailPanel.this.pageButton != null) && (rect != null)
									&& (!SwingThumbnailPanel.this.buttonDrawn[i]) && (SwingThumbnailPanel.this.pageButton[i] != null)
									&& (rect.intersects(SwingThumbnailPanel.this.pageButton[i].getBounds()))) {

								int h = thumbH;
								if (SwingThumbnailPanel.this.isLandscape[i]) h = thumbW;

								BufferedImage page = SwingThumbnailPanel.this.thumbDecoder.getPageAsThumbnail(i + 1, h);
								if (!SwingThumbnailPanel.this.interrupt) createThumbnail(page, i + 1, false);

							}
					}

				}
				catch (Exception e) {
					// stopped thumbnails
					e.printStackTrace();
				}

				// always reset flag so we can interupt
				SwingThumbnailPanel.this.interrupt = false;

				SwingThumbnailPanel.this.drawing = false;

				if (debugThumbnails) System.out.println("end drawThumbnails-------<");

				return null;
			}
		};

		this.worker.start();
	}

	/**
	 * create a blank tile with a cross to use as a thumbnail for unloaded page
	 */
	private static BufferedImage createBlankThumbnail(int w, int h) {
		BufferedImage blank = new BufferedImage(w + 1, h + 1, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2 = (Graphics2D) blank.getGraphics();
		g2.setColor(Color.white);
		g2.fill(new Rectangle(0, 0, w, h));
		g2.setColor(Color.black);
		g2.draw(new Rectangle(0, 0, w, h));
		g2.drawLine(0, 0, w, h);
		g2.drawLine(0, h, w, 0);
		return blank;
	}

	/**
	 * @param page
	 * @return BufferedImage for page
	 */
	public BufferedImage getThumbnail(int page) {

		if (this.pageButton == null || this.pageButton[page] == null) return null;
		else return (BufferedImage) ((ImageIcon) this.pageButton[page].getIcon()).getImage();
	}

	/**
	 * @param page
	 * @return BufferedImage for page
	 */
	@Override
	public synchronized BufferedImage getImage(int page) {

		// actually stored starting 0 not 1 so adjust
		page--;

		if (this.images == null || this.images[page] == null) {
			if (page > -1) {
				int h = thumbH;
				if (this.isLandscape[page]) h = thumbW;

				BufferedImage image = this.thumbDecoder.getPageAsThumbnail(page + 1, h);
				this.images[page] = image;

				return image;
			}
			else return null;
		}
		else return this.images[page];
	}

	/**
	 * setup a thumbnail button in outlines
	 */
	private void createThumbnail(BufferedImage page, int i, boolean highLightThumbnail) {

		i--; // convert from page to array

		if (page != null) {
			/** add a border */
			Graphics2D g2 = (Graphics2D) page.getGraphics();
			g2.setColor(Color.black);
			g2.draw(new Rectangle(0, 0, page.getWidth() - 1, page.getHeight() - 1));

			/** scale and refresh button */
			final ImageIcon pageIcon = new ImageIcon(page);

			if (SwingUtilities.isEventDispatchThread()) {
				// images[i]=page;
				this.pageButton[i].setIcon(pageIcon);

				this.buttonDrawn[i] = true;
			}
			else {

				final int ii = i;
				final Runnable doPaintComponent = new Runnable() {
					@Override
					public void run() {
						// images[i]=page;
						SwingThumbnailPanel.this.pageButton[ii].setIcon(pageIcon);

						SwingThumbnailPanel.this.buttonDrawn[ii] = true;
					}
				};
				SwingUtilities.invokeLater(doPaintComponent);
			}
		}
	}

	/**
	 * setup thumbnails at start - use for general images
	 */
	@Override
	public void setupThumbnails(int pages, int[] pageUsed, int pageCount) {

		this.isExtractor = true;

		if (debugThumbnails) System.out.println("setupThumbnails2");

		this.lastPage = -1;

		Font textFont = new Font("Serif", Font.PLAIN, 12);

		// remove any added last time
		// panel.removeAll();

		getVerticalScrollBar().setUnitIncrement(80);

		// create empty thumbnails and add to display
		BufferedImage blankPortrait = createBlankThumbnail(thumbW, thumbH);
		ImageIcon portrait = new ImageIcon(blankPortrait.getScaledInstance(-1, 100, Image.SCALE_SMOOTH));

		this.isLandscape = new boolean[pages];
		this.pageHeight = new int[pages];
		this.pageButton = new JButton[pages];
		this.images = new BufferedImage[pages];
		this.buttonDrawn = new boolean[pages];

		for (int i = 0; i < pages; i++) {

			int page = i + 1;

			if (pageCount < 2) this.pageButton[i] = new JButton(String.valueOf(page), portrait); //$NON-NLS-2$
			else this.pageButton[i] = new JButton(String.valueOf(page) + " ( Page " + pageUsed[i] + " )", portrait); //$NON-NLS-2$
			this.isLandscape[i] = false;
			this.pageHeight[i] = 100;

			this.pageButton[i].setVerticalTextPosition(SwingConstants.BOTTOM);
			this.pageButton[i].setHorizontalTextPosition(SwingConstants.CENTER);
			if ((i == 0) && (pages > 1)) this.pageButton[0].setBorder(BorderFactory.createLineBorder(Color.red));
			else this.pageButton[i].setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

			this.pageButton[i].setFont(textFont);
			// panel.add(pageButton[i],BorderLayout.CENTER);

		}
	}

	/** reset the highlights */
	@Override
	public void resetHighlightedThumbnail(int item) {

		if (debugThumbnails) System.out.println("resetHighlightedThumbnail");

		if (this.pageButton != null) {
			int pages = this.pageButton.length;

			for (int i = 0; i < pages; i++) {

				if ((i == item)) this.pageButton[i].setBorder(BorderFactory.createLineBorder(Color.red));
				else this.pageButton[i].setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
			}
		}
	}

	/**
	 * setup thumbnails at start - use when adding pages
	 */
	@Override
	public void setupThumbnails(int pages, Font textFont, String message, PdfPageData pageData) {

		if (debugThumbnails) System.out.println("setupThumbnails");

		this.lastPage = -1;

		getViewport().removeAll();
		this.panel.removeAll();
		// create dispaly for thumbnails
		getViewport().add(this.panel);
		this.panel.setLayout(new GridLayout(pages, 1, 0, 10));
		this.panel.scrollRectToVisible(new Rectangle(0, 0, 1, 1));

		getVerticalScrollBar().setUnitIncrement(80);

		// create empty thumbnails and add to display

		// empty thumbnails for unloaded pages
		BufferedImage blankPortrait = createBlankThumbnail(thumbW, thumbH);
		BufferedImage blankLandscape = createBlankThumbnail(thumbH, thumbW);
		ImageIcon landscape = new ImageIcon(blankLandscape.getScaledInstance(-1, 70, Image.SCALE_SMOOTH));
		ImageIcon portrait = new ImageIcon(blankPortrait.getScaledInstance(-1, 100, Image.SCALE_SMOOTH));

		this.isLandscape = new boolean[pages];
		this.pageHeight = new int[pages];
		this.pageButton = new JButton[pages];
		this.images = new BufferedImage[pages];
		this.buttonDrawn = new boolean[pages];

		for (int i = 0; i < pages; i++) {

			int page = i + 1;

			// create blank image with correct orientation
			final int ph;// pw
			int cropWidth = pageData.getCropBoxWidth(page);
			int cropHeight = pageData.getCropBoxHeight(page);
			int rotation = pageData.getRotation(page);
			ImageIcon usedLandscape, usedPortrait;

			if ((rotation == 0) | (rotation == 180)) {
				ph = (pageData.getMediaBoxHeight(page));
				// pw=(pageData.getMediaBoxWidth(page));//%%
				usedLandscape = landscape;
				usedPortrait = portrait;
			}
			else {
				ph = (pageData.getMediaBoxWidth(page));
				// pw=(pageData.getMediaBoxHeight(page));//%%
				usedLandscape = portrait;
				usedPortrait = landscape;
			}

			if (cropWidth > cropHeight) {
				this.pageButton[i] = new JButton(message + ' ' + page, usedLandscape); //$NON-NLS-2$
				this.isLandscape[i] = true;
				this.pageHeight[i] = ph;// w;%%
			}
			else {
				this.pageButton[i] = new JButton(message + ' ' + page, usedPortrait); //$NON-NLS-2$
				this.isLandscape[i] = false;
				this.pageHeight[i] = ph;
			}

			this.pageButton[i].setVerticalTextPosition(SwingConstants.BOTTOM);
			this.pageButton[i].setHorizontalTextPosition(SwingConstants.CENTER);
			if ((i == 0) && (pages > 1)) this.pageButton[0].setBorder(BorderFactory.createLineBorder(Color.red));
			else this.pageButton[i].setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

			this.pageButton[i].setFont(textFont);
			this.panel.add(this.pageButton[i], BorderLayout.CENTER);

		}
	}

	/**
	 * return a button holding the image,so we can add listener
	 */
	@Override
	public Object[] getButtons() {
		return this.pageButton;
	}

	@Override
	public void setThumbnailsEnabled(boolean newValue) {
		this.showThumbnailsdefault = newValue;
		this.showThumbnails = newValue;
	}

	@Override
	public boolean isShownOnscreen() {
		return this.showThumbnails;
	}

	@Override
	public void resetToDefault() {
		this.showThumbnails = this.showThumbnailsdefault;
	}

	@Override
	public void setIsDisplayedOnscreen(boolean b) {
		this.showThumbnails = b;
	}

	@Override
	public void addComponentListener() {
		this.panel.addComponentListener(this.painter);
	}

	@Override
	public void removeAllListeners() {
		this.panel.removeComponentListener(this.painter);

		// remove all listeners
		Object[] buttons = getButtons();
		if (buttons != null) {
			for (Object button : buttons) {
				ActionListener[] l = ((JButton) button).getActionListeners();
				for (ActionListener aL : l)
					((JButton) button).removeActionListener(aL);
			}
		}
	}

	/** stop any drawing */
	@Override
	public void terminateDrawing() {

		// disable
		this.generateOtherVisibleThumbnails = false;

		// tell our code to exit cleanly asap
		if (this.drawing) {

			this.interrupt = true;
			while (this.drawing) {

				try {
					Thread.sleep(20);
				}
				catch (InterruptedException e) {
					// should never be called
					e.printStackTrace();
				}

			}

			this.interrupt = false; // ensure synched
		}
	}

	@Override
	public void refreshDisplay() {
		validate();
	}

	@Override
	public void dispose() {

		this.removeAll();

		this.worker = null;

		if (this.panel != null) this.panel.removeAll();
		this.panel = null;

		this.painter = null;

		this.thumbDecoder = null;

		this.pageButton = null;

		this.buttonDrawn = null;

		this.isLandscape = null;

		this.pageHeight = null;

		this.images = null;
	}
}
