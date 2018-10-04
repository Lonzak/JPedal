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
 * Type3.java
 * ---------------
 */
package org.jpedal.fonts;

import java.util.Map;

import org.jpedal.exception.PdfException;
import org.jpedal.fonts.glyph.T3Glyph;
import org.jpedal.fonts.glyph.T3Glyphs;
import org.jpedal.fonts.glyph.T3Size;
import org.jpedal.io.ObjectStore;
import org.jpedal.io.PdfObjectReader;
import org.jpedal.objects.GraphicsState;
import org.jpedal.objects.raw.PdfDictionary;
import org.jpedal.objects.raw.PdfKeyPairsIterator;
import org.jpedal.objects.raw.PdfObject;
import org.jpedal.parser.T3StreamDecoder;
import org.jpedal.parser.ValueTypes;
import org.jpedal.render.T3Display;
import org.jpedal.render.T3Renderer;
import org.jpedal.utils.LogWriter;

/**
 * handles type1 specifics
 */
public class Type3 extends PdfFont {

	private static final long serialVersionUID = 3597911525618769050L;

	/** handle onto GS so we can read color */
	private GraphicsState currentGraphicsState = new GraphicsState();

	/** allow us to track if user is printing this font - needed for type3 */
	private boolean isPrinting;

	/**
	 * get handles onto Reader so we can access the file
	 * 
	 * @param current_pdf_file
	 *            - handle to PDF file
	 **/
	public Type3(PdfObjectReader current_pdf_file, boolean isPrinting) {

		this.glyphs = new T3Glyphs();

		this.isPrinting = isPrinting;

		init(current_pdf_file);
	}

	/** read in a font and its details from the pdf file */
	@Override
	final public void createFont(PdfObject pdfObject, String fontID, boolean renderPage, ObjectStore objectStore, Map substitutedFonts)
			throws Exception {

		this.fontTypes = StandardFonts.TYPE3;

		// generic setup
		init(fontID, renderPage);

		/**
		 * get FontDescriptor object - if present contains metrics on glyphs
		 */
		PdfObject pdfFontDescriptor = pdfObject.getDictionary(PdfDictionary.FontDescriptor);

		// get any dimensions if present (note FBoundBox if in pdfObject not Descriptor)
		setBoundsAndMatrix(pdfObject);

		setName(pdfObject, fontID);
		setEncoding(pdfObject, pdfFontDescriptor);

		readWidths(pdfObject, false);

		readEmbeddedFont(pdfObject, objectStore);

		// make sure a font set
		if (renderPage) setFont(getBaseFontName(), 1);
	}

	private void readEmbeddedFont(PdfObject pdfObject, ObjectStore objectStore) {

		final boolean hires = true;

		int key, otherKey;

		PdfObject CharProcs = pdfObject.getDictionary(PdfDictionary.CharProcs);

		// handle type 3 charProcs and store for later lookup
		if (CharProcs != null) {

			T3StreamDecoder glyphDecoder = new T3StreamDecoder(this.currentPdfFile, hires, this.isPrinting);
			glyphDecoder.setParameters(false, true, 7, 0);

			glyphDecoder.setObjectValue(ValueTypes.ObjectStore, objectStore);

			PdfObject Resources = pdfObject.getDictionary(PdfDictionary.Resources);
			if (Resources != null) {
				try {
					glyphDecoder.readResources(Resources, false);
				}
				catch (PdfException e) {
					// tell user and log
					if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
				}
			}

			/**
			 * read all the key pairs for Glyphs
			 */
			PdfKeyPairsIterator keyPairs = CharProcs.getKeyPairsIterator();

			String glyphKey, pKey;
			PdfObject glyphObj;

			while (keyPairs.hasMorePairs()) {

				glyphKey = keyPairs.getNextKeyAsString();
				glyphObj = keyPairs.getNextValueAsDictionary();

				Object diffValue = null;
				if (this.diffLookup != null) {
					pKey = StandardFonts.convertNumberToGlyph(glyphKey, this.containsHexNumbers, this.allNumbers);
					diffValue = this.diffLookup.get(pKey);
				}

				// decode and store in array
				if (glyphObj != null && this.renderPage) {

					// decode and create graphic of glyph
					T3Renderer glyphDisplay = new T3Display(0, false, 20, objectStore);

					glyphDisplay.setHiResImageForDisplayMode(hires);
					glyphDisplay.setType3Glyph(glyphKey);

					try {
						glyphDecoder.setObjectValue(ValueTypes.DynamicVectorRenderer, glyphDisplay);
						glyphDecoder.setDefaultColors(this.currentGraphicsState.getNonstrokeColor(), this.currentGraphicsState.getNonstrokeColor());
						int renderX, renderY;

						// if size is 1 we need to scale up so we can see
						int factor = 1;
						double[] fontMatrix = pdfObject.getDoubleArray(PdfDictionary.FontMatrix);
						if (fontMatrix != null && fontMatrix[0] == 1 && (fontMatrix[3] == 1 || fontMatrix[3] == -1)) factor = 10;

						GraphicsState gs = new GraphicsState(0, 0);
						gs.CTM = new float[][] { { factor, 0, 0 }, { 0, factor, 0 }, { 0, 0, 1 } };

						T3Size t3 = glyphDecoder.decodePageContent(glyphObj, gs);

						renderX = t3.x;
						renderY = t3.y;

						// allow for rotated on page in case swapped
						if (renderX == 0 && renderY != 0) {
							renderX = t3.y;
							renderY = t3.x;
						}

						T3Glyph glyph = new T3Glyph(glyphDisplay, renderX, renderY, glyphDecoder.ignoreColors, glyphKey);
						glyph.setScaling(1f / factor);

						otherKey = -1;
						if (diffValue != null) {
							key = (Integer) diffValue;

							if (keyPairs.isNextKeyANumber()) otherKey = keyPairs.getNextKeyAsNumber();
						}
						else {
							key = keyPairs.getNextKeyAsNumber();
						}

						this.glyphs.setT3Glyph(key, otherKey, glyph);

					}
					catch (Exception e) {
						e.printStackTrace(System.out);

						if (LogWriter.isOutput()) LogWriter.writeLog("Exception " + e + " is Type3 font code");
					}
				}

				// roll on
				keyPairs.nextPair();
			}
			this.isFontEmbedded = true;
		}
	}
}
