package io.capstone.keeper.features.department

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import io.capstone.keeper.databinding.LayoutItemDepartmentBinding
import io.capstone.keeper.features.shared.components.BasePagingAdapter
import io.capstone.keeper.features.shared.components.BaseViewHolder

class DepartmentAdapter(
    private val onItemActionListener: OnItemActionListener
): BasePagingAdapter<Department, DepartmentAdapter.DepartmentViewHolder>(Companion) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DepartmentViewHolder {
        val binding = LayoutItemDepartmentBinding.inflate(LayoutInflater.from(parent.context),
            parent, false)
        return DepartmentViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: DepartmentViewHolder, position: Int) {
        holder.onBind(getItem(position))
    }

    inner class DepartmentViewHolder(itemView: View): BaseViewHolder<Department>(itemView) {
        private val binding = LayoutItemDepartmentBinding.bind(itemView)

        override fun onBind(data: Department?) {
            binding.titleTextView.text = data?.name
            binding.bodyTextView.text = data?.managerSSN?.name

            binding.root.setOnClickListener {
                onItemActionListener.onActionPerformed(data, Action.SELECT)
            }
        }
    }

    companion object : DiffUtil.ItemCallback<Department>() {
        override fun areItemsTheSame(oldItem: Department, newItem: Department): Boolean {
            return oldItem.departmentId == newItem.departmentId
        }

        override fun areContentsTheSame(oldItem: Department, newItem: Department): Boolean {
            return oldItem == newItem
        }

    }
}