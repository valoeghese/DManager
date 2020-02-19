var _rcs = []; // rename channel subscribers
var _mls = []; // main loop subscribers

/**
 * Subscribes a callback to the event for renaming channels.
 * @param 0 callback: the event callback
 * @return (void)
 *
 * @CallbackSchema {
 *    @param 0 id: the id of the channel, in the format servername:channelname
 *    @param 1 name: the name of the channel, as modified by previous renameChannel subscribers
 *    @return (string): the modified name of the channel
 * }
 */
function subscribeRenameChannel(callback) {
	_rcs.push(callback);
}

/**
 * Subscribes a callback to the main loop event
 * @param 0 callback: the event callback
 * @return (void)
 *
 * @CallbackSchema {
 *    @return (void)
 * }
 */
function subscribeMainLoop(callback) {
	_mls.push(callback);
}

_channelNameMap = {};

function _renameChannel(id, channel, prefix) {
	if (channel.getAttribute("dmanager_modified") == null) { // check if not flagged as already renamed
		var oldName = channel.innerHTML.substring(prefix.length); // deal with the Message # thing in case of the placeholder
		var newName = _channelNameMap[oldName];
		
		if (newName == null) { // if there is no mapped entry for channel name, add one
			newName = oldName;
			
			for (var me = 0; me < _rcs.length; ++me) { // bet you were expecting 'i', but NO! it was me :P
				newName = _rcs[me](id, newName);
			}
			
			_channelNameMap[oldName] = newName;
		}
		
		channel.innerHTML = prefix + newName;
		channel.setAttribute("dmanager_modified", "true"); // flag
	}
}

setInterval(function() {
	// rename channel event
	var channels = getChannelElements();
	var server = getServer();
	
	for (var i = 0; i < channels.length; ++i) { // rename channels in sidebar
		var id = server + ":" + channels[i].innerHTML;
		_renameChannel(id, channels[i], "");
	}

	var channelTitleElement = getChannelTitleElement();
	var currentId = server + ":" + channelTitleElement.innerHTML;
	
	_renameChannel(currentId, channelTitleElement, ""); // rename title
	_renameChannel(currentId, getPlaceholderMessage(), "Message #"); // rename placeholder
	
	// main loop event
	for (var i = 0; i < _mls.length; ++i) {
		_mls[i]();
	}
}, 3);