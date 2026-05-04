package com.sheguard.contacts

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.sheguard.R
import com.sheguard.dashboard.DashboardActivity
import com.sheguard.databinding.ActivityContactListBinding
import com.sheguard.db.AppDatabase
import com.sheguard.db.Contact
import com.sheguard.history.HistoryActivity
import com.sheguard.settings.SettingsActivity
import kotlinx.coroutines.launch

class ContactListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityContactListBinding
    private lateinit var appDatabase: AppDatabase
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var contactAdapter: ContactAdapter
    private val contacts = mutableListOf<Contact>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContactListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        appDatabase = AppDatabase.getDatabase(this)
        firebaseAuth = FirebaseAuth.getInstance()

        contactAdapter = ContactAdapter(
            contacts = contacts,
            onEditClicked = { contact ->
                startActivity(Intent(this, AddContactActivity::class.java).apply {
                    putExtra(AddContactActivity.EXTRA_CONTACT_ID, contact.id)
                })
            },
            onDeleteClicked = { contact -> deleteContact(contact) }
        )

        binding.contactsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.contactsRecyclerView.adapter = contactAdapter
        binding.bottomNavigation.selectedItemId = R.id.nav_contacts

        binding.addContactFab.setOnClickListener {
            startActivity(Intent(this, AddContactActivity::class.java))
        }

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, DashboardActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                R.id.nav_history -> {
                    startActivity(Intent(this, HistoryActivity::class.java))
                    finish()
                    true
                }
                else -> true
            }
        }

        loadContacts()
    }

    private fun loadContacts() {
        val userId = firebaseAuth.currentUser?.uid ?: return
        lifecycleScope.launch {
            appDatabase.contactDao().getContacts(userId).collect { contactList ->
                contacts.clear()
                contacts.addAll(contactList)
                binding.emptyContactsTextView.visibility = if (contactList.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
                contactAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun deleteContact(contact: Contact) {
        lifecycleScope.launch {
            appDatabase.contactDao().deleteContact(contact.id)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
