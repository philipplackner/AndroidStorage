package com.plcoding.androidstorage

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintSet
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.plcoding.androidstorage.databinding.ItemLoadingBinding
import com.plcoding.androidstorage.databinding.ItemPhotoBinding
import java.lang.IllegalArgumentException

class SharedPhotoAdapter(
    private val onPhotoClick: (SharedStoragePhoto) -> Unit
) : ListAdapter<SharedStoragePhoto, BaseViewHolder>(sharePhotoDiffUtil) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        return when (viewType) {
            R.layout.item_photo -> {
                BaseViewHolder.ImageViewHolder(
                    ItemPhotoBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }

            R.layout.item_loading -> {
                BaseViewHolder.ProgressViewHolder(
                    ItemLoadingBinding.inflate(
                        LayoutInflater.from(
                            parent.context
                        ), parent, false
                    )
                )
            }
            else -> throw IllegalArgumentException("Invalid ViewHolder type provided")
        }
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        val item = currentList[position]
        when(holder) {
            is BaseViewHolder.ImageViewHolder -> holder.bind(item,onPhotoClick)
            is BaseViewHolder.ProgressViewHolder -> { /* No operation */ }
        }
    }

    override fun getItemCount(): Int {
        return currentList.size
    }

    override fun getItemViewType(position: Int): Int {
        return if(position == currentList.size - 1) {
            R.layout.item_loading  // if current element is last element then show loading view
        } else {
            R.layout.item_photo // if current element is not last element then continue showing images.
        }
    }

    companion object {
        val sharePhotoDiffUtil = object : DiffUtil.ItemCallback<SharedStoragePhoto>() {
            override fun areItemsTheSame(
                oldItem: SharedStoragePhoto,
                newItem: SharedStoragePhoto
            ): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(
                oldItem: SharedStoragePhoto,
                newItem: SharedStoragePhoto
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}