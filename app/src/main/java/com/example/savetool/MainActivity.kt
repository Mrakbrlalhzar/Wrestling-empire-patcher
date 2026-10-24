package com.example.savetool

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                SaveEditorApp()
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

    val openLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedCharIndex = null
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
                                ?: throw java.io.IOException("cannot open output")
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                "✓ ذخیره شد (${vm.editsCount()} تغییر)",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "خطا: ${e.message}", Toast.LENGTH_LONG).show()
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
        topBar = {
            TopAppBar(
                title = {
                    Text(selectedChar?.name?.ifEmpty { "بدون نام" } ?: "Save Editor")
                },
                navigationIcon = {
                    if (selectedChar != null) {
                        IconButton(onClick = { selectedCharIndex = null }) {
                            Text("←", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (vm.session != null) {
                BottomAppBar {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${vm.editsCount()} تغییر", Modifier.weight(1f))
                        Button(
                            onClick = { saveLauncher.launch("Save.bytes.patched") },
                            enabled = vm.editsCount() > 0
                        ) {
                            Text("ذخیره")
                        }
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
                vm.isLoading -> {
                    Column(
                        Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(16.dp))
                        Text("در حال خواندن فایل...")
                    }
                }

                vm.error != null -> {
                    Column(
                        Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("خطا", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(8.dp))
                        Text(vm.error!!, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { openLauncher.launch(arrayOf("*/*")) }) {
                            Text("انتخاب فایل دیگر")
                        }
                    }
                }

                vm.session == null -> {
                    Column(
                        Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "فایل Save.bytes را انتخاب کنید",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { openLauncher.launch(arrayOf("*/*")) }) {
                            Text("📂 انتخاب فایل")
                        }
                    }
                }

                selectedChar != null -> {
                    CharacterDetailScreen(
                        vm = vm,
                        char = selectedChar,
                        onBack = { selectedCharIndex = null }
                    )
                }

                else -> {
                    CharacterListScreen(
                        vm = vm,
                        characters = characters,
                        onSelect = { selectedCharIndex = it.arrayIndex }
                    )
                }
            }
        }
    }
}

@Composable
fun CharacterListScreen(
    vm: SaveViewModel,
    characters: List<Character>,
    onSelect: (Character) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Button(
            onClick = { vm.setAllToEveryone(-1000f) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text("⚡ همه‌ی استت‌ها → -1000.0")
        }
        if (characters.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("شخصیتی یافت نشد")
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(characters) { char ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .clickable { onSelect(char) }
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                char.name.ifEmpty { "(بدون نام)" },
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${char.stats.size} استت — ایندکس #${char.arrayIndex}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CharacterDetailScreen(
    vm: SaveViewModel,
    char: Character,
    onBack: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Button(
            onClick = { vm.setCharacterStats(char, 1000f) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text("⚡ همه‌ی استت‌های این شخصیت → 1000.0")
        }
        if (char.stats.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("این شخصیت استت ندارد")
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(char.stats.size) { i ->
                    StatRow(vm = vm, slot = char.stats[i], label = "stat[$i]")
                }
            }
        }
    }
}

@Composable
fun StatRow(vm: SaveViewModel, slot: StatSlot, label: String) {
    var dialogOpen by remember { mutableStateOf(false) }
    val currentValue = vm.currentValueOf(slot)

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(
            "%.2f".format(currentValue),
            Modifier.width(100.dp),
            style = MaterialTheme.typography.bodyMedium
        )
        TextButton(onClick = { dialogOpen = true }) {
            Text("ویرایش")
        }
    }

    if (dialogOpen) {
        EditStatDialog(
            currentValue = currentValue,
            label = label,
            onDismiss = { dialogOpen = false },
            onConfirm = { v ->
                vm.setStat(slot, v)
                dialogOpen = false
            }
        )
    }
}

@Composable
fun EditStatDialog(
    currentValue: Float,
    label: String,
    onDismiss: () -> Unit,
    onConfirm: (Float) -> Unit
) {
    var text by remember { mutableStateOf(currentValue.toString()) }
    val parsed = text.replace(',', '.').toFloatOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ویرایش $label") },
        text = {
            Column {
                Text("مقدار فعلی: $currentValue", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = parsed == null,
                    singleLine = true,
                    label = { Text("مقدار جدید") }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { parsed?.let(onConfirm) },
                enabled = parsed != null
            ) { Text("تأیید") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}
