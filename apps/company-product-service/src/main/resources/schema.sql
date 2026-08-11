CREATE UNIQUE INDEX IF NOT EXISTS idx_company_name_active
ON p_companies (name)
WHERE deleted_at IS NULL;