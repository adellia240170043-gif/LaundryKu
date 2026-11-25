package com.laundryku.app.ui.orders

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.laundryku.app.R
import com.laundryku.app.data.model.Order
import com.laundryku.app.data.model.OrderStatus
import com.laundryku.app.data.model.ServiceType
import com.laundryku.app.databinding.FragmentOrderFormBinding
import com.laundryku.app.util.ImageUtils
import com.laundryku.app.util.UiState
import com.laundryku.app.util.formatRupiah
import com.laundryku.app.util.formatWeight
import com.laundryku.app.util.isOnline
import java.io.File

class OrderFormFragment : Fragment() {

    private var _binding: FragmentOrderFormBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OrderViewModel by activityViewModels()
    private val formViewModel: OrderFormViewModel by activityViewModels()

    private var orderId: String = ""
    private var existingOrder: Order? = null
    private var selectedService = ServiceType.CUCI_KERING
    private var selectedStatus = OrderStatus.DITERIMA
    private var isDeleting = false
    private var cameraFilePath: String? = null

    // Hasil kamera
    private val takePicture =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            val path = cameraFilePath
            if (success && path != null) {
                processPhoto(Uri.fromFile(File(path)))
            }
        }

    // Hasil permintaan izin kamera
    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) launchCamera() else showCameraDenied()
        }

    // Hasil pilih foto dari galeri
    private val pickImage =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) processPhoto(uri)
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrderFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        orderId = arguments?.getString("orderId").orEmpty()
        val isEdit = orderId.isNotEmpty()
        existingOrder = if (isEdit) viewModel.findOrder(orderId) else null

        // Mode edit tapi datanya tidak ditemukan: kembali ke daftar
        if (isEdit && existingOrder == null) {
            findNavController().popBackStack()
            return
        }

        binding.tvFormTitle.setText(if (isEdit) R.string.title_edit_order else R.string.title_add_order)
        binding.btnDelete.isVisible = isEdit

        setupDropdowns()

        if (savedInstanceState != null) {
            // Layar diputar: pulihkan pilihan dropdown
            selectedService = ServiceType.fromKey(savedInstanceState.getString(KEY_SERVICE).orEmpty())
            selectedStatus = OrderStatus.fromKey(savedInstanceState.getString(KEY_STATUS).orEmpty())
            isDeleting = savedInstanceState.getBoolean(KEY_DELETING)
            cameraFilePath = savedInstanceState.getString(KEY_CAMERA_PATH)
        } else {
            existingOrder?.let { fillForm(it) }
        }

        // Foto: mulai dari foto pesanan yang ada (kalau form dibuka baru)
        if (savedInstanceState == null || !formViewModel.draftReady) {
            formViewModel.draftPhoto = existingOrder?.photo.orEmpty()
            formViewModel.draftReady = true
        }

        showSelections()
        showPhotoPreview()

        binding.etWeight.doOnTextChanged { _, _, _, _ -> updateTotal() }
        binding.btnTakePhoto.setOnClickListener { onTakePhotoClick() }
        binding.btnPickGallery.setOnClickListener { openGallery() }
        binding.btnRemovePhoto.setOnClickListener {
            formViewModel.draftPhoto = ""
            showPhotoPreview()
        }
        binding.btnSave.setOnClickListener { save() }
        binding.btnDelete.setOnClickListener { confirmDelete() }
        binding.btnCancel.setOnClickListener { findNavController().popBackStack() }

        viewModel.saveState.observe(viewLifecycleOwner) { handleSaveState(it) }
    }

    // ---------- Foto: kamera dan galeri ----------

    private fun onTakePhotoClick() {
        val granted = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            launchCamera()
        } else {
            // Izin diminta saat runtime
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchCamera() {
        try {
            val dir = File(requireContext().cacheDir, "photos").apply { mkdirs() }
            val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
            cameraFilePath = file.absolutePath
            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                file
            )
            takePicture.launch(uri)
        } catch (e: Exception) {
            Snackbar.make(binding.root, R.string.msg_camera_unavailable, Snackbar.LENGTH_LONG).show()
        }
    }

    // Izin ditolak: aplikasi tetap normal, pengguna diarahkan ke galeri
    private fun showCameraDenied() {
        if (_binding == null) return
        Snackbar.make(binding.root, R.string.msg_camera_denied, Snackbar.LENGTH_LONG)
            .setAction(R.string.action_pick_gallery) { openGallery() }
            .show()
    }

    private fun openGallery() {
        pickImage.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    // Foto dikecilkan di latar belakang supaya layar tidak macet
    private fun processPhoto(uri: Uri) {
        if (_binding == null) return
        val appContext = requireContext().applicationContext
        val draft = formViewModel
        val handler = Handler(Looper.getMainLooper())
        setPhotoBusy(true)

        Thread {
            val base64 = ImageUtils.uriToBase64(appContext, uri)
            if (uri.scheme == "file") uri.path?.let { File(it).delete() }

            handler.post {
                if (base64 != null) draft.draftPhoto = base64
                if (_binding == null) return@post
                setPhotoBusy(false)
                if (base64 == null) {
                    Snackbar.make(binding.root, R.string.msg_photo_failed, Snackbar.LENGTH_LONG).show()
                } else {
                    showPhotoPreview()
                }
            }
        }.start()
    }

    private fun showPhotoPreview() {
        val photo = formViewModel.draftPhoto
        val bitmap = if (photo.isNotEmpty()) ImageUtils.base64ToBitmap(photo) else null
        binding.ivPhotoPreview.setImageBitmap(bitmap)
        binding.ivPhotoPreview.isVisible = bitmap != null
        binding.btnRemovePhoto.isVisible = bitmap != null
    }

    private fun setPhotoBusy(busy: Boolean) {
        binding.progress.isVisible = busy
        binding.btnSave.isEnabled = !busy
        binding.btnTakePhoto.isEnabled = !busy
        binding.btnPickGallery.isEnabled = !busy
        binding.btnRemovePhoto.isEnabled = !busy
    }

    // ---------- Form pesanan ----------

    private fun setupDropdowns() {
        val serviceLabels = ServiceType.values().map { getString(it.labelRes) }
        binding.actService.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, serviceLabels)
        )
        binding.actService.setOnItemClickListener { _, _, position, _ ->
            selectedService = ServiceType.values()[position]
            updateTotal()
        }

        val statusLabels = OrderStatus.values().map { getString(it.labelRes) }
        binding.actStatus.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, statusLabels)
        )
        binding.actStatus.setOnItemClickListener { _, _, position, _ ->
            selectedStatus = OrderStatus.values()[position]
        }
    }

    private fun fillForm(order: Order) {
        binding.etCustomerName.setText(order.customerName)
        binding.etWeight.setText(formatWeight(order.weight))
        selectedService = ServiceType.fromKey(order.serviceType)
        selectedStatus = OrderStatus.fromKey(order.status)
    }

    private fun showSelections() {
        binding.actService.setText(getString(selectedService.labelRes), false)
        binding.actStatus.setText(getString(selectedStatus.labelRes), false)
        updateTotal()
    }

    // "3,5" atau "3.5" -> 3.5
    private fun parseWeight(): Double? =
        binding.etWeight.text.toString().trim().replace(',', '.').toDoubleOrNull()

    private fun updateTotal() {
        val weight = parseWeight()
        val total = if (weight != null && weight > 0) selectedService.calculateTotal(weight) else 0L
        binding.tvTotalValue.text = formatRupiah(total)
    }

    private fun save() {
        val name = binding.etCustomerName.text.toString().trim()
        val weight = parseWeight()

        binding.tilCustomerName.error =
            if (name.isEmpty()) getString(R.string.error_customer_empty) else null
        binding.tilWeight.error = when {
            binding.etWeight.text.isNullOrBlank() -> getString(R.string.error_weight_empty)
            weight == null || weight <= 0.0 -> getString(R.string.error_weight_invalid)
            else -> null
        }

        if (binding.tilCustomerName.error != null ||
            binding.tilWeight.error != null ||
            weight == null
        ) return
        if (!ensureOnline()) return

        val order = Order(
            id = orderId,
            customerName = name,
            serviceType = selectedService.name,
            weight = weight,
            totalPrice = selectedService.calculateTotal(weight),
            status = selectedStatus.name,
            createdAt = existingOrder?.createdAt ?: System.currentTimeMillis(),
            photo = formViewModel.draftPhoto
        )
        isDeleting = false
        viewModel.saveOrder(order)
    }

    private fun confirmDelete() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_delete_title)
            .setMessage(R.string.dialog_delete_message)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                if (ensureOnline()) {
                    isDeleting = true
                    viewModel.deleteOrder(orderId)
                }
            }
            .show()
    }

    private fun ensureOnline(): Boolean {
        if (requireContext().isOnline()) return true
        Snackbar.make(binding.root, R.string.error_network, Snackbar.LENGTH_LONG).show()
        return false
    }

    private fun handleSaveState(state: UiState<Unit>) {
        when (state) {
            is UiState.Idle -> setLoading(false)
            is UiState.Loading -> setLoading(true)
            is UiState.Success -> {
                setLoading(false)
                val message = if (isDeleting) R.string.msg_order_deleted else R.string.msg_order_saved
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                viewModel.resetSaveState()
                findNavController().popBackStack()
            }
            is UiState.Error -> {
                setLoading(false)
                Snackbar.make(binding.root, state.messageRes, Snackbar.LENGTH_LONG).show()
                viewModel.resetSaveState()
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progress.isVisible = loading
        binding.btnSave.isEnabled = !loading
        binding.btnDelete.isEnabled = !loading
        binding.btnCancel.isEnabled = !loading
        binding.btnTakePhoto.isEnabled = !loading
        binding.btnPickGallery.isEnabled = !loading
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_SERVICE, selectedService.name)
        outState.putString(KEY_STATUS, selectedStatus.name)
        outState.putBoolean(KEY_DELETING, isDeleting)
        outState.putString(KEY_CAMERA_PATH, cameraFilePath)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val KEY_SERVICE = "service"
        private const val KEY_STATUS = "status"
        private const val KEY_DELETING = "deleting"
        private const val KEY_CAMERA_PATH = "cameraPath"
    }
}