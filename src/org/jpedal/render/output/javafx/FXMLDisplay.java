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
 * FXMLDisplay.java
 * ---------------
 */
package org.jpedal.render.output.javafx;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Pattern;

import org.jpedal.fonts.PdfFont;
import org.jpedal.fonts.glyph.PdfGlyph;
import org.jpedal.io.ObjectStore;
import org.jpedal.objects.GraphicsState;
import org.jpedal.render.DynamicVectorRenderer;
import org.jpedal.render.ShapeFactory;
import org.jpedal.render.output.FontMapper;
import org.jpedal.render.output.GenericFontMapper;
import org.jpedal.render.output.OutputDisplay;
import org.jpedal.utils.LogWriter;

public class FXMLDisplay extends OutputDisplay {

	// final private static String separator = System.getProperty("file.separator");

	String packageName = "";

	// used to track font changes
	private int lastFontTextLength, fontTextLength, lastFontSizeAsString;

	private String textWithSpaces;

	/** used to give each shape a unique ID */
	private int shapeCount = 0;
	private String divName;

	// Root directory of the image
	// private String imagePrefix=null;

	private String javaFxFileName = "";

	public FXMLDisplay(int pageNumber, Point2D midPoint, Rectangle cropBox, boolean addBackground, int defaultSize, ObjectStore newObjectRef) {
		super(pageNumber, midPoint, cropBox, addBackground, defaultSize, newObjectRef, null);

		this.type = DynamicVectorRenderer.CREATE_JAVAFX;

		// setup helper class for static helper code
		Helper = new org.jpedal.render.output.javafx.JavaFXHelper();

		this.firstPageName = System.getProperty("org.jpedal.pdf2javafx.firstPageName");

		Pattern p = Pattern.compile("[^a-zA-Z]");
		if (this.firstPageName != null) {
			if (this.firstPageName.length() == 0 || p.matcher(this.firstPageName.substring(0, 1)).find()) {
				throw new RuntimeException("org.jpedal.pdf2javafx.firstPageName must begin with a character A-Z or a-z.");
			}
		}
	}

	/**
	 * Add current date
	 * 
	 * @return returns date as string in 21 - November - 2011 format
	 */

	public static String getDate() {

		DateFormat dateFormat = new SimpleDateFormat("dd - MMMMM - yyyy"); // get current date

		Date date = new Date();
		// System.out.println(dateFormat.format(date));
		return dateFormat.format(date);
	}

	/**
	 * add footer and other material to complete
	 */
	@Override
	protected void completeOutput() {

		// flush any cached text before we write out
		flushText();

		if (DEBUG_DRAW_PAGE_BORDER) drawPageBorder();

		// No nav bar on single page files.
		boolean onePageFile = false;
		if (this.endPage == 1 || (this.endPage - this.startPage) == 0) onePageFile = true;

		/**
		 * write out Main fxml content
		 */
		writeCustom(TOFILE, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		writeCustom(TOFILE, "");
		writeCustom(TOFILE, "<?import java.lang.*?>");
		writeCustom(TOFILE, "<?import javafx.scene.*?>");
		writeCustom(TOFILE, "<?import javafx.scene.control.*?>");
		writeCustom(TOFILE, "<?import javafx.scene.layout.*?>");
		writeCustom(TOFILE, "<?import javafx.scene.image.*?>");
		writeCustom(TOFILE, "<?import javafx.scene.shape.*?>");
		writeCustom(TOFILE, "<?import javafx.scene.text.*?>");
		writeCustom(TOFILE, "");
		writeCustom(
				TOFILE,
				"<BorderPane prefHeight=\"" + (this.pageData.getCropBoxHeight(this.pageNumber) + 50) + "\" prefWidth=\""
						+ this.pageData.getCropBoxWidth(this.pageNumber) + "\" xmlns:fx=\"http://javafx.com/fxml\" fx:controller=\""
						+ this.packageName + "."
						+ (this.firstPageName != null && this.pageNumber == 1 ? this.firstPageName : "page" + this.pageNumberAsString) + "\">");
		writeCustom(TOFILE, "\t");
		writeCustom(TOFILE, "\t<center>");
		writeCustom(TOFILE, "\t");
		writeCustom(TOFILE, "\t\t<Pane fx:id=\"PDFContent\">");
		writeCustom(TOFILE, "");
		writeCustom(TOFILE, "");

		// script
		try {
			writeCustom(TOFILE, this.script.toString());
		}
		catch (Exception e) {
			// tell user and log
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
		}

		// End of PDF content
		writeCustom(TOFILE, "\t\t</Pane>");
		writeCustom(TOFILE, "\t");
		writeCustom(TOFILE, "\t</center>");

		if (!onePageFile) {// navBar related

			// NavBar Content
			writeCustom(TOFILE, "\n\t<bottom>");
			writeCustom(TOFILE,
					"\t\t<HBox id=\"navBar\" alignment=\"center\" spacing=\"5\" style=\"-fx-background-color: #F0F8FF\" prefHeight=\"50\" prefWidth=\""
							+ this.pageData.getCropBoxWidth(this.pageNumber) + "\" layoutY=\""
							+ (this.pageData.getCropBoxHeight(this.pageNumber) + 10) + '"' + '>');
			writeCustom(TOFILE, "\t\t\t<children>");
			writeCustom(TOFILE, "\t\t\t\t");
			writeCustom(TOFILE, "\t\t\t\t<Button id=\"start\" onAction=\"#firstPage\" >");
			writeCustom(TOFILE, "\t\t\t\t\t<tooltip>");
			writeCustom(TOFILE, "\t\t\t\t\t\t<Tooltip text=\"First page\" />");
			writeCustom(TOFILE, "\t\t\t\t\t</tooltip>");
			writeCustom(TOFILE, "\t\t\t\t\t<graphic>");
			writeCustom(TOFILE, "\t\t\t\t\t\t<ImageView>");
			writeCustom(TOFILE, "\t\t\t\t\t\t\t<image>");
			writeCustom(TOFILE, "\t\t\t\t\t\t\t\t<Image url=\"@icons/smstart.gif\"/>");
			writeCustom(TOFILE, "\t\t\t\t\t\t\t</image>");
			writeCustom(TOFILE, "\t\t\t\t\t\t</ImageView>");
			writeCustom(TOFILE, "\t\t\t\t\t</graphic>");
			writeCustom(TOFILE, "\t\t\t\t</Button>");
			writeCustom(TOFILE, "\t\t\t\t<Button id=\"back10Pages\" onAction=\"#previous10Pages\" >");
			writeCustom(TOFILE, "\t\t\t\t\t<tooltip>");
			writeCustom(TOFILE, "\t\t\t\t\t\t<Tooltip text=\"Previous 10 pages\" />");
			writeCustom(TOFILE, "\t\t\t\t\t</tooltip>");
			writeCustom(TOFILE, "\t\t\t\t\t<graphic>");
			writeCustom(TOFILE, "\t\t\t\t\t\t<ImageView>");
			writeCustom(TOFILE, "\t\t\t\t\t\t\t<image>");
			writeCustom(TOFILE, "\t\t\t\t\t\t\t\t<Image url=\"@icons/smfback.gif\"/>");
			writeCustom(TOFILE, "\t\t\t\t\t\t\t</image>");
			writeCustom(TOFILE, "\t\t\t\t\t\t</ImageView>");
			writeCustom(TOFILE, "\t\t\t\t\t</graphic>");
			writeCustom(TOFILE, "\t\t\t\t</Button>");
			writeCustom(TOFILE, "\t\t\t\t<Button id=\"backOnePage\" onAction=\"#previousPage\" >");
			writeCustom(TOFILE, "\t\t\t\t\t<tooltip>");
			writeCustom(TOFILE, "\t\t\t\t\t\t<Tooltip text=\"Previous page\" />");
			writeCustom(TOFILE, "\t\t\t\t\t</tooltip>");
			writeCustom(TOFILE, "\t\t\t\t\t<graphic>");
			writeCustom(TOFILE, "\t\t\t\t\t\t<ImageView>");
			writeCustom(TOFILE, "\t\t\t\t\t\t\t<image>");
			writeCustom(TOFILE, "\t\t\t\t\t\t\t\t<Image url=\"@icons/smback.gif\"/>");
			writeCustom(TOFILE, "\t\t\t\t\t\t\t</image>");
			writeCustom(TOFILE, "\t\t\t\t\t\t</ImageView>");
			writeCustom(TOFILE, "\t\t\t\t\t</graphic>");
			writeCustom(TOFILE, "\t\t\t\t</Button>");
			writeCustom(TOFILE, "\t\t\t\t<TextField alignment=\"center\" text= \"" + this.pageNumber
					+ "\" maxHeight=\"25\" prefWidth=\"50\" editable= \"NO\"/>");
			writeCustom(TOFILE, "\t\t\t\t<Label text=\":\" style=\"-fx-font: DARK 26 Arial;\"/>");
			writeCustom(TOFILE, "\t\t\t\t<TextField alignment=\"center\" text= \"" + this.endPage
					+ "\" maxHeight=\"25\" prefWidth=\"50\" editable= \"NO\"/>");
			writeCustom(TOFILE, "\t\t\t\t<Button id=\"forward\" onAction=\"#nextPage\" >");
			writeCustom(TOFILE, "\t\t\t\t\t<tooltip>");
			writeCustom(TOFILE, "\t\t\t\t\t\t<Tooltip text=\"Next page\" />");
			writeCustom(TOFILE, "\t\t\t\t\t</tooltip>");
			writeCustom(TOFILE, "\t\t\t\t\t<graphic>");
			writeCustom(TOFILE, "\t\t\t\t\t\t<ImageView>");
			writeCustom(TOFILE, "\t\t\t\t\t\t\t<image>");
			writeCustom(TOFILE, "\t\t\t\t\t\t\t\t<Image url=\"@icons/smforward.gif\"/>");
			writeCustom(TOFILE, "\t\t\t\t\t\t\t</image>");
			writeCustom(TOFILE, "\t\t\t\t\t\t</ImageView>");
			writeCustom(TOFILE, "\t\t\t\t\t</graphic>");
			writeCustom(TOFILE, "\t\t\t\t</Button>");
			writeCustom(TOFILE, "\t\t\t\t<Button id=\"next10Pages\" onAction=\"#next10Pages\" >");
			writeCustom(TOFILE, "\t\t\t\t\t<tooltip>");
			writeCustom(TOFILE, "\t\t\t\t\t\t<Tooltip text=\"Next 10 pages\" />");
			writeCustom(TOFILE, "\t\t\t\t\t</tooltip>");
			writeCustom(TOFILE, "\t\t\t\t\t<graphic>");
			writeCustom(TOFILE, "\t\t\t\t\t\t<ImageView>");
			writeCustom(TOFILE, "\t\t\t\t\t\t\t<image>");
			writeCustom(TOFILE, "\t\t\t\t\t\t\t\t<Image url=\"@icons/smfforward.gif\"/>");
			writeCustom(TOFILE, "\t\t\t\t\t\t\t</image>");
			writeCustom(TOFILE, "\t\t\t\t\t\t</ImageView>");
			writeCustom(TOFILE, "\t\t\t\t\t</graphic>");
			writeCustom(TOFILE, "\t\t\t\t</Button>");
			writeCustom(TOFILE, "\t\t\t\t<Button  id=\"end\" onAction=\"#endPage\" >");
			writeCustom(TOFILE, "\t\t\t\t\t<tooltip>");
			writeCustom(TOFILE, "\t\t\t\t\t\t<Tooltip text=\"Last page\" />");
			writeCustom(TOFILE, "\t\t\t\t\t</tooltip>");
			writeCustom(TOFILE, "\t\t\t\t\t<graphic>");
			writeCustom(TOFILE, "\t\t\t\t\t\t<ImageView>");
			writeCustom(TOFILE, "\t\t\t\t\t\t\t<image>");
			writeCustom(TOFILE, "\t\t\t\t\t\t\t\t<Image url=\"@icons/smend.gif\"/>");
			writeCustom(TOFILE, "\t\t\t\t\t\t\t</image>");
			writeCustom(TOFILE, "\t\t\t\t\t\t</ImageView>");
			writeCustom(TOFILE, "\t\t\t\t\t</graphic>");
			writeCustom(TOFILE, "\t\t\t\t</Button>");
			writeCustom(TOFILE, "\t\t\t\t<Hyperlink onAction=\"#ourWebSite\">");
			writeCustom(TOFILE, "\t\t\t\t\t<graphic>");
			writeCustom(TOFILE, "\t\t\t\t\t\t<ImageView>");
			writeCustom(TOFILE, "\t\t\t\t\t\t\t<image>");
			writeCustom(TOFILE, "\t\t\t\t\t\t\t\t<Image url=\"@icons/logo.gif\"/>");
			writeCustom(TOFILE, "\t\t\t\t\t\t\t</image>");
			writeCustom(TOFILE, "\t\t\t\t\t\t</ImageView>");
			writeCustom(TOFILE, "\t\t\t\t\t</graphic>");
			writeCustom(TOFILE, "\t\t\t\t\t<tooltip>");
			writeCustom(TOFILE, "\t\t\t\t\t\t<Tooltip text=\"Visit our website\" />");
			writeCustom(TOFILE, "\t\t\t\t\t</tooltip>");
			writeCustom(TOFILE, "\t\t\t\t</Hyperlink>");
			writeCustom(TOFILE, "\t\t\t\t");
			writeCustom(TOFILE, "\t\t\t</children>");
			writeCustom(TOFILE, "\t\t</HBox>");
			// End of NavBar content
			writeCustom(TOFILE, "\t</bottom>");

		}

		// End of file
		writeCustom(TOFILE, "</BorderPane>");

		/**
		 * Write out mainClass content for each FXML file
		 */
		createMainClassContent(onePageFile);

		/**
		 * Write out PDFStage content used to make sure the same stage is passed between classes
		 */
		if (!onePageFile) {// navBar related
			createFXMLPDFStage();
		}

		this.customIO.flush();
	}

	private String createFXMLPDFStage() {

		writeCustom(OutputDisplay.FXMLPDFSTAGE, "/**");
		writeCustom(OutputDisplay.FXMLPDFSTAGE, "* ===========================================");
		writeCustom(OutputDisplay.FXMLPDFSTAGE, "* Java Pdf Extraction Decoding Access Library");
		writeCustom(OutputDisplay.FXMLPDFSTAGE, "* ===========================================");
		writeCustom(OutputDisplay.FXMLPDFSTAGE, "*");
		writeCustom(OutputDisplay.FXMLPDFSTAGE, "* Project Info:  http://www.idrsolutions.com");
		writeCustom(OutputDisplay.FXMLPDFSTAGE, "*");
		writeCustom(OutputDisplay.FXMLPDFSTAGE, "* generated by JPedal PDF to FXML");
		writeCustom(OutputDisplay.FXMLPDFSTAGE, "*");
		writeCustom(OutputDisplay.FXMLPDFSTAGE, "* --------------------------------------------");
		writeCustom(OutputDisplay.FXMLPDFSTAGE, "* PDFStage.java");
		writeCustom(OutputDisplay.FXMLPDFSTAGE, "* --------------------------------------------");
		writeCustom(OutputDisplay.FXMLPDFSTAGE, "* --------------------------------------------");
		writeCustom(OutputDisplay.FXMLPDFSTAGE, "* " + getDate());
		writeCustom(OutputDisplay.FXMLPDFSTAGE, "* --------------------------------------------");
		writeCustom(OutputDisplay.FXMLPDFSTAGE, "*/");
		writeCustom(OutputDisplay.FXMLPDFSTAGE, "");
		writeCustom(OutputDisplay.FXMLPDFSTAGE, "");
		writeCustom(OutputDisplay.FXMLPDFSTAGE, "package " + this.packageName + ';');
		writeCustom(OutputDisplay.FXMLPDFSTAGE, "");
		writeCustom(OutputDisplay.FXMLPDFSTAGE, "import javafx.stage.Stage;");
		writeCustom(OutputDisplay.FXMLPDFSTAGE, "");
		writeCustom(OutputDisplay.FXMLPDFSTAGE, "");
		writeCustom(OutputDisplay.FXMLPDFSTAGE, "public class PDFStage {");
		writeCustom(OutputDisplay.FXMLPDFSTAGE, "");
		writeCustom(OutputDisplay.FXMLPDFSTAGE, "");
		writeCustom(OutputDisplay.FXMLPDFSTAGE, "\tprivate static Stage stage = null;");
		writeCustom(OutputDisplay.FXMLPDFSTAGE, "\t");
		writeCustom(OutputDisplay.FXMLPDFSTAGE, "\tpublic static Stage getInstance(){");
		writeCustom(OutputDisplay.FXMLPDFSTAGE, "\t");
		writeCustom(OutputDisplay.FXMLPDFSTAGE, "\t\tif(stage == null){");
		writeCustom(OutputDisplay.FXMLPDFSTAGE, "\t\t\tstage = new Stage();");
		writeCustom(OutputDisplay.FXMLPDFSTAGE, "\t\t}");
		writeCustom(OutputDisplay.FXMLPDFSTAGE, "\t");
		writeCustom(OutputDisplay.FXMLPDFSTAGE, "\t\tstage.centerOnScreen();");
		writeCustom(OutputDisplay.FXMLPDFSTAGE, "\t\tstage.resizableProperty().set(false);");
		writeCustom(OutputDisplay.FXMLPDFSTAGE, "\t");
		writeCustom(OutputDisplay.FXMLPDFSTAGE, "\t\treturn stage;");
		writeCustom(OutputDisplay.FXMLPDFSTAGE, "\t");
		writeCustom(OutputDisplay.FXMLPDFSTAGE, "\t}");
		writeCustom(OutputDisplay.FXMLPDFSTAGE, "\t");
		writeCustom(OutputDisplay.FXMLPDFSTAGE, "}");

		// make sure FXML Dir exists
		String fxmlPDFStagePath = this.rootDir;
		File fxmlPDFStageDir = new File(fxmlPDFStagePath);
		if (!fxmlPDFStageDir.exists()) {
			fxmlPDFStageDir.mkdirs();
		}

		try {
			PrintWriter fxmlPDFStageOutput = new PrintWriter(new FileOutputStream(fxmlPDFStagePath + "PDFStage.java"));

			fxmlPDFStageOutput.println(this.fxmlPDFStage.toString());

			fxmlPDFStageOutput.flush();
			fxmlPDFStageOutput.close();

		}
		catch (Exception e) {
			// tell user and log
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
		}

		return fxmlPDFStagePath;
	}

	private String createMainClassContent(Boolean onePageFile) {

		writeCustom(OutputDisplay.CSS, "/**");
		writeCustom(OutputDisplay.CSS, "* ===========================================");
		writeCustom(OutputDisplay.CSS, "* Java Pdf Extraction Decoding Access Library");
		writeCustom(OutputDisplay.CSS, "* ===========================================");
		writeCustom(OutputDisplay.CSS, "*");
		writeCustom(OutputDisplay.CSS, "* Project Info:  http://www.idrsolutions.com");
		writeCustom(OutputDisplay.CSS, "*");
		writeCustom(OutputDisplay.CSS, "* generated by JPedal PDF to FXML");
		writeCustom(OutputDisplay.CSS, "*");
		writeCustom(OutputDisplay.CSS, "* --------------------------------------------");
		writeCustom(OutputDisplay.CSS, "* " + this.packageName + ".pdf");
		writeCustom(OutputDisplay.CSS, "* --------------------------------------------");
		writeCustom(OutputDisplay.CSS, "* --------------------------------------------");
		writeCustom(OutputDisplay.CSS, "* " + getDate());
		writeCustom(OutputDisplay.CSS, "* --------------------------------------------");
		writeCustom(OutputDisplay.CSS, "*/");
		writeCustom(OutputDisplay.CSS, "");
		writeCustom(OutputDisplay.CSS, "");
		writeCustom(OutputDisplay.CSS, " package " + this.packageName + ';');
		writeCustom(OutputDisplay.CSS, "");
		writeCustom(OutputDisplay.CSS, "");
		writeCustom(OutputDisplay.CSS, " import java.io.IOException;");
		writeCustom(OutputDisplay.CSS, " import java.net.URI;");
		writeCustom(OutputDisplay.CSS, " import java.net.URISyntaxException;");
		writeCustom(OutputDisplay.CSS, " import javafx.application.Application;");
		writeCustom(OutputDisplay.CSS, " import java.net.URL;");
		writeCustom(OutputDisplay.CSS, " import java.util.ResourceBundle;");
		writeCustom(OutputDisplay.CSS, " import javafx.fxml.Initializable;");
		writeCustom(OutputDisplay.CSS, " import javafx.fxml.FXMLLoader;");
		writeCustom(OutputDisplay.CSS, " import javafx.scene.Parent;");
		writeCustom(OutputDisplay.CSS, " import javafx.scene.Scene;");
		writeCustom(OutputDisplay.CSS, " import javafx.stage.Stage;");
		writeCustom(OutputDisplay.CSS, " import javafx.fxml.FXML;");
		writeCustom(OutputDisplay.CSS, " import javafx.event.ActionEvent;");
		writeCustom(OutputDisplay.CSS, " import javafx.scene.text.Text;");
		writeCustom(OutputDisplay.CSS, " import javafx.scene.text.Font;");
		writeCustom(OutputDisplay.CSS, " import javafx.scene.text.FontPosture;");
		writeCustom(OutputDisplay.CSS, " import javafx.scene.text.FontWeight;");

		if (!onePageFile) {// navBar related
			writeCustom(OutputDisplay.CSS, " import javafx.scene.layout.Pane;");
			writeCustom(OutputDisplay.CSS, " import javafx.util.Duration;");
			writeCustom(OutputDisplay.CSS, " import javafx.animation.FadeTransition;");
			writeCustom(OutputDisplay.CSS, " import javafx.scene.Node;");
			writeCustom(OutputDisplay.CSS, " import java.lang.reflect.Method;");
		}

		writeCustom(OutputDisplay.CSS, " ");
		writeCustom(OutputDisplay.CSS, "");
		writeCustom(OutputDisplay.CSS, "");
		writeCustom(OutputDisplay.CSS, "public class "
				+ (this.firstPageName != null && this.pageNumber == 1 ? this.firstPageName : "page" + this.pageNumberAsString)
				+ " extends Application implements Initializable{");
		writeCustom(OutputDisplay.CSS, "");

		if (!onePageFile) {// navBar related
			writeCustom(OutputDisplay.CSS, "\t@FXML private Pane PDFContent;");
		}

		// Creating text elements.
		for (int i = 1; i <= this.textID - 1; i++) {
			writeCustom(OutputDisplay.CSS, "\t@FXML private Text tb_" + i + ';');
		}

		writeCustom(OutputDisplay.CSS, "");
		writeCustom(OutputDisplay.CSS, "\tpublic static void main(String[] args) {");
		writeCustom(OutputDisplay.CSS, "\t\tApplication.launch("
				+ (this.firstPageName != null && this.pageNumber == 1 ? this.firstPageName : "page" + this.pageNumberAsString) + ".class, args);");
		writeCustom(OutputDisplay.CSS, "\t}");
		writeCustom(OutputDisplay.CSS, "\t");
		writeCustom(OutputDisplay.CSS, "\t@Override");
		writeCustom(OutputDisplay.CSS, "\tpublic void start(Stage stage) throws Exception {");

		if (!onePageFile) {// navBar related
			writeCustom(OutputDisplay.CSS, "\t\tstage = PDFStage.getInstance();");
		}

		writeCustom(OutputDisplay.CSS, "\t\tloadPage(stage);");
		writeCustom(OutputDisplay.CSS, "\t}");
		writeCustom(OutputDisplay.CSS, "");
		writeCustom(OutputDisplay.CSS, "\t@Override");
		writeCustom(OutputDisplay.CSS, "\tpublic void initialize(URL url, ResourceBundle rb) {");

		if (!onePageFile) {// navBar related
			writeCustom(OutputDisplay.CSS, "\t\tcreateTransition(PDFContent);");
		}

		// This will limit the number of lines to lineLimit, and chain methods so that they cannot be too large to compile.
		if (this.fxmlText.size() != 0) {

			writeCustom(OutputDisplay.CSS, "\t\tdrawPage0();");
			writeCustom(OutputDisplay.CSS, "\t}\n");

			int methodNum = 0;
			int lineNum = 0;
			int lineLimit = 50;
			while (true) {
				StringBuilder sb = new StringBuilder(lineLimit);
				for (int ii = 0; ii < lineLimit; ii++) {
					if (this.fxmlText.size() > lineNum) {
						sb.append(this.fxmlText.get(lineNum) + '\n');
						lineNum++;
					}
					else break;
				}

				writeCustom(OutputDisplay.CSS, "\tprivate void drawPage" + methodNum + "() {");
				writeCustom(OutputDisplay.CSS, sb.toString());

				if (this.fxmlText.size() > lineNum) {
					writeCustom(OutputDisplay.CSS, "\t\tdrawPage" + (++methodNum)
							+ "();// Chain to prevent methods being over 64KB so that the class will not refuse to compile. \n\t}\n");
				}
				else {
					break;
				}
			}

		}

		writeCustom(OutputDisplay.CSS, "\t}");
		writeCustom(OutputDisplay.CSS, "\t");

		if (!onePageFile) {// navBar related

			writeCustom(OutputDisplay.CSS, "\t@FXML");
			writeCustom(OutputDisplay.CSS, "\tprivate void firstPage(ActionEvent event) {");
			writeCustom(OutputDisplay.CSS, "\t\tnewPage( 0 , false , PDFStage.getInstance());");
			writeCustom(OutputDisplay.CSS, "\t}");
			writeCustom(OutputDisplay.CSS, "\t");
			writeCustom(OutputDisplay.CSS, "\t@FXML");
			writeCustom(OutputDisplay.CSS, "\tprivate void previous10Pages(ActionEvent event) {");
			writeCustom(OutputDisplay.CSS, "\t\tnewPage( 10 , false , PDFStage.getInstance());");
			writeCustom(OutputDisplay.CSS, "\t}");
			writeCustom(OutputDisplay.CSS, "\t");
			writeCustom(OutputDisplay.CSS, "\t@FXML");
			writeCustom(OutputDisplay.CSS, "\tprivate void previousPage(ActionEvent event) {");
			writeCustom(OutputDisplay.CSS, "\t\tnewPage( 1 , false , PDFStage.getInstance());");
			writeCustom(OutputDisplay.CSS, "\t}");
			writeCustom(OutputDisplay.CSS, "\t");
			writeCustom(OutputDisplay.CSS, "\t@FXML");
			writeCustom(OutputDisplay.CSS, "\tprivate void nextPage(ActionEvent event) {");
			writeCustom(OutputDisplay.CSS, "\t\tnewPage( 1 , true , PDFStage.getInstance());");
			writeCustom(OutputDisplay.CSS, "\t}");
			writeCustom(OutputDisplay.CSS, "\t");
			writeCustom(OutputDisplay.CSS, "\t@FXML");
			writeCustom(OutputDisplay.CSS, "\tprivate void next10Pages(ActionEvent event) {");
			writeCustom(OutputDisplay.CSS, "\t\tnewPage( 10 , true , PDFStage.getInstance());");
			writeCustom(OutputDisplay.CSS, "\t}");
			writeCustom(OutputDisplay.CSS, "\t");
			writeCustom(OutputDisplay.CSS, "\t@FXML");
			writeCustom(OutputDisplay.CSS, "\tprivate void endPage(ActionEvent event) {");
			writeCustom(OutputDisplay.CSS, "\t\tnewPage( 9999 , false , PDFStage.getInstance());");
			writeCustom(OutputDisplay.CSS, "\t}");
			writeCustom(OutputDisplay.CSS, "\t");
			writeCustom(OutputDisplay.CSS, "\t@FXML");
			writeCustom(OutputDisplay.CSS, "\tprivate void ourWebSite(ActionEvent event) throws IOException, URISyntaxException {");
			writeCustom(OutputDisplay.CSS, "\t\tjava.awt.Desktop.getDesktop().browse(new URI(\"http://www.idrsolutions.com/\"));");
			writeCustom(OutputDisplay.CSS, "\t}");
			writeCustom(OutputDisplay.CSS, "\t");

		}

		writeCustom(OutputDisplay.CSS, "\tpublic void loadPage(Stage stage) throws IOException {");
		writeCustom(OutputDisplay.CSS, "\t");
		writeCustom(OutputDisplay.CSS, "\t\tParent root = FXMLLoader.load(getClass().getResource(\""
				+ (this.firstPageName != null && this.pageNumber == 1 ? this.firstPageName : "page" + this.pageNumberAsString) + ".fxml\"));");
		writeCustom(OutputDisplay.CSS, "\t\t");

		if (onePageFile) {
			writeCustom(OutputDisplay.CSS, "\t\tstage.setTitle(\"" + this.packageName + "\");");
		}
		else {// navBar related
			writeCustom(OutputDisplay.CSS, "\t\tstage.setTitle(\"" + this.packageName + " - Page " + this.pageNumber + "\");");
		}

		writeCustom(OutputDisplay.CSS, "\t\tstage.setScene(new Scene(root));");
		writeCustom(OutputDisplay.CSS, "\t\tstage.show();");
		writeCustom(OutputDisplay.CSS, "\t}");
		writeCustom(OutputDisplay.CSS, "\t");
		writeCustom(OutputDisplay.CSS, "\tprivate static void setTextsize(Text textBox, float requiredWidth) {");
		writeCustom(OutputDisplay.CSS, "\t\tfloat actualWidth=(int) textBox.getLayoutBounds().getWidth();");
		writeCustom(OutputDisplay.CSS, "\t\tfloat dx=requiredWidth-actualWidth;");
		writeCustom(OutputDisplay.CSS, "\t\tfloat scalingNeeded=requiredWidth/actualWidth;");
		writeCustom(OutputDisplay.CSS, "\t\ttextBox.setScaleX(scalingNeeded);");
		writeCustom(OutputDisplay.CSS, "\t\ttextBox.setScaleY(scalingNeeded);");
		writeCustom(OutputDisplay.CSS, "\t\ttextBox.setTranslateX(dx/2);");
		writeCustom(OutputDisplay.CSS, "\t}");
		writeCustom(OutputDisplay.CSS, "\t");

		if (!onePageFile) {// navBar related

			writeCustom(OutputDisplay.CSS, "\tprivate static void createTransition(Node i){");
			writeCustom(OutputDisplay.CSS, "\t\tFadeTransition fadeOutTransition = new FadeTransition(Duration.seconds(2), i);");
			writeCustom(OutputDisplay.CSS, "\t\tfadeOutTransition.setFromValue(0.0);");
			writeCustom(OutputDisplay.CSS, "\t\tfadeOutTransition.setToValue(3.0);");
			writeCustom(OutputDisplay.CSS, "\t\tfadeOutTransition.play();");
			writeCustom(OutputDisplay.CSS, "\t}");
			writeCustom(OutputDisplay.CSS, "\t");
			writeCustom(OutputDisplay.CSS, "\tprivate static void newPage(int change, boolean forward, final Stage stage) {");
			writeCustom(OutputDisplay.CSS, "\t");
			writeCustom(OutputDisplay.CSS, "\t\tfinal int currentPageNo = " + this.pageNumber + ", pageCount = " + this.endPage + " ;");
			writeCustom(OutputDisplay.CSS, "\t\tint newPageNo = 0;");
			writeCustom(OutputDisplay.CSS, "\t\t");
			writeCustom(OutputDisplay.CSS, "\t\tif (change == 0) { //special case 1st page");
			writeCustom(OutputDisplay.CSS, "\t\t\tnewPageNo = 1;");
			writeCustom(OutputDisplay.CSS, "\t\t} else if (change == 9999) { //special case last page");
			writeCustom(OutputDisplay.CSS, "\t\t\tnewPageNo = pageCount;");
			writeCustom(OutputDisplay.CSS, "\t\t} else {");
			writeCustom(OutputDisplay.CSS, "\t\t\tif (forward) {");
			writeCustom(OutputDisplay.CSS, "\t\t\t\tnewPageNo = currentPageNo + change;");
			writeCustom(OutputDisplay.CSS, "\t\t\t} else {");
			writeCustom(OutputDisplay.CSS, "\t\t\t\tnewPageNo = currentPageNo - change;");
			writeCustom(OutputDisplay.CSS, "\t\t\t}");
			writeCustom(OutputDisplay.CSS, "\t\t}");
			writeCustom(OutputDisplay.CSS, "\t\t");
			writeCustom(OutputDisplay.CSS, "\t\t//error check for bounds");
			writeCustom(OutputDisplay.CSS, "\t\tif (newPageNo < 1) {");
			writeCustom(OutputDisplay.CSS, "\t\t\tnewPageNo = 1;");
			writeCustom(OutputDisplay.CSS, "\t\t} else if (newPageNo > pageCount) {");
			writeCustom(OutputDisplay.CSS, "\t\t\tnewPageNo = pageCount;");
			writeCustom(OutputDisplay.CSS, "\t\t}");
			writeCustom(OutputDisplay.CSS, "\t\t");

			// Check to make sure that firstPageName isn't empty
			if (this.firstPageName != null && this.firstPageName.length() > 0) {
				writeCustom(OutputDisplay.CSS, "\t\tString customClassName = \"" + this.packageName + '.' + this.firstPageName + "\";");
				writeCustom(OutputDisplay.CSS, "\t\tboolean customFirstPage = false;");
			}
			writeCustom(OutputDisplay.CSS, "\t\t//Add zero when required as prefix of the new pages.");
			writeCustom(OutputDisplay.CSS, "\t\tString newPageNoAsString = String.valueOf(newPageNo);");

			// Check to make sure that firstPageName isn't empty
			if (this.firstPageName != null && this.firstPageName.length() > 0) {
				writeCustom(OutputDisplay.CSS, "");
				writeCustom(OutputDisplay.CSS, "\t\tif(newPageNoAsString.equals(\"1\"))");
				writeCustom(OutputDisplay.CSS, "\t\t\tcustomFirstPage=true;");
				writeCustom(OutputDisplay.CSS, "");
			}

			writeCustom(OutputDisplay.CSS, "\t\tString maxNumberOfPages = String.valueOf(pageCount);");
			writeCustom(OutputDisplay.CSS, "\t\tint padding = maxNumberOfPages.length() - newPageNoAsString.length();");
			writeCustom(OutputDisplay.CSS, "\t\tfor (int ii = 0; ii < padding; ii++) {");
			writeCustom(OutputDisplay.CSS, "\t\t\tnewPageNoAsString = '0' + newPageNoAsString;");
			writeCustom(OutputDisplay.CSS, "\t\t}");
			writeCustom(OutputDisplay.CSS, "\t\t");

			// Check to make sure that firstPageName isn't empty
			if (this.firstPageName != null && this.firstPageName.length() > 0) {
				writeCustom(OutputDisplay.CSS, "\t\t//workout new class from pageNumber");
				writeCustom(OutputDisplay.CSS, "\t\tString newClass=\"\";");
				writeCustom(OutputDisplay.CSS, "\t\t");
				writeCustom(OutputDisplay.CSS, "\t\tif(customFirstPage)");
				writeCustom(OutputDisplay.CSS, "\t\t\tnewClass=customClassName;");
				writeCustom(OutputDisplay.CSS, "\t\telse");
				writeCustom(OutputDisplay.CSS, "\t\t\tnewClass=\"" + this.packageName + ".page" + "\"+newPageNoAsString;");
			}
			else {
				writeCustom(OutputDisplay.CSS, "\t\t//workout new class from pageNumber");
				writeCustom(OutputDisplay.CSS, "\t\tString newClass=\"" + this.packageName + ".page" + "\"+newPageNoAsString;");
			}
			writeCustom(OutputDisplay.CSS, "\t\t");
			writeCustom(OutputDisplay.CSS, "\t\t//create an instance");
			writeCustom(OutputDisplay.CSS, "\t\ttry {");
			writeCustom(OutputDisplay.CSS, "\t\t\tClass c = Class.forName(newClass);");
			writeCustom(OutputDisplay.CSS, "\t\t\tApplication nextPage = (javafx.application.Application) c.newInstance();");
			writeCustom(OutputDisplay.CSS, "\t\t");
			writeCustom(OutputDisplay.CSS, "\t\t\tMethod m = c.getMethod(\"loadPage\", new Class[]{Stage.class});");
			writeCustom(OutputDisplay.CSS, "\t\t\tm.invoke(nextPage, new Object[]{stage});");
			writeCustom(OutputDisplay.CSS, "\t\t\t");
			writeCustom(OutputDisplay.CSS, "\t\t} catch (Exception e) {");
			writeCustom(OutputDisplay.CSS, "\t\t\te.printStackTrace();");
			writeCustom(OutputDisplay.CSS, "\t\t}");
			writeCustom(OutputDisplay.CSS, "\t\t");

			// End of newPage Method
			writeCustom(OutputDisplay.CSS, "\t}");

		}

		// End of class
		writeCustom(OutputDisplay.CSS, "}");

		// make sure css Dir exists
		String cssPath = this.rootDir;
		File cssDir = new File(cssPath);
		if (!cssDir.exists()) {
			cssDir.mkdirs();
		}

		try {
			PrintWriter CSSOutput = new PrintWriter(new FileOutputStream(cssPath
					+ (this.firstPageName != null && this.pageNumber == 1 ? this.firstPageName : "page" + this.pageNumberAsString) + ".java"));

			// css header

			CSSOutput.println(this.css.toString());

			CSSOutput.flush();
			CSSOutput.close();

		}
		catch (Exception e) {
			// tell user and log
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
		}
		return cssPath;
	}

	private int textX, textY;
	private String currentAffine = "";
	private boolean simpleText; // Doesn't require matrix.

	@Override
	protected void writeoutTextAsDiv(float fontScaling) {

		if (this.currentTextBlock.isEmpty()) {
			throw new RuntimeException("writeoutTextAsDiv() called incorrectly.  Attempted to write out text with empty text block use flushText.");
		}

		// uniqueID
		this.divName = "tb_" + this.textID;
		String text = this.currentTextBlock.getOutputString(true);

		// font
		int adjustedFontSize = (this.currentTextBlock.getFontSize() + this.currentTextBlock.getFontAdjustment());

		int altFontSize = this.currentTextBlock.getAltFontSize();
		int fontCondition = this.currentTextBlock.getFontCondition();
		if (altFontSize > 0) {

			switch (fontCondition) {
				case 1:
					if (text.length() > 32) adjustedFontSize = altFontSize;
					break;

				case 2:
					adjustedFontSize = altFontSize;
					break;
			}
		}

		int fontSizeAsString = (int) (adjustedFontSize * this.scaling);

		// ignore tiny text
		if (adjustedFontSize < 1) return;

		this.textWithSpaces = this.currentTextBlock.getOutputString(false);
		this.fontTextLength = this.textWithSpaces.length();

		writePosition("", false, fontScaling); // A little different for FXML - it sets boolean simpleText and int textX & textY.

		String position = this.simpleText ? "layoutX=\"" + this.textX + "\" layoutY=\"" + this.textY + "\" " : "";

		// Set the font weight - update as needed.
		String weight = this.currentTextBlock.getWeight();
		String fxmlWeight = setJavaFxWeight(weight);

		// set the font style - Regular or italic
		String style;
		if (this.currentTextBlock.getStyle().equals("normal")) style = "REGULAR";
		else style = "ITALIC";

		// I considered putting font setting in the .fxml file to simplify things but as the weight and posture has
		// to go in with the name, it complicates things and will not work nice with our own fonts in the future.
		writeCustom(FXMLTEXT, "\t" + this.divName + ".setFont(Font.font(\"" + this.currentTextBlock.getFont() + "\", FontWeight." + fxmlWeight
				+ ", FontPosture." + style + ", " + fontSizeAsString + "));");

		// Leon come back to this. Same for JavaFX
		// writeCustom(OutputDisplay.CSS, "\t\tsetTextsize("+nameOfText+ ',' + ((int)(currentTextBlock.getWidth()))+");\n");

		String color = "";
		// update color if not black(-14475232)
		if (this.currentTextBlock.getColor() != -14475232) {
			color = "fill=\"" + hexColor(this.currentTextBlock.getColor()) + "\"";
		}

		if (text.startsWith("$")) {
			text = "\u200B$" + text.substring(1); // Case 13202 - FXML text can't start with a dollar.
		}

		writeCustom(SCRIPT, "\n\t<Text fx:id=\"" + this.divName + "\" text=\"" + tidyQuotes(text) + "\" " + position + color + ">"
				+ this.currentAffine + "</Text>");

		this.textID++;
	}

	@Override
	protected void writeTextPosition(float[] aff, int tx, int ty, float fontScaling) {

		if (aff[0] > 0 && aff[3] > 0 && aff[1] == 0 && aff[2] == 0) { // simple case (left to right text)

			this.simpleText = true;
			this.currentAffine = "";
			this.textX = tx;
			this.textY = ty;

		}
		else { // needs a matrix to handle rotation

			this.simpleText = false;
			this.currentAffine = "\n\t\t<transforms>\n\t\t\t<javafx.scene.transform.Affine mxx=\"" + setPrecision(aff[0] / fontScaling, 2)
					+ "\" myx=\"" + setPrecision(aff[1] / fontScaling, 2) + "\" mxy=\"" + setPrecision(aff[2] / fontScaling, 2) + "\" myy=\""
					+ setPrecision(aff[3] / fontScaling, 2) + "\" tx=\"" + tx + "\" ty=\"" + ty + "\" />\n\t\t</transforms>\n\t";
			// writeCustom(FXMLTEXT, '\t' +divName+".getTransforms().add(Transform.affine(" + setPrecision(aff[0]/ fontScaling,2) + ',' +
			// setPrecision(aff[1]/ fontScaling,2) + ',' + setPrecision(aff[2]/ fontScaling,2) + ',' +
			// setPrecision(aff[3]/ fontScaling,2) + ',' + tx + ", " + ty + "));");

		}
	}

	/**
	 * Handles any characters which need conversion or escaping
	 * 
	 * @param outputString
	 * @return Clean outputString that works with FXML
	 */
	private static String tidyQuotes(String outputString) {
		char[] character = { '\"', '&', '<', '>', '\'', '\u00A3' };
		String[] replacement = { "&quot;", "&amp;", "&lt;", "&gt;", "&apos;", "&#163;" };
		String newOutput = "";

		for (int pos = 0; pos < outputString.length(); pos++) {
			// Loop through the string checking each character to see if it needs replacing
			boolean replaced = false;
			for (int i = 0; i < character.length; i++) {
				if (outputString.charAt(pos) == character[i]) {
					newOutput += replacement[i];
					replaced = true;
					break;
				}
			}
			if (!replaced) {
				newOutput += outputString.charAt(pos);
			}

		}

		return newOutput;
	}

	// allow user to control various values
	@Override
	public void setBooleanValue(int key, boolean value) {

		switch (key) {
			case IncludeJSFontResizingCode:
				this.includeJSFontResizingCode = value;
				break;
			default:
				super.setBooleanValue(key, value);
		}
	}

	/**
	 * allow user to set own value for certain tags Throws RuntimeException
	 * 
	 * @param type
	 * @param value
	 */
	@Override
	public void setTag(int type, String value) {

		switch (type) {

			case FORM_TAG:
				this.tag[FORM_TAG] = value;
				break;

			default:
				super.setTag(type, value);
		}
	}

	/**
	 * not actually needed because all the FX stuff happens via flushText() and FXHelper but added for development so we can disable
	 */
	@Override
	public void drawEmbeddedText(float[][] Trm, int fontSize, PdfGlyph embeddedGlyph, Object javaGlyph, int type, GraphicsState gs,
			AffineTransform at, String glyf, PdfFont currentFontData, float glyfWidth) {

		super.drawEmbeddedText(Trm, fontSize, embeddedGlyph, javaGlyph, type, gs, at, glyf, currentFontData, glyfWidth);
	}

	/**
	 * not actually needed because all the FX stuff happens via JavaFXShape() but added for development so we can disable
	 */
	@Override
	public void drawShape(Shape currentShape, GraphicsState gs, int cmd) {

		// if(true)
		// return;

		super.drawShape(currentShape, gs, cmd);
	}

	// save image in array to draw
	@Override
	public int drawImage(int pageNumber, BufferedImage image, GraphicsState gs, boolean alreadyCached, String name, int optionsApplied,
			int previousUse) {

		int flag = super.drawImage(pageNumber, image, gs, alreadyCached, name, optionsApplied, previousUse);

		if (flag == -2) { // returned by Super to show we use

			/**
			 * add in any image transparency.
			 */
			float opacity = gs.getAlpha(GraphicsState.FILL);
			String opacityAsString = "";
			if (opacity < 1.0f) opacityAsString = " opacity=\"" + opacity + "\"";

			writeCustom(SCRIPT, "\n\t<ImageView id=\"" + name + "\" layoutX=\"" + this.currentImage[0] + "\" layoutY=\"" + this.currentImage[1]
					+ "\" fitWidth=\"" + this.currentImage[2] + "\" fitHeight=\"" + this.currentImage[3] + "\"" + opacityAsString + ">");
			writeCustom(SCRIPT, "\t<image>");
			writeCustom(SCRIPT, "\t\t<Image url=\"@" + this.imageName + "\"/>");
			writeCustom(SCRIPT, "\t</image>");
			writeCustom(SCRIPT, "</ImageView>");
		}

		return -1;
	}

	@Override
	public void setOutputDir(String outputDir, String outputFilename, String pageNumberAsString) {

		super.setOutputDir(outputDir, outputFilename, pageNumberAsString);

		// create name as combination of both
		if (this.pageNumber == 1 && this.firstPageName != null && this.firstPageName.length() > 0) {// If firstPageName is set.
			this.javaFxFileName = this.firstPageName;
			this.fileName = pageNumberAsString;
		}
		else {
			this.javaFxFileName = "page" + pageNumberAsString;
			this.fileName = pageNumberAsString;
		}

		this.packageName = outputFilename;
		if (this.packageName.contains(" ")) this.packageName = this.packageName.replaceAll(" ", "_");
	}

	/* setup renderer */
	@Override
	public void init(int width, int height, int rawRotation, Color backgroundColor) {

		super.init(width, height, rawRotation, backgroundColor);

		/**
		 * create the file or tell user to set
		 */
		if (this.rootDir == null) throw new RuntimeException("Please pass in output_dir (second param if running ExtractpageAsJavaFX");

		try {
			this.customIO.setupOutput(this.rootDir + this.javaFxFileName + ".fxml", false, this.encodingType[JAVA_TYPE]);
		}
		catch (Exception e) {
			// tell user and log
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
		}
	}

	/**
	 * Add HTML to correct area so we can assemble later. Can also be used for any specific code features (ie setting a value)
	 */
	@Override
	public synchronized void writeCustom(int section, Object str) {

		// System.out.println(output+" "+str);

		switch (section) {

			case TOFILE:
				this.customIO.writeString(str.toString());
				break;

			case TOP_SECTION:
				this.topSection.append('\t'); // indent
				this.topSection.append(str.toString());
				this.topSection.append('\n');
				break;

			case SCRIPT:
				this.script.append('\t'); // indent
				this.script.append(str.toString());
				this.script.append('\n');
				break;

			case FXMLTEXT:
				this.fxmlText.add("\t" + str.toString());
				break;

			case FORM:
				this.form.append(str.toString());
				break;

			case TEXT:

				this.testDivs.append(str.toString());
				break;

			case CSS:
				this.css.append(str.toString());
				this.css.append('\n');
				break;

			case FXMLPDFSTAGE:
				this.fxmlPDFStage.append(str.toString());
				this.fxmlPDFStage.append('\n');

				break;

			case KEEP_GLYFS_SEPARATE:

				try {
					this.writeEveryGlyf = (Boolean) str;
				}
				catch (Exception e) {
					// tell user and log
					if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
				}
				break;

			case SET_ENCODING_USED:

				try {
					this.encodingType = ((String[]) str);
				}
				catch (Exception e) {
					// tell user and log
					if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
				}
				break;

			case JSIMAGESPECIAL:
				if (!this.jsImagesAdded) {
					writeCustom(TOP_SECTION, str);
					this.jsImagesAdded = true;
				}
				break;

			// special case used from PdfStreamDecoder to get font data
			case SAVE_EMBEDDED_FONT:

				// save ttf font data as file
				Object[] fontData = (Object[]) str;
				PdfFont pdfFont = (PdfFont) fontData[0];
				String fontName = pdfFont.getFontName();
				String fileType = (String) fontData[2];

				// make sure Dir exists
				String fontPath = this.rootDir + this.javaFxFileName + '/';
				File cssDir = new File(fontPath);
				if (!cssDir.exists()) {
					cssDir.mkdirs();
				}

				try {
					BufferedOutputStream fontOutput = new BufferedOutputStream(new FileOutputStream(fontPath + fontName + '.' + fileType));
					fontOutput.write((byte[]) fontData[1]);
					fontOutput.flush();
					fontOutput.close();

				}
				catch (Exception e) {
					// tell user and log
					if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
				}

				// save details into CSS so we can put in HTML
				StringBuffer fontTag = new StringBuffer();
				fontTag.append("@font-face {\n"); // indent
				fontTag.append("\tfont-family: ").append(fontName).append(";\n");
				fontTag.append("\tsrc: url(\"").append(this.javaFxFileName).append('/').append(fontName).append('.').append(fileType)
						.append("\");\n");
				fontTag.append("}\n");

				writeCustom(OutputDisplay.CSS, fontTag);

				break;

			default:
				super.writeCustom(section, str);
		}
	}

	@Override
	protected void drawPatternedShape(Shape currentShape, GraphicsState gs) {

		super.drawPatternedShape(currentShape, gs);

		if (this.currentPatternedShape[0] != -1) {
			writeCustom(SCRIPT, "\n\t<ImageView layoutX=\"" + this.currentPatternedShape[0] + "\" layoutY=\"" + this.currentPatternedShape[1]
					+ "\" fitWidth=\"" + this.currentPatternedShape[2] + "\" fitHeight=\"" + this.currentPatternedShape[3] + "\">");
			writeCustom(SCRIPT, "\t<image>");
			writeCustom(SCRIPT, "\t\t<Image url=\"@" + this.currentPatternedShapeName + "\"/>");
			writeCustom(SCRIPT, "\t</image>");
			writeCustom(SCRIPT, "</ImageView>");
		}
	}

	@Override
	protected void drawNonPatternedShape(Shape currentShape, GraphicsState gs, int cmd, String name, Rectangle2D cropBox, Point2D midPoint) {

		ShapeFactory shape = new org.jpedal.render.output.javafx.FXMLShape(cmd, this.shapeCount, this.scaling, currentShape, gs,
				new AffineTransform(), midPoint, cropBox.getBounds(), this.currentColor, this.dpCount, this.pageRotation, this.pageData,
				this.pageNumber, this.includeClip);
		shape.setShapeNumber(this.shapeCount);
		this.shapeCount++;

		// Stores the content of the shapes that gets outputed in the javafx file

		if (!shape.isEmpty()) {
			writeCustom(SCRIPT, shape.getContent());

			// update current color
			this.currentColor = shape.getShapeColor();
		}
	}

	/**
	 * Draws boxes around where the text should be.
	 */
	@Override
	protected void drawTextArea() {
		if (this.currentTextBlock.isEmpty()) return;
	}

	/**
	 * Draw a debug area around border of page.
	 */
	@Override
	protected void drawPageBorder() {
		double[] coords = { this.cropBox.getX(), this.cropBox.getY() };
		writeCustom(SCRIPT, "pdf.moveTo(" + coordsToStringParam(coords, 2) + ");");
		coords[0] += this.cropBox.getWidth();
		writeCustom(SCRIPT, "pdf.lineTo(" + coordsToStringParam(coords, 2) + ");");
		coords[1] += this.cropBox.getHeight();
		writeCustom(SCRIPT, "pdf.lineTo(" + coordsToStringParam(coords, 2) + ");");
		coords[0] -= this.cropBox.getWidth();
		writeCustom(SCRIPT, "pdf.lineTo(" + coordsToStringParam(coords, 2) + ");");
		writeCustom(SCRIPT, "pdf.closePath();");
		writeCustom(SCRIPT, "pdf.strokeStyle = '" + rgbToColor(0) + "';");
		writeCustom(SCRIPT, "pdf.lineWidth = '1'");
		writeCustom(SCRIPT, "pdf.stroke();");
	}

	@Override
	protected FontMapper getFontMapper(PdfFont currentFontData) {
		return new GenericFontMapper(currentFontData.getFontName(), this.fontMode, currentFontData.isFontEmbedded);
	}

}
