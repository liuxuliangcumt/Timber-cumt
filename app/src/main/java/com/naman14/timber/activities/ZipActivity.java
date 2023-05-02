package com.naman14.timber.activities;

import static android.os.Environment.DIRECTORY_DOWNLOADS;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.naman14.timber.R;


import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;

import java.io.File;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;


public class ZipActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ziop);
        requestPermission();
    }


    public static String getFilePathFromContentUri(Uri selectedVideoUri,
                                                   Activity context) {
        String filePath = "";
        String[] filePathColumn = {MediaStore.MediaColumns.DATA};

//      Cursor cursor = contentResolver.query(selectedVideoUri, filePathColumn, null, null, null);
//      也可用下面的方法拿到cursor
        Cursor cursor = context.managedQuery(selectedVideoUri, filePathColumn, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            filePath = cursor.getString(columnIndex);
            try {
                //4.0以上的版本会自动关闭 (4.0--14;; 4.0.3--15)
                if (Integer.parseInt(Build.VERSION.SDK) < 14) {
                    cursor.close();
                }
            } catch (Exception e) {
                Log.e("转换地址", "error:" + e);
            }
        }
        return filePath;
    }

    private int REQUEST_CODE = 20;

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // 先判断有没有权限
            if (Environment.isExternalStorageManager()) {
                writeFile();
            } else {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQUEST_CODE);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 先判断有没有权限
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                writeFile();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE);
            }
        } else {
            writeFile();
        }
    }

    private void writeFile() {
        Intent intent = getIntent();

        Uri uri = intent.getData();

        if (uri != null) {

            String scheme = uri.getScheme();

            String path = uri.getPath();

            String query = uri.getQuery();

            Uri fileUrl = new Uri.Builder().scheme(scheme).encodedAuthority(uri.getAuthority()).path(path).query(query).build();
            String filePath = getFilePathFromContentUri(fileUrl, this);
            Log.e("TAG", "onCreate:filePath " + filePath);// /storage/emulated/0/Download/Browser/斗牛.zip

            if (uncompress(new File(filePath), getSaveFilePath(), "")) {
                scanMp3File();
                Toast.makeText(this, "解压成功", Toast.LENGTH_SHORT).show();
            }
            Log.e("TAG", "onCreate: " + fileUrl); // content://media/external_primary/downloads/224069

        } else {
            Log.e("TAG", "onCreate:uri==null ");

        }
        // MediaStore.Audio.Media.

    }

    private String getSaveFilePath() {
        //  /storage/emulated/0/Download/jay
        String savePath = Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS).getAbsolutePath();
        return new File(savePath, "jay").getAbsolutePath();
    }

    private void scanMp3File() {

        ArrayList arrayList = getAllDataFileName(getSaveFilePath());
        String[] res = new String[arrayList.size()];
        res = (String[]) arrayList.toArray(new String[0]);
        MediaScannerConnection.scanFile(this, res, new String[]{"video/mp4", "audio/mp3"}, new MediaScannerConnection.MediaScannerConnectionClient() {
            @Override
            public void onMediaScannerConnected() {
                Log.e("TAG", "onMediaScannerConnected:  ");

            }

            @Override
            public void onScanCompleted(String path, Uri uri) {
                //  /data/user/0/naman14.timber/files/jay/斗牛/周杰伦 - 斗牛.mp3
                Log.e("TAG", "onScanCompleted :  " + path);
                Log.e("TAG", "onScanCompleted :  " + uri);

            }
        });

        //    new MediaScanner(this).scanFile(new File(getFilesDir(), "jay"), "audio/mp3");
        startActivity(new Intent(this, MainActivity.class));
    }

    public static ArrayList<String> getAllDataFileName(String folderPath) {
        ArrayList<String> fileList = new ArrayList<>();
        File file = new File(folderPath);
        File[] tempList = file.listFiles();
        for (int i = 0; i < tempList.length; i++) {
            if (tempList[i].isFile()) {
                String filePath = tempList[i].getPath();
                if (tempList[i].length() > 100 & tempList[i].getName().contains("mp3")) {    //  根据自己的需要进行类型筛选
                    fileList.add(filePath);
                }
            } else {
                fileList.addAll(getAllDataFileName(tempList[i].getPath()));
            }
        }
        return fileList;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                writeFile();
            } else {
                Toast.makeText(this, "存储权限获取失败", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                writeFile();
            } else {
                Toast.makeText(this, "存储权限获取失败", Toast.LENGTH_SHORT).show();


            }
        }
    }

    /**
     * 解压
     *
     * @param sourceFile 解压源文件，会在源文件所在目录下新建一个unzip文件夹存放解压后的文件
     * @param password   密码
     */
    public static boolean uncompress(File sourceFile, String targetPath, String password) {
        File dir = new File(targetPath);
        dir.mkdir();

        ZipFile zipFile = new ZipFile(sourceFile);
        zipFile.setCharset(StandardCharsets.UTF_8);
        try {
            if (zipFile.isEncrypted()) {
                zipFile.setPassword(password.toCharArray());
            }
            zipFile.extractAll(targetPath);
            Log.e("TAG", " 解压 完成 ");
            return true;
        } catch (ZipException e) {
            Log.e("TAG", " 解压异常ZipException: " + e);
            return false;
        }
    }


}