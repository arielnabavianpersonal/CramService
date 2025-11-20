import * as cdk from 'aws-cdk-lib';
import * as lambda from 'aws-cdk-lib/aws-lambda';
import * as apigateway from 'aws-cdk-lib/aws-apigateway';
import { Construct } from 'constructs';

export class ApiServiceStack extends cdk.Stack {
  constructor(scope: Construct, id: string, props?: cdk.StackProps) {
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
    });

    const integration = new apigateway.LambdaIntegration(apiLambda);
    api.root.addProxy({
      defaultIntegration: integration,
      anyMethod: true,
      defaultMethodOptions: {
        authorizationType: apigateway.AuthorizationType.IAM,
      },
    });
  }
}
