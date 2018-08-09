/* 
 * Adobe Doc Object
 * By Lyndon Armitage
 */

function doc() {
    this.alternatePresentations = undefined;
    this.author = new String();
    this.baseURL = new String();
    this.bookmarkRoot = null;
    this.calculate = true;
    this.creationDate = null;
    this.creator = new String();
    this.dataObjects = new Array();
    this.delay = false;
    this.dirty = false;
    this.disclosed = false;
    this.docID = new Array();
    this.documentFileName = new String();
    this.dynamicXFAForm = false;
    this.external = true;
    this.filesize = new Number();
    this.hidden = false;
    this.hostContainer = null;
    this.icons = null;
    this.info = null;
    this.innerAppWindowRect = new Array();
    this.innerDocWindowRect = new Array();
    this.isModal = false;
    this.keywords = null;
    this.layout = new String();
    this.media = null;
    this.metadata = new String();
    this.modDate = null;
    this.mouseX = new Number();
    this.mouseY = new Number();
    this.noautocomplete = undefined;
    this.nocache = undefined;
    this.numFields = new Number();
    this.numPages = new Number();
    this.numTemplates = new Number();
    this.path = new String();
    this.outerAppWindowRect = new Array();
    this.outerDocWindowRect = new Array();
    this.pageNum = new Number();
    this.pageWindowRect = new Array();
    this.permStatusReady = true
    this.producer = new String();
    this.requiresFullSave = false;
    this.securityhandler = new String();
    this.selectedAnnots = new Array();
    this.sounds = new Array();
    this.spellDictionaryOrder = new Array();
    this.spellLanguageOrder = new Array();
    this.subject = new String();
    this.templates = new Array();
    this.title = new String();
    this.URL = new String();
    this.viewState = null;
    this.xfa = null;
    this.XFAForeground = false;
    this.zoom = new Number(100);
    this.zoomType = new String()
    
    function addAnnot(objectLiteral) {
        
    }
    function addField(cName, cFieldType, nPageNum, oCoords) {
        
    }
    function addIcon(cName, icon) {
        
    }
    function addLink(nPage, oCoords) {
        
    }
    function addRecipientListCryptFilter(cCryptFilter, oGroup) {
        
    }
    function addRequirement(cType, oReq) {
        
    }
    function addScript(cName, cScript) {
        
    }
    function addThumbnails(nStart, nEnd) {
        
    }
    function addWatermarkFromFile(cDIPath, nSourcePage, nStart, nEnd, bOnTop, bOnScreen, bOnPrint, nHorizAlign, nVertAlign, nHorizValue, nVertValue, bPercentage, nScale, bFixedPrint, nRotation, nOpacity) {
        
    }
    function addWatermarkFromText(cDIPath, nSourcePage, nStart, nEnd, bOnTop, bOnScreen, bOnPrint, nHorizAlign, nVertAlign, nHorizValue, nVertValue, bPercentage, nScale, bFixedPrint, nRotation, nOpacity) {
        
    }
    function addWeblinks(nStart, nEnd) {
        
    }
    function bringToFront() {
        
    }
    function calculateNow() {
        
    }
    function closeDoc(bNoSave) {
        
    }
    function colorConvertPage(pageNum, actions, inkActions) {
        
    }
    function createDataObject(cName, cValue, cMIMEType, cCryptFilter) {
        
    }
    function createTemplate(cName, nPage) {
        
    }
    function deletePages(nStart, nEnd) {
        
    }
    function deleteSound(cName) {
        
    }
    function embedDocAsDataObject(cName, oDoc, cCryptFilter, bUI) {
        
    }
    function embedOutputIntent(outputIntentColorSpace) {
        
    }
    function encryptForRecipients(oGroups, bMetaData, bUI) {
        
    }
    function encryptUsingPolicy(oPolicy, oGroups, oHandler, bUI) {
        
    }
    function exportAsFDF(bAllFields, bNoPassword, aFields, bFlags, cPath, bAnnotations) {
        
    }
    function exportAsFDFStr(bAllFields, bNoPassword, aFields, bFlags, cPath, bAnnotations, cHRef) {
        
    }
    function exportAsText(bNoPassword, aFields, cPath) {
        
    }
    function exportAsXFDF(bAllFields, bNoPassword, aFields, cPath, bAnnotations) {
        
    }
    function exportAsXFDFStr(bAllFields, bNoPassword, aFields, cPath, bAnnotations, cHRef) {
        
    }
    function exportDataObject(cName, cDIPath, bAllowAuth, nLaunch) {
        
    }
    function exportXFAData(cPath, bXDP, aPackets) {
        
    }
    function extractPages(nStart, nEnd, cPath) {
        
    }
    function flattenPages(nStart, nEnd, nNonPrint) {
        
    }
    function getAnnot(nPage, cName) {
        
    }
    function getAnnot3D(nPage, cName) {
        
    }
    function getAnnots(nPage, nSortBy, bReverse, nFilterBy) {
        
    }
    function getAnnots3D(nPage) {
        
    }
    function getColorConvertAction() {
        
    }
    function getDataObject(cName) {
        
    }
    function getDataObjectContents(bAllowAuth) {
        
    }
    function getField(cName) {
        
    }
    function getIcon(cName) {
        
    }
    function getLegalWarnings(bExecute) {
        
    }
    function getDataObject(cName) {
        
    }
    function getDataObjectContents(cName, bAllowAuth) {
        
    }
    function getLinks(nPage, oCoords) {
        
    }
    function getNthFieldName(nIndex) {
        
    }
    function getNthTemplate(nIndex) {
        
    }
    function getOCGs(nPage) {
        
    }
    function getOCGOrder() {
        
    }
    function getPageBox(cBox, nPage) {
        
    }
    function getPageLabel(nPage) {
        
    }
    function getPageNthWord(nPage, nWord, bStrip) {
        
    }
    function getPageNthWordQuads(nPage, nWord) {
        
    }
    function getPageNumWords(nPage) {
        
    }
    function getPageRotation(nPage) {
        
    }
    function getPageTransition(nPage) {
        
    }
    function getPrintParams() {
        
    }
    function getSound(cName) {
        
    }
    function getTemplate(cName) {
        
    }
    function getURL(cURL, bAppend) {
        
    }
    function gotoNamedDest(cName) {
        
    }
    function importAnFDF(cPath) {
        
    }
    function importAnXFDF(cPath) {
        
    }
    function importDataObject(cName, cDIPath, cCryptFilter) {
        
    }
    function importIcon(cName, cDIPath, nPage) {
        
    }
    function importSound(cName, cDIPath) {
        
    }
    function importTextData(cPath, nRow) {
        
    }
    function importXFAData(cPath) {
        
    }
    function insertPages(nPage, cPath, nStart, nEnd) {
        
    }
    function mailDoc(bUI, cTo, cCc, cBcc, cSubject, cMsg) {
        
    }
    function mailForm(bUI, cTo, cCc, cBcc, cSubject, cMsg) {
        
    }
    function movePage(nPage, nAfter) {
        
    }
    function newPage(nPage, nWidth, nHeight) {
        
    }
    function openDataObject(cName) {
        
    }
    function print(bUI, nStart, nEnd, bSilent, bShrinkToFit, bPrintAsImage, bReverse, bAnnotations, printParams) {
        
    }
    function removeDataObject(cName) {
        
    }
    function removeField(cName) {
        
    }
    function removeIcon(cName) {
        
    }
    function removeLinks(nPage, oCoords) {
        
    }
    function removeRequirement(cType) {
        
    }
    function removeScript(cName) {
        
    }
    function removeTemplate(cName) {
        
    }
    function removeThumbnails(nStart, nEnd) {
        
    }
    function removeWeblinks(nStart, nEnd) {
        
    }
    function replacePages(nPage, cPath, nStart, nEnd) {
        
    }
    function resetForm(aFields) {
        
    }
    function saveAs(cPath, cConvID, cFS, bCopy, bPromptToOverwrite) {
        
    }
    function scroll(nX, nY) {
        
    }
    function selectPageNthWord(nPage, nWord, bScroll) {
        
    }
    function setAction(cTrigger, cScript) {
        
    }
    function setDataObjectContents(cName, oStream, cCryptFilter) {
        
    }
    function setOCGOrder(oOrderArray) {
        
    }
    function setPageAction(nPage, cTrigger, cScript) {
        
    }
    function setPageBoxes(cBox, nStart, nEnd, rBox) {
        
    }
    function setPageLabels(nPage, aLabel) {
        
    }
    function setPageRotations(nStart, nEnd, nRotate) {
        
    }
    function setPageTabOrder(nPage, cOrder) {
        
    }
    function setPageTransitions(nStart, nEnd, aTrans) {
        
    }
    function spawnPageFromTemplate(cTemplate, nPage, bRename, bOverlay, oXObject) {
        
    }
    function submitForm(cURL, bFDF, bEmpty, aFields, bGet, bAnnotations, bXML, bIncrChanges, bPDF, bCanonical, bExclNonUserAnnots, bExclFKey, cPassword, bEmbedForm, oJavaScript, cSubmitAs, bInclNMKey, aPackets, cCharset, oXML, cPermID, cInstID, cUsageRights) {
        
    }
    function syncAnnotScan() {
        
    }
}
