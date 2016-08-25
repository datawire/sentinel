# Makefile: hello-mobius

VERSION=$(shell cat VERSION)
QUARK_REQUIREMENTS=$(shell sed -e '/^[[:space:]]*$$/d' -e '/^[[:space:]]*\#/d' requirements-quark.txt | tr '\n' ' ' )

DOCKER_REGISTRY_HOST="us.gcr.io"
DOCKER_REPO="datawireio/hello-mobius"

.PHONY: all

all: clean build

build:
	# Produces a language build artifact (e.g.: .jar, .whl, .gem). Alternatively a GZIP tarball
	# can be provided if more appropriate.
	:
 
docker:
	# Produces a Docker image.
	docker build -t $(DOCKER_REPO):$(VERSION) .

docker-bash:
	docker run -i -t --entrypoint /bin/bash $(DOCKER_REPO):$(VERSION)
 
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
	docker tag $(DOCKER_REPO):$(VERSION) $(DOCKER_REGISTRY_HOST)/$(DOCKER_REPO):$(VERSION)
	gcloud docker push $(DOCKER_REGISTRY_HOST)/$(DOCKER_REPO):$(VERSION)

publish-no-build:
	docker tag $(DOCKER_REPO):$(VERSION) $(DOCKER_REGISTRY_HOST)/$(DOCKER_REPO):$(VERSION)
	gcloud docker push $(DOCKER_REGISTRY_HOST)/$(DOCKER_REPO):$(VERSION)

run-dev: venv
	# Run the service or application in development mode.
	venv/bin/python service/service.py

run-docker: docker
	# Run the service or application in production mode.
	docker run --rm --name datawire-hello-mobius -e DATAWIRE_ROUTABLE_HOST=127.0.0.1 -e DATAWIRE_TOKEN=$(DATAWIRE_TOKEN) -it -p 5000:5000 $(DOCKER_REPO):$(VERSION)

run-docker-no-rebuild:
	# Run the service or application in production mode.
	docker run --rm --name datawire-hello-mobius -e DATAWIRE_ROUTABLE_HOST=127.0.0.1 -e DATAWIRE_TOKEN=$(DATAWIRE_TOKEN) -it -p 5000:5000 $(DOCKER_REPO):$(VERSION)

test: venv
	# Run the full test suite.

unit-test: venv
	# Run only the unit tests.

version:
	@echo $(VERSION)

# Python virtualenv automatic setup. Ensures that targets relying on the virtualenv always have an updated python to
# use.
#
# This is intended for developer convenience. Do not attempt to make venv in a Docker container or use a virtualenv in
# docker container because you will be going down into a world of darkness.

venv: venv/bin/activate

venv/bin/activate: requirements.txt
	test -d venv || virtualenv venv
	venv/bin/pip install -Ur requirements.txt
	touch venv/bin/activate
