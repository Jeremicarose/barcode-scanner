  package com.arose.barcodescanner;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.barcode.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;

import java.util.List;

import static android.content.ContentValues.TAG;

public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISION_CODE = 222;
    private static final int READ_STORAGE_PERMISSION_CODE = 144;
    private static final int WRITE_STORAGE_PERMISSION_CODE = 144;

    Button btn_barcode;
     ImageView qr_image;
    TextView tvResult;
    BarcodeScanner scanner;

    InputImage inputImage;

    ActivityResultLauncher<Intent> cameraLauncher;
    ActivityResultLauncher<Intent> galleryLauncher; 

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btn_barcode = findViewById(R.id.btn_barcode);
        qr_image = findViewById(R.id.qr_image);
        tvResult = findViewById(R.id.result);
        scanner = BarcodeScanning.getClient();

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        Intent data = result.getData();
                        try {
                            Bitmap photo =(Bitmap) data.getExtras().get("data");
                            inputImage = InputImage.fromBitmap(photo, 0);
                            inputImage = InputImage.fromFilePath(MainActivity.this,data.getData());
                            processQr();
                        }catch (Exception e){
                            Log.d(TAG,"onActivityResult: "+e.getMessage());
                        }
                    }
                });
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override 
                    public void onActivityResult(ActivityResult result) {
                        Intent data = result.getData();
                        try {
                            inputImage = InputImage.fromFilePath(MainActivity.this,data.getData());
                        }catch (Exception e){
                            Log.d(TAG,"onActivityResult: "+e.getMessage());
                        }

                    }
                });

        btn_barcode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String[]options = {"camera","gallery"};
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Pick an Option");
                builder.setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(which == 0){
                            Intent camera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                            cameraLauncher.launch(camera);

                        }else {
                            Intent storage = new Intent();
                            storage.setType("Image/*");
                            storage.setAction(Intent.ACTION_GET_CONTENT);
                            galleryLauncher.launch(storage);
                        }

                    }
                });
                builder.show();

            }
        });
    }

    private void processQr(){
        qr_image.setVisibility(View.GONE);
        tvResult.setVisibility(View.VISIBLE);

        Task<List<Barcode>> result = scanner.process(inputImage)
                .addOnSuccessListener(new OnSuccessListener<List<Barcode>>() {
                    @Override
                    public void onSuccess(List<Barcode> barcodes) {
                        for (Barcode barcode: barcodes) {
                            int valueType = barcode.getValueType();
                            // See API reference for complete list of supported types
                            switch (valueType) {
                                case Barcode.TYPE_WIFI:
                                    String ssid = barcode.getWifi().getSsid();
                                    String password = barcode.getWifi().getPassword();
                                    int type = barcode.getWifi().getEncryptionType();

                                    tvResult.setText("SSID: "+ssid+"\n"+
                                            "Password: "+password+"\n"+
                                            "Type: "+type+"\n");
                                    break;
                                case Barcode.TYPE_URL:
                                    String title = barcode.getUrl().getTitle();
                                    String url = barcode.getUrl().getUrl();
                                    tvResult.setText("Title: "+title+"\n"+
                                            "Url: "+url+"\n");
                                    break;
                                default:
                                    String data = barcode.getDisplayValue();
                                    tvResult.setText("Data: "+data);
                            }
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "onFailure: "+e.getMessage());
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPermission( Manifest.permission.CAMERA, CAMERA_PERMISION_CODE);
    }

    public void checkPermission(String permission, int requestCode) {
        if(ContextCompat.checkSelfPermission(MainActivity.this, permission)== PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{permission}, requestCode);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == CAMERA_PERMISION_CODE){
            if(!(grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_DENIED)){
                Toast.makeText(MainActivity.this,"Camera Permission Denied", Toast.LENGTH_SHORT).show();
            }else{
                checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE, READ_STORAGE_PERMISSION_CODE);
            }
        }else if(requestCode == READ_STORAGE_PERMISSION_CODE){
            if(!(grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_DENIED)){
                Toast.makeText(MainActivity.this,"Storage Permission Denied", Toast.LENGTH_SHORT).show();
            }else{
                checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, WRITE_STORAGE_PERMISSION_CODE);
            }
        }else if(requestCode == WRITE_STORAGE_PERMISSION_CODE){
            if(!(grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_DENIED)){
                Toast.makeText(MainActivity.this,"Storage Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}