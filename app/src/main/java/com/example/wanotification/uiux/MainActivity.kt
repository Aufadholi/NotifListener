package com.example.wanotification.uiux

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.PopupProperties
import com.example.wanotification.listener.NotificationListener
import com.example.wanotification.config.SupportedApps
import com.example.wanotification.config.TTSSettingsManager
import com.example.wanotification.filter.ContactStore
import com.example.wanotification.ui.theme.SpaceCyan
import com.example.wanotification.ui.theme.SpaceIndigo
import com.example.wanotification.ui.theme.SpaceMuted
import com.example.wanotification.ui.theme.SpaceNavy
import com.example.wanotification.ui.theme.SpacePurple
import com.example.wanotification.ui.theme.SpaceText
import com.example.wanotification.ui.theme.SpaceBackgroundDeep
import com.example.wanotification.ui.theme.SpaceBackgroundMid
import com.example.wanotification.ui.theme.SpaceCardAppSelector
import com.example.wanotification.ui.theme.SpaceCardContactItem
import com.example.wanotification.ui.theme.SpaceCardEmpty
import com.example.wanotification.ui.theme.SpaceCardNotification
import com.example.wanotification.ui.theme.SpaceCardTts
import com.example.wanotification.ui.theme.SpaceGreen
import com.example.wanotification.ui.theme.SpaceRed
import com.example.wanotification.ui.theme.WaNotificationTheme
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

data class AppOption(val label: String, val packageName: String)

data class ContactEntry(
    val name: String,
    val appLabel: String,
    val appPackage: String
)

data class FilterOption(
    val label: String,
    val appPackage: String?
)

class MainActivity : ComponentActivity() {

    private val appOptions = listOf(
        AppOption("WhatsApp", SupportedApps.WHATSAPP),
        AppOption("Instagram", SupportedApps.INSTAGRAM)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            WaNotificationTheme {
                MainScreen(
                    appOptions = appOptions,
                    onOpenNotificationSettings = {
                        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }
                )
            }
        }
    }
}

@Composable
private fun MainScreen(
    appOptions: List<AppOption>,
    onOpenNotificationSettings: () -> Unit
) {
    val ctx = LocalContext.current
    val appCtx = ctx.applicationContext
    val lifecycleOwner = LocalLifecycleOwner.current

    val selectedTab = rememberSaveable { mutableStateOf(0) }
    val addAppIndex = rememberSaveable { mutableStateOf(0) }
    val filterIndex = rememberSaveable { mutableStateOf(0) }
    val inputText = rememberSaveable { mutableStateOf("") }
    val editInputText = rememberSaveable { mutableStateOf("") }
    val editingEntry = remember { mutableStateOf<ContactEntry?>(null) }

    val waContacts = remember { mutableStateListOf<String>() }
    val igContacts = remember { mutableStateListOf<String>() }

    val ttsEnabled = rememberSaveable { mutableStateOf(TTSSettingsManager.isEnabled(appCtx)) }
    val notificationAccessGranted = rememberSaveable {
        mutableStateOf(isNotificationListenerEnabled(ctx))
    }

    fun refreshContacts(appPackage: String) {
        val target = when (appPackage) {
            SupportedApps.WHATSAPP -> waContacts
            SupportedApps.INSTAGRAM -> igContacts
            else -> null
        } ?: return

        target.clear()
        target.addAll(ContactStore.getAllowedContacts(appCtx, appPackage))
    }

    fun refreshAllContacts() {
        refreshContacts(SupportedApps.WHATSAPP)
        refreshContacts(SupportedApps.INSTAGRAM)
    }

    LaunchedEffect(Unit) {
        refreshAllContacts()
        notificationAccessGranted.value = isNotificationListenerEnabled(ctx)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationAccessGranted.value = isNotificationListenerEnabled(ctx)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val spaceBackground = Brush.verticalGradient(
        colors = listOf(
            SpaceNavy,
            SpaceIndigo,
            SpaceBackgroundMid,
            SpaceBackgroundDeep
        )
    )

    val filterOptions = listOf(
        FilterOption("Semua", null),
        FilterOption("WhatsApp", SupportedApps.WHATSAPP),
        FilterOption("Instagram", SupportedApps.INSTAGRAM)
    )

    val displayedContacts = buildList {
        val selectedFilter = filterOptions[filterIndex.value]
        if (selectedFilter.appPackage == null) {
            val waEntries = waContacts.asReversed().map {
                ContactEntry(it, appOptions[0].label, appOptions[0].packageName)
            }
            val igEntries = igContacts.asReversed().map {
                ContactEntry(it, appOptions[1].label, appOptions[1].packageName)
            }
            addAll(waEntries)
            addAll(igEntries)
        } else if (selectedFilter.appPackage == SupportedApps.WHATSAPP) {
            addAll(waContacts.asReversed().map {
                ContactEntry(it, appOptions[0].label, appOptions[0].packageName)
            })
        } else {
            addAll(igContacts.asReversed().map {
                ContactEntry(it, appOptions[1].label, appOptions[1].packageName)
            })
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(spaceBackground)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                NavigationBar(containerColor = SpaceCardNotification.copy(alpha = 0.9f)) {
                    NavigationBarItem(
                        selected = selectedTab.value == 0,
                        onClick = { selectedTab.value = 0 },
                        icon = { Text("H", color = SpaceText) },
                        label = { Text("Home") }
                    )
                    NavigationBarItem(
                        selected = selectedTab.value == 1,
                        onClick = { selectedTab.value = 1 },
                        icon = { Text("C", color = SpaceText) },
                        label = { Text("Contacts") }
                    )
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
                    .padding(top = 20.dp, bottom = 16.dp)
            ) {
                Text(
                    text = "WaNotification",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 30.sp),
                    color = SpaceText
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (selectedTab.value == 0) {
                    HomeScreen(
                        notificationAccessGranted = notificationAccessGranted.value,
                        ttsEnabled = ttsEnabled.value,
                        onToggleTts = {
                            ttsEnabled.value = it
                            TTSSettingsManager.setEnabled(appCtx, it)
                        },
                        onOpenNotificationSettings = onOpenNotificationSettings
                    )
                } else {
                    ContactsScreen(
                        appOptions = appOptions,
                        addAppIndex = addAppIndex.value,
                        onAddAppChange = { addAppIndex.value = it },
                        inputText = inputText.value,
                        onInputChange = { inputText.value = it },
                        onAddContact = {
                            val appPackage = appOptions[addAppIndex.value].packageName
                            val result = ContactStore.addContact(appCtx, appPackage, inputText.value)
                            when (result) {
                                ContactStore.AddResult.ADDED -> {
                                    inputText.value = ""
                                    Toast.makeText(ctx, "Kontak ditambahkan", Toast.LENGTH_SHORT).show()
                                    refreshContacts(appPackage)
                                }
                                ContactStore.AddResult.DUPLICATE -> Toast.makeText(ctx, "Nama sudah ada", Toast.LENGTH_SHORT).show()
                                ContactStore.AddResult.LIMIT -> Toast.makeText(ctx, "Maksimal 5 kontak per aplikasi", Toast.LENGTH_SHORT).show()
                                ContactStore.AddResult.INVALID -> Toast.makeText(ctx, "Nama tidak valid", Toast.LENGTH_SHORT).show()
                            }
                        },
                        filterOptions = filterOptions,
                        filterIndex = filterIndex.value,
                        onFilterChange = { filterIndex.value = it },
                        contacts = displayedContacts,
                        onRemove = { entry ->
                            ContactStore.removeContact(appCtx, entry.appPackage, entry.name)
                            refreshContacts(entry.appPackage)
                        },
                        onEdit = { entry ->
                            editInputText.value = entry.name
                            editingEntry.value = entry
                        }
                    )
                }
            }
        }
    }

    EditContactDialog(
        entry = editingEntry.value,
        inputText = editInputText.value,
        onInputChange = { editInputText.value = it },
        onDismiss = { editingEntry.value = null },
        onSave = { entry, newName ->
            val result = ContactStore.updateContact(appCtx, entry.appPackage, entry.name, newName)
            when (result) {
                ContactStore.UpdateResult.UPDATED -> {
                    Toast.makeText(ctx, "Kontak diperbarui", Toast.LENGTH_SHORT).show()
                    refreshContacts(entry.appPackage)
                    editingEntry.value = null
                }
                ContactStore.UpdateResult.DUPLICATE -> Toast.makeText(ctx, "Nama sudah ada", Toast.LENGTH_SHORT).show()
                ContactStore.UpdateResult.INVALID -> Toast.makeText(ctx, "Nama tidak valid", Toast.LENGTH_SHORT).show()
                ContactStore.UpdateResult.NOT_FOUND -> {
                    Toast.makeText(ctx, "Kontak tidak ditemukan", Toast.LENGTH_SHORT).show()
                    refreshContacts(entry.appPackage)
                    editingEntry.value = null
                }
            }
        }
    )
}

@Composable
private fun HomeScreen(
    notificationAccessGranted: Boolean,
    ttsEnabled: Boolean,
    onToggleTts: (Boolean) -> Unit,
    onOpenNotificationSettings: () -> Unit
) {
    val statusColor = if (notificationAccessGranted) SpaceGreen else SpaceRed
    val statusLabel = if (notificationAccessGranted) "Active" else "Nonactive"

    Spacer(modifier = Modifier.height(18.dp))

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(190.dp)
                .clip(CircleShape)
                .background(statusColor)
                .clickable { onOpenNotificationSettings() },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = SpaceText
                )
                Text(
                    text = "Notifikasi",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SpaceText.copy(alpha = 0.9f)
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = SpaceCardTts.copy(alpha = 0.95f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .alpha(if (notificationAccessGranted) 1f else 0.5f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.fillMaxWidth(0.72f)) {
                    Text(
                        text = "Aktifkan TTS",
                        style = MaterialTheme.typography.bodyLarge,
                        color = SpaceText,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "TTS hanya bisa diubah saat notifikasi aktif.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SpaceMuted
                    )
                }
                Switch(
                    checked = ttsEnabled,
                    onCheckedChange = onToggleTts,
                    enabled = notificationAccessGranted
                )
            }
        }
    }
}

@Composable
private fun ContactsScreen(
    appOptions: List<AppOption>,
    addAppIndex: Int,
    onAddAppChange: (Int) -> Unit,
    inputText: String,
    onInputChange: (String) -> Unit,
    onAddContact: () -> Unit,
    filterOptions: List<FilterOption>,
    filterIndex: Int,
    onFilterChange: (Int) -> Unit,
    contacts: List<ContactEntry>,
    onRemove: (ContactEntry) -> Unit,
    onEdit: (ContactEntry) -> Unit
) {
    Spacer(modifier = Modifier.height(14.dp))

    Card(
        colors = CardDefaults.cardColors(containerColor = SpaceCardAppSelector.copy(alpha = 0.95f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Pilih Sosmed",
                style = MaterialTheme.typography.labelLarge,
                color = SpaceCyan,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            SosmedDropdown(
                currentLabel = appOptions[addAppIndex].label,
                options = appOptions.map { it.label },
                onSelectIndex = onAddAppChange
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = onInputChange,
                    label = { Text("Nama kontak") },
                    modifier = Modifier.fillMaxWidth(0.65f)
                )

                Spacer(modifier = Modifier.size(10.dp))

                Button(
                    onClick = onAddContact,
                    modifier = Modifier.width(96.dp).height(40.dp)
                ) {
                    Text("Tambah")
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    Card(
        colors = CardDefaults.cardColors(containerColor = SpaceCardNotification.copy(alpha = 0.95f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Filter Kontak",
                style = MaterialTheme.typography.labelLarge,
                color = SpaceCyan,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            SosmedDropdown(
                currentLabel = filterOptions[filterIndex].label,
                options = filterOptions.map { it.label },
                onSelectIndex = onFilterChange
            )
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    ContactListHeader()

    Spacer(modifier = Modifier.height(8.dp))

    if (contacts.isEmpty()) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SpaceCardEmpty.copy(alpha = 0.85f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Belum ada kontak yang diizinkan",
                modifier = Modifier.padding(16.dp),
                color = SpaceMuted
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
        ) {
            itemsIndexed(contacts) { index, entry ->
                ContactRow(
                    index = index + 1,
                    entry = entry,
                    onRemove = { onRemove(entry) },
                    onEdit = { onEdit(entry) }
                )
            }
        }
    }
}

@Composable
private fun SosmedDropdown(
    currentLabel: String,
    options: List<String>,
    onSelectIndex: (Int) -> Unit
) {
    val expanded = rememberSaveable { mutableStateOf(false) }

    Box {
        OutlinedButton(onClick = { expanded.value = true }) {
            Text(currentLabel)
        }

        DropdownMenu(
            expanded = expanded.value,
            onDismissRequest = { expanded.value = false },
            properties = PopupProperties(focusable = true)
        ) {
            options.forEachIndexed { index, label ->
                DropdownMenuItem(
                    text = { Text(label, color = MaterialTheme.colorScheme.onSurface) },
                    onClick = {
                        onSelectIndex(index)
                        expanded.value = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ContactListHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "No",
            color = SpaceMuted,
            modifier = Modifier.width(32.dp)
        )
        Text(
            text = "Nama",
            color = SpaceMuted,
            modifier = Modifier.width(160.dp)
        )
        Text(
            text = "Sosmed",
            color = SpaceMuted,
            modifier = Modifier.width(96.dp)
        )
        Text(
            text = "Aksi",
            color = SpaceMuted,
            modifier = Modifier.width(120.dp)
        )
    }
}

@Composable
private fun ContactRow(
    index: Int,
    entry: ContactEntry,
    onRemove: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SpaceCardContactItem.copy(alpha = 0.96f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(width = 1.dp, color = SpacePurple.copy(alpha = 0.18f), shape = MaterialTheme.shapes.medium)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = index.toString(),
                color = SpaceText,
                modifier = Modifier.width(32.dp)
            )
            Text(
                text = entry.name,
                color = SpaceText,
                modifier = Modifier.width(160.dp)
            )
            Text(
                text = entry.appLabel,
                color = SpaceText,
                modifier = Modifier.width(96.dp)
            )
            Row(
                modifier = Modifier.width(120.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Edit")
                }
                Button(
                    onClick = onRemove,
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Hapus")
                }
            }
        }
    }
}

@Composable
private fun EditContactDialog(
    entry: ContactEntry?,
    inputText: String,
    onInputChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: (ContactEntry, String) -> Unit
) {
    if (entry == null) {
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Kontak") },
        text = {
            Column {
                Text(
                    text = "Sosmed: ${entry.appLabel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = SpaceMuted
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = inputText,
                    onValueChange = onInputChange,
                    label = { Text("Nama kontak") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(entry, inputText) }) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

private fun isNotificationListenerEnabled(context: android.content.Context): Boolean {
    val expected = ComponentName(context, NotificationListener::class.java).flattenToString()
    val enabled = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    return !enabled.isNullOrBlank() && TextUtils.split(enabled, ":").any { it == expected }
}
