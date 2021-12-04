package de.luki2811.dev.inzidenzencheck

import android.app.Activity
import android.content.Context
import android.widget.*
import org.json.JSONException
import org.json.JSONObject
import java.net.MalformedURLException
import java.net.URL
import java.util.*

class CoronaData(private var context: Context, private var activity: Activity) : Thread() {
    override fun run() {
        val urlLand: URL
        val urlBund: URL
        try {
            urlLand =
                URL("https://services7.arcgis.com/mOBPykOjAyBO2ZKk/arcgis/rest/services/RKI_Landkreisdaten/FeatureServer/0/query?where=1%3D1&outFields=cases7_per_100k,county&returnGeometry=false&outSR=4326&f=json")
            urlBund =
                URL("https://services7.arcgis.com/mOBPykOjAyBO2ZKk/arcgis/rest/services/Coronaf%C3%A4lle_in_den_Bundesl%C3%A4ndern/FeatureServer/0/query?where=1%3D1&outFields=cases7_bl_per_100k,LAN_ew_GEN&returnGeometry=false&outSR=4326&f=json")
        } catch (e: MalformedURLException) {
            e.printStackTrace()
            return
        }
        val land = InternetRequest(urlLand, context, MainActivity.fileNameDataKreise)
        land.start()
        val bund = InternetRequest(urlBund, context, MainActivity.fileNameDataBundeslaender)
        bund.start()
        try {
            bund.join()
            land.join()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        setAutoCompleteList()
        val sendButton = activity.findViewById<Button>(R.id.button)
        val output = activity.findViewById<TextView>(R.id.textOutput)
        activity.runOnUiThread {
            sendButton.isEnabled = true
            output.text = activity.getString(R.string.click_to_start_abfrage)
        }
    }

    fun setAutoCompleteList() {
        var type = 0
        val radio_lk = activity.findViewById<RadioButton>(R.id.chip_landkreis)
        val radio_sk = activity.findViewById<RadioButton>(R.id.chip_stadtkreis)
        val radio_bl = activity.findViewById<RadioButton>(R.id.chip_bundesland)
        if (radio_bl.isChecked) type = Corona.BUNDESLAND
        if (radio_lk.isChecked) type = Corona.LANDKREIS
        if (radio_sk.isChecked) type = Corona.STADTKREIS
        var list: Array<String?>? = null
        println("setAuto: $type")
        if (type == Corona.BUNDESLAND) {
            val fileBund = Datein(MainActivity.fileNameDataBundeslaender)
            try {
                val jsonArrayBund = JSONObject(
                    fileBund.loadFromFile(
                        context
                    )
                ).getJSONArray("features")
                println(jsonArrayBund)
                val length = jsonArrayBund.length()
                list = arrayOfNulls(length)
                for (i in 0 until jsonArrayBund.length()) {
                    list[i] = jsonArrayBund.getJSONObject(i).getJSONObject("attributes")
                        .getString("LAN_ew_GEN")
                }
                println(Arrays.toString(list))

                // Shows null positions
                for (i in 0 until length - 1) {
                    if (list[i] == null) {
                        System.err.println("BL null at $i")
                    }
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
        if (type == Corona.LANDKREIS || type == Corona.STADTKREIS) {
            val fileLand = Datein(MainActivity.fileNameDataKreise)
            try {
                val jsonArrayLand = JSONObject(fileLand.loadFromFile(context)).getJSONArray("features")
                val length = jsonArrayLand.length()
                list = arrayOfNulls(length)
                for (i in 0 until jsonArrayLand.length()) {
                    val SkOrLk = jsonArrayLand.getJSONObject(i).getJSONObject("attributes").getString("county")
                    if (SkOrLk.contains("SK") && type == Corona.STADTKREIS) list[i] =
                        jsonArrayLand.getJSONObject(i).getJSONObject("attributes").getString("county").replace("SK ".toRegex(), "")
                    if (SkOrLk.contains("LK") && type == Corona.LANDKREIS) list[i] =
                        jsonArrayLand.getJSONObject(i).getJSONObject("attributes").getString("county").replace("LK ".toRegex(), "")
                }
                val liste: MutableList<String> = ArrayList()
                for (s in list) {
                    if (s != null && s.isNotEmpty()) {
                        liste.add(s)
                    }
                }
                list = arrayOfNulls(liste.size)
                list = liste.toTypedArray()
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
        if (list == null) return
        Arrays.sort(list)
        val autoTextView = activity.findViewById<AutoCompleteTextView>(R.id.textInput)
        val adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, list)
        activity.runOnUiThread { autoTextView.setAdapter(adapter) }
    }
}