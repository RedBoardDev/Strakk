JAVA_HOME ?= /opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export JAVA_HOME

.PHONY: lint lint-kotlin lint-swift lint-deno \
        test test-shared \
        build build-android build-android-release build-ios-framework \
        ios-project ios-build \
        deploy-functions migrate seed \
        setup check

# ── Lint ──────────────────────────────────────────────────────

lint: lint-kotlin lint-swift lint-deno

lint-kotlin:
	./gradlew detektAll --daemon

lint-swift:
	cd iosApp && swiftlint lint --strict

lint-deno:
	@command -v deno >/dev/null 2>&1 || { echo "deno not installed — run: brew install deno"; exit 1; }
	deno lint supabase/functions/
	deno check supabase/functions/*/index.ts supabase/functions/_shared/*.ts

# ── Test ──────────────────────────────────────────────────────

test: test-shared

test-shared:
	./gradlew :shared:allTests --daemon

# ── Build ─────────────────────────────────────────────────────

build: build-android

build-android:
	./gradlew :androidApp:assembleDebug --daemon

build-android-release:
	./gradlew :androidApp:assembleRelease --daemon

build-ios-framework:
	./gradlew :shared:linkDebugFrameworkIosSimulatorArm64 --daemon

# ── iOS ───────────────────────────────────────────────────────

ios-project:
	cd iosApp && xcodegen generate

ios-build: ios-project
	cd iosApp && xcodebuild build \
		-project Strakk.xcodeproj \
		-scheme Strakk \
		-sdk iphonesimulator \
		-arch arm64 \
		-configuration Debug \
		CODE_SIGNING_ALLOWED=NO SKIP_INSTALL=YES \
		-quiet

# ── Supabase ─────────────────────────────────────────────────

deploy-functions:
	@for dir in supabase/functions/*/; do \
		fn=$$(basename "$$dir"); \
		if [ "$$fn" != "_shared" ]; then \
			echo "Deploying $$fn..."; \
			supabase functions deploy "$$fn" --no-verify-jwt; \
		fi; \
	done

migrate:
	supabase db push

seed:
	supabase db reset --linked

# ── Setup ─────────────────────────────────────────────────────

setup:
	@command -v lefthook >/dev/null 2>&1 || { echo "Installing lefthook..."; brew install lefthook; }
	lefthook install
	@echo "Warming Gradle daemon..."
	./gradlew --daemon --quiet
	@echo "Setup complete. Pre-commit hooks are active."

# ── Full check ────────────────────────────────────────────────

check: lint test build
	@echo "All checks passed."
