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
 * DynamicVectorRenderer.java
 * ---------------
 */
package org.jpedal.render;

import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Set;

import org.jpedal.color.PdfPaint;
import org.jpedal.exception.PdfException;
import org.jpedal.fonts.PdfFont;
import org.jpedal.fonts.glyph.PdfGlyph;
import org.jpedal.io.ObjectStore;
import org.jpedal.objects.GraphicsState;

public interface DynamicVectorRenderer {

	public final static int TEXT = 1;
	public final static int SHAPE = 2;
	public final static int IMAGE = 3;
	public final static int TRUETYPE = 4;
	public final static int TYPE1C = 5;
	public final static int TYPE3 = 6;
	public final static int CLIP = 7;
	public final static int COLOR = 8;
	public final static int AF = 9;
	public final static int TEXTCOLOR = 10;
	public final static int FILLCOLOR = 11;
	public final static int STROKECOLOR = 12;
	public final static int STROKE = 14;
	public final static int TR = 15;
	public final static int STRING = 16;
	public final static int STROKEOPACITY = 17;
	public final static int FILLOPACITY = 18;

	public final static int STROKEDSHAPE = 19;
	public final static int FILLEDSHAPE = 20;

	public final static int FONTSIZE = 21;
	public final static int LINEWIDTH = 22;

	public final static int CUSTOM = 23;

	public final static int fontBB = 24;

	public final static int XFORM = 25;

	public final static int DELETED_IMAGE = 27;
	public final static int REUSED_IMAGE = 29;

	public final static int MARKER = 200;

	/** flag to enable debugging of painting */
	public static boolean debugPaint = false;

	/**
	 * various types of DVR which we have
	 */
	public static final int DISPLAY_SCREEN = 1;//
	public static final int DISPLAY_IMAGE = 2;//
	public static final int CREATE_PATTERN = 3;
	public static final int CREATE_HTML = 4;
	public static final int CREATE_SVG = 5;
	public static final int CREATE_JAVAFX = 6;
	public static final int CREATE_EPOS =7;
    public static final int CREATE_SMASK =8;

	/**
	 * Keys for use with set value
	 */
	public final static int ALT_BACKGROUND_COLOR = 1;
	public final static int ALT_FOREGROUND_COLOR = 2;
	public final static int FOREGROUND_INCLUDE_LINEART = 3; // Alt foreground color changes lineart as well
	public final static int COLOR_REPLACEMENT_THRESHOLD = 4;

	/**
	 * used to pass in Graphics2D for all versions
	 * 
	 * @param g2
	 */
	public void setG2(Graphics2D g2);

	/**
	 * set optimised painting as true or false and also reset if true
	 * 
	 * @param optimsePainting
	 */
	public abstract void setOptimsePainting(boolean optimsePainting);

	/* remove all page objects and flush queue */
	public abstract void flush();

	/* remove all page objects and flush queue */
	public abstract void dispose();

	/**
	 * only needed for screen display
	 * 
	 * @param x
	 * @param y
	 */
	public abstract void setInset(int x, int y);

	/* renders all the objects onto the g2 surface for screen display */
	public abstract Rectangle paint(Rectangle[] highlights, AffineTransform viewScaling, Rectangle userAnnot);

	/**
	 * allow user to set component for waring message in renderer to appear - if unset no message will appear
	 * 
	 * @param frame
	 */
	public abstract void setMessageFrame(Container frame);

	public abstract void paintBackground(Shape dirtyRegion);

	/* saves text object with attributes for rendering */
	public abstract void drawText(float[][] Trm, String text, GraphicsState currentGraphicsState, float x, float y, Font javaFont);

	/** workout combined area of shapes in an area */
	// public abstract Rectangle getCombinedAreas(Rectangle targetRectangle, boolean justText);

	/* setup renderer */
	public abstract void init(int x, int y, int rawRotation, Color backgroundColor);

	/* save image in array to draw */
	public abstract int drawImage(int pageNumber, BufferedImage image, GraphicsState currentGraphicsState, boolean alreadyCached, String name,
			int optionsApplied, int previousUse);

	/**
	 * 
	 * @return which part of page drawn onto
	 */
	public abstract Rectangle getOccupiedArea();

	/* save shape in array to draw cmd is Cmd.F or Cmd.S */
	public abstract void drawShape(Shape currentShape, GraphicsState currentGraphicsState, int cmd);

	/* add XForm object */
	public abstract void drawXForm(DynamicVectorRenderer dvr, GraphicsState gs);

	/** reset on colorspace change to ensure cached data up to data */
	public abstract void resetOnColorspaceChange();

	/* save shape colour */
	public abstract void drawFillColor(PdfPaint currentCol);

	/* save opacity settings */
	public abstract void setGraphicsState(int fillType, float value);

	/* Method to add Shape, Text or image to main display on page over PDF - will be flushed on redraw */
	public abstract void drawAdditionalObjectsOverPage(int[] type, Color[] colors, Object[] obj) throws PdfException;

	public abstract void flushAdditionalObjOnPage();

	/* save shape colour */
	public abstract void drawStrokeColor(Paint currentCol);

	/* save custom shape */
	public abstract void drawCustom(Object value);

	/* save shape stroke */
	public abstract void drawTR(int value);

	/* save shape stroke */
	public abstract void drawStroke(Stroke current);

	/* save clip in array to draw */
	public abstract void drawClip(GraphicsState currentGraphicsState, Shape defaultClip, boolean alwaysApply);

	/**
	 * store glyph info
	 */
	public abstract void drawEmbeddedText(float[][] Trm, int fontSize, PdfGlyph embeddedGlyph, Object javaGlyph, int type, GraphicsState gs,
			AffineTransform at, String glyf, PdfFont currentFontData, float glyfWidth);

	/**
	 * store fontBounds info
	 */
	public abstract void drawFontBounds(Rectangle newfontBB);

	/**
	 * store af info
	 */
	public abstract void drawAffine(double[] afValues);

	/**
	 * store af info
	 */
	public abstract void drawFontSize(int fontSize);

	/**
	 * store line width info
	 */
	public abstract void setLineWidth(int lineWidth);

	/**
	 * Screen drawing using hi res images and not down-sampled images but may be slower and use more memory<br>
	 * Default setting is <b>false</b> and does nothing in OS version
	 */
	public abstract void setHiResImageForDisplayMode(boolean useHiResImageForDisplay);

	public abstract void setScalingValues(double cropX, double cropH, float scaling);

	/**
	 * stop screen bein cleared on repaint - used by Canoo code
	 * 
	 * NOT PART OF API and subject to change (DO NOT USE)
	 **/
	public abstract void stopClearOnNextRepaint(boolean flag);

	public abstract void setCustomImageHandler(org.jpedal.external.ImageHandler customImageHandler);

	public abstract void setCustomColorHandler(org.jpedal.external.ColorHandler colorController);

	/**
	 * operations to do once page done
	 */
	public abstract void flagDecodingFinished();

	// used internally - please do not use
	public abstract ObjectStore getObjectStore();

	public abstract void flagImageDeleted(int i);

	public abstract void setOCR(boolean isOCR);

	// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * turn object into byte[] so we can move across this way should be much faster than the stadard Java serialise.
	 * 
	 * NOT PART OF API and subject to change (DO NOT USE)
	 * 
	 * @throws java.io.IOException
	 */
	public abstract byte[] serializeToByteArray(Set fontsAlreadyOnClient) throws IOException;

	/**
	 * for font if we are generatign glyph on first render
	 */
	public abstract void checkFontSaved(Object glyph, String name, PdfFont currentFontData);

	public abstract boolean hasObjectsBehind(float[][] CTM);

	public abstract Rectangle getArea(int i);

	/**
	 * @return number of image in display queue or -1 if none
	 */
	public abstract int isInsideImage(int x, int y);

	public abstract void saveImage(int id, String des, String type);

	/**
	 * @return number of image in display queue or -1 if none
	 */
	public abstract int getObjectUnderneath(int x, int y);

	public void setneedsVerticalInvert(boolean b);

	public void setneedsHorizontalInvert(boolean b);

	// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * just for printing
	 */
	public abstract void stopG2HintSetting(boolean isSet);

	public abstract void setPrintPage(int currentPrintPage);

	/** used by custom versions (not JPedal) */
	public void setOutputDir(String output_dir, String filename, String pageAsString);

	public void writeCustom(int section, Object str);

	/** allow us to identify different types of renderer (ie HTML, Screen, Image) */
	public int getType();

	/** allow tracking of specific commands **/
	public void flagCommand(int commandID, int tokenNumber);

	/** allow custom values to be passed in - used for HTML */
	public void setTag(int formTag, String value);

	// generic method used by HTML to pass in values
	void setValue(int key, int i);

	void setValue(int key, String[] S);

	// generic method used by HTML for getting values
	public int getValue(int key);

	BufferedImage getSingleImagePattern();

	void setBooleanValue(int key, boolean b);

	/** used by JavaFX and HTML5 conversion to override scaling */
	boolean isScalingControlledByUser();

	public boolean avoidDownSamplingImage();

	/** allow user to read */
	public boolean getBooleanValue(int key);

	float getScaling();

	/**
	 * only used in HTML5 and SVG conversion
	 * 
	 * @param baseFontName
	 * @param s
	 * @param potentialWidth
	 */
	public void saveAdvanceWidth(String baseFontName, String s, int potentialWidth);
	
	Object getObjectValue(int id);
}
