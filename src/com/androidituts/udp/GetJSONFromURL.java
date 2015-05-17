package com.androidituts.udp;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.util.Log;


class GetJSONFromUrl extends AsyncTask<String, Void, String> {

	InputStream is = null;
	String result = "";
	String error_text="";
	JSONObject j = null;
	String URL="";
	String channel="0";

	protected String doInBackground(String... urls) {

		// http post
		try {

			/*if (!urls[0].contains("last"))
				UdpActivity.updatetrack("Loading: 0");*/
			URL=urls[0];
			channel=URL.substring(0, URL.indexOf("feed")).replaceAll("\\D+","");
			HttpClient httpclient = new DefaultHttpClient(new BasicHttpParams());
			HttpGet httpget = new HttpGet(urls[0]);
			httpget.setHeader("Content-type", "application/json");

			System.out.println("Getting...");
			HttpResponse response = httpclient.execute(httpget);
			HttpEntity entity = response.getEntity();
			//HttpParams myParams = new BasicHttpParams();
			/*HttpConnectionParams.setConnectionTimeout(myParams, 10000);
			HttpConnectionParams.setSoTimeout(myParams, 10000);*/
			
			is = entity.getContent();
			System.out.println("Done.");
			System.out.println("Building string...");
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					is, "UTF-8"), 8);
			StringBuilder sb = new StringBuilder();
			int bytes,totalbytes=0;
			char [] buf=new char[1024];
			//long bytes=0;
			while ((bytes = reader.read(buf)) != -1) {
				sb.append(buf, 0, bytes);
				totalbytes++; // counts kilobytes
				/*if (!urls[0].contains("last"))
					UdpActivity.updatetrack("Loading:"+totalbytes);*/
			}
			is.close();
			result = sb.toString();
			System.out.println("Done.");
			
		} catch (Exception e) {
			Log.e("log_tag", "Error in http connection " + e.toString());
		}
		return "";
	}

	protected void onPostExecute(String s) {
		try {
			//super.onPostExecute(result);
			
			System.out.println("Parsing JSON channel "+channel+"...");
			if (result!=null) {
				JSONObject jObject;
				try {
					jObject = new JSONObject(result);

					if (jObject.has("channel")) {
						JSONObject ch=jObject.getJSONObject("channel");
						Iterator <String> keys = ch.keys();
						while (keys.hasNext()) {
							String key=keys.next();						
							if (key.contains("field")) 
								UdpActivity.updatetrack(channel+key+"/"+ch.getString(key));
						}						
					}
					
					if (jObject.has("feeds")) {
						JSONArray jArray=jObject.getJSONArray("feeds");
						String time="";
						for(int i = 0; i<jArray.length(); i++) {
							JSONObject jRow=jArray.getJSONObject(i);
							Iterator <String> keys = jRow.keys();
							String date=jRow.getString("created_at");		// 2014-10-13T05:10:00Z
							SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'Z");									
							//Log.d("UDPTIME", "date "+date);
							Date parsedDate=f.parse(date+"+0000");
							time=parsedDate.getTime()+"";
							//Log.d("UDPTIME", "long "+time);
							while (keys.hasNext()) {
								String key=keys.next();				
								if (key.contains("field")) 
									UdpActivity.updatetrack(channel+key+":"+jRow.getString(key)+":"+time);
							}
						}
					}
					else {
					//jObject = new JSONObject(result);
						Iterator <String> keys = jObject.keys();
						String time="";
							String date=jObject.getString("created_at");		// 2014-10-13T05:10:00Z
							SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'Z");									
							//Log.d("UDPTIME", "date "+date);
							Date parsedDate=f.parse(date+"+0000");
							time=parsedDate.getTime()+"";
						
						while (keys.hasNext()) {
							String key=keys.next();
							//Log.d("UDPTIME", "key "+key);
							if (key.contains("field")) 
								UdpActivity.updatetrack(channel+key+":"+jObject.getString(key)+":"+time);
								///UdpActivity.updatetrack(key+":"+jObject.getString(key));
						}
					}
/*					for(int i = 0; i<jObject.length(); i++) {
						String key = jObject.next();
						if (key.contains("field")) 
							UdpActivity.updatetrack(key+":"+jObject.getString(key));
					}*/

					System.out.println("Done.");
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					Log.e("log_tag", "Error parsing data " + e.toString());
					e.printStackTrace();
				}
			}
			
		} catch (Exception e) {
			Log.e("log_tag", "Error converting result " + e.toString());
		}

	}
	
	
	public static Map<String,String> parse(JSONObject json , Map<String,String> out) throws JSONException{
	    Iterator<String> keys = json.keys();
	    while(keys.hasNext()){
	        String key = keys.next();
	        String val = null;
	        try{
	             JSONObject value = json.getJSONObject(key);
	             parse(value,out);
	        }catch(Exception e){
	            val = json.getString(key);
	            System.out.println(key);
	        }

	        if(val != null){
	            out.put(key,val);
	        }
	    }
	    return out;
	}

}


