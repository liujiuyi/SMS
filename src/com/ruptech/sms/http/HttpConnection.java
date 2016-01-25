package com.ruptech.sms.http;

import java.util.ArrayList;

import org.apache.http.HttpException;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;

import com.ruptech.sms.App;
import com.ruptech.sms.utils.Utils;

public class HttpConnection implements java.io.Serializable {
	private static final long serialVersionUID = 6352073463192119964L;
	public static final String TAG = Utils.CATEGORY
			+ HttpConnection.class.getSimpleName();

	protected HttpClient http = null;

	public HttpConnection(Context mContext) {
		super();
		http = new HttpClient();
	}

	protected Response get(String url, ArrayList<BasicNameValuePair> params,
			boolean authenticated) throws HttpException {
		if (null != params && params.size() > 0) {
			if (url.indexOf("?") == -1) {
				url += "?" + HttpClient.encodeParameters(params);
			} else {
				url += "&" + HttpClient.encodeParameters(params);
			}
		}
		Log.w(TAG, url);
		Response response = http.get(url, authenticated);
		return response;
	}

	public boolean sendSmsMessage(String message) throws Exception {
		ArrayList<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
		params.add(new BasicNameValuePair("message", message));

		Response res = get(Utils.SERVER_URL + "/help/send_sms_message.php",
				params, false);

		JSONObject result = res.asJSONObject();
		boolean success = result.getBoolean("success");

		return success;
	}

}
