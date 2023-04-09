package com.example.safetylife

import android.app.AlertDialog
import android.app.AlertDialog.Builder
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.safetylife.data.nightSetting
import com.example.safetylife.data.pushSetting
import com.example.safetylife.data.soundSetting



class settings : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        getSupportActionBar()?.setTitle("Настройки")
        val pushSwitch = findViewById<Switch>(R.id.switch1)
        val nightSwitch = findViewById<Switch>(R.id.switch2)
        val soundSwitch = findViewById<Switch>(R.id.switch4)
        pushSwitch.setOnClickListener{
            pushSetting = pushSwitch.isChecked
        }
        nightSwitch.setOnClickListener{
            nightSetting = nightSwitch.isChecked
        }
        soundSwitch.setOnClickListener{
            soundSetting = soundSwitch.isChecked
        }

        val nightbut = findViewById<TextView>(R.id.textView10)
        nightbut.setOnClickListener{
            showAlertDialogButtonClicked()

        }

    }

    fun openControl(v : View) {
        val intent = Intent(this, Control::class.java)
        startActivity(intent)
    }

    fun openAlert(v : View) {
        val intent = Intent(this, FullScreenAlert::class.java)
        startActivity(intent)
    }

    fun settingPriority(v: View){
        startActivity(Intent(Settings.ACTION_SETTINGS))
    }
    fun showAlertDialogButtonClicked() {
        // Create an alert builder
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Name")

        // set the custom layout
        val customLayout: View = layoutInflater.inflate(R.layout.login_dialog, null)
        builder.setView(customLayout)

        // add a button
        builder.setPositiveButton("OK") { dialog: DialogInterface?, which: Int ->
            // send data from the AlertDialog to the Activity
            val editText = customLayout.findViewById<EditText>(R.id.dialogNameEt)
            sendDialogDataToActivity(editText.text.toString())
        }


        // Слушатель для отмены AlertDialog'а

        // create and show the alert dialog
        val dialog = builder.create()
        dialog.show()
    }

    // Do something with the data coming from the AlertDialog
    private fun sendDialogDataToActivity(data: String) {
        Toast.makeText(this, data, Toast.LENGTH_SHORT).show()
    }
}