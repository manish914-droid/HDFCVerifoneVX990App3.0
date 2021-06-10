package com.example.verifonevx990app.emiCatalogue

import androidx.annotation.Nullable
import androidx.recyclerview.widget.DiffUtil

class EMICompareDiffUtil(
    private val oldList: MutableList<IssuerBankModal>?,
    private val newList: MutableList<IssuerBankModal>
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldList?.size ?: 0

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList?.get(oldItemPosition)?.issuerID === newList[newItemPosition].issuerID
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldData = oldList?.get(oldItemPosition)
        val newData = newList[newItemPosition]

        return oldData?.issuerBankTenure == newData.issuerBankTenure && oldData?.issuerID == newData.issuerID
    }

    @Nullable
    override fun getChangePayload(oldPosition: Int, newPosition: Int): Any? {
        return super.getChangePayload(oldPosition, newPosition)
    }
}