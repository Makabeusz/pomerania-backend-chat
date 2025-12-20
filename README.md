# Getting Started


# Build and deployment

### Build
docker build --platform linux/amd64 -t pomeranian-backend-chat:latest .

### Tag for Artifact Registry (replace placeholders)
docker tag pomeranian-backend-chat:latest europe-central2-docker.pkg.dev/pomeranian-463011/pomeranian-backend-chat/pomeranian-backend-chat:latest

### Authenticate (only initially)
gcloud auth configure-docker europe-west1-docker.pkg.dev

### Push
docker push europe-central2-docker.pkg.dev/pomeranian-463011/pomeranian-backend-chat/pomeranian-backend-chat:latest


