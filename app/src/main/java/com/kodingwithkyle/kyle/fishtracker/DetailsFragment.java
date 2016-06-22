package com.kodingwithkyle.kyle.fishtracker;

import android.app.DialogFragment;
import android.app.Fragment;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

/**
 * Created by Kyle on 5/17/2016.
 */
public class DetailsFragment extends Fragment {

    private Spinner baitSpinner, speciesSpinner;
    private ImageView fishImage;
    private TextView pounds, ounces;
    private long rowId;//row Id of the query

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        setRetainInstance(true); // save fragment across config changes

        // get Bundle of arguments
        Bundle arguments = getArguments();

        //extract the row id from the bundle if the bundle is not null
        if (arguments != null) {
            rowId = arguments.getLong("rowId");
        }
        DatabaseConnector db = new DatabaseConnector(getActivity());
        new LoadFishTask().execute(rowId); // load fish at rowId
        // inflate DetailsFragment's layout
        View view =
                inflater.inflate(R.layout.details_fragment, container, false);
        setHasOptionsMenu(true); // this fragment has menu items to display

        // get the EditTexts
        fishImage = (ImageView) view.findViewById(R.id.detailsFrag_imageView);
        speciesSpinner = (Spinner) view.findViewById(R.id.speciesSpinner);
        baitSpinner = (Spinner) view.findViewById(R.id.detailsFrag_baitSpinner);
        pounds = (TextView) view.findViewById(R.id.poundsTextView);
        ounces = (TextView) view.findViewById(R.id.ouncesTextView);
        return view;
    }//end onCreateView

    //create an options menu
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_details_menu, menu);
    }

    // handle option item selections
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_delete:
                deleteFish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }//end onOptionsItemSelected()

    private void deleteFish() {
        DialogFragment confDelete = new DialogFrag();
        Bundle arguments = new Bundle();
        arguments.putLong("id", rowId);
        confDelete.setArguments(arguments);
        // use FragmentManager to display the confirmDelete DialogFragment
        confDelete.show(getFragmentManager(), "confirm delete");
    }//end deleteFish()

    // performs database query outside UI thread
    private class LoadFishTask extends AsyncTask<Long, Object, Cursor> {
        DatabaseConnector databaseConnector =
                new DatabaseConnector(getActivity());

        // open database & get Cursor representing specified fishes data
        @Override
        protected Cursor doInBackground(Long... params) {
            databaseConnector.open();
            return databaseConnector.getOneFish(params[0]);
        }

        // use the Cursor returned from the doInBackground method
        @Override
        protected void onPostExecute(Cursor result) {
            super.onPostExecute(result);

            result.moveToFirst(); // move to the first item

            // get the column index for each data item
            int speciesIndex = result.getColumnIndex("species");
            int baitIndex = result.getColumnIndex("bait");
            int poundsIndex = result.getColumnIndex("pounds");
            int ouncesIndex = result.getColumnIndex("ounces");
            int imageIndex = result.getColumnIndex("photoFilePath");

            //get the path to the picture associated with marker and  get the thumbnail
            String pathName = result.getString(imageIndex);

            if(pathName != null){
               setPic(pathName);
            }
            else{
                fishImage.setImageBitmap(BitmapFactory.decodeResource(getResources(),
                        R.drawable.fish_icon_final));
            }

            speciesSpinner.setSelection(result.getInt(speciesIndex));
            baitSpinner.setSelection(result.getInt(baitIndex));
            pounds.setText(result.getString(poundsIndex));
            ounces.setText(result.getString(ouncesIndex));
            result.close(); // close the result cursor
            databaseConnector.close(); // close database connection
        } // end method onPostExecute

    } // end class LoadFishTask

    private void setPic(final String pathName) {
        new Thread(new Runnable() {

            @Override
            public void run() {
                // Get the dimensions of the View
                int targetW = fishImage.getWidth();
                int targetH = fishImage.getHeight();

                // Get the dimensions of the bitmap
                BitmapFactory.Options bmOptions = new BitmapFactory.Options();
                bmOptions.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(pathName, bmOptions);
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

                final Bitmap bitmap = BitmapFactory.decodeFile(pathName, bmOptions);

                fishImage.post(new Runnable() {
                    @Override
                    public void run() {
                        fishImage.setImageBitmap(bitmap);
                    }
                });
            }
        }).start();
    }

}//end of DetailsFragment
