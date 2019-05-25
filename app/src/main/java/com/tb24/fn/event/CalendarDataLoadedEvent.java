package com.tb24.fn.event;

import com.tb24.fn.model.CalendarTimelineResponse;

public class CalendarDataLoadedEvent {
	private final CalendarTimelineResponse.ClientEventState calendarData;

	public CalendarDataLoadedEvent(CalendarTimelineResponse.ClientEventState calendarData) {
		this.calendarData = calendarData;
	}
}
