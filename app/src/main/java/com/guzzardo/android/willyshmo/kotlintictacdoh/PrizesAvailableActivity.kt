package com.guzzardo.android.willyshmo.kotlintictacdoh

import android.app.Activity
import android.app.ListFragment
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.ListView
import android.widget.Toast
import com.guzzardo.android.willyshmo.kotlintictacdoh.MainActivity.UserPreferences

class PrizesAvailableActivity : Activity(), ToastMessage {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mResources = resources
        errorHandler =
            ErrorHandler()
        val customTitleSupported =
            requestWindowFeature(Window.FEATURE_CUSTOM_TITLE)
        try {
            setContentView(R.layout.prize_frame)
            if (customTitleSupported) {
                window.setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.prizes_title)
            }
        } catch (e: Exception) {
            sendToastMessage(e.message)
        }
    }

    /**
     * This is the "top-level" fragment, showing a list of items that the
     * user can pick.  Upon picking an item, it takes care of displaying the
     * data to the user as appropriate based on the currrent UI layout.
     */
    class PrizesAvailableFragment : ListFragment() {
        var mDualPane = false
        var mCurCheckPosition = 0
        override fun onActivityCreated(savedInstanceState: Bundle?) {
            super.onActivityCreated(savedInstanceState)

            /* uncomment this when we figure out why getActivity is of the wrong type */
            val adapter = LazyAdapter(
                activity,
                WillyShmoApplication.prizeNames,
                WillyShmoApplication.bitmapImages,
                WillyShmoApplication.imageWidths,
                WillyShmoApplication.imageHeights,
                WillyShmoApplication.prizeDistances,
                WillyShmoApplication.prizeLocations,
                mResources!!
            )
            listAdapter = adapter

            // Check to see if we have a frame in which to embed the details
            // fragment directly in the containing UI.
            //View detailsFrame = getActivity().findViewById(R.id.details);
            //mDualPane = detailsFrame != null && detailsFrame.getVisibility() == View.VISIBLE;
            if (mDualPane) {
                // In dual-pane mode, the list view highlights the selected item.
                listView.choiceMode = ListView.CHOICE_MODE_SINGLE
                // Make sure our UI is in the correct state.
                showDetails(mCurCheckPosition)
            }
        }

        override fun onSaveInstanceState(outState: Bundle) {
            super.onSaveInstanceState(outState)
            outState.putInt("curChoice", mCurCheckPosition)
        }

        override fun onListItemClick(l: ListView, v: View, position: Int, id: Long) {
            val url = WillyShmoApplication.prizeUrls[position]
            if (url != null && url.length > 4) {
                val browserIntent =
                    Intent(Intent.ACTION_VIEW, Uri.parse("http://$url"))
                startActivity(browserIntent)
            }
        }

        /**
         * Helper function to show the details of a selected item, either by
         * displaying a fragment in-place in the current UI, or starting a
         * whole new activity in which it is displayed.
         */
        private fun showDetails(index: Int) {
            mCurCheckPosition = index

//            if (mDualPane) {
//                // We can display everything in-place with fragments, so update
//                // the list to highlight the selected item and show the data.
//                getListView().setItemChecked(index, true);
//
//                // Check what fragment is currently shown, replace if needed.
//                DetailsFragment details = (DetailsFragment)
//                        getFragmentManager().findFragmentById(R.id.details);
//                if (details == null || details.getShownIndex() != index) {
//                    // Make new fragment to show this selection.
//                    details = DetailsFragment.newInstance(index);
//
//                    // Execute a transaction, replacing any existing fragment
//                    // with this one inside the frame.
//                    FragmentTransaction ft = getFragmentManager().beginTransaction();
//                    if (index == 0) {
//                        ft.replace(R.id.details, details);
//                    } else {
//                        ft.replace(R.id.a_item, details);
//                    }
//                    ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
//                    ft.commit();
//                }
//
//            } else {
//                // Otherwise we need to launch a new activity to display
//                // the dialog fragment with selected text.
//                Intent intent = new Intent();
//                intent.setClass(getActivity(), DetailsActivity.class);
//                intent.putExtra("index", index);
//                startActivity(intent);
//            }
        }
    }

    //mPrizesAvailable = settings.getString("ga_prizes_available", null);
    //mPlayer1Id = settings.getInt(GameActivity.PLAYER1_ID, 0); 
    //mPlayer1Name = settings.getString(GameActivity.PLAYER1_NAME, null); 
    private val sharedPreferences: Unit
        private get() {
            val settings = getSharedPreferences(UserPreferences.PREFS_NAME, Context.MODE_PRIVATE)
            //mPrizesAvailable = settings.getString("ga_prizes_available", null);
            //mPlayer1Id = settings.getInt(GameActivity.PLAYER1_ID, 0); 
            //mPlayer1Name = settings.getString(GameActivity.PLAYER1_NAME, null); 
        }

    inner class ErrorHandler : Handler() {
        override fun handleMessage(msg: Message) {
            Toast.makeText(applicationContext, msg.obj as String, Toast.LENGTH_LONG).show()
        }
    }

    override fun sendToastMessage(message: String?) {
        val msg = errorHandler!!.obtainMessage()
        msg.obj = message
        errorHandler!!.sendMessage(msg)
    }

    companion object {
        var errorHandler: ErrorHandler? = null
        private var mResources: Resources? = null
        private fun writeToLog(filter: String, msg: String) {
            if ("true".equals(mResources!!.getString(R.string.debug), ignoreCase = true)
            ) { Log.d(filter, msg) }
        }
    }
}