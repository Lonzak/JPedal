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
 * PdfStreamDecoderForSampling.java
 * ---------------
 */

package org.jpedal.parser;

import org.jpedal.exception.PdfException;
import org.jpedal.external.ExternalHandlers;
import org.jpedal.io.PdfObjectReader;
import org.jpedal.objects.GraphicsState;
import org.jpedal.objects.raw.PdfDictionary;
import org.jpedal.objects.raw.PdfObject;

public class PdfStreamDecoderForSampling extends PdfStreamDecoder {

	public PdfStreamDecoderForSampling(PdfObjectReader currentPdfFile) {

		super(currentPdfFile);
	}

	/**
	 * 
	 * just scan for DO and CM to get image sizes so we can work out sampling used
	 */
	public final float decodePageContentForImageSampling(PdfObject pdfObject) throws PdfException {/* take out min's%% */

		try {

			this.renderDirectly = true;

			// check switched off
			this.imagesProcessedFully = true;

			// reset count
			this.imageCount = 0;

			this.gs = new GraphicsState(0, 0);/* take out min's%% */

			// get the binary data from the file
			byte[] b_data;

			byte[][] pageContents = null;
			if (pdfObject != null) {
				pageContents = pdfObject.getKeyArray(PdfDictionary.Contents);
				this.isDataValid = pdfObject.streamMayBeCorrupt();
			}

			if (pdfObject != null && pageContents == null) b_data = this.currentPdfFile.readStream(pdfObject, true, true, false, false, false,
					pdfObject.getCacheName(this.currentPdfFile.getObjectReader()));
			else
				if (this.pageStream != null) b_data = this.pageStream;
				else b_data = this.currentPdfFile.getObjectReader().readPageIntoStream(pdfObject);

			// if page data found, turn it into a set of commands
			// and decode the stream of commands
			if (b_data != null && b_data.length > 0) {
				this.getSamplingOnly = true;
				decodeStreamIntoObjects(b_data, false);
			}

			// flush fonts
			this.cache.resetFonts();

			return this.samplingUsed;

		}
		catch (Error err) {
			this.errorTracker.addPageFailureMessage("Problem decoding page " + err);
			if (ExternalHandlers.throwMissingCIDError && err.getMessage().contains("kochi")) throw err;
		}

		return -1;
	}

}
