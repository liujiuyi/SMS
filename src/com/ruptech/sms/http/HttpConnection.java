package com.ruptech.sms.http;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import android.content.Context;
import android.util.Log;

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

	public Map<String, String> genParams(Map<String, String> params) {
		if (params == null) {
			params = new HashMap<>();
		}
		String loginid = Utils.LOGIN_ID;
		params.put("loginid", loginid);
		String sign = Utils.genSign(params, loginid);
		params.put("sign", sign);

		return params;
	}

	public String genRequestURL(String url, Map<String, String> params) {
		url += "?" + Utils.encodeParameters(params);
		return url;
	}

	protected Response get(String url, Map<String, String> params)
			throws Exception {
		params = genParams(params);
		url = genRequestURL(url, params);

		Log.w(TAG, url);
		Response response = http.get(url);
		return response;
	}

	public boolean sendSmsMessage(String message) throws Exception {
		Map<String, String> params = new HashMap<>();
		params.put("message", message);

		Response res = get(Utils.SERVER_URL + "/help/send_sms_message_to_email.php",
				params);

		JSONObject result = res.asJSONObject();
		boolean success = result.getBoolean("success");

		return success;
	}

}
