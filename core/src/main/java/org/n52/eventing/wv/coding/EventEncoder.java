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
import org.n52.eventing.wv.model.Rule;
import org.n52.eventing.wv.model.WvEvent;
import org.n52.eventing.wv.model.WvEventMail;
import org.n52.eventing.wv.model.WvEventMessage;
import org.n52.eventing.wv.rest.WvCustomResources;
import org.n52.eventing.wv.services.EventTypesStore;
import org.n52.eventing.wv.view.EventTriggerView;
import org.n52.eventing.wv.view.EventView;
import org.n52.eventing.wv.view.IdHref;

/**
 *
 * @author <a href="mailto:m.rieke@52north.org">Matthes Rieke</a>
 */
public class EventEncoder extends ModelEncoder<WvEvent, EventView> {

    private final EventTypesStore eventTypesStore;
    private final I18nProvider i18n;
    private final String olderThan;
    private final String upToDate;
    private final String subscriptionCreated;
    private final String subscriptionCancelled;
    private final String eventingEnabled;
    private final String eventingDisabled;

    public EventEncoder(Context context, EventTypesStore store, I18nProvider i18n) {
        super(context);
        this.eventTypesStore = store;
        this.i18n = i18n;

        // pre-set string
        this.olderThan = i18n.getString("series.checkage.olderthan"); // "ist älter als"
        this.upToDate = i18n.getString("series.checkage.uptodate"); // "ist wieder aktuell"
        this.subscriptionCreated = i18n.getString("series.subscription.created"); //"Abonnement erstellt"
        this.subscriptionCancelled = i18n.getString("series.subscription.cancelled"); //"Abonnement abbestellt"
        this.eventingEnabled = i18n.getString("series.eventing.enabled"); // "Eventing aktiviert"
        this.eventingDisabled = i18n.getString("series.eventing.disabled"); // "Eventing aktiviert"
    }

    @Override
    public EventView encode(WvEvent model) throws IllegalStateException {
        if (model == null) {
            throw new IllegalStateException("source cannot be null");
        }

        EventView result = new EventView();
        result.setId(Integer.toString(model.getId(), 10));
        result.setTimestampCreated(model.getCreated().getTime());

        if (model.getTimestamp() != null) {
            result.setTimestamp(model.getTimestamp().getTime());
        } else {
            result.setTimestamp(model.getCreated().getTime());
        }

        Context ctx = getContext();

        int typeId = Integer.MIN_VALUE;
        if (model.getEventType() != null) {
            typeId = model.getEventType().getId();
            Optional<String> typeLabel = this.eventTypesStore.getLabelForId(model.getEventType().getId());
            if (typeLabel.isPresent() && ctx != null && ctx.getBaseUrl() != null) {
                result.setEventType(new IdHref(Integer.toString(typeId, 10),
                String.format("%s/%s/%s", ctx.getBaseUrl(), WvCustomResources.EVENTTYPE_RESOURCE, typeId)));
            }
        }

        if (model.getLogMails() != null) {
            Set<WvEventMail> logMails = model.getLogMails();

            // Optional<WvEventMail> logMailOpt = logMails.stream().filter(lm ->
            // lm.getNotificationLevel() != null || lm.getSubscription() !=
            // null).findFirst();
            for (WvEventMail logMail : logMails) {
                if (logMail.getNotificationLevel() != null || logMail.getSubscription() != null) {

                    result.setLabel(logMail.getContent());

                    if (logMail.getNotificationLevel() != null && ctx != null && ctx.getBaseUrl() != null) {
                        int nlId = logMail.getNotificationLevel().getId();
                        result.setNotificationLevel(new IdHref(Integer.toString(nlId, 10), String.format("%s/%s/%s",
                                ctx.getBaseUrl(), WvCustomResources.NOTIFICATIONLEVELS_RESOURCE, nlId)));
                    }

                    if (ctx != null && ctx.getBaseUrl() != null && ctx.isExpanded() && logMail.getSubscription() != null) {
                        addSubscription(logMail.getSubscription().getId(), result, ctx);
                    }
                }
            }

        }

        if (model.getSeries() != null && ctx != null && ctx.getBaseUrl() != null) {
            int seriesId = model.getSeries().getId();
            result.setPublication(new IdHref(Integer.toString(seriesId, 10),
                String.format("%s/%s/%s", ctx.getBaseUrl(), UrlSettings.PUBLICATIONS_RESOURCE, seriesId)));
        }

        encodeData(model, typeId, result, ctx);

        return result;
    }

    private void addSubscription(int subId, EventView result, Context ctx) {
        if (ctx.isExpanded()) {
            result.addSubscription(new IdHref(Integer.toString(subId, 10),
                    String.format("%s/%s/%s", ctx.getBaseUrl(), UrlSettings.SUBSCRIPTIONS_RESOURCE, subId)));
        }
    }

    private void encodeData(WvEvent model, int eventTypeId, EventView result, Context ctx) {
        // event types are hardcoded at the moment
        EventView.EventDetailsView data = new EventView.EventDetailsView();

        // Regelereignis
        if (eventTypeId == 1) {
            encodeTimestampAndValues(model, data);

            // the trigger
            EventTriggerView trigger = new EventTriggerView();
            Rule rule = model.getRule();
            trigger.setCode(rule.getTrendCode().getId());
            trigger.setLabel(rule.getTrendCode().getLabel());
            trigger.setThreshold(rule.getThreshold());
            trigger.setThresholdUnit(model.getSeries().getUnit().getCode());

            data.setEventTrigger(trigger);
        }

        // Alterskontrolle
        else if (eventTypeId == 3) {
            encodeTimestampAndValues(model, data);

            // the trigger
            EventTriggerView trigger = new EventTriggerView();
            if (model.getEventMessage() != null) {
                WvEventMessage msg = model.getEventMessage();
                switch (msg.getId()) {
                    case 3:
                        trigger.setCode(112);
                        trigger.setLabel(this.olderThan);
                        break;
                    case 4:
                        trigger.setCode(111);
                        trigger.setLabel(this.upToDate);
                        break;
                    default:
                        break;
                }
                if (model.getSeries().getCheckAge() != null) {
                    addThresold(trigger, model.getSeries().getCheckAge());
                }
            }
            data.setEventTrigger(trigger);
        }

        // Abonnement
        else if (eventTypeId == 4) {
            // the trigger
            EventTriggerView trigger = new EventTriggerView();
            if (model.getEventMessage() != null) {
                WvEventMessage msg = model.getEventMessage();
                switch (msg.getId()) {
                    case 5:
                        trigger.setCode(211);
                        trigger.setLabel(this.subscriptionCreated);
                        break;
                    case 6:
                        trigger.setCode(212);
                        trigger.setLabel(this.subscriptionCancelled);
                        break;
                    case 7:
                        trigger.setCode(213);
                        trigger.setLabel(this.subscriptionCreated);
                        break;
                    case 8:
                        trigger.setCode(214);
                        trigger.setLabel(this.subscriptionCancelled);
                        break;
                    default:
                        break;
                }

                if (model.getUser()!= null && ctx != null && ctx.getBaseUrl() != null) {
                    int uid = model.getUser().getId();
                    trigger.setUser(new IdHref(Integer.toString(uid, 10),
                        String.format("%s/%s/%s", ctx.getBaseUrl(), WvCustomResources.USERS_RESOURCE, uid)));
                }

                if (model.getGroup()!= null && ctx != null && ctx.getBaseUrl() != null) {
                    int gid = model.getGroup().getId();
                    trigger.setGroup(new IdHref(Integer.toString(gid, 10),
                        String.format("%s/%s/%s", ctx.getBaseUrl(), WvCustomResources.GROUPS_RESOURCE, gid)));
                }

                if (model.getNotification() != null && ctx != null && ctx.getBaseUrl() != null) {
                    int nid = model.getNotification().getId();
                    trigger.setNotification(new IdHref(Integer.toString(nid, 10),
                        String.format("%s/%s/%s", ctx.getBaseUrl(), UrlSettings.NOTIFICATIONS_RESOURCE, nid)));
                }
            }

            data.setEventTrigger(trigger);
        }

        // Zeitreihen-Verwaltung
        else if (eventTypeId == 2) {
            // the trigger
            EventTriggerView trigger = new EventTriggerView();
            if (model.getEventMessage() != null) {
                WvEventMessage msg = model.getEventMessage();
                switch (msg.getId()) {
                    case 1:
                        trigger.setCode(311);
                        trigger.setLabel(this.eventingEnabled);
                        break;
                    case 2:
                        trigger.setCode(312);
                        trigger.setLabel(this.eventingDisabled);
                        break;
                    default:
                        break;
                }

                data.setEventTrigger(trigger);
            }
        }

        result.setEventDetails(data);
    }

    private void encodeTimestampAndValues(WvEvent model, EventView.EventDetailsView data) {
        if (model.getPreviousTimestamp() != null) {
            data.setPreviousTimestamp(model.getPreviousTimestamp().getTime());
        }
        if (model.getPreviousValue() != null) {
            data.setPreviousValue(model.getPreviousValue());
        }
        if (model.getValue() != null) {
            data.setValue(model.getValue());
        }
        if (model.getSeries() != null && model.getSeries().getUnit() != null) {
            data.setUnit(model.getSeries().getUnit().getCode());
        }
    }

}
