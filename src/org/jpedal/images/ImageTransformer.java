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
 * ImageTransformer.java
 * ---------------
 */
package org.jpedal.images;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;

import org.jpedal.color.ColorSpaces;
import org.jpedal.io.ColorSpaceConvertor;
import org.jpedal.objects.GraphicsState;
import org.jpedal.utils.LogWriter;

/**
 * class to shrink and clip an extracted image On reparse just calculates co-ords
 */
public class ImageTransformer {

	/** holds the actual image */
	private BufferedImage current_image;

	/** matrices used in transformation */
	private float[][] Trm, CTM;

	/** image co-ords */
	private int i_x = 0, i_y = 0, i_w = 0, i_h = 0;

	/**
	 * pass in image information and apply transformation matrix to image
	 */
	public ImageTransformer(GraphicsState current_graphics_state, BufferedImage new_image, boolean scaleImage) {

		// save global values
		this.current_image = new_image;
		int w, h;

		w = this.current_image.getWidth(); // raw width
		h = this.current_image.getHeight(); // raw height

		this.CTM = current_graphics_state.CTM; // local copy of CTM

		// build transformation matrix by hand to avoid errors in rounding
		this.Trm = new float[3][3];
		this.Trm[0][0] = (this.CTM[0][0] / w);
		this.Trm[0][1] = -(this.CTM[0][1] / w);
		this.Trm[0][2] = 0;
		this.Trm[1][0] = -(this.CTM[1][0] / h);
		this.Trm[1][1] = (this.CTM[1][1] / h);
		this.Trm[1][2] = 0;
		this.Trm[2][0] = this.CTM[2][0];
		this.Trm[2][1] = this.CTM[2][1];
		this.Trm[2][2] = 1;

		// round numbers if close to 1
		for (int y = 0; y < 3; y++) {
			for (int x = 0; x < 3; x++) {
				if ((this.Trm[x][y] > .99) & (this.Trm[x][y] < 1)) this.Trm[x][y] = 1;
			}
		}

		scale(scaleImage, w, h);

		completeImage();
	}

	private void scale(boolean scaleImage, int w, int h) {

		/**
		 * transform the image only if needed
		 */
		if (this.Trm[0][0] != 1.0 || this.Trm[1][1] != 1.0 || this.Trm[0][1] != 0.0 || this.Trm[1][0] != 0.0) {

			// workout transformation for the image
			AffineTransform image_at = new AffineTransform(this.Trm[0][0], this.Trm[0][1], this.Trm[1][0], this.Trm[1][1], 0, 0);

			// apply it to the shape first so we can align
			Area r = new Area(new Rectangle(0, 0, w, h));
			r.transform(image_at);

			// make sure it fits onto image (must start at 0,0)
			double ny = r.getBounds2D().getY();
			double nx = r.getBounds2D().getX();

			float a = this.Trm[0][0];
			float b = this.Trm[0][1];
			float c = this.Trm[1][0];
			float d = this.Trm[1][1];
			image_at = new AffineTransform(a, b, c, d, -nx, -ny);

			/**
			 * avoid upscaling
			 */
			if (a < 0) a = -a;
			if (b < 0) b = -b;
			if (c < 0) c = -c;
			if (d < 0) d = -d;

			// avoid large figures
			if (a > 5 || b > 5 || c > 5 || d > 5) return;

			// Create the affine operation.
			// ColorSpaces.hints causes single lines to vanish);
			AffineTransformOp invert;

			if (w > 1 && h > 1) {

				// fix image inversion if matrix (0,x,-y,0)
				if (this.CTM[0][0] == 0 && this.CTM[1][1] == 0 && this.CTM[0][1] > 0 && this.CTM[1][0] < 0) {
					image_at.scale(-1, 1);
					image_at.translate(-this.current_image.getWidth(), 0);
				}

				invert = new AffineTransformOp(image_at, ColorSpaces.hints);

			}
			else {

				// allow for line with changing values
				boolean isSolid = true;

				if (h == 1) {
					// test all pixels set so we can keep a solid line
					Raster ras = this.current_image.getRaster();
					int bands = ras.getNumBands();
					int width = ras.getWidth();
					int[] elements = new int[(width * bands) + 1];

					ras.getPixels(0, 0, width, 1, elements);
					for (int j = 0; j < bands; j++) {
						int first = elements[0];
						for (int i = 1; i < width; i++) {
							if (elements[i * j] != first) {
								isSolid = false;
								i = width;
								j = bands;
							}
						}
					}
				}

				if (isSolid) invert = new AffineTransformOp(image_at, null);
				else invert = new AffineTransformOp(image_at, ColorSpaces.hints);
			}

			// if there is a rotation make image ARGB so we can clip
			if (this.CTM[1][0] != 0 || this.CTM[0][1] != 0) this.current_image = ColorSpaceConvertor.convertToARGB(this.current_image);

			// scale image to produce final version
			if (scaleImage) {

				/** if not sheer/rotate, then bicubic */
				if (true || this.CTM[1][0] != 0 || this.CTM[0][1] != 0) {

					if (h > 1) {

						boolean failed = false;
						// allow for odd behaviour on some files
						try {
							this.current_image = invert.filter(this.current_image, null);
						}
						catch (Exception e) {
							// tell user and log
							if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());

							failed = true;
						}
						if (failed) {
							try {
								invert = new AffineTransformOp(image_at, null);
								this.current_image = invert.filter(this.current_image, null);
							}
							catch (Exception e) {}
						}
					}
				}
				else {

					int dx = 1, dy = 1;

					/** workout size and if needs to be inverted */
					w = (int) this.CTM[0][0];
					if (w == 0) w = (int) this.CTM[0][1];
					h = (int) this.CTM[1][1];
					if (h == 0) h = (int) this.CTM[1][0];

					if (w < 0) {
						w = -w;
						dx = -1;
					}
					if (h < 0) {
						h = -h;
						dy = -1;
					}

					/** turn around if needed */
					if (dx == -1 || dy == -1) {

						image_at = new AffineTransform();
						image_at.scale(dx, dy);

						if (dx == -1) image_at.translate(-this.current_image.getWidth(), 0);
						if (dy == -1) image_at.translate(0, -this.current_image.getHeight());

						// Create the affine operation.
						invert = new AffineTransformOp(image_at, ColorSpaces.hints);
						this.current_image = invert.filter(this.current_image, null);

					}

					Image scaledImage = this.current_image.getScaledInstance(w, h, Image.SCALE_SMOOTH);

					int type = this.current_image.getType();
					if ((type == 0) | (type == 2)) type = BufferedImage.TYPE_INT_RGB;

					this.current_image = new BufferedImage(scaledImage.getWidth(null), scaledImage.getHeight(null), type);

					Graphics2D g2 = this.current_image.createGraphics();
					g2.drawImage(scaledImage, 0, 0, null);

				}
			}
		}
	}

	/**
	 * complete image
	 */
	private void completeImage() {

		/**
		 * now workout correct screen co-ords allow for rotation
		 * 
		 * if ((CTM[1][0] == 0) &( (CTM[0][1] == 0))){ i_w =(int) Math.sqrt((CTM[0][0] * CTM[0][0]) + (CTM[0][1] * CTM[0][1])); i_h =(int)
		 * Math.sqrt((CTM[1][1] * CTM[1][1]) + (CTM[1][0] * CTM[1][0]));
		 * 
		 * }else{ i_h =(int) Math.sqrt((CTM[0][0] * CTM[0][0]) + (CTM[0][1] * CTM[0][1])); i_w =(int) Math.sqrt((CTM[1][1] * CTM[1][1]) + (CTM[1][0] *
		 * CTM[1][0])); }
		 * 
		 * if (CTM[1][0] < 0) i_x = (int) (CTM[2][0] + CTM[1][0]); else i_x = (int) CTM[2][0];
		 * 
		 * if (CTM[0][1] < 0) { i_y = (int) (CTM[2][1] + CTM[0][1]); } else { i_y = (int) CTM[2][1]; }
		 * 
		 * //alter to allow for back to front or reversed if (CTM[1][1] < 0) i_y = i_y - i_h;
		 * 
		 * if (CTM[0][0] < 0) i_x = i_x - i_w;
		 */
		calcCoordinates();
	}

	/**
	 * workout correct screen co-ords allow for rotation
	 */
	private final void calcCoordinates() {

		if (this.CTM[1][0] == 0 && this.CTM[0][1] == 0) {

			this.i_x = (int) this.CTM[2][0];
			this.i_y = (int) this.CTM[2][1];

			this.i_w = (int) this.CTM[0][0];
			this.i_h = (int) this.CTM[1][1];
			if (this.i_w < 0) this.i_w = -this.i_w;

			if (this.i_h < 0) this.i_h = -this.i_h;

		}
		else { // some rotation/skew
			this.i_w = (int) (Math.sqrt((this.CTM[0][0] * this.CTM[0][0]) + (this.CTM[0][1] * this.CTM[0][1])));
			this.i_h = (int) (Math.sqrt((this.CTM[1][1] * this.CTM[1][1]) + (this.CTM[1][0] * this.CTM[1][0])));

			if (this.CTM[1][0] > 0 && this.CTM[0][1] < 0) {
				this.i_x = (int) (this.CTM[2][0]);
				this.i_y = (int) (this.CTM[2][1] + this.CTM[0][1]);
				// System.err.println("AA "+i_w+" "+i_h);

			}
			else
				if (this.CTM[1][0] < 0 && this.CTM[0][1] > 0) {
					this.i_x = (int) (this.CTM[2][0] + this.CTM[1][0]);
					this.i_y = (int) (this.CTM[2][1]);
					// System.err.println("BB "+i_w+" "+i_h);

				}
				else
					if (this.CTM[1][0] > 0 && this.CTM[0][1] > 0) {
						this.i_x = (int) (this.CTM[2][0]);
						this.i_y = (int) (this.CTM[2][1]);
						// System.err.println("CC "+i_w+" "+i_h);
					}
					else {
						// System.err.println("DD "+i_w+" "+i_h);
						this.i_x = (int) (this.CTM[2][0]);
						this.i_y = (int) (this.CTM[2][1]);
					}

		}

		// alter to allow for back to front or reversed
		if (this.CTM[1][1] < 0) this.i_y = this.i_y - this.i_h;
		if (this.CTM[0][0] < 0) this.i_x = this.i_x - this.i_w;
	}

	/**
	 * get y of image (x1,y1 is top left)
	 */
	final public int getImageY() {
		return this.i_y;
	}

	/**
	 * get image
	 */
	final public BufferedImage getImage() {
		return this.current_image;
	}

	// ////////////////////////////////////////////////////////////////////////
	/**
	 * get width of image
	 */
	final public int getImageW() {
		return this.i_w;
	}

	// ////////////////////////////////////////////////////////////////////////
	/**
	 * get height of image
	 */
	final public int getImageH() {
		return this.i_h;
	}

	// ////////////////////////////////////////////////////////////////////////
	/**
	 * get X of image (x,y is top left)
	 */
	final public int getImageX() {
		return this.i_x;
	}

	// ///////////////////////////////////////////////////////////////////////
	/**
	 * clip the image
	 */
	final public void clipImage(Area current_shape) {

		// create a copy of clip (so we don't alter clip)
		Area final_clip = (Area) current_shape.clone();

		// actual size so we can trap any rounding error
		int image_w = this.current_image.getWidth();
		int image_h = this.current_image.getHeight();

		// shape of final image
		double shape_x = final_clip.getBounds2D().getX();
		double shape_y = final_clip.getBounds2D().getY();
		double shape_h = final_clip.getBounds2D().getHeight();
		double d_y = (image_h - shape_h);
		AffineTransform upside_down = new AffineTransform();
		upside_down.translate(-shape_x, -shape_y); // center
		upside_down.scale(1, -1); // reflect in x axis
		upside_down.translate(shape_x, -(shape_y + shape_h));
		final_clip.transform(upside_down);

		// line up to shape
		AffineTransform align_clip = new AffineTransform();

		// if not working at 72 dpi, alter clip to fit
		align_clip.translate(-this.i_x, this.i_y + d_y);
		final_clip.transform(align_clip);

		// co-ords of transformed shape
		// reset sizes to remove area clipped
		double x = final_clip.getBounds2D().getX();
		double y = final_clip.getBounds2D().getY();
		double w = final_clip.getBounds2D().getWidth();
		double h = final_clip.getBounds2D().getHeight();

		// get type of image used
		int image_type = this.current_image.getType();

		// set type so ICC and RGB uses ARGB
		if ((image_type == 0)) image_type = BufferedImage.TYPE_INT_ARGB; //
		else
			if ((image_type == BufferedImage.TYPE_INT_RGB)) image_type = BufferedImage.TYPE_INT_ARGB; //

		// draw image onto graphic (with clip) and then re-extract
		BufferedImage offscreen = new BufferedImage(image_w, image_h, image_type);
		// image of 'canvas'
		Graphics2D image_g2 = offscreen.createGraphics(); // g2 of canvas

		// if not transparent make background white
		if (offscreen.getColorModel().hasAlpha() == false) {
			image_g2.setBackground(Color.white);
			image_g2.fill(new Rectangle(0, 0, image_w, image_h));
		}

		image_g2.setClip(final_clip);

		try {
			// redraw image clipped and extract as rectangular shape
			image_g2.drawImage(this.current_image, 0, 0, null);
		}
		catch (Exception e) {
			LogWriter.writeLog("Exception " + e + " plotting clipping image");
		}

		// get image (now clipped )

		// check for rounding errors
		if (y < 0) {
			h = h + y;
			y = 0;
		}
		if (x < 0) {
			w = w + x;
			x = 0;
		}
		if (w > image_w) w = image_w;
		if (h > image_h) h = image_h;
		if (y + h > image_h) h = image_h - y;
		if (x + w > image_w) w = image_w - x;

		try {
			this.current_image = offscreen.getSubimage((int) x, (int) y, (int) (w), (int) (h));
		}
		catch (Exception e) {
			LogWriter
					.writeLog("Exception " + e + " extracting clipped image with values x=" + x + " y=" + y + " w=" + w + " h=" + h + " from image ");
		}

		// work out new co-ords from shape and current
		double x1, y1;
		if (this.i_x > shape_x) x1 = this.i_x;
		else x1 = shape_x;
		if (this.i_y > shape_y) y1 = this.i_y;
		else y1 = shape_y;

		this.i_x = (int) (x1);
		this.i_y = (int) (y1);
		this.i_w = (int) w;
		this.i_h = (int) h;
	}
}
