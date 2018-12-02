package pite.android.aut.bme.hu.raspberryapp

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_menu.*
import pite.android.aut.bme.hu.raspberryapp.network.RaspberryAPI

class MenuActivity : AppCompatActivity() {


   // private val raspberryAPI = RaspberryAPI()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)
        val cookie = intent.getStringExtra("cookie")
        val _xsrf = intent.getStringExtra("xsrf")

        val raspberryAPI = RaspberryAPI.instance
        var zones = raspberryAPI.getZonesList()

        btnStartPCA.setOnClickListener {
            val zone: HashMap<String, Boolean> = hashMapOf(swBedroom.text.toString() to swBedroom.isChecked,swDiningRoom.text.toString() to swDiningRoom.isChecked, swFrontEntrance.text.toString() to swFrontEntrance.isChecked,
                    swHallway.text.toString() to swHallway.isChecked,swLivingRoom.text.toString() to swLivingRoom.isChecked, swKitchen.text.toString() to swKitchen.isChecked)
            async {

                raspberryAPI.startPCA(cookie,_xsrf,zone)
            }
        }

        btnStopPCA.setOnClickListener {
            async {

                raspberryAPI.stopPCA(this)
            }
        }
    }

    private fun async(call: () -> String) {
        Thread {
            val response = call()
            runOnUiThread { showResponse(response) }
        }.start()
    }

    private fun showResponse(response: String) {
        tvResponse2.text = response
        //mywebview!!.loadUrl(response)
    }
}
