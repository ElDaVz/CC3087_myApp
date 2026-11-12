package com.uvg.cc3087.myapp.ui.resources

import androidx.annotation.StringRes
import com.uvg.cc3087.myapp.R
import com.uvg.cc3087.myapp.data.repository.PublicationError

@get:StringRes
val PublicationError.messageRes: Int
    get() = when (this) {
        PublicationError.SIGN_IN_REQUIRED -> R.string.publication_sign_in_required
        PublicationError.FORM_NOT_FOUND -> R.string.publication_form_not_found
        PublicationError.BACKEND_NOT_CONFIGURED -> R.string.publication_backend_not_configured
        PublicationError.SETUP_REQUIRED -> R.string.publication_setup_required
        PublicationError.UNAVAILABLE -> R.string.publication_unavailable
        PublicationError.INVALID_FORM -> R.string.publication_invalid_form
        PublicationError.TOO_MANY_FIELDS -> R.string.publication_too_many_fields
        PublicationError.TEXT_TOO_LONG -> R.string.publication_text_too_long
        PublicationError.COVER_PREPARATION_FAILED -> R.string.publication_cover_preparation_failed
        PublicationError.COVER_NOT_CONFIGURED -> R.string.publication_cover_not_configured
        PublicationError.COVER_UPLOAD_FAILED -> R.string.publication_cover_upload_failed
        PublicationError.DRAFT_CHANGED -> R.string.publication_draft_changed
        PublicationError.PUBLISH_FAILED -> R.string.publication_publish_failed
        PublicationError.ARCHIVE_FAILED -> R.string.publication_archive_failed
    }
