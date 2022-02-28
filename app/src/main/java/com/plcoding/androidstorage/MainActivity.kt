package com.plcoding.androidstorage

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.appcompat.app.AppCompatActivity
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        val takePicture = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
            bitmap?.let {
                val isPrivate = mBinding.switchPrivate.isChecked
                if (isPrivate) {
                    val isSaveSuccess = storeBitmapIntoInternalStorage(bitmap)
                    if (isSaveSuccess) {
                        loadDataIntoInternalStorageRecyclerView()
                        Toast.makeText(this, "Picture saved successfully", Toast.LENGTH_SHORT)
                            .show()
                    } else
                        Toast.makeText(this, "Failed to save picture", Toast.LENGTH_SHORT)
                            .show()
                }
            }
        }

        mInternalStoragePhotoAdapter = InternalStoragePhotoAdapter {
            val isDeleteSuccess = deleteFile(it.name)
            if (isDeleteSuccess) {
                loadDataIntoInternalStorageRecyclerView()
                Toast.makeText(this, "Picture delete successfully", Toast.LENGTH_SHORT)
                    .show()
            } else
                Toast.makeText(this, "Failed to delete picture", Toast.LENGTH_SHORT)
                    .show()
        }

        mBinding.btnTakePhoto.setOnClickListener {
            takePicture.launch()
        }

        setUpInternalStorageRecyclerView()

        loadDataIntoInternalStorageRecyclerView()
    }

    private fun setUpInternalStorageRecyclerView() = mBinding.rvPrivatePhotos.apply {
        adapter = mInternalStoragePhotoAdapter
        layoutManager = StaggeredGridLayoutManager(3,RecyclerView.VERTICAL)
    }

    private suspend fun getAllInternalImages(): List<InternalStoragePhoto> {
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

    private fun loadDataIntoInternalStorageRecyclerView() {
        lifecycleScope.launch {
            val pictures = getAllInternalImages()
            mInternalStoragePhotoAdapter.submitList(pictures)
        }
    }

    private fun storeBitmapIntoInternalStorage(bitmap: Bitmap): Boolean {
        return try {
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

    companion object {
        private const val TAG = "MainActivity"
    }
}