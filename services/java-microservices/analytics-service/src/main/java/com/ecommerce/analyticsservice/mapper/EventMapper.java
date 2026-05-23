package com.ecommerce.analyticsservice.mapper;

import com.ecommerce.analyticsservice.dto.EventResponse;
import com.ecommerce.analyticsservice.dto.MetricResponse;
import com.ecommerce.analyticsservice.entity.Event;
import com.ecommerce.analyticsservice.entity.Metric;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EventMapper {

    EventResponse toEventResponse(Event event);

    List<EventResponse> toEventResponseList(List<Event> events);

    @Mapping(target = "metricType", expression = "java(metric.getMetricType().name())")
    MetricResponse toMetricResponse(Metric metric);

    List<MetricResponse> toMetricResponseList(List<Metric> metrics);
}
