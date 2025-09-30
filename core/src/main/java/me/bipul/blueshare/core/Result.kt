package me.bipul.blueshare.core

import me.bipul.blueshare.core.model.TransferError

/**
 * A generic wrapper for operations that can succeed or fail.
 * Follows functional programming principles for error handling.
 *
 * @param T The type of data on success
 */
sealed class Result<out T> {
    /**
     * Operation succeeded with data
     */
    data class Success<T>(val data: T) : Result<T>()

    /**
     * Operation failed with error
     */
    data class Error(val error: TransferError) : Result<Nothing>()

    /**
     * Maps the success value to a new type
     */
    inline fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
    }

    /**
     * Returns the success value or null
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }

    /**
     * Returns true if the result is a success
     */
    fun isSuccess(): Boolean = this is Success

    /**
     * Returns true if the result is an error
     */
    fun isError(): Boolean = this is Error
}