package ai.a2i2.locationtracking.ui.history

import ai.a2i2.locationtracking.databinding.LocationHistoryFragmentBinding
import ai.a2i2.locationtracking.viewmodels.LocationHistoryViewModel
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider

class LocationHistoryFragment : Fragment() {

    private lateinit var binding: LocationHistoryFragmentBinding

    companion object {
        fun newInstance() = LocationHistoryFragment()
    }

    private lateinit var viewModel: LocationHistoryViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = LocationHistoryFragmentBinding.inflate(layoutInflater)
        viewModel = ViewModelProvider(this).get(LocationHistoryViewModel::class.java)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.locationsLiveData.observe(viewLifecycleOwner, { locations ->
            val adapter =
                ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, locations)
            binding.locations.adapter = adapter
        })
    }
}