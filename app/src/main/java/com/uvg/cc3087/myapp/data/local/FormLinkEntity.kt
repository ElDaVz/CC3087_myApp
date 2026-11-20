package com.uvg.cc3087.myapp.data.local

import androidx.room.Entity
import com.uvg.cc3087.myapp.data.repository.FormLinks

@Entity(tableName = "form_links", primaryKeys = ["ownerId", "formId"])
data class FormLinkEntity(
    val ownerId: String,
    val formId: String,
    val singleUseToken: String?,
    // Legacy column retained for non-destructive migration compatibility.
    val trackingToken: String
) {
    fun links(publicUrl: String) = FormLinks(
        openUrl = publicUrl,
        singleUseUrl = singleUseToken?.let { "${publicUrl.substringBeforeLast("/f/")}/i/$formId/$it" }
    )

    companion object {
        fun from(owner: String, formId: String, links: FormLinks): FormLinkEntity {
            return FormLinkEntity(
                ownerId = owner,
                formId = formId,
                singleUseToken = links.singleUseUrl?.substringAfterLast('/', "")?.takeIf {
                    it != links.openUrl.substringAfterLast('/') && it.length >= 20
                },
                trackingToken = ""
            )
        }
    }
}
