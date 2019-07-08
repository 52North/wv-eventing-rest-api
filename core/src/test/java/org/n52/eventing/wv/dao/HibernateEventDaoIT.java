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
package org.n52.eventing.wv.dao;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import org.hamcrest.CoreMatchers;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.n52.eventing.wv.dao.hibernate.HibernateCategoryDao;
import org.n52.eventing.wv.dao.hibernate.HibernateEventDao;
import org.n52.eventing.wv.dao.hibernate.HibernateEventTypeDao;
import org.n52.eventing.wv.dao.hibernate.HibernateFeatureOfInterestDao;
import org.n52.eventing.wv.dao.hibernate.HibernateGroupDao;
import org.n52.eventing.wv.dao.hibernate.HibernateNotificationDao;
import org.n52.eventing.wv.dao.hibernate.HibernateNotificationLevelDao;
import org.n52.eventing.wv.dao.hibernate.HibernateNotificationRuleDao;
import org.n52.eventing.wv.dao.hibernate.HibernatePhenomenonDao;
import org.n52.eventing.wv.dao.hibernate.HibernateProcedureDao;
import org.n52.eventing.wv.dao.hibernate.HibernateRuleDao;
import org.n52.eventing.wv.dao.hibernate.HibernateSeriesDao;
import org.n52.eventing.wv.dao.hibernate.HibernateSubscriptionDao;
import org.n52.eventing.wv.dao.hibernate.HibernateTrendDao;
import org.n52.eventing.wv.dao.hibernate.HibernateUnitDao;
import org.n52.eventing.wv.database.HibernateDatabaseConnection;
import org.n52.eventing.wv.model.Category;
import org.n52.eventing.wv.model.FeatureOfInterest;
import org.n52.eventing.wv.model.Group;
import org.n52.eventing.wv.model.Notification;
import org.n52.eventing.wv.model.NotificationRule;
import org.n52.eventing.wv.model.Phenomenon;
import org.n52.eventing.wv.model.Procedure;
import org.n52.eventing.wv.model.Rule;
import org.n52.eventing.wv.model.Series;
import org.n52.eventing.wv.model.Unit;
import org.n52.eventing.wv.model.WvEvent;
import org.n52.eventing.wv.model.WvEventMessage;
import org.n52.eventing.wv.model.WvEventSubscription;
import org.n52.eventing.wv.model.WvSubscription;
import org.n52.eventing.wv.security.GroupPolicies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:m.rieke@52north.org">Matthes Rieke</a>
 */
@RunWith(Parameterized.class)
public class HibernateEventDaoIT {

    private static final Logger LOG = LoggerFactory.getLogger(HibernateEventDaoIT.class.getName());

    private Session session;

    private Series createdSeries;
    private Rule createdRule;
    private Notification createdNotification;
    private NotificationRule createdNotificationRule;
    private WvSubscription createdSubscription;
    private FeatureOfInterest createdFeature;
    private WvEvent createdEvent1;
    private WvEvent createdEvent2;

    @Parameterized.Parameters
    public static List<Object[]> data() throws Exception {
        HibernateDatabaseConnection hdc = new HibernateDatabaseConnection();
        hdc.afterPropertiesSet();
        Object[][] params = new Object[100][1];
        for (int i = 0; i < params.length; i++) {
            params[i][0] = hdc;
        }
        return Arrays.asList(params);
    }

    @Parameter(0)
    public HibernateDatabaseConnection hdc;

    @Before
    public void setup() throws Exception {
        this.session = hdc.createSession();
    }

    @After
    public void cleanup() throws DatabaseException {
        Transaction trans = session.beginTransaction();
        if (this.createdSubscription != null) {
            new HibernateSubscriptionDao(session).remove(createdSubscription, true);
        }
        if (this.createdNotificationRule != null) {
            new HibernateNotificationRuleDao(session).remove(createdNotificationRule, true);
        }
        if (this.createdNotification != null) {
            new HibernateNotificationDao(session).remove(createdNotification, true);
        }
        if (this.createdEvent1 != null) {
            new HibernateEventDao(session).remove(createdEvent1, true);
        }
        if (this.createdEvent2 != null) {
            new HibernateEventDao(session).remove(createdEvent2, true);
        }
        if (this.createdRule != null) {
            new HibernateRuleDao(session).remove(createdRule, true);
        }
        if (this.createdSeries != null) {
            new HibernateSeriesDao(session).remove(createdSeries, true);
        }
        if (this.createdFeature != null) {
            new HibernateFeatureOfInterestDao(session).remove(createdFeature, true);
        }
        trans.commit();

        this.session.close();
    }

    /**
SELECT
  event_log.time_stamp_created,
  event_log.event_type_pkid,
  event_log.observation_time_stamp,
  event_log.observation_numeric_value
FROM
  sensorweb2.subscription sub,
  sensorweb2.notification_rule,
  sensorweb2.rule,
  sensorweb2.notification,
  sensorweb2.event_log
WHERE
  sub.notification_pkid = notification.pkid AND
  notification.pkid = notification_rule.notification_pkid AND
  notification_rule.rule_pkid = rule.pkid AND
  rule.pkid = event_log.rule_pkid AND
  sub.pkid = 3;

     * @throws DatabaseException
     */

    @Test
    public void testRetrievalBySubscription() throws DatabaseException {
        HibernateEventDao dao = new HibernateEventDao(session);
        HibernateSubscriptionDao subDao = new HibernateSubscriptionDao(session);
        HibernateSeriesDao seriesDao = new HibernateSeriesDao(session);
        HibernateRuleDao ruleDao = new HibernateRuleDao(session);

        try {
            Transaction trans = session.beginTransaction();
            WvSubscription sub1 = createNewSubscription(seriesDao, ruleDao, 67);
            subDao.store(sub1, true);

            WvSubscription sub2 = createNewSubscription(seriesDao, ruleDao, 68);
            subDao.store(sub2, true);

            this.createdEvent1 = new WvEvent(sub1.getNotification().getNotificationRules().iterator().next().getRule(), new Date(), 5.0, new Date(), 4.0);
            createdEvent1.setCreated(new Date());
            createdEvent1.setSeries(createdSeries);
            createdEvent1.setEventType(new HibernateEventTypeDao(session).retrieveById(4).get());
            WvEventMessage msg1 = new WvEventMessage();
            msg1.setMessageText("test message event 1");
            msg1.setName("event-1-msg");
            createdEvent1.setEventMessage(msg1);

            this.createdEvent2 = new WvEvent(sub2.getNotification().getNotificationRules().iterator().next().getRule(), new Date(), 3.0, new Date(), 2.0);
            createdEvent2.setCreated(new Date());
            createdEvent2.setSeries(createdSeries);
            createdEvent2.setEventType(new HibernateEventTypeDao(session).retrieveById(4).get());
            WvEventMessage msg2 = new WvEventMessage();
            msg2.setMessageText("test message event 2");
            msg2.setName("event-2-msg");
            createdEvent2.setEventMessage(msg2);

            dao.store(createdEvent1, true);
            dao.store(createdEvent2, true);

            trans.commit();

            List<WvEvent> sub1events = dao.retrieveForSubscription(sub1.getId()).getResult();
            List<WvEvent> sub2events = dao.retrieveForSubscription(sub2.getId()).getResult();
            List<WvEvent> subAllevents = dao.retrieveForSubscription(sub1.getId(), sub2.getId()).getResult();

            Assert.assertThat(sub1events.size(), CoreMatchers.is(1));
            Assert.assertThat(sub2events.size(), CoreMatchers.is(1));
            Assert.assertThat(subAllevents.size(), CoreMatchers.is(2));

            Assert.assertThat(sub1events.get(0).getValue(), CoreMatchers.is(5.0));
            Assert.assertThat(sub1events.get(0).getPreviousValue(), CoreMatchers.is(4.0));
            Assert.assertThat(sub1events.get(0).getEventMessage().getName(), CoreMatchers.equalTo("event-1-msg"));

            Assert.assertThat(sub2events.get(0).getValue(), CoreMatchers.is(3.0));
            Assert.assertThat(sub2events.get(0).getPreviousValue(), CoreMatchers.is(2.0));
            Assert.assertThat(sub2events.get(0).getEventMessage().getName(), CoreMatchers.equalTo("event-2-msg"));

            // now with custom filter method
            Map<String, String[]> filter = new HashMap<>();
            filter.put("publication", new String[] {Integer.toString(createdSeries.getId())});
            filter.put("subscriptions", new String[] {Integer.toString(sub1.getId())});

            List<WvEvent> subEventsFiltered = dao.retrieveWithFilter(filter, null).getResult();
            Assert.assertThat(subEventsFiltered.size(), CoreMatchers.is(1));
            Assert.assertThat(subEventsFiltered.get(0).getValue(), CoreMatchers.is(5.0));
            Assert.assertThat(subEventsFiltered.get(0).getPreviousValue(), CoreMatchers.is(4.0));
            Assert.assertThat(subEventsFiltered.get(0).getEventMessage().getName(), CoreMatchers.equalTo("event-1-msg"));

            filter = new HashMap<>();
            filter.put("publication", new String[] {Integer.toString(createdSeries.getId() + 50000)});
            filter.put("subscriptions", new String[] {Integer.toString(sub1.getId())});

            subEventsFiltered = dao.retrieveWithFilter(filter, null).getResult();
            Assert.assertThat(subEventsFiltered.size(), CoreMatchers.is(0));

            filter = new HashMap<>();
            filter.put("publication", new String[] {Integer.toString(createdSeries.getId(), createdSeries.getId() + 50000)});
            filter.put("subscriptions", new String[] {Integer.toString(sub1.getId())});

            subEventsFiltered = dao.retrieveWithFilter(filter, null).getResult();
            Assert.assertThat(subEventsFiltered.size(), CoreMatchers.is(1));
            Assert.assertThat(subEventsFiltered.get(0).getValue(), CoreMatchers.is(5.0));
            Assert.assertThat(subEventsFiltered.get(0).getPreviousValue(), CoreMatchers.is(4.0));
            Assert.assertThat(subEventsFiltered.get(0).getEventMessage().getName(), CoreMatchers.equalTo("event-1-msg"));
        }
        catch (DatabaseException | RuntimeException e) {
            LOG.warn(e.getMessage(), e);
            throw e;
        }

    }

    private WvSubscription createNewSubscription(HibernateSeriesDao seriesDao, HibernateRuleDao ruleDao, int trendCode) throws DatabaseException {
        this.createdSeries = new Series();
        createdSeries.setCategory(createCategory("test-category"));
        createdSeries.setPhenomenon(createPhenomenon("test-phenomenon"));
        createdSeries.setProcedure(createProcedure("test-procedure"));

        this.createdFeature = new FeatureOfInterest("test-feature-"+UUID.randomUUID().toString().substring(0, 6),
                "Test Feature", "point", new Random().nextInt(), "its not a bug");
        new HibernateFeatureOfInterestDao(session).store(createdFeature, true);

        createdSeries.setFeature(createdFeature);
        createdSeries.setUnit(createUnit("cm"));
        seriesDao.store(createdSeries, true);

        this.createdRule = new Rule(22.0, new HibernateTrendDao(session).retrieveByDomainTrend(TrendDao.DomainTrend.LessEqual).get(), 1, createdSeries);
        ruleDao.store(createdRule, true);

        // notification
        this.createdNotification = new Notification();
        createdNotification.setSeries(createdSeries);

        this.createdNotificationRule = new NotificationRule();
        createdNotificationRule.setRule(createdRule);
        createdNotificationRule.setPrimaryRule(true);
        createdNotificationRule.setLevel(new HibernateNotificationLevelDao(session).retrieveById(1).get());
        createdNotificationRule.setNotification(createdNotification);
        createdNotification.setNotificationRules(Collections.singleton(createdNotificationRule));

        new HibernateNotificationDao(session).store(createdNotification, true);
        new HibernateNotificationRuleDao(session).store(createdNotificationRule, true);

        HibernateGroupDao groupDao = new HibernateGroupDao(session,new GroupPolicies());
        Group group = groupDao.retrieve(null).getResult().get(0);

        this.createdSubscription = new WvSubscription(createdNotification);
        createdSubscription.setGroup(group);
        return createdSubscription;
    }

    private Category createCategory(String name) throws DatabaseException {
        HibernateCategoryDao dao = new HibernateCategoryDao(session);
        if (dao.exists(name)) {
            return dao.retrieveByName(name).get();
        }
        Category r = new Category(name);
        dao.store(r, true);

        return r;
    }

    private Phenomenon createPhenomenon(String name) throws DatabaseException {
        HibernatePhenomenonDao dao = new HibernatePhenomenonDao(session);
        if (dao.exists(name)) {
            return dao.retrieveByName(name).get();
        }
        Phenomenon r = new Phenomenon(name);
        dao.store(r, true);

        return r;
    }

    private Procedure createProcedure(String name) throws DatabaseException {
        HibernateProcedureDao dao = new HibernateProcedureDao(session);
        if (dao.exists(name)) {
            return dao.retrieveByName(name).get();
        }
        Procedure r = new Procedure(name);
        dao.store(r, true);

        return r;
    }

    private Unit createUnit(String name) throws DatabaseException {
        HibernateUnitDao dao = new HibernateUnitDao(session);
        Optional<Unit> unit = dao.retrieveByCode(name);
        if (unit.isPresent()) {
            return unit.get();
        }
        Unit r = new Unit(name);
        dao.store(r, true);

        return r;
    }

}
