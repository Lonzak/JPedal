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
 * FileAccess.java
 * ---------------
 */
package org.jpedal;

import java.security.PrivateKey;
import java.security.cert.Certificate;

import org.jpedal.constants.PDFflags;
import org.jpedal.io.DecryptionFactory;
import org.jpedal.io.PdfFileReader;
import org.jpedal.io.PdfObjectReader;
import org.jpedal.io.PdfReader;

public class FileAccess {

	/** user can open encrypted file with certificate */
	Certificate certificate;

	/** used for opening encrypted file */
	PrivateKey key;

	public boolean isFileViewable(PdfObjectReader currentPdfFile) {
		if (currentPdfFile != null) {
			PdfFileReader objectReader = currentPdfFile.getObjectReader();

			DecryptionFactory decryption = objectReader.getDecryptionObject();
			return decryption == null || decryption.getBooleanValue(PDFflags.IS_FILE_VIEWABLE) || this.certificate != null;
		}
		else return false;
	}

	public boolean isPasswordSupplied(PdfObjectReader currentPdfFile) {
		// allow through if user has verified password or set certificate
		if (currentPdfFile != null) {
			PdfFileReader objectReader = currentPdfFile.getObjectReader();

			DecryptionFactory decryption = objectReader.getDecryptionObject();
			return decryption != null && (decryption.getBooleanValue(PDFflags.IS_PASSWORD_SUPPLIED) || this.certificate != null);
		}
		else return false;
	}

	public void setUserEncryption(Certificate certificate, PrivateKey key) {

		this.certificate = certificate;
		this.key = key;
	}

	public PdfObjectReader getNewReader() {

		PdfObjectReader currentPdfFile;

		if (this.certificate != null) {
			currentPdfFile = new PdfReader(this.certificate, this.key);
		}
		else currentPdfFile = new PdfReader();

		return currentPdfFile;
	}
}
