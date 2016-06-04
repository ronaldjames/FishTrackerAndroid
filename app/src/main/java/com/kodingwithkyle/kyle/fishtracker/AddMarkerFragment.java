package com.kodingwithkyle.kyle.fishtracker;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by Kyle on 5/17/2016.
 */
public class AddMarkerFragment extends Fragment {
    private static final String LOG_TAG = "logtag";

    private static final int REQUEST_TAKE_PHOTO = 2;

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
    private static final String CAMERA_DIR = "/DCIM/";
    private static final String ALBUM_NAME = "FishTracker";


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
        captureImageButton.setOnClickListener(captureImageButtonClicked);
        saveFishButton = (Button) view
                .findViewById(R.id.saveButton);
        saveFishButton.setOnClickListener(saveButtonClicked);
        return view;
    }//end onCreateView()

    View.OnClickListener captureImageButtonClicked = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            //we need an intent...
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
        }


    };


    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_TAKE_PHOTO) {
                Bitmap thumbnail = (Bitmap) data.getExtras().get("data");

                File album = null;
                if (isExternalStorageWritable()) {

                    album = getAlbumStorageDir();
                }

                File myImageFile = new File(album,
                        JPEG_FILE_PREFIX + System.currentTimeMillis() + JPEG_FILE_SUFFIX);

                FileOutputStream fo;
                try {
                    mCurrentPhotoPath = myImageFile.getAbsolutePath();
                    fo = new FileOutputStream(myImageFile);
                    thumbnail.compress(Bitmap.CompressFormat.JPEG, 90, fo);
                    fo.flush();
                    fo.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                addEditImageView.setImageBitmap(thumbnail);
            }
        }

    }//end onActivityResult()

    // Checks if external storage is available for read and write
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    public File getAlbumStorageDir() {
        // Get the directory for the user's public pictures directory.
        File album = new File(Environment.getExternalStorageDirectory() + CAMERA_DIR + ALBUM_NAME);
        if (!album.mkdirs()) {
            Log.e(LOG_TAG, "Directory not created");
        }
        return album;
    }

    // responds to event generated when user saves a closet item
    View.OnClickListener saveButtonClicked = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
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
                      //  saveFish(); // save fish item to the database
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
    }; // end saveButtonClicked

    private void hideKeyboard() {
        // Check if no view has focus:
        View view = getActivity().getCurrentFocus();
        if (view != null) {
            InputMethodManager inputManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }//end hideKeyboard()
}//end addMarkerFragment
