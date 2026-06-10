# Kubernetes Troubleshooting Guide

## Pod Not Starting

**Symptom:** Pod stuck in `Pending`, `CrashLoopBackOff`, or `ImagePullBackOff`.

### ImagePullBackOff

```bash
kubectl describe pod <pod-name>
```

Look at `Events` section. Common causes:
- Image name/tag wrong in `deployment.yaml`
- Registry credentials not configured

Fix:
```bash
kubectl create secret docker-registry regcred \
  --docker-server=<registry-url> \
  --docker-username=<username> \
  --docker-password=<password>
```

Then add to deployment spec:
```yaml
imagePullSecrets:
  - name: regcred
```

### CrashLoopBackOff

Check application logs:
```bash
kubectl logs deployment/ncba-app --previous
```

Common causes:
- Database connection failure — check secret values and MySQL is running
- Missing environment variable — check configmap/secret refs in deployment

---

## Application Cannot Connect to MySQL

**Symptom:** `Communications link failure` or `Access denied` in logs.

1. Verify MySQL pod is running:
   ```bash
   kubectl get pods -l app=mysql
   ```

2. Check the secret URL points to `mysql-service:3306`:
   ```bash
   kubectl get secret ncba-secret -o jsonpath='{.data.SPRING_DATASOURCE_URL}' | base64 -d
   ```

3. Test connectivity from the app pod:
   ```bash
   kubectl exec -it deployment/ncba-app -- sh
   # inside pod:
   curl -v telnet://mysql-service:3306
   ```

4. Verify MySQL service is running:
   ```bash
   kubectl get svc mysql-service
   ```

---

## Liveness/Readiness Probe Failing

**Symptom:** Pod restarts repeatedly, `Readiness probe failed`.

1. Check what the probe returns:
   ```bash
   kubectl exec -it deployment/ncba-app -- sh
   curl http://localhost:8080/actuator/health
   ```

2. If the app is slow to start, increase `initialDelaySeconds` in `deployment.yaml`:
   ```yaml
   livenessProbe:
     initialDelaySeconds: 60
   ```

3. Check application logs for startup errors:
   ```bash
   kubectl logs deployment/ncba-app
   ```

---

## HPA Not Scaling

**Symptom:** HPA shows `<unknown>` for current CPU or does not scale.

1. Check metrics-server is installed:
   ```bash
   kubectl top nodes
   kubectl top pods
   ```

2. If `metrics-server` is missing, install it:
   ```bash
   kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
   ```

3. Check HPA status:
   ```bash
   kubectl describe hpa ncba-hpa
   ```

---

## PVC Not Binding

**Symptom:** MySQL pod stuck in `Pending`, PVC in `Pending` state.

1. Check PVC status:
   ```bash
   kubectl get pvc mysql-pvc
   kubectl describe pvc mysql-pvc
   ```

2. Ensure your cluster has a default StorageClass:
   ```bash
   kubectl get storageclass
   ```

3. If no default StorageClass, add one or specify it explicitly in the PVC:
   ```yaml
   storageClassName: standard
   ```

---

## Viewing Logs

```bash
# All pods in deployment
kubectl logs deployment/ncba-app

# Specific pod
kubectl logs <pod-name>

# Follow logs
kubectl logs -f deployment/ncba-app

# Previous crashed container
kubectl logs deployment/ncba-app --previous
```

---

## Describing Resources

```bash
kubectl describe deployment ncba-app
kubectl describe pod <pod-name>
kubectl describe service ncba-service
kubectl describe statefulset mysql
```

---

## Exec Into a Pod

```bash
kubectl exec -it deployment/ncba-app -- sh
```

Useful for checking environment variables, network connectivity, or filesystem.

---

## Restarting Pods

```bash
kubectl rollout restart deployment/ncba-app
```
