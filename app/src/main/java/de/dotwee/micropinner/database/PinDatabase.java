package de.dotwee.micropinner.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import androidx.annotation.NonNull;
import androidx.collection.ArrayMap;
import android.util.Log;

import java.util.Map;

import de.dotwee.micropinner.BuildConfig;

import static android.database.DatabaseUtils.queryNumEntries;
import static de.dotwee.micropinner.tools.SQLiteStatementsLogger.logDelete;
import static de.dotwee.micropinner.tools.SQLiteStatementsLogger.logInsertWithOnConflict;
import static de.dotwee.micropinner.tools.SQLiteStatementsLogger.logUpdate;

/**
 * Created by lukas on 10.08.2016.
 */
public class PinDatabase extends SQLiteOpenHelper {
    /* integer columns */
    static final String COLUMN_ID = "_id";
    /* string columns */
    static final String COLUMN_TITLE = "title";
    static final String COLUMN_CONTENT = "content";
    static final String GROUP_COLUMN_NAME = "name";
    /* integer columns */
    static final String COLUMN_VISIBILITY = "visibility";
    static final String COLUMN_PRIORITY = "priority";
    static final String COLUMN_COLOR = "color";
    /* boolean columns */
    static final String COLUMN_PERSISTENT = "persistent";
    static final String COLUMN_SHOW_ACTIONS = "show_actions";
    /* foreign id columns */
    static final String COLUMN_GROUP_ID = "group_id";
    /* other */
    private static final String TABLE_PINS = "pins";
    private static final String TABLE_GROUPS = "groups";
    private static final String TAG = PinDatabase.class.getSimpleName();
    private static final String DATABASE_NAME = "comments.db";
    private static final int DATABASE_VERSION = 2;
    // Database creation sql statement
    private static final String CREATE_TABLE_GROUPS = "create table "
            + TABLE_GROUPS + "( "
            + COLUMN_ID + " integer primary key autoincrement, "
            + GROUP_COLUMN_NAME + " text not null unique"
            + "); ";
    private static final String CREATE_TABLE_PINS = "create table "
            + TABLE_PINS + "( "

            + COLUMN_ID + " integer primary key autoincrement, "
            + COLUMN_TITLE + " text not null, "
            + COLUMN_CONTENT + " text not null, "

            + COLUMN_VISIBILITY + " integer not null, "
            + COLUMN_PRIORITY + " integer not null, "
            + COLUMN_COLOR + " integer default null, "

            + COLUMN_PERSISTENT + " integer not null, "
            + COLUMN_SHOW_ACTIONS + " integer not null, "

            + COLUMN_GROUP_ID + " integer default null, "
            + "FOREIGN KEY("+ COLUMN_GROUP_ID +") REFERENCES "+ TABLE_GROUPS +"("+ COLUMN_ID +")"
            + ");";
    public static final String GROUP_NAME_UNLIMITED = "unlimited";
    private static final String CREATE_VALUE_GROUPS = "insert into "
            + TABLE_GROUPS + " (" + GROUP_COLUMN_NAME + ") values "
            + "(\"" + GROUP_NAME_UNLIMITED + "\")" ;
    private static final String[] columns = {
            PinDatabase.COLUMN_ID,
            PinDatabase.COLUMN_TITLE,
            PinDatabase.COLUMN_CONTENT,
            PinDatabase.COLUMN_VISIBILITY,
            PinDatabase.COLUMN_PRIORITY,
            PinDatabase.COLUMN_COLOR,
            PinDatabase.COLUMN_PERSISTENT,
            PinDatabase.COLUMN_SHOW_ACTIONS,
            PinDatabase.COLUMN_GROUP_ID,
    };
    private static PinDatabase instance = null;
    private final SQLiteDatabase database;

    private PinDatabase(@NonNull Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        database = getWritableDatabase();
    }

    public static synchronized PinDatabase getInstance(@NonNull Context context) {
        if (PinDatabase.instance == null) {
            PinDatabase.instance = new PinDatabase(context.getApplicationContext());
        }

        return PinDatabase.instance;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.beginTransaction();
        try {
            sqLiteDatabase.execSQL(CREATE_TABLE_GROUPS);
            sqLiteDatabase.execSQL(CREATE_TABLE_PINS);

            sqLiteDatabase.execSQL(CREATE_VALUE_GROUPS);
            sqLiteDatabase.setTransactionSuccessful();
        } catch(SQLException sql) {
            Log.e(TAG, "Could not upgrade database", sql);
            throw sql;
        }
        finally {
            sqLiteDatabase.endTransaction();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);

        sqLiteDatabase.beginTransaction();
        try {
            final String tempTable = "temp_pins";
            sqLiteDatabase.execSQL("alter table " + TABLE_PINS + " rename to " + tempTable);

            sqLiteDatabase.execSQL("drop table if exists " + TABLE_PINS);
            sqLiteDatabase.execSQL("drop table if exists " + TABLE_GROUPS);

            sqLiteDatabase.execSQL(CREATE_TABLE_GROUPS);
            sqLiteDatabase.execSQL(CREATE_TABLE_PINS);

            sqLiteDatabase.execSQL(CREATE_VALUE_GROUPS);

            // Copy data from old table
            // https://stackoverflow.com/questions/1559789/how-to-copy-data-between-two-tables-in-sqlite
            final String[] oldColumns = {
                    PinDatabase.COLUMN_ID,
                    PinDatabase.COLUMN_TITLE,
                    PinDatabase.COLUMN_CONTENT,
                    PinDatabase.COLUMN_VISIBILITY,
                    PinDatabase.COLUMN_PRIORITY,
                    PinDatabase.COLUMN_PERSISTENT,
                    PinDatabase.COLUMN_SHOW_ACTIONS,
            };
            final String allOldColumns = String.join(", ", oldColumns);
            sqLiteDatabase.execSQL("insert into "+TABLE_PINS+" ("+ allOldColumns +") select "+allOldColumns+" from "+tempTable+";");

            sqLiteDatabase.execSQL("drop table if exists " + tempTable);

            sqLiteDatabase.setTransactionSuccessful();
        } catch(SQLException sql) {
            Log.e(TAG, "Could not upgrade database", sql);
            throw sql;
        }
        finally {
            sqLiteDatabase.endTransaction();
        }

        Log.d(TAG, "Database '" + DATABASE_NAME + "' upgraded");
    }

    /**
     * This method decides whether a new pin should be created or updated in the database
     *
     * @param pin the pin to write
     */
    public void writePin(@NonNull PinSpec pin) {
        Log.i(TAG, "Write pin called for pin " + pin.toString());

        if (pin.getId() == -1) {
            createPin(pin);
        } else {
            updatePin(pin);
        }
    }

    /**
     * This method creates a pin within the database and gives it a unique id
     *
     * @param pin the pin to create
     */
    private void createPin(@NonNull PinSpec pin) {
        ContentValues contentValues = pin.toContentValues();

        if (BuildConfig.DEBUG) {
            logInsertWithOnConflict(PinDatabase.TABLE_PINS, null, contentValues, SQLiteDatabase.CONFLICT_NONE);
        }
        long id = database.insert(PinDatabase.TABLE_PINS, null, contentValues);
        Log.i(TAG, "Created new pin with id " + id);
        pin.setId(id);

        onDatabaseAction();
    }

    /**
     * This method updates a pin in the database without changing its id
     *
     * @param pin the pin to update
     */
    private void updatePin(@NonNull PinSpec pin) {
        ContentValues contentValues = pin.toContentValues();
        long id = pin.getId();

        String whereClause = PinDatabase.COLUMN_ID + " = ?";
        String[] whereArgs = new String[]{String.valueOf(id)};
        if (BuildConfig.DEBUG) {
            logUpdate(PinDatabase.TABLE_PINS, contentValues, whereClause, whereArgs);
        }
        database.update(PinDatabase.TABLE_PINS, contentValues, whereClause, whereArgs);
        Log.i(TAG, "Updated new pin with id " + id);
        pin.setId(id);

        onDatabaseAction();
    }

    /**
     * This method deletes a pin from the database
     *
     * @param pin to delete
     */
    public void deletePin(PinSpec pin) {
        long id = pin.getId();

        String whereClause = PinDatabase.COLUMN_ID + " = ?";
        String[] whereArgs = new String[]{String.valueOf(id)};
        if (BuildConfig.DEBUG) {
            logDelete(PinDatabase.TABLE_PINS, whereClause, whereArgs);
        }
        boolean success = database.delete(PinDatabase.TABLE_PINS, whereClause, whereArgs) > 0;
        Log.i(TAG, "Deleting pin with id " + id + "; success " + success);
        pin.setId(-1);

        onDatabaseAction();
    }

    public void deleteAll() {
        Log.i(TAG, "Deleting all pins");
        database.delete(PinDatabase.TABLE_PINS, null, null);
    }

    /**
     * This method returns the amount of entries in the pin database
     *
     * @return the amount of entries
     */
    public long count() {
        return queryNumEntries(database, PinDatabase.TABLE_PINS);
    }

    /**
     * This method gets called on insert() and delete()
     */
    private void onDatabaseAction() {
        long count = count();

        Log.i(TAG, "onDatabaseAction() count " + count);
    }

    public String getGroup(long groupId) {
        // TODO: optimize this
         return getAllGroups().get(groupId);
    }
    /**
     * This method returns a map of all groups in the database with their id as key.
     *
     * @return map of all groups
     */
    public Map<Long, String> getAllGroups() {
        Map<Long, String> groupMap = new ArrayMap<>();

        try (Cursor cursor = database.query(PinDatabase.TABLE_GROUPS, new String[]{COLUMN_ID, GROUP_COLUMN_NAME}, null, null, null, null, null)) {
            if (cursor.moveToFirst()) {
                while (!cursor.isAfterLast()) {
                    ContentValues contentValues = new ContentValues();
                    DatabaseUtils.cursorRowToContentValues(cursor, contentValues);
                    long id = contentValues.getAsLong(COLUMN_ID);
                    groupMap.put(id, contentValues.getAsString(GROUP_COLUMN_NAME));
                    cursor.moveToNext();
                }
            }
            return groupMap;
        }
    }

    /**
     * This method returns a map of all pins in the database with their id as key
     *
     * @return map of all pins
     */
    @NonNull
    public Map<Integer, PinSpec> getAllPinsMap() {
        Map<Integer, PinSpec> pinMap = new ArrayMap<>();

        Cursor cursor = database.query(PinDatabase.TABLE_PINS, columns, null, null, null, null, null);
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                PinSpec pinSpec = new PinSpec(cursor);
                pinMap.put(pinSpec.getIdAsInt(), pinSpec);
                cursor.moveToNext();
            }
        }

        cursor.close();
        return pinMap;
    }
}
