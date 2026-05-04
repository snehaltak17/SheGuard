package com.sheguard.helpline

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.sheguard.databinding.ActivityHelplineBinding

class HelplineActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHelplineBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHelplineBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.policeButton.setOnClickListener { dial("100") }
        binding.womenHelplineButton.setOnClickListener { dial("1091") }
        binding.ambulanceButton.setOnClickListener { dial("108") }
        binding.fireButton.setOnClickListener { dial("101") }
    }

    private fun dial(number: String) {
        startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
