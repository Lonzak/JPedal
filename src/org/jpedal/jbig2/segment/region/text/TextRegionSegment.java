/**
 * ===========================================
 * Java Pdf Extraction Decoding Access Library
 * ===========================================
 *
 * Project Info:  http://www.jpedal.org
 * (C) Copyright 1997-2008, IDRsolutions and Contributors.
 * Main Developer: Simon Barnett
 *
 * 	This file is part of JPedal
 *
 * Copyright (c) 2008, IDRsolutions
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the IDRsolutions nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY IDRsolutions ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL IDRsolutions BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Other JBIG2 image decoding implementations include
 * jbig2dec (http://jbig2dec.sourceforge.net/)
 * xpdf (http://www.foolabs.com/xpdf/)
 * 
 * The final draft JBIG2 specification can be found at http://www.jpeg.org/public/fcd14492.pdf
 * 
 * All three of the above resources were used in the writing of this software, with methodologies,
 * processes and inspiration taken from all three.
 *
 * ---------------
 * TextRegionSegment.java
 * ---------------
 */
package org.jpedal.jbig2.segment.region.text;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.jpedal.jbig2.JBIG2Exception;
import org.jpedal.jbig2.decoders.HuffmanDecoder;
import org.jpedal.jbig2.decoders.JBIG2StreamDecoder;
import org.jpedal.jbig2.image.JBIG2Bitmap;
import org.jpedal.jbig2.segment.Segment;
import org.jpedal.jbig2.segment.pageinformation.PageInformationSegment;
import org.jpedal.jbig2.segment.region.RegionFlags;
import org.jpedal.jbig2.segment.region.RegionSegment;
import org.jpedal.jbig2.segment.symboldictionary.SymbolDictionarySegment;
import org.jpedal.jbig2.util.BinaryOperation;

public class TextRegionSegment extends RegionSegment {
	private TextRegionFlags textRegionFlags = new TextRegionFlags();

	private TextRegionHuffmanFlags textRegionHuffmanFlags = new TextRegionHuffmanFlags();

	private boolean inlineImage;

	private short[] symbolRegionAdaptiveTemplateX = new short[2], symbolRegionAdaptiveTemplateY = new short[2];

	public TextRegionSegment(JBIG2StreamDecoder streamDecoder, boolean inlineImage) {
		super(streamDecoder);

		this.inlineImage = inlineImage;
	}

	@Override
	public void readSegment() throws IOException, JBIG2Exception {
		if (JBIG2StreamDecoder.debug) System.out.println("==== Reading Text Region ====");

		super.readSegment();

		/** read text region Segment flags */
		readTextRegionFlags();

		short[] buff = new short[4];
		this.decoder.readByte(buff);
		int noOfSymbolInstances = BinaryOperation.getInt32(buff);

		if (JBIG2StreamDecoder.debug) System.out.println("noOfSymbolInstances = " + noOfSymbolInstances);

		int noOfReferredToSegments = this.segmentHeader.getReferredToSegmentCount();
		int[] referredToSegments = this.segmentHeader.getReferredToSegments();

		// List codeTables = new ArrayList();
		List<Segment> segmentsReferenced = new LinkedList<Segment>();
		int noOfSymbols = 0;

		if (JBIG2StreamDecoder.debug) System.out.println("noOfReferredToSegments = " + noOfReferredToSegments);

		for (int i = 0; i < noOfReferredToSegments; i++) {
			Segment seg = this.decoder.findSegment(referredToSegments[i]);
			int type = seg.getSegmentHeader().getSegmentType();

			if (type == Segment.SYMBOL_DICTIONARY) {
				segmentsReferenced.add(seg);
				noOfSymbols += ((SymbolDictionarySegment) seg).getNoOfExportedSymbols();
			}
			else
				if (type == Segment.TABLES) {
					// codeTables.add(seg);
				}
		}

		int symbolCodeLength = 0;
		int count = 1;

		while (count < noOfSymbols) {
			symbolCodeLength++;
			count <<= 1;
		}

		int currentSymbol = 0;
		JBIG2Bitmap[] symbols = new JBIG2Bitmap[noOfSymbols];
		for (Iterator<Segment> it = segmentsReferenced.iterator(); it.hasNext();) {
			Segment seg = it.next();
			if (seg.getSegmentHeader().getSegmentType() == Segment.SYMBOL_DICTIONARY) {
				JBIG2Bitmap[] bitmaps = ((SymbolDictionarySegment) seg).getBitmaps();
				for (int j = 0; j < bitmaps.length; j++) {
					symbols[currentSymbol] = bitmaps[j];
					currentSymbol++;
				}
			}
		}

		int[][] huffmanFSTable = null;
		int[][] huffmanDSTable = null;
		int[][] huffmanDTTable = null;

		int[][] huffmanRDWTable = null;
		int[][] huffmanRDHTable = null;

		int[][] huffmanRDXTable = null;
		int[][] huffmanRDYTable = null;
		int[][] huffmanRSizeTable = null;

		boolean sbHuffman = this.textRegionFlags.getFlagValue(TextRegionFlags.SB_HUFF) != 0;

		int i = 0;
		if (sbHuffman) {
			int sbHuffFS = this.textRegionHuffmanFlags.getFlagValue(TextRegionHuffmanFlags.SB_HUFF_FS);
			if (sbHuffFS == 0) {
				huffmanFSTable = HuffmanDecoder.huffmanTableF;
			}
			else
				if (sbHuffFS == 1) {
					huffmanFSTable = HuffmanDecoder.huffmanTableG;
				}
				else {

				}

			int sbHuffDS = this.textRegionHuffmanFlags.getFlagValue(TextRegionHuffmanFlags.SB_HUFF_DS);
			if (sbHuffDS == 0) {
				huffmanDSTable = HuffmanDecoder.huffmanTableH;
			}
			else
				if (sbHuffDS == 1) {
					huffmanDSTable = HuffmanDecoder.huffmanTableI;
				}
				else
					if (sbHuffDS == 2) {
						huffmanDSTable = HuffmanDecoder.huffmanTableJ;
					}
					else {

					}

			int sbHuffDT = this.textRegionHuffmanFlags.getFlagValue(TextRegionHuffmanFlags.SB_HUFF_DT);
			if (sbHuffDT == 0) {
				huffmanDTTable = HuffmanDecoder.huffmanTableK;
			}
			else
				if (sbHuffDT == 1) {
					huffmanDTTable = HuffmanDecoder.huffmanTableL;
				}
				else
					if (sbHuffDT == 2) {
						huffmanDTTable = HuffmanDecoder.huffmanTableM;
					}
					else {

					}

			int sbHuffRDW = this.textRegionHuffmanFlags.getFlagValue(TextRegionHuffmanFlags.SB_HUFF_RDW);
			if (sbHuffRDW == 0) {
				huffmanRDWTable = HuffmanDecoder.huffmanTableN;
			}
			else
				if (sbHuffRDW == 1) {
					huffmanRDWTable = HuffmanDecoder.huffmanTableO;
				}
				else {

				}

			int sbHuffRDH = this.textRegionHuffmanFlags.getFlagValue(TextRegionHuffmanFlags.SB_HUFF_RDH);
			if (sbHuffRDH == 0) {
				huffmanRDHTable = HuffmanDecoder.huffmanTableN;
			}
			else
				if (sbHuffRDH == 1) {
					huffmanRDHTable = HuffmanDecoder.huffmanTableO;
				}
				else {

				}

			int sbHuffRDX = this.textRegionHuffmanFlags.getFlagValue(TextRegionHuffmanFlags.SB_HUFF_RDX);
			if (sbHuffRDX == 0) {
				huffmanRDXTable = HuffmanDecoder.huffmanTableN;
			}
			else
				if (sbHuffRDX == 1) {
					huffmanRDXTable = HuffmanDecoder.huffmanTableO;
				}
				else {

				}

			int sbHuffRDY = this.textRegionHuffmanFlags.getFlagValue(TextRegionHuffmanFlags.SB_HUFF_RDY);
			if (sbHuffRDY == 0) {
				huffmanRDYTable = HuffmanDecoder.huffmanTableN;
			}
			else
				if (sbHuffRDY == 1) {
					huffmanRDYTable = HuffmanDecoder.huffmanTableO;
				}
				else {

				}

			int sbHuffRSize = this.textRegionHuffmanFlags.getFlagValue(TextRegionHuffmanFlags.SB_HUFF_RSIZE);
			if (sbHuffRSize == 0) {
				huffmanRSizeTable = HuffmanDecoder.huffmanTableA;
			}
			else {

			}
		}

		int[][] runLengthTable = new int[36][4];
		int[][] symbolCodeTable = new int[noOfSymbols + 1][4];
		if (sbHuffman) {

			this.decoder.consumeRemainingBits();

			for (i = 0; i < 32; i++) {
				runLengthTable[i] = new int[] { i, this.decoder.readBits(4), 0, 0 };
			}

			runLengthTable[32] = new int[] { 0x103, this.decoder.readBits(4), 2, 0 };

			runLengthTable[33] = new int[] { 0x203, this.decoder.readBits(4), 3, 0 };

			runLengthTable[34] = new int[] { 0x20b, this.decoder.readBits(4), 7, 0 };

			runLengthTable[35] = new int[] { 0, 0, HuffmanDecoder.jbig2HuffmanEOT };

			runLengthTable = HuffmanDecoder.buildTable(runLengthTable, 35);

			for (i = 0; i < noOfSymbols; i++) {
				symbolCodeTable[i] = new int[] { i, 0, 0, 0 };
			}

			i = 0;
			while (i < noOfSymbols) {
				int j = this.huffmanDecoder.decodeInt(runLengthTable).intResult();
				if (j > 0x200) {
					for (j -= 0x200; j != 0 && i < noOfSymbols; j--) {
						symbolCodeTable[i++][1] = 0;
					}
				}
				else
					if (j > 0x100) {
						for (j -= 0x100; j != 0 && i < noOfSymbols; j--) {
							symbolCodeTable[i][1] = symbolCodeTable[i - 1][1];
							i++;
						}
					}
					else {
						symbolCodeTable[i++][1] = j;
					}
			}

			symbolCodeTable[noOfSymbols][1] = 0;
			symbolCodeTable[noOfSymbols][2] = HuffmanDecoder.jbig2HuffmanEOT;
			symbolCodeTable = HuffmanDecoder.buildTable(symbolCodeTable, noOfSymbols);

			this.decoder.consumeRemainingBits();
		}
		else {
			symbolCodeTable = null;
			this.arithmeticDecoder.resetIntStats(symbolCodeLength);
			this.arithmeticDecoder.start();
		}

		boolean symbolRefine = this.textRegionFlags.getFlagValue(TextRegionFlags.SB_REFINE) != 0;
		int logStrips = this.textRegionFlags.getFlagValue(TextRegionFlags.LOG_SB_STRIPES);
		int defaultPixel = this.textRegionFlags.getFlagValue(TextRegionFlags.SB_DEF_PIXEL);
		int combinationOperator = this.textRegionFlags.getFlagValue(TextRegionFlags.SB_COMB_OP);
		boolean transposed = this.textRegionFlags.getFlagValue(TextRegionFlags.TRANSPOSED) != 0;
		int referenceCorner = this.textRegionFlags.getFlagValue(TextRegionFlags.REF_CORNER);
		int sOffset = this.textRegionFlags.getFlagValue(TextRegionFlags.SB_DS_OFFSET);
		int template = this.textRegionFlags.getFlagValue(TextRegionFlags.SB_R_TEMPLATE);

		if (symbolRefine) {
			this.arithmeticDecoder.resetRefinementStats(template, null);
		}

		JBIG2Bitmap bitmap = new JBIG2Bitmap(this.regionBitmapWidth, this.regionBitmapHeight, this.arithmeticDecoder, this.huffmanDecoder,
				this.mmrDecoder);

		bitmap.readTextRegion(sbHuffman, symbolRefine, noOfSymbolInstances, logStrips, noOfSymbols, symbolCodeTable, symbolCodeLength, symbols,
				defaultPixel, combinationOperator, transposed, referenceCorner, sOffset, huffmanFSTable, huffmanDSTable, huffmanDTTable,
				huffmanRDWTable, huffmanRDHTable, huffmanRDXTable, huffmanRDYTable, huffmanRSizeTable, template, this.symbolRegionAdaptiveTemplateX,
				this.symbolRegionAdaptiveTemplateY, this.decoder);

		if (this.inlineImage) {
			PageInformationSegment pageSegment = this.decoder.findPageSegement(this.segmentHeader.getPageAssociation());
			JBIG2Bitmap pageBitmap = pageSegment.getPageBitmap();

			if (JBIG2StreamDecoder.debug) System.out.println(pageBitmap + " " + bitmap);

			int externalCombinationOperator = this.regionFlags.getFlagValue(RegionFlags.EXTERNAL_COMBINATION_OPERATOR);
			pageBitmap.combine(bitmap, this.regionBitmapXLocation, this.regionBitmapYLocation, externalCombinationOperator);
		}
		else {
			bitmap.setBitmapNumber(getSegmentHeader().getSegmentNumber());
			this.decoder.appendBitmap(bitmap);
		}

		this.decoder.consumeRemainingBits();
	}

	private void readTextRegionFlags() throws IOException {
		/** extract text region Segment flags */
		short[] textRegionFlagsField = new short[2];
		this.decoder.readByte(textRegionFlagsField);

		int flags = BinaryOperation.getInt16(textRegionFlagsField);
		this.textRegionFlags.setFlags(flags);

		if (JBIG2StreamDecoder.debug) System.out.println("text region Segment flags = " + flags);

		boolean sbHuff = this.textRegionFlags.getFlagValue(TextRegionFlags.SB_HUFF) != 0;
		if (sbHuff) {
			/** extract text region Segment Huffman flags */
			short[] textRegionHuffmanFlagsField = new short[2];
			this.decoder.readByte(textRegionHuffmanFlagsField);

			flags = BinaryOperation.getInt16(textRegionHuffmanFlagsField);
			this.textRegionHuffmanFlags.setFlags(flags);

			if (JBIG2StreamDecoder.debug) System.out.println("text region segment Huffman flags = " + flags);
		}

		boolean sbRefine = this.textRegionFlags.getFlagValue(TextRegionFlags.SB_REFINE) != 0;
		int sbrTemplate = this.textRegionFlags.getFlagValue(TextRegionFlags.SB_R_TEMPLATE);
		if (sbRefine && sbrTemplate == 0) {
			this.symbolRegionAdaptiveTemplateX[0] = readATValue();
			this.symbolRegionAdaptiveTemplateY[0] = readATValue();
			this.symbolRegionAdaptiveTemplateX[1] = readATValue();
			this.symbolRegionAdaptiveTemplateY[1] = readATValue();
		}
	}

	public TextRegionFlags getTextRegionFlags() {
		return this.textRegionFlags;
	}

	public TextRegionHuffmanFlags getTextRegionHuffmanFlags() {
		return this.textRegionHuffmanFlags;
	}
}
