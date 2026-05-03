package com.swx.adbremote.service;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.swx.adbremote.entity.DeviceInfo;
import com.swx.adbremote.utils.NetworkScanner;
import com.swx.adbremote.utils.ThreadPoolService;

import java.util.ArrayList;
import java.util.List;

public class IpScanService {
    private static final String TAG = "IpScanService";
    
    private NetworkScanner networkScanner;
    private Handler mainHandler;
    private volatile boolean isScanning = false;
    private volatile boolean isCancelled = false;

    public IpScanService() {
        this.networkScanner = new NetworkScanner();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public interface IpScanCallback {
        void onScanStart();
        void onDeviceFound(DeviceInfo device);
        void onScanProgress(int current, int total);
        void onScanComplete(List<DeviceInfo> devices);
        void onScanError(String error);
        void onScanCancelled();
    }

    public void startScan(Context context, IpScanCallback callback) {
        if (isScanning) {
            Log.w(TAG, "Already scanning");
            return;
        }

        isScanning = true;
        isCancelled = false;

        if (callback != null) {
            mainHandler.post(callback::onScanStart);
        }

        ThreadPoolService.newTask(() -> {
            try {
                List<DeviceInfo> devices = networkScanner.scanNetwork(context, 5555, 
                    new NetworkScanner.ScanProgressCallback() {
                        @Override
                        public void onProgress(int current, int total) {
                            if (!isCancelled && callback != null) {
                                mainHandler.post(() -> 
                                    callback.onScanProgress(current, total));
                            }
                        }

                        @Override
                        public void onDeviceFound(DeviceInfo device) {
                            if (!isCancelled && callback != null) {
                                mainHandler.post(() -> 
                                    callback.onDeviceFound(device));
                            }
                        }
                    });

                if (!isCancelled && callback != null) {
                    mainHandler.post(() -> 
                        callback.onScanComplete(devices));
                }

            } catch (Exception e) {
                Log.e(TAG, "Scan failed", e);
                if (!isCancelled && callback != null) {
                    mainHandler.post(() -> 
                        callback.onScanError(e.getMessage()));
                }
            } finally {
                isScanning = false;
            }
        });
    }

    public void stopScan() {
        isCancelled = true;
        isScanning = false;
        Log.d(TAG, "Scan cancelled");
    }

    public boolean isScanning() {
        return isScanning;
    }
}
