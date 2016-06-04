package com.kodingwithkyle.kyle.fishtracker;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;

/**
 * Created by Kyle on 6/1/2016.
 */
public class DialogFrag extends DialogFragment {

    DeleteFishListener listener;
    long rowId;


    public static DialogFrag newInstance(long id) {
        DialogFrag f = new DialogFrag();

        // Supply num input as an argument.
        Bundle args = new Bundle();
        args.putLong("rowId", id);
        f.setArguments(args);

        return f;
    }


    public interface DeleteFishListener {
        void onFishDeleted();
    }

    //attach the Details fragment listener to the MainActivity
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        listener = (DeleteFishListener) context;
    }

    // remove DetailsFragmentListener when fragment detached
    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);

        rowId = getArguments().getLong("id");
        AlertDialog.Builder builder = null;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            builder = new AlertDialog.Builder(getContext());
        } else {
            builder = new AlertDialog.Builder(getActivity());
        }

        builder.setTitle("Delete");
        builder.setMessage("Are you sure?");

        // provide a delete button that dismisses the dialog
        builder.setPositiveButton("delete",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(
                            DialogInterface dialog, int button) {

                        final DatabaseConnector databaseConnector =
                                new DatabaseConnector(getActivity());

                        // AsyncTask deletes fish and notifies listener
                        AsyncTask<Long, Object, Object> deleteTask =
                                new AsyncTask<Long, Object, Object>() {
                                    @Override
                                    protected Object doInBackground(Long... params) {
                                        databaseConnector.deleteFish(params[0]);
                                        return null;
                                    }

                                    @Override
                                    protected void onPostExecute(Object result) {
                                        listener.onFishDeleted();
                                    }
                                }; // end new AsyncTask

                        // execute the AsyncTask to delete fish at rowID
                        deleteTask.execute(new Long[]{rowId});
                    } // end method onClick
                } // end anonymous inner class
        ); // end call to method setPositiveButton

        builder.setNegativeButton("cancel", null);
        return builder.create(); // return the AlertDialog
    }

}
