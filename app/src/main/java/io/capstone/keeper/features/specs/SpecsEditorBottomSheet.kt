package io.capstone.keeper.features.specs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import io.capstone.keeper.databinding.FragmentEditorSpecificationBinding
import io.capstone.keeper.features.shared.components.BaseBottomSheet

class SpecsEditorBottomSheet(manager: FragmentManager): BaseBottomSheet(manager) {
    private var _binding: FragmentEditorSpecificationBinding? = null
    private var requestKey = REQUEST_KEY_CREATE

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditorSpecificationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.let {
            requestKey = REQUEST_KEY_UPDATE

            it.getString(EXTRA_KEY)?.let { key ->
                binding.nameTextInput.setText(key)
            }
            it.getString(EXTRA_VALUE)?.let { value ->
                binding.valueTextInput.setText(value)
            }
        }

        binding.actionButton.setOnClickListener {
            val key = binding.nameTextInput.text.toString()
            val value = binding.valueTextInput.text.toString()

            setFragmentResult(requestKey,
                bundleOf(EXTRA_KEY to key, EXTRA_VALUE to value))
            this.dismiss()
        }
    }

    companion object {
        const val REQUEST_KEY_CREATE = "request:create"
        const val REQUEST_KEY_UPDATE = "request:update"

        const val EXTRA_KEY = "extra:key"
        const val EXTRA_VALUE = "extra:value"
        const val EXTRA_SPECIFICATION = "extra:specs"
    }
}