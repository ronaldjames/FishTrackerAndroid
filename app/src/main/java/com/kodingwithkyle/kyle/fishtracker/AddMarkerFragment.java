package com.kodingwithkyle.kyle.fishtracker;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Kyle on 5/17/2016.
 */
public class AddMarkerFragment extends Fragment {

    private AddMarkerFragListener listener;
    private ImageView addEditImageView;
    private Button captureImageButton;
    private Button saveFishButton;
    private Spinner fishSpeciesSpinner;
    private int speciesSpinnerPosition, baitSpinnerPosition;

    private Spinner fishBaitSpinner;
    private EditText ouncesEditText;
    private EditText poundsEditText;
    private String ouncesText, poundsText;
    public String mCurrentPhotoPath;
    private LatLng latLng;
    private String latitude;
    private String longitude;
    private Activity activity = getActivity();

    private static final String JPEG_FILE_SUFFIX = ".jpg";
    private static final String JPEG_FILE_PREFIX = "IMG_";
    private static final String CAMERA_DIR = "/dcim/";
    private static final String ALBUM_NAME = "FishTracker";
    private static final int REQUEST_TAKE_PHOTO = 1;
    static final int MY_PERMISSION_ACCESS_WRITE_STORAGE = 13;
    static final int MY_PERMISSION_ACCESS_READ_STORAGE = 14;

    public interface AddMarkerFragListener {

        void onAddMarkerCompleted();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }//end onCreate()

    // set AddEditFragmentListener when Fragment attached
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        listener = (AddMarkerFragListener) context;
    }

    // remove AddEditFragmentListener when Fragment detached
    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.add_marker_fragment, container, false);
        activity = getActivity();
        Bundle arguments = getArguments();
        if (arguments != null) {
            latitude = arguments.getString("lat");
            longitude = arguments.getString("lng");
            latLng = new LatLng(Double.parseDouble(latitude), Double.parseDouble(longitude));
        }

        addEditImageView = (ImageView) view.findViewById(R.id.fishImageView);
        captureImageButton = (Button) view.findViewById(R.id.captureImageButton);
        fishSpeciesSpinner = (Spinner) view.findViewById(R.id.fishSpeciesSpinner);
        fishBaitSpinner = (Spinner) view.findViewById(R.id.baitSpinner);
        poundsEditText = (EditText) view.findViewById(R.id.poundsEditText);
        ouncesEditText = (EditText) view.findViewById(R.id.ouncesEditText);
        saveFishButton = (Button) view.findViewById(R.id.saveButton);
        captureImageButton = (Button) view
                .findViewById(R.id.captureImageButton);

        captureImageButton.setOnClickListener(clickListener);
        saveFishButton = (Button) view
                .findViewById(R.id.saveButton);
        saveFishButton.setOnClickListener(clickListener);
        return view;
    }//end onCreateView()


    View.OnClickListener clickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            if (v.getId() == R.id.captureImageButton) {
                isStoragePermissionGranted();
            } else if (v.getId() == R.id.saveButton) {
                saveFish();
            }
        }
    };

    private void saveFish() {
        Toast.makeText(getActivity(), "save", Toast.LENGTH_LONG).show();
        speciesSpinnerPosition = fishSpeciesSpinner.getSelectedItemPosition();
        baitSpinnerPosition = fishBaitSpinner.getSelectedItemPosition();
        ouncesText = ouncesEditText.getText().toString();
        poundsText = poundsEditText.getText().toString();

        // AsyncTask to save fish, then notify listener
        AsyncTask<Object, Object, Object> saveFishTask = new AsyncTask<Object, Object, Object>() {
            @Override
            protected Object doInBackground(Object... params) {

                // get DatabaseConnector to interact with the SQLite database
                DatabaseConnector databaseConnector = new DatabaseConnector(activity);

                databaseConnector.insertFish(latitude, longitude,
                        speciesSpinnerPosition,
                        baitSpinnerPosition,
                        poundsText, ouncesText,
                        mCurrentPhotoPath);

                mCurrentPhotoPath = null;// clear the photo path
                hideKeyboard();

                return null;
            }

            @Override
            protected void onPostExecute(Object result) {
                listener.onAddMarkerCompleted();
            }
        }; // end AsyncTask

        // save the fish item to the database using a separate thread
        saveFishTask.execute((Object[]) null);
    }

    public void isStoragePermissionGranted() {

        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(getActivity(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {

                Log.d("perm", "Permission is granted");
                dispatchTakePictureIntent();

            } else {

                Log.d("perm", "Permission is revoked");
                requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}
                        , MY_PERMISSION_ACCESS_WRITE_STORAGE);

            }
        } else { //permission is automatically granted on sdk<23 upon installation
            Log.d("perm", "Permission is granted");

        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case MY_PERMISSION_ACCESS_WRITE_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // storage-related task you need to do.
                   // dispatchTakePictureIntent();

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {

            // Create the File where the photo should go
            File photoFile = null;
            photoFile = createImageFile();

            // Continue only if the File was successfully created
            if (photoFile != null) {
                mCurrentPhotoPath = photoFile.getAbsolutePath();
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    private File createImageFile() {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = new File(Environment.getExternalStorageDirectory()
                + CAMERA_DIR
                + ALBUM_NAME);

        //if it worked
        if (storageDir != null) {
            Log.d("storage directory", "the storage directory was created");
            //next line makes the directory
            if (!storageDir.mkdirs()) {
                Log.d("storage directory", "the storage directory previously existed");
                //if it already exists...
                if (!storageDir.exists()) {
                    //something went wrong. Log it
                    Log.d("Album", "problem making the storage directory");
                    storageDir = null;
                }
            }
        }
        File image = null;
        try {
            image = File.createTempFile(
                    imageFileName,    /* prefix */
                    ".jpg", /* suffix */
                    storageDir        /* directory */
            );
        } catch (IOException e) {
            Log.d("Making Temp File prob", imageFileName + " " + " " + storageDir);
        }
        // Save a file: path for use with ACTION_VIEW intents
        //  mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_TAKE_PHOTO) {
                setPic();
                sendToGallery();
            }
        }
    }//end onActivityResult()

    private void setPic() {

        new Thread(new Runnable() {

            @Override
            public void run() {
                // Get the dimensions of the View
                int targetW = addEditImageView.getWidth();
                int targetH = addEditImageView.getHeight();

                // Get the dimensions of the bitmap
                BitmapFactory.Options bmOptions = new BitmapFactory.Options();
                bmOptions.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
                int photoW = bmOptions.outWidth;
                int photoH = bmOptions.outHeight;
                //  Toast.makeText(getActivity(), mCurrentPhotoPath, Toast.LENGTH_SHORT).show();
                int scaleFactor = 1;

                if (targetH > 0 || targetW > 0) {
                    // Determine how much to scale down the image
                    scaleFactor = Math.min(photoW / targetW, photoH / targetH);
                }
                // Decode the image file into a Bitmap sized to fill the View
                bmOptions.inJustDecodeBounds = false;
                bmOptions.inSampleSize = scaleFactor;
                bmOptions.inPurgeable = true;

                final Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);

                addEditImageView.post(new Runnable() {
                    @Override
                    public void run() {
                        addEditImageView.setImageBitmap(bitmap);
                    }
                });
            }
        }).start();
    }

    private void sendToGallery() {
        Log.d("sendToGallery", "putting the pic in the gallery");
        //then we need to send the pic to the gallery :)
        //create an intent
        Intent mediaScanIntent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE");
        //get the file

        File f = new File(mCurrentPhotoPath);
        //get a URI
        Uri contentUri = Uri.fromFile(f);
        //set the data of the intent...
        mediaScanIntent.setData(contentUri);
        //send it...
        getActivity().sendBroadcast(mediaScanIntent);
        //reset the current photopath
        // mCurrentPhotoPath = null;
    }

    private void hideKeyboard() {
        // Check if no view has focus:
        View view = getActivity().getCurrentFocus();
        if (view != null) {
            InputMethodManager inputManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }//end hideKeyboard()

}//end addMarkerFragment
