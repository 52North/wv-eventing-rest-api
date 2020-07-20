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

-- Function: public.interval_text(interval)

-- DROP FUNCTION public.interval_text(interval);

CREATE OR REPLACE FUNCTION public.interval_text(input_interval interval)
  RETURNS character varying AS
$BODY$
-- Wandelt das Eingabeintervall in eine lesbare Dauer um.
-- tt-2016-05-25
DECLARE
    output_text VARCHAR := '';
    x INTEGER;
BEGIN
    x = EXTRACT(year FROM input_interval);
    IF x > 0 THEN
        output_text := output_text || x::VARCHAR || ' ';
        IF x = 1 THEN
            output_text := output_text || 'Jahr';
        ELSE
            output_text := output_text || 'Jahre';
        END IF;
    END IF;
    x = EXTRACT(month FROM input_interval);
    IF x > 0 THEN
        IF LENGTH(output_text) > 0 THEN
            output_text := output_text || ' ';
        END IF;
        output_text := output_text || x::VARCHAR || ' ';
        IF x = 1 THEN
            output_text := output_text || 'Monat';
        ELSE
            output_text := output_text || 'Monate';
        END IF;
    END IF;
    x = EXTRACT(day FROM input_interval);
    IF x > 0 THEN
        IF LENGTH(output_text) > 0 THEN
            output_text := output_text || ' ';
        END IF;
        output_text := output_text || x::VARCHAR || ' ';
        IF x = 1 THEN
            output_text := output_text || 'Tag';
        ELSE
            output_text := output_text || 'Tage';
        END IF;
    END IF;
    x = EXTRACT(hour FROM input_interval);
    IF x > 0 THEN
        IF LENGTH(output_text) > 0 THEN
            output_text := output_text || ' ';
        END IF;
        output_text := output_text || x::VARCHAR || ' ';
        IF x = 1 THEN
            output_text := output_text || 'Stunde';
        ELSE
            output_text := output_text || 'Stunden';
        END IF;
    END IF;
    x = EXTRACT(minute FROM input_interval);
    IF x > 0 THEN
        IF LENGTH(output_text) > 0 THEN
            output_text := output_text || ' ';
        END IF;
        output_text := output_text || x::VARCHAR || ' ';
        IF x = 1 THEN
            output_text := output_text || 'Minute';
        ELSE
            output_text := output_text || 'Minuten';
        END IF;
    END IF;
    x = EXTRACT(second FROM input_interval);
    IF x > 0 THEN
        IF LENGTH(output_text) > 0 THEN
            output_text := output_text || ' ';
        END IF;
        output_text := output_text || x::VARCHAR || ' ';
        IF x = 1 THEN
            output_text := output_text || 'Sekunde';
        ELSE
            output_text := output_text || 'Sekunden';
        END IF;
    END IF;
    RETURN output_text;
END;

$BODY$
  LANGUAGE plpgsql VOLATILE SECURITY DEFINER
  COST 100;
ALTER FUNCTION public.interval_text(interval)
  OWNER TO postgres;
COMMENT ON FUNCTION public.interval_text(interval) IS 'tt-2016-05-25 Wandelt das Eingabeintervall in eine lesbare Dauer um';
