package com.sheguard.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sheguard.R
import com.sheguard.databinding.ItemIncidentBinding
import com.sheguard.db.Incident
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class IncidentAdapter(private val incidents: List<Incident>) :
    RecyclerView.Adapter<IncidentAdapter.IncidentViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IncidentViewHolder {
        val binding = ItemIncidentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return IncidentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: IncidentViewHolder, position: Int) {
        holder.bind(incidents[position])
    }

    override fun getItemCount(): Int = incidents.size

    inner class IncidentViewHolder(private val binding: ItemIncidentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(incident: Incident) {
            val formatter = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            binding.timestampTextView.text = formatter.format(Date(incident.timestamp))
            binding.locationTextView.text =
                "https://maps.google.com/?q=${incident.latitude},${incident.longitude}"
            binding.metaTextView.text = binding.root.context.getString(
                R.string.incident_status,
                "${incident.status} • ${binding.root.context.getString(R.string.contacts_notified, incident.contactsNotified)}"
            )
        }
    }
}
