# Makefile: sentinel

SERVICE_NAME=$(shell cat Datawirefile | python -c 'import sys, json; print json.load(sys.stdin)["service"]["name"]')
SERVICE_VERSION=$(shell cat Datawirefile | python -c 'import sys, json; print json.load(sys.stdin)["service"]["version"]')

DOCKER_REGISTRY_HOST=docker.io
DOCKER_REPO=datawire/$(SERVICE_NAME)

.PHONY: all

all: clean build

build:
	./gradlew test shadowJar

clean:
	./gradlew clean

compile:
	./gradlew build

docker-build:
	docker build -t $(DOCKER_REPO):$(SERVICE_VERSION) .

docker-sh:
	( \
		docker run -it --rm \
		--name $(SERVICE_NAME)-shell \
		-e DATAWIRE_TOKEN=$(DATAWIRE_TOKEN) \
		--entrypoint /bin/sh \
		-p 5000:5000 \
		$(DOCKER_REPO):$(SERVICE_VERSION) \
	)

docker-run: docker-build
	( \
		docker run \
		-it \
		--rm \
		--name $(SERVICE_NAME)-$(SERVICE_VERSION) \
		-e DATAWIRE_TOKEN=$(DATAWIRE_TOKEN) \
		-e MDK_SERVICE_NAME=$(SERVICE_NAME) \
		-e MDK_SERVICE_VERSION=$(SERVICE_VERSION) \
		-e DATAWIRE_ROUTABLE_HOST=127.0.0.1 \
		-e DATAWIRE_ROUTABLE_PORT=5000 \
		-p 5000:5000 \
		-v $$(pwd)/config:/opt/sentinel/config \
		$(DOCKER_REPO):$(SERVICE_VERSION) \
	)

docker-push: docker-build
    docker push $(DOCKER_REPO):$(SERVICE_VERSION)

test:
	./gradlew test

unit-test:
	./gradlew test

version:
	@echo SERVICE_VERSION

service-name:
	@echo SERVICE_NAME