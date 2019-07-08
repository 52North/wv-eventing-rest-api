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
package org.n52.eventing.wv.model;

import java.io.Serializable;

/**
 *
 * @author <a href="mailto:m.rieke@52north.org">Matthes Rieke</a>
 */
public class Series implements BaseEntity, Serializable {

    private int id;
    private Category category;
    private Phenomenon phenomenon;
    private Procedure procedure;
    private FeatureOfInterest feature;
    private Unit unit;
    protected Short eventingFlag;
    private boolean activeForEventing;
    private SeriesCheckAge checkAge;


    public Series() {
    }

    public Series(Category category, Phenomenon phenomenon, Procedure procedure, FeatureOfInterest feature, Unit u) {
        this.category = category;
        this.phenomenon = phenomenon;
        this.procedure = procedure;
        this.feature = feature;
        this.unit = u;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Phenomenon getPhenomenon() {
        return phenomenon;
    }

    public void setPhenomenon(Phenomenon phenomenon) {
        this.phenomenon = phenomenon;
    }

    public Procedure getProcedure() {
        return procedure;
    }

    public void setProcedure(Procedure procedure) {
        this.procedure = procedure;
    }

    public FeatureOfInterest getFeature() {
        return feature;
    }

    public void setFeature(FeatureOfInterest feature) {
        this.feature = feature;
    }

    public Unit getUnit() {
        return unit;
    }

    public void setUnit(Unit unit) {
        this.unit = unit;
    }

    public Short getEventingFlag() {
        return eventingFlag;
    }

    public void setEventingFlag(Short eventingFlag) {
        this.eventingFlag = eventingFlag;
        this.activeForEventing = eventingFlag == 1;
    }

    public boolean isActiveForEventing() {
        return activeForEventing;
    }

    public void setActiveForEventing(boolean activeForEventing) {
        this.eventingFlag = activeForEventing ? (short) 1 : (short) 0;
        this.activeForEventing = activeForEventing;
    }

    public static String generateSeriesLabel(Series s) {
        if (s == null) {
            return "n/a";
        }
        String label = String.format("%s %s, %s (%s)",
                s.getPhenomenon().getName(),
                s.getProcedure().getName(),
                s.getFeature().getName(),
                s.getUnit().getCode());

        return label;
    }

    public SeriesCheckAge getCheckAge() {
        return checkAge;
    }

    public void setCheckAge(SeriesCheckAge checkAge) {
        this.checkAge = checkAge;
    }

}
