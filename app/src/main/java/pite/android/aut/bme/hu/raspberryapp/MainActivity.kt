package pite.android.aut.bme.hu.raspberryapp

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.EditText
import kotlinx.android.synthetic.main.activity_main.*
import pite.android.aut.bme.hu.raspberryapp.network.RaspberryAPI
import java.util.*

class MainActivity : AppCompatActivity() {

    var raspberryAPI = RaspberryAPI.instance
   // var mywebview: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        async{raspberryAPI.getIndex()}

        btnLogin.setOnClickListener {
            async {

                //raspberryAPI.getIndex()
                raspberryAPI.postPass(etPassword.text.toString())
                raspberryAPI.postPass2(etPassword.text.toString())
                raspberryAPI.getControl()
              //  rasberryAPI.start
            }
            val intent = Intent(this,MenuActivity::class.java)
            intent.putExtra("cookie",raspberryAPI.csrf_token)
            intent.putExtra("xsrf", raspberryAPI._xsrf)
            //intent.putExtra("raspib", raspberryAPI)
            startActivity(intent)
        }
    }

    fun getCookie() :String {
        return raspberryAPI.csrf_token
    }
    fun sendPassword(view: View) {

    }
    private fun showResponse(response: String) {
       // tvResponse.text = response
        //mywebview!!.loadUrl(response)
    }

    private fun async(call: () -> String) {
        Thread {
            val response = call()
            runOnUiThread { showResponse(response) }
        }.start()
    }
}
