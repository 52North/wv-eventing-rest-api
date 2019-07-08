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

/**
 *
 * @author <a href="mailto:m.rieke@52north.org">Matthes Rieke</a>
 */
@JsonPropertyOrder({"id", "href", "primaryRule", "eventTrigger", "notificationLevel", "eventType"})
public class RuleView {

    private String id;
    private String href;
    private Boolean primaryRule;
    private EventTriggerView eventTrigger;
    private IdHref notificationLevel;
    private IdHref eventType;

    @JsonView(org.n52.eventing.rest.model.views.Views.BaseView.class)
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setId(int id) {
        setId(Integer.toString(id));
    }

    @JsonView(org.n52.eventing.rest.model.views.Views.BaseView.class)
    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    @JsonView(org.n52.eventing.rest.model.views.Views.TemplateExpanded.class)
    public Boolean getPrimaryRule() {
        return primaryRule;
    }

    public void setPrimaryRule(Boolean primaryRule) {
        this.primaryRule = primaryRule;
    }

    @JsonView(org.n52.eventing.rest.model.views.Views.TemplateExpanded.class)
    public EventTriggerView getEventTrigger() {
        return eventTrigger;
    }

    public void setEventTrigger(EventTriggerView eventTrigger) {
        this.eventTrigger = eventTrigger;
    }

    @JsonView(org.n52.eventing.rest.model.views.Views.TemplateExpanded.class)
    public IdHref getNotificationLevel() {
        return notificationLevel;
    }

    public void setNotificationLevel(IdHref notificationLevel) {
        this.notificationLevel = notificationLevel;
    }

    @JsonView(org.n52.eventing.rest.model.views.Views.TemplateExpanded.class)
    public IdHref getEventType() {
        return eventType;
    }

    public void setEventType(IdHref eventType) {
        this.eventType = eventType;
    }



}
