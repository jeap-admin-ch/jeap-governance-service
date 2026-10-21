-- The Message Contract Service reports contracts whose latest version cannot be determined as UNKNOWN, without a
-- latest version. They must be distinguishable from outdated contracts, so the state replaces the up_to_date flag.
-- The table is a snapshot that every import replaces completely, so the backfill only covers the time until then.
ALTER TABLE mc_version_status ADD COLUMN state VARCHAR;
UPDATE mc_version_status SET state = CASE WHEN up_to_date THEN 'UP_TO_DATE' ELSE 'OUTDATED' END;
ALTER TABLE mc_version_status ALTER COLUMN state SET NOT NULL;
ALTER TABLE mc_version_status ALTER COLUMN latest_version DROP NOT NULL;
DROP INDEX mc_version_status_app_outdated;
ALTER TABLE mc_version_status DROP COLUMN up_to_date;
CREATE INDEX mc_version_status_app_state ON mc_version_status (app_name, state);
