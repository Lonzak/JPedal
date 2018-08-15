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
 * CoonsPatch.java
 * ---------------
 */
package com.idrsolutions.pdf.color.shading;

import org.jpedal.color.GenericColorSpace;

import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;


public class CoonsPatch {

    // Points stored in order of: 1, 1 control towards 2, 2 control towards 1, 2...
    // Each coordinate refers to a proportional position between 0 and 1 on the raster. (Probably.)
    private final double[] points = new double[24];

    protected int pointsAdded;

    protected int colorCompCount;

    protected int colorCompsAdded;

    protected float[][] colors;
    protected ArrayList actualColors;

    protected GeneralPath patchShape;

    protected ArrayList shapes = new ArrayList();

    protected double[][][] calculationPoints;

    protected boolean checkCellBounds;

    protected boolean colorsInitialised;

    private final HashMap bezCacheX;
    private final HashMap bezCacheY;

    protected GenericColorSpace shadingColorSpace;

        public CoonsPatch(final int colorCompCount, final GenericColorSpace shadingColorSpace) {
        this.colorCompCount = colorCompCount;
        colors = new float[4][colorCompCount];
        this.shadingColorSpace = shadingColorSpace;
        bezCacheX = new HashMap();
        bezCacheY = new HashMap();
    }

    /**
     * Stores a points x and y coordinates. Points must be added in order, as defined above.
     * @param x x coordinate
     * @param y y coordinate
     */
    public void addPoint(final double x, final double y) {
        if (pointsAdded*2 < points.length) {
            points[pointsAdded*2] = x;
            points[(pointsAdded*2)+1] = y;
        }
        pointsAdded++;
    }


    /**
     * Stores a color value. Must be added in order.
     * @param c single color value
     */
    public void addColorValue(final float c) {
        colors[colorCompsAdded/colorCompCount][colorCompsAdded%colorCompCount] = c;
        colorCompsAdded++;
    }

    /**
     * Sets up the colors.
     */
    private void initialiseColors() {
        actualColors = new ArrayList();
        for(int i=0; i<4; i++) {
            //Retrieve values from array
            final float[] values = new float[colors[i].length];
            System.arraycopy(colors[i], 0, values, 0, colors[i].length);

            //Convert to a color
            shadingColorSpace.setColor(values,values.length);
            final Color c = (Color) shadingColorSpace.getColor();

            actualColors.add(c);
        }
    }


    /**
     * returns the coordinates of a specified point on the curve starting at point fp (0-3 clockwise from start).
     * @param t Point on line (0-1)
     * @param fp Starting point
     * @return Coordinates of point
     */
    private double[] bezier(double t, final int fp) {
        //check if cached
        final Double key = (2 * fp) + t;
        if (bezCacheX.containsKey(key)) {
            return new double[]{(Double) bezCacheX.get(key),
                    (Double) bezCacheY.get(key)};
        }

        //Coords go other way for C1 and D2 (pdf spec fails to mention this for some reason...)
        if (fp == C1 || fp == D2) {
            t = 1 - t;
        }
        
        final int b = 6*fp;
        final double[] result = new double[2];
        final double pow1t3 = Math.pow((1-t),3);
        final double pow1t2 = 3*t*Math.pow((1-t),2);
        final double powt2 = 3*Math.pow(t,2)*(1-t);
        final double powt3 = Math.pow(t,3);
        result[0] = (pow1t3*points[b]) + (pow1t2*points[b+2]) +
                        (powt2*points[b+4]) + (powt3*points[(b+6)%24]);
        result[1] = (pow1t3*points[b+1]) + (pow1t2*points[b+3]) +
                        (powt2*points[b+5]) + (powt3*points[(b+7)%24]);

        //cache
        bezCacheX.put(key, result[0]);
        bezCacheY.put(key, result[1]);

        return result;
    }

    public void printOntoG2(final Graphics2D g2, final Rectangle2D bounds, final boolean printBack) {
        if (shapes.isEmpty()) {
            calculate();
        }

        //Fill the patch shape
        if (printBack) {
            g2.setPaint((Color)actualColors.get(0));
            g2.fill(patchShape);
        }

        //loop through cells, either checking if within raster or not
        if (checkCellBounds) {
            for (final Object shape1 : shapes) {
                final ColorShape shape = (ColorShape) shape1;
                if (shape.getShape().getBounds2D().intersects(bounds)) {
                    g2.setPaint(shape.getColor());
                    g2.fill(shape.getShape());
                }
            }
        } else {
            for (final Object shape1 : shapes) {
                final ColorShape shape = (ColorShape) shape1;
                g2.setPaint(shape.getColor());
                g2.fill(shape.getShape());
            }
        }
    }

    private static final int C1=3;
    private static final int C2=1;
    private static final int D1=0;
    private static final int D2=2;

    /**
     * This method should match the description of the coordinate mapping given on p322 of the PDF spec.
     * @param u The x axis
     * @param v The y axis
     * @return The resultant point
     */
    private double[] mapping(final double u, final double v) {
        
        final double[] result = new double[2];

        //Sc(u,v)
        final double[] bezC1u=bezier(u,C1);
        final double[] bezC2u=bezier(u,C2);
        result[0] = ((1- v)* bezC1u[0])+(v *bezC2u[0]);
        result[1] = ((1- v)* bezC1u[1])+(v *bezC2u[1]);

        //+Sd(u,v)
        final double[] bezD1v=bezier(v,D1);
        final double[] bezD2v=bezier(v,D2);
        result[0] += ((1- u)*bezD1v[0]) + (u * bezD2v[0]);
        result[1] += ((1- u)*bezD1v[1]) + (u * bezD2v[1]);

        //-Sb(u,v)
        final double[] bezC10=bezier(0,C1);
        final double[] bezC11=bezier(1,C1);
        final double[] bezC20=bezier(0,C2);
        final double[] bezC21=bezier(1,C2);
        result[0] -= (((1- v)* (((1- u)*bezC10[0])+(u *bezC11[0])))
                + (v * (((1- u)*bezC20[0])+(u *bezC21[0]))));
        result[1] -= (((1- v)* (((1- u)*bezC10[1])+(u *bezC11[1])))
                + (v * (((1- u)*bezC20[1])+(u *bezC21[1]))));

        return result;
    }                     

    /**
     * Calculates the color at a specified point.
     * @param x X coord in patch space
     * @param y Y coord in patch space
     * @return Color at specified point
     */
    protected Color calculateColor(final double x, final double y) {
        /**
		   A  B
           C  D
        */

        if(!colorsInitialised) {
            initialiseColors();
        }

        //Calculate proportions of each color
        final double a = (1-x)*y;
        final double b = x*y;
        final double c = (1-x)*(1-y);
        final double d = x*(1-y);

        final Color c1 = (Color) actualColors.get(1);
        final Color c2 = (Color) actualColors.get(2);
        final Color c3 = (Color) actualColors.get(0);
        final Color c4 = (Color) actualColors.get(3);

        final double red = ((c1.getRed()*a)+(c2.getRed()*b)+(c3.getRed()*c)+(c4.getRed()*d));
        final double green = ((c1.getGreen()*a)+(c2.getGreen()*b)+(c3.getGreen()*c)+(c4.getGreen()*d));
        final double blue = ((c1.getBlue()*a)+(c2.getBlue()*b)+(c3.getBlue()*c)+(c4.getBlue()*d));

        return new Color((int)red, (int)green, (int)blue);
    }


    public void calculate() {
        //Calculate edge shape
        if (patchShape==null) {
            initialiseShape();
        }

        //create sample points for shapes
        Rectangle2D patchRect = patchShape.getBounds2D();
        int res = (int)(30*(patchRect.getWidth()+patchRect.getHeight()));
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
                final ColorShape shape = new ColorShape(i,j, calculateColor(div*i, div*j));
                shapes.add(shape);
            }
        }
    }

    private void initialiseShape() {
        patchShape = new GeneralPath();
        patchShape.moveTo((float)points[0],(float) points[1]);
        patchShape.curveTo((float)points[2],(float)points[3],(float)points[4],(float)points[5],(float)points[6],(float)points[7]);
        patchShape.curveTo((float)points[8],(float)points[9],(float)points[10],(float)points[11],(float)points[12],(float)points[13]);
        patchShape.curveTo((float)points[14],(float)points[15],(float)points[16],(float)points[17],(float)points[18],(float)points[19]);
        patchShape.curveTo((float)points[20],(float)points[21],(float)points[22],(float)points[23],(float)points[0],(float)points[1]);
    }

    public Rectangle2D getBounds2D() {
        //Calculate edge shape
        if (patchShape==null) {
            initialiseShape();
        }
        return patchShape.getBounds2D();
    }

    protected class ColorShape {
        private final GeneralPath p = new GeneralPath();
        private final Color c;

        ColorShape(final int x, final int y, final Color c) {
            p.moveTo((float)calculationPoints[x]  [y]  [0], (float)calculationPoints[x]  [y]  [1]);
            p.lineTo((float)calculationPoints[x+1][y]  [0], (float)calculationPoints[x+1][y]  [1]);
            p.lineTo((float)calculationPoints[x+1][y+1][0], (float)calculationPoints[x+1][y+1][1]);
            p.lineTo((float)calculationPoints[x]  [y+1][0], (float)calculationPoints[x]  [y+1][1]);
            p.lineTo((float)calculationPoints[x]  [y]  [0], (float)calculationPoints[x]  [y]  [1]);
            this.c = c;
        }

        public Shape getShape() {
            return p;
        }

        public Color getColor() {
            return c;
        }
    }
    
    public double[] getPoints(){
        return points;
    }
    
    public float[][] getColors(){
        return colors;
    }
}
