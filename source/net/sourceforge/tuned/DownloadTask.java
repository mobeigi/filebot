
package net.sourceforge.tuned;


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
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
	
	private URL url;
	
	private long size = -1;
	private long bytesRead = 0;
	private DownloadState state = DownloadState.PENDING;
	
	private Map<String, String> postParameters;
	private Map<String, String> requestHeaders;
	private Map<String, List<String>> responseHeaders;
	
	
	public DownloadTask(URL url) {
		this.url = url;
	}
	

	protected HttpURLConnection createConnection() throws Exception {
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		
		if (requestHeaders != null) {
			for (Entry<String, String> entry : requestHeaders.entrySet()) {
				connection.addRequestProperty(entry.getKey(), entry.getValue());
			}
		}
		
		return connection;
	}
	

	@Override
	protected ByteBuffer doInBackground() throws Exception {
		setDownloadState(DownloadState.CONNECTING);
		
		HttpURLConnection connection = createConnection();
		
		if (postParameters != null) {
			connection.setRequestMethod("POST");
			connection.setDoOutput(true);
			WritableByteChannel out = Channels.newChannel(connection.getOutputStream());
			out.write(encodeParameters(postParameters));
			out.close();
		}
		
		size = connection.getContentLength();
		
		responseHeaders = connection.getHeaderFields();
		
		setDownloadState(DownloadState.DOWNLOADING);
		
		ReadableByteChannel in = Channels.newChannel(connection.getInputStream());
		ByteBufferOutputStream buffer = new ByteBufferOutputStream((int) (size > 0 ? size : 32 * 1024));
		
		int count = 0;
		
		try {
			while (!isCancelled() && ((count = buffer.transferFrom(in)) >= 0)) {
				bytesRead += count;
				
				firePropertyChange(DOWNLOAD_PROGRESS, null, bytesRead);
				
				if (isDownloadSizeKnown()) {
					setProgress((int) ((bytesRead * 100) / size));
				}
			}
		} catch (IOException e) {
			// IOException (Premature EOF) is always thrown when the size of 
			// the response body is not known in advance, so we ignore it, if that is the case
			if (isDownloadSizeKnown())
				throw e;
			
		} finally {
			in.close();
			buffer.close();
			
			// download either finished or an exception is thrown
			setDownloadState(DownloadState.DONE);
		}
		
		return buffer.getByteBuffer();
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
	

	public void setRequestHeaders(Map<String, String> requestHeaders) {
		this.requestHeaders = new HashMap<String, String>(requestHeaders);
	}
	

	public void setPostParameters(Map<String, String> postParameters) {
		this.postParameters = new HashMap<String, String>(postParameters);
	}
	

	public Map<String, List<String>> getResponseHeaders() {
		return responseHeaders;
	}
	

	public Map<String, String> getPostParameters() {
		return postParameters;
	}
	

	public Map<String, String> getRequestHeaders() {
		return requestHeaders;
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
