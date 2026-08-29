package com.example.wanotification.uiux

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.wanotification.state.ContactEntry
import com.example.wanotification.state.ContactsUiEvent
import com.example.wanotification.state.HomeUiEvent
import com.example.wanotification.state.HomeUiState
import com.example.wanotification.ui.theme.*
import com.example.wanotification.viewmodel.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*

data class FilterOption(
    val label: String,
    val appPackage: String?
)

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

@Composable
private fun MainScreenContainer(appContainer: com.example.wanotification.di.AppContainer) {
    val homeViewModel: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(
            appContainer.checkNotificationAccessUseCase,
            appContainer.enableTTUseCase,
            appContainer.getTtsSettingsUseCase
        )
    )
    val contactsViewModel: ContactsViewModel = viewModel(
        factory = ContactsViewModelFactory(
            appContainer.addContactUseCase,
            appContainer.getContactUseCase,
            appContainer.deleteContactUseCase,
            appContainer.updateContactUseCase
        )
    )

    MainScreen(homeViewModel, contactsViewModel)
}

@Composable
private fun MainScreen(
    homeViewModel: HomeViewModel,
    contactsViewModel: ContactsViewModel
) {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val homeUiState by homeViewModel.uiState.collectAsState()
    val contactsUiState by contactsViewModel.uiState.collectAsState()

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

    LaunchedEffect(Unit) {
        homeViewModel.sideEffects.collect { sideEffect ->
            when (sideEffect) {
                HomeSideEffect.OpenNotificationSettings -> {
                    ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                }
            }
        }
    }

    LaunchedEffect(contactsUiState.selectedApp) {
        contactsViewModel.handleEvent(ContactsUiEvent.LoadContacts)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                homeViewModel.handleEvent(HomeUiEvent.CheckNotificationAccess)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // No-op for unused backgroundBrush

    val displayedContacts = buildList {
        val selectedFilter = filterOptions[filterIndex.value]
        if (selectedFilter.appPackage == null) {
            addAll(contactsUiState.contacts.asReversed())
        } else if (selectedFilter.appPackage == SupportedApps.WHATSAPP) {
            addAll(contactsUiState.contacts.filter { it.appPackage == SupportedApps.WHATSAPP }.asReversed())
        } else {
            addAll(contactsUiState.contacts.filter { it.appPackage == SupportedApps.INSTAGRAM }.asReversed())
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                NavigationBar(containerColor = Color.White.copy(alpha = 0.9f)) {
                    NavigationBarItem(
                        selected = selectedTab.value == 0,
                        onClick = { selectedTab.value = 0 },
                        icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },

                    )
                    NavigationBarItem(
                        selected = selectedTab.value == 1,
                        onClick = { selectedTab.value = 1 },
                        icon = { Icon(Icons.Filled.Contacts, contentDescription = "Contacts") },

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
                    text = "Notification Listener",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 30.sp
                    ),
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    color = AppTextDark
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (selectedTab.value == 0) {
                    when (homeUiState) {
                        is HomeUiState.Loading -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Loading...", color = AppTextDark)
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
                        color = AppTextDark.copy(alpha = 0.6f)
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
            text = "Gunakan untuk kontak spesial anda",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Normal, fontSize = 15.sp),
            color = AppTextDark
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onOpenNotificationSettings,
            modifier = Modifier
                .size(190.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = statusColor,
                contentColor = Color.White
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.fillMaxWidth(0.72f)) {
                Text(
                    text = "Aktifkan TTS",
                    style = MaterialTheme.typography.bodyLarge,
                    color = AppTextDark,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "TTS hanya bisa diubah saat notifikasi aktif.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppTextDark.copy(alpha = 0.6f)
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
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.7f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Pilih Sosmed",
                        style = MaterialTheme.typography.labelLarge,
                        color = AppTextDark,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SosmedDropdown(
                        currentLabel = appOptions[addAppIndex].label,
                        options = appOptions.map { it.label },
                        onSelectIndex = onAddAppChange
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                }
                OutlinedTextField(
                    value = inputText,
                    onValueChange = onInputChange,
                    label = { Text("Nama kontak") },
                    modifier = Modifier.fillMaxWidth(0.65f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.size(10.dp))

                Button(
                    onClick = onAddContact,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AppTextDark)
                ) {
                    Text("Tambah")
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.7f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Filter Kontak",
                style = MaterialTheme.typography.labelLarge,
                color = AppTextDark,
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
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Belum ada kontak yang diizinkan",
                modifier = Modifier.padding(16.dp),
                color = AppTextDark.copy(alpha = 0.6f)
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
            color = AppTextDark.copy(alpha = 0.5f),
            modifier = Modifier.width(32.dp)
        )
        Text(
            text = "Nama",
            color = AppTextDark.copy(alpha = 0.5f),
            modifier = Modifier.width(160.dp)
        )
        Text(
            text = "Sosmed",
            color = AppTextDark.copy(alpha = 0.5f),
            modifier = Modifier.width(96.dp)
        )
        Text(
            text = "Aksi",
            color = AppTextDark.copy(alpha = 0.5f),
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
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.8f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(width = 1.dp, color = AppTextDark.copy(alpha = 0.1f), shape = MaterialTheme.shapes.medium)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = index.toString(),
                color = AppTextDark,
                modifier = Modifier.width(32.dp)
            )
            Text(
                text = entry.name,
                color = AppTextDark,
                modifier = Modifier.width(160.dp)
            )
            Text(
                text = entry.appLabel,
                color = AppTextDark,
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
                    modifier = Modifier.height(32.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SpaceRed)
                ) {
                    Text("Hapus")
                }
            }
        }
    }
}
