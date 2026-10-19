package com.uvg.cc3087.myapp.data

import com.uvg.cc3087.myapp.data.model.FormTemplate
import com.uvg.cc3087.myapp.data.model.TemplateType

object TemplateSampleData {
    // son las mismas cuatro tarjetas que ya teníamos, ahora con ids estables
    val templates = listOf(
        FormTemplate("job-application", TemplateType.JOB_APPLICATION),
        FormTemplate("order-form", TemplateType.ORDER_FORM),
        FormTemplate("event-rsvp", TemplateType.EVENT_RSVP),
        FormTemplate("feedback", TemplateType.FEEDBACK)
    )
}
