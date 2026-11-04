package com.uvg.cc3087.myapp.data.repository

import com.uvg.cc3087.myapp.data.model.RequestAnswerValue
import com.uvg.cc3087.myapp.data.model.RequestAttachment
import com.uvg.cc3087.myapp.data.model.RequestDetail
import com.uvg.cc3087.myapp.data.model.RequestDetailAnswer
import com.uvg.cc3087.myapp.data.model.toSummary
import java.time.Instant

/** Debug fixtures shared by the normal app journey and the standalone detail demo. */
object DemoRequestRepository : RequestRepository {
    val requests = listOf(
        RequestDetail(
            "demo-form", "demo-response", "Pedido de pastelería", Instant.parse("2026-10-29T16:30:00Z"),
            listOf(
                RequestDetailAnswer("name", "Nombre", RequestAnswerValue.Text("Cliente de ejemplo")),
                RequestDetailAnswer("order", "Pedido", RequestAnswerValue.Text("Pastel de chocolate para 12 personas")),
                RequestDetailAnswer("extras", "Complementos", RequestAnswerValue.Choices(listOf("Velas", "Tarjeta"))),
                RequestDetailAnswer("delivery", "Entrega a domicilio", RequestAnswerValue.Checked(false)),
                RequestDetailAnswer("notes", "Observaciones", RequestAnswerValue.Text("")),
                RequestDetailAnswer("reference", "Referencia", RequestAnswerValue.Attachments(listOf(
                    RequestAttachment("pastel-ejemplo.jpg", "demo/reference")
                )))
            )
        ),
        RequestDetail(
            "demo-form", "demo-response-2", "Pedido de pastelería", Instant.parse("2026-10-29T17:00:00Z"),
            listOf(
                RequestDetailAnswer("name", "Nombre", RequestAnswerValue.Text("Segundo cliente de ejemplo")),
                RequestDetailAnswer("order", "Pedido", RequestAnswerValue.Text("Caja de 6 cupcakes"))
            )
        ),
        RequestDetail(
            "demo-reservation", "demo-response", "Reserva de cita", Instant.parse("2026-10-29T18:00:00Z"),
            listOf(
                RequestDetailAnswer("name", "Nombre", RequestAnswerValue.Text("Cliente de reserva")),
                RequestDetailAnswer("date", "Fecha y hora solicitadas", RequestAnswerValue.Text("30/10/2026, 14:00"))
            )
        )
    )

    override suspend fun getDetail(formId: String, responseId: String) =
        requests.find { it.formId == formId && it.responseId == responseId }

    override suspend fun getRequests() = requests.map { it.toSummary() }
}
