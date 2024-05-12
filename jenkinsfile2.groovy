pipeline {
    agent any
    
    tools {
        nodejs 'NodeJS'
        jdk 'Java'
    } 
    
    environment {
        scannerHome = tool 'SonarQubeServer'
    }
    
    stages {
        stage('Run Frontend Unit Tests') {
            steps {
                dir('spring-blog-client') {
                    // sh 'npm run test'
                }
            }
        }
         stage('Build Frontend') {
            steps {
                dir('spring-blog-client') {
                    sh 'npm install --legacy-peer-deps'
                    sh 'npm run build -- --configuration=production'
                }
            }
        }
        stage('SonarQube Code Analysis') {
            steps {
                script {
                    dir('spring-blog-client') {
                        sh 'npm install --legacy-peer-deps'
                        sh 'npm run build -- --configuration=production'
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
        }
        
        stage('Build Docker Image') {
            steps {
                script {
                    dir('spring-blog-client') {
                        bat 'docker build -t fatma24/frontend:latest .'
                    }
                }
            }
        }
        
        stage('Push to Docker Hub') {
            steps {
                script {
                        bat 'docker login -u $fatma24 -p $rootroot24'
                        bat 'docker push fatma24/frontend:latest'
                    }
                }
            }
        
        stage('Create Container') {
            steps {
                script {
                    bat 'docker run -d --name frontend fatma24/frontend:latest'
                }
            }
        }
    }
}
