package pite.android.aut.bme.hu.raspberryapp.network

import android.content.Context
import android.util.Base64
import android.util.Log
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSession
import android.util.Base64.NO_WRAP
import android.widget.Toast
import com.beust.klaxon.Json
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import com.beust.klaxon.Parser
import okhttp3.*
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor
import com.franmontiel.persistentcookiejar.cache.SetCookieCache
import com.franmontiel.persistentcookiejar.PersistentCookieJar
import com.franmontiel.persistentcookiejar.ClearableCookieJar
import com.google.gson.Gson
import com.google.gson.GsonBuilder
//import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import java.io.Serializable
import java.net.CookiePolicy


public class RaspberryAPI private constructor(){

    /*** Make it to a Singleton Class ***/
    init {
        println("This ($this) is a singleton")
    }

    private object Holder {
        val INSTANCE = RaspberryAPI()
    }
    data class Zones(val title:Boolean?)
    data class Zone(val zones: Map<String,Boolean>)
    companion object {
        val instance: RaspberryAPI by lazy {
            Holder.INSTANCE
        }
        private var zones_json : String? = ""
        private var BASE_URL = "https://"
        private const val UTF_8 = "UTF-8"
        private const val TAG = "Network"
        private const val RESPONSE_ERROR = "ERROR"
    }
    //var base64EncodedCredentials = Base64.encodeToString(password.toByteArray(), Base64.NO_WRAP)

    class DO_NOT_VERIFY_IMP: javax.net.ssl.HostnameVerifier {
        override fun verify(p0: String?, p1: javax.net.ssl.SSLSession?): Boolean {
            return true
        }
    }
    val notVerify = DO_NOT_VERIFY_IMP()
    var csrf_token = ""
    var _xsrf = ""
    // init cookie manager
    var cookieJar = NonPersistentCookieJar()
    // init OkHttpClient
    private val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .hostnameVerifier(notVerify)
            .cookieJar(cookieJar)
            .build()

    /** ------------------ Private Methods ------------------ **/

    private fun httpGet(url: String): String {
        val request = Request.Builder()
                .url(url)
                .build()

        //The execute call blocks the thread

        val response = client.newCall(request).execute()
        val headers = response.headers()
        for(i in 0..headers.size()-1) {
            //println(headers.name(i) + ":" + headers.value(i))
            if(headers.name(i).equals("Set-Cookie"))
                csrf_token = headers.value(i)
        }
        println(request)
        println(request.headers())

        val index = csrf_token.indexOf('=')
        var string = csrf_token.substring(index + 1)
        val index2 = string.indexOf(';')
        string = string.substringBefore(';')
       // string = string.replace('|', '%')
        _xsrf = string
        println(response)
        println(response.headers())
        return response.body()?.string() ?: "EMPTY"
    }

    private fun httpGetControl(url: String) : String {
        val request = Request.Builder()
                .url(url)
                /*.addHeader("Authorization","Basic " + base64EncodedCredentials)
                .addHeader("x-csrf-token","fetch")*/
                .addHeader("Cookie", csrf_token)
                .build()

        //The execute call blocks the thread

        val response = client.newCall(request).execute()
        return response.body()?.string() ?: "EMPTY"
    }


    private fun httpGetZones(url: String) : String {
        val request = Request.Builder()
                .url(url)
                .addHeader("Cookie", csrf_token)
                .build()
        val response = client.newCall(request).execute()
        zones_json = response.body()?.string()
        //println("ZONAK: ")
        println(zones_json)
        return zones_json ?: "EMPTY"
    }

    private fun httpPost(url: String, password: String) : String {
        val stringbody = "_xsrf=" +_xsrf + "&password=" + password
        println(stringbody)
        val request = Request.Builder()
                .url(url)
               // .addHeader("Referer","https://192.168.1.106:8080/login?next=%2Fcontrol")
                .addHeader("X-Requested-With","XMLHttpRequest")
                .addHeader("Content-type","application/x-www-form-urlencoded; charset=UTF-8")
                //.addHeader("Authorization","Basic " + base64EncodedCredentials)
                .addHeader("X-Csrftoken",_xsrf)
                .header("Cookie", csrf_token)
                .post(RequestBody.create(MediaType.parse("application/x-www-form-urlencoded"),stringbody))
                .build()
        println(request)
        println(request.body().toString())
        println(request.headers())
        val response = client.newCall(request).execute()
        println(response)
        return response.body()?.string() ?: "EMPTY"

    }

    private fun httpPostStart(url: String,cookie:String, xsrf: String,zone:HashMap<String,Boolean>) : String {
        var i = 0
        val keys: MutableSet<String> = zone.keys
        /*val postMessage = "&on=true&zone=%7B%22bedroom%22%3A" + zone.getValue("Bedroom") +
                "+%2C%22living+room%22%3A" + zone.getValue("Living Room") +
                "+%2C%22hallway%22%3A" + zone.getValue("Hallway") +
                "+%2C%22kitchen%22%3A" + zone.getValue("Kitchen") +
                "+%2C%22dining+room%22%3A" + zone.getValue("Dining Room") +
                "+%2C%22front+entrance%22%3A" + zone.getValue("Front Entrance") + "+%7D"*/
        var postMessage = "&on=true&zone=%7B%22"
        for(zone in zone) {
            var zone_string = zone.key.toString().toLowerCase()
            zone_string = zone_string.replace("\\s".toRegex(), "+")
            postMessage += zone_string + "%22%3A" + zone.value + "+%2C%22"
        }
        postMessage = postMessage.replaceAfterLast("+", "%7D")
        println(postMessage)
        val request = Request.Builder()
                .url(url)
                //.addHeader("Referer","https://192.168.1.106:8080/control")
                .addHeader("X-Requested-With","XMLHttpRequest")
                .addHeader("Content-type","application/x-www-form-urlencoded; charset=UTF-8")
                //.addHeader("Authorization","Basic " + base64EncodedCredentials)
                .addHeader("X-Csrftoken",_xsrf)
                .header("Cookie", cookie)
                .post(RequestBody.create(MediaType.parse("application/x-www-form-urlencoded"),postMessage))
                .build()

        val response = client.newCall(request).execute()
        return response.body()?.string() ?: "EMPTY"
    }

    /** ------------------ Public Methods ------------------ **/


    fun getZonesList() :MutableSet<String> {
        val parser: Parser = Parser()
        val stringBuilder: StringBuilder = StringBuilder(zones_json)
        val json: JsonObject = parser.parse(stringBuilder) as JsonObject
        return json.keys
    }

    fun setURL(url: String) {
        BASE_URL += url
    }

    fun setXSRFToken(token: String) {
        csrf_token = token
    }
    fun httpPostStop(url : String):String{
        val stringbody2 = "&on=off&zone="
        val request = Request.Builder()
                .url(url)
                //.addHeader("Referer","https://192.168.1.106:8080/control")
                .addHeader("X-Requested-With","XMLHttpRequest")
                .addHeader("Content-type","application/x-www-form-urlencoded; charset=UTF-8")
                //.addHeader("Authorization","Basic " + base64EncodedCredentials)
               // .addHeader("X-Csrftoken",_xsrf)
               // .header("Cookie", cookie)
                .post(RequestBody.create(MediaType.parse("application/x-www-form-urlencoded"),stringbody2))
                .build()
        println(request)
        println(request.body().toString())
        println(request.headers())
        val response = client.newCall(request).execute()
        println(response)
        println(response.headers())
        return response.body()?.string() ?: "EMPTY"
    }

    fun getFeed(userName: String, direction: Int): String {
        //TODO
        return ""
    }

    fun getMainPage(url: String) : String {
        return try {
            Log.d(TAG, "Call to $url")
            httpGet(BASE_URL)
        } catch (e: Exception) {
            e.printStackTrace()
            RESPONSE_ERROR
        }
    }

    fun getZones() :String {
        return try {
            val url = "$BASE_URL" + "/zones"
            Log.d(TAG,"Call to $url")
            httpGetZones(url)
        } catch (e: Exception) {
            e.printStackTrace()
            RESPONSE_ERROR
        }
    }
    fun postPass(password: String) : String {
        return try {
            val indexUrl = "$BASE_URL" + "/login"

            Log.d(TAG, "Call to $indexUrl")
            httpPost(indexUrl, password)
        } catch (e: Exception) {
            e.printStackTrace()
            RESPONSE_ERROR
        }

    }

    fun postPass2(password: String) : String {
        return try {
            val indexUrl = "$BASE_URL" + "/login?next=%2F"

            Log.d(TAG, "Call to $indexUrl")
            httpPost(indexUrl, password)
        } catch (e: Exception) {
            e.printStackTrace()
            RESPONSE_ERROR
        }
    }

    /* This method get /control page
    * return: get response
    */

    fun getControl() : String {
        return try {
            val indexUrl = BASE_URL + "/control"

            Log.d(TAG, "Call to $indexUrl")
            httpGetControl(indexUrl)
        } catch (e: Exception) {
            e.printStackTrace()
            RESPONSE_ERROR
        }
    }

    /* This method starts the PCA System
    * Send POST message to ~/control page
    * @param cookie: Cookie
    * @param xsrf: the _xsrf cookie from the Cookie
    * @param zone: map of the zone from the switch boxes
    * return: response */

    fun startPCA(cookie: String, xsrf: String, zone:HashMap<String,Boolean>) :String {
        return try {
            val indexUrl = BASE_URL + "/control"

            Log.d(TAG, "Call to $indexUrl")
            httpPostStart(indexUrl,cookie,xsrf, zone)
        } catch (e: Exception) {
            e.printStackTrace()
            RESPONSE_ERROR
        }

    }

    /* This method starts the PCA System
    * Send POST message to ~/control page
    * @param zone: map of the zone from the switch boxes
    * return: response */

    fun stopPCA(context: Context) :String {
        return try {
            val indexUrl = BASE_URL + "/control"

            Log.d(TAG, "Call to $indexUrl")
            httpPostStop(indexUrl)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context,e.printStackTrace().toString(),Toast.LENGTH_SHORT).show()
            RESPONSE_ERROR
        }
    }



}