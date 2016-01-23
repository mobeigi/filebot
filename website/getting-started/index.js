
function getData() {
	var data = [{
		link: 'https://www.youtube.com/watch?v=RRq2_Pjyko8&index=1&list=PLdPvEJhzxLMCEJJpb1mJtVkOpS7FfALnd',
		video: 'http://app.filebot.net/getting-started/videos/rename.mp4',
		image: 'images/rename.png',
		thumb: 'images/rename.thumb.png'
	}, {
		link: 'https://www.youtube.com/watch?v=btNSv7AnMMw&index=2&list=PLdPvEJhzxLMCEJJpb1mJtVkOpS7FfALnd',
		video: 'http://app.filebot.net/getting-started/videos/episodes.mp4',
		image: 'images/episodes.png',
		thumb: 'images/episodes.thumb.png'
	}, {
		link: 'https://www.youtube.com/watch?v=q-oZ_hovsTY&index=3&list=PLdPvEJhzxLMCEJJpb1mJtVkOpS7FfALnd',
		video: 'http://app.filebot.net/getting-started/videos/subtitle-lookup.mp4',
		image: 'images/subtitle-hash-lookup.png',
		thumb: 'images/subtitle-hash-lookup.thumb.png'
	}, {
		link: 'https://www.youtube.com/watch?v=R80tKtHf4zw&index=4&list=PLdPvEJhzxLMCEJJpb1mJtVkOpS7FfALnd',
		video: 'http://app.filebot.net/getting-started/videos/subtitle-search.mp4',
		image: 'images/subtitle-search.png',
		thumb: 'images/subtitle-search.thumb.png'
	}, {
		link: 'https://www.youtube.com/watch?v=4KWkSPr3fQY&index=5&list=PLdPvEJhzxLMCEJJpb1mJtVkOpS7FfALnd',
		video: 'http://app.filebot.net/getting-started/videos/sfv.mp4',
		image: 'images/sfv.png',
		thumb: 'images/sfv.thumb.png'
	}, {
		image: 'images/rename.screenshot.png',
		thumb: 'images/rename.screenshot.thumb.png'
	}, {
		image: 'images/format.screenshot.png',
		thumb: 'images/format.screenshot.thumb.png'
	}, {
		image: 'images/subtitle-hash-lookup.screenshot.png',
		thumb: 'images/subtitle-hash-lookup.screenshot.thumb.png'
	}, {
		image: 'images/subtitle-search.screenshot.png',
		thumb: 'images/subtitle-search.screenshot.thumb.png'
	}, {
		image: 'images/sfv.screenshot.png',
		thumb: 'images/sfv.screenshot.thumb.png'
	}, {
		image: 'images/cli.screenshot.png',
		thumb: 'images/cli.screenshot.thumb.png'
	}, {
		image: 'images/node.screenshot.png',
		thumb: 'images/node.screenshot.thumb.png'
	}]

	var links = location.hash.length > 0
	var youtube = !(/zh(.CN)?/i).test(navigator.locale ? navigator.locale : navigator.language) // YouTube is blocked in China (mainland)

	if (links) {
		data = data.slice(0, 5) // use only tutorial images

		if (location.hash == '#mas') {
			data.splice(1, 0, {
					image: 'images/permissions.png',
					thumb: 'images/permissions.thumb.png'
			}) // add sandbox permissions
			data.splice(3, 1) // remove subtitle support
			data.splice(3, 1) // remove subtitle support
		} else if (location.hash == '#usc') {
			data[0].link = 'https://www.youtube.com/watch?v=sEFP3CsntNs&index=6&list=PLdPvEJhzxLMCEJJpb1mJtVkOpS7FfALnd' // ubuntu video
		}
	}

	if (!links) {
		data.forEach(function(it) {
			it.video = it[youtube ? 'link' : 'video']
		})
	}
	if (!youtube) {
		data.forEach(function(it) {
			it[links ? 'link' : 'iframe'] = it.video
			delete it.video
		})
	}
	return data
}

function runGalleria() {
	var data = getData()

	Galleria.run('.galleria', {
		dataSource: data,
		popupLinks: true,
		maxScaleRatio: 1,
		youtube: {
			VQ: 'HD1080'
		},
		thumbnails: 'lazy'
	})

	Galleria.ready(function() {
		this.bind("image", function(e) {
			if (this.doLazyLoadChunks !== false) {
				this.doLazyLoadChunks = false
				this.lazyLoadChunks(data.length)
			}
		})
		this.attachKeyboard({
			left: this.prev,
			right: this.next
		})
	})
}

function isFullScreen() {
	return (document.fullScreenElement && document.fullScreenElement !== null) || document.mozFullScreen || document.webkitIsFullScreen
}

function requestFullScreen(element) {
	if (element.requestFullscreen)
		element.requestFullscreen()
	else if (element.msRequestFullscreen)
		element.msRequestFullscreen()
	else if (element.mozRequestFullScreen)
		element.mozRequestFullScreen()
	else if (element.webkitRequestFullscreen)
		element.webkitRequestFullscreen()
}

function exitFullScreen() {
	if (document.exitFullscreen)
		document.exitFullscreen()
	else if (document.msExitFullscreen)
		document.msExitFullscreen()
	else if (document.mozCancelFullScreen)
		document.mozCancelFullScreen()
	else if (document.webkitExitFullscreen)
		document.webkitExitFullscreen()
}

function toggleFullScreen(element) {
	if (isFullScreen())
		exitFullScreen()
	else
		requestFullScreen(element || document.documentElement)
}
