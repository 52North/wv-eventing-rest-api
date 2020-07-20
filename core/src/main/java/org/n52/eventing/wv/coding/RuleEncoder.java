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
package org.n52.eventing.wv.coding;

import java.util.ArrayList;
import java.util.List;

import org.n52.eventing.wv.i18n.I18nProvider;
import org.n52.eventing.wv.model.Notification;
import org.n52.eventing.wv.model.NotificationLevel;
import org.n52.eventing.wv.model.NotificationRule;
import org.n52.eventing.wv.model.Rule;
import org.n52.eventing.wv.rest.WvCustomResources;
import org.n52.eventing.wv.view.EventTriggerView;
import org.n52.eventing.wv.view.IdHref;
import org.n52.eventing.wv.view.RuleView;

/**
 *
 * @author <a href="mailto:m.rieke@52north.org">Matthes Rieke</a>
 */
public class RuleEncoder extends ModelEncoder<Rule, RuleView> {

    private final I18nProvider i18n;
    private final String olderThan;
    private final String upToDate;
    private final String subscriptionCreated;
    private final String subscriptionCancelled;
    private final String eventingEnabled;
    private final String eventingDisabled;
    private final String ruleEvent;

    public RuleEncoder(Context context, I18nProvider i18n) {
        super(context);
        this.i18n = i18n;

        // pre-set string
        this.olderThan = i18n.getString("series.checkage.olderthan"); // "ist älter als"
        this.upToDate = i18n.getString("series.checkage.uptodate"); // "ist wieder aktuell"
        this.subscriptionCreated = i18n.getString("series.subscription.created"); //"Abonnement erstellt"
        this.subscriptionCancelled = i18n.getString("series.subscription.cancelled"); //"Abonnement abbestellt"
        this.eventingEnabled = i18n.getString("series.eventing.enabled"); // "Eventing aktiviert"
        this.eventingDisabled = i18n.getString("series.eventing.disabled"); // "Eventing aktiviert"
        this.ruleEvent = i18n.getString("series.ruleevent"); // "Regelereignis"
    }

    @Override
    public RuleView encode(Rule model) throws IllegalStateException {
        return encode(model, null);
    }

    public RuleView encode(Rule model, NotificationRule notificationRule) throws IllegalStateException {
        if (model == null) {
            throw new IllegalStateException("source cannot be null");
        }

        RuleView result = new RuleView();
        result.setId(model.getId());

        if (notificationRule != null && notificationRule.isPrimaryRule()) {
            result.setPrimaryRule(true);
        }

        if (getContext() != null && getContext().getBaseUrl() != null) {
            result.setHref(String.format("%s/%s/%s",
                    getContext().getBaseUrl(),
                    WvCustomResources.RULES_RESOURCE,
                    model.getId()));

            if (notificationRule != null) {
                IdHref nl = new IdHref(Integer.toString(notificationRule.getLevel().getId(), 10),
                        String.format("%s/%s/%s",
                                getContext().getBaseUrl(),
                                WvCustomResources.NOTIFICATIONLEVELS_RESOURCE,
                                notificationRule.getLevel().getId()));
                if (notificationRule.getLevel().getLabel() != null) {
                    nl.setLabel(notificationRule.getLevel().getLabel());
                }
                result.setNotificationLevel(nl);
            }

            // hardcoded event types
            IdHref eventType = new IdHref("1",
                    String.format("%s/%s/%s",
                            getContext().getBaseUrl(),
                            WvCustomResources.EVENTTYPE_RESOURCE, 1));
            eventType.setLabel(this.ruleEvent);
            result.setEventType(eventType);

            EventTriggerView trigger = new EventTriggerView();
            trigger.setCode(model.getTrendCode().getId());
            trigger.setLabel(model.getTrendCode().getLabel());
            trigger.setThreshold(model.getThreshold());
            trigger.setThresholdUnit(model.getSeries().getUnit().getCode());
            // END hardcoded event types

            result.setEventTrigger(trigger);
        }



        return result;
    }

    public List<RuleView> createCheckAgeRules(Notification model, NotificationLevel warnLevel, NotificationLevel unwarnLevel) {
        IdHref eventType = new IdHref("3",
                    String.format("%s/%s/%s",
                            getContext().getBaseUrl(),
                            WvCustomResources.EVENTTYPE_RESOURCE, 3));
        eventType.setLabel("Alterskontrolle");
        List<RuleView> result = new ArrayList<>();

        RuleView onRule = new RuleView();
        onRule.setEventType(eventType);
        EventTriggerView onTrigger = new EventTriggerView();
        onTrigger.setCode(111);
        onTrigger.setLabel(this.upToDate);
        addThresold(onTrigger, model.getSeries().getCheckAge());
        onRule.setEventTrigger(onTrigger);

        if (unwarnLevel != null) {
            IdHref nl = new IdHref(Integer.toString(unwarnLevel.getId(), 10),
                    String.format("%s/%s/%s",
                            getContext().getBaseUrl(),
                            WvCustomResources.NOTIFICATIONLEVELS_RESOURCE,
                            unwarnLevel.getId()));
            if (unwarnLevel.getLabel() != null) {
                nl.setLabel(unwarnLevel.getLabel());
            }
            onRule.setNotificationLevel(nl);
        }


        RuleView offRule = new RuleView();
        offRule.setEventType(eventType);
        EventTriggerView ofTrigger = new EventTriggerView();
        ofTrigger.setCode(112);
        ofTrigger.setLabel(this.olderThan);
        addThresold(ofTrigger, model.getSeries().getCheckAge());
        offRule.setEventTrigger(ofTrigger);

        if (warnLevel != null) {
            IdHref nl2 = new IdHref(Integer.toString(warnLevel.getId(), 10),
                    String.format("%s/%s/%s",
                            getContext().getBaseUrl(),
                            WvCustomResources.NOTIFICATIONLEVELS_RESOURCE,
                            warnLevel.getId()));
            if (warnLevel.getLabel() != null) {
                nl2.setLabel(warnLevel.getLabel());
            }
            offRule.setNotificationLevel(nl2);
        }


        result.add(onRule);
        result.add(offRule);

        return result;
    }

    public List<RuleView> createManagementRules(Notification model, NotificationLevel infoLevel) {
        IdHref eventType = new IdHref("2",
                    String.format("%s/%s/%s",
                            getContext().getBaseUrl(),
                            WvCustomResources.EVENTTYPE_RESOURCE, 2));
        eventType.setLabel("Zeitreihen-Verwaltung");
        List<RuleView> result = new ArrayList<>();

        RuleView onRule = new RuleView();
        onRule.setEventType(eventType);
        EventTriggerView onTrigger = new EventTriggerView();
        onTrigger.setCode(311);
        onTrigger.setLabel(this.eventingEnabled);
        onRule.setEventTrigger(onTrigger);

        RuleView offRule = new RuleView();
        offRule.setEventType(eventType);
        EventTriggerView ofTrigger = new EventTriggerView();
        ofTrigger.setCode(312);
        ofTrigger.setLabel(this.eventingDisabled);
        offRule.setEventTrigger(ofTrigger);

        if (infoLevel != null) {
            IdHref nl = new IdHref(Integer.toString(infoLevel.getId(), 10),
                    String.format("%s/%s/%s",
                            getContext().getBaseUrl(),
                            WvCustomResources.NOTIFICATIONLEVELS_RESOURCE,
                            infoLevel.getId()));

            if (infoLevel.getLabel() != null) {
                nl.setLabel(infoLevel.getLabel());
            }

            onRule.setNotificationLevel(nl);
            offRule.setNotificationLevel(nl);
        }


        result.add(onRule);
        result.add(offRule);

        return result;
    }


}
