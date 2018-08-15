/*
 * ===========================================
 * Java Pdf Extraction Decoding Access Library
 * ===========================================
 *
 * Project Info:  http://www.idrsolutions.com
 *
 * List of all example and a link to zip at http://www.idrsolutions.com/java-code-examples-for-pdf-files/
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
 *
 * ---------------
 * Overlay.java
 * ---------------
 */
package com.idrsolutions.pdf.color.blends;

/**
 *
 */
class Overlay extends BMContext {

    Overlay(final float alpha) {
        super(alpha);
    }
    
    @Override
    int[] blend(final int[] src, final int[] dst){
        //int newAlpha = src[3] + dst[3];
        int[] ndst = new int[4];
        if(dst[0]==255 && dst[1]==255 && dst[2]==255 && dst[3]==255){
            ndst = src;
        }else if(src[3] == 255){
            ndst = dst;
        }else{
            for(int i = 0; i < src.length; i++){
                if(dst[i] < 128){ // Mutiply
                    ndst[i]=(src[i])*dst[i] >> 7;
                }else{ // Screen
                    ndst[i] = 255 - ((255-dst[i])*(255-(src[i])) >> 7);
                }
            }
        }
            return ndst;
     }

}


