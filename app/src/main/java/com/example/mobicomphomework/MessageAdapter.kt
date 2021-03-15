package com.example.mobicomphomework

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.example.mobicomphomework.db.ReminderMessage
import java.text.SimpleDateFormat
import java.util.*

class MessageAdapter(context: Context, private val dataSource: List<ReminderMessage>) : BaseAdapter() {

    private val inflater: LayoutInflater =
            context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getView(position: Int, convertView: View?, container: ViewGroup?): View? {
        val rowView = inflater.inflate(R.layout.activity_message_item, container, false)

        val databaseTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm")
        val displayTimeFormat = SimpleDateFormat("HH:mm dd.MM.yyyy")

        val reminderCalendar = Calendar.getInstance()
        reminderCalendar.time = databaseTimeFormat.parse(dataSource[position].reminder_time)!!

        val eventNameTextView = rowView.findViewById(R.id.txtEventName) as TextView
        val eventTimeTextView = rowView.findViewById(R.id.txtEventTime) as TextView
        val eventLocationTextView = rowView.findViewById(R.id.txtEventLocation) as TextView

        eventNameTextView.text = dataSource[position].message
        
        if (reminderCalendar.timeInMillis > 0) {
            eventTimeTextView.text = displayTimeFormat.format(reminderCalendar.time)
        } else {
            eventTimeTextView.text = "disabled"
        }
        
        if (dataSource[position].location_x == Constants.UNDEFINED_COORDINATE) {
            eventLocationTextView.text = "disabled"
        } else {
            eventLocationTextView.text = "Lat: %.${3}f Long: %.${3}f".format(
                    dataSource[position].location_y, dataSource[position].location_x)
        }
        return rowView
    }

    override fun getItem(position: Int): Any {
        return dataSource[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getCount(): Int {
        return dataSource.size
    }

}