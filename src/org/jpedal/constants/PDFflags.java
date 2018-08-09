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
 * PDFflags.java
 * ---------------
 */
package org.jpedal.constants;

public class PDFflags {

	/** see Table 3.20 in PDF spec for meaning of values returned */
	public static final Integer USER_ACCESS_PERMISSIONS = 1;

	/** ask JPedal for status on password */
	public static final Integer VALID_PASSWORD_SUPPLIED = 2;

	/** possible return keys from VALID_PASSWORD_SUPPLIED */
	public static final int NO_VALID_PASSWORD = 0;
	public static final int VALID_USER_PASSWORD = 1;
	public static final int VALID_OWNER_PASSWORD = 2;

	public static final int IS_FILE_VIEWABLE = 100;

	public static final int IS_FILE_ENCRYPTED = 101;

	public static final int IS_METADATA_ENCRYPTED = 102;

	public static final int IS_EXTRACTION_ALLOWED = 103;

	public static final int IS_PASSWORD_SUPPLIED = 104;
}
