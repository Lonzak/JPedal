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
 * ShadingDatastreamReader.java
 * ---------------
 */
package com.idrsolutions.pdf.color.shading;

import java.awt.geom.Point2D;
import org.jpedal.color.GenericColorSpace;

import java.util.ArrayList;

public class ShadingDatastreamReader {

    private final BitReader reader;

    private final int bitsPerCoord, bitsPerComp, compCount, bitsPerFlag, type;

    private boolean processed;

    private final GenericColorSpace shadingColorSpace;

    private final ArrayList patches;

    public ShadingDatastreamReader(final byte[] data, final int bitsPerCoord, final int bitsPerComp, final int compCount, final int bitsPerFlag, final GenericColorSpace shadingColorSpace, final int type) {
        patches = new ArrayList();
        this.bitsPerCoord = bitsPerCoord;
        this.bitsPerComp = bitsPerComp;
        this.compCount = compCount;
        this.bitsPerFlag = bitsPerFlag;
        this.shadingColorSpace = shadingColorSpace;
        this.type = type;
        boolean hasSmallBits = bitsPerFlag < 8 || bitsPerComp < 8 || bitsPerCoord < 8;
        this.reader = new BitReader(data, hasSmallBits);
    }

    /**
     * Returns the patches.
     *
     * @return A list of patches
     */
    public ArrayList getPatches() {
        if (!processed) {
            process();
            processed = true;
        }
        return patches;
    }

    private void addShapePoints8(CoonsPatch patch) {
        for (int i = 0; i < 8; i++) {
            patch.addPoint(reader.getFloat(bitsPerCoord), reader.getFloat(bitsPerCoord));
        }
    }

    private void addShapePoints12(CoonsPatch patch) {
        for (int i = 0; i < 12; i++) {
            patch.addPoint(reader.getFloat(bitsPerCoord), reader.getFloat(bitsPerCoord));
        }
    }

    private void addShapePoints16(CoonsPatch patch) {
        for (int i = 0; i < 12; i++) {
            patch.addPoint(reader.getFloat(bitsPerCoord), reader.getFloat(bitsPerCoord));
        }
        for (int i = 0; i < 4; i++) {
            reader.getFloat(bitsPerCoord);
            reader.getFloat(bitsPerCoord);
        }
    }

    /**
     * Processes the patch data and creates the patch objects.
     */
    private void process() {
        while (reader.getPointer() < reader.getTotalBitLen()) {
            int flag = reader.getPositive(bitsPerFlag);
            Point2D a4[] = new Point2D[8]; // array for points with flag values;
            switch (flag) {
                case 0:
                    final CoonsPatch patch;
                    if (type == ShadedPaint.COONS) {
                        patch = new CoonsPatch(compCount, shadingColorSpace);
                        addShapePoints12(patch);//Retrieve 12 pairs of edge coordinates
                    } else if (type == ShadedPaint.TENSOR) { //needs to use TensorPatch eventually!
                        patch = new CoonsPatch(compCount, shadingColorSpace);
                        addShapePoints16(patch);
                    } else {
                        return;
                    }
                    for (int i = 0; i < 4; i++) {
                        for (int c = 0; c < compCount; c++) {
                            patch.addColorValue(reader.getFloat(bitsPerComp));
                        }
                    }
                    patches.add(patch);

                    break;
                case 1:
                    if (type == ShadedPaint.TENSOR) {
                        return;
                    }
                    final CoonsPatch patch1 = new CoonsPatch(compCount, shadingColorSpace);
                    CoonsPatch prev1 = (CoonsPatch) patches.get(patches.size() - 1);
                    double[] prev1Arr = prev1.getPoints();
                    float[][] prev1Color = prev1.getColors();

                    a4[0] = new Point2D.Double(prev1Arr[6], prev1Arr[7]);
                    a4[1] = new Point2D.Double(prev1Arr[8], prev1Arr[9]);
                    a4[2] = new Point2D.Double(prev1Arr[10], prev1Arr[11]);
                    a4[3] = new Point2D.Double(prev1Arr[12], prev1Arr[13]);

                    for (int i = 0; i < 4; i++) {
                        patch1.addPoint(a4[i].getX(), a4[i].getY());
                    }

                    addShapePoints8(patch1);

                    float[][] temp1 = new float[2][compCount];
                    temp1[0] = prev1Color[1];
                    temp1[1] = prev1Color[2];

                    for (int i = 0; i < 2; i++) {
                        for (int c = 0; c < compCount; c++) {
                            patch1.addColorValue(temp1[i][c]);
                        }
                    }

                    for (int i = 0; i < 2; i++) {
                        for (int c = 0; c < compCount; c++) {
                            patch1.addColorValue(reader.getFloat(bitsPerComp));
                        }
                    }
                    patches.add(patch1);

                    break;
                case 2:
                    if (type == ShadedPaint.TENSOR) {
                        return;
                    }
                    final CoonsPatch patch2 = new CoonsPatch(compCount, shadingColorSpace);
                    CoonsPatch prev2 = (CoonsPatch) patches.get(patches.size() - 1);
                    double[] prev2Arr = prev2.getPoints();
                    float[][] prev2Color = prev2.getColors();

                    a4[0] = new Point2D.Double(prev2Arr[12], prev2Arr[13]);
                    a4[1] = new Point2D.Double(prev2Arr[14], prev2Arr[15]);
                    a4[2] = new Point2D.Double(prev2Arr[16], prev2Arr[17]);
                    a4[3] = new Point2D.Double(prev2Arr[18], prev2Arr[19]);

                    for (int i = 0; i < 4; i++) {
                        patch2.addPoint(a4[i].getX(), a4[i].getY());
                    }

                    addShapePoints8(patch2);

                    for (int i = 0; i < 2; i++) {
                        for (int c = 0; c < compCount; c++) {
                            patch2.addColorValue(prev2Color[2 + i][c]);
                        }
                    }

                    for (int i = 0; i < 2; i++) {
                        for (int c = 0; c < compCount; c++) {
                            patch2.addColorValue(reader.getFloat(bitsPerComp));
                        }
                    }
                    patches.add(patch2);

                    break;
                case 3:
                    if (type == ShadedPaint.TENSOR) {
                        return;
                    }
                    final CoonsPatch patch3 = new CoonsPatch(compCount, shadingColorSpace);
                    CoonsPatch prev3 = (CoonsPatch) patches.get(patches.size() - 1);
                    double[] prev3Arr = prev3.getPoints();
                    float[][] prev3Color = prev3.getColors();

                    a4[0] = new Point2D.Double(prev3Arr[18], prev3Arr[19]);
                    a4[1] = new Point2D.Double(prev3Arr[20], prev3Arr[21]);
                    a4[2] = new Point2D.Double(prev3Arr[22], prev3Arr[23]);
                    a4[3] = new Point2D.Double(prev3Arr[0], prev3Arr[1]);

                    for (int i = 0; i < 4; i++) {
                        patch3.addPoint(a4[i].getX(), a4[i].getY());
                    }

                    addShapePoints8(patch3);

                    float[][] temp = new float[2][compCount];
                    temp[0] = prev3Color[3];
                    temp[1] = prev3Color[0];

                    for (int i = 0; i < 2; i++) {
                        for (int c = 0; c < compCount; c++) {
                            patch3.addColorValue(temp[i][c]);
                        }
                    }

                    for (int i = 0; i < 2; i++) {
                        for (int c = 0; c < compCount; c++) {
                            patch3.addColorValue(reader.getFloat(bitsPerComp));
                        }
                    }
                    patches.add(patch3);
                    break;
            }
        }
    }

    /**
     * Returns a number between 0 and 1 represented by an arbitrary number of
     * bits.
     *
     * @param bits Bits to convert
     * @return Decimal
     */
//    private float getDecimal(final int bits) {
//        if (bits == 32) {
//            float number = (data[pointer] & 255) / 256f;
//            pointer++;
//
//            number += (data[pointer] & 255) / 65536f;
//            pointer++;
//
//            number += (data[pointer] & 255) / 16777216f;
//            pointer++;
//
//            number += (data[pointer] & 255) / 4294967296f;
//            pointer++;
//
//            return number;
//        } else if(bits == 24){
//            float number = (data[pointer] & 255) / 256f;
//            pointer++;
//
//            number += (data[pointer] & 255) / 65536f;
//            pointer++;
//
//            number += (data[pointer] & 255) / 16777216f;
//            pointer++;
//            return number;
//            
//        } else if (bits == 16) {
//            final float number;
//            final float dec;
//
//            number = (data[pointer] & 255) / 256f;
//
//            pointer++;
//
//            dec = (data[pointer] & 255) / 65536f;
//
//            pointer++;
//
//            return (number + dec);
//        } else if (bits == 8) {
//            final float number;
//            number = (data[pointer] & 255) / 256f;
//            pointer++;
//            return number;
//        } else {
//            return 0;
//        }
//    }
}
