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
package org.n52.eventing.wv.database;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Date;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;
import org.joda.time.Period;
import org.postgresql.util.PGInterval;

/**
 *
 * @author <a href="mailto:m.rieke@52north.org">Matthes Rieke</a>
 */
public class IntervalType implements UserType {

    @Override
    public int[] sqlTypes() {
        return new int[] {Types.OTHER};
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Class returnedClass() {
        return Integer.class;
    }

    @Override
    public boolean equals(Object x, Object y) throws HibernateException {
        if (x != null) {
            return x.equals(y);
        }

        return false;
    }

    @Override
    public int hashCode(Object x) throws HibernateException {
        if (x != null) {
            return x.hashCode();
        }

        throw new HibernateException("Object was null");
    }

    @Override
    public Object nullSafeGet(ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner) throws HibernateException, SQLException {
        String interval = rs.getString(names[0]);

        if (interval == null || rs.wasNull()) {
            return null;
        }

        PGInterval pgi = new PGInterval(interval);
        int sec = ((Double) pgi.getSeconds()).intValue();
        int millis = (int) (pgi.getSeconds() * 1000) % 1000;
        return new Period(pgi.getHours(), pgi.getMinutes(), sec, millis);
//
//        // add unix time zero to the timestamp
//        Date timestamp = new Date(0l);
//        pgi.add(timestamp);
//
//        return (int) timestamp.getTime() / 1000;
    }

    public static String getInterval(int value) {
        return new PGInterval(0, 0, 0, 0, 0, value).getValue();
    }

    @Override
    public void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session) throws HibernateException, SQLException {
        if (value == null) {
            st.setNull(index, Types.VARCHAR);
        } else {
            st.setObject(index, getInterval((Integer) value), sqlTypes()[0]);
        }
    }

    @Override
    public Object deepCopy(Object value) throws HibernateException {
        return value;
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public Serializable disassemble(Object value) throws HibernateException {
        return (Serializable) value;
    }

    @Override
    public Object assemble(Serializable cached, Object owner)
            throws HibernateException {
        return cached;
    }

    @Override
    public Object replace(Object original, Object target, Object owner)
            throws HibernateException {
        return original;
    }

}
