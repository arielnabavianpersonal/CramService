import * as cdk from 'aws-cdk-lib';
import * as cognito from 'aws-cdk-lib/aws-cognito';
import * as secretsmanager from 'aws-cdk-lib/aws-secretsmanager';
import * as certificatemanager from 'aws-cdk-lib/aws-certificatemanager';
import { Construct } from 'constructs';

export class CognitoStack extends cdk.Stack {
  public readonly userPool: cognito.UserPool;
  public readonly userPoolClient: cognito.UserPoolClient;

  constructor(scope: Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

    // Retrieve Google OAuth credentials from Secrets Manager
    const googleOAuthSecret = secretsmanager.Secret.fromSecretNameV2(
      this,
      'GoogleOAuthSecret',
      'cram/google-oauth-credentials'
    );

    const googleClientId = googleOAuthSecret.secretValueFromJson('clientId').unsafeUnwrap();
    const googleClientSecret = googleOAuthSecret.secretValueFromJson('clientSecret');

    // Create User Pool
    this.userPool = new cognito.UserPool(this, 'CramUserPool', {
      userPoolName: 'cram-user-pool',
      selfSignUpEnabled: true,
      signInAliases: {
        email: true,
      },
      autoVerify: {
        email: true,
      },
      standardAttributes: {
        email: {
          required: true,
          mutable: true,
        },
        givenName: {
          required: false,
          mutable: true,
        },
        familyName: {
          required: false,
          mutable: true,
        },
      },
      passwordPolicy: {
        minLength: 8,
        requireLowercase: true,
        requireUppercase: true,
        requireDigits: true,
        requireSymbols: false,
      },
      accountRecovery: cognito.AccountRecovery.EMAIL_ONLY,
      removalPolicy: cdk.RemovalPolicy.RETAIN,
    });

    // Create Google Identity Provider
    const googleProvider = new cognito.UserPoolIdentityProviderGoogle(this, 'GoogleProvider', {
      userPool: this.userPool,
      clientId: googleClientId,
      clientSecretValue: googleClientSecret,
      scopes: ['profile', 'email', 'openid'],
      attributeMapping: {
        email: cognito.ProviderAttribute.GOOGLE_EMAIL,
        givenName: cognito.ProviderAttribute.GOOGLE_GIVEN_NAME,
        familyName: cognito.ProviderAttribute.GOOGLE_FAMILY_NAME,
      },
    });

    // Create User Pool Client
    this.userPoolClient = new cognito.UserPoolClient(this, 'CramUserPoolClient', {
      userPool: this.userPool,
      userPoolClientName: 'cram-web-client',
      generateSecret: false,
      authFlows: {
        userSrp: true,
      },
      oAuth: {
        flows: {
          authorizationCodeGrant: true,
        },
        scopes: [
          cognito.OAuthScope.EMAIL,
          cognito.OAuthScope.OPENID,
          cognito.OAuthScope.PROFILE,
        ],
        callbackUrls: [
          'http://localhost:3000/callback',
          'https://cram-ai.com/callback',
        ],
        logoutUrls: [
          'http://localhost:3000',
          'https://cram-ai.com',
        ],
      },
      supportedIdentityProviders: [
        cognito.UserPoolClientIdentityProvider.GOOGLE,
        cognito.UserPoolClientIdentityProvider.COGNITO,
      ],
    });

    // Ensure the Google provider is created before the client
    this.userPoolClient.node.addDependency(googleProvider);

    // Import the ACM certificate for the custom domain
    const certificate = certificatemanager.Certificate.fromCertificateArn(
      this,
      'CognitoDomainCertificate',
      'arn:aws:acm:us-east-1:529088301024:certificate/15f7dc88-718b-463e-a517-bcc9feb27e1c'
    );

    // Create Custom Domain for User Pool with Managed Login
    this.userPool.addDomain('CramUserPoolCustomDomain', {
      customDomain: {
        domainName: 'login.cram-ai.com',
        certificate: certificate,
      }
    });
  }
}
