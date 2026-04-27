# Getting Started

# Build and deployment

### Env

```sh
CHAT_IMAGE_VERSION="0.2.1"
CHAT_IMAGE_REGISTRY="europe-central2-docker.pkg.dev/pomeranian-463011/pomeranian-backend-chat/pomeranian-backend-chat"
```

### Build (with MacOs caffeinate during the build)

```sh
caffeinate -i docker build --platform linux/amd64 -t ${CHAT_IMAGE_REGISTRY}:latest -t ${CHAT_IMAGE_REGISTRY}:${CHAT_IMAGE_VERSION} .
```                                                                 

### Authenticate (only initially)

```sh
gcloud auth configure-docker europe-central2-docker.pkg.dev
```

### Push

```sh
caffeinate -i docker push ${CHAT_IMAGE_REGISTRY}:latest
docker push ${CHAT_IMAGE_REGISTRY}:${CHAT_IMAGE_VERSION}
```

# Fun tools

count code lines:
```sh
./count-java-lines.sh
```