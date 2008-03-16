
package net.sourceforge.filebot.web;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingWorker;


public class DownloadTask extends SwingWorker<ByteBuffer, Object> {
	
	public static final String DOWNLOAD_STATE = "download state";
	public static final String BYTES_READ = "bytes read";
	
	
	public static enum DownloadState {
		PENDING, CONNECTING, DOWNLOADING, DONE;
	}
	
	private static final int BUFFER_SIZE = 4 * 1024;
	
	private URL url;
	private ByteBuffer postdata = null;
	
	private long size = -1;
	private long bytesRead = 0;
	private DownloadState state = DownloadState.PENDING;
	
	
	public DownloadTask(URL url) {
		this.url = url;
	}
	

	public DownloadTask(URL url, ByteBuffer postdata) {
		this.url = url;
		this.postdata = postdata;
	}
	

	public DownloadTask(URL url, Map<String, String> postdata) {
		this.url = url;
		this.postdata = encodeParameters(postdata);
	}
	

	@Override
	protected ByteBuffer doInBackground() throws Exception {
		setDownloadState(DownloadState.CONNECTING);
		
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		
		if (postdata != null) {
			connection.setRequestMethod("POST");
			connection.setDoOutput(true);
			WritableByteChannel out = Channels.newChannel(connection.getOutputStream());
			out.write(postdata);
			out.close();
		}
		
		size = connection.getContentLength();
		
		setDownloadState(DownloadState.DOWNLOADING);
		
		InputStream in = connection.getInputStream();
		ByteArrayOutputStream out = new ByteArrayOutputStream(BUFFER_SIZE);
		
		byte[] buffer = new byte[BUFFER_SIZE];
		int len = 0;
		
		try {
			while ((len = in.read(buffer)) > 0) {
				out.write(buffer, 0, len);
				
				bytesRead += len;
				getPropertyChangeSupport().firePropertyChange(BYTES_READ, null, bytesRead);
			}
		} catch (IOException e) {
			// IOException (Premature EOF) is always thrown when the size of 
			// the response body is not known in advance, so we ignore it
			if (isSizeKnown())
				throw e;
			
		} finally {
			in.close();
			out.close();
			connection.disconnect();
		}
		
		setDownloadState(DownloadState.DONE);
		return ByteBuffer.wrap(out.toByteArray());
	}
	

	private void setDownloadState(DownloadState state) {
		this.state = state;
		getPropertyChangeSupport().firePropertyChange(DOWNLOAD_STATE, null, state);
	}
	

	public DownloadState getDownloadState() {
		return state;
	}
	

	public long getBytesRead() {
		return bytesRead;
	}
	

	public boolean isSizeKnown() {
		return size >= 0;
	}
	

	public long getSize() {
		return size;
	}
	

	private static ByteBuffer encodeParameters(Map<String, String> parameters) {
		StringBuffer sb = new StringBuffer();
		
		int i = 0;
		
		for (String key : parameters.keySet()) {
			if (i > 0)
				sb.append("&");
			
			sb.append(key);
			sb.append("=");
			
			try {
				sb.append(URLEncoder.encode(parameters.get(key), "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				// will never happen
				Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE, e.toString(), e);
			}
			
			i++;
		}
		
		return Charset.forName("UTF-8").encode(sb.toString());
	}
	
}
