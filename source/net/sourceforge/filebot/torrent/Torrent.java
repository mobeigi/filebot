
package net.sourceforge.filebot.torrent;


import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


public class Torrent {
	
	private String name;
	private String encoding;
	private String createdBy;
	private String announce;
	private String comment;
	private Long creationDate;
	
	private List<Entry> files;
	
	private boolean singleFileTorrent;
	
	
	public Torrent(File torrent) throws IOException {
		FileInputStream in = new FileInputStream(torrent);
		Map<?, ?> torrentMap = null;
		
		try {
			torrentMap = BDecoder.decode(new BufferedInputStream(in));
		} finally {
			in.close();
		}
		
		Charset charset = Charset.forName("UTF-8");
		
		encoding = decodeString(torrentMap.get("encoding"), charset);
		
		try {
			charset = Charset.forName(encoding);
		} catch (Exception e) {
			
		}
		
		createdBy = decodeString(torrentMap.get("created by"), charset);
		announce = decodeString(torrentMap.get("announce"), charset);
		comment = decodeString(torrentMap.get("comment"), charset);
		creationDate = decodeLong(torrentMap.get("creation date"));
		
		Map<?, ?> infoMap = (Map<?, ?>) torrentMap.get("info");
		
		name = decodeString(infoMap.get("name"), charset);
		
		if (infoMap.containsKey("files")) {
			singleFileTorrent = false;
			
			files = new ArrayList<Entry>();
			
			for (Object fileMapObject : (List<?>) infoMap.get("files")) {
				Map<?, ?> fileMap = (Map<?, ?>) fileMapObject;
				List<?> pathList = (List<?>) fileMap.get("path");
				
				StringBuffer pathBuffer = new StringBuffer();
				String entryName = null;
				
				Iterator<?> iterator = pathList.iterator();
				
				while (iterator.hasNext()) {
					String pathElement = decodeString(iterator.next(), charset);
					
					if (iterator.hasNext()) {
						pathBuffer.append(pathElement);
						pathBuffer.append("/");
					} else {
						// the last element in the path list is the filename
						entryName = pathElement;
					}
				}
				
				Long length = decodeLong(fileMap.get("length"));
				
				files.add(new Entry(entryName, length, pathBuffer.toString()));
			}
		} else {
			// single file torrent
			singleFileTorrent = true;
			files = new ArrayList<Entry>(1);
			
			Long length = decodeLong(infoMap.get("length"));
			
			files.add(new Entry(name, length, ""));
		}
	}
	

	private String decodeString(Object bytearray, Charset charset) {
		if (bytearray == null)
			return null;
		
		return new String((byte[]) bytearray, charset);
	}
	

	private Long decodeLong(Object number) {
		if (number == null)
			return null;
		
		return (Long) number;
	}
	

	public String getAnnounce() {
		return announce;
	}
	

	public String getComment() {
		return comment;
	}
	

	public String getCreatedBy() {
		return createdBy;
	}
	

	public Long getCreationDate() {
		return creationDate;
	}
	

	public String getEncoding() {
		return encoding;
	}
	

	public List<Entry> getFiles() {
		return files;
	}
	

	public String getName() {
		return name;
	}
	

	public boolean isSingleFileTorrent() {
		return singleFileTorrent;
	}
	
	
	public static class Entry {
		
		private String name;
		private long length;
		private String path;
		
		
		public Entry(String name, Long length, String path) {
			this.name = name;
			this.length = length;
			this.path = path;
		}
		

		public Long getLength() {
			return length;
		}
		

		public String getName() {
			return name;
		}
		

		public String getPath() {
			return path;
		}
	}
	
}
