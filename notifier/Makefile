.PHONY: build test typecheck docker clean

build: ## Build TypeScript
	npm run build

test: ## Run tests
	npm test

typecheck: ## Type-check without emitting
	npm run typecheck

docker: ## Build Docker image
	docker build -t ynab-notifier .

clean: ## Remove build artifacts
	rm -rf dist
