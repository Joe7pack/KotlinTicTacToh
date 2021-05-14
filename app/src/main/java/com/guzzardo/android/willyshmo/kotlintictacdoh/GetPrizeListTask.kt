package com.guzzardo.android.willyshmo.kotlintictacdoh

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.guzzardo.android.willyshmo.kotlintictacdoh.WebServerInterface.converseWithWebServer
import kotlinx.coroutines.runBlocking
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

// An AsyncTask that will be used to get a list of available prizes

class GetPrizeListTask {
    private lateinit var mCallerActivity: FusedLocationActivity
    //private var applicationContext: Context? = null
    private var mPrizesAvailable: String? = null

    fun main(callerActivity: FusedLocationActivity, resources: Resources, startMainActivity: String) = runBlocking {
        //var prizesAvailable: String? = null
        mCallerActivity = callerActivity
        mResources = resources
        mStartMainActivity = startMainActivity.toBoolean()
        writeToLog("GetPrizeListTask","main() called at: " + SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date()))
        val longitude = WillyShmoApplication.longitude
        val latitude = WillyShmoApplication.latitude
        mCallerActivity.setGettingPrizesCalled()
        val url = mResources!!.getString(R.string.domainName) + "/prize/getPrizesByDistance/?longitude=" + longitude + "&latitude=" + latitude
        try {
            mPrizesAvailable = converseWithWebServer(url, null, mCallerActivity, mResources!!)
            mCallerActivity.setPrizesRetrievedFromServer()
        } catch (e: Exception) {
            writeToLog("GetPrizeListTask", "doInBackground: " + e.message)
            mCallerActivity.sendToastMessage("Playing without host server")
        }
        writeToLog("GetPrizeListTask", "WebServerInterfaceUsersOnlineTask doInBackground called usersOnline: $mPrizesAvailable")
        postExecute()
    }

    private fun postExecute() {
        try {
            writeToLog("GetPrizeListTask","onPostExecute called usersOnline: $mPrizesAvailable")
            writeToLog("GetPrizeListTask", "onPostExecute called at: " + SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date()))
            mCallerActivity.prizeLoadInProgress()
            if (mStartMainActivity) {
                val willyShmoApplicationContext = WillyShmoApplication.willyShmoApplicationContext
                val myIntent = Intent(willyShmoApplicationContext, MainActivity::class.java)
                mCallerActivity.startActivity(myIntent)
                mCallerActivity.finish()
            }
            if (mPrizesAvailable != null && mPrizesAvailable!!.length > 20) {
                getPrizesAvailable()
                mCallerActivity.setPrizesLoadIntoObjects()
                convertStringsToBitmaps()
                savePrizeArrays()
                mCallerActivity.setPrizesLoadedAllDone()
            }
        } catch (e: Exception) {
            writeToLog("GetPrizeListTask", "onPostExecute exception called " + e.message)
            mCallerActivity.sendToastMessage(e.message)
        }
        writeToLog("GetPrizeListTask", "onPostExecute completed at: " + SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
        )
    }

    private fun getPrizesAvailable() {
        val prizes = parsePrizeList()
        val userKeySet: Set<String> = prizes.keys // this is where the keys (userNames) gets sorted
        val objectArray: Array<Any> = prizes.keys.toTypedArray()
        mPrizeNames = arrayOfNulls(objectArray.size)
        mPrizeIds = arrayOfNulls(objectArray.size)
        mPrizeImages = arrayOfNulls(objectArray.size)
        mPrizeImageWidths = arrayOfNulls(objectArray.size)
        mPrizeImageHeights = arrayOfNulls(objectArray.size)
        mPrizeDistances = arrayOfNulls(objectArray.size)
        mBitmapImages = arrayOfNulls(objectArray.size)
        mPrizeUrls = arrayOfNulls(objectArray.size)
        mPrizeLocations = arrayOfNulls(objectArray.size)
        for (x in objectArray.indices) {
            mPrizeNames.set(x, objectArray[x] as String)
            val prizeValues: Array<String?>? = prizes.get(objectArray[x])
            mPrizeIds[x] = prizeValues!![0]
            val workString = prizeValues[1]?.let { StringBuilder(it) }
            val newImage = workString?.substring(1, workString.length - 1)
            mPrizeImages[x] = newImage
            mPrizeImageWidths[x] = prizeValues[2]
            mPrizeImageHeights[x] = prizeValues[3]
            mPrizeDistances[x] = prizeValues[4]
            mPrizeUrls[x] = prizeValues[5]
            mPrizeLocations[x] = prizeValues[6]
        }
    }

    private fun parsePrizeList(): TreeMap<String, Array<String?>> {
        val userTreeMap = TreeMap<String, Array<String?>>()
        try {
            val convertedPrizesAvailable = convertToArray(StringBuilder(mPrizesAvailable))
            val jsonObject = JSONObject(convertedPrizesAvailable)
            val prizeArray = jsonObject.getJSONArray("PrizeList")
            for (x in 0 until prizeArray.length()) {
                val prize = prizeArray.getJSONObject(x)
                val prizeId = prize.getInt("id")
                val distance = prize.getDouble("distance")
                val prizeName = prize.getString("name")
                val image = prize.getString("image")
                val prizeUrl = prize.getString("url")
                val location = prize.getString("location")
                val imageWidth = prize.getInt("imageWidth")
                val imageHeight = prize.getInt("imageHeight")
                val prizeArrayValues = arrayOfNulls<String>(7)
                prizeArrayValues[0] = Integer.toString(prizeId)
                prizeArrayValues[1] = image
                prizeArrayValues[2] = Integer.toString(imageWidth)
                prizeArrayValues[3] = Integer.toString(imageHeight)
                prizeArrayValues[4] = distance.toString()
                prizeArrayValues[5] = prizeUrl
                prizeArrayValues[6] = location
                userTreeMap[prizeName] = prizeArrayValues
            }
        } catch (e: JSONException) {
            writeToLog("GetPrizeListTask", "PrizeList: " + e.message)
            mCallerActivity.sendToastMessage(e.message)
        }
        return userTreeMap
    }

    private fun convertToArray(inString: StringBuilder): String {
        var inputString = inString
        var startValue = 0
        var start = 0
        var end = 0
        val replaceString = "\"prize:"
        start = inputString.indexOf(replaceString, startValue)
        end = inputString.indexOf("{", start + 1)
        inputString = inputString.replace(start - 1, end, "[")
        startValue = end
        for (x in end until inputString.length) {
            start = inputString.indexOf(replaceString, startValue)
            if (start > -1) {
                end = inputString.indexOf("{", start)
                inputString = inputString.replace(start, end, "")
                startValue = end
            } else {
                break
            }
        }
        end = inputString.length - 5
        start = inputString.indexOf("}}}", end)
        inputString = inputString.replace(start, inputString.length - 1, "}]}")
        return inputString.toString()
    }

    private fun convertStringsToBitmaps() {
        for (x in mPrizeIds.indices) {
            val imageStrings =
                mPrizeImages[x]!!.split(",".toRegex()).toTypedArray()
            val imageBytes = ByteArray(imageStrings.size)
            for (y in imageBytes.indices) {
                imageBytes[y] = imageStrings[y].toByte()
            }
            mBitmapImages[x] =
                BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        }
    }

    private fun savePrizeArrays() {
        if (WillyShmoApplication.isNetworkAvailable) {
            WillyShmoApplication.prizeIds = mPrizeIds
            WillyShmoApplication.prizeNames = mPrizeNames
            WillyShmoApplication.bitmapImages = mBitmapImages
            WillyShmoApplication.imageWidths = mPrizeImageWidths
            WillyShmoApplication.imageHeights = mPrizeImageHeights
            WillyShmoApplication.prizeDistances = mPrizeDistances
            WillyShmoApplication.prizeUrls = mPrizeUrls
            WillyShmoApplication.prizeLocations = mPrizeLocations
        }
    }

    companion object {
        private var mResources: Resources? = null
        private lateinit var mPrizeImages: Array<String?>
        private lateinit var mPrizeImageWidths: Array<String?>
        private lateinit var mPrizeImageHeights: Array<String?>
        private lateinit var mPrizeNames: Array<String?>
        private lateinit var mPrizeUrls: Array<String?>
        private lateinit var mPrizeLocations: Array<String?>
        private lateinit var mPrizeIds: Array<String?>
        private lateinit var mPrizeDistances: Array<String?>
        private lateinit var mBitmapImages: Array<Bitmap?>
        private var mStartMainActivity = false
        private fun writeToLog(filter: String, msg: String) {
            if ("true".equals(mResources!!.getString(R.string.debug), ignoreCase = true)) {
                Log.d(filter, msg)
            }
        }
    }
}