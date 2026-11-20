package com.uvg.cc3087.myapp.data.repository

import com.uvg.cc3087.myapp.ui.state.FormSortOrder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

/** Owner is an opaque partition key: "guest" or an authenticated user ID. */
interface FormListPreferences {
    fun observe(owner: String): Flow<FormSortOrder>
    suspend fun set(owner: String, order: FormSortOrder)
}

/** Process-local default for previews and tests; no preference crosses a session. */
class InMemoryFormListPreferences : FormListPreferences {
    private val orders = mutableMapOf<String, MutableStateFlow<FormSortOrder>>()

    override fun observe(owner: String) = synchronized(orders) {
        orders.getOrPut(owner) { MutableStateFlow(FormSortOrder.RECENT_FIRST) }
    }

    override suspend fun set(owner: String, order: FormSortOrder) {
        observe(owner).value = order
    }
}

object UnavailableFormListPreferences : FormListPreferences {
    override fun observe(owner: String): Flow<FormSortOrder> = flowOf(FormSortOrder.RECENT_FIRST)
    override suspend fun set(owner: String, order: FormSortOrder) = Unit
}
