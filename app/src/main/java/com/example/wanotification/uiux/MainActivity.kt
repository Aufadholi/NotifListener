package com.example.wanotification.uiux

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wanotification.config.SupportedApps
import com.example.wanotification.di.DefaultAppContainer
import com.example.wanotification.filter.ContactStore
import com.example.wanotification.listener.NotificationListener
import com.example.wanotification.state.ContactEntry
import com.example.wanotification.state.ContactsUiEvent
import com.example.wanotification.state.HomeUiEvent
import com.example.wanotification.state.HomeUiState
import com.example.wanotification.ui.theme.SpaceBackgroundDeep
import com.example.wanotification.ui.theme.SpaceBackgroundMid
import com.example.wanotification.ui.theme.SpaceCardAppSelector
import com.example.wanotification.ui.theme.SpaceCardContactItem
import com.example.wanotification.ui.theme.SpaceCardEmpty
import com.example.wanotification.ui.theme.SpaceCardNotification
import com.example.wanotification.ui.theme.SpaceCyan
import com.example.wanotification.ui.theme.SpaceGreen
import com.example.wanotification.ui.theme.SpaceIndigo
import com.example.wanotification.ui.theme.SpaceMuted
import com.example.wanotification.ui.theme.SpaceNavy
import com.example.wanotification.ui.theme.SpacePurple
import com.example.wanotification.ui.theme.SpaceRed
import com.example.wanotification.ui.theme.SpaceText
import com.example.wanotification.ui.theme.WaNotificationTheme
import com.example.wanotification.viewmodel.AppOption
import com.example.wanotification.viewmodel.ContactsViewModel
import com.example.wanotification.viewmodel.ContactsViewModelFactory
import com.example.wanotification.viewmodel.HomeViewModel
import com.example.wanotification.viewmodel.HomeViewModelFactory
import com.example.wanotification.viewmodel.HomeSideEffect

/**
 * FilterOption for contact filter dropdown
 */
data class FilterOption(
    val label: String,
    val appPackage: String?
)

/**
 * MainActivity - Entry point with MVVM architecture
 *
 * ✅ PRESERVED: All original UI functionality
 * ✅ ADDED: MVVM layer for state management
 * ✅ SAFE: No business logic changes to services/managers
 */
class MainActivity : ComponentActivity() {

    private lateinit var appContainer: DefaultAppContainer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appContainer = DefaultAppContainer(this)

        setContent {
            WaNotificationTheme {
                MainScreenContainer(appContainer)
            }
        }
    }
}

/**
 * Container that sets up ViewModels with DI
 */
@Composable
private fun MainScreenContainer(appContainer: com.example.wanotification.di.AppContainer) {
    val homeViewModel: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(appContainer.settingsRepository)
    )
    val contactsViewModel: ContactsViewModel = viewModel(
        factory = ContactsViewModelFactory(appContainer.contactRepository)
    )

    MainScreen(homeViewModel, contactsViewModel)
}

/**
 * Main screen with tabs (Home + Contacts)
 *
 * UDF Pattern:
 * - Observes state from ViewModels
 * - Sends events to ViewModels
 * - No local mutable state except UI transients (input, dialog)
 */
@Composable
private fun MainScreen(
    homeViewModel: HomeViewModel,
    contactsViewModel: ContactsViewModel
) {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Observe state from ViewModels
    val homeUiState by homeViewModel.uiState.collectAsState()
    val contactsUiState by contactsViewModel.uiState.collectAsState()

    // UI-only transient state (input fields, dialogs)
    val selectedTab = rememberSaveable { mutableStateOf(0) }
    val addAppIndex = rememberSaveable { mutableStateOf(0) }
    val filterIndex = rememberSaveable { mutableStateOf(0) }
    val inputText = rememberSaveable { mutableStateOf("") }
    val editInputText = rememberSaveable { mutableStateOf("") }
    val editingEntry = remember { mutableStateOf<ContactEntry?>(null) }

    val appOptions = listOf(
        AppOption("WhatsApp", SupportedApps.WHATSAPP),
        AppOption("Instagram", SupportedApps.INSTAGRAM)
    )

    val filterOptions = listOf(
        FilterOption("Semua", null),
        FilterOption("WhatsApp", SupportedApps.WHATSAPP),
        FilterOption("Instagram", SupportedApps.INSTAGRAM)
    )

    // Handle side effects (one-time events from ViewModel)
    LaunchedEffect(Unit) {
        homeViewModel.sideEffects.collect { sideEffect ->
            when (sideEffect) {
                HomeSideEffect.OpenNotificationSettings -> {
                    ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                }
            }
        }
    }

    // Refresh contacts when they change
    LaunchedEffect(contactsUiState.selectedApp) {
        contactsViewModel.handleEvent(ContactsUiEvent.LoadContacts)
    }

    // Refresh notification access status when resumed
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                homeViewModel.handleEvent(HomeUiEvent.CheckNotificationAccess)
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

    // Filter contacts for display
    val displayedContacts = buildList {
        val selectedFilter = filterOptions[filterIndex.value]
        if (selectedFilter.appPackage == null) {
            // Show all contacts from all apps
            addAll(contactsUiState.contacts.asReversed())
        } else if (selectedFilter.appPackage == SupportedApps.WHATSAPP) {
            // Show only WhatsApp
            addAll(contactsUiState.contacts.filter { it.appPackage == SupportedApps.WHATSAPP }.asReversed())
        } else {
            // Show only Instagram
            addAll(contactsUiState.contacts.filter { it.appPackage == SupportedApps.INSTAGRAM }.asReversed())
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
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 30.sp
                    ),
                    color = SpaceText
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (selectedTab.value == 0) {
                    // Home Screen
                    when (homeUiState) {
                        is HomeUiState.Loading -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Loading...", color = SpaceText)
                            }
                        }
                        is HomeUiState.Success -> {
                            val successState = homeUiState as HomeUiState.Success
                            HomeScreen(
                                notificationAccessGranted = successState.notificationAccessGranted,
                                ttsEnabled = successState.ttsEnabled,
                                onToggleTts = { enabled ->
                                    homeViewModel.handleEvent(HomeUiEvent.ToggleTts(enabled))
                                },
                                onOpenNotificationSettings = {
                                    homeViewModel.handleEvent(HomeUiEvent.OpenNotificationSettings)
                                }
                            )
                        }
                        is HomeUiState.Error -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Error: ${(homeUiState as HomeUiState.Error).message}", color = SpaceRed)
                            }
                        }
                    }
                } else {
                    // Contacts Screen
                    ContactsScreen(
                        appOptions = appOptions,
                        addAppIndex = addAppIndex.value,
                        onAddAppChange = { addAppIndex.value = it },
                        inputText = inputText.value,
                        onInputChange = { inputText.value = it },
                        onAddContact = {
                            val appPackage = appOptions[addAppIndex.value].packageName
                            contactsViewModel.handleEvent(
                                ContactsUiEvent.AddContact(appPackage, inputText.value)
                            )
                            if (contactsUiState.successMessage != null) {
                                Toast.makeText(ctx, contactsUiState.successMessage, Toast.LENGTH_SHORT).show()
                            }
                        },
                        filterOptions = filterOptions,
                        filterIndex = filterIndex.value,
                        onFilterChange = { filterIndex.value = it },
                        contacts = displayedContacts,
                        onRemove = { entry ->
                            contactsViewModel.handleEvent(
                                ContactsUiEvent.DeleteContact(entry.appPackage, entry.name)
                            )
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

    // Edit dialog
    if (editingEntry.value != null) {
        val entry = editingEntry.value!!
        AlertDialog(
            onDismissRequest = { editingEntry.value = null },
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
                        value = editInputText.value,
                        onValueChange = { editInputText.value = it },
                        label = { Text("Nama kontak") }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        contactsViewModel.handleEvent(
                            ContactsUiEvent.UpdateContact(entry.appPackage, entry.name, editInputText.value)
                        )
                        editingEntry.value = null
                    }
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingEntry.value = null }) {
                    Text("Batal")
                }
            }
        )
    }
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
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "NotificationListener",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 30.sp),
            color = SpaceText
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onOpenNotificationSettings,
            modifier = Modifier
                .size(190.dp)
                .clip(CircleShape)
                .background(statusColor)
                .clickable { onOpenNotificationSettings() }
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
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
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
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

/**
 * REPOSITORY LAYER - Consolidated into MainActivity
 */
interface IContactRepository {
    suspend fun getAllContacts(app: String): List<String>
    suspend fun addContact(app: String, name: String): AddResult
    suspend fun deleteContact(app: String, name: String)
    suspend fun updateContact(app: String, oldName: String, newName: String): UpdateResult
}

class ContactRepository(private val context: android.content.Context) : IContactRepository {
    override suspend fun getAllContacts(app: String): List<String> =
        withContext(Dispatchers.IO) {
            ContactStore.getAllowedContacts(context, app)
        }

    override suspend fun addContact(app: String, name: String): AddResult =
        withContext(Dispatchers.IO) {
            when (ContactStore.addContact(context, app, name)) {
                ContactStore.AddResult.ADDED -> AddResult.ADDED
                ContactStore.AddResult.DUPLICATE -> AddResult.DUPLICATE
                ContactStore.AddResult.LIMIT -> AddResult.LIMIT
                ContactStore.AddResult.INVALID -> AddResult.INVALID
            }
        }

    override suspend fun deleteContact(app: String, name: String) {
        withContext(Dispatchers.IO) {
            ContactStore.removeContact(context, app, name)
        }
    }

    override suspend fun updateContact(app: String, oldName: String, newName: String): UpdateResult =
        withContext(Dispatchers.IO) {
            when (ContactStore.updateContact(context, app, oldName, newName)) {
                ContactStore.UpdateResult.UPDATED -> UpdateResult.UPDATED
                ContactStore.UpdateResult.DUPLICATE -> UpdateResult.DUPLICATE
                ContactStore.UpdateResult.INVALID -> UpdateResult.INVALID
                ContactStore.UpdateResult.NOT_FOUND -> UpdateResult.NOT_FOUND
            }
        }
}

enum class AddResult { ADDED, DUPLICATE, LIMIT, INVALID }
enum class UpdateResult { UPDATED, DUPLICATE, INVALID, NOT_FOUND }
