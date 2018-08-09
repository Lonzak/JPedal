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
 * CertificateReader.java
 * ---------------
 */

package org.jpedal.io;

import java.security.Key;
import java.security.PrivateKey;
import java.security.cert.Certificate;

import org.bouncycastle.cms.CMSEnvelopedData;
import org.bouncycastle.cms.RecipientInformation;
import org.bouncycastle.cms.jcajce.JceKeyTransEnvelopedRecipient;
import org.jpedal.utils.LogWriter;

public class CertificateReader {

	// <start-adobe><start-wrap>
	public static byte[] readCertificate(byte[][] recipients, Certificate certificate, Key key) {

		byte[] envelopedData = null;

		/**
		 * values for BC
		 */
		String provider = "BC";

		/**
		 * loop through all and get data if match found
		 */
		for (byte[] recipient : recipients) {

			try {
				CMSEnvelopedData recipientEnvelope = new CMSEnvelopedData(recipient);

				Object[] recipientList = recipientEnvelope.getRecipientInfos().getRecipients().toArray();
				int listCount = recipientList.length;

				for (int ii = 0; ii < listCount; ii++) {
					RecipientInformation recipientInfo = (RecipientInformation) recipientList[ii];

					if (recipientInfo.getRID().match(certificate)) {
						envelopedData = recipientInfo.getContent(new JceKeyTransEnvelopedRecipient((PrivateKey)key).setProvider(provider));
						ii = listCount;
					}
				}
			}
			catch (Exception e) {
				// tell user and log
				if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
			}
		}

		return envelopedData;
	}

	// <end-wrap><end-adobe>

}
