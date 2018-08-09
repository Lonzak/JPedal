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
 * JavaFXShape.java
 * ---------------
 */
package org.jpedal.render.output.javafx;

import java.awt.BasicStroke;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;

import org.jpedal.color.PdfPaint;
import org.jpedal.objects.GraphicsState;
import org.jpedal.objects.PdfPageData;
import org.jpedal.render.ShapeFactory;
import org.jpedal.render.output.OutputShape;

public class JavaFXShape extends OutputShape implements ShapeFactory {

	// JavaFX supports both winding rules, it's default is non-zero
	private int windingRule = PathIterator.WIND_NON_ZERO;

	public JavaFXShape(int cmd, int shapeCount, float scaling, Shape currentShape, GraphicsState gs, AffineTransform scalingTransform,
			Point2D midPoint, Rectangle cropBox, int currentColor, int dpCount, int pageRotation, PdfPageData pageData, int pageNumber,
			boolean includeClip) {

		super(cmd, scaling, currentShape, gs, scalingTransform, midPoint, cropBox, currentColor, dpCount, pageRotation, pageData, pageNumber,
				includeClip);

		this.shapeCount = shapeCount;
		this.windingRule = currentShape.getPathIterator(scalingTransform).getWindingRule(); // Added winding rule via shape iterator
		generateShapeFromG2Data(gs, scalingTransform, cropBox);
	}

	@Override
	protected void beginShape() {
		this.pathCommands.add("\n");
		this.pathCommands.add("\tPath path_" + this.shapeCount + " = new Path();");
		this.pathCommands.add("\tObservableList<PathElement> shape_" + this.shapeCount + " = path_" + this.shapeCount + ".getElements();");
		this.pathCommands.add("\taddToGroup.add(path_" + this.shapeCount + ");");// add the shape to the group
	}

	@Override
	protected void lineTo(double[] coords) {
		this.pathCommands.add("\tshape_" + this.shapeCount + ".add(new LineTo(" + coordsToStringParam(coords, 2) + "));");
	}

	@Override
	protected void bezierCurveTo(double[] coords) {
		this.pathCommands.add("\tshape_" + this.shapeCount + ".add(new CubicCurveTo(" + coordsToStringParam(coords, 6) + "));");
	}

	@Override
	protected void quadraticCurveTo(double[] coords) {
		this.pathCommands.add("\tshape_" + this.shapeCount + ".add(new QuadCurveTo(" + coordsToStringParam(coords, 4) + "));");
	}

	@Override
	protected void moveTo(double[] coords) {
		this.pathCommands.add("\tshape_" + this.shapeCount + ".add(new MoveTo(" + coordsToStringParam(coords, 2) + "));");
	}

	@Override
	protected void closePath() {
		this.pathCommands.add("\tshape_" + this.shapeCount + ".add(new ClosePath());");
	}

	@Override
	protected void drawCropBox() {
		this.pathCommands.clear();
		this.pathCommands.add("\n\t\t/**");
		this.pathCommands.add("\t* Crop Box properties");
		this.pathCommands.add("\t*/ ");
		this.pathCommands.add("\tPath path_" + this.shapeCount + " = new Path();");
		this.pathCommands.add("\tObservableList<PathElement> " + "shape_" + this.shapeCount + " = path_" + this.shapeCount + ".getElements();");
		this.pathCommands.add("\taddToGroup.add(path_" + this.shapeCount + ");");// add the cropBox to the group

		double[] coords = { this.cropBox.x, this.cropBox.y };

		this.pathCommands.add("\tshape_" + this.shapeCount + ".add(new MoveTo(" + convertCoords(coords, 2) + "));");
		coords[0] += this.cropBox.width;
		this.pathCommands.add("\tshape_" + this.shapeCount + ".add(new LineTo(" + convertCoords(coords, 2) + "));");
		coords[1] += this.cropBox.height;
		this.pathCommands.add("\tshape_" + this.shapeCount + ".add(new LineTo(" + convertCoords(coords, 2) + "));");
		coords[0] -= this.cropBox.width;
		this.pathCommands.add("\tshape_" + this.shapeCount + ".add(new LineTo(" + convertCoords(coords, 2) + "));");
	}

	@Override
	protected void applyGraphicsStateToPath(GraphicsState gs) {
		int fillType = gs.getFillType();

		if (fillType == GraphicsState.FILL || fillType == GraphicsState.FILLSTROKE) {
			PdfPaint col = gs.getNonstrokeColor();

			if (this.windingRule == PathIterator.WIND_EVEN_ODD) {
				this.pathCommands.add("\tpath_" + this.shapeCount + ".setFillRule(FillRule.EVEN_ODD);");
			}

			float fillOpacity = gs.getAlpha(GraphicsState.FILL);
			if (fillOpacity != 1) {
				this.pathCommands.add("\tpath_" + this.shapeCount + ".setOpacity(" + fillOpacity + ");");
			}

			this.pathCommands.add("\tpath_" + this.shapeCount + ".setStroke(null);"); // The default value is null for all shapes except Line,
																						// Polyline, and Path. The default value is Color.BLACK for
																						// those shapes.

			this.pathCommands.add("\tpath_" + this.shapeCount + ".setFill(Color." + rgbToCSSColor(col.getRGB()) + ");");
			this.currentColor = col.getRGB();
		}

		if (fillType == GraphicsState.STROKE || fillType == GraphicsState.FILLSTROKE) {
			BasicStroke stroke = (BasicStroke) gs.getStroke();

			// allow for any opacity
			float strokeOpacity = gs.getAlpha(GraphicsState.STROKE);
			if (strokeOpacity != 1) {
				this.pathCommands.add("\tpath_" + this.shapeCount + ".setOpacity(" + strokeOpacity + ");"); // JavaFX fills & strokes cannot have
																											// separate opacities.
			}

			if (gs.getOutputLineWidth() != 1) { // attribute double lineWidth; (default 1)
				this.pathCommands.add("\tpath_" + this.shapeCount + ".setStrokeWidth(" + gs.getOutputLineWidth() + ");");
			}

			if (stroke.getMiterLimit() != 10) { // attribute double miterLimit; // (default 10)
				this.pathCommands.add("\tpath_" + this.shapeCount + ".setStrokeMiterLimit(" + ((double) stroke.getLineWidth() * this.scaling) + ");");
			}

			this.pathCommands.add("\tpath_" + this.shapeCount + ".setStrokeLineCap(StrokeLineCap." + determineLineCap(stroke) + ");");
			this.pathCommands.add("\tpath_" + this.shapeCount + ".setStrokeLineJoin(StrokeLineJoin." + determineLineJoin(stroke) + ");");

			PdfPaint col = gs.getStrokeColor();
			this.pathCommands.add("\tpath_" + this.shapeCount + ".setStroke(Color." + rgbToCSSColor(col.getRGB()) + ");");
		}
	}

	/**
	 * Extract line cap attribute javaFX implementation
	 */
	protected static String determineLineCap(BasicStroke stroke) {// javaFX implementation
		// attribute DOMString lineCap; // "butt", "round", "square" (default "butt")
		String attribute;

		switch (stroke.getEndCap()) {
			case (BasicStroke.CAP_ROUND):
				attribute = "ROUND";
				break;
			case (BasicStroke.CAP_SQUARE):
				attribute = "SQUARE";
				break;
			default:
				attribute = "BUTT";
				break;
		}
		return attribute;
	}

	/**
	 * Extract line join attribute javaFX implementation
	 */
	protected static String determineLineJoin(BasicStroke stroke) {// javaFX implementation
		// attribute DOMString lineJoin; // "round", "bevel", "miter" (default "miter")
		String attribute;
		switch (stroke.getLineJoin()) {
			case (BasicStroke.JOIN_ROUND):
				attribute = "ROUND";
				break;
			case (BasicStroke.JOIN_BEVEL):
				attribute = "BEVEL";
				break;
			default:
				attribute = "MITER";
				break;
		}
		return attribute;
	}

	@Override
	public void setShapeNumber(int shapeCount) {
		this.shapeCount = shapeCount;
	}

}