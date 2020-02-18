/**
 * @return (HtmlElement): the element representing the server name in the top right corner.
 */
function getServerElement() {
	return document.getElementsByClassName("name-3YKhmS")[0];
}

/**
 * @return (string): the name of the server. May be altered by other scripts.
 */
function getServer() {
	return document.getElementsByClassName("name-3YKhmS")[0].innerHTML;
}

/**
 * @return (Collection<HtmlElement>): a collection of elements representing the channels currently in view.
 */
function getChannelElements() {
	return document.getElementsByClassName("name-3_Dsmg");
}

/**
 * @return (HtmlElement): the element representing the server name at the top of the display.
 */
function getChannelTitleElement() {
	return document.getElementsByClassName("title-29uC1r base-1x0h_U size16-1P40sf")[0];
}

/**
 * @return (HtmlElement): the element representing the placeholder for typed text in the text msg box.
 */
function getPlaceholderMessage() {
	return document.getElementsByClassName("placeholder-37qJjk fontSize15Padding-2bMrCq")[0];
}