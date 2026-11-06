package com.uvg.cc3087.myapp.data.repository

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.uvg.cc3087.myapp.data.local.*
import com.uvg.cc3087.myapp.domain.telemetry.ProductAnalytics
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject

/** Logical-event deduplication in encrypted Room; dispatch never contains domain free text. */
class FirebaseProductAnalytics(context: Context, private val dao: AnalyticsDao, private val environment: String) : ProductAnalytics {
    private val sdk = FirebaseAnalytics.getInstance(context)
    private val mutex = Mutex()
    init {
        sdk.setAnalyticsCollectionEnabled(true)
        sdk.setDefaultEventParameters(Bundle().apply { putString("environment", environment) })
    }
    override suspend fun record(name: String, logicalId: String, parameters: Map<String, Any>) {
        require(name in setOf("form_creation_started", "form_published"))
        require(parameters.keys.all { it in setOf("form_id", "creation_attempt_id", "template_type", "duration_seconds", "field_count") })
        require(parameters["form_id"].toString().matches(Regex("draft-[0-9a-f-]{36}")))
        require(parameters["creation_attempt_id"].toString().matches(Regex("[0-9a-f-]{36}")))
        require(parameters["template_type"] in setOf("unselected", "CUSTOM", "LEGACY", "JOB_APPLICATION", "ORDER_FORM", "EVENT_RSVP", "FEEDBACK"))
        parameters["duration_seconds"]?.let { require(it is Double && it.isFinite() && it >= 0) }
        parameters["field_count"]?.let { require(it is Long && it in 1..10) }
        if (name == "form_published" && dao.exists("start:${parameters["creation_attempt_id"]}") == 0) return
        dao.enqueue(AnalyticsEventEntity(logicalId, name, JSONObject(parameters).toString()))
        flush()
    }
    suspend fun flush() = mutex.withLock {
        for (event in dao.pending()) if (dao.claim(event.id) == 1) {
            val json = JSONObject(event.parametersJson)
            val bundle = Bundle()
            json.keys().forEach { key -> when (val value = json.get(key)) {
                is String -> bundle.putString(key, value)
                is Double -> bundle.putDouble(key, value)
                is Number -> bundle.putLong(key, value.toLong())
            } }
            // Claim first: SDK submission is at most once locally. Analytics delivery is not transactional.
            sdk.logEvent(event.name, bundle)
        }
    }
}
