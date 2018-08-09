/* 
 * Adobe App Object
 * By Lyndon Armitage
 */


function app() {
    
    this.activeDocs = new Array();
    this.calclate = true;
    this.constants = null;
    this.focusRect = false;
    this.formsVersion = new Number();
    this.fromPDFConverters = new Array();
    this.fs = null;
    this.fullscreen = false;
    this.language = "ENU";
    this.media = null;
    this.monitors = null;
    this.numPlugIns = new Number();
    this.openInPlace = false;
    this.platform = "WIN";
    this.plugIns = new Array();
    this.printColorProfiles = new Array();
    this.printerNames = new Array();
    this.runtimeHighlight = false;
    this.runtimeHighlightColor = null;
    this.thermometer = null;
    this.toolbar = false;
    this.toolbarHorizontal = false;
    this.toolbarVertical = false;
    this.viewerType = "Reader";
    this.viewerVariation = new Number();
    this.viewerVersion = new Number();
    
    this.addMenuItem = function (cName, cUser, cParent, nPos, cExec, cEnable, cMarked, bPrepend) {
        
    };
    this.addSubMenu = function (cName, cUser, cParent, nPos) {
        
    };
    this.addToolButton = function (cName, oIcon, cExec, cEnable, cMarked, cTooltext, nPos, cLabel) {
        
    };
    this.alert = function (cMsg, nIcon, nType, cTitle, oDoc, oCheckbox) {
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
    this.beep = function (nType) {
        
    }
    this.beginPriv = function () {
        
    }
    this.browseForDoc = function (bSave, cFilenameInit, cFSInit) {
        
    }
    this.clearInterval = function (oInterval) {
        
    }
    this.clearTimeOut = function (oTime) {
        
    }
    this.endPriv = function () {
        
    }
    this.execDialog = function (monitor, inheritDialog, parentDoc) {
        
    }
    this.execMenuItem = function (cMenuItem, oDoc) {
        
    }
    this.getNthPlugInName = function (nIndex) {
        
    }
    this.getPath = function (cCategory, cFolder) {
        
    }
    this.goBack = function () {
        
    }
    this.goForward = function () {
        
    }
    this.hideMenuItem = function (cName) {
        
    }
    this.hideToolbarButton = function (cName) {
        
    }
    this.launchURL = function (cURL, bNewFrame) {
        
    }
    this.listMenuItems = function (cName, oChildren) {
        
    }
    this.listToolbarButtons = function () {
        
    }
    this.mailGetAddrs = function (cTo, cCc, cBcc, cCaption, bCc, bBcc) {
        
    }
    this.mailMsg = function (bUI, cTo, cCc, cBcc, cSubject, cMsg) {
        
    }
    this.newDoc = function (nWidth, nHeight) {
        
    }
    this.newFDF = function () {
        
    }
    this.openDoc = function (cPath, oDoc, cFS, bHidden, bUseConv, cDest) {
        
    }
    this.openFDF = function (cDIPath) {
        
    }
    this.popUpMenu = function (cItem, Array) {
        
    }
    this.popUpMenuEx = function (menuItem) {
        
    }
    this.removeToolButton = function (cName) {
        
    }
    this.response = function (cQuestion, cTitle, cDefault, bPassword, cLabel) {
        
    }
    this.setInterval = function (cExpr, nMilliseconds) {
        
    }
    this.setTimeOut = function (cExpr, nMilliseconds) {
        
    }
    this.trustedFunction = function (oFunc) {
        
    }
    this.trustPropagatorFunction = function (oFunc) {
        
    }
}