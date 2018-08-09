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
 * ViewerBeanBeanInfo.java
 * ---------------
 */

package org.jpedal.examples.viewer.javabean;

import java.beans.BeanDescriptor;
import java.beans.EventSetDescriptor;
import java.beans.IntrospectionException;
import java.beans.MethodDescriptor;
import java.beans.PropertyDescriptor;
import java.beans.SimpleBeanInfo;

public class ViewerBeanBeanInfo extends SimpleBeanInfo {

	// Property identifiers
	private static final int PROPERTY_document = 0;
	private static final int PROPERTY_pageNumber = 1;
	private static final int PROPERTY_rotation = 2;
	private static final int PROPERTY_zoom = 3;
	// private static final int PROPERTY_pageLayout = 4;

	private static final int PROPERTY_menuBar = 4;
	private static final int PROPERTY_toolBar = 5;
	private static final int PROPERTY_displayOptionsBar = 6;
	private static final int PROPERTY_sideTabBar = 7;
	private static final int PROPERTY_navigationBar = 8;

	private static final int defaultPropertyIndex = -1;
	private static final int defaultEventIndex = -1;

	/**
	 * Gets the bean's <code>BeanDescriptor</code>s.
	 * 
	 * @return BeanDescriptor describing the editable properties of this bean. May return null if the information should be obtained by automatic
	 *         analysis.
	 */
	@Override
	public BeanDescriptor getBeanDescriptor() {
		BeanDescriptor beanDescriptor = new BeanDescriptor(ViewerBean.class, null); // NOI18N

		// Here you can add code for customizing the BeanDescriptor.

		return beanDescriptor;
	}

	/**
	 * Gets the bean's <code>PropertyDescriptor</code>s.
	 * 
	 * @return An array of PropertyDescriptors describing the editable properties supported by this bean. May return null if the information should be
	 *         obtained by automatic analysis.
	 *         <p>
	 *         If a property is indexed, then its entry in the result array will belong to the IndexedPropertyDescriptor subclass of
	 *         PropertyDescriptor. A client of getPropertyDescriptors can use "instanceof" to check if a given PropertyDescriptor is an
	 *         IndexedPropertyDescriptor.
	 */
	@Override
	public PropertyDescriptor[] getPropertyDescriptors() {
		PropertyDescriptor[] properties = new PropertyDescriptor[9];

		try {
			properties[PROPERTY_document] = new PropertyDescriptor("document", ViewerBean.class, null, "setDocument"); // NOI18N
			properties[PROPERTY_document].setDisplayName("Document");
			properties[PROPERTY_document].setShortDescription("Set the default open document");
			properties[PROPERTY_document].setPreferred(true);

			properties[PROPERTY_pageNumber] = new PropertyDescriptor("pageNumber", ViewerBean.class, "getPageNumber", "setPageNumber"); // NOI18N
			properties[PROPERTY_pageNumber].setDisplayName("Page Number");
			properties[PROPERTY_pageNumber].setShortDescription("Set the page number to open on");
			properties[PROPERTY_pageNumber].setPreferred(true);

			properties[PROPERTY_rotation] = new PropertyDescriptor("rotation", ViewerBean.class, "getRotation", "setRotation"); // NOI18N
			properties[PROPERTY_rotation].setPropertyEditorClass(RotationEditor.class);
			properties[PROPERTY_rotation].setDisplayName("Rotation");
			properties[PROPERTY_rotation].setShortDescription("Set the default rotation");
			properties[PROPERTY_rotation].setPreferred(true);

			properties[PROPERTY_zoom] = new PropertyDescriptor("zoom", ViewerBean.class, "getZoom", "setZoom"); // NOI18N
			properties[PROPERTY_zoom].setDisplayName("Zoom");
			properties[PROPERTY_zoom].setShortDescription("Set the default scaling factor");
			properties[PROPERTY_zoom].setPreferred(true);

			properties[PROPERTY_menuBar] = new PropertyDescriptor("menuBar", ViewerBean.class, "getMenuBar", "setMenuBar"); // NOI18N
			properties[PROPERTY_menuBar].setDisplayName("Show Menu Bar");
			properties[PROPERTY_menuBar].setShortDescription("Show the Menu Bar");
			properties[PROPERTY_menuBar].setPreferred(false);
			properties[PROPERTY_menuBar].setConstrained(true);

			properties[PROPERTY_toolBar] = new PropertyDescriptor("toolBar", ViewerBean.class, "getToolBar", "setToolBar"); // NOI18N
			properties[PROPERTY_toolBar].setDisplayName("Show Tool Bar");
			properties[PROPERTY_toolBar].setShortDescription("Show the Tool Bar");
			properties[PROPERTY_toolBar].setPreferred(false);
			properties[PROPERTY_toolBar].setConstrained(true);

			properties[PROPERTY_displayOptionsBar] = new PropertyDescriptor("displayOptionsBar", ViewerBean.class, "getDisplayOptionsBar",
					"setDisplayOptionsBar"); // NOI18N
			properties[PROPERTY_displayOptionsBar].setDisplayName("Show Display Options Bar");
			properties[PROPERTY_displayOptionsBar].setShortDescription("Show the Display Options Bar");
			properties[PROPERTY_displayOptionsBar].setPreferred(false);
			properties[PROPERTY_displayOptionsBar].setConstrained(true);

			properties[PROPERTY_sideTabBar] = new PropertyDescriptor("sideTabBar", ViewerBean.class, "getSideTabBar", "setSideTabBar"); // NOI18N
			properties[PROPERTY_sideTabBar].setDisplayName("Display Side Tab Bar");
			properties[PROPERTY_sideTabBar].setShortDescription("Display the Side Tab Bar");
			properties[PROPERTY_sideTabBar].setPreferred(false);
			properties[PROPERTY_sideTabBar].setConstrained(true);

			properties[PROPERTY_navigationBar] = new PropertyDescriptor("navigationBar", ViewerBean.class, "getNavigationBar", "setNavigationBar"); // NOI18N
			properties[PROPERTY_navigationBar].setDisplayName("Display Navigation Bar");
			properties[PROPERTY_navigationBar].setShortDescription("Display the Navigation Bar");
			properties[PROPERTY_navigationBar].setPreferred(false);
			properties[PROPERTY_navigationBar].setConstrained(true);

			// properties[PROPERTY_pageLayout] = new PropertyDescriptor ( "pageLayout", ViewerPanel.class, "getPageLayout", "setPageLayout" ); //
			// NOI18N
			// properties[PROPERTY_pageLayout].setPropertyEditorClass(LayoutEditor.class);
		}
		catch (IntrospectionException e) {
			e.printStackTrace();
		}

		// Here you can add code for customizing the properties array.

		return properties;
	}

	/**
	 * Gets the bean's <code>EventSetDescriptor</code>s.
	 * 
	 * @return An array of EventSetDescriptors describing the kinds of events fired by this bean. May return null if the information should be
	 *         obtained by automatic analysis.
	 */
	@Override
	public EventSetDescriptor[] getEventSetDescriptors() {
		EventSetDescriptor[] eventSets = new EventSetDescriptor[0];

		// Here you can add code for customizing the event sets array.

		return eventSets;
	}

	/**
	 * Gets the bean's <code>MethodDescriptor</code>s.
	 * 
	 * @return An array of MethodDescriptors describing the methods implemented by this bean. May return null if the information should be obtained by
	 *         automatic analysis.
	 */
	@Override
	public MethodDescriptor[] getMethodDescriptors() {
		MethodDescriptor[] methods = new MethodDescriptor[0];

		// Here you can add code for customizing the methods array.

		return methods;
	}

	/**
	 * A bean may have a "default" property that is the property that will mostly commonly be initially chosen for update by human's who are
	 * customizing the bean.
	 * 
	 * @return Index of default property in the PropertyDescriptor array returned by getPropertyDescriptors.
	 *         <P>
	 *         Returns -1 if there is no default property.
	 */
	@Override
	public int getDefaultPropertyIndex() {
		return defaultPropertyIndex;
	}

	/**
	 * A bean may have a "default" event that is the event that will mostly commonly be used by human's when using the bean.
	 * 
	 * @return Index of default event in the EventSetDescriptor array returned by getEventSetDescriptors.
	 *         <P>
	 *         Returns -1 if there is no default event.
	 */
	@Override
	public int getDefaultEventIndex() {
		return defaultEventIndex;
	}

	@Override
	public java.awt.Image getIcon(int iconKind) {
		return loadImage("/org/jpedal/examples/viewer/res/pdf.png");
	}
}
