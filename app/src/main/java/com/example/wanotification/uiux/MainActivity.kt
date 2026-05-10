package com.example.wanotification.uiux

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button

import androidx.activity.ComponentActivity

class MainActivity :
    ComponentActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        val button = Button(this)

        button.text =
            "AKTIFKAN NOTIFICATION ACCESS"

        setContentView(button)

        button.setOnClickListener {

            startActivity(

                Intent(
                    Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
                )
            )
        }
    }
}