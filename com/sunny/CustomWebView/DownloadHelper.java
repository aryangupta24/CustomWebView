package com.sunny.CustomWebView;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
import android.os.Build;
import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.EventDispatcher;
import com.google.appinventor.components.runtime.OnDestroyListener;

import java.util.Timer;
import java.util.TimerTask;

@DesignerComponent(version = 1,
        versionName = "1.1",
        description ="Helper class of CustomWebView extension for downloading files <br> Developed by Sunny Gupta",
        category = ComponentCategory.EXTENSION,
        nonVisible = true,
        iconName = "https://res.cloudinary.com/andromedaviewflyvipul/image/upload/c_scale,h_20,w_20/v1571472765/ktvu4bapylsvnykoyhdm.png",
        helpUrl="https://github.com/vknow360/CustomWebView",
        androidMinSdk = 21)
@SimpleObject(external=true)
public class DownloadHelper extends AndroidNonvisibleComponent implements OnDestroyListener{
    private Context context;
    private DownloadManager downloadManager;
    private long lastRequestId;
    private int visibility = 1;
    public BroadcastReceiver completed = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) == lastRequestId){
                DownloadCompleted();
            }
        }
    };
    public DownloadHelper(ComponentContainer container){
        super(container.$form());
        context = container.$context();
        downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        context.registerReceiver(completed,new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    @SimpleProperty(description="Sets download notification visibility")
    public void NotificationVisibility(int i){
        visibility = i;
    }

    @SimpleFunction(description = "Returns guessed file name")
    public String GuessFileName(String url, String mimeType, String contentDisposition){
        return URLUtil.guessFileName(url, contentDisposition, mimeType);
    }
    @SimpleFunction(description = "Downloads the given file")
    public void Download(String url, String mimeType, String fileName, String downloadDir) {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setMimeType(mimeType);
        String cookies = CookieManager.getInstance().getCookie(url);
        request.addRequestHeader("cookie", cookies);
        //request.addRequestHeader("User-Agent", UserAgent);
        request.setDescription("Downloading file...");
        request.setTitle(fileName);
        request.setNotificationVisibility(visibility);
        request.setTitle(fileName);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            request.setDestinationInExternalFilesDir(context, downloadDir, fileName);
        } else {
            request.setDestinationInExternalPublicDir(downloadDir, fileName);
        }
        lastRequestId = downloadManager.enqueue(request);
        final Timer progressTimer = new Timer();
        progressTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                DownloadManager.Query downloadQuery = new DownloadManager.Query();
                downloadQuery.setFilterById(lastRequestId);
                Cursor cursor = downloadManager.query(downloadQuery);
                if (cursor.moveToFirst()){
                    long downloadedSize = (long)cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                    long totalSize = (long)cursor.getLong(cursor
                            .getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                    cursor.close();
                    final int progress = (int)((downloadedSize*100)/totalSize);
                    form.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            DownloadProgressChanged(progress);
                            if (progress >= 99) {
                                progressTimer.cancel();
                                progressTimer.purge();
                            }
                        }
                    });
                }
            }
        },0,1000);
    }
    @SimpleEvent(description = "Event invoked when downloading gets completed")
    public void DownloadCompleted(){
        EventDispatcher.dispatchEvent(this,"DownloadCompleted");
    }
    @SimpleEvent(description = "Event invoked when downloading progress changes")
    public void DownloadProgressChanged(int progress){
        EventDispatcher.dispatchEvent(this,"DownloadProgressChanged",progress);
    }
    @SimpleFunction(description = "Tries to open the last downloaded file")
    public void OpenFile(){
        try {
            downloadManager.openDownloadedFile(lastRequestId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @SimpleFunction(description = "Cancels the current download request")
    public void Cancel(){
        downloadManager.remove(lastRequestId);
    }
    @Override
    public void onDestroy() {
        context.unregisterReceiver(completed);
    }

}