package com.github.uiautomator.test;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class ServiceApkInstaller {
    public final static String TAG = "JussiTest";
    private final String apkPackName = "com.dta.test.frida";
    private String apkName;
    private String newApkPath = Environment.getExternalStorageDirectory() + File.separator + "apktemp" + File.separator + "tmp.apk";
    private Context mContext;
    private Thread subThread;

    public ServiceApkInstaller() {
    }

    public ServiceApkInstaller(Context context, String name) {
        mContext = context;
        apkName = name;
    }



    class installTask implements Runnable {
        @Override
        public void run() {
            if (!hasInstalled()) {
//                setInstallPermission();
//                Intent intent = new Intent(Intent.ACTION_VIEW);
//                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                intent.setDataAndType(Uri.parse("file://" + newApkPath),
//                        "application/vnd.android.package-archive");
//                mContext.startActivity(intent);


                AssetManager assets = mContext.getAssets();
                try
                {
                    //获取assets资源目录下的himarket.mp3,实际上是himarket.apk,为了避免被编译压缩，修改后缀名。
                    InputStream stream = assets.open(apkName);
                    if(stream==null){
                        Log.v(TAG,"no file");
                        return;
                    }
                    String folder = Environment.getExternalStorageDirectory() + File.separator + "apktemp" + File.separator;
//                    String folder = "/data/local/tmp/apktmp";
                    File f=new File(folder);
                    if(!f.exists()){
                        f.mkdir();
                    }
                    String apkPath = folder + "tmp.apk";
                    File file = new File(apkPath);
                    //创建apk文件
                    file.createNewFile();
                    //将资源中的文件重写到sdcard中
                    //<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
                    writeStreamToFile(stream, file);
                    //安装apk
                    //<uses-permission android:name="android.permission.INSTALL_PACKAGES" />
                    installApk(apkPath);
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            } else{
                Log.v(TAG,"Had been install !");
            }
        }
    }

    private void writeStreamToFile(InputStream stream, File file)
    {
        try
        {
            //
            OutputStream output = null;
            try
            {
                output = new FileOutputStream(file);
            }
            catch (FileNotFoundException e1)
            {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            try
            {
                try
                {
                    final byte[] buffer = new byte[1024];
                    int read;

                    while ((read = stream.read(buffer)) != -1)
                        output.write(buffer, 0, read);

                    output.flush();
                }
                finally
                {
                    output.close();
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        finally
        {
            try
            {
                stream.close();
            }
            catch (IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private void installApk(String apkPath)
    {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        File apkFile = new File(apkPath);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Uri uri = FileProvider.getUriForFile(mContext, mContext.getPackageName() + ".fileprovider", apkFile);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
        } else {
            intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");
        }
        mContext.startActivity(intent);
    }

    private void installApk(){
        Intent intent = new Intent(Intent.ACTION_VIEW);
        File apkFile = new File(newApkPath);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Uri uri = FileProvider.getUriForFile(mContext, mContext.getPackageName() + ".fileprovider", apkFile);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
        } else {
            intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");
        }
        mContext.startActivity(intent);
    }


    public ServiceApkInstaller install() {
        if (subThread != null && subThread.isAlive()) {
            return this;
        }
        subThread = new Thread(new installTask());
        subThread.start();
        return this;
    }

    public void uninstall() {
        Uri packageURI = Uri.parse("package:" + apkPackName);
        Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageURI);
        mContext.startActivity(uninstallIntent);
    }


    public boolean hasInstalled() {


        final PackageManager packageManager = mContext.getPackageManager();
        List<PackageInfo> installedPackInfoList = packageManager.getInstalledPackages(0);
        for (int i = 0; installedPackInfoList != null && i < installedPackInfoList.size(); i++) {
            PackageInfo installedPackInfo = installedPackInfoList.get(i);
            if (installedPackInfo != null && TextUtils.equals(apkPackName, installedPackInfo.packageName)) {

                copyApkFromAssets(mContext, apkName, newApkPath);
                PackageInfo assetPackInfo = packageManager.getPackageArchiveInfo(newApkPath, PackageManager.GET_ACTIVITIES);
                if (assetPackInfo != null) {
                    ApplicationInfo appInfo = assetPackInfo.applicationInfo;
                    String assetVersionName = assetPackInfo.versionName;
                    int assetVersionCode = assetPackInfo.versionCode;
                    if (!TextUtils.equals(assetVersionName, installedPackInfo.versionName) || installedPackInfo.versionCode < assetVersionCode) {
                        return false;
                    } else {
                        return true;
                    }
                }


                return true;
            }
        }
        return false;
    }

    public boolean copyApkFromAssets(Context context, String fileName, String path) {
        boolean copyIsFinish = false;
        try {
            InputStream is = context.getAssets().open(fileName);
            File file = new File(path);
            file.createNewFile();
            FileOutputStream fos = new FileOutputStream(file);
            byte[] temp = new byte[1024];
            int i = 0;
            while ((i = is.read(temp)) > 0) {
                fos.write(temp, 0, i);
            }
            fos.close();
            is.close();
            copyIsFinish = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return copyIsFinish;
    }
}
