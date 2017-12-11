package com.lyman.video.system;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.lyman.video.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TakePhotoActivity extends AppCompatActivity {
    private static final String TAG = "TakePhotoActivity";
    private static final int REQUEST_IMAGE_DEFAULT_CAPTURE = 1;
    private static final int REQUEST_IMAGE_CUSTOM_PATH_CAPTURE = 2;
    private static final int REQUEST_WRITE_EXTERNAL_STORAGE = 0;
    private ImageView mImageView;
    private String mCurrentPhotoPath;
    private boolean isAddToGallery = false;
    private boolean isNeedScalePhoto = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_photo);
        requestPermissions();
        mImageView = (ImageView) findViewById(R.id.imageView);
    }

    private void requestPermissions() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_WRITE_EXTERNAL_STORAGE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_WRITE_EXTERNAL_STORAGE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else {
                }
                return;
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_DEFAULT_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            mImageView.setImageBitmap(imageBitmap);
        }

        if (requestCode == REQUEST_IMAGE_CUSTOM_PATH_CAPTURE && resultCode == RESULT_OK
                && isNeedScalePhoto) {
            //mImageView.setImageURI(Uri.fromFile(new File(mCurrentPhotoPath)));
            setPic();
            return;
        }

        if (requestCode == REQUEST_IMAGE_CUSTOM_PATH_CAPTURE && resultCode == RESULT_OK
                && !isAddToGallery) {
            mImageView.setImageURI(Uri.fromFile(new File(mCurrentPhotoPath)));
            return;
        }


        if (requestCode == REQUEST_IMAGE_CUSTOM_PATH_CAPTURE && resultCode == RESULT_OK
                && isAddToGallery) {
            mImageView.setImageURI(Uri.fromFile(new File(mCurrentPhotoPath)));
            galleryAddPic();
            return;
        }
    }

    public void scalePhoto(View view) {
        if (!checkHasCamera()) return;
        isAddToGallery = false;
        isNeedScalePhoto = true;
        dispatchTakePictureCustomPathIntent();
    }

    private void setPic() {
        // Get the dimensions of the View
        int targetW = mImageView.getWidth();
        int targetH = mImageView.getHeight();
        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        mImageView.setImageBitmap(bitmap);
        Log.d(TAG, "setPic: scale success sample size is " + scaleFactor);
    }

    public void addToGallery(View view) {
        if (!checkHasCamera()) return;
        isAddToGallery = true;
        isNeedScalePhoto = false;
        dispatchTakePictureCustomPathIntent();
    }

    public void customPathTakePhotoIntent(View view) {
        if (!checkHasCamera()) return;
        isAddToGallery = false;
        isNeedScalePhoto = false;
        dispatchTakePictureCustomPathIntent();
    }

    private void dispatchTakePictureCustomPathIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File

            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.lyman.video.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CUSTOM_PATH_CAPTURE);
                Log.d(TAG, "dispatchTakePictureCustomPathIntent: custom picture path success");
            }
        }
    }


    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mCurrentPhotoPath);
        try {
            MediaStore.Images.Media.insertImage(getContentResolver(),
                    f.getAbsolutePath(), f.getName(), null);
            Log.d(TAG, "galleryAddPic: add to Media Scanner success");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.e(TAG, "galleryAddPic: add to Media Scanner failed");
        }

        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
        Toast.makeText(this, "Add to Gallery success", Toast.LENGTH_SHORT).show();
    }


    public void defaultTakePhotoIntent(View view) {
        if (!checkHasCamera()) return;
        dispatchTakePictureIntent();
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_DEFAULT_CAPTURE);
        }
    }

    private boolean checkHasCamera() {
        PackageManager packageManager = getPackageManager();
        boolean hasCamera = packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA);
        if (!hasCamera) {
            Toast.makeText(this, "without camera！！！", Toast.LENGTH_SHORT);
            return false;
        }
        return true;
    }


}
