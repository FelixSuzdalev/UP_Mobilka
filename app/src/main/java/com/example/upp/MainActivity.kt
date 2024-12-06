package com.example.upp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.*
import java.util.*
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class MainActivity : AppCompatActivity() {


    private val themePreferenceKey = "AppTheme"
    private val notificationChannelId = "CalendarEventChannel"
    private val notificationId = 1
    private lateinit var calendarView: CalendarView
    private lateinit var addEventButton: Button
    private lateinit var selectedDateText: TextView
    private lateinit var eventListView: ListView
    private lateinit var allEventDatesTextView: TextView

    private var selectedDate: String = getCurrentDate()
    private val eventsMap: MutableMap<String, MutableList<String>> = mutableMapOf()
    private val sharedPreferencesKey = "CalendarEvents"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d("MainActivity", "onCreate: Инициализация приложения началась")

        // Инициализация элементов
        calendarView = findViewById(R.id.calendarView)
        addEventButton = findViewById(R.id.addEventButton)
        selectedDateText = findViewById(R.id.selectedDate)
        eventListView = findViewById(R.id.eventListView)
        allEventDatesTextView = findViewById(R.id.allEventDatesTextView)

        // Загрузка событий из SharedPreferences
        try {
            loadEvents()
            Log.d("MainActivity", "onCreate: События загружены: $eventsMap")
        } catch (e: Exception) {
            Log.e("MainActivity", "onCreate: Ошибка при загрузке событий", e)
        }

        // Установка текущей даты
        selectedDateText.text = "Выбранная дата: $selectedDate"
        Log.d("MainActivity", "onCreate: Текущая дата: $selectedDate")

        // Обработчик выбора даты
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            selectedDate = "$dayOfMonth/${month + 1}/$year"
            selectedDateText.text = "Выбранная дата: $selectedDate"
            Log.d("MainActivity", "onDateChange: Выбранная дата изменена на $selectedDate")
            updateEventList()
        }

        // Обработчик добавления события
        addEventButton.setOnClickListener {
            Log.d("MainActivity", "onAddEvent: Кнопка добавления события нажата")
            showAddEventDialog()
        }

        // Обновление списка событий для текущей даты
        updateEventList()
        updateAllEventDates()
        Log.d("MainActivity", "onCreate: Инициализация завершена")
        createNotificationChannel()

        // Открытие нового окна с записями
        findViewById<Button>(R.id.viewAllEventsButton).setOnClickListener {
            val intent = Intent(this, AllEventsActivity::class.java)
            intent.putExtra("eventsMap", HashMap(eventsMap)) // Передача событий
            startActivityForResult(intent, REQUEST_CODE_ALL_EVENTS) // Передаем код запроса
        }

        // Проверка событий и отправка уведомления
        checkUpcomingEvents()
        eventListView.setOnItemClickListener { parent, view, position, id ->
            val event = parent.getItemAtPosition(position) as String
            showEditEventDialog(event, selectedDate)
        }

        eventListView.setOnItemLongClickListener { parent, view, position, id ->
            val event = parent.getItemAtPosition(position) as String
            showDeleteEventDialog(event, selectedDate)
            true
        }
    }

    private fun getAllEventDates(): List<String> {
        return eventsMap.keys.toList() // Возвращает список всех ключей (дат) в eventsMap
    }

    private fun addEvent(event: String) {
        if (!eventsMap.containsKey(selectedDate)) {
            eventsMap[selectedDate] = mutableListOf()
        }
        eventsMap[selectedDate]?.add(event)
        saveEvents()
        updateEventList()
        Log.d("MainActivity", "addEvent: Добавлено событие '$event' для даты $selectedDate")
    }

    private fun updateEventList() {
        val events = eventsMap[selectedDate] ?: emptyList()
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, events)
        eventListView.adapter = adapter
        Log.d("MainActivity", "updateEventList: Обновлен список событий для $selectedDate: $events")
    }

    private fun showAddEventDialog() {
        val builder = AlertDialog.Builder(this)
        val input = EditText(this)
        builder.setTitle("Добавить событие")
        builder.setView(input)

        builder.setPositiveButton("Добавить") { dialog, _ ->
            val event = input.text.toString()
            if (event.isNotEmpty()) {
                addEvent(event)
            } else {
                Log.w("MainActivity", "showAddEventDialog: Попытка добавить пустое событие")
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Отмена") { dialog, _ ->
            Log.d("MainActivity", "showAddEventDialog: Добавление события отменено")
            dialog.cancel()
        }

        builder.show()
    }

    private fun saveEvents() {
        val sharedPreferences = getSharedPreferences(sharedPreferencesKey, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        try {
            for ((key, value) in eventsMap) {
                editor.putString(key, value.joinToString(";"))
            }
            editor.apply()
            Log.d("MainActivity", "saveEvents: События сохранены: $eventsMap")
        } catch (e: Exception) {
            Log.e("MainActivity", "saveEvents: Ошибка при сохранении событий", e)
        }
        updateAllEventDates() // Обновляем даты после сохранения
    }

    private fun loadEvents() {
        val sharedPreferences = getSharedPreferences(sharedPreferencesKey, Context.MODE_PRIVATE)
        sharedPreferences.all.forEach { (key, value) ->
            if (value is String) {
                eventsMap[key] = value.split(";").toMutableList()
            }
        }
        Log.d("MainActivity", "loadEvents: Загружены события: $eventsMap")
        updateAllEventDates() // Обновляем даты при загрузке
    }

    override  fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        Log.d("MainActivity", "onCreateOptionsMenu: Меню создано")
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.themeMenu -> {
                Log.d("MainActivity", "onOptionsItemSelected: Смена темы")
                //toggleTheme()
                true
            }
            R.id.aboutMenu -> {
                Log.d("MainActivity", "onOptionsItemSelected: О программе")
                showAboutDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


   /* private fun toggleTheme() {
        val isDarkTheme = loadTheme()
        saveTheme(!isDarkTheme) // Сохраняем новое состояние темы
        recreate() // Перезапускаем активность для применения изменений
    }*/

    private fun showAboutDialog() {
        AlertDialog.Builder(this)
            .setTitle("О программе")
            .setMessage("Приложение для записи событий в календаре. Версия 1.0.")
            .setPositiveButton("OK", null)
            .show()
        Log.d("MainActivity", "showAboutDialog: О программе показано")
    }
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                notificationChannelId,
                "Напоминания о событиях",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Канал для уведомлений о событиях"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun checkUpcomingEvents() {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, 1) // Завтра
        val tomorrow = "${calendar.get(Calendar.DAY_OF_MONTH)}/${calendar.get(Calendar.MONTH) + 1}/${calendar.get(Calendar.YEAR)}"

        if (eventsMap.containsKey(tomorrow)) {
            sendNotification("Напоминание", "Есть события на завтра: $tomorrow")
        }
    }

    private fun sendNotification(title: String, message: String) {
        val notification = NotificationCompat.Builder(this, notificationChannelId)
            .setSmallIcon(R.drawable.icon)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(notificationId, notification)
    }

    companion object {
        private const val REQUEST_CODE_ALL_EVENTS = 1
        private fun getCurrentDate(): String {
            val calendar = Calendar.getInstance()
            val currentDate = "${calendar.get(Calendar.DAY_OF_MONTH)}/${calendar.get(Calendar.MONTH) + 1}/${calendar.get(Calendar.YEAR)}"
            Log.d("MainActivity", "getCurrentDate: Текущая дата: $currentDate")
            return currentDate
        }
    }
    /*private fun setTheme() {
        val isDarkTheme = loadTheme()
        if (isDarkTheme) {
            setTheme(R.style.Theme_AppCompat_Dark)
        } else {
            setTheme(R.style.Theme_AppCompat_Light)
        }
    }

    private fun loadTheme(): Boolean {
        val sharedPreferences = getSharedPreferences(sharedPreferencesKey, Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean(themePreferenceKey, false)
    }

    private fun saveTheme(isDarkTheme: Boolean) {
        val sharedPreferences = getSharedPreferences(sharedPreferencesKey, Context.MODE_PRIVATE)
        sharedPreferences.edit().putBoolean(themePreferenceKey, isDarkTheme).apply()
    }*/
    private fun showEditEventDialog(event: String, date: String) {
        val builder = AlertDialog.Builder(this)
        val input = EditText(this).apply { setText(event) }
        builder.setTitle("Изменить событие")
        builder.setView(input)

        builder.setPositiveButton("Сохранить") { dialog, _ ->
            val newEvent = input.text.toString()
            if (newEvent.isNotEmpty()) {
                updateEvent(event, newEvent, date)
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Отмена") { dialog, _ -> dialog.cancel() }

        builder.show()
    }

    private fun updateEvent(oldEvent: String, newEvent: String, date: String) {
        eventsMap[date]?.let { events ->
            val index = events.indexOf(oldEvent)
            if (index != -1) {
                events[index] = newEvent
                saveEvents()
                updateEventList()
                Log.d("MainActivity", "updateEvent: Изменено событие '$oldEvent' на '$newEvent' для даты $date")
            }
        }
    }

    private fun showDeleteEventDialog(event: String, date: String) {
        AlertDialog.Builder(this)
            .setTitle("Удалить событие")
            .setMessage("Вы уверены, что хотите удалить событие '$event'?")
            .setPositiveButton("Удалить") { dialog, _ ->
                deleteEvent(event, date)
                dialog.dismiss()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun deleteEvent(event: String, date: String) {
        eventsMap[date]?.let { events ->
            events.remove(event)
            if (events.isEmpty()) {
                eventsMap.remove(date) // Удаляем дату, если нет событий
            }
            saveEvents()
            updateEventList()
            Log.d("MainActivity", "deleteEvent: Удалено событие '$event' для даты $date")
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_ALL_EVENTS && resultCode == RESULT_OK) {
            val bundle = data?.getBundleExtra("eventsMap") // Извлечение Bundle
            val updatedEventsMap = mutableMapOf<String, MutableList<String>>()

            bundle?.keySet()?.forEach { key ->
                val eventsString = bundle.getString(key) ?: ""
                updatedEventsMap[key] = eventsString.split(";").toMutableList()
            }

            // Обновляем карту событий
            eventsMap.clear() // Очищаем существующие данные
            eventsMap.putAll(updatedEventsMap) // Обновляем из переданных данных
            updateEventList() // Обновляем список событий
        }
    }
    private fun updateAllEventDates() {
        val allDates = getAllEventDates().joinToString(", ") // Собираем все даты в строку
        allEventDatesTextView.text = "Все даты событий: $allDates" // Устанавливаем текст в TextView
    }
}