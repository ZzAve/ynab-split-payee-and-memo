# Run stage
FROM debian:bookworm-slim
RUN apt-get update && apt-get install -y ca-certificates && rm -rf /var/lib/apt/lists/*
#COPY ./build/bin/linuxX64/releaseExecutable/ynab-split-payee-and-memo.kexe /app/ynab-split-payee
COPY ./build/bin/linuxX64/debugExecutable/ynab-split-payee-and-memo.kexe /app/ynab-split-payee
RUN chmod +x /app/ynab-split-payee
ENTRYPOINT ["/app/ynab-split-payee"]
