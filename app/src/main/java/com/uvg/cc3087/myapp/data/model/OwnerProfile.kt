package com.uvg.cc3087.myapp.data.model

/**
 * Optional contact details for one account. The login email stays in the auth
 * session: these addresses are independent and are never used to sign in.
 * Business data is published only on explicit republish; emails stay private.
 */
data class OwnerProfile(
    val ownerId: String,
    val personalName: String = "",
    val personalEmail: String = "",
    val businessName: String = "",
    val businessEmail: String = "",
    val logoPath: String? = null,
    val revision: Long = 0,
    val syncedRevision: Long = 0
) {
    val hasBusiness: Boolean get() = businessName.isNotBlank() || logoPath != null
}
