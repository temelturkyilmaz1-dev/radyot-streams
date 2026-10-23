package com.radyo.turkiyeradyolari

import android.app.AlarmManager
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.graphics.Typeface
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import android.view.View
import android.widget.AdapterView
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.core.widget.NestedScrollView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.color.MaterialColors
import java.text.DateFormat
import java.util.Date

class TimerSettingsActivity : AppCompatActivity() {
    private data class PendingAlarm(val hour: Int, val minute: Int, val radioName: String)

    private var pendingAlarm: PendingAlarm? = null
    private lateinit var alarmStatus: TextView
    private lateinit var alarmStation: Spinner
    private lateinit var alarmVolumeText: TextView
    private val exactAlarmPermission = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        val request = pendingAlarm ?: return@registerForActivityResult
        pendingAlarm = null
        val manager = getSystemService(ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms()) {
            scheduleAlarmClock(request.hour, request.minute, request.radioName)
        } else {
            Toast.makeText(this, R.string.alarm_permission_denied, Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppPreferences.applySavedTheme(this)
        super.onCreate(savedInstanceState)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(8), dp(18), dp(40))
            addView(header())
            addView(sleepTimerCard(), cardParams())
            addView(alarmCard(), cardParams())
        }
        val scroll = NestedScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            addView(content)
        }
        ViewCompat.setOnApplyWindowInsetsListener(scroll) { view, insets ->
            val safe = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.mandatorySystemGestures())
            view.updatePadding(top = safe.top, left = safe.left, right = safe.right, bottom = safe.bottom + dp(32))
            insets
        }
        setContentView(scroll)
        ViewCompat.requestApplyInsets(scroll)
    }

    private fun header() = LinearLayout(this).apply {
        gravity = Gravity.CENTER_VERTICAL
        addView(ImageButton(this@TimerSettingsActivity).apply {
            setImageResource(R.drawable.ic_back)
            contentDescription = getString(R.string.back)
            background = null
            setPadding(dp(10), dp(10), dp(10), dp(10))
            setOnClickListener { finish() }
        }, LinearLayout.LayoutParams(dp(48), dp(48)))
        addView(TextView(this@TimerSettingsActivity).apply {
            text = getString(R.string.timer)
            textSize = 25f
            typeface = Typeface.DEFAULT_BOLD
        })
    }

    private fun sleepTimerCard() = styledCard().apply {
        addView(LinearLayout(this@TimerSettingsActivity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(16))
            addView(title(getString(R.string.sleep_timer)))
            addView(TextView(this@TimerSettingsActivity).apply {
                text = getString(R.string.sleep_timer_summary)
                textSize = 14f
                alpha = .7f
                setPadding(0, dp(3), 0, dp(12))
            })
            addView(ChipGroup(this@TimerSettingsActivity).apply {
                isSingleSelection = true
                isSelectionRequired = false
                isSingleLine = false
                listOf(15, 30, 45, 60, 90, 120).forEach { minutes ->
                    addView(Chip(this@TimerSettingsActivity).apply {
                        text = getString(R.string.minutes_short, minutes)
                        isCheckable = true
                        setOnClickListener {
                            SleepTimer.schedule(minutes)
                            Toast.makeText(this@TimerSettingsActivity, R.string.sleep_timer_set, Toast.LENGTH_SHORT).show()
                        }
                    })
                }
            })
            addView(MaterialButton(this@TimerSettingsActivity).apply {
                text = getString(R.string.cancel_sleep_timer)
                isAllCaps = false
                setOnClickListener {
                    SleepTimer.cancel()
                    Toast.makeText(this@TimerSettingsActivity, R.string.sleep_timer_cancelled, Toast.LENGTH_SHORT).show()
                }
            })
        })
    }

    private fun alarmCard() = styledCard().apply {
        addView(LinearLayout(this@TimerSettingsActivity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
            addView(title(getString(R.string.radio_alarm)))
            addView(TextView(this@TimerSettingsActivity).apply {
                text = getString(R.string.alarm_summary)
                textSize = 14f
                alpha = .7f
                setPadding(0, dp(3), 0, dp(12))
            })
            alarmStatus = TextView(this@TimerSettingsActivity).apply {
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(0, 0, 0, dp(8))
            }
            addView(alarmStatus)
            val stationNames = RadioRepository.radios.map(Radio::name)
            alarmStation = Spinner(this@TimerSettingsActivity).apply {
                adapter = ArrayAdapter(
                    this@TimerSettingsActivity,
                    android.R.layout.simple_spinner_dropdown_item,
                    stationNames
                )
                setPadding(dp(10), dp(8), dp(10), dp(8))
                val selectedIndex = stationNames.indexOf(RadioAlarmScheduler.selectedRadio(this@TimerSettingsActivity))
                if (selectedIndex >= 0) setSelection(selectedIndex, false)
                onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        stationNames.getOrNull(position)?.let {
                            RadioAlarmScheduler.setSelectedRadio(this@TimerSettingsActivity, it)
                        }
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) = Unit
                }
            }
            addView(alarmStation)
            alarmVolumeText = TextView(this@TimerSettingsActivity).apply {
                textSize = 14f
                setPadding(0, dp(10), 0, 0)
            }
            addView(alarmVolumeText)
            addView(SeekBar(this@TimerSettingsActivity).apply {
                max = 100
                progress = RadioAlarmScheduler.volume(this@TimerSettingsActivity)
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                        val safe = progress.coerceAtLeast(5)
                        if (fromUser && safe != progress) seekBar?.progress = safe
                        RadioAlarmScheduler.setVolume(this@TimerSettingsActivity, safe)
                        updateAlarmVolumeLabel()
                    }
                    override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
                    override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
                })
            })
            addView(MaterialButton(this@TimerSettingsActivity).apply {
                text = getString(R.string.choose_alarm_time)
                isAllCaps = false
                setOnClickListener {
                    TimePickerDialog(this@TimerSettingsActivity, { _, hour, minute ->
                        scheduleAlarm(hour, minute, alarmStation.selectedItem.toString())
                    }, RadioAlarmScheduler.hour(this@TimerSettingsActivity).takeIf { it >= 0 } ?: 8,
                        RadioAlarmScheduler.minute(this@TimerSettingsActivity).takeIf { it >= 0 } ?: 0, true).show()
                }
            })
            addView(MaterialButton(this@TimerSettingsActivity).apply {
                text = getString(R.string.cancel_radio_alarm)
                isAllCaps = false
                setOnClickListener {
                    RadioAlarmScheduler.cancel(this@TimerSettingsActivity)
                    refreshAlarmStatus()
                    Toast.makeText(this@TimerSettingsActivity, R.string.radio_alarm_cancelled, Toast.LENGTH_SHORT).show()
                }
            })
            refreshAlarmStatus()
            updateAlarmVolumeLabel()
        })
    }

    private fun styledCard() = MaterialCardView(this).apply {
        radius = dp(20).toFloat()
        cardElevation = 0f
        strokeWidth = dp(1)
        strokeColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOutlineVariant)
        setCardBackgroundColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurfaceContainer))
    }

    private fun cardParams() = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
        setMargins(0, dp(7), 0, dp(7))
    }

    private fun title(value: String) = TextView(this).apply {
        text = value
        textSize = 19f
        typeface = Typeface.DEFAULT_BOLD
    }

    private fun scheduleAlarm(hour: Int, minute: Int, radioName: String) {
        val manager = getSystemService(ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !manager.canScheduleExactAlarms()) {
            pendingAlarm = PendingAlarm(hour, minute, radioName)
            Toast.makeText(this, R.string.alarm_permission_required, Toast.LENGTH_LONG).show()
            exactAlarmPermission.launch(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$packageName")
                }
            )
            return
        }
        scheduleAlarmClock(hour, minute, radioName)
    }

    private fun scheduleAlarmClock(hour: Int, minute: Int, radioName: String) {
        if (RadioAlarmScheduler.schedule(this, hour, minute, radioName)) {
            refreshAlarmStatus()
            Toast.makeText(this, getString(R.string.alarm_set, hour, minute), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, R.string.alarm_permission_denied, Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::alarmStatus.isInitialized) refreshAlarmStatus()
    }

    private fun refreshAlarmStatus() {
        if (!::alarmStatus.isInitialized) return
        alarmStatus.text = if (RadioAlarmScheduler.isScheduled(this)) {
            val formatted = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                .format(Date(RadioAlarmScheduler.triggerAt(this)))
            getString(R.string.radio_alarm_scheduled_status, formatted, RadioAlarmScheduler.selectedRadio(this))
        } else {
            getString(R.string.radio_alarm_not_scheduled)
        }
    }

    private fun updateAlarmVolumeLabel() {
        if (::alarmVolumeText.isInitialized) {
            alarmVolumeText.text = getString(R.string.alarm_volume_percent, RadioAlarmScheduler.volume(this))
        }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
