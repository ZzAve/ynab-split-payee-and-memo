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

e2e-test:
	./gradlew integrationTest

yolo:
	./gradlew build -x test

clean:
	./gradlew clean

docker:
	docker build --build-arg APP_VERSION=${docker_image_version} -t ${docker_image_name}:${docker_image_version} .

native:
	./gradlew nativeCompile --no-configuration-cache

native-test: native
	./build/native/nativeCompile/ynab-split-payee --help

docker-native:
	docker build -f Dockerfile.native -t ${docker_image_name}:${docker_image_version}-native .

run: docker
	docker run \
	-v "${CURRENT_DIR}/logs:/app/logs" \
	--env-file .env \
	--rm --name ynab-updater \
	${docker_image_name}:${docker_image_version}

dry-run: docker-native
	docker run \
		--env-file .env \
		--rm --name ynab-updater \
		${docker_image_name}:${docker_image_version}-native --dry-run



#	 This outputs any command in the Makefile. With a short description taken from a ## prefixed command after the command (preferred) or the line above
#	 ## build the project
#	 build:
#    	<build command>
#
#    yolo: ## quick build of the project - with as little validation as possible
#    	<yolo command>
#
help: ## Show this help

	@echo "Usage: make <command>"; \
	echo ""; \
	desc=""; \
	while IFS= read -r line; do \
		case "$$line" in \
			'## '*)              desc="$${line#\#\# }" ;; \
			[a-zA-Z_-]*:[[:space:]]*'## '*) printf '\033[36m%-20s\033[0m %s\n' "$${line%%:*}" "$${line#*\#\# }"; desc="" ;; \
			[a-zA-Z_-]*:|[a-zA-Z_-]*:[[:space:]]*) printf '\033[36m%-20s\033[0m %s\n' "$${line%%:*}" "$$desc"; desc="" ;; \
			*)                   desc="" ;; \
		esac; \
	done < $(MAKEFILE_LIST) | sort
