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
