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

-- Function: public.wintersommerzeit(timestamp without time zone)

-- DROP FUNCTION public.wintersommerzeit(timestamp without time zone);

CREATE OR REPLACE FUNCTION public.wintersommerzeit(pzeitpunkt timestamp without time zone)
  RETURNS timestamp without time zone AS
$BODY$
-- Anpassen eines Zeitpunktes auf die lokale Sommerzeit
-- tt-2011-09-14
DECLARE
  vDatum DATE;
  vSonntag DATE;
  vSommerzeitAnfang TIMESTAMP;
  vSommerzeitEnde TIMESTAMP;
  vZeitpunkt TIMESTAMP;
BEGIN
  -- Ausgehend vom 1.4. des Jahres den vorangehenden Sonntag bestimmen
  vDatum := (EXTRACT(YEAR FROM pZeitpunkt) || '-04-01')::DATE;
  vSonntag := vDatum - TO_CHAR(vDatum - 1, 'd')::INT;
  vSommerzeitAnfang := (vSonntag::TEXT || ' 02:00:00')::TIMESTAMP;
  -- Ausgehend vom 1.11. des Jahres den vorangehenden Sonntag bestimmen
  vDatum := (EXTRACT(YEAR FROM pZeitpunkt) || '-11-01')::DATE;
  vSonntag := vDatum - TO_CHAR(vDatum - 1, 'd')::INT;
  vSommerzeitEnde := (vSonntag::TEXT || ' 02:00:00')::TIMESTAMP;
  -- Uebergebenen Winterzeitpunkt auf Sommerzeit korrigieren
  IF pZeitpunkt BETWEEN vSommerzeitAnfang AND vSommerzeitEnde THEN
    vZeitpunkt := pZeitpunkt + INTERVAL '1 hour';
  ELSE
    vZeitpunkt := pZeitpunkt;
  END IF;
  RETURN vZeitpunkt;
END;

$BODY$
  LANGUAGE plpgsql VOLATILE SECURITY DEFINER
  COST 100;
ALTER FUNCTION public.wintersommerzeit(timestamp without time zone)
  OWNER TO postgres;
GRANT EXECUTE ON FUNCTION public.wintersommerzeit(timestamp without time zone) TO public;
GRANT EXECUTE ON FUNCTION public.wintersommerzeit(timestamp without time zone) TO postgres;
COMMENT ON FUNCTION public.wintersommerzeit(timestamp without time zone) IS 'tt-2015-09-23 Umrechnen eines Zeitpunktes (MEZ) auf die lokale (Sommer-) Zeit (MESZ/MEZ)';
