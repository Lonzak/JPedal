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
 * ExampleImageDrawOnScreenHandler.java
 * ---------------
 */
package org.jpedal.examples.handlers;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.File;

import javax.media.jai.JAI;
import javax.media.jai.KernelJAI;
import javax.media.jai.TiledImage;
import javax.media.jai.operator.CropDescriptor;

import org.jpedal.color.ColorSpaces;
import org.jpedal.constants.PDFImageProcessing;
import org.jpedal.io.JAIHelper;
import org.jpedal.io.ObjectStore;
import org.jpedal.objects.GraphicsState;
import org.jpedal.objects.raw.PdfObject;
import org.jpedal.utils.LogWriter;

/**
 * example code to plugin external image handler. Code to enable commented out in Viewer
 */
public class ExampleImageDrawOnScreenHandler implements org.jpedal.external.ImageHandler {

	// tell JPedal if it ignores its own Image code or not
	@Override
	public boolean alwaysIgnoreGenericHandler() {
		return false; // To change body of implemented methods use File | Settings | File Templates.
	}// pass in raw data for image handling - if valid image returned it will be used.
		// if alwaysIgnoreGenericHandler() is true JPedal code always ignored. If false, JPedal code used if null

	@Override
	public BufferedImage processImageData(GraphicsState gs, PdfObject XObject) {
		return null; // To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public boolean imageHasBeenScaled() {
		return false; // To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public boolean drawImageOnscreen(BufferedImage image, int optionsApplied, AffineTransform upside_down, String currentImageFile, Graphics2D g2,
			boolean renderDirect, ObjectStore objectStoreRef, boolean isPrinting) {

		// this is the draw code from DynamicVectorRenderer as at 11th June 2007

		double[] values = new double[6];
		upside_down.getMatrix(values);

		boolean isSlightlyRotated = (values[0] * values[1] != 0) || (values[2] * values[3] != 0);

		// accelerate large bw images non-rotated
		// accelerate large bw images non-rotated (use for all images for moment)
		if (isSlightlyRotated || image.getWidth() < 800 || renderDirect) { // image.getType()!=12 || CTM[0][0]<0 || CTM[1][1]<0 || CTM[1][0]<0 ||
																			// CTM[0][1]<0)
			g2.drawImage(image, upside_down, null);
		}
		else { // speedup large straightforward images

			double dy = 0, dx = 0;

			// if already turned, tweak transform
			if (optionsApplied != PDFImageProcessing.NOTHING) {

				// int count=values.length;
				// for(int jj=0;jj<count;jj++)
				// System.out.println(jj+"=="+values[jj]);

				// System.out.println(image.getWidth());
				// System.out.println(image.getHeight());
				// System.out.println(values[4]*image.getHeight()/image.getWidth());

				// alter array to account for rotation elsewhere
				if ((optionsApplied & PDFImageProcessing.IMAGE_ROTATED) == PDFImageProcessing.IMAGE_ROTATED) {

					if (values[0] > 0 && values[3] < 0 && (optionsApplied & PDFImageProcessing.IMAGE_INVERTED) == PDFImageProcessing.IMAGE_INVERTED) {
						double newWidth = (values[0] * image.getWidth());
						double newHeight = -((values[3] * image.getHeight()));
						dy = values[5] - newHeight;
						values[5] = newHeight;

						// allow for rounding error in scaling
						if (newWidth - (int) newWidth > 0.5) {
							dx = dx - 1;
						}

					}
					else
						if (values[0] < 0 && values[3] > 0) {
							double tmp1 = values[0];
							// double tmp3=values[2];
							values[0] = values[3];
							values[3] = tmp1;
							values[4] = 0;
							values[5] = (int) (values[4] * image.getHeight() / image.getWidth());

						}
				}
				else
					if (values[0] > 0 && values[3] > 0 && (optionsApplied & PDFImageProcessing.IMAGE_INVERTED) == PDFImageProcessing.IMAGE_INVERTED) {

						dy = values[5];

						double tmp1 = values[0];
						// double tmp3=values[2];
						values[0] = values[3];
						values[3] = tmp1;
						values[4] = 0;
						values[5] = (int) (values[4] * image.getHeight() / image.getWidth());
					}
					else {}

				upside_down = new AffineTransform(values);
			}

			boolean imageProcessed = false;

			if (JAIHelper.isJAIused()) {

				// assume worked and set if fails
				// imageProcessed=true;

				/**
				 * try in memory first - does not seem to be a big hit if it fails
				 * 
				 * try{
				 * 
				 * image = (javax.media.jai.JAI.create("affine", image, upside_down, new
				 * javax.media.jai.InterpolationBicubic(1))).getAsBufferedImage();
				 * 
				 * imageProcessed=false; }catch(Exception ee){ imageProcessed=false; ee.printStackTrace(); }catch(Error err){ imageProcessed=false;
				 * 
				 * } /
				 **/

				if (!imageProcessed && currentImageFile != null) { // try it via tiled image if failed (thanks to Cesssna for starting code)

					// assume worked and set if fails
					imageProcessed = true;

					try {

						// The minium tile size of an image
						final Dimension tileSize = new Dimension(512, 512);

						RenderedImage ri;

						com.sun.media.jai.codec.SeekableStream s = new com.sun.media.jai.codec.FileSeekableStream(new File(
								objectStoreRef.getFileForCachedImage(currentImageFile)));

						com.sun.media.jai.codec.TIFFDecodeParam param = null;

						com.sun.media.jai.codec.ImageDecoder dec = com.sun.media.jai.codec.ImageCodec.createImageDecoder("tiff", s, param);

						// Which of the multiple images in the TIFF file do we want to load
						// 0 refers to the first, 1 to the second and so on.
						int imageToLoad = 0;

						ri = new javax.media.jai.NullOpImage(dec.decodeAsRenderedImage(imageToLoad), null, null, javax.media.jai.OpImage.OP_IO_BOUND);
						// Create color model and color space references
						ColorModel cm;// = ri.getColorModel();
						// ColorSpace cs = cm.getColorSpace();

						RenderingHints hints;

						// Convert base image to enforce a minimum tile layout to reduce
						// memory resource utilization and enhance performance.
						// Skip conversion if tile already meets minimum tile requirements
						// Tile image first to improve downstream processing
						if ((ri.getTileWidth() * ri.getTileHeight()) > (tileSize.width * tileSize.height)) {
							cm = ri.getColorModel();
							SampleModel sm = ri.getSampleModel().createCompatibleSampleModel(tileSize.width, tileSize.height);
							javax.media.jai.ImageLayout layout = new javax.media.jai.ImageLayout(0, 0, tileSize.width, tileSize.height, sm, cm);
							hints = new RenderingHints(javax.media.jai.JAI.KEY_IMAGE_LAYOUT, layout);

							// Convert base image using AbsoluteDescriptor
							ri = javax.media.jai.operator.AbsoluteDescriptor.create(ri, hints);
						}

						// @jason - 3 scaling options and bicubic takes a number parameter
						// For best rendering performance you should use a buffered image
						image = (javax.media.jai.JAI.create("affine", ri, upside_down, new javax.media.jai.InterpolationBicubic(1)))
								.getAsBufferedImage();
						// image = (javax.media.jai.JAI.create("affine", ri, upside_down, new
						// javax.media.jai.InterpolationBicubic2(2))).getAsBufferedImage();
						// image = (javax.media.jai.JAI.create("affine", ri, upside_down, new
						// javax.media.jai.InterpolationBicubic(1))).getAsBufferedImage();

						// image=getAffineTransform(ri, upside_down, hints,quality);

					}
					catch (Exception ee) {
						imageProcessed = false;
						ee.printStackTrace();
					}
					catch (Error err) {
						imageProcessed = false;

					}
				}

				if (!imageProcessed) LogWriter.writeLog("Unable to use JAI for image inversion");
			}
			else imageProcessed = true;

			// fall back on lower quality standard op
			if (!imageProcessed) {

				imageProcessed = true;

				try {
					AffineTransformOp invert = new AffineTransformOp(upside_down, ColorSpaces.hints);

					image = invert.filter(image, null);
				}
				catch (Exception ee) {
					imageProcessed = false;
					ee.printStackTrace();
				}
				catch (Error err) {
					imageProcessed = false;

				}
			}

			if (imageProcessed) {

				Shape rawClip = null;

				if (isPrinting && dy == 0) { // adjust to fit
					double[] affValues = new double[6];
					g2.getTransform().getMatrix(affValues);

					// for(int i=0;i<6;i++)
					// System.out.println(i+"="+affValues[i]);

					dx = affValues[4] / affValues[0];
					if (dx > 0) dx = -dx;

					dy = affValues[5] / affValues[3];

					if (dy > 0) dy = -dy;

					dy = -(dy + image.getHeight());

				}

				// stop part pixels causing black lines
				if (dy != 0) {
					rawClip = g2.getClip();

					int xDiff;
					double xScale = g2.getTransform().getScaleX();
					if (xScale < 1) xDiff = (int) (1 / xScale);
					else xDiff = (int) (xScale + 0.5d);
					int yDiff;
					double yScale = g2.getTransform().getScaleY();
					if (yScale < 1) yDiff = (int) (1 / yScale);
					else yDiff = (int) (yScale + 0.5d);

					g2.clipRect((int) dx, (int) (dy + 1.5), image.getWidth() - xDiff, image.getHeight() - yDiff);

				}
				g2.drawImage(image, (int) dx, (int) dy, null);

				// put it back
				if (rawClip != null) g2.setClip(rawClip);
			}
			else g2.drawImage(image, upside_down, null);

		}

		return true; // To change body of implemented methods use File | Settings | File Templates.
	}

	// Code for Affine transform
	public static synchronized BufferedImage getAffineTransform(RenderedImage ri, AffineTransform src2me, RenderingHints hints, float quality) {

		// @jason - value was undefined (I have assumed its final size of image)
		// tile size of an image
		double[] values = new double[6];
		src2me.getMatrix(values);
		final Dimension preferredTileSize = new Dimension((int) (values[0] * ri.getWidth()), (int) (values[3] * ri.getHeight()));

		// Get translated tile grid offsets
		Point2D pt = new Point2D.Double(ri.getTileGridXOffset(), ri.getTileGridYOffset());
		pt = src2me.transform(pt, null);

		// Get translated image bounds
		Rectangle $bounds = new Rectangle((int) pt.getX(), (int) pt.getY(), ri.getWidth(), ri.getHeight());
		Rectangle bounds = src2me.createTransformedShape($bounds).getBounds();

		System.out.println(">>" + ri);
		System.out.println(">>" + bounds);

		// Create color and sample modle
		ColorModel cm = ri.getColorModel();
		SampleModel sm = cm.createCompatibleSampleModel(ri.getTileWidth(), ri.getTileHeight());

		// Create Destination image
		TiledImage img = new TiledImage((int) bounds.getMinX(), (int) bounds.getMinY(), (int) bounds.getWidth(), (int) bounds.getHeight(),
				(int) pt.getX(), (int) pt.getY(), sm, cm);

		// Create blur kernel
		final KernelJAI kernel = createBlurKernel((float) src2me.getScaleX(), (float) src2me.getScaleY(), quality);
		final int kw = kernel.getWidth();
		final int kh = kernel.getHeight();

		AffineTransform at = (AffineTransform) src2me.clone();
		at.translate((pt.getX()), (pt.getY()));

		double $numXTiles = (ri.getWidth() / preferredTileSize.getWidth());
		double $numYTiles = (ri.getHeight() / preferredTileSize.getHeight());

		if ($numXTiles % 2 > 0) ++$numXTiles;
		if ($numYTiles % 2 > 0) ++$numYTiles;
		int numXTiles = (int) $numXTiles;
		int numYTiles = (int) $numYTiles;

		// For best performance & memory transform each tile
		for (int x = 0; x < numXTiles; x++) {
			for (int y = 0; y < numYTiles; y++) {
				int $x = (int) (x * preferredTileSize.getWidth());
				int $y = (int) (y * preferredTileSize.getWidth());
				int $w = (int) (preferredTileSize.getWidth());
				int $h = (int) (preferredTileSize.getHeight());

				if ($x < 0) $x = 0;
				if ($y < 0) $y = 0;
				if ($x + $w > ri.getWidth()) $w = ri.getWidth() - $x;
				if ($y + $h > ri.getHeight()) $h = ri.getHeight() - $y;

				// Create tile bounds
				Rectangle tileBounds = new Rectangle($x, $y, $w, $h);

				// Fix Kernel
				$x = tileBounds.x - kw;
				$y = tileBounds.y - kh;
				$w = tileBounds.width + (kw * 2);
				$h = tileBounds.height + (kh * 2);

				if ($x < 0) $x = 0;
				if ($y < 0) $y = 0;
				if ($x + $w > ri.getWidth()) $w = ri.getWidth() - $x;
				if ($y + $h > ri.getHeight()) $h = ri.getHeight() - $y;

				// Adjust tile bounds for convolve operation
				Rectangle $tileBounds = new Rectangle($x, $y, $w, $h);

				// if(!srcBounds.intersects($tileBounds))
				// continue;

				// Create artificial tile
				WritableRaster wr;
				try {
					wr = (WritableRaster) ri.getData($tileBounds);
				}
				catch (Exception e) {
					wr = null;
				}

				if (wr == null) {
					System.out.println(">>>");
					continue;
				}

				wr = wr.createWritableTranslatedChild(0, 0);

				// Create a buffered image for convolve operation
				BufferedImage buf = new BufferedImage(ri.getColorModel(), wr, ri.getColorModel().isAlphaPremultiplied(), null);

				// Create the convolved image
				RenderedImage $ri = buf;
				if (quality > 0) {
					$ri = JAI.create("convolve", buf, kernel);

					// Crop convolve edges from the convolved image
					if (((wr.getWidth() - (kw * 2)) > 0) && ((wr.getHeight() - (kh * 2)) > 0)) $ri = CropDescriptor.create($ri, (float) kw,
							(float) kh, (float) (wr.getWidth() - (kw * 2)), (float) wr.getHeight() - (kh * 2), hints);
				}

				// Draw Rendered Image
				AffineTransform $at = (AffineTransform) at.clone();
				$at.translate(tileBounds.getX(), tileBounds.getY());

				Graphics2D g2d = img.createGraphics();
				g2d.drawRenderedImage($ri, $at);
				g2d.dispose();
			}
		}

		return img.getAsBufferedImage();
	} // End Method

	public static KernelJAI createBlurKernel(float scaleX, float scaleY, float quality) {
		// Normalize transform
		scaleX = Math.abs(scaleX);
		scaleY = Math.abs(scaleY);

		int sizeX = 1 + Math.round(quality / scaleX);
		int sizeY = 1 + Math.round(quality / scaleY);

		// Temporary fix - grid size of
		// 3,3;4,4;5,5 cause a convolution
		// error >> image is black
		if (sizeX == 4 && sizeY == 4) {
			sizeX = 3;
			sizeY = 3;
		}
		if (sizeX == 5 && sizeY == 5) {
			sizeX = 3;
			sizeY = 3;
		}
		if (sizeX == 6 && sizeY == 6) {
			sizeX = 7;
			sizeY = 7;
		}

		float[] data = new float[sizeX * sizeY];
		float factor = 1F / data.length;

		for (int i = 0; i < data.length; i++) {
			data[i] = factor;
		}

		return new KernelJAI(sizeX, sizeY, data);
	} // End Method

}
