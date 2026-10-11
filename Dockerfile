# syntax=docker/dockerfile:1

FROM ubuntu:20.04 AS native-builder
ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update && apt-get install -y --no-install-recommends \
    ca-certificates \
    build-essential \
    git \
    curl

# Add Kitware repo for newer CMake version
RUN curl -s https://apt.kitware.com/kitware-archive.sh | bash -s
RUN apt-get update && apt-get install -y --no-install-recommends cmake

RUN rm -rf /var/lib/apt/lists/*

WORKDIR /workspace

COPY .git .git
COPY .gitmodules .gitmodules
COPY CMakeLists.txt .
COPY pom.xml .
COPY build_linux.sh .
COPY src src

ARG TARGETARCH

RUN TARGETARCH=${TARGETARCH} ./build_linux.sh

# Stage 2: Export binaries
FROM scratch AS export
COPY --from=native-builder /workspace/src/main/resources/native/linux-*/libparakeet.so /
