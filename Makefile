SERVICE_TARGET := maven
HYBRIS_FOLDER := /opt/jboss/keycloak

# all our targets are phony (no files to check).
.PHONY: help build package

# suppress makes own output
#.SILENT:

help:
	@echo 'Usage: make [TARGET]                                             '

build:
	docker-compose build --pull

package: build
	$(eval DOCKER_CONTAINER := $(shell docker create nicolabeghin/keycloak-multiple-ds-user-storage:latest))
	docker cp ${DOCKER_CONTAINER}:/app/target/multiple-ds-user-storage.jar .
	docker rm ${DOCKER_CONTAINER}
	docker image rm nicolabeghin/keycloak-multiple-ds-user-storage:latest
