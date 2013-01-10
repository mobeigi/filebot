
package net.sourceforge.filebot.web;


import java.io.Serializable;


public class AudioTrack implements Serializable {
	
	private String artist;
	private String title;
	private String album;
	
	
	public AudioTrack(String artist, String title, String album) {
		this.artist = artist;
		this.title = title;
		this.album = album;
	}
	
	
	public String getArtist() {
		return artist;
	}
	
	
	public String getTitle() {
		return title;
	}
	
	
	public String getAlbum() {
		return album;
	}
	
	
	@Override
	public String toString() {
		return String.format("%s - %s", getArtist(), getTitle());
	}
	
}
