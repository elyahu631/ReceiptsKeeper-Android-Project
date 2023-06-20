package com.example.receiptskeeper.classes;

import android.graphics.Color;

import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.prolificinteractive.materialcalendarview.spans.DotSpan;

public class DecorateDaysWithQueue implements DayViewDecorator {
    private final CalendarDay date;

    @Override
    public void decorate(DayViewFacade view) {
        view.addSpan(new DotSpan(6, Color.RED));
    }
    public DecorateDaysWithQueue(CalendarDay date) {
        this.date = date;

    }

    @Override
    public boolean shouldDecorate(CalendarDay day) {
        return day.equals(date);
    }

}
