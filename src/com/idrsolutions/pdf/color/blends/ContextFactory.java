/*
 * ===========================================
 * Java Pdf Extraction Decoding Access Library
 * ===========================================
 *
 * Project Info:  http://www.idrsolutions.com
 *
 * List of all example and a link to zip at http://www.idrsolutions.com/java-code-examples-for-pdf-files/
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
 *
 * ---------------
 * ContextFactory.java
 * ---------------
 */
package com.idrsolutions.pdf.color.blends;

import java.awt.CompositeContext;
import org.jpedal.objects.raw.PdfDictionary;

/**
 *
 */
class ContextFactory {

    static CompositeContext getBlendContext(final int blendMode, final float alpha) {
        
        CompositeContext context;
        
        switch(blendMode){
            
            case PdfDictionary.Multiply:
                
                context=new Multiply(alpha);
                break;
               
            case PdfDictionary.Normal:
                //should never be called but here for completeness 
                
            case PdfDictionary.Overlay:
                
                context=new Overlay(alpha);
                break;
                
            case PdfDictionary.Screen:
                context = new Screen(alpha);
                break;
            
            case PdfDictionary.HardLight:
                context = new HardLight(alpha);
                break;
                
            case PdfDictionary.Darken:
                context = new Darken(alpha);
                break;
            default:
                
                context=new UnimplementedContext(alpha);
                break;
        }
        
        return context;
    }

}


