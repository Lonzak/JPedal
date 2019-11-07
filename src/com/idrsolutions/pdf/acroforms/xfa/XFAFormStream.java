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
 * XFAFormStream.java
 * ---------------
 */
package com.idrsolutions.pdf.acroforms.xfa;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.jpedal.io.PdfObjectReader;
import org.jpedal.objects.acroforms.utils.ConvertToString;
import org.jpedal.objects.raw.FormObject;
import org.jpedal.objects.raw.FormStream;
import org.jpedal.objects.raw.PdfArrayIterator;
import org.jpedal.objects.raw.PdfDictionary;
import org.jpedal.objects.raw.PdfObject;
import org.jpedal.objects.raw.StreamObject;
import org.jpedal.utils.LogWriter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * XFA version to turn XFA data into Form Object
 */
public class XFAFormStream extends FormStream {

	private static boolean showBug = false;

	// DEBUG Flags
	private static final boolean debugXFAstream = false;// old
	/** shows the root XFA data stream */
	private static final boolean showXFAdata = false;

	// local variables
	/** used so create is only called once. */
	private boolean calledOnce = false;

	private Node config;
	private Node dataset;
	private Node template;

	// OLD code variables
	/** used for the current formObject and has methods to help generate the field */
	private XFAFormObject formObject;

	/** stores the array of components as they are read and setup */
	private LinkedList<XFAFormObject> xfaFormList;

	// private Node[] nodes;

	/** used to store the page number for each field */
	private String pagenum;

	private int contentX;
	// private int contentY;
	// private int contentW;
	private int contentH;

	/** node name to value map */
	private Map<String, String> valueMap = new HashMap<String, String>();

	private static final int UNKNOWN = -1;
	private static final int PAGESET = 1;
	private static final int PAGEAREA = 2;
	private static final int SUBFORM = 3;
	private static final int BREAK = 4;
	private static final int BREAKBEFORE = 5;
	private static final int BREAKAFTER = 6;

	// map to hold page number and related PageContents
	public HashMap<Integer, XFAPageContent> pageMap = new HashMap<Integer, XFAPageContent>();

	// list contains pageArea nodes
	public ArrayList<Node> pageAreaList = new ArrayList<Node>();

	public XFAFormStream(PdfObject acroFormObj, PdfObjectReader inCurrentPdfFile) {

		this.currentPdfFile = inCurrentPdfFile;

		readXFA(acroFormObj);
	}

	private Node toDocument(int type, byte[] xmlString) {
		// NodeList nodes;
		// Element currentElement;

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		Document content = null;
		try {
			content = factory.newDocumentBuilder().parse(new ByteArrayInputStream(xmlString));

			// /**
			// * get the print values and extract info
			// */
			// nodes = doc.getElementsByTagName("print");
			//
			// currentElement = (Element) nodes.item(0);
		}
		catch (Exception e) {
			content = null;
		}

		/**
		 * SHOW Data
		 */
		if (showXFAdata) {
			if (type == PdfDictionary.XFA_TEMPLATE) {
				System.out.println("xfaTemplate=================");
			}
			else
				if (type == PdfDictionary.XFA_DATASET) {
					System.out.println("XFA_DATASET=================");
				}
				else
					if (type == PdfDictionary.XFA_CONFIG) {
						System.out.println("xfaConfig=================");
					}

			InputStream stylesheet = this.getClass().getResourceAsStream("/org/jpedal/examples/text/xmlstyle.xslt");

			TransformerFactory transformerFactory = TransformerFactory.newInstance();

			/** output tree */
			try {
				Transformer transformer = transformerFactory.newTransformer(new StreamSource(stylesheet));

				// useful for debugging
				transformer.transform(new DOMSource(content), new StreamResult(System.out));

				System.out.println("/n==========================================");

			}
			catch (Exception e) {

				// tell user and log
				if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
			}

		}

		return content;
	}

	@Override
	public Node getXFA(int type) {

		Node returnValue = null;

		switch (type) {

			case PdfDictionary.XFA_CONFIG:
				returnValue = this.config;
				break;

			case PdfDictionary.XFA_DATASET:
				returnValue = this.dataset;
				break;

			case PdfDictionary.XFA_TEMPLATE:
				returnValue = this.template;
				break;

			case PdfDictionary.XFA_PREAMBLE:
				// currentAcroFormData.setXFAFormData(PdfFormData.XFA_PREAMBLE,objData);
				break;

			case PdfDictionary.XFA_LOCALESET:
				// currentAcroFormData.setXFAFormData(PdfFormData.XFA_LOCALESET,objData);
				break;

			case PdfDictionary.XFA_PDFSECURITY:
				// currentAcroFormData.setXFAFormData(PdfFormData.XFA_PDFSECURITY,objData);
				break;

			case PdfDictionary.XFA_XMPMETA:
				// currentAcroFormData.setXFAFormData(PdfFormData.XFA_XMPMETA,objData);
				break;

			case PdfDictionary.XFA_XFDF:
				// currentAcroFormData.setXFAFormData(PdfFormData.XFA_XFDF,objData);
				break;

			case PdfDictionary.XFA_POSTAMBLE:
				// currentAcroFormData.setXFAFormData(PdfFormData.XFA_POSTAMBLE,objData);
				break;

			default:
				// System.out.println("type="+type+" str="+str+" "+new String(objData));
				break;
		}

		return returnValue;
	}

	private void readXFA(PdfObject acroFormObj) {

		/** flag if XFA */
		PdfObject XFAasStream = null;
		PdfArrayIterator XFAasArray = null;
		XFAasStream = acroFormObj.getDictionary(PdfDictionary.XFA);
		if (XFAasStream == null) {
			XFAasArray = acroFormObj.getMixedArray(PdfDictionary.XFA);

			// empty array
			if (XFAasArray != null && XFAasArray.getTokenCount() == 0) XFAasArray = null;
		}

		/** decide if ref to object or list of objects */
		if (XFAasStream != null) {

			byte[] decodedStream = XFAasStream.getDecodedStream();

			if (debug) {
				this.config = xmlToNode(PdfDictionary.XFA_CONFIG, decodedStream);
				System.out.println("\nConfig");
				System.out.println("config length = " + this.config.getChildNodes().getLength());
				ConvertToString.convertDocumentToString(this.config);
			}

			if (debug) {
				this.dataset = xmlToNode(PdfDictionary.XFA_DATASET, decodedStream);
				System.out.println("\n\ndataset");
				System.out.println("dataset length = " + this.dataset.getChildNodes().getLength());
				ConvertToString.convertDocumentToString(this.dataset);
			}

			this.template = xmlToNode(PdfDictionary.XFA_TEMPLATE, decodedStream);

			if (debug) {
				System.out.println("\n\ntemplate");
				System.out.println("template length = " + this.template.getChildNodes().getLength());
				ConvertToString.convertDocumentToString(this.template);
			}

		}
		else {

			/**
			 * read XFA values
			 */
			PdfObject obj = null;
			int type = 0;

			while (XFAasArray != null && XFAasArray.hasMoreTokens()) {

				type = XFAasArray.getNextValueAsConstant(true);

				obj = new StreamObject(XFAasArray.getNextValueAsString(true));

				this.currentPdfFile.readObject(obj);

				byte[] objData = obj.getDecodedStream();

				switch (type) {

					case PdfDictionary.XFA_CONFIG:
						this.config = toDocument(PdfDictionary.XFA_CONFIG, objData);
						break;

					case PdfDictionary.XFA_DATASET:
						this.dataset = toDocument(PdfDictionary.XFA_DATASET, objData);
						break;

					case PdfDictionary.XFA_TEMPLATE:
						this.template = toDocument(PdfDictionary.XFA_TEMPLATE, objData);
						break;

					case PdfDictionary.XFA_PREAMBLE:
						// currentAcroFormData.setXFAFormData(PdfFormData.XFA_PREAMBLE,objData);
						break;

					case PdfDictionary.XFA_LOCALESET:
						// currentAcroFormData.setXFAFormData(PdfFormData.XFA_LOCALESET,objData);
						break;

					case PdfDictionary.XFA_PDFSECURITY:
						// currentAcroFormData.setXFAFormData(PdfFormData.XFA_PDFSECURITY,objData);
						break;

					case PdfDictionary.XFA_XMPMETA:
						// currentAcroFormData.setXFAFormData(PdfFormData.XFA_XMPMETA,objData);
						break;

					case PdfDictionary.XFA_XFDF:
						// currentAcroFormData.setXFAFormData(PdfFormData.XFA_XFDF,objData);
						break;

					case PdfDictionary.XFA_POSTAMBLE:
						// currentAcroFormData.setXFAFormData(PdfFormData.XFA_POSTAMBLE,objData);
						break;

					default:
						// System.out.println("type="+type+" str="+str+" "+new String(objData));
						break;
				}
			}
		}
	}

	private static Node xmlToNode(int xfaConfig, byte[] decodedStream) {

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		Document document = null;
		try {
			document = factory.newDocumentBuilder().parse(new ByteArrayInputStream(decodedStream));
		}
		catch (Exception e) {
			document = null;
		}

		switch (xfaConfig) {
			case PdfDictionary.XFA_CONFIG: {

				NodeList nodes = document.getElementsByTagName("config");

				for (int i = 0; i < nodes.getLength(); i++) {
					Element element = (Element) nodes.item(i);
					String att = element.getAttribute("xmlns");

					if (att.length() > 0) {
						return element;
					}
				}

				break;
			}
			case PdfDictionary.XFA_DATASET: {
				// xfa:datasets

				NodeList nodes = document.getElementsByTagName("xfa:datasets");

				Element element = (Element) nodes.item(0);

				return element;

				// break;
			}
			case PdfDictionary.XFA_TEMPLATE: {

				NodeList nodes = document.getElementsByTagName("template");

				for (int i = 0; i < nodes.getLength(); i++) {
					Element element = (Element) nodes.item(i);
					String att = element.getAttribute("xmlns:xfa");

					if (att.length() > 0) {
						// DocumentBuilderFactory f1 = DocumentBuilderFactory.newInstance();
						//
						// try {
						// Document d1 = f1.newDocumentBuilder().newDocument();
						// d1.importNode(element, true);
						//
						// return d1;
						// } catch (ParserConfigurationException e) {
						// e.printStackTrace();
						// }

						// template = element;

						return element;
					}
				}

				break;
			}
		}

		return null;
	}

	@Override
	public boolean hasXFADataSet() {
		return this.dataset != null;
	}

	// ########## chris START old code
	public FormObject[] createAppearanceString(FormObject[] forms) {

		if (!this.calledOnce) {
			this.calledOnce = true;
		}
		else {
			return null;
		}

		return oldXFA(forms);
	}

	private FormObject[] oldXFA(FormObject[] forms) {

		this.xfaFormList = new LinkedList<XFAFormObject>();

		// create Raw XFA objects
		parseStream();

		/**
		 * mix in with Form Objects
		 * 
		 * matchedforms[i][0] contains original matchedforms[i][1] contains XFA version
		 */

		int xfaListSize = this.xfaFormList.size();
		int listSize = forms.length < xfaListSize ? forms.length : xfaListSize;
		FormObject[][] matchedforms = new FormObject[listSize][2];
		XFAFormObject xfaForm;
		int i;
		String formName;
		for (i = 0; i < listSize; i++) {
			matchedforms[i][0] = forms[i];
			// System.out.println("annot="+forms[i].getFieldName()+"<");
			if (forms[i] != null) {
				formName = forms[i].getTextStreamValue(PdfDictionary.T);

				if (formName != null) {
					int index2 = formName.lastIndexOf("[0]");
					int index1 = formName.lastIndexOf('.', index2 - 1);
					if (index1 != -1) {
						if (index2 != -1) {
							formName = formName.substring(index1 + 1, index2);
						}
						else {
							formName = formName.substring(index1 + 1);
						}
					}
					else {
						if (index2 != -1) {
							formName = formName.substring(0, index2);
						}
						else {
							// do NOT alter NOT needed.
						}
					}
				}
			}
			else {
				formName = null;
			}

			for (Object aXfaFormList : this.xfaFormList) {
				xfaForm = (XFAFormObject) aXfaFormList;
				// System.out.println("xfa="+xfaForm);
				if (xfaForm != null) {
					// System.out.println("xfa="+xfaForm.getFieldName()+" formname="+formName);
					String xfaName = xfaForm.getTextStreamValue(PdfDictionary.T);
					if (xfaName.equals(formName)) {
						matchedforms[i][1] = xfaForm;
						break;
					}
				}
			}

			if (formName != null) {
				String newVal = this.valueMap.get(formName.toLowerCase());

				if (newVal != null) matchedforms[i][0].setTextValue(newVal);
			}
		}

		// use either original or new version in which case add in data
		forms = new FormObject[listSize];
		for (i = 0; i < listSize; i++) {
			if (matchedforms[i][1] != null) {
				matchedforms[i][1].overwriteWith(matchedforms[i][0]);
				forms[i] = matchedforms[i][1];
				if (XFAFormStream.showBug) System.out.println("wrong " + i);
			}
			else {
				forms[i] = matchedforms[i][0];
				if (XFAFormStream.showBug) System.out.println("correct " + i);
			}
			if (XFAFormStream.showBug) System.out.println(i + " " + forms[i].getTextString());
		}
		return forms;
	}

	protected void parseStream() {
		ArrayList<Node> nodelist = new ArrayList<Node>();
		parseNode(this.template, nodelist);

		setupTemplate(nodelist.iterator());

		// datasets should be done after the template
		ArrayList<Node> datalist = new ArrayList<Node>();
		parseNode(this.dataset, datalist);

		// MUST be called first to populate the values map that is then called once field names are definded
		setupDataSet(datalist.iterator());
	}

	private void setupDataSet(Iterator<Node> nodeIterator) {
		while (nodeIterator.hasNext()) {
			Node node = nodeIterator.next();
			String nodeName = node.getNodeName();

			if (nodeName.equals("xfa:data")) {
				data(node, nodeIterator);

			}
			else {}
		}
	}

	private void data(Node node, Iterator<Node> nodeIterator) {
		NodeList childs = node.getChildNodes();
		for (int i = 0; i < childs.getLength(); i++) {
			Node chnode = childs.item(i);
			// String nodeName = chnode.getNodeName();//comment out

			NodeList kidNodes = chnode.getChildNodes();
			if (kidNodes.getLength() > 0) {
				Node kidNode = kidNodes.item(0);
				kidNodes = kidNode.getChildNodes();
				String nodeName = kidNode.getNodeName();

				if (kidNodes.getLength() > 0) {

					Node valueNd = kidNodes.item(0);
					this.valueMap.put(nodeName.toLowerCase(), valueNd.getNodeValue());
				}
			}
		}
	}

	/**
	 * creates a new formobject, used to help generate each field, and appends old one to LinkedList sent in to this method but private to the
	 * parseStream method
	 */
	private void nextFormObject() {
		// System.out.println("nextForm===================");
		// boolean test = false;
		if (this.formObject != null) {
			// System.out.println("added="+formObject);
			this.xfaFormList.add(this.formObject);
			// test = true;
		}
		this.formObject = new XFAFormObject();
		//
		// if(test){
		// System.out.println("TEST IF changed in linkedlist");
		// System.out.println("list="+xfaFormList.getLast());
		// System.out.println("current="+formObject);
		// }
	}

	private static void parseNode(Node nodeToParse, ArrayList<Node> nodeList) {
		nodeList.add(nodeToParse);
		NodeList setOfNodes = nodeToParse.getChildNodes();
		for (int i = 0; i < setOfNodes.getLength(); i++) {
			parseNode(setOfNodes.item(i), nodeList);
		}
	}

	/**
	 * tests the name and parses its value
	 */
	private void setupTemplate(Iterator<Node> nodeIterator) {
		while (nodeIterator.hasNext()) {
			Node node = nodeIterator.next();
			String nodeName = node.getNodeName();

			if (nodeName.equals("field")) {
				field(node, nodeIterator);

			}
			else
				if (nodeName.equals("pageArea")) {
					// store as the page these forms apply to
					if (debugXFAstream) System.out.println("  pagearea=" + node.getNodeValue() + " att=" + node.getAttributes());

					NamedNodeMap att = node.getAttributes();

					// store page for this set of fields
					Node tmp = att.getNamedItem("id");
					this.pagenum = tmp.getNodeValue();
					this.pagenum = this.pagenum.substring(this.pagenum.indexOf("Page") + 4);

					if (debugXFAstream) {
						tmp = att.getNamedItem("name");
						System.out.println("name=" + tmp.getNodeValue() + " att=" + tmp.getAttributes() + " childs="
								+ tmp.getChildNodes().getLength());
					}

				}
				else
					if (nodeName.equals("contentArea")) {
						NamedNodeMap att = node.getAttributes();
						if (att != null) {
							// gather x,y,w,h,name from field
							Node tmp;
							if ((tmp = att.getNamedItem("x")) != null) {
								this.contentX = resolveMeasurementToPoints(tmp.getNodeValue());
							}
							// if ((tmp = att.getNamedItem("y")) != null) {
							// contentY = resolveMeasurementToPoints(tmp.getNodeValue());
							// }
							// if ((tmp = att.getNamedItem("w")) != null) {
							// contentW = resolveMeasurementToPoints(tmp.getNodeValue());
							// }
							if ((tmp = att.getNamedItem("h")) != null) {
								this.contentH = resolveMeasurementToPoints(tmp.getNodeValue());
							}
						}

						// }else if(nodeName.equals("template")){
						// template(node,nodeIterator);
					}
					else
						if (nodeName.equals("#document")) {
							// ignore
						}
						else
							if (nodeName.equals("templateDesigner")) {
								// ignore
							}
							else {}
		}
		nextFormObject();
	}

	private static int resolveMeasurementToPoints(String attValue) {
		int val = 0;

		if (attValue.endsWith("pt")) {// 1 pt = 1 pt on screen
			val = new Double(attValue.substring(0, attValue.indexOf("pt"))).intValue();

		}
		else
			if (attValue.endsWith("in")) {// 1 in = 72 pt
				val = (int) (72 * (Double.parseDouble(attValue.substring(0, attValue.indexOf("in")))));

			}
			else
				if (attValue.endsWith("cm")) {// 1 cm = 28.35 pt
					val = (int) (28.35 * (Double.parseDouble(attValue.substring(0, attValue.indexOf("cm")))));

				}
				else
					if (attValue.endsWith("mm")) {// 1 mm = 2.835 pt
						val = (int) (2.835 * (Double.parseDouble(attValue.substring(0, attValue.indexOf("mm")))));

					}
					else {
						LogWriter.writeFormLog("UNIMPLEMENTED type of y size=" + attValue, FormStream.debugUnimplemented);
					}
		return val;
	}

	private void field(Node nodeToParse, Iterator<Node> nodeIterator) {
		// save current setup and move on to next field
		nextFormObject();

		// recall and save pagenumber for this field
		this.formObject.setPageNumber(this.pagenum);

		NamedNodeMap att = nodeToParse.getAttributes();
		// System.out.println("field rect="+att);
		if (att != null) {
			// gather x,y,w,h,name from field
			Node tmp;
			if ((tmp = att.getNamedItem("x")) != null) {
				this.formObject.setX(this.contentX + resolveMeasurementToPoints(tmp.getNodeValue()));
			}
			if ((tmp = att.getNamedItem("y")) != null) {
				// System.out.println("y="+contentY+" h="+contentH);
				this.formObject.setY(this.contentH - resolveMeasurementToPoints(tmp.getNodeValue()));
			}
			if ((tmp = att.getNamedItem("w")) != null) {
				// contentW
				this.formObject.setWidth(resolveMeasurementToPoints(tmp.getNodeValue()));
			}
			if ((tmp = att.getNamedItem("h")) != null) {
				// contentH
				this.formObject.setHeight(resolveMeasurementToPoints(tmp.getNodeValue()));
			}
			if ((tmp = att.getNamedItem("name")) != null) {
				// - we now have annot and alt names in FormObject as well
				this.formObject.setTextStreamValue(PdfDictionary.T, tmp.getNodeValue());
			}
			else {
				this.formObject.setTextStreamValue(PdfDictionary.T, "");
			}
		}

		// find ui node, should be next node
		// reform to trap field or ui alone
		Node uiNode = null;
		while (nodeIterator.hasNext()) {
			uiNode = nodeIterator.next();
			if (uiNode.getNodeName().equals("ui")) {
				break;
			}
			else {
				uiNode = null;
			}
		}

		if (uiNode == null) {
			System.out.println("ERROR ERROR  ERROR no ui in field=" + ConvertToString.convertDocumentToString(nodeToParse));
		}
		else {
			if (debugXFAstream) System.out.println("ui=" + uiNode);
			Node tmpNode = null;
			String nodeName = null;
			while (nodeIterator.hasNext()) {
				tmpNode = nodeIterator.next();
				nodeName = tmpNode.getNodeName();

				if (nodeName.equals("checkButton")) {
					checkButton(tmpNode, nodeIterator);
				}
				else
					if (nodeName.equals("button")) {
						button(tmpNode, nodeIterator);
					}
					else
						if (nodeName.equals("choiceList")) {
							choiceList(tmpNode, nodeIterator);
						}
						else
							if (nodeName.equals("textEdit")) {
								textEdit(tmpNode, nodeIterator);
							}
							else {
								LogWriter.writeFormLog("node not implemented nodename=" + nodeName, debugUnimplemented);
							}
			}
		}
	}

	private void textEdit(Node nodeToParse, Iterator<Node> nodeIterator) {
		if (debugXFAstream) System.out.println("textEdit - ");

		this.formObject.setType(PdfDictionary.Tx, true);
		this.formObject.setFlag(FormObject.MULTILINE_ID, true);
		this.formObject.setFlag(FormObject.PASSWORD_ID, false);

		String nodeName = nodeToParse.getNodeName();
		Object nodeValue = nodeToParse.getNodeValue();
		if (debugXFAstream) System.out.println("textEdit=" + nodeName + " = " + nodeValue);

		NamedNodeMap att = nodeToParse.getAttributes();
		if (att != null) {
			if (debugXFAstream) System.out.println(" attributes=" + att.toString());
		}

		Node tmpNode;
		String nodename;
		while (nodeIterator.hasNext()) {
			tmpNode = nodeIterator.next();
			nodename = tmpNode.getNodeName();

			if (nodename.equals("templateDesigner")) {
				// ignore
			}
			else
				if (nodename.equals("field")) {
					field(tmpNode, nodeIterator);

				}
				else
					if (nodename.equals("value")) {
						if (debugXFAstream) System.out.println("  value=" + tmpNode.toString());

					}
					else
						if (nodename.equals("caption")) {
							if (debugXFAstream) System.out.println("  caption=" + tmpNode.toString());

						}
						else
							if (nodename.equals("text")) {
								// System.out.println("   text="+tmpNode);
								if (nodeIterator.hasNext()) {
									Node tmp = nodeIterator.next();
									if (debugXFAstream) System.out.println("    text=" + tmp.getNodeValue());
									// @xfa text - this is broken
									// if you open f1_60 (bottom right corner) ends up with Do I need an EIN
									// which is actually from a text box on page 2
									// System.out.println("setting text value="+tmp.getNodeValue());
									// System.out.println("formobject="+formObject.getFieldName());
									// formObject.setTextValue(tmp.getNodeValue());

									// System.out.println("name="+tmp.getNodeName());
									// valueMap.put(key,tmp.getNodeValue());
								}

							}
							else
								if (nodename.equals("para")) {
									if (debugXFAstream) System.out.println("  para=" + tmpNode.toString());

									NamedNodeMap var = tmpNode.getAttributes();
									Node tmp;
									if ((tmp = var.getNamedItem("hAlign")) != null) {
										if (debugXFAstream) System.out.println("horiz=" + tmp.getNodeValue());
										this.formObject.setHorizontalAlign(tmp.getNodeValue());
									}
									if ((tmp = var.getNamedItem("vAlign")) != null) {
										if (debugXFAstream) System.out.println("vertic=" + tmp.getNodeValue());
										this.formObject.setVerticalAllign(tmp.getNodeValue());
									}

								}
								else
									if (nodename.equals("font")) {
										Node tmp = tmpNode.getAttributes().getNamedItem("typeface");
										if (tmp != null) {
											if (debugXFAstream) System.out.println("  font=" + tmp.getNodeValue());
											// @xfa font
											// Font font = new Font(tmp.getNodeValue(),style,size);
											// formObject.setTextFont();
										}
									}
									else
										if (nodename.equals("edge")) {
											Node tmp = tmpNode.getAttributes().getNamedItem("stroke");
											if (tmp != null) {
												if (debugXFAstream) System.out.println("  edge=" + tmp.getNodeValue());
												this.formObject.setBorderStroke(tmp.getNodeValue());
											}
										}
										else
											if (nodename.equals("border")) {
												if (debugXFAstream) System.out.println("  border=" + tmpNode.toString());

											}
											else
												if (nodename.equals("margin")) {
													if (debugXFAstream) System.out.println("  margin=" + tmpNode.toString());

												}
												else
													if (nodename.equals("proto")) {
														if (debugXFAstream) System.out.println("  proto=" + tmpNode.toString());

													}
													else {
														LogWriter.writeFormLog("node name not implemented in textEdit name=" + nodename,
																debugUnimplemented);
													}
		}
	}

	private void choiceList(Node nodeToParse, Iterator<Node> nodeIterator) {
		if (debugXFAstream) System.out.println("choiceList - ");

		this.formObject.setType(PdfDictionary.Ch, true);
		this.formObject.setFlag(FormObject.COMBO_ID, false);

		String nodeName = nodeToParse.getNodeName();
		Object nodeValue = nodeToParse.getNodeValue();
		if (debugXFAstream) System.out.println(nodeName + " = " + nodeValue);

		NamedNodeMap att = nodeToParse.getAttributes();
		if (att != null) {
			if (debugXFAstream) System.out.println(" attributes=" + att.toString());
			Node tmp;
			if ((tmp = att.getNamedItem("open")) != null) {
				this.formObject.setChoiceOpening(tmp.getNodeValue());
			}
		}

		Node tmpNode;
		String nodename;
		while (nodeIterator.hasNext()) {
			tmpNode = nodeIterator.next();
			nodename = tmpNode.getNodeName();

			if (nodename.equals("templateDesigner")) {
				// ignore
			}
			else
				if (nodename.equals("field")) {
					field(tmpNode, nodeIterator);

				}
				else
					if (nodename.equals("value")) {
						if (debugXFAstream) System.out.println("  value=" + tmpNode.toString());

					}
					else
						if (nodename.equals("caption")) {
							if (debugXFAstream) System.out.println("  caption=" + tmpNode.toString());

						}
						else
							if (nodename.equals("items")) {
								// System.out.println("   items="+tmpNode);

								/**
								 * NodeList items = tmpNode.getChildNodes(); Node tmpItem; String[] listOfItems = new String[items.getLength()]; for
								 * (int i = 0; i < items.getLength(); i++) { tmpItem = items.item(i); if (tmpItem.getNodeName().equals("text")) { if
								 * (debugXFAstream) System.out.println("text item" + i + '=' + tmpItem.getChildNodes().item(0));
								 * 
								 * NodeList kidNodes = tmpItem.getChildNodes(); if(kidNodes.getLength()>0) listOfItems[i] =
								 * kidNodes.item(0).getNodeValue(); } } //formObject.setlistOfItems(listOfItems, true); /
								 **/

							}
							else
								if (nodename.equals("text")) {
									// System.out.println("   text="+tmpNode);
									if (nodeIterator.hasNext()) {
										Node tmp = nodeIterator.next();
										if (debugXFAstream) System.out.println("    text=" + tmp);
										this.formObject.setTextValue(tmp.getNodeValue());
									}

								}
								else
									if (nodename.equals("para")) {
										if (debugXFAstream) System.out.println("  para=" + tmpNode.toString());

										NamedNodeMap var = tmpNode.getAttributes();
										Node tmp;
										if ((tmp = var.getNamedItem("hAlign")) != null) {
											if (debugXFAstream) System.out.println("horiz=" + tmp.getNodeValue());
											this.formObject.setHorizontalAlign(tmp.getNodeValue());
										}
										if ((tmp = var.getNamedItem("vAlign")) != null) {
											if (debugXFAstream) System.out.println("vertic=" + tmp.getNodeValue());
											this.formObject.setVerticalAllign(tmp.getNodeValue());
										}

									}
									else
										if (nodename.equals("font")) {
											Node tmp = tmpNode.getAttributes().getNamedItem("typeface");
											if (tmp != null) {
												if (debugXFAstream) System.out.println("  font=" + tmp.getNodeValue());
												// @xfa font
												// Font font = new Font(tmp.getNodeValue(),style,size);
												// formObject.setTextFont();
											}

										}
										else
											if (nodename.equals("edge")) {
												Node tmp = tmpNode.getAttributes().getNamedItem("stroke");
												if (tmp != null) {
													if (debugXFAstream) System.out.println("  edge=" + tmp.getNodeValue());
													this.formObject.setBorderStroke(tmp.getNodeValue());
												}

											}
											else
												if (nodename.equals("border")) {
													if (debugXFAstream) System.out.println("  border=" + tmpNode.toString());

												}
												else
													if (nodename.equals("margin")) {
														if (debugXFAstream) System.out.println("  margin=" + tmpNode.toString());

													}
													else
														if (nodename.equals("draw")) {
															if (debugXFAstream) System.out.println("  draw=" + tmpNode.toString());

														}
														else
															if (nodename.equals("rectangle")) {
																if (debugXFAstream) System.out.println("  rectangle=" + tmpNode.toString());

															}
															else {
																LogWriter.writeFormLog("node name not implemented in choiceList name=" + nodename,
																		debugUnimplemented);
															}
		}
	}

	private void button(Node nodeToParse, Iterator<Node> nodeIterator) {
		if (debugXFAstream) System.out.println("button - ");

		this.formObject.setType(PdfDictionary.Btn, true);
		this.formObject.setFlag(FormObject.PUSHBUTTON_ID, true);

		String nodeName = nodeToParse.getNodeName();
		Object nodeValue = nodeToParse.getNodeValue();
		if (debugXFAstream) System.out.println(nodeName + " = " + nodeValue);

		NamedNodeMap att = nodeToParse.getAttributes();
		if (att != null) {
			if (debugXFAstream) System.out.println(" attributes=" + att.toString());
		}

		Node tmpNode;
		String nodename;
		while (nodeIterator.hasNext()) {
			tmpNode = nodeIterator.next();
			nodename = tmpNode.getNodeName();

			if (nodename.equals("templateDesigner")) {
				// ignore
			}
			else
				if (nodename.equals("field")) {
					field(tmpNode, nodeIterator);

				}
				else
					if (nodename.equals("edge")) {
						Node tmp = tmpNode.getAttributes().getNamedItem("stroke");
						if (tmp != null) {
							if (debugXFAstream) System.out.println("  edge node=" + tmp.getNodeValue());
							this.formObject.setBorderStroke(tmp.getNodeValue());
						}
					}
					else
						if (nodename.equals("caption")) {
							if (debugXFAstream) System.out.println("  caption node=" + tmpNode.toString());

						}
						else
							if (nodename.equals("value")) {
								if (debugXFAstream) System.out.println("  value=" + tmpNode);

							}
							else
								if (nodename.equals("text")) {
									// System.out.println("   text="+tmpNode);
									if (nodeIterator.hasNext()) {
										Node tmp = nodeIterator.next();
										if (debugXFAstream) System.out.println("    text=" + tmp);
										this.formObject.setNormalCaption(tmp.getNodeValue());
									}

								}
								else
									if (nodename.equals("para")) {
										if (debugXFAstream) System.out.println("  para=" + tmpNode);
										NamedNodeMap var = tmpNode.getAttributes();
										Node tmp;
										if ((tmp = var.getNamedItem("hAlign")) != null) {
											if (debugXFAstream) System.out.println("horiz=" + tmp.getNodeValue());
											this.formObject.setHorizontalAlign(tmp.getNodeValue());
										}
										if ((tmp = var.getNamedItem("vAlign")) != null) {
											if (debugXFAstream) System.out.println("vertic=" + tmp.getNodeValue());
											this.formObject.setVerticalAllign(tmp.getNodeValue());
										}
									}
									else
										if (nodename.equals("font")) {
											Node tmp = tmpNode.getAttributes().getNamedItem("typeface");
											if (tmp != null) {
												if (debugXFAstream) System.out.println("  font=" + tmp.getNodeValue());
												// @xfa font
												// Font font = new Font(tmp.getNodeValue(),style,size);
												// formObject.setTextFont();
											}

										}
										else
											if (nodename.equals("border")) {
												if (debugXFAstream) System.out.println("  border=" + tmpNode);

											}
											else
												if (nodename.equals("fill")) {
													if (debugXFAstream) System.out.println("  fill=" + tmpNode);

												}
												else
													if (nodename.equals("color")) {
														Node tmp = tmpNode.getAttributes().getNamedItem("value");
														if (tmp != null) {
															if (debugXFAstream) System.out.println("  color=" + tmp.getNodeValue());
															this.formObject.setBackgroundColor(tmp.getNodeValue());
														}

													}
													else
														if (nodename.equals("bind")) {
															if (debugXFAstream) System.out.println("  bind=" + tmpNode);

														}
														else
															if (nodename.equals("event")) {
																if (debugXFAstream) System.out.println("   event=" + tmpNode);
																Node tmp = tmpNode.getAttributes().getNamedItem("activity");
																this.formObject.setEventAction(tmp.getNodeValue());

															}
															else
																if (nodename.equals("script")) {
																	if (debugXFAstream) System.out.println("  script=" + tmpNode.toString());
																	NamedNodeMap tmpatt = tmpNode.getAttributes();
																	if (tmpatt != null) {
																		Node contentType = tmpatt.getNamedItem("contentType");
																		if (contentType != null) this.formObject.setScriptType(contentType
																				.getNodeValue());
																	}

																	if (nodeIterator.hasNext()) {
																		Node tmp = nodeIterator.next();
																		if (debugXFAstream) System.out.println("    #text=" + tmp);

																		if (tmp != null) this.formObject.setScript(tmp.getNodeValue());
																	}
																}
																else
																	if (nodename.equals("submit")) {
																		NamedNodeMap subAtt = tmpNode.getAttributes();

																		Node format = subAtt.getNamedItem("format");
																		if (format != null) this.formObject.setSubmitFormat(format.getNodeValue());
																		Node target = subAtt.getNamedItem("target");
																		if (target != null) this.formObject.setSubmitURL(target.getNodeValue());
																		Node textEncoding = subAtt.getNamedItem("textEncoding");
																		if (textEncoding != null) this.formObject.setSubmitTextEncoding(textEncoding
																				.getNodeValue());

																		if (debugXFAstream) {
																			System.out.println("   submit##="
																					+ ConvertToString.convertDocumentToString(tmpNode));
																			ConvertToString.printStackTrace(1);
																		}
																	}
																	else {
																		LogWriter.writeFormLog(
																				"node name not implemented in button name=" + nodename,
																				debugUnimplemented);
																	}
		}
	}

	private void checkButton(Node nodeToParse, Iterator<Node> nodeIterator) {
		if (debugXFAstream) System.out.println("checkButton - ");

		this.formObject.setType(PdfDictionary.Ch, true);
		this.formObject.setFlag(FormObject.COMBO_ID, true);

		String nodeName = nodeToParse.getNodeName();
		Object nodeValue = nodeToParse.getNodeValue();
		if (debugXFAstream) System.out.println(nodeName + " = " + nodeValue);

		NamedNodeMap att = nodeToParse.getAttributes();
		if (att != null) {
			if (debugXFAstream) System.out.println(" attributes=" + att.toString());
		}

		Node tmpNode;
		String nodename;
		while (nodeIterator.hasNext()) {
			tmpNode = nodeIterator.next();
			nodename = tmpNode.getNodeName();

			if (nodename.equals("templateDesigner")) {
				// ignore
			}
			else
				if (nodename.equals("field")) {
					field(tmpNode, nodeIterator);

				}
				else
					if (nodename.equals("value")) {
						if (debugXFAstream) System.out.println("   value=" + tmpNode);

					}
					else
						if (nodename.equals("caption")) {
							if (debugXFAstream) System.out.println("  caption node=" + tmpNode.toString());

						}
						else
							if (nodename.equals("text")) {
								// System.out.println("   text="+tmpNode);
								if (nodeIterator.hasNext()) {
									Node tmp = nodeIterator.next();
									if (debugXFAstream) System.out.println("    text=" + tmp);
									this.formObject.setTextValue(tmp.getNodeValue());
								}

							}
							else
								if (nodename.equals("integer")) {
									// System.out.println("   integer="+tmpNode);
									if (nodeIterator.hasNext()) {
										Node tmp = nodeIterator.next();
										if (debugXFAstream) System.out.println("    integer=" + Integer.parseInt(tmp.getNodeValue()));
										this.formObject.setIntegerValue(tmp.getNodeValue());
										// @xfa integer
									}

								}
								else
									if (nodename.equals("para")) {
										if (debugXFAstream) System.out.println("   para=" + tmpNode);

										NamedNodeMap var = tmpNode.getAttributes();
										Node tmp;
										if ((tmp = var.getNamedItem("hAlign")) != null) {
											if (debugXFAstream) System.out.println("horiz=" + tmp.getNodeValue());
											this.formObject.setHorizontalAlign(tmp.getNodeValue());
										}
										if ((tmp = var.getNamedItem("vAlign")) != null) {
											if (debugXFAstream) System.out.println("vertic=" + tmp.getNodeValue());
											this.formObject.setVerticalAllign(tmp.getNodeValue());
										}

									}
									else
										if (nodename.equals("font")) {
											Node tmp = tmpNode.getAttributes().getNamedItem("typeface");
											if (tmp != null) {
												if (debugXFAstream) System.out.println("  font=" + tmp.getNodeValue());
												// @xfa font
												// Font font = new Font(tmp.getNodeValue(),style,size);
												// formObject.setTextFont();
											}

										}
										else
											if (nodename.equals("edge")) {
												Node tmp = tmpNode.getAttributes().getNamedItem("stroke");
												if (tmp != null) {
													if (debugXFAstream) System.out.println("  edge node=" + tmp.getNodeValue());
													this.formObject.setBorderStroke(tmp.getNodeValue());
												}

											}
											else
												if (nodename.equals("border")) {
													if (debugXFAstream) System.out.println("   border=" + tmpNode);

												}
												else
													if (nodename.equals("fill")) {
														if (debugXFAstream) System.out.println("   fill=" + tmpNode);

													}
													else
														if (nodename.equals("margin")) {
															if (debugXFAstream) System.out.println("   margin=" + tmpNode);

														}
														else
															if (nodename.equals("event")) {
																if (debugXFAstream) System.out.println("   event=" + tmpNode);

															}
															else
																if (nodename.equals("items")) {
																	// System.out.println("   items="+tmpNode);
																	/**
																	 * NodeList items = tmpNode.getChildNodes(); Node tmpItem; String[] listOfItems =
																	 * new String[items.getLength()]; for (int i = 0; i < items.getLength(); i++) {
																	 * tmpItem = items.item(i); if (tmpItem.getNodeName().equals("integer")) { if
																	 * (debugXFAstream) System.out.println("integer item" + i + '=' +
																	 * tmpItem.getChildNodes().item(0)); listOfItems[i] =
																	 * tmpItem.getChildNodes().item(0).getNodeValue(); } }
																	 * formObject.setlistOfItems(listOfItems, true); /
																	 **/
																}
																else
																	if (nodename.equals("exclGroup")) {
																		if (debugXFAstream) System.out.println("   exclGroup=" + tmpNode);

																	}
																	else
																		if (nodename.equals("proto")) {
																			if (debugXFAstream) System.out.println("   proto=" + tmpNode);

																		}
																		else {
																			LogWriter.writeFormLog("node name not implemented in checkbutton name="
																					+ nodename, debugUnimplemented);
																		}
		}
	}

	public Map getRefToFormArray() {
		return null;
	}

	// ########## chris END old code

	private Node getPageAreaNodeById(String strID) {
		for (Node n : this.pageAreaList) {
			Node attr = n.getAttributes().getNamedItem("id");
			if (attr != null && attr.getNodeValue().equals(strID)) {
				return n;
			}
		}
		return null;
	}

	private Node getPageAreaNodeByName(String strName) {
		for (Node n : this.pageAreaList) {
			Node attr = n.getAttributes().getNamedItem("name");
			if (attr != null && attr.getNodeValue().equals(strName)) {
				return n;
			}
		}
		return null;
	}

	/**
	 * method recursively look until which finds the pageArea element and store the page number and rootNode into pageArea HashMap
	 * 
	 * @param node
	 * @param depth recursion depth
	 * */
	private void allocatePagesWithContents(Node node, int depth) {

		// for(int i=0;i<depth;i++)
		// //System.out.print(" ");

		NodeList nodeList = node.getChildNodes();

		for (int i = 0; i < nodeList.getLength(); i++) {
			Node currentNode = nodeList.item(i);
			int nodeType = getNodeType(currentNode.getNodeName());

			if (nodeType == PAGESET) {
				NodeList pageSetChildList = currentNode.getChildNodes();
				for (int z = 0; z < pageSetChildList.getLength(); z++) {
					if (getNodeType(pageSetChildList.item(z).getNodeName()) == PAGEAREA) {
						this.pageAreaList.add(pageSetChildList.item(z));
					}
				}

				NodeList childList = currentNode.getParentNode().getChildNodes();
				ArrayList<Node> pageSetSiblings = new ArrayList<Node>();
				for (int z = 0; z < childList.getLength(); z++) {
					if (getNodeType(childList.item(z).getNodeName()) == SUBFORM) {
						pageSetSiblings.add(childList.item(z));
					}
				}

				if (this.pageAreaList.size() == 1 && pageSetSiblings.size() == 1) {
					XFAPageContent pCont = new XFAPageContent();
					pCont.setPageAreaNode(this.pageAreaList.get(0));
					pCont.getNodeList().add(pageSetSiblings.get(0));
					this.pageMap.put(1, pCont);
				}
				else {
					int pageCount = 1;
					Node currentPageArea = this.pageAreaList.get(0);

					bigLoop: // loop is useful if breakAfter attribute is found in subform
					for (int z = 0; z < pageSetSiblings.size(); z++) {
						Node sibNode = pageSetSiblings.get(z);
						NodeList sibChildList = sibNode.getChildNodes();

						for (int q = 0; q < sibChildList.getLength(); q++) {
							Node formChild = sibChildList.item(q);
							int type = getNodeType(formChild.getNodeName());
							if (type == BREAK) {
								Node beforeTargetAttr = formChild.getAttributes().getNamedItem("beforeTarget");
								Node startNewAttr = formChild.getAttributes().getNamedItem("startNew");

								if (startNewAttr != null) {
									if (this.pageAreaList.size() > 1 && pageCount < this.pageAreaList.size()) {
										pageCount++;
									}
									if (beforeTargetAttr != null) {
										currentPageArea = getPageAreaNodeById(beforeTargetAttr.getNodeValue());
									}
									else {
										currentPageArea = this.pageAreaList.get(pageCount - 1);
									}
								}
								else {
									if (this.pageMap.get(pageCount) != null) {
										pageCount++;
									}
								}
								break;
							}
							else
								if (type == BREAKBEFORE) {
									Node targetAttr = formChild.getAttributes().getNamedItem("target");
									Node startNewAttr = formChild.getAttributes().getNamedItem("startNew");

									if (startNewAttr != null) {// ....startNew="1"/>
										if (this.pageAreaList.size() > 1 && pageCount < this.pageAreaList.size()) {
											pageCount++;
										}
										if (targetAttr != null) {
											currentPageArea = getPageAreaNodeById(targetAttr.getNodeValue());
										}
										else {
											currentPageArea = this.pageAreaList.get(pageCount - 1);
										}
									}
									else {
										// if page area is accessed by second time without startNew attr.
										if (this.pageMap.get(pageCount) != null) {
											pageCount++;
										}
									}
									break;
								}
								else
									if (type == BREAKAFTER) {
										if (formChild.getAttributes().getNamedItem("startNew") != null) {
											if (this.pageMap.get(pageCount) == null) {
												XFAPageContent pc = new XFAPageContent();
												pc.setPageAreaNode(currentPageArea);
												pc.getNodeList().add(sibNode);
												this.pageMap.put(pageCount, pc);
											}
											else {
												this.pageMap.get(pageCount).getNodeList().add(sibNode);
											}
											pageCount++;
										}
										continue bigLoop;
									}
						}

						if (this.pageMap.get(pageCount) == null) {
							XFAPageContent pc = new XFAPageContent();
							pc.setPageAreaNode(currentPageArea);
							pc.getNodeList().add(sibNode);
							this.pageMap.put(pageCount, pc);
						}
						else {
							this.pageMap.get(pageCount).getNodeList().add(sibNode);
						}
					}
				}
				break;
			}
			else {
				allocatePagesWithContents(currentNode, depth++);
			}
		}
	}

	/** for the moment simple code compare */
	private int getNodeType(String nodeName) {

		int nodeType = UNKNOWN;
		String name = nodeName.toLowerCase();

		if (name.equals("pagearea")) {
			nodeType = PAGEAREA;
		}
		else
			if (name.equals("subform")) {
				nodeType = SUBFORM;
			}
			else
				if (name.equals("pageset")) {
					nodeType = PAGESET;
				}
				else
					if (name.equals("break")) {
						nodeType = BREAK;
					}
					else
						if (name.equals("breakbefore")) {
							nodeType = BREAKBEFORE;
						}
						else
							if (name.equals("breakafter")) {
								nodeType = BREAKAFTER;
							}

		return nodeType;
	}
}
