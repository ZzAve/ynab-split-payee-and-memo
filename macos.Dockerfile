# Run stage
FROM debian:bookworm-slim
RUN apt-get update && apt-get install -y ca-certificates && rm -rf /var/lib/apt/lists/*
RUN ls -l
#COPY ./build/bin/linuxX64/releaseExecutable /app
COPY ./build/bin/linuxX64/debugExecutable /app
RUN ls -l /app
RUN #ls -l /app/ynab-split-payee

RUN chmod +x /app/ynab-split-payee-and-memo.kexe
ENTRYPOINT ["/app/ynab-split-payee-and-memo.kexe"]
