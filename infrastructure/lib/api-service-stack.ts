import * as cdk from 'aws-cdk-lib';
import * as lambda from 'aws-cdk-lib/aws-lambda';
import * as apigateway from 'aws-cdk-lib/aws-apigateway';
import * as cognito from 'aws-cdk-lib/aws-cognito';
import { Construct } from 'constructs';

export interface ApiServiceStackProps extends cdk.StackProps {
  userPool: cognito.UserPool;
}

export class ApiServiceStack extends cdk.Stack {
  constructor(scope: Construct, id: string, props: ApiServiceStackProps) {
    super(scope, id, props);

    const apiLambda = new lambda.Function(this, 'ApiHandler', {
      runtime: lambda.Runtime.JAVA_17,
      handler: 'com.example.ApiHandler::handleRequest',
      code: lambda.Code.fromAsset('../service/target/lambda-service-1.0.0.jar'),
      memorySize: 512,
      timeout: cdk.Duration.seconds(30),
    });

    const api = new apigateway.RestApi(this, 'ApiGateway', {
      restApiName: 'Service API',
      description: 'API Gateway for Lambda service',
      deployOptions: {
        stageName: 'prod',
      },
      defaultCorsPreflightOptions: {
        allowOrigins: apigateway.Cors.ALL_ORIGINS,
        allowMethods: apigateway.Cors.ALL_METHODS,
        allowHeaders: ['Content-Type', 'Authorization'],
      },
    });

    // Create Cognito Authorizer
    const authorizer = new apigateway.CognitoUserPoolsAuthorizer(this, 'CognitoAuthorizer', {
      cognitoUserPools: [props.userPool],
      authorizerName: 'CramCognitoAuthorizer',
      identitySource: 'method.request.header.Authorization',
    });

    const integration = new apigateway.LambdaIntegration(apiLambda);
    
    // Add proxy for all paths
    api.root.addProxy({
      defaultIntegration: integration,
      anyMethod: true,
      defaultMethodOptions: {
        authorizationType: apigateway.AuthorizationType.COGNITO,
        authorizer: authorizer,
      },
    });

    // Secure the root path as well
    api.root.addMethod('ANY', integration, {
      authorizationType: apigateway.AuthorizationType.COGNITO,
      authorizer: authorizer,
    });
  }
}
