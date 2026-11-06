package com.uvg.cc3087.myapp

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.uvg.cc3087.myapp.data.local.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class AnalyticsLedgerTest {
    @Test fun logicalMilestoneCanOnlyBeEnqueuedAndClaimedOnce() = runBlocking {
        val db=Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext,FormLinkDatabase::class.java).build()
        try {
            val event=AnalyticsEventEntity("publish:random", "form_published", "{}")
            assertTrue(db.analytics().enqueue(event)!=-1L)
            assertEquals(-1L,db.analytics().enqueue(event))
            assertEquals(1,db.analytics().pending().size)
            assertEquals(1,db.analytics().claim(event.id));assertEquals(0,db.analytics().claim(event.id))
            assertTrue(db.analytics().pending().isEmpty())
        }finally{db.close()}
    }
}
