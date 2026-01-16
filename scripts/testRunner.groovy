def runTestsAndCollectAllure(Map config) {
    sh """
      set -e

      echo "Prep allure dir"
      rm -rf ${config.resultsDir}
      mkdir -p ${config.resultsDir}

      echo "Run tests in container: ${config.container}"
      docker rm -f ${config.container} || true

      docker run --name ${config.container} \
        --network ${config.network} \
        ${config.image} \
        mvn -B -Dtest=${config.testSelector} test

      echo "Collect the allure results"
      docker cp \
        ${config.container}:/tests/target/allure-results/. \
        ${config.resultsDir}/

      echo "Clean container"
      docker rm -f ${config.container}
    """
}

return this