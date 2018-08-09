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
 * XFAStreamDecoder.java
 * ---------------
 */
package org.jpedal.parser;

import java.util.ArrayList;

import org.jpedal.exception.PdfException;
import org.jpedal.fonts.glyph.T3Size;
import org.jpedal.io.PdfObjectReader;
import org.jpedal.objects.acroforms.rendering.AcroRenderer;
import org.jpedal.objects.layers.PdfLayerList;
import org.jpedal.objects.raw.PdfDictionary;
import org.jpedal.objects.raw.PdfObject;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * decode the XML tags inside an XFA stream and write to display
 */
public class XFAStreamDecoder extends PdfStreamDecoder {

	AcroRenderer formRenderer;

	public XFAStreamDecoder(PdfObjectReader currentPdfFile, boolean useHiResImageForDisplay, PdfLayerList pdfLayerList, AcroRenderer formRenderer) {
		super(currentPdfFile, useHiResImageForDisplay, pdfLayerList);

		this.formRenderer = formRenderer;
	}

	@Override
	public final T3Size decodePageContent(PdfObject pdfObject) throws PdfException {

		Node root = this.formRenderer.getXFA(PdfDictionary.XFA_TEMPLATE);

		/**
		 * for the moment just print out content
		 */
		parseNode(root, new ArrayList<Node>(), 0);

		return null;
	}

	private static void parseNode(Node nodeToParse, ArrayList<Node> nodeList, int indent) {

		for (int ii = 0; ii < indent; ii++) {
			System.out.print(" ");
		}
		System.out.print(nodeToParse);

		System.out.println("[" + nodeToParse.getTextContent() + " ]");

		nodeList.add(nodeToParse);
		NodeList setOfNodes = nodeToParse.getChildNodes();
		for (int i = 0; i < setOfNodes.getLength(); i++) {
			parseNode(setOfNodes.item(i), nodeList, indent + 3);
		}
	}

}
