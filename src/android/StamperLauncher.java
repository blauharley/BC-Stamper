package com.phonegap.stamper;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.location.GpsStatus;

import java.util.List;
import java.util.Iterator;


public class StamperLauncher extends CordovaPlugin implements GpsStatus.Listener, LocationListener, ProviderInterface{

	public static final String ACTION_REQUEST_PROVIDER = "request";
	
	private long interval;
	
	// wait at the beginning
	private long startTime;
	private long warmUpTime = 3000;
	private LocationManager locationManager = null;
	private LocationListener locationListenerGPS;
	private LocationListener locationListenerNetwork;
	private LocationListener locationListenerPassive;
	
	private Location locGPS = null;
	private long startTimeMilliGPS;
	private long endTimeMilliGPS;
	private Location locNetwork = null;
	private long startTimeMilliNetwork;
	private long endTimeMilliNetwork;
	private Location locPassive = null;
	private long startTimeMilliPassive;
	private long endTimeMilliPassive;
	private Location locBest = null;
	private long startTimeMilliBest;
	private long endTimeMilliBest;
	private boolean firstLocationUpdate = true;
	
	private Handler serviceHandler = null;
	
	private boolean providerEnabled = true;
	private int providerStatus = -1;
	private Location lastGPSLocation = null;
	private long lastGPSLocationMillis = 0;
	private boolean isGPSAvailable = false;
	private GpsStatus gpsStatus = null;
	
	private int locationTimerTimeout = 1000*3;
	
	private Activity thisAct;
	private CallbackContext callCtx;
	
	class timer implements Runnable {
          public void run() {
          	onProviderLocationChanged();
          }
    }
    
	public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {
		
		try {
			
			thisAct = this.cordova.getActivity();
			
			callCtx = callbackContext;
			
			startTime = System.currentTimeMillis();
			
			if(ACTION_REQUEST_PROVIDER.equalsIgnoreCase(action)){
				interval = args.getInt(0);
				PluginResult r = new PluginResult(PluginResult.Status.NO_RESULT);
				r.setKeepCallback(true);
				callbackContext.sendPluginResult(r);
				requestLocationAccurancy();
				return true;
			}
			else{
				callbackContext.error("Call undefined action: "+action);
				return false;
			}
		
		} catch (JSONException e) {
			callbackContext.error("Reminder exception occured: "+e.toString());
			return false;
		}
		
		
	}

	private String getBestProvider(){
		
		Criteria c = new Criteria();
        c.setAccuracy(Criteria.ACCURACY_LOW);
        c.setHorizontalAccuracy(DESIRED_LOCATION_ACCURANCY_MEDIUM);
        c.setPowerRequirement(Criteria.POWER_LOW);

        locationManager = (LocationManager) thisAct.getSystemService(Context.LOCATION_SERVICE);
        
        return locationManager.getBestProvider(c,true);
        
	}
	
	private void onProviderLocationChanged(){
		
		providerStatus = LocationProvider.OUT_OF_SERVICE;
	    
  		JSONObject gps;
  		
  		if(locGPS == null){
  			gps = getProviderResponseByLocation(new Location(getBestProvider()),"gps");
  		}
  		else{
  			gps = getProviderResponseByLocation(locGPS,"gps");
  		}
  		
  		JSONObject net;
  		
  		if(locNetwork == null){
  			net = getProviderResponseByLocation(new Location(getBestProvider()),"net");
  		}
  		else{
  			net = getProviderResponseByLocation(locNetwork,"net");
  		}
  		
  		JSONObject passive;
      		
  		if(locPassive == null){
  			passive = getProviderResponseByLocation(new Location(getBestProvider()),"passive");
  		}
  		else{
  			passive = getProviderResponseByLocation(locPassive,"passive");
  		}
  		
  		JSONObject best;
  		
  		if(locBest == null){
  			best = getProviderResponseByLocation(new Location(getBestProvider()),"best");
  		}
  		else{
  			best = getProviderResponseByLocation(locBest,"best");
  		}
  		
		sendInfoResponse(gps, net, passive, best);
			
	}
	
	private void requestLocationAccurancy(){
		
		if(locationManager != null){
			removeLocationListeners();
		}
		
		locationListenerGPS = new LocationListener() {
	        @Override
	        public void onStatusChanged(String provider, int status, Bundle extras) {
	        }
	
	        @Override
	        public void onProviderEnabled(String provider) {
	        }
	
	        @Override
	        public void onProviderDisabled(String provider) {
	        }
	
	        @Override
	        public void onLocationChanged(Location location) {
				locGPS = location;
				lastGPSLocation = location;
				lastGPSLocationMillis = System.currentTimeMillis();
				endTimeMilliGPS = System.currentTimeMillis();
				
				if(firstLocationUpdate){
					firstLocationUpdate = false;
					onProviderLocationChanged();
				}
				
	        }
	    };
	    
	    locationListenerNetwork = new LocationListener() {
	        @Override
	        public void onStatusChanged(String provider, int status, Bundle extras) {
	        }
	
	        @Override
	        public void onProviderEnabled(String provider) {
	        }
	
	        @Override
	        public void onProviderDisabled(String provider) {
	        }
	
	        @Override
	        public void onLocationChanged(Location location) {
				locNetwork = location;
				endTimeMilliNetwork = System.currentTimeMillis();
				
				if(firstLocationUpdate){
					firstLocationUpdate = false;
					onProviderLocationChanged();
				}
				
	        }
	    };
	 
	 	locationListenerPassive = new LocationListener() {
	        @Override
	        public void onStatusChanged(String provider, int status, Bundle extras) {
	            
	        }
	
	        @Override
	        public void onProviderEnabled(String provider) {
	        }
	
	        @Override
	        public void onProviderDisabled(String provider) {
	        }
	
	        @Override
	        public void onLocationChanged(Location location) {
				locPassive = location;
				endTimeMilliPassive = System.currentTimeMillis();
				
				if(firstLocationUpdate){
					firstLocationUpdate = false;
					onProviderLocationChanged();
				}
				
	        }
	    };
    	
		locationManager = (LocationManager) thisAct.getSystemService(Context.LOCATION_SERVICE);
		
		if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, interval, 0,locationListenerGPS);
			locationManager.addGpsStatusListener(this);
		}
		
		if(locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER )){
			locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, interval, 0,locationListenerNetwork);
		}
		
		if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || 
		   locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER )){
			
			locationManager.requestLocationUpdates(getBestProvider(), interval, 0,this);
            locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, interval, 0,locationListenerPassive);
            
			serviceHandler = new Handler();
        	serviceHandler.postDelayed( new timer(),interval+locationTimerTimeout);
        
	        lastGPSLocation = null;
	        isGPSAvailable = false;
	        providerEnabled = false;
	        gpsStatus = null;
			
		}
		else{
			
			PluginResult r = new PluginResult(PluginResult.Status.ERROR,"provider is not enabled");
			callCtx.sendPluginResult(r);
			
		}
		
	}
	
	private boolean timeWarmUpOut(){
		return System.currentTimeMillis() >= (startTime + warmUpTime);
	}
	
	private void sendInfoResponse(JSONObject gps, JSONObject net, JSONObject passive, JSONObject best){
		
		try{
			
			JSONObject jsonObj = new JSONObject();
			jsonObj.put("gps",gps);
			jsonObj.put("net",net);
			jsonObj.put("passive",passive);
			jsonObj.put("best",best);
			
			PluginResult r = new PluginResult(PluginResult.Status.OK, jsonObj);
			r.setKeepCallback(true);
			
			callCtx.sendPluginResult(r);
			
			serviceHandler.removeCallbacksAndMessages(null);
			
			serviceHandler = new Handler();
	        serviceHandler.postDelayed( new timer(),interval+locationTimerTimeout);
	        
		}
		catch (JSONException e){
			callCtx.sendPluginResult(new PluginResult(PluginResult.Status.JSON_EXCEPTION));
        }
        
	}
	
	private JSONObject getProviderResponseByLocation(Location location, String type){
		
		try{
			
			JSONObject jsonObj = new JSONObject();
	
	        JSONObject coords = new JSONObject();
	        
	        coords.put("latitude",location.getLatitude());
	        coords.put("longitude",location.getLongitude());
	        
	        coords.put("accurancy", location.getAccuracy());
	        
	        if(type == "gps"){
	        	coords.put("endtime", endTimeMilliGPS);
	        	coords.put("gps_fix", isGPSAvailable);
	        }
	        else if(type == "net"){
	        	coords.put("endtime", endTimeMilliNetwork);
	        }
	        else if(type == "passive"){
	        	coords.put("endtime", endTimeMilliPassive);
	        }
	        else if(type == "best"){
	        	coords.put("endtime", endTimeMilliBest);
	        }
	        
	        if(location.hasBearing()){
	        	coords.put("heading", location.getBearing());	
	        }
	        if(location.hasAltitude()){
	        	coords.put("altitude", location.getAltitude());	
	        }
	        if(location.hasSpeed()){
	        	coords.put("speed", location.getSpeed());	
	        }
	        
			jsonObj.put("coords",coords);
			jsonObj.put("timestamp", location.getTime());
			
			return jsonObj;
			
		}
		catch (JSONException e){
			callCtx.sendPluginResult(new PluginResult(PluginResult.Status.JSON_EXCEPTION));
        }
        
        return new JSONObject();
        
	}
	
	private void removeLocationListeners(){
		locationManager.removeGpsStatusListener(this);
		locationManager.removeUpdates(locationListenerGPS);
        locationManager.removeUpdates(locationListenerNetwork);
        locationManager.removeUpdates(locationListenerPassive);
	}
	
	@Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
		providerStatus = status;
    }

	@Override
    public void onProviderEnabled(String provider) {
		providerEnabled = true;
    }
	
	@Override
    public void onProviderDisabled(String provider) {
		providerEnabled = false;
    }
    
    // used to receive best provider coords
    @Override
    public void onLocationChanged(Location location) {
		locBest = location;
		endTimeMilliBest = System.currentTimeMillis();
		
		if(firstLocationUpdate){
			firstLocationUpdate = false;
			onProviderLocationChanged();
		}
		
    }
    
    public void onGpsStatusChanged(int event) {
    	gpsStatus = locationManager.getGpsStatus(gpsStatus);
        switch (event) {
            case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
            
                if (lastGPSLocation != null){
                    isGPSAvailable = (System.currentTimeMillis() - lastGPSLocationMillis) < warmUpTime;
				}
				else{
					isGPSAvailable = false;
				}
				
                break;
            case GpsStatus.GPS_EVENT_FIRST_FIX:
                isGPSAvailable = true;
                break;
        }
    }
    
}