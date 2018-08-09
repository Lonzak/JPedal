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
 * PS2OTFFontWriter.java
 * ---------------
 */
package org.jpedal.fonts.tt.conversion;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.jpedal.fonts.PdfFont;
import org.jpedal.fonts.TrueType;
import org.jpedal.fonts.Type1C;
import org.jpedal.fonts.glyph.PdfGlyph;
import org.jpedal.fonts.glyph.PdfJavaGlyphs;
import org.jpedal.fonts.glyph.T1Glyphs;
import org.jpedal.fonts.tt.FontFile2;
import org.jpedal.fonts.tt.Hhea;
import org.jpedal.fonts.tt.Hmtx;
import org.jpedal.fonts.tt.TTGlyphs;
import org.jpedal.utils.LogWriter;

public class PS2OTFFontWriter extends FontWriter {

	private static final long serialVersionUID = 3332020682301464589L;

	byte[] cff;

	byte[] cmap = null;

	PdfFont pdfFont, originalFont;
	PdfJavaGlyphs glyphs;

	int minCharCode;
	int maxCharCode;

	// Glyph Metrics
	private int xAvgCharWidth = 0;
	private double xMaxExtent = Double.MIN_VALUE;
	private double minRightSideBearing = Double.MAX_VALUE;
	private double minLeftSideBearing = Double.MAX_VALUE;
	private double advanceWidthMax = Double.MIN_VALUE;
	private double lowestDescender = -1;
	private double highestAscender = 1;
	private float[] fontBBox = new float[4];
	private double emSquareSize = 1000;
	private int[] advanceWidths;
	private int[] lsbs = null;
	private HashMap<String, Integer> widthMap;
	private String[] glyphList = null;

	FontFile2 orginTTTables = null;

	public PS2OTFFontWriter(PdfFont originalFont, byte[] rawFontData, String fileType, HashMap<String, Integer> widths) throws Exception {

		boolean is1C = fileType.equals("cff");

		this.name = originalFont.getBaseFontName();

		this.widthMap = widths;

		// adjust for TT or Postscript
		String[] tablesUsed = new String[] { "CFF ",
				// "FFTM",
				// "GDEF",
				"OS/2", "cmap", "head", "hhea", "hmtx", "maxp", "name", "post" };

		if (fileType.equals("ttf")) {

			this.subType = TTF;

			// read the data into our T1/t1c object so we can then parse
			this.glyphs = originalFont.getGlyphData();
			this.pdfFont = new TrueType(rawFontData, this.glyphs);

			this.orginTTTables = new FontFile2(rawFontData);

			// order for writing out tables
			// (header if different order) so we also need to translate
			// tablesUsed=new String[]{"head","hhea","maxp",
			// "OS/2",
			// "hmtx",
			// "cmap",
			// "loca",
			// "glyf",
			// "name",
			// "post"
			// };

			// new woff table order
			tablesUsed = new String[] { "OS/2", "cmap", "cvt ", "fpgm", "glyf", "head", "hhea", "hmtx", "loca", "maxp", "name", "post", "prep"

			};

			for (int i = 0; i < tablesUsed.length; i++) {
				this.IDtoTable.put(tablesUsed[i], i);
			}

		}
		else {
			/**
			 * for the moment we reread with diff paramters to extract other data
			 */
			// read the data into our T1/t1c object so we can then parse
			this.glyphs = new T1Glyphs(false, is1C);

			this.pdfFont = new Type1C(rawFontData, this.glyphs, is1C);

		}

		this.glyphCount = this.glyphs.getGlyphCount();

		this.originalFont = originalFont;
		this.cff = rawFontData;

		this.tableList = new ArrayList<String>();

		this.numTables = tablesUsed.length;

		int floor = 1;
		while (floor * 2 <= this.numTables)
			floor *= 2;

		this.searchRange = floor * 16;

		this.entrySelector = 0;
		while (Math.pow(2, this.entrySelector) < floor)
			this.entrySelector++;

		this.rangeShift = (this.numTables * 16) - this.searchRange;

		this.tableList.addAll(Arrays.asList(tablesUsed).subList(0, this.numTables));

		// location of tables
		this.checksums = new int[this.tableCount][1];
		this.tables = new int[this.tableCount][1];
		this.tableLength = new int[this.tableCount][1];

		this.type = OPENTYPE;

		// @sam - used to hack in font
		// if(name.contains("KTBBOD+HermesFB-Bold")){
		// // if(name.contains("ZapfEchos")){
		//
		// try{
		// File file=new File("C:/Users/Sam/Downloads/HermesFB-Regular.otf");
		// // File file=new File("C:/Users/Sam/Downloads/zapfechosWorks.otf");
		// int len=(int)file.length();
		// byte[] data=new byte[len];
		// FileInputStream fis=new FileInputStream(file);
		// fis.read(data);
		// fis.close();
		// FontFile2 f=new FontFile2(data,false);
		//
		//
		// for(int l=0;l<numTables;l++){
		//
		// String tag= (String) tableList.get(l);
		//
		// //read table value (including for head which is done differently at end)
		// int id= getTableID(tag);
		//
		// if(id!=-1){
		// this.tables[id][currentFontID]=f.getTableStart(id);
		// }
		// }
		//
		// }
		// }
	}

	@Override
	void readTables() {

		// Fetch advance widths
		int totalWidth = 0;
		this.advanceWidths = new int[this.glyphCount];
		if (this.widthMap != null) {
			for (int i = 0; i < this.glyphCount; i++) {
				Integer w;
				if (this.pdfFont.isCIDFont()) {
					w = this.widthMap.get(this.glyphs.getCharGlyph(i + 1));
				}
				else {
					w = this.widthMap.get(this.glyphs.getIndexForCharString(i + 1));
				}

				if (w != null) {
					this.advanceWidths[i] = w;
				}
				else
					if (this.pdfFont.is1C()) {
						int fd = 0;
						if (((Type1C) this.pdfFont).getFDSelect() != null) {
							fd = ((Type1C) this.pdfFont).getFDSelect()[i];
						}
						Integer num = this.widthMap.get("JPedalDefaultWidth" + fd);
						if (num != null) {
							this.advanceWidths[i] = num;
						}
					}
				this.advanceWidthMax = this.advanceWidthMax > this.advanceWidths[i] ? this.advanceWidthMax : this.advanceWidths[i];
				totalWidth += this.advanceWidths[i];
			}
		}

		// Store average width
		if (this.glyphCount > 0) this.xAvgCharWidth = (int) ((double) totalWidth / (double) this.glyphCount);

		// Collect glyph metrics
		double maxX = Double.MIN_VALUE;

		int iterationCount = this.glyphCount;
		if (this.originalFont.getCIDToGIDMap() != null) {
			if (this.originalFont.getCIDToGIDMap().length < iterationCount) {
				iterationCount = this.originalFont.getCIDToGIDMap().length;
			}
		}

		for (int i = 0; i < iterationCount && i < 256; i++) {
			PdfGlyph glyph = this.glyphs.getEmbeddedGlyph(new org.jpedal.fonts.glyph.T1GlyphFactory(), this.pdfFont.getMappedChar(i, false),
					new float[][] { { 1, 0 }, { 0, 1 } }, i, this.pdfFont.getGlyphValue(i), this.pdfFont.getWidth(i),
					this.pdfFont.getMappedChar(i, false));

			if (glyph != null && glyph.getShape() != null) {

				Rectangle2D area = glyph.getShape().getBounds2D();

				double lsb = area.getMinX();
				double rsb = this.advanceWidths[i] - area.getMaxX();
				double extent = area.getMinX() + area.getWidth();

				this.minLeftSideBearing = this.minLeftSideBearing < lsb ? this.minLeftSideBearing : lsb;
				this.minRightSideBearing = this.minRightSideBearing < rsb ? this.minRightSideBearing : rsb;
				this.xMaxExtent = this.xMaxExtent > extent ? this.xMaxExtent : extent;
				this.lowestDescender = this.lowestDescender < area.getMinY() ? this.lowestDescender : area.getMinY();
				this.highestAscender = this.highestAscender > area.getMaxY() ? this.highestAscender : area.getMaxY();
				maxX = maxX > area.getMaxX() ? maxX : area.getMaxX();
			}
		}

		if (this.originalFont.is1C()) {
			this.fontBBox = this.pdfFont.FontBBox;
		}
		else {
			this.fontBBox = this.originalFont.getFontBounds();
		}

		this.minLeftSideBearing = this.minLeftSideBearing < this.fontBBox[0] ? this.minLeftSideBearing : this.fontBBox[0];
		this.lowestDescender = this.lowestDescender < this.fontBBox[1] ? this.lowestDescender : this.fontBBox[1];
		maxX = maxX > this.fontBBox[2] ? maxX : this.fontBBox[2];
		this.highestAscender = this.highestAscender > this.fontBBox[3] ? this.highestAscender : this.fontBBox[3];

		this.fontBBox = new float[] { (float) this.minLeftSideBearing, (float) this.lowestDescender, (float) maxX, (float) this.highestAscender };
	}

	@Override
	public byte[] getTableBytes(int tableID) {

		byte[] fontData = new byte[0];

		FontTableWriter tableWriter = null;

		switch (tableID) {

			case CFF:
				if (this.pdfFont.is1C()) {
					// fix bad commands in CFF data
					fontData = (new CFFFixer(this.cff)).getBytes();
				}
				else {
					// convert type 1 to 1c
					tableWriter = new CFFWriter(this.glyphs, this.name);

					// Fetch glyph names and metrics
					CFFWriter cffWriter = (CFFWriter) tableWriter;
					this.glyphList = cffWriter.getGlyphList();
					this.advanceWidths = cffWriter.getWidths();
					this.lsbs = cffWriter.getBearings();
					this.fontBBox = cffWriter.getBBox();
					this.emSquareSize = cffWriter.getEmSquareSize();

					this.highestAscender = this.fontBBox[3];
					this.lowestDescender = this.fontBBox[1];
					this.advanceWidthMax = 0;

					// Calculate metrics
					int totalWidth = 0;
					for (int i = 0; i < this.advanceWidths.length; i++) {
						this.advanceWidthMax = this.advanceWidthMax > this.advanceWidths[i] ? this.advanceWidthMax : this.advanceWidths[i];
						totalWidth += this.advanceWidths[i];
						this.minLeftSideBearing = this.minLeftSideBearing < this.lsbs[i] ? this.minLeftSideBearing : this.lsbs[i];
					}

					if (this.glyphCount > 0) this.xAvgCharWidth = (int) ((double) totalWidth / (double) this.glyphCount);

					// try {
					// ByteArrayOutputStream bos = new ByteArrayOutputStream();
					// BufferedReader reader = new BufferedReader(new FileReader(new File("C:\\Users\\Sam\\Desktop\\FontForgeAmerican.txt")));
					//
					// while (reader.ready()) {
					// String line = reader.readLine();
					// String first = line.split("\t")[0];
					// first = first.replaceAll("[^01]", "");
					// if (first.length() == 8) {
					// int mag = 128;
					// byte val = 0;
					// for (int i=0; i<8; i++) {
					// boolean is1 = first.charAt(i) == '1';
					// if (is1)
					// val += mag;
					//
					// mag = mag / 2;
					// }
					//
					// bos.write(val);
					// fontData = bos.toByteArray();
					// }
					// }
					// }
				}
				break;

			case HEAD:
				if (this.subType == TTF) {
					fontData = this.orginTTTables.getTableBytes(HEAD);
				}
				else {
					tableWriter = new HeadWriter(this.fontBBox);
				}

				break;

			case CMAP:
				if (this.subType == TTF) {
					// byte [] data = orginTTTables.getTableBytes(CMAP);
					// if(data.length>0){
					tableWriter = new CMAPWriter(this.name, this.pdfFont, this.originalFont, this.glyphs, this.glyphList);
					// }
					// //identify files which does not contain cmap , cidToGidCmap or ToUnicode data
					// else if(originalFont.getCIDToGIDStream()==null && originalFont.getToUnicode()==null){
					// tableWriter=new CMAPWriter(name,pdfFont,originalFont,glyphs,glyphList);
					// break;
					// }
					// else{
					// fontData=data;
					// }
				}
				else {
					// if(name.contains("BKJFHK+HermesFB-Regular")){
					tableWriter = new CMAPWriter(this.name, this.pdfFont, this.originalFont, this.glyphs, this.glyphList);
					this.minCharCode = tableWriter.getIntValue(FontTableWriter.MIN_CHAR_CODE);
					this.maxCharCode = tableWriter.getIntValue(FontTableWriter.MAX_CHAR_CODE);
					// }
				}
				break;

			case GLYF:
				fontData = this.orginTTTables.getTableBytes(GLYF);
				break;

			case HHEA:
				if (this.subType == TTF) {
					TTGlyphs ttGlyphs = (TTGlyphs) this.glyphs;
					Hmtx hmtx = (Hmtx) ttGlyphs.getTable(HMTX);
					Hhea hhea = (Hhea) ttGlyphs.getTable(FontFile2.HHEA);

					boolean changed = false;

					// If advanceWidthMax is 0 recalculate
					int localAdvanceWidthMax = hhea.getIntValue(Hhea.ADVANCEWIDTHMAX);
					if (localAdvanceWidthMax == 0) {
						for (int z = 0; z < this.glyphs.getGlyphCount(); z++) {
							int temp = (int) hmtx.getUnscaledWidth(z);
							localAdvanceWidthMax = temp > localAdvanceWidthMax ? temp : localAdvanceWidthMax;
						}

						// Mark if changed so we can regenerate
						if (localAdvanceWidthMax != hhea.getIntValue(Hhea.ADVANCEWIDTHMAX)) {
							changed = true;
						}
					}

					if (changed) {
						// if advancewidthmax <= 0 calculate value from hmtx unscaled width [see reallybad.pdf case 13246]
						tableWriter = new HheaWriter(this.glyphs, hhea.getIntValue(Hhea.XMAXEXTENT), hhea.getIntValue(Hhea.MINIMUMRIGHTSIDEBEARING),
								hhea.getIntValue(Hhea.MINIMUMRIGHTSIDEBEARING), localAdvanceWidthMax, hhea.getIntValue(Hhea.DESCENDER),
								hhea.getIntValue(Hhea.ASCENDER));
					}
					else {
						fontData = this.orginTTTables.getTableBytes(HHEA);
					}
				}
				else {
					tableWriter = new HheaWriter(this.glyphs, this.xMaxExtent, this.minRightSideBearing, this.minLeftSideBearing,
							this.advanceWidthMax, this.lowestDescender, this.highestAscender);
				}
				break;

			case HMTX:
				if (this.subType == TTF) {
					fontData = this.orginTTTables.getTableBytes(HMTX);
				}
				else {
					tableWriter = new HmtxWriter(this.glyphs, this.advanceWidths, this.lsbs);
				}
				break;

			case LOCA:
				if (this.subType == TTF) {
					fontData = this.orginTTTables.getTableBytes(LOCA);
				}
				else {
					tableWriter = new LocaWriter(this.name, this.pdfFont, this.originalFont, this.glyphs, this.glyphList);
				}

				break;

			case OS2:
				if (this.subType == TTF) {
					byte[] data = this.orginTTTables.getTableBytes(OS2);
					// check whether OS2 table exists, other wise create new
					if (data.length > 0) {
						fontData = data;
					}
					else {
						tableWriter = new OS2Writer(this.originalFont, this.glyphs, this.xAvgCharWidth, this.minCharCode, this.maxCharCode,
								this.fontBBox, this.emSquareSize);
					}
				}
				else {
					tableWriter = new OS2Writer(this.originalFont, this.glyphs, this.xAvgCharWidth, this.minCharCode, this.maxCharCode,
							this.fontBBox, this.emSquareSize);
				}
				break;

			case MAXP:
				if (this.subType == TTF) {
					fontData = this.orginTTTables.getTableBytes(MAXP);
				}
				else {
					tableWriter = new MAXPWriter(this.glyphs);
				}
				break;

			case NAME:
				if (this.subType == TTF) {
					// fontData = orginTTTables.getTableBytes(NAME);
					tableWriter = new NameWriter(this.pdfFont, this.glyphs, this.name);
				}
				else {
					tableWriter = new NameWriter(this.pdfFont, this.glyphs, this.name);
				}
				break;

			case POST:
				if (this.subType == TTF) {
					// check whether the POST table exists, other wise create new
					byte[] data = this.orginTTTables.getTableBytes(POST);
					if (data.length > 0) {
						fontData = data;
					}
					else {
						tableWriter = new PostWriter();
					}
				}
				else {
					tableWriter = new PostWriter();
				}
				break;

			// new additions START
			case PREP:
				fontData = this.orginTTTables.getTableBytes(PREP);
				// fill with zero values if no bytes found
				if (fontData.length == 0) {
					fontData = new byte[] { FontWriter.setNextUint8(0) };
				}
				break;

			case CVT:
				fontData = this.orginTTTables.getTableBytes(CVT);
				// fill with zero values if no bytes found; cvt values always has to be double bytes
				if (fontData.length == 0) {
					fontData = FontWriter.setNextUint16(0);
				}
				break;

			case FPGM:
				fontData = this.orginTTTables.getTableBytes(FPGM);
				// fill with zero values if no bytes found
				if (fontData.length == 0) {
					fontData = new byte[] { FontWriter.setNextUint8(0) };
				}
				break;
			// new additions END

			default:

				// //@sam - code to hack table from manulaly converted
				// // font (setup fro Zapf)
				// if(name.contains("BKJFHK+HermesFB-Regular")){
				// // if(name.contains("apf")){
				//
				// try {
				// File file=new File("C:/Users/Sam/Downloads/BKJFHK+HermesFB-Regular.otf");
				// // File file=new File("C:/Users/Sam/Downloads/zapfechosWorks.otf");
				// int len=(int)file.length();
				// byte[] data=new byte[len];
				// FileInputStream fis=new FileInputStream(file);
				// fis.read(data);
				// fis.close();
				// FontFile2 f=new FontFile2(data,false);
				// f.selectTable(tableID);
				// fontData=f.getTableBytes(tableID);
				// }
				// }

				break;
		}

		if (tableWriter != null) {
			try {
				fontData = tableWriter.writeTable();
			}
			catch (Exception e) {
				// tell user and log
				if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
			}
		}

		// Code to save out table
		// if (tableID == CFF && name.contains("NAAAFM"))
		// new BinaryTool(fontData,"C:/Users/sam/desktop/iab/"+name+".iab");

		return fontData;
	}
}
