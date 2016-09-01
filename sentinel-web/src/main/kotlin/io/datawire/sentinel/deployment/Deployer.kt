package io.datawire.sentinel.deployment

import io.datawire.sentinel.model.KubernetesDeployContext
import io.fabric8.kubernetes.api.model.*
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

    // TODO: We need an external credentials management story if we are going to talk to Tenant's kube clusters
//    val kubeConfig = Config.builder()
//        .withMasterUrl(config().getJsonObject("kube").getString("masterUrl"))
//        .withUsername(config().getJsonObject("kube").getString("masterUsername"))
//        //.withPassword(System.getProperty("kubePassword", config().getJsonObject("kube").getString("masterPassword")))
//        .withTrustCerts(false)
//        .build()

    kube = DefaultKubernetesClient()

//    vertx.executeBlocking<Int>(
//        { fut ->
//          val tenants = config().getJsonArray("tenants")
//          for (record in tenants) {
//            when (record) {
//              is JsonObject -> {
//                val tenantId = record.getString("id")
//                logger.info("Initializing tenant (tenant: {})", tenantId)
//                val km = KubeModels(tenantId)
//                val ns = km.createNamespace()
//
//                if (kube.namespaces().withName(ns.metadata.name).get() == null) {
//                  logger.debug("Creating namespace (ns: {})", ns.metadata.name)
//                  kube.namespaces().create(ns)
//                } else {
//                  logger.debug("Namespace already exists (ns: {})", ns.metadata.name)
//                }
//
//                val sec = km.createTokenSecret(record.getString("token"))
//                if (kube.secrets().inNamespace(ns.metadata.name).withName(sec.metadata.name).get() == null) {
//                  logger.debug("Creating secret (sec: {})", sec.metadata.name)
//                  kube.secrets().create(sec)
//                } else {
//                  logger.debug("Secret already exists (sec: {})", sec.metadata.name)
//                }
//              }
//            }
//          }
//
//          fut.complete(tenants.size())
//        },
//        false,
//        { res ->
//          if (res.succeeded()) {
//            logger.info("Succeeded creating tenants (count: {})", res.result())
//          } else {
//            logger.error("Failed creating tenants", res.cause())
//          }
//        })

    super.start(startFuture)
  }

  override fun start() {
    val kubeDeployer = vertx.eventBus().consumer<JsonObject>("deployer.kube")

    kubeDeployer.handler { msg ->
      val ctx = KubernetesDeployContext.fromJson(msg.body())
      logger.info("Handling deployment for tenant (tn: {})", ctx.datawire.tenant.id)
      vertx.executeBlocking<Void>(
          { fut ->
            runService(ctx.datawire.tenant.id, ctx.datawire.tenant.id, ctx)
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

  private fun runService(tenant: String, namespace: String, config: KubernetesDeployContext) {
    val realName = "${config.service.name}-${config.service.version.replace('.', '-')}"

    logger.debug("Real name = {}", realName)
    logger.debug("Tenant    = {}", tenant)
    logger.debug("Config    = {}", config)
    logger.debug("Srv name  = {}", config.service.name)
    logger.debug("Srv vers  = {}", config.service.version)

    EnvVarSourceBuilder().withNewFieldRef("v1", "status.podIP").build()

    val deployment = DeploymentBuilder().withNewMetadata()
        .withNamespace(namespace)
        .withName(realName)
        .endMetadata()
        .withNewSpec()
        .withReplicas(1)
        .withNewTemplate()
        .withNewMetadata()
        .withLabels(mapOf(
            "app" to config.service.name
        ))
        .endMetadata()
        .withNewSpec()
        .withContainers(listOf(
            ContainerBuilder()
                .withName(realName)
                .withImage("us.gcr.io/datawireio/$tenant-${config.service.name}:${config.service.version}")
                .withEnv(
                    listOf(
                        EnvVar("MDK_SERVICE_NAME", config.service.name, null),
                        EnvVar("MDK_SERVICE_VERSION", config.service.version, null),
                        EnvVar("DATAWIRE_TOKEN",
                               null,
                               EnvVarSourceBuilder().withSecretKeyRef(SecretKeySelector("token", tenant)).build()),

                        EnvVar("DATAWIRE_ROUTABLE_HOST",
                               null,
                               EnvVarSourceBuilder().withNewFieldRef("v1", "status.podIP").build()
                        ),
                        EnvVar("DATAWIRE_ROUTABLE_PORT", "5000", null)
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