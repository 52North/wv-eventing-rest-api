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
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.n52.eventing.wv.coding;

import org.n52.eventing.wv.model.SeriesCheckAge;
import org.n52.eventing.wv.view.EventTriggerView;

/**
 *
 * @author <a href="mailto:m.rieke@52north.org">Matthes Rieke</a>
 * @param <S> the source class
 * @param <T> the target class
 */
public abstract class ModelEncoder<S, T> {

    private final Context context;

    public ModelEncoder() {
        this.context = null;
    }

    public ModelEncoder(Context context) {
        this.context = context;
    }

    public Context getContext() {
        return context;
    }

    public abstract T encode(S model) throws IllegalStateException;

    public void addThresold(EventTriggerView onTrigger, SeriesCheckAge checkAge) {
        onTrigger.setThresholdUnit(checkAge.getUnit().getCode());
        switch (onTrigger.getThresholdUnit()) {
            case "h":
            case "hh":
            case "hour":
                onTrigger.setThreshold(checkAge.getCheckInterval().toStandardHours().getHours());
                break;
            case "min":
            case "mm":
                onTrigger.setThreshold(checkAge.getCheckInterval().toStandardMinutes().getMinutes());
                break;
            default:
                onTrigger.setThreshold(checkAge.getCheckInterval().toStandardMinutes().getMinutes());
                break;
        }
    }

}
