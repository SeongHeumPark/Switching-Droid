package com.dgssm.switchingdroid.services;

/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.dgssm.switchingdroid.utils.Constants;
import com.dgssm.switchingdroid.utils.LOGS;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;

/**
 * A BroadcastReceiver that notifies of important wifi p2p events.
 */
public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {

	private static final String tag = "WiFiDirectBroadcastReceiver";
	
	private WifiMgrThread mManagerThread;
    private WifiP2pManager manager;
    private Channel channel;

    /**
     * @param manager WifiP2pManager system service
     * @param channel Wifi p2p channel
     * @param activity activity associated with the receiver
     */
    public WiFiDirectBroadcastReceiver(WifiP2pManager manager, Channel channel, WifiMgrThread wifimgr) {
        super();
        this.manager = manager;
        this.channel = channel;
        this.mManagerThread = wifimgr;
    }

    /*
     * (non-Javadoc)
     * @see android.content.BroadcastReceiver#onReceive(android.content.Context,
     * android.content.Intent)
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        LOGS.d(tag, action);
        
        // Wifi manager initialization returns below action.
        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
        	LOGS.d(tag, "# received WIFI_P2P_STATE_CHANGED_ACTION");

            // Determine if Wifi Direct mode is enabled or not, alert
            // the Activity.
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
            	LOGS.d(tag, "# WIFI_P2P_STATE_ENABLED");
            	mManagerThread.setIsWifiP2pEnabled(true);
            	mManagerThread.setStatus(Constants.STATUS_CHANNEL_CONNECTED);
            } else {
            	LOGS.d(tag, "# WIFI_P2P_STATE_DISABLED");
            	mManagerThread.setIsWifiP2pEnabled(false);
            	mManagerThread.setStatus(Constants.STATUS_CHANNEL_DISCONNECT);
            }
        } 
        // If peer discover succeeded, receive below action.
        else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            // Request available peers from the wifi p2p manager. This is an
            // asynchronous call and the calling activity is notified with a
            // callback on PeerListListener.onPeersAvailable()
        	LOGS.d(tag, "# received WIFI_P2P_PEERS_CHANGED_ACTION");
            if (manager != null 
            		&& (mManagerThread.getStatus() == Constants.STATUS_DISCOVER_PEERS_SUCCESS) ) {
            	manager.requestPeers(channel, mManagerThread.mPeerListListener);
            	mManagerThread.setStatus(Constants.STATUS_REQUEST_PEERS);
            } 
            else if(manager != null 
            		&& (mManagerThread.getStatus() == Constants.STATUS_CONNECT_PEER
            				|| mManagerThread.getStatus() == Constants.STATUS_CONNECT_PEER_SUCCESS) ) {
            	// Waiting for WIFI_P2P_CONNECTION_CHANGED_ACTION broadcast, do nothing.
            }
            else if(manager != null 
            		&& (mManagerThread.getStatus() == Constants.STATUS_CONNECTED_GROUP_AS_CLIENT
            				|| mManagerThread.getStatus() == Constants.STATUS_CONNECTED_GROUP_AS_OWNER) ) {
            	// Connection established already. If you want to make 1:N connection, do something here.
            }
            else {
            	if(mManagerThread.peer_connect==0){
            		mManagerThread.setStatus(Constants.STATUS_CHANNEL_DISCONNECT);
            	} 
            	
            }
        }
        // Connected to peer. Request network info and connection info.
        else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
        	LOGS.d(tag, "# received WIFI_P2P_CONNECTION_CHANGED_ACTION");
        	
            if (manager == null) {
            	mManagerThread.setStatus(Constants.STATUS_CHANNEL_DISCONNECT);
                return;
            }

            NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

            if (networkInfo.isConnected()) {
                // we are connected with the other device, request connection
                // info to find group owner IP
                LOGS.d(tag, "Connected to p2p network. Requesting network details");
                manager.requestConnectionInfo(channel, (ConnectionInfoListener) mManagerThread.mConnectionInfoListener);
                mManagerThread.setStatus(Constants.STATUS_REQUEST_CONNECTION_INFO);
            } else {
                // It's a disconnect
            	LOGS.d(tag, "Disconnected from p2p network. Go back to discovering stage...");
            	mManagerThread.setStatus(Constants.STATUS_CHANNEL_CONNECTED);	// Go back to discovering stage
            }
        }
        else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
        	LOGS.d(tag, "# received WIFI_P2P_THIS_DEVICE_CHANGED_ACTION");
            WifiP2pDevice device = (WifiP2pDevice) intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
    		LOGS.d(tag, "Name = "+device.deviceName);
    		LOGS.d(tag, "Address = "+device.deviceAddress);
    		LOGS.d(tag, "Primary type = "+device.primaryDeviceType);
            LOGS.d(tag, "Device status -" + device.status);
        }
    }
}
