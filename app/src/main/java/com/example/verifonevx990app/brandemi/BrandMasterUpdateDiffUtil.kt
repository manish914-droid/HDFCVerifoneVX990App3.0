package com.example.verifonevx990app.brandemi

import androidx.annotation.Nullable
import androidx.recyclerview.widget.DiffUtil

class BrandMasterUpdateDiffUtil(
    private val oldList: MutableList<BrandEMIMasterDataModal>? = null,
    private val newList: MutableList<BrandEMIMasterDataModal>? = null
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldList?.size ?: 0

    override fun getNewListSize(): Int = newList?.size ?: 0

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList?.get(oldItemPosition)?.brandID === newList?.get(newItemPosition)?.brandID
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldData = oldList?.get(oldItemPosition)
        val newData = newList?.get(newItemPosition)

        return oldData?.brandName == newData?.brandName && oldData?.brandID == newData?.brandID
    }

    @Nullable
    override fun getChangePayload(oldPosition: Int, newPosition: Int): Any? {
        return super.getChangePayload(oldPosition, newPosition)
    }
}