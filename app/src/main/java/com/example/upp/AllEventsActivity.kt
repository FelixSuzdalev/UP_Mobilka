package com.example.upp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.app.AlertDialog
import android.content.Intent
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView

class AllEventsActivity : AppCompatActivity() {
    private lateinit var allEventsListView: ListView
    private lateinit var allEvents: MutableList<String>

    // Объявляем переменную для хранения карты событий
    private var eventsMap: MutableMap<String, MutableList<String>>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_events)

        allEventsListView = findViewById(R.id.allEventsListView)

        // Получение переданных событий
        eventsMap = intent.getSerializableExtra("eventsMap") as? MutableMap<String, MutableList<String>>

        // Формирование списка всех событий
        allEvents = eventsMap?.entries?.flatMap { entry ->
            entry.value.map { "Дата: ${entry.key}\nСобытие: $it" }
        }?.toMutableList() ?: mutableListOf()

        // Установка адаптера для отображения
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, allEvents)
        allEventsListView.adapter = adapter

        allEventsListView.setOnItemClickListener { parent, view, position, id ->
            val selectedEvent = allEvents[position]
            showEditEventDialog(selectedEvent, position)
        }

        allEventsListView.setOnItemLongClickListener { parent, view, position, id ->
            val selectedEvent = allEvents[position]
            showDeleteEventDialog(selectedEvent, position)
            true
        }

        // Обработчик нажатия на кнопку "Вернуться"
        findViewById<Button>(R.id.button_return).setOnClickListener {
            returnDataToMainActivity() // Возвращаем обновленные данные в MainActivity
        }
    }

    private fun showEditEventDialog(event: String, position: Int) {
        val builder = AlertDialog.Builder(this)
        val input = EditText(this).apply { setText(event) }
        builder.setTitle("Изменить событие")
        builder.setView(input)

        builder.setPositiveButton("Сохранить") { dialog, _ ->
            val newEvent = input.text.toString()
            if (newEvent.isNotEmpty()) {
                updateEvent(newEvent, position)
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Отмена") { dialog, _ -> dialog.cancel() }

        builder.show()
    }

    private fun updateEvent(newEvent: String, position: Int) {
        val (date, oldEvent) = allEvents[position].split("\nСобытие: ")
        eventsMap?.get(date)?.let { events ->
            val index = events.indexOf(oldEvent)
            if (index != -1) {
                events[index] = newEvent
                saveEvents() // Сохраняем изменения
                recreate() // Обновляем активность
            }
        }
    }

    private fun showDeleteEventDialog(event: String, position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Удалить событие")
            .setMessage("Вы уверены, что хотите удалить событие '$event'?")
            .setPositiveButton("Удалить") { dialog, _ ->
                deleteEvent(position)
                dialog.dismiss()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun deleteEvent(position: Int) {
        val (date, eventToDelete) = allEvents[position].split("\nСобытие: ")
        eventsMap?.get(date)?.let { events ->
            events.remove(eventToDelete)
            if (events.isEmpty()) {
                eventsMap?.remove(date)
            }
            saveEvents() // Сохраняем изменения
            recreate() // Обновляем активность
        }
    }

    private fun saveEvents() {
        val sharedPreferences = getSharedPreferences("CalendarEvents", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        for ((key, value) in eventsMap ?: emptyMap()) {
            editor.putString(key, value.joinToString(";"))
        }
        editor.apply()
        // Возвращаем обновленные данные обратно в MainActivity
        returnDataToMainActivity()
    }

    private fun returnDataToMainActivity() {
        val intent = Intent()

        // Возвращаем обновленную карту событий
        val bundle = Bundle()
        eventsMap?.forEach { (date, events) ->
            bundle.putString(date, events.joinToString(";"))
        }

        intent.putExtra("eventsMap", bundle)
        setResult(RESULT_OK, intent) // Устанавливаем результат
        finish() // Закрываем активность
    }

}