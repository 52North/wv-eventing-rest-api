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

