/* Patrick Kirsch, <pkirsch@zscho.de>
 */
package de.zscho.samples;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.params.ConnManagerPNames;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.scheme.LayeredSocketFactory;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.scheme.SocketFactory;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import android.content.Context;
import android.util.Log;

public class HTTP_Update {

	private String imei = null;
	public String pos = null;
	private String LOG_TAG = "HTTP_Update";
	String urlStagingLive = "http://zscho.de/hydranten_map_app/hydrant.php";
	String urlStagingLiveSubmit = "http://zscho.de/hydranten_map_app/hydrant.php";

	// latitude=51.809364&longitude=012.386478&
	// smsUTC=1/29/2011%208:22:17%20PM&
	/* wichtig ist, dass später die Leerzeichen durch %20 ersetzt werden */
	
	public Context context = null;
		
	public void setImei(String imei) {
		this.imei = imei;
	}
	
	public HTTP_Update() {
		// init functions calls overwrite backend function
	}

	public String[] overwriteFromBackend() {
		String[] latlon = null;
		try {
		Log.d( LOG_TAG,"overwriteFromBackend");
		String returned = getString(urlStagingLive,this.pos);
		Log.d( LOG_TAG,"overwriteFromBackend, returned: "+returned);
		String[] datensatz = returned.split("\n");
		Log.d( LOG_TAG,"overwriteFromBackend, datensatz.length: "+(datensatz.length-3));
		latlon = new String[datensatz.length*3-3];
		
		//  49.4593182	11.0819087	Hydrant	Text	Ol_icon_blue_example.png	24,24	0,-24
		// see:  perl xml2text.pl; cat hydrant.txt 
		// 0. ist Header
		
		int latlon_i = 0;
		for (int i=1;i<datensatz.length;i++) {
			String t = (datensatz[i].split("\t")[0]).replaceAll("\\.", "");
			if (t.length()<8)
				t+="0000";
			latlon[latlon_i++]	=""+Integer.parseInt(t.substring(0, 8));
			t = (datensatz[i].split("\t")[1]).replaceAll("\\.", "");
			if (t.length()<8)
				t+="0000";
			latlon[latlon_i++]	=""+Integer.parseInt(t.substring(0,8));
			
			t = (datensatz[i].split("\t")[2])+"\t"+(datensatz[i].split("\t")[3]);
			latlon[latlon_i++]	= t;
			//System.out.println("latlon[]:"+latlon.length+","+i+", "+latlon[latlon_i-1]);
		}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return latlon;
	}
	
    public void pushString(String info) {
    	ClientConnectionManager clientConnectionManager;  
    	HttpContext context;  
    	HttpParams params;
    	SchemeRegistry schemeRegistry = new SchemeRegistry();

		// http scheme
		schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
		// https scheme
		schemeRegistry.register(new Scheme("https", new EasySSLSocketFactory(), 443));

		params = new BasicHttpParams();
		params.setParameter(ConnManagerPNames.MAX_TOTAL_CONNECTIONS, 1);
		params.setParameter(ConnManagerPNames.MAX_CONNECTIONS_PER_ROUTE, new ConnPerRouteBean(1));
		params.setParameter(HttpProtocolParams.USE_EXPECT_CONTINUE, false);
		HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
		HttpProtocolParams.setContentCharset(params, "utf8");

		// ignore that the ssl cert is self signed
		CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
		credentialsProvider.setCredentials(new AuthScope("yourServerHere.com", AuthScope.ANY_PORT),
				new UsernamePasswordCredentials("YourUserNameHere", "UserPasswordHere"));
		clientConnectionManager = new ThreadSafeClientConnManager(params, schemeRegistry);

		context = new BasicHttpContext();
		//context.setAttribute("http.auth.credentials-provider", credentialsProvider);
		
		try {
			DefaultHttpClient client = new DefaultHttpClient(clientConnectionManager, params);
			info = info.replaceAll(" ", "%20");
			HttpGet post = new HttpGet(urlStagingLiveSubmit+'?'+info);
			Log.v(LOG_TAG, "Submit: "+info+" zu "+urlStagingLiveSubmit);
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);    
			nameValuePairs.add(new BasicNameValuePair("hydrant", info));  
			//post.setEntity(new UrlEncodedFormEntity(nameValuePairs));  
		
			HttpResponse response = client.execute(post, context);
			String result = "";
        try{
            InputStream in = response.getEntity().getContent();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            StringBuilder str = new StringBuilder();
            String line = null;
            while((line = reader.readLine()) != null){
                //str.append(line + "\n");
                //Log.v("test",line);
            }
            in.close();
            result = str.toString();
        }catch(Exception ex){
            result = "Error";
        }
		
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
    
    private String getString(String url, String queryParams) {
    	
    	ClientConnectionManager clientConnectionManager;  
    	HttpContext context;  
    	HttpParams params;
    	String result = "";
    	SchemeRegistry schemeRegistry = new SchemeRegistry();

		// http scheme
		schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
		// https scheme
		schemeRegistry.register(new Scheme("https", new EasySSLSocketFactory(), 443));

		params = new BasicHttpParams();
		params.setParameter(ConnManagerPNames.MAX_TOTAL_CONNECTIONS, 1);
		params.setParameter(ConnManagerPNames.MAX_CONNECTIONS_PER_ROUTE, new ConnPerRouteBean(1));
		params.setParameter(HttpProtocolParams.USE_EXPECT_CONTINUE, false);
		HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
		HttpProtocolParams.setContentCharset(params, "utf8");

		// ignore that the ssl cert is self signed
		CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
		credentialsProvider.setCredentials(new AuthScope("yourServerHere.com", AuthScope.ANY_PORT),
				new UsernamePasswordCredentials("YourUserNameHere", "UserPasswordHere"));
		clientConnectionManager = new ThreadSafeClientConnManager(params, schemeRegistry);

		context = new BasicHttpContext();
		context.setAttribute("http.auth.credentials-provider", credentialsProvider);
		
		try {
		DefaultHttpClient client = new DefaultHttpClient(clientConnectionManager, params);
		HttpGet get = new HttpGet(url+"?"+queryParams);
		HttpResponse response = client.execute(get, context);
		
        try{
            InputStream in = response.getEntity().getContent();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            StringBuilder str = new StringBuilder();
            String line = null;
            while((line = reader.readLine()) != null){
                str.append(line+"\n");
                //Log.v("getString",line);
            }
            in.close();
            result = str.toString();
        }catch(Exception ex){
            result = "Error";
        }
		
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
    }
    
    private void sendString(String url, String queryParams) {
    	
    	ClientConnectionManager clientConnectionManager;  
    	HttpContext context;  
    	HttpParams params;
    	SchemeRegistry schemeRegistry = new SchemeRegistry();

		// http scheme
		schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
		// https scheme
		schemeRegistry.register(new Scheme("https", new EasySSLSocketFactory(), 443));

		params = new BasicHttpParams();
		params.setParameter(ConnManagerPNames.MAX_TOTAL_CONNECTIONS, 1);
		params.setParameter(ConnManagerPNames.MAX_CONNECTIONS_PER_ROUTE, new ConnPerRouteBean(1));
		params.setParameter(HttpProtocolParams.USE_EXPECT_CONTINUE, false);
		HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
		HttpProtocolParams.setContentCharset(params, "utf8");

		// ignore that the ssl cert is self signed
		CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
		credentialsProvider.setCredentials(new AuthScope("yourServerHere.com", AuthScope.ANY_PORT),
				new UsernamePasswordCredentials("YourUserNameHere", "UserPasswordHere"));
		clientConnectionManager = new ThreadSafeClientConnManager(params, schemeRegistry);

		context = new BasicHttpContext();
		context.setAttribute("http.auth.credentials-provider", credentialsProvider);
		
		try {
		DefaultHttpClient client = new DefaultHttpClient(clientConnectionManager, params);
		Log.d( LOG_TAG,"URL: "+url+queryParams.replaceAll(" ", "%20"));
		// URLEncoder.encode(queryParams, "utf-8")
		HttpGet get = new HttpGet(url+queryParams.replaceAll(" ", "%20"));
		client.execute(get, context);
		
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
    private void trustEveryone() {
        try {
                
                SSLContext context = SSLContext.getInstance("TLS");
                context.init(null, new X509TrustManager[]{new X509TrustManager(){
                        public void checkClientTrusted(X509Certificate[] chain,
                                        String authType) throws CertificateException {}
                        public void checkServerTrusted(X509Certificate[] chain,
                                        String authType) throws CertificateException {}
                        public X509Certificate[] getAcceptedIssuers() {
                                return new X509Certificate[0];
                        }}}, new SecureRandom());
                HttpsURLConnection.setDefaultSSLSocketFactory(
                                context.getSocketFactory());
        } catch (Exception e) { // should never happen
                e.printStackTrace();
        }
}

    /* NEW */
    public class EasySSLSocketFactory implements SocketFactory, LayeredSocketFactory {

    	private SSLContext sslcontext = null;

    	private SSLContext createEasySSLContext() throws IOException {
    		try {
    			SSLContext context = SSLContext.getInstance("TLS");
    			context.init(null, new TrustManager[] { new EasyX509TrustManager(null) }, null);
    			return context;
    		} catch (Exception e) {
    			throw new IOException(e.getMessage());
    		}
    	}

    	private SSLContext getSSLContext() throws IOException {
    		if (this.sslcontext == null) {
    			this.sslcontext = createEasySSLContext();
    		}
    		return this.sslcontext;
    	}
    	public Socket connectSocket(Socket sock, String host, int port, InetAddress localAddress, int localPort,
    			HttpParams params) throws IOException, UnknownHostException, ConnectTimeoutException {
    		int connTimeout = HttpConnectionParams.getConnectionTimeout(params);
    		int soTimeout = HttpConnectionParams.getSoTimeout(params);
    		InetSocketAddress remoteAddress = new InetSocketAddress(host, port);
    		SSLSocket sslsock = (SSLSocket) ((sock != null) ? sock : createSocket());

    		if ((localAddress != null) || (localPort > 0)) {
    			if (localPort < 0) {
    				localPort = 0; 
    			}
    			InetSocketAddress isa = new InetSocketAddress(localAddress, localPort);
    			sslsock.bind(isa);
    		}
    		sslsock.connect(remoteAddress, connTimeout);
    		sslsock.setSoTimeout(soTimeout);
    		return sslsock;

    	}
    	public Socket createSocket() throws IOException {
    		return getSSLContext().getSocketFactory().createSocket();
    	}
    	public boolean isSecure(Socket socket) throws IllegalArgumentException {
    		return true;
    	}
    	public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException,
    			UnknownHostException {
    		return getSSLContext().getSocketFactory().createSocket(socket, host, port, autoClose);
    	}
    	public boolean equals(Object obj) {
    		return ((obj != null) && obj.getClass().equals(EasySSLSocketFactory.class));
    	}

    	public int hashCode() {
    		return EasySSLSocketFactory.class.hashCode();
    	}

    }
    public class EasyX509TrustManager implements X509TrustManager {

    	private X509TrustManager standardTrustManager = null;
    	public EasyX509TrustManager(KeyStore keystore) throws NoSuchAlgorithmException, KeyStoreException {
    		super();
    		TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    		factory.init(keystore);
    		TrustManager[] trustmanagers = factory.getTrustManagers();
    		if (trustmanagers.length == 0) {
    			throw new NoSuchAlgorithmException("no trust manager found");
    		}
    		this.standardTrustManager = (X509TrustManager) trustmanagers[0];
    	}
    	public void checkClientTrusted(X509Certificate[] certificates, String authType) throws CertificateException {
    		standardTrustManager.checkClientTrusted(certificates, authType);
    	}
    	public void checkServerTrusted(X509Certificate[] certificates, String authType) throws CertificateException {
    		if ((certificates != null) && (certificates.length == 1)) {
    			certificates[0].checkValidity();
    		} else {
    			standardTrustManager.checkServerTrusted(certificates, authType);
    		}
    	}
    	public X509Certificate[] getAcceptedIssuers() {
    		return this.standardTrustManager.getAcceptedIssuers();
    	}

    }
    
}
