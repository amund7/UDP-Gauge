package com.androidituts.udp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.DigitalClock;
import android.widget.GridView;
import android.widget.ToggleButton;



public class UdpActivity extends Activity implements OnLongClickListener {
	public static int SERVERPORT = 4444;
	public static int udpTimeout;
	boolean thingspeakStartupEnabled;
	String thingspeakStartupURL;
	boolean thingspeakEnabled;
	String thingspeakURL;
	int thingspeakInterval;

	// public EditText input;
	public GridView gridView1;
	public boolean start;
	public static Handler Handler;
	Server serverThread;
	ArrayList<Sensor> sensors;
	public GaugeAdapter adapter;
	static long lastTimeMillis;
	ToggleButton button1;

	public static boolean showgraph=true;
	public static boolean numberofsensorschanged=false;

	private SharedPreferences mPrefs;

	private static boolean activityVisible=true;
	static int numColumns=2;

	// Notification stuff
    final String ACTION = "NotifyServiceAction";
    static NotificationManager notificationManager;
    static Notification myNotification;
    static String notificationText;
    static String notificationTitle;
    final static String myBlog = "http://android-er.blogspot.com/";
    final static int MY_NOTIFICATION_ID=1;
    static Context context;
    static TextDrawable textDrawable;
    
    static boolean istablet; // cache this one, because we will be polling it lots from GaugeAdapter
    
    // Bluetooth stuff
    BluetoothAdapter mBluetoothAdapter;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    OutputStream mmOutputStream;
    InputStream mmInputStream;
    Thread workerThread;
    volatile boolean stopWorker;

    // thingspeak stuff
	//Handler thingspeakhandler;
    
	
	@Override
    public void onDestroy() {
    	if (serverThread!=null)
    		serverThread.run=false;
    	super.onDestroy();
    }

	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		
		// Notification stuff
		notificationManager =
	    	      (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		context = getApplicationContext();
		
		istablet=isTablet(context); // cache this one, because we will be polling it lots from GaugeAdapter
		
		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		//new GetJSONFromUrl();

		setContentView(R.layout.main);

		/*Map<String, ?> allEntries = mPrefs.getAll();
		// If prefs are empty (= first start), add a default Thingspeak channel.
		if (allEntries.isEmpty()) {
			SharedPreferences.Editor ed = mPrefs.edit();
			ed.putString("channel0", "12983");
			ed.putBoolean("channel0Enabled", true);
		}*/

	    PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

		getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		sensors= new ArrayList<Sensor>();
		//for (int i=0; i<10; i++) sensors[i]=new Sensor("Test"+i);
		gridView1=(GridView)findViewById(R.id.gridView1);
		adapter = new GaugeAdapter(this, sensors);
		gridView1.setAdapter(adapter);

		thingspeakStartupEnabled=mPrefs.getBoolean("thingspeak_startup_enabled", false);
		thingspeakStartupURL=mPrefs.getString("thingspeak_startup_url","");
		thingspeakEnabled=mPrefs.getBoolean("thingspeak_enabled", false);
		thingspeakURL=mPrefs.getString("thingspeak_url","");
		thingspeakInterval=Integer.parseInt(mPrefs.getString("thingspeak_interval", ""))*1000;
		
		SERVERPORT=Integer.parseInt(mPrefs.getString("udp_port", "4444"));
		udpTimeout=Integer.parseInt(mPrefs.getString("udp_timeout", "30"))*1000;		
		Log.d("UDPprefs", "udp_timeout: "+mPrefs.getString("udp_timeout", "0"));

		numColumns=mPrefs.getInt("numColumns",2);		
		gridView1.setNumColumns(numColumns);

		DigitalClock digitalClock=(DigitalClock)findViewById(R.id.digitalClock1);
		digitalClock.setVisibility(mPrefs.getInt("clockVisible", 8));

		
		registerForContextMenu(gridView1);
		
		
		start=false;
		new Thread(serverThread=new Server()).start();
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) { }

		
		final Thingspeak thingspeak=new Thingspeak();	

		Map<String, ?> allEntries = mPrefs.getAll();
		//allEntries=mPrefs.getAll();
		for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
		    Log.d("map values", entry.getKey() + ": " + entry.getValue().toString());
			if ((entry.getKey().contains("channel")) && 
				(!entry.getKey().contains("Enabled"))){
				boolean en=mPrefs.getBoolean(entry.getKey()+"Enabled",false);
					thingspeak.Add(entry.getValue().toString(), thingspeakInterval, thingspeakStartupURL, thingspeakURL, en);
			}
		}
		
			if (thingspeakInterval>0) {
				final Handler thingspeakhandler = new Handler();
				thingspeakhandler.postDelayed(new Runnable(){
					public void run(){
						thingspeak.PollAll();
						thingspeakhandler.postDelayed(this, thingspeakInterval);
					}
				}, thingspeakInterval);
			}
		


		
		Handler = new Handler() {
			//        @Override 
			public void handleMessage(Message msg) {
				String text = (String)msg.obj;
				if (text.contains("null")) return; // thingspeak sometimes returns the string 'null' as value, annoying...
				text=text.replaceFirst(",",".");
				boolean setdesc=text.contains("/"); 
				String [] splitted;
				if (setdesc) splitted=text.split("/");
				else splitted=text.split(":");
				boolean found=false;
				float val=0;
				for (int i=0; i<sensors.size(); i++) {
					int j = sensors.get(i).id.indexOf(splitted[0]+":");
					if (j >= 0) { // id found
						if (setdesc) {
							sensors.get(i).description=splitted[1];
							sensors.get(i).timeout=thingspeakInterval*2;
							found=true;
						} 
						else {
							try {
								val=Float.parseFloat(splitted[1].trim());
							} catch (NumberFormatException e) { 
								return; 
							}
							long time;
							if (splitted.length>2)
								try {
									time=Long.parseLong(splitted[2].trim());
								} catch (NumberFormatException e) { 
									time=System.currentTimeMillis(); 
								}
							else time=System.currentTimeMillis();
							/*if (button1.isChecked()) 
								Sensor.loginterval=1;
							else Sensor.loginterval=30;*/
							
							sensors.get(i).setValue(val,time);

							if (splitted.length>3) {// tyre pressure sensor, with additional info in description
								sensors.get(i).description=splitted[0]+" "+splitted[3];
								sensors.get(i).timeout=thingspeakInterval*2;						
								sensors.get(i).changed=true; // force update, just in case temp or voltage changed
							}

							found=true;

							if (sensors.get(i).changed)
								adapter.notifyDataSetChanged();
							break;
						}
					}
				}
				if (!found) {
					if (setdesc) {
						int order=mPrefs.getInt(splitted[0]+':'+"order",-Sensor.createcounter);
						sensors.add(0,new Sensor(splitted[0]+':',splitted[1],order));
						Sensor s=sensors.get(0);
						s.timeout=thingspeakInterval*2;
					}
					else {
						try {
							val=Float.parseFloat(splitted[1].trim());
						} catch (NumberFormatException e) { return; }

						long time;
						if (splitted.length>2)
							try {
								time=Long.parseLong(splitted[1].trim());
							} catch (NumberFormatException e) { 
								time=System.currentTimeMillis(); 
							}
						else time=System.currentTimeMillis();
				
						int order=mPrefs.getInt(splitted[0]+':'+"order",Sensor.createcounter);

						sensors.add(new Sensor(splitted[0]+':',val,time,order));
						Sensor s=sensors.get(sensors.size()-1);

						if (splitted.length>3) {// tyre pressure sensor, with additional info in description
							s.description=splitted[0]+" "+splitted[3];
							s.timeout=thingspeakInterval*2;						
							//s.changed=true; // force update, just in case temp or voltage changed
						}

						Log.d("UDP", "loaded "+s.id+"order=" + s.sortorder);            	
						s.max=mPrefs.getFloat(s.id+"max",s.max);
						Log.d("UDP", "loaded "+s.id+"max=" + s.max);            	
						s.min=mPrefs.getFloat(s.id+"min",s.min);
						Log.d("UDP", "loaded "+s.id+"min=" + s.min);
					}
					numberofsensorschanged=true;
					Collections.sort(sensors);
					adapter.notifyDataSetChanged();

				}

				if (!activityVisible) return;

				long millis=System.currentTimeMillis();
				
				
				//if (millis>lastTimeMillis+20000) // try to stop items from being removed if connection is lost or app is returning from background 
				//	lastTimeMillis=millis;
				if (millis>lastTimeMillis) {
					boolean removedSomething=false;
					for (int i=0; i<sensors.size(); i++) {
						if (sensors.get(i).timedout()) {
							Sensor s=sensors.get(i);
							Log.d("UDP", "removed=" + i+ " id="+sensors.get(i).id+" time="+sensors.get(i).timesinceupdated());
							SharedPreferences.Editor ed = mPrefs.edit();
							ed.putInt(s.id+"order", s.sortorder);
							Log.d("UDP", "saved "+s.id+"order=" + s.max);
							ed.putFloat(s.id+"max", s.max);
							Log.d("UDP", "saved "+s.id+"max=" + s.max);
							ed.putFloat(s.id+"min", s.min);        	
							Log.d("UDP", "saved "+s.id+"min=" + s.min);
							ed.commit();

							sensors.remove(i);
							removedSomething=true;
							numberofsensorschanged=true;
							i--;
						}
					}
					if (removedSomething) 
						adapter.notifyDataSetChanged();
				/*	if (numberofsensorschanged) {
						int numvisible=gridView1.getLastVisiblePosition()-gridView1.getFirstVisiblePosition()+1;
						if (numvisible>1) {
							if (sensors.size()>numvisible) {
								plusOnClick(null);
								plusOnClick(null);
							}
							else {
								minusOnClick(null);
								minusOnClick(null);
							}
								
						}
						numberofsensorschanged=false;
					}*/

					lastTimeMillis=millis+10000;
				}
			}
		};
	}

	@Override
	public void onResume() {
		super.onResume();
		for (Sensor s: sensors) 
			s.timestamp=System.currentTimeMillis();
		activityVisible=true;
		Log.d("UDP","activityVisible=true");
	}


	public class Server implements Runnable {
		boolean run=true;
		@Override
		public void run() {
			DatagramSocket socket = null;
			byte[] buf;
			DatagramPacket packet;
			String s;
			try {
				Thread.sleep(1000);
				//updatetrack("\nServer: Start connecting\n");
				socket = new DatagramSocket(SERVERPORT);
				while(run) {
					buf = new byte[256];
					packet = new DatagramPacket(buf, buf.length);
					//updatetrack("Server: Receiving\n");
					socket.receive(packet);
					s=new String(packet.getData());
					s=s.substring(0,s.indexOf("\0"));
					String [] splitted=s.split("\r\n");
					if (splitted.length>0) 
						for (int j=0; j<splitted.length; j++)
							if (splitted[j].contains(":"))
							updatetrack(splitted[j]);
					else
						if (splitted[j].contains(":"))
							updatetrack(s);
					//System.gc();
				}
			} catch (Exception e) {
				updatetrack(e.toString());				
			}
			if (socket!=null) socket.close();
		}
	}

	public static void updatetrack(String s){
		try {
		Message msg = new Message();
		String textTochange = s;
		Log.d("UDPIN",s);
		msg.obj = textTochange;
		Handler.sendMessage(msg);
		} catch (Exception e) {
			Log.d("UDPIN","Exception "+e.toString()+" "+s);
		}
	}


	protected void onPause() {
		super.onPause();

		SharedPreferences.Editor ed = mPrefs.edit();
		for (Sensor s:sensors) {
			ed.putInt(s.id+"order", s.sortorder);
			Log.d("UDP", "saved "+s.id+"order=" + s.max);
			ed.putFloat(s.id+"max", s.max);
			Log.d("UDP", "saved "+s.id+"max=" + s.max);
			ed.putFloat(s.id+"min", s.min);        	
			Log.d("UDP", "saved "+s.id+"min=" + s.min);

		}
		ed.putInt("numColumns", numColumns);

		DigitalClock digitalClock=(DigitalClock)findViewById(R.id.digitalClock1);
		ed.putInt("clockVisible", digitalClock.getVisibility());
		
		ed.commit();
		
		activityVisible=false;
		Log.d("UDP","activityVisible=false");
	}

		
	@Override
	public boolean onLongClick(View arg0) {
		Log.d("UDP", "Clicked");
		// TODO Auto-generated method stub
		return start;
	}

	boolean showGraphClick() {
		showgraph=!showgraph;
		for (Sensor s: sensors) {
			s.changed=true;
			s.graphchanged=true;
		}
		adapter.notifyDataSetChanged();
		return showgraph;
	}

	boolean showClockClick() {
		DigitalClock digitalClock=(DigitalClock)findViewById(R.id.digitalClock1);
		digitalClock.setVisibility(digitalClock.getVisibility()==8 ? 0 : 8);
		return digitalClock.getVisibility()==0;
	}

	public void minusClick() {

		numColumns--;
		if (numColumns<1) numColumns=1;

		gridView1.setNumColumns(numColumns);
		
		/*for (Sensor s: sensors) {
			s.changed=true;
			//s.graphchanged=true;
		}*/
	}

	public void plusClick() {

		numColumns++;

		gridView1.setNumColumns(numColumns);

		/*for (Sensor s: sensors) {
			s.changed=true;
			//s.graphchanged=true;
		}*/

	}
	
	// Put notification in status bar
    public static void notification(String s,float v) {
        // Send Notification
    	Log.d("UDPNotification", s);
        myNotification = new Notification(android.R.drawable.btn_star_big_on, s,
          System.currentTimeMillis());
        notificationTitle = s;
     	Intent notificationIntent = new Intent(context, UdpActivity.class);
    	PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
        myNotification.number=Math.round(v);
        myNotification.setLatestEventInfo(context,
          notificationTitle,
          notificationText,
          contentIntent);
        notificationManager.notify(MY_NOTIFICATION_ID, myNotification);
   	}

    public static boolean isTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    int findBT()
    {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null)
        {
            Log.d("UDPbt","No bluetooth adapter available");
            return -1;
        }

        if(!mBluetoothAdapter.isEnabled())
        {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if(pairedDevices.size() > 0)
        {
            for(BluetoothDevice device : pairedDevices)
            {
            	Log.d("UDPbt",device.getName());
                if(device.getName().contains("iTPMS")) 
                {
                    mmDevice = device;
                    break;
                }
            }
        }
        Log.d("UDPbt","Bluetooth Device Found");
        return 0;
    }

    void openBT() throws IOException
    {
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //Standard SerialPortService ID
        mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);        
        mmSocket.connect();
        mmOutputStream = mmSocket.getOutputStream();
        mmInputStream = mmSocket.getInputStream();

        beginListenForData();

        Log.d("UDPbt","Bluetooth Opened");
    }

    void beginListenForData()
    {
    	stopWorker = false;
    	workerThread = new Thread(new Runnable()
    	{
    		public void run()
    		{                
				final byte[] packetBytes = new byte[13];
				byte[] old=new byte[13];
    			while(!Thread.currentThread().isInterrupted() && !stopWorker)
    			{
    				try 
    				{
    					mmInputStream.read(packetBytes);
    					//Log.d("UDPbt",(char)packetBytes[0]+""+(char)packetBytes[1]+(char)packetBytes[2]);
    					if (((char)packetBytes[0]+""+(char)packetBytes[1]+(char)packetBytes[2]).contentEquals("TPV")) // attempt error checking, check header
    						if (!Arrays.equals(packetBytes, old)) {
    							updatetrack("Tire "+packetBytes[3]+":"+
    									(0xFF&packetBytes[9])+":: - "+
    									((0xFF&packetBytes[8])-50)+"c "+((0xFF&packetBytes[10])/50.0)+"v");
    							old=packetBytes.clone();
    						}
    						else Log.d("UDPbt","Repeat "+packetBytes[3]);
    					else Log.d("UDPbt","Garbage "+packetBytes[3]);
        				//Thread.sleep(5000);
    				}
    				catch (IOException ex) 
    				{
    					stopWorker = true;
					}
    			}
    		}
    	});

    	workerThread.start();
    }
    
    
    // main menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.showGraph:
                item.setChecked(showGraphClick());
                return true;
                
            case R.id.showClock:
            	item.setChecked(showClockClick());
            	return true;
                
            case R.id.settings:
                Intent intent = new Intent(this, PrefsActivity.class);
                startActivity(intent);
                finish(); // Kill this one. Else it causes too much trouble to try to reinitialize all values and threads
                return true;
            case R.id.plus:
            	plusClick();
            	return true;
            case R.id.minus:
            	minusClick();
            	return true;
            case R.id.bluetooth:
        		try {
        			if (findBT()==0)
        				openBT();
        		}
        			catch (IOException ex) {
                	Log.d("UDPbt",ex.toString());
                }
            	return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.contextmenu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
    	int pos=info.position;
        switch (item.getItemId()) {
            case R.id.up:
            	if (pos==0) return true; // cant move further up
            	int order1=sensors.get(pos-1).sortorder;
            	int order2=sensors.get(pos).sortorder;
            	// swap position of the sortorder index
            	sensors.get(pos-1).sortorder=order2;
            	sensors.get(pos).sortorder=order1;
            	adapter.notifyDataSetChanged();
            	Collections.sort(sensors);
                return true;
            case R.id.down:
            	if (pos==sensors.size()-1) return true; // cant move further down
            	order1=sensors.get(pos+1).sortorder;
            	order2=sensors.get(pos).sortorder;
            	// swap position of the sortorder index
            	sensors.get(pos+1).sortorder=order2;
            	sensors.get(pos).sortorder=order1;
            	Collections.sort(sensors);
            	adapter.notifyDataSetChanged();
                return true;
            case R.id.top:
            	if (pos==0) return true; // cant move further up
            	order1=sensors.get(0).sortorder;
            	order2=sensors.get(pos).sortorder;
            	// swap position of the sortorder index
            	sensors.get(0).sortorder=order2;
            	sensors.get(pos).sortorder=order1;
            	Collections.sort(sensors);
            	adapter.notifyDataSetChanged();
                return true;
            case R.id.bottom:
            	if (pos==sensors.size()-1) return true; // cant move further down
            	order1=sensors.get(sensors.size()-1).sortorder;
            	order2=sensors.get(pos).sortorder;
            	// swap position of the sortorder index
            	sensors.get(sensors.size()-1).sortorder=order2;
            	sensors.get(pos).sortorder=order1;
            	Collections.sort(sensors);
            	adapter.notifyDataSetChanged();
                return true;
            case R.id.clear:
            	float min=sensors.get(pos).history.get(1).floatValue();
            	float max=min;
            	for (int i=1; i<sensors.get(pos).history.size(); i+=2) {
            		if (sensors.get(pos).history.get(i).floatValue()<min) min=sensors.get(pos).history.get(i).floatValue();
            		if (sensors.get(pos).history.get(i).floatValue()>max) max=sensors.get(pos).history.get(i).floatValue();
            	}
            	sensors.get(pos).min=min;
            	sensors.get(pos).max=max;
            	return true;
            case R.id.notify:
            	
            default:
                return super.onContextItemSelected(item);
        }
    }
    
    
}

