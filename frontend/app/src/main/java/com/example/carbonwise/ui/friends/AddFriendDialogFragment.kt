package com.example.carbonwise.ui.friends

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.example.carbonwise.databinding.DialogAddFriendBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AddFriendDialogFragment : BottomSheetDialogFragment() {

    private var _binding: DialogAddFriendBinding? = null
    private val binding get() = _binding!!

    private lateinit var friendsViewModel: FriendsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = DialogAddFriendBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        friendsViewModel = ViewModelProvider(requireActivity())[FriendsViewModel::class.java]

        friendsViewModel.userFriendCode.observe(viewLifecycleOwner) { friendCode ->
            binding.textFriendCode.text = friendCode
        }

        binding.imageClipboard.setOnClickListener {
            val codeText = binding.textFriendCode.text.toString()
            Log.d("Clipboard", "Copying friend code: $codeText")
            if (codeText.isNotEmpty()) {
                val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Friend Code", codeText)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(requireContext(), "Friend code copied!", Toast.LENGTH_SHORT).show()
            }
        }

        binding.buttonAddFriend.setOnClickListener {
            val friendCode = binding.editFriendCode.text.toString().trim()
            if (friendCode.isNotEmpty()) {
                friendsViewModel.sendFriendRequest(friendCode)
                hideKeyboardAndClearFocus()
                binding.editFriendCode.text.clear()
                Toast.makeText(requireContext(), "Friend request sent!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Enter a valid friend code", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.editFriendCode.requestFocus()
        binding.editFriendCode.postDelayed({
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.editFriendCode, InputMethodManager.SHOW_IMPLICIT)
        }, 100)
    }

    private fun hideKeyboardAndClearFocus() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.editFriendCode.windowToken, 0)
        binding.editFriendCode.clearFocus()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
