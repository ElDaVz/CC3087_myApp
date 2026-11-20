package com.uvg.cc3087.myapp.data.model

/**
 * Optional profile contacts use the same basic shape as email form answers.
 * Blank is allowed: it means the owner has not provided that address.
 */
object ContactEmail {
    private val FORMAT = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

    fun isValid(value: String): Boolean = value.isBlank() || FORMAT.matches(value)
}
