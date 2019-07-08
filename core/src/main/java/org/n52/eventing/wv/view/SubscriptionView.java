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
import org.joda.time.DateTime;
import org.n52.eventing.rest.model.DeliveryMethodDefinition;
import org.n52.eventing.rest.model.Subscription;
import org.n52.eventing.rest.model.views.Views;
import org.n52.eventing.rest.templates.TemplateInstance;
import org.n52.eventing.rest.users.User;

/**
 *
 * @author <a href="mailto:m.rieke@52north.org">Matthes Rieke</a>
 */
@JsonPropertyOrder({"id", "href", "label", "created", "modified", "endOfLife", "notification", "notificationInstance", "deliveryMethod", "group", "user"})
public class SubscriptionView implements Subscription {

    private String id;
    private UserView user;
    private GroupView group;
    private NotificationView notification;
    private String href;
    private String label;
    private DateTime created;
    private DateTime modified;
    private DateTime endOfLife;
    private TemplateInstance notificationInstance;
    private DeliveryMethodDefinition deliveryMethod;


    @JsonView(value = {Views.SubscriptionExpanded.class, Views.TemplateOverview.class})
    @Override
    public User getUser() {
        return user;
    }

    public void setUser(UserView user) {
        this.user = user;
    }

    @JsonView(value = {Views.SubscriptionExpanded.class, Views.TemplateOverview.class})
    public GroupView getGroup() {
        return group;
    }

    public void setGroup(GroupView group) {
        this.group = group;
    }

    @JsonView(Views.SubscriptionExpanded.class)
    public NotificationView getNotification() {
        return notification;
    }

    public void setNotification(NotificationView notification) {
        this.notification = notification;
    }

    @JsonView(value = {Views.SubscriptionExpanded.class, Views.TemplateOverview.class})
    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public void setHref(String h) {
        this.href = h;
    }

    @JsonView(value = {Views.SubscriptionExpanded.class, Views.TemplateOverview.class})
    public String getHref() {
        return href;
    }

    @Override
    public String getPublicationId() {
        return null;
    }

    @JsonView(Views.SubscriptionExpanded.class)
    @Override
    public TemplateInstance getNotificationInstance() {
        return this.notificationInstance;
    }

    public void setNotificationInstance(TemplateInstance notificationInstance) {
        this.notificationInstance = notificationInstance;
    }

    @JsonView(value = {Views.SubscriptionExpanded.class, Views.TemplateOverview.class})
    public DeliveryMethodDefinition getDeliveryMethod() {
        return deliveryMethod;
    }

    public void setDeliveryMethod(DeliveryMethodDefinition deliveryMethod) {
        this.deliveryMethod = deliveryMethod;
    }

    @JsonView(Views.SubscriptionExpanded.class)
    @Override
    public String getLabel() {
        return this.label;
    }

    @Override
    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public void setPublicationId(String pubId) {
    }

    @Override
    public void setUser(User u) {
    }

    @Override
    public void setCreated(DateTime c) {
        this.created = c;
    }

    @Override
    public void setModified(DateTime m) {
        this.modified = m;
    }

    @Override
    public DateTime getEndOfLife() {
        return this.endOfLife;
    }

    @Override
    public void setEndOfLife(DateTime eol) {
        this.endOfLife = eol;
    }

    public DateTime getCreated() {
        return created;
    }

    public DateTime getModified() {
        return modified;
    }

}
