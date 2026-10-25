package com.example.savetool

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFC7E3C),
    onPrimary = Color(0xFF1A1A1A),
    primaryContainer = Color(0xFF5C2C0D),
    onPrimaryContainer = Color(0xFFFFDBC8),
    secondary = Color(0xFFFFB74D),
    onSecondary = Color(0xFF1A1A1A),
    secondaryContainer = Color(0xFF5C3D0D),
    onSecondaryContainer = Color(0xFFFFE0B2),
    tertiary = Color(0xFF4FC3F7),
    onTertiary = Color(0xFF003548),
    tertiaryContainer = Color(0xFF004D67),
    onTertiaryContainer = Color(0xFFB8E6FF),
    background = Color(0xFF0F0F12),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1C1B1F),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF2B2930),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99),
    error = Color(0xFFCF6679),
    onError = Color(0xFF3B0A0A)
)

private val BgGradient = Brush.verticalGradient(
    listOf(Color(0xFF0F0F12), Color(0xFF1A1520), Color(0xFF0F0F12))
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = DarkColors) {
                Box(Modifier.fillMaxSize().background(BgGradient)) {
                    SaveEditorApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveEditorApp(vm: SaveViewModel = viewModel()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedCharIndex by remember { mutableStateOf<Int?>(null) }
    var selectMode by remember { mutableStateOf(false) }
    var selectedIndices by remember { mutableStateOf(setOf<Int>()) }
    var showBulkDialog by remember { mutableStateOf(false) }

    val openLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedCharIndex = null
            selectMode = false
            selectedIndices = emptySet()
            vm.load(uri, context)
        }
    }

    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        if (uri != null) {
            val bytes = vm.buildBytes()
            if (bytes != null) {
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                                ?: throw java.io.IOException("Cannot open output")
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                "Saved (${vm.editsCount()} changes)",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    val characters = vm.session?.document?.characters.orEmpty()
    val selectedChar = selectedCharIndex?.let { idx ->
        characters.firstOrNull { it.arrayIndex == idx }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        selectedChar?.name?.ifEmpty { "Unnamed" } ?: "Save Editor",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    if (selectedChar != null) {
                        IconButton(onClick = { selectedCharIndex = null }) {
                            Text("←", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        bottomBar = {
            if (vm.session != null && selectedChar == null) {
                BottomAppBar(
                    containerColor = Color(0xFF1C1B1F).copy(alpha = 0.95f)
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${vm.editsCount()} changes",
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(
                            onClick = { saveLauncher.launch("Save.bytes.patched") },
                            enabled = vm.editsCount() > 0
                        ) { Text("Save") }
                    }
                }
            }
        }
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when {
                vm.isLoading -> LoadingState()
                vm.error != null -> ErrorState(
                    message = vm.error!!,
                    onRetry = { openLauncher.launch(arrayOf("*/*")) }
                )
                vm.session == null -> EmptyState(
                    onOpen = { openLauncher.launch(arrayOf("*/*")) }
                )
                selectedChar != null -> CharacterDetailScreen(
                    vm = vm,
                    char = selectedChar
                )
                else -> CharacterListScreen(
                    vm = vm,
                    characters = characters,
                    selectMode = selectMode,
                    selected = selectedIndices,
                    onToggleSelectMode = {
                        selectMode = !selectMode
                        if (!selectMode) selectedIndices = emptySet()
                    },
                    onToggleSelection = { idx ->
                        selectedIndices = if (idx in selectedIndices)
                            selectedIndices - idx
                        else selectedIndices + idx
                    },
                    onOpenDetail = { selectedCharIndex = it.arrayIndex },
                    onChangeSelectedClick = { showBulkDialog = true }
                )
            }
        }
    }

    if (showBulkDialog) {
        val selectedChars = characters.filter { it.arrayIndex in selectedIndices }
        if (selectedChars.isNotEmpty()) {
            ChangeSelectedDialog(
                firstChar = selectedChars.first(),
                selectedCount = selectedChars.size,
                onDismiss = { showBulkDialog = false },
                onApply = { change ->
                    vm.applyBulkChange(selectedChars, change)
                    showBulkDialog = false
                    selectMode = false
                    selectedIndices = emptySet()
                }
            )
        } else {
            showBulkDialog = false
        }
    }
}

@Composable
fun LoadingState() {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text("Loading file...")
    }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Error", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(8.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) { Text("Choose another file") }
    }
}

@Composable
fun EmptyState(onOpen: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Save Editor", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Select a Save.bytes file to get started",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onOpen) { Text("📂  Select File") }
    }
}

@Composable
fun CharacterListScreen(
    vm: SaveViewModel,
    characters: List<Character>,
    selectMode: Boolean,
    selected: Set<Int>,
    onToggleSelectMode: () -> Unit,
    onToggleSelection: (Int) -> Unit,
    onOpenDetail: (Character) -> Unit,
    onChangeSelectedClick: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(characters, searchQuery) {
        if (searchQuery.isBlank()) characters
        else characters.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            label = { Text("Search by name") },
            singleLine = true
        )

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!selectMode) {
                Button(
                    onClick = onToggleSelectMode,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Select Characters")
                }
            } else {
                OutlinedButton(
                    onClick = onToggleSelectMode,
                    modifier = Modifier.weight(1f)
                ) { Text("Cancel") }
                Button(
                    onClick = onChangeSelectedClick,
                    modifier = Modifier.weight(1f),
                    enabled = selected.isNotEmpty()
                ) {
                    Text("Change ${selected.size}")
                }
            }
        }

        Text(
            if (selectMode) "${selected.size} selected of ${filtered.size}"
            else "${filtered.size} of ${characters.size} characters",
            Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(if (characters.isEmpty()) "No characters found" else "No results for this search")
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(filtered) { char ->
                    val isSelected = char.arrayIndex in selected
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .clickable {
                                if (selectMode) onToggleSelection(char.arrayIndex)
                                else onOpenDetail(char)
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                        )
                    ) {
                        Row(
                            Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (selectMode) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = null
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                            Column(Modifier.weight(1f)) {
                                Text(
                                    char.name.ifEmpty { "(unnamed)" },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "${char.stats.size} stats · #${char.arrayIndex}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CharacterDetailScreen(vm: SaveViewModel, char: Character) {
    Column(Modifier.fillMaxSize()) {
        Button(
            onClick = { vm.setCharacterStats(char, 1000f) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text("Set all stats to 1000")
        }
        LazyColumn(Modifier.fillMaxSize()) {
            if (char.extraFields.isNotEmpty()) {
                item {
                    Text(
                        "Profile",
                        Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(char.extraFields) { f ->
                    EditableRow(vm, f, label = f.name)
                }
            }
            if (char.stats.isNotEmpty()) {
                item {
                    Text(
                        "Stats (${char.stats.size})",
                        Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(char.stats) { slot ->
                    EditableRow(vm, slot, label = "stat[${slot.index}]")
                }
            }
        }
    }
}

@Composable
fun EditableRow(vm: SaveViewModel, e: Editable, label: String) {
    var dialogOpen by remember { mutableStateOf(false) }
    val currentValue = vm.currentValueOf(e)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
        )
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                formatValue(currentValue, e.type),
                Modifier.width(100.dp),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.secondary
            )
            TextButton(onClick = { dialogOpen = true }) { Text("Edit") }
        }
    }
    if (dialogOpen) {
        EditStatDialog(
            currentValue = currentValue,
            type = e.type,
            label = label,
            onDismiss = { dialogOpen = false },
            onConfirm = { v ->
                vm.setValue(e, v)
                dialogOpen = false
            }
        )
    }
}

@Composable
fun EditStatDialog(
    currentValue: Float,
    type: PrimTypeEnum,
    label: String,
    onDismiss: () -> Unit,
    onConfirm: (Float) -> Unit
) {
    var text by remember { mutableStateOf(formatValue(currentValue, type)) }
    val parsed = text.replace(',', '.').toFloatOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit $label") },
        text = {
            Column {
                Text(
                    "Current: ${formatValue(currentValue, type)}",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = parsed == null,
                    singleLine = true,
                    label = { Text("New value") }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { parsed?.let(onConfirm) },
                enabled = parsed != null
            ) { Text("Apply") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ChangeSelectedDialog(
    firstChar: Character,
    selectedCount: Int,
    onDismiss: () -> Unit,
    onApply: (BulkChange) -> Unit
) {
    var statsText by remember { mutableStateOf("") }
    var ageText by remember { mutableStateOf("") }
    var heightText by remember { mutableStateOf("") }
    var armMassText by remember { mutableStateOf("") }
    var muscleMassText by remember { mutableStateOf("") }
    var bodyMassText by remember { mutableStateOf("") }
    var legMassText by remember { mutableStateOf("") }

    val extras = mutableMapOf<String, Float>()
    ageText.replace(',', '.').toFloatOrNull()?.let { extras["age"] = it }
    heightText.replace(',', '.').toFloatOrNull()?.let { extras["height"] = it }
    armMassText.replace(',', '.').toFloatOrNull()?.let { extras["armMass"] = it }
    muscleMassText.replace(',', '.').toFloatOrNull()?.let { extras["muscleMass"] = it }
    bodyMassText.replace(',', '.').toFloatOrNull()?.let { extras["bodyMass"] = it }
    legMassText.replace(',', '.').toFloatOrNull()?.let { extras["legMass"] = it }
    val statsValue = statsText.replace(',', '.').toFloatOrNull()
    val hasAny = statsValue != null || extras.isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Change $selectedCount Character${if (selectedCount != 1) "s" else ""}")
        },
        text = {
            Column(
                Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    "Leave a field empty to skip it. Filled values are applied to all selected characters.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                BulkField("All stats", statsText, { statsText = it }, hintFor(firstChar, "stat"))
                BulkField("Age", ageText, { ageText = it }, hintFor(firstChar, "age"))
                BulkField("Height", heightText, { heightText = it }, hintFor(firstChar, "height"))
                BulkField("Arm Mass", armMassText, { armMassText = it }, hintFor(firstChar, "armMass"))
                BulkField("Muscle Mass", muscleMassText, { muscleMassText = it }, hintFor(firstChar, "muscleMass"))
                BulkField("Body Mass", bodyMassText, { bodyMassText = it }, hintFor(firstChar, "bodyMass"))
                BulkField("Leg Mass", legMassText, { legMassText = it }, hintFor(firstChar, "legMass"))
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onApply(BulkChange(statsValue, extras)) },
                enabled = hasAny
            ) { Text("Apply") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun BulkField(label: String, value: String, onChange: (String) -> Unit, currentHint: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        placeholder = {
            if (currentHint.isNotEmpty()) Text("current: $currentHint")
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true
    )
}

fun hintFor(char: Character, name: String): String = when (name) {
    "stat" -> char.stats.firstOrNull()?.let { formatValue(it.value, it.type) } ?: ""
    else -> char.extraFields.firstOrNull { it.name == name }
        ?.let { formatValue(it.value, it.type) } ?: ""
}
