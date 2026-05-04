package com.sheguard.contacts

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.sheguard.R
import com.sheguard.databinding.ActivityAddContactBinding
import com.sheguard.db.AppDatabase
import com.sheguard.db.Contact
import kotlinx.coroutines.launch

class AddContactActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddContactBinding
    private lateinit var appDatabase: AppDatabase
    private lateinit var firebaseAuth: FirebaseAuth
    private var editingContactId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddContactBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        appDatabase = AppDatabase.getDatabase(this)
        firebaseAuth = FirebaseAuth.getInstance()
        editingContactId = intent.getIntExtra(EXTRA_CONTACT_ID, -1)

        if (editingContactId != -1) {
            binding.toolbar.title = getString(R.string.edit_contact)
            loadContact()
        }

        binding.saveButton.setOnClickListener { saveContact() }
    }

    private fun loadContact() {
        lifecycleScope.launch {
            val contact = appDatabase.contactDao().getContactById(editingContactId) ?: return@launch
            binding.nameEditText.setText(contact.name)
            binding.phoneEditText.setText(contact.phone)
            binding.relationshipEditText.setText(contact.relationship)
            binding.priorityEditText.setText(contact.priorityLevel.toString())
        }
    }

    private fun saveContact() {
        val userId = firebaseAuth.currentUser?.uid ?: return
        val name = binding.nameEditText.text.toString().trim()
        val phone = binding.phoneEditText.text.toString().trim()
        val relationship = binding.relationshipEditText.text.toString().trim()
        val priority = binding.priorityEditText.text.toString().trim().toIntOrNull()

        if (name.isEmpty() || phone.isEmpty() || relationship.isEmpty() || priority == null) {
            Toast.makeText(this, getString(R.string.all_fields_required), Toast.LENGTH_SHORT).show()
            return
        }

        val contact = Contact(
            id = if (editingContactId != -1) editingContactId else 0,
            name = name,
            phone = phone,
            relationship = relationship,
            priorityLevel = priority,
            userId = userId
        )

        lifecycleScope.launch {
            if (editingContactId == -1) {
                appDatabase.contactDao().insert(contact)
                Toast.makeText(this@AddContactActivity, getString(R.string.contact_saved), Toast.LENGTH_SHORT).show()
            } else {
                appDatabase.contactDao().update(contact)
                Toast.makeText(this@AddContactActivity, getString(R.string.contact_updated), Toast.LENGTH_SHORT).show()
            }
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    companion object {
        const val EXTRA_CONTACT_ID = "extra_contact_id"
    }
}
