package com.example.data_collection.Custom_DataCollection;

import static android.os.Build.VERSION.SDK_INT;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.data_collection.Camera_Usage.CameraConnectionFragment;
import com.example.data_collection.Camera_Usage.ImageUtils;
import com.example.data_collection.R;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CustomDataCollection extends AppCompatActivity  implements ImageReader.OnImageAvailableListener{

    LinearProgressIndicator lpi;
    static ExecutorService threadPool;
    static int count = 0;
    int n = 0;
    private LinearLayout mBottomSheetLayout;
    private BottomSheetBehavior sheetBehavior;
    private ImageView header_Arrow_Image;
    TextView Showing;
    Switch log,Zip;
    EditText Frames_et,label_et;
    public static String label_values,frames,name;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_datacollection);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,WindowManager.LayoutParams.FLAG_SECURE);



        threadPool = Executors.newFixedThreadPool(2);
        final MediaPlayer logon = MediaPlayer.create(this, R.raw.datastart);
        final MediaPlayer logoff = MediaPlayer.create(this, R.raw.datastop);
        lpi = findViewById(R.id.progressbar);
        Showing = findViewById(R.id.tvtxt);

        Frames_et= findViewById(R.id.frames);
        label_et=findViewById(R.id.labels);



        Zip = findViewById(R.id.zip);
        Zip.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(isChecked){
                try{
                    lpi.setVisibility(View.VISIBLE);
                    lpi.setProgress(0);
                    Showing.setText("Data Saving...");
                    Showing.setVisibility(View.VISIBLE);

                    new Thread(() -> {
                        ZipFilesCustom.main();
                        // Update progress on the UI thread
                        runOnUiThread(() -> {
                            lpi.setProgress(50);
                            Showing.setText("Data Saving... 50%");
                        });
                        DeleteDirectoryCustom.main();
                        // Update progress on the UI thread
                        runOnUiThread(() -> {
                            lpi.setProgress(100);
                            Showing.setText("Data Saved 100%");
                            lpi.setVisibility(View.INVISIBLE);
                            Toast.makeText(this, "DataSaved", Toast.LENGTH_SHORT).show();
                            // Hide the TextView after 5 seconds
                            new Handler().postDelayed(() -> {
                                Showing.setVisibility(View.GONE);
                            }, 5000);
                        });
                    }).start();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        });
        log = findViewById(R.id.Log);
        log.setOnCheckedChangeListener((buttonView, isChecked) -> {

            if (isChecked) {

                label_values = label_et.getText().toString().trim();
                frames = Frames_et.getText().toString().trim();
                if (!label_values.equals("")&&!frames.equals("")){
                    logon.start();
                    count++;
                }
                else {
                    log.setChecked(false);
                    Toast.makeText(this, "Please Fill All Values", Toast.LENGTH_SHORT).show();
                }


            } else {
                logoff.start();
                n=0;

            }
        });


        //TODO ask for camera permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.CAMERA}, 121);
            } else {
                //TODO show live camera footage
                setFragment();
            }
        } else {
            setFragment();

        }


        setFragment();

        mBottomSheetLayout = findViewById(R.id.bottom_sheet_layout);
        sheetBehavior = BottomSheetBehavior.from(mBottomSheetLayout);
        header_Arrow_Image = findViewById(R.id.bottom_sheet_arrow);




        header_Arrow_Image.setOnClickListener(view -> {
            if (sheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED){
                sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }else{
                sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });

        sheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {

            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                header_Arrow_Image.setRotation((slideOffset * 180));

            }
        });

    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //TODO show live camera footage
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            //TODO show live camera footage
            if (SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    
                    setFragment();

                } else {
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    Uri uri = Uri.fromParts("package", CustomDataCollection.this.getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                }
            }
            setFragment();

        } else {
            finish();
        }
    }



    //TODO fragment which show live footage from camera
    int previewHeight = 0, previewWidth = 0;
    int sensorOrientation;

    protected void setFragment() {
        final CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        String cameraId = null;
        try {
            cameraId = Arrays.toString(manager.getCameraIdList());
            Log.d("Camera NAMe @ IDS",cameraId);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        try {
            cameraId = manager.getCameraIdList()[0];
            // the camera Part change the id will change ex: .getCameraIdList()[1] for front camera
            // cameraId = manager.getCameraIdList()[1];
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        CameraConnectionFragment fragment;
        CameraConnectionFragment camera2Fragment =
                CameraConnectionFragment.newInstance(
                        (size, rotation) -> {
                            previewHeight = size.getHeight();
                            previewWidth = size.getWidth();
                            Log.d("tryOrientation", "rotation: " + rotation + "   orientation: " + getScreenOrientation() + "  " + previewWidth + "   " + previewHeight);
                            sensorOrientation = rotation - getScreenOrientation();
                        },
                        this,
                        R.layout.camera_fragment,
                        new Size(640, 480));

        camera2Fragment.setCamera(cameraId);
        fragment = camera2Fragment;
        getFragmentManager().beginTransaction().replace(R.id.fragmentContainerView, fragment).commit();
    }

    protected int getScreenOrientation() {
        switch (getWindowManager().getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_270:
                return 270;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_90:
                return 90;
            default:
                return 0;
            case Surface.ROTATION_0:
                break;
        }
        return 0;
    }

    //TODO getting frames of live camera footage and passing them to model
    private boolean isProcessingFrame = false;
    private final byte[][] yuvBytes = new byte[3][];
    private int[] rgbBytes = null;
    private int yRowStride;
    private Runnable postInferenceCallback;
    private Runnable imageConverter;
    private Bitmap rgbFrameBitmap;


    public void onImageAvailable(ImageReader reader) {
        // We need wait until we have some size from onPreviewSizeChosen
        if (previewWidth == 0 || previewHeight == 0) {
            return;
        }
        if (rgbBytes == null) {
            rgbBytes = new int[previewWidth * previewHeight];
        }
        try {
            final Image image = reader.acquireLatestImage();

            if (image == null) {
                return;
            }

            if (isProcessingFrame) {
                image.close();
                return;
            }
            isProcessingFrame = true;
            final Image.Plane[] planes = image.getPlanes();
            fillBytes(planes, yuvBytes);
            yRowStride = planes[0].getRowStride();
            final int uvRowStride = planes[1].getRowStride();
            final int uvPixelStride = planes[1].getPixelStride();

            imageConverter =
                    () -> ImageUtils.convertYUV420ToARGB8888(
                            yuvBytes[0],
                            yuvBytes[1],
                            yuvBytes[2],
                            previewWidth,
                            previewHeight,
                            yRowStride,
                            uvRowStride,
                            uvPixelStride,
                            rgbBytes);

            postInferenceCallback =
                    () -> {
                        image.close();
                        isProcessingFrame = false;
                    };

            processImage();

        } catch (final Exception e) {
            Log.d("tryError", e.getMessage());

        }

    }

    private void processImage() {
        imageConverter.run();
        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);
        rgbFrameBitmap.setPixels(rgbBytes, 0, previewWidth, 0, 0, previewWidth, previewHeight);
        //Do your work here

        if (log.isChecked()) {
            System.out.println("Switch is ON");
            SaveImage(rgbFrameBitmap);
            threadPool.execute(new TextThread());
            if (frames.equals(name)){
                log.setChecked(false);
                Toast.makeText(this, "Finished '"+frames+"' Frames", Toast.LENGTH_SHORT).show();
            }
        }


        postInferenceCallback.run();
    }

    protected void fillBytes(final Image.Plane[] planes, final byte[][] yuvBytes) {
        // Because of the variable row stride it's not possible to know in
        // advance the actual necessary dimensions of the yuv planes.
        for (int i = 0; i < planes.length; ++i) {
            final ByteBuffer buffer = planes[i].getBuffer();
            if (yuvBytes[i] == null) {
                yuvBytes[i] = new byte[buffer.capacity()];
            }
            buffer.get(yuvBytes[i]);
        }
    }


    public static String image_name;

    private void SaveImage(Bitmap finalBitmap) {

        File root = new File(Environment.getExternalStorageDirectory() + File.separator + "DataCollection/Dataset1/" + count + "/Images");
        root.mkdirs();
        String fname = "Image-" + n + ".jpg";
        name = String.valueOf(n);
        n++;

        File file = new File(root, fname);
        if (file.exists()) file.delete();
        try {
            FileOutputStream out = new FileOutputStream(file);
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 10, out);
            out.flush();
            out.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        image_name = fname;
        Log.i("image path", image_name);



    }


    static class TextThread implements Runnable {

        @Override
        public void run() {

            File formatter = new File("Frame");
            String fileName = formatter + ".csv";//like 2016_01_12.txt

            try {
                String cv_path = "DataCollection/" + "Dataset1/";

                File root = new File(Environment.getExternalStorageDirectory() + File.separator + cv_path + count, "Files");
                if (!root.exists()) {
                    root.mkdirs();

                }
                File text_file = new File(root, fileName);
                FileWriter writer = new FileWriter(text_file, true);
                String data =count+"/Images/"+image_name+","+label_values+"\n";

                    writer.write(data);
                    writer.flush();
                    writer.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}