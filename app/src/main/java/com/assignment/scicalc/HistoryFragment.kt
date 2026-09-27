package com.assignment.scicalc

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.assignment.scicalc.databinding.FragmentHistoryBinding

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AppViewModel by activityViewModels()
    private val adapter = HistoryAdapter(
        onCopy = { copy(it.result) },
        onDelete = { viewModel.deleteHistory(it.id) }
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.rvHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHistory.adapter = adapter
        viewModel.history.observe(viewLifecycleOwner) { render(it, binding.etSearch.text?.toString().orEmpty()) }
        binding.etSearch.doAfterTextChanged { render(viewModel.history.value.orEmpty(), it?.toString().orEmpty()) }
        binding.btnClearHistory.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.clear_history_q)
                .setMessage(R.string.clear_history_msg)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.clear_history) { _, _ ->
                    viewModel.clearHistory()
                    Toast.makeText(requireContext(), R.string.history_cleared, Toast.LENGTH_SHORT).show()
                }
                .show()
        }
    }

    private fun render(items: List<HistoryEntry>, query: String) {
        val filtered = if (query.isBlank()) items else items.filter {
            it.expression.contains(query, true) || it.result.contains(query, true)
        }
        adapter.submit(filtered)
        binding.emptyState.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        binding.emptyState.setText(if (items.isEmpty()) R.string.history_empty else R.string.history_empty_sub)
    }

    private fun copy(text: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Nexora", text))
        Toast.makeText(requireContext(), R.string.copied, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

private class HistoryAdapter(
    private val onCopy: (HistoryEntry) -> Unit,
    private val onDelete: (HistoryEntry) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.Holder>() {

    private var items: List<HistoryEntry> = emptyList()

    fun submit(next: List<HistoryEntry>) {
        items = next
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_history, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        holder.expr.text = item.expression
        holder.result.text = item.result
        holder.copy.setOnClickListener { onCopy(item) }
        holder.delete.setOnClickListener { onDelete(item) }
    }

    override fun getItemCount(): Int = items.size

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val expr: TextView = view.findViewById(R.id.tvHistExpr)
        val result: TextView = view.findViewById(R.id.tvHistResult)
        val copy: TextView = view.findViewById(R.id.btnCopy)
        val delete: TextView = view.findViewById(R.id.btnDelete)
    }
}
