package io.datawire.sentinel.deployment

import io.fabric8.kubernetes.api.model.*
import java.util.*


data class KubeModels(private val tenantId: String) {

  companion object {
    private const val VALIDATION_ENABLED = true
    private const val NO_VALIDATION_ENABLED = false
  }

  fun createMetadata(): ObjectMeta {
    val metadata = createOpenMetadata()
    return metadata.build()
  }

  fun createOpenMetadata(): ObjectMetaBuilder {
    return ObjectMetaBuilder(VALIDATION_ENABLED).withNamespace(tenantId)
  }

  fun createNamespace(): Namespace {
    val namespace = NamespaceBuilder(VALIDATION_ENABLED)

    namespace.withNewMetadata()
        .withName(tenantId)
        .endMetadata()

    return namespace.build()
  }

  fun createTokenSecret(token: String): Secret {
    val metadata = createOpenMetadata().withName(tenantId).build()

    val secret = SecretBuilder(VALIDATION_ENABLED)
        .withMetadata(metadata)
        .addToData("token", Base64.getEncoder().encodeToString(token.toByteArray()))

    return secret.build()
  }
}