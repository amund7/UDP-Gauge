package com.androidituts.udp;

import java.util.Map;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.util.Log;

public class PrefsActivity extends PreferenceActivity 
implements OnSharedPreferenceChangeListener {
	
	PreferenceCategory category;
	int currentChannel=0;

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,String key) {
		//if (key.equals(KEY_PREF_SYNC_CONN)) {
			Preference connectionPref = findPreference(key);
			// Set summary to be the user-description for the selected value
			if (connectionPref!=null)
				if (connectionPref instanceof EditTextPreference)
				connectionPref.setSummary(sharedPreferences.getString(key, ""));

			//Preference pref = findPreference(key);

		    /*if (connectionPref instanceof EditTextPreference) {
		        EditTextPreference listPref = (EditTextPreference) connectionPref;
		        connectionPref.setSummary(listPref.getText());
		    }*/

	//	}
	}

	private void addChannelUI(PreferenceCategory category,String key) {

		String channelID=key;
		Log.d("UDPsettings", "Key="+key);
		int channelNumber;
		try {
			channelNumber=Integer.parseInt(key.replaceAll("\\D+",""));
			currentChannel=channelNumber;
			//channelID="channelNumber"+
		} catch(NumberFormatException nfe) {
		   channelNumber=currentChannel;
		   channelID="channel"+channelNumber;
		} 
		Log.d("UDPsettings", "ChannelID="+channelID);
		
		EditTextPreference textBoxPref = new EditTextPreference(this);
		textBoxPref.setTitle("Channel");
		textBoxPref.setSummary("Thingspeak channel number");
		textBoxPref.setKey(channelID);
		
		CheckBoxPreference checkBoxPref = new CheckBoxPreference(this);
		checkBoxPref.setTitle("");
		checkBoxPref.setSummary("Enable");
		checkBoxPref.setChecked(true);
		checkBoxPref.setKey(channelID+"Enabled");

		category.addPreference(textBoxPref);
		category.addPreference(checkBoxPref);
		
		currentChannel++;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		
		PreferenceScreen screen = this.getPreferenceScreen();// getPreferenceManager(). .createPreferenceScreen(this);
	
		category = new PreferenceCategory(this);
		category.setTitle("Thingspeak channels");

		screen.addPreference(category);
		
		setPreferenceScreen(screen);
		
		Preference button = (Preference)findPreference("button");
		button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
		                @Override
		                public boolean onPreferenceClick(Preference arg0) { 
		                    addChannelUI(category,"channel"+currentChannel);   
		                    return true;
		                }
		            });	
		
		Map<String, ?> allEntries = screen.getSharedPreferences().getAll();
		for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
		    Log.d("map values", entry.getKey() + ": " + entry.getValue().toString());
			if (entry.getKey().contains("channel"))
				if (!entry.getKey().contains("Enabled"))
					addChannelUI(category,entry.getKey());		
			onSharedPreferenceChanged(screen.getSharedPreferences(),entry.getKey()) ;
		}		
		screen.getSharedPreferences().registerOnSharedPreferenceChangeListener( this  );
		
	} 
	
	/*private void restartActivity() {
	    Intent intent = getIntent();
	    finish();
	    startActivity(intent);	    
	}
	
    @Override
    protected void onStop() {
        super.onStop();
        restartActivity();
        //getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }*/
	
}