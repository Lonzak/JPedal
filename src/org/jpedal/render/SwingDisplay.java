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
 * SwingDisplay.java
 * ---------------
 */
package org.jpedal.render;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;

import org.jpedal.PdfDecoder;
import org.jpedal.color.ColorSpaces;
import org.jpedal.color.PdfColor;
import org.jpedal.color.PdfPaint;
import org.jpedal.constants.PDFImageProcessing;
import org.jpedal.exception.PdfException;
import org.jpedal.external.JPedalCustomDrawObject;
import org.jpedal.fonts.PdfFont;
import org.jpedal.fonts.glyph.GlyphFactory;
import org.jpedal.fonts.glyph.PdfGlyph;
import org.jpedal.fonts.glyph.PdfGlyphs;
import org.jpedal.fonts.glyph.PdfJavaGlyphs;
import org.jpedal.io.ColorSpaceConvertor;
import org.jpedal.io.JAIHelper;
import org.jpedal.io.ObjectStore;
import org.jpedal.objects.GraphicsState;
import org.jpedal.parser.Cmd;
import org.jpedal.parser.DecoderOptions;
import org.jpedal.utils.LogWriter;
import org.jpedal.utils.Matrix;
import org.jpedal.utils.Messages;
import org.jpedal.utils.repositories.Vector_Double;
import org.jpedal.utils.repositories.Vector_Float;
import org.jpedal.utils.repositories.Vector_Int;
import org.jpedal.utils.repositories.Vector_Object;
import org.jpedal.utils.repositories.Vector_Rectangle;
import org.jpedal.utils.repositories.Vector_Shape;

public class SwingDisplay extends BaseDisplay implements DynamicVectorRenderer {

	private static boolean drawPDFShapes = true;
	// */

	// Flag to prevent drawing highlights too often.
	boolean ignoreHighlight = false;

	/** stop screen being cleared on next repaint */
	private boolean noRepaint = false;

	/** track items painted to reduce unnecessary calls */
	private int lastItemPainted = -1;

	/** tell renderer to optimise calls if possible */
	private boolean optimsePainting = false;

	private boolean needsHorizontalInvert = false;

	private boolean needsVerticalInvert = false;

	private int pageX1 = 9999, pageX2 = -9999, pageY1 = -9999, pageY2 = 9999;

	// flag highlights need to be generated for page
	private boolean highlightsNeedToBeGenerated = false;

	/** used to cache single image */
	private BufferedImage singleImage = null;

	private int imageCount = 0;

	/** default array size */
	private static final int defaultSize = 5000;

	// used to track end of PDF page in display
	int endItem = -1;

	/** hint for conversion ops */
	private static RenderingHints hints = null;

	private final Map cachedWidths = new HashMap(10);

	private final Map cachedHeights = new HashMap(10);

	private Map fonts = new HashMap(50);

	private Map fontsUsed = new HashMap(50);

	protected GlyphFactory factory = null;

	private PdfGlyphs glyphs;

	private Map imageID = new HashMap(10);

	private final Map imageIDtoName = new HashMap(10);

	private Map storedImageValues = new HashMap(10);

	/** text highlights if needed */
	private int[] textHighlightsX, textHighlightsWidth, textHighlightsHeight;

	// allow user to diable g2 setting
	boolean stopG2setting;

	/** store x */
	private float[] x_coord;

	/** store y */
	private float[] y_coord;

	/** cache for images */
	private Map largeImages = new WeakHashMap(10);

	private Vector_Object text_color;
	private Vector_Object stroke_color;
	private Vector_Object fill_color;

	private Vector_Object stroke;

	/** initial Q & D object to hold data */
	private Vector_Object pageObjects;

	private Vector_Int shapeType;

	private Vector_Rectangle fontBounds;

	private Vector_Double af1;
	private Vector_Double af2;
	private Vector_Double af3;
	private Vector_Double af4;

	/** image options */
	private Vector_Int imageOptions;

	/** TR for text */
	private Vector_Int TRvalues;

	/** font sizes for text */
	private Vector_Int fs;

	/** line widths if not 0 */
	private Vector_Int lw;

	/** holds rectangular outline to test in redraw */
	private Vector_Shape clips;

	/** holds object type */
	private Vector_Int objectType;

	/** holds object type */
	private Vector_Object javaObjects;

	/** holds fill type */
	private Vector_Int textFillType;

	/** holds object type */
	private Vector_Float opacity;

	/** current item added to queue */
	private int currentItem = 0;

	// used to track col changes
	private int lastFillTextCol, lastFillCol, lastStrokeCol;

	/** used to track strokes */
	private Stroke lastStroke = null;

	// trakc affine transform changes
	private double[] lastAf = new double[4];

	/** used to minimise TR and font changes by ignoring duplicates */
	private int lastTR = 2, lastFS = -1, lastLW = -1;

	/** ensure colors reset if text */
	private boolean resetTextColors = true;

	private boolean fillSet = false, strokeSet = false;

	/**
	 * If highlgihts are not null and no highlgihts are drawn then it is likely a scanned page. Treat differently.
	 */
	private boolean needsHighlights = true;

	private int paintThreadCount = 0;
	private int paintThreadID = 0;

	/**
	 * For IDR internal use only
	 */
	private boolean[] drawnHighlights;

	/**
	 * flag if OCR so we need to redraw at end
	 */
	private boolean hasOCR = false;

	protected int type = DynamicVectorRenderer.DISPLAY_SCREEN;

	static {

		hints = new RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		hints.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
	}

	public SwingDisplay() {}

	/**
	 * @param defaultSize
	 */
	void setupArrays(int defaultSize) {

		this.x_coord = new float[defaultSize];
		this.y_coord = new float[defaultSize];
		this.text_color = new Vector_Object(defaultSize);
		this.textFillType = new Vector_Int(defaultSize);
		this.stroke_color = new Vector_Object(defaultSize);
		this.fill_color = new Vector_Object(defaultSize);
		this.stroke = new Vector_Object(defaultSize);
		this.pageObjects = new Vector_Object(defaultSize);
		this.javaObjects = new Vector_Object(defaultSize);
		this.shapeType = new Vector_Int(defaultSize);
		this.areas = new Vector_Rectangle(defaultSize);
		this.af1 = new Vector_Double(defaultSize);
		this.af2 = new Vector_Double(defaultSize);
		this.af3 = new Vector_Double(defaultSize);
		this.af4 = new Vector_Double(defaultSize);

		this.fontBounds = new Vector_Rectangle(defaultSize);

		this.clips = new Vector_Shape(defaultSize);
		this.objectType = new Vector_Int(defaultSize);
	}

	public SwingDisplay(int pageNumber, boolean addBackground, int defaultSize, ObjectStore newObjectRef) {

		this.pageNumber = pageNumber;
		this.objectStoreRef = newObjectRef;
		this.addBackground = addBackground;

		setupArrays(defaultSize);
	}

	public SwingDisplay(int pageNumber, ObjectStore newObjectRef, boolean isPrinting) {

		this.pageNumber = pageNumber;
		this.objectStoreRef = newObjectRef;
		this.isPrinting = isPrinting;

		setupArrays(defaultSize);
	}

	/**
	 * set optimised painting as true or false and also reset if true
	 * 
	 * @param optimsePainting
	 */
	@Override
	public void setOptimsePainting(boolean optimsePainting) {
		this.optimsePainting = optimsePainting;
		this.lastItemPainted = -1;
	}

	private void renderHighlight(Rectangle highlight, Graphics2D g2) {

		if (highlight != null && !this.ignoreHighlight) {
			Shape currentClip = g2.getClip();

			g2.setClip(null);

			// Backup current g2 paint and composite
			Composite comp = g2.getComposite();
			Paint p = g2.getPaint();

			// Set new values for highlight
			g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, PdfDecoder.highlightComposite));

			if (invertHighlight) {
				g2.setColor(Color.WHITE);
				g2.setXORMode(Color.BLACK);
			}
			else {

				g2.setPaint(DecoderOptions.highlightColor);
			}
			// Draw highlight
			g2.fill(highlight);

			// Reset to starting values
			g2.setComposite(comp);
			g2.setPaint(p);

			this.needsHighlights = false;

			g2.setClip(currentClip);
		}
	}

	@Override
	public void stopG2HintSetting(boolean isSet) {
		this.stopG2setting = isSet;
	}

	/* remove all page objects and flush queue */
	@Override
	public void flush() {

		this.singleImage = null;

		this.imageCount = 0;

		this.lastFS = -1;

		if (this.shapeType != null) {

			this.shapeType.clear();
			this.pageObjects.clear();
			this.objectType.clear();
			this.areas.clear();
			this.clips.clear();
			this.x_coord = new float[defaultSize];
			this.y_coord = new float[defaultSize];
			this.textFillType.clear();
			this.text_color.clear();
			this.fill_color.clear();
			this.stroke_color.clear();
			this.stroke.clear();

			if (this.TRvalues != null) this.TRvalues = null;

			if (this.imageOptions != null) this.imageOptions = null;

			if (this.fs != null) this.fs = null;

			if (this.lw != null) this.lw = null;

			this.af1.clear();
			this.af2.clear();
			this.af3.clear();
			this.af4.clear();

			this.fontBounds.clear();

			if (this.opacity != null) this.opacity = null;

			if (this.isPrinting) this.largeImages.clear();

			this.endItem = -1;
		}

		// pointer we use to flag color change
		this.lastFillTextCol = 0;
		this.lastFillCol = 0;
		this.lastStrokeCol = 0;

		this.lastClip = null;
		this.hasClips = false;

		// track strokes
		this.lastStroke = null;

		this.lastAf = new double[4];

		this.currentItem = 0;

		this.fillSet = false;
		this.strokeSet = false;

		this.fonts.clear();
		this.fontsUsed.clear();

		this.imageID.clear();

		this.pageX1 = 9999;
		this.pageX2 = -9999;
		this.pageY1 = -9999;
		this.pageY2 = 9999;

		this.lastScaling = 0;
	}

	/* remove all page objects and flush queue */
	@Override
	public void dispose() {

		this.singleImage = null;

		this.shapeType = null;
		this.pageObjects = null;
		this.objectType = null;
		this.areas = null;
		this.clips = null;
		this.x_coord = null;
		this.y_coord = null;
		this.textFillType = null;
		this.text_color = null;
		this.fill_color = null;
		this.stroke_color = null;
		this.stroke = null;

		this.TRvalues = null;

		this.imageOptions = null;

		this.fs = null;

		this.lw = null;

		this.af1 = null;
		this.af2 = null;
		this.af3 = null;
		this.af4 = null;

		this.fontBounds = null;

		this.opacity = null;

		this.largeImages = null;

		this.lastClip = null;

		this.lastStroke = null;

		this.lastAf = null;

		this.fonts = null;
		this.fontsUsed = null;

		this.imageID = null;

		this.storedImageValues = null;
	}

	private double minX = -1;

	private double minY = -1;

	private double maxX = -1;

	private double maxY = -1;

	private boolean renderFailed;

	/** optional frame for user to pass in - if present, error warning will be displayed */
	private Container frame = null;

	/** make sure user only gets 1 error message a session */
	private static boolean userAlerted = false;

	/* renders all the objects onto the g2 surface */
	@Override
	public Rectangle paint(Rectangle[] highlights, AffineTransform viewScaling, Rectangle userAnnot) {

		// if OCR we need to track over draw at end
		Vector_Rectangle ocr_highlights = null;
		HashMap ocr_used = null;
		if (this.hasOCR) {
			ocr_highlights = new Vector_Rectangle(4000);
			ocr_used = new HashMap(10);
		}

		// take a lock
		int currentThreadID = ++this.paintThreadID;
		this.paintThreadCount++;

		/**
		 * Keep track of drawn highlights so we don't draw multiple times
		 */
		if (highlights != null) {
			this.drawnHighlights = new boolean[highlights.length];
			for (int i = 0; i != this.drawnHighlights.length; i++)
				this.drawnHighlights[i] = false;
		}

		// ensure all other threads dead or this one killed (screen only)
		if (this.paintThreadCount > 1) {
			try {
				Thread.sleep(50);
			}
			catch (InterruptedException e) {
				// tell user and log
				if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
			}

			if (currentThreadID != this.paintThreadID) {
				this.paintThreadCount--;
				return null;
			}
		}

		final boolean debug = false;

		// int paintedCount=0;

		String fontUsed;
		float a = 0, b = 0, c = 0, d = 0;

		Rectangle dirtyRegion = null;

		// local copies
		int[] objectTypes = this.objectType.get();
		int[] textFill = this.textFillType.get();

		// currentItem to make the code work - can you let me know what you think
		int count = this.currentItem; // DO nOT CHANGE
		Area[] pageClips = this.clips.get();
		double[] afValues1 = this.af1.get();
		int[] fsValues = null;
		if (this.fs != null) fsValues = this.fs.get();

		Rectangle[] fontBounds = this.fontBounds.get();

		int[] lwValues = null;
		if (this.lw != null) lwValues = this.lw.get();
		double[] afValues2 = this.af2.get();
		double[] afValues3 = this.af3.get();
		double[] afValues4 = this.af4.get();
		Object[] text_color = this.text_color.get();
		Object[] fill_color = this.fill_color.get();

		Object[] stroke_color = this.stroke_color.get();
		Object[] pageObjects = this.pageObjects.get();

		Object[] javaObjects = this.javaObjects.get();
		Object[] stroke = this.stroke.get();
		int[] fillType = this.shapeType.get();

		float[] opacity = null;
		if (this.opacity != null) opacity = this.opacity.get();

		int[] TRvalues = null;
		if (this.TRvalues != null) TRvalues = this.TRvalues.get();

		Rectangle[] areas = null;

		if (this.areas != null) areas = this.areas.get();

		int[] imageOptions = null;
		if (this.imageOptions != null) imageOptions = this.imageOptions.get();

		Shape rawClip = this.g2.getClip();
		if (rawClip != null) dirtyRegion = rawClip.getBounds();

		boolean isInitialised = false;

		Shape defaultClip = this.g2.getClip();

		// used to optimise clipping
		Area clipToUse = null;
		boolean newClip = false;

		/**/
		if (this.noRepaint) this.noRepaint = false;
		else
			if (this.lastItemPainted == -1) {
				paintBackground(dirtyRegion);/**/
			}
		/** save raw scaling and apply any viewport */
		AffineTransform rawScaling = this.g2.getTransform();
		if (viewScaling != null) {
			this.g2.transform(viewScaling);
			defaultClip = this.g2.getClip(); // not valid if viewport so disable
		}

		// reset tracking of box
		this.minX = -1;
		this.minY = -1;
		this.maxX = -1;
		this.maxY = -1;

		Object currentObject;
		int type, textFillType, currentTR = GraphicsState.FILL;
		int lineWidth = 0;
		float fillOpacity = 1.0f;
		float strokeOpacity = 1.0f;
		float x, y;
		int iCount = 0, cCount = 0, sCount = 0, fsCount = -1, lwCount = 0, afCount = -1, tCount = 0, stCount = 0, fillCount = 0, strokeCount = 0, trCount = 0, opCount = 0, stringCount = 0;// note
																																															// af
																																															// is
																																															// 1
																																															// behind!
		PdfPaint textStrokeCol = null, textFillCol = null, fillCol = null, strokeCol = null;
		Stroke currentStroke = null;

		// if we reuse image this is pointer to live image
		int imageUsed;

		// use preset colours for T3 glyph
		if (this.colorsLocked) {
			strokeCol = this.strokeCol;
			fillCol = this.fillCol;
		}

		// setup first time something to highlight
		if (this.highlightsNeedToBeGenerated && areas != null && highlights != null) generateHighlights(this.g2, count, objectTypes, pageObjects, a,
				b, c, d, afValues1, afValues2, afValues3, afValues4, fsValues, fontBounds);

		/**
		 * now draw all shapes
		 */
		for (int i = 0; drawPDFShapes && i < count; i++) {

			// if(i>4800)
			// break;

			boolean ignoreItem = false;

			type = objectTypes[i];

			// ignore items flagged as deleted
			if (type == DynamicVectorRenderer.DELETED_IMAGE) continue;

			Rectangle currentArea = null;

			// exit if later paint recall
			if (currentThreadID != this.paintThreadID) {
				this.paintThreadCount--;

				return null;
			}

			if (type > 0) {

				x = this.x_coord[i];
				y = this.y_coord[i];

				currentObject = pageObjects[i];

				// swap in replacement image
				if (type == DynamicVectorRenderer.REUSED_IMAGE) {
					type = DynamicVectorRenderer.IMAGE;
					imageUsed = (Integer) currentObject;
					currentObject = pageObjects[imageUsed];
				}
				else imageUsed = -1;

				/**
				 * workout area occupied by glyf
				 */
				if (currentArea == null) currentArea = getObjectArea(afValues1, fsValues, afValues2, afValues3, afValues4, pageObjects, areas, type,
						x, y, fsCount, afCount, i);

				ignoreItem = false;

				// see if we need to draw
				if (currentArea != null) {

					// was glyphArea, changed back to currentArea to fix highlighting issue in Sams files.
					// last width test for odd print issue in phonobingo
					if (type < 7 && (userAnnot != null) && ((!userAnnot.intersects(currentArea))) && currentArea.width > 0) {
						ignoreItem = true;
					}
				}
				// }else if((optimiseDrawing)&&(rotation==0)&&(dirtyRegion!=null)&&(type!=DynamicVectorRenderer.STROKEOPACITY)&&
				// (type!=DynamicVectorRenderer.FILLOPACITY)&&(type!=DynamicVectorRenderer.CLIP)
				// &&(currentArea!=null)&&
				// ((!dirtyRegion.intersects(currentArea))))
				// ignoreItem=true;

				if (ignoreItem || (this.lastItemPainted != -1 && i < this.lastItemPainted)) {
					// keep local counts in sync
					switch (type) {

						case DynamicVectorRenderer.SHAPE:
							sCount++;
							break;
						case DynamicVectorRenderer.IMAGE:
							iCount++;
							break;
						case DynamicVectorRenderer.REUSED_IMAGE:
							iCount++;
							break;
						case DynamicVectorRenderer.CLIP:
							cCount++;
							break;
						case DynamicVectorRenderer.FONTSIZE:
							fsCount++;
							break;
						case DynamicVectorRenderer.LINEWIDTH:
							lwCount++;
							break;
						case DynamicVectorRenderer.TEXTCOLOR:
							tCount++;
							break;
						case DynamicVectorRenderer.FILLCOLOR:
							fillCount++;
							break;
						case DynamicVectorRenderer.STROKECOLOR:
							strokeCount++;
							break;
						case DynamicVectorRenderer.STROKE:
							stCount++;
							break;
						case DynamicVectorRenderer.TR:
							trCount++;
							break;
					}

				}
				else {

					if (!isInitialised && !this.stopG2setting) {

						if (userHints != null) {
							this.g2.setRenderingHints(userHints);
						}
						else {
							// set hints to produce high quality image
							this.g2.setRenderingHints(hints);
						}
						isInitialised = true;
					}

					// paintedCount++;

					if (currentTR == GraphicsState.INVISIBLE) {
						this.needsHighlights = true;
					}

					Rectangle highlight = null;

					switch (type) {

						case DynamicVectorRenderer.SHAPE:

							if (debug) System.out.println("Shape");

							if (newClip) {
								RenderUtils.renderClip(clipToUse, dirtyRegion, defaultClip, this.g2);
								newClip = false;
							}

							Shape s = null;
							if (this.endItem != -1 && this.endItem < i) {
								s = this.g2.getClip();
								this.g2.setClip(defaultClip);

							}

							renderShape(defaultClip, fillType[sCount], strokeCol, fillCol, currentStroke, (Shape) currentObject, strokeOpacity,
									fillOpacity);

							if (this.endItem != -1 && this.endItem < i) this.g2.setClip(s);

							sCount++;

							break;

						case DynamicVectorRenderer.TEXT:

							if (debug) System.out.println("Text");

							if (newClip) {
								RenderUtils.renderClip(clipToUse, dirtyRegion, defaultClip, this.g2);
								newClip = false;
							}

							if (!invertHighlight) highlight = setHighlightForGlyph(currentArea, highlights);

							if (this.hasOCR && highlight != null) {
								String key = highlight.x + " " + highlight.y;
								if (ocr_used.get(key) == null) {

									ocr_used.put(key, "x"); // avoid multiple additions
									ocr_highlights.addElement(highlight);
								}
							}

							AffineTransform def = this.g2.getTransform();

							renderHighlight(highlight, this.g2);

							this.g2.transform(new AffineTransform(afValues1[afCount], afValues2[afCount], -afValues3[afCount], -afValues4[afCount],
									x, y));

							renderText(x, y, currentTR, (Area) currentObject, highlight, textStrokeCol, textFillCol, strokeOpacity, fillOpacity);

							this.g2.setTransform(def);

							break;

						case DynamicVectorRenderer.TRUETYPE:

							if (debug) System.out.println("Truetype");

							if (newClip) {
								RenderUtils.renderClip(clipToUse, dirtyRegion, defaultClip, this.g2);
								newClip = false;
							}

							// hack to fix exceptions in a PDF using this code to create ReadOnly image
							if (afCount == -1) break;

							AffineTransform aff = new AffineTransform(afValues1[afCount], afValues2[afCount], afValues3[afCount], afValues4[afCount],
									x, y);

							if (!invertHighlight) highlight = setHighlightForGlyph(currentArea, highlights);

							if (this.hasOCR && highlight != null) {

								String key = highlight.x + " " + highlight.y;
								if (ocr_used.get(key) == null) {

									ocr_used.put(key, "x"); // avoid multiple additions
									ocr_highlights.addElement(highlight);
								}
							}

							renderHighlight(highlight, this.g2);
							renderEmbeddedText(currentTR, currentObject, DynamicVectorRenderer.TRUETYPE, aff, highlight, textStrokeCol, textFillCol,
									strokeOpacity, fillOpacity, lineWidth);

							break;

						case DynamicVectorRenderer.TYPE1C:

							if (debug) System.out.println("Type1c");

							if (newClip) {
								RenderUtils.renderClip(clipToUse, dirtyRegion, defaultClip, this.g2);
								newClip = false;
							}

							aff = new AffineTransform(afValues1[afCount], afValues2[afCount], afValues3[afCount], afValues4[afCount], x, y);

							if (!invertHighlight) highlight = setHighlightForGlyph(currentArea, highlights);

							if (this.hasOCR && highlight != null) {
								String key = highlight.x + " " + highlight.y;
								if (ocr_used.get(key) == null) {

									ocr_used.put(key, "x"); // avoid multiple additions
									ocr_highlights.addElement(highlight);

								}
							}

							renderHighlight(highlight, this.g2);

							renderEmbeddedText(currentTR, currentObject, DynamicVectorRenderer.TYPE1C, aff, highlight, textStrokeCol, textFillCol,
									strokeOpacity, fillOpacity, lineWidth);

							break;

						case DynamicVectorRenderer.TYPE3:

							if (debug) System.out.println("Type3");

							if (newClip) {
								RenderUtils.renderClip(clipToUse, dirtyRegion, defaultClip, this.g2);
								newClip = false;
							}

							aff = new AffineTransform(afValues1[afCount], afValues2[afCount], afValues3[afCount], afValues4[afCount], x, y);

							if (!invertHighlight) highlight = setHighlightForGlyph(currentArea, highlights);

							if (this.hasOCR && highlight != null) {
								String key = highlight.x + " " + highlight.y;
								if (ocr_used.get(key) == null) {

									ocr_used.put(key, "x"); // avoid multiple additions
									ocr_highlights.addElement(highlight);

								}
							}

							renderHighlight(highlight, this.g2);
							renderEmbeddedText(currentTR, currentObject, DynamicVectorRenderer.TYPE3, aff, highlight, textStrokeCol, textFillCol,
									strokeOpacity, fillOpacity, lineWidth);

							break;

						case DynamicVectorRenderer.IMAGE:

							if (newClip) {
								RenderUtils.renderClip(clipToUse, dirtyRegion, defaultClip, this.g2);
								newClip = false;
							}

							renderImage(afValues1, afValues2, afValues3, afValues4, pageObjects, imageOptions, currentObject, fillOpacity, x, y,
									iCount, afCount, imageUsed, i);

							iCount++;

							break;

						case DynamicVectorRenderer.CLIP:

							clipToUse = pageClips[cCount];
							newClip = true;

							cCount++;
							break;

						case DynamicVectorRenderer.AF:
							afCount++;
							break;
						case DynamicVectorRenderer.FONTSIZE:
							fsCount++;
							break;
						case DynamicVectorRenderer.LINEWIDTH:
							lineWidth = lwValues[lwCount];
							lwCount++;
							break;
						case DynamicVectorRenderer.TEXTCOLOR:

							if (debug) System.out.println("TextCOLOR");

							textFillType = textFill[tCount];

							// if(textColor==null || (textColor!=null && !(endItem!=-1 && i>=endItem) && !checkColorThreshold(((PdfPaint)
							// text_color[tCount]).getRGB()))){ //Not specified an overriding color

							if (textFillType == GraphicsState.STROKE) textStrokeCol = (PdfPaint) text_color[tCount];
							else textFillCol = (PdfPaint) text_color[tCount];

							// }else{ //Use specified overriding color
							// if(textFillType==GraphicsState.STROKE)
							// textStrokeCol = new PdfColor(textColor.getRed(), textColor.getGreen(), textColor.getBlue());
							// else
							// textFillCol = new PdfColor(textColor.getRed(), textColor.getGreen(), textColor.getBlue());
							// // }
							tCount++;
							break;
						case DynamicVectorRenderer.FILLCOLOR:

							if (debug) System.out.println("FillCOLOR");

							if (!this.colorsLocked) {
								fillCol = (PdfPaint) fill_color[fillCount];

								// if(textColor!=null && !(endItem!=-1 && i>=endItem) && checkColorThreshold(fillCol.getRGB())){
								// fillCol = new PdfColor(textColor.getRed(), textColor.getGreen(), textColor.getBlue());
								// }

							}
							fillCount++;

							break;
						case DynamicVectorRenderer.STROKECOLOR:

							if (debug) System.out.println("StrokeCOL");

							if (!this.colorsLocked) {

								strokeCol = (PdfPaint) stroke_color[strokeCount];

								// if(textColor!=null && !(endItem!=-1 && i>=endItem) && checkColorThreshold(strokeCol.getRGB())){
								// strokeCol = new PdfColor(textColor.getRed(), textColor.getGreen(), textColor.getBlue());
								// }

								if (strokeCol != null) strokeCol.setScaling(this.cropX, this.cropH, this.scaling, 0, 0);
							}

							strokeCount++;
							break;

						case DynamicVectorRenderer.STROKE:

							currentStroke = (Stroke) stroke[stCount];

							if (debug) System.out.println("STROKE");

							stCount++;
							break;

						case DynamicVectorRenderer.TR:

							if (debug) System.out.println("TR");

							currentTR = TRvalues[trCount];
							trCount++;
							break;

						case DynamicVectorRenderer.STROKEOPACITY:

							if (debug) System.out.println("Stroke Opacity " + opacity[opCount] + " opCount=" + opCount);

							strokeOpacity = opacity[opCount];
							opCount++;
							break;

						case DynamicVectorRenderer.FILLOPACITY:

							if (debug) System.out.println("Set Fill Opacity " + opacity[opCount] + " count=" + opCount);

							fillOpacity = opacity[opCount];
							opCount++;
							break;

						case DynamicVectorRenderer.STRING:

							Shape s1 = this.g2.getClip();
							this.g2.setClip(defaultClip);
							AffineTransform defaultAf = this.g2.getTransform();
							String displayValue = (String) currentObject;

							double[] af = new double[6];

							this.g2.getTransform().getMatrix(af);

							if (af[2] != 0) af[2] = -af[2];
							if (af[3] != 0) af[3] = -af[3];
							this.g2.setTransform(new AffineTransform(af));

							Font javaFont = (Font) javaObjects[stringCount];

							this.g2.setFont(javaFont);

							if ((currentTR & GraphicsState.FILL) == GraphicsState.FILL) {

								if (textFillCol != null) textFillCol.setScaling(this.cropX, this.cropH, this.scaling, 0, 0);

								if (this.customColorHandler != null) {
									this.customColorHandler.setPaint(this.g2, textFillCol, this.pageNumber, this.isPrinting);
								}
								else
									if (PdfDecoder.Helper != null) {
										PdfDecoder.Helper.setPaint(this.g2, textFillCol, this.pageNumber, this.isPrinting);
									}
									else this.g2.setPaint(textFillCol);

							}

							if ((currentTR & GraphicsState.STROKE) == GraphicsState.STROKE) {

								if (textStrokeCol != null) textStrokeCol.setScaling(this.cropX, this.cropH, this.scaling, 0, 0);

								if (this.customColorHandler != null) {
									this.customColorHandler.setPaint(this.g2, textFillCol, this.pageNumber, this.isPrinting);
								}
								else
									if (PdfDecoder.Helper != null) {
										PdfDecoder.Helper.setPaint(this.g2, textFillCol, this.pageNumber, this.isPrinting);
									}
									else this.g2.setPaint(textFillCol);

							}

							this.g2.drawString(displayValue, x, y);

							// restore defaults
							this.g2.setTransform(defaultAf);
							this.g2.setClip(s1);

							stringCount++;

							break;

						case DynamicVectorRenderer.XFORM:

							renderXForm((DynamicVectorRenderer) currentObject, fillOpacity);
							break;

						case DynamicVectorRenderer.CUSTOM:

							Shape s2 = this.g2.getClip();
							this.g2.setClip(defaultClip);
							AffineTransform af2 = this.g2.getTransform();

							JPedalCustomDrawObject customObj = (JPedalCustomDrawObject) currentObject;
							if (this.isPrinting) customObj.print(this.g2, this.pageNumber);
							else customObj.paint(this.g2);

							this.g2.setTransform(af2);
							this.g2.setClip(s2);

							break;

					}
				}
			}
		}

		// needs to be before we return defualts to factor
		// in a viewport for abacus
		if (this.needsHighlights && highlights != null) {
			for (int h = 0; h != highlights.length; h++) {
				this.ignoreHighlight = false;
				renderHighlight(highlights[h], this.g2);
			}
		}

		// draw OCR highlights at end
		if (ocr_highlights != null) {
			Rectangle[] highlights2 = ocr_highlights.get();

			// Backup current g2 paint and composite
			Composite comp = this.g2.getComposite();
			Paint p = this.g2.getPaint();

			for (int h = 0; h != highlights2.length; h++) {
				if (highlights2[h] != null) {

					// Set new values for highlight
					this.g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, PdfDecoder.highlightComposite));

					this.g2.setPaint(DecoderOptions.highlightColor);

					// Draw highlight
					this.g2.fill(highlights2[h]);

				}
				// Reset to starting values
				this.g2.setComposite(comp);
				this.g2.setPaint(p);

			}
		}

		// restore defaults
		this.g2.setClip(defaultClip);

		this.g2.setTransform(rawScaling);

		// if(DynamicVectorRenderer.debugPaint)
		// System.err.println("Painted "+paintedCount);

		// tell user if problem
		if (this.frame != null && this.renderFailed && !userAlerted) {

			userAlerted = true;

			if (PdfDecoder.showErrorMessages) {
				String status = (Messages.getMessage("PdfViewer.ImageDisplayError") + Messages.getMessage("PdfViewer.ImageDisplayError1")
						+ Messages.getMessage("PdfViewer.ImageDisplayError2") + Messages.getMessage("PdfViewer.ImageDisplayError3")
						+ Messages.getMessage("PdfViewer.ImageDisplayError4") + Messages.getMessage("PdfViewer.ImageDisplayError5")
						+ Messages.getMessage("PdfViewer.ImageDisplayError6") + Messages.getMessage("PdfViewer.ImageDisplayError7"));

				JOptionPane.showMessageDialog(this.frame, status);

				this.frame.invalidate();
				this.frame.repaint();
			}
		}

		// reduce count
		this.paintThreadCount--;

		// track so we do not redo onto raster
		if (this.optimsePainting) {
			this.lastItemPainted = count;
		}
		else this.lastItemPainted = -1;

		// track
		this.lastScaling = this.scaling;

		// if we highlighted text return oversized
		if (this.minX == -1) return null;
		else return new Rectangle((int) this.minX, (int) this.minY, (int) (this.maxX - this.minX), (int) (this.maxY - this.minY));
	}

	private static Rectangle getObjectArea(double[] afValues1, int[] fsValues, double[] afValues2, double[] afValues3, double[] afValues4,
			Object[] pageObjects, Rectangle[] areas, int type, float x, float y, int fsCount, int afCount, int i) {

		Rectangle currentArea = null;

		if (afValues1 != null && type == DynamicVectorRenderer.IMAGE) {

			if (areas != null) currentArea = areas[i];

		}
		else
			if (afValues1 != null && type == DynamicVectorRenderer.SHAPE) {

				currentArea = ((Shape) pageObjects[i]).getBounds();

			}
			else
				if (type == DynamicVectorRenderer.TEXT && afCount > -1) {

					// Use on page coords to make sure the glyph needs highlighting
					currentArea = RenderUtils.getAreaForGlyph(new float[][] { { (float) afValues1[afCount], (float) afValues2[afCount], 0 },
							{ (float) afValues3[afCount], (float) afValues4[afCount], 0 }, { x, y, 1 } });

				}
				else
					if (fsCount != -1 && afValues1 != null) {// && afCount>-1){

						int realSize = fsValues[fsCount];
						if (realSize < 0) // ignore sign which is used as flag elsewhere
						currentArea = new Rectangle((int) x + realSize, (int) y, -realSize, -realSize);
						else currentArea = new Rectangle((int) x, (int) y, realSize, realSize);
					}
		return currentArea;
	}

	private void renderImage(double[] afValues1, double[] afValues2, double[] afValues3, double[] afValues4, Object[] pageObjects,
			int[] imageOptions, Object currentObject, float fillOpacity, float x, float y, int iCount, int afCount, int imageUsed, int i) {

		int currentImageOption = PDFImageProcessing.NOTHING;
		if (imageOptions != null) currentImageOption = imageOptions[iCount];

		int sampling = 1, w1 = 0, pY = 0, defaultSampling = 1;

		// generate unique value to every image on given page (no more overighting stuff in the hashmap)
		String key = Integer.toString(this.pageNumber) + Integer.toString(iCount);

		// useHiResImageForDisplay added back by Mark as break memory images - use customers1/annexe1.pdf to test any changes

		// now draw the image (hires or downsampled)
		if (this.useHiResImageForDisplay) {

			double aa = 1;
			if (sampling >= 1 && this.scaling > 1 && w1 > 0) // factor in any scaling
			aa = ((float) sampling) / defaultSampling;

			AffineTransform imageAf = new AffineTransform(afValues1[afCount] * aa, afValues2[afCount] * aa, afValues3[afCount] * aa,
					afValues4[afCount] * aa, x, y);

			// get image and reload if needed
			BufferedImage img = null;
			if (currentObject != null) img = (BufferedImage) currentObject;

			if (currentObject == null) img = reloadCachedImage(imageUsed, i, img);

			if (img != null) renderImage(imageAf, img, fillOpacity, null, x, y, currentImageOption);

		}
		else {

			AffineTransform before = this.g2.getTransform();
			this.extraRot = false;

			if (pY > 0) {

				double[] matrix = new double[6];
				this.g2.getTransform().getMatrix(matrix);
				double ratio = ((float) pY) / ((BufferedImage) currentObject).getHeight();

				matrix[0] = ratio;
				matrix[1] = 0;
				matrix[2] = 0;
				matrix[3] = -ratio;

				this.g2.scale(1f / this.scaling, 1f / this.scaling);

				this.g2.setTransform(new AffineTransform(matrix));

			}
			else {
				this.extraRot = true;
			}

			renderImage(null, (BufferedImage) currentObject, fillOpacity, null, x, y, currentImageOption);
			this.g2.setTransform(before);
		}
	}

	private BufferedImage resampleImageData(int sampling, int w1, int h1, byte[] maskCol, int newW, int newH, String key, int ID) {

		// get data
		byte[] data = this.objectStoreRef.getRawImageData(key);

		// make 1 bit indexed flat
		byte[] index = null;
		if (maskCol != null && ID != ColorSpaces.DeviceRGB) index = maskCol;

		int size = newW * newH;
		if (index != null) size = size * 3;

		byte[] newData = new byte[size];

		final int[] flag = { 1, 2, 4, 8, 16, 32, 64, 128 };

		int origLineLength = (w1 + 7) >> 3;

		int offset = 0;

		for (int y1 = 0; y1 < newH; y1++) {
			for (int x1 = 0; x1 < newW; x1++) {

				int bytes = 0, count1 = 0;

				// allow for edges in number of pixels left
				int wCount = sampling, hCount = sampling;
				int wGapLeft = w1 - x1;
				int hGapLeft = h1 - y1;
				if (wCount > wGapLeft) wCount = wGapLeft;
				if (hCount > hGapLeft) hCount = hGapLeft;

				// count pixels in sample we will make into a pixel (ie 2x2 is 4 pixels , 4x4 is 16 pixels)
				int ptr;
				byte currentByte;
				for (int yy = 0; yy < hCount; yy++) {
					for (int xx = 0; xx < wCount; xx++) {

						ptr = ((yy + (y1 * sampling)) * origLineLength) + (((x1 * sampling) + xx) >> 3);
						if (ptr < data.length) {
							currentByte = data[ptr];
						}
						else {
							currentByte = (byte) 255;
						}

						int bit = currentByte & flag[7 - (((x1 * sampling) + xx) & 7)];

						if (bit != 0) bytes++;
						count1++;

					}
				}

				// set value as white or average of pixels
				if (count1 > 0) {

					if (index == null) {
						newData[x1 + (newW * y1)] = (byte) ((255 * bytes) / count1);
					}
					else {
						for (int ii = 0; ii < 3; ii++) {
							if (bytes / count1 < 0.5f) newData[offset] = (byte) ((((maskCol[ii] & 255))));
							else newData[offset] = (byte) 255;

							offset++;

						}
					}
				}
				else {
					if (index == null) {
						newData[x1 + (newW * y1)] = (byte) 255;
					}
					else {
						for (int ii = 0; ii < 3; ii++) {
							newData[offset] = (byte) 255;

							offset++;
						}
					}
				}
			}
		}

		/**
		 * build the image
		 */
		BufferedImage image = null;
		Raster raster;
		int type = BufferedImage.TYPE_BYTE_GRAY;
		DataBuffer db = new DataBufferByte(newData, newData.length);
		int[] bands = { 0 };
		int count = 1;

		if (maskCol == null && (w1 * h1 * 3 == data.length)) {// && ID!=ColorSpaces.DeviceRGB){ //use this set of values for this case
			type = BufferedImage.TYPE_INT_RGB;
			bands = new int[] { 0, 1, 2 };
			count = 3;
		}

		image = new BufferedImage(newW, newH, type);
		raster = Raster.createInterleavedRaster(db, newW, newH, newW * count, count, bands, null);
		image.setData(raster);

		return image;
	}

	private BufferedImage reloadCachedImage(int imageUsed, int i, BufferedImage img) {
		Object currentObject;
		try {

			// cache single images in memory for speed
			if (this.singleImage != null) {
				currentObject = this.singleImage.getSubimage(0, 0, this.singleImage.getWidth(), this.singleImage.getHeight());

				/**
				 * load from memory or disk
				 */
			}
			else
				if (this.rawKey == null) currentObject = this.largeImages.get("HIRES_" + i);
				else currentObject = this.largeImages.get("HIRES_" + i + '_' + this.rawKey);

			if (currentObject == null) {

				int keyID = i;
				if (imageUsed != -1) keyID = imageUsed;

				if (this.rawKey == null) currentObject = this.objectStoreRef.loadStoredImage(this.pageNumber + "_HIRES_" + keyID);
				else currentObject = this.objectStoreRef.loadStoredImage(this.pageNumber + "_HIRES_" + keyID + '_' + this.rawKey);

				// flag if problem
				if (currentObject == null) this.renderFailed = true;

				// recache
				if (!this.isPrinting) {

					if (this.rawKey == null) this.largeImages.put("HIRES_" + i, currentObject);
					else this.largeImages.put("HIRES_" + i, currentObject + "_" + this.rawKey);
				}
			}

			img = (BufferedImage) currentObject;

		}
		catch (Exception e) {
			// tell user and log
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
		}
		return img;
	}

	/**
	 * allow user to set component for waring message in renderer to appear - if unset no message will appear
	 * 
	 * @param frame
	 */
	@Override
	public void setMessageFrame(Container frame) {
		this.frame = frame;
	}

	/**
	 * highlight a glyph by reversing the display. For white text, use black
	 */
	private Rectangle setHighlightForGlyph(Rectangle area, Rectangle[] highlights) {

		if (highlights == null || this.textHighlightsX == null) return null;

		this.ignoreHighlight = false;
		for (int j = 0; j != highlights.length; j++) {
			if (highlights[j] != null && area != null && (highlights[j].intersects(area))) {

				// Get intersection of the two areas
				Rectangle intersection = highlights[j].intersection(area);

				// Intersection area between highlight and text area
				float iArea = intersection.width * intersection.height;

				// 25% of text area
				float tArea = (area.width * area.height) / 4f;

				// Only highlight if (x.y) is with highlight and more than 25% intersects
				// or intersect is greater than 60%
				if ((highlights[j].contains(area.x, area.y) && iArea > tArea) || iArea > (area.width * area.height) / 1.667f) {
					if (!this.drawnHighlights[j]) {
						this.ignoreHighlight = false;
						this.drawnHighlights[j] = true;
						return highlights[j];
					}
					else {
						this.ignoreHighlight = true;
						return highlights[j];
					}
				}
			}
		}

		// old code not used
		return null;
	}

	/* saves text object with attributes for rendering */
	@Override
	public void drawText(float[][] Trm, String text, GraphicsState currentGraphicsState, float x, float y, Font javaFont) {

		/**
		 * set color first
		 */
		PdfPaint currentCol = null;

		if (Trm != null) {
			double[] nextAf = new double[] { Trm[0][0], Trm[0][1], Trm[1][0], Trm[1][1], Trm[2][0], Trm[2][1] };

			if ((this.lastAf[0] == nextAf[0]) && (this.lastAf[1] == nextAf[1]) && (this.lastAf[2] == nextAf[2]) && (this.lastAf[3] == nextAf[3])) {}
			else {
				this.drawAffine(nextAf);
				this.lastAf[0] = nextAf[0];
				this.lastAf[1] = nextAf[1];
				this.lastAf[2] = nextAf[2];
				this.lastAf[3] = nextAf[3];
			}
		}

		int text_fill_type = currentGraphicsState.getTextRenderType();

		// for a fill
		if ((text_fill_type & GraphicsState.FILL) == GraphicsState.FILL) {
			currentCol = currentGraphicsState.getNonstrokeColor();

			if (currentCol.isPattern()) {
				drawColor(currentCol, GraphicsState.FILL);
				this.resetTextColors = true;
			}
			else {

				int newCol = (currentCol).getRGB();
				if ((this.resetTextColors) || ((this.lastFillTextCol != newCol))) {
					this.lastFillTextCol = newCol;
					drawColor(currentCol, GraphicsState.FILL);
				}
			}
		}

		// and/or do a stroke
		if ((text_fill_type & GraphicsState.STROKE) == GraphicsState.STROKE) {
			currentCol = currentGraphicsState.getStrokeColor();

			if (currentCol.isPattern()) {
				drawColor(currentCol, GraphicsState.STROKE);
				this.resetTextColors = true;
			}
			else {

				int newCol = currentCol.getRGB();
				if ((this.resetTextColors) || (this.lastStrokeCol != newCol)) {
					this.lastStrokeCol = newCol;
					drawColor(currentCol, GraphicsState.STROKE);
				}
			}
		}

		this.pageObjects.addElement(text);
		this.javaObjects.addElement(javaFont);

		this.objectType.addElement(DynamicVectorRenderer.STRING);

		// add to check for transparency if large
		int fontSize = javaFont.getSize();
		if (fontSize > 100) this.areas.addElement(new Rectangle((int) x, (int) y, fontSize, fontSize));
		else this.areas.addElement(null);

		this.x_coord = RenderUtils.checkSize(this.x_coord, this.currentItem);
		this.y_coord = RenderUtils.checkSize(this.y_coord, this.currentItem);
		this.x_coord[this.currentItem] = x;
		this.y_coord[this.currentItem] = y;

		this.currentItem++;

		this.resetTextColors = false;
	}

	/** workout combined area of shapes in an area */
	@Override
	public Rectangle getCombinedAreas(Rectangle targetRectangle, boolean justText) {

		Rectangle combinedRectangle = null;

		if (this.areas != null) {

			// set defaults for new area
			Rectangle target = targetRectangle.getBounds();
			int x2 = target.x;
			int y2 = target.y;
			int x1 = x2 + target.width;
			int y1 = y2 + target.height;

			boolean matchFound = false;

			Rectangle[] currentAreas = this.areas.get();

			// find all items enclosed by this rectangle
			for (Rectangle currentArea : currentAreas) {
				if ((currentArea != null) && (targetRectangle.contains(currentArea))) {
					matchFound = true;

					int newX = currentArea.x;
					if (x1 > newX) x1 = newX;
					newX = currentArea.x + currentArea.width;
					if (x2 < newX) x2 = newX;

					int newY = currentArea.y;
					if (y1 > newY) y1 = newY;
					newY = currentArea.y + currentArea.height;
					if (y2 < newY) y2 = newY;
				}
			}

			// allow margin of 1 around object
			if (matchFound) {
				combinedRectangle = new Rectangle(x1 - 1, y1 + 1, (x2 - x1) + 2, (y2 - y1) + 2);

			}
		}

		return combinedRectangle;
	}

	/* save image in array to draw */
	@Override
	public int drawImage(int pageNumber, BufferedImage image, GraphicsState currentGraphicsState, boolean alreadyCached, String name,
			int optionsApplied, int previousUse) {

		if (previousUse != -1) return redrawImage(pageNumber, currentGraphicsState, name, previousUse);

		this.pageNumber = pageNumber;
		float CTM[][] = currentGraphicsState.CTM;

		float x = currentGraphicsState.x;
		float y = currentGraphicsState.y;

		double[] nextAf = new double[6];

		boolean cacheInMemory = (image.getWidth() < 100 && image.getHeight() < 100) || image.getHeight() == 1;

		String key;
		if (this.rawKey == null) key = pageNumber + "_" + (this.currentItem + 1);
		else key = this.rawKey + '_' + (this.currentItem + 1);

		if (this.imageOptions == null) {
			this.imageOptions = new Vector_Int(defaultSize);
			this.imageOptions.setCheckpoint();
		}

		// for special case on Page 7 randomHouse/9780857510839
		boolean oddRotationCase = optionsApplied == 0 && CTM[0][0] < 0 && CTM[0][1] > 0 && CTM[1][0] < 0 && CTM[1][1] < 0 && this.pageRotation == 0
				&& this.type == 1;

		// Turn image around if needed
		// (avoid if has skew on as well as currently breaks image)
		if (!alreadyCached && image.getHeight() > 1 && ((optionsApplied & PDFImageProcessing.IMAGE_INVERTED) != PDFImageProcessing.IMAGE_INVERTED)) {

			boolean turnLater = (this.optimisedTurnCode && (CTM[0][0] * CTM[0][1] == 0) && (CTM[1][1] * CTM[1][0] == 0) && !RenderUtils
					.isRotated(CTM));

			if ((!this.optimisedTurnCode || !turnLater) && this.pageRotation != 90 && this.pageRotation != 270) {
				if (this.type == 3 || oddRotationCase) image = RenderUtils.invertImage(CTM, image);
			}

			if (turnLater) optionsApplied = optionsApplied + PDFImageProcessing.TURN_ON_DRAW;

		}

		this.imageOptions.addElement(optionsApplied);

		if (this.useHiResImageForDisplay) {

			int w, h;

			if (!alreadyCached || this.cachedWidths.get(key) == null) {
				w = image.getWidth();
				h = image.getHeight();
			}
			else {
				w = (Integer) this.cachedWidths.get(key);
				h = (Integer) this.cachedHeights.get(key);
			}

			boolean isRotated = RenderUtils.isRotated(CTM);
			if (isRotated) {

				if ((optionsApplied & PDFImageProcessing.IMAGE_ROTATED) != PDFImageProcessing.IMAGE_ROTATED) { // fix for odd rotated behaviour

					AffineTransform tx = new AffineTransform();
					tx.rotate(Math.PI / 2, w / 2, h / 2);
					tx.translate(-(h - tx.getTranslateX()), -tx.getTranslateY());

					// allow for 1 pixel high
					double[] matrix = new double[6];
					tx.getMatrix(matrix);
					if (matrix[4] < 1) {
						matrix[4] = 1;
						tx = new AffineTransform(matrix);
					}

					AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_BILINEAR);

					if (image != null) {

						if (image.getHeight() > 1 && image.getWidth() > 1) image = op.filter(image, null);

						if (RenderUtils.isInverted(CTM) && ((optionsApplied & PDFImageProcessing.IMAGE_ROTATED) != PDFImageProcessing.IMAGE_ROTATED)) {
							// turn upside down
							AffineTransform image_at2 = new AffineTransform();
							image_at2.scale(-1, 1);
							image_at2.translate(-image.getWidth(), 0);

							AffineTransformOp invert3 = new AffineTransformOp(image_at2, ColorSpaces.hints);

							if (image.getType() == 12) { // avoid turning into ARGB
								BufferedImage source = image;
								image = new BufferedImage(source.getWidth(), source.getHeight(), source.getType());

								invert3.filter(source, image);
							}
							else image = invert3.filter(image, null);
						}
					}

					float[][] scaleDown = { { 0, 1f / h, 0 }, { 1f / w, 0, 0 }, { 0, 0, 1 } };
					CTM = Matrix.multiply(scaleDown, CTM);

				}
				else {
					float[][] scaleDown = { { 0, 1f / w, 0 }, { 1f / h, 0, 0 }, { 0, 0, 1 } };
					CTM = Matrix.multiply(scaleDown, CTM);
				}

			}
			else {
				float[][] scaleDown = { { 1f / w, 0, 0 }, { 0, 1f / h, 0 }, { 0, 0, 1 } };
				CTM = Matrix.multiply(scaleDown, CTM);
			}

			AffineTransform upside_down = null;

			upside_down = new AffineTransform(CTM[0][0], CTM[0][1], CTM[1][0], CTM[1][1], 0, 0);

			upside_down.getMatrix(nextAf);

			this.drawAffine(nextAf);

			this.lastAf[0] = nextAf[0];
			this.lastAf[1] = nextAf[1];
			this.lastAf[2] = nextAf[2];
			this.lastAf[3] = nextAf[3];

			if (!alreadyCached && !cacheInMemory) {

				if (!this.isPrinting) {
					if (this.rawKey == null) {
						this.largeImages.put("HIRES_" + this.currentItem, image);
					}
					else this.largeImages.put("HIRES_" + this.currentItem + '_' + this.rawKey, image);

					// cache PDF with single image for speed
					if (this.imageCount == 0) {
						this.singleImage = image.getSubimage(0, 0, image.getWidth(), image.getHeight());

						this.imageCount++;
					}
					else this.singleImage = null;
				}
				if (this.rawKey == null) {
					this.objectStoreRef.saveStoredImage(pageNumber + "_HIRES_" + this.currentItem, image, false, false, "tif");
					this.imageIDtoName.put(this.currentItem, pageNumber + "_HIRES_" + this.currentItem);
				}
				else {
					this.objectStoreRef.saveStoredImage(pageNumber + "_HIRES_" + this.currentItem + '_' + this.rawKey, image, false, false, "tif");
					this.imageIDtoName.put(this.currentItem, pageNumber + "_HIRES_" + this.currentItem + '_' + this.rawKey);
				}

				if (this.rawKey == null) key = pageNumber + "_" + this.currentItem;
				else key = this.rawKey + '_' + this.currentItem;

				this.cachedWidths.put(key, w);
				this.cachedHeights.put(key, h);
			}
		}

		this.x_coord = RenderUtils.checkSize(this.x_coord, this.currentItem);
		this.y_coord = RenderUtils.checkSize(this.y_coord, this.currentItem);
		this.x_coord[this.currentItem] = x;
		this.y_coord[this.currentItem] = y;

		this.objectType.addElement(DynamicVectorRenderer.IMAGE);
		float WidthModifier = 1;
		float HeightModifier = 1;

		if (this.useHiResImageForDisplay) {
			if (!alreadyCached) {
				WidthModifier = image.getWidth();
				HeightModifier = image.getHeight();
			}
			else {
				WidthModifier = (Integer) this.cachedWidths.get(key);
				HeightModifier = (Integer) this.cachedHeights.get(key);
			}
		}

		// ignore in this case /PDFdata/baseline_screens/customers3/1773_A2.pdf
		if (CTM[0][0] > 0 && CTM[0][0] < 0.05 && CTM[0][1] != 0 && CTM[1][0] != 0 && CTM[1][1] != 0) {
			this.areas.addElement(null);
		}
		else {
			this.w = (int) (CTM[0][0] * WidthModifier);
			if (this.w == 0) this.w = (int) (CTM[0][1] * WidthModifier);
			this.h = (int) (CTM[1][1] * HeightModifier);
			if (this.h == 0) this.h = (int) (CTM[1][0] * HeightModifier);

			// fix for bug if sheered in low res
			if (!this.useHiResImageForDisplay && CTM[1][0] < 0 && CTM[0][1] > 0 && CTM[0][0] == 0 && CTM[1][1] == 0) {
				int tmp = this.w;
				this.w = -this.h;
				this.h = tmp;
			}

			// corrected in generation
			if (this.h < 0 && !this.useHiResImageForDisplay) this.h = -this.h;

			// fix negative height on Ghostscript image in printing
			int x1 = (int) currentGraphicsState.x;
			int y1 = (int) currentGraphicsState.y;
			int w1 = this.w;
			int h1 = this.h;
			if (h1 < 0) {
				y1 = y1 + h1;
				h1 = -h1;
			}

			if (h1 == 0) h1 = 1;

			Rectangle rect = new Rectangle(x1, y1, w1, h1);

			this.areas.addElement(rect);

			checkWidth(rect);
		}

		if (this.useHiResImageForDisplay && !cacheInMemory) {
			this.pageObjects.addElement(null);
		}
		else this.pageObjects.addElement(image);

		// store id so we can get as low res image
		this.imageID.put(name, this.currentItem);

		// nore minus one as affine not yet done
		this.storedImageValues.put("imageOptions-" + this.currentItem, optionsApplied);
		this.storedImageValues.put("imageAff-" + this.currentItem, nextAf);

		this.currentItem++;

		return this.currentItem - 1;
	}

	/* save image in array to draw */
	private int redrawImage(int pageNumber, GraphicsState currentGraphicsState, String name, int previousUse) {

		this.pageNumber = pageNumber;

		float x = currentGraphicsState.x;
		float y = currentGraphicsState.y;

		this.imageOptions.addElement((Integer) this.storedImageValues.get("imageOptions-" + previousUse));

		if (this.useHiResImageForDisplay) {}

		this.x_coord = RenderUtils.checkSize(this.x_coord, this.currentItem);
		this.y_coord = RenderUtils.checkSize(this.y_coord, this.currentItem);
		this.x_coord[this.currentItem] = x;
		this.y_coord[this.currentItem] = y;

		this.objectType.addElement(DynamicVectorRenderer.REUSED_IMAGE);

		Rectangle previousRectangle = this.areas.elementAt(previousUse);

		Rectangle newRect = null;

		if (previousRectangle != null) newRect = new Rectangle((int) x, (int) y, previousRectangle.width, previousRectangle.height);

		this.areas.addElement(newRect);

		if (previousRectangle != null) checkWidth(newRect);

		this.pageObjects.addElement(previousUse);

		// store id so we can get as low res image
		this.imageID.put(name, previousUse);

		this.currentItem++;

		return this.currentItem - 1;
	}

	/**
	 * track actual size of shape
	 */
	private void checkWidth(Rectangle rect) {

		int x1 = rect.getBounds().x;
		int y2 = rect.getBounds().y;
		int y1 = y2 + rect.getBounds().height;
		int x2 = x1 + rect.getBounds().width;

		if (x1 < this.pageX1) this.pageX1 = x1;
		if (x2 > this.pageX2) this.pageX2 = x2;

		if (y1 > this.pageY1) this.pageY1 = y1;
		if (y2 < this.pageY2) this.pageY2 = y2;
	}

	/**
	 * @return which part of page drawn onto
	 */
	@Override
	public Rectangle getOccupiedArea() {
		return new Rectangle(this.pageX1, this.pageY1, (this.pageX2 - this.pageX1), (this.pageY1 - this.pageY2));
	}

	/* save shape in array to draw */
	@Override
	public void drawShape(Shape currentShape, GraphicsState currentGraphicsState, int cmd) {

		int fillType = currentGraphicsState.getFillType();
		PdfPaint currentCol;

		int newCol;

		// check for 1 by 1 complex shape and replace with dot
		if (currentShape.getBounds().getWidth() == 1 && currentShape.getBounds().getHeight() == 1 && currentGraphicsState.getLineWidth() < 1) currentShape = new Rectangle(
				currentShape.getBounds().x, currentShape.getBounds().y, 1, 1);

		// stroke and fill (do fill first so we don't overwrite Stroke)
		if (fillType == GraphicsState.FILL || fillType == GraphicsState.FILLSTROKE) {

			currentCol = currentGraphicsState.getNonstrokeColor();
			if (currentCol.isPattern()) {

				drawFillColor(currentCol);
				this.fillSet = true;
			}
			else {
				newCol = currentCol.getRGB();
				if ((!this.fillSet) || (this.lastFillCol != newCol)) {
					this.lastFillCol = newCol;
					drawFillColor(currentCol);
					this.fillSet = true;
				}
			}
		}

		if ((fillType == GraphicsState.STROKE) || (fillType == GraphicsState.FILLSTROKE)) {

			currentCol = currentGraphicsState.getStrokeColor();

			if (currentCol instanceof Color) {
				newCol = (currentCol).getRGB();

				if ((!this.strokeSet) || (this.lastStrokeCol != newCol)) {
					this.lastStrokeCol = newCol;
					drawStrokeColor(currentCol);
					this.strokeSet = true;
				}
			}
			else {
				drawStrokeColor(currentCol);
				this.strokeSet = true;
			}
		}

		Stroke newStroke = currentGraphicsState.getStroke();
		if ((this.lastStroke != null) && (this.lastStroke.equals(newStroke))) {

		}
		else {
			this.lastStroke = newStroke;
			drawStroke((newStroke));
		}

		this.pageObjects.addElement(currentShape);
		this.objectType.addElement(DynamicVectorRenderer.SHAPE);
		this.areas.addElement(currentShape.getBounds());

		checkWidth(currentShape.getBounds());

		this.x_coord = RenderUtils.checkSize(this.x_coord, this.currentItem);
		this.y_coord = RenderUtils.checkSize(this.y_coord, this.currentItem);
		this.x_coord[this.currentItem] = currentGraphicsState.x;
		this.y_coord[this.currentItem] = currentGraphicsState.y;

		this.shapeType.addElement(fillType);
		this.currentItem++;

		this.resetTextColors = true;
	}

	/* save text colour */
	@Override
	public void drawColor(PdfPaint currentCol, int type) {

		this.areas.addElement(null);
		this.pageObjects.addElement(null);
		this.objectType.addElement(DynamicVectorRenderer.TEXTCOLOR);
		this.textFillType.addElement(type); // used to flag which has changed

		this.text_color.addElement(currentCol);

		this.x_coord = RenderUtils.checkSize(this.x_coord, this.currentItem);
		this.y_coord = RenderUtils.checkSize(this.y_coord, this.currentItem);
		this.x_coord[this.currentItem] = 0;
		this.y_coord[this.currentItem] = 0;

		this.currentItem++;

		// ensure any shapes reset color
		this.strokeSet = false;
		this.fillSet = false;
	}

	/* add XForm object */
	@Override
	public void drawXForm(DynamicVectorRenderer dvr, GraphicsState gs) {

		this.areas.addElement(null);
		this.pageObjects.addElement(dvr);
		this.objectType.addElement(DynamicVectorRenderer.XFORM);

		this.x_coord = RenderUtils.checkSize(this.x_coord, this.currentItem);
		this.y_coord = RenderUtils.checkSize(this.y_coord, this.currentItem);

		this.x_coord[this.currentItem] = 0;
		this.y_coord[this.currentItem] = 0;

		this.currentItem++;
	}

	/** reset on colorspace change to ensure cached data up to data */
	@Override
	public void resetOnColorspaceChange() {

		this.fillSet = false;
		this.strokeSet = false;
	}

	/* save shape colour */
	@Override
	public void drawFillColor(PdfPaint currentCol) {

		this.pageObjects.addElement(null);
		this.objectType.addElement(DynamicVectorRenderer.FILLCOLOR);
		this.areas.addElement(null);

		this.fill_color.addElement(currentCol);

		this.x_coord = RenderUtils.checkSize(this.x_coord, this.currentItem);
		this.y_coord = RenderUtils.checkSize(this.y_coord, this.currentItem);
		this.x_coord[this.currentItem] = 0;
		this.y_coord[this.currentItem] = 0;

		this.currentItem++;

		this.lastFillCol = currentCol.getRGB();
	}

	/* save opacity settings */
	@Override
	public void setGraphicsState(int fillType, float value) {

		if (value != 1.0f || this.opacity != null) {

			if (this.opacity == null) {
				this.opacity = new Vector_Float(defaultSize);
				this.opacity.setCheckpoint();
			}

			this.pageObjects.addElement(null);
			this.areas.addElement(null);

			if (fillType == GraphicsState.STROKE) this.objectType.addElement(DynamicVectorRenderer.STROKEOPACITY);
			else this.objectType.addElement(DynamicVectorRenderer.FILLOPACITY);

			this.opacity.addElement(value);

			this.x_coord = RenderUtils.checkSize(this.x_coord, this.currentItem);
			this.y_coord = RenderUtils.checkSize(this.y_coord, this.currentItem);
			this.x_coord[this.currentItem] = 0;
			this.y_coord[this.currentItem] = 0;

			this.currentItem++;
		}
	}

	/* Method to add Shape, Text or image to main display on page over PDF - will be flushed on redraw */
	@Override
	public void drawAdditionalObjectsOverPage(int[] type, Color[] colors, Object[] obj) throws PdfException {

		if (obj == null) {
			return;
		}

		/**
		 * remember end of items from PDF page
		 */
		if (this.endItem == -1) {

			this.endItem = this.currentItem;

			this.objectType.setCheckpoint();

			this.shapeType.setCheckpoint();

			this.pageObjects.setCheckpoint();

			this.areas.setCheckpoint();

			this.clips.setCheckpoint();

			this.textFillType.setCheckpoint();

			this.text_color.setCheckpoint();

			this.fill_color.setCheckpoint();

			this.stroke_color.setCheckpoint();

			this.stroke.setCheckpoint();

			if (this.imageOptions == null) this.imageOptions = new Vector_Int(defaultSize);

			this.imageOptions.setCheckpoint();

			if (this.TRvalues == null) this.TRvalues = new Vector_Int(defaultSize);

			this.TRvalues.setCheckpoint();

			if (this.fs == null) this.fs = new Vector_Int(defaultSize);

			this.fs.setCheckpoint();

			if (this.lw == null) this.lw = new Vector_Int(defaultSize);

			this.lw.setCheckpoint();

			this.af1.setCheckpoint();

			this.af2.setCheckpoint();

			this.af3.setCheckpoint();

			this.af4.setCheckpoint();

			this.fontBounds.setCheckpoint();

			if (this.opacity != null) this.opacity.setCheckpoint();

		}

		/**
		 * cycle through items and add to display - throw exception if not valid
		 */
		int count = type.length;

		final boolean debug = false;

		int currentType;

		GraphicsState gs;

		for (int i = 0; i < count; i++) {

			currentType = type[i];

			if (debug) System.out.println(i + " " + getTypeAsString(currentType) + " " + " " + obj[i]);

			switch (currentType) {
				case DynamicVectorRenderer.FILLOPACITY:
					setGraphicsState(GraphicsState.FILL, ((Float) obj[i]).floatValue());
					break;

				case DynamicVectorRenderer.STROKEOPACITY:
					setGraphicsState(GraphicsState.STROKE, ((Float) obj[i]).floatValue());
					break;

				case DynamicVectorRenderer.STROKEDSHAPE:
					gs = new GraphicsState();
					gs.setFillType(GraphicsState.STROKE);
					gs.setStrokeColor(new PdfColor(colors[i].getRed(), colors[i].getGreen(), colors[i].getBlue()));
					drawShape((Shape) obj[i], gs, Cmd.S);

					break;

				case DynamicVectorRenderer.FILLEDSHAPE:
					gs = new GraphicsState();
					gs.setFillType(GraphicsState.FILL);
					gs.setNonstrokeColor(new PdfColor(colors[i].getRed(), colors[i].getGreen(), colors[i].getBlue()));
					drawShape((Shape) obj[i], gs, Cmd.F);

					break;

				case DynamicVectorRenderer.CUSTOM:
					drawCustom(obj[i]);

					break;

				case DynamicVectorRenderer.IMAGE:
					ImageObject imgObj = (ImageObject) obj[i];
					gs = new GraphicsState();

					gs.CTM = new float[][] { { imgObj.image.getWidth(), 0, 1 }, { 0, imgObj.image.getHeight(), 1 }, { 0, 0, 0 } };

					gs.x = imgObj.x;
					gs.y = imgObj.y;

					drawImage(this.pageNumber, imgObj.image, gs, false, "extImg" + i, PDFImageProcessing.NOTHING, -1);

					break;

				case DynamicVectorRenderer.STRING:
					TextObject textObj = (TextObject) obj[i];
					gs = new GraphicsState();
					float fontSize = textObj.font.getSize();
					double[] afValues = { fontSize, 0f, 0f, fontSize, 0f, 0f };
					drawAffine(afValues);

					drawTR(GraphicsState.FILL);
					gs.setTextRenderType(GraphicsState.FILL);
					gs.setNonstrokeColor(new PdfColor(colors[i].getRed(), colors[i].getGreen(), colors[i].getBlue()));
					drawText(null, textObj.text, gs, textObj.x, -textObj.y, textObj.font); // note y is negative

					break;

				case 0:
					break;

				default:
					throw new PdfException("Unrecognised type " + currentType);
			}
		}
	}

	private static String getTypeAsString(int i) {

		String str = "Value Not set";

		switch (i) {
			case DynamicVectorRenderer.FILLOPACITY:
				str = "FILLOPACITY";
				break;

			case DynamicVectorRenderer.STROKEOPACITY:
				str = "STROKEOPACITY";
				break;

			case DynamicVectorRenderer.STROKEDSHAPE:
				str = "STROKEDSHAPE";
				break;

			case DynamicVectorRenderer.FILLEDSHAPE:
				str = "FILLEDSHAPE";
				break;

			case DynamicVectorRenderer.CUSTOM:
				str = "CUSTOM";
				break;

			case DynamicVectorRenderer.IMAGE:
				str = "IMAGE";
				break;

			case DynamicVectorRenderer.STRING:
				str = "String";
				break;

		}

		return str;
	}

	@Override
	public void flushAdditionalObjOnPage() {
		// reset and remove all from page

		// reset pointer
		if (this.endItem != -1) this.currentItem = this.endItem;

		this.endItem = -1;

		this.objectType.resetToCheckpoint();

		this.shapeType.resetToCheckpoint();

		this.pageObjects.resetToCheckpoint();

		this.areas.resetToCheckpoint();

		this.clips.resetToCheckpoint();

		this.textFillType.resetToCheckpoint();

		this.text_color.resetToCheckpoint();

		this.fill_color.resetToCheckpoint();

		this.stroke_color.resetToCheckpoint();

		this.stroke.resetToCheckpoint();

		if (this.imageOptions != null) this.imageOptions.resetToCheckpoint();

		if (this.TRvalues != null) this.TRvalues.resetToCheckpoint();

		if (this.fs != null) this.fs.resetToCheckpoint();

		if (this.lw != null) this.lw.resetToCheckpoint();

		this.af1.resetToCheckpoint();

		this.af2.resetToCheckpoint();

		this.af3.resetToCheckpoint();

		this.af4.resetToCheckpoint();

		this.fontBounds.resetToCheckpoint();

		if (this.opacity != null) this.opacity.resetToCheckpoint();

		// reset pointers we use to flag color change
		this.lastFillTextCol = 0;
		this.lastFillCol = 0;
		this.lastStrokeCol = 0;

		this.lastClip = null;
		this.hasClips = false;

		this.lastStroke = null;

		this.lastAf = new double[4];

		this.fillSet = false;
		this.strokeSet = false;
	}

	/* save shape colour */
	@Override
	public void drawStrokeColor(Paint currentCol) {

		this.pageObjects.addElement(null);
		this.objectType.addElement(DynamicVectorRenderer.STROKECOLOR);
		this.areas.addElement(null);

		// stroke_color.addElement(new Color (currentCol.getRed(),currentCol.getGreen(),currentCol.getBlue()));
		this.stroke_color.addElement(currentCol);

		this.x_coord = RenderUtils.checkSize(this.x_coord, this.currentItem);
		this.y_coord = RenderUtils.checkSize(this.y_coord, this.currentItem);
		this.x_coord[this.currentItem] = 0;
		this.y_coord[this.currentItem] = 0;

		this.currentItem++;

		this.strokeSet = false;
		this.fillSet = false;
		this.resetTextColors = true;
	}

	/* save custom shape */
	@Override
	public void drawCustom(Object value) {

		this.pageObjects.addElement(value);
		this.objectType.addElement(DynamicVectorRenderer.CUSTOM);
		this.areas.addElement(null);

		this.x_coord = RenderUtils.checkSize(this.x_coord, this.currentItem);
		this.y_coord = RenderUtils.checkSize(this.y_coord, this.currentItem);
		this.x_coord[this.currentItem] = 0;
		this.y_coord[this.currentItem] = 0;

		this.currentItem++;
	}

	/* save shape stroke */
	@Override
	public void drawTR(int value) {

		if (value != this.lastTR) { // only cache if needed

			if (this.TRvalues == null) {
				this.TRvalues = new Vector_Int(defaultSize);
				this.TRvalues.setCheckpoint();
			}

			this.lastTR = value;

			this.pageObjects.addElement(null);
			this.objectType.addElement(DynamicVectorRenderer.TR);
			this.areas.addElement(null);

			this.TRvalues.addElement(value);

			this.x_coord = RenderUtils.checkSize(this.x_coord, this.currentItem);
			this.y_coord = RenderUtils.checkSize(this.y_coord, this.currentItem);
			this.x_coord[this.currentItem] = 0;
			this.y_coord[this.currentItem] = 0;

			this.currentItem++;
		}
	}

	/* save shape stroke */
	@Override
	public void drawStroke(Stroke current) {

		this.pageObjects.addElement(null);
		this.objectType.addElement(DynamicVectorRenderer.STROKE);
		this.areas.addElement(null);

		this.stroke.addElement((current));

		this.x_coord = RenderUtils.checkSize(this.x_coord, this.currentItem);
		this.y_coord = RenderUtils.checkSize(this.y_coord, this.currentItem);
		this.x_coord[this.currentItem] = 0;
		this.y_coord[this.currentItem] = 0;

		this.currentItem++;
	}

	/* save clip in array to draw */
	@Override
	public void drawClip(GraphicsState currentGraphicsState, Shape defaultClip, boolean canBeCached) {

		boolean resetClip = false;

		Area clip = currentGraphicsState.getClippingShape();

		if (canBeCached && this.hasClips && this.lastClip == null && clip == null) {

		}
		else
			if (!canBeCached || this.lastClip == null || clip == null) {

				resetClip = true;
			}
			else {

				Rectangle bounds = clip.getBounds();
				Rectangle oldBounds = this.lastClip.getBounds();

				// see if different size
				if (bounds.x != oldBounds.x || bounds.y != oldBounds.y || bounds.width != oldBounds.width || bounds.height != oldBounds.height) {
					resetClip = true;
				}
				else { // if both rectangle and same size skip

					int count = isRectangle(bounds);
					int count2 = isRectangle(oldBounds);

					if (count == 6 && count2 == 6) {

					}
					else
						if (!clip.equals(this.lastClip)) { // only do slow test at this point
							resetClip = true;
						}
				}
			}

		if (resetClip) {

			this.pageObjects.addElement(null);
			this.objectType.addElement(DynamicVectorRenderer.CLIP);
			this.areas.addElement(null);

			this.lastClip = clip;

			if (clip == null) {
				this.clips.addElement(null);
			}
			else {
				this.clips.addElement((Area) clip.clone());
			}

			this.x_coord = RenderUtils.checkSize(this.x_coord, this.currentItem);
			this.y_coord = RenderUtils.checkSize(this.y_coord, this.currentItem);
			this.x_coord[this.currentItem] = currentGraphicsState.x;
			this.y_coord[this.currentItem] = currentGraphicsState.y;

			this.currentItem++;

			this.hasClips = true;
		}
	}

	/**
	 * store glyph info
	 */
	@Override
	public void drawEmbeddedText(float[][] Trm, int fontSize, PdfGlyph embeddedGlyph, Object javaGlyph, int type, GraphicsState gs,
			AffineTransform at, String glyf, PdfFont currentFontData, float glyfWidth) {

		/**
		 * set color first
		 */
		PdfPaint currentCol;

		int text_fill_type = gs.getTextRenderType();

		// for a fill
		if ((text_fill_type & GraphicsState.FILL) == GraphicsState.FILL) {
			currentCol = gs.getNonstrokeColor();

			if (currentCol.isPattern()) {
				drawColor(currentCol, GraphicsState.FILL);
				this.resetTextColors = true;
			}
			else {

				int newCol = (currentCol).getRGB();
				if ((this.resetTextColors) || ((this.lastFillTextCol != newCol))) {
					this.lastFillTextCol = newCol;
					drawColor(currentCol, GraphicsState.FILL);
					this.resetTextColors = false;
				}
			}
		}

		// and/or do a stroke
		if ((text_fill_type & GraphicsState.STROKE) == GraphicsState.STROKE) {
			currentCol = gs.getStrokeColor();

			if (currentCol.isPattern()) {
				drawColor(currentCol, GraphicsState.STROKE);
				this.resetTextColors = true;
			}
			else {
				int newCol = currentCol.getRGB();
				if ((this.resetTextColors) || (this.lastStrokeCol != newCol)) {
					this.resetTextColors = false;
					this.lastStrokeCol = newCol;
					drawColor(currentCol, GraphicsState.STROKE);
				}
			}
		}

		// allow for lines as shadows
		setLineWidth((int) gs.getLineWidth());

		drawFontSize(fontSize);

		if (javaGlyph != null) {

			if (Trm != null) {
				double[] nextAf = new double[] { Trm[0][0], Trm[0][1], Trm[1][0], Trm[1][1], Trm[2][0], Trm[2][1] };

				if ((this.lastAf[0] == nextAf[0]) && (this.lastAf[1] == nextAf[1]) && (this.lastAf[2] == nextAf[2]) && (this.lastAf[3] == nextAf[3])) {}
				else {

					this.drawAffine(nextAf);
					this.lastAf[0] = nextAf[0];
					this.lastAf[1] = nextAf[1];
					this.lastAf[2] = nextAf[2];
					this.lastAf[3] = nextAf[3];
				}
			}

			if (!(javaGlyph instanceof Area)) type = -type;

		}
		else {

			double[] nextAf = new double[6];
			at.getMatrix(nextAf);
			if ((this.lastAf[0] == nextAf[0]) && (this.lastAf[1] == nextAf[1]) && (this.lastAf[2] == nextAf[2]) && (this.lastAf[3] == nextAf[3])) {}
			else {
				this.drawAffine(nextAf);
				this.lastAf[0] = nextAf[0];
				this.lastAf[1] = nextAf[1];
				this.lastAf[2] = nextAf[2];
				this.lastAf[3] = nextAf[3];
			}
		}

		if (embeddedGlyph == null) this.pageObjects.addElement(javaGlyph);
		else this.pageObjects.addElement(embeddedGlyph);

		this.objectType.addElement(type);

		if (type < 0) {
			this.areas.addElement(null);
		}
		else {
			if (javaGlyph != null) {

				this.areas.addElement(new Rectangle((int) Trm[2][0], (int) Trm[2][1], fontSize, fontSize));
				checkWidth(new Rectangle((int) Trm[2][0], (int) Trm[2][1], fontSize, fontSize));

			}
			else {
				/** now text */
				int realSize = fontSize;
				if (realSize < 0) realSize = -realSize;
				Rectangle area = new Rectangle((int) Trm[2][0], (int) Trm[2][1], realSize, realSize);

				this.areas.addElement(area);
				checkWidth(area);
			}
		}

		this.x_coord = RenderUtils.checkSize(this.x_coord, this.currentItem);
		this.y_coord = RenderUtils.checkSize(this.y_coord, this.currentItem);
		this.x_coord[this.currentItem] = Trm[2][0];
		this.y_coord[this.currentItem] = Trm[2][1];

		this.currentItem++;
	}

	/**
	 * store fontBounds info
	 */
	@Override
	public void drawFontBounds(Rectangle newfontBB) {

		this.pageObjects.addElement(null);
		this.objectType.addElement(DynamicVectorRenderer.fontBB);
		this.areas.addElement(null);

		this.fontBounds.addElement(newfontBB);

		this.x_coord = RenderUtils.checkSize(this.x_coord, this.currentItem);
		this.y_coord = RenderUtils.checkSize(this.y_coord, this.currentItem);
		this.x_coord[this.currentItem] = 0;
		this.y_coord[this.currentItem] = 0;

		this.currentItem++;
	}

	/**
	 * store af info
	 */
	@Override
	public void drawAffine(double[] afValues) {

		this.pageObjects.addElement(null);
		this.objectType.addElement(DynamicVectorRenderer.AF);
		this.areas.addElement(null);

		this.af1.addElement(afValues[0]);
		this.af2.addElement(afValues[1]);
		this.af3.addElement(afValues[2]);
		this.af4.addElement(afValues[3]);

		this.x_coord = RenderUtils.checkSize(this.x_coord, this.currentItem);
		this.y_coord = RenderUtils.checkSize(this.y_coord, this.currentItem);
		this.x_coord[this.currentItem] = (float) afValues[4];
		this.y_coord[this.currentItem] = (float) afValues[5];

		this.currentItem++;
	}

	/**
	 * store af info
	 */
	@Override
	public void drawFontSize(int fontSize) {

		int realSize = fontSize;
		if (realSize < 0) realSize = -realSize;

		if (realSize != this.lastFS) {
			this.pageObjects.addElement(null);
			this.objectType.addElement(DynamicVectorRenderer.FONTSIZE);
			this.areas.addElement(null);

			if (this.fs == null) {
				this.fs = new Vector_Int(defaultSize);
				this.fs.setCheckpoint();
			}

			this.fs.addElement(fontSize);

			this.x_coord = RenderUtils.checkSize(this.x_coord, this.currentItem);
			this.y_coord = RenderUtils.checkSize(this.y_coord, this.currentItem);
			this.x_coord[this.currentItem] = 0;
			this.y_coord[this.currentItem] = 0;

			this.currentItem++;

			this.lastFS = realSize;

		}
	}

	/**
	 * store line width info
	 */
	@Override
	public void setLineWidth(int lineWidth) {

		if (lineWidth != this.lastLW) {

			this.areas.addElement(null);
			this.pageObjects.addElement(null);
			this.objectType.addElement(DynamicVectorRenderer.LINEWIDTH);

			if (this.lw == null) {
				this.lw = new Vector_Int(defaultSize);
				this.lw.setCheckpoint();
			}

			this.lw.addElement(lineWidth);

			this.x_coord = RenderUtils.checkSize(this.x_coord, this.currentItem);
			this.y_coord = RenderUtils.checkSize(this.y_coord, this.currentItem);
			this.x_coord[this.currentItem] = 0;
			this.y_coord[this.currentItem] = 0;

			this.currentItem++;

			this.lastLW = lineWidth;

		}
	}

	/**
	 * rebuild serialised version
	 * 
	 * NOT PART OF API and subject to change (DO NOT USE)
	 * 
	 * @param fonts
	 * 
	 */
	public SwingDisplay(byte[] stream, Map fonts) {

		// we use Cannoo to turn our stream back into a DynamicVectorRenderer
		try {
			this.fonts = fonts;

			ByteArrayInputStream bis = new ByteArrayInputStream(stream);

			// read version and throw error is not correct version
			int version = bis.read();
			if (version != 1) throw new PdfException("Unknown version in serialised object " + version);

			int isHires = bis.read(); // 0=no,1=yes
			this.useHiResImageForDisplay = isHires == 1;

			this.pageNumber = bis.read();

			this.x_coord = (float[]) RenderUtils.restoreFromStream(bis);
			this.y_coord = (float[]) RenderUtils.restoreFromStream(bis);

			// read in arrays - opposite of serializeToByteArray();
			// we may need to throw an exception to allow for errors

			this.text_color = (Vector_Object) RenderUtils.restoreFromStream(bis);

			this.textFillType = (Vector_Int) RenderUtils.restoreFromStream(bis);

			this.stroke_color = new Vector_Object();
			this.stroke_color.restoreFromStream(bis);

			this.fill_color = new Vector_Object();
			this.fill_color.restoreFromStream(bis);

			this.stroke = new Vector_Object();
			this.stroke.restoreFromStream(bis);

			this.pageObjects = new Vector_Object();
			this.pageObjects.restoreFromStream(bis);

			this.javaObjects = (Vector_Object) RenderUtils.restoreFromStream(bis);

			this.shapeType = (Vector_Int) RenderUtils.restoreFromStream(bis);

			this.af1 = (Vector_Double) RenderUtils.restoreFromStream(bis);

			this.af2 = (Vector_Double) RenderUtils.restoreFromStream(bis);

			this.af3 = (Vector_Double) RenderUtils.restoreFromStream(bis);

			this.af4 = (Vector_Double) RenderUtils.restoreFromStream(bis);

			this.fontBounds = new Vector_Rectangle();
			this.fontBounds.restoreFromStream(bis);

			this.clips = new Vector_Shape();
			this.clips.restoreFromStream(bis);

			this.objectType = (Vector_Int) RenderUtils.restoreFromStream(bis);

			this.opacity = (Vector_Float) RenderUtils.restoreFromStream(bis);

			this.imageOptions = (Vector_Int) RenderUtils.restoreFromStream(bis);

			this.TRvalues = (Vector_Int) RenderUtils.restoreFromStream(bis);

			this.fs = (Vector_Int) RenderUtils.restoreFromStream(bis);
			this.lw = (Vector_Int) RenderUtils.restoreFromStream(bis);

			int fontCount = (Integer) RenderUtils.restoreFromStream(bis);
			for (int ii = 0; ii < fontCount; ii++) {

				Object key = RenderUtils.restoreFromStream(bis);
				Object glyphs = RenderUtils.restoreFromStream(bis);
				fonts.put(key, glyphs);
			}

			int alteredFontCount = (Integer) RenderUtils.restoreFromStream(bis);
			for (int ii = 0; ii < alteredFontCount; ii++) {

				Object key = RenderUtils.restoreFromStream(bis);

				PdfJavaGlyphs updatedFont = (PdfJavaGlyphs) fonts.get(key);

				updatedFont.setDisplayValues((Map) RenderUtils.restoreFromStream(bis));
				updatedFont.setCharGlyphs((Map) RenderUtils.restoreFromStream(bis));
				updatedFont.setEmbeddedEncs((Map) RenderUtils.restoreFromStream(bis));

			}

			bis.close();

		}
		catch (Exception e) {
			// tell user and log
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
		}

		// used in loop to draw so needs to be set
		this.currentItem = this.pageObjects.get().length;
	}

	/**
	 * stop screen bein cleared on repaint - used by Canoo code
	 * 
	 * NOT PART OF API and subject to change (DO NOT USE)
	 **/
	@Override
	public void stopClearOnNextRepaint(boolean flag) {
		this.noRepaint = flag;
	}

	/**
	 * turn object into byte[] so we can move across this way should be much faster than the stadard Java serialise.
	 * 
	 * NOT PART OF API and subject to change (DO NOT USE)
	 * 
	 * @throws IOException
	 */
	@Override
	public byte[] serializeToByteArray(Set fontsAlreadyOnClient) throws IOException {

		ByteArrayOutputStream bos = new ByteArrayOutputStream();

		// add a version so we can flag later changes
		bos.write(1);

		// flag hires
		// 0=no,1=yes
		if (this.useHiResImageForDisplay) bos.write(1);
		else bos.write(0);

		// save page
		bos.write(this.pageNumber);

		// the WeakHashMaps are local caches - we ignore

		// we do not copy across hires images

		// we need to copy these in order

		// if we write a count for each we can read the count back and know how many objects
		// to read back

		// write these values first
		// pageNumber;
		// objectStoreRef;
		// isPrinting;

		this.text_color.trim();
		this.stroke_color.trim();
		this.fill_color.trim();
		this.stroke.trim();
		this.pageObjects.trim();
		this.javaObjects.trim();
		this.stroke.trim();
		this.pageObjects.trim();
		this.javaObjects.trim();
		this.shapeType.trim();
		this.af1.trim();
		this.af2.trim();
		this.af3.trim();
		this.af4.trim();

		this.fontBounds.trim();

		this.clips.trim();
		this.objectType.trim();
		if (this.opacity != null) this.opacity.trim();
		if (this.imageOptions != null) this.imageOptions.trim();
		if (this.TRvalues != null) this.TRvalues.trim();

		if (this.fs != null) this.fs.trim();

		if (this.lw != null) this.lw.trim();

		RenderUtils.writeToStream(bos, this.x_coord, "x_coord");
		RenderUtils.writeToStream(bos, this.y_coord, "y_coord");
		RenderUtils.writeToStream(bos, this.text_color, "text_color");
		RenderUtils.writeToStream(bos, this.textFillType, "textFillType");
		this.stroke_color.writeToStream(bos);
		this.fill_color.writeToStream(bos);

		this.stroke.writeToStream(bos);

		this.pageObjects.writeToStream(bos);

		RenderUtils.writeToStream(bos, this.javaObjects, "javaObjects");
		RenderUtils.writeToStream(bos, this.shapeType, "shapeType");

		RenderUtils.writeToStream(bos, this.af1, "af1");
		RenderUtils.writeToStream(bos, this.af2, "af2");
		RenderUtils.writeToStream(bos, this.af3, "af3");
		RenderUtils.writeToStream(bos, this.af4, "af4");

		this.fontBounds.writeToStream(bos);

		this.clips.writeToStream(bos);

		RenderUtils.writeToStream(bos, this.objectType, "objectType");
		RenderUtils.writeToStream(bos, this.opacity, "opacity");
		RenderUtils.writeToStream(bos, this.imageOptions, "imageOptions");
		RenderUtils.writeToStream(bos, this.TRvalues, "TRvalues");

		RenderUtils.writeToStream(bos, this.fs, "fs");
		RenderUtils.writeToStream(bos, this.lw, "lw");

		int fontCount = 0, updateCount = 0;
		Map fontsAlreadySent = new HashMap(10);
		Map newFontsToSend = new HashMap(10);

		for (Object fontUsed : this.fontsUsed.keySet()) {
			if (!fontsAlreadyOnClient.contains(fontUsed)) {
				fontCount++;
				newFontsToSend.put(fontUsed, "x");
			}
			else {
				updateCount++;
				fontsAlreadySent.put(fontUsed, "x");
			}
		}

		/**
		 * new fonts
		 */
		RenderUtils.writeToStream(bos, fontCount, "new Integer(fontCount)");

		Iterator keys = newFontsToSend.keySet().iterator();
		while (keys.hasNext()) {
			Object key = keys.next();

			RenderUtils.writeToStream(bos, key, "key");
			RenderUtils.writeToStream(bos, this.fonts.get(key), "font");

			fontsAlreadyOnClient.add(key);
		}

		/**
		 * new data on existing fonts
		 */
		/**
		 * new fonts
		 */
		RenderUtils.writeToStream(bos, updateCount, "new Integer(existingfontCount)");

		keys = fontsAlreadySent.keySet().iterator();
		while (keys.hasNext()) {
			Object key = keys.next();

			RenderUtils.writeToStream(bos, key, "key");
			PdfJavaGlyphs aa = (PdfJavaGlyphs) this.fonts.get(key);
			RenderUtils.writeToStream(bos, aa.getDisplayValues(), "display");
			RenderUtils.writeToStream(bos, aa.getCharGlyphs(), "char");
			RenderUtils.writeToStream(bos, aa.getEmbeddedEncs(), "emb");

		}

		bos.close();

		this.fontsUsed.clear();

		return bos.toByteArray();
	}

	@Override
	public void setneedsVerticalInvert(boolean b) {
		this.needsVerticalInvert = b;
	}

	@Override
	public void setneedsHorizontalInvert(boolean b) {
		this.needsHorizontalInvert = b;
	}

	/**
	 * for font if we are generatign glyph on first render
	 */
	@Override
	public void checkFontSaved(Object glyph, String name, PdfFont currentFontData) {

		// save glyph at start
		/** now text */
		this.pageObjects.addElement(glyph);
		this.objectType.addElement(DynamicVectorRenderer.MARKER);
		this.areas.addElement(null);
		this.currentItem++;

		if (this.fontsUsed.get(name) == null || currentFontData.isFontSubsetted()) {
			this.fonts.put(name, currentFontData.getGlyphData());
			this.fontsUsed.put(name, "x");
		}
	}

	@Override
	public Rectangle getArea(int i) {
		return this.areas.elementAt(i);
	}

	/**
	 * @return number of image in display queue or -1 if none
	 */
	@Override
	public int isInsideImage(int x, int y) {
		int outLine = -1;

		Rectangle[] areas = this.areas.get();
		Rectangle possArea = null;
		int count = areas.length;

		int[] types = this.objectType.get();
		for (int i = 0; i < count; i++) {
			if (areas[i] != null) {

				if (RenderUtils.rectangleContains(areas[i], x, y, i) && types[i] == DynamicVectorRenderer.IMAGE) {
					// Check for smallest image that contains this point
					if (possArea != null) {
						int area1 = possArea.height * possArea.width;
						int area2 = areas[i].height * areas[i].width;
						if (area2 < area1) possArea = areas[i];
						outLine = i;
					}
					else {
						possArea = areas[i];
						outLine = i;
					}
				}
			}
		}
		return outLine;
	}

	@Override
	public void saveImage(int id, String des, String type) {

		String name = (String) this.imageIDtoName.get(id);
		BufferedImage image = null;
		if (this.useHiResImageForDisplay) {
			image = this.objectStoreRef.loadStoredImage(name);

			// if not stored, try in memory
			if (image == null) image = (BufferedImage) this.pageObjects.elementAt(id);
		}
		else image = (BufferedImage) this.pageObjects.elementAt(id);

		if (image != null) {

			if (!this.optimisedTurnCode) image = RenderUtils.invertImage(null, image);

			if (image.getType() == BufferedImage.TYPE_CUSTOM || (type.equals("jpg") && image.getType() == BufferedImage.TYPE_INT_ARGB)) {
				image = ColorSpaceConvertor.convertToRGB(image);
				if (image.getType() == BufferedImage.TYPE_CUSTOM && PdfDecoder.showErrorMessages) JOptionPane
						.showMessageDialog(
								null,
								"This is a custom Image, Java's standard libraries may not be able to save the image as a jpg correctly.\n"
										+ "Enabling JAI will ensure correct output. \n\nFor information on how to do this please go to http://www.jpedal.org/flags.php");
			}

			if (this.needsHorizontalInvert) {
				image = RenderUtils.invertImageBeforeSave(image, true);
			}

			if (this.needsVerticalInvert) {
				image = RenderUtils.invertImageBeforeSave(image, false);
			}

			if (JAIHelper.isJAIused() && type.toLowerCase().startsWith("tif")) {
				javax.media.jai.JAI.create("filestore", image, des, type);
			}
			else
				if (type.toLowerCase().startsWith("tif")) {
					if (PdfDecoder.showErrorMessages) JOptionPane.showMessageDialog(null, "Please setup JAI library for Tiffs");

					if (LogWriter.isOutput()) LogWriter.writeLog("Please setup JAI library for Tiffs");
				}
				else {
					try {
						BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(new File(des)));
						ImageIO.write(image, type, bos);
						bos.flush();
						bos.close();
					}
					catch (IOException e) {
						// tell user and log
						if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
					}
				}
		}
	}

	/**
	 * operations to do once page done
	 */
	@Override
	public void flagDecodingFinished() {

		this.highlightsNeedToBeGenerated = true;
	}

	private void generateHighlights(Graphics2D g2, int count, int[] objectTypes, Object[] pageObjects, float a, float b, float c, float d,
			double[] afValues1, double[] afValues2, double[] afValues3, double[] afValues4, int[] fsValues, Rectangle[] fontBounds) {

		// flag done for page
		this.highlightsNeedToBeGenerated = false;

		// array for text highlights
		int[] highlightIDs = new int[count];

		int fsCount = -1, fontBBCount = 0;// note af is 1 behind!

		float x, y;

		Rectangle currentHighlight = null;

		float[] top = new float[count];
		float[] bottom = new float[count];
		float[] left = new float[count];
		float[] right = new float[count];
		boolean[] isFontEmbedded = new boolean[count];
		int[] fontSizes = new int[count];
		float[] w = new float[count];

		this.textHighlightsX = new int[count];
		int[] textHighlightsY = new int[count];
		this.textHighlightsWidth = new int[count];
		this.textHighlightsHeight = new int[count];

		/**
		 * get highlights
		 */
		// fontBoundsX=0,
		int fontBoundsY = 0, fontBoundsH = 1000, fontBoundsW = 1000, fontSize = 1, realSize = 1;

		double matrix[] = new double[6];
		g2.getTransform().getMatrix(matrix);

		// see if rotated
		int pageRotation = 0;
		if (matrix[1] < 0 && matrix[2] < 0) pageRotation = 270;

		for (int i = 0; i < count; i++) {

			this.type = objectTypes[i];

			if (this.type > 0) {

				x = this.x_coord[i];
				y = this.y_coord[i];

				// put in displacement if text moved up by inversion
				if (realSize < 0) x = x + realSize;

				Object currentObject = pageObjects[i];

				if (this.type == DynamicVectorRenderer.fontBB) {

					currentHighlight = fontBounds[fontBBCount];

					fontBoundsH = currentHighlight.height;
					// fontBoundsX=currentHighlight.x;
					fontBoundsY = currentHighlight.y;
					fontBoundsW = currentHighlight.width;
					fontBBCount++;

				}
				else
					if (this.type == DynamicVectorRenderer.FONTSIZE) {
						fsCount++;
						realSize = fsValues[fsCount];
						if (realSize < 0) fontSize = -realSize;
						else fontSize = realSize;

					}
					else
						if (this.type == DynamicVectorRenderer.TRUETYPE || this.type == DynamicVectorRenderer.TYPE1C
								|| this.type == DynamicVectorRenderer.TEXT) {

							// this works in 2 different unit spaces for embedded and non-embedded hence flags
							float scaling = 1f;

							if (this.type == DynamicVectorRenderer.TRUETYPE || this.type == DynamicVectorRenderer.TYPE1C) {
								PdfGlyph raw = ((PdfGlyph) currentObject);

								scaling = fontSize / 1000f;

								this.textHighlightsX[i] = raw.getFontBB(PdfGlyph.FontBB_X);
								textHighlightsY[i] = fontBoundsY;
								this.textHighlightsWidth[i] = raw.getFontBB(PdfGlyph.FontBB_WIDTH);
								this.textHighlightsHeight[i] = fontBoundsH;

								isFontEmbedded[i] = true;

								if (pageRotation == 90) {
									bottom[i] = -((textHighlightsY[i] * scaling)) + x;
									left[i] = (this.textHighlightsX[i] * scaling) + y;
								}
								else
									if (pageRotation == 270) {
										bottom[i] = ((textHighlightsY[i] * scaling)) + x;
										left[i] = -((this.textHighlightsX[i] * scaling) + y);
									}
									else { // 0 and 180 work the same way
										bottom[i] = ((textHighlightsY[i] * scaling)) + y;
										left[i] = ((this.textHighlightsX[i] * scaling)) + x;
									}

								top[i] = bottom[i] + (this.textHighlightsHeight[i] * scaling);
								right[i] = left[i] + (this.textHighlightsWidth[i] * scaling);

								w[i] = 10; // any non zero number
								fontSizes[i] = fontSize;

							}
							else {
								scaling = 1f;

								float scale = 1000f / fontSize;
								this.textHighlightsX[i] = (int) x;
								textHighlightsY[i] = (int) (y + (fontBoundsY / scale));
								this.textHighlightsWidth[i] = (int) ((fontBoundsW) / scale);
								this.textHighlightsHeight[i] = (int) ((fontBoundsH - fontBoundsY) / scale);

								if (pageRotation == 90) {
									bottom[i] = -textHighlightsY[i];
									left[i] = this.textHighlightsX[i];
								}
								else
									if (pageRotation == 270) {
										bottom[i] = (textHighlightsY[i]);
										left[i] = -this.textHighlightsX[i];
									}
									else { // 0 and 180 work the same way
										bottom[i] = textHighlightsY[i];
										left[i] = this.textHighlightsX[i];
									}

								top[i] = bottom[i] + this.textHighlightsHeight[i];
								right[i] = left[i] + this.textHighlightsWidth[i];

								w[i] = ((Area) currentObject).getBounds().width;

								fontSizes[i] = fontSize;

							}
							highlightIDs[i] = i;

						}
			}
		}

		// sort highlights
		// highlightIDs=Sorts.quicksort(left,bottom,highlightIDs);

		int zz = -31;
		// scan each and adjust so it touches next
		// if(1==2)
		for (int aa = 0; aa < count - 1; aa++) {

			int ptr = highlightIDs[aa];

			{// if(textHighlights[ptr]!=null){

				if (ptr == zz) System.out.println("*" + ptr + " = " + " left=" + left[ptr] + " bottom=" + bottom[ptr] + " right=" + right[ptr]
						+ " top=" + top[ptr]);

				int gap = 0;
				for (int next = aa + 1; next < count; next++) {
					int nextPtr = highlightIDs[next];

					// skip empty
					if (isFontEmbedded[nextPtr] != isFontEmbedded[ptr] || w[nextPtr] < 1) continue;

					if (ptr == zz) System.out.println("compare with=" + nextPtr + " left=" + left[nextPtr] + " right=" + right[nextPtr] + ' '
							+ (left[nextPtr] > left[ptr] && left[nextPtr] < right[ptr]));

					// find glyph on right
					if ((left[nextPtr] > left[ptr] && left[nextPtr] < right[ptr])
							|| (left[nextPtr] > ((left[ptr] + right[ptr]) / 2) && right[ptr] < right[nextPtr])) {

						int currentW = this.textHighlightsWidth[ptr];
						// int currentH=textHighlightsHeight[ptr];
						int currentX = this.textHighlightsX[ptr];
						// int currentY=textHighlightsY[ptr];

						if (isFontEmbedded[nextPtr]) {
							float diff = left[nextPtr] - right[ptr];

							if (diff > 0) diff = diff + .5f;
							else diff = diff + .5f;

							gap = (int) (((diff * 1000f / fontSizes[ptr])));

							if (this.textHighlightsX[nextPtr] > 0) gap = gap + this.textHighlightsX[nextPtr];

						}
						else gap = (int) (left[nextPtr] - right[ptr]);

						if (ptr == zz) System.out.println((left[nextPtr] - right[ptr]) + " gap=" + gap + ' '
								+ (((left[nextPtr] - right[ptr]) * 1000f / fontSizes[ptr])) + " currentX=" + currentX + " scaling=" + this.scaling
								+ ' ' + fontBoundsW);

						boolean isCorrectLocation = (gap > 0 || (gap < 0 && left[ptr] < left[nextPtr] && right[ptr] > left[nextPtr]
								&& right[ptr] < right[nextPtr] && left[ptr] < right[ptr] && ((-gap < fontSizes[ptr] && !isFontEmbedded[ptr]) || (-gap < fontBoundsW && isFontEmbedded[ptr]))));
						if (bottom[ptr] < top[nextPtr] && bottom[nextPtr] < top[ptr] && (gap > 0 || isCorrectLocation)) {
							if (isCorrectLocation
									&& ((!isFontEmbedded[ptr] && gap < fontSizes[ptr] && currentW + gap < fontSizes[ptr]) || (isFontEmbedded[ptr] && gap < fontBoundsW))) {

								if (ptr == zz) System.out.println(nextPtr + " = " + " left=" + left[nextPtr] + " bottom=" + bottom[nextPtr]
										+ " right=" + right[nextPtr] + " top=" + top[nextPtr]);

								if (isFontEmbedded[ptr]) {

									if (gap > 0) {
										this.textHighlightsWidth[ptr] = currentW + gap;
										// textHighlightsX[nextPtr]=textHighlightsX[nextPtr]-half-1;
									}
									else {
										this.textHighlightsWidth[ptr] = currentW - gap;
									}

								}
								else
									if (gap > 0) this.textHighlightsWidth[ptr] = gap;
									else this.textHighlightsWidth[ptr] = currentW + gap;

								if (ptr == zz) {
									System.out.println("new=" + this.textHighlightsWidth[ptr]);
								}

								next = count;
							}
							else
								if (gap > fontBoundsW) { // off line so exit
									// next=count;
								}
						}
					}
				}
			}
		}
	}

	@Override
	public void setPrintPage(int currentPrintPage) {
		this.pageNumber = currentPrintPage;
	}

	/**
	 * @return number of image in display queue or -1 if none
	 */
	@Override
	public int getObjectUnderneath(int x, int y) {
		int typeFound = -1;
		Rectangle[] areas = this.areas.get();
		// Rectangle possArea = null;
		int count = areas.length;

		int[] types = this.objectType.get();
		boolean nothing = true;
		for (int i = count - 1; i > -1; i--) {
			if (areas[i] != null) {
				if (RenderUtils.rectangleContains(areas[i], x, y, i)) {
					if (types[i] != DynamicVectorRenderer.SHAPE && types[i] != DynamicVectorRenderer.CLIP) {
						nothing = false;
						typeFound = types[i];
						i = -1;
					}
				}
			}
		}

		if (nothing) return -1;

		return typeFound;
	}

	@Override
	public void flagImageDeleted(int i) {
		this.objectType.setElementAt(DynamicVectorRenderer.DELETED_IMAGE, i);
	}

	@Override
	public void setOCR(boolean isOCR) {
		this.hasOCR = isOCR;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SwingDisplay [ignoreHighlight=");
		builder.append(this.ignoreHighlight);
		builder.append(", noRepaint=");
		builder.append(this.noRepaint);
		builder.append(", lastItemPainted=");
		builder.append(this.lastItemPainted);
		builder.append(", optimsePainting=");
		builder.append(this.optimsePainting);
		builder.append(", needsHorizontalInvert=");
		builder.append(this.needsHorizontalInvert);
		builder.append(", needsVerticalInvert=");
		builder.append(this.needsVerticalInvert);
		builder.append(", pageX1=");
		builder.append(this.pageX1);
		builder.append(", pageX2=");
		builder.append(this.pageX2);
		builder.append(", pageY1=");
		builder.append(this.pageY1);
		builder.append(", pageY2=");
		builder.append(this.pageY2);
		builder.append(", highlightsNeedToBeGenerated=");
		builder.append(this.highlightsNeedToBeGenerated);
		builder.append(", ");
		if (this.singleImage != null) {
			builder.append("singleImage=");
			builder.append(this.singleImage);
			builder.append(", ");
		}
		builder.append("imageCount=");
		builder.append(this.imageCount);
		builder.append(", endItem=");
		builder.append(this.endItem);
		builder.append(", ");
		if (this.cachedWidths != null) {
			builder.append("cachedWidths=");
			builder.append(this.cachedWidths);
			builder.append(", ");
		}
		if (this.cachedHeights != null) {
			builder.append("cachedHeights=");
			builder.append(this.cachedHeights);
			builder.append(", ");
		}
		if (this.fonts != null) {
			builder.append("fonts=");
			builder.append(this.fonts);
			builder.append(", ");
		}
		if (this.fontsUsed != null) {
			builder.append("fontsUsed=");
			builder.append(this.fontsUsed);
			builder.append(", ");
		}
		if (this.factory != null) {
			builder.append("factory=");
			builder.append(this.factory);
			builder.append(", ");
		}
		if (this.glyphs != null) {
			builder.append("glyphs=");
			builder.append(this.glyphs);
			builder.append(", ");
		}
		if (this.imageID != null) {
			builder.append("imageID=");
			builder.append(this.imageID);
			builder.append(", ");
		}
		if (this.imageIDtoName != null) {
			builder.append("imageIDtoName=");
			builder.append(this.imageIDtoName);
			builder.append(", ");
		}
		if (this.storedImageValues != null) {
			builder.append("storedImageValues=");
			builder.append(this.storedImageValues);
			builder.append(", ");
		}
		if (this.textHighlightsX != null) {
			builder.append("textHighlightsX=");
			builder.append(Arrays.toString(this.textHighlightsX));
			builder.append(", ");
		}
		if (this.textHighlightsWidth != null) {
			builder.append("textHighlightsWidth=");
			builder.append(Arrays.toString(this.textHighlightsWidth));
			builder.append(", ");
		}
		if (this.textHighlightsHeight != null) {
			builder.append("textHighlightsHeight=");
			builder.append(Arrays.toString(this.textHighlightsHeight));
			builder.append(", ");
		}
		builder.append("stopG2setting=");
		builder.append(this.stopG2setting);
		builder.append(", ");
		if (this.x_coord != null) {
			builder.append("x_coord=");
			builder.append(Arrays.toString(this.x_coord));
			builder.append(", ");
		}
		if (this.y_coord != null) {
			builder.append("y_coord=");
			builder.append(Arrays.toString(this.y_coord));
			builder.append(", ");
		}
		if (this.largeImages != null) {
			builder.append("largeImages=");
			builder.append(this.largeImages);
			builder.append(", ");
		}
		if (this.text_color != null) {
			builder.append("text_color=");
			builder.append(this.text_color);
			builder.append(", ");
		}
		if (this.stroke_color != null) {
			builder.append("stroke_color=");
			builder.append(this.stroke_color);
			builder.append(", ");
		}
		if (this.fill_color != null) {
			builder.append("fill_color=");
			builder.append(this.fill_color);
			builder.append(", ");
		}
		if (this.stroke != null) {
			builder.append("stroke=");
			builder.append(this.stroke);
			builder.append(", ");
		}
		if (this.pageObjects != null) {
			builder.append("pageObjects=");
			builder.append(this.pageObjects);
			builder.append(", ");
		}
		if (this.shapeType != null) {
			builder.append("shapeType=");
			builder.append(this.shapeType);
			builder.append(", ");
		}
		if (this.fontBounds != null) {
			builder.append("fontBounds=");
			builder.append(this.fontBounds);
			builder.append(", ");
		}
		if (this.af1 != null) {
			builder.append("af1=");
			builder.append(this.af1);
			builder.append(", ");
		}
		if (this.af2 != null) {
			builder.append("af2=");
			builder.append(this.af2);
			builder.append(", ");
		}
		if (this.af3 != null) {
			builder.append("af3=");
			builder.append(this.af3);
			builder.append(", ");
		}
		if (this.af4 != null) {
			builder.append("af4=");
			builder.append(this.af4);
			builder.append(", ");
		}
		if (this.imageOptions != null) {
			builder.append("imageOptions=");
			builder.append(this.imageOptions);
			builder.append(", ");
		}
		if (this.TRvalues != null) {
			builder.append("TRvalues=");
			builder.append(this.TRvalues);
			builder.append(", ");
		}
		if (this.fs != null) {
			builder.append("fs=");
			builder.append(this.fs);
			builder.append(", ");
		}
		if (this.lw != null) {
			builder.append("lw=");
			builder.append(this.lw);
			builder.append(", ");
		}
		if (this.clips != null) {
			builder.append("clips=");
			builder.append(this.clips);
			builder.append(", ");
		}
		if (this.objectType != null) {
			builder.append("objectType=");
			builder.append(this.objectType);
			builder.append(", ");
		}
		if (this.javaObjects != null) {
			builder.append("javaObjects=");
			builder.append(this.javaObjects);
			builder.append(", ");
		}
		if (this.textFillType != null) {
			builder.append("textFillType=");
			builder.append(this.textFillType);
			builder.append(", ");
		}
		if (this.opacity != null) {
			builder.append("opacity=");
			builder.append(this.opacity);
			builder.append(", ");
		}
		builder.append("currentItem=");
		builder.append(this.currentItem);
		builder.append(", lastFillTextCol=");
		builder.append(this.lastFillTextCol);
		builder.append(", lastFillCol=");
		builder.append(this.lastFillCol);
		builder.append(", lastStrokeCol=");
		builder.append(this.lastStrokeCol);
		builder.append(", ");
		if (this.lastStroke != null) {
			builder.append("lastStroke=");
			builder.append(this.lastStroke);
			builder.append(", ");
		}
		if (this.lastAf != null) {
			builder.append("lastAf=");
			builder.append(Arrays.toString(this.lastAf));
			builder.append(", ");
		}
		builder.append("lastTR=");
		builder.append(this.lastTR);
		builder.append(", lastFS=");
		builder.append(this.lastFS);
		builder.append(", lastLW=");
		builder.append(this.lastLW);
		builder.append(", resetTextColors=");
		builder.append(this.resetTextColors);
		builder.append(", fillSet=");
		builder.append(this.fillSet);
		builder.append(", strokeSet=");
		builder.append(this.strokeSet);
		builder.append(", needsHighlights=");
		builder.append(this.needsHighlights);
		builder.append(", paintThreadCount=");
		builder.append(this.paintThreadCount);
		builder.append(", paintThreadID=");
		builder.append(this.paintThreadID);
		builder.append(", ");
		if (this.drawnHighlights != null) {
			builder.append("drawnHighlights=");
			builder.append(Arrays.toString(this.drawnHighlights));
			builder.append(", ");
		}
		builder.append("hasOCR=");
		builder.append(this.hasOCR);
		builder.append(", type=");
		builder.append(this.type);
		builder.append(", minX=");
		builder.append(this.minX);
		builder.append(", minY=");
		builder.append(this.minY);
		builder.append(", maxX=");
		builder.append(this.maxX);
		builder.append(", maxY=");
		builder.append(this.maxY);
		builder.append(", renderFailed=");
		builder.append(this.renderFailed);
		builder.append(", ");
		if (this.frame != null) {
			builder.append("frame=");
			builder.append(this.frame);
		}
		builder.append("]");
		return builder.toString();
	}
}