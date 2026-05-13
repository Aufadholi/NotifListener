package com.example.wanotification.uiux

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast

import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat

import com.example.wanotification.R
import com.example.wanotification.config.SupportedApps
import com.example.wanotification.filter.ContactStore

class MainActivity :
    ComponentActivity() {

    private data class AppOption(
        val label: String,
        val packageName: String
    )

    private val appOptions = listOf(
        AppOption("WhatsApp", SupportedApps.WHATSAPP),
        AppOption("Instagram", SupportedApps.INSTAGRAM)
    )

    private lateinit var appSpinner: Spinner
    private lateinit var inputName: EditText
    private lateinit var addButton: Button
    private lateinit var listContainer: LinearLayout
    private lateinit var emptyHint: TextView
    private lateinit var countText: TextView

    private var selectedAppPackage: String =
        SupportedApps.WHATSAPP

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val accessButton =
            findViewById<Button>(R.id.btn_notification_access)

        appSpinner =
            findViewById(R.id.app_spinner)

        inputName =
            findViewById(R.id.input_contact_name)

        addButton =
            findViewById(R.id.btn_add_contact)

        listContainer =
            findViewById(R.id.allowed_list_container)

        emptyHint =
            findViewById(R.id.text_empty_hint)

        countText =
            findViewById(R.id.text_contact_count)

        accessButton.setOnClickListener {

            startActivity(

                Intent(
                    Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
                )
            )
        }

        setupAppSpinner()

        addButton.setOnClickListener {
            addContact()
        }
    }

    private fun setupAppSpinner() {

        val labels =
            appOptions.map { it.label }

        val adapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                labels
            )

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        appSpinner.adapter = adapter

        appSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long
                ) {

                    selectedAppPackage =
                        appOptions[position].packageName

                    refreshList()
                }

                override fun onNothingSelected(
                    parent: AdapterView<*>
                ) {
                    selectedAppPackage =
                        appOptions.first().packageName
                }
            }

        appSpinner.setSelection(0)
    }

    private fun addContact() {

        val rawName =
            inputName.text?.toString() ?: ""

        val result =
            ContactStore.addContact(
                this,
                selectedAppPackage,
                rawName
            )

        when (result) {

            ContactStore.AddResult.ADDED -> {
                inputName.setText("")
                showToast("Kontak ditambahkan")
            }

            ContactStore.AddResult.DUPLICATE ->
                showToast("Nama sudah ada")

            ContactStore.AddResult.LIMIT ->
                showToast("Maksimal 5 kontak per aplikasi")

            ContactStore.AddResult.INVALID ->
                showToast("Nama tidak valid")
        }

        refreshList()
    }

    private fun refreshList() {

        val list =
            ContactStore.getAllowedContacts(
                this,
                selectedAppPackage
            )

        val max =
            ContactStore.maxContacts()

        countText.text =
            "Kontak diizinkan: ${list.size}/$max"

        listContainer.removeAllViews()

        emptyHint.visibility =
            if (list.isEmpty()) View.VISIBLE else View.GONE

        list.forEach { name ->
            listContainer.addView(createRow(name))
        }
    }

    private fun createRow(
        name: String
    ): View {

        val row =
            LinearLayout(this)

        row.orientation =
            LinearLayout.HORIZONTAL

        val rowParams =
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

        rowParams.topMargin =
            dpToPx(8)

        row.layoutParams = rowParams

        row.setPadding(
            dpToPx(12),
            dpToPx(10),
            dpToPx(12),
            dpToPx(10)
        )

        row.setBackgroundResource(R.drawable.bg_card)

        val nameView =
            TextView(this)

        nameView.text = name

        nameView.setTextColor(
            ContextCompat.getColor(
                this,
                R.color.text_primary
            )
        )

        nameView.layoutParams =
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )

        val deleteButton =
            Button(this)

        deleteButton.text = "Hapus"

        deleteButton.setBackgroundResource(
            R.drawable.bg_button_secondary
        )

        deleteButton.setTextColor(
            ContextCompat.getColor(
                this,
                R.color.text_on_secondary
            )
        )

        deleteButton.isAllCaps = false

        deleteButton.setOnClickListener {
            ContactStore.removeContact(
                this,
                selectedAppPackage,
                name
            )
            refreshList()
        }

        row.addView(nameView)
        row.addView(deleteButton)

        return row
    }

    private fun dpToPx(
        value: Int
    ): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun showToast(
        message: String
    ) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}