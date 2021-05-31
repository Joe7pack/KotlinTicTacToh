package com.guzzardo.android.willyshmo.kotlintictacdoh

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.ListView
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.ListFragment
import com.guzzardo.android.willyshmo.kotlintictacdoh.MainActivity.UserPreferences

class PrizesAvailableActivity : FragmentActivity(), ToastMessage {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.ErrorHandler()
        mResources = resources
        errorHandler = ErrorHandler()
        val customTitleSupported = requestWindowFeature(Window.FEATURE_CUSTOM_TITLE)
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
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

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
        }
    }

    inner class ErrorHandler : Handler(Looper.getMainLooper()) {
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