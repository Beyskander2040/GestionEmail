pipeline {
    agent any

    tools {
        maven 'M2_HOME'
    }

    stages {
        stage('Checkout Git repository') {
            steps {
                echo 'Pulling code from Git'
                git branch: 'skander', credentialsId: 'your-credentials-id', url: 'https://github.com/Beyskander2040/GestionEmail.git'
            }
        }
    }
}
