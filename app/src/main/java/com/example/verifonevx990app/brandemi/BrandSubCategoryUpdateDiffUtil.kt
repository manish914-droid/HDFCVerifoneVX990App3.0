package com.example.verifonevx990app.brandemi

import androidx.annotation.Nullable
import androidx.recyclerview.widget.DiffUtil

class BrandSubCategoryUpdateDiffUtil(
    private val oldList: MutableList<BrandEMIMasterSubCategoryDataModal>? = null,
    private val newList: MutableList<BrandEMIMasterSubCategoryDataModal>? = null
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldList?.size ?: 0

    override fun getNewListSize(): Int = newList?.size ?: 0

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList?.get(oldItemPosition)?.categoryID === newList?.get(newItemPosition)?.categoryID
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldData = oldList?.get(oldItemPosition)
        val newData = newList?.get(newItemPosition)

        return oldData?.categoryName == newData?.categoryName && oldData?.categoryID == newData?.categoryID
    }

    @Nullable
    override fun getChangePayload(oldPosition: Int, newPosition: Int): Any? {
        return super.getChangePayload(oldPosition, newPosition)
    }
}