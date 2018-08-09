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
 * RenderUtils.java
 * ---------------
 */
package org.jpedal.render;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

import org.jpedal.color.ColorSpaces;
import org.jpedal.io.JAIHelper;
import org.jpedal.utils.LogWriter;

/**
 * static helper methods for rendering
 */
public class RenderUtils {

	public static boolean isInverted(float[][] CTM) {
		return CTM[0][0] < 0 || CTM[1][1] < 0;
	}

	public static boolean isRotated(float[][] CTM) {
		return (CTM[0][0] == 0 && CTM[1][1] == 0) || (CTM[0][1] > 0 && CTM[1][0] < 0) || (CTM[0][1] < 0 && CTM[1][0] > 0);
	}

	static BufferedImage invertImage(float[][] CTM, BufferedImage image) {

		// if((CTM[0][0]==0 || CTM[1][1]==0) || (isRotated(CTM) && !isInverted(CTM))) {
		// return image;
		// }

		// turn upside down
		AffineTransform image_at2 = new AffineTransform();
		image_at2.scale(1, -1);
		image_at2.translate(0, -image.getHeight());

		AffineTransformOp invert3 = new AffineTransformOp(image_at2, ColorSpaces.hints);

		boolean imageProcessed = false;

		if (JAIHelper.isJAIused()) {

			imageProcessed = true;

			try {
				image = (javax.media.jai.JAI.create("affine", image, image_at2, new javax.media.jai.InterpolationNearest())).getAsBufferedImage();
			}
			catch (Exception e) {
				imageProcessed = false;
				// tell user and log
				if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
			}
			catch (Error err) {
				imageProcessed = false;

				// tell user and log
				if (LogWriter.isOutput()) LogWriter.writeLog("Error: " + err.getMessage());
			}

			if (!imageProcessed && LogWriter.isOutput()) LogWriter.writeLog("Unable to use JAI for image inversion");

		}

		if (!imageProcessed) {

			if (image.getType() == 12) { // avoid turning into ARGB

				BufferedImage source = image;
				image = new BufferedImage(source.getWidth(), source.getHeight(), source.getType());

				invert3.filter(source, image);
			}
			else {

				boolean failed = false;
				// allow for odd behaviour on some files
				try {
					image = invert3.filter(image, null);
				}
				catch (Exception e) {
					failed = true;
				}
				if (failed) {
					try {
						invert3 = new AffineTransformOp(image_at2, null);
						image = invert3.filter(image, null);
					}
					catch (Exception e) {}
				}
			}
		}

		return image;
	}

	static BufferedImage invertImageBeforeSave(BufferedImage image, boolean horizontal) {

		// turn upside down
		AffineTransform image_at2 = new AffineTransform();
		if (horizontal) {
			image_at2.scale(-1, 1);
			image_at2.translate(-image.getWidth(), 0);
		}
		else {
			image_at2.scale(1, -1);
			image_at2.translate(0, -image.getHeight());
		}
		AffineTransformOp invert3 = new AffineTransformOp(image_at2, ColorSpaces.hints);

		boolean imageProcessed = false;

		if (JAIHelper.isJAIused()) {

			imageProcessed = true;

			try {
				image = (javax.media.jai.JAI.create("affine", image, image_at2, new javax.media.jai.InterpolationNearest())).getAsBufferedImage();
			}
			catch (Exception e) {
				imageProcessed = false;

				// tell user and log
				if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());

			}
			catch (Error err) {
				imageProcessed = false;

				// tell user and log
				if (LogWriter.isOutput()) LogWriter.writeLog("Error: " + err.getMessage());
			}

			if (!imageProcessed && LogWriter.isOutput()) {
				LogWriter.writeLog("Unable to use JAI for image inversion");
			}

		}

		if (!imageProcessed) {

			if (image.getType() == 12) { // avoid turning into ARGB

				BufferedImage source = image;
				image = new BufferedImage(source.getWidth(), source.getHeight(), source.getType());

				invert3.filter(source, image);
			}
			else image = invert3.filter(image, null);

		}
		return image;
	}

	/** resize array */
	static float[] checkSize(float[] array, int currentItem) {

		int size = array.length;
		if (size <= currentItem) {
			int newSize = size * 2;
			float[] newArray = new float[newSize];
			System.arraycopy(array, 0, newArray, 0, size);

			array = newArray;
		}

		return array;
	}

	/**
	 * update clip
	 * 
	 * @param defaultClip
	 */
	public static void renderClip(Area clip, Rectangle dirtyRegion, Shape defaultClip, Graphics2D g2) {

		if (clip != null) {
			g2.setClip(clip);

			// can cause problems in Canoo so limit effect if Canoo running
			if (dirtyRegion != null) // && (!isRunningOnRemoteClient || clip.intersects(dirtyRegion)))
			g2.clip(dirtyRegion);
		}
		else g2.setClip(defaultClip);
	}

	// work out size glyph occupies
	static Rectangle getAreaForGlyph(float[][] trm) {
		// workout area
		int w = (int) Math.sqrt((trm[0][0] * trm[0][0]) + (trm[1][0] * trm[1][0]));
		int h = (int) Math.sqrt((trm[1][1] * trm[1][1]) + (trm[0][1] * trm[0][1]));

		float xDiff = 0;
		float yDiff = 0;

		if (trm[0][0] < 0) xDiff = trm[0][0];
		else
			if (trm[1][0] < 0) xDiff = trm[1][0];

		if (trm[1][1] < 0) yDiff = trm[1][1];
		else
			if (trm[0][1] < 0) yDiff = trm[0][1];

		return (new Rectangle((int) (trm[2][0] + xDiff), (int) (trm[2][1] + yDiff), w, h));
	}

	/**
	 * Rectangle contains code does not handle negative values Use this instead.
	 * 
	 * @param area
	 *            : Rectangle to look in
	 * @param x
	 *            : value on the x axis
	 * @param y
	 *            : value on the y axis
	 * @return true is point is within area
	 */
	public static boolean rectangleContains(Rectangle area, int x, int y, int i) {

		int lowX = area.x;
		int hiX = area.x + area.width;
		int lowY = area.y;
		int hiY = area.y + area.height;
		boolean containsPoint = false;

		// if negative value used swap the lowest and highest point
		if (lowX > hiX) {
			int temp = lowX;
			lowX = hiX;
			hiX = temp;
		}

		if (lowY > hiY) {
			int temp = lowY;
			lowY = hiY;
			hiY = temp;
		}

		if ((lowY < y && y < hiY) && (lowX < x && x < hiX)) containsPoint = true;

		return containsPoint;
	}

	/**
	 * generic method to return a serilized object from an InputStream
	 * 
	 * NOT PART OF API and subject to change (DO NOT USE)
	 * 
	 * @param bis
	 *            - ByteArrayInputStream containing serilized object
	 * @return - deserilized object
	 * @throws java.io.IOException
	 * @throws ClassNotFoundException
	 */
	public static Object restoreFromStream(ByteArrayInputStream bis) throws IOException, ClassNotFoundException {

		// turn back into object
		ObjectInput os = new ObjectInputStream(bis);

		return os.readObject();
	}

	/**
	 * generic method to serilized an object to an OutputStream
	 * 
	 * NOT PART OF API and subject to change (DO NOT USE)
	 * 
	 * @param bos
	 *            - ByteArrayOutputStream to serilize to
	 * @param obj
	 *            - object to serilize
	 * @param string2
	 * @throws java.io.IOException
	 */
	public static void writeToStream(ByteArrayOutputStream bos, Object obj, String string2) throws IOException {

		ObjectOutput os = new ObjectOutputStream(bos);

		os.writeObject(obj);
		os.close();
	}
}
