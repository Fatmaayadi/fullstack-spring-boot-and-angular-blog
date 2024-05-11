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
        ARTIFACT_VERSION = "0.0.1-SNAPSHOT" // Set the artifact version
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
                    // Read POM xml file using 'readMavenPom' step
                    pom = readMavenPom file: "spring-blog-backend/pom.xml"
                    // Find built artifact under target folder
                    filesByGlob = findFiles(glob: "target/*.${pom.packaging}")
                    // Print some info from the artifact found
                    echo "${filesByGlob[0].name} ${filesByGlob[0].path} ${filesByGlob[0].directory} ${filesByGlob[0].length} ${filesByGlob[0].lastModified}"
                    
                    // Check if filesByGlob array is empty
                    if (filesByGlob) {
                        // Extract the path from the File found
                        artifactPath = filesByGlob[0].path;
                        // Assign to a boolean response verifying If the artifact name exists
                        artifactExists = fileExists artifactPath;

                        if(artifactExists) {
                            echo "*** File: ${artifactPath}, group: ${pom.groupId}, packaging: ${pom.packaging}, version ${pom.version}";

                            nexusArtifactUploader(
                                nexusVersion: NEXUS_VERSION,
                                protocol: NEXUS_PROTOCOL,
                                nexusUrl: NEXUS_URL,
                                groupId: pom.groupId,
                                version: ARTIFACT_VERSION,
                                repository: NEXUS_REPOSITORY,
                                credentialsId: NEXUS_CREDENTIAL_ID,
                                artifacts: [
                                    // Artifact generated such as .jar, .ear and .war files.
                                    [artifactId: pom.artifactId,
                                    classifier: '',
                                    file: artifactPath,
                                    type: pom.packaging]
                                ]
                            );

                        } else {
                            error "*** File: ${artifactPath}, could not be found";
                        }
                    } else {
                        error "No files found matching the glob pattern"
                    }
                }
            }
        }
    }
}
