pipeline {
    agent {
        label 'Agent'
    }    
    
    tools {
        maven 'Maven'
    }
    
    environment {
        scannerHome = tool 'SonarQubeServer'
    }
    
    stages {
        stage('Run Backend Unit Tests') {
            steps {
                dir('spring-blog-backend') {
                    sh 'mvn clean test'
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
        
        stage('Deploying artifacts to Nexus') {
            steps {
                dir('spring-blog-backend') {
                    nexusArtifactUploader(
                        artifacts: [
                            [
                                artifactId: 'spring-blog-backend',
                                classifier: '',
                                file: 'target/spring-blog-backend-0.0.1-SNAPSHOT.jar',
                                type: 'jar'
                            ]
                        ], 
                        credentialsId: 'nexus-credentials',
                        groupId: 'com.github.braians',
                        nexusUrl: '192.168.74.134:8081',
                        nexusVersion: 'nexus3',
                        protocol: 'http',
                        repository: 'Mavenupload',
                        version: '0.0.1'
                    )
                }
            }
        }

        stage('Deploy Backend with Ansible') {
            steps {
                withCredentials([sshUserPrivateKey(credentialsId: 'Ansible', keyFileVariable: 'SSH_KEY')]) {
                    sh """
                        ansible-playbook -i hosts.ini --private-key $SSH_KEY --ask-become-pass deploy-backend.yml
                    """
                }
            }
        }
    }
}
