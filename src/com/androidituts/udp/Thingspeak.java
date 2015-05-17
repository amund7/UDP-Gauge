package com.androidituts.udp;

import java.util.ArrayList;

public class Thingspeak {
	
	private class Channel {
		public String number;
		public int pollingDelay;
		public String startupurl;
		public String url;
		public boolean enabled;
		
		public Channel(String Number,int PollingDelay,String StartupURL,String URL,boolean Enabled) {
			number=Number;
			pollingDelay=PollingDelay;
			startupurl=StartupURL;
			url=URL;
			enabled=Enabled;
			if (enabled)
				PollStartUp();
		}
		
		void Poll() {
			if (enabled)
				new GetJSONFromUrl().execute(url.replace("{0}", number));
		}
		
		void PollStartUp() {
			new GetJSONFromUrl().execute(startupurl.replace("{0}", number));			
		}
				
	}
	
	ArrayList<Channel> channels;

	Thingspeak() {
		channels=new ArrayList<Channel>();	
	}
	
	public void Enable(String Number, boolean Enable) {
		
	}

	public void PollAll() {
		for (Channel c:channels) {			
			c.Poll();
		}
	}
	
	public void Add(String number,int pollingdelay,String startupurl,String url,boolean Enabled) {
		channels.add(new Channel(number,pollingdelay,startupurl,url,Enabled));
	}
}

