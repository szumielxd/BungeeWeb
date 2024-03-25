// Logs pageaddLogs(0)
pages.logs = (function() {
	const queryFilters = new Map();
	const extraFilters = new Map();
	var searchTimer = 0;
	
	// When the whole client is loaded
	function load() {
		setFilters($('#logs .filters'));
		for (type of ['chat', 'command']) {
			switchExtraFilters($('#logs'), type, false);
		}
	}
	
	// When the page is navigated to
	function navigate() {
		resetLogs();
	}
	
	// When the data needs to be updated
	function update(lastUpdate) {
		addLogs(parseInt($('#logs li').first().attr('data-id')), -1, 'prepend');
	}
	
	// Logs reset
	function resetLogs() {
		$('#logs .log').html('');
		addLogs(0);
	}

	/*function switchExtraFilters(parent, type, state) {
		if (!state) {
			parent.find(`.${type}-filters > div > input`).val('');
			parent.find(`.${type}-filters`).hide();
		} else {
			parent.find(`.${type}-filters`).show();
		}
	}*/
	
	// Logs retrieval
	function addLogs(minId = 0, maxId = -1, position = "append", cb) {
		const limit = 50;
		const params = new Map();
		params.set('minId', minId);
		params.set('maxId', maxId);
		params.set('limit', limit);
		extraFilters.forEach((v, k) => params.set(k, v));
		queryFilters.forEach((v, k) => params.set(k, v));
		params.set('types', getFilters($('#logs .filters')));
		const url = `api/getlogs?${new URLSearchParams(params)}`;
		query(url, function(data) {
			var entries = '';
			for (log of data) {
				var d = new Date(log.time * 1000);
				entries += `<li class="entry" data-type="${log.type}" data-id="${log.id}">
						<div class="log-face">
							<div class="content">${formatLog(log, true, true)}</div>
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
					</li>`;
			}
			
			if (position == 'prepend') {
				$('#logs .log').prepend(entries);
			} else {
				$('#logs .log').append(entries);
			}
			
			if (data.length == limit && $('#logs .log .more').length == 0) $('#logs .log').append('<li class="more">Show more</li>');
			if (cb !== undefined) cb();
		});
	}
	
	// Logs filter
	$('#logs .filters').on('click', 'a', function() {
		const enabled = $(this).toggleClass('active').hasClass('active');
		const type = $(this).attr('data-type-id');
		switch (type) {
			case 'chat':
			case 'command': {
				switchExtraFilters($('#logs'), type, enabled);
				break;
			}
		}
		resetLogs();
	});

	// Logs "show more" button handler
	$('#logs .log').on('click', '.more', function() {
		const more = $(this);
		const parent = more.parent();
		const maxId = parseInt(parent.find('li.entry').last().attr('data-id'));
		more.removeClass('more').text(lang.logs.loading);
		addLogs(-1, maxId, 'append', function() {
			more.remove();
		});
	});
	
	// Logs query filters handler
	$('#logs .query-filters').submit(async function(e) {
		e.preventDefault();
		const a = Array.from(getExtraFilters($('#logs .query-filters')).entries()).map(([k, v]) => {
			if (k === 'usernames') {
				return getUUIDs([... new Set(v.split(' ').filter(element => element))]).then((arr) => {
					return ['uuids', [... new Set(arr.filter(e => e).map(e => e.uuid))].join(' ')];
				});
			} else {
				return Promise.resolve([k, v]);
			}
		});
		const result = await Promise.all(a);
		queryFilters.clear();
		result.forEach(([k, v]) => queryFilters.set(k, v));
		resetLogs();
	});
	
	// Logs extra filters handler
	$('#logs .extra-filters').submit(function(e) {
		e.preventDefault();
		extraFilters.clear();
		getExtraFilters($('#logs .extra-filters')).forEach((v, k) => extraFilters.set(k, v));
		resetLogs();
	});
	
	$('#logs .search input').keyup(function(e) {
		if (session.autosearch) {
			var form = $(this).closest('form');
			clearTimeout(searchTimer);
			searchTimer = setTimeout(function() {
				form.submit();
			}, 500);
		}
	});
	
	// Window scroll handler
	$(window).scroll(function() {
		if ($('#logs').hasClass('active') && $(window).scrollTop() + $(window).height() > $(document).height() - 50) {
			$('#logs .log .more').click();
		}
	});
	
	return {
		load: load,
		navigate: navigate,
		update: update
	}
})();