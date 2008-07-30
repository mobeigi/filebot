
package net.sourceforge.tuned;


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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingWorker;


public class DownloadTask extends SwingWorker<ByteBuffer, Void> {
	
	public static final String DOWNLOAD_STATE = "download state";
	public static final String DOWNLOAD_PROGRESS = "download progress";
	
	
	public static enum DownloadState {
		PENDING,
		CONNECTING,
		DOWNLOADING,
		DONE
	}
	
	private static final int BUFFER_SIZE = 4 * 1024;
	
	private URL url;
	private ByteBuffer postdata;
	
	private long size = -1;
	private long bytesRead = 0;
	private DownloadState state = DownloadState.PENDING;
	
	private final Map<String, String> requestHeaders = new HashMap<String, String>();
	private final Map<String, String> responseHeaders = new HashMap<String, String>();
	
	
	public DownloadTask(URL url) {
		this(url, null);
	}
	

	public DownloadTask(URL url, Map<String, String> postParameters) {
		this.url = url;
		
		if (postParameters != null) {
			this.postdata = encodeParameters(postParameters);
		}
	}
	

	protected HttpURLConnection createConnection() throws Exception {
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		
		for (String key : requestHeaders.keySet()) {
			connection.addRequestProperty(key, requestHeaders.get(key));
		}
		
		return connection;
	}
	

	@Override
	protected ByteBuffer doInBackground() throws Exception {
		setDownloadState(DownloadState.CONNECTING);
		
		HttpURLConnection connection = createConnection();
		
		if (postdata != null) {
			connection.setRequestMethod("POST");
			connection.setDoOutput(true);
			WritableByteChannel out = Channels.newChannel(connection.getOutputStream());
			out.write(postdata);
			out.close();
		}
		
		size = connection.getContentLength();
		
		for (String key : connection.getHeaderFields().keySet()) {
			responseHeaders.put(key, connection.getHeaderField(key));
		}
		
		setDownloadState(DownloadState.DOWNLOADING);
		
		InputStream in = connection.getInputStream();
		ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max((int) size, BUFFER_SIZE));
		
		byte[] buffer = new byte[BUFFER_SIZE];
		int len = 0;
		
		try {
			while (((len = in.read(buffer)) >= 0) && !isCancelled()) {
				out.write(buffer, 0, len);
				
				bytesRead += len;
				
				firePropertyChange(DOWNLOAD_PROGRESS, null, bytesRead);
				
				if (isDownloadSizeKnown()) {
					setProgress((int) ((bytesRead * 100) / size));
				}
			}
		} catch (IOException e) {
			// IOException (Premature EOF) is always thrown when the size of 
			// the response body is not known in advance, so we ignore it
			if (isDownloadSizeKnown())
				throw e;
			
		} finally {
			in.close();
			out.close();
		}
		
		setDownloadState(DownloadState.DONE);
		return ByteBuffer.wrap(out.toByteArray());
	}
	

	protected void setDownloadState(DownloadState state) {
		this.state = state;
		firePropertyChange(DOWNLOAD_STATE, null, state);
	}
	

	public DownloadState getDownloadState() {
		return state;
	}
	

	public URL getUrl() {
		return url;
	}
	

	public long getBytesRead() {
		return bytesRead;
	}
	

	public boolean isDownloadSizeKnown() {
		return size >= 0;
	}
	

	public long getDownloadSize() {
		return size;
	}
	

	public void setRequestHeader(String name, String value) {
		requestHeaders.put(name, value);
	}
	

	public Map<String, String> getResponseHeaders() {
		return Collections.unmodifiableMap(responseHeaders);
	}
	

	protected static ByteBuffer encodeParameters(Map<String, String> parameters) {
		StringBuilder sb = new StringBuilder();
		
		int i = 0;
		
		for (Entry<String, String> entry : parameters.entrySet()) {
			if (i > 0)
				sb.append("&");
			
			sb.append(entry.getKey());
			sb.append("=");
			
			try {
				sb.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				// will never happen
				Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE, e.toString(), e);
			}
			
			i++;
		}
		
		return Charset.forName("UTF-8").encode(sb.toString());
	}
	
}
