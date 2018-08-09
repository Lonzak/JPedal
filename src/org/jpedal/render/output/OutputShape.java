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
 * OutputShape.java
 * ---------------
 */

package org.jpedal.render.output;

import java.awt.BasicStroke;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jpedal.objects.GraphicsState;
import org.jpedal.objects.PdfPageData;
import org.jpedal.parser.Cmd;

public class OutputShape {
	private static final boolean ENABLE_CROPPING = true;

	// track if shape not closed in FXML
	protected boolean shapeIsOpen = false;

	boolean hasMoveTo = false;

	protected int shapeCount = 0;

	double minXcoord = 999999, minYcoord = 999999;

	protected List<String> pathCommands; // generics not supported in java ME
	protected int currentColor;
	protected Rectangle cropBox;
	private Point2D midPoint;

	private double[] lastVisiblePoint;
	private double[] lastInvisiblePoint;
	private double[] previousPoint;
	private boolean isPathSegmentVisible;
	protected double[] entryPoint;

	boolean includeClip = false;

	private double[] exitPoint;

	// For rectangle larger than cropBox
	private List<Point> largeBox;
	private boolean isLargeBox;
	private boolean largeBoxSideAlternation; // Flag to check that lines are alternating between horizontal and vertical.

	static int debug = 0;

	Rectangle clipBox = null;

	private int pageRotation = 0;

	// applied to the whole page. Default= 1
	protected float scaling;

	private boolean debugPath = false;

	int pathCommand;

	private float[][] ctm;

	private Shape currentShape;
	private int minX, minY;

	// Cmd.F or Cmd.S or Cmd.B for type of shape
	protected int cmd = -1;

	int adjustY = 0;

	public OutputShape(int cmd, float scaling, Shape currentShape, GraphicsState gs, AffineTransform scalingTransform, Point2D midPoint,
			Rectangle cropBox, int currentColor, int dpCount, int pageRotation, PdfPageData pageData, int pageNumber, boolean includeClip) {

		// used by HTML to flag if we clip with clip or in our code
		this.includeClip = includeClip;

		// adjust for rounding
		if (currentShape.getBounds2D().getHeight() > 0.6f && currentShape.getBounds2D().getHeight() < 1.0f
				&& currentShape.getBounds2D().getWidth() > 1f) {

			double diff = currentShape.getBounds2D().getMinY() - currentShape.getBounds().y;
			if (diff > 0.7) {
				this.adjustY = 1;//
			}
		}

		this.cmd = cmd;

		if (cmd == Cmd.Tj) {
			if (cropBox != null) {
				this.minX = (int) cropBox.getMinX();
				this.minY = (int) cropBox.getMinY();
			}
		}
		else {
			this.minX = pageData.getCropBoxX(pageNumber);
			this.minY = pageData.getCropBoxY(pageNumber);
		}

		if (this.debugPath) System.out.println("raw shape=" + currentShape.getBounds() + " minx=" + this.minX + ' ' + this.minY);

		// get any Clip (only rectangle outline)
		Area clip = gs.getClippingShape();
		if (clip != null) {
			this.clipBox = clip.getBounds();
		}
		else this.clipBox = null;

		// adjust for offsets
		if ((this.minX != 0 || this.minY != 0) && this.clipBox != null && cropBox != null) {
			this.clipBox.translate(-this.minX, -this.minY);
		}

		this.currentShape = currentShape;
		this.ctm = gs.CTM;
		this.scaling = scaling;
		this.currentColor = currentColor;
		this.cropBox = cropBox;
		this.midPoint = midPoint;

		this.pageRotation = pageRotation;

		this.isPathSegmentVisible = true;
		this.lastVisiblePoint = new double[2];
		this.lastInvisiblePoint = new double[2];
		this.previousPoint = new double[2];
		this.exitPoint = new double[2];
		this.entryPoint = new double[2];

		this.pathCommands = new ArrayList<String>();
		this.largeBox = new ArrayList<Point>();
		this.isLargeBox = true; // Prove wrong
	}

	boolean isFilled = false, evenStroke = true;

	protected void generateShapeFromG2Data(GraphicsState gs, AffineTransform scalingTransform, Rectangle cropBox) {

		/** Start of fix for pixel perfect lines. Ask Leon if confused **/
		this.isFilled = gs.getFillType() == GraphicsState.FILL;
		int strokeWidth = (int) ((gs.getCTMAdjustedLineWidth() * (this.scaling)) + 0.99); // It is possible it may be worth dividing the scaling by
																							// 1.33f (To get same thickness as PDF @ 100%)
		this.evenStroke = strokeWidth != 0 && strokeWidth % 2 == 0;
		gs.setOutputLineWidth(strokeWidth);
		/** End of fix for pixel perfect lines **/

		PathIterator it = this.currentShape.getPathIterator(scalingTransform);

		// debugPath=cmd==Cmd.n && currentShape.getBounds().x==0 && currentShape.getBounds().y==44;

		this.shapeIsOpen = true;

		beginShape();

		boolean firstDrawCommand = true && ENABLE_CROPPING;

		if (this.debugPath) {
			System.out.println("About to generate commands for shape with bounds" + this.currentShape.getBounds());
			System.out.println("------------------------------------------------");
			System.out.println("crop bounds=" + cropBox.getBounds());
			System.out.println("shape bounds=" + this.currentShape.getBounds());
			System.out.println("minX=" + this.minX + " minY=" + this.minY);
			if (this.clipBox != null) System.out.println("clip bounds=" + this.clipBox.getBounds());
		}

		int segCount = 0;

		double[] lastPt;

		while (!it.isDone()) {
			double[] coords = { 0, 0, 0, 0, 0, 0 };

			this.pathCommand = it.currentSegment(coords);
			segCount++;

			if (cropBox != null) {
				for (int ii = 0; ii < 3; ii++) {
					coords[ii * 2] = coords[ii * 2] - this.minX;
					coords[(ii * 2) + 1] = coords[(ii * 2) + 1] - this.minY;
				}
			}

			if (this.debugPath) System.out.println("\n=======Get pathCommand segment " + (int) coords[0] + ' ' + (int) coords[1] + ' '
					+ (int) coords[2] + ' ' + (int) coords[3] + ' ' + (int) coords[4] + ' ' + (int) coords[5]);

			boolean isCropBoxDrawn = false;
			if (this.cmd != Cmd.n) isCropBoxDrawn = checkLargeBox(coords, this.pathCommand);

			// isCropBoxDrawn=false;
			// avoid duplicate lineto if box drawn (ie sample_pdfs_html/general/test22.pdf page 1
			if (isCropBoxDrawn) {
				it.next();
				continue;
			}

			if (firstDrawCommand) { // see if starts visible
				int dx = (int) coords[getCoordOffset(this.pathCommand)];
				int dy = (int) coords[getCoordOffset(this.pathCommand) + 1];

				// use CropBox or clip (whichever is smaller)
				Rectangle cropBoxtoTest = cropBox;

				if (this.clipBox != null) {
					cropBoxtoTest = this.clipBox;
				}

				// avoid rounding error in comparison
				if (cropBoxtoTest != null && cropBoxtoTest.width > 1) {
					if (dx == cropBoxtoTest.x) dx = dx + 1;
					else
						if (dx == (cropBoxtoTest.x + cropBoxtoTest.width)) dx = dx - 1;
				}

				this.isPathSegmentVisible = cropBoxtoTest == null || cropBoxtoTest.contains(dx, dy);
				firstDrawCommand = false;

				if (this.debugPath) System.out.println("isPathSegmentVisible=" + this.isPathSegmentVisible);

			}

			if (this.pathCommand == PathIterator.SEG_CUBICTO) {

				boolean isPointVisible = testDrawLimits(coords, PathIterator.SEG_CUBICTO);
				if (this.debugPath) System.out.println("PathIterator.SEG_CUBICTO isPointVisible=" + isPointVisible);

				if (isPointVisible && this.isPathSegmentVisible) bezierCurveTo(coords);

				// flag incase next item is a line
				this.isPathSegmentVisible = isPointVisible;

			}
			else
				if (this.pathCommand == PathIterator.SEG_LINETO) {

					lastPt = new double[] { this.previousPoint[0], this.previousPoint[1] };

					boolean isPointVisible = testDrawLimits(coords, PathIterator.SEG_LINETO);

					if (this.debugPath) System.out.println("PathIterator.SEG_LINETO isPointVisible=" + isPointVisible + " isPathSegmentVisible="
							+ this.isPathSegmentVisible + ' ' + (int) coords[0] + ' ' + (int) coords[1]);

					if (isPointVisible && this.isPathSegmentVisible) {

						if (this.debugPath) System.out.println("pdf.lineTo(" + coordsToStringParam(coords, 2) + ')');

						lineTo(coords);

					}
					else
						if (isPointVisible != this.isPathSegmentVisible) {

							if (!this.isPathSegmentVisible) {

								if (gs.getFillType() != GraphicsState.FILL) {
									moveTo(this.entryPoint);
									this.hasMoveTo = true;

									if (this.debugPath) System.out.println("pdf.moveTo(" + coordsToStringParam(coords, 2));
								}
								else {
									// if it has not entered box yet, work out which corner to start at
									if (!this.hasMoveTo && !cropBox.contains(lastPt[0], lastPt[1])) {

										double cx = cropBox.x + cropBox.width;
										double cy = cropBox.y + cropBox.height;

										if (cx > this.lastInvisiblePoint[0]) cx = cropBox.x;

										if (this.lastInvisiblePoint[1] < cropBox.y) cy = cropBox.y;
										else
											if (cy < this.lastInvisiblePoint[1]) cy = cropBox.y;

										double[] cornerEntryPt = new double[] { cx, cy };
										this.hasMoveTo = true;
										moveTo(cornerEntryPt);

										if (this.debugPath) System.out.println("pdf.moveTo(" + coordsToStringParam(cornerEntryPt, 2));

									}

									lineTo(this.entryPoint);
								}

								lineTo(coords);

								if (this.debugPath) System.out.println("pdf.lineTo(" + coordsToStringParam(coords, 2) + ");");

								this.isPathSegmentVisible = true;
							}
							else {

								lineTo(this.exitPoint);

								this.isPathSegmentVisible = false;

								if (this.debugPath) System.out.println("pdf.lineTo(" + coordsToStringParam(this.exitPoint, 2) + ");");
							}
						}
				}
				else
					if (this.pathCommand == PathIterator.SEG_QUADTO) {

						if (this.debugPath) System.out.println("PathIterator.SEG_QUADTO");

						if (testDrawLimits(coords, PathIterator.SEG_QUADTO)) {

							if (this.debugPath) System.out.println("pdf.quadraticCurveTo(" + coordsToStringParam(coords, 4));

							quadraticCurveTo(coords);
							this.isPathSegmentVisible = true;

						}
						else {
							this.isPathSegmentVisible = false;
						}
					}
					else
						if (this.pathCommand == PathIterator.SEG_MOVETO) {
							if (this.debugPath) System.out.println("PathIterator.SEG_MOVETO");
							if (testDrawLimits(coords, PathIterator.SEG_MOVETO)) {
								this.isPathSegmentVisible = true;
								if (this.debugPath) System.out.println("pdf.moveTo(" + coordsToStringParam(coords, 2) + ");");
								this.hasMoveTo = true;
								moveTo(coords);

							}
							else {
								this.isPathSegmentVisible = false;
							}
						}
						else
							if (this.pathCommand == PathIterator.SEG_CLOSE) {
								if (this.debugPath) System.out.println("pdf.closePath();");
								closePath();
							}
			it.next();
		}

		if (this.pathCommands.size() == 1) {
			// No commands made it through
			this.pathCommands.clear();
		}
		else {

			if (this.cmd == Cmd.Tj || (!this.isPathSegmentVisible && gs.getFillType() != GraphicsState.FILL)) {

				this.shapeIsOpen = false;

				finishShape();

				if (this.debugPath) System.out.println("pdf.closePath();");
			}
			applyGraphicsStateToPath(gs);
		}

		// ignore rect if totally outside clip
		if (this.cmd == Cmd.S && segCount == 6 && this.clipBox != null && this.currentShape.getBounds().width > 10
				&& this.currentShape.getBounds().height > 10 && this.clipBox.x >= this.currentShape.getBounds().x
				&& this.clipBox.x + this.clipBox.width <= this.currentShape.getBounds().x + this.currentShape.getBounds().width
				&& this.clipBox.y >= this.currentShape.getBounds().y
				&& this.clipBox.y + this.clipBox.height <= this.currentShape.getBounds().y + this.currentShape.getBounds().height) {
			this.pathCommands.clear();
			if (this.debugPath) System.out.println("shape removed");
		}

		// only does something on FXML
		checkShapeClosed();
		this.isFilled = false;
	}

	public void checkShapeClosed() {
		// only does something on FXML which overrides this
	}

	// empty version
	protected void moveTo(double[] coords) {}

	// empty version
	protected void quadraticCurveTo(double[] coords) {}

	// empty version
	protected void lineTo(double[] coords) {}

	// empty version
	protected void closePath() {}

	// empty version
	protected void finishShape() {}

	// empty version
	protected void beginShape() {}

	// empty version
	protected void bezierCurveTo(double[] coords) {}

	/**
	 * trim if needed
	 * 
	 * @param i
	 * @return
	 * 
	 *         (note duplicate in HTMLDisplay)
	 */
	private String setPrecision(double i, boolean isX) {

		// New pixel perfect version
		String value = "";

		if (this.isFilled || this.evenStroke) {
			value = "" + (int) (i);
		}
		else {
			int num = (int) i;
			value += num + ".5";
		}

		// track corner so we can reuse if needed for shading
		if (isX && i < this.minXcoord) this.minXcoord = i;
		else
			if (!isX && i < this.minYcoord) this.minYcoord = i;

		return value;
	}

	/**
	 * Extracts information out of graphics state to use in HTML - empty version
	 */
	protected void applyGraphicsStateToPath(GraphicsState gs) {}

	/**
	 * Extract line cap attribute
	 */
	protected static String determineLineCap(BasicStroke stroke) {
		// attribute DOMString lineCap; // "butt", "round", "square" (default "butt")
		String attribute;

		switch (stroke.getEndCap()) {
			case (BasicStroke.CAP_ROUND):
				attribute = "round";
				break;
			case (BasicStroke.CAP_SQUARE):
				attribute = "square";
				break;
			default:
				attribute = "butt";
				break;
		}
		return attribute;
	}

	/**
	 * Extract line join attribute
	 */
	protected static String determineLineJoin(BasicStroke stroke) {
		// attribute DOMString lineJoin; // "round", "bevel", "miter" (default "miter")
		String attribute;
		switch (stroke.getLineJoin()) {
			case (BasicStroke.JOIN_ROUND):
				attribute = "round";
				break;
			case (BasicStroke.JOIN_BEVEL):
				attribute = "bevel";
				break;
			default:
				attribute = "miter";
				break;
		}
		return attribute;
	}

	/**
	 * @return true if coordinates lie with visible area
	 */
	private boolean testDrawLimits(double[] coords, int pathCommand) {

		// html uses clipping so we do not alter shapes
		if (this.includeClip) return true;

		if (this.debugPath) System.out.println("testDrawLimits coords[0] + coords[1]=" + (int) coords[0] + ' ' + (int) coords[1]);

		if (!ENABLE_CROPPING) {
			return true;
		}

		int offset = getCoordOffset(pathCommand);

		// assume not visible by default
		boolean isCurrentPointVisible;

		if (this.debugPath) System.out.println("crop=" + this.cropBox + " clip=" + this.clipBox);

		// use CropBox or clip (whichever is smaller)
		Rectangle cropBox = this.cropBox;

		if (this.clipBox != null) {
			cropBox = cropBox.intersection(this.clipBox);

			if (this.debugPath) System.out.println("merged crop=" + cropBox);
		}

		/**
		 * turn co-ords into a Point (the 1s are to allow for rounding errors)
		 */
		double x = coords[offset];
		if (x > cropBox.x + 1) x = x - 1;
		double y = coords[offset + 1];
		if (y > cropBox.y + 1) y = y - 1;
		double[] point = { x, y };

		if (cropBox.contains(point[0], point[1])) {
			this.lastVisiblePoint = point;
			isCurrentPointVisible = true;

			if (this.debugPath) System.out.println("Point visible in cropBox or clip");

		}
		else { // this point outside visible area
			this.lastInvisiblePoint = point;
			isCurrentPointVisible = false;

			if (this.debugPath) System.out.println("Point invisible " + (int) point[0] + ' ' + (int) point[1] + " crop=" + cropBox.getBounds());
		}

		if (!isCurrentPointVisible && this.isPathSegmentVisible) {

			if (this.debugPath) System.out.println("Case1 this point " + (int) x + ',' + (int) y + " invisible and isPathSegmentVisible");

			findSwitchPoint(point, true);
		}
		else
			if (isCurrentPointVisible && !this.isPathSegmentVisible) {

				if (this.debugPath) System.out.println("Case2 this point " + (int) x + ',' + (int) y + " visible and isPathSegment invisible");

				findSwitchPoint(point, false);

			}
			else {

				if (this.debugPath) System.out.println("Case3 NOT COVERED isCurrentPointVisible=" + isCurrentPointVisible + " isPathSegmentVisible"
						+ this.isPathSegmentVisible);
			}

		// Check whether this point and last point cross crop box.
		if (!isCurrentPointVisible && (!cropBox.contains(this.previousPoint[0], this.previousPoint[1])) && pathCommand == PathIterator.SEG_LINETO) {

			if (this.debugPath) System.out.println("checkTraversalPoints");

			checkTraversalPoints(point, this.previousPoint, cropBox);

		}

		this.previousPoint = point;

		return isCurrentPointVisible;
	}

	/**
	 * Figure out where to draw a line if the given points do not lie within the cropbox but should draw a line because they pass over it.
	 */
	private void checkTraversalPoints(double[] startPoint, double[] endPoint, Rectangle cropBox) {
		boolean xTraversal = (endPoint[0] < cropBox.x && startPoint[0] > (cropBox.x + cropBox.width))
				|| (startPoint[0] < cropBox.x && endPoint[0] > (cropBox.x + cropBox.width));
		boolean yTraversal = (endPoint[1] < cropBox.y && startPoint[1] > (cropBox.y + cropBox.height))
				|| (startPoint[1] < cropBox.y && endPoint[1] > (cropBox.y + cropBox.height));
		boolean completeCropBoxMiss = isCompleteCropBoxMiss(startPoint, endPoint, cropBox);

		if (this.debugPath) {

			System.out.println("checkTraversalPoints xtrav=" + xTraversal + " yTrav=" + yTraversal + " completeCropBoxMiss=" + completeCropBoxMiss
					+ ' ' + (endPoint[0] < cropBox.x && startPoint[0] > (cropBox.x + cropBox.width)));
			System.out.println("start=" + (int) startPoint[0] + ' ' + (int) startPoint[1] + "  end=" + (int) endPoint[0] + ' ' + (int) endPoint[1]);
			// System.out.println("cropBox="+cropBox.x+" "+cropBox.y+" "+cropBox.width+" "+cropBox.height);
		}

		if (!xTraversal && !yTraversal) {
			return;
		}

		// double xSide = startPoint[0] - endPoint[0];
		// double ySide = startPoint[1] - endPoint[1];

		// System.out.println((int)startPoint[0]+" "+(int)startPoint[1]+" end="+(int)endPoint[0]+" "+(int)endPoint[1]+" "+cropBox);

		// work out gradient of line
		double m = (endPoint[1] - startPoint[1]) / (endPoint[0] - startPoint[0]);
		double rawM = m;

		// allow for vertical line
		if (Math.abs(endPoint[0] - startPoint[0]) < 1) m = 0;

		if (m < 0.001) m = 0;

		// and workout y
		if (m == 0) {

			if (rawM < 0 && endPoint[0] < 0 && (endPoint[0] < startPoint[0])) {
				this.exitPoint = calcCrossoverPoint(endPoint, cropBox, m, startPoint);
				this.entryPoint = calcCrossoverPoint(startPoint, cropBox, m, endPoint);
			}
			else {
				this.entryPoint = calcCrossoverPoint(endPoint, cropBox, m, startPoint);
				this.exitPoint = calcCrossoverPoint(startPoint, cropBox, m, endPoint);
			}
		}
		else {
			this.exitPoint = calcCrossoverPoint(endPoint, cropBox, m, startPoint);
			this.entryPoint = calcCrossoverPoint(startPoint, cropBox, m, endPoint);
		}

		if (this.debugPath) {
			System.out.println("entry=" + this.entryPoint[0] + ' ' + this.entryPoint[1] + " exit=" + this.exitPoint[0] + ' ' + this.exitPoint[1]);
			System.out.println("XXpdf.lineTo(" + coordsToStringParam(this.entryPoint, 2) + ");");
			System.out.println("XXpdf.lineTo(" + coordsToStringParam(this.exitPoint, 2) + "); hasMoveTo=" + this.hasMoveTo);
		}

		// if it has not entered box yet, work out which corner to start at (see awjune p13)
		if (!this.hasMoveTo && endPoint[0] < 0 && startPoint[0] > this.entryPoint[0] && this.entryPoint[0] > this.exitPoint[0]) {

			double cx = cropBox.x + cropBox.width;
			double cy = cropBox.y + cropBox.height;
			if (cx > this.lastVisiblePoint[0]) cx = cropBox.x;
			if (cy < this.lastVisiblePoint[1]) cy = cropBox.y;

			double[] cornerEntryPt = new double[] { cx, cy };
			this.hasMoveTo = true;
			moveTo(cornerEntryPt);

			if (this.debugPath) System.out.println("pdf.moveTo(" + coordsToStringParam(cornerEntryPt, 2));
			lineTo(this.exitPoint);
			lineTo(this.entryPoint);
		}
		else {
			lineTo(this.entryPoint);
			lineTo(this.exitPoint);
		}
	}

	private static double[] calcCrossoverPoint(double[] pt, Rectangle cropBox, double m, double[] otherPt) {

		double x1 = 0, y1 = 0;

		/**
		 * case 1/2 and -above top of of cropBox or below
		 */
		// (use cropBox vertical line as Y and check if in bounds to make sure not a side line)
		if (pt[1] < cropBox.y || pt[1] > cropBox.y + cropBox.height) {

			// choose y to match
			if (pt[1] < cropBox.y) y1 = cropBox.y;
			else y1 = cropBox.y + cropBox.height;

			if (m == 0) x1 = pt[0];
			else x1 = ((y1 - pt[1]) / m) + pt[0];

			// check it crosses top line and not one of sides
			if (x1 < cropBox.x || x1 > cropBox.x + cropBox.width) { // actually it crosses as side line
				// choose left or right side
				if (x1 < cropBox.x) x1 = cropBox.x;
				else x1 = cropBox.x + cropBox.width;

				// and workout y
				y1 = (m * (x1 - pt[0])) + pt[1];
			}

			/**
			 * case 3/4 and -to left or right of cropBox
			 */
			// (use cropBox horizontal line as X and check if in bounds to make sure not a vertical)
		}
		else
			if (pt[0] < cropBox.x || pt[0] > cropBox.x + cropBox.width) {

				// choose left or right side
				if (pt[0] < cropBox.x) x1 = cropBox.x;
				else x1 = cropBox.x + cropBox.width;

				y1 = (m * (x1 - pt[0])) + pt[1];

				// check it crosses side line and not one of verticals
				if (y1 < cropBox.y || y1 > cropBox.y + cropBox.height) { // actually it crosses as side line
					// choose y to match
					if (pt[1] < cropBox.y) y1 = cropBox.y;
					else y1 = cropBox.y + cropBox.height;

					x1 = ((y1 - pt[1]) / m) + pt[0];

				}
			}

		return new double[] { x1, y1 };
	}

	/**
	 * Figure out where the line disappears or reappears off the cropbox boundary.
	 * 
	 * @param point
	 *            The current point
	 * @param exit
	 *            true if you wish to find an exit point (one that marks a disappearance)
	 */
	private void findSwitchPoint(double[] point, boolean exit) {

		double[] lastPoint;
		double[] switchPoint = new double[2];

		lastPoint = exit ? this.lastVisiblePoint : this.lastInvisiblePoint;

		if (this.debugPath) {
			if (exit) System.out.println("Find point of exit lastPoint=" + (int) lastPoint[0] + ' ' + (int) lastPoint[1] + " current="
					+ (int) point[0] + ' ' + (int) point[1]);
			else System.out.println("Find point of entry lastPoint=" + (int) lastPoint[0] + ' ' + (int) lastPoint[1] + " current=" + (int) point[0]
					+ ' ' + (int) point[1]);
		}

		if (!exit) {
			double[] tmp = point;
			point = lastPoint;
			lastPoint = tmp;
		}

		double xSide = point[0] - lastPoint[0];
		double ySide = point[1] - lastPoint[1];

		// To indicate whether a coordinate has been found
		boolean xFound = false;
		boolean yFound = false;

		if (this.clipBox != null && point[0] >= this.clipBox.width + this.clipBox.x) {
			switchPoint[0] = this.clipBox.width + this.clipBox.x;
			xFound = true;
		}
		else
			if (point[0] >= this.cropBox.width + this.cropBox.x) {
				switchPoint[0] = this.cropBox.width + this.cropBox.x;
				xFound = true;
			}
			else
				if (this.clipBox != null && point[0] < this.clipBox.x) {
					switchPoint[0] = this.clipBox.x;
					xFound = true;
				}
				else
					if (point[0] < this.cropBox.x) {
						switchPoint[0] = this.cropBox.x;
						xFound = true;
					}

		if (this.clipBox != null && point[1] > this.clipBox.height + this.clipBox.y) {
			switchPoint[1] = this.clipBox.height + this.clipBox.y;
			yFound = true;
		}
		else
			if (point[1] > this.cropBox.height + this.cropBox.y) {
				switchPoint[1] = this.cropBox.height + this.cropBox.y;
				yFound = true;
			}
			else
				if (this.clipBox != null && point[1] < this.clipBox.y) {
					switchPoint[1] = this.clipBox.y;
					yFound = true;
				}
				else
					if (point[1] < this.cropBox.y) {
						switchPoint[1] = this.cropBox.y;
						yFound = true;
					}

		if (yFound) {
			if (xSide == 0) {
				switchPoint[0] = point[0];
			}
			else {
				double tan = xSide / ySide;
				if (ySide == 0) tan = 1;
				double distanceToCropY = switchPoint[1] - point[1];
				switchPoint[0] = point[0] + (tan * distanceToCropY);
			}
		}
		if (xFound) {
			if (ySide == 0) {
				switchPoint[1] = point[1];
			}
			else {
				double tan = ySide / xSide;
				double distanceToCropX = switchPoint[0] - point[0];
				switchPoint[1] = point[1] + (tan * distanceToCropX);
			}
		}

		if (exit) {
			this.exitPoint = switchPoint;
			if (this.debugPath) System.out.println("returns exit=" + (int) switchPoint[0] + ' ' + (int) switchPoint[1]);

		}
		else {
			this.entryPoint = switchPoint;
			if (this.debugPath) System.out.println("returns entry=" + (int) switchPoint[0] + ' ' + (int) switchPoint[1]);
		}
	}

	/**
	 * Add the coords to the large box list if it might possibly be part of a rectangle.
	 * 
	 * @param coords
	 * @param pathCommand
	 */
	private boolean checkLargeBox(double[] coords, int pathCommand) {

		boolean isDrawn = false;

		if (this.debugPath) System.out.println("check large " + (int) coords[0] + ' ' + (int) coords[1]);

		if (!this.isLargeBox && (pathCommand != PathIterator.SEG_LINETO || pathCommand != PathIterator.SEG_MOVETO)) {
			return false;
		}

		double px = coords[getCoordOffset(pathCommand)], py = coords[getCoordOffset(pathCommand) + 1];
		double[] adjustedCords = this.correctCoords(new double[] { px, py });
		px = adjustedCords[0];
		py = adjustedCords[1];

		Point point = new Point((int) px, (int) py);

		if (this.largeBox.isEmpty()) {
			this.largeBox.add(point);
		}
		else {
			Point2D last = this.largeBox.get(this.largeBox.size() - 1);
			double xSide = last.getX() - point.getX();
			double ySide = last.getY() - point.getY();

			// First time we can compare so check here and ignore below.
			if (this.largeBox.size() == 1) {

				if (ySide != 0) // allow for div by zero
				this.largeBoxSideAlternation = xSide / ySide != 0;
				else this.largeBoxSideAlternation = true;
			}

			// If this point and the last point do not form a horizontal or vertical line its not part of a large rectangular shape.
			if (xSide / ySide == 0 || ySide / xSide == 0) {

				boolean currentSide = xSide / ySide == 0;

				// Ignore if its part of a continous line. The continous line could be going back on it self but currently not accounted for.
				if (this.largeBox.size() > 1 || (currentSide != this.largeBoxSideAlternation)) {
					this.largeBox.add(point);
					this.largeBoxSideAlternation = xSide / ySide == 0;
				}
			}
			else
				if (!point.equals(this.largeBox.get(this.largeBox.size() - 1))) {
					this.isLargeBox = false;
					return false;
				}

			// Check if we have enough point to see if it is larger than the whole page.
			if (this.largeBox.size() >= 4 && isLargerThanCropBox()) {
				drawCropBox();
				isDrawn = true;
			}
		}

		return isDrawn;
	}

	/**
	 * return true if the coordinates in this path specify a box larger than the cropbox.
	 */
	private boolean isLargerThanCropBox() {
		if (!this.isLargeBox || this.cropBox == null) {
			return false;
		}

		Point2D x = this.largeBox.get(this.largeBox.size() - 4);
		Point2D y = this.largeBox.get(this.largeBox.size() - 3);
		Point2D z = this.largeBox.get(this.largeBox.size() - 2);
		Point2D w = this.largeBox.get(this.largeBox.size() - 1);

		int shortestSide = this.cropBox.width < this.cropBox.height ? this.cropBox.width : this.cropBox.height;
		// Can not cover the page if this is true.
		if (x.distance(y) < shortestSide || y.distance(z) < shortestSide || z.distance(w) < shortestSide) {
			return false;
		}

		int outsideCount = 0;

		if (!this.cropBox.contains(x.getX(), x.getY())) outsideCount++;
		if (!this.cropBox.contains(y.getX(), y.getY())) outsideCount++;
		if (!this.cropBox.contains(z.getX(), z.getY())) outsideCount++;
		if (!this.cropBox.contains(w.getX(), w.getY())) outsideCount++;

		if (outsideCount <= 2) {
			return false;
		}

		Set<Point2D> points = new HashSet<Point2D>();
		points.add(x);
		points.add(y);
		points.add(z);
		points.add(w);

		outsideCount = 0;

		// Test that points lie in correct areas to justify a box covering the page.
		for (int hOffset = -1; hOffset <= 1; hOffset++) {
			for (int wOffset = -1; wOffset <= 1; wOffset++) {
				if (hOffset == 0 && wOffset == 0) { // Would mean the test rectangle is same as cropbox so ignore
					continue;
				}
				Rectangle outside = new Rectangle(this.cropBox);
				outside.translate(wOffset * this.cropBox.width, hOffset * this.cropBox.height);
				if (doesPointSetCollide(points, outside)) outsideCount++;
			}
		}

		return outsideCount >= 3;
	}

	/**
	 * return true if any of the given points are contained in the given rectangle
	 */
	private static boolean doesPointSetCollide(Set<Point2D> points, Rectangle rect) {
		// converted so java ME compilies
		for (Object point : points) {
			Point2D pt = (Point2D) point;
			if (rect.contains(pt)) {
				return true;
			}
		}
		return false;
	}

	protected void drawCropBox() {}

	private double getClosestCropEdgeX(double x) {
		return x < this.cropBox.x + (this.cropBox.width / 2) ? this.cropBox.x : this.cropBox.x + this.cropBox.width;
	}

	private double getClosestCropEdgeY(double y) {
		return y < this.cropBox.y + (this.cropBox.height / 2) ? this.cropBox.y : this.cropBox.y + this.cropBox.height;
	}

	/**
	 * Convert from PDF coords to java coords.
	 */
	private double[] correctCoords(double[] coords) {

		if (this.cropBox != null) {
			int offset;

			switch (this.pathCommand) {
				case (PathIterator.SEG_CUBICTO):
					offset = 4;
					break;
				case (PathIterator.SEG_QUADTO):
					offset = 2;
					break;
				default:
					offset = 0;
					break;
			}

			// ensure fits
			if (offset > coords.length) offset = coords.length - 2;

			for (int i = 0; i < offset + 2; i += 2) {

				// adjust for rounding error
				coords[i + 1] = coords[i + 1] + this.adjustY;

				coords[i] = coords[i] - this.midPoint.getX() + this.minX;
				coords[i] += this.cropBox.width / 2;

				coords[i + 1] = coords[i + 1] - this.midPoint.getY() + this.minY;
				coords[i + 1] = 0 - coords[i + 1];
				coords[i + 1] += this.cropBox.height / 2;

			}
		}

		return coords;
	}

	/**
	 * Return the index of the start coordinate
	 */
	private static int getCoordOffset(int pathCommand) {
		int offset;

		switch (pathCommand) {
			case (PathIterator.SEG_CUBICTO):
				offset = 4;
				break;
			case (PathIterator.SEG_QUADTO):
				offset = 2;
				break;
			default:
				offset = 0;
				break;
		}
		return offset;
	}

	/**
	 * Tests whether the line between the two given points crosses over the crop box.
	 * 
	 * @return true if it misses completely.
	 */
	private static boolean isCompleteCropBoxMiss(double[] start, double[] end, Rectangle cropBox) {
		int xLimMin = cropBox.x;
		int xLimMax = xLimMin + cropBox.width;
		int yLimMin = cropBox.y;
		int yLimMax = xLimMax + cropBox.height;

		return ((start[0] < xLimMin && end[0] < xLimMin) || (start[0] > xLimMax && end[0] > xLimMax))
				&& ((start[1] < yLimMin && end[1] < yLimMin) || (start[1] > yLimMax && end[1] > yLimMax));
	}

	/**
	 * Coverts an array of numbers to a String for JavaScript parameters. Removes cropbox offset.
	 * 
	 * @param coords
	 *            Numbers to change
	 * @param count
	 *            Use up to count doubles from coords array
	 * @return String Bracketed stringified version of coords (note numbers rounded to nearest int to keep down filesize)
	 */
	protected String coordsToStringParam(double[] coords, int count) {
		// make copy factoring in size
		int size = coords.length;
		double[] copy = new double[size];
		System.arraycopy(coords, 0, copy, 0, size);

		coords = correctCoords(copy);

		return convertCoords(coords, count);
	}

	protected String convertCoords(double[] coords, int count) {

		String result = "";

		int cropBoxW = 0, cropBoxH = 0;
		if (this.cropBox != null) {
			cropBoxW = this.cropBox.width;
			cropBoxH = this.cropBox.height;
		}

		switch (this.pageRotation) {
			case 90:

				// for each set of coordinates, set value
				for (int i = 0; i < count; i = i + 2) {
					if (i != 0) {
						result += ",";
					}

					if (this.ctm[0][0] == 0 && this.ctm[1][1] == 0 && this.ctm[0][1] > 0 && (this.ctm[1][0] < 0 || Math.abs(this.ctm[1][0]) < 0.5)) {// adjust
																																						// to
																																						// take
																																						// account
																																						// negative
																																						// shape
																																						// offset
						result += setPrecision(((cropBoxH - coords[i + 1]) * this.scaling), false);
						result += ",";
						result += setPrecision(coords[i] * this.scaling, (i & 1) != 1);
					}
					else {

						result += setPrecision(((cropBoxH - coords[i + 1]) * this.scaling), false);

						result += ",";
						result += setPrecision(coords[i] * this.scaling, (i & 1) != 1);// - (i%2 == 1 ? cropBox.x : cropBox.y) );
					}
				}
				break;

			case 180: {

				// convert x and y values to output coords from PDF
				for (int i = 0; i < count; i = i + 2) {

					// handle x values
					if (i != 0) {
						result += ",";
					}
					result += setPrecision((cropBoxW - coords[i]) * this.scaling, true);// - (i%2 == 1 ? cropBox.x : cropBox.y) );

					// handle y values
					if (i + 1 != 0) {
						result += ",";
					}
					// System.out.println(cropBox.height+" "+coords[i+1]);
					result += setPrecision(((cropBoxH - coords[i + 1]) * this.scaling), false);// - (i%2 == 1 ? cropBox.x : cropBox.y) );
				}
			}
				break;

			case 270: {
				// for each set of coordinates, set value
				for (int i = 0; i < count; i = i + 2) {
					if (i != 0) {
						result += ",";
					}

					if (this.ctm[0][0] == 0 && this.ctm[1][1] == 0 && this.ctm[0][1] > 0 && (this.ctm[1][0] < 0 || Math.abs(this.ctm[1][0]) < 0.5)) {// adjust
																																						// to
																																						// take
																																						// account
																																						// negative
																																						// shape
																																						// offset
						result += setPrecision(((cropBoxH - coords[i + 1]) * this.scaling), false);
						result += ",";
						result += setPrecision(coords[i] * this.scaling, (i & 1) != 1);

					}
					else {

						result += setPrecision(((coords[i + 1]) * this.scaling), false);
						// result += setPrecision((cropBox.height-coords[i+1]-pageData.getCropBoxY(pageNumber))*scaling);

						result += ",";
						result += setPrecision((cropBoxW - coords[i]) * this.scaling, (i & 1) != 1);// - (i%2 == 1 ? cropBox.x : cropBox.y) );
						// result += setPrecision((coords[i]-(pageData.getCropBoxX(pageNumber)/2))*scaling);//- (i%2 == 1 ? cropBox.x : cropBox.y) );
					}
				}
			}
				break;

			default: {

				// convert x and y values to output coords from PDF
				for (int i = 0; i < count; i = i + 2) {

					// handle x values
					if (i != 0) {
						result += ",";
					}
					result += setPrecision(coords[i] * this.scaling, true);// - (i%2 == 1 ? cropBox.x : cropBox.y) );

					// handle y values
					if (i + 1 != 0) {
						result += ",";
					}
					result += setPrecision((coords[i + 1] * this.scaling), false);// - (i%2 == 1 ? cropBox.x : cropBox.y) );
				}
			}
		}
		return result;
	}

	/**
	 * Formats an int to CSS rgb(r,g,b) string
	 */
	public static String rgbToCSSColor(int raw) {
		int r = (raw >> 16) & 255;
		int g = (raw >> 8) & 255;
		int b = raw & 255;

		return "rgb(" + r + ',' + g + ',' + b + ')';
	}

	public String getContent() {
		int size = this.pathCommands.size();

		StringBuilder result = new StringBuilder(size * 15);

		for (int i = 0; i < size; i++) {

			// @TODO Hack to backspace out tab so as to not break test.
			if (i != 0) {
				result.append('\t');
			}
			result.append(this.pathCommands.get(i));
			if (i != (size - 1)) {
				result.append('\n');
			}
		}

		return result.toString();
	}

	public boolean isEmpty() {
		return this.pathCommands.isEmpty();
	}

	public int getShapeColor() {
		return this.currentColor;
	}

	public double getMinXcoord() {
		return this.minXcoord;
	}

	public double getMinYcoord() {
		return this.minYcoord;
	}
}
