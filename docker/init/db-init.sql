-- Cria o role e o banco caso não existam.
-- Docker Compose : executado automaticamente na primeira subida do container
--                  (montado em /docker-entrypoint-initdb.d/).
-- Localhost       : psql -U postgres -h localhost -f ./docker/db-init.sql

DO $$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'saas') THEN
    CREATE ROLE saas LOGIN PASSWORD 'saas123';
  END IF;
END $$;

SELECT 'CREATE DATABASE permissions_saas OWNER saas'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'permissions_saas')
\gexec

GRANT ALL PRIVILEGES ON DATABASE permissions_saas TO saas;

\c permissions_saas

GRANT ALL ON SCHEMA public TO saas;
