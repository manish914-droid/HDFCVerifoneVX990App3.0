package com.example.verifonevx990app.vxUtils

import androidx.annotation.Nullable
import androidx.recyclerview.widget.DiffUtil
import com.example.verifonevx990app.realmtables.BatchFileDataTable

class RecyclerViewUpdateUtils(
    oldBatchFileList: MutableList<BatchFileDataTable>,
    newBatchFileList: MutableList<BatchFileDataTable>
) : DiffUtil.Callback() {

    private var oldBatchList: MutableList<BatchFileDataTable>? = null
    private var newBatchList: MutableList<BatchFileDataTable>? = null

    init {
        this.oldBatchList = oldBatchFileList
        this.newBatchList = newBatchFileList
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldBatchList?.get(oldItemPosition)?.invoiceNumber == newBatchList?.get(
            newItemPosition
        )?.invoiceNumber
    }

    override fun getOldListSize(): Int {
        return oldBatchList?.size ?: 0
    }

    override fun getNewListSize(): Int {
        return newBatchList?.size ?: 0
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldBatchList?.get(oldItemPosition) == newBatchList?.get(newItemPosition)
    }

    @Nullable
    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
        return super.getChangePayload(oldItemPosition, newItemPosition)
    }
}