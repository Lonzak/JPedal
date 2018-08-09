/* 
 * Adobe Field Object
 * By Lyndon Armitage
 */

function Field() {
    this.alignment = "left";
    this.borderStyle = "solid";
    this.buttonAlignX = 50;
    this.buttonAlignY = 50;
    this.buttonFitBounds = false;
    this.buttonPosition = new Number();
    this.buttonScaleHow = new Number();
    this.buttonScaleWhen = new Number();
    this.calcOrderIndex = new Number();
    this.charLimit = new Number();
    this.comb = false;
    this.commitOnSelChange = false;
    this.currentValueIndices = null;
    this.defaultStyle = null;
    this.defaultValue = new String();
    this.doNotScroll = false;
    this.doNotSpellCheck = false;
    this.delay = false;
    this.display = new Number();
    this.doc = null;
    this.editable = false;
    this.exportValues = new Array();
    this.fileSelect = false;
    this.fillColor = new Array();
    this.hidden = false;
    this.highlight = "none";
    this.lineWidth = 1;
    this.multiline = false;
    this.multipleSelection = false;
    this.name = new String();
    this.numItems = new Number(0);
    this.page = new Number();
    this.password = false;
    this.print = true;
    this.radiosInUnison = false;
    this.readonly = false;
    this.rect = new Array();
    this.required = false;
    this.richText = false;
    this.richValue = new Array();
    this.rotation = 0;
    this.strokeColor = new Array();
    this.style = new String();
    this.submitName = new String();
    this.textColor = new Array();
    this.textFont = new String();
    this.textSize = new Number();
    this.type = new String();
    this.userName = new String();
    this.value = null;
    this.valueAsString = new String();
    
    this.browseForFileToSubmit = function () {
        
    };
    this.buttonGetCaption = function (nFace) {
        
    };
    this.buttonGeticon = function (nFace) {
        
    };
    this.buttonImportIcon = function (cPath, nPage) {
        
    };
    this.buttonSetCaption = function (cCaption, nFace) {
        
    };
    this.buttonSetIcon = function (oIcon, nFace) {
        
    };
    this.checkThisBox = function (nWidget, bCheckIt) {
        
    };
    this.clearItems = function (nWidget, bIsDefaultChecked) {
        
    };
    this.deleteItemAt = function (nIdx) {
        
    };
    this.getArry = function () {
        
    };
    this.getItemAt = function (nIdx, bExportValue) {
        
    };
    this.getLock = function () {
        
    };
    this.insertItemAt = function (cName, cExport, nIdx) {
        
    };
    this.isBoxChecked = function (nWidget) {
        
    };
    this.isDefaultChecked = function (nWidget) {
        
    };
    this.setAction = function (cTrigger, cScript) {
        
    };
    this.setFocus = function () {
        
    };
    this.setItems = function (oArray) {
        
    };
    this.setLock = function (oLock) {
        
    };
    this.signatureGetModifications = function () {
        
    };
    this.signatureGetSeedValue = function () {
        
    };
    this.signatureInfo = function (oSig) {
        
    };
    this.signatureSetSeedValue = function (oSigSeedValue) {
        
    };
    this.signatureSign = function (oSig, oInfo, cDIPath, bUI, cLegalAttest) {
        
    };
    this.signatureValidate = function (oSig, bUI) {
        
    };
}

