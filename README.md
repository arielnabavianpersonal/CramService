# Java Lambda API Service

Quick deployment service with separate infrastructure and service deployments.

## Structure
- `infrastructure/` - CDK infrastructure code
- `service/` - Java Lambda service code
- `.github/workflows/` - CI/CD pipelines

## Deployment
- Push to `master` branch deploys both infrastructure and service
- Infrastructure changes: modify files in `infrastructure/`
- Service changes: modify files in `service/`

## Local Development
```bash
# Build service
cd service && mvn clean package

# Deploy infrastructure
cd infrastructure && npm install && cdk deploy

# Deploy service only
cd infrastructure && cdk deploy --hotswap
```
