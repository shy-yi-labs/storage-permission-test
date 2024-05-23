package com.example.storagepermissiontest

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.storagepermissiontest.ui.theme.StoragePermissionTestTheme
import java.io.File
import java.io.FileNotFoundException


class MainActivity : ComponentActivity() {

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            Toast.makeText(this, "$it", Toast.LENGTH_SHORT).show()
        }

    private var readPermissionState by mutableStateOf(false)
    private var writePermissionState by mutableStateOf(false)

    private val testTargetDirs by lazy {
        buildList {
            var externalFilesDir = getExternalFilesDir(null)
            repeat(4) {
                add(externalFilesDir)
                externalFilesDir = externalFilesDir?.parentFile
            }
            add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS))
            add(Environment.getExternalStorageDirectory())
            add(File(Environment.getExternalStorageDirectory(), "test"))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            StoragePermissionTestTheme {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "External Read Permission ${if (readPermissionState) "Granted" else "Denied"}")
                        Text(text = "External Write Permission ${if (writePermissionState) "Granted" else "Denied"}")
                        Button(onClick = { permissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE) }) {
                            Text(text = "Request External Read Permission")
                        }
                        Button(onClick = { permissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) }) {
                            Text(text = "Request External Write Permission")
                        }
                        Button(onClick = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            val uri = Uri.fromParts("package", packageName, null)
                            intent.setData(uri)
                            startActivity(intent)
                        }) {
                            Text(text = "Permission Settings")
                        }

                        Button(onClick = ::readExternalStorage) {
                            Text(text = "Read")
                        }

                        Button(onClick = ::writeExternalStorage) {
                            Text(text = "Write")
                        }

                        Button(onClick = ::deleteExternalStorage) {
                            Text(text = "Delete")
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        readPermissionState = checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED
        writePermissionState = checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    private fun readExternalStorage() {
        testTargetDirs.forEach {
            readTestFile(it)
        }
    }

    private fun readTestFile(dir: File?) {
        val file = File(dir, "test.jpg")
        try {
            file.inputStream().use {
                println(it.readBytes().size.toString())
            }
            Log.d("MainActivity", "Read Success(${file.exists()}): ${dir?.absolutePath}")
        } catch (e: FileNotFoundException) {
            Log.d("MainActivity", "Read Failed(${file.exists()}): ${dir?.absolutePath}")
        }
    }

    private fun writeExternalStorage() {
        testTargetDirs.forEach {
            writeTestFile(it)
        }
    }

    private fun writeTestFile(dir: File?) {
        val file = File(dir, "test.txt")
        try {
            file.outputStream().use { fileOutputStream ->
                fileOutputStream.writer().use {
                    it.write("Hello World!")
                }
            }
            Log.d("MainActivity", "Write Success(${file.exists()}): ${dir?.absolutePath}")
        } catch (e: FileNotFoundException) {
            Log.d("MainActivity", "Write Failed(${file.exists()}): ${dir?.absolutePath}")
        }
    }

    private fun deleteExternalStorage() {
        testTargetDirs.forEach {
            deleteTestFile(it)
        }
    }

    private fun deleteTestFile(dir: File?) {
        val file = File(dir, "test.txt")
        file.delete()
        if (file.exists().not()) {
            Log.d("MainActivity", "Delete Success(${file.exists()}): ${dir?.absolutePath}")
        } else {
            Log.d("MainActivity", "Delete Failed(${file.exists()}): ${dir?.absolutePath}")
        }
    }
}