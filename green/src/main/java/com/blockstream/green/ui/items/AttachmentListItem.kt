package com.blockstream.green.ui.items

import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockstream.green.R
import com.blockstream.green.databinding.ListAttachmentBinding
import com.mikepenz.fastadapter.binding.AbstractBindingItem
import com.mikepenz.fastadapter.ui.utils.StringHolder

data class AttachmentListItem constructor(
    var text: StringHolder
): AbstractBindingItem<ListAttachmentBinding>() {
    override val type: Int
     get() = R.id.fastadapter_attachment_text_item_id

    init {
        identifier = hashCode().toLong()
    }

    override fun bindView(binding: ListAttachmentBinding, payloads: List<Any>) {
        text.applyTo(binding.attachmentName)
    }

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?
    ): ListAttachmentBinding {
        return ListAttachmentBinding.inflate(inflater, parent, false)
    }
}
