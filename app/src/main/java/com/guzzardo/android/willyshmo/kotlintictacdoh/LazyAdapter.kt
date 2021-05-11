package com.guzzardo.android.willyshmo.kotlintictacdoh

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Handler
import android.os.Message
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.FragmentActivity
import java.math.BigDecimal

class LazyAdapter(
    private val activity: FragmentActivity?,
    private val imageDescription: Array<String?>,
    private val imageBitmap: Array<Bitmap?>,
    private val imageWidth: Array<String?>,
    private val imageHeight: Array<String?>,
    private val prizeDistance: Array<String?>,
    private val prizeLocation: Array<String?>,
    private val resources: Resources
    ) : BaseAdapter(), ToastMessage {
        override fun getCount(): Int {
            if (imageDescription != null) {
                return imageDescription.size
            }
        return 0
        }

    override fun getItem(position: Int): Any {
        return position
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(
        position: Int,
        convertView: View?,
        parent: ViewGroup
    ): View {
        var vi = convertView
        try {
            if (convertView == null) {
                vi = inflater!!.inflate(R.layout.prizes, null)
            }
        } catch (e: Exception) {
            sendToastMessage("Lazy adapter inflater error: " + e.message)
            //			System.out.println("convert View: " + e.getMessage());
        }
        val text = vi?.findViewById<View>(R.id.prize_description) as TextView
        text.text = imageDescription?.get(position) ?: ""
        text.setBackgroundColor(Color.LTGRAY)
        val image = vi.findViewById<View>(R.id.prize_image) as ImageView
        val width = imageWidth[position]?.let { Integer.valueOf(it) }
        val height = imageHeight[position]?.let { Integer.valueOf(it) }
        image.layoutParams = width?.let { height?.let { it1 -> LinearLayout.LayoutParams(it, it1) } }
        image.setImageBitmap(imageBitmap[position])
        val textDistance = vi.findViewById<View>(R.id.prize_distance) as TextView
        if (prizeLocation[position] == "1") {
            val distance = prizeDistance[position]
            var decimal = BigDecimal(distance)
            decimal = decimal.setScale(2, BigDecimal.ROUND_UP)
            textDistance.text = decimal.toString()
        } else if (prizeLocation[position] == "0") {
            textDistance.text = resources.getString(R.string.not_applicable)
        } else if (prizeLocation[position] == "2") {
            textDistance.text = resources.getString(R.string.multiple_locations)
        } else {
            textDistance.text = "???"
        }
        return vi
    }

    override fun sendToastMessage(message: String?) {
        TODO("Not yet implemented")
    }

    override fun finish() {
        // TODO Auto-generated method stub
    }

    inner class ErrorHandler : Handler() {
        override fun handleMessage(msg: Message) {
            Toast.makeText(
                activity!!.applicationContext,
                msg.obj as String,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun startActivity(i: Intent?) {}

    companion object {
        private var inflater: LayoutInflater? = null
        var errorHandler: ErrorHandler? = null
    }

    init {
        inflater = activity!!.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }
}