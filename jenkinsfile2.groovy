pipeline {
    agent {
        label 'Agent'
    }       
    tools {
        nodejs 'NodeJS'
        jdk 'Java'
        dockerTool 'docker' 
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
                    sh 'npm config set registry https://registry.npmjs.org/'
                    sh 'npm install --legacy-peer-deps'
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
        stage('Build Docker Image') {
            steps {
                script {
                    dir('spring-blog-client') {
                        sh 'docker build -t fatma24/frontend:latest .'
                    }
                }
            }
        }
        stage('Push to Docker Hub') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'dockerhub', passwordVariable: 'DOCKER_HUB_PASSWORD', usernameVariable: 'DOCKER_HUB_USERNAME')]) {
                    script {
                        sh "docker login -u ${DOCKER_HUB_USERNAME} -p ${DOCKER_HUB_PASSWORD}"
                        sh "docker push fatma24/frontend:latest"
                    }
                }
            }
        }
        stage('Create Container') {
            steps {
                script {
                    sh 'docker run -d --name frontend fatma24/frontend:latest'
                }
            }
        }
    }
}
