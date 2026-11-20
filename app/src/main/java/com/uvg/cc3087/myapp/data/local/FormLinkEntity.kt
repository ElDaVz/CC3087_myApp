package com.uvg.cc3087.myapp.data.local

import androidx.room.Entity
import com.uvg.cc3087.myapp.data.repository.FormLinks

@Entity(tableName = "form_links", primaryKeys = ["ownerId", "formId"])
data class FormLinkEntity(
    val ownerId: String,
    val formId: String,
    val singleUseToken: String?,
    val trackingToken: String
) {
    fun links(publicUrl: String) = FormLinks(
        openUrl = publicUrl,
        singleUseUrl = singleUseToken?.let { "${publicUrl.substringBeforeLast("/f/")}/i/$it" },
        trackingToken = trackingToken
    )

    companion object {
        fun from(owner: String, formId: String, links: FormLinks): FormLinkEntity {
            val base = links.openUrl.substringBeforeLast("/f/")
            return FormLinkEntity(
                ownerId = owner,
                formId = formId,
                singleUseToken = links.singleUseUrl?.substringAfterLast('/', "")?.takeIf {
                    it != links.openUrl.substringAfterLast('/') && it.length >= 20
                },
                trackingToken = checkNotNull(links.trackingToken?.takeIf { it.length >= 20 })
            )
        }
    }
}
