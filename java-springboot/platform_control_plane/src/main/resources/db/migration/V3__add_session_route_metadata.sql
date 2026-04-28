ALTER TABLE platform_sessions
    ADD COLUMN route_gateway_host VARCHAR(255);

ALTER TABLE platform_sessions
    ADD COLUMN route_service_name VARCHAR(255);

ALTER TABLE platform_sessions
    ADD COLUMN route_service_namespace VARCHAR(255);

ALTER TABLE platform_sessions
    ADD COLUMN route_upstream_base_url VARCHAR(1024);
