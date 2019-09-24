JPedal (LGPL) - Open Source PDF Viewer
======================================

To fulfill the obligations of the LGPL license and since the original source of JPedal ist not available anymore: Here is a fork of the last
official [JPedal PDF library] which integrates contributions and bug fixes to the latest available open source version 4.92.
Since the original author removed the LGPL JPedal version from sourceforge completely (https://sourceforge.net/projects/jpedal) this page may also act as a reference.

It is worth mentioning that there is a JavaFX version of JPedal called [OpenViewerFX](https://github.com/IDRSolutions/maven-OpenViewerFX-src) also published under LGPL.
The code is coming from its JPedal roots but the Swing/AWT GUI has been replaced with JavaFX.

**Update**: The OpenViewerFX was also completely removed from github. You may find the source at maven central.

If you would like to contribute you are welcome to do so - just create an issue (with an attached patch) or do a pull request and it will be integrated.
This is however no bug reporting or "please fix my bugs" place - for that head over to (https://support.idrsolutions.com/hc/en-us/requests/new) or to [Stackoverflow](http://stackoverflow.com/questions/tagged/jpedal).

Please note that for a successful maven build the local maven .settings configuration needs to be connected to a (central) maven repository.

Changes so far:
---------------
- moved from the archaic ANT build system to maven
- integrated Borisvl changes and performance improvements of the [JBIG2 library](https://github.com/Borisvl/JBIG2-Image-Decoder)
- clean up, upgraded some libraries, java version update to 1.6 ...
- improved performance of PDF to image conversion
- fixed a bug (introduced in JPedal 4.77) which caused signature images in landscape documents to disappear
- fixed a bug which caused bold text to disappear
- fixed a bug which caused strange black glyphs when using the image export
- upgraded bouncycastle to Version 1.54
- fixed huge memory leak when handling zip/pdf encoding. This first occurred when using Java8 but was there all the time. (After this patch the server didn't freeze and the memory requirement was reduced by half when converting PDF to images.)
- fixed a font bug (Type3 font in combination with WinAnsiEncoding)
- fixed a bug when extracting text for documents with bullet point lists (p11)
- fixed a bug where trying to open a file or url without extension would throw an exception
