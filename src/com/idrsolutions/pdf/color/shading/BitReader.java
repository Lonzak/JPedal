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
 * BitReader.java
 * ---------------
 */
package com.idrsolutions.pdf.color.shading;

import java.util.BitSet;

/**
 * a java class to handle bitwise reading in shading object
 */
public class BitReader {

    private int p; //pointer
    private final int totalBitLen;
    private BitSet bitset;
    private byte[] data;
    private final boolean hasSmallBits;

    public BitReader(final byte[] data, final boolean hasSmallBits) {
        this.hasSmallBits = hasSmallBits;
        this.totalBitLen = data.length * 8;
        if (this.hasSmallBits) {
            this.bitset = new BitSet(totalBitLen);
            int c = 0;
            for (int i = 0; i < data.length; i++) {
                byte b = data[i];
                for (int j = 7; j >= 0; j--) {
                    boolean isOn = ((b >> j) & 1) == 1;
                    bitset.set(c, isOn);
                    c++;
                }
            }
        } else {
            this.data = data;
        }
    }

    /**
     *
     * @param lenToRead
     * @return this return value is not actual int and it is a data
     * representation in 32 bits
     */
    private int readBits(int lenToRead) {
        int retVal = 0;
        if (hasSmallBits) {
            BitSet smallSet = bitset.get(p, p + lenToRead);
            for (int i = 0; i < lenToRead; i++) {
                if (smallSet.get(i)) {
                    retVal = (retVal << 1) | 1;
                } else {
                    retVal = (retVal << 1);
                }
            }
            p += lenToRead;
        } else {
            int len = lenToRead / 8;
            for (int i = 0; i < len; i++) {
                retVal = (retVal << 8);
                retVal |= ((data[p / 8] & 0xff));
                p += 8;
            }
        }
        return retVal;
    }

    /**
     * return positive integer only
     *
     * @param bitLen
     * @return this return value is not actual int and it is a data
     */
    public int getPositive(int bitLen) {
        return readBits(bitLen);
    }

    /**
     *
     * @param bitLen
     * @return floating point
     */
    public float getFloat(int bitLen) {
        int value = readBits(bitLen);
        byte[] temp = new byte[]{(byte) (value >>> 24), (byte) (value >>> 16), (byte) (value >>> 8), (byte) value};
        float number = 0.0f;
        switch (bitLen) {
            case 1:
            case 2:
            case 4:
            case 8:
                number = (temp[3] & 255) / 256f;
                break;
            case 16:
                number = (temp[2] & 255) / 256f;
                number += (temp[3] & 255) / 65536f;
                break;
            case 24:
                number = (temp[1] & 255) / 256f;
                number += (temp[2] & 255) / 65536f;
                number += (temp[3] & 255) / 16777216f;
                break;
            case 32:
                number = (temp[0] & 255) / 256f;
                number += (temp[1] & 255) / 65536f;
                number += (temp[2] & 255) / 16777216f;
                number += (temp[3] & 255) / 4294967296f;
                break;
        }
        return number;
    }

    public int getPointer() {
        return p;
    }

    public int getTotalBitLen() {
        return totalBitLen;
    }

//    public static void main(String[] args) {
        //        byte[] data = new byte[]{-128, 72, 0};
        //        BitSet bitset = new BitSet(data.length * 8);
        //        int c = 0;
        //        for (int i = 0; i < data.length; i++) {
        //            byte b = data[i];
        //            System.out.println(" "+Integer.toBinaryString(b));
        //            for (int j = 7; j >= 0; j--) {
        //                boolean isOn = ((b >> j) & 1) == 1;
        //                bitset.set(c, isOn);
        //                c++;//                
        //            }
        //        }//        
        //        BitReader bit = new BitReader(data);
        //       
        //        for (int i = 0; i < 24; i++) {
        //            System.out.println(" -- "+bit.readBits(3));
        //        }
//    }
    }
