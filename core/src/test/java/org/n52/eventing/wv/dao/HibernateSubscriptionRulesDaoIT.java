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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.n52.eventing.wv.dao.hibernate.HibernateRuleDao;
import org.n52.eventing.wv.dao.hibernate.HibernateSeriesDao;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import org.hamcrest.CoreMatchers;
import org.hibernate.Session;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.n52.eventing.wv.dao.hibernate.HibernateCategoryDao;
import org.n52.eventing.wv.dao.hibernate.HibernateFeatureOfInterestDao;
import org.n52.eventing.wv.dao.hibernate.HibernateGroupDao;
import org.n52.eventing.wv.dao.hibernate.HibernateNotificationDao;
import org.n52.eventing.wv.dao.hibernate.HibernateNotificationLevelDao;
import org.n52.eventing.wv.dao.hibernate.HibernateNotificationRuleDao;
import org.n52.eventing.wv.dao.hibernate.HibernatePhenomenonDao;
import org.n52.eventing.wv.dao.hibernate.HibernateProcedureDao;
import org.n52.eventing.wv.dao.hibernate.HibernateSubscriptionDao;
import org.n52.eventing.wv.dao.hibernate.HibernateTrendDao;
import org.n52.eventing.wv.dao.hibernate.HibernateUnitDao;
import org.n52.eventing.wv.dao.hibernate.HibernateUserDao;
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
import org.n52.eventing.wv.model.WvSubscription;
import org.n52.eventing.wv.model.WvUser;
import org.n52.eventing.wv.security.GroupPolicies;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:m.rieke@52north.org">Matthes Rieke</a>
 */
public class HibernateSubscriptionRulesDaoIT {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(HibernateSubscriptionRulesDaoIT.class.getName());

    private Session session;
    private final GroupPolicies gp = new GroupPolicies();
    private final List<Series> createdSeriesList = new ArrayList<>();
    private final List<FeatureOfInterest> createdFeatures = new ArrayList<>();
    private final List<Rule> createdRules = new ArrayList<>();
    private final List<Notification> createdNotifications = new ArrayList<>();
    private final List<NotificationRule> createdNotificationRules = new ArrayList<>();
    private final List<WvSubscription> createdSubscriptions = new ArrayList<>();
    private final List<WvUser> createdUsers = new ArrayList<>();
    private final List<Group> createdGroups = new ArrayList<>();

    @Before
    public void setup() throws Exception {
        HibernateDatabaseConnection hdc = new HibernateDatabaseConnection();
        hdc.afterPropertiesSet();
        this.session = hdc.createSession();
    }

    @After
    public void cleanup() throws DatabaseException {
        if (!this.createdSubscriptions.isEmpty()) {
            HibernateSubscriptionDao dao = new HibernateSubscriptionDao(session);
            for (WvSubscription s : createdSubscriptions) {
                dao.remove(s);
            }
        }
        if (!this.createdNotificationRules.isEmpty()) {
            HibernateNotificationRuleDao dao = new HibernateNotificationRuleDao(session);
            for (NotificationRule notificationRule : createdNotificationRules) {
                dao.remove(notificationRule);
            }
        }
        if (!this.createdNotifications.isEmpty()) {
            HibernateNotificationDao dao = new HibernateNotificationDao(session);
            for (Notification notification : createdNotifications) {
                dao.remove(notification);
            }
        }
        if (!this.createdRules.isEmpty()) {
            HibernateRuleDao dao = new HibernateRuleDao(session);
            for (Rule rule : createdRules) {
                dao.remove(rule);
            }
        }
        if (!this.createdSeriesList.isEmpty()) {
            HibernateSeriesDao dao = new HibernateSeriesDao(session);
            for (Series createdSery : createdSeriesList) {
                dao.remove(createdSery);
            }
        }
        if (!this.createdFeatures.isEmpty()) {
            HibernateFeatureOfInterestDao dao = new HibernateFeatureOfInterestDao(session);
            for (FeatureOfInterest featureOfInterest : createdFeatures) {
                dao.remove(featureOfInterest);
            }
        }
        if (!this.createdUsers.isEmpty()) {
            HibernateUserDao dao = new HibernateUserDao(session, gp);
            this.createdUsers.forEach(s -> {
                try {
                    dao.remove(s);
                } catch (DatabaseException ex) {
                    LOG.warn(ex.getMessage(), ex);
                }
            });
        }
        if (!this.createdGroups.isEmpty()) {
            HibernateGroupDao dao = new HibernateGroupDao(session, gp);
            this.createdGroups.forEach(s -> {
                try {
                    dao.remove(s);
                } catch (DatabaseException ex) {
                    LOG.warn(ex.getMessage(), ex);
                }
            });
        }


        this.session.close();
    }

    @Test
    public void roundtrip() throws ImmutableException, DatabaseException  {
        HibernateSubscriptionDao subDao = new HibernateSubscriptionDao(session);
        HibernateSeriesDao seriesDao = new HibernateSeriesDao(session);
        HibernateRuleDao ruleDao = new HibernateRuleDao(session);
        HibernateUserDao userDao = new HibernateUserDao(session, gp);

        WvUser u1 = new WvUser();
        u1.setName("one more tester");
        u1.setPassword("wurz");
        u1.setId(new Random().nextInt(90000));
        userDao.store(u1);
        createdUsers.add(u1);

        WvSubscription sub1 = createNewSubscription(seriesDao, ruleDao, true);
        sub1.setUser(u1);
        subDao.store(sub1);
        createdSubscriptions.add(sub1);

        subDao = new HibernateSubscriptionDao(session);
        HibernateNotificationDao notiDao = new HibernateNotificationDao(session);

        Optional<Notification> r1r = notiDao.retrieveById(sub1.getNotification().getId());
        Assert.assertThat(r1r.isPresent(), CoreMatchers.is(true));
        Assert.assertThat(r1r.get().getNotificationRules().iterator().next().getRule().getThreshold(), CoreMatchers.is(22.0));
        Assert.assertThat(r1r.get().getSeries().getId(), CoreMatchers.is(sub1.getNotification().getNotificationRules().iterator().next().getRule().getSeries().getId()));
        Assert.assertThat(r1r.get().getSeries().getId(), CoreMatchers.not(0));
        Assert.assertThat(r1r.get().getSeries().getUnit().getCode(), CoreMatchers.is("cm"));

        Optional<WvSubscription> sub1r = subDao.retrieveById(sub1.getId());
        Assert.assertThat(sub1r.isPresent(), CoreMatchers.is(true));
        Assert.assertThat(sub1r.get().getNotification().getId(), CoreMatchers.is(r1r.get().getId()));

        List<WvSubscription> subs = subDao.retrieve(null).getResult();
        Assert.assertThat(subs.size() > 0, CoreMatchers.is(true));
    }

    @Test
    public void testUserSubscriptions() throws DatabaseException {
        HibernateSubscriptionDao subDao = new HibernateSubscriptionDao(session);
        HibernateSeriesDao seriesDao = new HibernateSeriesDao(session);
        HibernateRuleDao ruleDao = new HibernateRuleDao(session);

        WvUser u1 = new WvUser();
        u1.setName("one more tester");
        u1.setPassword("wurz");
        u1.setId(new Random().nextInt(90000));
        createdUsers.add(u1);

        WvUser u2 = new WvUser();
        u2.setName("another more tester");
        u2.setPassword("wurzl");
        u2.setId(new Random().nextInt(90000));
        createdUsers.add(u2);

        HibernateUserDao userDao = new HibernateUserDao(session, gp);
        userDao.store(u1);
        userDao.store(u2);

        WvSubscription sub1 = createNewSubscription(seriesDao, ruleDao, true);
        sub1.setUser(u1);
        createdSubscriptions.add(sub1);

        WvSubscription sub2 = createNewSubscription(seriesDao, ruleDao, true);
        sub2.setUser(u2);
        createdSubscriptions.add(sub2);

        subDao.store(sub1);
        subDao.store(sub2);

        Assert.assertThat(subDao.hasEntity(sub1), CoreMatchers.is(true));
        Assert.assertThat(ruleDao.hasEntity(sub1.getNotification().getNotificationRules().iterator().next().getRule()), CoreMatchers.is(true));
        Assert.assertThat(subDao.hasEntity(sub2), CoreMatchers.is(true));
        Assert.assertThat(ruleDao.hasEntity(sub2.getNotification().getNotificationRules().iterator().next().getRule()), CoreMatchers.is(true));

        List<WvSubscription> sub1r = subDao.retrieveByUser(u1).getResult();
        Assert.assertThat(sub1r.size(), CoreMatchers.is(1));
        Assert.assertThat(sub1r.get(0).getUser(), CoreMatchers.is(u1));
    }

    @Test
    public void testGroupSubscriptions() throws DatabaseException {
        HibernateSubscriptionDao subDao = new HibernateSubscriptionDao(session);
        HibernateSeriesDao seriesDao = new HibernateSeriesDao(session);
        HibernateRuleDao ruleDao = new HibernateRuleDao(session);

        Group g1 = new Group(gp.getGroupPrefix()+"-"+UUID.randomUUID().toString(), "n/a");
        g1.setId(new Random().nextInt(90000));
        createdGroups.add(g1);
        Group g2 = new Group(gp.getGroupPrefix()+"-"+UUID.randomUUID().toString(), "n/a");
        g2.setId(new Random().nextInt(90000));
        createdGroups.add(g2);

        HibernateGroupDao groupDao = new HibernateGroupDao(session, gp);
        groupDao.store(g1);
        groupDao.store(g2);

        WvSubscription sub1 = createNewSubscription(seriesDao, ruleDao, true);
        sub1.setGroup(g1);
        createdSubscriptions.add(sub1);

        WvSubscription sub2 = createNewSubscription(seriesDao, ruleDao, true);
        sub2.setGroup(g2);
        createdSubscriptions.add(sub2);

        subDao.store(sub1);
        subDao.store(sub2);

        Assert.assertThat(subDao.hasEntity(sub1), CoreMatchers.is(true));
        Assert.assertThat(subDao.hasEntity(sub2), CoreMatchers.is(true));

        List<WvSubscription> sub1r = subDao.retrieveByGroup(g1).getResult();
        Assert.assertThat(sub1r.size(), CoreMatchers.is(1));
        Assert.assertThat(sub1r.get(0).getGroup(), CoreMatchers.is(g1));
    }

    @Test
    public void testNotActiveForEventing() throws DatabaseException {
       HibernateSubscriptionDao subDao = new HibernateSubscriptionDao(session);
        HibernateSeriesDao seriesDao = new HibernateSeriesDao(session);
        HibernateRuleDao ruleDao = new HibernateRuleDao(session);

        Group g1 = new Group(gp.getGroupPrefix()+"-"+UUID.randomUUID().toString(), "n/a");
        g1.setId(new Random().nextInt(90000));
        createdGroups.add(g1);

        HibernateGroupDao groupDao = new HibernateGroupDao(session, gp);
        groupDao.store(g1);

        WvSubscription sub1 = createNewSubscription(seriesDao, ruleDao, false);
        sub1.setGroup(g1);
        createdSubscriptions.add(sub1);

        subDao.store(sub1);

        Assert.assertThat(subDao.hasEntity(sub1), CoreMatchers.is(true));
        Assert.assertThat(ruleDao.hasEntity(sub1.getNotification().getNotificationRules().iterator().next().getRule()), CoreMatchers.is(false));

        List<Rule> series1r = ruleDao.retrieveBySeries(Integer.toString(sub1.getNotification().getSeries().getId())).getResult();
        Assert.assertThat(series1r.size(), CoreMatchers.is(0));
    }


    private WvSubscription createNewSubscription(HibernateSeriesDao seriesDao, HibernateRuleDao ruleDao, boolean activeForEventing) throws DatabaseException {
        Series createdSeries = new Series();
        createdSeries.setCategory(createCategory("test-category"));
        createdSeries.setPhenomenon(createPhenomenon("test-phenomenon"));
        createdSeries.setProcedure(createProcedure("test-procedure"));
        createdSeries.setActiveForEventing(activeForEventing);

        FeatureOfInterest createdFeature = new FeatureOfInterest("test-feature-"+UUID.randomUUID().toString().substring(0, 6),
                "Test Feature", "point", new Random().nextInt(), "its not a bug");
        new HibernateFeatureOfInterestDao(session).store(createdFeature);
        createdFeatures.add(createdFeature);

        createdSeries.setFeature(createdFeature);
        createdSeries.setUnit(createUnit("cm"));
        seriesDao.store(createdSeries);
        createdSeriesList.add(createdSeries);

        Rule createdRule = new Rule(22.0, new HibernateTrendDao(session).retrieveByDomainTrend(TrendDao.DomainTrend.LessEqual).get(), 1, createdSeries);
        ruleDao.store(createdRule);
        createdRules.add(createdRule);

        // notification
        Notification createdNotification = new Notification();
        createdNotification.setSeries(createdSeries);

        NotificationRule createdNotificationRule = new NotificationRule();
        createdNotificationRule.setRule(createdRule);
        createdNotificationRule.setPrimaryRule(true);
        createdNotificationRule.setLevel(new HibernateNotificationLevelDao(session).retrieveById(1).get());
        createdNotificationRule.setNotification(createdNotification);
        createdNotification.setNotificationRules(Collections.singleton(createdNotificationRule));

        new HibernateNotificationDao(session).store(createdNotification);
        createdNotifications.add(createdNotification);
        new HibernateNotificationRuleDao(session).store(createdNotificationRule);
        createdNotificationRules.add(createdNotificationRule);

        return new WvSubscription(createdNotification);
    }

    private Category createCategory(String name) throws DatabaseException {
        HibernateCategoryDao dao = new HibernateCategoryDao(session);
        if (dao.exists(name)) {
            return dao.retrieveByName(name).get();
        }
        Category r = new Category(name);
        dao.store(r);

        return r;
    }

    private Phenomenon createPhenomenon(String name) throws DatabaseException {
        HibernatePhenomenonDao dao = new HibernatePhenomenonDao(session);
        if (dao.exists(name)) {
            return dao.retrieveByName(name).get();
        }
        Phenomenon r = new Phenomenon(name);
        dao.store(r);

        return r;
    }

    private Procedure createProcedure(String name) throws DatabaseException {
        HibernateProcedureDao dao = new HibernateProcedureDao(session);
        if (dao.exists(name)) {
            return dao.retrieveByName(name).get();
        }
        Procedure r = new Procedure(name);
        dao.store(r);

        return r;
    }

    private Unit createUnit(String name) throws DatabaseException {
        HibernateUnitDao dao = new HibernateUnitDao(session);
        Optional<Unit> unit = dao.retrieveByCode(name);
        if (unit.isPresent()) {
            return unit.get();
        }
        Unit r = new Unit(name);
        dao.store(r);

        return r;
    }

}
