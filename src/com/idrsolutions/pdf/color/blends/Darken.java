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
 * Darken.java
 * ---------------
 */
package com.idrsolutions.pdf.color.blends;

/**
 *
 */
public class Darken extends BMContext {

    public Darken(final float alpha) {
        super(alpha);
    }
    
    @Override
    int[] blend(final int[] src, final int[] dst){
        
        int[] ndst = new int[4];
        
        if((src[0]==0 && src[1]==0 && src[2]==0 && src[3]==0)){ 
            ndst = dst;
            dst[3]=255;
           
        }else{
            for(int a=0;a<src.length;a++){
                if(dst[a]<src[a]) {
                    ndst[a] = dst[a];
                }else{
                    ndst[a] = src[a];
                }
            }
        }
        ndst[3]=255;
        
        return ndst;
     }

}


