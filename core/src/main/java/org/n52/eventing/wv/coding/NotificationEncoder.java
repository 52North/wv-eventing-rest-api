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

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import org.n52.eventing.rest.UrlSettings;
import org.n52.eventing.wv.dao.NotificationLevelDao;
import org.n52.eventing.wv.i18n.I18nProvider;
import org.n52.eventing.wv.model.Notification;
import org.n52.eventing.wv.model.NotificationLevel;
import org.n52.eventing.wv.model.NotificationRule;
import org.n52.eventing.wv.model.Series;
import org.n52.eventing.wv.model.SeriesCheckAge;
import org.n52.eventing.wv.view.IdHref;
import org.n52.eventing.wv.view.NotificationView;
import org.n52.eventing.wv.view.RuleView;

/**
 *
 * @author <a href="mailto:m.rieke@52north.org">Matthes Rieke</a>
 */
public class NotificationEncoder extends ModelEncoder<Notification, NotificationView> {

    private static final NumberFormat numberFormatter = DecimalFormat.getInstance(Locale.GERMAN);
    private final I18nProvider i18n;
    private NotificationLevelDao dao;
    private NotificationLevel infoLevel;
    private NotificationLevel warnLevel;
    private NotificationLevel unwarnLevel;

    public NotificationEncoder(Context context, I18nProvider i18n) {
        this(context, i18n, null);
    }

    public NotificationEncoder(Context context, I18nProvider i18n, NotificationLevelDao dao) {
        super(context);
        this.i18n = i18n;
        this.dao = dao;

        if (this.dao != null) {
            //TODO this is currently static to the domain table notification_level
            this.infoLevel = this.dao.retrieveById(0).get();
            this.warnLevel = this.dao.retrieveById(1).get();
            this.unwarnLevel = this.dao.retrieveById(9).get();
        }
    }

    @Override
    public NotificationView encode(Notification model) throws IllegalStateException {
        if (model == null) {
            throw new IllegalStateException("source cannot be null");
        }

        if (model.getSeries() == null) {
            throw new IllegalStateException("related series cannot be null");
        }

        NotificationView result = new NotificationView();
        result.setId(Integer.toString(model.getId(), 10));


        Optional<NotificationRule> primaryRuleOpt = model.getNotificationRules().stream().filter(nr -> nr.isPrimaryRule()).findFirst();
        if (primaryRuleOpt.isPresent()) {
            NotificationRule primaryRule = primaryRuleOpt.get();
            String label = createLabelFromNotificationRule(primaryRule);
            result.setLabel(label);
        }

        // workaround for #11: set the label to empty string
        if (result.getLabel() == null) {
            result.setLabel("");
        }


        if (getContext() != null && !getContext().isExpanded() && getContext().getBaseUrl() != null) {
            result.setHref(String.format("%s/%s/%s",
                    getContext().getBaseUrl(), UrlSettings.NOTIFICATIONS_RESOURCE, model.getId()));
        }
//        else {
            Context ruleContext;
            if (getContext() != null) {
                ruleContext = new Context(getContext().getBaseUrl(), true, true);
            }
            else {
                ruleContext = new Context(null, true, false);
            }
            result.setPublication(new IdHref(Integer.toString(model.getSeries().getId(), 10),
                String.format("%s/%s/%s",
                    getContext().getBaseUrl(), UrlSettings.PUBLICATIONS_RESOURCE, model.getSeries().getId())));

            RuleEncoder ruleEncoder = new RuleEncoder(ruleContext, i18n);
            List<RuleView> rules = model.getNotificationRules().stream()
                .filter(nr -> nr.getNotification() != null)
                .map(nr -> {
                    RuleView rule = ruleEncoder.encode(nr.getRule(), nr);
                    return rule;
                })
                .collect(Collectors.toList());

            // add custom rules as required
            //TODO: Alterskontrolle: Wenn zu einer Zeitreihe ein Eintrag in check_age besteht. Immer Warnung und Entwarnung.
            if (model.getSeries().getCheckAge() != null) {
                SeriesCheckAge checkAge = model.getSeries().getCheckAge();
                if (checkAge.getCheckInterval() != null) {
                    rules.addAll(ruleEncoder.createCheckAgeRules(model, warnLevel, unwarnLevel));
                }
            }

            //TODO: Zeitreihen-Verwaltung (Eventing Ein/Aus): Hardcodiert. Immer Warnung und Entwarnung.
            rules.addAll(ruleEncoder.createManagementRules(model, infoLevel));

            result.setRules(rules);
//        }

        return result;
    }

    public static String createLabelFromNotificationRule(NotificationRule primaryRule) {
        Series series = primaryRule.getNotification().getSeries();
        String label = String.format("%s, %s, %s, %s %s %s",
                series.getPhenomenon().getName(),
                series.getFeature().getName(),
                series.getProcedure().getName(),
                primaryRule.getRule().getTrendCode().getLabel(),
                numberFormatter.format(primaryRule.getRule().getThreshold()),
                series.getUnit().getCode());
        return label;
    }

}
