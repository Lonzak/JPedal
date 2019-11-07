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
 * TextDecoder.java
 * ---------------
 */
package org.jpedal.parser;

import java.awt.Font;
import java.awt.Rectangle;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.jpedal.PdfDecoder;
import org.jpedal.fonts.PdfFont;
import org.jpedal.fonts.StandardFonts;
import org.jpedal.fonts.glyph.GlyphFactory;
import org.jpedal.fonts.glyph.PdfGlyph;
import org.jpedal.fonts.glyph.PdfJavaGlyphs;
import org.jpedal.io.ObjectDecoder;
import org.jpedal.io.PdfObjectReader;
import org.jpedal.objects.GraphicsState;
import org.jpedal.objects.PdfData;
import org.jpedal.objects.TextState;
import org.jpedal.objects.raw.MCObject;
import org.jpedal.objects.raw.PdfDictionary;
import org.jpedal.objects.raw.PdfObject;
import org.jpedal.render.DynamicVectorRenderer;
import org.jpedal.utils.Fonts;
import org.jpedal.utils.LogWriter;
import org.jpedal.utils.Matrix;
import org.jpedal.utils.NumberUtils;
import org.jpedal.utils.repositories.Vector_Int;
import org.jpedal.utils.repositories.Vector_Rectangle;
//<start-adobe>
//<end-adobe>

/**
 * handle conversion of the text operands
 */
public class TextDecoder extends BaseDecoder implements org.jpedal.parser.Decoder {

	public static boolean showInvisibleText = false;

	private Map lines = new HashMap(1000);

	private PdfFont currentFontData;

	private int fontSize = 0;

	private int currentRotation = 0;

	private PdfData pdfData;

	boolean markedContentExtracted = false;

	private Vector_Rectangle textAreas = new Vector_Rectangle();

	private Vector_Int textDirections = new Vector_Int();

	private TextState currentTextState;

	private boolean isPrinting = false;

	/** constant for conversion */
	private static final double radiansToDegrees = 180f / Math.PI;

	// use in rotation handling to handle minor issues
	private float unRotatedY = -1;

	/** flag to show text is being extracted */
	private boolean textExtracted = true;

	/** flag to show content is being rendered */
	private boolean renderText = false;

	/** flags to show we need colour data as well */
	private boolean textColorExtracted = false;

	// Use highlgiht y coords for text
	private boolean highlightCoords = true;

	// last Trm incase of multple Tj commands
	private boolean multipleTJs = false;

	/** flag to show type of move command being executed */
	private int moveCommand = 0; // 0=t*, 1=Tj, 2=TD

	/**
	 * flag to show some fonts might need hinting turned on to display properly
	 */
	private boolean ttHintingRequired = false;

	/** used in grouping rotated text by Storypad */
	private double rotationAsRadians = 0;

	/** length of current text fragment */
	private int textLength = 0;

	/** start of ascii escape char */
	private static final String[] hex = { "&#0;", "&#1;", "&#2;", "&#3;", "&#4;", "&#5;", "&#6;", "&#7;", "&#8;", "&#9;", "&#10;", "&#11;", "&#12;",
			"&#13;", "&#14;", "&#15;", "&#16;", "&#17;", "&#18;", "&#19;", "&#20;", "&#21;", "&#22;", "&#23;", "&#24;", "&#25;", "&#26;", "&#27;",
			"&#28;", "&#29;", "&#30;", "&#31;" };

	/** thousand as a value */
	final static private float THOUSAND = 1000;

	/** gap between characters */
	private float charSpacing = 0;

	private GlyphFactory factory = new org.jpedal.fonts.glyph.T1GlyphFactory();

	/** used by forms code to read text */
	private boolean returnText = false;

	/** used to speed-up conversion of hex strings to numbers */
	final static private int[] multiply8 = { 0, 3, 6, 9, 12, 15 };

	final static private int[] multiply16 = { 0, 4, 8, 12, 16, 20, 24, 28, 32, 36, 40 };

	private static final int NONE = 0;

	private static final int RIGHT = 1;

	/** co-ords (x1,y1 is top left corner) */
	float x1, y1, x2, y2;

	boolean isXMLExtraction = true;

	LayerDecoder layerDecoder;

	// if ActualText set store value and use if preference for text extraction
	private String actualText;

	public TextDecoder(PdfData pdfData, boolean isXMLExtraction, LayerDecoder layerDecoder) {
		this.pdfData = pdfData;

		this.isXMLExtraction = isXMLExtraction;

		this.layerDecoder = layerDecoder;
	}

	public TextDecoder(LayerDecoder layerDecoder) {
		this.layerDecoder = layerDecoder;

		this.markedContentExtracted = true;
	}

	TextDecoder() {}

	/**
	 * When highlightCoords == true Calculate the x coords for text here y coords are calculated in the method processTextArray(byte[] stream,int
	 * startCommand,int dataPointer)
	 */
	private void calcCoordinates(float x, float[][] rawTrm, boolean horizontal, float max_height, int fontSize, float y) {

		// clone data so we can manipulate
		float[][] trm = new float[3][3];
		for (int xx = 0; xx < 3; xx++) {
			System.arraycopy(rawTrm[xx], 0, trm[xx], 0, 3);
		}

		this.x1 = x;
		this.x2 = trm[2][0] - (this.charSpacing * trm[0][0]);

		if (horizontal) {
			if (trm[1][0] < 0) {
				this.x1 = x + trm[1][0] - (this.charSpacing * trm[0][0]);
				this.x2 = trm[2][0];
			}
			else
				if (trm[1][0] > 0) {
					this.x1 = x;
					this.x2 = trm[2][0];
				}
		}
		else
			if (trm[1][0] > 0) {
				this.x1 = trm[2][0];
				this.x2 = x + trm[1][0] - (this.charSpacing * trm[0][0]);
			}
			else
				if (trm[1][0] < 0) {
					this.x2 = trm[2][0];
					this.x1 = x + trm[1][0] - (this.charSpacing * trm[0][0]);
				}

		if (!this.highlightCoords) {
			/** any adjustments */
			if (horizontal) {
				// workout the height ratio
				float s_height = 1.0f;
				if (this.currentFontData.getFontType() == StandardFonts.TYPE3) s_height = (max_height / (fontSize));

				if (trm[0][1] != 0) {
					this.y1 = trm[2][1] - trm[0][1] + ((trm[0][1] + trm[1][1]) * s_height);
					this.y2 = y;

				}
				else {
					this.y1 = y + (trm[1][1] * s_height);
					this.y2 = trm[2][1];
				}
			}
			else
				if (trm[0][1] <= 0) {
					this.y2 = trm[2][1];
					this.y1 = y;
				}
				else
					if (trm[0][1] > 0) {
						this.y1 = trm[2][1];
						this.y2 = y;
					}
		}
	}

	@Override
	public void setFont(PdfFont currentFontData) {
		this.currentFontData = currentFontData;
	}

	@Override
	public void setTextState(TextState currentTextState) {

		this.currentTextState = currentTextState;
	}

	/**
	 * process each token and add to text or decode if not known command, place in array (may be operand which is later used by command)
	 */
	@Override
	public int processToken(TextState currentTextState, int commandID, int startCommand, int dataPointer) {

		this.currentTextState = currentTextState;

		switch (commandID) {

			case Cmd.BDC:
				PdfObject BDCobj = BDC(startCommand, dataPointer, this.parser.getStream(), this.parser.generateOpAsString(0, false),
						this.layerDecoder, this.gs, this.currentPdfFile, this.current, this.markedContentExtracted);

				// track setting and use in preference for text extraction
				this.actualText = BDCobj.getTextStreamValue(PdfDictionary.ActualText);

				break;

			case Cmd.BMC:
				this.layerDecoder.BMC();

				break;

			case Cmd.BT:
				currentTextState.resetTm();
				break;

			case Cmd.EMC:

				this.actualText = null;

				this.layerDecoder.EMC(this.current, this.gs);
				break;

			case Cmd.ET:
				this.current.resetOnColorspaceChange();
				break;

			case Cmd.DP:
				break;

			case Cmd.MP:
				break;

			case Cmd.Tf:
				currentTextState.TF(this.parser.parseFloat(0), (this.parser.generateOpAsString(1, true)));
				break;

			case Cmd.Tc:
				currentTextState.setCharacterSpacing(this.parser.parseFloat(0));
				break;

			case Cmd.TD:
				TD(false, this.parser.parseFloat(1), this.parser.parseFloat(0), currentTextState);
				break;

			case Cmd.Td:
				TD(true, this.parser.parseFloat(1), this.parser.parseFloat(0), currentTextState);
				break;

			case Cmd.Tj:
				TJ(this.parser.getStream(), startCommand, dataPointer);
				break;

			case Cmd.TJ:
				TJ(this.parser.getStream(), startCommand, dataPointer);
				break;

			case Cmd.quote:
				TSTAR();
				TJ(this.parser.getStream(), startCommand, dataPointer);
				break;

			case Cmd.doubleQuote:
				double_quote(this.parser.getStream(), startCommand, dataPointer, this.parser.parseFloat(1), this.parser.parseFloat(2));
				break;

			case Cmd.Tm:
				// set Tm matrix
				currentTextState.Tm[0][0] = this.parser.parseFloat(5);
				currentTextState.Tm[0][1] = this.parser.parseFloat(4);
				currentTextState.Tm[0][2] = 0;
				currentTextState.Tm[1][0] = this.parser.parseFloat(3);
				currentTextState.Tm[1][1] = this.parser.parseFloat(2);
				currentTextState.Tm[1][2] = 0;
				currentTextState.Tm[2][0] = this.parser.parseFloat(1);
				currentTextState.Tm[2][1] = this.parser.parseFloat(0);
				currentTextState.Tm[2][2] = 1;

				// set Tm matrix
				currentTextState.TmNoRotation[0][0] = currentTextState.Tm[0][0];
				currentTextState.TmNoRotation[0][1] = currentTextState.Tm[0][1];
				currentTextState.TmNoRotation[0][2] = 0;
				currentTextState.TmNoRotation[1][0] = currentTextState.Tm[1][0];
				currentTextState.TmNoRotation[1][1] = currentTextState.Tm[1][1];
				currentTextState.TmNoRotation[1][2] = 0;
				currentTextState.TmNoRotation[2][0] = currentTextState.Tm[2][0];
				currentTextState.TmNoRotation[2][1] = currentTextState.Tm[2][1];
				currentTextState.TmNoRotation[2][2] = 1;

				TM();
				break;

			case Cmd.Tstar:
				TSTAR();
				break;

			case Cmd.Tr:
				TR(this.parser.parseInt(0), this.gs);
				break;

			case Cmd.Ts:
				currentTextState.setTextRise(this.parser.parseFloat(0));
				break;

			case Cmd.Tw:
				currentTextState.setWordSpacing(this.parser.parseFloat(0));
				break;

			case Cmd.Tz:
				currentTextState.setHorizontalScaling(this.parser.parseFloat(0) / 100);
				break;

			case Cmd.TL:
				currentTextState.setLeading(this.parser.parseFloat(0));
				break;
		}
		return dataPointer;
	}

	private static PdfObject BDC(int startCommand, int dataPointer, byte[] raw, String op, LayerDecoder layerDecoder, GraphicsState gs,
			PdfObjectReader currentPdfFile, DynamicVectorRenderer current, boolean markedContentExtracted) {

		PdfObject BDCobj = new MCObject(op);
		BDCobj.setID(PdfDictionary.BDC); // use an existing feature to add unknown tags

		int rawStart = startCommand;

		if (startCommand < 1) startCommand = 1;

		boolean hasDictionary = true;
		while (startCommand < raw.length && raw[startCommand] != '<' && raw[startCommand - 1] != '<') {
			startCommand++;

			if (raw[startCommand] == 'B' && raw[startCommand + 1] == 'D' && raw[startCommand + 2] == 'C') {
				hasDictionary = false;
				break;
			}
		}

		/**
		 * read Dictionary object
		 */
		if (hasDictionary && (markedContentExtracted || (layerDecoder.getPdfLayerList() != null && layerDecoder.isLayerVisible()))) {
			ObjectDecoder objectDecoder = new ObjectDecoder(currentPdfFile.getObjectReader());
			objectDecoder.setEndPt(dataPointer);
			objectDecoder.readDictionaryAsObject(BDCobj, startCommand + 1, raw);
		}

		layerDecoder.BDC(BDCobj, gs, current, dataPointer, raw, hasDictionary, rawStart);

		return BDCobj;
	}

	private void double_quote(byte[] characterStream, int startCommand, int dataPointer, float tc, float tw) {

		// Tc part
		this.currentTextState.setCharacterSpacing(tc);

		// Tw
		this.currentTextState.setWordSpacing(tw);

		TSTAR();

		// we can have values which are not accounted for before stream so rollon so we ignore
		while (characterStream[startCommand] != '(' && characterStream[startCommand] != '<' && characterStream[startCommand] != '[') {
			startCommand++;
		}

		TJ(characterStream, startCommand, dataPointer);
	}

	private void TD(boolean isLowerCase, float x, float y, TextState currentTextState) {

		relativeMove(x, y, currentTextState);

		if (!isLowerCase) { // set leading as well
			float TL = -y;
			currentTextState.setLeading(TL);
		}
		reset();
	}

	private void TM() {

		boolean includeRotation = false;
		if (includeRotation) {

			float[][] trm = this.currentTextState.Tm;

			if (trm[1][0] == 0 && trm[0][1] == 0) {
				this.currentRotation = 0;
				this.unRotatedY = -1;
			}
			else {

				// note we convert radians to degrees - ignore if slight
				if (trm[0][1] == 0 || trm[1][0] == 0) {
					this.currentRotation = 0;
					this.unRotatedY = -1;

				}
				else {
					this.rotationAsRadians = -Math.asin(trm[1][0] / trm[0][0]);

					int newRotation = (int) (this.rotationAsRadians * radiansToDegrees);

					if (newRotation == 0) {
						this.currentRotation = 0;
						this.unRotatedY = -1;
					}
					else {
						// set new rotation
						this.currentRotation = newRotation;
						convertToUnrotated(trm);
					}
				}
			}
		}

		// keep position in case we need
		this.currentTextState.setTMAtLineStart();
		this.currentTextState.setTMAtLineStartNoRotation();

		reset();

		// move command
		this.moveCommand = 1; // 0=t*, 1=Tj, 2=TD
	}

	private void TJ(byte[] characterStream, int startCommand, int dataPointer) {

		/** set colors */
		if (this.renderText && this.gs.getTextRenderType() != GraphicsState.INVISIBLE) {
			this.gs.setStrokeColor(this.gs.strokeColorSpace.getColor());
			this.gs.setNonstrokeColor(this.gs.nonstrokeColorSpace.getColor());
		}

		StringBuffer current_value = processTextArray(characterStream, startCommand, dataPointer, this.multiplyer);

		/** get fontsize and ensure positive */
		int fontSize = this.fontSize;
		if (fontSize == 0) fontSize = (int) this.currentTextState.getTfs();

		if (fontSize < 0) fontSize = -fontSize;

		// <start-adobe>
		// will be null if no content
		if (current_value != null && this.isPageContent) {

			String currentColor = null;

			// get colour if needed
			if (this.textColorExtracted) {
				if ((this.gs.getTextRenderType() & GraphicsState.FILL) == GraphicsState.FILL) {
					currentColor = this.gs.nonstrokeColorSpace.getXMLColorToken();
				}
				else {
					currentColor = this.gs.strokeColorSpace.getXMLColorToken();
				}
			}

			storeExtractedText(currentColor, current_value, fontSize, this.currentFontData.getFontName());
		}
		// <end-adobe>

		this.moveCommand = -1; // flags no move!
	}

	private void TR(int value, GraphicsState gs) {

		// Text render mode

		if (value == 0) value = GraphicsState.FILL;
		else
			if (value == 1) value = GraphicsState.STROKE;
			else
				if (value == 2) value = GraphicsState.FILLSTROKE;
				else
					if (value == 3) {
						value = GraphicsState.INVISIBLE;

						// allow user to over-ride
						if (showInvisibleText) value = GraphicsState.FILL;

					}
					else
						if (value == 7) value = GraphicsState.CLIPTEXT;

		gs.setTextRenderType(value);

		if (this.renderPage && !this.renderDirectly) this.current.drawTR(value);
	}

	private void TSTAR() {
		relativeMove(0, -this.currentTextState.getLeading(), this.currentTextState);

		// move command
		this.moveCommand = 0; // 0=t*, 1=Tj, 2=TD

		reset();
	}

	/**
	 * remove rotation on matrix and set unrotated
	 */
	private void convertToUnrotated(float[][] trm) {

		final boolean showCommands = false;

		if (showCommands) {
			System.out.println("------------original value--------------");
			Matrix.show(trm);
		}

		// now we have it, apply to trm to turn back

		// note we convert radians to degrees - ignore if slight
		if (trm[0][1] == 0 || trm[1][0] == 0) return;

		this.rotationAsRadians = -Math.asin(trm[1][0] / trm[0][0]);

		// build transformation matrix by hand to avoid errors in rounding
		float[][] rotation = new float[3][3];
		rotation[0][0] = (float) Math.cos(-this.rotationAsRadians);
		rotation[0][1] = (float) Math.sin(-this.rotationAsRadians);
		rotation[0][2] = 0;
		rotation[1][0] = (float) -Math.sin(-this.rotationAsRadians);
		rotation[1][1] = (float) Math.cos(-this.rotationAsRadians);
		rotation[1][2] = 0;
		rotation[2][0] = 0;
		rotation[2][1] = 0;
		rotation[2][2] = 1;

		// round numbers if close to 1
		for (int yy = 0; yy < 3; yy++) {
			for (int xx = 0; xx < 3; xx++) {
				if ((rotation[xx][yy] > .99) & (rotation[xx][yy] < 1)) rotation[xx][yy] = 1;
			}
		}

		// matrix for corner
		float[][] pt = new float[3][3];
		pt[0][0] = trm[2][0];// +trm[0][1];
		pt[1][1] = trm[2][1];// +trm[1][0];
		pt[2][2] = 1;

		if (showCommands) {
			System.out.println("---------------------pt before-----------rotation=" + this.currentRotation + " radians=" + this.rotationAsRadians);
			Matrix.show(pt);
		}

		pt = Matrix.multiply(rotation, pt);

		if (showCommands) {
			System.out.println("---------------------pt--------------------------" + (pt[1][0] + pt[1][1]));
			Matrix.show(pt);
		}
		// apply to trm

		if (showCommands) {
			System.out.println("====================before====================rotation=" + this.currentRotation + " radians="
					+ this.rotationAsRadians);
			// Matrix.show(trm);
		}
		float[][] unrotatedTrm = Matrix.multiply(rotation, trm);

		// put onto start of line
		float diffY = pt[1][0];
		float newY = pt[1][1] - diffY;
		float convertedY = this.currentTextState.Tm[2][1];
		Integer key = (int) (newY + .5);
		Float mappedY = (Float) this.lines.get(key);

		// allow for fp error
		if (mappedY == null) mappedY = (Float) this.lines.get((int) (newY + 1));

		if (mappedY == null) {
			this.lines.put(key, this.currentTextState.Tm[2][1]);

			// if(currentTextState.Tm[2][0]>1200)
			// System.out.println(mappedY+" "+newY+" "+key+" "+lines.keySet());

		}
		else {
			convertedY = mappedY;

		}

		unrotatedTrm[2][1] = convertedY;

		this.currentTextState.TmNoRotation = unrotatedTrm;

		// adjust matrix so all on same line if on same line
		// float rotatedY;
		if (this.unRotatedY == -1) {
			// track last line
			this.unRotatedY = this.currentTextState.TmNoRotation[2][1];
			// rotatedY =currentTextState.Tm[2][1];

			// currentTextState.TmNoRotation[2][1]= rotatedY;
		}

		this.currentTextState.TmNoRotation[0][1] = 0;
		this.currentTextState.TmNoRotation[1][0] = 0;

		if (showCommands) Matrix.show(this.currentTextState.TmNoRotation);

		/**
		 * if(tokenNumber>3514) showCommands=false; else showCommands=true; /
		 **/
	}

	/**
	 * used by TD and T* to move current co-ord
	 */
	private void relativeMove(float new_x, float new_y, TextState currentTextState) {

		// create matrix to update Tm
		float[][] temp = new float[3][3];

		// float oldX=currentTextState.Tm[2][0];

		currentTextState.Tm = currentTextState.getTMAtLineStart();

		// set Tm matrix
		temp[0][0] = 1;
		temp[0][1] = 0;
		temp[0][2] = 0;
		temp[1][0] = 0;
		temp[1][1] = 1;
		temp[1][2] = 0;
		temp[2][0] = new_x;
		temp[2][1] = new_y;
		temp[2][2] = 1;

		// multiply to get new Tm
		currentTextState.Tm = Matrix.multiply(temp, currentTextState.Tm);

		// hack to fix what appears to be rounding error with Td/multiple TJs on customers-June2011/test2-papeer.pdf
		// float diff=oldX-currentTextState.Tm[2][0];

		// @ask MArk
		// if(1==2 && !endsWithSpace && new_y==0 && new_x>2 && new_x<10 && this.multipleTJs &&
		// oldX>currentTextState.Tm[2][0]&& (diff>2 && diff<4.5) && currentTextState.Tm[0][0]==currentTextState.Tm[1][1] &&
		// currentTextState.Tm[0][0]>10 && currentTextState.Tm[0][0]<12){
		// currentTextState.Tm[2][0]=oldX+0.2f;
		// }

		currentTextState.setTMAtLineStart();

		if (this.currentRotation != 0) {
			// create matrix to update Tm
			float[][] temp2 = new float[3][3];

			currentTextState.TmNoRotation = currentTextState.getTMAtLineStartNoRotation();

			// set Tm matrix
			temp2[0][0] = 1;
			temp2[0][1] = 0;
			temp2[0][2] = 0;
			temp2[1][0] = 0;
			temp2[1][1] = 1;
			temp2[1][2] = 0;
			temp2[2][0] = new_x;
			temp2[2][1] = new_y;
			temp2[2][2] = 1;

			// multiply to get new Tm
			currentTextState.TmNoRotation = Matrix.multiply(temp2, currentTextState.TmNoRotation);

			float plusX = new_x, plusY = new_y;
			if (plusX < 0) plusX = -new_x;
			if (plusY < 0) plusY = -new_y;

			// if new object, recalculate
			if (plusX > currentTextState.Tm[0][0] && plusY > currentTextState.Tm[1][1]) convertToUnrotated(currentTextState.Tm);

			currentTextState.setTMAtLineStartNoRotation();

		}

		// move command
		this.moveCommand = 2; // 0=t*, 1=Tj, 2=TD
	}

	/**
	 * turn TJ into string and plot. THis routine is long but requently called so we want all code 'inlined'
	 */
	private StringBuffer processTextArray(byte[] stream, int startCommand, int dataPointer, float multiplyer) {

		// flag text found as opposed to just spacing
		boolean hasContent = false, isMultiple = false, firstTime = true;

		boolean isTabRemapped = this.currentFontData.getDiffMapping(9) != null;
		boolean isCRRemapped = this.currentFontData.getDiffMapping(10) != null;
		boolean isReturnRemapped = this.currentFontData.getDiffMapping(13) != null;

		int textShiftedMode = NONE;
		final int streamLength = stream.length;

		// roll on at start if necessary
		while ((stream[startCommand] == 91) || (stream[startCommand] == 10) || (stream[startCommand] == 13) || (stream[startCommand] == 32)) {

			if (stream[startCommand] == 91) isMultiple = true;

			startCommand++;
		}

		/** reset global variables and initialise local ones */
		this.textLength = 0;
		int Tmode = this.gs.getTextRenderType();
		// int foreground =((Color)nonstrokeColorSpace.getColor()).getRGB();
		int orientation; // show if horizontal or vertical text and running which way using constants in PdfData
		boolean isHorizontal, inText = false;
		float[][] TrmWithRotationRemoved = new float[3][3]; // needed by Storypad to turn

		float[][] Trm;
		float[][] temp = new float[3][3];
		float[][] TrmBeforeSpace = new float[3][3];
		float[][] TrmBeforeSpaceWithRotationRemoved = new float[3][3];
		char rawChar = ' ', nextChar, lastChar = ' ', openChar = ' ', lastTextChar = 'x';
		int rawInt;
		float actualWidth;
		float width = 0, fontScale, lastWidth = 0, currentWidth = 0, leading = 0;
		String displayValue = "";
		float TFS = this.currentTextState.getTfs();

		// used by HTML to track chars which have ISSUES
		int valueForHTML = -1;

		float rawTFS = TFS;

		if (TFS < 0) TFS = -TFS;

		int type = this.currentFontData.getFontType();

		float spaceWidth = this.currentFontData.getCurrentFontSpaceWidth();

		String unicodeValue = "";
		StringBuffer textData = null;
		if (this.textExtracted) textData = new StringBuffer(50); // used to return a value

		float currentGap;

		boolean isCID = this.currentFontData.isCIDFont();
		int isDouble = this.currentFontData.isDoubleBytes();

		boolean isFontVertical = this.currentFontData.isFontVertical();

		// flag to show text highlight needs to be shifted up to allow for displacement in Trm
		boolean isTextShifted = false;

		/** set character size */
		int charSize = 2;

		if (isCID && !this.currentFontData.isSingleByte()) {
			charSize = 4;
		}

		/** create temp matrix for current text location and factor in scaling */
		Trm = Matrix.multiply(this.currentTextState.Tm, this.gs.CTM);

		// fix for CRBtrader rotated text
		// if(1==2 && Trm[0][0]==0 && Trm[1][1]==0 && Trm[0][1]>0 && Trm[1][0]<0 && pageData.getRotation(pageNum)==0 && currentRotation==0 && !
		// currentFontData.isFontEmbedded){
		//
		// Trm[1][0]=-Trm[1][0];
		//
		// textShiftedMode=RIGHT;
		// }

		if (this.currentRotation != 0) TrmWithRotationRemoved = Matrix.multiply(this.currentTextState.TmNoRotation, this.gs.CTM);

		// adjust for negative TFS
		// if(rawTFS<0){
		// Trm[2][0]=Trm[2][0]-(Trm[0][0]/2);
		// Trm[2][1]=Trm[2][1]-(Trm[1][1]/2);
		//
		// if(currentRotation!=0){
		// TrmWithRotationRemoved[2][0]=TrmWithRotationRemoved[2][0]-(TrmWithRotationRemoved[0][0]/2);
		// TrmWithRotationRemoved[2][1]= Trm[2][1]-(TrmWithRotationRemoved[1][1]/2);
		// }
		// }

		this.charSpacing = this.currentTextState.getCharacterSpacing() / TFS;
		float wordSpacing = this.currentTextState.getWordSpacing() / TFS;

		if (this.multipleTJs) { // allow for consecutive TJ commands
			Trm[2][0] = this.currentTextState.Tm[2][0];
			Trm[2][1] = this.currentTextState.Tm[2][1];

			if (this.currentRotation != 0) {
				TrmWithRotationRemoved[2][0] = this.currentTextState.TmNoRotation[2][0];
				TrmWithRotationRemoved[2][1] = this.currentTextState.TmNoRotation[2][1];
			}
		}

		/** define matrix used for converting to correctly scaled matrix and multiply to set Trm */
		temp[0][0] = rawTFS * this.currentTextState.getHorizontalScaling();
		temp[1][1] = rawTFS;
		temp[2][1] = this.currentTextState.getTextRise();
		temp[2][2] = 1;
		Trm = Matrix.multiply(temp, Trm);

		if (this.currentRotation != 0) TrmWithRotationRemoved = Matrix.multiply(temp, TrmWithRotationRemoved);

		// check for leading before text and adjust position to include
		if (isMultiple && stream[startCommand] != 60 && stream[startCommand] != 40 && stream[startCommand] != 93) {

			float offset = 0;
			while (stream[startCommand] != 40 && stream[startCommand] != 60 && stream[startCommand] != 93) {
				StringBuilder kerning = new StringBuilder(10);
				while (stream[startCommand] != 60 && stream[startCommand] != 40 && stream[startCommand] != 93 && stream[startCommand] != 32) {
					kerning.append((char) stream[startCommand]);
					startCommand++;
				}
				offset = offset + Float.parseFloat(kerning.toString());

				while (stream[startCommand] == 32)
					startCommand++;
			}

			// new condition as we did not cover case where text rotated by matrix so
			// we were adding 0 * offset which is zero! Fixed for just the case found
			// where Trm[0][1]>0 && Trm[1][0]<0
			if (Trm[0][0] == 0 && Trm[1][1] == 0 && Trm[0][1] > 0 && Trm[1][0] < 0) {

				offset = Trm[0][1] * offset / THOUSAND;

				Trm[2][1] = Trm[2][1] - offset;

				if (this.currentRotation != 0) TrmWithRotationRemoved[2][1] = TrmWithRotationRemoved[2][1] - offset;
			}
			else {
				offset = Trm[0][0] * offset / THOUSAND;

				Trm[2][0] = Trm[2][0] - offset;

				if (this.currentRotation != 0) TrmWithRotationRemoved[2][0] = TrmWithRotationRemoved[2][0] - offset;

			}
		}

		this.multipleTJs = true; // flag will be reset by Td/Tj/T* if move takes place.

		/** workout if horizontal or vertical plot and set values */
		if (Trm[1][1] != 0) {
			isHorizontal = true;
			orientation = PdfData.HORIZONTAL_LEFT_TO_RIGHT;

			if (Trm[1][1] < 0) this.fontSize = (int) (Trm[1][1] - 0.5f);
			else this.fontSize = (int) (Trm[1][1] + 0.5f);

			if (this.fontSize == 0) {

				if (Trm[0][1] < 0) this.fontSize = (int) (Trm[0][1] - 0.5f);
				else this.fontSize = (int) (Trm[0][1] + 0.5f);
			}

			fontScale = Trm[0][0];

			// allow for this odd case in 20090818_Mortgage Key Issue Packag .pdf
			if (Trm[0][0] == 0 && Trm[0][1] > 0 && Trm[1][0] < 0 && Trm[1][1] > 0) orientation = PdfData.VERTICAL_BOTTOM_TO_TOP;

		}
		else {

			isHorizontal = false;

			if (Trm[1][0] < 0) this.fontSize = (int) (Trm[1][0] - 0.5f);
			else this.fontSize = (int) (Trm[1][0] + 0.5f);

			if (this.fontSize == 0) {
				if (Trm[0][0] < 0) this.fontSize = (int) (Trm[0][0] - 0.5f);
				else this.fontSize = (int) (Trm[0][0] + 0.5f);
			}

			if (this.fontSize < 0) {
				this.fontSize = -this.fontSize;
				orientation = PdfData.VERTICAL_BOTTOM_TO_TOP;
			}
			else orientation = PdfData.VERTICAL_TOP_TO_BOTTOM;
			fontScale = Trm[0][1];
		}

		// Set text orientation state
		this.currentTextState.writingMode = orientation;

		if (this.fontSize == 0) this.fontSize = 1;

		/**
		 * text printing mode to get around problems with PCL printers
		 */
		Font javaFont = null;

		if (this.isPrinting && this.textPrint == PdfDecoder.STANDARDTEXTSTRINGPRINT
				&& StandardFonts.isStandardFont(this.currentFontData.getFontName(), true)) {

			javaFont = this.currentFontData.getJavaFontX(this.fontSize);

		}
		else
			if (this.currentFontData.isFontEmbedded && !this.currentFontData.isFontSubstituted()) {
				javaFont = null;
			}
			else
				if ((PdfStreamDecoder.useTextPrintingForNonEmbeddedFonts || this.textPrint != PdfDecoder.NOTEXTPRINT) && this.isPrinting) javaFont = this.currentFontData
						.getJavaFontX(this.fontSize);

		float x, y;
		/** extract starting x and y values (we update Trm as we work through text) */
		if (this.currentRotation == 0) {
			x = Trm[2][0];
			y = Trm[2][1];
		}
		else {
			x = TrmWithRotationRemoved[2][0];
			y = TrmWithRotationRemoved[2][1];
		}

		// track text needs to be moved up in highlight
		if (Trm[1][0] < 0 && Trm[0][1] > 0 && Trm[1][1] == 0 && Trm[0][0] == 0) isTextShifted = true;

		/** set max height for CID of guess sensble figure for non-CID */
		float max_height = this.fontSize;

		// fix for Type3 and fontSize not always good guide
		if (type == StandardFonts.TYPE3 && this.fontSize > 10) max_height = 10;

		if (isCID) max_height = Trm[1][1];

		/** now work through all glyphs and render/decode */
		int i = startCommand;

		int numOfPrefixes = 0;

		boolean resetCoords = true;

		while (i < dataPointer) {

			// used by Sanface file to fix spacing issue (only set in specific case)
			actualWidth = -1;

			// extract the next binary index value and convert to char, losing any returns
			while (true) {
				if (lastChar == 92 && rawChar == 92) // checks if \ has been escaped in '\\'=92
				lastChar = 120;
				else lastChar = rawChar;

				rawInt = stream[i];
				if (rawInt < 0) rawInt = 256 + rawInt;
				rawChar = (char) rawInt;

				// eliminate escaped tabs and returns
				if ((rawChar == 92) && (stream[i + 1] == 13 || stream[i + 1] == 10)) { // '\\'=92
					i++;
					rawInt = stream[i];
					if (rawInt < 0) rawInt = 256 + rawInt;
					rawChar = (char) rawInt;

				}

				// stop any returns in data stream getting through (happens in ghostscript)
				if (rawChar != 10 && rawChar != 13) break;

				i++;
			}

			// if(rawInt==15 && !currentFontData.getBaseFontName().contains("AOALOK+y7pob")){
			// if(rawInt==0)
			// System.out.println(rawInt+" "+(char)rawInt+" "+currentFontData.getBaseFontName());

			// }

			/** flag if we have entered/exited text block */
			if (inText) {
				// non CID deliminator (allow for escaped deliminator)
				if (lastChar != 92 && (rawChar == 40 || rawChar == 41)) { // '\\'=92 ')'=41
					if (rawChar == 40) {
						numOfPrefixes++;
					}
					else
						if (rawChar == 41) { // ')'=41
							if (numOfPrefixes <= 0) {
								inText = false; // unset text flag
							}
							else {
								numOfPrefixes--;
							}
						}
				}
				else
					if (openChar == 60 && rawChar == 62) // ie <01>tj '<'=60 '<'=62
					inText = false; // unset text flag
			}

			/** either handle glyph, process leading or handle a deliminator */
			if (inText) { // process if still in text

				lastTextChar = rawChar; // remember last char so we can avoid a rollon at end if its a space

				// convert escape or turn index into correct glyph allow for stream
				if (openChar == 60) { // '<'=60

					int val = 0, chars = 0, nextInt;

					// get number of chars
					for (int i2 = 1; i2 < charSize; i2++) {
						nextInt = stream[i + i2];

						if (nextInt == 62) { // allow for less than 4 chars at end of stream (ie 6c>)
							i2 = 4;
							charSize = 2;
						}
						else
							if (nextInt == 10 || nextInt == 13) { // avoid any returns
								i++;
								i2--;
							}
							else {
								chars++;
							}
					}

					// now convert to value
					int topHex, ptr = 0;
					loop:

					for (int aa = 0; aa < chars + 1; aa++) {

						topHex = stream[i + chars - aa];

						// convert to number
						if (topHex >= 'A' && topHex <= 'F') {
							topHex = topHex - 55;
						}
						else
							if (topHex >= 'a' && topHex <= 'f') {
								topHex = topHex - 87;
							}
							else
								if (topHex >= '0' && topHex <= '9') {
									topHex = topHex - 48;
								}
								else { // ignore 'bum' values
									continue loop;
								}
						val = val + (topHex << multiply16[ptr]);
						ptr++;
					}

					rawInt = val;

					i = i + charSize - 1; // move offset

					rawChar = (char) rawInt;
					displayValue = this.currentFontData.getGlyphValue(rawInt);

					if (isCID && this.currentFontData.getCMAP() != null && this.currentFontData.getUnicodeMapping(rawInt) == null) {
						rawChar = displayValue.charAt(0);
						rawInt = rawChar;
					}

					if (this.textExtracted) unicodeValue = this.currentFontData.getUnicodeValue(displayValue, rawInt);

				}
				else
					if (rawChar == 92 && !isCID) { // any escape chars '\\'=92

						i++;
						lastChar = rawChar;// update last char as escape

						if ((streamLength > (i + 2)) && (Character.isDigit((char) stream[i]))) {

							// see how long number is
							int numberCount = 1;
							if (Character.isDigit((char) stream[i + 1])) {
								numberCount++;
								if (Character.isDigit((char) stream[i + 2])) {
									numberCount++;
								}
							}

							// convert octal escapes
							rawInt = readEscapeValue(i, numberCount, 8, stream);
							i = i + numberCount - 1;

							if (rawInt > 255) {
								rawInt = rawInt - 256;
							}

							rawChar = (char) rawInt; // set to dummy value as may be / value

							displayValue = this.currentFontData.getGlyphValue(rawInt);

							if (this.textExtracted) {
								unicodeValue = this.currentFontData.getUnicodeValue(displayValue, rawInt);
							}

							// allow for \134 (ie \\)
							if (rawChar == 92) // '\\'=92
							{
								rawChar = 120;
							}

						}
						else {

							rawInt = stream[i];
							rawChar = (char) rawInt;

							if (rawChar == 'u') { // convert unicode of format uxxxx to char value
								rawInt = readEscapeValue(i + 1, 4, 16, stream);
								i = i + 4;
								// rawChar = (char) rawInt;
								displayValue = this.currentFontData.getGlyphValue(rawInt);
								if (this.textExtracted) unicodeValue = this.currentFontData.getUnicodeValue(displayValue, rawInt);

							}
							else {

								if (rawChar == 'n') {
									rawInt = '\n';
									rawChar = '\n';
								}
								else
									if (rawChar == 'b') {
										rawInt = '\b';
										rawChar = '\b';
									}
									else
										if (rawChar == 't') {
											rawInt = '\t';
											rawChar = '\t';
										}
										else
											if (rawChar == 'r') {
												rawInt = '\r';
												rawChar = '\r';
											}
											else
												if (rawChar == 'f') {
													rawInt = '\f';
													rawChar = '\f';
												}

								displayValue = this.currentFontData.getGlyphValue(rawInt);

								if (this.textExtracted) {
									unicodeValue = this.currentFontData.getUnicodeValue(displayValue, rawInt);
								}
								if (displayValue.length() > 0) // set raw char
								{
									rawChar = displayValue.charAt(0);
								}
							}
						}

						// fix for character wrong in some T1 fonts
						if (this.currentFontData.getFontType() == StandardFonts.TYPE1
								&& (this.current.getType() == DynamicVectorRenderer.CREATE_HTML
										|| this.current.getType() == DynamicVectorRenderer.CREATE_JAVAFX || this.current.getType() == DynamicVectorRenderer.CREATE_SVG)) {
							String possAltValue = this.currentFontData.getMappedChar(rawInt, true);
							if (possAltValue != null && possAltValue.length() == 1 && possAltValue.toLowerCase().equals(unicodeValue.toLowerCase())) {
								displayValue = possAltValue;
								unicodeValue = displayValue;
							}
						}

					}
					else
						if (isCID) { // could be nonCID cid

							/**
							 * first time we read the first 2 values and then decide if we are in single or double byte mode (ie is there a 0 x 0y
							 * pattern) (or do the 2 values on their own form valid settings)
							 */
							final boolean debug = false;

							// lazy init if needed
							if (StandardFonts.CMAP == null) {
								StandardFonts.readCMAP();
							}

							int firstVal;
							String firstValue, newValue = null;

							/**
							 * read first value
							 */
							// if escaped roll on
							if (stream[i] == 92) {

								i++;

								firstVal = stream[i] & 255;

								if ((streamLength > (i + 2)) && (Character.isDigit((char) stream[i]))) {

									// see how long number is
									int numberCount = 1;
									if (Character.isDigit((char) stream[i + 1])) {
										numberCount++;
										if (Character.isDigit((char) stream[i + 2])) {
											numberCount++;
										}
									}

									// convert octal escapes
									firstVal = readEscapeValue(i, numberCount, 8, stream);
									i = i + numberCount - 1;

									if (firstVal > 255) {
										firstVal = firstVal - 256;
									}

								}
								else
									if (firstVal == 'u') { // convert unicode of format uxxxx to char value
										firstVal = readEscapeValue(i + 1, 4, 16, stream);
										i = i + 4;

									}
									else {

										if (firstVal == 'n') {
											firstVal = '\n';
										}
										else
											if (firstVal == 'b') {
												firstVal = '\b';
											}
											else
												if (firstVal == 't') {
													firstVal = '\t';
												}
												else
													if (firstVal == 'r') {
														firstVal = '\r';
													}
													else
														if (firstVal == 'f') {
															firstVal = '\f';
														}
									}

								rawChar = (char) firstVal;
								rawInt = firstVal;

							}
							else {
								firstVal = rawChar;
							}

							// get as 1 byte value
							firstValue = StandardFonts.CMAP[rawChar];

							if (debug) System.out.println("1 byte values=" + (int) rawChar + " val=" + firstValue + " isDouble=" + isDouble
									+ " currentFontData.hasDoubleBytes=" + this.currentFontData.hasDoubleBytes);// +" "+(char)stream[i-2]+" "+(char)stream[i-1]+" "+(char)stream[i]+" "+(char)stream[i+1]+" "+(char)stream[i+2]+" "+(char)stream[i+3]);

							/**
							 * read second byte if needed (we always read first time to see if double byte or single)
							 */
							boolean isEmbedded = this.currentFontData.isFontEmbedded;
							if (this.currentFontData.hasDoubleBytes || firstValue == null || isDouble != 0 || rawInt > 128) {

								// flag incase we are wrong and need to switch back
								int iBefore = i;

								i++;

								int secondVal = stream[i] & 255;

								boolean secondByteIsEscaped = false;

								// if escaped roll on as workaround hack
								if (stream[i] == 92) {
									i++;

									secondByteIsEscaped = true;

									secondVal = stream[i] & 255;

									if ((streamLength > (i + 2)) && (Character.isDigit((char) stream[i]))) {

										// see how long number is
										int numberCount = 1;
										if (Character.isDigit((char) stream[i + 1])) {
											numberCount++;
											if (Character.isDigit((char) stream[i + 2])) {
												numberCount++;
											}
										}

										// convert octal escapes
										secondVal = readEscapeValue(i, numberCount, 8, stream);
										i = i + numberCount - 1;

										if (secondVal > 255) {
											secondVal = secondVal - 256;
										}

									}
									else
										if (secondVal == 'u') { // convert unicode of format uxxxx to char value
											secondVal = readEscapeValue(i + 1, 4, 16, stream);
											i = i + 4;

										}
										else {

											if (secondVal == 'n') {
												secondVal = '\n';
											}
											else
												if (secondVal == 'b') {
													secondVal = '\b';
												}
												else
													if (secondVal == 't') {
														secondVal = '\t';
													}
													else
														if (secondVal == 'r') {
															secondVal = '\r';
														}
														else
															if (secondVal == 'f') {
																secondVal = '\f';
															}
										}
								}

								int secondByte = secondVal;

								char combinedVal = (char) ((rawChar << 8) + secondVal);

								// lookup in 2 byte version
								newValue = StandardFonts.CMAP[combinedVal];

								isDouble = this.currentFontData.isDoubleBytes(firstVal, secondByte, secondByteIsEscaped);

								if (debug) System.out.println("2 byte values=" + newValue + " " + " isDouble=" + isDouble + " " + combinedVal + " "
										+ firstValue);

								// if no 2 byte value either default to 1 byte
								if (isEmbedded && (isDouble == 1 || combinedVal < 256 || newValue != null)) {// || (!secondByteIsEscaped &&
																												// secondByte!=')'))){
									rawInt = combinedVal;
									rawChar = (char) rawInt;

									if (debug) System.out.println("use 2 values=" + combinedVal + " new value=" + newValue + " isEmbedded="
											+ isEmbedded + " " + (!secondByteIsEscaped && secondByte != ')'));

								}
								else
									if (!isEmbedded && isDouble == 1
											&& (newValue != null || combinedVal < 256 || (!secondByteIsEscaped && secondByte != ')'))) {
										rawInt = combinedVal;
										rawChar = (char) rawInt;

										if (debug) System.out.println("use 2 values=" + combinedVal + " " + newValue);

									}
									else
										if (isDouble == 0 && !isEmbedded && firstVal > 128 && newValue != null && firstValue == null) {
											rawInt = combinedVal;
											rawChar = (char) rawInt;

											if (debug) System.out.println("TEST2 " + newValue + " " + StandardFonts.CMAP[secondByte]);
										}
										else
											if (isDouble == 0 && !isEmbedded && firstVal > 128 && newValue == null && firstValue != null) {

												i = iBefore;
												// rawInt=combinedVal;
												// rawChar=(char)f;
												// newValue = String.valueOf(rawChar);
												newValue = firstValue;
												if (debug) System.out.println("TEST2 " + newValue + " " + StandardFonts.CMAP[secondByte]);

											}
											else {
												i = iBefore;

												if (debug) System.out.println("reset " + newValue + " " + StandardFonts.CMAP[secondByte]);
											}

								if (!isEmbedded) {
									actualWidth = this.currentFontData.getDefaultWidth(rawInt);

									if (actualWidth == -1) {
										actualWidth = this.currentFontData.getDefaultWidth(-1);
									}
								}

							}
							else {

								actualWidth = -1;

								if (!isEmbedded) {
									if (this.currentFontData.getFontType() == StandardFonts.CIDTYPE0
											|| this.currentFontData.getFontType() == StandardFonts.CIDTYPE2) {
										actualWidth = this.currentFontData.getDefaultWidth(rawInt);

										if (actualWidth == -1) {
											actualWidth = this.currentFontData.getDefaultWidth(-1) / 2;
										}
									}
								}
							}

							// if no value ignore for moment
							if (newValue != null) {
								displayValue = newValue;
							}
							else { // default if no value
								displayValue = String.valueOf(rawChar);
							}

							if (this.textExtracted) { // (not sure if this is correct - may need more samples)
								unicodeValue = this.currentFontData.getUnicodeValue(displayValue, rawChar);
							}

							// fix for \\) at end of stream
							if (rawChar == 92) {
								valueForHTML = 92;
								rawChar = 120;
							}

							if (debug) System.out.println("returns =" + displayValue + " " + unicodeValue + " int=" + rawInt + " actualWidth="
									+ actualWidth);

						}
						else {

							displayValue = this.currentFontData.getGlyphValue(rawInt);

							/**
							 * remap chars for HTML,etc - not needed and breaks other code needed to fix glyph mapping issue in odd file for PDF2HTML5
							 * /sample_pdfs_html/thoughtcorp/Simple Relational Contracts.pdf
							 */
							if (displayValue.length() == 0
									&& (this.current.getType() == DynamicVectorRenderer.CREATE_HTML
											|| this.current.getType() == DynamicVectorRenderer.CREATE_JAVAFX || this.current.getType() == DynamicVectorRenderer.CREATE_SVG)) {

								if (!this.currentFontData.isCIDFont()) {
									String charGlyph = this.currentFontData.getMappedChar(rawInt, false);

									if (charGlyph != null) {
										int newRawInt = this.currentFontData.getDiffChar(charGlyph);

										if (newRawInt != -1) {
											rawInt = newRawInt; // only reassign if not -1 as messes up code further down
											displayValue = String.valueOf((char) rawInt);
										}
									}
								}

								// System.out.println(charGlyph+" displayValue="+displayValue+" "+rawInt);
							}

							// if space is actually mapped onto something else we need to reset
							// this variable which tracks space chars (as false match)
							if (rawInt == 32 && !displayValue.equals(" ")) {
								lastTextChar = 'Z';
								// rawChar='Z';
							}

							if (this.textExtracted) unicodeValue = this.currentFontData.getUnicodeValue(displayValue, rawInt);

							// fix for character wrong in some T1 fonts
							if (this.currentFontData.getFontType() == StandardFonts.TYPE1
									&& (this.current.getType() == DynamicVectorRenderer.CREATE_HTML
											|| this.current.getType() == DynamicVectorRenderer.CREATE_JAVAFX || this.current.getType() == DynamicVectorRenderer.CREATE_SVG)) {
								String possAltValue = this.currentFontData.getMappedChar(rawInt, true);
								if (possAltValue != null && possAltValue.length() == 1
										&& possAltValue.toLowerCase().equals(unicodeValue.toLowerCase())) {
									displayValue = possAltValue;
									unicodeValue = displayValue;
								}
							}
						}

				// Handle extracting CID Identity fonts
				if (StandardFonts.samsCIDExtractionCode
						&& !this.currentFontData.hasToUnicode()
						&& this.currentFontData.getFontType() == StandardFonts.CIDTYPE0
						&& this.currentFontData.getGlyphData().isIdentity()
						&& (this.current.getType() == DynamicVectorRenderer.CREATE_JAVAFX
								|| this.current.getType() == DynamicVectorRenderer.CREATE_HTML || this.current.getType() == DynamicVectorRenderer.CREATE_SVG)) {

					// Check if proper char has been stored instead
					int charToUse = rawChar;
					if (valueForHTML != -1) {
						charToUse = valueForHTML;
						valueForHTML = -1;
					}

					int rawC = StandardFonts.mapCIDToValidUnicode(this.currentFontData.getBaseFontName(), charToUse);
					unicodeValue = String.valueOf((char) (rawC));
				}

				// Itext likes to use Tabs!
				if (rawInt == 9 && !isTabRemapped && this.currentFontData.isFontSubstituted()) {
					rawInt = 32;
					displayValue = " ";
					unicodeValue = " ";
				}

				// MOVE pointer to next location by updating matrix
				temp[0][0] = 1;
				temp[0][1] = 0;
				temp[0][2] = 0;
				temp[1][0] = 0;
				temp[1][1] = 1;
				temp[1][2] = 0;

				if (isFontVertical) {
					temp[2][1] = -(currentWidth + leading); // tx;
					temp[2][0] = 0; // ty;
				}
				else {
					temp[2][0] = (currentWidth + leading); // tx;
					temp[2][1] = 0; // ty;
				}
				temp[2][2] = 1;
				Trm = Matrix.multiply(temp, Trm); // multiply to get new Tm

				if (this.currentRotation != 0) TrmWithRotationRemoved = Matrix.multiply(temp, TrmWithRotationRemoved); // multiply to get new Tm

				/** save pointer in case its just multiple spaces at end */
				if (rawChar == ' ' && lastChar != ' ') {
					TrmBeforeSpace = Trm;

					if (this.currentRotation != 0) TrmBeforeSpaceWithRotationRemoved = TrmWithRotationRemoved;
				}

				leading = 0; // reset leading

				PdfJavaGlyphs glyphs = this.currentFontData.getGlyphData();

				if (this.currentFontData.isCIDFont() && glyphs.is1C() && !glyphs.isIdentity()) {

					int idx = glyphs.getCMAPValue(rawInt);
					if (idx > 0) rawInt = idx;

				}
				int idx = rawInt;

				if (!glyphs.isCorrupted()) {
					if (this.currentFontData.isCIDFont() && !glyphs.isIdentity()) {
						int mappedIdx = glyphs.getConvertedGlyph(rawInt);

						if (mappedIdx != -1) idx = mappedIdx;
					}
					else
						if (this.currentFontData.getFontType() != StandardFonts.TYPE3) {// if a numeric value we need to replace to get correct glyph
							int diff = this.currentFontData.getDiffChar(rawInt);
							if (diff > 0) {
								rawInt = diff;
							}
						}
				}

				// if(currentFontData.getBaseFontName().contains("ABGDHB+ICON1"))
				// System.out.println("display="+displayValue);

				// fix for odd font issue in Sanface output with CID (ie Customers-Dec2011/japanese.pdf)

				if (actualWidth > 0) {
					currentWidth = actualWidth;
				}
				else {
					currentWidth = this.currentFontData.getWidth(idx);
				}

				// used by HTML
				if ((this.current.getType() == DynamicVectorRenderer.CREATE_HTML || this.current.getType() == DynamicVectorRenderer.CREATE_JAVAFX || this.current
						.getType() == DynamicVectorRenderer.CREATE_SVG)) {
					this.currentFontData.setCurrentWidth(currentWidth);
				}

				// System.out.println(idx+" "+rawInt+" "+currentWidth+" "+currentFontData+" "+currentFontData.getBaseFontName());
				/**
				 * Corel can add spaces and use Tw to move the cursor back so space does not really exist. This code fixes it and removes space for
				 * HTML
				 */
				if (wordSpacing < 0
						&& rawInt == 32
						&& (this.current.getType() == DynamicVectorRenderer.CREATE_HTML || this.current.getType() == DynamicVectorRenderer.CREATE_SVG || this.current
								.getType() == DynamicVectorRenderer.CREATE_JAVAFX)) {

					float diff = Math.abs(wordSpacing + currentWidth);

					// if space does not really exist
					if (diff < 0.01) {
						unicodeValue = "";
					}
				}

				// if(currentWidth==0)
				// currentWidth=0.6f;
				// debug code to lock out text if not in area
				// System.out.println(currentWidth+"=========="+" rawInt="+rawInt+" idx="+idx+" d="+displayValue+"< uni="+unicodeValue+"< "+currentFontData+" "+currentFontData.getFontName()+" "+currentFontData.getBaseFontName());

				/** if we have a valid character and we are rendering, draw it */

				if (this.renderText && Tmode != GraphicsState.INVISIBLE) {

					if (this.isPrinting
							&& javaFont != null
							&& (this.textPrint == PdfDecoder.STANDARDTEXTSTRINGPRINT || (this.textPrint == PdfDecoder.TEXTSTRINGPRINT || (PdfStreamDecoder.useTextPrintingForNonEmbeddedFonts && (!this.currentFontData.isFontEmbedded || this.currentFontData
									.isFontSubstituted()))))) {

						/** support for TR7 */
						if (Tmode == GraphicsState.CLIPTEXT) {

							/** set values used if rendering as well */
							boolean isSTD = DecoderOptions.isRunningOnMac || StandardFonts.isStandardFont(this.currentFontData.getBaseFontName(), false);
							Area transformedGlyph2 = glyphs.getStandardGlyph(Trm, rawInt, displayValue, currentWidth, isSTD);

							if (transformedGlyph2 != null) {
								this.gs.addClip(transformedGlyph2);
								// current.drawClip(gs) ;
							}

							this.current.drawClip(this.gs, null, true);

						}

						if (displayValue != null && !displayValue.startsWith("&#")) {
							if (this.current.getType() == DynamicVectorRenderer.CREATE_JAVAFX
									|| this.current.getType() == DynamicVectorRenderer.CREATE_HTML
									|| this.current.getType() == DynamicVectorRenderer.CREATE_SVG) this.current.drawEmbeddedText(Trm, this.fontSize,
									null, null, DynamicVectorRenderer.TEXT, this.gs, null, displayValue, this.currentFontData, -100);
							else this.current.drawText(Trm, displayValue, this.gs, Trm[2][0], -Trm[2][1], javaFont);
						}

					}
					else
						if (((this.textPrint != PdfDecoder.TEXTGLYPHPRINT) || (javaFont == null))
								&& (this.currentFontData.isFontEmbedded && this.currentFontData.isFontSubstituted() && ((rawInt == 9 && !isTabRemapped)
										|| (rawInt == 10 && !isCRRemapped) || (rawInt == 13 && !isReturnRemapped)))) { // &&
							// lose returns which can cause odd display
						}
						else
							if (((this.textPrint != PdfDecoder.TEXTGLYPHPRINT) || (javaFont == null))
									&& (this.currentFontData.isFontSubstituted() && currentWidth == 0 && displayValue.charAt(0) == 13)) { // remove
																																			// substituted
																																			// values
																																			// so do
																																			// not
																																			// enter
																																			// test
																																			// below
							}
							else
								if (((this.textPrint != PdfDecoder.TEXTGLYPHPRINT) || (javaFont == null)) && (this.currentFontData.isFontEmbedded)) { // &&
									// (!currentFontData.isFontSubstituted() || !displayValue.startsWith("&#"))){

									// get glyph if not CID
									String charGlyph = "notdef";

									try {

										if (!this.currentFontData.isCIDFont()) charGlyph = this.currentFontData.getMappedChar(rawInt, false);

										PdfGlyph glyph;

										{

											glyph = glyphs.getEmbeddedGlyph(this.factory, charGlyph, Trm, rawInt, displayValue, currentWidth,
													this.currentFontData.getEmbeddedChar(rawInt));

										}

										// avoid null type 3 glyphs and set color if needed
										if (type == StandardFonts.TYPE3) {

											if (glyph != null && glyph.getmaxWidth() == 0) glyph = null;
											else
												if (glyph != null && glyph.ignoreColors()) {

													glyph.setT3Colors(this.gs.getNonstrokeColor(), this.gs.getNonstrokeColor(), true);
												}
										}

										if (glyph != null) {

											// set raw width to use for scaling
											if (glyph != null && type == StandardFonts.TYPE1) glyph.setWidth(currentWidth * 1000);

											float[][] finalTrm = { { Trm[0][0], Trm[0][1], 0 }, { Trm[1][0], Trm[1][1], 0 },
													{ Trm[2][0], Trm[2][1], 1 } };

											float[][] finalScale = {
													{ (float) this.currentFontData.FontMatrix[0], (float) this.currentFontData.FontMatrix[1], 0 },
													{ (float) this.currentFontData.FontMatrix[2], (float) this.currentFontData.FontMatrix[3], 0 },
													{ 0, 0, 1 } };

											// factor in fontmatrix (which may include italic)
											finalTrm = Matrix.multiply(finalTrm, finalScale);

											finalTrm[2][0] = Trm[2][0];
											finalTrm[2][1] = Trm[2][1];

											// manipulate matrix to get right rotation
											if (finalTrm[1][0] < 0 && finalTrm[0][1] < 0) {
												finalTrm[1][0] = -finalTrm[1][0];
												finalTrm[0][1] = -finalTrm[0][1];
											}

											if (type == StandardFonts.TYPE3) {

												float h = 0;
												if (finalTrm[1][1] != 0) h = (this.fontSize * finalTrm[1][1]);
												else
													if (finalTrm[0][0] != 0) h = (this.fontSize * finalTrm[0][0]);
													else
														if (finalTrm[1][0] != 0) h = (this.fontSize * finalTrm[1][0]);

												if (h < 0) h = -h;

												if (h > max_height) max_height = h;

											}

											// create shape for text using tranformation to make correct size
											AffineTransform at = new AffineTransform(finalTrm[0][0], finalTrm[0][1], finalTrm[1][0], finalTrm[1][1],
													finalTrm[2][0], finalTrm[2][1]);

											// add to renderer
											int fontType = DynamicVectorRenderer.TYPE1C;
											if (type == StandardFonts.OPENTYPE) {
												fontType = DynamicVectorRenderer.TYPE1C;

												// and fix for scaling in OTF
												float z = 1000f / (glyph.getmaxWidth());
												at.scale(currentWidth * z, 1);

											}
											else
												if (type == StandardFonts.TRUETYPE || type == StandardFonts.CIDTYPE2
														|| (this.currentFontData.isFontSubstituted() && type != StandardFonts.TYPE1)) {
													fontType = DynamicVectorRenderer.TRUETYPE;
												}
												else
													if (type == StandardFonts.TYPE3) {
														fontType = DynamicVectorRenderer.TYPE3;
													}

											// negative as flag to show we need to decode later
											if (this.generateGlyphOnRender) fontType = -fontType;

											/**
											 * add glyph outline to shape in TR7 mode
											 */
											if ((Tmode == GraphicsState.CLIPTEXT)) {

												if (glyph.getShape() != null) {

													Area glyphShape = (Area) (glyph.getShape()).clone();

													glyphShape.transform(at);

													if (glyphShape.getBounds().getWidth() > 0 && glyphShape.getBounds().getHeight() > 0) {

														this.gs.addClip(glyphShape);

														this.current.drawClip(this.gs, null, false);

													}
												}
											}

											float lw = this.gs.getLineWidth();
											float lineWidth = 0;
											if (multiplyer > 0) {
												lineWidth = 1f / multiplyer;

											}
											this.gs.setLineWidth(lineWidth);

											if (isTextShifted) this.current.drawEmbeddedText(Trm, -this.fontSize, glyph, null, fontType, this.gs, at,
													unicodeValue, this.currentFontData, -100);
											else this.current.drawEmbeddedText(Trm, this.fontSize, glyph, null, fontType, this.gs, at, unicodeValue,
													this.currentFontData, -100);

											this.gs.setLineWidth(lw);

										}
										else { // if no valid glyph data, treat as a space
											displayValue = " ";
											unicodeValue = " ";
										}
									}
									catch (Exception e) {

										// tell user and log
										if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());

										this.errorTracker.addPageFailureMessage("Exception " + e + " on embedded font renderer");

									}

								}
								else
									if (displayValue.length() > 0 && !displayValue.startsWith("&#")) firstTime = renderTextWithJavaFonts(actualWidth,
											firstTime, Tmode, this.fontSize, rawInt, currentWidth, displayValue, unicodeValue, isTextShifted, glyphs,
											Trm);
				}

				/** now we have plotted it we update pointers and extract the text */
				currentWidth = currentWidth + this.charSpacing;

				if (rawChar == ' ') // add word spacing if
				currentWidth = currentWidth + wordSpacing;

				// workout gap between chars and decide if we should add a space
				currentGap = (width + this.charSpacing - lastWidth);
				String spaces = "";
				if (currentGap > 0 && lastWidth > 0) {
					spaces = PdfFont.getSpaces(currentGap, spaceWidth, PdfStreamDecoder.currentThreshold);
				}

				this.textLength++; // counter on chars in data
				width = width + currentWidth;
				lastWidth = width; // increase width by current char

				// add unicode value to our text data with embedded width
				if (this.textExtracted) hasContent = writeOutText(hasContent, isHorizontal, TrmWithRotationRemoved, Trm, fontScale, currentWidth,
						unicodeValue, textData, spaces, this.isXMLExtraction, this.currentRotation);

			}
			else
				if (rawChar == 40 || rawChar == 60) { // start of text stream '('=40 '<'=60

					inText = true; // set text flag - no escape character possible
					openChar = rawChar;

				}
				else
					if ((rawChar == 41) || (rawChar == 62 && openChar == 60)
							|| ((!inText) && ((rawChar == '-') || (rawChar >= '0' && rawChar <= '9')))) { // ')'=41 '>'=62 '<'=60

						// handle leading between text ie -100 in (The)-100(text)

						float value = 0;
						i++;

						// allow for spaces
						while (stream[i] == 32 || stream[i] == 13 || stream[i] == 10)
							// ' '=32
							i++;

						nextChar = (char) stream[i];

						// allow for )( or >< (ie no value)
						if (nextChar == 40 || nextChar == 60) { // '('=40 '<'=60
							i--;
						}
						else
							if ((nextChar != 39) && (nextChar != 34) && (nextChar != 40) && (nextChar != 93) && (nextChar != 60)) { // leading so roll
																																	// on char
								// '\''=39 '\"'=34 '('=40 //']'=93 '<'=60
								int ptr = 0;

								int leadingStart = i; // allow for failure
								boolean failed = false;
								boolean isMultipleValues = false, isLastValue = false;
								while (!failed) {
									rawChar = nextChar;
									if (rawChar != 10 && rawChar != 13) {
										ptr++;
									}

									nextChar = (char) stream[i + 1];

									if (nextChar == 32) isMultipleValues = true;

									if (nextChar == ']') isLastValue = true;

									if (nextChar == 40 || nextChar == 60 || nextChar == ']' || nextChar == 10) // '('=40 '<'=60
									break;

									if (nextChar == 45 || nextChar == 46 || nextChar == 32 || (nextChar >= '0' && nextChar <= '9')) {
										// '-'=45 '.'=46 ' '=32
									}
									else failed = true;

									i++;
								}

								if (failed) i = leadingStart;
								else {
									value = getLeading(stream, value, ptr, leadingStart, isMultipleValues);
								}

								// is someone adds on leading at end ignore as it breaks extraction width calculation
								if (isLastValue && value == -width) {
									// width=width-value;
									leading = leading - value;
								}
							}

						width = width + value;
						leading = leading + value; // keep count on leading

					}

			// textExtracted added by Mark
			// generate if we are in Viewer (do not bother if thumbnails)
			if (this.textExtracted) resetCoords = setExtractedText(textShiftedMode, Trm, fontScale, currentWidth, displayValue, resetCoords);

			i++;
		}

		/** all text is now drawn (if required) and text has been decoded */

		// final move to get end of shape
		temp[0][0] = 1;
		temp[0][1] = 0;
		temp[0][2] = 0;
		temp[1][0] = 0;
		temp[1][1] = 1;
		temp[1][2] = 0;

		// if leading moves it back into text, leave off
		if (leading < 0) temp[2][0] = (currentWidth);
		else temp[2][0] = (currentWidth + leading); // tx;

		temp[2][1] = 0; // ty;
		temp[2][2] = 1;
		Trm = Matrix.multiply(temp, Trm); // multiply to get new Tm

		// update Tm to cursor
		this.currentTextState.Tm[2][0] = Trm[2][0];
		this.currentTextState.Tm[2][1] = Trm[2][1] - this.currentTextState.getTextRise();

		if (this.currentRotation != 0) {

			TrmWithRotationRemoved = Matrix.multiply(temp, TrmWithRotationRemoved); // multiply to get new Tm

			// update Tm to cursor
			this.currentTextState.TmNoRotation[2][0] = TrmWithRotationRemoved[2][0];
			this.currentTextState.TmNoRotation[2][1] = TrmWithRotationRemoved[2][1];

		}

		/**
		 * we need this outside textExtracted as needed in all modes
		 */
		// endsWithSpace=lastTextChar == ' ';

		/**
		 * now workout the rectangular shape this text occupies by creating a box of the correct width/height and transforming it (this routine could
		 * undoutedly be better coded but it works and I don't want to break it!!)
		 */
		if (this.textExtracted) {

			/** roll on if last char is not a space - otherwise restore to before spaces */
			if (lastTextChar == ' ') {

				Trm = TrmBeforeSpace;

				if (this.currentRotation != 0) TrmWithRotationRemoved = TrmBeforeSpaceWithRotationRemoved;
			}

			/** calculate rectangular shape of text */
			if (this.currentRotation == 0) calcCoordinates(x, Trm, isHorizontal, max_height, this.fontSize, y);
			else calcCoordinates(x, TrmWithRotationRemoved, isHorizontal, max_height, this.fontSize, y);

			/**
			 * if we have an /ActualText use that instead with the width data at start of original
			 */
			if (textData != null && this.actualText != null) {
				int startValue = textData.indexOf(PdfData.marker, 2);
				if (startValue > 0) startValue = textData.indexOf(PdfData.marker, startValue + 1);

				if (startValue > 0) {
					textData.setLength(startValue + 1); // keep width data but lose text
					textData.append(this.actualText); // subsitute in /ActualText
				}

				this.actualText = null;
			}

			/** return null for no text */
			if (textData.length() == 0 || !hasContent) // return null if no text
			textData = null;

			return textData;
		}
		else return null;
	}

	private boolean renderTextWithJavaFonts(float actualWidth, boolean firstTime, int Tmode, int fontSize, int rawInt, float currentWidth,
			String displayValue, String unicodeValue, boolean isTextShifted, PdfJavaGlyphs glyphs, float[][] displayTrm) {
		{

			/** set values used if rendering as well */
			Object transformedGlyph2;
			AffineTransform glyphAt = null;

			{ // render now

				boolean isSTD = actualWidth > 0 || DecoderOptions.isRunningOnMac
						|| StandardFonts.isStandardFont(this.currentFontData.getBaseFontName(), false) || this.currentFontData.isBrokenFont();

				/** flush cache if needed */
				// if(!DynamicVectorRenderer.newCode2){
				if (glyphs.lastTrm[0][0] != displayTrm[0][0] || glyphs.lastTrm[1][0] != displayTrm[1][0] || glyphs.lastTrm[0][1] != displayTrm[0][1]
						|| glyphs.lastTrm[1][1] != displayTrm[1][1]) {
					glyphs.lastTrm = displayTrm;
					glyphs.flush();
				}
				// }

				// either calculate the glyph to draw or reuse if already drawn
				Area glyph = glyphs.getCachedShape(rawInt);
				glyphAt = glyphs.getCachedTransform(rawInt);

				if (glyph == null) {

					double dY = -1, dX = 1, x3 = 0, y3 = 0;

					// allow for text running up the page
					if ((displayTrm[1][0] < 0 && displayTrm[0][1] >= 0) || (displayTrm[0][1] < 0 && displayTrm[1][0] >= 0)) {
						dX = 1f;
						dY = -1f;
					}

					if (isSTD) {

						glyph = glyphs.getGlyph(rawInt, displayValue, currentWidth);

						// hack to fix problem with Java Arial font
						if (glyph != null && rawInt == 146 && glyphs.isArialInstalledLocally) y3 = -(glyph.getBounds().height - glyph.getBounds().y);
					}
					else {

						// remap font if needed
						String xx = displayValue;
						if (glyphs.remapFont && (!glyphs.getUnscaledFont().canDisplay(xx.charAt(0)))) xx = String.valueOf((char) (rawInt + 0xf000));

						GlyphVector gv1 = null;

						// do not show CID fonts as Lucida unless match
						if (!glyphs.isCIDFont || glyphs.isFontInstalled) gv1 = glyphs.getUnscaledFont().createGlyphVector(PdfJavaGlyphs.frc, xx);

						if (gv1 != null) {

							glyph = new Area(gv1.getOutline());

							// put glyph into display position
							double glyphX = gv1.getOutline().getBounds2D().getX();

							// ensure inside box
							x3 = 0;

							if (glyphX < 0) {
								glyphX = -glyphX;

								x3 = glyphX * 2;

								// System.out.println(x3+" "+displayTrm[0][0]+" "+displayTrm[0][0]);

								if (displayTrm[0][0] > displayTrm[0][1]) x3 = x3 * displayTrm[0][0];
								else x3 = x3 * displayTrm[0][1];

								// glyphAt =AffineTransform.getTranslateInstance(x3,0);

							}

							double glyphWidth = gv1.getVisualBounds().getWidth() + (glyphX * 2), scaleFactor = currentWidth / glyphWidth;
							if (scaleFactor < 1) dX = dX * scaleFactor;

							if (x3 > 0) {
								x3 = x3 * dX;
							}
						}
					}

					glyphAt = new AffineTransform(dX * displayTrm[0][0], dX * displayTrm[0][1], dY * displayTrm[1][0], dY * displayTrm[1][1], x3, y3);
					// create shape for text using transformation to make correct size
					// glyphAt =new AffineTransform(dX* displayTrm[0][0],dX* displayTrm[0][1],dY* displayTrm[1][0],dY* displayTrm[1][1] ,x3, y3);

					// save so we can reuse if it occurs again in this TJ command
					glyphs.setCachedShape(rawInt, glyph, glyphAt);
				}

				if (glyph != null && Tmode == GraphicsState.CLIPTEXT && glyph.getBounds().width > 0) {
					/** support for TR7 */

					Area glyphShape = (Area) glyph.clone();

					// we need to apply to make it all work
					glyphShape.transform(glyphAt);

					// if its already generated we just need to move it
					if (this.renderDirectly) {
						AffineTransform at2 = AffineTransform.getTranslateInstance(displayTrm[2][0], (displayTrm[2][1]));
						glyphShape.transform(at2);
					}

					this.gs.addClip(glyphShape);

					this.current.drawClip(this.gs, null, false);

					if (this.renderDirectly) glyph = null;

				}

				transformedGlyph2 = glyph;

			}

			if (transformedGlyph2 != null) {

				// add to renderer
				if (this.renderDirectly) {
					this.current.drawEmbeddedText(displayTrm, fontSize, null, transformedGlyph2, DynamicVectorRenderer.TEXT, this.gs, glyphAt,
							unicodeValue, this.currentFontData, -100);
				}
				else {

					if (isTextShifted) this.current.drawEmbeddedText(displayTrm, -fontSize, null, transformedGlyph2, DynamicVectorRenderer.TEXT,
							this.gs, null, unicodeValue, this.currentFontData, -100);
					else this.current.drawEmbeddedText(displayTrm, fontSize, null, transformedGlyph2, DynamicVectorRenderer.TEXT, this.gs, null,
							unicodeValue, this.currentFontData, -100);
				}
			}
		}
		return firstTime;
	}

	/**
	 * add text chars to our text object for extraction
	 * 
	 * @param hasContent
	 * @param isHorizontal
	 * @param TrmWithRotationRemoved
	 * @param Trm
	 * @param fontScale
	 * @param currentWidth
	 * @param unicodeValue
	 * @param textData
	 * @param spaces
	 */
	private static boolean writeOutText(boolean hasContent, boolean isHorizontal, float[][] TrmWithRotationRemoved, float[][] Trm, float fontScale,
			float currentWidth, String unicodeValue, StringBuffer textData, String spaces, boolean isXMLExtraction, int currentRotation) {

		if (unicodeValue.length() > 0) {

			// add character to text we have decoded with width
			// if large space separate out
			if (PdfDecoder.embedWidthData) {

				float xx = Trm[2][0];
				float yy = Trm[2][1];

				if (currentRotation != 0) {
					xx = TrmWithRotationRemoved[2][0];
					yy = TrmWithRotationRemoved[2][1];
				}

				textData.append(spaces);

				// embed width information in data
				if (isHorizontal) {
					textData.append(PdfData.marker);
					textData.append(xx);
					textData.append(PdfData.marker);

				}
				else {
					textData.append(PdfData.marker);
					textData.append(yy);
					textData.append(PdfData.marker);
				}
				// if(hasTextSpace)
				// textData.append((currentWidth-charSpacing) * fontScale);
				// else
				textData.append(currentWidth * fontScale);

				textData.append(PdfData.marker);

			}
			else textData.append(spaces);

			/** add data to output */

			// turn chars less than 32 into escape
			int length = unicodeValue.length();
			char next;
			for (int ii = 0; ii < length; ii++) {
				next = unicodeValue.charAt(ii);

				hasContent = true;

				// map tab to space
				if (next == 9) next = 32;

				if (next == '<' && isXMLExtraction) textData.append("&lt;");
				else
					if (next == '>' && isXMLExtraction) textData.append("&gt;");
					else
						if (next == 64258) textData.append("fl");
						else
							if (next > 31) textData.append(next);
							else textData.append(hex[next]);
			}
		}
		else textData.append(spaces);

		return hasContent;
	}

	/** return the data */
	@Override
	public Object getObjectValue(int key) {

		switch (key) {

			case ValueTypes.TextAreas:
				return this.textAreas;

			case ValueTypes.TextDirections:
				return this.textDirections;

		}

		return null;
	}

	/**
	 * convert to to String
	 */
	private static String getString(int start, int end, byte[] dataStream) {

		String s;

		// lose spaces or returns at end
		while ((dataStream[end] == 32) || (dataStream[end] == 13) || (dataStream[end] == 10))
			end--;

		int count = end - start + 1;

		// discount duplicate spaces
		int spaces = 0;
		for (int ii = 0; ii < count; ii++) {
			if ((ii > 0) && ((dataStream[start + ii] == 32) || (dataStream[start + ii] == 13) || (dataStream[start + ii] == 10))
					&& ((dataStream[start + ii - 1] == 32) || (dataStream[start + ii - 1] == 13) || (dataStream[start + ii - 1] == 10))) spaces++;
		}

		char[] charString = new char[count - spaces];
		int pos = 0;

		for (int ii = 0; ii < count; ii++) {
			if ((ii > 0) && ((dataStream[start + ii] == 32) || (dataStream[start + ii] == 13) || (dataStream[start + ii] == 10))
					&& ((dataStream[start + ii - 1] == 32) || (dataStream[start + ii - 1] == 13) || (dataStream[start + ii - 1] == 10))) {}
			else {
				if ((dataStream[start + ii] == 10) || (dataStream[start + ii] == 13)) charString[pos] = ' ';
				else charString[pos] = (char) dataStream[start + ii];
				pos++;
			}
		}

		s = String.copyValueOf(charString);

		return s;
	}

	private static float getLeading(byte[] stream, float value, int ptr, int leadingStart, boolean isMultipleValues) {
		// more than one value separated by space
		if (isMultipleValues) {

			// get string
			int strt = leadingStart;
			while (stream[strt] == 10 || stream[strt] == 9 || stream[strt] == 32 || stream[strt] == 13)
				strt++;

			String val = getString(strt, strt + ptr - 1, stream);

			// read values
			StringTokenizer values = new StringTokenizer(val);
			value = 0;
			while (values.hasMoreTokens())
				value = value + Float.parseFloat(values.nextToken());

			value = -value / THOUSAND;
		}
		else
			if (ptr > 0) {

				int strt = leadingStart;
				while (stream[strt] == 10 || stream[strt] == 9 || stream[strt] == 32 || stream[strt] == 13)
					strt++;

				value = -NumberUtils.parseFloat(strt, strt + ptr, stream) / THOUSAND;
			}
		return value;
	}

	private boolean setExtractedText(int textShiftedMode, float[][] Trm, float fontScale, float currentWidth, String displayValue, boolean resetCoords) {
		{
			if (displayValue.length() > 0 && !displayValue.equals(" ")) {

				float xx = ((int) Trm[2][0]);

				// adjust as text actually inverted
				if (textShiftedMode == RIGHT) {
					xx = xx - Trm[1][0];
				}

				float yy = ((int) Trm[2][1]);
				/*
				 * When using Trm values are not precise So use values used for text positioning as these are more precise
				 */
				// float ww=((int)Trm[0][0]);
				// if(ww==0)
				// ww=((int)Trm[1][0]);

				float ww = (currentWidth * fontScale);

				float hh = (Trm[1][1]);
				if (hh == 0) hh = (Trm[0][1]);

				// correct silly figures used in T3 font on some scanned pages
				if (this.currentFontData.getFontType() == StandardFonts.TYPE3 && hh != 0 && ((int) hh) == 0
						&& this.currentFontData.FontMatrix[3] == -1) {
					hh = hh * (this.currentFontData.FontBBox[3] - this.currentFontData.FontBBox[1]);
					hh = -hh;
				}

				hh = (int) hh;

				if (ww < 0) {
					ww = -ww;
					xx = xx - ww;
				}
				if (hh < 0) {
					hh = -hh;
					yy = yy - hh;
				}

				// System.out.println("a hh="+hh);
				Rectangle fontbb = this.currentFontData.getBoundingBox();
				// System.out.println("fontbb="+fontbb);

				// @kieran - this fixes odd font
				if (fontbb.y < 0) {
					fontbb.height = fontbb.height - fontbb.y;
					fontbb.y = 0;
				}
				// /////////@old version of text code 20090727
				/**/
				// @kieran - your code assumes fy is a minor indent and that font is drawn around y=0.
				// this is not true in this case
				float fy = fontbb.y;
				if (fy == 0) // If no y set it may be embedded so we should guess a value
				fy = 100;
				if (fy < 0) fy = -fy;

				float h = 1000 + (fy);
				// Percentage of fontspace used compared to default
				h = 1000 / h;
				float fontHeight;
				switch (this.currentTextState.writingMode) {
					case PdfData.HORIZONTAL_LEFT_TO_RIGHT:
						fontHeight = (hh / h);
						yy = yy - (fontHeight - hh);
						hh = fontHeight;
						break;
					case PdfData.HORIZONTAL_RIGHT_TO_LEFT:
						break;
					case PdfData.VERTICAL_TOP_TO_BOTTOM:
						fontHeight = (ww / h);
						xx = xx - (fontHeight - ww);
						ww = fontHeight;
						break;
					case PdfData.VERTICAL_BOTTOM_TO_TOP:
						fontHeight = (ww / h);
						xx = xx - fontHeight;
						ww = fontHeight;
						break;
				}

				// Highlight area around text so increase x coord
				xx = xx - 1;
				ww = ww + 2;

				/**
				 * When highlightCoords == true Calculate the y coords for text here x coords are calculated in the method calcCoordinates(float x,
				 * float[][] rawTrm, boolean horizontal, float max_height, int fontSize, float y)
				 */
				if (this.highlightCoords) {
					if (resetCoords) {
						// x1 = xx;
						this.y2 = yy;

						// x2 = xx+ww;
						this.y1 = yy + hh;
						resetCoords = false;
					}

					// if(xx<x1)
					// x1 = xx;
					if (yy < this.y2) this.y2 = yy;
					// if((xx+ww)>x2)
					// x2 = (xx+ww);
					if ((yy + hh) > this.y1) this.y1 = (yy + hh);
				}

				// if( yy<700 && yy>600)
				// System.out.println(textData.toString()+"<==>"+new Rectangle((int)xx ,(int)yy ,(int)ww ,(int)hh));
				if (this.renderText) {
					this.textAreas.addElement(new Rectangle((int) xx, (int) yy, (int) ww, (int) hh));
					this.textDirections.addElement(this.currentTextState.writingMode);
				}
				// pdf.addToLineAreas(new Rectangle((int)xx ,(int)yy ,(int)ww ,(int)hh), currentTextState.writingMode, pageNum);
				// current.addToLineAreas(new Rectangle((int)xx ,(int)yy ,(int)ww ,(int)hh), currentTextState.writingMode);
			}
		}
		return resetCoords;
	}

	/**
	 * get unicode/escape value and convert to value
	 */
	private static int readEscapeValue(int start, int count, int base, byte[] characterStream) {

		int val = 0;

		if (base == 8) {

			// now convert to value
			int topHex, ptr = 0;

			loopoctal:

			for (int aa = 1; aa < count + 1; aa++) {

				topHex = characterStream[start + count - aa];

				// convert to number
				if (topHex >= '0' && topHex <= '7') {
					topHex = topHex - 48;
				}
				else { // ignore 'bum' values
					continue loopoctal;
				}
				val = val + (topHex << multiply8[ptr]);
				ptr++;
			}

		}
		else
			if (base == 16) {
				// now convert to value
				int topHex, ptr = 0;
				loophex:

				for (int aa = 1; aa < count + 1; aa++) {

					topHex = characterStream[start + count - aa];

					// convert to number
					if (topHex >= 'A' && topHex <= 'F') {
						topHex = topHex - 55;
					}
					else
						if (topHex >= 'a' && topHex <= 'f') {
							topHex = topHex - 87;
						}
						else
							if (topHex >= '0' && topHex <= '9') {
								topHex = topHex - 48;
							}
							else { // ignore 'bum' values
								continue loophex;
							}
					val = val + (topHex << multiply16[ptr]);
					ptr++;
				}

			}
			else { // other cases

				StringBuilder chars = new StringBuilder(10);

				for (int pointer = 0; pointer < count; pointer++)
					chars.append((char) characterStream[start + pointer]);

				val = Integer.parseInt(chars.toString(), base);

			}

		return val;
	}

	// <start-adobe>
	void storeExtractedText(String currentColor, StringBuffer current_value, int fontSize, String fontName) {

		/** save item and add in graphical elements */
		if (this.textExtracted) {

			this.pdfData.addRawTextElement((this.charSpacing * THOUSAND), this.currentTextState.writingMode,
					Fonts.createFontToken(fontName, fontSize), this.currentFontData.getCurrentFontSpaceWidth(), fontSize, this.x1, this.y1, this.x2,
					this.y2, this.moveCommand, current_value, this.textLength, currentColor, this.isXMLExtraction);
		}
	}

	// <end-adobe>

	@Override
	public boolean isTTHintingRequired() {
		return this.ttHintingRequired;
	}

	@Override
	public void reset() {
		this.multipleTJs = false;
	}

	@Override
	public void setReturnText(boolean returnText) {
		this.returnText = returnText;
	}

	@Override
	public void setParameters(boolean isPageContent, boolean renderPage, int renderMode, int extractionMode, boolean isPrinting) {

		super.setParameters(isPageContent, renderPage, renderMode, extractionMode);

		this.isPrinting = isPrinting;

		this.textExtracted = (extractionMode & PdfDecoder.TEXT) == PdfDecoder.TEXT;

		this.renderText = renderPage && (renderMode & PdfDecoder.RENDERTEXT) == PdfDecoder.RENDERTEXT;

		this.textColorExtracted = (extractionMode & PdfDecoder.TEXTCOLOR) == PdfDecoder.TEXTCOLOR;
	}

	@Override
	public void setBooleanValue(int key, boolean value) {

		switch (key) {

			case GenerateGlyphOnRender:
				this.generateGlyphOnRender = value;
				break;

			default:
				super.setBooleanValue(key, value);
		}
	}

	@Override
	public void setIntValue(int key, int value) {

		switch (key) {

			case TextPrint:
				this.textPrint = value;
				break;

			default:
				super.setIntValue(key, value);
				break;
		}
	}
}
