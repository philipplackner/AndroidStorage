package com.plcoding.androidstorage

import android.Manifest
import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.ContentValues
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.plcoding.androidstorage.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var mBinding: ActivityMainBinding

    private lateinit var mInternalStoragePhotoAdapter: InternalStoragePhotoAdapter

    private lateinit var mSharedPhotoAdapter: SharedPhotoAdapter

    private lateinit var mPermissionLauncher: ActivityResultLauncher<Array<String>>

    private lateinit var mIntentSenderLauncher: ActivityResultLauncher<IntentSenderRequest>

    private lateinit var mContentObserver: ContentObserver

    private var readPermissionGranted = false

    private var writePermissionGranted = false

    private var mDeleteContentUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBinding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(mBinding.root)

        mInternalStoragePhotoAdapter = InternalStoragePhotoAdapter {
            lifecycleScope.launch {
                val isDeleteSuccess = deleteImageFromInternalStorage(it.name)
                if (isDeleteSuccess) {
                    loadDataIntoInternalStorageAdapter()
                    Toast.makeText(
                        this@MainActivity,
                        "Picture delete successfully",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                } else
                    Toast.makeText(
                        this@MainActivity,
                        "Failed to delete picture",
                        Toast.LENGTH_SHORT
                    ).show()
            }
        }

        mSharedPhotoAdapter = SharedPhotoAdapter {
            lifecycleScope.launch {
                deleteImageFromSharedStorage(it.contentUri)
                mDeleteContentUri = it.contentUri
            }
        }

        mIntentSenderLauncher =
            registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    if (isSDK29()) {
                        lifecycleScope.launch {
                            deleteImageFromSharedStorage(mDeleteContentUri ?: return@launch)
                        }
                    }
                    Toast.makeText(
                        this@MainActivity,
                        "Image deleted successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Failed to delete image",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                loadDataIntoSharedStorageAdapter()
            }

        mPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions())
            { permissions ->
                lifecycleScope.launch {
                    readPermissionGranted =
                        permissions[Manifest.permission.READ_EXTERNAL_STORAGE]
                            ?: readPermissionGranted
                    writePermissionGranted =
                        permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE]
                            ?: writePermissionGranted

                    if (readPermissionGranted) {
                        loadDataIntoSharedStorageAdapter()
                    } else
                        Toast.makeText(
                            this@MainActivity,
                            "Permission required to load images",
                            Toast.LENGTH_SHORT
                        ).show()
                }
            }

        requestPermissions()

        val takePicture =
            registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
                lifecycleScope.launch {
                    bitmap?.let {
                        val isPrivate = mBinding.switchPrivate.isChecked

                        val isSaveSuccessfully = when {
                            isPrivate -> saveBitmapIntoInternalStorage(bitmap)
                            writePermissionGranted -> saveBitmapIntoExternalStorage(bitmap)
                            else -> false
                        }

                        if (isSaveSuccessfully) {
                            Toast.makeText(
                                this@MainActivity,
                                "Picture saved successfully",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else
                            Toast.makeText(
                                this@MainActivity,
                                "Failed to save picture",
                                Toast.LENGTH_SHORT
                            ).show()


                        if (isPrivate) {
                            loadDataIntoInternalStorageAdapter()
                        } else {
                            loadDataIntoSharedStorageAdapter()
                        }
                    }
                }
            }

        mBinding.btnTakePhoto.setOnClickListener {
            takePicture.launch()
        }

        initContentObserver()

        setUpInternalStorageRecyclerView()

        setUpSharedStorageRecyclerView()

        loadDataIntoInternalStorageAdapter()

        loadDataIntoSharedStorageAdapter()
    }

    private fun initContentObserver() {
        mContentObserver = object : ContentObserver(null) {
            override fun onChange(selfChange: Boolean) {
                if (selfChange && readPermissionGranted) {
                    loadDataIntoSharedStorageAdapter()
                }
            }
        }

        // register to content observer for listening any changes made at image collection on shared storage.
        contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            mContentObserver
        )
    }

    /**
     * set up recyclerview
     */
    private fun setUpSharedStorageRecyclerView() = mBinding.rvPublicPhotos.apply {
        adapter = mSharedPhotoAdapter
        layoutManager = StaggeredGridLayoutManager(3, RecyclerView.VERTICAL)
    }

    private fun setUpInternalStorageRecyclerView() = mBinding.rvPrivatePhotos.apply {
        adapter = mInternalStoragePhotoAdapter
        layoutManager = StaggeredGridLayoutManager(3, RecyclerView.VERTICAL)
    }

    /**
     * loads data into adapter
     */
    private fun loadDataIntoInternalStorageAdapter() {
        lifecycleScope.launch {
            val pictures = getPrivateStorageImages()
            mInternalStoragePhotoAdapter.submitList(pictures)
        }
    }

    private fun loadDataIntoSharedStorageAdapter() {
        lifecycleScope.launch {
            val pictures = withContext(Dispatchers.IO) {
                getSharedStorageImages()
            }
            mSharedPhotoAdapter.submitList(pictures)
        }
    }

    /**
     * save bitmap into storage
     */
    private suspend fun saveBitmapIntoInternalStorage(bitmap: Bitmap): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val fileName = "${UUID.randomUUID()}.jpg" // random file name.
                openFileOutput(fileName, MODE_PRIVATE).use { outputStream ->
                    val isSaved = bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
                    if (!isSaved)
                        throw IOException("Failed to saved bitmap into internal storage.")
                }
                true
            } catch (e: IOException) {
                e.printStackTrace()
                Log.e(TAG, "storeBitmapIntoInternalStorage: ${e.message}")
                false
            }
        }
    }

    private suspend fun saveBitmapIntoExternalStorage(bitmap: Bitmap): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val imageCollection = isSDK30AndUp {
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY) // content uri for sdk level 29 or higher
                }
                    ?: MediaStore.Images.Media.EXTERNAL_CONTENT_URI // content uri for sdk level lower than 29.

                val displayName = UUID.randomUUID().toString()

                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "$displayName.jpg")
                    put(MediaStore.Images.Media.WIDTH, bitmap.width)
                    put(MediaStore.Images.Media.HEIGHT, bitmap.height)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                }

                contentResolver.insert(imageCollection, contentValues)?.also { uri ->
                    contentResolver.openOutputStream(uri).use {
                        if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 99, it)) {
                            throw IOException("Couldn't save the bitmap in shared storage")
                        }
                    }
                } ?: throw IOException("Couldn't create media store entry")

                true
            } catch (e: IOException) {
                e.printStackTrace()
                Log.e(TAG, "saveBitmapIntoExternalStorage: ${e.message}")
                false
            }
        }
    }

    /**
     * Gets image from the storage
     */
    private suspend fun getPrivateStorageImages(): List<InternalStoragePhoto> {
        return withContext(Dispatchers.IO) {
            val files = filesDir.listFiles() // get the root folder files.
            files?.filter { file ->
                file != null && file.canRead() && file.name.endsWith(".jpg")
            }?.map { file ->
                val bitmap =
                    BitmapFactory.decodeByteArray(file.readBytes(), 0, file.readBytes().size)
                InternalStoragePhoto(file.name, bitmap)
            } ?: listOf()
        }
    }

    private fun getSharedStorageImages(): List<SharedStoragePhoto> {
        val collection = isSDK30AndUp {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } ?: MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
        )

        val sharedPhotosList = mutableListOf<SharedStoragePhoto>()

        contentResolver.query(
            collection,
            projection,
            null,
            null,
            "${MediaStore.Images.Media.DISPLAY_NAME} ASC"
        )?.use { cursor ->
            val idColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val displayNameColumnIndex =
                cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val widthColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val heightColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)

            var count = 20

            while (cursor.moveToNext() && count-- >= 0) {
                val id = cursor.getLong(idColumnIndex)
                val displayName = cursor.getString(displayNameColumnIndex)
                val width = cursor.getInt(widthColumnIndex)
                val height = cursor.getInt(heightColumnIndex)
                val imageUri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                sharedPhotosList.add(
                    SharedStoragePhoto(
                        id,
                        displayName,
                        width,
                        height,
                        imageUri
                    )
                )
                Log.i(TAG, "getSharedStorageImages: $displayName")
            }
        }
        return sharedPhotosList.toList()
    }


    /**
     * Delete image
     */
    private suspend fun deleteImageFromInternalStorage(filename: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                deleteFile(filename)
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    private suspend fun deleteImageFromSharedStorage(contentUri: Uri) {
        return withContext(Dispatchers.IO) {
            try {
                // case 1 : delete image on api level 29 below
                contentResolver.delete(contentUri, null, null)
            } catch (e: SecurityException) {
                // case 2 : delete image on api level 29 and above
                val intentSender = when {
                    isSDK29() -> {
                        val recoverableSecurityException =
                            e as? RecoverableSecurityException ?: throw RuntimeException(
                                e.message,
                                e
                            )
                        recoverableSecurityException.userAction.actionIntent.intentSender
                    }

                    isSDK30AndUp() -> {
                        MediaStore.createDeleteRequest(
                            contentResolver,
                            listOf(contentUri)
                        ).intentSender
                    }

                    else -> null
                }

                intentSender?.let {
                    mIntentSenderLauncher.launch(IntentSenderRequest.Builder(it).build())
                }
                Log.e(TAG, "deleteImageFromSharedStorage: ${e.message}")
            }
        }
    }

    private fun requestPermissions() {
        val permissions = mutableListOf<String>()
        val hasReadPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        val hasWritePermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        val minSdk29 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

        readPermissionGranted = hasReadPermission

        writePermissionGranted = hasWritePermission || minSdk29

        if (!readPermissionGranted) {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        if (!writePermissionGranted) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        if (permissions.isNotEmpty()) {
            mPermissionLauncher.launch(permissions.toTypedArray())
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        contentResolver.unregisterContentObserver(mContentObserver)
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
