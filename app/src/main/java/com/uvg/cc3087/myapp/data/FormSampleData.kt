package com.uvg.cc3087.myapp.data

import com.uvg.cc3087.myapp.data.model.FormStatus
import com.uvg.cc3087.myapp.data.model.FormSummary

// datos locales para practicar la lista antes de conectarla a una fuente real :D
object FormSampleData {
    val forms = listOf(
        FormSummary(
            id = "bakery-custom-order",
            title = "Bakery Custom Order",
            responseCount = 12,
            updatedDate = "Jul 18",
            status = FormStatus.ACTIVE,
            imageUrl = "https://picsum.photos/seed/bakery-custom-order/160/160"
        ),
        FormSummary(
            id = "birthday-cake-request",
            title = "Birthday Cake Request",
            responseCount = 5,
            updatedDate = "Jul 14",
            status = FormStatus.ACTIVE,
            imageUrl = "https://picsum.photos/seed/birthday-cake-request/160/160"
        ),
        FormSummary(
            id = "wholesale-inquiry",
            title = "Wholesale Inquiry",
            responseCount = 2,
            updatedDate = "Jun 30",
            status = FormStatus.DRAFT,
            imageUrl = "https://picsum.photos/seed/wholesale-inquiry/160/160"
        ),
        FormSummary(
            id = "wedding-cake-consultation",
            title = "Wedding Cake Consultation",
            responseCount = 8,
            updatedDate = "Jun 27",
            status = FormStatus.ACTIVE,
            imageUrl = "https://picsum.photos/seed/wedding-cake-consultation/160/160"
        ),
        FormSummary(
            id = "catering-order",
            title = "Catering Order",
            responseCount = 15,
            updatedDate = "Jun 22",
            status = FormStatus.ACTIVE,
            imageUrl = "https://picsum.photos/seed/catering-order/160/160"
        ),
        FormSummary(
            id = "seasonal-menu-feedback",
            title = "Seasonal Menu Feedback",
            responseCount = 21,
            updatedDate = "Jun 16",
            status = FormStatus.ACTIVE,
            imageUrl = "https://picsum.photos/seed/seasonal-menu-feedback/160/160"
        ),
        FormSummary(
            id = "custom-cupcake-box",
            title = "Custom Cupcake Box",
            responseCount = 4,
            updatedDate = "Jun 10",
            status = FormStatus.DRAFT,
            imageUrl = "https://picsum.photos/seed/custom-cupcake-box/160/160"
        ),
        FormSummary(
            id = "corporate-event-order",
            title = "Corporate Event Order",
            responseCount = 9,
            updatedDate = "Jun 4",
            status = FormStatus.ACTIVE,
            imageUrl = "https://picsum.photos/seed/corporate-event-order/160/160"
        ),
        FormSummary(
            id = "delivery-request",
            title = "Delivery Request",
            responseCount = 6,
            updatedDate = "May 28",
            status = FormStatus.DRAFT,
            imageUrl = "https://picsum.photos/seed/delivery-request/160/160"
        ),
        FormSummary(
            id = "customer-satisfaction-survey",
            title = "Customer Satisfaction Survey",
            responseCount = 18,
            updatedDate = "May 20",
            status = FormStatus.ACTIVE,
            imageUrl = "https://picsum.photos/seed/customer-satisfaction-survey/160/160"
        )
    )
}
