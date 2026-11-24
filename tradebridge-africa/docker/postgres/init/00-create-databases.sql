-- TradeBridge Africa — create the four service databases.
-- Runs once on first postgres container startup (docker-entrypoint-initdb.d).

CREATE DATABASE tb_identity_db;
CREATE DATABASE tb_country_db;
CREATE DATABASE tb_notification_db;
CREATE DATABASE tb_customer_db;

-- Ownership is inherited from POSTGRES_USER (tradebridge).
GRANT ALL PRIVILEGES ON DATABASE tb_identity_db TO tradebridge;
GRANT ALL PRIVILEGES ON DATABASE tb_country_db TO tradebridge;
GRANT ALL PRIVILEGES ON DATABASE tb_notification_db TO tradebridge;
GRANT ALL PRIVILEGES ON DATABASE tb_customer_db TO tradebridge;
