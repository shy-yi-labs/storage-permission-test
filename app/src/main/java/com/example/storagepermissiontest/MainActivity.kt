package com.example.storagepermissiontest

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
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
            add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES))
            add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES))
            add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC))
            add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS))
            add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS))
            add(Environment.getExternalStorageDirectory())
            add(File(Environment.getExternalStorageDirectory(), "test"))
        }
    }

    private val testTargetMediaStores by lazy {
        buildList {
            add(MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            add(MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            add(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
            if (Build.VERSION_CODES.Q <= Build.VERSION.SDK_INT) {
                add(MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL))
                add(MediaStore.Downloads.EXTERNAL_CONTENT_URI)
            } else {
                add(MediaStore.Files.getContentUri("external"))
            }
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
        testTargetDirs.forEachIndexed { index, file ->
            readTestFile(file, index)
        }

        testTargetMediaStores.forEachIndexed { index, file ->
            readUsingMediaStore(file, "test$index.txt")
            readUsingMediaStore(file, "images$index.jpeg")
        }
    }

    private fun readUsingMediaStore(uri: Uri, fileName: String) {
        val selection = "${MediaStore.Files.FileColumns.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(fileName)
        val cursor = runCatching {
            contentResolver.query(uri, null, selection, selectionArgs, null)
        }.getOrNull()

        if (cursor != null && cursor.moveToFirst()) {
            val idColumn = cursor.getColumnIndex(MediaStore.Files.FileColumns._ID)
            val id = cursor.getLong(idColumn)
            val contentUri = Uri.withAppendedPath(uri, id.toString())

            val size = contentResolver.openInputStream(contentUri)?.use { inputStream ->
                inputStream.readBytes().size.also {
                    println(it.toString())
                }
            }

            Log.d("MainActivity", "Read Success($size): $uri")
        } else {
            Log.d("MainActivity", "Read Failed: $uri")
        }

        cursor?.close()
    }

    private fun readTestFile(dir: File?, index: Int) {
        val textFile = File(dir, "test$index.txt")
        try {
            textFile.inputStream().use {
                println(it.readBytes().size.toString())
            }
            Log.d("MainActivity", "Read Success(${textFile.exists()}): ${textFile.absolutePath}")
        } catch (e: FileNotFoundException) {
            Log.d("MainActivity", "Read Failed(${textFile.exists()}): ${textFile.absolutePath}")
        }

        val imageFile = File(dir, "images$index.jpeg")
        try {
            imageFile.inputStream().use {
                println(it.readBytes().size.toString())
            }
            Log.d("MainActivity", "Read Success(${imageFile.exists()}): ${imageFile.absolutePath}")
        } catch (e: FileNotFoundException) {
            Log.d("MainActivity", "Read Failed(${imageFile.exists()}): ${imageFile.absolutePath}")
        }
    }

    private fun writeExternalStorage() {
        testTargetDirs.forEachIndexed { index, file ->
            writeTestFile(file, index)
        }

        testTargetMediaStores.forEachIndexed { index, file ->
            writeUsingMediaStore(file, index)
        }
    }

    private fun writeUsingMediaStore(uri: Uri, index: Int) {
        val textFileName = "test$index.txt"
        val textFileUri = runCatching {
            contentResolver.insert(
                uri,
                ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, textFileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                    put(MediaStore.MediaColumns.DATA, File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), textFileName).absolutePath)
                }
            )
        }
//            .onFailure { it.printStackTrace() }
            .getOrNull()

        if (textFileUri == null) {
            Log.d("MainActivity", "Write Failed: ${Uri.withAppendedPath(uri,textFileName)}")
        } else {
            contentResolver.openOutputStream(textFileUri)?.use { outputStream ->
                outputStream.writer().use {
                    it.write("Hello World!")
                }
            }

            Log.d("MainActivity", "Write Success: $textFileUri")
        }

        val imageFileName = "images$index.jpeg"

        val fileUri = runCatching {
            contentResolver.insert(
                uri,
                ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, imageFileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.MediaColumns.DATA, File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), imageFileName).absolutePath)
                }
            )
        }
//            .onFailure { it.printStackTrace() }
            .getOrNull()

        if (fileUri == null) {
            Log.d("MainActivity", "Write Failed: ${Uri.withAppendedPath(uri,imageFileName)}")
        } else {
            contentResolver.openOutputStream(fileUri)?.use { outputStream ->
                resources.openRawResource(R.raw.images).use { inputStream ->
                    outputStream.write(inputStream.readBytes())
                }
            }

            Log.d("MainActivity", "Write Success: $fileUri")
        }
    }

    private fun writeTestFile(dir: File?, index: Int) {
        if (dir?.exists() == false) dir.mkdirs()

        val textFile = File(dir, "test$index.txt")

        try {
            textFile.outputStream().use { fileOutputStream ->
                fileOutputStream.writer().use {
                    it.write("Hello World!")
                }
            }
            Log.d("MainActivity", "Write Success(${textFile.exists()}): ${textFile.absolutePath}")
        } catch (e: FileNotFoundException) {
            Log.d("MainActivity", "Write Failed(${textFile.exists()}): ${textFile.absolutePath}")
        }

        val imageFile = File(dir, "images$index.jpeg")
        try {
            imageFile.outputStream().use { fileOutputStream ->
                resources.openRawResource(R.raw.images).use { inputStream ->
                    fileOutputStream.write(inputStream.readBytes())
                }
            }
            Log.d("MainActivity", "Write Success(${imageFile.exists()}): ${imageFile.absolutePath}")
        } catch (e: FileNotFoundException) {
            Log.d("MainActivity", "Write Failed(${imageFile.exists()}): ${imageFile.absolutePath}")
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