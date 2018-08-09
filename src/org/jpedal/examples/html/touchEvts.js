/**
 * Created with IntelliJ IDEA.
 * User: Lyndon Armitage
 * Date: 08/02/13
 * Time: 09:10
 */

function PDFTouchEventHandlerCreator(currentPage, pageCount) {

//	console.log(currentPage + " " + pageCount);
	var maxPages = pageCount; // need to know max number of pages
	var page = currentPage; // need to know the current page number

	var enabled = false; // are the touch events enabled

	var triggeringId = null; // this variable is used to identity the triggering element
	var fingerCount = 0;
	var startX = 0;
	var startY = 0;
	var curX = 0;
	var curY = 0;
	var minLength = 72; // the shortest distance that is considered a swipe

	this.touchStart = function(event,passedName) {
		if(!enabled) return;
		fingerCount = event.touches.length; // oiginal count of fingers

		// make sure only one finger was used
		if ( event.touches.length == 1 ) {
			event.preventDefault();

			startX = event.touches[0].pageX;
			startY = event.touches[0].pageY;

			// store the element for later use
			triggeringId = passedName;
		} else {
			// cancel even as they used more than one finger
			this.touchCancel(event);
		}
	}

	this.touchMove = function(event) {
		if(!enabled) return;

		if (event.touches.length == 1) {
			event.preventDefault();
			curX = event.touches[0].pageX;
			curY = event.touches[0].pageY;
		} else {
			this.touchCancel(event);
		}
	}

	this.touchEnd = function(event) {
		if(!enabled) return;

		if (fingerCount == 1 && curX != 0) {
			event.preventDefault();

			// use the Distance Formula to determine the length of the swipe
			var swipeLength = Math.round(Math.sqrt(Math.pow(curX - startX,2) + Math.pow(curY - startY,2)));

			// if the user swiped more than the minimum length, perform the appropriate action
			if (swipeLength >= minLength) {
				var swipeAngle = caluculateAngle();
				var swipeDirection = determineSwipeDirection(swipeAngle);
				processingRoutine(swipeDirection);
				this.touchCancel(event); // reset
			} else {
				this.touchCancel(event);
			}
		} else {
			this.touchCancel(event);
		}
	}

	this.touchCancel = function(event) {
		fingerCount = 0;
		startX = 0;
		startY = 0;
		curX = 0;
		curY = 0;
		triggeringId = null;
	}

	function caluculateAngle() {
		var X = startX-curX;
		var Y = curY-startY;
		var r = Math.atan2(Y,X);
		var swipeAngle = Math.round(r * 180 / Math.PI); // get angle in degrees
		if (swipeAngle < 0) {
			// lock the angle to between 0 and 360
			swipeAngle =  360 - Math.abs(swipeAngle);
		}
		return swipeAngle;
	}

	function determineSwipeDirection(swipeAngle) {
		var swipeDirection;
		if ( (swipeAngle <= 45) && (swipeAngle >= 0) ) {
			swipeDirection = 'left';
		} else if ( (swipeAngle <= 360) && (swipeAngle >= 315) ) {
			swipeDirection = 'left';
		} else if ( (swipeAngle >= 135) && (swipeAngle <= 225) ) {
			swipeDirection = 'right';
		} else if ( (swipeAngle > 45) && (swipeAngle < 135) ) {
			swipeDirection = 'down';
		} else {
			swipeDirection = 'up';
		}
		return swipeDirection;
	}

	function getPageHref(page, count) {
		var iWithLeadingZeros = page.toString();
		var leadingZerosNeeded = count.toString().length - iWithLeadingZeros.length;

		for(var n = 0; n < leadingZerosNeeded; n ++) {
			iWithLeadingZeros = "0" + iWithLeadingZeros;
		}
		return iWithLeadingZeros;
	}

	function processingRoutine(swipeDirection) {
//		alert("GO " + swipeDirection + " " + page + "/" + maxPages);
		if ( swipeDirection == 'left' ) {
			// back
			if(page > 1) {
				var href = getPageHref(page - 1, maxPages);
				window.location.href= href + ".html";
			}
		} else if ( swipeDirection == 'right' ) {
			// forward
			if(page < maxPages) {
				var href = getPageHref(page + 1, maxPages);
				window.location.href= href + ".html";
			}
		} else if ( swipeDirection == 'up' ) {
			// REPLACE WITH YOUR ROUTINES
		} else if ( swipeDirection == 'down' ) {
			// REPLACE WITH YOUR ROUTINES
		}
	}

	/*
	 Toggles the code on and off in case you want to use default touch behaviour.
	 */
	this.toggle = function() {
		enabled = !enabled;
		return enabled;
	}

}