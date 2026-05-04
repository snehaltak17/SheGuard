package com.sheguard.history

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.sheguard.R
import com.sheguard.contacts.ContactListActivity
import com.sheguard.dashboard.DashboardActivity
import com.sheguard.dashboard.IncidentAdapter
import com.sheguard.databinding.ActivityHistoryBinding
import com.sheguard.db.AppDatabase
import com.sheguard.db.Incident
import com.sheguard.settings.SettingsActivity
import kotlinx.coroutines.launch

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var appDatabase: AppDatabase
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var incidentAdapter: IncidentAdapter
    private val incidents = mutableListOf<Incident>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        appDatabase = AppDatabase.getDatabase(this)
        firebaseAuth = FirebaseAuth.getInstance()

        incidentAdapter = IncidentAdapter(incidents)
        binding.historyRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.historyRecyclerView.adapter = incidentAdapter

        binding.bottomNavigation.selectedItemId = R.id.nav_history
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, DashboardActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_contacts -> {
                    startActivity(Intent(this, ContactListActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                else -> true
            }
        }

        loadHistory()
    }

    private fun loadHistory() {
        val userId = firebaseAuth.currentUser?.uid ?: return
        lifecycleScope.launch {
            appDatabase.incidentDao().getIncidents(userId).collect { incidentList ->
                incidents.clear()
                incidents.addAll(incidentList)
                binding.emptyHistoryTextView.visibility = if (incidentList.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
                incidentAdapter.notifyDataSetChanged()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
