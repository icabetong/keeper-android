package io.capstone.keeper.features.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.discord.panels.OverlappingPanelsLayout
import io.capstone.keeper.R
import io.capstone.keeper.databinding.FragmentHomeBinding
import io.capstone.keeper.features.shared.components.BaseFragment

class HomeFragment: BaseFragment() {
    private var _binding: FragmentHomeBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar(binding.appBar.toolbar, {
            getOverlappingPanelLayout().openStartPanel()
        }, R.string.activity_home, R.drawable.ic_hero_menu, R.menu.menu_main, { id ->
            when (id) {
                R.id.action_menu -> getOverlappingPanelLayout().openEndPanel()
            }
        })

    }
}