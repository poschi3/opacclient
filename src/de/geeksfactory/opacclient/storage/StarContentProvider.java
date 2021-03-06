package de.geeksfactory.opacclient.storage;

import java.util.List;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

public class StarContentProvider extends ContentProvider {
	private StarDatabase database;

	public static final String STAR_TYPE = "star";
	public static final String AUTHORITY = "de.geeksfactory.opacclient.starprovider";
	public static final String BASE_URI = "content://" + AUTHORITY + "/";
	public static final Uri STAR_URI = Uri.parse(BASE_URI + STAR_TYPE);

	private static enum Mime {
		STAR_ITEM, STAR_DIR
	}

	@Override
	public boolean onCreate() {
		database = new StarDatabase(getContext());
		return true;
	}

	private static final String MIME_PREFIX = "vnd.android.cursor.";
	private static final String STAR_MIME_POSTFIX = "/vnd.de.opacapp.type"
			+ STAR_TYPE;
	private static final String STAR_DIR_MIME = MIME_PREFIX + "dir"
			+ STAR_MIME_POSTFIX;
	private static final String STAR_ITEM_MIME = MIME_PREFIX + "item"
			+ STAR_MIME_POSTFIX;

	private static Mime getTypeMime(Uri uri) {
		if (!AUTHORITY.equals(uri.getAuthority())
				&& !uri.getAuthority().startsWith("de.opacapp.")) {
			return null;
		}
		List<String> segments = uri.getPathSegments();
		if (segments == null || segments.size() == 0) {
			return null;
		}

		String type = segments.get(0);
		if (STAR_TYPE.equals(type)) {
			switch (segments.size()) {
			case 1:
				return Mime.STAR_DIR;
			case 2:
				return Mime.STAR_ITEM;
			default:
				return null;
			}
		} else {
			return null;
		}
	}

	@Override
	public String getType(Uri uri) {
		switch (getTypeMime(uri)) {
		case STAR_DIR:
			return STAR_DIR_MIME;
		case STAR_ITEM:
			return STAR_ITEM_MIME;
		default:
			return null;
		}
	}

	private int deleteInDatabase(String table, String whereClause,
			String[] whereArgs) {
		return database.getWritableDatabase().delete(table, whereClause,
				whereArgs);
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		int rowsAffected;
		switch (getTypeMime(uri)) {
		case STAR_DIR:
			rowsAffected = deleteInDatabase(StarDatabase.STAR_TABLE, selection,
					selectionArgs);
			break;
		case STAR_ITEM:
			rowsAffected = deleteInDatabase(StarDatabase.STAR_TABLE,
					StarDatabase.STAR_WHERE_ID, selectionForUri(uri));
			break;
		default:
			rowsAffected = 0;
			break;
		}

		if (rowsAffected > 0) {
			notifyUri(uri);
		}
		return rowsAffected;
	}

	private long insertIntoDatabase(String table, ContentValues values) {
		return database.getWritableDatabase()
				.insertOrThrow(table, null, values);
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		Uri itemUri;
		long id;
		switch (getTypeMime(uri)) {
		case STAR_DIR:
			id = insertIntoDatabase(StarDatabase.STAR_TABLE, values);
			itemUri = ContentUris.withAppendedId(STAR_URI, id);
			notifyUri(STAR_URI);
			break;
		default:
			itemUri = null;
			break;
		}
		if (itemUri != null) {
			notifyUri(uri);
		}
		return itemUri;
	}

	private Cursor queryDatabase(String table, String[] projection,
			String selection, String[] selectionArgs, String groupBy,
			String having, String orderBy) {
		return database.getReadableDatabase().query(table, projection,
				selection, selectionArgs, groupBy, having, orderBy);
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		Cursor cursor;
		switch (getTypeMime(uri)) {
		case STAR_DIR:
			cursor = queryDatabase(StarDatabase.STAR_TABLE, projection,
					selection, selectionArgs, null, null, sortOrder);
			break;
		case STAR_ITEM:
			cursor = queryDatabase(StarDatabase.STAR_TABLE, projection,
					StarDatabase.STAR_WHERE_ID, selectionForUri(uri), null,
					null, sortOrder);
			break;
		default:
			return null;
		}
		cursor.setNotificationUri(getContext().getContentResolver(), uri);
		return cursor;
	}

	private int updateInDatabase(String table, ContentValues values,
			String selection, String[] selectionArgs) {
		return database.getWritableDatabase().update(table, values, selection,
				selectionArgs);
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		int rowsAffected;
		switch (getTypeMime(uri)) {
		case STAR_DIR:
			rowsAffected = updateInDatabase(StarDatabase.STAR_TABLE, values,
					selection, selectionArgs);
			break;
		case STAR_ITEM:
			rowsAffected = updateInDatabase(StarDatabase.STAR_TABLE, values,
					StarDatabase.STAR_WHERE_ID, selectionForUri(uri));
			break;
		default:
			rowsAffected = 0;
			break;
		}

		if (rowsAffected > 0) {
			notifyUri(uri);
		}
		return rowsAffected;
	}

	private void notifyUri(Uri uri) {
		getContext().getContentResolver().notifyChange(uri, null);
	}

	private String[] selectionForUri(Uri uri) {
		return new String[] { String.valueOf(ContentUris.parseId(uri)) };
	}
}