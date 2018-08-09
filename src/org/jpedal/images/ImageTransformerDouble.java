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
 * ImageTransformerDouble.java
 * ---------------
 */
package org.jpedal.images;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;

import org.jpedal.color.ColorSpaces;
import org.jpedal.io.ColorSpaceConvertor;
import org.jpedal.objects.GraphicsState;
import org.jpedal.render.BaseDisplay;
import org.jpedal.render.DynamicVectorRenderer;
import org.jpedal.utils.LogWriter;
import org.jpedal.utils.Matrix;

/**
 * class to shrink and clip an extracted image On reparse just calculates co-ords
 */
public class ImageTransformerDouble {

	double ny = 0, nx = 0;

	/** the clip */
	private Area clip = null;

	/** holds the actual image */
	private BufferedImage current_image;

	/** matrices used in transformation */
	private float[][] Trm, Trm1, Trm2, CTM;

	/** image co-ords */
	private int i_x = 0, i_y = 0, i_w = 0, i_h = 0;

	private boolean scaleImage;

	/** flag to show image clipped */
	private boolean hasClip = false;

	/**
	 * pass in image information and apply transformation matrix to image
	 */
	public ImageTransformerDouble(GraphicsState currentGS, BufferedImage new_image, boolean scaleImage) {

		// save global values
		this.current_image = new_image;
		this.scaleImage = scaleImage;

		this.CTM = currentGS.CTM; // local copy of CTM

		createMatrices();

		// get clipped image and co-ords
		if (currentGS.getClippingShape() != null) this.clip = (Area) currentGS.getClippingShape().clone();

		calcCoordinates();
	}

	/**
	 * applies the shear/rotate of a double transformation to the clipped image
	 */
	final public void doubleScaleTransformShear(DynamicVectorRenderer current) {

		final boolean debug = false;

		scale(this.Trm1);

		if (debug) {
			System.out.println("doubleScaleTransformShear");
		}

		// create a copy of clip (so we don't alter clip)
		if (this.clip != null) {

			Area final_clip = (Area) this.clip.clone();

			Area unscaled_clip = getUnscaledClip((Area) this.clip.clone());

			int segCount = BaseDisplay.isRectangle(final_clip);

			clipImage(unscaled_clip, final_clip, segCount);

			this.i_x = (int) this.clip.getBounds2D().getMinX();
			this.i_y = (int) this.clip.getBounds2D().getMinY();
			this.i_w = (int) ((this.clip.getBounds2D().getMaxX()) - this.i_x);
			this.i_h = (int) ((this.clip.getBounds2D().getMaxY()) - this.i_y);
		}
		else
			if (this.current_image.getType() == 10) { // do not need to be argb
			}
			else {
				this.current_image = ColorSpaceConvertor.convertToARGB(this.current_image);
			}
	}

	/**
	 * applies the scale of a double transformation to the clipped image
	 */
	final public void doubleScaleTransformScale() {

		if ((this.CTM[0][0] != 0.0) & (this.CTM[1][1] != 0.0)) scale(this.Trm2);
	}

	/** complete image and workout co-ordinates */
	final public void completeImage() {

		// Matrix.show(CTM);

		/*
		 * if((CTM[0][1]>0 )&(CTM[1][0]>0 )){ //ShowGUIMessage.showGUIMessage("",current_image,"a "); AffineTransform image_at =new AffineTransform();
		 * image_at.scale(-1,-1); image_at.translate(-current_image.getWidth(),-current_image.getHeight()); AffineTransformOp invert= new
		 * AffineTransformOp(image_at, ColorSpaces.hints); current_image = invert.filter(current_image,null); }
		 */

		// ShowGUIMessage.showGUIMessage("",current_image,"a ");

		/**/
		if (this.hasClip) {
			this.i_x = (int) this.clip.getBounds2D().getMinX();
			this.i_y = (int) this.clip.getBounds2D().getMinY();
			this.i_w = (this.current_image.getWidth());
			this.i_h = (this.current_image.getHeight());

			// System.out.println(current_image.getWidth()+" "+current_image.getHeight());
			// System.out.println(i_x+" "+i_y+" "+i_w+" "+i_h+" "+clip.getBounds2D());

		}
	}

	/** scale image to size */
	private void scale(float[][] Trm) {

		/**
		 * transform the image only if needed
		 */
		if (Trm[0][0] != 1.0 || Trm[1][1] != 1.0 || Trm[0][1] != 0.0 || Trm[1][0] != 0.0) {

			int w = this.current_image.getWidth(); // raw width
			int h = this.current_image.getHeight(); // raw height

			// workout transformation for the image
			AffineTransform image_at = new AffineTransform(Trm[0][0], -Trm[0][1], -Trm[1][0], Trm[1][1], 0, 0);

			// apply it to the shape first so we can align
			Area r = new Area(new Rectangle(0, 0, w, h));
			r.transform(image_at);

			// make sure it fits onto image (must start at 0,0)
			this.ny = r.getBounds2D().getY();
			this.nx = r.getBounds2D().getX();
			image_at = new AffineTransform(Trm[0][0], -Trm[0][1], -Trm[1][0], Trm[1][1], -this.nx, -this.ny);

			// Create the affine operation.
			// ColorSpaces.hints causes single lines to vanish);
			AffineTransformOp invert;
			if ((w > 10) & (h > 10)) invert = new AffineTransformOp(image_at, ColorSpaces.hints);
			else invert = new AffineTransformOp(image_at, null);

			// scale image to produce final version
			if (this.scaleImage) this.current_image = invert.filter(this.current_image, null);

		}
	}

	/** workout the transformation as 1 or 2 transformations */
	private void createMatrices() {

		int w = this.current_image.getWidth(); // raw width
		int h = this.current_image.getHeight(); // raw height

		// build transformation matrix by hand to avoid errors in rounding
		this.Trm = new float[3][3];
		this.Trm[0][0] = (this.CTM[0][0] / w);
		this.Trm[0][1] = (this.CTM[0][1] / w);
		this.Trm[0][2] = 0;
		this.Trm[1][0] = (this.CTM[1][0] / h);
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

		/** now work out as 2 matrices */
		this.Trm1 = new float[3][3];
		this.Trm2 = new float[3][3];

		// used to handle sheer
		float x1, x2, y1, y2;

		x1 = this.CTM[0][0];
		if (x1 < 0) x1 = -x1;
		x2 = this.CTM[0][1];
		if (x2 < 0) x2 = -x2;

		y1 = this.CTM[1][1];
		if (y1 < 0) y1 = -y1;
		y2 = this.CTM[1][0];
		if (y2 < 0) y2 = -y2;

		// factor out scaling to produce just the sheer/rotation
		if (this.CTM[0][0] == 0.0 || this.CTM[1][1] == 0.0) {
			this.Trm1 = this.Trm;

		}
		else
			if ((this.CTM[0][1] == 0.0) && (this.CTM[1][0] == 0.0)) {

				this.Trm1[0][0] = w / (this.CTM[0][0]);
				this.Trm1[0][1] = 0;
				this.Trm1[0][2] = 0;

				this.Trm1[1][0] = 0;
				this.Trm1[1][1] = h / (this.CTM[1][1]);
				this.Trm1[1][2] = 0;

				this.Trm1[2][0] = 0;
				this.Trm1[2][1] = 0;
				this.Trm1[2][2] = 1;

				this.Trm1 = Matrix.multiply(this.Trm, this.Trm1);

				// round numbers if close to 1
				for (int y = 0; y < 3; y++) {
					for (int x = 0; x < 3; x++) {
						if ((this.Trm1[x][y] > .99) & (this.Trm1[x][y] < 1)) this.Trm1[x][y] = 1;
					}
				}

				/**
				 * correct if image reversed on horizontal axis
				 */
				if (this.Trm1[2][0] < 0 && this.Trm1[0][0] > 0 && this.CTM[0][0] < 0) {
					this.Trm1[2][0] = 0;
					this.Trm1[0][0] = -1f;

				}

				/**
				 * correct if image reversed on vertical axis
				 */
				if (this.Trm1[2][1] < 0 && this.Trm1[1][1] > 0 && this.CTM[1][1] < 0 && this.CTM[0][0] < 0) {
					this.Trm1[2][1] = 0;
					this.Trm1[1][1] = -1f;
				}

			}
			else { // its got sheer/rotation

				if (x1 > x2) this.Trm1[0][0] = w / (this.CTM[0][0]);
				else this.Trm1[0][0] = w / (this.CTM[0][1]);
				if (this.Trm1[0][0] < 0) this.Trm1[0][0] = -this.Trm1[0][0];
				this.Trm1[0][1] = 0;
				this.Trm1[0][2] = 0;

				this.Trm1[1][0] = 0;

				if (y1 > y2) this.Trm1[1][1] = h / (this.CTM[1][1]);
				else this.Trm1[1][1] = h / (this.CTM[1][0]);
				if (this.Trm1[1][1] < 0) this.Trm1[1][1] = -this.Trm1[1][1];
				this.Trm1[1][2] = 0;

				this.Trm1[2][0] = 0;
				this.Trm1[2][1] = 0;
				this.Trm1[2][2] = 1;

				this.Trm1 = Matrix.multiply(this.Trm, this.Trm1);

				// round numbers if close to 1
				for (int y = 0; y < 3; y++) {
					for (int x = 0; x < 3; x++) {
						if ((this.Trm1[x][y] > .99) & (this.Trm1[x][y] < 1)) this.Trm1[x][y] = 1;
					}
				}
			}

		// create a transformation with just the scaling
		if (x1 > x2) this.Trm2[0][0] = (this.CTM[0][0] / w);
		else this.Trm2[0][0] = (this.CTM[0][1] / w);

		if (this.Trm2[0][0] < 0) this.Trm2[0][0] = -this.Trm2[0][0];
		this.Trm2[0][1] = 0;
		this.Trm2[0][2] = 0;
		this.Trm2[1][0] = 0;
		if (y1 > y2) this.Trm2[1][1] = (this.CTM[1][1] / h);
		else this.Trm2[1][1] = (this.CTM[1][0] / h);

		if (this.Trm2[1][1] < 0) this.Trm2[1][1] = -this.Trm2[1][1];

		this.Trm2[1][2] = 0;
		this.Trm2[2][0] = 0;
		this.Trm2[2][1] = 0;
		this.Trm2[2][2] = 1;

		// round numbers if close to 1
		for (int y = 0; y < 3; y++) {
			for (int x = 0; x < 3; x++) {
				if ((this.Trm2[x][y] > .99) & (this.Trm2[x][y] < 1)) this.Trm2[x][y] = 1;
			}
		}
	}

	/**
	 * workout correct screen co-ords allow for rotation
	 */
	private final void calcCoordinates() {

		if ((this.CTM[1][0] == 0) & ((this.CTM[0][1] == 0))) {

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

			if ((this.CTM[1][0] > 0) & (this.CTM[0][1] < 0)) {
				this.i_x = (int) (this.CTM[2][0]);
				this.i_y = (int) (this.CTM[2][1] + this.CTM[0][1]);
				// System.err.println("AA "+i_w+" "+i_h);

			}
			else
				if ((this.CTM[1][0] < 0) & (this.CTM[0][1] > 0)) {
					this.i_x = (int) (this.CTM[2][0] + this.CTM[1][0]);
					this.i_y = (int) (this.CTM[2][1]);
					// System.err.println("BB "+i_w+" "+i_h);
				}
				else
					if ((this.CTM[1][0] > 0) & (this.CTM[0][1] > 0)) {
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

		// System.err.println(i_x+" "+i_y+" "+i_w+" "+i_h);
		// Matrix.show(CTM);
		// alter to allow for back to front or reversed
		if (this.CTM[1][1] < 0) this.i_y = this.i_y - this.i_h;
		if (this.CTM[0][0] < 0) this.i_x = this.i_x - this.i_w;

		// ShowGUIMessage.showGUIMessage("",current_image,"xx="+i_x+" "+i_y+" "+i_w+" "+i_h+" h="+current_image.getHeight());
	}

	// ////////////////////////////////////////////////////////////////////////
	/**
	 * get y of image (x1,y1 is top left)
	 */
	final public int getImageY() {
		return this.i_y;
	}

	// ////////////////////////////////////////////////////////////////////////
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

	/**
	 * clip the image
	 */
	final private void clipImage(Area final_clip, Area unscaled_clip, int segCount) {

		double shape_x = unscaled_clip.getBounds2D().getX();
		double shape_y = unscaled_clip.getBounds2D().getY();

		int image_w = this.current_image.getWidth();
		int image_h = this.current_image.getHeight();

		// co-ords of transformed shape
		// reset sizes to remove area clipped
		int x = (int) final_clip.getBounds().getX();
		int y = (int) final_clip.getBounds().getY();
		int w = (int) final_clip.getBounds().getWidth();
		int h = (int) final_clip.getBounds().getHeight();

		// System.out.println(x+" "+y+" "+w+" "+h+" "+current_image.getWidth()+" "+current_image.getHeight());
		// if(BaseDisplay.isRectangle(final_clip)<7 && Math.abs(final_clip.getBounds().getWidth()-current_image.getWidth())<=1 &&
		// Math.abs(final_clip.getBounds().getHeight()-current_image.getHeight())<=1){

		/**
		 * if not rectangle create inverse of clip and paint on to add transparency
		 */
		if (segCount > 5) {

			// turn image upside down
			AffineTransform image_at = new AffineTransform();
			image_at.scale(1, -1);
			image_at.translate(0, -this.current_image.getHeight());
			AffineTransformOp invert = new AffineTransformOp(image_at, ColorSpaces.hints);

			this.current_image = invert.filter(this.current_image, null);

			Area inverseClip = new Area(new Rectangle(0, 0, image_w, image_h));
			inverseClip.exclusiveOr(final_clip);
			this.current_image = ColorSpaceConvertor.convertToARGB(this.current_image);// make sure has opacity

			Graphics2D image_g2 = this.current_image.createGraphics(); // g2 of canvas
			image_g2.setComposite(AlphaComposite.Clear);
			image_g2.fill(inverseClip);

			// and invert again
			AffineTransform image_at2 = new AffineTransform();
			image_at2.scale(1, -1);
			image_at2.translate(0, -this.current_image.getHeight());
			AffineTransformOp invert3 = new AffineTransformOp(image_at2, ColorSpaces.hints);

			this.current_image = invert3.filter(this.current_image, null);

		}
		// get image (now clipped )

		// check for rounding errors
		if (y < 0) {
			h = h - y;
			y = 0;
		}
		else {

			y = image_h - h - y;

			// allow for fp error
			if (y < 0) {
				y = 0;
			}
		}

		if (x < 0) {
			w = w - x;
			x = 0;
		}

		if (w > image_w) w = image_w;
		if (h > image_h) h = image_h;

		if (y + h > image_h) h = image_h - y;

		if (x + w > image_w) w = image_w - x;

		// extract if smaller with clip
		if (h < 1 || w < 1) { // ignore if not wide/high enough
		}
		else
			if (x == 0 && y == 0 && w == this.current_image.getWidth() && h == this.current_image.getHeight()) {
				// dont bother if no change
			}
			else {
				try {
					this.current_image = this.current_image.getSubimage(x, y, w, h);
				}
				catch (Exception e) {
					LogWriter.writeLog("Exception " + e + " extracting clipped image with values x=" + x + " y=" + y + " w=" + w + " h=" + h
							+ " from image " + this.current_image);

				}
				catch (Error err) {}
			}

		// work out new co-ords from shape and current
		double x1, y1;
		if (this.i_x > shape_x) {
			x1 = this.i_x;
		}
		else {
			x1 = shape_x;
		}
		if (this.i_y > shape_y) {
			y1 = this.i_y;
		}
		else {
			y1 = shape_y;
		}

		this.i_x = (int) x1;
		this.i_y = (int) y1;
		this.i_w = w;
		this.i_h = h;
	}

	private Area getUnscaledClip(Area final_clip) {

		double dx = -(this.CTM[2][0]), dy = -this.CTM[2][1];

		if (this.CTM[1][0] < 0) {
			dx = dx - this.CTM[1][0];
		}
		if ((this.CTM[0][0] < 0) && (this.CTM[1][0] >= 0)) {
			dx = dx - this.CTM[1][0];
		}

		if (this.CTM[0][1] < 0) {
			dy = dy - this.CTM[0][1];
		}
		if (this.CTM[1][1] < 0) {
			if (this.CTM[0][1] > 0) {
				dy = dy - this.CTM[0][1];
			}
			else
				if (this.CTM[1][1] < 0) {
					dy = dy - this.CTM[1][1];
				}
		}

		AffineTransform align_clip = new AffineTransform();
		align_clip.translate(dx, dy);
		final_clip.transform(align_clip);

		AffineTransform invert2 = new AffineTransform(1 / this.Trm2[0][0], 0, 0, 1 / this.Trm2[1][1], 0, 0);

		final_clip.transform(invert2);

		// fix for 'mirror' image on Mac
		int dxx = (int) final_clip.getBounds().getX();
		if (dxx < 0) {
			final_clip.transform(AffineTransform.getTranslateInstance(-dxx, 0));
		}

		return final_clip;
	}

}
