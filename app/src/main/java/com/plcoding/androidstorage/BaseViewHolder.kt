package com.plcoding.androidstorage

import android.util.Log
import androidx.constraintlayout.widget.ConstraintSet
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.plcoding.androidstorage.databinding.ItemLoadingBinding
import com.plcoding.androidstorage.databinding.ItemPhotoBinding

/**
 * Created By Dhruv Limbachiya on 04-03-2022 12:05 PM.
 */
sealed class BaseViewHolder(binding:ViewBinding) : RecyclerView.ViewHolder(binding.root) {

    class ImageViewHolder(private val binding: ItemPhotoBinding) : BaseViewHolder(binding) {
        fun bind (item: SharedStoragePhoto,onPhotoClick: (SharedStoragePhoto) -> Unit) {

            binding.ivPhoto.setImageURI(item.contentUri)

//            val aspectRatio = binding.ivPhoto.width.toFloat() / binding.ivPhoto.height.toFloat()
//
//            ConstraintSet().apply {
//                clone(binding.root)
//                setDimensionRatio(binding.ivPhoto.id, aspectRatio.toString())
//                applyTo(binding.root)
//            }

            binding.ivPhoto.setOnLongClickListener {
                onPhotoClick(item)
                true
            }

            Log.i("BaseViewHolder", "bind: ")
        }
    }

    class ProgressViewHolder(binding:ItemLoadingBinding) : BaseViewHolder(binding) {}
}