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
 * RecentDocuments.java
 * ---------------
 */
package org.jpedal.examples.viewer;

import java.util.Stack;
import java.util.StringTokenizer;

import org.jpedal.examples.viewer.utils.PropertiesFile;

public class RecentDocuments {

	int noOfRecentDocs;
	PropertiesFile properties;

	private Stack previousFiles = new Stack();
	private Stack nextFiles = new Stack();

	public RecentDocuments(int noOfRecentDocs, PropertiesFile properties) {

		this.noOfRecentDocs = noOfRecentDocs;
		this.properties = properties;
	}

	static String getShortenedFileName(String fileNameToAdd) {
		final int maxChars = 30;

		if (fileNameToAdd.length() <= maxChars) return fileNameToAdd;

		StringTokenizer st = new StringTokenizer(fileNameToAdd, "\\/");

		int noOfTokens = st.countTokens();

		// allow for /filename.pdf
		if (noOfTokens == 1) return fileNameToAdd.substring(0, maxChars);

		String[] arrayedFile = new String[noOfTokens];
		for (int i = 0; i < noOfTokens; i++)
			arrayedFile[i] = st.nextToken();

		String filePathBody = fileNameToAdd.substring(arrayedFile[0].length(), fileNameToAdd.length() - arrayedFile[noOfTokens - 1].length());

		StringBuilder sb = new StringBuilder(filePathBody);

		int start, end;
		for (int i = noOfTokens - 2; i > 0; i--) {

			start = sb.lastIndexOf(arrayedFile[i]);
			end = start + arrayedFile[i].length();
			sb.replace(start, end, "...");

			if (sb.toString().length() <= maxChars) break;
		}

		return arrayedFile[0] + sb + arrayedFile[noOfTokens - 1];
	}

	public String getPreviousDocument() {

		String fileToOpen = null;

		if (this.previousFiles.size() > 1) {
			this.nextFiles.push(this.previousFiles.pop());
			fileToOpen = (String) this.previousFiles.pop();
		}

		return fileToOpen;
	}

	public String getNextDocument() {

		String fileToOpen = null;

		if (!this.nextFiles.isEmpty()) fileToOpen = (String) this.nextFiles.pop();

		return fileToOpen;
	}

	public void addToFileList(String selectedFile) {
		this.previousFiles.push(selectedFile);
	}

}
