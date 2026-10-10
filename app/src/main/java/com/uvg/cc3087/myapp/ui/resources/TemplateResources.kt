package com.uvg.cc3087.myapp.ui.resources

import androidx.annotation.StringRes
import com.uvg.cc3087.myapp.R
import com.uvg.cc3087.myapp.data.model.TemplateType

// los recursos se resuelven en la ui para mantener el idioma fuera del viewmodel
@get:StringRes
val TemplateType.titleResId: Int
    get() = when (this) {
        TemplateType.JOB_APPLICATION -> R.string.template_job_application
        TemplateType.ORDER_FORM -> R.string.template_order_form
        TemplateType.EVENT_RSVP -> R.string.template_event_rsvp
        TemplateType.FEEDBACK -> R.string.template_feedback
    }

@get:StringRes
val TemplateType.subtitleResId: Int
    get() = when (this) {
        TemplateType.JOB_APPLICATION -> R.string.template_job_application_subtitle
        TemplateType.ORDER_FORM -> R.string.template_order_form_subtitle
        TemplateType.EVENT_RSVP -> R.string.template_event_rsvp_subtitle
        TemplateType.FEEDBACK -> R.string.template_feedback_subtitle
    }
