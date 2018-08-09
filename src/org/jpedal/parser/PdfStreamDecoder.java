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
 * PdfStreamDecoder.java
 * ---------------
 */
package org.jpedal.parser;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.jpedal.PdfDecoder;
import org.jpedal.color.ColorSpaces;
import org.jpedal.color.GenericColorSpace;
import org.jpedal.color.PdfPaint;
import org.jpedal.constants.PDFImageProcessing;
import org.jpedal.constants.PageInfo;
import org.jpedal.exception.PdfException;
import org.jpedal.external.ExternalHandlers;
import org.jpedal.external.ImageHandler;
import org.jpedal.external.Options;
import org.jpedal.fonts.FontMappings;
import org.jpedal.fonts.PdfFont;
import org.jpedal.fonts.StandardFonts;
import org.jpedal.fonts.glyph.T3Size;
import org.jpedal.images.SamplingFactory;
import org.jpedal.io.ErrorTracker;
import org.jpedal.io.ObjectStore;
import org.jpedal.io.PdfObjectReader;
import org.jpedal.io.StatusBar;
import org.jpedal.objects.GraphicsState;
import org.jpedal.objects.PdfData;
import org.jpedal.objects.PdfImageData;
import org.jpedal.objects.PdfPageData;
import org.jpedal.objects.PdfShape;
import org.jpedal.objects.TextState;
import org.jpedal.objects.layers.PdfLayerList;
import org.jpedal.objects.raw.FontObject;
import org.jpedal.objects.raw.FormObject;
import org.jpedal.objects.raw.PdfArrayIterator;
import org.jpedal.objects.raw.PdfDictionary;
import org.jpedal.objects.raw.PdfObject;
import org.jpedal.render.DynamicVectorRenderer;
import org.jpedal.render.SwingDisplay;
import org.jpedal.render.output.OutputDisplay;
import org.jpedal.utils.LogWriter;
import org.jpedal.utils.Matrix;
import org.jpedal.utils.repositories.Vector_Int;
import org.jpedal.utils.repositories.Vector_Object;
import org.jpedal.utils.repositories.Vector_Rectangle;

/**
 * Contains the code which 'parses' the commands in the stream and extracts the data (images and text). Users should not need to call it.
 */
public class PdfStreamDecoder extends BaseDecoder {

	protected GraphicsState newGS = null;

	protected byte[] pageStream = null;

	PdfLayerList layers;

	protected boolean getSamplingOnly = false;

	TextState currentTextState = new TextState();

	private Map shadingColorspacesObjects = new HashMap(50);

	/** flag to show if stack setup */
	private boolean isStackInitialised = false;

	/** stack for graphics states */
	private Vector_Object graphicsStateStack;

	/** stack for graphics states */
	private Vector_Object strokeColorStateStack;

	/** stack for graphics states */
	private Vector_Object nonstrokeColorStateStack;

	/** stack for graphics states */
	private Vector_Object textStateStack;

	private boolean isTTHintingRequired = false;

	private Vector_Int textDirections = new Vector_Int();

	private Vector_Rectangle textAreas = new Vector_Rectangle();

	/** shows if t3 glyph uses internal colour or current colour */
	public boolean ignoreColors = false;

	// trap for recursive loop of xform calling itself
	int lastDataPointer = -1;

	private T3Decoder t3Decoder = null;

	/** flag to show if we REMOVE shapes */
	private boolean removeRenderImages = false;

	/** flags to show we need colour data as well */
	private boolean textColorExtracted = false, colorExtracted = false;

	/** flag to show text is being extracted */
	private boolean textExtracted = true;

	/** flag to show content is being rendered */
	private boolean renderText = false;

	/**
	 * if forms flattened, different calculation needed
	 */
	private boolean isFlattenedForm = false;
	private float flattenX = 0, flattenY = 0;

	/** list of images used for display */
	private String imagesInFile = null;

	// set threshold - value indicates several possible values
	public static float currentThreshold = 0.595f;

	private boolean flattenXFormToImage = false;

	private boolean requestTimeout = false;

	private int timeoutInterval = -1;

	protected ImageHandler customImageHandler = null;

	private PdfFontFactory pdfFontFactory;

	private boolean isXMLExtraction = false;

	/** interactive display */
	private StatusBar statusBar = null;

	private boolean markedContentExtracted = false;

	/** store text data and can be passed out to other classes */
	private PdfData pdfData = new PdfData();

	/** store image data extracted from pdf */
	private PdfImageData pdfImages = new PdfImageData();

	/** used to debug */
	protected static String indent = "";

	/** show if possible error in stream data */
	protected boolean isDataValid = true;

	/** used to store font information from pdf and font functionality */
	private PdfFont currentFontData;

	/** flag to show we use hi-res images to draw onscreen */
	protected boolean useHiResImageForDisplay = false;

	protected ObjectStore objectStoreStreamRef;

	private String formName = "";

	protected boolean isType3Font;

	public static boolean useTextPrintingForNonEmbeddedFonts = false;

	static {
		SamplingFactory.setDownsampleMode(null);
	}

	public PdfStreamDecoder(PdfObjectReader currentPdfFile) {

		init(currentPdfFile);
	}

	/**
	 * create new StreamDecoder to create screen display with hires images
	 */
	public PdfStreamDecoder(PdfObjectReader currentPdfFile, boolean useHiResImageForDisplay, PdfLayerList layers) {

		if (layers != null) this.layers = layers;

		init(currentPdfFile);
	}

	private void init(PdfObjectReader currentPdfFile) {

		this.cache = new PdfObjectCache();
		this.gs = new GraphicsState();
		this.layerDecoder = new LayerDecoder();
		this.errorTracker = new ErrorTracker();
		this.pageData = new PdfPageData();

		StandardFonts.checkLoaded(StandardFonts.STD);
		StandardFonts.checkLoaded(StandardFonts.MAC);

		this.currentPdfFile = currentPdfFile;
		this.pdfFontFactory = new PdfFontFactory(currentPdfFile);
	}

	/**
	 * 
	 * objects off the page, stitch into a stream and decode and put into our data object. Could be altered if you just want to read the stream
	 * 
	 * @param pageStream
	 * @throws PdfException
	 */
	public final T3Size decodePageContent(GraphicsState newGS, byte[] pageStream) throws PdfException {

		this.newGS = newGS;
		this.pageStream = pageStream;

		return decodePageContent(null);
	}

	/**
	 * 
	 * objects off the page, stitch into a stream and decode and put into our data object. Could be altered if you just want to read the stream
	 * 
	 * @param pdfObject
	 * @throws PdfException
	 */
	public T3Size decodePageContent(PdfObject pdfObject) throws PdfException {

		try {

			// check switched off
			this.imagesProcessedFully = true;

			// reset count
			this.imageCount = 0;

			this.isTimeout = false;

			this.layerDecoder.setPdfLayerList(this.layers);

			// reset count
			this.imagesInFile = null; // also reset here as good point as syncs with font code

			if (!this.renderDirectly && this.statusBar != null) this.statusBar.percentageDone = 0;

			if (this.newGS != null) this.gs = this.newGS;
			else this.gs = new GraphicsState(0, 0);

			// save for later
			if (this.renderPage) {

				/**
				 * check setup and throw exception if null
				 */
				if (this.current == null) throw new PdfException("DynamicVectorRenderer not setup PdfStreamDecoder setStore(...) should be called");

				this.current.drawClip(this.gs, this.defaultClip, false);

				// Paint background here to ensure we all for changed background color in extraction modes
				this.current.paintBackground(new Rectangle(this.pageData.getCropBoxX(this.pageNum), this.pageData.getCropBoxY(this.pageNum),
						this.pageData.getCropBoxWidth(this.pageNum), this.pageData.getCropBoxHeight(this.pageNum)));
			}

			// get the binary data from the file
			byte[] b_data;

			byte[][] pageContents = null;
			if (pdfObject != null) {
				pageContents = pdfObject.getKeyArray(PdfDictionary.Contents);
				this.isDataValid = pdfObject.streamMayBeCorrupt();
			}

			// get any page grouping obj
			if (pdfObject == null) this.cache.pageGroupingObj = null;
			else this.cache.pageGroupingObj = pdfObject.getDictionary(PdfDictionary.Group);

			if (pdfObject != null && pageContents == null) b_data = this.currentPdfFile.readStream(pdfObject, true, true, false, false, false,
					pdfObject.getCacheName(this.currentPdfFile.getObjectReader()));
			else
				if (this.pageStream != null) b_data = this.pageStream;
				else b_data = this.currentPdfFile.getObjectReader().readPageIntoStream(pdfObject);

			// trap for recursive loop of xform calling itself
			this.lastDataPointer = -1;

			// if page data found, turn it into a set of commands
			// and decode the stream of commands
			if (b_data != null && b_data.length > 0) decodeStreamIntoObjects(b_data, false);

			// flush fonts
			if (!this.isType3Font) this.cache.resetFonts();

			T3Size t3 = new T3Size();
			if (this.t3Decoder != null) {
				t3.x = this.t3Decoder.T3maxWidth;
				t3.y = this.t3Decoder.T3maxHeight;
				this.ignoreColors = this.t3Decoder.ignoreColors;
				this.t3Decoder = null;
			}

			return t3;

		}
		catch (Error err) {

			if (ExternalHandlers.throwMissingCIDError && err.getMessage().contains("kochi")) throw err;

			this.errorTracker.addPageFailureMessage("Problem decoding page " + err);

		}

		return null;
	}

	/**
	 * routine to decode an XForm stream
	 */
	public void drawFlattenedForm(PdfObject form) throws PdfException {

		this.isFlattenedForm = true;

		// check if this form should be displayed
		boolean[] characteristic = ((FormObject) form).getCharacteristics();
		if (characteristic[0] || characteristic[1] || characteristic[5]
				|| (form.getBoolean(PdfDictionary.Open) == false && form.getParameterConstant(PdfDictionary.Subtype) == PdfDictionary.Popup)) {
			// this form should be hidden
			return;
		}

		PdfObject imgObj = null;

		PdfObject APobjN = form.getDictionary(PdfDictionary.AP).getDictionary(PdfDictionary.N);
		Map otherValues = new HashMap();
		if (APobjN != null) {
			otherValues = APobjN.getOtherDictionaries();
		}

		String defaultState = form.getName(PdfDictionary.AS);
		if (defaultState != null && defaultState.equals(((FormObject) form).getNormalOnState())) {
			// use the selected appearance stream
			if (APobjN.getDictionary(PdfDictionary.On) != null) {
				imgObj = APobjN.getDictionary(PdfDictionary.On);
			}
			else
				if (APobjN.getDictionary(PdfDictionary.Off) != null && defaultState != null && defaultState.equals("Off")) {
					imgObj = APobjN.getDictionary(PdfDictionary.Off);
				}
				else
					if (otherValues != null && defaultState != null) {

						imgObj = (PdfObject) otherValues.get(defaultState);
					}
					else {
						if (otherValues != null && !otherValues.isEmpty()) {
							Iterator keys = otherValues.keySet().iterator();
							PdfObject val;
							String key;
							// while(keys.hasNext()){
							key = (String) keys.next();
							val = (PdfObject) otherValues.get(key);
							// System.out.println("key="+key+" "+val.getName(PdfDictionary.AS));
							imgObj = val;
							// }
						}
					}
		}
		else {
			// use the normal appearance Stream
			if (APobjN != null || form.getDictionary(PdfDictionary.MK).getDictionary(PdfDictionary.I) != null) {

				// if we have a root stream then it is the off value
				// check in order of N Off, MK I, then N
				// as N Off overrides others and MK I is in preference to N
				if (APobjN != null && APobjN.getDictionary(PdfDictionary.Off) != null) {
					imgObj = APobjN.getDictionary(PdfDictionary.Off);

				}
				else
					if (form.getDictionary(PdfDictionary.MK).getDictionary(PdfDictionary.I) != null
							&& form.getDictionary(PdfDictionary.MK).getDictionary(PdfDictionary.IF) == null) {
						// look here for MK IF
						// if we have an IF inside the MK then use the MK I as some files shown that this value is there
						// only when the MK I value is not as important as the AP N.
						imgObj = form.getDictionary(PdfDictionary.MK).getDictionary(PdfDictionary.I);

					}
					else
						if (APobjN != null && APobjN.getDecodedStream() != null) {
							imgObj = APobjN;
						}
			}
		}

		if (imgObj == null) return;

		this.currentPdfFile.checkResolved(imgObj);
		byte[] formData = imgObj.getDecodedStream(); // get from the AP
		// debug code for mark, for the flattern case 10295
		// System.out.println("ref="+form.getObjectRefAsString()+" stream="+new String(formData));

		// might be needed to pick up fonts
		PdfObject resources = imgObj.getDictionary(PdfDictionary.Resources);
		readResources(resources, false);

		/**
		 * see if bounding box and set
		 */
		float[] BBox = form.getFloatArray(PdfDictionary.Rect);

		// if we flatten form objects with XForms, we need to use diff calculation
		if (this.isFlattenedForm) {
			this.flattenX = BBox[0];
			this.flattenY = BBox[1];
		}

		// please dont delete through merge this fixes most of the flatten form positionsing.
		float[] matrix = imgObj.getFloatArray(PdfDictionary.Matrix);

		// we need to factor in this to calculations
		int pageRotation = this.pageData.getRotation(this.pageNum);

		// System.out.println("pageRot="+pageRotation);
		// System.out.println("BBox="+BBox[0]+" "+BBox[1]+" "+BBox[2]+" "+BBox[3]);
		// System.out.println("matrix="+matrix[0]+" "+matrix[1]+" "+matrix[2]+" "+matrix[3]);
		float x = BBox[0], y = BBox[1];
		Area newClip = null;

		// check for null and then recalculate insets
		if (matrix != null) {
			switch (pageRotation) {
				case 90:
					// change commented out below as breaks test file baselineSpecific/flattenForms/ny1891.pdf
					x = BBox[2];

					// Added code to fix ny1981.pdf and 133419-without-annotations-p2.pdf
					if (matrix[4] < 0) x = BBox[0] + matrix[4];
					// newClip=new Area(new Rectangle((int)BBox[2],(int)BBox[1],(int)BBox[0],(int)BBox[3]));
					break;
				default:
					x = BBox[0] + matrix[4];
					newClip = new Area(new Rectangle((int) BBox[0], (int) BBox[1], (int) BBox[2], (int) BBox[3]));
					break;
			}
			y = BBox[1] + matrix[5];

			float xScale = 1;
			float yScale = 1;

			// Check for appearnce stream
			PdfObject temp = form.getDictionary(PdfDictionary.AP);
			if (temp != null) {

				// Check for N object
				temp = temp.getDictionary(PdfDictionary.N);
				if (temp != null) {

					// Check for a bounding box of this object
					float[] BoundingBox = temp.getFloatArray(PdfDictionary.BBox);
					if (BoundingBox != null) {

						// If different from BB provided and matrix is standard than add scaling
						if (BBox[0] != BoundingBox[0] && BBox[1] != BoundingBox[1] && BBox[2] != BoundingBox[2] && BBox[3] != BoundingBox[3] &&
						// Check matrix is standard
								matrix[0] * matrix[3] == 1.0f && matrix[1] * matrix[2] == 0.0f) {

							float bbw = BBox[2] - BBox[0];
							float bbh = BBox[3] - BBox[1];
							float imw = BoundingBox[2] - BoundingBox[0];
							float imh = BoundingBox[3] - BoundingBox[1];

							// Adjust scale on the x to fit form size
							if ((int) bbw != (int) imw) {
								xScale = bbw / imw;

								// @KIERAN :: If position issue on non rotated page, check here first
								// Move form up instead of drawing top at bottom of area
								x -= (imw * xScale);
							}

							// Adjust scale on the y to fit form size
							if ((int) bbh != (int) imh) {
								yScale = bbh / imh;
							}

						}
					}
				}
			}
			// set gs.CTM to form coords (probably {1,0,0}{0,1,0}{x,y,1} at a guess
			this.gs.CTM = new float[][] { { matrix[0] * xScale, matrix[1], 0 }, { matrix[2], matrix[3] * yScale, 0 }, { x, y, 1 } };

		}
		else {
			this.gs.CTM = new float[][] { { 1, 0, 0 }, { 0, 1, 0 }, { x, y, 1 } };
			newClip = new Area(new Rectangle((int) BBox[0], (int) BBox[1], (int) BBox[2], (int) BBox[3]));
		}

		// set clip to match bounds on form
		if (newClip != null) this.gs.updateClip(new Area(newClip));

		this.current.drawClip(this.gs, this.defaultClip, false);

		/** decode the stream */
		setBooleanValue(IsFlattenedForm, this.isFlattenedForm);
		decodeStreamIntoObjects(formData, false);

		/**
		 * we need to reset clip otherwise items drawn afterwards like forms data in image or print will not appear.
		 */
		this.gs.updateClip(null);
		this.current.drawClip(this.gs, null, true);
	}

	@Override
	public void setObjectValue(int key, Object obj) {

		switch (key) {

			case ValueTypes.Name:
				setName((String) obj);
				break;

			case ValueTypes.DynamicVectorRenderer:
				this.current = (DynamicVectorRenderer) obj;
				// flag OCR used
				boolean isOCR = (this.renderMode & PdfDecoder.OCR_PDF) == PdfDecoder.OCR_PDF;
				if (isOCR && this.current != null) this.current.setOCR(true);
				break;

			case ValueTypes.PDFPageData:
				this.pageData = (PdfPageData) obj;
				// flag if colour info being extracted
				if (this.textColorExtracted) this.pdfData.enableTextColorDataExtraction();

				break;

			/**
			 * pass in status bar object
			 * 
			 */
			case ValueTypes.StatusBar:
				this.statusBar = (StatusBar) obj;
				break;

			case ValueTypes.PdfLayerList:
				this.layers = (PdfLayerList) obj;
				break;

			case ValueTypes.ImageHandler:
				this.customImageHandler = (ImageHandler) obj;
				if (this.customImageHandler != null && this.current != null) this.current.setCustomImageHandler(this.customImageHandler);
				break;

			/**
			 * setup stream decoder to render directly to g2 (used by image extraction)
			 */
			case ValueTypes.DirectRendering:

				this.renderDirectly = true;
				Graphics2D g2 = (Graphics2D) obj;
				this.defaultClip = g2.getClip();

				break;

			/**
			 * should be called after constructor or other methods may not work
			 * <p>
			 * Also initialises DynamicVectorRenderer
			 */
			case ValueTypes.ObjectStore:
				this.objectStoreStreamRef = (ObjectStore) obj;

				this.current = new SwingDisplay(this.pageNum, this.objectStoreStreamRef, false);
				this.current.setHiResImageForDisplayMode(this.useHiResImageForDisplay);

				if (this.customImageHandler != null && this.current != null) this.current.setCustomImageHandler(this.customImageHandler);

				break;

			default:
				super.setObjectValue(key, obj);

		}
	}

	/**
	 * flag to show interrupted by user
	 */
	private boolean isTimeout = false;

	boolean isPrinting = false;

	/**
	 * NOT PART OF API tells software to generate glyph when first rendered not when decoded. Should not need to be called in general usage
	 */
	@Override
	public void setBooleanValue(int key, boolean value) {

		switch (key) {

			case IsPrinting:
				this.isPrinting = value;
				break;

			case ValueTypes.XFormFlattening:
				this.flattenXFormToImage = value;
				break;

			default:
				super.setBooleanValue(key, value);
		}
	}

	/**/

	/** used internally to allow for colored streams */
	public void setDefaultColors(PdfPaint strokeCol, PdfPaint nonstrokeCol) {

		this.gs.strokeColorSpace.setColor(strokeCol);
		this.gs.nonstrokeColorSpace.setColor(nonstrokeCol);
		this.gs.setStrokeColor(strokeCol);
		this.gs.setNonstrokeColor(nonstrokeCol);
	}

	/** return the data */
	@Override
	public Object getObjectValue(int key) {

		switch (key) {
			case ValueTypes.PDFData:
				if (PdfDecoder.embedWidthData) this.pdfData.widthIsEmbedded();

				// store page width/height so we can translate 270
				// rotation co-ords
				this.pdfData.maxX = this.pageData.getMediaBoxWidth(this.pageNum);
				this.pdfData.maxY = this.pageData.getMediaBoxHeight(this.pageNum);

				return this.pdfData;

			case ValueTypes.PDFImages:
				return this.pdfImages;

			case ValueTypes.TextAreas:
				return this.textAreas;

			case ValueTypes.TextDirections:
				return this.textDirections;

			case ValueTypes.DynamicVectorRenderer:
				return this.current;

			case PdfDictionary.Font:
				return this.pdfFontFactory.getFontsInFile();

			case PdfDictionary.Image:
				return this.imagesInFile;

			case DecodeStatus.NonEmbeddedCIDFonts:
				return this.pdfFontFactory.getnonEmbeddedCIDFonts();

			case PageInfo.COLORSPACES:
				return this.cache.iterator(PdfObjectCache.ColorspacesUsed);

			default:
				return super.getObjectValue(key);

		}
	}

	/**
	 * read page header and extract page metadata
	 * 
	 * @throws PdfException
	 */
	public final void readResources(PdfObject Resources, boolean resetList) throws PdfException {

		if (resetList) this.pdfFontFactory.resetfontsInFile();

		this.cache.readResources(Resources, resetList, this.currentPdfFile);
	}

	/**
	 * decode the actual 'Postscript' stream into text and images by extracting commands and decoding each.
	 */
	public String decodeStreamIntoObjects(byte[] stream, boolean returnText) {

		if (stream.length == 0) return null;

		// start of Dictionary on Inline image
		int startInlineStream = 0;

		long startTime = System.currentTimeMillis();

		CommandParser parser = new CommandParser(stream);
		this.parser = parser;

		int streamSize = stream.length, dataPointer = 0, startCommand = 0;

		// used in CS to avoid miscaching
		String csInUse = "", CSInUse = "";

		PdfShape currentDrawShape = new PdfShape();

		// setup textDecoder
		TextDecoder textDecoder;
		if (this.markedContentExtracted) textDecoder = new TextDecoder(this.layerDecoder);
		else {
			textDecoder = new TextDecoder(this.pdfData, this.isXMLExtraction, this.layerDecoder);
			textDecoder.setReturnText(returnText);
		}

		if (this.errorTracker != null) textDecoder.setHandlerValue(Options.ErrorTracker, this.errorTracker);
		textDecoder.setParameters(this.isPageContent, this.renderPage, this.renderMode, this.extractionMode, this.isPrinting);
		textDecoder.setFileHandler(this.currentPdfFile);
		textDecoder.setIntValue(FormLevel, this.formLevel);
		textDecoder.setIntValue(TextPrint, this.textPrint);
		textDecoder.setBooleanValue(RenderDirectly, this.renderDirectly);
		textDecoder.setBooleanValue(GenerateGlyphOnRender, this.generateGlyphOnRender);
		textDecoder.setRenderer(this.current);

		if (!this.renderDirectly && this.statusBar != null) {
			this.statusBar.percentageDone = 0;
			this.statusBar.resetStatus("stream");
		}

		/**
		 * loop to read stream and decode
		 */
		while (true) {

			// allow user to request exit and fail page
			if (this.requestTimeout || (this.timeoutInterval != -1 && System.currentTimeMillis() - startTime > this.timeoutInterval)) {
				this.requestTimeout = false;
				this.timeoutInterval = -1;
				this.isTimeout = true;

				break;
			}

			if (!this.renderDirectly && this.statusBar != null) this.statusBar.percentageDone = (90 * dataPointer) / streamSize;

			dataPointer = parser.getCommandValues(dataPointer, streamSize, this.tokenNumber);
			int commandID = parser.getCommandID();

			// use negative flag to show commands found
			if (dataPointer < 0) {

				dataPointer = -dataPointer;
				try {

					/**
					 * call method to handle commands
					 */
					int commandType = Cmd.getCommandType(commandID);

					/**
					 * text commands first and all other commands if not found in first
					 **/
					switch (commandType) {

						case Cmd.TEXT_COMMAND:

							if ((commandID == Cmd.EMC || this.layerDecoder.isLayerVisible()) && !this.getSamplingOnly
									&& (this.renderText || this.textExtracted)) {

								textDecoder.setCommands(parser);
								textDecoder.setGS(this.gs);
								textDecoder.setTextState(this.currentTextState);
								textDecoder.setIntValue(TokenNumber, this.tokenNumber);

								if (this.renderPage && commandID == Cmd.BT) {
									// save for later and set TR
									this.current.drawClip(this.gs, this.defaultClip, true);
									this.current.drawTR(GraphicsState.FILL);

									// flag text block started
									this.current.flagCommand(Cmd.BT, this.tokenNumber);
								}

								if (commandID == Cmd.Tj || commandID == Cmd.TJ || commandID == Cmd.quote || commandID == Cmd.doubleQuote) {

									// flag which TJ command we are on
									this.current.flagCommand(Cmd.Tj, this.tokenNumber);

									if (this.currentTextState.hasFontChanged()) {

										// switch to correct font
										String fontID = this.currentTextState.getFontID();
										PdfFont restoredFont = resolveFont(fontID);
										if (restoredFont != null) {
											this.currentFontData = restoredFont;
											this.current.drawFontBounds(this.currentFontData.getBoundingBox());
										}
									}

									if (this.currentFontData == null) {
										this.currentFontData = new PdfFont(this.currentPdfFile);

										// use name for poss mappings (ie Helv)
										this.currentFontData.getGlyphData().logicalfontName = StandardFonts.expandName(this.currentTextState
												.getFontID());
									}

									if (this.currentTextState.hasFontChanged()) {
										this.currentTextState.setFontChanged(false);
									}

									textDecoder.setFont(this.currentFontData);
								}

								dataPointer = textDecoder.processToken(this.currentTextState, commandID, startCommand, dataPointer);

							}
							break;

						case Cmd.SHAPE_COMMAND:

							if (!this.getSamplingOnly) {
								switch (commandID) {

									case Cmd.B:
										if (!this.removeRenderImages) {
											Shape currentShape = ShapeCommands.B(false, false, this.gs, this.formLevel, currentDrawShape,
													this.layerDecoder, this.renderPage, this.current);
										}
										break;

									case Cmd.b:
										if (!this.removeRenderImages) {
											Shape currentShape = ShapeCommands.B(false, true, this.gs, this.formLevel, currentDrawShape,
													this.layerDecoder, this.renderPage, this.current);
										}
										break;

									case Cmd.bstar:
										if (!this.removeRenderImages) {
											Shape currentShape = ShapeCommands.B(true, true, this.gs, this.formLevel, currentDrawShape,
													this.layerDecoder, this.renderPage, this.current);
										}
										break;

									case Cmd.Bstar:
										if (!this.removeRenderImages) {
											Shape currentShape = ShapeCommands.B(true, false, this.gs, this.formLevel, currentDrawShape,
													this.layerDecoder, this.renderPage, this.current);
										}
										break;

									case Cmd.c:
										float x3 = parser.parseFloat(1);
										float y3 = parser.parseFloat(0);
										float x2 = parser.parseFloat(3);
										float y2 = parser.parseFloat(2);
										float x = parser.parseFloat(5);
										float y = parser.parseFloat(4);
										currentDrawShape.addBezierCurveC(x, y, x2, y2, x3, y3);
										break;

									case Cmd.d:
										ShapeCommands.D(parser, this.gs);
										break;

									case Cmd.F:
										if (!this.removeRenderImages) F(false, this.formLevel, currentDrawShape);
										break;

									case Cmd.f:
										if (!this.removeRenderImages) F(false, this.formLevel, currentDrawShape);
										break;

									case Cmd.Fstar:
										if (!this.removeRenderImages) F(true, this.formLevel, currentDrawShape);
										break;

									case Cmd.fstar:
										if (!this.removeRenderImages) F(true, this.formLevel, currentDrawShape);
										break;

									case Cmd.h:
										currentDrawShape.closeShape();
										break;

									// case Cmd.i:
									// I();
									// break;

									case Cmd.J:
										ShapeCommands.J(false, parser.parseInt(0), this.gs);
										break;

									case Cmd.j:
										ShapeCommands.J(true, parser.parseInt(0), this.gs);
										break;

									case Cmd.l:
										currentDrawShape.lineTo(parser.parseFloat(1), parser.parseFloat(0));
										break;

									case Cmd.M:
										this.gs.setMitreLimit((int) (parser.parseFloat(0)));
										break;

									case Cmd.m:
										currentDrawShape.moveTo(parser.parseFloat(1), parser.parseFloat(0));
										break;

									case Cmd.n:
										ShapeCommands.N(currentDrawShape, this.gs, this.formLevel, this.defaultClip, this.renderPage, this.current,
												this.pageData, this.pageNum);
										break;

									case Cmd.re:
										currentDrawShape.appendRectangle(parser.parseFloat(3), parser.parseFloat(2), parser.parseFloat(1),
												parser.parseFloat(0));
										break;

									case Cmd.S:
										if (!this.removeRenderImages) {
											Shape currentShape = ShapeCommands.S(false, this.layerDecoder, this.gs, currentDrawShape, this.current,
													this.renderPage);
										}
										break;
									case Cmd.s:
										if (!this.removeRenderImages) {
											Shape currentShape = ShapeCommands.S(true, this.layerDecoder, this.gs, currentDrawShape, this.current,
													this.renderPage);
										}
										break;

									case Cmd.v:
										currentDrawShape.addBezierCurveV(parser.parseFloat(3), parser.parseFloat(2), parser.parseFloat(1),
												parser.parseFloat(0));
										break;

									case Cmd.w:
										this.gs.setLineWidth(parser.parseFloat(0));
										break;

									case Cmd.Wstar: // set Winding rule
										currentDrawShape.setEVENODDWindingRule();
										currentDrawShape.setClip(true);
										break;

									case Cmd.W:
										currentDrawShape.setNONZEROWindingRule();
										currentDrawShape.setClip(true);
										break;

									case Cmd.y:
										currentDrawShape.addBezierCurveY(parser.parseFloat(3), parser.parseFloat(2), parser.parseFloat(1),
												parser.parseFloat(0));
										break;
								}
							}

							break;

						case Cmd.SHADING_COMMAND:

							if (!this.getSamplingOnly && (this.renderPage || this.textColorExtracted || this.colorExtracted)) {

								if (this.renderPage) {
									ShadingCommands.sh(parser.generateOpAsString(0, true), this.cache, this.gs, this.isPrinting,
											this.shadingColorspacesObjects, this.pageNum, this.currentPdfFile, this.pageData, this.current);
								}
							}

							break;

						case Cmd.COLOR_COMMAND:

							if (!this.getSamplingOnly && (this.renderPage || this.textColorExtracted || this.colorExtracted)) {
								if (commandID != Cmd.SCN && commandID != Cmd.scn && commandID != Cmd.SC && commandID != Cmd.sc) this.current
										.resetOnColorspaceChange();

								switch (commandID) {

									case Cmd.cs: {
										String colorspaceObject = parser.generateOpAsString(0, true);
										boolean isLowerCase = true;
										// ensure if used for both Cs and cs simultaneously we only cache one version and do not overwrite
										boolean alreadyUsed = (!isLowerCase && colorspaceObject.equals(csInUse))
												|| (isLowerCase && colorspaceObject.equals(CSInUse));

										if (isLowerCase) csInUse = colorspaceObject;
										else CSInUse = colorspaceObject;

										ColorCommands.CS(isLowerCase, colorspaceObject, this.gs, this.cache, this.currentPdfFile, this.isPrinting,
												this.pageNum, this.pageData, alreadyUsed);
										break;
									}
									case Cmd.CS:

										String colorspaceObject = parser.generateOpAsString(0, true);
										boolean isLowerCase = false;
										// ensure if used for both Cs and cs simultaneously we only cache one version and do not overwrite
										boolean alreadyUsed = (!isLowerCase && colorspaceObject.equals(csInUse))
												|| (isLowerCase && colorspaceObject.equals(CSInUse));

										if (isLowerCase) csInUse = colorspaceObject;
										else CSInUse = colorspaceObject;

										ColorCommands.CS(isLowerCase, colorspaceObject, this.gs, this.cache, this.currentPdfFile, this.isPrinting,
												this.pageNum, this.pageData, alreadyUsed);
										break;

									case Cmd.rg:
										ColorCommands.RG(true, this.gs, parser, this.cache);
										break;

									case Cmd.RG:
										ColorCommands.RG(false, this.gs, parser, this.cache);
										break;

									case Cmd.SCN:
										ColorCommands.SCN(false, this.gs, parser, this.cache);
										break;

									case Cmd.scn:
										ColorCommands.SCN(true, this.gs, parser, this.cache);
										break;

									case Cmd.SC:
										ColorCommands.SCN(false, this.gs, parser, this.cache);
										break;

									case Cmd.sc:
										ColorCommands.SCN(true, this.gs, parser, this.cache);
										break;

									case Cmd.g:
										ColorCommands.G(true, this.gs, parser, this.cache);
										break;

									case Cmd.G:
										ColorCommands.G(false, this.gs, parser, this.cache);
										break;

									case Cmd.k:
										ColorCommands.K(true, this.gs, parser, this.cache);
										break;

									case Cmd.K:
										ColorCommands.K(false, this.gs, parser, this.cache);
										break;

								}

							}

							break;

						case Cmd.GS_COMMAND:

							switch (commandID) {

								case Cmd.cm:

									CM(this.gs, parser);
									break;

								case Cmd.q:
									this.gs = Q(this.gs, true);
									break;

								case Cmd.Q:
									this.gs = Q(this.gs, false);
									break;

								case Cmd.gs:
									if (!this.getSamplingOnly) {
										PdfObject GS = (PdfObject) this.cache.GraphicsStates.get(parser.generateOpAsString(0, true));

										this.currentPdfFile.checkResolved(GS);

										this.gs.setMode(GS);

										this.current.setGraphicsState(GraphicsState.FILL, this.gs.getAlpha(GraphicsState.FILL));
										this.current.setGraphicsState(GraphicsState.STROKE, this.gs.getAlpha(GraphicsState.STROKE));
									}

									break;

							}

							// may have changed so read back and reset
							this.gs.setTextState(this.currentTextState);
							if (commandID == Cmd.cm && textDecoder != null) textDecoder.reset();

							break;

						case Cmd.IMAGE_COMMAND:

							if (commandID == Cmd.BI) {
								startInlineStream = dataPointer;
							}
							else {

								ImageDecoder imageDecoder;
								PdfObject XObject = null;
								int subtype = 1;
								if (commandID == Cmd.Do) {

									String name = parser.generateOpAsString(0, true);
									byte[] rawData = null;

									XObject = this.cache.getXObjects(name);
									if (XObject != null) {

										rawData = XObject.getUnresolvedData();

										this.currentPdfFile.checkResolved(XObject);

										subtype = XObject.getParameterConstant(PdfDictionary.Subtype);
									}

									if (subtype == PdfDictionary.Form) {

										if (this.formLevel > 10 && dataPointer == this.lastDataPointer) {
											// catch for odd files like customers-June2011/results.pdf
										}
										else {
											this.lastDataPointer = dataPointer;

											processXForm(dataPointer, XObject, this.defaultClip, parser);
											//Lonzak: Bad idea (=buggy implementation) of developer: causes complete area's to disappear
											// if lots of objects in play turn back to ref to save memory
											//if (rawData != null && this.cache.getXObjectCount() > 30) {
											//	String ref = XObject.getObjectRefAsString();
											//	
											//	this.cache.resetXObject(name, ref, rawData);
											//	XObject = null;
											//
											//}
										}
									}
								}

								if (subtype != PdfDictionary.Form) {

									imageDecoder = new ImageDecoder(this.customImageHandler, this.objectStoreStreamRef, this.renderDirectly,
											this.pdfImages, this.formLevel, this.pageData, this.imagesInFile, this.formName);

									imageDecoder.setIntValue(PageNumber, this.pageNum);
									imageDecoder.setIntValue(FormLevel, this.formLevel);
									imageDecoder.setHandlerValue(Options.ErrorTracker, this.errorTracker);
									imageDecoder.setRes(this.cache);
									imageDecoder.setGS(this.gs);
									imageDecoder.setSamplingOnly(this.getSamplingOnly);
									imageDecoder.setIntValue(ValueTypes.StreamType, this.streamType);
									imageDecoder.setName(this.fileName);
									imageDecoder.setFloatValue(Multiplier, this.multiplyer);
									imageDecoder.setFloatValue(SamplingUsed, this.samplingUsed);
									imageDecoder.setFileHandler(this.currentPdfFile);
									imageDecoder.setRenderer(this.current);
									imageDecoder.setIntValue(IsImage, this.imageStatus);

									imageDecoder.setParameters(this.isPageContent, this.renderPage, this.renderMode, this.extractionMode,
											this.isPrinting, this.isType3Font, this.useHiResImageForDisplay);

									imageDecoder.setIntValue(ImageCount, this.imageCount);

									if (commandID == Cmd.Do) {

										// size test to remove odd lines in abacus file abacus/EP_Print_Post_Suisse_ID_120824.pdf
										if (XObject == null
												|| !this.layerDecoder.isLayerVisible()
												|| (this.layers != null && !this.layers.isVisible(XObject))
												|| (this.gs.CTM != null && this.gs.CTM[1][1] == 0 && this.gs.CTM[1][0] != 0 && Math
														.abs(this.gs.CTM[1][0]) < 0.2))
										;// ignore
										else dataPointer = imageDecoder.processDOImage(parser.generateOpAsString(0, true), dataPointer, XObject);
									}
									else
										if (this.layerDecoder.isLayerVisible()) dataPointer = imageDecoder.processIDImage(dataPointer,
												startInlineStream, parser.getStream(), this.tokenNumber);

									this.samplingUsed = imageDecoder.getFloatValue(SamplingUsed);

									this.imageCount = imageDecoder.getIntValue(ImageCount);

									this.imagesInFile = imageDecoder.getImagesInFile();

									if (imageDecoder.getBooleanValue(HasYCCKimages)) this.hasYCCKimages = true;

									if (imageDecoder.getBooleanValue(ImagesProcessedFully)) this.imagesProcessedFully = true;
								}
							}
							break;

						case Cmd.T3_COMMAND:

							if (!this.getSamplingOnly && (this.renderText || this.textExtracted)) {

								if (this.t3Decoder == null) this.t3Decoder = new T3Decoder();

								this.t3Decoder.setCommands(parser);
								this.t3Decoder.setCommands(parser);
								this.t3Decoder.processToken(commandID);

							}
							break;
					}
				}
				catch (Exception e) {

					if (LogWriter.isOutput()) LogWriter.writeLog("[PDF] " + e + " Processing token >" + Cmd.getCommandAsString(commandID) + "<>"
							+ this.fileName + " <" + this.pageNum);

					// only exit if no issue with stream
					if (this.isDataValid) {}
					else dataPointer = streamSize;

					// cascade up
					if (e.getMessage() != null && e.getMessage().contains("JPeg 2000")) {
						throw new RuntimeException("JPeg 2000 Images needs the VM parameter -Dorg.jpedal.jai=true switch turned on");
					}
				}
				catch (OutOfMemoryError ee) {
					this.errorTracker.addPageFailureMessage("Memory error decoding token stream");

					if (LogWriter.isOutput()) LogWriter.writeLog("[MEMORY] Memory error - trying to recover");
				}

				// save for next command
				startCommand = dataPointer;

				// reset array of trailing values
				parser.reset();

				// increase pointer
				this.tokenNumber++;
			}

			// break at end
			if (streamSize <= dataPointer) break;
		}

		if (!this.renderDirectly && this.statusBar != null) this.statusBar.percentageDone = 100;

		// pick up TextDecoder values
		this.isTTHintingRequired = textDecoder.isTTHintingRequired();
		this.textAreas = (Vector_Rectangle) textDecoder.getObjectValue(ValueTypes.TextAreas);
		this.textDirections = (Vector_Int) textDecoder.getObjectValue(ValueTypes.TextDirections);

		return "";
	}

	/**
	 * decode or get font
	 * 
	 * @param fontID
	 */
	private PdfFont resolveFont(String fontID) {

		PdfFont restoredFont = (PdfFont) this.cache.resolvedFonts.get(fontID);

		// check it was decoded
		if (restoredFont == null) {

			// String ref=(String)cache.unresolvedFonts.get(fontID);
			PdfObject newFont = (PdfObject) this.cache.unresolvedFonts.get(fontID);
			if (newFont == null) {
				this.cache.directFonts.remove(fontID);
			}

            /**
             * in Flatten forms, if font not in our resources, we need to create one based on name
             * Otherwise we will not switch from whatever is last being used on page (and might have custom value)
             */
            if(this.isFlattenedForm && newFont==null){

                String name= StandardFonts.expandName(fontID.replace(",", "-"));

                //if font not present then use a replacement
                if(FontMappings.fontSubstitutionAliasTable.get(name)==null && FontMappings.fontSubstitutionTable!=null && FontMappings.fontSubstitutionTable.get(name)==null){
                    final String rawName=name.toLowerCase();
                    if(rawName.contains("bold")) {
                        name = "Arial-Bold";
                    } else if(rawName.contains("italic")) {
                        name = "Arial-Italic";
                    } else {
                        name = "Arial";
                    }
                }

                newFont=new FontObject("1 0 R");
                fontID=StandardFonts.expandName(name); //turns common shortened versions used in AP (ie Helv to Helvetica)
                newFont.setName(PdfDictionary.BaseFont,name);
                newFont.setName(PdfDictionary.FontName,name);
                newFont.setConstant(PdfDictionary.Subtype, StandardFonts.TRUETYPE);
            }

			if (newFont != null) {
				
				this.currentPdfFile.checkResolved(newFont);
				
				try {
					
					org.jpedal.render.DynamicVectorRenderer current= this.current;
                    final org.jpedal.render.DynamicVectorRenderer possibleHTMLDVR= (org.jpedal.render.DynamicVectorRenderer) current.getObjectValue(org.jpedal.render.output.OutputDisplay.DVR);
                    if(possibleHTMLDVR!=null) {
                        current = possibleHTMLDVR;
                    }
                    
                    boolean fallbackToArial=false;
                    
                    final boolean isHTML=org.jpedal.render.BaseDisplay.isHTMLorSVG(current);
                    
                    /** if text as shape or image, display Arial if font not embedded*/
                    if(isHTML && !current.getBooleanValue(OutputDisplay.IsRealText)){
                        fallbackToArial=true;
                    }
					
					restoredFont = this.pdfFontFactory.createFont(fallbackToArial, newFont, fontID, this.objectStoreStreamRef, this.renderPage, this.errorTracker,
							this.isPrinting);

					// <start-pro>
					/**
					 * //<end-pro>
					 * 
					 * //<start-std>
					 * 
					 * String fontName=restoredFont.getFontName();
					 * 
					 * //default option most of the time except invisible text on IMAGE in HTML DynamicVectorRenderer current=this.current;
					 * 
					 * // we need to swap for invisible text on HTML DynamicVectorRenderer htmlRenderer= (DynamicVectorRenderer)
					 * getObjectValue(ValueTypes.HTMLInvisibleTextHandler); if(htmlRenderer!=null){ current= htmlRenderer; }
					 * 
					 * int mode=current.getValue(org.jpedal.render.output.html.HTMLDisplay.FontMode); int type=current.getType();
					 * 
					 * if((type== org.jpedal.render.output.html.HTMLDisplay.CREATE_HTML || type==
					 * org.jpedal.render.output.html.HTMLDisplay.CREATE_JAVAFX || type== org.jpedal.render.output.html.HTMLDisplay.CREATE_SVG) &&
					 * (mode== GenericFontMapper.EMBED_ALL || (mode== GenericFontMapper.EMBED_ALL_EXCEPT_BASE_FAMILIES &&
					 * !StandardFonts.isStandardFont(restoredFont.getFontName(),true) && !fontName.contains("Arial")))){ //check for base fonts
					 * (explict Arial test for ArialMT)
					 * 
					 * PdfObject pdfFontDescriptor=newFont.getDictionary(PdfDictionary.FontDescriptor);
					 * 
					 * //if null check to see if it is a CIF font and get data from DescendantFonts obj if (pdfFontDescriptor== null ) { PdfObject
					 * Descendent=newFont.getDictionary(PdfDictionary.DescendantFonts); if(Descendent!=null)
					 * pdfFontDescriptor=Descendent.getDictionary(PdfDictionary.FontDescriptor); }
					 * 
					 * //write out any embedded font file data
					 * 
					 * if (pdfFontDescriptor!= null && current.getValue(org.jpedal.render.output.OutputDisplay.TextMode)!=
					 * OutputDisplay.TEXT_AS_SHAPE){
					 * 
					 * byte[] stream; PdfObject FontFile2=pdfFontDescriptor.getDictionary(PdfDictionary.FontFile2);
					 * 
					 * if(FontFile2!=null){ //truetype fonts stream=currentPdfFile.readStream(FontFile2,true,true,false, false,false,
					 * FontFile2.getCacheName(currentPdfFile.getObjectReader()));
					 * current.writeCustom(org.jpedal.render.output.html.HTMLDisplay.SAVE_EMBEDDED_FONT, new
					 * Object[]{restoredFont,stream,"ttf",fontID}); }else{ PdfObject
					 * FontFile3=pdfFontDescriptor.getDictionary(PdfDictionary.FontFile3); if(FontFile3!=null){ //type1c fonts
					 * restoredFont.getGlyphData().setRenderer(current); stream=currentPdfFile.readStream(FontFile3,true,true,false, false,false,
					 * FontFile3.getCacheName(currentPdfFile.getObjectReader()));
					 * current.writeCustom(org.jpedal.render.output.html.HTMLDisplay.SAVE_EMBEDDED_FONT, new
					 * Object[]{restoredFont,stream,"cff",fontID}); }else{
					 * 
					 * PdfObject FontFile=pdfFontDescriptor.getDictionary(PdfDictionary.FontFile);
					 * 
					 * if(FontFile!=null && OutputDisplay.convertT1Fonts){ //type1 fonts restoredFont.getGlyphData().setRenderer(current);
					 * stream=currentPdfFile.readStream(FontFile,true,true,false, false,false,
					 * FontFile.getCacheName(currentPdfFile.getObjectReader()));
					 * current.writeCustom(org.jpedal.render.output.html.HTMLDisplay.SAVE_EMBEDDED_FONT, new
					 * Object[]{restoredFont,stream,"t1",fontID}); } } } } }
					 * 
					 * //<end-std> /
					 **/
				}
				catch (PdfException e) {
					// tell user and log
					if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
				}
			}

			// store
			if (restoredFont != null && !this.isFlattenedForm) this.cache.resolvedFonts.put(fontID, restoredFont);

		}

		return restoredFont;
	}

	/**
	 * return boolean flags with appropriate ket
	 */
	@Override
	public boolean getBooleanValue(int key) {

		switch (key) {

			case ValueTypes.EmbeddedFonts:
				return this.pdfFontFactory.hasEmbeddedFonts();

			case DecodeStatus.PageDecodingSuccessful:
				return this.errorTracker.pageSuccessful;

			case DecodeStatus.NonEmbeddedCIDFonts:
				return this.pdfFontFactory.hasNonEmbeddedCIDFonts();

			case DecodeStatus.ImagesProcessed:
				return this.imagesProcessedFully;

			case DecodeStatus.YCCKImages:
				return this.hasYCCKimages;

			case DecodeStatus.Timeout:
				return this.isTimeout;

			case DecodeStatus.TTHintingRequired:
				return this.isTTHintingRequired;

			default:
				throw new RuntimeException("Unknown value " + key);
		}
	}

	public void dispose() {

		if (this.pdfData != null) this.pdfData.dispose();

		// this.pageLines=null;
	}

	/**
	 * private class TestShapeTracker implements ShapeTracker { public void addShape(int tokenNumber, int type, Shape currentShape, PdfPaint
	 * nonstrokecolor, PdfPaint strokecolor) {
	 * 
	 * //use this to see type //Cmd.getCommandAsString(type);
	 * 
	 * //print out details if(type==Cmd.S || type==Cmd.s){ //use stroke color to draw line
	 * System.out.println("-------Stroke-------PDF cmd="+Cmd.getCommandAsString(type));
	 * System.out.println("tokenNumber="+tokenNumber+" "+currentShape.getBounds()+" stroke color="+strokecolor);
	 * 
	 * }else if(type==Cmd.F || type==Cmd.f || type==Cmd.Fstar || type==Cmd.fstar){ //uses fill color to fill shape
	 * System.out.println("-------Fill-------PDF cmd="+Cmd.getCommandAsString(type));
	 * System.out.println("tokenNumber="+tokenNumber+" "+currentShape.getBounds()+" fill color="+nonstrokecolor);
	 * 
	 * }else{ //not yet implemented (probably B which is S and F combo) System.out.println("Not yet added");
	 * System.out.println("tokenNumber="+tokenNumber+" "+currentShape.getBounds()+" type="+type+" "+Cmd.getCommandAsString(type)); } } }
	 * 
	 * /** request exit from main loop
	 */
	public void reqestTimeout(Object value) {

		if (value == null) this.requestTimeout = true;
		else
			if (value instanceof Integer) {
				this.timeoutInterval = (Integer) value;
			}
	}

	@Override
	public void setIntValue(int key, int value) {

		switch (key) {

		/**
		 * currentPage number
		 */
			case ValueTypes.PageNum:
				this.pageNum = value;
				break;

			/**
			 * tells program to try and use Java's font printing if possible as work around for issue with PCL printing
			 */
			case TextPrint:
				this.textPrint = value;
				break;

			default:
				super.setIntValue(key, value);
		}
	}

	public void setXMLExtraction(boolean isXMLExtraction) {
		this.isXMLExtraction = isXMLExtraction;
	}

	@Override
	public void setParameters(boolean isPageContent, boolean renderPage, int renderMode, int extractionMode) {

		super.setParameters(isPageContent, renderPage, renderMode, extractionMode);

		/**
		 * flags
		 */

		this.renderText = renderPage && (renderMode & PdfDecoder.RENDERTEXT) == PdfDecoder.RENDERTEXT;

		this.textExtracted = (extractionMode & PdfDecoder.TEXT) == PdfDecoder.TEXT;

		this.textColorExtracted = (extractionMode & PdfDecoder.TEXTCOLOR) == PdfDecoder.TEXTCOLOR;

		this.colorExtracted = (extractionMode & PdfDecoder.COLOR) == PdfDecoder.COLOR;

		this.removeRenderImages = renderPage && (renderMode & PdfDecoder.REMOVE_RENDERSHAPES) == PdfDecoder.REMOVE_RENDERSHAPES;
	}

	/**
	 * recursive subroutine so in actual body of PdfStreamDecoder so it can recall decodeStream
	 * 
	 * @param dataPointer
	 */
	private void processXForm(int dataPointer, PdfObject XObject, Shape defaultClip, CommandParser parser) throws PdfException {

		final boolean debug = false;

		if (debug) System.out.println("processImage " + dataPointer + ' ' + XObject.getObjectRefAsString() + ' ' + defaultClip);

		if (!this.layerDecoder.isLayerVisible() || (this.layers != null && !this.layers.isVisible(XObject)) || XObject == null) return;

		String oldFormName = this.formName;

		String name = parser.generateOpAsString(0, true);

		// name is not unique if in form so we add form level to separate out
		if (this.formLevel > 1) name = this.formName + '_' + this.formLevel + '_' + name;

		// string to hold image details
		String details = name;

		try {

			if (ImageCommands.trackImages) {
				// add details to string so we can pass back
				if (this.imagesInFile == null) this.imagesInFile = details + " Form";
				else this.imagesInFile = details + " Form\n" + this.imagesInFile;
			}

			// reset operand
			parser.reset();

			// read stream for image
			byte[] objectData = this.currentPdfFile.readStream(XObject, true, true, false, false, false,
					XObject.getCacheName(this.currentPdfFile.getObjectReader()));
			if (objectData != null) {

				String oldIndent = PdfStreamDecoder.indent;
				PdfStreamDecoder.indent = PdfStreamDecoder.indent + "   ";

				// set value and see if Transform matrix
				float[] transformMatrix = new float[6];
				float[] matrix = XObject.getFloatArray(PdfDictionary.Matrix);
				boolean isIdentity = matrix == null || isIdentity(matrix);
				if (matrix != null) transformMatrix = matrix;

				float[][] CTM, oldCTM = null;

				// allow for stroke line width being altered by scaling
				float lineWidthInForm = -1; // negative values not used

				if (matrix != null && !isIdentity) {

					// save current
					float[][] currentCTM = new float[3][3];
					for (int i = 0; i < 3; i++)
						System.arraycopy(this.gs.CTM[i], 0, currentCTM[i], 0, 3);

					oldCTM = currentCTM;

					CTM = this.gs.CTM;

					float[][] scaleFactor = { { transformMatrix[0], transformMatrix[1], 0 }, { transformMatrix[2], transformMatrix[3], 0 },
							{ transformMatrix[4], transformMatrix[5], 1 } };

					scaleFactor = Matrix.multiply(scaleFactor, CTM);
					this.gs.CTM = scaleFactor;

					// work out line width
					lineWidthInForm = transformMatrix[0] * this.gs.getLineWidth();

					if (lineWidthInForm == 0) lineWidthInForm = transformMatrix[1] * this.gs.getLineWidth();

					if (lineWidthInForm < 0) lineWidthInForm = -lineWidthInForm;

					if (debug) System.out.println("setMatrix " + this.gs.CTM[0][0] + ' ' + this.gs.CTM[0][1] + ' ' + this.gs.CTM[1][0] + ' '
							+ this.gs.CTM[1][1] + ' ' + this.gs.CTM[2][0] + ' ' + this.gs.CTM[2][1]);
				}

				// track depth
				this.formLevel++;

				// track name so we can make unique key for image name
				if (this.formLevel == 1) this.formName = name;
				else this.formName = this.formName + '_' + name;

				// preserve colorspaces
				GenericColorSpace mainStrokeColorData = (GenericColorSpace) this.gs.strokeColorSpace.clone();
				GenericColorSpace mainnonStrokeColorData = (GenericColorSpace) this.gs.nonstrokeColorSpace.clone();

				// set form line width if appropriate
				if (lineWidthInForm > 0) this.gs.setLineWidth(lineWidthInForm);

				// set gs max to current so child gs values can not exceed
				float maxStrokeValue = this.gs.getAlphaMax(GraphicsState.STROKE);
				float maxFillValue = this.gs.getAlphaMax(GraphicsState.FILL);
				this.gs.setMaxAlpha(GraphicsState.STROKE, this.gs.getAlpha(GraphicsState.STROKE));

				if (this.formLevel == 1) this.gs.setMaxAlpha(GraphicsState.FILL, this.gs.getAlpha(GraphicsState.FILL));

				// make a copy s owe can restore to original state
				// we need to pass in and then undo any changes at end
				PdfObjectCache mainCache = this.cache.copy(); // setup cache
				this.cache.reset(mainCache); // copy in data

				/** read any resources */
				PdfObject Resources = XObject.getDictionary(PdfDictionary.Resources);
				readResources(Resources, false);

				/** read any resources */
				this.cache.groupObj = XObject.getDictionary(PdfDictionary.Group);
				this.currentPdfFile.checkResolved(this.cache.groupObj);

				/**
				 * see if bounding box and set
				 */
				float[] BBox = XObject.getFloatArray(PdfDictionary.BBox);
				Area clip = null;
				boolean clipChanged = false;

				// this code breaks customers-june2011/169351.pdf so added as possible fix
				if (BBox != null && BBox[2] > 1 && BBox[3] > 1 && this.gs.getClippingShape() == null && this.gs.CTM[0][1] == 0
						&& this.gs.CTM[1][0] == 0 && this.gs.CTM[2][1] != 0 && this.gs.CTM[2][0] < 0) {
					if (debug) System.out.println("setClip1 ");

					clip = setClip(defaultClip, BBox);
					clipChanged = true;

					// System.out.println(BBox[0]+" "+BBox[1]+" "+BBox[2]+" "+BBox[3]);
					// Matrix.show(gs.CTM);
				}
				else
					if (BBox != null && BBox[0] == 0 && BBox[1] == 0 && BBox[2] > 1 && BBox[3] > 1 && BBox[2] != BBox[3]
							&& (this.gs.CTM[0][0] > 0.99 || this.gs.CTM[2][1] < -1) && (this.gs.CTM[2][0] < -1 || this.gs.CTM[2][0] > 1)
							&& this.gs.CTM[2][1] != 0) {// ) && BBox[2]>1 && BBox[3]>1 ){//if(BBox!=null && matrix==null && BBox[0]==0 && BBox[1]==0){

						if (debug) System.out.println("setClip2");

						clip = setClip(defaultClip, BBox);
						clipChanged = true;
					}

					// attempt to fix odd customers3/slides1.pdf text off page issue
					// no obvious reason to ignore text on form other than negative y value
					// adjusted to fix customers-june2011/Request_For_Quotation.pdf
					// if(formLevel==1 && gs.CTM[0][0]!=0 && gs.CTM[0][0]!=gs.CTM[0][1] && gs.CTM[0][0]!=1 && currentTextState.Tm[0][0]==1 &&
					// gs.CTM[1][1]<1f && (gs.CTM[1][1]>0.92f || gs.CTM[0][1]!=0) && currentTextState.Tm[2][1]<0){

					// Lonzak: With this strange bugfix introduced in 4.77 a new bug has been introduced: signature image in landscape documents was not
					// shown anymore
					// else if(BBox!=null && BBox[0]==0 && BBox[1]==0 && BBox[2]>1 && BBox[3]>1 && gs.getClippingShape()!=null){
					// //&& (gs.CTM[0][0]>0.99 || gs.CTM[2][1]<-1) && (gs.CTM[2][0]<-1 || gs.CTM[2][0]>1) && gs.CTM[2][1]!=0 ){
					//
					// if(debug)
					// System.out.println("setClip3");
					//
					// clip = setClip(defaultClip, BBox);
					// clipChanged=true;
					// }
					else
						if (this.formLevel > 1 && BBox != null && BBox[0] > 50 && BBox[1] > 50 && this.gs.getClippingShape() != null
								&& (BBox[0] - 1) > this.gs.getClippingShape().getBounds().x
								&& (BBox[1] - 1) > this.gs.getClippingShape().getBounds().y) {

							// System.out.println("XX form="+formLevel);
							// System.out.println(BBox[0]+" "+BBox[1]+" "+BBox[2]+" "+BBox[3]);
							// System.out.println(gs.getClippingShape().getBounds()+" "+defaultClip);
							if (debug) System.out.println("setClip4");

							clip = setClip(defaultClip, BBox);
							clipChanged = true;
						}
						else
							if (debug) {
								System.out.println("no Clip set");

							}

				/** decode the stream */
				if (objectData.length > 0) {

					PdfObject newSMask = getSMask(BBox); // check for soft mask we need to apply
					int firstValue = getFirstValue(this.gs.getBM()); // see if multiply transparency

					// isTransparent sees if this case happens (randomHouse/9781580082778_DistX.pdf)
					// added formLevel to try and fix customer issue on customers3/Auktionsauftrag_45.9.33620.pdf (including printing)
					// if(!isTransparent(cache.groupObj) &&
					// (flattenXFormToImage || (isPrinting && (gs.CTM[2][0]==0) && gs.CTM[2][1]==0 && newSMask==null &&
					// firstValue!=PdfDictionary.Multiply && gs.getAlpha(GraphicsState.FILL)==1) ||
					// ((gs.getAlpha(GraphicsState.FILL)==1 || gs.SMask!=null || layerDecoder.layerLevel>0 || (formLevel==1 &&
					// gs.getAlpha(GraphicsState.FILL)<0.1f))))){ //use if looks like marked text block
					if (!isTransparent(this.cache.groupObj)
							&& (this.flattenXFormToImage
									|| (this.isPrinting && (this.gs.CTM[2][0] == 0) && this.gs.CTM[2][1] == 0 && newSMask == null
											&& firstValue != PdfDictionary.Multiply && this.gs.getAlpha(GraphicsState.FILL) == 1) || ((this.gs
									.getAlpha(GraphicsState.FILL) == 1 || this.layerDecoder.layerLevel > 0 || (this.formLevel == 1 && this.gs
									.getAlpha(GraphicsState.FILL) < 0.1f)) && newSMask == null && firstValue != PdfDictionary.Multiply))) { // use if
																																			// looks
																																			// like
																																			// marked
																																			// text
																																			// block

						if (debug) System.out.println("decode");

						decodeStreamIntoObjects(objectData, false);

					}
					else
						if (newSMask != null || firstValue == PdfDictionary.Multiply) { // if an smask render to image and apply Smask to it - then
																						// write out as image

							if (debug) System.out.println("createMaskForm");

							createMaskForm(XObject, name, newSMask, firstValue);

						}
						else {

							if (debug) System.out.println("other");

							// save renderer
							DynamicVectorRenderer oldCurrent = this.current;
							this.current = new SwingDisplay(this.pageNum, this.objectStoreStreamRef, false);
							this.current.setHiResImageForDisplayMode(this.useHiResImageForDisplay);

							boolean oldRenderDirectly = this.renderDirectly;

							// to draw image we need to use local 1
							float strokeAlpha = this.gs.getAlpha(GraphicsState.STROKE);
							float maxStroke = this.gs.getAlphaMax(GraphicsState.STROKE);
							float fillAlpha = this.gs.getAlpha(GraphicsState.FILL);
							float maxFill = this.gs.getAlphaMax(GraphicsState.FILL);

							this.currentPdfFile.checkResolved(this.cache.pageGroupingObj);

							if (this.cache.pageGroupingObj != null && this.renderDirectly) {
								this.gs.setMaxAlpha(GraphicsState.STROKE, 1);

								// needed for /PDFdata/baseline_screens/customers1/Milkshake BusyTime DistX.pdf
								// if(cache.pageGroupingObj.getDictionary(PdfDictionary.ColorSpace).getParameterConstant(PdfDictionary.ColorSpace)!=ColorSpaces.DeviceCMYK){
								// gs.setMaxAlpha(GraphicsState.FILL,1);
								// }
							}
							else {
								this.gs.setMaxAlpha(GraphicsState.STROKE, strokeAlpha);
								this.gs.setMaxAlpha(GraphicsState.FILL, fillAlpha);
							}

							if (this.renderDirectly
									&& (this.cache.pageGroupingObj == null || (this.cache.pageGroupingObj != null && this.cache.groupObj != null && (this.cache.groupObj
											.getDictionary(PdfDictionary.ColorSpace).getParameterConstant(PdfDictionary.ColorSpace) == ColorSpaces.ICC || this.cache.groupObj
											.getDictionary(PdfDictionary.ColorSpace).getParameterConstant(PdfDictionary.ColorSpace) != this.cache.pageGroupingObj
											.getDictionary(PdfDictionary.ColorSpace).getParameterConstant(PdfDictionary.ColorSpace))))) this.gs
									.setMaxAlpha(GraphicsState.FILL, 1);

							if (!this.renderDirectly) {
								this.gs.setAlpha(GraphicsState.STROKE, 1);
								this.gs.setAlpha(GraphicsState.FILL, 1);
							}

							this.renderDirectly = false;

							// ensure drawn
							oldCurrent.setGraphicsState(GraphicsState.STROKE, 1f);
							oldCurrent.setGraphicsState(GraphicsState.FILL, 1f);

							decodeStreamIntoObjects(objectData, false);

							this.gs.setMaxAlpha(GraphicsState.STROKE, maxStroke);
							this.gs.setMaxAlpha(GraphicsState.FILL, maxFill);

							if (!this.renderDirectly) {
								this.gs.setAlpha(GraphicsState.STROKE, strokeAlpha);
								this.gs.setAlpha(GraphicsState.FILL, fillAlpha);
							}

							oldCurrent.drawXForm(this.current, this.gs);

							// restore
							this.current = oldCurrent;
							this.current.setGraphicsState(GraphicsState.STROKE, strokeAlpha);
							this.current.setGraphicsState(GraphicsState.FILL, fillAlpha);

							this.renderDirectly = oldRenderDirectly;
						}
				}
				// restore clip if changed
				if (clipChanged) {
					this.gs.setClippingShape(clip);
					this.current.drawClip(this.gs, clip, false);
				}

				// restore settings
				this.formLevel--;

				//
				// restore old matrix or set default
				// fixes customers-dec2012/81564885_1355243032.pdf
				if (oldCTM != null) {
					this.gs.CTM = oldCTM;
				}
				else
					if (this.gs.CTM[0][0] == 1f && this.gs.CTM[1][1] == 1f) {
						this.gs.CTM = new float[][] { { 1, 0, 0 }, { 0, 1, 0 }, { 0, 0, 1 } };
					}

				/** restore old colorspace and fonts */
				this.gs.strokeColorSpace = mainStrokeColorData;
				this.gs.nonstrokeColorSpace = mainnonStrokeColorData;

				// put back original state
				this.cache.restore(mainCache);

				// restore gs max to current so child gs values can not exceed
				this.gs.setMaxAlpha(GraphicsState.STROKE, maxStrokeValue);
				this.gs.setMaxAlpha(GraphicsState.FILL, maxFillValue);

				PdfStreamDecoder.indent = oldIndent;
			}

		}
		catch (Error e) {
			// tell user and log
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());

			this.imagesProcessedFully = false;
			this.errorTracker.addPageFailureMessage("Error " + e + " in DO");

			if (ExternalHandlers.throwMissingCIDError && e.getMessage().contains("kochi")) throw e;
		}
		catch (Exception e) {

			if (LogWriter.isOutput()) LogWriter.writeLog("Exception " + e);

			this.imagesProcessedFully = false;
			this.errorTracker.addPageFailureMessage("Error " + e + " in DO");
		}

		this.formName = oldFormName;
	}

	private PdfObject getSMask(float[] BBox) {
		PdfObject newSMask = null;

		// ignore if none
		if (this.gs.SMask != null && this.gs.SMask.getGeneralType(PdfDictionary.SMask) == PdfDictionary.None) {

			return null;
		}

		if (this.gs.SMask != null && BBox != null && BBox[2] > 0) { // see if SMask to cache to image & stop negative cases such as Milkshake StckBook
																	// Activity disX.pdf
			if (this.gs.SMask.getParameterConstant(PdfDictionary.Type) != PdfDictionary.Mask || this.gs.SMask.getFloatArray(PdfDictionary.BC) != null) { // fix
																																							// for
																																							// waves
																																							// file
				newSMask = this.gs.SMask.getDictionary(PdfDictionary.G);
				this.currentPdfFile.checkResolved(newSMask);
			}
		}
		return newSMask;
	}

	private static int getFirstValue(PdfArrayIterator BMvalue) {

		int firstValue = PdfDictionary.Unknown;
		if (BMvalue != null && BMvalue.hasMoreTokens()) {
			firstValue = BMvalue.getNextValueAsConstant(false);
		}
		return firstValue;
	}

	private void createMaskForm(PdfObject XObject, String name, PdfObject newSMask, int firstValue) throws PdfException {

		float[] BBox;// size
		BBox = XObject.getFloatArray(PdfDictionary.BBox);

		/** get form as an image */
		int fx = (int) BBox[0];
		int fy = (int) BBox[1];
		int fw = (int) BBox[2];
		int fh = (int) (BBox[3]);

		// check x,y offsets and factor in
		if (fx < 0) fx = 0;

		// get the form
		BufferedImage image = null;

		// get smask if present and create as image for later
		if (newSMask != null) {

			image = getImageFromPdfObject(XObject, fx, fw, fy, fh);
			BufferedImage smaskImage = getImageFromPdfObject(newSMask, fx, fw, fy, fh);

			/**
			 * get Mask colourspace as we need to process mask differently depending on value
			 */
			PdfObject ColorSpace = null;
			PdfObject group = newSMask.getDictionary(PdfDictionary.Group);
			if (group != null) {
				this.currentPdfFile.checkResolved(group);
				ColorSpace = group.getDictionary(PdfDictionary.ColorSpace);
			}

			if (ColorSpace != null) this.currentPdfFile.checkResolved(ColorSpace);

			// apply SMask to image
			image = ImageCommands.applySmask(image, smaskImage, newSMask, true, true, ColorSpace, XObject, this.gs);

			if (smaskImage != null) {
				smaskImage.flush();
			}
		}

		// <start-pro>
		/**
		 * //<end-pro>
		 * 
		 * //<start-std>
		 * 
		 * // code to handle inverted HTML5 forms // (see sample_pdfs_html/general/test22.pdf) if(current.getType()==
		 * org.jpedal.render.output.html.HTMLDisplay.CREATE_HTML || current.getType()== org.jpedal.render.output.html.HTMLDisplay.CREATE_JAVAFX ||
		 * current.getType()== org.jpedal.render.output.html.HTMLDisplay.CREATE_SVG){
		 * 
		 * //physically turn for moment AffineTransform image_at =new AffineTransform(); image_at.scale(1,-1);
		 * image_at.translate(0,-image.getHeight()); AffineTransformOp invert= new AffineTransformOp(image_at, ColorSpaces.hints); image =
		 * invert.filter(image,null);
		 * 
		 * } /
		 **/
		// </end-std>

		GraphicsState gs1 = new GraphicsState();
		gs1.CTM = new float[][] { { image.getWidth(), 0, 1 }, { 0, image.getHeight(), 1 }, { 0, 0, 0 } };

		// different formula needed if flattening forms
		if (this.isFlattenedForm) {
			gs1.x = this.flattenX;
			gs1.y = this.flattenY;
		}
		else {
			gs1.x = fx;
			gs1.y = fy - image.getHeight();
		}

		// draw as image
		gs1.CTM[2][0] = gs1.x;
		gs1.CTM[2][1] = gs1.y;
		this.current.drawImage(this.pageNum, image, gs1, false, name, PDFImageProcessing.IMAGE_INVERTED, -1);
	}

	private BufferedImage createTransparentForm(PdfObject XObject, int fx, int fy, int fw, int fh) {
		BufferedImage image;
		byte[] objectData1 = this.currentPdfFile.readStream(XObject, true, true, false, false, false,
				XObject.getCacheName(this.currentPdfFile.getObjectReader()));

		ObjectStore localStore = new ObjectStore(null);
		DynamicVectorRenderer glyphDisplay = new SwingDisplay(0, false, 20, localStore);
		glyphDisplay.setHiResImageForDisplayMode(this.useHiResImageForDisplay);

		PdfStreamDecoder glyphDecoder = new PdfStreamDecoder(this.currentPdfFile, this.useHiResImageForDisplay, null); // switch to hires as well
		glyphDecoder.setParameters(this.isPageContent, this.renderPage, this.renderMode, this.extractionMode);
		glyphDecoder.setObjectValue(ValueTypes.ObjectStore, localStore);
		glyphDecoder.setIntValue(FormLevel, this.formLevel);
		glyphDecoder.setFloatValue(Multiplier, this.multiplyer);
		glyphDecoder.setFloatValue(SamplingUsed, this.samplingUsed);

		glyphDecoder.setObjectValue(ValueTypes.DynamicVectorRenderer, glyphDisplay);

		/** read any resources */
		try {

			PdfObject SMaskResources = XObject.getDictionary(PdfDictionary.Resources);
			if (SMaskResources != null) glyphDecoder.readResources(SMaskResources, false);

		}
		catch (Exception e) {
			// tell user and log
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
		}

		/** decode the stream */
		if (objectData1 != null) glyphDecoder.decodeStreamIntoObjects(objectData1, false);

		int hh = fh;
		if (fy > fh) hh = fy - fh;

		// get bit underneath and merge in
		image = new BufferedImage(fw, hh, BufferedImage.TYPE_INT_ARGB);

		Graphics2D formG2 = image.createGraphics();

		if (!this.isFlattenedForm) // already allowed for in form flattening
		formG2.translate(-fx, -fh);

		// current.paint(formG2,null,null,null,false,true);

		formG2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));

		glyphDisplay.setG2(formG2);
		glyphDisplay.paint(null, null, null);

		localStore.flush();

		return image;
	}

	private static boolean isTransparent(PdfObject groupObj) {

		boolean isTransparentRGB = false;
		if (groupObj != null) {
			String S = groupObj.getName(PdfDictionary.S);
			PdfObject colspace = groupObj.getDictionary(PdfDictionary.ColorSpace);

			isTransparentRGB = S != null && S.equals("Transparency") && colspace != null
					&& colspace.getParameterConstant(PdfDictionary.ColorSpace) == ColorSpaces.DeviceRGB;
		}

		return isTransparentRGB;
	}

	private Area setClip(Shape defaultClip, float[] BBox) {
		Area clip;
		float scalingW = this.gs.CTM[0][0];
		if (scalingW == 0) {
			scalingW = this.gs.CTM[0][1];
		}

		float scalingH = this.gs.CTM[1][1];
		if (scalingH == 0) {
			scalingH = this.gs.CTM[1][0];
		}

		int x, y, w, h;

		if (this.gs.CTM[0][1] > 0 && this.gs.CTM[1][0] < 0) {

			x = (int) (this.gs.CTM[2][0] - (BBox[3]));
			y = (int) (this.gs.CTM[2][1] + BBox[0]);
			w = (int) ((BBox[3] - BBox[1]) * scalingW);
			h = (int) ((BBox[2] - BBox[0]) * scalingH);

		}
		else
			if (this.gs.CTM[0][1] < 0 && this.gs.CTM[1][0] > 0) {

				x = (int) (this.gs.CTM[2][0] + BBox[1]);
				y = (int) (this.gs.CTM[2][1] - BBox[2]);
				w = (int) ((BBox[3] - BBox[1]) * -scalingW);
				h = (int) ((BBox[2] - BBox[0]) * -scalingH);

			}
			else { // note we adjust size using CTM to factor in scaling
				x = (int) ((this.gs.CTM[2][0] + BBox[0]));
				y = (int) ((this.gs.CTM[2][1] + BBox[1] - 1));
				w = (int) (1 + (BBox[2] - BBox[0]) * scalingW);
				h = (int) (2 + (BBox[3] - BBox[1]) * scalingH);

				if (this.gs.CTM[2][1] < 0) {
					h = (int) (h - (this.gs.CTM[2][1] * scalingH));
				}
				if (this.gs.CTM[2][0] < 0) {
					w = (int) (w - (this.gs.CTM[2][0] * scalingH));
				}

				// allow for inverted
				if (this.gs.CTM[1][1] < 0) {
					y = y - h;
				}
			}

		if (this.gs.getClippingShape() == null) {
			clip = null;
		}
		else {
			clip = (Area) this.gs.getClippingShape().clone();
		}

		Area newClip = new Area(new Rectangle(x, y, w, h));

		this.gs.updateClip(new Area(newClip));
		this.current.drawClip(this.gs, defaultClip, false);

		return clip;
	}

	final private static float[] matches = { 1f, 0f, 0f, 1f, 0f, 0f };

	private static boolean isIdentity(float[] matrix) {

		boolean isIdentity = true;// assume right and try to disprove

		if (matrix != null) {

			// see if it matches if not set flag and exit
			for (int ii = 0; ii < 6; ii++) {
				if (matrix[ii] != matches[ii]) {
					isIdentity = false;
					break;
				}
			}
		}

		return isIdentity;
	}

	private BufferedImage getImageFromPdfObject(PdfObject newSMask, int fx, int fw, int fy, int fh) throws PdfException {

		BufferedImage smaskImage;
		Graphics2D formG2;
		byte[] objectData = this.currentPdfFile.readStream(newSMask, true, true, false, false, false,
				newSMask.getCacheName(this.currentPdfFile.getObjectReader()));

		ObjectStore localStore = new ObjectStore(null);

		DynamicVectorRenderer glyphDisplay = new SwingDisplay(0, false, 20, localStore);

		boolean useHiRes = true;

		PdfStreamDecoder glyphDecoder = new PdfStreamDecoder(this.currentPdfFile, useHiRes, null); // switch to hires as well
		glyphDecoder.setParameters(this.isPageContent, this.renderPage, this.renderMode, this.extractionMode);
		glyphDecoder.setObjectValue(ValueTypes.ObjectStore, localStore);
		glyphDisplay.setHiResImageForDisplayMode(useHiRes);
		glyphDecoder.setObjectValue(ValueTypes.DynamicVectorRenderer, glyphDisplay);
		glyphDecoder.setFloatValue(Multiplier, this.multiplyer);
		glyphDecoder.setFloatValue(SamplingUsed, this.samplingUsed);
		glyphDecoder.setBooleanValue(IsFlattenedForm, this.isFlattenedForm);
		glyphDecoder.setIntValue(FormLevel, this.formLevel);

		// flag to image decoder that called form here and whether screen or image
		// if(renderDirectly)
		// glyphDecoder.setIntValue(IsImage,IMAGE_getImageFromPdfObject);
		// else
		glyphDecoder.setIntValue(IsImage, SCREEN_getImageFromPdfObject);

		/** read any resources */
		try {

			PdfObject SMaskResources = newSMask.getDictionary(PdfDictionary.Resources);
			if (SMaskResources != null) glyphDecoder.readResources(SMaskResources, false);

		}
		catch (Exception e) {
			// tell user and log
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
		}

		/** decode the stream */
		if (objectData != null) glyphDecoder.decodeStreamIntoObjects(objectData, false);

		glyphDecoder.dispose();

		int hh = fh;
		if (fy > fh) hh = fy - fh;

		if (fw == 0) fw = 1;
		try {
			smaskImage = new BufferedImage(fw, hh, BufferedImage.TYPE_INT_ARGB);
		}
		catch (Error err) {
			// tell user and log
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + err.getMessage());

			smaskImage = null;
		}

		if (smaskImage != null) {
			formG2 = smaskImage.createGraphics();

			formG2.translate(-fx, -fh);
			glyphDisplay.setG2(formG2);
			glyphDisplay.paint(null, null, null);

			localStore.flush();
		}
		return smaskImage;
	}

	/**
	 * put item in graphics stack
	 */
	private void pushGraphicsState(GraphicsState gs) {

		if (!this.isStackInitialised) {
			this.isStackInitialised = true;

			this.graphicsStateStack = new Vector_Object(10);
			this.textStateStack = new Vector_Object(10);
			this.strokeColorStateStack = new Vector_Object(20);
			this.nonstrokeColorStateStack = new Vector_Object(20);
			// clipStack=new Vector_Object(20);
		}

		// store
		this.graphicsStateStack.push(gs.clone());

		// store clip
		// Area currentClip=gs.getClippingShape();
		// if(currentClip==null)
		// clipStack.push(null);
		// else{
		// clipStack.push(currentClip.clone());
		// }
		// store text state (technically part of gs)
		this.textStateStack.push(this.currentTextState.clone());

		// save colorspaces
		this.nonstrokeColorStateStack.push(gs.nonstrokeColorSpace.clone());
		this.strokeColorStateStack.push(gs.strokeColorSpace.clone());

		this.current.resetOnColorspaceChange();
	}

	/**
	 * restore GraphicsState status from graphics stack
	 */
	private GraphicsState restoreGraphicsState(GraphicsState gs) {

		boolean hasClipChanged = false;

		if (!this.isStackInitialised) {

			if (LogWriter.isOutput()) LogWriter.writeLog("No GraphicsState saved to retrieve");

			// reset to defaults
			gs = new GraphicsState();
			this.currentTextState = new TextState();

		}
		else {

			// see if clip changed
			hasClipChanged = gs.hasClipChanged();

			gs = (GraphicsState) this.graphicsStateStack.pull();
			this.currentTextState = (TextState) this.textStateStack.pull();

			// @remove all caching?
			gs.strokeColorSpace = (GenericColorSpace) this.strokeColorStateStack.pull();
			gs.nonstrokeColorSpace = (GenericColorSpace) this.nonstrokeColorStateStack.pull();

			if (gs.strokeColorSpace.getID() == ColorSpaces.Separation) gs.strokeColorSpace.restoreColorStatus();

			if (gs.nonstrokeColorSpace.getID() == ColorSpaces.Separation) gs.nonstrokeColorSpace.restoreColorStatus();
		}
		// 20101122 removed by MS as not apparently needed
		// Object currentClip=clipStack.pull();

		/**
		 * if(hasClipChanged){ //if(!renderDirectly && hasClipChanged){ if(currentClip==null){
		 * 
		 * if(gs.current_clipping_shape!=null){ System.out.println("1shape="+gs.current_clipping_shape); throw new RuntimeException(); }
		 * gs.setClippingShape(null); }else{
		 * 
		 * if(!gs.current_clipping_shape.equals((Area)currentClip)){ System.out.println("2shape="+gs.current_clipping_shape); // throw new
		 * RuntimeException(); } gs.setClippingShape((Area)currentClip); } } /
		 **/
		// //////////////////////////////////

		// copy last CM
		for (int i = 0; i < 3; i++)
			System.arraycopy(gs.CTM, 0, gs.lastCTM, 0, 3);

		// save for later
		if (this.renderPage) {

			if (hasClipChanged) {

				this.current.drawClip(gs, this.defaultClip, true);
			}
			this.current.resetOnColorspaceChange();

			this.current.drawFillColor(gs.getNonstrokeColor());
			this.current.drawStrokeColor(gs.getStrokeColor());

			/**
			 * align display
			 */
			this.current.setGraphicsState(GraphicsState.FILL, gs.getAlpha(GraphicsState.FILL));
			this.current.setGraphicsState(GraphicsState.STROKE, gs.getAlpha(GraphicsState.STROKE));

			// current.drawTR(currentGraphicsState.getTextRenderType()); //reset TR value

		}

		return gs;
	}

	private GraphicsState Q(GraphicsState gs, boolean isLowerCase) {

		// save or retrieve
		if (isLowerCase) pushGraphicsState(gs);
		else {
			gs = restoreGraphicsState(gs);

			// flag font has changed
			this.currentTextState.setFontChanged(true);

		}

		return gs;
	}

	private static void CM(GraphicsState gs, CommandParser parser) {

		// create temp Trm matrix to update Tm
		float[][] Trm = new float[3][3];

		// set Tm matrix
		Trm[0][0] = parser.parseFloat(5);
		Trm[0][1] = parser.parseFloat(4);
		Trm[0][2] = 0;
		Trm[1][0] = parser.parseFloat(3);
		Trm[1][1] = parser.parseFloat(2);
		Trm[1][2] = 0;
		Trm[2][0] = parser.parseFloat(1);
		Trm[2][1] = parser.parseFloat(0);
		Trm[2][2] = 1;

		// copy last CM
		for (int i = 0; i < 3; i++)
			System.arraycopy(gs.CTM, 0, gs.lastCTM, 0, 3);

		// multiply to get new CTM
		gs.CTM = Matrix.multiply(Trm, gs.CTM);

		// remove slight sheer
		if (gs.CTM[0][0] > 0 && gs.CTM[1][1] > 0 && gs.CTM[1][0] > 0 && gs.CTM[1][0] < 0.01 && gs.CTM[0][1] < 0) {
			gs.CTM[0][1] = 0;
			gs.CTM[1][0] = 0;
		}
	}

	private void F(boolean isStar, int formLevel, PdfShape currentDrawShape) {

		// ignore transparent white if group set
		if (formLevel > 0 && this.cache.groupObj != null && !this.cache.groupObj.getBoolean(PdfDictionary.K)
				&& this.gs.getAlphaMax(GraphicsState.FILL) > 0.84f && (this.gs.nonstrokeColorSpace.getID() == ColorSpaces.DeviceCMYK)) {

			PdfArrayIterator BMvalue = this.gs.getBM();

			// check not handled elsewhere
			int firstValue = PdfDictionary.Unknown;
			if (BMvalue != null && BMvalue.hasMoreTokens()) {
				firstValue = BMvalue.getNextValueAsConstant(false);
			}

			if (this.gs.nonstrokeColorSpace.getColor().getRGB() == -1
					|| (firstValue == PdfDictionary.Multiply && this.gs.getAlpha(GraphicsState.FILL) == 1f)) return;
		}

		/**
		 * if SMask with this color, we need to ignore (only case of white with BC of 1,1,1 at present for customers-june2011/12.pdf)
		 */
		if (this.gs.SMask != null && this.gs.nonstrokeColorSpace.getID() == ColorSpaces.DeviceCMYK) {

			float[] BC = this.gs.SMask.getFloatArray(PdfDictionary.BC);
			if (this.gs.nonstrokeColorSpace.getColor().getRGB() == -16777216 && BC != null && BC[0] == 1.0f) return;
		}

		/**
		 * if SMask with this color, we need to ignore (only case of white with BC of 1,1,1 at present for customers-june2011/4.pdf)
		 */
		if (this.gs.SMask != null && this.gs.nonstrokeColorSpace.getID() == ColorSpaces.ICC) {

			float[] BC = this.gs.SMask.getFloatArray(PdfDictionary.BC);
			if (this.gs.nonstrokeColorSpace.getColor().getRGB() == -16777216 && BC != null && BC[0] == 0.0f) return;
		}

		// replace F with image if soft mask set (see randomHouse/9781609050917_DistX.pdf)
		if (this.gs.SMask != null && this.gs.SMask.getDictionary(PdfDictionary.G) != null
				&& this.gs.nonstrokeColorSpace.getID() == ColorSpaces.DeviceRGB) {

			if (this.gs.nonstrokeColorSpace.getColor().getRGB() == -1 && this.gs.getOPM() == 1.0f) return;

			float[] BC = this.gs.SMask.getFloatArray(PdfDictionary.BC);

			if (this.gs.nonstrokeColorSpace.getColor().getRGB() == -16777216 && BC != null && BC[0] == 1.0f && BC[1] == 1.0f && BC[2] == 1.0f) return;

			try {
				createSMaskFill();
			}
			catch (PdfException e) {
				// tell user and log
				if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
			}
			return;
		}

		// (see randomHouse/9781609050917_DistX.pdf)
		// if(gs.SMask!=null && (gs.SMask.getGeneralType(PdfDictionary.SMask)==PdfDictionary.None ||
		// gs.SMask.getGeneralType(PdfDictionary.SMask)==PdfDictionary.Multiply) && gs.nonstrokeColorSpace.getID() == ColorSpaces.DeviceRGB &&
		// gs.getOPM()==1.0f && gs.nonstrokeColorSpace.getColor().getRGB()==-16777216){

		if (this.gs.SMask != null && this.gs.SMask.getGeneralType(PdfDictionary.SMask) != PdfDictionary.None
				&& this.gs.nonstrokeColorSpace.getID() == ColorSpaces.DeviceRGB && this.gs.getOPM() == 1.0f
				&& this.gs.nonstrokeColorSpace.getColor().getRGB() == -16777216) {
			return;
		}

		if (this.layerDecoder.isLayerVisible()) {

			// set Winding rule
			if (isStar) {
				currentDrawShape.setEVENODDWindingRule();
			}
			else currentDrawShape.setNONZEROWindingRule();

			currentDrawShape.closeShape();

			// generate shape and stroke and status. Type required to check if EvenOdd rule emulation required.
			Shape currentShape = currentDrawShape.generateShapeFromPath(this.gs.CTM, this.gs.getLineWidth(), Cmd.F, this.current.getType());

			// simulate overPrint - may need changing to draw at back of stack
			if (this.gs.nonstrokeColorSpace.getID() == ColorSpaces.DeviceCMYK && this.gs.getOPM() == 1.0f) {

				PdfArrayIterator BMvalue = this.gs.getBM();

				// check not handled elsewhere
				int firstValue = PdfDictionary.Unknown;
				if (BMvalue != null && BMvalue.hasMoreTokens()) {
					firstValue = BMvalue.getNextValueAsConstant(false);
				}

				if (firstValue == PdfDictionary.Multiply) {

					float[] rawData = this.gs.nonstrokeColorSpace.getRawValues();

					if (rawData != null && rawData[3] == 1) {

						// try to keep as binary if possible
						// boolean hasObjectBehind=current.hasObjectsBehind(gs.CTM);
						// if(hasObjectBehind){
						currentShape = null;
						// }
					}
				}
			}

			if (currentShape != null && this.gs.nonstrokeColorSpace.getID() == ColorSpaces.ICC && this.gs.getOPM() == 1.0f) {

				PdfArrayIterator BMvalue = this.gs.getBM();

				// check not handled elsewhere
				int firstValue = PdfDictionary.Unknown;
				if (BMvalue != null && BMvalue.hasMoreTokens()) {
					firstValue = BMvalue.getNextValueAsConstant(false);
				}

				if (firstValue == PdfDictionary.Multiply) {

					float[] rawData = this.gs.nonstrokeColorSpace.getRawValues();

					/**
					 * if(rawData!=null && rawData[2]==1){
					 * 
					 * //try to keep as binary if possible boolean hasObjectBehind=current.hasObjectsBehind(gs.CTM); if(hasObjectBehind)
					 * currentShape=null; }else
					 */
					{ // if zero just remove
						boolean isZero = true;
						for (float aRawData : rawData)
							if (aRawData != 0) isZero = false;

						if (isZero) currentShape = null;
					}
				}

				// fix for very odd shading in Customers-dec2011/92804635.pdf
				// if(gs.getClippingShape()!=null && isRectangleOrCircle(currentShape) && isRectangleOrCircle(gs.getClippingShape()) &&
				// gs.getClippingShape().getBounds().width<80){// && gs.getClippingShape().getBounds().width< currentShape.getBounds().width){
				// currentShape=null;
				// }
			}

			// do not paint white CMYK in overpaint mode
			if (currentShape != null && this.gs.getAlpha(GraphicsState.FILL) < 1 && this.gs.nonstrokeColorSpace.getID() == ColorSpaces.DeviceN
					&& this.gs.getOPM() == 1.0f && this.gs.nonstrokeColorSpace.getColor().getRGB() == -16777216) {

				// System.out.println(gs.getNonStrokeAlpha());
				// System.out.println(nonstrokeColorSpace.getAlternateColorSpace()+" "+nonstrokeColorSpace.getColorComponentCount()+" "+nonstrokeColorSpace.pantoneName);
				boolean ignoreTransparent = true; // assume true and disprove
				float[] raw = this.gs.nonstrokeColorSpace.getRawValues();

				if (raw != null) {
					int count = raw.length;
					for (int ii = 0; ii < count; ii++) {

						// System.out.println(ii+"="+raw[ii]+" "+count);

						if (raw[ii] > 0) {
							ignoreTransparent = false;
							ii = count;
						}
					}
				}

				if (ignoreTransparent) {
					currentShape = null;
				}
			}

			// save for later
			if (currentShape != null && this.renderPage) {

				// delete from non-HTML5 build
				// <start-pro>
				/**
				 * //<end-pro>
				 * 
				 * //<start-std>
				 * 
				 * // Fixes 12106 - No support in html5 for EvenOdd Winding. e.g. samsung/4.jpegSample.pdf (No entry signs) if
				 * (currentDrawShape.requiresEvenOddEmulation() && currentShape.getBounds2D().getWidth() > 4 && currentShape.getBounds2D().getHeight()
				 * > 4) { // Implicitly, only true when when type==HTML
				 * 
				 * current.setBooleanValue(org.jpedal.render.output.html.HTMLDisplay.EmulateEvenOdd, true); current.drawShape(currentShape, gs, 0);
				 * current.setBooleanValue(org.jpedal.render.output.html.HTMLDisplay.EmulateEvenOdd, false);
				 * 
				 * } else //<end-std> /
				 **/
				{
					this.gs.setStrokeColor(this.gs.strokeColorSpace.getColor());
					this.gs.setNonstrokeColor(this.gs.nonstrokeColorSpace.getColor());
					this.gs.setFillType(GraphicsState.FILL);

					this.current.drawShape(currentShape, this.gs, Cmd.F);
				}
			}
		}
		// always reset flag
		currentDrawShape.setClip(false);
		currentDrawShape.resetPath(); // flush all path ops stored
	}

	/**
	 * make image from SMask and colour in with fill colour to simulate effect
	 */
	private void createSMaskFill() throws PdfException {

		PdfObject maskObj = this.gs.SMask.getDictionary(PdfDictionary.G);
		this.currentPdfFile.checkResolved(maskObj);
		float[] BBox;// size
		BBox = maskObj.getFloatArray(PdfDictionary.BBox);

		/** get dimensions as an image */
		int fx = (int) BBox[0];
		int fy = (int) BBox[1];
		int fw = (int) BBox[2];
		int fh = (int) (BBox[3]);

		// check x,y offsets and factor in
		if (fx < 0) fx = 0;

		/**
		 * get the SMAsk
		 */
		BufferedImage smaskImage = getImageFromPdfObject(maskObj, fx, fw, fy, fh);

		WritableRaster ras = smaskImage.getRaster();

		int w = ras.getWidth();
		int h = ras.getHeight();

		/**
		 * and colour in
		 */
		boolean transparent;
		int[] values = new int[4];

		// get fill colour
		int fillColor = this.gs.nonstrokeColorSpace.getColor().getRGB();
		values[0] = (byte) ((fillColor >> 16) & 0xFF);
		values[1] = (byte) ((fillColor >> 8) & 0xFF);
		values[2] = (byte) ((fillColor) & 0xFF);

		int[] transparentPixel = { 0, 0, 0, 0 };
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {

				// get raw color data
				ras.getPixels(x, y, 1, 1, values);

				// see if transparent
				transparent = (values[0] == 0 && values[1] == 0 && values[2] == 0 && values[3] == 255);

				// if it matched replace and move on
				if (transparent) {
					ras.setPixels(x, y, 1, 1, transparentPixel);
				}
				else {

					int[] newPixel = new int[4];

					newPixel[3] = (int) (255 * 0.75f);
					newPixel[0] = values[0];
					newPixel[1] = values[1];
					newPixel[2] = values[2];

					ras.setPixels(x, y, 1, 1, newPixel);
				}
			}
		}

		/**
		 * draw the shape as image
		 */
		GraphicsState gs1 = new GraphicsState();
		gs1.CTM = new float[][] { { smaskImage.getWidth(), 0, 1 }, { 0, smaskImage.getHeight(), 1 }, { 0, 0, 0 } };

		gs1.x = fx;
		gs1.y = fy - smaskImage.getHeight();

		// add as image
		gs1.CTM[2][0] = gs1.x;
		gs1.CTM[2][1] = gs1.y;
		this.current.drawImage(this.pageNum, smaskImage, gs1, false, "F" + this.tokenNumber, PDFImageProcessing.IMAGE_INVERTED, -1);

		smaskImage.flush();
	}

}
