pipeline {
    agent any

    tools {
        maven 'M2_HOME'
    }

    stages {
        stage('Checkout Git repository') {
            steps {
                echo 'Pulling code from Git'
                git branch: 'skander',  url: 'https://github.com/Beyskander2040/GestionEmail.git'
            }
        }
    }
}
