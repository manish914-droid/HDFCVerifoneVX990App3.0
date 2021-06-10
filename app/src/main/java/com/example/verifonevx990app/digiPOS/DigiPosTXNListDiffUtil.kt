package com.example.verifonevx990app.digiPOS

import androidx.annotation.Nullable
import androidx.recyclerview.widget.DiffUtil

class DigiPosTXNListDiffUtil(
    private val oldList: MutableList<DigiPosTxnModal>? = null,
    private val newList: MutableList<DigiPosTxnModal>? = null
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldList?.size ?: 0

    override fun getNewListSize(): Int = newList?.size ?: 0

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList?.get(oldItemPosition)?.mTXNID === newList?.get(newItemPosition)?.mTXNID
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldData = oldList?.get(oldItemPosition)
        val newData = newList?.get(newItemPosition)

        return oldData?.mTXNID == newData?.mTXNID && oldData?.partnerTXNID == newData?.partnerTXNID
    }

    @Nullable
    override fun getChangePayload(oldPosition: Int, newPosition: Int): Any? {
        return super.getChangePayload(oldPosition, newPosition)
    }
}