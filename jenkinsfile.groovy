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
                    sh 'npm install'
                    sh 'npm run build -- --configuration=production'
                }
            }
        }
        
        stage('Code Analysis') {
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
        
        stage('Deploy Artifacts to Nexus') {
            steps{
                script{
                    pom = readMavenPom file: "spring-blog-backend/pom.xml";
                    filesByGlob = findFiles(glob: "target/*.${pom.packaging}");

                    if(filesByGlob.isEmpty()) {
                        error "No files found for deployment"
                    } else {
                        artifactPath = filesByGlob[0].path;
                        echo "* File: ${artifactPath}, group: ${pom.groupId}, packaging: ${pom.packaging}, version ${pom.version}, artifactID ${pom.artifactId}, target/${pom.artifactId}.${pom.packaging}";
                        nexusArtifactUploader artifacts: [
                            [artifactId: "${pom.artifactId}",
                            classifier: '',
                            file: "${artifactPath}",
                            type: "${pom.packaging}"]
                        ],
                        credentialsId: 'nexusCredential',
                        groupId: "${pom.groupId}",
                        nexusUrl: '192.168.74.134:8081',
                        nexusVersion: 'nexus3',
                        protocol: 'http',
                        repository: 'Mavenupload',
                        version: "${pom.version}"
                    }
                }
            }
        }
    }
}
