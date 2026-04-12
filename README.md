# Getting Started


# Build and deployment

### Env
CHAT_IMAGE_VERSION="0.1.5"
CHAT_IMAGE_REGISTRY="europe-central2-docker.pkg.dev/pomeranian-463011/pomeranian-backend-chat/pomeranian-backend-chat"

### Build (with MacOs caffeinate during the build)
caffeinate -i docker build --platform linux/amd64 -t ${CHAT_IMAGE_REGISTRY}:latest -t ${CHAT_IMAGE_REGISTRY}:${CHAT_IMAGE_VERSION} .

### Authenticate (only initially)
gcloud auth configure-docker europe-central2-docker.pkg.dev

### Push
docker push ${CHAT_IMAGE_REGISTRY}:latest
docker push ${CHAT_IMAGE_REGISTRY}:${CHAT_IMAGE_VERSION}
