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
 * PatternColorSpace.java
 * ---------------
 */
package org.jpedal.color;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import org.jpedal.exception.PdfException;
import org.jpedal.io.ColorSpaceConvertor;
import org.jpedal.io.ObjectStore;
import org.jpedal.io.PdfObjectReader;
import org.jpedal.objects.GraphicsState;
import org.jpedal.objects.raw.PatternObject;
import org.jpedal.objects.raw.PdfDictionary;
import org.jpedal.objects.raw.PdfObject;
import org.jpedal.parser.PdfStreamDecoder;
import org.jpedal.parser.ValueTypes;
import org.jpedal.render.DynamicVectorRenderer;
import org.jpedal.render.T3Renderer;
import org.jpedal.utils.LogWriter;
import org.jpedal.utils.Matrix;

import com.idrsolutions.pdf.color.shading.ShadedPaint;

/**
 * handle Pattern ColorSpace (there is also a shading class)
 */
public class PatternColorSpace extends GenericColorSpace {

	private static final long serialVersionUID = 9108293275924704003L;

	// lookup tables for stored previous values
	private Map cachedPaints = new HashMap();

	// local copy so we can access File data
	private PdfObjectReader currentPdfFile = null;

	private boolean colorsReversed;

	/**
	 * Just initialises variables
	 * 
	 * @param currentPdfFile
	 */
	public PatternColorSpace(PdfObjectReader currentPdfFile) {

		this.value = ColorSpaces.Pattern;

		this.currentPdfFile = currentPdfFile;

		// default value for color
		this.currentColor = new PdfColor(1.0f, 1.0f, 1.0f);
	}

	/**
	 * convert color value to pattern
	 */
	@Override
	public void setColor(String[] value_loc, int operandCount) {

		PatternObject PatternObj = (PatternObject) this.patterns.get(value_loc[0]);

		// if already setup just reuse
		String ref = PatternObj.getObjectRefAsString();
		if (ref != null && this.cachedPaints.containsKey(ref)) {
			this.currentColor = (PdfPaint) this.cachedPaints.get(ref);

			return;
		}

		/**
		 * decode Pattern on first use
		 */

		// ensure read
		this.currentPdfFile.checkResolved(PatternObj);

		// lookup table
		byte[] streamData = this.currentPdfFile.readStream(PatternObj, true, true, true, false, false,
				PatternObj.getCacheName(this.currentPdfFile.getObjectReader()));

		// type of Pattern (shading or tiling)
		final int shadingType = PatternObj.getInt(PdfDictionary.PatternType);

		// get optional matrix values
		float[][] matrix = null;
		float[] inputs = PatternObj.getFloatArray(PdfDictionary.Matrix);

		if (inputs != null) {

			if (shadingType == 1) {
				float[][] Nmatrix = { { inputs[0], inputs[1], 0f }, { inputs[2], inputs[3], 0f }, { 0f, 0f, 1f } };

				if (inputs[5] < 0) { // check against adobe/PP_download and adobe/source.pdf
					inputs[4] = 0;
					inputs[5] = 0;
				}
				matrix = Nmatrix;
			}
			else {
				float[][] Nmatrix = { { inputs[0], inputs[1], 0f }, { inputs[2], inputs[3], 0f }, { inputs[4], inputs[5], 1f } };

				if (Nmatrix[2][0] < 0) this.colorsReversed = true;
				else this.colorsReversed = false;

				matrix = Matrix.multiply(Nmatrix, this.CTM);
			}
		}

		/**
		 * setup appropriate type
		 */
		if (shadingType == 1) // tiling
		this.currentColor = setupTiling(PatternObj, inputs, matrix, streamData);
		else
			if (shadingType == 2) // shading
			this.currentColor = setupShading(PatternObj, matrix);
	}

	/**
	 * handle pattern made of constantly repeated pattern. If it is rotated, it gets rather messy.
	 */
	private PdfPaint setupTiling(PdfObject PatternObj, float[] inputs, float[][] matrix, byte[] streamData) {

		boolean needsAdjusting = inputs != null && inputs[5] == this.pageHeight && inputs[4] == 0;
		this.inputs = inputs;

		PdfPaint paint = null;

		boolean tesslateOntoImage = true;

		BufferedImage img = null;

		AffineTransform imageScale = null;

		// create copy of matrix unchanged;
		float[][] rawMatrix = null;
		if (matrix != null) {
			rawMatrix = new float[3][3];
			for (int y = 0; y < 3; y++) {
				for (int x = 0; x < 3; x++) {
					rawMatrix[x][y] = matrix[x][y];
				}
			}
		}
		/**
		 * ignore silly rotations and inputs
		 */
		if (matrix != null && matrix[0][0] != 0 && matrix[0][1] < 0.001 && matrix[0][1] > -0.001) matrix[0][1] = 0;
		if (matrix != null && matrix[1][1] != 0 && matrix[1][0] < 0.001 && matrix[1][0] > -0.001) matrix[1][0] = 0;

		if (inputs != null) {
			for (int aa = 0; aa < 6; aa++) {
				if (inputs[aa] != 0 && inputs[aa] < 0.001 && inputs[aa] > -0.001) inputs[aa] = 0;
			}
		}

		if (matrix != null && matrix[0][0] < 0 && matrix[1][1] < 0) {

			matrix[0][0] = -matrix[0][0];
			matrix[1][1] = -matrix[1][1];

			if (inputs != null) {
				inputs[0] = -inputs[0];
				inputs[3] = -inputs[3];
			}
		}

		// flag to track rotation which needs custom handling
		boolean isRotated = false, isUpsideDown = false, isSideways = false;

		/**
		 * work out if upside down and manipulate matrix so will appear on tile
		 */
		if (matrix != null) {

			isRotated = matrix[1][0] != 0 && matrix[0][1] != 0 && matrix[0][0] != 0 && matrix[1][1] != 0;

			// ignore slight rotations
			if (isRotated && matrix[0][0] != 0 && matrix[0][0] < 0.001 && matrix[1][1] != 0 && matrix[1][1] < 0.001) {
				isRotated = false;
				matrix[0][0] = -matrix[0][1];
				matrix[1][1] = -matrix[1][0];

				matrix[1][0] = 0;
				matrix[0][1] = 0;
			}

			if (isRotated && matrix[0][0] > 0 && matrix[0][1] < 0 && matrix[1][0] > 0 && matrix[1][1] > 0) {}
			else
				if (isRotated && matrix[0][0] < 0 && matrix[0][1] < 0 && matrix[1][0] < 0 && matrix[1][1] > 0) {

					matrix[0][0] = -matrix[0][0];
					matrix[1][0] = -matrix[1][0];

					isSideways = true;
				}

			isUpsideDown = (matrix[1][1] < 0 || matrix[0][1] < 0);

			// allow for this special case
			if (matrix[0][0] > 0 && matrix[0][1] < 0 && matrix[1][0] > 0 && matrix[1][1] > 0) isUpsideDown = false;

			// breaks Scooby page so ignore
			if (matrix[0][0] > 0.1f && (isRotated || isUpsideDown)) tesslateOntoImage = false;

			// used by rotation code
			if (isUpsideDown && matrix[0][1] > 0 && matrix[1][0] > 0) isUpsideDown = false;

		}

		/**
		 * get values for pattern for PDF object
		 */
		int rawXStep = (int) PatternObj.getFloatNumber(PdfDictionary.XStep);
		int rawYStep = (int) PatternObj.getFloatNumber(PdfDictionary.YStep);
		int XStep = rawXStep;
		int YStep = rawYStep;

		// ensure a value
		if (XStep == 0) XStep = 1;
		if (YStep == 0) YStep = 1;

		// ensure positive
		if (XStep < 0) XStep = -XStep;
		if (YStep < 0) YStep = -YStep;

		float dx = 0, dy = 0;

		// position of tile if inputs not null and less than 1
		int input_dxx = 0, input_dyy = 0;

		/**
		 * adjust matrix to suit
		 **/
		if (matrix != null) {

			// allow for upside down
			if (matrix[1][1] < 0) matrix[2][1] = YStep;

			// needed for reporttype file
			if (matrix[1][0] != 0.0) matrix[2][1] = -matrix[1][0];

		}

		/**
		 * convert stream into an DynamicVector object we can then draw onto screen or tile
		 */
		ObjectStore localStore = new ObjectStore(null);
		DynamicVectorRenderer glyphDisplay = decodePatternContent(PatternObj, matrix, streamData, localStore);

		// get the image
		BufferedImage image = glyphDisplay.getSingleImagePattern();

		/**
		 * unashamed hack for very odd file Investigate when more examples/time (customers-june2011/10664.pdf)
		 */
		// float[] BBox=PatternObj.getFloatArray(PdfDictionary.BBox);
		if (inputs != null && inputs[0] < 1.5 && inputs[3] < 1.5 && inputs[1] == 0 && inputs[2] == 0
				&& (image != null || (rawXStep == -32768 && rawYStep == -32768))) {// || (BBox!=null && rawXStep==BBox[2] && rawYStep==BBox[3])){
			// if((rawXStep==-32768 && rawYStep==-32768)){//|| (BBox!=null && rawXStep==BBox[2] && rawYStep==BBox[3])){

			// turn upside down
			AffineTransform image_at2 = new AffineTransform();
			image_at2.scale(1, -1);
			image_at2.translate(0, -image.getHeight());
			AffineTransformOp invert3 = new AffineTransformOp(image_at2, null);
			image = invert3.filter(image, null);

			// ensure ARGB to avoid issues if we draw to BufferedImage
			if (image.getType() != BufferedImage.TYPE_INT_ARGB) image = ColorSpaceConvertor.convertToARGB(image);

			// return as pattern to use
			return new PdfTexturePaint(image, new Rectangle((int) (inputs[4] - image.getWidth()), (int) (inputs[5] - image.getHeight()),
					image.getWidth(), image.getHeight()));

		}

		/**
		 * if image is generated larger than slot we draw it into we will lose definition. To avoid this, we draw at full size and scale only when
		 * drawn onto page
		 */
		// workout unscaled tile size
		float rawWidth = 0, rawHeight = 0;
		boolean isDownSampled = false;

		if (matrix != null) {
			rawWidth = matrix[0][0];
			if (rawWidth == 0) rawWidth = matrix[0][1];
			if (rawWidth < 0) rawWidth = -rawWidth;
			rawHeight = matrix[1][1];
			if (rawHeight == 0) rawHeight = matrix[1][0];
			if (rawHeight < 0) rawHeight = -rawHeight;
		}

		if (matrix != null) { // this scales/rotated image onto tile - Java cannot handle this so we do it onto an unrotated tile

			// image scaled up to fit so create large tile and then draw upscaled version onto it
			if (inputs != null && inputs[0] > 1 && inputs[3] > 1 && !isRotated) {

				img = new BufferedImage((int) (XStep * rawWidth), (int) (YStep * rawHeight), BufferedImage.TYPE_INT_ARGB);
				Graphics2D g2 = img.createGraphics();
				g2.setClip(new Rectangle(0, 0, img.getWidth(), img.getHeight()));
				glyphDisplay.setG2(g2);
				glyphDisplay
						.paint(null, new AffineTransform(matrix[0][0], matrix[0][1], matrix[1][0], matrix[1][1], inputs[4], -inputs[5] / 2), null);

			}
			else { // buffer onto unrotated tile

				BufferedImage tileImg = null; // tile for image as faster to draw ONCE and then replicate image

				/**
				 * workout dx,dy and ensure positive and scaled
				 */
				dx = matrix[0][0];
				dy = matrix[1][1];

				if (dx == 0) dx = matrix[0][1];
				if (dx < 0) dx = -dx;

				if (dy == 0) dy = matrix[1][0];
				if (dy < 0) dy = -dy;

				dx = dx * XStep;
				dy = dy * YStep;

				/**
				 * workout size required for image
				 */
				int imgW = XStep, imgH = YStep; // default values for tile size

				if (!isRotated) {
					/**
					 * special cases
					 */
					if (isUpsideDown) {

						int xCount = (int) (XStep / dx), yCount = (int) (YStep / dy);

						if (xCount > 0 && yCount > 0) {
							imgW = (int) ((xCount + 1) * dx);
							imgH = (int) ((yCount + 1) * dy);

							XStep = imgW;
							YStep = imgH;
						}
					}
					else
						if (inputs != null && inputs[0] > 0 && inputs[0] < 1 && inputs[3] > 0 && inputs[3] < 1) {
							imgW = (int) (dx);
							imgH = (int) (dy);

							if (imgH == 0) imgH = 1;

							// workout offsets
							input_dxx = (int) inputs[4];
							if (input_dxx > XStep) {
								while (input_dxx > 0) {
									input_dxx = input_dxx - XStep;

									if (input_dxx == 0) break;
								}

								input_dxx = input_dxx / 2;
							}

							// workout offsets
							input_dyy = (int) inputs[5];
							if (input_dyy > imgH) {
								while (input_dyy > 0)
									input_dyy = input_dyy - imgH;
							}
						}

					if (isDownSampled) {
						img = new BufferedImage((int) (rawWidth + .5f), (int) (rawHeight + .5f), BufferedImage.TYPE_INT_ARGB);
						imageScale = AffineTransform.getScaleInstance(XStep / rawWidth, YStep / rawHeight);
					}
					else {

						// a few file generate values less than 1 so code defensively for this case
						if (imgW < 1 || imgH < 1) img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
						else img = new BufferedImage(imgW, imgH, BufferedImage.TYPE_INT_ARGB);
					}

					Graphics2D g2 = img.createGraphics();
					AffineTransform defaultAf = g2.getTransform();

					g2.setClip(new Rectangle(0, 0, img.getWidth(), img.getHeight()));

					/**
					 * allow for tile not draw from 0,0
					 */
					int startX = 0;
					Rectangle actualTileRect = glyphDisplay.getOccupiedArea().getBounds();

					int tileW, tileH;
					int dxx = 0, dyy = 0;
					if (actualTileRect.x < 0) {
						tileW = actualTileRect.width - actualTileRect.x;
						dxx = actualTileRect.x;
					}
					else tileW = actualTileRect.width + actualTileRect.x;

					if (actualTileRect.y < 0) {
						tileH = actualTileRect.height - actualTileRect.y;
						dyy = actualTileRect.y;
					}
					else tileH = actualTileRect.height + actualTileRect.y;

					if (tileH == 0) tileH = 1;

					if (tesslateOntoImage) {

						// a few file generate values less than 1 so code defensively for this case
						if (tileW < 1 || tileH < 1) tileImg = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
						else tileImg = new BufferedImage(tileW, tileH, BufferedImage.TYPE_INT_ARGB);

						Graphics2D tileG2 = tileImg.createGraphics();
						tileG2.translate(-dxx, -dyy);
						glyphDisplay.setG2(tileG2);
						glyphDisplay.paint(null, null, null);
					}

					int rectX = actualTileRect.x;
					if (rectX < 0 && !tesslateOntoImage) startX = (int) (-rectX * matrix[0][0]);

					// if tile is smaller than Xstep,Ystep, tesselate to fill
					float max = YStep;
					if (tesslateOntoImage) max = YStep + (tileImg.getHeight() * 2);

					// fix for odd pattern on phonbingo
					int min = 0;
					if (matrix[1][1] < 0) min = (int) (-max / 2);

					// adjustment for pattern on customers-june2011/Verizon PowerPoint Template.pdf
					if (needsAdjusting && matrix[0][0] > 0 && matrix[1][1] < 0) min = min + (this.pageHeight - this.pageWidth);

					for (float y = min; y < max; y = y + dy) {
						for (float x = startX; x < XStep; x = x + dx) {

							if (isUpsideDown) g2.translate(x, -y);
							else g2.translate(x, y);

							if (tesslateOntoImage) {
								AffineTransform tileAff = new AffineTransform();
								ColorSpaceConvertor.drawImage(g2, tileImg, tileAff, null);
							}
							else {
								glyphDisplay.setG2(g2);
								glyphDisplay.paint(null, imageScale, null);
							}
							g2.setTransform(defaultAf);
						}
					}
				}
			}
		}
		else { // no matrix value so just draw onto tile

			if (isDownSampled) {
				img = new BufferedImage((int) (rawWidth + .5f), (int) (rawHeight + .5f), BufferedImage.TYPE_INT_ARGB); // .5 to allow for rounding of
																														// decimal
				imageScale = AffineTransform.getScaleInstance(XStep / rawWidth, YStep / rawHeight);
			}
			else img = new BufferedImage(XStep, YStep, BufferedImage.TYPE_INT_ARGB);

			Graphics2D g2 = img.createGraphics();

			glyphDisplay.setG2(g2);
			glyphDisplay.paint(null, null, null);

			// flip now once for speed if upside down
			if (isUpsideDown && img.getHeight() > 1) {
				AffineTransform flip = new AffineTransform();
				flip.translate(0, img.getHeight());
				flip.scale(1, -1);
				AffineTransformOp invert = new AffineTransformOp(flip, ColorSpaces.hints);
				img = invert.filter(img, null);
			}
		}

		// now delete any stored images used in content
		localStore.flush();

		/**
		 * create paint using image or decoded content if rotated
		 */
		if (img != null) paint = new PdfTexturePaint(img, new Rectangle(input_dxx, input_dyy, img.getWidth(), img.getHeight()));

		if (isRotated) {

			/**
			 * useful debug code to isolate just one pattern System.out.println(PatternObj.getObjectRefAsString());
			 * if(!PatternObj.getObjectRefAsString().equals("19 0 R")){ paint=new PdfColor(255,0,0);
			 * cachedPaints.put(PatternObj.getObjectRefAsString(),paint); return paint; } /
			 **/
			paint = new RotatedTexturePaint(isSideways, rawMatrix, PatternObj, tesslateOntoImage, glyphDisplay, matrix, XStep, YStep, dx, dy,
					imageScale);
		}

		if (paint != null) this.cachedPaints.put(PatternObj.getObjectRefAsString(), paint);

		return paint;
	}

	private DynamicVectorRenderer decodePatternContent(PdfObject PatternObj, float[][] matrix, byte[] streamData, ObjectStore localStore) {

		PdfObject Resources = PatternObj.getDictionary(PdfDictionary.Resources);

		// decode and create graphic of glyph

		PdfStreamDecoder glyphDecoder = new PdfStreamDecoder(this.currentPdfFile);
		glyphDecoder.setParameters(false, true, 7, 0);

		glyphDecoder.setIntValue(ValueTypes.StreamType, ValueTypes.PATTERN);

		glyphDecoder.setObjectValue(ValueTypes.ObjectStore, localStore);

		// glyphDecoder.setMultiplier(multiplyer);

		// T3Renderer glyphDisplay=new T3Display(0,false,20,localStore);
		T3Renderer glyphDisplay = new PatternDisplay(0, false, 20, localStore);
		glyphDisplay.setOptimisedRotation(false);

		try {
			glyphDecoder.setObjectValue(ValueTypes.DynamicVectorRenderer, glyphDisplay);

			/** read the resources for the page */
			if (Resources != null) {
				glyphDecoder.readResources(Resources, true);
			}
			glyphDecoder.setDefaultColors(this.gs.getStrokeColor(), this.gs.getNonstrokeColor());

			/**
			 * setup matrix so scales correctly
			 **/
			GraphicsState currentGraphicsState = new GraphicsState(0, 0);
			// multiply to get new CTM
			if (matrix != null) currentGraphicsState.CTM = matrix;

			glyphDecoder.decodePageContent(currentGraphicsState, streamData);

		}
		catch (PdfException e) {
			// tell user and log
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
		}

		// flush as image now created
		return glyphDisplay;
	}

	private PdfPaint setupShading(PdfObject PatternObj, float[][] matrix) {

		/**
		 * get the shading object
		 */

		PdfObject Shading = PatternObj.getDictionary(PdfDictionary.Shading);

		/**
		 * work out colorspace
		 */
		PdfObject ColorSpace = Shading.getDictionary(PdfDictionary.ColorSpace);

		// convert colorspace and get details
		GenericColorSpace newColorSpace = ColorspaceFactory.getColorSpaceInstance(this.currentPdfFile, ColorSpace);

		// use alternate as preference if CMYK
		if (newColorSpace.getID() == ColorSpaces.ICC && ColorSpace.getParameterConstant(PdfDictionary.Alternate) == ColorSpaces.DeviceCMYK) newColorSpace = new DeviceCMYKColorSpace();

		return new ShadedPaint(Shading, isPrinting,newColorSpace, currentPdfFile,matrix,colorsReversed, CTM);
	}
}
