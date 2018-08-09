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
 * PdfShape.java
 * ---------------
 */
package org.jpedal.objects;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.io.Serializable;

import org.jpedal.parser.Cmd;
import org.jpedal.render.DynamicVectorRenderer;
import org.jpedal.utils.LogWriter;
import org.jpedal.utils.repositories.Vector_Float;
import org.jpedal.utils.repositories.Vector_Int;

/**
 * <p>
 * defines the current shape which is created by command stream
 * </p>
 * <p>
 * <b>This class is NOT part of the API </b>
 * </p>
 * . Shapes can be drawn onto pdf or used as a clip on other image/shape/text. Shape is built up by storing commands and then turning these commands
 * into a shape. Has to be done this way as Winding rule is not necessarily declared at start.
 */
public class PdfShape implements Serializable {

	private static final long serialVersionUID = -4877252327369495970L;

	/**
	 * used to stop lots of huge, complex shapes. Note we DO NOT reset as we reuse this object and it stores cumulative count
	 */
	int complexClipCount = 0;

	/** we tell user we have not used some shapes only ONCE */
	private Vector_Float shape_primitive_x2 = new Vector_Float(1000);
	private Vector_Float shape_primitive_y = new Vector_Float(1000);

	/** store shape currently being assembled */
	private Vector_Int shape_primitives = new Vector_Int(1000);

	/** type of winding rule used to draw shape */
	private int winding_rule = Path2D.WIND_NON_ZERO;
	private Vector_Float shape_primitive_x3 = new Vector_Float(1000);
	private Vector_Float shape_primitive_y3 = new Vector_Float(1000);

	/** used when trying to choose which shapes to use to test furniture */
	private Vector_Float shape_primitive_y2 = new Vector_Float(1000);
	private Vector_Float shape_primitive_x = new Vector_Float(1000);
	private static final int H = 3;
	private static final int L = 2;
	private static final int V = 6;

	/** flags for commands used */
	private static final int M = 1;
	private static final int Y = 4;
	private static final int C = 5;

	/** flag to show if image is for clip */
	private boolean isClip = false;

	private boolean emulateEvenOddRule = false;

	// ///////////////////////////////////////////////////////////////////////
	/**
	 * end a shape, storing info for later
	 */
	final public void closeShape() {
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
	final public void addBezierCurveC(float x, float y, float x2, float y2, float x3, float y3) {
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
	 * set winding rule - non zero
	 */
	final public void setNONZEROWindingRule() {
		this.winding_rule = Path2D.WIND_NON_ZERO;
	}

	// ////////////////////////////////////////////////////////////////////////
	/**
	 * add a line to the shape
	 */
	final public void lineTo(float x, float y) {
		this.shape_primitives.addElement(L);
		this.shape_primitive_x.addElement(x);
		this.shape_primitive_y.addElement(y);

		// add empty values to keep in sync
		// add empty values
		this.shape_primitive_x2.addElement(0);
		this.shape_primitive_y2.addElement(0);
		this.shape_primitive_x3.addElement(0);
		this.shape_primitive_y3.addElement(0);
	}

	// /////////////////////////////////////////////////////////////////////////
	/**
	 * add a curve to the shape
	 */
	final public void addBezierCurveV(float x2, float y2, float x3, float y3) {
		this.shape_primitives.addElement(V);
		this.shape_primitive_x.addElement(200);
		this.shape_primitive_y.addElement(200);

		// add empty values to keep in sync
		// add empty values
		this.shape_primitive_x2.addElement(x2);
		this.shape_primitive_y2.addElement(y2);
		this.shape_primitive_x3.addElement(x3);
		this.shape_primitive_y3.addElement(y3);
	}

	// ////////////////////////////////////////////////////////////////////////
	/**
	 * turn shape commands into a Shape object, storing info for later. Has to be done this way because we need the winding rule to initialise the
	 * shape in Java, and it could be set anywhere in the command stream
	 */
	final public Shape generateShapeFromPath(float[][] CTM, float thickness, int cmd, int type) {

		boolean is_clip = this.isClip;
		if (cmd == Cmd.n) is_clip = false;

		// create the shape - we have to do it this way
		// because we get the WINDING RULE last and we need it
		// to initialise the shape
		GeneralPath current_path = null;
		Area current_area = null;
		Shape current_shape;

		// init points
		float[] x = this.shape_primitive_x.get();
		float[] y = this.shape_primitive_y.get();
		float[] x2 = this.shape_primitive_x2.get();
		float[] y2 = this.shape_primitive_y2.get();
		float[] x3 = this.shape_primitive_x3.get();
		float[] y3 = this.shape_primitive_y3.get();
		int[] command = this.shape_primitives.get();

		// float lx=0,ly=0;
		// float xs=0,ys=0;
		int end = this.shape_primitives.size() - 1;

		// used to debug
		final boolean show = false;
		final boolean debug = false;

		// If type is HTML & winding rule is EvenOdd & the shape has a loop, then the EvenOdd rule needs to be emulated.
		// (SVG has both rules, unsure about JavaFX. If not, just add && type == DynamicVectorRendered.CREATE_JAVAFX)
		// Segment count > 6 is an added optimisation. Unlikely to draw a shape that this would affect with < 6 segments.
		if (this.winding_rule == Path2D.WIND_EVEN_ODD && type == DynamicVectorRenderer.CREATE_HTML && getSegmentCount() > 6) {

			int i = 0;
			float xx = 0;
			float yy = 0;
			while (i < end && !this.emulateEvenOddRule) {

				// only used to create clips
				if (command[i] == H) {
					// H is Close path
					this.emulateEvenOddRule = true;
				}
				else
					if (command[i] == L) {
						// L is Line to x[i], y[i]
						if (xx == x[i] && yy == y[i]) this.emulateEvenOddRule = true;
					}
					else
						if (command[i] == M) {
							// M is Move to x[i], y[i]
							xx = x[i];
							yy = y[i];
						}
						else {
							// Curve to x3[i], y3[i]
							if (xx == x3[i] && yy == y3[i]) this.emulateEvenOddRule = true;
						}

				i++;
			}
		}

		// loop through commands and add to shape
		for (int i = 0; i < end; i++) {
			if (current_path == null) {
				current_path = new GeneralPath(this.winding_rule);
				current_path.moveTo(x[i], y[i]);
				// lx=x[i];
				// ly=y[i];
				// xs=lx;
				// ys=ly;

				if (show) LogWriter.writeLog("==START=" + x[i] + ' ' + y[i]);
			}

			// only used to create clips
			if (command[i] == H) {

				current_path.closePath();
				if (is_clip) {

					// current_path.lineTo(xs,ys);
					// current_path.closePath();
					if (show) LogWriter.writeLog("==H\n\n" + current_area + ' ' + current_path.getBounds2D() + ' '
							+ new Area(current_path).getBounds2D());

					if (current_area == null) {
						current_area = new Area(current_path);

						// trap for apparent bug in Java where small paths create a 0 size Area
						if ((current_area.getBounds2D().getWidth() <= 0.0) || (current_area.getBounds2D().getHeight() <= 0.0)) current_area = new Area(
								current_path.getBounds2D());

					}
					else current_area.add(new Area(current_path));

					current_path = null;
				}
				else {
					if (show) LogWriter.writeLog("close shape " + command[i] + " i=" + i);

				}
			}

			if (command[i] == L) {

				current_path.lineTo(x[i], y[i]);

				// lx=x[i];
				// ly=y[i];

				if (show) LogWriter.writeLog("==L" + x[i] + ',' + y[i] + "  ");

			}
			else
				if (command[i] == M) {
					current_path.moveTo(x[i], y[i]);
					// lx=x[i];
					// ly=y[i];
					if (show) LogWriter.writeLog("==M" + x[i] + ',' + y[i] + "  ");
				}
				else {
					// cubic curves which use 2 control points
					if (command[i] == Y) {
						if (show) LogWriter.writeLog("==Y " + x[i] + ' ' + y[i] + ' ' + x3[i] + ' ' + y3[i] + ' ' + x3[i] + ' ' + y3[i]);

						current_path.curveTo(x[i], y[i], x3[i], y3[i], x3[i], y3[i]);
						// lx=x3[i];
						// ly=y3[i];

					}
					else
						if (command[i] == C) {
							if (show) LogWriter.writeLog("==C " + x[i] + ' ' + y[i] + ' ' + x2[i] + ' ' + y2[i] + ' ' + x3[i] + ' ' + y3[i]);

							current_path.curveTo(x[i], y[i], x2[i], y2[i], x3[i], y3[i]);
							// lx=x3[i];
							// ly=y3[i];

						}
						else
							if (command[i] == V) {
								float c_x = (float) current_path.getCurrentPoint().getX();
								float c_y = (float) current_path.getCurrentPoint().getY();
								if (show) LogWriter.writeLog("==v " + c_x + ',' + c_y + ',' + x2[i] + ',' + y2[i] + ',' + x3[i] + ',' + y3[i]);

								current_path.curveTo(c_x, c_y, x2[i], y2[i], x3[i], y3[i]);
								// lx=x3[i];
								// ly=y3[i];
							}
				}

			if (debug) {
				try {
					java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(700, 700, java.awt.image.BufferedImage.TYPE_INT_ARGB);

					Graphics2D gg2 = img.createGraphics();
					gg2.setPaint(Color.RED);
					gg2.translate(current_path.getBounds().width + 10, current_path.getBounds().height + 10);
					gg2.draw(current_path);

					org.jpedal.gui.ShowGUIMessage.showGUIMessage("path", img, "path " + current_path.getBounds());
				}
				catch (Exception e) {
					// tell user and log
					if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
				}
			}
		}

		// second part hack for artus file with thin line
		if ((current_path != null)
				&& (current_path.getBounds().getHeight() == 0 || (thickness > 0.8 && thickness < 0.9 && current_path.getBounds2D().getHeight() < 0.1f))) {

			if (current_path.getBounds2D().getWidth() == 0 && current_path.getBounds2D().getHeight() == 0) {
				// ignore this case
			}
			else
				if (thickness > 1 && current_path.getBounds2D().getWidth() < 1) {
					current_path.moveTo(0, -thickness / 2);
					current_path.lineTo(0, thickness / 2);
				}
				else current_path.moveTo(0, 1);
		}

		if ((current_path != null) && (current_path.getBounds().getWidth() == 0)) current_path.moveTo(1, 0);

		// transform matrix only if needed
		if ((CTM[0][0] == (float) 1.0) && (CTM[1][0] == (float) 0.0) && (CTM[2][0] == (float) 0.0) && (CTM[0][1] == (float) 0.0)
				&& (CTM[1][1] == (float) 1.0) && (CTM[2][1] == (float) 0.0) && (CTM[0][2] == (float) 0.0) && (CTM[1][2] == (float) 0.0)
				&& (CTM[2][2] == (float) 1.0)) {
			// don't waste time if not needed
		}
		else {
			AffineTransform CTM_transform = new AffineTransform(CTM[0][0], CTM[0][1], CTM[1][0], CTM[1][1], CTM[2][0], CTM[2][1]);

			// apply CTM alterations
			if (current_path != null) {

				// transform
				current_path.transform(CTM_transform);
				// if(CTM[0][0]==0 && CTM[1][1]==0 && CTM[0][1]<0 && CTM[1][0]>0){
				// current_path.transform(AffineTransform.getTranslateInstance(0,current_path.getBounds().height/CTM[0][1]));
				// System.out.println("transforms "+CTM_transform+" "+current_path.getBounds());
				// }
			}
			else
				if (current_area != null) current_area.transform(CTM_transform);
		}
		// set to current or clip
		if (is_clip == false) {
			if (current_area == null) current_shape = current_path;
			else current_shape = current_area;
		}
		else current_shape = current_area;

		// track complex clips
		if (cmd == Cmd.n && getSegmentCount() > 2500) {
			this.complexClipCount++;
		}

		return current_shape;
	}

	// ////////////////////////////////////////////////////////////////////////
	/**
	 * add a rectangle to set of shapes
	 */
	final public void appendRectangle(float x, float y, float w, float h) {
		moveTo(x, y);
		lineTo(x + w, y);
		lineTo(x + w, y + h);
		lineTo(x, y + h);
		lineTo(x, y);
		closeShape();
	}

	// ////////////////////////////////////////////////////////////////////////
	/**
	 * start a shape by creating a shape object
	 */
	final public void moveTo(float x, float y) {
		this.shape_primitives.addElement(M);
		this.shape_primitive_x.addElement(x);
		this.shape_primitive_y.addElement(y);

		// add empty values
		this.shape_primitive_x2.addElement(0);
		this.shape_primitive_y2.addElement(0);
		this.shape_primitive_x3.addElement(0);
		this.shape_primitive_y3.addElement(0);

		// delete lines for grouping over boxes
	}

	/**
	 * add a curve to the shape
	 */
	final public void addBezierCurveY(float x, float y, float x3, float y3) {
		this.shape_primitives.addElement(Y);
		this.shape_primitive_x.addElement(x);
		this.shape_primitive_y.addElement(y);

		// add empty values to keep in sync
		// add empty values
		this.shape_primitive_x2.addElement(0);
		this.shape_primitive_y2.addElement(0);
		this.shape_primitive_x3.addElement(x3);
		this.shape_primitive_y3.addElement(y3);
	}

	/**
	 * reset path to empty
	 */
	final public void resetPath() {
		// reset the store
		this.shape_primitives.clear();
		this.shape_primitive_x.clear();
		this.shape_primitive_y.clear();
		this.shape_primitive_x2.clear();
		this.shape_primitive_y2.clear();
		this.shape_primitive_x3.clear();
		this.shape_primitive_y3.clear();

		// and reset winding rule
		this.winding_rule = Path2D.WIND_NON_ZERO;

		// and reset winding rule emulation flag
		this.emulateEvenOddRule = false;
	}

	// /////////////////////////////////////////////////////////////////////////
	/**
	 * set winding rule - even odd
	 */
	final public void setEVENODDWindingRule() {
		this.winding_rule = Path2D.WIND_EVEN_ODD;
	}

	/**
	 * show the shape segments for debugging
	 * 
	 * static final private void showShape( Shape current_shape ) { PathIterator xx = current_shape.getPathIterator( null ); double[] coords = new
	 * double[6]; while( xx.isDone() == false ) { int type = xx.currentSegment( coords ); xx.next(); switch( type ) { case PathIterator.SEG_MOVETO:
	 * LogWriter.writeLog( "MoveTo" + coords[0] + ' ' + coords[1] ); if( ( coords[0] == 0 ) & ( coords[1] == 0 ) ) LogWriter.writeLog( "xxx" ); break;
	 * 
	 * case PathIterator.SEG_LINETO: LogWriter.writeLog( "LineTo" + coords[0] + ' ' + coords[1] ); if( ( coords[0] == 0 ) & ( coords[1] == 0 ) )
	 * LogWriter.writeLog( "xxx" ); break;
	 * 
	 * case PathIterator.SEG_CLOSE: LogWriter.writeLog( "CLOSE" ); break;
	 * 
	 * default: LogWriter.writeLog( "Other" + coords[0] + ' ' + coords[1] ); break; } } }/
	 **/

	/**
	 * number of segments in current shape (0 if no shape or none)
	 */
	public int getSegmentCount() {

		if (this.shape_primitives == null) return 0;
		else {
			return this.shape_primitives.size() - 1;
		}
	}

	public void setClip(boolean b) {
		this.isClip = b;
	}

	public boolean isClip() {
		return this.isClip;
	}

	public boolean requiresEvenOddEmulation() {
		return this.emulateEvenOddRule;
	}

	public int getComplexClipCount() {
		return this.complexClipCount;
	}
}
