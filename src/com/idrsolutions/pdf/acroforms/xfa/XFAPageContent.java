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
 * XFAPageContent.java
 * ---------------
 */
package com.idrsolutions.pdf.acroforms.xfa;

import java.util.ArrayList;

import org.w3c.dom.Node;

/**
 * Class hold the subform list and corresponding page area nodes
 */
public class XFAPageContent {
	private Node pageAreaNode = null;// template of page
	private ArrayList<Node> nodeList = new ArrayList<Node>();// subforms and other nodes

	public Node getPageAreaNode() {
		return this.pageAreaNode;
	}

	public void setPageAreaNode(Node pageAreaNode) {
		this.pageAreaNode = pageAreaNode;
	}

	public ArrayList<Node> getNodeList() {
		return this.nodeList;
	}
}
