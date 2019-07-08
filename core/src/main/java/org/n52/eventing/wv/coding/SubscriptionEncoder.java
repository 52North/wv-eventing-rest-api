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
package org.n52.eventing.wv.coding;

import java.util.Optional;
import java.util.Set;
import org.n52.eventing.rest.UrlSettings;
import org.n52.eventing.wv.i18n.I18nProvider;
import org.n52.eventing.wv.model.NotificationRule;
import org.n52.eventing.wv.model.WvSubscription;
import org.n52.eventing.wv.view.SubscriptionView;

/**
 *
 * @author <a href="mailto:m.rieke@52north.org">Matthes Rieke</a>
 */
public class SubscriptionEncoder extends ModelEncoder<WvSubscription, SubscriptionView> {

    private final I18nProvider i18n;

    public SubscriptionEncoder(Context context, I18nProvider i18n) {
        super(context);
        this.i18n = i18n;
    }

    @Override
    public SubscriptionView encode(WvSubscription model) throws IllegalStateException {
        if (model == null) {
            throw new IllegalStateException("source cannot be null");
        }

        if (model.getNotification() == null) {
            throw new IllegalStateException("related notification cannot be null");
        }

        SubscriptionView result = new SubscriptionView();
        result.setId(Integer.toString(model.getId(), 10));

        Context asRefContext = new Context(getContext().getBaseUrl(), false, true);

        if (model.getUser() != null) {
            result.setUser(new UserEncoder(asRefContext).encode(model.getUser()));
        }

        if (model.getGroup() != null) {
            result.setGroup(new GroupEncoder(asRefContext).encode(model.getGroup()));
        }

        // create the label from the primary rule
//        Set<NotificationRule> rules = model.getNotification().getNotificationRules();
//        if (rules != null) {
//            Optional<NotificationRule> primary = rules.stream().filter(r -> r.isPrimaryRule()).findAny();
//            if (primary.isPresent()) {
//                result.setLabel(NotificationEncoder.createLabelFromNotificationRule(primary.get()));
//            }
//        }
//        if (result.getLabel() == null) {
//            result.setLabel("");
//        }

        result.setNotification(new NotificationEncoder(asRefContext, i18n).encode(model.getNotification()));

        // set a default delivery method
        EmailDeliveryMethodView edmv = new EmailDeliveryMethodView();
        if (getContext() != null && getContext().getBaseUrl() != null) {
            edmv.setHref(String.format("%s/%s/%s",
                getContext().getBaseUrl(),
                    UrlSettings.DELIVERY_METHODS_RESOURCE, edmv.getId()));

            result.setHref(String.format("%s/%s/%s",
                getContext().getBaseUrl(),
                    UrlSettings.SUBSCRIPTIONS_RESOURCE, model.getId()));
        }
        result.setDeliveryMethod(edmv);

        return result;
    }

}
