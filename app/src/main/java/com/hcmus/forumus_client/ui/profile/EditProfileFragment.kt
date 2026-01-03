package com.hcmus.forumus_client.ui.profile

import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import coil.load
import com.hcmus.forumus_client.R
import com.hcmus.forumus_client.data.model.UserRole
import com.hcmus.forumus_client.data.model.UserStatus
import com.hcmus.forumus_client.databinding.FragmentEditProfileBinding
import com.hcmus.forumus_client.ui.main.MainSharedViewModel
import kotlinx.coroutines.launch
import kotlin.getValue

//class EditProfileFragment : Fragment() {
//
//    private var _binding: FragmentEditProfileBinding? = null
//    private val binding get() = _binding!!
//
//    private val vm: EditProfileViewModel by viewModels()
//    private val mainSharedViewModel: MainSharedViewModel by activityViewModels()
//    private val navController by lazy { findNavController() }
//
//    private val pickAvatarLauncher =
//        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
//            if (uri != null) {
//                vm.onPickAvatar(uri)
//
//                // Preview ngay (Option B: chưa upload)
//                binding.avatarImageView.load(uri) {
//                    placeholder(R.drawable.default_avatar)
//                    error(R.drawable.default_avatar)
//                }
//
//                Toast.makeText(requireContext(), "Selected new avatar (will upload when Save)", Toast.LENGTH_SHORT).show()
//            }
//        }
//
//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
//        return binding.root
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        setupHeaderActions()
//        setupActions()
//        observeState()
//
//        vm.loadCurrentUser()
//    }
//
//    private fun setupHeaderActions() {
//        binding.topAppBar.setNavigationOnClickListener {
//            navController.popBackStack()
//        }
//    }
//
//    private fun setupActions() {
//
//        binding.fullnameInputLayout.setEndIconOnClickListener {
//            binding.fullnameEditText.isEnabled = true
//            binding.fullnameEditText.requestFocus()
//            binding.fullnameEditText.setSelection(binding.fullnameEditText.text?.length ?: 0)
//        }
//
//        // Upload button chỉ mở picker
//        binding.uploadButton.setOnClickListener {
//            pickAvatarLauncher.launch("image/*")
//        }
//
//        // click avatar cũng mở picker
//        binding.avatarImageView.setOnClickListener {
//            pickAvatarLauncher.launch("image/*")
//        }
//
//        binding.discardButton.setOnClickListener {
//            navController.popBackStack()
//        }
//
//        binding.saveButton.setOnClickListener {
//            val newFullName = binding.fullnameEditText.text?.toString()?.trim().orEmpty()
//            if (newFullName.isBlank()) {
//                Toast.makeText(requireContext(), "Full name cannot be empty", Toast.LENGTH_SHORT).show()
//                return@setOnClickListener
//            }
//
//            vm.saveProfile(newFullName) {
//                Toast.makeText(requireContext(), "Profile saved", Toast.LENGTH_SHORT).show()
//                navController.popBackStack()
//            }
//        }
//    }
//
//    private fun observeState() {
//        viewLifecycleOwner.lifecycleScope.launch {
//            vm.uiState.collect { state ->
//                state.error?.let {
//                    Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
//                    vm.clearError()
//                }
//
//                val user = state.user
//                if (user != null) {
//                    binding.fullnameEditText.setText(user.fullName)
//
//                    binding.emailText.text = user.email
//
//                    binding.roleChip.text = user.role.name.replace('_', ' ')
//                    binding.statusChip.text = user.status.name.replace('_', ' ')
//
//                    applyRoleChipStyle(user.role)
//                    applyStatusChipStyle(user.status)
//
//                    // Nếu chưa chọn preview thì load từ URL
//                    if (state.avatarPreviewUri == null) {
//                        binding.avatarImageView.load(user.profilePictureUrl) {
//                            placeholder(R.drawable.default_avatar)
//                            error(R.drawable.default_avatar)
//                        }
//                    }
//                }
//
//                // Disable nút khi đang save (vì save có thể upload)
//                val enabled = !state.isSaving
//                binding.uploadButton.isEnabled = enabled
//                binding.saveButton.isEnabled = enabled
//                binding.discardButton.isEnabled = enabled
//                binding.fullnameInputLayout.isEnabled = enabled
//
//                binding.saveButton.text = if (state.isSaving) "Saving..." else "Save"
//            }
//        }
//    }
//
//    private fun applyRoleChipStyle(role: UserRole) {
//        val (textColorRes, bgColorRes) = when (role) {
//            UserRole.STUDENT ->
//                R.color.role_student to R.color.role_student_tonal   // xanh dương
//
//            UserRole.TEACHER ->
//                R.color.role_teacher to R.color.role_teacher_tonal   // tím
//
//            UserRole.ADMIN ->
//                R.color.role_admin to R.color.role_admin_tonal       // vàng
//
//            else ->
//                R.color.text_primary to R.color.neutral_tonal
//        }
//
//        val textColor = ContextCompat.getColor(requireContext(), textColorRes)
//        val bgColor = ContextCompat.getColor(requireContext(), bgColorRes)
//
//        binding.roleChip.setTextColor(textColor)
//        binding.roleChip.chipBackgroundColor = ColorStateList.valueOf(bgColor)
//        binding.roleChip.chipStrokeColor =
//            ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.stroke))
//        binding.roleChip.setChipIconTint(ColorStateList.valueOf(textColor))
//    }
//
//    private fun applyStatusChipStyle(status: UserStatus) {
//        val (textColorRes, bgColorRes) = when (status) {
//            UserStatus.BANNED ->
//                R.color.status_banned to R.color.status_banned_tonal      // đỏ
//
//            UserStatus.WARNED ->
//                R.color.status_warned to R.color.status_warned_tonal      // vàng
//
//            UserStatus.REMINDED ->
//                R.color.status_reminded to R.color.status_reminded_tonal  // xanh dương
//
//            else ->
//                R.color.status_normal to R.color.status_normal_tonal      // xanh lá (normal/active)
//        }
//
//        val textColor = ContextCompat.getColor(requireContext(), textColorRes)
//        val bgColor = ContextCompat.getColor(requireContext(), bgColorRes)
//
//        binding.statusChip.setTextColor(textColor)
//        binding.statusChip.chipBackgroundColor = ColorStateList.valueOf(bgColor)
//        binding.statusChip.chipStrokeColor =
//            ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.stroke))
//        binding.statusChip.setChipIconTint(ColorStateList.valueOf(textColor))
//    }
//
//    override fun onDestroyView() {
//        _binding = null
//        super.onDestroyView()
//    }
//}

class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    private val vm: EditProfileViewModel by viewModels()
    private val mainSharedViewModel: MainSharedViewModel by activityViewModels()
    private val navController by lazy { findNavController() }

    private var hasBoundOnce = false

    private val pickAvatarLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                vm.onPickAvatar(uri)
                binding.avatarImageView.load(uri) {
                    placeholder(R.drawable.default_avatar)
                    error(R.drawable.default_avatar)
                }
                Toast.makeText(requireContext(), "Selected new avatar (will upload when Save)", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupHeaderActions()
        setupActions()
        observeMainShared()
        observeEditState()

        mainSharedViewModel.loadCurrentUser()
    }

    private fun observeMainShared() {
        mainSharedViewModel.currentUser.observe(viewLifecycleOwner) { user ->
            vm.setUser(user)

            // Bind UI 1 lần khi user về (tránh setText lặp khi đang gõ)
            if (user != null && !hasBoundOnce) {
                hasBoundOnce = true
                binding.fullnameEditText.setText(user.fullName)
                binding.emailText.text = user.email

                binding.roleChip.text = user.role.name.replace('_', ' ')
                binding.statusChip.text = user.status.name.replace('_', ' ')
                applyRoleChipStyle(user.role)
                applyStatusChipStyle(user.status)

                binding.avatarImageView.load(user.profilePictureUrl) {
                    placeholder(R.drawable.default_avatar)
                    error(R.drawable.default_avatar)
                }
            }
        }

        mainSharedViewModel.error.observe(viewLifecycleOwner) { err ->
            if (!err.isNullOrBlank()) Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeEditState() {
        viewLifecycleOwner.lifecycleScope.launch {
            vm.uiState.collect { state ->
                state.error?.let {
                    Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                    vm.clearError()
                }

                // Disable khi saving
                val enabled = !state.isSaving
                binding.uploadButton.isEnabled = enabled
                binding.saveButton.isEnabled = enabled
                binding.discardButton.isEnabled = enabled
                binding.fullnameInputLayout.isEnabled = enabled
                binding.saveButton.text = if (state.isSaving) "Saving..." else "Save"

                // Nếu chưa chọn preview mà user thay đổi avatarUrl (sau save) thì load lại
                val user = state.user
                if (user != null && state.avatarPreviewUri == null) {
                    binding.avatarImageView.load(user.profilePictureUrl) {
                        placeholder(R.drawable.default_avatar)
                        error(R.drawable.default_avatar)
                    }
                }
            }
        }
    }

    private fun setupHeaderActions() {
        binding.topAppBar.setNavigationOnClickListener { navController.popBackStack() }
    }

    private fun setupActions() {
        binding.fullnameInputLayout.setEndIconOnClickListener {
            binding.fullnameEditText.isEnabled = true
            binding.fullnameEditText.requestFocus()
            binding.fullnameEditText.setSelection(binding.fullnameEditText.text?.length ?: 0)
        }

        binding.uploadButton.setOnClickListener { pickAvatarLauncher.launch("image/*") }
        binding.avatarImageView.setOnClickListener { pickAvatarLauncher.launch("image/*") }

        binding.discardButton.setOnClickListener { navController.popBackStack() }

        binding.saveButton.setOnClickListener {
            val newFullName = binding.fullnameEditText.text?.toString()?.trim().orEmpty()
            if (newFullName.isBlank()) {
                Toast.makeText(requireContext(), "Full name cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            vm.saveProfile(newFullName) { updatedUser ->
                // cập nhật user chung cho toàn app
                mainSharedViewModel.setCurrentUser(updatedUser)
                // hoặc nếu muốn chắc chắn lấy từ server:
                // mainSharedViewModel.refreshCurrentUser()

                Toast.makeText(requireContext(), "Profile saved", Toast.LENGTH_SHORT).show()
                navController.popBackStack()
            }
        }
    }

    private fun applyRoleChipStyle(role: UserRole) {
        val (textColorRes, bgColorRes) = when (role) {
            UserRole.STUDENT ->
                R.color.role_student to R.color.role_student_tonal   // xanh dương

            UserRole.TEACHER ->
                R.color.role_teacher to R.color.role_teacher_tonal   // tím

            UserRole.ADMIN ->
                R.color.role_admin to R.color.role_admin_tonal       // vàng

            else ->
                R.color.text_primary to R.color.neutral_tonal
        }

        val textColor = ContextCompat.getColor(requireContext(), textColorRes)
        val bgColor = ContextCompat.getColor(requireContext(), bgColorRes)

        binding.roleChip.setTextColor(textColor)
        binding.roleChip.chipBackgroundColor = ColorStateList.valueOf(bgColor)
        binding.roleChip.chipStrokeColor =
            ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.stroke))
        binding.roleChip.setChipIconTint(ColorStateList.valueOf(textColor))
    }

    private fun applyStatusChipStyle(status: UserStatus) {
        val (textColorRes, bgColorRes) = when (status) {
            UserStatus.BANNED ->
                R.color.status_banned to R.color.status_banned_tonal      // đỏ

            UserStatus.WARNED ->
                R.color.status_warned to R.color.status_warned_tonal      // vàng

            UserStatus.REMINDED ->
                R.color.status_reminded to R.color.status_reminded_tonal  // xanh dương

            else ->
                R.color.status_normal to R.color.status_normal_tonal      // xanh lá
        }

        val textColor = ContextCompat.getColor(requireContext(), textColorRes)
        val bgColor = ContextCompat.getColor(requireContext(), bgColorRes)

        binding.statusChip.setTextColor(textColor)
        binding.statusChip.chipBackgroundColor = ColorStateList.valueOf(bgColor)
        binding.statusChip.chipStrokeColor =
            ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.stroke))
        binding.statusChip.setChipIconTint(ColorStateList.valueOf(textColor))
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
