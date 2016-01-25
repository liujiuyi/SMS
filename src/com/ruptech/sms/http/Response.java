package com.ruptech.sms.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.util.CharArrayBuffer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;

import android.util.Log;

import com.ruptech.sms.utils.Utils;

public class Response {
	private static Pattern escaped = Pattern.compile("&#([0-9]{3,5});");

	public static final String TAG = Utils.CATEGORY + Response.class.getSimpleName();

	/**
	 * EntityUtils.toString(entity, "UTF-8");
	 * 
	 * @param entity
	 * @return
	 * @throws IOException
	 * @throws Exception
	 */
	public static String entityToString(final HttpEntity entity) throws IOException {
		// DebugTimer.betweenStart("AS STRING");
		if (null == entity) {
			throw new IllegalArgumentException("HTTP entity may not be null");
		}
		InputStream instream = entity.getContent();
		// InputStream instream = asStream(entity);
		if (instream == null) {
			return "";
		}
		if (entity.getContentLength() > Integer.MAX_VALUE) {
			throw new IllegalArgumentException("HTTP entity too large to be buffered in memory");
		}

		int i = (int) entity.getContentLength();
		if (i < 0) {
			i = 4096;
		}
		Log.i("LDS", i + " content length");

		Reader reader = new BufferedReader(new InputStreamReader(instream, "UTF-8"));
		CharArrayBuffer buffer = new CharArrayBuffer(i);
		try {
			char[] tmp = new char[1024];
			int l;
			while ((l = reader.read(tmp)) != -1) {
				buffer.append(tmp, 0, l);
			}
		} finally {
			reader.close();
		}

		// DebugTimer.betweenEnd("AS STRING");
		return buffer.toString();
	}

	/**
	 * Unescape UTF-8 escaped characters to string.
	 * 
	 * @author pengjianq...@gmail.com
	 * 
	 * @param original
	 *            The string to be unescaped.
	 * @return The unescaped string
	 */
	public static String unescape(String original) {
		Matcher mm = escaped.matcher(original);
		StringBuffer unescaped = new StringBuffer();
		while (mm.find()) {
			mm.appendReplacement(unescaped, Character.toString((char) Integer.parseInt(mm.group(1), 10)));
		}
		mm.appendTail(unescaped);
		return unescaped.toString();
	}

	private final HttpResponse mResponse;

	private final boolean mStreamConsumed = false;

	public Response(HttpResponse res) {
		mResponse = res;
	}

	/**
	 * @return
	 * @throws Exception
	 * @throws
	 */
	public Document asDocument() throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document dom = builder.parse(asStream());
		return dom;
	}

	public JSONArray asJSONArray() throws RuntimeException {
		try {
			return new JSONArray(asString());
		} catch (Exception jsone) {
			throw new RuntimeException(jsone.getMessage(), jsone);
		}
	}

	public JSONObject asJSONObject() throws Exception {
		try {
			return new JSONObject(asString());
		} catch (JSONException jsone) {
			throw new Exception(jsone.getMessage() + ":" + asString(), jsone);
		}
	}

	/**
	 * Convert Response to inputStream
	 * 
	 * @return InputStream or null
	 * @throws Exception
	 */
	public InputStream asStream() throws Exception {
		try {
			final HttpEntity entity = mResponse.getEntity();
			if (entity != null) {
				return entity.getContent();
			}
		} catch (IllegalStateException e) {
			throw new Exception(e.getMessage(), e);
		} catch (IOException e) {
			throw new Exception(e.getMessage(), e);
		}
		return null;
	}

	/**
	 * Convert Response to Context String
	 * 
	 * @return response context string or null
	 * @throws Exception
	 */
	public String asString() throws RuntimeException {
		try {
			String s = Response.entityToString(mResponse.getEntity());
			Log.v(TAG, s);
			return s;
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	public boolean isStreamConsumed() {
		return mStreamConsumed;
	}

}
