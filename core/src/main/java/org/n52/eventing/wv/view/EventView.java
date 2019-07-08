/*
 * Copyright (C) 2016 - 2019 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation.
 *
 * If the program is linked with libraries which are licensed under one of
 * the following licenses, the combination of the program with the linked
 * library is not considered a "derivative work" of the program:
 *
 *     - Apache License, version 2.0
 *     - Apache Software License, version 1.0
 *     - GNU Lesser General Public License, version 3
 *     - Mozilla Public License, versions 1.0, 1.1 and 2.0
 *     - Common Development and Distribution License (CDDL), version 1.0
 *
 * Therefore the distribution of the program linked with libraries licensed
 * under the aforementioned licenses, is permitted by the copyright holders
 * if the distribution is compliant with both the GNU General Public
 * License version 2 and the aforementioned licenses.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 */
package org.n52.eventing.wv.view;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import org.n52.eventing.rest.model.EventHolder;
import org.n52.subverse.delivery.Streamable;

/**
 *
 * @author <a href="mailto:m.rieke@52north.org">Matthes Rieke</a>
 */
@JsonPropertyOrder({ "id", "href", "label", "timestamp", "timestampCreated", "eventType", "notificationLevel",
        "publication", "subscriptions", "eventDetails" })
public class EventView
        implements
        EventHolder {

    private Long timestamp;
    private String href;
    private Long timestampCreated;
    private String id;
    private IdHref eventType;
    private IdHref notificationLevel;
    private IdHref publication;
    private Set<IdHref> subscriptions = new LinkedHashSet<>();
    private String label;
    private EventDetailsView eventDetails;


    @JsonView(org.n52.eventing.rest.model.views.Views.EventOverview.class)
    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    @JsonView(org.n52.eventing.rest.model.views.Views.EventOverview.class)
    @Override
    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public void setHref(String h) {
        this.href = h;
    }

    @JsonView(org.n52.eventing.rest.model.views.Views.EventOverview.class)
    @Override
    public String getHref() {
        return this.href;
    }

    @Override
    public void setContent(String c) {
    }

    @Override
    public String getContent() {
        return null;
    }

    @Override
    public Optional<Streamable> streamableObject() {
        return Optional.empty();
    }

    @JsonView(org.n52.eventing.rest.model.views.Views.EventOverview.class)
    public Long getTimestampCreated() {
        return timestampCreated;
    }

    public void setTimestampCreated(Long timestampCreated) {
        this.timestampCreated = timestampCreated;
    }

    @JsonView(org.n52.eventing.rest.model.views.Views.EventOverview.class)
    public IdHref getEventType() {
        return eventType;
    }

    public void setEventType(IdHref eventType) {
        this.eventType = eventType;
    }

    @JsonView(org.n52.eventing.rest.model.views.Views.EventOverview.class)
    public IdHref getNotificationLevel() {
        return notificationLevel;
    }

    public void setNotificationLevel(IdHref notificationLevel) {
        this.notificationLevel = notificationLevel;
    }

    @JsonView(org.n52.eventing.rest.model.views.Views.EventOverview.class)
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @JsonView(org.n52.eventing.rest.model.views.Views.EventOverview.class)
    public IdHref getPublication() {
        return publication;
    }

    public void setPublication(IdHref publication) {
        this.publication = publication;
    }

    @JsonView(org.n52.eventing.rest.model.views.Views.EventExpanded.class)
    public Set<IdHref> getSubscriptions() {
        return subscriptions;
    }

    public void setSubscriptions(Set<IdHref> subscriptions) {
        this.subscriptions.clear();
        if (subscriptions != null) {
            this.subscriptions.addAll(subscriptions);
        }
    }

    public void addSubscriptions(Set<IdHref> subscriptions) {
        if (subscriptions != null) {
            this.subscriptions.addAll(subscriptions);
        }
    }

    public void addSubscription(IdHref subscription) {
        if (subscription != null) {
            this.subscriptions.add(subscription);
        }
    }

    @JsonView(org.n52.eventing.rest.model.views.Views.EventExpanded.class)
    public EventDetailsView getEventDetails() {
        return eventDetails;
    }

    public void setEventDetails(EventDetailsView eventDetails) {
        this.eventDetails = eventDetails;
    }


    public static class EventDetailsView {

        private Long previousTimestamp;
        private Double previousValue;
        private Double value;
        private String unit;
        private EventTriggerView eventTrigger;

        @JsonView(org.n52.eventing.rest.model.views.Views.EventExpanded.class)
        public Long getPreviousTimestamp() {
            return previousTimestamp;
        }

        public void setPreviousTimestamp(Long previousTimestamp) {
            this.previousTimestamp = previousTimestamp;
        }

        @JsonView(org.n52.eventing.rest.model.views.Views.EventExpanded.class)
        public Double getPreviousValue() {
            return previousValue;
        }

        public void setPreviousValue(Double previousValue) {
            this.previousValue = previousValue;
        }

        @JsonView(org.n52.eventing.rest.model.views.Views.EventExpanded.class)
        public Double getValue() {
            return value;
        }

        public void setValue(Double value) {
            this.value = value;
        }

        @JsonView(org.n52.eventing.rest.model.views.Views.EventExpanded.class)
        public String getUnit() {
            return unit;
        }

        public void setUnit(String unit) {
            this.unit = unit;
        }

        @JsonView(org.n52.eventing.rest.model.views.Views.EventExpanded.class)
        public EventTriggerView getEventTrigger() {
            return eventTrigger;
        }

        public void setEventTrigger(EventTriggerView eventTrigger) {
            this.eventTrigger = eventTrigger;
        }

    }

}
