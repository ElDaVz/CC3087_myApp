package com.uvg.cc3087.myapp.data

import com.uvg.cc3087.myapp.data.model.FormStatus
import com.uvg.cc3087.myapp.data.model.FormSummary

// datos locales para practicar la lista antes de conectarla a una fuente real :D
object FormSampleData {
    val forms = listOf(
        FormSummary(
            id = "bakery-custom-order",
            title = "Pedido personalizado de pastelería",
            responseCount = 12,
            updatedDate = "18 jul",
            status = FormStatus.ACTIVE,
            imageUrl = "https://picsum.photos/seed/bakery-custom-order/160/160"
        ),
        FormSummary(
            id = "birthday-cake-request",
            title = "Solicitud de pastel de cumpleaños",
            responseCount = 5,
            updatedDate = "14 jul",
            status = FormStatus.ACTIVE,
            imageUrl = "https://picsum.photos/seed/birthday-cake-request/160/160"
        ),
        FormSummary(
            id = "wholesale-inquiry",
            title = "Consulta de venta mayorista",
            responseCount = 2,
            updatedDate = "30 jun",
            status = FormStatus.DRAFT,
            imageUrl = "https://picsum.photos/seed/wholesale-inquiry/160/160"
        ),
        FormSummary(
            id = "wedding-cake-consultation",
            title = "Consulta para pastel de boda",
            responseCount = 8,
            updatedDate = "27 jun",
            status = FormStatus.ACTIVE,
            imageUrl = "https://picsum.photos/seed/wedding-cake-consultation/160/160"
        ),
        FormSummary(
            id = "catering-order",
            title = "Pedido de catering",
            responseCount = 15,
            updatedDate = "22 jun",
            status = FormStatus.ACTIVE,
            imageUrl = "https://picsum.photos/seed/catering-order/160/160"
        ),
        FormSummary(
            id = "seasonal-menu-feedback",
            title = "Opinión sobre el menú de temporada",
            responseCount = 21,
            updatedDate = "16 jun",
            status = FormStatus.ACTIVE,
            imageUrl = "https://picsum.photos/seed/seasonal-menu-feedback/160/160"
        ),
        FormSummary(
            id = "custom-cupcake-box",
            title = "Caja personalizada de cupcakes",
            responseCount = 4,
            updatedDate = "10 jun",
            status = FormStatus.DRAFT,
            imageUrl = "https://picsum.photos/seed/custom-cupcake-box/160/160"
        ),
        FormSummary(
            id = "corporate-event-order",
            title = "Pedido para evento empresarial",
            responseCount = 9,
            updatedDate = "4 jun",
            status = FormStatus.ACTIVE,
            imageUrl = "https://picsum.photos/seed/corporate-event-order/160/160"
        ),
        FormSummary(
            id = "delivery-request",
            title = "Solicitud de entrega",
            responseCount = 6,
            updatedDate = "28 may",
            status = FormStatus.DRAFT,
            imageUrl = "https://picsum.photos/seed/delivery-request/160/160"
        ),
        FormSummary(
            id = "customer-satisfaction-survey",
            title = "Encuesta de satisfacción",
            responseCount = 18,
            updatedDate = "20 may",
            status = FormStatus.ACTIVE,
            imageUrl = "https://picsum.photos/seed/customer-satisfaction-survey/160/160"
        )
    )
}
