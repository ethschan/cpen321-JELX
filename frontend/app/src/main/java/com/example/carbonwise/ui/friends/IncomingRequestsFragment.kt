package com.example.carbonwise.ui.friends

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.carbonwise.databinding.FragmentIncomingRequestsBinding

class IncomingRequestsFragment : Fragment() {

    private var _binding: FragmentIncomingRequestsBinding? = null
    private val binding get() = _binding!!

    private lateinit var friendsViewModel: FriendsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        friendsViewModel = ViewModelProvider(requireActivity())[FriendsViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIncomingRequestsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (_binding == null) return
        super.onViewCreated(view, savedInstanceState)

        val adapter = IncomingRequestsAdapter(
            mutableListOf(),
            onAccept = { friendRequest -> friendsViewModel.acceptFriendRequest(friendRequest.user_uuid) },
            onReject = { friendRequest -> friendsViewModel.rejectFriendRequest(friendRequest.user_uuid) }
        )

        binding.recyclerIncomingRequests.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerIncomingRequests.adapter = adapter

        friendsViewModel.incomingRequests.observe(viewLifecycleOwner) { incoming ->
            friendsViewModel.outgoingRequests.observe(viewLifecycleOwner) { outgoing ->

                val incomingTagged = incoming
                    .filterNotNull()
                    .onEach { it.isOutgoing = false }

                val outgoingTagged = outgoing
                    .filterNotNull()
                    .onEach { it.isOutgoing = true }

                val combined = incomingTagged + outgoingTagged
                adapter.updateRequests(combined)
            }
        }


        friendsViewModel.fetchFriendRequests()
        friendsViewModel.fetchOutgoingRequests()

    }

    override fun onResume() {
        super.onResume()
        friendsViewModel.fetchFriendRequests()
        friendsViewModel.fetchUserFriendCode()
        friendsViewModel.fetchOutgoingRequests()

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): IncomingRequestsFragment {
            return IncomingRequestsFragment()
        }
    }
}
