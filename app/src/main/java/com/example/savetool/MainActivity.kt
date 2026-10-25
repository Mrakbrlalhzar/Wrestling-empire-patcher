package com.example.savetool
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SaveEditorTheme { SaveEditorApp() } }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveEditorApp(vm: SaveViewModel = viewModel()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedCharIndex by remember { mutableStateOf<Int?>(null) }
    var showAbout by remember { mutableStateOf(false) }
    val openLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) { selectedCharIndex = null; vm.load(uri, context) }
    }
    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri != null) {
            val bytes = vm.buildBytes()
            if (bytes != null) {
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) } ?: throw java.io.IOException("open fail")
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "✓ ذخیره شد (${vm.editsCount()} تغییر)", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) { Toast.makeText(context, "خطا: ${e.message}", Toast.LENGTH_LONG).show() }
                    }
                }
            }
        }
    }
    val characters = vm.session?.document?.characters.orEmpty()
    val selectedChar = selectedCharIndex?.let { idx -> characters.firstOrNull { it.arrayIndex == idx } }
    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface, titleContentColor = MaterialTheme.colorScheme.primary),
                title = { Text(selectedChar?.name?.ifEmpty { "بدون نام" } ?: "Save Editor", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (selectedChar != null) IconButton(onClick = { selectedCharIndex = null }) { Text("←", style = MaterialTheme.typography.headlineSmall) }
                },
                actions = { IconButton(onClick = { showAbout = true }) { Text("ℹ️", style = MaterialTheme.typography.titleLarge) } }
            )
        },
        bottomBar = {
            if (vm.session != null) {
                BottomAppBar(containerColor = MaterialTheme.colorScheme.surface) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${vm.editsCount()} تغییر", Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(onClick = { saveLauncher.launch("Save.bytes.patched") }, enabled = vm.editsCount() > 0, shape = RoundedCornerShape(12.dp)) { Text("💾 ذخیره") }
                    }
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                vm.isLoading -> Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(); Spacer(Modifier.height(16.dp)); Text("در حال خواندن فایل...")
                }
                vm.error != null -> Column(Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("خطا", style = MaterialTheme.typography.titleLarge); Spacer(Modifier.height(8.dp))
                    Text(vm.error!!, style = MaterialTheme.typography.bodyMedium); Spacer(Modifier.height(16.dp))
                    Button(onClick = { openLauncher.launch(arrayOf("*/*")) }) { Text("انتخاب فایل دیگر") }
                }
                vm.session == null -> Column(Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("❤", style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(16.dp)); Text("فایل Save.bytes را انتخاب کنید", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(20.dp))
                    Button(onClick = { openLauncher.launch(arrayOf("*/*")) }, shape = RoundedCornerShape(12.dp)) { Text("📂 انتخاب فایل") }
                }
                selectedChar != null -> CharacterDetailScreen(vm = vm, char = selectedChar)
                else -> CharacterListScreen(vm = vm, characters = characters, onSelect = { selectedCharIndex = it.arrayIndex })
            }
        }
    }
    if (showAbout) AboutDialog(onDismiss = { showAbout = false })
}
@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Text("❤", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary) },
        title = { Text("درباره") },
        text = {
            Column {
                Text("It's made with ♥ in Iran", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(20.dp))
                Text("سازنده", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Text("Mrakbrlalhzar",
                    modifier = Modifier.clickable {
                        try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Mrakbrlalhzar"))) } catch (_: Exception) {}
                    },
                    style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("github.com/Mrakbrlalhzar", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("بستن") } }
    )
}
@Composable
fun CharacterListScreen(vm: SaveViewModel, characters: List<Character>, onSelect: (Character) -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    var showBulkDialog by remember { mutableStateOf(false) }
    val filtered = remember(characters, searchQuery) {
        if (searchQuery.isBlank()) characters else characters.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }
    val visibleIndices = remember(filtered) { filtered.map { it.arrayIndex }.toSet() }
    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            label = { Text("🔍 جستجوی اسم") }, singleLine = true, shape = RoundedCornerShape(14.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilledTonalButton(onClick = { vm.selectAllVisible(visibleIndices) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                Text("✓ همه", style = MaterialTheme.typography.bodyMedium)
            }
            FilledTonalButton(onClick = { vm.invertVisible(visibleIndices) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                Text("⇄ برعکس", style = MaterialTheme.typography.bodyMedium)
            }
            if (vm.selectedIndices.isNotEmpty()) {
                FilledTonalButton(onClick = { vm.clearSelection() }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError)) {
                    Text("✕ ${vm.selectedIndices.size}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Column(Modifier.padding(horizontal = 12.dp)) {
            Button(onClick = { vm.setAllToEveryone(-1000f) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError)) {
                Text("⚡ همه‌ی استت‌ها → -1000.0", fontWeight = FontWeight.Bold)
            }
            if (vm.selectedIndices.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Button(onClick = { showBulkDialog = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Text("⚡ ${vm.selectedIndices.size} انتخاب‌شده → ویرایش گروهی", fontWeight = FontWeight.Bold)
                }
            }
        }
        Text("${filtered.size} از ${characters.size} شخصیت", Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(if (characters.isEmpty()) "شخصیتی یافت نشد" else "نتیجه‌ای برای جستجو نیست", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(filtered, key = { it.arrayIndex }) { char ->
                    CharacterCard(char = char, isSelected = char.arrayIndex in vm.selectedIndices,
                        onToggleSelect = { vm.toggleSelection(char.arrayIndex) }, onClick = { onSelect(char) })
                }
            }
        }
    }
    if (showBulkDialog) {
        BulkEditDialog(count = vm.selectedIndices.size, onDismiss = { showBulkDialog = false },
            onConfirm = { change -> vm.applyBulkChangeToSelected(change); showBulkDialog = false })
    }
}
@Composable
fun CharacterCard(char: Character, isSelected: Boolean, onToggleSelect: () -> Unit, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = isSelected, onCheckedChange = { onToggleSelect() })
            Column(Modifier.weight(1f).clickable { onClick() }.padding(vertical = 8.dp, horizontal = 6.dp)) {
                Text(char.name.ifEmpty { "(بدون نام)" }, style = MaterialTheme.typography.titleMedium,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Text("${char.stats.size} استت — #${char.arrayIndex}", style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("›", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
@Composable
fun BulkEditDialog(count: Int, onDismiss: () -> Unit, onConfirm: (BulkChange) -> Unit) {
    var statsText by remember { mutableStateOf("-1000.0") }
    var applyStats by remember { mutableStateOf(true) }
    var ageText by remember { mutableStateOf("") }
    var heightText by remember { mutableStateOf("") }
    var armMassText by remember { mutableStateOf("") }
    var muscleMassText by remember { mutableStateOf("") }
    var bodyMassText by remember { mutableStateOf("") }
    var legMassText by remember { mutableStateOf("") }
    val quickValues = listOf("-1000", "0", "1000", "10000", "99999")
    AlertDialog(onDismissRequest = onDismiss, title = { Text("ویرایش گروهی $count شخصیت") },
        text = {
            Column(Modifier.height(420.dp).verticalScroll(rememberScrollState())) {
                Text("استت‌ها:", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = applyStats, onCheckedChange = { applyStats = it })
                    Text("اعمال روی statها", style = MaterialTheme.typography.bodyMedium)
                }
                if (applyStats) {
                    OutlinedTextField(value = statsText, onValueChange = { statsText = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true,
                        shape = RoundedCornerShape(12.dp), label = { Text("مقدار stat") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        quickValues.take(3).forEach { q -> FilledTonalButton(onClick = { statsText = q }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) { Text(q, style = MaterialTheme.typography.bodySmall) } }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        quickValues.drop(3).forEach { q -> FilledTonalButton(onClick = { statsText = q }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) { Text(q, style = MaterialTheme.typography.bodySmall) } }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("فیلدهای اضافی (خالی = بدون تغییر):", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(value = ageText, onValueChange = { ageText = it }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, shape = RoundedCornerShape(12.dp), label = { Text("age") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(value = heightText, onValueChange = { heightText = it }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, shape = RoundedCornerShape(12.dp), label = { Text("height") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(value = armMassText, onValueChange = { armMassText = it }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, shape = RoundedCornerShape(12.dp), label = { Text("armMass") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(value = muscleMassText, onValueChange = { muscleMassText = it }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, shape = RoundedCornerShape(12.dp), label = { Text("muscleMass") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(value = bodyMassText, onValueChange = { bodyMassText = it }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, shape = RoundedCornerShape(12.dp), label = { Text("bodyMass") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(value = legMassText, onValueChange = { legMassText = it }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, shape = RoundedCornerShape(12.dp), label = { Text("legMass") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val statsVal = if (applyStats) statsText.replace(',', '.').toFloatOrNull() else null
                val extras = mutableMapOf<String, Float>()
                ageText.replace(',', '.').toFloatOrNull()?.let { extras["age"] = it }
                heightText.replace(',', '.').toFloatOrNull()?.let { extras["height"] = it }
                armMassText.replace(',', '.').toFloatOrNull()?.let { extras["armMass"] = it }
                muscleMassText.replace(',', '.').toFloatOrNull()?.let { extras["muscleMass"] = it }
                bodyMassText.replace(',', '.').toFloatOrNull()?.let { extras["bodyMass"] = it }
                legMassText.replace(',', '.').toFloatOrNull()?.let { extras["legMass"] = it }
                onConfirm(BulkChange(statsVal, extras))
            }) { Text("اعمال") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } })
}
@Composable
fun CharacterDetailScreen(vm: SaveViewModel, char: Character) {
    Column(Modifier.fillMaxSize()) {
        Button(onClick = { vm.setCharacterStats(char, 1000f) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp), shape = RoundedCornerShape(12.dp)) {
            Text("⚡ همه‌ی استت‌های این شخصیت → 1000.0", fontWeight = FontWeight.Bold)
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
            if (char.extraFields.isNotEmpty()) {
                item { Text("📊 مشخصات", Modifier.padding(horizontal = 12.dp, vertical = 8.dp), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }
                items(char.extraFields) { f -> EditableRow(vm, f, label = f.name) }
            }
            if (char.stats.isNotEmpty()) {
                item { Text("💪 استت‌ها (${char.stats.size})", Modifier.padding(horizontal = 12.dp, vertical = 8.dp), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }
                items(char.stats) { slot -> EditableRow(vm, slot, label = "stat[${slot.index}]") }
            }
        }
    }
}
@Composable
fun EditableRow(vm: SaveViewModel, e: Editable, label: String) {
    var dialogOpen by remember { mutableStateOf(false) }
    val currentValue = vm.currentValueOf(e)
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 3.dp), shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(formatValue(currentValue, e.type), Modifier.width(90.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
            TextButton(onClick = { dialogOpen = true }, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) { Text("ویرایش") }
        }
    }
    if (dialogOpen) {
        EditStatDialog(currentValue = currentValue, type = e.type, label = label,
            onDismiss = { dialogOpen = false }, onConfirm = { v -> vm.setValue(e, v); dialogOpen = false })
    }
}
@Composable
fun EditStatDialog(currentValue: Float, type: PrimTypeEnum, label: String, onDismiss: () -> Unit, onConfirm: (Float) -> Unit) {
    var text by remember { mutableStateOf(formatValue(currentValue, type)) }
    val parsed = text.replace(',', '.').toFloatOrNull()
    AlertDialog(onDismissRequest = onDismiss, title = { Text("ویرایش $label") },
        text = {
            Column {
                Text("مقدار فعلی: ${formatValue(currentValue, type)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(value = text, onValueChange = { text = it }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = parsed == null, singleLine = true, shape = RoundedCornerShape(12.dp), label = { Text("مقدار جدید") })
            }
        },
        confirmButton = { TextButton(onClick = { parsed?.let(onConfirm) }, enabled = parsed != null) { Text("تأیید") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } })
}
