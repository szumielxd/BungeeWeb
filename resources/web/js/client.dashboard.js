// Dashboard page
pages.dashboard = (function() {
	var stats = {};
	var chart = {};
	
	// When the whole client is loaded
	function load() {
		// Setup graphs
		stats = { 'playercount': lang.dashboard.playercount, 'maxplayers': lang.dashboard.playerlimit, 'activity': lang.dashboard.loggeditems };
		Highcharts.setOptions({
			global: {
				useUTC: false
			}
		});
	}
	
	// When the page is navigated to
	function navigate() {
		// Retrieve initial graph data
		getStatsData('', function(data) {
			/*chart = new Highcharts.StockChart({
				chart: { renderTo: 'graph-dashboard' },
				series: data,
				yAxis: { min: 0 },
				rangeSelector: {
					buttons: [
						{
							count: 1,
							type: 'hour',
							text: '1h'
						},
						{
							count: 3,
							type: 'hour',
							text: '3h'
						},
						{
							count: 1,
							type: 'day',
							text: '1d'
						},
						{
							count: 1,
							type: 'week',
							text: '1w'
						},
						{
							count: 2,
							type: 'week',
							text: '2w'
						},
						{
							count: 1,
							type: 'month',
							text: '1m'
						}
					],
					inputEnabled: true,
					selected: 0
				}
			});*/
			
			chart = Highcharts.chart('graph-dashboard', {
	            chart: {
	                zoomType: 'x'
	            },
	            title: {
	                text: 'Online players'
	            },
	            subtitle: {
	                text: document.ontouchstart === undefined ?
	                    'Click and drag in the plot area to zoom in' : 'Pinch the chart to zoom in'
	            },
	            xAxis: {
	                type: 'datetime'
	            },
	            yAxis: {
                    min: 0,
	                title: {
	                    text: 'Players'
	                }
	            },
	            legend: {
	                enabled: true
	            },
	            plotOptions: {
	                area: {
	                    fillColor: {
	                        linearGradient: {
	                            x1: 0,
	                            y1: .1,
	                            x2: .1,
	                            y2: .9
	                        },
	                        stops: [
	                            [0, Highcharts.getOptions().colors[0]],
	                            [1, Highcharts.color(Highcharts.getOptions().colors[0]).setOpacity(0).get('rgba')]
	                        ]
	                    },
	                    marker: {
	                        radius: 2
	                    },
	                    lineWidth: 1,
	                    states: {
	                        hover: {
	                            lineWidth: 1
	                        }
	                    },
	                    threshold: null
	                }
	            },
	
	            series: data
	        });
		});
		
		update(0);
	}
	
	// When the data needs to be updated
	function update(lastUpdate) {
		// List of servers
		var players = 0;
		query('api/listservers', function(data) {
			var entries = '';
			var i = 0;
			for (server in data) {
				entries += `<li data-server="${server}"><a class="serverlink" data-server="${server}">${server}<span class="badge">${data[server]}</span></a></li>`;
				players += data[server];
				i++;
			}
			
			$('#dashboard .servers .log').html(entries);
			$('#dashboard .servers h1 span').text(i + ' ' + lang.dashboard.servers.toLowerCase());
			$('#dashboard .logs h1 span').text(players + ' ' + lang.dashboard.players);

			// Latest logs
			if (i < 5) i = 5;
			query('api/getlogs?limit=' + i + '&time=' + lastUpdate, function(data) {
				var entries = '';
				for (item in data) {
					entries += '<li>' + formatLog(data[item], true) + '</li>';
				}
				
				$('#dashboard .logs .log').prepend(entries);
				
				if ($('#dashboard .logs .log li').length > i) {
					$('#dashboard .logs .log li:gt(' + (i - 1) + ')').remove();
				}
			});
		});
		
		// Graph data
		if (lastUpdate > 0) {
			getStatsData(lastUpdate, function(data, inc) {
				for (c in data) {
					var set = data[c].data;
					for (i in set) {
						chart.series[c].addPoint(set[i], true, true);
					}
				}
				increment = inc;
			});
		}
	}
	
	// Retrieve the statistics for the graph
	function getStatsData(since, cb) {
		if (hasPermission('stats')) {
			query('api/getstats?since=' + since, function(data) {
				var out = [];
				for (c in stats) {
					out.push({
						name: stats[c],
						data: data.data[c]
					});
				}
				cb(out, data.increment);
			});
		}
	}
	
	return {
		load: load,
		navigate: navigate,
		update: update
	}
})();