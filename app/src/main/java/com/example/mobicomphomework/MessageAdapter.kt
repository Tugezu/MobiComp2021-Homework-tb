package com.example.mobicomphomework

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.example.mobicomphomework.db.ReminderMessage

class MessageAdapter(context: Context, private val dataSource: List<ReminderMessage>) : BaseAdapter() {

    private val inflater: LayoutInflater =
            context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getView(position: Int, convertView: View?, container: ViewGroup?): View? {
        val rowView = inflater.inflate(R.layout.activity_message_item, container, false)

        val eventNameTextView = rowView.findViewById<TextView>(R.id.txtEventName) as TextView
        val eventTimeTextView = rowView.findViewById<TextView>(R.id.txtEventTime) as TextView

        eventNameTextView.text = dataSource[position].message
        eventTimeTextView.text = dataSource[position].reminder_time

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