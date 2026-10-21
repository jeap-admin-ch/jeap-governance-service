-- The Message Contract Service reports contracts whose latest version cannot be determined as UNKNOWN, without a
-- latest version. They must be distinguishable from outdated contracts.
ALTER TABLE mc_version_status ADD COLUMN state VARCHAR;
UPDATE mc_version_status SET state = CASE WHEN up_to_date THEN 'UP_TO_DATE' ELSE 'OUTDATED' END;
ALTER TABLE mc_version_status ALTER COLUMN state SET NOT NULL;
ALTER TABLE mc_version_status ALTER COLUMN latest_version DROP NOT NULL;
