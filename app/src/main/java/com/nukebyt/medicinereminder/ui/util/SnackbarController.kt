package com.nukebyt.medicinereminder.ui.util

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

data class SnackbarRequest(
    val message: String,
    val actionLabel: String?,
    /** Completes with true if the user pressed the action, false if it was dismissed or timed out. */
    val result: CompletableDeferred<Boolean>,
)

/**
 * App-wide snackbar queue, shown by the root Scaffold, so a message survives navigating away from
 * the screen that raised it ("Metformin saved. Next reminder at 8:00 am.", "Deleted. Undo").
 */
@Singleton
class SnackbarController @Inject constructor() {
    private val channel = Channel<SnackbarRequest>(Channel.UNLIMITED)
    val requests = channel.receiveAsFlow()

    /** Suspends until the snackbar is gone. Returns true if the action was pressed. */
    suspend fun show(message: String, actionLabel: String? = null): Boolean {
        val result = CompletableDeferred<Boolean>()
        channel.send(SnackbarRequest(message, actionLabel, result))
        return result.await()
    }

    /** Fire and forget version for messages without an action. */
    fun post(message: String) {
        channel.trySend(SnackbarRequest(message, null, CompletableDeferred()))
    }
}
