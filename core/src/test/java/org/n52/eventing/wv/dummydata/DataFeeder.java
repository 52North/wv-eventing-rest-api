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
package org.n52.eventing.wv.dummydata;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.hibernate.Session;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.n52.eventing.wv.dao.DatabaseException;
import org.n52.eventing.wv.dao.TrendDao;
import org.n52.eventing.wv.dao.hibernate.HibernateCategoryDao;
import org.n52.eventing.wv.dao.hibernate.HibernateEventDao;
import org.n52.eventing.wv.dao.hibernate.HibernateEventMessageDao;
import org.n52.eventing.wv.dao.hibernate.HibernateEventTypeDao;
import org.n52.eventing.wv.dao.hibernate.HibernateFeatureOfInterestDao;
import org.n52.eventing.wv.dao.hibernate.HibernateGroupDao;
import org.n52.eventing.wv.dao.hibernate.HibernateNotificationDao;
import org.n52.eventing.wv.dao.hibernate.HibernateNotificationLevelDao;
import org.n52.eventing.wv.dao.hibernate.HibernatePhenomenonDao;
import org.n52.eventing.wv.dao.hibernate.HibernateProcedureDao;
import org.n52.eventing.wv.dao.hibernate.HibernateRuleDao;
import org.n52.eventing.wv.dao.hibernate.HibernateSeriesDao;
import org.n52.eventing.wv.dao.hibernate.HibernateSubscriptionDao;
import org.n52.eventing.wv.dao.hibernate.HibernateTrendDao;
import org.n52.eventing.wv.dao.hibernate.HibernateUnitDao;
import org.n52.eventing.wv.dao.hibernate.HibernateUserDao;
import org.n52.eventing.wv.database.HibernateDatabaseConnection;
import org.n52.eventing.wv.model.Category;
import org.n52.eventing.wv.model.FeatureOfInterest;
import org.n52.eventing.wv.model.Notification;
import org.n52.eventing.wv.model.NotificationRule;
import org.n52.eventing.wv.model.Phenomenon;
import org.n52.eventing.wv.model.Procedure;
import org.n52.eventing.wv.model.Rule;
import org.n52.eventing.wv.model.Series;
import org.n52.eventing.wv.model.SeriesCheckAge;
import org.n52.eventing.wv.model.Unit;
import org.n52.eventing.wv.model.WvEvent;
import org.n52.eventing.wv.model.WvEventMail;
import org.n52.eventing.wv.model.WvEventMessage;
import org.n52.eventing.wv.model.WvEventType;
import org.n52.eventing.wv.model.WvSubscription;
import org.n52.eventing.wv.model.WvUser;
import org.n52.eventing.wv.security.GroupPolicies;

/**
 *
 * @author <a href="mailto:m.rieke@52north.org">Matthes Rieke</a>
 */
public class DataFeeder {

    public HibernateDatabaseConnection hdc;
    private Session session;
    private HibernateEventDao eventDao;
    private HibernateSeriesDao seriesDao;
    private HibernateFeatureOfInterestDao featureDao;
    private HibernatePhenomenonDao phenoDao;
    private HibernateProcedureDao procedureDao;
    private HibernateUnitDao unitDao;
    private HibernateCategoryDao categoryDao;
    private HibernateUserDao userDao;
    private HibernateGroupDao groupDao;
    private HibernateTrendDao trendDao;
    private HibernateNotificationLevelDao notiLevelDao;
    private HibernateNotificationDao notiDao;
    private HibernateRuleDao ruleDao;

    public static void main(String[] args) throws Exception {
        new DataFeeder().feed();
    }
    private HibernateEventTypeDao eventTypeDao;
    private HibernateEventMessageDao eventMessageDao;
    private HibernateSubscriptionDao subscriptionDao;

    public void configure() {

    }

    public void feed() throws Exception {
        this.setupDbConnection();

        // set up phenomena, procedure and features
        this.createBaselineData();
    }

    private void setupDbConnection() throws Exception {
        this.hdc = new HibernateDatabaseConnection();
        this.hdc.afterPropertiesSet();
        this.session = this.hdc.createSession();
        this.eventDao = new HibernateEventDao(session);
        this.seriesDao = new HibernateSeriesDao(session);
        this.featureDao = new HibernateFeatureOfInterestDao(session);
        this.phenoDao = new HibernatePhenomenonDao(session);
        this.procedureDao = new HibernateProcedureDao(session);
        this.unitDao = new HibernateUnitDao(session);
        this.categoryDao = new HibernateCategoryDao(session);
        GroupPolicies gp = new GroupPolicies();
        this.userDao = new HibernateUserDao(session, gp);
        this.groupDao = new HibernateGroupDao(session, gp);
        this.trendDao = new HibernateTrendDao(session);
        this.notiLevelDao = new HibernateNotificationLevelDao(session);
        this.notiDao = new HibernateNotificationDao(session);
        this.ruleDao = new HibernateRuleDao(session);
        this.eventTypeDao = new HibernateEventTypeDao(session);
        this.eventMessageDao = new HibernateEventMessageDao(session);
        this.subscriptionDao = new HibernateSubscriptionDao(session);

//        WvEventType et1 = new WvEventType();
//        et1.setDescription("Regelereignis");
//        et1.setId(1);
//        et1.setName(et1.getDescription());
//        this.eventTypeDao.store(et1);
//
//        WvEventType et2 = new WvEventType();
//        et2.setDescription("Alterskontrolle");
//        et2.setId(2);
//        et2.setName(et2.getDescription());
//        this.eventTypeDao.store(et2);
//
//        WvEventType et3 = new WvEventType();
//        et3.setDescription("Abonnement");
//        et3.setId(3);
//        et3.setName(et3.getDescription());
//        this.eventTypeDao.store(et3);
//
//        WvEventType et4 = new WvEventType();
//        et4.setDescription("Zeitreihen-Verwaltung");
//        et4.setId(4);
//        et4.setName(et1.getDescription());
//        this.eventTypeDao.store(et4);
    }

    private void createBaselineData() throws DatabaseException {
        Unit targetUnit = this.unitDao.retrieve(null).getResult().get(0);
        Category targetCat = this.categoryDao.retrieve(null).getResult().get(0);
        WvUser targetUser = this.userDao.retrieveByName("matthes").get();
        for (int i = 0; i < 2; i++) {
            Phenomenon phen = new Phenomenon();
            phen.setName("phenomenon-" + UUID.randomUUID().toString().substring(0, 6));
            phen.setPhenomenonId(phen.getName());
            this.phenoDao.store(phen);

            Procedure proc = new Procedure();
            proc.setName("procedure-" + UUID.randomUUID().toString().substring(0, 6));
            proc.setProcedureId(proc.getName());
            this.procedureDao.store(proc);

            FeatureOfInterest feat = new FeatureOfInterest();
            feat.setName("feature-" + UUID.randomUUID().toString().substring(0, 6));
            feat.setIdentifier(feat.getName());
            feat.setReferenceId(100 + i);
            feat.setLabel("A test feature with name: "+ feat.getName());
            this.featureDao.store(feat);

            Series series = new Series();
            series.setCategory(targetCat);
            series.setUnit(targetUnit);
            series.setEventingFlag((short) 1);
            series.setFeature(feat);
            series.setPhenomenon(phen);
            series.setProcedure(proc);
            this.seriesDao.store(series);
            if (i % 4 == 0) {
                SeriesCheckAge checkAge = new SeriesCheckAge();
                checkAge.setCheckInterval(new Period(0,55,0,0));
                checkAge.setMessageGeneratedOn(new DateTime().minusDays(1).toDate());
                checkAge.setSeries(series);
                checkAge.setId(series.getId());
                series.setCheckAge(checkAge);
                session.save(checkAge);
            }

            Notification noti = this.createNotification(series);

            WvSubscription sub = new WvSubscription();
            sub.setNotification(noti);
            sub.setUser(targetUser);
            this.subscriptionDao.store(sub);

            // create a set of events
            this.createEventsForSeries(series, noti, targetUser, sub);
        }
    }

    private void createEventsForSeries(Series series, Notification noti, WvUser user, WvSubscription sub) throws DatabaseException {
        Rule rule = noti.getNotificationRules().iterator().next().getRule();

        WvEventType subManagementType = this.eventTypeDao.retrieveById(3).get();
        WvEventType ageType = this.eventTypeDao.retrieveById(2).get();
        WvEventType ruleType = this.eventTypeDao.retrieveById(1).get();

        WvEventMessage subOnMsg = this.eventMessageDao.retrieveById(5).get();
        WvEventMessage subOffMsg = this.eventMessageDao.retrieveById(6).get();
        WvEventMessage outdated = this.eventMessageDao.retrieveById(3).get();
        WvEventMessage uptodate = this.eventMessageDao.retrieveById(4).get();

        double preValue = rule.getThreshold() - 1;
        int total = 500;
        DateTime preTimestamp = new DateTime().minusHours(total);
        for (int j = 1; j < total + 1; j++) {
            WvEvent ev = new WvEvent();
            ev.setUser(user);

            ev.setSeries(series);
            ev.setRule(rule);
            ev.setNotification(noti);

            // first: enable sub
            if (j == 1) {
                ev.setEventType(subManagementType);
                ev.setEventMessage(subOnMsg);
            }
            else if (j == total) {
                // last: cancel sub
                ev.setEventType(subManagementType);
                ev.setEventMessage(subOffMsg);
            }
            else if (j % 25 == 0) {
                // outdated
                ev.setEventType(ageType);
                ev.setEventMessage(outdated);
            }
            else if (j % 25 == 1) {
                // uptodate again
                ev.setEventType(ageType);
                ev.setEventMessage(uptodate);
            }
            else {
                ev.setPreviousValue(preValue);
                ev.setPreviousTimestamp(preTimestamp.toDate());

                // increate value and time
                preValue += 1;
                ev.setValue(preValue);
                ev.setTimestamp(preTimestamp.plusHours(1).toDate());
                ev.setEventType(ruleType);
            }

            preTimestamp = preTimestamp.plusHours(1);
            ev.setCreated(preTimestamp.toDate());

            this.eventDao.store(ev);

            if (j % 4 == 0) {
                WvEventMail em = new WvEventMail();
                em.setContent("Well, this went kind of wrong: " + j);
                em.setMailAddress("test@wrong.io");
                ev.setLogMails(Collections.singleton(em));
                em.setId(ev.getId());
                session.save(em);
            }

        }
    }

    private Notification createNotification(Series series) throws DatabaseException {
        Notification noti = new Notification();
        noti.setSeries(series);

        Set<NotificationRule> rules = new HashSet<>();
        // rule 1
        Rule rule1 = new Rule();
        rule1.setSeries(series);
        rule1.setThreshold(101);
        rule1.setTrendCode(this.trendDao.retrieveByDomainTrend(TrendDao.DomainTrend.EqualGreater).get());
        this.ruleDao.store(rule1);
        NotificationRule nr1 = new NotificationRule();
        nr1.setRule(rule1);
        nr1.setNotification(noti);
        nr1.setPrimaryRule(true);
        nr1.setLevel(this.notiLevelDao.retrieveById(1).get());
        rules.add(nr1);

        noti.setNotificationRules(rules);

        this.notiDao.store(noti);
        session.save(nr1);

        return noti;
    }

}
