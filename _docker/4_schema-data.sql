--
-- PostgreSQL database dump
--

SET statement_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;

--
-- Name: sensorweb2; Type: SCHEMA; Schema: -; Owner: sensorweb2
--

CREATE SCHEMA sensorweb2;


ALTER SCHEMA sensorweb2 OWNER TO sensorweb2;

--
-- Name: SCHEMA sensorweb2; Type: COMMENT; Schema: -; Owner: sensorweb2
--

COMMENT ON SCHEMA sensorweb2 IS 'Datenbestand SensorWeb 2';


SET search_path = sensorweb2, pg_catalog;

--
-- Name: event_log_descriptions(integer, integer, character varying); Type: FUNCTION; Schema: sensorweb2; Owner: sensorweb2
--

CREATE FUNCTION event_log_descriptions(in_event_log_pkid integer, in_subscription_pkid integer, in_locale character varying DEFAULT NULL::character varying, OUT out_notification_level_description character varying, OUT out_phenomenon_name character varying, OUT out_feature_of_interest_name character varying, OUT out_procedure_name character varying, OUT out_trend_description character varying, OUT out_numeric_value character varying, OUT out_unit character varying) RETURNS SETOF record
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$

/*

Beispiel: SELECT * FROM sensorweb2.event_log_descriptions(2267, 330);
Ergebnis:
 out_notification_level_description | out_phenomenon_name | out_feature_of_interest_name | out_procedure_name |  out_trend_description  | out_numeric_value | out_unit
------------------------------------+---------------------+------------------------------+--------------------+-------------------------+-------------------+----------
 Entwarnung                         | Wasserstand         | Opladen                      | Einzelwert         | fällt auf den Grenzwert | 47                | cm
(1 row)

--Beispiel: SELECT * FROM sensorweb2.event_log_descriptions(2267, 330, 'de_DE');
Beispiel: SELECT * FROM sensorweb2.event_log_descriptions(2267, 330, 'lala');
Ergebnis:
 out_notification_level_description | out_phenomenon_name | out_feature_of_interest_name | out_procedure_name |  out_trend_description  | out_numeric_value | out_unit
------------------------------------+---------------------+------------------------------+--------------------+-------------------------+-------------------+----------
 Entwarnung                         | Wasser-Stand        | Opladen                      | Einzelwert         | fällt auf den Grenzwert | 47                | cm
(1 row)

*/

DECLARE

  var_notification_pkid              sensorweb2.notification.pkid%TYPE;
  var_notification_level_id          sensorweb2.notification_level.level_id%TYPE;
  var_notification_level_description sensorweb2.notification_level.description%TYPE;
  var_phenomenon_pkid                sensorweb2.phenomenon.pkid%TYPE;
  var_phenomenon_name                sensorweb2.phenomenon.phenomenon_name%TYPE;
  var_feature_of_interest_pkid       sensorweb2.feature_of_interest.pkid%TYPE;
  var_feature_of_interest_name       sensorweb2.feature_of_interest.feature_of_interest_name%TYPE;
  var_procedure_pkid                 sensorweb2.procedure.pkid%TYPE;
  var_procedure_name                 sensorweb2.procedure.procedure_name%TYPE;
  var_trend_code                     sensorweb2.trend.code%TYPE;
  var_trend_description              sensorweb2.trend.description%TYPE;
  var_rule_pkid                      sensorweb2.rule.pkid%TYPE;
  var_rule_threshold                 sensorweb2.rule.threshold%TYPE;
  var_threshold_text                 CHARACTER VARYING(30);
  var_unit_pkid                      sensorweb2.unit.pkid%TYPE;
  var_unit                           sensorweb2.unit.unit%TYPE;

BEGIN

  --INSERT INTO dbadmin.debug (text) VALUES ('in_event_log_pkid = ' || in_event_log_pkid);
  --INSERT INTO dbadmin.debug (text) VALUES ('in_locale = ' || COALESCE(in_locale, 'NULL'));

  ----------------------------------------------------------------------------------------------------
  -- vorab notwendige Feldinhalte ermitteln
  
  SELECT 
    series.phenomenon_pkid,
    series.feature_of_interest_pkid,
    series.procedure_pkid,
    series.unit_pkid,
    rule.pkid,
    rule.trend_code, 
    rule.threshold
  INTO
    var_phenomenon_pkid, 
    var_feature_of_interest_pkid,
    var_procedure_pkid,
    var_unit_pkid,
    var_rule_pkid,
    var_trend_code, 
    var_rule_threshold
  FROM sensorweb2.event_log
    INNER JOIN sensorweb2.series ON series.pkid = event_log.series_pkid
    INNER JOIN sensorweb2.rule ON rule.pkid = event_log.rule_pkid
  WHERE event_log.pkid = in_event_log_pkid;
  
  SELECT notification_pkid
  INTO var_notification_pkid
  FROM sensorweb2.subscription
  WHERE pkid = in_subscription_pkid;
  
  ----------------------------------------------------------------------------------------------------
  -- notification_level.description
  
  SELECT notification_level_id
  INTO var_notification_level_id
  FROM sensorweb2.notification_rule
  WHERE notification_pkid = var_notification_pkid
    AND rule_pkid = var_rule_pkid;

  IF in_locale IS NOT NULL THEN
    SELECT description 
    INTO var_notification_level_description
    FROM sensorweb2.notification_level_i18n
    WHERE notification_level_id = var_notification_level_id
      AND locale = in_locale;
  END IF;
  IF var_notification_level_description IS NULL THEN
    SELECT description 
    INTO var_notification_level_description
    FROM sensorweb2.notification_level
    WHERE level_id = var_notification_level_id;
  END IF;
  IF var_notification_level_description IS NULL THEN
    var_notification_level_description := '';
  END IF;

  ----------------------------------------------------------------------------------------------------
  -- phenomenon_name
  
  IF in_locale IS NOT NULL THEN
    SELECT name 
    INTO var_phenomenon_name
    FROM sensorweb2.phenomenon_i18n
    WHERE phenomenon_pkid = var_phenomenon_pkid
      AND locale = in_locale;
  END IF;
  IF var_phenomenon_name IS NULL THEN
    SELECT phenomenon_name 
    INTO var_phenomenon_name
    FROM sensorweb2.phenomenon
    WHERE pkid = var_phenomenon_pkid;
  END IF;
  IF var_phenomenon_name IS NULL THEN
    var_phenomenon_name := '';
  END IF;

  ----------------------------------------------------------------------------------------------------
  -- feature_of_interest_name
  
  IF in_locale IS NOT NULL THEN
    SELECT name 
    INTO var_feature_of_interest_name
    FROM sensorweb2.feature_of_interest_i18n
    WHERE feature_of_interest_pkid = var_feature_of_interest_pkid
      AND locale = in_locale;
  END IF;
  IF var_feature_of_interest_name IS NULL THEN
    SELECT feature_of_interest_name 
    INTO var_feature_of_interest_name
    FROM sensorweb2.feature_of_interest
    WHERE pkid = var_feature_of_interest_pkid;
  END IF;
  IF var_feature_of_interest_name IS NULL THEN
    var_feature_of_interest_name := '';
  END IF;

  ----------------------------------------------------------------------------------------------------
  -- procedure_name
  
  IF in_locale IS NOT NULL THEN
    SELECT name 
    INTO var_procedure_name
    FROM sensorweb2.procedure_i18n
    WHERE procedure_pkid = var_procedure_pkid
      AND locale = in_locale;
  END IF;
  IF var_procedure_name IS NULL THEN
    SELECT procedure_name 
    INTO var_procedure_name
    FROM sensorweb2.procedure
    WHERE pkid = var_procedure_pkid;
  END IF;
  IF var_procedure_name IS NULL THEN
    var_procedure_name := '';
  END IF;

  ----------------------------------------------------------------------------------------------------
  -- trend_description
  
  IF in_locale IS NOT NULL THEN
    SELECT description
    INTO var_trend_description
    FROM sensorweb2.trend_i18n
    WHERE trend_code = var_trend_code
      AND locale = in_locale;
  END IF;
  IF var_trend_description IS NULL THEN
    SELECT description 
    INTO var_trend_description
    FROM sensorweb2.trend
    WHERE code = var_trend_code;
  END IF;
  IF var_trend_description IS NULL THEN
    var_trend_description := '';
  END IF;

  ----------------------------------------------------------------------------------------------------
  -- rule_threshold
  -- Hinweis: hier bis auf Weiteres den Dezimalpunkt immer in ein Komma umwandeln

  var_threshold_text := REPLACE(var_rule_threshold::REAL::TEXT, '.', ',');

  ----------------------------------------------------------------------------------------------------
  -- unit
  
  IF in_locale IS NOT NULL THEN
    SELECT unit
    INTO var_unit
    FROM sensorweb2.unit_i18n
    WHERE unit_pkid = var_unit_pkid
      AND locale = in_locale;
  END IF;
  IF var_unit IS NULL THEN
    SELECT unit 
    INTO var_unit
    FROM sensorweb2.unit
    WHERE pkid = var_unit_pkid;
  END IF;
  IF var_unit IS NULL THEN
    var_unit := '';
  END IF;

  ----------------------------------------------------------------------------------------------------
  -- Ermittelte Daten zurückgeben
  RETURN QUERY
    SELECT
      var_notification_level_description,
      var_phenomenon_name,
      var_feature_of_interest_name,
      var_procedure_name,
      var_trend_description,
      var_threshold_text,
      var_unit;
  RETURN;
  
END;

$$;


ALTER FUNCTION sensorweb2.event_log_descriptions(in_event_log_pkid integer, in_subscription_pkid integer, in_locale character varying, OUT out_notification_level_description character varying, OUT out_phenomenon_name character varying, OUT out_feature_of_interest_name character varying, OUT out_procedure_name character varying, OUT out_trend_description character varying, OUT out_numeric_value character varying, OUT out_unit character varying) OWNER TO sensorweb2;

--
-- Name: FUNCTION event_log_descriptions(in_event_log_pkid integer, in_subscription_pkid integer, in_locale character varying, OUT out_notification_level_description character varying, OUT out_phenomenon_name character varying, OUT out_feature_of_interest_name character varying, OUT out_procedure_name character varying, OUT out_trend_description character varying, OUT out_numeric_value character varying, OUT out_unit character varying); Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON FUNCTION event_log_descriptions(in_event_log_pkid integer, in_subscription_pkid integer, in_locale character varying, OUT out_notification_level_description character varying, OUT out_phenomenon_name character varying, OUT out_feature_of_interest_name character varying, OUT out_procedure_name character varying, OUT out_trend_description character varying, OUT out_numeric_value character varying, OUT out_unit character varying) IS 'Liefert die einzelnen Texte zu einem Event Log-Eintrag, optional zu einer bestimmten Sprache-Region-Kombination (tt-2017-07-05)';


--
-- Name: get_event_log_description(integer, character varying, integer, integer); Type: FUNCTION; Schema: sensorweb2; Owner: postgres
--

CREATE FUNCTION get_event_log_description(param_event_log_pkid integer, param_mail_address character varying DEFAULT ''::character varying, param_subscription_pkid integer DEFAULT 0, param_notification_level_id integer DEFAULT (-1), OUT return_description character varying) RETURNS character varying
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$

-- Liefert die Beschreibung eines Ereignisprotokoll-Eintrages
-- Die uebergebene Mail-Adresse dient zur Ermittlung der [später zu implementierenden] Localization
-- Ist die param_subscription_pkid valide, dann werden die Notification-Einstellungen ueber die Subscription ermittelt
-- Ist ein Notification Level explizit angegeben, dann wird seine Beschreibung im Ereignistext verwendet
-- Beispiele Regel-Ereignisse: 
--   SELECT sensorweb2.get_event_log_description(933); -> 'Wasserstand Müllensiepen, Zulaufpegel, Einzelwert, steigt über 21 cm'
--   SELECT sensorweb2.get_event_log_description(933, 'tt@wupperverband.de', 60); -> 'Warnung: Wasserstand Müllensiepen, Zulaufpegel, Einzelwert, steigt über 21 cm'
-- Beispiel Zeitreihenverwaltung:
--   SELECT sensorweb2.get_event_log_description(1); -> 'Ereignisprotokollierung eingeschaltet (Schüttmenge Bever-Talsperre, Sickerwassermessstelle S2B, Test)'
-- Beispiel Alterskontrolle:
--   SELECT sensorweb2.get_event_log_description(1903, 'tt@wupperverband.de', 0, 1); -> 'Warnung: Letzter Messwert ist älter als 30 Minuten (Schüttmenge Bever-Talsperre, Sickerwassermessstelle S2B, Test)'
-- Beispiel Abonnement:
--   SELECT sensorweb2.get_event_log_description(3080, 'tt@wupperverband.de'); -> 'Neues Abonnement für die Benutzergruppe "sensorweb-eventing-users" zur Benachrichtigung "Warnung: Schüttmenge Bever-Talsperre, Sickerwassermessstelle S2B (Test) steigt über 1 l/s" definiert'
-- tt-2018-04-05
-- tt-2018-09-07 Umwandlung threshold in Text eines Regel-Ereignisses
-- tt-2018-09-12 Kommata am Ende des threshold-Textes entfernen

DECLARE

  var_event_type_pkid                   sensorweb2.event_type.pkid%TYPE;
  var_event_message_text                sensorweb2.event_message.message_text%TYPE;
  var_notification_pkid                 sensorweb2.notification.pkid%TYPE;
  var_notification_level_description    sensorweb2.notification_level.description%TYPE;
  var_rule_pkid                         sensorweb2.rule.pkid%TYPE;
  var_rule_threshold                    sensorweb2.rule.threshold%TYPE;
  var_trend_description                 sensorweb2.trend.description%TYPE;
  var_series_pkid                       sensorweb2.series.pkid%TYPE;
  var_unit_description                  sensorweb2.unit.description%TYPE;
  var_usergroup_name                    sensorweb2.usergroup.name%TYPE;
  var_user_name                         CHARACTER VARYING(250);
  var_interval                          NUMERIC;
  var_interval_text                     CHARACTER VARYING;
  var_temp_text                         CHARACTER VARYING;
  
BEGIN

  -- Feldinhalte von Tabellen ermitteln, die von event_log referenziert werden
  -- [spaeter] sprachabhängig
  SELECT
      event_log.series_pkid,  
      event_type.pkid, 
      TRIM(event_message.message_text),
      notification.pkid,
      rule.pkid, 
      rule.threshold, 
      TRIM(trend.description),
      COALESCE(usergroup.name, ''),
      COALESCE(TRIM(user_.firstname) || ' ', '') || COALESCE(TRIM(user_.middlename) || ' ', '') || COALESCE(TRIM(user_.lastname), '')
    INTO
      var_series_pkid,
      var_event_type_pkid, 
      var_event_message_text,
      var_notification_pkid,
      var_rule_pkid,
      var_rule_threshold, 
      var_trend_description,
      var_usergroup_name,
      var_user_name
    FROM sensorweb2.event_log
      INNER JOIN sensorweb2.event_type               ON event_type.pkid = event_log.event_type_pkid
      LEFT OUTER JOIN sensorweb2.event_message       ON event_message.pkid = event_log.event_message_pkid
      LEFT OUTER JOIN sensorweb2.notification        ON notification.pkid = event_log.notification_pkid
      LEFT OUTER JOIN sensorweb2.rule                ON rule.pkid = event_log.rule_pkid
      LEFT OUTER JOIN sensorweb2.trend               ON trend.code = rule.trend_code
      LEFT OUTER JOIN sensorweb2.usergroup           ON usergroup.usergroupid = event_log.usergroup_usergroupid
      LEFT OUTER JOIN sensorweb2.user_               ON user_.userid = event_log.user_userid
    WHERE event_log.pkid = param_event_log_pkid;

  -- Notification-Level-Beschreibung des Events bestimmen
  -- a) Ist eine Subscription angegeben, dann werden die Notification-Daten via Subscription bestimmt
  IF param_subscription_pkid > 0 THEN
    SELECT notification_level.description
      INTO var_notification_level_description
      FROM sensorweb2.subscription
        LEFT OUTER JOIN sensorweb2.notification_rule   
          ON notification_rule.notification_pkid = subscription.notification_pkid    
          AND notification_rule.rule_pkid = var_rule_pkid
        LEFT OUTER JOIN sensorweb2.notification_level
          ON notification_level.level_id = notification_rule.notification_level_id
      WHERE pkid = param_subscription_pkid;
  END IF;
  -- b) Ist ein Notification-Level direkt angegeben, dann wird seine Beschreibung verwendet (-> Alterskontrolle) 
  IF param_notification_level_id >= 0 THEN
    SELECT notification_level.description
      INTO var_notification_level_description
      FROM sensorweb2.notification_level
      WHERE level_id = param_notification_level_id;
  END IF;
  IF LENGTH(var_notification_level_description) = 0 THEN
    var_notification_level_description = NULL;
  END IF;

  -- [spaeter] Mail-Adresse zur Bestimmung von Benutzereinstellungen verwenden (z.B. Sprache, fixer Mail-Versandzeitpunkt, etc.)

  -- Fallunterscheidung nach event_type
  CASE
    ----------------------------------------------------------------------------
    -- Regel-Ereignis
    -- Beispiel: "Warnung: Wasserstand Opladen, Einzelwert fällt unter den Grenzwert 60 cm"
    WHEN var_event_type_pkid = 1 THEN
      SELECT COALESCE(var_notification_level_description || ': ', '')
          || sensorweb2.get_series_description_without_unit(var_series_pkid) || ', '
          || var_trend_description || ' '  
          || REPLACE(RTRIM(RTRIM(var_rule_threshold::TEXT, '0'), '.') , '.', ',') 
          || COALESCE(' ' || unit.unit, '')
        INTO return_description
        FROM sensorweb2.series
          LEFT OUTER JOIN sensorweb2.unit ON unit.pkid = series.unit_pkid
        WHERE series.pkid = var_series_pkid;
    ----------------------------------------------------------------------------
    -- Zeitreihenverwaltung
    -- Beispiel: "Ereignisprotokollierung eingeschaltet (Wasserstand Opladen, Einzelwert)"
    WHEN var_event_type_pkid = 2 THEN
      var_temp_text := var_event_message_text
        || ' (' || sensorweb2.get_series_description_without_unit(var_series_pkid) || ')';
      return_description := var_temp_text;
    ----------------------------------------------------------------------------
    -- Alterskontrolle
    -- Beispiel: "Warnung: Letzter Messwert ist älter als 90 Minuten (Wasserstand Opladen, Einzelwert)"
    WHEN var_event_type_pkid = 3 THEN
      -- Intervall in Sekunden und Beschreibung der zugehoerigen Einheit ermitteln
      SELECT
          ROUND(EXTRACT('epoch' FROM series_check_age.check_interval)::NUMERIC / series_check_age.factor_seconds, series_check_age.conversion_precision),
          unit.description          
        INTO var_interval, var_unit_description
        FROM sensorweb2.series_check_age
          LEFT OUTER JOIN sensorweb2.unit on unit.pkid = series_check_age.unit_pkid
        WHERE series_check_age.series_pkid = var_series_pkid;
      -- Intervall-Text erzeugen, dabei Einzahl/Mehrzahl der Einheit beachten
      IF var_interval = 1 THEN
        -- Einzahl der Einheit anfuegen
        var_interval_text := var_interval || ' ' || REGEXP_REPLACE(var_unit_description, '\(.*\)', '');
      ELSE
        -- Mehrzahl der Einheit anfuegen
        var_interval_text := var_interval || ' ' || REGEXP_REPLACE(var_unit_description, '\(|\)', '', 'g');
      END IF;
      var_temp_text := COALESCE(var_notification_level_description || ': ', '')
        || REPLACE(var_event_message_text, '{interval_text}', var_interval_text)
        || ' (' || sensorweb2.get_series_description_without_unit(var_series_pkid) || ')';
      return_description := var_temp_text;
    ----------------------------------------------------------------------------
    -- Abonnement
    -- Beispiel: "Neues Abonnement für die Benutzergruppe "sensorweb-eventing-users" zur Benachrichtigung "Warnung: Schüttmenge Bever-Talsperre, Sickerwassermessstelle S2B (Test) steigt über 1 l/s" definiert"
    WHEN var_event_type_pkid = 4 THEN
      var_temp_text := REPLACE(var_event_message_text, '{notification}', sensorweb2.get_notification_description(var_notification_pkid));
      var_temp_text := REPLACE(var_temp_text, '{usergroup}', var_usergroup_name);
      var_temp_text := REPLACE(var_temp_text, '{user}', var_user_name);
      return_description := var_temp_text;
    ----------------------------------------------------------------------------
    ELSE
      return_description := '';
  END CASE;

END;

$$;


ALTER FUNCTION sensorweb2.get_event_log_description(param_event_log_pkid integer, param_mail_address character varying, param_subscription_pkid integer, param_notification_level_id integer, OUT return_description character varying) OWNER TO postgres;

--
-- Name: FUNCTION get_event_log_description(param_event_log_pkid integer, param_mail_address character varying, param_subscription_pkid integer, param_notification_level_id integer, OUT return_description character varying); Type: COMMENT; Schema: sensorweb2; Owner: postgres
--

COMMENT ON FUNCTION get_event_log_description(param_event_log_pkid integer, param_mail_address character varying, param_subscription_pkid integer, param_notification_level_id integer, OUT return_description character varying) IS 'returns the description of an event';


--
-- Name: get_notification_description(integer); Type: FUNCTION; Schema: sensorweb2; Owner: postgres
--

CREATE FUNCTION get_notification_description(param_notification_pkid integer, OUT return_description character varying) RETURNS character varying
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$

-- Liefert die Beschreibung einer Benachrichtigung
-- Beispiel: 
--   SELECT sensorweb2.get_notification_description(1); -> 'Warnung: Abfluss Manfort (Einzelwert) steigt über den Grenzwert 1,1 m³/s'
-- tt-2018-03-23

BEGIN

  -- Feldinhalte von Tabellen ermitteln, ausgehend von notification
  -- [spaeter] alle Eintraege Benutzer-/Sprachabhaengig ermitteln
  SELECT
    COALESCE(notification_level.description || ': ', '')
    || phenomenon.phenomenon_name || ' '
    || feature_of_interest.feature_of_interest_name || ' '
    || '(' || procedure.procedure_name || ') '
    || trend.description || ' '
    || REPLACE(rule.threshold::REAL::TEXT, '.', ',')
    || COALESCE(' ' || unit.unit, '')
  INTO return_description
  FROM sensorweb2.notification
    LEFT OUTER JOIN sensorweb2.series              ON series.pkid = notification.series_pkid
    LEFT OUTER JOIN sensorweb2.phenomenon          ON phenomenon.pkid = series.phenomenon_pkid
    LEFT OUTER JOIN sensorweb2.feature_of_interest ON feature_of_interest.pkid = series.feature_of_interest_pkid
    LEFT OUTER JOIN sensorweb2.procedure           ON procedure.pkid = series.procedure_pkid
    LEFT OUTER JOIN sensorweb2.unit                ON unit.pkid = series.unit_pkid
    LEFT OUTER JOIN sensorweb2.notification_rule   ON notification_rule.notification_pkid = notification.pkid 
    LEFT OUTER JOIN sensorweb2.notification_level  ON notification_level.level_id = notification_rule.notification_level_id
    LEFT OUTER JOIN sensorweb2.rule                ON rule.pkid = notification_rule.rule_pkid
    LEFT OUTER JOIN sensorweb2.trend               ON trend.code = rule.trend_code
  WHERE notification.pkid = param_notification_pkid
    AND notification_rule.primary_rule_flag = 1
  LIMIT 1;  -- zur Sicherheit, falls es zu einer Notification mehrere (primary_rule_flag = 1) geben sollte

END;

$$;


ALTER FUNCTION sensorweb2.get_notification_description(param_notification_pkid integer, OUT return_description character varying) OWNER TO postgres;

--
-- Name: FUNCTION get_notification_description(param_notification_pkid integer, OUT return_description character varying); Type: COMMENT; Schema: sensorweb2; Owner: postgres
--

COMMENT ON FUNCTION get_notification_description(param_notification_pkid integer, OUT return_description character varying) IS 'Liefert die Beschreibung einer Benachrichtigung';


--
-- Name: get_notification_last_event_log_id(integer); Type: FUNCTION; Schema: sensorweb2; Owner: postgres
--

CREATE FUNCTION get_notification_last_event_log_id(param_notification_pkid integer) RETURNS integer
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$

/*
Ermittelt zu einer Notification den zuletzt gespeicherten Event-Log-Eintrag. Dabei gilt:
  1. Bei Rule-Events wird der aktuellste Event zur Notification ermittelt
  2. Bei Series-Events, hier: Ein-/Ausschalten des Eventing-Flags, wird der aktuellste Event der Series ermittelt
  3. Bei (extern initiierten) Series-Events, hier: Alterskontrolle, wird der aktuellste Event der Series ermittelt
  4. Subscription-Events werden nicht beruecksichtigt
  5. Von den unter 1.-3. ermittelten Event-Log-PKIDs wird die aktuellste zurueckgegeben
Fachliche Konzeption der Funktionalitaet mit CMi-2018-04-26
tt-2018-04-27

Beispiel: SELECT sensorweb2.get_notification_last_event_log_id(608); -->
*/

DECLARE

  var_event_log_pkid_1     sensorweb2.event_log.pkid%TYPE := NULL;
  var_event_log_pkid_23    sensorweb2.event_log.pkid%TYPE := NULL;

BEGIN

  -- Pruefen Rule-Events
  SELECT MAX(event_log.pkid)
  INTO var_event_log_pkid_1
  FROM sensorweb2.notification_rule
    INNER JOIN sensorweb2.event_log ON event_log.rule_pkid = notification_rule.rule_pkid
  WHERE notification_rule.notification_pkid = param_notification_pkid;
  
  -- Pruefen Series-Events (Eventing-Flag und Alterskontrolle)
  SELECT MAX(event_log.pkid)
  INTO var_event_log_pkid_23
  FROM sensorweb2.event_log
  WHERE event_log.event_type_pkid IN (2, 3)
    AND event_log.series_pkid = (SELECT series_pkid FROM sensorweb2.notification WHERE pkid = param_notification_pkid);

  -- Ergebnis
  RETURN GREATEST(var_event_log_pkid_1, var_event_log_pkid_23);
  
END

$$;


ALTER FUNCTION sensorweb2.get_notification_last_event_log_id(param_notification_pkid integer) OWNER TO postgres;

--
-- Name: FUNCTION get_notification_last_event_log_id(param_notification_pkid integer); Type: COMMENT; Schema: sensorweb2; Owner: postgres
--

COMMENT ON FUNCTION get_notification_last_event_log_id(param_notification_pkid integer) IS 'Ermittelt zu einer Notification die Primary Key ID des zuletzt zugeordneten Event-Log-Eintrags';


--
-- Name: get_series_description(integer); Type: FUNCTION; Schema: sensorweb2; Owner: postgres
--

CREATE FUNCTION get_series_description(pseriespkid integer, OUT rdescription character varying) RETURNS character varying
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
-- Ermittelt die Beschreibung einer Zeitreihe
-- Beispiel: 
--   SELECT sensorweb2.get_series_description(8); -> 'Abfluss Kluserbrücke, Einzelwert [m³/s]'
-- tt-2015-02-19
BEGIN
  SELECT
    phenomenon.phenomenon_name ||
    ' ' || feature_of_interest.feature_of_interest_name ||
    ', ' || procedure.procedure_name ||
    ' [' || unit.unit || ']'
  INTO rDescription
  FROM sensorweb2.series
  LEFT OUTER JOIN sensorweb2.phenomenon on phenomenon.pkid = series.phenomenon_pkid
  LEFT OUTER JOIN sensorweb2.feature_of_interest on feature_of_interest.pkid = series.feature_of_interest_pkid
  LEFT OUTER JOIN sensorweb2.procedure on procedure.pkid = series.procedure_pkid
  LEFT OUTER JOIN sensorweb2.unit on unit.pkid = series.unit_pkid
  WHERE series.pkid = pSeriesPkid;
END;

$$;


ALTER FUNCTION sensorweb2.get_series_description(pseriespkid integer, OUT rdescription character varying) OWNER TO postgres;

--
-- Name: FUNCTION get_series_description(pseriespkid integer, OUT rdescription character varying); Type: COMMENT; Schema: sensorweb2; Owner: postgres
--

COMMENT ON FUNCTION get_series_description(pseriespkid integer, OUT rdescription character varying) IS 'Ermittelt die Beschreibung einer Zeitreihe (tt-2015-02-19)';


--
-- Name: get_series_description_with_category(integer); Type: FUNCTION; Schema: sensorweb2; Owner: postgres
--

CREATE FUNCTION get_series_description_with_category(pseriespkid integer, OUT rdescription character varying) RETURNS character varying
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$

-- Ermittelt die Beschreibung einer Zeitreihe
-- Beispiel: 
--   SELECT sensorweb2.get_series_description_with_category(8); -> 'Gewässer: Abfluss Kluserbrücke, Einzelwert [m³/s]'
-- tt-2018-03-16

BEGIN

  SELECT
    category.category_name || ': ' ||
    phenomenon.phenomenon_name ||
    ' ' || feature_of_interest.feature_of_interest_name ||
    ', ' || procedure.procedure_name ||
    ' [' || COALESCE(unit.unit, '-') || ']'
  INTO rDescription
  FROM sensorweb2.series
    LEFT OUTER JOIN sensorweb2.category on category.pkid = series.category_pkid
    LEFT OUTER JOIN sensorweb2.phenomenon on phenomenon.pkid = series.phenomenon_pkid
    LEFT OUTER JOIN sensorweb2.feature_of_interest on feature_of_interest.pkid = series.feature_of_interest_pkid
    LEFT OUTER JOIN sensorweb2.procedure on procedure.pkid = series.procedure_pkid
    LEFT OUTER JOIN sensorweb2.unit on unit.pkid = series.unit_pkid
  WHERE series.pkid = pSeriesPkid;

END;

$$;


ALTER FUNCTION sensorweb2.get_series_description_with_category(pseriespkid integer, OUT rdescription character varying) OWNER TO postgres;

--
-- Name: FUNCTION get_series_description_with_category(pseriespkid integer, OUT rdescription character varying); Type: COMMENT; Schema: sensorweb2; Owner: postgres
--

COMMENT ON FUNCTION get_series_description_with_category(pseriespkid integer, OUT rdescription character varying) IS 'Ermittelt die Beschreibung einer Zeitreihe incl. Kategorie';


--
-- Name: get_series_description_with_ids(integer); Type: FUNCTION; Schema: sensorweb2; Owner: postgres
--

CREATE FUNCTION get_series_description_with_ids(pseriespkid integer, OUT rdescription character varying) RETURNS character varying
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
-- Ermittelt die Beschreibung einer Zeitreihe
-- Beispiel: 
--   SELECT sensorweb2.get_series_description_with_ids(8); -> 'Abfluss Kluserbrücke, Einzelwert [m³/s]'
-- tt-2017-05-09
BEGIN
  SELECT
    '#' || phenomenon.pkid || ' ' || phenomenon.phenomenon_name ||
    ' #' || feature_of_interest.pkid || ' ' || feature_of_interest.feature_of_interest_name ||
    ', #' || procedure.pkid || ' ' || procedure.procedure_name ||
    ' [#' || unit.pkid || ' ' || unit.unit || ']'
  INTO rDescription
  FROM sensorweb2.series
  LEFT OUTER JOIN sensorweb2.phenomenon on phenomenon.pkid = series.phenomenon_pkid
  LEFT OUTER JOIN sensorweb2.feature_of_interest on feature_of_interest.pkid = series.feature_of_interest_pkid
  LEFT OUTER JOIN sensorweb2.procedure on procedure.pkid = series.procedure_pkid
  LEFT OUTER JOIN sensorweb2.unit on unit.pkid = series.unit_pkid
  WHERE series.pkid = pSeriesPkid;
END;

$$;


ALTER FUNCTION sensorweb2.get_series_description_with_ids(pseriespkid integer, OUT rdescription character varying) OWNER TO postgres;

--
-- Name: FUNCTION get_series_description_with_ids(pseriespkid integer, OUT rdescription character varying); Type: COMMENT; Schema: sensorweb2; Owner: postgres
--

COMMENT ON FUNCTION get_series_description_with_ids(pseriespkid integer, OUT rdescription character varying) IS 'Ermittelt die Beschreibung einer Zeitreihe incl. den einzelnen IDs (tt-2017-05-09)';


--
-- Name: get_series_description_without_unit(integer); Type: FUNCTION; Schema: sensorweb2; Owner: postgres
--

CREATE FUNCTION get_series_description_without_unit(pseriespkid integer, OUT rdescription character varying) RETURNS character varying
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
-- Ermittelt die Beschreibung einer Zeitreihe
-- Beispiel: 
--   SELECT sensorweb2.get_series_description_without_unit(8); -> 'Abfluss Kluserbrücke, Einzelwert'
-- tt-2018-03-09
BEGIN
  SELECT
    phenomenon.phenomenon_name ||
    ' ' || feature_of_interest.feature_of_interest_name ||
    ', ' || procedure.procedure_name
  INTO rDescription
  FROM sensorweb2.series
    LEFT OUTER JOIN sensorweb2.phenomenon on phenomenon.pkid = series.phenomenon_pkid
    LEFT OUTER JOIN sensorweb2.feature_of_interest on feature_of_interest.pkid = series.feature_of_interest_pkid
    LEFT OUTER JOIN sensorweb2.procedure on procedure.pkid = series.procedure_pkid
    LEFT OUTER JOIN sensorweb2.unit on unit.pkid = series.unit_pkid
  WHERE series.pkid = pSeriesPkid;
END;

$$;


ALTER FUNCTION sensorweb2.get_series_description_without_unit(pseriespkid integer, OUT rdescription character varying) OWNER TO postgres;

--
-- Name: FUNCTION get_series_description_without_unit(pseriespkid integer, OUT rdescription character varying); Type: COMMENT; Schema: sensorweb2; Owner: postgres
--

COMMENT ON FUNCTION get_series_description_without_unit(pseriespkid integer, OUT rdescription character varying) IS 'Ermittelt die Beschreibung einer Zeitreihe ohne Einheit';


--
-- Name: notification_list_events(integer, timestamp without time zone, timestamp without time zone); Type: FUNCTION; Schema: sensorweb2; Owner: sensorweb2
--

CREATE FUNCTION notification_list_events(param_notification_pkid integer, param_time_stamp_from timestamp without time zone, param_time_stamp_to timestamp without time zone) RETURNS TABLE(event_log_pkid integer)
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
/*
Ermittelt zu einer Notification die gespeicherten Events im angegebenen Zeitraum. Dabei gilt:
  1. Bei Rule-Events werden alle PKIDs ermittelt, die zu einer Notification gehoeren
  2. Bei Series-Events, hier: Ein-/Ausschalten des Eventing-Flags, werden alle PKIDs ermittelt, die zu einer Notification gehoeren
  3. Bei (extern initiierten) Series-Events, hier: Alterskontrolle, werden alle PKIDs ermittelt, die zu einer Notification gehoeren
  4. Subscription-Events werden nicht beruecksichtigt
  5. Von den unter 1.-3. ermittelten Event-Log-PKIDs werden nur die Events im angegebenen Zeitraum zurueckgegeben
Fachliche Konzeption der Funktionalitaet mit CMi-2018-07-09
tt-2018-07-09

Beispiel: select * from sensorweb2.notification_list_events(5, '2016-07-09', '2018-07-09');
Ergebnis:



*/

DECLARE

  recYear RECORD;
  
BEGIN

  -- Ermitteln der Rule-Events
  RETURN QUERY
    SELECT event_log.pkid
    FROM sensorweb2.notification_rule
      INNER JOIN sensorweb2.event_log 
        ON event_log.rule_pkid = notification_rule.rule_pkid
        AND event_log.time_stamp_created BETWEEN param_time_stamp_from AND param_time_stamp_to
    WHERE notification_rule.notification_pkid = param_notification_pkid;

  -- Ermitteln der Series-Events (Eventing-Flag und Alterskontrolle)
  RETURN QUERY
    SELECT event_log.pkid
    FROM sensorweb2.event_log
    WHERE event_log.event_type_pkid IN (2, 3)
      AND event_log.series_pkid = (SELECT series_pkid FROM sensorweb2.notification WHERE pkid = param_notification_pkid)
      AND event_log.time_stamp_created BETWEEN param_time_stamp_from AND param_time_stamp_to;

  RETURN;
END;

$$;


ALTER FUNCTION sensorweb2.notification_list_events(param_notification_pkid integer, param_time_stamp_from timestamp without time zone, param_time_stamp_to timestamp without time zone) OWNER TO sensorweb2;

--
-- Name: FUNCTION notification_list_events(param_notification_pkid integer, param_time_stamp_from timestamp without time zone, param_time_stamp_to timestamp without time zone); Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON FUNCTION notification_list_events(param_notification_pkid integer, param_time_stamp_from timestamp without time zone, param_time_stamp_to timestamp without time zone) IS 'Ermittelt zu einer Notification die gespeicherten Events im angegebenen Zeitraum';


--
-- Name: observation_del_to_series(); Type: FUNCTION; Schema: sensorweb2; Owner: postgres
--

CREATE FUNCTION observation_del_to_series() RETURNS trigger
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
DECLARE

  c1 CURSOR FOR
    SELECT 
      first_time_stamp,
      first_numeric_value,
      last_time_stamp,
      last_numeric_value
    FROM sensorweb2.series
    WHERE pkid = OLD.series_pkid;
  
  first_ts TIMESTAMP;
  first_val NUMERIC;
  last_ts TIMESTAMP;
  last_val NUMERIC;
  
  max_ts TIMESTAMP;
  max_val NUMERIC;
  min_ts TIMESTAMP;
  min_val NUMERIC;
  
BEGIN

  -- series abfragen
  OPEN c1;
  FETCH c1 INTO first_ts, first_val, last_ts, last_val;

  -- Zeitpunkte und Werte vergleichen
  CASE

    -- Geloeschter Zeitpunkt ist der letzte
    WHEN OLD.time_stamp = last_ts THEN
      -- Vorangehenden Zeitpunkt ermitteln
      max_ts := NULL;
      SELECT MAX(time_stamp)
      INTO max_ts
      FROM sensorweb2.observation
      WHERE series_pkid = OLD.series_pkid;
      IF max_ts IS NULL THEN
        -- Es gibt keinen vorangehenden Zeitpunkt
        UPDATE sensorweb2.series SET
          last_time_stamp = NULL, 
          last_numeric_value = NULL 
        WHERE CURRENT OF c1;
      ELSE
        -- Wert des vorangehenden Zeitpunktes ermitteln
        SELECT numeric_value
        INTO max_val
        FROM sensorweb2.observation
        WHERE series_pkid = OLD.series_pkid
          AND time_stamp = max_ts;
        -- series aktualisieren
        UPDATE sensorweb2.series SET
          last_time_stamp = max_ts, 
          last_numeric_value = max_val 
        WHERE CURRENT OF c1;
      END IF;
      
    -- Geloeschter Zeitpunkt ist der erste
    WHEN OLD.time_stamp = first_ts THEN
      -- Nachfolgenden Zeitpunkt ermitteln
      min_ts := NULL;
      SELECT MIN(time_stamp)
      INTO min_ts
      FROM sensorweb2.observation
      WHERE series_pkid = OLD.series_pkid;
      IF min_ts IS NULL THEN
        -- Es gibt keinen nachfolgenden Zeitpunkt
        UPDATE sensorweb2.series SET
          first_time_stamp = NULL, 
          first_numeric_value = NULL 
        WHERE CURRENT OF c1;
      ELSE
        -- Wert des nachfolgenden Zeitpunktes ermitteln
        SELECT numeric_value
        INTO min_val
        FROM sensorweb2.observation
        WHERE series_pkid = OLD.series_pkid
          AND time_stamp = min_ts;
        -- series aktualisieren
        UPDATE sensorweb2.series SET
          first_time_stamp = min_ts, 
          first_numeric_value = min_val 
        WHERE CURRENT OF c1;
      END IF;
      
    -- Geloeschter Zeitpunkt ist der einzig verbliebene
    WHEN OLD.time_stamp = last_ts AND OLD.time_stamp = first_ts THEN
      -- Vorangehenden Zeitpunkt ermitteln (zur Sicherheit)
      max_ts := NULL;
      min_ts := NULL;
      SELECT MAX(time_stamp), MIN(time_stamp)
      INTO max_ts, min_ts
      FROM sensorweb2.observation
      WHERE series_pkid = OLD.series_pkid;
      IF max_ts IS NULL AND min_ts IS NULL THEN
        -- Es gibt keinen Zeitpunkt mehr
        UPDATE sensorweb2.series SET
          last_time_stamp = NULL, 
          last_numeric_value = NULL, 
          first_time_stamp = NULL, 
          first_numeric_value = NULL 
        WHERE CURRENT OF c1;
      ELSE
        -- Zeitpunkte speichern
        SELECT numeric_value
        INTO max_val
        FROM sensorweb2.observation
        WHERE series_pkid = OLD.series_pkid
          AND time_stamp = max_ts;
        SELECT numeric_value
        INTO min_val
        FROM sensorweb2.observation
        WHERE series_pkid = OLD.series_pkid
          AND time_stamp = max_ts;
        -- series aktualisieren
        UPDATE sensorweb2.series SET
          last_time_stamp = max_ts, 
          last_numeric_value = max_val, 
          first_time_stamp = min_ts, 
          first_numeric_value = min_val 
        WHERE CURRENT OF c1;
      END IF;
      
    ELSE NULL;

  END CASE;

  CLOSE c1;
  RETURN NEW;

END
$$;


ALTER FUNCTION sensorweb2.observation_del_to_series() OWNER TO postgres;

--
-- Name: FUNCTION observation_del_to_series(); Type: COMMENT; Schema: sensorweb2; Owner: postgres
--

COMMENT ON FUNCTION observation_del_to_series() IS 'delete in observation changes the first/last timestamps and values in series (tt-2014-03-18)';


--
-- Name: observation_generate_events(); Type: FUNCTION; Schema: sensorweb2; Owner: postgres
--

CREATE FUNCTION observation_generate_events() RETURNS trigger
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$

DECLARE
  -- Lokale Variablen
  var_eventing            sensorweb2.series.eventing_flag%TYPE;       -- Eventing-Steuerungskennzeichen einer Zeitreihe
  var_last_ts             sensorweb2.series.last_time_stamp%TYPE;     -- Zeitpunkt des zuletzt gemessenen Wertes
  var_last_val            sensorweb2.series.last_numeric_value%TYPE;  -- zuletzt gemessener Wert
  var_min_val             sensorweb2.series.last_numeric_value%TYPE;  -- minimum value
  var_max_val             sensorweb2.series.last_numeric_value%TYPE;  -- maximum value
  var_rule_val            sensorweb2.rule.threshold%TYPE;             -- Vergleichswert
  var_rule_trend          sensorweb2.rule.trend_code%TYPE;            -- Trend von letztem und aktuellem Wert bezueglich Vergleichswert
  var_rule_pkid           sensorweb2.rule.pkid%TYPE;                  -- Primary Key ID einer Regel
  var_event_log_pkid      sensorweb2.event_log.pkid%TYPE;             -- Primary Key ID des Event Log Eintrags
  var_content             sensorweb2.event_log_mail.content%TYPE;     -- Generierte Beschreibung eines Events
  rec_user                RECORD;                                     -- (Teile eines) User Record
  rec_description         RECORD;                                     -- Texte (Beschreibungen) eines Event Log Eintrags
  -- Grenzwerte aufsteigend
  cur_rule_asc CURSOR FOR
    SELECT DISTINCT
      threshold
    FROM sensorweb2.rule
    WHERE series_pkid = NEW.series_pkid
      AND threshold BETWEEN var_min_val AND var_max_val
    ORDER BY threshold ASC; 
  -- Grenzwerte absteigend
  cur_rule_desc CURSOR FOR
    SELECT DISTINCT
      threshold
    FROM sensorweb2.rule
    WHERE series_pkid = NEW.series_pkid
      AND threshold BETWEEN var_min_val AND var_max_val
    ORDER BY threshold DESC; 
BEGIN
  -- Ermitteln des Eventing-Kennzeichens und der letzten Beobachtung
  -- Hinweis: das funktioniert so nur mit einem *BEFORE* INSERT TRIGGER (weil sonst schon die last_-Spalten gefüllt wären)
  -- Hinweis: hier keinen Cursor verwenden, weil sonst Fehler zu bereis verwendeten Cursor-Namen auftreten (wegen kaskadierenden Triggern)  
  var_eventing := NULL; 
  var_last_ts := NULL; 
  var_last_val := NULL;
  SELECT eventing_flag, last_time_stamp, last_numeric_value
    INTO var_eventing, var_last_ts, var_last_val
  FROM sensorweb2.series
  WHERE pkid = NEW.series_pkid; 
  IF var_eventing IS NULL THEN
    -- Das Eventing-Kennzeichen ist leer => Keine weitere Verarbeitung 
    RETURN NEW; 
  END IF; 
  IF var_eventing = 0 THEN
    -- Das Eventing-Kennzeichen ist 0 => Keine weitere Verarbeitung 
    RETURN NEW; 
  END IF; 
  IF var_last_ts IS NOT NULL AND var_last_val IS NOT NULL THEN
    -- Es gibt einen zuletzt gemessen Wert mit Zeitpunkt
    IF NEW.time_stamp > var_last_ts THEN
      -- Der neue Zeitpunkt ist neuer als der letzte Zeitpunkt der Zeitreihe
      -- Ermitteln aller definierten Grenzwerte der aktuellen Zeitreihe zwischen dem vorangehenden und dem aktuellen Messwert
      var_min_val := LEAST(var_last_val, NEW.numeric_value); 
      var_max_val := GREATEST(var_last_val, NEW.numeric_value); 
      IF var_last_val <= NEW.numeric_value THEN
        -- Ermittelte Grenzwerte aufsteigend verarbeiten
        OPEN cur_rule_asc; 
      ELSE
        -- Ermittelte Grenzwerte absteigend verarbeiten
        OPEN cur_rule_desc; 
      END IF; 
      LOOP
        -- Alle ermittelten Grenzwerte verarbeiten
        var_rule_val := NULL; 
        IF var_last_val <= NEW.numeric_value THEN
          -- Naechsten (groesseren) Grenzwert ermitteln
          FETCH cur_rule_asc INTO var_rule_val; 
        ELSE
          -- Naechsten (kleineren) Grenzwert ermitteln
          FETCH cur_rule_desc INTO var_rule_val; 
        END IF; 
        IF var_rule_val IS NULL THEN
          -- Schleifenabbruch: es gibt keinen Grenzwert (mehr) zwischen dem vorangehenen und dem aktuellen Messwert
          EXIT; 
        END IF; 
        -- also: es gibt mindestens einen Grenzwert zwischen dem vorangehenen und dem aktuellen Messwert
        -- pruefen: gibt es eine Kombination von Grenzwert und Trend?
        -- Trend vom vorangehenden zum aktuellen Wert berechnen
        CASE
          WHEN var_last_val < var_rule_val THEN var_rule_trend := 10; 
          WHEN var_last_val = var_rule_val THEN var_rule_trend := 20; 
          WHEN var_last_val > var_rule_val THEN var_rule_trend := 30; 
        END CASE; 
        CASE
          WHEN NEW.numeric_value < var_rule_val THEN var_rule_trend := var_rule_trend + 1; 
          WHEN NEW.numeric_value = var_rule_val THEN var_rule_trend := var_rule_trend + 2; 
          WHEN NEW.numeric_value > var_rule_val THEN var_rule_trend := var_rule_trend + 3; 
        END CASE; 
        -- (einzige) Regel der Zeitreihe ermitteln, die zu der Grenzwert-Trend-Kombination passt
        var_rule_pkid := 0;
        SELECT pkid
          INTO var_rule_pkid
          FROM sensorweb2.rule
          WHERE series_pkid = NEW.series_pkid
            AND threshold = var_rule_val
            AND trend_code = var_rule_trend;
        IF var_rule_pkid > 0 THEN
          -- also: es gibt eine Regel mit der Grenzwert-Trend-Kombination
          ----------------------------------------------------------------------
          -- Aktion 1: Eintrag im Ereignis-Log erzeugen
          INSERT INTO sensorweb2.event_log (
            time_stamp_created,
            event_type_pkid,
            series_pkid,
            rule_pkid, 
            observation_time_stamp, observation_numeric_value,
            observation_previous_time_stamp, observation_previous_numeric_value)
          VALUES (
            clock_timestamp(),
            1,  -- Regel-Ereignis
            NEW.series_pkid,
            var_rule_pkid,
            NEW.time_stamp, NEW.numeric_value,
            var_last_ts, var_last_val)
          RETURNING pkid INTO var_event_log_pkid;
          ----------------------------------------------------------------------
          -- Aktion 2: fuer alle Benutzer- und Gruppen-Abonnements der Regel einen Mail-Eintrag erstellen
          FOR rec_user IN
            -- alle E-Mail-Adressen ermitteln (incl. Benutzergruppen-Mitgliedschaften)
            SELECT
              user_.userid,
              user_.emailaddress,
              subscription.pkid AS subscription_pkid,
              notification_rule.notification_level_id
            FROM sensorweb2.rule
              INNER JOIN sensorweb2.notification_rule ON notification_rule.rule_pkid = rule.pkid
              INNER JOIN sensorweb2.subscription ON subscription.notification_pkid = notification_rule.notification_pkid 
              INNER JOIN sensorweb2.user_ ON user_.userid = subscription.userid
            WHERE rule.pkid = var_rule_pkid    
            UNION
            SELECT
              user_.userid,
              user_.emailaddress,
              subscription.pkid,
              notification_rule.notification_level_id
            FROM sensorweb2.rule
              INNER JOIN sensorweb2.notification_rule ON notification_rule.rule_pkid = rule.pkid
              INNER JOIN sensorweb2.subscription ON subscription.notification_pkid = notification_rule.notification_pkid 
              INNER JOIN sensorweb2.users_usergroups ON users_usergroups.usergroupid = subscription.usergroupid
              INNER JOIN sensorweb2.user_ ON user_.userid = users_usergroups.userid
            WHERE rule.pkid = var_rule_pkid  
          LOOP
            IF rec_user.emailaddress IS NOT NULL THEN
              -- Log-Eintrag fuer Mail-Versand erzeugen
              -- Anmerkung: ein externes Programm gruppiert noch nicht verschickte Eintraege und erzeugt Mails
              var_content := sensorweb2.get_event_log_description(var_event_log_pkid, rec_user.emailaddress, rec_user.subscription_pkid);
              INSERT INTO sensorweb2.event_log_mail (
                event_log_pkid,
                mail_address,
                subscription_pkid,
                content,
                userid,
                notification_level_id)
              VALUES (
                var_event_log_pkid,
                rec_user.emailaddress,
                rec_user.subscription_pkid,
                var_content,
                rec_user.userid,
                rec_user.notification_level_id);
            END IF;
          END LOOP;  -- user data (with subscription and notification data)
          ----------------------------------------------------------------------
        END IF;  -- rule (pkid > 0)
        ----------------------------------------------------------------------------------------------------
      END LOOP; 
      IF var_last_val <= NEW.numeric_value THEN
        CLOSE cur_rule_asc; 
      ELSE
        CLOSE cur_rule_desc; 
      END IF; 
    END IF; 
  END IF; 
  RETURN NEW; 
END

$$;


ALTER FUNCTION sensorweb2.observation_generate_events() OWNER TO postgres;

--
-- Name: FUNCTION observation_generate_events(); Type: COMMENT; Schema: sensorweb2; Owner: postgres
--

COMMENT ON FUNCTION observation_generate_events() IS 'look for rules that fits, then generate event log entry and entry for mail generation if nessesary';


--
-- Name: observation_ins_upd_to_series(); Type: FUNCTION; Schema: sensorweb2; Owner: postgres
--

CREATE FUNCTION observation_ins_upd_to_series() RETURNS trigger
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
DECLARE

  c1 CURSOR FOR
    SELECT 
      first_time_stamp, 
      first_numeric_value, 
      last_time_stamp, 
      last_numeric_value
    FROM sensorweb2.series
    WHERE pkid = NEW.series_pkid;
  
  first_ts TIMESTAMP;
  first_val NUMERIC;
  last_ts TIMESTAMP;
  last_val NUMERIC;

BEGIN

  -- series abfragen
  OPEN c1;
  FETCH c1 INTO first_ts, first_val, last_ts, last_val;

  -- Zeitpunkte und Werte vergleichen
  CASE

    -- Erster Eintrag in series => alle Felder fuellen
    WHEN first_ts IS NULL AND last_ts IS NULL THEN
      UPDATE sensorweb2.series SET
        first_time_stamp = NEW.time_stamp,
        first_numeric_value = NEW.numeric_value, 
        last_time_stamp = NEW.time_stamp, 
        last_numeric_value = NEW.numeric_value
      WHERE CURRENT OF c1;
      
    -- Erster Eintrag ist gefuellt, letzter Eintrag ist nicht gefuellt (sollte eigentlich nicht vorkommen)
    WHEN first_ts IS NULL AND last_ts IS NOT NULL THEN
      UPDATE sensorweb2.series SET
        first_time_stamp = NEW.time_stamp,
        first_numeric_value = NEW.numeric_value 
      WHERE CURRENT OF c1;
      
    -- Erster Eintrag ist nicht gefuellt, letzter Eintrag ist gefuellt (sollte eigentlich nicht vorkommen)
    WHEN first_ts IS NOT NULL AND last_ts IS NULL THEN
      UPDATE sensorweb2.series SET
        last_time_stamp = NEW.time_stamp, 
        last_numeric_value = NEW.numeric_value
      WHERE CURRENT OF c1;
      
    -- Neuer Zeitpunkt liegt nach dem bisher letzten Zeitpunkt => last-Felder aktualisieren
    WHEN NEW.time_stamp > last_ts THEN 
      UPDATE sensorweb2.series SET
        last_time_stamp = NEW.time_stamp, 
        last_numeric_value = NEW.numeric_value
      WHERE CURRENT OF c1;

    -- Letzter Zeitpunkt ist gleich, aber Wert ist geaendert => last-value-Feld aktualisieren
    WHEN NEW.time_stamp = last_ts AND NEW.numeric_value <> last_val THEN 
      UPDATE sensorweb2.series SET
        last_numeric_value = NEW.numeric_value
      WHERE CURRENT OF c1;

    -- Neuer Zeitpunkt liegt vor dem bisher ersten Zeitpunkt => first-Felder aktualisieren
    WHEN NEW.time_stamp < first_ts THEN
      UPDATE sensorweb2.series SET
        first_time_stamp = NEW.time_stamp, 
        first_numeric_value = NEW.numeric_value
      WHERE CURRENT OF c1;

    -- Erster Zeitpunkt ist gleich, aber Wert ist geaendert => first-value-Feld aktualisieren
    WHEN NEW.time_stamp = first_ts AND NEW.numeric_value <> first_val THEN
      UPDATE sensorweb2.series SET
        first_numeric_value = NEW.numeric_value
      WHERE CURRENT OF c1;
      
    ELSE NULL;
      
  END CASE;

  CLOSE c1;
  RETURN NEW;

END
$$;


ALTER FUNCTION sensorweb2.observation_ins_upd_to_series() OWNER TO postgres;

--
-- Name: FUNCTION observation_ins_upd_to_series(); Type: COMMENT; Schema: sensorweb2; Owner: postgres
--

COMMENT ON FUNCTION observation_ins_upd_to_series() IS 'insert or update in observation updates the first/last timestamps and values in series (2014-03-18)';


--
-- Name: observation_last(integer); Type: FUNCTION; Schema: sensorweb2; Owner: sensorweb2
--

CREATE FUNCTION observation_last(p_series_pkid integer, OUT time_stamp timestamp without time zone, OUT numeric_value numeric) RETURNS SETOF record
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
-- Beispiel: SELECT * FROM sensorweb2.observation_last(68);
-- Ergebnis:
-- --------------------+--------------
-- time_stamp          | numeric_value
-- --------------------+--------------
-- 2015-11-03 16:00:00 | 55.3388020000
-- --------------------+--------------

DECLARE

  tLast TIMESTAMP WITHOUT TIME ZONE;
  nLast NUMERIC(20,10); 

BEGIN

  -- Letzten Zeitpunkt ermitteln
  SELECT MAX(o.time_stamp) 
  INTO tLast
  FROM sensorweb2.observation o
  WHERE o.series_pkid = p_series_pkid;

  -- Wert des letzten Zeitpunkts
  SELECT o.numeric_value 
  INTO nLast
  FROM sensorweb2.observation o
  WHERE o.series_pkid = p_series_pkid
    AND o.time_stamp = tLast;

  -- Ermittelte Daten zurückgeben
  RETURN QUERY SELECT tLast, nLast;

  RETURN;
  
END;

$$;


ALTER FUNCTION sensorweb2.observation_last(p_series_pkid integer, OUT time_stamp timestamp without time zone, OUT numeric_value numeric) OWNER TO sensorweb2;

--
-- Name: FUNCTION observation_last(p_series_pkid integer, OUT time_stamp timestamp without time zone, OUT numeric_value numeric); Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON FUNCTION observation_last(p_series_pkid integer, OUT time_stamp timestamp without time zone, OUT numeric_value numeric) IS 'tt-2015-11-04 Ermittelt zu einer Zeitreihe den letzten Zeitpunkt mit dem zugehoerigen Messwert';


--
-- Name: observation_last_before(integer, timestamp without time zone); Type: FUNCTION; Schema: sensorweb2; Owner: postgres
--

CREATE FUNCTION observation_last_before(pseries integer, ptimestamp timestamp without time zone, OUT time_stamp timestamp without time zone, OUT numeric_value numeric) RETURNS SETOF record
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
-- Ermittelt zu einem Zeitpunkt einer Zeitreihe 
-- den davor zuletzt gespeicherten Zeitpunkt mit dem zugehoerigen Messwert
-- und gibt einen entsprechenden Datensatz zurueck.   
-- tt-2015-09-24
-- Beispiel: select * from sensorweb2.observation_last_before(66, '2015-09-24 14:00:00');
-- Ergebnis:
-- --------------------+---------------
-- time_stamp          | numeric_value
-- --------------------+---------------
-- 2015-09-24 13:52:00 | 90.4488980000
-- --------------------+---------------

DECLARE

  tLast sensorweb2.observation.time_stamp%TYPE;
  nLast sensorweb2.observation.numeric_value%TYPE; 

BEGIN

  -- Letzten Zeitpunkt ermitteln
  SELECT MAX(obs.time_stamp) 
  INTO tLast
  FROM sensorweb2.observation obs
  WHERE obs.series_pkid = pSeries
    AND obs.time_stamp < pTimestamp;

  -- Wert des letzten Zeitpunkts
  SELECT obs.numeric_value 
  INTO nLast
  FROM sensorweb2.observation obs
  WHERE obs.series_pkid = pSeries
    AND obs.time_stamp = tLast;

  -- Ermittelte Daten zurueckgeben
  RETURN QUERY SELECT tLast, nLast;

  RETURN;
END;

$$;


ALTER FUNCTION sensorweb2.observation_last_before(pseries integer, ptimestamp timestamp without time zone, OUT time_stamp timestamp without time zone, OUT numeric_value numeric) OWNER TO postgres;

--
-- Name: FUNCTION observation_last_before(pseries integer, ptimestamp timestamp without time zone, OUT time_stamp timestamp without time zone, OUT numeric_value numeric); Type: COMMENT; Schema: sensorweb2; Owner: postgres
--

COMMENT ON FUNCTION observation_last_before(pseries integer, ptimestamp timestamp without time zone, OUT time_stamp timestamp without time zone, OUT numeric_value numeric) IS 'tt-2015-09-24 Ermittelt den vor einem Zeitpunkt zuletzt gespeicherten Zeitpunkt mit dem zugehoerigen Messwert';


--
-- Name: observation_sync_derived(); Type: FUNCTION; Schema: sensorweb2; Owner: postgres
--

CREATE FUNCTION observation_sync_derived() RETURNS trigger
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
DECLARE

  op CHAR(1);
  sd RECORD;
  pkid_base INTEGER;
  newval NUMERIC(20,10);
  
BEGIN

  op := UPPER(LEFT(TG_OP, 1));  -- I/U/D fuer Insert/Update/Delete

  IF op = 'D' THEN
    pkid_base := OLD.series_pkid;
  END IF;
  IF op = 'I' OR op = 'U' THEN
    pkid_base := NEW.series_pkid;
  END IF;

  FOR sd IN 
    SELECT 
      series_pkid_derived,
      formula
    FROM sensorweb2.series_derived
    WHERE series_pkid_base = pkid_base
  LOOP
    IF op = 'D' OR op = 'U' THEN
      -- 1. Schritt: Delete oder Update: Alte Observation loeschen
      DELETE FROM sensorweb2.observation 
        WHERE series_pkid = sd.series_pkid_derived
          AND time_stamp = OLD.time_stamp;
    END IF;
    IF op = 'I' OR op = 'U' THEN
      -- 2. Schritt: Insert oder Update: Neue Observation speichern
      EXECUTE 'SELECT ' || REPLACE(sd.formula, '{x}', NEW.numeric_value::TEXT) INTO newval;
      INSERT INTO sensorweb2.observation (time_stamp, series_pkid, numeric_value, result_time)
        VALUES (NEW.time_stamp, sd.series_pkid_derived, newval, NEW.time_stamp);
    END IF;
  END LOOP;
  
  RETURN NEW;

END
$$;


ALTER FUNCTION sensorweb2.observation_sync_derived() OWNER TO postgres;

--
-- Name: FUNCTION observation_sync_derived(); Type: COMMENT; Schema: sensorweb2; Owner: postgres
--

COMMENT ON FUNCTION observation_sync_derived() IS 'synchronize derived observations (tt-2016-05-20)';


--
-- Name: series_generate_events(); Type: FUNCTION; Schema: sensorweb2; Owner: postgres
--

CREATE FUNCTION series_generate_events() RETURNS trigger
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$

DECLARE
  -- Lokale Variablen
  var_event_message_pkid  sensorweb2.event_message.pkid%TYPE;         -- Primary Key einer Event Message
  var_event_log_pkid      sensorweb2.event_log.pkid%TYPE;             -- Primary Key ID des Event Log Eintrags
  var_content             sensorweb2.event_log_mail.content%TYPE;     -- Generierte Beschreibung eines Events
  rec_user                RECORD;                                     -- (Teile eines) User Record
BEGIN
  var_event_message_pkid := 0;
  IF TG_OP = 'UPDATE' THEN
    -- Vergleich mit dem zuletzt gespeicherten Eventing-Kennzeichen
    IF OLD.eventing_flag = 0 AND NEW.eventing_flag = 1 THEN
      var_event_message_pkid := 1;  -- Ereignisprotokollierung eingeschaltet
    END IF;
    IF OLD.eventing_flag = 1 AND NEW.eventing_flag = 0 THEN
      var_event_message_pkid := 2;  -- Ereignisprotokollierung ausgeschaltet
    END IF;
  END IF;
  IF var_event_message_pkid > 0 THEN
    ----------------------------------------------------------------------
    -- Aktion 1: Eintrag im Ereignis-Log erzeugen
    INSERT INTO sensorweb2.event_log (
      event_type_pkid,
      series_pkid,
      event_message_pkid)
    VALUES (
      2,  -- Zeitreihen-Verwaltung
      NEW.pkid,
      var_event_message_pkid)
    RETURNING pkid INTO var_event_log_pkid;
    ----------------------------------------------------------------------
    -- Aktion 2: fuer alle Benutzer- und Gruppen-Abonnements einer Zeitreihe einen Mail-Eintrag erstellen
    FOR rec_user IN
      -- alle E-Mail-Adressen -unique- ermitteln (incl. Benutzergruppen-Mitgliedschaften), die irgendwas von der Zeitreihe abonniert haben
      SELECT DISTINCT
        user_.userid,
        user_.emailaddress
      FROM sensorweb2.notification
        INNER JOIN sensorweb2.subscription ON subscription.notification_pkid = notification.pkid 
        INNER JOIN sensorweb2.user_ ON user_.userid = subscription.userid
      WHERE notification.series_pkid = NEW.pkid
      UNION
      SELECT
        user_.userid,
        user_.emailaddress
      FROM sensorweb2.notification
        INNER JOIN sensorweb2.subscription ON subscription.notification_pkid = notification.pkid 
        INNER JOIN sensorweb2.users_usergroups ON users_usergroups.usergroupid = subscription.usergroupid
        INNER JOIN sensorweb2.user_ ON user_.userid = users_usergroups.userid
      WHERE notification.series_pkid = NEW.pkid
    LOOP
      IF rec_user.emailaddress IS NOT NULL THEN
        -- Log-Eintrag fuer Mail-Versand erzeugen
        -- Anmerkung: ein externes Programm gruppiert noch nicht verschickte Eintraege und erzeugt Mails
        var_content := sensorweb2.get_event_log_description(var_event_log_pkid, rec_user.emailaddress);
        INSERT INTO sensorweb2.event_log_mail (
          event_log_pkid,
          mail_address,
          content,
          userid)
        VALUES (
          var_event_log_pkid,
          rec_user.emailaddress,
          var_content,
          rec_user.userid);
      END IF;
    END LOOP;  -- email adresses
  END IF; 
  RETURN NEW; 
END

$$;


ALTER FUNCTION sensorweb2.series_generate_events() OWNER TO postgres;

--
-- Name: FUNCTION series_generate_events(); Type: COMMENT; Schema: sensorweb2; Owner: postgres
--

COMMENT ON FUNCTION series_generate_events() IS 'generate event log entries and entries for mail generation if nessesary';


--
-- Name: series_initialize_fields(); Type: FUNCTION; Schema: sensorweb2; Owner: postgres
--

CREATE FUNCTION series_initialize_fields() RETURNS trigger
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$

DECLARE
BEGIN
  NEW.pegel_online := 0;
  NEW.published_flag := 0;
  NEW.eventing_flag := 0;
  RETURN NEW; 
END

$$;


ALTER FUNCTION sensorweb2.series_initialize_fields() OWNER TO postgres;

--
-- Name: FUNCTION series_initialize_fields(); Type: COMMENT; Schema: sensorweb2; Owner: postgres
--

COMMENT ON FUNCTION series_initialize_fields() IS 'initialize series fields (new record content)';


--
-- Name: subscription_generate_events(); Type: FUNCTION; Schema: sensorweb2; Owner: postgres
--

CREATE FUNCTION subscription_generate_events() RETURNS trigger
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$

  -- Das Definieren oder Entfernen eines Abonnements zu einer Benutzergruppe oder einem Benutzer
  -- ... generiert ein Ereignis
  -- tt-2018-03-23

DECLARE

  var_usergroupid             sensorweb2.usergroup.usergroupid%TYPE;      -- Primary Key ID der Usergroup
  var_userid                  sensorweb2.user_.userid%TYPE;               -- Primary Key ID des Users
  var_notification_pkid       sensorweb2.notification.pkid%TYPE;          -- Primary Key ID der Notification
  var_event_message_pkid      sensorweb2.event_message.pkid%TYPE;         -- Primary Key ID der Event Message
  var_series_pkid             sensorweb2.series.pkid%TYPE;                -- Primary Key ID der Series
  var_event_log_pkid          sensorweb2.event_log.pkid%TYPE;             -- Primary Key ID des Event Log Eintrags
  var_event_log_mail_content  sensorweb2.event_log_mail.content%TYPE;     -- Generierte Beschreibung eines Events
  rec_user                    RECORD;                                     -- (Teile eines) User Record
  
BEGIN

  ----------------------------------------------------------------------------------------------------
  -- Vergleich 1: Gruppen-Identifikation
  var_usergroupid := NULL;
  var_notification_pkid := NULL;
  var_event_message_pkid := 0;
  CASE TG_OP 
    WHEN 'INSERT' THEN 
      IF NEW.usergroupid IS NOT NULL THEN
        var_event_message_pkid := 5;  -- Neues Abonnement einer Benachrichtigung zu einer Benutzergruppe definiert
        var_notification_pkid := NEW.notification_pkid;
        var_usergroupid := NEW.usergroupid;
      END IF;
    WHEN 'UPDATE' THEN 
      IF OLD.usergroupid IS NULL AND NEW.usergroupid IS NOT NULL THEN
        var_event_message_pkid := 5;  -- Neues Abonnement einer Benachrichtigung zu einer Benutzergruppe definiert
        var_notification_pkid := NEW.notification_pkid;
        var_usergroupid := NEW.usergroupid;
      END IF;
      IF OLD.usergroupid IS NOT NULL AND NEW.usergroupid IS NULL THEN
        var_event_message_pkid := 6;  -- Bestehendes Abonnement einer Benachrichtigung zu einer Benutzergruppe entfernt
        var_notification_pkid := OLD.notification_pkid;
        var_usergroupid := OLD.usergroupid;
      END IF;
    WHEN 'DELETE' THEN 
      IF OLD.usergroupid IS NOT NULL THEN
        var_event_message_pkid := 6;  -- Bestehendes Abonnement einer Benachrichtigung zu einer Benutzergruppe entfernt
        var_notification_pkid := OLD.notification_pkid;
        var_usergroupid := OLD.usergroupid;
      END IF;
  END CASE;

  IF var_event_message_pkid > 0 THEN

    -- Aktion 1.1: Zeitreihe ermitteln
    SELECT series_pkid
      INTO var_series_pkid
      FROM sensorweb2.notification
      WHERE pkid = var_notification_pkid;

    -- Aktion 1.2: Eintrag im Ereignis-Log erzeugen, hier: usergroup und notification
    INSERT INTO sensorweb2.event_log (
      event_type_pkid,
      series_pkid,
      event_message_pkid,
      usergroup_usergroupid,
      notification_pkid)
    VALUES (
      4,  -- Abonnement
      var_series_pkid,
      var_event_message_pkid,
      var_usergroupid,
      var_notification_pkid)
    RETURNING pkid INTO var_event_log_pkid;

    -- Aktion 1.3: fuer alle ... einen Mail-Eintrag erstellen
    FOR rec_user IN
      -- alle E-Mail-Adressen ermitteln, die eine Benachrichtigung zu Abonnements erhalten sollen
      -- TEST-TEST-TEST-TEST-TEST-TEST-TEST:
      SELECT DISTINCT
        user_.userid,
        user_.emailaddress
      FROM sensorweb2.user_
      WHERE user_.emailaddress = 'tt@wupperverband.de'
    LOOP
      IF rec_user.emailaddress IS NOT NULL THEN
        -- Log-Eintrag fuer Mail-Versand erzeugen
        -- Anmerkung: ein externes Programm gruppiert noch nicht verschickte Eintraege und erzeugt Mails
        var_event_log_mail_content := sensorweb2.get_event_log_description(var_event_log_pkid, rec_user.emailaddress);
        INSERT INTO sensorweb2.event_log_mail (
          event_log_pkid,
          mail_address,
          content,
          userid)
        VALUES (
          var_event_log_pkid,
          rec_user.emailaddress,
          var_event_log_mail_content,
          rec_user.userid);
      END IF;
    END LOOP;  -- email adresses

  END IF; -- event message

  ----------------------------------------------------------------------------------------------------
  -- Vergleich 2: Benutzer-Identifikation
  var_userid := NULL;
  var_notification_pkid := NULL;
  var_event_message_pkid := 0;
  CASE TG_OP 
    WHEN 'INSERT' THEN 
      IF NEW.userid IS NOT NULL THEN
        var_event_message_pkid := 7;  -- Neues Abonnement einer Benachrichtigung zu einem Benutzer definiert
        var_notification_pkid := NEW.notification_pkid;
        var_userid := NEW.userid;
      END IF;
    WHEN 'UPDATE' THEN 
      IF OLD.userid IS NULL AND NEW.userid IS NOT NULL THEN
        var_event_message_pkid := 7;  -- Neues Abonnement einer Benachrichtigung zu einem Benutzer definiert
        var_notification_pkid := NEW.notification_pkid;
        var_userid := NEW.userid;
      END IF;
      IF OLD.userid IS NOT NULL AND NEW.userid IS NULL THEN
        var_event_message_pkid := 8;  -- Bestehendes Abonnement einer Benachrichtigung zu einem Benutzer entfernt
        var_notification_pkid := OLD.notification_pkid;
        var_userid := OLD.userid;
      END IF;
    WHEN 'DELETE' THEN 
      IF OLD.userid IS NOT NULL THEN
        var_event_message_pkid := 8;  -- Bestehendes Abonnement einer Benachrichtigung zu einem Benutzer entfernt
        var_notification_pkid := OLD.notification_pkid;
        var_userid := OLD.userid;
      END IF;
  END CASE;

  IF var_event_message_pkid > 0 THEN

    -- Aktion 2.1: Zeitreihe ermitteln
    SELECT series_pkid
      INTO var_series_pkid
      FROM sensorweb2.notification
      WHERE pkid = var_notification_pkid;

    -- Aktion 2.2: Eintrag im Ereignis-Log erzeugen, hier: user und notification
    INSERT INTO sensorweb2.event_log (
      event_type_pkid,
      series_pkid,
      event_message_pkid,
      user_userid,
      notification_pkid)
    VALUES (
      4,  -- Abonnement
      var_series_pkid,
      var_event_message_pkid,
      var_userid,
      var_notification_pkid)
    RETURNING pkid INTO var_event_log_pkid;

    -- Aktion 2.3: fuer alle ... einen Mail-Eintrag erstellen
    FOR rec_user IN
      -- alle E-Mail-Adressen ermitteln, die eine Benachrichtigung zu Abonnements erhalten sollen
      -- TEST-TEST-TEST-TEST-TEST-TEST-TEST:
      SELECT DISTINCT
        user_.userid,
        user_.emailaddress
      FROM sensorweb2.user_
      WHERE user_.emailaddress = 'tt@wupperverband.de'
    LOOP
      IF rec_user.emailaddress IS NOT NULL THEN
        -- Log-Eintrag fuer Mail-Versand erzeugen
        -- Anmerkung: ein externes Programm gruppiert noch nicht verschickte Eintraege und erzeugt Mails
        var_event_log_mail_content := sensorweb2.get_event_log_description(var_event_log_pkid, rec_user.emailaddress);
        INSERT INTO sensorweb2.event_log_mail (
          event_log_pkid,
          mail_address,
          content,
          userid)
        VALUES (
          var_event_log_pkid,
          rec_user.emailaddress,
          var_event_log_mail_content,
          rec_user.userid);
      END IF;
    END LOOP;  -- email adresses

  END IF; -- event message

  ----------------------------------------------------------------------------------------------------
  RETURN NEW; 
END

$$;


ALTER FUNCTION sensorweb2.subscription_generate_events() OWNER TO postgres;

--
-- Name: FUNCTION subscription_generate_events(); Type: COMMENT; Schema: sensorweb2; Owner: postgres
--

COMMENT ON FUNCTION subscription_generate_events() IS 'generate event log entries and entries for mail generation if nessesary';


SET default_with_oids = false;

--
-- Name: category; Type: TABLE; Schema: sensorweb2; Owner: sensorweb2
--

CREATE TABLE category (
    pkid integer NOT NULL,
    category_id character varying(100) NOT NULL,
    category_name character varying(100),
    description character varying(100)
);


ALTER TABLE sensorweb2.category OWNER TO sensorweb2;

--
-- Name: TABLE category; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON TABLE category IS 'represents categories, which classifies series';


--
-- Name: COLUMN category.pkid; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN category.pkid IS 'serial primary key';


--
-- Name: COLUMN category.category_id; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN category.category_id IS 'identification of the category without special chars';


--
-- Name: COLUMN category.category_name; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN category.category_name IS 'default name of the category';


--
-- Name: COLUMN category.description; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN category.description IS 'default description of the category';


--
-- Name: category_i18n; Type: TABLE; Schema: sensorweb2; Owner: sensorweb2
--

CREATE TABLE category_i18n (
    pkid integer NOT NULL,
    category_pkid integer NOT NULL,
    locale character varying(10) NOT NULL,
    name character varying(100),
    description character varying(100)
);


ALTER TABLE sensorweb2.category_i18n OWNER TO sensorweb2;

--
-- Name: TABLE category_i18n; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON TABLE category_i18n IS 'internationalization of categories';


--
-- Name: COLUMN category_i18n.pkid; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN category_i18n.pkid IS 'serial primary key';


--
-- Name: COLUMN category_i18n.category_pkid; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN category_i18n.category_pkid IS 'Id of the category the record belongs to';


--
-- Name: COLUMN category_i18n.locale; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN category_i18n.locale IS 'language identifier and region identifier';


--
-- Name: COLUMN category_i18n.name; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN category_i18n.name IS 'name of the category in locale''s language';


--
-- Name: COLUMN category_i18n.description; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN category_i18n.description IS 'description of the category in locale''s language';


--
-- Name: category_i18n_pkid_seq; Type: SEQUENCE; Schema: sensorweb2; Owner: sensorweb2
--

CREATE SEQUENCE category_i18n_pkid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE sensorweb2.category_i18n_pkid_seq OWNER TO sensorweb2;

--
-- Name: category_i18n_pkid_seq; Type: SEQUENCE OWNED BY; Schema: sensorweb2; Owner: sensorweb2
--

ALTER SEQUENCE category_i18n_pkid_seq OWNED BY category_i18n.pkid;


--
-- Name: category_pkid_seq; Type: SEQUENCE; Schema: sensorweb2; Owner: sensorweb2
--

CREATE SEQUENCE category_pkid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE sensorweb2.category_pkid_seq OWNER TO sensorweb2;

--
-- Name: category_pkid_seq; Type: SEQUENCE OWNED BY; Schema: sensorweb2; Owner: sensorweb2
--

ALTER SEQUENCE category_pkid_seq OWNED BY category.pkid;


--
-- Name: cluster; Type: TABLE; Schema: sensorweb2; Owner: sensorweb2
--

CREATE TABLE cluster (
    pkid integer NOT NULL,
    cluster_id character varying(100) NOT NULL,
    cluster_name character varying(100),
    description character varying(100)
);


ALTER TABLE sensorweb2.cluster OWNER TO sensorweb2;

--
-- Name: TABLE cluster; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON TABLE cluster IS 'represents clusters, which bundels series';


--
-- Name: COLUMN cluster.pkid; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN cluster.pkid IS 'serial primary key';


--
-- Name: COLUMN cluster.cluster_id; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN cluster.cluster_id IS 'identification of the cluster without special chars';


--
-- Name: COLUMN cluster.cluster_name; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN cluster.cluster_name IS 'default name of the cluster';


--
-- Name: COLUMN cluster.description; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN cluster.description IS 'default description of the cluster';


--
-- Name: cluster_pkid_seq; Type: SEQUENCE; Schema: sensorweb2; Owner: sensorweb2
--

CREATE SEQUENCE cluster_pkid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE sensorweb2.cluster_pkid_seq OWNER TO sensorweb2;

--
-- Name: cluster_pkid_seq; Type: SEQUENCE OWNED BY; Schema: sensorweb2; Owner: sensorweb2
--

ALTER SEQUENCE cluster_pkid_seq OWNED BY cluster.pkid;


--
-- Name: event_log; Type: TABLE; Schema: sensorweb2; Owner: sensorweb2
--

CREATE TABLE event_log (
    pkid integer NOT NULL,
    time_stamp_created timestamp without time zone DEFAULT (clock_timestamp())::timestamp without time zone NOT NULL,
    event_type_pkid integer NOT NULL,
    series_pkid integer,
    observation_time_stamp timestamp without time zone,
    observation_numeric_value numeric(20,10),
    observation_previous_time_stamp timestamp without time zone,
    observation_previous_numeric_value numeric(20,10),
    event_message_pkid integer,
    rule_pkid integer,
    user_userid bigint,
    usergroup_usergroupid bigint,
    notification_pkid integer
);


ALTER TABLE sensorweb2.event_log OWNER TO sensorweb2;

--
-- Name: TABLE event_log; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON TABLE event_log IS 'logged events';


--
-- Name: COLUMN event_log.pkid; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN event_log.pkid IS 'serial primary key';


--
-- Name: COLUMN event_log.time_stamp_created; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN event_log.time_stamp_created IS 'point in time at which the event is logged';


--
-- Name: COLUMN event_log.event_type_pkid; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN event_log.event_type_pkid IS 'primary key of the event type the logged event belongs to';


--
-- Name: COLUMN event_log.series_pkid; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN event_log.series_pkid IS 'primary key of the series the logged event value belongs to';


--
-- Name: COLUMN event_log.observation_time_stamp; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN event_log.observation_time_stamp IS 'point in time at which the observation happens';


--
-- Name: COLUMN event_log.observation_numeric_value; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN event_log.observation_numeric_value IS 'observed value';


--
-- Name: COLUMN event_log.observation_previous_time_stamp; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN event_log.observation_previous_time_stamp IS 'point in time at which the previous observation happens';


--
-- Name: COLUMN event_log.observation_previous_numeric_value; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN event_log.observation_previous_numeric_value IS 'previous observed value';


--
-- Name: COLUMN event_log.event_message_pkid; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN event_log.event_message_pkid IS 'primary key of the message the logged event value belongs to';


--
-- Name: COLUMN event_log.rule_pkid; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN event_log.rule_pkid IS 'primary key of the rule the logged event belongs to';


--
-- Name: COLUMN event_log.user_userid; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN event_log.user_userid IS 'primary key of the user the logged event belongs to';


--
-- Name: COLUMN event_log.usergroup_usergroupid; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN event_log.usergroup_usergroupid IS 'primary key of the usergroup the logged event belongs to';


--
-- Name: COLUMN event_log.notification_pkid; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN event_log.notification_pkid IS 'primary key of the notification the logged event belongs to';


--
-- Name: event_log_mail; Type: TABLE; Schema: sensorweb2; Owner: sensorweb2
--

CREATE TABLE event_log_mail (
    event_log_pkid integer,
    mail_address character varying(200) NOT NULL,
    mail_sent timestamp without time zone,
    subscription_pkid integer,
    content character varying(4096),
    userid bigint,
    notification_level_id integer
);


ALTER TABLE sensorweb2.event_log_mail OWNER TO sensorweb2;

--
-- Name: TABLE event_log_mail; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON TABLE event_log_mail IS 'generated mails from logged events';


--
-- Name: COLUMN event_log_mail.event_log_pkid; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN event_log_mail.event_log_pkid IS 'primary key of the event log the mail belongs to';


--
-- Name: COLUMN event_log_mail.mail_address; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN event_log_mail.mail_address IS 'receiver of the mail';


--
-- Name: COLUMN event_log_mail.mail_sent; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN event_log_mail.mail_sent IS 'point in time at which the mail was sent';


--
-- Name: COLUMN event_log_mail.subscription_pkid; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN event_log_mail.subscription_pkid IS 'primary key of the subscription the mail belongs to';


--
-- Name: COLUMN event_log_mail.content; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN event_log_mail.content IS 'message text of a single event';


--
-- Name: COLUMN event_log_mail.userid; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN event_log_mail.userid IS 'internal id of the user (see table user_) corresponding to the mail_address';


--
-- Name: COLUMN event_log_mail.notification_level_id; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN event_log_mail.notification_level_id IS 'id of the notification level which belongs to the content';


--
-- Name: event_log_pkid_seq; Type: SEQUENCE; Schema: sensorweb2; Owner: sensorweb2
--

CREATE SEQUENCE event_log_pkid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE sensorweb2.event_log_pkid_seq OWNER TO sensorweb2;

--
-- Name: event_log_pkid_seq; Type: SEQUENCE OWNED BY; Schema: sensorweb2; Owner: sensorweb2
--

ALTER SEQUENCE event_log_pkid_seq OWNED BY event_log.pkid;


--
-- Name: event_message; Type: TABLE; Schema: sensorweb2; Owner: sensorweb2
--

CREATE TABLE event_message (
    pkid integer NOT NULL,
    name character varying(100) NOT NULL,
    message_text character varying(500) NOT NULL
);


ALTER TABLE sensorweb2.event_message OWNER TO sensorweb2;

--
-- Name: TABLE event_message; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON TABLE event_message IS 'represents event messages';


--
-- Name: COLUMN event_message.pkid; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN event_message.pkid IS 'serial primary key';


--
-- Name: COLUMN event_message.name; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN event_message.name IS 'unique name of the event message';


--
-- Name: COLUMN event_message.message_text; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN event_message.message_text IS 'text of the event message';


--
-- Name: event_message_i18n; Type: TABLE; Schema: sensorweb2; Owner: sensorweb2
--

CREATE TABLE event_message_i18n (
    pkid integer NOT NULL,
    event_message_pkid integer NOT NULL,
    locale character varying(10) NOT NULL,
    name character varying(100),
    message_text character varying(500)
);


ALTER TABLE sensorweb2.event_message_i18n OWNER TO sensorweb2;

--
-- Name: TABLE event_message_i18n; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON TABLE event_message_i18n IS 'internationalization of event messages';


--
-- Name: COLUMN event_message_i18n.pkid; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN event_message_i18n.pkid IS 'serial primary key';


--
-- Name: COLUMN event_message_i18n.event_message_pkid; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN event_message_i18n.event_message_pkid IS 'Id of the event message the record belongs to';


--
-- Name: COLUMN event_message_i18n.locale; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN event_message_i18n.locale IS 'language identifier and region identifier';


--
-- Name: COLUMN event_message_i18n.name; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN event_message_i18n.name IS 'name of the event message in locale''s language';


--
-- Name: COLUMN event_message_i18n.message_text; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN event_message_i18n.message_text IS 'text of the event message in locale''s language';


--
-- Name: event_message_i18n_pkid_seq; Type: SEQUENCE; Schema: sensorweb2; Owner: sensorweb2
--

CREATE SEQUENCE event_message_i18n_pkid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE sensorweb2.event_message_i18n_pkid_seq OWNER TO sensorweb2;

--
-- Name: event_message_i18n_pkid_seq; Type: SEQUENCE OWNED BY; Schema: sensorweb2; Owner: sensorweb2
--

ALTER SEQUENCE event_message_i18n_pkid_seq OWNED BY event_message_i18n.pkid;


--
-- Name: event_message_pkid_seq; Type: SEQUENCE; Schema: sensorweb2; Owner: sensorweb2
--

CREATE SEQUENCE event_message_pkid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE sensorweb2.event_message_pkid_seq OWNER TO sensorweb2;

--
-- Name: event_message_pkid_seq; Type: SEQUENCE OWNED BY; Schema: sensorweb2; Owner: sensorweb2
--

ALTER SEQUENCE event_message_pkid_seq OWNED BY event_message.pkid;


--
-- Name: event_type; Type: TABLE; Schema: sensorweb2; Owner: sensorweb2
--

CREATE TABLE event_type (
    pkid integer NOT NULL,
    name character varying(100) NOT NULL,
    description character varying(200) NOT NULL
);


ALTER TABLE sensorweb2.event_type OWNER TO sensorweb2;

--
-- Name: TABLE event_type; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON TABLE event_type IS 'represents event types';


--
-- Name: COLUMN event_type.pkid; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN event_type.pkid IS 'serial primary key';


--
-- Name: COLUMN event_type.name; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN event_type.name IS 'unique name of the event type';


--
-- Name: COLUMN event_type.description; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN event_type.description IS 'default description of the event type';


--
-- Name: event_type_i18n; Type: TABLE; Schema: sensorweb2; Owner: sensorweb2
--

CREATE TABLE event_type_i18n (
    pkid integer NOT NULL,
    event_type_pkid integer NOT NULL,
    locale character varying(10) NOT NULL,
    name character varying(100),
    description character varying(200)
);


ALTER TABLE sensorweb2.event_type_i18n OWNER TO sensorweb2;

--
-- Name: TABLE event_type_i18n; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON TABLE event_type_i18n IS 'internationalization of event types';


--
-- Name: COLUMN event_type_i18n.pkid; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN event_type_i18n.pkid IS 'serial primary key';


--
-- Name: COLUMN event_type_i18n.event_type_pkid; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN event_type_i18n.event_type_pkid IS 'Id of the event type the record belongs to';


--
-- Name: COLUMN event_type_i18n.locale; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN event_type_i18n.locale IS 'language identifier and region identifier';


--
-- Name: COLUMN event_type_i18n.name; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN event_type_i18n.name IS 'name of the event type in locale''s language';


--
-- Name: COLUMN event_type_i18n.description; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN event_type_i18n.description IS 'description of the event type in locale''s language';


--
-- Name: event_type_i18n_pkid_seq; Type: SEQUENCE; Schema: sensorweb2; Owner: sensorweb2
--

CREATE SEQUENCE event_type_i18n_pkid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE sensorweb2.event_type_i18n_pkid_seq OWNER TO sensorweb2;

--
-- Name: event_type_i18n_pkid_seq; Type: SEQUENCE OWNED BY; Schema: sensorweb2; Owner: sensorweb2
--

ALTER SEQUENCE event_type_i18n_pkid_seq OWNED BY event_type_i18n.pkid;


--
-- Name: event_type_pkid_seq; Type: SEQUENCE; Schema: sensorweb2; Owner: sensorweb2
--

CREATE SEQUENCE event_type_pkid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE sensorweb2.event_type_pkid_seq OWNER TO sensorweb2;

--
-- Name: event_type_pkid_seq; Type: SEQUENCE OWNED BY; Schema: sensorweb2; Owner: sensorweb2
--

ALTER SEQUENCE event_type_pkid_seq OWNED BY event_type.pkid;


--
-- Name: feature_of_interest; Type: TABLE; Schema: sensorweb2; Owner: sensorweb2
--

CREATE TABLE feature_of_interest (
    pkid integer NOT NULL,
    feature_of_interest_id character varying(100) NOT NULL,
    feature_of_interest_name character varying(100) NOT NULL,
    geom public.geometry,
    feature_class character varying(100),
    reference_wv_id integer NOT NULL,
    description character varying(100)
);


ALTER TABLE sensorweb2.feature_of_interest OWNER TO sensorweb2;

--
-- Name: TABLE feature_of_interest; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON TABLE feature_of_interest IS 'represents the feature of interest of an observation';


--
-- Name: COLUMN feature_of_interest.pkid; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN feature_of_interest.pkid IS 'serial primary key';


--
-- Name: COLUMN feature_of_interest.feature_of_interest_id; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN feature_of_interest.feature_of_interest_id IS 'identification of the feature of interest without special chars';


--
-- Name: COLUMN feature_of_interest.feature_of_interest_name; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN feature_of_interest.feature_of_interest_name IS 'default name of the feature of interest';


--
-- Name: COLUMN feature_of_interest.geom; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN feature_of_interest.geom IS 'geometry of the feature of interest';


--
-- Name: COLUMN feature_of_interest.feature_class; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN feature_of_interest.feature_class IS 'name of the feature class the feature of interest is coming from';


--
-- Name: COLUMN feature_of_interest.reference_wv_id; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN feature_of_interest.reference_wv_id IS 'internal unique reference number';


--
-- Name: COLUMN feature_of_interest.description; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN feature_of_interest.description IS 'default description of the feature of interest';


--
-- Name: feature_of_interest_i18n; Type: TABLE; Schema: sensorweb2; Owner: sensorweb2
--

CREATE TABLE feature_of_interest_i18n (
    pkid integer NOT NULL,
    feature_of_interest_pkid integer NOT NULL,
    locale character varying(10) NOT NULL,
    name character varying(100),
    description character varying(100)
);


ALTER TABLE sensorweb2.feature_of_interest_i18n OWNER TO sensorweb2;

--
-- Name: TABLE feature_of_interest_i18n; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON TABLE feature_of_interest_i18n IS 'internationalization of feature of interests';


--
-- Name: COLUMN feature_of_interest_i18n.pkid; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN feature_of_interest_i18n.pkid IS 'serial primary key';


--
-- Name: COLUMN feature_of_interest_i18n.feature_of_interest_pkid; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN feature_of_interest_i18n.feature_of_interest_pkid IS 'Id of the feature of interest the record belongs to';


--
-- Name: COLUMN feature_of_interest_i18n.locale; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN feature_of_interest_i18n.locale IS 'language identifier and region identifier';


--
-- Name: COLUMN feature_of_interest_i18n.name; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN feature_of_interest_i18n.name IS 'name of the feature of interest in locale''s language';


--
-- Name: COLUMN feature_of_interest_i18n.description; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN feature_of_interest_i18n.description IS 'description of the feature of interest in locale''s language';


--
-- Name: feature_of_interest_i18n_pkid_seq; Type: SEQUENCE; Schema: sensorweb2; Owner: sensorweb2
--

CREATE SEQUENCE feature_of_interest_i18n_pkid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE sensorweb2.feature_of_interest_i18n_pkid_seq OWNER TO sensorweb2;

--
-- Name: feature_of_interest_i18n_pkid_seq; Type: SEQUENCE OWNED BY; Schema: sensorweb2; Owner: sensorweb2
--

ALTER SEQUENCE feature_of_interest_i18n_pkid_seq OWNED BY feature_of_interest_i18n.pkid;


--
-- Name: feature_of_interest_pkid_seq; Type: SEQUENCE; Schema: sensorweb2; Owner: sensorweb2
--

CREATE SEQUENCE feature_of_interest_pkid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE sensorweb2.feature_of_interest_pkid_seq OWNER TO sensorweb2;

--
-- Name: feature_of_interest_pkid_seq; Type: SEQUENCE OWNED BY; Schema: sensorweb2; Owner: sensorweb2
--

ALTER SEQUENCE feature_of_interest_pkid_seq OWNED BY feature_of_interest.pkid;


--
-- Name: notification; Type: TABLE; Schema: sensorweb2; Owner: sensorweb2
--

CREATE TABLE notification (
    pkid integer NOT NULL,
    series_pkid integer NOT NULL
);


ALTER TABLE sensorweb2.notification OWNER TO sensorweb2;

--
-- Name: TABLE notification; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON TABLE notification IS 'main notification definition';


--
-- Name: COLUMN notification.pkid; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN notification.pkid IS 'serial primary key';


--
-- Name: COLUMN notification.series_pkid; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN notification.series_pkid IS 'primary key of the series the notification belongs to';


--
-- Name: notification_rule; Type: TABLE; Schema: sensorweb2; Owner: sensorweb2
--

CREATE TABLE notification_rule (
    notification_pkid integer NOT NULL,
    rule_pkid integer NOT NULL,
    notification_level_id integer NOT NULL,
    primary_rule_flag numeric(1,0) DEFAULT 0 NOT NULL
);


ALTER TABLE sensorweb2.notification_rule OWNER TO sensorweb2;

--
-- Name: TABLE notification_rule; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON TABLE notification_rule IS 'rules belonging to notifications';


--
-- Name: COLUMN notification_rule.notification_pkid; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN notification_rule.notification_pkid IS 'primary key of the notification the notification rule belongs to';


--
-- Name: COLUMN notification_rule.rule_pkid; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN notification_rule.rule_pkid IS 'primary key of the rule the notification rule belongs to';


--
-- Name: COLUMN notification_rule.notification_level_id; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN notification_rule.notification_level_id IS 'primary key of the notification level the notification rule belongs to';


--
-- Name: COLUMN notification_rule.primary_rule_flag; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN notification_rule.primary_rule_flag IS 'marks the primary rule of a notification';


--
-- Name: notification_events; Type: VIEW; Schema: sensorweb2; Owner: postgres
--

CREATE VIEW notification_events AS
    SELECT notification_rule.notification_pkid, event_log.series_pkid, event_log.pkid AS event_log_pkid, event_log.event_type_pkid FROM (notification_rule JOIN event_log ON ((event_log.rule_pkid = notification_rule.rule_pkid))) UNION SELECT notification.pkid AS notification_pkid, event_log.series_pkid, event_log.pkid AS event_log_pkid, event_log.event_type_pkid FROM (event_log JOIN notification ON ((notification.series_pkid = event_log.series_pkid))) WHERE (event_log.event_type_pkid = ANY (ARRAY[2, 3])) ORDER BY 1, 3;


ALTER TABLE sensorweb2.notification_events OWNER TO postgres;

--
-- Name: VIEW notification_events; Type: COMMENT; Schema: sensorweb2; Owner: postgres
--

COMMENT ON VIEW notification_events IS 'list of notifications and related events';


--
-- Name: notification_level; Type: TABLE; Schema: sensorweb2; Owner: sensorweb2
--

CREATE TABLE notification_level (
    level_id integer NOT NULL,
    description character varying(100) NOT NULL
);


ALTER TABLE sensorweb2.notification_level OWNER TO sensorweb2;

--
-- Name: TABLE notification_level; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON TABLE notification_level IS 'levels of notifications';


--
-- Name: COLUMN notification_level.level_id; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN notification_level.level_id IS 'primary key of the notification level';


--
-- Name: COLUMN notification_level.description; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN notification_level.description IS 'default description of the notification level';


--
-- Name: notification_level_i18n; Type: TABLE; Schema: sensorweb2; Owner: sensorweb2
--

CREATE TABLE notification_level_i18n (
    pkid integer NOT NULL,
    notification_level_id integer NOT NULL,
    locale character varying(10) NOT NULL,
    description character varying(100)
);


ALTER TABLE sensorweb2.notification_level_i18n OWNER TO sensorweb2;

--
-- Name: TABLE notification_level_i18n; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON TABLE notification_level_i18n IS 'internationalization of notification levels';


--
-- Name: COLUMN notification_level_i18n.pkid; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN notification_level_i18n.pkid IS 'serial primary key';


--
-- Name: COLUMN notification_level_i18n.notification_level_id; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN notification_level_i18n.notification_level_id IS 'Id of the notification level the record belongs to';


--
-- Name: COLUMN notification_level_i18n.locale; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN notification_level_i18n.locale IS 'language identifier and region identifier';


--
-- Name: COLUMN notification_level_i18n.description; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN notification_level_i18n.description IS 'description of the notification level in locale''s language';


--
-- Name: notification_level_i18n_pkid_seq; Type: SEQUENCE; Schema: sensorweb2; Owner: sensorweb2
--

CREATE SEQUENCE notification_level_i18n_pkid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE sensorweb2.notification_level_i18n_pkid_seq OWNER TO sensorweb2;

--
-- Name: notification_level_i18n_pkid_seq; Type: SEQUENCE OWNED BY; Schema: sensorweb2; Owner: sensorweb2
--

ALTER SEQUENCE notification_level_i18n_pkid_seq OWNED BY notification_level_i18n.pkid;


--
-- Name: rule; Type: TABLE; Schema: sensorweb2; Owner: sensorweb2
--

CREATE TABLE rule (
    pkid integer NOT NULL,
    series_pkid integer NOT NULL,
    threshold numeric(20,10) NOT NULL,
    trend_code numeric(2,0) NOT NULL
);


ALTER TABLE sensorweb2.rule OWNER TO sensorweb2;

--
-- Name: TABLE rule; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON TABLE rule IS 'events to be checked';


--
-- Name: COLUMN rule.pkid; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN rule.pkid IS 'serial primary key';


--
-- Name: COLUMN rule.series_pkid; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN rule.series_pkid IS 'primary key of the series the rule belongs to';


--
-- Name: COLUMN rule.threshold; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN rule.threshold IS 'value at which the rule is applied to';


--
-- Name: COLUMN rule.trend_code; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN rule.trend_code IS 'primary key of the trend code the rule belongs to';


--
-- Name: subscription; Type: TABLE; Schema: sensorweb2; Owner: sensorweb2
--

CREATE TABLE subscription (
    pkid integer NOT NULL,
    userid bigint,
    usergroupid bigint,
    notification_pkid integer,
    CONSTRAINT subscription_check_1 CHECK ((((userid IS NULL) AND (usergroupid IS NOT NULL)) OR ((userid IS NOT NULL) AND (usergroupid IS NULL))))
);


ALTER TABLE sensorweb2.subscription OWNER TO sensorweb2;

--
-- Name: TABLE subscription; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON TABLE subscription IS 'subscriptions of events for a user or user group';


--
-- Name: COLUMN subscription.pkid; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN subscription.pkid IS 'serial primary key';


--
-- Name: COLUMN subscription.userid; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN subscription.userid IS 'primary key of the user the subscription belongs to';


--
-- Name: COLUMN subscription.usergroupid; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN subscription.usergroupid IS 'primary key of the user group the subscription belongs to';


--
-- Name: COLUMN subscription.notification_pkid; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN subscription.notification_pkid IS 'primary key of the notification the subscription belongs to';


--
-- Name: trend; Type: TABLE; Schema: sensorweb2; Owner: sensorweb2
--

CREATE TABLE trend (
    code numeric(2,0) NOT NULL,
    description character varying(100) NOT NULL
);


ALTER TABLE sensorweb2.trend OWNER TO sensorweb2;

--
-- Name: TABLE trend; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON TABLE trend IS 'trends of adjacent observations';


--
-- Name: COLUMN trend.code; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN trend.code IS 'identifier: coded relation of previous and actual value (1=under, 2=equal, 3=above, 99=missing)';


--
-- Name: COLUMN trend.description; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN trend.description IS 'default description of the relation of adjacent (previous and actual) values';


--
-- Name: user_; Type: TABLE; Schema: sensorweb2; Owner: sensorweb2
--

CREATE TABLE user_ (
    userid bigint NOT NULL,
    password_ character varying(75),
    screenname character varying(75),
    emailaddress character varying(75),
    languageid character varying(75),
    firstname character varying(75),
    middlename character varying(75),
    lastname character varying(75)
);


ALTER TABLE sensorweb2.user_ OWNER TO sensorweb2;

--
-- Name: TABLE user_; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON TABLE user_ IS 'defined users, synchronized from database "fluggs"';


--
-- Name: usergroup; Type: TABLE; Schema: sensorweb2; Owner: sensorweb2
--

CREATE TABLE usergroup (
    usergroupid bigint NOT NULL,
    companyid bigint,
    parentusergroupid bigint,
    name character varying(75),
    description text,
    addedbyldapimport boolean
);


ALTER TABLE sensorweb2.usergroup OWNER TO sensorweb2;

--
-- Name: TABLE usergroup; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON TABLE usergroup IS 'defined usergroups, synchronized from database "fluggs"';


--
-- Name: users_usergroups; Type: TABLE; Schema: sensorweb2; Owner: sensorweb2
--

CREATE TABLE users_usergroups (
    userid bigint NOT NULL,
    usergroupid bigint NOT NULL
);


ALTER TABLE sensorweb2.users_usergroups OWNER TO sensorweb2;

--
-- Name: TABLE users_usergroups; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON TABLE users_usergroups IS 'defined links between users and usergroups, synchronized from database "fluggs"';


--
-- Name: notification_list; Type: VIEW; Schema: sensorweb2; Owner: postgres
--

CREATE VIEW notification_list AS
    SELECT n.pkid, get_series_description(n.series_pkid) AS series_description, t.description AS trend_description, (rtrim(rtrim((r.threshold)::text, '0'::text), '.'::text))::numeric AS threshold, (SELECT array_to_string(array_agg(u.emailaddress), ', '::text) AS array_to_string FROM (subscription s JOIN user_ u ON ((u.userid = s.userid))) WHERE (s.notification_pkid = n.pkid)) AS mail_users, (SELECT array_to_string(array_agg(ug.name), ', '::text) AS array_to_string FROM (subscription s JOIN usergroup ug ON ((ug.usergroupid = s.usergroupid))) WHERE (s.notification_pkid = n.pkid)) AS mail_groups, (SELECT array_to_string(array_agg(DISTINCT u.emailaddress), ', '::text) AS array_to_string FROM (((subscription s JOIN usergroup ug ON ((ug.usergroupid = s.usergroupid))) JOIN users_usergroups uug ON ((uug.usergroupid = ug.usergroupid))) JOIN user_ u ON ((u.userid = uug.userid))) WHERE (s.notification_pkid = n.pkid)) AS mail_users_in_groups FROM (((notification n LEFT JOIN notification_rule nr ON ((nr.notification_pkid = n.pkid))) LEFT JOIN rule r ON ((r.pkid = nr.rule_pkid))) LEFT JOIN trend t ON ((t.code = r.trend_code))) WHERE (nr.primary_rule_flag = (1)::numeric) ORDER BY get_series_description(n.series_pkid), (rtrim(rtrim((r.threshold)::text, '0'::text), '.'::text))::numeric;


ALTER TABLE sensorweb2.notification_list OWNER TO postgres;

--
-- Name: VIEW notification_list; Type: COMMENT; Schema: sensorweb2; Owner: postgres
--

COMMENT ON VIEW notification_list IS 'list of notification details: series, threshold, mail users, mail groups';


--
-- Name: notification_pkid_seq; Type: SEQUENCE; Schema: sensorweb2; Owner: sensorweb2
--

CREATE SEQUENCE notification_pkid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE sensorweb2.notification_pkid_seq OWNER TO sensorweb2;

--
-- Name: notification_pkid_seq; Type: SEQUENCE OWNED BY; Schema: sensorweb2; Owner: sensorweb2
--

ALTER SEQUENCE notification_pkid_seq OWNED BY notification.pkid;


--
-- Name: notification_primary; Type: VIEW; Schema: sensorweb2; Owner: postgres
--

CREATE VIEW notification_primary AS
    SELECT n.pkid, get_series_description(n.series_pkid) AS get_series_description, t.description, (rtrim(rtrim((r.threshold)::text, '0'::text), '.'::text))::numeric AS threshold FROM (((notification n JOIN notification_rule nr ON ((nr.notification_pkid = n.pkid))) JOIN rule r ON ((r.pkid = nr.rule_pkid))) JOIN trend t ON ((t.code = r.trend_code))) WHERE (nr.primary_rule_flag = (1)::numeric) ORDER BY get_series_description(n.series_pkid), (rtrim(rtrim((r.threshold)::text, '0'::text), '.'::text))::numeric;


ALTER TABLE sensorweb2.notification_primary OWNER TO postgres;

--
-- Name: VIEW notification_primary; Type: COMMENT; Schema: sensorweb2; Owner: postgres
--

COMMENT ON VIEW notification_primary IS 'list of primary notifications';


--
-- Name: phenomenon; Type: TABLE; Schema: sensorweb2; Owner: sensorweb2
--

CREATE TABLE phenomenon (
    pkid integer NOT NULL,
    phenomenon_id character varying(100) NOT NULL,
    phenomenon_name character varying(100),
    description character varying(100)
);


ALTER TABLE sensorweb2.phenomenon OWNER TO sensorweb2;

--
-- Name: TABLE phenomenon; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON TABLE phenomenon IS 'represents phenomena';


--
-- Name: COLUMN phenomenon.pkid; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN phenomenon.pkid IS 'serial primary key';


--
-- Name: COLUMN phenomenon.description; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN phenomenon.description IS 'default description of the phenomenon';


--
-- Name: procedure; Type: TABLE; Schema: sensorweb2; Owner: sensorweb2
--

CREATE TABLE procedure (
    pkid integer NOT NULL,
    procedure_id character varying(100) NOT NULL,
    procedure_name character varying(100),
    description_type character varying(100),
    reference_flag smallint,
    description character varying(100)
);


ALTER TABLE sensorweb2.procedure OWNER TO sensorweb2;

--
-- Name: TABLE procedure; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON TABLE procedure IS 'represents the procedure which produces the observation values';


--
-- Name: COLUMN procedure.pkid; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN procedure.pkid IS 'serial primary key';


--
-- Name: COLUMN procedure.procedure_id; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN procedure.procedure_id IS 'identification of the procedure without special chars';


--
-- Name: COLUMN procedure.procedure_name; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN procedure.procedure_name IS 'default name of the procedure';


--
-- Name: COLUMN procedure.description_type; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN procedure.description_type IS 'content type of the description';


--
-- Name: COLUMN procedure.reference_flag; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN procedure.reference_flag IS 'binary flag indicating if the procedure is a reference procedure';


--
-- Name: COLUMN procedure.description; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN procedure.description IS 'default description of the procedure';


--
-- Name: series; Type: TABLE; Schema: sensorweb2; Owner: sensorweb2
--

CREATE TABLE series (
    pkid integer NOT NULL,
    category_pkid integer NOT NULL,
    phenomenon_pkid integer NOT NULL,
    procedure_pkid integer NOT NULL,
    feature_of_interest_pkid integer NOT NULL,
    decimals smallint,
    unit_pkid integer NOT NULL,
    first_time_stamp timestamp without time zone,
    first_numeric_value numeric(20,10),
    last_time_stamp timestamp without time zone,
    last_numeric_value numeric(20,10),
    data_origin character varying(100),
    pegel_online numeric(1,0),
    published_flag smallint DEFAULT 0,
    time_zone character varying(10),
    data_origin_comment character varying(100),
    retention_time interval,
    eventing_flag smallint DEFAULT 0
);


ALTER TABLE sensorweb2.series OWNER TO sensorweb2;

--
-- Name: TABLE series; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON TABLE series IS 'stores valid combinations of category, phenomenon, procedure and feature_of_interest';


--
-- Name: COLUMN series.pkid; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN series.pkid IS 'serial primary key';


--
-- Name: COLUMN series.category_pkid; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN series.category_pkid IS 'reference to the related category';


--
-- Name: COLUMN series.phenomenon_pkid; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN series.phenomenon_pkid IS 'reference to the related phenomenon';


--
-- Name: COLUMN series.procedure_pkid; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN series.procedure_pkid IS 'reference to the related procedure';


--
-- Name: COLUMN series.feature_of_interest_pkid; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN series.feature_of_interest_pkid IS 'reference to the related feature of interest';


--
-- Name: COLUMN series.decimals; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN series.decimals IS 'number of digits after the decimal point';


--
-- Name: COLUMN series.unit_pkid; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN series.unit_pkid IS 'reference to the related unit';


--
-- Name: COLUMN series.first_time_stamp; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN series.first_time_stamp IS 'point in time at which the first observation happens';


--
-- Name: COLUMN series.first_numeric_value; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN series.first_numeric_value IS 'first observed value';


--
-- Name: COLUMN series.last_time_stamp; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN series.last_time_stamp IS 'point in time at which the latest observation happens';


--
-- Name: COLUMN series.last_numeric_value; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN series.last_numeric_value IS 'latest observed value';


--
-- Name: COLUMN series.data_origin; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN series.data_origin IS 'first system storing the observed value';


--
-- Name: COLUMN series.pegel_online; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN series.pegel_online IS 'Wird diese Zeitreihe in "Pegel Online" verwendet?';


--
-- Name: COLUMN series.published_flag; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN series.published_flag IS 'Steuert, ob die Zeitreihe veroeffentlicht wird';


--
-- Name: COLUMN series.time_zone; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN series.time_zone IS 'Zeitzone, z.B. "Ortszeit" oder "MESZ"';


--
-- Name: COLUMN series.data_origin_comment; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN series.data_origin_comment IS 'Kommentar zu den Originaldaten';


--
-- Name: COLUMN series.retention_time; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN series.retention_time IS 'Wie lange sollen die zugehoerigen Messwerte gespeichert bleiben?';


--
-- Name: COLUMN series.eventing_flag; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN series.eventing_flag IS 'controls if the series will be considered during eventing actions';


--
-- Name: unit; Type: TABLE; Schema: sensorweb2; Owner: sensorweb2
--

CREATE TABLE unit (
    pkid integer NOT NULL,
    unit character varying(30),
    description character varying(100)
);


ALTER TABLE sensorweb2.unit OWNER TO sensorweb2;

--
-- Name: TABLE unit; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON TABLE unit IS 'represents units';


--
-- Name: COLUMN unit.pkid; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN unit.pkid IS 'serial primary key';


--
-- Name: COLUMN unit.unit; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN unit.unit IS 'default unit';


--
-- Name: COLUMN unit.description; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN unit.description IS 'default description of the unit';


--
-- Name: notifications_and_rules; Type: VIEW; Schema: sensorweb2; Owner: postgres
--

CREATE VIEW notifications_and_rules AS
    SELECT DISTINCT series.pkid AS series_pkid, (rule.threshold)::real AS rule_threshold, notification.pkid AS notification_pkid, NULL::numeric AS rule_trend_code, NULL::integer AS rule_pkid, NULL::text AS "primary", (((((((('-------- '::text || (phenomenon.phenomenon_name)::text) || ' '::text) || (feature_of_interest.feature_of_interest_name)::text) || ', '::text) || (procedure.procedure_name)::text) || ' ---> '::text) || replace(((rule.threshold)::real)::text, '.'::text, ','::text)) || COALESCE((' '::text || (unit.unit)::text), ''::text)) AS description FROM ((((((((notification LEFT JOIN series ON ((series.pkid = notification.series_pkid))) LEFT JOIN phenomenon ON ((phenomenon.pkid = series.phenomenon_pkid))) LEFT JOIN feature_of_interest ON ((feature_of_interest.pkid = series.feature_of_interest_pkid))) LEFT JOIN procedure ON ((procedure.pkid = series.procedure_pkid))) LEFT JOIN unit ON ((unit.pkid = series.unit_pkid))) LEFT JOIN notification_rule ON ((notification_rule.notification_pkid = notification.pkid))) LEFT JOIN notification_level ON ((notification_level.level_id = notification_rule.notification_level_id))) LEFT JOIN rule ON ((rule.pkid = notification_rule.rule_pkid))) UNION SELECT notification.series_pkid, (rule.threshold)::real AS rule_threshold, notification.pkid AS notification_pkid, rule.trend_code AS rule_trend_code, rule.pkid AS rule_pkid, CASE notification_rule.primary_rule_flag WHEN 1 THEN '  --->'::text ELSE NULL::text END AS "primary", (COALESCE(((notification_level.description)::text || ': '::text), ''::text) || (trend.description)::text) AS description FROM ((((notification LEFT JOIN notification_rule ON ((notification_rule.notification_pkid = notification.pkid))) LEFT JOIN notification_level ON ((notification_level.level_id = notification_rule.notification_level_id))) LEFT JOIN rule ON ((rule.pkid = notification_rule.rule_pkid))) LEFT JOIN trend ON ((trend.code = rule.trend_code))) ORDER BY 1, 2, 3, 4 DESC;


ALTER TABLE sensorweb2.notifications_and_rules OWNER TO postgres;

--
-- Name: VIEW notifications_and_rules; Type: COMMENT; Schema: sensorweb2; Owner: postgres
--

COMMENT ON VIEW notifications_and_rules IS 'list of notifications and their related rules';


--
-- Name: observation; Type: TABLE; Schema: sensorweb2; Owner: sensorweb2
--

CREATE TABLE observation (
    observation_id integer NOT NULL,
    time_stamp timestamp without time zone NOT NULL,
    series_pkid integer NOT NULL,
    numeric_value numeric(20,10),
    result_time timestamp without time zone NOT NULL,
    time_stamp_insert timestamp without time zone DEFAULT now(),
    comment character varying(100)
);


ALTER TABLE sensorweb2.observation OWNER TO sensorweb2;

--
-- Name: TABLE observation; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON TABLE observation IS 'represents observations';


--
-- Name: COLUMN observation.observation_id; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN observation.observation_id IS 'serial primary key';


--
-- Name: COLUMN observation.time_stamp; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN observation.time_stamp IS 'point in time at which the observation happens';


--
-- Name: COLUMN observation.series_pkid; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN observation.series_pkid IS 'primary key of the series the observed value belongs to';


--
-- Name: COLUMN observation.numeric_value; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN observation.numeric_value IS 'observed value';


--
-- Name: COLUMN observation.result_time; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN observation.result_time IS 'point in time at which the observation became available';


--
-- Name: COLUMN observation.time_stamp_insert; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN observation.time_stamp_insert IS 'point in time at which the observation is stored in the table';


--
-- Name: COLUMN observation.comment; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN observation.comment IS 'additional information about the observed value';


--
-- Name: observation_float_view; Type: VIEW; Schema: sensorweb2; Owner: postgres
--

CREATE VIEW observation_float_view AS
    SELECT observation.observation_id, observation.time_stamp, observation.series_pkid, (observation.numeric_value)::double precision AS numeric_value, observation.result_time, observation.time_stamp_insert FROM observation;


ALTER TABLE sensorweb2.observation_float_view OWNER TO postgres;

--
-- Name: VIEW observation_float_view; Type: COMMENT; Schema: sensorweb2; Owner: postgres
--

COMMENT ON VIEW observation_float_view IS 'lists all observation values in float format';


--
-- Name: observation_observation_id_seq; Type: SEQUENCE; Schema: sensorweb2; Owner: sensorweb2
--

CREATE SEQUENCE observation_observation_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE sensorweb2.observation_observation_id_seq OWNER TO sensorweb2;

--
-- Name: observation_observation_id_seq; Type: SEQUENCE OWNED BY; Schema: sensorweb2; Owner: sensorweb2
--

ALTER SEQUENCE observation_observation_id_seq OWNED BY observation.observation_id;


--
-- Name: phenomenon_i18n; Type: TABLE; Schema: sensorweb2; Owner: sensorweb2
--

CREATE TABLE phenomenon_i18n (
    pkid integer NOT NULL,
    phenomenon_pkid integer NOT NULL,
    locale character varying(10) NOT NULL,
    name character varying(100),
    description character varying(100)
);


ALTER TABLE sensorweb2.phenomenon_i18n OWNER TO sensorweb2;

--
-- Name: TABLE phenomenon_i18n; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON TABLE phenomenon_i18n IS 'internationalization of phenomenons';


--
-- Name: COLUMN phenomenon_i18n.pkid; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN phenomenon_i18n.pkid IS 'serial primary key';


--
-- Name: COLUMN phenomenon_i18n.phenomenon_pkid; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN phenomenon_i18n.phenomenon_pkid IS 'Id of the phenomenon the record belongs to';


--
-- Name: COLUMN phenomenon_i18n.locale; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN phenomenon_i18n.locale IS 'language identifier and region identifier';


--
-- Name: COLUMN phenomenon_i18n.name; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN phenomenon_i18n.name IS 'name of the phenomenon in locale''s language';


--
-- Name: COLUMN phenomenon_i18n.description; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN phenomenon_i18n.description IS 'description of the phenomenon in locale''s language';


--
-- Name: phenomenon_i18n_pkid_seq; Type: SEQUENCE; Schema: sensorweb2; Owner: sensorweb2
--

CREATE SEQUENCE phenomenon_i18n_pkid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE sensorweb2.phenomenon_i18n_pkid_seq OWNER TO sensorweb2;

--
-- Name: phenomenon_i18n_pkid_seq; Type: SEQUENCE OWNED BY; Schema: sensorweb2; Owner: sensorweb2
--

ALTER SEQUENCE phenomenon_i18n_pkid_seq OWNED BY phenomenon_i18n.pkid;


--
-- Name: phenomenon_pkid_seq; Type: SEQUENCE; Schema: sensorweb2; Owner: sensorweb2
--

CREATE SEQUENCE phenomenon_pkid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE sensorweb2.phenomenon_pkid_seq OWNER TO sensorweb2;

--
-- Name: phenomenon_pkid_seq; Type: SEQUENCE OWNED BY; Schema: sensorweb2; Owner: sensorweb2
--

ALTER SEQUENCE phenomenon_pkid_seq OWNED BY phenomenon.pkid;


--
-- Name: procedure_i18n; Type: TABLE; Schema: sensorweb2; Owner: sensorweb2
--

CREATE TABLE procedure_i18n (
    pkid integer NOT NULL,
    procedure_pkid integer NOT NULL,
    locale character varying(10) NOT NULL,
    name character varying(100),
    description character varying(100)
);


ALTER TABLE sensorweb2.procedure_i18n OWNER TO sensorweb2;

--
-- Name: TABLE procedure_i18n; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON TABLE procedure_i18n IS 'internationalization of procedures';


--
-- Name: COLUMN procedure_i18n.pkid; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN procedure_i18n.pkid IS 'serial primary key';


--
-- Name: COLUMN procedure_i18n.procedure_pkid; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN procedure_i18n.procedure_pkid IS 'Id of the procedure the record belongs to';


--
-- Name: COLUMN procedure_i18n.locale; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN procedure_i18n.locale IS 'language identifier and region identifier';


--
-- Name: COLUMN procedure_i18n.name; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN procedure_i18n.name IS 'name of the procedure in locale''s language';


--
-- Name: COLUMN procedure_i18n.description; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN procedure_i18n.description IS 'description of the procedure in locale''s language';


--
-- Name: procedure_i18n_pkid_seq; Type: SEQUENCE; Schema: sensorweb2; Owner: sensorweb2
--

CREATE SEQUENCE procedure_i18n_pkid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE sensorweb2.procedure_i18n_pkid_seq OWNER TO sensorweb2;

--
-- Name: procedure_i18n_pkid_seq; Type: SEQUENCE OWNED BY; Schema: sensorweb2; Owner: sensorweb2
--

ALTER SEQUENCE procedure_i18n_pkid_seq OWNED BY procedure_i18n.pkid;


--
-- Name: procedure_pkid_seq; Type: SEQUENCE; Schema: sensorweb2; Owner: sensorweb2
--

CREATE SEQUENCE procedure_pkid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE sensorweb2.procedure_pkid_seq OWNER TO sensorweb2;

--
-- Name: procedure_pkid_seq; Type: SEQUENCE OWNED BY; Schema: sensorweb2; Owner: sensorweb2
--

ALTER SEQUENCE procedure_pkid_seq OWNED BY procedure.pkid;


--
-- Name: rule_pkid_seq; Type: SEQUENCE; Schema: sensorweb2; Owner: sensorweb2
--

CREATE SEQUENCE rule_pkid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE sensorweb2.rule_pkid_seq OWNER TO sensorweb2;

--
-- Name: rule_pkid_seq; Type: SEQUENCE OWNED BY; Schema: sensorweb2; Owner: sensorweb2
--

ALTER SEQUENCE rule_pkid_seq OWNED BY rule.pkid;


--
-- Name: series_check_age; Type: TABLE; Schema: sensorweb2; Owner: sensorweb2
--

CREATE TABLE series_check_age (
    series_pkid integer NOT NULL,
    check_interval interval NOT NULL,
    message_generated timestamp without time zone,
    unit_pkid integer,
    factor_seconds integer,
    conversion_precision integer
);


ALTER TABLE sensorweb2.series_check_age OWNER TO sensorweb2;

--
-- Name: TABLE series_check_age; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON TABLE series_check_age IS 'defines series for generating a message if the last value is too old';


--
-- Name: COLUMN series_check_age.series_pkid; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN series_check_age.series_pkid IS 'primary key of the series to be checked';


--
-- Name: COLUMN series_check_age.check_interval; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN series_check_age.check_interval IS 'age of the last stored value (of a series) after which a message will be generated';


--
-- Name: COLUMN series_check_age.message_generated; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN series_check_age.message_generated IS 'point in time the last message (of a series) was generated';


--
-- Name: COLUMN series_check_age.unit_pkid; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN series_check_age.unit_pkid IS 'reference to the related unit';


--
-- Name: COLUMN series_check_age.factor_seconds; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN series_check_age.factor_seconds IS 'conversion factor between seconds and the related unit';


--
-- Name: COLUMN series_check_age.conversion_precision; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN series_check_age.conversion_precision IS 'decimal places of the converted interval';


--
-- Name: series_cluster; Type: TABLE; Schema: sensorweb2; Owner: sensorweb2
--

CREATE TABLE series_cluster (
    series_pkid integer NOT NULL,
    cluster_pkid integer NOT NULL
);


ALTER TABLE sensorweb2.series_cluster OWNER TO sensorweb2;

--
-- Name: TABLE series_cluster; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON TABLE series_cluster IS 'clusters of series';


--
-- Name: COLUMN series_cluster.series_pkid; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN series_cluster.series_pkid IS 'primary key of a series belonging to a cluster';


--
-- Name: COLUMN series_cluster.cluster_pkid; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN series_cluster.cluster_pkid IS 'primary key of a cluster a series belongs to';


--
-- Name: series_cluster_usage; Type: VIEW; Schema: sensorweb2; Owner: postgres
--

CREATE VIEW series_cluster_usage AS
    SELECT s.pkid AS series_pkid, get_series_description(s.pkid) AS series_description, (SELECT 'X'::text AS text FROM series_cluster sc WHERE ((sc.series_pkid = s.pkid) AND (sc.cluster_pkid = 1))) AS "1_tamis", (SELECT 'X'::text AS text FROM series_cluster sc WHERE ((sc.series_pkid = s.pkid) AND (sc.cluster_pkid = 2))) AS "2_intern", (SELECT 'X'::text AS text FROM series_cluster sc WHERE ((sc.series_pkid = s.pkid) AND (sc.cluster_pkid = 3))) AS "3_extern", (SELECT 'X'::text AS text FROM series_cluster sc WHERE ((sc.series_pkid = s.pkid) AND (sc.cluster_pkid = 4))) AS "4_mudak" FROM series s ORDER BY s.pkid;


ALTER TABLE sensorweb2.series_cluster_usage OWNER TO postgres;

--
-- Name: VIEW series_cluster_usage; Type: COMMENT; Schema: sensorweb2; Owner: postgres
--

COMMENT ON VIEW series_cluster_usage IS 'list of all series showing in which cluster they are defined';


--
-- Name: series_derived; Type: TABLE; Schema: sensorweb2; Owner: sensorweb2
--

CREATE TABLE series_derived (
    series_pkid_base integer NOT NULL,
    series_pkid_derived integer NOT NULL,
    formula character varying(200)
);


ALTER TABLE sensorweb2.series_derived OWNER TO sensorweb2;

--
-- Name: TABLE series_derived; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON TABLE series_derived IS 'derived series with computed values';


--
-- Name: COLUMN series_derived.series_pkid_base; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN series_derived.series_pkid_base IS 'primary key of the base series with the original values';


--
-- Name: COLUMN series_derived.series_pkid_derived; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN series_derived.series_pkid_derived IS 'primary key of the derived series with the computed values';


--
-- Name: COLUMN series_derived.formula; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN series_derived.formula IS 'calculation formula to get the computed value';


--
-- Name: series_list_check_age; Type: VIEW; Schema: sensorweb2; Owner: postgres
--

CREATE VIEW series_list_check_age AS
    SELECT a.series_pkid, get_series_description(a.series_pkid) AS series_description, s.category_pkid, c.category_name, a.check_interval, public.interval_text(a.check_interval) AS check_interval_de FROM ((series_check_age a LEFT JOIN series s ON ((s.pkid = a.series_pkid))) LEFT JOIN category c ON ((c.pkid = s.category_pkid)));


ALTER TABLE sensorweb2.series_list_check_age OWNER TO postgres;

--
-- Name: VIEW series_list_check_age; Type: COMMENT; Schema: sensorweb2; Owner: postgres
--

COMMENT ON VIEW series_list_check_age IS 'list of all series with check intervals';


--
-- Name: series_pkid_seq; Type: SEQUENCE; Schema: sensorweb2; Owner: sensorweb2
--

CREATE SEQUENCE series_pkid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE sensorweb2.series_pkid_seq OWNER TO sensorweb2;

--
-- Name: series_pkid_seq; Type: SEQUENCE OWNED BY; Schema: sensorweb2; Owner: sensorweb2
--

ALTER SEQUENCE series_pkid_seq OWNED BY series.pkid;


--
-- Name: series_pkids; Type: VIEW; Schema: sensorweb2; Owner: postgres
--

CREATE VIEW series_pkids AS
    SELECT s.pkid, ca.pkid AS cat_pkid, ca.category_id AS cat_id, ph.pkid AS phen_pkid, ph.phenomenon_id AS phen_id, pr.pkid AS proc_pkid, pr.procedure_id AS proc_id, foi.pkid AS foi_pkid, foi.feature_of_interest_id AS foi_id FROM ((((series s JOIN category ca ON ((ca.pkid = s.category_pkid))) JOIN phenomenon ph ON ((ph.pkid = s.phenomenon_pkid))) JOIN procedure pr ON ((pr.pkid = s.procedure_pkid))) JOIN feature_of_interest foi ON ((foi.pkid = s.feature_of_interest_pkid))) ORDER BY ca.pkid, ph.pkid, pr.pkid, foi.pkid;


ALTER TABLE sensorweb2.series_pkids OWNER TO postgres;

--
-- Name: VIEW series_pkids; Type: COMMENT; Schema: sensorweb2; Owner: postgres
--

COMMENT ON VIEW series_pkids IS 'shows all (pk-) ids used in series';


--
-- Name: series_pkids_full; Type: VIEW; Schema: sensorweb2; Owner: postgres
--

CREATE VIEW series_pkids_full AS
    SELECT s.pkid, ca.pkid AS cat_pkid, ca.category_id AS cat_id, ph.pkid AS phen_pkid, ph.phenomenon_id AS phen_id, pr.pkid AS proc_pkid, pr.procedure_id AS proc_id, foi.pkid AS foi_pkid, foi.feature_of_interest_id AS foi_id, u.pkid AS unit_pkid, u.unit, s.decimals, s.first_time_stamp, s.first_numeric_value, s.last_time_stamp, s.last_numeric_value, s.data_origin FROM (((((series s JOIN category ca ON ((ca.pkid = s.category_pkid))) JOIN phenomenon ph ON ((ph.pkid = s.phenomenon_pkid))) JOIN procedure pr ON ((pr.pkid = s.procedure_pkid))) JOIN feature_of_interest foi ON ((foi.pkid = s.feature_of_interest_pkid))) JOIN unit u ON ((u.pkid = s.unit_pkid))) ORDER BY ca.pkid, ph.pkid, pr.pkid, foi.pkid;


ALTER TABLE sensorweb2.series_pkids_full OWNER TO postgres;

--
-- Name: VIEW series_pkids_full; Type: COMMENT; Schema: sensorweb2; Owner: postgres
--

COMMENT ON VIEW series_pkids_full IS 'shows all series (pk-) ids and their attributes';


--
-- Name: series_reference; Type: TABLE; Schema: sensorweb2; Owner: sensorweb2
--

CREATE TABLE series_reference (
    series_pkid_from integer NOT NULL,
    sort_no numeric(2,0),
    series_pkid_to integer NOT NULL
);


ALTER TABLE sensorweb2.series_reference OWNER TO sensorweb2;

--
-- Name: TABLE series_reference; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON TABLE series_reference IS 'valid references between series';


--
-- Name: COLUMN series_reference.series_pkid_from; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN series_reference.series_pkid_from IS 'primary key of the series the observed values belongs to';


--
-- Name: COLUMN series_reference.sort_no; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN series_reference.sort_no IS 'display order';


--
-- Name: COLUMN series_reference.series_pkid_to; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN series_reference.series_pkid_to IS 'primary key of the series the comparative values belongs to';


--
-- Name: series_times; Type: VIEW; Schema: sensorweb2; Owner: postgres
--

CREATE VIEW series_times AS
    WITH series_select AS (SELECT s.pkid, c.category_id, f.feature_of_interest_id, p.phenomenon_id, r.procedure_id, ((s.data_origin)::text || COALESCE((', '::text || (s.data_origin_comment)::text), ''::text)) AS origin_comment, s.time_zone FROM ((((series s JOIN category c ON ((c.pkid = s.category_pkid))) JOIN feature_of_interest f ON ((f.pkid = s.feature_of_interest_pkid))) JOIN phenomenon p ON ((p.pkid = s.phenomenon_pkid))) JOIN procedure r ON ((r.pkid = s.procedure_pkid))) WHERE (NOT (s.pkid IN (SELECT series_reference.series_pkid_to FROM series_reference)))) SELECT a.pkid, a.category_id, a.feature_of_interest_id, a.phenomenon_id, a.procedure_id, a.origin_comment, ((to_char(min(o.time_stamp), 'DD.MM.YYYY'::text) || ' - '::text) || to_char(max(o.time_stamp), 'DD.MM.YYYY'::text)) AS period, count(o.numeric_value) AS count_values, avg((o.time_stamp - (SELECT observation_last_before.time_stamp FROM observation_last_before(o.series_pkid, o.time_stamp) observation_last_before(time_stamp, numeric_value)))) AS avg_time_lag, CASE a.time_zone WHEN 'MESZ'::text THEN min((o.time_stamp_insert - public.wintersommerzeit(o.time_stamp))) ELSE min((o.time_stamp_insert - o.time_stamp)) END AS min_import_lag, CASE a.time_zone WHEN 'MESZ'::text THEN max((o.time_stamp_insert - public.wintersommerzeit(o.time_stamp))) ELSE max((o.time_stamp_insert - o.time_stamp)) END AS max_import_lag FROM (series_select a JOIN observation o ON (((o.series_pkid = a.pkid) AND (o.time_stamp > (('now'::text)::timestamp(0) without time zone - '3 mons'::interval))))) GROUP BY a.pkid, a.category_id, a.feature_of_interest_id, a.phenomenon_id, a.procedure_id, a.origin_comment, a.time_zone ORDER BY a.category_id, a.feature_of_interest_id, a.phenomenon_id, a.procedure_id;


ALTER TABLE sensorweb2.series_times OWNER TO postgres;

--
-- Name: VIEW series_times; Type: COMMENT; Schema: sensorweb2; Owner: postgres
--

COMMENT ON VIEW series_times IS 'time statistics about imported values';


--
-- Name: COLUMN series_times.pkid; Type: COMMENT; Schema: sensorweb2; Owner: postgres
--

COMMENT ON COLUMN series_times.pkid IS 'serial primary key';


--
-- Name: COLUMN series_times.category_id; Type: COMMENT; Schema: sensorweb2; Owner: postgres
--

COMMENT ON COLUMN series_times.category_id IS 'identification of the category';


--
-- Name: COLUMN series_times.feature_of_interest_id; Type: COMMENT; Schema: sensorweb2; Owner: postgres
--

COMMENT ON COLUMN series_times.feature_of_interest_id IS 'identification of the feature of interest';


--
-- Name: COLUMN series_times.phenomenon_id; Type: COMMENT; Schema: sensorweb2; Owner: postgres
--

COMMENT ON COLUMN series_times.phenomenon_id IS 'identification of the phenomenon';


--
-- Name: COLUMN series_times.procedure_id; Type: COMMENT; Schema: sensorweb2; Owner: postgres
--

COMMENT ON COLUMN series_times.procedure_id IS 'identification of the procedure';


--
-- Name: COLUMN series_times.origin_comment; Type: COMMENT; Schema: sensorweb2; Owner: postgres
--

COMMENT ON COLUMN series_times.origin_comment IS 'first system storing the observed value';


--
-- Name: COLUMN series_times.period; Type: COMMENT; Schema: sensorweb2; Owner: postgres
--

COMMENT ON COLUMN series_times.period IS 'only observations during this period are queried';


--
-- Name: COLUMN series_times.count_values; Type: COMMENT; Schema: sensorweb2; Owner: postgres
--

COMMENT ON COLUMN series_times.count_values IS 'count of analyzed observations during the period';


--
-- Name: COLUMN series_times.avg_time_lag; Type: COMMENT; Schema: sensorweb2; Owner: postgres
--

COMMENT ON COLUMN series_times.avg_time_lag IS 'average time lag between two adjacend observations during the period';


--
-- Name: COLUMN series_times.min_import_lag; Type: COMMENT; Schema: sensorweb2; Owner: postgres
--

COMMENT ON COLUMN series_times.min_import_lag IS 'smallest time lag between measuring time stamp and importing time stamp during the period';


--
-- Name: COLUMN series_times.max_import_lag; Type: COMMENT; Schema: sensorweb2; Owner: postgres
--

COMMENT ON COLUMN series_times.max_import_lag IS 'biggest time lag between measuring time stamp and importing time stamp during the period';


--
-- Name: subscription_events; Type: VIEW; Schema: sensorweb2; Owner: postgres
--

CREATE VIEW subscription_events AS
    SELECT subscription.pkid AS "subscription.pkid", user_.userid AS "user_.userid", btrim((user_.emailaddress)::text) AS "user_.emailaddress", usergroup.usergroupid AS "usergroup.usergroupid", usergroup.name AS "usergroup.name", notification.pkid AS "notification.pkid", notification.series_pkid AS "notification.series_pkid", btrim((get_series_description(notification.series_pkid))::text) AS get_series_description, notification_rule.primary_rule_flag AS "notification_rule.primary_rule_flag", notification_level.description AS "notification_level.description", rule.pkid AS "rule.pkid", trend.code AS "trend.code", trend.description AS "trend.description", (((rule.threshold)::real)::text)::numeric AS "rule.threshold", (SELECT count(*) AS count FROM event_log WHERE (event_log.rule_pkid = rule.pkid)) AS event_log_count FROM (((((((subscription LEFT JOIN usergroup ON ((usergroup.usergroupid = subscription.usergroupid))) LEFT JOIN user_ ON ((user_.userid = subscription.userid))) LEFT JOIN notification ON ((notification.pkid = subscription.notification_pkid))) LEFT JOIN notification_rule ON ((notification_rule.notification_pkid = notification.pkid))) LEFT JOIN notification_level ON ((notification_level.level_id = notification_rule.notification_level_id))) LEFT JOIN rule ON ((rule.pkid = notification_rule.rule_pkid))) LEFT JOIN trend ON ((trend.code = rule.trend_code))) WHERE (subscription.userid IS NOT NULL) UNION SELECT subscription.pkid AS "subscription.pkid", user_.userid AS "user_.userid", btrim((user_.emailaddress)::text) AS "user_.emailaddress", usergroup.usergroupid AS "usergroup.usergroupid", usergroup.name AS "usergroup.name", notification.pkid AS "notification.pkid", notification.series_pkid AS "notification.series_pkid", btrim((get_series_description(notification.series_pkid))::text) AS get_series_description, notification_rule.primary_rule_flag AS "notification_rule.primary_rule_flag", notification_level.description AS "notification_level.description", rule.pkid AS "rule.pkid", trend.code AS "trend.code", trend.description AS "trend.description", (((rule.threshold)::real)::text)::numeric AS "rule.threshold", (SELECT count(*) AS count FROM event_log WHERE (event_log.rule_pkid = rule.pkid)) AS event_log_count FROM ((((((((subscription LEFT JOIN usergroup ON ((usergroup.usergroupid = subscription.usergroupid))) LEFT JOIN users_usergroups ON ((users_usergroups.usergroupid = usergroup.usergroupid))) LEFT JOIN user_ ON ((user_.userid = users_usergroups.userid))) LEFT JOIN notification ON ((notification.pkid = subscription.notification_pkid))) LEFT JOIN notification_rule ON ((notification_rule.notification_pkid = notification.pkid))) LEFT JOIN notification_level ON ((notification_level.level_id = notification_rule.notification_level_id))) LEFT JOIN rule ON ((rule.pkid = notification_rule.rule_pkid))) LEFT JOIN trend ON ((trend.code = rule.trend_code))) WHERE (subscription.usergroupid IS NOT NULL) ORDER BY 1, 2, 4, 5, 11;


ALTER TABLE sensorweb2.subscription_events OWNER TO postgres;

--
-- Name: VIEW subscription_events; Type: COMMENT; Schema: sensorweb2; Owner: postgres
--

COMMENT ON VIEW subscription_events IS 'shows all eventing data from subscription.pkid to event_log count';


--
-- Name: subscription_pkid_seq; Type: SEQUENCE; Schema: sensorweb2; Owner: sensorweb2
--

CREATE SEQUENCE subscription_pkid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE sensorweb2.subscription_pkid_seq OWNER TO sensorweb2;

--
-- Name: subscription_pkid_seq; Type: SEQUENCE OWNED BY; Schema: sensorweb2; Owner: sensorweb2
--

ALTER SEQUENCE subscription_pkid_seq OWNED BY subscription.pkid;


--
-- Name: trend_i18n; Type: TABLE; Schema: sensorweb2; Owner: sensorweb2
--

CREATE TABLE trend_i18n (
    pkid integer NOT NULL,
    trend_code numeric(2,0) NOT NULL,
    locale character varying(10) NOT NULL,
    description character varying(100)
);


ALTER TABLE sensorweb2.trend_i18n OWNER TO sensorweb2;

--
-- Name: TABLE trend_i18n; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON TABLE trend_i18n IS 'internationalization of trends';


--
-- Name: COLUMN trend_i18n.pkid; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN trend_i18n.pkid IS 'serial primary key';


--
-- Name: COLUMN trend_i18n.trend_code; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN trend_i18n.trend_code IS 'code of the trend the record belongs to';


--
-- Name: COLUMN trend_i18n.locale; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN trend_i18n.locale IS 'language identifier and region identifier';


--
-- Name: COLUMN trend_i18n.description; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN trend_i18n.description IS 'description of the trend in locale''s language';


--
-- Name: trend_i18n_pkid_seq; Type: SEQUENCE; Schema: sensorweb2; Owner: sensorweb2
--

CREATE SEQUENCE trend_i18n_pkid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE sensorweb2.trend_i18n_pkid_seq OWNER TO sensorweb2;

--
-- Name: trend_i18n_pkid_seq; Type: SEQUENCE OWNED BY; Schema: sensorweb2; Owner: sensorweb2
--

ALTER SEQUENCE trend_i18n_pkid_seq OWNED BY trend_i18n.pkid;


--
-- Name: unit_i18n; Type: TABLE; Schema: sensorweb2; Owner: sensorweb2
--

CREATE TABLE unit_i18n (
    pkid integer NOT NULL,
    unit_pkid integer NOT NULL,
    locale character varying(10) NOT NULL,
    unit character varying(30),
    description character varying(100)
);


ALTER TABLE sensorweb2.unit_i18n OWNER TO sensorweb2;

--
-- Name: TABLE unit_i18n; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON TABLE unit_i18n IS 'internationalization of units';


--
-- Name: COLUMN unit_i18n.pkid; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN unit_i18n.pkid IS 'serial primary key';


--
-- Name: COLUMN unit_i18n.unit_pkid; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN unit_i18n.unit_pkid IS 'Id of the unit the record belongs to';


--
-- Name: COLUMN unit_i18n.locale; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN unit_i18n.locale IS 'language identifier and region identifier';


--
-- Name: COLUMN unit_i18n.unit; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN unit_i18n.unit IS 'unit in locale''s language';


--
-- Name: COLUMN unit_i18n.description; Type: COMMENT; Schema: sensorweb2; Owner: sensorweb2
--

COMMENT ON COLUMN unit_i18n.description IS 'description of the unit in locale''s language';


--
-- Name: unit_i18n_pkid_seq; Type: SEQUENCE; Schema: sensorweb2; Owner: sensorweb2
--

CREATE SEQUENCE unit_i18n_pkid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE sensorweb2.unit_i18n_pkid_seq OWNER TO sensorweb2;

--
-- Name: unit_i18n_pkid_seq; Type: SEQUENCE OWNED BY; Schema: sensorweb2; Owner: sensorweb2
--

ALTER SEQUENCE unit_i18n_pkid_seq OWNED BY unit_i18n.pkid;


--
-- Name: unit_pkid_seq; Type: SEQUENCE; Schema: sensorweb2; Owner: sensorweb2
--

CREATE SEQUENCE unit_pkid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE sensorweb2.unit_pkid_seq OWNER TO sensorweb2;

--
-- Name: unit_pkid_seq; Type: SEQUENCE OWNED BY; Schema: sensorweb2; Owner: sensorweb2
--

ALTER SEQUENCE unit_pkid_seq OWNED BY unit.pkid;


--
-- Name: usergroup_emailaddresses; Type: VIEW; Schema: sensorweb2; Owner: postgres
--

CREATE VIEW usergroup_emailaddresses AS
    SELECT g.name AS usergroup, u.emailaddress FROM ((usergroup g JOIN users_usergroups uu ON ((uu.usergroupid = g.usergroupid))) JOIN user_ u ON ((u.userid = uu.userid)));


ALTER TABLE sensorweb2.usergroup_emailaddresses OWNER TO postgres;

--
-- Name: VIEW usergroup_emailaddresses; Type: COMMENT; Schema: sensorweb2; Owner: postgres
--

COMMENT ON VIEW usergroup_emailaddresses IS 'defined e-mail adresses in usergroups';


--
-- Name: pkid; Type: DEFAULT; Schema: sensorweb2; Owner: sensorweb2
--

ALTER TABLE ONLY category ALTER COLUMN pkid SET DEFAULT nextval('category_pkid_seq'::regclass);


--
-- Name: pkid; Type: DEFAULT; Schema: sensorweb2; Owner: sensorweb2
--

ALTER TABLE ONLY category_i18n ALTER COLUMN pkid SET DEFAULT nextval('category_i18n_pkid_seq'::regclass);


--
-- Name: pkid; Type: DEFAULT; Schema: sensorweb2; Owner: sensorweb2
--

ALTER TABLE ONLY cluster ALTER COLUMN pkid SET DEFAULT nextval('cluster_pkid_seq'::regclass);


--
-- Name: pkid; Type: DEFAULT; Schema: sensorweb2; Owner: sensorweb2
--

ALTER TABLE ONLY event_log ALTER COLUMN pkid SET DEFAULT nextval('event_log_pkid_seq'::regclass);


--
-- Name: pkid; Type: DEFAULT; Schema: sensorweb2; Owner: sensorweb2
--

ALTER TABLE ONLY event_message ALTER COLUMN pkid SET DEFAULT nextval('event_message_pkid_seq'::regclass);


--
-- Name: pkid; Type: DEFAULT; Schema: sensorweb2; Owner: sensorweb2
--

ALTER TABLE ONLY event_message_i18n ALTER COLUMN pkid SET DEFAULT nextval('event_message_i18n_pkid_seq'::regclass);


--
-- Name: pkid; Type: DEFAULT; Schema: sensorweb2; Owner: sensorweb2
--

ALTER TABLE ONLY event_type ALTER COLUMN pkid SET DEFAULT nextval('event_type_pkid_seq'::regclass);


--
-- Name: pkid; Type: DEFAULT; Schema: sensorweb2; Owner: sensorweb2
--

ALTER TABLE ONLY event_type_i18n ALTER COLUMN pkid SET DEFAULT nextval('event_type_i18n_pkid_seq'::regclass);


--
-- Name: pkid; Type: DEFAULT; Schema: sensorweb2; Owner: sensorweb2
--

ALTER TABLE ONLY feature_of_interest ALTER COLUMN pkid SET DEFAULT nextval('feature_of_interest_pkid_seq'::regclass);


--
-- Name: pkid; Type: DEFAULT; Schema: sensorweb2; Owner: sensorweb2
--

ALTER TABLE ONLY feature_of_interest_i18n ALTER COLUMN pkid SET DEFAULT nextval('feature_of_interest_i18n_pkid_seq'::regclass);


--
-- Name: pkid; Type: DEFAULT; Schema: sensorweb2; Owner: sensorweb2
--

ALTER TABLE ONLY notification ALTER COLUMN pkid SET DEFAULT nextval('notification_pkid_seq'::regclass);


--
-- Name: pkid; Type: DEFAULT; Schema: sensorweb2; Owner: sensorweb2
--

ALTER TABLE ONLY notification_level_i18n ALTER COLUMN pkid SET DEFAULT nextval('notification_level_i18n_pkid_seq'::regclass);


--
-- Name: observation_id; Type: DEFAULT; Schema: sensorweb2; Owner: sensorweb2
--

ALTER TABLE ONLY observation ALTER COLUMN observation_id SET DEFAULT nextval('observation_observation_id_seq'::regclass);


--
-- Name: pkid; Type: DEFAULT; Schema: sensorweb2; Owner: sensorweb2
--

ALTER TABLE ONLY phenomenon ALTER COLUMN pkid SET DEFAULT nextval('phenomenon_pkid_seq'::regclass);


--
-- Name: pkid; Type: DEFAULT; Schema: sensorweb2; Owner: sensorweb2
--

ALTER TABLE ONLY phenomenon_i18n ALTER COLUMN pkid SET DEFAULT nextval('phenomenon_i18n_pkid_seq'::regclass);


--
-- Name: pkid; Type: DEFAULT; Schema: sensorweb2; Owner: sensorweb2
--

ALTER TABLE ONLY procedure ALTER COLUMN pkid SET DEFAULT nextval('procedure_pkid_seq'::regclass);


--
-- Name: pkid; Type: DEFAULT; Schema: sensorweb2; Owner: sensorweb2
--

ALTER TABLE ONLY procedure_i18n ALTER COLUMN pkid SET DEFAULT nextval('procedure_i18n_pkid_seq'::regclass);


--
-- Name: pkid; Type: DEFAULT; Schema: sensorweb2; Owner: sensorweb2
--

ALTER TABLE ONLY rule ALTER COLUMN pkid SET DEFAULT nextval('rule_pkid_seq'::regclass);


--
-- Name: pkid; Type: DEFAULT; Schema: sensorweb2; Owner: sensorweb2
--

ALTER TABLE ONLY series ALTER COLUMN pkid SET DEFAULT nextval('series_pkid_seq'::regclass);


--
-- Name: pkid; Type: DEFAULT; Schema: sensorweb2; Owner: sensorweb2
--

ALTER TABLE ONLY subscription ALTER COLUMN pkid SET DEFAULT nextval('subscription_pkid_seq'::regclass);


--
-- Name: pkid; Type: DEFAULT; Schema: sensorweb2; Owner: sensorweb2
--

ALTER TABLE ONLY trend_i18n ALTER COLUMN pkid SET DEFAULT nextval('trend_i18n_pkid_seq'::regclass);


--
-- Name: pkid; Type: DEFAULT; Schema: sensorweb2; Owner: sensorweb2
--

ALTER TABLE ONLY unit ALTER COLUMN pkid SET DEFAULT nextval('unit_pkid_seq'::regclass);


--
-- Name: pkid; Type: DEFAULT; Schema: sensorweb2; Owner: sensorweb2
--

ALTER TABLE ONLY unit_i18n ALTER COLUMN pkid SET DEFAULT nextval('unit_i18n_pkid_seq'::regclass);


--
-- TOC entry 5088 (class 0 OID 20742)
-- Dependencies: 275
-- Data for Name: category; Type: TABLE DATA; Schema: sensorweb2; Owner: sensorweb2
--

COPY sensorweb2.category (pkid, category_id, category_name, description) FROM stdin;
3	Verwaltung	Verwaltung	\N
\.


--
-- TOC entry 5089 (class 0 OID 20745)
-- Dependencies: 276
-- Data for Name: category_i18n; Type: TABLE DATA; Schema: sensorweb2; Owner: sensorweb2
--

COPY sensorweb2.category_i18n (pkid, category_pkid, locale, name, description) FROM stdin;
2	3	zh	水体质量	\N
16	3	en	Administration	\N
17	3	de	Verwaltung	\N
\.


--
-- TOC entry 5092 (class 0 OID 20752)
-- Dependencies: 279
-- Data for Name: cluster; Type: TABLE DATA; Schema: sensorweb2; Owner: sensorweb2
--

COPY sensorweb2.cluster (pkid, cluster_id, cluster_name, description) FROM stdin;
1	TAMIS	TAMIS	Talsperren-Mess- und Informationssystem
2	intern	intern	Alle fachlichen Daten fuer den internen Gebrauch
3	extern	extern	Alle fachlichen Daten fuer den externen Gebrauch
4	MUDAK	MUDAK	Multidisziplinäre Datenakquisition
\.


--
-- TOC entry 5094 (class 0 OID 20757)
-- Dependencies: 281
-- Data for Name: event_log; Type: TABLE DATA; Schema: sensorweb2; Owner: sensorweb2
--

COPY sensorweb2.event_log (pkid, time_stamp_created, event_type_pkid, series_pkid, observation_time_stamp, observation_numeric_value, observation_previous_time_stamp, observation_previous_numeric_value, event_message_pkid, rule_pkid, user_userid, usergroup_usergroupid, notification_pkid) FROM stdin;
\.


--
-- TOC entry 5095 (class 0 OID 20761)
-- Dependencies: 282
-- Data for Name: event_log_mail; Type: TABLE DATA; Schema: sensorweb2; Owner: sensorweb2
--

COPY sensorweb2.event_log_mail (event_log_pkid, mail_address, mail_sent, subscription_pkid, content, userid, notification_level_id) FROM stdin;
\.


--
-- TOC entry 5097 (class 0 OID 20769)
-- Dependencies: 284
-- Data for Name: event_message; Type: TABLE DATA; Schema: sensorweb2; Owner: sensorweb2
--

COPY sensorweb2.event_message (pkid, name, message_text) FROM stdin;
1	eventing active	Ereignisprotokollierung eingeschaltet
2	eventing inactive	Ereignisprotokollierung ausgeschaltet
5	new subscription to usergroup	Neues Abonnement für die Benutzergruppe "{usergroup}" zur Benachrichtigung "{notification}" definiert
6	remove subscription from usergroup	Definiertes Abonnement der Benutzergruppe "{usergroup}" zur Benachrichtigung "{notification}" entfernt
7	new subscription to user	Neues Abonnement für den Benutzernamen "{user}" zur Benachrichtigung "{notification}" definiert
8	remove subscription from user	Definiertes Abonnement des Benutzernamens "{user}" zur Benachrichtigung "{notification}" entfernt
3	age exceeded	Letzter Messwert ist älter als {interval_text}
4	age ok	Letzter Messwert ist wieder aktueller als {interval_text}
\.


--
-- TOC entry 5098 (class 0 OID 20775)
-- Dependencies: 285
-- Data for Name: event_message_i18n; Type: TABLE DATA; Schema: sensorweb2; Owner: sensorweb2
--

COPY sensorweb2.event_message_i18n (pkid, event_message_pkid, locale, name, message_text) FROM stdin;
\.


--
-- TOC entry 5101 (class 0 OID 20785)
-- Dependencies: 288
-- Data for Name: event_type; Type: TABLE DATA; Schema: sensorweb2; Owner: sensorweb2
--

COPY sensorweb2.event_type (pkid, name, description) FROM stdin;
1	Regel-Ereignis	Das Ereignis wurde durch eine Regel erzeugt
3	Alterskontrolle	Das Ereignis wurde duch die Kontrolle der aktuellsten Beobachtung einer Zeitreihe erzeugt
4	Abonnement	Das Ereignis wurde duch die Zuordnung von Benachrichtigungen zu Benutzer oder Gruppen erzeugt
2	Zeitreihen-Verwaltung	Das Ereignis wurde durch die Aenderung eines Attributes einer Zeitreihe erzeugt
\.


--
-- TOC entry 5102 (class 0 OID 20788)
-- Dependencies: 289
-- Data for Name: event_type_i18n; Type: TABLE DATA; Schema: sensorweb2; Owner: sensorweb2
--

COPY sensorweb2.event_type_i18n (pkid, event_type_pkid, locale, name, description) FROM stdin;
\.


--
-- TOC entry 5105 (class 0 OID 20795)
-- Dependencies: 292
-- Data for Name: feature_of_interest; Type: TABLE DATA; Schema: sensorweb2; Owner: sensorweb2
--

COPY sensorweb2.feature_of_interest (pkid, feature_of_interest_id, feature_of_interest_name, geom, feature_class, reference_wv_id, description) FROM stdin;
\.


--
-- TOC entry 5106 (class 0 OID 20801)
-- Dependencies: 293
-- Data for Name: feature_of_interest_i18n; Type: TABLE DATA; Schema: sensorweb2; Owner: sensorweb2
--

COPY sensorweb2.feature_of_interest_i18n (pkid, feature_of_interest_pkid, locale, name, description) FROM stdin;
\.


--
-- TOC entry 5109 (class 0 OID 20808)
-- Dependencies: 296
-- Data for Name: notification; Type: TABLE DATA; Schema: sensorweb2; Owner: sensorweb2
--

COPY sensorweb2.notification (pkid, series_pkid) FROM stdin;
\.


--
-- TOC entry 5111 (class 0 OID 20820)
-- Dependencies: 299
-- Data for Name: notification_level; Type: TABLE DATA; Schema: sensorweb2; Owner: sensorweb2
--

COPY sensorweb2.notification_level (level_id, description) FROM stdin;
0	Info
1	Warnung
9	Entwarnung
13	Schwellenwert Minimum
17	Schwellenwert Maximum
\.


--
-- TOC entry 5112 (class 0 OID 20823)
-- Dependencies: 300
-- Data for Name: notification_level_i18n; Type: TABLE DATA; Schema: sensorweb2; Owner: sensorweb2
--

COPY sensorweb2.notification_level_i18n (pkid, notification_level_id, locale, description) FROM stdin;
\.


--
-- TOC entry 5110 (class 0 OID 20811)
-- Dependencies: 297
-- Data for Name: notification_rule; Type: TABLE DATA; Schema: sensorweb2; Owner: sensorweb2
--

COPY sensorweb2.notification_rule (notification_pkid, rule_pkid, notification_level_id, primary_rule_flag) FROM stdin;
\.


--
-- TOC entry 5125 (class 0 OID 20884)
-- Dependencies: 316
-- Data for Name: observation; Type: TABLE DATA; Schema: sensorweb2; Owner: sensorweb2
--

COPY sensorweb2.observation (observation_id, time_stamp, series_pkid, numeric_value, result_time, time_stamp_insert, comment) FROM stdin;
\.


--
-- TOC entry 5121 (class 0 OID 20865)
-- Dependencies: 311
-- Data for Name: phenomenon; Type: TABLE DATA; Schema: sensorweb2; Owner: sensorweb2
--

COPY sensorweb2.phenomenon (pkid, phenomenon_id, phenomenon_name, description) FROM stdin;
1	Wasserstand	Wasserstand	\N
\.


--
-- TOC entry 5127 (class 0 OID 20894)
-- Dependencies: 319
-- Data for Name: phenomenon_i18n; Type: TABLE DATA; Schema: sensorweb2; Owner: sensorweb2
--

COPY sensorweb2.phenomenon_i18n (pkid, phenomenon_pkid, locale, name, description) FROM stdin;
26	1	de	Wasserstand	\N
27	1	en	Water Level	\N
\.


--
-- TOC entry 5122 (class 0 OID 20868)
-- Dependencies: 312
-- Data for Name: procedure; Type: TABLE DATA; Schema: sensorweb2; Owner: sensorweb2
--

COPY sensorweb2.procedure (pkid, procedure_id, procedure_name, description_type, reference_flag, description) FROM stdin;
267	Tagesmittelwert	Tagesmittelwert	text/xml;subtype="SensorML/1.0.1"	0	\N
254	Einzelwert	Einzelwert	text/xml;subtype="SensorML/1.0.1"	0	\N
\.


--
-- TOC entry 5130 (class 0 OID 20901)
-- Dependencies: 322
-- Data for Name: procedure_i18n; Type: TABLE DATA; Schema: sensorweb2; Owner: sensorweb2
--

COPY sensorweb2.procedure_i18n (pkid, procedure_pkid, locale, name, description) FROM stdin;
3	254	de	Einzelwert	\N
4	254	en	Single Value	\N
9	267	de	Tagesmittelwert	\N
10	267	en	Daily mean	\N
\.


--
-- TOC entry 5114 (class 0 OID 20828)
-- Dependencies: 302
-- Data for Name: rule; Type: TABLE DATA; Schema: sensorweb2; Owner: sensorweb2
--

COPY sensorweb2.rule (pkid, series_pkid, threshold, trend_code) FROM stdin;
\.


--
-- TOC entry 5123 (class 0 OID 20871)
-- Dependencies: 313
-- Data for Name: series; Type: TABLE DATA; Schema: sensorweb2; Owner: sensorweb2
--

COPY sensorweb2.series (pkid, category_pkid, phenomenon_pkid, procedure_pkid, feature_of_interest_pkid, decimals, unit_pkid, first_time_stamp, first_numeric_value, last_time_stamp, last_numeric_value, data_origin, pegel_online, published_flag, time_zone, data_origin_comment, retention_time, eventing_flag) FROM stdin;
\.


--
-- TOC entry 5134 (class 0 OID 20910)
-- Dependencies: 326
-- Data for Name: series_check_age; Type: TABLE DATA; Schema: sensorweb2; Owner: sensorweb2
--

COPY sensorweb2.series_check_age (series_pkid, check_interval, message_generated, unit_pkid, factor_seconds, conversion_precision) FROM stdin;
\.


--
-- TOC entry 5135 (class 0 OID 20913)
-- Dependencies: 327
-- Data for Name: series_cluster; Type: TABLE DATA; Schema: sensorweb2; Owner: sensorweb2
--

COPY sensorweb2.series_cluster (series_pkid, cluster_pkid) FROM stdin;
\.


--
-- TOC entry 5136 (class 0 OID 20921)
-- Dependencies: 329
-- Data for Name: series_derived; Type: TABLE DATA; Schema: sensorweb2; Owner: sensorweb2
--

COPY sensorweb2.series_derived (series_pkid_base, series_pkid_derived, formula) FROM stdin;
\.


--
-- TOC entry 5138 (class 0 OID 20941)
-- Dependencies: 334
-- Data for Name: series_reference; Type: TABLE DATA; Schema: sensorweb2; Owner: sensorweb2
--

COPY sensorweb2.series_reference (series_pkid_from, sort_no, series_pkid_to) FROM stdin;
\.


--
-- TOC entry 5115 (class 0 OID 20831)
-- Dependencies: 303
-- Data for Name: subscription; Type: TABLE DATA; Schema: sensorweb2; Owner: sensorweb2
--

COPY sensorweb2.subscription (pkid, userid, usergroupid, notification_pkid) FROM stdin;
\.


--
-- TOC entry 5116 (class 0 OID 20835)
-- Dependencies: 304
-- Data for Name: trend; Type: TABLE DATA; Schema: sensorweb2; Owner: sensorweb2
--

COPY sensorweb2.trend (code, description) FROM stdin;
11	bleibt unter
12	steigt auf
13	steigt über
21	fällt unter
22	bleibt auf
23	steigt über
31	fällt unter
32	fällt auf
33	bleibt über
\.


--
-- TOC entry 5140 (class 0 OID 20956)
-- Dependencies: 338
-- Data for Name: trend_i18n; Type: TABLE DATA; Schema: sensorweb2; Owner: sensorweb2
--

COPY sensorweb2.trend_i18n (pkid, trend_code, locale, description) FROM stdin;
1	11	en	\N
2	12	en	\N
3	13	en	\N
4	21	en	\N
5	22	en	\N
6	23	en	\N
7	31	en	\N
8	32	en	\N
9	33	en	\N
\.


--
-- TOC entry 5124 (class 0 OID 20876)
-- Dependencies: 314
-- Data for Name: unit; Type: TABLE DATA; Schema: sensorweb2; Owner: sensorweb2
--

COPY sensorweb2.unit (pkid, unit, description) FROM stdin;
5	cm	Zentimeter
\.


--
-- TOC entry 5142 (class 0 OID 20961)
-- Dependencies: 340
-- Data for Name: unit_i18n; Type: TABLE DATA; Schema: sensorweb2; Owner: sensorweb2
--

COPY sensorweb2.unit_i18n (pkid, unit_pkid, locale, unit, description) FROM stdin;
1	5	zh	厘米	\N
\.


--
-- TOC entry 5117 (class 0 OID 20838)
-- Dependencies: 305
-- Data for Name: user_; Type: TABLE DATA; Schema: sensorweb2; Owner: sensorweb2
--

COPY sensorweb2.user_ (userid, password_, screenname, emailaddress, languageid, firstname, middlename, lastname) FROM stdin;
\.


--
-- TOC entry 5118 (class 0 OID 20844)
-- Dependencies: 306
-- Data for Name: usergroup; Type: TABLE DATA; Schema: sensorweb2; Owner: sensorweb2
--

COPY sensorweb2.usergroup (usergroupid, companyid, parentusergroupid, name, description, addedbyldapimport) FROM stdin;
\.


--
-- TOC entry 5119 (class 0 OID 20850)
-- Dependencies: 307
-- Data for Name: users_usergroups; Type: TABLE DATA; Schema: sensorweb2; Owner: sensorweb2
--

COPY sensorweb2.users_usergroups (userid, usergroupid) FROM stdin;
\.


--
-- TOC entry 5490 (class 0 OID 0)
-- Dependencies: 277
-- Name: category_i18n_pkid_seq; Type: SEQUENCE SET; Schema: sensorweb2; Owner: sensorweb2
--

SELECT pg_catalog.setval('sensorweb2.category_i18n_pkid_seq', 17, true);


--
-- TOC entry 5491 (class 0 OID 0)
-- Dependencies: 278
-- Name: category_pkid_seq; Type: SEQUENCE SET; Schema: sensorweb2; Owner: sensorweb2
--

SELECT pg_catalog.setval('sensorweb2.category_pkid_seq', 13, true);


--
-- TOC entry 5492 (class 0 OID 0)
-- Dependencies: 280
-- Name: cluster_pkid_seq; Type: SEQUENCE SET; Schema: sensorweb2; Owner: sensorweb2
--

SELECT pg_catalog.setval('sensorweb2.cluster_pkid_seq', 4, true);


--
-- TOC entry 5493 (class 0 OID 0)
-- Dependencies: 283
-- Name: event_log_pkid_seq; Type: SEQUENCE SET; Schema: sensorweb2; Owner: sensorweb2
--

SELECT pg_catalog.setval('sensorweb2.event_log_pkid_seq', 38657, true);


--
-- TOC entry 5494 (class 0 OID 0)
-- Dependencies: 286
-- Name: event_message_i18n_pkid_seq; Type: SEQUENCE SET; Schema: sensorweb2; Owner: sensorweb2
--

SELECT pg_catalog.setval('sensorweb2.event_message_i18n_pkid_seq', 1, false);


--
-- TOC entry 5495 (class 0 OID 0)
-- Dependencies: 287
-- Name: event_message_pkid_seq; Type: SEQUENCE SET; Schema: sensorweb2; Owner: sensorweb2
--

SELECT pg_catalog.setval('sensorweb2.event_message_pkid_seq', 9, false);


--
-- TOC entry 5496 (class 0 OID 0)
-- Dependencies: 290
-- Name: event_type_i18n_pkid_seq; Type: SEQUENCE SET; Schema: sensorweb2; Owner: sensorweb2
--

SELECT pg_catalog.setval('sensorweb2.event_type_i18n_pkid_seq', 1, false);


--
-- TOC entry 5497 (class 0 OID 0)
-- Dependencies: 291
-- Name: event_type_pkid_seq; Type: SEQUENCE SET; Schema: sensorweb2; Owner: sensorweb2
--

SELECT pg_catalog.setval('sensorweb2.event_type_pkid_seq', 5, false);


--
-- TOC entry 5498 (class 0 OID 0)
-- Dependencies: 294
-- Name: feature_of_interest_i18n_pkid_seq; Type: SEQUENCE SET; Schema: sensorweb2; Owner: sensorweb2
--

SELECT pg_catalog.setval('sensorweb2.feature_of_interest_i18n_pkid_seq', 41, true);


--
-- TOC entry 5499 (class 0 OID 0)
-- Dependencies: 295
-- Name: feature_of_interest_pkid_seq; Type: SEQUENCE SET; Schema: sensorweb2; Owner: sensorweb2
--

SELECT pg_catalog.setval('sensorweb2.feature_of_interest_pkid_seq', 211, true);


--
-- TOC entry 5500 (class 0 OID 0)
-- Dependencies: 301
-- Name: notification_level_i18n_pkid_seq; Type: SEQUENCE SET; Schema: sensorweb2; Owner: sensorweb2
--

SELECT pg_catalog.setval('sensorweb2.notification_level_i18n_pkid_seq', 1, false);


--
-- TOC entry 5501 (class 0 OID 0)
-- Dependencies: 309
-- Name: notification_pkid_seq; Type: SEQUENCE SET; Schema: sensorweb2; Owner: sensorweb2
--

SELECT pg_catalog.setval('sensorweb2.notification_pkid_seq', 166, false);


--
-- TOC entry 5502 (class 0 OID 0)
-- Dependencies: 318
-- Name: observation_observation_id_seq; Type: SEQUENCE SET; Schema: sensorweb2; Owner: sensorweb2
--

SELECT pg_catalog.setval('sensorweb2.observation_observation_id_seq', 142628714, true);


--
-- TOC entry 5503 (class 0 OID 0)
-- Dependencies: 320
-- Name: phenomenon_i18n_pkid_seq; Type: SEQUENCE SET; Schema: sensorweb2; Owner: sensorweb2
--

SELECT pg_catalog.setval('sensorweb2.phenomenon_i18n_pkid_seq', 27, true);


--
-- TOC entry 5504 (class 0 OID 0)
-- Dependencies: 321
-- Name: phenomenon_pkid_seq; Type: SEQUENCE SET; Schema: sensorweb2; Owner: sensorweb2
--

SELECT pg_catalog.setval('sensorweb2.phenomenon_pkid_seq', 52, true);


--
-- TOC entry 5505 (class 0 OID 0)
-- Dependencies: 323
-- Name: procedure_i18n_pkid_seq; Type: SEQUENCE SET; Schema: sensorweb2; Owner: sensorweb2
--

SELECT pg_catalog.setval('sensorweb2.procedure_i18n_pkid_seq', 12, true);


--
-- TOC entry 5506 (class 0 OID 0)
-- Dependencies: 324
-- Name: procedure_pkid_seq; Type: SEQUENCE SET; Schema: sensorweb2; Owner: sensorweb2
--

SELECT pg_catalog.setval('sensorweb2.procedure_pkid_seq', 313, true);


--
-- TOC entry 5507 (class 0 OID 0)
-- Dependencies: 325
-- Name: rule_pkid_seq; Type: SEQUENCE SET; Schema: sensorweb2; Owner: sensorweb2
--

SELECT pg_catalog.setval('sensorweb2.rule_pkid_seq', 938, false);


--
-- TOC entry 5508 (class 0 OID 0)
-- Dependencies: 331
-- Name: series_pkid_seq; Type: SEQUENCE SET; Schema: sensorweb2; Owner: sensorweb2
--

SELECT pg_catalog.setval('sensorweb2.series_pkid_seq', 703, true);


--
-- TOC entry 5509 (class 0 OID 0)
-- Dependencies: 337
-- Name: subscription_pkid_seq; Type: SEQUENCE SET; Schema: sensorweb2; Owner: sensorweb2
--

SELECT pg_catalog.setval('sensorweb2.subscription_pkid_seq', 513, true);


--
-- TOC entry 5510 (class 0 OID 0)
-- Dependencies: 339
-- Name: trend_i18n_pkid_seq; Type: SEQUENCE SET; Schema: sensorweb2; Owner: sensorweb2
--

SELECT pg_catalog.setval('sensorweb2.trend_i18n_pkid_seq', 10, false);


--
-- TOC entry 5511 (class 0 OID 0)
-- Dependencies: 341
-- Name: unit_i18n_pkid_seq; Type: SEQUENCE SET; Schema: sensorweb2; Owner: sensorweb2
--

SELECT pg_catalog.setval('sensorweb2.unit_i18n_pkid_seq', 1, false);


--
-- TOC entry 5512 (class 0 OID 0)
-- Dependencies: 342
-- Name: unit_pkid_seq; Type: SEQUENCE SET; Schema: sensorweb2; Owner: sensorweb2
--

SELECT pg_catalog.setval('sensorweb2.unit_pkid_seq', 26, true);


