pipeline {
    agent any
    
    // Define tools used in the pipeline
    tools {
        // Define Maven tool with version 3.6.3
        maven 'Maven'
        nodejs 'NodeJS'
        jdk 'Java'
    }
    
    environment {
        scannerHome = tool 'SonarQubeServer'
    }
    
    stages {
        stage('Login to Nexus') {
            steps {
                // Add your login step here
                // For example, using Nexus credentials
                withCredentials([usernamePassword(credentialsId: 'nexusCredential', usernameVariable: 'NEXUS_USERNAME', passwordVariable: 'NEXUS_PASSWORD')]) {
                    sh "curl -u ${NEXUS_USERNAME}:${NEXUS_PASSWORD} http://192.168.74.134:8081"
                }
            }
        }
        
        stage('Run Backend Unit Tests') {
            steps {
                dir('spring-blog-backend') {
                    sh 'mvn clean test'
                }
            }
        }

        stage('Run Frontend Unit Tests') {
            steps {
                dir('spring-blog-client') {
                    // sh 'npm run test'
                }
            }
        }
        
        stage('Build Backend') {
            steps {
                dir('spring-blog-backend') {
                    sh 'mvn clean install'
                }
            }
        }
        
        stage('Build Frontend') {
            steps {
                dir('spring-blog-client') {
                    sh 'npm install'
                    sh 'npm run build -- --configuration=production'
                }
            }
        }
        
        stage('SonarQube Code Analysis') {
            steps {
                script {
                    sh """
                        ${scannerHome}/bin/sonar-scanner \
                        -Dsonar.projectKey=Test \
                        -Dsonar.java.binaries=. \
                        -Dsonar.host.url=http://192.168.74.139:9010 \
                        -Dsonar.login=squ_1ca99673dccd74a3038f8b7c456368bba9b4d85b
                    """
                }
            }
        }
        
        stage('Deploying artifacts to Nexus') {
            steps {
                dir('spring-blog-backend') {
                    // Deploy backend artifact
                    nexusArtifactUploader(
                        artifacts: [
                            [
                                artifactId: 'spring-blog-backend',
                                classifier: '',
                                file: 'target/spring-blog-backend-0.0.1-SNAPSHOT.jar',
                                type: 'jar'
                            ]
                        ], 
                        credentialsId: 'nexusCredential',
                        groupId: 'com.github.braians',
                        nexusUrl: '192.168.74.134:8081',
                        nexusVersion: 'nexus3',
                        protocol: 'http',
                        repository: 'Mavenupload',
                        version: '0.0.1'
                    )
                }
                dir('spring-blog-client') {
                    script {
                        def NEXUS_URL = '192.168.74.134:8081'
                        def NEXUS_REPO = 'npm-public'
                        def NEXUS_CREDENTIALS_ID = 'npmCred'            
                        // Configure npm to use the Nexus registry
                        sh "npm config set registry http://192.168.74.134:8081/repository/npm-public/"
                        // Publish frontend artifacts using npm publish
                        sh "npm publish"
                    }
                }
            }
        }
    }
}
