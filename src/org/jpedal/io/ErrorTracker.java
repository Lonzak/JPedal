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
 * ErrorTracker.java
 * ---------------
 */
package org.jpedal.io;

/**
 * Used to log errors in printing
 */
public class ErrorTracker {

	/** flag used to show if printing worked */
	public boolean pageSuccessful = true;

	/** Any printer errors */
	private String pageErrorMessages = "";

	/**
	 * flag to show if printing failed use getPageDecodeStatus(DecodeStatus.PageDecodingSuccessful)
	 * 
	 * public boolean isPageSuccessful() {
	 * 
	 * return pageSuccessful; }/
	 **/

	/**
	 * return list of messages
	 */
	public String getPageFailureMessage() {
		return this.pageErrorMessages;
	}

	/**
	 * add message on problem
	 */
	public void addPageFailureMessage(String value) {
		this.pageSuccessful = false;
		this.pageErrorMessages = this.pageErrorMessages + value + '\n';
	}
}
