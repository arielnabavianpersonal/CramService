#!/usr/bin/env node
import 'source-map-support/register';
import * as cdk from 'aws-cdk-lib';
import { ApiServiceStack } from '../lib/api-service-stack';
import { CognitoStack } from '../lib/cognito-stack';

const app = new cdk.App();

const env = {
  account: process.env.CDK_DEFAULT_ACCOUNT,
  region: process.env.CDK_DEFAULT_REGION,
};

const cognitoStack = new CognitoStack(app, 'CramCognitoStack', { env });

// Create API Service Stack with Cognito integration
new ApiServiceStack(app, 'ApiServiceStack', {
  env,
  userPool: cognitoStack.userPool,
});
