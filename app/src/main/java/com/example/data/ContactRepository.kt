package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.DeviceContact
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ContactRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("ghostlink_device_contacts", Context.MODE_PRIVATE)
    private val KEY_CONTACTS = "saved_contacts_list"

    private val _contacts = MutableStateFlow<List<DeviceContact>>(emptyList())
    val contacts: StateFlow<List<DeviceContact>> = _contacts.asStateFlow()

    init {
        loadContacts()
    }

    private fun loadContacts() {
        val rawSet = prefs.getStringSet(KEY_CONTACTS, emptySet()) ?: emptySet()
        val list = rawSet.mapNotNull { DeviceContact.deserialize(it) }
            .sortedBy { it.name.lowercase() }
        _contacts.value = list
    }

    fun saveContact(name: String, ip: String, note: String = ""): DeviceContact {
        val current = _contacts.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.ip == ip }

        val contact = if (existingIndex >= 0) {
            current[existingIndex].copy(name = name.trim(), note = note.trim())
        } else {
            DeviceContact(name = name.trim(), ip = ip.trim(), note = note.trim())
        }

        if (existingIndex >= 0) {
            current[existingIndex] = contact
        } else {
            current.add(contact)
        }

        persist(current)
        return contact
    }

    fun updateContact(contact: DeviceContact) {
        val current = _contacts.value.toMutableList()
        val idx = current.indexOfFirst { it.id == contact.id }
        if (idx >= 0) {
            current[idx] = contact
            persist(current)
        }
    }

    fun deleteContact(contactId: String) {
        val current = _contacts.value.filter { it.id != contactId }
        persist(current)
    }

    fun findByIp(ip: String): DeviceContact? {
        return _contacts.value.firstOrNull { it.ip.trim() == ip.trim() }
    }

    private fun persist(list: List<DeviceContact>) {
        val serialized = list.map { it.serialize() }.toSet()
        prefs.edit().putStringSet(KEY_CONTACTS, serialized).apply()
        _contacts.value = list.sortedBy { it.name.lowercase() }
    }
}
