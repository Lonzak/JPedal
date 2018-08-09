/*
	==========================================================================
	Module: AForm.js
	==========================================================================
	Pre-canned functions to map the user interface into JavaScripts.
	==========================================================================
	The Software, including this file, is subject to the End User License
	Agreement.
	Copyright (c) 1998, Adobe Systems Incorporated, All Rights Reserved.
	==========================================================================
*/

// PDFevent object needed @Lyndon
function PDFevent() {
    this.change = new String();
    this.changeEx = null;
    this.commitKey = new Number(0);
    this.fieldFull = false;
    this.keyDown = false;
    this.modifer = false;
    this.name = new String();
    this.rc = true;
    this.richChange = null;
    this.richChangeEx = null;
    this.richValue = null;
    this.selEnd = new Number(0);
    this.selStart = new Number(0);
    this.shift = false;
    this.source = null;
    this.target = null;
    this.targetName = new String();
    this.type = new String();
    this.value = null;
    this.willCommit = false; 
    this.reset = function () {
	    this.change = new String();
	    this.changeEx = null;
	    this.commitKey = new Number(0);
	    this.fieldFull = false;
	    this.keyDown = false;
	    this.modifer = false;
	    this.name = new String();
	    this.rc = true;
	    this.richChange = null;
	    this.richChangeEx = null;
	    this.richValue = null;
	    this.selEnd = new Number(0);
	    this.selStart = new Number(0);
	    this.shift = false;
	    this.source = null;
	    this.target = null;
	    this.targetName = new String();
	    this.type = new String();
	    this.value = null;
	    this.willCommit = false; 
    };
}

var PDFevent = new PDFevent();

function app() {
    this.alert =  function (cMsg, nIcon, nType, cTitle, oDoc, oCheckbox) {
        if(nType === undefined) {
            window.alert(cMsg);
            return;
        }
        switch(nType) {
        case 0:
        	window.alert(cMsg);
        	break;
        case 1:
        case 2:
        case 3:
        	return window.confirm(cMsg);
        	break;
        }
    };
    this.beep = function (num) {
	
    };
}

function util() {
    this.crackURL = function (cURL) {
        
    };
    this.iconStreamFromIcon = function (oIcon) {
        
    };
    this.printd = function (cFormat, oDate, bXFAPicture) {
        
    };
    this.printf = function (cFormat, arguments) {
        var start = cFormat.indexOf("%");
        if(start === -1) {
            return cFormat + " " + arguments;
        }
        var gapFormat = cFormat[start+1];
        var decimalPoint = cFormat.indexOf(".");;
        var decimalCount = cFormat[decimalPoint+1];
        var num = arguments.toFixed(decimalCount);
        // support for mask/currency symbol
        if(start > 0) {
            num = cFormat.substring(0, start) + num;
        }
        return num;
    };
    this.printx = function (cFormat, cSource) {
        
    };
    this.scand = function (cFormat, cDate) {
        
    };
    this.spansToXML = function (spanObjects) {
        
    };
    this.streamFromString = function (cString, cCharSet) {
        
    };
    this.stringFromStream = function (oStream, cCharSet) {
        
    };
    this.xmlToSpans = function (string) {
        
    };
}
function Field() {
    this.hidden = false;
    this.name = new String();
    this.richText = false;
    this.richValue = new Array();
    this.value = null;
    this.valueAsString = new String();
    this.target = null;
}


var util = new util();
var app = new app();
var fields = undefined;

function getFields() {
    var array = new Array();
    var forms = document.forms;
    //console.log(forms.length);
    for(var n=0; n < forms.length; n++) {
		var form = forms[n];
		//console.log(form);
		for(var i=0;i<form.elements.length; i ++) {
		    var el = form.elements[i];
		    var field = new Field();
		    field.name = el.getAttribute("id");
		    field.value = el.getAttribute("value");
		    field.target = el;
		    //console.log(field.value);
		    array.push(field);
		}
    }
    return array;
}

function getField(field) { 
    if(fields === undefined || fields.length === 0) {
    	fields = getFields();
    }
    
    var f; // the Field object
    for(var i=0; i < fields.length; i++) {
		if(fields[i].name===field) {
		    f = fields[i];
		    break;
		}
    }
    return f;
    //return document.getElementById(field);
}

HTMLInputElement.prototype.getField = function (field) {
   return getField(field); 
};

// The following code "exports" any strings in the list into the current scope.
//var esStrsToExport =["IDS_GREATER_THAN", "IDS_GT_AND_LT", "IDS_LESS_THAN", "IDS_INVALID_MONTH",
//				   "IDS_INVALID_DATE", "IDS_INVALID_VALUE", "IDS_AM", "IDS_PM", "IDS_MONTH_INFO", 
//				   "IDS_STARTUP_CONSOLE_MSG"];
 
//for(var n = 0; n < esStrsToExport.length; n++)
//	eval(esStrsToExport[n] + " = " + app.getString("EScript", esStrsToExport[n]).toSource());
//
IDS_GREATER_THAN = "The number is not greater than:";
IDS_GT_AND_LT = "The number is not equal to:";
IDS_LESS_THAN = "The number is not less than:";
IDS_INVALID_MONTH = "That is an invalid month:";
IDS_INVALID_DATE = "That is an invalid date:";
IDS_INVALID_VALUE = "The value is not valid:";
IDS_AM = "AM";
IDS_PM = "PM";
IDS_MONTH_INFO = "";
IDS_STARTUP_CONSOLE_MSG = "Javascript started";

console.log(IDS_STARTUP_CONSOLE_MSG);

RE_NUMBER_ENTRY_DOT_SEP = new Array(
	"[+-]?\\d*\\.?\\d*"
);
RE_NUMBER_COMMIT_DOT_SEP = new Array(
	"[+-]?\\d+(\\.\\d+)?",		/* -1.0 or -1 */
	"[+-]?\\.\\d+",				/* -.1 */
	"[+-]?\\d+\\."				/* -1. */
);
RE_NUMBER_ENTRY_COMMA_SEP = new Array(
	"[+-]?\\d*,?\\d*"
);
RE_NUMBER_COMMIT_COMMA_SEP = new Array(
	"[+-]?\\d+([.,]\\d+)?",		/* -1,0 or -1 */
	"[+-]?[.,]\\d+",				/* -,1 */
	"[+-]?\\d+[.,]"				/* -1, */
);
RE_ZIP_ENTRY = new Array(
	"\\d{0,5}"
);
RE_ZIP_COMMIT = new Array(
	"\\d{5}"
);
RE_ZIP4_ENTRY = new Array(
	"\\d{0,5}(\\.|[- ])?\\d{0,4}"
);
RE_ZIP4_COMMIT = new Array(
	"\\d{5}(\\.|[- ])?\\d{4}"
);
RE_PHONE_ENTRY = new Array(
	"\\d{0,3}(\\.|[- ])?\\d{0,3}(\\.|[- ])?\\d{0,4}",		/* 555-1234 or 408 555-1234 */
	"\\(\\d{0,3}",											/* (408 */
	"\\(\\d{0,3}\\)(\\.|[- ])?\\d{0,3}(\\.|[- ])?\\d{0,4}",	/* (408) 555-1234 */
		/* (allow the addition of parens as an afterthought) */
	"\\(\\d{0,3}(\\.|[- ])?\\d{0,3}(\\.|[- ])?\\d{0,4}",	/* (408 555-1234 */
	"\\d{0,3}\\)(\\.|[- ])?\\d{0,3}(\\.|[- ])?\\d{0,4}",	/* 408) 555-1234 */
	"011(\\.|[- \\d])*"										/* international */
);
RE_PHONE_COMMIT = new Array(
	"\\d{3}(\\.|[- ])?\\d{4}",							/* 555-1234 */
	"\\d{3}(\\.|[- ])?\\d{3}(\\.|[- ])?\\d{4}",			/* 408 555-1234 */
	"\\(\\d{3}\\)(\\.|[- ])?\\d{3}(\\.|[- ])?\\d{4}",	/* (408) 555-1234 */
	"011(\\.|[- \\d])*"									/* international */
);
RE_SSN_ENTRY = new Array(
	"\\d{0,3}(\\.|[- ])?\\d{0,2}(\\.|[- ])?\\d{0,4}"
);
RE_SSN_COMMIT = new Array(
	"\\d{3}(\\.|[- ])?\\d{2}(\\.|[- ])?\\d{4}"
);

/* Function definitions for the color object. */

function ColorConvert(oColor, cColorspace)
{	// Converts a color to a specific colorspace.
	var oOut = oColor;

	switch (cColorspace) {
		case "G":
			// Note that conversion to the DeviceGray colorspace is lossy in the same
			// way that a color signal on a B/W TV is lossy.
			if (oColor[0] == "RGB")
				oOut = new Array("G", 0.3 * oColor[1] + 0.59 * oColor[2] + 0.11 * oColor[3]);
			else if (oColor[0] == "CMYK")
				oOut = new Array("G", 1.0 - Math.min(1.0, 
					0.3 * oColor[1] + 0.59 * oColor[2] + 0.11 * oColor[3] + oColor[4]));
		break;
		case "RGB":
			if (oColor[0] == "G")
				oOut = new Array("RGB", oColor[1], oColor[1], oColor[1]);
			else if (oColor[0] == "CMYK")
				oOut = new Array("RGB", 1.0 - Math.min(1.0, oColor[1] + oColor[4]), 
					1.0 - Math.min(1.0, oColor[2] + oColor[4]),
					1.0 - Math.min(1.0, oColor[3] + oColor[4]));
		break;
		case "CMYK":
			if (oColor[0] == "G")
				oOut = new Array("CMYK", 0, 0, 0, 1.0 - oColor[1]);
			else if (oColor[0] == "RGB")
				oOut = new Array("CMYK", 1.0 - oColor[1], 1.0 - oColor[2], 1.0 - oColor[3], 0); 
		break;
	}

	return oOut;
}

function ColorEqual(c1, c2)
{	// Compare two colors. 
	/* The gray colorspace conversion is lossy so we avoid if possible. */
	if (c1[0] == "G")
		c1 = color.convert(c1, c2[0]);
	else
		c2 = color.convert(c2, c1[0]);

	/* Colorspace must be equal. */
	if (c1[0] != c2[0])	{
		return false;
	}

	/* Compare the individual components. */
	var nComponents = 0;
		
	switch (c1[0]) {
		case "G":
			nComponents = 1;
		break;
		case "RGB":
			nComponents = 3;
		break;
		case "CMYK":
			nComponents = 4;
		break;
	}

	for (var i = 1; i <= nComponents; i++) {
		if (c1[i] != c2[i])	{
			return false;
		}
	}

	return true;
}

/* ==== Convenience Objects ==== */

/* Stock color definitions for ease of use. */
color = new Object();
color.equal = ColorEqual;
color.convert = ColorConvert;
color.transparent = new Array("T");
color.black = new Array("G", 0);
color.white = new Array("G", 1);
color.dkGray = new Array("G", 0.25);
color.gray = new Array("G", 0.5);
color.ltGray = new Array("G", 0.75);
color.red = new Array("RGB", 1, 0, 0);
color.green = new Array("RGB", 0, 1, 0);
color.blue = new Array("RGB", 0, 0, 1);
color.cyan = new Array("CMYK", 1, 0, 0, 0);
color.magenta = new Array("CMYK", 0, 1, 0, 0);
color.yellow = new Array("CMYK", 0, 0, 1, 0);

/* Font definitions for ease of use */
font = new Object();
font.Times = "Times-Roman";
font.TimesB = "Times-Bold";
font.TimesI = "Times-Italic";
font.TimesBI = "Times-BoldItalic";
font.Helv = "Helvetica";
font.HelvB = "Helvetica-Bold";
font.HelvI = "Helvetica-Oblique";
font.HelvBI = "Helvetica-BoldOblique";
font.Cour = "Courier";
font.CourB = "Courier-Bold";
font.CourI = "Courier-Oblique";
font.CourBI = "Courier-BoldOblique";
font.Symbol = "Symbol";
font.ZapfD = "ZapfDingbats";
font.KaGo = "HeiseiKakuGo-W5-UniJIS-UCS2-H";
font.KaMi = "HeiseiMin-W3-UniJIS-UCS2-H";

/* Border style definitions for ease of use */
border = new Object();
border.s = "solid";
border.d = "dashed";
border.b = "beveled";
border.i = "inset";
border.u = "underline";

/* Radio/Check button style definitions for ease of use */
style = new Object();
style.ch = "check";
style.cr = "cross";
style.di = "diamond";
style.ci = "circle";
style.st = "star";
style.sq = "square"; 

/* highlight modes of on a push button */
highlight = new Object();
highlight.n = "none";
highlight.i = "invert";
highlight.p = "push";
highlight.o = "outline";

/* zoom types for a document */
zoomtype = new Object();
zoomtype.none = "NoVary";
zoomtype.fitW = "FitWidth";
zoomtype.fitH = "FitHeight";
zoomtype.fitP = "FitPage";
zoomtype.fitV = "FitVisibleWidth";
zoomtype.pref = "Preferred";

/* Cursor behavior in full screen mode. */
cursor = new Object();
cursor.visible = 0;
cursor.hidden = 1;
cursor.delay = 2;

/* Transition definitions. */
trans = new Object();
trans.blindsH		= "BlindsHorizontal";
trans.blindsV		= "BlindsVertical";
trans.boxI			= "BoxIn";
trans.boxO			= "BoxOut";
trans.dissolve		= "Dissolve";
trans.glitterD		= "GlitterDown";
trans.glitterR		= "GlitterRight";
trans.glitterRD		= "GlitterRightDown";
trans.random		= "Random";
trans.replace		= "Replace";
trans.splitHI		= "SplitHorizontalIn";
trans.splitHO		= "SplitHorizontalOut";
trans.splitVI		= "SplitVerticalIn";
trans.splitVO		= "SplitVerticalOut";
trans.wipeD			= "WipeDown";
trans.wipeL			= "WipeLeft";
trans.wipeR			= "WipeRight";
trans.wipeU			= "WipeUp";

/* Icon/Text placement. */
position = new Object();
position.textOnly	= 0;
position.iconOnly	= 1;
position.iconTextV	= 2;
position.textIconV	= 3;
position.iconTextH	= 4;
position.textIconH	= 5;
position.overlay	= 6;

/* When does icon scale. */
scaleWhen = new Object();
scaleWhen.always	= 0;
scaleWhen.never		= 1;
scaleWhen.tooBig	= 2;
scaleWhen.tooSmall	= 3;

/* How does icon scale. */
scaleHow = new Object();
scaleHow.proportional	= 0;
scaleHow.anamorphic		= 1;


/* Field display. */
display = new Object();
display.visible		= 0;
display.hidden		= 1;
display.noPrint		= 2;
display.noView		= 3;

/* ==== Functions ==== */

/* these may be used a lot -- they are language independent */

AFDigitsRegExp = new RegExp();
AFDigitsRegExp.compile("\\d+");
AFPMRegExp = new RegExp();
//AFPMRegExp.compile(IDS_PM, "i");
AFAMRegExp = new RegExp();
//AFAMRegExp.compile(IDS_AM, "i");
AFTimeLongRegExp = new RegExp();
AFTimeLongRegExp.compile("\\d{1,2}:\\d{1,2}:\\d{1,2}");
AFTimeShortRegExp = new RegExp();
AFTimeShortRegExp.compile("\\d{1,2}:\\d{1,2}");

function AFBuildRegExps(array)
/* Takes an array of strings and turns it into an array of compiled regular
 * expressions -- is used for the definitions that follow */
{
	var retVal = new Array();

	retVal.length = array.length;
	for(var it = 0; it < array.length; it++)
	{
		retVal[it] = new RegExp();
		retVal[it].compile(array[it], "i");
	}
	return retVal;
}

/* these may be used a lot -- they are NOT language independent and are 
 * derived from the localizable (RE_xxx) stuff above */

AFNumberDotSepEntryRegExp = AFBuildRegExps(RE_NUMBER_ENTRY_DOT_SEP);
AFNumberDotSepCommitRegExp = AFBuildRegExps(RE_NUMBER_COMMIT_DOT_SEP);
AFNumberCommaSepEntryRegExp = AFBuildRegExps(RE_NUMBER_ENTRY_COMMA_SEP);
AFNumberCommaSepCommitRegExp = AFBuildRegExps(RE_NUMBER_COMMIT_COMMA_SEP);
AFZipEntryRegExp = AFBuildRegExps(RE_ZIP_ENTRY);
AFZipCommitRegExp = AFBuildRegExps(RE_ZIP_COMMIT);
AFZip4EntryRegExp = AFBuildRegExps(RE_ZIP4_ENTRY);
AFZip4CommitRegExp = AFBuildRegExps(RE_ZIP4_COMMIT);
AFPhoneEntryRegExp = AFBuildRegExps(RE_PHONE_ENTRY);
AFPhoneCommitRegExp = AFBuildRegExps(RE_PHONE_COMMIT);
AFSSNEntryRegExp = AFBuildRegExps(RE_SSN_ENTRY);
AFSSNCommitRegExp = AFBuildRegExps(RE_SSN_COMMIT);
//AFMonthsRegExp = AFBuildRegExps(IDS_MONTH_INFO.split(/\[\d+\]/));

function AFExactMatch(rePatterns, sString)
{	/* match a string against an array of RegExps */
	var it;

	if(!rePatterns.length && rePatterns.test(sString) && RegExp.lastMatch == sString)
		return true;
	for(it = 0; it < rePatterns.length; it++)
		if(rePatterns[it].test(sString) && RegExp.lastMatch == sString)
			return it + 1;
	return 0;
}

function AFExtractNums(string)
{	/* returns an array of numbers that it managed to extract from the given 
	 * string or null on failure */
	var nums = new Array();

	if (string.charAt(0) == '.' || string.charAt(0) == ',')
		string = "0" + string;
		 
	while(AFDigitsRegExp.test(string)) {
		nums.length++;
		nums[nums.length - 1] = RegExp.lastMatch;
		string = RegExp.rightContext;
	}
	if(nums.length >= 1) return nums;
	return null;
}

function AFMakeNumber(string)
{	/* attempts to make a number out of a string that may not use '.' as the
	 * seperator; it expects that the number is fairly well-behaved other than
	 * possibly having a non-JavaScript friendly separator */
	var type = typeof string;

	if (type == "number")
		return string;
	if (type != "string")
		return null;

	var array = AFExtractNums(string);

	if(array)
	{
		var joined = array.join(".");

		if (string.indexOf("-.") >= 0)
			joined = "0." + joined;
		return joined * (string.indexOf("-") >= 0 ? -1.0 : 1.0);
	}
	else
		return null;
}

function AFExtractRegExp(rePattern, string)
{	/* attempts to match the pattern given against the string given; on 
	 * success, returns an array containing (at index 0) the initial
	 * string with the matched text removed and (at index 1) the matched
	 * text; on failure, returns null */
	var retVal = new Array();

	if(rePattern.test(string))
	{
		retVal.length = 2;
		retVal[0] = RegExp.leftContext + RegExp.rightContext;
		retVal[1] = RegExp.lastMatch;
		return retVal;
	}
	return null;
}

function AFMakeArrayFromList(string)
{
  var type = typeof string;

  if(type == "string")
  {
 	var reSep = new RegExp();
	reSep.compile(",[ ]?");
	return string.split(reSep);
  }
  return string;
}

function AFExtractTime(string)
{	/* attempts to extract a WELL FORMED time from a string; returned 
	 * is an array in the same vein as AFExtractRegExp or null on
	 * failure. a WELL FORMED time looks like 12:23:56pm */
	
	var pm = "";
	var info;

	info = AFExtractRegExp(AFPMRegExp, string);
	if(info)
	{
		pm = info[1];
		string = info[0];
	}
	info = AFExtractRegExp(AFAMRegExp, string);
	if(info)
	{
		string = info[0];
	}
	info = AFExtractRegExp(AFTimeLongRegExp, string);
	if(info)
	{
		info[1] += pm;
		return info;
	}
	info = AFExtractRegExp(AFTimeShortRegExp, string);
	if(info)
	{
		info[1] += pm;
		return info;
	}

	return null;
}

function AFGetMonthIndex(string)
{	/* attempts to identify the given string as a month or a valid abbreviation,
	 * it expects the given string to be the valid month from the matced RegExp.
	 * returns the month index (January = 1) or zero on failure */
	var monthre = new RegExp(string + "\\[(\\d+)\\]", "i");
	var result = monthre.exec(IDS_MONTH_INFO);
	
	if(string && result) return 1.0 * result[1];
	return 0;
}

function AFMatchMonth(string)
{	/* attempts to find a valid month embedded in a string; returns the month
	 * index (January = 1) or zero on failure */

	for(var it = 0; it < AFMonthsRegExp.length; it++)
		if(AFMonthsRegExp[it].test(string))
			return AFGetMonthIndex(RegExp.lastMatch);
	return 0;
}

function AFGetMonthString(index)
{	/* returns the string corresponding to the given month or a string that
	 * is indicative of the fact that the index was invalid */
	var monthre = new RegExp("(\\w+)\\[" + index + "\\]");
	var result = monthre.exec(IDS_MONTH_INFO);

	if(result) return result[1];
	return IDS_INVALID_MONTH;
}

function AFParseTime(string, date)
{	/* attempts to parse a string containing a time; returns null on failure
	 * or a Date object on success. Time can be in ugly format. */
	var pm, am;
	var nums = AFExtractNums(string);
	if (!date)
		date = new Date();
	var hour, minutes, seconds;

	if(!string) return date;
	if(!nums) return null;
	if(nums.length < 2 || nums.length > 3) return null;
	pm = AFPMRegExp.test(string);
	am = AFAMRegExp.test(string);
	hour = new Number(nums[0]); /* force it to number */
	if(pm)
	{
		if(hour < 12) hour += 12;
	}
	else if (am)
	{
		if(hour >= 12) hour -= 12;
	}
	minutes = nums[1];
	if(nums.length == 3) seconds = nums[2];
	else seconds = 0;
	date.setHours(hour);
	date.setMinutes(minutes);
	date.setSeconds(seconds);
	if(date.getHours() != hour)
		return null;
	if(date.getMinutes() != minutes)
		return null;
	if(date.getSeconds() != seconds)
		return null;
	return date;
}

function AFDateFromYMD(nYear, nMonth, nDate)
{	/* Validates the given fields and returns a date based on them */
	var dDate = new Date();

	dDate.setFullYear(nYear, nMonth, nDate);
	if(dDate.getFullYear() != nYear)
		return null;
	if(dDate.getMonth() != nMonth)
		return null;
	if(dDate.getDate() != nDate)
		return null;
	return dDate;
}

function AFParseDateEx(cString, cOrder)
{	/* Attempts to parse a string containing some form of date; returns null
	** on failure or a Date object on success. cOrder should be the order in
	** which the date is entered (e.g. ymd, mdy, etc.). Use AFParseDateOrder to
	** generate this string from an arbitrary format string. */
	var nYear;
	var nMonth;
	var nDate;
	var nYCount;
	var dDate = new Date();

	dDate.setHours(12, 0, 0);

	/* Empty string returns current date/time. */
	if (!cString) { 
		return dDate;
	}

	nYCount = AFParseDateYCount(cOrder); /* count the number of digits for year in the selected format */
	cOrder = AFParseDateOrder(cOrder); /* make sure its in the "ymd" format */

	/* Extract any time information in the string. */
	var info = AFExtractTime(cString);
	if (info)
		cString = info[0];

	/* Break down the date into an array of numbers. */
	var aNums = AFExtractNums(cString);
	if(!aNums) 
		return null;	/* No numbers? */

	/* User supplied three numbers. */
	if (aNums.length == 3) {
		nYear = 1.0 * aNums[cOrder.indexOf("y")];
		if (nYCount > 2 && nYear < 100)
			return null; /* must enter 4 digits for the year to match with the format of the field */
		nYear = AFDateHorizon(nYear);

		dDate = AFDateFromYMD(nYear, aNums[cOrder.indexOf("m")] - 1, aNums[cOrder.indexOf("d")]);
		if (info)
			dDate = AFParseTime(info[1], dDate);
		return dDate;
	}

	/* Find text based month, if supplied. */
	nMonth = AFMatchMonth(cString);	

	/* User supplied two numbers. */
	if(aNums.length == 2) {
		if (nMonth) {
			/* Easy case, the month was text and we have two numbers. */
			if (cOrder.indexOf("y") < cOrder.indexOf("d")) {
				nYear = 1.0 * aNums[0];
				nDate = aNums[1];
			} else {
				nYear = 1.0 * aNums[1];
				nDate = aNums[0];
			}
			if (nYCount > 2 && nYear < 100)
				return null; /* must enter 4 digits for the year to match with the format of the field */
		
			nYear = AFDateHorizon(nYear);
			dDate = AFDateFromYMD(nYear, nMonth - 1, nDate);

			if (info)
				dDate = AFParseTime(info[1], dDate);
			return dDate;
		}

		/* More difficult case. We have two numbers and three slots, how
		** to allocate them? */
		if (cOrder.indexOf("y") < cOrder.indexOf("d"))	{
			/* Year comes before date and as such we allocate the two
			** numbers to the month and the year only. */
			if (cOrder.indexOf("y") < cOrder.indexOf("m")) {
				nYear = 1.0 * aNums[0];
				nMonth = aNums[1];
			} else {
				nYear = 1.0 * aNums[1];
				nMonth = aNums[0];
			}
			if (nYCount > 2 && nYear < 100)
				return null; /* must enter 4 digits for the year to match with the format of the field */
		
			nYear = AFDateHorizon(nYear);
			dDate = AFDateFromYMD(nYear, nMonth - 1, 1);
		} else {
			/* Date comes before year and so we allocate the two numbers
			** to the date and the month only. */
			nYear = dDate.getFullYear();
			if (cOrder.indexOf("d") < cOrder.indexOf("m")) {
				dDate = AFDateFromYMD(nYear, aNums[1] - 1, aNums[0]);
			} else {
				dDate = AFDateFromYMD(nYear, aNums[0] - 1, aNums[1]);
			}
		}
	
		if (info)
			dDate = AFParseTime(info[1], dDate);
		return dDate;
	}

	/* User supplied one number. */
	if(aNums.length == 1)	{
		if (nMonth) {
			/* We have one number and two slots (y/d) and need to allocate
			** them based on who came first in the format. */
			if(cOrder.indexOf("y") < cOrder.indexOf("d")) {
				nYear = 1.0 * aNums[0];
				if (nYCount > 2 && nYear < 100)
					return null; /* must enter 4 digits for the year to match with the format of the field */
			
				nYear = AFDateHorizon(nYear);
				dDate = AFDateFromYMD(nYear, nMonth - 1, 1);
			} else {
				nYear = dDate.getFullYear();
				dDate = AFDateFromYMD(nYear, nMonth - 1, aNums[0]);
			}
			if (info)
				dDate = AFParseTime(info[1], date);
			return dDate;
		}

		/* We have one number and three slots and need to allocate them
		** based on who came first in the format. */
		nYear = dDate.getFullYear();
		nMonth = dDate.getMonth();
		nDate = dDate.getDate();
		switch (cOrder.charAt(0)) {
			case "y":
				nYear = 1.0 * aNums[0];
				if (nYCount > 2 && nYear < 100)
					return null; /* must enter 4 digits for the year to match with the format of the field */
			
				nYear = AFDateHorizon(nYear);
			break;
			case "m":
				nMonth = aNums[0] - 1;
			break;
			case "d":
				nDate = aNums[0];
			break;
		}
		dDate = AFDateFromYMD(nYear, nMonth, nDate);


		if (info)
			dDate = AFParseTime(info[1], date);
		return dDate;
	}

	/* No idea how to deal with the other combinations. */
	return null;
}

function AFDateHorizon(nYear)
{	/* Takes the year supplied and applies the date horizon heuristic.
	** All years between 50 and 100 we add 1900. All years less than 50 we add 2000. */
	if (nYear < 100 && nYear >= 50) {
		nYear += 1900;
	} else if (nYear >= 0 && nYear < 50) {
		nYear += 2000;
	}

	return nYear;
}

function AFParseDate(string, longEntry, shortEntry, wordMonthEntry, monthYearEntry)
{	/* OBSOLETE: Use AFParseDateEx instead. */
	var nums;
	var year, month;
	var date;
	var info = AFExtractTime(string);

	if(!string) return new Date();

	if(info)
		string = info[0];

	date = new Date();
	nums = AFExtractNums(string);
	if(!nums) return null;
	if(nums.length == 3)
	{
		year = 1.0 * nums[eval(longEntry.charAt(0))];
		year = AFDateHorizon(year);
		date = AFDateFromYMD(year, nums[eval(longEntry.charAt(1))] - 1, nums[eval(longEntry.charAt(2))]);
		if (info)
			date = AFParseTime(info[1], date);
		return date;
	}
	month = AFMatchMonth(string);
	if(nums.length == 2)
	{
		if(month)
		{
			year = 1.0 * nums[eval(wordMonthEntry.charAt(0))];
			year = AFDateHorizon(year);
			date = AFDateFromYMD(year, month - 1, nums[eval(wordMonthEntry.charAt(1))]);
			if (info)
				date = AFParseTime(info[1], date);
			return date;
		}
		if(monthYearEntry)
		{
			year = 1.0 * nums[eval(monthYearEntry.charAt(0))];
			year = AFDateHorizon(year);
			date = AFDateFromYMD(year, nums[eval(monthYearEntry.charAt(1))] - 1, 1);
		}
		else
			date = AFDateFromYMD(date.getFullYear(),
				nums[eval(shortEntry.charAt(0))] - 1,
				nums[eval(shortEntry.charAt(1))]);
		if (info)
			date = AFParseTime(info[1], date);
		return date;
	}
	if(month && nums.length == 1)
	{
		if(monthYearEntry)
		{
			year = 1.0 * nums[0];
			year = AFDateHorizon(year);
			date = AFDateFromYMD(year, month - 1, 1);
		}
		else
			date = AFDateFromYMD(date.getFullYear(), month - 1,	nums[0]);
		if (info)
			date = AFParseTime(info[1], date);
		return date;
	}

	return null;
}

function AFParseDateWithPDF(value, pdf)
{ /* OBSOLETE: Use AFParseDateEx instead. */
	var cOldFormats = new Array(
		"m/d", "m/d/yy", "mm/dd/yy", "mm/yy", "d-mmm", "d-mmm-yy", "dd-mmm-yy",
		"yy-mm-dd", "mmm-yy", "mmmm-yy", "mmm d, yyyy", "mmmm d, yyyy",
		"m/d/yy h:MM tt", "m/d/yy HH:MM" );
   
	return AFParseDateEx(value, cOldFormats[pdf]);
}

function AFMergeChange(PDFevent)
{	/* merges the last change with the uncommitted change */
	var prefix, postfix;
	var value = PDFevent.value;

	if(PDFevent.willCommit) return PDFevent.value;
	if(PDFevent.selStart >= 0)
		prefix = value.substring(0, PDFevent.selStart);
	else prefix = "";
	if(PDFevent.selEnd >= 0 && PDFevent.selEnd <= value.length)
		postfix = value.substring(PDFevent.selEnd, value.length);
	else postfix = "";
	return prefix + PDFevent.change + postfix;
}

function AFRange_Validate(bGreaterThan, nGreaterThan, bLessThan, nLessThan)
{       /* This function validates the current PDFevent to ensure that its value is 
	** within the specified range. */
	var cError = "";

	if (PDFevent.value == "")
		return;

	if (bGreaterThan && bLessThan) {
		if (PDFevent.value < nGreaterThan || PDFevent.value > nLessThan)
			cError = util.printf(IDS_GT_AND_LT, nGreaterThan, nLessThan);
	} else if (bGreaterThan) {
		if (PDFevent.value < nGreaterThan)
			cError = util.printf(IDS_GREATER_THAN, nGreaterThan);
	} else if (bLessThan) {
		if (PDFevent.value > nLessThan)
			cError = util.printf(IDS_LESS_THAN, nLessThan);
	}
	
	if (cError != "") {
		app.alert(cError, 0);
		PDFevent.rc = false;
	}
}

function AFSimpleInit(cFunction)
{	/* Convenience function used by AFSimple_Calculate. */
	switch (cFunction)
	{
		case "PRD":
			return 1.0;
			break;
	}

	return 0.0;
}

function AFSimple(cFunction, nValue1, nValue2)
{	/* Convenience function used by AFSimple_Calculate. */
	var nValue = 1.0 * nValue1;

	/* Have to do this otherwise JavaScript thinks it's dealing with strings. */
	nValue1 = 1.0 * nValue1;
	nValue2 = 1.0 * nValue2;

	switch (cFunction)
	{
		case "AVG":
		case "SUM":
			nValue = nValue1 + nValue2;
			break;
		case "PRD":
			nValue = nValue1 * nValue2;
			break;
		case "MIN":
			nValue = Math.min(nValue1,nValue2);
			break;
		case "MAX":
			nValue = Math.max(nValue1, nValue2);
			break;
	}

	return nValue;
}

function AFSimple_Calculate(cFunction, cFields)
{   /* Calculates the sum, average, product, etc. of the listed field values. */
	var nFields = 0;
	var nValue = AFSimpleInit(cFunction);

	/* Field name separator is one or more spaces followed by a comma, 
	** followed by one or more spaces.
	** or an array of field names */
 	var aFields = AFMakeArrayFromList(cFields);

	for (var i = 0; i < aFields.length; i++) {
		/* Found a field, process it's value. */
		var f = this.getField(aFields[i]);
//		var a = f.getArray();
//
//		for (var j = 0; j < a.length; j++) {
//			var nTemp = AFMakeNumber(a[j].value); 
//			if (i == 0 && j == 0 && (cFunction == "MIN" || cFunction == "MAX"))
//				nValue = nTemp;
//			nValue = AFSimple(cFunction, nValue, nTemp);
//			nFields++;
//		}
                var nTemp = AFMakeNumber(f.value); 
                if(i == 0 && (cFunction == "MIN" || cFunction == "MAX"))
                    nValue = nTemp;
                nValue = AFSimple(cFunction, nValue, nTemp);
                nFields ++;
	}

	if (cFunction == "AVG" && nFields > 0)
		nValue /= nFields;

	PDFevent.value = nValue;
}

function AFNumber_Keystroke(nDec, sepStyle, negStyle, currStyle, strCurrency, bCurrencyPrepend)
{       /* This function validates the current keystroke PDFevent to make sure the
		key pressed is reasonable for a numeric field. */

	var value = AFMergeChange(PDFevent);
	var commit, noCommit;

	if(!value) return;
	if(sepStyle > 1)
	{
		commit = AFNumberCommaSepCommitRegExp;
		noCommit = AFNumberCommaSepEntryRegExp;
	}
	else
	{
		commit = AFNumberDotSepCommitRegExp;
		noCommit = AFNumberDotSepEntryRegExp;
	}
	if(!AFExactMatch(PDFevent.willCommit ? commit : noCommit, value))
	{
		if (PDFevent.willCommit) {
			var cAlert = IDS_INVALID_VALUE;
			if (PDFevent.target != null)
				cAlert += " [ " + PDFevent.target.name + " ]";
			app.alert(cAlert);
		}
		else
			app.beep(0);
		PDFevent.rc = false;
	}
}

function AFPercent_Keystroke(nDec, sepStyle)
{
		AFNumber_Keystroke(nDec, sepStyle, 0, 0, "", true);
}

function AFSpecial_Keystroke(psf)
{       /* This function validates the current keystroke PDFevent to make sure the
		key pressed is reasonable for a "special" field. */
		
	/* The special formats, indicated by psf, are:
	
	psf             format
	---             ------
	0               zip code
	1               zip + 4
	2               phone
	3				SSN
	
	*/

	var value = AFMergeChange(PDFevent);
	var commit, noCommit;

	if(!value) return;
	switch (psf)
	{
		case 0:
			commit = AFZipCommitRegExp;
			noCommit = AFZipEntryRegExp;
			break;
		case 1:
			commit = AFZip4CommitRegExp;
			noCommit = AFZip4EntryRegExp;
			break;
		case 2:
			commit = AFPhoneCommitRegExp;
			noCommit = AFPhoneEntryRegExp;
			break;
		case 3:
			commit = AFSSNCommitRegExp;
			noCommit = AFSSNEntryRegExp;
			break;
	}		
	if(!AFExactMatch(PDFevent.willCommit ? commit : noCommit, value))
	{
		if (PDFevent.willCommit) {
			var cAlert = IDS_INVALID_VALUE;
			if (PDFevent.target != null)
				cAlert += " [ " + PDFevent.target.name + " ]";
			app.alert(cAlert);
		}
		else
			app.beep(0);
		PDFevent.rc = false;
	}
}

function AFDate_KeystrokeEx(cFormat)
{	/* This function validates the current keystroke PDFevent to make sure the
	** key pressed is reasonable for a date field. */
	if(PDFevent.willCommit && !AFParseDateEx(AFMergeChange(PDFevent), cFormat)) {
		/* Dates are only validated on commit */
		if (PDFevent.willCommit) {
			var cAlert = IDS_INVALID_DATE;
			if (PDFevent.target != null)
				cAlert += " [ " + PDFevent.target.name + " ]";
			app.alert(cAlert);
			if (PDFevent.target != null)
				PDFevent.target.setFocus();
		}
		else
			app.beep(0);
		PDFevent.rc = false;
	}
}

function AFDate_Keystroke(pdf)
{	/* OBSOLETE: Use AFDate_KeystrokeEx. */
	var cOldFormats = new Array(
		"m/d", "m/d/yy", "mm/dd/yy", "mm/yy", "d-mmm", "d-mmm-yy", "dd-mmm-yy",
		"yy-mm-dd", "mmm-yy", "mmmm-yy", "mmm d, yyyy", "mmmm d, yyyy",
		"m/d/yy h:MM tt", "m/d/yy HH:MM" );

	AFDate_KeystrokeEx(cOldFormats[pdf]);
}

function AFTime_Keystroke(ptf)
{	/* This function validates the current keystroke PDFevent to make sure the
	key pressed is reasonable for a time field. */

	if(PDFevent.willCommit && !AFParseTime(PDFevent.value, null))
					/* times are only validated on commit */
	{
		if (PDFevent.willCommit) {
			var cAlert = IDS_INVALID_VALUE;
			if (PDFevent.target != null)
				cAlert += " [ " + PDFevent.target.name + " ]";
			app.alert(cAlert);
		}
		else
			app.beep(0);
		PDFevent.rc = false;
	}
}

function AFNumber_Format(nDec, sepStyle, negStyle, currStyle, strCurrency, bCurrencyPrepend)
{       /* This function formats a numeric value according to the parameters. */

	var value = AFMakeNumber(PDFevent.value);
	var sign = (value < 0 ? -1 : 1);
	var f = PDFevent.target;

	if(value == null)
	{
		PDFevent.value = "";
		return;
	}	
	if ((negStyle == 2 /* ParensBlack */ || negStyle == 3 /* ParensRed */) && value < 0)
		var formatStr = "(";
	else 
		var formatStr = "";
	
	if (bCurrencyPrepend)
		formatStr = formatStr + strCurrency;
		
	formatStr = formatStr + "%," + sepStyle + "." + nDec + "f";
	if (! bCurrencyPrepend)
		formatStr = formatStr + strCurrency;
		
	if ((negStyle == 2 /* ParensBlack */ || negStyle == 3 /* ParensRed */) && value < 0)
		formatStr = formatStr + ")";

	if (negStyle != 0 /* MinusBlack */ || bCurrencyPrepend)
		value = Math.abs(value);
		
	if (negStyle == 1 /* Red */ || negStyle == 3 /* ParensRed */) {
		if (sign > 0 )
			f.textColor = color.black;
		else 
			f.textColor = color.red;
	}

	var tmp = util.printf(formatStr, value);
	if (sign < 0 && bCurrencyPrepend && negStyle == 0)
		tmp = '-' + tmp; /* prepend the -ve sign */
	PDFevent.value = tmp;
}

function AFPercent_Format(nDec, sepStyle)
{       /* This function formats a percentage value according to the parameters. */

	var value = AFMakeNumber(PDFevent.value) * 100;
	
	var formatStr = "%," + sepStyle + "." + nDec + "f";
		
	if(value == null)
	{
		PDFevent.value = "";
		return;
	}	

	value = util.printf(formatStr, value);
	
	PDFevent.value = value + "%";
}

function AFSpecial_Format(psf)
{   /* This function formats a "special" value according to the "PropsSpecialFormat" parameter psf. */
	/* The special formats, indicated by psf, are: 0 = zip code, 1 = zip + 4, 2 = phone, 3 = SSN. */
	var value = PDFevent.value;

	if(!value) return;	
	switch (psf) {
	
		case 0:                         
			var formatStr = "99999";
			break;
		case 1:                         
			var formatStr = "99999-9999";
			break;
		case 2:                         /* must distinguish between 2 styles: with and without area code */
			var NumbersStr = util.printx("9999999999", value);      /* try to suck out 10 numeric chars */
			if (NumbersStr.length >= 10 )
				var formatStr = "(999) 999-9999";
			else 
				var formatStr = "999-9999";
			break;
		case 3:
			var formatStr = "999-99-9999";
			break;
	}
		
	PDFevent.value = util.printx(formatStr, value);
}

function AFParseDateYCount(cFormat)
{
	/* Determine the order of the date. */
	var yCount = 0;
	for (var i = 0; i < cFormat.length; i++) {
		switch (cFormat.charAt(i)) {
			case "\\":	/* Escape character. */
				i++;
			break;
			case "y":
				yCount += 1;
			break;
		}
	}
	return yCount;
}

function AFParseDateOrder(cFormat)
{
	/* Determine the order of the date. */
	var cOrder = "";
	for (var i = 0; i < cFormat.length; i++) {
		switch (cFormat.charAt(i)) {
			case "\\":	/* Escape character. */
				i++;
			break;
			case "m":
				if (cOrder.indexOf("m") == -1)
					cOrder += "m";
			break;
			case "d":
				if (cOrder.indexOf("d") == -1)
					cOrder += "d";
			break;
			case "y":
				if (cOrder.indexOf("y") == -1)
					cOrder += "y";
			break;
		}
	}

	/* Make sure we have a full complement of 3 chars. */
	if (cOrder.indexOf("m") == -1)
		cOrder += "m";
	if (cOrder.indexOf("d") == -1)
		cOrder += "d";
	if (cOrder.indexOf("y") == -1)
		cOrder += "y";

	return cOrder;
}

function AFDate_FormatEx(cFormat)
{	/* cFormat is a format string with which the date is to be formatted. */
	if (!PDFevent.value) 
		return;	/* Blank fields remain blank */

	var date = AFParseDateEx(PDFevent.value, cFormat);
	if (!date) {
		PDFevent.value = "";
		return;
	}
	
	PDFevent.value = util.printd(cFormat, date);
}

function AFDate_Format(pdf)
{	/* OBSOLETE: Use AFDate_FormatEx. */
	var cOldFormats = new Array(
		"m/d", "m/d/yy", "mm/dd/yy", "mm/yy", "d-mmm", "d-mmm-yy", "dd-mmm-yy",
		"yy-mm-dd", "mmm-yy", "mmmm-yy", "mmm d, yyyy", "mmmm d, yyyy",
		"m/d/yy h:MM tt", "m/d/yy HH:MM" );

	AFDate_FormatEx(cOldFormats[pdf]);
}

function AFTime_Format(ptf)
{	/* This function formats a time value according to the "PropsTimeFormat" parameter ptf.
	** The time formats, indicated by ptf, are:
	** ptf             format                                                          
	** ---             ------                                                          
	** 0               PTF_24HR_MM     [ 14:30      ]
	** 1               PTF_12HR_MM     [ 2:30 PM    ]
	** 2               PTF_24HR_MM_SS  [ 14:30:15   ]
	** 3               PTF_12HR_MM_SS  [ 2:30:15 PM ] */

	if(!PDFevent.value) return;	/* Blank fields remain blank */

	var date = new AFParseTime(PDFevent.value, null);
	if(!date) {
		PDFevent.value = "";
		return;
	}

	var cFormats = new Array(
		"HH:MM", "h:MM tt", "HH:MM:ss", "h:MM:ss tt" ); 
	
	PDFevent.value = util.printd(cFormats[ptf], date);
}

function AFSignatureLock(doc, cOperation, cFields, bLock)
{	// Locks or unlocks a set of fields according to the specified operation.
	/* Field name separator is one or more spaces followed by a comma, 
	** followed by one or more spaces.
	** or an array of field names */
 	var aFields = AFMakeArrayFromList(cFields);

	/* Three cases: ALL, EXCEPT, THESE for the field name list. */
	if (cOperation != "THESE") {
		for (var i = 0; i < doc.numFields; i++) {
			var f = doc.getField(doc.getNthFieldName(i));
				
			f.readonly = bLock;
		 }
	}
	
	if (cOperation == "EXCEPT")
		/* EXCEPT = ALL(lock) then THESE(unlock) */
		bLock = !bLock;

	if (cOperation == "THESE" || (cOperation == "EXCEPT" && !bLock)) {
		for (var i = 0; i < aFields.length; i++) {
			var f = doc.getField(aFields[i]);
			var a = f.getArray();

			for (var j = 0; j < a.length; j++) {
				a[j].readonly = bLock;
			}
		}
	}
}

function AFSignature_Format(cOperation, cFields)
{	/* This function is invoked at format time but really is used to lock fields
	** in the document. We unlock all the specified fields if the value is
	** null (which means the signature hasn't been applied). */

	var bLock = (PDFevent.value != "");

	AFSignatureLock(this, cOperation, cFields, bLock);
}
