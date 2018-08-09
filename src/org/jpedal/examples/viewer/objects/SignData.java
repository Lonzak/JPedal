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
 * SignData.java
 * ---------------
 */

package org.jpedal.examples.viewer.objects;

import java.io.File;

import org.jpedal.examples.viewer.utils.ItextFunctions;

//import com.itextpdf.text.pdf.PdfSignatureAppearance;

/**
 * Models all the data you need in order to sign a Pdf document.
 */
public class SignData {

	private boolean signMode, canEncrypt, flatten, isVisibleSignature;
	private String outputPath, keyFilePath, keyStorePath, alias, reason, location;
	private char[] keyFilePassword, keyStorePassword, aliasPassword, encryptUserPassword, encryptOwnerPassword;
	private int certifyMode, encryptPermissions;
	float x1, y1, x2, y2;
	private File outputFile, keyFile;

	// Fields for use with checking validity of data.
	private boolean valid = false;
	private String invalidMessage;
	private int signaturePage;

	private boolean appendMode;

	/**
	 * @return True if using a keystore file to sign.
	 */
	public boolean isKeystoreSign() {
		return this.signMode;
	}

	/**
	 * @param b
	 *            True if using a keystore file to sign document
	 */
	public void setSignMode(boolean b) {
		this.signMode = b;
	}

	/**
	 * @param path
	 *            Absolute path of the destination of the signed document
	 */
	public void setOutputFilePath(String path) {
		this.outputPath = path;
	}

	public String getOutputFilePath() {
		return this.outputPath;
	}

	public File getOutput() {
		return this.outputFile;
	}

	/**
	 * @param path
	 *            Absolute path of .pfx file.
	 */
	public void setKeyFilePath(String path) {
		this.keyFilePath = path;
	}

	public String getKeyFilePath() {
		return this.keyFilePath;
	}

	public File getKeyFile() {
		return this.keyFile;
	}

	public void setKeyStorePath(String path) {
		this.keyStorePath = path;
	}

	public String getKeyStorePath() {
		return this.keyStorePath;
	}

	public char[] getKeystorePassword() {
		return this.keyStorePassword;
	}

	public void setKeystorePassword(char[] password) {
		this.keyStorePassword = password;
	}

	public String getAlias() {
		return this.alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	public char[] getAliasPassword() {
		return this.aliasPassword;
	}

	public void setAliasPassword(char[] password) {
		this.aliasPassword = password;
	}

	public void setKeyFilePassword(char[] password) {
		this.keyFilePassword = password;
	}

	public char[] getKeyFilePassword() {
		return this.keyFilePassword;
	}

	public boolean canEncrypt() {
		return this.canEncrypt;
	}

	public void setEncrypt(boolean b) {
		this.canEncrypt = b;
	}

	public String getReason() {
		return this.reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	/**
	 * @param certifyMode
	 *            Certify mode in accordance with PdfSignatureAppearance constants.
	 */
	public void setCertifyMode(int certifyMode) {
		this.certifyMode = certifyMode;
	}

	public int getCertifyMode() {
		return this.certifyMode;
	}

	public void setFlatten(boolean selected) {
		this.flatten = selected;
	}

	public boolean canFlatten() {
		return this.flatten;
	}

	public void setEncryptUserPass(char[] password) {
		this.encryptUserPassword = password;
	}

	public char[] getEncryptUserPass() {
		return this.encryptUserPassword;
	}

	public void setEncryptOwnerPass(char[] password) {
		this.encryptOwnerPassword = password;
	}

	public char[] getEncryptOwnerPass() {
		return this.encryptOwnerPassword;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getLocation() {
		return this.location;
	}

	/**
	 * @param permissions
	 *            In accordance with PdfWriter constants
	 */
	public void setEncryptPermissions(int permissions) {
		this.encryptPermissions = permissions;
	}

	public int getEncryptPermissions() {
		return this.encryptPermissions;
	}

	/**
	 * This method is overidden to display messages about its this objects state. Used after calling validate.
	 */
	@Override
	public String toString() {
		String result;

		if (this.valid) {
			result = "Output File: " + this.outputFile.getAbsolutePath() + '\n';
			if (this.signMode) {
				result += "Keystore: " + this.keyStorePath + '\n' + "Alias: " + this.alias + '\n';

			}
			else {
				result += ".pfx File:" + this.keyFilePath + '\n';
			}
		}
		else {
			return this.invalidMessage;
		}

		result += "Reason: \"" + this.reason + "\"\n" + "Location: " + this.location + '\n';

		if (canEncrypt()) {
			result += "Encrypt PDF" + '\n';
		}
		if (canFlatten()) {
			result += "Flatten PDF" + '\n';
		}
		if (this.certifyMode != ItextFunctions.NOT_CERTIFIED) {
			result += "Certify PDF" + '\n';
		}
		return result;
	}

	/**
	 * Initialises and checks validity of files. This objects toString() method changes to reflect failures in validation in order for the user to be
	 * informed.
	 * 
	 * @return True if the files are valid.
	 */
	public boolean validate() { // #TODO Validate whether an author or encryption signature is possible.
		this.outputFile = new File(this.outputPath);

		if (this.outputFile.exists() || this.outputFile.isDirectory()) {
			this.invalidMessage = "Output file already exists."; // TODO Signer: Internalisation of messages
			return this.valid = false;
		}
		if (!this.signMode) {
			this.keyFile = new File(this.keyFilePath);
			if (!this.keyFile.exists() || this.keyFile.isDirectory()) {
				this.invalidMessage = "Key file not found."; // TODO Signer: Internalisation of messages
				return this.valid = false;
			}
		}
		return this.valid = true;
	}

	public boolean isVisibleSignature() {
		return this.isVisibleSignature;
	}

	public void setVisibleSignature(boolean b) {
		this.isVisibleSignature = b;
	}

	public void setRectangle(float x1, float y1, float x2, float y2) {
		if (x1 < x2) {
			this.x1 = x1;
			this.x2 = x2;
		}
		else {
			this.x2 = x1;
			this.x1 = x2;
		}

		if (y1 < y2) {
			this.y1 = y1;
			this.y2 = y2;
		}
		else {
			this.y2 = y1;
			this.y1 = y2;
		}
	}

	/**
	 * @return Four coordinates representing the visible signature area.
	 */
	public float[] getRectangle() {
		float result[] = { this.x1, this.y1, this.x2, this.y2 };
		return result;
	}

	/**
	 * @return The page to sign
	 */
	public int getSignPage() {
		return this.signaturePage;
	}

	public void setSignPage(int page) {
		this.signaturePage = page;
	}

	public void setAppend(boolean b) {
		this.appendMode = b;
	}

	public boolean isAppendMode() {
		return this.appendMode;
	}
}
