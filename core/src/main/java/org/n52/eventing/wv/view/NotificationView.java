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
import java.util.List;
import org.n52.eventing.rest.model.TemplateDefinition;
import org.n52.eventing.rest.model.views.Views;

/**
 *
 * @author <a href="mailto:m.rieke@52north.org">Matthes Rieke</a>
 */
@JsonPropertyOrder({"id", "href", "label", "publication", "rules"})
public class NotificationView implements TemplateDefinition {

    private String id;
    private String href;
    private IdHref publication;
    private List<RuleView> rules;
    private String label;


    @JsonView(value = {Views.SubscriptionExpanded.class, Views.TemplateOverview.class})
    public IdHref getPublication() {
        return publication;
    }

    public void setPublication(IdHref publication) {
        this.publication = publication;
    }

    @JsonView(value = {Views.TemplateExpanded.class})
    public List<RuleView> getRules() {
        return rules;
    }

    public void setRules(List<RuleView> rules) {
        this.rules = rules;
    }

    @JsonView(value = {Views.SubscriptionOverview.class, Views.TemplateOverview.class})
    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    @JsonView(value = {Views.SubscriptionOverview.class, Views.TemplateOverview.class})
    @Override
    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @JsonView(value = {Views.SubscriptionExpanded.class, Views.TemplateOverview.class})
    @Override
    public String getLabel() {
        return this.label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

}
