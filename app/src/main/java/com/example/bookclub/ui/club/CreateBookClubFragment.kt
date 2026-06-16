package com.example.bookclub.ui.club

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.bookclub.R
import com.example.bookclub.data.ServiceLocator
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class CreateBookClubFragment : Fragment(R.layout.fragment_create_book_club) {

    private val args: CreateBookClubFragmentArgs by navArgs()
    private val vm: CreateClubViewModel by viewModels()

    private lateinit var etTitle: TextInputEditText
    private lateinit var etAuthor: TextInputEditText
    private lateinit var etDescription: TextInputEditText

    private lateinit var txtStartDate: TextView
    private lateinit var txtEndDate: TextView
    private lateinit var btnPickStart: Button
    private lateinit var btnPickEnd: Button
    private lateinit var btnCreate: Button

    private var startDate: LocalDate? = null
    private var startTime: LocalTime? = null
    private var endDate: LocalDate? = null
    private var endTime: LocalTime? = null

    private var startAtInstant: Instant = Instant.now()
    private var endAtInstant: Instant = Instant.now().plusSeconds(3600)

    private val uiFormatter: DateTimeFormatter = DateTimeFormatter
        .ofPattern("dd MMM yyyy, HH:mm")
        .withZone(ZoneId.systemDefault())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etTitle = view.findViewById(R.id.etTitle)
        etAuthor = view.findViewById(R.id.etAuthor)
        etDescription = view.findViewById(R.id.etDescription)

        txtStartDate = view.findViewById(R.id.txtStartDate)
        txtEndDate = view.findViewById(R.id.txtEndDate)
        btnPickStart = view.findViewById(R.id.btnPickStart)
        btnPickEnd = view.findViewById(R.id.btnPickEnd)
        btnCreate = view.findViewById(R.id.btnCreate)

        val session = ServiceLocator.sessionManager(requireContext()).get()

        if (session == null) {
            Toast.makeText(
                requireContext(),
                getString(R.string.err_not_logged_in),
                Toast.LENGTH_LONG
            ).show()

            findNavController().popBackStack()
            return
        }

        if (session.role != "ADMIN") {
            Toast.makeText(
                requireContext(),
                "Only admins can create clubs",
                Toast.LENGTH_LONG
            ).show()

            findNavController().popBackStack()
            return
        }

        etTitle.setText(args.title)
        etAuthor.setText(args.author)

        val startDefault = ZonedDateTime.now().plusMinutes(5)
        applyStartPicked(startDefault.toLocalDate(), startDefault.toLocalTime())

        val endDefault = startDefault.plusHours(1)
        applyEndPicked(endDefault.toLocalDate(), endDefault.toLocalTime())

        btnPickStart.setOnClickListener {
            pickDateTime(isStart = true)
        }

        btnPickEnd.setOnClickListener {
            pickDateTime(isStart = false)
        }

        btnCreate.setOnClickListener {
            val title = etTitle.text?.toString()?.trim().orEmpty()
            val author = etAuthor.text?.toString()?.trim().orEmpty().ifEmpty { "Unknown" }
            val description = etDescription.text?.toString()?.trim()

            if (title.isBlank()) {
                etTitle.error = getString(R.string.hint_title)
                return@setOnClickListener
            }

            if (!endAtInstant.isAfter(startAtInstant)) {
                Toast.makeText(
                    requireContext(),
                    R.string.err_end_before_start,
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            vm.createClub(
                adminId = session.userId,
                workId = args.workId,
                title = title,
                author = author,
                description = description,
                coverUrl = args.coverUrl.ifEmpty { null },
                startAt = startAtInstant,
                closeAt = endAtInstant
            )
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.state.collectLatest { st ->
                    when (st) {
                        is CreateState.Idle -> Unit

                        is CreateState.Loading -> {
                            btnCreate.isEnabled = false
                        }

                        is CreateState.Success -> {
                            btnCreate.isEnabled = true

                            Toast.makeText(
                                requireContext(),
                                "Club created (id=${st.clubId})",
                                Toast.LENGTH_SHORT
                            ).show()

                            vm.reset()

                            val opts = NavOptions.Builder()
                                .setPopUpTo(R.id.navigation_home, false)
                                .setLaunchSingleTop(true)
                                .build()

                            findNavController().navigate(R.id.homeFragment, null, opts)
                        }

                        is CreateState.Error -> {
                            btnCreate.isEnabled = true

                            Toast.makeText(
                                requireContext(),
                                st.t.message ?: "Create failed",
                                Toast.LENGTH_LONG
                            ).show()

                            vm.reset()
                        }
                    }
                }
            }
        }
    }

    private fun pickDateTime(isStart: Boolean) {
        val now = ZonedDateTime.now()

        val d = if (isStart) {
            startDate ?: now.toLocalDate()
        } else {
            endDate ?: now.toLocalDate()
        }

        val t = if (isStart) {
            startTime ?: now.toLocalTime()
        } else {
            endTime ?: now.toLocalTime()
        }

        DatePickerDialog(requireContext(), { _, y, m, day ->
            val date = LocalDate.of(y, m + 1, day)

            TimePickerDialog(requireContext(), { _, hh, mm ->
                val time = LocalTime.of(hh, mm)

                if (isStart) {
                    applyStartPicked(date, time)
                } else {
                    applyEndPicked(date, time)
                }
            }, t.hour, t.minute, true).show()

        }, d.year, d.monthValue - 1, d.dayOfMonth).apply {
            if (isStart) {
                datePicker.minDate = System.currentTimeMillis()
            }
        }.show()
    }

    private fun applyStartPicked(date: LocalDate, time: LocalTime) {
        startDate = date
        startTime = time

        val zdt = ZonedDateTime.of(date, time, ZoneId.systemDefault())
        startAtInstant = zdt.toInstant()

        txtStartDate.text = getString(
            R.string.label_start_at,
            uiFormatter.format(zdt)
        )

        val currentEndZdt = ZonedDateTime.of(
            endDate ?: date,
            endTime ?: time,
            ZoneId.systemDefault()
        )

        if (!currentEndZdt.toInstant().isAfter(startAtInstant)) {
            val fixedEnd = zdt.plusHours(1)
            applyEndPicked(fixedEnd.toLocalDate(), fixedEnd.toLocalTime())
        }
    }

    private fun applyEndPicked(date: LocalDate, time: LocalTime) {
        endDate = date
        endTime = time

        val zdt = ZonedDateTime.of(date, time, ZoneId.systemDefault())
        endAtInstant = zdt.toInstant()

        txtEndDate.text = getString(
            R.string.label_end_at,
            uiFormatter.format(zdt)
        )
    }
}