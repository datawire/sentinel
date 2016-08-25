package io.datawire.sentinel.deployment

import io.datawire.sentinel.kube.KubeDeployConfig
import io.datawire.sentinel.kube.KubeDeploymentHandler
import io.fabric8.kubernetes.api.model.*
import io.fabric8.kubernetes.api.model.extensions.Deployment
import io.fabric8.kubernetes.api.model.extensions.DeploymentBuilder
import io.fabric8.kubernetes.client.Config
import io.fabric8.kubernetes.client.DefaultKubernetesClient
import io.fabric8.kubernetes.client.KubernetesClient
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory


class Deployer : AbstractVerticle() {

  private val logger = LoggerFactory.getLogger(Deployer::class.java)

  private lateinit var kube: KubernetesClient

  override fun start(startFuture: Future<Void>?) {
    logger.debug(System.getProperty("kubePassword"))


    val kubeConfig = Config.builder()
        .withMasterUrl(config().getJsonObject("kube").getString("masterUrl"))
        .withUsername(config().getJsonObject("kube").getString("masterUsername"))
        .withPassword(System.getProperty("kubePassword", config().getJsonObject("kube").getString("masterPassword")))
        .withTrustCerts(false)
        .build()

    kube = DefaultKubernetesClient(kubeConfig)

    vertx.executeBlocking<Int>(
        { fut ->
          val tenants = config().getJsonArray("tenants")
          for (record in tenants) {
            when (record) {
              is JsonObject -> {
                val tenantId = record.getString("id")
                logger.info("Initializing tenant (tenant: {})", tenantId)
                val km = KubeModels(tenantId)
                val ns = km.createNamespace()

                if (kube.namespaces().withName(ns.metadata.name).get() == null) {
                  logger.debug("Creating namespace (ns: {})", ns.metadata.name)
                  kube.namespaces().create(ns)
                } else {
                  logger.debug("Namespace already exists (ns: {})", ns.metadata.name)
                }

                val sec = km.createTokenSecret(record.getString("token"))
                if (kube.secrets().inNamespace(ns.metadata.name).withName(sec.metadata.name).get() == null) {
                  logger.debug("Creating secret (sec: {})", sec.metadata.name)
                  kube.secrets().create(sec)
                } else {
                  logger.debug("Secret already exists (sec: {})", sec.metadata.name)
                }
              }
            }
          }

          fut.complete(tenants.size())
        },
        false,
        { res ->
          if (res.succeeded()) {
            logger.info("Succeeded creating tenants (count: {})", res.result())
          } else {
            logger.error("Failed creating tenants", res.cause())
          }
        })

    super.start(startFuture)
  }

  override fun start() {
    val kubeDeployer = vertx.eventBus().consumer<JsonObject>("deployer.kube")

    kubeDeployer.handler { msg ->
      val deployConfig = KubeDeployConfig.fromJson(msg.body())
      logger.info("Handling deployment for tenant (tn: {})", deployConfig.tenant)
      vertx.executeBlocking<Void>(
          { fut ->
            runService(deployConfig.tenant, "ns-${deployConfig.tenant}", deployConfig)
            fut.complete()
          },
          false,
          { res ->
            if (res.succeeded()) {
              logger.info("Deployment succeeded!")
            } else {
              logger.error("Failed to run Pod", res.cause())
            }
          })
    }
  }

  private fun runService(tenant: String, namespace: String, config: KubeDeployConfig) {
    val realName = "${config.mobiusConfig.getString("service.name")}-${config.mobiusConfig.getString("service.version")}"

    EnvVarSourceBuilder().withNewFieldRef("v1", "status.podIP").build()

    val deployment = DeploymentBuilder().withNewMetadata()
        .withNamespace(namespace)
        .withName(realName)
        .endMetadata()
        .withNewSpec()
        .withReplicas(2)
        .withNewTemplate()
        .withNewMetadata()
        .withLabels(mapOf(
            "app" to config.mobiusConfig.getString("service.name")
        ))
        .endMetadata()
        .withNewSpec()
        .withContainers(listOf(
            ContainerBuilder()
                .withName(config.mobiusConfig.getString("service.name"))
                .withImage("us.gcr.io/datawireio/$tenant-${config.mobiusConfig.getString("service.name")}:${config.mobiusConfig.getString("service.version")}")
                .withEnv(
                    listOf(
                        EnvVar("MDK_SERVICE_NAME", config.mobiusConfig.getString("service.name"), null),
                        EnvVar("MDK_SERVICE_VERSION", config.mobiusConfig.getString("service.version"), null),
                        EnvVar("DATAWIRE_TOKEN",
                               null,
                               EnvVarSourceBuilder().withSecretKeyRef(SecretKeySelector("token", "sec-$tenant")).build()),

                        EnvVar("DATAWIRE_ROUTABLE_HOST",
                               null,
                               EnvVarSourceBuilder().withNewFieldRef("v1", "status.podIP").build()
                        )
                    )
                ).withPorts(listOf(ContainerPortBuilder().withContainerPort(5000).build())).build()
        ))
        .endSpec()
        .endTemplate()
        .endSpec()
        .build()

    if (kube.extensions().deployments().inNamespace(namespace).withName(realName).get() == null) {
      logger.info("Creating deployment (entry: $realName)")
      kube.extensions().deployments().create(deployment)
    } else {
      logger.warn("Deployment already exists (entry: $realName)")
    }
  }
}