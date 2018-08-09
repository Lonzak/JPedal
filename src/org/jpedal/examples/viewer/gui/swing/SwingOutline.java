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
 * SwingOutline.java
 * ---------------
 */
package org.jpedal.examples.viewer.gui.swing;

import java.util.HashMap;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.ScrollPaneConstants;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeSelectionModel;

import org.jpedal.examples.viewer.gui.generic.GUIOutline;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** holds tree outline displayed in nav bar */
public class SwingOutline extends JScrollPane implements GUIOutline {

	private static final long serialVersionUID = 7137966529258622413L;

	/** used by tree to convert page title into page number */
	private Map pageLookupTableViaTitle = new HashMap();

	// private Map pageLookupTableViaNodeNumber=new HashMap();
	private Map nodeToRef = new HashMap();

	// list of closed nodes so we display closed at start
	private Map closedNodes = new HashMap();

	/** used by tree to find point to scroll to */
	// private Map pointLookupTable=new HashMap();

	private DefaultMutableTreeNode top = new DefaultMutableTreeNode("Root"); //$NON-NLS-1$

	private JTree tree;

	private boolean hasDuplicateTitles;

	/** specify bookmark for each page */
	// private String[] defaultRefsForPage;

	// private TreeNode[] defaultPageLookup;

	public SwingOutline() {
		this.getViewport().add(new JLabel("No outline"));
	}

	@Override
	public void reset(Node rootNode) {

		this.top.removeAllChildren();
		if (this.tree != null) getViewport().remove(this.tree);

		this.pageLookupTableViaTitle.clear();

		this.nodeToRef.clear();

		this.closedNodes.clear();

		this.hasDuplicateTitles = false;

		/**
		 * default settings for bookmarks for each page
		 */
		// defaultRefsForPage=decode_pdf.getOutlineDefaultReferences();
		// this.defaultPageLookup=new TreeNode[this.pageCount];

		if (rootNode != null) {
			this.hasDuplicateTitles = false;
			readChildNodes(rootNode, this.top, 0);
		}
		this.tree = new JTree(this.top);
		this.tree.setName("Tree");

		if (rootNode != null) expandAll();

		this.tree.setRootVisible(false);

		this.tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

		// create display for bookmarks
		getViewport().add(this.tree);

		setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
	}

	/**
	 * Expand all nodes found from the XML outlines for the PDF.
	 */
	private void expandAll() {
		int row = 1;
		// while (row < tree.getRowCount()) {
		while (row < 4) {

			if (!this.closedNodes.containsKey(row)) this.tree.expandRow(row);
			else this.tree.collapseRow(row);

			row++;
		}
	}

	/**
	 * Scans sublist to get the children bookmark nodes.
	 * 
	 * @param rootNode
	 *            Node
	 * @param topNode
	 *            DefaultMutableTreeNode
	 */
	@Override
	public int readChildNodes(Node rootNode, DefaultMutableTreeNode topNode, int nodeIndex) {

		if (topNode == null) topNode = this.top;

		NodeList children = rootNode.getChildNodes();
		int childCount = children.getLength();

		for (int i = 0; i < childCount; i++) {

			Node child = children.item(i);

			Element currentElement = (Element) child;

			String title = currentElement.getAttribute("title");
			String page = currentElement.getAttribute("page");
			String isClosed = currentElement.getAttribute("isClosed");
			String ref = currentElement.getAttribute("objectRef");

			// String ref=currentElement.getAttribute("objectRef");

			/** create the lookup table */

			if (this.pageLookupTableViaTitle.containsKey(title)) {
				this.hasDuplicateTitles = true;
			}
			else {
				this.pageLookupTableViaTitle.put(title, page);
			}

			if (isClosed.equals("true")) this.closedNodes.put(nodeIndex, "x");

			// build lookup tables so we can cross-ref against tree in viewer
			// pageLookupTableViaNodeNumber.put(new Integer(nodeIndex), page);
			this.nodeToRef.put(nodeIndex, ref);

			nodeIndex++;

			/** create the point lookup table */
			// if ((rawDest != null) && (rawDest.indexOf("/XYZ") != -1)) {
			//
			// rawDest = rawDest.substring(rawDest.indexOf("/XYZ") + 4);
			//
			// StringTokenizer values = new StringTokenizer(rawDest, "[] ");
			//
			// //ignore the first, read next 2
			//
			// String x = values.nextToken();
			// if (x.equals("null"))
			// x = "0";
			// String y = values.nextToken();
			// if (y.equals("null"))
			// y = "0";
			//
			// pointLookupTable.put(title, new Point((int) Float.parseFloat(x), (int) Float.parseFloat(y)));
			// }

			DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(title);

			/** add the nodes or initialise to top level */
			topNode.add(childNode);

			if (child.hasChildNodes()) nodeIndex = readChildNodes(child, childNode, nodeIndex);

		}

		return nodeIndex;
	}

	@Override
	public String getPage(String title) {
		if (this.hasDuplicateTitles) return null;
		// throw new RuntimeException("Bookmark "+title+" not unique");
		else return (String) this.pageLookupTableViaTitle.get(title);
	}

	/**
	 * public String getPageViaNodeNumber(int nodeNumber){ return (String) pageLookupTableViaNodeNumber.get(new Integer(nodeNumber)); }/
	 **/

	@Override
	public String convertNodeIDToRef(int nodeNumber) {
		return (String) this.nodeToRef.get(nodeNumber);
	}

	/**
	 * Handles the functionality for highlighting the correct bookmark tree node for the page we opened the PDF to.
	 */
	@Override
	public void selectBookmark() {

		/**
		 * code to walk not fully operational so only runs on example
		 * 
		 * traverse();
		 * 
		 * try{ System.out.println(defaultPageLookup[this.currentPage-1]); ignoreAlteredBookmark=true; tree.setSelectionPath(new
		 * TreePath(defaultPageLookup[this.currentPage])); ignoreAlteredBookmark=false;
		 * System.out.println(tree.getSelectionPath()+" "+currentPage+" "+defaultPageLookup[this.currentPage-1]); }catch(Exception e){
		 * e.printStackTrace(); System.exit(1); }
		 **/
	}

	// public Point getPoint(String title) {
	//
	// return (Point) pointLookupTable.get(title);
	// }

	@Override
	public Object getTree() {
		return this.tree;
	}

	@Override
	public DefaultMutableTreeNode getLastSelectedPathComponent() {
		return (DefaultMutableTreeNode) this.tree.getLastSelectedPathComponent();
	}
}
