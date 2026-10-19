package com.uvg.cc3087.myapp.data

import android.content.res.Resources
import com.uvg.cc3087.myapp.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.uvg.cc3087.myapp.data.model.FormStatus
import com.uvg.cc3087.myapp.data.model.FormSummary

// datos locales para practicar la lista antes de conectarla a una fuente real :D
object FormSampleData {
    val forms: List<FormSummary>
        get() = forms()

    fun forms(resources: Resources? = null): List<FormSummary> {
        val locale = resources?.configuration?.locales?.get(0) ?: Locale.getDefault()
        val formatter = DateTimeFormatter.ofPattern("d MMM", locale)
        fun date(month: Int, day: Int) = LocalDate.of(2026, month, day).format(formatter)
        return listOf(
        FormSummary(
            id = "bakery-custom-order",
            title = resources?.getString(R.string.sample_bakery_order) ?: "Custom bakery order",
            responseCount = 12,
            updatedDate = date(7, 18),
            status = FormStatus.ACTIVE,
            imageUrl = "https://picsum.photos/seed/bakery-custom-order/160/160"
        ),
        FormSummary(
            id = "birthday-cake-request",
            title = resources?.getString(R.string.sample_birthday_cake) ?: "Birthday cake request",
            responseCount = 5,
            updatedDate = date(7, 14),
            status = FormStatus.ACTIVE,
            imageUrl = "https://picsum.photos/seed/birthday-cake-request/160/160"
        ),
        FormSummary(
            id = "wholesale-inquiry",
            title = resources?.getString(R.string.sample_wholesale) ?: "Wholesale inquiry",
            responseCount = 2,
            updatedDate = date(6, 30),
            status = FormStatus.DRAFT,
            imageUrl = "https://picsum.photos/seed/wholesale-inquiry/160/160"
        ),
        FormSummary(
            id = "wedding-cake-consultation",
            title = resources?.getString(R.string.sample_wedding_cake) ?: "Wedding cake consultation",
            responseCount = 8,
            updatedDate = date(6, 27),
            status = FormStatus.ACTIVE,
            imageUrl = "https://picsum.photos/seed/wedding-cake-consultation/160/160"
        ),
        FormSummary(
            id = "catering-order",
            title = resources?.getString(R.string.sample_catering) ?: "Catering order",
            responseCount = 15,
            updatedDate = date(6, 22),
            status = FormStatus.ACTIVE,
            imageUrl = "https://picsum.photos/seed/catering-order/160/160"
        ),
        FormSummary(
            id = "seasonal-menu-feedback",
            title = resources?.getString(R.string.sample_seasonal_menu) ?: "Seasonal menu feedback",
            responseCount = 21,
            updatedDate = date(6, 16),
            status = FormStatus.ACTIVE,
            imageUrl = "https://picsum.photos/seed/seasonal-menu-feedback/160/160"
        ),
        FormSummary(
            id = "custom-cupcake-box",
            title = resources?.getString(R.string.sample_cupcakes) ?: "Custom cupcake box",
            responseCount = 4,
            updatedDate = date(6, 10),
            status = FormStatus.DRAFT,
            imageUrl = "https://picsum.photos/seed/custom-cupcake-box/160/160"
        ),
        FormSummary(
            id = "corporate-event-order",
            title = resources?.getString(R.string.sample_corporate_event) ?: "Corporate event order",
            responseCount = 9,
            updatedDate = date(6, 4),
            status = FormStatus.ACTIVE,
            imageUrl = "https://picsum.photos/seed/corporate-event-order/160/160"
        ),
        FormSummary(
            id = "delivery-request",
            title = resources?.getString(R.string.sample_delivery) ?: "Delivery request",
            responseCount = 6,
            updatedDate = date(5, 28),
            status = FormStatus.DRAFT,
            imageUrl = "https://picsum.photos/seed/delivery-request/160/160"
        ),
        FormSummary(
            id = "customer-satisfaction-survey",
            title = resources?.getString(R.string.sample_satisfaction) ?: "Customer satisfaction survey",
            responseCount = 18,
            updatedDate = date(5, 20),
            status = FormStatus.ACTIVE,
            imageUrl = "https://picsum.photos/seed/customer-satisfaction-survey/160/160"
        )
    )
    }
}
