package org.gnucash.android.ui.syncable

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import org.gnucash.android.R
import org.gnucash.android.databinding.FragmentSyncableLinkBinding

const val TAG = "SyncableLinkFragment"

class SyncableLinkFragment : Fragment() {

    companion object {
        fun newInstance() = SyncableLinkFragment()
    }

    private lateinit var viewModel: SyncableLinkViewModel
    private var binding: FragmentSyncableLinkBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(SyncableLinkViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSyncableLinkBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.connectButton?.setOnClickListener {
            binding?.connectButton?.isEnabled = false
            lifecycleScope.launch {
                val connection = SyncableConnection("10.66.1.16", 8443)
                withContext(Dispatchers.IO) {
                    val accounts = connection.getAccountTree()
                    SyncableDatabaseWriter().writeAccountTree(accounts)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}