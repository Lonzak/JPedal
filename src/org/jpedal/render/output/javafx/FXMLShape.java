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
 * FXMLShape.java
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
import org.jpedal.render.output.OutputDisplay;
import org.jpedal.render.output.OutputShape;

public class FXMLShape extends OutputShape implements ShapeFactory {

	private GraphicsState gs;
	private int windingRule = PathIterator.WIND_NON_ZERO;// FXML supports both winding rules, it's default is non-zero

	public FXMLShape(int cmd, int shapeCount, float scaling, Shape currentShape, GraphicsState gs, AffineTransform scalingTransform,
			Point2D midPoint, Rectangle cropBox, int currentColor, int dpCount, int pageRotation, PdfPageData pageData, int pageNumber,
			boolean includeClip) {

		super(cmd, scaling, currentShape, gs, scalingTransform, midPoint, cropBox, currentColor, dpCount, pageRotation, pageData, pageNumber,
				includeClip);

		this.shapeCount = shapeCount;
		this.gs = gs;

		this.windingRule = currentShape.getPathIterator(scalingTransform).getWindingRule(); // Added winding rule via shape iterator

		generateShapeFromG2Data(gs, scalingTransform, cropBox);
	}

	@Override
	public void checkShapeClosed() {
		if (this.shapeIsOpen) {
			this.shapeIsOpen = false;
			finishShape();
		}
	}

	@Override
	protected void beginShape() {

		String strokeWidth = "", strokeLineCap = "", strokeLineJoin = "", strokeColor = "", strokeMiterLimit = "", fillColor = "", fillRule = "", opacity = "";
		int fillType = this.gs.getFillType();

		if (fillType == GraphicsState.FILL || fillType == GraphicsState.FILLSTROKE) {
			PdfPaint col = this.gs.getNonstrokeColor();

			if (this.windingRule == PathIterator.WIND_EVEN_ODD) {
				fillRule = " fillRule=\"even_odd\"";
			}

			float fillOpacity = this.gs.getAlpha(GraphicsState.FILL);
			if (fillOpacity != 1) {
				opacity = " opacity=\"" + fillOpacity + "\"";
			}

			String hexColor = OutputDisplay.hexColor(col.getRGB());
			strokeColor = " stroke=" + "\"TRANSPARENT\""; // The default value is null for all shapes except Line, Polyline, and Path. The default
															// value is Color.BLACK for those shapes.

			fillColor = " fill=" + '"' + hexColor + '"';
			this.currentColor = col.getRGB();
		}

		if (fillType == GraphicsState.STROKE || fillType == GraphicsState.FILLSTROKE) {
			BasicStroke stroke = (BasicStroke) this.gs.getStroke();

			// allow for any opacity
			float strokeOpacity = this.gs.getAlpha(GraphicsState.STROKE);
			if (strokeOpacity != 1) {
				opacity = " opacity=\"" + strokeOpacity + "\""; // JavaFX fills & strokes cannot have separate opacities.
			}

			if (this.gs.getOutputLineWidth() != 1) { // attribute double lineWidth; (default 1)
				strokeWidth = " strokeWidth=" + '"' + this.gs.getOutputLineWidth() + '"';
			}

			if (stroke.getMiterLimit() != 10) { // attribute double miterLimit; // (default 10)
				strokeMiterLimit = " strokeMiterLimit=" + '"' + this.gs.getOutputLineWidth() + '"';
			}

			strokeLineCap = " strokeLineCap=" + '"' + determineLineCap(stroke) + '"';
			strokeLineJoin = " strokeLineJoin=" + '"' + determineLineJoin(stroke) + '"';

			PdfPaint col = this.gs.getStrokeColor();

			String hexColor = OutputDisplay.hexColor(col.getRGB());
			strokeColor = " stroke=" + '"' + hexColor + '"';
		}

		this.pathCommands.add("\t");
		this.pathCommands.add("<Path id=\"path" + this.shapeCount + '"' + fillRule + strokeColor + fillColor + strokeWidth + strokeLineCap
				+ strokeLineJoin + strokeMiterLimit + opacity + '>');
		this.pathCommands.add("\t<elements>");
	}

	@Override
	protected void finishShape() {
		this.pathCommands.add("\t</elements>");
		this.pathCommands.add("</Path>");
	}

	@Override
	protected void lineTo(double[] coords) {
		// Get coords size as String
		String coordinates = coordsToStringParam(coords, 2);

		// Values separated by the ,
		String delimiter = ",";

		// Temp storage for prefPageSize before converting to float
		String[] temp = coordinates.split(delimiter);

		this.pathCommands.add("\t\t<LineTo x=\"" + temp[0] + "\" y=\"" + temp[1] + "\"/>");
	}

	@Override
	protected void bezierCurveTo(double[] coords) {
		// Get coords size as String
		String coordinates = coordsToStringParam(coords, 6);

		// Values separated by the ,
		String delimiter = ",";

		// Temp storage for prefPageSize before converting to float
		String[] temp = coordinates.split(delimiter);

		this.pathCommands.add("\t\t<CubicCurveTo controlX1=\"" + temp[0] + "\" controlY1=\"" + temp[1] + "\" controlX2=\"" + temp[2]
				+ "\" controlY2=\"" + temp[3] + "\" x=\"" + temp[4] + "\" y=\"" + temp[5] + "\" />");
	}

	@Override
	protected void quadraticCurveTo(double[] coords) {
		// Get coords size as String
		String coordinates = coordsToStringParam(coords, 4);

		// Values separated by the ,
		String delimiter = ",";

		// Temp storage for prefPageSize before converting to float
		String[] temp = coordinates.split(delimiter);

		this.pathCommands.add("\t\t<QuadCurveTo controlX=\"" + temp[0] + "\" controlY=\"" + temp[1] + "\" x=\"" + temp[2] + "\" y=\"" + temp[3]
				+ "\" />");
	}

	@Override
	protected void moveTo(double[] coords) {
		// Get coords size as String
		String coordinates = coordsToStringParam(coords, 2);

		// Values separated by the ,
		String delimiter = ",";

		// Temp storage for prefPageSize before converting to float
		String[] temp = coordinates.split(delimiter);

		this.pathCommands.add("\t\t<MoveTo x=\"" + temp[0] + "\" y=\"" + temp[1] + "\"/>");
	}

	@Override
	protected void closePath() {
		this.pathCommands.add("\t\t<ClosePath />");
	}

	@Override
	protected void drawCropBox() {
		double[] coords = { this.cropBox.x, this.cropBox.y };

		this.pathCommands.add("\t\t<MoveTo x=\"" + coords[0] + "\" y=\"" + coords[1] + "\"/>");
		coords[0] += this.cropBox.width;
		this.pathCommands.add("\t\t<LineTo x=\"" + coords[0] + "\" y=\"" + coords[1] + "\"/>");
		coords[1] += this.cropBox.height;
		this.pathCommands.add("\t\t<LineTo x=\"" + coords[0] + "\" y=\"" + coords[1] + "\"/>");
		coords[0] -= this.cropBox.width;
		this.pathCommands.add("\t\t<LineTo x=\"" + coords[0] + "\" y=\"" + coords[1] + "\"/>");
	}

	@Override
	protected void applyGraphicsStateToPath(GraphicsState gs) {
		// implemented in the beginShape() method
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