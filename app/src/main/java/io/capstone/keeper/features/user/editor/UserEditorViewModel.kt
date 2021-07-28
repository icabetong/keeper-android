package io.capstone.keeper.features.user.editor

import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import io.capstone.keeper.components.persistence.UserProperties
import io.capstone.keeper.features.core.backend.Operation
import io.capstone.keeper.features.shared.components.BaseViewModel
import io.capstone.keeper.features.user.User
import io.capstone.keeper.features.user.UserRepository
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class UserEditorViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val firebaseAuth: FirebaseAuth,
    private val userProperties: UserProperties
): BaseViewModel() {

    private val _reauthentication = Channel<Operation<Nothing>>(Channel.BUFFERED)
    val reauthentication = _reauthentication.receiveAsFlow()

    fun reauthenticate(password: String?) = viewModelScope.launch(IO) {
        val email = userProperties.email
        if (email.isNullOrBlank() || password.isNullOrBlank()) {
            _reauthentication.send(Operation.Success(null))
            return@launch
        }

        val credential = EmailAuthProvider.getCredential(email, password)
        firebaseAuth.currentUser?.reauthenticate(credential)
            ?.addOnCompleteListener {
                this.launch {
                    if (it.isSuccessful)
                        _reauthentication.send(Operation.Success(null))
                    else _reauthentication.send(Operation.Error(it.exception))
                }
            }?.await()
    }

    var user = User()

    fun create() = viewModelScope.launch(IO) {
        userRepository.create(user)
    }
    fun update() = viewModelScope.launch(IO) {
        userRepository.update(user)
    }
}