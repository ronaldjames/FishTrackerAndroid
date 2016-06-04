package com.kodingwithkyle.kyle.fishtracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Kyle on 5/17/2016.
 */
public class DatabaseConnector {
    // database name
    private static final String DATABASE_NAME = "Fish";

    private SQLiteDatabase database; // for interacting with the database
    private DatabaseOpenHelper databaseOpenHelper; // creates the database

    // public constructor for DatabaseConnector
    public DatabaseConnector(Context context) {
        // create a new DatabaseOpenHelper
        databaseOpenHelper = new DatabaseOpenHelper(context, DATABASE_NAME,
                null, 1);
    }

    // open the database connection
    public void open() throws SQLException {
        // create or open a database for reading/writing
        database = databaseOpenHelper.getWritableDatabase();
    }//end open()

    // close the database connection
    public void close() {
        if (database != null)
            database.close(); // close the database connection
    }//end close()

    public Cursor getAllMarkers(){

        return database.query(DATABASE_NAME, new String[]{"_id", "lat", "longitude", "species"}, null, null, null, null, null, null);

    }//end getAllMarkers()

    // inserts a new fish in the database
    public void insertFish(String lat, String longitude, int species, int bait,
                           String pounds, String ounces, String photoFilePath) {
        ContentValues newFish = new ContentValues();
        newFish.put("lat", lat);
        newFish.put("longitude", longitude);
        newFish.put("species", species);
        newFish.put("bait", bait);
        newFish.put("pounds", pounds);
        newFish.put("ounces", ounces);
        newFish.put("photoFilePath", photoFilePath);

        open(); // open the database
        database.insert(DATABASE_NAME, null, newFish);
        close(); // close the database

    }//end insertFish()

    // return a fish containing specified closet information
    public Cursor getOneFish(long rowId) {
        return database.query(DATABASE_NAME, null, "_id=" + rowId,
                null, null,
                null, null);
    }//end getOneFish()

    // delete the fish specified by the given String name
    public void deleteFish(long rowId) {
        open(); // open the database
        database.delete(DATABASE_NAME, "_id=" + rowId, null);
        close(); // close the database
    }//end deleteFish()

    private class DatabaseOpenHelper extends SQLiteOpenHelper {
        // constructor
        public DatabaseOpenHelper(Context context, String name,
                                  SQLiteDatabase.CursorFactory factory, int version) {
            super(context, name, factory, version);
        }

        // creates the fish table when the database is created
        @Override
        public void onCreate(SQLiteDatabase db) {
            // query to create a new table named closetItems
            String createQuery = "CREATE TABLE " + DATABASE_NAME
                    + "(_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + " lat TEXT, longitude TEXT, species INTEGER,"
                    + " bait INTEGER, pounds TEXT, ounces TEXT, photoFilePath TEXT);";

            db.execSQL(createQuery); // execute query to create the database
        }//end of onCreate()

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        }
    } // end class DatabaseOpenHelper
}//end DatabaseConnector.Class
