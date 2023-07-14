/**
 * BungeeWeb
 * https://github.com/Dead-i/BungeeWeb
 */

// Define variables
var groups = [];
var lang = {};
var session = {};
var pages = {};
var activepage = {};

// Load handler
$(document).ready(function() {
	updateLang(function() {
		updateSession(loadClient);
	});
});

// Login handler
$('.login form').submit(function(e) {
	e.preventDefault();
	hide($('.login .error'));
	$.post('login', $(this).serialize()).done(function(data) {
		var data = parse(data);
		if (data.status == 1) {
			updateSession(function() {
				hide($('.login'), loadClient);
			});
		}else{
			$('.login .error').slideDown(200);
		}
	});
});

// Session updater
function updateSession(cb) {
	query('api/getsession', function(data) {
		session = data;
		if (data.group > 0) {
			updatePermissions();
			cb();
		}else{
			show($('.login'));
		}
	});
}

// Language updater
function updateLang(cb) {
	query('api/getlang', function(data) {
		lang = data.language;
		groups = lang.groups;
		$('[data-lang]').each(function() {
			var id = $(this).attr('data-lang');
			var split = id.split('.');
			var out = lang;
			for (i in split) {
				out = out[split[i]];	
			}

			if ($(this).hasClass('langcaps')) {
				out = out.toUpperCase();
			}

			if ($(this).hasClass('langval')) {
				$(this).val(out);
			}else if ($(this).hasClass('langholder')) {
				$(this).attr('placeholder', out);
			}else{
				$(this).prepend(out);
			}
		});
		cb();
	}, 'Your language file has incorrect JSON. Please check your JSON formatting and try again.');
}

// Permission updater
function updatePermissions() {
	$('[data-permission]').each(function() {
		if (!hasPermission($(this).attr('data-permission'))) $(this).hide();
	});
}

// Navigation handler
$('.navbar .right a, .dropdown a').click(function(e) {
	var href = $(this).attr('href');
	var link = href.substring(1);
	
	if (href.indexOf('#') != 0 && $('.client > #' + link).length == 0) return;	
	e.preventDefault();
	
	if ($(this).hasClass('active') && href != '#dropdown') return;
	if (!$('.navbar .active[href="#dropdown"]').length) $('.navbar .active').removeClass('active');
	if ($(this).parent().hasClass('right')) $(this).toggleClass('active');
	
	if (link == 'dropdown') {
		e.stopPropagation();
		var el = $('.dropdown > div');
		if (el.hasClass('active')) {
			hide(el);
		}else{
			show(el);
		}
		el.toggleClass('active');
		return;
	}
	
	window.history.pushState({}, '', href);
	hide($('.client > div.active').removeClass('active'), function() {
		show($('.client > #' + link).addClass('active'));
		if (link in pages && 'navigate' in pages[link]) {
			activepage = pages[link];
			activepage.navigate();
		}
	});
});

// Dropdown hide handler
$(document).click(function() {
	if ($('.dropdown > div').hasClass('active') && $('.navbar .active').length) {
		$('.navbar .active[href="#dropdown"]').removeClass('active');
		hide($('.dropdown > div'));
	}
});

// Logo click handler
$('.navbar h1').click(function() {
	$('.navbar a[href="/dashboard"]').click();
});

// Player link click handler
$('*').on('click', 'a.playerlink', function() {
	hideWindows();
	showPlayer($(this).attr('data-player'));
});

// Player link click handler
$('*').on('click', 'a.serverlink', function() {
	hideWindows();
	showServer($(this).attr('data-server'));
});

// Dialog escape handler
$('.mask').click(function() {
	hide($(this), function() {
		$('body').css({ 'overflow': 'visible' });
		hideWindows();
	});
});
$('.dialog').click(function(e) {
	e.stopPropagation();
});
$('.dialog .close').click(function() {
	$('.mask').click();
});

// Initial client loader
function loadClient() {
	loadTypes(function() {		
		for (page in pages) {
			if ('load' in pages[page]) {
				pages[page].load();
			}
		}
		
		if (session.transitions) {
			$('.navbar').slideDown(300);
		}else{
			$('.navbar').show();
		}

		show($('#dashboard, .footer').addClass('active'));
		var path = window.location.pathname.split('/')[1];
		if (path != '' && $('.client > #' + path).length) {
			activepage = pages[path];
			if (activepage && activepage.navigate) activepage.navigate();
			
			$('.client > div.active').hide().removeClass('active');
			show($('.client > #' + path).addClass('active'));
			
			$('.navbar .active').removeClass('active');
			var link = 'a[href="/' + path + '"]';
			$('.navbar ' + link + ', .dropdown ' + link).addClass('active');
		}else{
			activepage = pages.dashboard;
			activepage.navigate();
		}
		
		if (session.updatetime > 0) {
			var lastupdate = 0;
			setInterval(function() {
				if (activepage.update && lastupdate > 0) activepage.update(lastupdate);
				lastupdate = Math.floor(new Date().getTime() / 1000);
			}, session.updatetime * 1000);
		}
	});
}

// Types loader
var types = {};
function loadTypes(cb) {
	query('api/gettypes', function(data) {
		for (id of data) {
			if (id in lang.logs) {
				types[id] = lang.logs[id].type;	
			}else{
				types[id] = data[id];
			}
		}
		if (cb !== undefined) cb();
	});
}

function hideWindows() {
	$('#playerinfo').removeClass('active');
	$('#serverinfo').removeClass('active');
}

// Filters loader
function setFilters(el) {
	el.find('a').remove();
	for (t in types) {
		el.append('<a data-type-id="' + t + '">' + types[t] + '</a>');
	}
}

// Filter string
function getFilters(el) {
	var filter = '';
	el.find('a').each(function() {
		if ($(this).hasClass('active')) {
			filter += $(this).attr('data-type-id') + ',';
		}
	});
	return (filter == '' ? filter : filter.substring(0, filter.length - 1));
}

// Filter string
function getExtraFilters(el) {
	const filter = new Map();
	el.find('input[type="text"]').each(function() {
		if ($(this).val() !== "") {
			filter.set($(this).attr('name'), $(this).val());
		}
	});
	return filter;
}

function switchExtraFilters(parent, type, state) {
	if (!state) {
		parent.find(`.${type}-filters > div > input`).val('');
		parent.find(`.${type}-filters`).hide();
	} else {
		parent.find(`.${type}-filters`).show();
	}
}

// Permission check
function hasPermission(permission) {
	return $.inArray(permission, session.permissions) != -1;
}

async function getUUIDs(usernames) {
	promises = [];
	for (username of usernames) {
		promises.push(fetchUUID(username));
	}
	responses = await Promise.all(promises);
	return responses;
}

function fetchUUID(username) {
	return new Promise((resolve, reject) => {
		$.ajax({
			dataType: "text",
			url: `api/getuuid?username=${username}`,
			success: (res) => {
				json = parse(res);
				resolve(json !== undefined ? {name: username, uuid: json.uuid} : undefined);
			},
			error: (err) => reject(err)
		  })
	})
}

// Player dialog
function showPlayer(uuid) {
	$('body').css({ 'overflow': 'hidden' });
	$('#playerinfo').attr('data-uuid', uuid);
	show($('.mask'));
	setFilters($('#playerinfo .filters'));
	for (type of ['chat', 'command']) {
		switchExtraFilters($('#playerinfo'), type, false);
	}
	resetPlayer(uuid);
}

// Server dialog
function showServer(serverName) {
	$('body').css({ 'overflow': 'hidden' });
	$('#serverinfo').attr('data-servername', serverName);
	show($('.mask'));
	setFilters($('#serverinfo .filters'));
	for (type of ['chat', 'command']) {
		switchExtraFilters($('#serverinfo'), type, false);
	}
	resetServer(serverName);
}

// Player info retrieval
function resetPlayer(uuid) {
	addPlayerLogs(uuid, -1, getFilters($('#playerinfo .filters')), getExtraFilters($('#playerinfo .extra-filters')));
}

// Player info retrieval
function resetServer(server) {
	addServerLogs(server, -1, getFilters($('#serverinfo .filters')), getExtraFilters($('#serverinfo .extra-filters')));
}

// Player add logs
function addPlayerLogs(uuid, maxId=-1, types, extraFilters, cb) {
	var limit = 30;
	const params = new Map();
	params.set('uuids', uuid);
	params.set('maxId', maxId);
	params.set('limit', limit);
	extraFilters.forEach((v, k) => params.set(k, v));
	params.set('types', types);
	query(`api/getlogs?${new URLSearchParams(params)}`, function(data) {
		if (maxId === -1) {
			$('#playerinfo .log').html('');
			if (data.length > 0) {
				var user = data[0].session.player.name;
			$('#playerinfo h1').text(user);
			$('#playerinfo .uuid').text(uuid);
			skinview.changeSkin(user);
			}
		}

		for (log of data) {
			var d = new Date(log['time'] * 1000);
			$('#playerinfo .log').append(`<li class="entry" data-type="${log['type']}" data-id="${log['id']}">
				        <div class="left">${formatLog(log, false)}</div>
					    <div class="right fade time">${d.toLocaleString()}</div>
					    <div class="right fade server"><a class="serverlink" data-server="${log['server']}">${log['server']}</a></div>
					</li>`);
			if (log.session.player.name != user) {
				$('#playerinfo .log').append('<li>' + log.session.player.name + ' is now known as ' + user + '</li>');
				user = log.session.player.name;
			}
		}

		if (data.length == limit) $('#playerinfo .log').append('<li class="more">Show more</li>');
		if (cb !== undefined) cb();
		$('#playerinfo').addClass('active');
	});
}

// Server add logs
function addServerLogs(server, maxId=-1, types, extraFilters, cb) {
	var limit = 30;
	const params = new Map();
	params.set('servers', server);
	params.set('maxId', maxId);
	params.set('limit', limit);
	extraFilters.forEach((v, k) => params.set(k, v));
	params.set('types', types);
	query(`api/getlogs?${new URLSearchParams(params)}`, function(data) {
		if (maxId === -1) {
			$('#serverinfo .log').html('');
			$('#serverinfo h1').text(server);
		}

		for (log of data) {
			var d = new Date(data[item]['time'] * 1000);
			$('#serverinfo .log').append(`<li class="entry" data-type="${log['type']}" data-id="${log['id']}">
				        <div class="left">${formatLog(log, true)}</div>
					    <div class="right fade time">${d.toLocaleString()}</div>
					    <div class="right fade server">${log['server']}</div>
					</li>`);
		}

		if (data.length == limit) $('#serverinfo .log').append('<li class="more">Show more</li>');
		if (cb !== undefined) cb();
		$('#serverinfo').addClass('active');
	});
}

// Player logs filter
$('#playerinfo .filters').on('click', 'a', function() {
	const enabled = $(this).toggleClass('active').hasClass('active');
	const type = $(this).attr('data-type-id');
	switch (type) {
		case 'chat':
		case 'command': {
			switchExtraFilters($('#playerinfo'), type, enabled);
			break;
		}
	}
	resetPlayer($('#playerinfo').attr('data-uuid'));
});

// Player logs "show more" button handler
$('#playerinfo .log').on('click', '.more', function() {
	var more = $('#playerinfo .log .more');
	more.removeClass('more').text('Loading...');
	addPlayerLogs($('#playerinfo').attr('data-uuid'), $('#playerinfo .log li').last().attr('data-id'), getFilters($('#playerinfo .filters')), getExtraFilters($('#playerinfo .extra-filters')), function() {
		more.remove();
	});
});

// Server logs filter
$('#serverinfo .filters').on('click', 'a', function() {
	const enabled = $(this).toggleClass('active').hasClass('active');
	const type = $(this).attr('data-type-id');
	switch (type) {
		case 'chat':
		case 'command': {
			switchExtraFilters($('#serverinfo'), type, enabled);
			break;
		}
	}
	resetServer($('#serverinfo').attr('data-servername'));
});

// Server logs "show more" button handler
$('#serverinfo .log').on('click', '.more', function() {
	var more = $('#serverinfo .log .more');
	more.removeClass('more').text('Loading...');
	addServerLogs($('#serverinfo').attr('data-servername'), $('#serverinfo .log li').last().attr('data-id'), getFilters($('#serverinfo .filters')), getExtraFilters($('#serverinfo .extra-filters')), function() {
		more.remove();
	});
});

/*// Player logs search handler
$('#playerinfo .search').submit(function(e) {
	e.preventDefault();
	resetPlayer($('#playerinfo').attr('data-uuid'));
});

var searchTimer = 0;
$('#playerinfo .search input').keyup(function(e) {
	var form = $(this).closest('form');
	clearTimeout(searchTimer);
	searchTimer = setTimeout(function() {
		form.submit();
	}, 500);
});*/

// Mask scroll handler
$('.mask .logs').scroll(function() {
	if ($('#playerinfo').hasClass('active') && $('.mask .logs').scrollTop() + $('.mask .logs').height() > $('.mask .logs')[0].scrollHeight - 50) {
		$('#playerinfo .log .more').click();
	}
});

// Show function
function show(el, cb) {
	if (session.transitions) {
		el.fadeIn(100, cb);
	}else{
		el.show(0, cb);
	}
}

// Hide function
function hide(el, cb) {
	if (session.transitions) {
		el.fadeOut(100, cb);
	}else{
		el.hide(0, cb);
	}
}

// API query handler
function query(url, cb, msg) {
	$.get(url, function(data) {
		cb(parse(data, msg));
	});
}

// JSON handler
function parse(data, msg) {
	try {
		var json = $.parseJSON(data);
		if ('error' in json) {
			error(json.error);
			return;
		}
	} catch(err) {
		console.log(err);
		error(msg || lang.error.internal);
		return;
	}
	return json;
}

// Log handler
function formatLog(log, linked) {
	let msg = lang.logs[log.type.toLowerCase()].entry;
	if (linked) {
		msg = msg.replace('{PLAYER}', '<a class="playerlink" data-player="{UUID}">{PLAYER}</a>');
	}
	const x = msg.replace(/{([^ ]+?)}/g, (match, g1) => {
			switch (g1.toLowerCase()) {
				case 'id': return log.id;
				case 'player': return log.session.player.name;
				case 'uuid': return log.session.player.uuid;
				case 'fallback_server': return log.fallbackServer;
				case 'kick_fallback_server': return log.fallbackServer !== "" ? lang.logs.kick_fallback_server.replace("{fallback_server}", log.fallbackServer) : "";
				case 'kick_reason': return parseJsonChat(log.reason);
				default: return match;
			}
		});
	return x;
}


// Parse text with HTML entities
function strip(content) {
	return $('<div/>').text(content).html();
}

const NAMED_COLOR_MAPPINGS = {
	'black' : "#000000",
	'dark_blue' : "#0000aa",
	'dark_green' : "#00aa00",
	'dark_aqua' : "#00aaaa",
	'dark_red' : "#aa0000",
	'dark_purple' : "#aa00aa",
	'gold' : "#ffaa00",
	'gray' : "#aaaaaa",
	'dark_gray' : "#555555",
	'blue' : "#5555ff",
	'green' : "#55ff55",
	'aqua' : "#55ffff",
	'red' : "#ff5555",
	'light_purple' : "#ff55ff",
	'yellow' : "#ffff55",
	'white' : "#ffffff"
};
COLOR_REGEX = /#[0-9a-f]{6}/i;

function parseJsonChat(component) {
	const json = JSON.parse(component);
	let style = {};
	if ('color' in json) {
		if (json.color in NAMED_COLOR_MAPPINGS) {
			json.color = NAMED_COLOR_MAPPINGS[json.color];
		}
		style['color'] = json.color;
	}
	if ('italic' in json) {
		if (json.italic === true) style['font-style'] = 'italic';
		else if (json.italic === false) style['font-style'] = 'normal';
	}
	if ('bold' in json) {
		if (json.bold === true) style['font-weight'] = 'bold';
		else if (json.bold === false) style['font-weight'] = 'normal';
	}
	if ('italic' in json) {
		if (json.italic === true) style['font-style'] = 'italic';
		else if (json.italic === false) style['font-style'] = 'normal';
	}
	// Yes... underline and strikethrough overlap each other 
	if ('underlined' in json) {
		if (json.underlined === true) style['text-decoration'] = 'underline';
		else if (json.underlined === false) style['text-decoration'] = 'none';
	}
	if ('strikethrough' in json) {
		if (json.strikethrough === true) style['text-decoration'] = 'line-through';
		else if (json.strikethrough === false) style['text-decoration'] = 'none';
	}
	let text = strip(json.text);
	if ('extra' in json) {
		for (child of json.extra) {
			text += parseJsonChat(child);
		}
	}
	if (Object.keys(style).length > 0) {
		let styleStr = '';
		for (s in style) {
			styleStr += `${s}:${style[s]};`
		}
		text = `<span style="${styleStr}">${text}</span>`;
	}
	return text;
}

// Error handler
function error(err) {
	if (err === undefined) {
		var err = lang.error.internal || 'An internal error occurred when processing your request.';
	}
	$('.errorbar').text(err).slideDown(300).delay(4000).slideUp(300);
}

