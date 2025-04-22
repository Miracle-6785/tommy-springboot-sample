import groovy.json.JsonSlurper

pipeline {
    agent any

    tools {
        nodejs 'Node20'
        dockerTool 'docker-latest'
        maven 'Maven-3.9'
        jdk 'JDK21'
    }

    environment {
        MARKER_FILE = ".jenkins_commit_marker_${env.BUILD_ID}"
        IS_USING_SONARQUBE = "true"
        IS_USING_DEPENDENCY_TRACK = "true"
        IS_DEPLOY_ON_ECS = "true"
        IS_USING_DATADOG = "false"
        CLOUD_SERVICE_ANNOTATION = "cloud-service/app-info"
        DEPENDENCY_TRACK_ANNOTATION = "dependencytrack/project-id"
        ECS_RESOURCE_MONITORING_ANNOTATION = "aws.amazon.com/amazon-ecs-service-tags"
        AWS_COST_INSIGHT_ANNOTATION = "aws.amazon.com/cost-insights-tags"
        APP_NAME = "tommy-test-springboot"
        AWS_REGION = "ap-southeast-1"
        CLOUD_SERVICE_BACKEND_URL = "https://cloud-service.da-icy.social"
        CLOUD_SERVICE_PROJECT_ID = "1"
        API_URL = "${CLOUD_SERVICE_BACKEND_URL}/1/applications"
        SONAR_PROJECT_KEY = "tommy-test-springboot"
        SONAR_BASE_URL = "https://sonar.da-icy.social"
        DEPENDENCE_BASE_URL = "https://dtrack-be.da-icy.social"
        BACKSTAGE_BE_BASE_URL = "https://backstage-be.da-icy.social"
        FILE_PATH = "catalog-info.yaml"
        DATADOG_DASHBOARD_URL_ANNOTATION = "datadoghq.com/dashboard-url"
        DATADOG_SITE_ANNOTATION = "datadoghq.com/site"
        DATADOG_BASE_URL = "https://api.us5.datadoghq.com"
        DATADOG_SITE_URL = "us5.datadoghq.com"
        DATADOG_FILE_PATH = "datadog.json"
    }

    options {
        skipDefaultCheckout false
    }

    stages {
        stage('Check Branch') {
            when {
                expression { env.BRANCH_NAME != 'main' }
            }
            steps {
                echo "❌ Skipping execution. This branch is not main."
                script {
                    currentBuild.result = 'ABORTED'
                    error("Pipeline stopped: Not on main branch.")
                }
            }
        }

        stage('Check for Skip') {
            steps {
                scmSkip(deleteBuild: true, skipPattern:'.*\\[ci skip\\].*')
            }
        }

        stage('Install packages') {
            steps {
                script {
                    sh '''
                    mvn dependency:go-offline -B
                    echo "✅ Packages installed successfully"
                    '''
                }
            }
        }

        stage('Run test & generate coverage and SBOM'){
            steps {
                script {
                    parallel(
                        'Run Tests & Generate Coverage': {
                            if (env.IS_USING_SONARQUBE == 'true') {
                                sh '''
                                mvn test
                                echo "✅ Coverage test completed successfully"
                                '''
                            } else {
                                echo "🚀 Skipping Tests & Coverage since IS_USING_SONARQUBE is not true."
                            }
                        },

                        "Generate SBOM": {
                            if (env.IS_USING_DEPENDENCY_TRACK == 'true') {
                                sh '''
                                mvn cyclonedx:makeAggregateBom
                                echo "✅ SBOM generated successfully"
                                '''
                            } else {
                                echo "🚀 Skipping SBOM generation since IS_USING_DEPENDENCY_TRACK is not true."
                            }
                        },
                    )
                }
            }
        }

        stage('Upload report to analytics server & update dependeny track annotation') {
            steps {
                script {
                    parallel(
                        'SonarQube Analysis': {
                            if (env.IS_USING_SONARQUBE == 'true') {
                                withSonarQubeEnv('SonarQube') {
                                    echo "📤 Uploading scanned result to SonarQube server"
                                    sh """
                                    mvn verify org.sonarsource.scanner.maven:sonar-maven-plugin:sonar \
                                        -Dsonar.projectKey=$SONAR_PROJECT_KEY \
                                        -Dsonar.projectName=$SONAR_PROJECT_KEY \
                                        -Dsonar.host.url=$SONAR_BASE_URL
                                    """
                                }
                            } else {
                                echo "🚀 Skipping SonarQube Analysis since IS_USING_SONARQUBE is not true."
                            }
                        },

                        'dependencyTrackPublisher': {
                            if (env.IS_USING_DEPENDENCY_TRACK == 'true') {
                                withCredentials([string(credentialsId: 'DPCred', variable: 'API_KEY')]) {
                                    dependencyTrackPublisher artifact: 'target/bom.json', projectName: '$APP_NAME', projectVersion: '1.1.1', synchronous: true, dependencyTrackApiKey: API_KEY
                                }
                                sh 'echo "✅ Dependency Track scan completed successfully"'
                            } else {
                                echo "🚀 Skipping Dependency-Track Publisher since IS_USING_DEPENDENCY_TRACK is not true."
                            }
                        },

                        'Check & Update Dependency-Track Project ID': {
                            if (env.IS_USING_DEPENDENCY_TRACK == 'true') {
                                withCredentials([string(credentialsId: 'DPCred', variable: 'API_KEY')]) {
                                    script {
                                        if (checkAnnotationNotExist(DEPENDENCY_TRACK_ANNOTATION, FILE_PATH)) {
                                            echo "🔍 dependencytrack/project-id is empty. Fetching Project UUID from Dependency-Track..."

                                            def projectId = fetchProjectUUID(APP_NAME, API_KEY)

                                            if (projectId && projectId != "null") {
                                                echo "✅ Found Project UUID: ${projectId}"

                                                updateCatalogInfo(DEPENDENCY_TRACK_ANNOTATION, projectId)

                                                gitCommit("Updated dependencytrack/project-id in catalog-info.yaml")

                                                createMakerFile()
                                            } else {
                                                error "❌ Failed to fetch Project UUID from Dependency-Track!"
                                            }
                                        } else {
                                            echo "✅ dependencytrack/project-id is already set. Skipping update."
                                        }
                                    }
                                }
                            } else {
                                echo "🚀 Skipping Dependency-Track Project ID update since IS_USING_DEPENDENCY_TRACK is not true."
                            }
                        }
                    )
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId:'miracle-docker-cred', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
                        sh 'docker build -t miraclezz/$APP_NAME:${BUILD_NUMBER} .'
                        sh 'docker login -u ${USERNAME} -p ${PASSWORD}'
                        sh 'docker push miraclezz/$APP_NAME:${BUILD_NUMBER}'
                        echo "✅ Docker image pushed to Docker Hub"
                    }
                }
            }
        }

        stage('Configure & Deploy to ECS') {
            when {
                expression { return env.IS_DEPLOY_ON_ECS == 'true' }
            }
            steps {
                script {
                    echo "🚀 Deploying to ECS"
                    def response = sh(script: """
                        curl -X POST ${API_URL} \
                        -H 'accept: application/json' \
                        -H 'Content-Type: application/json' \
                        -d '{
                            "applicationName": "${APP_NAME}",
                            "description": "",
                            "parameter": {
                                "provider": "miracle-credential",
                                "services": [
                                {
                                    "network_type": "public",
                                    "container_configs": {
                                    "resource_request": "Micro (1 vCPU - 2GB RAM)",
                                    "image": "miraclezz/$APP_NAME:${BUILD_NUMBER}",
                                    "port": 8080
                                    },
                                    "desired_count": 1,
                                    "autoscaling_min_capacity": 1,
                                    "autoscaling_max_capacity": 1,
                                    "name": "${APP_NAME}-svc"
                                }
                                ],
                                "ecr_configs": {
                                "repository_type": "public",
                                "repository_name": "${APP_NAME}-registry"
                                },
                                "cluster_name": "${APP_NAME}-cluster"
                            },
                            "templateUuid": "8e2de2e7-e3de-4a22-93b8-2f7a6b56b2a4"
                            }'
                    """, returnStdout: true).trim()

                    echo "✅ ECS deployment completed successfully"
                }
            }
        }

        stage('Check & Update Cloud service annotation') {
            when {
                expression { return env.IS_DEPLOY_ON_ECS == 'true' }
            }
            steps {
                script {
                    if (checkAnnotationNotExist(CLOUD_SERVICE_ANNOTATION, FILE_PATH)) {
                        echo "🔍 cloud-service/app-info is empty. Fetching application information..."

                        def applicationId = fetchApplicationUUID()

                        echo "Application UUID: ${applicationId}"

                        if (applicationId && applicationId != "null") {
                            echo "✅ Found Project UUID: ${applicationId}"

                            updateCatalogInfo(CLOUD_SERVICE_ANNOTATION, "${CLOUD_SERVICE_PROJECT_ID}/${applicationId}")

                            gitCommit("Updated cloud-service/app-info in catalog-info.yaml")

                            updateCatalogInfo(ECS_RESOURCE_MONITORING_ANNOTATION, "Application=${applicationId}")

                            gitCommit("Updated ECS resources monitoring annotation in catalog-info.yaml")

                            createMakerFile()

                            if (env.IS_USING_DATADOG == 'true') {
                                // Create datadog dashboard, shared dashboard and update annotation
                                withCredentials([string(credentialsId: 'DataDogApiKey', variable: 'API_KEY'),
                                                string(credentialsId: 'DataDogApplicationKey', variable: 'APPLICATION_KEY')]) {

                                    def dashboardId = createDataDogDashboard(API_KEY, APPLICATION_KEY)
                                    def dashboardUrl = shareDataDogDashboard(API_KEY, APPLICATION_KEY, dashboardId)

                                    updateCatalogInfo(DATADOG_SITE_ANNOTATION, DATADOG_SITE_URL)
                                    gitCommit("Updated datadog site url in catalog-info.yaml")

                                    updateCatalogInfo(DATADOG_DASHBOARD_URL_ANNOTATION, dashboardUrl)
                                    gitCommit("Updated datadog dashboard url in catalog-info.yaml")
                                }
                            }
                        } else {
                            error "❌ Failed to fetch Cloud service application information!"
                        }
                    } else {
                        echo "✅ Cloud service application information is exist. Skipping update."
                    }
                }
            }
        }

        stage('Check & Update cost insight annotation') {
            steps {
                script {
                    if (checkAnnotationNotExist(AWS_COST_INSIGHT_ANNOTATION, FILE_PATH)) {
                        echo "🔍 aws.amazon.com/cost-insights-tags is empty. Fetching application information..."

                        def applicationId = fetchApplicationUUID()

                        echo "Application UUID: ${applicationId}"

                        if (applicationId && applicationId != "null") {
                            echo "✅ Found Project UUID: ${applicationId}"

                            updateCatalogInfo(AWS_COST_INSIGHT_ANNOTATION, "Application=${applicationId}")

                            gitCommit("Updated aws.amazon.com/cost-insights-tags in catalog-info.yaml")

                            createMakerFile()
                        } else {
                            error "❌ Failed to fetch Cost insight information!"
                        }
                    } else {
                        echo "✅ Cost insight is exist. Skipping update."
                    }
                }
            }
        }

        stage('Push changes') {
            when {
                expression { return fileExists(env.MARKER_FILE) }
            }
            steps {
                script {
                    echo "Commit marker file found. Proceeding with push..."

                    pushChanges()

                    deregisterWithBackstage()

                    registerWithBackstage()

                    sh "rm -f ${env.MARKER_FILE}"
                }
            }
        }
    }

    post {
        always {
            cleanWs()
            echo "🎉 Pipeline completed successfully!"
        }
    }
}

def createDataDogDashboard(apiKey, applicationKey) {
    def response = sh(script: """
    curl -X POST "${DATADOG_BASE_URL}/api/v1/dashboard" \
    -H "Content-Type: application/json" \
    -H "DD-API-KEY: ${apiKey}" \
    -H "DD-APPLICATION-KEY: ${applicationKey}" \
    --data-binary "@${DATADOG_FILE_PATH}"
    """, returnStdout: true).trim()

    def slurper = new groovy.json.JsonSlurper()
    def result = slurper.parseText(response)

    def dashboardId = result.id

    return dashboardId
}

def shareDataDogDashboard(apiKey, applicationKey, dashboardId) {
    def response = sh(script: """
    curl -X POST "${DATADOG_BASE_URL}/api/v1/dashboard/public" \
    -H "Content-Type: application/json" \
    -H "DD-API-KEY: ${apiKey}" \
    -H "DD-APPLICATION-KEY: ${applicationKey}" \
    -d '{
        "dashboard_id": "${dashboardId}",
        "dashboard_type": "custom_screenboard",
        "share_type": "open"
    }'
    """, returnStdout: true).trim()

    def dashboardUrl = sh(script: "echo '${response}' | jq -r '.public_url'", returnStdout: true).trim()

    return dashboardUrl
}

def createMakerFile() {
    sh "touch ${env.MARKER_FILE}"
    echo "Marker file ${env.MARKER_FILE} created."
}

def checkAnnotationNotExist(annotation, filePath) {
    def value = sh(script: "grep '${annotation}:' ${filePath} | awk -F': ' '{print \$2}'", returnStdout: true).trim()
    check = value.replaceAll('"', '')
    return check == "" || check == "null"
}

def fetchApplicationUUID() {
    def response = sh(script: """
        curl -s -X 'GET' '${CLOUD_SERVICE_BACKEND_URL}/${CLOUD_SERVICE_PROJECT_ID}/applications' \
        -H 'accept: application/json'
    """, returnStdout: true).trim()

    def applicationId = sh(script: """
        clean_res=\$(echo '${response}' | tr -d '\\000-\\031')
        echo "\$clean_res" | jq -r '.data[] | select(.name == "${APP_NAME}") | .uuid'
    """, returnStdout: true).trim()

    return applicationId
}

def fetchProjectUUID(appName, apiKey) {
    def response = sh(script: """
        curl -s -X 'GET' '${DEPENDENCE_BASE_URL}/api/v1/project/lookup?name=${appName}&version=1.1.1' \
        -H 'accept: application/json' \
        -H 'X-Api-Key: ${apiKey}'
    """, returnStdout: true).trim()

    def projectId = sh(script: "echo '${response}' | jq -r '.uuid'", returnStdout: true).trim()

    return projectId
}

def updateCatalogInfo(key, value) {
    def exists = sh(script: "grep -q '^[[:space:]]*${key}:' ${FILE_PATH}", returnStatus: true) == 0

    if (exists) {
        sh "sed -i 's|${key}: \"\"|${key}: \"${value}\"|' ${FILE_PATH}"
    } else {
        sh "sed -i '/annotations:/a\\    ${key}: \"${value}\"' ${FILE_PATH}"
    }
    echo "✅ ${key} updated successfully!"
}

def gitCommit(commitMessage) {
    withCredentials([usernamePassword(credentialsId: 'adminGithubCred', usernameVariable: 'GIT_USER', passwordVariable: 'GIT_PASS')]) {
        script {
            def repoUrl = sh(script: "git config --get remote.origin.url", returnStdout: true).trim()
            repoUrl = repoUrl.startsWith("git@") ? repoUrl.replace("git@", "https://").replace(".com:", ".com/") : repoUrl

            echo "🔄 Using Git repository: ${repoUrl}"
            sh """
            git config --global user.email "miracleiztb@gmail.com"
            git config --global user.name "Miracle"
            git add ${FILE_PATH}
            git commit -m "[ci skip] ${commitMessage}"
            """
            echo "✅ Changes committed successfully!"
        }
    }
}

def pushChanges() {
    withCredentials([usernamePassword(credentialsId: 'adminGithubCred', usernameVariable: 'GIT_USER', passwordVariable: 'GIT_PASS')]) {
        script {
            def repoUrl = sh(script: "git config --get remote.origin.url", returnStdout: true).trim()
            repoUrl = repoUrl.startsWith("git@") ? repoUrl.replace("git@", "https://").replace(".com:", ".com/") : repoUrl

            echo "🔄 Using Git repository: ${repoUrl}"
            sh """
            git push https://${GIT_USER}:${GIT_PASS}@${repoUrl.replace('https://', '')} HEAD:main
            """
            echo "✅ Changes committed and pushed successfully!"
        }
    }
}

def deregisterWithBackstage() {
    withCredentials([string(credentialsId: 'BackstageAPIKey', variable: 'API_KEY')]) {
        script {
            def repoUrl = sh(script: "git config --get remote.origin.url", returnStdout: true).trim()
            def cleanedRepoUrl = repoUrl.replace('.git', '')

            def locations = sh(script: """
                curl -s -X GET "${BACKSTAGE_BE_BASE_URL}/api/catalog/locations" \\
                    -H "accept: application/json" \\
                    -H "Content-Type: application/json" \\
                    -H "Authorization: Bearer ${API_KEY}" \\
            """, returnStdout: true).trim()

            //  .[] | .data | select(.target == "https://github.com/Miracle-6785/demo25/tree/main/catalog-info.yaml") | .id

            def locationId = sh(script: """
                echo '${locations}' | jq -r '.[] | .data | select(.target == "${cleanedRepoUrl}/tree/main/catalog-info.yaml") | .id'
            """, returnStdout: true).trim()

            def res = sh(script: """
                curl -s -X DELETE "${BACKSTAGE_BE_BASE_URL}/api/catalog/locations/${locationId}" \\
                    -H "accept: application/json" \\
                    -H "Content-Type: application/json" \\
                    -H "Authorization: Bearer ${API_KEY}" \\
            """, returnStdout: true).trim()

            echo "✅ De-registered catalog-info.yaml with Backstage: ${locationId}"
        }
    }
}

def registerWithBackstage() {
    withCredentials([string(credentialsId: 'BackstageAPIKey', variable: 'API_KEY')]) {
        script {
            def repoUrl = sh(script: "git config --get remote.origin.url", returnStdout: true).trim()
            def cleanedRepoUrl = repoUrl.replace('.git', '')

            def res = sh(script: """
                curl -s -X POST "${BACKSTAGE_BE_BASE_URL}/api/catalog/locations" \\
                    -H "accept: application/json" \\
                    -H "Content-Type: application/json" \\
                    -H "Authorization: Bearer ${API_KEY}" \\
                    -d '{
                        "type": "url",
                        "target": "${cleanedRepoUrl}/blob/main/catalog-info.yaml"
                    }'
            """, returnStdout: true).trim()

            echo "✅ Registered catalog-info.yaml with Backstage: ${res}"
        }
    }
}
