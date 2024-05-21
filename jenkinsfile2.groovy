pipeline {
    agent {
        label 'Agent'
    }       
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
                        -Dsonar.login=squ_fe54d823cffc1f983186143238d3c607ff2a017b
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
                    sh 'docker-compose up --force-recreate --build -d'
                }
            }
        }
        stage('Deploy Frontend on kubernetes cluster with Ansible') {
            steps {
                withCredentials([sshUserPrivateKey(credentialsId: 'Ansible', keyFileVariable: 'SSH_KEY')]) {
                    sh """
                        ansible-playbook -i hosts.ini --private-key $SSH_KEY --ask-become-pass deploy-frontend.yml
                    """
                }
            }
        }
    }
}
