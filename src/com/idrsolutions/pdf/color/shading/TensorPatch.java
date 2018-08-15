/*
 * ===========================================
 * Java Pdf Extraction Decoding Access Library
 * ===========================================
 *
 * Project Info:  http://www.idrsolutions.com
 * Help section for developers at http://www.idrsolutions.com/java-pdf-library-support/
 *
 * (C) Copyright 1997-2014, IDRsolutions and Contributors.
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
 * TensorPatch.java
 * ---------------
 */
package com.idrsolutions.pdf.color.shading;

import org.jpedal.color.GenericColorSpace;

import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;


public class TensorPatch extends CoonsPatch {

    // Points stored in order of: 1, 1 control towards 2, 2 control towards 1, 2...
    // Each coordinate refers to a proportional position between 0 and 1 on the raster. (Probably.)
    private final double[][] px = new double[4][4];
    private final double[][] py = new double[4][4];


    @SuppressWarnings("UnusedDeclaration")
    public TensorPatch(final int colorCompCount, final GenericColorSpace shadingColorSpace) {
        super(colorCompCount, shadingColorSpace);
    }

    /**
     * Stores a points x and y coordinates. Points must be added in order, as defined above.
     * @param x x coordinate
     * @param y y coordinate
     */
    @Override
    public void addPoint(final double x, final double y) {
        switch(pointsAdded) {
            case 0:
                px[0][3] = x;
                py[0][3] = y;
                break;
            case 1:
                px[0][2] = x;
                py[0][2] = y;
                break;
            case 2:
                px[0][1] = x;
                py[0][1] = y;
                break;
            case 3:
                px[0][0] = x;
                py[0][0] = y;
                break;
            case 4:
                px[1][0] = x;
                py[1][0] = y;
                break;
            case 5:
                px[2][0] = x;
                py[2][0] = y;
                break;
            case 6:
                px[3][0] = x;
                py[3][0] = y;
                break;
            case 7:
                px[3][1] = x;
                py[3][1] = y;
                break;
            case 8:
                px[3][2] = x;
                py[3][2] = y;
                break;
            case 9:
                px[3][3] = x;
                py[3][3] = y;
                break;
            case 10:
                px[2][3] = x;
                py[2][3] = y;
                break;
            case 11:
                px[1][3] = x;
                py[1][3] = y;
                break;
            case 12:
                px[1][2] = x;
                py[1][2] = y;
                break;
            case 13:
                px[1][1] = x;
                py[1][1] = y;
                break;
            case 14:
                px[2][1] = x;
                py[2][1] = y;
                break;
            case 15:
                px[2][2] = x;
                py[2][2] = y;
                break;
        }
        pointsAdded++;
    }


	public static double BernsteinPolynomials(final int axisPt, final double t){
		switch(axisPt){
            case 0 : return Math.pow((1-t),3);
            case 1 : return (3*t)*Math.pow((1-t),2);
            case 2 : return Math.pow((3*t),2)*(1-t);
            case 3 : return Math.pow(t,3);
            default : return t;
        }
    }


    private double[] mapping(final double u, final double v) {
        double x = 0, y = 0;

        for (int i=0; i<4; i++) {
            for (int j=0; j<4; j++) {
                x += px[i][j] * BernsteinPolynomials(i, u) * BernsteinPolynomials((3-j), v);
                y += py[i][j] * BernsteinPolynomials(i, u) * BernsteinPolynomials((3-j), v);
            }
        }

        return new double[]{x,y};
    }

    private void initialiseShape() {
        patchShape = new GeneralPath();
        patchShape.moveTo( (float) px[0][0], (float) py[0][0]);
        patchShape.curveTo((float) px[1][0], (float) py[1][0], (float) px[2][0], (float) py[2][0], (float) px[3][0], (float) py[3][0]);
        patchShape.curveTo((float) px[3][1], (float) py[3][1], (float) px[3][2], (float) py[3][2], (float) px[3][3], (float) py[3][3]);
        patchShape.curveTo((float) px[2][3], (float) py[2][3], (float) px[1][3], (float) py[1][3], (float) px[0][3], (float) py[0][3]);
        patchShape.curveTo((float) px[0][2], (float) py[0][2], (float) px[0][1], (float) py[0][1], (float) px[0][0], (float) py[0][0]);
    }


    @Override
    public void calculate() {
        //Calculate edge shape
        if (patchShape==null) {
            initialiseShape();
        }

        //create sample points for shapes
        int res = (int)(30*(patchShape.getBounds2D().getWidth()+patchShape.getBounds2D().getHeight()));
        if (res < 4) {
            res = 4;
        }
        if (res > 40) {
            res = 40;
        }
        final double div = (double)1/res;
        checkCellBounds = res > 7;
        calculationPoints = new double[res+1][res+1][2];
        for (int x=0; x<res+1; x++) {
            for (int y=0; y<res+1; y++) {
                final double[] m = mapping(x*div, y*div);
                calculationPoints[x][y]=m;
            }
        }

        //create shapes
        for (int i=0; i<res; i++) {
            for (int j=0; j<res; j++) {
                final ColorShape shape = new ColorShape(i,j, calculateColor(i*div,j*div));
                shapes.add(shape);
            }
        }
    }

    @Override
    public Rectangle2D getBounds2D() {
        //Calculate edge shape
        if (patchShape==null) {
            initialiseShape();
        }
        return patchShape.getBounds2D();
    }
}
