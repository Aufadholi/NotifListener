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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.PopupProperties
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wanotification.listener.NotificationListener
import com.example.wanotification.config.SupportedApps
import com.example.wanotification.config.TTSSettingsManager
import com.example.wanotification.filter.ContactStore
import com.example.wanotification.ui.theme.SpaceCyan
import com.example.wanotification.ui.theme.SpaceIndigo
import com.example.wanotification.ui.theme.SpaceMuted
import com.example.wanotification.ui.theme.SpaceNavy
import com.example.wanotification.ui.theme.SpacePurple
import com.example.wanotification.ui.theme.SpaceRose
import com.example.wanotification.ui.theme.SpaceText
import com.example.wanotification.ui.theme.WaNotificationTheme

data class AppOption(val label: String, val packageName: String)

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

    val selectedIndex = rememberSaveable { mutableStateOf(0) }
    val inputText = rememberSaveable { mutableStateOf("") }
    val contacts = remember { mutableStateListOf<String>() }
    val ttsEnabled = rememberSaveable { mutableStateOf(TTSSettingsManager.isEnabled(ctx)) }
    val dropdownExpanded = rememberSaveable { mutableStateOf(false) }
    val notificationAccessGranted = rememberSaveable {
        mutableStateOf(isNotificationListenerEnabled(ctx))
    }

    // load initial list when selected app changes
    LaunchedEffect(selectedIndex.value) {
        contacts.clear()
        contacts.addAll(ContactStore.getAllowedContacts(ctx, appOptions[selectedIndex.value].packageName))
        notificationAccessGranted.value = isNotificationListenerEnabled(ctx)
    }

    val spaceBackground = Brush.verticalGradient(
        colors = listOf(
            SpaceNavy,
            SpaceIndigo,
            Color(0xFF111831),
            Color(0xFF0A0F1C)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(spaceBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(top = 28.dp, bottom = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "WaNotification",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 32.sp),
                color = SpaceText
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Modern space theme untuk mengatur notifikasi, kontak, dan TTS dengan lebih rapi.",
                style = MaterialTheme.typography.bodyMedium,
                color = SpaceMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(18.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121B35).copy(alpha = 0.96f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = if (notificationAccessGranted.value) "Akses notifikasi aktif" else "Akses notifikasi belum diaktifkan",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (notificationAccessGranted.value) SpaceCyan else SpaceRose,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Aktifkan akses notifikasi agar listener bisa membaca pesan masuk dari sistem.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SpaceText.copy(alpha = 0.9f)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = {
                            onOpenNotificationSettings()
                            notificationAccessGranted.value = isNotificationListenerEnabled(ctx)
                        }) {
                            Text("Buka Akses Notifikasi")
                        }
                        OutlinedButton(onClick = {
                            notificationAccessGranted.value = isNotificationListenerEnabled(ctx)
                        }) {
                            Text("Cek Ulang")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF162242).copy(alpha = 0.95f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Pilih Aplikasi",
                            style = MaterialTheme.typography.labelLarge,
                            color = SpaceCyan,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = appOptions[selectedIndex.value].label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = SpaceText
                        )
                    }

                    Box {
                        Button(onClick = { dropdownExpanded.value = true }) {
                            Text("Ubah")
                        }

                        DropdownMenu(
                            expanded = dropdownExpanded.value,
                            onDismissRequest = { dropdownExpanded.value = false },
                            properties = PopupProperties(focusable = true)
                        ) {
                            appOptions.forEachIndexed { idx, opt ->
                                DropdownMenuItem(
                                    text = {
                                        Text(opt.label, color = MaterialTheme.colorScheme.onSurface)
                                    },
                                    onClick = {
                                        selectedIndex.value = idx
                                        dropdownExpanded.value = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2443).copy(alpha = 0.95f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.fillMaxWidth(0.72f)) {
                        val label = if (ttsEnabled.value) "Nonaktifkan TTS Pesan" else "Aktifkan TTS Pesan"
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = SpaceText,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Saat aktif, isi pesan akan ikut dibacakan. Saat nonaktif, hanya frasa pembuka yang dibaca.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SpaceMuted
                        )
                    }
                    Switch(
                        checked = ttsEnabled.value,
                        onCheckedChange = {
                            ttsEnabled.value = it
                            TTSSettingsManager.setEnabled(ctx, it)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131C33).copy(alpha = 0.95f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Tambah Kontak",
                        style = MaterialTheme.typography.labelLarge,
                        color = SpaceRose,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = inputText.value,
                            onValueChange = { inputText.value = it },
                            label = { Text("Nama kontak") },
                            modifier = Modifier.fillMaxWidth(0.70f)
                        )

                        Spacer(modifier = Modifier.size(10.dp))

                        Button(onClick = {
                            val result = ContactStore.addContact(ctx, appOptions[selectedIndex.value].packageName, inputText.value)
                            when (result) {
                                ContactStore.AddResult.ADDED -> {
                                    inputText.value = ""
                                    Toast.makeText(ctx, "Kontak ditambahkan", Toast.LENGTH_SHORT).show()
                                }
                                ContactStore.AddResult.DUPLICATE -> Toast.makeText(ctx, "Nama sudah ada", Toast.LENGTH_SHORT).show()
                                ContactStore.AddResult.LIMIT -> Toast.makeText(ctx, "Maksimal 5 kontak per aplikasi", Toast.LENGTH_SHORT).show()
                                ContactStore.AddResult.INVALID -> Toast.makeText(ctx, "Nama tidak valid", Toast.LENGTH_SHORT).show()
                            }
                            contacts.clear()
                            contacts.addAll(ContactStore.getAllowedContacts(ctx, appOptions[selectedIndex.value].packageName))
                        }) {
                            Text("Tambah")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Kontak diizinkan: ${contacts.size}/${ContactStore.maxContacts()}",
                style = MaterialTheme.typography.labelLarge,
                color = SpaceText
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (contacts.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF182242).copy(alpha = 0.85f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Belum ada kontak yang diizinkan",
                        modifier = Modifier.padding(16.dp),
                        color = SpaceMuted
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(contacts) { name ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF182A4A).copy(alpha = 0.96f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .border(width = 1.dp, color = SpacePurple.copy(alpha = 0.18f), shape = MaterialTheme.shapes.medium)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = name,
                                    color = SpaceText,
                                    modifier = Modifier.fillMaxWidth(0.72f)
                                )
                                Button(onClick = {
                                    ContactStore.removeContact(ctx, appOptions[selectedIndex.value].packageName, name)
                                    contacts.clear()
                                    contacts.addAll(ContactStore.getAllowedContacts(ctx, appOptions[selectedIndex.value].packageName))
                                }) {
                                    Text("Hapus")
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

private fun isNotificationListenerEnabled(context: android.content.Context): Boolean {
    val expected = ComponentName(context, NotificationListener::class.java).flattenToString()
    val enabled = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    return !enabled.isNullOrBlank() && TextUtils.split(enabled, ":").any { it == expected }
}
