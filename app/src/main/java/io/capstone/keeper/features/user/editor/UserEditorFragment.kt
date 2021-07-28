package io.capstone.keeper.features.user.editor

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.setActionButtonEnabled
import com.afollestad.materialdialogs.input.getInputField
import com.afollestad.materialdialogs.input.input
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import dagger.hilt.android.AndroidEntryPoint
import io.capstone.keeper.R
import io.capstone.keeper.components.extensions.setup
import io.capstone.keeper.components.utils.PasswordManager
import io.capstone.keeper.databinding.FragmentEditorUserBinding
import io.capstone.keeper.features.core.backend.Operation
import io.capstone.keeper.features.department.Department
import io.capstone.keeper.features.department.picker.DepartmentPickerBottomSheet
import io.capstone.keeper.features.shared.components.BaseEditorFragment
import io.capstone.keeper.features.user.User
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.concurrent.Executor

@AndroidEntryPoint
class UserEditorFragment: BaseEditorFragment(), FragmentResultListener {
    private var _binding: FragmentEditorUserBinding? = null
    private var controller: NavController? = null
    private var requestKey = REQUEST_KEY_CREATE

    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    private val binding get() = _binding!!
    private val viewModel: UserEditorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = buildContainerTransform()
        sharedElementReturnTransition = buildContainerTransform()

        executor = ContextCompat.getMainExecutor(requireContext())
        biometricPrompt = BiometricPrompt(this, executor, biometricCallback)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditorUserBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.transitionName = TRANSITION_NAME_ROOT

        controller = Navigation.findNavController(view)
        binding.appBar.toolbar.setup(
            titleRes = R.string.title_user_create,
            iconRes = R.drawable.ic_hero_x,
            onNavigationClicked = { controller?.navigateUp() }
        )

        binding.passwordTextInput.setText(PasswordManager.generateRandom(
            isWithLetters = true,
            isWithUppercase = true,
            isWithNumbers = true,
            isWithSpecial = true,
            length = 10
        ))

        arguments?.getParcelable<User>(EXTRA_USER)?.let {
            requestKey = REQUEST_KEY_UPDATE
            viewModel.user = it

            binding.appBar.toolbar.setTitle(R.string.title_user_update)
            binding.root.transitionName = TRANSITION_NAME_ROOT + it.userId
            binding.passwordTextInputLayout.visibility = View.GONE

            binding.firstNameTextInput.setText(it.firstName)
            binding.lastNameTextInput.setText(it.lastName)
            binding.emailTextInput.setText(it.email)
            binding.positionTextInput.setText(it.position)

            it.department?.name?.let { departmentName ->
                binding.departmentTextView.text = departmentName
            }

            binding.readChip.isChecked = it.hasPermission(User.PERMISSION_READ)
            binding.writeChip.isChecked = it.hasPermission(User.PERMISSION_WRITE)
            binding.deleteChip.isChecked = it.hasPermission(User.PERMISSION_DELETE)
            binding.auditChip.isChecked = it.hasPermission(User.PERMISSION_AUDIT)
            binding.administrativeChip.isChecked = it.hasPermission(User.PERMISSION_MANAGE_USERS)
        }

        registerForFragmentResult(
            arrayOf(DepartmentPickerBottomSheet.REQUEST_KEY_PICK),
            this)
    }

    override fun onStart() {
        super.onStart()

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.reauthentication.collect {
                when(it) {
                    is Operation.Error -> {
                        binding.root.isEnabled = true
                        createSnackbar(R.string.error_auth_failed)
                    }
                    is Operation.Success -> {
                        binding.root.isEnabled = true

                        if (requestKey == REQUEST_KEY_UPDATE)
                            viewModel.update()
                        else viewModel.create()
                        controller?.navigateUp()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        binding.departmentTextView.setOnClickListener {
            DepartmentPickerBottomSheet(childFragmentManager)
                .show()
        }

        binding.actionButton.setOnClickListener {
            viewModel.user.firstName = binding.firstNameTextInput.text.toString()
            viewModel.user.lastName = binding.lastNameTextInput.text.toString()
            viewModel.user.position = binding.positionTextInput.text.toString()
            viewModel.user.email = binding.emailTextInput.text.toString()

            if (viewModel.user.firstName.isNullOrBlank()) {
                createSnackbar(R.string.feedback_empty_first_name)
                return@setOnClickListener
            }
            if (viewModel.user.lastName.isNullOrBlank()) {
                createSnackbar(R.string.feedback_empty_last_name)
                return@setOnClickListener
            }
            if (viewModel.user.position.isNullOrBlank()) {
                createSnackbar(R.string.feedback_empty_position)
                return@setOnClickListener
            }
            if (viewModel.user.email.isNullOrBlank()) {
                createSnackbar(R.string.feedback_empty_email)
                return@setOnClickListener
            }
            if (viewModel.user.department == null) {
                createSnackbar(R.string.feedback_empty_department)
                return@setOnClickListener
            }

            promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.authentication_confirm))
                .setSubtitle(getString(R.string.authentication_confirm_summary))
                .setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG
                        or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build()

            biometricPrompt.authenticate(promptInfo)
            return@setOnClickListener
        }
    }

    override fun onFragmentResult(requestKey: String, result: Bundle) {
        when(requestKey) {
            DepartmentPickerBottomSheet.REQUEST_KEY_PICK -> {
                result.getParcelable<Department>(DepartmentPickerBottomSheet.EXTRA_DEPARTMENT)?.let {
                    viewModel.user.department = it.minimize()

                    binding.departmentTextView.text = it.name
                }
            }
        }
    }

    private val biometricCallback = object: BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            super.onAuthenticationError(errorCode, errString)
            when(errorCode) {
                BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL -> {
                    MaterialDialog(requireContext()).show {
                        lifecycleOwner(viewLifecycleOwner)
                        title(R.string.authentication_confirm)
                        message(R.string.authentication_confirm_summary)
                        input(hintRes = R.string.hint_password, waitForPositiveButton = false,
                            inputType = InputType.TYPE_TEXT_VARIATION_PASSWORD) { dialog, text ->

                            val inputField = dialog.getInputField()
                            val passwordIsValid = text.isNotBlank()

                            inputField.error = if (passwordIsValid) null
                            else getString(R.string.error_empty_password)
                            dialog.setActionButtonEnabled(WhichButton.POSITIVE, passwordIsValid)
                        }
                        positiveButton(R.string.button_continue) {
                            val password = it.getInputField().text.toString()

                            viewModel.reauthenticate(password)
                        }
                    }
                }
                BiometricPrompt.ERROR_LOCKOUT -> {
                    MaterialDialog(requireContext()).show {
                        lifecycleOwner(viewLifecycleOwner)
                        title(R.string.error_auth_failed)
                        message(R.string.error_auth_failed_too_many_attempts)
                        positiveButton(android.R.string.ok)
                    }
                }
                BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> {
                    MaterialDialog(requireContext()).show {
                        lifecycleOwner(viewLifecycleOwner)
                        title(R.string.error_auth_failed)
                        message(R.string.error_auth_failed_too_many_attempts_permanent)
                        positiveButton(android.R.string.ok)
                    }
                }
                BiometricPrompt.ERROR_TIMEOUT -> {
                    MaterialDialog(requireContext()).show {
                        lifecycleOwner(viewLifecycleOwner)
                        title(R.string.error_auth_failed)
                        message(R.string.error_auth_failed_timeout)
                        positiveButton(android.R.string.ok)
                    }
                }
                else -> { /** Other error codes; not sure if we need to handle it **/ }
            }
        }

        override fun onAuthenticationFailed() {
            super.onAuthenticationFailed()

            createSnackbar(R.string.error_auth_failed)
        }

        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            super.onAuthenticationSucceeded(result)

            if (requestKey == REQUEST_KEY_UPDATE)
                viewModel.update()
            else viewModel.create()
            controller?.navigateUp()
        }
    }

    companion object {
        const val REQUEST_KEY_CREATE = "request:create"
        const val REQUEST_KEY_UPDATE = "request:update"
        const val EXTRA_USER = "extra:user"
    }
}