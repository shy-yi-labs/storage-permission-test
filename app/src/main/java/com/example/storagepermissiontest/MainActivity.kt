package com.example.storagepermissiontest

import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.storagepermissiontest.ui.theme.StoragePermissionTestTheme
import java.io.File

class MainActivity : ComponentActivity() {

    private val sdCardPath = Environment.getExternalStorageDirectory().path

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        Toast.makeText(this, "$it", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)

        setContent {
            StoragePermissionTestTheme {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column {
                        Button(onClick = ::readExternalStorage) {
                            Text(text = "Read")
                        }

                        Button(onClick = ::writeExternalStorage) {
                            Text(text = "Write")
                        }
                    }
                }
            }
        }
    }

    private fun readExternalStorage() {
        File(sdCardPath).listFiles()?.forEach {
            println(it.name)
        }

        File(sdCardPath + "/Download/text.txt").inputStream().use {
            println(it.readBytes().size.toString())
        }

        File(sdCardPath + "/Download/images.jpeg").inputStream().use {
            println(it.readBytes().size.toString())
        }
    }

    private fun writeExternalStorage() {
        if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_DENIED) {
            permissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            return
        }

        File(sdCardPath + "/Download/text.txt").outputStream().use { fileOutputStream ->
            fileOutputStream.writer().use {
                it.write("Hello World!")
            }
        }
    }
}