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
 * OutputDisplay.java
 * ---------------
 */
package org.jpedal.render.output;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.jpedal.color.ColorSpaces;
import org.jpedal.color.PdfPaint;
import org.jpedal.fonts.PdfFont;
import org.jpedal.fonts.StandardFonts;
import org.jpedal.fonts.glyph.PdfGlyph;
import org.jpedal.io.ObjectStore;
import org.jpedal.objects.GraphicsState;
import org.jpedal.objects.PdfPageData;
import org.jpedal.objects.TextState;
import org.jpedal.objects.acroforms.rendering.AcroRenderer;
import org.jpedal.parser.Cmd;
import org.jpedal.render.BaseDisplay;
import org.jpedal.render.DynamicVectorRenderer;
import org.jpedal.render.output.io.CustomIO;
import org.jpedal.render.output.io.DefaultIO;
import org.jpedal.utils.LogWriter;
import org.jpedal.utils.Matrix;
import org.jpedal.utils.StringUtils;
import org.jpedal.utils.repositories.Vector_Rectangle;

public class OutputDisplay extends BaseDisplay {

	// alt name for page 1
	protected String firstPageName;

	public static boolean convertT1Fonts = true;

	protected String packageName = "", javaFxFileName = "";

	protected HashMap<String, Object[]> fontsToConvert = new HashMap<String, Object[]>();

	protected HashMap<String, HashMap<String, Integer>> widths = new HashMap<String, HashMap<String, Integer>>();

	protected static boolean enableOTFConversion = false;

	// flag to show embedded fonts present so need to use full name
	protected boolean hasEmbeddedFonts = false;

	/** track the embedded fonts we might embed */
	protected Map embeddedFonts = new HashMap();

	/** track by full name as well */
	protected Map baseFontNames = new HashMap();

	/** show if any embedded fonts in Map */
	protected boolean hasEmbededFonts = false;

	/** track images written out to reduce IO if already drawn */
	private Map imagesAlreadyWritten = new HashMap();

	protected AcroRenderer acroRenderer;

	// offset in single page mode for svg
	// protected double newTotalHeight = 0;

	// page range
	protected int startPage, endPage;

	protected int pageGap = 50;

	String lastGlyf = "";

	boolean keepOriginalImage = false;

	protected String isOnlineConverter;

	protected boolean requiresTransform = false;
	protected boolean requiresTransformGlobal = false;
	protected boolean requiresTextGlobal = false;

	final public static int TEXT_AS_TEXT = -1;
	final public static int TEXT_AS_SHAPE = 1;
	final public static int TEXT_INVISIBLE_ON_IMAGE = 2;
	final public static int TEXT_VISIBLE_ON_IMAGE = 3;

	final public static int HTML_VIEW_MULTIFILE = 1;
	final public static int SVG_VIEW_MULTIFILE = 2;
	final public static int HTML_VIEW_MULTIFILE_SPLITSPREADS = 3;
	final public static int SVG_VIEW_MULTIFILE_SPLITSPREADS = 4;
	final public static int HTML_VIEW_SINGLEFILE = 5;
	final public static int SVG_VIEW_SINGLEFILE = 6;
	final public static int HTML_VIEW_SINGLEFILE_SPLITSPREADS = 7;
	final public static int SVG_VIEW_SINGLEFILE_SPLITSPREADS = 8;
	final public static int HTML_VIEW_SINGLEFILE_HORIZONTAL = 9;
	final public static int SVG_VIEW_SINGLEFILE_HORIZONTAL = 10;

	protected String clip = null;

	// provides common static functions
	static protected org.jpedal.render.output.OutputHelper Helper = null;

	FontMapper fontMapper = null;
	String lastFontUsed = "";

	private Map usedFontIDs = new HashMap();

	protected boolean includeClip = false;

	protected int textMode = -1;

	protected Map embeddedFontsByFontID = new HashMap();

	/** track if JS for glyf already inserted */
	private Map glyfsRasterized = new HashMap();

	final public static int MaxNumberOfDecimalPlaces = 0;
	final public static int FontMode = 1;
	final public static int PercentageScaling = 2;
	// final public static int SpacingPercentage=3;
	final public static int IncludeJSFontResizingCode = 4;

	final public static int ExcludeMetadata = 6;
	final public static int EmbedImageAsBase64Stream = 7;
	final public static int AddNavBar = 8;
	final public static int UseCharSpacing = 10;
	final public static int UseWordSpacing = 11;
	final public static int UseFontResizing = 12;
	final public static int HasJavascript = 13;
	final public static int ConvertSpacesTonbsp = 14;
	final public static int EncloseContentInDiv = 15;
	final public static int IncludeClip = 16;
	final public static int UseImagesOnNavBar = 17;
	final public static int TextMode = 18; // e.g. TEXT_AS_TEXT, TEXT_VISIBLE_ON_IMAGE
	final public static int DisplayMode = 19; // e.g. HTML_VIEW_MULTIFILE, SVG_VIEW_SINGLEFILE
	final public static int CustomIO = 21;
	final public static int StartOfDecode = 22;
	final public static int EndOfDecode = 23;
	final public static int EmulateEvenOdd = 24;
	final public static int HTMLImageMode = 26;
	final public static int UseThumbnailNavbar = 27;
	final public static int AcroRenderer = 28;
	final public static int AddTwitterButton = 30;
	final public static int AddFacebookButton = 31;
	final public static int AddGooglePlusButton = 32;
	final public static int AddRedditButton = 33;
	final public static int AddLinkedInButton = 34;
	final public static int AddDiggButton = 35;
	final public static int AddStumbleUponButton = 36;
	final public static int AddTumblrButton = 37;

	/** ie file we Launch via button from form */
	final public static int ConvertPDFExternalFileToOutputType = 38;
	final public static int GetIsSinglePage = 39;

	// Allow us to add a border if needed (SVG only)
	final public static int AddBorder = 40;
    public static final int IsSVGMode = 44;
    public static final int IsTextSelectable = 45;
    public static final int IsRealText = 46;

    public static final int DisableImageFallback = 47;
    public static final int PageTurning = 55;
    public static final int AddJavaScript = 56;
    public static final int EmbedImagesAsBase64 = 58;
    public static final int Base64Background = 59;
    public static final int DrawInvisibleText = 61;

    public static final int FontsToRasterizeInTextMode = 62;
    public static final int IsIDRViewer = 63;

	// hold a default value set via JVM flag for font mapping
	protected int defaultMode = FontMapper.DEFAULT_ON_UNMAPPED;

	protected int fontMode = FontMapper.DEFAULT_ON_UNMAPPED;

	// only output to 1 page
	protected boolean isSingleFileOutput = false;

	// visible or invisible text on image
	protected boolean htmlImageMode = false;

	// give user option to embed image inside HTML5
	protected boolean embedImageAsBase64 = false;

	private boolean groupGlyphsInTJ = true;

	// allow us to position every glyf on its own and not try to merge into strings*/
	protected boolean writeEveryGlyf = false;

	// control whether CSS inlined in HTML file or in own css file
	public boolean inlineCSS = false;

	/** debug flags */
	static final public boolean debugForms = false;
	final private static boolean DISABLE_IMAGES = false;
	final private static boolean DISABLE_SHAPE = false;
	final private static boolean DISABLE_TEXT = false;
	protected final static boolean DEBUG_TEXT_AREA = false;
	protected final static boolean DEBUG_DRAW_PAGE_BORDER = false;

	public final static int TOFILE = 0;
	public final static int TOP_SECTION = 1;
	public final static int SCRIPT = 2;
	public final static int FORM = 3;
	public final static int CSS = 4;
	public final static int TEXT = 6;
	public final static int KEEP_GLYFS_SEPARATE = 7;
	public final static int SET_ENCODING_USED = 8;
	public final static int JSIMAGESPECIAL = 9;
	public final static int SAVE_EMBEDDED_FONT = 10;
	public final static int PAGEDATA = 11;
	public final static int IMAGE_CONTROLLER = 12;
	public final static int FXMLPDFSTAGE = 13;
	public final static int FONT_AS_SHAPE = 14;
	public final static int FXMLTEXT = 15;
	public final static int FORMJS = 16;
	public final static int FORMJS_ONLOAD = 17;
	public final static int NAVBAR = 18;
	public final static int EXTERNAL_JS = 19;
	public final static int TEXTJS = 20;
	public static final int CALCULATION_ORDER = 21;
    public static final int SVGINHTML = 22;
    public static final int SVGCLIPS = 23;
    public static final int SVGBUFFER = 25;
    public static final int DVR = 26;
	public static final int LEGACY_CSS = 28;
	public static final int BOOKMARK = 29;
	//public static final int CustomIO = 31;
	public static final int HasJavaScript =32;
	public static final int THUMBNAIL_DISPLAY=35;
	public static final int IMAGE_DISPLAY=37;
	public static final int COMPATABILITY_DISPLAY=38;

	protected OutputImageController imageController = null;

	protected StringBuilder script = new StringBuilder(10000);
	protected ArrayList<String> fxScript = new ArrayList<String>();
	protected ArrayList<String> fxmlText = new ArrayList<String>();
	protected HashMap<Integer, String> base64Images = new HashMap<Integer, String>();
	protected HashMap<Integer, String> base64Shades = new HashMap<Integer, String>();

	protected StringBuilder fonts_as_shapes = new StringBuilder(10000);
	protected StringBuilder formJS = new StringBuilder(10000);
	protected StringBuilder formJSOnLoad = new StringBuilder(10000);
	protected StringBuilder form = new StringBuilder(10000);
	protected StringBuilder testDivs = new StringBuilder(10000);
	protected StringBuilder css = new StringBuilder(10000);
	protected StringBuilder topSection = new StringBuilder(10000);
	protected StringBuilder fxmlPDFStage = new StringBuilder(10000);
	protected StringBuilder navbarSection = new StringBuilder(10000);
	protected StringBuilder externalJSFile = new StringBuilder(10000);
	protected StringBuilder textJS = new StringBuilder(3000);
	// @Leon TODO - We could probably be less wasteful with these figures. They are initial capacities, after all.

	/** allow user to control scaling of images */
	protected boolean userControlledImageScaling = false;
	protected boolean emulateEvenOdd = false;

	/** current text element number if using Divs. Used as link to CSS */
	protected int textID = 1;

	protected int shadeId = 0;
	protected int imageId = 0;

	/** number of decimal places on numbers */
	protected int dpCount = 0;

	public String rootDir = null, fileName = null;

	protected int dx;
	protected int dy;
	protected int dr;

	protected boolean excludeMetadata = false;

	private boolean convertSpacesTonbsp = false;

	/** include a user nav tollbar in output */
	protected boolean addNavBar = false, useImagesOnNavBar = false;
	protected boolean useThumbnailNavbar = false;

	/** Controls blocks of text so they can be joined up */
	protected TextBlock currentTextBlock;
	protected TextBlock previousTextBlock;

	protected TextPosition currentTextPosition;

	protected Rectangle2D cropBox;
	protected Point2D midPoint;

	/** Used to reduce canvas color javascript calls between text and shapes */
	protected int currentColor = 0;

	/** control for encodings Java/CSS */
	protected String[] encodingType = new String[] { "UTF-8", "utf-8" };
	protected static final int JAVA_TYPE = 0;
	protected static final int OUTPUT_TYPE = 1;
	public static final int FORM_TAG = 0;

	protected float scaling = 1.0f; // Scale to large and images may be lost due to lack of memory.

	// amount of font change needed
	protected int fontChangeNeeded = -1;

	// Should we add a border to page (currently only used by SVG)
	protected boolean addBorder = true;

	// protected float spacingNeeded=1.0f; //amount of space needed

	float w;
	protected float h; // override int super.w and super.h for better accuracy. @Shit

	protected String[] tag = { "<form>" };

	// flag to say if JS has been added to allow images to work for checkboxes and radio buttons.
	protected boolean jsImagesAdded = false;

	protected String pageNumberAsString = null;
	protected PdfPageData pageData;
	private int currentTokenNumber = -1, lastTokenNumber = 496; // 496 is impossible value
	protected boolean includeJSFontResizingCode = true;

	protected String imageName;
	protected float iw, ih;
	protected double[] coords;

	// used to eliminate duplicate glyfs
	protected float[][] lastTrm;

	/** handles IO so user can override */
	protected CustomIO customIO = new DefaultIO();

	protected String imageArray = null;

	protected int[] currentImage;
	protected int[] currentPatternedShape;
	protected String currentPatternedShapeName;

	protected Color stageColor = new Color(55, 55, 65);

	protected String[] jsCalculationOrder;

	// Social Media Buttons
	protected boolean addTwitter = false;
	protected String viaTwitter = ""; // a twitter username you want to append to the end of the shared tweet

	protected boolean addFacebook = false;

	protected boolean addGooglePlus = false;

	protected boolean addReddit = false;

	protected boolean addLinkedIn = false;

	// protected boolean addDelicous = false;

	protected boolean addDigg = false;

	protected boolean addStumbleUpon = false;

	protected boolean addTumblr = false;

	protected boolean enableMagazineSplitSpreads = false;
	protected boolean enableSinglePageHorizontal = false;
	private boolean convertPDFExternalFileToOutputType = true;
	protected boolean useExternalJS = true;
	protected boolean enableTouchEvents = false; // add touch events to the html

	public OutputDisplay(int pageNumber, Point2D midPoint, Rectangle2D cropBox, boolean addBackground, int defaultSize, ObjectStore newObjectRef,
			Map params) {

		super();

		setParameters(params);

		// setup FontMode with any value passed in via JVM or default
		this.fontMode = this.defaultMode;

		this.type = DynamicVectorRenderer.CREATE_HTML;

		this.pageNumber = pageNumber;
		this.objectStoreRef = newObjectRef;
		this.addBackground = addBackground;
		this.cropBox = cropBox;
		this.midPoint = midPoint;

		// setupArrays(defaultSize);
		this.areas = new Vector_Rectangle(defaultSize);
	}

	private void setParameters(Map params) {

		if (params == null) {

			String mode = System.getProperty("org.jpedal.pdf2html.fontMode");

			if (mode != null) {

				mode = mode.toLowerCase();

				if (mode.equals("embed_all")) this.defaultMode = FontMapper.EMBED_ALL;
				else
					if (mode.equals("embed_all_except_base_families")) this.defaultMode = FontMapper.EMBED_ALL_EXCEPT_BASE_FAMILIES;
					else throw new RuntimeException("Mode " + mode + " not recognised");
			}

			if (System.getProperty("org.jpedal.pdf2html.keepOriginalImage") != null
					&& System.getProperty("org.jpedal.pdf2html.keepOriginalImage").toLowerCase().equals("true")) this.keepOriginalImage = true;

			if (System.getProperty("org.jpedal.pdf2html.convertOTFFonts") != null
					&& System.getProperty("org.jpedal.pdf2html.convertOTFFonts").toLowerCase().equals("true")) enableOTFConversion = true;

			this.isOnlineConverter = System.getProperty("IsOnlineConverter");

			this.firstPageName = System.getProperty("org.jpedal.pdf2html.firstPageName");

			if (System.getProperty("org.jpedal.pdf2html.stageColor") != null) {
				this.stageColor = Color.decode(System.getProperty("org.jpedal.pdf2html.stageColor"));
			}
		}
		else {

			String mode = (String) params.get("org.jpedal.pdf2html.fontMode");

			if (mode != null) {

				if (mode.equals("embed_all")) this.defaultMode = FontMapper.EMBED_ALL;
				else
					if (mode.equals("embed_all_except_base_families")) this.defaultMode = FontMapper.EMBED_ALL_EXCEPT_BASE_FAMILIES;
					else throw new RuntimeException("Mode " + mode + " not recognised");
			}

			if ((String) params.get("org.jpedal.pdf2html.keepOriginalImage") != null
					&& ((String) params.get("org.jpedal.pdf2html.keepOriginalImage")).equals("true")) this.keepOriginalImage = true;

			if ((String) params.get("org.jpedal.pdf2html.convertOTFFonts") != null
					&& ((String) params.get("org.jpedal.pdf2html.convertOTFFonts")).equals("true")) enableOTFConversion = true;

			this.isOnlineConverter = (String) params.get("IsOnlineConverter");

			this.firstPageName = (String) params.get("org.jpedal.pdf2html.firstPageName");

			if ((String) params.get("org.jpedal.pdf2html.stageColor") != null) {
				this.stageColor = Color.decode((String) params.get("org.jpedal.pdf2html.stageColor"));
			}
		}
	}

	// allow user to control various values
	@Override
	public void setValue(int key, int value) {

		switch (key) {

			case DisplayMode:
				switch (value) {
					case HTML_VIEW_MULTIFILE:
					case SVG_VIEW_MULTIFILE:
						this.isSingleFileOutput = false;
						this.enableMagazineSplitSpreads = false;
						this.enableSinglePageHorizontal = false;
						break;
					case HTML_VIEW_MULTIFILE_SPLITSPREADS:
					case SVG_VIEW_MULTIFILE_SPLITSPREADS:
						this.isSingleFileOutput = false;
						this.enableMagazineSplitSpreads = true;
						this.enableSinglePageHorizontal = false;
						break;
					case HTML_VIEW_SINGLEFILE:
					case SVG_VIEW_SINGLEFILE:
						this.isSingleFileOutput = true;
						this.enableMagazineSplitSpreads = false;
						this.enableSinglePageHorizontal = false;
						break;
					case HTML_VIEW_SINGLEFILE_SPLITSPREADS:
					case SVG_VIEW_SINGLEFILE_SPLITSPREADS:
						this.isSingleFileOutput = true;
						this.enableMagazineSplitSpreads = true;
						this.enableSinglePageHorizontal = false;
						break;
					case HTML_VIEW_SINGLEFILE_HORIZONTAL:
					case SVG_VIEW_SINGLEFILE_HORIZONTAL:
						this.isSingleFileOutput = true;
						this.enableMagazineSplitSpreads = false;
						this.enableSinglePageHorizontal = true;
						break;
				}
				break;

			case EndOfDecode:
				this.endPage = value;
				break;

			case FontMode:
				// <start-std>
				/**
				 * //<end-std> if(value==org.jpedal.render.output.GenericFontMapper.EMBED_ALL ||
				 * value==org.jpedal.render.output.GenericFontMapper.EMBED_ALL_EXCEPT_BASE_FAMILIES){ break; } /
				 **/
				this.fontMode = value;
				break;

			case MaxNumberOfDecimalPlaces:
				this.dpCount = value;
				break;

			case PercentageScaling:
				this.scaling = value / 100f;

				// adjust for single page
				// if(isSingleFileOutput)
				// newTotalHeight = (int)((cropBox.getBounds2D().getHeight()*scaling) + pageGap) * (pageNumber-1) ; //How far down the page the
				// content of next page starts in single file output

				break;

			case StartOfDecode:
				this.startPage = value;
				break;

			case TextMode:
				this.textMode = value;
				break;

			case UseFontResizing:
				this.fontChangeNeeded = value;
				break;

		// case SpacingPercentage:
		// this.spacingNeeded=value/100f;
		// break;

		}
	}

	// allow user to control various values
	@Override
	public boolean getBooleanValue(int key) {

		switch (key) {
			case ConvertPDFExternalFileToOutputType:
				return this.convertPDFExternalFileToOutputType;

			case GetIsSinglePage:
				return this.isSingleFileOutput;

			default:
				return super.getBooleanValue(key);
		}
	}

	// allow user to control various values
	@Override
	public void setBooleanValue(int key, boolean value) {

		switch (key) {

			case AddNavBar:
				this.addNavBar = value;
				break;

			case ConvertPDFExternalFileToOutputType:
				this.convertPDFExternalFileToOutputType = value;
				break;

			case ConvertSpacesTonbsp:
				this.convertSpacesTonbsp = value;
				break;

			case ExcludeMetadata:
				this.excludeMetadata = value;
				break;

			case IncludeClip:
				this.includeClip = value;
				break;

			case UseImagesOnNavBar:
				this.useImagesOnNavBar = value;
				break;

			case UseThumbnailNavbar:
				this.useThumbnailNavbar = value;
				break;

			case HTMLImageMode:
				this.htmlImageMode = value;
				break;

			case AddTwitterButton:
				this.addTwitter = value;
				break;

			case AddFacebookButton:
				this.addFacebook = value;
				break;

			case AddGooglePlusButton:
				this.addGooglePlus = value;
				break;

			case AddRedditButton:
				this.addReddit = value;
				break;

			case AddLinkedInButton:
				this.addLinkedIn = value;
				break;

			case AddDiggButton:
				this.addDigg = value;
				break;

			case AddStumbleUponButton:
				this.addStumbleUpon = value;
				break;

			case AddTumblrButton:
				this.addTumblr = value;
				break;

			case AddBorder:
				this.addBorder = value;
				break;
		}
	}

	// allow user to control various values
	@Override
	public int getValue(int key) {

		int value = -1;

		switch (key) {

			case FontMode:
				value = this.fontMode;
				break;

			case PercentageScaling:
				value = (int) (this.scaling * 100f);
				break;

			case TextMode:
				value = this.textMode;
				break;

		}

		return value;
	}

	/**
	 * allow user to set own value for certain tags Throws RuntimeException
	 * 
	 * @param type
	 * @param value
	 */
	@Override
	public void setTag(int type, String value) {

		// switch (type) {
		//
		// case FORM_TAG:
		// tag[FORM_TAG]=value;
		// break;
		//
		// default:
		throw new RuntimeException("Unknown tag value " + type);
		// }
	}

	/* setup renderer */
	@Override
	public void init(int width, int height, int rawRotation, Color backgroundColor) {

		if (rawRotation == 90 || rawRotation == 270) {
			this.h = width * this.scaling;
			this.w = height * this.scaling;
		}
		else {
			this.w = width * this.scaling;
			this.h = height * this.scaling;
		}

		this.pageRotation = rawRotation;
		this.backgroundColor = backgroundColor;
		this.shadeId = 0;

		this.currentTextBlock = new TextBlock();
		this.previousTextBlock = new TextBlock();
	}

	/**
	 * Add output to correct area so we can assemble later. Can also be used for any specific code features (ie setting a value)
	 */
	@Override
	public synchronized void writeCustom(int section, Object str) {

		switch (section) {

			case PAGEDATA:

				this.pageData = (PdfPageData) str;

				// any offset
				this.dx = this.pageData.getCropBoxX(this.pageNumber);
				this.dy = this.pageData.getCropBoxY(this.pageNumber);
				this.dr = this.pageData.getRotation(this.pageNumber);
				break;

			case IMAGE_CONTROLLER:

				this.imageController = (OutputImageController) str;
				this.userControlledImageScaling = this.imageController != null;
				break;

			case AcroRenderer:
				this.acroRenderer = (AcroRenderer) str;
				break;

			case CustomIO:

				this.customIO = (CustomIO) str;
				break;

			// <start-std>
			// special case used from PdfStreamDecoder to get font data
			case SAVE_EMBEDDED_FONT:

				// save font data as file
				Object[] fontData = (Object[]) str;
				PdfFont pdfFont = (PdfFont) fontData[0];
				String fontName = pdfFont.getFontName();
				String fileType = (String) fontData[2];
				String fontID = (String) fontData[3];

				// make sure Dir exists
				String fontPath;

				fontPath = this.rootDir + this.fileName + "/fonts/";

				File cssDir = new File(fontPath);
				if (!cssDir.exists()) {
					cssDir.mkdirs();
				}

				byte[] rawFontData = (byte[]) fontData[1];
				fontName = fontName.replaceAll("[.,*#]", "-");

				// the fonts with , in name from Ghostscript do not work so ignore
				// add 1==2 && to line below and it will now cascade into Sam's font handler
				if (fileType.equals("ttf") && !fontName.contains(",")) { // truetype tidied up to TT

					/** ensure unique if embedded by adding fontID and size of data */
					/**
					 * second text is becuase if both start with ABCDEF+ we can safely merge- example /PDFdata/sample_pdfs_html/thoughtcorp/SDM Code
					 * Audit RFP Proposal Feb 2010 v6
					 */
					if (this.embeddedFonts.containsKey(fontName) && !this.baseFontNames.containsKey(pdfFont.getBaseFontName())) {

						// flag before we change it
						this.baseFontNames.put(pdfFont.getBaseFontName(), "x");

						fontName = fontName + '_' + fontID + '_' + rawFontData.length;
						pdfFont.resetNameForHTML(fontName); // and write back to font object so we use this in CSS tags

					}

					try {
						rawFontData = org.jpedal.fonts.HTMLFontUtils.convertTTForHTML(pdfFont, fontName, rawFontData);

						if (enableOTFConversion) {
							rawFontData = org.jpedal.fonts.HTMLFontUtils.convertPSForHTMLOTF(pdfFont, fontName, rawFontData, fileType,
									this.widths.get(fontName));
							fileType = "otf";
						}
						else {
							rawFontData = org.jpedal.fonts.HTMLFontUtils.convertPSForHTMLWOFF(pdfFont, fontName, rawFontData, fileType,
									this.widths.get(fontName));
							fileType = "woff";
						}

						if (rawFontData != null) {
							this.customIO.writeFont(fontPath + fontName + '.' + fileType, rawFontData);
						}
					}
					catch (Exception e) {
						// tell user and log
						if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
					}
				}
				else
					if ((fileType.equals("t1") && convertT1Fonts) || fileType.equals("cff") || (fileType.equals("ttf") && !fontName.contains(","))) { // postscript
																																						// to
																																						// otf

						if (fileType.equals("ttf")) {
							fontName = pdfFont.getBaseFontName().substring(7);
						}
						else fontName = pdfFont.getBaseFontName().replace('+', '-');

						/** ensure unique if embedded by adding fontID and size of data */
						if (this.embeddedFonts.containsKey(fontName)) {
							fontName = fontName + '_' + fontID + '_' + rawFontData.length;
							pdfFont.resetNameForHTML(fontName); // and write back to font object so we use this in CSS tags
						}

						// tell software name needs to be unique
						this.hasEmbeddedFonts = true;
						this.fontsToConvert.put(fontName, fontData);

						if (enableOTFConversion) {
							fileType = "otf";
						}
						else {
							fileType = "woff";
						}

					}

				if (rawFontData != null) {
					// save details into CSS so we can put in HTML
					StringBuilder fontTag = new StringBuilder();
					String replacedFontName = fontName.replaceAll("[.,*#]", "-");
					if (this.type == CREATE_JAVAFX) {

						fontTag.append("Font.loadFont("); // indent
						fontTag.append(this.javaFxFileName + ".class.getResource(\"").append(this.fileName).append("/fonts/")
								.append(replacedFontName).append('.').append(fileType).append("\").toExternalForm(),10);\n");

					}
					else {
						fontTag.append("@font-face {\n"); // indent
						fontTag.append("\tfont-family: ").append(replacedFontName).append(";\n");
						fontTag.append("\tsrc: url(\"").append(this.fileName).append("/fonts/").append(replacedFontName).append('.').append(fileType)
								.append("\");\n");
						fontTag.append("}\n");
					}
					// save font details to use later
					this.embeddedFonts.put(fontName, fontTag);

					this.hasEmbededFonts = true;

					// save font id so we can see how many fonts mapped onto this name
					String value = (String) this.embeddedFontsByFontID.get(fontName);
					if (value == null) this.embeddedFontsByFontID.put(fontName, fontID);
					else this.embeddedFontsByFontID.put(fontName, value + ',' + fontID);
				}

				break;

			// <end-std>

			default:
				throw new RuntimeException("Option " + section + " not recognised");
		}
	}

	@Override
	public synchronized void flagDecodingFinished() {

		if (this.customIO != null && this.customIO.isOutputOpen()) {
			completeOutput();
		}

		Object[] fontNames = this.fontsToConvert.keySet().toArray();
		for (Object fontName1 : fontNames) {
			Object[] fontData = this.fontsToConvert.get(fontName1);

			PdfFont pdfFont = (PdfFont) fontData[0];
			String fontName;// = pdfFont.getFontName();
			String fileType = (String) fontData[2];
			String fontID = (String) fontData[3];

			// make sure Dir exists
			String fontPath = this.rootDir + this.fileName + "/fonts/";
			File cssDir = new File(fontPath);
			if (!cssDir.exists()) {
				cssDir.mkdirs();
			}

			byte[] rawFontData = (byte[]) fontData[1];

			if (fileType.equals("ttf")) fontName = pdfFont.getBaseFontName().replace('+', '-').substring(7);
			else fontName = pdfFont.getBaseFontName().replace('+', '-');

			// tell software name needs to be unique
			this.hasEmbeddedFonts = true;

			boolean saveRawFonts = false;

			if (saveRawFonts) {
				this.customIO.writeFont(fontPath + fontName + ".cff", rawFontData);
			}

			try {
				if (enableOTFConversion) {
					rawFontData = org.jpedal.fonts.HTMLFontUtils.convertPSForHTMLOTF(pdfFont, fontName, rawFontData, fileType,
							this.widths.get(fontName));
					fileType = "otf";
				}
				else {
					rawFontData = org.jpedal.fonts.HTMLFontUtils.convertPSForHTMLWOFF(pdfFont, fontName, rawFontData, fileType,
							this.widths.get(fontName));
					fileType = "woff";
				}
			}
			catch (Exception e) {
				// tell user and log
				if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
			}

			/**/
			fontName = fontName.replaceAll("[.,*#]", "-");

			if (rawFontData != null) {

				this.customIO.writeFont(fontPath + fontName + '.' + fileType, rawFontData);

				// save details into CSS so we can put in HTML
				StringBuilder fontTag = new StringBuilder();
				fontTag.append("@font-face {\n"); // indent
				fontTag.append("\tfont-family: ").append(fontName).append(";\n");
				fontTag.append("\tsrc: url(\"").append(this.fileName).append("/fonts/").append(fontName).append('.').append(fileType)
						.append("\");\n");
				fontTag.append("}\n");

				// save font details to use later
				this.embeddedFonts.put(fontName, fontTag);

				// also include so we can see if it is embedded or needs replacing
				this.embeddedFonts.put(fontID, fontName);

				this.hasEmbededFonts = true;

				// save font id so we can see how many fonts mapped onto this name
				String value = (String) this.embeddedFontsByFontID.get(fontName);
				if (value == null) this.embeddedFontsByFontID.put(fontName, fontID);
				else this.embeddedFontsByFontID.put(fontName, value + ',' + fontID);
			}
		}
	}

	protected String roundUp(double d) {
		return "" + (int) (d + 0.99);
	}

	protected boolean usingCachedImage = false;
	protected int cachedImageId = 0;

	// save image in array to draw
	@Override
	public int drawImage(int pageNumber, BufferedImage image, GraphicsState gs, boolean alreadyCached, String name, int optionsApplied,
			int previousUse) {

		/**
		 * ensure if image is reused we do not write out again (assumes name is unique for key on page) If first time, flag as used. We only use if no
		 * clip as I have seen examples of images with diff clips This would be slow to track and null works on target files.
		 */
		String cacheKey = pageNumber + '-' + name;
		this.usingCachedImage = false;// reset
		boolean imagesWritten = gs.getClippingShape() == null && this.imagesAlreadyWritten.get(cacheKey) != null;
		if (!imagesWritten) {
			this.imagesAlreadyWritten.put(cacheKey, this.imageId + 1);// @Leon TODO - Possible bug here, what if the first image doesn't get output
																		// because the cropboxes don't intersect?
		}
		else
			if (!this.embedImageAsBase64) { // except in this case we can ignore image so set to null and just calc coords //@Leon TODO - With my new
											// JS code, we could do this for base64 images.
				this.usingCachedImage = true;
				this.cachedImageId = (Integer) this.imagesAlreadyWritten.get(cacheKey);
				image = null;
			}

		// flush any cached text before we write out
		flushText();

		// show if image is upside down
		boolean needsflipping = false;// pageRotation==180 && gs.CTM[0][0]>0 && gs.CTM[1][1]>0;

		// figure out co-ords
		float x = (gs.CTM[2][0] * this.scaling);
		float y = (gs.CTM[2][1] * this.scaling);

		this.iw = (gs.CTM[0][0] + Math.abs(gs.CTM[1][0])) * this.scaling;
		this.ih = (gs.CTM[1][1] + Math.abs(gs.CTM[0][1])) * this.scaling;

		// value can also be set this way but we need to adjust the x coordinate
		if (this.iw == 0) {
			this.iw = gs.CTM[1][0] * this.scaling;

			if (this.iw < 0) this.iw = -this.iw;
		}

		// again value can be set this way
		if (this.ih == 0) {
			this.ih = gs.CTM[0][1] * this.scaling;
		}

		// Reset with to positive if negative
		if (this.iw < 0) this.iw = this.iw * -1;

		if (this.iw < 1) this.iw = 1;

		if (this.ih == 0) {
			this.ih = 1;
		}

		// Account for negative widths (ie ficha_acceso_a_ofimatica_e-portafirma.pdf)
		if (this.ih < 1) {
			y += this.ih;
			this.ih = Math.abs(this.ih);
		}

		// add negative width value to of the x coord problem_document2.pdf page 3
		if (gs.CTM[0][0] < 0) {
			x = x - this.iw;
		}

		// adjust in this case so in correct place
		if (gs.CTM[0][0] > 0 && gs.CTM[1][1] < 0 && gs.CTM[0][1] == 0 && gs.CTM[1][0] == 0) {
			// x=x-iw;
			needsflipping = true;
		}

		// factor in non-zero offset for cropBox
		// for the moment lets limit it to just this case of page cropping/4.pdf
		if (gs.CTM[0][0] == 0 && gs.CTM[1][1] == 0 && gs.CTM[0][1] > 0 && gs.CTM[1][0] > 0) {
			x = x - (this.pageData.getCropBoxX2D(pageNumber) / 2);
			y = y - this.pageData.getCropBoxY2D(pageNumber);
		}

		Graphics2D g2savedImage = null;
		// scale image
		BufferedImage savedImage = image;
		// Image img=null;

		boolean isXSet = false, isYSet = false;

		// if crop smaller, use as image size
		if (gs.getClippingShape() != null) {
			Rectangle2D clipBounds = gs.getClippingShape().getBounds2D();

			float clipW = (float) (clipBounds.getWidth() * this.scaling);
			float clipH = (float) (clipBounds.getHeight() * this.scaling);

			if (clipW < this.iw) {
				this.iw = clipW;
				// ih=clipH;
				x = (float) (clipBounds.getMinX() * this.scaling);
				// y= (float) (clipBounds.getMinY()*scaling);

				isXSet = true;
			}

			if (clipH < this.ih) {
				// iw=clipW;
				this.ih = clipH;
				// x= (float) (clipBounds.getMinX()*scaling);
				y = (float) (clipBounds.getMinY() * this.scaling);

				isYSet = true;
			}
		}

		// factor in offset if not done above
		if (!isXSet) {
			if (gs.CTM[1][0] < 0 && gs.CTM[0][0] != 0) {
				x = x + (gs.CTM[1][0] * this.scaling);
			}

			if (gs.CTM[1][0] < 0 && gs.CTM[0][0] == 0) {
				x = x - this.iw;
			}
		}

		if (!isYSet) {
			// (guess for bottom right image on general/forum/2.html)
			if (gs.CTM[0][1] < 0 && gs.CTM[1][1] != 0) {// && gs.CTM[0][0]<gs.CTM[1][0]){
				y = y + (gs.CTM[0][1] * this.scaling);
			}

			if (gs.CTM[0][1] < 0 && gs.CTM[1][1] == 0) {
				y = y - this.ih;
			}
		}

		if (this.htmlImageMode) {
			if (this.pageRotation == 90 || this.pageRotation == 270) {
				this.coords = new double[] { y, x };
			}
			else {
				this.coords = new double[] { x, y };
			}
		}
		else {

			// Covert PDF coords (factor out scaling in calc)
			switch (this.pageRotation) {

			// adjust co-ords
			// image actually rotated in rotateImage()

				case 180:
					this.coords = new double[] { this.cropBox.getWidth() - ((this.iw + x) / this.scaling),
							this.cropBox.getHeight() - ((this.ih + y) / this.scaling) };
					break;

				//
				case 270:
					// System.out.println(gs.CTM[0][0]+" "+gs.CTM[1][0]+" "+" "+gs.CTM[0][1]+" "+gs.CTM[1][1]);

					// special case /PDFdata/sample_pdfs_html/samsung/rotate270.pdf
					if (gs.CTM[0][0] > 0 && gs.CTM[1][1] > 0 && gs.CTM[1][0] == 0 && gs.CTM[0][1] == 0) {
						this.coords = new double[] { this.cropBox.getWidth() - ((x + this.iw) / this.scaling), y / this.scaling };
					}
					else {
						this.coords = new double[] { x / this.scaling, y / this.scaling };
					}
					break;

				default:
					this.coords = new double[] { x / this.scaling, y / this.scaling };
					break;
			}

		}

		correctCoords(this.coords);

		// add back in scaling
		this.coords[0] = this.coords[0] * this.scaling;
		this.coords[1] = this.coords[1] * this.scaling;

		// subtract image height as y co-ordinate inverted
		this.coords[1] -= this.ih;

		Rectangle2D rect = new Rectangle2D.Double(this.coords[0], this.coords[1], this.iw, this.ih);
		Rectangle2D cropBoxScaled = new Rectangle2D.Double(this.cropBox.getX() * this.scaling, this.cropBox.getY() * this.scaling,
				this.cropBox.getWidth() * this.scaling, this.cropBox.getHeight() * this.scaling);

		if (cropBoxScaled.intersects(rect)) {

			if (!this.usingCachedImage) this.imageId++;

			/**
			 * where we can scale down
			 */
			// if(!MARKS_NEW_IMAGE_CODE){ //not sure on whther to use - keeps full hires image
			if (!this.userControlledImageScaling && !this.htmlImageMode && image != null) { // default values

				if (this.pageRotation != 0) {
					image = rotateImage(image, this.pageRotation);
				}

				// 1 pixel high images also do not scale due to Java bug (see /PDFdata/sample_pdfs_html/general/20120227.pdf)
				if (image.getHeight() == 1 || (!needsflipping && this.iw == image.getWidth() && this.ih == image.getHeight())) { // if size correct do
																																	// not scale
					savedImage = image;

				}
				else {
					if (g2savedImage == null) {
						savedImage = new BufferedImage((int) this.iw == 0 ? 1 : (int) this.iw, (int) this.ih == 0 ? 1 : (int) this.ih,
								image.getType());

						g2savedImage = (Graphics2D) savedImage.getGraphics();

						AffineTransform aff = new AffineTransform();

						// Matrix.show(gs.CTM);
						//
						// try {
						// ImageIO.write(image, "PNG", new File("/Users/markee/Desktop/before-" + name + ".png"));
						// } catch (IOException e) {
						// e.printStackTrace();
						// }

						// add transform to flip if needed
						if (needsflipping) {
							aff.scale(1, -1);
							aff.translate(0, -this.ih);
						}

						g2savedImage.setTransform(aff);

					}
					g2savedImage.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

					g2savedImage.drawImage(image, 0, 0, (int) this.iw, (int) this.ih, null);

				}
			}

			/**
			 * store image
			 */

			if (this.embedImageAsBase64) {
				// convert image into base64 content
				try {
					ByteArrayOutputStream bos = new ByteArrayOutputStream();
					javax.imageio.ImageIO.write(savedImage, "PNG", bos);
					bos.close();
					sun.misc.BASE64Encoder b64 = new sun.misc.BASE64Encoder();
					this.imageArray = new sun.misc.BASE64Encoder().encode(bos.toByteArray());
					this.imageArray = this.imageArray.replace("\r\n", "\\\r\n"); // \ is required at EOLs as defined in JS
				}
				catch (IOException e) {
					// tell user and log
					if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
				}
			}
			else {

				String imageDir = this.fileName + "/img/";

				if (!imagesWritten) { // make sure image Dir exists and write out as first time
					File imgDir = new File(this.rootDir + imageDir);
					if (!imgDir.exists()) imgDir.mkdirs();

					this.customIO.writeImage(this.rootDir, imageDir + this.imageId, savedImage);
					this.imageName = imageDir + this.imageId + this.customIO.getImageTypeUsed();

				}
				else {
					this.imageName = imageDir + this.cachedImageId + this.customIO.getImageTypeUsed();
				}
			}

			if (this.htmlImageMode) {
				// These final values are used by all HTML, SVG, JavaFX, FXML, etc
				this.currentImage = new int[] { 0, 0, (int) this.iw, (int) this.ih };
			}
			else {
				// From the PDF spec - If an Image overlaps a pixel by more than 50%, the pixel gets filled, otherwise not.
				int finalX, finalY, finalWidth, finalHeight;
				switch (this.pageRotation) {
					case 90:
						double tmpX = this.pageData.getCropBoxHeight2D(pageNumber) * this.scaling - this.coords[1] - this.iw;
						double tmpY = this.coords[0];
						finalWidth = (int) ((tmpX - ((int) tmpX)) + this.iw + 0.99);
						finalHeight = (int) ((tmpY - ((int) tmpY)) + this.ih + 0.99);
						finalX = (int) tmpX;
						finalY = (int) tmpY;
						break;

					case 270:
						finalWidth = (int) ((this.coords[1] - ((int) this.coords[1])) + this.iw + 0.99);
						finalHeight = (int) ((this.coords[0] - ((int) this.coords[0])) + this.ih + 0.99);
						finalX = (int) (this.coords[1]);
						finalY = (int) (this.coords[0]);
						break;

					default:
						finalWidth = (int) ((this.coords[0] - ((int) this.coords[0])) + this.iw + 0.99);
						finalHeight = (int) ((this.coords[1] - ((int) this.coords[1])) + this.ih + 0.99);
						finalX = (int) (this.coords[0]);
						finalY = (int) (this.coords[1]);
						break;
				}

				// These final values are used by all HTML, SVG, JavaFX, FXML, etc
				this.currentImage = new int[] { finalX, finalY, finalWidth, finalHeight };
			}
			return -2;
		}
		else return -1;
	}

	protected BufferedImage rotateImage(BufferedImage savedImage, int angle) {

		BufferedImage rotatedImage;

		if (angle == 180) {

			AffineTransform tx = AffineTransform.getScaleInstance(-1, -1);
			tx.translate(-savedImage.getWidth(null), -savedImage.getHeight(null));
			AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
			rotatedImage = op.filter(savedImage, null);

		}
		else {

			int w = savedImage.getWidth();
			int h = savedImage.getHeight();
			rotatedImage = new BufferedImage(h, w, savedImage.getType());
			Graphics2D g2 = rotatedImage.createGraphics();

			g2.rotate(Math.toRadians(this.pageRotation), w / 2, h / 2);

			int diff = (w - h) / 2;

			if (angle == 90) {
				g2.drawImage(savedImage, diff, diff, null);

			}
			else
				if (angle == 270) {
					g2.drawImage(savedImage, -diff, -diff, null);

				}
				else {}

			// swap values
			float a = this.iw;
			this.iw = this.ih;
			this.ih = a;

		}

		return rotatedImage;
	}

	/**
	 * trim if needed
	 * 
	 * @param i
	 * @return * (note duplicate in OutputShape)
	 */
	protected String setPrecision(double i) {

		String value = String.valueOf(i);

		int ptr = value.indexOf('.');
		int len = value.length();
		int decimals = len - ptr - 1;

		if (ptr > 0 && decimals > this.dpCount) {
			if (this.dpCount == 0) value = value.substring(0, ptr + this.dpCount);
			else value = value.substring(0, ptr + this.dpCount + 1);

		}

		return removeEmptyDecimals(value);
	}

	final static int[] indices = new int[] { 1, 10, 100, 1000 };

	/**
	 * trim if needed
	 * 
	 * @param i
	 * @return * (note duplicate in OutputShape)
	 */
	protected static String setPrecision(double i, int dpCount) {

		if (dpCount > 3) throw new RuntimeException("dp count must be less than 4");

		double roundedValue = (double) (((int) (i * indices[dpCount]))) / indices[dpCount];

		if (roundedValue > 0.98 && roundedValue < 1.01) {
			return "1";
		}
		else
			if (roundedValue == 0) {
				return "0";
			}
			else
				if (roundedValue == -0) {
					return "0";
				}
				else
					if (roundedValue < -0.98 && roundedValue > -1.01) {
						return "-1";
					}
					else {
						return String.valueOf((double) (((int) (i * indices[dpCount]))) / indices[dpCount]);
					}
	}

	/* save clip in array to draw */
	@Override
	public void drawClip(GraphicsState currentGraphicsState, Shape defaultClip, boolean canBeCached) {
		// RenderUtils.renderClip(currentGraphicsState.getClippingShape(), null, defaultClip, g2);
	}

	// For debugging text
	// static int textShown = 0;
	// static int amount = 7;

	@Override
	public void drawEmbeddedText(float[][] Trm, int fontSize, PdfGlyph embeddedGlyph, Object javaGlyph, int type, GraphicsState gs,
			AffineTransform at, String glyf, PdfFont currentFontData, float glyfWidth) {

		/**
		 * type 3 are a special case and need to be rendered as images in all modes So we will need to detect and render here for both
		 */
		// if(currentFontData.getFontType()==StandardFonts.TYPE3){
		//
		// }

		float[][] rawTrm = new float[3][3];
		rawTrm[0][0] = Trm[0][0];
		rawTrm[0][1] = Trm[0][1];
		rawTrm[0][2] = Trm[0][2];
		rawTrm[1][0] = Trm[1][0];
		rawTrm[1][1] = Trm[1][1];
		rawTrm[1][2] = Trm[1][2];
		rawTrm[2][0] = Trm[2][0];
		rawTrm[2][1] = Trm[2][1];
		rawTrm[2][2] = Trm[2][2];

		// ignore blocks of multiple spaces
		if (this.currentTokenNumber == this.lastTokenNumber && glyf.equals(" ") && this.lastGlyf.equals(" ")) {
			flushText();
			return;
		}
		// trap any non-standard glyfs
		if (OutputDisplay.Helper != null && glyf.length() > 3 && !StandardFonts.isValidGlyphName(glyf)) {
			glyf = OutputDisplay.Helper.mapNonstandardGlyfName(glyf, currentFontData);
		}

		// if its not visible, ignore it
		Area clip = gs.getClippingShape();
		if (clip != null && !clip.getBounds().contains(new Point((int) Trm[2][0] + 1, (int) Trm[2][1] + 1))
				&& !clip.getBounds().contains(new Point((int) Trm[2][0] + fontSize / 2, (int) Trm[2][1] + fontSize / 2))) {
			return;
		}

		/**
		 * workout location
		 */
		double[] coords = { Trm[2][0], Trm[2][1] };

		correctCoords(coords);

		if (this.textMode == TEXT_AS_SHAPE || this.textMode == TEXT_INVISIBLE_ON_IMAGE && getType() == DynamicVectorRenderer.CREATE_SVG) { // option
																																			// to
																																			// convert
																																			// to
																																			// shapes
			this.currentTextPosition = new TextPosition(coords, new float[] { rawTrm[0][0], rawTrm[0][1], rawTrm[1][0], rawTrm[1][1], rawTrm[2][0],
					rawTrm[2][1] });
			rasterizeTextAsShape(embeddedGlyph, gs, currentFontData, glyf);
			return;
		}

		// if(!glyf.equals("5") || fontSize<40)
		// return;

		/**
		 * factor in page rotation to Trm (not on canvas)
		 */
		switch (this.pageRotation) {

			case 90: {

				float x = Trm[2][0], y = Trm[2][1];
				Trm = Matrix.multiply(Trm, new float[][] { { 0, -1, 0 }, { 1, 0, 0 }, { 0, 0, 1 } });
				if (Trm[0][0] == 0 && Trm[1][1] == 0 && Trm[0][1] * Trm[1][0] < 0 && gs.CTM[0][0] > 0 && gs.CTM[1][1] > 0) { // ie
																																// /Users/markee/PDFdata/sample_pdfs_html/abacus/6208_Test%20PDF.pdf
					Trm[2][0] = y;
					Trm[2][1] = (float) (this.cropBox.getHeight() - x);// cropBox.height-x;//-(fontSize/2);

				}
				else { // ie /Users/markee/PDFdata/sample_pdfs_html/abacus/6208_Test%20PDF.pdf
					Trm[2][0] = x;// y;
					Trm[2][1] = y;// cropBox.height-x;//-(fontSize/2);
				}
			}
				break;

			case 270: {

				float x = Trm[2][0], y = Trm[2][1];
				if (Trm[0][0] > 0 && Trm[1][1] > 0 && Trm[0][1] == 0 && Trm[1][0] == 0) {}
				else {
					Trm = Matrix.multiply(Trm, new float[][] { { 0, 1, 0 }, { -1, 0, 0 }, { 0, 0, 1 } });
				}

				break;
			}
		}

		// if -100 we get value - allows user to override
		if (glyfWidth == -100) {
			glyfWidth = currentFontData.getWidth(-1);
		}

		/**
		 * optimisations to choose best fontsize for different cases of FontSize, Tc and Tw
		 */
		int altFontSize = -1, fontCondition = -1;
		TextState currentTextState = gs.getTextState();
		if (currentTextState != null) {
			float rawFontSize = Trm[0][0];
			float tc = currentTextState.getCharacterSpacing();
			float tw = currentTextState.getWordSpacing();

			// do not use rounded up fontSize - we generate value again
			float diff = (rawFontSize - (int) rawFontSize);

			if (rawFontSize > 1) {
				// case 1 font sixe 8.5 or above and negative Tc
				if (fontSize == 9 && diff == 0.5f && tc > -0.2 && tw == 0 && fontSize != (int) rawFontSize) {
					altFontSize = (int) rawFontSize;

					// identify type
					fontCondition = 1;

				}
				else
					if (fontSize == 8 && rawFontSize > 8 && tc > 0 && diff < 0.3f && tw == 0) {
						altFontSize = fontSize + 1;
						fontCondition = 2;
					}
			}
		}

		// code assumes Trm is square - if not alter font size
		if (Trm[0][0] != Trm[1][1] && Trm[1][0] == 0 && Trm[0][1] == 0) {
			fontSize = (int) Trm[0][0];
		}

		// Ignore empty or crappy characters
		if (glyf.length() == 0 || TextBlock.ignoreGlyf(glyf)) {
			return;
		}

		// text size sometimes negative as a flag so ensure always positive
		if (fontSize < 0) fontSize = -fontSize;

		float charWidth = fontSize * glyfWidth;

		// get font name and convert if needed for output and changed
		if (!currentFontData.getBaseFontName().equals(this.lastFontUsed)) {
			this.fontMapper = getFontMapper(currentFontData);
			this.lastFontUsed = currentFontData.getBaseFontName();

			// save font id so we can see how many fonts mapped onto this name
			String value = (String) this.embeddedFontsByFontID.get(this.lastFontUsed);
			if (value == null) this.embeddedFontsByFontID.put(this.lastFontUsed, "browser");
			else
				if (!value.contains("browser")) this.embeddedFontsByFontID.put(this.lastFontUsed, value + ',' + "browser");
		}

		int textFillType = gs.getTextRenderType();
		int color = textFillType == GraphicsState.STROKE ? gs.getStrokeColor().getRGB() : gs.getNonstrokeColor().getRGB();

		// reject duplicate text used to create text bold by creating slighlty offset second character
		if (this.lastTrm != null && Trm[0][0] == this.lastTrm[0][0] && Trm[0][1] == this.lastTrm[0][1] && Trm[1][0] == this.lastTrm[1][0]
				&& Trm[1][1] == this.lastTrm[1][1] && glyf.equals(this.lastGlyf)) {

			// work out absolute diffs
			float xDiff = Math.abs(Trm[2][0] - this.lastTrm[2][0]);
			float yDiff = Math.abs(Trm[2][1] - this.lastTrm[2][1]);

			// if does not look like new char, ignore
			float fontDiffAllowed = 1;
			if (xDiff < fontDiffAllowed && yDiff < fontDiffAllowed) {
				return;
			}
		}

		float x = (float) coords[0];
		float y = (float) coords[1];
		float rotX = x;
		float rotY = y;

		// needed for this case to ensure text runs across page
		if (this.pageRotation == 90) {
			rotX = Trm[2][1];
			rotY = Trm[2][0];
		}

		// Append new glyf to text block if we can otherwise flush it
		if (this.writeEveryGlyf
				|| !this.currentTextBlock.isSameFont(fontSize, this.fontMapper, Trm, color)
				|| !this.currentTextBlock.appendText(glyf, charWidth, rotX, rotY, this.groupGlyphsInTJ,
						(!this.groupGlyphsInTJ || this.currentTokenNumber != this.lastTokenNumber), x, y)) {

			flushText();
			this.currentTextPosition = new TextPosition(coords, new float[] { rawTrm[0][0], rawTrm[0][1], rawTrm[1][0], rawTrm[1][1], rawTrm[2][0],
					rawTrm[2][1] });

			// Set up new block, if it just a space disregard it.
			if (!glyf.equals(" ")) {
				float spaceWidth = fontSize * currentFontData.getCurrentFontSpaceWidth();

				this.currentTextBlock = new TextBlock(glyf, fontSize, this.fontMapper, Trm, x, y, charWidth, color, spaceWidth,
						this.cropBox.getBounds(), Trm, altFontSize, fontCondition, rotX, rotY, this.pageRotation);

				if (this.convertSpacesTonbsp) this.currentTextBlock.convertSpacesTonbsp(true);

				if (this.currentTextBlock.getRotationAngle() == 0) {
					this.currentTextBlock.adjustY(-fontSize);
				}
			}
			else {
				this.currentTextBlock = new TextBlock();
			}
		}

		// update incase changed
		this.lastTokenNumber = this.currentTokenNumber;
		this.lastGlyf = glyf;
		this.lastTrm = Trm;
	}

	private void rasterizeTextAsShape(PdfGlyph embeddedGlyph, GraphicsState gs, PdfFont currentFontData, String glyf) {

		// if(currentFontData.getBaseFontName().contains("FFAAHC+MSTT31ca9d00")){
		// System.out.println(fontSize+" "+glyf+" "+currentFontData.getBaseFontName()+" "+currentFontData);
		// }

		/**
		 * convert text to shape and draw shape instead Generally text at 1000x1000 matrix so we scale down by 1000 and then up by fontsize
		 */
		if (embeddedGlyph != null && embeddedGlyph.getShape() != null && !glyf.equals(" ")) {

			GraphicsState TextGs = gs;// new GraphicsState();

			// name we will store draw code under as routine
			String JSRoutineName;

			// check all chars letters or numbers and use int value if invalid
			boolean isInvalid = false;
			for (int aa = 0; aa < glyf.length(); aa++) {
				if (!Character.isLetterOrDigit(glyf.charAt(aa))) {
					isInvalid = true;
				}

				// exit if wrong
				if (isInvalid) break;
			}

			// fix for the occurance of . in fontIDs
			String safeFontID = (String) currentFontData.getFontID();
			safeFontID = StringUtils.makeMethodSafe(safeFontID);
			if (this.usedFontIDs.containsKey(safeFontID) && this.usedFontIDs.get(safeFontID) != currentFontData.getBaseFontName()) {
				// add extra fontID stuff
				safeFontID += StringUtils.makeMethodSafe(currentFontData.getBaseFontName());
			}
			else {
				this.usedFontIDs.put(safeFontID, currentFontData.getBaseFontName());
			}

			if (isInvalid) JSRoutineName = safeFontID + Integer.toHexString(glyf.charAt(0));
			else
				if (glyf.length() == 0) {
					JSRoutineName = safeFontID + '_' + embeddedGlyph.getID();
				}
				else {
					JSRoutineName = safeFontID + glyf;
				}

			// System.out.println(currentFontData.getBaseFontName() + " " + JSRoutineName + "  fontID= " + safeFontID);

			// ensure starts with letter
			if (!Character.isLetter(JSRoutineName.charAt(0))) JSRoutineName = 's' + JSRoutineName;

			// flag to show if generated
			String cacheKey = currentFontData.getBaseFontName() + '.' + JSRoutineName;

			// see if we have already decoded glyph and use that data to reduce filesize
			boolean isAlreadyDecoded = this.glyfsRasterized.containsKey(cacheKey);

			// get the glyph as textGlyf shape (which we already have to render)
			Area textGlyf = (Area) embeddedGlyph.getShape().clone();

			// useful to debug
			// textGlyf=new Area(new Rectangle(0,0,1000,1000));

			// adjust GS to work correctly
			TextGs.setClippingShape(null);
			TextGs.setFillType(gs.getTextRenderType());

			/**
			 * adjust scaling to factor in font size
			 */
			float d = (float) (1f / currentFontData.FontMatrix[0]);

			// allow for rescaled TT which work on a much larger grid
			if (textGlyf.getBounds().height > 2000) {
				d = d * 100;
			}

			// do the actual rendering
			writeCustom(SCRIPT, "pdf.save();");

			/**
			 * adjust placing
			 */
			writePosition(JSRoutineName, true, d);

			completeTextShape(gs, JSRoutineName);

			// generate the JS ONCE for each glyf
			if (!isAlreadyDecoded) {

				drawNonPatternedShape(textGlyf, TextGs, Cmd.Tj, JSRoutineName, null, null);
				this.glyfsRasterized.put(cacheKey, "x"); // flag it as now in file
			}

			completeRasterizedText();
		}
	}

	protected void completeTextShape(GraphicsState gs, String jsRoutineName) {
		throw new RuntimeException("method root completeTextShape(GraphicsState gs, String JSRoutineName) should not be called");
	}

	protected FontMapper getFontMapper(PdfFont currentFontData) {
		return null;
	}

	/* save shape in array to draw */

	@Override
	public void drawShape(Shape currentShape, GraphicsState gs, int cmd) {

		/** Start of missing line/pixel perfect fix **/
		if (gs.getFillType() == GraphicsState.FILL) {
			double x = currentShape.getBounds2D().getX();
			double y = currentShape.getBounds2D().getY();
			double width = currentShape.getBounds2D().getWidth();
			double height = currentShape.getBounds2D().getHeight();
			float lineWidth = gs.getCTMAdjustedLineWidth();

			if (height <= 1 && lineWidth <= 1) {
				gs.setFillType(GraphicsState.STROKE);
				gs.setStrokeColor(gs.getNonstrokeColor());
				gs.setCTMAdjustedLineWidth(0.1f);
				currentShape = new Line2D.Double(x, y, x + width, y);
			}

			if (width <= 1 && lineWidth <= 1) {
				gs.setFillType(GraphicsState.STROKE);
				gs.setStrokeColor(gs.getNonstrokeColor());
				gs.setCTMAdjustedLineWidth(0.1f);
				currentShape = new Line2D.Double(x, y, x, y + height);
			}
		}
		/** End of missing line/pixel perfect fix **/

		if (!isObjectVisible(currentShape.getBounds(), gs.getClippingShape())) {
			return;
		}

		// flush any cached text before we write out
		flushText();

		// turn pattern into an image
		if (this.emulateEvenOdd || gs.getNonstrokeColor().isPattern() || gs.nonstrokeColorSpace.getID() == ColorSpaces.Pattern) { // complex stuff

			drawPatternedShape(currentShape, gs);

		}
		else { // standard shapes

			drawNonPatternedShape(currentShape, gs, cmd, null, this.cropBox, this.midPoint);

		}
	}

	protected void drawNonPatternedShape(Shape currentShape, GraphicsState gs, int cmd, String name, Rectangle2D cropBox, Point2D midPoint) {}

	protected void drawPatternedShape(Shape currentShape, GraphicsState gs) {
		// This is how I have interpreted what the PDF spec says about shapes. Leon

		double iw = currentShape.getBounds2D().getWidth();
		double ih = currentShape.getBounds2D().getHeight();
		double ix = currentShape.getBounds2D().getX();
		double iy = currentShape.getBounds2D().getY();

		this.coords = new double[] { ix, iy };
		correctCoords(this.coords);

		int ixScaled = (int) (this.coords[0] * this.scaling);
		// The x plus width is the unrounded x position plus the width, then everything rounded up.
		int iwScaled = (int) ((((this.coords[0] + iw) - (int) this.coords[0]) * this.scaling) + 1);
		int iyScaled = (int) ((this.coords[1] - ih) * this.scaling);
		int ihScaled = (int) ((((this.coords[1] + ih) - (int) this.coords[1]) * this.scaling) + 1);

		if (iwScaled < 1 || ihScaled < 1) {
			return; // Invisible shading - THIS SHOULD BE IMPOSSIBLE GIVEN THE ABOVE CALCULATIONS.
		}

		BufferedImage img = new BufferedImage(iwScaled, ihScaled, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = img.createGraphics();
		AffineTransform aff = new AffineTransform();
		PdfPaint col;
		if (this.emulateEvenOdd) {
			col = gs.nonstrokeColorSpace.getColor();
		}
		else {
			col = gs.getNonstrokeColor();
		}

		aff.scale(this.scaling, this.scaling);
		aff.scale(1, -1);
		aff.translate(0, -ih);
		aff.translate(-ix, -iy);

		int pageH = this.pageData.getCropBoxHeight(this.pageNumber), pageY = 0;
		col.setScaling(0, pageH, this.scaling, 0, pageY);
		col.setRenderingType(DynamicVectorRenderer.CREATE_HTML);

		g2.setTransform(aff);
		g2.setPaint(col);
		if (this.emulateEvenOdd) {
			// Apply clip if there is one
			Area clipShape = gs.getClippingShape();
			if (clipShape != null) g2.clip(clipShape);
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); // Putting above the clip may help.
																										// chinese/lect...b22
		}
		g2.fill(currentShape);
		if (!this.emulateEvenOdd) g2.draw(currentShape);

		Rectangle2D shading = new Rectangle(ixScaled, iyScaled, iwScaled, ihScaled);
		Rectangle2D cropBoxScaled = new Rectangle.Double(this.cropBox.getX() * this.scaling, this.cropBox.getY() * this.scaling,
				this.cropBox.getWidth() * this.scaling, this.cropBox.getHeight() * this.scaling);

		// Check shade will be drawn on page.
		if (cropBoxScaled.intersects(shading)) {
			this.shadeId++;

			/*
			 * Crop image if it goes off page. boolean crop = false; int xxx = 0; int yyy = 0; if (ixScaled<0) { // Image goes off left xxx = ixScaled
			 * * -1; iwScaled = iwScaled+ixScaled; ixScaled = 0; crop = true; } if (ixScaled+iwScaled > cropBoxScaled.getWidth()) { // Image goes off
			 * right iwScaled = round(cropBoxScaled.getWidth() - ixScaledUnrounded, 0.5); crop = true; } if (ihScaled<0) { // Image goes off top yyy =
			 * iyScaled * -1; ihScaled = ihScaled+iyScaled; iyScaled = 0; crop = true; } if (iyScaled+ihScaled > cropBoxScaled.getHeight()) { // Image
			 * goes off bottom ihScaled = round(cropBoxScaled.getHeight() - iyScaledUnrounded, 0.5); crop = true; } if (crop) { if (xxx + iwScaled >
			 * img.getWidth()) { iwScaled = img.getWidth() - xxx; } if (yyy + ihScaled > img.getHeight()) { ihScaled = img.getHeight() - yyy; } img =
			 * img.getSubimage(xxx, yyy, iwScaled, ihScaled); } /*
			 */

			if (this.pageRotation == 90 || this.pageRotation == 270) {

				iyScaled = (int) (cropBoxScaled.getHeight() - iyScaled - ihScaled);
				// This needs to be calculated way up there. These X,Y,W,H values were calculated based on the rotation at 0.

				int tmp;
				tmp = iwScaled;
				iwScaled = ihScaled;
				ihScaled = tmp;

				tmp = ixScaled;
				ixScaled = iyScaled;
				iyScaled = tmp;

				img = rotateImage(img, this.pageRotation);
			}

			if (this.embedImageAsBase64) {
				// convert image into base64 content
				try {
					ByteArrayOutputStream bos = new ByteArrayOutputStream();
					javax.imageio.ImageIO.write(img, "PNG", bos);
					bos.close();
					this.imageArray = new sun.misc.BASE64Encoder().encode(bos.toByteArray());
					this.imageArray = this.imageArray.replace("\r\n", "\\\r\n"); // \ is required at EOLs as defined in JS
				}
				catch (IOException e) {
					// tell user and log
					if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
				}
			}
			else {
				String imageDir = this.fileName + "/shade/";
				File imgDir = new File(this.rootDir + imageDir);
				if (!imgDir.exists()) imgDir.mkdirs();

				// Store image.
				this.currentPatternedShapeName = this.customIO.writeImage(this.rootDir, imageDir + this.shadeId, img);
			}

			// These final values are used by all HTML, SVG, JavaFX, FXML, etc
			this.currentPatternedShape = new int[] { ixScaled, iyScaled, iwScaled, ihScaled };
		}
		else {
			this.currentPatternedShape = new int[] { -1, -1, -1, -1 };
		}
	}

	private boolean isObjectVisible(Rectangle bounds, Area clip) {

		if (!this.emulateEvenOdd && this.dx == 0 && this.dy == 0 && this.dr == 0) {
			// get any Clip (only rectangle outline)
			Rectangle clipBox;

			// This was already commented
			// if(dx!=0 || dy!=0) //factor in offset on crop
			// bounds.translate(-dx, -dy);

			if (clip != null) clipBox = clip.getBounds();
			else clipBox = null;

			// If shape is outside of the clip or crop boxes, shape is not visible.
			// I have chosen to do a manual check rather than use .intersects().
			int boundsStartX = bounds.x;
			int boundsStartY = bounds.y;
			int boundsEndX = bounds.width + boundsStartX;
			int boundsEndY = bounds.height + boundsStartY;
			if (this.cropBox != null) {
				double cropStartX = this.cropBox.getBounds2D().getX();
				double cropStartY = this.cropBox.getBounds2D().getY();
				double cropEndX = this.cropBox.getBounds2D().getWidth() + cropStartX;
				double cropEndY = this.cropBox.getBounds2D().getHeight() + cropStartY;

				if (boundsEndX < cropStartX || boundsStartX > cropEndX || boundsEndY < cropStartY || boundsStartY > cropEndY) {
					// System.out.println("CROP: " + boundsEndX + " < " + cropStartX + " || " + boundsStartX + " > " + cropEndX + " || " + boundsEndY
					// + " < " + cropStartX + " || " + boundsStartY + " > " + cropEndY);
					return false;
				}
			}
			if (clipBox != null) {
				int clipStartX = clipBox.x;
				int clipStartY = clipBox.y;
				int clipEndX = clipBox.width + clipStartX;
				int clipEndY = clipBox.height + clipStartY;

				if (boundsEndX < clipStartX || boundsStartX > clipEndX || boundsEndY < clipStartY || boundsStartY > clipEndY) {
					// System.out.println("CLIP: " + boundsEndX + " < " + clipStartX + " || " + boundsStartX + " > " + clipEndX + " || " + boundsEndY
					// + " < " + clipStartX + " || " + boundsStartY + " > " + clipEndY);
					return false;
				}
			}

		}

		return true;
	}

	/* add XForm object */

	@Override
	final public void drawXForm(DynamicVectorRenderer dvr, GraphicsState gs) {

		// flush any cached text before we write out
		flushText();

		renderXForm(dvr, gs.getAlpha(GraphicsState.STROKE));
	}

	/**
	 * add footer and other material to complete
	 */
	protected void completeOutput() {}

	@Override
	public void setOutputDir(String outputDir, String outputFilename, String pageNumberAsString) {
		this.rootDir = outputDir;
		this.fileName = outputFilename;

		this.pageNumberAsString = pageNumberAsString;
	}

	/**
	 * Coverts an array of numbers to a String for JavaScript parameters.
	 * 
	 * @param coords
	 *            Numbers to change
	 * @param count
	 *            Use up to count doubles from coords array
	 * @return String Bracketed stringified version of coords
	 */
	protected String coordsToStringParam(double[] coords, int count) {
		String result = "";

		for (int i = 0; i < count; i++) {
			if (i != 0) {
				result += ",";
			}

			result += setPrecision(coords[i]);
		}

		return result;
	}

	/**
	 * Converts coords from Pdf system to java.
	 */
	protected void correctCoords(double[] coords) {
		coords[0] = coords[0] - this.midPoint.getX();
		coords[0] += this.cropBox.getWidth() / 2;

		coords[1] = coords[1] - this.midPoint.getY();
		coords[1] = 0 - coords[1];
		coords[1] += this.cropBox.getHeight() / 2;
	}

	/**
	 * Formats an int to CSS rgb(r,g,b) string
	 * 
	 */
	protected static String rgbToColor(int raw) {
		int r = (raw >> 16) & 255;
		int g = (raw >> 8) & 255;
		int b = raw & 255;

		return "rgb(" + r + ',' + g + ',' + b + ')';
	}

	/**
	 * Used in FXML and SVG for now. *
	 * 
	 * @param rgb
	 *            - The raw RGB values
	 * @return The Hex equivalents
	 */
	// RGB converted to Hex color
	public static String hexColor(int rgb) {

		String hexColor;

		hexColor = Integer.toHexString(rgb);
		hexColor = hexColor.substring(2, 8);
		hexColor = '#' + hexColor;

		// System.out.println(currentTextBlock.getColor()+" "+Integer.toHexString(currentTextBlock.getColor())+" "+hexColor);

		return hexColor;
	}

	/**
	 * Add current date
	 * 
	 * @return returns date as string in 21 - November - 2011 format
	 */

	public static String getDate() {

		DateFormat dateFormat = new SimpleDateFormat("dd - MMMMM - yyyy"); // get current date

		Date date = new Date();
		// System.out.println(dateFormat.format(date));
		return dateFormat.format(date);
	}

	/**
	 * Draws boxes around where the text should be.
	 */
	protected void drawTextArea() {}

	/**
	 * Draw a debug area around border of page.
	 */
	protected void drawPageBorder() {}

	/**
	 * allow tracking of specific commands
	 **/
	@Override
	public void flagCommand(int commandID, int tokenNumber) {

		switch (commandID) {

			case Cmd.BT:

				// reset to will be rest for text
				// lastR=-1;
				// lastG=-1;
				// lastB=-1;
				break;

			case Cmd.Tj:
				this.currentTokenNumber = tokenNumber;
				break;
		}
	}

	protected String replaceTokenValues(String name) {

		// Strip the file name from the root dir
		String nameOfPDF = this.rootDir.substring(0, this.rootDir.length() - 1); // strip off last / or \

		// find end (which could be after \ or /)
		int pt = nameOfPDF.lastIndexOf('\\');
		int fowardSlash = nameOfPDF.lastIndexOf('/');
		if (fowardSlash > pt) pt = fowardSlash;

		// and remove path from filename
		nameOfPDF = nameOfPDF.substring(pt + 1);

		// all tokens start $ so ignore if not present
		if (name != null && name.contains("$")) {

			// possible replacement values
			String fileName = nameOfPDF;
			String pageCount = String.valueOf(this.pageData.getPageCount());

			// do the replacements
			name = name.replace("$filename$", fileName);
			name = name.replace("$pagecount$", pageCount);

		}

		return name;
	}

	@Override
	public boolean isScalingControlledByUser() {
		return this.userControlledImageScaling;
	}

	/**
	 * Used in JavaFx and FXML to convert pdf font weight to the JavaFX/FXML equivalent
	 * 
	 * @param weight
	 *            - pdf weight that needs converting to javaFx
	 * @return JavaFx / FXML equivalent weight
	 */
	protected static String setJavaFxWeight(String weight) {
		String javaFxWeight = "";
		if (weight.equals("normal")) javaFxWeight = "NORMAL";
		else
			if (weight.equals("bold")) javaFxWeight = "BOLD";
			else
				if (weight.equals("bolder")) javaFxWeight = "BLACK";
				else
					if (weight.equals("lighter")) javaFxWeight = "EXTRA_LIGHT";
					else
						if (weight.equals("100")) javaFxWeight = "THIN";
						else
							if (weight.equals("900")) javaFxWeight = "BLACK";

		return javaFxWeight;
	}

	/**
	 * Write out text buffer in correct format
	 */
	protected void flushText() {
		if (DEBUG_TEXT_AREA) {
			drawTextArea();
		}

		if (this.currentTextBlock == null || this.currentTextBlock.isEmpty()) {
			return;
		}

		writeoutTextAsDiv(getFontScaling());

		// Reset current block and store the last one so we dont have to repeat javascript code later
		if (!this.currentTextBlock.isEmpty()) {
			this.previousTextBlock = this.currentTextBlock;
		}
		this.currentTextBlock = new TextBlock();
	}

	private float getFontScaling() {

		float fontScaling = 0;
		/**
		 * work out font scaling needed
		 */
		float[] aff = this.currentTextPosition.getRawAffine();

		if (aff[2] == 0) {
			fontScaling = aff[3];
		}
		else
			if (aff[3] == 0) {
				fontScaling = aff[2];
			}
			else {
				fontScaling = (float) Math.sqrt((aff[2] * aff[2]) + (aff[3] * aff[3]));
			}
		fontScaling = Math.abs(fontScaling);

		return fontScaling; // To change body of created methods use File | Settings | File Templates.
	}

	private float getFontScalingMarksNewVersion() {

		float fontScaling = 0;
		/**
		 * work out font scaling needed
		 */
		float[] aff = this.currentTextPosition.getRawAffine();

		if (aff[0] == 0) {
			System.out.println("1");
			fontScaling = aff[1];
		}
		else
			if (aff[1] == 0) {
				System.out.println("2");
				fontScaling = aff[0];
			}
			else {
				System.out.println("3");
				fontScaling = (float) Math.sqrt((aff[2] * aff[2]) + (aff[3] * aff[3]));
			}
		fontScaling = Math.abs(fontScaling);

		return fontScaling;
	}

	protected void writeoutTextAsDiv(float fontScaling) {
	}

	protected void writePosition(String JSRoutineName, boolean isShape, float fontScaling) {

		float[] aff = new float[4]; // will hold matrix

		double[] coords = this.currentTextPosition.getCoords();

		// calc text co-ords
		double tx, ty;

		// ////
		/**
		 * factor in page rotation here and adjust txt,ty
		 * 
		 * see http://en.wikipedia.org/wiki/Rotation_matrix https://groups.google.com/forum/?fromgroups=#!topic/libharu/CPZy7kT9PQI for explanation of
		 * rotations
		 * 
		 * The div and rast methods work slightly differently, hence 2 methods
		 */

		float[][] rotatedTrm = new float[3][3];

		// if(currentTextBlock.getOutputString(true).contains("Revenue Growth"))
		// System.out.println(currentTextBlock.getOutputString(true));

		switch (this.pageRotation) {
			default:
				rotatedTrm = this.currentTextPosition.getTrm();

				tx = (coords[0] * this.scaling);
				ty = (coords[1] * this.scaling);

				aff[0] = rotatedTrm[0][0];
				aff[1] = rotatedTrm[0][1];
				aff[2] = rotatedTrm[1][0];
				aff[3] = rotatedTrm[1][1];

				/**
				 * cords work slightly differently for canvas and text (canvas is from orgin, text is corner of text div) so routines not the same
				 */
				if (isShape) { // simple version used for shape, no scaling
					if (aff[3] != 0) {
						aff[3] = -aff[3];
					}
				}
				else { // text div version

					if (this.type != CREATE_SVG && this.type != CREATE_JAVAFX) {
						tx = (tx - (aff[1] * this.scaling));
						ty = (ty - (aff[3] * this.scaling));
					}

					if (aff[2] != 0) {
						aff[2] = -aff[2];
					}
				}

				// both versions need this tweak
				if (aff[1] != 1) {
					aff[1] = -aff[1];
				}

				break;

			case 90:

				ty = (coords[0] * this.scaling);
				tx = ((this.cropBox.getHeight() - coords[1]) * this.scaling);

				float[][] rotated;
				if (isShape) {
					rotated = Matrix.multiply(this.currentTextPosition.getTrm(), new float[][] { { -1, 0, 0 }, { 0, 1, 0 }, { 0, 0, 1 } });
				}
				else {
					rotated = Matrix.multiply(new float[][] { { 0, 1, 0 }, { -1, 0, 0 }, { 0, 0, 1 } }, this.currentTextPosition.getTrm());
				}

				aff[0] = rotated[0][0];
				aff[1] = rotated[0][1];
				aff[2] = rotated[1][0];
				aff[3] = rotated[1][1];

				if (!isShape) {

					if (aff[0] < 0 && aff[3] < 0) {
						aff[0] = -aff[0];
						aff[3] = -aff[3];
					}

					tx = (tx + (aff[1] * this.scaling));

					ty = (ty - (aff[3] * this.scaling));
				}

				break;

			case 180:

				tx = ((this.cropBox.getWidth() - coords[0]) * this.scaling);
				ty = ((this.cropBox.getHeight() - coords[1]) * this.scaling);

				rotatedTrm = Matrix.multiply(this.currentTextPosition.getTrm(), new float[][] { { 1, 0, 0 }, { 0, -1, 0 }, { 0, 0, 1 } });
				aff[0] = rotatedTrm[0][0];
				aff[1] = rotatedTrm[0][1];
				aff[2] = rotatedTrm[1][0];
				aff[3] = rotatedTrm[1][1];

				// text needs turning upside down
				if (!isShape) {
					aff[0] = -aff[0];
					ty = (ty - (aff[3] * this.scaling));
				}
				break;

			case 270:

				ty = ((this.cropBox.getWidth() - coords[0]) * this.scaling);
				tx = ((coords[1]) * this.scaling);

				if (isShape) {
					rotated = Matrix.multiply(this.currentTextPosition.getTrm(), new float[][] { { -1, 0, 0 }, { 0, 1, 0 }, { 0, 0, 1 } });
				}
				else {
					rotated = Matrix.multiply(this.currentTextPosition.getTrm(), new float[][] { { 0, -1, 0 }, { 1, 0, 0 }, { 0, 0, 1 } });
				}

				aff[0] = rotated[0][0];
				aff[1] = rotated[0][1];
				aff[2] = rotated[1][0];
				aff[3] = rotated[1][1];

				if (!isShape) {
					if (aff[0] < 0 && aff[3] < 0) {
						aff[0] = -aff[0];
						aff[3] = -aff[3];
					}
					tx = (tx + (aff[1] * this.scaling));
				}

				break;
		}

		for (int ii = 0; ii < 4; ii++) {
			if (aff[ii] == -0.0) {
				aff[ii] = 0.0f;
			}
		}

		// useful debug code
		// System.out.println(fontScaling+" "+pageRotation+" "+" "+aff[0]+" "+aff[1]+" "+aff[2]+" "+aff[3]+" "+" "+currentTextBlock.getOutputString(true));

		/**
		 * in both cases we have a simple version for left to write text and a transform for anything more complicated
		 */
		if (isShape) {
			writeRasterizedTextPosition(JSRoutineName, aff, (int) tx, (int) ty, fontScaling);
		}
		else {
			writeTextPosition(aff, (int) tx, (int) ty, fontScaling);
		}
	}

	protected void writeTextPosition(float[] aff, int tx, int ty, float scaling) {
		throw new RuntimeException("writeTextPosition(float[] aff, int tx, int ty, int scaling)");
	}

	protected void writeRasterizedTextPosition(String JSRoutineName, float[] aff, int tx, int ty, float fontScaling) {
		throw new RuntimeException("method root writeRasterizedTextPosition(String JSRoutineName, float[] aff, int tx, int ty) should not be called");
	}

	protected void completeRasterizedText() {
		throw new RuntimeException("completeRasterizedText");
	}

	protected static String tidy(float val) {
		return removeEmptyDecimals(String.valueOf(val));
	}

	private static String removeEmptyDecimals(String numberValue) {
		// remove any .0000)
		int ptr = numberValue.indexOf('.');
		if (ptr > -1) {
			boolean onlyZeros = true;
			int len = numberValue.length();
			for (int ii = ptr + 1; ii < len; ii++) { // test if . followed by just zeros
				if (numberValue.charAt(ii) != '0') {
					onlyZeros = false;
					ii = len;
				}
			}

			// if so remove
			if (onlyZeros) {
				numberValue = numberValue.substring(0, ptr);
			}
		}

		return numberValue;
	}

	@Override
	public void saveAdvanceWidth(String fontName, String glyphName, int width) {

		fontName = fontName.replace('+', '-');

		HashMap<String, Integer> font = this.widths.get(fontName);

		if (font == null) {
			font = new HashMap<String, Integer>();
			this.widths.put(fontName, font);
		}

		font.put(glyphName, width);
	}

	/**
	 * Work out page number to use in the Href tag
	 * 
	 * @param pageNumber
	 *            Page number to go to.
	 * @return Page number as string appends initial "0" if needed
	 */
	public String getPageAsHTMLRef(int pageNumber) {
		// validate
		if (pageNumber < 1) pageNumber = 1;
		if (pageNumber > this.endPage) pageNumber = this.endPage;

		// convert to string
		String pageAsString = String.valueOf(pageNumber);

		// include option to call page one something else
		if (this.firstPageName != null && pageNumber == 1) {
			pageAsString = this.firstPageName;
		}
		else {
			// add required zeros
			String maxNumberOfPages = String.valueOf(this.endPage);
			int padding = maxNumberOfPages.length() - pageAsString.length();
			for (int ii = 0; ii < padding; ii++) {
				pageAsString = '0' + pageAsString;
			}
		}

		return pageAsString;
	}

	/*
	 * getMagazineAsHTMLRef has been created so that it can be reused for nav bars and init() methods.
	 */
	protected String getMagazinePageAsHTMLRef(int pageNumber) {
		if (pageNumber == 1) {
			return getPageAsHTMLRef(pageNumber);
		}
		else
			if (pageNumber % 2 == 0) {
				if (pageNumber == this.endPage) {
					return getPageAsHTMLRef(pageNumber);
				}
				else {
					return getPageAsHTMLRef(pageNumber) + "-" + getPageAsHTMLRef(pageNumber + 1);
				}
			}
			else {
				return getPageAsHTMLRef(pageNumber - 1) + "-" + getPageAsHTMLRef(pageNumber);
			}
	}

	@Override
	public boolean avoidDownSamplingImage() {
		return this.keepOriginalImage;
	}

}
