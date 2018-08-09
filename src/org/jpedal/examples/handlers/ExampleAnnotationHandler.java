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
 * ExampleAnnotationHandler.java
 * ---------------
 */
package org.jpedal.examples.handlers;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Iterator;
import java.util.Map;

import javax.swing.JFileChooser;

import org.jpedal.PdfDecoder;
import org.jpedal.examples.viewer.Values;
import org.jpedal.examples.viewer.gui.SwingGUI;
import org.jpedal.external.AnnotationHandler;
import org.jpedal.io.ObjectStore;
import org.jpedal.io.PdfObjectReader;
import org.jpedal.objects.raw.FormObject;
import org.jpedal.objects.raw.PdfArrayIterator;
import org.jpedal.objects.raw.PdfDictionary;
import org.jpedal.objects.raw.PdfObject;
import org.jpedal.render.DynamicVectorRenderer;
import org.jpedal.render.ImageObject;

public class ExampleAnnotationHandler implements AnnotationHandler {

	/**
	 * example code to adapt
	 * 
	 * @param decode_pdf
	 * @param objs
	 * @param p
	 */
	@Override
	public void handleAnnotations(PdfDecoder decode_pdf, Map objs, int p) {

		// new list we can parse
		PdfArrayIterator annotListForPage = decode_pdf.getFormRenderer().getAnnotsOnPage(p);

		if (annotListForPage != null && annotListForPage.getTokenCount() > 0) { // can have empty lists

			/**
			 * new code to set Annot to have custom image
			 * 
			 * scan and overwrite images of certain types
			 */
			int i = 0;
			int count = annotListForPage.getTokenCount();
			int[] type = new int[count];
			Color[] colors = new Color[count];
			Object[] obj = new Object[count];

			while (annotListForPage.hasMoreTokens()) {

				// Due to the way some pdf's are created it is necessery to take the offset of a page into account when addding custom objects
				// Variables mX and mY represent that offset and need to be taken in to account when placing any additional object on a page.

				int mX = decode_pdf.getPdfPageData().getMediaBoxX(p);
				int mY = decode_pdf.getPdfPageData().getMediaBoxY(p);

				// get ID of annot which has already been decoded and get actual object
				String annotKey = annotListForPage.getNextValueAsString(true);

				Object[] rawObj = decode_pdf.getFormRenderer().getCompData().getRawForm(annotKey);

				for (Object aRawObj : rawObj) {
					if (aRawObj != null) {
						FormObject annotObj = (FormObject) aRawObj;

						// get the FS value
						// PdfObject FS=annotObj.getDictionary(PdfDictionary.FS);

						// and the text
						// if(FS!=null){
						// System.out.println("----"+annotObj.getObjectRefAsString());
						// System.out.println("Contents="+annotObj.getTextStreamValue(PdfDictionary.Contents));
						// System.out.println("F="+FS.getTextStreamValue(PdfDictionary.F));
						// System.out.println("D="+FS.getTextStreamValue(PdfDictionary.D));
						// }

						int subtype = annotObj.getParameterConstant(PdfDictionary.Subtype);

						// if(subtype==PdfDictionary.Link){
						// System.out.println("----"+annotObj.getObjectRefAsString());
						// PdfObject Aobj=annotObj.getDictionary(PdfDictionary.A);
						// System.out.println("A="+Aobj+" "+subtype);
						// PdfObject winObj=Aobj.getDictionary(PdfDictionary.Win);
						// System.out.println("Win="+winObj+" "+winObj.getTextStreamValue(PdfDictionary.D)+" "+winObj.getTextStreamValue(PdfDictionary.P));
						//
						// }
						// subtypes set in PdfDictionary - please use Constant as values may change
						// if not present ask me and I will add
						if (subtype == PdfDictionary.Link) { // might also be PdfDictionary.Link

							// @annot -save so we can lookup (kept as HashMap siply to reduce changes and in case
							// we want something else as value. Could be Set or Vector
							objs.put(annotObj, "x");

							Color col = Color.BLUE;

							// get origin of Form Object
							Rectangle location = annotObj.getBoundingRectangle();

							// SOME EXAMPLES to show other possible uses

							// example stroked shape
							// type[i]= org.jpedal.render.DynamicVectorRenderer.STROKEDSHAPE;
							// colors[i]=Color.RED;
							// obj[i]=new Rectangle(location.x,location.y,20,20); //ALSO sets location. Any shape can be used

							// example simple filled shape
							// type[i]= org.jpedal.render.DynamicVectorRenderer.FILLEDSHAPE;
							// colors[i]=Color.GREEN;
							// obj[i]=new Rectangle(location.x,location.y,20,20); //ALSO sets location. Any shape can be used

							// example text object
							// type[i]= org.jpedal.render.DynamicVectorRenderer.STRING;
							// org.jpedal.render.TextObject textObject=new org.jpedal.render.TextObject(); //composite object so we can pass in
							// parameters
							// textObject.x=location.x+mX;
							// textObject.y=location.y+mY;
							// textObject.text=""+(i+1);
							// textObject.font=new Font("Serif",Font.PLAIN,12);
							// colors[i]=col;
							// obj[i]=textObject; //ALSO sets location

							// example image object
							/**/
							type[i] = DynamicVectorRenderer.IMAGE;
							ImageObject imgObject = new ImageObject(); // composite object so we can pass in parameters
							imgObject.x = location.x + mX;
							imgObject.y = location.y + mY;
							imgObject.image = createUniqueImage(16, String.valueOf((i + 1)), col);
							obj[i] = imgObject; // ALSO sets location

							// example custom - TOTAL FLEXIBILTY as you implement you own custom object
							// type[i]=org.jpedal.render.DynamicVectorRenderer.CUSTOM;
							//
							// JPedalCustomDrawObject exampleObj=new ExampleCustomDrawObject();
							// exampleObj.setMedX(location.x+mX);
							// exampleObj.setMedY(location.x+mY);
							// obj[i]=exampleObj;

							i++;
						}
					}
				}
			}
			// pass into JPEDAL after page decoded - will be removed automatically on new page/open file
			// BUT PRINTING retains values until manually removed

			/**/

			// this code will remove ALL items already drawn on page
			// try{
			// decode_pdf.flushAdditionalObjectsOnPage(commonValues.getCurrentPage());
			// }catch(PdfException e){
			// e.printStackTrace();
			// //ShowGUIMessage.showGUIMessage( "", new JLabel(e.getMessage()),"Exception adding object to display");
			// }

		}
	}

	@Override
	public void checkLinks(Map objs, boolean mouseClicked, PdfObjectReader pdfObjectReader, int x, int y, SwingGUI currentGUI, Values commonValues) {

		// new code to check for match
		Iterator objKeys = objs.keySet().iterator();
		FormObject annotObj = null;
		while (objKeys.hasNext()) {
			annotObj = (FormObject) objKeys.next();
			if (annotObj.getBoundingRectangle().contains(x, y)) {
				break;
			}

			// reset to null so when exits no match
			annotObj = null;
		}

		/** action for moved over of clicked */
		if (annotObj != null) {

			/**
			 * get EF object containing file data
			 */
			// annotObj is now actual object (on lazy initialisation so EF has not been read).....

			System.out.println("clicked on=" + mouseClicked + " obj=" + annotObj + ' ' + annotObj.getObjectRefAsString() + ' '
					+ annotObj.getBoundingRectangle());

			// @annot - in my example, ignore if not clicked
			if (!mouseClicked) return;

			// FS obj contains an EF obj which contains an F obj with the data in
			// F can be various - we are only interested in it as a Dictionary with a stream
			PdfObject EFobj = null, FSobj = annotObj.getDictionary(PdfDictionary.FS);
			if (FSobj != null) EFobj = FSobj.getDictionary(PdfDictionary.EF);

			/**
			 * create the file chooser to select the file name
			 **/
			JFileChooser chooser = new JFileChooser(commonValues.getInputDir());
			chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			int state = chooser.showSaveDialog(currentGUI.getFrame());

			/**
			 * save file and take the hit
			 */
			if (state == 0) {
				File fileTarget = chooser.getSelectedFile();

				// here is where we take the hit (Only needed if on lazy init - ie Unread Dictionary like EF)....
				if (EFobj != null) pdfObjectReader.checkResolved(EFobj);

				// contains the actual file data
				PdfObject Fobj = EFobj.getDictionary(PdfDictionary.F);

				// see if cached or decoded (cached if LARGE)
				// IMPORTANT NOTE!!! - if the object is in a compressed stream (a 'blob' of
				// objects which we need to read in one go, it will not be cached
				String nameOnDisk = Fobj.getCachedStreamFile(pdfObjectReader.getObjectReader());

				// if you get null, make sure you have enabled caching and
				// file is bigger than cache
				System.out.println("file=" + nameOnDisk);

				if (nameOnDisk != null) { // just copy
					ObjectStore.copy(nameOnDisk, fileTarget.toString());
				}
				else { // save out
					byte[] fileData = Fobj.getDecodedStream();

					if (fileData != null) { // write out if in memory
						FileOutputStream fos;
						try {
							fos = new FileOutputStream(fileTarget);
							fos.write(fileData);
							fos.close();
						}
						catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
	}

	private static BufferedImage createUniqueImage(int size, String text, Color col) {
		// create a unique graphic
		BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = (Graphics2D) img.getGraphics();
		g2.setColor(col);
		g2.fill(new Rectangle(0, 0, size, size));
		g2.setColor(Color.BLACK);
		g2.draw(new Rectangle(0, 0, size - 1, size - 1));
		g2.setColor(Color.white);
		g2.drawString(text, 2, 12);

		return img;
	}
}
