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
 * PdfDecoder.java
 * ---------------
 */

package org.jpedal;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TimerTask;

import javax.imageio.stream.ImageInputStream;
import javax.print.attribute.SetOfIntegerSyntax;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.Border;

import org.jpedal.constants.PDFflags;
import org.jpedal.constants.SpecialOptions;
import org.jpedal.examples.viewer.gui.swing.SwingMouseFunctionality;
import org.jpedal.examples.viewer.gui.swing.SwingMouseListener;
import org.jpedal.exception.PdfException;
import org.jpedal.external.ColorHandler;
import org.jpedal.external.ExternalHandlers;
import org.jpedal.external.ImageHelper;
import org.jpedal.external.JPedalHelper;
import org.jpedal.external.Options;
import org.jpedal.external.RenderChangeListener;
import org.jpedal.fonts.FontMappings;
import org.jpedal.fonts.StandardFonts;
import org.jpedal.fonts.tt.TTGlyph;
import org.jpedal.grouping.PdfGroupingAlgorithms;
import org.jpedal.io.ColorSpaceConvertor;
import org.jpedal.io.DecryptionFactory;
import org.jpedal.io.ObjectStore;
import org.jpedal.io.PdfFileReader;
import org.jpedal.io.PdfObjectReader;
import org.jpedal.io.PdfReader;
import org.jpedal.io.StatusBar;
import org.jpedal.linear.LinearParser;
import org.jpedal.objects.PdfData;
import org.jpedal.objects.PdfFileInformation;
import org.jpedal.objects.PdfImageData;
import org.jpedal.objects.PdfPageData;
import org.jpedal.objects.PdfResources;
import org.jpedal.objects.acroforms.creation.FormFactory;
import org.jpedal.objects.acroforms.rendering.AcroRenderer;
import org.jpedal.objects.acroforms.rendering.DefaultAcroRenderer;
import org.jpedal.objects.layers.PdfLayerList;
import org.jpedal.objects.outlines.OutlineData;
import org.jpedal.objects.raw.PageObject;
import org.jpedal.objects.raw.PdfDictionary;
import org.jpedal.objects.raw.PdfObject;
import org.jpedal.objects.structuredtext.MarkedContentGenerator;
import org.jpedal.parser.DecoderOptions;
import org.jpedal.parser.DecoderResults;
import org.jpedal.parser.PdfStreamDecoder;
import org.jpedal.parser.SwingPrinter;
import org.jpedal.parser.ValueTypes;
import org.jpedal.render.DynamicVectorRenderer;
import org.jpedal.render.SwingDisplay;
import org.jpedal.render.output.OutputDisplay;
import org.jpedal.text.TextLines;
import org.jpedal.utils.DPIFactory;
import org.jpedal.utils.LogWriter;
import org.jpedal.utils.SwingWorker;
import org.jpedal.utils.repositories.Vector_Int;
import org.jpedal.utils.repositories.Vector_Rectangle;
import org.w3c.dom.Document;
//<start-adobe><start-ulc><start-thin>
//<end-thin><end-ulc><end-adobe>
//<start-adobe>
//<start-thin>
//<end-thin><end-adobe>
//<start-adobe>
//<end-adobe>

/**
 * Provides an object to decode pdf files and provide a rasterizer if required - Normal usage is to create instance of PdfDecoder and access via
 * public methods. Examples showing usage in org.jpedal.examples
 * <p/>
 * We recommend you access JPedal using only public methods listed in API
 */
public class PdfDecoder extends JPanel {

	private static final long serialVersionUID = 9083244515021854449L;

	/** flag to enable our new code */
	private static boolean newXFACode = false;

	/**
	 ****************************** static constants ******************************
	 */

	public static final String version = "4.92b23-OS";
	/**/

	/**
	 * flag to show extraction mode includes any text
	 */
	public static final int TEXT = 1;

	/**
	 * flag to show extraction mode includes original images
	 */
	public static final int RAWIMAGES = 2;

	/**
	 * flag to show extraction mode includes final scaled/clipped
	 */
	public static final int FINALIMAGES = 4;

	/**
	 * undocumented flag to allow shape extraction
	 */
	protected static final int PAGEDATA = 8;

	/**
	 * flag to show extraction mode includes final scaled/clipped
	 */
	public static final int RAWCOMMANDS = 16;

	/**
	 * flag to show extraction of clipped images at highest res
	 */
	public static final int CLIPPEDIMAGES = 32;

	/**
	 * flag to show extraction of clipped images at highest res
	 */
	public static final int TEXTCOLOR = 64;

	/**
	 * flag to show extraction of raw cmyk images
	 */
	public static final int CMYKIMAGES = 128;

	/**
	 * flag to show extraction of xforms metadata
	 */
	public static final int XFORMMETADATA = 256;

	/**
	 * flag to show extraction of color required (used in Storypad grouping)
	 */
	public static final int COLOR = 512;

	/**
	 * flag to show render mode includes any text
	 */
	public static final int RENDERTEXT = 1;

	/**
	 * flag to show render mode includes any images
	 */
	public static final int RENDERIMAGES = 2;

	/**
	 * flag to show render mode includes any images
	 */
	public static final int REMOVE_RENDERSHAPES = 16;

	/**
	 * flag to stop forms on decodePage
	 */
	public static final int REMOVE_NOFORMS = 32;

	/**
	 * flag to show text highlights need to be done last
	 */
	public static final int OCR_PDF = 32;

	/**
	 * printing mode using inbuilt java fonts and getting java to rasterize fonts using Java font if match found (added to get around limitations in
	 * PCL printing via JPS) - this is the default off setting
	 */
	public static final int NOTEXTPRINT = 0;

	/**
	 * printing mode using inbuilt java fonts and getting java to rasterize fonts using Java font if match found (added to get around limitations in
	 * PCL printing via JPS)
	 */
	public static final int TEXTGLYPHPRINT = 1;

	/**
	 * printing mode using inbuilt java fonts and getting java to rasterize fonts using Java font if match found (added to get around limitations in
	 * PCL printing via JPS)
	 */
	public static final int TEXTSTRINGPRINT = 2;

	/**
	 * printing mode using inbuilt java fonts and getting java to rasterize fonts using Java font if match found (added to get around limitations in
	 * PCL printing via JPS) - overrides embedded fonts for standard fonts (ie Arial)
	 */
	public static final int STANDARDTEXTSTRINGPRINT = 3;

	public static final int SUBSTITUTE_FONT_USING_FILE_NAME = 1;
	public static final int SUBSTITUTE_FONT_USING_POSTSCRIPT_NAME = 2;
	public static final int SUBSTITUTE_FONT_USING_FAMILY_NAME = 3;
	public static final int SUBSTITUTE_FONT_USING_FULL_FONT_NAME = 4;
	public static final int SUBSTITUTE_FONT_USING_POSTSCRIPT_NAME_USE_FAMILY_NAME_IF_DUPLICATES = 5;

	/**
	 ****************************** static variables ******************************
	 */

	public boolean generatingThumbnail = false;

	/**
	 * version number
	 */
	public static float javaVersion = 0f;

	/**
	 * The transparency of the highlighting box around the text stored as a float
	 */
	public static float highlightComposite = 0.35f;

	// Show onscreen mouse dragged box
	public static boolean showMouseBox = false;

	/**
	 * dpi for final images
	 */
	public static int dpi = 72;

	/**
	 * flag to tell software to embed x point after each character so we can merge any overlapping text together
	 */
	public static boolean embedWidthData = false;

	/**
	 ****************************** static objects ******************************
	 */

	// allow user to override code
	public static JPedalHelper Helper = null;// new org.jpedal.examples.ExampleHelper();

	DecoderOptions options = new DecoderOptions();

	/**
	 ****************************** swing objects ******************************
	 */

	SwingPainter swingPainter = new SwingPainter(this, this.options);

	public SwingPrinter swingPrinter = new SwingPrinter();

	/**
	 ****************************** general ******************************
	 */

	FileAccess fileAcces = new FileAccess();

	/** handlers file reading/decoding of linear PDF data */
	LinearParser linearParser = new LinearParser();

	private DPIFactory scalingdpi = new DPIFactory();

	ExternalHandlers externalHandlers = new ExternalHandlers();

	// Darker background, glowing pages
	public boolean useNewGraphicsMode = true;

	protected Display pages = null;

	/** default renderer for acroforms */
	protected AcroRenderer formRenderer;

	private PageOffsets currentOffset;

	/**
	 * copy of flag to tell program whether to create (and possibly update) screen display
	 */
	protected boolean renderPage = false;

	/** display mode (continuous, facing, single) */
	protected int displayView = Display.SINGLE_PAGE;

	/** amount we scroll screen to make visible */
	private int scrollInterval = 10;

	/** count of how many pages loaded */
	protected int pageCount = 0;

	/** when true setPageParameters draws the page rotated for use with scale to window */
	boolean isNewRotationSet = false;

	/** flag to stop multiple attempts to decode */
	protected boolean isDecoding = false;

	/** current page */
	public int pageNumber = 1;

	protected int alignment = Display.DISPLAY_LEFT_ALIGNED;

	/** used by setPageParameters to draw rotated pages */
	public int displayRotation = 0;

	/** used to reduce or increase display size */
	protected AffineTransform displayScaling;

	/** holds page information used in grouping */
	private PdfPageData pageData = new PdfPageData();

	/** allow for inset of display */
	public int insetW = 0;
	public int insetH = 0;

	/** width of the BufferedImage in pixels */
	int x_size = 100;

	/** height of the BufferedImage in pixels */
	int y_size = 100;

	/** unscaled page height */
	int max_y;

	/** unscaled page Width */
	int max_x;

	/**
	 * any scaling factor being used to convert co-ords into correct values and to alter image size
	 */
	public float scaling = 1;

	/** border for component */
	protected Border myBorder = null;

	/** the ObjectStore for this file */
	public ObjectStore objectStoreRef = new ObjectStore(null);

	/** the actual display object */
	protected DynamicVectorRenderer currentDisplay = new SwingDisplay(1, this.objectStoreRef, false); //

	protected boolean useAcceleration = true;

	/**
	 * provide access to pdf file objects
	 */
	public PdfObjectReader currentPdfFile;

	/**
	 * flag to stop multiple access to background decoding
	 */
	private boolean isBackgroundDecoding = false;

	/**
	 * store image data extracted from pdf
	 */
	private PdfImageData pdfImages = new PdfImageData();

	/**
	 * store image data extracted from pdf
	 */
	private PdfImageData pdfBackgroundImages = new PdfImageData();

	/**
	 * store text data and can be passed out to other classes
	 */
	private PdfData pdfData;

	/**
	 * store text data and can be passed out to other classes
	 */
	private PdfData pdfBackgroundData;

	// <start-adobe>
	private RefreshLayout viewListener = null;
	// <end-adobe>

	/**
	 * direct graphics 2d to render onto
	 */
	private Graphics2D g2 = null;

	/**
	 * list of fonts for decoded page
	 */
	private String fontsInFile = "";

	/**
	 * list of images for decoded page
	 */
	private String imagesInFile = "";

	/** custom upscale val for JPedal settings */
	private float multiplyer = 1;

	/**
	 * current extraction mode
	 */
	private int extractionMode = 7;

	/**
	 * current render mode
	 */
	protected int renderMode = 7;

	/**
	 * decodes page or image
	 */
	// private PdfStreamDecoder current;

	PdfResources res = new PdfResources();

	/**
	 * interactive status Bar
	 */
	protected StatusBar statusBar = null;

	boolean useHiResImageForDisplay = true;

	String filename;
	/**/

	// flag to track if page decoded twice
	private int lastPageDecoded = -1;

	/**
	 * sed to stop close on method
	 */
	private boolean closeOnExit = true;

	/**
	 * see if file open - may not be open if user interrupted open or problem encountered
	 */
	public boolean isOpen() {
		return this.isOpen;
	}

	// <start-adobe>
	/**
	 * return markedContent object as XML Document
	 * 
	 * @return Document containing XML structure with data
	 */
	public Document getMarkedContent() {

		/**
		 * objects for structured content
		 */
		return new MarkedContentGenerator().getMarkedContentTree(this.res, this.pageData, this.currentPdfFile);
	}

	/**
	 * used by remote printing to pass in page metrics
	 * 
	 * @param pageData
	 */
	public void setPageData(PdfPageData pageData) {
		this.pageData = pageData;
	}

	// <end-adobe>

	/**
	 * return page number for last page decoded (only use in SingleDisplay mode)
	 */
	public int getlastPageDecoded() {
		return this.lastPageDecoded;
	}

	/**
	 * return details on page for type (defined in org.jpedal.constants.PageInfo) or null if no values Unrecognised key will throw a RunTime exception
	 * 
	 * null returned if JPedal not clear on result
	 */
	public Iterator getPageInfo(int type) {
		return this.resultsFromDecode.getPageInfo(type);
	}

	// <start-adobe>
	/**
	 * provide direct access to outlineData object
	 * 
	 * @return OutlineData
	 */
	public OutlineData getOutlineData() {
		return this.res.getOutlineData();
	}

	/**
	 * track if file still loaded in background
	 * 
	 */
	public boolean isLoadingLinearizedPDF() {
		return false;
	}

	protected void resetMultiPageForms(int page) {
		this.swingPainter.resetMultiPageForms(page, this, this.formRenderer, this.pages);
	}

	/**
	 * class to repaint multiple views
	 */
	private class RefreshLayout extends ComponentAdapter implements Serializable {

        private static final long serialVersionUID = 5332414763142396868L;
        java.util.Timer t2 = null;

		/*
		 * (non-Javadoc)
		 * @see java.awt.event.ComponentListener#componentMoved(java.awt.event.ComponentEvent)
		 */
		@Override
		public void componentMoved(ComponentEvent e) {
			startTimer();
			// screenNeedsRedrawing=true;
		}

		/*
           */
		@Override
		public void componentResized(ComponentEvent e) {
			startTimer();
			// screenNeedsRedrawing=true;
		}

		private void startTimer() {

			// whatever else, stop current decode
			// pages.stopGeneratingPage();

			// turn if off if running
			if (this.t2 != null) this.t2.cancel();

			// restart - if its not stopped it will trigger page update
			TimerTask listener = new PageListener();
			this.t2 = new java.util.Timer();
			this.t2.schedule(listener, 500);

		}

		/**
		 * fix submitted by Niklas Matthies
		 */
		public void dispose() {
			if (this.t2 != null) this.t2.cancel();
		}

		/**
		 * used to update statusBar object if exists
		 */
		class PageListener extends TimerTask {

			@Override
			public void run() {

				if (Display.debugLayout) System.out.println("ActionPerformed " + PdfDecoder.this.pageCount);

				try {
					if (PdfDecoder.this.pages != null) {

						PdfDecoder.this.pages.stopGeneratingPage();

						// Ensure page range does not drop below one
						if (PdfDecoder.this.pageNumber < 1) PdfDecoder.this.pageNumber = 1;

						if (PdfDecoder.this.pages != null) PdfDecoder.this.pages.decodeOtherPages(PdfDecoder.this.pageNumber,
								PdfDecoder.this.pageCount);
					}
				}
				catch (Exception ee) {
					LogWriter.writeLog("Exception in run() " + ee.getMessage());
				}
			}
		}
	}

	// <end-adobe>
	/**
	 * work out machine type so we can call OS X code to get around Java bugs.
	 */
	static {

		/**
		 * see if mac
		 */
		try {
			String name = System.getProperty("os.name");
			if (name.equals("Mac OS X")) DecoderOptions.isRunningOnMac = true;
			else
				if (name.startsWith("Windows")) {
					DecoderOptions.isRunningOnWindows = true;
				}
				else
					if (name.startsWith("AIX")) {
						DecoderOptions.isRunningOnAIX = true;
					}
					else {
						if (name.equals("Linux")) {
							DecoderOptions.isRunningOnLinux = true;
						}
					}
		}
		catch (Exception e) {
			// tell user and log
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
		}

		/**
		 * get version number so we can avoid bugs in various versions
		 */
		try {
			PdfDecoder.javaVersion = Float.parseFloat(System.getProperty("java.specification.version"));
		}
		catch (Exception e) {
			// tell user and log
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
		}
	}

	// <start-demo>
	/**
	 * //<end-demo> public void showExpiry(){ } /
	 **/

	// <start-adobe>
	/**
	 * turns off the viewable area, scaling the page back to original scaling <br>
	 * <br>
	 * NOT RECOMMENDED FOR GENERAL USE (this has been added for a specific client and we have found it can be unpredictable on some PDF files).
	 */
	public void resetViewableArea() {
		this.swingPainter.resetViewableArea(this, this.pageData);
	}

	/**
	 * allows the user to create a viewport within the displayed page, the aspect ratio is keep for the PDF page <br>
	 * <br>
	 * Passing in a null value is the same as calling resetViewableArea()
	 * <p/>
	 * <br>
	 * <br>
	 * The viewport works from the bottom left of the PDF page <br>
	 * The general formula is <br>
	 * (leftMargin, <br>
	 * bottomMargin, <br>
	 * pdfWidth-leftMargin-rightMargin, <br>
	 * pdfHeight-bottomMargin-topMargin)
	 * <p/>
	 * <br>
	 * <br>
	 * NOT RECOMMENDED FOR GENERAL USE (this has been added for a specific client and we have found it can be unpredictable on some PDF files).
	 * <p/>
	 * <br>
	 * <br>
	 * The viewport will not be incorporated in printing <br>
	 * <br>
	 * Throws PdfException if the viewport is not totally enclosed within the 100% cropped pdf
	 */
	public AffineTransform setViewableArea(Rectangle viewport) throws PdfException {
		return this.swingPainter.setViewableArea(viewport, this, this.pageData);
	}

	// <end-adobe>

	// <start-adobe>
	/**
	 * return type of alignment for pages if smaller than panel - see options in Display class.
	 */
	public int getPageAlignment() {
		return this.alignment;
	}

	/**
	 * This will be needed for text extraction as it paramter makes sure widths included in text stream
	 * 
	 * @param newEmbedWidthData
	 *            - flag to embed width data in text fragments for use by grouping algorithms
	 */
	public static void init(boolean newEmbedWidthData) {

		/** get local handles onto objects/data passed in */
		embedWidthData = newEmbedWidthData;
	}

	// <end-adobe>

	/**
	 * Recommend way to create a PdfDecoder if no rendering of page may be required<br>
	 * Otherwise use PdfDecoder()
	 * 
	 * @param newRender
	 *            flag to show if pages are being rendered (true) or only extraction taking place (false).
	 */
	public PdfDecoder(boolean newRender) {

		this.pages = new SingleDisplay(this);

		/** get local handles onto flag passed in */
		this.renderPage = newRender;

		startup();

		// needs to be set so we can over-ride
		if (this.renderPage) {

			setLayout(null);
			setPreferredSize(new Dimension(100, 100));
		}
	}

	private void startup() {

		this.formRenderer = new DefaultAcroRenderer();

		// pass in user handler if set
		this.formRenderer.resetHandler(null, this, Options.FormsActionHandler);

		// once only setup for fonts (dispose sets flag to false just incase)
		if (!FontMappings.fontsInitialised) {
			FontMappings.initFonts();
			FontMappings.fontsInitialised = true;
		}
	}

	/**
	 * flag to enable popup of error messages in JPedal
	 */
	public static boolean showErrorMessages = false;

	protected int specialMode = SpecialOptions.NONE;

	/**
	 * Recommend way to create a PdfDecoder for renderer only viewer (not recommended for server extraction only processes)
	 */
	public PdfDecoder() {

		this.pages = new SingleDisplay(this);

		this.renderPage = true;

		setLayout(null);

		startup();

		setPreferredSize(new Dimension(100, 100));
	}

	private boolean isOpen = false;

	// <start-adobe>
	// <end-adobe>

	/**
	 * remove all static elements - only do once completely finished with JPedal as will not be reinitialised
	 */
	static public void disposeAllStatic() {

		StandardFonts.dispose();

		FontMappings.dispose();
	}

	/**
	 * convenience method to remove all items from memory If you wish to clear all static objects as well, you will also need to call
	 * disposeAllStatic()
	 */
	final public void dispose() {
		if (SwingUtilities.isEventDispatchThread()) {
			disposeObjects();
		}
		else {
			final Runnable doPaintComponent = new Runnable() {
				@Override
				public void run() {
					disposeObjects();
				}
			};
			SwingUtilities.invokeLater(doPaintComponent);
		}
	}

	final private void disposeObjects() {

		FontMappings.fontsInitialised = false;

		this.options.disposeObjects();

		// <start-adobe>
		// code fix from Niklas Matthies
		if (this.viewListener != null) this.viewListener.dispose();
		// <end-adobe>

		if (this.pdfData != null) this.pdfData.dispose();
		this.pdfData = null;

		if (this.pages != null) this.pages.dispose();
		this.pages = null;

		FontMappings.defaultFont = null;

		if (this.currentDisplay != null) this.currentDisplay.dispose();
		this.currentDisplay = null;

		// if(current!=null)
		// current.dispose();

		// current=null;
		if (this.currentPdfFile != null) this.currentPdfFile.dispose();
		this.currentPdfFile = null;

		// dispose the javascript object before the formRenderer object as JS accesses the renderer object
		if (this.formRenderer != null) this.formRenderer.dispose();
		this.formRenderer = null;
	}

	/**
	 * convenience method to close the current PDF file
	 */
	final public void closePdfFile() {

		// <start-demo>
		/**
		 * //<end-demo> /
		 **/

		if (!this.isOpen) {
			return;
		}

		this.isOpen = false;

		// ensure no previous file still being decoded
		waitForDecodingToFinish();

		// flush linearization objects and
		// make sure we have stopped thread doing background linear reading
		this.linearParser.closePdfFile();

		this.displayScaling = null;

		this.lastPageDecoded = -1;

		if (this.pages != null) this.pages.stopGeneratingPage();

		// we are closing the page so call the closeing page for forms actions.
		// we need an example and then we can implement against
		// we just have to find it hopefully attached to the page object
		// if (formRenderer != null)
		// formRenderer.getActionHandler().PO(pageNumber);

		this.pages.disableScreen();

		this.swingPrinter.clear();

		// pass handle into renderer
		if (this.formRenderer != null) {
			this.formRenderer.openFile(this.pageCount);

			this.formRenderer.resetFormData(this.insetW, this.insetH, this.pageData, this.currentPdfFile,
					this.res.getPdfObject(PdfResources.AcroFormObj));

			this.formRenderer.removeDisplayComponentsFromScreen();
		}

		// <start-adobe>
		// remove listener if setup
		if (this.viewListener != null) {

			// flush any cached pages
			this.pages.flushPageCaches();

			removeComponentListener(this.viewListener);

			this.viewListener.dispose();
			this.viewListener = null;

		}
		// <end-adobe>

		if (this.currentPdfFile != null && this.closeOnExit) {
			this.currentPdfFile.closePdfFile();

			this.currentPdfFile = null;
		}

		this.pages.disableScreen();
		this.currentDisplay.flush();

		ObjectStore.flushPages();

		this.objectStoreRef.flush();

		this.pageCount = 0;

		this.res.flushObjects();

		// <start-adobe>
		this.setDisplayView(Display.SINGLE_PAGE, Display.DISPLAY_CENTERED);
		// <end-adobe>

		if (SwingUtilities.isEventDispatchThread()) {
			validate();
		}
		else {
			final Runnable doPaintComponent = new Runnable() {
				@Override
				public void run() {
					validate();
				}
			};
			SwingUtilities.invokeLater(doPaintComponent);
		}
	}

	// <start-adobe>
	/**
	 * Access should not generally be required to this class. Please look at getBackgroundGroupingObject() - provide method for outside class to get
	 * data object containing text and metrics of text. - Viewer can only access data for finding on page
	 * 
	 * @return PdfData object containing text content from PDF
	 */
	final public PdfData getPdfBackgroundData() {
		return this.pdfBackgroundData;
	}

	/**
	 * Access should not generally be required to this class. Please look at getGroupingObject() - provide method for outside class to get data object
	 * containing raw text and metrics of text<br>
	 * - Viewer can only access data for finding on page
	 * 
	 * @return PdfData object containing text content from PDF
	 */
	final public PdfData getPdfData() throws PdfException {
		if ((this.extractionMode & PdfDecoder.TEXT) == 0) throw new PdfException(
				"[PDF] Page data object requested will be empty as text extraction disabled. Enable with PdfDecoder method setExtractionMode(PdfDecoder.TEXT | other values");
		else return this.pdfData;
	}

	/**
	 * flag to show if PDF document contains an outline
	 */
	final public boolean hasOutline() {
		return this.res.hasOutline();
	}

	/**
	 * return a DOM document containing the PDF Outline object as a DOM Document - may return null
	 */
	final public Document getOutlineAsXML() {
		return this.res.getOutlineAsXML(this.currentPdfFile, this.pageCount);
	}

	// <end-adobe>
	/**
	 * Provides method for outside class to get data object containing information on the page for calculating grouping <br>
	 * Please note: Structure of PdfPageData is not guaranteed to remain constant. Please contact IDRsolutions for advice.
	 * 
	 * @return PdfPageData object
	 */
	final public PdfPageData getPdfPageData() {
		return this.pageData;
	}

	/**
	 * set page range (inclusive) - If end is less than start it will print them backwards (invalid range will throw PdfException)
	 * 
	 * @throws PdfException
	 */
	public void setPagePrintRange(int start, int end) throws PdfException {

		this.swingPrinter.setPagePrintRange(start, end, this.pageCount);
	}

	/**
	 * tells program to try and use Java's font printing if possible as work around for issue with PCL printing - values are PdfDecoder.TEXTGLYPHPRINT
	 * (use Java to rasterize font if available) PdfDecoder.TEXTSTRINGPRINT( print as text not raster - fastest option) PdfDecoder.NOTEXTPRINT
	 * (default - highest quality)
	 */
	public void setTextPrint(int textPrint) {
		this.textPrint = textPrint;
	}

	/**
	 * flag to use Java's inbuilt font renderer if possible
	 */
	public int textPrint = 0;

	/**
	 * the size above which objects stored on disk (-1 is off)
	 */
	private int minimumCacheSize = -1;// 20000;

	/**
	 * return any messages on decoding
	 */
	String decodeStatus = "";

	private DecoderResults resultsFromDecode = new DecoderResults();

	private Object customSwingHandle;

	private Object userExpressionEngine;

	private boolean generateGlyphOnRender;

	/** use this to turn javascript on and off, default is on. */
	public void setJavaScriptUsed(boolean jsEnabled) {
		this.options.setJavaScriptUsed(jsEnabled);
	}

	/**
	 * If you are printing PDFs using JPedal in your custom code, you may find pages missing, because JPedal does not know about these additional
	 * pages. This method allows you to tell JPedal you have already printed pagesPrinted
	 */
	public void useLogicalPrintOffset(int pagesPrinted) {
		this.swingPrinter.useLogicalPrintOffset(pagesPrinted);
	}

	/** used to render to image and then print the image */
	BufferedImage printImage = null;

	/**
	 * generate BufferedImage of a page in current file
	 * 
	 * Page size is defined by CropBox
	 */
	public BufferedImage getPageAsImage(int pageIndex) throws PdfException {
		return getPageAsImage(pageIndex, false);
	}

	/**
	 * generate BufferedImage of a page in current file
	 */
	public BufferedImage getPageAsTransparentImage(int pageIndex) throws PdfException {
		return getPageAsImage(pageIndex, true);
	}

	/**
	 * generate BufferedImage of a page in current file
	 */
	private BufferedImage getPageAsImage(int pageIndex, boolean imageIsTransparent) throws PdfException {

		BufferedImage image = null;

		// make sure in range
		if (pageIndex > this.pageCount || pageIndex < 1) {
			if (LogWriter.isOutput()) LogWriter.writeLog("Page " + pageIndex + " not in range");
		}
		else {

			if (this.currentPdfFile == null) throw new PdfException("File not open - did you call closePdfFile() inside a loop and not reopen");

			/** get pdf object id for page to decode */
			String currentPageOffset = this.currentPdfFile.getReferenceforPage(pageIndex);

			if (currentPageOffset != null) {

				PDFtoImageConvertor PDFtoImage;

				if (!this.generatingThumbnail) {
					PDFtoImage = new PDFtoImageConvertor(this.multiplyer, this.options);
					// pass in HTML handler if in invisible text on Image mode
					if (this.currentDisplay.getValue(org.jpedal.render.output.OutputDisplay.TextMode) == OutputDisplay.TEXT_INVISIBLE_ON_IMAGE
							|| this.currentDisplay.getValue(org.jpedal.render.output.OutputDisplay.TextMode) == OutputDisplay.TEXT_VISIBLE_ON_IMAGE) {
						PDFtoImage.setHTMLInvisibleTextHandler(this.currentDisplay);
					}
				}
				else {
					PDFtoImage = new PDFtoImageConvertor(1, this.options); // fixed multiplyer for thumbnails
				}

				image = PDFtoImage.convert(this.resultsFromDecode, this.displayRotation, this.res, this.displayView, this.externalHandlers,
						this.renderMode, this.pageData, this.formRenderer, this.scaling, this.currentPdfFile, pageIndex, imageIsTransparent,
						currentPageOffset);

				this.multiplyer = PDFtoImage.getMultiplyer();

				// just incase also rendered
				if (this.pages != null) this.formRenderer.getCompData().resetScaledLocation(this.pages.getOldScaling(), this.displayRotation, 0);

			}

			// workaround for bug in AIX
			if (!DecoderOptions.isRunningOnAIX && !imageIsTransparent && image != null) image = ColorSpaceConvertor.convertToRGB(image);

		}

		return image;
	}

	// <start-wrap>
	/**
	 * return scaleup factor applied to last Hires image of page generated
	 * 
	 * negative values mean no upscaling applied and should be ignored
	 */
	public float getHiResUpscaleFactor() {
		return this.multiplyer;
	}

	// <end-wrap>

	/**
	 * provide method for outside class to clear store of objects once written out to reclaim memory
	 * 
	 * @param reinit
	 *            flag to show if image data flushed as well
	 */
	final public void flushObjectValues(boolean reinit) {

		if (this.pdfData != null) this.pdfData.flushTextList(reinit);

		if (this.pdfImages != null && reinit) this.pdfImages.clearImageData();
	}

	// <start-adobe>

	/**
	 * provide method for outside class to get data object containing images
	 * 
	 * @return PdfImageData containing image metadata
	 */
	final public PdfImageData getPdfImageData() {
		return this.pdfImages;
	}

	/**
	 * provide method for outside class to get data object containing images.
	 * 
	 * @return PdfImageData containing image metadata
	 */
	final public PdfImageData getPdfBackgroundImageData() {
		return this.pdfBackgroundImages;
	}

	// <end-adobe>

	/**
	 * set render mode to state what is displayed onscreen (ie RENDERTEXT,RENDERIMAGES) - only generally required if you do not wish to show all
	 * objects on screen (default is all). Add values together to combine settings.
	 */
	final public void setRenderMode(int mode) {

		this.renderMode = mode;

		this.extractionMode = mode;
	}

	/**
	 * set extraction mode telling JPedal what to extract - (TEXT,RAWIMAGES,FINALIMAGES - add together to combine) - See org.jpedal.examples for
	 * specific extraction examples
	 */
	final public void setExtractionMode(int mode) {

		this.extractionMode = mode;
	}

	/**
	 * method to return null or object giving access info fields and metadata.
	 */
	final public PdfFileInformation getFileInformationData() {
		return this.res.getMetaData(this.currentPdfFile);
	}

	/**
	 * 
	 * Please do not use for general usage. Use setPageParameters(scalingValue, pageNumber) to set page scaling
	 */
	final public void setExtractionMode(int mode, float scaling) {

		this.scaling = scaling;

		this.pageData.setScalingValue(scaling); // ensure aligned

		this.extractionMode = mode;

		PdfLayerList layers = this.res.getPdfLayerList();
		if (layers != null) {
			boolean layersChanged = layers.setZoom(scaling);

			if (layersChanged) {
				try {
					decodePage(-1);
				}
				catch (Exception e) {
					// tell user and log
					if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
				}
			}
		}
	}

	/**
	 * 
	 * Please do not use for general usage. Use setPageParameters(scalingValue, pageNumber) instead;
	 * 
	 * @deprecated use setPageParameters(scalingValue, pageNumber) instead
	 */
	@Deprecated
	final public void setExtractionMode(int mode, int imageDpi, float scaling) {

		if (dpi % 72 != 0 && LogWriter.isOutput()) LogWriter.writeLog("Dpi is not a factor of 72- this may cause problems");

		dpi = imageDpi;

		this.scaling = scaling;

		this.pageData.setScalingValue(scaling); // ensure aligned

		this.extractionMode = mode;

		PdfLayerList layers = this.res.getPdfLayerList();
		if (layers != null) {
			boolean layersChanged = layers.setZoom(scaling);

			if (layersChanged) {
				try {
					decodePage(-1);
				}
				catch (Exception e) {
					// tell user and log
					if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
				}
			}
		}
	}

	/**
	 * @return handle on PDFFactory which adjusts display size so matches size in Acrobat
	 * 
	 */
	public DPIFactory getDPIFactory() {
		return this.scalingdpi;
	}

	/**
	 * initialise panel and set size to fit PDF page<br>
	 * intializes display with rotation set to the default, specified in the PDF document scaling value of -1 means keep existing setting
	 */
	final public void setPageParameters(float scaling, int pageNumber) {

		this.pageNumber = pageNumber;
		this.multiplyer = 1; // Reset multiplier so we don't get an image scaled too far in

		// pick up flag to prevent loop
		if (this.displayView == Display.PAGEFLOW && scaling == -100f) return;

		// ignore negative value or set
		if (scaling > 0) this.scaling = scaling;
		else scaling = this.scaling;

		if (this.pages != null) this.pages.setScaling(scaling);

		PdfLayerList layers = this.res.getPdfLayerList();
		if (layers != null) {
			boolean layersChanged = layers.setZoom(this.scalingdpi.removeScaling(scaling));

			if (layersChanged) {
				try {
					decodePage(-1);
				}
				catch (Exception e) {
					// tell user and log
					if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
				}
			}
		}

		this.pageData.setScalingValue(scaling); // ensure aligned

		int mediaW = this.pageData.getMediaBoxWidth(pageNumber);
		this.max_y = this.pageData.getMediaBoxHeight(pageNumber);
		this.max_x = this.pageData.getMediaBoxWidth(pageNumber);

		int cropW = this.pageData.getCropBoxWidth(pageNumber);
		int cropH = this.pageData.getCropBoxHeight(pageNumber);

		this.x_size = (int) ((cropW) * scaling);
		this.y_size = (int) ((cropH) * scaling);

		// rotation is broken in viewer without this - you can't alter it
		// can anyone remember why we added this code???
		// it breaks PDFs if the rotation changes between pages
		if (!this.isNewRotationSet && this.displayView != Display.PAGEFLOW) {
			this.displayRotation = this.pageData.getRotation(pageNumber);
		}
		else {
			this.isNewRotationSet = false;
		}

		this.currentDisplay.init(mediaW, this.max_y, this.displayRotation, this.options.getPageColor());

		// <start-html>
		if (this.currentDisplay.getType() != DynamicVectorRenderer.CREATE_HTML && this.currentDisplay.getType() != DynamicVectorRenderer.CREATE_SVG
				&& this.currentDisplay.getType() != DynamicVectorRenderer.CREATE_JAVAFX) {
			this.currentDisplay.setValue(DynamicVectorRenderer.ALT_BACKGROUND_COLOR, this.options.getPageColor().getRGB());
			if (this.options.getTextColor() != null) {
				this.currentDisplay.setValue(DynamicVectorRenderer.ALT_FOREGROUND_COLOR, this.options.getTextColor().getRGB());

				if (this.options.getChangeTextAndLine()) this.currentDisplay.setValue(DynamicVectorRenderer.FOREGROUND_INCLUDE_LINEART, 1);
				else this.currentDisplay.setValue(DynamicVectorRenderer.FOREGROUND_INCLUDE_LINEART, 0);
			}
		}
		// <end-html>

		/** update the AffineTransform using the current rotation */
		this.swingPainter.setPageRotation(this.displayRotation, this.pageData);

		// refresh forms in case any effected
		this.formRenderer.getCompData().setForceRedraw(true);
	}

	/** calls setPageParameters(scaling,pageNumber) after setting rotation to draw page */
	final public void setPageParameters(float scaling, int pageNumber, int newRotation) {

		this.isNewRotationSet = true;
		this.displayRotation = newRotation;
		if (this.displayView == Display.PAGEFLOW) this.pages.init(0, 0, this.displayRotation, 0, null, false, null, 0, 0);
		else setPageParameters(scaling, pageNumber);
	}

	// <start-adobe>
	/**
	 * set status bar to use when decoding a page - StatusBar provides a GUI object to display progress and messages.
	 */
	public void setStatusBarObject(StatusBar statusBar) {
		this.statusBar = statusBar;
	}

	// <end-adobe>

	/**
	 * wait for decoding to finish
	 */
	public void waitForDecodingToFinish() {

		// wait to die
		while (this.isDecoding) {
			// System.out.println("Waiting to die");
			try {
				Thread.sleep(100);
			}
			catch (InterruptedException e) {
				// tell user and log
				if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());

				// ensure will exit loop
				this.isDecoding = false;
			}
		}
	}

	/**
	 * used by Javascript to update page number
	 */
	public void updatePageNumberDisplayed(int page) {

		// update page number
		if (page != -1 && this.customSwingHandle != null) ((org.jpedal.gui.GUIFactory) this.customSwingHandle).setPage(page);
	}

	// <start-adobe>

	/**
	 * gets DynamicVector Object
	 */
	public DynamicVectorRenderer getDynamicRenderer() {
		return this.currentDisplay;
	}

	/**
	 * gets DynamicVector Object - NOT PART OF API and subject to change (DO NOT USE)
	 */
	public DynamicVectorRenderer getDynamicRenderer(boolean reset) {

		DynamicVectorRenderer latestVersion = this.currentDisplay;

		if (reset) this.currentDisplay = new SwingDisplay(0, this.objectStoreRef, false);

		return latestVersion;
	}

	// <end-adobe>

	/**
	 * Override setCursor so that we can turn on and off
	 */
	public void setPDFCursor(Cursor c) {
		if (SingleDisplay.allowChangeCursor) {
			this.setCursor(c);
		}
	}

	// All Pdf decoder to remember the default for current settings
	private Cursor defaultCursor = null;

	/**
	 * When changing the mouse mode we call this method to set the mouse mode default cursor
	 * 
	 * @param c
	 *            :: The cursor to set as the default
	 */
	public void setDefaultCursor(Cursor c) {
		this.defaultCursor = c;
		if (SingleDisplay.allowChangeCursor) {
			setCursor(c);
		}
	}

	// When default option is used, use the set default not the component default
	@Override
	public void setCursor(Cursor c) {
		if (SingleDisplay.allowChangeCursor) {
			if (c.getType() == Cursor.DEFAULT_CURSOR && this.defaultCursor != null) {
				super.setCursor(this.defaultCursor);
			}
			else {
				super.setCursor(c);
			}
		}
	}

	/**
	 * decode a page, - <b>page</b> must be between 1 and <b>PdfDecoder.getPageCount()</b> - Will kill off if already running
	 * 
	 * returns minus page if trying to open linearized page not yet available
	 */
	final public void decodePage(int rawPage) throws PdfException {

		// allow us to insert our own version (ie HTML)
		Object customDVR = this.externalHandlers.getExternalHandler(Options.CustomOutput);
		if (customDVR != null) this.currentDisplay = (DynamicVectorRenderer) customDVR;

		boolean isForm = false;

		// flag to allow us to not do some things when we re decode the page with layers on for example
		boolean isDuplicate = false;
		if (rawPage == -1) {
			rawPage = this.lastPageDecoded;
			isDuplicate = true;
		}

		boolean isSamePage = false;
		if (rawPage == this.lastPageDecoded) isSamePage = true;

		final int page = rawPage;

		if (this.isDecoding) {
			if (LogWriter.isOutput()) LogWriter
					.writeLog("[PDF]WARNING - this file is being decoded already - use  waitForDecodingToFinish() to check");

		}
		else {

			boolean isPageAvailable = isPageAvailable(rawPage);
			PdfObject pdfObject = this.linearParser.getLinearPageObject();

			/**
			 * if linearized and PdfObject then setup
			 */
			if (!isPageAvailable) {
				this.isDecoding = false;
				return;
			}
			else
				if (isPageAvailable && pdfObject != null) {
					this.isDecoding = true;
					readAllPageReferences(true, pdfObject, new HashMap(1000), new HashMap(1000), rawPage);
				}

			// reset
			this.resultsFromDecode.resetTimeout();

			try {
				this.isDecoding = true;

				PdfLayerList layers = this.res.getPdfLayerList();
				if (layers != null && layers.getChangesMade()) {

					this.lastPageDecoded = -1;
					layers.setChangesMade(false);// set flag to say we have decoded the changes

					// refresh forms in case any effected by layer change
					this.formRenderer.getCompData().setForceRedraw(true);
					this.formRenderer.getCompData().setLayerData(layers);
					this.formRenderer.getCompData().resetScaledLocation(this.scaling, this.displayRotation, (int) this.swingPainter.getIndent());// indent
																																					// here
																																					// does
																																					// nothing.

				}

				if (this.displayView != Display.SINGLE_PAGE) {
					this.isDecoding = false;
					return;
				}

				this.lastPageDecoded = page;
				this.decodeStatus = "";
				this.swingPainter.setCursorBoxOnScreen(null, isSamePage, this.formRenderer);

				/** flush renderer */
				this.currentDisplay.flush();
				this.pages.refreshDisplay();

				/** check in range */
				if (page > this.pageCount || page < 1) {

					if (LogWriter.isOutput()) LogWriter.writeLog("Page out of bounds");

					this.isDecoding = false;

				}
				else {

					// <start-adobe>
					/**
					 * title changes to give user something to see under timer control
					 */
					Timer t = null;
					if (this.statusBar != null) {
						ActionListener listener = new ProgressListener();
						t = new Timer(150, listener);
						t.start(); // start it
					}

					// <end-adobe>

					this.pageNumber = page;
					String currentPageOffset = this.currentPdfFile.getReferenceforPage(page);

					/**
					 * decode the file if not already decoded, there is a valid object id and it is unencrypted
					 */
					if (currentPageOffset != null && this.currentPdfFile == null) throw new PdfException(
							"File not open - did you call closePdfFile() inside a loop and not reopen");

					/** get pdf object id for page to decode */
					if (pdfObject == null) {
						pdfObject = new PageObject(currentPageOffset);
						this.currentPdfFile.readObject(pdfObject);

						// allow for res in parent and add to object
						this.currentPdfFile.checkParentForResources(pdfObject);
					}

					PdfStreamDecoder current = null;

					/** read page or next pages */
					if (pdfObject != null) {

						byte[][] pageContents = pdfObject.getKeyArray(PdfDictionary.Contents);

						if (this.getFormRenderer().isXFA() && newXFACode) {
							current = new org.jpedal.parser.XFAStreamDecoder(this.currentPdfFile, this.useHiResImageForDisplay,
									this.res.getPdfLayerList(), getFormRenderer());
						}
						else {
							// needs to be out of loop as we can get flattened forms on pages with no content
							current = new PdfStreamDecoder(this.currentPdfFile, this.useHiResImageForDisplay, this.res.getPdfLayerList());
						}

						/** set hires mode or not for display */
						current.setXMLExtraction(this.options.isXMLExtraction());

						this.currentDisplay.setHiResImageForDisplayMode(this.useHiResImageForDisplay);
						this.currentDisplay.setPrintPage(page);
						this.currentDisplay.setCustomColorHandler((ColorHandler) this.externalHandlers.getExternalHandler(Options.ColorHandler));

						current.setParameters(true, this.renderPage, this.renderMode, this.extractionMode);

						this.externalHandlers.addHandlers(current);

						current.setObjectValue(ValueTypes.Name, this.filename);
						current.setIntValue(ValueTypes.PageNum, page);
						current.setObjectValue(ValueTypes.ObjectStore, this.objectStoreRef);
						current.setObjectValue(ValueTypes.StatusBar, this.statusBar);
						current.setObjectValue(ValueTypes.PDFPageData, this.pageData);
						current.setObjectValue(ValueTypes.DynamicVectorRenderer, this.currentDisplay);

						// if it has no content but it still has a name obj then we need to still do all this.
						if (pageContents != null) {

							this.res.setupResources(current, false, pdfObject.getDictionary(PdfDictionary.Resources), this.pageNumber,
									this.currentPdfFile);

							if (this.g2 != null) current.setObjectValue(ValueTypes.DirectRendering, this.g2);

							this.currentDisplay.init(this.pageData.getMediaBoxWidth(this.pageNumber),
									this.pageData.getMediaBoxHeight(this.pageNumber), this.pageData.getRotation(this.pageNumber),
									this.options.getPageColor());

							// <start-html>
							if (this.currentDisplay.getType() != DynamicVectorRenderer.CREATE_HTML
									&& this.currentDisplay.getType() != DynamicVectorRenderer.CREATE_SVG
									&& this.currentDisplay.getType() != DynamicVectorRenderer.CREATE_JAVAFX) {

								if (this.options.getTextColor() != null) {
									this.currentDisplay.setValue(DynamicVectorRenderer.ALT_FOREGROUND_COLOR, this.options.getTextColor().getRGB());

									if (this.options.getChangeTextAndLine()) {
										this.currentDisplay.setValue(DynamicVectorRenderer.FOREGROUND_INCLUDE_LINEART, 1);
									}
									else {
										this.currentDisplay.setValue(DynamicVectorRenderer.FOREGROUND_INCLUDE_LINEART, 0);
									}

									this.currentDisplay.setValue(DynamicVectorRenderer.COLOR_REPLACEMENT_THRESHOLD,
											this.options.getReplacementColorThreshold());
								}
							}
							// <end-html>

							try {
								/*
								 * If highlights are required for page, reset highlights
								 */
								if (this.textLines != null && true) {
									this.textLines.setLineAreas(null);
								}

								current.decodePageContent(pdfObject);

								// All data loaded so now get all line areas for page
								if (this.textLines != null && this.extractionMode > 0) {
									Vector_Rectangle vr = (Vector_Rectangle) current.getObjectValue(ValueTypes.TextAreas);
									vr.trim();
									Rectangle[] pageTextAreas = vr.get();

									Vector_Int vi = (Vector_Int) current.getObjectValue(ValueTypes.TextDirections);
									vi.trim();
									int[] pageTextDirections = vi.get();

									for (int k = 0; k != pageTextAreas.length; k++) {
										this.textLines.addToLineAreas(pageTextAreas[k], pageTextDirections[k], page);
									}
								}

							}
							catch (Error err) {
								this.decodeStatus = this.decodeStatus + "Error in decoding page " + err.toString();
							}
							catch (Exception e) {

								// cascade up so we can show in viewer
								if (e.getMessage().contains("JPeg 2000")) {
									this.decodeStatus = this.decodeStatus + "Error in decoding page " + e.toString();
								}
							}

							/**
							 * set flags after decode
							 */
							this.fontsInFile = (String) current.getObjectValue(PdfDictionary.Font);
							this.imagesInFile = (String) current.getObjectValue(PdfDictionary.Image);

							this.pdfData = (PdfData) current.getObjectValue(ValueTypes.PDFData);

							this.pdfImages = (PdfImageData) current.getObjectValue(ValueTypes.PDFImages);

							// read flags
							this.resultsFromDecode.update(current, true);

						}
						else
							if (this.currentDisplay.getType() == DynamicVectorRenderer.CREATE_HTML
									|| this.currentDisplay.getType() == DynamicVectorRenderer.CREATE_SVG) { // needed if no page content

								int mediaW = this.pageData.getMediaBoxWidth(this.pageNumber);
								int mediaH = this.pageData.getMediaBoxHeight(this.pageNumber);
								// int mediaX = pageData.getMediaBoxX(pageNumber);
								// int mediaY = pageData.getMediaBoxY(pageNumber);
								int rotation = this.pageData.getRotation(this.pageNumber);
								this.currentDisplay.init(mediaW, mediaH, rotation, this.options.getPageColor());

								// removed as breaks HTML with NPE
								// currentDisplay.setValue(currentDisplay.ALT_BACKGROUND_COLOR, options.getPageColor().getRGB());
								// currentDisplay.setValue(currentDisplay.ALT_FOREGROUND_COLOR, options.getTextColor().getRGB());

							}
					}

					/** turn off status bar update */
					// <start-adobe>
					if (t != null) {
						t.stop();
						this.statusBar.setProgress(100);

					}
					// <end-adobe>

					/**
					 * handle acroform data to display
					 */
					if (this.renderPage) {

						if (!isDuplicate && this.formRenderer != null && !this.formRenderer.ignoreForms() && this.formRenderer.hasFormsOnPage(page)) {

							boolean runDirect = false;

							// swing needs it to be done with invokeLater
							if (!DefaultAcroRenderer.isLazyInit && !runDirect && !SwingUtilities.isEventDispatchThread()
									&& this.formRenderer.getFormFactory().getType() == FormFactory.SWING) { //

								final PdfStreamDecoder finalCurrent = current;
								// final Runnable doPaintComponent2 = new Runnable() {
								// public void run() {
								//
								// System.out.println("create forms");
								//
								// createFormComponents(page, finalCurrent);
								//
								//
								// isDecoding=false;
								//
								// //tell software page all done
								// currentDisplay.flagDecodingFinished();
								//
								// // System.out.println("done");
								//
								// }
								// };

								// don't set isDecoding false at end but in our routine
								isForm = true;

								new SwingWorker() {
									@Override
									public Object construct() {
										// System.out.println("create forms");

										createFormComponents(page, finalCurrent);

										PdfDecoder.this.isDecoding = false;

										// tell software page all done
										PdfDecoder.this.currentDisplay.flagDecodingFinished();

										// System.out.println("done");
										return null;
									}
								}.start();

								// SwingUtilities.invokeLater(doPaintComponent2);

								// this.waitForDecodingToFinish();

							}
							else {
								if ((this.renderMode & PdfDecoder.REMOVE_NOFORMS) != PdfDecoder.REMOVE_NOFORMS) {

									createFormComponents(page, current);

								}
								this.isDecoding = false;
							}
						}
					}
				}

			}
			finally {

				if (!isForm) {

					this.isDecoding = false;

					if (this.statusBar != null) this.statusBar.percentageDone = 100;
				}
				else waitForDecodingToFinish();
			}
		}

		// Check for exceptions in TrueType hinting and re decode if neccessary
		if (TTGlyph.redecodePage) {
			TTGlyph.redecodePage = false;
			decodePage(rawPage);
		}

		// tell software page all done
		this.currentDisplay.flagDecodingFinished();
	}

	/**
	 * see if page available if in Linearized mode or return true
	 * 
	 * @param rawPage
	 */
	public synchronized boolean isPageAvailable(int rawPage) {
		return this.linearParser.isPageAvailable(rawPage, this.currentPdfFile);
	}

	void createFormComponents(int page, PdfStreamDecoder current) {

		// no forms on JavaFX
		if (this.currentDisplay.getType() == DynamicVectorRenderer.CREATE_JAVAFX) return;

		if (this.currentOffset != null) this.formRenderer.getCompData().setPageValues(this.scaling, this.displayRotation,
				(int) this.swingPainter.getIndent(), 0, 0, Display.SINGLE_PAGE, this.currentOffset.widestPageNR, this.currentOffset.widestPageR);

		this.formRenderer.createDisplayComponentsForPage(page, current);
	}

	/**
	 * store objects to use on a print
	 * 
	 * @param page
	 * @param type
	 * @param colors
	 * @param obj
	 * @throws PdfException
	 */
	public void printAdditionalObjectsOverPage(int page, int[] type, Color[] colors, Object[] obj) throws PdfException {

		this.swingPrinter.printAdditionalObjectsOverPage(page, type, colors, obj);
	}

	/**
	 * store objects to use on a print
	 * 
	 * @param type
	 * @param colors
	 * @param obj
	 * @throws PdfException
	 */
	public void printAdditionalObjectsOverAllPages(int[] type, Color[] colors, Object[] obj) throws PdfException {

		this.swingPrinter.printAdditionalObjectsOverAllPages(type, colors, obj);
	}

	/**
	 * uses hires images to create a higher quality display - downside is it is slower and uses more memory (default is false).- Does nothing in OS
	 * version
	 * 
	 * @param value
	 */
	public void useHiResScreenDisplay(boolean value) {
		// <start-wrap>
		this.useHiResImageForDisplay = value;
		// <end-wrap>
	}

	// <start-adobe>
	/**
	 * decode a page as a background thread (use other background methods to access data)
	 * 
	 * we now recommend you use decodePage as this has been heavily optimised for speed
	 */
	final synchronized public void decodePageInBackground(int i) {

		if (this.isDecoding) {
			if (LogWriter.isOutput()) {
				LogWriter.writeLog("[PDF]WARNING - this file is being decoded already in foreground");
				LogWriter.writeLog("[PDF]Multiple access not recommended - use  waitForDecodingToFinish() to check");
			}

		}
		else
			if (this.isBackgroundDecoding) {
				if (LogWriter.isOutput()) LogWriter.writeLog("[PDF]WARNING - this file is being decoded already in background");
			}
			else {

				try {
					this.isBackgroundDecoding = true;

					/** check in range */
					if (i > this.pageCount) {

						if (LogWriter.isOutput()) LogWriter.writeLog("Page out of bounds");

					}
					else {

						/** get pdf object id for page to decode */
						String currentPageOffset = this.currentPdfFile.getReferenceforPage(i);

						/**
						 * decode the file if not already decoded, there is a valid object id and it is unencrypted
						 */
						if ((currentPageOffset != null)) {

							if (this.currentPdfFile == null) throw new PdfException(
									"File not open - did you call closePdfFile() inside a loop and not reopen");

							/** read page or next pages */
							PdfObject pdfObject = new PageObject(currentPageOffset);
							this.currentPdfFile.readObject(pdfObject);
							PdfObject Resources = pdfObject.getDictionary(PdfDictionary.Resources);

							if (pdfObject != null) {

								ObjectStore backgroundObjectStoreRef = new ObjectStore(
										(ImageHelper) this.externalHandlers.getExternalHandler(Options.ImageLibrary));

								PdfStreamDecoder backgroundDecoder = new PdfStreamDecoder(this.currentPdfFile);
								backgroundDecoder.setParameters(true, false, 0, this.extractionMode);

								backgroundDecoder.setXMLExtraction(this.options.isXMLExtraction());
								this.externalHandlers.addHandlers(backgroundDecoder);

								backgroundDecoder.setObjectValue(ValueTypes.Name, this.filename);

								backgroundDecoder.setObjectValue(ValueTypes.ObjectStore, backgroundObjectStoreRef);
								backgroundDecoder.setObjectValue(ValueTypes.PDFPageData, this.pageData);
								backgroundDecoder.setIntValue(ValueTypes.PageNum, i);

								this.res.setupResources(backgroundDecoder, false, Resources, this.pageNumber, this.currentPdfFile);

								backgroundDecoder.decodePageContent(pdfObject);

								// get extracted data
								this.pdfBackgroundData = (PdfData) backgroundDecoder.getObjectValue(ValueTypes.PDFData);
								this.pdfBackgroundImages = (PdfImageData) backgroundDecoder.getObjectValue(ValueTypes.PDFImages);

							}
						}
					}

				}
				catch (Exception e) {
					// tell user and log
					if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
				}
				finally {
					this.isBackgroundDecoding = false;
				}
			}
	}

	// <end-adobe>

	/**
	 * get page count of current PDF file
	 */
	final public int getPageCount() {
		return this.pageCount;
	}

	/**
	 * return true if the current pdf file is encrypted <br>
	 * check <b>isFileViewable()</b>,<br>
	 * <br>
	 * if file is encrypted and not viewable - a user specified password is needed.
	 */
	final public boolean isEncrypted() {

		if (this.currentPdfFile != null) {
			PdfFileReader objectReader = this.currentPdfFile.getObjectReader();
			DecryptionFactory decryption = objectReader.getDecryptionObject();
			return decryption != null && decryption.getBooleanValue(PDFflags.IS_FILE_ENCRYPTED);
		}
		else return false;
	}

	// <start-demo>
	/**
	 * //<end-demo>
	 * 
	 * 
	 * 
	 * 
	 * /** show if encryption password has been supplied or set a certificate
	 */
	final public boolean isPasswordSupplied() {

		FileAccess fileAccess = (FileAccess) getJPedalObject(PdfDictionary.FileAccess);
		return fileAccess.isPasswordSupplied(this.currentPdfFile);
	}

	/**
	 * show if encrypted file can be viewed,<br>
	 * if false a password needs entering
	 */
	public boolean isFileViewable() {

		FileAccess fileAccess = (FileAccess) getJPedalObject(PdfDictionary.FileAccess);
		return fileAccess.isFileViewable(this.currentPdfFile);
	}

	/**
	 * show if content can be extracted
	 */
	public boolean isExtractionAllowed() {

		if (this.currentPdfFile != null) {

			PdfFileReader objectReader = this.currentPdfFile.getObjectReader();

			DecryptionFactory decryption = objectReader.getDecryptionObject();
			return decryption == null || decryption.getBooleanValue(PDFflags.IS_EXTRACTION_ALLOWED);

		}
		else return false;
	}

	/**
	 * used to retest access and see if entered password is valid,<br>
	 * If so file info read and isFileViewable will return true
	 */
	private void verifyAccess() {
		if (this.currentPdfFile != null) {
			try {
				openPdfFile();
			}
			catch (Exception e) {
				if (LogWriter.isOutput()) LogWriter.writeLog("Exception " + e + " opening file");
			}
		}
	}

	/**
	 * set a password for encryption - software will resolve if user or owner password- calls verifyAccess() from 2.74 so no separate call needed
	 */
	final public void setEncryptionPassword(String password) throws PdfException {

		if (this.currentPdfFile == null) throw new PdfException("Must open PdfDecoder file first");

		this.currentPdfFile.getObjectReader().setPassword(password);

		verifyAccess();
	}

	/**
	 * routine to open a byte stream containing the PDF file and extract key info from pdf file so we can decode any pages. Does not actually decode
	 * the pages themselves - By default files over 16384 bytes are cached to disk but this can be altered by setting
	 * PdfFileReader.alwaysCacheInMemory to a maximimum size or -1 (always keep in memory)
	 * 
	 */
	final public void openPdfArray(byte[] data) throws PdfException {

		if (data == null) throw new RuntimeException("Attempting to open null byte stream");

		if (this.isOpen)
		// throw new RuntimeException("Previous file not closed");
		closePdfFile(); // also checks decoding done

		this.isOpen = false;

		this.res.flush();

		try {

			this.currentPdfFile = new PdfReader();

			/** get reader object to open the file */
			this.currentPdfFile.openPdfFile(data);

			openPdfFile();

			/** store file name for use elsewhere as part of ref key without .pdf */
			this.objectStoreRef.storeFileName("r" + System.currentTimeMillis());

		}
		catch (Exception e) {
			throw new PdfException("[PDF] OpenPdfArray generated exception " + e.getMessage());
		}
	}

	/**
	 * allow user to open file using Certificate and key
	 * 
	 * @param filename
	 * @param certificate
	 * @param key
	 */
	public void openPdfFile(String filename, Certificate certificate, PrivateKey key) throws PdfException {

		/**
		 * set values and then call generic open
		 */
		this.fileAcces.setUserEncryption(certificate, key);

		openPdfFile(filename);
	}

	/**
	 * routine to open PDF file and extract key info from pdf file so we can decode any pages which also sets password. Does not actually decode the
	 * pages themselves. Also reads the form data. You must explicitly close your stream!!
	 */
	final public void openPdfFileFromStream(Object filename, String password) throws PdfException {

		// tell JPedal NOT to close stream!!!
		this.closeOnExit = false;

		if (filename instanceof ImageInputStream) {

			ImageInputStream iis = (ImageInputStream) filename;

			if (this.isOpen)
			// throw new RuntimeException("Previous file not closed");
			closePdfFile(); // also checks decoding done

			this.isOpen = false;

			this.displayScaling = null;

			this.filename = "ImageInputStream" + System.currentTimeMillis();

			this.res.flush();

			/** store file name for use elsewhere as part of ref key without .pdf */
			this.objectStoreRef.storeFileName(this.filename);

			this.currentPdfFile = new PdfReader(password);

			/** get reader object to open the file */
			this.currentPdfFile.openPdfFile(iis);

			openPdfFile();
		}
		else {
			throw new RuntimeException(filename + " not currently an option");
		}
	}

	/**
	 * routine to open PDF file and extract key info from pdf file so we can decode any pages. Does not actually decode the pages themselves. Also
	 * reads the form data. You must explicitly close any open files with closePdfFile() to Java will not release all the memory
	 */
	final public void openPdfFile(final String filename) throws PdfException {

		if (this.isOpen && this.linearParser.linearizedBackgroundReaderer == null)
		// throw new RuntimeException("Previous file not closed");
		closePdfFile(); // also checks decoding done

		this.isOpen = false;

		this.displayScaling = null;

		// System.out.println(filename);

		this.filename = filename;
		this.res.flush();
		// pagesReferences.clear();

		/** store file name for use elsewhere as part of ref key without .pdf */
		this.objectStoreRef.storeFileName(filename);

		/**
		 * create Reader, passing in certificate if set
		 */
		this.currentPdfFile = this.fileAcces.getNewReader();

		/** get reader object to open the file */
		this.currentPdfFile.openPdfFile(filename);

		/**
		 * test code in case we need to test byte[] version //get size try{ File file=new File(filename); int length= (int) file.length(); byte[]
		 * fileData=new byte[length]; FileInputStream fis=new FileInputStream(filename); fis.read(fileData); fis.close();
		 * currentPdfFile.openPdfFile(fileData); }catch(Exception e){
		 * 
		 * }/
		 **/

		openPdfFile();
	}

	/**
	 * routine to open PDF file and extract key info from pdf file so we can decode any pages which also sets password. Does not actually decode the
	 * pages themselves. Also reads the form data. You must explicitly close any open files with closePdfFile() or Java will not release all the
	 * memory
	 */
	final public void openPdfFile(final String filename, String password) throws PdfException {

		if (this.isOpen)
		// throw new RuntimeException("Previous file not closed");
		closePdfFile(); // also checks decoding done

		this.isOpen = false;

		this.displayScaling = null;

		this.filename = filename;
		this.res.flush();

		/** store file name for use elsewhere as part of ref key without .pdf */
		this.objectStoreRef.storeFileName(filename);

		this.currentPdfFile = new PdfReader(password);

		/** get reader object to open the file */
		this.currentPdfFile.openPdfFile(filename);

		openPdfFile();
	}

	/**
	 * routine to open PDF file via URL and extract key info from pdf file so we can decode any pages - Does not actually decode the pages themselves
	 * - Also reads the form data - Based on an idea by Peter Jacobsen <br />
	 * You must explicitly close any open files with closePdfFile() so Java will release all the memory <br />
	 * 
	 * If boolean supportLinearized is true, method will return with true value once Linearized part read
	 */
	final public boolean openPdfFileFromURL(String pdfUrl, boolean supportLinearized) throws PdfException {

		InputStream is = null;

		String rawFileName = null;

		try {
			URL url;
			url = new URL(pdfUrl);
			rawFileName = url.getPath().substring(url.getPath().lastIndexOf('/') + 1);

			is = url.openStream();
		}
		catch (Exception e) {
			// tell user and log
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
		}

		return readFile(supportLinearized, is, rawFileName, null);
	}

	/**
	 * routine to open PDF file via URL and extract key info from pdf file so we can decode any pages - Does not actually decode the pages themselves
	 * - Also reads the form data - Based on an idea by Peter Jacobsen <br />
	 * You must explicitly close any open files with closePdfFile() so Java will release all the memory <br />
	 * 
	 * If boolean supportLinearized is true, method will return with true value once Linearized part read
	 */
	final public boolean openPdfFileFromURL(String pdfUrl, boolean supportLinearized, String password) throws PdfException {

		InputStream is = null;

		String rawFileName = null;

		try {
			URL url;
			url = new URL(pdfUrl);
			rawFileName = url.getPath().substring(url.getPath().lastIndexOf('/') + 1);

			is = url.openStream();
		}
		catch (Exception e) {
			// tell user and log
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
		}

		return readFile(supportLinearized, is, rawFileName, password);
	}

	/**
	 * routine to open PDF file via InputStream and extract key info from pdf file so we can decode any pages - Does not actually decode the pages
	 * themselves -
	 * <p/>
	 * You must explicitly close any open files with closePdfFile() to Java will not release all the memory
	 * 
	 * IMPORTANT NOTE: If the stream does not contain enough bytes, test for Linearization may fail If boolean supportLinearized is true, method will
	 * return with true value once Linearized part read (we recommend use you false unless you know exactly what you are doing)
	 */
	final public boolean openPdfFileFromInputStream(InputStream is, boolean supportLinearized) throws PdfException {

		String rawFileName = "inputstream" + System.currentTimeMillis() + '-' + this.objectStoreRef.getKey() + ".pdf";

		// filename=rawFileName;

		// make sure it will be deleted
		this.objectStoreRef.setFileToDeleteOnFlush(ObjectStore.temp_dir + rawFileName);
		this.objectStoreRef.setFileToDeleteOnFlush(rawFileName);

		return readFile(supportLinearized, is, rawFileName, null);
	}

	/**
	 * routine to open PDF file via InputStream and extract key info from pdf file so we can decode any pages - Does not actually decode the pages
	 * themselves -
	 * <p/>
	 * You must explicitly close any open files with closePdfFile() to Java will not release all the memory
	 * 
	 * IMPORTANT NOTE: If the stream does not contain enough bytes, test for Linearization may fail If boolean supportLinearized is true, method will
	 * return with true value once Linearized part read (we recommend use you false unless you know exactly what you are doing)
	 */
	final public boolean openPdfFileFromInputStream(InputStream is, boolean supportLinearized, String password) throws PdfException {

		String rawFileName = "inputstream" + System.currentTimeMillis() + '-' + this.objectStoreRef.getKey() + ".pdf";

		// make sure it will be deleted
		this.objectStoreRef.setFileToDeleteOnFlush(ObjectStore.temp_dir + rawFileName);
		this.objectStoreRef.setFileToDeleteOnFlush(rawFileName);

		return readFile(supportLinearized, is, rawFileName, password);
	}

	/**
	 * common code for reading URL and InputStream
	 * 
	 * @param supportLinearized
	 * @param is
	 * @param rawFileName

	 * @throws PdfException
	 */
	private boolean readFile(boolean supportLinearized, InputStream is, String rawFileName, String password) throws PdfException {

		this.displayScaling = null;
		this.res.flush();

		if (password == null) {
			this.currentPdfFile = new PdfReader();
		}
		else {
			this.currentPdfFile = new PdfReader(password);
		}

		if (is != null) {
			try {

				File tempURLFile;
				if (rawFileName.startsWith("inputstream")) {
					tempURLFile = new File(ObjectStore.temp_dir + rawFileName);
					this.filename = tempURLFile.getAbsolutePath();
				}
				else tempURLFile = ObjectStore.createTempFile(rawFileName);

				/** store fi name for use elsewhere as part of ref key without .pdf */
                int separatorIndex = tempURLFile.getName().lastIndexOf('.');
                String storeFilename;
                if (separatorIndex >= 0) {
                    storeFilename = tempURLFile.getName().substring(0, tempURLFile.getName().lastIndexOf('.'));
                } else {
                    storeFilename = tempURLFile.getName();
                }
                this.objectStoreRef.storeFileName(storeFilename);

				if (supportLinearized) {

					byte[] linearBytes = this.linearParser.readLinearData(this.currentPdfFile, tempURLFile, is, this);

					if (linearBytes != null) {

						/**
						 * read partial data so we can display
						 */

						this.currentPdfFile.openPdfFile(linearBytes);
						openPdfFile();

						// read rest of file and reset
						this.linearParser.linearizedBackgroundReaderer.start();

						return true;
					}

				}
				else {

					this.currentPdfFile.openPdfFile(is);
					openPdfFile();
				}

				if (supportLinearized) {
					/* get reader object to open the file */
					openPdfFile(tempURLFile.getAbsolutePath());

                    /* store fi name for use elsewhere as part of ref key without .pdf */
                    separatorIndex = tempURLFile.getName().lastIndexOf('.');
                    if (separatorIndex >= 0) {
                        storeFilename = tempURLFile.getName().substring(0, tempURLFile.getName().lastIndexOf('.'));
                    } 
                    else {
                        storeFilename = tempURLFile.getName();
                    }
                    this.objectStoreRef.storeFileName(storeFilename);
				}

			}
			catch (IOException e) {
				if (LogWriter.isOutput()) LogWriter.writeLog("[PDF] Exception " + e + " opening URL ");

				e.printStackTrace();
			}
		}
		return false;
	}

	/**
	 * common code to all open routines
	 */
	private synchronized void openPdfFile() throws PdfException {

		// <start-demo>
		/**
		 * //<end-demo> /
		 **/

		this.pageNumber = 1; // reset page number for metadata

		// force redraw
		this.swingPainter.forceRedraw();

		try {
			this.isDecoding = true;

			this.pages.resetCachedValues();

			// <start-adobe>
			// remove listener if not removed by close
			if (this.viewListener != null) {

				// flush any cached pages
				this.pages.flushPageCaches();

				removeComponentListener(this.viewListener);

				this.viewListener.dispose();
				this.viewListener = null;

			}
			// <end-adobe>

			// set cache size to use

			this.currentPdfFile.getObjectReader().setCacheSize(this.minimumCacheSize);

			// reset printing
			this.swingPrinter.lastPrintedPage = -1;
			this.swingPrinter.currentPrintDecoder = null;

			if (this.formRenderer != null) {
				this.formRenderer.getCompData().setRootDisplayComponent(this);

			}
			// invalidate();

			// reset page data - needed to flush crop settings
			this.pageData = new PdfPageData();
			// read and log the version number of pdf used
			String pdfVersion = this.currentPdfFile.getObjectReader().getType();

			if (LogWriter.isOutput()) LogWriter.writeLog("Pdf version : " + pdfVersion);

			if (pdfVersion == null) {
				this.currentPdfFile = null;
				this.isDecoding = false;

				throw new PdfException("No version on first line ");

			}

			// read reference table so we can find all objects and say if
			// encrypted
			PdfObject pdfObject = null;

			int linearPageCount = -1;

			// linear page object set differently
			if (this.linearParser.hasLinearData()) {

				/**
				 * read and decode the hints table and the ref table
				 */
				pdfObject = this.linearParser.readHintTable(pdfObject, this.currentPdfFile);

				linearPageCount = this.linearParser.getPageCount();

			}
			else // <end-gpl>
			pdfObject = this.currentPdfFile.getObjectReader().readReferenceTable(null);

			// new load code - be more judicious in how far down tree we scan
			final boolean ignoreRecursion = true;

			// open if not encrypted or has password
			if (!isEncrypted() || isPasswordSupplied()) {

				if (pdfObject != null) {
					pdfObject.ignoreRecursion(ignoreRecursion);

					this.res.setValues(pdfObject, this.currentPdfFile);

					// check read as may be used for Dest
					PdfObject nameObj = pdfObject.getDictionary(PdfDictionary.Names);
					if (nameObj != null) {
						this.currentPdfFile.readNames(nameObj, this.options.getJS(), false);
					}
				}

				int type = pdfObject.getParameterConstant(PdfDictionary.Type);
				if (type != PdfDictionary.Page) {

					PdfObject pageObj = pdfObject.getDictionary(PdfDictionary.Pages);
					if (pageObj != null) { // do this way incase in separate compressed stream

						pdfObject = new PageObject(pageObj.getObjectRefAsString());
						this.currentPdfFile.readObject(pdfObject);

						// System.out.println("page="+pageObj+" "+pageObj.getObjectRefAsString());
						// catch for odd files
						if (pdfObject.getParameterConstant(PdfDictionary.Type) == -1) pdfObject = pageObj;

						// System.out.println("test code called");
					}
				}

				if (pdfObject != null) {

					if (LogWriter.isOutput()) LogWriter.writeLog("Pages being read from " + pdfObject + ' ' + pdfObject.getObjectRefAsString());

					this.pageNumber = 1; // reset page number for metadata
					// reset lookup table
					// pageLookup = new PageLookup();

					// flush annots before we reread

					if (this.formRenderer != null) this.formRenderer.resetAnnotData(this.insetW, this.insetH, this.pageData, 1, this.currentPdfFile,
							null);

					// recursively read all pages
					int tempPageCount = readAllPageReferences(ignoreRecursion, pdfObject, new HashMap(1000), new HashMap(1000), 1);

					// set PageCount if in Linearized data
					if (linearPageCount > 0) {
						this.pageCount = linearPageCount;
					}
					else {
						this.pageCount = tempPageCount - 1; // save page count
					}

					this.pageNumber = 0; // reset page number for metadata;
					if (this.pageCount == 0 && LogWriter.isOutput()) LogWriter.writeLog("No pages found");
				}

				// pass handle into renderer
				if (this.formRenderer != null) {
					this.formRenderer.openFile(this.pageCount);

					this.formRenderer.resetFormData(this.insetW, this.insetH, this.pageData, this.currentPdfFile,
							this.res.getPdfObject(PdfResources.AcroFormObj));
				}
			}

			this.currentOffset = null;

			// reset so if continuous view mode set it will be recalculated for page
			this.pages.disableScreen();
			this.pages.stopGeneratingPage();

			// force back if only 1 page
			if (this.pageCount < 2) this.displayView = Display.SINGLE_PAGE;
			else this.displayView = this.options.getPageMode();

			// <start-adobe>
			setDisplayView(this.displayView, this.alignment); // force reset and add back listener
			/**
			 * //<end-adobe> if (currentOffset == null) currentOffset = new PageOffsets(pageCount, pageData); /
			 **/

			this.isOpen = true;
		}
		catch (PdfException e) {

			// ensure all data structures/handles flushed
			this.isDecoding = false;
			this.isOpen = true; // temporarily set to true as otherwise will not work
			closePdfFile();

			this.isOpen = false;

			this.isDecoding = false;
			throw new PdfException(e.getMessage() + " opening file");
		}

		this.isDecoding = false;
	}

	/**
	 * @return will return some dictionary values - if not a set value, will return null
	 * 
	 */
	public Object getJPedalObject(int id) {
		switch (id) {
			case PdfDictionary.Layer:
				return this.res.getPdfLayerList();

			case PdfDictionary.Linearized:

				return this.linearParser.getLinearObject(this.isOpen, this.currentPdfFile);

			case PdfDictionary.LinearizedReader:

				return this.linearParser.linearizedBackgroundReaderer;

			case PdfDictionary.FileAccess:

				return this.fileAcces;

			default:
				return null;
		}
	}

	public void setPageMode(int mode) {
		this.options.setPageMode(mode);
	}

	/**
	 * read the data from pages lists and pages so we can open each page.
	 * 
	 * object reference to first trailer
	 */
	private int readAllPageReferences(boolean ignoreRecursion, PdfObject pdfObject, Map rotations, Map parents, int tempPageCount) {

		String currentPageOffset = pdfObject.getObjectRefAsString();

		final boolean debug = false;

		int rotation = 0;

		int type = pdfObject.getParameterConstant(PdfDictionary.Type);

		if (debug) System.out.println("currentPageOffset=" + currentPageOffset + " type=" + type + ' ' + PdfDictionary.showAsConstant(type));

		if (type == PdfDictionary.Unknown) type = PdfDictionary.Pages;

		/**
		 * handle common values which can occur at page level or higher
		 */

		/** page rotation for this or up tree */
		int rawRotation = pdfObject.getInt(PdfDictionary.Rotate);
		String parent = pdfObject.getStringKey(PdfDictionary.Parent);

		if (rawRotation == -1) {

			while (parent != null && rawRotation == -1) {

				if (parent != null) {
					Object savedRotation = rotations.get(parent);
					if (savedRotation != null) rawRotation = (Integer) savedRotation;
				}

				if (rawRotation == -1) parent = (String) parents.get(parent);

			}

			// save
			if (rawRotation != -1) {
				rotations.put(currentPageOffset, rawRotation);
				parents.put(currentPageOffset, parent);
			}
		}
		else { // save so we can lookup
			rotations.put(currentPageOffset, rawRotation);
			parents.put(currentPageOffset, parent);
		}

		if (rawRotation != -1) rotation = rawRotation;

		this.pageData.setPageRotation(rotation, tempPageCount);

		/**
		 * handle media and crop box, defaulting to higher value if needed (ie Page uses Pages and setting crop box
		 */
		float[] mediaBox = pdfObject.getFloatArray(PdfDictionary.MediaBox);
		float[] cropBox = pdfObject.getFloatArray(PdfDictionary.CropBox);

		if (mediaBox != null) this.pageData.setMediaBox(mediaBox);

		if (cropBox != null) this.pageData.setCropBox(cropBox);

		/** process page to read next level down */
		if (type == PdfDictionary.Pages) {

			this.res.setPdfObject(PdfResources.GlobalResources, pdfObject.getDictionary(PdfDictionary.Resources));

			byte[][] kidList = pdfObject.getKeyArray(PdfDictionary.Kids);

			int kidCount = 0;
			if (kidList != null) kidCount = kidList.length;

			if (debug) System.out.println("PAGES---------------------currentPageOffset=" + currentPageOffset + " kidCount=" + kidCount);

			/** allow for empty value and put next pages in the queue */
			if (kidCount > 0) {

				if (debug) System.out.println("KIDS---------------------currentPageOffset=" + currentPageOffset);

				PdfObject nextObject;
				for (int ii = 0; ii < kidCount; ii++) {

					nextObject = new PageObject(new String(kidList[ii]));
					nextObject.ignoreRecursion(ignoreRecursion);
					nextObject.ignoreStream(true);

					this.currentPdfFile.readObject(nextObject);
					tempPageCount = readAllPageReferences(ignoreRecursion, nextObject, rotations, parents, tempPageCount);
				}

			}

		}
		else
			if (type == PdfDictionary.Page) {

				if (debug) System.out.println("PAGE---------------------currentPageOffset=" + currentPageOffset);

				// store ref for later
				this.currentPdfFile.setLookup(currentPageOffset, tempPageCount);

				this.pageData.checkSizeSet(tempPageCount); // make sure we have min values

				/**
				 * add Annotations
				 */
				if (this.formRenderer != null) {

					// read the annotations reference for the page we have found lots of issues with annotations so trap errors
					byte[][] annotList = pdfObject.getKeyArray(PdfDictionary.Annots);

					// allow for empty
					if (annotList != null && annotList.length == 1 && annotList[0] == null) annotList = null;

					if (annotList != null) {

						// pass handle into renderer
						this.formRenderer.resetAnnotData(this.insetW, this.insetH, this.pageData, tempPageCount, this.currentPdfFile, annotList);
					}
				}

				tempPageCount++;
			}

		return tempPageCount;
	}

	// <end-canoo><end-os>

	/**
	 * shows if text extraction is XML or pure text
	 */
	public boolean isXMLExtraction() {
		return this.options.isXMLExtraction();
	}

	/**
	 * XML extraction is the default - pure text extraction is much faster
	 */
	public void useTextExtraction() {

		this.options.setXMLExtraction(false);
	}

	/**
	 * XML extraction is the default - pure text extraction is much faster
	 */
	public void useXMLExtraction() {

		this.options.setXMLExtraction(true);
	}

	/**
	 * remove all displayed objects for JPanel display (wipes current page)
	 */
	public void clearScreen() {
		this.currentDisplay.flush();
		this.pages.refreshDisplay();
	}

	/**
	 * allows user to cache large objects to disk to avoid memory issues, setting minimum size in bytes (of uncompressed stream) above which object
	 * will be stored on disk if possible (default is -1 bytes which is all objects stored in memory) - Must be set before file opened.
	 * 
	 */
	public void setStreamCacheSize(int size) {
		this.minimumCacheSize = size;
	}

	/**
	 * shows if embedded fonts present on page just decoded
	 */
	public boolean hasEmbeddedFonts() {
		return this.resultsFromDecode.hasEmbeddedFonts();
	}

	/**
	 * shows if whole document contains embedded fonts and uses them
	 */
	final public boolean PDFContainsEmbeddedFonts() throws PdfException {

		boolean hasEmbeddedFonts = false;
		PdfStreamDecoder current = new PdfStreamDecoder(this.currentPdfFile);
		PdfObject pdfObject;

		/**
		 * scan all pages
		 */
		for (int page = 1; page < this.pageCount + 1; page++) {

			/** get pdf object id for page to decode */
			String currentPageOffset = this.currentPdfFile.getReferenceforPage(page);

			/**
			 * decode the file if not already decoded, there is a valid object id and it is unencrypted
			 */
			if (currentPageOffset != null) {

				/** read page or next pages */
				pdfObject = new PageObject(currentPageOffset);
				pdfObject.ignoreStream(true);
				this.currentPdfFile.readObject(pdfObject);

				/** get information for the page */
				byte[][] pageContents = pdfObject.getKeyArray(PdfDictionary.Contents);

				if (pageContents != null) {

					// current.setParameters(true, false, renderMode, extractionMode);

					// externalHandlers.addHandlers(current);

					// current.setObjectValue(ValueTypes.PDFPageData,pageData);
					// current.setIntValue(ValueTypes.PageNum, page);
					// current.setObjectValue(ValueTypes.DynamicVectorRenderer,currentDisplay);

					/** read the resources for the page */
					this.res.setupResources(current, false, pdfObject.getDictionary(PdfDictionary.Resources), this.pageNumber, this.currentPdfFile);

					hasEmbeddedFonts = current.getBooleanValue(ValueTypes.EmbeddedFonts);

					// exit on first true
					if (hasEmbeddedFonts) page = this.pageCount;
				}
			}
		}

		return hasEmbeddedFonts;
	}

	/**
	 * given a ref, what is the page
	 * 
	 * @param ref
	 *            - PDF object reference
	 * @return - page number with being first page
	 */
	public int getPageFromObjectRef(String ref) {
		return this.currentPdfFile.convertObjectToPageNumber(ref);
	}

	/**
	 * Returns list of the fonts used on the current page decoded or null type can be PdfDictionary.Font or PdfDictionary.Image
	 */
	public String getInfo(int type) {

		String returnValue;

		switch (type) {
			case PdfDictionary.Font:

				if (this.fontsInFile == null) {
					returnValue = "No fonts defined";
				}
				else {
					returnValue = this.fontsInFile;
				}

				break;

			case PdfDictionary.Image:

				if (this.imagesInFile == null) {
					returnValue = "No images defined as XObjects";
				}
				else {
					returnValue = this.imagesInFile;
				}

				break;

			default:
				returnValue = null;
		}

		return returnValue;
	}

	/**
	 * used to update statusBar object if exists
	 */
	private class ProgressListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent evt) {

			PdfDecoder.this.statusBar.setProgress((int) (PdfDecoder.this.statusBar.percentageDone));
		}
	}

	/**
	 * Allow user to access Forms renderer - returns null not available (should not generally be needed)
	 */
	public AcroRenderer getFormRenderer() {
		return this.formRenderer;
	}

	/**
	 * shows if page reported any errors while printing. Log can be found with getPageFailureMessage()
	 * 
	 * @return Returns the printingSuccessful.
	 */
	public boolean isPageSuccessful() {
		return this.swingPrinter.isPageSuccessful();
	}

	/**
	 * return any errors or other messages while calling decodePage() - zero length is no problems
	 */
	public String getPageDecodeReport() {
		return this.decodeStatus;
	}

	/**
	 * Return String with all error messages from last printed (useful for debugging)
	 */
	public String getPageFailureMessage() {
		return this.swingPrinter.getPageFailureMessage();
	}

	/**
	 * If running in GUI mode, will extract a section of rendered page as BufferedImage -coordinates are PDF co-ordinates. If you wish to use hires
	 * image, you will need to enable hires image display with decode_pdf.useHiResScreenDisplay(true);
	 * 
	 * @param t_x1
	 * @param t_y1
	 * @param t_x2
	 * @param t_y2
	 * @param scaling
	 * @return pageErrorMessages - Any printer errors
	 */
	public BufferedImage getSelectedRectangleOnscreen(float t_x1, float t_y1, float t_x2, float t_y2, float scaling) {

		BufferedImage img = SwingPainter.getSelectedRectangleOnscreen(t_x1, t_y1, t_x2, t_y2, scaling, this.pageNumber, this.pageData,
				this.formRenderer, this.currentDisplay, this.currentPdfFile);

		this.formRenderer.getCompData().resetScaledLocation(this.pages.getOldScaling(), this.displayRotation, 0);

		return img;
	}

	/**
	 * return object which provides access to file images and name
	 */
	public ObjectStore getObjectStore() {
		return this.objectStoreRef;
	}

	/**
	 * return object which provides access to file images and name (use not recommended)
	 */
	public void setObjectStore(ObjectStore newStore) {
		this.objectStoreRef = newStore;
	}

	/**
	 * Return decoder options as object for cases where value is needed externally and can't be static
	 * 
	 * @return DecoderOptions object containing settings for this PdfDecoder object
	 */
	public DecoderOptions getDecoderOptions() {
		return this.options;
	}

	// <start-adobe>
	/**
	 * returns object containing grouped text of last decoded page - if no page decoded, a Runtime exception is thrown to warn user Please see
	 * org.jpedal.examples.text for example code.
	 * 
	 */
	public PdfGroupingAlgorithms getGroupingObject() throws PdfException {
		return this.options.getGroupingObject(this.lastPageDecoded, getPdfData(), this.pageData);
	}

	/**
	 * returns object containing grouped text from background grouping - Please see org.jpedal.examples.text for example code
	 */
	public PdfGroupingAlgorithms getBackgroundGroupingObject() {
		return this.options.getBackgroundGroupingObject(this.pdfBackgroundData, this.pageData);
	}

	// <end-adobe>

	/**
	 * get PDF version in file
	 */
	final public String getPDFVersion() {

		if (this.currentPdfFile == null) return "";
		else return this.currentPdfFile.getObjectReader().getType();
	}

	// <start-adobe>

	/**
	 * used for non-PDF files to reset page
	 */
	public void resetForNonPDFPage(int pageCount) {

		this.displayScaling = null;

		/** set hires mode or not for display */
		this.currentDisplay.setHiResImageForDisplayMode(false);

		this.fontsInFile = "";
		this.pageCount = pageCount;

		if (this.formRenderer != null) this.formRenderer.removeDisplayComponentsFromScreen();

		// reset page data
		this.pageData = new PdfPageData();
	}

	/**
	 * set view mode used in panel and redraw in new mode SINGLE_PAGE,CONTINUOUS,FACING,CONTINUOUS_FACING delay is the time in milli-seconds which
	 * scrolling can stop before background page drawing starts Multipage views not in OS releases
	 */
	public void setDisplayView(int displayView, final int orientation) {
		//
		// //ensure method is correctly accessed
		// if (SwingUtilities.isEventDispatchThread()) {
		//
		// setDisplayViewx(displayView, orientation);
		//
		// } else {
		// final Runnable doPaintComponent = new Runnable() {
		//
		// public void run() {
		// setDisplayViewx(displayView,orientation);
		// }
		// };
		// SwingUtilities.invokeLater(doPaintComponent);
		// }
		// }
		//
		// private void setDisplayViewx(int displayView, int orientation) {

		this.alignment = orientation;

		if (this.pages != null) this.pages.stopGeneratingPage();

		boolean needsReset = (displayView != Display.SINGLE_PAGE || this.displayView != Display.SINGLE_PAGE);
		if (needsReset && (this.displayView == Display.FACING || displayView == Display.FACING)) needsReset = false;

		if (displayView != Display.SINGLE_PAGE) needsReset = true;

		boolean hasChanged = displayView != this.displayView;

		// log what we are changing from
		int lastDisplayView = this.displayView;

		this.displayView = displayView;

		// <start-thin>
		switch (displayView) {
			case Display.SINGLE_PAGE:
				if (this.pages == null || hasChanged) {
					this.pages = new SingleDisplay(this.pageNumber, this.pageCount, this.currentDisplay);
				}
				break;

		}

		// <end-thin>

		// remove listener if setup
		if (this.viewListener != null) {

			removeComponentListener(this.viewListener);

			this.viewListener.dispose();
			this.viewListener = null;
		}

		/**
		 * setup once per page getting all page sizes and working out settings for views
		 */
		if (this.currentOffset == null) this.currentOffset = new PageOffsets(this.pageCount, this.pageData);

		this.pages.setup(this.useAcceleration, this.currentOffset, this);
		this.pages.init(this.scaling, this.pageCount, this.displayRotation, this.pageNumber, this.currentDisplay, true, this.pageData, this.insetW,
				this.insetH);

		// force redraw
		this.swingPainter.forceRedraw();

		this.pages.refreshDisplay();

		// add listener if one not already there
		if (this.viewListener == null) {
			this.viewListener = new RefreshLayout();
			addComponentListener(this.viewListener);
		}

		// move to correct page
		if (this.pageNumber > 0) {
			if (hasChanged && displayView == Display.SINGLE_PAGE) {
				try {
					unsetScaling();
					setPageParameters(this.scaling, this.pageNumber, this.displayRotation);
					invalidate();
					updateUI();
					decodePage(this.pageNumber);
				}
				catch (Exception e) {
					// tell user and log
					if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
				}
			}
			else
				if (displayView != Display.SINGLE_PAGE && displayView != Display.PAGEFLOW) {

					throw new RuntimeException("Only SINGLE_PAGE is available in LGPL release");
					/**/
				}
		}
	}

	// <end-adobe>

	/**
	 * flag to show if we suspect problem with some images
	 */
	public boolean hasAllImages() {
		return this.resultsFromDecode.getImagesProcessedFully();
	}

	public boolean getPageDecodeStatus(int status) {

		/**
		 * if(status.equals(DecodeStatus.PageDecodingSuccessful)) return pageSuccessful; else
		 */
		return this.resultsFromDecode.getPageDecodeStatus(status);
	}

	/**
	 * get page statuses
	 */
	public String getPageDecodeStatusReport(int status) {
		return this.resultsFromDecode.getPageDecodeStatusReport(status);
	}

	/**
	 * set print mode (Matches Abodes Auto Print and rotate output
	 */
	public void setPrintAutoRotateAndCenter(boolean value) {
		this.swingPrinter.isPrintAutoRotateAndCenter = value;
	}

	public void setPrintCurrentView(boolean value) {
		this.swingPrinter.printOnlyVisible = value;
	}

	/**
	 * allows external helper classes to be added to JPedal to alter default functionality - not part of the API and should be used in conjunction
	 * with IDRsolutions only <br>
	 * if Options.FormsActionHandler is the type then the <b>newHandler</b> should be of the form <b>org.jpedal.objects.acroforms.ActionHandler</b>
	 * 
	 * @param newHandler
	 * @param type
	 */
	public void addExternalHandler(Object newHandler, int type) {

		switch (type) {

			case Options.FormFactory:
				this.formRenderer.setFormFactory((FormFactory) newHandler);
				break;

			case Options.MultiPageUpdate:
				this.customSwingHandle = newHandler;
				break;

			// case Options.LinkHandler:
			//
			// if (formRenderer != null)
			// formRenderer.resetHandler(newHandler, this,Options.LinkHandler);
			//
			// break;

			case Options.FormsActionHandler:

				if (this.formRenderer != null) this.formRenderer.resetHandler(newHandler, this, Options.FormsActionHandler);

				break;

			// <start-thin><start-adobe>
			case Options.SwingMouseHandler:
				if (this.formRenderer != null) {
					this.formRenderer.getActionHandler().setMouseHandler((SwingMouseListener) newHandler);
				}
				break;

			case Options.CustomMouseHandler:
				SwingMouseListener.setCustomMouseFunctions((SwingMouseFunctionality) newHandler);
				break;

			case Options.ThumbnailHandler:
				this.pages.setThumbnailPanel((org.jpedal.examples.viewer.gui.generic.GUIThumbnailPanel) newHandler);
				break;
			// <end-adobe><end-thin>

			default:
				this.externalHandlers.addExternalHandler(newHandler, type);

		}
	}

	/**
	 * allows external helper classes to be accessed if needed - also allows user to access SwingGUI if running full Viewer package - not all Options
	 * available to get - please contact IDRsolutions if you are looking to use
	 * 
	 * @param type
	 */
	public Object getExternalHandler(int type) {

		switch (type) {

			case Options.FormFactory:
				return this.formRenderer.getFormFactory();

			case Options.MultiPageUpdate:
				return this.customSwingHandle;

			case Options.Display:
				return this.pages;

			case Options.CurrentOffset:
				return this.currentOffset;

			default:
				return this.externalHandlers.getExternalHandler(type);

		}
	}

	public void unsetScaling() {

		this.displayScaling = null;
	}

	public PdfObjectReader getIO() {
		return this.currentPdfFile;
	}

	public String getFileName() {
		return this.filename;
	}

	public boolean isForm() {
		return this.res.isForm();
	}

	/**
	 * reference for Page object
	 * 
	 * @param page
	 * @return String ref (ie 1 0 R) pdfObject=new PageObject(currentPageOffset); currentPdfFile.readObject(pdfObject);
	 */
	public String getReferenceforPage(int page) {
		return this.currentPdfFile.getReferenceforPage(page);
	}

	/**
	 * allow printing of different sizes pages (default is false as PrintJob does not support different page sizes whereas DocPrintJob does)
	 * 
	 * @param allowDifferentPrintPageSizes
	 */
	public void setAllowDifferentPrintPageSizes(boolean allowDifferentPrintPageSizes) {
		this.swingPrinter.allowDifferentPrintPageSizes = allowDifferentPrintPageSizes;
	}

	/** holds lines of text we create */
	private org.jpedal.text.TextLines textLines = new TextLines();

	/**
	 * 
	 * access textlines object
	 */
	public TextLines getTextLines() {
		return this.textLines;
	}

	// <start-adobe>

	/**
	 * set an inset display so that display will not touch edge of panel
	 */
	final public void setInset(int width, int height) {
		this.insetW = width;
		this.insetH = height;
	}

	/**
	 * make screen scroll to ensure point is visible
	 */
	public void ensurePointIsVisible(Point p) {
		super.scrollRectToVisible(new Rectangle(p.x, this.y_size - p.y, this.scrollInterval, this.scrollInterval));
	}

	// <end-adobe>

	/**
	 * set a left margin for printing pages (ie for duplex)
	 * 
	 * @param oddPages
	 */
	public void setPrintIndent(int oddPages, int evenPages) {

		this.swingPrinter.setPrintIndent(oddPages, evenPages);
	}

	/**
	 * allow user to 'move' display of PDF
	 * 
	 * mode is a Constant in org.jpedal.external.OffsetOptions (ie OffsetOptions.SWING_DISPLAY,OffsetOptions.PRINTING)
	 */
	public void setUserOffsets(int x, int y, int mode) {

		this.swingPainter.getDisplayOffsets().setUserOffsets(x, y, mode, this.externalHandlers, this);
	}

	public Point getUserOffsets(int mode) {
		return this.swingPainter.getDisplayOffsets().getUserOffsets(mode);
	}

	/**
	 * get sizes of panel <BR>
	 * This is the PDF pagesize (as set in the PDF from pagesize) - It now includes any scaling factor you have set (ie a PDF size 800 * 600 with a
	 * scaling factor of 2 will return 1600 *1200)
	 */
	@Override
	final public Dimension getMaximumSize() {

		Dimension pageSize = null;

		if (this.displayView != Display.SINGLE_PAGE) pageSize = this.pages.getPageSize(this.displayView);

		if (pageSize == null) {
			if (this.displayRotation == 90 || this.displayRotation == 270) pageSize = new Dimension(this.y_size + this.insetW + this.insetW,
					this.x_size + this.insetH + this.insetH);
			else pageSize = new Dimension(this.x_size + this.insetW + this.insetW, this.y_size + this.insetH + this.insetH);

		}

		if (pageSize == null) pageSize = getMinimumSize();

		return pageSize;
	}

	/**
	 * get width
	 */
	@Override
	final public Dimension getMinimumSize() {
		return new Dimension(100 + this.insetW, 100 + this.insetH);
	}

	/**
	 * get sizes of panel <BR>
	 * This is the PDF pagesize (as set in the PDF from pagesize) - It now includes any scaling factor you have set (ie a PDF size 800 * 600 with a
	 * scaling factor of 2 will return 1600 *1200)
	 */
	@Override
	public Dimension getPreferredSize() {
		return getMaximumSize();
	}

	// <start-adobe>

	/**
	 * update rectangle we draw to highlight an area - See Viewer example for example code showing current usage.
	 */
	final public void updateCursorBoxOnScreen(Rectangle newOutlineRectangle, Color outlineColor) {

		if (this.displayView != Display.SINGLE_PAGE) return;

		this.swingPainter.updateCursorBoxOnScreen(newOutlineRectangle, outlineColor, this, this.pageNumber);
	}

	// <end-adobe>

	@Override
	public void paint(Graphics g) {

		try {

			super.paint(g);

			if (!this.isDecoding) {
				this.swingPainter.drawCursor(g, this.alignment);
			}

		}
		catch (Exception e) {
			// tell user and log
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());

			this.pages.flushPageCaches();

		}
		catch (Error err) { // for tight memory

			this.pages.flushPageCaches();
			this.pages.stopGeneratingPage();

			super.paint(g);

		}
	}

	/** standard method to draw page and any highlights onto JPanel */
	@Override
	public void paintComponent(Graphics g) {

		final RenderChangeListener customRenderChangeListener = (RenderChangeListener) this.externalHandlers
				.getExternalHandler(Options.RenderChangeListener);
		if (customRenderChangeListener != null) // call custom class if present
		customRenderChangeListener.renderingStarted(this.pageNumber);

		super.paintComponent(g);

		if (SwingUtilities.isEventDispatchThread()) {

			threadSafePaint(g);

			if (customRenderChangeListener != null) // call custom class if present
			customRenderChangeListener.renderingEnded(this.pageNumber);

		}
		else {
			final Graphics g2 = g;
			final int page = this.pageNumber;
			final Runnable doPaintComponent = new Runnable() {
				@Override
				public void run() {
					threadSafePaint(g2);

					if (customRenderChangeListener != null) // call custom class if present
					customRenderChangeListener.renderingEnded(page);

				}
			};
			SwingUtilities.invokeLater(doPaintComponent);
		}
	}

	/**
	 * update display
	 */
	synchronized void threadSafePaint(Graphics g) {

		if (this.displayScaling == null) return;

		Graphics2D g2 = (Graphics2D) g;

		if (!this.isDecoding && this.renderPage) {
			this.swingPainter.paintPage(g2, this, this.pages, this.pageData, this.pageNumber, this.currentDisplay, this.displayView,
					this.displayRotation, this.insetW, this.insetH, this.textLines, this.myBorder, this.currentOffset, this.externalHandlers);
		}
		else { // just fill the background
			this.currentDisplay.setG2(g2);
			this.currentDisplay.paintBackground(null);
		}
	}

	/**
	 * get sizes of panel <BR>
	 * This is the PDF pagesize (as set in the PDF from pagesize) - It now includes any scaling factor you have set
	 */
	final public int getPDFWidth() {
		if (this.displayRotation == 90 || this.displayRotation == 270) return this.y_size + this.insetW + this.insetW;
		else return this.x_size + this.insetW + this.insetW;
	}

	/**
	 * get sizes of panel - This is the PDF pagesize
	 */
	final public int getPDFHeight() {
		if ((this.displayRotation == 90 || this.displayRotation == 270)) return this.x_size + this.insetH + this.insetH;
		else return this.y_size + this.insetH + this.insetH;
	}

	/**
	 * set border for screen and print which will be displayed<br>
	 * Setting a new value will enable screen and border painting - disable with disableBorderForPrinting()
	 */
	final public void setPDFBorder(Border newBorder) {
		this.myBorder = newBorder;
	}

	/**
	 * Enables/Disables hardware acceleration of screen rendering in 1.4 (default is on)
	 */
	public void setHardwareAccelerationforScreen(boolean useAcceleration) {
		this.useAcceleration = useAcceleration;
	}

	// <start-adobe>

	/** return amount to scroll window by when scrolling (default is 10) */
	public int getScrollInterval() {
		return this.scrollInterval;
	}

	/** set amount to scroll window by when scrolling */
	public void setScrollInterval(int scrollInterval) {
		this.scrollInterval = scrollInterval;
	}

	/**
	 * returns view mode used - ie SINGLE_PAGE,CONTINUOUS,FACING,CONTINUOUS_FACING (no effect in OS versions)
	 */
	public int getDisplayView() {
		return this.displayView;
	}

	// <end-adobe>

	/**
	 * set page scaling mode to use - default setting is PAGE_SCALING_REDUCE_TO_PRINTER_MARGINS All values start PAGE_SCALING
	 */
	public void setPrintPageScalingMode(int pageScalingMode) {
		this.swingPrinter.setPrintPageScalingMode(pageScalingMode);
	}

	public void setUsePDFPaperSize(boolean usePDFPaperSize) {
		this.swingPrinter.usePDFPaperSize = usePDFPaperSize;
	}

	public float getScaling() {
		return this.scaling;
	}

	public int getInsetH() {
		return this.insetH;
	}

	public int getInsetW() {
		return this.insetW;
	}

	public Rectangle getCursorBoxOnScreen() {
		return this.swingPainter.getCursorBoxOnScreen();
	}

	/**
	 * part of pageable interface - used only in printing Use getPageCount() for number of pages
	 * 
	 * @see java.awt.print.Pageable#getNumberOfPages()
	 */
	public int getNumberOfPages() {
		return this.swingPrinter.getNumberOfPages(this.pageCount);
	}

	/**
	 * part of pageable interface
	 * 
	 * @see java.awt.print.Pageable#getPageFormat(int)
	 */
	public PageFormat getPageFormat(int p) throws IndexOutOfBoundsException {
		return this.swingPrinter.getPageFormat(p, this.pageData, this.pageCount);
	}

	public void setCenterOnScaling(boolean center) {

		this.swingPrinter.setCenterOnScaling(center);
	}

	/**
	 * set pageformat for a specific page - if no pageFormat is set a default will be used. Recommended to use setPageFormat(PageFormat pf)
	 */
	public void setPageFormat(int p, PageFormat pf) {

		this.swingPrinter.putPageFormat(p, pf);
	}

	/**
	 * set pageformat for a specific page - if no pageFormat is set a default will be used.
	 */
	public void setPageFormat(PageFormat pf) {

		this.swingPrinter.putPageFormat("standard", pf);
	}

	/**
	 * set inclusive range to print (see SilentPrint.java and Viewer.java for sample print code (invalid range will throw PdfException) can take
	 * values such as new PageRanges("3,5,7-9,15");
	 */
	public void setPagePrintRange(SetOfIntegerSyntax range) throws PdfException {

		if (range == null) throw new PdfException("[PDF] null page range entered");

		this.swingPrinter.setPagePrintRange(range, this.pageCount);
	}

	/**
	 * allow user to select only odd or even pages to print
	 */
	public void setPrintPageMode(int mode) {
		this.swingPrinter.setPrintPageMode(mode);
	}

	/**
	 * ask JPedal to stop printing a page
	 */
	final public void stopPrinting() {
		this.swingPrinter.stopPrinting();
	}

	/**
	 * return page currently being printed or -1 if finished
	 */
	public int getCurrentPrintPage() {
		return this.swingPrinter.getCurrentPrintPage();
	}

	public void resetCurrentPrintPage() {
		this.swingPrinter.currentPrintPage = 0;

		this.formRenderer.getCompData().resetAfterPrinting();
	}
}
