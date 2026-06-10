# Kubernetes Deployment Guide

## Prerequisites

- kubectl configured and pointing to your cluster
- Docker image built and pushed to a registry accessible by the cluster
- MySQL StatefulSet deployed (or an external MySQL instance)

---

## Step 1: Build and Push the Docker Image

```bash
# Build the image
docker build -t ncba-country-service:latest .

# Tag for your registry (replace with your registry URL)
docker tag ncba-country-service:latest your-registry/ncba-country-service:1.0.0

# Push
docker push your-registry/ncba-country-service:1.0.0
```

Update `k8s/deployment.yaml` image field to match the pushed tag.

---

## Step 2: Deploy MySQL

```bash
kubectl apply -f k8s/mysql-deployment.yaml
```

Wait for MySQL to be ready:

```bash
kubectl rollout status statefulset/mysql
```

---

## Step 3: Create ConfigMap and Secret

```bash
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secret.yaml
```

To verify:

```bash
kubectl get configmap ncba-config -o yaml
kubectl get secret ncba-secret -o yaml
```

---

## Step 4: Deploy the Application

```bash
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
```

Check rollout status:

```bash
kubectl rollout status deployment/ncba-app
```

---

## Step 5: Deploy HPA

```bash
kubectl apply -f k8s/hpa.yaml
```

Verify HPA:

```bash
kubectl get hpa ncba-hpa
```

---

## Step 6: Verify All Resources

```bash
kubectl get all -n default
```

Expected output includes:
- `deployment.apps/ncba-app` with 2/2 ready
- `statefulset.apps/mysql` with 1/1 ready
- `service/ncba-service` ClusterIP
- `service/mysql-service` ClusterIP
- `horizontalpodautoscaler.autoscaling/ncba-hpa`

---

## Step 7: Access the Application

The service is ClusterIP, so it is only accessible within the cluster. To test locally:

```bash
kubectl port-forward service/ncba-service 8080:80
```

Then access `http://localhost:8080/actuator/health`.

For production exposure, add an Ingress or change the service type to LoadBalancer.

---

## Updating the Application

```bash
# Update image in deployment
kubectl set image deployment/ncba-app ncba-app=your-registry/ncba-country-service:1.1.0

# Monitor rollout
kubectl rollout status deployment/ncba-app

# Rollback if needed
kubectl rollout undo deployment/ncba-app
```

---

## Cleanup

```bash
kubectl delete -f k8s/
```
