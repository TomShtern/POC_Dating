# Infrastructure & Deployment

## Overview

Production-ready infrastructure setup for dating app using AWS, Docker, Kubernetes, and Terraform.

**Stack:**
- **Cloud**: AWS
- **Containers**: Docker
- **Orchestration**: Kubernetes (EKS)
- **IaC**: Terraform
- **CI/CD**: GitHub Actions

---

## Table of Contents

1. [Docker Setup](#docker-setup)
2. [Kubernetes Configuration](#kubernetes-configuration)
3. [Terraform Infrastructure](#terraform-infrastructure)
4. [CI/CD Pipeline](#cicd-pipeline)
5. [Monitoring & Logging](#monitoring--logging)
6. [Secrets Management](#secrets-management)

---

## Docker Setup

### Directory Structure

```
docker/
├── api/
│   └── Dockerfile
├── websocket/
│   └── Dockerfile
├── ml-service/
│   └── Dockerfile
└── docker-compose.yml
```

---

### API Service Dockerfile

```dockerfile
# docker/api/Dockerfile
FROM node:20-alpine AS builder

WORKDIR /app

# Copy package files
COPY package*.json ./
COPY tsconfig.json ./

# Install dependencies
RUN npm ci

# Copy source code
COPY src/ ./src/
COPY prisma/ ./prisma/

# Generate Prisma Client
RUN npx prisma generate

# Build TypeScript
RUN npm run build

# Production image
FROM node:20-alpine

WORKDIR /app

# Install production dependencies only
COPY package*.json ./
RUN npm ci --only=production && npm cache clean --force

# Copy built files from builder
COPY --from=builder /app/dist ./dist
COPY --from=builder /app/node_modules/.prisma ./node_modules/.prisma
COPY --from=builder /app/node_modules/@prisma ./node_modules/@prisma

# Create non-root user
RUN addgroup -g 1001 -S nodejs && \
    adduser -S nodejs -u 1001

USER nodejs

EXPOSE 3000

CMD ["node", "dist/index.js"]
```

---

### WebSocket Service Dockerfile

```dockerfile
# docker/websocket/Dockerfile
FROM node:20-alpine AS builder

WORKDIR /app

COPY package*.json ./
COPY tsconfig.json ./

RUN npm ci

COPY src/ ./src/

RUN npm run build

# Production image
FROM node:20-alpine

WORKDIR /app

COPY package*.json ./
RUN npm ci --only=production && npm cache clean --force

COPY --from=builder /app/dist ./dist

RUN addgroup -g 1001 -S nodejs && \
    adduser -S nodejs -u 1001

USER nodejs

EXPOSE 3001

CMD ["node", "dist/websocket-server.js"]
```

---

### ML Service Dockerfile (Python)

```dockerfile
# docker/ml-service/Dockerfile
FROM python:3.11-slim

WORKDIR /app

# Install dependencies
COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt

# Copy application
COPY . .

# Create non-root user
RUN useradd -m -u 1001 appuser && \
    chown -R appuser:appuser /app

USER appuser

EXPOSE 8000

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
```

---

### Docker Compose (Development)

```yaml
# docker-compose.yml
version: '3.8'

services:
  postgres:
    image: postgis/postgis:15-3.3
    environment:
      POSTGRES_DB: dating_app
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    command: redis-server --appendonly yes

  mongodb:
    image: mongo:7
    environment:
      MONGO_INITDB_ROOT_USERNAME: mongo
      MONGO_INITDB_ROOT_PASSWORD: mongo
      MONGO_INITDB_DATABASE: dating_app
    ports:
      - "27017:27017"
    volumes:
      - mongodb_data:/data/db

  api:
    build:
      context: .
      dockerfile: docker/api/Dockerfile
    ports:
      - "3000:3000"
    environment:
      DATABASE_URL: postgresql://postgres:postgres@postgres:5432/dating_app
      REDIS_HOST: redis
      REDIS_PORT: 6379
      MONGODB_URI: mongodb://mongo:mongo@mongodb:27017/dating_app
      NODE_ENV: development
    depends_on:
      - postgres
      - redis
      - mongodb
    volumes:
      - ./src:/app/src

  websocket:
    build:
      context: .
      dockerfile: docker/websocket/Dockerfile
    ports:
      - "3001:3001"
    environment:
      REDIS_HOST: redis
      MONGODB_URI: mongodb://mongo:mongo@mongodb:27017/dating_app
      NODE_ENV: development
    depends_on:
      - redis
      - mongodb
    volumes:
      - ./src:/app/src

  ml-service:
    build:
      context: ./ml-service
      dockerfile: Dockerfile
    ports:
      - "8000:8000"
    environment:
      MODEL_PATH: /app/models

volumes:
  postgres_data:
  redis_data:
  mongodb_data:
```

---

## Kubernetes Configuration

### Namespace

```yaml
# k8s/namespace.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: dating-app
```

---

### API Deployment

```yaml
# k8s/api-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: api-server
  namespace: dating-app
spec:
  replicas: 3
  selector:
    matchLabels:
      app: api-server
  template:
    metadata:
      labels:
        app: api-server
    spec:
      containers:
      - name: api
        image: <account-id>.dkr.ecr.us-east-1.amazonaws.com/dating-app-api:latest
        ports:
        - containerPort: 3000
        env:
        - name: NODE_ENV
          value: production
        - name: DATABASE_URL
          valueFrom:
            secretKeyRef:
              name: db-credentials
              key: postgres-url
        - name: REDIS_HOST
          value: redis-cluster
        - name: MONGODB_URI
          valueFrom:
            secretKeyRef:
              name: db-credentials
              key: mongodb-uri
        - name: JWT_SECRET
          valueFrom:
            secretKeyRef:
              name: app-secrets
              key: jwt-secret
        resources:
          requests:
            cpu: 500m
            memory: 512Mi
          limits:
            cpu: 1000m
            memory: 1Gi
        livenessProbe:
          httpGet:
            path: /health
            port: 3000
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /ready
            port: 3000
          initialDelaySeconds: 10
          periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: api-service
  namespace: dating-app
spec:
  selector:
    app: api-server
  ports:
  - protocol: TCP
    port: 80
    targetPort: 3000
  type: ClusterIP
```

---

### WebSocket Deployment

```yaml
# k8s/websocket-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: websocket-server
  namespace: dating-app
spec:
  replicas: 5
  selector:
    matchLabels:
      app: websocket-server
  template:
    metadata:
      labels:
        app: websocket-server
    spec:
      containers:
      - name: websocket
        image: <account-id>.dkr.ecr.us-east-1.amazonaws.com/dating-app-ws:latest
        ports:
        - containerPort: 3001
        env:
        - name: NODE_ENV
          value: production
        - name: REDIS_HOST
          value: redis-cluster
        - name: MONGODB_URI
          valueFrom:
            secretKeyRef:
              name: db-credentials
              key: mongodb-uri
        - name: JWT_SECRET
          valueFrom:
            secretKeyRef:
              name: app-secrets
              key: jwt-secret
        resources:
          requests:
            cpu: 500m
            memory: 512Mi
          limits:
            cpu: 1000m
            memory: 1Gi
        livenessProbe:
          httpGet:
            path: /health
            port: 3001
          initialDelaySeconds: 30
          periodSeconds: 10
---
apiVersion: v1
kind: Service
metadata:
  name: websocket-service
  namespace: dating-app
spec:
  selector:
    app: websocket-server
  ports:
  - protocol: TCP
    port: 80
    targetPort: 3001
  type: ClusterIP
  sessionAffinity: ClientIP # Sticky sessions for WebSocket
```

---

### Redis Cluster

```yaml
# k8s/redis-statefulset.yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: redis-cluster
  namespace: dating-app
spec:
  serviceName: redis-cluster
  replicas: 3
  selector:
    matchLabels:
      app: redis
  template:
    metadata:
      labels:
        app: redis
    spec:
      containers:
      - name: redis
        image: redis:7-alpine
        ports:
        - containerPort: 6379
        volumeMounts:
        - name: data
          mountPath: /data
        command:
        - redis-server
        - --appendonly
        - "yes"
  volumeClaimTemplates:
  - metadata:
      name: data
    spec:
      accessModes: [ "ReadWriteOnce" ]
      resources:
        requests:
          storage: 10Gi
---
apiVersion: v1
kind: Service
metadata:
  name: redis-cluster
  namespace: dating-app
spec:
  selector:
    app: redis
  ports:
  - protocol: TCP
    port: 6379
    targetPort: 6379
  clusterIP: None
```

---

### Ingress (ALB)

```yaml
# k8s/ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: dating-app-ingress
  namespace: dating-app
  annotations:
    kubernetes.io/ingress.class: alb
    alb.ingress.kubernetes.io/scheme: internet-facing
    alb.ingress.kubernetes.io/target-type: ip
    alb.ingress.kubernetes.io/certificate-arn: arn:aws:acm:us-east-1:xxx:certificate/xxx
    alb.ingress.kubernetes.io/ssl-policy: ELBSecurityPolicy-TLS-1-2-2017-01
    alb.ingress.kubernetes.io/listen-ports: '[{"HTTP": 80}, {"HTTPS": 443}]'
    alb.ingress.kubernetes.io/ssl-redirect: '443'
spec:
  rules:
  - host: api.datingapp.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: api-service
            port:
              number: 80
  - host: ws.datingapp.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: websocket-service
            port:
              number: 80
```

---

### Horizontal Pod Autoscaler

```yaml
# k8s/hpa.yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: api-hpa
  namespace: dating-app
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: api-server
  minReplicas: 3
  maxReplicas: 20
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: websocket-hpa
  namespace: dating-app
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: websocket-server
  minReplicas: 5
  maxReplicas: 30
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
```

---

## Terraform Infrastructure

### Provider Configuration

```hcl
# terraform/provider.tf
terraform {
  required_version = ">= 1.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  backend "s3" {
    bucket         = "dating-app-terraform-state"
    key            = "prod/terraform.tfstate"
    region         = "us-east-1"
    encrypt        = true
    dynamodb_table = "terraform-locks"
  }
}

provider "aws" {
  region = var.aws_region
}
```

---

### VPC

```hcl
# terraform/vpc.tf
module "vpc" {
  source  = "terraform-aws-modules/vpc/aws"
  version = "~> 5.0"

  name = "dating-app-vpc"
  cidr = "10.0.0.0/16"

  azs             = ["us-east-1a", "us-east-1b", "us-east-1c"]
  private_subnets = ["10.0.1.0/24", "10.0.2.0/24", "10.0.3.0/24"]
  public_subnets  = ["10.0.101.0/24", "10.0.102.0/24", "10.0.103.0/24"]

  enable_nat_gateway = true
  enable_vpn_gateway = false
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = {
    Environment = var.environment
    Application = "dating-app"
  }
}
```

---

### EKS Cluster

```hcl
# terraform/eks.tf
module "eks" {
  source  = "terraform-aws-modules/eks/aws"
  version = "~> 19.0"

  cluster_name    = "dating-app-cluster"
  cluster_version = "1.28"

  vpc_id     = module.vpc.vpc_id
  subnet_ids = module.vpc.private_subnets

  cluster_endpoint_public_access = true

  eks_managed_node_groups = {
    general = {
      desired_size = 3
      min_size     = 2
      max_size     = 10

      instance_types = ["t3.large"]
      capacity_type  = "ON_DEMAND"
    }

    spot = {
      desired_size = 2
      min_size     = 0
      max_size     = 5

      instance_types = ["t3.large", "t3a.large"]
      capacity_type  = "SPOT"
    }
  }

  tags = {
    Environment = var.environment
  }
}
```

---

### RDS PostgreSQL

```hcl
# terraform/rds.tf
resource "aws_db_subnet_group" "main" {
  name       = "dating-app-db-subnet"
  subnet_ids = module.vpc.private_subnets

  tags = {
    Name = "Dating App DB Subnet Group"
  }
}

resource "aws_security_group" "rds" {
  name        = "dating-app-rds-sg"
  description = "Security group for RDS PostgreSQL"
  vpc_id      = module.vpc.vpc_id

  ingress {
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [module.eks.node_security_group_id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_db_instance" "postgres" {
  identifier     = "dating-app-db"
  engine         = "postgres"
  engine_version = "15.4"
  instance_class = "db.r6g.large"

  allocated_storage     = 100
  max_allocated_storage = 500
  storage_type          = "gp3"
  storage_encrypted     = true

  db_name  = "datingapp"
  username = var.db_username
  password = var.db_password

  vpc_security_group_ids = [aws_security_group.rds.id]
  db_subnet_group_name   = aws_db_subnet_group.main.name

  multi_az               = true
  backup_retention_period = 7
  backup_window          = "03:00-04:00"
  maintenance_window     = "mon:04:00-mon:05:00"

  enabled_cloudwatch_logs_exports = ["postgresql", "upgrade"]

  deletion_protection = true
  skip_final_snapshot = false
  final_snapshot_identifier = "dating-app-final-snapshot"

  tags = {
    Name        = "dating-app-postgres"
    Environment = var.environment
  }
}
```

---

### ElastiCache Redis

```hcl
# terraform/elasticache.tf
resource "aws_elasticache_subnet_group" "main" {
  name       = "dating-app-redis-subnet"
  subnet_ids = module.vpc.private_subnets
}

resource "aws_security_group" "redis" {
  name        = "dating-app-redis-sg"
  description = "Security group for ElastiCache Redis"
  vpc_id      = module.vpc.vpc_id

  ingress {
    from_port       = 6379
    to_port         = 6379
    protocol        = "tcp"
    security_groups = [module.eks.node_security_group_id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_elasticache_replication_group" "redis" {
  replication_group_id       = "dating-app-redis"
  replication_group_description = "Dating App Redis Cluster"

  engine               = "redis"
  engine_version       = "7.0"
  node_type            = "cache.r6g.large"
  num_cache_clusters   = 3
  parameter_group_name = "default.redis7"
  port                 = 6379

  subnet_group_name  = aws_elasticache_subnet_group.main.name
  security_group_ids = [aws_security_group.redis.id]

  automatic_failover_enabled = true
  multi_az_enabled          = true

  at_rest_encryption_enabled = true
  transit_encryption_enabled = true

  snapshot_retention_limit = 5
  snapshot_window          = "03:00-05:00"

  tags = {
    Name        = "dating-app-redis"
    Environment = var.environment
  }
}
```

---

### S3 Bucket

```hcl
# terraform/s3.tf
resource "aws_s3_bucket" "media" {
  bucket = "dating-app-media-${var.environment}"

  tags = {
    Name        = "Dating App Media"
    Environment = var.environment
  }
}

resource "aws_s3_bucket_versioning" "media" {
  bucket = aws_s3_bucket.media.id

  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "media" {
  bucket = aws_s3_bucket.media.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_public_access_block" "media" {
  bucket = aws_s3_bucket.media.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_cors_configuration" "media" {
  bucket = aws_s3_bucket.media.id

  cors_rule {
    allowed_headers = ["*"]
    allowed_methods = ["GET", "PUT", "POST"]
    allowed_origins = ["https://app.datingapp.com"]
    expose_headers  = ["ETag"]
    max_age_seconds = 3000
  }
}
```

---

### CloudFront CDN

```hcl
# terraform/cloudfront.tf
resource "aws_cloudfront_origin_access_identity" "media" {
  comment = "OAI for dating app media bucket"
}

resource "aws_cloudfront_distribution" "media" {
  enabled             = true
  is_ipv6_enabled     = true
  comment             = "Dating App Media CDN"
  price_class         = "PriceClass_100"

  origin {
    domain_name = aws_s3_bucket.media.bucket_regional_domain_name
    origin_id   = "S3-${aws_s3_bucket.media.id}"

    s3_origin_config {
      origin_access_identity = aws_cloudfront_origin_access_identity.media.cloudfront_access_identity_path
    }
  }

  default_cache_behavior {
    allowed_methods  = ["GET", "HEAD", "OPTIONS"]
    cached_methods   = ["GET", "HEAD"]
    target_origin_id = "S3-${aws_s3_bucket.media.id}"

    forwarded_values {
      query_string = false
      cookies {
        forward = "none"
      }
    }

    viewer_protocol_policy = "redirect-to-https"
    min_ttl                = 0
    default_ttl            = 86400
    max_ttl                = 31536000
    compress               = true
  }

  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }

  viewer_certificate {
    cloudfront_default_certificate = true
  }

  tags = {
    Environment = var.environment
  }
}
```

---

## CI/CD Pipeline

### GitHub Actions

```yaml
# .github/workflows/deploy.yml
name: Deploy to Production

on:
  push:
    branches: [main]

env:
  AWS_REGION: us-east-1
  ECR_REGISTRY: ${{ secrets.AWS_ACCOUNT_ID }}.dkr.ecr.us-east-1.amazonaws.com

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Setup Node.js
        uses: actions/setup-node@v3
        with:
          node-version: '20'
          cache: 'npm'

      - name: Install dependencies
        run: npm ci

      - name: Run linter
        run: npm run lint

      - name: Run tests
        run: npm test

      - name: Run type check
        run: npm run type-check

  build-and-push:
    needs: test
    runs-on: ubuntu-latest
    strategy:
      matrix:
        service: [api, websocket, ml-service]
    steps:
      - uses: actions/checkout@v3

      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v2
        with:
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region: ${{ env.AWS_REGION }}

      - name: Login to Amazon ECR
        id: login-ecr
        uses: aws-actions/amazon-ecr-login@v1

      - name: Build and push Docker image
        env:
          IMAGE_TAG: ${{ github.sha }}
        run: |
          docker build -t $ECR_REGISTRY/dating-app-${{ matrix.service }}:$IMAGE_TAG \
            -f docker/${{ matrix.service }}/Dockerfile .
          docker push $ECR_REGISTRY/dating-app-${{ matrix.service }}:$IMAGE_TAG
          docker tag $ECR_REGISTRY/dating-app-${{ matrix.service }}:$IMAGE_TAG \
            $ECR_REGISTRY/dating-app-${{ matrix.service }}:latest
          docker push $ECR_REGISTRY/dating-app-${{ matrix.service }}:latest

  deploy:
    needs: build-and-push
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v2
        with:
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region: ${{ env.AWS_REGION }}

      - name: Update kubeconfig
        run: |
          aws eks update-kubeconfig --name dating-app-cluster --region $AWS_REGION

      - name: Deploy to Kubernetes
        env:
          IMAGE_TAG: ${{ github.sha }}
        run: |
          kubectl set image deployment/api-server \
            api=$ECR_REGISTRY/dating-app-api:$IMAGE_TAG \
            -n dating-app

          kubectl set image deployment/websocket-server \
            websocket=$ECR_REGISTRY/dating-app-websocket:$IMAGE_TAG \
            -n dating-app

          kubectl rollout status deployment/api-server -n dating-app
          kubectl rollout status deployment/websocket-server -n dating-app

      - name: Verify deployment
        run: |
          kubectl get pods -n dating-app
          kubectl get services -n dating-app
```

---

## Monitoring & Logging

### CloudWatch Logs

```hcl
# terraform/cloudwatch.tf
resource "aws_cloudwatch_log_group" "api" {
  name              = "/aws/eks/dating-app/api"
  retention_in_days = 30

  tags = {
    Environment = var.environment
  }
}

resource "aws_cloudwatch_log_group" "websocket" {
  name              = "/aws/eks/dating-app/websocket"
  retention_in_days = 30

  tags = {
    Environment = var.environment
  }
}
```

---

### Prometheus & Grafana (Optional)

```yaml
# k8s/monitoring/prometheus.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: monitoring
---
# Use Prometheus Operator Helm chart
# helm install prometheus prometheus-community/kube-prometheus-stack -n monitoring
```

---

## Secrets Management

### AWS Secrets Manager

```hcl
# terraform/secrets.tf
resource "aws_secretsmanager_secret" "db_credentials" {
  name = "dating-app/db-credentials"
}

resource "aws_secretsmanager_secret_version" "db_credentials" {
  secret_id = aws_secretsmanager_secret.db_credentials.id
  secret_string = jsonencode({
    postgres_url = "postgresql://${var.db_username}:${var.db_password}@${aws_db_instance.postgres.endpoint}/datingapp"
    mongodb_uri  = "mongodb://..."
  })
}
```

### Kubernetes Secrets

```yaml
# k8s/secrets.yaml
apiVersion: v1
kind: Secret
metadata:
  name: db-credentials
  namespace: dating-app
type: Opaque
stringData:
  postgres-url: postgresql://user:pass@host:5432/db
  mongodb-uri: mongodb://user:pass@host:27017/db
---
apiVersion: v1
kind: Secret
metadata:
  name: app-secrets
  namespace: dating-app
type: Opaque
stringData:
  jwt-secret: your-jwt-secret-here
  aws-access-key: your-aws-key
  aws-secret-key: your-aws-secret
```

---

## Deployment Commands

### Initial Setup

```bash
# 1. Initialize Terraform
cd terraform
terraform init
terraform plan
terraform apply

# 2. Update kubeconfig
aws eks update-kubeconfig --name dating-app-cluster --region us-east-1

# 3. Create namespace
kubectl apply -f k8s/namespace.yaml

# 4. Create secrets
kubectl apply -f k8s/secrets.yaml

# 5. Deploy Redis
kubectl apply -f k8s/redis-statefulset.yaml

# 6. Deploy API
kubectl apply -f k8s/api-deployment.yaml

# 7. Deploy WebSocket
kubectl apply -f k8s/websocket-deployment.yaml

# 8. Deploy Ingress
kubectl apply -f k8s/ingress.yaml

# 9. Apply HPA
kubectl apply -f k8s/hpa.yaml
```

### Updates

```bash
# Update API deployment
kubectl set image deployment/api-server \
  api=<account>.dkr.ecr.us-east-1.amazonaws.com/dating-app-api:v1.2.3 \
  -n dating-app

# Check rollout status
kubectl rollout status deployment/api-server -n dating-app

# Rollback if needed
kubectl rollout undo deployment/api-server -n dating-app
```

---

## Cost Optimization

### Estimated Monthly Costs (10K DAU)

| Service | Size | Cost/Month |
|---------|------|------------|
| EKS Cluster | 1 cluster | $73 |
| EC2 (EKS nodes) | 3 x t3.large | $188 |
| RDS PostgreSQL | db.r6g.large | $275 |
| ElastiCache Redis | cache.r6g.large x 3 | $448 |
| S3 | 10TB storage | $230 |
| CloudFront | 50TB transfer | $425 |
| ALB | 1 load balancer | $23 |
| **Total** | | **$1,662** |

### Cost Savings

- Use Spot Instances for non-critical workloads
- Reserved Instances for predictable workloads (40% savings)
- S3 Intelligent-Tiering for media
- CloudFront caching optimization

---

## Summary

This infrastructure provides:

✅ **High Availability**: Multi-AZ deployment
✅ **Scalability**: Auto-scaling for compute and databases
✅ **Security**: Encrypted data, private subnets, secrets management
✅ **Monitoring**: CloudWatch logs and metrics
✅ **CI/CD**: Automated testing and deployment
✅ **Cost-Effective**: Optimized resource allocation

**Next**: Deploy and test in staging environment before production launch.
