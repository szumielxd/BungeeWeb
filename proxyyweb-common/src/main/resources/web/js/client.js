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
let activityPie;
let clientsPie;

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
$('.log').on('click', 'a.playerlink', function() {
	hideWindows();
	showPlayer($(this).attr('data-player'));
});

// Server link click handler
$('.log').on('click', 'a.serverlink', function() {
	hideWindows();
	showServer($(this).attr('data-server'));
});

// Dialog escape handler
$('.mask').click(function() {
	$(this).removeClass('active');
	$('body').css({ 'overflow': 'visible' });
	hideWindows();
	/* hide($(this), function() {
		$('body').css({ 'overflow': 'visible' });
		hideWindows();
	}); */
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
			var link = `a[href="/${path}"]`;
			$(`.navbar ${link}, .dropdown ${link}`).addClass('active');
		}else{
			activepage = pages.dashboard;
			activepage.navigate();
		}
		
		if (session.updatetime > 0) {
			var lastupdate = 0;
			setInterval(function() {
				//if (activepage.update && lastupdate > 0) activepage.update(lastupdate);
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
	$('#playerinfo').attr('data-selector', uuid);
	$('.mask').addClass('active');
	setFilters($('#playerinfo .filters'));
	for (type of ['chat', 'command']) {
		switchExtraFilters($('#playerinfo'), type, false);
	}
	resetPlayer(uuid);
}

// Server dialog
function showServer(serverName) {
	$('body').css({ 'overflow': 'hidden' });
	$('#serverinfo').attr('data-selector', serverName);
	$('.mask').addClass('active');
	setFilters($('#serverinfo .filters'));
	for (type of ['chat', 'command']) {
		switchExtraFilters($('#serverinfo'), type, false);
	}
	resetServer(serverName);
}

// Player info retrieval
function resetPlayer(uuid) {
	resetDialogLogs($('#playerinfo'), uuid);
	//addPlayerLogs(uuid, -1, getFilters($('#playerinfo .filters')), getExtraFilters($('#playerinfo .extra-filters')));
}

function resetDialogLogs(dialog, selector) {
	const method = window[dialog.attr('scroll-method')];
	if (method) {
		method(selector, -1, getFilters(dialog.find('.filters')), getExtraFilters(dialog.find('.extra-filters')));
	}
}

// Player info retrieval
function resetServer(server) {
	resetDialogLogs($('#serverinfo'), server);
	//addServerLogs(server, -1, getFilters($('#serverinfo .filters')), getExtraFilters($('#serverinfo .extra-filters')));
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
		let user;
		if (maxId === -1) {
			$('#playerinfo .log').html('');
			if (data.length > 0) {
				user = data[0].session.player.name;
				getPlayerActivityData(uuid);
				$('#playerinfo h1').text(user);
				$('#playerinfo .uuid').text(uuid);
				skinview.changeSkin(user);
			}
		}

		for (log of data) {
			var d = new Date(log.time * 1000);
			$('#playerinfo .log').append(
					`<li class="entry" data-type="${log.type}" data-id="${log.id}">
						<div class="log-face">
							<div class="content">${formatLog(log, false, true)}</div>
							<div class="fade server"><a class="serverlink" data-server="${log.server}">${log.server}</a></div>
							<div class="fade time">${d.toLocaleString()}</div>
						</div>
						<div class="log-extra">
							<span>UUID: <span>${log.session.player.uuid}</span></span>
							<span>IP: <span>${log.session.ip}</span></span>
							<span>Host: <span>hypixel.pl</span></span>
							<span>Client: <span>${log.session.client}</span></span>
							<span>Version: <span>${log.session.protocol.name}</span></span>
						</div>
					</li>`);
			if (log.session.player.name != user) {
				if(user) {
					$('#playerinfo .log').append(`<li>${user} is now known as ${log.session.player.name}</li>`);
				}
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
			getServerClientData(server);
		}

		for (log of data) {
			var d = new Date(log.time * 1000);
			$('#serverinfo .log').append(
					`<li class="entry" data-type="${log.type}" data-id="${log.id}">
						<div class="log-face">
							<div class="content">${formatLog(log, true, true)}</div>
							<div class="fade server">${log.server}</div>
							<div class="fade time">${d.toLocaleString()}</div>
						</div>
						<div class="log-extra">
							<span>UUID: <span>${log.session.player.uuid}</span></span>
							<span>IP: <span>${log.session.ip}</span></span>
							<span>Host: <span>hypixel.pl</span></span>
							<span>Client: <span>${log.session.client}</span></span>
							<span>Version: <span>${log.session.protocol.name}</span></span>
						</div>
					</li>`);
		}

		if (data.length == limit) $('#serverinfo .log').append('<li class="more">Show more</li>');
		if (cb !== undefined) cb();
		$('#serverinfo').addClass('active');
	});
}

// Dialog logs filter
$('.dialog .filters').on('click', 'a', function() {
	const enabled = $(this).toggleClass('active').hasClass('active');
	const type = $(this).attr('data-type-id');
	const parent = $(this).parents('.dialog');
	switch (type) {
		case 'chat':
		case 'command': {
			switchExtraFilters(parent, type, enabled);
			break;
		}
	}
	resetDialogLogs(parent, parent.attr('data-selector'));
});

// Dialog logs "show more" button handler
$('.dialog .log').on('click', '.more', function() {
	const more = $(this);
	const parent = more.parents('.dialog');
	more.removeClass('more').text('Loading...');
	const method = window[parent.attr('scroll-method')];
	if (method) {
		const selector = parent.attr('data-selector');
		const id = parseInt(parent.find('.log li.entry').last().attr('data-id'));
		const filters = getFilters(parent.find('.filters'));
		const extra = getExtraFilters(parent.find('.extra-filters'));
		method(selector, id, filters, extra, function() {
			more.remove();
		});
	}
});


let lastClicked = null;
// Show/hide extra info
$('.log').on('click', '.log-face', function(event) {
	let state = false;
	if (event.ctrlKey || event.metaKey) {
		$(this).parent().toggleClass('selected');
	} else if (event.shiftKey) {
		if (lastClicked) {
			let active = null;
			const checks = [lastClicked, this.parentNode];
			for (const ch of $(this).parent().parent().children('li.entry')) {
				for (cmp of checks) {
					if (ch === cmp) {
						active = !active;
					}
				}
				if (active !== null) {
					$(ch).addClass('selected');
					if (!active) {
						break;
					}
				}
			}
		}
	} else {
		$(this).parent().toggleClass('active');
		// clear selection
		state = true;
	}
	lastClicked = this.parentNode;
	return state;
});

// clear selection
$(document).on('click', _ => {
	$('li.entry.selected').removeClass('selected');
});

$(document).on('keydown', event => {
	if (event.ctrlKey || event.metaKey) {
		if (!event.shiftClick && event.keyCode === 67) {
			const selection = $('li.entry.selected');
			if (selection.length > 0) {
				const lines = [];
				selection.each((i, el) => {
					const e = $(el);
					const time = e.find('.time').text();
					const srv = e.find('.server').text();
					const content = e.find('.content').text();
					lines.push(`[${time}]${!srv ? '' : ` (${srv})`} ${content}`);
				});
				const content = lines.join('\n');
				if (navigator.clipboard) {
					navigator.clipboard.writeText(content);
					return false; // cancel default copy
				} else {
					const input = document.getElementById('copied-content');
					$(input).val(content);
					input.select();
					document.execCommand('copy');
					return false;
				}
			}
		}
	}
});

// Mask scroll handler
$('.mask .dialog .logs').on('scroll', function() {
	const parent = $(this).parents('.dialog');
	if (parent.hasClass('active') && $(this).scrollTop() + $(this).height() > $(this)[0].scrollHeight - 50) {
		parent.find('.log .more').click();
	}
});

let resizingEvents = {};
$('.dialog .resizer').on('mousedown', event => {
	const el = $($(event.target).attr('target'));
	let lastCoords = event.pageX;
	resizingEvents[event.target] = x => {
		const diff = lastCoords - x;
		lastCoords = x;
		el.each((i, e) =>{
			const newWidth = Math.max(e.offsetWidth + diff, 0);
			$(e).width(newWidth);
		});
	}
});

$(':root').on('mouseup', _ => {
	resizingEvents = {};
});

$('.dialog').on('mousemove', event => {
	for (el in resizingEvents) {
		resizingEvents[el](event.pageX);
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
function formatLog(log, linked=false, formatted=false) {
	let msg = lang.logs[log.type.toLowerCase()].entry;
	if (linked) {
		msg = msg.replace('{PLAYER}', '<a class="playerlink" data-player="{!UUID}">{PLAYER}</a>');
	}
	const x = formatted ? replaceTextInLog(msg, log, (x,y) => `<span replacement="${y}">${x}</span>`) : replaceTextInLog(msg, log);
	return x;
}

function replaceTextInLog(msg, log, formatter=(x, y)=>x) {
	return msg.replace(/{(\!?)([^ ]+?)}/g, (match, g1, g2) => {
		match = g2.toLowerCase();
		const cond = (x, y) => {
			return g1 !== "!" ? formatter(x, y) : x;
		}
		switch (match) {
			case 'id': return cond(log.id, match);
			case 'player': return cond(log.session.player.name, match);
			case 'uuid': return cond(log.session.player.uuid, match);
			case 'message': return cond(log.message, match);
			case 'command': return cond(log.command, match);
			case 'arguments': return cond(log.arguments, match);
			case 'target_server': return cond(log.targetServer, match);
			case 'fallback_server': return cond(log.fallbackServer, match);
			case 'kick_fallback_server': return cond(log.fallbackServer !== "" ? replaceTextInLog(lang.logs.kick_fallback_server, log) : "", match);
			case 'kick_reason': return cond(parseJsonChat(log.reason), match);
			default: return match;
		}
	});
}

function getPlayerActivityData(uuid, cb) {
	if (hasPermission('activity')) {
		const to = Math.floor(new Date().getTime() / (3_600_000)) * 3_600; // last full hour
		const since = to - (1 * 30 * 24 * 3_600); // 6 * 30 days earlier
		query(`api/getactivity?uuid=${uuid}&since=${since}&to=${to}`, function(data) {
			delete data[""];
			let servers = [];
			for (srv in data) {
				let times = [];
				let sum = 0;
				for (let i = since; i <= to; i += 3600) {
					times.push([ i * 1000, data[srv][i] ?? 0 ]);
					sum += data[srv][i] ?? 0;
				}
				servers.push({
					name: srv,
					data: times
				});
			}
			buildPlayerActivityChart(servers, 'graph-player-activity-chart');
			activityPie = buildPlayerActivityPie([], 'graph-player-activity-pie');
		});
	}
}

function getServerClientData(server, cb) {
	if (hasPermission('clients')) {
		const to = Math.floor(new Date().getTime() / (3_600_000)) * 3_600; // last full hour
		const since = to - (1 * 30 * 24 * 3_600); // 6 * 30 days earlier
		query(`api/getclients?server=${server}&since=${since}&to=${to}`, function(data) {
			let clients = [];
			for (cl in data) {
				let times = [];
				for ([date, count] of Object.entries(data[cl])) {
					times.push([date * 1000, count]);
				}
				clients.push({
					name: cl,
					data: times
				});
			}
			buildServerClientsChart(clients, 'graph-server-clients-chart');
			clientsPie = buildServerClientsPie([], 'graph-server-clients-pie');
		});
	}
}

function buildPlayerActivityChart(data, id) {
	lastUpdate = new Date().getTime();
	return Highcharts.stockChart(id, {
		accessibility: {
			enabled: false
		},
		boost: {
			useGPUTranslations: true,
			// Chart-level boost when there are more than 5 series in the chart
			seriesThreshold: 5
		},
		chart: {
			type: 'column',
			backgroundColor: null,
			styledMode: true,
			events: {
				async render() {
					const chart = this;
					let all = 0;
					let activity = [];

					chart.series.forEach(series => {
						if (series.name !== 'Navigator 1') {
							if (series.visible) {
								const sum = series.processedYData.reduce((acc, point) => acc + point, 0);
								all += sum;
								activity.push({
									name : series.name,
									y: sum
								});
							}
						}
					});
					$('#sum-player-activity-chart').text(formatTimespan(Math.round(all)));
					const updateTime = new Date().getTime(); 
					lastUpdate = updateTime;
					setTimeout(() => {
						if (lastUpdate === updateTime) {
							if (activityPie !== undefined) {
								activityPie.series[0].setData(activity);
							}
						}
					}, 500); // 500ms treshold
				}
			}
		},
		rangeSelector: {
            selected: 1
        },
		xAxis: {
			type: 'datetime'
		},
		yAxis: {
			allowDecimals: false,
			min: 0,
			title: {
				text: 'minutes'
			}
		},
		legend: {
			enabled: true,
			maxHeight: 100
		},
		navigator: {
			height: 30
		},
		plotOptions: {
			column: {
				stacking: 'normal'
			}
		},
		series: data
	});
}

function buildPlayerActivityPie(data, id) {
	return Highcharts.chart(id, {
		accessibility: {
			enabled: false
		},
		boost: {
			useGPUTranslations: true,
			// Chart-level boost when there are more than 5 series in the chart
			seriesThreshold: 5
		},
		chart: {
			backgroundColor: null,
			plotBorderWidth: null,
			plotShadow: false,
			type: 'pie'
		},
		title: {
			text: null
		},
		tooltip: {
			formatter: function () {
				return `Percentage: <b>${this.percentage.toFixed(1)}%</b><br/>Time: <b>${formatTimespan(this.y)}</b>`;
			}
		},
		plotOptions: {
			pie: {
				//allowPointSelect: true,
				cursor: 'pointer',
				dataLabels: {
					enabled: true,
					format: '<b>{point.name}</b>: {point.percentage:.1f}%',
					style: {
						color: 'var(--highcharts-neutral-color-80)'
					}
				}
			}
		},
		series: [{
			boostThreshold: 1,
			name: 'Servers',
			colorByPoint: true,
			data: data
		}]
	});
}

function buildServerClientsChart(data, id) {
	lastUpdate = new Date().getTime();
	return Highcharts.stockChart(id, {
		accessibility: {
			enabled: false
		},
		boost: {
			useGPUTranslations: true,
			// Chart-level boost when there are more than 5 series in the chart
			seriesThreshold: 5
		},
		chart: {
			type: 'area',
			backgroundColor: null,
			styledMode: true,
			events: {
				async render() {
					const chart = this;
					let activity = [];
					let maxClient = '';
					let maxValue = 0;

					chart.series.forEach(series => {
						if (series.name !== 'Navigator 1') {
							if (series.visible) {
								const sum = series.processedYData.reduce((acc, point) => acc + point, 0);
								const max = Math.max(series.processedYData);
								if (max > maxValue || maxClient === '') {
									maxValue = max;
									maxClient = ` (${series.name})`;
								}
								activity.push({
									name : series.name,
									y: sum
								});
							}
						}
					});
					$('#max-server-clients-chart').text(`${maxValue}${maxClient}`);
					const updateTime = new Date().getTime(); 
					lastUpdate = updateTime;
					setTimeout(() => {
						if (lastUpdate === updateTime) {
							if (clientsPie !== undefined) {
								clientsPie.series[0].setData(activity);
							}
						}
					}, 500); // 500ms treshold
				}
			}
		},
		rangeSelector: {
            selected: 1
        },
		xAxis: {
			allowDecimals: false,
			type: 'datetime'
		},
		yAxis: {
			allowDecimals: false,
			min: 0,
			title: {
				text: 'players'
			}
		},
		legend: {
			enabled: true,
			maxHeight: 100
		},
		navigator: {
			height: 30
		},
		plotOptions: {
			area: {
				stacking: 'normal'
			}
		},
		series: data
	});
}

function buildServerClientsPie(data, id) {
	return Highcharts.chart(id, {
		accessibility: {
			enabled: false
		},
		boost: {
			useGPUTranslations: true,
			// Chart-level boost when there are more than 5 series in the chart
			seriesThreshold: 5
		},
		chart: {
			backgroundColor: null,
			plotBorderWidth: null,
			plotShadow: false,
			type: 'pie'
		},
		title: {
			text: null
		},
		tooltip: {
			formatter: function () {
				return `Percentage: <b>${this.percentage.toFixed(1)}%</b><br/>Time: <b>${formatTimespan(this.y)}</b>`;
			}
		},
		plotOptions: {
			pie: {
				//allowPointSelect: true,
				cursor: 'pointer',
				dataLabels: {
					enabled: true,
					format: '<b>{point.name}</b>: {point.percentage:.1f}%',
					style: {
						color: 'var(--highcharts-neutral-color-80)'
					}
				}
			}
		},
		series: [{
			boostThreshold: 1,
			name: 'Clients',
			colorByPoint: true,
			data: data
		}]
	});
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
	try {
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
	} catch (SyntaxError) {
		return component;
	}
}

function formatTimespan(minutes) {
	if (minutes == 0) {
		return `0m`;
	}
	const divs = [60, 24];
	const chars = ['m', 'h', 'd'];

	let times = [];
	for (d of divs) {
		times.push(minutes % d);
		minutes = Math.floor(minutes / d);
	}
	times.push(minutes);

	let res = "";
	for (i in times) {
		if (times[i] > 0) {
			res = ` ${times[i]}${chars[i]}` + res;
		}
	}
	return res.substring(1);
}

// Error handler
function error(err) {
	if (err === undefined) {
		var err = lang.error.internal || 'An internal error occurred when processing your request.';
	}
	$('.errorbar').text(err).slideDown(300).delay(4000).slideUp(300);
}

