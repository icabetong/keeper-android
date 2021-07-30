package io.capstone.keeper.features.core.backend

sealed class Response<out T> {

    data class Success<out R>(
        val data: R
    ): Response<R>()

    data class Error<out R>(
        val throwable: Throwable?,
        val action: R? = null
    ): Response<R>()

    enum class Action { CREATE, UPDATE, REMOVE }

    companion object {
        const val QUERY_LIMIT = 15
    }

}