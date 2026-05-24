# Getting Started

# Build and deployment

### Env

```sh
CHAT_IMAGE_VERSION="0.3.0"
CHAT_IMAGE_REGISTRY="europe-west1-docker.pkg.dev/kwink3r/pomerania-backend-chat/20260524"
```

### Build (with MacOs caffeinate during the build)

```sh
caffeinate -i docker build --platform linux/amd64 -t ${CHAT_IMAGE_REGISTRY}:latest -t ${CHAT_IMAGE_REGISTRY}:${CHAT_IMAGE_VERSION} .
```                                                                 

### Authenticate (only initially)

```sh
gcloud auth configure-docker europe-west1-docker.pkg.dev
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