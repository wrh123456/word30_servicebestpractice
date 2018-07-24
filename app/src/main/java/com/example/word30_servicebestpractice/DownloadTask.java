package com.example.word30_servicebestpractice;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DownloadTask extends AsyncTask<String,Integer,Integer>{
    private static final String TAG = "DownloadTask";
    public static final int TYPE_SUCCESS=0;
    public static final int TYPE_FAILED=1;
    public static final int TYPE_PAUSED=2;
    public static final int TYPE_CANCELED=3;

    private DownloadListener listener;
    private boolean isCanceled=false;
    private boolean isPaused=false;
    private int lastProgress;

    public DownloadTask(DownloadListener listener){
        this.listener=listener;
    }

    @Override
    protected Integer doInBackground(String... strings) {//用于后台执行具体的逻辑  //参数传的是下载地址
        InputStream is=null;
        RandomAccessFile savedFile=null;
        File file=null;

        try {
            long downloadedLength = 0;//记录已下载的文件长度
            String downloadUrl = strings[0];
            String filename = downloadUrl.substring(downloadUrl.lastIndexOf("/"));
            String directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
            Log.d(TAG, "doInBackground: "+directory);
            file = new File(directory + filename);
            if (file.exists()) {
                downloadedLength = file.length();
            }
            long contentLength = getContentLength(downloadUrl);
            if (contentLength == 0) {
                return TYPE_FAILED;
            } else if (contentLength == downloadedLength) {
                return TYPE_SUCCESS;
            }
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .addHeader("RANGE", "bytes=" + downloadedLength + "-")
                    .url(downloadUrl)
                    .build();
            Response response = client.newCall(request).execute();
            if (response != null) {
                is = response.body().byteStream();
                savedFile = new RandomAccessFile(file, "rw");
                savedFile.seek(downloadedLength);
                byte[] b=new byte[1024];
                int tocal=0;
                int len;
                while((len=is.read(b))!=-1){
                    if(isCanceled){
                        Log.d(TAG, "doInBackground: isCanceled:"+isCanceled);
                        return TYPE_CANCELED;
                    }
                    else if (isPaused){
                        return TYPE_PAUSED;
                    }
                    else{
                        tocal+=len;
                        savedFile.write(b,0,len);
                        //计算已经下载的百分比
                        int progress=(int) ((tocal+downloadedLength)*100/contentLength);
                        Log.d(TAG, "doInBackground: "+progress);
                        publishProgress(progress);
                    }
                }
                response.body().close();
                return  TYPE_SUCCESS;
            }
        }catch(Exception e){
            e.printStackTrace();
        }finally {
            try{
                if(is!=null){
                    is.close();
                }
                if(savedFile!=null){
                    savedFile.close();
                }
                if(isCanceled&&file!=null){
                    file.delete();
                }
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
        return TYPE_FAILED;
    }
    protected void onProgressUpdate(Integer... vlaues){
        int progress=vlaues[0];
        if(progress>lastProgress){
            listener.onProgress(progress);
            lastProgress=progress;
        }
    }

    @Override
    protected void onPostExecute(Integer integer) {
        switch (integer){
            case TYPE_SUCCESS:
                listener.onSuccess();;
                break;
            case TYPE_FAILED:
                listener.onFailed();
                break;
            case TYPE_PAUSED:
                listener.onPaused();
                break;
            case TYPE_CANCELED:
                listener.onCanceled();
                break;
                default:break;
        }
    }
    public void pauseDownload(){
        isPaused=true;
    }
    public void cancelDownload(){
       isCanceled=true;
    }
    private long getContentLength(String downloadUrl){
        OkHttpClient client=new OkHttpClient();
        Request request=new Request.Builder()
                .url(downloadUrl)
                .build();
        Response response= null;
        try {
            response = client.newCall(request).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(response!=null&&response.isSuccessful()){
            long contentLength=response.body().contentLength();
            response.body().close();
            return contentLength;
        }
        return 0;
    }
}
