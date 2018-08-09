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
 * PdfSwingPopup.java
 * ---------------
 */
package org.jpedal.objects.acroforms.overridingImplementations;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

import javax.swing.BorderFactory;
import javax.swing.JInternalFrame;
import javax.swing.JTextArea;

import org.jpedal.objects.raw.FormObject;
import org.jpedal.objects.raw.PdfDictionary;
import org.jpedal.objects.raw.PdfObject;

/**
 * provide PDF poup for Annotations
 */
public class PdfSwingPopup extends JInternalFrame {

	private static final long serialVersionUID = 796302916236391896L;

	public PdfSwingPopup(FormObject formObj, PdfObject popupObj, int cropBoxWidth) {
		super();

		if (formObj.getParameterConstant(PdfDictionary.Subtype) == PdfDictionary.Text) {
			String name = formObj.getTextStreamValue(PdfDictionary.Name);
			if (name != null) {
				// a Comment Annotation where we setup a standard popup
				if (name.equals("Comment")) {

					// change Rect - Comment Annotation bounds are the formObj.y and width of page for x,
					// then we make sure its visible in bottom right of display
					float[] rect = popupObj.getFloatArray(PdfDictionary.Rect);

					// rect[0] = cropBoxWidth;
					// rect[2] = rect[0]+250;//worked out sizes from comparing to adobe.
					// rect[3] = rect[1];
					// rect[1] = rect[1]-170;

					popupObj.setFloatArray(PdfDictionary.Rect, rect);

					// change Open - we invert after setup so set to false to show in end.
					popupObj.setBoolean(PdfDictionary.Open, false);

					// if no color defined so use default yellow for Comment (255,255,0)
					if (formObj.getFloatArray(PdfDictionary.C) == null) formObj.setFloatArray(PdfDictionary.C, new float[] { 255, 255, 0 });
				}
			}
			else
				if (popupObj != null) {
					float[] rect = popupObj.getFloatArray(PdfDictionary.Rect);
					if (rect != null) {
						rect[0] = rect[0];
						rect[2] = rect[2] + 160;// worked out sizes from comparing to adobe.
						rect[3] = rect[3] + 80;
						rect[1] = rect[1];
						popupObj.setFloatArray(PdfDictionary.Rect, rect);
					}
				}
		}

		// read in date for title bar
		String mStream = formObj.getTextStreamValue(PdfDictionary.M);
		StringBuffer date = null;
		if (mStream != null) {
			date = new StringBuffer(mStream);
			date.delete(0, 2);// delete D:
			date.insert(10, ':');
			date.insert(13, ':');
			date.insert(16, ' ');

			String year = date.substring(0, 4);
			String day = date.substring(6, 8);
			date.delete(6, 8);
			date.delete(0, 4);
			date.insert(0, day);
			date.insert(4, year);
			date.insert(2, '/');
			date.insert(5, '/');
			date.insert(10, ' ');

			// date.delete(19, date.length());//delete the +01'00' Time zone definition
		}

		// setup title text for popup
		String subject = formObj.getTextStreamValue(PdfDictionary.Subj);
		String popupTitle = formObj.getTextStreamValue(PdfDictionary.T);
		if (popupTitle == null) popupTitle = "";

		String title = "";
		if (subject != null) title += subject + '\t';
		if (date != null) title += date;
		title += '\n' + popupTitle;

		// main body text on contents is always a text readable form of the form or the content of the popup window.
		String contentString = formObj.getTextStreamValue(PdfDictionary.Contents);
		if (contentString == null) contentString = "";
		if (contentString.indexOf('\r') != -1) contentString = contentString.replaceAll("\r", "\n");

		// setup background color
		float[] col = formObj.getFloatArray(PdfDictionary.C);
		Color bgColor = null;
		if (col != null) {
			if (col[0] > 1 || col[1] > 1 || col[2] > 1) bgColor = new Color((int) col[0], (int) col[1], (int) col[2]);
			else bgColor = new Color(col[0], col[1], col[2]);

			// and set border to that if valid
			setBorder(BorderFactory.createLineBorder(bgColor));
		}

		// remove title bar from internalframe so its looks as we want
		((javax.swing.plaf.basic.BasicInternalFrameUI) this.getUI()).setNorthPane(null);

		setLayout(new BorderLayout());

		// add title bar
		JTextArea titleBar = new JTextArea(title);
		titleBar.setEditable(false);
		if (bgColor != null) titleBar.setBackground(bgColor);
		add(titleBar, BorderLayout.NORTH);

		// add content area
		JTextArea contentArea = new JTextArea(contentString);
		contentArea.setWrapStyleWord(true);
		contentArea.setLineWrap(true);
		add(contentArea, BorderLayout.CENTER);

		// set the font sizes so that they look more like adobes popups
		Font titFont = titleBar.getFont();
		titleBar.setFont(new Font(titFont.getName(), titFont.getStyle(), titFont.getSize() - 1));
		Font curFont = contentArea.getFont();
		contentArea.setFont(new Font(curFont.getName(), curFont.getStyle(), curFont.getSize() - 2));

		// add our drag listener so it acts like an internal frame
		MyMouseMotionAdapter mmma = new MyMouseMotionAdapter();
		titleBar.addMouseMotionListener(mmma);

		// add focus listener to bring selected popup to front
		addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent e) {
				toFront();
				super.focusGained(e);
			}
		});
	}

	private class MyMouseMotionAdapter extends MouseMotionAdapter {
		@Override
		public void mouseDragged(MouseEvent e) {
			// move the popup as the user drags the mouse
			Point pt = e.getPoint();
			Point curLoc = getLocation();
			curLoc.translate(pt.x, pt.y);
			setLocation(curLoc);

			super.mouseDragged(e);
		}
	}

	/*
	 * unsorted1\1414f3_file.pdf baseline_screens\customers1\2007-09-17_PLUMBING_SHOP_DWGS-_PART_1.pdf baseline_screens\extra\todd\B05016_12_TEST.pdf
	 * baseline_screens\extra\todd\B07037_55_TEST.pdf baseline_screens\extra\todd\B11115_99_Test.pdf baseline_screens\extra\todd\B15002_71_TEST.pdf
	 * baseline_screens\extra\todd\B15040_99_TEST.pdf baseline_screens\extra\todd\B16006_02_TEST.pdf baseline_screens\extra\todd\B16052_01_TEST.pdf
	 * baseline_screens\extra\todd\B16099_07_TEST.pdf baseline_screens\extra\todd\B17079_01_TEST.pdf baseline_screens\extra\todd\B20009_03_TEST.pdf
	 * baseline_screens\extra\todd\B25025_01_TEST.pdf baseline_screens\extra\todd\B35023_02_test.pdf baseline_screens\customers1\Binder1.pdf
	 * baseline_screens\debug1\DET.0000000159.03172005-S1.pdf baseline_screens\debug1\DrWang.pdf baseline_screens\customers1\example_cern.pdf
	 * pdftestfiles\rogsFiles\FelderTest.pdf baseline_screens\adobe\form2-1.pdf baseline_screens\abacus\Kreditorenbelege.pdf
	 * baseline_screens\abacus\Kreditorenbelege2.pdf baseline_screens\forms\multiplerevisions.pdf baseline_screens\abacus\multiplerevisions2.pdf
	 * C:\Documents and Settings\chris\My Documents\idrsolutions\tasks\Plumbing_Fixtures.pdf baseline_screens\docusign\Problem4.pdf
	 * baseline_screens\extra\bayer\Reportt0.pdf baseline_screens\acroforms\smart-mortgageapp.pdf
	 * baseline_screens\acroforms\smart-mortgageapp_signed.pdf baseline_screens\acroforms\smart-mortgageapp_unsigned.pdf
	 * baseline_screens\debug2\StampsProblems.pdf extras\annotsforChris\technical-contract.pdf baseline_screens\customers1\test_filefails_jpedal.pdf
	 * baseline_screens\customers2\ULTRA+PETROLEUM+2006_Annual_Report.pdf
	 */
}
