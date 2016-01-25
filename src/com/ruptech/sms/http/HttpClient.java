package com.ruptech.sms.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.SSLHandshakeException;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.HttpVersion;
import org.apache.http.NoHttpResponseException;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;

import android.util.Log;

import com.ruptech.sms.utils.Utils;

/**
 * Wrap of org.apache.http.impl.client.DefaultHttpClient
 * 
 * @author lds
 * 
 */
public class HttpClient {

	static class GzipDecompressingEntity extends HttpEntityWrapper {

		public GzipDecompressingEntity(final HttpEntity entity) {
			super(entity);
		}

		@Override
		public InputStream getContent() throws IOException, IllegalStateException {

			// the wrapped entity's getContent() decides about repeatability
			InputStream wrappedin = wrappedEntity.getContent();
			return new GZIPInputStream(wrappedin);
		}

		@Override
		public long getContentLength() {
			// length of ungzipped content is not known
			return -1;
		}

	}

	/** Bad Gateway: Weibo is down or being upgraded. */
	public static final int BAD_GATEWAY = 502;

	/**
	 * Bad Request: The request was invalid. An accompanying error message will
	 * explain why. This is the status code will be returned during rate
	 * limiting.
	 */
	public static final int BAD_REQUEST = 400;
	private static final int CONNECTION_TIMEOUT_MS = 60 * 1000;
	private final static boolean DEBUG = true;
	/**
	 * Forbidden: The request is understood, but it has been refused. An
	 * accompanying error message will explain why.
	 */
	public static final int FORBIDDEN = 403;
	private static HttpResponseInterceptor gzipResponseIntercepter = new HttpResponseInterceptor() {

		@Override
		public void process(HttpResponse response, HttpContext context) {
			HttpEntity entity = response.getEntity();
			Header ceheader = entity.getContentEncoding();
			if (ceheader != null) {
				HeaderElement[] codecs = ceheader.getElements();
				for (int i = 0; i < codecs.length; i++) {
					if (codecs[i].getName().equalsIgnoreCase("gzip")) {
						response.setEntity(new GzipDecompressingEntity(response.getEntity()));
						return;
					}
				}
			}

		}
	};
	/**
	 * Internal Server Error: Something is broken. Please post to the group so
	 * the Weibo team can investigate.
	 */
	public static final int INTERNAL_SERVER_ERROR = 500;
	private static boolean isAuthenticationEnabled = false;
	/**
	 * Not Acceptable: Returned by the Search API when an invalid format is
	 * specified in the request.
	 */
	public static final int NOT_ACCEPTABLE = 406;
	/** Not Authorized: Authentication credentials were missing or incorrect. */
	public static final int NOT_AUTHORIZED = 401;
	/**
	 * Not Found: The URI requested is invalid or the resource requested, such
	 * as a user, does not exists.
	 */
	public static final int NOT_FOUND = 404;

	/** Not Modified: There was no new data to return. */
	public static final int NOT_MODIFIED = 304;
	/** OK: Success! */
	public static final int OK = 200;

	/**
	 * 异常自动恢复处理, 使用HttpRequestRetryHandler接口实现请求的异常恢复
	 */
	private static HttpRequestRetryHandler requestRetryHandler = new HttpRequestRetryHandler() {
		// 自定义的恢复策略
		@Override
		public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
			// 设置恢复策略，在发生异常时候将自动重试N次
			if (executionCount >= RETRIED_TIME) {
				// Do not retry if over max retry count
				return false;
			}
			if (exception instanceof NoHttpResponseException) {
				// Retry if the server dropped connection on us
				return true;
			}
			if (exception instanceof SSLHandshakeException) {
				// Do not retry on SSL handshake exception
				return false;
			}
			HttpRequest request = (HttpRequest) context.getAttribute(ExecutionContext.HTTP_REQUEST);
			boolean idempotent = (request instanceof HttpEntityEnclosingRequest);
			if (!idempotent) {
				// Retry if the request is considered idempotent
				return true;
			}
			return false;
		}
	};
	public static final int RETRIED_TIME = 3;

	public static final int RETRIEVE_LIMIT = 20;
	/**
	 * Service Unavailable: The Weibo servers are up, but overloaded with
	 * requests. Try again later. The search and trend methods use this to
	 * indicate when you are being rate limited.
	 */
	public static final int SERVICE_UNAVAILABLE = 503;

	private static final int SOCKET_TIMEOUT_MS = 60 * 1000;
	protected static final String TAG = Utils.CATEGORY + HttpClient.class.getSimpleName();

	public static String decode(String value) throws RuntimeException {
		try {
			return URLDecoder.decode(value, HTTP.UTF_8);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	public static String encode(String value) throws RuntimeException {
		try {
			return URLEncoder.encode(value, HTTP.UTF_8);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	public static String encodeParameters(ArrayList<BasicNameValuePair> params) throws RuntimeException {
		StringBuffer buf = new StringBuffer();
		for (int j = 0; j < params.size(); j++) {
			if (j != 0) {
				buf.append("&");
			}
			BasicNameValuePair param = params.get(j);
			if (param.getValue() != null) {
				try {
					buf.append(URLEncoder.encode(param.getName(), "UTF-8")).append("=")
							.append(URLEncoder.encode(param.getValue(), "UTF-8"));
				} catch (java.io.UnsupportedEncodingException neverHappen) {
					throw new RuntimeException(neverHappen.getMessage(), neverHappen);
				}
			}
		}
		return buf.toString();
	}

	/**
	 * 解析HTTP错误码
	 * 
	 * @param statusCode
	 * @return
	 */
	private static String getCause(int statusCode) {
		String cause = null;
		switch (statusCode) {
		case NOT_MODIFIED:
			break;
		case BAD_REQUEST:
			cause = "The request was invalid.  An accompanying error message will explain why. This is the status code will be returned during rate limiting.";
			break;
		case NOT_AUTHORIZED:
			cause = "Authentication credentials were missing or incorrect.";
			break;
		case FORBIDDEN:
			cause = "The request is understood, but it has been refused.  An accompanying error message will explain why.";
			break;
		case NOT_FOUND:
			cause = "The URI requested is invalid or the resource requested, such as a user, does not exists.";
			break;
		case NOT_ACCEPTABLE:
			cause = "Returned by the Search API when an invalid format is specified in the request.";
			break;
		case INTERNAL_SERVER_ERROR:
			cause = "Something is broken.  Please post to the group so the dev team can investigate.";
			break;
		case BAD_GATEWAY:
			cause = "Weibo is down or being upgraded.";
			break;
		case SERVICE_UNAVAILABLE:
			cause = "Service Unavailable: The Weibo servers are up, but overloaded with requests. Try again later. The search and trend methods use this to indicate when you are being rate limited.";
			break;
		default:
			cause = "";
		}
		return statusCode + ":" + cause;
	}

	public static void log(String msg) {
		if (DEBUG) {
			Log.w(TAG, msg);
		}
	}

	private BasicHttpContext localcontext;

	private DefaultHttpClient mClient;

	private String mPassword;

	private String mUserId;

	public HttpClient() {
		prepareHttpClient();
	}

	/**
	 * Create request method, such as POST, GET, DELETE
	 * 
	 * @param httpMethod
	 *            "GET","POST","DELETE"
	 * @param uri
	 *            请求的URI
	 * @param postParams
	 *            POST参数
	 * @return httpMethod Request implementations for the various HTTP methods
	 *         like GET and POST.
	 * @throws Exception
	 *             createMultipartEntity 或 UrlEncodedFormEntity引发的IOException
	 */
	private HttpUriRequest createMethod(String httpMethod, URI uri, ArrayList<BasicNameValuePair> postParams)
			throws RuntimeException {

		HttpUriRequest method;

		if (httpMethod.equalsIgnoreCase(HttpPost.METHOD_NAME)) {
			// POST METHOD

			HttpPost post = new HttpPost(uri);
			// See this:
			// http://groups.google.com/group/twitter-development-talk/browse_thread/thread/e178b1d3d63d8e3b
			post.getParams().setBooleanParameter("http.protocol.expect-continue", false);

			try {
				HttpEntity entity = null;
				if (null != postParams) {
					entity = new UrlEncodedFormEntity(postParams, HTTP.UTF_8);
				}
				post.setEntity(entity);
			} catch (IOException ioe) {
				throw new RuntimeException(ioe.getMessage(), ioe);
			}

			method = post;
		} else if (httpMethod.equalsIgnoreCase(HttpDelete.METHOD_NAME)) {
			method = new HttpDelete(uri);
		} else {
			method = new HttpGet(uri);
		}

		return method;
	}

	/**
	 * CreateURI from URL string
	 * 
	 * @param url
	 * @return request URI
	 * @throws Exception
	 *             Cause by URISyntaxException
	 */
	private URI createURI(String url) throws RuntimeException {
		URI uri;

		try {
			uri = new URI(url);
		} catch (URISyntaxException e) {
			Log.e(TAG, e.getMessage(), e);
			throw new RuntimeException("Invalid URL.");
		}

		return uri;
	}

	private void enableDebug() {
		Log.w(TAG, "enable apache.http debug");

		java.util.logging.Logger.getLogger("org.apache.http").setLevel(java.util.logging.Level.FINEST);
		java.util.logging.Logger.getLogger("org.apache.http.wire").setLevel(java.util.logging.Level.FINER);
		java.util.logging.Logger.getLogger("org.apache.http.headers").setLevel(java.util.logging.Level.OFF);

		/*
		 * System.setProperty("log.tag.org.apache.http", "VERBOSE");
		 * System.setProperty("log.tag.org.apache.http.wire", "VERBOSE");
		 * System.setProperty("log.tag.org.apache.http.headers", "VERBOSE");
		 * 
		 * 在这里使用System.setProperty设置不会生效, 原因不明, 必须在终端上输入以下命令方能开启http调试信息: > adb
		 * shell setprop log.tag.org.apache.http VERBOSE > adb shell setprop
		 * log.tag.org.apache.http.wire VERBOSE > adb shell setprop
		 * log.tag.org.apache.http.headers VERBOSE
		 */
	}

	public Response get(String url) throws Exception {
		return httpRequest(url, null, false, HttpGet.METHOD_NAME);
	}

	public Response get(String url, ArrayList<BasicNameValuePair> params) throws Exception {
		return httpRequest(url, params, false, HttpGet.METHOD_NAME);
	}

	public Response get(String url, ArrayList<BasicNameValuePair> params, boolean authenticated) throws Exception {
		return httpRequest(url, params, authenticated, HttpGet.METHOD_NAME);
	}

	public Response get(String url, boolean authenticated) throws RuntimeException {
		return httpRequest(url, null, authenticated, HttpGet.METHOD_NAME);
	}

	/**
	 * @return authed user password
	 */
	public String getPassword() {
		return mPassword;
	}

	/**
	 * @return authed user id
	 */
	public String getUserId() {
		return mUserId;
	}

	/**
	 * Handle Status code
	 * 
	 * @param statusCode
	 *            响应的状态码
	 * @param res
	 *            服务器响应
	 * @throws Exception
	 *             当响应码不为200时都会报出此异常:<br />
	 *             <li>HttpRequestException, 通常发生在请求的错误,如请求错误了 网址导致404等, 抛出此异常,
	 *             首先检查request log, 确认不是人为错误导致请求失败</li> <li>Exception,
	 *             通常发生在Auth失败, 检查用于验证登录的用户名/密码/KEY等</li> <li>
	 *             Exception, 通常发生在服务器接受到请求, 但拒绝请求, 可是多种原因, 具体原因 服务器会返回拒绝理由,
	 *             调用Exception#getError#getMessage查看</li> <li>
	 *             Exception, 通常发生在服务器发生错误时, 检查服务器端是否在正常提供服务</li> <li>
	 *             Exception, 其他未知错误.</li>
	 */
	private void HandleResponseStatusCode(int statusCode, Response res) throws RuntimeException {
		String msg = getCause(statusCode) + "\n";

		switch (statusCode) {
		// It's OK, do nothing
		case OK:
			break;

		// Mine mistake, Check the Log
		case NOT_MODIFIED:
		case BAD_REQUEST:
		case NOT_FOUND:
		case NOT_ACCEPTABLE:
			throw new RuntimeException(msg + res.asString() + ", statusCode:" + statusCode);

			// UserName/Password incorrect
		case NOT_AUTHORIZED:
			throw new RuntimeException(msg + res.asString() + ", statusCode:" + statusCode);

			// Server will return a error message, use
			// Exception#getError() to see.
		case FORBIDDEN:
			throw new RuntimeException(msg + ", statusCode:" + statusCode);

			// Something wrong with server
		case INTERNAL_SERVER_ERROR:
		case BAD_GATEWAY:
		case SERVICE_UNAVAILABLE:
			throw new RuntimeException(msg + ", statusCode:" + statusCode);

			// Others
		default:
			throw new RuntimeException(msg + res.asString() + ", statusCode:" + statusCode);
		}
	}

	/**
	 * Execute the DefaultHttpClient
	 * 
	 * @param url
	 *            target
	 * @param postParams
	 * @param authenticated
	 *            need or not
	 * @param httpMethod
	 *            HttpPost.METHOD_NAME HttpGet.METHOD_NAME
	 *            HttpDelete.METHOD_NAME
	 * @return Response from server
	 * @throws Exception
	 *             此异常包装了一系列底层异常 <br />
	 * <br />
	 *             1. 底层异常, 可使用getCause()查看: <br />
	 *             <li>URISyntaxException, 由`new URI` 引发的.</li> <li>IOException,
	 *             由`createMultipartEntity` 或 `UrlEncodedFormEntity` 引发的.</li>
	 *             <li>IOException和ClientProtocolException,
	 *             由`HttpClient.execute` 引发的.</li><br />
	 * 
	 *             2. 当响应码不为200时报出的各种子类异常: <li>HttpRequestException,
	 *             通常发生在请求的错误,如请求错误了 网址导致404等, 抛出此异常, 首先检查request log,
	 *             确认不是人为错误导致请求失败</li> <li>Exception, 通常发生在Auth失败,
	 *             检查用于验证登录的用户名/密码/KEY等</li> <li>Exception, 通常发生在服务器接受到请求,
	 *             但拒绝请求, 可是多种原因, 具体原因 服务器会返回拒绝理由,
	 *             调用Exception#getError#getMessage查看</li> <li>
	 *             Exception, 通常发生在服务器发生错误时, 检查服务器端是否在正常提供服务</li> <li>
	 *             Exception, 其他未知错误.</li>
	 */
	public Response httpRequest(String url, ArrayList<BasicNameValuePair> postParams, boolean authenticated,
			String httpMethod) throws RuntimeException {
		if (DEBUG) {
			// DebugTimer.betweenStart("HTTP");
		}

		URI uri = createURI(url);

		HttpResponse response = null;
		Response res = null;
		HttpUriRequest method = null;

		// Create POST, GET or DELETE METHOD
		method = createMethod(httpMethod, uri, postParams);
		// Setup ConnectionParams, Request Headers
		SetupHTTPConnectionParams(method);

		// Execute Request
		try {
			// 加入OAuth认证信息
			if (authenticated) {
				// mOAuthClient.signRequest(method);
			}
			response = mClient.execute(method, localcontext);
			res = new Response(response);
		} catch (ClientProtocolException e) {
			Log.e(TAG, e.getMessage(), e);
			throw new RuntimeException(e.getMessage(), e);
		} catch (IOException ioe) {
			throw new RuntimeException(ioe.getMessage(), ioe);
		}

		if (response != null) {
			int statusCode = response.getStatusLine().getStatusCode();
			// It will throw a weiboException while status code is not 200
			HandleResponseStatusCode(statusCode, res);
		} else {
			Log.e(TAG, "response is null");
		}

		if (DEBUG) {
			// DebugTimer.betweenEnd("HTTP");
		}
		return res;
	}

	public boolean isAuthenticationEnabled() {
		return isAuthenticationEnabled;
	}

	public Response post(String url) throws Exception {
		return httpRequest(url, null, false, HttpPost.METHOD_NAME);
	}

	public Response post(String url, ArrayList<BasicNameValuePair> params) throws Exception {
		return httpRequest(url, params, true, HttpPost.METHOD_NAME);
	}

	public Response post(String url, ArrayList<BasicNameValuePair> postParams, boolean authenticated) throws Exception {
		if (null == postParams) {
			postParams = new ArrayList<BasicNameValuePair>();
		}
		return httpRequest(url, postParams, authenticated, HttpPost.METHOD_NAME);
	}

	public Response post(String url, boolean authenticated) throws Exception {
		return httpRequest(url, null, authenticated, HttpPost.METHOD_NAME);
	}

	/**
	 * Setup DefaultHttpClient
	 * 
	 * Use ThreadSafeClientConnManager.
	 * 
	 */
	private void prepareHttpClient() {
		if (DEBUG) {
			enableDebug();
		}

		// Create and initialize HTTP parameters
		HttpParams params = new BasicHttpParams();
		ConnManagerParams.setMaxTotalConnections(params, 10);
		HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);

		// Create and initialize scheme registry
		SchemeRegistry schemeRegistry = new SchemeRegistry();
		schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
		schemeRegistry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));

		// Create an HttpClient with the ThreadSafeClientConnManager.
		ThreadSafeClientConnManager cm = new ThreadSafeClientConnManager(params, schemeRegistry);
		mClient = new DefaultHttpClient(cm, params);
		mClient.getCredentialsProvider().setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
				new UsernamePasswordCredentials("zhaolei", "zhaolei"));

		// Support GZIP
		mClient.addResponseInterceptor(gzipResponseIntercepter);

		// TODO: need to release this connection in httpRequest()
		// cm.releaseConnection(conn, validDuration, timeUnit);
		// httpclient.getConnectionManager().shutdown();
	}

	public void removeProxy() {
		mClient.getParams().removeParameter(ConnRoutePNames.DEFAULT_PROXY);
	}

	/**
	 * Empty the credentials
	 */
	public void reset() {
		setCredentials("", "");
	}

	/**
	 * Setup Credentials for HTTP Basic Auth
	 * 
	 * @param username
	 * @param password
	 */
	public void setCredentials(String username, String password) {
		mUserId = username;
		mPassword = password;
		// FOR TEST: disable Basic auth
		// mClient.getCredentialsProvider().setCredentials(mAuthScope,
		// new UsernamePasswordCredentials(username, password));
		isAuthenticationEnabled = true;
	}

	/**
	 * @param hostname
	 *            the hostname (IP or DNS name)
	 * @param port
	 *            the port number. -1 indicates the scheme default port.
	 * @param scheme
	 *            the name of the scheme. null indicates the default scheme
	 */
	public void setProxy(String host, int port, String scheme) {
		HttpHost proxy = new HttpHost(host, port, scheme);
		mClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
	}

	/**
	 * Setup HTTPConncetionParams
	 * 
	 * @param method
	 */
	private void SetupHTTPConnectionParams(HttpUriRequest method) {
		HttpConnectionParams.setConnectionTimeout(method.getParams(), CONNECTION_TIMEOUT_MS);
		HttpConnectionParams.setSoTimeout(method.getParams(), SOCKET_TIMEOUT_MS);
		mClient.setHttpRequestRetryHandler(requestRetryHandler);
		method.addHeader("Accept-Encoding", "gzip, deflate");
		method.addHeader("Accept-Charset", "UTF-8,*;q=0.5");
	}

}