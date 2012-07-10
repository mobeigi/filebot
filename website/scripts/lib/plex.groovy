// plex functions
def refreshPlexLibrary(server, port = 32400, files = null) {
	def sections = new URL("http://$server:$port/plex").getXml()
	def locations = sections.Directory.Location.collect{ [path:it.'@path', key:it.parent().'@key'] }
	
	// limit refresh locations
	if (files != null) {
		locations = locations.findAll{ loc -> files.find{ it.path; it.path.startsWith(loc.path) }}
	}
	
	locations*.key.unique().each{ key ->
		new URL("http://$server:$port/library/sections/$key/refresh/").get()
	}
}
