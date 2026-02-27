.PHONY: *


docker_image_name = zzave/ynab-split-payee
docker_image_version := $(shell ./gradlew -q printVersion)
CURRENT_DIR := $(shell pwd)

build:
	./gradlew build

test:
ifdef TEST
	./gradlew test --tests "$(TEST)"
else
	./gradlew test
endif

yolo:
	./gradlew build -x test

clean:
	./gradlew clean

docker:
	docker build --build-arg APP_VERSION=${docker_image_version} -t ${docker_image_name}:${docker_image_version} .

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
