pipeline {
	agent any

	parameters {
		gitParameter branchFilter: 'origin/(.*)', defaultValue: 'dev', name: 'MANUAL_GIT_BRANCH', type: 'PT_BRANCH'
		booleanParam name: 'SHOULD_PUBLISH_JOB_STATUS', description: 'true to publish job status', defaultValue: true
	}

	options {
		ansiColor('css')
		timestamps()
	}

	triggers {
		// second of the month
		cron('H 0 2 * *')
	}

	stages {
		stage('print env') {
			steps {
				echo """
							env.GIT_BRANCH: ${env.GIT_BRANCH}
						 MANUAL_GIT_BRANCH: ${MANUAL_GIT_BRANCH}
				"""
			}
		}

		stage('build base images') {
			parallel {
				stage('gcc prior') {
					steps {
						script {
							dispatchBuildBaseImageJob('gcc-prior', 'ubuntu', true)
						}
					}
				}
				stage('gcc latest') {
					steps {
						script {
							dispatchBuildBaseImageJob('gcc-latest', 'ubuntu', true)
						}
					}
				}
				stage('gcc 10 [debian]') {
					steps {
						script {
							dispatchBuildBaseImageJob('gcc-debian', 'debian', false)
						}
					}
				}
				stage('gcc westmere') {
					steps {
						script {
							dispatchBuildBaseImageJob('gcc-westmere', 'ubuntu', true)
						}
					}
				}
				stage('gcc [fedora]') {
					steps {
						script {
							dispatchBuildBaseImageJob('gcc-latest', 'fedora', false)
						}
					}
				}

				stage('clang prior') {
					steps {
						script {
							dispatchBuildBaseImageJob('clang-prior', 'ubuntu', true)
						}
					}
				}
				stage('clang latest') {
					steps {
						script {
							dispatchBuildBaseImageJob('clang-latest', 'ubuntu', true)
						}
					}
				}

				stage('clang ausan') {
					steps {
						script {
							dispatchBuildBaseImageJob('clang-ausan', 'ubuntu', false)
						}
					}
				}
				stage('clang tsan') {
					steps {
						script {
							dispatchBuildBaseImageJob('clang-tsan', 'ubuntu', false)
						}
					}
				}

				stage('msvc latest') {
					steps {
						script {
							dispatchBuildBaseImageJob('msvc-latest', 'windows', true)
						}
					}
				}
				stage('msvc prior') {
					steps {
						script {
							dispatchBuildBaseImageJob('msvc-prior', 'windows', true)
						}
					}
				}

				stage('release base image') {
					steps {
						script {
							dispatchPrepareBaseImageJob('release', 'ubuntu')
						}
					}
				}

				stage('test base image') {
					steps {
						script {
							dispatchPrepareBaseImageJob('test', 'ubuntu')
						}
					}
				}
				stage('test base image [debian]') {
					steps {
						script {
							dispatchPrepareBaseImageJob('test', 'debian')
						}
					}
				}
				stage('test base image [fedora]') {
					steps {
						script {
							dispatchPrepareBaseImageJob('test', 'fedora')
						}
					}
				}
				stage('test base image [windows]') {
					steps {
						script {
							dispatchPrepareBaseImageJob('test', 'windows')
						}
					}
				}
			}
		}
	}
	post {
		success {
			script {
				if (env.SHOULD_PUBLISH_JOB_STATUS?.toBoolean()) {
					helper.sendDiscordNotification(
						':confetti_ball: Catapult Client All Image Job Successfully completed',
						'Not much to see here, all is good',
						env.BUILD_URL,
						currentBuild.currentResult
					)
				}
			}
		}
		unsuccessful {
			script {
				if (env.SHOULD_PUBLISH_JOB_STATUS?.toBoolean()) {
					helper.sendDiscordNotification(
						":confused: Catapult Client All Image Job Failed for ${currentBuild.fullDisplayName}",
						"At least an image job failed for Build#${env.BUILD_NUMBER} with a result of ${currentBuild.currentResult}.",
						env.BUILD_URL,
						currentBuild.currentResult
					)
				}
			}
		}
	}
}

void dispatchBuildBaseImageJob(String compilerConfiguration, String operatingSystem, Boolean shouldBuildConanLayer) {
	build job: 'catapult-client-build-base-image', parameters: [
		string(name: 'COMPILER_CONFIGURATION', value: "${compilerConfiguration}"),
		string(name: 'OPERATING_SYSTEM', value: "${operatingSystem}"),
		string(name: 'SHOULD_BUILD_CONAN_LAYER', value: "${shouldBuildConanLayer}"),
		string(name: 'MANUAL_GIT_BRANCH', value: "${params.MANUAL_GIT_BRANCH}"),
		booleanParam(
			name: 'SHOULD_PUBLISH_FAIL_JOB_STATUS',
			value: "${!env.SHOULD_PUBLISH_JOB_STATUS || env.SHOULD_PUBLISH_JOB_STATUS.toBoolean()}"
		)
	]
}

void dispatchPrepareBaseImageJob(String imageType, String operatingSystem) {
	build job: 'catapult-client-prepare-base-image', parameters: [
		string(name: 'IMAGE_TYPE', value: "${imageType}"),
		string(name: 'OPERATING_SYSTEM', value: "${operatingSystem}"),
		string(name: 'MANUAL_GIT_BRANCH', value: "${params.MANUAL_GIT_BRANCH}"),
		booleanParam(
			name: 'SHOULD_PUBLISH_FAIL_JOB_STATUS',
			value: "${!env.SHOULD_PUBLISH_JOB_STATUS || env.SHOULD_PUBLISH_JOB_STATUS.toBoolean()}"
		)
	]
}
