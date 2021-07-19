package io.capstone.keeper.features.scan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import io.capstone.keeper.databinding.FragmentOptionsScanBinding
import io.capstone.keeper.features.shared.components.BaseFragment

class ScanOptionsFragment: BaseFragment() {
    private var _binding: FragmentOptionsScanBinding? = null

    private val binding get() = _binding!!
    private val viewModel: ScanViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOptionsScanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

}