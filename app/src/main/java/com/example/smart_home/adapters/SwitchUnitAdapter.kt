package com.example.smart_home.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.smart_home.R
import com.example.smart_home.models.MultiSwitch

class SwitchUnitAdapter(
    private var switches: List<MultiSwitch.SwitchState>,
    private val onToggle: (Int) -> Unit
) : RecyclerView.Adapter<SwitchUnitAdapter.SwitchViewHolder>() {

    fun updateSwitches(newSwitches: List<MultiSwitch.SwitchState>) {
        this.switches = newSwitches
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SwitchViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_switch_unit, parent, false)
        return SwitchViewHolder(view)
    }

    override fun onBindViewHolder(holder: SwitchViewHolder, position: Int) {
        holder.bind(switches[position], position, onToggle)
    }

    override fun getItemCount(): Int = switches.size

    class SwitchViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val name: TextView = view.findViewById(R.id.switch_name)
        private val toggle: SwitchCompat = view.findViewById(R.id.switch_toggle)

        fun bind(state: MultiSwitch.SwitchState, position: Int, onToggle: (Int) -> Unit) {
            name.text = "Switch ${state.switchNumber}"
            
            toggle.setOnCheckedChangeListener(null)
            toggle.isChecked = state.state == "ON"
            
            toggle.setOnCheckedChangeListener { _, _ ->
                onToggle(position)
            }
        }
    }
}
