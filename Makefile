# Makefile: hello-mobius

SERVICE_NAME=$(shell cat Datawirefile | python -c 'import sys, json; print json.load(sys.stdin)["service"]["name"]')
SERVICE_VERSION=$(shell cat Datawirefile | python -c 'import sys, json; print json.load(sys.stdin)["service"]["version"]')
QUARK_REQUIREMENTS=$(shell sed -e '/^[[:space:]]*$$/d' -e '/^[[:space:]]*\#/d' requirements-quark.txt | tr '\n' ' ' )

DOCKER_REGISTRY_HOST=$(shell cat Datawirefile | python -c 'import sys, json; print json.load(sys.stdin)["docker"]["registryAddress"]')
DOCKER_REPO=datawireio/sentinel

.PHONY: all

all: clean build

build:
	# Produces a language build artifact (e.g.: .jar, .whl, .gem). Alternatively a GZIP tarball
	# can be provided if more appropriate.
	:
 
docker:
	# Produces a Docker image.
	docker build \
	    --build-arg SERVICE_VERSION=$(SERVICE_VERSION) \
	    -t $(DOCKER_REPO):$(SERVICE_VERSION) \
	    .

docker-bash:
	docker run -i -t --entrypoint /bin/bash $(DOCKER_REPO):$(SERVICE_VERSION)
 
clean:
	# Clean previous build outputs (e.g. class files) and temporary files. Customize as needed.
	:
 
compile:
	# Compile code (may do nothing for interpreted languages).
	:

quark-install:
	# Compiles AND installs Quark language sources if there are any.
	~/.quark/bin/quark install --python $(QUARK_REQUIREMENTS)

quark-install-venv: venv
	# Compiles AND installs Quark language sources if there are any.
	( \
		. venv/bin/activate; \
		~/.quark/bin/quark install --python $(QUARK_REQUIREMENTS); \
	)

publish: docker
	docker tag $(DOCKER_REPO):$(SERVICE_VERSION) $(DOCKER_REGISTRY_HOST)/$(DOCKER_REPO):$(SERVICE_VERSION)
	docker push $(DOCKER_REGISTRY_HOST)/$(DOCKER_REPO):$(SERVICE_VERSION)

publish-no-build:
	docker tag $(DOCKER_REPO):$(SERVICE_VERSION) $(DOCKER_REGISTRY_HOST)/$(DOCKER_REPO):$(SERVICE_VERSION)
	docker push $(DOCKER_REGISTRY_HOST)/$(DOCKER_REPO):$(SERVICE_VERSION)

run-dev: venv
	# Run the service or application in development mode.
	venv/bin/python service/service.py

run-docker: docker run-docker-no-rebuild
	:

run-docker-no-rebuild:
	# Run the service or application in production mode.
	docker run -it --rm --name datawire-sentinel \
	    -e ENV=develop \
		-e DATAWIRE_ROUTABLE_HOST=127.0.0.1 \
		-e DATAWIRE_ROUTABLE_PORT=5000 \
		-e DATAWIRE_TOKEN=$(DATAWIRE_TOKEN) \
		-e MDK_SERVICE_NAME=$(SERVICE_NAME) \
		-e MDK_SERVICE_VERSION=$(SERVICE_VERSION) \
		-v $(shell pwd)/sentinel-web/config:/opt/sentinel/config \
		-p 5000:5000 \
		$(DOCKER_REPO):$(SERVICE_VERSION)

test: venv
	# Run the full test suite.

unit-test: venv
	# Run only the unit tests.

version:
	@echo $(SERVICE_VERSION)

service-name:
	@echo $(SERVICE_VERSION)