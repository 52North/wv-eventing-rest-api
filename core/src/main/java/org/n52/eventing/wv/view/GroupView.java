/*
 * Copyright (C) 2016-2020 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License version 2 as publishedby the Free
 * Software Foundation.
 *
 * If the program is linked with libraries which are licensed under one of the
 * following licenses, the combination of the program with the linked library is
 * not considered a "derivative work" of the program:
 *
 *     - Apache License, version 2.0
 *     - Apache Software License, version 1.0
 *     - GNU Lesser General Public License, version 3
 *     - Mozilla Public License, versions 1.0, 1.1 and 2.0
 *     - Common Development and Distribution License (CDDL), version 1.0
 *
 * Therefore the distribution of the program linked with libraries licensed under
 * the aforementioned licenses, is permitted by the copyright holders if the
 * distribution is compliant with both the GNU General Public License version 2
 * and the aforementioned licenses.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 */
package org.n52.eventing.wv.view;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;
import org.n52.eventing.rest.model.views.Views;

/**
 *
 * @author <a href="mailto:m.rieke@52north.org">Matthes Rieke</a>
 */
@JsonPropertyOrder({"id", "href", "label", "description", "userGroupId", "admin"})
public class GroupView {

    private String id;
    private Long userGroupId;
    private String label;
    private String description;
    private Boolean admin;
    private String href;

    @JsonView(value = {Views.SubscriptionExpanded.class, org.n52.eventing.wv.view.Views.UserOverview.class, Views.TemplateOverview.class})
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setId(int id) {
        setId(Integer.toString(id, 10));
    }
    @JsonView(value = {Views.SubscriptionExpanded.class, org.n52.eventing.wv.view.Views.UserOverview.class, Views.TemplateOverview.class})
    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    @JsonView(value = {Views.SubscriptionExpanded.class, org.n52.eventing.wv.view.Views.UserOverview.class, Views.TemplateOverview.class})
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @JsonView(value = org.n52.eventing.wv.view.Views.UserOverview.class)
    public Boolean getAdmin() {
        return admin;
    }

    @JsonView(value = org.n52.eventing.wv.view.Views.UserOverview.class)
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setAdmin(Boolean admin) {
        this.admin = admin;
    }

    public Long getUserGroupId() {
        return userGroupId;
    }

    public void setUserGroupId(Long userGroupId) {
        this.userGroupId = userGroupId;
    }

}
