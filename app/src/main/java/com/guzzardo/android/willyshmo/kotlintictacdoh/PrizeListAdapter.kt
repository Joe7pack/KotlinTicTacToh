package com.guzzardo.android.willyshmo.kotlintictacdoh

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.FragmentActivity
import java.math.BigDecimal

class PrizeListAdapter(
    activity: FragmentActivity?,
    private val imageDescription: Array<String?>,
    private val imageBitmap: Array<Bitmap?>,
    private val imageWidth: Array<String?>,
    private val imageHeight: Array<String?>,
    private val prizeDistance: Array<String?>,
    private val prizeLocation: Array<String?>,
    private val resources: Resources) : BaseAdapter(), ToastMessage {

        override fun getCount(): Int {
            return imageDescription.size
        }

        override fun getItem(position: Int): Any {
            return position
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var vi = convertView
            try {
                if (convertView == null) {
                    vi = inflater!!.inflate(R.layout.prizes, null)
                }
            } catch (e: Exception) {
                sendToastMessage("PrizeListAdapter inflater error: " + e.message) //this won't work since we never set mApplicationContext!
                writeToLog("PrizeListAdapter", "getView: $e.message")
            }
            val prizeDescription = vi?.findViewById<View>(R.id.prize_description) as TextView
            prizeDescription.text = imageDescription[position] ?: ""
            prizeDescription.setBackgroundColor(Color.LTGRAY)
            val image = vi.findViewById<View>(R.id.prize_image) as ImageView
            val width = imageWidth[position]?.let { Integer.valueOf(it) }
            val height = imageHeight[position]?.let { Integer.valueOf(it) }
            image.layoutParams = width?.let { height?.let { it1 -> LinearLayout.LayoutParams(it, it1) } }
            image.setImageBitmap(imageBitmap[position])
            val textDistance = vi.findViewById<View>(R.id.prize_distance) as TextView
            when {
                prizeLocation[position] == "1" -> {
                    val distance = prizeDistance[position]
                    var decimal = BigDecimal(distance)
                    decimal = decimal.setScale(2, BigDecimal.ROUND_UP)
                    textDistance.text = decimal.toString()
                }
                prizeLocation[position] == "0" -> {
                    textDistance.text = resources.getString(R.string.not_applicable)
                }
                prizeLocation[position] == "2" -> {
                    textDistance.text = resources.getString(R.string.multiple_locations)
                }
                else -> {
                    textDistance.text = "???"
                }
            }
            return vi
        }

        override fun sendToastMessage(message: String?) {
            val msg = mErrorHandler!!.obtainMessage()
            msg.obj = message
            FusedLocationActivity.mErrorHandler!!.sendMessage(msg)
        }

    class ErrorHandler : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            Toast.makeText(
                mApplicationContext,
                msg.obj as String,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    companion object {
        private var inflater: LayoutInflater? = null
        var mErrorHandler: ErrorHandler? = null
        private var mApplicationContext: Context? = null
        private lateinit var mResources: Resources
        private lateinit var mCallerActivity: Activity

        private fun writeToLog(filter: String, msg: String) {
            if ("true".equals(mResources.getString(R.string.debug), ignoreCase = true)) {
                Log.d(filter, msg)
            }
        }
    }

    init {
        inflater = activity!!.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        mCallerActivity = activity
        mErrorHandler = ErrorHandler()
        mResources = resources
    }
}