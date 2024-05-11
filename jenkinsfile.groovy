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
        NEXUS_VERSION = "nexus3"
        NEXUS_PROTOCOL = "http"
        NEXUS_URL = "192.168.74.134:8081"
        NEXUS_REPOSITORY = "Mavenupload"
        NEXUS_CREDENTIAL_ID = "nexusCredential"
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
            steps {
                script {
                    def pom = readMavenPom file: "spring-blog-backend/pom.xml"
                    def filesByGlob = findFiles(glob: "target/*.${pom.packaging}")
                    
                    if (filesByGlob.isEmpty()) {
                        error "No files found matching the glob pattern"
                    } else {
                        def artifactPath = filesByGlob[0].path
                        def artifactExists = fileExists artifactPath

                        if (artifactExists) {
                            // Assign the value of ARTIFACT_VERSION here
                            def ARTIFACT_VERSION = pom.version
                            echo "*** File: ${artifactPath}, group: ${pom.groupId}, packaging: ${pom.packaging}, version ${ARTIFACT_VERSION}"

                            nexusArtifactUploader(
                                nexusVersion: NEXUS_VERSION,
                                protocol: NEXUS_PROTOCOL,
                                nexusUrl: NEXUS_URL,
                                groupId: pom.groupId,
                                version: ARTIFACT_VERSION,
                                repository: NEXUS_REPOSITORY,
                                credentialsId: NEXUS_CREDENTIAL_ID,
                                artifacts: [
                                    [artifactId: pom.artifactId,
                                     classifier: '',
                                     file: artifactPath,
                                     type: pom.packaging]
                                ]
                            )
                        } else {
                            error "*** File: ${artifactPath}, could not be found"
                        }
                    }
                }
            }
        }
    }
}
