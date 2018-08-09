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
 * ConvertPagesToGoogleMaps.java
 * ---------------
 */

package org.jpedal.examples.images;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;

import org.jpedal.PdfDecoder;
import org.jpedal.objects.PdfPageData;

public class ConvertPagesToGoogleMaps {

	public static void main(String[] args) {
		try {
			// General input validation/checks.
			if (args.length != 2) throw new Exception("Arguments incorrect. Arguments are \"/PDF_Location/pdf.pdf\" \"/Output_Directory/\"");

			if (!args[0].endsWith(".pdf")) throw new Exception(args[0] + " not a PDF.");

			File pdf = new File(args[0]);

			if (!pdf.exists()) throw new Exception(pdf.getAbsolutePath() + " does not exist.");

			File outputDir = new File(args[1]);

			if (!outputDir.exists()) throw new Exception(outputDir.getAbsolutePath() + " does not exist.");

			// Begin conversion.
			PdfDecoder decoder = new PdfDecoder(true);
			decoder.openPdfFile(args[0]);

			String pdfName = pdf.getName().substring(0, pdf.getName().length() - 4);

			outputDir = new File(outputDir, pdfName);
			outputDir.mkdir();

			int pageCount = decoder.getPageCount();

			for (int page = 1; page <= pageCount; page++) {

				String pageAsString = getPageAsString(page, pageCount);
				new File(outputDir.getAbsolutePath() + File.separator + pageAsString + File.separator).mkdir();

				PdfPageData pageData = decoder.getPdfPageData();

				int pdfPageWidth = pageData.getCropBoxWidth(page);
				int pdfPageHeight = pageData.getCropBoxHeight(page);

				final int tileSize = 256;
				final int minZoom = 1;
				final int maxZoom = 4;

				// Generate tiles for each zoom level.
				for (int zoomLevel = minZoom; zoomLevel <= maxZoom; zoomLevel++) {
					int numTiles = (int) Math.sqrt((int) Math.pow(4, zoomLevel));
					int pageSize = tileSize * numTiles;

					// Make the longest side our wanted length.
					float scaleBy;
					if (pdfPageWidth > pdfPageHeight) {
						scaleBy = pageSize / (float) pdfPageWidth;
					}
					else {
						scaleBy = pageSize / (float) pdfPageHeight;
					}

					// Grab page at our wanted resolution.
					BufferedImage image;

					decoder.setPageParameters(scaleBy, page);
					image = decoder.getPageAsImage(page);

					// Make square and center page.
					BufferedImage bf = new BufferedImage(pageSize, pageSize, BufferedImage.TYPE_INT_ARGB);
					Graphics2D g2 = bf.createGraphics();

					int newX = (pageSize - image.getWidth()) / 2;
					int newY = (pageSize - image.getHeight()) / 2;

					g2.drawImage(image, newX, newY, image.getWidth(), image.getHeight(), null);

					// Generate the required tiles.
					for (int yTile = 0; yTile < numTiles; yTile++) {
						int y = yTile * tileSize;

						for (int xTile = 0; xTile < numTiles; xTile++) {
							int x = xTile * tileSize;

							BufferedImage chop = bf.getSubimage(x, y, tileSize, tileSize);
							javax.imageio.ImageIO.write(chop, "png", new java.io.FileOutputStream(outputDir.getAbsolutePath() + File.separator
									+ pageAsString + File.separator + "tile_" + zoomLevel + "_" + xTile + "-" + yTile + ".png"));
						}
					}
				}

				// Generate our HTML files.
				try {
					BufferedOutputStream CSSOutput = new BufferedOutputStream(new FileOutputStream(outputDir.getAbsolutePath() + File.separator
							+ pageAsString + ".html"));

					String prevPage = (page > 1) ? getPageAsString(page - 1, pageCount) : null;
					String nextPage = (page < pageCount) ? getPageAsString(page + 1, pageCount) : null;

					CSSOutput.write(getHTML(pdfName, pageAsString, prevPage, nextPage, "" + pageCount, minZoom, maxZoom).getBytes());
					CSSOutput.flush();
					CSSOutput.close();

				}
				catch (Exception ee) {
					ee.printStackTrace();
				}

				System.out.println("Page " + page + " completed!");
			}

			decoder.closePdfFile();

			try {
				BufferedOutputStream CSSOutput = new BufferedOutputStream(new FileOutputStream(outputDir.getAbsolutePath() + File.separator
						+ "index.html"));

				String page = "<!DOCTYPE html><html><head><meta http-equiv=\"Refresh\" content=\"0; url=" + getPageAsString(1, pageCount)
						+ ".html\"></head><body></body></html>";
				CSSOutput.write(page.getBytes());
				CSSOutput.flush();
				CSSOutput.close();

			}
			catch (Exception ee) {
				ee.printStackTrace();
			}

		}
		catch (Exception e) {
			System.out.println("Failed: " + e.getMessage());
			e.printStackTrace();
		}
	}

	// Returns page number with 0's prefixed if required.
	private static String getPageAsString(int page, int pageCount) {
		int zeros = ("" + pageCount).length() - ("" + page).length();
		String pageAsString = "";
		for (int i = 0; i < zeros; i++)
			pageAsString += "0";
		pageAsString += "" + page;
		return pageAsString;
	}

	// Build the output for a page in HTML.
	private static String getHTML(String pdfName, String pageNumber, String prevPage, String nextPage, String pageCount, int minZoom, int maxZoom) {

		StringBuilder sb = new StringBuilder();

		sb.append("<!DOCTYPE html>\n");
		sb.append("<html lang=\"en\">\n");
		sb.append("<head>\n");
		sb.append("\t<meta charset=\"utf-8\" />\n");
		sb.append("\t<title>" + pdfName + " Page " + pageNumber + "</title>\n");
		sb.append("</head>\n");
		sb.append("\n");
		sb.append("<body>\n");
		sb.append("\t<div id=\"page" + pageNumber + "\" style=\"width:1050px;height:590px;margin:10px auto;border:2px solid #000;\"></div>\n");
		sb.append("\t<center>" + ((prevPage != null) ? "<a href=\"" + prevPage + ".html\" >&lt;&lt;</a>" : "&lt;&lt;") + " Page " + pageNumber
				+ " of " + pageCount + " " + ((nextPage != null) ? "<a href=\"" + nextPage + ".html\" > &gt;&gt;</a>" : "&gt;&gt;") + "</center>\n");
		sb.append("\n");
		sb.append("\t<script type=\"text/javascript\" src=\"http://maps.google.com/maps/api/js?libraries=geometry&sensor=false\"></script>\n");
		sb.append("\t<script type=\"text/javascript\">\n");
		sb.append("\t/* <![CDATA[ */\n");
		sb.append("\t\t// Google Maps Demo\n");
		sb.append("\t\tvar Demo = Demo || {};\n");
		sb.append("\t\tDemo.ImagesBaseUrl = '';\n");
		sb.append("\n");
		sb.append("\t\t//Page" + pageNumber + "\n");
		sb.append("\t\tDemo.Page" + pageNumber + " = function (container) {\n");
		sb.append("\t\t\t// Create map\n");
		sb.append("\t\t\tthis._map = new google.maps.Map(container, {\n");
		sb.append("\t\t\t\tzoom: " + minZoom + ",\n");
		sb.append("\t\t\t\tcenter: new google.maps.LatLng(0, -20),\n");
		sb.append("\t\t\t\tmapTypeControl: false,\n");
		sb.append("\t\t\t\tstreetViewControl: false\n");
		sb.append("\t\t\t});\n");
		sb.append("\n");
		sb.append("\t\t\t// Set custom tiles\n");
		sb.append("\t\t\tthis._map.mapTypes.set('" + pageNumber + "', new Demo.ImgMapType('" + pageNumber + "', '#E5E3DF'));\n");
		sb.append("\t\t\tthis._map.setMapTypeId('" + pageNumber + "');\n");
		sb.append("\t\t};\n");
		sb.append("\n");
		sb.append("\t\t// ImgMapType class\n");
		sb.append("\t\tDemo.ImgMapType = function (theme, backgroundColor) {\n");
		sb.append("\t\t\tthis.name = this._theme = theme;\n");
		sb.append("\t\t\tthis._backgroundColor = backgroundColor;\n");
		sb.append("\t\t};\n");
		sb.append("\n");
		sb.append("\t\tDemo.ImgMapType.prototype.tileSize = new google.maps.Size(256, 256);\n");
		sb.append("\t\tDemo.ImgMapType.prototype.minZoom = " + minZoom + ";\n");
		sb.append("\t\tDemo.ImgMapType.prototype.maxZoom = " + maxZoom + ";\n");
		sb.append("\n");
		sb.append("\t\tDemo.ImgMapType.prototype.getTile = function (coord, zoom, ownerDocument) {\n");
		sb.append("\t\t\tvar tilesCount = Math.pow(2, zoom);\n");
		sb.append("\n");
		sb.append("\t\t\tif (coord.x >= tilesCount || coord.x < 0 || coord.y >= tilesCount || coord.y < 0) {\n");
		sb.append("\t\t\t\tvar div = ownerDocument.createElement('div');\n");
		sb.append("\t\t\t\tdiv.style.width = this.tileSize.width + 'px';\n");
		sb.append("\t\t\t\tdiv.style.height = this.tileSize.height + 'px';\n");
		sb.append("\t\t\t\tdiv.style.backgroundColor = this._backgroundColor;\n");
		sb.append("\t\t\t\treturn div;\n");
		sb.append("\t\t\t}\n");
		sb.append("\n");
		sb.append("\t\t\tvar img = ownerDocument.createElement('IMG');\n");
		sb.append("\t\t\timg.width = this.tileSize.width;\n");
		sb.append("\t\t\timg.height = this.tileSize.height;\n");
		sb.append("\t\t\timg.src = Demo.Utils.GetImageUrl(this._theme + '/tile_' + zoom + '_' + coord.x + '-' + coord.y + '.png');\n");
		sb.append("\n");
		sb.append("\t\t\treturn img;\n");
		sb.append("\t\t};\n");
		sb.append("\n");
		sb.append("\t\t// Other\n");
		sb.append("\t\tDemo.Utils = Demo.Utils || {};\n");
		sb.append("\n");
		sb.append("\t\tDemo.Utils.GetImageUrl = function (image) {\n");
		sb.append("\t\t\treturn Demo.ImagesBaseUrl + image;\n");
		sb.append("\t\t};\n");
		sb.append("\n");
		sb.append("\t\t// Map creation\n");
		sb.append("\t\tgoogle.maps.event.addDomListener(window, 'load', function () {\n");
		sb.append("\t\t\tvar page" + pageNumber + " = new Demo.Page" + pageNumber + "(document.getElementById('page" + pageNumber + "'));\n");
		sb.append("\t\t});\n");
		sb.append("\t/* ]]> */\n");
		sb.append("\t</script>\n");
		sb.append("</body>\n");
		sb.append("</html>\n");

		return sb.toString();
	}
}
