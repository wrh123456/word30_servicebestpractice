package com.example.word30_servicebestpractice;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.File;

public class DownloadService extends Service {
    private static final String TAG = "DownloadService";
    private DownloadTask downloadTask;
    private String downloadUrl;
    private DownloadListener listener= new DownloadListener() {
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onProgress(int progress) {
            getNotificationManager().notify(1,getNotification("下载中...",progress));
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onSuccess() {
            downloadTask=null;
            stopForeground(true);
            getNotificationManager().notify(1,getNotification("下载成功",-1));
            Toast.makeText(DownloadService.this,"下载成功",Toast.LENGTH_SHORT).show();
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onFailed() {
            downloadTask=null;
            stopForeground(true);
            getNotificationManager().notify(1,getNotification("下载失败",-1));
            Toast.makeText(DownloadService.this,"下载失败",Toast.LENGTH_SHORT).show();

        }

        @Override
        public void onPaused() {
            downloadTask=null;
            Toast.makeText(DownloadService.this,"暂停",Toast.LENGTH_SHORT).show();

        }

        @Override
        public void onCanceled() {
            downloadTask=null;
            stopForeground(true);
            Toast.makeText(DownloadService.this,"取消",Toast.LENGTH_SHORT).show();

        }
    };
    private DownloadBinder mBinder=new DownloadBinder();
    public DownloadService() {

    }
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }


    class DownloadBinder extends Binder{
        @RequiresApi(api = Build.VERSION_CODES.O)
        public void startDownload(String url){
            if(downloadTask==null){
                downloadUrl=url;
                downloadTask=new DownloadTask((DownloadListener)listener);
                downloadTask.execute(downloadUrl);
                startForeground(1,getNotification("下载中",0));
                Toast.makeText(DownloadService.this,"下载中",Toast.LENGTH_LONG).show();
            }
        }
        public void pauseDownload(){
            if(downloadTask!=null){
                downloadTask.pauseDownload();
            }
        }
        public void cancerDownload(){
            if(downloadTask!=null){
                downloadTask.cancelDownload();
            }
            else{
                if(downloadUrl!=null){
                    String fileName=downloadUrl.substring(downloadUrl.lastIndexOf("/"));
                    String directory= Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
                    File file=new File(directory+fileName);
                    if (file.exists()) {
                        file.delete();
                    }
                    getNotificationManager().cancel(1);
                    stopForeground(true);
                    Toast.makeText(DownloadService.this,"取消",Toast.LENGTH_LONG).show();
                }
            }
        }
    }
    private NotificationManager getNotificationManager(){
        return (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    private Notification getNotification(String title, int progress){
        Intent intent=new Intent(this,MainActivity.class);
        PendingIntent pi=PendingIntent.getActivity(this,0,intent,0);
        NotificationManager nm= (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel nc=new NotificationChannel("11","hah",NotificationManager.IMPORTANCE_DEFAULT);
        nc.enableLights(true);
        nc.enableVibration(true);
        nm.createNotificationChannel(nc);
        Notification notification=new Notification.Builder(this,"11")
                .setContentText("下载")
                .setContentTitle(title)
                .setContentIntent(pi)
                .setProgress(100,progress,false)
                .setSmallIcon(R.mipmap.ic_launcher)
                .build();
        return notification;
    }

}
