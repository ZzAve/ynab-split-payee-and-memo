.PHONY: *


docker_image_name = zzave/ynab-split-payee
docker_image_version = 0-SNAPSHOT
CURRENT_DIR := $(shell pwd)

build:
	./gradlew build

docker: build
	docker build -t ${docker_image_name}:${docker_image_version} .

run: docker
	docker run \
	-v "${CURRENT_DIR}/logs:/app/logs" \
	--env-file .env \
	--rm --name ynab-updater \
	${docker_image_name}:${docker_image_version}

dry-run: docker
	docker run \
		-v "${CURRENT_DIR}/logs:/app/logs" \
		-e YNAB_LOG=CLI \
		--env-file .env \
		--rm --name ynab-updater \
		${docker_image_name}:${docker_image_version} --dry-run
