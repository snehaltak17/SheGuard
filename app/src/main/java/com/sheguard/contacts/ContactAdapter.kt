package com.sheguard.contacts

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sheguard.databinding.ItemContactBinding
import com.sheguard.db.Contact

class ContactAdapter(
    private val contacts: List<Contact>,
    private val onEditClicked: (Contact) -> Unit,
    private val onDeleteClicked: (Contact) -> Unit
) : RecyclerView.Adapter<ContactAdapter.ContactViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val binding = ItemContactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ContactViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        holder.bind(contacts[position])
    }

    override fun getItemCount(): Int = contacts.size

    inner class ContactViewHolder(private val binding: ItemContactBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(contact: Contact) {
            binding.nameTextView.text = contact.name
            binding.phoneTextView.text = contact.phone
            binding.metaTextView.text = "${contact.relationship} • Priority ${contact.priorityLevel}"
            binding.editButton.setOnClickListener { onEditClicked(contact) }
            binding.deleteButton.setOnClickListener { onDeleteClicked(contact) }
        }
    }
}
