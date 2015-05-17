package com.androidituts.udp;

import java.util.ArrayList;

public class Sensor implements Comparable<Sensor> {
	
	Sensor(String ID,float v,long time,int order) { 
		id=ID; 
		description=ID;
		value=v;
		max=v; min=v;
		changed=true;
		graphchanged=true;
		history=new ArrayList<Number>();
		//timestamps=new ArrayList<Long>();
		loginterval=1;
		timestamp=globaltimestamp=System.currentTimeMillis(); 
		timesupdated=0;
		timeout=UdpActivity.udpTimeout;
		acc=0;
		uninitialized=false;
		createcounter++;
		sortorder=order;
		
		setValue(v,time);
		
	}

	Sensor(String ID,String desc,int order) { 
		id=ID;
		description=desc;
		history=new ArrayList<Number>();
		//timestamps=new ArrayList<Long>();
		loginterval=1;
		timestamp=globaltimestamp=System.currentTimeMillis(); 
		timesupdated=0;
		acc=0;
		uninitialized=true;
		createcounter++;
		sortorder=order;
	}

	String id;
	// private:
	float value,lastvalue;
	public float max;
	public float min;
	boolean changed,graphchanged;
	long timestamp;
	static long globaltimestamp; // Last update of any sensor
	long timeout;
	ArrayList<Number> history;
	ArrayList<Long> timestamps;
	int historystep;
	static int loginterval;
	int timesupdated;
	float acc;
	String description;
	boolean uninitialized;
	int sortorder;
	static int createcounter;
	
	//public:
	float getValue() {
		changed=false;
		return value;
	}
	float getMax() { return max; }
	float getMin() { return min; }
	
	void setValue(float v, long time) {
		//if (time<System.currentTimeMillis()-(24*60*60*1000)) 
		//	return;
		if (uninitialized) { // set max/min to v if this instance was constructed by description only 
			max=v;
			min=v;
			uninitialized=false;
		}
		timesupdated++;
		value=v;
		acc+=v;
		changed = (!(lastvalue==value));
		lastvalue=value;
		if (value>max) {
			max=value;
		}		
		if (value<min) {
			min=value;
		}
		//Log.d("UDPSensor", time+"");            	

		timestamp=globaltimestamp=System.currentTimeMillis();

		if (++historystep>=loginterval) {
			if (loginterval==1) {
				history.add(time);
				history.add(v);
				acc=0;
			}
			else {
				history.add(time);
				history.add(acc/loginterval);
				acc=0;
			}
			
			while (history.size()>1000) { // Delete if more than 500 datapoints (x,y)
				history.remove(0);
				history.remove(0);
			}
			
			while ((history.size()>4)&& // Delete entries older than 24h
					(history.get(0).longValue()<(history.get(history.size()-2).longValue()-(24*60*60*1000)))) {
				history.remove(0);
				history.remove(0);
			}
			
			
			/*while (timestamps.size()>500)
				timestamps.remove(0);*/
			historystep=0;		
			graphchanged=true; // force screen update if graph point was added, even if value wasn't actually changed
		}
		//if (changed) {
			if (id.contains("field4:"))
					UdpActivity.notificationText=description+" "+v+"%";
			if (id.contains("field3:"))
				UdpActivity.notification(description+" "+v,v);
		//}
		
	}
	
/*	boolean failed() {	// Returns true after 5 fails
		fails++;
		return (fails>=5);
	}*/
	
	long timesinceupdated() {
		return System.currentTimeMillis()-timestamp;
	}
	
	boolean timedout() {
		//Log.d("timedout()", id+" "+(System.currentTimeMillis()-timestamp) + "timeout=" + timeout);
		return (System.currentTimeMillis()-timestamp>timeout);
	}

	@Override
	public int compareTo(Sensor arg0) {		
		return sortorder-arg0.sortorder;
	}
	

};
