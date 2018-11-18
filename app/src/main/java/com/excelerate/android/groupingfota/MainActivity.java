package com.excelerate.android.groupingfota;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    //    public Uri getUriToDrawable(@NonNull Context context, @AnyRes int drawableId) {
//        Uri imageUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE +
//                "://" + context.getResources().getResourcePackageName(drawableId)
//                + '/' + context.getResources().getResourceTypeName(drawableId)
//                + '/' + context.getResources().getResourceEntryName(drawableId) );
//        return imageUri;
//    }

    private static boolean hasPermissions(Context context, String... permissions) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    public void createFileInDevice() {
        Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.check);
        if (icon == null) {
            Drawable myDrawable = getResources().getDrawable(R.drawable.check);
            icon = ((BitmapDrawable) myDrawable).getBitmap();
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        icon.compress(Bitmap.CompressFormat.PNG, 40, bytes);
//you can create a new file name "mms.png" in sdcard folder.
        File f = new File(Environment.getExternalStorageDirectory()
                + File.separator + "mms.png");
        boolean success = false;
        try {
            success = f.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
//write the bytes in file
        if (success) {
            FileOutputStream fo = null;
            try {
                fo = new FileOutputStream(f);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            try {
                if (fo != null) {
                    fo.write(bytes.toByteArray());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

// remember close de FileOutput
            try {
                if (fo != null) {
                    fo.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(this, "File already exists in memory", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        EditText url = findViewById(R.id.urlEditText);
        EditText phnNumber = findViewById(R.id.phnEditText);
        phnNumber.setHint("Enter Phone Number");

        url.setHint("E.g. www.google.com");
        if (Build.VERSION.SDK_INT >= 23) {
            String[] PERMISSIONS = {android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE};
            if (!hasPermissions(this, PERMISSIONS)) {
                ActivityCompat.requestPermissions(this, PERMISSIONS, 112);
            } else {
                createFileInDevice();
            }
        }
    }

    public Uri convertFileToContentUri(Context context, File file) throws Exception {

        //Uri localImageUri = Uri.fromFile(localImageFile); // Not suitable as it's not a content Uri

        ContentResolver cr = context.getContentResolver();
        String imagePath = file.getAbsolutePath();
        String imageName = null;
        String imageDescription = null;
        String uriString = MediaStore.Images.Media.insertImage(cr, imagePath, imageName, imageDescription);
        return Uri.parse(uriString);
    }

    public void sendMMS(View view) throws Exception {
        EditText phnNumber = findViewById(R.id.phnEditText);
        RadioButton smsRadioButton=findViewById(R.id.smsRadioButton);
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        if(smsRadioButton.isChecked()) {
            String message = "Testing SMS";
            sendIntent.setType("text/plain");
            boolean found = false;
            PackageManager manager = this.getPackageManager();
            List<ResolveInfo> infos = manager.queryIntentActivities(sendIntent, 0);
            if (!infos.isEmpty()) {
                for (ResolveInfo resolveInfo : infos) {
                    if (resolveInfo.activityInfo.name.toLowerCase().contains("mms")) {
                        sendIntent.putExtra("address", phnNumber.getText().toString());
                        sendIntent.putExtra("sms_body", message);
                        //sendIntent.putExtra(Intent.EXTRA_STREAM, attachment);
                        sendIntent.setPackage(resolveInfo.activityInfo.packageName);
                        found = true;
                        sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        break;
                    }
                }
                if (!found)
                    return;
            }
        }
        else{
            sendIntent.setType("image/png");
                File path = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "mms.png");
                Uri attachment = convertFileToContentUri(this, path);
                String message = "Testing MMS";
                boolean found=false;
                PackageManager manager = this.getPackageManager();
                List<ResolveInfo> infos = manager.queryIntentActivities(sendIntent, 0);
                if(!infos.isEmpty()){
                    for (ResolveInfo resolveInfo:infos) {
                        if(resolveInfo.activityInfo.name.toLowerCase().contains("mms")){
                            sendIntent.putExtra("address", phnNumber.getText().toString());
                            sendIntent.putExtra("sms_body", message);
                            sendIntent.putExtra(Intent.EXTRA_STREAM, attachment);
                            sendIntent.setPackage(resolveInfo.activityInfo.packageName);
                            found=true;
                            sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            break;
                        }
                    }
                    if(!found)
                        return;
            }
        }
        startActivity(sendIntent);
        finish();


        //Uri attachment=getUriToDrawable(this,R.drawable.check);


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 112: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    createFileInDevice();
                }
            }
        }
    }

    public void loadUrl(View view) {
        boolean found=false;
        EditText url = findViewById(R.id.urlEditText);
        Uri webpage = Uri.parse("http:" + url.getText().toString());
        Intent intent = new Intent(Intent.ACTION_VIEW, webpage);
        PackageManager manager = this.getPackageManager();
        List<ResolveInfo> infos = manager.queryIntentActivities(intent, 0);
        if(!infos.isEmpty()){
            for (ResolveInfo resolveInfo:infos) {
                if (resolveInfo.activityInfo.name.toLowerCase().contains("sbrowser")) {
                    intent.setPackage(resolveInfo.activityInfo.packageName);
                    found = true;
                    break;
                }
            }
            if(!found)
                return;
            startActivity(intent);
            finish();
        }
    }
}
