package com.uvg.cc3087.myapp.ui.resources

import com.uvg.cc3087.myapp.R
import com.uvg.cc3087.myapp.data.model.RequestStatus

val RequestStatus.labelRes: Int get() = when (this) {
    RequestStatus.PENDING -> R.string.request_status_pending
    RequestStatus.IN_PROGRESS -> R.string.request_status_in_progress
    RequestStatus.COMPLETED -> R.string.request_status_completed
}
