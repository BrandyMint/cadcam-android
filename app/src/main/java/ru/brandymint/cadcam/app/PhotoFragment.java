package ru.brandymint.cadcam.app;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;

import ru.brandymint.cadcam.app.provider.DatabaseContract;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Large screen devices (such as tablets) are supported by replacing the ListView
 * with a GridView.
 * <p/>
 */
public class PhotoFragment extends Fragment implements AbsListView.OnItemClickListener {

    private static final boolean DBG = BuildConfig.DEBUG && true;
    private static final String TAG = "PhotoFragment";

    private OnFragmentInteractionListener mListener;

    /**
     * The fragment's ListView/GridView.
     */
    private AbsListView mListView;

    /**
     * The Adapter which will be used to populate the ListView/GridView with
     * Views.
     */
    private CursorAdapter mAdapter;

    public static PhotoFragment newInstance() {
        return new PhotoFragment();
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public PhotoFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_photo_grid, container, false);

        // Set the adapter
        mListView = (GridView) view.findViewById(android.R.id.list);
        ((AdapterView<ListAdapter>) mListView).setAdapter(mAdapter);

        // Set OnItemClickListener so we can be notified on item clicks
        mListView.setOnItemClickListener(this);

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mAdapter = new PhotoListAdapter(getActivity());
        ((AdapterView<ListAdapter>) mListView).setAdapter(mAdapter);

        getLoaderManager().initLoader(0, null, mLoaderCallbacks);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (null != mListener) {
            Cursor c = (Cursor) ((CursorAdapter) parent.getAdapter()).getItem(position);
            // Notify the active callbacks interface (the activity, if the
            // fragment is attached to one) that an item has been selected.
            mListener.onPhotoSelected(id, c.getString(c.getColumnIndex(DatabaseContract.Photos.URL)));
        }
    }

    public LoaderManager.LoaderCallbacks<Cursor> mLoaderCallbacks = new LoaderManager.LoaderCallbacks<Cursor>() {

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            String argCity;
            String selection;
            String selectionArgs[];

            if (DBG) Log.v(TAG, "onCreateLoader() " + id + " " + args);

            selection = null;
            selectionArgs = null;

            return new CursorLoader(
                    getActivity(),
                    DatabaseContract.Photos.CONTENT_URI,
                    null, null, null, null);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
            mAdapter.swapCursor(cursor);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> cursorLoader) {
            mAdapter.swapCursor(null);
        }
    };


    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        public void onPhotoSelected(long id, String photoUrl);
    }


    public static class PhotoListAdapter extends SimpleCursorAdapter {
        public PhotoListAdapter(Context context) {
            super(context,
                    R.layout.item_photo_grid,
                    null,
                    new String[]{
                            DatabaseContract.Photos.FILE_NAME,
                            DatabaseContract.Photos.CREATED_AT,
                            DatabaseContract.Photos.SYNC_STATUS,
                            DatabaseContract.Photos.COMMENT
                    },
                    new int[]{
                            R.id.photo,
                            R.id.date,
                            R.id.status,
                            R.id.comment
                    },
                    0
            );
            setViewBinder(new OurViewBinder(context));
        }

    }

    private static class OurViewBinder implements SimpleCursorAdapter.ViewBinder {
        private Picasso picasso;
        private SimpleDateFormat mDateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());

        private OurViewBinder(Context context) {
            picasso = Picasso.with(context);
            picasso.setDebugging(DBG);
        }

        @Override
        public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
            switch (view.getId()) {
                case R.id.photo:
                    String photoFilename = cursor.getString(columnIndex);
                    File filePath = new File(Utils.getPicturesDirectory(), photoFilename);
                    picasso.
                            load(filePath)
                            .fit()
                            .centerCrop()
                            .error(R.drawable.no_photo)
                            .into((ImageView) view);
                    break;
                case R.id.date:
                    long ts = cursor.getLong(columnIndex);
                    ((TextView) view).setText(mDateFormat.format(ts));
                    break;
                case R.id.status:
                    int status = cursor.getInt(columnIndex);
                    int statusStr;
                    switch (status) {
                        case DatabaseContract.SYNC_STATUS_NEW:
                            statusStr = R.string.sync_status_new;
                            break;
                        case DatabaseContract.SYNC_STATUS_UPLOADED:
                            statusStr = R.string.sync_status_uploaded;
                            break;
                        case DatabaseContract.SYNC_STATUS_UPLOADING:
                            statusStr = R.string.sync_status_uploading;
                            break;
                        default:
                            statusStr = R.string.sync_status_uploaded;
                            break;
                    }
                    ((TextView) view).setText(statusStr);
                    break;
                default:
                    return false;
            }
            return true;
        }
    }
}
