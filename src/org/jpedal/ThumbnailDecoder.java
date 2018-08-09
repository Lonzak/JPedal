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
 * ThumbnailDecoder.java
 * ---------------
 */
package org.jpedal;

import java.awt.Graphics;
import java.awt.image.BufferedImage;

import org.jpedal.exception.PdfException;
import org.jpedal.utils.LogWriter;

/**
 * generates thumbnails of pages for display
 */
public class ThumbnailDecoder {

	private PdfDecoder decode_pdf;

	public ThumbnailDecoder(PdfDecoder decode_pdf) {

		this.decode_pdf = decode_pdf;
	}

	/**
	 * get pdf as Image of any page scaling is size (100 = full size)
	 */
	final synchronized public BufferedImage getPageAsThumbnail(int pageNumber, int height) {

		BufferedImage newImg = null;
		// stopDecoding=false;
		try {
			BufferedImage pageImage = this.decode_pdf.getPageAsImage(pageNumber);

			int imgHeight = pageImage.getHeight();
			double scale = height / (double) imgHeight;
			int width = (int) (pageImage.getWidth() * scale);

			newImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
			Graphics g = newImg.getGraphics();
			g.drawImage(pageImage, 0, 0, width, height, null);
			g.dispose();

		}
		catch (PdfException e) {
			// tell user and log
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
		}

		return newImg;

		/**
		 * //###### OLD code no longer needed BufferedImage image = null; int mediaX, mediaY, mediaW, mediaH;
		 * 
		 * //the actual display object DynamicVectorRenderer imageDisplay=null;// ObjectStore.getCachedPage(new Integer(pageNumber));
		 * 
		 * if(imageDisplay!=null){ imageDisplay.setObjectStoreRef(decode_pdf.objectStoreRef); }else{ imageDisplay = new
		 * DynamicVectorRenderer(pageNumber,true, 1000, decode_pdf.objectStoreRef); //
		 * imageDisplay.setHiResImageForDisplayMode(decode_pdf.useHiResImageForDisplay);
		 * 
		 * 
		 * try {
		 * 
		 * // check in range if (pageNumber > decode_pdf.getPageCount()) {
		 * 
		 * LogWriter.writeLog("Page " + pageNumber + " out of bounds");
		 * 
		 * } else {
		 * 
		 * // resolve page size mediaX = decode_pdf.pageData.getMediaBoxX(pageNumber); mediaY = decode_pdf.pageData.getMediaBoxY(pageNumber); mediaW =
		 * decode_pdf.pageData.getMediaBoxWidth(pageNumber); mediaH = decode_pdf.pageData.getMediaBoxHeight(pageNumber);
		 * 
		 * // get pdf object id for page to decode String currentPageOffset = (String) decode_pdf.pagesReferences.get(new Integer(pageNumber));
		 * 
		 * // decode the file if not already decoded, there is a valid object id and it is unencrypted if ((currentPageOffset != null)) {
		 * 
		 * // read page or next pages PdfObject pdfObject=new PageObject(currentPageOffset); decode_pdf.currentPdfFile.readObject(pdfObject);
		 * PdfObject Resources=pdfObject.getDictionary(PdfDictionary.Resources);
		 * 
		 * if (pdfObject != null) {
		 * 
		 * //layers.setScaling(scaling); imageDecoder = new PdfStreamDecoder(decode_pdf.useHiResImageForDisplay, null, decode_pdf.globalResources);
		 * imageDecoder.setExternalImageRender(decode_pdf.customImageHandler);
		 * 
		 * 
		 * if(stopDecoding){ imageDecoder=null; return null; }
		 * 
		 * imageDecoder.setName(decode_pdf.filename); imageDecoder.setStore(decode_pdf.objectStoreRef);
		 * 
		 * if(stopDecoding){ imageDecoder=null; return null; }
		 * 
		 * imageDecoder.init(true, true, decode_pdf.renderMode, 0, decode_pdf.pageData, pageNumber, imageDisplay, decode_pdf.currentPdfFile);
		 * 
		 * if (decode_pdf.globalResources != null){ decode_pdf.currentPdfFile.checkResolved(decode_pdf.globalResources);
		 * imageDecoder.readResources(decode_pdf.globalResources,true);
		 * 
		 * PdfObject propObj=decode_pdf.globalResources.getDictionary(PdfDictionary.Properties); if(propObj!=null) decode_pdf.PropertiesObj=propObj; }
		 * 
		 * //read the resources for the page if (Resources != null){ decode_pdf.currentPdfFile.checkResolved(Resources);
		 * imageDecoder.readResources(Resources,true);
		 * 
		 * PdfObject propObj=Resources.getDictionary(PdfDictionary.Properties); if(propObj!=null) decode_pdf.PropertiesObj=propObj; }
		 * 
		 * decode_pdf.setupResources(imageDecoder, true);
		 * 
		 * if(stopDecoding){ imageDecoder=null; return null; }
		 * 
		 * 
		 * imageDisplay.init(mediaW, mediaH, decode_pdf.pageData.getRotation(pageNumber));
		 * 
		 * imageDecoder.decodePageContent(pdfObject, mediaX,mediaY, null, null);
		 * 
		 * } } } }
		 * 
		 * imageDecoder=null;
		 * 
		 * //removed as unreliable //ObjectStore.cachePage(new Integer(pageNumber),imageDisplay); }
		 * 
		 * // workout scaling and get image
		 * 
		 * if(!stopDecoding ) image = decode_pdf.getImageFromRenderer(height, imageDisplay, pageNumber);
		 * 
		 * 
		 * return image; //### END - OLD code no longer needed /
		 **/
	}
}
