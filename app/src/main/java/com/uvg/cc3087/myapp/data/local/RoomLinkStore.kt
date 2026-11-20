package com.uvg.cc3087.myapp.data.local

import com.uvg.cc3087.myapp.data.repository.FormLinks
import com.uvg.cc3087.myapp.data.repository.LinkStore

/** Secrets stay in the encrypted, owner-partitioned local database. */
class RoomLinkStore(private val dao: LinkDao, private val publicUrl: (String) -> String) : LinkStore {
    override suspend fun get(owner: String, formId: String): FormLinks? =
        dao.get(owner, formId)?.links(publicUrl(formId))

    override suspend fun save(owner: String, formId: String, links: FormLinks) {
        dao.save(FormLinkEntity.from(owner, formId, links))
    }
}
