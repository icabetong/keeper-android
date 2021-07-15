package io.capstone.keeper.features.shared.components

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.view.View
import androidx.annotation.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentResultListener
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.discord.panels.OverlappingPanelsLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.color.MaterialColors
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialContainerTransform
import io.capstone.keeper.R
import me.saket.cascade.CascadePopupMenu
import me.saket.cascade.overrideOverflowMenu

abstract class BaseFragment: Fragment() {

    protected fun registerForFragmentResult(keys: Array<String>, listener: FragmentResultListener) {
        keys.forEach {
            childFragmentManager.setFragmentResultListener(it, viewLifecycleOwner, listener)
        }
    }

    protected fun setupToolbar(toolbar: MaterialToolbar,
                               navigation: () -> Unit,
                               @StringRes id: Int = 0,
                               @DrawableRes icon: Int = R.drawable.ic_hero_arrow_left,
                               @MenuRes menu: Int = 0,
                               menuListener: ((id: Int) -> Unit)? = null){
        with(toolbar) {
            if (id != 0) setTitle(id)
            if (menu != 0) inflateMenu(menu)
            setNavigationIcon(icon)
            setNavigationOnClickListener { navigation() }
            overrideOverflowMenu(::customPopupProvider)
            setOnMenuItemClickListener{
                menuListener?.let { listener -> listener(it.itemId) }
                true
            }
        }
    }

    protected fun getParentView(): View? {
        return parentFragment?.parentFragment?.view
    }

    protected fun getOverlappingPanelLayout(): OverlappingPanelsLayout {
        return getParentView()?.findViewById(R.id.overlappingPanels) as OverlappingPanelsLayout
    }

    protected fun createSnackbar(@StringRes textRes: Int, length: Int = Snackbar.LENGTH_SHORT): Snackbar {
        return Snackbar.make(requireView(), textRes, length).apply {
            show()
        }
    }

    protected fun setSystemBarColor(@ColorRes colorId: Int) {
        with(requireActivity().window) {
            statusBarColor = ContextCompat.getColor(this.context, colorId)

            /**
             *  Check if the device supports windowLightNavigationBar or
             *  if the device is in night mode then apply the same color
             *  of the fragment to the navigationBar
             */
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1 &&
                context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_YES ==
                Configuration.UI_MODE_NIGHT_YES) {
                navigationBarColor = ContextCompat.getColor(this.context, colorId)
            }
        }
    }

    protected fun buildContainerTransform(@IdRes id: Int = R.id.navHostFragment) =
        MaterialContainerTransform().apply {
            drawingViewId = id
            duration = TRANSITION_DURATION
            scrimColor = Color.TRANSPARENT
            fadeMode = MaterialContainerTransform.FADE_MODE_OUT
            interpolator = FastOutSlowInInterpolator()
            setAllContainerColors(
                MaterialColors.getColor(requireContext(), R.attr.colorSurface,
                ContextCompat.getColor(requireContext(), R.color.keeper_surface)))
        }

    private fun customPopupProvider(context: Context, anchor: View): CascadePopupMenu {
        return CascadePopupMenu(context, anchor,
            styler = CascadePopupMenu.Styler(
                background = {
                    ContextCompat.getDrawable(context, R.drawable.shape_cascade_background)
                }
            ))
    }

    companion object {
        const val TRANSITION_DURATION = 300L
        const val TRANSITION_NAME_ROOT = "transition:root:"
    }
}