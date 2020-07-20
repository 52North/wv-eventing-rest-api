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
package org.n52.eventing.wv.dao;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.hibernate.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.n52.eventing.wv.dao.hibernate.HibernateTrendDao;
import org.n52.eventing.wv.database.HibernateDatabaseConnection;
import org.n52.eventing.wv.model.Trend;

/**
 *
 * @author <a href="mailto:m.rieke@52north.org">Matthes Rieke</a>
 */
public class HibernateTrendDaoIT {

    private HibernateDatabaseConnection hdc;
    private Session session;

    @BeforeEach
    public void setup() throws Exception {
        this.hdc = new HibernateDatabaseConnection();
        this.hdc.afterPropertiesSet();
        this.session = this.hdc.createSession();
    }

    @Test
    public void roundtrip() throws ImmutableException, DatabaseException  {
        HibernateTrendDao dao = new HibernateTrendDao(session);

        Optional<Trend> t1 = dao.retrieveByDomainTrend(TrendDao.DomainTrend.Failure);
        MatcherAssert.assertThat(t1.get().getId(), CoreMatchers.is(99));

        t1 = dao.retrieveByDomainTrend(TrendDao.DomainTrend.LessLess);
        MatcherAssert.assertThat(t1.get().getId(), CoreMatchers.is(11));

        t1 = dao.retrieveByDomainTrend(TrendDao.DomainTrend.GreaterGreater);
        MatcherAssert.assertThat(t1.get().getId(), CoreMatchers.is(33));
    }

    @Test
    public void testLocale() throws ImmutableException, DatabaseException  {
        HibernateTrendDao dao = new HibernateTrendDao(session);

        Optional<Trend> t1 = dao.retrieveByDomainTrend(TrendDao.DomainTrend.LessLess, Locale.forLanguageTag("en"));
        MatcherAssert.assertThat(t1.isPresent(), CoreMatchers.is(true));
        MatcherAssert.assertThat(t1.get().getLocale(), CoreMatchers.is("en"));

        List<Trend> list = dao.retrieve(null, Locale.forLanguageTag("en")).getResult().stream()
                .collect(Collectors.toList());

        MatcherAssert.assertThat(list.size(), CoreMatchers.is(10));
        MatcherAssert.assertThat(list.get(0).getLocale(), CoreMatchers.is("en"));
        MatcherAssert.assertThat(list.get(9).getLocale(), CoreMatchers.is("en"));
    }


    @AfterEach
    public void shutdown() {
        session.close();
    }

}

