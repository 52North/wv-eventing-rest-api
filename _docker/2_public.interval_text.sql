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
