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
 * GUIData.java
 * ---------------
 */
package org.jpedal.objects.acroforms.formData;

import java.awt.Color;
import java.util.List;
import java.util.Map;

import org.jpedal.PdfDecoder;
import org.jpedal.external.CustomFormPrint;
import org.jpedal.io.PdfObjectReader;
import org.jpedal.objects.Javascript;
import org.jpedal.objects.PdfPageData;
import org.jpedal.objects.acroforms.creation.FormFactory;
import org.jpedal.objects.layers.PdfLayerList;
import org.jpedal.objects.raw.FormObject;

/**
 * Abstraction so forms can be rendered in ULC - see SwingData for full details of usage
 */
public interface GUIData {

	void resetDuplicates();

	void removeAllComponentsFromScreen();

	void setPageData(PdfPageData pageData, int insetW, int insetH);

	void completeField(FormObject formObject, int formNum, Integer widgetType, Object retComponent, PdfObjectReader currentPdfFile);

	void displayComponents(int startPage, int endPage);

	int getNextFreeField();

	void reportError(int code, Object[] args);

	/** resets the storage values for the unformatted and invalid values */
	void reset(String[] aFields);

	List getComponentNameList(int pageNumber);

	/** returns the displayed component which is being displayed e.g. jtextfield */
	Object[] getComponentsByName(String objectName);

	int getStartComponentCountForPage(int page);

	void initParametersForPage(PdfPageData pageData, int page, PdfDecoder decoder);

	void setLayerData(PdfLayerList layers);

	/**
	 * return true if was successful, false if we want to keepValues and the new formcount is lower than the current count
	 */
	boolean resetComponents(int formCount, int pageCount, boolean keepValues);

	void setJavascript(Javascript javascript);

	/**
	 * valid flag used by Javascript to allow rollback &nbsp call ttf to reset all values held
	 * 
	 * @param value
	 *            - the value to change it to
	 * @param isValid
	 *            - is a valid value or not
	 * @param isFormatted
	 *            - is formatted properly, then we will store the previoud value as the last unformatted value
	 */
	void setValue(String ref, Object value, boolean isValid, boolean isFormatted);

	/** set the last unformatted value directly */
	void setUnformattedValue(String ref, Object value);

	/** set the last valid value for the form directly */
	void setLastValidValue(String ref, Object value);

	/** returns the last valid value for the pdf ref given */
	Object getLastValidValue(String ref);

	/** returns the last unformatted value for the pdf ref given */
	Object getLastUnformattedValue(String fieldName);

	/**
	 * returns the value for the given field, from either the field name or the field Pdf Ref
	 */
	Object getValue(Object fieldRef);

	/** returns the widget object */
	Object getWidget(Object fieldName);

	void loseFocus();

	void renderFormsOntoG2(Object g2, int page, float scaling, int indent, int curRotation, Map componentsToIgnore, FormFactory formFactory,
			PdfObjectReader currentPdfFile, int pageHeight);

	void resetScaledLocation(float scaling, int displayRotation, int indent);

	void setRootDisplayComponent(Object pdfDecoder);

	void setPageValues(float scaling, int rotation, int indent, int userX, int userY, int displayView, int widestPageNR, int widestPageR);

	void setPageDisplacements(int[] reached, int[] reached2);

	/** returns the type of form we have defined by this name, ie annotation, signature, pushbutton etc */
	Integer getTypeValueByName(String name);

	void storeRawData(FormObject formObject);

	public FormObject getFormObject(int i);

	void flushFormData();

	/** returns all the formObjects by the specified name */
	Object[] getRawForm(String objectName);

	/** returns all the formObjects by the specified name, check case insentively is <b>false</b> */
	Object[] getRawForm(String objectName, boolean caseSensitive);

	/** returns the PDF referance to the name passed in ie 128 0 R */
	String getnameToRef(String keyToCheck);

	int getIndexFromName(String name);

	/** returns the raw FormObject data */
	Map getRawFormData();

	void setOffset(int offset);

	void invalidate(String name);

	/** converts the field index to its unique Pdf Reference */
	String convertIDtoRef(int objectID);

	/** stores the displayed fields value, into the FormObject */
	void storeDisplayValue(String fieldRef);

	String[] getChildNames(String name);

	/** updates the visible value of the changed form, used via javascript actions */
	void setCompVisible(String ref, boolean visible);

	/**
	 * allow user to lookup page with name of Form.
	 * 
	 * @param formName
	 *            or ref (10 0 R)
	 * @return page number or -1 if no page found
	 */
	public int getPageForFormObject(String formName);

	void popup(FormObject formObj, PdfObjectReader currentPdfFile);

	/** used internally to correct printing display */
	void setUnsortedListForPage(int page, List unsortedForms);

	/** sets the text color for the specified form */
	public void setTextColor(String objectRefAsString, Color textColor);

	void setCustomPrintInterface(CustomFormPrint customFormPrint);

	/**
	 * you can now send in the formobject and this will return the super form type ie ComponentData.TEXT_TYPE, ComponentData.LIST_TYPE (list, combo)
	 * or ComponentData.BUTTON_TYPE (sign,annot,radio,check,push)
	 */
	public int getFieldType(Object swingComp);

	/**
	 * send in either FormObject for precise types, or swing components. <br />
	 * if returnSuper is true then we return the super form type ie. ComponentData.TEXT_TYPE, ComponentData.LIST_TYPE (list, combo) or
	 * ComponentData.BUTTON_TYPE (sign,annot,radio,check,push) <br />
	 * if returnSuper is false then we return the type for the specified form ie. formFactory.list,FormFactory.pushbutton etc
	 */
	// removed 20121104
	// public int getFieldType(Object swingComp,boolean returnSuper);

	// store last used value so we can align if kids
	public void flagLastUsedValue(Object component, FormObject formObject, boolean sync);

	public void syncAllValues();

	public void resetAfterPrinting();

	public void hideComp(String compName, boolean visible);

	public Object generateBorderfromForm(FormObject form, float scaling);

	public String getFieldNameFromRef(String ref);

	public void storeXFARefToForm(Map xfaRefToFormObject);

	public void setForceRedraw(boolean b);

	public boolean formsRasterizedForDisplay();

	public void setRasterizeForms(boolean inlineForms);
}
