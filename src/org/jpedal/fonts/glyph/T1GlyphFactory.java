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
 * T1GlyphFactory.java
 * ---------------
 */
package org.jpedal.fonts.glyph;

import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;

import org.jpedal.utils.repositories.Vector_Float;
import org.jpedal.utils.repositories.Vector_Int;
import org.jpedal.utils.repositories.Vector_Path;

/**
 * decodes the T1 glyph into a set of paths wrapped around a glyph
 */
public class T1GlyphFactory implements GlyphFactory {

	private static final float zero = 0f;

	/** we tell user we have not used some shapes only ONCE */
	private Vector_Float shape_primitive_x2 = new Vector_Float(1000);
	private Vector_Float shape_primitive_y = new Vector_Float(1000);

	/** store shape currently being assembled */
	private Vector_Int shape_primitives = new Vector_Int(1000);

	private Vector_Float shape_primitive_x3 = new Vector_Float(1000);
	private Vector_Float shape_primitive_y3 = new Vector_Float(1000);

	private Vector_Float shape_primitive_y2 = new Vector_Float(1000);
	private Vector_Float shape_primitive_x = new Vector_Float(1000);

	private static final int H = 3;
	private static final int L = 2;

	/** flags for commands used */
	private static final int M = 1;
	private static final int C = 5;

	/** vertical positioning and scaling */
	private float ymin = 0;
	private int leftSideBearing = 0;

	/*
	 * (non-Javadoc)
	 * @see org.jpedal.fonts.GlyphFactory#reinitialise(double[])
	 */
	@Override
	public void reinitialise(double[] fontMatrix) {

		// this.FontMatrix=FontMatrix;
	}

	/*
	 * (non-Javadoc)
	 * @see org.jpedal.fonts.GlyphFactory#getGlyph()
	 */
	@Override
	public PdfGlyph getGlyph(boolean debug) {

		// initialise cache
		Vector_Path cached_current_path = new Vector_Path(100);

		// create the shape - we have to do it this way
		// because we get the WINDING RULE last and we need it
		// to initialise the shape
		GeneralPath current_path = new GeneralPath(Path2D.WIND_NON_ZERO);
		current_path.moveTo(0, 0);
		// init points
		float[] x = this.shape_primitive_x.get();
		float[] y = this.shape_primitive_y.get();
		float[] x2 = this.shape_primitive_x2.get();
		float[] y2 = this.shape_primitive_y2.get();
		float[] x3 = this.shape_primitive_x3.get();
		float[] y3 = this.shape_primitive_y3.get();
		int i = 0;
		int end = this.shape_primitives.size() - 1;
		int[] commands = this.shape_primitives.get();

		// loop through commands and add to glyph
		while (i < end) {

			// System.out.println(i+" "+x+" "+y);
			if (commands[i] == L) {
				current_path.lineTo(x[i], y[i] - this.ymin);
			}
			else
				if (commands[i] == H) {
					current_path.closePath();

					// save for later use
					cached_current_path.addElement(current_path);

					current_path = new GeneralPath(Path2D.WIND_NON_ZERO);
					current_path.moveTo(0, 0);
				}
				else
					if (commands[i] == M) {
						current_path.moveTo(x[i], y[i] - this.ymin);

					}
					else
						if (commands[i] == C) {
							current_path.curveTo(x[i], y[i] - this.ymin, x2[i], y2[i] - this.ymin, x3[i], y3[i] - this.ymin);

						}

			i++;
		}

		// now garbage collect arrays as not needed
		if (!debug) {
			this.shape_primitive_x2.reuse();
			this.shape_primitive_y.reuse();
			this.shape_primitives.reuse();
			this.shape_primitive_x3.reuse();
			this.shape_primitive_y3.reuse();
			this.shape_primitive_y2.reuse();
			this.shape_primitive_x.reuse();
		}

		return new T1Glyph(cached_current_path);
	}

	// ///////////////////////////////////////////////////////////////////////
	/**
	 * end a shape, storing info for later
	 */
	@Override
	final public void closePath() {
		this.shape_primitives.addElement(H);

		// add empty values
		this.shape_primitive_x.addElement(0);
		this.shape_primitive_y.addElement(0);
		this.shape_primitive_x2.addElement(0);
		this.shape_primitive_y2.addElement(0);
		this.shape_primitive_x3.addElement(0);
		this.shape_primitive_y3.addElement(0);
	}

	// ////////////////////////////////////////////////////////////////////////
	/**
	 * add a curve to the shape
	 */
	@Override
	final public void curveTo(float x, float y, float x2, float y2, float x3, float y3) {
		this.shape_primitives.addElement(C);
		this.shape_primitive_x.addElement(x);
		this.shape_primitive_y.addElement(y);

		// add empty values to keep in sync
		// add empty values
		this.shape_primitive_x2.addElement(x2);
		this.shape_primitive_y2.addElement(y2);
		this.shape_primitive_x3.addElement(x3);
		this.shape_primitive_y3.addElement(y3);
	}

	// ////////////////////////////////////////////////////////////////////////
	/**
	 * add a line to the shape
	 */
	@Override
	final public void lineTo(float x, float y) {
		this.shape_primitives.addElement(L);
		this.shape_primitive_x.addElement(x);
		this.shape_primitive_y.addElement(y);

		// add empty values to keep in sync
		// add empty values

		this.shape_primitive_x2.addElement(zero);
		this.shape_primitive_y2.addElement(zero);
		this.shape_primitive_x3.addElement(zero);
		this.shape_primitive_y3.addElement(zero);
	}

	/**
	 * start a shape by creating a shape object
	 */
	@Override
	final public void moveTo(float x, float y) {
		this.shape_primitives.addElement(M);
		this.shape_primitive_x.addElement(x);
		this.shape_primitive_y.addElement(y);

		// add empty values
		this.shape_primitive_x2.addElement(0);
		this.shape_primitive_y2.addElement(0);
		this.shape_primitive_x3.addElement(0);
		this.shape_primitive_y3.addElement(0);
	}

	/**
	 * set ymin - ie vertical kern
	 */
	@Override
	public void setYMin(float ymin, float ymax) {

		this.ymin = ymin;
		// this.ymax=ymax;
	}

	@Override
	public int getLSB() {
		return this.leftSideBearing;
	}
}
