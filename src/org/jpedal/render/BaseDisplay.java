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
 * BaseDisplay.java
 * ---------------
 */
package org.jpedal.render;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
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
import java.awt.geom.PathIterator;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.util.Set;

import org.jpedal.PdfDecoder;
import org.jpedal.color.ColorSpaces;
import org.jpedal.color.PdfColor;
import org.jpedal.color.PdfPaint;
import org.jpedal.constants.PDFImageProcessing;
import org.jpedal.exception.PdfException;
import org.jpedal.fonts.PdfFont;
import org.jpedal.fonts.glyph.PdfGlyph;
import org.jpedal.io.ObjectStore;
import org.jpedal.objects.GraphicsState;
import org.jpedal.parser.DecoderOptions;
import org.jpedal.utils.LogWriter;
import org.jpedal.utils.Matrix;
import org.jpedal.utils.repositories.Vector_Rectangle;

public class BaseDisplay implements DynamicVectorRenderer {

	protected int type;

	/** counter to make each form unique */
	private int formNum = 0;

	boolean isType3Font = false;

	/** set flag to show if we add a background */
	protected boolean addBackground = true;

	/** holds rectangular outline to test in redraw */
	protected Vector_Rectangle areas;

	protected ObjectStore objectStoreRef;

	protected int currentItem = -1;

	// used to track end of PDF page in display
	protected int endItem = -1;

	/** raw page rotation */
	protected int pageRotation = 0;

	Area lastClip = null;

	boolean hasClips = false;

	/** shows if colours over-ridden for type3 font */
	boolean colorsLocked;

	Graphics2D g2;

	/** internal flag to control how we turn images */
	boolean optimisedTurnCode = true;

	/** use hi res images to produce better quality display */
	boolean useHiResImageForDisplay = false;

	boolean extraRot = false;

	// used by type3 fonts as identifier
	String rawKey = null;

	/** global colours if set */
	PdfPaint fillCol = null, strokeCol = null;

	public int pageNumber = 0;

	int xx = 0, yy = 0;

	public static boolean invertHighlight = false;

	boolean isPrinting;

	org.jpedal.external.ImageHandler customImageHandler = null;

	org.jpedal.external.ColorHandler customColorHandler = null;

	double cropX, cropH;

	float scaling, lastScaling;

	/** real size of pdf page */
	int w = 0, h = 0;

	/** background color */
	protected Color backgroundColor = Color.WHITE;
	protected Color textColor = null;
	protected int colorThresholdToReplace = 255;

	protected boolean changeLineArtAndText = false;

	/** allow user to control */
	public static RenderingHints userHints = null;

	@Override
	public void setInset(int x, int y) {
		this.xx = x;
		this.yy = y;
	}

	@Override
	public void setG2(Graphics2D g2) {
		this.g2 = g2;
		// If user hints has been defined use these values.
		if (userHints != null) {
			this.g2.setRenderingHints(userHints);
		}
	}

	@Override
	public void init(int width, int height, int rawRotation, Color backgroundColor) {
		this.w = width;
		this.h = height;
		this.pageRotation = rawRotation;
		this.backgroundColor = backgroundColor;
	}

	@Override
	public void paintBackground(Shape dirtyRegion) {
		if ((this.addBackground)) {
			if (this.g2 != null) {
				this.g2.setColor(this.backgroundColor);

				if (dirtyRegion == null) {
					this.g2.fill(new Rectangle(this.xx, this.yy, (int) (this.w * this.scaling), (int) (this.h * this.scaling)));
				}
				else {
					this.g2.fill(dirtyRegion);
				}
			}
		}
	}

	protected boolean checkColorThreshold(int col) {

		int r = (col) & 0xFF;
		int g = (col >> 8) & 0xFF;
		int b = (col >> 16) & 0xFF;

		if (r <= this.colorThresholdToReplace && g <= this.colorThresholdToReplace && b <= this.colorThresholdToReplace) return true;
		else return false;
	}

	void renderEmbeddedText(int text_fill_type, Object rawglyph, int glyphType, AffineTransform glyphAT, Rectangle textHighlight,
			PdfPaint strokePaint, PdfPaint fillPaint, float strokeOpacity, float fillOpacity, int lineWidth) {

		// ensure stroke only shows up
		float strokeOnlyLine = 0;
		if (text_fill_type == GraphicsState.STROKE && lineWidth >= 1.0) {
			strokeOnlyLine = this.scaling;
		}

		// get glyph to draw
		PdfGlyph glyph = (PdfGlyph) rawglyph;

		AffineTransform at = this.g2.getTransform();

		// and also as flat values so we can test below
		double[] affValues = new double[6];
		at.getMatrix(affValues);

		if (glyph != null) {

			Stroke currentStoke = this.g2.getStroke();

			if (lineWidth != 0) {
				float w = lineWidth * (float) this.g2.getTransform().getScaleX();
				if (w < 0) w = -w;
				this.g2.setStroke(new BasicStroke(w));
			}

			// set transform
			this.g2.transform(glyphAT);

			// type of draw operation to use
			Composite comp = this.g2.getComposite();

			/**
			 * Fill Text
			 */
			if ((text_fill_type & GraphicsState.FILL) == GraphicsState.FILL) {

				// If we have an alt text color, its within threshold and not an additional item, use alt color
				if (this.textColor != null && (this.currentItem == -1 || (this.endItem == -1 || this.currentItem <= this.endItem))
						&& checkColorThreshold(fillPaint.getRGB())) {
					fillPaint = new PdfColor(this.textColor.getRed(), this.textColor.getGreen(), this.textColor.getBlue());
				}

				fillPaint.setScaling(this.cropX, this.cropH, this.scaling, 0, 0);

				if (this.customColorHandler != null) {
					this.customColorHandler.setPaint(this.g2, fillPaint, this.pageNumber, this.isPrinting);
				}
				else
					if (PdfDecoder.Helper != null) {
						PdfDecoder.Helper.setPaint(this.g2, fillPaint, this.pageNumber, this.isPrinting);
					}
					else {
						this.g2.setPaint(fillPaint);
					}

				if (fillOpacity != 1f) {
					this.g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fillOpacity));
				}

				if (textHighlight != null) {
					if (invertHighlight) {
						Color color = this.g2.getColor();
						this.g2.setColor(new Color(255 - color.getRed(), 255 - color.getGreen(), 255 - color.getBlue()));
					}
					else
						if (DecoderOptions.backgroundColor != null) {
							this.g2.setColor(DecoderOptions.backgroundColor);
						}
				}

				// pass down color for drawing text
				if (glyphType == DynamicVectorRenderer.TYPE3 && !glyph.ignoreColors()) {
					glyph.setT3Colors(strokePaint, fillPaint, false);
				}

				glyph.render(GraphicsState.FILL, this.g2, this.scaling, false);

				// reset opacity
				this.g2.setComposite(comp);

			}

			/**
			 * Stroke Text (Can be fill and stroke so not in else)
			 */
			if (text_fill_type == GraphicsState.STROKE) {
				glyph.setStrokedOnly(true);
			}

			// creates shadow printing to Mac so added work around
			if (DecoderOptions.isRunningOnMac && this.isPrinting && text_fill_type == GraphicsState.FILLSTROKE) {}
			else
				if ((text_fill_type & GraphicsState.STROKE) == GraphicsState.STROKE) {

					if (strokePaint != null) {
						// If we have an alt text color, its within threshold and not an additional item, use alt color
						if (this.textColor != null && (this.currentItem == -1 || (this.endItem == -1 || this.currentItem <= this.endItem))
								&& checkColorThreshold(strokePaint.getRGB())) {
							strokePaint = new PdfColor(this.textColor.getRed(), this.textColor.getGreen(), this.textColor.getBlue());
						}
						strokePaint.setScaling(this.cropX, this.cropH, this.scaling, 0, 0);
					}

					if (this.customColorHandler != null) {
						this.customColorHandler.setPaint(this.g2, strokePaint, this.pageNumber, this.isPrinting);
					}
					else
						if (PdfDecoder.Helper != null) {
							PdfDecoder.Helper.setPaint(this.g2, strokePaint, this.pageNumber, this.isPrinting);
						}
						else {
							this.g2.setPaint(strokePaint);
						}

					if (strokeOpacity != 1f) {
						this.g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, strokeOpacity));
					}

					if (textHighlight != null) {
						if (invertHighlight) {
							Color color = this.g2.getColor();
							this.g2.setColor(new Color(255 - color.getRed(), 255 - color.getGreen(), 255 - color.getBlue()));
						}
						else
							if (DecoderOptions.backgroundColor != null) {
								this.g2.setColor(DecoderOptions.backgroundColor);
							}
					}

					try {
						glyph.render(GraphicsState.STROKE, this.g2, strokeOnlyLine, false);
					}
					catch (Exception e) {
						// tell user and log
						if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
					}

					// reset opacity
					this.g2.setComposite(comp);
				}

			// restore transform
			this.g2.setTransform(at);

			if (lineWidth != 0) {
				this.g2.setStroke(currentStoke);
			}
		}
	}

	public void renderXForm(DynamicVectorRenderer dvr, float nonstrokeAlpha) {

		Shape clip = null;
		if (this.g2 != null) {
			clip = this.g2.getClip();
		}

		Rectangle area = dvr.getOccupiedArea();
		int fx = area.x;
		int fw = area.width;
		int fy = area.y;
		int fh = area.height;

		// check x,y offsets and factor in
		if (fx < 0) {
			fx = 0;
		}

		if (fy > 0) {
			fh = fh + fy;
		}

		// ignore offpage
		if (fw <= 0 || fh <= 0) {
			return;
		}

		BufferedImage formImg = new BufferedImage(fw, fh, BufferedImage.TYPE_INT_ARGB);

		Graphics2D formG2 = formImg.createGraphics();

		if (this.g2 == null) {
			formG2.scale(1, -1);
			formG2.translate(0, -fh);
		}

		formG2.translate(-fx, 0);

		dvr.setG2(formG2);
		dvr.paint(null, null, null);

		// if odd matrix can break text and already applied for drawing so now remove
		if (this.g2 != null) {
			this.g2.setClip(null);
		}

		GraphicsState gs = new GraphicsState();
		gs.CTM[0][0] = fw;
		gs.CTM[1][0] = 0.0f;
		gs.CTM[2][0] = 0f;
		gs.CTM[0][1] = 0.0f;
		gs.CTM[1][1] = fh;
		gs.CTM[2][1] = 0f;
		gs.CTM[0][2] = 0.0f;
		gs.CTM[1][2] = 0.0f;
		gs.CTM[2][2] = 1.0f;

		if (this.g2 != null) {
			this.g2.translate(fx, 0);
			renderImage(new AffineTransform(), formImg, nonstrokeAlpha, gs, area.x, area.y, PDFImageProcessing.IMAGE_INVERTED);
			this.g2.translate(-fx, 0);
			// put it back
			this.g2.setClip(clip);
		}
		else {
			gs.CTM[2][0] = area.x;
			gs.CTM[2][1] = area.y - fh + area.height;
			drawImage(this.pageNumber, formImg, gs, false, "xf" + this.formNum, 0, -1);

			this.formNum++;
		}
	}

	final void renderShape(Shape defaultClip, int fillType, PdfPaint strokeCol, PdfPaint fillCol, Stroke shapeStroke, Shape currentShape,
			float strokeOpacity, float fillOpacity) {

		boolean clipChanged = false;

		Shape clip = this.g2.getClip();

		Composite comp = this.g2.getComposite();

		// stroke and fill (do fill first so we don't overwrite Stroke)
		if (fillType == GraphicsState.FILL || fillType == GraphicsState.FILLSTROKE) {

			if (fillCol.getRGB() != -1)
			// If we have an alt text color, are changing line art as well, its within threshold and not an additional item, use alt color
			if (this.changeLineArtAndText && this.textColor != null && !fillCol.isPattern()
					&& (this.currentItem == -1 || (this.endItem == -1 || this.currentItem <= this.endItem)) && checkColorThreshold(fillCol.getRGB())) {
				fillCol = new PdfColor(this.textColor.getRed(), this.textColor.getGreen(), this.textColor.getBlue());
			}

			fillCol.setScaling(this.cropX, this.cropH, this.scaling, 0, 0);

			if (this.customColorHandler != null) {
				this.customColorHandler.setPaint(this.g2, fillCol, this.pageNumber, this.isPrinting);
			}
			else
				if (PdfDecoder.Helper != null) {
					PdfDecoder.Helper.setPaint(this.g2, fillCol, this.pageNumber, this.isPrinting);
				}
				else {
					this.g2.setPaint(fillCol);
				}

			if (fillOpacity != 1f) {
				this.g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fillOpacity));
			}

			try {
				// thin lines do not appear unless we use fillRect
				double iw = currentShape.getBounds2D().getWidth();
				double ih = currentShape.getBounds2D().getHeight();

				if ((ih == 0d || iw == 0d) && ((BasicStroke) this.g2.getStroke()).getLineWidth() <= 1.0f) {
					this.g2.fillRect(currentShape.getBounds().x, currentShape.getBounds().y, currentShape.getBounds().width,
							currentShape.getBounds().height);
				}
				else this.g2.fill(currentShape);

			}
			catch (Exception e) {
				if (LogWriter.isOutput()) LogWriter.writeLog("Exception " + e + " filling shape");
			}

			this.g2.setComposite(comp);
		}

		if ((fillType == GraphicsState.STROKE) || (fillType == GraphicsState.FILLSTROKE)) {

			// set values for drawing the shape
			Stroke currentStroke = this.g2.getStroke();

			// fix for using large width on point to draw line
			if (currentShape.getBounds2D().getWidth() < 1.0f && ((BasicStroke) shapeStroke).getLineWidth() > 10) {
				this.g2.setStroke(new BasicStroke(1));
			}
			else {
				this.g2.setStroke(shapeStroke);
			}

			// If we have an alt text color, are changing line art, its within threshold and not an additional item, use alt color
			if (this.changeLineArtAndText && this.textColor != null && !strokeCol.isPattern()
					&& (this.currentItem == -1 || (this.endItem == -1 || this.currentItem <= this.endItem))
					&& checkColorThreshold(strokeCol.getRGB())) {
				strokeCol = new PdfColor(this.textColor.getRed(), this.textColor.getGreen(), this.textColor.getBlue());
			}

			strokeCol.setScaling(this.cropX, this.cropH, this.scaling, 0, 0);

			if (this.customColorHandler != null) {
				this.customColorHandler.setPaint(this.g2, strokeCol, this.pageNumber, this.isPrinting);
			}
			else
				if (PdfDecoder.Helper != null) {
					PdfDecoder.Helper.setPaint(this.g2, strokeCol, this.pageNumber, this.isPrinting);
				}
				else {
					this.g2.setPaint(strokeCol);
				}

			if (strokeOpacity != 1f) {
				this.g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, strokeOpacity));
			}

			// breaks printing so disabled there
			if (!this.isPrinting && clip != null && (clip.getBounds2D().getHeight() < 1 || clip.getBounds2D().getWidth() < 1)) {
				this.g2.setClip(defaultClip); // use null or visible screen area
				clipChanged = true;
			}

			this.g2.draw(currentShape);
			this.g2.setStroke(currentStroke);
			this.g2.setComposite(comp);
		}

		if (clipChanged) this.g2.setClip(clip);
	}

	final void renderImage(AffineTransform imageAf, BufferedImage image, float alpha, GraphicsState currentGraphicsState, float x, float y,
			int optionsApplied) {

		boolean renderDirect = (currentGraphicsState != null);

		if (image == null) {
			return;
		}

		int w = image.getWidth();
		int h = image.getHeight();

		// plot image (needs to be flipped as well as co-ords upside down)
		// graphics less than 1 get swallowed if flipped
		AffineTransform upside_down = new AffineTransform();

		boolean applyTransform = false;

		float CTM[][] = new float[3][3];
		if (currentGraphicsState != null) {
			CTM = currentGraphicsState.CTM;
		}

		// special case - ignore rotation
		if (CTM[0][0] < 0 && CTM[1][1] < 0 && CTM[1][0] > -2 && CTM[1][0] < 0 && CTM[0][1] > 0 && CTM[0][1] < 10) {
			CTM[0][1] = 0;
			CTM[1][0] = 0;
		}

		AffineTransform before = this.g2.getTransform();

		boolean invertInAff = false;

		float dx = 0, dy = 0;

		/**
		 * setup for printing
		 */
		if (renderDirect || this.useHiResImageForDisplay) {

			if (renderDirect) {

				upside_down = null;

				// Turn image around if needed (ie JPEG not yet turned)
				if ((optionsApplied & PDFImageProcessing.IMAGE_INVERTED) != PDFImageProcessing.IMAGE_INVERTED) {

					if (!this.optimisedTurnCode) {
						image = RenderUtils.invertImage(CTM, image);
					}
					else {

						if ((CTM[0][1] < 0 && CTM[1][0] > 0) && (CTM[0][0] * CTM[1][1] == 0)) {

							upside_down = new AffineTransform(CTM[0][0] / w, CTM[0][1] / w, -CTM[1][0] / h, CTM[1][1] / h, CTM[2][0] + CTM[1][0],
									CTM[2][1]);

						}
						else
							if ((CTM[0][1] < 0 || CTM[1][0] < 0)) {

								float[][] flip2 = { { 1f / w, 0, 0 }, { 0, -1f / h, 0 }, { 0, 1f / h, 1 } };
								float[][] rot = { { CTM[0][0], CTM[0][1], 0 }, { CTM[1][0], CTM[1][1], 0 }, { 0, 0, 1 } };

								flip2 = Matrix.multiply(flip2, rot);
								upside_down = new AffineTransform(flip2[0][0], flip2[0][1], flip2[1][0], flip2[1][1], flip2[2][0], flip2[2][1]);

								dx = CTM[2][0] - image.getHeight() * flip2[1][0];
								dy = CTM[2][1];

								// special case
								// if (CTM[0][0] < 0 && CTM[1][0] < 0 && CTM[0][1] > 0 && CTM[1][1] < 0) {
								// System.out.println("a");
								// } else if (CTM[1][1] != 0) {
								dy = dy + CTM[1][1];
								// System.out.println("b");
								// }

							}
							else
								if ((CTM[0][0] * CTM[0][1] == 0 && CTM[1][1] * CTM[1][0] == 0) && (CTM[0][1] > 0 && CTM[1][0] > 0)) {
									float[][] flip2 = { { -1f / w, 0, 0 }, { 0, 1f / h, 0 }, { 1f / w, 0, 1 } };
									float[][] rot = { { CTM[0][0], CTM[0][1], 0 }, { CTM[1][0], CTM[1][1], 0 }, { 0, 0, 1 } };

									flip2 = Matrix.multiply(flip2, rot);
									upside_down = new AffineTransform(flip2[0][0], flip2[1][0], flip2[0][1], flip2[1][1], flip2[2][0], flip2[2][1]);

									dx = CTM[2][0] - image.getHeight() * flip2[0][1];
									dy = CTM[2][1];

								}
								else
									if (CTM[1][1] != 0) {
										invertInAff = true;
									}
					}
				}

				if (upside_down == null) {
					upside_down = new AffineTransform(CTM[0][0] / w, CTM[0][1] / w, CTM[1][0] / h, CTM[1][1] / h, CTM[2][0], CTM[2][1]);
				}
			}
			else {
				upside_down = imageAf;

				invertInAff = ((optionsApplied & PDFImageProcessing.TURN_ON_DRAW) == PDFImageProcessing.TURN_ON_DRAW);
			}

			applyTransform = true;

		}

		Composite c = this.g2.getComposite();

		Shape clip = this.g2.getClip();
		boolean isClipChanged = false;

		if (alpha != 1.0f) this.g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

		/**
		 * color type3 glyphs if not black
		 */
		if (this.isType3Font && this.fillCol != null) {

			int[] maskCol = new int[4];
			int foreground = this.fillCol.getRGB();
			maskCol[0] = ((foreground >> 16) & 0xFF);
			maskCol[1] = ((foreground >> 8) & 0xFF);
			maskCol[2] = ((foreground) & 0xFF);
			maskCol[3] = 255;

			if (maskCol[0] == 0 && maskCol[1] == 0 && maskCol[2] == 0) {
				// System.out.println("black");
			}
			else {

				// hack for white text in printing (see Host account statement from open.pdf)
				if (image.getType() == 10 && maskCol[0] > 250 && maskCol[1] > 250 && maskCol[2] > 250) {
					return;
				}

				BufferedImage img = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());

				Raster src = image.getRaster();
				WritableRaster dest = img.getRaster();
				int[] values = new int[4];
				for (int yy = 0; yy < image.getHeight(); yy++) {
					for (int xx = 0; xx < image.getWidth(); xx++) {

						// get raw color data
						src.getPixel(xx, yy, values);

						// if not transparent, fill with color
						if (values[3] > 2) {
							dest.setPixel(xx, yy, maskCol);
						}
					}
				}
				image = img;
			}
		}

		if (renderDirect || this.useHiResImageForDisplay) {

			try {

				if (this.optimisedTurnCode
						&& (invertInAff && (optionsApplied & PDFImageProcessing.IMAGE_INVERTED) != PDFImageProcessing.IMAGE_INVERTED)) {

					double[] values = new double[6];
					upside_down.getMatrix(values);

					dx = (float) (values[4] + (values[1] * image.getWidth()));
					dy = (float) (values[5] + (image.getHeight() * values[3]));

					// correct rotation case
					if (values[0] > 0 && values[1] > 0 && values[2] > 0 && values[3] < 0) {
						values[2] = -values[2];
					}

					values[3] = -values[3];

					values[4] = 0;
					values[5] = 0;

					upside_down = new AffineTransform(values);

				}

				// allow user to over-ride
				boolean useCustomRenderer = this.customImageHandler != null;

				if (useCustomRenderer) {
					useCustomRenderer = this.customImageHandler.drawImageOnscreen(image, optionsApplied, upside_down, null, this.g2, renderDirect,
							this.objectStoreRef, this.isPrinting);
				}

				// exit if done
				if (useCustomRenderer) {
					this.g2.setComposite(c);
					return;
				}

				this.g2.translate(dx, dy);

				// hack to make bw
				if (this.customColorHandler != null) {
					BufferedImage newImage = this.customColorHandler.processImage(image, this.pageNumber, this.isPrinting);
					if (newImage != null) {
						image = newImage;
					}
				}
				else
					if (PdfDecoder.Helper != null) {
						BufferedImage newImage = PdfDecoder.Helper.processImage(image, this.pageNumber, this.isPrinting);
						if (newImage != null) {
							image = newImage;
						}
					}

				Shape g2clip = this.g2.getClip();
				boolean isClipReset = false;

				// hack to fix clipping issues due to sub-pixels
				if (g2clip != null) {

					double cy = this.g2.getClip().getBounds2D().getY();
					double ch = this.g2.getClip().getBounds2D().getHeight();
					double diff = image.getHeight() - ch;
					if (diff < 0) {
						diff = -diff;
					}

					if (diff > 0 && diff < 1 && cy < 0 && image.getHeight() > 1 && image.getHeight() < 10) {

						int count = 0;
						PathIterator i = this.g2.getClip().getPathIterator(null);

						while (!i.isDone() && count < 6) { // see if rectangle or complex clip
							i.next();
							count++;
						}

						if (count < 6) {
							double cx = this.g2.getClip().getBounds2D().getX();
							double cw = this.g2.getClip().getBounds2D().getWidth();

							this.g2.setClip(new Rectangle((int) cx, (int) cy, (int) cw, (int) ch));

							isClipReset = false;
						}
					}
				}

				// If image is gray scale and small use quicker scale method
				// Possibly in future use different scale methods based on size to improve speed?
				// 23032012 - disabled by Mark as breaks with new print code
				if (1 == 2 && image.getType() == BufferedImage.TYPE_BYTE_GRAY && image.getHeight() < 43 && !this.isPrinting) {

					// This is how we fix the gray scale image
					AffineTransformOp op = new AffineTransformOp(upside_down, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
					this.g2.drawImage(image, op, 0, 0);

				}
				else {

					// Draw image as normal
					this.g2.drawImage(image, upside_down, null);

				}

				if (isClipReset) {
					this.g2.setClip(g2clip);
				}

			}
			catch (Exception e) {
				// tell user and log
				if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
			}
		}
		else {

			try {

				if (applyTransform) {
					AffineTransformOp invert = new AffineTransformOp(upside_down, ColorSpaces.hints);
					image = invert.filter(image, null);
				}

				this.g2.translate(x, y);

				if (this.optimisedTurnCode && (optionsApplied & PDFImageProcessing.TURN_ON_DRAW) == PDFImageProcessing.TURN_ON_DRAW) {

					AffineTransform flip2;

					float[] flip = new float[] { 1f, 0f, 0f, -1f, 0f, image.getHeight() };
					AffineTransform flip3;
					if (this.pageRotation == 0) {

						flip2 = new AffineTransform(flip);

					}
					else
						if (this.pageRotation == 90) {

							flip2 = new AffineTransform();

							if (this.extraRot) {
								flip2.rotate(Math.PI, 0, 0);
							}
							else {
								flip2.rotate(Math.PI / 2, 0, 0);
							}
							flip2.translate(-image.getWidth(), -image.getHeight());

							flip = new float[] { -1f, 0f, 0f, 1f, image.getWidth(), 0 };// (float)image.getHeight()};
							flip3 = new AffineTransform(flip);

							flip2.concatenate(flip3);

						}
						else
							if (this.pageRotation == 180) {

								flip2 = new AffineTransform();
								if (this.extraRot) {
									flip2.rotate(Math.PI, 0, 0);
								}
								flip2.translate(-image.getWidth(), -image.getHeight());

								flip = new float[] { -1f, 0f, 0f, 1f, image.getWidth(), 0 };// (float)image.getHeight()};

								flip3 = new AffineTransform(flip);

								flip2.concatenate(flip3);

							}
							else {

								flip2 = new AffineTransform();
								if (this.extraRot) {
									flip2.rotate(Math.PI, 0, 0);
								}
								else {
									flip2.rotate(Math.PI / 2 + Math.PI, 0, 0);
								}
								flip2.translate(-image.getWidth(), -image.getHeight());
								flip = new float[] { -1f, 0f, 0f, 1f, image.getWidth(), 0 };
								flip3 = new AffineTransform(flip);
								flip2.concatenate(flip3);

							}

					this.g2.drawImage(image, flip2, null);

				}
				else {
					this.g2.drawImage(image, 0, 0, null);
				}

				this.g2.translate(-x, -y);

			}
			catch (Exception e) {
				// tell user and log
				if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
			}
		}

		this.g2.setTransform(before);

		if (isClipChanged && (DecoderOptions.isRunningOnMac) && (clip != null)) {
			this.g2.setClip(clip);
		}

		this.g2.setComposite(c);
	}

	final void renderText(float x, float y, int type, Area transformedGlyph2, Rectangle textHighlight, PdfPaint strokePaint, PdfPaint textFillCol,
			float strokeOpacity, float fillOpacity) {

		Paint currentCol = this.g2.getPaint();

		// type of draw operation to use
		Composite comp = this.g2.getComposite();

		if ((type & GraphicsState.FILL) == GraphicsState.FILL) {

			if (textFillCol != null) {
				// If we have an alt text color, its within threshold and not an additional item, use alt color
				if (this.textColor != null && (this.currentItem == -1 || (this.endItem == -1 || this.currentItem <= this.endItem))
						&& checkColorThreshold(textFillCol.getRGB())) {
					textFillCol = new PdfColor(this.textColor.getRed(), this.textColor.getGreen(), this.textColor.getBlue());
				}
				textFillCol.setScaling(this.cropX, this.cropH, this.scaling, x, y);
			}

			if (this.customColorHandler != null) {
				this.customColorHandler.setPaint(this.g2, textFillCol, this.pageNumber, this.isPrinting);
			}
			else
				if (PdfDecoder.Helper != null) {
					PdfDecoder.Helper.setPaint(this.g2, textFillCol, this.pageNumber, this.isPrinting);
				}
				else {
					this.g2.setPaint(textFillCol);
				}

			if (fillOpacity != 1f) {
				this.g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fillOpacity));
			}

			if (textHighlight != null) {
				if (invertHighlight) {
					Color col = this.g2.getColor();
					this.g2.setColor(new Color(255 - col.getRed(), 255 - col.getGreen(), 255 - col.getBlue()));
				}
				else
					if (DecoderOptions.backgroundColor != null) {
						this.g2.setColor(DecoderOptions.backgroundColor);
					}
			}

			this.g2.fill(transformedGlyph2);

			// reset opacity
			this.g2.setComposite(comp);

		}

		if ((type & GraphicsState.STROKE) == GraphicsState.STROKE) {

			if (strokePaint != null) {
				// If we have an alt text color, its within threshold and not an additional item, use alt color
				if (this.textColor != null && (this.currentItem == -1 || (this.endItem == -1 || this.currentItem <= this.endItem))
						&& checkColorThreshold(strokePaint.getRGB())) {
					strokePaint = new PdfColor(this.textColor.getRed(), this.textColor.getGreen(), this.textColor.getBlue());
				}
				strokePaint.setScaling(this.cropX + x, this.cropH + y, this.scaling, x, y);
			}

			if (this.customColorHandler != null) {
				this.customColorHandler.setPaint(this.g2, strokePaint, this.pageNumber, this.isPrinting);
			}
			else
				if (PdfDecoder.Helper != null) {
					PdfDecoder.Helper.setPaint(this.g2, strokePaint, this.pageNumber, this.isPrinting);
				}
				else {
					this.g2.setPaint(strokePaint);
				}

			if (strokeOpacity != 1f) {
				this.g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, strokeOpacity));
			}

			if (textHighlight != null) {
				if (invertHighlight) {
					Color col = this.g2.getColor();
					this.g2.setColor(new Color(255 - col.getRed(), 255 - col.getGreen(), 255 - col.getBlue()));
				}
				else
					if (DecoderOptions.backgroundColor != null) {
						this.g2.setColor(DecoderOptions.backgroundColor);
					}
			}

			// factor in scaling
			float lineWidth = (float) (1f / this.g2.getTransform().getScaleX());

			if (lineWidth < 0) {
				lineWidth = -lineWidth;
			}

			this.g2.setStroke(new BasicStroke(lineWidth));

			if (lineWidth < 0.1f) {
				this.g2.draw(transformedGlyph2);
			}
			else {
				this.g2.fill(transformedGlyph2);
			}

			// reset opacity
			this.g2.setComposite(comp);
		}

		this.g2.setPaint(currentCol);
	}

	// used internally - please do not use
	@Override
	public ObjectStore getObjectStore() {
		return this.objectStoreRef;
	}

	/**
	 * Screen drawing using hi res images and not down-sampled images but may be slower and use more memory
	 */
	@Override
	public void setHiResImageForDisplayMode(boolean useHiResImageForDisplay) {
		this.useHiResImageForDisplay = useHiResImageForDisplay;
	}

	@Override
	public void setScalingValues(double cropX, double cropH, float scaling) {

		this.cropX = cropX;
		this.cropH = cropH;
		this.scaling = scaling;
	}

	@Override
	public void setCustomImageHandler(org.jpedal.external.ImageHandler customImageHandler) {
		this.customImageHandler = customImageHandler;
	}

	@Override
	public void setCustomColorHandler(org.jpedal.external.ColorHandler colorController) {
		this.customColorHandler = colorController;
	}

	// //////////////////NOT used except by screen/////////////////////////////////
	/**
	 * reset on colorspace change to ensure cached data up to data
	 */
	@Override
	public void resetOnColorspaceChange() {
		// To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void drawFontBounds(Rectangle newfontBB) {}

	/**
	 * store af info
	 */
	@Override
	public void drawAffine(double[] afValues) {
		// To change body of implemented methods use File | Settings | File Templates.
	}

	/**
	 * store af info
	 */
	@Override
	public void drawFontSize(int fontSize) {
		// To change body of implemented methods use File | Settings | File Templates.
	}

	/**
	 * store line width info
	 */
	@Override
	public void setLineWidth(int lineWidth) {
		// To change body of implemented methods use File | Settings | File Templates.
	}

	/**
	 * stop screen bein cleared on repaint - used by Canoo code
	 * <p/>
	 * NOT PART OF API and subject to change (DO NOT USE)
	 */
	@Override
	public void stopClearOnNextRepaint(boolean flag) {
		// To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public boolean hasObjectsBehind(float[][] CTM) {

		boolean hasObject = false;

		int x = (int) CTM[2][0];
		int y = (int) CTM[2][1];
		int w = (int) CTM[0][0];
		if (w == 0) {
			w = (int) CTM[0][1];
		}
		int h = (int) CTM[1][1];
		if (h == 0) {
			h = (int) CTM[1][0];
		}

		// if h or w are negative, reverse values
		// as intersects and contains can't cope with it
		if (h < 0) {
			y = y + h;
			h = y - h;
		}

		if (w < 0) {
			x = x + w;
			w = x - w;
		}

		Rectangle[] areas = this.areas.get();
		int count = areas.length;

		int rx, ry, rw, rh;

		for (int i = 0; i < count; i++) {
			if (areas[i] != null) {

				// find if overlap and exit once found
				rx = areas[i].x;
				ry = areas[i].y;
				rw = areas[i].width;
				rh = areas[i].height;

				boolean xOverlap = valueInRange(x, rx, rx + rw) || valueInRange(rx, x, x + w);
				boolean yOverlap = xOverlap && valueInRange(y, ry, ry + rh) || valueInRange(ry, y, y + rh);

				if (xOverlap && yOverlap) { // first match
					i = count;
					hasObject = true;
				}

			}
		}

		return hasObject;
	}

	private static boolean valueInRange(int value, int min, int max) {
		return (value >= min && value <= max);
	}

	/**
	 * operations to do once page done
	 */
	@Override
	public void flagDecodingFinished() {
		// To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void flagImageDeleted(int i) {
		// To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void setOCR(boolean isOCR) {
		// To change body of implemented methods use File | Settings | File Templates.
	}

	/**
	 * turn object into byte[] so we can move across this way should be much faster than the stadard Java serialise.
	 * <p/>
	 * NOT PART OF API and subject to change (DO NOT USE)
	 * 
	 * @throws java.io.IOException
	 */
	@Override
	public byte[] serializeToByteArray(Set fontsAlreadyOnClient) throws IOException {
		return new byte[0];
	}

	/**
	 * for font if we are generatign glyph on first render
	 */
	@Override
	public void checkFontSaved(Object glyph, String name, PdfFont currentFontData) {
		// To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public Rectangle getArea(int i) {
		return null; // To change body of implemented methods use File | Settings | File Templates.
	}

	/**
	 * return number of image in display queue or -1 if none
	 * 
	 * @return number of image in display queue or -1 if none (but currently hardcoded to 0)
	 */
	@Override
	public int isInsideImage(int x, int y) {
		return 0; // To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void saveImage(int id, String des, String type) {
		// To change body of implemented methods use File | Settings | File Templates.
	}

	/**
	 * HTML, or Image, or Display
	 */
	@Override
	public int getType() {
		return this.type;
	}

	/**
	 * return number of image in display queue or -1 if none
	 * 
	 * @return number of image in display queue or -1 if none (but currently hardcoded to 0)
	 */
	@Override
	public int getObjectUnderneath(int x, int y) {
		return 0; // To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void setneedsVerticalInvert(boolean b) {
		// To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void setneedsHorizontalInvert(boolean b) {
		// To change body of implemented methods use File | Settings | File Templates.
	}

	/**
	 * just for printing
	 * 
	 */
	@Override
	public void stopG2HintSetting(boolean isSet) {
		// To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void setPrintPage(int currentPrintPage) {
		// To change body of implemented methods use File | Settings | File Templates.
	}

	public void drawColor(PdfPaint currentCol, int type) {
		// To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void drawShape(Shape currentShape, GraphicsState currentGraphicsState, int cmd) {
		// throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void drawCustom(Object value) {
		// throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void drawEmbeddedText(float[][] Trm, int fontSize, PdfGlyph embeddedGlyph, Object javaGlyph, int type, GraphicsState gs,
			AffineTransform at, String glyf, PdfFont currentFontData, float glyfWidth) {
		// throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Rectangle paint(Rectangle[] highlights, AffineTransform viewScaling, Rectangle userAnnot) {
		// throw new UnsupportedOperationException("Not supported yet.");
		return null;
	}

	@Override
	public void setMessageFrame(Container frame) {
		// throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void dispose() {}

	@Override
	public int drawImage(int pageNumber, BufferedImage image, GraphicsState currentGraphicsState, boolean alreadyCached, String name,
			int optionsApplied, int previousUse) {
		return -1;
	}

	@Override
	public void drawXForm(DynamicVectorRenderer dvr, GraphicsState gs) {}

	@Override
	public void drawFillColor(PdfPaint currentCol) {}

	@Override
	public void drawAdditionalObjectsOverPage(int[] type, Color[] colors, Object[] obj) throws PdfException {}

	@Override
	public void flushAdditionalObjOnPage() {}

	@Override
	public void setOptimsePainting(boolean optimsePainting) {
		// throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void flush() {
		// throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void drawText(float[][] Trm, String text, GraphicsState currentGraphicsState, float x, float y, Font javaFont) {
		// throw new UnsupportedOperationException("Not supported yet.");
	}

	public Rectangle getCombinedAreas(Rectangle targetRectangle, boolean justText) {
		return null;// throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Rectangle getOccupiedArea() {
		return null;// throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void setGraphicsState(int fillType, float value) {
		// throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void drawStrokeColor(Paint currentCol) {
		// throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void drawTR(int value) {
		// throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void drawStroke(Stroke current) {
		// throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void drawClip(GraphicsState currentGraphicsState, Shape defaultClip, boolean alwaysDraw) {
		// throw new UnsupportedOperationException("Not supported yet.");
	}

	/**
	 * used by some custom version of DynamicVectorRenderer
	 * 
	 * @param output_dir
	 * @param pageAsString
	 */
	@Override
	public void setOutputDir(String output_dir, String fileName, String pageAsString) {}

	/**
	 * used by some custom version of DynamicVectorRenderer
	 */
	@Override
	public void writeCustom(int key, Object value) {}

	/** allow tracking of specific commands **/
	@Override
	public void flagCommand(int commandID, int tokenNumber) {
	}

	@Override
	public void setTag(int formTag, String value) {
		// used by HTML to set tags like form to custom values
	}

	@Override
	public void setValue(int key, int i) {
		switch (key) {
			case ALT_BACKGROUND_COLOR:
				this.backgroundColor = new Color(i);
				break;
			case ALT_FOREGROUND_COLOR:
				this.textColor = new Color(i);
				break;
			case FOREGROUND_INCLUDE_LINEART:
				if (i > 0) this.changeLineArtAndText = true;
				else this.changeLineArtAndText = false;
				break;
			case COLOR_REPLACEMENT_THRESHOLD:
				this.colorThresholdToReplace = i;
				break;
		}
	}

	@Override
	public void setValue(int key, String[] S) {
	}

	@Override
	public int getValue(int key) {
		// used by HTML to get font handing mode, etc
		// this is the unused 'dummy' default implementation required for other modes as in Interface
		return -1;
	}

	/**
	 * used by Pattern code internally (do not use)
	 * 
	 * @return null (hardcoded)
	 */
	@Override
	public BufferedImage getSingleImagePattern() {
		return null; // To change body of implemented methods use File | Settings | File Templates.
	}

	/**
	 * only used in HTMLDisplay at present
	 * 
	 * @param includeJSFontResizingCode
	 * @param b
	 */
	@Override
	public void setBooleanValue(int includeJSFontResizingCode, boolean b) {
	}

	/** used by JavaFX and HTML5 conversion to override scaling */
	@Override
	public boolean isScalingControlledByUser() {
		return false;
	}

	/** used by HTML to retai nimage quality */
	@Override
	public boolean avoidDownSamplingImage() {
		return false;
	}

	/**
	 * allow user to read
	 */
	@Override
	public boolean getBooleanValue(int encloseContentInDiv) {
		return false;
	}

	/**
	 * page scaling used by HTML code only
	 * 
	 * @return scaling value
	 */
	@Override
	public float getScaling() {
		return this.scaling;
	}

	/**
	 * only used in HTML5 and SVG conversion
	 * 
	 * @param baseFontName
	 * @param s
	 * @param potentialWidth
	 */
	@Override
	public void saveAdvanceWidth(String baseFontName, String s, int potentialWidth) {
	}

	public static int isRectangle(Shape bounds) {

		int count = 0;
		PathIterator i = bounds.getPathIterator(null);

		while (!i.isDone() && count < 8) { // see if rectangle or complex clip
			i.next();
			count++;
		}

		return count;
	}
	
    @Override
    //used by HTML/SVG mode only
    public Object getObjectValue(final int id) {
        return null;
    }
	
    public static boolean isHTMLorSVG(final DynamicVectorRenderer dvr) {
        return dvr.getType() == DynamicVectorRenderer.CREATE_HTML || dvr.getType() == DynamicVectorRenderer.CREATE_SVG || dvr.getType() == DynamicVectorRenderer.CREATE_EPOS;
    }
}
