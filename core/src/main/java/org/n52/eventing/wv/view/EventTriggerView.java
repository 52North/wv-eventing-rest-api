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
@JsonPropertyOrder({"code", "label", "threshold", "thresholdUnit", "notification", "user", "group"})
public class EventTriggerView {

    private Integer code;
    private String label;
    private Number threshold;
    private String thresholdUnit;
    private IdHref group;
    private IdHref user;
    private IdHref notification;

    @JsonView(value = { org.n52.eventing.rest.model.views.Views.EventExpanded.class,
            org.n52.eventing.rest.model.views.Views.TemplateExpanded.class })
    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    @JsonView(value = { org.n52.eventing.rest.model.views.Views.EventExpanded.class,
            org.n52.eventing.rest.model.views.Views.TemplateExpanded.class })
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @JsonView(value = { org.n52.eventing.rest.model.views.Views.EventExpanded.class,
            org.n52.eventing.rest.model.views.Views.TemplateExpanded.class })
    public Number getThreshold() {
        return threshold;
    }

    public void setThreshold(Number threshold) {
        this.threshold = threshold;
    }

    @JsonView(value = { org.n52.eventing.rest.model.views.Views.EventExpanded.class,
            org.n52.eventing.rest.model.views.Views.TemplateExpanded.class })
    public String getThresholdUnit() {
        return thresholdUnit;
    }

    public void setThresholdUnit(String thresholdUnit) {
        this.thresholdUnit = thresholdUnit;
    }

    @JsonView(value = { org.n52.eventing.rest.model.views.Views.EventExpanded.class,
            org.n52.eventing.rest.model.views.Views.TemplateExpanded.class })
    public IdHref getGroup() {
        return group;
    }

    public void setGroup(IdHref group) {
        this.group = group;
    }

    @JsonView(value = { org.n52.eventing.rest.model.views.Views.EventExpanded.class,
            org.n52.eventing.rest.model.views.Views.TemplateExpanded.class })
    public IdHref getUser() {
        return user;
    }

    public void setUser(IdHref user) {
        this.user = user;
    }

    @JsonView(value = { org.n52.eventing.rest.model.views.Views.EventExpanded.class,
            org.n52.eventing.rest.model.views.Views.TemplateExpanded.class })
    public IdHref getNotification() {
        return notification;
    }

    public void setNotification(IdHref notification) {
        this.notification = notification;
    }
}
