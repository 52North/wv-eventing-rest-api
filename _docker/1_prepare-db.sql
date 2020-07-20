--
-- Copyright (C) 2016-2020 52°North Initiative for Geospatial Open Source
-- Software GmbH
--
-- This program is free software; you can redistribute it and/or modify it under
-- the terms of the GNU General Public License version 2 as publishedby the Free
-- Software Foundation.
--
-- If the program is linked with libraries which are licensed under one of the
-- following licenses, the combination of the program with the linked library is
-- not considered a "derivative work" of the program:
--
--     - Apache License, version 2.0
--     - Apache Software License, version 1.0
--     - GNU Lesser General Public License, version 3
--     - Mozilla Public License, versions 1.0, 1.1 and 2.0
--     - Common Development and Distribution License (CDDL), version 1.0
--
-- Therefore the distribution of the program linked with libraries licensed under
-- the aforementioned licenses, is permitted by the copyright holders if the
-- distribution is compliant with both the GNU General Public License version 2
-- and the aforementioned licenses.
--
-- This program is distributed in the hope that it will be useful, but WITHOUT ANY
-- WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
-- PARTICULAR PURPOSE. See the GNU General Public License for more details.
--

--
-- Prepare database so that wv-dump can be imported
--

-- create the roles used by wv-data dump
CREATE ROLE sensorweb2;
CREATE ROLE sensorweb2_lesen;
CREATE ROLE sensorweb2_basis_lesen;
CREATE ROLE sensorweb2_aendern;
CREATE ROLE sensorweb2_observation_aendern;
CREATE ROLE sensorweb2_eventing_lesen;
CREATE ROLE sensorweb2_eventing_aendern;
CREATE ROLE sensorweb2_mailing_aendern;
CREATE ROLE sensorweb2intern_lesen;

-- some defined functiones are not qualified
SET search_path TO sensorweb2,public;

